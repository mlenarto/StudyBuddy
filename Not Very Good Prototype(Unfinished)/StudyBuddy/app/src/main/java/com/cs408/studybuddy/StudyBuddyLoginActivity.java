package com.cs408.studybuddy;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;

import com.parse.Parse;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;



/**
 * A login screen that offers login via email/password.
 */
public class StudyBuddyLoginActivity extends Activity{

    private static final int LOGIN_REQUEST = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_buddy_login);

        // Required - Initialize the Parse SDK
        Parse.initialize(this, getString(R.string.parse_app_id),
                getString(R.string.parse_client_key));

        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        ParseLoginBuilder builder = new ParseLoginBuilder(StudyBuddyLoginActivity.this);
        startActivityForResult(builder.build(), LOGIN_REQUEST);
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
            }
        }
    }
}



