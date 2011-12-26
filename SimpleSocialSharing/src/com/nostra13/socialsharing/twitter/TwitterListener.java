package com.nostra13.socialsharing.twitter;

import twitter4j.TwitterException;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
interface TwitterListener {

	void onStatusUpdateComplete();

	void onStatusUpdateFailed(TwitterException e);
}
