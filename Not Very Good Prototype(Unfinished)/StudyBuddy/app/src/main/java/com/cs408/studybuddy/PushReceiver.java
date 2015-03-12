package com.cs408.studybuddy;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.parse.ParsePushBroadcastReceiver;

import java.util.List;

/**
 * Created by Tyler on 3/12/2015.
 */
public class PushReceiver extends ParsePushBroadcastReceiver {
    @Override
    public void onPushOpen(Context context, Intent intent) {
        //TODO: Instead of opening login activity, have it open the request
        Intent i = new Intent(context, StudyBuddyLoginActivity.class);
        i.putExtras(intent.getExtras());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }



}
