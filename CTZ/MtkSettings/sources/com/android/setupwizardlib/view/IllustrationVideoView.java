package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Animatable;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import com.android.setupwizardlib.R;

@TargetApi(14)
public class IllustrationVideoView extends TextureView implements Animatable, MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, TextureView.SurfaceTextureListener {
    protected float mAspectRatio;
    protected MediaPlayer mMediaPlayer;
    Surface mSurface;
    private int mVideoResId;

    public IllustrationVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAspectRatio = 1.0f;
        this.mVideoResId = 0;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwIllustrationVideoView);
        this.mVideoResId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwIllustrationVideoView_suwVideo, 0);
        typedArrayObtainStyledAttributes.recycle();
        setScaleX(0.9999999f);
        setScaleX(0.9999999f);
        setSurfaceTextureListener(this);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        float f = size2;
        float f2 = size;
        if (f < this.mAspectRatio * f2) {
            size = (int) (f / this.mAspectRatio);
        } else {
            size2 = (int) (f2 * this.mAspectRatio);
        }
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(size, 1073741824), View.MeasureSpec.makeMeasureSpec(size2, 1073741824));
    }

    public void setVideoResource(int i) {
        if (i != this.mVideoResId) {
            this.mVideoResId = i;
            createMediaPlayer();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            start();
        } else {
            stop();
        }
    }

    private void createMediaPlayer() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.release();
        }
        if (this.mSurface == null || this.mVideoResId == 0) {
            return;
        }
        this.mMediaPlayer = MediaPlayer.create(getContext(), this.mVideoResId);
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.setSurface(this.mSurface);
            this.mMediaPlayer.setOnPreparedListener(this);
            this.mMediaPlayer.setOnSeekCompleteListener(this);
            this.mMediaPlayer.setOnInfoListener(this);
            float videoHeight = this.mMediaPlayer.getVideoHeight() / this.mMediaPlayer.getVideoWidth();
            if (this.mAspectRatio != videoHeight) {
                this.mAspectRatio = videoHeight;
                requestLayout();
            }
        } else {
            Log.wtf("IllustrationVideoView", "Unable to initialize media player for video view");
        }
        if (getWindowVisibility() == 0) {
            start();
        }
    }

    protected boolean shouldLoop() {
        return true;
    }

    public void release() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
        if (this.mSurface != null) {
            this.mSurface.release();
            this.mSurface = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        setVisibility(4);
        this.mSurface = new Surface(surfaceTexture);
        createMediaPlayer();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @Override
    public void start() {
        if (this.mMediaPlayer != null && !this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.pause();
        }
    }

    @Override
    public boolean isRunning() {
        return this.mMediaPlayer != null && this.mMediaPlayer.isPlaying();
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        if (i == 3) {
            setVisibility(0);
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.setLooping(shouldLoop());
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    public int getCurrentPosition() {
        if (this.mMediaPlayer == null) {
            return 0;
        }
        return this.mMediaPlayer.getCurrentPosition();
    }
}
