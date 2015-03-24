package com.cs408.studybuddy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
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
import java.util.List;

/**
 * Created by Evan on 2/8/2015.
 */
public class RequestListActivity extends ActionBarActivity {
	TextView noRequestText;

	private static final int NEW_REQUEST_INTENT = 1;
	public static final String CREATED_NEW_REQUEST = "created_new_request";

    private static String course = new String();
    private ArrayList<ClassRequest> requests = new ArrayList<ClassRequest>();

    private static String course_obj_id = new String();
	private LocationService gps;
	private Handler handler;
    private SwipeRefreshLayout swipeLayout;
    private ProgressDialog progress;
	Runnable runnable;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_list);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView mText = (TextView) findViewById(R.id.title_text);
		Button newRequest = (Button) findViewById(R.id.newRequest);
		noRequestText = (TextView) findViewById(R.id.no_requests);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
        setupSwipeLayout();
		mText.setText(R.string.title_Request_List);

		gps = LocationService.getInstance(getApplicationContext());
		gps.startGPS(15*1000, 15);

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

		handler = new Handler(getMainLooper());
        progress = ProgressDialog.show(RequestListActivity.this, "Loading requests...", "Please wait...", true);
		updateList();

		newRequest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(RequestListActivity.this, NewRequestActivity.class);
				i.putExtra("selected_class", course);
				startActivityForResult(i, NEW_REQUEST_INTENT);
			}
		});

		mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
	}

    private void setupSwipeLayout()
    {
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(true);
				progress.show();
                updateList();
                swipeLayout.setRefreshing(false);
            }
        });
    }

	private void updateList()
    {
		if(!gps.isConnected())
			gps.startGPS(15 * 1000, 15);

		gps.getLocationInBackground(20, new LocationService.locationUpdateListener()
        {
			@Override
			public void onLocationObtained(final Location loc)
            {
				if (loc != null)
                {
					// Inner query to grab course pointer so we pull help requests for the specific course
					ParseQuery innerQuery = new ParseQuery("Course");
					innerQuery.whereEqualTo("courseNumber", course);

					//Retrieve helpRequest list from Parse
					ParseQuery<ParseObject> request_query = ParseQuery.getQuery("HelpRequest");
					request_query.whereMatchesQuery("course", innerQuery);
					request_query.findInBackground(new FindCallback<ParseObject>()
                    {
						public void done(List<ParseObject> requests, ParseException e)
                        {
							if (e == null)
                            {
								RequestListActivity.this.requests.clear();
								for (ParseObject request : requests)
                                {
									ClassRequest nextReq = new ClassRequest(request.getString("title"), request.getObjectId());
									double distance = gps.distance(loc.getLatitude(), loc.getLongitude(),
											request.getParseGeoPoint("geoLocation").getLatitude(), request.getParseGeoPoint("geoLocation").getLongitude());
									nextReq.setDistance(distance);
									RequestListActivity.this.requests.add(nextReq);
								}
								Log.d("RequestListActivity", "Retrieved " + requests.size() + " help requests");

								if (!RequestListActivity.this.requests.isEmpty())
                                {
									noRequestText.setVisibility(View.GONE);
									ListView requestList = (ListView) findViewById(R.id.class_list);

                                    //This makes sure the list of requests will not refresh if the list is longer than one screen
                                    requestList.setOnScrollListener(new AbsListView.OnScrollListener()
                                    {
                                        @Override
                                        public void onScrollStateChanged(AbsListView view, int scrollState)
                                        {
                                        }

                                        @Override
                                        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
                                        {

                                            if (firstVisibleItem == 0)
                                            {
                                                swipeLayout.setEnabled(true);
                                            }

                                            else
                                            {
                                                swipeLayout.setEnabled(false);
                                            }

                                        }
                                    });

									//sort list by location
									Collections.sort(RequestListActivity.this.requests);

									requestList.setAdapter(new ArrayAdapter<>(RequestListActivity.this,
											android.R.layout.simple_list_item_1, android.R.id.text1, RequestListActivity.this.requests));

									requestList.setOnItemClickListener(new AdapterView.OnItemClickListener()
                                    {
										@Override
										public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                                        {
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
                                    progress.dismiss();
									noRequestText.setVisibility(View.VISIBLE);
									Log.d("RequestListActivity", "Requests ArrayList is empty.");
								}

							}

                            else
                            {
								Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.error_getting_requests), Toast.LENGTH_LONG);
								toast.setGravity(Gravity.CENTER, 0, 0);
                                progress.dismiss();
								toast.show();
								Log.d("RequestListActivity", "Error: " + e.getMessage());
							}
                            progress.dismiss();
						}
					});
                    progress.dismiss();
				}

                else
                {
					Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.location_error), Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
                    progress.dismiss();
					toast.show();
					Log.d("RequestListActivity", "Error obtaining Location");
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		gps.startGPS(15*1000, 15);

		runnable = new Runnable() {
			public void run() {
				updateList();
				handler.postDelayed(this, 30 * 1000);
			}
		};

		handler.postDelayed(runnable, 30 * 1000);
	}

	@Override
	public void onPause() {
		super.onPause();
		gps.stopGPS();
		handler.removeCallbacks(runnable);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == NEW_REQUEST_INTENT && resultCode == RESULT_OK) {

			if(data.getBooleanExtra(CREATED_NEW_REQUEST, false)) {
				progress.show();
				updateList();
			}
		}
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
