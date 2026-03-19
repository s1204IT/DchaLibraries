package com.android.soundrecorder;

import android.app.Activity;
import android.app.AlertDialog;
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
    public void onCreate(Bundle bundle) {
        Bundle bundle2;
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent != null) {
            String type = intent.getType();
            if ("audio/amr".equals(type) || "audio/3gpp".equals(type) || "audio/*".equals(type) || "*/*".equals(type)) {
                this.mRequestedType = type;
            } else if (type != null) {
                setResult(0);
                finish();
                return;
            }
            this.mMaxFileSize = intent.getLongExtra("android.provider.MediaStore.extra.MAX_BYTES", -1L);
        }
        if ("audio/*".equals(this.mRequestedType) || "*/*".equals(this.mRequestedType)) {
            this.mRequestedType = "audio/3gpp";
        }
        setContentView(R.layout.main);
        this.mRecorder = new Recorder();
        this.mRecorder.setOnStateChangedListener(this);
        this.mRemainingTimeCalculator = new RemainingTimeCalculator();
        this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(6, "SoundRecorder");
        initResourceRefs();
        setResult(0);
        registerExternalStorageListener();
        if (bundle != null && (bundle2 = bundle.getBundle("recorder_state")) != null) {
            this.mRecorder.restoreState(bundle2);
            this.mSampleInterrupted = bundle2.getBoolean("sample_interrupted", false);
            this.mMaxFileSize = bundle2.getLong("max_file_size", -1L);
        }
        updateUi();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        setContentView(R.layout.main);
        initResourceRefs();
        updateUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mRecorder.sampleLength() == 0) {
            return;
        }
        Bundle bundle2 = new Bundle();
        this.mRecorder.saveState(bundle2);
        bundle2.putBoolean("sample_interrupted", this.mSampleInterrupted);
        bundle2.putLong("max_file_size", this.mMaxFileSize);
        bundle.putBundle("recorder_state", bundle2);
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
        ((AudioManager) getSystemService("audio")).requestAudioFocus(null, 3, 1);
    }

    @Override
    public void onClick(View view) {
        if (!view.isEnabled()) {
            return;
        }
        switch (view.getId()) {
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
                    this.mRecorder.startRecording(3, ".amr", this);
                } else if ("audio/3gpp".equals(this.mRequestedType)) {
                    this.mRemainingTimeCalculator.setBitRate(5900);
                    this.mRecorder.startRecording(1, ".3gpp", this);
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

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
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
        return super.onKeyDown(i, keyEvent);
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
        if (this.mRecorder.sampleLength() == 0) {
            return;
        }
        try {
            Uri uriAddToMediaDB = addToMediaDB(this.mRecorder.sampleFile());
            if (uriAddToMediaDB == null) {
                return;
            }
            setResult(-1, new Intent().setData(uriAddToMediaDB).setFlags(1));
        } catch (UnsupportedOperationException e) {
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
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MEDIA_EJECT");
            intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
            intentFilter.addDataScheme("file");
            registerReceiver(this.mSDCardMountEventReceiver, intentFilter);
        }
    }

    private Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        try {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                return null;
            }
            return contentResolver.query(uri, strArr, str, strArr2, str2);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    private void addToPlaylist(ContentResolver contentResolver, int i, long j) {
        Uri contentUri = MediaStore.Audio.Playlists.Members.getContentUri("external", j);
        Cursor cursorQuery = contentResolver.query(contentUri, new String[]{"count(*)"}, null, null, null);
        cursorQuery.moveToFirst();
        int i2 = cursorQuery.getInt(0);
        cursorQuery.close();
        ContentValues contentValues = new ContentValues();
        contentValues.put("play_order", Integer.valueOf(i2 + i));
        contentValues.put("audio_id", Integer.valueOf(i));
        contentResolver.insert(contentUri, contentValues);
    }

    private int getPlaylistId(Resources resources) {
        Cursor cursorQuery = query(MediaStore.Audio.Playlists.getContentUri("external"), new String[]{"_id"}, "name=?", new String[]{resources.getString(R.string.audio_db_playlist_name)}, null);
        if (cursorQuery == null) {
            Log.v("SoundRecorder", "query returns null");
        }
        int i = -1;
        if (cursorQuery != null) {
            cursorQuery.moveToFirst();
            if (!cursorQuery.isAfterLast()) {
                i = cursorQuery.getInt(0);
            }
        }
        cursorQuery.close();
        return i;
    }

    private Uri createPlaylist(Resources resources, ContentResolver contentResolver) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", resources.getString(R.string.audio_db_playlist_name));
        Uri uriInsert = contentResolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), contentValues);
        if (uriInsert == null) {
            new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.error_mediadb_new_record).setPositiveButton(R.string.button_ok, (DialogInterface.OnClickListener) null).setCancelable(false).show();
        }
        return uriInsert;
    }

    private Uri addToMediaDB(File file) {
        Resources resources = getResources();
        ContentValues contentValues = new ContentValues();
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jLastModified = file.lastModified();
        String str = new SimpleDateFormat(resources.getString(R.string.audio_db_title_format)).format(new Date(jCurrentTimeMillis));
        long jSampleLength = ((long) this.mRecorder.sampleLength()) * 1000;
        contentValues.put("is_music", "0");
        contentValues.put("title", str);
        contentValues.put("_data", file.getAbsolutePath());
        contentValues.put("date_added", Integer.valueOf((int) (jCurrentTimeMillis / 1000)));
        contentValues.put("date_modified", Integer.valueOf((int) (jLastModified / 1000)));
        contentValues.put("duration", Long.valueOf(jSampleLength));
        contentValues.put("mime_type", this.mRequestedType);
        contentValues.put("artist", resources.getString(R.string.audio_db_artist_name));
        contentValues.put("album", resources.getString(R.string.audio_db_album_name));
        Log.d("SoundRecorder", "Inserting audio record: " + contentValues.toString());
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.d("SoundRecorder", "ContentURI: " + uri);
        Uri uriInsert = contentResolver.insert(uri, contentValues);
        if (uriInsert == null) {
            new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.error_mediadb_new_record).setPositiveButton(R.string.button_ok, (DialogInterface.OnClickListener) null).setCancelable(false).show();
            return null;
        }
        if (getPlaylistId(resources) == -1) {
            createPlaylist(resources, contentResolver);
        }
        addToPlaylist(contentResolver, Integer.valueOf(uriInsert.getLastPathSegment()).intValue(), getPlaylistId(resources));
        sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", uriInsert));
        return uriInsert;
    }

    private void updateTimerView() {
        getResources();
        int iState = this.mRecorder.state();
        boolean z = iState == 1 || iState == 2;
        long jProgress = z ? this.mRecorder.progress() : this.mRecorder.sampleLength();
        this.mTimerView.setText(String.format(this.mTimerFormat, Long.valueOf(jProgress / 60), Long.valueOf(jProgress % 60)));
        if (iState == 2) {
            this.mStateProgressBar.setProgress((int) ((100 * jProgress) / ((long) this.mRecorder.sampleLength())));
        } else if (iState == 1) {
            updateTimeRemaining();
        }
        if (z) {
            this.mHandler.postDelayed(this.mUpdateTimer, 1000L);
        }
    }

    private void updateTimeRemaining() {
        long jTimeRemaining = this.mRemainingTimeCalculator.timeRemaining();
        if (jTimeRemaining <= 0) {
            this.mSampleInterrupted = true;
            switch (this.mRemainingTimeCalculator.currentLowerLimit()) {
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
        Resources resources = getResources();
        String str = "";
        if (jTimeRemaining < 60) {
            str = String.format(resources.getString(R.string.sec_available), Long.valueOf(jTimeRemaining));
        } else if (jTimeRemaining < 540) {
            str = String.format(resources.getString(R.string.min_available), Long.valueOf((jTimeRemaining / 60) + 1));
        }
        this.mStateMessage1.setText(str);
    }

    private void updateUi() {
        Resources resources = getResources();
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
                    setTitle(resources.getString(R.string.record_your_message));
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
                    setTitle(resources.getString(R.string.message_recorded));
                }
                if (this.mSampleInterrupted) {
                    this.mStateMessage2.setVisibility(0);
                    this.mStateMessage2.setText(resources.getString(R.string.recording_stopped));
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
                this.mStateMessage2.setText(resources.getString(R.string.recording));
                this.mExitButtons.setVisibility(4);
                this.mVUMeter.setVisibility(0);
                this.mStateProgressBar.setVisibility(4);
                setTitle(resources.getString(R.string.record_your_message));
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
                setTitle(resources.getString(R.string.review_message));
                break;
        }
        updateTimerView();
        this.mVUMeter.invalidate();
    }

    @Override
    public void onStateChanged(int i) {
        if (i == 2 || i == 1) {
            this.mSampleInterrupted = false;
            this.mErrorUiMessage = null;
            this.mWakeLock.acquire();
        } else if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        updateUi();
    }

    @Override
    public void onError(int i) {
        String string;
        Resources resources = getResources();
        switch (i) {
            case 1:
                string = resources.getString(R.string.error_sdcard_access);
                break;
            case 2:
            case 3:
                string = resources.getString(R.string.error_app_internal);
                break;
            default:
                string = null;
                break;
        }
        if (string != null) {
            new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(string).setPositiveButton(R.string.button_ok, (DialogInterface.OnClickListener) null).setCancelable(false).show();
        }
    }
}
