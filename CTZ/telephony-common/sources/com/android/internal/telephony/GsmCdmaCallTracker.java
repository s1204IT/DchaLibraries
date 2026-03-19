package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GsmCdmaCallTracker extends CallTracker {
    protected static final boolean DBG_POLL = false;
    protected static final String LOG_TAG = "GsmCdmaCallTracker";
    protected static final int MAX_CONNECTIONS_CDMA = 8;
    public static final int MAX_CONNECTIONS_GSM = 19;
    private static final int MAX_CONNECTIONS_PER_CALL_CDMA = 1;
    private static final int MAX_CONNECTIONS_PER_CALL_GSM = 5;
    protected static final boolean REPEAT_POLLING = false;
    protected static final boolean VDBG = false;
    protected int m3WayCallFlashDelay;

    @VisibleForTesting
    public GsmCdmaConnection[] mConnections;
    protected boolean mHangupPendingMO;
    protected boolean mIsEcmTimerCanceled;
    protected boolean mIsInEmergencyCall;
    protected int mPendingCallClirMode;
    protected boolean mPendingCallInEcm;
    protected GsmCdmaConnection mPendingMO;
    public GsmCdmaPhone mPhone;
    protected RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    protected RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();
    protected ArrayList<GsmCdmaConnection> mDroppedDuringPoll = new ArrayList<>(19);
    public GsmCdmaCall mRingingCall = new GsmCdmaCall(this);
    public GsmCdmaCall mForegroundCall = new GsmCdmaCall(this);
    public GsmCdmaCall mBackgroundCall = new GsmCdmaCall(this);
    private boolean mDesiredMute = false;
    public PhoneConstants.State mState = PhoneConstants.State.IDLE;
    protected TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();
    private RegistrantList mCallWaitingRegistrants = new RegistrantList();
    private BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                boolean booleanExtra = intent.getBooleanExtra("phoneinECMState", false);
                GsmCdmaCallTracker.this.log("Received ACTION_EMERGENCY_CALLBACK_MODE_CHANGED isInEcm = " + booleanExtra);
                if (!booleanExtra) {
                    ArrayList<Connection> arrayList = new ArrayList();
                    arrayList.addAll(GsmCdmaCallTracker.this.mRingingCall.getConnections());
                    arrayList.addAll(GsmCdmaCallTracker.this.mForegroundCall.getConnections());
                    arrayList.addAll(GsmCdmaCallTracker.this.mBackgroundCall.getConnections());
                    if (GsmCdmaCallTracker.this.mPendingMO != null) {
                        arrayList.add(GsmCdmaCallTracker.this.mPendingMO);
                    }
                    for (Connection connection : arrayList) {
                        if (connection != null) {
                            connection.onExitedEcmMode();
                        }
                    }
                }
            }
        }
    };

    public GsmCdmaCallTracker(GsmCdmaPhone gsmCdmaPhone) {
        this.mPhone = gsmCdmaPhone;
        this.mCi = gsmCdmaPhone.mCi;
        this.mCi.registerForCallStateChanged(this, 2, null);
        this.mCi.registerForOn(this, 9, null);
        this.mCi.registerForNotAvailable(this, 10, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mEcmExitReceiver, intentFilter);
        updatePhoneType(true);
    }

    public void updatePhoneType() {
        updatePhoneType(false);
    }

    protected void updatePhoneType(boolean z) {
        if (!z) {
            reset();
            pollCallsWhenSafe();
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            this.mConnections = new GsmCdmaConnection[19];
            this.mCi.unregisterForCallWaitingInfo(this);
            if (this.mIsInEmergencyCall) {
                this.mPhone.mDcTracker.setInternalDataEnabled(true);
                return;
            }
            return;
        }
        this.mConnections = new GsmCdmaConnection[8];
        this.mPendingCallInEcm = false;
        this.mIsInEmergencyCall = false;
        this.mPendingCallClirMode = 0;
        this.mIsEcmTimerCanceled = false;
        this.m3WayCallFlashDelay = 0;
        this.mCi.registerForCallWaitingInfo(this, 15, null);
    }

    protected void reset() {
        Rlog.d(LOG_TAG, "reset");
        for (GsmCdmaConnection gsmCdmaConnection : this.mConnections) {
            if (gsmCdmaConnection != null) {
                gsmCdmaConnection.onDisconnect(36);
                gsmCdmaConnection.dispose();
            }
        }
        if (this.mPendingMO != null) {
            this.mPendingMO.dispose();
        }
        this.mConnections = null;
        this.mPendingMO = null;
        clearDisconnected();
    }

    protected void finalize() {
        Rlog.d(LOG_TAG, "GsmCdmaCallTracker finalized");
    }

    @Override
    public void registerForVoiceCallStarted(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mVoiceCallStartedRegistrants.add(registrant);
        if (this.mState != PhoneConstants.State.IDLE) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    @Override
    public void unregisterForVoiceCallStarted(Handler handler) {
        this.mVoiceCallStartedRegistrants.remove(handler);
    }

    @Override
    public void registerForVoiceCallEnded(Handler handler, int i, Object obj) {
        this.mVoiceCallEndedRegistrants.add(new Registrant(handler, i, obj));
    }

    @Override
    public void unregisterForVoiceCallEnded(Handler handler) {
        this.mVoiceCallEndedRegistrants.remove(handler);
    }

    public void registerForCallWaiting(Handler handler, int i, Object obj) {
        this.mCallWaitingRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForCallWaiting(Handler handler) {
        this.mCallWaitingRegistrants.remove(handler);
    }

    protected void fakeHoldForegroundBeforeDial() {
        List list = (List) this.mForegroundCall.mConnections.clone();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            ((GsmCdmaConnection) list.get(i)).fakeHoldBeforeDial();
        }
    }

    public synchronized Connection dial(String str, int i, UUSInfo uUSInfo, Bundle bundle) throws CallStateException {
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        String strConvertNumberIfNecessary = convertNumberIfNecessary(this.mPhone, str);
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            switchWaitingOrHoldingAndActive();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
            }
            fakeHoldForegroundBeforeDial();
        }
        if (this.mForegroundCall.getState() != Call.State.IDLE) {
            throw new CallStateException("cannot dial in current state");
        }
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(strConvertNumberIfNecessary), this, this.mForegroundCall, PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), strConvertNumberIfNecessary));
        this.mHangupPendingMO = false;
        this.mMetrics.writeRilDial(this.mPhone.getPhoneId(), this.mPendingMO, i, uUSInfo);
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            pollCallsWhenSafe();
        } else {
            setMute(false);
            this.mCi.dial(this.mPendingMO.getAddress(), i, uUSInfo, obtainCompleteMessage());
        }
        if (this.mNumberConverted) {
            this.mPendingMO.setConverted(str);
            this.mNumberConverted = false;
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return this.mPendingMO;
    }

    protected void handleEcmTimer(int i) {
        this.mPhone.handleTimerInEmergencyCallbackMode(i);
        switch (i) {
            case 0:
                this.mIsEcmTimerCanceled = false;
                break;
            case 1:
                this.mIsEcmTimerCanceled = true;
                break;
            default:
                Rlog.e(LOG_TAG, "handleEcmTimer, unsupported action " + i);
                break;
        }
    }

    protected void disableDataCallInEmergencyCall(String str) {
        if (PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), str)) {
            log("disableDataCallInEmergencyCall");
            setIsInEmergencyCall();
        }
    }

    public void setIsInEmergencyCall() {
        this.mIsInEmergencyCall = true;
        this.mPhone.mDcTracker.setInternalDataEnabled(false);
        this.mPhone.notifyEmergencyCallRegistrants(true);
        this.mPhone.sendEmergencyCallStateChange(true);
    }

    protected Connection dial(String str, int i) throws CallStateException {
        String strConvertNumberIfNecessary;
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        TelephonyManager telephonyManager = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        String networkCountryIsoForPhone = telephonyManager.getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
        String simCountryIsoForPhone = telephonyManager.getSimCountryIsoForPhone(this.mPhone.getPhoneId());
        boolean z = (TextUtils.isEmpty(networkCountryIsoForPhone) || TextUtils.isEmpty(simCountryIsoForPhone) || simCountryIsoForPhone.equals(networkCountryIsoForPhone)) ? false : true;
        if (z) {
            if (!"us".equals(simCountryIsoForPhone)) {
                if ("vi".equals(simCountryIsoForPhone)) {
                    if (!z || "us".equals(networkCountryIsoForPhone)) {
                    }
                }
            } else {
                z = z && !"vi".equals(networkCountryIsoForPhone);
            }
        }
        if (z) {
            strConvertNumberIfNecessary = convertNumberIfNecessary(this.mPhone, str);
        } else {
            strConvertNumberIfNecessary = str;
        }
        boolean zIsInEcm = this.mPhone.isInEcm();
        boolean zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), strConvertNumberIfNecessary);
        if (zIsInEcm && zIsLocalEmergencyNumber) {
            handleEcmTimer(1);
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            return dialThreeWay(strConvertNumberIfNecessary);
        }
        this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(strConvertNumberIfNecessary), this, this.mForegroundCall, zIsLocalEmergencyNumber);
        this.mHangupPendingMO = false;
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            pollCallsWhenSafe();
        } else {
            setMute(false);
            disableDataCallInEmergencyCall(strConvertNumberIfNecessary);
            if (!zIsInEcm || (zIsInEcm && zIsLocalEmergencyNumber)) {
                this.mCi.dial(this.mPendingMO.getAddress(), i, obtainCompleteMessage());
            } else {
                this.mPhone.exitEmergencyCallbackMode();
                this.mPhone.setOnEcbModeExitResponse(this, 14, null);
                this.mPendingCallClirMode = i;
                this.mPendingCallInEcm = true;
            }
        }
        if (this.mNumberConverted) {
            this.mPendingMO.setConverted(str);
            this.mNumberConverted = false;
        }
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
        return this.mPendingMO;
    }

    protected Connection dialThreeWay(String str) {
        if (!this.mForegroundCall.isIdle()) {
            disableDataCallInEmergencyCall(str);
            this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(str), this, this.mForegroundCall, this.mIsInEmergencyCall);
            PersistableBundle config = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfig();
            if (config != null) {
                this.m3WayCallFlashDelay = config.getInt("cdma_3waycall_flash_delay_int");
            } else {
                this.m3WayCallFlashDelay = 0;
            }
            if (this.m3WayCallFlashDelay > 0) {
                this.mCi.sendCDMAFeatureCode("", obtainMessage(20));
            } else {
                this.mCi.sendCDMAFeatureCode(this.mPendingMO.getAddress(), obtainMessage(16));
            }
            return this.mPendingMO;
        }
        return null;
    }

    public Connection dial(String str) throws CallStateException {
        if (isPhoneTypeGsm()) {
            return dial(str, 0, (Bundle) null);
        }
        return dial(str, 0);
    }

    public Connection dial(String str, UUSInfo uUSInfo, Bundle bundle) throws CallStateException {
        return dial(str, 0, uUSInfo, bundle);
    }

    private Connection dial(String str, int i, Bundle bundle) throws CallStateException {
        return dial(str, i, null, bundle);
    }

    public void acceptCall() throws CallStateException {
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            Rlog.i("phone", "acceptCall: incoming...");
            setMute(false);
            this.mCi.acceptCall(obtainCompleteMessage());
        } else {
            if (this.mRingingCall.getState() == Call.State.WAITING) {
                if (isPhoneTypeGsm()) {
                    setMute(false);
                } else {
                    GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection) this.mRingingCall.getLatestConnection();
                    gsmCdmaConnection.updateParent(this.mRingingCall, this.mForegroundCall);
                    gsmCdmaConnection.onConnectedInOrOut();
                    updatePhoneState();
                }
                switchWaitingOrHoldingAndActive();
                return;
            }
            throw new CallStateException("phone not ringing");
        }
    }

    public void rejectCall() throws CallStateException {
        if (this.mRingingCall.getState().isRinging()) {
            this.mCi.rejectCall(obtainCompleteMessage());
            return;
        }
        throw new CallStateException("phone not ringing");
    }

    protected void flashAndSetGenericTrue() {
        this.mCi.sendCDMAFeatureCode("", obtainMessage(8));
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        }
        if (isPhoneTypeGsm()) {
            this.mCi.switchWaitingOrHoldingAndActive(obtainCompleteMessage(8));
        } else if (this.mForegroundCall.getConnections().size() > 1) {
            flashAndSetGenericTrue();
        } else {
            this.mCi.sendCDMAFeatureCode("", obtainMessage(8));
        }
    }

    public void conference() {
        if (isPhoneTypeGsm()) {
            this.mCi.conference(obtainCompleteMessage(11));
        } else {
            flashAndSetGenericTrue();
        }
    }

    public void explicitCallTransfer() {
        this.mCi.explicitCallTransfer(obtainCompleteMessage(13));
    }

    public void clearDisconnected() {
        internalClearDisconnected();
        updatePhoneState();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public boolean canConference() {
        return this.mForegroundCall.getState() == Call.State.ACTIVE && this.mBackgroundCall.getState() == Call.State.HOLDING && !this.mBackgroundCall.isFull() && !this.mForegroundCall.isFull();
    }

    protected boolean canDial() {
        int state = this.mPhone.getServiceState().getState();
        boolean z = (state == 3 || this.mPendingMO != null || this.mRingingCall.isRinging() || SystemProperties.get("ro.telephony.disable-call", "false").equals("true") || (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive() && (isPhoneTypeGsm() || this.mForegroundCall.getState() != Call.State.ACTIVE))) ? false : true;
        if (!z) {
            Object[] objArr = new Object[8];
            objArr[0] = Integer.valueOf(state);
            objArr[1] = Boolean.valueOf(state != 3);
            objArr[2] = Boolean.valueOf(this.mPendingMO == null);
            objArr[3] = Boolean.valueOf(!this.mRingingCall.isRinging());
            objArr[4] = Boolean.valueOf(!r1.equals("true"));
            objArr[5] = Boolean.valueOf(!this.mForegroundCall.getState().isAlive());
            objArr[6] = Boolean.valueOf(this.mForegroundCall.getState() == Call.State.ACTIVE);
            objArr[7] = Boolean.valueOf(!this.mBackgroundCall.getState().isAlive());
            log(String.format("canDial is false\n((serviceState=%d) != ServiceState.STATE_POWER_OFF)::=%s\n&& pendingMO == null::=%s\n&& !ringingCall.isRinging()::=%s\n&& !disableCall.equals(\"true\")::=%s\n&& (!foregroundCall.getState().isAlive()::=%s\n   || foregroundCall.getState() == GsmCdmaCall.State.ACTIVE::=%s\n   ||!backgroundCall.getState().isAlive())::=%s)", objArr));
        }
        return z;
    }

    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            return (this.mForegroundCall.getState() == Call.State.ACTIVE || this.mForegroundCall.getState() == Call.State.ALERTING || this.mForegroundCall.getState() == Call.State.DIALING) && this.mBackgroundCall.getState() == Call.State.HOLDING;
        }
        Rlog.e(LOG_TAG, "canTransfer: not possible in CDMA");
        return false;
    }

    protected void internalClearDisconnected() {
        this.mRingingCall.clearDisconnected();
        this.mForegroundCall.clearDisconnected();
        this.mBackgroundCall.clearDisconnected();
    }

    public Message obtainCompleteMessage() {
        return obtainCompleteMessage(4);
    }

    public Message obtainCompleteMessage(int i) {
        this.mPendingOperations++;
        this.mLastRelevantPoll = null;
        this.mNeedsPoll = true;
        return obtainMessage(i);
    }

    protected void operationComplete() {
        this.mPendingOperations--;
        if (this.mPendingOperations == 0 && this.mNeedsPoll) {
            this.mLastRelevantPoll = obtainMessage(1);
            this.mCi.getCurrentCalls(this.mLastRelevantPoll);
        } else if (this.mPendingOperations < 0) {
            Rlog.e(LOG_TAG, "GsmCdmaCallTracker.pendingOperations < 0");
            this.mPendingOperations = 0;
        }
    }

    protected void updatePhoneState() {
        PhoneConstants.State state = this.mState;
        if (this.mRingingCall.isRinging()) {
            this.mState = PhoneConstants.State.RINGING;
        } else if (this.mPendingMO != null || !this.mForegroundCall.isIdle() || !this.mBackgroundCall.isIdle()) {
            this.mState = PhoneConstants.State.OFFHOOK;
        } else {
            Phone imsPhone = this.mPhone.getImsPhone();
            if (this.mState == PhoneConstants.State.OFFHOOK && imsPhone != null) {
                imsPhone.callEndCleanupHandOverCallIfAny();
            }
            this.mState = PhoneConstants.State.IDLE;
        }
        if (this.mState == PhoneConstants.State.IDLE && state != this.mState) {
            this.mVoiceCallEndedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        } else if (state == PhoneConstants.State.IDLE && state != this.mState) {
            this.mVoiceCallStartedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
        log("update phone state, old=" + state + " new=" + this.mState);
        if (this.mState != state) {
            this.mPhone.notifyPhoneStateChanged();
            this.mMetrics.writePhoneState(this.mPhone.getPhoneId(), this.mState);
        }
    }

    @Override
    protected synchronized void handlePollCalls(AsyncResult asyncResult) {
        List arrayList;
        boolean z;
        GsmCdmaConnection gsmCdmaConnection;
        Phone imsPhone;
        boolean zOnDisconnect;
        boolean z2;
        boolean z3;
        List list;
        if (asyncResult.exception == null) {
            arrayList = (List) asyncResult.result;
        } else {
            if (!isCommandExceptionRadioNotAvailable(asyncResult.exception)) {
                pollCallsAfterDelay();
                return;
            }
            arrayList = new ArrayList();
        }
        ArrayList<Connection> arrayList2 = new ArrayList();
        int size = this.mHandoverConnections.size();
        int size2 = arrayList.size();
        int i = 0;
        int i2 = 0;
        boolean z4 = true;
        boolean z5 = false;
        Connection connectionCheckMtFindNewRinging = null;
        GsmCdmaConnection gsmCdmaConnection2 = null;
        boolean z6 = false;
        while (i < this.mConnections.length) {
            GsmCdmaConnection gsmCdmaConnection3 = this.mConnections[i];
            if (i2 < size2) {
                DriverCall driverCall = (DriverCall) arrayList.get(i2);
                if (driverCall.index == i + 1) {
                    i2++;
                } else {
                    driverCall = null;
                }
                if (gsmCdmaConnection3 != null || driverCall != null) {
                    z4 = false;
                }
                if (gsmCdmaConnection3 != null || driverCall == null) {
                    list = arrayList;
                    if (gsmCdmaConnection3 != null && driverCall == null) {
                        if (isPhoneTypeGsm()) {
                            this.mDroppedDuringPoll.add(gsmCdmaConnection3);
                        } else {
                            int size3 = this.mForegroundCall.mConnections.size();
                            for (int i3 = 0; i3 < size3; i3++) {
                                log("adding fgCall cn " + i3 + " to droppedDuringPoll");
                                this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mForegroundCall.mConnections.get(i3));
                            }
                            int size4 = this.mRingingCall.mConnections.size();
                            for (int i4 = 0; i4 < size4; i4++) {
                                log("adding rgCall cn " + i4 + " to droppedDuringPoll");
                                this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mRingingCall.mConnections.get(i4));
                            }
                            if (this.mIsEcmTimerCanceled) {
                                handleEcmTimer(0);
                            }
                            checkAndEnableDataCallAfterEmergencyCallDropped();
                        }
                        this.mConnections[i] = null;
                    } else if (gsmCdmaConnection3 != null && driverCall != null && !gsmCdmaConnection3.compareTo(driverCall) && isPhoneTypeGsm()) {
                        this.mDroppedDuringPoll.add(gsmCdmaConnection3);
                        this.mConnections[i] = new GsmCdmaConnection(this.mPhone, driverCall, this, i);
                        if (this.mConnections[i].getCall() == this.mRingingCall) {
                            connectionCheckMtFindNewRinging = this.mConnections[i];
                        }
                    } else if (gsmCdmaConnection3 != null && driverCall != null) {
                        if (isPhoneTypeGsm() || gsmCdmaConnection3.isIncoming() == driverCall.isMT) {
                            z5 = z5 || gsmCdmaConnection3.update(driverCall);
                        } else if (driverCall.isMT) {
                            this.mDroppedDuringPoll.add(gsmCdmaConnection3);
                            Connection connectionCheckMtFindNewRinging2 = checkMtFindNewRinging(driverCall, i);
                            if (connectionCheckMtFindNewRinging2 == null) {
                                gsmCdmaConnection2 = gsmCdmaConnection3;
                                z6 = true;
                            }
                            checkAndEnableDataCallAfterEmergencyCallDropped();
                            connectionCheckMtFindNewRinging = connectionCheckMtFindNewRinging2;
                        } else {
                            Rlog.e(LOG_TAG, "Error in RIL, Phantom call appeared " + driverCall);
                        }
                    }
                    i++;
                    arrayList = list;
                } else if (this.mPendingMO == null || !this.mPendingMO.compareTo(driverCall)) {
                    log("pendingMo=" + this.mPendingMO + ", dc=" + driverCall);
                    this.mConnections[i] = new GsmCdmaConnection(this.mPhone, driverCall, this, i);
                    Connection hoConnection = getHoConnection(driverCall);
                    if (hoConnection != null) {
                        this.mConnections[i].migrateFrom(hoConnection);
                        if (hoConnection.mPreHandoverState != Call.State.ACTIVE && hoConnection.mPreHandoverState != Call.State.HOLDING && driverCall.state == DriverCall.State.ACTIVE) {
                            this.mConnections[i].onConnectedInOrOut();
                        }
                        this.mHandoverConnections.remove(hoConnection);
                        if (isPhoneTypeGsm()) {
                            Iterator<Connection> it = this.mHandoverConnections.iterator();
                            while (it.hasNext()) {
                                Connection next = it.next();
                                StringBuilder sb = new StringBuilder();
                                List list2 = arrayList;
                                sb.append("HO Conn state is ");
                                sb.append(next.mPreHandoverState);
                                Rlog.i(LOG_TAG, sb.toString());
                                if (next.mPreHandoverState == this.mConnections[i].getState()) {
                                    Rlog.i(LOG_TAG, "Removing HO conn " + hoConnection + next.mPreHandoverState);
                                    it.remove();
                                }
                                arrayList = list2;
                            }
                        }
                        list = arrayList;
                        this.mPhone.notifyHandoverStateChanged(this.mConnections[i]);
                    } else {
                        list = arrayList;
                        connectionCheckMtFindNewRinging = checkMtFindNewRinging(driverCall, i);
                        if (connectionCheckMtFindNewRinging == null) {
                            if (isPhoneTypeGsm()) {
                                arrayList2.add(this.mConnections[i]);
                            } else {
                                gsmCdmaConnection2 = this.mConnections[i];
                            }
                            z6 = true;
                        }
                    }
                } else {
                    this.mConnections[i] = this.mPendingMO;
                    this.mPendingMO.mIndex = i;
                    this.mPendingMO.update(driverCall);
                    this.mPendingMO = null;
                    if (this.mHangupPendingMO) {
                        this.mHangupPendingMO = false;
                        if (!isPhoneTypeGsm() && this.mIsEcmTimerCanceled) {
                            handleEcmTimer(0);
                        }
                        try {
                            log("poll: hangupPendingMO, hangup conn " + i);
                            hangup(this.mConnections[i]);
                        } catch (CallStateException e) {
                            Rlog.e(LOG_TAG, "unexpected error on hangup");
                        }
                        return;
                    }
                    list = arrayList;
                }
                z5 = true;
                i++;
                arrayList = list;
            }
        }
        if (!isPhoneTypeGsm() && z4) {
            checkAndEnableDataCallAfterEmergencyCallDropped();
        }
        if (this.mPendingMO != null) {
            Rlog.d(LOG_TAG, "Pending MO dropped before poll fg state:" + this.mForegroundCall.getState());
            this.mDroppedDuringPoll.add(this.mPendingMO);
            gsmCdmaConnection = null;
            this.mPendingMO = null;
            this.mHangupPendingMO = false;
            if (isPhoneTypeGsm()) {
                z = false;
            } else {
                if (this.mPendingCallInEcm) {
                    z = false;
                    this.mPendingCallInEcm = false;
                } else {
                    z = false;
                }
                checkAndEnableDataCallAfterEmergencyCallDropped();
            }
        } else {
            z = false;
            gsmCdmaConnection = null;
        }
        if (connectionCheckMtFindNewRinging != null) {
            this.mPhone.notifyNewRingingConnection(connectionCheckMtFindNewRinging);
        }
        ArrayList<GsmCdmaConnection> arrayList3 = new ArrayList<>();
        int size5 = this.mDroppedDuringPoll.size() - 1;
        boolean z7 = z;
        while (size5 >= 0) {
            GsmCdmaConnection gsmCdmaConnection4 = this.mDroppedDuringPoll.get(size5);
            if (gsmCdmaConnection4.isIncoming() && gsmCdmaConnection4.getConnectTime() == 0) {
                int i5 = gsmCdmaConnection4.mCause == 3 ? 16 : 1;
                log("missed/rejected call, conn.cause=" + gsmCdmaConnection4.mCause);
                log("setting cause to " + i5);
                this.mDroppedDuringPoll.remove(size5);
                zOnDisconnect = z7 | gsmCdmaConnection4.onDisconnect(i5);
                arrayList3.add(gsmCdmaConnection4);
            } else {
                if (gsmCdmaConnection4.mCause != 3 && gsmCdmaConnection4.mCause != 7) {
                    z2 = z7;
                    z3 = z;
                    if (isPhoneTypeGsm() && z3 && z6 && gsmCdmaConnection4 == gsmCdmaConnection2) {
                        z6 = z;
                        gsmCdmaConnection2 = gsmCdmaConnection;
                    }
                    size5--;
                    z7 = z2;
                }
                this.mDroppedDuringPoll.remove(size5);
                zOnDisconnect = z7 | gsmCdmaConnection4.onDisconnect(gsmCdmaConnection4.mCause);
                arrayList3.add(gsmCdmaConnection4);
            }
            z2 = zOnDisconnect;
            z3 = true;
            if (isPhoneTypeGsm()) {
            }
            size5--;
            z7 = z2;
        }
        if (arrayList3.size() > 0) {
            this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), arrayList3);
        }
        Iterator<Connection> it2 = this.mHandoverConnections.iterator();
        while (it2.hasNext()) {
            Connection next2 = it2.next();
            log("handlePollCalls - disconnect hoConn= " + next2 + " hoConn.State= " + next2.getState());
            if (next2.getState().isRinging()) {
                next2.onDisconnect(1);
            } else {
                next2.onDisconnect(-1);
            }
            it2.remove();
        }
        if (this.mDroppedDuringPoll.size() > 0) {
            this.mCi.getLastCallFailCause(obtainNoPollCompleteMessage(5));
        }
        if (connectionCheckMtFindNewRinging != null || z5 || z7) {
            internalClearDisconnected();
        }
        updatePhoneState();
        if (z6) {
            if (isPhoneTypeGsm()) {
                for (Connection connection : arrayList2) {
                    log("Notify unknown for " + connection);
                    this.mPhone.notifyUnknownConnection(connection);
                }
            } else {
                this.mPhone.notifyUnknownConnection(gsmCdmaConnection2);
            }
        }
        if (z5 || connectionCheckMtFindNewRinging != null || z7) {
            this.mPhone.notifyPreciseCallStateChanged();
            updateMetrics(this.mConnections);
        }
        if (size > 0 && this.mHandoverConnections.size() == 0 && (imsPhone = this.mPhone.getImsPhone()) != null) {
            imsPhone.callEndCleanupHandOverCallIfAny();
        }
    }

    protected void updateMetrics(GsmCdmaConnection[] gsmCdmaConnectionArr) {
        ArrayList<GsmCdmaConnection> arrayList = new ArrayList<>();
        for (GsmCdmaConnection gsmCdmaConnection : gsmCdmaConnectionArr) {
            if (gsmCdmaConnection != null) {
                arrayList.add(gsmCdmaConnection);
            }
        }
        this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), arrayList);
    }

    private void handleRadioNotAvailable() {
        pollCallsWhenSafe();
    }

    protected void dumpState() {
        Rlog.i(LOG_TAG, "Phone State:" + this.mState);
        Rlog.i(LOG_TAG, "Ringing call: " + this.mRingingCall.toString());
        List<Connection> connections = this.mRingingCall.getConnections();
        int size = connections.size();
        for (int i = 0; i < size; i++) {
            Rlog.i(LOG_TAG, connections.get(i).toString());
        }
        Rlog.i(LOG_TAG, "Foreground call: " + this.mForegroundCall.toString());
        List<Connection> connections2 = this.mForegroundCall.getConnections();
        int size2 = connections2.size();
        for (int i2 = 0; i2 < size2; i2++) {
            Rlog.i(LOG_TAG, connections2.get(i2).toString());
        }
        Rlog.i(LOG_TAG, "Background call: " + this.mBackgroundCall.toString());
        List<Connection> connections3 = this.mBackgroundCall.getConnections();
        int size3 = connections3.size();
        for (int i3 = 0; i3 < size3; i3++) {
            Rlog.i(LOG_TAG, connections3.get(i3).toString());
        }
    }

    public void hangup(GsmCdmaConnection gsmCdmaConnection) throws CallStateException {
        if (gsmCdmaConnection.mOwner != this) {
            throw new CallStateException("GsmCdmaConnection " + gsmCdmaConnection + "does not belong to GsmCdmaCallTracker " + this);
        }
        if (gsmCdmaConnection == this.mPendingMO) {
            log("hangup: set hangupPendingMO to true");
            this.mHangupPendingMO = true;
        } else if (!isPhoneTypeGsm() && gsmCdmaConnection.getCall() == this.mRingingCall && this.mRingingCall.getState() == Call.State.WAITING) {
            gsmCdmaConnection.onLocalDisconnect();
            updatePhoneState();
            this.mPhone.notifyPreciseCallStateChanged();
            return;
        } else {
            try {
                this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), gsmCdmaConnection, gsmCdmaConnection.getGsmCdmaIndex());
                this.mCi.hangupConnection(gsmCdmaConnection.getGsmCdmaIndex(), obtainCompleteMessage());
            } catch (CallStateException e) {
                Rlog.w(LOG_TAG, "GsmCdmaCallTracker WARN: hangup() on absent connection " + gsmCdmaConnection);
            }
        }
        gsmCdmaConnection.onHangupLocal();
    }

    public void separate(GsmCdmaConnection gsmCdmaConnection) throws CallStateException {
        if (gsmCdmaConnection.mOwner != this) {
            throw new CallStateException("GsmCdmaConnection " + gsmCdmaConnection + "does not belong to GsmCdmaCallTracker " + this);
        }
        try {
            this.mCi.separateConnection(gsmCdmaConnection.getGsmCdmaIndex(), obtainCompleteMessage(12));
        } catch (CallStateException e) {
            Rlog.w(LOG_TAG, "GsmCdmaCallTracker WARN: separate() on absent connection " + gsmCdmaConnection);
        }
    }

    public void setMute(boolean z) {
        this.mDesiredMute = z;
        this.mCi.setMute(this.mDesiredMute, null);
    }

    public boolean getMute() {
        return this.mDesiredMute;
    }

    public void hangup(GsmCdmaCall gsmCdmaCall) throws CallStateException {
        if (gsmCdmaCall.getConnections().size() == 0) {
            throw new CallStateException("no connections in call");
        }
        if (gsmCdmaCall == this.mRingingCall) {
            log("(ringing) hangup waiting or background");
            logHangupEvent(gsmCdmaCall);
            this.mCi.hangupWaitingOrBackground(obtainCompleteMessage());
        } else if (gsmCdmaCall == this.mForegroundCall) {
            if (gsmCdmaCall.isDialingOrAlerting()) {
                log("(foregnd) hangup dialing or alerting...");
                hangup((GsmCdmaConnection) gsmCdmaCall.getConnections().get(0));
            } else if (isPhoneTypeGsm() && this.mRingingCall.isRinging()) {
                log("hangup all conns in active/background call, without affecting ringing call");
                hangupAllConnections(gsmCdmaCall);
            } else {
                logHangupEvent(gsmCdmaCall);
                hangupForegroundResumeBackground();
            }
        } else if (gsmCdmaCall == this.mBackgroundCall) {
            if (this.mRingingCall.isRinging()) {
                log("hangup all conns in background call");
                hangupAllConnections(gsmCdmaCall);
            } else {
                hangupWaitingOrBackground();
            }
        } else {
            throw new RuntimeException("GsmCdmaCall " + gsmCdmaCall + "does not belong to GsmCdmaCallTracker " + this);
        }
        gsmCdmaCall.onHangupLocal();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    protected void logHangupEvent(GsmCdmaCall gsmCdmaCall) {
        int gsmCdmaIndex;
        int size = gsmCdmaCall.mConnections.size();
        for (int i = 0; i < size; i++) {
            GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection) gsmCdmaCall.mConnections.get(i);
            try {
                gsmCdmaIndex = gsmCdmaConnection.getGsmCdmaIndex();
            } catch (CallStateException e) {
                gsmCdmaIndex = -1;
            }
            this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), gsmCdmaConnection, gsmCdmaIndex);
        }
    }

    public void hangupWaitingOrBackground() {
        log("hangupWaitingOrBackground");
        logHangupEvent(this.mBackgroundCall);
        this.mCi.hangupWaitingOrBackground(obtainCompleteMessage());
    }

    public void hangupForegroundResumeBackground() {
        log("hangupForegroundResumeBackground");
        this.mCi.hangupForegroundResumeBackground(obtainCompleteMessage());
    }

    public void hangupConnectionByIndex(GsmCdmaCall gsmCdmaCall, int i) throws CallStateException {
        int size = gsmCdmaCall.mConnections.size();
        for (int i2 = 0; i2 < size; i2++) {
            GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection) gsmCdmaCall.mConnections.get(i2);
            if (!gsmCdmaConnection.mDisconnected && gsmCdmaConnection.getGsmCdmaIndex() == i) {
                this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), gsmCdmaConnection, gsmCdmaConnection.getGsmCdmaIndex());
                this.mCi.hangupConnection(i, obtainCompleteMessage());
                return;
            }
        }
        throw new CallStateException("no GsmCdma index found");
    }

    public void hangupAllConnections(GsmCdmaCall gsmCdmaCall) {
        try {
            int size = gsmCdmaCall.mConnections.size();
            for (int i = 0; i < size; i++) {
                GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection) gsmCdmaCall.mConnections.get(i);
                if (!gsmCdmaConnection.mDisconnected) {
                    this.mMetrics.writeRilHangup(this.mPhone.getPhoneId(), gsmCdmaConnection, gsmCdmaConnection.getGsmCdmaIndex());
                    this.mCi.hangupConnection(gsmCdmaConnection.getGsmCdmaIndex(), obtainCompleteMessage());
                }
            }
        } catch (CallStateException e) {
            Rlog.e(LOG_TAG, "hangupConnectionByIndex caught " + e);
        }
    }

    public GsmCdmaConnection getConnectionByIndex(GsmCdmaCall gsmCdmaCall, int i) throws CallStateException {
        int size = gsmCdmaCall.mConnections.size();
        for (int i2 = 0; i2 < size; i2++) {
            GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection) gsmCdmaCall.mConnections.get(i2);
            if (!gsmCdmaConnection.mDisconnected && gsmCdmaConnection.getGsmCdmaIndex() == i) {
                return gsmCdmaConnection;
            }
        }
        return null;
    }

    protected void notifyCallWaitingInfo(CdmaCallWaitingNotification cdmaCallWaitingNotification) {
        if (this.mCallWaitingRegistrants != null) {
            this.mCallWaitingRegistrants.notifyRegistrants(new AsyncResult((Object) null, cdmaCallWaitingNotification, (Throwable) null));
        }
    }

    protected void handleCallWaitingInfo(CdmaCallWaitingNotification cdmaCallWaitingNotification) {
        new GsmCdmaConnection(this.mPhone.getContext(), cdmaCallWaitingNotification, this, this.mRingingCall);
        updatePhoneState();
        notifyCallWaitingInfo(cdmaCallWaitingNotification);
    }

    protected PhoneInternalInterface.SuppService getFailedService(int i) {
        if (i == 8) {
            return PhoneInternalInterface.SuppService.SWITCH;
        }
        switch (i) {
            case 11:
                return PhoneInternalInterface.SuppService.CONFERENCE;
            case 12:
                return PhoneInternalInterface.SuppService.SEPARATE;
            case 13:
                return PhoneInternalInterface.SuppService.TRANSFER;
            default:
                return PhoneInternalInterface.SuppService.UNKNOWN;
        }
    }

    @Override
    public void handleMessage(Message message) {
        int i;
        Connection latestConnection;
        int i2 = message.what;
        String str = null;
        if (i2 != 20) {
            switch (i2) {
                case 1:
                    Rlog.d(LOG_TAG, "Event EVENT_POLL_CALLS_RESULT Received");
                    if (message == this.mLastRelevantPoll) {
                        this.mNeedsPoll = false;
                        this.mLastRelevantPoll = null;
                        handlePollCalls((AsyncResult) message.obj);
                        return;
                    }
                    return;
                case 2:
                case 3:
                    pollCallsWhenSafe();
                    return;
                case 4:
                    operationComplete();
                    return;
                case 5:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    operationComplete();
                    if (asyncResult.exception != null) {
                        i = 16;
                        Rlog.i(LOG_TAG, "Exception during getLastCallFailCause, assuming normal disconnect");
                    } else {
                        LastCallFailCause lastCallFailCause = (LastCallFailCause) asyncResult.result;
                        int i3 = lastCallFailCause.causeCode;
                        str = lastCallFailCause.vendorCause;
                        i = i3;
                    }
                    if (i == 34 || i == 41 || i == 42 || i == 44 || i == 49 || i == 58 || i == 65535) {
                        CellLocation cellLocation = this.mPhone.getCellLocation();
                        int baseStationId = -1;
                        if (cellLocation != null) {
                            if (isPhoneTypeGsm()) {
                                baseStationId = ((GsmCellLocation) cellLocation).getCid();
                            } else {
                                baseStationId = ((CdmaCellLocation) cellLocation).getBaseStationId();
                            }
                        }
                        EventLog.writeEvent(EventLogTags.CALL_DROP, Integer.valueOf(i), Integer.valueOf(baseStationId), Integer.valueOf(TelephonyManager.getDefault().getNetworkType()));
                    }
                    int size = this.mDroppedDuringPoll.size();
                    for (int i4 = 0; i4 < size; i4++) {
                        this.mDroppedDuringPoll.get(i4).onRemoteDisconnect(i, str);
                    }
                    updatePhoneState();
                    this.mPhone.notifyPreciseCallStateChanged();
                    this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), this.mDroppedDuringPoll);
                    this.mDroppedDuringPoll.clear();
                    return;
                default:
                    switch (i2) {
                        case 8:
                        case 12:
                        case 13:
                            break;
                        case 9:
                            handleRadioAvailable();
                            return;
                        case 10:
                            handleRadioNotAvailable();
                            return;
                        case 11:
                            if (isPhoneTypeGsm() && ((AsyncResult) message.obj).exception != null && (latestConnection = this.mForegroundCall.getLatestConnection()) != null) {
                                latestConnection.onConferenceMergeFailed();
                            }
                            break;
                        case 14:
                            if (!isPhoneTypeGsm()) {
                                if (this.mPendingCallInEcm) {
                                    this.mCi.dial(this.mPendingMO.getAddress(), this.mPendingCallClirMode, obtainCompleteMessage());
                                    this.mPendingCallInEcm = false;
                                }
                                this.mPhone.unsetOnEcbModeExitResponse(this);
                                return;
                            }
                            throw new RuntimeException("unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
                        case 15:
                            if (!isPhoneTypeGsm()) {
                                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                                if (asyncResult2.exception == null) {
                                    handleCallWaitingInfo((CdmaCallWaitingNotification) asyncResult2.result);
                                    Rlog.d(LOG_TAG, "Event EVENT_CALL_WAITING_INFO_CDMA Received");
                                    return;
                                }
                                return;
                            }
                            throw new RuntimeException("unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
                        case 16:
                            if (!isPhoneTypeGsm()) {
                                if (((AsyncResult) message.obj).exception == null) {
                                    this.mPendingMO.onConnectedInOrOut();
                                    this.mPendingMO = null;
                                    return;
                                }
                                return;
                            }
                            throw new RuntimeException("unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
                        default:
                            throw new RuntimeException("unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
                    }
                    if (isPhoneTypeGsm()) {
                        if (((AsyncResult) message.obj).exception != null) {
                            this.mPhone.notifySuppServiceFailed(getFailedService(message.what));
                        }
                        operationComplete();
                        return;
                    } else {
                        if (message.what != 8) {
                            throw new RuntimeException("unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
                        }
                        return;
                    }
            }
        }
        if (!isPhoneTypeGsm()) {
            if (((AsyncResult) message.obj).exception == null) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (GsmCdmaCallTracker.this.mPendingMO != null) {
                            GsmCdmaCallTracker.this.mCi.sendCDMAFeatureCode(GsmCdmaCallTracker.this.mPendingMO.getAddress(), GsmCdmaCallTracker.this.obtainMessage(16));
                        }
                    }
                }, this.m3WayCallFlashDelay);
                return;
            } else {
                this.mPendingMO = null;
                Rlog.w(LOG_TAG, "exception happened on Blank Flash for 3-way call");
                return;
            }
        }
        throw new RuntimeException("unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
    }

    protected void checkAndEnableDataCallAfterEmergencyCallDropped() {
        if (this.mIsInEmergencyCall) {
            this.mIsInEmergencyCall = false;
            boolean zIsInEcm = this.mPhone.isInEcm();
            log("checkAndEnableDataCallAfterEmergencyCallDropped,inEcm=" + zIsInEcm);
            if (!zIsInEcm) {
                this.mPhone.mDcTracker.setInternalDataEnabled(true);
                this.mPhone.notifyEmergencyCallRegistrants(false);
            }
            this.mPhone.sendEmergencyCallStateChange(false);
        }
    }

    protected Connection checkMtFindNewRinging(DriverCall driverCall, int i) {
        if (this.mConnections[i].getCall() == this.mRingingCall) {
            GsmCdmaConnection gsmCdmaConnection = this.mConnections[i];
            log("Notify new ring " + driverCall);
            return gsmCdmaConnection;
        }
        Rlog.e(LOG_TAG, "Phantom call appeared " + driverCall);
        if (driverCall.state != DriverCall.State.ALERTING && driverCall.state != DriverCall.State.DIALING) {
            this.mConnections[i].onConnectedInOrOut();
            if (driverCall.state == DriverCall.State.HOLDING) {
                this.mConnections[i].onStartedHolding();
            }
        }
        return null;
    }

    public boolean isInEmergencyCall() {
        return this.mIsInEmergencyCall;
    }

    protected boolean isPhoneTypeGsm() {
        return this.mPhone.getPhoneType() == 1;
    }

    public GsmCdmaPhone getPhone() {
        return this.mPhone;
    }

    @Override
    protected void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "] " + str);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("GsmCdmaCallTracker extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mConnections: length=" + this.mConnections.length);
        for (int i = 0; i < this.mConnections.length; i++) {
            printWriter.printf("  mConnections[%d]=%s\n", Integer.valueOf(i), this.mConnections[i]);
        }
        printWriter.println(" mVoiceCallEndedRegistrants=" + this.mVoiceCallEndedRegistrants);
        printWriter.println(" mVoiceCallStartedRegistrants=" + this.mVoiceCallStartedRegistrants);
        if (!isPhoneTypeGsm()) {
            printWriter.println(" mCallWaitingRegistrants=" + this.mCallWaitingRegistrants);
        }
        printWriter.println(" mDroppedDuringPoll: size=" + this.mDroppedDuringPoll.size());
        for (int i2 = 0; i2 < this.mDroppedDuringPoll.size(); i2++) {
            printWriter.printf("  mDroppedDuringPoll[%d]=%s\n", Integer.valueOf(i2), this.mDroppedDuringPoll.get(i2));
        }
        printWriter.println(" mRingingCall=" + this.mRingingCall);
        printWriter.println(" mForegroundCall=" + this.mForegroundCall);
        printWriter.println(" mBackgroundCall=" + this.mBackgroundCall);
        printWriter.println(" mPendingMO=" + this.mPendingMO);
        printWriter.println(" mHangupPendingMO=" + this.mHangupPendingMO);
        printWriter.println(" mPhone=" + this.mPhone);
        printWriter.println(" mDesiredMute=" + this.mDesiredMute);
        printWriter.println(" mState=" + this.mState);
        if (!isPhoneTypeGsm()) {
            printWriter.println(" mPendingCallInEcm=" + this.mPendingCallInEcm);
            printWriter.println(" mIsInEmergencyCall=" + this.mIsInEmergencyCall);
            printWriter.println(" mPendingCallClirMode=" + this.mPendingCallClirMode);
            printWriter.println(" mIsEcmTimerCanceled=" + this.mIsEcmTimerCanceled);
        }
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mState;
    }

    public int getMaxConnectionsPerCall() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return 5;
        }
        return 1;
    }

    @Override
    public void cleanupCalls() {
        pollCallsWhenSafe();
    }
}
