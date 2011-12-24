package com.nostra13.socialsharing.facebook;

import java.util.LinkedList;

import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.common.LogoutListener;
import com.nostra13.socialsharing.common.PostListener;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class FacebookEvents {

	private static LinkedList<AuthListener> authListeners = new LinkedList<AuthListener>();
	private static LinkedList<LogoutListener> logoutListeners = new LinkedList<LogoutListener>();
	private static LinkedList<PostListener> postListeners = new LinkedList<PostListener>();

	/**
	 * Associate the given listener with this Facebook object. The listener's callback interface will be invoked when
	 * authentication events occur.
	 * 
	 * @param listener
	 *            The callback object for notifying the application when auth events happen.
	 */
	public static void addAuthListener(AuthListener listener) {
		authListeners.add(listener);
	}

	/**
	 * Remove the given listener from the list of those that will be notified when authentication events occur.
	 * 
	 * @param listener
	 *            The callback object for notifying the application when auth events happen.
	 */
	public static void removeAuthListener(AuthListener listener) {
		authListeners.remove(listener);
	}

	/**
	 * Associate the given listener with this Facebook object. The listener's callback interface will be invoked when
	 * logout occurs.
	 * 
	 * @param listener
	 *            The callback object for notifying the application when log out starts and finishes.
	 */
	public static void addLogoutListener(LogoutListener listener) {
		logoutListeners.add(listener);
	}

	/**
	 * Remove the given listener from the list of those that will be notified when logout occurs.
	 * 
	 * @param listener
	 *            The callback object for notifying the application when log out starts and finishes.
	 */
	public static void removeLogoutListener(LogoutListener listener) {
		logoutListeners.remove(listener);
	}

	/**
	 * Associate the given listener with this Facebook object. The listener's callback interface will be invoked when
	 * post publishing occurs.
	 * 
	 * @param listener
	 *            The callback object for notifying the application when post was published (or publishing was failed).
	 */
	public static void addPostListener(PostListener listener) {
		postListeners.add(listener);
	}

	/**
	 * Remove the given listener from the list of those that will be notified when post publishing occurs.
	 * 
	 * @param listener
	 *            The callback object for notifying the application when post was published (or publishing was failed).
	 */
	public static void removePostListener(PostListener listener) {
		postListeners.remove(listener);
	}

	static void onLoginSuccess() {
		for (AuthListener listener : authListeners) {
			listener.onAuthSucceed();
		}
	}

	static void onLoginError(String error) {
		for (AuthListener listener : authListeners) {
			listener.onAuthFail(error);
		}
	}

	static void onLogoutComplete() {
		for (LogoutListener l : logoutListeners) {
			l.onLogoutComplete();
		}
	}

	static void onPostPublished() {
		for (PostListener l : postListeners) {
			l.onPostPublished();
		}
	}

	static void onPostPublishingFailed() {
		for (PostListener l : postListeners) {
			l.onPostPublishingFailed();
		}
	}
}
