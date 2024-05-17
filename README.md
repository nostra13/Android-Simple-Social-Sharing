## ! THIS PROJECT IS NO LONGER BEING MAINTAINED !

# Simple Social Sharing for Android

This project aims to provide a reusable instrument for simple sharing with popular social networks (Facebook, Twiiter).
Project uses other open source projects' code (actual on 25.02.2012):

 * [Facebook SDK for Android](https://github.com/facebook/facebook-android-sdk)
 * [OAuth Signpost](https://github.com/kaeppler/signpost)
 * [JTwitter](https://github.com/winterstein/JTwitter)


## Features
 * Simple API for Facebook and Twitter sharing (fast indroduction)
 * Simple API for event listening (authentication, posting, logging out)
 * Support only simple sharing (post message or image to Facebook, post status to Twitter)

**Important:** In your Twitter Developer Application Settings (https://dev.twitter.com/) you must fill "Callback URL" field with any URL (i.e. http://abc.de).
It's behaviour by default.

There are two classes in SSS:

 * TwitterDialog
 * CallbackTwitterDialog
 
```CallbackTwitterDialog``` is used by default and it works only when "Callback URL" is filled in your Twitter Application settings.
You may not use callback URL. Then you should clear "Callback URL" field in app settings and replace all using ```CallbackTwitterDialog``` with ```TwitterDilaog``` (in ```TwitterFacade``` class).

## Usage

### Sharing API

#### Facebook

``` java
FacebookFacade facebook = new FacebookFacade(activity, FACEBOOK_APP_ID);
if (!facebook.isAuthorized()) {
	facebook.authorize();
}
facebook.publishMessage("This is great App!");
facebook.logout();
```

**More powerful posting:**

``` java
Map actions = new HashMap<String, String>();
actions.put("Android Simple Social Sharing", "https://github.com/nostra13");
facebook.publishMessage("Look at this great App!",
						"Use Android Simple Social Sharing in your project!",
						"https://github.com/nostra13/Android-Simple-Social-Sharing",
						"Also see other projects of nostra13 on GitHub!",
						"http://.......facebook-android-logo-1.jpg",
						actions);
```
![Screenshot](https://github.com/nostra13/Android-Simple-Social-Sharing/raw/master/FacebookPost.png)

#### Twitter

``` java
TwitterFacade twitter = new TwitterFacade(context, TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);
if (!twitter.isAuthorized()) {
	twitter.authorize();
}
twitter.publishMessage("This is great app!");
twitter.logout();
```

### Event listening API

#### Facebook

``` java
...
	@Override
	protected void onStart() {
		super.onStart();
		FacebookEvents.addAuthListener(authListener);
		FacebookEvents.addPostListener(postListener);
		FacebookEvents.addLogoutListener(logoutListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		FacebookEvents.removeAuthListener(authListener);
		FacebookEvents.removePostListener(postListener);
		FacebookEvents.removeLogoutListener(logoutListener);
	}

	private AuthListener authListener = new AuthListener() {
		@Override
		public void onAuthSucceed() {
			showToastOnUIThread("Facebook authentication is successful");
		}

		@Override
		public void onAuthFail(String error) {
			showToastOnUIThread("Error was occurred during Facebook authentication");
		}
	};

	private PostListener postListener = new PostListener() {
		@Override
		public void onPostPublishingFailed() {
			showToastOnUIThread("Post publishing was failed");
		}

		@Override
		public void onPostPublished() {
			showToastOnUIThread("Posted to Facebook successfully");
		}
	};

	private LogoutListener logoutListener = new LogoutListener() {
		@Override
		public void onLogoutComplete() {
			showToastOnUIThread("You are logged out");
		}
	};

	private void showToastOnUIThread(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(YourActivity.this, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
...
```

#### Twitter

Like Facebook listening example but use TwitterEvents instead of FacebookEvents.
 
## License

Licensed under the [BSD 3-clause](http://www.opensource.org/licenses/BSD-3-Clause)
