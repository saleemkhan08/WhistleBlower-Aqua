package co.thnki.whistleblower.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;

import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.doas.IssuesDao;
import co.thnki.whistleblower.singletons.Otto;

public class DialogsUtil
{
    public static final String ISSUE_TYPE = "issueType";
    private final Resources mResources;
    private Activity mActivity;
    private SharedPreferences mPreferences;
    private String mIssueTypes[];

    public DialogsUtil(AppCompatActivity context)
    {
        mActivity = context;
        mPreferences = WhistleBlower.getPreferences();
        mResources = mActivity.getResources();
        mIssueTypes = mResources.getStringArray(R.array.issue_types);
    }


    public void showIssueTypeDialog()
    {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setTitle(mResources.getString(R.string.select_issue_type));

        dialog.setSingleChoiceItems(mIssueTypes, mPreferences.getInt(ISSUE_TYPE, 0), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                mPreferences.edit().putInt(ISSUE_TYPE, i).apply();
                dialogInterface.dismiss();
                Otto.post(ISSUE_TYPE);
            }
        });
        dialog.show();
    }

    public void showAnonymousDialog()
    {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setTitle(mActivity.getResources().getString(R.string.anonymous_confirmation));
        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                mPreferences.edit().putBoolean(IssuesDao.ANONYMOUS, true).apply();
                dialog.dismiss();
                Otto.post(IssuesDao.ANONYMOUS);
            }
        });
        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                mPreferences.edit().putBoolean(IssuesDao.ANONYMOUS, false).apply();
                dialog.dismiss();
                Otto.post(IssuesDao.ANONYMOUS);
            }
        });
        dialog.show();
    }

    public String getSelectedIssueType()
    {
        return mIssueTypes[mPreferences.getInt(ISSUE_TYPE, 0)];
    }
}