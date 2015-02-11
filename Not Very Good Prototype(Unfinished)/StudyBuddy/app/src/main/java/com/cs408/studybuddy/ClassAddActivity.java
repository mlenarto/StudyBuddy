package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;


/**
 * Created by Jacob on 2/9/2015.
 */

public class ClassAddActivity extends ActionBarActivity
{
    public AutoCompleteTextView newClass;
    public ListView classList;
    private Toolbar mToolbar;
    public SharedPreferences prefs;
    classAdapter arrayAdapter;
    private List<String> classes;
    private List<String> allClasses;
    private Boolean is_valid;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_add);
        is_valid = false;
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
		classList = (ListView) findViewById(R.id.classList);
		newClass = (AutoCompleteTextView) findViewById(R.id.newClass);


        allClasses = readInMasterClassList();
        setupClassSelecter();

       /* for(String s : allClasses)
        {
            Log.v("masterclasslist",s);
        }*/

        //Adapter used for the autocomplete window
        ArrayAdapter completeAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,allClasses);
        newClass.setAdapter(completeAdapter);
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

    //This makes sure that the user picks from the list given in the autocomplete.  Keeps a variable that sets to null if the user does not select a correct class
    private void setupClassSelecter()
    {
        newClass.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Log.v("add class error", "Clicked on option");
                is_valid = true;
            }
        });

        newClass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                Toast toast;
                Log.v("add class error", "Should not add...");
                is_valid = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }


    //This reads in from a .txt file the master class list and returns it as an ArrayList (kinda crappy but it should only have to do this when a user wants to change classes)
    private ArrayList<String> readInMasterClassList()
    {
        List<String> c;
        c = new ArrayList<>();
        Scanner in;
        Resources res = getResources();
        InputStream in_s = res.openRawResource(R.raw.classes);

        InputStreamReader inputreader = new InputStreamReader(in_s);

        in = new Scanner(in_s);
        in.useDelimiter("=");

        String line;
        StringBuilder text = new StringBuilder();

        while (in.hasNextLine())
        {
            c.add(in.next());
            in.nextLine();
        }

        in.close();
        try
        {
            in_s.close();
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
        return (ArrayList<String>)c;
    }



    public void setupConfirmButton()
    {
        Button confirm = (Button) findViewById(R.id.confirmClasses);

        confirm.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View arg0)
                {
                    if(!is_valid)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please select from the drop down", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER,0,0);
                        toast.show();
                        newClass.setText("");
                        return;
                    }

                    String insert = newClass.getText().toString();

					String classString = prefs.getString("class_list", null);
					SharedPreferences.Editor edit = prefs.edit();

					if(classString != null && !classString.isEmpty())
                    {
						classString += "," + insert;
						edit.putString("class_list", classString);
					}

                    else
                    {
						edit.putString("class_list", insert);
					}

					edit.commit();


					classes.add(insert);
					Collections.sort(classes);
					arrayAdapter.notifyDataSetChanged();

					newClass.setText("");

					//TODO: Add this class to the server as well
                }
            }
        );
    }

    //This will convert any array into a string seperated by commas
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
