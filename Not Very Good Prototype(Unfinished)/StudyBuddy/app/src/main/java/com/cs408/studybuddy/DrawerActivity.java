package com.cs408.studybuddy;

import android.app.Application;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.support.v4.widget.DrawerLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DrawerActivity extends ActionBarActivity {


	private static final int CLASS_LIST_FRAGMENT = 0;
	private static final int GROUP_FRAGMENT = 1;
	private static final int MESSAGES_FRAGMENT = 2;
	private static final int PROFILE_FRAGMENT = 3;


    /**
     * Activity managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private TextView mTitle;

	private Fragment currentFragment;
	private int currentFragmentId = CLASS_LIST_FRAGMENT;

	//This is only used to start gathering location data for later
	private LocationService gps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navigation_drawer);
		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mTitle = (TextView) findViewById(R.id.title_text);

		gps = LocationService.getInstance(getApplicationContext());
		gps.startGPS(20000, 15);

		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		//Class list is the default tab to have open at start-up
		currentFragment = new ClassListFragment();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(R.id.container, currentFragment);
		transaction.commit();
		currentFragmentId = CLASS_LIST_FRAGMENT;
		mTitle.setText(R.string.title_class_list);


        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
			}

		};


		mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				displayView(position);
			}
		});

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mDrawerToggle.syncState();

		String[] drawerItems = getResources().getStringArray(R.array.drawer_class_list);

        mDrawerList.setAdapter(new ArrayAdapter<>(this,
				R.layout.drawer_item_fragment, R.id.drawer_text, drawerItems));
    }

	@Override
	protected void onPause() {
		super.onPause();
		gps.stopGPS();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
		if(mDrawerToggle.onOptionsItemSelected(item))
			return true;

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

	private void displayView(int position) {

		if(position == currentFragmentId) {
			if(position == GROUP_FRAGMENT) {
				((RequestInfoFragment) currentFragment).refreshInfo();
			}

			//No need to switch to current fragment
			mDrawerLayout.closeDrawer(mDrawerList);
			return;
		}

		FrameLayout frame = (FrameLayout) findViewById(R.id.container);
		frame.removeAllViews();

		switch (position) {
			case CLASS_LIST_FRAGMENT:
				currentFragment = new ClassListFragment();
				mTitle.setText(R.string.title_class_list);
				break;
			case GROUP_FRAGMENT:
				currentFragment = new RequestInfoFragment();
				mTitle.setText(getString(R.string.title_my_group));
				break;
			case MESSAGES_FRAGMENT:
				currentFragment = new MessagesFragment();
                // FLAW: Don't change title
				//mTitle.setText(R.string.title_messages);
				break;
			case PROFILE_FRAGMENT:
				currentFragment = new ProfileFragment();
				mTitle.setText(R.string.title_my_profile);
				break;
			default:
				break;
		}

		if(currentFragment != null) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.container, currentFragment);
			transaction.commit();
		}
		currentFragmentId = position;
		mDrawerLayout.closeDrawer(mDrawerList);

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

}
