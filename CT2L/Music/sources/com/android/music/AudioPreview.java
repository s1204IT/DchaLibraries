package com.android.music;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class AudioPreview extends Activity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private AudioManager mAudioManager;
    private int mDuration;
    private TextView mLoadingText;
    private boolean mPausedByTransientLossOfFocus;
    private PreviewPlayer mPlayer;
    private Handler mProgressRefresher;
    private SeekBar mSeekBar;
    private TextView mTextLine1;
    private TextView mTextLine2;
    private Uri mUri;
    private boolean mSeeking = false;
    private long mMediaId = -1;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (AudioPreview.this.mPlayer == null) {
                AudioPreview.this.mAudioManager.abandonAudioFocus(this);
                return;
            }
            switch (focusChange) {
                case -3:
                case -2:
                    if (AudioPreview.this.mPlayer.isPlaying()) {
                        AudioPreview.this.mPausedByTransientLossOfFocus = true;
                        AudioPreview.this.mPlayer.pause();
                    }
                    break;
                case -1:
                    AudioPreview.this.mPausedByTransientLossOfFocus = false;
                    if (AudioPreview.this.mPlayer.isPlaying()) {
                        AudioPreview.this.mPlayer.pause();
                    }
                    break;
                case 1:
                    if (AudioPreview.this.mPausedByTransientLossOfFocus) {
                        AudioPreview.this.mPausedByTransientLossOfFocus = false;
                        AudioPreview.this.start();
                    }
                    break;
            }
            AudioPreview.this.updatePlayPause();
        }
    };
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            AudioPreview.this.mSeeking = true;
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (fromuser && AudioPreview.this.mPlayer != null) {
                AudioPreview.this.mPlayer.seekTo(progress);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            AudioPreview.this.mSeeking = false;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        this.mUri = intent.getData();
        if (this.mUri == null) {
            finish();
            return;
        }
        String scheme = this.mUri.getScheme();
        setVolumeControlStream(3);
        requestWindowFeature(1);
        setContentView(R.layout.audiopreview);
        this.mTextLine1 = (TextView) findViewById(R.id.line1);
        this.mTextLine2 = (TextView) findViewById(R.id.line2);
        this.mLoadingText = (TextView) findViewById(R.id.loading);
        if (scheme.equals("http")) {
            String msg = getString(R.string.streamloadingtext, new Object[]{this.mUri.getHost()});
            this.mLoadingText.setText(msg);
        } else {
            this.mLoadingText.setVisibility(8);
        }
        this.mSeekBar = (SeekBar) findViewById(R.id.progress);
        this.mProgressRefresher = new Handler();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        PreviewPlayer player = (PreviewPlayer) getLastNonConfigurationInstance();
        if (player == null) {
            this.mPlayer = new PreviewPlayer();
            this.mPlayer.setActivity(this);
            try {
                this.mPlayer.setDataSourceAndPrepare(this.mUri);
            } catch (Exception ex) {
                Log.d("AudioPreview", "Failed to open file: " + ex);
                Toast.makeText(this, R.string.playback_failed, 0).show();
                finish();
                return;
            }
        } else {
            this.mPlayer = player;
            this.mPlayer.setActivity(this);
            if (this.mPlayer.isPrepared()) {
                showPostPrepareUI();
            }
        }
        AsyncQueryHandler mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {
                    int titleIdx = cursor.getColumnIndex("title");
                    int artistIdx = cursor.getColumnIndex("artist");
                    int idIdx = cursor.getColumnIndex("_id");
                    int displaynameIdx = cursor.getColumnIndex("_display_name");
                    if (idIdx >= 0) {
                        AudioPreview.this.mMediaId = cursor.getLong(idIdx);
                    }
                    if (titleIdx >= 0) {
                        String title = cursor.getString(titleIdx);
                        AudioPreview.this.mTextLine1.setText(title);
                        if (artistIdx >= 0) {
                            String artist = cursor.getString(artistIdx);
                            AudioPreview.this.mTextLine2.setText(artist);
                        }
                    } else if (displaynameIdx >= 0) {
                        String name = cursor.getString(displaynameIdx);
                        AudioPreview.this.mTextLine1.setText(name);
                    } else {
                        Log.w("AudioPreview", "Cursor had no names for us");
                    }
                } else {
                    Log.w("AudioPreview", "empty cursor");
                }
                if (cursor != null) {
                    cursor.close();
                }
                AudioPreview.this.setNames();
            }
        };
        if (scheme.equals("content")) {
            if (this.mUri.getAuthority() == "media") {
                mAsyncQueryHandler.startQuery(0, null, this.mUri, new String[]{"title", "artist"}, null, null, null);
                return;
            } else {
                mAsyncQueryHandler.startQuery(0, null, this.mUri, null, null, null, null);
                return;
            }
        }
        if (scheme.equals("file")) {
            String path = this.mUri.getPath();
            mAsyncQueryHandler.startQuery(0, null, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id", "title", "artist"}, "_data=?", new String[]{path}, null);
        } else if (this.mPlayer.isPrepared()) {
            setNames();
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        PreviewPlayer player = this.mPlayer;
        this.mPlayer = null;
        return player;
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    private void stopPlayback() {
        if (this.mProgressRefresher != null) {
            this.mProgressRefresher.removeCallbacksAndMessages(null);
        }
        if (this.mPlayer != null) {
            this.mPlayer.release();
            this.mPlayer = null;
            this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
        }
    }

    @Override
    public void onUserLeaveHint() {
        stopPlayback();
        finish();
        super.onUserLeaveHint();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!isFinishing()) {
            this.mPlayer = (PreviewPlayer) mp;
            setNames();
            this.mPlayer.start();
            showPostPrepareUI();
        }
    }

    private void showPostPrepareUI() {
        ProgressBar pb = (ProgressBar) findViewById(R.id.spinner);
        pb.setVisibility(8);
        this.mDuration = this.mPlayer.getDuration();
        if (this.mDuration != 0) {
            this.mSeekBar.setMax(this.mDuration);
            this.mSeekBar.setVisibility(0);
        }
        this.mSeekBar.setOnSeekBarChangeListener(this.mSeekListener);
        this.mLoadingText.setVisibility(8);
        View v = findViewById(R.id.titleandbuttons);
        v.setVisibility(0);
        this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 2);
        this.mProgressRefresher.postDelayed(new ProgressRefresher(), 200L);
        updatePlayPause();
    }

    private void start() {
        this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 2);
        this.mPlayer.start();
        this.mProgressRefresher.postDelayed(new ProgressRefresher(), 200L);
    }

    public void setNames() {
        if (TextUtils.isEmpty(this.mTextLine1.getText())) {
            this.mTextLine1.setText(this.mUri.getLastPathSegment());
        }
        if (TextUtils.isEmpty(this.mTextLine2.getText())) {
            this.mTextLine2.setVisibility(8);
        } else {
            this.mTextLine2.setVisibility(0);
        }
    }

    class ProgressRefresher implements Runnable {
        ProgressRefresher() {
        }

        @Override
        public void run() {
            if (AudioPreview.this.mPlayer != null && !AudioPreview.this.mSeeking && AudioPreview.this.mDuration != 0) {
                int currentPosition = AudioPreview.this.mPlayer.getCurrentPosition() / AudioPreview.this.mDuration;
                AudioPreview.this.mSeekBar.setProgress(AudioPreview.this.mPlayer.getCurrentPosition());
            }
            AudioPreview.this.mProgressRefresher.removeCallbacksAndMessages(null);
            AudioPreview.this.mProgressRefresher.postDelayed(AudioPreview.this.new ProgressRefresher(), 200L);
        }
    }

    private void updatePlayPause() {
        ImageButton b = (ImageButton) findViewById(R.id.playpause);
        if (b != null) {
            if (this.mPlayer.isPlaying()) {
                b.setImageResource(R.drawable.btn_playback_ic_pause_small);
            } else {
                b.setImageResource(R.drawable.btn_playback_ic_play_small);
                this.mProgressRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, R.string.playback_failed, 0).show();
        finish();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        this.mSeekBar.setProgress(this.mDuration);
        updatePlayPause();
    }

    public void playPauseClicked(View v) {
        if (this.mPlayer != null) {
            if (this.mPlayer.isPlaying()) {
                this.mPlayer.pause();
            } else {
                start();
            }
            updatePlayPause();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, "open in music");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(1);
        if (this.mMediaId >= 0) {
            item.setVisible(true);
            return true;
        }
        item.setVisible(false);
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
            case 86:
                stopPlayback();
                finish();
                return true;
            case 79:
            case 85:
                if (this.mPlayer.isPlaying()) {
                    this.mPlayer.pause();
                } else {
                    start();
                }
                updatePlayPause();
                return true;
            case 87:
            case 88:
            case 89:
            case 90:
                return true;
            case 126:
                start();
                updatePlayPause();
                return true;
            case 127:
                if (this.mPlayer.isPlaying()) {
                    this.mPlayer.pause();
                }
                updatePlayPause();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private static class PreviewPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener {
        AudioPreview mActivity;
        boolean mIsPrepared;

        private PreviewPlayer() {
            this.mIsPrepared = false;
        }

        public void setActivity(AudioPreview activity) {
            this.mActivity = activity;
            setOnPreparedListener(this);
            setOnErrorListener(this.mActivity);
            setOnCompletionListener(this.mActivity);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalStateException, SecurityException, IOException, IllegalArgumentException {
            setDataSource(this.mActivity, uri);
            prepareAsync();
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            this.mIsPrepared = true;
            this.mActivity.onPrepared(mp);
        }

        boolean isPrepared() {
            return this.mIsPrepared;
        }
    }
}
