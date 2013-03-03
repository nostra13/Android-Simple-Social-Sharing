package com.nostra13.socialsharing.twitter;

import android.content.Context;

import com.nostra13.socialsharing.common.AuthListener;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class TwitterFacade {

	private Context context;
	private AsyncTwitter asyncTwitter;
	private CallbackTwitterDialog dialog;

	private String consumerKey;
	private String consumerSecret;

	public TwitterFacade(Context context, String consumerKey, String consumerSecret) {
		this.context = context;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		initTwitter();
	}

	private void initTwitter() {
		asyncTwitter = new AsyncTwitter(consumerKey, consumerSecret);
		dialog = new CallbackTwitterDialog(context, asyncTwitter);
		TwitterSessionStore.restore(asyncTwitter, context);
	}

	public boolean isAuthorized() {
		return TwitterSessionStore.isValidSession(context);
	}

	public void authorize() {
		authorize(null);
	}

	public void authorize(AuthListener authListener) {
		dialog.setAuthListener(authListener);
		dialog.show();
	}

	public void logout() {
		TwitterSessionStore.clear(context);
		initTwitter();
		TwitterEvents.onLogoutComplete();
	}

	public void publishMessage(String message) {
		asyncTwitter.updateStatus(message, new TwitterPostListener());
	}
}
