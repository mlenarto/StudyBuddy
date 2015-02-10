package com.cs408.studybuddy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by Jacob on 2/9/2015.
 */

public class ClassAddActivity extends ActionBarActivity
{
    public EditText newClass;
    public ListView classList;
    private Toolbar mToolbar;
    public SharedPreferences pref;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<String> classes;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_add);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        classes = new ArrayList<>();

        classList = (ListView) findViewById(R.id.classList);
        newClass = (EditText) findViewById(R.id.newClass);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, classes);
        classList.setAdapter(arrayAdapter);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        setupConfirmButton();
    }

    public void setupConfirmButton()
    {

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        Button confirm = (Button) findViewById(R.id.confirmClasses);

        confirm.setOnClickListener(new View.OnClickListener()
            {
                @Override
                //Hopefully will dynamically add elements to the list view, no checks for phony classes yet
                public void onClick(View arg0)
                {
                    //TODO: Checks for phony classes
                    String insert;
                    insert = newClass.getText().toString();

                    arrayAdapter.add(insert);
                    classes.add(insert);
                    pref.edit().putString("classes", convertToString(classes));
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        );
    }

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
