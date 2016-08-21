package co.thnki.whistleblower.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.R;

public class CommentViewHolder extends RecyclerView.ViewHolder
{
    @Bind(R.id.deleteIcon)
    public ImageView mDeleteIcon;

    @Bind(R.id.profilePic)
    public ImageView mProfilePic;

    @Bind(R.id.username)
    public TextView mUsername;

    @Bind(R.id.commentText)
    public TextView mCommentText;

    public CommentViewHolder(View parentView)
    {
        super(parentView);
        ButterKnife.bind(this,parentView);
    }
}
