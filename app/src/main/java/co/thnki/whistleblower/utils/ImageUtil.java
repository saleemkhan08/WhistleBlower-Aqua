package co.thnki.whistleblower.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import co.thnki.whistleblower.AddIssueActivity;
import co.thnki.whistleblower.Crop;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.interfaces.PermissionResultListener;

public class ImageUtil
{
    public ImageLoader mImageLoader;
    Context mContext;
    DisplayImageOptions dpOptions, issueOptions;

    public ImageUtil(Context context)
    {
        mContext = context;
        mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(ImageLoaderConfiguration.createDefault(mContext));
        issueOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.mipmap.loading)
                .showImageForEmptyUri(R.mipmap.loading)
                .showImageOnFail(R.mipmap.loading)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        dpOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.mipmap.user_primary_dark_o)
                .showImageForEmptyUri(R.mipmap.user_primary_dark_o)
                .showImageOnFail(R.mipmap.user_primary_dark_o)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .displayer(new RoundedBitmapDisplayer(100))
                .build();
    }

    public void displayImage(String photo_url, ImageView view, boolean isRounded)
    {
        if(isRounded)
        {
            mImageLoader.displayImage(photo_url, view, dpOptions);
        }
        else
        {
            mImageLoader.displayImage(photo_url, view, issueOptions);
        }
    }

    public void displayImage(Context context, String photo_url, ImageView view, boolean isRounded)
    {
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(context));
        DisplayImageOptions imageOptions;
        if(isRounded)
        {
            imageOptions = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.mipmap.user_primary_dark_o)
                    .showImageForEmptyUri(R.mipmap.user_primary_dark_o)
                    .showImageOnFail(R.mipmap.user_primary_dark_o)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .displayer(new RoundedBitmapDisplayer(100))
                    .build();
        }
        else
        {
            imageOptions = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.mipmap.loading)
                    .showImageForEmptyUri(R.mipmap.loading)
                    .showImageOnFail(R.mipmap.loading)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
        }
        imageLoader.displayImage(photo_url, view, imageOptions);
    }

    public static int pixels(Context mContext, double dp)
    {
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        double pixels = metrics.density * dp;
        return (int) pixels;
    }

    public static int dp(Context mContext, double pixels)
    {
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        double dp = pixels/metrics.density;
        return (int) dp;
    }

    public static int getAdWidth(AppCompatActivity context)
    {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthInPix = metrics.widthPixels;
        int width = dp(context, widthInPix);
        if(width < 1200)
        {
            return width;
        }
        return 1200;
    }
    public static final int LOAD_GALLERY_REQUEST = 303;
    AppCompatActivity mActivity;
    public static Uri imageUri;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int CAPTURE_IMAGE_REQUEST = 301;
    public static final int RECORD_VIDEO_REQUEST = 302;
    public static final String IS_PHOTO = "IS_PHOTO";
    public static final String IMAGE_DIRECTORY_NAME = "WhistleBlower";
    public static File mediaStorageDir;

    public  File getMediaStorageDir()
    {
        mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists())
        {
            if (mediaStorageDir.mkdirs())
            {
                mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            }
        }
        return mediaStorageDir;
    }

    public  void pickImage()
    {

    }

    public  void getImage(final AppCompatActivity activity, final boolean isFromGallery)
    {
        if (!PermissionUtil.isCameraAndStoragePermissionsAvailable())
        {
            PermissionUtil.requestPermission(PermissionUtil.SDCARD_PERMISSION, new PermissionResultListener()
            {
                @Override
                public void onGranted()
                {
                    getImageFromDevice(activity, isFromGallery);
                }

                @Override
                public void onDenied()
                {
                    WhistleBlower.toast("Image can't be added without this Permission");
                }
            });
        }
        else
        {
           getImageFromDevice(activity, isFromGallery);
        }
    }

    private  void getImageFromDevice(AppCompatActivity activity, boolean isFromGallery)
    {
        if(isFromGallery)
        {
            Crop.pickImage(activity, AddIssueActivity.REQUEST_CODE_IMAGE_PICKER);
        }
        else
        {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getOutputMediaFileUri(MEDIA_TYPE_IMAGE));
            activity.startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
        }
    }
    private  Uri getOutputMediaFileUri(int type)
    {
        imageUri = Uri.fromFile(getOutputMediaFile(type));
        return imageUri;
    }

    private  File getOutputMediaFile(int type)
    {
        // Create a media file name
        if(mediaStorageDir == null)
        {
            mediaStorageDir = getMediaStorageDir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        }
        else if (type == MEDIA_TYPE_VIDEO)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        }
        else
        {
            return null;
        }
        return mediaFile;
    }

    public  void launchIssueEditor(AppCompatActivity mActivity, boolean isPhoto)
    {
        Intent intent = new Intent(mActivity, AddIssueActivity.class);
        intent.putExtra(IS_PHOTO, isPhoto);
        mActivity.startActivity(intent);
    }

}
