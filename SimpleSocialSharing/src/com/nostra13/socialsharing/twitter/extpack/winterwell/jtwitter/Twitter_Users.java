package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONArray;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.TwitterException.E403;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.TwitterException.SuspendedUser;



/**
 * API calls relating to users and relationships (the social network). Use
 * {@link Twitter#users()} to get one of these objects.
 * <p>
 * Conceptually, this is an extension of {@link Twitter}. The methods are here
 * because Twitter was getting crowded.
 * 
 * @author Daniel
 */
public class Twitter_Users {

	private final IHttpClient http;

	private final Twitter jtwit;

	Twitter_Users(Twitter jtwit) {
		this.jtwit = jtwit;
		http = jtwit.getHttpClient();
	}

	/**
	 * blocks/create: Blocks screenName from following the authenticating user.
	 * In addition the blocked user will not show in the authenticating users
	 * mentions or timeline (unless retweeted by another user). If a follow or
	 * friend relationship exists it is destroyed.
	 * 
	 * @param screenName
	 * @return info on the (now blocked) user
	 * @see #unblock(String)
	 */
	public User block(String screenName) {
		HashMap vars = new HashMap();
		vars.put("screen_name", screenName);
		// Returns if the authenticating user is blocking a target user.
		// Will return the blocked user's object if a block exists, and error
		// with
		// a HTTP 404 response code otherwise.
		String json = http.post(jtwit.TWITTER_URL + "/blocks/create.json",
				vars, true);
		return InternalUtils.user(json);
	}

	/**
	 * Common backend for {@link #bulkShow(List)} and
	 * {@link #bulkShowById(List)}. Works in batches of 100.
	 * <p>
	 * This will throw exceptions from the 1st page of results, but swallow them
	 * from subsequent pages (which are likely to be rate limit errors).
	 * <p>
	 * Suspended bot accounts seem to just get ignored.
	 * 
	 * @param stringOrNumber
	 * @param screenNamesOrIds
	 */
	List<User> bulkShow2(String apiMethod, Class stringOrNumber,
			Collection screenNamesOrIds) {
		int batchSize = 100;
		ArrayList<User> users = new ArrayList<User>(screenNamesOrIds.size());
		List _screenNamesOrIds = screenNamesOrIds instanceof List ? (List) screenNamesOrIds
				: new ArrayList(screenNamesOrIds);
		for (int i = 0; i < _screenNamesOrIds.size(); i += batchSize) {
			int last = i + batchSize;
			String names = InternalUtils.join(_screenNamesOrIds, i, last);
			String var = stringOrNumber == String.class ? "screen_name"
					: "user_id";
			Map<String, String> vars = InternalUtils.asMap(var, names);
			try {
				String json = http.getPage(jtwit.TWITTER_URL + apiMethod, vars,
						http.canAuthenticate());
				List<User> usersi = User.getUsers(json);
				users.addAll(usersi);
			} catch (TwitterException e) {
				// Stop here.
				// Don't normally throw an exception so we don't waste the
				// results we have.
				if (users.size() == 0)
					throw e;
				break;
			}
		}
		return users;
	}

	/**
	 * Start following a user.
	 * 
	 * @param username
	 *            Required. The ID or screen name of the user to befriend.
	 * @return The befriended user, or null if (a) they were already being
	 *         followed, or (b) they protect their tweets & you already
	 *         requested to follow them.
	 * @throws TwitterException
	 *             if the user does not exist or has been suspended.
	 * @see #stopFollowing(String)
	 */
	public User follow(String username) throws TwitterException {
		if (username == null)
			throw new NullPointerException();
		if (username.equals(jtwit.getScreenName()))
			throw new IllegalArgumentException("follow yourself makes no sense");
		String page = null;
		try {
			Map<String, String> vars = InternalUtils.asMap("screen_name",
					username);
			page = http.post(jtwit.TWITTER_URL + "/friendships/create.json",
					vars, true);
			// is this needed? doesn't seem to fix things
			// http.getPage(jtwit.TWITTER_URL+"/friends", null, true);
			return new User(new JSONObject(page), null);
		} catch (SuspendedUser e) {
			throw e;
		} catch (TwitterException.Repetition e) {
			return null;
		} catch (E403 e) {
			// check if we've tried to follow someone we're already following
			try {
				if (isFollowing(username))
					return null;
			} catch (TwitterException e2) {
				// no extra info then
			}
			throw e;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(page, e);
		}
	}

	/**
	 * Convenience for {@link #follow(String)}
	 * 
	 * @param user
	 * @return fresh user object, or null if (a) they were already being
	 *         followed, or (b) they protect their tweets & you already
	 *         requested to follow them.
	 */
	public User follow(User user) {
		return follow(user.screenName);
	}

	/**
	 * @return an array of numeric user ids the authenticating user is blocking.
	 *         Use {@link #showById(Collection)} if you want to convert thse
	 *         into User objects.
	 */
	public List<Number> getBlockedIds() {
		String json = http.getPage(jtwit.TWITTER_URL
				+ "/blocks/blocking/ids.json", null, true);
		try {
			JSONArray arr = new JSONArray(json);
			List<Number> ids = new ArrayList(arr.length());
			for (int i = 0, n = arr.length(); i < n; i++) {
				ids.add(arr.getLong(i));
			}
			return ids;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Returns a list of the users currently featured on the site with their
	 * current statuses inline.
	 * <p>
	 * Note: This is no longer part of the Twitter API. Support is provided via
	 * other methods.
	 */
	public List<User> getFeatured() throws TwitterException {
		List<User> users = new ArrayList<User>();
		List<Status> featured = jtwit.getPublicTimeline();
		for (Status status : featured) {
			User user = status.getUser();
			users.add(user);
		}
		return users;
	}

	/**
	 * Returns the IDs of the authenticating user's followers.
	 * 
	 * @throws TwitterException
	 */
	public List<Number> getFollowerIDs() throws TwitterException {
		return getUserIDs(jtwit.TWITTER_URL + "/followers/ids.json", null);
	}

	/**
	 * Returns the IDs of the specified user's followers.
	 * 
	 * @param The
	 *            screen name of the user whose followers are to be fetched.
	 * @throws TwitterException
	 */
	public List<Number> getFollowerIDs(String screenName)
			throws TwitterException {
		return getUserIDs(jtwit.TWITTER_URL + "/followers/ids.json", screenName);
	}

	/**
	 * Returns the authenticating user's (latest) followers, each with current
	 * status inline. Occasionally contains duplicates.
	 * 
	 * @deprecated Twitter advise using {@link #getFollowerIDs()} and
	 *             {@link #show(Number)}
	 */
	@Deprecated
	public List<User> getFollowers() throws TwitterException {
		return getUsers(jtwit.TWITTER_URL + "/statuses/followers.json", null);
	}

	/**
	 * 
	 * Returns the (latest 100) given user's followers, each with current status
	 * inline. Occasionally contains duplicates.
	 * 
	 * @param username
	 *            The screen name of the user for whom to request a list of
	 *            friends.
	 * @throws TwitterException
	 */

	public List<User> getFollowers(String username) throws TwitterException {
		return getUsers(jtwit.TWITTER_URL + "/statuses/followers.json",
				username);
	}

	/**
	 * Returns the IDs of the authenticating user's friends. (people who the
	 * user follows).
	 * 
	 * @throws TwitterException
	 */
	public List<Number> getFriendIDs() throws TwitterException {
		return getUserIDs(jtwit.TWITTER_URL + "/friends/ids.json", null);
	}

	/**
	 * Returns the IDs of the specified user's friends. Occasionally contains
	 * duplicates.
	 * 
	 * @param The
	 *            screen name of the user whose friends are to be fetched.
	 * @throws TwitterException
	 */
	public List<Number> getFriendIDs(String screenName) throws TwitterException {
		return getUserIDs(jtwit.TWITTER_URL + "/friends/ids.json", screenName);
	}

	/**
	 * Returns the authenticating user's (latest 100) friends, each with current
	 * status inline. NB - friends are people who *you* follow. Occasionally
	 * contains duplicates.
	 * <p>
	 * Note that there seems to be a small delay from Twitter in updates to this
	 * list.
	 * 
	 * @throws TwitterException
	 * @see #getFriendIDs()
	 * @see #isFollowing(String)
	 * @deprecated Twitter advise you to use {@link #getFriendIDs()} with
	 *             {@link Twitter_Users#showById(List)} instead.
	 */
	@Deprecated
	public List<User> getFriends() throws TwitterException {
		return getUsers(jtwit.TWITTER_URL + "/statuses/friends.json", null);
	}

	/**
	 * 
	 * Returns the (latest 100) given user's friends, each with current status
	 * inline. Occasionally contains duplicates.
	 * 
	 * @param username
	 *            The screen name of the user for whom to request a list of
	 *            friends.
	 * @throws TwitterException
	 */
	public List<User> getFriends(String username) throws TwitterException {
		return getUsers(jtwit.TWITTER_URL + "/statuses/friends.json", username);
	}

	/**
	 * Bulk-fetch relationship info by screen-name.
	 * 
	 * @param screenNames
	 *            Can be empty
	 * @return User objects which are mostly blank, but do have
	 *         {@link User#isFollowingYou()} and {@link User#isFollowedByYou()}
	 *         set (plus name, screenname and id).
	 * @see #getRelationshipInfoById(List)
	 */
	public List<User> getRelationshipInfo(List<String> screenNames) {
		if (screenNames.size() == 0)
			return Collections.EMPTY_LIST;
		List<User> users = bulkShow2("/friendships/lookup.json", String.class,
				screenNames);
		return users;
	}

	/**
	 * Bulk-fetch relationship info by user-id.
	 * 
	 * @param userIDs
	 *            Can be empty
	 * @return User objects which are mostly blank, but which have
	 *         {@link User#isFollowingYou()} and {@link User#isFollowedByYou()}
	 *         set (plus name, screenname and id).
	 * @see #getRelationshipInfo(List)
	 */
	public List<User> getRelationshipInfoById(List<? extends Number> userIDs) {
		if (userIDs.size() == 0)
			return Collections.EMPTY_LIST;
		List<User> users = bulkShow2("/friendships/lookup.json", Number.class,
				userIDs);
		return users;
	}

	/**
	 * Synonym for {@link #show(long)}. show is the Twitter API name, getUser
	 * feels more Java-like.
	 * 
	 * @param userId
	 *            The user-id of a user.
	 * @return the user info
	 * @see #getUser(String)
	 */
	public User getUser(long userId) {
		return show(userId);
	}

	/**
	 * Synonym for {@link #show(String)}. show is the Twitter API name, getUser
	 * feels more Java-like.
	 * 
	 * @param screenName
	 *            The screen name of a user.
	 * @return the user info
	 */
	public User getUser(String screenName) {
		return show(screenName);
	}

	/**
	 * 
	 * @param url
	 *            API method to call
	 * @param screenName
	 * @return twitter-id numbers for friends/followers of screenName Is
	 *         affected by {@link #maxResults}
	 */
	private List<Number> getUserIDs(String url, String screenName) {
		Long cursor = -1L;
		List<Number> ids = new ArrayList<Number>();
		Map<String, String> vars = InternalUtils.asMap("screen_name",
				screenName);
		while (cursor != 0 && !jtwit.enoughResults(ids)) {
			vars.put("cursor", String.valueOf(cursor));
			String json = http.getPage(url, vars, http.canAuthenticate());
			try {
				// it seems Twitter will occasionally return a raw array
				JSONArray jarr;
				if (json.charAt(0) == '[') {
					jarr = new JSONArray(json);
					cursor = 0L;
				} else {
					JSONObject jobj = new JSONObject(json);
					jarr = (JSONArray) jobj.get("ids");
					cursor = new Long(jobj.getString("next_cursor"));
				}
				for (int i = 0; i < jarr.length(); i++) {
					ids.add(jarr.getLong(i));
				}
			} catch (JSONException e) {
				throw new TwitterException.Parsing(json, e);
			}
		}
		return ids;
	}

	/**
	 * Low-level method for fetching e.g. your friends
	 * 
	 * @param url
	 * @param screenName
	 *            e.g. your screen-name
	 * @return
	 */
	private List<User> getUsers(String url, String screenName) {
		Map<String, String> vars = InternalUtils.asMap("screen_name",
				screenName);
		List<User> users = new ArrayList<User>();
		Long cursor = -1L;
		while (cursor != 0 && !jtwit.enoughResults(users)) {
			vars.put("cursor", cursor.toString());
			JSONObject jobj;
			try {
				jobj = new JSONObject(http.getPage(url, vars,
						http.canAuthenticate()));
				users.addAll(User.getUsers(jobj.getString("users")));
				cursor = new Long(jobj.getString("next_cursor"));
			} catch (JSONException e) {
				throw new TwitterException.Parsing(null, e);
			}
		}
		return users;
	}

	public boolean isBlocked(Long userId) {
		try {
			HashMap vars = new HashMap();
			vars.put("user_id", Long.toString(userId));
			// Returns if the authenticating user is blocking a target user.
			// Will return the blocked user's object if a block exists, and
			// error with
			// a HTTP 404 response code otherwise.
			String json = http.getPage(jtwit.TWITTER_URL
					+ "/blocks/exists.json", vars, true);
			return true;
		} catch (TwitterException.E404 e) {
			return false;
		}
	}

	public boolean isBlocked(String screenName) {
		try {
			HashMap vars = new HashMap();
			vars.put("screen_name", screenName);
			// Returns if the authenticating user is blocking a target user.
			// Will return the blocked user's object if a block exists, and
			// error with
			// a HTTP 404 response code otherwise.
			String json = http.getPage(jtwit.TWITTER_URL
					+ "/blocks/exists.json", vars, true);
			return true;
		} catch (TwitterException.E404 e) {
			return false;
		}
	}

	/**
	 * Is the authenticating user <i>followed by</i> userB?
	 * 
	 * @param userB
	 *            The screen name of a Twitter user.
	 * @return Whether or not the user is followed by userB.
	 */
	public boolean isFollower(String userB) {
		return isFollower(userB, jtwit.getScreenName());
	}

	/**
	 * @return true if followerScreenName <i>is</i> following followedScreenName
	 * 
	 * @throws TwitterException.E403
	 *             if one of the users has protected their updates and you don't
	 *             have access. This can be counter-intuitive (and annoying) at
	 *             times! Also throws E403 if one of the users has been
	 *             suspended (we use the {@link SuspendedUser} exception
	 *             sub-class for this).
	 * @throws TwitterException.E404
	 *             if one of the users does not exist
	 */
	public boolean isFollower(String followerScreenName,
			String followedScreenName) {
		assert followerScreenName != null && followedScreenName != null;
		try {
			Map vars = InternalUtils.asMap("user_a", followerScreenName,
					"user_b", followedScreenName);
			String page = http.getPage(jtwit.TWITTER_URL
					+ "/friendships/exists.json", vars, http.canAuthenticate());
			return Boolean.valueOf(page);
		} catch (TwitterException.E403 e) {
			if (e instanceof SuspendedUser)
				throw e;
			// Should this be a suspended user exception instead?
			// Let's ask Twitter
			// TODO check rate limits - only do if we have spare capacity
			String whoFirst = followedScreenName.equals(jtwit.getScreenName()) ? followerScreenName
					: followedScreenName;
			try {
				// this could throw a SuspendedUser exception
				show(whoFirst);
				String whoSecond = whoFirst.equals(followedScreenName) ? followerScreenName
						: followedScreenName;
				if (whoSecond.equals(jtwit.getScreenName()))
					throw e;
				show(whoSecond);
			} catch (TwitterException.RateLimit e2) {
				// ignore
			}
			// both shows worked?
			throw e;
		} catch (TwitterException e) {
			// FIXME investigating a weird new bug
			if (e.getMessage() != null
					&& e.getMessage().contains(
							"Two user ids or screen_names must be supplied"))
				throw new TwitterException("WTF? inputs: follower="
						+ followerScreenName + ", followed="
						+ followedScreenName + ", call-by="
						+ jtwit.getScreenName() + "; " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Does the authenticating user <i>follow</i> userB?
	 * 
	 * @param userB
	 *            The screen name of a Twitter user.
	 * @return Whether or not the user follows userB.
	 */
	public boolean isFollowing(String userB) {
		return isFollower(jtwit.getScreenName(), userB);
	}

	/**
	 * Convenience for {@link #isFollowing(String)}
	 * 
	 * @param user
	 */
	public boolean isFollowing(User user) {
		return isFollowing(user.screenName);
	}

	/**
	 * Switches off notifications for updates from the specified user <i>who
	 * must already be a friend</i>.
	 * 
	 * @param screenName
	 *            Stop getting notifications from this user, who must already be
	 *            one of your friends.
	 * @return the specified user
	 */
	public User leaveNotifications(String screenName) {
		Map<String, String> vars = InternalUtils.asMap("screen_name",
				screenName);
		String page = http.getPage(jtwit.TWITTER_URL
				+ "/notifications/leave.json", vars, true);
		try {
			return new User(new JSONObject(page), null);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(page, e);
		}
	}

	/**
	 * Enables notifications for updates from the specified user <i>who must
	 * already be a friend</i>.
	 * 
	 * @param username
	 *            Get notifications from this user, who must already be one of
	 *            your friends.
	 * @return the specified user
	 */
	public User notify(String username) {
		Map<String, String> vars = InternalUtils.asMap("screen_name", username);
		String page = http.getPage(jtwit.TWITTER_URL
				+ "/notifications/follow.json", vars, true);
		try {
			return new User(new JSONObject(page), null);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(page, e);
		}
	}

	public User reportSpammer(String screenName) {
		HashMap vars = new HashMap();
		vars.put("screen_name", screenName);
		// Returns if the authenticating user is blocking a target user.
		// Will return the blocked user's object if a block exists, and error
		// with
		// a HTTP 404 response code otherwise.
		String json = http.post(jtwit.TWITTER_URL + "/report_spam.json", vars,
				true);
		return InternalUtils.user(json);
	}

	/**
	 * Warning: there is a bug within twitter.com which means that
	 * location-based searches are treated as OR. E.g. "John near:Scotland" will
	 * happily return "Andrew from Aberdeen" :(
	 * <p>
	 * Unlike tweet search, this method does not support any operators. Only the
	 * first 1000 matches are available.
	 * <p>
	 * Does not do paging-to-max-results. But does support using
	 * {@link #setPageNumber(Integer)}, and {@link #setMaxResults(int)} for less
	 * than the standard 20.
	 * 
	 * @param searchTerm
	 * @return
	 */
	public List<User> searchUsers(String searchTerm) {
		assert searchTerm != null;
		Map<String, String> vars = InternalUtils.asMap("q", searchTerm);
		if (jtwit.pageNumber != null) {
			vars.put("page", jtwit.pageNumber.toString());
		}
		if (jtwit.count != null && jtwit.count < 20) {
			vars.put("per_page", String.valueOf(jtwit.count));
		}
		// yes, it requires authentication
		String json = http.getPage(jtwit.TWITTER_URL + "/users/search.json",
				vars, true);
		List<User> users = User.getUsers(json);
		return users;
	}

	/**
	 * Lookup user info. This is done in batches of 100. Users can look up at
	 * most 1000 users in an hour.
	 * 
	 * @param screenNames
	 *            Can be empty (in which case we avoid wasting an API call)
	 * @return user objects for screenNames. Warning 1: This may be less than
	 *         the full set if Twitter returns an error part-way through (e.g.
	 *         you hit your rate limit). Warning 2: the ordering may be
	 *         different from the screenNames parameter
	 * @see #showById(List)
	 */
	public List<User> show(List<String> screenNames) {
		if (screenNames.size() == 0)
			return Collections.EMPTY_LIST;
		return bulkShow2("/users/lookup.json", String.class, screenNames);
	}

	/**
	 * Returns information of a given user, specified by user-id.
	 * 
	 * @param userId
	 *            The user-id of a user.
	 * @throws exception
	 *             if the user does not exist - or has been terminated (as
	 *             happens to spam bots).
	 */
	public User show(Number userId) {
		Map<String, String> vars = InternalUtils.asMap("user_id",
				userId.toString());
		String json = http.getPage(jtwit.TWITTER_URL + "/users/show.json",
				vars, http.canAuthenticate());
		try {
			User user = new User(new JSONObject(json), null);
			return user;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Returns information of a given user, specified by screen name.
	 * 
	 * @param screenName
	 *            The screen name of a user.
	 * @throws exception
	 *             if the user does not exist
	 * @throws SuspendedUser
	 *             if the user has been terminated (as happens to spam bots).
	 * @see #show(long)
	 */
	public User show(String screenName) throws TwitterException,
			TwitterException.SuspendedUser {
		Map vars = InternalUtils.asMap("screen_name", screenName);
		//Test Code Debugger at work - expected closures until 2012
		String json = "";
		try{ 
			json = http.getPage(jtwit.TWITTER_URL + "/users/show.json",
				vars, http.canAuthenticate());
		}
		catch (Exception e){
			//we get here?
			throw new TwitterException.E404("User " + screenName
					+ " does not seem to exist, their user account may have been removed from the service");
		}
		//Debuggers no longer at work
		if (json.length() == 0)
			throw new TwitterException.E404(screenName
					+ " does not seem to exist");
		try {
			User user = new User(new JSONObject(json), null);
			return user;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Lookup user info. Same as {@link #show(List)}, but works with Twitter
	 * user-ID numbers. Done in batches of 100, limited to 1000 an hour.
	 * 
	 * @param userIds
	 *            . Can be empty (in which case we avoid making a wasted API
	 *            call).
	 */
	public List<User> showById(Collection<? extends Number> userIds) {
		if (userIds.size() == 0)
			return Collections.EMPTY_LIST;
		return bulkShow2("/users/lookup.json", Number.class, userIds);
	}

	/**
	 * Destroy: Discontinues friendship with the user specified in the ID
	 * parameter as the authenticating user.
	 * 
	 * @param username
	 *            The screen name of the user with whom to discontinue
	 *            friendship.
	 * @return the un-friended user (if they were a friend), or null if the
	 *         method fails because the specified user was not a friend.
	 */
	public User stopFollowing(String username) {
		String page;
		try {
			Map<String, String> vars = InternalUtils.asMap("screen_name",
					username);
			page = jtwit.http.post(jtwit.TWITTER_URL
					+ "/friendships/destroy.json", vars, true);
			// ?? is this needed to make Twitter update its cache? doesn't seem
			// to fix things
			// http.getPage(jtwit.TWITTER_URL+"/friends", null, true);
		} catch (TwitterException e) {
			// were they a friend anyway?
			if (e.getMessage() != null
					&& e.getMessage().contains("not friends"))
				return null;
			// Something else went wrong
			throw e;
		}
		// outside the try-catch block in case there is a json exception
		try {
			User user = new User(new JSONObject(page), null);
			return user;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(page, e);
		}
	}

	/**
	 * Convenience for {@link #stopFollowing(String)}
	 * 
	 * @param user
	 * @return
	 */
	public User stopFollowing(User user) {
		return stopFollowing(user.screenName);
	}

	/**
	 * blocks/destroy: Un-blocks screenName for the authenticating user. Returns
	 * the un-blocked user when successful. If relationships existed before the
	 * block was instated, they will not be restored.
	 * 
	 * @param screenName
	 * @return the now un-blocked User
	 * @see #block(String)
	 */
	public User unblock(String screenName) {
		HashMap vars = new HashMap();
		vars.put("screen_name", screenName);
		// Returns if the authenticating user is blocking a target user.
		// Will return the blocked user's object if a block exists, and error
		// with
		// a HTTP 404 response code otherwise.
		String json = http.post(jtwit.TWITTER_URL + "/blocks/destroy.json",
				vars, true);
		return InternalUtils.user(json);
	}

	/**
	 * Does a user with the specified name or id exist?
	 * 
	 * @param screenName
	 *            The screen name or user id of the suspected user.
	 * @return False if the user doesn't exist or has been suspended, true
	 *         otherwise.
	 */
	public boolean userExists(String screenName) {
		try {
			// ?? possibly we should bypass the API:
			// request their twitter.com page & check for a 404
			show(screenName);
		} catch (TwitterException.SuspendedUser e) {
			return false;
		} catch (TwitterException.E404 e) {
			return false;
		}
		return true;
	}
}
