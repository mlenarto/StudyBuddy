package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Evan on 2/9/2015.
 */
public class RequestInfoActivity extends ActionBarActivity {

	private Button joinOrLeaveRequest;
	private SharedPreferences prefs;

	boolean isInGroup;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_info);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.title_text);
		joinOrLeaveRequest = (Button) findViewById(R.id.request_join_leave);

		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		mText.setText(R.string.title_request_info);

		prefs = getSharedPreferences(getString(R.string.app_preferences), 0);

		//Async tasks let you return the view faster (so it loads on screen) instead
		//of waiting for the data from the server.
		DownloadRequestTask download = new DownloadRequestTask();
		download.execute();

		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}


	private class DownloadRequestTask extends AsyncTask<Void, Void, String[]> {

		protected String[] doInBackground(Void... params) {
			//TODO: Do Network Stuff here to retrieve the request info


			return new String[]{
					"Example Title",
					"6600000",	//Time assigned to request in milliseconds (Total time, not remaining)
					"5",		//number of helpers
					"8",		//total group members
					"Lawson Commons, round table under the TVs.",	//Location
					"This is an example of a somewhat long description that takes multiple lines.", //Description
					"12315645613", //Make this a timestamp of the creation time please"
					"gi32kln"	//ID of the request object
			};
		}

		//We can use this method to change the UI after the background task is finished
		//(onPreExecute can be used as well, for set up before the background task)
		protected  void onPostExecute(final String[] result) {
			TextView requestTitle = (TextView) findViewById(R.id.request_title);
			TextView timeRemaining = (TextView) findViewById(R.id.request_time);
			TextView memberCount = (TextView) findViewById(R.id.member_count);
			TextView requestLocation = (TextView) findViewById(R.id.request_location);
			TextView requestDescription = (TextView) findViewById(R.id.request_description);

			int timeMillis = Integer.parseInt(result[1]);
			int timeHours = timeMillis / 3600000;		//Milliseconds in an hour
			int timeMinutes = (timeMillis - timeHours * 3600000) / 60000;
			String h = getResources().getQuantityString(R.plurals.hours, timeHours);
			String m = getResources().getQuantityString(R.plurals.minutes, timeMinutes);

			int helperCount = Integer.parseInt(result[2]);
			int membersTotal = Integer.parseInt(result[3]);
			String help = getResources().getQuantityString(R.plurals.helpers, helperCount);
			String members = getResources().getQuantityString(R.plurals.members, membersTotal);

			requestTitle.setText(result[0]);
			timeRemaining.setText(timeHours + " " + h + ", " + timeMinutes + " " + m);
			memberCount.setText(helperCount + " " + help + ", " + membersTotal + " " + members);
			requestLocation.setText(result[4]);
			requestDescription.setText(result[5]);

			String currentGroup = prefs.getString(getString(R.string.my_request_id), null);
			if(currentGroup != null && currentGroup.equals(result[7]) ) {
				joinOrLeaveRequest.setText(getString(R.string.request_leave));
				isInGroup = true;
			}
			else {
				joinOrLeaveRequest.setText(getString(R.string.request_join));
				isInGroup = false;
			}

			joinOrLeaveRequest.setVisibility(View.VISIBLE);

			joinOrLeaveRequest.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(isInGroup) {		//Always confirm for leaving the group
						AlertDialog.Builder builder = new AlertDialog.Builder(RequestInfoActivity.this);

						builder.setMessage(getString(R.string.request_leave_generic_warning))
								.setTitle(getString(R.string.request_leave_with_name, result[0]));

						builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//TODO: Leave group on server
								leaveGroup();
							}
						});

						builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//Do nothing
							}
						});

						builder.create().show();
					}
					else {		//Only confirm for joining if the user is in another group
						if(prefs.contains(getString(R.string.my_request_id))) {
							AlertDialog.Builder builder = new AlertDialog.Builder(RequestInfoActivity.this);

							builder.setMessage(getString(R.string.request_leave_other_group_with_name, result[0]))  //Change this to the other group's title
									.setTitle(getString(R.string.request_already_in_group_warning));

							builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									//TODO: join group on server
									joinGroup(result[7]);
								}
							});

							builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									//Do nothing
								}
							});

							builder.create().show();
						} else {
							//TODO: join group on server
							joinGroup(result[7]);
						}
					}
				}
			});

		}
	}


	private void joinGroup(String parseID) {
		SharedPreferences.Editor edit = prefs.edit();

		edit.putString(getString(R.string.my_request_id), parseID);

		edit.apply();
		isInGroup = true;
		joinOrLeaveRequest.setText(getString(R.string.request_leave));
	}

	private void leaveGroup() {
		SharedPreferences.Editor edit = prefs.edit();

		edit.remove(getString(R.string.my_request_id));

		edit.apply();
		isInGroup = false;
		joinOrLeaveRequest.setText(getString(R.string.request_join));
	}
}
