package com.cs408.studybuddy;

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

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;

/**
 * Created by Evan on 2/8/2015.
 */
public class NewRequestActivity extends ActionBarActivity {

	Toolbar mToolbar;
	EditText requestTitleEdit;
	TextView requestTitleLength;
	EditText descriptionTextEdit;
	TextView descriptionLength;
	EditText requestLocationEdit;
	TextView requestLocationLength;
    ParseObject request, courseObj;
    ParseUser user;


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

		submit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
                    Toast.makeText(getApplicationContext(), "Error: Cannot find extras bundle.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                String course = extras.getString("selected_class"); //grabs the selected class
                if(course == null){
                    Toast.makeText(getApplicationContext(), "Error: Cannot find selected class.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                Log.d("NewRequestActivity", "Course: " + course);

                //Check that Parse user was resolved properly
                user = ParseUser.getCurrentUser(); //grabs current Parse user
                if(user == null){
                    Toast.makeText(getApplicationContext(), "Error: Cannot find Parse user.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }

                //Check that all fields were entered correctly
                if(requestTitle.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a title.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(requestLengthMillis <= 0){
                    Toast.makeText(getApplicationContext(), "Please enter a valid duration.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(requestLocation.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter your location.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(requestDescription.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a task description.",
                            Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getApplicationContext(), "Error: Cannot get class from database.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                //Create the HelpRequest object in Parse
                request = new ParseObject("HelpRequest");
                request.put("course", courseObj);
                request.put("title", requestTitle);
                request.put("description", requestDescription);
                request.put("locationDescription", requestLocation);
                ParseGeoPoint point = new ParseGeoPoint(30.0, -20.0);   //TODO: grab the user's actual coordinates
                request.put("geoLocation", point);
                request.put("duration", requestLengthMillis);
                ParseRelation<ParseUser> members = request.getRelation("members");
                members.add(user);
                //request.put("user", user);
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
                                        //no toast here because one appears when below save is complete
                                    } else{
                                        //course was not saved properly.
                                        e.printStackTrace();
                                        Toast.makeText(getApplicationContext(), "Error: Cannot save course in database.",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            });

                            /*add the HelpRequest to the user's object*/
                            user.put("currentRequest", request);
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if(e == null){
                                        //user saved properly.
                                        Toast.makeText(getApplicationContext(), "Your request was created!",
                                                Toast.LENGTH_SHORT).show();
                                    } else{
                                        //user was not saved properly.
                                        e.printStackTrace();
                                        Toast.makeText(getApplicationContext(), "Error: Cannot save user in database.",
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            });

                        }else{
                            //request didn't save properly
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Error: Cannot save request in database.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                });


                finish();

			}
		});

	}

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
	}
}
