package com.android.server.telecom;

import android.os.Handler;
import android.os.Message;
import android.telecom.PhoneAccountHandle;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IInCallAdapter;

class InCallAdapter extends IInCallAdapter.Stub {
    private final CallIdMapper mCallIdMapper;
    private final CallsManager mCallsManager;
    private final Handler mHandler = new InCallAdapterHandler();

    private final class InCallAdapterHandler extends Handler {
        private InCallAdapterHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            SomeArgs someArgs;
            switch (message.what) {
                case 0:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        Call call = InCallAdapter.this.mCallIdMapper.getCall(someArgs.arg1);
                        int iIntValue = ((Integer) someArgs.arg2).intValue();
                        if (call != null) {
                            InCallAdapter.this.mCallsManager.answerCall(call, iIntValue);
                        } else {
                            Log.w(this, "answerCall, unknown call id: %s", message.obj);
                        }
                        return;
                    } finally {
                    }
                case 1:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        Call call2 = InCallAdapter.this.mCallIdMapper.getCall(someArgs.arg1);
                        boolean z = someArgs.argi1 == 1;
                        String str = (String) someArgs.arg2;
                        if (call2 != null) {
                            InCallAdapter.this.mCallsManager.rejectCall(call2, z, str);
                        } else {
                            Log.w(this, "setRingback, unknown call id: %s", someArgs.arg1);
                        }
                        return;
                    } finally {
                    }
                case 2:
                    Call call3 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call3 != null) {
                        InCallAdapter.this.mCallsManager.playDtmfTone(call3, (char) message.arg1);
                        return;
                    } else {
                        Log.w(this, "playDtmfTone, unknown call id: %s", message.obj);
                        return;
                    }
                case 3:
                    Call call4 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call4 != null) {
                        InCallAdapter.this.mCallsManager.stopDtmfTone(call4);
                        return;
                    } else {
                        Log.w(this, "stopDtmfTone, unknown call id: %s", message.obj);
                        return;
                    }
                case 4:
                    Call call5 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call5 != null) {
                        InCallAdapter.this.mCallsManager.postDialContinue(call5, message.arg1 == 1);
                        return;
                    } else {
                        Log.w(this, "postDialContinue, unknown call id: %s", message.obj);
                        return;
                    }
                case 5:
                    Call call6 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call6 != null) {
                        InCallAdapter.this.mCallsManager.disconnectCall(call6);
                        return;
                    } else {
                        Log.w(this, "disconnectCall, unknown call id: %s", message.obj);
                        return;
                    }
                case 6:
                    Call call7 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call7 != null) {
                        InCallAdapter.this.mCallsManager.holdCall(call7);
                        return;
                    } else {
                        Log.w(this, "holdCall, unknown call id: %s", message.obj);
                        return;
                    }
                case 7:
                    Call call8 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call8 != null) {
                        InCallAdapter.this.mCallsManager.unholdCall(call8);
                        return;
                    } else {
                        Log.w(this, "unholdCall, unknown call id: %s", message.obj);
                        return;
                    }
                case 8:
                    InCallAdapter.this.mCallsManager.mute(message.arg1 == 1);
                    return;
                case 9:
                    InCallAdapter.this.mCallsManager.setAudioRoute(message.arg1);
                    return;
                case 10:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        Call call9 = InCallAdapter.this.mCallIdMapper.getCall(someArgs.arg1);
                        Call call10 = InCallAdapter.this.mCallIdMapper.getCall(someArgs.arg2);
                        if (call9 != null && call10 != null) {
                            InCallAdapter.this.mCallsManager.conference(call9, call10);
                        } else {
                            Log.w(this, "conference, unknown call id: %s", message.obj);
                        }
                        return;
                    } finally {
                    }
                case 11:
                    Call call11 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call11 != null) {
                        call11.splitFromConference();
                        return;
                    } else {
                        Log.w(this, "splitFromConference, unknown call id: %s", message.obj);
                        return;
                    }
                case 12:
                default:
                    return;
                case 13:
                    someArgs = (SomeArgs) message.obj;
                    try {
                        Call call12 = InCallAdapter.this.mCallIdMapper.getCall(someArgs.arg1);
                        if (call12 != null) {
                            InCallAdapter.this.mCallsManager.phoneAccountSelected(call12, (PhoneAccountHandle) someArgs.arg2, someArgs.argi1 == 1);
                        } else {
                            Log.w(this, "phoneAccountSelected, unknown call id: %s", someArgs.arg1);
                        }
                        return;
                    } finally {
                    }
                case 14:
                    InCallAdapter.this.mCallsManager.turnOnProximitySensor();
                    return;
                case 15:
                    InCallAdapter.this.mCallsManager.turnOffProximitySensor(((Boolean) message.obj).booleanValue());
                    return;
                case 16:
                    Call call13 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call13 != null) {
                        call13.mergeConference();
                        return;
                    } else {
                        Log.w(this, "mergeConference, unknown call id: %s", message.obj);
                        return;
                    }
                case 17:
                    Call call14 = InCallAdapter.this.mCallIdMapper.getCall(message.obj);
                    if (call14 != null) {
                        call14.swapConference();
                        return;
                    } else {
                        Log.w(this, "swapConference, unknown call id: %s", message.obj);
                        return;
                    }
            }
        }
    }

    public InCallAdapter(CallsManager callsManager, CallIdMapper callIdMapper) {
        ThreadUtil.checkOnMainThread();
        this.mCallsManager = callsManager;
        this.mCallIdMapper = callIdMapper;
    }

    public void answerCall(String str, int i) {
        Log.d(this, "answerCall(%s,%d)", str, Integer.valueOf(i));
        if (this.mCallIdMapper.isValidCallId(str)) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = Integer.valueOf(i);
            this.mHandler.obtainMessage(0, someArgsObtain).sendToTarget();
        }
    }

    public void rejectCall(String str, boolean z, String str2) {
        Log.d(this, "rejectCall(%s,%b,%s)", str, Boolean.valueOf(z), str2);
        if (this.mCallIdMapper.isValidCallId(str)) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.argi1 = z ? 1 : 0;
            someArgsObtain.arg2 = str2;
            this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
        }
    }

    public void playDtmfTone(String str, char c) {
        Log.d(this, "playDtmfTone(%s,%c)", str, Character.valueOf(c));
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(2, c, 0, str).sendToTarget();
        }
    }

    public void stopDtmfTone(String str) {
        Log.d(this, "stopDtmfTone(%s)", str);
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(3, str).sendToTarget();
        }
    }

    public void postDialContinue(String str, boolean z) {
        Log.d(this, "postDialContinue(%s)", str);
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(4, z ? 1 : 0, 0, str).sendToTarget();
        }
    }

    public void disconnectCall(String str) {
        Log.v(this, "disconnectCall: %s", str);
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(5, str).sendToTarget();
        }
    }

    public void holdCall(String str) {
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(6, str).sendToTarget();
        }
    }

    public void unholdCall(String str) {
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(7, str).sendToTarget();
        }
    }

    public void phoneAccountSelected(String str, PhoneAccountHandle phoneAccountHandle, boolean z) {
        if (this.mCallIdMapper.isValidCallId(str)) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = phoneAccountHandle;
            someArgsObtain.argi1 = z ? 1 : 0;
            this.mHandler.obtainMessage(13, someArgsObtain).sendToTarget();
        }
    }

    public boolean extraVolume(boolean z) {
        return this.mCallsManager.extraVolume(z);
    }

    public boolean record() {
        return this.mCallsManager.record();
    }

    public void mute(boolean z) {
        this.mHandler.obtainMessage(8, z ? 1 : 0, 0).sendToTarget();
    }

    public void setAudioRoute(int i) {
        this.mHandler.obtainMessage(9, i, 0).sendToTarget();
    }

    public void conference(String str, String str2) {
        if (this.mCallIdMapper.isValidCallId(str) && this.mCallIdMapper.isValidCallId(str2)) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            this.mHandler.obtainMessage(10, someArgsObtain).sendToTarget();
        }
    }

    public void splitFromConference(String str) {
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(11, str).sendToTarget();
        }
    }

    public void mergeConference(String str) {
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(16, str).sendToTarget();
        }
    }

    public void swapConference(String str) {
        if (this.mCallIdMapper.isValidCallId(str)) {
            this.mHandler.obtainMessage(17, str).sendToTarget();
        }
    }

    public void turnOnProximitySensor() {
        this.mHandler.obtainMessage(14).sendToTarget();
    }

    public void turnOffProximitySensor(boolean z) {
        this.mHandler.obtainMessage(15, Boolean.valueOf(z)).sendToTarget();
    }
}
