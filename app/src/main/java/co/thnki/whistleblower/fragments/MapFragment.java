package co.thnki.whistleblower.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.thnki.whistleblower.AddIssueActivity;
import co.thnki.whistleblower.IssueActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.interfaces.GeoCodeListener;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.receivers.InternetConnectivityListener;
import co.thnki.whistleblower.services.LocationTrackingService;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.GeoCoderTask;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.LocationUtil;
import co.thnki.whistleblower.utils.MarkerAndCirclesUtil;
import co.thnki.whistleblower.utils.PermissionUtil;
import co.thnki.whistleblower.utils.TransitionUtil;
import co.thnki.whistleblower.view.TouchableWrapper;

import static co.thnki.whistleblower.AddIssueActivity.ISSUE_DATA;
import static co.thnki.whistleblower.utils.LocationUtil.distFrom;

public class MapFragment extends SupportMapFragment implements
        TouchableWrapper.OnMapTouchListener,
        GeoCodeListener,
        GoogleMap.OnCameraChangeListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener
{
    public static final String DIALOG_DISMISS = "dialogDismiss";
    public static final String TURN_OFF_TRAVEL_MODE = "turnOffTravelMode";
    public static final String TURN_ON_TRAVEL_MODE = "turnOnTravelMode";
    public static final String MAP_TYPE_CHANGED = "mapTypeChanged";
    public static final String ADDRESS = "address";
    public static final String LATLNG = "latlng";
    public static final String RADIUS = "radius";
    private static final String FRAGMENT_TAG_SETTINGS = "settingsFragment";
    public static final String IS_RELOAD_NECESSARY = "isReloadNecessary";
    @BindString(R.string.noInternet)
    String NO_INTERNET;

    private View mOriginalContentView;
    private int searchBarMargin;
    public boolean isAddPhotoButtonShown;
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    private static final String ZOOM = "ZOOM";
    private static final String TILT = "TILT";
    private static final String BEARING = "BEARING";

    private static final String MAP_TYPE = "mapType";
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 91;
    private static final String ACCURACY = "ACCURACY";

    private Marker myLocMarker;
    private Circle myLocCircle;

    private GoogleMap mGoogleMap;

    private AppCompatActivity mActivity;

    private SharedPreferences mPreferences;

    @BindColor(R.color.travel_mode)
    int travelModeColor;

    @Bind(R.id.searchWaitProgress)
    RelativeLayout mSearchWaitProgress;

    @BindColor(R.color.colorAccent)
    int accentColor;

    @BindColor(R.color.my_location_radius)
    int radiusColor;

    @Bind(R.id.map_fab_buttons)
    ViewGroup map_fab_buttons;

    @Bind(R.id.searchBarInnerWrapper)
    RelativeLayout searchBar;

    @Bind(R.id.my_loc)
    FloatingActionButton buttonMyLoc;

    @Bind(R.id.addPhotoButton)
    FloatingActionButton mSubmitButton;

    @Bind(R.id.searchBar)
    RelativeLayout mToolbar;

    @Bind(R.id.hoverPlaceName)
    TextView searchText;

    @Bind(R.id.searchProgress)
    ProgressBar mSearchProgress;

    @Bind(R.id.settingsIcon)
    ImageView mSettingsIcon;

    @Bind(R.id.radiusSeekBarValueWrapper)
    View radiusSeekBarValueWrapper;

    @Bind(R.id.radiusSeekBarInnerWrapper)
    ViewGroup radiusSeekBarInnerWrapper;

    @Bind(R.id.titleBar)
    RelativeLayout mTitleBar;

    @Bind(R.id.radiusSeekBar)
    SeekBar radiusSeekBar;

    @Bind(R.id.radiusSeekBarValue)
    TextView radiusSeekBarValue;

    @Bind(R.id.select_location)
    View select_location;

    @Bind(R.id.searchIcon)
    ImageView searchIcon;

    private int mRetryAttemptsCount;
    private GeoCoderTask mGeoCoderTask;
    private LatLng mGeoCodeLatLng;
    private LatLng mOnActionDownLatLng;
    private int mRadiusType;
    private float mCurrentZoom;
    private boolean mIsPlacesApiResult;
    public MarkerAndCirclesUtil mMarkerAndCircle;
    private Circle mActionCircle;

    @BindString(R.string.slowInternet)
    String SLOW_INTERNET;

    @BindDrawable(R.mipmap.plus_white)
    Drawable mAddIcon;
    private static boolean travelModeOffHelpTextShown;
    private Toast mRadiusToast;
    private Toast mAddPhotoToast;
    private Toast mSelectLocationToast;
    private boolean mTravelModeUiState;
    private boolean mIsActionDown;
    private CameraPosition mCameraPosition;

    public MapFragment()
    {
        Log.d("MapFragmentFlowLogs", "Constructor");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        Log.d("MapFragmentFlowLogs", "onCreateView");
        mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
        TouchableWrapper mTouchView = new TouchableWrapper(getActivity(), this);
        mTouchView.addView(mOriginalContentView);
        mPreferences = WhistleBlower.getPreferences();
        mPreferences.edit().putBoolean(MapFragment.IS_RELOAD_NECESSARY, true).apply();
        getMapAsync(this);
        return mTouchView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Log.d("MapFragmentFlowLogs", "onActivityCreated");
        mActivity = (AppCompatActivity) getActivity();
        ButterKnife.bind(this, mActivity);

        startLocationTrackingService();
        Log.d("MapFragmentFlowLogs", "Start Service Called");
        setupRadiusSeekBar();
        setUpMyLocationButton();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        Otto.register(this);
        mSearchWaitProgress.setVisibility(View.GONE);
        searchIcon.setClickable(true);
        searchText.setClickable(true);
        Log.d("MapFragmentFlowLogs", "onResume");
        changeTravelModeState(false, false);
        updateLocationInfo();
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.getUiSettings().setCompassEnabled(false);
        Log.d("MarkerNCircleAddingTask", "onMapReady");
        reloadMarkersAndCircles();
        if (PermissionUtil.isLocationPermissionAvailable())
        {
            mGoogleMap.setMyLocationEnabled(false);
        }
        mGoogleMap.setOnCameraChangeListener(this);
        if (mIsPlacesApiResult)
        {
            mIsPlacesApiResult = false;
        }
        else
        {
            showMyLocOnMap(false);
            boolean isTravelModeOn = mPreferences.getBoolean(LocationTrackingService.KEY_TRAVELLING_MODE, false);
            changeTravelModeState(isTravelModeOn, false);
        }
    }

    public void reloadMarkersAndCircles()
    {
        if (mPreferences.getBoolean(IS_RELOAD_NECESSARY, false))
        {
            mMarkerAndCircle = new MarkerAndCirclesUtil(mGoogleMap, accentColor, radiusColor);
            mPreferences.edit().putBoolean(MapFragment.IS_RELOAD_NECESSARY, false).apply();
        }
    }

    private void showMyLocOnMap(boolean animate)
    {
        Log.d("MapFragmentFlowLogs", "showMyLocOnMap");
        LatLng latLng = getLatLng();
        showMyLocationMarkerAndCircle(latLng, getAccuracy());
        gotoLatLng(latLng, animate);
    }

    @Subscribe
    public void showIssueOnMap(Issue issue)
    {
        Log.d("MapFragmentFlowLogs", "showAlarmOnMap");
        LatLng latLng = LocationUtil.getLatLng(issue.latitude, issue.longitude);
        mMarkerAndCircle.addMarkerAndCircle(issue);
        gotoLatLng(latLng, false);
    }

    private void setupRadiusSeekBar()
    {
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                setRadiusSeekBarValue();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        radiusSeekBarValueWrapper.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(mActivity, v);
                popup.getMenuInflater()
                        .inflate(R.menu.radius_options, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        mRadiusType = item.getItemId();
                        setRadiusSeekBarValue();
                        return true;
                    }
                });
                popup.show();
                hideToast();
            }
        });
    }

    private void setRadiusSeekBarValue()
    {
        String radiusSeekBarText;
        int radius = getSeekBarValue();
        switch (mRadiusType)
        {
            case R.id.radius_km:
                radiusSeekBarText = radius + getText(R.string.kilometer).toString();
                break;
            case R.id.radius_mi:
                radiusSeekBarText = radius + getText(R.string.miles).toString();
                break;
            case R.id.radius_fts:
                radiusSeekBarText = radius + getText(R.string.feet).toString();
                break;
            default:
                radiusSeekBarText = radius + getText(R.string.meter).toString();
                break;
        }

        radiusSeekBarValue.setText(radiusSeekBarText);
        drawCircleOnMap();
    }

    @SuppressWarnings("WeakerAccess")
    @OnClick(R.id.addPhotoButton)
    public void alarmButton()
    {
        if (isAddPhotoButtonShown)
        {
            addPhoto();
        }
        else
        {
            showAddPhotoButtonAndHideAddButton();

        }
    }

    private void hideToast()
    {
        if (mSelectLocationToast != null)
        {
            mSelectLocationToast.cancel();
            mRadiusToast.cancel();
            mAddPhotoToast.cancel();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        hideToast();
    }

    private void selectLocationHintToast()
    {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.select_location_toast,
                (ViewGroup) mActivity.findViewById(R.id.toastRoot));
        int xOffset = ImageUtil.pixels(mActivity, mActivity.getResources().getInteger(R.integer.select_location_x_offset));
        int yOffset = ImageUtil.pixels(mActivity, mActivity.getResources().getInteger(R.integer.select_location_y_offset));

        mSelectLocationToast = new Toast(mActivity);
        mSelectLocationToast.setGravity(Gravity.CENTER_VERTICAL, xOffset, yOffset);
        mSelectLocationToast.setDuration(Toast.LENGTH_SHORT);
        mSelectLocationToast.setView(layout);
        mSelectLocationToast.show();
    }

    private void setRadiusHintToast()
    {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.set_radius_toast,
                (ViewGroup) mActivity.findViewById(R.id.toastRoot));
        int xOffset = ImageUtil.pixels(mActivity, mActivity.getResources().getInteger(R.integer.set_radius_x_offset));
        int yOffset = ImageUtil.pixels(mActivity, mActivity.getResources().getInteger(R.integer.set_radius_y_offset));

        mRadiusToast = new Toast(mActivity);
        mRadiusToast.setGravity(Gravity.BOTTOM | Gravity.LEFT, xOffset, yOffset);
        mRadiusToast.setDuration(Toast.LENGTH_SHORT);
        mRadiusToast.setView(layout);
        mRadiusToast.show();
    }

    private void addPhotoHintToast()
    {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.add_photo_toast,
                (ViewGroup) mActivity.findViewById(R.id.toastRoot));

        int xOffset = ImageUtil.pixels(mActivity, mActivity.getResources().getInteger(R.integer.add_photo_x_offset));
        int yOffset = ImageUtil.pixels(mActivity, mActivity.getResources().getInteger(R.integer.add_photo_y_offset));
        mAddPhotoToast = new Toast(mActivity);
        mAddPhotoToast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, xOffset, yOffset);
        mAddPhotoToast.setDuration(Toast.LENGTH_SHORT);
        mAddPhotoToast.setView(layout);
        mAddPhotoToast.show();
    }

    private void showAddPhotoButtonAndHideAddButton()
    {
        if (PermissionUtil.isConnected(mActivity))
        {
            setupRadiusSeekBar();
            mSubmitButton.setIcon(R.mipmap.add_a_photo_white);
            isAddPhotoButtonShown = true;

            TransitionUtil.slideTransition(radiusSeekBarInnerWrapper);
            TransitionUtil.slideTransition(mTitleBar);

            radiusSeekBarInnerWrapper.setVisibility(View.VISIBLE);
            mTitleBar.setVisibility(View.INVISIBLE);
            drawCircleOnMap();
            changeTravelModeState(false, false);
            if (mPreferences.getBoolean(SettingsFragment.SHOW_HINTS, true))
            {
                selectLocationHintToast();
                setRadiusHintToast();
                addPhotoHintToast();
            }
        }
        else
        {
            toast(NO_INTERNET);
        }
    }

    public void hideSubmitButtonAndShowAddButton()
    {
        isAddPhotoButtonShown = false;
        mSubmitButton.setIconDrawable(mAddIcon);

        TransitionUtil.slideTransition(radiusSeekBarInnerWrapper);
        TransitionUtil.slideTransition(mTitleBar);
        radiusSeekBarInnerWrapper.setVisibility(View.INVISIBLE);
        mTitleBar.setVisibility(View.VISIBLE);
        removeActionCircle();
        hideToast();
    }

    private void removeActionCircle()
    {
        if (mActionCircle != null)
        {
            mActionCircle.remove();
            mActionCircle = null;
        }
    }


    @Override
    public void onStop()
    {
        super.onStop();
        mMarkerAndCircle.unregister();
        Otto.unregister(this);
        cancelAsyncTask(mGeoCoderTask);
        mSearchWaitProgress.setVisibility(View.GONE);
        Log.d("mSearchWaitProgress", "gone");
    }

    private void setUpMyLocationButton()
    {
        buttonMyLoc.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startLocationTrackingService();
                showMyLocOnMap(true);
                if (mPreferences.getBoolean(SettingsFragment.SHOW_HINTS, true))
                {
                    if(mTravelModeUiState)
                    {
                        toast(getString(R.string.travel_mode_off_helptext));
                    }
                    else
                    {
                        toast(getString(R.string.travel_mode_on_helptext));
                    }

                }
            }
        });

        buttonMyLoc.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                showMyLocOnMap(true);
                if (!getTravelMode())
                {
                    changeTravelModeState(true, true);
                    startLocationTrackingService();
                }
                else
                {
                    changeTravelModeState(false, true);
                    Otto.post(LocationTrackingService.STOP_SERVICE);
                }
                return true;
            }
        });
    }

    private void changeTravelModeState(boolean state, boolean showToast)
    {
        boolean currentState = mPreferences.getBoolean(LocationTrackingService.KEY_TRAVELLING_MODE, false);
        if (currentState != state || mTravelModeUiState != state)
        {
            mPreferences.edit()
                    .putBoolean(LocationTrackingService.KEY_TRAVELLING_MODE, state)
                    .apply();

            String toast = getString(R.string.travel_mode_off);

            if (state)
            {
                toast = getString(R.string.travel_mode_on);
                buttonMyLoc.setColorNormal(travelModeColor);
                buttonMyLoc.setColorPressedResId(R.color.travel_mode_pressed);
                mTravelModeUiState = true;

            }
            else
            {
                buttonMyLoc.setColorNormal(accentColor);
                buttonMyLoc.setColorPressedResId(R.color.colorAccentPressed);
                mTravelModeUiState = false;
            }

            if (showToast)
            {
                toast(toast);
            }
        }
    }

    private void startLocationTrackingService()
    {
        Log.d("MapFragmentFlowLogs", "startLocationTrackingService");
        Intent intent = new Intent(mActivity, LocationTrackingService.class);
        mActivity.startService(intent);
    }

    private void addPhoto()
    {
        hideSubmitButtonAndShowAddButton();
        hideToast();
        Intent intent = new Intent(mActivity, AddIssueActivity.class);
        intent.putExtra(LATLNG, mGeoCodeLatLng);
        intent.putExtra(ADDRESS, searchText.getText().toString());
        intent.putExtra(RADIUS, getRadiusInMeter());
        startActivity(intent);
    }


    private CameraPosition getCameraPos(LatLng latLng)
    {
        return new CameraPosition(latLng, getZoom(), getTilt(), getBearing());
    }

    private LatLng getLatLng()
    {
        double latitude = Double.parseDouble(mPreferences.getString(LATITUDE, "12.9667"));
        double longitude = Double.parseDouble(mPreferences.getString(LONGITUDE, "77.5667"));
        return new LatLng(latitude, longitude);
    }

    private float getBearing()
    {
        return mPreferences.getFloat(BEARING, 0);
    }

    private float getZoom()
    {
        float zoom = mPreferences.getFloat(ZOOM, 16);
        if (zoom < 3)
        {
            zoom = 16;
        }
        return zoom;
    }

    private float getTilt()
    {
        return mPreferences.getFloat(TILT, 0);
    }

    private float getAccuracy()
    {
        return mPreferences.getFloat(ACCURACY, 500);
    }

    private void setCamera(CameraUpdate update, boolean animate)
    {
        if (animate)
        {
            mGoogleMap.animateCamera(update);
        }
        else
        {
            mGoogleMap.moveCamera(update);
        }
    }

    private void saveLocation(Location location)
    {
        mPreferences.edit().putString(LATITUDE, "" + location.getLatitude())
                .putString(LONGITUDE, "" + location.getLongitude())
                .putFloat(ACCURACY, location.getAccuracy())
                .apply();
    }

    public void gotoLatLng(LatLng latLng, boolean animate)
    {
        setMapType();
        setCamera(CameraUpdateFactory.newCameraPosition(getCameraPos(latLng)), animate);
    }

    private void setMapType()
    {
        int mapType = Integer.parseInt(mPreferences.getString(MAP_TYPE, "1"));
        if (mGoogleMap.getMapType() != mapType)
        {
            mGoogleMap.setMapType(mapType);
        }
    }

    private int getSeekBarValue()
    {
        int progress = radiusSeekBar.getProgress() + 1;
        switch (mRadiusType)
        {
            case R.id.radius_fts:
                return progress * 100;
            case R.id.radius_km:
                return progress;
            case R.id.radius_mi:
                return progress;
            default:
                return progress * 100;
        }
    }

    private void toast(String str)
    {
        Toast toast = Toast.makeText(mActivity, str, Toast.LENGTH_SHORT);
        ViewGroup view = (ViewGroup) toast.getView();
        ((TextView) view.getChildAt(0)).setGravity(Gravity.CENTER);
        toast.setView(view);
        toast.show();
    }

    @Override
    public View getView()
    {
        return mOriginalContentView;
    }

    @Override
    public void onActionUp()
    {
        mIsActionDown = false;
        TransitionUtil.slideTransition(map_fab_buttons);
        map_fab_buttons.setVisibility(View.VISIBLE);

        TransitionUtil.defaultTransition(searchBar);

        RelativeLayout.LayoutParams searchBarLayoutParams = (RelativeLayout.LayoutParams) searchBar.getLayoutParams();
        searchBarLayoutParams.topMargin = searchBarMargin;
        searchBar.setLayoutParams(searchBarLayoutParams);

        mToolbar.animate().translationY(0).start();

        if (isAddPhotoButtonShown && mOnActionDownLatLng.equals(mGeoCodeLatLng))
        {
            drawCircleOnMap();
        }
        onCameraChange(mCameraPosition);
    }

    @Override
    public void onScroll()
    {
        changeTravelModeState(false, true);
    }

    @Override
    public void onDrag()
    {
        changeTravelModeState(false, true);
    }

    @Override
    public void onActionDown()
    {
        mIsActionDown = true;
        mOnActionDownLatLng = mGeoCodeLatLng;
        TransitionUtil.slideTransition(map_fab_buttons);
        map_fab_buttons.setVisibility(View.GONE);
        mToolbar.animate().translationY(-mToolbar.getBottom()).start();

        TransitionUtil.defaultTransition(searchBar);

        RelativeLayout.LayoutParams searchBarLayoutParams = (RelativeLayout.LayoutParams) searchBar.getLayoutParams();
        searchBarMargin = searchBarLayoutParams.topMargin;
        searchBarLayoutParams.topMargin = 0;
        searchBar.setLayoutParams(searchBarLayoutParams);

        if (isAddPhotoButtonShown)
        {
            removeActionCircle();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        try
        {
            switch (requestCode)
            {
                case PLACE_AUTOCOMPLETE_REQUEST_CODE:
                    Place place = PlaceAutocomplete.getPlace(mActivity, data);
                    Log.d("PlacesApi", "" + place);
                    mIsPlacesApiResult = true;

                    switch (resultCode)
                    {
                        case Activity.RESULT_OK:
                            mGeoCodeLatLng = place.getLatLng();
                            Log.d("PlacesApi", "" + place.getAddress());
                            Log.d("PlacesApi", "" + place.getLatLng());
                            Log.d("PlacesApi", "" + place.getName());

                            gotoLatLng(mGeoCodeLatLng, true);
                            searchText.setText(place.getAddress());
                            break;
                        case PlaceAutocomplete.RESULT_ERROR:
                            Log.d("PlacesApi", "RESULT_ERROR : " + data);
                            toast(SLOW_INTERNET);
                            break;
                        case Activity.RESULT_CANCELED:
                            Log.d("PlacesApi", "RESULT_CANCELED");
                            break;
                    }
                    break;
            }
        }catch (NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onLocationChanged(Location location)
    {
        saveLocation(location);
        showMyLocOnMap(true);
    }

    private void showMyLocationMarkerAndCircle(LatLng latLng, float accuracy)
    {
        if (myLocMarker != null)
        {
            myLocMarker.remove();
        }

        myLocMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .zIndex(10)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.my_loc_dot)));

        if (myLocCircle != null)
        {
            myLocCircle.remove();
        }
        myLocCircle = mGoogleMap.addCircle(new CircleOptions()
                .radius(accuracy)
                .strokeWidth(2)
                .strokeColor(accentColor)
                .fillColor(radiusColor)
                .center(latLng));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition)
    {
        mCameraPosition = cameraPosition;
        if (!mIsActionDown)
        {
            double dist = distFrom(cameraPosition.target, getLatLng());
            if (dist > 100)
            {
                changeTravelModeState(false, true);
            }
            mCurrentZoom = cameraPosition.zoom;
            mGeoCodeLatLng = cameraPosition.target;
            updateLocationInfo();
            mRetryAttemptsCount = 0;
            if (isAddPhotoButtonShown)
            {
                drawCircleOnMap();
            }
        }
    }

    private void drawCircleOnMap()
    {
        int mRadius = getRadiusInMeter();
        Log.d("MapFragmentFlowLogs", "zoom : radius : " + mRadius);
        showActionCircle(mGeoCodeLatLng, mRadius);
        setZoomLevel(mRadius);
    }

    private int getRadiusInMeter()
    {
        int radius = getSeekBarValue();
        switch (mRadiusType)
        {
            case R.id.radius_fts:
                return (int) Math.floor(radius / 3.28084);
            case R.id.radius_km:
                return (int) Math.floor(radius * 1000);
            case R.id.radius_mi:
                return (int) Math.floor(radius * 1609.344051499);
            default:
                return radius;
        }
    }

    private void showActionCircle(LatLng latLng, int radius)
    {
        removeActionCircle();
        mActionCircle = mGoogleMap.addCircle(new CircleOptions()
                .radius(radius)
                .strokeWidth(2)
                .strokeColor(accentColor)
                .fillColor(radiusColor)
                .center(latLng));
    }

    @SuppressWarnings("UnusedAssignment")
    private void setZoomLevel(int radius)
    {
        double zoom = 16;
        if (radius < 400)
        {
            zoom = 16;
        }
        else if (radius < 600)
        {
            zoom = 15.5;
        }
        else if (radius < 800)
        {
            zoom = 15;
        }
        else if (radius < 1000)
        {
            zoom = 14.5;
        }
        else if (radius < 2000)
        {
            zoom = 14;
        }
        else if (radius < 3000)
        {
            zoom = 13;
        }
        else if (radius < 5000)
        {
            zoom = 12.5;
        }
        else if (radius < 6000)
        {
            zoom = 12;
        }
        else if (radius < 8000)
        {
            zoom = 11.5;
        }
        else if (radius < 10000)
        {
            zoom = 11;
        }
        else if (radius < 12000)
        {
            zoom = 10.5;
        }
        else
        {
            zoom = 10;
        }

        if (!isCurrentZoomSetByUser(mCurrentZoom))
        {
            if (zoom != mCurrentZoom)
            {
                if (mGoogleMap != null && mGeoCodeLatLng != null)
                {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mGeoCodeLatLng, (float) zoom));
                }
            }
        }
    }

    private boolean isCurrentZoomSetByUser(float currentZoom)
    {
        double[] zooms = {16, 15.5, 15, 14.5, 14, 13, 12.5, 12, 11.5, 11, 10.5, 10};
        for (double zoom : zooms)
        {
            if (zoom == currentZoom)
            {
                return false;
            }
        }
        return true;
    }

    private void updateLocationInfo()
    {
        Log.d("MapFragmentFlowLogs", "updateLocationInfo");

        if (PermissionUtil.isConnected(mActivity))
        {
            try
            {
                if (mGeoCoderTask != null && mGeoCodeLatLng != null && mGeoCoderTask.mLatLng != null)
                {
                    double dist = distFrom(mGeoCoderTask.mLatLng, mGeoCodeLatLng);
                    if (dist > 5 || searchText.getText().equals(NO_INTERNET) || searchText.getText().equals(SLOW_INTERNET))
                    {
                        showSearchProgress();
                        cancelAsyncTask(mGeoCoderTask);
                        mGeoCoderTask = new GeoCoderTask(mActivity, mGeoCodeLatLng, this);
                        mGeoCoderTask.execute(mRetryAttemptsCount++);
                    }
                }
                else
                {
                    showSearchProgress();
                    mGeoCoderTask = new GeoCoderTask(mActivity, mGeoCodeLatLng, this);
                    mGeoCoderTask.execute(mRetryAttemptsCount++);
                }
            }catch (NullPointerException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            searchText.setText(NO_INTERNET);
        }

    }

    private void cancelAsyncTask(AsyncTask task)
    {
        if (task != null)
        {
            AsyncTask.Status status = task.getStatus();
            if (status == AsyncTask.Status.RUNNING || status == AsyncTask.Status.PENDING)
            {
                task.cancel(true);
            }
        }
    }


    @Override
    public void onAddressObtained(String result)
    {
        hideSearchProgress();
        Log.d("MapFragmentFlowLogs", "onAddressObtained");
        if (result == null)
        {
            searchText.setText(SLOW_INTERNET);
        }
        else
        {
            searchText.setText(result);
        }
    }

    private void showSearchProgress()
    {
        mSearchProgress.setVisibility(View.VISIBLE);
        mSettingsIcon.setVisibility(View.GONE);
    }

    private void hideSearchProgress()
    {
        mSearchProgress.setVisibility(View.GONE);
        mSettingsIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGeoCodingFailed()
    {
        hideSearchProgress();
        if (mRetryAttemptsCount < 10)
        {
            updateLocationInfo();
        }
        else
        {
            onAddressObtained(null);
        }
    }

    @Override
    public void onCancelled()
    {
        hideSearchProgress();
        mRetryAttemptsCount = 0;
    }

    @OnClick(R.id.settingsIcon)
    public void showSettings()
    {
        FragmentManager mFragmentManager = mActivity.getFragmentManager();
        SettingsFragment settingsFragment = (SettingsFragment) mFragmentManager.findFragmentByTag(FRAGMENT_TAG_SETTINGS);
        if (settingsFragment == null)
        {
            settingsFragment = new SettingsFragment();
        }
        mFragmentManager.executePendingTransactions();
        if (!settingsFragment.isAdded())
        {
            settingsFragment.show(mFragmentManager, FRAGMENT_TAG_SETTINGS);
        }
        hideToast();
    }

    @SuppressWarnings("WeakerAccess")
    @OnClick({R.id.searchIcon, R.id.hoverPlaceName})
    public void searchPlace(final View view)
    {
        view.setClickable(false);
        changeTravelModeState(false, false);
        Log.d("mSearchWaitProgress", "visible");
        if (PermissionUtil.isConnected(mActivity))
        {
            mSearchWaitProgress.setVisibility(View.VISIBLE);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        PlaceAutocomplete.IntentBuilder builder = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);
                        startActivityForResult(builder.build(mActivity), PLACE_AUTOCOMPLETE_REQUEST_CODE);
                    }
                    catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e)
                    {
                        Log.d("mSearchWaitProgress", e.getMessage() + ", should have been handled in Loading activity");
                    }
                }
            }, 500);

        }
        else
        {
            toast(NO_INTERNET);
        }
    }

    @Subscribe
    public void ottoListener(String action)
    {
        Log.d("ConnectivityListener", "onInternetConnected : map fragment : " + action);
        switch (action)
        {
            case DIALOG_DISMISS:
                reloadMarkersAndCircles();
                showMyLocationMarkerAndCircle(getLatLng(), getAccuracy());
                break;
            case InternetConnectivityListener.INTERNET_CONNECTED:
                updateLocationInfo();
                break;
            case TURN_OFF_TRAVEL_MODE:
                changeTravelModeState(false, false);
                break;
            case TURN_ON_TRAVEL_MODE:
                changeTravelModeState(true, false);
                break;
            case MAP_TYPE_CHANGED:
                setMapType();
                break;
        }
    }

    private boolean getTravelMode()
    {
        return mPreferences.getBoolean(LocationTrackingService.KEY_TRAVELLING_MODE, false);
    }

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        String id = mMarkerAndCircle.getId(marker.getId());
        if (id != null)
        {
            Issue issue = IssuesDao.getIssue(id);
            if (issue != null)
            {
                Intent intent = new Intent(mActivity, IssueActivity.class);
                intent.putExtra(ISSUE_DATA, issue);
                mActivity.startActivity(intent);
                return true;
            }
        }
        return false;
    }
}