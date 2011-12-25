package com.nostra13.example.socialsharing;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nostra13.example.socialsharing.Constants.Extra;
import com.nostra13.example.socialsharing.base.FacebookBaseActivity;
import com.nostra13.socialsharing.facebook.FacebookFacade;

/**
 * Activity for sharing information with Facebook
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class FacebookActivity extends FacebookBaseActivity {

	private FacebookFacade facebook;

	private TextView messageView;
	private TextView linkNameView;
	private TextView linkDescriptionView;
	private Button postButton;

	private String link;
	private String linkName;
	private String linkDescription;
	private Map<String, String> actionsMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ac_facebook);

		facebook = new FacebookFacade(this, Constants.FACEBOOK_APP_ID);

		messageView = (TextView) findViewById(R.id.message);
		linkNameView = (TextView) findViewById(R.id.link_name);
		linkDescriptionView = (TextView) findViewById(R.id.link_description);
		postButton = (Button) findViewById(R.id.button_post);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String message = bundle.getString(Extra.POST_MESSAGE);
			link = bundle.getString(Extra.POST_LINK);
			linkName = bundle.getString(Extra.POST_LINK_NAME);
			linkDescription = bundle.getString(Extra.POST_LINK_DESCRIPTION);
			actionsMap = new HashMap<String, String>();
			actionsMap.put(Constants.FACEBOOK_SHARE_ACTION_NAME, Constants.FACEBOOK_SHARE_ACTION_LINK);

			messageView.setText(message);
			linkNameView.setText(linkName);
			linkDescriptionView.setText(linkDescription);
		}

		postButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (facebook.isAuthorized()) {
					facebook.publishMessage(messageView.getText().toString(), link, linkName, linkDescription, Constants.FACEBOOK_SHARE_PICTURE, actionsMap);
					finish();
				} else {
					facebook.authorize();
				}
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!facebook.isAuthorized()) {
			facebook.authorize();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_facebook_twitter, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.item_logout:
				facebook.logout();
				return true;
			default:
				return false;
		}
	}
}
