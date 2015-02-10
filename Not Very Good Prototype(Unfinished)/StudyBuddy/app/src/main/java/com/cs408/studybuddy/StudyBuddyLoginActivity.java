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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean LoggedIn = prefs.getBoolean("Islogin", false);
        Log.v("login","Boolean value: " + LoggedIn);

        // FIXME: This doesn't work because it doesn't actually log the user into Parse
        /*if(LoggedIn)           //if the user has already logged in, take them straight to the main page skipping the login screen.
        {
            Log.v("login","Skipping Parse");
            LoginHandler.finishLogIn();
            Intent i;
            i = new Intent(StudyBuddyLoginActivity.this, DrawerActivity.class);
            startActivity(i);
            finish();
        }

        else*/
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
                LoginHandler.finishLogIn();
                Intent i = new Intent (StudyBuddyLoginActivity.this, DrawerActivity.class);
                //i.putExtra("User", ParseUser.getCurrentUser().getString("name"));
                startActivity(i);
				finish();
            }
        }
    }
}



