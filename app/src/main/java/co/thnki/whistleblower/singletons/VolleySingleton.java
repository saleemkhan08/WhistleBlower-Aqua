package co.thnki.whistleblower.singletons;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import co.thnki.whistleblower.WhistleBlower;


public class VolleySingleton
{
    private static VolleySingleton sInstance = null;
    private RequestQueue mRequestQueue;
    private VolleySingleton()
    {
        mRequestQueue = Volley.newRequestQueue(WhistleBlower.getAppContext());
    }
    public static VolleySingleton getInstance()
    {
        if(sInstance == null)
        {
            sInstance = new VolleySingleton();
        }
        return sInstance;
    }

    public RequestQueue getRequestQueue()
    {
        return mRequestQueue;
    }
}
