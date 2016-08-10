package co.thnki.whistleblower;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.VolunteerFragment;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.pojos.VolleyResponse;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.TransitionUtil;
import co.thnki.whistleblower.utils.VolleyUtil;

import static co.thnki.whistleblower.R.id.deleteIcon;
import static co.thnki.whistleblower.WhistleBlower.toast;
import static co.thnki.whistleblower.fragments.VolunteerFragment.POSTING_COMMENT;
import static co.thnki.whistleblower.fragments.VolunteerFragment.VOLUNTEER_REQUEST_COMMENT;
import static co.thnki.whistleblower.fragments.VolunteerFragment.VOLUNTEER_REQUEST_NGO;

public class IssueActivity extends AppCompatActivity
{
    private static final String GET_COMMENTS = "getComments";
    private static final int COMMENTS_REQUEST_CODE = 103;
    private static final String COMMENT_ID = "commentId";
    private static final String DELETE_COMMENT = "deleteComment";
    @Bind(R.id.areaTypeName)
    TextView areaTypeName;
    @Bind(R.id.username)
    TextView username;
    @Bind(R.id.issueDescription)
    TextView issueDescription;

    @Bind(R.id.commentProgressContainer)
    LinearLayout mBar;

    @Bind(R.id.issueImage)
    ImageView issueImage;
    @Bind(R.id.profilePic)
    ImageView profilePic;
    @Bind(deleteIcon)
    ImageView optionsIcon;
    @Bind(R.id.shareIcon)
    ImageView shareIcon;
    @Bind(R.id.locationIcon)
    ImageView locationIcon;
    @Bind(R.id.volunteerIcon)
    ImageView volunteerIcon;

    @Bind(R.id.optionsIconContainer)
    View optionsIconContainer;
    @Bind(R.id.locationContainer)
    View mLocationContainer;
    @Bind(R.id.volunteerContainer)
    View volunteerContainer;
    @Bind(R.id.shareContainer)
    View mShareContainer;

    @BindColor(R.color.transparent)
    int colorTransparent;

    ImageUtil mImageUtil;

    SharedPreferences mPreferences;
    Issue mIssue;
    private ProgressDialog mProgressDialog;
    public static final String VOLUNTEER_FRAGMENT_TAG = "volunteerFragmentTag";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        handleEnterAndExitTransition();
        setContentView(R.layout.activity_issue_layout);

        mImageUtil = new ImageUtil(this);
        ButterKnife.bind(this);
        Otto.register(this);
        mPreferences = WhistleBlower.getPreferences();

        checkIntentData();
        handleDescription();
        handleViewOnMap();
        handleShare();
        handleVolunteer();
        handleOptionPopMenu();
        handleProfilePic();
        handleIssueImage();

        new CommentsTask().execute();
    }

    private void handleEnterAndExitTransition()
    {
        if (Build.VERSION.SDK_INT >= 21)
        {
            getWindow().setSharedElementEnterTransition(TransitionInflater
                    .from(this).inflateTransition(R.transition.shared_element_transition));

            getWindow().setSharedElementExitTransition(TransitionInflater
                    .from(this).inflateTransition(R.transition.shared_element_transition));
        }
    }

    private void handleDescription()
    {
        if (mIssue.description.trim().equals(""))
        {
            issueDescription.setVisibility(View.GONE);
        }
        else
        {
            issueDescription.setText(mIssue.description);
        }
    }

    private void handleIssueImage()
    {
        mImageUtil.displayImage(mIssue.imgUrl, issueImage, false);
    }

    private void handleProfilePic()
    {
        String dpUrl = mIssue.userDpUrl;
        if (dpUrl == null || dpUrl.isEmpty())
        {
            profilePic.setImageResource(R.mipmap.user_primary_dark_o);
        }
        else
        {
            mImageUtil.displayImage(dpUrl, profilePic, true);
            profilePic.setBackgroundColor(colorTransparent);
        }
    }

    private void handleOptionPopMenu()
    {
        optionsIconContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(IssueActivity.this, v);
                popup.getMenuInflater()
                        .inflate(R.menu.issue_options, popup.getMenu());

                Menu menu = popup.getMenu();
                if (mIssue.userId.equals(mPreferences.getString(Accounts.GOOGLE_ID, "")))
                {
                    menu.getItem(0).setVisible(false);
                    menu.getItem(1).setVisible(true);
                    menu.getItem(2).setVisible(true);
                }
                else
                {
                    menu.getItem(0).setVisible(true);
                    menu.getItem(1).setVisible(false);
                    menu.getItem(2).setVisible(false);
                }
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                {
                    public boolean onMenuItemClick(MenuItem item)
                    {
                        if (ConnectivityUtil.isConnected(IssueActivity.this))
                        {
                            switch (item.getItemId())
                            {
                                case R.id.editIssue:
                                    editIssue(mIssue);
                                    break;
                                case R.id.deleteIssue:
                                    deleteIssue(mIssue.issueId);
                                    break;
                                case R.id.reportIssue:
                                    reportIssue(mIssue.issueId);
                                    break;

                            }
                        }
                        else
                        {
                            toast(getString(R.string.noInternet));
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });
    }

    private void handleVolunteer()
    {
        volunteerContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ConnectivityUtil.isConnected(IssueActivity.this))
                {
                    FragmentManager manager = getSupportFragmentManager();
                    VolunteerFragment mVolunteerFragment = (VolunteerFragment) manager.findFragmentByTag(VOLUNTEER_FRAGMENT_TAG);

                    if (mVolunteerFragment == null)
                    {
                        mVolunteerFragment = new VolunteerFragment();
                    }

                    Bundle bundle = new Bundle();
                    bundle.putString(IssuesDao.ISSUE_ID, mIssue.issueId);
                    mVolunteerFragment.setArguments(bundle);

                    mVolunteerFragment.show(manager, VOLUNTEER_FRAGMENT_TAG);
                }
                else
                {
                    toast(getString(R.string.noInternet));
                }
            }
        });
    }

    private void handleShare()
    {
        mShareContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ConnectivityUtil.isConnected(IssueActivity.this))
                {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    share.putExtra(Intent.EXTRA_TEXT, "whistleblower-thnkin.rhcloud.com/issue.php?issueId=" + mIssue.issueId);
                    startActivity(Intent.createChooser(share, "Share link!"));
                }
                else
                {
                    toast(getString(R.string.noInternet));
                }
            }
        });

    }

    private void handleViewOnMap()
    {
        mLocationContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onBackPressed();
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Otto.post(mIssue);
                    }
                }, 1000);
            }
        });
    }

    private void checkIntentData()
    {
        Intent intent = getIntent();
        if (intent.hasExtra(IssuesDao.ISSUE_ID))
        {
            mIssue = intent.getParcelableExtra(IssuesDao.ISSUE_ID);
        }
        else if (intent.hasExtra(AddIssueActivity.ISSUE_DATA))
        {
            mIssue = intent.getParcelableExtra(AddIssueActivity.ISSUE_DATA);
        }
        else
        {
            startActivity(new Intent(this, MainActivity.class));
        }

        areaTypeName.setText(mIssue.areaType);
        username.setText(mIssue.username);
    }

    private void editIssue(Issue issue)
    {
        Intent intent = new Intent(this, AddIssueActivity.class);
        intent.putExtra(AddIssueActivity.ISSUE_DATA, issue);

        if (Build.VERSION.SDK_INT >= 21)
        {
            issueImage.setTransitionName(getString(R.string.sharedElement1));
            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(this, issueImage, issueImage.getTransitionName());
            startActivity(intent, optionsCompat.toBundle());
        }
        else
        {
            startActivity(intent);
        }
    }

    private void showProgressDialog(String msg)
    {
        if (mProgressDialog == null)
        {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setMessage(msg);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog()
    {
        if (mProgressDialog != null && mProgressDialog.isShowing())
        {
            mProgressDialog.hide();
        }
    }

    private void deleteIssue(String issueId)
    {
        showProgressDialog("Deleting...");
        Map<String, String> map = new HashMap<>();
        map.put(VolleyUtil.KEY_ACTION, "deleteIssue");
        map.put(IssuesDao.ISSUE_ID, issueId);
        VolleyUtil.sendPostData(map, new ResultListener<String>()
        {
            @Override
            public void onSuccess(String result)
            {
                hideProgressDialog();
                Toast.makeText(IssueActivity.this, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                Toast.makeText(IssueActivity.this, "Please Try Again" + "\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reportIssue(String issueId)
    {
        showProgressDialog("Reporting...");
        Map<String, String> map = new HashMap<>();
        map.put(VolleyUtil.KEY_ACTION, "reportSpam");
        map.put(IssuesDao.ISSUE_ID, issueId);
        VolleyUtil.sendPostData(map, new ResultListener<String>()
        {
            @Override
            public void onSuccess(String result)
            {
                hideProgressDialog();
                Toast.makeText(IssueActivity.this, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                Toast.makeText(IssueActivity.this, "Please Try Again" + "\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    class CommentsTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected void onPreExecute()
        {
            TransitionUtil.slideTransition(mBar);
            mBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            Bundle bundle = new Bundle();
            bundle.putString(VolleyUtil.KEY_ACTION, GET_COMMENTS);
            bundle.putString(IssuesDao.ISSUE_ID, mIssue.issueId);
            VolleyUtil.sendPostData(bundle, COMMENTS_REQUEST_CODE);
            return null;
        }
    }

    @Subscribe
    public void showProgressBar(String action)
    {
        if(action.equals(POSTING_COMMENT))
        {
            mBar.setVisibility(View.VISIBLE);
        }
    }
    @Subscribe
    public void volleyResponse(VolleyResponse response)
    {
        switch (response.mRequestCode)
        {
            case COMMENTS_REQUEST_CODE:
                if (response.mStatus)
                {
                    loadComments(response.mResponse);
                }
                TransitionUtil.slideTransition(mBar);
                mBar.setVisibility(View.GONE);
                break;

            case VOLUNTEER_REQUEST_COMMENT:
                if (response.mStatus)
                {
                    new CommentsTask().execute();
                }

                break;
            case VOLUNTEER_REQUEST_NGO:
                if (response.mStatus)
                {
                    new CommentsTask().execute();
                }
                break;
        }
    }

    private void loadComments(String mResponse)
    {
        try
        {
            JSONArray array = new JSONArray(mResponse);
            int totalNoOfComments = array.length();
            LinearLayout commentsContainer = (LinearLayout) findViewById(R.id.commentsContainer);
            commentsContainer.removeAllViews();
            for (int commentIndex = 0; commentIndex < totalNoOfComments; commentIndex++)
            {
                final JSONObject json = (JSONObject) array.get(commentIndex);

                LayoutInflater inflater = LayoutInflater.from(this);
                LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.comment_layout, null, false);

                if(mPreferences.getString(Accounts.GOOGLE_ID, "").equals(json.getString(IssuesDao.USER_ID)))
                {
                    ImageView deleteIcon = (ImageView) layout.findViewById(R.id.deleteIcon);
                    deleteIcon.setVisibility(View.VISIBLE);
                    deleteIcon.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            try
                            {
                                deleteComment(json.getString(COMMENT_ID));
                            }
                            catch (JSONException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                ImageView profilePic = (ImageView) layout.findViewById(R.id.profilePic);
                mImageUtil.displayImage(json.getString(IssuesDao.USER_DP_URL), profilePic, true);

                TextView username = (TextView) layout.findViewById(R.id.username);
                username.setText(json.getString(IssuesDao.USERNAME));

                TextView commentTextView = (TextView) layout.findViewById(R.id.commentText);
                String ngoName = json.getString("ngoName");
                String comment = json.getString("comment");
                String ngoUrl = json.getString("ngoUrl");
                String text = "";
                if (comment != null && !comment.isEmpty())
                {
                    text = comment;
                }
                if (ngoName != null && !ngoName.isEmpty() && ngoUrl != null && !ngoUrl.isEmpty())
                {
                    text += "<br> NGO Link : <a href='" + ngoUrl + "'> " + ngoName + " </a>";
                }

                commentTextView.setClickable(true);
                commentTextView.setMovementMethod(LinkMovementMethod.getInstance());

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                {
                    commentTextView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
                }
                else
                {
                    commentTextView.setText(Html.fromHtml(text));
                }
                commentsContainer.addView(layout);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void deleteComment(String string)
    {
        TransitionUtil.slideTransition(mBar);
        mBar.setVisibility(View.VISIBLE);

        Map<String, String> data = new HashMap<>();
        data.put(VolleyUtil.KEY_ACTION, DELETE_COMMENT);
        data.put(COMMENT_ID, string);
        VolleyUtil.sendPostData(data, new ResultListener<String>()
        {
            @Override
            public void onSuccess(String result)
            {
                if(IssueActivity.this != null && !IssueActivity.this.isDestroyed())
                {
                    new CommentsTask().execute();
                }
            }

            @Override
            public void onError(VolleyError error)
            {

            }
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Otto.unregister(this);
    }
}
