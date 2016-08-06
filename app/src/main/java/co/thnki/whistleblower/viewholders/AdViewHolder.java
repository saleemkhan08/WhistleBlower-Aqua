package co.thnki.whistleblower.viewholders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;

import co.thnki.whistleblower.R;
import co.thnki.whistleblower.WhistleBlower;
import co.thnki.whistleblower.services.RemoteConfigService;
import co.thnki.whistleblower.utils.ImageUtil;

public class AdViewHolder extends RecyclerView.ViewHolder
{
    public AdViewHolder(View parentView)
    {
        super(parentView);
        AdRequest mAdRequest = new AdRequest.Builder()
                .addTestDevice("51B143E236817102C0BC44F96EE8A5F7")
                .build();

        RelativeLayout layout = (RelativeLayout) parentView.findViewById(R.id.listNativeAdViewContainer);
        NativeExpressAdView adView = setupAd(parentView);

        if(adView.getAdSize().getWidth() == 280)
        {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) parentView.getLayoutParams();
            int margin = ImageUtil.pixels(parentView.getContext(), 20);
            layoutParams.setMargins(0,margin,0,margin);
            parentView.setLayoutParams(layoutParams);
        }
        adView.loadAd(mAdRequest);
        layout.addView(adView);
    }



    private synchronized NativeExpressAdView setupAd(final View parentView)
    {
        parentView.setVisibility(View.GONE);

        Context context = parentView.getContext();
        NativeExpressAdView adView = new NativeExpressAdView(context);
        String mAdUnitId = WhistleBlower.getPreferences()
                .getString(RemoteConfigService.AD_UNIT_ID + "2", "ca-app-pub-9949935976977846/3250322417");

        adView.setAdSize(new AdSize( 320, 320));
        adView.setAdUnitId(mAdUnitId);

        adView.setAdListener(new AdListener()
        {
            @Override
            public void onAdLoaded()
            {
                super.onAdLoaded();
                parentView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int i)
            {
                super.onAdFailedToLoad(i);
                Log.d("Ads","Load : "+i);
                Log.d("AdWidth","width : "+ImageUtil.dp(parentView.getContext(), parentView.getWidth()));
            }
        });
        return adView;
    }
}
