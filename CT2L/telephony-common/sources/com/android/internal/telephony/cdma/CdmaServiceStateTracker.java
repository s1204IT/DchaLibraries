package com.android.internal.telephony.cdma;

import android.R;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.TimeUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.HbpcdUtils;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CdmaServiceStateTracker extends ServiceStateTracker {
    protected static final String DEFAULT_MNC = "00";
    protected static final String INVALID_MCC = "000";
    static final String LOG_TAG = "CdmaSST";
    private static final int MAX_NITZ_YEAR = 2037;
    private static final int MS_PER_HOUR = 3600000;
    private static final int NITZ_UPDATE_DIFF_DEFAULT = 2000;
    private static final int NITZ_UPDATE_SPACING_DEFAULT = 600000;
    private static final String UNACTIVATED_MIN2_VALUE = "000000";
    private static final String UNACTIVATED_MIN_VALUE = "1111110111";
    private static final String WAKELOCK_TAG = "ServiceStateTracker";
    private ContentObserver mAutoTimeObserver;
    private ContentObserver mAutoTimeZoneObserver;
    protected RegistrantList mCdmaForSubscriptionInfoReadyRegistrants;
    private CdmaSubscriptionSourceManager mCdmaSSM;
    CdmaCellLocation mCellLoc;
    private ContentResolver mCr;
    private String mCurrentCarrier;
    int mCurrentOtaspMode;
    private int mDefaultRoamingIndicator;
    protected boolean mGotCountryCode;
    protected HbpcdUtils mHbpcdUtils;
    protected int[] mHomeNetworkId;
    protected int[] mHomeSystemId;
    private boolean mIsEriTextLoaded;
    private boolean mIsInPrl;
    protected boolean mIsMinInfoReady;
    protected boolean mIsSubscriptionFromRuim;
    protected String mMdn;
    protected String mMin;
    protected boolean mNeedFixZone;
    CdmaCellLocation mNewCellLoc;
    private int mNitzUpdateDiff;
    private int mNitzUpdateSpacing;
    CDMAPhone mPhone;
    protected String mPrlVersion;
    private String mRegistrationDeniedReason;
    protected int mRegistrationState;
    private int mRoamingIndicator;
    long mSavedAtTime;
    long mSavedTime;
    String mSavedTimeZone;
    private PowerManager.WakeLock mWakeLock;
    private boolean mZoneDst;
    private int mZoneOffset;
    private long mZoneTime;

    public CdmaServiceStateTracker(CDMAPhone phone) {
        this(phone, new CellInfoCdma());
    }

    protected CdmaServiceStateTracker(CDMAPhone phone, CellInfo cellInfo) {
        super(phone, phone.mCi, cellInfo);
        this.mCurrentOtaspMode = 0;
        this.mNitzUpdateSpacing = SystemProperties.getInt("ro.nitz_update_spacing", NITZ_UPDATE_SPACING_DEFAULT);
        this.mNitzUpdateDiff = SystemProperties.getInt("ro.nitz_update_diff", NITZ_UPDATE_DIFF_DEFAULT);
        this.mRegistrationState = -1;
        this.mCdmaForSubscriptionInfoReadyRegistrants = new RegistrantList();
        this.mNeedFixZone = false;
        this.mGotCountryCode = false;
        this.mHomeSystemId = null;
        this.mHomeNetworkId = null;
        this.mIsMinInfoReady = false;
        this.mIsEriTextLoaded = false;
        this.mIsSubscriptionFromRuim = false;
        this.mHbpcdUtils = null;
        this.mCurrentCarrier = null;
        this.mAutoTimeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                CdmaServiceStateTracker.this.log("Auto time state changed");
                CdmaServiceStateTracker.this.revertToNitzTime();
            }
        };
        this.mAutoTimeZoneObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                CdmaServiceStateTracker.this.log("Auto time zone state changed");
                CdmaServiceStateTracker.this.revertToNitzTimeZone();
            }
        };
        this.mPhone = phone;
        this.mCr = phone.getContext().getContentResolver();
        this.mCellLoc = new CdmaCellLocation();
        this.mNewCellLoc = new CdmaCellLocation();
        this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(phone.getContext(), this.mCi, this, 39, null);
        this.mIsSubscriptionFromRuim = this.mCdmaSSM.getCdmaSubscriptionSource() == 0;
        PowerManager powerManager = (PowerManager) phone.getContext().getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, WAKELOCK_TAG);
        this.mCi.registerForRadioStateChanged(this, 1, null);
        this.mCi.registerForVoiceNetworkStateChanged(this, 30, null);
        this.mCi.setOnNITZTime(this, 11, null);
        this.mCi.registerForCdmaPrlChanged(this, 40, null);
        phone.registerForEriFileLoaded(this, 36, null);
        this.mCi.registerForCdmaOtaProvision(this, 37, null);
        int airplaneMode = Settings.Global.getInt(this.mCr, "airplane_mode_on", 0);
        this.mDesiredPowerState = airplaneMode <= 0;
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time"), true, this.mAutoTimeObserver);
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), true, this.mAutoTimeZoneObserver);
        setSignalStrengthDefaultValues();
        this.mHbpcdUtils = new HbpcdUtils(phone.getContext());
        phone.notifyOtaspChanged(0);
    }

    @Override
    public void dispose() {
        checkCorrectThread();
        log("ServiceStateTracker dispose");
        this.mCi.unregisterForRadioStateChanged(this);
        this.mCi.unregisterForVoiceNetworkStateChanged(this);
        this.mCi.unregisterForCdmaOtaProvision(this);
        this.mPhone.unregisterForEriFileLoaded(this);
        if (this.mUiccApplcation != null) {
            this.mUiccApplcation.unregisterForReady(this);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForRecordsLoaded(this);
        }
        this.mCi.unSetOnNITZTime(this);
        this.mCr.unregisterContentObserver(this.mAutoTimeObserver);
        this.mCr.unregisterContentObserver(this.mAutoTimeZoneObserver);
        this.mCdmaSSM.dispose(this);
        this.mCi.unregisterForCdmaPrlChanged(this);
        super.dispose();
    }

    protected void finalize() {
        log("CdmaServiceStateTracker finalized");
    }

    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCdmaForSubscriptionInfoReadyRegistrants.add(r);
        if (isMinInfoReady()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
        this.mCdmaForSubscriptionInfoReadyRegistrants.remove(h);
    }

    private void saveCdmaSubscriptionSource(int source) {
        log("Storing cdma subscription source: " + source);
        Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", source);
        log("Read from settings: " + Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", -1));
    }

    private void getSubscriptionInfoAndStartPollingThreads() {
        this.mCi.getCDMASubscription(obtainMessage(34));
        pollState();
    }

    @Override
    public void handleMessage(Message msg) {
        if (!this.mPhone.mIsTheCurrentActivePhone) {
            loge("Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
        }
        switch (msg.what) {
            case 1:
                if (this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON) {
                    handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                    queueNextSignalStrengthPoll();
                }
                setPowerStateToDesired();
                pollState();
                break;
            case 2:
            case 4:
            case 6:
            case 7:
            case 8:
            case 9:
            case 13:
            case 15:
            case 16:
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
            case 28:
            case 29:
            case 32:
            case 33:
            case RadioNVItems.RIL_NV_MIP_PROFILE_HA_SPI:
            case 41:
            case 42:
            case 43:
            case com.android.internal.telephony.gsm.CallFailCause.CHANNEL_NOT_AVAIL:
            default:
                super.handleMessage(msg);
                break;
            case 3:
                if (this.mCi.getRadioState().isOn()) {
                    onSignalStrengthResult((AsyncResult) msg.obj, false);
                    queueNextSignalStrengthPoll();
                }
                break;
            case 5:
            case SmsHeader.ELT_ID_STANDARD_WVG_OBJECT:
            case SmsHeader.ELT_ID_CHARACTER_SIZE_WVG_OBJECT:
                handlePollStateResult(msg.what, (AsyncResult) msg.obj);
                break;
            case 10:
                this.mCi.getSignalStrength(obtainMessage(3));
                break;
            case 11:
                AsyncResult ar = (AsyncResult) msg.obj;
                String nitzString = (String) ((Object[]) ar.result)[0];
                long nitzReceiveTime = ((Long) ((Object[]) ar.result)[1]).longValue();
                setTimeFromNITZString(nitzString, nitzReceiveTime);
                break;
            case 12:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                this.mDontPollSignalStrength = true;
                onSignalStrengthResult(ar2, false);
                break;
            case 14:
                log("EVENT_POLL_STATE_NETWORK_SELECTION_MODE");
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3.exception == null && ar3.result != null) {
                    int[] ints = (int[]) ar3.result;
                    if (ints[0] == 1) {
                        this.mPhone.setNetworkSelectionModeAutomatic(null);
                    }
                } else {
                    log("Unable to getNetworkSelectionMode");
                }
                break;
            case 18:
                if (((AsyncResult) msg.obj).exception == null) {
                    this.mCi.getVoiceRegistrationState(obtainMessage(31, null));
                }
                break;
            case SmsHeader.ELT_ID_EXTENDED_OBJECT_DATA_REQUEST_CMD:
                if (this.mPhone.getLteOnCdmaMode() == 1) {
                    log("Receive EVENT_RUIM_READY");
                    pollState();
                } else {
                    log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    getSubscriptionInfoAndStartPollingThreads();
                }
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                this.mPhone.prepareEri();
                break;
            case 27:
                log("EVENT_RUIM_RECORDS_LOADED: what=" + msg.what);
                updatePhoneObject();
                updateSpnDisplay();
                break;
            case com.android.internal.telephony.gsm.CallFailCause.STATUS_ENQUIRY:
                pollState();
                break;
            case 31:
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    String[] states = (String[]) ar4.result;
                    int baseStationId = -1;
                    int baseStationLatitude = Integer.MAX_VALUE;
                    int baseStationLongitude = Integer.MAX_VALUE;
                    int systemId = -1;
                    int networkId = -1;
                    if (states.length > 9) {
                        try {
                            if (states[4] != null) {
                                baseStationId = Integer.parseInt(states[4]);
                            }
                            if (states[5] != null) {
                                baseStationLatitude = Integer.parseInt(states[5]);
                            }
                            if (states[6] != null) {
                                baseStationLongitude = Integer.parseInt(states[6]);
                            }
                            if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                                baseStationLatitude = Integer.MAX_VALUE;
                                baseStationLongitude = Integer.MAX_VALUE;
                            }
                            if (states[8] != null) {
                                systemId = Integer.parseInt(states[8]);
                            }
                            if (states[9] != null) {
                                networkId = Integer.parseInt(states[9]);
                            }
                        } catch (NumberFormatException ex) {
                            loge("error parsing cell location data: " + ex);
                        }
                    }
                    this.mCellLoc.setCellLocationData(baseStationId, baseStationLatitude, baseStationLongitude, systemId, networkId);
                    this.mPhone.notifyLocationChanged();
                }
                disableSingleLocationUpdate();
                break;
            case 34:
                AsyncResult ar5 = (AsyncResult) msg.obj;
                if (ar5.exception == null) {
                    String[] cdmaSubscription = (String[]) ar5.result;
                    if (cdmaSubscription != null && cdmaSubscription.length >= 5) {
                        this.mMdn = cdmaSubscription[0];
                        parseSidNid(cdmaSubscription[1], cdmaSubscription[2]);
                        this.mMin = cdmaSubscription[3];
                        this.mPrlVersion = cdmaSubscription[4];
                        log("GET_CDMA_SUBSCRIPTION: MDN=" + this.mMdn);
                        this.mIsMinInfoReady = true;
                        updateOtaspState();
                        if (!this.mIsSubscriptionFromRuim && this.mIccRecords != null) {
                            log("GET_CDMA_SUBSCRIPTION set imsi in mIccRecords");
                            this.mIccRecords.setImsi(getImsi());
                        } else {
                            log("GET_CDMA_SUBSCRIPTION either mIccRecords is null  or NV type device - not setting Imsi in mIccRecords");
                        }
                    } else {
                        log("GET_CDMA_SUBSCRIPTION: error parsing cdmaSubscription params num=" + cdmaSubscription.length);
                    }
                }
                break;
            case 35:
                updatePhoneObject();
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                getSubscriptionInfoAndStartPollingThreads();
                break;
            case 36:
                log("[CdmaServiceStateTracker] ERI file has been loaded, repolling.");
                pollState();
                break;
            case 37:
                AsyncResult ar6 = (AsyncResult) msg.obj;
                if (ar6.exception == null) {
                    int[] ints2 = (int[]) ar6.result;
                    int otaStatus = ints2[0];
                    if (otaStatus == 8 || otaStatus == 10) {
                        log("EVENT_OTA_PROVISION_STATUS_CHANGE: Complete, Reload MDN");
                        this.mCi.getCDMASubscription(obtainMessage(34));
                    }
                }
                break;
            case 39:
                handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                break;
            case RadioNVItems.RIL_NV_MIP_PROFILE_MN_HA_SS:
                AsyncResult ar7 = (AsyncResult) msg.obj;
                if (ar7.exception == null) {
                    int[] ints3 = (int[]) ar7.result;
                    this.mPrlVersion = Integer.toString(ints3[0]);
                }
                break;
            case 45:
                log("EVENT_CHANGE_IMS_STATE");
                setPowerStateToDesired();
                break;
        }
    }

    private void handleCdmaSubscriptionSource(int newSubscriptionSource) {
        log("Subscription Source : " + newSubscriptionSource);
        this.mIsSubscriptionFromRuim = newSubscriptionSource == 0;
        log("isFromRuim: " + this.mIsSubscriptionFromRuim);
        saveCdmaSubscriptionSource(newSubscriptionSource);
        if (!this.mIsSubscriptionFromRuim) {
            sendMessage(obtainMessage(35));
        }
    }

    @Override
    protected void setPowerStateToDesired() {
        if (this.mDesiredPowerState && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            this.mCi.setRadioPower(true, null);
            return;
        }
        if (!this.mDesiredPowerState && this.mCi.getRadioState().isOn()) {
            DcTrackerBase dcTracker = this.mPhone.mDcTracker;
            powerOffRadioSafely(dcTracker);
        } else if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
            this.mCi.requestShutdown(null);
        }
    }

    @Override
    protected void updateSpnDisplay() {
        String plmn = this.mSS.getOperatorAlphaLong();
        boolean showPlmn = false;
        if (!TextUtils.equals(plmn, this.mCurPlmn)) {
            showPlmn = plmn != null;
            log(String.format("updateSpnDisplay: changed sending intent showPlmn='%b' plmn='%s'", Boolean.valueOf(showPlmn), plmn));
            Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            intent.addFlags(536870912);
            intent.putExtra("showSpn", false);
            intent.putExtra("spn", "");
            intent.putExtra("showPlmn", showPlmn);
            intent.putExtra(Telephony.CellBroadcasts.PLMN, plmn);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), showPlmn, plmn, false, "")) {
                this.mSpnUpdatePending = true;
            }
        }
        this.mCurShowSpn = false;
        this.mCurShowPlmn = showPlmn;
        this.mCurSpn = "";
        this.mCurPlmn = plmn;
    }

    @Override
    protected Phone getPhone() {
        return this.mPhone;
    }

    protected void handlePollStateResultMessage(int what, AsyncResult ar) {
        switch (what) {
            case 5:
                String[] states = (String[]) ar.result;
                log("handlePollStateResultMessage: EVENT_POLL_STATE_GPRS states.length=" + states.length + " states=" + states);
                int regState = 4;
                int dataRadioTechnology = 0;
                if (states.length > 0) {
                    try {
                        regState = Integer.parseInt(states[0]);
                        if (states.length >= 4 && states[3] != null) {
                            dataRadioTechnology = Integer.parseInt(states[3]);
                        }
                    } catch (NumberFormatException ex) {
                        loge("handlePollStateResultMessage: error parsing GprsRegistrationState: " + ex);
                    }
                    break;
                }
                int dataRegState = regCodeToServiceState(regState);
                this.mNewSS.setDataRegState(dataRegState);
                this.mNewSS.setRilDataRadioTechnology(dataRadioTechnology);
                this.mNewSS.setDataRoaming(regCodeIsRoaming(regState));
                log("handlPollStateResultMessage: cdma setDataRegState=" + dataRegState + " regState=" + regState + " dataRadioTechnology=" + dataRadioTechnology);
                return;
            case SmsHeader.ELT_ID_STANDARD_WVG_OBJECT:
                String[] states2 = (String[]) ar.result;
                int registrationState = 4;
                int radioTechnology = -1;
                int baseStationId = -1;
                int baseStationLatitude = Integer.MAX_VALUE;
                int baseStationLongitude = Integer.MAX_VALUE;
                int cssIndicator = 0;
                int systemId = 0;
                int networkId = 0;
                int roamingIndicator = -1;
                int systemIsInPrl = 0;
                int defaultRoamingIndicator = 0;
                int reasonForDenial = 0;
                if (states2.length >= 14) {
                    try {
                        if (states2[0] != null) {
                            registrationState = Integer.parseInt(states2[0]);
                        }
                        if (states2[3] != null) {
                            radioTechnology = Integer.parseInt(states2[3]);
                        }
                        if (states2[4] != null) {
                            baseStationId = Integer.parseInt(states2[4]);
                        }
                        if (states2[5] != null) {
                            baseStationLatitude = Integer.parseInt(states2[5]);
                        }
                        if (states2[6] != null) {
                            baseStationLongitude = Integer.parseInt(states2[6]);
                        }
                        if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                            baseStationLatitude = Integer.MAX_VALUE;
                            baseStationLongitude = Integer.MAX_VALUE;
                        }
                        if (states2[7] != null) {
                            cssIndicator = Integer.parseInt(states2[7]);
                        }
                        if (states2[8] != null) {
                            systemId = Integer.parseInt(states2[8]);
                        }
                        if (states2[9] != null) {
                            networkId = Integer.parseInt(states2[9]);
                        }
                        if (states2[10] != null) {
                            roamingIndicator = Integer.parseInt(states2[10]);
                        }
                        if (states2[11] != null) {
                            systemIsInPrl = Integer.parseInt(states2[11]);
                        }
                        if (states2[12] != null) {
                            defaultRoamingIndicator = Integer.parseInt(states2[12]);
                        }
                        if (states2[13] != null) {
                            reasonForDenial = Integer.parseInt(states2[13]);
                        }
                        break;
                    } catch (NumberFormatException ex2) {
                        loge("EVENT_POLL_STATE_REGISTRATION_CDMA: error parsing: " + ex2);
                    }
                    this.mRegistrationState = registrationState;
                    boolean cdmaRoaming = regCodeIsRoaming(registrationState) && !isRoamIndForHomeSystem(states2[10]);
                    this.mNewSS.setVoiceRoaming(cdmaRoaming);
                    this.mNewSS.setState(regCodeToServiceState(registrationState));
                    this.mNewSS.setRilVoiceRadioTechnology(radioTechnology);
                    this.mNewSS.setCssIndicator(cssIndicator);
                    this.mNewSS.setSystemAndNetworkId(systemId, networkId);
                    this.mRoamingIndicator = roamingIndicator;
                    this.mIsInPrl = systemIsInPrl != 0;
                    this.mDefaultRoamingIndicator = defaultRoamingIndicator;
                    this.mNewCellLoc.setCellLocationData(baseStationId, baseStationLatitude, baseStationLongitude, systemId, networkId);
                    if (reasonForDenial == 0) {
                        this.mRegistrationDeniedReason = "General";
                    } else if (reasonForDenial == 1) {
                        this.mRegistrationDeniedReason = "Authentication Failure";
                    } else {
                        this.mRegistrationDeniedReason = "";
                    }
                    if (this.mRegistrationState == 3) {
                        log("Registration denied, " + this.mRegistrationDeniedReason);
                        return;
                    }
                    return;
                }
                throw new RuntimeException("Warning! Wrong number of parameters returned from RIL_REQUEST_REGISTRATION_STATE: expected 14 or more strings and got " + states2.length + " strings");
            case SmsHeader.ELT_ID_CHARACTER_SIZE_WVG_OBJECT:
                String[] opNames = (String[]) ar.result;
                if (opNames != null && opNames.length >= 3) {
                    if (opNames[2] == null || opNames[2].length() < 5 || "00000".equals(opNames[2])) {
                        opNames[2] = SystemProperties.get(CDMAPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "00000");
                        log("RIL_REQUEST_OPERATOR.response[2], the numeric,  is bad. Using SystemProperties '" + CDMAPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC + "'= " + opNames[2]);
                    }
                    if (!this.mIsSubscriptionFromRuim) {
                        this.mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                        return;
                    }
                    String brandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                    if (brandOverride != null) {
                        this.mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                        return;
                    } else {
                        this.mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                        return;
                    }
                }
                log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                return;
            default:
                loge("handlePollStateResultMessage: RIL response handle in wrong phone! Expected CDMA RIL request and get GSM RIL request.");
                return;
        }
    }

    @Override
    protected void handlePollStateResult(int what, AsyncResult ar) {
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
                    loge("handlePollStateResult: RIL returned an error where it must succeed" + ar.exception);
                }
            } else {
                try {
                    handlePollStateResultMessage(what, ar);
                } catch (RuntimeException ex) {
                    loge("handlePollStateResult: Exception while polling service state. Probably malformed RIL response." + ex);
                }
            }
            this.mPollingContext[0] = r7[0] - 1;
            if (this.mPollingContext[0] == 0) {
                boolean namMatch = false;
                if (!isSidsAllZeros() && isHomeSid(this.mNewSS.getSystemId())) {
                    namMatch = true;
                }
                if (this.mIsSubscriptionFromRuim) {
                    this.mNewSS.setVoiceRoaming(isRoamingBetweenOperators(this.mNewSS.getVoiceRoaming(), this.mNewSS));
                }
                boolean isVoiceInService = this.mNewSS.getVoiceRegState() == 0;
                int dataRegType = this.mNewSS.getRilDataRadioTechnology();
                if (isVoiceInService && ServiceState.isCdma(dataRegType)) {
                    this.mNewSS.setDataRoaming(this.mNewSS.getVoiceRoaming());
                }
                this.mNewSS.setCdmaDefaultRoamingIndicator(this.mDefaultRoamingIndicator);
                this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                boolean isPrlLoaded = true;
                if (TextUtils.isEmpty(this.mPrlVersion)) {
                    isPrlLoaded = false;
                }
                if (!isPrlLoaded || this.mNewSS.getRilVoiceRadioTechnology() == 0) {
                    log("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    this.mNewSS.setCdmaRoamingIndicator(1);
                } else if (!isSidsAllZeros()) {
                    if (!namMatch && !this.mIsInPrl) {
                        this.mNewSS.setCdmaRoamingIndicator(this.mDefaultRoamingIndicator);
                    } else if (namMatch && !this.mIsInPrl) {
                        if (this.mNewSS.getRilVoiceRadioTechnology() == 14) {
                            log("Turn off roaming indicator as voice is LTE");
                            this.mNewSS.setCdmaRoamingIndicator(1);
                        } else {
                            this.mNewSS.setCdmaRoamingIndicator(2);
                        }
                    } else if ((namMatch || !this.mIsInPrl) && this.mRoamingIndicator <= 2) {
                        this.mNewSS.setCdmaRoamingIndicator(1);
                    } else {
                        this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                    }
                }
                int roamingIndicator = this.mNewSS.getCdmaRoamingIndicator();
                this.mNewSS.setCdmaEriIconIndex(this.mPhone.mEriManager.getCdmaEriIconIndex(roamingIndicator, this.mDefaultRoamingIndicator));
                this.mNewSS.setCdmaEriIconMode(this.mPhone.mEriManager.getCdmaEriIconMode(roamingIndicator, this.mDefaultRoamingIndicator));
                log("Set CDMA Roaming Indicator to: " + this.mNewSS.getCdmaRoamingIndicator() + ". voiceRoaming = " + this.mNewSS.getVoiceRoaming() + ". dataRoaming = " + this.mNewSS.getDataRoaming() + ", isPrlLoaded = " + isPrlLoaded + ". namMatch = " + namMatch + " , mIsInPrl = " + this.mIsInPrl + ", mRoamingIndicator = " + this.mRoamingIndicator + ", mDefaultRoamingIndicator= " + this.mDefaultRoamingIndicator);
                pollStateDone();
            }
        }
    }

    @Override
    protected void setRoamingType(ServiceState currentServiceState) {
        boolean isVoiceInService = currentServiceState.getVoiceRegState() == 0;
        if (isVoiceInService) {
            if (currentServiceState.getVoiceRoaming()) {
                int[] intRoamingIndicators = this.mPhone.getContext().getResources().getIntArray(R.array.config_defaultNotificationVibePattern);
                if (intRoamingIndicators != null && intRoamingIndicators.length > 0) {
                    currentServiceState.setVoiceRoamingType(2);
                    int curRoamingIndicator = currentServiceState.getCdmaRoamingIndicator();
                    int i = 0;
                    while (true) {
                        if (i >= intRoamingIndicators.length) {
                            break;
                        }
                        if (curRoamingIndicator != intRoamingIndicators[i]) {
                            i++;
                        } else {
                            currentServiceState.setVoiceRoamingType(3);
                            break;
                        }
                    }
                } else if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
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
            if (ServiceState.isCdma(dataRegType)) {
                if (isVoiceInService) {
                    currentServiceState.setDataRoamingType(currentServiceState.getVoiceRoamingType());
                    return;
                } else {
                    currentServiceState.setDataRoamingType(1);
                    return;
                }
            }
            if (inSameCountry(currentServiceState.getDataOperatorNumeric())) {
                currentServiceState.setDataRoamingType(2);
            } else {
                currentServiceState.setDataRoamingType(3);
            }
        }
    }

    @Override
    protected String getHomeOperatorNumeric() {
        String numeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhoneBase.getPhoneId());
        if (TextUtils.isEmpty(numeric)) {
            return SystemProperties.get(CDMAPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
        }
        return numeric;
    }

    protected void setSignalStrengthDefaultValues() {
        this.mSignalStrength = new SignalStrength(false);
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
                pollStateDone();
                break;
            case RADIO_OFF:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = false;
                pollStateDone();
                break;
            default:
                int[] iArr = this.mPollingContext;
                iArr[0] = iArr[0] + 1;
                this.mCi.getOperator(obtainMessage(25, this.mPollingContext));
                int[] iArr2 = this.mPollingContext;
                iArr2[0] = iArr2[0] + 1;
                this.mCi.getVoiceRegistrationState(obtainMessage(24, this.mPollingContext));
                int[] iArr3 = this.mPollingContext;
                iArr3[0] = iArr3[0] + 1;
                this.mCi.getDataRegistrationState(obtainMessage(5, this.mPollingContext));
                break;
        }
    }

    protected void fixTimeZone(String isoCountryCode) {
        TimeZone zone;
        String zoneName = SystemProperties.get("persist.sys.timezone");
        log("fixTimeZone zoneName='" + zoneName + "' mZoneOffset=" + this.mZoneOffset + " mZoneDst=" + this.mZoneDst + " iso-cc='" + isoCountryCode + "' iso-cc-idx=" + Arrays.binarySearch(GMT_COUNTRY_CODES, isoCountryCode));
        if (this.mZoneOffset == 0 && !this.mZoneDst && zoneName != null && zoneName.length() > 0 && Arrays.binarySearch(GMT_COUNTRY_CODES, isoCountryCode) < 0) {
            zone = TimeZone.getDefault();
            if (this.mNeedFixZone) {
                long ctm = System.currentTimeMillis();
                long tzOffset = zone.getOffset(ctm);
                log("fixTimeZone: tzOffset=" + tzOffset + " ltod=" + TimeUtils.logTimeOfDay(ctm));
                if (getAutoTime()) {
                    long adj = ctm - tzOffset;
                    log("fixTimeZone: adj ltod=" + TimeUtils.logTimeOfDay(adj));
                    setAndBroadcastNetworkSetTime(adj);
                } else {
                    this.mSavedTime -= tzOffset;
                    log("fixTimeZone: adj mSavedTime=" + this.mSavedTime);
                }
            }
            log("fixTimeZone: using default TimeZone");
        } else if (isoCountryCode.equals("")) {
            zone = getNitzTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime);
            log("fixTimeZone: using NITZ TimeZone");
        } else {
            zone = TimeUtils.getTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime, isoCountryCode);
            log("fixTimeZone: using getTimeZone(off, dst, time, iso)");
        }
        this.mNeedFixZone = false;
        if (zone != null) {
            log("fixTimeZone: zone != null zone.getID=" + zone.getID());
            if (getAutoTimeZone()) {
                setAndBroadcastNetworkSetTimeZone(zone.getID());
            } else {
                log("fixTimeZone: skip changing zone as getAutoTimeZone was false");
            }
            saveNitzTimeZone(zone.getID());
            return;
        }
        log("fixTimeZone: zone == null, do nothing for zone");
    }

    protected void pollStateDone() {
        String eriText;
        log("pollStateDone: cdma oldSS=[" + this.mSS + "] newSS=[" + this.mNewSS + "]");
        if (this.mPhone.isMccMncMarkedAsNonRoaming(this.mNewSS.getOperatorNumeric()) || this.mPhone.isSidMarkedAsNonRoaming(this.mNewSS.getSystemId())) {
            log("pollStateDone: override - marked as non-roaming.");
            this.mNewSS.setVoiceRoaming(false);
            this.mNewSS.setDataRoaming(false);
            this.mNewSS.setCdmaEriIconIndex(1);
        } else if (this.mPhone.isMccMncMarkedAsRoaming(this.mNewSS.getOperatorNumeric()) || this.mPhone.isSidMarkedAsRoaming(this.mNewSS.getSystemId())) {
            log("pollStateDone: override - marked as roaming.");
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
            this.mNewSS.setCdmaEriIconIndex(0);
            this.mNewSS.setCdmaEriIconMode(0);
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("telephony.test.forceRoaming", false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        boolean hasRegistered = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        if (this.mSS.getVoiceRegState() != 0 || this.mNewSS.getVoiceRegState() != 0) {
        }
        boolean hasCdmaDataConnectionAttached = this.mSS.getDataRegState() != 0 && this.mNewSS.getDataRegState() == 0;
        boolean hasCdmaDataConnectionDetached = this.mSS.getDataRegState() == 0 && this.mNewSS.getDataRegState() != 0;
        boolean hasCdmaDataConnectionChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState();
        boolean hasRilVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology();
        boolean hasRilDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology();
        boolean hasChanged = !this.mNewSS.equals(this.mSS);
        boolean hasVoiceRoamingOn = !this.mSS.getVoiceRoaming() && this.mNewSS.getVoiceRoaming();
        boolean hasVoiceRoamingOff = this.mSS.getVoiceRoaming() && !this.mNewSS.getVoiceRoaming();
        boolean hasDataRoamingOn = !this.mSS.getDataRoaming() && this.mNewSS.getDataRoaming();
        boolean hasDataRoamingOff = this.mSS.getDataRoaming() && !this.mNewSS.getDataRoaming();
        boolean hasLocationChanged = !this.mNewCellLoc.equals(this.mCellLoc);
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState() || this.mSS.getDataRegState() != this.mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        CdmaCellLocation tcl = this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
        }
        if (hasRegistered) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
        }
        if (hasChanged) {
            if (this.mCi.getRadioState().isOn() && !this.mIsSubscriptionFromRuim) {
                if (this.mSS.getVoiceRegState() == 0) {
                    eriText = this.mPhone.getCdmaEriText();
                } else {
                    eriText = this.mPhone.getContext().getText(R.string.PERSOSUBSTATE_SIM_SP_EHPLMN_SUCCESS).toString();
                }
                this.mSS.setOperatorAlphaLong(eriText);
            }
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlphaLong());
            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = this.mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                log("operatorNumeric " + operatorNumeric + "is invalid");
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), "");
                this.mGotCountryCode = false;
            } else {
                String isoCountryCode = "";
                operatorNumeric.substring(0, 3);
                try {
                    isoCountryCode = MccTable.countryCodeForMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
                } catch (NumberFormatException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex2) {
                    loge("pollStateDone: countryCodeForMcc error" + ex2);
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), isoCountryCode);
                this.mGotCountryCode = true;
                setOperatorIdd(operatorNumeric);
                if (shouldFixTimeZoneNow(this.mPhone, operatorNumeric, prevOperatorNumeric, this.mNeedFixZone)) {
                    fixTimeZone(isoCountryCode);
                }
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), this.mSS.getVoiceRoaming() || this.mSS.getDataRoaming());
            updateSpnDisplay();
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            this.mPhone.notifyServiceStateChanged(this.mSS);
        }
        if (hasCdmaDataConnectionAttached) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionChanged || hasRilDataRadioTechnologyChanged) {
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
    }

    protected boolean isInvalidOperatorNumeric(String operatorNumeric) {
        return operatorNumeric == null || operatorNumeric.length() < 5 || operatorNumeric.startsWith(INVALID_MCC);
    }

    protected String fixUnknownMcc(String operatorNumeric, int sid) {
        if (sid <= 0) {
            return operatorNumeric;
        }
        boolean isNitzTimeZone = false;
        int timeZone = 0;
        if (this.mSavedTimeZone != null) {
            timeZone = TimeZone.getTimeZone(this.mSavedTimeZone).getRawOffset() / MS_PER_HOUR;
            isNitzTimeZone = true;
        } else {
            TimeZone tzone = getNitzTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime);
            if (tzone != null) {
                timeZone = tzone.getRawOffset() / MS_PER_HOUR;
            }
        }
        int mcc = this.mHbpcdUtils.getMcc(sid, timeZone, this.mZoneDst ? 1 : 0, isNitzTimeZone);
        if (mcc > 0) {
            operatorNumeric = Integer.toString(mcc) + DEFAULT_MNC;
        }
        return operatorNumeric;
    }

    protected void setOperatorIdd(String operatorNumeric) {
        String idd = this.mHbpcdUtils.getIddByMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
        if (idd != null && !idd.isEmpty()) {
            this.mPhone.setSystemProperty("gsm.operator.idpstring", idd);
        } else {
            this.mPhone.setSystemProperty("gsm.operator.idpstring", "+");
        }
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
            rawOffset -= MS_PER_HOUR;
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

    protected int radioTechnologyToDataServiceState(int code) {
        switch (code) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                break;
            case 6:
            case 7:
            case 8:
            case 12:
            case 13:
                break;
            case 9:
            case 10:
            case 11:
            default:
                loge("radioTechnologyToDataServiceState: Wrong radioTechnology code.");
                break;
        }
        return 1;
    }

    protected int regCodeToServiceState(int code) {
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
            default:
                loge("regCodeToServiceState: unexpected service state " + code);
                break;
        }
        return 1;
    }

    @Override
    public int getCurrentDataConnectionState() {
        return this.mSS.getDataRegState();
    }

    protected boolean regCodeIsRoaming(int code) {
        return 5 == code;
    }

    private boolean isRoamIndForHomeSystem(String roamInd) {
        String[] homeRoamIndicators = this.mPhone.getContext().getResources().getStringArray(R.array.config_concurrentDisplayDeviceStates);
        if (homeRoamIndicators == null) {
            return false;
        }
        for (String homeRoamInd : homeRoamIndicators) {
            if (homeRoamInd.equals(roamInd)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRoamingBetweenOperators(boolean cdmaRoaming, ServiceState s) {
        String spn = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNameForPhone(this.mPhoneBase.getPhoneId());
        String onsl = s.getVoiceOperatorAlphaLong();
        String onss = s.getVoiceOperatorAlphaShort();
        boolean equalsOnsl = onsl != null && spn.equals(onsl);
        boolean equalsOnss = onss != null && spn.equals(onss);
        return (!cdmaRoaming || equalsOnsl || equalsOnss) ? false : true;
    }

    private void setTimeFromNITZString(String nitz, long nitzReceiveTime) {
        long start = SystemClock.elapsedRealtime();
        log("NITZ: " + nitz + "," + nitzReceiveTime + " start=" + start + " delay=" + (start - nitzReceiveTime));
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.clear();
            c.set(16, 0);
            String[] nitzSubs = nitz.split("[/:,+-]");
            int year = Integer.parseInt(nitzSubs[0]) + NITZ_UPDATE_DIFF_DEFAULT;
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
                if (iso == null || iso.length() <= 0) {
                    zone = getNitzTimeZone(tzOffset2, dst != 0, c.getTimeInMillis());
                } else {
                    zone = TimeUtils.getTimeZone(tzOffset2, dst != 0, c.getTimeInMillis(), iso);
                }
            }
            if (zone == null || this.mZoneOffset != tzOffset2) {
                this.mNeedFixZone = true;
                this.mZoneOffset = tzOffset2;
                this.mZoneDst = dst != 0;
                this.mZoneTime = c.getTimeInMillis();
            } else {
                if (this.mZoneDst != (dst != 0)) {
                }
            }
            log("NITZ: tzOffset=" + tzOffset2 + " dst=" + dst + " zone=" + (zone != null ? zone.getID() : "NULL") + " iso=" + iso + " mGotCountryCode=" + this.mGotCountryCode + " mNeedFixZone=" + this.mNeedFixZone);
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
                if (getAutoTime()) {
                    long gained = c.getTimeInMillis() - System.currentTimeMillis();
                    long timeSinceLastUpdate = SystemClock.elapsedRealtime() - this.mSavedAtTime;
                    int nitzUpdateSpacing = Settings.Global.getInt(this.mCr, "nitz_update_spacing", this.mNitzUpdateSpacing);
                    int nitzUpdateDiff = Settings.Global.getInt(this.mCr, "nitz_update_diff", this.mNitzUpdateDiff);
                    if (this.mSavedAtTime != 0 && timeSinceLastUpdate <= nitzUpdateSpacing && Math.abs(gained) <= nitzUpdateDiff) {
                        log("NITZ: ignore, a previous update was " + timeSinceLastUpdate + "ms ago and gained=" + gained + "ms");
                        long end = SystemClock.elapsedRealtime();
                        log("NITZ: end=" + end + " dur=" + (end - start));
                        this.mWakeLock.release();
                        return;
                    }
                    log("NITZ: Auto updating time of day to " + c.getTime() + " NITZ receive delay=" + millisSinceNitzReceived + "ms gained=" + gained + "ms from " + nitz);
                    setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                }
                log("NITZ: update nitz time property");
                SystemProperties.set("gsm.nitz.time", String.valueOf(c.getTimeInMillis()));
                this.mSavedTime = c.getTimeInMillis();
                this.mSavedAtTime = SystemClock.elapsedRealtime();
            } finally {
                long end2 = SystemClock.elapsedRealtime();
                log("NITZ: end=" + end2 + " dur=" + (end2 - start));
                this.mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            loge("NITZ: Parsing NITZ time " + nitz + " ex=" + ex);
        }
    }

    private boolean getAutoTime() {
        try {
            return Settings.Global.getInt(this.mCr, "auto_time") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    private boolean getAutoTimeZone() {
        try {
            return Settings.Global.getInt(this.mCr, "auto_time_zone") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    private void saveNitzTimeZone(String zoneId) {
        this.mSavedTimeZone = zoneId;
    }

    private void setAndBroadcastNetworkSetTimeZone(String zoneId) {
        log("setAndBroadcastNetworkSetTimeZone: setTimeZone=" + zoneId);
        AlarmManager alarm = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        alarm.setTimeZone(zoneId);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIMEZONE");
        intent.addFlags(536870912);
        intent.putExtra("time-zone", zoneId);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
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
        if (Settings.Global.getInt(this.mCr, "auto_time", 0) != 0) {
            log("revertToNitzTime: mSavedTime=" + this.mSavedTime + " mSavedAtTime=" + this.mSavedAtTime);
            if (this.mSavedTime != 0 && this.mSavedAtTime != 0) {
                setAndBroadcastNetworkSetTime(this.mSavedTime + (SystemClock.elapsedRealtime() - this.mSavedAtTime));
            }
        }
    }

    private void revertToNitzTimeZone() {
        if (Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "auto_time_zone", 0) != 0) {
            log("revertToNitzTimeZone: tz='" + this.mSavedTimeZone);
            if (this.mSavedTimeZone != null) {
                setAndBroadcastNetworkSetTimeZone(this.mSavedTimeZone);
            }
        }
    }

    protected boolean isSidsAllZeros() {
        if (this.mHomeSystemId != null) {
            for (int i = 0; i < this.mHomeSystemId.length; i++) {
                if (this.mHomeSystemId[i] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isHomeSid(int sid) {
        if (this.mHomeSystemId != null) {
            for (int i = 0; i < this.mHomeSystemId.length; i++) {
                if (sid == this.mHomeSystemId[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isConcurrentVoiceAndDataAllowed() {
        return false;
    }

    public String getMdnNumber() {
        return this.mMdn;
    }

    public String getCdmaMin() {
        return this.mMin;
    }

    public String getPrlVersion() {
        return this.mPrlVersion;
    }

    String getImsi() {
        String operatorNumeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhoneBase.getPhoneId());
        if (TextUtils.isEmpty(operatorNumeric) || getCdmaMin() == null) {
            return null;
        }
        return operatorNumeric + getCdmaMin();
    }

    public boolean isMinInfoReady() {
        return this.mIsMinInfoReady;
    }

    int getOtasp() {
        int provisioningState;
        if (this.mIsSubscriptionFromRuim && this.mMin == null) {
            return 2;
        }
        if (this.mMin == null || this.mMin.length() < 6) {
            log("getOtasp: bad mMin='" + this.mMin + "'");
            provisioningState = 1;
        } else if (this.mMin.equals(UNACTIVATED_MIN_VALUE) || this.mMin.substring(0, 6).equals(UNACTIVATED_MIN2_VALUE) || SystemProperties.getBoolean("test_cdma_setup", false)) {
            provisioningState = 2;
        } else {
            provisioningState = 3;
        }
        log("getOtasp: state=" + provisioningState);
        return provisioningState;
    }

    @Override
    protected void hangupAndPowerOff() {
        this.mPhone.mCT.mRingingCall.hangupIfAlive();
        this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
        this.mPhone.mCT.mForegroundCall.hangupIfAlive();
        this.mCi.setRadioPower(false, null);
    }

    protected void parseSidNid(String sidStr, String nidStr) {
        if (sidStr != null) {
            String[] sid = sidStr.split(",");
            this.mHomeSystemId = new int[sid.length];
            for (int i = 0; i < sid.length; i++) {
                try {
                    this.mHomeSystemId[i] = Integer.parseInt(sid[i]);
                } catch (NumberFormatException ex) {
                    loge("error parsing system id: " + ex);
                }
            }
        }
        log("CDMA_SUBSCRIPTION: SID=" + sidStr);
        if (nidStr != null) {
            String[] nid = nidStr.split(",");
            this.mHomeNetworkId = new int[nid.length];
            for (int i2 = 0; i2 < nid.length; i2++) {
                try {
                    this.mHomeNetworkId[i2] = Integer.parseInt(nid[i2]);
                } catch (NumberFormatException ex2) {
                    loge("CDMA_SUBSCRIPTION: error parsing network id: " + ex2);
                }
            }
        }
        log("CDMA_SUBSCRIPTION: NID=" + nidStr);
    }

    protected void updateOtaspState() {
        int otaspMode = getOtasp();
        int oldOtaspMode = this.mCurrentOtaspMode;
        this.mCurrentOtaspMode = otaspMode;
        if (this.mCdmaForSubscriptionInfoReadyRegistrants != null) {
            log("CDMA_SUBSCRIPTION: call notifyRegistrants()");
            this.mCdmaForSubscriptionInfoReadyRegistrants.notifyRegistrants();
        }
        if (oldOtaspMode != this.mCurrentOtaspMode) {
            log("CDMA_SUBSCRIPTION: call notifyOtaspChanged old otaspMode=" + oldOtaspMode + " new otaspMode=" + this.mCurrentOtaspMode);
            this.mPhone.notifyOtaspChanged(this.mCurrentOtaspMode);
        }
    }

    protected UiccCardApplication getUiccCardApplication() {
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 2);
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
                if (this.mIsSubscriptionFromRuim) {
                    this.mUiccApplcation.registerForReady(this, 26, null);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.registerForRecordsLoaded(this, 27, null);
                    }
                }
            }
        }
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[CdmaSST] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[CdmaSST] " + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CdmaServiceStateTracker extends:");
        super.dump(fd, pw, args);
        pw.flush();
        pw.println(" mPhone=" + this.mPhone);
        pw.println(" mSS=" + this.mSS);
        pw.println(" mNewSS=" + this.mNewSS);
        pw.println(" mCellLoc=" + this.mCellLoc);
        pw.println(" mNewCellLoc=" + this.mNewCellLoc);
        pw.println(" mCurrentOtaspMode=" + this.mCurrentOtaspMode);
        pw.println(" mRoamingIndicator=" + this.mRoamingIndicator);
        pw.println(" mIsInPrl=" + this.mIsInPrl);
        pw.println(" mDefaultRoamingIndicator=" + this.mDefaultRoamingIndicator);
        pw.println(" mRegistrationState=" + this.mRegistrationState);
        pw.println(" mNeedFixZone=" + this.mNeedFixZone);
        pw.flush();
        pw.println(" mZoneOffset=" + this.mZoneOffset);
        pw.println(" mZoneDst=" + this.mZoneDst);
        pw.println(" mZoneTime=" + this.mZoneTime);
        pw.println(" mGotCountryCode=" + this.mGotCountryCode);
        pw.println(" mSavedTimeZone=" + this.mSavedTimeZone);
        pw.println(" mSavedTime=" + this.mSavedTime);
        pw.println(" mSavedAtTime=" + this.mSavedAtTime);
        pw.println(" mWakeLock=" + this.mWakeLock);
        pw.println(" mCurPlmn=" + this.mCurPlmn);
        pw.println(" mMdn=" + this.mMdn);
        pw.println(" mHomeSystemId=" + this.mHomeSystemId);
        pw.println(" mHomeNetworkId=" + this.mHomeNetworkId);
        pw.println(" mMin=" + this.mMin);
        pw.println(" mPrlVersion=" + this.mPrlVersion);
        pw.println(" mIsMinInfoReady=" + this.mIsMinInfoReady);
        pw.println(" mIsEriTextLoaded=" + this.mIsEriTextLoaded);
        pw.println(" mIsSubscriptionFromRuim=" + this.mIsSubscriptionFromRuim);
        pw.println(" mCdmaSSM=" + this.mCdmaSSM);
        pw.println(" mRegistrationDeniedReason=" + this.mRegistrationDeniedReason);
        pw.println(" mCurrentCarrier=" + this.mCurrentCarrier);
        pw.flush();
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        log("ImsRegistrationState - registered : " + registered);
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
}
