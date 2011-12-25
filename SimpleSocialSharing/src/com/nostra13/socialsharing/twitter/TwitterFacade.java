package com.nostra13.socialsharing.twitter;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import android.content.Context;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class TwitterFacade {

	private Context context;
	private AsyncTwitter asyncTwitter;
	private TwitterDialog dialog;
	
	private String consumerKey;
	private String consumerSecret;

	public TwitterFacade(Context context, String consumerKey, String consumerSecret) {
		this.context = context;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		initTwitter();
	}
	
	private void initTwitter() {
		asyncTwitter = new AsyncTwitterFactory().getInstance();
		asyncTwitter.setOAuthConsumer(consumerKey, consumerSecret);
		asyncTwitter.addListener(new TwitterPostListener());
		dialog = new TwitterDialog(context, asyncTwitter);
		TwitterSessionStore.restore(asyncTwitter, context);
	}

	public boolean isAuthorized() {
		return TwitterSessionStore.isValidSession(context);
	}

	public void authorize() {
		dialog.show();
	}

	public void logout() {
		TwitterSessionStore.clear(context);
		initTwitter();
		TwitterEvents.onLogoutComplete();
	}

	public void publishMessage(String message) {
		asyncTwitter.updateStatus(message);
	}
}
