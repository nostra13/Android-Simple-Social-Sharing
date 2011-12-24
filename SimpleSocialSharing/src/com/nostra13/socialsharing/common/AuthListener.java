package com.nostra13.socialsharing.common;

/**
 * Callback interface for authorization events.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public interface AuthListener {

	/**
	 * Called when a auth flow completes successfully and a valid OAuth Token was received.
	 * 
	 * Executed by the thread that initiated the authentication.
	 * 
	 * API requests can now be made.
	 */
	public void onAuthSucceed();

	/**
	 * Called when a login completes unsuccessfully with an error.
	 * 
	 * Executed by the thread that initiated the authentication.
	 */
	public void onAuthFail(String error);
}
