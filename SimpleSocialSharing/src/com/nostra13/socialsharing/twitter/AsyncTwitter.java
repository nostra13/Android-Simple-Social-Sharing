package com.nostra13.socialsharing.twitter;

import java.net.URI;

import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.OAuthSignpostClient;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter;


/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class AsyncTwitter {

	private Twitter twitter;
	private TaskExecutor dispatcher;
	private OAuthSignpostClient oauthClient;

	private String consumerKey;
	private String consumerSecret;
	private AccessToken accessToken;

	public AsyncTwitter(String consumerKey, String consumerSecret) {
		dispatcher = TaskExecutor.newInstance();
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
	}

	public void setOAuthAccessToken(AccessToken accessToken) {
		this.accessToken = accessToken;
	}

	public AccessToken getOAuthAccessToken(String pin) {
		initOAuthClient();

		oauthClient.setAuthorizationCode(pin);
		String[] token = oauthClient.getAccessToken();
		return new AccessToken(token[0], token[1]);
	}

	public void getOAuthRequestToken(final AuthRequestListener listener) {
		initOAuthClient();

		dispatcher.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					URI url = oauthClient.authorizeUrl();
					listener.onAuthRequestComplete(url.toString());
				} catch (Exception e) {
					listener.onAuthRequestFailed(e);
				}
			}
		});
	}

	public void updateStatus(final String message, final TwitterListener listener) {
		initOAuthClient();

		if (twitter == null) twitter = new Twitter(null, oauthClient);
		dispatcher.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					twitter.updateStatus(message);
					listener.onStatusUpdateComplete();
				} catch (Exception e) {
					listener.onStatusUpdateFailed(e);
				}
			}
		});
	}

	private void initOAuthClient() {
		if (oauthClient == null) {
			if (accessToken == null) {
				oauthClient = new OAuthSignpostClient(consumerKey, consumerSecret, "http://abcd.ef");
			} else {
				oauthClient = new OAuthSignpostClient(consumerKey, consumerSecret, accessToken.getToken(), accessToken.getTokenSecret());
			}
		}
	}
}
