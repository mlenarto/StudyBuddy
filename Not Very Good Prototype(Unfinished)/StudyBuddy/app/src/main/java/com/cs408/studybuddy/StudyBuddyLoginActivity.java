package com.cs408.studybuddy;

import android.app.Activity;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;



/**
 * A login screen that offers login via email/password.
 */
public class StudyBuddyLoginActivity extends Activity{

    private static final int LOGIN_REQUEST = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean LoggedIn = prefs.getBoolean("Islogin", false);
        Log.v("login","Boolean value: " + LoggedIn);

        // Required - Initialize the Parse SDK
        Parse.initialize(this, getString(R.string.parse_app_id),
                getString(R.string.parse_client_key));

        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        // Subscribe to broadcast channel for push notifications
        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e("com.parse.push", "failed to subscribe for push", e);
                }
            }
        });

        if(LoggedIn)           //if the user has already logged in, take them straight to the main page skipping the login screen.
        {
            Log.v("login","Skipping Parse");
            Intent i;
            i = new Intent(StudyBuddyLoginActivity.this, DrawerActivity.class);
            startActivity(i);
            finish();
        }

        else
        {
            ParseLoginBuilder builder = new ParseLoginBuilder(StudyBuddyLoginActivity.this);
            startActivityForResult(builder.build(), LOGIN_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Check which request we're responding to
        if (requestCode == LOGIN_REQUEST)
        {
            // Check if the login request was successful
            if (resultCode == RESULT_OK)
            {
                Intent i = new Intent (StudyBuddyLoginActivity.this, DrawerActivity.class);
                //i.putExtra("User", ParseUser.getCurrentUser().getString("name"));
                startActivity(i);
				finish();
            }
        }
    }
}



