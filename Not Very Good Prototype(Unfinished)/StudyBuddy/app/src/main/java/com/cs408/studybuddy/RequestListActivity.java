package com.cs408.studybuddy;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;

/**
 * Created by Evan on 2/8/2015.
 */
public class RequestListActivity extends ActionBarActivity {
    private static String course;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_list);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.title_text);
		Button newRequest = (Button) findViewById(R.id.newRequest);

		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		mText.setText(R.string.title_Request_List);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            course = extras.getString("selected_class");
            /*ArrayList<ParseObject> helpRequestList = (ArrayList<ParseObject>) ParseUser.getCurrentUser().get("helpRequests");

             */
        }

		ListView classList = (ListView) findViewById(R.id.class_list);

		String[] classes = new String[] {
				"First homework assignment",
				"First quiz",
				"Help with first homework",
				"Problem 3?"
		};

		classList.setAdapter(new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, classes));

		newRequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                Intent i = new Intent(RequestListActivity.this, NewRequestActivity.class);
                i.putExtra("class_selected", course);
                startActivity(i);
			}
		});

		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}
}
