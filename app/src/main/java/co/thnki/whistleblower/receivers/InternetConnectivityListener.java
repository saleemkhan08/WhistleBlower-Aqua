package co.thnki.whistleblower.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import co.thnki.whistleblower.services.RemoteConfigService;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;

public class InternetConnectivityListener extends BroadcastReceiver
{
    public static final String INTERNET_CONNECTED = "INTERNET_CONNECTED";
    public static final String INTERNET_DISCONNECTED = "INTERNET_DISCONNECTED";

    public InternetConnectivityListener()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("ConnectivityListener", "onReceive");
        if (intent.getExtras() != null)
        {
            if (ConnectivityUtil.isConnected())
            {
                Log.d("RemoteConfigService", "RemoteConfigService Started  : InternetConnectivityListener");
                Otto.post(INTERNET_CONNECTED);

                if (Build.VERSION.SDK_INT < 21)
                {
                    context.startService(new Intent(context, RemoteConfigService.class));
                }
            }
            else
            {
                Otto.post(INTERNET_DISCONNECTED);
            }
        }
    }
}