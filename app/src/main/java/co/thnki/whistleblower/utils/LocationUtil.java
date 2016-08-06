package co.thnki.whistleblower.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import static co.thnki.whistleblower.utils.MarkerAndCirclesUtil.getResizedBitmap;

public class LocationUtil
{
    public static LatLng getLatLng(String lat, String lng)
    {
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
    }

    public static BitmapDescriptor getMapMarker(Context context, int resourceId, double size)
    {
        Resources resources = context.getResources();

        Bitmap original = BitmapFactory.decodeResource(resources, resourceId);

        DisplayMetrics metrics = resources.getDisplayMetrics();

        int pixels = (int) (metrics.density * size);
        Bitmap resized = getResizedBitmap(original, pixels, pixels);

        return BitmapDescriptorFactory.fromBitmap(resized);
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2)
    {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (earthRadius * c);
    }

    public static double distFrom(String lat1, String lng1, String lat2, String lng2)
    {
        return distFrom(getLatLng(lat1, lng1), getLatLng(lat2, lng2));
    }

    public static double distFrom(LatLng latLng1, LatLng latLng2)
    {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(latLng2.latitude - latLng1.latitude);
        double dLng = Math.toRadians(latLng2.longitude - latLng1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latLng1.latitude)) * Math.cos(Math.toRadians(latLng2.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (earthRadius * c);
    }

    public static String getAddressLines(String address, int noOfLines)
    {
        String[] addressLines = address.split(",");
        String msg = "";
        int len = addressLines.length;
        for (int i = 0; i < len; i++)
        {
            if (i < (noOfLines - 1))
            {
                msg += addressLines[i] + ", ";
            }
        }
        return msg.substring(0, msg.length() - 2);
    }
}
