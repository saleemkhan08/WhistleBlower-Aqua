package co.thnki.whistleblower;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.MapFragment;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.services.AddIssueService;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;
import co.thnki.whistleblower.utils.DialogsUtil;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.LocationUtil;

import static co.thnki.whistleblower.MainActivity.ADD_TEMP_MARKER;
import static co.thnki.whistleblower.R.id.areaTypeName;
import static co.thnki.whistleblower.R.id.editIcon;
import static co.thnki.whistleblower.WhistleBlower.toast;
import static co.thnki.whistleblower.doas.IssuesDao.ANONYMOUS;
import static co.thnki.whistleblower.utils.DialogsUtil.ISSUE_TYPE;

public class AddIssueActivity extends AppCompatActivity
{
    public static final String ISSUE_DATA = "issueData";
    private static final int REQUIRED_WIDTH = 640;
    public static final int REQUEST_CODE_IMAGE_PICKER = 1020;
    private static final int IMAGE_ERROR = 1;
    private static final int AREA_TYPE_ERROR = 2;
    private static final int ISSUE_TYPE_ERROR = 3;
    private static final int NO_ERROR = 0;
    private static final int AREA_TYPE_AND_ISSUE_TYPE_ERROR = 4;
    public static final String NEW_ISSUE = "newIssue";
    private String mImageUri;
    private SharedPreferences mPreferences;
    private DialogsUtil mDialogsUtil;

    @Bind(R.id.profilePic)
    ImageView mProfilePic;

    @Bind(R.id.username)
    TextView mUsername;

    @Bind(R.id.issueImageContainer)
    public RelativeLayout issueImageContainer;

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
    private static int temp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21)
        {
            getWindow().setSharedElementEnterTransition(TransitionInflater
                    .from(this).inflateTransition(R.transition.shared_element_transition));
        }
        setContentView(R.layout.activity_add_issue);
        ButterKnife.bind(this);
        Otto.register(this);
        mDialogsUtil = new DialogsUtil(this);
        mImageUtil = new ImageUtil(this);
        mPreferences = WhistleBlower.getPreferences();
        Intent intent = getIntent();
        mAnonymousString = getString(R.string.anonymous);
        if (intent.hasExtra(ISSUE_DATA))
        {
            mIssue = intent.getParcelableExtra(ISSUE_DATA);
            updateFromIssueObject();
        }
        else
        {
            mAddress = intent.getStringExtra(MapFragment.ADDRESS);
            mLatLng = intent.getParcelableExtra(MapFragment.LATLNG);
            mRadius = intent.getIntExtra(MapFragment.RADIUS, 100);
        }
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
        if (mIssue == null)
        {
            showAreaNameAndTypeDetails();
            showUserNameAndPic();
        }
    }

    private void updateFromIssueObject()
    {
        if (mIssue != null)
        {
            mLatLng = LocationUtil.getLatLng(mIssue.latitude, mIssue.longitude);
            mRadius = mIssue.radius;

            mAreaTypeNameEditText.setText(mIssue.areaType);
            mAddress = getAddress(mIssue.areaType);
            mPreferences.edit().putInt(ISSUE_TYPE, findIssueType(mIssue.areaType)).apply();

            if (mIssue.anonymous)
            {
                mProfilePic.setImageResource(R.mipmap.user_icon_accent);
                mUsername.setText(mAnonymousString);
            }
            else
            {
                String dpUrl = mPreferences.getString(Accounts.PHOTO_URL, "");
                String userName = mPreferences.getString(Accounts.NAME, mAnonymousString);
                mUsername.setText(userName);
                if (dpUrl.trim().isEmpty())
                {
                    mProfilePic.setImageResource(R.mipmap.user_icon_accent);
                }
                else
                {
                    mImageUtil.displayRoundedImage(dpUrl, mProfilePic);
                }
            }
            mImageUri = mIssue.imgUrl;
            mImageUtil.displayImage(mIssue.imgUrl, mImgPreview);
            if (!mIssue.description.trim().isEmpty())
            {
                mDescription.setText(mIssue.description);
            }
            else
            {
                mDescription.setText("");
            }
        }
    }

    private String getAddress(String mAreaTypeNameText)
    {
        return mAreaTypeNameText.substring(1, mAreaTypeNameText.indexOf("#") - 2);
    }

    private int findIssueType(String str)
    {
        Pattern MY_PATTERN = Pattern.compile("#(\\S+)");
        Matcher mat = MY_PATTERN.matcher(str);
        while (mat.find())
        {
            switch (mat.group(1))
            {
                case "Humanity":
                    return 0;
                case "AnimalCare":
                    return 1;
                case "Environmental":
                    return 2;
            }
        }
        return -1;
    }


    @Subscribe
    public void updateUi(String action)
    {
        switch (action)
        {
            case IssuesDao.ANONYMOUS:
                showUserNameAndPic();
                break;
            case ISSUE_TYPE:
                showAreaNameAndTypeDetails();
                break;
        }
    }

    private void showUserNameAndPic()
    {
        if (mPreferences.getBoolean(IssuesDao.ANONYMOUS, false))
        {
            mProfilePic.setImageResource(R.mipmap.user_icon_accent);
            mUsername.setText(mAnonymousString);
        }
        else
        {
            String dpUrl = mPreferences.getString(Accounts.PHOTO_URL, "");
            String userName = mPreferences.getString(Accounts.NAME, mAnonymousString);
            mUsername.setText(userName);
            if (dpUrl.trim().isEmpty())
            {
                mProfilePic.setImageResource(R.mipmap.user_icon_accent);
            }
            else
            {
                mImageUtil.displayRoundedImage(dpUrl, mProfilePic);
            }
        }
    }

    private void showAreaNameAndTypeDetails()
    {
        String placeTypeNameText = "@" + mAddress;
        String temp = mDialogsUtil.getSelectedIssueType();
        placeTypeNameText += ",\n" + temp;
        mAreaTypeNameEditText.setText(placeTypeNameText);

    }

    @OnClick({R.id.zone, R.id.anonymous, R.id.camera, R.id.gallery})
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
                mImageUtil.getImage(this, true);
                break;
            case R.id.editIcon:
                mAreaTypeNameEditText.requestFocus();
                break;
        }
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    @OnClick({R.id.postIssue, R.id.postIssueTop})
    public void addIssue()
    {
        switch (isIssueCategoryCheck())
        {
            case IMAGE_ERROR:
                toast(getString(R.string.pleaseUploadImage));
                break;
            case AREA_TYPE_ERROR:
                toast(getString(R.string.pleaseEnterAddress));
                break;
            case ISSUE_TYPE_ERROR:
                toast(getString(R.string.pleaseEnterIssueType));
                break;
            default:
                if (ConnectivityUtil.isConnected())
                {
                    submitIssue();
                }
                else
                {
                    toast(getString(R.string.noInternet));
                }
                break;
        }
    }

    private void submitIssue()
    {
        Intent intent = new Intent(this, AddIssueService.class);
        if (mIssue == null)
        {
            mIssue = new Issue();
            mIssue.issueId = NEW_ISSUE;
        }
        mIssue.anonymous = mPreferences.getBoolean(ANONYMOUS, false);
        mIssue.areaType = mAreaTypeNameEditText.getText().toString();
        mIssue.description = mDescription.getText().toString();
        mIssue.imgUrl = mImageUri;
        mIssue.latitude = mLatLng.latitude + "";
        mIssue.longitude = mLatLng.longitude + "";
        mIssue.radius = mRadius;
        mIssue.userDpUrl = mPreferences.getString(Accounts.PHOTO_URL, "");
        mIssue.username = mPreferences.getString(Accounts.NAME, mAnonymousString);
        mIssue.userId = mPreferences.getString(Accounts.GOOGLE_ID, "");

        if (mIssue.anonymous)
        {
            mIssue.userDpUrl = "";
            mIssue.username = mAnonymousString;
        }

        intent.putExtra(ISSUE_DATA, mIssue);
        startService(intent);

        intent = new Intent(AddIssueActivity.this, MainActivity.class);
        intent.putExtra(ISSUE_DATA, mIssue);
        intent.putExtra(ADD_TEMP_MARKER, true);
        startActivity(intent);
        finish();
    }

    private void beginCrop(Uri source)
    {
        Uri destination = Uri.fromFile(new File(mImageUtil.getMediaStorageDir(), "temp" + (temp++) + ".png"));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result)
    {
        if (resultCode == RESULT_OK)
        {
            Uri imgUri = Crop.getOutput(result);
            mImgPreview.setImageURI(imgUri);
            mImageUri = imgUri.getPath();
        }
        else if (resultCode == Crop.RESULT_ERROR)
        {
            Log.d("CropGetError",Crop.getError(result).getMessage());
            toast(getString(R.string.sorryImageFormatNotSupported));
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
                        toast(getString(R.string.please_try_again));
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
                            toast(getString(R.string.please_try_again));
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

    private int isIssueCategoryCheck()
    {
        String areaAndIssueType = mAreaTypeNameEditText.getText().toString().trim();
        if (mImageUri == null || mImageUri.isEmpty())
        {
            return IMAGE_ERROR;
        }
        else if ((!mImageUri.contains(".png") && !mImageUri.contains(".jpg")))
        {
            return IMAGE_ERROR;
        }
        else if (areaAndIssueType.isEmpty())
        {
            return AREA_TYPE_AND_ISSUE_TYPE_ERROR;
        }
        else if (!areaAndIssueType.contains("@"))
        {
            return AREA_TYPE_ERROR;
        }
        else if (!areaAndIssueType.contains("#"))
        {
            return ISSUE_TYPE_ERROR;
        }
        return NO_ERROR;
    }
}
