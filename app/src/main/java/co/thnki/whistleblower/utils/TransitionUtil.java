package co.thnki.whistleblower.utils;

import android.os.Build;
import android.transition.Explode;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.ViewGroup;

public class TransitionUtil
{
    public static void defaultTransition(ViewGroup container)
    {
        TransitionManager.beginDelayedTransition(container);
    }
    public static void slideTransition(ViewGroup container)
    {
        if(Build.VERSION.SDK_INT >= 21)
        {
            Slide slide = new Slide();
            TransitionManager.beginDelayedTransition(container, slide);
        }
        else
        {
            TransitionManager.beginDelayedTransition(container);
        }
    }

    public static void explodeTransition(ViewGroup container)
    {
        if(Build.VERSION.SDK_INT >= 21)
        {
            Explode slide = new Explode();
            TransitionManager.beginDelayedTransition(container, slide);
        }
        else
        {
            TransitionManager.beginDelayedTransition(container);
        }
    }
}
