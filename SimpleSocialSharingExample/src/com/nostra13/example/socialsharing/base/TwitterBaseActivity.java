package com.nostra13.example.socialsharing.base;

import android.app.Activity;
import android.widget.Toast;

import com.nostra13.socialsharing.R;
import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.common.LogoutListener;
import com.nostra13.socialsharing.common.PostListener;
import com.nostra13.socialsharing.twitter.TwitterEvents;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public abstract class TwitterBaseActivity extends Activity {

	private AuthListener authListener = new AuthListener() {
		@Override
		public void onAuthSucceed() {
			Toast.makeText(TwitterBaseActivity.this, R.string.toast_twitter_auth_success, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onAuthFail(String error) {
			Toast.makeText(TwitterBaseActivity.this, R.string.toast_twitter_auth_fail, Toast.LENGTH_LONG).show();
		}
	};

	private PostListener postListener = new PostListener() {
		@Override
		public void onPostPublishingFailed() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(TwitterBaseActivity.this, R.string.twitter_post_publishing_failed, Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onPostPublished() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(TwitterBaseActivity.this, R.string.twitter_post_published, Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	private LogoutListener logoutListener = new LogoutListener() {
		@Override
		public void onLogoutComplete() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(TwitterBaseActivity.this, R.string.twitter_logged_out, Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		TwitterEvents.addAuthListener(authListener);
		TwitterEvents.addPostListener(postListener);
		TwitterEvents.addLogoutListener(logoutListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		TwitterEvents.removeAuthListener(authListener);
		TwitterEvents.removePostListener(postListener);
		TwitterEvents.removeLogoutListener(logoutListener);
	}
}
