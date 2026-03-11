package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.R;

public class VideoPreference extends Preference {
    boolean mAnimationAvailable;
    private float mAspectRadio;
    private final Context mContext;
    MediaPlayer mMediaPlayer;
    private int mPreviewResource;
    private Uri mVideoPath;
    private boolean mVideoPaused;
    private boolean mVideoReady;

    public VideoPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAspectRadio = 1.0f;
        this.mContext = context;
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.VideoPreference, 0, 0);
        try {
            try {
                this.mVideoPath = new Uri.Builder().scheme("android.resource").authority(context.getPackageName()).appendPath(String.valueOf(typedArrayObtainStyledAttributes.getResourceId(0, 0))).build();
                this.mMediaPlayer = MediaPlayer.create(this.mContext, this.mVideoPath);
                if (this.mMediaPlayer != null && this.mMediaPlayer.getDuration() > 0) {
                    setVisible(true);
                    setLayoutResource(R.layout.video_preference);
                    this.mPreviewResource = typedArrayObtainStyledAttributes.getResourceId(1, 0);
                    this.mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public final void onSeekComplete(MediaPlayer mediaPlayer) {
                            this.f$0.mVideoReady = true;
                        }
                    });
                    this.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public final void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.setLooping(true);
                        }
                    });
                    this.mAnimationAvailable = true;
                    updateAspectRatio();
                } else {
                    setVisible(false);
                }
            } catch (Exception e) {
                Log.w("VideoPreference", "Animation resource not found. Will not show animation.");
            }
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (!this.mAnimationAvailable) {
            return;
        }
        TextureView textureView = (TextureView) preferenceViewHolder.findViewById(R.id.video_texture_view);
        final ImageView imageView = (ImageView) preferenceViewHolder.findViewById(R.id.video_preview_image);
        final ImageView imageView2 = (ImageView) preferenceViewHolder.findViewById(R.id.video_play_button);
        AspectRatioFrameLayout aspectRatioFrameLayout = (AspectRatioFrameLayout) preferenceViewHolder.findViewById(R.id.video_container);
        imageView.setImageResource(this.mPreviewResource);
        aspectRatioFrameLayout.setAspectRatio(this.mAspectRadio);
        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                VideoPreference.lambda$onBindViewHolder$2(this.f$0, imageView2, view);
            }
        });
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
                if (VideoPreference.this.mMediaPlayer != null) {
                    VideoPreference.this.mMediaPlayer.setSurface(new Surface(surfaceTexture));
                    VideoPreference.this.mVideoReady = false;
                    VideoPreference.this.mMediaPlayer.seekTo(0);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                imageView.setVisibility(0);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                if (VideoPreference.this.mVideoReady) {
                    if (imageView.getVisibility() == 0) {
                        imageView.setVisibility(8);
                    }
                    if (!VideoPreference.this.mVideoPaused && VideoPreference.this.mMediaPlayer != null && !VideoPreference.this.mMediaPlayer.isPlaying()) {
                        VideoPreference.this.mMediaPlayer.start();
                        imageView2.setVisibility(8);
                    }
                }
                if (VideoPreference.this.mMediaPlayer != null && !VideoPreference.this.mMediaPlayer.isPlaying() && imageView2.getVisibility() != 0) {
                    imageView2.setVisibility(0);
                }
            }
        });
    }

    public static void lambda$onBindViewHolder$2(VideoPreference videoPreference, ImageView imageView, View view) {
        if (videoPreference.mMediaPlayer != null) {
            if (videoPreference.mMediaPlayer.isPlaying()) {
                videoPreference.mMediaPlayer.pause();
                imageView.setVisibility(0);
                videoPreference.mVideoPaused = true;
            } else {
                videoPreference.mMediaPlayer.start();
                imageView.setVisibility(8);
                videoPreference.mVideoPaused = false;
            }
        }
    }

    @Override
    public void onDetached() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer.reset();
            this.mMediaPlayer.release();
        }
        super.onDetached();
    }

    public void onViewVisible(boolean z) {
        this.mVideoPaused = z;
        if (this.mVideoReady && this.mMediaPlayer != null && !this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.seekTo(0);
        }
    }

    public void onViewInvisible() {
        if (this.mMediaPlayer != null && this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.pause();
        }
    }

    public boolean isVideoPaused() {
        return this.mVideoPaused;
    }

    void updateAspectRatio() {
        this.mAspectRadio = this.mMediaPlayer.getVideoWidth() / this.mMediaPlayer.getVideoHeight();
    }
}
