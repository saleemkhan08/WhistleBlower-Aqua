package co.thnki.whistleblower.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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

import com.android.volley.VolleyError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.thnki.whistleblower.AddIssueActivity;
import co.thnki.whistleblower.IssueActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.MapFragment;
import co.thnki.whistleblower.fragments.VolunteerFragment;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.receivers.InternetConnectivityListener;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.IssueRecyclerViewUtil;
import co.thnki.whistleblower.utils.VolleyUtil;
import co.thnki.whistleblower.viewholders.AdViewHolder;
import co.thnki.whistleblower.viewholders.IssueViewHolder;
import co.thnki.whistleblower.viewholders.LoadingViewHolder;

import static co.thnki.whistleblower.WhistleBlower.toast;
import static co.thnki.whistleblower.adapters.IssueAndCommentsAdapter.VOLUNTEER_FRAGMENT_TAG;
import static java.lang.Math.abs;

public class IssueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private IssueRecyclerViewUtil mIssuesRecyclerViewUtil;
    private RecyclerView mIssuesRecyclerView;
    private AppCompatActivity mActivity;
    public ArrayList<Issue> mIssuesPlusAdList;

    private static final int LOADING_VIEW = 126;
    private static final int AD_VIEW = 125;
    private static final int CONTENT_VIEW = 124;
    private boolean isAdInserted;

    private ImageUtil mImageUtil;

    private SharedPreferences mPreferences;
    private ProgressDialog mProgressDialog;
    private VolunteerFragment mVolunteerFragment;
    private FragmentManager mFragmentManager;
    private NativeExpressAdView mAdView;
    public boolean mAreMoreIssuesAvailable;

    public IssueAdapter(AppCompatActivity activity)
    {
        mActivity = activity;
        Otto.register(this);
        mIssuesPlusAdList = new ArrayList<>();
        mPreferences = WhistleBlower.getPreferences();
        mImageUtil = new ImageUtil(mActivity);
        mFragmentManager = mActivity.getSupportFragmentManager();
        mAreMoreIssuesAvailable = true;
        fetchAd();
    }

    private void fetchAd()
    {
        Log.d("fetchingAd", "fetchingAd");
        mAdView = new NativeExpressAdView(mActivity);
        AdRequest mAdRequest = new AdRequest.Builder()
                .build();
        String mAdUnitId = "ca-app-pub-9949935976977846/3250322417";
        mAdView.setAdSize(new AdSize(340, 320));
        mAdView.setAdUnitId(mAdUnitId);
        mAdView.setAdListener(new AdListener()
        {
            @Override
            public void onAdLoaded()
            {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int i)
            {
                super.onAdFailedToLoad(i);
            }
        });
        mAdView.loadAd(mAdRequest);
    }

    public void unRegister()
    {
        Otto.unregister(this);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder viewHolder;

        switch (viewType)
        {
            case LOADING_VIEW:
                View loadingView = inflater.inflate(R.layout.loading_view, parent, false);
                viewHolder = new LoadingViewHolder(loadingView);
                break;

            case AD_VIEW:
                View adParentView = inflater.inflate(R.layout.ad_row, parent, false);
                viewHolder = new AdViewHolder(adParentView);
                ((AdViewHolder) viewHolder).updateAd(mAdView);
                break;

            default:
                View contentParentView = inflater.inflate(R.layout.issue_layout, parent, false);
                viewHolder = new IssueViewHolder(contentParentView);
                break;
        }
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position)
    {
        if (mIssuesPlusAdList.get(position) == null)
        {
            return LOADING_VIEW;
        }
        else
        {
            int posType = (position - 1) % 5;

            if (!isAdInserted)
            {
                posType = -1;
            }
            switch (posType)
            {
                case 0:
                    Log.d("GetItemType", posType + " : AD_VIEW : Pos : " + position);
                    return AD_VIEW;
                default:
                    Log.d("GetItemType", posType + " : CONTENT_VIEW : Pos : " + position);
                    return CONTENT_VIEW;
            }
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position)
    {
        switch (getItemViewType(position))
        {
            case CONTENT_VIEW:
                final Issue issue = mIssuesPlusAdList.get(position);
                if (null != issue)
                {
                    configureContent(issue, viewHolder, position);
                }
                break;
        }
    }


    private void configureContent(final Issue issue, RecyclerView.ViewHolder viewHolder, final int position)
    {
        final IssueViewHolder holder = (IssueViewHolder) viewHolder;

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

        configureShare(holder, issue);
        configureVolunteer(holder, issue);
        configureOptions(holder, issue, position);

        mImageUtil.displayImage(issue.imgUrl, holder.issueImage);

        String dpUrl = issue.userDpUrl;
        if (dpUrl == null || dpUrl.isEmpty())
        {
            holder.profilePic.setImageResource(R.mipmap.user_icon_accent);
        }
        else
        {
            mImageUtil.displayRoundedImage(dpUrl, holder.profilePic);
        }

        configureIssue(holder, issue);
    }

    private void configureOptions(final IssueViewHolder holder, final Issue issue, final int position)
    {
        holder.optionsIconContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                PopupMenu popup = new PopupMenu(mActivity, v);
                popup.getMenuInflater()
                        .inflate(R.menu.issue_options, popup.getMenu());

                Menu menu = popup.getMenu();
                if (issue.userId.equals(mPreferences.getString(Accounts.GOOGLE_ID, "")))
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
                                    editIssue(issue, holder.issueImage);
                                    break;
                                case R.id.deleteIssue:
                                    confirmDelete(issue, position);
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
    }

    private void configureVolunteer(IssueViewHolder holder, final Issue issue)
    {
        holder.volunteerContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ConnectivityUtil.isConnected())
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
    }

    private void configureIssue(IssueViewHolder holder, final Issue issue)
    {
        holder.issueImageContainer.setOnClickListener(new View.OnClickListener()
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

    private void configureShare(IssueViewHolder holder, final Issue issue)
    {
        holder.shareContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ConnectivityUtil.isConnected())
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

    private void confirmDelete(final Issue mIssue, final int position)
    {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setTitle(R.string.action_delete);
        dialog.setMessage(R.string.areYouSureYouWantToDeleteThisPost);
        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                deleteIssue(mIssue.issueId);
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void deleteIssue(final String issueId)
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
                    IssuesDao.delete(issueId);
                    toast(mActivity.getString(R.string.deleted));
                    mPreferences.edit().putBoolean(MapFragment.IS_RELOAD_NECESSARY, true).apply();
                    updateList(IssuesDao.getList());
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
        return mIssuesPlusAdList == null ? 0 : mIssuesPlusAdList.size();
    }

    public void setVolunteerFragment(VolunteerFragment volunteerFragment)
    {
        mVolunteerFragment = volunteerFragment;
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

    @Subscribe
    public void onInternetConnected(String action)
    {
        Log.d("ConnectivityListener", "onInternetConnected : adapter : " + action);
        switch (action)
        {
            case InternetConnectivityListener.INTERNET_CONNECTED:
                updateList(IssuesDao.getList());
                Log.d("onInternetConnected", "notifyDataSetChanged");
                break;
        }
    }

    public void updateList(ArrayList<Issue> issueArrayList)
    {
        insertAdSpace(issueArrayList);
        notifyDataSetChanged();
    }

    private void insertAdSpace(ArrayList<Issue> issueArrayList)
    {
        isAdInserted = false;
        mIssuesPlusAdList = new ArrayList<>();
        if (!ConnectivityUtil.isConnected())
        {
            mIssuesPlusAdList = issueArrayList;
        }
        else
        {
            isAdInserted = true;
            int size = issueArrayList.size();

            int finalSize = abs(size / 5) + size + 1;
            Log.d("insertAdSpace", "finalSize : " + finalSize + ", mAlarmList : " + size);

            if (size > 0)
            {
                for (int i = 0, j = 0; i < finalSize; i++)
                {
                    if (((i - 1) % 5) == 0)
                    {
                        Log.d("insertAdSpace", "Ad " + i);
                        mIssuesPlusAdList.add(i, new Issue());
                    }
                    else
                    {
                        Log.d("insertAdSpace", "Content " + i);
                        mIssuesPlusAdList.add(i, issueArrayList.get(j++));
                    }
                }
            }
        }
    }

    private void log(String msg)
    {
        Log.d("IssueRecyclerViewUtil", msg);
    }

    public void onLoad(ArrayList<Issue> issueList)
    {
        log("issueList : " + issueList);
        int currentSize = mIssuesPlusAdList.size();
        if (issueList != null)
        {
            if (issueList.size() > 0)
            {
                isAdInserted = true;
                int size = issueList.size();

                int initialSize = +currentSize;
                int finalSize = initialSize + abs(size / 5) + size;

                if (size > 0)
                {
                    for (int i = initialSize, j = 0; i < finalSize; i++)
                    {
                        if (((i - 1) % 5) == 0)
                        {
                            log("Ad " + i);
                            mIssuesPlusAdList.add(i, null);
                        }
                        else
                        {
                            log("Content " + i);
                            mIssuesPlusAdList.add(i, issueList.get(j++));
                        }
                    }
                }
            }
            else
            {
                mAreMoreIssuesAvailable = false;
            }
        }
        removeLoading();
    }

    private void removeLoading()
    {
        for (int i = 0; i < mIssuesPlusAdList.size(); i++)
        {
            if (mIssuesPlusAdList.get(i) == null)
            {
                mIssuesPlusAdList.remove(i);
            }
        }
        notifyDataSetChanged();
    }

    public void loadingStarted()
    {
        mIssuesPlusAdList.add(null);
        notifyItemInserted(mIssuesPlusAdList.size() - 1);
    }
}