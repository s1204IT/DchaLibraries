package com.mediatek.callrecorder;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Recorder implements MediaRecorder.OnErrorListener {
    private static final String TAG = Recorder.class.getSimpleName();
    static boolean sIsRecording;
    Context mContext;
    private OnEventListener mOnEventListener;
    private OnStateChangedListener mOnStateChangedListener;
    protected String mRecordStoragePath;
    private MediaRecorder mRecorder;
    protected File mSampleFile;
    protected long mSampleLength;
    private long mSampleStart;
    private int mState = 0;
    final int SHOW_TOAST = 1;
    Handler mMainThreadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                Toast toastMakeText = Toast.makeText(Recorder.this.mContext, (String) message.obj, 1);
                toastMakeText.getWindowParams().flags |= 524288;
                toastMakeText.show();
            }
        }
    };

    public interface OnEventListener {
        void onEvent(int i, String str);
    }

    public interface OnStateChangedListener {
        void onStateChanged(int i);
    }

    protected abstract void onMediaServiceError();

    protected Recorder(Context context) {
        this.mContext = context;
    }

    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        this.mOnStateChangedListener = onStateChangedListener;
    }

    public void setEventListener(OnEventListener onEventListener) {
        this.mOnEventListener = onEventListener;
    }

    protected void deleteSampleFile() {
        if (this.mSampleFile != null) {
            this.mSampleFile.delete();
        }
        this.mSampleFile = null;
        this.mSampleLength = 0L;
    }

    public void startRecording(int i, String str) throws IOException {
        log("startRecording");
        String str2 = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        if (!externalStorageDirectory.canWrite()) {
            Slog.i(TAG, "----- file can't write!! ---");
            externalStorageDirectory = new File("/sdcard");
        }
        File file = new File(externalStorageDirectory.getAbsolutePath() + "/PhoneRecord");
        if (!file.exists()) {
            file.mkdirs();
        }
        log("sampleDir path is " + file.getAbsolutePath());
        this.mRecordStoragePath = file.getCanonicalPath();
        try {
            this.mSampleFile = File.createTempFile(str2, str, file);
            log("finish creating temp file, start to record");
            this.mRecorder = new MediaRecorder();
            this.mRecorder.setOnErrorListener(this);
            String str3 = SystemProperties.get("persist.vendor.incallrec.audiosource", "-1");
            log("recordType is: " + Integer.parseInt(str3));
            if (str3.equals("-1")) {
                this.mRecorder.setAudioSource(4);
                this.mRecorder.setAudioChannels(2);
            } else {
                this.mRecorder.setAudioSource(Integer.parseInt(str3));
                if (str3.equals("4")) {
                    this.mRecorder.setAudioChannels(2);
                } else {
                    this.mRecorder.setAudioChannels(1);
                }
            }
            this.mRecorder.setOutputFormat(i);
            this.mRecorder.setAudioEncoder(3);
            this.mRecorder.setAudioEncodingBitRate(24000);
            this.mRecorder.setAudioSamplingRate(16000);
            this.mRecorder.setOutputFile(this.mSampleFile.getAbsolutePath());
            try {
                this.mRecorder.prepare();
                this.mRecorder.start();
                this.mSampleStart = System.currentTimeMillis();
                setState(1);
            } catch (Exception e) {
                log("startRecording, encounter exception");
                handleException();
                deleteSampleFile();
            }
        } catch (IOException e2) {
            showToast(R.string.error_sdcard_access);
            Slog.i(TAG, "----***------- can't access sdcard !! " + e2);
            e2.printStackTrace();
            throw e2;
        }
    }

    private void handleException() {
        if (sIsRecording) {
            sIsRecording = false;
        }
        showToast(R.string.alert_device_error);
        this.mRecorder.reset();
        this.mRecorder.release();
        this.mRecorder = null;
        setState(0, true);
    }

    public void stopRecording() {
        log("stopRecording");
        if (this.mRecorder == null) {
            return;
        }
        this.mSampleLength = System.currentTimeMillis() - this.mSampleStart;
        try {
            this.mRecorder.stop();
        } catch (RuntimeException e) {
            e.printStackTrace();
            deleteSampleFile();
        }
        this.mRecorder.release();
        this.mRecorder = null;
    }

    void setState(int i) {
        setState(i, false);
    }

    void setState(int i, boolean z) {
        if (i != this.mState || z) {
            this.mState = i;
            fireStateChanged(this.mState);
        }
    }

    private void fireStateChanged(int i) {
        log("fireStateChanged " + i);
        if (this.mOnStateChangedListener != null) {
            this.mOnStateChangedListener.onStateChanged(i);
        }
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int i, int i2) {
        log("onError");
        if (i == 1) {
            onMediaServiceError();
        }
    }

    public String getRecordingPath() {
        return this.mRecordStoragePath;
    }

    private void log(String str) {
        Slog.d(TAG, str);
    }

    void showToast(int i) {
        showToast(this.mContext.getResources().getString(i));
    }

    void showToast(String str) {
        this.mMainThreadHandler.obtainMessage(1, str).sendToTarget();
        log("showToast");
    }

    void showToastInClient(String str) {
        if (this.mOnEventListener != null) {
            log("showToastInClient");
            this.mOnEventListener.onEvent(0, str);
        }
    }

    void showToastInClient(int i) {
        showToastInClient(this.mContext.getResources().getString(i));
    }
}
