package com.cs408.studybuddy;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.os.Handler;
import android.view.MenuItem;


public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        switchToLoginScreen();                                  //switch over to the next screen after the splash screen, couldn't remember if it was the login screen or not...
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void switchToLoginScreen()
    {
        //Wait 3 seconds before switching to the login screen
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run()
            {
                startActivity(new Intent(SplashScreenActivity.this, StudyBuddyLoginActivity.class));
                finish();
            }
        }, 3000);
    }

}
