package com.nostra13.socialsharing.twitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.nostra13.socialsharing.Constants.Preferences;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
final class TwitterSessionStore {

	private TwitterSessionStore() {
	}

	public static boolean save(AccessToken session, Context context) {
		Editor editor = context.getSharedPreferences(Preferences.TWITTER_KEY, Context.MODE_PRIVATE).edit();
		editor.putString(Preferences.TWITTER_TOKEN, session.getToken());
		editor.putString(Preferences.TWITTER_TOKEN_SECRET, session.getTokenSecret());
		return editor.commit();
	}

	public static boolean restore(AsyncTwitter session, Context context) {
		SharedPreferences savedSession = context.getSharedPreferences(Preferences.TWITTER_KEY, Context.MODE_PRIVATE);

		boolean restoredSuccessfully = false;
		if (isValidSession(context)) {
			String token = savedSession.getString(Preferences.TWITTER_TOKEN, null);
			String tokenSecret = savedSession.getString(Preferences.TWITTER_TOKEN_SECRET, null);
			AccessToken accessToken = new AccessToken(token, tokenSecret);
			session.setOAuthAccessToken(accessToken);
			restoredSuccessfully = true;
		}
		return restoredSuccessfully;
	}

	public static void clear(Context context) {
		Editor editor = context.getSharedPreferences(Preferences.TWITTER_KEY, Context.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}

	public static boolean isValidSession(Context context) {
		SharedPreferences savedSession = context.getSharedPreferences(Preferences.TWITTER_KEY, Context.MODE_PRIVATE);
		String token = savedSession.getString(Preferences.TWITTER_TOKEN, null);
		String tokenSecret = savedSession.getString(Preferences.TWITTER_TOKEN_SECRET, null);
		return token != null && tokenSecret != null;
	}
}
