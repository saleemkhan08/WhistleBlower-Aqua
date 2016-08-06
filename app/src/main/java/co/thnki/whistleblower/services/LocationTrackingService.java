package co.thnki.whistleblower.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.squareup.otto.Subscribe;

import co.thnki.whistleblower.interfaces.SettingsResultListener;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.PermissionUtil;

public class LocationTrackingService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks
{
    public static final String STOP_SERVICE = "stopService";
    public static final String FORCE_STOP = "forceStop";
    public static final String TURN_ON_LOCATION_SETTINGS = "turnOnLocationSettings";
    private static final String TAG = "LocationTrackingService";
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationSettingsRequest mLocationSettingsRequest;

    public static final String KEY_TRAVELLING_MODE = "KEY_TRAVELLING_MODE";
    private static final String KEY_LOCATION_UPDATE_FREQ = "updateFreq";
    private boolean mStartLocationUpdates;
    public LocationTrackingService()
    {
        Log.d(TAG, "Service : Constructor");
    }

    private SharedPreferences preferences;

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "Service : onBind");
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Otto.register(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
        Log.d(TAG, "Service : onCreate");
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId)
    {
        if (intent == null)
        {
            Log.d(TAG, "onStartCommand");
            if (!isStopServiceConditionMet())
            {
                if (PermissionUtil.isLocationPermissionAvailable() && !PermissionUtil.isLocationSettingsOn())
                {
                    String msg = "You have set a Location Alarm.\nPlease turn on Location settings?";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            }
        }
        if (PermissionUtil.isLocationPermissionAvailable() && PermissionUtil.isLocationSettingsOn())
        {
            startLocationUpdates();
        }
        else
        {
            turnOnLocationSettings();
        }
        return START_STICKY;
    }

    private void turnOnLocationSettings()
    {
        PermissionUtil.turnOnLocationSettings(mGoogleApiClient, mLocationSettingsRequest, new SettingsResultListener()
        {
            @Override
            public void onLocationSettingsTurnedOn()
            {
                startLocationUpdates();
            }

            @Override
            public void onLocationSettingsCancelled()
            {
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates()
    {
        Log.d(TAG, "startLocationUpdates");
        if (mGoogleApiClient.isConnected())
        {
            int interval = Integer.parseInt(preferences.getString(KEY_LOCATION_UPDATE_FREQ, "750"));
            mLocationRequest.setInterval(interval);
            mLocationRequest.setFastestInterval(interval);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else
        {
            mStartLocationUpdates = true;
        }
    }

    private void stopLocationUpdates()
    {
        Log.d(TAG, "stopLocationUpdates");
        if (mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        mStartLocationUpdates = false;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void communicator(String action)
    {
        Log.d("Communicator", action);
        switch (action)
        {
            case STOP_SERVICE:
                stopService();
                break;
            case FORCE_STOP:
                stopSelf();
                break;
            case TURN_ON_LOCATION_SETTINGS:
                turnOnLocationSettings();
                break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "Service : onDestroy");
        Otto.unregister(this);
        stopLocationUpdates();
    }

    private synchronized void buildGoogleApiClient()
    {
        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    private void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Integer.parseInt(preferences.getString(KEY_LOCATION_UPDATE_FREQ, "500")));
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest()
    {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "Service  : onConnected : mStartLocationUpdates : " + mStartLocationUpdates);

        if (mStartLocationUpdates)
        {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.d(TAG, "Service  : onConnectionSuspended");
        stopLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Log.d(TAG, "onLocationChanged");
        Otto.post(location);
        stopService();
    }

    private void stopService()
    {
        Log.d(TAG, "stopService");
        if (isStopServiceConditionMet())
        {
            stopLocationUpdates();
            stopSelf();
            Log.d(TAG, "stopSelf");
        }
    }

    private boolean isStopServiceConditionMet()
    {
        boolean isTravellingMode = preferences.getBoolean(KEY_TRAVELLING_MODE, false);
        if (!isTravellingMode)
        {
            Log.d(TAG, "isStopServiceConditionMet  : true");
            return true;
        }
        else
        {
            Log.d(TAG, "isStopServiceConditionMet  : false");
            return false;
        }
    }
}