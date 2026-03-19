package android.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Cea708CaptionRenderer;
import android.media.ClosedCaptionRenderer;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.media.SubtitleController;
import android.media.SubtitleTrack;
import android.media.TtmlRenderer;
import android.media.WebVttRenderer;
import android.net.Uri;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Vector;

public class VideoView extends SurfaceView implements MediaController.MediaPlayerControl, SubtitleController.Anchor {
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PREPARING = 1;
    private static final String TAG = "VideoView";
    private AudioAttributes mAudioAttributes;
    private int mAudioFocusType;
    private AudioManager mAudioManager;
    private int mAudioSession;
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener;
    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private MediaPlayer.OnCompletionListener mCompletionListener;
    private int mCurrentBufferPercentage;
    private int mCurrentState;
    private MediaPlayer.OnErrorListener mErrorListener;
    private Map<String, String> mHeaders;
    private MediaPlayer.OnInfoListener mInfoListener;
    private MediaController mMediaController;
    private MediaPlayer mMediaPlayer;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;
    private MediaPlayer.OnInfoListener mOnInfoListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private final Vector<Pair<InputStream, MediaFormat>> mPendingSubtitleTracks;
    MediaPlayer.OnPreparedListener mPreparedListener;
    SurfaceHolder.Callback mSHCallback;
    private int mSeekWhenPrepared;
    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener;
    private SubtitleTrack.RenderingWidget mSubtitleWidget;
    private SubtitleTrack.RenderingWidget.OnChangedListener mSubtitlesChangedListener;
    private int mSurfaceHeight;
    private SurfaceHolder mSurfaceHolder;
    private int mSurfaceWidth;
    private int mTargetState;
    private Uri mUri;
    private int mVideoHeight;
    private int mVideoWidth;

    public VideoView(Context context) {
        this(context, null);
    }

    public VideoView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public VideoView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public VideoView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mPendingSubtitleTracks = new Vector<>();
        this.mCurrentState = 0;
        this.mTargetState = 0;
        this.mSurfaceHolder = null;
        this.mMediaPlayer = null;
        this.mAudioFocusType = 1;
        this.mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i3, int i4) {
                VideoView.this.mVideoWidth = mediaPlayer.getVideoWidth();
                VideoView.this.mVideoHeight = mediaPlayer.getVideoHeight();
                if (VideoView.this.mVideoWidth != 0 && VideoView.this.mVideoHeight != 0) {
                    VideoView.this.getHolder().setFixedSize(VideoView.this.mVideoWidth, VideoView.this.mVideoHeight);
                    VideoView.this.requestLayout();
                }
            }
        };
        this.mPreparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                VideoView.this.mCurrentState = 2;
                Metadata metadata = mediaPlayer.getMetadata(false, false);
                if (metadata == null) {
                    VideoView.this.mCanPause = VideoView.this.mCanSeekBack = VideoView.this.mCanSeekForward = true;
                } else {
                    VideoView.this.mCanPause = !metadata.has(1) || metadata.getBoolean(1);
                    VideoView.this.mCanSeekBack = !metadata.has(2) || metadata.getBoolean(2);
                    VideoView.this.mCanSeekForward = !metadata.has(3) || metadata.getBoolean(3);
                }
                if (VideoView.this.mOnPreparedListener != null) {
                    VideoView.this.mOnPreparedListener.onPrepared(VideoView.this.mMediaPlayer);
                }
                if (VideoView.this.mMediaController != null) {
                    VideoView.this.mMediaController.setEnabled(true);
                }
                VideoView.this.mVideoWidth = mediaPlayer.getVideoWidth();
                VideoView.this.mVideoHeight = mediaPlayer.getVideoHeight();
                int i3 = VideoView.this.mSeekWhenPrepared;
                if (i3 != 0) {
                    VideoView.this.seekTo(i3);
                }
                if (VideoView.this.mVideoWidth != 0 && VideoView.this.mVideoHeight != 0) {
                    VideoView.this.getHolder().setFixedSize(VideoView.this.mVideoWidth, VideoView.this.mVideoHeight);
                    if (VideoView.this.mSurfaceWidth == VideoView.this.mVideoWidth && VideoView.this.mSurfaceHeight == VideoView.this.mVideoHeight) {
                        if (VideoView.this.mTargetState == 3) {
                            VideoView.this.start();
                            if (VideoView.this.mMediaController != null) {
                                VideoView.this.mMediaController.show();
                                return;
                            }
                            return;
                        }
                        if (!VideoView.this.isPlaying()) {
                            if ((i3 != 0 || VideoView.this.getCurrentPosition() > 0) && VideoView.this.mMediaController != null) {
                                VideoView.this.mMediaController.show(0);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
                if (VideoView.this.mTargetState == 3) {
                    VideoView.this.start();
                }
            }
        };
        this.mCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                VideoView.this.mCurrentState = 5;
                VideoView.this.mTargetState = 5;
                if (VideoView.this.mMediaController != null) {
                    VideoView.this.mMediaController.hide();
                }
                if (VideoView.this.mOnCompletionListener != null) {
                    VideoView.this.mOnCompletionListener.onCompletion(VideoView.this.mMediaPlayer);
                }
                if (VideoView.this.mAudioFocusType != 0) {
                    VideoView.this.mAudioManager.abandonAudioFocus(null);
                }
            }
        };
        this.mInfoListener = new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int i3, int i4) {
                if (VideoView.this.mOnInfoListener != null) {
                    VideoView.this.mOnInfoListener.onInfo(mediaPlayer, i3, i4);
                    return true;
                }
                return true;
            }
        };
        this.mErrorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i3, int i4) {
                int i5;
                Log.d(VideoView.TAG, "Error: " + i3 + "," + i4);
                VideoView.this.mCurrentState = -1;
                VideoView.this.mTargetState = -1;
                if (VideoView.this.mMediaController != null) {
                    VideoView.this.mMediaController.hide();
                }
                if ((VideoView.this.mOnErrorListener == null || !VideoView.this.mOnErrorListener.onError(VideoView.this.mMediaPlayer, i3, i4)) && VideoView.this.getWindowToken() != null) {
                    VideoView.this.mContext.getResources();
                    if (i3 == 200) {
                        i5 = 17039381;
                    } else {
                        i5 = 17039377;
                    }
                    new AlertDialog.Builder(VideoView.this.mContext).setMessage(i5).setPositiveButton(17039376, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i6) {
                            if (VideoView.this.mOnCompletionListener != null) {
                                VideoView.this.mOnCompletionListener.onCompletion(VideoView.this.mMediaPlayer);
                            }
                        }
                    }).setCancelable(false).show();
                }
                return true;
            }
        };
        this.mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i3) {
                VideoView.this.mCurrentBufferPercentage = i3;
            }
        };
        this.mSHCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i3, int i4, int i5) {
                VideoView.this.mSurfaceWidth = i4;
                VideoView.this.mSurfaceHeight = i5;
                boolean z = false;
                boolean z2 = VideoView.this.mTargetState == 3;
                if (VideoView.this.mVideoWidth == i4 && VideoView.this.mVideoHeight == i5) {
                    z = true;
                }
                if (VideoView.this.mMediaPlayer != null && z2 && z) {
                    if (VideoView.this.mSeekWhenPrepared != 0) {
                        VideoView.this.seekTo(VideoView.this.mSeekWhenPrepared);
                    }
                    VideoView.this.start();
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                VideoView.this.mSurfaceHolder = surfaceHolder;
                VideoView.this.openVideo();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                VideoView.this.mSurfaceHolder = null;
                if (VideoView.this.mMediaController != null) {
                    VideoView.this.mMediaController.hide();
                }
                VideoView.this.release(true);
            }
        };
        this.mVideoWidth = 0;
        this.mVideoHeight = 0;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mAudioAttributes = new AudioAttributes.Builder().setUsage(1).setContentType(3).build();
        getHolder().addCallback(this.mSHCallback);
        getHolder().setType(3);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        this.mCurrentState = 0;
        this.mTargetState = 0;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size;
        int size2;
        int i3;
        int defaultSize = getDefaultSize(this.mVideoWidth, i);
        int defaultSize2 = getDefaultSize(this.mVideoHeight, i2);
        if (this.mVideoWidth > 0 && this.mVideoHeight > 0) {
            int mode = View.MeasureSpec.getMode(i);
            size = View.MeasureSpec.getSize(i);
            int mode2 = View.MeasureSpec.getMode(i2);
            size2 = View.MeasureSpec.getSize(i2);
            if (mode == 1073741824 && mode2 == 1073741824) {
                if (this.mVideoWidth * size2 < this.mVideoHeight * size) {
                    size = (this.mVideoWidth * size2) / this.mVideoHeight;
                } else if (this.mVideoWidth * size2 > this.mVideoHeight * size) {
                    defaultSize2 = (this.mVideoHeight * size) / this.mVideoWidth;
                }
            } else if (mode == 1073741824) {
                int i4 = (this.mVideoHeight * size) / this.mVideoWidth;
                if (mode2 != Integer.MIN_VALUE || i4 <= size2) {
                    size2 = i4;
                }
            } else {
                if (mode2 == 1073741824) {
                    i3 = (this.mVideoWidth * size2) / this.mVideoHeight;
                    if (mode != Integer.MIN_VALUE || i3 <= size) {
                    }
                } else {
                    int i5 = this.mVideoWidth;
                    int i6 = this.mVideoHeight;
                    if (mode2 != Integer.MIN_VALUE || i6 <= size2) {
                        i3 = i5;
                        size2 = i6;
                    } else {
                        i3 = (this.mVideoWidth * size2) / this.mVideoHeight;
                    }
                    if (mode == Integer.MIN_VALUE && i3 > size) {
                        defaultSize2 = (this.mVideoHeight * size) / this.mVideoWidth;
                    }
                }
                size = i3;
            }
            setMeasuredDimension(size, size2);
        }
        size = defaultSize;
        size2 = defaultSize2;
        setMeasuredDimension(size, size2);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return VideoView.class.getName();
    }

    public int resolveAdjustedSize(int i, int i2) {
        return getDefaultSize(i, i2);
    }

    public void setVideoPath(String str) {
        setVideoURI(Uri.parse(str));
    }

    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, Map<String, String> map) {
        this.mUri = uri;
        this.mHeaders = map;
        this.mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void setAudioFocusRequest(int i) {
        if (i != 0 && i != 1 && i != 2 && i != 3 && i != 4) {
            throw new IllegalArgumentException("Illegal audio focus type " + i);
        }
        this.mAudioFocusType = i;
    }

    public void setAudioAttributes(AudioAttributes audioAttributes) {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        this.mAudioAttributes = audioAttributes;
    }

    public void addSubtitleSource(InputStream inputStream, MediaFormat mediaFormat) {
        if (this.mMediaPlayer == null) {
            this.mPendingSubtitleTracks.add(Pair.create(inputStream, mediaFormat));
            return;
        }
        try {
            this.mMediaPlayer.addSubtitleSource(inputStream, mediaFormat);
        } catch (IllegalStateException e) {
            this.mInfoListener.onInfo(this.mMediaPlayer, 901, 0);
        }
    }

    public void stopPlayback() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
            this.mCurrentState = 0;
            this.mTargetState = 0;
            this.mAudioManager.abandonAudioFocus(null);
        }
    }

    private void openVideo() {
        if (this.mUri == null || this.mSurfaceHolder == null) {
            return;
        }
        release(false);
        if (this.mAudioFocusType != 0) {
            this.mAudioManager.requestAudioFocus(null, this.mAudioAttributes, this.mAudioFocusType, 0);
        }
        try {
            this.mMediaPlayer = new MediaPlayer();
            Context context = getContext();
            SubtitleController subtitleController = new SubtitleController(context, this.mMediaPlayer.getMediaTimeProvider(), this.mMediaPlayer);
            subtitleController.registerRenderer(new WebVttRenderer(context));
            subtitleController.registerRenderer(new TtmlRenderer(context));
            subtitleController.registerRenderer(new Cea708CaptionRenderer(context));
            subtitleController.registerRenderer(new ClosedCaptionRenderer(context));
            this.mMediaPlayer.setSubtitleAnchor(subtitleController, this);
            if (this.mAudioSession != 0) {
                this.mMediaPlayer.setAudioSessionId(this.mAudioSession);
            } else {
                this.mAudioSession = this.mMediaPlayer.getAudioSessionId();
            }
            this.mMediaPlayer.setOnPreparedListener(this.mPreparedListener);
            this.mMediaPlayer.setOnVideoSizeChangedListener(this.mSizeChangedListener);
            this.mMediaPlayer.setOnCompletionListener(this.mCompletionListener);
            this.mMediaPlayer.setOnErrorListener(this.mErrorListener);
            this.mMediaPlayer.setOnInfoListener(this.mInfoListener);
            this.mMediaPlayer.setOnBufferingUpdateListener(this.mBufferingUpdateListener);
            this.mCurrentBufferPercentage = 0;
            this.mMediaPlayer.setDataSource(this.mContext, this.mUri, this.mHeaders);
            this.mMediaPlayer.setDisplay(this.mSurfaceHolder);
            this.mMediaPlayer.setAudioAttributes(this.mAudioAttributes);
            this.mMediaPlayer.setScreenOnWhilePlaying(true);
            this.mMediaPlayer.prepareAsync();
            for (Pair<InputStream, MediaFormat> pair : this.mPendingSubtitleTracks) {
                try {
                    this.mMediaPlayer.addSubtitleSource(pair.first, pair.second);
                } catch (IllegalStateException e) {
                    this.mInfoListener.onInfo(this.mMediaPlayer, 901, 0);
                }
            }
            this.mCurrentState = 1;
            attachMediaController();
        } catch (IOException e2) {
            Log.w(TAG, "Unable to open content: " + this.mUri, e2);
            this.mCurrentState = -1;
            this.mTargetState = -1;
            this.mErrorListener.onError(this.mMediaPlayer, 1, 0);
        } catch (IllegalArgumentException e3) {
            Log.w(TAG, "Unable to open content: " + this.mUri, e3);
            this.mCurrentState = -1;
            this.mTargetState = -1;
            this.mErrorListener.onError(this.mMediaPlayer, 1, 0);
        } finally {
            this.mPendingSubtitleTracks.clear();
        }
    }

    public void setMediaController(MediaController mediaController) {
        if (this.mMediaController != null) {
            this.mMediaController.hide();
        }
        this.mMediaController = mediaController;
        attachMediaController();
    }

    private void attachMediaController() {
        View view;
        if (this.mMediaPlayer != null && this.mMediaController != null) {
            this.mMediaController.setMediaPlayer(this);
            if (getParent() instanceof View) {
                view = (View) getParent();
            } else {
                view = this;
            }
            this.mMediaController.setAnchorView(view);
            this.mMediaController.setEnabled(isInPlaybackState());
        }
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = onPreparedListener;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        this.mOnCompletionListener = onCompletionListener;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener onInfoListener) {
        this.mOnInfoListener = onInfoListener;
    }

    private void release(boolean z) {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.reset();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
            this.mPendingSubtitleTracks.clear();
            this.mCurrentState = 0;
            if (z) {
                this.mTargetState = 0;
            }
            if (this.mAudioFocusType != 0) {
                this.mAudioManager.abandonAudioFocus(null);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 && isInPlaybackState() && this.mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 && isInPlaybackState() && this.mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return super.onTrackballEvent(motionEvent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        boolean z;
        if (i == 4 || i == 24 || i == 25 || i == 164 || i == 82 || i == 5 || i == 6) {
            z = false;
        } else {
            z = true;
        }
        if (isInPlaybackState() && z && this.mMediaController != null) {
            if (i == 79 || i == 85) {
                if (this.mMediaPlayer.isPlaying()) {
                    pause();
                    this.mMediaController.show();
                } else {
                    start();
                    this.mMediaController.hide();
                }
                return true;
            }
            if (i == 126) {
                if (!this.mMediaPlayer.isPlaying()) {
                    start();
                    this.mMediaController.hide();
                }
                return true;
            }
            if (i == 86 || i == 127) {
                if (this.mMediaPlayer.isPlaying()) {
                    pause();
                    this.mMediaController.show();
                }
                return true;
            }
            toggleMediaControlsVisiblity();
        }
        return super.onKeyDown(i, keyEvent);
    }

    private void toggleMediaControlsVisiblity() {
        if (this.mMediaController.isShowing()) {
            this.mMediaController.hide();
        } else {
            this.mMediaController.show();
        }
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            this.mMediaPlayer.start();
            this.mCurrentState = 3;
        }
        this.mTargetState = 3;
    }

    @Override
    public void pause() {
        if (isInPlaybackState() && this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.pause();
            this.mCurrentState = 4;
        }
        this.mTargetState = 4;
    }

    public void suspend() {
        release(false);
    }

    public void resume() {
        openVideo();
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return this.mMediaPlayer.getDuration();
        }
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return this.mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        if (isInPlaybackState()) {
            this.mMediaPlayer.seekTo(i);
            this.mSeekWhenPrepared = 0;
        } else {
            this.mSeekWhenPrepared = i;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && this.mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (this.mMediaPlayer != null) {
            return this.mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (this.mMediaPlayer == null || this.mCurrentState == -1 || this.mCurrentState == 0 || this.mCurrentState == 1) ? false : true;
    }

    @Override
    public boolean canPause() {
        return this.mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return this.mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return this.mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        if (this.mAudioSession == 0) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            this.mAudioSession = mediaPlayer.getAudioSessionId();
            mediaPlayer.release();
        }
        return this.mAudioSession;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mSubtitleWidget != null) {
            this.mSubtitleWidget.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mSubtitleWidget != null) {
            this.mSubtitleWidget.onDetachedFromWindow();
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mSubtitleWidget != null) {
            measureAndLayoutSubtitleWidget();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mSubtitleWidget != null) {
            int iSave = canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            this.mSubtitleWidget.draw(canvas);
            canvas.restoreToCount(iSave);
        }
    }

    private void measureAndLayoutSubtitleWidget() {
        this.mSubtitleWidget.setSize((getWidth() - getPaddingLeft()) - getPaddingRight(), (getHeight() - getPaddingTop()) - getPaddingBottom());
    }

    @Override
    public void setSubtitleWidget(SubtitleTrack.RenderingWidget renderingWidget) {
        if (this.mSubtitleWidget == renderingWidget) {
            return;
        }
        boolean zIsAttachedToWindow = isAttachedToWindow();
        if (this.mSubtitleWidget != null) {
            if (zIsAttachedToWindow) {
                this.mSubtitleWidget.onDetachedFromWindow();
            }
            this.mSubtitleWidget.setOnChangedListener(null);
        }
        this.mSubtitleWidget = renderingWidget;
        if (renderingWidget != null) {
            if (this.mSubtitlesChangedListener == null) {
                this.mSubtitlesChangedListener = new SubtitleTrack.RenderingWidget.OnChangedListener() {
                    @Override
                    public void onChanged(SubtitleTrack.RenderingWidget renderingWidget2) {
                        VideoView.this.invalidate();
                    }
                };
            }
            setWillNotDraw(false);
            renderingWidget.setOnChangedListener(this.mSubtitlesChangedListener);
            if (zIsAttachedToWindow) {
                renderingWidget.onAttachedToWindow();
                requestLayout();
            }
        } else {
            setWillNotDraw(true);
        }
        invalidate();
    }

    @Override
    public Looper getSubtitleLooper() {
        return Looper.getMainLooper();
    }
}
