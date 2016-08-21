package co.thnki.whistleblower.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.NativeExpressAdView;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.thnki.whistleblower.R;

public class AdViewHolder extends RecyclerView.ViewHolder
{
    @Bind(R.id.nativeAdViewContainer)
    RelativeLayout mAdContainer;

    public AdViewHolder(final View parentView)
    {
        super(parentView);
        ButterKnife.bind(this, parentView);
    }

    public void updateAd(NativeExpressAdView mNativeExpressAdView)
    {
        mAdContainer.removeAllViews();
        mAdContainer.addView(mNativeExpressAdView);
    }
}