package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.io.IOException;
import java.text.ParseException;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;


/**
 * A runtime exception for when Twitter requests don't work. All {@link Twitter}
 * methods can throw this.
 * <p>
 * This contains several subclasses which should be thrown to mark different
 * problems. Error handling is particularly important as Twitter tends to be a
 * bit flaky.
 * <p>
 * I believe unchecked exceptions are preferable to checked ones, because they
 * avoid the problems caused by swallowing exceptions. But if you don't like
 * runtime exceptions, just edit this class.
 * 
 * @author Daniel Winterstein
 */
public class TwitterException extends RuntimeException {

	/**
	 * Subclass of 403 thrown when you breach the access level of the app /
	 * oauth-token.
	 * 
	 * @see Twitter_Account#getAccessLevel()
	 */
	public static class AccessLevel extends E401 {
		private static final long serialVersionUID = 1L;

		public AccessLevel(String msg) {
			super(msg);
		}
	}

	/**
	 * Exception thrown when Twitter doesn't like a parameter. E.g. if you set a
	 * since_id which goes back too far, you'll see this.
	 * <p>
	 * This extends E403 because Twitter uses http code 403 (forbidden) to
	 * signal this.
	 */
	public static class BadParameter extends E403 {
		private static final long serialVersionUID = 1L;

		public BadParameter(String msg) {
			super(msg);
		}
	}

	/**
	 * An unauthorised exception. This is thrown (eg) if a password is wrong or
	 * a login is required. It may also be thrown when accessing a protected
	 * stream (you might expect an E403 there, but be prepared for an E401).
	 */
	public static class E401 extends E40X {
		private static final long serialVersionUID = 1L;

		public E401(String string) {
			super(string);
		}
	}

	/**
	 * A Forbidden exception. This is thrown if the authenticating used does not
	 * have the right to make a request. Possible causes: - Accessing a
	 * suspended account (ie. trying to look at messages from a spambot) -
	 * Accessing a protected stream - Repeatedly posting the same status - If
	 * search is passed a sinceId which is too old. Though the API documentation
	 * suggests a 404 should be thrown instead.
	 */
	public static class E403 extends E40X {
		private static final long serialVersionUID = 1L;

		public E403(String string) {
			super(string);
		}
	}

	/**
	 * Indicates a 404: resource does not exist error from Twitter. Note: *Used*
	 * to be thrown in relation to suspended users (e.g. spambots) These now get
	 * a 403, as of August 2010.
	 */
	public static class E404 extends E40X {
		private static final long serialVersionUID = 1L;

		public E404(String string) {
			super(string);
		}
	}

	/**
	 * Not Acceptable. One or more of the parameters are not suitable for the
	 * resource. The track parameter, for example, would throw this error if:
	 * 
	 * The track keyword is too long or too short. The bounding box specified is
	 * invalid. No predicates defined for filtered resource, for example,
	 * neither track nor follow parameter defined. Follow userid cannot be read.
	 */
	public static class E406 extends E40X {
		private static final long serialVersionUID = 1L;

		public E406(String string) {
			super(string);
		}
	}

	/**
	 * A user-error. This is sub-classed to provide more info.
	 * <p>
	 * This indicates an error in you request. You should catch E40X and deal
	 * with the cause. Don't just re-try -- it won't work.
	 */
	public static class E40X extends TwitterException {
		private static final long serialVersionUID = 1L;

		public E40X(String string) {
			super(string);
		}
	}

	/**
	 * Too Long. A parameter list is too long. The track parameter, for example,
	 * would throw this error if:
	 * 
	 * Too many track tokens specified for role; contact API team for increased
	 * access. Too many bounding boxes specified for role; contact API team for
	 * increased access. Too many follow userids specified for role; contact API
	 * team for increased access.
	 */
	public static class E413 extends E40X {
		private static final long serialVersionUID = 1L;

		public E413(String string) {
			super(string);
		}
	}

	/**
	 * Range Unacceptable. Possible reasons are:
	 * 
	 * Count parameter is not allowed in role. Count parameter value is too
	 * large.
	 */
	public static class E416 extends E40X {
		private static final long serialVersionUID = 1L;

		public E416(String string) {
			super(string);
		}
	}

	/**
	 * A code 50X error (e.g. 502) - indicating something went wrong at
	 * Twitter's end. The API equivalent of the Fail Whale. Usually retrying in
	 * a minute will fix this.
	 * <p>
	 * Note: some socket exceptions are assumed to be server errors - because
	 * they probably are - but could conceivably be caused by an error in your
	 * internet connection.
	 */
	public static class E50X extends TwitterException {
		private static final long serialVersionUID = 1L;

		public E50X(String string) {
			super(msg(string));
		}
		/**
		 * Sometimes Twitter sends a full web page by mistake.
		 */
		static String msg(String msg) {
			if (msg==null) return null;
			// strip any html tags
			// NB: this doesn't clean out script tags
			msg = InternalUtils.TAG_REGEX.matcher(msg).replaceAll("");
			msg = msg.replaceAll("\\s+", " ");
			if (msg.length() > 280) msg = msg.substring(0, 280) + "...";
			return msg;
		}
	}

	/**
	 * Subclass of 403 thrown when you follow too many people.
	 */
	public static class FollowerLimit extends E403 {
		private static final long serialVersionUID = 1L;

		public FollowerLimit(String msg) {
			super(msg);
		}
	}

	/**
	 * An IO exception, eg. a network issue. Call {@link #getCause()} to get the
	 * original IOException
	 */
	// ?? Should this extend E50X?
	public static class IO extends TwitterException {
		private static final long serialVersionUID = 1L;

		public IO(IOException e) {
			super(e);
		}

		@Override
		public IOException getCause() {
			return (IOException) super.getCause();
		}
	}

	/**
	 * Problems reading the JSON returned by Twitter. This should not normally
	 * occur! This indicates either a change in the API, or a bug in JTwitter.
	 */
	public static class Parsing extends TwitterException {
		private static final long serialVersionUID = 1L;

		/**
		 * Convenience to shorten a potentially long string.
		 */
		private static String clip(String json, int len) {
			return json == null ? null : json.length() <= len ? json : json
					.substring(len) + "...";
		}

		public Parsing(String json, JSONException e) {
			super((json==null? String.valueOf(e) : clip(json, 280))
					+causeLine(e), e);
		}

		/**
		 * Where did this error come from? an ultra mini stack-trace
		 * @param e
		 * @return " caused by ..." or ""
		 */
		private static String causeLine(JSONException e) {
			if (e==null) return "";
			StackTraceElement[] st = e.getStackTrace();
			for (StackTraceElement ste : st) {
				if (ste.getClassName().contains("JSON")) continue;
				return " caused by "+ste;
			}
			return "";
		}

		public Parsing(String date, ParseException e) {
			super(date, e);
		}
	}

	/**
	 * Indicates a rate limit error (i.e. you've over-used Twitter)
	 */
	public static class RateLimit extends TwitterException {
		private static final long serialVersionUID = 1L;

		public RateLimit(String string) {
			super(string);
		}
	}

	/**
	 * Subclass of 403 thrown when you try to do something twice, like post the
	 * same status. This is only thrown for immediate repetition. You may get a
	 * plain E403 instead for less blatant repetition.
	 */
	public static class Repetition extends E403 {
		private static final long serialVersionUID = 1L;

		public Repetition(String tweet) {
			super("Already tweeted! " + tweet);
		}
	}

	/**
	 * Exception thrown when trying to query a suspended account. Note that
	 * *deleted* accounts may generate an E404 instead.
	 * <p>
	 * This extends E403 because Twitter uses http code 403 (forbidden) to
	 * signal this.
	 */
	public static class SuspendedUser extends E403 {
		private static final long serialVersionUID = 1L;

		SuspendedUser(String msg) {
			super(msg);
		}
	}

	/**
	 * A timeout exception - probably caused by Twitter being overloaded.
	 */
	public static class Timeout extends TwitterException.E50X {
		private static final long serialVersionUID = 1L;

		public Timeout(String string) {
			super(string);
		}
	}

	/**
	 * A code 420 error indicates that the account has been logging in too
	 * often. This code will be the indication that the account has exceeded the
	 * per-account-per-application connection limit. This situation should be
	 * raised to the user, as they have been automatically and temporarily
	 * banned from User Streams for an excessive login rate. The user should be
	 * advised to shut down extra copies of the application, perhaps instances
	 * running on another device, to restore streaming access.
	 */
	public static class TooManyLogins extends E40X {
		private static final long serialVersionUID = 1L;

		public TooManyLogins(String string) {
			super(string);
		}
	}

	/**
	 * Thrown if you poll too frequently.
	 */
	public static class TooRecent extends E403 {
		private static final long serialVersionUID = 1L;

		TooRecent(String msg) {
			super(msg);
		}
	}

	/**
	 * Exception thrown if something goes wrong with twilonger.com integration
	 * for long tweets.
	 */
	public static class TwitLongerException extends TwitterException {
		private static final long serialVersionUID = 1L;

		public TwitLongerException(String string, String details) {
			super(string, details);
		}
	}

	/**
	 * Something has gone wrong. Occasionally Twitter behaves strangely.
	 */
	public static class Unexplained extends TwitterException {
		private static final long serialVersionUID = 1L;

		public Unexplained(String msg) {
			super(msg);
		}
	}

	/**
	 * Legacy exception thrown when trying to use basic auth instead of oauth.
	 */
	public static class UpdateToOAuth extends E401 {
		private static final long serialVersionUID = 1L;

		public UpdateToOAuth() {
			super(
					"You need to switch to OAuth. Twitter no longer support basic authentication.");
		}
	}

	private static final long serialVersionUID = 1L;

	private String additionalInfo = "";

	/**
	 * Wrap an exception as a TwitterException.
	 */
	TwitterException(Exception e) {
		super(e);
		// avoid gratuitous nesting of exceptions
		assert !(e instanceof TwitterException) : e;
	}

	/**
	 * @param string
	 */
	public TwitterException(String string) {
		super(string);
	}

	TwitterException(String msg, Exception e) {
		super(msg, e);
		// avoid gratuitous nesting of exceptions
		assert !(e instanceof TwitterException) : e;
	}

	public TwitterException(String string, String additionalInfo) {
		this(string);
		this.setAdditionalInfo(additionalInfo);
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
}
