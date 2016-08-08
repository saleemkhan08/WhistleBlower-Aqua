package co.thnki.whistleblower.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.android.volley.VolleyError;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import co.thnki.whistleblower.AddIssueActivity;
import co.thnki.whistleblower.IssueActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.VolleyUtil;

import static co.thnki.whistleblower.services.NewsFeedsUpdateService.ISSUES_FEEDS_UPDATED;

public class AddIssueService extends Service implements ResultListener<String>
{
    private static final String ACTION_ADD_ISSUE = "addIssue";
    private static final String FILE_NAME = "image";
    private static final String ACTION_UPDATE_ISSUE = "updateIssue";
    private NotificationManager mNotifyManager;
    private static final int NOTIFICATION_ID = 805;
    private static final String ERROR = "ERROR";
    private NotificationCompat.Builder mBuilder;
    private Context mContext;
    private Issue mIssue;

    public AddIssueService()
    {
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent != null)
        {
            mContext = this;
            mIssue = intent.getParcelableExtra(AddIssueActivity.ISSUE_DATA);
            if (mIssue == null)
            {
                mIssue = new Issue();
            }

            mBuilder = new NotificationCompat.Builder(mContext)
                    .setSmallIcon(R.mipmap.bullhorn)
                    .setOngoing(true)
                    .setContentTitle("Posting the Issue.")
                    .setProgress(0, 0, true)
                    .setAutoCancel(false);

            mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            Log.d("AddIssueService", "NOTIFICATION Shown");
            new UploadTask().execute();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class UploadTask extends AsyncTask<Object, Object, Void>
    {
        @Override
        protected Void doInBackground(Object... voids)
        {
            Map<String, String> map = new HashMap<>();
            Log.d("AddIssueService", "Upload : " + mIssue.imgUrl);
            if (!mIssue.imgUrl.contains("http://") && !mIssue.imgUrl.contains("https://"))
            {
                try
                {
                    Bitmap image = BitmapFactory.decodeFile(mIssue.imgUrl);
                    String converted = getStringImage(image);
                    map.put(FILE_NAME, converted);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.d("AddIssueService", "Exception : " + e.getMessage());
                    onError();
                }
            }

            if (mIssue.issueId.equals(AddIssueActivity.NEW_ISSUE))
            {
                map.put(ResultListener.ACTION, ACTION_ADD_ISSUE);
            }
            else
            {
                map.put(ResultListener.ACTION, ACTION_UPDATE_ISSUE);
            }

            map.put(IssuesDao.NO_OF_IMAGES, "1");
            map.put(IssuesDao.USER_ID, mIssue.userId);
            map.put(IssuesDao.USER_DP_URL, mIssue.userDpUrl);
            map.put(IssuesDao.USERNAME, mIssue.username);
            map.put(IssuesDao.DESCRIPTION, mIssue.description);
            map.put(IssuesDao.AREA_TYPE, mIssue.areaType);
            map.put(IssuesDao.RADIUS, mIssue.radius + "");
            map.put(IssuesDao.LATITUDE, mIssue.latitude);
            map.put(IssuesDao.LONGITUDE, mIssue.longitude);
            map.put(IssuesDao.ISSUE_ID, mIssue.issueId);
            VolleyUtil.sendPostData(map, AddIssueService.this);
            return null;
        }
    }

    private String getStringImage(Bitmap bmp)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }


    @Override
    public void onSuccess(String response)
    {
        Log.d("AddIssueService", response);
        if (response.trim().equalsIgnoreCase(ERROR))
        {
           onError();
        }
        else
        {
            Intent openIntent = new Intent(mContext, IssueActivity.class);
            mIssue.issueId = response;
            IssuesDao.delete(mIssue.issueId);
            startService(new Intent(this, NewsFeedsUpdateService.class));
            Otto.post(ISSUES_FEEDS_UPDATED);
            openIntent.putExtra(AddIssueActivity.ISSUE_DATA, mIssue);

            PendingIntent pIntent = PendingIntent.getActivity(mContext, (int) System.currentTimeMillis(), openIntent, 0);
            mBuilder = new NotificationCompat.Builder(mContext);
            mBuilder.setOngoing(false)
                    .setSmallIcon(R.mipmap.bullhorn)
                    .setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.issuePosted))
                    .setContentText(getText(R.string.clickNotificationToOpen));
        }
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        AddIssueService.this.stopSelf();
    }

    @Override
    public void onError(VolleyError error)
    {
        onError();
    }

    private void onError()
    {
        mBuilder.setOngoing(false)
                .setSmallIcon(R.mipmap.bullhorn)
                .setContentTitle(getString(R.string.postingIssueFailed))
                .setContentText(getString(R.string.clickOnRetryToPostAgain))
                .setProgress(0, 0, false)
                .setAutoCancel(true);
    }
}
