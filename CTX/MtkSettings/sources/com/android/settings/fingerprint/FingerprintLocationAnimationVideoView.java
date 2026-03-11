package com.android.settings.fingerprint;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import com.android.settings.R;

public class FingerprintLocationAnimationVideoView extends TextureView implements FingerprintFindSensorAnimation {
    protected float mAspect;
    protected MediaPlayer mMediaPlayer;

    public FingerprintLocationAnimationVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAspect = 1.0f;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, View.MeasureSpec.makeMeasureSpec(Math.round(this.mAspect * View.MeasureSpec.getSize(i)), 1073741824));
    }

    protected Uri getFingerprintLocationAnimation() {
        return resourceEntryToUri(getContext(), R.raw.fingerprint_location_animation);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            private SurfaceTexture mTextureToDestroy = null;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
                FingerprintLocationAnimationVideoView.this.setVisibility(4);
                Uri fingerprintLocationAnimation = FingerprintLocationAnimationVideoView.this.getFingerprintLocationAnimation();
                if (FingerprintLocationAnimationVideoView.this.mMediaPlayer != null) {
                    FingerprintLocationAnimationVideoView.this.mMediaPlayer.release();
                }
                if (this.mTextureToDestroy != null) {
                    this.mTextureToDestroy.release();
                    this.mTextureToDestroy = null;
                }
                FingerprintLocationAnimationVideoView.this.mMediaPlayer = FingerprintLocationAnimationVideoView.this.createMediaPlayer(FingerprintLocationAnimationVideoView.this.mContext, fingerprintLocationAnimation);
                if (FingerprintLocationAnimationVideoView.this.mMediaPlayer == null) {
                    return;
                }
                FingerprintLocationAnimationVideoView.this.mMediaPlayer.setSurface(new Surface(surfaceTexture));
                FingerprintLocationAnimationVideoView.this.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(true);
                    }
                });
                FingerprintLocationAnimationVideoView.this.mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mediaPlayer, int i3, int i4) {
                        if (i3 == 3) {
                            FingerprintLocationAnimationVideoView.this.setVisibility(0);
                        }
                        return false;
                    }
                });
                FingerprintLocationAnimationVideoView.this.mAspect = FingerprintLocationAnimationVideoView.this.mMediaPlayer.getVideoHeight() / FingerprintLocationAnimationVideoView.this.mMediaPlayer.getVideoWidth();
                FingerprintLocationAnimationVideoView.this.requestLayout();
                FingerprintLocationAnimationVideoView.this.startAnimation();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                this.mTextureToDestroy = surfaceTexture;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        });
    }

    MediaPlayer createMediaPlayer(Context context, Uri uri) {
        return MediaPlayer.create(this.mContext, uri);
    }

    protected static Uri resourceEntryToUri(Context context, int i) {
        Resources resources = context.getResources();
        return Uri.parse("android.resource://" + resources.getResourcePackageName(i) + '/' + resources.getResourceTypeName(i) + '/' + resources.getResourceEntryName(i));
    }

    @Override
    public void startAnimation() {
        if (this.mMediaPlayer != null && !this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
    }

    @Override
    public void pauseAnimation() {
        if (this.mMediaPlayer != null && this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.pause();
        }
    }
}
