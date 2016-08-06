package co.thnki.whistleblower;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.ButterKnife;
import co.thnki.whistleblower.fragments.MapFragment;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.receivers.InternetConnectivityListener;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.LocationUtil;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.COLLAPSED;
import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState.EXPANDED;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = "LocationAlarmMain";
    private static final String MAP_FRAGMENT = "mapFragment";

    @Bind(R.id.sliding_layout)
    SlidingUpPanelLayout mLayout;

    @Bind(R.id.descriptionText)
    TextView mDescriptionText;

    @Bind(R.id.scrollUp)
    ImageView scrollUp;

    @BindColor(R.color.colorAccent)
    int mAccentColor;

    @BindColor(R.color.my_location_radius)
    int mRadiusColor;

    @Bind(R.id.mapContainer)
    ViewGroup mMapFragmentView;

    @Bind(R.id.titleText)
    TextView titleText;

    @Bind(R.id.titleBar)
    RelativeLayout mTitleBar;
    private MapFragment mMapFragment;


    private GoogleApiClient client;
    @BindString(R.string.tapToViewList)
    String mTapToViewTheList;

    @BindString(R.string.tapToViewMap)
    String mTapToViewTheMap;
    private SharedPreferences mPreferences;
    private NativeExpressAdView mAdView;
    private SlidingUpPanelLayout.PanelState mSlidingToolbarState;

    /**
     * Fragment Content
     */

    public static final String ALARM_LIST_EMPTY_TEXT = "ALARM_LIST_EMPTY_TEXT";
    public static final String RELOAD_LIST = "reloadList";

    @BindString(R.string.noLocationAlarmsAreSet)
    String youHaventSetAnyLocationAlarm;

    @Bind(R.id.emptyList)
    ViewGroup emptyList;

    @Bind(R.id.emptyListTextView)
    TextView emptyListTextView;

    @Bind(R.id.locationAlarmList)
    RecyclerView mLocationAlarmList;

    @Bind(R.id.card_view)
    CardView adCardView;

    /**
     * End of Fragment Content
     */

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("LagIssue", "onCreate  : MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_sliding_up);
        mPreferences = WhistleBlower.getPreferences();
        ButterKnife.bind(this);
        Otto.register(this);
        setUpSlidingToolbar();

        initializeAppBarLayout();
        initializeAppIndexing();

        if (Build.VERSION.SDK_INT >= 21)
        {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private void initializeAppBarLayout()
    {
        FragmentManager manager = getSupportFragmentManager();
        addMapFragment(manager);
        TextView title = (TextView) findViewById(R.id.titleText);
        title.setTypeface(WhistleBlower.getTypeFace());
    }

    private void initializeAppIndexing()
    {
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    private void addMapFragment(FragmentManager manager)
    {
        Log.d("LagIssue", "addMapFragment  : MainActivity");
        mMapFragment = new MapFragment();
        manager.beginTransaction()
                .replace(R.id.mapContainer, mMapFragment, MAP_FRAGMENT)
                .commit();
    }

    @Override
    public void onBackPressed()
    {
        if (mSlidingToolbarState == EXPANDED)
        {
            mLayout.setPanelState(COLLAPSED);
        }
        else if (mMapFragment != null && mMapFragment.isAddPhotoButtonShown)
        {
            mMapFragment.hideSubmitButtonAndShowAddButton();
        }
        else
        {
            super.onBackPressed();
        }
    }


    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Otto.post(RELOAD_LIST);
    }


    @Subscribe
    public void showIssueOnMap(Issue issue)
    {
        mLayout.setPanelState(COLLAPSED);
        mMapFragment.gotoLatLng(LocationUtil.getLatLng(issue.latitude, issue.longitude), true);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Otto.unregister(this);
    }

    @Subscribe
    public void onInternetConnected(String action)
    {
        Log.d("ConnectivityListener", "onInternetConnected : main activity : " + action);
        switch (action)
        {
            case InternetConnectivityListener.INTERNET_CONNECTED:
                break;
        }
    }

    private Action getIndexApiAction()
    {
        Thing object = new Thing.Builder()
                .setName(getString(R.string.app_name))
                .setUrl(Uri.parse("http://www.thnki.co"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop()
    {
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private void setUpSlidingToolbar()
    {

        mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener()
        {
            @Override
            public void onPanelSlide(View panel, float slideOffset)
            {
                if (slideOffset > 0.5)
                {
                    mDescriptionText.setText(mTapToViewTheMap);
                    scrollUp.setImageResource(R.mipmap.scroll_down);
                }
                else
                {
                    mDescriptionText.setText(mTapToViewTheList);
                    scrollUp.setImageResource(R.mipmap.scroll_up);
                }
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState)
            {
                //Log.i(TAG, "onPanelStateChanged " + newState);
                mSlidingToolbarState = newState;
                if (newState == COLLAPSED)
                {
                    Otto.post(MapFragment.DIALOG_DISMISS);
                }
            }
        });
        mLayout.setFadeOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mLayout.setPanelState(COLLAPSED);
            }
        });
    }
}