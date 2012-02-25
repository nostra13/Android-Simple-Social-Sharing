/**
 * 
 */
package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @deprecated There are bugs on Twitter's end -- the messages returned by this
 *             stream may not include all the messages to a user. The results
 *             vary from user to user!
 *             <p>
 *             Connect to the streaming API.
 *             <p>
 *             This class picks up the following tweets: <br>
 * 
 *             - Tweets by you <br>
 *             - Tweets that mention you <br>
 *             - Tweets by people you follow IF
 *             {@link #setWithFollowings(boolean)} is true. <br>
 *             - Retweets of your messages. <br>
 *             - Retweets made by you.
 * 
 *             <p>
 *             Duplicate messages may be delivered when reconnecting to the
 *             Streaming API.
 * 
 *             TODO test out url-signing over header-signing -- c.f.
 *             http://groups
 *             .google.com/group/twitter-development-talk/browse_thread
 *             /thread/420c4b555198aa6c/f85e2507b7f65e39?pli=1
 * 
 *             "figured it out on my own. must use HTTP GET with OAuth params
 *             passed in URI string. Not mentioned in the documentation. Wasted
 *             many hours figuring out this stuff would be clarified if someone
 *             updated the docs and made some examples."
 * 
 * 
 * @author Daniel
 * @testedby {@link UserStreamTest}
 */
@Deprecated
public class UserStream extends AStream {

	boolean withFollowings;

	public UserStream(Twitter jtwit) {
		super(jtwit);
	}

	@Override
	HttpURLConnection connect2() throws IOException {
		String url = "https://userstream.twitter.com/2/user.json?delimited=length";
		Map<String, String> vars = InternalUtils.asMap("with",
				(withFollowings ? "followings" : "user"));
		HttpURLConnection con = client.connect(url, vars, true);
		return con;
	}

	/**
	 * Use the REST API to fill in: mentions of you. Missed you-follow-them
	 * events are automatically generated on reconnect.
	 */
	@Override
	void fillInOutages2(Twitter jtwit2, Outage outage)
			throws UnsupportedOperationException, TwitterException {
		// fetch
		if (withFollowings)
			// TODO pull in network activity
			throw new UnsupportedOperationException("TODO");
		// get mentions of you
		List<Status> mentions = jtwit2.getMentions();
		for (Status status : mentions) {
			if (tweets.contains(status)) {
				continue;
			}
			tweets.add(status);
		}
		// get your traffic
		List<Status> updates = jtwit2.getUserTimeline(jtwit2.getScreenName());
		for (Status status : updates) {
			if (tweets.contains(status)) {
				continue;
			}
			tweets.add(status);
		}
		// Missed follow events are sort of OK: the reconnect will update
		// friends
	}

	/**
	 * @return people who the user follows -- at the point when the stream last
	 *         connected.
	 */
	public Collection<Long> getFriends() {
		// TODO update the friends list from follow events??
		return friends;
	}

	/**
	 * @param withFollowings
	 *            if true, pick up all tweets by the people the user follows.
	 */
	public void setWithFollowings(boolean withFollowings) {
		assert !isConnected();
		this.withFollowings = withFollowings;
	}

}
