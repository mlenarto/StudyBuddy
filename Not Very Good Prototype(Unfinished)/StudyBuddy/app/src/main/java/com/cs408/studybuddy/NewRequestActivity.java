package com.cs408.studybuddy;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by Evan on 2/8/2015.
 */
public class NewRequestActivity extends ActionBarActivity {

	Toolbar mToolbar;
	EditText requestTitleEdit;
	TextView requestTitleLength;
	EditText descriptionTextEdit;
	TextView descriptionLength;


	public void onCreate(Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_request);

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mTitle = (TextView) findViewById(R.id.title_text);
		requestTitleEdit = (EditText) findViewById(R.id.request_title);
		requestTitleLength = (TextView)findViewById(R.id.request_title_length);
		descriptionTextEdit = (EditText)findViewById(R.id.description);
		descriptionLength = (TextView)findViewById(R.id.description_length);
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
				int requestLengthHours = Integer.parseInt(hoursLengthSpinner.getSelectedItem().toString());
				int requestLengthMinutes = Integer.parseInt(minutesLengthSpinner.getSelectedItem().toString());
				int requestLengthMillis = requestLengthHours*60*60*1000 + requestLengthMinutes*60*1000;

				//Do server stuff with this
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


	}
}
