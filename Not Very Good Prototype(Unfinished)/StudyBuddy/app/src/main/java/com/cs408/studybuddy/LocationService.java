package com.cs408.studybuddy;

import android.content.*;
import android.location.*;
import android.app.*;
import android.os.Bundle;
import android.os.IBinder;

public class LocationService extends Service implements LocationListener
{
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	private Context myContext;
	private Location currentLocation;
	private boolean isConnected = false;
	protected LocationManager locationManager = null;

	boolean gpsEnabled;
	boolean networkEnabled;


	private static LocationService mGPS = null;


	private LocationService(Context c)
	{
		myContext = c;
	}

	public static LocationService getInstance(Context c)
	{
		if(mGPS == null)
		{
			mGPS = new LocationService(c);
		}
		return mGPS;
	}


	/**
	 * returns a location object with current location or null if failed
	 */
	public Location getLocation()
	{
		if(locationManager == null)
			return null;
		if(currentLocation != null)
			return currentLocation;
		else if(networkEnabled)
			return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		else if(gpsEnabled)
			return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		else
			return null;
	}


	/**
	 *	Quick approximation of the distance between two locations in meters.
	 *	Needs to be tested.
	 */
	public double distance(double lat_a, double long_a, double lat_b, double long_b) {
		double degreeLen = 110.25;

		double x = lat_a - lat_b;
		double y = (long_a - long_b) * Math.cos(lat_b);

		return degreeLen * Math.sqrt(x*x + y*y);
	}


	private boolean checkProvider(int timer, int distance){
		gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		if(networkEnabled) {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timer, distance, this);
		}
		if(gpsEnabled) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, timer, distance, this);
		}

		return (networkEnabled || gpsEnabled);
		//Log.d("GPS", "gps: " + gpsEnabled);
		//Log.d("GPS", "network: " + networkEnabled);
	}

	/**
	 * Start collecting location data from the GPS
	 * 
	 * @param minTime		minimum time between updates. (milliseconds)
	 * @param minDistance	minimum distance between updates. (meters)
	 *
	 * @return success		returns success if either the network or gps networks are available
	 */
	public boolean startGPS(int minTime, int minDistance){
		if(!isConnected) {
			locationManager = (LocationManager) myContext.getSystemService(LOCATION_SERVICE);
			isConnected = checkProvider(minTime, minDistance);
		}
		return isConnected;
	}
	
	
	public void stopGPS()
	{
		if(locationManager != null)
		{
			locationManager.removeUpdates(LocationService.this);
		}
		isConnected = false;
		locationManager = null;
	}
	
	/**
	 * Accuracy on a GPS currentLocation is given by the float being a radius of a circle
	 * centered at the location such that there is a 68% chance the user is within
	 * that circle
	 * 
	 * returns 0 on failure
	 */
	public float getAccuracy(){
		if(currentLocation != null)
		{
			return currentLocation.getAccuracy();
		}
		return 0;
	}
	
	@Override
    public void onLocationChanged(Location location) 
	{
		if(isBetterLocation(location, currentLocation))
			currentLocation = location;
    }


	/** Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}



 
    @Override
    public void onProviderDisabled(String provider) {
    }
 
    @Override
    public void onProviderEnabled(String provider) {
    }
 
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
 
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}