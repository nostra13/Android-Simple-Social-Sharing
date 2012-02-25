package com.nostra13.socialsharing.twitter;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public interface AuthRequestListener {

	void onAuthRequestComplete(String requestUrl);

	void onAuthRequestFailed(Exception e);
}
