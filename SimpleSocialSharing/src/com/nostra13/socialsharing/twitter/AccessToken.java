package com.nostra13.socialsharing.twitter;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
class AccessToken {

	private String token;
	private String tokenSecret;

	AccessToken(String token, String tokenSecret) {
		this.token = token;
		this.tokenSecret = tokenSecret;
	}

	public String getToken() {
		return token;
	}

	public String getTokenSecret() {
		return tokenSecret;
	}
}
