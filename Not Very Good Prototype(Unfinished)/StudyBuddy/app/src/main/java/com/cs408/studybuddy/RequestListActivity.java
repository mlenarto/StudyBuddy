package com.cs408.studybuddy;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cs408.studybuddy.R;

/**
 * Created by Evan on 2/8/2015.
 */
public class RequestListActivity extends ActionBarActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_list);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.titleText);

		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		mText.setText(R.string.title_Request_List);

		ListView classList = (ListView) findViewById(R.id.class_list);

		String[] classes = new String[] {
				"First homework assignment",
				"First quiz",
				"Help with first homework",
				"Problem 3?"
		};

		classList.setAdapter(new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, classes));


		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}
}
