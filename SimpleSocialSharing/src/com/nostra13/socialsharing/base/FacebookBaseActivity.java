package com.nostra13.socialsharing.base;

import android.app.Activity;
import android.widget.Toast;

import com.nostra13.socialsharing.R;
import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.common.LogoutListener;
import com.nostra13.socialsharing.common.PostListener;
import com.nostra13.socialsharing.facebook.FacebookEvents;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public abstract class FacebookBaseActivity extends Activity {

	private AuthListener authListener = new AuthListener() {
		@Override
		public void onAuthSucceed() {
			Toast.makeText(FacebookBaseActivity.this, R.string.toast_facebook_auth_success, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onAuthFail(String error) {
			Toast.makeText(FacebookBaseActivity.this, R.string.toast_facebook_auth_fail, Toast.LENGTH_LONG).show();
		}
	};

	private PostListener postListener = new PostListener() {
		@Override
		public void onPostPublishingFailed() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(FacebookBaseActivity.this, R.string.facebook_post_publishing_failed, Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onPostPublished() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(FacebookBaseActivity.this, R.string.facebook_post_published, Toast.LENGTH_SHORT).show();
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
					Toast.makeText(FacebookBaseActivity.this, R.string.facebook_logged_out, Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

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
}
