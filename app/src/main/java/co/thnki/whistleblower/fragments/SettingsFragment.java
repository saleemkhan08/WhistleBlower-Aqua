package co.thnki.whistleblower.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.thnki.whistleblower.LoginActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.singletons.Otto;

import static co.thnki.whistleblower.pojos.Accounts.LOGIN_STATUS;

public class SettingsFragment extends DialogFragment
{
    private static final String MAP_TYPE_TEXT = "mapTypeText";
    public static final String SHOW_HINTS = "showHints";
    private SharedPreferences mPreference;

    @Bind(R.id.mapTypeExpandList)
    View mMapTypeExpandList;

    @Bind(R.id.mapTypeValue)
    TextView mMapTypeValue;

    @Bind(R.id.hintsSettingValue)
    TextView mHintsSettingValue;
    private boolean mIsMapTypeExpanded;

    public SettingsFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parentView = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.bind(this, parentView);
        Otto.register(this);
        TextView dialogTitle = (TextView) parentView.findViewById(R.id.dialogTitle);
        dialogTitle.setTypeface(WhistleBlower.getTypeFace());
        mPreference = WhistleBlower.getPreferences();
        mMapTypeValue.setText(mPreference.getString(MAP_TYPE_TEXT, getString(R.string.normal)));
        mHintsSettingValue.setText(mPreference.getBoolean(SHOW_HINTS,false) ? getString(R.string.show) : getString(R.string.hide));
        return parentView;
    }

    @OnClick(R.id.mapTypeDisplay)
    public void expandMapTypeList()
    {
        if (mIsMapTypeExpanded)
        {
            mMapTypeExpandList.setVisibility(View.GONE);
            mIsMapTypeExpanded = false;
        }
        else
        {
            mMapTypeExpandList.setVisibility(View.VISIBLE);
            mIsMapTypeExpanded = true;
        }
    }

    @OnClick({R.id.normal, R.id.satellite, R.id.terrain, R.id.hybrid})
    public void setMapType(View view)
    {
        String mapType = "1";
        String mapTypeText = getString(R.string.normal);
        switch (view.getId())
        {
            case R.id.normal:
                mapType = "1";
                mapTypeText = getString(R.string.normal);
                break;
            case R.id.satellite:
                mapType = "2";
                mapTypeText = getString(R.string.satellite);
                break;
            case R.id.terrain:
                mapType = "3";
                mapTypeText = getString(R.string.terrain);
                break;
            case R.id.hybrid:
                mapType = "4";
                mapTypeText = getString(R.string.hybrid);
                break;
        }
        mMapTypeValue.setText(mapTypeText);
        mPreference.edit().putString("mapType", mapType).apply();
        mPreference.edit().putString(MAP_TYPE_TEXT, mapTypeText).apply();
        Otto.post(MapFragment.MAP_TYPE_CHANGED);
        expandMapTypeList();
    }

    @OnClick(R.id.closeDialog)
    public void close()
    {
        dismiss();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        super.onDismiss(dialog);
        Otto.post(MapFragment.DIALOG_DISMISS);
        Otto.unregister(this);
    }

    @OnClick(R.id.logoutSettings)
    public void logout()
    {
        mPreference.edit().putBoolean(LOGIN_STATUS, false).apply();
        Activity activity = getActivity();
        activity.startActivity(new Intent(activity, LoginActivity.class));
    }

    @OnClick(R.id.hintSettings)
    public void hideHints()
    {
        if(mPreference.getBoolean(SHOW_HINTS, false))
        {
            mHintsSettingValue.setText(getString(R.string.hide));
            mPreference.edit().putBoolean(SHOW_HINTS, false).apply();
        }
        else
        {
            mHintsSettingValue.setText(getString(R.string.show));
            mPreference.edit().putBoolean(SHOW_HINTS, true).apply();
        }
    }

}
