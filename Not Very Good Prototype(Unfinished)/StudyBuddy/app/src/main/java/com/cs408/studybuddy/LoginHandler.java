package com.cs408.studybuddy;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.parse.ParseInstallation;
import com.parse.ParseUser;

/**
 * Handles user login and logout.
 * Created by Aaron on 2/10/2015.
 */
public final class LoginHandler {
    /**
     * Updates the application state after a user has logged in.
     */
    public static void finishLogIn() {
        setUpInstallation();
        logIntoSinch();
    }

    /**
     * Logs the current user out.
     * @param context The context to use.
     */
    public static void logOut(Context context) {
        unlinkInstallation();
        logOutOfSinch();
        ParseUser.logOut();
    }

    /**
     * Sets up the Parse installation for a user that has logged in.
     */
    private static void setUpInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        installation.saveInBackground();
    }

    /**
     * Logs the user into Sinch.
     */
    private static void logIntoSinch() {
        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        sinch.startClient(ParseUser.getCurrentUser().getEmail());
    }

    /**
     * Logs the user out of sinch.
     */
    private static void logOutOfSinch() {
        SinchService.SinchServiceInterface sinch = StudyBuddyApplication.getSinchServiceInterface();
        sinch.stopClient();
    }

    /**
     * Unlinks the Parse installation from the current user.
     */
    private static void unlinkInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.remove("user");
        installation.saveInBackground();
    }

    private LoginHandler() {
        // Static class, can't construct
    }
}
