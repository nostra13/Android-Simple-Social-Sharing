package com.nostra13.socialsharing.twitter;

import android.util.Log;
import twitter4j.Status;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class TwitterPostListener extends TwitterAdapter {

	private static final String TAG = TwitterPostListener.class.getSimpleName();

	@Override
	public void updatedStatus(Status status) {
		TwitterEvents.onPostPublished();
	}

	@Override
	public void onException(TwitterException e, TwitterMethod method) {
		Log.e(TAG, e.getMessage(), e);
		if (method == TwitterMethod.UPDATE_STATUS) {
			TwitterEvents.onPostPublishingFailed();
		}
	}
};
