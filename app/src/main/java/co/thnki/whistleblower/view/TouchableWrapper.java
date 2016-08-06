package co.thnki.whistleblower.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

@SuppressLint("ViewConstructor")
public class TouchableWrapper extends FrameLayout
{
    private OnMapTouchListener onMapTouchListener;

    public TouchableWrapper(Context context, OnMapTouchListener listener)
    {
        super(context);
        try
        {
            onMapTouchListener = listener;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(context.toString() + " must implement OnMapTouchListener");
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                onMapTouchListener.onActionDown();
                break;

            case MotionEvent.ACTION_UP:
                onMapTouchListener.onActionUp();
                break;

            case MotionEvent.ACTION_CANCEL:
                onMapTouchListener.onActionUp();
                break;

            case MotionEvent.ACTION_SCROLL :
                onMapTouchListener.onScroll();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onDragEvent(DragEvent event)
    {
        onMapTouchListener.onDrag();
        return true;
    }

    public interface OnMapTouchListener
    {
        void onActionUp();
        void onScroll();
        void onDrag();
        void onActionDown();
    }
}
