package com.cs408.studybuddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evan on 2/8/2015.
 */
public class RequestListActivity extends ActionBarActivity {
    private static String course;
    private List<String> requests = new ArrayList<>();

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
            Log.d("RequestListActivity", "Selected class is " + course);

        }
        else
        {
            Log.d("RequestListActivity", "EXTRAS IS NULL :(");
        }

		String[] classes = new String[] {
				"First homework assignment",
				"First quiz",
				"Help with first homework",
				"Problem 3?"
		};

        //Retrieve helpRequest list from Parse
        ParseQuery<ParseObject> query = ParseQuery.getQuery("HelpRequest");
        query.whereContains("course", course);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> requestList, ParseException e)
            {
                if (e == null)
                {
                    for (ParseObject request : requestList)
                    {
                        requests.add(request.getString("title"));
                    }
                    Log.d("RequestListActivity", "Retrieved " + requestList.size() + " help requests");

                    if (!requests.isEmpty())
                    {
                        ListView classList = (ListView) findViewById(R.id.class_list);
                        //TODO: Can someone take a look at the code below (commented out), I can't resolve the error
                        /*
                        classList.setAdapter(new ArrayAdapter<>(this,
                                android.R.layout.simple_list_item_1, android.R.id.text1, requests));
*/
                        classList.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                startActivity(new Intent(RequestListActivity.this, RequestInfoActivity.class));
                            }
                        });
                    }
                    else
                    {
                        Log.d("RequestListActivity", "Requests ArrayList is empty.");
                    }
                }
                else
                {
                    Log.d("RequestListActivity", "Error: " + e.getMessage());
                }
            }
        });


		newRequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                Intent i = new Intent(RequestListActivity.this, NewRequestActivity.class);
                i.putExtra("selected_class", course);
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
