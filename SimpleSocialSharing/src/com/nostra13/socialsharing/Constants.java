package com.nostra13.socialsharing;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public final class Constants {
	private Constants() {
	}

	public static final String[] FACEBOOK_PERMISSIONS = new String[] {"publish_stream"};

	public static final class Preferences {
		public static final String FACEBOOK_KEY = "facebook-session";
		public static final String FACEBOOK_TOKEN = "access_token";
		public static final String FACEBOOK_EXPIRES = "expires_in";

		public static final String TWITTER_KEY = "twitter-session";
		public static final String TWITTER_TOKEN = "twitter_token";
		public static final String TWITTER_TOKEN_SECRET = "twitter_token_secret";
	}
}
