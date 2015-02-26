package com.cs408.studybuddy;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Evan on 2/26/2015.
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

		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		//get all request info from server
		Bundle extras = getIntent().getExtras(); //grabs the bundle

		Fragment info = new RequestInfoFragment();
		info.setArguments(extras);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.info_fragment, info);
		ft.commit();
	}
}
