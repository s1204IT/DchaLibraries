package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.android.gallery3d.R;
import com.android.gallery3d.app.ControllerOverlay;
import com.android.gallery3d.util.SaveVideoFileInfo;
import com.android.gallery3d.util.SaveVideoFileUtils;
import java.io.File;
import java.io.IOException;

public class TrimVideo extends Activity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, ControllerOverlay.Listener {
    private Context mContext;
    private TrimControllerOverlay mController;
    public ProgressDialog mProgress;
    private TextView mSaveVideoTextView;
    private Uri mUri;
    private VideoView mVideoView;
    private final Handler mHandler = new Handler();
    private int mTrimStartTime = 0;
    private int mTrimEndTime = 0;
    private int mVideoPosition = 0;
    private boolean mHasPaused = false;
    private String mSrcVideoPath = null;
    private SaveVideoFileInfo mDstFileInfo = null;
    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = TrimVideo.this.setProgress();
            TrimVideo.this.mHandler.postDelayed(TrimVideo.this.mProgressChecker, 200 - (pos % 200));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.mContext = getApplicationContext();
        super.onCreate(savedInstanceState);
        requestWindowFeature(8);
        requestWindowFeature(9);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(0, 2);
        actionBar.setDisplayOptions(16, 16);
        actionBar.setCustomView(R.layout.trim_menu);
        this.mSaveVideoTextView = (TextView) findViewById(R.id.start_trim);
        this.mSaveVideoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                TrimVideo.this.trimVideo();
            }
        });
        this.mSaveVideoTextView.setEnabled(false);
        Intent intent = getIntent();
        this.mUri = intent.getData();
        this.mSrcVideoPath = intent.getStringExtra("media-item-path");
        setContentView(R.layout.trim_view);
        View rootView = findViewById(R.id.trim_view_root);
        this.mVideoView = (VideoView) rootView.findViewById(R.id.surface_view);
        this.mController = new TrimControllerOverlay(this.mContext);
        ((ViewGroup) rootView).addView(this.mController.getView());
        this.mController.setListener(this);
        this.mController.setCanReplay(true);
        this.mVideoView.setOnErrorListener(this);
        this.mVideoView.setOnCompletionListener(this);
        this.mVideoView.setVideoURI(this.mUri);
        playVideo();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mHasPaused) {
            this.mVideoView.seekTo(this.mVideoPosition);
            this.mVideoView.resume();
            this.mHasPaused = false;
        }
        this.mHandler.post(this.mProgressChecker);
    }

    @Override
    public void onPause() {
        this.mHasPaused = true;
        this.mHandler.removeCallbacksAndMessages(null);
        this.mVideoPosition = this.mVideoView.getCurrentPosition();
        this.mVideoView.suspend();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (this.mProgress != null) {
            this.mProgress.dismiss();
            this.mProgress = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        this.mVideoView.stopPlayback();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("trim_start", this.mTrimStartTime);
        savedInstanceState.putInt("trim_end", this.mTrimEndTime);
        savedInstanceState.putInt("video_pos", this.mVideoPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mTrimStartTime = savedInstanceState.getInt("trim_start", 0);
        this.mTrimEndTime = savedInstanceState.getInt("trim_end", 0);
        this.mVideoPosition = savedInstanceState.getInt("video_pos", 0);
    }

    private int setProgress() {
        this.mVideoPosition = this.mVideoView.getCurrentPosition();
        if (this.mVideoPosition < this.mTrimStartTime) {
        }
        if (this.mVideoPosition >= this.mTrimEndTime && this.mTrimEndTime > 0) {
            if (this.mVideoPosition > this.mTrimEndTime) {
                this.mVideoView.seekTo(this.mTrimEndTime);
                this.mVideoPosition = this.mTrimEndTime;
            }
            this.mController.showEnded();
            this.mVideoView.pause();
        }
        int duration = this.mVideoView.getDuration();
        if (duration > 0 && this.mTrimEndTime == 0) {
            this.mTrimEndTime = duration;
        }
        this.mController.setTimes(this.mVideoPosition, duration, this.mTrimStartTime, this.mTrimEndTime);
        this.mSaveVideoTextView.setEnabled(isModified());
        return this.mVideoPosition;
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

    private boolean isModified() {
        int delta = this.mTrimEndTime - this.mTrimStartTime;
        return delta >= 100 && Math.abs(this.mVideoView.getDuration() - delta) >= 100;
    }

    private void trimVideo() {
        this.mDstFileInfo = SaveVideoFileUtils.getDstMp4FileInfo("'TRIM'_yyyyMMdd_HHmmss", getContentResolver(), this.mUri, getString(R.string.folder_download));
        final File mSrcFile = new File(this.mSrcVideoPath);
        showProgressDialog();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    VideoUtils.startTrim(mSrcFile, TrimVideo.this.mDstFileInfo.mFile, TrimVideo.this.mTrimStartTime, TrimVideo.this.mTrimEndTime);
                    SaveVideoFileUtils.insertContent(TrimVideo.this.mDstFileInfo, TrimVideo.this.getContentResolver(), TrimVideo.this.mUri);
                    TrimVideo.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TrimVideo.this.getApplicationContext(), TrimVideo.this.getString(R.string.save_into, new Object[]{TrimVideo.this.mDstFileInfo.mFolderName}), 0).show();
                            if (TrimVideo.this.mProgress != null) {
                                TrimVideo.this.mProgress.dismiss();
                                TrimVideo.this.mProgress = null;
                                Intent intent = new Intent("android.intent.action.VIEW");
                                intent.setDataAndType(Uri.fromFile(TrimVideo.this.mDstFileInfo.mFile), "video/*");
                                intent.putExtra("android.intent.extra.finishOnCompletion", false);
                                TrimVideo.this.startActivity(intent);
                                TrimVideo.this.finish();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    if (TrimVideo.this.mProgress != null) {
                        TrimVideo.this.mProgress.dismiss();
                        TrimVideo.this.mProgress = null;
                    }
                    Log.e("@@@@", "Can't trim file: " + mSrcFile, e);
                    TrimVideo.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TrimVideo.this.getApplicationContext(), "Can't trim video", 0).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showProgressDialog() {
        this.mProgress = new ProgressDialog(this);
        this.mProgress.setTitle(getString(R.string.trimming));
        this.mProgress.setMessage(getString(R.string.please_wait));
        this.mProgress.setCancelable(false);
        this.mProgress.setCanceledOnTouchOutside(false);
        this.mProgress.show();
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
        pauseVideo();
    }

    @Override
    public void onSeekMove(int time) {
        this.mVideoView.seekTo(time);
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        this.mVideoView.seekTo(time);
        this.mTrimStartTime = start;
        this.mTrimEndTime = end;
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
        this.mVideoView.seekTo(this.mTrimStartTime);
        playVideo();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.mController.showEnded();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}
