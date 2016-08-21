package co.thnki.whistleblower.utils;

import android.content.SharedPreferences;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.adapters.IssueAdapter;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Issue;

import static co.thnki.whistleblower.services.NewsFeedsUpdateService.GET_ISSUES;
import static co.thnki.whistleblower.services.NewsFeedsUpdateService.LIMIT;
import static co.thnki.whistleblower.services.NewsFeedsUpdateService.OFFSET;

public class IssueRecyclerViewUtil extends RecyclerView.OnScrollListener implements ResultListener<String>
{
    private static final String HASH_TAGS = "hashTags";
    private IssueAdapter mIssueAdapter;
    public static final int ISSUES_LIMIT = 20;
    private SharedPreferences mPreference;
    private static boolean mIsLoading;
    private static final int VISIBLE_THRESHOLD = 1;

    private void log(String msg)
    {
        Log.d("IssueRecyclerViewUtil", msg);
    }

    public IssueRecyclerViewUtil(IssueAdapter adapter)
    {
        mIssueAdapter = adapter;
        mPreference = WhistleBlower.getPreferences();
        log("constructor");
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy)
    {
        super.onScrolled(recyclerView, dx, dy);
        if (ConnectivityUtil.isConnected() && mIssueAdapter.mAreMoreIssuesAvailable)
        {
            log("dx, dy : " + dx + "," + dy + ", mIsLoading : " + mIsLoading);
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int totalItemCount = linearLayoutManager.getItemCount();
            int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

            log("totalItemCount : " + totalItemCount + ", lastVisibleItem : " + lastVisibleItem);

            if (!mIsLoading && totalItemCount <= (lastVisibleItem + VISIBLE_THRESHOLD))
            {
                mIsLoading = true;
                Map<String, String> getParams = new HashMap<>();
                getParams.put(ResultListener.ACTION, GET_ISSUES);
                getParams.put(LIMIT, ISSUES_LIMIT + "");
                getParams.put(OFFSET, getOffset());
                getParams.put(HASH_TAGS, mPreference.getString(HASH_TAGS, ""));
                VolleyUtil.sendGetData(getParams, this);
                mIssueAdapter.loadingStarted();
                log("VolleyUtil : " + getParams);
            }
        }
    }

    private String getOffset()
    {
        int adsCount = 0;
        int totalSize = mIssueAdapter.mIssuesPlusAdList.size();
        for (Issue issue : mIssueAdapter.mIssuesPlusAdList)
        {
            if (issue.areaType == null)
            {
                adsCount++;
            }
        }
        log("totalSize : "+totalSize+", adsCount : "+adsCount);
        return (totalSize - adsCount) + "";
    }

    @Override
    public void onSuccess(String result)
    {
        log("onSuccess : " + result);
        mIsLoading = false;
        ArrayList<Issue> issueList = new ArrayList<>();
        try
        {
            JSONArray array = new JSONArray(result);
            int totalNoOfIssues = array.length();
            log("totalNoOfIssues : " + totalNoOfIssues);
            for (int issueIndex = 0; issueIndex < totalNoOfIssues; issueIndex++)
            {
                Issue issue = new Issue();
                JSONObject json = (JSONObject) array.get(issueIndex);
                issue.issueId = json.getString(IssuesDao.ISSUE_ID);
                issue.photoId = json.getString(IssuesDao.PHOTO_ID);
                issue.imgUrl = VolleyUtil.IMAGE_URL + issue.photoId + ".png";
                issue.userDpUrl = json.getString(IssuesDao.USER_DP_URL);
                issue.userId = json.getString(IssuesDao.USER_ID);
                issue.username = json.getString(IssuesDao.USERNAME);
                issue.description = json.getString(IssuesDao.DESCRIPTION);
                issue.areaType = json.getString(IssuesDao.AREA_TYPE);
                issue.radius = json.getInt(IssuesDao.RADIUS);
                issue.latitude = json.getDouble(IssuesDao.LATITUDE) + "";
                issue.longitude = json.getDouble(IssuesDao.LONGITUDE) + "";
                issueList.add(issue);
            }
            mIssueAdapter.onLoad(issueList);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(VolleyError error)
    {
        mIsLoading = false;
        mIssueAdapter.onLoad(null);
    }
}
