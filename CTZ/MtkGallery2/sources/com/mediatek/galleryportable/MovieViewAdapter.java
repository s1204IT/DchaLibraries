package com.mediatek.galleryportable;

import android.content.Context;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class MovieViewAdapter extends SurfaceView {
    private SubtitleImpl mSubtitleImpl;

    public MovieViewAdapter(Context context) {
        this(context, null, 0);
    }

    public MovieViewAdapter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MovieViewAdapter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mSubtitleImpl = new SubtitleImpl(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mSubtitleImpl.onLayout();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        this.mSubtitleImpl.draw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSubtitleImpl.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mSubtitleImpl.onDetachedFromWindow();
    }

    public void register(Context context, MediaPlayer mp) {
        this.mSubtitleImpl.register(context, mp);
    }
}
