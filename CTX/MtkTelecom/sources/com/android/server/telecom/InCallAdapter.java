package com.android.server.telecom;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.telecom.Log;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telecom.IInCallAdapter;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.server.telecom.MtkUtil;
import java.util.ArrayList;
import java.util.List;
import mediatek.telecom.MtkTelecomHelper;

class InCallAdapter extends IInCallAdapter.Stub {
    private final CallIdMapper mCallIdMapper;
    private final CallsManager mCallsManager;
    private final MtkTelecomHelper.MtkInCallAdapterHelper.ICommandProcessor mCommandProcessor = new MtkTelecomHelper.MtkInCallAdapterHelper.ICommandProcessor() {
        public void hangupHold() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    InCallAdapter.this.mCallsManager.hangupHoldCall();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void hangupAll() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    InCallAdapter.this.mCallsManager.hangupAll();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void explicitCallTransfer(String str) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    InCallAdapter.this.mCallIdMapper.getCall(str).explicitCallTransfer();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void inviteConferenceParticipants(String str, ArrayList<String> arrayList) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    InCallAdapter.this.mCallIdMapper.getCall(str).inviteNumbersToConference(arrayList);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void blindOrAssuredEct(String str, String str2, int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    Call call = InCallAdapter.this.mCallIdMapper.getCall(str);
                    call.explicitCallTransfer(call, str2, i);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void hangupActiveAndAnswerWaiting() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    InCallAdapter.this.mCallsManager.hangupActiveAndAnswerWaiting();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setSortedIncomingCallList(ArrayList<String> arrayList) {
            Call call;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (InCallAdapter.this.mLock) {
                    if (!arrayList.isEmpty()) {
                        call = InCallAdapter.this.mCallIdMapper.getCall(arrayList.get(0));
                    } else {
                        call = null;
                    }
                    InCallAdapter.this.mCallsManager.setForegroundIncomingCall(call);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void startVoiceRecording() {
            try {
                Log.startSession("ICA.startVR");
                if (!MtkUtil.canVoiceRecord(InCallAdapter.this.mOwnerComponentName, "voiceRecord")) {
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (InCallAdapter.this.mLock) {
                        InCallAdapter.this.mCallsManager.startVoiceRecord();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }

        public void stopVoiceRecording() {
            try {
                Log.startSession("ICA.stopVR");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (InCallAdapter.this.mLock) {
                        InCallAdapter.this.mCallsManager.stopVoiceRecord();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }

        public void deviceSwitch(String str, String str2, String str3) {
            try {
                Log.startSession("ICA.dS");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (InCallAdapter.this.mLock) {
                        Call call = InCallAdapter.this.mCallIdMapper.getCall(str);
                        call.deviceSwitch(call, str2, str3);
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }

        public void cancelDeviceSwitch(String str) {
            try {
                Log.startSession("ICA.cDS");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (InCallAdapter.this.mLock) {
                        Call call = InCallAdapter.this.mCallIdMapper.getCall(str);
                        call.cancelDeviceSwitch(call);
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } finally {
                Log.endSession();
            }
        }
    };
    private final TelecomSystem.SyncRoot mLock;
    private final String mOwnerComponentName;

    public InCallAdapter(CallsManager callsManager, CallIdMapper callIdMapper, TelecomSystem.SyncRoot syncRoot, String str) {
        this.mCallsManager = callsManager;
        this.mCallIdMapper = callIdMapper;
        this.mLock = syncRoot;
        this.mOwnerComponentName = str;
    }

    public void answerCall(String str, int i) {
        try {
            Log.startSession("ICA.aC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.d(this, "answerCall(%s,%d)", new Object[]{str, Integer.valueOf(i)});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.answerCall(call, i);
                    } else {
                        Log.w(this, "answerCall, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void deflectCall(String str, Uri uri) {
        try {
            Log.startSession("ICA.defC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.i(this, "deflectCall - %s, %s ", new Object[]{str, Log.pii(uri)});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.deflectCall(call, uri);
                    } else {
                        Log.w(this, "deflectCall, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void rejectCall(String str, boolean z, String str2) {
        try {
            Log.startSession("ICA.rC", this.mOwnerComponentName);
            if (!this.mCallsManager.isReplyWithSmsAllowed(Binder.getCallingUid())) {
                str2 = null;
                z = false;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.d(this, "rejectCall(%s,%b,%s)", new Object[]{str, Boolean.valueOf(z), str2});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.rejectCall(call, z, str2);
                    } else {
                        Log.w(this, "setRingback, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void playDtmfTone(String str, char c) {
        try {
            Log.startSession("ICA.pDT", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.d(this, "playDtmfTone(%s,%c)", new Object[]{str, Character.valueOf(c)});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.playDtmfTone(call, c);
                    } else {
                        Log.w(this, "playDtmfTone, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void stopDtmfTone(String str) {
        try {
            Log.startSession("ICA.sDT", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.d(this, "stopDtmfTone(%s)", new Object[]{str});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.stopDtmfTone(call);
                    } else {
                        Log.w(this, "stopDtmfTone, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void postDialContinue(String str, boolean z) {
        try {
            Log.startSession("ICA.pDC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.d(this, "postDialContinue(%s)", new Object[]{str});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.postDialContinue(call, z);
                    } else {
                        Log.w(this, "postDialContinue, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void disconnectCall(String str) {
        try {
            Log.startSession("ICA.dC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Log.v(this, "disconnectCall: %s", new Object[]{str});
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.disconnectCall(call);
                    } else {
                        Log.w(this, "disconnectCall, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void holdCall(String str) {
        try {
            Log.startSession("ICA.hC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.holdCall(call);
                    } else {
                        Log.w(this, "holdCall, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void unholdCall(String str) {
        try {
            Log.startSession("ICA.uC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.unholdCall(call);
                    } else {
                        Log.w(this, "unholdCall, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void phoneAccountSelected(String str, PhoneAccountHandle phoneAccountHandle, boolean z) {
        try {
            Log.startSession("ICA.pAS", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        this.mCallsManager.phoneAccountSelected(call, phoneAccountHandle, z);
                    } else {
                        Log.w(this, "phoneAccountSelected, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void mute(boolean z) {
        try {
            Log.startSession("ICA.m", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    this.mCallsManager.mute(z);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void setAudioRoute(int i, String str) {
        try {
            Log.startSession("ICA.sAR", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    this.mCallsManager.setAudioRoute(i, str);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void conference(String str, String str2) {
        try {
            Log.startSession("ICA.c", this.mOwnerComponentName);
            if (!MtkUtil.canConference(this.mOwnerComponentName, "conference")) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    Call call2 = this.mCallIdMapper.getCall(str2);
                    if (call != null && call2 != null) {
                        this.mCallsManager.conference(call, call2);
                    } else {
                        Log.w(this, "conference, unknown call id: %s or %s", new Object[]{str, str2});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void splitFromConference(String str) {
        try {
            Log.startSession("ICA.sFC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.splitFromConference();
                    } else {
                        Log.w(this, "splitFromConference, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void mergeConference(String str) {
        try {
            Log.startSession("ICA.mC", this.mOwnerComponentName);
            if (!MtkUtil.canConference(this.mOwnerComponentName, "mergeConference")) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.mergeConference();
                    } else {
                        Log.w(this, "mergeConference, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void swapConference(String str) {
        try {
            Log.startSession("ICA.sC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.swapConference();
                    } else {
                        Log.w(this, "swapConference, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void pullExternalCall(String str) {
        try {
            Log.startSession("ICA.pEC", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.pullExternalCall();
                    } else {
                        Log.w(this, "pullExternalCall, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void sendCallEvent(String str, String str2, int i, Bundle bundle) {
        try {
            Log.startSession("ICA.sCE", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.sendCallEvent(str2, i, bundle);
                    } else {
                        Log.w(this, "sendCallEvent, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void putExtras(String str, Bundle bundle) {
        try {
            Log.startSession("ICA.pE", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.putExtras(2, bundle);
                    } else {
                        Log.w(this, "putExtras, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void removeExtras(String str, List<String> list) {
        try {
            Log.startSession("ICA.rE", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.removeExtras(2, list);
                    } else {
                        Log.w(this, "removeExtra, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void turnOnProximitySensor() {
        try {
            Log.startSession("ICA.tOnPS", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    this.mCallsManager.turnOnProximitySensor();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void turnOffProximitySensor(boolean z) {
        try {
            Log.startSession("ICA.tOffPS", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    this.mCallsManager.turnOffProximitySensor(z);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void sendRttRequest(String str) {
        try {
            Log.startSession("ICA.sRR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.sendRttRequest();
                    } else {
                        Log.w(this, "stopRtt(): call %s not found", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void respondToRttRequest(String str, int i, boolean z) {
        try {
            Log.startSession("ICA.rTRR");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.handleRttRequestResponse(i, z);
                    } else {
                        Log.w(this, "respondToRttRequest(): call %s not found", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void stopRtt(String str) {
        try {
            Log.startSession("ICA.sRTT");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.stopRtt();
                    } else {
                        Log.w(this, "stopRtt(): call %s not found", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void setRttMode(String str, int i) {
        try {
            Log.startSession("ICA.sRM");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void handoverTo(String str, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle) {
        try {
            Log.startSession("ICA.hT", this.mOwnerComponentName);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    Call call = this.mCallIdMapper.getCall(str);
                    if (call != null) {
                        call.handoverTo(phoneAccountHandle, i, bundle);
                    } else {
                        Log.w(this, "handoverTo, unknown call id: %s", new Object[]{str});
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } finally {
            Log.endSession();
        }
    }

    public void doMtkAction(Bundle bundle) {
        MtkTelecomHelper.MtkInCallAdapterHelper.handleExtCommand(bundle, this.mCommandProcessor);
    }
}
