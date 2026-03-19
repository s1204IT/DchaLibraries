package com.mediatek.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.Log;
import android.widget.Toast;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.R;
import com.mediatek.internal.telecom.ICallRecorderCallback;
import com.mediatek.internal.telecom.ICallRecorderService;

public class CallRecorderManager {
    private static final String TAG = CallRecorderManager.class.getSimpleName();
    private ICallRecorderService mCallRecorderService;
    private final CallsManager mCallsManager;
    private final Context mContext;
    private RecordStateListener mListener;
    private int mRecordingState = 0;
    private Call mRecordingCall = null;
    private Call mPendingStopRecordCall = null;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CallRecorderManager.this.logd("[onServiceConnected]");
            CallRecorderManager.this.mHandler.obtainMessage(1, iBinder).sendToTarget();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            CallRecorderManager.this.logd("[onServiceDisconnected]");
            CallRecorderManager.this.mHandler.obtainMessage(2).sendToTarget();
        }
    };
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    CallRecorderManager.this.handleServiceConnected((IBinder) message.obj);
                    break;
                case CallState.SELECT_PHONE_ACCOUNT:
                    CallRecorderManager.this.handleServiceDisconnected();
                    break;
                case CallState.DIALING:
                    CallRecorderManager.this.handleStartRecord((Call) message.obj);
                    break;
                case CallState.RINGING:
                    CallRecorderManager.this.handleStopRecord((Call) message.obj);
                    break;
                case CallState.ACTIVE:
                    CallRecorderManager.this.handleRecordStateChanged(((Integer) message.obj).intValue());
                    break;
                case CallState.ON_HOLD:
                    CallRecorderManager.this.handleRecordeEvent(message.arg1, (String) message.obj);
                    break;
            }
        }
    };

    public interface RecordStateListener {
        void onRecordStateChanged(int i);
    }

    private synchronized void setRecordingState(int i) {
        logd("setRecordingState to " + recordStateToString(i));
        this.mRecordingState = i;
    }

    private synchronized int getRecordingState() {
        return this.mRecordingState;
    }

    private synchronized Call getPendingStopRecordCall() {
        return this.mPendingStopRecordCall;
    }

    private synchronized void setPendingStopRecordCall(Call call) {
        this.mPendingStopRecordCall = call;
    }

    private boolean canStartRecord() {
        return getRecordingState() == 0;
    }

    private boolean canStopRecord() {
        return getRecordingState() == 2;
    }

    private boolean needPendingStopRecord() {
        return getRecordingState() == 1;
    }

    private String recordStateToString(int i) {
        switch (i) {
            case CallState.NEW:
                return "RECORD_STATE_IDLE";
            case 1:
                return "RECORD_STATE_STARTING";
            case CallState.SELECT_PHONE_ACCOUNT:
                return "RECORD_STATE_STARTED";
            case CallState.DIALING:
                return "RECORD_STATE_STOPING";
            default:
                return "Unknown message";
        }
    }

    private void handleStartRecord(Call call) {
        logd("[handleStartRecord] on call " + call.getId());
        if (getRecordingState() != 1 && getRecordingState() != 0) {
            logw("[handleStartRecord] return without start, mPendingRequest=" + getRecordingState() + ", mRecordingCall=" + this.mRecordingCall);
            return;
        }
        if (call.getState() != 5) {
            logw("[handleStartRecord]call not active: " + call.getState());
            setRecordingState(0);
            return;
        }
        if (this.mCallsManager.getForegroundCall() != call) {
            logw("[handleStartRecord]call not foreground");
            setRecordingState(0);
            return;
        }
        this.mRecordingCall = call;
        setRecordingState(1);
        if (this.mCallRecorderService != null) {
            startVoiceRecordInternal();
            return;
        }
        logd("[handleStartRecord]start bind");
        Intent intent = new Intent("mediatek.telecom.action.CALL_RECORD");
        intent.setComponent(new ComponentName("com.mediatek.callrecorder", "com.mediatek.callrecorder.CallRecorderService"));
        if (!this.mContext.bindServiceAsUser(intent, this.mConnection, 67108865, UserHandle.SYSTEM)) {
            MtkTelecomGlobals.getInstance().showToast(R.string.start_record_failed);
            this.mRecordingCall = null;
            this.mContext.unbindService(this.mConnection);
            setRecordingState(0);
        }
    }

    private void handleStopRecord(Call call) {
        logd("[handleStopRecord] on call " + call.getId());
        if (getRecordingState() != 3 && getRecordingState() != 2) {
            logw("[handleStopRecord] unexpected state, just return");
            return;
        }
        if (this.mRecordingCall == null) {
            logw("[handleStopRecord] no call recording");
            setRecordingState(0);
            return;
        }
        if (this.mCallRecorderService == null) {
            logw("[handleStopRecord] call recorder service not connected");
            setRecordingState(0);
            return;
        }
        if (call != null && this.mRecordingCall != call) {
            logw("[handleStopRecord] state machine wrong, trying to stop a call which is notin recording state: " + this.mRecordingCall.getId() + " vs " + call.getId());
        }
        try {
            if (this.mCallRecorderService == null) {
                logw("[handleStopRecord] call recorder service not connected");
                setRecordingState(0);
            } else {
                setRecordingState(3);
                this.mCallRecorderService.stopVoiceRecord();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            setRecordingState(0);
        }
    }

    private void handleServiceConnected(IBinder iBinder) {
        this.mCallRecorderService = ICallRecorderService.Stub.asInterface(iBinder);
        startVoiceRecordInternal();
    }

    private void handleServiceDisconnected() {
        logd("[handleServiceDisconnected]");
        if (this.mRecordingCall != null && this.mListener != null) {
            logd("handleServiceDisconnected mRecordingCall not null, do error handling");
            this.mListener.onRecordStateChanged(0);
        }
        this.mRecordingCall = null;
        this.mCallRecorderService = null;
        setRecordingState(0);
        setListener(null);
    }

    private void handleRecordStateChanged(int i) {
        logd("[handleRecordStateChanged]");
        if (this.mRecordingCall != null && this.mListener != null) {
            this.mListener.onRecordStateChanged(i);
        }
        if (i == 0) {
            this.mContext.unbindService(this.mConnection);
            this.mRecordingCall = null;
            this.mCallRecorderService = null;
            setListener(null);
            setRecordingState(0);
            return;
        }
        if (i == 1) {
            setRecordingState(2);
            Call pendingStopRecordCall = getPendingStopRecordCall();
            if (pendingStopRecordCall != null && pendingStopRecordCall == this.mRecordingCall) {
                logd("handlePendingStopRecord");
                this.mHandler.obtainMessage(4, pendingStopRecordCall).sendToTarget();
            }
            setPendingStopRecordCall(null);
        }
    }

    private void handleRecordeEvent(int i, String str) {
        logd("[handleRecordeEvent]event: " + i);
        if (i == 0) {
            Toast toastMakeText = Toast.makeText(this.mContext, str, 1);
            toastMakeText.getWindowParams().flags |= 524288;
            toastMakeText.show();
        }
    }

    public CallRecorderManager(Context context, CallsManager callsManager) {
        this.mContext = context;
        this.mCallsManager = callsManager;
    }

    public boolean startVoiceRecord(Call call) {
        if (!canStartRecord()) {
            logd("[startVoiceRecord] fail, record state is " + recordStateToString(getRecordingState()));
            return false;
        }
        this.mRecordingCall = call;
        setRecordingState(1);
        setPendingStopRecordCall(null);
        logd("[startVoiceRecord] on call " + call.getId());
        this.mHandler.obtainMessage(3, call).sendToTarget();
        return true;
    }

    public void stopVoiceRecord(Call call) {
        if (!canStopRecord()) {
            if (needPendingStopRecord()) {
                logd("[stopVoiceRecord] pending, record state is" + recordStateToString(getRecordingState()));
                setPendingStopRecordCall(call);
                return;
            }
            logd("[stopVoiceRecord] fail, record state is" + recordStateToString(getRecordingState()));
            return;
        }
        setRecordingState(3);
        setPendingStopRecordCall(null);
        this.mHandler.obtainMessage(4, call).sendToTarget();
    }

    private void startVoiceRecordInternal() {
        if (this.mCallRecorderService == null) {
            return;
        }
        try {
            this.mCallRecorderService.setCallback(new Callback());
            if (getRecordingState() == 1) {
                this.mCallRecorderService.startVoiceRecord();
            } else if (getRecordingState() == 2 || getRecordingState() == 3) {
                logw("handleServiceConnected, unexpeted state %d" + getRecordingState());
                setRecordingState(0);
            }
        } catch (RemoteException e) {
            this.mRecordingCall = null;
            this.mCallRecorderService = null;
            setRecordingState(0);
            e.printStackTrace();
        }
    }

    private class Callback extends ICallRecorderCallback.Stub {
        private Callback() {
        }

        public void onRecordStateChanged(int i) throws RemoteException {
            CallRecorderManager.this.logd("[onRecordStateChanged] state: " + i);
            CallRecorderManager.this.mHandler.obtainMessage(5, Integer.valueOf(i)).sendToTarget();
        }

        public void onRecordEvent(int i, String str) throws RemoteException {
            CallRecorderManager.this.logd("[onRecordEvent] event: " + i);
            CallRecorderManager.this.mHandler.obtainMessage(6, i, 0, str).sendToTarget();
        }
    }

    public void setListener(RecordStateListener recordStateListener) {
        this.mListener = recordStateListener;
    }

    private void logd(String str) {
        Log.d(TAG, str, new Object[0]);
    }

    private void logw(String str) {
        Log.w(TAG, str, new Object[0]);
    }

    public boolean isCallRecorderInstalled() {
        boolean z = false;
        try {
            this.mContext.getPackageManager().getApplicationInfo("com.mediatek.callrecorder", 0);
            z = true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        logd("[isCallRecorderInstalled] " + z);
        return z;
    }
}
