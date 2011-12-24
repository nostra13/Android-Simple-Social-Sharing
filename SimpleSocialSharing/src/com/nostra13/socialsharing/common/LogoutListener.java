package com.nostra13.socialsharing.common;

/**
 * Callback interface for logout events.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public interface LogoutListener {
	/**
	 * Called when the session information has been cleared. UI should be updated to reflect logged-out state.
	 * 
	 * Executed by the thread that initiated the logout.
	 */
	public void onLogoutComplete();
}