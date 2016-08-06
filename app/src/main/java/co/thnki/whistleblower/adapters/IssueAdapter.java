package co.thnki.whistleblower.adapters;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.IssueActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.VolleyUtil;

import static co.thnki.whistleblower.WhistleBlower.toast;

public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder>
{
    LayoutInflater mInflater;
    AppCompatActivity mActivity;
    ArrayList<Issue> mIssuesArrayList;
    ImageUtil mImageUtil;

    SharedPreferences preferences;
    private ProgressDialog mProgressDialog;

    public IssueAdapter(AppCompatActivity activity, ArrayList<Issue> mIssuesList)
    {
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);
        preferences = WhistleBlower.getPreferences();
        this.mIssuesArrayList = mIssuesList;
        mImageUtil = new ImageUtil(mActivity);
    }

    @Override
    public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = mInflater.inflate(R.layout.issue_layout, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(IssueViewHolder holder, final int position)
    {
        final Issue issue = mIssuesArrayList.get(position);

        holder.areaTypeName.setText(issue.areaType);
        holder.username.setText(issue.username);
        if(issue.description.trim().equals(""))
        {
            holder.issueDescription.setVisibility(View.GONE);
        }else
        {
            holder.issueDescription.setText(issue.description);
        }

        holder.locationContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Otto.post(issue);
            }
        });

        holder.shareContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, "Title Of The Post");
                share.putExtra(Intent.EXTRA_TEXT, "whistleblower-thnkin.rhcloud.com/issue.php?issueId="+issue.issueId);
                mActivity.startActivity(Intent.createChooser(share, "Share link!"));

            }
        });

        holder.volunteerContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toast("Volunteer : " + position);
            }
        });

        holder.optionsIconContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(mActivity, v);
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
                                deleteIssue(issue.issueId, position);
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

        mImageUtil.displayImage(issue.imgUrl, holder.issueImage, false);
        String dpUrl = issue.userDpUrl;
        if (dpUrl == null || dpUrl.isEmpty())
        {
            holder.profilePic.setImageResource(R.mipmap.user_primary_dark_o);
        }
        else
        {
            mImageUtil.displayImage(dpUrl, holder.profilePic, true);
        }

        holder.issueImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                Intent intent = new Intent(mActivity, IssueActivity.class);
                intent.putExtra(IssuesDao.ISSUE_ID, issue);
                mActivity.startActivity(intent);
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
                Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                Toast.makeText(mActivity, "Please Try Again"+"\n"+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteIssue(final String issueId, final int position)
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
                Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
                if(result.equals("Deleted"))
                {
                    removeAt(position);
                    IssuesDao.delete(issueId);
                }
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                Toast.makeText(mActivity, "Please Try Again"+"\n"+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void removeAt(int position)
    {
        mIssuesArrayList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mIssuesArrayList.size());
    }

    private void editIssue(Issue issue)
    {
        toast("Not Implemented");
    }

    @Override
    public int getItemCount()
    {
        return mIssuesArrayList.size();
    }

    class IssueViewHolder extends RecyclerView.ViewHolder
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

        public IssueViewHolder(View view)
        {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private void showProgressDialog(String msg)
    {
        if (mProgressDialog == null)
        {
            mProgressDialog = new ProgressDialog(mActivity);
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
}