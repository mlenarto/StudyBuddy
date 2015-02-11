package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Created by Jacob on 2/9/2015.
 */

public class ClassAddActivity extends ActionBarActivity
{
    public EditText newClass;
    public ListView classList;
    private Toolbar mToolbar;
    public SharedPreferences prefs;
    classAdapter arrayAdapter;
    List<String> classes;
    String newCourse;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_add);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
		classList = (ListView) findViewById(R.id.classList);
		newClass = (EditText) findViewById(R.id.newClass);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		prefs = getSharedPreferences(getResources().getString(R.string.app_preferences), 0);

		String classString = prefs.getString("class_list", null);
		if(classString != null && !classString.isEmpty()) {
			classes = new ArrayList<>(Arrays.asList(classString.split(",")));
			Collections.sort(classes);
		} else {
			classes = new ArrayList<>();
		}

		arrayAdapter = new classAdapter(this, R.layout.fragment_class_add_item, classes);
		classList.setAdapter(arrayAdapter);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

        setupConfirmButton();
    }

    public void setupConfirmButton()
    {
        Button confirm = (Button) findViewById(R.id.confirmClasses);

        confirm.setOnClickListener(new View.OnClickListener()
            {
                @Override
                //Hopefully will dynamically add elements to the list view, no checks for phony classes yet
                public void onClick(View arg0)
                {
                    //TODO: Select class from a drop-down, rather than typing it in.
                    newCourse = newClass.getText().toString();

					String classString = prefs.getString("class_list", null);
					SharedPreferences.Editor edit = prefs.edit();

					if(classString != null && !classString.isEmpty()) {
						classString += "," + newCourse;
						edit.putString("class_list", classString);
					} else {
						edit.putString("class_list", newCourse);
					}
					edit.commit();

                    //TODO: Handle the case where we fail to add the course on the server, so we don't add it locally.

					classes.add(newCourse);
					Collections.sort(classes);
					arrayAdapter.notifyDataSetChanged();

					newClass.setText("");

                    /* Add class to the user's course list on database */
                    //Grab class object from database
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
                    query.whereContains("courseNumber", newCourse);
                    query.getFirstInBackground(new GetCallback<ParseObject>() {
                        public void done(ParseObject classObj, ParseException e) {
                            if (e == null) {
                                //found the class
                                Log.d("ClassAddActivity", "Class: " + classObj.getString("courseName"));
                                //TODO: Check if the user already has this course in their courseList
                                //add it to the user's course list
                                ParseUser userObj = ParseUser.getCurrentUser();
                                ParseRelation<ParseObject> relation = userObj.getRelation("courseList");
                                relation.add(classObj);
                                userObj.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null){
                                            //user saved properly.
                                            Toast.makeText(getApplicationContext(), newCourse + " was added!",
                                                    Toast.LENGTH_SHORT).show();
                                        } else{
                                            //course was not saved properly.
                                            e.printStackTrace();
                                            Toast.makeText(getApplicationContext(), "Error: Cannot save user in database.",
                                                    Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }
                                });

                            } else {
                                //didn't find the class (should never happen once we pick classes from a drop-down)
                                Log.d("ClassAddActivity", "Error: " + e.getMessage());
                                Toast.makeText(getApplicationContext(), "Error: Class not found.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        );
    }

    public static String convertToString(ArrayList<String> list)
    {
        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String s : list)
        {
            sb.append(delim);
            sb.append(s);
            delim = ",";
        }
        return sb.toString();
    }

    //USE THIS TO CONVERT FROM SHARED PREFS BACK INTO THE ARRAYLIST
    public static ArrayList<String> convertToArray(String string)
    {

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(string.split(",")));
        return list;
    }

	private class classAdapter extends ArrayAdapter<String> {

		int resource;

		public classAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);

			this.resource = resource;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			TextView classNameText;
			Button remove;
			final String className;

			if(v == null) {
				LayoutInflater li = LayoutInflater.from(getContext());
				v = li.inflate(resource, parent, false);
			}


			classNameText = (TextView)v.findViewById(R.id.class_name);
			remove = (Button)v.findViewById(R.id.remove_class);

			className = this.getItem(position);
			classNameText.setText(className);


			remove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

					builder.setMessage("Are you sure you want to leave this class?")
							.setTitle("Leave " + className + "?");

					builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String classesString = prefs.getString("class_list", null);
							SharedPreferences.Editor edit = prefs.edit();

							int start = classesString.indexOf(className);

							if(classes.size() == 1) {
								edit.putString("class_list", "");
							} else if(start == 0) {		//Name is at the start of the array
								classesString = classesString.substring(className.length()+1, classesString.length());
								edit.putString("class_list", classesString);
							} else if((start + className.length()) == classesString.length()) {
								classesString = classesString.substring(0, start-1);
								edit.putString("class_list", classesString);
							} else {
								classesString = classesString.substring(0, start-1) +
										classesString.substring(start + className.length(), classesString.length());
								edit.putString("class_list", classesString);
							}
							edit.commit();

							classes.remove(className);
							Collections.sort(classes);
							arrayAdapter.notifyDataSetChanged();

							//TODO: remove class on server side
						}
					});

					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//Do nothing
						}
					});

					builder.create().show();
				}
			});

			return v;
		}
	}
}
