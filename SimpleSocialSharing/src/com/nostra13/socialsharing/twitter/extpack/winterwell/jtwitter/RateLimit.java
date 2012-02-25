package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.util.Date;

/**
 * Info on your Twitter API usage - how many calls do you have to use?
 * 
 * @author daniel
 */
public final class RateLimit {
	/*
	 * We use lazy parsing for efficiency (most of these objects will never be
	 * examined).
	 */

	private String limit;
	private String remaining;
	private String reset;

	public RateLimit(String limit, String remaining, String reset) {
		this.limit = limit;
		this.remaining = remaining;
		this.reset = reset;
	}

	public int getLimit() {
		return Integer.valueOf(limit);
	}

	public int getRemaining() {
		return Integer.valueOf(remaining);
	}

	/**
	 * @return The date at which the limit will be reset.
	 */
	public Date getReset() {
		return InternalUtils.parseDate(reset);
	}

	/**
	 * @return true if the reset time has passed, so this rate limit no longer
	 *         applies.
	 */
	public boolean isOutOfDate() {
		return getReset().getTime() < System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return remaining;
	}

	/**
	 * Wait until the reset date. This will put the thread to sleep until the
	 * reset date (regardless of whether you still have remaining calls or not).
	 * Does nothing if the reset date has passed.
	 */
	public void waitForReset() {
		Long r = Long.valueOf(reset);
		long now = System.currentTimeMillis();
		long wait = r - now;
		if (wait < 0)
			return;
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			// wrap this for convenience??
			throw new TwitterException(e);
		}
	}
}
