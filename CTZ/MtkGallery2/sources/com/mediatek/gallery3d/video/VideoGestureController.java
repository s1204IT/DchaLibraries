package com.mediatek.gallery3d.video;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.app.MovieControllerOverlay;
import com.android.gallery3d.ui.GestureRecognizer;

public class VideoGestureController implements GestureRecognizer.Listener {
    private Context mContext;
    private MovieControllerOverlay mController;
    private GestureRecognizer mGestureRecognizer;

    public VideoGestureController(Context context, View view, MovieControllerOverlay movieControllerOverlay) {
        this.mContext = context;
        this.mController = movieControllerOverlay;
        this.mGestureRecognizer = new GestureRecognizer(this.mContext, this);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view2, MotionEvent motionEvent) {
                VideoGestureController.this.mGestureRecognizer.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    @Override
    public boolean onSingleTapUp(float f, float f2) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(float f, float f2) {
        this.mController.show();
        return true;
    }

    @Override
    public boolean onDoubleTap(float f, float f2) {
        return false;
    }

    @Override
    public boolean onScroll(float f, float f2, float f3, float f4) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public boolean onScaleBegin(float f, float f2) {
        return false;
    }

    @Override
    public boolean onScale(float f, float f2, float f3) {
        return false;
    }

    @Override
    public void onScaleEnd() {
    }

    @Override
    public void onDown(float f, float f2) {
    }

    @Override
    public void onUp() {
    }
}
