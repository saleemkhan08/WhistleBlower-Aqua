package co.thnki.whistleblower.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.interfaces.ResultListener;
import co.thnki.whistleblower.pojos.Issue;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.VolleyUtil;

public class NewsFeedsUpdateService extends IntentService
{
    public static final String ISSUES_FEEDS_UPDATED = "issuesFeedsUpdated";

    public NewsFeedsUpdateService()
    {
        super("NewsFeedsUpdateService");
    }

    private static final String GET_ISSUES = "getIssues";
    private static final String LIMIT = "limit";
    private static final String OFFSET = "offset";

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Map<String, String> getParams = new HashMap<>();
        getParams.put(ResultListener.ACTION, GET_ISSUES);
        getParams.put(LIMIT, "10");
        getParams.put(OFFSET, "0");

        VolleyUtil.sendGetData(getParams, new ResultListener<String>()
        {
            @Override
            public void onSuccess(String result)
            {
                try
                {
                    JSONArray array = new JSONArray(result);
                    try
                    {
                        IssuesDao.delete();//TODO Find a better Logic to do this
                    }
                    catch (SQLiteException e)
                    {
                        Log.d("Database", "Does not exist");
                    }
                    int totalNoOfIssues = array.length();
                    for (int issueIndex = 0; issueIndex < totalNoOfIssues; issueIndex++)
                    {
                        Issue issue = new Issue();
                        JSONObject json = (JSONObject) array.get(issueIndex);
                        issue.issueId = json.getString(IssuesDao.ISSUE_ID);
                        issue.photoId = json.getString(IssuesDao.PHOTO_ID);
                        issue.imgUrl = VolleyUtil.IMAGE_URL + issue.photoId + ".png";
                        Log.d("imgUrl","Service : "+issue.imgUrl);
                        issue.userDpUrl = json.getString(IssuesDao.USER_DP_URL);
                        issue.userId = json.getString(IssuesDao.USER_ID);
                        issue.username = json.getString(IssuesDao.USERNAME);
                        issue.description = json.getString(IssuesDao.DESCRIPTION);
                        issue.areaType = json.getString(IssuesDao.AREA_TYPE);
                        issue.radius = json.getInt(IssuesDao.RADIUS);
                        issue.latitude = json.getDouble(IssuesDao.LATITUDE) + "";
                        issue.longitude = json.getDouble(IssuesDao.LONGITUDE) + "";
                        IssuesDao.insert(issue);
                    }
                    Otto.post(ISSUES_FEEDS_UPDATED);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(VolleyError error)
            {
            }
        });
    }
}
