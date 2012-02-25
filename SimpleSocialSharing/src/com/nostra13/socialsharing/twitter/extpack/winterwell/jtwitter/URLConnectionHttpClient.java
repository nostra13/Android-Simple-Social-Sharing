package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.nostra13.socialsharing.twitter.extpack.lgpl.haustein.Base64Encoder;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.KRequestType;




/**
 * A simple http client that uses the built in URLConnection class.
 * <p>
 * Provides Twitter-focused error-handling, generating the right
 * TwitterException. Also has a retry-on-error mode which can help smooth out
 * Twitter's sometimes intermittent service. See
 * {@link #setRetryOnError(boolean)}.
 * 
 * @author Daniel Winterstein
 * 
 */
public class URLConnectionHttpClient implements Twitter.IHttpClient,
		Serializable {
	private static final int dfltTimeOutMilliSecs = 10 * 1000;

	private static final long serialVersionUID = 1L;

	/**
	 * Close a reader/writer/stream, ignoring any exceptions that result. Also
	 * flushes if there is a flush() method.
	 * 
	 * @param input
	 *            Can be null
	 */
	protected static void close(Closeable input) {
		if (input == null)
			return;
		// Flush (annoying that this is not part of Closeable)
		try {
			Method m = input.getClass().getMethod("flush");
			m.invoke(input);
		} catch (Exception e) {
			// Ignore
		}
		// Close
		try {
			input.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	private Map<String, List<String>> headers;

	int minRateLimit;

	protected String name;

	private final String password;

	final Map<KRequestType, RateLimit> rateLimits = new EnumMap(
			KRequestType.class);

	/**
	 * If true, will wait 1/2 second and make a 2nd request when presented with
	 * a server error.
	 */
	boolean retryOnError;

	protected int timeout = dfltTimeOutMilliSecs;

	private boolean htmlImpliesError = true;
	
	/**
	 * @param htmlImpliesError default is true. If true, an html response will
	 * be treated as a server error & generate a TwitterException.E50X 
	 */
	public void setHtmlImpliesError(boolean htmlImpliesError) {
		this.htmlImpliesError = htmlImpliesError;
	}

	public URLConnectionHttpClient() {
		this(null, null);
	}

	public URLConnectionHttpClient(String name, String password) {
		this.name = name;
		this.password = password;
		assert (name != null && password != null)
				|| (name == null && password == null);
	}

	@Override
	public boolean canAuthenticate() {
		return name != null && password != null;
	}

	@Override
	public HttpURLConnection connect(String url, Map<String, String> vars,
			boolean authenticate) throws IOException {
		if (vars != null && vars.size() != 0) {
			// add get variables
			StringBuilder uri = new StringBuilder(url);
			if (url.indexOf('?') == -1) {
				uri.append("?");
			} else if (!url.endsWith("&")) {
				uri.append("&");
			}
			for (Entry<String, String> e : vars.entrySet()) {
				if (e.getValue() == null) {
					continue;
				}
				String ek = InternalUtils.encode(e.getKey());
				assert !url.contains(ek + "=") : url + " " + vars;
				uri.append(ek + "=" + InternalUtils.encode(e.getValue()) + "&");
			}
			url = uri.toString();
		}
		// Setup a connection
		HttpURLConnection connection = (HttpURLConnection) new URL(url)
				.openConnection();
		// Authenticate
		if (authenticate) {
			setAuthentication(connection, name, password);
		}
		// To keep the search API happy - which wants either a referrer or a
		// user agent
		connection.setRequestProperty("User-Agent", "JTwitter/"
				+ Twitter.version);
		connection.setDoInput(true);
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.setConnectTimeout(timeout);
		// Open a connection
		processError(connection);
		processHeaders(connection);
		return connection;
	}

	@Override
	public IHttpClient copy() {
		URLConnectionHttpClient c = new URLConnectionHttpClient(name, password);
		c.setTimeout(timeout);
		c.setRetryOnError(retryOnError);
		c.setMinRateLimit(minRateLimit);
		c.rateLimits.putAll(rateLimits);
		return c;
	}

	protected final void disconnect(HttpURLConnection connection) {
		if (connection == null)
			return;
		try {
			connection.disconnect();
		} catch (Throwable t) {
			// ignore
		}
	}

	private String getErrorStream(HttpURLConnection connection) {
		try {
			return InternalUtils.toString(connection.getErrorStream());
		} catch (NullPointerException e) {
			return null;
		}
	}

	@Override
	public String getHeader(String headerName) {
		if (headers == null)
			return null;
		List<String> vals = headers.get(headerName);
		return vals == null || vals.isEmpty() ? null : vals.get(0);
	}

	String getName() {
		return name;
	}

	@Override
	public final String getPage(String url, Map<String, String> vars,
			boolean authenticate) throws TwitterException 
	{		
		assert url != null;
		InternalUtils.count(url);
		// This method handles the retry behaviour.
		try {
			// Do the actual work
			String json = getPage2(url, vars, authenticate);
			// ?? Test for and treat html as an error??
			if (htmlImpliesError && 
				(json.startsWith("<!DOCTYPE html") || json.startsWith("<html"))) {
				// whitelist: sometimes we do expect html
				if (url.startsWith("http://twitter.com")/*used by flush()*/) {
					// OK
				} else {
					String meat = InternalUtils.stripTags(json);
					throw new TwitterException.E50X(meat);
				}
			}
			return json;			
		} catch (SocketTimeoutException e) {
			if ( ! retryOnError) throw getPage2_ex(e, url);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return getPage2(url, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, url);
			}
		} catch (TwitterException.E50X e) {
			if ( ! retryOnError) throw getPage2_ex(e, url);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return getPage2(url, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, url);
			}
		} catch (IOException e) {
			throw new TwitterException.IO(e);
		} 
	}

	/**
	 * Called on error. What to throw? 
	 */
	private TwitterException getPage2_ex(Exception ex, String url) {
		if (ex instanceof TwitterException) return (TwitterException) ex;
		if (ex instanceof SocketTimeoutException) {
			return new TwitterException.Timeout(url);
		}
		if (ex instanceof IOException) {
			return new TwitterException.IO((IOException) ex);
		}
		return new TwitterException(ex);
	}
	/**
	 * Does the actual work for {@link #getPage(String, Map, boolean)}
	 * 
	 * @param url
	 * @param vars
	 * @param authenticate
	 * @return page if successful
	 * @throws IOException 
	 */
	private String getPage2(String url, Map<String, String> vars,
			boolean authenticate) throws IOException {
		HttpURLConnection connection = null;	
		try {
			connection = connect(url, vars, authenticate);
			InputStream inStream = connection.getInputStream();
			// Read in the web page
			String page = InternalUtils.toString(inStream);
			// Done
			return page;
		} finally {
			disconnect(connection);
		}		
	}

	@Override
	public RateLimit getRateLimit(KRequestType reqType) {
		return rateLimits.get(reqType);
	}

	@Override
	public final String post(String uri, Map<String, String> vars,
			boolean authenticate) throws TwitterException {		
		InternalUtils.count(uri);
		try {
			// do the actual work
			String json = post2(uri, vars, authenticate);
			// ?? Test for and treat html as an error??
			return json;
		} catch (TwitterException.E50X e) {
			if ( ! retryOnError) throw getPage2_ex(e, uri);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return post2(uri, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, uri);
			}
		} catch (SocketTimeoutException e) {
			if ( ! retryOnError) throw getPage2_ex(e, uri);
			try {
				// wait half a second before retrying
				Thread.sleep(500);
				return post2(uri, vars, authenticate);
			} catch (Exception e2) {
				throw getPage2_ex(e, uri);
			}
		} catch (Exception e) {
			throw getPage2_ex(e, uri);
		}
	}

	private String post2(String uri, Map<String, String> vars,
			boolean authenticate) throws Exception 
	{
		HttpURLConnection connection = null;
		try {
			connection = post2_connect(uri, vars);
			// Get the response
			String response = InternalUtils.toString(connection
					.getInputStream());
			return response;
		} finally {
			disconnect(connection);
		}
	}

	@Override
	public HttpURLConnection post2_connect(String uri, Map<String, String> vars)
			throws Exception {
		InternalUtils.count(uri);
		HttpURLConnection connection = (HttpURLConnection) new URL(uri)
				.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		// post methods are alwasy with authentication
		setAuthentication(connection, name, password);

		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		connection.setReadTimeout(timeout);
		connection.setConnectTimeout(timeout);
		// build the post body
		String payload = post2_getPayload(vars);
		connection.setRequestProperty("Content-Length", "" + payload.length());
		OutputStream os = connection.getOutputStream();
		os.write(payload.getBytes());
		close(os);
		// check connection & process the envelope
		processError(connection);
		processHeaders(connection);
		return connection;
	}

	protected String post2_getPayload(Map<String, String> vars) {
		if (vars == null || vars.isEmpty())
			return "";
		StringBuilder encodedData = new StringBuilder();

		for (String key : vars.keySet()) {
			String val = InternalUtils.encode(vars.get(key));
			encodedData.append(InternalUtils.encode(key));
			encodedData.append('=');
			encodedData.append(val);
			encodedData.append('&');
		}
		encodedData.deleteCharAt(encodedData.length() - 1);
		return encodedData.toString();
	}

	/**
	 * Throw an exception if the connection failed
	 * 
	 * @param connection
	 */
	 final void processError(HttpURLConnection connection) {
		try {
			int code = connection.getResponseCode();
			if (code == 200)
				return;
			URL url = connection.getURL();
			// any explanation?
			String error = processError2_reason(connection);
			// which error?
			if (code == 401) {
				if (error.contains("Basic authentication is not supported"))
					throw new TwitterException.UpdateToOAuth();
				throw new TwitterException.E401(error + "\n" + url + " ("
						+ (name == null ? "anonymous" : name) + ")");
			}
			if (code == 403) {
				// separate out the 403 cases
				processError2_403(url, error);
			}
			if (code == 404) {
				// user deleted?
				if (error != null && error.contains("deleted"))
					// Note: This is a 403 exception
					throw new TwitterException.SuspendedUser(error+ "\n"+ url);
				throw new TwitterException.E404(error + "\n" + url);
			}
			if (code == 406)
				throw new TwitterException.E406(error + "\n" + url);
			if (code == 413)
				throw new TwitterException.E413(error + "\n" + url);
			if (code == 416)
				throw new TwitterException.E416(error + "\n" + url);
			if (code == 420)
				throw new TwitterException.TooManyLogins(error + "\n" + url);
			if (code >= 500 && code < 600)
				throw new TwitterException.E50X(error + "\n" + url);

			// Over the rate limit?
			processError2_rateLimit(connection, code, error);

			// just report it as a vanilla exception
			throw new TwitterException(code + " " + error + " " + url);

		} catch (SocketTimeoutException e) {
			URL url = connection.getURL();
			throw new TwitterException.Timeout(timeout + "milli-secs for "
					+ url);
		} catch (ConnectException e) {
			// probably also a time out
			URL url = connection.getURL();
			throw new TwitterException.Timeout(url.toString());
		} catch (SocketException e) {
			// treat as a server error - because it probably is
			// (yes, it could also be an error at your end)
			throw new TwitterException.E50X(e.toString());
		} catch (IOException e) {
			throw new TwitterException(e);
		}
	}

	private String processError2_reason(HttpURLConnection connection) throws IOException {
		// Try for a helpful message from Twitter
		InputStream es = connection.getErrorStream();
		String errorPage = null;
		if (es != null) {
			try {
				errorPage = read(es);
				// is it json?			
				JSONObject je = new JSONObject(errorPage);
				String error = je.getString("error");
				if (error!=null && error.length() != 0) {
					return error;
				}
			} catch (Exception e) {
				// guess not!				
			}				
		}
		// normal error channels
		String error = connection.getResponseMessage();
		Map<String, List<String>> headers = connection.getHeaderFields();
		List<String> errorMessage = headers.get(null);
		if (errorMessage != null && !errorMessage.isEmpty()) {
			error += "\n" + errorMessage.get(0);
		}
		if (errorPage != null && errorPage.length() > 0) {
			error += "\n" + errorPage;
		}		
		return error;
	}

	private void processError2_403(URL url, String errorPage) {
		// is this a "too old" exception?
		String _name = name==null? "anon" : name;
		if (errorPage == null) {
			throw new TwitterException.E403(url + " (" + _name+ ")");
		}
		if (errorPage.contains("too old"))
			throw new TwitterException.BadParameter(errorPage + "\n" + url);
		// is this a suspended user exception?
		if (errorPage.contains("suspended"))
			throw new TwitterException.SuspendedUser(errorPage + "\n" + url);
		// this can be caused by looking up is-follower wrt a suspended
		// account
		if (errorPage.contains("Could not find"))
			throw new TwitterException.SuspendedUser(errorPage + "\n" + url);
		if (errorPage.contains("too recent"))
			throw new TwitterException.TooRecent(errorPage + "\n" + url);
		if (errorPage.contains("already requested to follow"))
			throw new TwitterException.Repetition(errorPage + "\n" + url);
		if (errorPage.contains("duplicate"))
			throw new TwitterException.Repetition(errorPage);
		if (errorPage.contains("unable to follow more people"))
			throw new TwitterException.FollowerLimit(name + " " + errorPage);
		if (errorPage.contains("application is not allowed to access"))
			throw new TwitterException.AccessLevel(name + " " + errorPage);
		throw new TwitterException.E403(errorPage + "\n" + url + " (" + _name+ ")");
	}

	private void processError2_rateLimit(HttpURLConnection connection,
			int code, String error) {
		boolean rateLimitExceeded = error.contains("Rate limit exceeded");
		if (rateLimitExceeded) {
			// store the rate limit info
			processHeaders(connection);
			throw new TwitterException.RateLimit(getName() + ": " + error);
		}
		// The Rate limiter can sometimes cause a 400 Bad Request
		if (code == 400) {
			try {
				String json = getPage(
						"http://twitter.com/account/rate_limit_status.json",
						null, password != null);
				JSONObject obj = new JSONObject(json);
				int hits = obj.getInt("remaining_hits");
				if (hits < 1)
					throw new TwitterException.RateLimit(error);
			} catch (Exception e) {
				// oh well
			}
		}
	}

	/**
	 * Cache headers for {@link #getHeader(String)}
	 * 
	 * @param connection
	 */
	protected final void processHeaders(HttpURLConnection connection) {
		headers = connection.getHeaderFields();
		updateRateLimits();
	}

	static String read(InputStream stream) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			final int bufSize = 8192; // this is the default BufferredReader
			// buffer size
			StringBuilder sb = new StringBuilder(bufSize);
			char[] cbuf = new char[bufSize];
			while (true) {
				int chars = reader.read(cbuf);
				if (chars == -1) {
					break;
				}
				sb.append(cbuf, 0, chars);
			}
			return sb.toString();
		} finally {
			stream.close();
		}
	}

	/**
	 * Set a header for basic authentication login.
	 */
	protected void setAuthentication(URLConnection connection, String name,
			String password) {
		assert name != null && password != null : "Authentication requested but no login details are set!";
		String token = name + ":" + password;
		String encoding = Base64Encoder.encode(token);
		connection.setRequestProperty("Authorization", "Basic " + encoding);
	}

	/**
	 * Use this to protect your Twitter API rate-limit. E.g. if you want to keep
	 * some credit in reserve for core activity. 0 by default. If set above
	 * zero, this JTwitter object will start pre-emptively throwing rate-limit
	 * exceptions when it gets down to the specified level.
	 */
	public void setMinRateLimit(int minRateLimit) {
		this.minRateLimit = minRateLimit;
	}

	/**
	 * False by default. Setting this to true switches on a robustness
	 * workaround: when presented with a 50X server error, the system will wait
	 * 1/2 a second and make a second attempt.
	 */
	public void setRetryOnError(boolean retryOnError) {
		this.retryOnError = retryOnError;
	}

	@Override
	public void setTimeout(int millisecs) {
		this.timeout = millisecs;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[name=" + name + ", password="
				+ (password == null ? "null" : "XXX") + "]";
	}

	/**
	 * {@link #processHeaders(HttpURLConnection)} MUST have been called first.
	 */
	void updateRateLimits() {
		for (KRequestType type : KRequestType.values()) {
			String limit = getHeader("X-" + type.rateLimit + "RateLimit-Limit");
			if (limit == null) {
				continue;
			}
			String remaining = getHeader("X-" + type.rateLimit
					+ "RateLimit-Remaining");
			String reset = getHeader("X-" + type.rateLimit + "RateLimit-Reset");
			rateLimits.put(type, new RateLimit(limit, remaining, reset));
			// Stop early to protect limits?
			// TODO move this code into Twitter so we can do it before a request
			if (minRateLimit > 0 && Integer.valueOf(limit) <= minRateLimit)
				throw new TwitterException.RateLimit(
						"Pre-emptive rate-limit block.");
		}
	}

}
