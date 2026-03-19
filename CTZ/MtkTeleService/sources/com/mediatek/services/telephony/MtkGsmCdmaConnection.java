package com.mediatek.services.telephony;

import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.settings.SettingsConstants;
import com.android.services.telephony.DisconnectCauseUtil;
import com.android.services.telephony.EmergencyTonePlayer;
import com.android.services.telephony.Log;
import com.android.services.telephony.TelephonyConnection;
import com.android.services.telephony.TelephonyConnectionService;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import java.util.LinkedList;
import java.util.Queue;
import mediatek.telecom.MtkConnection;

public class MtkGsmCdmaConnection extends TelephonyConnection {
    private static final boolean MTK_SVLTE_SUPPORT = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.boot.opt_c2k_lte_mode"));
    private boolean mAllowMute;
    private boolean mDtmfBurstConfirmationPending;
    private final Queue<Character> mDtmfQueue;
    private final EmergencyTonePlayer mEmergencyTonePlayer;
    private final Handler mHandler;
    private boolean mIsCallWaiting;
    private boolean mIsForceDialing;
    private final boolean mIsOutgoing;
    private int mPhoneType;

    public MtkGsmCdmaConnection(int i, Connection connection, String str, EmergencyTonePlayer emergencyTonePlayer, boolean z, boolean z2) {
        super(connection, str, z2);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        MtkGsmCdmaConnection.this.hangupCallWaiting(1);
                        break;
                    case 2:
                        MtkGsmCdmaConnection.this.handleBurstDtmfConfirmation();
                        break;
                    case 3:
                        MtkGsmCdmaConnection.this.handleFakeHold(message.arg1);
                        break;
                }
            }
        };
        this.mDtmfQueue = new LinkedList();
        boolean z3 = false;
        this.mDtmfBurstConfirmationPending = false;
        this.mIsForceDialing = false;
        this.mPhoneType = i;
        Log.d(this, "MtkGsmCdmaConnection constructor mPhoneType = " + this.mPhoneType, new Object[0]);
        this.mEmergencyTonePlayer = emergencyTonePlayer;
        this.mAllowMute = z;
        this.mIsOutgoing = z2;
        if (connection != null && connection.getState() == Call.State.WAITING) {
            z3 = true;
        }
        this.mIsCallWaiting = z3;
        boolean z4 = getOriginalConnection() instanceof ImsPhoneConnection;
        if (this.mPhoneType == 2 && this.mIsCallWaiting && !z4) {
            startCallWaitingTimer();
        }
    }

    private void log(String str) {
        Log.d("MtkGsmCdmaConn", str, new Object[0]);
    }

    public int getPhoneType() {
        return this.mPhoneType;
    }

    @Override
    public void setOriginalConnection(Connection connection) {
        int i = this.mPhoneType;
        int phoneType = connection.getPhoneType();
        if (phoneType == 5) {
            this.mPhoneType = connection.getCall().getPhone().getDefaultPhone().getPhoneType();
            this.mAllowMute = true;
        } else {
            this.mPhoneType = phoneType;
        }
        boolean z = false;
        Log.d(this, "setOriginalConnection origPhoneType: " + phoneType + "mPhoneType: " + i + " -> " + this.mPhoneType, new Object[0]);
        super.setOriginalConnection(connection);
        if (connection != null && connection.getState() == Call.State.WAITING) {
            z = true;
        }
        this.mIsCallWaiting = z;
        boolean z2 = getOriginalConnection() instanceof ImsPhoneConnection;
        if (this.mPhoneType == 2 && this.mIsCallWaiting && this.mHandler != null && !z2) {
            startCallWaitingTimer();
        }
    }

    @Override
    public TelephonyConnection cloneConnection() {
        return new MtkGsmCdmaConnection(this.mPhoneType, getOriginalConnection(), getTelecomCallId(), this.mEmergencyTonePlayer, this.mAllowMute, this.mIsOutgoing);
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (this.mPhoneType == 1) {
            if (getPhone() != null) {
                getPhone().startDtmf(c);
                this.mDtmfRequestIsStarted = true;
                return;
            }
            return;
        }
        if (this.mPhoneType != 2 || getPhone() == null) {
            return;
        }
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
        if (this.mPhoneType == 1) {
            if (getPhone() != null) {
                getPhone().stopDtmf();
                this.mDtmfRequestIsStarted = false;
                return;
            }
            return;
        }
        if (this.mPhoneType == 2 && getPhone() != null && !useBurstDtmf()) {
            getPhone().stopDtmf();
        }
    }

    @Override
    protected int buildConnectionProperties() {
        int iBuildConnectionProperties = super.buildConnectionProperties();
        if (this.mPhoneType == 1) {
            if ((getConnectionProperties() & 64) != 0) {
                return iBuildConnectionProperties | 64;
            }
            return iBuildConnectionProperties;
        }
        if (this.mPhoneType == 2 && !isImsConnection()) {
            return iBuildConnectionProperties | 65536;
        }
        return iBuildConnectionProperties;
    }

    @Override
    protected int buildConnectionCapabilities() {
        boolean zIsRealConnected;
        int iBuildConnectionCapabilities = super.buildConnectionCapabilities();
        if (this.mPhoneType == 1) {
            iBuildConnectionCapabilities |= 64;
        } else if (this.mPhoneType == 2 && this.mAllowMute) {
            iBuildConnectionCapabilities |= 64;
        }
        boolean zIsFeatureSupported = false;
        if (this.mPhoneType == 1) {
            if (!shouldTreatAsEmergencyCall()) {
                iBuildConnectionCapabilities |= 2;
                if (isHoldable() && (getState() == 4 || getState() == 5)) {
                    iBuildConnectionCapabilities |= 1;
                }
            }
            if (getConnectionService() != null) {
                TelephonyConnectionService telephonyConnectionService = (TelephonyConnectionService) getConnectionService();
                if (telephonyConnectionService.canTransfer(this)) {
                    iBuildConnectionCapabilities |= 134217728;
                }
                Phone phone = null;
                if (this.mOriginalConnection != null && this.mOriginalConnection.getCall() != null) {
                    phone = this.mOriginalConnection.getCall().getPhone();
                }
                if (phone != null && (phone instanceof MtkImsPhone)) {
                    zIsFeatureSupported = ((MtkImsPhone) phone).isFeatureSupported(MtkImsPhone.FeatureType.VOLTE_ECT);
                }
                if (telephonyConnectionService.canBlindAssuredTransfer(this) && zIsFeatureSupported && getState() == 4 && !shouldTreatAsEmergencyCall()) {
                    iBuildConnectionCapabilities |= 536870912;
                }
            }
        } else if (this.mPhoneType == 2 && MTK_SVLTE_SUPPORT && !isImsConnection()) {
            com.mediatek.internal.telephony.MtkGsmCdmaConnection originalConnection = getOriginalConnection();
            if ((originalConnection instanceof com.mediatek.internal.telephony.MtkGsmCdmaConnection) && 2 == originalConnection.getPhoneType()) {
                zIsRealConnected = originalConnection.isRealConnected();
            } else {
                zIsRealConnected = getState() == 4;
            }
            Log.d(this, "buildConnectionCapabilities, origConn=" + originalConnection + ", isRealConnected=" + zIsRealConnected, new Object[0]);
            if (!shouldTreatAsEmergencyCall()) {
                iBuildConnectionCapabilities |= 2;
                if ((getState() == 4 && ((this.mIsOutgoing && zIsRealConnected) || !this.mIsOutgoing)) || getState() == 5) {
                    iBuildConnectionCapabilities |= 1;
                }
            }
        }
        log("buildConnectionCapabilities: " + MtkConnection.capabilitiesToString(iBuildConnectionCapabilities));
        return iBuildConnectionCapabilities;
    }

    @Override
    protected void onRemovedFromCallService() {
        super.onRemovedFromCallService();
    }

    @Override
    public void onReject() {
        Connection originalConnection;
        if (this.mPhoneType == 1) {
            super.onReject();
            return;
        }
        if (this.mPhoneType == 2 && (originalConnection = getOriginalConnection()) != null) {
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
        if (this.mPhoneType == 2) {
            this.mHandler.removeMessages(1);
        }
        super.onAnswer();
    }

    @Override
    public void onStateChanged(int i) {
        if (this.mPhoneType == 2) {
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
        }
        super.onStateChanged(i);
    }

    @Override
    public void performConference(android.telecom.Connection connection) {
        if (this.mPhoneType == 1) {
            super.performConference(connection);
        } else if (this.mPhoneType == 2) {
            if (isImsConnection()) {
                super.performConference(connection);
            } else {
                Log.w(this, "Non-IMS CDMA Connection attempted to call performConference.", new Object[0]);
            }
        }
    }

    public void forceAsDialing(boolean z) {
        if (z) {
            setStateOverride(Call.State.DIALING);
            this.mIsForceDialing = true;
        } else {
            resetStateOverride();
            this.mIsForceDialing = false;
        }
    }

    public boolean isOutgoing() {
        return this.mIsOutgoing;
    }

    public boolean isCallWaiting() {
        return this.mIsCallWaiting;
    }

    private void startCallWaitingTimer() {
        this.mHandler.removeMessages(1);
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
        Phone phone = getPhone();
        if (phone != null) {
            phone.sendBurstDtmf(str, 0, 0, this.mHandler.obtainMessage(2));
            this.mDtmfBurstConfirmationPending = true;
        }
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
        return (phone == null || getAddress() == null || !PhoneNumberUtils.isLocalEmergencyNumber(phone.getContext(), getAddress().getSchemeSpecificPart())) ? false : true;
    }

    @Override
    protected void handleExitedEcmMode() {
        this.mAllowMute = true;
        super.handleExitedEcmMode();
    }

    @Override
    protected void setActiveInternal() {
        if (getState() == 4) {
            Log.w(this, "Should not be called if this is already ACTIVE", new Object[0]);
            return;
        }
        if (getConnectionService() != null) {
            for (android.telecom.Connection connection : getConnectionService().getAllConnections()) {
                if (connection != this && (connection instanceof MtkGsmCdmaConnection)) {
                    MtkGsmCdmaConnection mtkGsmCdmaConnection = (MtkGsmCdmaConnection) connection;
                    if (mtkGsmCdmaConnection.getState() == 4) {
                        mtkGsmCdmaConnection.updateState();
                    }
                }
            }
        }
        if (this.mIsForceDialing) {
            return;
        }
        setActive();
    }

    @Override
    public void performHold() {
        if (this.mPhoneType == 1) {
            super.performHold();
            return;
        }
        if (this.mPhoneType == 2) {
            if (MTK_SVLTE_SUPPORT && !isImsConnection()) {
                Log.d(this, "performHold, just set the hold status.", new Object[0]);
                this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 3, 1, 0), 200L);
            } else {
                super.performHold();
            }
        }
    }

    @Override
    public void performUnhold() {
        if (this.mPhoneType == 1) {
            super.performUnhold();
            return;
        }
        if (this.mPhoneType == 2) {
            if (MTK_SVLTE_SUPPORT && !isImsConnection()) {
                Log.d(this, "performUnhold, just set the active status.", new Object[0]);
                this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 3, 0, 0), 200L);
            } else {
                super.performUnhold();
            }
        }
    }

    private void handleFakeHold(int i) {
        Log.d(this, "handleFakeHold, operation=", Integer.valueOf(i));
        if (1 == i) {
            setOnHold();
        } else if (i == 0) {
            setActive();
        }
        updateState();
    }
}
