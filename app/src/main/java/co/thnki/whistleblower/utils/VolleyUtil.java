package co.thnki.whistleblower.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.services.VolleyService;
import co.thnki.whistleblower.singletons.VolleySingleton;


public class VolleyUtil
{
    public static final String IMAGE_URL = "http://uploads-thnkin.rhcloud.com/uploads/";
    private static final int MY_SOCKET_TIMEOUT_MS = 10000;

    public static void sendPostData(final Map<String, String> postData, final ResultListener<String> listener)
    {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ResultListener.URL,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        if(listener != null)
                        {
                            listener.onSuccess(response);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        if(listener != null)
                        {
                            listener.onError(error);
                        }
                    }
                })
        {
            @Override
            protected Map<String, String> getParams()
            {
                return postData;
            }
        };
        RequestQueue requestQueue = VolleySingleton.getInstance().getRequestQueue();
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);
    }

    public static void sendPostData(Map<String, String> postData, int requestCode)
    {
        sendPostData(convertMapToBundle(postData), requestCode);
    }

    public static void sendPostData(Bundle bundle, int requestCode)
    {
        Context appContext = WhistleBlower.getAppContext();
        Intent intent = new Intent(appContext, VolleyService.class);
        intent.putExtra(VolleyService.VOLLEY_BUNDLE, bundle );
        intent.putExtra(VolleyService.VOLLEY_REQUEST_CODE, requestCode);
        appContext.startService(intent);
    }

    public static void sendPostData(Bundle postData, final ResultListener<String> listener)
    {
        Log.d("VolleyService", "sendPostData");
        final Map<String, String> mapData = convertBundleToMap(postData);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ResultListener.URL,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        Log.d("VolleyService", "Volley response");
                        if(listener != null)
                        {
                            listener.onSuccess(response);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.d("VolleyService", "Volley response");
                        if(listener != null)
                        {
                            listener.onError(error);
                        }
                    }
                })
        {
            @Override
            protected Map<String, String> getParams()
            {
                return mapData;
            }
        };
        RequestQueue requestQueue = VolleySingleton.getInstance().getRequestQueue();
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);
    }

    private static Map<String, String> convertBundleToMap(Bundle postData)
    {
        Map<String, String> map = new HashMap<>();
        for (String key : postData.keySet())
        {
            map.put(key, postData.getString(key));
        }
        return map;
    }

    private static Bundle convertMapToBundle(Map<String, String> postData)
    {
        Bundle bundle = new Bundle();
        for (String key : postData.keySet())
        {
            bundle.putString(key, postData.get(key));
        }
        return bundle;
    }

    public static void sendGetData(final Map<String, String> getData, final ResultListener<String> listener)
    {
        String customURL = ResultListener.URL + "?";
        for (String key : getData.keySet())
        {
            customURL += key + "=" + getData.get(key) + "&";
        }
        customURL = customURL.substring(0, customURL.length() - 1);
        Log.d("sendGetData", customURL);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, customURL,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        if(listener != null)
                        {
                            listener.onSuccess(response);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        if(listener != null)
                        {
                            listener.onError(error);
                        }
                    }
                });

        RequestQueue requestQueue = VolleySingleton.getInstance().getRequestQueue();
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    public static final String KEY_ACTION = "action";
    public static final String KEY_PARTIAL_STR ="str";
    public static final String KEY_OFFSET ="offset";
    public static final String KEY_LIMIT ="limit";
}
