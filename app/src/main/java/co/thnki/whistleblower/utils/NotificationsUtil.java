package co.thnki.whistleblower.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.net.URL;

import co.thnki.whistleblower.MainActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.pojos.NotificationData;
import co.thnki.whistleblower.receivers.NotificationActionReceiver;


public class NotificationsUtil
{
    private static final String TAG = "NotificationsUtil";
    private static Context mAppContext;
    private static NotificationManager mNotificationManager;

    static
    {
        mAppContext = WhistleBlower.getAppContext();
        mNotificationManager = (NotificationManager) mAppContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    public static void showAlarmNotification(String name, int noOfAlarms)
    {
        NotificationData data = new NotificationData();

        data.action1IntentText = (noOfAlarms > 1) ? "Turn Off All Alarms" : "Turn Off Alarm";
        data.action1IntentIcon = R.mipmap.bell_slash_grey;
        data.action1IntentTag = NotificationActionReceiver.CANCEL_ALL_ALARMS;

        data.contentIntentTag = MainActivity.TAG;
        data.contentText = name;
        data.contentTitle = "Location Alarm";
        data.onGoing = true;
        data.notificationId = NotificationActionReceiver.NOTIFICATION_ID_LOCATION_ALARMS;
        showNotification(data);
    }


    public static void showNotification(NotificationData data)
    {
        if (data.largeIconUrl == null)
        {
            showNotification(data, null);
        }
        else
        {
            new ShowNormalNotification().execute(data);
        }
    }

    public static void showNotification(NotificationData data, Bitmap mLargeIcon)
    {
        Intent contentIntent = new Intent(mAppContext, NotificationActionReceiver.class);

        contentIntent.putExtra(NotificationActionReceiver.NOTIFICATION_ACTION, data.contentIntentTag);
        PendingIntent contentPendingIntent = PendingIntent.getBroadcast(mAppContext, (int) System.currentTimeMillis(), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mAppContext);
        mBuilder.setContentTitle(data.contentTitle)
                .setSmallIcon(R.mipmap.app_icon_white)
                .setOngoing(data.onGoing)
                .setAutoCancel(true)
                .setContentText(data.contentText)
                .setContentIntent(contentPendingIntent);

        if (data.action1IntentTag != null)
        {
            Intent actionIntent = new Intent(mAppContext, NotificationActionReceiver.class);
            actionIntent.putExtra(NotificationActionReceiver.NOTIFICATION_ACTION, data.action1IntentTag);
            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(mAppContext, (int) System.currentTimeMillis(), actionIntent, 0);

            mBuilder.addAction(data.action1IntentIcon, data.action1IntentText, actionPendingIntent);
        }

        if (data.action2IntentTag != null)
        {
            Intent actionIntent = new Intent(mAppContext, NotificationActionReceiver.class);
            actionIntent.putExtra(NotificationActionReceiver.NOTIFICATION_ACTION, data.action2IntentTag);
            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(mAppContext, (int) System.currentTimeMillis(), actionIntent, 0);

            mBuilder.addAction(data.action2IntentIcon, data.action2IntentText, actionPendingIntent);
        }

        if (mLargeIcon != null)
        {
            mBuilder.setLargeIcon(mLargeIcon);
        }

        if(data.vibrate)
        {
            Notification notificationDefault = new Notification();
            notificationDefault.defaults |= Notification.DEFAULT_LIGHTS; // LED
            notificationDefault.defaults |= Notification.DEFAULT_VIBRATE; //Vibration
            notificationDefault.defaults |= Notification.DEFAULT_SOUND; // Sound
            mBuilder.setDefaults(notificationDefault.defaults);
        }
        mNotificationManager.notify(data.notificationId, mBuilder.build());
    }

    private static Bitmap getCircleBitmapFromUrl(String photoUrl)
    {
        try
        {
            URL url = new URL(photoUrl);
            return getCircleBitmap(BitmapFactory.decodeStream(url.openConnection().getInputStream()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.d(TAG, " Error : " + e.getMessage());
        }
        return BitmapFactory.decodeResource(mAppContext.getResources(), R.mipmap.app_icon);
    }

    private static class ShowNormalNotification extends AsyncTask<NotificationData, Void, Void>
    {
        NotificationData mNotificationData;
        Bitmap mLargeIcon;

        @Override
        protected Void doInBackground(NotificationData... params)
        {
            mNotificationData = params[0];
            mLargeIcon = getCircleBitmapFromUrl(mNotificationData.largeIconUrl);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            showNotification(mNotificationData, mLargeIcon);
        }
    }

    private static Bitmap getCircleBitmap(Bitmap bitmap)
    {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();
        return output;
    }

    public static void removeNotification(int id)
    {
        mNotificationManager.cancel(id);
    }
}