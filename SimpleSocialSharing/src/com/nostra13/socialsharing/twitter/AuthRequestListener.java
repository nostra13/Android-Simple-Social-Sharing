package com.nostra13.socialsharing.twitter;

import twitter4j.TwitterException;
import twitter4j.auth.RequestToken;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public interface AuthRequestListener {

	void onAuthRequestComplete(RequestToken requestToken);

	void onAuthRequestFailed(TwitterException e);
}
