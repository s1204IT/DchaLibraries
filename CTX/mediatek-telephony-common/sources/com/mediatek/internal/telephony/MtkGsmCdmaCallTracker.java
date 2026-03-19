package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.GsmCdmaCall;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaConnection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneConnection;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MtkGsmCdmaCallTracker extends GsmCdmaCallTracker {
    protected static final int EVENT_CDMA_CALL_ACCEPTED = 1004;
    protected static final int EVENT_DIAL_CALL_RESULT = 1002;
    protected static final int EVENT_ECONF_SRVCC_INDICATION = 1005;
    protected static final int EVENT_HANG_UP_RESULT = 1003;
    protected static final int EVENT_INCOMING_CALL_INDICATION = 1000;
    protected static final int EVENT_MTK_BASE = 1000;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 1001;
    protected static final int EVENT_RADIO_ON = 1006;
    private static final String PROP_LOG_TAG = "GsmCdmaCallTkr";
    private boolean bAllCallsDisconnectedButNotHandled;
    private int[] mEconfSrvccConnectionIds;
    private boolean mHasPendingCheckAndEnableData;
    boolean mHasPendingSwapRequest;
    private boolean mHasPendingUpdatePhoneType;
    MtkGsmCdmaCallTrackerHelper mHelper;
    protected Connection mImsConfHostConnection;
    private ArrayList<Connection> mImsConfParticipants;
    private final BroadcastReceiver mIntentReceiver;
    public MtkRIL mMtkCi;
    protected IMtkGsmCdmaCallTrackerExt mMtkGsmCdmaCallTrackerExt;
    protected boolean mNeedWaitImsEConfSrvcc;
    private int mPhoneType;
    TelephonyDevController mTelDevController;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    WaitForHoldToHangup mWaitForHoldToHangupRequest;
    WaitForHoldToRedial mWaitForHoldToRedialRequest;

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasC2kOverImsModem()) ? false : true;
    }

    public int getMaxConnections() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return 19;
        }
        return 8;
    }

    class WaitForHoldToRedial {
        private boolean mWaitToRedial = false;
        private String mDialString = null;
        private int mClirMode = 0;
        private UUSInfo mUUSInfo = null;

        WaitForHoldToRedial() {
            resetToRedial();
        }

        boolean isWaitToRedial() {
            return this.mWaitToRedial;
        }

        void setToRedial() {
            this.mWaitToRedial = true;
        }

        public void setToRedial(String str, int i, UUSInfo uUSInfo) {
            this.mWaitToRedial = true;
            this.mDialString = str;
            this.mClirMode = i;
            this.mUUSInfo = uUSInfo;
        }

        public void resetToRedial() {
            MtkGsmCdmaCallTracker.this.proprietaryLog("Reset mWaitForHoldToRedialRequest variables");
            this.mWaitToRedial = false;
            this.mDialString = null;
            this.mClirMode = 0;
            this.mUUSInfo = null;
        }

        private boolean resumeDialAfterHold() {
            boolean zIsEmergencyNumber;
            MtkGsmCdmaCallTracker.this.proprietaryLog("resumeDialAfterHold begin");
            if (MtkGsmCdmaCallTracker.this.hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(MtkGsmCdmaCallTracker.this.mPhone.getPhoneId()) || MtkGsmCdmaCallTracker.this.mPhone.getServiceState().getState() != 0)) {
                zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(this.mDialString);
            } else {
                zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(MtkGsmCdmaCallTracker.this.mPhone.getSubId(), this.mDialString);
            }
            if (this.mWaitToRedial) {
                if (zIsEmergencyNumber && !MtkPhoneNumberUtils.isSpecialEmergencyNumber(MtkGsmCdmaCallTracker.this.mPhone.getSubId(), this.mDialString)) {
                    MtkGsmCdmaCallTracker.this.mMtkCi.setEccServiceCategory(MtkPhoneNumberUtils.getServiceCategoryFromEccBySubId(this.mDialString, MtkGsmCdmaCallTracker.this.mPhone.getSubId()), null);
                    MtkGsmCdmaCallTracker.this.mMtkCi.emergencyDial(this.mDialString, this.mClirMode, this.mUUSInfo, MtkGsmCdmaCallTracker.this.obtainCompleteMessage(1002));
                } else {
                    MtkGsmCdmaCallTracker.this.mCi.dial(this.mDialString, this.mClirMode, this.mUUSInfo, MtkGsmCdmaCallTracker.this.obtainCompleteMessage(1002));
                }
                resetToRedial();
                MtkGsmCdmaCallTracker.this.proprietaryLog("resumeDialAfterHold end");
                return true;
            }
            return false;
        }
    }

    class WaitForHoldToHangup {
        private boolean mWaitToHangup = false;
        private boolean mHoldDone = false;
        private GsmCdmaCall mCall = null;

        WaitForHoldToHangup() {
            resetToHangup();
        }

        boolean isWaitToHangup() {
            return this.mWaitToHangup;
        }

        boolean isHoldDone() {
            return this.mHoldDone;
        }

        void setHoldDone() {
            this.mHoldDone = true;
        }

        void setToHangup() {
            this.mWaitToHangup = true;
        }

        public void setToHangup(GsmCdmaCall gsmCdmaCall) {
            this.mWaitToHangup = true;
            this.mCall = gsmCdmaCall;
        }

        public void resetToHangup() {
            MtkGsmCdmaCallTracker.this.proprietaryLog("Reset mWaitForHoldToHangupRequest variables");
            this.mWaitToHangup = false;
            this.mHoldDone = false;
            this.mCall = null;
        }

        private boolean resumeHangupAfterHold() {
            MtkGsmCdmaCallTracker.this.proprietaryLog("resumeHangupAfterHold begin");
            if (this.mWaitToHangup && this.mCall != null) {
                MtkGsmCdmaCallTracker.this.proprietaryLog("resumeHangupAfterHold to hangup call");
                this.mWaitToHangup = false;
                this.mHoldDone = false;
                try {
                    MtkGsmCdmaCallTracker.this.hangup(this.mCall);
                } catch (CallStateException e) {
                    e.printStackTrace();
                    Rlog.e(MtkGsmCdmaCallTracker.PROP_LOG_TAG, "unexpected error on hangup (" + e.getMessage() + ")");
                }
                MtkGsmCdmaCallTracker.this.proprietaryLog("resumeHangupAfterHold end");
                this.mCall = null;
                return true;
            }
            resetToHangup();
            return false;
        }
    }

    public MtkGsmCdmaCallTracker(GsmCdmaPhone gsmCdmaPhone) {
        super(gsmCdmaPhone);
        this.mMtkCi = null;
        this.mTelephonyCustomizationFactory = null;
        this.mMtkGsmCdmaCallTrackerExt = null;
        this.mHasPendingSwapRequest = false;
        this.mWaitForHoldToRedialRequest = new WaitForHoldToRedial();
        this.mWaitForHoldToHangupRequest = new WaitForHoldToHangup();
        this.bAllCallsDisconnectedButNotHandled = false;
        this.mPhoneType = 0;
        this.mHasPendingUpdatePhoneType = false;
        this.mHasPendingCheckAndEnableData = false;
        this.mTelDevController = TelephonyDevController.getInstance();
        this.mNeedWaitImsEConfSrvcc = false;
        this.mImsConfHostConnection = null;
        this.mImsConfParticipants = new ArrayList<>();
        this.mEconfSrvccConnectionIds = null;
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.telephony.action.SIM_CARD_STATE_CHANGED")) {
                    int intExtra = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                    int intExtra2 = intent.getIntExtra("slot", 0);
                    if (11 == intExtra && intExtra2 == MtkGsmCdmaCallTracker.this.mPhone.getPhoneId()) {
                        Rlog.d("GsmCdmaCallTracker", "Set ECC list to MD when SIM ready, slot: " + intExtra2);
                        MtkGsmCdmaCallTracker.this.mMtkCi.setEccList();
                    }
                }
            }
        };
        this.mRingingCall = new MtkGsmCdmaCall(this);
        this.mForegroundCall = new MtkGsmCdmaCall(this);
        this.mBackgroundCall = new MtkGsmCdmaCall(this);
        this.mMtkCi = this.mCi;
        this.mMtkCi.setOnIncomingCallIndication(this, 1000, null);
        this.mMtkCi.registerForOffOrNotAvailable(this, 1001, null);
        this.mHelper = new MtkGsmCdmaCallTrackerHelper(gsmCdmaPhone.getContext(), this);
        this.mMtkCi.registerForEconfSrvcc(this, 1005, null);
        this.mMtkCi.registerForOn(this, 1006, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.SIM_CARD_STATE_CHANGED");
        gsmCdmaPhone.getContext().registerReceiver(this.mIntentReceiver, intentFilter);
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext());
            this.mMtkGsmCdmaCallTrackerExt = this.mTelephonyCustomizationFactory.makeMtkGsmCdmaCallTrackerExt(gsmCdmaPhone.getContext());
        } catch (Exception e) {
            Rlog.d("GsmCdmaCallTracker", "mMtkGsmCdmaCallTrackerExt init fail");
            e.printStackTrace();
        }
    }

    protected void updatePhoneType(boolean z) {
        if (this.mPhoneType == 2 && !this.mPhone.isPhoneTypeGsm()) {
            return;
        }
        if (!z) {
            if (this.mLastRelevantPoll != null) {
                this.mHasPendingUpdatePhoneType = true;
                Rlog.d("GsmCdmaCallTracker", "[updatePhoneType]mHasPendingUpdatePhoneType = true");
                if (this.mPhoneType == 2 && this.mPhone.isPhoneTypeGsm()) {
                    this.mHasPendingCheckAndEnableData = true;
                    return;
                }
                return;
            }
            reset();
            if (hasC2kOverImsModem() || MtkTelephonyManagerEx.getDefault().useVzwLogic()) {
                Phone imsPhone = this.mPhone.getImsPhone();
                if (imsPhone == null || (imsPhone != null && imsPhone.getHandoverConnection() == null)) {
                    pollCallsWhenSafe();
                } else {
                    Rlog.d("GsmCdmaCallTracker", "not trigger pollCall since imsCall exists");
                }
            }
        }
        super.updatePhoneType(true);
        if (this.mPhone.isPhoneTypeGsm()) {
            if (this.mMtkCi == null) {
                this.mMtkCi = this.mCi;
            }
            this.mMtkCi.unregisterForCdmaCallAccepted(this);
            this.mPhoneType = 1;
            return;
        }
        if (this.mMtkCi == null) {
            this.mMtkCi = this.mCi;
        }
        this.mMtkCi.unregisterForCdmaCallAccepted(this);
        this.mMtkCi.registerForCdmaCallAccepted(this, 1004, null);
        this.mPhoneType = 2;
    }

    protected void reset() {
        if (!hasC2kOverImsModem() && !MtkTelephonyManagerEx.getDefault().useVzwLogic()) {
            super.reset();
            return;
        }
        Rlog.d("GsmCdmaCallTracker", "reset");
        for (GsmCdmaConnection gsmCdmaConnection : this.mConnections) {
            if (gsmCdmaConnection != null) {
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

    protected synchronized void handlePollCalls(AsyncResult asyncResult) {
        List arrayList;
        Phone imsPhone;
        boolean zOnDisconnect;
        boolean z;
        boolean z2;
        int i;
        GsmCdmaConnection gsmCdmaConnection;
        DriverCall driverCall;
        if (asyncResult.exception == null) {
            arrayList = (List) asyncResult.result;
        } else {
            if (!isCommandExceptionRadioNotAvailable(asyncResult.exception)) {
                if (!this.mNeedWaitImsEConfSrvcc || hasParsingCEPCapability()) {
                    pollCallsAfterDelay();
                    return;
                } else {
                    proprietaryLog("SRVCC: +ECONFSRVCC is still not arrival, skip this poll call.");
                    return;
                }
            }
            arrayList = new ArrayList();
        }
        ArrayList<Connection> arrayList2 = new ArrayList();
        int size = this.mHandoverConnections.size();
        int size2 = arrayList.size();
        int i2 = 0;
        int i3 = 0;
        boolean z3 = true;
        boolean z4 = false;
        Connection connectionCheckMtFindNewRinging = null;
        boolean z5 = false;
        GsmCdmaConnection gsmCdmaConnection2 = null;
        boolean z6 = false;
        while (i2 < this.mConnections.length) {
            GsmCdmaConnection gsmCdmaConnection3 = this.mConnections[i2];
            if (i3 < size2) {
                driverCall = (DriverCall) arrayList.get(i3);
                if (isPhoneTypeGsm()) {
                    i = size2;
                    gsmCdmaConnection = gsmCdmaConnection2;
                } else {
                    i = size2;
                    gsmCdmaConnection = gsmCdmaConnection2;
                    driverCall.number = processPlusCodeForDriverCall(driverCall.number, driverCall.isMT, driverCall.TOA);
                }
                if (driverCall.index == i2 + 1) {
                    i3++;
                    if (gsmCdmaConnection3 == null || driverCall != null) {
                        z3 = false;
                    }
                    if (gsmCdmaConnection3 == null || driverCall == null) {
                        if (gsmCdmaConnection3 == null && driverCall == null) {
                            if (!isPhoneTypeGsm() || this.mPhoneType == 2) {
                                int size3 = this.mForegroundCall.mConnections.size();
                                for (int i4 = 0; i4 < size3; i4++) {
                                    log("adding fgCall cn " + i4 + " to droppedDuringPoll");
                                    this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mForegroundCall.mConnections.get(i4));
                                }
                                int size4 = this.mRingingCall.mConnections.size();
                                for (int i5 = 0; i5 < size4; i5++) {
                                    log("adding rgCall cn " + i5 + " to droppedDuringPoll");
                                    this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mRingingCall.mConnections.get(i5));
                                }
                                if (this.mIsEcmTimerCanceled) {
                                    handleEcmTimer(0);
                                }
                                checkAndEnableDataCallAfterEmergencyCallDropped();
                            } else {
                                if (((gsmCdmaConnection3.getCall() == this.mForegroundCall && this.mForegroundCall.mConnections.size() == 1 && this.mBackgroundCall.isIdle()) || (gsmCdmaConnection3.getCall() == this.mBackgroundCall && this.mBackgroundCall.mConnections.size() == 1 && this.mForegroundCall.isIdle())) && this.mRingingCall.getState() == Call.State.WAITING) {
                                    this.mRingingCall.mState = Call.State.INCOMING;
                                }
                                this.mDroppedDuringPoll.add(gsmCdmaConnection3);
                                if (this.mIsEcmTimerCanceled) {
                                    handleEcmTimer(0);
                                }
                                this.mConnections[i2] = null;
                                this.mHelper.CallIndicationEnd();
                                this.mHelper.clearForwardingAddressVariables(i2);
                            }
                            this.mConnections[i2] = null;
                        } else if (gsmCdmaConnection3 != null || driverCall == null || gsmCdmaConnection3.compareTo(driverCall) || !isPhoneTypeGsm()) {
                            if (gsmCdmaConnection3 == null && driverCall != null) {
                                if (isPhoneTypeGsm() || gsmCdmaConnection3.isIncoming() == driverCall.isMT) {
                                    boolean z7 = (gsmCdmaConnection3 instanceof MtkGsmCdmaConnection) && ((MtkGsmCdmaConnection) gsmCdmaConnection3).isRealConnected();
                                    boolean z8 = z4 || gsmCdmaConnection3.update(driverCall);
                                    if (isPhoneTypeGsm() || z7 || !(gsmCdmaConnection3 instanceof MtkGsmCdmaConnection) || !((MtkGsmCdmaConnection) gsmCdmaConnection3).isRealConnected()) {
                                        z4 = z8;
                                    } else {
                                        z4 = z8;
                                        gsmCdmaConnection2 = gsmCdmaConnection;
                                        z5 = true;
                                    }
                                } else if (driverCall.isMT) {
                                    this.mConnections[i2] = new MtkGsmCdmaConnection(this.mPhone, driverCall, this, i2);
                                    this.mDroppedDuringPoll.add(gsmCdmaConnection3);
                                    Connection connectionCheckMtFindNewRinging2 = checkMtFindNewRinging(driverCall, i2);
                                    if (connectionCheckMtFindNewRinging2 == null) {
                                        gsmCdmaConnection = gsmCdmaConnection3;
                                        z6 = true;
                                    }
                                    checkAndEnableDataCallAfterEmergencyCallDropped();
                                    connectionCheckMtFindNewRinging = connectionCheckMtFindNewRinging2;
                                } else {
                                    Rlog.e("GsmCdmaCallTracker", "Error in RIL, Phantom call appeared " + driverCall);
                                }
                            }
                            i2++;
                            size2 = i;
                        } else {
                            this.mDroppedDuringPoll.add(gsmCdmaConnection3);
                            if (this.mPendingMO == null || !this.mPendingMO.compareTo(driverCall)) {
                                this.mConnections[i2] = new MtkGsmCdmaConnection(this.mPhone, driverCall, this, i2);
                            } else {
                                Rlog.d(PROP_LOG_TAG, "ringing disc not updated yet & replaced by pendingMo");
                                this.mConnections[i2] = this.mPendingMO;
                                this.mPendingMO.mIndex = i2;
                                this.mPendingMO.update(driverCall);
                                this.mPendingMO = null;
                            }
                            if (this.mConnections[i2].getCall() == this.mRingingCall) {
                                connectionCheckMtFindNewRinging = this.mConnections[i2];
                            }
                        }
                        i2++;
                        size2 = i;
                    } else if (this.mPendingMO == null || !this.mPendingMO.compareTo(driverCall)) {
                        log("pendingMo=" + this.mPendingMO + ", dc=" + driverCall);
                        if (this.mPendingMO != null && !this.mPendingMO.compareTo(driverCall)) {
                            proprietaryLog("MO/MT conflict! MO should be hangup by MD");
                        }
                        this.mConnections[i2] = new MtkGsmCdmaConnection(this.mPhone, driverCall, this, i2);
                        if (isPhoneTypeGsm()) {
                            this.mHelper.setForwardingAddressToConnection(i2, this.mConnections[i2]);
                        }
                        MtkImsPhoneConnection hoConnection = getHoConnection(driverCall);
                        if (hoConnection == null) {
                            connectionCheckMtFindNewRinging = checkMtFindNewRinging(driverCall, i2);
                            if (connectionCheckMtFindNewRinging == null) {
                                if (isPhoneTypeGsm()) {
                                    arrayList2.add(this.mConnections[i2]);
                                } else {
                                    gsmCdmaConnection = this.mConnections[i2];
                                }
                                z6 = true;
                            }
                        } else if ((hoConnection instanceof MtkImsPhoneConnection) && hoConnection.isMultipartyBeforeHandover() && hoConnection.isConfHostBeforeHandover() && !hasParsingCEPCapability()) {
                            Rlog.i("GsmCdmaCallTracker", "SRVCC: goes to conference case.");
                            this.mConnections[i2].mOrigConnection = hoConnection;
                            this.mImsConfParticipants.add(this.mConnections[i2]);
                        } else {
                            Rlog.i("GsmCdmaCallTracker", "SRVCC: goes to normal call case.");
                            this.mConnections[i2].migrateFrom(hoConnection);
                            if (((Connection) hoConnection).mPreHandoverState != Call.State.ACTIVE && ((Connection) hoConnection).mPreHandoverState != Call.State.HOLDING && driverCall.state == DriverCall.State.ACTIVE) {
                                this.mConnections[i2].onConnectedInOrOut();
                            }
                            this.mHandoverConnections.remove(hoConnection);
                            if (isPhoneTypeGsm()) {
                                Iterator it = this.mHandoverConnections.iterator();
                                while (it.hasNext()) {
                                    Connection connection = (Connection) it.next();
                                    Rlog.i("GsmCdmaCallTracker", "HO Conn state is " + connection.mPreHandoverState);
                                    if (connection.mPreHandoverState == this.mConnections[i2].getState()) {
                                        Rlog.i("GsmCdmaCallTracker", "Removing HO conn " + hoConnection + connection.mPreHandoverState);
                                        it.remove();
                                    }
                                }
                            }
                            if (this.mIsInEmergencyCall && !this.mIsEcmTimerCanceled && this.mPhone.isInEcm()) {
                                Rlog.i("GsmCdmaCallTracker", "Ecm timer has been canceled in IMS, so set mIsEcmTimerCanceled=true directly");
                                this.mIsEcmTimerCanceled = true;
                            }
                            this.mPhone.notifyHandoverStateChanged(this.mConnections[i2]);
                            this.mConnections[i2].onConnectionEvent("android.telecom.event.CALL_REMOTELY_UNHELD", (Bundle) null);
                        }
                    } else {
                        this.mConnections[i2] = this.mPendingMO;
                        this.mPendingMO.mIndex = i2;
                        this.mPendingMO.update(driverCall);
                        this.mPendingMO = null;
                        if (!isPhoneTypeGsm() && (this.mConnections[i2] instanceof MtkGsmCdmaConnection) && ((MtkGsmCdmaConnection) this.mConnections[i2]).isRealConnected()) {
                            z5 = true;
                        }
                        if (this.mHangupPendingMO) {
                            this.mHangupPendingMO = false;
                            if (this.mIsEcmTimerCanceled) {
                                handleEcmTimer(0);
                            }
                            try {
                                log("poll: hangupPendingMO, hangup conn " + i2);
                                hangup(this.mConnections[i2]);
                            } catch (CallStateException e) {
                                Rlog.e("GsmCdmaCallTracker", "unexpected error on hangup");
                            }
                            return;
                        }
                    }
                    gsmCdmaConnection2 = gsmCdmaConnection;
                    z4 = true;
                    i2++;
                    size2 = i;
                }
            } else {
                i = size2;
                gsmCdmaConnection = gsmCdmaConnection2;
            }
            driverCall = null;
            if (gsmCdmaConnection3 == null) {
                z3 = false;
                if (gsmCdmaConnection3 == null) {
                    if (gsmCdmaConnection3 == null) {
                        if (gsmCdmaConnection3 != null) {
                        }
                        gsmCdmaConnection2 = gsmCdmaConnection3 == null ? gsmCdmaConnection : gsmCdmaConnection;
                    }
                }
                i2++;
                size2 = i;
            }
        }
        GsmCdmaConnection gsmCdmaConnection4 = gsmCdmaConnection2;
        if (!isPhoneTypeGsm() && z3) {
            checkAndEnableDataCallAfterEmergencyCallDropped();
        }
        if (this.mPendingMO != null) {
            Rlog.d("GsmCdmaCallTracker", "Pending MO dropped before poll fg state:" + this.mForegroundCall.getState());
            this.mDroppedDuringPoll.add(this.mPendingMO);
            this.mPendingMO = null;
            this.mHangupPendingMO = false;
            if (this.mPendingCallInEcm) {
                this.mPendingCallInEcm = false;
            }
            if (this.mIsEcmTimerCanceled) {
                handleEcmTimer(0);
            }
            if (!isPhoneTypeGsm()) {
                checkAndEnableDataCallAfterEmergencyCallDropped();
            }
        }
        if (arrayList.size() == 0 && this.mConnections.length == 0 && !isPhoneTypeGsm()) {
            int size5 = this.mForegroundCall.mConnections.size();
            for (int i6 = 0; i6 < size5; i6++) {
                log("adding fgCall cn " + i6 + " to droppedDuringPoll");
                this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mForegroundCall.mConnections.get(i6));
            }
            int size6 = this.mRingingCall.mConnections.size();
            for (int i7 = 0; i7 < size6; i7++) {
                log("adding rgCall cn " + i7 + " to droppedDuringPoll");
                this.mDroppedDuringPoll.add((GsmCdmaConnection) this.mRingingCall.mConnections.get(i7));
            }
        }
        if (connectionCheckMtFindNewRinging != null) {
            this.mPhone.notifyNewRingingConnection(connectionCheckMtFindNewRinging);
        }
        ArrayList arrayList3 = new ArrayList();
        int size7 = this.mDroppedDuringPoll.size() - 1;
        GsmCdmaConnection gsmCdmaConnection5 = gsmCdmaConnection4;
        boolean z9 = false;
        while (size7 >= 0) {
            GsmCdmaConnection gsmCdmaConnection6 = (GsmCdmaConnection) this.mDroppedDuringPoll.get(size7);
            if (isCommandExceptionRadioNotAvailable(asyncResult.exception)) {
                this.mDroppedDuringPoll.remove(size7);
                zOnDisconnect = z9 | gsmCdmaConnection6.onDisconnect(14);
            } else if (gsmCdmaConnection6.isIncoming() && gsmCdmaConnection6.getConnectTime() == 0) {
                int i8 = gsmCdmaConnection6.mCause == 3 ? 16 : 1;
                log("missed/rejected call, conn.cause=" + gsmCdmaConnection6.mCause);
                log("setting cause to " + i8);
                this.mDroppedDuringPoll.remove(size7);
                zOnDisconnect = z9 | gsmCdmaConnection6.onDisconnect(i8);
                arrayList3.add(gsmCdmaConnection6);
            } else {
                if (gsmCdmaConnection6.mCause != 3 && gsmCdmaConnection6.mCause != 7) {
                    z = z9;
                    z2 = false;
                    if (isPhoneTypeGsm() && z2 && z6 && gsmCdmaConnection6 == gsmCdmaConnection5) {
                        gsmCdmaConnection5 = null;
                        z6 = false;
                    }
                    size7--;
                    z9 = z;
                }
                this.mDroppedDuringPoll.remove(size7);
                zOnDisconnect = z9 | gsmCdmaConnection6.onDisconnect(gsmCdmaConnection6.mCause);
                arrayList3.add(gsmCdmaConnection6);
            }
            z = zOnDisconnect;
            z2 = true;
            if (isPhoneTypeGsm()) {
            }
            size7--;
            z9 = z;
        }
        if (arrayList3.size() > 0) {
            this.mMetrics.writeRilCallList(this.mPhone.getPhoneId(), arrayList3);
        }
        if (this.mImsConfHostConnection != null) {
            MtkImsPhoneConnection mtkImsPhoneConnection = this.mImsConfHostConnection;
            if (this.mImsConfParticipants.size() >= 2) {
                restoreConferenceParticipantAddress();
                proprietaryLog("SRVCC: notify new participant connections");
                mtkImsPhoneConnection.notifyConferenceConnectionsConfigured(this.mImsConfParticipants);
            } else if (this.mImsConfParticipants.size() == 1) {
                GsmCdmaConnection gsmCdmaConnection7 = this.mImsConfParticipants.get(0);
                String conferenceParticipantAddress = mtkImsPhoneConnection.getConferenceParticipantAddress(0);
                proprietaryLog("SRVCC: restore participant connection with address: " + conferenceParticipantAddress);
                if (gsmCdmaConnection7 instanceof MtkGsmCdmaConnection) {
                    ((MtkGsmCdmaConnection) gsmCdmaConnection7).updateConferenceParticipantAddress(conferenceParticipantAddress);
                }
                proprietaryLog("SRVCC: only one connection, consider it as a normal call SRVCC");
                this.mPhone.notifyHandoverStateChanged(gsmCdmaConnection7);
            } else {
                Rlog.e(PROP_LOG_TAG, "SRVCC: abnormal case, no participant connections.");
            }
            this.mImsConfParticipants.clear();
            this.mImsConfHostConnection = null;
            this.mEconfSrvccConnectionIds = null;
        }
        Iterator it2 = this.mHandoverConnections.iterator();
        while (it2.hasNext()) {
            Connection connection2 = (Connection) it2.next();
            log("handlePollCalls - disconnect hoConn= " + connection2 + " hoConn.State= " + connection2.getState());
            if (connection2.getState().isRinging()) {
                connection2.onDisconnect(1);
            } else {
                connection2.onDisconnect(-1);
            }
            it2.remove();
        }
        if (this.mDroppedDuringPoll.size() > 0) {
            this.mMtkCi.getLastCallFailCause(obtainNoPollCompleteMessage(5));
        }
        if ((connectionCheckMtFindNewRinging != null || z4 || z9) && !this.mHasPendingSwapRequest) {
            internalClearDisconnected();
        }
        updatePhoneState();
        if (z6) {
            if (isPhoneTypeGsm()) {
                for (Connection connection3 : arrayList2) {
                    log("Notify unknown for " + connection3);
                    this.mPhone.notifyUnknownConnection(connection3);
                }
            } else {
                this.mPhone.notifyUnknownConnection(gsmCdmaConnection5);
            }
        }
        if (z4 || connectionCheckMtFindNewRinging != null || z9) {
            this.mPhone.notifyPreciseCallStateChanged();
            updateMetrics(this.mConnections);
        }
        if (!isPhoneTypeGsm() && (this.mPhone instanceof MtkGsmCdmaPhone) && z5) {
            ((MtkGsmCdmaPhone) this.mPhone).notifyCdmaCallAccepted();
        }
        if (size > 0 && this.mHandoverConnections.size() == 0 && (imsPhone = this.mPhone.getImsPhone()) != null) {
            imsPhone.callEndCleanupHandOverCallIfAny();
        }
        if (isPhoneTypeGsm() && this.mConnections != null && this.mConnections.length == 19 && this.mHelper.getCurrentTotalConnections() == 1 && this.mRingingCall.getState() == Call.State.WAITING) {
            this.mRingingCall.mState = Call.State.INCOMING;
        }
    }

    protected void dumpState() {
        Rlog.i("GsmCdmaCallTracker", "Phone State:" + this.mState);
        Rlog.i("GsmCdmaCallTracker", "Ringing call: " + this.mRingingCall.toString());
        List connections = this.mRingingCall.getConnections();
        int size = connections.size();
        for (int i = 0; i < size; i++) {
            Rlog.i("GsmCdmaCallTracker", connections.get(i).toString());
        }
        Rlog.i("GsmCdmaCallTracker", "Foreground call: " + this.mForegroundCall.toString());
        List connections2 = this.mForegroundCall.getConnections();
        int size2 = connections2.size();
        for (int i2 = 0; i2 < size2; i2++) {
            Rlog.i("GsmCdmaCallTracker", connections2.get(i2).toString());
        }
        Rlog.i("GsmCdmaCallTracker", "Background call: " + this.mBackgroundCall.toString());
        List connections3 = this.mBackgroundCall.getConnections();
        int size3 = connections3.size();
        for (int i3 = 0; i3 < size3; i3++) {
            Rlog.i("GsmCdmaCallTracker", connections3.get(i3).toString());
        }
        if (isPhoneTypeGsm()) {
            this.mHelper.LogState();
        }
    }

    public void handleMessage(Message message) {
        this.mHelper.LogerMessage(message.what);
        int i = message.what;
        if (i == 1) {
            Rlog.d("GsmCdmaCallTracker", "Event EVENT_POLL_CALLS_RESULT Received");
            if (message == this.mLastRelevantPoll) {
                this.mNeedsPoll = false;
                this.mLastRelevantPoll = null;
                boolean zNoAnyCallFromModemExist = noAnyCallFromModemExist((AsyncResult) message.obj);
                if (!zNoAnyCallFromModemExist && this.mHasPendingUpdatePhoneType) {
                    this.mHasPendingUpdatePhoneType = false;
                    updatePhoneType();
                    Rlog.d("GsmCdmaCallTracker", "[EVENT_POLL_CALLS_RESULT]!bNoCallExists");
                }
                handlePollCalls((AsyncResult) message.obj);
                if (zNoAnyCallFromModemExist && this.mHasPendingUpdatePhoneType) {
                    this.mHasPendingUpdatePhoneType = false;
                    updatePhoneType();
                    Rlog.d("GsmCdmaCallTracker", "[EVENT_POLL_CALLS_RESULT]bNoCallExists");
                }
                if (this.mHasPendingCheckAndEnableData) {
                    if (zNoAnyCallFromModemExist) {
                        checkAndEnableDataCallAfterEmergencyCallDropped();
                    }
                    this.mHasPendingCheckAndEnableData = false;
                }
                if (this.mWaitForHoldToHangupRequest.isHoldDone()) {
                    proprietaryLog("Switch ends, and poll call done, then resume hangup");
                    this.mWaitForHoldToHangupRequest.resumeHangupAfterHold();
                    return;
                }
                return;
            }
            return;
        }
        if (i == 8) {
            if (isPhoneTypeGsm()) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception != null) {
                    if (this.mWaitForHoldToRedialRequest.isWaitToRedial()) {
                        if (this.mPendingMO != null) {
                            this.mPendingMO.mCause = 3;
                            this.mPendingMO.onDisconnect(3);
                            this.mPendingMO = null;
                            this.mHangupPendingMO = false;
                            updatePhoneState();
                        }
                        resumeBackgroundAfterDialFailed();
                        this.mWaitForHoldToRedialRequest.resetToRedial();
                    }
                    this.mPhone.notifySuppServiceFailed(getFailedService(message.what));
                } else if (this.mWaitForHoldToRedialRequest.isWaitToRedial()) {
                    proprietaryLog("Switch success, then resume dial");
                    this.mWaitForHoldToRedialRequest.resumeDialAfterHold();
                }
                if (this.mWaitForHoldToHangupRequest.isWaitToHangup()) {
                    if (asyncResult.exception == null && this.mWaitForHoldToHangupRequest.mCall != null) {
                        proprietaryLog("Switch ends, found waiting hangup. switch fg/bg call.");
                        if (this.mWaitForHoldToHangupRequest.mCall != this.mForegroundCall) {
                            if (this.mWaitForHoldToHangupRequest.mCall == this.mBackgroundCall) {
                                this.mWaitForHoldToHangupRequest.setToHangup(this.mForegroundCall);
                            }
                        } else {
                            this.mWaitForHoldToHangupRequest.setToHangup(this.mBackgroundCall);
                        }
                    }
                    proprietaryLog("Switch ends, wait for poll call done to hangup");
                    this.mWaitForHoldToHangupRequest.setHoldDone();
                }
                this.mHasPendingSwapRequest = false;
                operationComplete();
                return;
            }
            return;
        }
        if (i == 14) {
            proprietaryLog("Receives EVENT_EXIT_ECM_RESPONSE_CDMA");
            if (this.mPendingCallInEcm) {
                String str = (String) ((AsyncResult) message.obj).userObj;
                if (this.mPendingMO == null) {
                    this.mPendingMO = new GsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(str), this, this.mForegroundCall, false);
                }
                if (!isPhoneTypeGsm()) {
                    this.mCi.dial(this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(str), this.mPendingCallClirMode, obtainCompleteMessage());
                    if (needToConvert(str)) {
                        this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(str));
                    }
                } else {
                    Rlog.e("GsmCdmaCallTracker", "originally unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
                    this.mCi.dial(this.mPendingMO.getAddress(), this.mPendingCallClirMode, (UUSInfo) null, obtainCompleteMessage());
                }
                this.mPendingCallInEcm = false;
            }
            this.mPhone.unsetOnEcbModeExitResponse(this);
            return;
        }
        if (i == 16) {
            if (!isPhoneTypeGsm()) {
                if (((AsyncResult) message.obj).exception == null && this.mPendingMO != null) {
                    this.mPendingMO.onConnectedInOrOut();
                    this.mPendingMO = null;
                    return;
                }
                return;
            }
            Rlog.e("GsmCdmaCallTracker", "unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
            return;
        }
        if (i != 20) {
            switch (i) {
                case 1000:
                    this.mHelper.CallIndicationProcess((AsyncResult) message.obj);
                    break;
                case 1001:
                    proprietaryLog("Receives EVENT_RADIO_OFF_OR_NOT_AVAILABLE");
                    handlePollCalls(new AsyncResult((Object) null, (Object) null, new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE)));
                    this.mLastRelevantPoll = null;
                    if (this.mPhone.isInEcm() && (this.mPhone instanceof MtkGsmCdmaPhone)) {
                        ((MtkGsmCdmaPhone) this.mPhone).sendExitEmergencyCallbackModeMessage();
                        break;
                    }
                    break;
                case 1002:
                    if (((AsyncResult) message.obj).exception != null) {
                        proprietaryLog("dial call failed!!");
                    }
                    operationComplete();
                    break;
                case 1003:
                    operationComplete();
                    break;
                case 1004:
                    proprietaryLog("Receives EVENT_CDMA_CALL_ACCEPTED");
                    if (((AsyncResult) message.obj).exception == null) {
                        handleCallAccepted();
                    }
                    break;
                case 1005:
                    log("Receives EVENT_ECONF_SRVCC_INDICATION");
                    if (!hasParsingCEPCapability()) {
                        this.mEconfSrvccConnectionIds = (int[]) ((AsyncResult) message.obj).result;
                        this.mNeedWaitImsEConfSrvcc = false;
                        pollCallsWhenSafe();
                    }
                    break;
                case 1006:
                    log("Receives EVENT_RADIO_ON");
                    this.mMtkCi.setEccList();
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
            return;
        }
        proprietaryLog("Receives EVENT_THREE_WAY_DIAL_BLANK_FLASH");
        if (!isPhoneTypeGsm()) {
            if (((AsyncResult) message.obj).exception == null) {
                final String str2 = (String) ((AsyncResult) message.obj).userObj;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (MtkGsmCdmaCallTracker.this.mPendingMO != null) {
                            MtkGsmCdmaCallTracker.this.mCi.sendCDMAFeatureCode(MtkGsmCdmaCallTracker.this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(str2), MtkGsmCdmaCallTracker.this.obtainMessage(16));
                            if (MtkGsmCdmaCallTracker.this.needToConvert(str2)) {
                                MtkGsmCdmaCallTracker.this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(str2));
                            }
                        }
                    }
                }, this.m3WayCallFlashDelay);
                return;
            } else {
                this.mPendingMO = null;
                Rlog.w("GsmCdmaCallTracker", "exception happened on Blank Flash for 3-way call");
                return;
            }
        }
        Rlog.e("GsmCdmaCallTracker", "unexpected event " + message.what + " not handled by phone type " + this.mPhone.getPhoneType());
    }

    public void hangupAll() throws CallStateException {
        proprietaryLog("hangupAll");
        this.mMtkCi.hangupAll(obtainCompleteMessage());
        if (!this.mRingingCall.isIdle()) {
            this.mRingingCall.onHangupLocal();
        }
        if (!this.mForegroundCall.isIdle()) {
            this.mForegroundCall.onHangupLocal();
        }
        if (!this.mBackgroundCall.isIdle()) {
            this.mBackgroundCall.onHangupLocal();
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
                this.mCi.hangupConnection(gsmCdmaConnection.getGsmCdmaIndex(), obtainCompleteMessage(1003));
            } catch (CallStateException e) {
                Rlog.w("GsmCdmaCallTracker", "GsmCdmaCallTracker WARN: hangup() on absent connection " + gsmCdmaConnection);
            }
        }
        gsmCdmaConnection.onHangupLocal();
    }

    public void hangup(GsmCdmaCall gsmCdmaCall) throws CallStateException {
        boolean zIsLocalEmergencyNumber;
        if (gsmCdmaCall.getConnections().size() == 0) {
            throw new CallStateException("no connections in call");
        }
        if (gsmCdmaCall == this.mRingingCall) {
            log("(ringing) hangup waiting or background");
            logHangupEvent(gsmCdmaCall);
            hangup((GsmCdmaConnection) gsmCdmaCall.getConnections().get(0));
        } else if (gsmCdmaCall == this.mForegroundCall) {
            if (gsmCdmaCall.isDialingOrAlerting()) {
                log("(foregnd) hangup dialing or alerting...");
                hangup((GsmCdmaConnection) gsmCdmaCall.getConnections().get(0));
            } else {
                logHangupEvent(gsmCdmaCall);
                log("(foregnd) hangup active");
                if (isPhoneTypeGsm()) {
                    String address = ((GsmCdmaConnection) gsmCdmaCall.getConnections().get(0)).getAddress();
                    if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(this.mPhone.getPhoneId()) || this.mPhone.getServiceState().getState() != 0)) {
                        zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), address);
                    } else {
                        zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), this.mPhone.getSubId(), address);
                    }
                    if (zIsLocalEmergencyNumber && !MtkPhoneNumberUtils.isSpecialEmergencyNumber(this.mPhone.getSubId(), address)) {
                        proprietaryLog("(foregnd) hangup active ECC call by connection index");
                        hangup((GsmCdmaConnection) gsmCdmaCall.getConnections().get(0));
                    } else if (!this.mWaitForHoldToHangupRequest.isWaitToHangup()) {
                        hangupForegroundResumeBackground();
                    } else {
                        this.mWaitForHoldToHangupRequest.setToHangup(gsmCdmaCall);
                    }
                } else {
                    hangupForegroundResumeBackground();
                }
            }
        } else if (gsmCdmaCall == this.mBackgroundCall) {
            if (this.mRingingCall.isRinging()) {
                log("hangup all conns in background call");
                hangupAllConnections(gsmCdmaCall);
            } else {
                log("(backgnd) hangup waiting/background");
                if (!this.mWaitForHoldToHangupRequest.isWaitToHangup()) {
                    hangupWaitingOrBackground();
                } else {
                    this.mWaitForHoldToHangupRequest.setToHangup(gsmCdmaCall);
                }
            }
        } else {
            throw new RuntimeException("GsmCdmaCall " + gsmCdmaCall + "does not belong to GsmCdmaCallTracker " + this);
        }
        gsmCdmaCall.onHangupLocal();
        this.mPhone.notifyPreciseCallStateChanged();
    }

    public void hangupWaitingOrBackground() {
        log("hangupWaitingOrBackground");
        logHangupEvent(this.mBackgroundCall);
        this.mCi.hangupWaitingOrBackground(obtainCompleteMessage(1003));
    }

    public void hangupForegroundResumeBackground() {
        log("hangupForegroundResumeBackground");
        this.mCi.hangupForegroundResumeBackground(obtainCompleteMessage(1003));
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

    private void CheckIfCallDisconnectButNotHandled(AsyncResult asyncResult) {
        List arrayList;
        boolean z;
        if (asyncResult.exception == null) {
            arrayList = (List) asyncResult.result;
        } else {
            arrayList = new ArrayList();
        }
        boolean z2 = false;
        int i = 0;
        while (true) {
            if (i < this.mConnections.length) {
                if (this.mConnections[i] == null) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (z && arrayList.size() == 0) {
            z2 = true;
        }
        this.bAllCallsDisconnectedButNotHandled = z2;
    }

    private boolean noAnyCallFromModemExist(AsyncResult asyncResult) {
        List arrayList;
        if (asyncResult.exception == null) {
            arrayList = (List) asyncResult.result;
        } else {
            arrayList = new ArrayList();
        }
        return arrayList.size() == 0;
    }

    void proprietaryLog(String str) {
        Rlog.d(PROP_LOG_TAG, str);
    }

    private PersistableBundle getCarrierConfig() {
        return ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
    }

    public boolean canConference() {
        boolean z;
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig != null) {
            z = carrierConfig.getBoolean("mtk_key_multiline_allow_cross_line_conference_bool");
        } else {
            z = false;
        }
        if (z) {
            return super.canConference();
        }
        return this.mMtkGsmCdmaCallTrackerExt.areConnectionsInSameLine(this.mConnections) && super.canConference();
    }

    public void conference() {
        boolean z;
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig != null) {
            z = carrierConfig.getBoolean("mtk_key_multiline_allow_cross_line_conference_bool");
        } else {
            z = false;
        }
        if (!z && !this.mMtkGsmCdmaCallTrackerExt.areConnectionsInSameLine(this.mConnections)) {
            Rlog.e(PROP_LOG_TAG, "conference fail. (not same line)");
        } else {
            super.conference();
        }
    }

    public synchronized Connection dial(String str, int i, UUSInfo uUSInfo, Bundle bundle) throws CallStateException {
        boolean zIsLocalEmergencyNumber;
        boolean zIsEmergencyNumber;
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }
        String strConvertNumberIfNecessary = convertNumberIfNecessary(this.mPhone, str);
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            this.mWaitForHoldToRedialRequest.setToRedial();
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
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(this.mPhone.getPhoneId()) || this.mPhone.getServiceState().getState() != 0)) {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), strConvertNumberIfNecessary);
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(strConvertNumberIfNecessary);
        } else {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), this.mPhone.getSubId(), strConvertNumberIfNecessary);
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), strConvertNumberIfNecessary);
        }
        this.mPendingMO = new MtkGsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(strConvertNumberIfNecessary), this, this.mForegroundCall, zIsLocalEmergencyNumber);
        this.mHangupPendingMO = false;
        this.mMetrics.writeRilDial(this.mPhone.getPhoneId(), this.mPendingMO, i, uUSInfo);
        String strConvertDialString = this.mMtkGsmCdmaCallTrackerExt.convertDialString(bundle, this.mPendingMO.getAddress());
        if (strConvertDialString != null) {
            this.mPendingMO.setConnectionExtras(bundle);
        }
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            this.mWaitForHoldToRedialRequest.resetToRedial();
            pollCallsWhenSafe();
        } else {
            setMute(false);
            if (!this.mWaitForHoldToRedialRequest.isWaitToRedial()) {
                if (zIsEmergencyNumber && !MtkPhoneNumberUtils.isSpecialEmergencyNumber(this.mPhone.getSubId(), strConvertNumberIfNecessary)) {
                    this.mMtkCi.setEccServiceCategory(MtkPhoneNumberUtils.getServiceCategoryFromEccBySubId(strConvertNumberIfNecessary, this.mPhone.getSubId()), null);
                    this.mMtkCi.emergencyDial(this.mPendingMO.getAddress(), i, uUSInfo, obtainCompleteMessage(1002));
                } else {
                    if (strConvertDialString != null) {
                        this.mNumberConverted = true;
                    } else {
                        strConvertDialString = this.mPendingMO.getAddress();
                    }
                    this.mCi.dial(strConvertDialString, i, uUSInfo, obtainCompleteMessage());
                }
            } else {
                if (strConvertDialString != null) {
                    this.mNumberConverted = true;
                } else {
                    strConvertDialString = this.mPendingMO.getAddress();
                }
                this.mWaitForHoldToRedialRequest.setToRedial(strConvertDialString, i, uUSInfo);
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

    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        if (this.mRingingCall.getState() == Call.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        }
        if (isPhoneTypeGsm()) {
            if (!this.mHasPendingSwapRequest) {
                this.mWaitForHoldToHangupRequest.setToHangup();
                this.mCi.switchWaitingOrHoldingAndActive(obtainCompleteMessage(8));
                this.mHasPendingSwapRequest = true;
                return;
            }
            return;
        }
        if (this.mForegroundCall.getConnections().size() > 1) {
            flashAndSetGenericTrue();
        } else {
            this.mCi.sendCDMAFeatureCode("", obtainMessage(8));
        }
    }

    private void resumeBackgroundAfterDialFailed() {
        List list = (List) this.mBackgroundCall.mConnections.clone();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            ((MtkGsmCdmaConnection) list.get(i)).resumeHoldAfterDialFailed();
        }
    }

    protected void disableDataCallInEmergencyCall(String str) {
        boolean zIsLocalEmergencyNumber;
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(this.mPhone.getPhoneId()) || this.mPhone.getServiceState().getState() != 0)) {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), str);
        } else {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), this.mPhone.getSubId(), str);
        }
        if (zIsLocalEmergencyNumber) {
            log("disableDataCallInEmergencyCall");
            setIsInEmergencyCall();
        }
    }

    private void disableDataCallInEmergencyCall(boolean z) {
        if (z) {
            log("disableDataCallInEmergencyCall");
            setIsInEmergencyCall();
        }
    }

    protected boolean canDial() {
        if (!isPhoneTypeGsm() && this.mForegroundCall.getState() == Call.State.DIALING) {
            Rlog.i(PROP_LOG_TAG, "canDial(), state: " + this.mForegroundCall.getState());
            return false;
        }
        return super.canDial();
    }

    protected Connection dial(String str, int i) throws CallStateException {
        String strConvertNumberIfNecessary;
        boolean zIsLocalEmergencyNumber;
        boolean zIsEmergencyNumber;
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
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(this.mPhone.getPhoneId()) || this.mPhone.getServiceState().getState() != 0)) {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), strConvertNumberIfNecessary);
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(strConvertNumberIfNecessary);
        } else {
            zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mPhone.getContext(), this.mPhone.getSubId(), strConvertNumberIfNecessary);
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(this.mPhone.getSubId(), strConvertNumberIfNecessary);
        }
        if ("OP20".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "")) && zIsInEcm && !zIsLocalEmergencyNumber) {
            throw new CallStateException("cannot dial in ECBM");
        }
        if (zIsInEcm && zIsLocalEmergencyNumber) {
            handleEcmTimer(1);
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            return dialThreeWay(strConvertNumberIfNecessary);
        }
        this.mPendingMO = new MtkGsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(strConvertNumberIfNecessary), this, this.mForegroundCall, zIsLocalEmergencyNumber);
        this.mHangupPendingMO = false;
        if (this.mPendingMO.getAddress() == null || this.mPendingMO.getAddress().length() == 0 || this.mPendingMO.getAddress().indexOf(78) >= 0) {
            this.mPendingMO.mCause = 7;
            pollCallsWhenSafe();
        } else {
            setMute(false);
            disableDataCallInEmergencyCall(zIsLocalEmergencyNumber);
            if (!zIsInEcm || (zIsInEcm && zIsLocalEmergencyNumber)) {
                if (zIsEmergencyNumber) {
                    this.mMtkCi.emergencyDial(this.mPendingMO.getAddress(), i, null, obtainCompleteMessage());
                } else {
                    this.mCi.dial(this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(strConvertNumberIfNecessary), i, obtainCompleteMessage());
                }
                if (needToConvert(strConvertNumberIfNecessary)) {
                    this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(strConvertNumberIfNecessary));
                }
            } else {
                this.mPhone.exitEmergencyCallbackMode();
                this.mPhone.setOnEcbModeExitResponse(this, 14, strConvertNumberIfNecessary);
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
            this.mPendingMO = new MtkGsmCdmaConnection(this.mPhone, checkForTestEmergencyNumber(str), this, this.mForegroundCall, this.mIsInEmergencyCall);
            PersistableBundle config = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfig();
            if (config != null) {
                this.m3WayCallFlashDelay = config.getInt("cdma_3waycall_flash_delay_int");
            } else {
                this.m3WayCallFlashDelay = 0;
            }
            if (this.m3WayCallFlashDelay > 0) {
                this.mCi.sendCDMAFeatureCode("", obtainMessage(20, str));
            } else {
                this.mCi.sendCDMAFeatureCode(this.mPendingMO.getAddress() + "," + PhoneNumberUtils.extractNetworkPortionAlt(str), obtainMessage(16));
                if (needToConvert(str)) {
                    this.mPendingMO.setConverted(PhoneNumberUtils.extractNetworkPortionAlt(str));
                }
            }
            return this.mPendingMO;
        }
        return null;
    }

    protected void handleCallWaitingInfo(CdmaCallWaitingNotification cdmaCallWaitingNotification) {
        processPlusCodeForWaitingCall(cdmaCallWaitingNotification);
        if (!shouldNotifyWaitingCall(cdmaCallWaitingNotification)) {
            return;
        }
        if (this.mForegroundCall.mConnections.size() > 1) {
            Iterator it = this.mForegroundCall.mConnections.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Connection connection = (Connection) it.next();
                if (cdmaCallWaitingNotification.number != null && cdmaCallWaitingNotification.number.equals(connection.getAddress())) {
                    connection.onDisconnect(2);
                    break;
                }
            }
        }
        new MtkGsmCdmaConnection(this.mPhone.getContext(), cdmaCallWaitingNotification, this, this.mRingingCall);
        updatePhoneState();
        notifyCallWaitingInfo(cdmaCallWaitingNotification);
    }

    private void handleCallAccepted() {
        List connections = this.mForegroundCall.getConnections();
        int size = connections.size();
        proprietaryLog("handleCallAccepted, fgcall count=" + size);
        if (size == 1) {
            GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection) connections.get(0);
            if ((gsmCdmaConnection instanceof MtkGsmCdmaConnection) && (this.mPhone instanceof MtkGsmCdmaPhone) && ((MtkGsmCdmaConnection) gsmCdmaConnection).onCdmaCallAccepted()) {
                ((MtkGsmCdmaPhone) this.mPhone).notifyCdmaCallAccepted();
            }
        }
    }

    private String processPlusCodeForDriverCall(String str, boolean z, int i) {
        if (z && i == 145) {
            if (str != null && str.length() > 0 && str.charAt(0) == '+') {
                str = str.substring(1, str.length());
            }
            str = PlusCodeProcessor.getPlusCodeUtils().removeIddNddAddPlusCode(str);
        }
        return PhoneNumberUtils.stringFromStringAndTOA(str, i);
    }

    private void processPlusCodeForWaitingCall(CdmaCallWaitingNotification cdmaCallWaitingNotification) {
        String str = cdmaCallWaitingNotification.number;
        if (str != null && str.length() > 0) {
            cdmaCallWaitingNotification.number = processPlusCodeForWaitingCall(str, cdmaCallWaitingNotification.numberType);
        }
    }

    private String processPlusCodeForWaitingCall(String str, int i) {
        String strRemoveIddNddAddPlusCode = PlusCodeProcessor.getPlusCodeUtils().removeIddNddAddPlusCode(str);
        if (strRemoveIddNddAddPlusCode == null) {
            return str;
        }
        if (i == 1 && strRemoveIddNddAddPlusCode.length() > 0 && strRemoveIddNddAddPlusCode.charAt(0) != '+') {
            return "+" + strRemoveIddNddAddPlusCode;
        }
        return strRemoveIddNddAddPlusCode;
    }

    private boolean needToConvert(String str) {
        String dialString = GsmCdmaConnection.formatDialString(str);
        return (str == null || dialString == null || str.equals(dialString)) ? false : true;
    }

    private boolean shouldNotifyWaitingCall(CdmaCallWaitingNotification cdmaCallWaitingNotification) {
        GsmCdmaConnection latestConnection;
        String str = cdmaCallWaitingNotification.number;
        proprietaryLog("shouldNotifyWaitingCall, address=" + str);
        if (str != null && str.length() > 0 && (latestConnection = this.mRingingCall.getLatestConnection()) != null && str.equals(latestConnection.getAddress())) {
            proprietaryLog("handleCallWaitingInfo, skip duplicate waiting call!");
            return false;
        }
        return true;
    }

    protected void updatePhoneState() {
        PhoneConstants.State state = this.mState;
        if (this.mRingingCall.isRinging()) {
            this.mState = PhoneConstants.State.RINGING;
        } else if (this.mPendingMO != null || !this.mForegroundCall.isIdle() || !this.mBackgroundCall.isIdle()) {
            this.mState = PhoneConstants.State.OFFHOOK;
        } else {
            Phone imsPhone = this.mPhone.getImsPhone();
            if (imsPhone != null) {
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

    protected void notifySrvccState(Call.SrvccState srvccState, ArrayList<Connection> arrayList) {
        if (srvccState == Call.SrvccState.STARTED && arrayList != null) {
            this.mHandoverConnections.addAll(arrayList);
            if (!hasParsingCEPCapability()) {
                for (Connection connection : this.mHandoverConnections) {
                    if (connection.isMultiparty() && (connection instanceof MtkImsPhoneConnection) && connection.isConferenceHost()) {
                        log("srvcc: mNeedWaitImsEConfSrvcc set True");
                        this.mNeedWaitImsEConfSrvcc = true;
                        this.mImsConfHostConnection = connection;
                    }
                }
            }
        } else if (srvccState != Call.SrvccState.COMPLETED) {
            this.mHandoverConnections.clear();
        }
        log("notifySrvccState: mHandoverConnections= " + this.mHandoverConnections.toString());
    }

    protected Connection getHoConnection(DriverCall driverCall) {
        if (driverCall == null) {
            return null;
        }
        if (this.mEconfSrvccConnectionIds != null && driverCall != null) {
            int i = this.mEconfSrvccConnectionIds[0];
            int i2 = 1;
            while (true) {
                if (i2 > i) {
                    break;
                }
                if (driverCall.index != this.mEconfSrvccConnectionIds[i2]) {
                    i2++;
                } else {
                    proprietaryLog("SRVCC: getHoConnection for call-id:" + driverCall.index + " in a conference is found!");
                    if (this.mImsConfHostConnection == null) {
                        proprietaryLog("SRVCC: but mImsConfHostConnection is null, try to find by callState");
                    } else {
                        proprietaryLog("SRVCC: ret= " + this.mImsConfHostConnection);
                        return this.mImsConfHostConnection;
                    }
                }
            }
        }
        log("SRVCC: getHoConnection() with dc, number = " + driverCall.number + " state = " + driverCall.state);
        if (driverCall.number != null && !driverCall.number.isEmpty()) {
            for (Connection connection : this.mHandoverConnections) {
                log("getHoConnection - compare number: hoConn= " + connection.toString());
                if (connection.getAddress() != null && connection.getAddress().contains(driverCall.number)) {
                    log("getHoConnection: Handover connection match found = " + connection.toString());
                    return connection;
                }
            }
        }
        for (Connection connection2 : this.mHandoverConnections) {
            log("getHoConnection: compare state hoConn= " + connection2.toString());
            if (connection2.getStateBeforeHandover() == Call.stateFromDCState(driverCall.state)) {
                log("getHoConnection: Handover connection match found = " + connection2.toString());
                return connection2;
            }
        }
        return null;
    }

    private synchronized boolean restoreConferenceParticipantAddress() {
        if (this.mEconfSrvccConnectionIds == null) {
            proprietaryLog("SRVCC: restoreConferenceParticipantAddress():ignore because mEconfSrvccConnectionIds is empty");
            return false;
        }
        int i = this.mEconfSrvccConnectionIds[0];
        boolean z = false;
        for (int i2 = 1; i2 <= i; i2++) {
            GsmCdmaConnection gsmCdmaConnection = this.mConnections[this.mEconfSrvccConnectionIds[i2] - 1];
            if (gsmCdmaConnection != null) {
                proprietaryLog("SRVCC: found conference connections!");
                if (gsmCdmaConnection.mOrigConnection instanceof MtkImsPhoneConnection) {
                    MtkImsPhoneConnection mtkImsPhoneConnection = gsmCdmaConnection.mOrigConnection;
                    if (mtkImsPhoneConnection == null) {
                        proprietaryLog("SRVCC: no host, ignore connection: " + gsmCdmaConnection);
                    } else {
                        String conferenceParticipantAddress = mtkImsPhoneConnection.getConferenceParticipantAddress(i2 - 1);
                        if (gsmCdmaConnection instanceof MtkGsmCdmaConnection) {
                            ((MtkGsmCdmaConnection) gsmCdmaConnection).updateConferenceParticipantAddress(conferenceParticipantAddress);
                        }
                        proprietaryLog("SRVCC: restore Connection=" + gsmCdmaConnection + " with address:" + conferenceParticipantAddress);
                        z = true;
                    }
                } else {
                    proprietaryLog("SRVCC: host is abnormal, ignore connection: " + gsmCdmaConnection);
                }
            }
        }
        return z;
    }

    boolean hasParsingCEPCapability() {
        MtkHardwareConfig mtkHardwareConfig = (MtkHardwareConfig) this.mTelDevController.getModem(0);
        if (mtkHardwareConfig == null) {
            return false;
        }
        return mtkHardwareConfig.hasParsingCEPCapability();
    }

    public int getHandoverConnectionSize() {
        return this.mHandoverConnections.size();
    }
}
