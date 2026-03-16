package com.android.services.telephony;

import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import java.util.LinkedList;
import java.util.Queue;

final class CdmaConnection extends TelephonyConnection {
    private final boolean mAllowMute;
    private boolean mDtmfBurstConfirmationPending;
    private final Queue<Character> mDtmfQueue;
    private final EmergencyTonePlayer mEmergencyTonePlayer;
    private final Handler mHandler;
    private boolean mIsCallWaiting;
    private final boolean mIsOutgoing;

    CdmaConnection(Connection connection, EmergencyTonePlayer emergencyTonePlayer, boolean allowMute, boolean isOutgoing) {
        super(connection);
        boolean z = false;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
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
        this.mDtmfBurstConfirmationPending = false;
        this.mEmergencyTonePlayer = emergencyTonePlayer;
        this.mAllowMute = allowMute;
        this.mIsOutgoing = isOutgoing;
        if (connection != null && connection.getState() == Call.State.WAITING) {
            z = true;
        }
        this.mIsCallWaiting = z;
        if (this.mIsCallWaiting) {
            startCallWaitingTimer();
        }
    }

    @Override
    public void onPlayDtmfTone(char digit) {
        if (useBurstDtmf()) {
            Log.i(this, "sending dtmf digit as burst", new Object[0]);
            sendShortDtmfToNetwork(digit);
        } else {
            Log.i(this, "sending dtmf digit directly", new Object[0]);
            getPhone().startDtmf(digit);
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
        Connection connection = getOriginalConnection();
        if (connection != null) {
            switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$Call$State[connection.getState().ordinal()]) {
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
        CdmaConnection cdmaConnection = new CdmaConnection(getOriginalConnection(), this.mEmergencyTonePlayer, this.mAllowMute, this.mIsOutgoing);
        return cdmaConnection;
    }

    @Override
    public void onStateChanged(int state) {
        Connection originalConnection = getOriginalConnection();
        this.mIsCallWaiting = originalConnection != null && originalConnection.getState() == Call.State.WAITING;
        if (state == 3) {
            if (isEmergency()) {
                this.mEmergencyTonePlayer.start();
            }
        } else {
            this.mEmergencyTonePlayer.stop();
        }
        super.onStateChanged(state);
    }

    @Override
    protected int buildConnectionCapabilities() {
        int capabilities = super.buildConnectionCapabilities();
        if (this.mAllowMute) {
            return capabilities | 64;
        }
        return capabilities;
    }

    @Override
    public void performConference(TelephonyConnection otherConnection) {
        if (isImsConnection()) {
            super.performConference(otherConnection);
        } else {
            Log.w(this, "Non-IMS CDMA Connection attempted to call performConference.", new Object[0]);
        }
    }

    void forceAsDialing(boolean isDialing) {
        if (isDialing) {
            setDialing();
        } else {
            updateState();
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

    private void hangupCallWaiting(int telephonyDisconnectCause) {
        Connection originalConnection = getOriginalConnection();
        if (originalConnection != null) {
            try {
                originalConnection.hangup();
            } catch (CallStateException e) {
                Log.e(this, e, "Failed to hangup call waiting call", new Object[0]);
            }
            setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(telephonyDisconnectCause));
        }
    }

    private boolean useBurstDtmf() {
        if (isImsConnection()) {
            Log.d(this, "in ims call, return false", new Object[0]);
            return false;
        }
        int dtmfTypeSetting = Settings.System.getInt(getPhone().getContext().getContentResolver(), "dtmf_tone_type", 0);
        return dtmfTypeSetting == 0;
    }

    private void sendShortDtmfToNetwork(char digit) {
        synchronized (this.mDtmfQueue) {
            if (this.mDtmfBurstConfirmationPending) {
                this.mDtmfQueue.add(new Character(digit));
            } else {
                sendBurstDtmfStringLocked(Character.toString(digit));
            }
        }
    }

    private void sendBurstDtmfStringLocked(String dtmfString) {
        getPhone().sendBurstDtmf(dtmfString, 0, 0, this.mHandler.obtainMessage(2));
        this.mDtmfBurstConfirmationPending = true;
    }

    private void handleBurstDtmfConfirmation() {
        String dtmfDigits = null;
        synchronized (this.mDtmfQueue) {
            this.mDtmfBurstConfirmationPending = false;
            if (!this.mDtmfQueue.isEmpty()) {
                StringBuilder builder = new StringBuilder(this.mDtmfQueue.size());
                while (!this.mDtmfQueue.isEmpty()) {
                    builder.append(this.mDtmfQueue.poll());
                }
                dtmfDigits = builder.toString();
                Log.i(this, "%d dtmf character[s] removed from the queue", Integer.valueOf(dtmfDigits.length()));
            }
            if (dtmfDigits != null) {
                sendBurstDtmfStringLocked(dtmfDigits);
            }
        }
    }

    private boolean isEmergency() {
        Phone phone = getPhone();
        return phone != null && PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), getAddress().getSchemeSpecificPart());
    }
}
