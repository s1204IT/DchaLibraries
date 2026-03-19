package com.mediatek.ims;

import android.content.Context;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.util.Log;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.internal.annotations.VisibleForTesting;
import com.mediatek.ims.internal.MtkImsCallSession;
import com.mediatek.ims.op.OpImsCall;
import com.mediatek.ims.op.OpImsFwkFactoryBase;
import java.util.Objects;

public class MtkImsCall extends ImsCall {
    private static final String TAG = "MtkImsCall";
    private static final int UPDATE_DEVICE_SWITCH = 8;
    private static final int UPDATE_ECT = 7;
    private String mAddress;
    private boolean mIsConferenceMerging;
    private OpImsCall mOpImsCall;

    public MtkImsCall(Context context, ImsCallProfile imsCallProfile) {
        super(context, imsCallProfile);
        this.mIsConferenceMerging = false;
        this.mAddress = null;
        this.mOpImsCall = OpImsFwkFactoryBase.getInstance().makeOpImsCall();
    }

    public static class Listener extends ImsCall.Listener {
        public void onPauInfoChanged(ImsCall imsCall) {
        }

        public void onCallTransferred(ImsCall imsCall) {
        }

        public void onCallTransferFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onTextCapabilityChanged(ImsCall imsCall, int i, int i2, int i3, int i4) {
        }

        public void onRttEventReceived(ImsCall imsCall, int i) {
        }

        public void onCallDeviceSwitched(ImsCall imsCall) {
        }

        public void onCallDeviceSwitchFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
        }
    }

    public void start(ImsCallSession imsCallSession, String str) throws ImsException {
        super.start(imsCallSession, str);
        this.mAddress = str;
    }

    public void start(ImsCallSession imsCallSession, String[] strArr) throws ImsException {
        super.start(imsCallSession, strArr);
        if (this.mCallProfile != null && this.mCallProfile.getCallExtraBoolean("conference")) {
            this.mIsConferenceHost = true;
        }
    }

    protected void setTransientSessionAsPrimary(ImsCallSession imsCallSession) {
        synchronized (this) {
            if (this.mSession != null) {
                this.mSession.setListener((ImsCallSession.Listener) null);
                this.mSession.close();
            }
            this.mSession = imsCallSession;
            if (this.mSession != null) {
                this.mSession.setListener(createCallSessionListener());
                ImsCallProfile callProfile = this.mSession.getCallProfile();
                if (callProfile != null) {
                    this.mCallProfile.updateCallType(callProfile);
                }
            }
        }
    }

    public boolean isIncomingCallMultiparty() {
        synchronized (this.mLockObj) {
            if (this.mSession != null && (this.mSession instanceof MtkImsCallSession)) {
                return this.mSession.isIncomingCallMultiparty();
            }
            return false;
        }
    }

    public void explicitCallTransfer() throws ImsException {
        logi("explicitCallTransfer :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("explicitCallTransfer :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            if (this.mSession instanceof MtkImsCallSession) {
                this.mSession.explicitCallTransfer();
                this.mUpdateRequest = 7;
            }
        }
    }

    public void unattendedCallTransfer(String str, int i) throws ImsException {
        logi("explicitCallTransfer :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("explicitCallTransfer :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            if (this.mSession instanceof MtkImsCallSession) {
                this.mSession.unattendedCallTransfer(str, i);
                this.mUpdateRequest = 7;
            }
        }
    }

    public void deviceSwitch(String str, String str2) throws ImsException {
        logi("deviceSwitch :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("deviceSwitch :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            if (this.mSession instanceof MtkImsCallSession) {
                this.mSession.deviceSwitch(str, str2);
                this.mUpdateRequest = UPDATE_DEVICE_SWITCH;
            }
        }
    }

    public void cancelDeviceSwitch() throws ImsException {
        logi("cancelDeviceSwitch :: ");
        synchronized (this.mLockObj) {
            if (this.mSession instanceof MtkImsCallSession) {
                this.mSession.cancelDeviceSwitch();
            }
        }
    }

    protected ImsCall createNewCall(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        MtkImsCall mtkImsCall = new MtkImsCall(this.mContext, imsCallProfile);
        try {
            mtkImsCall.attachSession(imsCallSession);
            return mtkImsCall;
        } catch (ImsException e) {
            mtkImsCall.close();
            return null;
        }
    }

    protected ImsCallSession.Listener createCallSessionListener() {
        this.mImsCallSessionListenerProxy = new MtkImsCallSessionListenerProxy();
        return this.mImsCallSessionListenerProxy;
    }

    @VisibleForTesting
    public class MtkImsCallSessionListenerProxy extends ImsCall.ImsCallSessionListenerProxy {
        public MtkImsCallSessionListenerProxy() {
            super(MtkImsCall.this);
        }

        protected boolean doesCallSessionExistsInMerge(ImsCallSession imsCallSession) {
            String callId = imsCallSession.getCallId();
            return (MtkImsCall.this.isMergeHost() && Objects.equals((MtkImsCall.this.mMergePeer != null && MtkImsCall.this.mMergePeer.mSession != null) ? MtkImsCall.this.mMergePeer.mSession.getCallId() : "", callId)) || (MtkImsCall.this.isMergePeer() && Objects.equals((MtkImsCall.this.mMergeHost != null && MtkImsCall.this.mMergeHost.mSession != null) ? MtkImsCall.this.mMergeHost.mSession.getCallId() : "", callId)) || Objects.equals(MtkImsCall.this.mSession == null ? "" : MtkImsCall.this.mSession.getCallId(), callId);
        }

        public void callSessionMergeComplete(ImsCallSession imsCallSession) {
            MtkImsCall.this.logi("callSessionMergeComplete :: newSession =" + imsCallSession);
            if (!MtkImsCall.this.isMergeHost()) {
                if (MtkImsCall.this.mMergeHost == null) {
                    MtkImsCall.this.logd("merge host is null, terminate conf");
                    if (imsCallSession != null) {
                        imsCallSession.terminate(102);
                        return;
                    }
                    return;
                }
                MtkImsCall.this.mMergeHost.processMergeComplete();
                return;
            }
            if (imsCallSession != null) {
                MtkImsCall mtkImsCall = MtkImsCall.this;
                if (doesCallSessionExistsInMerge(imsCallSession)) {
                    imsCallSession = null;
                }
                mtkImsCall.mTransientConferenceSession = imsCallSession;
                if (MtkImsCall.this.mTransientConferenceSession == null) {
                    MtkImsCall.this.logi("callSessionMergeComplete :: callSessionExisted.");
                }
            }
            MtkImsCall.this.processMergeComplete();
        }

        public void callSessionTransferred(ImsCallSession imsCallSession) {
            ImsCall.Listener listener;
            MtkImsCall.this.loge("callSessionTransferred :: session=" + imsCallSession);
            synchronized (MtkImsCall.this) {
                MtkImsCall.this.mUpdateRequest = 0;
                listener = MtkImsCall.this.mListener;
            }
            if (listener != null && (listener instanceof Listener)) {
                try {
                    ((Listener) listener).onCallTransferred(MtkImsCall.this);
                } catch (Throwable th) {
                    MtkImsCall.this.loge("callSessionTransferred :: ", th);
                }
            }
        }

        public void callSessionTransferFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            ImsCall.Listener listener;
            MtkImsCall.this.loge("callSessionTransferFailed :: session=" + imsCallSession + " reasonInfo=" + imsReasonInfo);
            synchronized (MtkImsCall.this) {
                MtkImsCall.this.mUpdateRequest = 0;
                listener = MtkImsCall.this.mListener;
            }
            if (listener != null && (listener instanceof Listener)) {
                try {
                    ((Listener) listener).onCallTransferFailed(MtkImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    MtkImsCall.this.loge("callSessionTransferFailed :: ", th);
                }
            }
        }

        public void callSessionDeviceSwitched(ImsCallSession imsCallSession) {
            ImsCall.Listener listener;
            MtkImsCall.this.loge("callSessionTransferred :: session=" + imsCallSession);
            synchronized (MtkImsCall.this) {
                MtkImsCall.this.mUpdateRequest = 0;
                listener = MtkImsCall.this.mListener;
            }
            if (listener != null && (listener instanceof Listener)) {
                try {
                    ((Listener) listener).onCallDeviceSwitched(MtkImsCall.this);
                } catch (Throwable th) {
                    MtkImsCall.this.loge("callSessionDeviceSwitched :: ", th);
                }
            }
        }

        public void callSessionDeviceSwitchFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            ImsCall.Listener listener;
            MtkImsCall.this.loge("callSessionDeviceSwitchedFailed :: session=" + imsCallSession + " reasonInfo=" + imsReasonInfo);
            synchronized (MtkImsCall.this) {
                MtkImsCall.this.mUpdateRequest = 0;
                listener = MtkImsCall.this.mListener;
            }
            if (listener != null && (listener instanceof Listener)) {
                try {
                    ((Listener) listener).onCallDeviceSwitchFailed(MtkImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    MtkImsCall.this.loge("callSessionDeviceSwitchedFailed :: ", th);
                }
            }
        }

        public void callSessionTextCapabilityChanged(ImsCallSession imsCallSession, int i, int i2, int i3, int i4) {
            ImsCall.Listener listener;
            synchronized (MtkImsCall.this) {
                listener = MtkImsCall.this.mListener;
            }
            MtkImsCall.this.mOpImsCall.callSessionTextCapabilityChanged(listener, MtkImsCall.this, i, i2, i3, i4);
        }

        public void callSessionRttModifyRequestReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            ImsCall.Listener listener;
            synchronized (MtkImsCall.this) {
                listener = MtkImsCall.this.mListener;
            }
            if (!imsCallProfile.mMediaProfile.isRttCall()) {
                MtkImsCall.this.logi("callSessionRttModifyRequestReceived:: receive a downgrade.");
                MtkImsCall.this.mOpImsCall.notifyRttDowngradeEvent(listener, MtkImsCall.this);
            }
            super.callSessionRttModifyRequestReceived(imsCallSession, imsCallProfile);
        }

        public void callSessionRttEventReceived(ImsCallSession imsCallSession, int i) {
            ImsCall.Listener listener;
            synchronized (MtkImsCall.this) {
                listener = MtkImsCall.this.mListener;
            }
            MtkImsCall.this.mOpImsCall.callSessionRttEventReceived(listener, MtkImsCall.this, i);
        }
    }

    protected String updateRequestToString(int i) {
        switch (i) {
            case 7:
                return "ECT";
            case UPDATE_DEVICE_SWITCH:
                return "DEVICE_SWITCH";
            default:
                return super.updateRequestToString(i);
        }
    }

    protected void logi(String str) {
        Log.i(TAG, appendImsCallInfoToString(str));
    }

    private String appendImsCallInfoToString(String str) {
        return str + " MtkImsCall=" + this;
    }

    protected void copyCallProfileIfNecessary(ImsStreamMediaProfile imsStreamMediaProfile) {
        if (this.mCallProfile != null) {
            this.mCallProfile.mMediaProfile.copyFrom(imsStreamMediaProfile);
        }
    }

    protected void checkIfConferenceMerge(ImsReasonInfo imsReasonInfo) {
        if (isCallSessionMergePending() && isMultiparty()) {
            logi("this is a conference host during merging, and is disconnected..");
            processMergeFailed(imsReasonInfo);
        }
    }

    protected void updateHoldStateIfNecessary(boolean z) {
        this.mHold = z;
    }

    protected boolean shouldSkipResetMergePending() {
        return true;
    }

    protected void resetConferenceMergingFlag() {
        this.mIsConferenceMerging = false;
    }

    public void sendRttDowngradeRequest() {
        synchronized (this.mLockObj) {
            if (this.mSession != null && (this.mSession instanceof MtkImsCallSession)) {
                this.mOpImsCall.sendRttDowngradeRequest(this.mCallProfile, (MtkImsCallSession) this.mSession);
            }
        }
    }

    public void setRttMode(int i) {
        this.mOpImsCall.setRttMode(i, this.mCallProfile);
    }

    private OpImsCall getOpImsCall() {
        if (this.mOpImsCall == null) {
            this.mOpImsCall = OpImsFwkFactoryBase.getInstance().makeOpImsCall();
        }
        return this.mOpImsCall;
    }

    public void setTerminationRequestFlag(boolean z) {
        this.mTerminationRequestPending = z;
    }
}
