package com.nostra13.socialsharing.twitter;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
interface TwitterListener {

	void onStatusUpdateComplete();

	void onStatusUpdateFailed(Exception e);
}
