package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * Created by Evan on 2/9/2015.
 */
public class RequestInfoActivity extends ActionBarActivity {

	private Button joinOrLeaveRequest;
	private Button joinAsHelper;
	TextView memberCount;
    private ParseObject requestObj;
	private ParseObject currentGroup;
	private RelativeLayout.LayoutParams groupJoinedParams;
	private int marginSize = 0;

	boolean isInGroup;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_info);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.title_text);
		joinOrLeaveRequest = (Button) findViewById(R.id.request_join_leave);
		joinAsHelper = (Button) findViewById(R.id.request_join_helper);
		memberCount = (TextView) findViewById(R.id.member_count);

		groupJoinedParams = (RelativeLayout.LayoutParams) joinOrLeaveRequest.getLayoutParams();
		marginSize = groupJoinedParams.leftMargin;

		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		mText.setText(R.string.title_request_info);

		setupInfo();

		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}


	private void setupInfo() {
        //get all request info from server
        Bundle extras = getIntent().getExtras(); //grabs the bundle
        String request_title = extras.getString("request_title"); //grabs the selected request title
        String request_id = extras.getString("request_id"); //grabs the selected request object id

        //fetch request object from server
        ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
        //TODO: Add loading indicator while this occurs
        try {
            requestObj = query.get(request_id);
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        //fetch number of group members from server
        int numMembers = 0;
        ParseQuery<ParseUser> memberQuery = ParseUser.getQuery();
        memberQuery.whereEqualTo("currentRequest", requestObj);
        //TODO: Add loading indicator while this occurs
        try {
            numMembers = memberQuery.count();
            Log.d("RequestInfoActivity", "There are " + numMembers + " members.");
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        //fetch number of helper group members from server
        int numHelpers = 0;
        ParseQuery<ParseUser> helperQuery = ParseUser.getQuery();
        helperQuery.whereEqualTo("currentRequest", requestObj);
        helperQuery.whereEqualTo("isHelper", true);
        //TODO: Add loading indicator while this occurs
        try {
            numHelpers = helperQuery.count();
            Log.d("RequestInfoActivity", "There are " + numHelpers + " helpers.");
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        final String[] result = new String[]{
                requestObj.getString("title"),
                "" + requestObj.getNumber("duration"),	//Time assigned to request in milliseconds (Total time, not remaining)
                "" + numHelpers,		//number of helpers
                "" + numMembers,		//total group members
                requestObj.getString("locationDescription"),	//Location
                requestObj.getString("description"), //Description
                "" + requestObj.getCreatedAt(), //Make this a timestamp of the creation time please"
                request_id	//ID of the request object
        };
/*
        final String[] result = new String[]{
				"Example Title",
				"6600000",	//Time assigned to request in milliseconds (Total time, not remaining)
				"5",		//number of helpers
				"8",		//total group members
				"Lawson Commons, round table under the TVs.",	//Location
				"This is an example of a somewhat long description that takes multiple lines.", //Description
				"12315645613", //Make this a timestamp of the creation time please"
				"gi32kln"	//ID of the request object
		};
*/

		TextView requestTitle = (TextView) findViewById(R.id.request_title);
		TextView timeRemaining = (TextView) findViewById(R.id.request_time);
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

		currentGroup = (ParseObject) ParseUser.getCurrentUser().get("currentRequest");

		if(currentGroup != null) {
			try {
				currentGroup.fetchIfNeeded();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			if(result[7].equals(currentGroup.getObjectId()))
				isInGroup = true;
		}
		else {
			isInGroup = false;
		}
		setupButtonLayout();

		joinAsHelper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(currentGroup != null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(RequestInfoActivity.this);

					builder.setMessage(getString(R.string.request_leave_other_group_with_name, currentGroup.get("title")))  //Change this to the other group's title
							.setTitle(getString(R.string.request_already_in_group_warning));

					builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//join group on server
							joinGroup(true);
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
					//join group on server
					joinGroup(true);
				}
			}
		});

		joinOrLeaveRequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isInGroup) {		//Always confirm for leaving the group
					AlertDialog.Builder builder = new AlertDialog.Builder(RequestInfoActivity.this);

					builder.setMessage(getString(R.string.request_leave_generic_warning))
							.setTitle(getString(R.string.request_leave_with_name, currentGroup.get("title")));

					builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Leave group on server
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
					if(currentGroup != null) {
						AlertDialog.Builder builder = new AlertDialog.Builder(RequestInfoActivity.this);

						builder.setMessage(getString(R.string.request_leave_other_group_with_name, currentGroup.get("title")))  //Change this to the other group's title
								.setTitle(getString(R.string.request_already_in_group_warning));

						builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//join group on server
								joinGroup(false);

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
						//join group on server
                        joinGroup(false);
					}
				}
			}
		});
	}


	private void joinGroup(boolean isHelper) {
		ParseUser.getCurrentUser().put("currentRequest", requestObj);
		ParseUser.getCurrentUser().put("isHelper", isHelper);
		ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
			@Override
			public void done(ParseException e) {
				if (e == null) {
					//user saved properly.
					isInGroup = true;
					currentGroup = requestObj;
					setupButtonLayout();
					Toast.makeText(getApplicationContext(), R.string.join_group_success,
							Toast.LENGTH_SHORT).show();
				} else {
					//user was not saved properly.
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), getString(R.string.network_error),
							Toast.LENGTH_SHORT).show();
				}

			}
		});
	}

	private void leaveGroup() {
		ParseUser.getCurrentUser().remove("currentRequest");
		ParseUser.getCurrentUser().put("isHelper", false);
		ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
			@Override
			public void done(ParseException e) {
				if (e == null) {
					//user saved properly.
					isInGroup = false;
					currentGroup = null;
					setupButtonLayout();
					Toast.makeText(getApplicationContext(), R.string.leave_group_success,
							Toast.LENGTH_SHORT).show();
				} else {
					//user was not saved properly.
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), getString(R.string.network_error),
							Toast.LENGTH_SHORT).show();
				}

			}
		});
	}



	private void setupButtonLayout() {
		groupJoinedParams = (RelativeLayout.LayoutParams) joinOrLeaveRequest.getLayoutParams();
		if(isInGroup) {
			joinOrLeaveRequest.setText(getString(R.string.request_leave));
			groupJoinedParams.leftMargin = 0;
			groupJoinedParams.addRule(RelativeLayout.RIGHT_OF, 0);
			joinOrLeaveRequest.setLayoutParams(groupJoinedParams);
			joinAsHelper.setVisibility(View.GONE);
		} else {
			joinOrLeaveRequest.setText(getString(R.string.request_join));
			groupJoinedParams.leftMargin = marginSize;
			groupJoinedParams.addRule(RelativeLayout.RIGHT_OF, R.id.center);
			joinOrLeaveRequest.setLayoutParams(groupJoinedParams);
			joinAsHelper.setVisibility(View.VISIBLE);
		}
	}
}
