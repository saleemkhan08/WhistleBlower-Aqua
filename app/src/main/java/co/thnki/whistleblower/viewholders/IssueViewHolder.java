package co.thnki.whistleblower.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.R;

public class IssueViewHolder extends RecyclerView.ViewHolder
{
    @Bind(R.id.areaTypeName)
    public TextView areaTypeName;
    @Bind(R.id.username)
    public TextView username;
    @Bind(R.id.issueImage)
    public ImageView issueImage;

    @Bind(R.id.issueImageContainer)
    public RelativeLayout issueImageContainer;

    @Bind(R.id.profilePic)
    public ImageView profilePic;
    @Bind(R.id.deleteIcon)
    public ImageView optionsIcon;
    @Bind(R.id.shareIcon)
    public ImageView shareIcon;
    @Bind(R.id.locationIcon)
    public ImageView locationIcon;
    @Bind(R.id.volunteerIcon)
    public ImageView volunteerIcon;

    @Bind(R.id.optionsIconContainer)
    public View optionsIconContainer;
    @Bind(R.id.locationContainer)
    public View locationContainer;
    @Bind(R.id.volunteerContainer)
    public View volunteerContainer;
    @Bind(R.id.shareContainer)
    public View shareContainer;

    public IssueViewHolder(View view)
    {
        super(view);
        ButterKnife.bind(this, view);
    }
}
