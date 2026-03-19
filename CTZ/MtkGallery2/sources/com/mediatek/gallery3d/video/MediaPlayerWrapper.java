package com.mediatek.gallery3d.video;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceHolder;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.MovieView;
import com.mediatek.galleryportable.VideoMetadataUtils;
import java.io.IOException;
import java.util.Map;

public class MediaPlayerWrapper implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnVideoSizeChangedListener, MovieView.SurfaceCallback {
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PREPARING = 1;
    private static final String TAG = "VP_MediaPlayerWrapper";
    private int mAudioSession;
    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private boolean mCanSetVideoSize;
    private Context mContext;
    private int mCurrentBufferPercentage;
    private int mDuration;
    private Map<String, String> mHeaders;
    private Listener mListener;
    private MediaPlayer mMediaPlayer;
    private MovieView mMovieView;
    private MultiWindowListener mMultiWindowListener;
    private boolean mOnResumed;
    private int mSeekWhenPrepared;
    private Object mSurfaceHeight;
    private SurfaceHolder mSurfaceHolder;
    private Object mSurfaceWidth;
    private Uri mUri;
    private int mVideoHeight;
    private int mVideoWidth;
    private int mCurrentState = 0;
    private int mTargetState = 0;

    public interface Listener {
        void onBufferingUpdate(MediaPlayer mediaPlayer, int i);

        void onCompletion(MediaPlayer mediaPlayer);

        boolean onError(MediaPlayer mediaPlayer, int i, int i2);

        boolean onInfo(MediaPlayer mediaPlayer, int i, int i2);

        void onPrepared(MediaPlayer mediaPlayer);

        void onSeekComplete(MediaPlayer mediaPlayer);

        void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface MultiWindowListener {
        void onSurfaceDestroyed();
    }

    public MediaPlayerWrapper(Context context, MovieView movieView) {
        this.mContext = context;
        this.mMovieView = movieView;
        initialize();
    }

    private void initialize() {
        if (this.mMovieView != null) {
            this.mMovieView.setSurfaceListener(this);
        }
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, Map<String, String> map) {
        Log.v(TAG, "setVideoURI(" + uri + ", " + map + ")");
        this.mDuration = -1;
        setResumed(true);
        this.mUri = uri;
        this.mHeaders = map;
        openVideo();
    }

    public void setResumed(boolean z) {
        Log.v(TAG, "setResumed(" + z + ") mUri=" + this.mUri + ", mOnResumed=" + this.mOnResumed);
        this.mOnResumed = z;
    }

    private void openVideo() {
        if (!this.mOnResumed || this.mUri == null || this.mSurfaceHolder == null) {
            Log.v(TAG, "openVideo, not ready for playback just yet, will try again later, mOnResumed = " + this.mOnResumed + ", mUri = " + this.mUri + ", mSurfaceHolder = " + this.mSurfaceHolder);
            return;
        }
        Log.v(TAG, "openVideo");
        release(false);
        try {
            this.mMediaPlayer = new MediaPlayer();
            this.mMovieView.register(this.mContext, this.mMediaPlayer);
            if (this.mAudioSession != 0) {
                this.mMediaPlayer.setAudioSessionId(this.mAudioSession);
            } else {
                this.mAudioSession = this.mMediaPlayer.getAudioSessionId();
            }
            this.mMediaPlayer.setOnPreparedListener(this);
            this.mMediaPlayer.setOnVideoSizeChangedListener(this);
            this.mMediaPlayer.setOnCompletionListener(this);
            this.mMediaPlayer.setOnErrorListener(this);
            this.mMediaPlayer.setOnInfoListener(this);
            this.mMediaPlayer.setOnBufferingUpdateListener(this);
            this.mMediaPlayer.setOnSeekCompleteListener(this);
            this.mMediaPlayer.setDataSource(this.mContext, this.mUri, this.mHeaders);
            this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
            this.mMediaPlayer.setAudioStreamType(3);
            this.mMediaPlayer.setScreenOnWhilePlaying(true);
            this.mMediaPlayer.prepareAsync();
            this.mCurrentBufferPercentage = 0;
            this.mCurrentState = 1;
        } catch (IOException e) {
            Log.e(TAG, "unable to open content: " + this.mUri, e);
            onError(this.mMediaPlayer, 1, 0);
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "unable to open content: " + this.mUri, e2);
            onError(this.mMediaPlayer, 1, 0);
        }
    }

    public void stop() {
        Log.v(TAG, "stop");
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
            this.mCurrentState = 0;
            this.mTargetState = 0;
        }
    }

    private void release(boolean z) {
        Log.v(TAG, "release");
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.reset();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
            this.mCurrentState = 0;
            if (z) {
                this.mTargetState = 0;
            }
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        Log.v(TAG, "onSeekComplete");
        if (this.mListener != null) {
            this.mListener.onSeekComplete(mediaPlayer);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2) {
        Log.v(TAG, "onVideoSizeChanged, width = " + i + ", height = " + i2);
        this.mVideoWidth = mediaPlayer.getVideoWidth();
        this.mVideoHeight = mediaPlayer.getVideoHeight();
        if (this.mVideoWidth != 0 && this.mVideoHeight != 0 && this.mSurfaceHolder != null) {
            this.mSurfaceHolder.setFixedSize(this.mVideoWidth, this.mVideoHeight);
        }
        if (this.mListener != null) {
            this.mListener.onVideoSizeChanged(mediaPlayer, i, i2);
        }
        if (this.mCanSetVideoSize) {
            setVideoLayout();
        }
    }

    private void setVideoLayout() {
        if (this.mMovieView != null) {
            this.mMovieView.setVideoLayout(this.mVideoWidth, this.mVideoHeight);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        Log.v(TAG, "onInfo(" + mediaPlayer + ") what: " + i + ", extra:" + i2);
        if (i == 3) {
            setVideoLayout();
            this.mCanSetVideoSize = true;
        }
        if (this.mListener != null) {
            this.mListener.onInfo(mediaPlayer, i, i2);
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.v(TAG, "onPrepared(" + mediaPlayer + ")");
        this.mCurrentState = 2;
        this.mCanPause = VideoMetadataUtils.canPause(mediaPlayer);
        this.mCanSeekBack = VideoMetadataUtils.canSeekBack(mediaPlayer);
        this.mCanSeekForward = VideoMetadataUtils.canSeekForward(mediaPlayer);
        Log.v(TAG, "onPrepared, mCanPause=" + this.mCanPause + ", mCanSeekBack = " + this.mCanSeekBack + ", mCanSeekForward = " + this.mCanSeekForward);
        if (this.mListener != null) {
            this.mListener.onPrepared(mediaPlayer);
        }
        Log.d(TAG, "onPrepared, mSeekWhenPrepared = " + this.mSeekWhenPrepared);
        if (this.mSeekWhenPrepared != 0) {
            seekTo(this.mSeekWhenPrepared);
        }
        this.mVideoWidth = mediaPlayer.getVideoWidth();
        this.mVideoHeight = mediaPlayer.getVideoHeight();
        Log.v(TAG, "onPrepared, mVideoWidth = " + this.mVideoWidth + ", mVideoHeight = " + this.mVideoHeight);
        if (this.mVideoWidth != 0 && this.mVideoHeight != 0 && this.mSurfaceHolder != null) {
            this.mSurfaceHolder.setFixedSize(this.mVideoWidth, this.mVideoHeight);
        }
        if (this.mTargetState == 3) {
            start();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Log.v(TAG, "onBufferingUpdate(" + mediaPlayer + "), percent: " + i);
        this.mCurrentBufferPercentage = i;
        if (this.mListener != null) {
            this.mListener.onBufferingUpdate(mediaPlayer, i);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.v(TAG, "onCompletion(" + mediaPlayer + ")");
        this.mCurrentState = 5;
        this.mTargetState = 5;
        if (this.mListener != null) {
            this.mListener.onCompletion(mediaPlayer);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Log.e(TAG, "onError(" + mediaPlayer + "), what = " + i + ", extra = " + i2);
        if (this.mCurrentState == -1) {
            Log.v(TAG, "current state is error, skip error: " + i + ", " + i2);
            return true;
        }
        this.mCurrentState = -1;
        this.mTargetState = -1;
        if (this.mListener != null) {
            this.mListener.onError(mediaPlayer, i, i2);
        }
        return true;
    }

    public void start() {
        Log.v(TAG, "start(), isInPlaybackState = " + isInPlaybackState());
        if (isInPlaybackState()) {
            this.mMediaPlayer.start();
            this.mCurrentState = 3;
        }
        this.mTargetState = 3;
    }

    public void pause() {
        Log.v(TAG, "pause");
        if (isInPlaybackState() && this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.pause();
            this.mCurrentState = 4;
        }
        this.mTargetState = 4;
    }

    public void suspend() {
        Log.v(TAG, "suspend");
        release(false);
    }

    public void resume() {
        Log.v(TAG, "resume");
        openVideo();
    }

    public void seekTo(int i) {
        Log.v(TAG, "seekTo " + i + ", isInPlaybackState = " + isInPlaybackState());
        if (isInPlaybackState()) {
            this.mMediaPlayer.seekTo(i);
            this.mSeekWhenPrepared = 0;
        } else {
            this.mSeekWhenPrepared = i;
        }
    }

    public int getCurrentPosition() {
        int currentPosition;
        if (this.mSeekWhenPrepared > 0) {
            currentPosition = this.mSeekWhenPrepared;
        } else if (isInPlaybackState()) {
            currentPosition = this.mMediaPlayer.getCurrentPosition();
        } else {
            currentPosition = 0;
        }
        Log.v(TAG, "getCurrentPosition() return " + currentPosition);
        return currentPosition;
    }

    public void setDuration(int i) {
        Log.v(TAG, "setDuration(" + i + ")");
        if (i > 0) {
            i = -i;
        }
        this.mDuration = i;
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            if (this.mDuration > 0) {
                return this.mDuration;
            }
            this.mDuration = this.mMediaPlayer.getDuration();
            Log.v(TAG, "getDuration from mediaplayer is " + this.mDuration);
        }
        return this.mDuration;
    }

    public void clearDuration() {
        Log.v(TAG, "clearDuration() mDuration=" + this.mDuration);
        this.mDuration = -1;
    }

    public void clearSeek() {
        Log.v(TAG, "clearSeek() mSeekWhenPrepared=" + this.mSeekWhenPrepared);
        this.mSeekWhenPrepared = 0;
    }

    public int getBufferPercentage() {
        if (this.mMediaPlayer != null) {
            return this.mCurrentBufferPercentage;
        }
        return 0;
    }

    public boolean isPlaying() {
        boolean z = isInPlaybackState() && this.mMediaPlayer.isPlaying();
        Log.v(TAG, "isPlaying = " + z);
        return z;
    }

    public boolean isCurrentPlaying() {
        Log.v(TAG, "isCurrentPlaying() mCurrentState=" + this.mCurrentState);
        return this.mCurrentState == 3;
    }

    public boolean isInPlaybackState() {
        Log.d(TAG, "isInPlaybackState: " + this.mCurrentState);
        return (this.mMediaPlayer == null || this.mCurrentState == -1 || this.mCurrentState == 0 || this.mCurrentState == 1) ? false : true;
    }

    public boolean canPause() {
        Log.v(TAG, "canPause: " + this.mCanPause);
        return this.mCanPause;
    }

    public boolean canSeekBackward() {
        Log.v(TAG, "mCanSeekBack: " + this.mCanSeekBack);
        return this.mCanSeekBack;
    }

    public boolean canSeekForward() {
        Log.v(TAG, "mCanSeekForward: " + this.mCanSeekForward);
        return this.mCanSeekForward;
    }

    public int getAudioSessionId() {
        if (this.mAudioSession == 0) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            this.mAudioSession = mediaPlayer.getAudioSessionId();
            mediaPlayer.release();
        }
        return this.mAudioSession;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
        Log.v(TAG, "onSurfaceCreated(" + surfaceHolder + ")");
        this.mSurfaceHolder = surfaceHolder;
        openVideo();
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        Log.v(TAG, "surfaceChanged(" + surfaceHolder + ", " + i + ", " + i2 + ", " + i3 + ")");
        StringBuilder sb = new StringBuilder();
        sb.append("surfaceChanged() mMediaPlayer=");
        sb.append(this.mMediaPlayer);
        sb.append(", mTargetState=");
        sb.append(this.mTargetState);
        sb.append(", mVideoWidth=");
        sb.append(this.mVideoWidth);
        sb.append(", mVideoHeight=");
        sb.append(this.mVideoHeight);
        Log.v(TAG, sb.toString());
        this.mSurfaceWidth = Integer.valueOf(i2);
        this.mSurfaceHeight = Integer.valueOf(i3);
        boolean z = false;
        boolean z2 = this.mTargetState == 3;
        if (this.mVideoWidth == i2 && this.mVideoHeight == i3) {
            z = true;
        }
        if (this.mMediaPlayer != null && z2 && z && this.mCurrentState != 3) {
            if (this.mSeekWhenPrepared != 0) {
                seekTo(this.mSeekWhenPrepared);
            }
            Log.v(TAG, "surfaceChanged() start()");
            start();
        }
    }

    @Override
    public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.v(TAG, "surfaceDestroyed(" + surfaceHolder + ")");
        if (this.mMultiWindowListener != null) {
            this.mMultiWindowListener.onSurfaceDestroyed();
        }
        this.mSurfaceHolder = null;
        this.mCanSetVideoSize = false;
        release(true);
    }

    public void setMultiWindowListener(MultiWindowListener multiWindowListener) {
        this.mMultiWindowListener = multiWindowListener;
    }
}
