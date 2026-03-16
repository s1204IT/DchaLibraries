package com.android.services.telephony.sip;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telecom.AudioState;
import android.telecom.Connection;
import android.util.EventLog;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.sip.SipPhone;
import com.android.services.telephony.DisconnectCauseUtil;
import com.android.services.telephony.Log;
import java.util.Objects;

final class SipConnection extends Connection {
    private com.android.internal.telephony.Connection mOriginalConnection;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SipConnection.this.updateState(false);
                    break;
            }
        }
    };
    private Call.State mOriginalConnectionState = Call.State.IDLE;

    SipConnection() {
        setInitializing();
    }

    void initialize(com.android.internal.telephony.Connection connection) {
        this.mOriginalConnection = connection;
        if (getPhone() != null) {
            getPhone().registerForPreciseCallStateChanged(this.mHandler, 1, (Object) null);
        }
        updateAddress();
        setInitialized();
    }

    public void onAudioStateChanged(AudioState state) {
        if (getPhone() != null) {
            getPhone().setEchoSuppressionEnabled();
        }
    }

    @Override
    public void onStateChanged(int state) {
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (getPhone() != null) {
            getPhone().startDtmf(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        if (getPhone() != null) {
            getPhone().stopDtmf();
        }
    }

    @Override
    public void onDisconnect() {
        try {
            if (getCall() != null && !getCall().isMultiparty()) {
                getCall().hangup();
            } else if (this.mOriginalConnection != null) {
                this.mOriginalConnection.hangup();
            }
        } catch (CallStateException e) {
            log("onDisconnect, exception: " + e);
        }
    }

    @Override
    public void onSeparate() {
        try {
            if (this.mOriginalConnection != null) {
                this.mOriginalConnection.separate();
            }
        } catch (CallStateException e) {
            log("onSeparate, exception: " + e);
        }
    }

    @Override
    public void onAbort() {
        onDisconnect();
    }

    @Override
    public void onHold() {
        try {
            if (getPhone() != null && getState() == 4) {
                getPhone().switchHoldingAndActive();
            }
        } catch (CallStateException e) {
            log("onHold, exception: " + e);
        }
    }

    @Override
    public void onUnhold() {
        try {
            if (getPhone() != null && getState() == 5) {
                getPhone().switchHoldingAndActive();
            }
        } catch (CallStateException e) {
            log("onUnhold, exception: " + e);
        }
    }

    @Override
    public void onAnswer(int videoState) {
        try {
            if (isValidRingingCall() && getPhone() != null) {
                getPhone().acceptCall(videoState);
            }
        } catch (CallStateException e) {
            log("onAnswer, exception: " + e);
        } catch (IllegalArgumentException e2) {
            log("onAnswer, IllegalArgumentException: " + e2);
            EventLog.writeEvent(1397638484, "31752213", -1, "Invalid SDP.");
            onReject();
        } catch (IllegalStateException e3) {
            log("onAnswer, IllegalStateException: " + e3);
            EventLog.writeEvent(1397638484, "31752213", -1, "Invalid codec.");
            onReject();
        }
    }

    @Override
    public void onReject() {
        try {
            if (isValidRingingCall() && getPhone() != null) {
                getPhone().rejectCall();
            }
        } catch (CallStateException e) {
            log("onReject, exception: " + e);
        }
    }

    @Override
    public void onPostDialContinue(boolean proceed) {
    }

    private Call getCall() {
        if (this.mOriginalConnection != null) {
            return this.mOriginalConnection.getCall();
        }
        return null;
    }

    SipPhone getPhone() {
        Call call = getCall();
        if (call != null) {
            return call.getPhone();
        }
        return null;
    }

    private boolean isValidRingingCall() {
        Call call = getCall();
        return call != null && call.getState().isRinging() && call.getEarliestConnection() == this.mOriginalConnection;
    }

    private void updateState(boolean force) {
        if (this.mOriginalConnection != null) {
            Call.State newState = this.mOriginalConnection.getState();
            if (force || this.mOriginalConnectionState != newState) {
                this.mOriginalConnectionState = newState;
                switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$Call$State[newState.ordinal()]) {
                    case 2:
                        setActive();
                        break;
                    case 3:
                        setOnHold();
                        break;
                    case 4:
                    case 5:
                        setDialing();
                        setRingbackRequested(true);
                        break;
                    case 6:
                    case 7:
                        setRinging();
                        break;
                    case 8:
                        setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(this.mOriginalConnection.getDisconnectCause()));
                        close();
                        break;
                }
                updateCallCapabilities(force);
            }
        }
    }

    static class AnonymousClass2 {
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

    private int buildCallCapabilities() {
        if (getState() != 4 && getState() != 5) {
            return 66;
        }
        int capabilities = 66 | 1;
        return capabilities;
    }

    void updateCallCapabilities(boolean force) {
        int newCallCapabilities = buildCallCapabilities();
        if (force || getConnectionCapabilities() != newCallCapabilities) {
            setConnectionCapabilities(newCallCapabilities);
        }
    }

    void onAddedToCallService() {
        updateState(true);
        updateCallCapabilities(true);
        setAudioModeIsVoip(true);
        if (this.mOriginalConnection != null) {
            setCallerDisplayName(this.mOriginalConnection.getCnapName(), this.mOriginalConnection.getCnapNamePresentation());
        }
    }

    private void updateAddress() {
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

    private static Uri getAddressFromNumber(String number) {
        if (number == null) {
            number = "";
        }
        return Uri.fromParts("sip", number, null);
    }

    private void close() {
        if (getPhone() != null) {
            getPhone().unregisterForPreciseCallStateChanged(this.mHandler);
        }
        this.mOriginalConnection = null;
        destroy();
    }

    private static void log(String msg) {
        android.util.Log.d("SIP", "[SipConnection] " + msg);
    }
}
