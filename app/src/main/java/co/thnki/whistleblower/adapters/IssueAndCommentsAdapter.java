package co.thnki.whistleblower.adapters;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.thnki.whistleblower.AddIssueActivity;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.fragments.VolunteerFragment;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.pojos.Comments;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.receivers.InternetConnectivityListener;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.ConnectivityUtil;
import co.thnki.whistleblower.utils.ImageUtil;
import co.thnki.whistleblower.utils.TransitionUtil;
import co.thnki.whistleblower.utils.VolleyUtil;
import co.thnki.whistleblower.viewholders.AdViewHolder;
import co.thnki.whistleblower.viewholders.CommentViewHolder;
import co.thnki.whistleblower.viewholders.SingleIssueViewHolder;

import static co.thnki.whistleblower.WhistleBlower.toast;
import static java.lang.Math.abs;

public class IssueAndCommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final String COMMENT_ID = "commentId";
    private static final String DELETE_COMMENT = "deleteComment";
    public static final String VOLUNTEER_FRAGMENT_TAG = "volunteerFragmentTag";
    private AppCompatActivity mActivity;
    private ArrayList mCommentsPlusAdList;
    private static final int AD_VIEW = 3;
    private static final int ISSUE_VIEW = 1;
    private static final int COMMENT_VIEW = 2;
    private boolean isAdInserted;
    private Issue mIssue;
    private ImageView mIssueImage;
    private ImageUtil mImageUtil;

    private SharedPreferences mPreferences;
    private VolunteerFragment mVolunteerFragment;
    private FragmentManager mFragmentManager;
    private NativeExpressAdView mAdView;
    private LinearLayout mCommentProgressContainer;

    public void log(String msg)
    {
        Log.d("CommentAdapter", msg);
    }

    public IssueAndCommentsAdapter(AppCompatActivity activity, Issue issue)
    {
        mActivity = activity;
        mIssue = issue;
        Otto.register(this);
        mCommentsPlusAdList = new ArrayList<>();
        mCommentsPlusAdList.add(0, mIssue);
        mPreferences = WhistleBlower.getPreferences();
        mImageUtil = new ImageUtil(mActivity);
        mFragmentManager = mActivity.getSupportFragmentManager();
        log("constructor");
        fetchAd();
    }

    private void fetchAd()
    {
        log("fetchAd");
        if (ConnectivityUtil.isConnected())
        {
            mAdView = new NativeExpressAdView(mActivity);
            AdRequest mAdRequest = new AdRequest.Builder()
                    .build();
            String mAdUnitId = "ca-app-pub-9949935976977846/8315233219";
            mAdView.setAdSize(new AdSize(320, 132));
            mAdView.setAdUnitId(mAdUnitId);
            mAdView.loadAd(mAdRequest);
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        log("onCreateViewHolder");
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder viewHolder;
        log("viewType : " + viewType);
        switch (viewType)
        {
            case AD_VIEW:
                log("adView");
                View adParentView = inflater.inflate(R.layout.comment_ad_row, parent, false);
                viewHolder = new AdViewHolder(adParentView);
                ((AdViewHolder) viewHolder).updateAd(mAdView);
                break;

            case ISSUE_VIEW:
                log("ISSUE_VIEW");
                View issueParentView = inflater.inflate(R.layout.content_issue, parent, false);
                viewHolder = new SingleIssueViewHolder(issueParentView);
                break;

            case COMMENT_VIEW:
                log("COMMENT_VIEW");
            default:
                View contentParentView = inflater.inflate(R.layout.comment_layout, parent, false);
                viewHolder = new CommentViewHolder(contentParentView);
                break;
        }
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position)
    {
        log("position : " + position);
        switch (position)
        {
            case 0:
                return ISSUE_VIEW;
            default:
                int posType = (position - 2) % 5;
                if (!isAdInserted)
                {
                    posType = -1;
                }
                log("posType : " + posType + ", Pos : " + position);
                if (posType == 0)
                {
                    return AD_VIEW;
                }
                return COMMENT_VIEW;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position)
    {
        log("onBindViewHolder : " + position);
        switch (getItemViewType(position))
        {
            case ISSUE_VIEW:
                configureIssueContent(holder);
                break;
            case COMMENT_VIEW:
                Comments comment = (Comments) mCommentsPlusAdList.get(position);
                if (null != comment)
                {
                    configureComment(comment, holder, position);
                }
                break;
        }
    }

    private void configureIssueContent(RecyclerView.ViewHolder viewHolder)
    {
        log("configureIssueContent");
        final SingleIssueViewHolder holder = (SingleIssueViewHolder) viewHolder;
        mIssueImage = holder.issueImage;
        mCommentProgressContainer = holder.mCommentProgressContainer;
        if(mIssue.description != null && !mIssue.description.isEmpty())
        {
            holder.issueDescription.setText(mIssue.description);
        }
        else
        {
            holder.issueDescription.setVisibility(View.GONE);
        }
        holder.areaTypeName.setText(mIssue.areaType);
        holder.locationContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Otto.post(mIssue);
            }
        });

        configureShare(holder, mIssue);
        configureVolunteer(holder, mIssue);
        mImageUtil.displayImage(mIssue.imgUrl, holder.issueImage);
    }

    private void hideCommentsLoadingBar()
    {
        log("hideCommentsLoadingBar");
        TransitionUtil.slideTransition(mCommentProgressContainer);
        mCommentProgressContainer.setVisibility(View.GONE);
    }

    private void configureVolunteer(SingleIssueViewHolder holder, final Issue issue)
    {
        log("configureVolunteer");
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

    private void configureShare(SingleIssueViewHolder holder, final Issue issue)
    {
        log("configureShare");
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

    @SuppressWarnings("deprecation")
    private void configureComment(final Comments comment, RecyclerView.ViewHolder viewHolder, final int position)
    {
        log("configureComment : " + position);
        CommentViewHolder holder = (CommentViewHolder) viewHolder;
        if (comment.isCommenter)
        {
            holder.mDeleteIcon.setVisibility(View.VISIBLE);
            holder.mDeleteIcon.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    deleteComment(comment.commentId, position);
                }
            });
        }
        mImageUtil.displayRoundedImage(comment.userDpUrl, holder.mProfilePic);
        holder.mCommentText.setText(comment.userName);

        holder.mCommentText.setClickable(true);
        holder.mCommentText.setMovementMethod(LinkMovementMethod.getInstance());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            holder.mCommentText.setText(Html.fromHtml(comment.comment, Html.FROM_HTML_MODE_LEGACY));
        }
        else
        {
            holder.mCommentText.setText(Html.fromHtml(comment.comment));
        }
    }

    private void removeAt(int position)
    {
        log("removeAt : " + position);
        fetchAd();
        mCommentsPlusAdList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mCommentsPlusAdList.size());
    }

    public void editIssue()
    {
        log("editIssue");
        Intent intent = new Intent(mActivity, AddIssueActivity.class);
        intent.putExtra(AddIssueActivity.ISSUE_DATA, mIssue);

        if (Build.VERSION.SDK_INT >= 21)
        {
            mIssueImage.setTransitionName(mActivity.getString(R.string.sharedElement1));
            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(mActivity, mIssueImage, mIssueImage.getTransitionName());
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
        log("getItemCount : " + mCommentsPlusAdList.size());
        return mCommentsPlusAdList.size();
    }

    public void setVolunteerFragment(VolunteerFragment volunteerFragment)
    {
        log("setVolunteerFragment");
        mVolunteerFragment = volunteerFragment;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onInternetConnected(String action)
    {
        log("onInternetConnected : adapter : " + action);
        switch (action)
        {
            case InternetConnectivityListener.INTERNET_CONNECTED:
                fetchAd();
                break;
        }
    }

    private void updateList(ArrayList<Comments> commentsArrayList)
    {
        log("updateList");
        insertAdSpace(commentsArrayList);
        notifyDataSetChanged();
    }

    private void insertAdSpace(ArrayList<Comments> commentsArrayList)
    {
        log("insertAdSpace");
        isAdInserted = false;
        isAdInserted = true;
        int size = commentsArrayList.size();

        int finalSize = abs(size / 5) + size + 2;
        log("finalSize : " + finalSize + ", mAlarmList : " + size);

        if (size > 0)
        {
            for (int i = 1, j = 0; i < finalSize; i++)
            {
                if (((i - 2) % 5) == 0)
                {
                    log("Ad " + i);
                    mCommentsPlusAdList.add(i, mAdView);
                }
                else
                {
                    log("Content " + i);
                    mCommentsPlusAdList.add(i, commentsArrayList.get(j++));
                }
            }
        }
    }

    public void loadComments(String mResponse)
    {
        log("loadComments : " + mResponse);
        try
        {
            JSONArray array = new JSONArray(mResponse);
            int totalNoOfComments = array.length();
            ArrayList<Comments> commentsArrayList = new ArrayList<>();
            for (int commentIndex = 0; commentIndex < totalNoOfComments; commentIndex++)
            {
                Comments comment = new Comments();
                final JSONObject json = (JSONObject) array.get(commentIndex);

                String ngoName = json.getString("ngoName");
                String ngoUrl = json.getString("ngoUrl");
                String commentText = json.getString("comment");

                String text = "";
                if (commentText != null && !commentText.isEmpty())
                {
                    text = commentText;
                }
                if (ngoName != null && !ngoName.isEmpty() && ngoUrl != null && !ngoUrl.isEmpty())
                {
                    if (text.isEmpty())
                    {
                        text += "NGO : <a href='" + ngoUrl + "'> " + ngoName + " </a>";
                    }
                    else
                    {
                        text += "<br> NGO : <a href='" + ngoUrl + "'> " + ngoName + " </a>";
                    }
                }
                comment.comment = text;
                comment.isCommenter = mPreferences.getString(Accounts.GOOGLE_ID, "")
                        .equals(json.getString(IssuesDao.USER_ID));

                comment.commentId = json.getString(COMMENT_ID);
                comment.userDpUrl = json.getString(IssuesDao.USER_DP_URL);
                comment.userName = json.getString(IssuesDao.USERNAME);
                commentsArrayList.add(comment);
            }
            hideCommentsLoadingBar();
            updateList(commentsArrayList);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void deleteComment(String commentId, int position)
    {
        log("deleteComment : " + position + ", commentId : " + commentId);
        Map<String, String> data = new HashMap<>();
        data.put(VolleyUtil.KEY_ACTION, DELETE_COMMENT);
        data.put(COMMENT_ID, commentId);
        VolleyUtil.sendPostData(data, null);
        removeAt(position);
    }

    public void unRegister()
    {
        log("unRegister");
        Otto.unregister(this);
    }
}