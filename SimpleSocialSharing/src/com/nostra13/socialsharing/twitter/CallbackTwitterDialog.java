package com.nostra13.socialsharing.twitter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.TwitterException;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class CallbackTwitterDialog extends Dialog {

	private static final String TAG = CallbackTwitterDialog.class.getSimpleName();

	private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

	private static final String OAUTH_VERIFIER_KEY = "oauth_verifier";

	private static final String PARAMS_START = "?";
	private static final String PARAMS_SEPARATOR = "&";
	private static final String KEY_VALUE_SEPARATOR = "=";

	private ProgressDialog spinner;
	private WebView browser;
	private FrameLayout content;

	private AsyncTwitter twitter;
	private String requestUrl;

	private AuthListener authListener;
	private Handler handler;

	public CallbackTwitterDialog(Context context, AsyncTwitter twitter) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		this.twitter = twitter;
		this.handler = new Handler();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		spinner = new ProgressDialog(getContext());
		spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		spinner.setMessage("Loading...");
		spinner.setCancelable(false);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		content = new FrameLayout(getContext());
		setUpWebView(10);
		addContentView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	@Override
	public void show() {
		super.show();
		browser.setVisibility(View.INVISIBLE);
		spinner.show();
		if (requestUrl == null) {
			retrieveRequestToken();
		} else {
			browser.loadUrl(requestUrl);
		}
	}

	private void retrieveRequestToken() {
		twitter.getOAuthRequestToken(new AuthRequestListener() {
			@Override
			public void onAuthRequestFailed(Exception e) {
				Log.e(TAG, e.getMessage(), e);
				String errorMessage = e.getMessage();
				if (errorMessage == null) {
					errorMessage = e.getMessage();
				}
				TwitterEvents.onLoginError(errorMessage);
				spinner.dismiss();
				dismiss();
			}

			@Override
			public void onAuthRequestComplete(final String requestUrl) {
				CallbackTwitterDialog.this.requestUrl = requestUrl;
				handler.post(new Runnable() {
					@Override
					public void run() {
						browser.loadUrl(requestUrl);
					}
				});
			}
		});
	}

	@SuppressLint("SetJavaScriptEnabled")
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
				new AuthorizationTask().execute(pin);
			} else {
				if (authListener != null) authListener.onAuthFail(description);
				TwitterEvents.onLoginError(description);
				dismiss();
			}
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

	public void setAuthListener(AuthListener authListener) {
		this.authListener = authListener;
	}

	private class AuthorizationTask extends AsyncTask<String, Void, Boolean> {

		private Exception occuredException;

		@Override
		protected Boolean doInBackground(String... params) {
			String pin = params[0];
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(pin);
				TwitterSessionStore.save(accessToken, getContext());
				return true;
			} catch (TwitterException e) {
				Log.e(TAG, e.getMessage(), e);
				occuredException = e;
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				if (authListener != null) authListener.onAuthSucceed();
				TwitterEvents.onLoginSuccess();
			} else {
				if (authListener != null) authListener.onAuthFail(occuredException.getMessage());
				TwitterEvents.onLoginError(occuredException.getMessage());
			}
			spinner.dismiss();
			dismiss();
		}
	}
}
