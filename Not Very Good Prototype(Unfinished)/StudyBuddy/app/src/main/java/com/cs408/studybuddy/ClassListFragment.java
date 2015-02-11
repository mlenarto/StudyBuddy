package com.cs408.studybuddy;

import android.content.Intent;
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
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Evan on 2/6/2015.
 */
public class ClassListFragment extends Fragment {

	private View view;
    private List<String> courses = new ArrayList<>();

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_class_list, container, false);

        //Retrieve courseList from Parse
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> courseList, ParseException e)
            {
                if (e == null)
                {
                    for (ParseObject course : courseList)
                    {
                        courses.add(course.getString("courseNumber"));
                        Log.d("ClassListFragment" , "Course Number: "+ course.getString(("courseNumber")));
                    }
                    Collections.sort(courses);
                    Log.d("ClassListFragment", "Retrieved " + courseList.size() + " courses");

                    ListView classList = (ListView) view.findViewById(R.id.class_list);
                    if (!courses.isEmpty())
                    {
                        classList.setAdapter(new ArrayAdapter<>(view.getContext(),
                                android.R.layout.simple_list_item_1, android.R.id.text1, courses));

                        classList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Intent i = new Intent(getActivity(), RequestListActivity.class);
                                i.putExtra("selected_class", courses.get(position));
                                startActivity(i);
                            }
                        });
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
}
