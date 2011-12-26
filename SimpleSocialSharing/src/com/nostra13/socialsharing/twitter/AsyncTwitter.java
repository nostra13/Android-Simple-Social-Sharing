package com.nostra13.socialsharing.twitter;


import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.internal.async.Dispatcher;
import twitter4j.internal.async.DispatcherFactory;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class AsyncTwitter {

	private Twitter twitter;
	private Dispatcher dispatcher;

	public AsyncTwitter() {
		twitter = new TwitterFactory().getInstance();
		dispatcher = new DispatcherFactory().getInstance();
	}

	public void setOAuthConsumer(String consumerKey, String consumerSecret) {
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
	}

	public void setOAuthAccessToken(AccessToken accessToken) {
		twitter.setOAuthAccessToken(accessToken);
	}

	public AccessToken getOAuthAccessToken(RequestToken requestToken, String pin) throws TwitterException {
		return twitter.getOAuthAccessToken(requestToken, pin);
	}

	public void getOAuthRequestToken(final AuthRequestListener listener) {
		dispatcher.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					RequestToken requestToken = twitter.getOAuthRequestToken();
					listener.onAuthRequestComplete(requestToken);
				} catch (TwitterException e) {
					listener.onAuthRequestFailed(e);
				}
			}
		});
	}

	public void updateStatus(final String message, final TwitterListener listener) {
		dispatcher.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					twitter.updateStatus(message);
					listener.onStatusUpdateComplete();
				} catch (TwitterException e) {
					listener.onStatusUpdateFailed(e);
				}
			}
		});
	}

	public void shutdown() {
		twitter.shutdown();
	}
}
