package com.cs408.studybuddy;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Evan on 2/8/2015.
 */
public class RequestListActivity extends ActionBarActivity {
    private static String course = new String();
    //private List<String> requests = new ArrayList<>();
   // private List<String> requests_id = new ArrayList<>(); //Parse object ID for requests
    private ArrayList<ClassRequest> requests = new ArrayList<ClassRequest>();

    private static String course_obj_id = new String();
	private LocationService gps;
	private Location mLocation;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_list);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.title_text);
		Button newRequest = (Button) findViewById(R.id.newRequest);
		final TextView noRequestText = (TextView) findViewById(R.id.no_requests);

		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		mText.setText(R.string.title_Request_List);

		gps = LocationService.getInstance(this);
		gps.startGPS(10*1000, 5);

		noRequestText.setVisibility(View.GONE);
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

        // Inner query to grab course pointer so we pull help requests for the specific course
        ParseQuery innerQuery = new ParseQuery("Course");
        innerQuery.whereEqualTo("courseNumber", course);

        //Retrieve helpRequest list from Parse
        ParseQuery<ParseObject> request_query = ParseQuery.getQuery("HelpRequest");
        request_query.whereMatchesQuery("course", innerQuery);
        request_query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> requests, ParseException e)
            {
                if (e == null)
                {
                    Location loc = gps.getLocation();

                    for (ParseObject request : requests)
                    {
                        //RequestListActivity.this.requests.add(request.getString("title"));  //save request title
                        //RequestListActivity.this.requests_id.add(request.getObjectId());    //save object ID
                        ClassRequest nextReq = new ClassRequest(request.getString("title"), request.getObjectId());
                        double distance = gps.distance(loc.getLatitude(), loc.getLongitude(),
                                request.getParseGeoPoint("geoLocation").getLatitude(), request.getParseGeoPoint("geoLocation").getLongitude());
                        nextReq.setDistance(distance);
                        RequestListActivity.this.requests.add(nextReq);
                    }
                    Log.d("RequestListActivity", "Retrieved " + requests.size() + " help requests");

                    if (!RequestListActivity.this.requests.isEmpty())
                    {
                        ListView requestList = (ListView) findViewById(R.id.class_list);

                        //sort list by location
                        Collections.sort(RequestListActivity.this.requests);

                        requestList.setAdapter(new ArrayAdapter<>(RequestListActivity.this,
								android.R.layout.simple_list_item_1, android.R.id.text1, RequestListActivity.this.requests));

                        requestList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Intent i = new Intent(RequestListActivity.this, RequestInfoActivity.class);
                                i.putExtra("request_title", RequestListActivity.this.requests.get(position).title);
                                i.putExtra("request_id", RequestListActivity.this.requests.get(position).id);
                                Log.d("RequestListActivity", "opened: " + RequestListActivity.this.requests.get(position));
                                startActivity(i);
								//startActivity(new Intent(RequestListActivity.this, RequestInfoActivity.class));
							}
						});
                    }
                    else
                    {
						noRequestText.setVisibility(View.VISIBLE);
                        Log.d("RequestListActivity", "Requests ArrayList is empty.");
                    }
                }
                else
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "An error occurred when retrieving the help requests for this course.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
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

	@Override
	public void onPause() {
		super.onPause();
		gps.stopGPS();
	}
}

/*
* Class for storing request info, and sorting by distance
* Hand-crafted with love by Tyler
 */
class ClassRequest implements Comparable{
    String title;
    String id;
    double distance;

    ClassRequest(String title, String id){
        this.title = title;
        this.id = id;
    }

    void setDistance(double distance){
        this.distance = distance;
    }

    @Override
    public int compareTo(Object another) {
        ClassRequest o = (ClassRequest)another;
        if(distance > o.distance)
            return 1;
        else if(distance < o.distance)
            return -1;
        else
            return 0;
    }

    @Override
    public String toString() {
        return title;
    }
}