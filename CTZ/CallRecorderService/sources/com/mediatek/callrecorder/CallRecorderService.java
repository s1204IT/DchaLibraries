package com.mediatek.callrecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.storage.StorageVolume;
import android.util.Slog;
import com.mediatek.callrecorder.Recorder;
import com.mediatek.internal.telecom.ICallRecorderCallback;
import com.mediatek.internal.telecom.ICallRecorderService;

public class CallRecorderService extends Service {
    private static final int REQUEST_BIND_SERVICE = 3;
    private static final int REQUEST_QUIT = 6;
    private static final int REQUEST_SET_CALLBACK = 5;
    private static final int REQUEST_START_RECORDING = 1;
    private static final int REQUEST_STOP_RECORDING = 2;
    private static final int REQUEST_UNBIND_SERVICE = 4;
    private static final String TAG = CallRecorderService.class.getSimpleName();
    private CallRecorder mCallRecorder;
    private ICallRecorderCallback mCallback;
    private Handler mRecordHandler;
    private String mRecordStoragePath;
    private HandlerThread mWorkerThread;
    private IBinder mBinder = new ICallRecorderService.Stub() {
        public void startVoiceRecord() throws RemoteException {
            CallRecorderService.this.logd("[startVoiceRecord]");
            CallRecorderService.this.mRecordHandler.obtainMessage(CallRecorderService.REQUEST_START_RECORDING).sendToTarget();
        }

        public void stopVoiceRecord() throws RemoteException {
            CallRecorderService.this.logd("[stopVoiceRecord]");
            CallRecorderService.this.mRecordHandler.obtainMessage(CallRecorderService.REQUEST_STOP_RECORDING).sendToTarget();
        }

        public void setCallback(ICallRecorderCallback iCallRecorderCallback) throws RemoteException {
            CallRecorderService.this.logd("[setCallback]callback = " + iCallRecorderCallback);
            CallRecorderService.this.mRecordHandler.obtainMessage(CallRecorderService.REQUEST_SET_CALLBACK, iCallRecorderCallback).sendToTarget();
        }
    };
    private Recorder.OnStateChangedListener mCallRecorderStateListener = new Recorder.OnStateChangedListener() {
        @Override
        public void onStateChanged(int i) {
            try {
                CallRecorderService.this.mCallback.onRecordStateChanged(i);
                if (CallRecorderService.this.mRecordHandler != null && i == 0) {
                    CallRecorderService.this.logd("remove mRecordDiskCheck");
                    CallRecorderService.this.mRecordHandler.removeCallbacks(CallRecorderService.this.mRecordDiskCheck);
                }
            } catch (RemoteException e) {
                Slog.e(CallRecorderService.TAG, "CallRecordService: call listener onStateChanged() failed", e);
            }
        }
    };
    private Recorder.OnEventListener mCallRecorderEventListener = new Recorder.OnEventListener() {
        @Override
        public void onEvent(int i, String str) {
            try {
                CallRecorderService.this.mCallback.onRecordEvent(i, str);
            } catch (RemoteException e) {
                Slog.e(CallRecorderService.TAG, "CallRecordService: call listener onRecordEvent() failed", e);
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CallRecorderService.this.mCallRecorder != null) {
                if ("android.intent.action.MEDIA_EJECT".equals(intent.getAction()) || "android.intent.action.MEDIA_UNMOUNTED".equals(intent.getAction())) {
                    StorageVolume storageVolume = (StorageVolume) intent.getParcelableExtra("android.os.storage.extra.STORAGE_VOLUME");
                    if (storageVolume == null) {
                        CallRecorderService.this.logd("storageVolume is null");
                        return;
                    }
                    String path = storageVolume.getPath();
                    if (CallRecorderService.this.mRecordStoragePath == null || !path.equals(CallRecorderService.this.mRecordStoragePath)) {
                        CallRecorderService.this.logd("not current used storage unmount or eject");
                    } else if (CallRecorderService.this.mCallRecorder.isRecording()) {
                        CallRecorderService.this.mRecordHandler.removeCallbacks(CallRecorderService.this.mRecordDiskCheck);
                        CallRecorderService.this.logd("Current used sd card is ejected, stop voice record");
                        CallRecorderService.this.mRecordHandler.obtainMessage(CallRecorderService.REQUEST_STOP_RECORDING).sendToTarget();
                    }
                }
            }
        }
    };
    private Runnable mRecordDiskCheck = new Runnable() {
        @Override
        public void run() {
            CallRecorderService.this.checkRecordDisk();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        logd("[onBind]");
        this.mRecordHandler.obtainMessage(REQUEST_BIND_SERVICE).sendToTarget();
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        logd("[onUnbind]");
        this.mRecordHandler.obtainMessage(REQUEST_UNBIND_SERVICE).sendToTarget();
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logd("onCreate");
        this.mCallRecorder = CallRecorder.getInstance(this);
        this.mCallRecorder.setOnStateChangedListener(this.mCallRecorderStateListener);
        this.mCallRecorder.setEventListener(this.mCallRecorderEventListener);
        this.mWorkerThread = new HandlerThread("RecordWorker");
        this.mWorkerThread.start();
        this.mRecordHandler = new RecordHandler(this.mWorkerThread.getLooper());
        registerMediaStateReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logd("onDestroy");
        unregisterMediaStateReceiver();
        this.mRecordHandler.sendEmptyMessage(REQUEST_QUIT);
    }

    private void registerMediaStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    private void unregisterMediaStateReceiver() {
        if (this.mBroadcastReceiver != null) {
            unregisterReceiver(this.mBroadcastReceiver);
        }
    }

    private class RecordHandler extends Handler {
        public RecordHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            CallRecorderService.this.logd("[handleMessage] message = " + CallRecorderService.this.messageToStrings(message.what));
            switch (message.what) {
                case CallRecorderService.REQUEST_START_RECORDING:
                    if (CallRecorderService.this.mCallRecorder != null) {
                        CallRecorderService.this.logd("[handleMessage]do start recording");
                        CallRecorderService.this.mRecordStoragePath = RecorderUtils.getExternalStorageDefaultPath(CallRecorderService.this);
                        CallRecorderService.this.mCallRecorder.startRecord();
                    }
                    break;
                case CallRecorderService.REQUEST_STOP_RECORDING:
                    if (CallRecorderService.this.mCallRecorder != null) {
                        CallRecorderService.this.logd("[handleMessage]do stop recording");
                        CallRecorderService.this.mCallRecorder.stopRecord();
                    }
                    CallRecorderService.this.mRecordStoragePath = null;
                    break;
                case CallRecorderService.REQUEST_BIND_SERVICE:
                    removeCallbacks(CallRecorderService.this.mRecordDiskCheck);
                    postDelayed(CallRecorderService.this.mRecordDiskCheck, 500L);
                    break;
                case CallRecorderService.REQUEST_UNBIND_SERVICE:
                    CallRecorderService.this.mCallback = null;
                    CallRecorderService.this.mRecordHandler.removeCallbacks(CallRecorderService.this.mRecordDiskCheck);
                    break;
                case CallRecorderService.REQUEST_SET_CALLBACK:
                    CallRecorderService.this.mCallback = (ICallRecorderCallback) message.obj;
                    break;
                case CallRecorderService.REQUEST_QUIT:
                    CallRecorderService.this.logd("[handleMessage]quit worker thread and clear handler");
                    CallRecorderService.this.mWorkerThread.quit();
                    break;
                default:
                    CallRecorderService.this.logd("[handleMessage]unexpected message: " + message.what);
                    break;
            }
        }
    }

    private String messageToStrings(int i) {
        switch (i) {
            case REQUEST_START_RECORDING:
                return "REQUEST_START_RECORDING";
            case REQUEST_STOP_RECORDING:
                return "REQUEST_STOP_RECORDING";
            case REQUEST_BIND_SERVICE:
                return "REQUEST_BIND_SERVICE";
            case REQUEST_UNBIND_SERVICE:
                return "REQUEST_UNBIND_SERVICE";
            case REQUEST_SET_CALLBACK:
                return "REQUEST_SET_CALLBACK";
            case REQUEST_QUIT:
                return "REQUEST_QUIT";
            default:
                return "Unknown message";
        }
    }

    private void checkRecordDisk() {
        logd("checkRecordDisk " + this.mRecordStoragePath);
        if (this.mCallRecorder != null && this.mCallRecorder.isRecording() && !RecorderUtils.diskSpaceAvailable(this.mRecordStoragePath)) {
            this.mRecordHandler.removeCallbacks(this.mRecordDiskCheck);
            Slog.e("AN: ", "Checking result, disk is full, stop recording...");
            this.mRecordHandler.obtainMessage(REQUEST_STOP_RECORDING).sendToTarget();
            this.mCallRecorder.showToast(R.string.confirm_device_info_full);
            return;
        }
        this.mRecordHandler.postDelayed(this.mRecordDiskCheck, 50L);
    }

    private void logd(String str) {
        Slog.d(TAG, str);
    }
}
