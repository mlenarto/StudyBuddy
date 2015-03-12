package com.cs408.studybuddy;

import android.app.ProgressDialog;
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

import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

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
    private ParseObject request, courseObj;
    private ParseUser user;
	private LocationService gps;
    private ProgressDialog progress;
	private boolean isSubmitting = false;


	public void onCreate(Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_request);

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mTitle = (TextView) findViewById(R.id.title_text);
		requestTitleEdit = (EditText) findViewById(R.id.request_title);
		requestTitleLength = (TextView)findViewById(R.id.request_title_length);
		descriptionTextEdit = (EditText)findViewById(R.id.description);
		descriptionLength = (TextView)findViewById(R.id.description_length);
		requestLocationEdit = (EditText)findViewById(R.id.request_location);
		requestLocationLength = (TextView)findViewById(R.id.location_length);
		final Spinner hoursLengthSpinner = (Spinner)findViewById(R.id.request_duration_hours);
		final Spinner minutesLengthSpinner = (Spinner)findViewById(R.id.request_duration_minutes);
		Button submit = (Button)findViewById(R.id.submit_button);

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
				if(isSubmitting)
					return;

				isSubmitting = true;
				String requestTitle = requestTitleEdit.getText().toString();
				String requestDescription = descriptionTextEdit.getText().toString();
                String requestLocation = requestLocationEdit.getText().toString();
				int requestLengthHours = Integer.parseInt(hoursLengthSpinner.getSelectedItem().toString());
				int requestLengthMinutes = Integer.parseInt(minutesLengthSpinner.getSelectedItem().toString());
				int requestLengthMillis = requestLengthHours*60*60*1000 + requestLengthMinutes*60*1000;

				/* Create new help request on Parse */

                //Check that bundle information was retrieved properly
                Bundle extras = getIntent().getExtras(); //grabs the bundle
                if(extras == null){
                    Toast.makeText(getApplicationContext(), getString(R.string.bundle_error),
                            Toast.LENGTH_LONG).show(); //this should never happen
                    progress.dismiss();
                    finish();
                }
                String course = extras.getString("selected_class"); //grabs the selected class
                if(course == null){
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_class),
                            Toast.LENGTH_LONG).show(); //this should never happen
                    progress.dismiss();
                    finish();
                }
                Log.d("NewRequestActivity", "Course: " + course);

                //Check that Parse user was resolved properly
                user = ParseUser.getCurrentUser(); //grabs current Parse user
                if(user == null){
                    Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                            Toast.LENGTH_LONG).show();
                    progress.dismiss();
                    finish();
                }

                //Check that all fields were entered correctly
                if(requestTitle.isEmpty()){
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_title),
                            Toast.LENGTH_SHORT).show();
					isSubmitting = false;
                    progress.dismiss();
                    return;
                }
                else if(requestLengthMillis <= 0){
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_zero_time),
                            Toast.LENGTH_SHORT).show();
					isSubmitting = false;
                    progress.dismiss();
                    return;
                }
                else if(requestLocation.isEmpty()){
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_location),
                            Toast.LENGTH_SHORT).show();
                    progress.dismiss();
					isSubmitting = false;
                    return;
                }
                else if(requestDescription.isEmpty()){
                    Toast.makeText(getApplicationContext(), getString(R.string.new_request_error_description),
                            Toast.LENGTH_SHORT).show();
					isSubmitting = false;
                    progress.dismiss();
                    return;
                }

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
				if(loc != null) {
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
                request.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null){
                            //request saved properly
                            /*add the HelpRequest to the course's object*/
                            ParseRelation<ParseObject> requests = courseObj.getRelation("requests");
                            requests.add(request);
                            courseObj.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if(e == null){
                                        //course saved properly.
                                         /*add the HelpRequest to the user's object*/
                                        user.put("currentRequest", request);
                                        user.put("isHelper", false);    //assume if creating a request, they are not a helper.
                                        user.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                if(e == null){
                                                    //user saved properly.
													Intent result = new Intent();
													result.putExtra(RequestListActivity.CREATED_NEW_REQUEST, true);
													setResult(RESULT_OK, result);

                                                    Toast.makeText(getApplicationContext(), R.string.new_request_success,
                                                            Toast.LENGTH_SHORT).show();
                                                    progress.dismiss();
													finish();
                                                } else{
                                                    //user was not saved properly.
                                                    e.printStackTrace();
                                                    Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                                                            Toast.LENGTH_SHORT).show();
                                                    progress.dismiss();
													finish();
                                                }
                                            }
                                        });
                                    } else{
                                        //course was not saved properly.
                                        e.printStackTrace();
                                        progress.dismiss();
                                        Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                                                Toast.LENGTH_SHORT).show();
										finish();
                                    }
                                }
                            });
                        }else{
                            //request didn't save properly
                            e.printStackTrace();
                            progress.dismiss();
                            Toast.makeText(getApplicationContext(), getString(R.string.network_error),
                                    Toast.LENGTH_SHORT).show();
							finish();
                        }
                    }
                });
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
}
