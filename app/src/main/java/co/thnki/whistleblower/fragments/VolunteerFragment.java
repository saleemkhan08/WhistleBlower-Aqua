package co.thnki.whistleblower.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.pojos.Accounts;
import co.thnki.whistleblower.singletons.Otto;
import co.thnki.whistleblower.utils.VolleyUtil;

import static co.thnki.whistleblower.WhistleBlower.toast;
import static co.thnki.whistleblower.doas.IssuesDao.ISSUE_ID;

public class VolunteerFragment extends DialogFragment
{
    private static final String SAVE_NGO_DETAILS = "saveNgoDetails";
    private static final String NGO_NAME = "ngoName";
    private static final String NGO_URL = "ngoUrl";
    private static final String POST_COMMENT = "postComment";
    private static final String COMMENT = "comment";
    public static final int VOLUNTEER_REQUEST_COMMENT = 101;
    public static final int VOLUNTEER_REQUEST_NGO = 102;
    public static final String POSTING_COMMENT = "postingsComment";
    @Bind(R.id.ngoSuggestionContainer)
    LinearLayout mNgoSuggestionContainer;

    @Bind(R.id.comment)
    EditText mCommentEditText;

    @Bind(R.id.ngoName)
    EditText mNgoNameEditText;

    @Bind(R.id.ngoUrl)
    EditText mNgoUrlEditText;

    public boolean mIsNgoSuggestionExpanded;

    @Bind(R.id.suggestNgoButtonContainer)
    public View mSuggestNgoButtonContainer;

    public VolunteerFragment()
    {
    }

    private static String mIssueId = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View parentView = inflater.inflate(R.layout.fragment_volunteer, container, false);
        ButterKnife.bind(this, parentView);
        Otto.register(this);
        TextView dialogTitle = (TextView) parentView.findViewById(R.id.dialogTitle);
        dialogTitle.setTypeface(WhistleBlower.getTypeFace());
        mCommentEditText.requestFocus();
        Bundle bundle = getArguments();
        if (bundle != null)
        {
            if (bundle.containsKey(ISSUE_ID))
            {
                String tempIssueId = bundle.getString(ISSUE_ID);
                if (!mIssueId.equals(tempIssueId))
                {
                    clearEditTextViews();
                }
                mIssueId = tempIssueId;
            }
        }
        else
        {
            dismiss();
        }
        handleBackKey();
        return parentView;
    }

    private void clearEditTextViews()
    {
        mCommentEditText.setText("");
        mNgoNameEditText.setText("");
        mNgoUrlEditText.setText("");
    }

    @OnClick(R.id.postComment)
    public void saveNgoDetails()
    {
        Otto.post(POSTING_COMMENT);
        String ngoName = mNgoNameEditText.getText().toString().trim();
        String comment = mCommentEditText.getText().toString().trim();
        String ngoUrl = mNgoUrlEditText.getText().toString().trim();
        if (comment.isEmpty())
        {
            toast(getString(R.string.pleaseEnterTheComment));
        }
        else if(mIsNgoSuggestionExpanded && ngoName.isEmpty())
        {
            toast(getString(R.string.pleaseEnterNgoName));
        }
        else if (mIsNgoSuggestionExpanded && !isValidURL(ngoUrl))
        {
            toast(getString(R.string.pleaseEnterNgoUrl));
        }
        else
        {
            SharedPreferences preferences = WhistleBlower.getPreferences();
            Map<String, String> data = new HashMap<>();

            data.put(IssuesDao.ISSUE_ID, mIssueId);
            data.put(VolleyUtil.KEY_ACTION, POST_COMMENT);
            data.put(IssuesDao.USER_ID, preferences.getString(Accounts.GOOGLE_ID, ""));
            data.put(IssuesDao.USER_DP_URL, preferences.getString(Accounts.PHOTO_URL, ""));
            data.put(IssuesDao.USERNAME, preferences.getString(Accounts.NAME, ""));
            data.put(COMMENT, comment);
            data.put(NGO_NAME, ngoName);
            data.put(NGO_URL, ngoUrl);

            VolleyUtil.sendPostData(data, VOLUNTEER_REQUEST_NGO);
            dismiss();
        }
    }

    private boolean isValidURL(String urlStr)
    {
        try
        {
            URL url = new URL(urlStr);
            return true;
        }
        catch (MalformedURLException e)
        {
            return false;
        }
    }

    @OnClick(R.id.suggestNgoButton)
    public void showNgoSuggestionBox()
    {
        mSuggestNgoButtonContainer.setVisibility(View.GONE);
        mNgoSuggestionContainer.setVisibility(View.VISIBLE);
        mIsNgoSuggestionExpanded = true;
    }

    public void hideSuggestNgoContainer()
    {
        mSuggestNgoButtonContainer.setVisibility(View.VISIBLE);
        mNgoSuggestionContainer.setVisibility(View.GONE);
        mIsNgoSuggestionExpanded = false;
    }


    @OnClick(R.id.closeDialog)
    public void close()
    {
        dismiss();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        super.onDismiss(dialog);
        Otto.unregister(this);
    }

    private void handleBackKey()
    {
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener()
        {
            @Override
            public boolean onKey(android.content.DialogInterface dialog, int keyCode, android.view.KeyEvent event)
            {

                if ((keyCode == android.view.KeyEvent.KEYCODE_BACK))
                {
                    if (mIsNgoSuggestionExpanded)
                    {
                        hideSuggestNgoContainer();
                        return true; // pretend we've processed it
                    }
                }
                return false; // pass on to be processed as normal
            }
        });
    }
}