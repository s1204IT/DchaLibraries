package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;
import com.android.gallery3d.R;
import com.android.gallery3d.app.ControllerOverlay;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.util.GalleryUtils;

public class MoviePlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, ControllerOverlay.Listener {
    private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
    private AudioManager mAudioManager;
    private final Bookmarker mBookmarker;
    private Context mContext;
    private final MovieControllerOverlay mController;
    private boolean mDragging;
    private boolean mHasPaused;
    private long mResumeableTime;
    private final View mRootView;
    private boolean mShowing;
    private final Uri mUri;
    private int mVideoPosition;
    private final VideoView mVideoView;
    private Virtualizer mVirtualizer;
    private final Handler mHandler = new Handler();
    private int mLastSystemUiVis = 0;
    private int mSeekMuteCnt = 0;
    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            if (MoviePlayer.this.mVideoView.isPlaying()) {
                MoviePlayer.this.mController.showPlaying();
            } else {
                MoviePlayer.this.mHandler.postDelayed(MoviePlayer.this.mPlayingChecker, 250L);
            }
        }
    };
    private final Runnable mStartMuteAudio = new Runnable() {
        @Override
        public void run() {
            MoviePlayer.access$408(MoviePlayer.this);
            MoviePlayer.this.mAudioManager.setStreamMute(3, true);
            Log.w("MoviePlayer", "Start mute audio, mSeekMuteCnt = " + MoviePlayer.this.mSeekMuteCnt);
        }
    };
    private final Runnable mStopMuteAudio = new Runnable() {
        @Override
        public void run() {
            if (MoviePlayer.this.mSeekMuteCnt > 0) {
                MoviePlayer.access$410(MoviePlayer.this);
                MoviePlayer.this.mAudioManager.setStreamMute(3, false);
                Log.w("MoviePlayer", "Stop mute audio, mSeekMuteCnt = " + MoviePlayer.this.mSeekMuteCnt);
            }
        }
    };
    private final Runnable mRemoveBackground = new Runnable() {
        @Override
        public void run() {
            MoviePlayer.this.mRootView.setBackgroundDrawable(null);
        }
    };
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = MoviePlayer.this.setProgress();
            MoviePlayer.this.mHandler.postDelayed(MoviePlayer.this.mProgressChecker, 1000 - (pos % 1000));
        }
    };

    static int access$408(MoviePlayer x0) {
        int i = x0.mSeekMuteCnt;
        x0.mSeekMuteCnt = i + 1;
        return i;
    }

    static int access$410(MoviePlayer x0) {
        int i = x0.mSeekMuteCnt;
        x0.mSeekMuteCnt = i - 1;
        return i;
    }

    public MoviePlayer(View rootView, MovieActivity movieActivity, Uri videoUri, Bundle savedInstance, boolean canReplay) {
        this.mResumeableTime = Long.MAX_VALUE;
        this.mVideoPosition = 0;
        this.mHasPaused = false;
        this.mContext = movieActivity.getApplicationContext();
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mRootView = rootView;
        this.mVideoView = (VideoView) rootView.findViewById(R.id.surface_view);
        this.mBookmarker = new Bookmarker(movieActivity);
        this.mUri = videoUri;
        this.mController = new MovieControllerOverlay(this.mContext);
        ((ViewGroup) rootView).addView(this.mController.getView());
        this.mController.setListener(this);
        this.mController.setCanReplay(canReplay);
        this.mVideoView.setOnErrorListener(this);
        this.mVideoView.setOnCompletionListener(this);
        this.mVideoView.setVideoURI(this.mUri);
        Intent ai = movieActivity.getIntent();
        boolean virtualize = ai.getBooleanExtra("virtualize", false);
        if (virtualize) {
            int session = this.mVideoView.getAudioSessionId();
            if (session != 0) {
                this.mVirtualizer = new Virtualizer(0, session);
                this.mVirtualizer.setEnabled(true);
            } else {
                Log.w("MoviePlayer", "no audio session to virtualize");
            }
        }
        this.mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MoviePlayer.this.mController.show();
                return true;
            }
        });
        this.mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                if (!MoviePlayer.this.mVideoView.canSeekForward() || !MoviePlayer.this.mVideoView.canSeekBackward()) {
                    MoviePlayer.this.mController.setSeekable(false);
                } else {
                    MoviePlayer.this.mController.setSeekable(true);
                }
                MoviePlayer.this.setProgress();
            }
        });
        this.mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                MoviePlayer.this.mVideoView.setVisibility(0);
            }
        }, 500L);
        setOnSystemUiVisibilityChangeListener();
        showSystemUi(false);
        this.mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
        this.mAudioBecomingNoisyReceiver.register();
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        movieActivity.sendBroadcast(i);
        if (savedInstance != null) {
            this.mVideoPosition = savedInstance.getInt("video-position", 0);
            this.mResumeableTime = savedInstance.getLong("resumeable-timeout", Long.MAX_VALUE);
            this.mVideoView.start();
            this.mVideoView.suspend();
            this.mHasPaused = true;
            return;
        }
        Integer bookmark = this.mBookmarker.getBookmark(this.mUri);
        if (bookmark != null) {
            showResumeDialog(movieActivity, bookmark.intValue());
        } else {
            startVideo();
        }
    }

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private void setOnSystemUiVisibilityChangeListener() {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) {
            this.mVideoView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    int diff = MoviePlayer.this.mLastSystemUiVis ^ visibility;
                    MoviePlayer.this.mLastSystemUiVis = visibility;
                    if ((diff & 2) == 0 || (visibility & 2) != 0) {
                        MoviePlayer.this.mHandler.removeCallbacks(MoviePlayer.this.mRemoveBackground);
                        MoviePlayer.this.mHandler.postDelayed(MoviePlayer.this.mRemoveBackground, 1000L);
                    } else {
                        MoviePlayer.this.mController.show();
                        MoviePlayer.this.mHandler.removeCallbacks(MoviePlayer.this.mRemoveBackground);
                        MoviePlayer.this.mRootView.setBackgroundColor(-16777216);
                    }
                }
            });
        }
    }

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private void showSystemUi(boolean visible) {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            int flag = 1792;
            if (!visible) {
                flag = 1792 | 7;
            }
            this.mVideoView.setSystemUiVisibility(flag);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("video-position", this.mVideoPosition);
        outState.putLong("resumeable-timeout", this.mResumeableTime);
    }

    private void showResumeDialog(Context context, final int bookmark) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.resume_playing_title);
        builder.setMessage(String.format(context.getString(R.string.resume_playing_message), GalleryUtils.formatDuration(context, bookmark / 1000)));
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                MoviePlayer.this.onCompletion();
            }
        });
        builder.setPositiveButton(R.string.resume_playing_resume, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MoviePlayer.this.mVideoView.seekTo(bookmark);
                MoviePlayer.this.startVideo();
            }
        });
        builder.setNegativeButton(R.string.resume_playing_restart, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MoviePlayer.this.startVideo();
            }
        });
        builder.show();
    }

    public void onPause() {
        this.mHasPaused = true;
        this.mHandler.removeCallbacksAndMessages(null);
        this.mVideoPosition = this.mVideoView.getCurrentPosition();
        this.mBookmarker.setBookmark(this.mUri, this.mVideoPosition, this.mVideoView.getDuration());
        this.mVideoView.suspend();
        this.mResumeableTime = System.currentTimeMillis() + 180000;
        while (this.mSeekMuteCnt != 0) {
            this.mSeekMuteCnt--;
            this.mAudioManager.setStreamMute(3, false);
            Log.w("MoviePlayer", "Stop mute audio in onPause(), mSeekMuteCnt = " + this.mSeekMuteCnt);
        }
    }

    public void onResume() {
        if (this.mHasPaused) {
            this.mVideoView.seekTo(this.mVideoPosition);
            this.mVideoView.resume();
            if (System.currentTimeMillis() > this.mResumeableTime) {
                pauseVideo();
            }
        }
        this.mHandler.post(this.mProgressChecker);
    }

    public void onDestroy() {
        if (this.mVirtualizer != null) {
            this.mVirtualizer.release();
            this.mVirtualizer = null;
        }
        this.mVideoView.stopPlayback();
        this.mAudioBecomingNoisyReceiver.unregister();
    }

    private int setProgress() {
        if (this.mDragging || !this.mShowing) {
            return 0;
        }
        int position = this.mVideoView.getCurrentPosition();
        int duration = this.mVideoView.getDuration();
        this.mController.setTimes(position, duration, 0, 0);
        return position;
    }

    private void startVideo() {
        String scheme = this.mUri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)) {
            this.mController.showLoading();
            this.mHandler.removeCallbacks(this.mPlayingChecker);
            this.mHandler.postDelayed(this.mPlayingChecker, 250L);
        } else {
            this.mController.showPlaying();
            this.mController.hide();
        }
        this.mVideoView.start();
        setProgress();
    }

    private void playVideo() {
        this.mVideoView.start();
        this.mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        this.mVideoView.pause();
        this.mController.showPaused();
    }

    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        this.mHandler.removeCallbacksAndMessages(null);
        this.mController.showErrorMessage("");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.mController.showEnded();
        onCompletion();
    }

    public void onCompletion() {
    }

    @Override
    public void onPlayPause() {
        if (this.mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        this.mHandler.postDelayed(this.mStartMuteAudio, 0L);
        this.mDragging = true;
    }

    @Override
    public void onSeekMove(int time) {
        this.mVideoView.seekTo(time);
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        this.mDragging = false;
        this.mVideoView.seekTo(time);
        setProgress();
        this.mHandler.postDelayed(this.mStopMuteAudio, 500L);
    }

    @Override
    public void onShown() {
        this.mShowing = true;
        setProgress();
        showSystemUi(true);
    }

    @Override
    public void onHidden() {
        this.mShowing = false;
        showSystemUi(false);
    }

    @Override
    public void onReplay() {
        startVideo();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() > 0) {
            return isMediaKey(keyCode);
        }
        switch (keyCode) {
            case 79:
            case 85:
                if (this.mVideoView.isPlaying()) {
                    pauseVideo();
                    return true;
                }
                playVideo();
                return true;
            case 87:
            case 88:
                return true;
            case 126:
                if (this.mVideoView.isPlaying()) {
                    return true;
                }
                playVideo();
                return true;
            case 127:
                if (!this.mVideoView.isPlaying()) {
                    return true;
                }
                pauseVideo();
                return true;
            default:
                return false;
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return isMediaKey(keyCode);
    }

    private static boolean isMediaKey(int keyCode) {
        return keyCode == 79 || keyCode == 88 || keyCode == 87 || keyCode == 85 || keyCode == 126 || keyCode == 127;
    }

    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
        private AudioBecomingNoisyReceiver() {
        }

        public void register() {
            MoviePlayer.this.mContext.registerReceiver(this, new IntentFilter("android.media.AUDIO_BECOMING_NOISY"));
        }

        public void unregister() {
            MoviePlayer.this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MoviePlayer.this.mVideoView.isPlaying()) {
                MoviePlayer.this.pauseVideo();
            }
        }
    }
}
