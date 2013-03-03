package com.nostra13.socialsharing.twitter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
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
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.TwitterException;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class TwitterDialog extends Dialog {

	public static final String TAG = "twitter";

	static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

	static final String JS_HTML_EXTRACTOR = "javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');";
	static final String OAUTH_PIN_BLOCK_REGEXP = "id=\\\"oauth_pin((.|\\n)*)(\\d{7})";
	static final String OAUTH_PIN_REGEXP = "\\d{7}";

	private ProgressDialog spinner;
	private WebView browser;
	private FrameLayout content;

	private AsyncTwitter twitter;
	private String requestUrl;

	private AuthListener authListener;

	public TwitterDialog(Context context, AsyncTwitter twitter) {
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
			public void onAuthRequestComplete(String requestUrl) {
				TwitterDialog.this.requestUrl = requestUrl;
				browser.loadUrl(requestUrl);
			}
		});
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setUpWebView(int margin) {
		LinearLayout webViewContainer = new LinearLayout(getContext());
		browser = new WebView(getContext());
		browser.setVerticalScrollBarEnabled(false);
		browser.setHorizontalScrollBarEnabled(false);
		browser.setWebViewClient(new TwitterDialog.TwWebViewClient());
		browser.getSettings().setJavaScriptEnabled(true);
		browser.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		browser.setLayoutParams(FILL);

		webViewContainer.setPadding(margin, margin, margin, margin);
		webViewContainer.addView(browser);
		content.addView(webViewContainer);
	}

	public void setAuthListener(AuthListener authListener) {
		this.authListener = authListener;
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
			if (authListener != null) authListener.onAuthFail(description);
			TwitterEvents.onLoginError(description);
			TwitterDialog.this.dismiss();
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
			browser.loadUrl(JS_HTML_EXTRACTOR);

			content.setBackgroundColor(Color.TRANSPARENT);
			browser.setVisibility(View.VISIBLE);
		}
	}

	class MyJavaScriptInterface {
		@JavascriptInterface
		public void processHTML(String html) {
			String blockWithPin = findExpression(html, OAUTH_PIN_BLOCK_REGEXP);
			if (blockWithPin != null) {
				String pin = findExpression(blockWithPin, OAUTH_PIN_REGEXP);
				if (pin != null) {
					autorizeApp(pin);
					spinner.dismiss();
					dismiss();
				}
			}
			spinner.dismiss();
		}

		private String findExpression(String text, String regExp) {
			Pattern p = Pattern.compile(regExp);
			Matcher m = p.matcher(text);
			if (m.find()) {
				return m.group(0);
			} else {
				return null;
			}
		}

		private void autorizeApp(String pin) {
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(pin);
				TwitterSessionStore.save(accessToken, getContext());
				if (authListener != null) authListener.onAuthSucceed();
				TwitterEvents.onLoginSuccess();
			} catch (TwitterException e) {
				Log.e(TAG, e.getMessage(), e);
				if (authListener != null) authListener.onAuthFail(e.getMessage());
				TwitterEvents.onLoginError(e.getMessage());
			}
		}
	}
}
