package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.concurrent.Semaphore;

/**
 * Created by Evan on 2/8/2015.
 */
public class NewRequestActivity extends ActionBarActivity {

    private Toolbar mToolbar;
    private EditText requestTitleEdit;
    private TextView requestTitleLength;
    private EditText descriptionTextEdit;
    private TextView descriptionLength;
    private EditText requestLocationEdit;
    private TextView requestLocationLength;
    private ParseObject request, courseObj, currentGroup;
    private ParseUser user;
    private LocationService gps;
    private ProgressDialog progress;
    private boolean isSubmitting = false;
    private boolean proceed = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_request);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mTitle = (TextView) findViewById(R.id.title_text);
        requestTitleEdit = (EditText) findViewById(R.id.request_title);
        requestTitleLength = (TextView) findViewById(R.id.request_title_length);
        descriptionTextEdit = (EditText) findViewById(R.id.description);
        descriptionLength = (TextView) findViewById(R.id.description_length);
        requestLocationEdit = (EditText) findViewById(R.id.request_location);
        requestLocationLength = (TextView) findViewById(R.id.location_length);
        final Spinner hoursLengthSpinner = (Spinner) findViewById(R.id.request_duration_hours);
        final Spinner minutesLengthSpinner = (Spinner) findViewById(R.id.request_duration_minutes);
        Button submit = (Button) findViewById(R.id.submit_button);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mTitle.setText("New Request");

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        gps = LocationService.getInstance(getApplicationContext());
        gps.startGPS(15000, 10);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress = ProgressDialog.show(NewRequestActivity.this, "Adding your request...", "Please wait...", true);

                if (isSubmitting)
                    return;

                isSubmitting = true;
                String requestTitle = requestTitleEdit.getText().toString();

                // FLAW: Location and description are swapped
                String requestLocation = descriptionTextEdit.getText().toString();
                String requestDescription = requestLocationEdit.getText().toString();

                int requestLengthHours = Integer.parseInt(hoursLengthSpinner.getSelectedItem().toString());
                int requestLengthMinutes = Integer.parseInt(minutesLengthSpinner.getSelectedItem().toString());
                int requestLengthMillis = requestLengthHours * 60 * 60 * 1000 + requestLengthMinutes * 60 * 1000;

				/* Create new help request on Parse */

                //Check that bundle information was retrieved properly
                Bundle extras = getIntent().getExtras(); //grabs the bundle
                if (extras == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.bundle_error),
                            Toast.LENGTH_LONG).show(); //this should never happen
                    progress.dismiss();
                    finish();
                }
                String course = extras.getString("selected_class"); //grabs the selected class
                if (course == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_class),
                            Toast.LENGTH_LONG).show(); //this should never happen
                    progress.dismiss();
                    finish();
                }
                Log.d("NewRequestActivity", "Course: " + course);

                //Check that Parse user was resolved properly
                user = ParseUser.getCurrentUser(); //grabs current Parse user
                if (user == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                            Toast.LENGTH_LONG).show();
                    progress.dismiss();
                    finish();
                }

                //Check that all fields were entered correctly
                if (requestTitle.isEmpty()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_title),
                            Toast.LENGTH_SHORT).show();
                    isSubmitting = false;
                    progress.dismiss();
                    return;
                } else if (requestLengthMillis <= 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_zero_time),
                            Toast.LENGTH_SHORT).show();
                    isSubmitting = false;
                    progress.dismiss();
                    return;
                } else if (requestLocation.isEmpty()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_location),
                            Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                    isSubmitting = false;
                    return;
                } else if (requestDescription.isEmpty()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_description),
                            Toast.LENGTH_SHORT).show();
                    isSubmitting = false;
                    progress.dismiss();
                    return;
                }

                //Check if user is in a group, and prompt if so. Then create the request.
                checkAndProceed(course, requestTitle, requestDescription, requestLocation, requestLengthMillis);

            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        requestTitleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                requestTitleLength.setText(requestTitleEdit.length() + "/20");
            }
        });

        descriptionTextEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                descriptionLength.setText(descriptionTextEdit.length() + "/200");
            }
        });

        requestLocationEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                requestLocationLength.setText(requestLocationEdit.length() + "/150");
            }
        });

        gps.startGPS(15000, 10);
    }

    @Override
    public void onPause() {
        super.onPause();
        gps.stopGPS();
    }

    /**
     * Checks if user is in a group, and
     * prompt if so. Also check if group
     * will be deleted, and prompt.
     *
     * By Tyler
     */
    private void checkAndProceed(final String course, final String requestTitle, final String requestDescription, final String requestLocation, final int requestLengthMillis) {
        //progress = ProgressDialog.show(NewRequestActivity.this, "Adding your request...", "Please wait...", true);
        //check if user is in a group
        currentGroup = (ParseObject) ParseUser.getCurrentUser().get("currentRequest");

		try {
			if(currentGroup != null) {
				currentGroup.fetchIfNeeded();
				ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
				query.get(currentGroup.getObjectId());
			}
		} catch (ParseException e) {
			if(e.getCode() == ParseException.OBJECT_NOT_FOUND) {
				ParseUser.getCurrentUser().remove("currentRequest");
				ParseUser.getCurrentUser().put("isHelper", false);
				ParseUser.getCurrentUser().remove("cacheHelpers");
				ParseUser.getCurrentUser().remove("cacheMembers");
				try {
					ParseUser.getCurrentUser().save();
					currentGroup = null;
					ParseUser.getCurrentUser().pinInBackground();
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}


        if (currentGroup != null) {
            progress.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(NewRequestActivity.this);

            builder.setMessage(getString(R.string.request_leave_other_group_with_name, currentGroup.get("title")))
                    .setTitle(getString(R.string.request_already_in_group_warning));

            builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //proceed, and check if it will delete group
                    progress = ProgressDialog.show(NewRequestActivity.this, "Leaving group...", "Please wait...", true);
                    try {
                        //fetch number of group members from server, delete the request object if the current user is the only member
                        ParseQuery<ParseUser> memberQuery = ParseUser.getQuery();
                        memberQuery.whereEqualTo("currentRequest", currentGroup);
                        if (memberQuery.count() <= 1) {
                            //Prompt the user to let them know it will delete the group
                            progress.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewRequestActivity.this);
                            builder.setMessage(getString(R.string.request_leave_delete_warning))
                                    .setTitle(getString(R.string.request_leave_delete, currentGroup.get("title")));

                            builder.setPositiveButton(getString(R.string.confirm_option), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Delete group and continue to leave
                                    progress = ProgressDialog.show(NewRequestActivity.this, "Leaving group...", "Please wait...", true);
                                    boolean success = true;

                                    currentGroup.deleteInBackground(new DeleteCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if(e==null){
                                                progress.dismiss();
                                                progress = ProgressDialog.show(NewRequestActivity.this, "Adding your request...", "Please wait...", true);
                                                //create the request
                                                createRequest(course, requestTitle, requestDescription, requestLocation, requestLengthMillis);
                                            }
                                            else{
                                                e.printStackTrace();
                                                progress.dismiss();
                                                Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                                                        Toast.LENGTH_SHORT).show();
                                                //do nothing
                                            }
                                        }
                                    });
                                   
                                }
                            });

                            builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //User doesn't leave the group, don't proceed
                                    //do nothing
                                }
                            });

							AlertDialog dialogMessage = builder.create();
							dialogMessage.setOnDismissListener(new DialogInterface.OnDismissListener() {
								@Override
								public void onDismiss(DialogInterface dialog) {
									isSubmitting = false;
								}
							});
							dialogMessage.show();
                        } else {
                            //Leaving group will not cause it to be deleted, don't prompt user.
                            progress.dismiss();
                            progress = ProgressDialog.show(NewRequestActivity.this, "Adding your request...", "Please wait...", true);
                            //create the request
                            createRequest(course, requestTitle, requestDescription, requestLocation, requestLengthMillis);
                        }

                    } catch (ParseException e) {
                        Log.e("RequestInfoFragment", "Error deleting help request when user leaves");
                        e.printStackTrace();
                        progress.dismiss();
                        Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                                Toast.LENGTH_SHORT).show();
                        //do nothing
                    }
                }
            });

            builder.setNegativeButton(getString(R.string.cancel_option), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Don't proceed
                    //do nothing
                }
            });

			AlertDialog dialog = builder.create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					isSubmitting = false;
				}
			});
			dialog.show();

        } else {
            //not in a group, simply proceed
            //create the request
            createRequest(course, requestTitle, requestDescription, requestLocation, requestLengthMillis);
        }

        //stop.release();
    }

    private void setProceed(boolean set){
        proceed = set;
    }

    private void createRequest(String course, String requestTitle, String requestDescription, String requestLocation, int requestLengthMillis){
        //Grab class object from database
        //ParseObject courseObj = null;
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
        query.whereContains("courseNumber", course);
        try {
            courseObj = query.getFirst();
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                    Toast.LENGTH_SHORT).show();
            isSubmitting = false;
            progress.dismiss();
            return;
        }

        //Create the HelpRequest object in Parse
        request = new ParseObject("HelpRequest");
        request.put("course", courseObj);
        request.put("title", requestTitle);
        request.put("description", requestDescription);
        request.put("locationDescription", requestLocation);
        Location loc = gps.getLocation();
        ParseGeoPoint point;
        if (loc != null) {
            point = new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.location_error),
                    Toast.LENGTH_SHORT).show();
            progress.dismiss();
            isSubmitting = false;
            return;
        }
        request.put("geoLocation", point);
        request.put("duration", requestLengthMillis);

        Log.d("request", "request = " + request.toString());

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					request.save();

					ParseRelation<ParseObject> requests = courseObj.getRelation("requests");
					requests.add(request);
					courseObj.save();

					user.put("currentRequest", request);
					user.put("isHelper", false);    //assume if creating a request, they are not a helper.
					user.save();

					Intent result = new Intent();
					result.putExtra(RequestListActivity.REFRESH_REQUEST_LIST, true);
					setResult(RESULT_OK, result);

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), R.string.new_request_success,
									Toast.LENGTH_SHORT).show();
							progress.dismiss();
							finish();
						}
					});
				} catch (ParseException e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), getString(R.string.network_error),
									Toast.LENGTH_SHORT).show();
							progress.dismiss();
						}
					});
				}
			}
		}).start();
    }

}