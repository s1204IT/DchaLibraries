package com.android.server.telecom;

import android.media.AudioManager;
import android.os.Looper;
import android.os.Message;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import android.telecom.Logging.Session;
import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Printer;
import android.util.SparseArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.mediatek.server.telecom.MtkUtil;
import java.util.Objects;

public class CallAudioModeStateMachine extends StateMachine {
    private final AudioManager mAudioManager;
    private CallAudioManager mCallAudioManager;
    private int mCurFgPhoneId;
    private boolean mIsInitialized;
    private int mMostRecentMode;
    private final BaseState mOtherFocusState;
    private int mPreFgPhoneId;
    private final BaseState mRingingFocusState;
    private final BaseState mSimCallFocusState;
    private final BaseState mUnfocusedState;
    private final BaseState mVoipCallFocusState;
    private static final SparseArray<String> MESSAGE_CODE_TO_NAME = new SparseArray<String>() {
        {
            put(2, "ENTER_CALL_FOCUS_FOR_TESTING");
            put(3, "ENTER_COMMS_FOCUS_FOR_TESTING");
            put(4, "ENTER_RING_FOCUS_FOR_TESTING");
            put(5, "ENTER_TONE_OR_HOLD_FOCUS_FOR_TESTING");
            put(6, "ABANDON_FOCUS_FOR_TESTING");
            put(1001, "NO_MORE_ACTIVE_OR_DIALING_CALLS");
            put(1002, "NO_MORE_RINGING_CALLS");
            put(1003, "NO_MORE_HOLDING_CALLS");
            put(2001, "NEW_ACTIVE_OR_DIALING_CALL");
            put(2002, "NEW_RINGING_CALL");
            put(2003, "NEW_HOLDING_CALL");
            put(2004, "MT_AUDIO_SPEEDUP_FOR_RINGING_CALL");
            put(3001, "TONE_STARTED_PLAYING");
            put(3002, "TONE_STOPPED_PLAYING");
            put(4001, "FOREGROUND_VOIP_MODE_CHANGE");
            put(5001, "RINGER_MODE_CHANGE");
            put(10001, "ABANDON_FOCUS");
            put(9001, "RUN_RUNNABLE");
        }
    };
    public static final String TONE_HOLD_STATE_NAME = OtherFocusState.class.getSimpleName();
    public static final String UNFOCUSED_STATE_NAME = UnfocusedState.class.getSimpleName();
    public static final String CALL_STATE_NAME = SimCallFocusState.class.getSimpleName();
    public static final String RING_STATE_NAME = RingingFocusState.class.getSimpleName();
    public static final String COMMS_STATE_NAME = VoipCallFocusState.class.getSimpleName();
    private static final String LOG_TAG = CallAudioModeStateMachine.class.getSimpleName();

    public static class MessageArgs {
        public boolean foregroundCallIsVoip;
        public boolean hasActiveOrDialingCalls;
        public boolean hasHoldingCalls;
        public boolean hasRingingCalls;
        public boolean isTonePlaying;
        public Session session;

        public MessageArgs(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, Session session) {
            this.hasActiveOrDialingCalls = z;
            this.hasRingingCalls = z2;
            this.hasHoldingCalls = z3;
            this.isTonePlaying = z4;
            this.foregroundCallIsVoip = z5;
            this.session = session;
        }

        public MessageArgs() {
            this.session = Log.createSubsession();
        }

        public String toString() {
            return "MessageArgs{hasActiveCalls=" + this.hasActiveOrDialingCalls + ", hasRingingCalls=" + this.hasRingingCalls + ", hasHoldingCalls=" + this.hasHoldingCalls + ", isTonePlaying=" + this.isTonePlaying + ", foregroundCallIsVoip=" + this.foregroundCallIsVoip + ", session=" + this.session + '}';
        }
    }

    private class BaseState extends State {
        private BaseState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 9001) {
                if (i != 10001) {
                    switch (i) {
                        case 1:
                            CallAudioModeStateMachine.this.mIsInitialized = true;
                            break;
                        case CallState.SELECT_PHONE_ACCOUNT:
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mSimCallFocusState);
                            break;
                        case CallState.DIALING:
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mVoipCallFocusState);
                            break;
                        case CallState.RINGING:
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mRingingFocusState);
                            break;
                        case CallState.ACTIVE:
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mOtherFocusState);
                            break;
                    }
                    return true;
                }
                CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mUnfocusedState);
                return true;
            }
            ((Runnable) message.obj).run();
            return true;
        }
    }

    private class UnfocusedState extends BaseState {
        private UnfocusedState() {
            super();
        }

        public void enter() {
            if (CallAudioModeStateMachine.this.mIsInitialized) {
                Log.i(CallAudioModeStateMachine.LOG_TAG, "Abandoning audio focus: now UNFOCUSED", new Object[0]);
                CallAudioModeStateMachine.this.mAudioManager.abandonAudioFocusForCall();
                CallAudioModeStateMachine.this.mAudioManager.setMode(0);
                CallAudioModeStateMachine.this.mMostRecentMode = 0;
                if (MtkUtil.isInDsdaMode()) {
                    CallAudioModeStateMachine.this.mCurFgPhoneId = CallAudioModeStateMachine.this.getForegroundPhoneId();
                }
                Call foregroundCall = CallAudioModeStateMachine.this.mCallAudioManager.getForegroundCall();
                if (foregroundCall == null || (foregroundCall != null && !foregroundCall.isEmergencyCall())) {
                    CallAudioModeStateMachine.this.mCallAudioManager.setCallAudioRouteFocusState(1);
                }
            }
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            MessageArgs messageArgs = (MessageArgs) message.obj;
            int i = message.what;
            if (i != 3001) {
                switch (i) {
                    case 1001:
                        if (CallAudioModeStateMachine.this.mCallAudioManager.getForegroundCall() == null) {
                            CallAudioModeStateMachine.this.mCallAudioManager.setCallAudioRouteFocusState(1);
                        }
                        return true;
                    case 1002:
                        return true;
                    case 1003:
                        return true;
                    default:
                        switch (i) {
                            case 2001:
                                CallAudioModeStateMachine.this.transitionTo(messageArgs.foregroundCallIsVoip ? CallAudioModeStateMachine.this.mVoipCallFocusState : CallAudioModeStateMachine.this.mSimCallFocusState);
                                return true;
                            case 2002:
                                CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mRingingFocusState);
                                return true;
                            case 2003:
                                Log.w(CallAudioModeStateMachine.LOG_TAG, "Call was surprisingly put into hold from an unknown state. Args are: \n" + messageArgs.toString(), new Object[0]);
                                CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mOtherFocusState);
                                return true;
                            default:
                                return false;
                        }
                }
            }
            Log.w(CallAudioModeStateMachine.LOG_TAG, "Tone started playing unexpectedly. Args are: \n" + messageArgs.toString(), new Object[0]);
            return true;
        }
    }

    private class RingingFocusState extends BaseState {
        private RingingFocusState() {
            super();
        }

        private void tryStartRinging() {
            if (CallAudioModeStateMachine.this.mCallAudioManager.startRinging()) {
                CallAudioModeStateMachine.this.mAudioManager.requestAudioFocusForCall(2, 2);
                CallAudioModeStateMachine.this.mAudioManager.setMode(1);
                CallAudioModeStateMachine.this.mCallAudioManager.setCallAudioRouteFocusState(3);
                return;
            }
            Log.i(CallAudioModeStateMachine.LOG_TAG, "RINGING state, try start ringing but not acquiring audio focus", new Object[0]);
        }

        public void enter() {
            Log.i(CallAudioModeStateMachine.LOG_TAG, "Audio focus entering RINGING state", new Object[0]);
            tryStartRinging();
            CallAudioModeStateMachine.this.mCallAudioManager.stopCallWaiting();
        }

        public void exit() {
            CallAudioModeStateMachine.this.mCallAudioManager.stopRinging();
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            MessageArgs messageArgs = (MessageArgs) message.obj;
            int i = message.what;
            if (i == 5001) {
                Log.i(CallAudioModeStateMachine.LOG_TAG, "RINGING state, received RINGER_MODE_CHANGE", new Object[0]);
                tryStartRinging();
                return true;
            }
            switch (i) {
                case 1001:
                    return true;
                case 1002:
                    if (messageArgs.hasActiveOrDialingCalls) {
                        if (messageArgs.foregroundCallIsVoip) {
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mVoipCallFocusState);
                        } else {
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mSimCallFocusState);
                        }
                    } else if (messageArgs.hasHoldingCalls || messageArgs.isTonePlaying) {
                        CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mOtherFocusState);
                    } else {
                        CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mUnfocusedState);
                    }
                    return true;
                case 1003:
                    return true;
                default:
                    switch (i) {
                        case 2001:
                            CallAudioModeStateMachine.this.transitionTo(messageArgs.foregroundCallIsVoip ? CallAudioModeStateMachine.this.mVoipCallFocusState : CallAudioModeStateMachine.this.mSimCallFocusState);
                            return true;
                        case 2002:
                            Log.w(CallAudioModeStateMachine.LOG_TAG, "Unexpected behavior! New ringing call appeared while in ringing state.", new Object[0]);
                            return true;
                        case 2003:
                            Log.w(CallAudioModeStateMachine.LOG_TAG, "Call was surprisingly put into hold while ringing. Args are: " + messageArgs.toString(), new Object[0]);
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mOtherFocusState);
                            return true;
                        case 2004:
                            CallAudioModeStateMachine.this.transitionTo(messageArgs.foregroundCallIsVoip ? CallAudioModeStateMachine.this.mVoipCallFocusState : CallAudioModeStateMachine.this.mSimCallFocusState);
                            return true;
                        default:
                            return false;
                    }
            }
        }
    }

    private class SimCallFocusState extends BaseState {
        private SimCallFocusState() {
            super();
        }

        public void enter() {
            Log.i(CallAudioModeStateMachine.LOG_TAG, "Audio focus entering SIM CALL state", new Object[0]);
            CallAudioModeStateMachine.this.mAudioManager.requestAudioFocusForCall(0, 2);
            Log.d(CallAudioModeStateMachine.LOG_TAG, "start to set has_foucs", new Object[0]);
            CallAudioModeStateMachine.this.mCallAudioManager.setCallAudioRouteFocusState(2);
            Log.d(CallAudioModeStateMachine.LOG_TAG, "finish to set has_focus", new Object[0]);
            if (!MtkUtil.isInDsdaMode()) {
                Log.d(CallAudioModeStateMachine.LOG_TAG, "start to set mode", new Object[0]);
                CallAudioModeStateMachine.this.mAudioManager.setMode(2);
                Log.d(CallAudioModeStateMachine.LOG_TAG, "finish to set mode", new Object[0]);
                CallAudioModeStateMachine.this.mMostRecentMode = 2;
                return;
            }
            CallAudioModeStateMachine.this.mPreFgPhoneId = CallAudioModeStateMachine.this.mCurFgPhoneId;
            CallAudioModeStateMachine.this.mCurFgPhoneId = CallAudioModeStateMachine.this.getForegroundPhoneId();
            Log.d(CallAudioModeStateMachine.LOG_TAG, "mPreFgPhoneId=" + CallAudioModeStateMachine.this.mPreFgPhoneId + " mCurFgPhoneId=" + CallAudioModeStateMachine.this.mCurFgPhoneId, new Object[0]);
            if (CallAudioModeStateMachine.this.mPreFgPhoneId != CallAudioModeStateMachine.this.mCurFgPhoneId || CallAudioModeStateMachine.this.mAudioManager.getMode() != 2) {
                if (CallAudioModeStateMachine.this.mCurFgPhoneId == -1) {
                    Log.d(CallAudioModeStateMachine.LOG_TAG, "invalid mCurFgPhoneId, return, wait next time trigger", new Object[0]);
                    return;
                }
                if (CallAudioModeStateMachine.this.mAudioManager.getMode() == 2) {
                    Log.d(CallAudioModeStateMachine.LOG_TAG, "reset normal first", new Object[0]);
                    CallAudioModeStateMachine.this.mAudioManager.setMode(0);
                }
                CallAudioModeStateMachine.this.mAudioManager.setParameters("ForegroundPhoneId=" + CallAudioModeStateMachine.this.mCurFgPhoneId);
                CallAudioModeStateMachine.this.mAudioManager.setMode(2);
                CallAudioModeStateMachine.this.mMostRecentMode = 2;
                if (CallAudioModeStateMachine.this.mCallAudioManager.getCallAudioState().isMuted()) {
                    Log.d(CallAudioModeStateMachine.LOG_TAG, "restore mute state after set audio mode to in call!", new Object[0]);
                    CallAudioModeStateMachine.this.mCallAudioManager.restoreMuteOnWhenInCallMode();
                }
            }
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            MessageArgs messageArgs = (MessageArgs) message.obj;
            int i = message.what;
            if (i == 4001) {
                if (messageArgs.foregroundCallIsVoip) {
                    CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mVoipCallFocusState);
                }
                return true;
            }
            if (i != 4501) {
                switch (i) {
                    case 1001:
                        CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.destinationStateAfterNoMoreActiveCalls(messageArgs));
                        break;
                    case 1002:
                        if (messageArgs.isTonePlaying) {
                            CallAudioModeStateMachine.this.mCallAudioManager.stopCallWaiting();
                        }
                        if (!messageArgs.hasActiveOrDialingCalls) {
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.destinationStateAfterNoMoreActiveCalls(messageArgs));
                        }
                        break;
                    case 1003:
                        break;
                    default:
                        switch (i) {
                            case 2002:
                                CallAudioModeStateMachine.this.mCallAudioManager.startCallWaiting();
                                break;
                        }
                        break;
                }
                return true;
            }
            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mSimCallFocusState);
            return true;
        }
    }

    private class VoipCallFocusState extends BaseState {
        private VoipCallFocusState() {
            super();
        }

        public void enter() {
            Log.i(CallAudioModeStateMachine.LOG_TAG, "Audio focus entering VOIP CALL state", new Object[0]);
            CallAudioModeStateMachine.this.mAudioManager.requestAudioFocusForCall(0, 2);
            CallAudioModeStateMachine.this.mAudioManager.setMode(3);
            CallAudioModeStateMachine.this.mMostRecentMode = 3;
            CallAudioModeStateMachine.this.mCallAudioManager.setCallAudioRouteFocusState(2);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            MessageArgs messageArgs = (MessageArgs) message.obj;
            int i = message.what;
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.destinationStateAfterNoMoreActiveCalls(messageArgs));
                        break;
                    case 1002:
                        if (messageArgs.isTonePlaying) {
                            CallAudioModeStateMachine.this.mCallAudioManager.stopCallWaiting();
                        }
                        break;
                    case 1003:
                        break;
                    default:
                        switch (i) {
                            case 2002:
                                CallAudioModeStateMachine.this.mCallAudioManager.startCallWaiting();
                                break;
                        }
                        break;
                }
                return true;
            }
            if (!messageArgs.foregroundCallIsVoip) {
                CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mSimCallFocusState);
            }
            return true;
        }
    }

    private class OtherFocusState extends BaseState {
        private OtherFocusState() {
            super();
        }

        public void enter() {
            Call possiblyHeldForegroundCall;
            Log.i(CallAudioModeStateMachine.LOG_TAG, "Audio focus entering TONE/HOLDING state", new Object[0]);
            CallAudioModeStateMachine.this.mAudioManager.requestAudioFocusForCall(0, 2);
            if (MtkUtil.isInDsdaMode() && (possiblyHeldForegroundCall = CallAudioModeStateMachine.this.mCallAudioManager.getPossiblyHeldForegroundCall()) != null && possiblyHeldForegroundCall.getState() == 6 && possiblyHeldForegroundCall.isCdma()) {
                CallAudioModeStateMachine.this.mMostRecentMode = 0;
                Log.d(CallAudioModeStateMachine.LOG_TAG, "reset mCurFgPhoneId in OtherFocusState", new Object[0]);
                CallAudioModeStateMachine.this.mCurFgPhoneId = -1;
            }
            CallAudioModeStateMachine.this.mAudioManager.setMode(CallAudioModeStateMachine.this.mMostRecentMode);
            CallAudioModeStateMachine.this.mCallAudioManager.setCallAudioRouteFocusState(2);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            MessageArgs messageArgs = (MessageArgs) message.obj;
            int i = message.what;
            if (i != 3002) {
                switch (i) {
                    case 1002:
                        CallAudioModeStateMachine.this.mCallAudioManager.stopCallWaiting();
                        CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.destinationStateAfterNoMoreActiveCalls(messageArgs));
                        break;
                    case 1003:
                        if (messageArgs.hasActiveOrDialingCalls) {
                            CallAudioModeStateMachine.this.transitionTo(messageArgs.foregroundCallIsVoip ? CallAudioModeStateMachine.this.mVoipCallFocusState : CallAudioModeStateMachine.this.mSimCallFocusState);
                        } else if (messageArgs.hasRingingCalls) {
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mRingingFocusState);
                        } else if (!messageArgs.isTonePlaying) {
                            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mUnfocusedState);
                        }
                        break;
                    default:
                        switch (i) {
                            case 2001:
                                CallAudioModeStateMachine.this.transitionTo(messageArgs.foregroundCallIsVoip ? CallAudioModeStateMachine.this.mVoipCallFocusState : CallAudioModeStateMachine.this.mSimCallFocusState);
                                break;
                            case 2002:
                                CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.mRingingFocusState);
                                break;
                        }
                        break;
                }
                return true;
            }
            CallAudioModeStateMachine.this.transitionTo(CallAudioModeStateMachine.this.destinationStateAfterNoMoreActiveCalls(messageArgs));
            return false;
        }
    }

    public CallAudioModeStateMachine(AudioManager audioManager) {
        super(CallAudioModeStateMachine.class.getSimpleName());
        this.mUnfocusedState = new UnfocusedState();
        this.mRingingFocusState = new RingingFocusState();
        this.mSimCallFocusState = new SimCallFocusState();
        this.mVoipCallFocusState = new VoipCallFocusState();
        this.mOtherFocusState = new OtherFocusState();
        this.mIsInitialized = false;
        this.mPreFgPhoneId = -1;
        this.mCurFgPhoneId = -1;
        this.mAudioManager = audioManager;
        this.mMostRecentMode = 0;
        addState(this.mUnfocusedState);
        addState(this.mRingingFocusState);
        addState(this.mSimCallFocusState);
        addState(this.mVoipCallFocusState);
        addState(this.mOtherFocusState);
        setInitialState(this.mUnfocusedState);
        start();
        sendMessage(1, new MessageArgs());
    }

    public void setCallAudioManager(CallAudioManager callAudioManager) {
        this.mCallAudioManager = callAudioManager;
    }

    public void sendMessageWithArgs(int i, MessageArgs messageArgs) {
        sendMessage(i, messageArgs);
    }

    protected void onPreHandleMessage(Message message) {
        if (message.obj != null && (message.obj instanceof MessageArgs)) {
            Log.continueSession(((MessageArgs) message.obj).session, "CAMSM.pM_" + message.what);
            Log.i(LOG_TAG, "Message received: %s.", new Object[]{MESSAGE_CODE_TO_NAME.get(message.what)});
            return;
        }
        if (message.what == 9001 && (message.obj instanceof Runnable)) {
            Log.i(LOG_TAG, "Running runnable for testing", new Object[0]);
            return;
        }
        String str = LOG_TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Message sent must be of type nonnull MessageArgs, but got ");
        sb.append(message.obj == null ? "null" : message.obj.getClass().getSimpleName());
        Log.w(str, sb.toString(), new Object[0]);
        Log.w(LOG_TAG, "The message was of code %d = %s", new Object[]{Integer.valueOf(message.what), MESSAGE_CODE_TO_NAME.get(message.what)});
    }

    public void dumpPendingMessages(final IndentingPrintWriter indentingPrintWriter) {
        Looper looper = getHandler().getLooper();
        Objects.requireNonNull(indentingPrintWriter);
        looper.dump(new Printer() {
            @Override
            public final void println(String str) {
                indentingPrintWriter.println(str);
            }
        }, "");
    }

    protected void onPostHandleMessage(Message message) {
        Log.endSession();
    }

    private BaseState destinationStateAfterNoMoreActiveCalls(MessageArgs messageArgs) {
        if (messageArgs.hasHoldingCalls) {
            return this.mOtherFocusState;
        }
        if (messageArgs.hasRingingCalls) {
            return this.mRingingFocusState;
        }
        if (messageArgs.isTonePlaying) {
            return this.mOtherFocusState;
        }
        return this.mUnfocusedState;
    }

    private int getForegroundPhoneId() {
        int phoneId;
        Log.d(LOG_TAG, "enter getForegroundPhoneId isDsda: " + MtkUtil.isInDsdaMode(), new Object[0]);
        if (MtkUtil.isInDsdaMode()) {
            Call foregroundCall = this.mCallAudioManager.getForegroundCall();
            Log.d(LOG_TAG, "getForegroundCall, call:" + foregroundCall, new Object[0]);
            if (foregroundCall != null && (foregroundCall.isAlive() || foregroundCall.can(262144))) {
                phoneId = getPhoneId(foregroundCall.getTargetPhoneAccount());
            } else {
                phoneId = -1;
            }
        }
        Log.d(LOG_TAG, "foreground phoneId = " + phoneId, new Object[0]);
        return phoneId;
    }

    private int getPhoneId(PhoneAccountHandle phoneAccountHandle) {
        Log.d(LOG_TAG, "getPhoneId, handle:" + phoneAccountHandle, new Object[0]);
        if (phoneAccountHandle == null) {
            return -1;
        }
        if (TextUtils.isDigitsOnly(phoneAccountHandle.getId()) && phoneAccountHandle.getId().length() < 2) {
            return Integer.parseInt(phoneAccountHandle.getId());
        }
        int subIdForPhoneAccountHandle = MtkUtil.getSubIdForPhoneAccountHandle(phoneAccountHandle);
        Log.d(LOG_TAG, "getPhoneId, subId = " + subIdForPhoneAccountHandle, new Object[0]);
        return SubscriptionManager.getPhoneId(subIdForPhoneAccountHandle);
    }
}
