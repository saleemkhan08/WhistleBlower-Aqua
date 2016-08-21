package co.thnki.whistleblower.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import co.thnki.whistleblower.BuildConfig;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.pojos.NotificationData;
import co.thnki.whistleblower.receivers.NotificationActionReceiver;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.NotificationsUtil;

public class RemoteConfigService extends Service
{
    public static final String AD_UNIT_ID = "adUnitId";
    private static final String LATEST_VERSION_NAME = "latestVersionName";
    private static final String NEW_APP_ICON = "newAppIcon";
    private static final String NEW_APP_NAME = "newAppName";
    private static final String NOTIFICATION_TEXT = "notificationText";
    public static final String PACKAGE_NAME = "newAppPkgName";
    private static final String TAG = "UpdateCheckService";
    private static final String NOTIFICATION_TIME = "notificationTime";

    private SharedPreferences mSharedPreferences;
    private FirebaseRemoteConfig mFireBaseRemoteConfig;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("ConnectivityListener", "RemoteConfigService : onStartCommand" );
        mFireBaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .build();

        mFireBaseRemoteConfig.setConfigSettings(configSettings);
        mFireBaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);

        mSharedPreferences = WhistleBlower.getPreferences();

        long cacheExpiration = 0;
        mFireBaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            Log.d(TAG, "Fetch Succeeded");
                            mFireBaseRemoteConfig.activateFetched();
                            onFetched();
                        }
                        else
                        {
                            Log.d(TAG, "Fetch failed : " + task.toString());
                            finishJob();
                        }
                    }
                });

        defaultTask();
        updateIssueTable();
        return START_NOT_STICKY;
    }

    private void updateIssueTable()
    {
        startService(new Intent(this, NewsFeedsUpdateService.class));
    }

    private void finishJob()
    {
        if (Build.VERSION.SDK_INT >= 21)
        {
            Otto.post(InternetJobService.JOB_FINISHED);
        }
        stopSelf();
    }

    private void onFetched()
    {
        mSharedPreferences.edit()
                .putString(LATEST_VERSION_NAME, mFireBaseRemoteConfig.getString(LATEST_VERSION_NAME))
                .putString(PACKAGE_NAME, mFireBaseRemoteConfig.getString(PACKAGE_NAME))
                .putString(NEW_APP_ICON, mFireBaseRemoteConfig.getString(NEW_APP_ICON))
                .putString(NEW_APP_NAME, mFireBaseRemoteConfig.getString(NEW_APP_NAME))
                .putString(NOTIFICATION_TEXT, mFireBaseRemoteConfig.getString(NOTIFICATION_TEXT))
                .putString(AD_UNIT_ID + "1", mFireBaseRemoteConfig.getString("adUnitId1"))
                .putString(AD_UNIT_ID + "2", mFireBaseRemoteConfig.getString("adUnitId2"))
                .putString(AD_UNIT_ID + "3", mFireBaseRemoteConfig.getString("adUnitId3"))
                .apply();

        finishJob();
    }


    private void defaultTask()
    {
        String versionCode = mSharedPreferences.getString(LATEST_VERSION_NAME, mFireBaseRemoteConfig.getString(LATEST_VERSION_NAME));
        String newAppPkgName = mSharedPreferences.getString(PACKAGE_NAME, mFireBaseRemoteConfig.getString(PACKAGE_NAME));
        String newAppIcon = mSharedPreferences.getString(NEW_APP_ICON, mFireBaseRemoteConfig.getString(NEW_APP_ICON));
        String newAppName = mSharedPreferences.getString(NEW_APP_NAME, mFireBaseRemoteConfig.getString(NEW_APP_NAME));
        String notificationText = mSharedPreferences.getString(NOTIFICATION_TEXT, mFireBaseRemoteConfig.getString(NOTIFICATION_TEXT));

        Log.d("UpdateCheckService", "versionCode : " + versionCode);
        Log.d("UpdateCheckService", "BuildConfig.VERSION_CODE : " + BuildConfig.VERSION_CODE);
        Log.d("UpdateCheckService", "versionCode status preference : " + mSharedPreferences.getBoolean(versionCode, true));

        Log.d("UpdateCheckService", "newAppPkgName : " + newAppPkgName);
        Log.d("UpdateCheckService", "newAppIcon : " + newAppIcon);
        Log.d("UpdateCheckService", "newAppName : " + newAppName);
        Log.d("UpdateCheckService", "notificationText : " + notificationText);


        long currentTime = System.currentTimeMillis();
        long difference = currentTime - mSharedPreferences.getLong(NOTIFICATION_TIME, 0);

        if (difference > (1000*3*24*60*60))
        {
            Log.d("VERSION_CODE", BuildConfig.VERSION_CODE + " = "+versionCode);
            int configVersion = Integer.parseInt(versionCode);
            if (configVersion > BuildConfig.VERSION_CODE && mSharedPreferences.getBoolean(versionCode, true))
            {
                Log.d("UpdateCheckService", "Update Available");
                NotificationData data = new NotificationData();
                data.action1IntentIcon = R.mipmap.install_grey;
                data.action1IntentTag = NotificationActionReceiver.UPDATE_APP;
                data.action1IntentText = "Install";
                data.contentText = "New version is available!";
                data.contentTitle = getString(R.string.app_name);
                data.action2IntentIcon = R.mipmap.reject_grey;
                data.action2IntentTag = NotificationActionReceiver.CANCEL_UPDATE;
                data.action2IntentText = "Cancel";
                data.notificationId = NotificationActionReceiver.NOTIFICATION_ID_APP_UPDATE;
                data.vibrate = true;
                NotificationsUtil.showNotification(data);
                //to update the status of installation, on action obtained from notification
                mSharedPreferences.edit()
                        .putLong(NOTIFICATION_TIME, currentTime)
                        .putString(PACKAGE_NAME, versionCode)
                        .apply();
            }
            else if (!mSharedPreferences.contains(newAppPkgName))
            {
                if (!isAppInstalled(newAppPkgName))
                {
                    Log.d("UpdateCheckService", "getNewAppPkgName : " + newAppPkgName);
                    NotificationData data = new NotificationData();
                    data.action1IntentIcon = R.mipmap.install_grey;
                    data.action1IntentTag = NotificationActionReceiver.UPDATE_APP;
                    data.action1IntentText = "Install";

                    data.largeIconUrl = newAppIcon;
                    data.contentIntentTag = NotificationActionReceiver.UPDATE_APP;
                    data.contentTitle = newAppName;
                    data.contentText = notificationText;

                    data.action2IntentIcon = R.mipmap.reject_grey;
                    data.action2IntentTag = NotificationActionReceiver.CANCEL_UPDATE;
                    data.action2IntentText = "Cancel";
                    data.notificationId = NotificationActionReceiver.NOTIFICATION_ID_APP_UPDATE;
                    data.vibrate = true;
                    NotificationsUtil.showNotification(data);
                    mSharedPreferences.edit().putLong(NOTIFICATION_TIME, currentTime).apply();
                }
            }
        }
    }

    private boolean isAppInstalled(String uri)
    {
        PackageManager pm = getPackageManager();
        boolean app_installed;
        try
        {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            app_installed = false;
        }
        return app_installed;
    }
}
