package co.thnki.whistleblower.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import co.thnki.whistleblower.R;

public class LoadingViewHolder extends RecyclerView.ViewHolder
{
    public View mLoadingView;
    public LoadingViewHolder(final View parentView)
    {
        super(parentView);
        mLoadingView = parentView.findViewById(R.id.loading_view);
    }
}