package co.thnki.whistleblower;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.adapters.IssueAndCommentsAdapter;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.VolunteerFragment;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.pojos.VolleyResponse;
import co.thnki.whistleblower.receivers.InternetConnectivityListener;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.VolleyUtil;

import static co.thnki.whistleblower.WhistleBlower.toast;
import static co.thnki.whistleblower.fragments.VolunteerFragment.POSTING_COMMENT;
import static co.thnki.whistleblower.fragments.VolunteerFragment.VOLUNTEER_REQUEST_COMMENT;
import static co.thnki.whistleblower.fragments.VolunteerFragment.VOLUNTEER_REQUEST_NGO;

public class IssueRecyclerActivity extends AppCompatActivity
{
    private static final String GET_COMMENTS = "getComments";
    private static final int COMMENTS_REQUEST_CODE = 103;

    @SuppressWarnings("WeakerAccess")
    @Bind(R.id.issueAndCommentsList)
    RecyclerView mIssueAndCommentsList;

    @Bind(R.id.username)
    TextView mUsername;

    @Bind(R.id.optionsIconContainer)
    View optionsIconContainer;

    @SuppressWarnings("WeakerAccess")
    IssueAndCommentsAdapter mIssueAndCommentsAdapter;
    @Bind(R.id.profilePic)
    ImageView profilePic;
    private Issue mIssue;
    private ImageUtil mImageUtil;
    private SharedPreferences mPreferences;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        handleEnterAndExitTransition();
        setContentView(R.layout.activity_issue_recycler_layout);

        mImageUtil = new ImageUtil(this);
        ButterKnife.bind(this);
        Otto.register(this);
        mPreferences = WhistleBlower.getPreferences();

        checkIntentData();
        handleProfilePic();
        handleOptionPopMenu();

        mIssueAndCommentsAdapter = new IssueAndCommentsAdapter(this, mIssue);
        mIssueAndCommentsList.setAdapter(mIssueAndCommentsAdapter);
        mIssueAndCommentsList.setLayoutManager(new LinearLayoutManager(this));

        VolunteerFragment mVolunteerFragment = new VolunteerFragment();
        mIssueAndCommentsAdapter.setVolunteerFragment(mVolunteerFragment);

        if (ConnectivityUtil.isConnected())
        {
            fetchComments();
        }
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
        mUsername.setText(mIssue.username);
    }

    private void handleProfilePic()
    {
        String dpUrl = mIssue.userDpUrl;
        if (dpUrl == null || dpUrl.isEmpty())
        {
            profilePic.setImageResource(R.mipmap.user_icon_accent);
        }
        else
        {
            mImageUtil.displayRoundedImage(dpUrl, profilePic);
        }
    }

    private void handleOptionPopMenu()
    {
        optionsIconContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(IssueRecyclerActivity.this, v);
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
                        if (ConnectivityUtil.isConnected())
                        {
                            switch (item.getItemId())
                            {
                                case R.id.editIssue:
                                    mIssueAndCommentsAdapter.editIssue();
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
                Toast.makeText(IssueRecyclerActivity.this, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                Toast.makeText(IssueRecyclerActivity.this, "Please Try Again" + "\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
                Toast.makeText(IssueRecyclerActivity.this, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                Toast.makeText(IssueRecyclerActivity.this, "Please Try Again" + "\n" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchComments()
    {
        Bundle bundle = new Bundle();
        bundle.putString(VolleyUtil.KEY_ACTION, GET_COMMENTS);
        bundle.putString(VolleyUtil.KEY_LIMIT, "30");
        bundle.putString(VolleyUtil.KEY_OFFSET, "0");
        bundle.putString(IssuesDao.ISSUE_ID, mIssue.issueId);
        VolleyUtil.sendPostData(bundle, COMMENTS_REQUEST_CODE);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onInternetConnected(String action)
    {
        Log.d("ConnectivityListener", "onInternetConnected : adapter : " + action);
        switch (action)
        {
            case InternetConnectivityListener.INTERNET_CONNECTED:
                fetchComments();
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void showProgressBar(String action)
    {
        if (action.equals(POSTING_COMMENT))
        {
            showProgressDialog(getString(R.string.posting));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void volleyResponse(VolleyResponse response)
    {
        switch (response.mRequestCode)
        {
            case COMMENTS_REQUEST_CODE:
                if (response.mStatus)
                {
                    mIssueAndCommentsAdapter.loadComments(response.mResponse);
                }
                break;

            case VOLUNTEER_REQUEST_COMMENT:
            case VOLUNTEER_REQUEST_NGO:
                if (response.mStatus)
                {
                    fetchComments();
                }
                break;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Otto.unregister(this);
        mIssueAndCommentsAdapter.unRegister();
    }
}
