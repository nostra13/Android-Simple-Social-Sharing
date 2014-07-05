package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringBufferInputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.AbstractOAuthConsumer;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.OAuthConsumer;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.basic.DefaultOAuthProvider;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.basic.HttpURLConnectionRequestAdapter;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.exception.OAuthException;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.http.HttpRequest;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;


/**
 * OAuth based login using Signpost (http://code.google.com/p/oauth-signpost/).
 * This is the "official" JTwitter OAuth support.
 * <p>
 * First download the Signpost jar and add it to your classpath, as it isn't
 * included in the JTwitter download.
 * <p>
 * Example Usage #1 (out-of-bounds, desktop based):
 * 
 * <pre>
 * <code>
 * 	OAuthSignpostClient client = new OAuthSignpostClient(JTWITTER_OAUTH_KEY, JTWITTER_OAUTH_SECRET, "oob");
 * 	Twitter jtwit = new Twitter("yourtwittername", client);
 * 	// open the authorisation page in the user's browser
 * 	// This is a convenience method for directing the user to client.authorizeUrl()
 * 	client.authorizeDesktop();
 * 	// get the pin
 * 	String v = client.askUser("Please enter the verification PIN from Twitter");
 * 	client.setAuthorizationCode(v);
 * 	// Optional: store the authorisation token details
 * 	Object accessToken = client.getAccessToken();
 * 	// use the API!
 * 	jtwit.setStatus("Messing about in Java");
 *  	</code>
 * </pre>
 * 
 * <p>
 * Example Usage #2 (using callbacks):<br>
 * If you can handle callbacks, then this can be streamlined. 
 * 
 * On Android, you can use Intents to launch a web page, & to catch the resulting
 * callback. 
 * On a desktop, you need a webserver and a servlet (eg. use Jetty or Tomcat) 
 * to handle callbacks.
 * <p>
 * Replace "oob" with your callback url. Direct the user to
 * client.authorizeUrl(). Twitter will then call your callback with the request
 * token and verifier (authorisation code).
 * 
 * <pre>
 * <code>
 * 	OAuthSignpostClient client = new OAuthSignpostClient(JTWITTER_OAUTH_KEY, JTWITTER_OAUTH_SECRET, myCallbackUrl);
 * 	Twitter jtwit = new Twitter("yourtwittername", client);
 * 	URI url = client.authorizeUrl();
 * 	// Direct the user to this url!
 * 	</code>
 * </pre>
 * 
 * Now we wait for the callback...
 * 
 * <pre>
 * <code>
 * 	HttpServletRequest request = from your servlet; 
 * 	// get the pin
 * 	String verifier = request.getParameter("oauth_verifier");
 * 	client.setAuthorizationCode(verifier);
 * 
 * 	// The client is now good for use. But wait: if you get an access token
 * 	// and secret, you can store them for next time:
 * 	String[] accessTokenAndSecret = client.getAccessToken();
 * 	// Then you can in future use
 * 	// OAuthSignpostClient client = new OAuthSignpostClient(APP_KEY, APP_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
 * 
 * 	// use the API!
 * 	jtwit.setStatus("Messing about in Java");
 *  	</code>
 * </pre>
 * 
 * <p>
 * There are alternative OAuth libraries you can use:
 * 
 * @see OAuthHttpClient
 * @see OAuthScribeClient
 * @author Daniel
 */
public class OAuthSignpostClient extends URLConnectionHttpClient implements
		IHttpClient, Serializable {

	/**
	 * Use with #setProvider() to make this a foursquare OAuth client
	 */
	private static final CommonsHttpOAuthProvider FOURSQUARE_PROVIDER = new CommonsHttpOAuthProvider(
			"https://api.twitter.com/oauth/request_token",
			"https://api.twitter.com/oauth/access_token",
			"https://api.twitter.com/oauth/authorize");

	/**
	 * This consumer key (and secret) allows you to get up and running fast.
	 * However you are strongly advised to register your own app at
	 * http://dev.twitter.com Then use your own key and secret. This will be
	 * less confusing for users, and it protects you incase the JTwitter key
	 * gets changed.
	 */
	public static final String JTWITTER_OAUTH_KEY = "Cz8ZLgitPR2jrQVaD6ncw";

	/**
	 * For use with {@link #JTWITTER_OAUTH_KEY}
	 */
	public static final String JTWITTER_OAUTH_SECRET = "9FFYaWJSvQ6Yi5tctN30eN6DnXWmdw0QgJMl7V6KGI";

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * <i>Convenience method for desktop apps only - does not work in
	 * Android</i>
	 * </p>
	 * 
	 * Opens a popup dialog asking the user to enter the verification code. (you
	 * would then call {@link #setAuthorizationCode(String)}). This is only
	 * relevant when using out-of-band instead of a callback-url. This is a
	 * convenience method -- you will probably want to build your own UI around
	 * this.
	 * <p>
	 * <i>This method requires Swing. It will not work on Android devices!</i>
	 * 
	 * @param question
	 *            e.g. "Please enter the authorisation code from Twitter"
	 * @return
	 */
	public static String askUser(String question) {
		// This cumbersome approach avoids importing Swing classes
		// It will create a runtime exception on Android
		// -- but will allow the rest of the class to be used.
		// JOptionPane.showInputDialog(question);
		try {
			Class<?> JOptionPaneClass = Class
					.forName("javax.swing.JOptionPane");
			Method showInputDialog = JOptionPaneClass.getMethod(
					"showInputDialog", Object.class);
			return (String) showInputDialog.invoke(null, question);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String accessToken;
	private String accessTokenSecret;
	private String callbackUrl;
	private OAuthConsumer consumer;
	private String consumerKey;
	private String consumerSecret;
	private CommonsHttpOAuthProvider provider;

	/**
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param callbackUrl
	 *            Servlet that will get the verifier sent to it, or "oob" for
	 *            out-of-band (user copies and pastes the pin to you)
	 */
	public OAuthSignpostClient(String consumerKey, String consumerSecret,
			String callbackUrl) {
		assert consumerKey != null && consumerSecret != null
				&& callbackUrl != null;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.callbackUrl = callbackUrl;
		init();
	}

	/**
	 * Use this if you already have an accessToken for the user. You can then go
	 * straight to using the API without having to authorise again.
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param accessToken
	 */
	public OAuthSignpostClient(String consumerKey, String consumerSecret,
			String accessToken, String accessTokenSecret) {
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessTokenSecret = accessTokenSecret;
		init();
	}

	/**
	 * Redirect the user's browser to Twitter's authorise page. You will need to
	 * collect the verifier pin - either from the callback servlet, or from the
	 * user (out-of-band).
	 * <p>
	 * <i>This method requires Swing. It will not work on Android!</i>
	 * 
	 * @see #authorizeUrl()
	 */
	@Deprecated
	// this is convenient for getting started, but probably you should build
	// your own GUI.
	public void authorizeDesktop() {
		URI uri = authorizeUrl();
		try {
			// This cumbersome approach avoids importing Swing classes
			// It will create a runtime exception on Android
			// -- but will allow the rest of the class to be used.
			// Desktop d = Desktop.getDesktop();
			Class<?> desktopClass = Class.forName("java.awt.Desktop");
			Method getDesktop = desktopClass.getMethod("getDesktop", (Class[]) null);
			Object d = getDesktop.invoke(null, (Object[]) null);
			// d.browse(uri);
			Method browse = desktopClass.getMethod("browse", URI.class);
			browse.invoke(d, uri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return url to direct the user to for authorisation. Send the user to
	 *         this url. They click "OK", then get redirected to your callback
	 *         url.
	 */
	public URI authorizeUrl() {
		try {
			String url = provider.retrieveRequestToken(consumer, callbackUrl);
			return new URI(url);
		} catch (Exception e) {
			// Why does this happen?
			throw new TwitterException(e);
		}
	}

	@Override
	public boolean canAuthenticate() {
		return consumer.getToken() != null;
	}

	@Override
	public IHttpClient copy() {
		OAuthSignpostClient c;
		if (accessToken != null) {
			c = new OAuthSignpostClient(consumerKey, consumerSecret,
					accessToken, accessTokenSecret);
			c.callbackUrl = callbackUrl;
		} else {
			c = new OAuthSignpostClient(consumerKey, consumerSecret,
					callbackUrl);
		}
		c.setTimeout(timeout);
		c.setRetryOnError(retryOnError);
		c.setMinRateLimit(minRateLimit);
		c.rateLimits.putAll(rateLimits);
		return c;
	}

	/**
	 * @return the access token and access token secret - if this client was
	 *         constructed with an access token, or has successfully
	 *         authenticated and got one. null otherwise.
	 */
	public String[] getAccessToken() {
		if (accessToken == null)
			return null;
		return new String[] { accessToken, accessTokenSecret };
	}

	@Override
	String getName() {
		// avoid returning null, cos there always is a user, we just don't know
		// their name
		return name == null ? "?user" : name;
	}

	private void init() {
		// The default consumer can't do post requests!
		// TODO override AbstractAuthConsumer.collectBodyParameters() which
		// would be more efficient
		consumer = new AbstractOAuthConsumer(consumerKey, consumerSecret) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected HttpRequest wrap(final Object request) {
				if (request instanceof HttpRequest)
					return (HttpRequest) request;
				return new HttpURLConnectionRequestAdapter(
						(HttpURLConnection) request);
			}
		};
		if (accessToken != null) {
			consumer.setTokenWithSecret(accessToken, accessTokenSecret);
		}
		provider = new CommonsHttpOAuthProvider(
				"https://api.twitter.com/oauth/request_token",
				"https://api.twitter.com/oauth/access_token",
				"https://api.twitter.com/oauth/authorize");
	}

	@Override
	public HttpURLConnection post2_connect(String uri, Map<String, String> vars)
			throws IOException, OAuthException {
		HttpURLConnection connection = (HttpURLConnection) new URL(uri)
				.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		connection.setReadTimeout(timeout);
		connection.setConnectTimeout(timeout);
		final String payload = post2_getPayload(vars);
		// needed for OAuthConsumer.collectBodyParameters() not to get upset
		HttpURLConnectionRequestAdapter wrapped = new HttpURLConnectionRequestAdapter(
				connection) {
			@Override
			public InputStream getMessagePayload() throws IOException {
				// SHould we use ByteArrayInputStream instead? With what
				// encoding?
				return new StringBufferInputStream(payload);
			}
		};
		// safetyCheck();
		consumer.sign(wrapped);

		// add the payload
		OutputStream os = connection.getOutputStream();
		os.write(payload.getBytes());
		close(os);
		// check connection & process the envelope
		processError(connection);
		processHeaders(connection);
		return connection;
	}

	@Override
	protected void setAuthentication(URLConnection connection, String name,
			String password) {
		// safetyCheck();
		try {
			// sign the request
			consumer.sign(connection);
		} catch (OAuthException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Set the authorisation code (aka the verifier).
	 * 
	 * @param verifier
	 *            a pin code which Twitter gives the user (with the oob method),
	 *            or which you get from the callback response as the parameter
	 *            "oauth_verifier".
	 * @throws RuntimeException
	 *             throws an exception if the verifier is invalid
	 */
	public void setAuthorizationCode(String verifier) throws TwitterException {
		assert accessToken == null : "This JTwitter already has an access token and is ready for use.";
		try {
			provider.retrieveAccessToken(consumer, verifier);
			accessToken = consumer.getToken();
			accessTokenSecret = consumer.getTokenSecret();
		} catch (Exception e) {
			if (e.getMessage().contains("401")) {
				throw new TwitterException.E401(e.getMessage());
			}
			throw new TwitterException(e);
		}
	}

	public void setFoursquareProvider() {
		setProvider(FOURSQUARE_PROVIDER);
	}

	/**
	 * Unlike the base class {@link URLConnectionHttpClient}, this does not set
	 * name by default. But you can set it for nicer error messages.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set to Twitter settings by default. This method lets you override that.
	 * 
	 * @param provider
	 */
	public void setProvider(CommonsHttpOAuthProvider provider) {
		this.provider = provider;
	}

}
