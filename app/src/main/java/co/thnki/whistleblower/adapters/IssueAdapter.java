package co.thnki.whistleblower.adapters;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.AddIssueActivity;
import co.thnki.whistleblower.IssueActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.VolunteerFragment;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.VolleyUtil;

import static co.thnki.whistleblower.IssueActivity.VOLUNTEER_FRAGMENT_TAG;
import static co.thnki.whistleblower.WhistleBlower.toast;

public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder>
{
    LayoutInflater mInflater;
    AppCompatActivity mActivity;
    public ArrayList<Issue> mIssuesArrayList;
    public ImageUtil mImageUtil;

    SharedPreferences preferences;
    private ProgressDialog mProgressDialog;
    private VolunteerFragment mVolunteerFragment;
    private FragmentManager mFragmentManager;

    public IssueAdapter(AppCompatActivity activity, ArrayList<Issue> mIssuesList)
    {
        mActivity = activity;
        mInflater = LayoutInflater.from(mActivity);
        preferences = WhistleBlower.getPreferences();
        this.mIssuesArrayList = mIssuesList;
        mImageUtil = new ImageUtil(mActivity);
        mFragmentManager = mActivity.getSupportFragmentManager();
    }

    @Override
    public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = mInflater.inflate(R.layout.issue_layout, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final IssueViewHolder holder, final int position)
    {
        final Issue issue = mIssuesArrayList.get(position);

        holder.areaTypeName.setText(issue.areaType);
        holder.username.setText(issue.username);
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
                if (ConnectivityUtil.isConnected(mActivity))
                {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT, mActivity.getString(R.string.app_name));
                    share.putExtra(Intent.EXTRA_TEXT, "whistleblower-thnkin.rhcloud.com/issue.php?issueId=" + issue.issueId);
                    mActivity.startActivity(Intent.createChooser(share, "Share link!"));
                }
                else
                {
                    toast(mActivity.getString(R.string.noInternet));
                }
            }
        });

        holder.volunteerContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ConnectivityUtil.isConnected(mActivity))
                {
                    Bundle bundle = new Bundle();
                    bundle.putString(IssuesDao.ISSUE_ID, issue.issueId);
                    mVolunteerFragment.setArguments(bundle);
                    mVolunteerFragment.show(mFragmentManager, VOLUNTEER_FRAGMENT_TAG);
                }
                else
                {
                    toast(mActivity.getString(R.string.noInternet));
                }
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
                        if (ConnectivityUtil.isConnected(mActivity))
                        {
                            switch (item.getItemId())
                            {
                                case R.id.editIssue:
                                    editIssue(issue, holder.issueImage);
                                    break;
                                case R.id.deleteIssue:
                                    deleteIssue(issue.issueId, position);
                                    break;
                                case R.id.reportIssue:
                                    reportIssue(issue.issueId);
                                    break;

                            }
                        }
                        else
                        {
                            toast(mActivity.getString(R.string.noInternet));
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        Log.d("imgUrl", issue.imgUrl);
        mImageUtil.displayImage(issue.imgUrl, holder.issueImage, false);
        /*GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(holder.issueImage);
        Glide.with(mActivity).load(R.raw.loading).into(imageViewTarget);*/

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
            public void onClick(View view)
            {
                Intent intent = new Intent(mActivity, IssueActivity.class);
                intent.putExtra(IssuesDao.ISSUE_ID, issue);

                if (Build.VERSION.SDK_INT >= 21)
                {
                    view.setTransitionName(mActivity.getString(R.string.sharedElement1));
                    ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                            .makeSceneTransitionAnimation(mActivity, view, view.getTransitionName());
                    mActivity.startActivity(intent, optionsCompat.toBundle());
                }
                else
                {
                    mActivity.startActivity(intent);
                }
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
                toast(mActivity.getString(R.string.reported));
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                toast(mActivity.getString(R.string.please_try_again));
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
                if (result.equals("Deleted"))
                {
                    removeAt(position);
                    IssuesDao.delete(issueId);
                    toast(mActivity.getString(R.string.deleted));
                }
                else
                {
                    toast(mActivity.getString(R.string.couldntDelete));
                }
            }

            @Override
            public void onError(VolleyError error)
            {
                hideProgressDialog();
                toast(mActivity.getString(R.string.please_try_again));
            }
        });
    }

    public void removeAt(int position)
    {
        mIssuesArrayList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mIssuesArrayList.size());
    }

    private void editIssue(Issue issue, View view)
    {
        Intent intent = new Intent(mActivity, AddIssueActivity.class);
        intent.putExtra(AddIssueActivity.ISSUE_DATA, issue);

        if (Build.VERSION.SDK_INT >= 21)
        {
            view.setTransitionName(mActivity.getString(R.string.sharedElement1));
            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(mActivity, view, view.getTransitionName());
            mActivity.startActivity(intent, optionsCompat.toBundle());
        }
        else
        {
            mActivity.startActivity(intent);
        }
    }

    @Override
    public int getItemCount()
    {
        return mIssuesArrayList.size();
    }

    public void setVolunteerFragment(VolunteerFragment volunteerFragment)
    {
        mVolunteerFragment = volunteerFragment;
    }

    class IssueViewHolder extends RecyclerView.ViewHolder
    {
        @Bind(R.id.areaTypeName)
        TextView areaTypeName;
        @Bind(R.id.username)
        TextView username;
        @Bind(R.id.issueImage)
        ImageView issueImage;
        @Bind(R.id.profilePic)
        ImageView profilePic;
        @Bind(R.id.deleteIcon)
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