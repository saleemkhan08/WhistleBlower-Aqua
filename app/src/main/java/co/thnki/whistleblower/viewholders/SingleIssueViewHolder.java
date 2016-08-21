package co.thnki.whistleblower.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.R;

public class SingleIssueViewHolder extends RecyclerView.ViewHolder
{
    @Bind(R.id.areaTypeName)
    public TextView areaTypeName;

    @Bind(R.id.issueImage)
    public ImageView issueImage;

    @Bind(R.id.issueImageContainer)
    public RelativeLayout issueImageContainer;

    @Bind(R.id.shareIcon)
    public ImageView shareIcon;

    @Bind(R.id.locationIcon)
    public ImageView locationIcon;

    @Bind(R.id.volunteerIcon)
    public ImageView volunteerIcon;

    @Bind(R.id.locationContainer)
    public View locationContainer;

    @Bind(R.id.volunteerContainer)
    public View volunteerContainer;

    @Bind(R.id.shareContainer)
    public View shareContainer;

    @Bind(R.id.commentProgressContainer)
    public LinearLayout mCommentProgressContainer;

    @Bind(R.id.issueDescription)
    public TextView issueDescription;

    public SingleIssueViewHolder(View view)
    {
        super(view);
        ButterKnife.bind(this, view);
    }
}
