package com.android.internal.telephony.dataconnection;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.dataconnection.DcSwitchAsyncChannel;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class DcSwitchStateMachine extends StateMachine {
    private static final int BASE = 274432;
    private static final boolean DBG = true;
    private static final int DELAY_AFTER_FAILURE = 10000;
    private static final int EVENT_CONNECTED = 274432;
    private static final int EVENT_DATA_ATTACHING = 274442;
    private static final int EVENT_DATA_ATTACHING_RETRY = 274443;
    private static final int EVENT_DATA_DETTACHING = 274444;
    private static final int EVENT_DATA_DETTACHING_RETRY = 274445;
    private static final int EVENT_OEM_START = 274442;
    private static final String LOG_TAG = "DcSwitchSM";
    private static final int MAX_TRY_NUM = 5;
    private static final boolean VDBG = false;
    private AsyncChannel mAc;
    private int mAttachTryNum;
    private AttachedState mAttachedState;
    private AttachingState mAttachingState;
    private Boolean mDataAllowLock;
    private DefaultState mDefaultState;
    private DetachingState mDetachingState;
    private int mId;
    private IdleState mIdleState;
    private Phone mPhone;

    static int access$708(DcSwitchStateMachine x0) {
        int i = x0.mAttachTryNum;
        x0.mAttachTryNum = i + 1;
        return i;
    }

    protected DcSwitchStateMachine(Phone phone, String name, int id) {
        super(name);
        this.mIdleState = new IdleState();
        this.mAttachingState = new AttachingState();
        this.mAttachedState = new AttachedState();
        this.mDetachingState = new DetachingState();
        this.mDefaultState = new DefaultState();
        log("DcSwitchState constructor E");
        this.mPhone = phone;
        this.mId = id;
        this.mAttachTryNum = 0;
        this.mDataAllowLock = true;
        addState(this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mAttachingState, this.mDefaultState);
        addState(this.mAttachedState, this.mDefaultState);
        addState(this.mDetachingState, this.mDefaultState);
        setInitialState(this.mIdleState);
        log("DcSwitchState constructor X");
    }

    private class IdleState extends State {
        private IdleState() {
        }

        public void enter() {
            DcSwitchStateMachine.this.log("IdleState: enter");
            try {
                DctController.getInstance().processRequests();
            } catch (RuntimeException e) {
                DcSwitchStateMachine.this.loge("DctController is not ready");
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 274432:
                    DcSwitchStateMachine.this.log("IdleState: Receive invalid event EVENT_CONNECTED!");
                    break;
                case 278528:
                    DcSwitchStateMachine.this.log("IdleState: REQ_CONNECT");
                    if (!DcSwitchStateMachine.this.mDataAllowLock.booleanValue()) {
                        DcSwitchStateMachine.this.log("IdleState: mDataAllowLock" + DcSwitchStateMachine.this.mDataAllowLock);
                    }
                    DcSwitchStateMachine.this.mDataAllowLock = false;
                    DcSwitchStateMachine.this.mAttachTryNum = 0;
                    PhoneBase pb = (PhoneBase) ((PhoneProxy) DcSwitchStateMachine.this.mPhone).getActivePhone();
                    pb.mCi.setDataAllowed(true, DcSwitchStateMachine.this.obtainMessage(274442));
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278529, 1);
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachingState);
                    break;
                case 278532:
                    DcSwitchStateMachine.this.log("DIdleState: REQ_DISCONNECT_ALL");
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278533, 1);
                    break;
                case 278538:
                    DcSwitchStateMachine.this.log("AttachingState: EVENT_DATA_ATTACHED");
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachedState);
                    break;
                case 278540:
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachingState);
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278541, 1);
                    break;
            }
            return true;
        }
    }

    private class AttachingState extends State {
        PhoneBase pb;

        private AttachingState() {
        }

        public void enter() {
            DcSwitchStateMachine.this.log("AttachingState: enter");
            this.pb = (PhoneBase) ((PhoneProxy) DcSwitchStateMachine.this.mPhone).getActivePhone();
        }

        private void resetDataAttachingRetry() {
            DcSwitchStateMachine.this.removeMessages(DcSwitchStateMachine.EVENT_DATA_ATTACHING_RETRY);
            DcSwitchStateMachine.this.mDataAllowLock = true;
            DcSwitchStateMachine.this.mAttachTryNum = 0;
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 274442:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null && DcSwitchStateMachine.this.mAttachTryNum < 5) {
                        DcSwitchStateMachine.access$708(DcSwitchStateMachine.this);
                        DcSwitchStateMachine.this.log("AttachingState: EVENT_DATA_ATTACHING retry " + DcSwitchStateMachine.this.mAttachTryNum);
                        DcSwitchStateMachine.this.sendMessageDelayed(DcSwitchStateMachine.this.obtainMessage(DcSwitchStateMachine.EVENT_DATA_ATTACHING_RETRY), 10000L);
                    } else {
                        if (ar.exception != null) {
                            DcSwitchStateMachine.this.log("AttachingState: EVENT_DATA_ATTACHING fail");
                        }
                        resetDataAttachingRetry();
                        if (this.pb.getServiceStateTracker().getCurrentDataConnectionState() == 0) {
                            DcSwitchStateMachine.this.log("AttachingState: already attached");
                            DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachedState);
                        }
                    }
                    break;
                case DcSwitchStateMachine.EVENT_DATA_ATTACHING_RETRY:
                    DcSwitchStateMachine.this.log("AttachingState: EVENT_DATA_ATTACHING_RETRY run " + DcSwitchStateMachine.this.mAttachTryNum);
                    this.pb.mCi.setDataAllowed(true, DcSwitchStateMachine.this.obtainMessage(274442));
                    break;
                case 278528:
                    DcSwitchStateMachine.this.log("AttachingState: REQ_CONNECT");
                    if (DcSwitchStateMachine.this.mDataAllowLock.booleanValue()) {
                        DcSwitchStateMachine.this.mDataAllowLock = false;
                        this.pb.mCi.setDataAllowed(true, DcSwitchStateMachine.this.obtainMessage(274442));
                    }
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278529, 1);
                    break;
                case 278532:
                    DcSwitchStateMachine.this.log("AttachingState: REQ_DISCONNECT_ALL");
                    resetDataAttachingRetry();
                    DctController.getInstance().releaseAllRequests(DcSwitchStateMachine.this.mId);
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278533, 1);
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mDetachingState);
                    break;
                case 278538:
                    DcSwitchStateMachine.this.log("AttachingState: EVENT_DATA_ATTACHED");
                    resetDataAttachingRetry();
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachedState);
                    break;
            }
            return true;
        }
    }

    private class AttachedState extends State {
        private AttachedState() {
        }

        public void enter() {
            DcSwitchStateMachine.this.log("AttachedState: enter");
            DctController.getInstance().executeAllRequests(DcSwitchStateMachine.this.mId);
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 278528:
                    DcSwitchAsyncChannel.RequestInfo apnRequest = (DcSwitchAsyncChannel.RequestInfo) msg.obj;
                    DcSwitchStateMachine.this.log("AttachedState: REQ_CONNECT, apnRequest=" + apnRequest);
                    DctController.getInstance().executeRequest(apnRequest);
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278529, 1);
                    break;
                case 278530:
                    DcSwitchAsyncChannel.RequestInfo apnRequest2 = (DcSwitchAsyncChannel.RequestInfo) msg.obj;
                    DcSwitchStateMachine.this.log("AttachedState: REQ_DISCONNECT apnRequest=" + apnRequest2);
                    DctController.getInstance().releaseRequest(apnRequest2);
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278529, 1);
                    break;
                case 278532:
                    DcSwitchStateMachine.this.log("AttachedState: REQ_DISCONNECT_ALL");
                    DctController.getInstance().releaseAllRequests(DcSwitchStateMachine.this.mId);
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278533, 1);
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mDetachingState);
                    break;
                case 278539:
                    DcSwitchStateMachine.this.log("AttachedState: EVENT_DATA_DETACHED");
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachingState);
                    break;
            }
            return true;
        }
    }

    private class DetachingState extends State {
        private int mDetachTryNum;
        private PhoneBase pb;

        private DetachingState() {
        }

        private void resetDataDettachingRetry() {
            DcSwitchStateMachine.this.removeMessages(DcSwitchStateMachine.EVENT_DATA_DETTACHING_RETRY);
            this.mDetachTryNum = 0;
        }

        public void enter() {
            this.mDetachTryNum = 0;
            DcSwitchStateMachine.this.log("DetachingState: enter");
            this.pb = (PhoneBase) ((PhoneProxy) DcSwitchStateMachine.this.mPhone).getActivePhone();
            this.pb.mCi.setDataAllowed(false, DcSwitchStateMachine.this.obtainMessage(DcSwitchStateMachine.EVENT_DATA_DETTACHING));
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case DcSwitchStateMachine.EVENT_DATA_DETTACHING:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null && this.mDetachTryNum < 5) {
                        this.mDetachTryNum++;
                        DcSwitchStateMachine.this.log("DetachingState: EVENT_DATA_DETTACHING retry " + this.mDetachTryNum);
                        DcSwitchStateMachine.this.sendMessageDelayed(DcSwitchStateMachine.this.obtainMessage(DcSwitchStateMachine.EVENT_DATA_DETTACHING_RETRY), 10000L);
                    } else {
                        resetDataDettachingRetry();
                        int dataState = this.pb.getServiceStateTracker().getCurrentDataConnectionState();
                        if (ar.exception != null) {
                            DcSwitchStateMachine.this.log("DetachingState: EVENT_DATA_DETTACHING fail");
                            DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mIdleState);
                        } else if (1 == dataState || 3 == dataState) {
                            DcSwitchStateMachine.this.log("DetachingState: already detached");
                            DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mIdleState);
                        }
                    }
                    break;
                case DcSwitchStateMachine.EVENT_DATA_DETTACHING_RETRY:
                    DcSwitchStateMachine.this.log("DetachingState: EVENT_DATA_DETTACHING_RETRY run " + this.mDetachTryNum);
                    this.pb.mCi.setDataAllowed(false, DcSwitchStateMachine.this.obtainMessage(DcSwitchStateMachine.EVENT_DATA_DETTACHING));
                    break;
                case 278528:
                    if (!DcSwitchStateMachine.this.mDataAllowLock.booleanValue()) {
                        DcSwitchStateMachine.this.log("DetachingState: mDataAllowLock" + DcSwitchStateMachine.this.mDataAllowLock);
                    }
                    DcSwitchStateMachine.this.mDataAllowLock = false;
                    DcSwitchStateMachine.this.mAttachTryNum = 0;
                    this.pb.mCi.setDataAllowed(true, DcSwitchStateMachine.this.obtainMessage(274442));
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278529, 1);
                    resetDataDettachingRetry();
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mAttachingState);
                    break;
                case 278532:
                    DcSwitchStateMachine.this.log("DetachingState: REQ_DISCONNECT_ALL, already detaching");
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278533, 1);
                    break;
                case 278539:
                    DcSwitchStateMachine.this.log("DetachingState: EVENT_DATA_DETACHED");
                    resetDataDettachingRetry();
                    DcSwitchStateMachine.this.transitionTo(DcSwitchStateMachine.this.mIdleState);
                    break;
            }
            return true;
        }
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 69633:
                    if (DcSwitchStateMachine.this.mAc != null) {
                        DcSwitchStateMachine.this.mAc.replyToMessage(msg, 69634, 3);
                    } else {
                        DcSwitchStateMachine.this.mAc = new AsyncChannel();
                        DcSwitchStateMachine.this.mAc.connected((Context) null, DcSwitchStateMachine.this.getHandler(), msg.replyTo);
                        DcSwitchStateMachine.this.mAc.replyToMessage(msg, 69634, 0, DcSwitchStateMachine.this.mId, "hi");
                    }
                    return true;
                case 69635:
                    DcSwitchStateMachine.this.mAc.disconnect();
                    return true;
                case 69636:
                    DcSwitchStateMachine.this.mAc = null;
                    return true;
                case 278534:
                    boolean val = DcSwitchStateMachine.this.getCurrentState() == DcSwitchStateMachine.this.mIdleState;
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278535, val ? 1 : 0);
                    return true;
                case 278536:
                    boolean val2 = DcSwitchStateMachine.this.getCurrentState() == DcSwitchStateMachine.this.mIdleState || DcSwitchStateMachine.this.getCurrentState() == DcSwitchStateMachine.this.mDetachingState;
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278537, val2 ? 1 : 0);
                    return true;
                case 278540:
                    DcSwitchStateMachine.this.mAc.replyToMessage(msg, 278541, 1);
                    return true;
                default:
                    DcSwitchStateMachine.this.log("DefaultState: shouldn't happen but ignore msg.what=0x" + Integer.toHexString(msg.what));
                    return true;
            }
        }
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, "[" + getName() + "] " + s);
    }
}
