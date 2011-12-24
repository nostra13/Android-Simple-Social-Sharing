package com.nostra13.socialsharing.facebook;

import android.os.Bundle;
import android.util.Log;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class FacebookAuthListener implements DialogListener {

	private static final String TAG = FacebookAuthListener.class.getSimpleName();

	@Override
	public void onFacebookError(FacebookError e) {
		Log.e(TAG, e.getMessage(), e);
		FacebookEvents.onLoginError(e.getMessage());
	}

	@Override
	public void onError(DialogError e) {
		Log.e(TAG, e.getMessage(), e);
		FacebookEvents.onLoginError(e.getMessage());
	}

	@Override
	public void onComplete(Bundle values) {
		FacebookEvents.onLoginSuccess();
	}

	@Override
	public void onCancel() {
		// Do nothing
	}
}
