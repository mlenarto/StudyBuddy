package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
public class RequestInfoFragment extends Fragment {

	private View root;
	private ScrollView infoContainer;
	private Button joinOrLeaveRequest;
	private Button joinAsHelper;
	private TextView memberCount;
	private TextView noGroup;
    private ParseObject requestObj;
	private ParseObject currentGroup;
	private RelativeLayout.LayoutParams groupJoinedParams;
	private int marginSize = 0;
	boolean isInGroup;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.fragment_request_info, container, false);

		infoContainer = (ScrollView) root.findViewById(R.id.info_container);
		joinOrLeaveRequest = (Button) root.findViewById(R.id.request_join_leave);
		joinAsHelper = (Button) root.findViewById(R.id.request_join_helper);
		memberCount = (TextView) root.findViewById(R.id.member_count);
		noGroup = (TextView) root.findViewById(R.id.no_group);

		groupJoinedParams = (RelativeLayout.LayoutParams) joinOrLeaveRequest.getLayoutParams();
		marginSize = groupJoinedParams.leftMargin;

		LoadRequestTask loadRequest = new LoadRequestTask();
		loadRequest.execute();

		return root;
	}


	private class LoadRequestTask extends AsyncTask<Void, Void, String[]> {

		protected String[] doInBackground(Void... notUsed) {
			return loadData();
		}

		protected void onPostExecute(String[] result) {
			if(result != null)
				setupInfo(result);
		}
	}


	private String[] loadData() {
		//get the request info from either the server or the local currentGroup ParseObject
		Bundle extras = getArguments();

		if(extras != null) {	//Having arguments means it is grabbing a request from the server
			final String request_id = extras.getString("request_id");

			//fetch request object from server
			ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
			//TODO: Add loading indicator while this occurs
			try {
				requestObj = query.get(request_id);
			} catch (ParseException e) {
				e.printStackTrace();
				Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
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
				Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
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
				Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
						Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}

			return new String[]{
					requestObj.getString("title"),
					"" + requestObj.getNumber("duration"),
					"" + numHelpers,
					"" + numMembers,
					requestObj.getString("locationDescription"),
					requestObj.getString("description"),
					"" + requestObj.getCreatedAt(),
					request_id
			};

		} else {
			currentGroup = (ParseObject) ParseUser.getCurrentUser().get("currentRequest");
			if(currentGroup != null) {
				try {
					currentGroup.fetchIfNeeded();

					return new String[]{
							currentGroup.getString("title"),
							"" + currentGroup.getNumber("duration"),
							"" + 0,		//Need find a local save of this
							"" + 0,
							currentGroup.getString("locationDescription"),
							currentGroup.getString("description"),
							"" + currentGroup.getCreatedAt(),
							currentGroup.getObjectId()
					};
				} catch (ParseException e) {
					e.printStackTrace();
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							infoContainer.setVisibility(View.GONE);
							noGroup.setText(getString(R.string.group_error));
							noGroup.setVisibility(View.VISIBLE);
						}
					});
				}
			} else {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						infoContainer.setVisibility(View.GONE);
						noGroup.setText(getString(R.string.no_group));
						noGroup.setVisibility(View.VISIBLE);
					}
				});
			}
			return null;	//Error or not in group, display already handled
		}
	}

	/* Result string values:
	 *	result[0] - Group title
	 * 	result[1] - Time remaining (milliseconds)
	 *  result[2] - Number of helpers
	 *  result[3] - Number of group members (including helpers)
	 *  result[4] - Location description of the request
	 *  result[5] - Description of the request's task
	 *  result[6] - Timestamp of the request's creation
	 *  result[7] - ID of the ParseObject representing this request
 	 */
	private void setupInfo(String[] result) {

		TextView requestTitle = (TextView) root.findViewById(R.id.request_title);
		TextView timeRemaining = (TextView) root.findViewById(R.id.request_time);
		TextView requestLocation = (TextView) root.findViewById(R.id.request_location);
		TextView requestDescription = (TextView) root.findViewById(R.id.request_description);

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
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

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
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

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
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

						builder.setMessage(getString(R.string.request_leave_other_group_with_name, currentGroup.get("title")))
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
					Toast.makeText(getActivity().getApplicationContext(), R.string.join_group_success,
							Toast.LENGTH_SHORT).show();
				} else {
					//user was not saved properly.
					e.printStackTrace();
					Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
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
					Toast.makeText(getActivity().getApplicationContext(), R.string.leave_group_success,
							Toast.LENGTH_SHORT).show();
				} else {
					//user was not saved properly.
					e.printStackTrace();
					Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
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
