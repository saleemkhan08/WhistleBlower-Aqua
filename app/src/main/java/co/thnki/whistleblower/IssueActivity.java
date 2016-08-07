package co.thnki.whistleblower;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.VolleyUtil;


public class IssueActivity extends AppCompatActivity
{
    @Bind(R.id.areaTypeName)
    TextView areaTypeName;
    @Bind(R.id.username)
    TextView username;
    @Bind(R.id.issueDescription)
    TextView issueDescription;

    @Bind(R.id.issueImage)
    ImageView issueImage;
    @Bind(R.id.profilePic)
    ImageView profilePic;
    @Bind(R.id.optionsIcon)
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
    View locationContainer;
    @Bind(R.id.volunteerContainer)
    View volunteerContainer;
    @Bind(R.id.shareContainer)
    View shareContainer;

    @BindColor(R.color.transparent)
    int colorTransparent;

    ImageUtil mImageUtil;

    SharedPreferences preferences;
    Issue issue;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_layout);
        Intent intent = getIntent();
        mImageUtil = new ImageUtil(this);
        ButterKnife.bind(this);
        preferences = WhistleBlower.getPreferences();
        if (!intent.hasExtra(IssuesDao.ISSUE_ID))
        {
            startActivity(new Intent(this, MainActivity.class));
        }
        issue = intent.getParcelableExtra(IssuesDao.ISSUE_ID);
        // Set the results into TextViews
        areaTypeName.setText(issue.areaType);
        username.setText(issue.username);

        if(issue.description.trim().equals(""))
        {
            issueDescription.setVisibility(View.GONE);
        }else
        {
            issueDescription.setText(issue.description);
        }

        locationContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Otto.post(issue);
            }
        });
        shareContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(IssueActivity.this, "Share Post", Toast.LENGTH_SHORT).show();
            }
        });

        volunteerContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(IssueActivity.this, "Volunteer", Toast.LENGTH_SHORT).show();
            }
        });

        optionsIconContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(IssueActivity.this, v);
                popup.getMenuInflater()
                        .inflate(R.menu.issue_options, popup.getMenu());

                Menu menu = popup.getMenu();
                if (issue.userId.equals(preferences.getString(Accounts.GOOGLE_ID, "")))
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
                        switch (item.getItemId())
                        {
                            case R.id.editIssue:
                                editIssue(issue);
                                break;
                            case R.id.deleteIssue:
                                deleteIssue(issue.issueId);
                                break;
                            case R.id.reportIssue:
                                reportIssue(issue.issueId);
                                break;

                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        mImageUtil.displayImage(issue.imgUrl, issueImage, false);
        String dpUrl = issue.userDpUrl;
        if (dpUrl == null || dpUrl.isEmpty())
        {
            profilePic.setImageResource(R.mipmap.user_primary_dark_o);
        }
        else
        {
            mImageUtil.displayImage(dpUrl, profilePic, true);
            profilePic.setBackgroundColor(colorTransparent);
        }

        issueImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
            }
        });
    }

    private void editIssue(Issue issue)
    {
        Intent intent = new Intent(this, AddIssueActivity.class);
        intent.putExtra(AddIssueActivity.ISSUE_DATA, issue);
        startActivity(intent);
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

}
