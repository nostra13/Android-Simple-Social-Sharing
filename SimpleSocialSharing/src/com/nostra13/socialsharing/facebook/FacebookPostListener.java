package com.nostra13.socialsharing.facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.util.Log;

import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.FacebookError;
import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.AsyncFacebookRunner.RequestListener;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class FacebookPostListener implements RequestListener {

	private static final String TAG = FacebookPostListener.class.getSimpleName();

	@Override
	public void onComplete(final String response, final Object state) {
		FacebookEvents.onPostPublished();
	}

	@Override
	public void onFacebookError(FacebookError e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
		FacebookEvents.onPostPublishingFailed();
	}

	@Override
	public void onFileNotFoundException(FileNotFoundException e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
		FacebookEvents.onPostPublishingFailed();
	}

	@Override
	public void onIOException(IOException e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
		FacebookEvents.onPostPublishingFailed();
	}

	@Override
	public void onMalformedURLException(MalformedURLException e, final Object state) {
		Log.e(TAG, e.getMessage(), e);
		FacebookEvents.onPostPublishingFailed();
	}
}
