package co.thnki.whistleblower.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.util.List;

import co.thnki.whistleblower.MainActivity;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.services.RemoteConfigService;

public class NotificationActionReceiver extends BroadcastReceiver
{
    public static final String NOTIFICATION_ACTION = "notificationAction";
    public static final String CANCEL_ALL_ALARMS = "CancelAllAlarms";
    public static final int NOTIFICATION_ID_LOCATION_ALARMS = 181;
    public static final int NOTIFICATION_ID_APP_UPDATE = 182;
    public static final String UPDATE_APP = "updateApp";
    public static final String CANCEL_UPDATE = "cancelUpdate";

    public NotificationActionReceiver()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getStringExtra(NOTIFICATION_ACTION);
        SharedPreferences preferences = WhistleBlower.getPreferences();
        String pkgName = preferences.getString(RemoteConfigService.PACKAGE_NAME, context.getPackageName());

        Log.d(NOTIFICATION_ACTION, "NOTIFICATION_ACTION : " + action + ", pkgName : "+pkgName);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        switch (action)
        {
            case MainActivity.TAG:
                Intent intentMainActivity = new Intent(context, MainActivity.class);
                intentMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentMainActivity);
                break;
            case UPDATE_APP:
                openAppInGooglePlay(context, pkgName);
                notificationManager.cancel(NOTIFICATION_ID_APP_UPDATE);
                preferences.edit().putBoolean(pkgName, true).apply();
                break;
            case CANCEL_UPDATE:
                notificationManager.cancel(NOTIFICATION_ID_APP_UPDATE);
                preferences.edit().putBoolean(pkgName, false).apply();
        }
    }

    private static void openAppInGooglePlay(Context context, String packageName)
    {
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        boolean marketFound = false;

        // find all applications able to handle our rateIntent
        final List<ResolveInfo> otherApps = context.getPackageManager().queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp : otherApps)
        {
            // look for Google Play application
            if (otherApp.activityInfo.applicationInfo.packageName.equals("com.android.vending"))
            {
                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(
                        otherAppActivity.applicationInfo.packageName,
                        otherAppActivity.name
                );
                rateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                rateIntent.setComponent(componentName);
                context.startActivity(rateIntent);
                marketFound = true;
                break;

            }
        }

        // if GP not present on device, open web browser
        if (!marketFound)
        {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(webIntent);
        }
    }
}
