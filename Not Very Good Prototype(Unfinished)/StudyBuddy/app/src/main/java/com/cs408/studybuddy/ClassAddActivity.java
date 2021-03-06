package com.cs408.studybuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.io.InputStream;
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
    private AutoCompleteTextView newClass;
    private ListView classList;
    private Toolbar mToolbar;
    private SharedPreferences prefs;
    private classAdapter arrayAdapter;
    private List<String> classes;
    private ProgressDialog progress;

	private boolean animate = false;
	private boolean first = true;
	private boolean isEditing = false;
	private Animation fadeIn;
	private Animation fadeOut;
	private Animation hide;

	private static final int animationTime = 250;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_add);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
		classList = (ListView) findViewById(R.id.classList);
		newClass = (AutoCompleteTextView) findViewById(R.id.newClass);
		final Button editList = (Button) findViewById(R.id.edit_classes);
		final Button editListDone = (Button) findViewById(R.id.edit_classes_done);


		ReadMasterClassTask read = new ReadMasterClassTask();
		read.execute();

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

		hide = new AlphaAnimation(0, 0);
		hide.setDuration(0);
		hide.setFillAfter(true);

		fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setDuration(animationTime);
		fadeIn.setFillAfter(true);
		fadeIn.setInterpolator(new LinearInterpolator());

		fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setFillAfter(true);
		fadeOut.setDuration(animationTime);
		fadeOut.setInterpolator(new LinearInterpolator());

		editListDone.startAnimation(hide);

		//Begin editing - hide "Edit" button, show "Done" button and delete icons
		editList.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				first = false;
				isEditing = true;
				editListDone.startAnimation(hide);	//"Done" button fades in, so it has to be hidden first

				hide.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) { }

					@Override
					public void onAnimationEnd(Animation animation) {
						editListDone.setVisibility(View.VISIBLE);
						editList.startAnimation(fadeOut);
						editListDone.startAnimation(fadeIn);
						fadeOut.setAnimationListener(new Animation.AnimationListener() {
							@Override
							public void onAnimationStart(Animation animation) { }

							@Override
							public void onAnimationEnd(Animation animation) {
								editList.setVisibility(View.GONE);
							}

							@Override
							public void onAnimationRepeat(Animation animation) { }
						});
						editListDone.bringToFront();
						animate = true;
						arrayAdapter.notifyDataSetChanged();
					}

					@Override
					public void onAnimationRepeat(Animation animation) { }
				});


			}
		});

		//End editing - show "Edit" button, hide "Done" button and delete icons
		editListDone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isEditing = false;
				editList.startAnimation(hide);

				hide.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) { }

					@Override
					public void onAnimationEnd(Animation animation) {
						editList.setVisibility(View.VISIBLE);
						editList.startAnimation(fadeIn);
						editListDone.startAnimation(fadeOut);
						fadeOut.setAnimationListener(new Animation.AnimationListener() {
							@Override
							public void onAnimationStart(Animation animation) {
							}

							@Override
							public void onAnimationEnd(Animation animation) {
								editListDone.setVisibility(View.GONE);
							}

							@Override
							public void onAnimationRepeat(Animation animation) {
							}
						});
						editList.bringToFront();
						animate = true;
						arrayAdapter.notifyDataSetChanged();
					}

					@Override
					public void onAnimationRepeat(Animation animation) { }
				});
			}
		});

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

    }




	private class ReadMasterClassTask extends AsyncTask<Void, Void, ArrayList<String>> {

		//This reads in from a .txt file the master class list and returns it as an ArrayList (kinda crappy but it should only have to do this when a user wants to change classes)
		protected ArrayList<String> doInBackground(Void... v) {
			List<String> c;
			c = new ArrayList<>();
			Scanner in;
			Resources res = getResources();
			InputStream in_s = res.openRawResource(R.raw.classes);

			in = new Scanner(in_s);
			in.useDelimiter("=");

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

		//Set up the fancy EditText with listeners and an adapter
		//This makes sure that the user picks from the list given in the autocomplete.  Keeps a variable that sets to null if the user does not select a correct class
		protected void onPostExecute(ArrayList<String> classList) {
			ArrayAdapter completeAdapter = new ArrayAdapter<String>(ClassAddActivity.this
					, android.R.layout.simple_list_item_1, classList);
			newClass.setAdapter(completeAdapter);


			newClass.setEnabled(true);
			newClass.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			newClass.setHint(getResources().getString(R.string.class_prompt));

			newClass.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //Displays a loading indicator when a user adds a class, so they know what the heck is taking so long.
                    progress = ProgressDialog.show(ClassAddActivity.this, "Adding your class...", "Please wait...", true);
                    addClass();
                }
            });

			newClass.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(newClass.getText().toString().length() > 0)
						newClass.showDropDown();
				}
			});
		}
	}





    private void addClass()
    {

        final String newCourse = newClass.getText().toString();

		if(classes.contains(newCourse)) {
            progress.dismiss();
			newClass.setText("");
            Toast.makeText(getApplicationContext(), getString(R.string.duplicate_class), Toast.LENGTH_SHORT).show();
			return;
		}

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

                    //String spaceless_className = newCourse.replaceAll("\\s","");   MOVED BELOW
                    //ParsePush.subscribeInBackground(spaceless_className);          MOVED BELOW

					userObj.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							if(e == null){
								//course saved properly.
                                String classString = prefs.getString("class_list", null);
                                SharedPreferences.Editor edit = prefs.edit();

                                if(classString != null && !classString.isEmpty())
                                {
                                    classString += "," + newCourse;
                                    edit.putString("class_list", classString);
                                }

                                else
                                {
                                    edit.putString("class_list", newCourse);
                                }

                                edit.commit();

                                classes.add(newCourse);
                                Collections.sort(classes);
                                arrayAdapter.notifyDataSetChanged();

                                //subscribe user to class's channel
                                String spaceless_className = newCourse.replaceAll("\\s","");
                                Log.d("push",spaceless_className);
                                ParsePush.subscribeInBackground(spaceless_className);

								newClass.setText("");
                                progress.dismiss();
								Toast.makeText(getApplicationContext(), newCourse + " was added!",
										Toast.LENGTH_SHORT).show();
                            } else{
								//course was not saved properly.
								e.printStackTrace();
                                progress.dismiss();
								Toast.makeText(getApplicationContext(), "Error: Check your network connection.",
										Toast.LENGTH_SHORT).show();

								return;
							}
						}
					});

				} else {
					//couldn't retrieve the class
                    progress.dismiss();
					Log.d("ClassAddActivity", "Error: " + e.getMessage());
					Toast.makeText(getApplicationContext(), "Error: Check your network connection.",
							Toast.LENGTH_SHORT).show();
				}
			}
		});
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
			final Button remove;
			final String className;

			if(v == null) {
				LayoutInflater li = LayoutInflater.from(getContext());
				v = li.inflate(resource, parent, false);
			}


			classNameText = (TextView)v.findViewById(R.id.class_name);
			remove = (Button)v.findViewById(R.id.remove_class);

			className = this.getItem(position);
			classNameText.setText(className);

			//Control visibility of the delete icon
			if(first) {
				remove.startAnimation(hide);
			} else if(isEditing && animate) {
				Animation fadeIn = new AlphaAnimation(0, 1);
				fadeIn.setDuration(animationTime);
				fadeIn.setFillAfter(true);
				fadeIn.setInterpolator(new LinearInterpolator());
				remove.startAnimation(fadeIn);
				fadeIn.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						animate = false;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
			}
			else if(animate) {
				Animation fadeOut = new AlphaAnimation(1, 0);
				fadeOut.setDuration(animationTime);
				fadeOut.setFillAfter(true);
				fadeOut.setInterpolator(new LinearInterpolator());
				remove.startAnimation(fadeOut);
				fadeOut.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						animate = false;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
			}

			remove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!isEditing)	//Button is invisible and shouldn't react to touch events
						return;

					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
					builder.setMessage("Are you sure you want to leave this class?")
							.setTitle("Leave " + className + "?");

					builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
                            //Should only do the crappy loading thingy if they say they in fact, do wish to remove their class
                            progress = ProgressDialog.show(ClassAddActivity.this, "Removing the selected class from your class list...", "Please wait...", true);
							//removeClassFromPrefs(className);  MOVED BELOW

                            /* Remove class to the user's course list on database */
                            //Grab class object from database
                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
                            query.whereContains("courseNumber", className);
                            query.getFirstInBackground(new GetCallback<ParseObject>() {
                                public void done(ParseObject classObj, ParseException e) {
                                    if (e == null) {
                                        //found the class
                                        Log.d("ClassAddActivity", "Class: " + classObj.getString("courseName"));
                                        //TODO: Check if the user doesn't have this course in their courseList (not super important)
                                        //TODO: Check if the user is in a study group for this class. If they are, prompt them that it will remove them.
                                        //remove it from the user's course list
                                        ParseUser userObj = ParseUser.getCurrentUser();
                                        ParseRelation<ParseObject> relation = userObj.getRelation("courseList");
                                        relation.remove(classObj);

                                        // Unsubscribe user from the channel for that class
                                        //String spaceless_className = className.replaceAll("\\s","");  MOVED BELOW
                                        //ParsePush.unsubscribeInBackground(spaceless_className);       MOVED BELOW

                                        userObj.saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                if(e == null){
                                                    //user saved properly.
                                                    removeClassFromPrefs(className);

                                                    // Unsubscribe user from the channel for that class
                                                    String spaceless_className = className.replaceAll("\\s","");
                                                    ParsePush.unsubscribeInBackground(spaceless_className);
                                                    progress.dismiss();
                                                    Toast.makeText(getApplicationContext(), className + " was removed.",
                                                            Toast.LENGTH_SHORT).show();
                                                } else{
                                                    //course was not saved properly.
                                                    e.printStackTrace();
                                                    progress.dismiss();
                                                    Toast.makeText(getApplicationContext(), "Error: Check your network connection.",
                                                            Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                            }
                                        });

                                    } else {
                                        //couldn't retrieve class
                                        Log.d("ClassAddActivity", "Error: " + e.getMessage());
                                        progress.dismiss();
                                        Toast.makeText(getApplicationContext(), "Error: Check your network connection.",
                                                Toast.LENGTH_SHORT).show();

                                    }
                                }
                            });

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


	private void removeClassFromPrefs(String className) {
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
	}

    //Class method that is used to get the list of all classes the user is in
    public static ArrayList<String> getClasses(SharedPreferences prefs)
    {
        String classesString = prefs.getString("class_list",null);
        ArrayList<String> classes = convertToArray(classesString);
        return classes;
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
}
