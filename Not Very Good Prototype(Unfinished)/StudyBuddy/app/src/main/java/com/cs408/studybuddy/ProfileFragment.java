package com.cs408.studybuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.parse.ParseUser;


public class ProfileFragment extends Fragment {
    Button logoutButton, addClassesButton;
	public ProfileFragment() {}
    View view;
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_profile, container, false);
        setupLogoutButton();
        setupAddClassesButton();
		return view;
	}

    public void setupLogoutButton()
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

    public void setupAddClassesButton()
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
}
