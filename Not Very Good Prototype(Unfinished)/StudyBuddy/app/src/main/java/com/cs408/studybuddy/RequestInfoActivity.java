package com.cs408.studybuddy;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Evan on 2/9/2015.
 */
public class RequestInfoActivity extends ActionBarActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_info);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.title_text);


		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		mText.setText(R.string.title_request_info);

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
			//Do Network Stuff here
			return new String[]{
					"Example Title",
					"1 hour 25 minutes",
					"5 members",
					"Lawson Commons, round table under the TVs. I have my laptop out right now.",
					"This is an example of a somewhat long description that takes multiple lines. " +
							" Now a little bit longer to try to make the page scroll so I can see it. " +
							" Just a little bit more and it'll work"
			};
		}

		//We can use this method to change the UI after the background task is finished
		//(onPreExecute can be used as well, for set up before the background task)
		protected  void onPostExecute(String[] result) {
			TextView requestTitle = (TextView) findViewById(R.id.request_title);
			TextView timeRemaining = (TextView) findViewById(R.id.request_time);
			TextView memberCount = (TextView) findViewById(R.id.member_count);
			TextView requestLocation = (TextView) findViewById(R.id.request_location);
			TextView requestDescription = (TextView) findViewById(R.id.request_description);

			requestTitle.setText(result[0]);
			timeRemaining.setText(result[1]);
			memberCount.setText(result[2]);
			requestLocation.setText(result[3]);
			requestDescription.setText(result[4]);
		}
	}
}
