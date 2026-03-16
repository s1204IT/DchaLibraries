package com.android.services.telephony;

import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telecom.AudioState;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.PhoneGlobals;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

abstract class TelephonyConnection extends Connection {
    private int mAudioQuality;
    private boolean mLocalVideoCapable;
    private com.android.internal.telephony.Connection mOriginalConnection;
    private boolean mRemoteVideoCapable;
    private boolean mWasImsConnection;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.v(TelephonyConnection.this, "MSG_PRECISE_CALL_STATE_CHANGED", new Object[0]);
                    TelephonyConnection.this.updateState();
                    break;
                case 2:
                    Log.v(TelephonyConnection.this, "MSG_RINGBACK_TONE", new Object[0]);
                    if (TelephonyConnection.this.getOriginalConnection() != TelephonyConnection.this.getForegroundConnection()) {
                        Log.v(TelephonyConnection.this, "handleMessage, original connection is not foreground connection, skipping", new Object[0]);
                    } else {
                        TelephonyConnection.this.setRingbackRequested(((Boolean) ((AsyncResult) msg.obj).result).booleanValue());
                    }
                    break;
                case 3:
                    Log.v(TelephonyConnection.this, "MSG_HANDOVER_STATE_CHANGED", new Object[0]);
                    AsyncResult ar = (AsyncResult) msg.obj;
                    com.android.internal.telephony.Connection connection = (com.android.internal.telephony.Connection) ar.result;
                    if ((connection.getAddress() != null && TelephonyConnection.this.mOriginalConnection.getAddress() != null && TelephonyConnection.this.mOriginalConnection.getAddress().contains(connection.getAddress())) || connection.getStateBeforeHandover() == TelephonyConnection.this.mOriginalConnection.getState()) {
                        Log.d(TelephonyConnection.this, "SettingOriginalConnection " + TelephonyConnection.this.mOriginalConnection.toString() + " with " + connection.toString(), new Object[0]);
                        TelephonyConnection.this.setOriginalConnection(connection);
                        break;
                    }
                    break;
                case 4:
                    TelephonyConnection.this.updateState();
                    break;
            }
        }
    };
    private final Connection.PostDialListener mPostDialListener = new Connection.PostDialListener() {
        public void onPostDialWait() {
            Log.v(TelephonyConnection.this, "onPostDialWait", new Object[0]);
            if (TelephonyConnection.this.mOriginalConnection != null) {
                TelephonyConnection.this.setPostDialWait(TelephonyConnection.this.mOriginalConnection.getRemainingPostDialString());
            }
        }

        public void onPostDialChar(char c) {
            Log.v(TelephonyConnection.this, "onPostDialChar: %s", Character.valueOf(c));
            if (TelephonyConnection.this.mOriginalConnection != null) {
                TelephonyConnection.this.setNextPostDialWaitChar(c);
            }
        }
    };
    private final Connection.Listener mOriginalConnectionListener = new Connection.ListenerBase() {
        public void onVideoStateChanged(int videoState) {
            TelephonyConnection.this.setVideoState(videoState);
        }

        public void onLocalVideoCapabilityChanged(boolean capable) {
            TelephonyConnection.this.setLocalVideoCapable(capable);
        }

        public void onRemoteVideoCapabilityChanged(boolean capable) {
            TelephonyConnection.this.setRemoteVideoCapable(capable);
        }

        public void onVideoProviderChanged(Connection.VideoProvider videoProvider) {
            TelephonyConnection.this.setVideoProvider(videoProvider);
        }

        public void onAudioQualityChanged(int audioQuality) {
            TelephonyConnection.this.setAudioQuality(audioQuality);
        }

        public void onConferenceParticipantsChanged(List<ConferenceParticipant> participants) {
            TelephonyConnection.this.updateConferenceParticipants(participants);
        }
    };
    private Call.State mOriginalConnectionState = Call.State.IDLE;
    private boolean mIsMultiParty = false;
    private final Set<TelephonyConnectionListener> mTelephonyListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));

    public abstract TelephonyConnection cloneConnection();

    public static abstract class TelephonyConnectionListener {
        public void onOriginalConnectionConfigured(TelephonyConnection c) {
        }
    }

    protected TelephonyConnection(com.android.internal.telephony.Connection originalConnection) {
        if (originalConnection != null) {
            setOriginalConnection(originalConnection);
        }
    }

    public void onAudioStateChanged(AudioState audioState) {
        if (getPhone() != null) {
            getPhone().setEchoSuppressionEnabled();
        }
    }

    @Override
    public void onStateChanged(int state) {
        Log.v(this, "onStateChanged, state: " + android.telecom.Connection.stateToString(state), new Object[0]);
    }

    @Override
    public void onDisconnect() {
        Log.v(this, "onDisconnect", new Object[0]);
        hangup(3);
    }

    public void onDisconnectConferenceParticipant(Uri endpoint) {
        Log.v(this, "onDisconnectConferenceParticipant %s", endpoint);
        if (this.mOriginalConnection != null) {
            this.mOriginalConnection.onDisconnectConferenceParticipant(endpoint);
        }
    }

    @Override
    public void onSeparate() {
        Log.v(this, "onSeparate", new Object[0]);
        if (this.mOriginalConnection != null) {
            try {
                this.mOriginalConnection.separate();
            } catch (CallStateException e) {
                Log.e(this, e, "Call to Connection.separate failed with exception", new Object[0]);
            }
        }
    }

    @Override
    public void onAbort() {
        Log.v(this, "onAbort", new Object[0]);
        hangup(3);
    }

    @Override
    public void onHold() {
        performHold();
    }

    @Override
    public void onUnhold() {
        performUnhold();
    }

    @Override
    public void onAnswer(int videoState) {
        Log.v(this, "onAnswer", new Object[0]);
        if (isValidRingingCall() && getPhone() != null) {
            try {
                getPhone().acceptCall(videoState);
            } catch (CallStateException e) {
                Log.e(this, e, "Failed to accept call.", new Object[0]);
            }
        }
    }

    @Override
    public void onReject() {
        Log.v(this, "onReject", new Object[0]);
        if (isValidRingingCall()) {
            hangup(16);
        }
        super.onReject();
    }

    @Override
    public void onPostDialContinue(boolean proceed) {
        Log.v(this, "onPostDialContinue, proceed: " + proceed, new Object[0]);
        if (this.mOriginalConnection != null) {
            if (proceed) {
                this.mOriginalConnection.proceedAfterWaitChar();
            } else {
                this.mOriginalConnection.cancelPostDial();
            }
        }
    }

    public void performHold() {
        Log.v(this, "performHold", new Object[0]);
        if (Call.State.ACTIVE == this.mOriginalConnectionState) {
            Log.v(this, "Holding active call", new Object[0]);
            try {
                Phone phone = this.mOriginalConnection.getCall().getPhone();
                Call ringingCall = phone.getRingingCall();
                if (ringingCall.getState() != Call.State.WAITING) {
                    phone.switchHoldingAndActive();
                    return;
                }
                return;
            } catch (CallStateException e) {
                Log.e(this, e, "Exception occurred while trying to put call on hold.", new Object[0]);
                return;
            }
        }
        Log.w(this, "Cannot put a call that is not currently active on hold.", new Object[0]);
    }

    public void performUnhold() {
        Log.v(this, "performUnhold", new Object[0]);
        if (Call.State.HOLDING == this.mOriginalConnectionState) {
            try {
                if (!hasMultipleTopLevelCalls()) {
                    this.mOriginalConnection.getCall().getPhone().switchHoldingAndActive();
                } else {
                    Log.i(this, "Skipping unhold command for %s", this);
                }
                return;
            } catch (CallStateException e) {
                Log.e(this, e, "Exception occurred while trying to release call from hold.", new Object[0]);
                return;
            }
        }
        Log.w(this, "Cannot release a call that is not already on hold from hold.", new Object[0]);
    }

    public void performConference(TelephonyConnection otherConnection) {
        Log.d(this, "performConference - %s", this);
        if (getPhone() != null) {
            try {
                getPhone().conference();
            } catch (CallStateException e) {
                Log.e(this, e, "Failed to conference call.", new Object[0]);
            }
        }
    }

    protected int buildConnectionCapabilities() {
        if (!isImsConnection()) {
            return 0;
        }
        int callCapabilities = 0 | 2;
        if (getState() == 4 || getState() == 5) {
            return callCapabilities | 1;
        }
        return callCapabilities;
    }

    protected final void updateConnectionCapabilities() {
        int newCapabilities = applyConferenceTerminationCapabilities(applyAudioQualityCapabilities(applyVideoCapabilities(buildConnectionCapabilities())));
        if (getConnectionCapabilities() != newCapabilities) {
            setConnectionCapabilities(newCapabilities);
        }
    }

    protected final void updateAddress() {
        updateConnectionCapabilities();
        if (this.mOriginalConnection != null) {
            Uri address = getAddressFromNumber(this.mOriginalConnection.getAddress());
            int presentation = this.mOriginalConnection.getNumberPresentation();
            if (!Objects.equals(address, getAddress()) || presentation != getAddressPresentation()) {
                Log.v(this, "updateAddress, address changed", new Object[0]);
                setAddress(address, presentation);
            }
            String name = this.mOriginalConnection.getCnapName();
            int namePresentation = this.mOriginalConnection.getCnapNamePresentation();
            if (!Objects.equals(name, getCallerDisplayName()) || namePresentation != getCallerDisplayNamePresentation()) {
                Log.v(this, "updateAddress, caller display name changed", new Object[0]);
                setCallerDisplayName(name, namePresentation);
            }
        }
    }

    void setOriginalConnection(com.android.internal.telephony.Connection originalConnection) {
        Log.v(this, "new TelephonyConnection, originalConnection: " + originalConnection, new Object[0]);
        clearOriginalConnection();
        this.mOriginalConnection = originalConnection;
        getPhone().registerForPreciseCallStateChanged(this.mHandler, 1, (Object) null);
        getPhone().registerForHandoverStateChanged(this.mHandler, 3, (Object) null);
        getPhone().registerForRingbackTone(this.mHandler, 2, (Object) null);
        getPhone().registerForDisconnect(this.mHandler, 4, (Object) null);
        this.mOriginalConnection.addPostDialListener(this.mPostDialListener);
        this.mOriginalConnection.addListener(this.mOriginalConnectionListener);
        setVideoState(this.mOriginalConnection.getVideoState());
        setLocalVideoCapable(this.mOriginalConnection.isLocalVideoCapable());
        setRemoteVideoCapable(this.mOriginalConnection.isRemoteVideoCapable());
        setVideoProvider(this.mOriginalConnection.getVideoProvider());
        setAudioQuality(this.mOriginalConnection.getAudioQuality());
        if (isImsConnection()) {
            this.mWasImsConnection = true;
        }
        this.mIsMultiParty = this.mOriginalConnection.isMultiparty();
        fireOnOriginalConnectionConfigured();
        updateAddress();
    }

    void clearOriginalConnection() {
        if (this.mOriginalConnection != null) {
            getPhone().unregisterForPreciseCallStateChanged(this.mHandler);
            getPhone().unregisterForRingbackTone(this.mHandler);
            getPhone().unregisterForHandoverStateChanged(this.mHandler);
            getPhone().unregisterForDisconnect(this.mHandler);
            this.mOriginalConnection = null;
        }
    }

    protected void hangup(int telephonyDisconnectCode) {
        if (this.mOriginalConnection != null) {
            try {
                if (isValidRingingCall()) {
                    Call call = getCall();
                    if (call != null) {
                        call.hangup();
                    } else {
                        Log.w(this, "Attempting to hangup a connection without backing call.", new Object[0]);
                    }
                } else {
                    this.mOriginalConnection.hangup();
                }
            } catch (CallStateException e) {
                Log.e(this, e, "Call to Connection.hangup failed with exception", new Object[0]);
            }
        }
    }

    com.android.internal.telephony.Connection getOriginalConnection() {
        return this.mOriginalConnection;
    }

    protected Call getCall() {
        if (this.mOriginalConnection != null) {
            return this.mOriginalConnection.getCall();
        }
        return null;
    }

    Phone getPhone() {
        Call call = getCall();
        if (call != null) {
            return call.getPhone();
        }
        return null;
    }

    private boolean hasMultipleTopLevelCalls() {
        int numCalls = 0;
        Phone phone = getPhone();
        if (phone != null) {
            if (!phone.getRingingCall().isIdle()) {
                numCalls = 0 + 1;
            }
            if (!phone.getForegroundCall().isIdle()) {
                numCalls++;
            }
            if (!phone.getBackgroundCall().isIdle()) {
                numCalls++;
            }
        }
        return numCalls > 1;
    }

    private com.android.internal.telephony.Connection getForegroundConnection() {
        if (getPhone() != null) {
            return getPhone().getForegroundCall().getEarliestConnection();
        }
        return null;
    }

    private boolean isValidRingingCall() {
        if (getPhone() == null) {
            Log.v(this, "isValidRingingCall, phone is null", new Object[0]);
            return false;
        }
        Call ringingCall = getPhone().getRingingCall();
        if (!ringingCall.getState().isRinging()) {
            Log.v(this, "isValidRingingCall, ringing call is not in ringing state", new Object[0]);
            return false;
        }
        if (ringingCall.getEarliestConnection() != this.mOriginalConnection) {
            Log.v(this, "isValidRingingCall, ringing call connection does not match", new Object[0]);
            return false;
        }
        Log.v(this, "isValidRingingCall, returning true", new Object[0]);
        return true;
    }

    void updateState() {
        if (this.mOriginalConnection != null) {
            Call.State newState = this.mOriginalConnection.getState();
            Log.v(this, "Update state from %s to %s for %s", this.mOriginalConnectionState, newState, this);
            if (this.mOriginalConnectionState != newState) {
                this.mOriginalConnectionState = newState;
                switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$Call$State[newState.ordinal()]) {
                    case 2:
                        setActiveInternal();
                        break;
                    case 3:
                        setOnHold();
                        break;
                    case 4:
                    case 5:
                        setDialing();
                        break;
                    case 6:
                    case 7:
                        setRinging();
                        break;
                    case 8:
                        PhoneGlobals.getInstance().wakeUpScreen();
                        setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(this.mOriginalConnection.getDisconnectCause()));
                        close();
                        break;
                }
            }
            updateConnectionCapabilities();
            updateAddress();
            updateMultiparty();
        }
    }

    static class AnonymousClass4 {
        static final int[] $SwitchMap$com$android$internal$telephony$Call$State = new int[Call.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.ACTIVE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.HOLDING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DIALING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.ALERTING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.INCOMING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.WAITING.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DISCONNECTED.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DISCONNECTING.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
        }
    }

    private void updateMultiparty() {
        if (this.mOriginalConnection != null && this.mIsMultiParty != this.mOriginalConnection.isMultiparty()) {
            this.mIsMultiParty = this.mOriginalConnection.isMultiparty();
            if (this.mIsMultiParty) {
                notifyConferenceStarted();
            }
        }
    }

    private void setActiveInternal() {
        if (getState() == 4) {
            Log.w(this, "Should not be called if this is already ACTIVE", new Object[0]);
            return;
        }
        if (getConnectionService() != null) {
            for (android.telecom.Connection current : getConnectionService().getAllConnections()) {
                if (current != this && (current instanceof TelephonyConnection)) {
                    TelephonyConnection other = (TelephonyConnection) current;
                    if (other.getState() == 4) {
                        other.updateState();
                    }
                }
            }
        }
        setActive();
    }

    private void close() {
        Log.v(this, "close", new Object[0]);
        if (getPhone() != null) {
            getPhone().unregisterForPreciseCallStateChanged(this.mHandler);
            getPhone().unregisterForRingbackTone(this.mHandler);
            getPhone().unregisterForHandoverStateChanged(this.mHandler);
        }
        this.mOriginalConnection = null;
        destroy();
    }

    private int applyVideoCapabilities(int capabilities) {
        int currentCapabilities;
        if (this.mRemoteVideoCapable) {
            currentCapabilities = applyCapability(capabilities, 512);
        } else {
            currentCapabilities = removeCapability(capabilities, 512);
        }
        if (this.mLocalVideoCapable) {
            return applyCapability(currentCapabilities, 256);
        }
        return removeCapability(currentCapabilities, 256);
    }

    private int applyAudioQualityCapabilities(int capabilities) {
        if (this.mAudioQuality == 2) {
            int currentCapabilities = applyCapability(capabilities, 1024);
            return currentCapabilities;
        }
        int currentCapabilities2 = removeCapability(capabilities, 1024);
        return currentCapabilities2;
    }

    private int applyConferenceTerminationCapabilities(int capabilities) {
        if (this.mWasImsConnection) {
            return capabilities;
        }
        int currentCapabilities = capabilities | 8192;
        return currentCapabilities | 4096;
    }

    public void setLocalVideoCapable(boolean capable) {
        this.mLocalVideoCapable = capable;
        updateConnectionCapabilities();
    }

    public void setRemoteVideoCapable(boolean capable) {
        this.mRemoteVideoCapable = capable;
        updateConnectionCapabilities();
    }

    public void setAudioQuality(int audioQuality) {
        this.mAudioQuality = audioQuality;
        updateConnectionCapabilities();
    }

    void resetStateForConference() {
        if (getState() == 5 && this.mOriginalConnection.getState() == Call.State.ACTIVE) {
            setActive();
        }
    }

    boolean setHoldingForConference() {
        if (getState() != 4) {
            return false;
        }
        setOnHold();
        return true;
    }

    protected boolean isImsConnection() {
        return getOriginalConnection() instanceof ImsPhoneConnection;
    }

    public boolean wasImsConnection() {
        return this.mWasImsConnection;
    }

    private static Uri getAddressFromNumber(String number) {
        if (number == null) {
            number = "";
        }
        return Uri.fromParts("tel", number, null);
    }

    private int applyCapability(int capabilities, int capability) {
        int newCapabilities = capabilities | capability;
        return newCapabilities;
    }

    private int removeCapability(int capabilities, int capability) {
        int newCapabilities = capabilities & (capability ^ (-1));
        return newCapabilities;
    }

    public final TelephonyConnection addTelephonyConnectionListener(TelephonyConnectionListener l) {
        this.mTelephonyListeners.add(l);
        if (this.mOriginalConnection != null) {
            fireOnOriginalConnectionConfigured();
        }
        return this;
    }

    public final TelephonyConnection removeTelephonyConnectionListener(TelephonyConnectionListener l) {
        if (l != null) {
            this.mTelephonyListeners.remove(l);
        }
        return this;
    }

    private final void fireOnOriginalConnectionConfigured() {
        for (TelephonyConnectionListener l : this.mTelephonyListeners) {
            l.onOriginalConnectionConfigured(this);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TelephonyConnection objId:");
        sb.append(System.identityHashCode(this));
        sb.append(" type:");
        if (isImsConnection()) {
            sb.append("ims");
        } else if (this instanceof GsmConnection) {
            sb.append("gsm");
        } else if (this instanceof CdmaConnection) {
            sb.append("cdma");
        }
        sb.append(" state:");
        sb.append(android.telecom.Connection.stateToString(getState()));
        sb.append(" capabilities:");
        sb.append(capabilitiesToString(getConnectionCapabilities()));
        sb.append(" address:");
        sb.append(Log.pii(getAddress()));
        sb.append(" originalConnection:");
        sb.append(this.mOriginalConnection);
        sb.append(" partOfConf:");
        if (getConference() == null) {
            sb.append("N");
        } else {
            sb.append("Y");
        }
        sb.append("]");
        return sb.toString();
    }
}
