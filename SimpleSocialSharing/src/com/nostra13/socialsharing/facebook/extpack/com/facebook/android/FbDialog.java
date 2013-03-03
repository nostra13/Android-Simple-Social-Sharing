/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nostra13.socialsharing.facebook.extpack.com.facebook.android;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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

import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.Facebook.DialogListener;

public class FbDialog extends Dialog {

	static final int FB_BLUE = 0xFF6D84B4;
	static final float[] DIMENSIONS_DIFF_LANDSCAPE = {20, 60};
	static final float[] DIMENSIONS_DIFF_PORTRAIT = {40, 60};
	static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	static final int MARGIN = 4;
	static final int PADDING = 2;
	static final String DISPLAY_STRING = "touch";
	static final String FB_ICON = "icon.png";

	private String mUrl;
	private DialogListener mListener;
	private ProgressDialog mSpinner;
	private WebView mWebView;
	private FrameLayout mContent;

	public FbDialog(Context context, String url, DialogListener listener) {
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		mUrl = url;
		mListener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSpinner = new ProgressDialog(getContext());
		mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSpinner.setMessage("Loading...");
		mSpinner.setCancelable(false);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContent = new FrameLayout(getContext());

		setUpWebView(10);

		/* Finally add the 'x' image to the mContent layout and
		 * add mContent to the Dialog view
		 */
		addContentView(mContent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setUpWebView(int margin) {
		LinearLayout webViewContainer = new LinearLayout(getContext());
		mWebView = new WebView(getContext());
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setWebViewClient(new FbDialog.FbWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.loadUrl(mUrl);
		mWebView.setLayoutParams(FILL);
		mWebView.setVisibility(View.INVISIBLE);

		webViewContainer.setPadding(margin, margin, margin, margin);
		webViewContainer.addView(mWebView);
		mContent.addView(webViewContainer);
	}

	private class FbWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d("Facebook-WebView", "Redirect URL: " + url);
			if (url.startsWith(Facebook.REDIRECT_URI)) {
				Bundle values = Util.parseUrl(url);

				String error = values.getString("error");
				if (error == null) {
					error = values.getString("error_type");
				}

				if (error == null) {
					mListener.onComplete(values);
				} else if (error.equals("access_denied") || error.equals("OAuthAccessDeniedException")) {
					mListener.onCancel();
				} else {
					mListener.onFacebookError(new FacebookError(error));
				}

				FbDialog.this.dismiss();
				return true;
			} else if (url.startsWith(Facebook.CANCEL_URI)) {
				mListener.onCancel();
				FbDialog.this.dismiss();
				return true;
			} else if (url.contains(DISPLAY_STRING)) {
				return false;
			}
			// launch non-dialog URLs in a full browser
			getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			return true;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			mListener.onError(new DialogError(description, errorCode, failingUrl));
			FbDialog.this.dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d("Facebook-WebView", "Webview loading URL: " + url);
			super.onPageStarted(view, url, favicon);
			mSpinner.show();
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			mSpinner.dismiss();
			/* 
			 * Once webview is fully loaded, set the mContent background to be transparent
			 * and make visible the 'x' image. 
			 */
			mContent.setBackgroundColor(Color.TRANSPARENT);
			mWebView.setVisibility(View.VISIBLE);
		}
	}
}
