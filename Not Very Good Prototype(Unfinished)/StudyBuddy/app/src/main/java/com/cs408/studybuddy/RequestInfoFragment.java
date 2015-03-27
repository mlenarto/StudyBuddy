package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.parse.DeleteCallback;
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
    private ProgressDialog progress;
	private RelativeLayout.LayoutParams groupJoinedParams;
	private int marginSize = 0;
    private int numHelpers;
    private int numMembers;
    private boolean isInGroup;
	private boolean isProcessing = false;
	private boolean myGroup = true;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.fragment_request_info, container, false);
		//infoContainer = (ScrollView) root.findViewById(R.id.info_container);
		joinOrLeaveRequest = (Button) root.findViewById(R.id.request_join_leave);
		joinAsHelper = (Button) root.findViewById(R.id.request_join_helper);



		memberCount = (TextView) root.findViewById(R.id.member_count);
		noGroup = (TextView) root.findViewById(R.id.no_group);

		groupJoinedParams = (RelativeLayout.LayoutParams) joinOrLeaveRequest.getLayoutParams();
		marginSize = groupJoinedParams.leftMargin;
        progress = ProgressDialog.show(getActivity(), "Getting group info...", "Please wait...", true);
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
			myGroup = false;
			final String request_id = extras.getString("request_id");

			//fetch request object from server
			ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
			try {
				requestObj = query.get(request_id);
			} catch (ParseException e) {
				if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							displayDeletedGroupAlert(true);
						}
					});
					return null;
				}
				else {
					e.printStackTrace();
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
									Toast.LENGTH_SHORT).show();
							getActivity().finish();
						}
					});
					return null;
				}
			}

			//fetch number of group members from server
			numMembers = 0;
			ParseQuery<ParseUser> memberQuery = ParseUser.getQuery();
			memberQuery.whereEqualTo("currentRequest", requestObj);
			try {
				numMembers = memberQuery.count();
				Log.d("RequestInfoActivity", "There are " + numMembers + " members.");
			} catch (ParseException e) {
				e.printStackTrace();
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
								Toast.LENGTH_SHORT).show();
						getActivity().finish();
					}
				});
				return null;
			}

			//fetch number of helper group members from server
			numHelpers = 0;
			ParseQuery<ParseUser> helperQuery = ParseUser.getQuery();
			helperQuery.whereEqualTo("currentRequest", requestObj);
			helperQuery.whereEqualTo("isHelper", true);
			try {
				numHelpers = helperQuery.count();
				Log.d("RequestInfoActivity", "There are " + numHelpers + " helpers.");
			} catch (ParseException e) {
				e.printStackTrace();
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
								Toast.LENGTH_SHORT).show();
						getActivity().finish();
					}
				});
				return null;
			}

			return new String[]{
					requestObj.getString("title"),
					"" + requestObj.getNumber("duration"),
					"" + numHelpers,
					"" + numMembers,
					requestObj.getString("locationDescription"),
					requestObj.getString("description"),
					"" + requestObj.getCreatedAt().getTime(),
					request_id
			};

		} else {	//Grabbing the user's current group
			myGroup = true;
			currentGroup = (ParseObject) ParseUser.getCurrentUser().get("currentRequest");
			if(currentGroup != null) {
				try {
                    requestObj = currentGroup;

                    currentGroup.fetchIfNeeded();

                    /* attempt to fetch member/helper counts from server.
                        if it fails, use the cached numbers.
                     */

					if(!CheckInternet()) {
						numMembers = ParseUser.getCurrentUser().getInt("cacheMembers");
						numHelpers = ParseUser.getCurrentUser().getInt("cacheHelpers");
					} else {
						try {
							ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
							query.get(currentGroup.getObjectId());
						} catch (ParseException e) {
							if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {
								ParseUser.getCurrentUser().remove("currentRequest");
								ParseUser.getCurrentUser().put("isHelper", false);
								ParseUser.getCurrentUser().remove("cacheHelpers");
								ParseUser.getCurrentUser().remove("cacheMembers");
								ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
									@Override
									public void done(ParseException e) {
										if (e == null) {
											//user saved properly.
											isInGroup = false;
											currentGroup = null;
											ParseUser.getCurrentUser().pinInBackground();     //cache (lack of) group info
										} else {
											//user was not saved properly.
											e.printStackTrace();
										}
									}
								});
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										progress.dismiss();
										//infoContainer.setVisibility(View.GONE);
										noGroup.setText(getString(R.string.no_group));
										noGroup.setVisibility(View.VISIBLE);

										displayDeletedGroupAlert(false);
									}
								});
								return null;
							}
						}


						try {
							//fetch number of group members from server
							ParseQuery<ParseUser> memberQuery = ParseUser.getQuery();
							memberQuery.whereEqualTo("currentRequest", currentGroup);
							numMembers = memberQuery.count();
							Log.d("RequestInfoActivity", "There are " + numMembers + " members.");

							//fetch number of helper group members from server
							ParseQuery<ParseUser> helperQuery = ParseUser.getQuery();
							helperQuery.whereEqualTo("currentRequest", currentGroup);
							helperQuery.whereEqualTo("isHelper", true);
							numHelpers = helperQuery.count();
							Log.d("RequestInfoActivity", "There are " + numHelpers + " helpers.");
						} catch(ParseException e){
							//get helper/member count from cache
							numMembers = ParseUser.getCurrentUser().getInt("cacheMembers");
							numHelpers = ParseUser.getCurrentUser().getInt("cacheHelpers");
						}
					}


                    return new String[]{
							currentGroup.getString("title"),
							"" + currentGroup.getNumber("duration"),
							"" + numHelpers,
                            "" + numMembers,
							currentGroup.getString("locationDescription"),
							currentGroup.getString("description"),
							"" + currentGroup.getCreatedAt().getTime(),
							currentGroup.getObjectId()
					};
				} catch (ParseException e) {
					if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {
						ParseUser.getCurrentUser().remove("currentRequest");
						ParseUser.getCurrentUser().put("isHelper", false);
						ParseUser.getCurrentUser().remove("cacheHelpers");
						ParseUser.getCurrentUser().remove("cacheMembers");
						ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
							@Override
							public void done(ParseException e) {
								if (e == null) {
									//user saved properly.
									isInGroup = false;
									currentGroup = null;
									ParseUser.getCurrentUser().pinInBackground();     //cache (lack of) group info
								} else {
									//user was not saved properly.
									e.printStackTrace();
								}
							}
						});
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progress.dismiss();
								//infoContainer.setVisibility(View.GONE);
								noGroup.setText(getString(R.string.no_group));
								noGroup.setVisibility(View.VISIBLE);

								displayDeletedGroupAlert(false);
							}
						});
						return null;
					}
					e.printStackTrace();
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							//infoContainer.setVisibility(View.GONE);
							noGroup.setText(getString(R.string.group_error));
							noGroup.setVisibility(View.VISIBLE);
						}
					});
				}
			} else {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//infoContainer.setVisibility(View.GONE);
						noGroup.setText(getString(R.string.no_group));
						noGroup.setVisibility(View.VISIBLE);
					}
				});
			}
            progress.dismiss();
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
	 *  result[6] - Timestamp of the request's creation (saved as long)
	 *  result[7] - ID of the ParseObject representing this request
 	 */
	private void setupInfo(final String[] result) {

		TextView requestTitle = (TextView) root.findViewById(R.id.request_title);
		TextView timeRemaining = (TextView) root.findViewById(R.id.request_time);
		TextView requestLocation = (TextView) root.findViewById(R.id.request_location);
		TextView requestDescription = (TextView) root.findViewById(R.id.request_description);


		final int remainingTimeMillis = Integer.parseInt(result[1]);

		int timeHours = remainingTimeMillis / 3600000;		//Milliseconds in an hour
		int timeMinutes = (remainingTimeMillis - timeHours * 3600000) / 60000;
		String h = getResources().getQuantityString(R.plurals.hours, timeHours);
		String m = getResources().getQuantityString(R.plurals.minutes, timeMinutes);
		Log.d("time remaining", "remainingTimeMillis = " + remainingTimeMillis);
		if(timeHours == 0)
			timeRemaining.setText(timeMinutes + " " + m);
		else
			timeRemaining.setText(timeHours + " " + h + ", " + timeMinutes + " " + m);

		int helperCount = Integer.parseInt(result[2]);
		int membersTotal = Integer.parseInt(result[3]);
		String help = getResources().getQuantityString(R.plurals.helpers, helperCount);
		String members = getResources().getQuantityString(R.plurals.members, membersTotal);

		requestTitle.setText(result[0]);
		memberCount.setText(membersTotal + " " + members + " (" + helperCount + " " + help + ")");
		requestLocation.setText(result[4]);
		requestDescription.setText(result[5]);
        progress.dismiss();
		currentGroup = (ParseObject) ParseUser.getCurrentUser().get("currentRequest");

		if(currentGroup != null) {
			try {
				currentGroup.fetchIfNeeded();
				ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
				query.get(currentGroup.getObjectId());
			} catch (ParseException e) {
				if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {
					ParseUser.getCurrentUser().remove("currentRequest");
					ParseUser.getCurrentUser().put("isHelper", false);
					ParseUser.getCurrentUser().remove("cacheHelpers");
					ParseUser.getCurrentUser().remove("cacheMembers");
					try {
						ParseUser.getCurrentUser().save();

						isInGroup = false;
						currentGroup = null;
						ParseUser.getCurrentUser().pinInBackground();
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
				}
				e.printStackTrace();
			}
			if(currentGroup != null && result[7].equals(currentGroup.getObjectId()))
				isInGroup = true;
		}
		else {
            progress.dismiss();
			isInGroup = false;
		}
		setupButtonLayout();

		joinAsHelper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isProcessing)
					return;
				isProcessing = true;

				ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");

				try {
					//Checks if the request still exists when the user tries to join.
					//It doesn't seems like there is a way to check without querying for it which
					//throws a parse exception when it doesn't exist.
					requestObj = query.get(result[7]);

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

					if(currentGroup != null) {	//Only confirm for joining if the user is in another group
						builder.setMessage(getString(R.string.request_leave_other_group_with_name, currentGroup.get("title")))
								.setTitle(getString(R.string.request_already_in_group_warning));

						builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//join group on server
                                checkAndJoin(false);
							}
						});

						builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//Do nothing
								isProcessing = false;
							}
						});

						AlertDialog dialog = builder.create();
						/*dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialog) {
								isProcessing = false;
							}
						});*/
						dialog.show();
					} else {
						//join group on server
						joinGroup(false);
					}

				} catch (ParseException e) {
					if(e.getCode() == ParseException.OBJECT_NOT_FOUND)	//Group has been deleted
						displayDeletedGroupAlert(false);
					isProcessing = false;
					e.printStackTrace();
				}
			}
		});

		joinOrLeaveRequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isProcessing)
					return;
				isProcessing = true;

				ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");

				try {
					requestObj = query.get(result[7]);

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
								isProcessing = false;
							}
						});

						AlertDialog dialog = builder.create();
						/*dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialog) {
								isProcessing = false;
							}
						});*/
						dialog.show();
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
                                    checkAndJoin(true);

								}
							});

							builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									//Do nothing
									isProcessing = false;
								}
							});

							AlertDialog dialog = builder.create();
							/*dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface dialog) {
									isProcessing = false;
								}
							});*/
							dialog.show();
						} else {
							//join group on server

							joinGroup(true);
						}
					}

				} catch (ParseException e) {
					if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {	//Group has been deleted
						if(isInGroup) {			//Trying to leave a deleted request should leave as normal
							leaveGroup();
						} else {    			//Can't join a request that has been deleted.
							displayDeletedGroupAlert(false);
						}
					}
					isProcessing = false;
					e.printStackTrace();

				}
			}
		});
	}


	private void joinGroup(final boolean isHelper) {
        numMembers++;       //update member & helper count
        if(isHelper) {
            numHelpers++;
        }
        progress = ProgressDialog.show(getActivity(), "Joining group...", "Please wait...", true);
		ParseUser.getCurrentUser().put("currentRequest", requestObj);
		ParseUser.getCurrentUser().put("isHelper", isHelper);
        ParseUser.getCurrentUser().put("cacheHelpers", numHelpers);
        ParseUser.getCurrentUser().put("cacheMembers", numMembers);
		ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
			@Override
			public void done(ParseException e) {
				if (e == null) {
					//user saved properly.
					isInGroup = true;
					currentGroup = requestObj;
                    ParseUser.getCurrentUser().pinInBackground();     //cache current group info, assume happens almost immediately.
					setupButtonLayout();
                    String help = getResources().getQuantityString(R.plurals.helpers, numHelpers);      //update UI with helpers/members
                    String members = getResources().getQuantityString(R.plurals.members, numMembers);
                    //memberCount.setText(numMembers + " " + members + " (" + numHelpers + " " + help + ")");
					isProcessing = false;
                    progress.dismiss();
                    Toast.makeText(getActivity().getApplicationContext(), R.string.join_group_success,
							Toast.LENGTH_SHORT).show();
				} else {
					//user was not saved properly.
					e.printStackTrace();
					isProcessing = false;
                    progress.dismiss();
					Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
							Toast.LENGTH_SHORT).show();
				}

			}
		});
	}

	private void leaveGroup() {
        progress = ProgressDialog.show(getActivity(), "Leaving group...", "Please wait...", true);

        try {
            //fetch number of group members from server, delete the request object if the current user is the only member
            ParseQuery<ParseUser> memberQuery = ParseUser.getQuery();
            memberQuery.whereEqualTo("currentRequest", currentGroup);
            if (memberQuery.count() <= 1){
                //Prompt the user to let them know it will delete the group
                progress.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.request_leave_delete_warning))
                        .setTitle(getString(R.string.request_leave_delete, currentGroup.get("title")));

                builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete group and continue to leave
                        progress = ProgressDialog.show(getActivity(), "Leaving group...", "Please wait...", true);
                        currentGroup.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null){
                                    //Group delete successful, now leave it
                                    if(ParseUser.getCurrentUser().getBoolean("isHelper")){
                                        numHelpers--;
                                    }
                                    numMembers--;
                                    ParseUser.getCurrentUser().remove("currentRequest");
                                    ParseUser.getCurrentUser().put("isHelper", false);
                                    ParseUser.getCurrentUser().remove("cacheHelpers");
                                    ParseUser.getCurrentUser().remove("cacheMembers");
                                    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                //user saved properly.
                                                isInGroup = false;
                                                currentGroup = null;
                                                ParseUser.getCurrentUser().pinInBackground();     //cache (lack of) group info
                                                setupButtonLayout();
                                                String help = getResources().getQuantityString(R.plurals.helpers, numHelpers);      //update UI with helpers/members
                                                String members = getResources().getQuantityString(R.plurals.members, numMembers);
                                                //memberCount.setText(numMembers + " " + members + " (" + numHelpers + " " + help + ")");
                                                progress.dismiss();
                                                Toast.makeText(getActivity().getApplicationContext(), R.string.leave_group_success,
                                                        Toast.LENGTH_SHORT).show();
                                                isProcessing = false;
												if(!myGroup)		//Return to the request list if the request was opened from there
													((RequestInfoActivity)getActivity()).returnAndUpdate();
												else
													refreshInfo();
                                            } else {
                                                //user was not saved properly.
                                                e.printStackTrace();
                                                progress.dismiss();
                                                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
                                                        Toast.LENGTH_SHORT).show();
                                                isProcessing = false;
                                            }
                                        }
                                    });
                                }
                                else{
                                    //group was not deleted
                                    e.printStackTrace();
                                    progress.dismiss();
                                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
                                            Toast.LENGTH_SHORT).show();
                                    isProcessing = false;
                                }
                            }
                        });

                    }
                });

                builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //User doesn't leave the group, do nothing
                        isProcessing = false;
                    }
                });

                builder.create().show();
            }
            else{
                //Leaving group will not cause it to be deleted, don't prompt user.
                if(ParseUser.getCurrentUser().getBoolean("isHelper")){
                    numHelpers--;
                }
                numMembers--;
                ParseUser.getCurrentUser().remove("currentRequest");
                ParseUser.getCurrentUser().put("isHelper", false);
                ParseUser.getCurrentUser().remove("cacheHelpers");
                ParseUser.getCurrentUser().remove("cacheMembers");
                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            //user saved properly.
                            isInGroup = false;
                            currentGroup = null;
                            ParseUser.getCurrentUser().pinInBackground();     //cache (lack of) group info
                            setupButtonLayout();
                            String help = getResources().getQuantityString(R.plurals.helpers, numHelpers);      //update UI with helpers/members
                            String members = getResources().getQuantityString(R.plurals.members, numMembers);
                            //memberCount.setText(numMembers + " " + members + " (" + numHelpers + " " + help + ")");
                            progress.dismiss();
                            Toast.makeText(getActivity().getApplicationContext(), R.string.leave_group_success,
                                    Toast.LENGTH_SHORT).show();
                            isProcessing = false;
                        } else {
                            //user was not saved properly.
                            e.printStackTrace();
                            progress.dismiss();
                            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
                                    Toast.LENGTH_SHORT).show();
                            isProcessing = false;
                        }

                    }
                });
            }

        } catch(ParseException e){
            Log.e("RequestInfoFragment", "Error deleting help request when user leaves");
            e.printStackTrace();
            progress.dismiss();
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
            isProcessing = false;
        }
	}

	public void refreshInfo() {
		LoadRequestTask loadRequest = new LoadRequestTask();
		loadRequest.execute();
	}


	//Decide whether to show the "Leave group" button or both buttons for joining the group
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

	private void displayDeletedGroupAlert(final boolean leaveActivity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.request_deleted_info))
				.setTitle(getString(R.string.request_deleted));

		builder.setCancelable(false);
		builder.setNeutralButton(getString(R.string.neutral_option), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(leaveActivity) {
					((RequestInfoActivity) getActivity()).returnAndUpdate();
				}
			}
		});
		builder.create().show();
	}

	private boolean CheckInternet()
	{
		ConnectivityManager connec = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		android.net.NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if (wifi.isConnected()) {
			return true;
		} else if (mobile.isConnected()) {
			return true;
		}
		return false;
	}

    /**
     * Checks if leaving the user's old
     * group will cause it to be deleted.
     * If so, it prompts the user and then joins
     * if they select to continue.
     *
     * By Tyler
     */
    private void checkAndJoin(boolean isHelper){
        final Boolean isHelperLocal = isHelper;
        progress = ProgressDialog.show(getActivity(), "Leaving group...", "Please wait...", true);

        try {
            //fetch number of group members from server, delete the request object if the current user is the only member
            ParseQuery<ParseUser> memberQuery = ParseUser.getQuery();
            memberQuery.whereEqualTo("currentRequest", currentGroup);
            if (memberQuery.count() <= 1){
                //Prompt the user to let them know it will delete the group
                progress.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.request_leave_delete_warning))
                        .setTitle(getString(R.string.request_leave_delete, currentGroup.get("title")));

                builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete group and continue to leave
                        progress = ProgressDialog.show(getActivity(), "Leaving group...", "Please wait...", true);
                        currentGroup.deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null){
                                    //Group delete successful, now join the other group
                                    progress.dismiss();
                                    joinGroup(isHelperLocal);
                                }
                                else{
                                    //group was not deleted
                                    e.printStackTrace();
                                    progress.dismiss();
                                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
                                            Toast.LENGTH_SHORT).show();
                                    isProcessing = false;
                                }
                            }
                        });

                    }
                });

                builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //User doesn't leave the group, do nothing
                        isProcessing = false;
                    }
                });

                builder.create().show();
            }
            else{
                //Leaving group will not cause it to be deleted, don't prompt user.
                progress.dismiss();
                joinGroup(isHelperLocal);
            }

        } catch(ParseException e){
            Log.e("RequestInfoFragment", "Error deleting help request when user leaves");
            e.printStackTrace();
            progress.dismiss();
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
            isProcessing = false;
        }
    }
}
