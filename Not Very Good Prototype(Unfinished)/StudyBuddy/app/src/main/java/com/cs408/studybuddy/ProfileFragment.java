package com.cs408.studybuddy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;


public class ProfileFragment extends Fragment {
    private ProgressDialog progress;
    Button logoutButton, addClassesButton;
    SwitchCompat silent;
    SharedPreferences prefs;


	public ProfileFragment() {}
    View view;
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_profile, container, false);
        prefs = getActivity().getSharedPreferences(getResources().getString(R.string.app_preferences), 0);
        setupSilentMode();
        setupLogoutButton();
        setupAddClassesButton();
		return view;
	}

    private void setupLogoutButton()
    {
        logoutButton = (Button) view.findViewById(R.id.logout);

        logoutButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
				SharedPreferences prefs = getActivity().getSharedPreferences(getResources().getString(R.string.app_preferences), 0);
				SharedPreferences.Editor edit = prefs.edit();
				edit.clear();
				edit.commit();

                // TODO: Is this all that needs to be done?
                LoginHandler.logOut(view.getContext());

				Intent intent = new Intent(getActivity(), StudyBuddyLoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
				getActivity().finish();
            }
        }
        );
    }

    private void setupAddClassesButton()
    {
        addClassesButton = (Button) view.findViewById(R.id.addClass);

        addClassesButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View arg0)
                {
                    startActivity(new Intent(view.getContext(), ClassAddActivity.class));
                }

            }
        );
    }

    //Getting the silent mode switch to work
    private void setupSilentMode()
    {
        silent = (SwitchCompat)view.findViewById(R.id.alert_switch);
        silent.setChecked(false);
        silent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                ArrayList<String> classes = ClassAddActivity.getClasses(prefs);
                //if the switch is not checked, get the list of classes, and unsubscribe the user from them all
                if(isChecked)
                {
                    progress = ProgressDialog.show(getActivity(), "Turning on silent mode...", "Please wait...", true);
                    //Go down each class and check if it is in the database
                    for(final String className : classes)
                    {
                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
                        query.whereContains("courseNumber", className);
                        query.getFirstInBackground(new GetCallback<ParseObject>()
                        {
                            public void done(ParseObject classObj, ParseException e)
                            {
                                //If the class is found in the database
                                if (e == null)
                                {
                                    Log.d("ProfileFragment", "Class: " + classObj.getString("courseName"));
                                    ParseUser userObj = ParseUser.getCurrentUser();

                                    userObj.saveInBackground(new SaveCallback()
                                    {
                                        @Override
                                        public void done(ParseException e)
                                        {
                                            if (e == null)
                                            {
                                                // Unsubscribe user from the channel for that class
                                                String spaceless_className = className.replaceAll("\\s", "");
                                                ParsePush.unsubscribeInBackground(spaceless_className);
                                                progress.dismiss();
                                            }

                                            else
                                            {
                                                //course was not saved properly.
                                                e.printStackTrace();
                                                progress.dismiss();
                                                Toast.makeText(getActivity().getApplicationContext(), "Error: Check your network connection.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }
                                    });
                                }

                                else
                                {
                                    //couldn't retrieve class
                                    Log.d("ProfileFragment", "Error: " + e.getMessage());
                                    progress.dismiss();
                                    Toast.makeText(getActivity().getApplicationContext(), "Error: Check your network connection.", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
                    }
                }
                //if the user is not subscribed to the classes, resubscribe them.
                else
                {
                    progress = ProgressDialog.show(getActivity(), "Turning off silent mode...", "Please wait...", true);
                    //Go down each class and check if it is in the database
                    for(final String className : classes)
                    {
                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Course");
                        query.whereContains("courseNumber", className);
                        query.getFirstInBackground(new GetCallback<ParseObject>()
                        {
                            public void done(ParseObject classObj, ParseException e)
                            {
                                //If the class is found in the database
                                if (e == null)
                                {
                                    Log.d("ProfileFragment", "Class: " + classObj.getString("courseName"));
                                    ParseUser userObj = ParseUser.getCurrentUser();

                                    userObj.saveInBackground(new SaveCallback()
                                    {
                                        @Override
                                        public void done(ParseException e)
                                        {
                                            if (e == null)
                                            {
                                                // Unsubscribe user from the channel for that class
                                                String spaceless_className = className.replaceAll("\\s", "");
                                                ParsePush.subscribeInBackground(spaceless_className);
                                                progress.dismiss();
                                            }

                                            else
                                            {
                                                //course was not saved properly.
                                                e.printStackTrace();
                                                progress.dismiss();
                                                Toast.makeText(getActivity().getApplicationContext(), "Error: Check your network connection.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }
                                    });
                                }

                                else
                                {
                                    //couldn't retrieve class
                                    Log.d("ProfileFragment", "Error: " + e.getMessage());
                                    progress.dismiss();
                                    Toast.makeText(getActivity().getApplicationContext(), "Error: Check your network connection.", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
                    }
                }
            }
        });
    }
}
