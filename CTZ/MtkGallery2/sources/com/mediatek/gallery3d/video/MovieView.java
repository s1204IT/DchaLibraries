package com.mediatek.gallery3d.video;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.MovieViewAdapter;

public class MovieView extends MovieViewAdapter {
    private static final boolean LOG = true;
    private static final String TAG = "VP_MovieView";
    private SurfaceHolder.Callback mSHCallback;
    private SurfaceCallback mSurfaceListener;
    private int mVideoHeight;
    private int mVideoWidth;

    public interface SurfaceCallback {
        void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3);

        void onSurfaceCreated(SurfaceHolder surfaceHolder);

        void onSurfaceDestroyed(SurfaceHolder surfaceHolder);
    }

    public MovieView(Context context) {
        this(context, null, 0);
    }

    public MovieView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MovieView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mSHCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (MovieView.this.mSurfaceListener != null) {
                    MovieView.this.mSurfaceListener.onSurfaceDestroyed(surfaceHolder);
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (MovieView.this.mSurfaceListener != null) {
                    MovieView.this.mSurfaceListener.onSurfaceCreated(surfaceHolder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i2, int i3, int i4) {
                if (MovieView.this.mSurfaceListener != null) {
                    MovieView.this.mSurfaceListener.onSurfaceChanged(surfaceHolder, i2, i3, i4);
                }
            }
        };
        initVideoView();
    }

    private void initVideoView() {
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        getHolder().addCallback(this.mSHCallback);
        setFocusable(LOG);
        setFocusableInTouchMode(LOG);
        requestFocus();
    }

    public void setVideoLayout(int i, int i2) {
        Log.v(TAG, "setVideoLayout, videoWidth = " + i + ", videoHeight = " + i2);
        this.mVideoWidth = i;
        this.mVideoHeight = i2;
        requestLayout();
    }

    public void setSurfaceListener(SurfaceCallback surfaceCallback) {
        this.mSurfaceListener = surfaceCallback;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int defaultSize = getDefaultSize(this.mVideoWidth, i);
        int defaultSize2 = getDefaultSize(this.mVideoHeight, i2);
        if (this.mVideoWidth > 0 && this.mVideoHeight > 0) {
            if (this.mVideoWidth * defaultSize2 > this.mVideoHeight * defaultSize) {
                defaultSize2 = (this.mVideoHeight * defaultSize) / this.mVideoWidth;
            } else if (this.mVideoWidth * defaultSize2 < this.mVideoHeight * defaultSize) {
                defaultSize = (this.mVideoWidth * defaultSize2) / this.mVideoHeight;
            }
        }
        Log.v(TAG, "onMeasure[video size = " + this.mVideoWidth + 'x' + this.mVideoHeight + "] set view size = " + defaultSize + 'x' + defaultSize2);
        setMeasuredDimension(defaultSize, defaultSize2);
    }
}
