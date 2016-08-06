package co.thnki.whistleblower;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.widget.Toast;

import co.thnki.whistleblower.receivers.InternetConnectivityListener;

public class WhistleBlower extends MultiDexApplication
{
    private static Context context;
    private InternetConnectivityListener mReceiver;
    @Override
    public void onCreate()
    {
        super.onCreate();
        context = this.getApplicationContext();
        if(Build.VERSION.SDK_INT > 23)
        {
            registerInternetConnectionReceiver();
        }
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        if(Build.VERSION.SDK_INT > 23)
        {
            unregisterReceiver(mReceiver);
            Log.d("ConnectivityListener", "un - Registered" );
        }
    }

    private void registerInternetConnectionReceiver()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mReceiver = new InternetConnectivityListener();
        registerReceiver(mReceiver, filter);
        Log.d("ConnectivityListener", "Registered");
    }

    public static SharedPreferences getPreferences()
    {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void toast(String str)
    {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }
    public static Typeface getTypeFace()
    {
        return Typeface.createFromAsset(context.getAssets(), "Gabriola.ttf");
    }
    public static Context getAppContext()
    {
        return context;
    }


}