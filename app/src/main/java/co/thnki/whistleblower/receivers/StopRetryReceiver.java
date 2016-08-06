package co.thnki.whistleblower.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.services.AddIssueService;
import co.thnki.whistleblower.services.UploadIssueService;

public class StopRetryReceiver extends BroadcastReceiver
{
    public StopRetryReceiver()
    {
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("StopRetryReceiver", "Retry : "+intent.hasExtra(AddIssueService.RETRY)+", Cancel : "+intent.hasExtra(AddIssueService.CANCEL));
        NotificationManager mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyManager.cancel(UploadIssueService.NOTIFICATION_ID);

        Intent serviceIntent = new Intent(context, AddIssueService.class);
        context.stopService(serviceIntent);

        if (intent.hasExtra(UploadIssueService.RETRY))
        {
            serviceIntent = new Intent(context, AddIssueService.class);
            serviceIntent.putExtra(IssuesDao.IMAGE_LOCAL_URI, intent.getStringExtra(IssuesDao.IMAGE_LOCAL_URI));
            serviceIntent.putExtra(IssuesDao.AREA_TYPE, intent.getStringExtra(IssuesDao.AREA_TYPE));
            serviceIntent.putExtra(IssuesDao.DESCRIPTION, intent.getStringExtra(IssuesDao.DESCRIPTION));
            context.startService(serviceIntent);
        }
    }
}
