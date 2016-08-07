package co.thnki.whistleblower;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.MapFragment;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.services.AddIssueService;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.DialogsUtil;
import co.thnki.whistleblower.utils.ImageUtil;

import static co.thnki.whistleblower.R.id.areaTypeName;
import static co.thnki.whistleblower.R.id.editIcon;
import static co.thnki.whistleblower.doas.IssuesDao.ANONYMOUS;

public class AddIssueActivity extends AppCompatActivity
{
    public static final String ISSUE_DATA = "issueData";
    private static final int REQUIRED_WIDTH = 640;
    public static final int REQUEST_CODE_IMAGE_PICKER = 1020;
    private String mImageUri;
    private SharedPreferences mPreferences;
    private DialogsUtil mDialogsUtil;

    @Bind(R.id.profilePic)
    ImageView mProfilePic;

    @Bind(R.id.username)
    TextView mUsername;

    private String mAddress;
    private LatLng mLatLng;

    @Bind(areaTypeName)
    EditText mAreaTypeNameEditText;

    @Bind(R.id.issueDescription)
    EditText mDescription;

    @Bind(R.id.issueImage)
    ImageView mImgPreview;

    @Bind(editIcon)
    ImageView mEditIcon;
    private int mRadius;
    private String mAnonymousString;
    private ImageUtil mImageUtil;
    private Issue mIssue;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_issue);
        ButterKnife.bind(this);
        Otto.register(this);
        mDialogsUtil = new DialogsUtil(this);
        mPreferences = WhistleBlower.getPreferences();
        Intent intent = getIntent();
        if(intent.hasExtra(ISSUE_DATA))
        {
            mIssue = intent.getParcelableExtra(ISSUE_DATA);
        }
        else
        {
            mAddress = intent.getStringExtra(MapFragment.ADDRESS);
            mLatLng = intent.getParcelableExtra(MapFragment.LATLNG);
            mRadius = intent.getIntExtra(MapFragment.RADIUS, 100);
        }
        mImageUtil = new ImageUtil(this);
        mAreaTypeNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    mEditIcon.setVisibility(View.GONE);
                }
                else
                {
                    mEditIcon.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        showAreaNameAndTypeDetails();
        showUserNameAndPic();
        showDescription();
        showIssuePic();
    }

    private void showIssuePic()
    {
        if(mIssue !=null)
        {
            mImageUtil.displayImage(mIssue.imgUrl,mImgPreview,false);
        }
    }
    private void showDescription()
    {
        if(mIssue != null)
        {
            if(!mIssue.description.trim().isEmpty())
            {
                mDescription.setText(mIssue.description);
            }
            else
            {
                mDescription.setText("");
            }
        }
    }

    private boolean isMyServiceRunning()
    {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            String LocationUpdateServiceName = getPackageName() + "LocationDetailsService";

            if (LocationUpdateServiceName.equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    public void updateUi(String action)
    {
        switch (action)
        {
            case IssuesDao.ANONYMOUS :
                showUserNameAndPic();
                break;
            case DialogsUtil.ISSUE_TYPE :
                showAreaNameAndTypeDetails();
                break;
        }
    }

    private void showUserNameAndPic()
    {
        mAnonymousString = getString(R.string.anonymous);
        Drawable mAnonymousDrawable = ContextCompat.getDrawable(this, R.mipmap.user_primary_dark_o);

        if(mIssue != null)
        {
            if (mIssue.anonymous)
            {
                mProfilePic.setImageDrawable(mAnonymousDrawable);
                mUsername.setText(mAnonymousString);
            }
            else
            {
                String dpUrl = mPreferences.getString(Accounts.PHOTO_URL, "");
                String userName = mPreferences.getString(Accounts.NAME, mAnonymousString);
                mUsername.setText(userName);
                if (dpUrl.trim().isEmpty())
                {
                    mProfilePic.setBackground(mAnonymousDrawable);
                }
                else
                {
                    mImageUtil.displayImage(this, dpUrl, mProfilePic, true);
                }
            }
        }
        else
        {
            if (mPreferences.getBoolean(IssuesDao.ANONYMOUS, false))
            {
                mProfilePic.setImageDrawable(mAnonymousDrawable);
                mUsername.setText(mAnonymousString);
            }
            else
            {
                String dpUrl = mPreferences.getString(Accounts.PHOTO_URL, "");
                String userName = mPreferences.getString(Accounts.NAME, mAnonymousString);
                mUsername.setText(userName);
                if (dpUrl.trim().isEmpty())
                {
                    mProfilePic.setBackground(mAnonymousDrawable);
                }
                else
                {
                    mImageUtil.displayImage(this, dpUrl, mProfilePic, true);
                }
            }
        }
    }

    private void showAreaNameAndTypeDetails()
    {
        if(mIssue != null)
        {
            mAreaTypeNameEditText.setText(mIssue.areaType);
        }
        else
        {
            String placeTypeNameText = "@" + mAddress;
            String temp = mDialogsUtil.getSelectedIssueType();
            placeTypeNameText += ",\n" + temp;
            mAreaTypeNameEditText.setText(placeTypeNameText);
        }
    }

    @OnClick({R.id.zone,R.id.anonymous,R.id.camera,R.id.gallery,R.id.postIssue})
    public void onClick(View v)
    {
        int id = v.getId();
        switch (id)
        {
            case R.id.zone:
                mDialogsUtil.showIssueTypeDialog();
                break;
            case R.id.anonymous:
                mDialogsUtil.showAnonymousDialog();
                break;
            case R.id.camera:
                mImageUtil.getImage(this, false);
                break;
            case R.id.gallery:
                mImageUtil.getImage(this,true);
                break;
            case R.id.editIcon:
                mAreaTypeNameEditText.requestFocus();
                break;
            case R.id.postIssue:
                addIssue();
                break;
        }
    }

    private void addIssue()
    {
        if (mImageUri != null && (mImageUri.contains(".png")||mImageUri.contains(".jpg")))
        {
            Intent intent = new Intent(this, AddIssueService.class);
            Issue issue = new Issue();
            issue.anonymous = mPreferences.getBoolean(ANONYMOUS, false);
            issue.areaType = mAreaTypeNameEditText.getText().toString();
            issue.description = mDescription.getText().toString();
            issue.imgUrl = mImageUri;
            issue.latitude = mLatLng.latitude + "";
            issue.longitude = mLatLng.longitude + "";
            issue.radius = mRadius;
            issue.userDpUrl = mPreferences.getString(Accounts.PHOTO_URL, "");
            issue.username = mPreferences.getString(Accounts.NAME, mAnonymousString);
            issue.userId = mPreferences.getString(Accounts.GOOGLE_ID, "");
            if (issue.anonymous)
            {
                issue.userDpUrl = "";
                issue.username = mAnonymousString;
            }

            intent.putExtra(ISSUE_DATA, issue);
            startService(intent);
            intent = new Intent(AddIssueActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        else
        {
            Toast.makeText(this, "Please Upload An Image!", Toast.LENGTH_SHORT).show();
        }
    }

    private void beginCrop(Uri source)
    {
        Uri destination = Uri.fromFile(new File(mImageUtil.getMediaStorageDir(), "temp.png"));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result)
    {
        if (resultCode == RESULT_OK)
        {
            Uri imgUri = Crop.getOutput(result);
            mImgPreview.setImageURI(imgUri);
            mImageUri = imgUri.getPath();
            Toast.makeText(this, mImageUri, Toast.LENGTH_SHORT).show();
        }
        else if (resultCode == Crop.RESULT_ERROR)
        {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_IMAGE_PICKER:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        beginCrop(data.getData());
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    default:
                        Toast.makeText(this, "Please Try Again!", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            case Crop.REQUEST_CROP:
                handleCrop(resultCode, data);
                break;
            case ImageUtil.CAPTURE_IMAGE_REQUEST:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        Crop.of(ImageUtil.imageUri, ImageUtil.imageUri).asSquare().start(this);
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
            case ImageUtil.LOAD_GALLERY_REQUEST:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        mImageUri = copy(cursor.getString(columnIndex));
                        Bitmap mImage = getCompressedImageFile(mImageUri);
                        if (mImage == null)
                        {
                            Toast.makeText(this, "Please Try Again!", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            mImgPreview.setImageBitmap(mImage);
                        }
                        cursor.close();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    private String copy(String src)
    {
        // External sdcard location
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                ImageUtil.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists())
        {
            if (mediaStorageDir.mkdirs())
            {
                mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            }
        }

        String dest = mediaStorageDir + "/temp.png";
        Log.d("Img Size", "Dest : " + dest);
        Log.d("Img Size", "Src : " + src);

        try
        {
            File srcFile = new File(src);
            File destFile = new File(dest);
            Log.d("Img Size", "Try");
            if (!destFile.exists())
            {
                Log.d("Img Size", "file does not exist");
                if (!destFile.createNewFile())
                {
                    Log.d("Img Size", "Couldn't Create File");
                    return null;
                }
                Log.d("Img Size", "File Created");
            }
            Log.d("Img Size", "file exist");
            InputStream inputStream = new FileInputStream(srcFile);
            OutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        }
        catch (Exception e)
        {
            Log.d("Img Size", e.getMessage());
            e.printStackTrace();
            return null;
        }
        return dest;
    }

    public Bitmap getCompressedImageFile(final String path)
    {
        if (path == null)
        {
            return null;
        }

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        Log.d("Img Size", "Before : " + options.outWidth + "x" + options.outHeight);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, REQUIRED_WIDTH);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        final Bitmap compressedBitmap = BitmapFactory.decodeFile(path, options);

        Log.d("Img Size", "After : " + options.outWidth + "x" + options.outHeight);

        new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    File file = new File(path);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    compressedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                    outputStream.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }.run();
        return compressedBitmap;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth)
    {
        final int width = options.outWidth;
        float inSampleSize = 1;
        if (width > reqWidth)
        {
            inSampleSize = (int) Math.ceil((float) width / (float) reqWidth) + 1;
        }
        Log.d("Img Size", inSampleSize + "");
        return (int) inSampleSize;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Otto.unregister(this);
    }
}
