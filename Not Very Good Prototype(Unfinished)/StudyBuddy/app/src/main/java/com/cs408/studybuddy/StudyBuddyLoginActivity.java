package com.cs408.studybuddy;

import android.app.Activity;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
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

        if(ParseUser.getCurrentUser() != null)
        {
            // User is already logged in, so don't show a login screen
            Log.d("StudyBuddyLoginActivity", "Performing automatic login");
            logIn();
        }
        else
        {
            ParseLoginBuilder builder = new ParseLoginBuilder(StudyBuddyLoginActivity.this);
            startActivityForResult(builder.build(), LOGIN_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST && resultCode == RESULT_OK) {
            // Login successful
            logIn();
        }
    }

    /**
     * Logs the user in and opens the main activity.
     */
    private void logIn() {
        LoginHandler.finishLogIn();
        Intent i = new Intent (StudyBuddyLoginActivity.this, DrawerActivity.class);
        startActivity(i);
        finish();
    }
}



