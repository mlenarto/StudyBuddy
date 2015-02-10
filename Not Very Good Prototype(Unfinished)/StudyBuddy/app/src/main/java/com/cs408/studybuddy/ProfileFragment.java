package com.cs408.studybuddy;

import android.content.Intent;
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
        setupLoginButton();
        setupAddClassesButton();
		return view;
	}

    public void setupLoginButton()
    {
        logoutButton = (Button) view.findViewById(R.id.logout);

        logoutButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                // TODO: Is this all that needs to be done?
                LoginHandler.logOut(view.getContext());
                startActivity(new Intent(view.getContext(), StudyBuddyLoginActivity.class));
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
