package com.android.music;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.omadrm.OmaDrmUtils;
import java.io.IOException;

public class AudioPreview extends Activity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener {
    private AudioManager mAudioManager;
    private TextView mLoadingText;
    private boolean mPausedByTransientLossOfFocus;
    private PreviewPlayer mPlayer;
    private Handler mProgressRefresher;
    private SeekBar mSeekBar;
    private TextView mTextLine1;
    private TextView mTextLine2;
    private Toast mToast;
    private Uri mUri;
    private boolean mSeeking = false;
    private int mDuration = -1;
    private long mMediaId = -1;
    private boolean mPauseRefreshingProgressBar = false;
    private boolean mMediaCanSeek = true;
    private boolean mIsComplete = false;
    private BroadcastReceiver mUnmountReceiver = null;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int i) {
            MusicLogUtils.d("AudioPreview", "mPlayer:" + AudioPreview.this.mPlayer + ",focusChange:" + i);
            if (AudioPreview.this.mPlayer == null) {
                AudioPreview.this.mAudioManager.abandonAudioFocus(this);
                return;
            }
            if (i != 1) {
                switch (i) {
                    case -3:
                    case -2:
                        if (AudioPreview.this.mPlayer.isPlaying()) {
                            AudioPreview.this.mPausedByTransientLossOfFocus = true;
                            AudioPreview.this.mPlayer.pause();
                        }
                        break;
                    case -1:
                        AudioPreview.this.mPausedByTransientLossOfFocus = false;
                        AudioPreview.this.mPlayer.pause();
                        break;
                }
            } else if (AudioPreview.this.mPausedByTransientLossOfFocus) {
                AudioPreview.this.mPausedByTransientLossOfFocus = false;
                AudioPreview.this.start();
            }
            AudioPreview.this.updatePlayPause();
        }
    };
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (AudioPreview.this.mMediaCanSeek) {
                AudioPreview.this.mSeeking = true;
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
            if (z && !AudioPreview.this.mSeeking && AudioPreview.this.mPlayer != null && AudioPreview.this.mMediaCanSeek) {
                AudioPreview.this.mPlayer.seekTo(i);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (AudioPreview.this.mPlayer != null && AudioPreview.this.mMediaCanSeek) {
                AudioPreview.this.mPlayer.seekTo(seekBar.getProgress());
            }
            AudioPreview.this.mSeeking = false;
            AudioPreview.this.mIsComplete = false;
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MusicLogUtils.d("AudioPreview", "onCreate");
        Intent intent = getIntent();
        if (((AudioManager) getApplicationContext().getSystemService("audio")).getMode() == 3) {
            showToast(getString(R.string.audiofocus_request_failed_message));
            MusicLogUtils.v("AudioPreview", "onCreate: phone call is ongoing, can not play music!");
            finish();
            return;
        }
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
        getWindow().setCloseOnTouchOutside(false);
        this.mTextLine1 = (TextView) findViewById(R.id.line1);
        this.mTextLine2 = (TextView) findViewById(R.id.line2);
        this.mLoadingText = (TextView) findViewById(R.id.loading);
        if (scheme.equals("http")) {
            this.mLoadingText.setText(getString(R.string.streamloadingtext, new Object[]{this.mUri.getHost()}));
        } else {
            this.mLoadingText.setVisibility(8);
        }
        String stringExtra = intent.getStringExtra("title");
        if (stringExtra != null) {
            this.mTextLine1.setText(stringExtra);
        }
        String stringExtra2 = intent.getStringExtra("displayName");
        if (stringExtra2 != null) {
            this.mTextLine2.setText(stringExtra2);
        }
        this.mSeekBar = (SeekBar) findViewById(R.id.progress);
        this.mProgressRefresher = new Handler();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        PreviewPlayer previewPlayer = (PreviewPlayer) getLastNonConfigurationInstance();
        if (previewPlayer == null) {
            this.mPlayer = new PreviewPlayer();
            this.mPlayer.setActivity(this);
            try {
                this.mPlayer.setDataSourceAndPrepare(this.mUri);
            } catch (IOException e) {
                MusicLogUtils.d("AudioPreview", "Failed to open file: " + e);
                Toast.makeText(this, R.string.playback_failed, 0).show();
                finish();
                return;
            }
        } else {
            this.mPlayer = previewPlayer;
            this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 2);
            this.mPlayer.setActivity(this);
            MusicLogUtils.d("AudioPreview", "onCreate,mPlayer.isPrepared():" + this.mPlayer.isPrepared());
            if (this.mPlayer.isPrepared()) {
                showPostPrepareUI();
            }
        }
        registerExternalStorageListener();
        AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex("title");
                    int columnIndex2 = cursor.getColumnIndex("artist");
                    int columnIndex3 = cursor.getColumnIndex("_id");
                    int columnIndex4 = cursor.getColumnIndex("_display_name");
                    String string = AudioPreview.this.getString(R.string.unknown_artist_name);
                    if (columnIndex3 >= 0) {
                        AudioPreview.this.mMediaId = cursor.getLong(columnIndex3);
                    }
                    if (columnIndex >= 0) {
                        AudioPreview.this.mTextLine1.setText(cursor.getString(columnIndex));
                        if (columnIndex2 >= 0) {
                            String string2 = cursor.getString(columnIndex2);
                            MusicLogUtils.d("AudioPreview", "displayname" + string2);
                            if (string2 == null || string2.equals("<unknown>")) {
                                string2 = string;
                            }
                            AudioPreview.this.mTextLine2.setText(string2);
                        }
                    } else if (columnIndex4 >= 0) {
                        AudioPreview.this.mTextLine1.setText(cursor.getString(columnIndex4));
                    } else {
                        MusicLogUtils.d("AudioPreview", "Cursor had no names for us");
                    }
                } else {
                    MusicLogUtils.d("AudioPreview", "empty cursor");
                }
                if (cursor != null) {
                    cursor.close();
                }
                AudioPreview.this.setNames();
            }
        };
        if (scheme.equals("content")) {
            if (this.mUri.getAuthority().equals("media")) {
                asyncQueryHandler.startQuery(0, null, this.mUri, new String[]{"title", "artist"}, null, null, null);
                return;
            } else {
                asyncQueryHandler.startQuery(0, null, this.mUri, null, null, null, null);
                return;
            }
        }
        if (scheme.equals("file")) {
            asyncQueryHandler.startQuery(0, null, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id", "title", "artist"}, "_data=?", new String[]{this.mUri.getPath()}, null);
        } else if (this.mPlayer.isPrepared()) {
            setNames();
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        MusicLogUtils.d("AudioPreview", "onRetainNonConfigurationInstance:");
        PreviewPlayer previewPlayer = this.mPlayer;
        this.mPlayer = null;
        this.mAudioManager.abandonAudioFocus(this.mAudioFocusListener);
        return previewPlayer;
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.d("AudioPreview", "onDestroy()");
        stopPlayback();
        if (this.mUnmountReceiver != null) {
            unregisterReceiver(this.mUnmountReceiver);
            this.mUnmountReceiver = null;
        }
        super.onDestroy();
    }

    private void stopPlayback() {
        MusicLogUtils.d("AudioPreview", "stopPlayback(),mPlayer:" + this.mPlayer);
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
        MusicLogUtils.d("AudioPreview", "onUserLeaveHint()");
        stopPlayback();
        finish();
        super.onUserLeaveHint();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService("audio");
        if (isFinishing()) {
            return;
        }
        this.mPlayer = (PreviewPlayer) mediaPlayer;
        setNames();
        if (this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 2) == 0 || audioManager.getMode() == 3) {
            showToast(getString(R.string.audiofocus_request_failed_message));
            MusicLogUtils.d("AudioPreview", "onPrepared: phone call is ongoing, can not play music!");
        } else {
            this.mPlayer.start();
        }
        showPostPrepareUI();
    }

    private void showPostPrepareUI() {
        MusicLogUtils.d("AudioPreview", "showPostPrepareUI");
        ((ProgressBar) findViewById(R.id.spinner)).setVisibility(8);
        this.mDuration = this.mPlayer.getDuration();
        MusicLogUtils.d("AudioPreview", "mDuration:" + this.mDuration);
        String path = this.mUri.getPath();
        MusicLogUtils.d("AudioPreview", path);
        this.mMediaCanSeek = true;
        if (path.toLowerCase().endsWith(".imy") && this.mDuration == Integer.MAX_VALUE) {
            this.mMediaCanSeek = false;
        }
        if (this.mDuration != 0) {
            this.mSeekBar.setMax(this.mDuration);
            this.mSeekBar.setVisibility(0);
            if (!this.mSeeking) {
                this.mSeekBar.setProgress(this.mPlayer.getCurrentPosition());
            }
        }
        this.mSeekBar.setOnSeekBarChangeListener(this.mSeekListener);
        if (!this.mSeekBar.isInTouchMode()) {
            this.mSeekBar.requestFocus();
        }
        this.mLoadingText.setVisibility(8);
        findViewById(R.id.titleandbuttons).setVisibility(0);
        this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 2);
        if (this.mProgressRefresher != null) {
            this.mProgressRefresher.removeCallbacksAndMessages(null);
            this.mProgressRefresher.postDelayed(new ProgressRefresher(), 200L);
        }
        updatePlayPause();
    }

    private void start() {
        if (this.mAudioManager.requestAudioFocus(this.mAudioFocusListener, 3, 2) == 0) {
            showToast(getString(R.string.audiofocus_request_failed_message));
            MusicLogUtils.d("AudioPreview", "start: phone call is ongoing, can not play music!");
        } else {
            this.mPlayer.start();
            this.mProgressRefresher.postDelayed(new ProgressRefresher(), 0L);
        }
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
                int currentPosition = AudioPreview.this.mPlayer.getCurrentPosition();
                if (AudioPreview.this.mIsComplete && currentPosition + 500 >= AudioPreview.this.mDuration) {
                    currentPosition = AudioPreview.this.mDuration;
                }
                MusicLogUtils.d("AudioPreview", "ProgressRefresher Position:" + currentPosition);
                AudioPreview.this.mSeekBar.setProgress(AudioPreview.this.mPlayer.getCurrentPosition());
            }
            AudioPreview.this.mProgressRefresher.removeCallbacksAndMessages(null);
            if (!AudioPreview.this.mPauseRefreshingProgressBar) {
                AudioPreview.this.mProgressRefresher.postDelayed(AudioPreview.this.new ProgressRefresher(), 200L);
            }
        }
    }

    private void updatePlayPause() {
        ImageButton imageButton = (ImageButton) findViewById(R.id.playpause);
        if (imageButton != null && this.mPlayer != null) {
            if (this.mPlayer.isPlaying()) {
                imageButton.setImageResource(R.drawable.ic_appwidget_music_pause);
                return;
            }
            imageButton.setImageResource(R.drawable.ic_appwidget_music_play);
            if (this.mProgressRefresher != null) {
                this.mProgressRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Toast.makeText(this, R.string.playback_failed, 0).show();
        finish();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        MusicLogUtils.d("AudioPreview", "onCompletion Position:" + this.mPlayer.getCurrentPosition());
        updatePlayPause();
        this.mSeekBar.setProgress(this.mSeekBar.getMax());
        this.mIsComplete = true;
    }

    public void playPauseClicked(View view) {
        if (this.mPlayer == null) {
            return;
        }
        if (this.mPlayer.isPlaying()) {
            this.mPlayer.pause();
        } else {
            start();
        }
        this.mIsComplete = false;
        updatePlayPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, "open in music");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItemFindItem = menu.findItem(1);
        if (this.mMediaId >= 0) {
            menuItemFindItem.setVisible(true);
            return false;
        }
        menuItemFindItem.setVisible(false);
        return false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 4) {
            if (i != 79) {
                switch (i) {
                    case 85:
                        break;
                    case 86:
                        break;
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                        return true;
                    default:
                        switch (i) {
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
                                return super.onKeyDown(i, keyEvent);
                        }
                }
            }
            if (this.mPlayer.isPlaying()) {
                this.mPlayer.pause();
            } else {
                this.mIsComplete = false;
                start();
            }
            updatePlayPause();
            return true;
        }
        stopPlayback();
        finish();
        return true;
    }

    private static class PreviewPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener {
        AudioPreview mActivity;
        boolean mIsPrepared;

        private PreviewPlayer() {
            this.mIsPrepared = false;
        }

        public void setActivity(AudioPreview audioPreview) {
            this.mActivity = audioPreview;
            setOnPreparedListener(this);
            setOnErrorListener(this.mActivity);
            setOnCompletionListener(this.mActivity);
            setOnInfoListener(this.mActivity);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalStateException, SecurityException, IOException, IllegalArgumentException {
            setDataSource(this.mActivity, uri);
            prepareAsync();
        }

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            this.mIsPrepared = true;
            this.mActivity.onPrepared(mediaPlayer);
        }

        boolean isPrepared() {
            return this.mIsPrepared;
        }
    }

    @Override
    public void onPause() throws Throwable {
        DrmManagerClient drmManagerClient;
        this.mPauseRefreshingProgressBar = true;
        this.mProgressRefresher.removeCallbacksAndMessages(null);
        Intent intent = getIntent();
        String stringExtra = intent.getStringExtra("TOKEN");
        if (stringExtra != null) {
            MusicLogUtils.d("AudioPreview", "open from DataProtection, clear token and finish acivity when pause");
            try {
                drmManagerClient = new DrmManagerClient(this);
                try {
                    OmaDrmUtils.clearToken(drmManagerClient, intent.getStringExtra("TOKEN_KEY"), stringExtra);
                    drmManagerClient.release();
                    stopPlayback();
                    finish();
                } catch (Throwable th) {
                    th = th;
                    if (drmManagerClient != null) {
                        drmManagerClient.release();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                drmManagerClient = null;
            }
        }
        MusicLogUtils.d("AudioPreview", "onPause for stop ProgressRefresher!: token = " + stringExtra);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicLogUtils.d("AudioPreview", "onResume for start ProgressRefresher!");
        if (this.mPauseRefreshingProgressBar) {
            this.mPauseRefreshingProgressBar = false;
            this.mProgressRefresher.postDelayed(new ProgressRefresher(), 200L);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    private void showToast(CharSequence charSequence) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(getApplicationContext(), charSequence, 0);
        }
        this.mToast.setText(charSequence);
        this.mToast.show();
    }

    public void registerExternalStorageListener() {
        if (this.mUnmountReceiver == null) {
            this.mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals("android.intent.action.MEDIA_EJECT")) {
                        MusicLogUtils.d("AudioPreview", "MEDIA_EJECT");
                        String path = intent.getData().getPath();
                        MusicLogUtils.v("AudioPreview", "card eject: ejectingCardPath=" + path);
                        String path2 = AudioPreview.this.mUri.getPath();
                        if (path2 != null && path2.contains(path)) {
                            MusicLogUtils.d("AudioPreview", "MEDIA_EJECT: current track on an unmounting external card");
                            AudioPreview.this.finish();
                        }
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MEDIA_EJECT");
            intentFilter.addDataScheme("file");
            registerReceiver(this.mUnmountReceiver, intentFilter);
        }
    }
}
