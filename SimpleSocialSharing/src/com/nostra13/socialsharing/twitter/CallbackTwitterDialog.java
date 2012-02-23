package com.nostra13.socialsharing.twitter;

import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class CallbackTwitterDialog extends Dialog {

	private static final String TAG = CallbackTwitterDialog.class.getSimpleName();

	private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

	private static final String OAUTH_VERIFIER_KEY = "oauth_verifier";

	private static final String PARAMS_START = "?";
	private static final String PARAMS_SEPARATOR = "&";
	private static final String KEY_VALUE_SEPARATOR = "=";

	private ProgressDialog spinner;
	private WebView browser;
	private FrameLayout content;

	private AsyncTwitter twitter;
	private RequestToken requestToken;

	public CallbackTwitterDialog(Context context, AsyncTwitter twitter) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		this.twitter = twitter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		spinner = new ProgressDialog(getContext());
		spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		spinner.setMessage("Loading...");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		content = new FrameLayout(getContext());
		setUpWebView(10);
		addContentView(content, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	@Override
	public void show() {
		super.show();
		browser.setVisibility(View.INVISIBLE);
		spinner.show();
		if (requestToken == null) {
			retrieveRequestToken();
		} else {
			browser.loadUrl(requestToken.getAuthorizationURL());
		}
	}

	private void retrieveRequestToken() {
		twitter.getOAuthRequestToken(new AuthRequestListener() {
			@Override
			public void onAuthRequestFailed(TwitterException e) {
				Log.e(TAG, e.getErrorMessage(), e);
				String errorMessage = e.getErrorMessage();
				if (errorMessage == null) {
					errorMessage = e.getMessage();
				}
				TwitterEvents.onLoginError(errorMessage);
				spinner.dismiss();
				dismiss();
			}

			@Override
			public void onAuthRequestComplete(RequestToken requestToken) {
				CallbackTwitterDialog.this.requestToken = requestToken;
				browser.loadUrl(requestToken.getAuthorizationURL());
			}
		});
	}

	private void setUpWebView(int margin) {
		LinearLayout webViewContainer = new LinearLayout(getContext());
		browser = new WebView(getContext());
		browser.setVerticalScrollBarEnabled(false);
		browser.setHorizontalScrollBarEnabled(false);
		browser.setWebViewClient(new TwWebViewClient());
		browser.getSettings().setJavaScriptEnabled(true);
		browser.setLayoutParams(FILL);

		webViewContainer.setPadding(margin, margin, margin, margin);
		webViewContainer.addView(browser);
		content.addView(webViewContainer);
	}

	private class TwWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			String pin = extractAuthVerifier(failingUrl);
			if (pin != null) {
				authorizeApp(pin);
				spinner.dismiss();
			} else {
				TwitterEvents.onLoginError(description);
			}
			dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d(TAG, "WebView loading URL: " + url);
			super.onPageStarted(view, url, favicon);
			spinner.show();
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);

			content.setBackgroundColor(Color.TRANSPARENT);
			browser.setVisibility(View.VISIBLE);
			spinner.dismiss();
		}
	}

	private String extractAuthVerifier(String url) {
		String verifier = null;
		if (url.contains(PARAMS_START)) {
			url = url.substring(url.indexOf(PARAMS_START));
			String[] params = url.split(PARAMS_SEPARATOR);
			for (String param : params) {
				String[] keyValue = param.split(KEY_VALUE_SEPARATOR);
				if (OAUTH_VERIFIER_KEY.equals(keyValue[0])) {
					verifier = keyValue[1];
				}
			}
		}
		return verifier;
	}

	private void authorizeApp(String pin) {
		try {
			AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, pin);
			TwitterSessionStore.save(accessToken, getContext());
			TwitterEvents.onLoginSuccess();
		} catch (TwitterException e) {
			Log.e(TAG, e.getMessage(), e);
			TwitterEvents.onLoginError(e.getErrorMessage());
		}
	}
}
