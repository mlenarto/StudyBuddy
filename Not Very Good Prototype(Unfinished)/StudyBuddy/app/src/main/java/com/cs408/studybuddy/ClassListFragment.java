package com.cs408.studybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Evan on 2/6/2015.
 */
public class ClassListFragment extends Fragment {
	private View view;
	private ListView classList;
    private SharedPreferences prefs;
	private ArrayAdapter<String> classAdapter;
	private String classesString;

    private List<String> courses = new ArrayList<>();

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_class_list, container, false);
		classList = (ListView) view.findViewById(R.id.class_list);

		//If a preference should be available in the entire app, open it like this
		prefs = getActivity().getSharedPreferences(getResources().getString(R.string.app_preferences), 0);
/*
        ParseRelation<ParseObject> courseListRelation = ParseUser.getCurrentUser().getRelation("courseList");
        courseListRelation.getQuery().findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> courseList, ParseException e) {
                if (e != null) {
                    // There was an error
                    Log.d("ClassListFragment" , "Error retrieving course list for user");

                } else
                {
                    // courseList contains all Course objects related to user
                    Log.d("ClassListFragment" , "Success retrieving course list");
                }
            }
        });
*/

		//Retrieve courseList from Parse
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> courseList, ParseException e)
			{
				if (e == null)
				{
					courses.clear();
					classesString = "";
					for (ParseObject course : courseList)
					{
						courses.add(course.getString("courseNumber"));
//						Log.d("ClassListFragment" , "Course Number: "+ course.getString(("courseNumber")));
						classesString += course.getString("courseNumber") + ",";
					}
					Log.d("ClassListFragment", "Retrieved " + courseList.size() + " courses");

					if (!courses.isEmpty())
					{
						Collections.sort(courses);
						classAdapter = new ArrayAdapter<>(view.getContext(),
								android.R.layout.simple_list_item_1, android.R.id.text1, courses);

						classList.setAdapter(classAdapter);

						classList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
								Intent i = new Intent(getActivity(), RequestListActivity.class);
								i.putExtra("selected_class", classAdapter.getItem(position));
								Log.d("class", "opened " + classAdapter.getItem(position) + ", class = " + classAdapter.getItem(position).getClass());
								startActivity(i);
							}
						});

						classesString = classesString.substring(0, classesString.length()-1);	//Remove end comma
						SharedPreferences.Editor edit = prefs.edit();
						edit.putString("class_list", classesString);
						edit.commit();
					}
					else
					{
						Log.d("ClassListFragment", "Courses ArrayList is empty.");
					}
				}
				else
				{
					Log.d("ClassListFragment", "Error: " + e.getMessage());
				}
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		courses.clear();
		if(prefs.contains("class_list")) {
			classesString = prefs.getString("class_list", null);
			if(classesString != null) {
				courses = new ArrayList<>(Arrays.asList(classesString.split(",")));
				Collections.sort(courses);
			} else {
				courses.add("No classes");
			}
		} else {
			courses.add("No classes");
		}


		classAdapter = new ArrayAdapter<>(view.getContext(),
				android.R.layout.simple_list_item_1, android.R.id.text1, courses);
		classList.setAdapter(classAdapter);
		classAdapter.notifyDataSetChanged();

		classList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (!prefs.contains("class_list")) {
					Intent i = new Intent(getActivity(), ClassAddActivity.class);
					startActivity(i);
				} else {
					Intent i = new Intent(getActivity(), RequestListActivity.class);
					i.putExtra("selected_class", classAdapter.getItem(position));
					startActivity(i);
				}
			}
		});
	}
}
