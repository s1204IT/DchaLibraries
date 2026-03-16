package com.android.soundrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.soundrecorder.Recorder;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SoundRecorder extends Activity implements View.OnClickListener, Recorder.OnStateChangedListener {
    Button mAcceptButton;
    Button mDiscardButton;
    LinearLayout mExitButtons;
    ImageButton mPlayButton;
    ImageButton mRecordButton;
    Recorder mRecorder;
    RemainingTimeCalculator mRemainingTimeCalculator;
    ImageView mStateLED;
    TextView mStateMessage1;
    TextView mStateMessage2;
    ProgressBar mStateProgressBar;
    ImageButton mStopButton;
    String mTimerFormat;
    TextView mTimerView;
    VUMeter mVUMeter;
    PowerManager.WakeLock mWakeLock;
    String mRequestedType = "audio/*";
    boolean mSampleInterrupted = false;
    String mErrorUiMessage = null;
    long mMaxFileSize = -1;
    final Handler mHandler = new Handler();
    Runnable mUpdateTimer = new Runnable() {
        @Override
        public void run() {
            SoundRecorder.this.updateTimerView();
        }
    };
    private BroadcastReceiver mSDCardMountEventReceiver = null;

    @Override
    public void onCreate(Bundle icycle) {
        Bundle recorderState;
        super.onCreate(icycle);
        Intent i = getIntent();
        if (i != null) {
            String s = i.getType();
            if ("audio/amr".equals(s) || "audio/3gpp".equals(s) || "audio/*".equals(s) || "*/*".equals(s)) {
                this.mRequestedType = s;
            } else if (s != null) {
                setResult(0);
                finish();
                return;
            }
            this.mMaxFileSize = i.getLongExtra("android.provider.MediaStore.extra.MAX_BYTES", -1L);
        }
        if ("audio/*".equals(this.mRequestedType) || "*/*".equals(this.mRequestedType)) {
            this.mRequestedType = "audio/3gpp";
        }
        setContentView(R.layout.main);
        this.mRecorder = new Recorder();
        this.mRecorder.setOnStateChangedListener(this);
        this.mRemainingTimeCalculator = new RemainingTimeCalculator();
        PowerManager pm = (PowerManager) getSystemService("power");
        this.mWakeLock = pm.newWakeLock(6, "SoundRecorder");
        initResourceRefs();
        setResult(0);
        registerExternalStorageListener();
        if (icycle != null && (recorderState = icycle.getBundle("recorder_state")) != null) {
            this.mRecorder.restoreState(recorderState);
            this.mSampleInterrupted = recorderState.getBoolean("sample_interrupted", false);
            this.mMaxFileSize = recorderState.getLong("max_file_size", -1L);
        }
        updateUi();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.main);
        initResourceRefs();
        updateUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mRecorder.sampleLength() != 0) {
            Bundle recorderState = new Bundle();
            this.mRecorder.saveState(recorderState);
            recorderState.putBoolean("sample_interrupted", this.mSampleInterrupted);
            recorderState.putLong("max_file_size", this.mMaxFileSize);
            outState.putBundle("recorder_state", recorderState);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.app_name).setMessage(R.string.error_mediadb_new_record).setPositiveButton(R.string.button_ok, (DialogInterface.OnClickListener) null).setCancelable(false);
                AlertDialog dialog = builder.create();
                return dialog;
            default:
                return null;
        }
    }

    private void initResourceRefs() {
        this.mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        this.mPlayButton = (ImageButton) findViewById(R.id.playButton);
        this.mStopButton = (ImageButton) findViewById(R.id.stopButton);
        this.mStateLED = (ImageView) findViewById(R.id.stateLED);
        this.mStateMessage1 = (TextView) findViewById(R.id.stateMessage1);
        this.mStateMessage2 = (TextView) findViewById(R.id.stateMessage2);
        this.mStateProgressBar = (ProgressBar) findViewById(R.id.stateProgressBar);
        this.mTimerView = (TextView) findViewById(R.id.timerView);
        this.mExitButtons = (LinearLayout) findViewById(R.id.exitButtons);
        this.mAcceptButton = (Button) findViewById(R.id.acceptButton);
        this.mDiscardButton = (Button) findViewById(R.id.discardButton);
        this.mVUMeter = (VUMeter) findViewById(R.id.uvMeter);
        this.mRecordButton.setOnClickListener(this);
        this.mPlayButton.setOnClickListener(this);
        this.mStopButton.setOnClickListener(this);
        this.mAcceptButton.setOnClickListener(this);
        this.mDiscardButton.setOnClickListener(this);
        this.mTimerFormat = getResources().getString(R.string.timer_format);
        this.mVUMeter.setRecorder(this.mRecorder);
    }

    private void stopAudioPlayback() {
        AudioManager am = (AudioManager) getSystemService("audio");
        am.requestAudioFocus(null, 3, 1);
    }

    @Override
    public void onClick(View button) {
        if (button.isEnabled()) {
            switch (button.getId()) {
                case R.id.discardButton:
                    this.mRecorder.delete();
                    finish();
                    return;
                case R.id.acceptButton:
                    this.mRecorder.stop();
                    saveSample();
                    finish();
                    return;
                case R.id.uvMeter:
                default:
                    return;
                case R.id.recordButton:
                    this.mRemainingTimeCalculator.reset();
                    if (!Environment.getExternalStorageState().equals("mounted")) {
                        this.mSampleInterrupted = true;
                        this.mErrorUiMessage = getResources().getString(R.string.insert_sd_card);
                        updateUi();
                        return;
                    }
                    if (!this.mRemainingTimeCalculator.diskSpaceAvailable()) {
                        this.mSampleInterrupted = true;
                        this.mErrorUiMessage = getResources().getString(R.string.storage_is_full);
                        updateUi();
                        return;
                    }
                    stopAudioPlayback();
                    if ("audio/amr".equals(this.mRequestedType)) {
                        this.mRemainingTimeCalculator.setBitRate(5900);
                        this.mRecorder.startRecording(3, ".amr", this, 5900);
                    } else if ("audio/3gpp".equals(this.mRequestedType)) {
                        this.mRemainingTimeCalculator.setBitRate(5900);
                        this.mRecorder.startRecording(1, ".3gpp", this, 5900);
                    } else {
                        throw new IllegalArgumentException("Invalid output file type requested");
                    }
                    if (this.mMaxFileSize != -1) {
                        this.mRemainingTimeCalculator.setFileSizeLimit(this.mRecorder.sampleFile(), this.mMaxFileSize);
                        return;
                    }
                    return;
                case R.id.playButton:
                    this.mRecorder.startPlayback();
                    return;
                case R.id.stopButton:
                    this.mRecorder.stop();
                    return;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            switch (this.mRecorder.state()) {
                case 0:
                    if (this.mRecorder.sampleLength() > 0) {
                        saveSample();
                    }
                    finish();
                    return true;
                case 1:
                    this.mRecorder.clear();
                    return true;
                case 2:
                    this.mRecorder.stop();
                    saveSample();
                    return true;
                default:
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStop() {
        this.mRecorder.stop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        this.mSampleInterrupted = this.mRecorder.state() == 1;
        this.mRecorder.stop();
        super.onPause();
    }

    private void saveSample() {
        if (this.mRecorder.sampleLength() != 0) {
            try {
                Uri uri = addToMediaDB(this.mRecorder.sampleFile());
                if (uri != null) {
                    setResult(-1, new Intent().setData(uri));
                }
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    @Override
    public void onDestroy() {
        if (this.mSDCardMountEventReceiver != null) {
            unregisterReceiver(this.mSDCardMountEventReceiver);
            this.mSDCardMountEventReceiver = null;
        }
        super.onDestroy();
    }

    private void registerExternalStorageListener() {
        if (this.mSDCardMountEventReceiver == null) {
            this.mSDCardMountEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals("android.intent.action.MEDIA_EJECT")) {
                        SoundRecorder.this.mRecorder.delete();
                    } else if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                        SoundRecorder.this.mSampleInterrupted = false;
                        SoundRecorder.this.updateUi();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction("android.intent.action.MEDIA_EJECT");
            iFilter.addAction("android.intent.action.MEDIA_MOUNTED");
            iFilter.addDataScheme("file");
            registerReceiver(this.mSDCardMountEventReceiver, iFilter);
        }
    }

    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private void addToPlaylist(ContentResolver resolver, int audioId, long playlistId) {
        String[] cols = {"count(*)"};
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        cur.moveToFirst();
        int base = cur.getInt(0);
        cur.close();
        ContentValues values = new ContentValues();
        values.put("play_order", Integer.valueOf(base + audioId));
        values.put("audio_id", Integer.valueOf(audioId));
        resolver.insert(uri, values);
    }

    private int getPlaylistId(Resources res) {
        Uri uri = MediaStore.Audio.Playlists.getContentUri("external");
        String[] ids = {"_id"};
        String[] args = {res.getString(R.string.audio_db_playlist_name)};
        Cursor cursor = query(uri, ids, "name=?", args, null);
        if (cursor == null) {
            Log.v("SoundRecorder", "query returns null");
        }
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
        }
        cursor.close();
        return id;
    }

    private Uri createPlaylist(Resources res, ContentResolver resolver) {
        ContentValues cv = new ContentValues();
        cv.put("name", res.getString(R.string.audio_db_playlist_name));
        Uri uri = resolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), cv);
        if (uri == null) {
            new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.error_mediadb_new_record).setPositiveButton(R.string.button_ok, (DialogInterface.OnClickListener) null).setCancelable(false).show();
        }
        return uri;
    }

    private Uri addToMediaDB(File file) {
        Resources res = getResources();
        ContentValues cv = new ContentValues();
        long current = System.currentTimeMillis();
        long modDate = file.lastModified();
        Date date = new Date(current);
        SimpleDateFormat formatter = new SimpleDateFormat(res.getString(R.string.audio_db_title_format));
        String title = formatter.format(date);
        long sampleLengthMillis = ((long) this.mRecorder.sampleLength()) * 1000;
        cv.put("is_music", "0");
        cv.put("title", title);
        cv.put("_data", file.getAbsolutePath());
        cv.put("date_added", Integer.valueOf((int) (current / 1000)));
        cv.put("date_modified", Integer.valueOf((int) (modDate / 1000)));
        cv.put("duration", Long.valueOf(sampleLengthMillis));
        cv.put("mime_type", this.mRequestedType);
        cv.put("artist", res.getString(R.string.audio_db_artist_name));
        cv.put("album", res.getString(R.string.audio_db_album_name));
        Log.d("SoundRecorder", "Inserting audio record: " + cv.toString());
        ContentResolver resolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.d("SoundRecorder", "ContentURI: " + base);
        Uri result = resolver.insert(base, cv);
        if (result == null) {
            showDialog(0);
            return null;
        }
        if (getPlaylistId(res) == -1) {
            createPlaylist(res, resolver);
        }
        int audioId = Integer.valueOf(result.getLastPathSegment()).intValue();
        addToPlaylist(resolver, audioId, getPlaylistId(res));
        sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", result));
        return result;
    }

    private void updateTimerView() {
        getResources();
        int state = this.mRecorder.state();
        int sampleLength = this.mRecorder.sampleLength();
        boolean ongoing = state == 1 || state == 2;
        long time = ongoing ? this.mRecorder.progress() : sampleLength;
        String timeStr = String.format(this.mTimerFormat, Long.valueOf(time / 60), Long.valueOf(time % 60));
        this.mTimerView.setText(timeStr);
        if (state == 2) {
            if (sampleLength > 0) {
                this.mStateProgressBar.setProgress((int) ((100 * time) / ((long) sampleLength)));
            } else {
                Log.w("SoundRecorder", "Record error, sample length is " + sampleLength);
                this.mStateProgressBar.setProgress(0);
            }
        } else if (state == 1) {
            updateTimeRemaining();
        }
        if (ongoing) {
            this.mHandler.postDelayed(this.mUpdateTimer, 1000L);
        }
    }

    private void updateTimeRemaining() {
        long t = this.mRemainingTimeCalculator.timeRemaining();
        if (t <= 0) {
            this.mSampleInterrupted = true;
            int limit = this.mRemainingTimeCalculator.currentLowerLimit();
            switch (limit) {
                case 1:
                    this.mErrorUiMessage = getResources().getString(R.string.max_length_reached);
                    break;
                case 2:
                    this.mErrorUiMessage = getResources().getString(R.string.storage_is_full);
                    break;
                default:
                    this.mErrorUiMessage = null;
                    break;
            }
            this.mRecorder.stop();
            return;
        }
        Resources res = getResources();
        String timeStr = "";
        if (t < 60) {
            timeStr = String.format(res.getString(R.string.sec_available), Long.valueOf(t));
        } else if (t < 540) {
            String timeStr2 = String.format(res.getString(R.string.min), Long.valueOf(t / 60));
            timeStr = (timeStr2 + " ") + String.format(res.getString(R.string.sec), Long.valueOf(t % 60));
        }
        this.mStateMessage1.setText(timeStr);
    }

    private void updateUi() {
        Resources res = getResources();
        switch (this.mRecorder.state()) {
            case 0:
                if (this.mRecorder.sampleLength() == 0) {
                    this.mRecordButton.setEnabled(true);
                    this.mRecordButton.setFocusable(true);
                    this.mPlayButton.setEnabled(false);
                    this.mPlayButton.setFocusable(false);
                    this.mStopButton.setEnabled(false);
                    this.mStopButton.setFocusable(false);
                    this.mRecordButton.requestFocus();
                    this.mStateMessage1.setVisibility(4);
                    this.mStateLED.setVisibility(4);
                    this.mStateMessage2.setVisibility(4);
                    this.mExitButtons.setVisibility(4);
                    this.mVUMeter.setVisibility(0);
                    this.mStateProgressBar.setVisibility(4);
                    setTitle(res.getString(R.string.record_your_message));
                } else {
                    this.mRecordButton.setEnabled(true);
                    this.mRecordButton.setFocusable(true);
                    this.mPlayButton.setEnabled(true);
                    this.mPlayButton.setFocusable(true);
                    this.mStopButton.setEnabled(false);
                    this.mStopButton.setFocusable(false);
                    this.mStateMessage1.setVisibility(4);
                    this.mStateLED.setVisibility(4);
                    this.mStateMessage2.setVisibility(4);
                    this.mExitButtons.setVisibility(0);
                    this.mVUMeter.setVisibility(4);
                    this.mStateProgressBar.setVisibility(4);
                    setTitle(res.getString(R.string.message_recorded));
                }
                if (this.mSampleInterrupted) {
                    this.mStateMessage2.setVisibility(0);
                    this.mStateMessage2.setText(res.getString(R.string.recording_stopped));
                    this.mStateLED.setVisibility(4);
                }
                if (this.mErrorUiMessage != null) {
                    this.mStateMessage1.setText(this.mErrorUiMessage);
                    this.mStateMessage1.setVisibility(0);
                }
                break;
            case 1:
                this.mRecordButton.setEnabled(false);
                this.mRecordButton.setFocusable(false);
                this.mPlayButton.setEnabled(false);
                this.mPlayButton.setFocusable(false);
                this.mStopButton.setEnabled(true);
                this.mStopButton.setFocusable(true);
                this.mStateMessage1.setVisibility(0);
                this.mStateLED.setVisibility(0);
                this.mStateLED.setImageResource(R.drawable.recording_led);
                this.mStateMessage2.setVisibility(0);
                this.mStateMessage2.setText(res.getString(R.string.recording));
                this.mExitButtons.setVisibility(4);
                this.mVUMeter.setVisibility(0);
                this.mStateProgressBar.setVisibility(4);
                setTitle(res.getString(R.string.record_your_message));
                break;
            case 2:
                this.mRecordButton.setEnabled(true);
                this.mRecordButton.setFocusable(true);
                this.mPlayButton.setEnabled(false);
                this.mPlayButton.setFocusable(false);
                this.mStopButton.setEnabled(true);
                this.mStopButton.setFocusable(true);
                this.mStateMessage1.setVisibility(4);
                this.mStateLED.setVisibility(4);
                this.mStateMessage2.setVisibility(4);
                this.mExitButtons.setVisibility(0);
                this.mVUMeter.setVisibility(4);
                this.mStateProgressBar.setVisibility(0);
                setTitle(res.getString(R.string.review_message));
                break;
        }
        updateTimerView();
        this.mVUMeter.invalidate();
    }

    @Override
    public void onStateChanged(int state) {
        if (state == 2 || state == 1) {
            this.mSampleInterrupted = false;
            this.mErrorUiMessage = null;
            this.mWakeLock.acquire();
        } else if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        updateUi();
    }

    @Override
    public void onError(int error) {
        Resources res = getResources();
        String message = null;
        switch (error) {
            case 1:
                message = res.getString(R.string.error_sdcard_access);
                break;
            case 2:
            case 3:
                message = res.getString(R.string.error_app_internal);
                break;
        }
        if (message != null) {
            new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(message).setPositiveButton(R.string.button_ok, (DialogInterface.OnClickListener) null).setCancelable(false).show();
        }
    }
}
