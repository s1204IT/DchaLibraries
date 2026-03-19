package com.android.services.telephony;

import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import java.util.LinkedList;
import java.util.Queue;

final class CdmaConnection extends TelephonyConnection {
    private static final int MSG_CALL_WAITING_MISSED = 1;
    private static final int MSG_DTMF_SEND_CONFIRMATION = 2;
    private static final int TIMEOUT_CALL_WAITING_MILLIS = 20000;
    private boolean mAllowMute;
    private boolean mDtmfBurstConfirmationPending;
    private final Queue<Character> mDtmfQueue;
    private final EmergencyTonePlayer mEmergencyTonePlayer;
    private final Handler mHandler;
    private boolean mIsCallWaiting;

    CdmaConnection(Connection connection, EmergencyTonePlayer emergencyTonePlayer, boolean z, boolean z2, String str) {
        super(connection, str, z2);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        CdmaConnection.this.hangupCallWaiting(1);
                        break;
                    case 2:
                        CdmaConnection.this.handleBurstDtmfConfirmation();
                        break;
                }
            }
        };
        this.mDtmfQueue = new LinkedList();
        boolean z3 = false;
        this.mDtmfBurstConfirmationPending = false;
        this.mEmergencyTonePlayer = emergencyTonePlayer;
        this.mAllowMute = z;
        if (connection != null && connection.getState() == Call.State.WAITING) {
            z3 = true;
        }
        this.mIsCallWaiting = z3;
        boolean z4 = getOriginalConnection() instanceof ImsPhoneConnection;
        if (this.mIsCallWaiting && !z4) {
            startCallWaitingTimer();
        }
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (useBurstDtmf()) {
            Log.i(this, "sending dtmf digit as burst", new Object[0]);
            sendShortDtmfToNetwork(c);
        } else {
            Log.i(this, "sending dtmf digit directly", new Object[0]);
            getPhone().startDtmf(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        if (!useBurstDtmf()) {
            getPhone().stopDtmf();
        }
    }

    @Override
    public void onReject() {
        Connection originalConnection = getOriginalConnection();
        if (originalConnection != null) {
            switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$Call$State[originalConnection.getState().ordinal()]) {
                case 1:
                    super.onReject();
                    break;
                case 2:
                    hangupCallWaiting(16);
                    break;
                default:
                    Log.e(this, new Exception(), "Rejecting a non-ringing call", new Object[0]);
                    super.onReject();
                    break;
            }
        }
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$Call$State = new int[Call.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.INCOMING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.WAITING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    @Override
    public void onAnswer() {
        this.mHandler.removeMessages(1);
        super.onAnswer();
    }

    @Override
    public TelephonyConnection cloneConnection() {
        return new CdmaConnection(getOriginalConnection(), this.mEmergencyTonePlayer, this.mAllowMute, this.mIsOutgoing, getTelecomCallId());
    }

    @Override
    public void onStateChanged(int i) {
        Connection originalConnection = getOriginalConnection();
        this.mIsCallWaiting = originalConnection != null && originalConnection.getState() == Call.State.WAITING;
        if (this.mEmergencyTonePlayer != null) {
            if (i == 3) {
                if (isEmergency()) {
                    this.mEmergencyTonePlayer.start();
                }
            } else {
                this.mEmergencyTonePlayer.stop();
            }
        }
        super.onStateChanged(i);
    }

    @Override
    protected int buildConnectionCapabilities() {
        int iBuildConnectionCapabilities = super.buildConnectionCapabilities();
        if (this.mAllowMute) {
            return iBuildConnectionCapabilities | 64;
        }
        return iBuildConnectionCapabilities;
    }

    @Override
    public void performConference(android.telecom.Connection connection) {
        if (isImsConnection()) {
            super.performConference(connection);
        } else {
            Log.w(this, "Non-IMS CDMA Connection attempted to call performConference.", new Object[0]);
        }
    }

    void forceAsDialing(boolean z) {
        if (z) {
            setStateOverride(Call.State.DIALING);
        } else {
            resetStateOverride();
        }
    }

    boolean isOutgoing() {
        return this.mIsOutgoing;
    }

    boolean isCallWaiting() {
        return this.mIsCallWaiting;
    }

    private void startCallWaitingTimer() {
        this.mHandler.sendEmptyMessageDelayed(1, 20000L);
    }

    private void hangupCallWaiting(int i) {
        Connection originalConnection = getOriginalConnection();
        if (originalConnection != null) {
            try {
                originalConnection.hangup();
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Failed to hangup call waiting call", new Object[0]);
            }
            setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(i));
        }
    }

    private boolean useBurstDtmf() {
        if (!isImsConnection()) {
            return Settings.System.getInt(getPhone().getContext().getContentResolver(), "dtmf_tone_type", 0) == 0;
        }
        Log.d(this, "in ims call, return false", new Object[0]);
        return false;
    }

    private void sendShortDtmfToNetwork(char c) {
        synchronized (this.mDtmfQueue) {
            if (this.mDtmfBurstConfirmationPending) {
                this.mDtmfQueue.add(new Character(c));
            } else {
                sendBurstDtmfStringLocked(Character.toString(c));
            }
        }
    }

    private void sendBurstDtmfStringLocked(String str) {
        getPhone().sendBurstDtmf(str, 0, 0, this.mHandler.obtainMessage(2));
        this.mDtmfBurstConfirmationPending = true;
    }

    private void handleBurstDtmfConfirmation() {
        String string;
        synchronized (this.mDtmfQueue) {
            this.mDtmfBurstConfirmationPending = false;
            if (!this.mDtmfQueue.isEmpty()) {
                StringBuilder sb = new StringBuilder(this.mDtmfQueue.size());
                while (!this.mDtmfQueue.isEmpty()) {
                    sb.append(this.mDtmfQueue.poll());
                }
                string = sb.toString();
                Log.i(this, "%d dtmf character[s] removed from the queue", Integer.valueOf(string.length()));
            } else {
                string = null;
            }
            if (string != null) {
                sendBurstDtmfStringLocked(string);
            }
        }
    }

    private boolean isEmergency() {
        Phone phone = getPhone();
        return phone != null && PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), getAddress().getSchemeSpecificPart());
    }

    @Override
    protected void handleExitedEcmMode() {
        this.mAllowMute = true;
        super.handleExitedEcmMode();
    }
}
