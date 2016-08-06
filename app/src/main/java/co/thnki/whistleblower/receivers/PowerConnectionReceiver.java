package co.thnki.whistleblower.receivers;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import co.thnki.whistleblower.services.InternetJobService;

public class PowerConnectionReceiver extends WakefulBroadcastReceiver
{
    private static int sJobId;

    public PowerConnectionReceiver()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("ConnectivityListener", "status : "+intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_BATTERY_OKAY) || intent.getAction().equals(Intent.ACTION_POWER_CONNECTED))
        {
            Log.d("ConnectivityListener", "Charging");
            if (Build.VERSION.SDK_INT >= 21)
            {
                ComponentName mServiceComponent = new ComponentName(context, InternetJobService.class);
                JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                JobInfo.Builder builder = new JobInfo.Builder(sJobId++, mServiceComponent);
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);//when any network is available.
                builder.setRequiresDeviceIdle(false); //when screen is off
                builder.setRequiresCharging(false); // when phone is put for charging
                scheduler.schedule(builder.build());
            }
        }else
        {
            Log.d("ConnectivityListener", "Not Charging");
        }
    }
}
