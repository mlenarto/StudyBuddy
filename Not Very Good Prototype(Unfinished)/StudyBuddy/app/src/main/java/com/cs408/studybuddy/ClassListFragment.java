package com.cs408.studybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Evan on 2/6/2015.
 */
public class ClassListFragment extends Fragment {
	private View view;
    private ArrayList<String> classes;
    public SharedPreferences prefs;
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_class_list, container, false);
		ListView classList = (ListView) view.findViewById(R.id.class_list);

        prefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());

        //Grab the class list from shared prefs and convert it back into an array
        classes = ClassAddActivity.convertToArray(prefs.getString("classes", "No Classes"));
		/*String[] classes = new String[] {
			"CS 408",
			"CS 381",
			"PHYS 221",
			"SPAN 480"
		};*/

		classList.setAdapter(new ArrayAdapter<>(view.getContext(),
				android.R.layout.simple_list_item_1, android.R.id.text1, classes));

		classList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startActivity(new Intent(getActivity(), RequestListActivity.class));
			}
		});

		return view;
	}
}
