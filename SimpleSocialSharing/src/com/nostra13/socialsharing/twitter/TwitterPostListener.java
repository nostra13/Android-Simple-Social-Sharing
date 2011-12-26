package com.nostra13.socialsharing.twitter;

import twitter4j.TwitterException;
import android.util.Log;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class TwitterPostListener implements TwitterListener {

	private static final String TAG = TwitterPostListener.class.getSimpleName();

	@Override
	public void onStatusUpdateComplete() {
		TwitterEvents.onPostPublished();
	}

	@Override
	public void onStatusUpdateFailed(TwitterException e) {
		Log.e(TAG, e.getMessage(), e);
		TwitterEvents.onPostPublishingFailed();
	}
};
