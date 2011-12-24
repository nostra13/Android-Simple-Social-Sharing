package com.nostra13.socialsharing.twitter;

import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import android.app.Activity;
import android.content.Context;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class TwitterFacade {

	private Context context;

	private AsyncTwitter asyncTwitter;
	private TwitterDialog dialog;

	public TwitterFacade(Activity context) {
		this.context = context;
		asyncTwitter = AsyncTwitterFactory.getSingleton();
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
	}

	public void publishMessage(String message) {
		asyncTwitter.updateStatus(message);
	}
}
