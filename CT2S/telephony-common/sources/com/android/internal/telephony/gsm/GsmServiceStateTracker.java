package com.android.internal.telephony.gsm;

import android.R;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.TimeUtils;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RestrictedState;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

final class GsmServiceStateTracker extends ServiceStateTracker {
    protected static final String ACTION_DELAY_RADIO_ON = "android.intent.action.ACTION_DELAY_RADIO_ON";
    static final int CP_ASSERT = 1007;
    static final int CS_DISABLED = 1004;
    static final int CS_EMERGENCY_ENABLED = 1006;
    static final int CS_ENABLED = 1003;
    static final int CS_NORMAL_ENABLED = 1005;
    static final int CS_NOTIFICATION = 999;
    private static final int EVENT_ALL_DATA_DISCONNECTED = 1001;
    static final String LOG_TAG = "GsmSST";
    private static final int MAX_NITZ_YEAR = 2037;
    static final int PS_DISABLED = 1002;
    static final int PS_ENABLED = 1001;
    static final int PS_NOTIFICATION = 888;
    private static final int SYNC_APN_TIME_DELAY = 180000;
    static final boolean VDBG = false;
    private static final String WAKELOCK_TAG = "ServiceStateTracker";
    private ContentObserver mAutoTimeObserver;
    private ContentObserver mAutoTimeZoneObserver;
    GsmCellLocation mCellLoc;
    private ContentResolver mCr;
    private boolean mDataRoaming;
    private boolean mEmergencyOnly;
    private boolean mGotCountryCode;
    private boolean mGsmRoaming;
    private BroadcastReceiver mIntentReceiver;
    private CellIdentityLte mLasteCellIdentityLte;
    private int mMaxDataCalls;
    private boolean mNeedFixZoneAfterNitz;
    private CellIdentityLte mNewCellIdentityLte;
    GsmCellLocation mNewCellLoc;
    private int mNewMaxDataCalls;
    private int mNewReasonDataDenied;
    private boolean mNitzUpdatedTime;
    private Notification mNotification;
    private GSMPhone mPhone;
    int mPreferredNetworkType;
    private int mReasonDataDenied;
    private boolean mReportedGprsNoReg;
    long mSavedAtTime;
    long mSavedTime;
    String mSavedTimeZone;
    private boolean mStartedGprsRegCheck;
    private PowerManager.WakeLock mWakeLock;
    private boolean mZoneDst;
    private int mZoneOffset;
    private long mZoneTime;

    public GsmServiceStateTracker(GSMPhone phone) {
        super(phone, phone.mCi, new CellInfoLte());
        boolean z = false;
        this.mMaxDataCalls = 1;
        this.mNewMaxDataCalls = 1;
        this.mReasonDataDenied = -1;
        this.mNewReasonDataDenied = -1;
        this.mNewCellIdentityLte = new CellIdentityLte();
        this.mLasteCellIdentityLte = new CellIdentityLte();
        this.mGsmRoaming = false;
        this.mDataRoaming = false;
        this.mEmergencyOnly = false;
        this.mNeedFixZoneAfterNitz = false;
        this.mGotCountryCode = false;
        this.mNitzUpdatedTime = false;
        this.mStartedGprsRegCheck = false;
        this.mReportedGprsNoReg = false;
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!GsmServiceStateTracker.this.mPhone.mIsTheCurrentActivePhone) {
                    Rlog.e(GsmServiceStateTracker.LOG_TAG, "Received Intent " + intent + " while being destroyed. Ignoring.");
                    return;
                }
                if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                    GsmServiceStateTracker.this.updateSpnDisplay();
                    return;
                }
                if (intent.getAction().equals("android.intent.action.ACTION_RADIO_OFF")) {
                    GsmServiceStateTracker.this.mAlarmSwitch = false;
                    DcTrackerBase dcTracker = GsmServiceStateTracker.this.mPhone.mDcTracker;
                    GsmServiceStateTracker.this.powerOffRadioSafely(dcTracker);
                } else {
                    if (intent.getAction().equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                        GsmServiceStateTracker.this.log("Received Intent ACTION_SET_RADIO_CAPABILITY_DONE");
                        ArrayList<RadioAccessFamily> newPhoneRcList = intent.getParcelableArrayListExtra("rafs");
                        if (newPhoneRcList == null || newPhoneRcList.size() == 0) {
                            GsmServiceStateTracker.this.log("EXTRA_RADIO_ACCESS_FAMILY not present.");
                            return;
                        } else {
                            GsmServiceStateTracker.this.onSetPhoneRCDone(newPhoneRcList);
                            return;
                        }
                    }
                    if (intent.getAction().equals(GsmServiceStateTracker.ACTION_DELAY_RADIO_ON) && GsmServiceStateTracker.this.mDesiredPowerState && GsmServiceStateTracker.this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
                        GsmServiceStateTracker.this.mCi.setRadioPower(true, null);
                    }
                }
            }
        };
        this.mAutoTimeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Rlog.i("GsmServiceStateTracker", "Auto time state changed");
                GsmServiceStateTracker.this.revertToNitzTime();
            }
        };
        this.mAutoTimeZoneObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Rlog.i("GsmServiceStateTracker", "Auto time zone state changed");
                GsmServiceStateTracker.this.revertToNitzTimeZone();
            }
        };
        this.mPhone = phone;
        this.mCellLoc = new GsmCellLocation();
        this.mNewCellLoc = new GsmCellLocation();
        PowerManager powerManager = (PowerManager) phone.getContext().getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, WAKELOCK_TAG);
        this.mCi.registerForAvailable(this, 13, null);
        this.mCi.registerForRadioStateChanged(this, 1, null);
        this.mCi.registerForVoiceNetworkStateChanged(this, 2, null);
        this.mCi.setOnNITZTime(this, 11, null);
        this.mCi.setOnRestrictedStateChanged(this, 23, null);
        this.mCr = phone.getContext().getContentResolver();
        int airplaneMode = Settings.Global.getInt(this.mCr, "airplane_mode_on", 0);
        final String simEnabledKey = phone.getPhoneId() == PhoneConstants.SimId.SIM1.ordinal() ? "enable_sim1" : "enable_sim2";
        boolean simEnabled = Settings.Global.getInt(this.mCr, simEnabledKey, 1) != 0;
        Boolean needSyncApnToCp = Boolean.valueOf(SystemProperties.getBoolean("persist.radio.syncApn", false));
        log("needSyncApnToCp = " + needSyncApnToCp);
        if (!needSyncApnToCp.booleanValue()) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    GsmServiceStateTracker.this.log("After delay 180000s set property to true");
                    if (!SystemProperties.getBoolean("persist.radio.syncApn", false)) {
                        SystemProperties.set("persist.radio.syncApn", "true");
                        int airplaneMode2 = Settings.Global.getInt(GsmServiceStateTracker.this.mCr, "airplane_mode_on", 0);
                        boolean simEnabled2 = Settings.Global.getInt(GsmServiceStateTracker.this.mCr, simEnabledKey, 1) != 0;
                        GsmServiceStateTracker.this.mDesiredPowerState = airplaneMode2 <= 0 && simEnabled2;
                        GsmServiceStateTracker.this.mPhone.setRadioPower(GsmServiceStateTracker.this.mDesiredPowerState);
                    }
                }
            }, 180000L);
        }
        if (airplaneMode <= 0 && simEnabled && needSyncApnToCp.booleanValue()) {
            z = true;
        }
        this.mDesiredPowerState = z;
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time"), true, this.mAutoTimeObserver);
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), true, this.mAutoTimeZoneObserver);
        setSignalStrengthDefaultValues();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        phone.getContext().registerReceiver(this.mIntentReceiver, filter);
        IntentFilter filter2 = new IntentFilter();
        Context context = phone.getContext();
        filter2.addAction("android.intent.action.ACTION_RADIO_OFF");
        filter2.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        filter2.addAction(ACTION_DELAY_RADIO_ON);
        context.registerReceiver(this.mIntentReceiver, filter2);
    }

    @Override
    public void dispose() {
        checkCorrectThread();
        log("ServiceStateTracker dispose");
        this.mCi.unregisterForAvailable(this);
        this.mCi.unregisterForRadioStateChanged(this);
        this.mCi.unregisterForVoiceNetworkStateChanged(this);
        if (this.mUiccApplcation != null) {
            this.mUiccApplcation.unregisterForReady(this);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForRecordsLoaded(this);
        }
        this.mCi.unSetOnRestrictedStateChanged(this);
        this.mCi.unSetOnNITZTime(this);
        this.mCr.unregisterContentObserver(this.mAutoTimeObserver);
        this.mCr.unregisterContentObserver(this.mAutoTimeZoneObserver);
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        Phone imsPhone = this.mPhoneBase.getImsPhone();
        if (imsPhone != null) {
            ((ImsPhone) imsPhone).unregisterForImsStateChanged(this);
        }
        super.dispose();
    }

    protected void finalize() {
        log("finalize");
    }

    @Override
    protected Phone getPhone() {
        return this.mPhone;
    }

    @Override
    public void handleMessage(Message msg) {
        if (!this.mPhone.mIsTheCurrentActivePhone) {
            Rlog.e(LOG_TAG, "Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
            return;
        }
        switch (msg.what) {
            case 1:
                setPowerStateToDesired();
                pollState();
                return;
            case 2:
                pollState();
                return;
            case 3:
                if (this.mCi.getRadioState().isOn()) {
                    onSignalStrengthResult((AsyncResult) msg.obj, true);
                    queueNextSignalStrengthPoll();
                    return;
                }
                return;
            case 4:
            case 5:
            case 6:
            case 14:
                handlePollStateResult(msg.what, (AsyncResult) msg.obj);
                return;
            case 10:
                this.mCi.getSignalStrength(obtainMessage(3));
                return;
            case 11:
                AsyncResult ar = (AsyncResult) msg.obj;
                String nitzString = (String) ((Object[]) ar.result)[0];
                long nitzReceiveTime = ((Long) ((Object[]) ar.result)[1]).longValue();
                setTimeFromNITZString(nitzString, nitzReceiveTime);
                return;
            case 12:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception == null && ar2.result != null) {
                    SignalStrength tmpSignal = (SignalStrength) ar2.result;
                    if (tmpSignal.getGsmSignalStrength() == 67 && tmpSignal.getGsmBitErrorRate() == 89) {
                        SignalStrength newSignalStrength = new SignalStrength(99, tmpSignal.getGsmBitErrorRate(), tmpSignal.getCdmaDbm(), tmpSignal.getCdmaEcio(), tmpSignal.getEvdoDbm(), tmpSignal.getEvdoEcio(), tmpSignal.getEvdoSnr(), tmpSignal.getLteSignalStrength(), tmpSignal.getLteRsrp(), tmpSignal.getLteRsrq(), tmpSignal.getLteRssnr(), tmpSignal.getLteCqi(), tmpSignal.isGsm());
                        ar2.result = newSignalStrength;
                        Rlog.d(LOG_TAG, "CSQ:99, 99 is regarded as CP assert!");
                        setNotification(1007);
                    }
                }
                this.mDontPollSignalStrength = true;
                onSignalStrengthResult(ar2, true);
                return;
            case 13:
                return;
            case 15:
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3.exception == null) {
                    String[] states = (String[]) ar3.result;
                    int lac = -1;
                    int cid = -1;
                    if (states.length >= 3) {
                        try {
                            if (states[1] != null && states[1].length() > 0) {
                                lac = Integer.parseInt(states[1], 16);
                            }
                            if (states[2] != null && states[2].length() > 0) {
                                cid = Integer.parseInt(states[2], 16);
                            }
                        } catch (NumberFormatException ex) {
                            Rlog.w(LOG_TAG, "error parsing location: " + ex);
                        }
                    }
                    this.mCellLoc.setLacAndCid(lac, cid);
                    this.mPhone.notifyLocationChanged();
                    break;
                }
                disableSingleLocationUpdate();
                return;
            case 16:
                log("EVENT_SIM_RECORDS_LOADED: what=" + msg.what);
                this.mPhone.notifyOtaspChanged(3);
                updatePhoneObject();
                updateSpnDisplay();
                return;
            case 17:
                this.mOnSubscriptionsChangedListener.mPreviousSubId.set(-1);
                pollState();
                queueNextSignalStrengthPoll();
                return;
            case 18:
                if (((AsyncResult) msg.obj).exception == null) {
                    this.mCi.getVoiceRegistrationState(obtainMessage(15, null));
                    return;
                }
                return;
            case 19:
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    this.mPreferredNetworkType = ((int[]) ar4.result)[0];
                } else {
                    this.mPreferredNetworkType = 7;
                }
                Message message = obtainMessage(20, ar4.userObj);
                this.mCi.setPreferredNetworkType(7, message);
                return;
            case 20:
                Message message2 = obtainMessage(21, ((AsyncResult) msg.obj).userObj);
                this.mCi.setPreferredNetworkType(this.mPreferredNetworkType, message2);
                return;
            case 21:
                AsyncResult ar5 = (AsyncResult) msg.obj;
                if (ar5.userObj != null) {
                    AsyncResult.forMessage((Message) ar5.userObj).exception = ar5.exception;
                    ((Message) ar5.userObj).sendToTarget();
                    return;
                }
                return;
            case 22:
                if (this.mSS != null && !isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
                    GsmCellLocation loc = (GsmCellLocation) this.mPhone.getCellLocation();
                    Object[] objArr = new Object[2];
                    objArr[0] = this.mSS.getOperatorNumeric();
                    objArr[1] = Integer.valueOf(loc != null ? loc.getCid() : -1);
                    EventLog.writeEvent(EventLogTags.DATA_NETWORK_REGISTRATION_FAIL, objArr);
                    this.mReportedGprsNoReg = true;
                }
                this.mStartedGprsRegCheck = false;
                return;
            case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
                log("EVENT_RESTRICTED_STATE_CHANGED");
                onRestrictedStateChanged((AsyncResult) msg.obj);
                return;
            case 45:
                log("EVENT_CHANGE_IMS_STATE:");
                Phone imsPhone = this.mPhoneBase.getImsPhone();
                if (imsPhone != null && imsPhone.getServiceState().getState() != 0) {
                    boolean ret = processPendingRadioPowerOffAfterDataOff();
                    log("EVENT_CHANGE_IMS_STATE ret= " + ret);
                    return;
                }
                return;
            case 1001:
                int dds = SubscriptionManager.getDefaultDataSubId();
                ProxyController.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized (this) {
                    if (this.mPendingRadioPowerOffAfterDataOff) {
                        log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                    break;
                }
                return;
            default:
                super.handleMessage(msg);
                return;
        }
    }

    @Override
    protected void setPowerStateToDesired() {
        log("mDeviceShuttingDown = " + this.mDeviceShuttingDown);
        log("mDesiredPowerState = " + this.mDesiredPowerState);
        log("getRadioState = " + this.mCi.getRadioState());
        log("mPowerOffDelayNeed = " + this.mPowerOffDelayNeed);
        log("mAlarmSwitch = " + this.mAlarmSwitch);
        if (this.mAlarmSwitch) {
            log("mAlarmSwitch == true");
            AlarmManager am = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
            am.cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = false;
        }
        if (this.mDesiredPowerState && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            if (Dsds.isDualSimSolution() && this.mPhone.getPhoneId() == Dsds.getInitialDataAllowSIM()) {
                AlarmManager am2 = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
                PendingIntent intent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, new Intent(ACTION_DELAY_RADIO_ON), 0);
                am2.set(2, SystemClock.elapsedRealtime() + 1000, intent);
                return;
            }
            this.mCi.setRadioPower(true, null);
            return;
        }
        if (!this.mDesiredPowerState && this.mCi.getRadioState().isOn()) {
            if (this.mPowerOffDelayNeed) {
                if (this.mImsRegistrationOnOff && !this.mAlarmSwitch) {
                    log("mImsRegistrationOnOff == true");
                    Context context = this.mPhone.getContext();
                    AlarmManager am3 = (AlarmManager) context.getSystemService("alarm");
                    Intent intent2 = new Intent("android.intent.action.ACTION_RADIO_OFF");
                    this.mRadioOffIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);
                    this.mAlarmSwitch = true;
                    log("Alarm setting");
                    am3.set(2, SystemClock.elapsedRealtime() + 3000, this.mRadioOffIntent);
                    return;
                }
                DcTrackerBase dcTracker = this.mPhone.mDcTracker;
                powerOffRadioSafely(dcTracker);
                return;
            }
            DcTrackerBase dcTracker2 = this.mPhone.mDcTracker;
            powerOffRadioSafely(dcTracker2);
            return;
        }
        if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
            this.mCi.requestShutdown(null);
        }
    }

    @Override
    protected void hangupAndPowerOff() {
        if (this.mPhone.isInCall()) {
            this.mPhone.mCT.mRingingCall.hangupIfAlive();
            this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
            this.mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        this.mCi.setRadioPower(false, null);
    }

    @Override
    protected void updateSpnDisplay() {
        boolean showPlmn;
        String plmn;
        IccRecords iccRecords = this.mIccRecords;
        int rule = iccRecords != null ? iccRecords.getDisplayRule(this.mSS.getOperatorNumeric()) : 0;
        if (this.mSS.getVoiceRegState() == 1 || this.mSS.getVoiceRegState() == 2) {
            showPlmn = true;
            if (this.mEmergencyOnly) {
                plmn = Resources.getSystem().getText(R.string.emailTypeWork).toString();
            } else {
                plmn = Resources.getSystem().getText(R.string.duration_minutes_shortest).toString();
            }
            log("updateSpnDisplay: radio is on but out of service, set plmn='" + plmn + "'");
        } else if (this.mSS.getVoiceRegState() == 0) {
            plmn = this.mSS.getOperatorAlphaLong();
            showPlmn = !TextUtils.isEmpty(plmn) && (rule & 2) == 2;
        } else {
            showPlmn = true;
            plmn = Resources.getSystem().getText(R.string.duration_minutes_shortest).toString();
            log("updateSpnDisplay: radio is off w/ showPlmn=true plmn=" + plmn);
        }
        String spn = iccRecords != null ? iccRecords.getServiceProviderName() : "";
        boolean showSpn = !TextUtils.isEmpty(spn) && (rule & 1) == 1;
        if (this.mSS.getVoiceRegState() == 3 || (showPlmn && TextUtils.equals(spn, plmn))) {
            spn = null;
            showSpn = false;
        }
        if (showPlmn != this.mCurShowPlmn || showSpn != this.mCurShowSpn || !TextUtils.equals(spn, this.mCurSpn) || !TextUtils.equals(plmn, this.mCurPlmn)) {
            log(String.format("updateSpnDisplay: changed sending intent rule=" + rule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'", Boolean.valueOf(showPlmn), plmn, Boolean.valueOf(showSpn), spn));
            Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            intent.addFlags(536870912);
            intent.putExtra("showSpn", showSpn);
            intent.putExtra("spn", spn);
            intent.putExtra("showPlmn", showPlmn);
            intent.putExtra(Telephony.CellBroadcasts.PLMN, plmn);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), showPlmn, plmn, showSpn, spn)) {
                this.mSpnUpdatePending = true;
            }
        }
        this.mCurShowSpn = showSpn;
        this.mCurShowPlmn = showPlmn;
        this.mCurSpn = spn;
        this.mCurPlmn = plmn;
    }

    @Override
    protected void handlePollStateResult(int what, AsyncResult ar) {
        int mcc;
        int mnc;
        if (ar.userObj == this.mPollingContext) {
            if (ar.exception != null) {
                CommandException.Error err = null;
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) ar.exception).getCommandError();
                }
                if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                    cancelPollState();
                    return;
                } else if (!this.mCi.getRadioState().isOn()) {
                    cancelPollState();
                    return;
                } else if (err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                    loge("RIL implementation has returned an error where it must succeed" + ar.exception);
                }
            } else {
                try {
                    switch (what) {
                        case 4:
                            String[] states = (String[]) ar.result;
                            int lac = -1;
                            int cid = -1;
                            int type = 0;
                            int regState = 4;
                            int psc = -1;
                            if (states.length > 0) {
                                try {
                                    regState = Integer.parseInt(states[0]);
                                    if (states.length >= 3) {
                                        if (states[1] != null && states[1].length() > 0) {
                                            lac = Integer.parseInt(states[1], 16);
                                        }
                                        if (states[2] != null && states[2].length() > 0) {
                                            cid = Integer.parseInt(states[2], 16);
                                        }
                                        if (states.length >= 4 && states[3] != null) {
                                            type = Integer.parseInt(states[3]);
                                        }
                                    }
                                    if (states.length > 14 && states[14] != null && states[14].length() > 0) {
                                        psc = Integer.parseInt(states[14], 16);
                                    }
                                } catch (NumberFormatException ex) {
                                    loge("error parsing RegistrationState: " + ex);
                                }
                            }
                            this.mGsmRoaming = regCodeIsRoaming(regState);
                            this.mNewSS.setState(regCodeToServiceState(regState));
                            this.mNewSS.setRilVoiceRadioTechnology(type);
                            boolean isVoiceCapable = this.mPhoneBase.getContext().getResources().getBoolean(R.^attr-private.externalRouteEnabledDrawable);
                            if ((regState == 13 || regState == 20 || regState == 12 || regState == 14) && isVoiceCapable) {
                                this.mEmergencyOnly = true;
                            } else {
                                this.mEmergencyOnly = false;
                            }
                            this.mNewCellLoc.setLacAndCid(lac, cid);
                            this.mNewCellLoc.setPsc(psc);
                            break;
                        case 5:
                            String[] states2 = (String[]) ar.result;
                            int type2 = 0;
                            int regState2 = 4;
                            this.mNewReasonDataDenied = -1;
                            this.mNewMaxDataCalls = 1;
                            if (states2.length > 0) {
                                try {
                                    regState2 = Integer.parseInt(states2[0]);
                                    if (states2.length >= 4 && states2[3] != null) {
                                        type2 = Integer.parseInt(states2[3]);
                                    }
                                    if (states2.length >= 5 && regState2 == 3) {
                                        this.mNewReasonDataDenied = Integer.parseInt(states2[4]);
                                    }
                                    if (states2.length >= 6) {
                                        this.mNewMaxDataCalls = Integer.parseInt(states2[5]);
                                    }
                                } catch (NumberFormatException ex2) {
                                    loge("error parsing GprsRegistrationState: " + ex2);
                                }
                            }
                            int dataRegState = dataRegCodeToServiceState(regState2);
                            this.mNewSS.setDataRegState(dataRegState);
                            this.mDataRoaming = regCodeIsRoaming(regState2);
                            this.mNewSS.setRilDataRadioTechnology(type2);
                            if (type2 == 14) {
                                String operatorNumeric = null;
                                try {
                                    operatorNumeric = this.mNewSS.getOperatorNumeric();
                                    mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
                                } catch (Exception e) {
                                    try {
                                        operatorNumeric = this.mSS.getOperatorNumeric();
                                        mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
                                    } catch (Exception ex3) {
                                        loge("handlePollStateResultMessage: bad mcc operatorNumeric=" + operatorNumeric + " ex=" + ex3);
                                        operatorNumeric = "";
                                        mcc = Integer.MAX_VALUE;
                                    }
                                }
                                try {
                                    mnc = Integer.parseInt(operatorNumeric.substring(3));
                                } catch (Exception e2) {
                                    loge("handlePollStateResultMessage: bad mnc operatorNumeric=" + operatorNumeric + " e=" + e2);
                                    mnc = Integer.MAX_VALUE;
                                }
                                int tac = Integer.decode(states2[1]).intValue();
                                int eci = Integer.decode(states2[2]).intValue();
                                this.mNewCellIdentityLte = new CellIdentityLte(mcc, mnc, eci, Integer.MAX_VALUE, tac);
                                log("handlPollStateResultMessage: mNewCellIdentityLte =" + this.mNewCellIdentityLte.toString());
                            }
                            log("handlPollStateResultMessage: GsmSST setDataRegState=" + dataRegState + " regState=" + regState2 + " dataRadioTechnology=" + type2);
                            break;
                        case 6:
                            String[] opNames = (String[]) ar.result;
                            if (opNames != null && opNames.length >= 3) {
                                String brandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                                if (brandOverride != null) {
                                    log("EVENT_POLL_STATE_OPERATOR: use brandOverride=" + brandOverride);
                                    this.mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                                } else {
                                    this.mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                                }
                            }
                            break;
                        case 14:
                            int[] ints = (int[]) ar.result;
                            this.mNewSS.setIsManualSelection(ints[0] == 1);
                            if (ints[0] == 1 && !this.mPhone.isManualNetSelAllowed()) {
                                this.mPhone.setNetworkSelectionModeAutomatic(null);
                                log(" Forcing Automatic Network Selection, manual selection is not allowed");
                            }
                            break;
                    }
                } catch (RuntimeException ex4) {
                    loge("Exception while polling service state. Probably malformed RIL response." + ex4);
                }
            }
            this.mPollingContext[0] = r2[0] - 1;
            if (this.mPollingContext[0] == 0) {
                boolean roaming = this.mGsmRoaming || this.mDataRoaming;
                if (this.mGsmRoaming && !isOperatorConsideredRoaming(this.mNewSS) && (isSameNamedOperators(this.mNewSS) || isOperatorConsideredNonRoaming(this.mNewSS))) {
                    roaming = false;
                }
                if (this.mPhone.isMccMncMarkedAsNonRoaming(this.mNewSS.getOperatorNumeric())) {
                    roaming = false;
                } else if (this.mPhone.isMccMncMarkedAsRoaming(this.mNewSS.getOperatorNumeric())) {
                    roaming = true;
                }
                this.mNewSS.setVoiceRoaming(roaming);
                this.mNewSS.setDataRoaming(roaming);
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
                pollStateDone();
            }
        }
    }

    @Override
    protected void setRoamingType(ServiceState currentServiceState) {
        boolean isVoiceInService = currentServiceState.getVoiceRegState() == 0;
        if (isVoiceInService) {
            if (currentServiceState.getVoiceRoaming()) {
                if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                    currentServiceState.setVoiceRoamingType(2);
                } else {
                    currentServiceState.setVoiceRoamingType(3);
                }
            } else {
                currentServiceState.setVoiceRoamingType(0);
            }
        }
        boolean isDataInService = currentServiceState.getDataRegState() == 0;
        int dataRegType = currentServiceState.getRilDataRadioTechnology();
        if (isDataInService) {
            if (!currentServiceState.getDataRoaming()) {
                currentServiceState.setDataRoamingType(0);
                return;
            }
            if (ServiceState.isGsm(dataRegType)) {
                if (isVoiceInService) {
                    currentServiceState.setDataRoamingType(currentServiceState.getVoiceRoamingType());
                    return;
                } else {
                    currentServiceState.setDataRoamingType(1);
                    return;
                }
            }
            currentServiceState.setDataRoamingType(1);
        }
    }

    private void setSignalStrengthDefaultValues() {
        this.mSignalStrength = new SignalStrength(true);
    }

    @Override
    public void pollState() {
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        switch (this.mCi.getRadioState()) {
            case RADIO_UNAVAILABLE:
                this.mNewSS.setStateOutOfService();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = false;
                this.mNitzUpdatedTime = false;
                pollStateDone();
                break;
            case RADIO_OFF:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = false;
                this.mNitzUpdatedTime = false;
                pollStateDone();
                break;
            default:
                int[] iArr = this.mPollingContext;
                iArr[0] = iArr[0] + 1;
                this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
                int[] iArr2 = this.mPollingContext;
                iArr2[0] = iArr2[0] + 1;
                this.mCi.getDataRegistrationState(obtainMessage(5, this.mPollingContext));
                int[] iArr3 = this.mPollingContext;
                iArr3[0] = iArr3[0] + 1;
                this.mCi.getVoiceRegistrationState(obtainMessage(4, this.mPollingContext));
                int[] iArr4 = this.mPollingContext;
                iArr4[0] = iArr4[0] + 1;
                this.mCi.getNetworkSelectionMode(obtainMessage(14, this.mPollingContext));
                break;
        }
    }

    private void pollStateDone() {
        log("Poll ServiceState done:  oldSS=[" + this.mSS + "] newSS=[" + this.mNewSS + "] oldMaxDataCalls=" + this.mMaxDataCalls + " mNewMaxDataCalls=" + this.mNewMaxDataCalls + " oldReasonDataDenied=" + this.mReasonDataDenied + " mNewReasonDataDenied=" + this.mNewReasonDataDenied);
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("telephony.test.forceRoaming", false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        boolean hasRegistered = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        boolean hasDeregistered = this.mSS.getVoiceRegState() == 0 && this.mNewSS.getVoiceRegState() != 0;
        boolean hasGprsAttached = this.mSS.getDataRegState() != 0 && this.mNewSS.getDataRegState() == 0;
        boolean hasGprsDetached = this.mSS.getDataRegState() == 0 && this.mNewSS.getDataRegState() != 0;
        boolean hasDataRegStateChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState();
        boolean hasVoiceRegStateChanged = this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState();
        boolean hasRilVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology();
        boolean hasRilDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology();
        boolean hasChanged = !this.mNewSS.equals(this.mSS);
        boolean hasVoiceRoamingOn = !this.mSS.getVoiceRoaming() && this.mNewSS.getVoiceRoaming();
        boolean hasVoiceRoamingOff = this.mSS.getVoiceRoaming() && !this.mNewSS.getVoiceRoaming();
        boolean hasDataRoamingOn = !this.mSS.getDataRoaming() && this.mNewSS.getDataRoaming();
        boolean hasDataRoamingOff = this.mSS.getDataRoaming() && !this.mNewSS.getDataRoaming();
        boolean hasLocationChanged = !this.mNewCellLoc.equals(this.mCellLoc);
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (hasVoiceRegStateChanged || hasDataRegStateChanged) {
            EventLog.writeEvent(EventLogTags.GSM_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        if (hasRilVoiceRadioTechnologyChanged) {
            GsmCellLocation loc = this.mNewCellLoc;
            int cid = loc != null ? loc.getCid() : -1;
            EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED_NEW, Integer.valueOf(cid), Integer.valueOf(this.mSS.getRilVoiceRadioTechnology()), Integer.valueOf(this.mNewSS.getRilVoiceRadioTechnology()));
            log("RAT switched " + ServiceState.rilRadioTechnologyToString(this.mSS.getRilVoiceRadioTechnology()) + " -> " + ServiceState.rilRadioTechnologyToString(this.mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        GsmCellLocation tcl = this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        this.mReasonDataDenied = this.mNewReasonDataDenied;
        this.mMaxDataCalls = this.mNewMaxDataCalls;
        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilVoiceRadioTechnology());
        }
        if (hasRegistered) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
            log("pollStateDone: registering current mNitzUpdatedTime=" + this.mNitzUpdatedTime + " changing to false");
            this.mNitzUpdatedTime = false;
        }
        if (hasChanged) {
            updateSpnDisplay();
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlphaLong());
            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
            if (operatorNumeric == null) {
                log("operatorNumeric is null");
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), "");
                this.mGotCountryCode = false;
                this.mNitzUpdatedTime = false;
            } else {
                String iso = "";
                String mcc = "";
                try {
                    mcc = operatorNumeric.substring(0, 3);
                    iso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
                } catch (NumberFormatException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex2) {
                    loge("pollStateDone: countryCodeForMcc error" + ex2);
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), iso);
                this.mGotCountryCode = true;
                TimeZone zone = null;
                if (!this.mNitzUpdatedTime && !mcc.equals("000") && !TextUtils.isEmpty(iso) && getAutoTimeZone()) {
                    boolean testOneUniqueOffsetPath = SystemProperties.getBoolean("telephony.test.ignore.nitz", false) && (SystemClock.uptimeMillis() & 1) == 0;
                    ArrayList<TimeZone> uniqueZones = TimeUtils.getTimeZonesWithUniqueOffsets(iso);
                    if (uniqueZones.size() == 1 || testOneUniqueOffsetPath) {
                        zone = uniqueZones.get(0);
                        log("pollStateDone: no nitz but one TZ for iso-cc=" + iso + " with zone.getID=" + zone.getID() + " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath);
                        setAndBroadcastNetworkSetTimeZone(zone.getID());
                    } else {
                        log("pollStateDone: there are " + uniqueZones.size() + " unique offsets for iso-cc='" + iso + " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath + "', do nothing");
                    }
                }
                if (shouldFixTimeZoneNow(this.mPhone, operatorNumeric, prevOperatorNumeric, this.mNeedFixZoneAfterNitz)) {
                    String zoneName = SystemProperties.get("persist.sys.timezone");
                    log("pollStateDone: fix time zone zoneName='" + zoneName + "' mZoneOffset=" + this.mZoneOffset + " mZoneDst=" + this.mZoneDst + " iso-cc='" + iso + "' iso-cc-idx=" + Arrays.binarySearch(GMT_COUNTRY_CODES, iso));
                    if ("".equals(iso) && this.mNeedFixZoneAfterNitz) {
                        zone = getNitzTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime);
                        log("pollStateDone: using NITZ TimeZone");
                    } else if (this.mZoneOffset == 0 && !this.mZoneDst && zoneName != null && zoneName.length() > 0 && Arrays.binarySearch(GMT_COUNTRY_CODES, iso) < 0) {
                        if (this.mNeedFixZoneAfterNitz) {
                            zone = TimeZone.getDefault();
                            long ctm = System.currentTimeMillis();
                            long tzOffset = zone.getOffset(ctm);
                            log("pollStateDone: tzOffset=" + tzOffset + " ltod=" + TimeUtils.logTimeOfDay(ctm));
                            if (getAutoTime()) {
                                long adj = ctm - tzOffset;
                                log("pollStateDone: adj ltod=" + TimeUtils.logTimeOfDay(adj));
                                setAndBroadcastNetworkSetTime(adj);
                            } else {
                                this.mSavedTime -= tzOffset;
                            }
                        }
                        log("pollStateDone: using default TimeZone");
                    } else {
                        zone = TimeUtils.getTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime, iso);
                        log("pollStateDone: using getTimeZone(off, dst, time, iso)");
                    }
                    this.mNeedFixZoneAfterNitz = false;
                    if (zone != null) {
                        log("pollStateDone: zone != null zone.getID=" + zone.getID());
                        if (getAutoTimeZone()) {
                            setAndBroadcastNetworkSetTimeZone(zone.getID());
                        }
                        saveNitzTimeZone(zone.getID());
                    } else {
                        log("pollStateDone: zone == null");
                    }
                }
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), this.mSS.getVoiceRoaming());
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            this.mPhone.notifyServiceStateChanged(this.mSS);
        }
        if (hasGprsAttached) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasGprsDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasDataRegStateChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            this.mPhone.notifyDataConnection(null);
        }
        if (hasVoiceRoamingOn) {
            this.mVoiceRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasVoiceRoamingOff) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOn) {
            this.mDataRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOff) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (hasLocationChanged) {
            this.mPhone.notifyLocationChanged();
        }
        if (!isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
            if (!this.mStartedGprsRegCheck && !this.mReportedGprsNoReg) {
                this.mStartedGprsRegCheck = true;
                int check_period = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "gprs_register_check_period_ms", ServiceStateTracker.DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
                sendMessageDelayed(obtainMessage(22), check_period);
            }
        } else {
            this.mReportedGprsNoReg = false;
        }
        ArrayList<CellInfo> arrayCi = new ArrayList<>();
        synchronized (this.mCellInfo) {
            CellInfoLte cil = (CellInfoLte) this.mCellInfo;
            boolean cidChanged = !this.mNewCellIdentityLte.equals(this.mLasteCellIdentityLte);
            if (hasRegistered || hasDeregistered || cidChanged) {
                long jElapsedRealtime = SystemClock.elapsedRealtime() * 1000;
                boolean registered = this.mSS.getVoiceRegState() == 0;
                this.mLasteCellIdentityLte = this.mNewCellIdentityLte;
                cil.setRegistered(registered);
                cil.setCellIdentity(this.mLasteCellIdentityLte);
                log("pollStateDone: hasRegistered=" + hasRegistered + " hasDeregistered=" + hasDeregistered + " cidChanged=" + cidChanged + " mCellInfo=" + this.mCellInfo);
                arrayCi.add(this.mCellInfo);
                this.mPhoneBase.notifyCellInfo(arrayCi);
            }
        }
    }

    private boolean isGprsConsistent(int dataRegState, int voiceRegState) {
        return voiceRegState != 0 || dataRegState == 0;
    }

    private TimeZone getNitzTimeZone(int offset, boolean dst, long when) {
        TimeZone guess = findTimeZone(offset, dst, when);
        if (guess == null) {
            guess = findTimeZone(offset, !dst, when);
        }
        log("getNitzTimeZone returning " + (guess == null ? guess : guess.getID()));
        return guess;
    }

    private TimeZone findTimeZone(int offset, boolean dst, long when) {
        int rawOffset = offset;
        if (dst) {
            rawOffset -= 3600000;
        }
        String[] zones = TimeZone.getAvailableIDs(rawOffset);
        Date d = new Date(when);
        for (String zone : zones) {
            TimeZone tz = TimeZone.getTimeZone(zone);
            if (tz.getOffset(when) == offset && tz.inDaylightTime(d) == dst) {
                return tz;
            }
        }
        return null;
    }

    private void queueNextSignalStrengthPoll() {
        if (!this.mDontPollSignalStrength) {
            Message msg = obtainMessage();
            msg.what = 10;
            sendMessageDelayed(msg, 20000L);
        }
    }

    private void onRestrictedStateChanged(AsyncResult ar) {
        RestrictedState newRs = new RestrictedState();
        log("onRestrictedStateChanged: E rs " + this.mRestrictedState);
        if (ar.exception == null) {
            int[] ints = (int[]) ar.result;
            int state = ints[0];
            newRs.setCsEmergencyRestricted(((state & 1) == 0 && (state & 4) == 0) ? false : true);
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                newRs.setCsNormalRestricted(((state & 2) == 0 && (state & 4) == 0) ? false : true);
                newRs.setPsRestricted((state & 16) != 0);
            }
            log("onRestrictedStateChanged: new rs " + newRs);
            if (!this.mRestrictedState.isPsRestricted() && newRs.isPsRestricted()) {
                this.mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(1001);
            } else if (this.mRestrictedState.isPsRestricted() && !newRs.isPsRestricted()) {
                this.mPsRestrictDisabledRegistrants.notifyRegistrants();
                setNotification(1002);
            }
            if (this.mRestrictedState.isCsRestricted()) {
                if (!newRs.isCsRestricted()) {
                    setNotification(1004);
                } else if (!newRs.isCsNormalRestricted()) {
                    setNotification(1006);
                } else if (!newRs.isCsEmergencyRestricted()) {
                    setNotification(1005);
                }
            } else if (this.mRestrictedState.isCsEmergencyRestricted() && !this.mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    setNotification(1004);
                } else if (newRs.isCsRestricted()) {
                    setNotification(1003);
                } else if (newRs.isCsNormalRestricted()) {
                    setNotification(1005);
                }
            } else if (!this.mRestrictedState.isCsEmergencyRestricted() && this.mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    setNotification(1004);
                } else if (newRs.isCsRestricted()) {
                    setNotification(1003);
                } else if (newRs.isCsEmergencyRestricted()) {
                    setNotification(1006);
                }
            } else if (newRs.isCsRestricted()) {
                setNotification(1003);
            } else if (newRs.isCsEmergencyRestricted()) {
                setNotification(1006);
            } else if (newRs.isCsNormalRestricted()) {
                setNotification(1005);
            }
            this.mRestrictedState = newRs;
        }
        log("onRestrictedStateChanged: X rs " + this.mRestrictedState);
    }

    private int dataRegCodeToServiceState(int code) {
        switch (code) {
            case 0:
            case 2:
            case 3:
            case 4:
                break;
            case 1:
                break;
            case 5:
                break;
            case 6:
            case 7:
            case 9:
            case 10:
            default:
                loge("dataRegCodeToServiceState: unexpected service state " + code);
                break;
            case 8:
            case 11:
                break;
        }
        return 1;
    }

    private int regCodeToServiceState(int code) {
        switch (code) {
            case 0:
            case 2:
            case 3:
            case 4:
            case 12:
            case 13:
            case 14:
            case 20:
                break;
            case 1:
            case 9:
                break;
            case 5:
            case 10:
                break;
            case 6:
            case 7:
            case 8:
            case 11:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            default:
                loge("regCodeToServiceState: unexpected service state " + code);
                break;
        }
        return 1;
    }

    private boolean regCodeIsRoaming(int code) {
        return 5 == code || 7 == code || 10 == code;
    }

    private boolean isSameNamedOperators(ServiceState s) {
        String spn = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNameForPhone(getPhoneId());
        String onsl = s.getOperatorAlphaLong();
        String onss = s.getOperatorAlphaShort();
        boolean equalsOnsl = onsl != null && spn.equals(onsl);
        boolean equalsOnss = onss != null && spn.equals(onss);
        return currentMccEqualsSimMcc(s) && (equalsOnsl || equalsOnss);
    }

    private boolean currentMccEqualsSimMcc(ServiceState s) {
        String simNumeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(getPhoneId());
        String operatorNumeric = s.getOperatorNumeric();
        try {
            boolean equalsMcc = simNumeric.substring(0, 3).equals(operatorNumeric.substring(0, 3));
            return equalsMcc;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isOperatorConsideredNonRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String[] numericArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_clockTickVibePattern);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOperatorConsideredRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String[] numericArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_companionDeviceCerts);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCurrentDataConnectionState() {
        return this.mSS.getDataRegState();
    }

    @Override
    public boolean isConcurrentVoiceAndDataAllowed() {
        return this.mSS.getRilVoiceRadioTechnology() >= 3 || this.mSS.getRilDataRadioTechnology() >= 3;
    }

    public CellLocation getCellLocation() {
        if (this.mCellLoc.getLac() >= 0 && this.mCellLoc.getCid() >= 0) {
            log("getCellLocation(): X good mCellLoc=" + this.mCellLoc);
            return this.mCellLoc;
        }
        List<CellInfo> result = getAllCellInfo();
        if (result != null) {
            GsmCellLocation cellLocOther = new GsmCellLocation();
            for (CellInfo ci : result) {
                if (ci instanceof CellInfoGsm) {
                    CellInfoGsm cellInfoGsm = (CellInfoGsm) ci;
                    CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
                    cellLocOther.setLacAndCid(cellIdentityGsm.getLac(), cellIdentityGsm.getCid());
                    cellLocOther.setPsc(cellIdentityGsm.getPsc());
                    log("getCellLocation(): X ret GSM info=" + cellLocOther);
                    return cellLocOther;
                }
                if (ci instanceof CellInfoWcdma) {
                    CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) ci;
                    CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
                    cellLocOther.setLacAndCid(cellIdentityWcdma.getLac(), cellIdentityWcdma.getCid());
                    cellLocOther.setPsc(cellIdentityWcdma.getPsc());
                    log("getCellLocation(): X ret WCDMA info=" + cellLocOther);
                    return cellLocOther;
                }
                if ((ci instanceof CellInfoLte) && (cellLocOther.getLac() < 0 || cellLocOther.getCid() < 0)) {
                    CellInfoLte cellInfoLte = (CellInfoLte) ci;
                    CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                    if (cellIdentityLte.getTac() != Integer.MAX_VALUE && cellIdentityLte.getCi() != Integer.MAX_VALUE) {
                        cellLocOther.setLacAndCid(cellIdentityLte.getTac(), cellIdentityLte.getCi());
                        cellLocOther.setPsc(0);
                        log("getCellLocation(): possible LTE cellLocOther=" + cellLocOther);
                    }
                }
            }
            log("getCellLocation(): X ret best answer cellLocOther=" + cellLocOther);
            return cellLocOther;
        }
        log("getCellLocation(): X empty mCellLoc and CellInfo mCellLoc=" + this.mCellLoc);
        return this.mCellLoc;
    }

    private void setTimeFromNITZString(String nitz, long nitzReceiveTime) {
        long start = SystemClock.elapsedRealtime();
        log("NITZ: " + nitz + "," + nitzReceiveTime + " start=" + start + " delay=" + (start - nitzReceiveTime));
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.clear();
            c.set(16, 0);
            String[] nitzSubs = nitz.split("[/:,+-]");
            int year = Integer.parseInt(nitzSubs[0]) + 2000;
            if (year > MAX_NITZ_YEAR) {
                loge("NITZ year: " + year + " exceeds limit, skip NITZ time update");
                return;
            }
            c.set(1, year);
            int month = Integer.parseInt(nitzSubs[1]) - 1;
            c.set(2, month);
            int date = Integer.parseInt(nitzSubs[2]);
            c.set(5, date);
            int hour = Integer.parseInt(nitzSubs[3]);
            c.set(10, hour);
            int minute = Integer.parseInt(nitzSubs[4]);
            c.set(12, minute);
            int second = Integer.parseInt(nitzSubs[5]);
            c.set(13, second);
            boolean sign = nitz.indexOf(45) == -1;
            int tzOffset = Integer.parseInt(nitzSubs[6]);
            int dst = nitzSubs.length >= 8 ? Integer.parseInt(nitzSubs[7]) : 0;
            int tzOffset2 = (sign ? 1 : -1) * tzOffset * 15 * 60 * 1000;
            TimeZone zone = null;
            if (nitzSubs.length >= 9) {
                String tzname = nitzSubs[8].replace('!', '/');
                zone = TimeZone.getTimeZone(tzname);
            }
            String iso = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
            if (zone == null && this.mGotCountryCode) {
                if (iso != null && iso.length() > 0) {
                    zone = TimeUtils.getTimeZone(tzOffset2, dst != 0, c.getTimeInMillis(), iso);
                } else {
                    zone = getNitzTimeZone(tzOffset2, dst != 0, c.getTimeInMillis());
                }
            }
            if (zone == null) {
                zone = getNitzTimeZone(tzOffset2, dst != 0, c.getTimeInMillis());
            }
            if (zone != null && this.mZoneOffset == tzOffset2) {
                if (this.mZoneDst != (dst != 0)) {
                }
            } else {
                this.mNeedFixZoneAfterNitz = true;
                this.mZoneOffset = tzOffset2;
                this.mZoneDst = dst != 0;
                this.mZoneTime = c.getTimeInMillis();
            }
            if (zone != null) {
                if (getAutoTimeZone()) {
                    setAndBroadcastNetworkSetTimeZone(zone.getID());
                }
                saveNitzTimeZone(zone.getID());
            }
            String ignore = SystemProperties.get("gsm.ignore-nitz");
            if (ignore != null && ignore.equals("yes")) {
                log("NITZ: Not setting clock because gsm.ignore-nitz is set");
                return;
            }
            try {
                this.mWakeLock.acquire();
                if (getAutoTime()) {
                    long millisSinceNitzReceived = SystemClock.elapsedRealtime() - nitzReceiveTime;
                    if (millisSinceNitzReceived < 0) {
                        log("NITZ: not setting time, clock has rolled backwards since NITZ time was received, " + nitz);
                        return;
                    }
                    if (millisSinceNitzReceived > 2147483647L) {
                        log("NITZ: not setting time, processing has taken " + (millisSinceNitzReceived / 86400000) + " days");
                        return;
                    }
                    c.add(14, (int) millisSinceNitzReceived);
                    long timeGained = c.getTimeInMillis() - System.currentTimeMillis();
                    log("NITZ: Setting time of day to " + c.getTime() + " NITZ receive delay(ms): " + millisSinceNitzReceived + " gained(ms): " + timeGained + " from " + nitz);
                    if (timeGained > 2000 || timeGained < -2000) {
                        setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                        Rlog.i(LOG_TAG, "NITZ: after Setting time of day");
                    }
                }
                SystemProperties.set("gsm.nitz.time", String.valueOf(c.getTimeInMillis()));
                saveNitzTime(c.getTimeInMillis());
                this.mNitzUpdatedTime = true;
                this.mWakeLock.release();
            } finally {
                this.mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            loge("NITZ: Parsing NITZ time " + nitz + " ex=" + ex);
        }
    }

    private boolean getAutoTime() {
        try {
            return Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "auto_time") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    private boolean getAutoTimeZone() {
        try {
            return Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "auto_time_zone") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    private void saveNitzTimeZone(String zoneId) {
        this.mSavedTimeZone = zoneId;
    }

    private void saveNitzTime(long time) {
        this.mSavedTime = time;
        this.mSavedAtTime = SystemClock.elapsedRealtime();
    }

    private void setAndBroadcastNetworkSetTimeZone(String zoneId) {
        log("setAndBroadcastNetworkSetTimeZone: setTimeZone=" + zoneId);
        AlarmManager alarm = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        alarm.setTimeZone(zoneId);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIMEZONE");
        intent.addFlags(536870912);
        intent.putExtra("time-zone", zoneId);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        log("setAndBroadcastNetworkSetTimeZone: call alarm.setTimeZone and broadcast zoneId=" + zoneId);
    }

    private void setAndBroadcastNetworkSetTime(long time) {
        log("setAndBroadcastNetworkSetTime: time=" + time + "ms");
        SystemClock.setCurrentTimeMillis(time);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIME");
        intent.addFlags(536870912);
        intent.putExtra("time", time);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void revertToNitzTime() {
        if (Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "auto_time", 0) != 0) {
            log("Reverting to NITZ Time: mSavedTime=" + this.mSavedTime + " mSavedAtTime=" + this.mSavedAtTime);
            if (this.mSavedTime != 0 && this.mSavedAtTime != 0) {
                setAndBroadcastNetworkSetTime(this.mSavedTime + (SystemClock.elapsedRealtime() - this.mSavedAtTime));
            }
        }
    }

    private void revertToNitzTimeZone() {
        if (Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "auto_time_zone", 0) != 0) {
            log("Reverting to NITZ TimeZone: tz='" + this.mSavedTimeZone);
            if (this.mSavedTimeZone != null) {
                setAndBroadcastNetworkSetTimeZone(this.mSavedTimeZone);
            }
        }
    }

    public int getIntFromBytes(char[] charArray) {
        int ret = 0 | (charArray[3] & 255);
        return (((((ret << 8) | (charArray[2] & 255)) << 8) | (charArray[1] & 255)) << 8) | (charArray[0] & 255);
    }

    private void setNotification(int notifyType) {
        boolean found;
        String line;
        NotificationManager notificationManager;
        log("setNotification: create notification " + notifyType);
        boolean isSetNotification = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.fadedHeight);
        if (!isSetNotification) {
            log("Ignore all the notifications");
            return;
        }
        Context context = this.mPhone.getContext();
        this.mNotification = new Notification();
        this.mNotification.when = System.currentTimeMillis();
        this.mNotification.flags = 16;
        this.mNotification.icon = R.drawable.stat_sys_warning;
        Intent intent = new Intent();
        this.mNotification.contentIntent = PendingIntent.getActivity(context, 0, intent, 268435456);
        CharSequence details = "";
        CharSequence title = context.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_IN_PROGRESS);
        int notificationId = CS_NOTIFICATION;
        switch (notifyType) {
            case 1001:
                long dataSubId = SubscriptionManager.getDefaultDataSubId();
                if (dataSubId == this.mPhone.getSubId()) {
                    notificationId = PS_NOTIFICATION;
                    details = context.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_ENTRY);
                    log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                    this.mNotification.tickerText = title;
                    this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                    this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                    notificationManager = (NotificationManager) context.getSystemService("notification");
                    if (notifyType == 1002 || notifyType == 1004) {
                        notificationManager.cancel(notificationId);
                    } else {
                        notificationManager.notify(notificationId, this.mNotification);
                    }
                    return;
                }
                return;
            case 1002:
                notificationId = PS_NOTIFICATION;
                log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                this.mNotification.tickerText = title;
                this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                notificationManager = (NotificationManager) context.getSystemService("notification");
                if (notifyType == 1002) {
                    notificationManager.cancel(notificationId);
                }
                return;
            case 1003:
                details = context.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_SUCCESS);
                log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                this.mNotification.tickerText = title;
                this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                notificationManager = (NotificationManager) context.getSystemService("notification");
                if (notifyType == 1002) {
                }
                return;
            case 1004:
            default:
                log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                this.mNotification.tickerText = title;
                this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                notificationManager = (NotificationManager) context.getSystemService("notification");
                if (notifyType == 1002) {
                }
                return;
            case 1005:
                details = context.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_IN_PROGRESS);
                log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                this.mNotification.tickerText = title;
                this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                notificationManager = (NotificationManager) context.getSystemService("notification");
                if (notifyType == 1002) {
                }
                return;
            case 1006:
                details = context.getText(R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_ERROR);
                log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                this.mNotification.tickerText = title;
                this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                notificationManager = (NotificationManager) context.getSystemService("notification");
                if (notifyType == 1002) {
                }
                return;
            case 1007:
                title = "CP ASSERT!";
                String nvm_dir = System.getenv("NVM_ROOT_DIR");
                if (nvm_dir != null) {
                    File mNVMFile = new File(nvm_dir, "EEHandlerConfig_Linux.nvm");
                    if (mNVMFile.exists()) {
                        try {
                            BufferedReader mBuf = new BufferedReader(new InputStreamReader(new FileInputStream(mNVMFile), "ISO-8859-1"), 1024);
                            char[] header = new char[264];
                            try {
                                try {
                                    int readlen = mBuf.read(header, 0, 264);
                                    if (readlen == 264) {
                                        int structSize = getIntFromBytes(header);
                                        log("structSize is " + structSize);
                                        if (structSize <= 0 || 1 <= 0) {
                                            log("NVM Struct empty!");
                                            try {
                                                mBuf.close();
                                            } catch (IOException ex) {
                                                log("NVM File close ex = " + ex);
                                            }
                                        } else {
                                            char[] NVMstruct = new char[structSize * 1];
                                            int readlen2 = mBuf.read(NVMstruct, 0, structSize * 1);
                                            if (readlen2 != structSize * 1) {
                                                log("NVM Struct read error!");
                                                try {
                                                    mBuf.close();
                                                } catch (IOException ex2) {
                                                    log("NVM File close ex = " + ex2);
                                                }
                                            } else {
                                                char finalAction = NVMstruct[6];
                                                if (finalAction != 1) {
                                                    log("Final action is not STALL!");
                                                    try {
                                                        mBuf.close();
                                                    } catch (IOException ex3) {
                                                        log("NVM File close ex = " + ex3);
                                                    }
                                                } else {
                                                    try {
                                                        mBuf.close();
                                                    } catch (IOException ex4) {
                                                        log("NVM File close ex = " + ex4);
                                                    }
                                                    File mAssertFile = new File(nvm_dir, "CpErrorStatistic.log");
                                                    if (mAssertFile.exists()) {
                                                        try {
                                                            mBuf = new BufferedReader(new InputStreamReader(new FileInputStream(mAssertFile)), 1024);
                                                            found = false;
                                                        } catch (FileNotFoundException e) {
                                                            details = "No details, Please dump ACAT log.";
                                                        }
                                                        while (true) {
                                                            try {
                                                                line = mBuf.readLine();
                                                            } catch (IOException e2) {
                                                                details = "No details, Please dump ACAT log.";
                                                                try {
                                                                    mBuf.close();
                                                                } catch (IOException ex5) {
                                                                    log("Err log File close ex = " + ex5);
                                                                }
                                                            } catch (Throwable th) {
                                                                try {
                                                                    mBuf.close();
                                                                    break;
                                                                } catch (IOException ex6) {
                                                                    log("Err log File close ex = " + ex6);
                                                                }
                                                                throw th;
                                                            }
                                                            if (line != null) {
                                                                log("setNotification: CpErrorStatistic.log line =  " + line);
                                                                int index = line.indexOf("non silent CP Assert, Cause:");
                                                                if (index >= 0) {
                                                                    found = true;
                                                                    details = line.substring(index + 28);
                                                                    break;
                                                                }
                                                                log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                                                                this.mNotification.tickerText = title;
                                                                this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                                                                this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                                                                notificationManager = (NotificationManager) context.getSystemService("notification");
                                                                if (notifyType == 1002) {
                                                                }
                                                            }
                                                            break;
                                                        }
                                                        if (found) {
                                                            try {
                                                                mBuf.close();
                                                            } catch (IOException ex7) {
                                                                log("Err log File close ex = " + ex7);
                                                            }
                                                            log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                                                            this.mNotification.tickerText = title;
                                                            this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                                                            this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                                                            notificationManager = (NotificationManager) context.getSystemService("notification");
                                                            if (notifyType == 1002) {
                                                            }
                                                        } else {
                                                            try {
                                                                mBuf.close();
                                                            } catch (IOException ex8) {
                                                                log("Err log File close ex = " + ex8);
                                                            }
                                                        }
                                                        break;
                                                    } else if (new File(nvm_dir, "AppErrorStatistic.log").exists()) {
                                                        log("It's due to telephony module exception");
                                                    } else {
                                                        details = "No details, Please dump ACAT log.";
                                                        log("setNotification: put notification " + ((Object) title) + " / " + ((Object) details));
                                                        this.mNotification.tickerText = title;
                                                        this.mNotification.color = context.getResources().getColor(R.color.system_accent3_600);
                                                        this.mNotification.setLatestEventInfo(context, title, details, this.mNotification.contentIntent);
                                                        notificationManager = (NotificationManager) context.getSystemService("notification");
                                                        if (notifyType == 1002) {
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    } else {
                                        log("NVM header read error!");
                                        try {
                                            break;
                                        } catch (IOException ex9) {
                                        }
                                    }
                                    return;
                                } catch (IOException ex10) {
                                    log("NVM File read ex = " + ex10);
                                    try {
                                        mBuf.close();
                                        return;
                                    } catch (IOException ex11) {
                                        log("NVM File close ex = " + ex11);
                                        return;
                                    }
                                }
                            } finally {
                                try {
                                    mBuf.close();
                                    break;
                                } catch (IOException ex92) {
                                    log("NVM File close ex = " + ex92);
                                }
                            }
                        } catch (FileNotFoundException e3) {
                            return;
                        } catch (UnsupportedEncodingException e4) {
                            return;
                        }
                    }
                    return;
                }
                return;
        }
    }

    private UiccCardApplication getUiccCardApplication() {
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
    }

    @Override
    protected void onUpdateIccAvailability() {
        UiccCardApplication newUiccApplication;
        if (this.mUiccController != null && this.mUiccApplcation != (newUiccApplication = getUiccCardApplication())) {
            if (this.mUiccApplcation != null) {
                log("Removing stale icc objects.");
                this.mUiccApplcation.unregisterForReady(this);
                if (this.mIccRecords != null) {
                    this.mIccRecords.unregisterForRecordsLoaded(this);
                }
                this.mIccRecords = null;
                this.mUiccApplcation = null;
            }
            if (newUiccApplication != null) {
                log("New card found");
                this.mUiccApplcation = newUiccApplication;
                this.mIccRecords = this.mUiccApplcation.getIccRecords();
                this.mUiccApplcation.registerForReady(this, 17, null);
                if (this.mIccRecords != null) {
                    this.mIccRecords.registerForRecordsLoaded(this, 16, null);
                }
            }
        }
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[GsmSST] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[GsmSST] " + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmServiceStateTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mPhone=" + this.mPhone);
        pw.println(" mSS=" + this.mSS);
        pw.println(" mNewSS=" + this.mNewSS);
        pw.println(" mCellLoc=" + this.mCellLoc);
        pw.println(" mNewCellLoc=" + this.mNewCellLoc);
        pw.println(" mPreferredNetworkType=" + this.mPreferredNetworkType);
        pw.println(" mMaxDataCalls=" + this.mMaxDataCalls);
        pw.println(" mNewMaxDataCalls=" + this.mNewMaxDataCalls);
        pw.println(" mReasonDataDenied=" + this.mReasonDataDenied);
        pw.println(" mNewReasonDataDenied=" + this.mNewReasonDataDenied);
        pw.println(" mGsmRoaming=" + this.mGsmRoaming);
        pw.println(" mDataRoaming=" + this.mDataRoaming);
        pw.println(" mEmergencyOnly=" + this.mEmergencyOnly);
        pw.println(" mNeedFixZoneAfterNitz=" + this.mNeedFixZoneAfterNitz);
        pw.flush();
        pw.println(" mZoneOffset=" + this.mZoneOffset);
        pw.println(" mZoneDst=" + this.mZoneDst);
        pw.println(" mZoneTime=" + this.mZoneTime);
        pw.println(" mGotCountryCode=" + this.mGotCountryCode);
        pw.println(" mNitzUpdatedTime=" + this.mNitzUpdatedTime);
        pw.println(" mSavedTimeZone=" + this.mSavedTimeZone);
        pw.println(" mSavedTime=" + this.mSavedTime);
        pw.println(" mSavedAtTime=" + this.mSavedAtTime);
        pw.println(" mStartedGprsRegCheck=" + this.mStartedGprsRegCheck);
        pw.println(" mReportedGprsNoReg=" + this.mReportedGprsNoReg);
        pw.println(" mNotification=" + this.mNotification);
        pw.println(" mWakeLock=" + this.mWakeLock);
        pw.println(" mCurSpn=" + this.mCurSpn);
        pw.println(" mCurShowSpn=" + this.mCurShowSpn);
        pw.println(" mCurPlmn=" + this.mCurPlmn);
        pw.println(" mCurShowPlmn=" + this.mCurShowPlmn);
        pw.flush();
    }

    private int getMasterSimId() {
        boolean isSim2Master = SystemProperties.getBoolean("persist.sys.sim2.master.enable", false);
        return isSim2Master ? 1 : 0;
    }

    private boolean turnOffIms() {
        int phoneId = this.mPhoneBase.getPhoneId();
        if (phoneId == getMasterSimId()) {
            final ImsManager imsManager = ImsManager.getInstance(this.mPhoneBase.getContext(), phoneId);
            log("turnOffIms phoneId=" + phoneId);
            if (imsManager != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            imsManager.turnOffIms();
                        } catch (ImsException ex) {
                            GsmServiceStateTracker.this.log("turnOffIms ex=" + ex);
                        }
                    }
                }).start();
                return true;
            }
            log("turnOffIms IMS Manager is null");
            return false;
        }
        log("only support IMS on master SIM, this ImsSMSDispatcher is for " + this.mPhoneBase.getSubId());
        return false;
    }

    @Override
    public void powerOffRadioSafely(DcTrackerBase dcTracker) {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                SubscriptionManager.getDefaultDataSubId();
                Phone imsPhone = this.mPhoneBase.getImsPhone();
                if (imsPhone != null && imsPhone.getServiceState().getState() == 0) {
                    log(" IMS is registered now, wait it deregister, then cleanup connection");
                    ((ImsPhone) imsPhone).registerForImsStateChanged(this, 45, null);
                    if (turnOffIms()) {
                        Message msg = Message.obtain(this);
                        msg.what = 38;
                        int i = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                        this.mPendingRadioPowerOffAfterDataOffTag = i;
                        msg.arg1 = i;
                        if (sendMessageDelayed(msg, 3000L)) {
                            log("Wait upto 3s for ims unregistration, then turn off radio.");
                            this.mPendingRadioPowerOffAfterDataOff = true;
                        } else {
                            log("Cannot send delayed Msg, turn off radio right away.");
                            hangupAndPowerOff();
                        }
                    } else {
                        log("turnOffIms failed, turn off radio right away.");
                        hangupAndPowerOff();
                    }
                } else {
                    log("IMS is not registered, turn off radio right away.");
                    hangupAndPowerOff();
                }
            }
        }
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        if (this.mImsRegistrationOnOff && !registered && this.mAlarmSwitch) {
            this.mImsRegistrationOnOff = registered;
            Context context = this.mPhone.getContext();
            AlarmManager am = (AlarmManager) context.getSystemService("alarm");
            am.cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = false;
            sendMessage(obtainMessage(45));
            return;
        }
        this.mImsRegistrationOnOff = registered;
    }

    public void onSetPhoneRCDone(ArrayList<RadioAccessFamily> phoneRcs) {
        int networkMode;
        boolean needToChangeNetworkMode = false;
        int myPhoneId = this.mPhone.getPhoneId();
        int newCapability = 0;
        if (phoneRcs != null) {
            int size = phoneRcs.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                RadioAccessFamily phoneRaf = phoneRcs.get(i);
                if (myPhoneId != phoneRaf.getPhoneId()) {
                    i++;
                } else {
                    needToChangeNetworkMode = true;
                    newCapability = phoneRaf.getRadioAccessFamily();
                    break;
                }
            }
            if (needToChangeNetworkMode) {
                if ((newCapability & 16384) == 16384) {
                    networkMode = 9;
                } else if ((newCapability & 8) == 8) {
                    networkMode = 0;
                } else if ((newCapability & 65536) == 65536) {
                    networkMode = 1;
                } else {
                    networkMode = -1;
                    log("Error: capability is not define");
                }
                log("myPhoneId=" + myPhoneId + " newCapability=" + newCapability + " networkMode=" + networkMode);
                if (networkMode != -1) {
                    this.mCi.setPreferredNetworkType(networkMode, null);
                }
            }
        }
    }
}
