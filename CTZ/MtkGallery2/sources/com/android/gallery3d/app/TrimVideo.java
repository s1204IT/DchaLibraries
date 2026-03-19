package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.ControllerOverlay;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.android.gallery3d.util.SaveVideoFileUtils;
import com.mediatek.gallery3d.video.MediaPlayerWrapper;
import com.mediatek.gallery3d.video.MovieView;
import com.mediatek.gallery3d.video.MtkVideoFeature;
import java.io.File;
import java.io.IOException;

public class TrimVideo extends Activity implements AudioManager.OnAudioFocusChangeListener, ControllerOverlay.Listener, MediaPlayerWrapper.Listener {
    private Context mContext;
    private TrimControllerOverlay mController;
    private boolean mDragging;
    private MediaPlayerWrapper mMeidaPlayerWrapper;
    private MovieView mMovieView;
    private Uri mNewVideoUri;
    private View mPlaceHolder;
    private ProgressDialog mProgress;
    private TextView mSaveVideoTextView;
    private Uri mUri;
    private final Handler mHandler = new Handler();
    private int mTrimStartTime = 0;
    private int mTrimEndTime = 0;
    private int mVideoPosition = 0;
    private String mSrcVideoPath = null;
    private String mSaveFileName = null;
    private File mSrcFile = null;
    private File mSaveDirectory = null;
    private SaveVideoFileInfo mDstFileInfo = null;
    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            TrimVideo.this.showProgressDialog();
        }
    };
    private final Runnable mShowToastRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(TrimVideo.this.getApplicationContext(), TrimVideo.this.getString(R.string.can_not_trim), 0).show();
            TrimVideo.this.setSaveClickable(true);
        }
    };
    private boolean mPlayTrimVideo = false;
    private boolean mIsSaving = false;
    private File mDstFile = null;
    private String saveFolderName = null;
    private boolean mHasPaused = false;
    private boolean mIsInProgressCheck = false;
    private int mAudiofocusState = 0;
    private final Runnable mStartVideoRunnable = new Runnable() {
        @Override
        public void run() {
            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "StartVideoRunnable,HasPaused:" + TrimVideo.this.mHasPaused);
            if (TrimVideo.this.mHasPaused) {
                TrimVideo.this.mPlayTrimVideo = true;
                return;
            }
            Toast.makeText(TrimVideo.this.getApplicationContext(), TrimVideo.this.getString(R.string.save_into, TrimVideo.this.mDstFileInfo.mFolderName), 0).show();
            if (TrimVideo.this.mProgress != null) {
                TrimVideo.this.mProgress.dismiss();
                TrimVideo.this.mProgress = null;
            }
            if (TrimVideo.this.mNewVideoUri != null) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setDataAndTypeAndNormalize(TrimVideo.this.mNewVideoUri, "video/*");
                intent.putExtra("android.intent.extra.finishOnCompletion", false);
                TrimVideo.this.startActivity(intent);
            }
            TrimVideo.this.mPlayTrimVideo = false;
            TrimVideo.this.mIsSaving = false;
            TrimVideo.this.mDstFile = null;
            TrimVideo.this.saveFolderName = null;
            TrimVideo.this.finish();
        }
    };
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int progress = TrimVideo.this.setProgress();
            TrimVideo.this.mIsInProgressCheck = true;
            TrimVideo.this.mHandler.postDelayed(TrimVideo.this.mProgressChecker, 200 - (progress % 200));
        }
    };
    private final Runnable mDelayVideoRunnable = new Runnable() {
        @Override
        public void run() {
            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "mDelayVideoRunnable.run()");
            TrimVideo.this.mMovieView.setVisibility(0);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        this.mContext = getApplicationContext();
        super.onCreate(bundle);
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onCreate()");
        requestWindowFeature(8);
        requestWindowFeature(9);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(0, 2);
        actionBar.setDisplayOptions(16, 16);
        actionBar.setCustomView(R.layout.trim_menu);
        this.mIsSaving = false;
        this.mSaveVideoTextView = (TextView) findViewById(R.id.start_trim);
        this.mSaveVideoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrimVideo.this.setSaveClickable(false);
                TrimVideo.this.mIsSaving = true;
                com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "mSaveVideoTextView onclick");
                TrimVideo.this.trimVideo();
            }
        });
        this.mSaveVideoTextView.setEnabled(false);
        Intent intent = getIntent();
        this.mUri = intent.getData();
        this.mSrcVideoPath = intent.getStringExtra("media-item-path");
        setContentView(R.layout.trim_view);
        View viewFindViewById = findViewById(R.id.trim_view_root);
        this.mMovieView = (MovieView) viewFindViewById.findViewById(R.id.movie_view);
        this.mPlaceHolder = viewFindViewById.findViewById(R.id.place_holder);
        this.mMeidaPlayerWrapper = new MediaPlayerWrapper(this.mContext, this.mMovieView);
        this.mMeidaPlayerWrapper.setListener(this);
        this.mMeidaPlayerWrapper.setVideoURI(this.mUri, null);
        this.mController = new TrimControllerOverlay(this);
        ((ViewGroup) viewFindViewById).addView(this.mController.getView());
        this.mController.setListener(this);
        this.mController.setCanReplay(true);
        this.mController.setMediaPlayerWrapper(this.mMeidaPlayerWrapper);
        playVideo();
    }

    @Override
    public void onResume() {
        super.onResume();
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onResume()");
        this.mDragging = false;
        if (!requestAudioFocus()) {
            pauseVideo();
        }
        if (this.mHasPaused) {
            this.mMovieView.removeCallbacks(this.mDelayVideoRunnable);
            this.mMovieView.postDelayed(this.mDelayVideoRunnable, 500L);
            this.mMeidaPlayerWrapper.seekTo(this.mVideoPosition);
            this.mMeidaPlayerWrapper.resume();
            this.mController.setMediaPlayerWrapper(this.mMeidaPlayerWrapper);
            this.mHasPaused = false;
        }
        this.mHandler.post(this.mProgressChecker);
        if (this.mIsSaving) {
            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "need show progress dialog.");
            if (this.mProgress == null) {
                showProgressDialog();
            }
            setSaveClickable(false);
            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "mPlayTrimVideo = " + this.mPlayTrimVideo);
            if (this.mPlayTrimVideo) {
                this.mHandler.post(this.mStartVideoRunnable);
            }
        }
    }

    @Override
    public void onPause() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onPause()");
        this.mHasPaused = true;
        this.mHandler.removeCallbacksAndMessages(null);
        this.mMovieView.removeCallbacks(this.mDelayVideoRunnable);
        int currentPosition = this.mMeidaPlayerWrapper.getCurrentPosition();
        if (currentPosition < 0) {
            currentPosition = this.mVideoPosition;
        }
        this.mVideoPosition = currentPosition;
        this.mMeidaPlayerWrapper.suspend();
        super.onPause();
    }

    @Override
    public void onStop() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onStop()");
        abandonAudiofocus();
        if (this.mProgress != null) {
            this.mProgress.dismiss();
            this.mProgress = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onDestroy()");
        this.mMeidaPlayerWrapper.stop();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onSaveInstanceState()");
        bundle.putInt("trim_start", this.mTrimStartTime);
        bundle.putInt("trim_end", this.mTrimEndTime);
        bundle.putInt("video_pos", this.mVideoPosition);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onRestoreInstanceState()");
        this.mTrimStartTime = bundle.getInt("trim_start", 0);
        this.mTrimEndTime = bundle.getInt("trim_end", 0);
        this.mVideoPosition = bundle.getInt("video_pos", 0);
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "mTrimStartTime is " + this.mTrimStartTime + ", mTrimEndTime is " + this.mTrimEndTime + ", mVideoPosition is " + this.mVideoPosition);
    }

    private int setProgress() {
        this.mVideoPosition = this.mMeidaPlayerWrapper.getCurrentPosition();
        if (this.mDragging) {
            return 0;
        }
        if (!this.mIsInProgressCheck && this.mVideoPosition < this.mTrimStartTime) {
            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "setProgress() mVideoPosition < mTrimStartTime");
            this.mMeidaPlayerWrapper.seekTo(this.mTrimStartTime);
            this.mVideoPosition = this.mTrimStartTime;
        }
        if (this.mVideoPosition >= this.mTrimEndTime && this.mTrimEndTime > 0) {
            if (this.mVideoPosition > this.mTrimEndTime) {
                this.mMeidaPlayerWrapper.seekTo(this.mTrimEndTime);
                this.mVideoPosition = this.mTrimEndTime;
            }
            this.mController.showEnded();
            this.mMeidaPlayerWrapper.pause();
        }
        int duration = this.mMeidaPlayerWrapper.getDuration();
        if (duration > 0 && this.mTrimEndTime == 0) {
            this.mTrimEndTime = duration;
        }
        this.mController.setTimes(this.mVideoPosition, duration, this.mTrimStartTime, this.mTrimEndTime);
        this.mSaveVideoTextView.setEnabled(isModified());
        return this.mVideoPosition;
    }

    private void playVideo() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "playVideo()");
        if (!hasAudiofocus() && !requestAudioFocus()) {
            Toast.makeText(this.mContext.getApplicationContext(), this.mContext.getString(R.string.m_audiofocus_request_failed_message), 0).show();
            return;
        }
        this.mMeidaPlayerWrapper.start();
        this.mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "pauseVideo()");
        this.mMeidaPlayerWrapper.pause();
        this.mController.showPaused();
    }

    private boolean isModified() {
        int i = this.mTrimEndTime - this.mTrimStartTime;
        if (i < 100 || Math.abs(this.mMeidaPlayerWrapper.getDuration() - i) < 100) {
            return false;
        }
        return true;
    }

    public void setSaveClickable(boolean z) {
        this.mSaveVideoTextView.setClickable(z);
        this.mSaveVideoTextView.setEnabled(z);
    }

    private void trimVideo() {
        if (this.mSrcVideoPath == null) {
            return;
        }
        final File file = new File(this.mSrcVideoPath);
        if (!file.exists()) {
            setSaveClickable(true);
            this.mIsSaving = false;
            return;
        }
        this.mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo("'TRIM'_yyyyMMdd_HHmmss", getContentResolver(), this.mUri, file.getParentFile(), true, getString(R.string.folder_download));
        if (!isSpaceEnough(file)) {
            Toast.makeText(getApplicationContext(), getString(R.string.storage_not_enough), 0).show();
            setSaveClickable(true);
            this.mIsSaving = false;
        } else {
            if (ApiHelper.HAS_MEDIA_MUXER) {
                showProgressDialog();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!VideoUtils.startTrim(file, TrimVideo.this.mDstFileInfo.mFile, TrimVideo.this.mTrimStartTime, TrimVideo.this.mTrimEndTime, TrimVideo.this, TrimVideo.this.mProgress)) {
                            if (TrimVideo.this.mProgress != null) {
                                TrimVideo.this.mProgress.dismiss();
                                TrimVideo.this.mProgress = null;
                            }
                            TrimVideo.this.showToast();
                            TrimVideo.this.mIsSaving = false;
                            if (TrimVideo.this.mDstFileInfo.mFile.exists()) {
                                TrimVideo.this.mDstFileInfo.mFile.delete();
                                return;
                            }
                            return;
                        }
                        try {
                            TrimVideo.this.mPlayTrimVideo = true;
                            TrimVideo.this.mNewVideoUri = null;
                            TrimVideo.this.mNewVideoUri = SaveVideoFileUtils.insertContent(TrimVideo.this.mDstFileInfo, TrimVideo.this.getContentResolver(), TrimVideo.this.mUri);
                            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "mNewVideoUri = " + TrimVideo.this.mNewVideoUri);
                        } catch (IllegalArgumentException e) {
                            com.mediatek.gallery3d.util.Log.e("VP_TrimVideo", "setDataSource IllegalArgumentException");
                        } catch (UnsupportedOperationException e2) {
                            com.mediatek.gallery3d.util.Log.e("VP_TrimVideo", "db detech UnsupportedOperationException");
                        }
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                    com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "save trim video succeed!");
                    TrimVideo.this.mHandler.post(TrimVideo.this.mStartVideoRunnable);
                }
            }).start();
        }
    }

    private void showProgressDialog() {
        this.mProgress = new ProgressDialog(this);
        this.mProgress.setTitle(getString(R.string.trimming));
        this.mProgress.setMessage(getString(R.string.please_wait));
        this.mProgress.setCancelable(false);
        this.mProgress.setCanceledOnTouchOutside(false);
        this.mProgress.show();
    }

    public void showToast() {
        this.mHandler.removeCallbacks(this.mShowToastRunnable);
        this.mHandler.post(this.mShowToastRunnable);
    }

    @Override
    public void onPlayPause() {
        if (this.mHasPaused) {
            return;
        }
        if (this.mMeidaPlayerWrapper.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onSeekStart() mDragging is " + this.mDragging);
        this.mDragging = true;
        pauseVideo();
    }

    @Override
    public void onSeekMove(int i) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onSeekMove() seekto time is (" + i + ") mDragging is " + this.mDragging);
        if (!this.mDragging) {
            this.mMeidaPlayerWrapper.seekTo(i);
        }
    }

    @Override
    public void onSeekEnd(int i, int i2, int i3) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onSeekEnd() seekto time is " + i + ", start is " + i2 + ", end is " + i3 + " mDragging is " + this.mDragging);
        this.mDragging = false;
        this.mMeidaPlayerWrapper.seekTo(i);
        this.mTrimStartTime = i2;
        this.mTrimEndTime = i3;
        this.mIsInProgressCheck = false;
        setProgress();
    }

    @Override
    public void onShown() {
    }

    @Override
    public void onHidden() {
    }

    @Override
    public void onReplay() {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onReplay()");
        this.mMeidaPlayerWrapper.seekTo(this.mTrimStartTime);
        playVideo();
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onKeyDown keyCode = " + i);
        if (i == 90 || i == 87 || i == 88 || i == 89 || i == 85 || i == 79) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onKeyUp keyCode = " + i);
        if (i == 90 || i == 87 || i == 88 || i == 89 || i == 85 || i == 79) {
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public boolean powerSavingNeedShowController() {
        return false;
    }

    private long getAvailableSpace() {
        StatFs statFs = new StatFs(this.mSrcVideoPath);
        return ((long) statFs.getAvailableBlocks()) * ((long) statFs.getBlockSize());
    }

    private boolean isSpaceEnough(File file) {
        long j;
        if (MtkVideoFeature.isGmoRamOptimize()) {
            j = 9437184;
        } else {
            j = 50331648;
        }
        if (getAvailableSpace() < ((file.length() * ((long) (this.mTrimEndTime - this.mTrimStartTime))) / ((long) this.mMeidaPlayerWrapper.getDuration())) + j) {
            com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "space is not enough for save trim video");
            return false;
        }
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "onCompletion()");
        this.mController.showEnded();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2) {
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        if (i == 3 && this.mPlaceHolder != null) {
            com.mediatek.gallery3d.util.Log.d("VP_TrimVideo", "dismiss place holder");
            this.mPlaceHolder.setVisibility(8);
            return false;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    private boolean requestAudioFocus() {
        this.mAudiofocusState = ((AudioManager) getSystemService("audio")).requestAudioFocus(this, 3, 2);
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "requestAudioFocus mAudiofocusState= " + this.mAudiofocusState);
        if (hasAudiofocus()) {
            return true;
        }
        return false;
    }

    private void abandonAudiofocus() {
        ((AudioManager) getSystemService("audio")).abandonAudioFocus(this);
        this.mAudiofocusState = 0;
    }

    private boolean hasAudiofocus() {
        return this.mAudiofocusState == 1;
    }

    @Override
    public void onAudioFocusChange(int i) {
        com.mediatek.gallery3d.util.Log.v("VP_TrimVideo", "AudioFocusChange state is " + i);
        if (i == 1) {
            this.mAudiofocusState = 1;
        } else if (i == -1 || i == -2) {
            this.mAudiofocusState = 0;
            pauseVideo();
        }
    }
}
