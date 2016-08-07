package co.thnki.whistleblower.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.thnki.whistleblower.MainActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.pojos.MarkerAndCircle;
import co.thnki.whistleblower.singletons.Otto;

public class MarkerAndCirclesUtil
{
    private static final int TYPE_ISSUE = -1;
    private GoogleMap mGoogleMap;
    private Map<String, MarkerAndCircle> mMarkerAndCircleMap;
    private Map<String, Circle> circleMap;
    private Map<String, Marker> imgMarkerMap;
    private Map<String, Marker> bgMarkerMap;
    private int mAccentColor, mRadiusColor;

    public MarkerAndCirclesUtil(GoogleMap googleMap, int accentColor, int radiusColor)
    {
        this.mGoogleMap = googleMap;
        Otto.register(this);
        mAccentColor = accentColor;
        mRadiusColor = radiusColor;

        mMarkerAndCircleMap = new HashMap<>();
        circleMap = new HashMap<>();
        imgMarkerMap = new HashMap<>();
        bgMarkerMap = new HashMap<>();

        mGoogleMap.clear();
        new MarkerAndCircleAddingTask().execute();

        Log.d(MainActivity.TAG, "new - MarkerAndCirclesUtil");
    }

    private class MarkerAndCircleAddingTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            getIssueMarkers();
            
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            for (String key : mMarkerAndCircleMap.keySet())
            {
                MarkerAndCircle markerAndCircle = mMarkerAndCircleMap.get(key);
                addMarkerAndCircle(markerAndCircle);
            }
        }
    }

    private void getIssueMarkers()
    {
        List<Issue> issues = IssuesDao.getList();
        for(Issue issue: issues)
        {
           mMarkerAndCircleMap.put(issue.issueId, getMarkerAndCircle(issue));
        }
    }

    private void addMarkerAndCircle(MarkerAndCircle markerAndCircle)
    {
        if (circleMap.containsKey(markerAndCircle.id))
        {
            removeMarkerAndCircle(markerAndCircle.id);
        }

        circleMap.put(markerAndCircle.id, mGoogleMap.addCircle(new CircleOptions()
                .radius(markerAndCircle.radius)
                .strokeWidth(2)
                .strokeColor(mAccentColor)
                .fillColor(mRadiusColor)
                .center(markerAndCircle.latLng)));

        bgMarkerMap.put(markerAndCircle.id, mGoogleMap.addMarker(new MarkerOptions()
                .position(markerAndCircle.latLng)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .icon(getMapMarker(WhistleBlower.getAppContext(), R.mipmap.marker_bg, 40))));

        imgMarkerMap.put(markerAndCircle.id, mGoogleMap.addMarker(new MarkerOptions()
                .position(markerAndCircle.latLng)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .icon(getMapMarker(WhistleBlower.getAppContext(), getDrawableResId(markerAndCircle.type), 20))));

        mMarkerAndCircleMap.put(markerAndCircle.id, markerAndCircle);
    }

    private static BitmapDescriptor getMapMarker(Context context, int resourceId, double size)
    {
        Resources resources = context.getResources();

        Bitmap original = BitmapFactory.decodeResource(resources, resourceId);

        DisplayMetrics metrics = resources.getDisplayMetrics();

        int pixels = (int) (metrics.density * size);
        Bitmap resized = getResizedBitmap(original, pixels, pixels);

        return BitmapDescriptorFactory.fromBitmap(resized);
    }

    static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private void removeMarkerAndCircle(String id)
    {
        if (circleMap.containsKey(id))
        {
            circleMap.get(id).remove();
            circleMap.remove(id);
        }

        if (bgMarkerMap.containsKey(id))
        {
            bgMarkerMap.get(id).remove();
            bgMarkerMap.remove(id);
        }

        if (imgMarkerMap.containsKey(id))
        {
            imgMarkerMap.get(id).remove();
            imgMarkerMap.remove(id);
        }
        mMarkerAndCircleMap.remove(id);
    }

private MarkerAndCircle getMarkerAndCircle(Issue issue)
    {
        MarkerAndCircle markerAndCircle = new MarkerAndCircle();
        markerAndCircle.id = issue.issueId;
        markerAndCircle.latLng = LocationUtil.getLatLng(issue.latitude, issue.longitude);
        markerAndCircle.radius = issue.radius;
        markerAndCircle.type = TYPE_ISSUE;
        return markerAndCircle;
    }
	
    public void unregister()
    {
        Otto.unregister(this);
    }

public void addMarkerAndCircle(Issue issue)
    {
        addMarkerAndCircle(getMarkerAndCircle(issue));
    }
	
    private static int getDrawableResId(int index)
    {
        switch (index)
        {
            case TYPE_ISSUE:
                return R.mipmap.issue_primary_dark;
            default:
                return R.mipmap.map_pin;
        }
    }

    private static LatLng getLatLng(String lat, String lng)
    {
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
    }
}
