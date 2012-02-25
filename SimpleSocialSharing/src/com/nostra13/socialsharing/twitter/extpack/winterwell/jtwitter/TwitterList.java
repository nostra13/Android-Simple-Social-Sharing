package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONArray;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;



/**
 * A Twitter list, which uses lazy-fetching of its members.
 * <p>
 * The methods of this object will call Twitter when they need to, and store the
 * results. E.g. the first call to {@link #size()} might require a call to
 * Twitter, but subsequent calls will not.
 * <p>
 * WARNING: Twitter only returns list members in batches of 20. So reading a
 * large list can be slow and use quite a few calls to Twitter.
 * <p>
 * To find out what lists you or another user has, see
 * {@link Twitter#getLists()} and {@link Twitter#getLists(String)}.<br>
 * To find out what lists you or another user are *in*, see
 * {@link Twitter#getListsContainingMe()} and
 * {@link Twitter#getListsContaining(String, boolean)}.
 * 
 * @see Twitter
 * @author daniel
 * 
 */
public class TwitterList extends AbstractList<User> {

	/**
	 * A lazy-loading list viewer. This will fetch details from Twitter when you
	 * call it's methods. This is for access to an existing list - it does NOT
	 * create a new list on Twitter.
	 * 
	 * @param ownerScreenName
	 * 
	 * @param owner
	 *            .screenName The Twitter screen-name for the list's owner.
	 * @param slug
	 *            The list's name. Technically the slug and the name needn't be
	 *            the same, but they usually are.
	 * @param jtwit
	 *            a JTwitter object (this must be able to authenticate).
	 * @throws Twitter.Exception.E404
	 *             if the list does not exist
	 */
	public static TwitterList get(String ownerScreenName, String slug,
			Twitter jtwit) {
		return new TwitterList(ownerScreenName, slug, jtwit);
	}

	private boolean _private;

	/**
	 * cursor for paging through the members of the list
	 */
	private long cursor = -1;

	private String description;

	/**
	 * The same client as the JTwitter object used in the constructor.
	 */
	private final IHttpClient http;

	private Number id;

	private final Twitter jtwit;

	private int memberCount = -1;

	private String name;

	/**
	 * never null (but may be a dummy object)
	 */
	private User owner;

	private String slug;

	private int subscriberCount;

	private final List<User> users = new ArrayList<User>();

	/**
	 * Used by {@link Twitter#getLists(String)}
	 * 
	 * @param json
	 * @param jtwit
	 * @throws JSONException
	 */
	TwitterList(JSONObject json, Twitter jtwit) throws JSONException {
		this.jtwit = jtwit;
		this.http = jtwit.getHttpClient();
		init2(json);
	}

	/**
	 * A lazy-loading list viewer. This will fetch some details here, but the
	 * list of members will be loaded from Twitter on demand (to minimise the
	 * API calls). <b>This is for access to an existing list - it does NOT
	 * create a new list on Twitter.</b>
	 * 
	 * @see #TwitterList(String, Twitter, boolean, String) which creates new
	 *      lists.
	 * 
	 * @param owner
	 *            .screenName The Twitter screen-name for the list's owner.
	 * @param slug
	 *            The list's name. Technically the slug and the name needn't be
	 *            the same, but they usually are.
	 * @param jtwit
	 *            a JTwitter object (this must be able to authenticate).
	 * @throws Twitter.Exception.E404
	 *             if the list does not exist
	 * @deprecated Due to the potential for confusion with
	 *             {@link #TwitterList(String, Twitter, boolean, String)} Use
	 *             {@link #get(String, String, Twitter)} instead.
	 */
	@Deprecated
	public TwitterList(String ownerScreenName, String slug, Twitter jtwit) {
		assert ownerScreenName != null && slug != null && jtwit != null;
		this.jtwit = jtwit;
		this.owner = new User(ownerScreenName); // use a dummy here
		this.name = slug;
		this.slug = slug;
		this.http = jtwit.getHttpClient();
		init();
	}

	/**
	 * <b>CREATE</b> a brand new Twitter list. Accounts are limited to 20 lists.
	 * 
	 * @see #TwitterList(String, String, Twitter) which views existing lists.
	 * 
	 * @param listName
	 *            The list's name.
	 * @param jtwit
	 *            a JTwitter object (this must be able to authenticate).
	 * @param description
	 *            A description for this list. Can be null.
	 */
	public TwitterList(String listName, Twitter jtwit, boolean isPublic,
			String description) {
		assert listName != null && jtwit != null;
		this.jtwit = jtwit;
		String ownerScreenName = jtwit.getScreenName();
		assert ownerScreenName != null;
		this.name = listName;
		this.slug = listName;
		this.http = jtwit.getHttpClient();
		// create!
		String url = jtwit.TWITTER_URL + "/lists/create.json";
		Map<String, String> vars = InternalUtils.asMap("name", listName,
				"mode", isPublic ? "public" : "private", "description",
				description);
		String json = http.post(url, vars, true);
		try {
			JSONObject jobj = new JSONObject(json);
			init2(jobj);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	/**
	 * Add a user to the list. List size is limited to 500 users.
	 * 
	 * @return testing for membership could be slow, so this is usually true.
	 */
	@Override
	public boolean add(User user) {
		if (users.contains(user))
			return false;
		String url = jtwit.TWITTER_URL + "/lists/members/create.json";
		Map map = getListVars();
		map.put("screen_name", user.screenName);
		String json = http.post(url, map, true);
		// adjust size
		try {
			JSONObject jobj = new JSONObject(json);
			memberCount = jobj.getInt("member_count");
			// update local
			users.add(user);
			return true;
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	@Override
	public boolean addAll(Collection<? extends User> newUsers) {
		List newUsersList = new ArrayList(newUsers);
		newUsersList.removeAll(users);
		if (newUsersList.size() == 0)
			return false;
		String url = jtwit.TWITTER_URL + "/lists/members/create_all.json";
		Map map = getListVars();
		int batchSize = 100;
		for (int i = 0; i < users.size(); i += batchSize) {
			int last = i + batchSize;
			String names = InternalUtils.join(newUsersList, i, last);
			map.put("screen_name", names);
			String json = http.post(url, map, true);
			// adjust size
			try {
				JSONObject jobj = new JSONObject(json);
				memberCount = jobj.getInt("member_count");
			} catch (JSONException e) {
				throw new TwitterException("Could not parse response: " + e);
			}
		}
		// error messages?
		return true;
	}

	/**
	 * Delete this list!
	 * 
	 * @throws TwitterException
	 *             on failure
	 */
	public void delete() {
		try {
			String URL = jtwit.TWITTER_URL + "/" + owner.screenName + "/lists/"
					+ URLEncoder.encode(slug, "utf-8") + ".json?_method=DELETE";
			http.post(URL, null, http.canAuthenticate());
		} catch (UnsupportedEncodingException e) {
			throw new TwitterException(e);
		}
	}

	@Override
	public User get(int index) {
		// pull from Twitter as needed
		String url = jtwit.TWITTER_URL + "/lists/members.json";
		Map<String, String> vars = getListVars();
		while (users.size() < index + 1 && cursor != 0) {
			vars.put("cursor", Long.toString(cursor));
			String json = http.getPage(url, vars, true);
			try {
				JSONObject jobj = new JSONObject(json);
				JSONArray jarr = (JSONArray) jobj.get("users");
				List<User> users1page = User.getUsers(jarr.toString());
				users.addAll(users1page);
				cursor = new Long(jobj.getString("next_cursor"));
			} catch (JSONException e) {
				throw new TwitterException("Could not parse user list" + e);
			}
		}
		return users.get(index);
	}

	public String getDescription() {
		init();
		return description;
	}

	/**
	 * @return vars identifying the list in question
	 */
	private Map<String, String> getListVars() {
		Map vars = new HashMap();
		if (id != null) {
			vars.put("list_id", id);
			return vars;
		}
		vars.put("owner_screen_name", owner.screenName);
		vars.put("slug", slug);
		return vars;
	}

	public String getName() {
		return name;
	}

	public User getOwner() {
		return owner;
	}

	/**
	 * Returns a list of statuses from this list.
	 * 
	 * @return List<Status> a list of Status objects for the list
	 * @throws TwitterException
	 */
	// Added TG 3/31/10
	public List<Status> getStatuses() throws TwitterException {
		try {
			String jsonListStatuses = http.getPage(
					jtwit.TWITTER_URL + "/" + owner.screenName + "/lists/"
							+ URLEncoder.encode(slug, "UTF-8")
							+ "/statuses.json", null, http.canAuthenticate());
			List<Status> msgs = Status.getStatuses(jsonListStatuses);
			return msgs;
		} catch (UnsupportedEncodingException e) {
			throw new TwitterException(e);
		}
	}

	public int getSubscriberCount() {
		init();
		return subscriberCount;
	}

	/**
	 * @return users who follow this list. Currently this is just the first 20
	 *         users. TODO cursor support for more than 20 users.
	 */
	public List<User> getSubscribers() {
		String url = jtwit.TWITTER_URL + "/lists/subscribers.json";
		Map<String, String> vars = getListVars();
		String json = http.getPage(url, vars, true);
		try {
			JSONObject jobj = new JSONObject(json);
			JSONArray jsonUsers = jobj.getJSONArray("users");
			return User.getUsers2(jsonUsers);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	private String idOrSlug() {
		// TODO encode slugs here?
		return id != null ? id.toString() : slug;
	}

	/**
	 * Fetch list info from Twitter
	 */
	private void init() {
		if (memberCount != -1)
			return;
		String url = jtwit.TWITTER_URL + "/lists/show.json";
		Map<String, String> vars = getListVars();
		String json = http.getPage(url, vars, true);
		try {
			JSONObject jobj = new JSONObject(json);
			init2(jobj);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	private void init2(JSONObject jobj) throws JSONException {
		// owner.screenName = ;
		memberCount = jobj.getInt("member_count");
		subscriberCount = jobj.getInt("subscriber_count");
		name = jobj.getString("name");
		slug = jobj.getString("slug");
		id = jobj.getLong("id");
		_private = "private".equals(jobj.optString("mode"));
		description = jobj.optString("description");
		JSONObject user = jobj.getJSONObject("user");
		owner = new User(user, null);
	}

	public boolean isPrivate() {
		init();
		return _private;
	}

	/**
	 * Remove a user from the list.
	 * 
	 * @return testing for membership could be slow, so this is always true.
	 */
	@Override
	public boolean remove(Object o) {
		try {
			User user = (User) o;
			String url = jtwit.TWITTER_URL + "/lists/members/destroy.json";
			Map map = getListVars();
			map.put("screen_name", user.screenName);
			String json = http.post(url, map, true);
			// adjust size
			JSONObject jobj = new JSONObject(json);
			memberCount = jobj.getInt("member_count");
			// update local
			users.remove(user);
			return true;
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	public void setDescription(String description) {
		String url = jtwit.TWITTER_URL + "/lists/update.json";
		Map<String, String> vars = getListVars();
		vars.put("description", description);
		String json = http.getPage(url, vars, true);
		try {
			JSONObject jobj = new JSONObject(json);
			init2(jobj);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	public void setPrivate(boolean isPrivate) {
		String url = jtwit.TWITTER_URL + "/lists/update.json";
		Map<String, String> vars = getListVars();
		vars.put("mode", isPrivate ? "private" : "public");
		String json = http.getPage(url, vars, true);
		try {
			JSONObject jobj = new JSONObject(json);
			init2(jobj);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	@Override
	public int size() {
		init();
		return memberCount;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + owner + "." + name + "]";
	}

}
