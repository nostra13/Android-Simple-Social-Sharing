package com.nostra13.socialsharing.facebook;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.nostra13.socialsharing.Constants;
import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.AsyncFacebookRunner;
import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.DialogError;
import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.Facebook;
import com.nostra13.socialsharing.facebook.extpack.com.facebook.android.FacebookError;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class FacebookFacade {

	private static final String TAG = FacebookFacade.class.getSimpleName();

	private Activity context;
	private Facebook facebook;
	private AsyncFacebookRunner asyncFacebook;

	public FacebookFacade(Activity context, String facebookAppId) {
		this.context = context;
		facebook = new Facebook(facebookAppId);
		FacebookSessionStore.restore(facebook, context);
		asyncFacebook = new AsyncFacebookRunner(facebook);
	}

	public boolean isAuthorized() {
		return facebook.isSessionValid();
	}

	public void authorize() {
		authorize(null);
	}

	public void authorize(final AuthListener authListener) {
		facebook.authorize(context, Constants.FACEBOOK_PERMISSIONS, Facebook.FORCE_DIALOG_AUTH, new FacebookAuthListener() {
			@Override
			public void onFacebookError(FacebookError e) {
				if (authListener != null) authListener.onAuthFail(e.getMessage());
				super.onFacebookError(e);
			}

			@Override
			public void onError(DialogError e) {
				if (authListener != null) authListener.onAuthFail(e.getMessage());
				super.onError(e);
			}

			@Override
			public void onComplete(Bundle values) {
				FacebookSessionStore.save(facebook, context);
				if (authListener != null) authListener.onAuthSucceed();
				super.onComplete(values);
			}
		});
	}

	public void logout() {
		asyncFacebook.logout(context, new FacebookLogoutListener() {
			@Override
			public void onComplete(final String response, final Object state) {
				super.onComplete(response, state);
				FacebookSessionStore.clear(context);
			}
		});
	}

	public void publishMessage(String message) {
		publishMessage(message, null, null, null);
	}

	public void publishMessage(String message, String link, String linkName, String linkDescription) {
		publishMessage(message, link, linkName, linkDescription, null);
	}

	public void publishMessage(String message, String link, String linkName, String linkDescription, String pictureUrl) {
		publishMessage(message, link, linkName, linkDescription, pictureUrl, null);
	}

	public void publishMessage(String message, String link, String linkName, String linkDescription, String pictureUrl, Map<String, String> actions) {
		Bundle params = new Bundle();
		params.putString(RequestParameter.MESSAGE, message);
		if (link != null) {
			params.putString(RequestParameter.LINK, link);
		}
		if (linkName != null) {
			params.putString(RequestParameter.NAME, linkName);
		}
		if (linkDescription != null) {
			params.putString(RequestParameter.DESCRIPTION, linkDescription);
		}
		if (pictureUrl != null) {
			params.putString(RequestParameter.PICTURE, pictureUrl);
		}
		if (actions != null) {
			params.putString(RequestParameter.ACTIONS, buildActionsString(actions));
		}

		asyncFacebook.request("me/feed", params, "POST", new FacebookPostListener(), null);
	}

	private String buildActionsString(Map<String, String> actionsMap) {
		JSONObject actionsObject = new JSONObject();
		Set<Entry<String, String>> actionEntries = actionsMap.entrySet();
		for (Entry<String, String> actionEntry : actionEntries) {
			try {
				actionsObject.put(RequestParameter.NAME, actionEntry.getKey());
				actionsObject.put(RequestParameter.LINK, actionEntry.getValue());
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		return actionsObject.toString();
	}

	public void publishImage(byte[] imageData, String caption) {
		Bundle params = new Bundle();
		params.putString("method", "photos.upload");
		params.putString(RequestParameter.CAPTION, caption);
		params.putByteArray(RequestParameter.PICTURE, imageData);
		asyncFacebook.request(null, params, "POST", new FacebookPostListener(), null);
	}

	protected static class RequestParameter {
		public static final String MESSAGE = "message";
		public static final String PICTURE = "picture";
		public static final String CAPTION = "caption";
		public static final String LINK = "link";
		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String ACTIONS = "actions";
	}
}
