package co.thnki.whistleblower.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.VolleyError;

import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.VolleyResponse;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.VolleyUtil;

public class VolleyService extends Service implements ResultListener<String>
{
    public static final String VOLLEY_BUNDLE = "volleyBundle";
    public static final String VOLLEY_REQUEST_CODE = "volleyRequestCode";

    private int mRequestCode;
    public VolleyService()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("VolleyService", "onStartCommand");
        mRequestCode = intent.getIntExtra(VOLLEY_REQUEST_CODE,0);
        VolleyUtil.sendPostData(intent.getBundleExtra(VOLLEY_BUNDLE), this);
        return START_NOT_STICKY;
    }

    @Override
    public void onSuccess(String result)
    {
        Log.d("VolleyService", "onSuccess : "+result);
        Otto.post(new VolleyResponse(result, mRequestCode, true));
        stopSelf();
    }

    @Override
    public void onError(VolleyError error)
    {
        Log.d("VolleyService", "onError : "+error.getMessage());
        Otto.post(new VolleyResponse(error.getMessage(), mRequestCode, false));
        stopSelf();
    }
}
