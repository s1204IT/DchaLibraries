package com.android.internal.telephony.uicc;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyEventLog;
import com.android.internal.telephony.cat.BipUtils;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.test.SimulatedCommands;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UsimServiceTable;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.ITelephonyExt;
import com.mediatek.internal.telephony.ppl.PplControlData;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

public class SIMRecords extends IccRecords {

    private static final int[] f31x102e4fe = null;
    static final int CFF_LINE1_MASK = 15;
    static final int CFF_LINE1_RESET = 240;
    static final int CFF_UNCONDITIONAL_ACTIVE = 10;
    static final int CFF_UNCONDITIONAL_DEACTIVE = 5;
    private static final int CFIS_ADN_CAPABILITY_ID_OFFSET = 14;
    private static final int CFIS_ADN_EXTENSION_ID_OFFSET = 15;
    private static final int CFIS_BCD_NUMBER_LENGTH_OFFSET = 2;
    private static final int CFIS_TON_NPI_OFFSET = 3;
    private static final int CPHS_SST_MBN_ENABLED = 48;
    private static final int CPHS_SST_MBN_MASK = 48;
    private static final boolean CRASH_RIL = false;
    private static final int EVENT_APP_LOCKED = 35;
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 37;
    private static final int EVENT_CFU_IND = 211;
    private static final int EVENT_DELAYED_SEND_PHB_CHANGE = 200;
    private static final int EVENT_DUAL_IMSI_READY = 44;
    private static final int EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED = 203;
    protected static final int EVENT_GET_AD_DONE = 9;
    private static final int EVENT_GET_ALL_OPL_DONE = 104;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    private static final int EVENT_GET_CFF_DONE = 24;
    private static final int EVENT_GET_CFIS_DONE = 32;
    private static final int EVENT_GET_CPHSONS_DONE = 105;
    private static final int EVENT_GET_CPHS_MAILBOX_DONE = 11;
    private static final int EVENT_GET_CSP_CPHS_DONE = 33;
    private static final int EVENT_GET_EF_ICCID_DONE = 300;
    private static final int EVENT_GET_ELP_DONE = 43;
    private static final int EVENT_GET_GBABP_DONE = 209;
    private static final int EVENT_GET_GBANL_DONE = 210;
    private static final int EVENT_GET_GID1_DONE = 34;
    private static final int EVENT_GET_GID2_DONE = 36;
    private static final int EVENT_GET_ICCID_DONE = 4;
    private static final int EVENT_GET_IMSI_DONE = 3;
    private static final int EVENT_GET_INFO_CPHS_DONE = 26;
    private static final int EVENT_GET_LI_DONE = 42;
    private static final int EVENT_GET_MBDN_DONE = 6;
    private static final int EVENT_GET_MBI_DONE = 5;
    protected static final int EVENT_GET_MSISDN_DONE = 10;
    private static final int EVENT_GET_MWIS_DONE = 7;
    private static final int EVENT_GET_NEW_MSISDN_DONE = 206;
    private static final int EVENT_GET_PNN_DONE = 15;
    private static final int EVENT_GET_PSISMSC_DONE = 207;
    private static final int EVENT_GET_RAT_DONE = 204;
    private static final int EVENT_GET_SHORT_CPHSONS_DONE = 106;
    private static final int EVENT_GET_SIM_ECC_DONE = 102;
    private static final int EVENT_GET_SMSP_DONE = 208;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_SPDI_DONE = 13;
    private static final int EVENT_GET_SPN_DONE = 12;
    protected static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_GET_USIM_ECC_DONE = 103;
    private static final int EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE = 8;
    private static final int EVENT_IMSI_REFRESH_QUERY = 212;
    private static final int EVENT_IMSI_REFRESH_QUERY_DONE = 213;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_MELOCK_CHANGED = 400;
    private static final int EVENT_QUERY_ICCID_DONE = 107;
    private static final int EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP = 205;
    private static final int EVENT_QUERY_MENU_TITLE_DONE = 53;
    private static final int EVENT_RADIO_AVAILABLE = 41;
    private static final int EVENT_RADIO_STATE_CHANGED = 201;
    private static final int EVENT_SET_CPHS_MAILBOX_DONE = 25;
    private static final int EVENT_SET_MBDN_DONE = 20;
    private static final int EVENT_SIM_REFRESH = 31;
    private static final int EVENT_SMS_ON_SIM = 21;
    private static final int EVENT_UPDATE_DONE = 14;
    private static final String KEY_SIM_ID = "SIM_ID";
    protected static final String LOG_TAG = "SIMRecords";
    static final int TAG_FULL_NETWORK_NAME = 67;
    static final int TAG_SHORT_NETWORK_NAME = 69;
    static final int TAG_SPDI = 163;
    static final int TAG_SPDI_PLMN_LIST = 128;
    private String[] SIM_RECORDS_PROPERTY_ECC_LIST;
    String cphsOnsl;
    String cphsOnss;
    private int efLanguageToLoad;
    private boolean hasQueryIccId;
    private int iccIdQueryState;
    private boolean isDispose;
    private boolean isValidMBI;
    private int mCallForwardingStatus;
    private byte[] mCphsInfo;
    boolean mCspPlmnEnabled;
    byte[] mEfCPHS_MWI;
    byte[] mEfCff;
    byte[] mEfCfis;
    private byte[] mEfELP;
    String mEfEcc;
    private ArrayList<byte[]> mEfGbanlList;
    byte[] mEfLi;
    byte[] mEfMWIS;
    byte[] mEfPl;
    private byte[] mEfPsismsc;
    private byte[] mEfRat;
    private boolean mEfRatLoaded;
    private byte[] mEfSST;
    private byte[] mEfSmsp;
    private String mGbabp;
    private String[] mGbanl;
    private boolean mIsPhbEfResetDone;
    private String mMenuTitleFromEf;
    private ArrayList<OplRecord> mOperatorList;
    private boolean mPhbReady;
    private boolean mPhbWaitSub;
    private Phone mPhone;
    String mPnnHomeName;
    private ArrayList<OperatorName> mPnnNetworkNames;
    private RadioTechnologyChangedReceiver mRTC;
    private final BroadcastReceiver mReceiver;
    private boolean mSIMInfoReady;
    private String mSimImsi;
    private BroadcastReceiver mSimReceiver;
    int mSlotId;
    private String mSpNameInEfSpn;
    ArrayList<String> mSpdiNetworks;
    int mSpnDisplayCondition;
    SpnOverride mSpnOverride;
    private GetSpnFsmState mSpnState;
    private BroadcastReceiver mSubReceiver;
    private ITelephonyExt mTelephonyExt;
    private UiccCard mUiccCard;
    private UiccController mUiccController;
    UsimServiceTable mUsimServiceTable;
    VoiceMailConstants mVmConfig;
    private static final String[] MCCMNC_CODES_HAVING_3DIGITS_MNC = {"302370", "302720", SimulatedCommands.FAKE_MCC_MNC, "405025", "405026", "405027", "405028", "405029", "405030", "405031", "405032", "405033", "405034", "405035", "405036", "405037", "405038", "405039", "405040", "405041", "405042", "405043", "405044", "405045", "405046", "405047", "405750", "405751", "405752", "405753", "405754", "405755", "405756", "405799", "405800", "405801", "405802", "405803", "405804", "405805", "405806", "405807", "405808", "405809", "405810", "405811", "405812", "405813", "405814", "405815", "405816", "405817", "405818", "405819", "405820", "405821", "405822", "405823", "405824", "405825", "405826", "405827", "405828", "405829", "405830", "405831", "405832", "405833", "405834", "405835", "405836", "405837", "405838", "405839", "405840", "405841", "405842", "405843", "405844", "405845", "405846", "405847", "405848", "405849", "405850", "405851", "405852", "405853", "405875", "405876", "405877", "405878", "405879", "405880", "405881", "405882", "405883", "405884", "405885", "405886", "405908", "405909", "405910", "405911", "405912", "405913", "405914", "405915", "405916", "405917", "405918", "405919", "405920", "405921", "405922", "405923", "405924", "405925", "405926", "405927", "405928", "405929", "405930", "405931", "405932", "502142", "502143", "502145", "502146", "502147", "502148"};
    private static final String[] LANGUAGE_CODE_FOR_LP = {"de", "en", "it", "fr", "es", "nl", "sv", "da", "pt", "fi", "no", "el", "tr", "hu", "pl", UsimPBMemInfo.STRING_NOT_SET, "cs", "he", "ar", "ru", "is", UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET};
    static final String[] SIMRECORD_PROPERTY_RIL_PHB_READY = {"gsm.sim.ril.phbready", "gsm.sim.ril.phbready.2", "gsm.sim.ril.phbready.3", "gsm.sim.ril.phbready.4"};
    static final String[] SIMRECORD_PROPERTY_RIL_PUK1 = {"gsm.sim.retry.puk1", "gsm.sim.retry.puk1.2", "gsm.sim.retry.puk1.3", "gsm.sim.retry.puk1.4"};
    private static final int[] simServiceNumber = {1, 17, 51, 52, 54, 55, 56, 0, 12, 3, 0};
    private static final int[] usimServiceNumber = {0, 19, 45, 46, 48, 49, 51, 71, 12, 2, 0};

    public static class OperatorName {
        public String sFullName;
        public String sShortName;
    }

    public static class OplRecord {
        public int nMaxLAC;
        public int nMinLAC;
        public int nPnnIndex;
        public String sPlmn;
    }

    private static int[] m510xf89fffa2() {
        if (f31x102e4fe != null) {
            return f31x102e4fe;
        }
        int[] iArr = new int[GetSpnFsmState.valuesCustom().length];
        try {
            iArr[GetSpnFsmState.IDLE.ordinal()] = 5;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[GetSpnFsmState.INIT.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[GetSpnFsmState.READ_SPN_3GPP.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[GetSpnFsmState.READ_SPN_CPHS.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[GetSpnFsmState.READ_SPN_SHORT_CPHS.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        f31x102e4fe = iArr;
        return iArr;
    }

    @Override
    public String toString() {
        return "SimRecords: " + super.toString() + " mVmConfig" + this.mVmConfig + " mSpnOverride=mSpnOverride callForwardingEnabled=" + this.mCallForwardingStatus + " spnState=" + this.mSpnState + " mCphsInfo=" + this.mCphsInfo + " mCspPlmnEnabled=" + this.mCspPlmnEnabled + " efMWIS=" + this.mEfMWIS + " efCPHS_MWI=" + this.mEfCPHS_MWI + " mEfCff=" + this.mEfCff + " mEfCfis=" + this.mEfCfis + " getOperatorNumeric=" + getOperatorNumeric();
    }

    public SIMRecords(UiccCardApplication uiccCardApplication, Context context, CommandsInterface commandsInterface) {
        super(uiccCardApplication, context, commandsInterface);
        this.mCphsInfo = null;
        this.mCspPlmnEnabled = true;
        this.mEfMWIS = null;
        this.mEfCPHS_MWI = null;
        this.mEfCff = null;
        this.mEfCfis = null;
        this.mEfLi = null;
        this.mEfPl = null;
        this.mSpdiNetworks = null;
        this.mPnnHomeName = null;
        this.isValidMBI = CRASH_RIL;
        this.mEfRatLoaded = CRASH_RIL;
        this.mEfRat = null;
        this.iccIdQueryState = -1;
        this.efLanguageToLoad = 0;
        this.mIsPhbEfResetDone = CRASH_RIL;
        this.mSimImsi = null;
        this.mEfSST = null;
        this.mEfELP = null;
        this.mEfPsismsc = null;
        this.mEfSmsp = null;
        this.SIM_RECORDS_PROPERTY_ECC_LIST = new String[]{"ril.ecclist", "ril.ecclist1", "ril.ecclist2", "ril.ecclist3"};
        this.mPhbReady = CRASH_RIL;
        this.mPhbWaitSub = CRASH_RIL;
        this.mSIMInfoReady = CRASH_RIL;
        this.mPnnNetworkNames = null;
        this.mOperatorList = null;
        this.mSpNameInEfSpn = null;
        this.mMenuTitleFromEf = null;
        this.isDispose = CRASH_RIL;
        this.mEfEcc = UsimPBMemInfo.STRING_NOT_SET;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    return;
                }
                SIMRecords.this.sendMessage(SIMRecords.this.obtainMessage(37));
            }
        };
        this.mSlotId = uiccCardApplication.getSlotId();
        this.mUiccController = UiccController.getInstance();
        this.mUiccCard = this.mUiccController.getUiccCard(this.mSlotId);
        log("mUiccCard Instance = " + this.mUiccCard);
        this.mPhone = PhoneFactory.getPhone(uiccCardApplication.getPhoneId());
        this.mAdnCache = new AdnRecordCache(this.mFh, commandsInterface, uiccCardApplication);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.ACTION_PHONE_RESTART");
        intent.putExtra(PplSmsFilterExtension.INSTRUCTION_KEY_SIM_ID, this.mSlotId);
        this.mContext.sendBroadcast(intent);
        this.mVmConfig = new VoiceMailConstants();
        this.mSpnOverride = SpnOverride.getInstance();
        this.mRecordsRequested = CRASH_RIL;
        this.mRecordsToLoad = 0;
        this.cphsOnsl = null;
        this.cphsOnss = null;
        this.hasQueryIccId = CRASH_RIL;
        this.mCi.setOnSmsOnSim(this, 21, null);
        this.mCi.registerForIccRefresh(this, 31, null);
        this.mCi.registerForPhbReady(this, 410, null);
        this.mCi.registerForCallForwardingInfo(this, 211, null);
        this.mCi.registerForRadioStateChanged(this, 201, null);
        this.mCi.registerForAvailable(this, 41, null);
        this.mCi.registerForEfCspPlmnModeBitChanged(this, EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED, null);
        this.mCi.registerForMelockChanged(this, EVENT_MELOCK_CHANGED, null);
        this.mCi.registerForImsiRefreshDone(this, 212, null);
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        this.mParentApp.registerForLocked(this, 35, null);
        this.mSimReceiver = new SIMBroadCastReceiver(this, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.dm.LAWMO_WIPE");
        intentFilter.addAction("action_pin_dismiss");
        intentFilter.addAction("action_melock_dismiss");
        intentFilter.addAction(IWorldPhone.ACTION_SHUTDOWN_IPO);
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mSimReceiver, intentFilter);
        this.mSubReceiver = new SubBroadCastReceiver(this, 0 == true ? 1 : 0);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        this.mContext.registerReceiver(this.mSubReceiver, intentFilter2);
        this.mRTC = new RadioTechnologyChangedReceiver(this, 0 == true ? 1 : 0);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.mRTC, intentFilter3);
        log("SIMRecords updateIccRecords");
        if (this.mPhone.getIccPhoneBookInterfaceManager() != null) {
            this.mPhone.getIccPhoneBookInterfaceManager().updateIccRecords(this);
        }
        if (isPhbReady()) {
            log("Phonebook is ready.");
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
        try {
            this.mTelephonyExt = (ITelephonyExt) MPlugin.createInstance(ITelephonyExt.class.getName(), this.mContext);
        } catch (Exception e) {
            loge("Fail to create plug-in");
            e.printStackTrace();
        }
        log("SIMRecords X ctor this=" + this);
        IntentFilter intentFilter4 = new IntentFilter();
        intentFilter4.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter4);
    }

    @Override
    public void dispose() {
        log("Disposing SIMRecords this=" + this);
        this.isDispose = true;
        this.mCi.unregisterForIccRefresh(this);
        this.mCi.unSetOnSmsOnSim(this);
        this.mPhone.setVoiceCallForwardingFlag(1, CRASH_RIL, null);
        this.mCi.unregisterForCallForwardingInfo(this);
        this.mCi.unregisterForPhbReady(this);
        this.mCi.unregisterForRadioStateChanged(this);
        this.mCi.unregisterForEfCspPlmnModeBitChanged(this);
        this.mCi.unregisterForMelockChanged(this);
        this.mParentApp.unregisterForReady(this);
        this.mParentApp.unregisterForLocked(this);
        this.mContext.unregisterReceiver(this.mSimReceiver);
        this.mContext.unregisterReceiver(this.mSubReceiver);
        this.mContext.unregisterReceiver(this.mReceiver);
        this.mContext.unregisterReceiver(this.mRTC);
        this.mPhbWaitSub = CRASH_RIL;
        resetRecords();
        this.mAdnCache.reset();
        setPhbReady(CRASH_RIL);
        this.mIccId = null;
        this.mImsi = null;
        super.dispose();
    }

    protected void finalize() {
        log("finalized");
    }

    protected void resetRecords() {
        this.mImsi = null;
        this.mMsisdn = null;
        this.mVoiceMailNum = null;
        this.mMncLength = -1;
        log("setting0 mMncLength" + this.mMncLength);
        this.mIccId = null;
        this.mFullIccId = null;
        this.mSpnDisplayCondition = -1;
        this.mEfMWIS = null;
        this.mEfCPHS_MWI = null;
        this.mSpdiNetworks = null;
        this.mPnnHomeName = null;
        this.mGid1 = null;
        this.mGid2 = null;
        this.mAdnCache.reset();
        log("SIMRecords: onRadioOffOrNotAvailable set 'gsm.sim.operator.numeric' to operator=null");
        log("update icc_operator_numeric=" + ((Object) null));
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
        this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
        setSystemProperty("gsm.sim.operator.imsi", null);
        setSystemProperty("gsm.sim.operator.default-name", null);
        this.mRecordsRequested = CRASH_RIL;
    }

    @Override
    public String getIMSI() {
        return this.mImsi;
    }

    @Override
    public String getMsisdnNumber() {
        return this.mMsisdn;
    }

    @Override
    public String getGid1() {
        return this.mGid1;
    }

    @Override
    public String getGid2() {
        return this.mGid2;
    }

    @Override
    public UsimServiceTable getUsimServiceTable() {
        return this.mUsimServiceTable;
    }

    private int getExtFromEf(int ef) {
        switch (ef) {
            case IccConstants.EF_MSISDN:
                if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                    return IccConstants.EF_EXT5;
                }
                return IccConstants.EF_EXT1;
            default:
                return IccConstants.EF_EXT1;
        }
    }

    @Override
    public void setMsisdnNumber(String alphaTag, String number, Message onComplete) {
        this.mNewMsisdn = number;
        this.mNewMsisdnTag = alphaTag;
        log("Set MSISDN: " + this.mNewMsisdnTag + " xxxxxxx");
        AdnRecord adn = new AdnRecord(this.mNewMsisdnTag, this.mNewMsisdn);
        new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, null, obtainMessage(30, onComplete));
    }

    @Override
    public String getMsisdnAlphaTag() {
        return this.mMsisdnTag;
    }

    @Override
    public String getVoiceMailNumber() {
        log("getVoiceMailNumber " + this.mVoiceMailNum);
        return this.mVoiceMailNum;
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
        log("setVoiceMailNumber, mIsVoiceMailFixed " + this.mIsVoiceMailFixed + ", mMailboxIndex " + this.mMailboxIndex + ", mMailboxIndex " + this.mMailboxIndex);
        if (this.mIsVoiceMailFixed) {
            AsyncResult.forMessage(onComplete).exception = new IccVmFixedException("Voicemail number is fixed by operator");
            onComplete.sendToTarget();
            return;
        }
        this.mNewVoiceMailNum = voiceNumber;
        this.mNewVoiceMailTag = alphaTag;
        AdnRecord adn = new AdnRecord(this.mNewVoiceMailTag, this.mNewVoiceMailNum);
        if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
            new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, null, obtainMessage(20, onComplete));
            return;
        }
        if (isCphsMailboxEnabled()) {
            log("setVoiceMailNumber,load EF_MAILBOX_CPHS");
            new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, onComplete));
        } else {
            log("setVoiceMailNumber,Update SIM voice mailbox error");
            AsyncResult.forMessage(onComplete).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
            onComplete.sendToTarget();
        }
    }

    @Override
    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        if (line != 1) {
            return;
        }
        try {
            if (this.mEfMWIS != null) {
                this.mEfMWIS[0] = (byte) ((countWaiting != 0 ? 1 : 0) | (this.mEfMWIS[0] & 254));
                if (countWaiting < 0) {
                    this.mEfMWIS[1] = 0;
                } else {
                    this.mEfMWIS[1] = (byte) countWaiting;
                }
                this.mFh.updateEFLinearFixed(IccConstants.EF_MWIS, 1, this.mEfMWIS, null, obtainMessage(14, IccConstants.EF_MWIS, 0));
            }
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                log("[setVoiceMessageWaiting] It is USIM card, skip write CPHS file");
            } else {
                if (this.mEfCPHS_MWI == null) {
                    return;
                }
                this.mEfCPHS_MWI[0] = (byte) ((countWaiting == 0 ? 5 : 10) | (this.mEfCPHS_MWI[0] & 240));
                this.mFh.updateEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, this.mEfCPHS_MWI, obtainMessage(14, Integer.valueOf(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS)));
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            logw("Error saving voice mail state to SIM. Probably malformed SIM record", ex);
        }
    }

    private boolean validEfCfis(byte[] data) {
        if (data == null || data[0] < 1 || data[0] > 4) {
            return CRASH_RIL;
        }
        return true;
    }

    @Override
    public int getVoiceMessageCount() {
        int countVoiceMessages = 0;
        if (this.mEfMWIS != null) {
            boolean voiceMailWaiting = (this.mEfMWIS[0] & 1) != 0 ? true : CRASH_RIL;
            countVoiceMessages = this.mEfMWIS[1] & PplMessageManager.Type.INVALID;
            if (voiceMailWaiting && countVoiceMessages == 0) {
                countVoiceMessages = -1;
            }
            log(" VoiceMessageCount from SIM MWIS = " + countVoiceMessages);
        } else if (this.mEfCPHS_MWI != null) {
            int indicator = this.mEfCPHS_MWI[0] & 15;
            if (indicator == 10) {
                countVoiceMessages = -1;
            } else if (indicator == 5) {
                countVoiceMessages = 0;
            }
            log(" VoiceMessageCount from SIM CPHS = " + countVoiceMessages);
        }
        return countVoiceMessages;
    }

    @Override
    public int getVoiceCallForwardingFlag() {
        return this.mCallForwardingStatus;
    }

    @Override
    public void setVoiceCallForwardingFlag(int line, boolean enable, String dialNumber) {
        Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag: " + enable);
        if (line != 1) {
            return;
        }
        this.mCallForwardingStatus = enable ? 1 : 0;
        Rlog.d(LOG_TAG, " mRecordsEventsRegistrants: size=" + this.mRecordsEventsRegistrants.size());
        this.mRecordsEventsRegistrants.notifyResult(1);
    }

    @Override
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (!fileChanged) {
            return;
        }
        fetchSimRecords();
    }

    @Override
    public String getOperatorNumeric() {
        if (this.mImsi == null) {
            log("getOperatorNumeric: IMSI == null");
            return null;
        }
        if (this.mMncLength == -1 || this.mMncLength == 0) {
            log("getSIMOperatorNumeric: bad mncLength");
            return null;
        }
        return this.mImsi.substring(0, this.mMncLength + 3);
    }

    @Override
    public String getSIMCPHSOns() {
        if (this.cphsOnsl != null) {
            return this.cphsOnsl;
        }
        return this.cphsOnss;
    }

    @Override
    public void handleMessage(Message msg) {
        int i;
        int i2;
        String str;
        int length;
        int i3;
        int i4;
        String str2;
        int i5;
        int i6;
        int i7;
        boolean isRecordLoadResponse = CRASH_RIL;
        try {
            if (this.mDestroyed.get()) {
                loge("Received message " + msg + "[" + msg.what + "]  while being destroyed. Ignoring.");
                return;
            }
            try {
                switch (msg.what) {
                    case 1:
                        onReady();
                        fetchEccList();
                        break;
                    case 3:
                        isRecordLoadResponse = true;
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception != null) {
                            loge("Exception querying IMSI, Exception:" + ar.exception);
                        } else {
                            this.mImsi = (String) ar.result;
                            if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                                loge("invalid IMSI " + this.mImsi);
                                this.mImsi = null;
                            }
                            log("IMSI: mMncLength=" + this.mMncLength);
                            log("IMSI: " + this.mImsi.substring(0, 6) + "xxxxxxx");
                            setSystemProperty("gsm.sim.operator.imsi", this.mImsi);
                            if (this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) {
                                if (this.mImsi != null && this.mImsi.length() >= 6) {
                                    String mccmncCode = this.mImsi.substring(0, 6);
                                    String[] strArr = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                    int i8 = 0;
                                    int length2 = strArr.length;
                                    while (true) {
                                        if (i8 < length2) {
                                            String mccmnc = strArr[i8];
                                            if (mccmnc.equals(mccmncCode)) {
                                                this.mMncLength = 3;
                                                log("IMSI: setting1 mMncLength=" + this.mMncLength);
                                            } else {
                                                i8++;
                                            }
                                        }
                                    }
                                }
                            }
                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                try {
                                    int mcc = Integer.parseInt(this.mImsi.substring(0, 3));
                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                                    log("setting2 mMncLength=" + this.mMncLength);
                                } catch (NumberFormatException e) {
                                    this.mMncLength = 0;
                                    loge("Corrupt IMSI! setting3 mMncLength=" + this.mMncLength);
                                }
                            }
                            if (this.mMncLength != 0 && this.mMncLength != -1) {
                                log("update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
                                updateConfiguration(this.mImsi.substring(0, this.mMncLength + 3));
                            }
                            this.mImsiReadyRegistrants.notifyRegistrants();
                        }
                        break;
                    case 4:
                        isRecordLoadResponse = true;
                        AsyncResult ar2 = (AsyncResult) msg.obj;
                        byte[] data = (byte[]) ar2.result;
                        if (ar2.exception == null) {
                            this.mIccId = IccUtils.bcdToString(data, 0, data.length);
                            this.mFullIccId = IccUtils.bchToString(data, 0, data.length);
                            log("iccid: " + SubscriptionInfo.givePrintableIccid(this.mFullIccId));
                        }
                        break;
                    case 5:
                        isRecordLoadResponse = true;
                        AsyncResult ar3 = (AsyncResult) msg.obj;
                        byte[] data2 = (byte[]) ar3.result;
                        boolean isValidMbdn = CRASH_RIL;
                        if (ar3.exception == null) {
                            log("EF_MBI: " + IccUtils.bytesToHexString(data2));
                            this.mMailboxIndex = data2[0] & PplMessageManager.Type.INVALID;
                            if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
                                log("Got valid mailbox number for MBDN");
                                isValidMbdn = true;
                                this.isValidMBI = true;
                            }
                        }
                        this.mRecordsToLoad++;
                        if (isValidMbdn) {
                            log("EVENT_GET_MBI_DONE, to load EF_MBDN");
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                        } else if (isCphsMailboxEnabled()) {
                            log("EVENT_GET_MBI_DONE, to load EF_MAILBOX_CPHS");
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                        } else {
                            log("EVENT_GET_MBI_DONE, do nothing");
                            this.mRecordsToLoad--;
                        }
                        break;
                    case 6:
                    case 11:
                        this.mVoiceMailNum = null;
                        this.mVoiceMailTag = null;
                        isRecordLoadResponse = true;
                        AsyncResult ar4 = (AsyncResult) msg.obj;
                        if (ar4.exception != null) {
                            loge("Invalid or missing EF" + (msg.what == 11 ? "[MAILBOX]" : "[MBDN]"));
                            if (msg.what == 6) {
                                this.mRecordsToLoad++;
                                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                            }
                        } else {
                            AdnRecord adn = (AdnRecord) ar4.result;
                            log("VM: " + adn + (msg.what == 11 ? " EF[MAILBOX]" : " EF[MBDN]"));
                            if (adn.isEmpty() && msg.what == 6) {
                                this.mRecordsToLoad++;
                                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                            } else {
                                this.mVoiceMailNum = adn.getNumber();
                                this.mVoiceMailTag = adn.getAlphaTag();
                            }
                        }
                        break;
                    case 7:
                        isRecordLoadResponse = true;
                        AsyncResult ar5 = (AsyncResult) msg.obj;
                        byte[] data3 = (byte[]) ar5.result;
                        log("EF_MWIS : " + IccUtils.bytesToHexString(data3));
                        if (ar5.exception != null) {
                            loge("EVENT_GET_MWIS_DONE exception = " + ar5.exception);
                        } else if ((data3[0] & PplMessageManager.Type.INVALID) == 255) {
                            log("SIMRecords: Uninitialized record MWIS");
                        } else {
                            this.mEfMWIS = data3;
                        }
                        break;
                    case 8:
                        isRecordLoadResponse = true;
                        AsyncResult ar6 = (AsyncResult) msg.obj;
                        byte[] data4 = (byte[]) ar6.result;
                        log("EF_CPHS_MWI: " + IccUtils.bytesToHexString(data4));
                        if (ar6.exception != null) {
                            loge("EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE exception = " + ar6.exception);
                        } else {
                            this.mEfCPHS_MWI = data4;
                        }
                        break;
                    case 9:
                        isRecordLoadResponse = true;
                        try {
                            AsyncResult ar7 = (AsyncResult) msg.obj;
                            byte[] data5 = (byte[]) ar7.result;
                            if (ar7.exception != null) {
                                if (this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) {
                                    if (this.mImsi != null && this.mImsi.length() >= 6) {
                                        String mccmncCode2 = this.mImsi.substring(0, 6);
                                        log("mccmncCode=" + mccmncCode2);
                                        String[] strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                        int i9 = 0;
                                        int length3 = strArr2.length;
                                        while (true) {
                                            if (i9 < length3) {
                                                String mccmnc2 = strArr2[i9];
                                                if (mccmnc2.equals(mccmncCode2)) {
                                                    this.mMncLength = 3;
                                                    log("setting6 mMncLength=" + this.mMncLength);
                                                } else {
                                                    i9++;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (this.mMncLength == 0 || this.mMncLength == -1) {
                                    if (this.mImsi != null) {
                                        try {
                                            int mcc2 = Integer.parseInt(this.mImsi.substring(0, 3));
                                            this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc2);
                                            log("setting7 mMncLength=" + this.mMncLength);
                                        } catch (NumberFormatException e2) {
                                            this.mMncLength = 0;
                                            loge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                        }
                                    } else {
                                        this.mMncLength = 0;
                                        log("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                    }
                                }
                                if (this.mImsi != null && this.mMncLength != 0) {
                                    log("update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
                                    updateConfiguration(this.mImsi.substring(0, this.mMncLength + 3));
                                }
                            } else {
                                log("EF_AD: " + IccUtils.bytesToHexString(data5));
                                if (data5.length < 3) {
                                    log("Corrupt AD data on SIM");
                                    if (this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) {
                                        if (this.mImsi != null && this.mImsi.length() >= 6) {
                                            String mccmncCode3 = this.mImsi.substring(0, 6);
                                            log("mccmncCode=" + mccmncCode3);
                                            String[] strArr3 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                            int i10 = 0;
                                            int length4 = strArr3.length;
                                            while (true) {
                                                if (i10 < length4) {
                                                    String mccmnc3 = strArr3[i10];
                                                    if (mccmnc3.equals(mccmncCode3)) {
                                                        this.mMncLength = 3;
                                                        log("setting6 mMncLength=" + this.mMncLength);
                                                    } else {
                                                        i10++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (this.mMncLength == 0 || this.mMncLength == -1) {
                                        if (this.mImsi != null) {
                                            try {
                                                int mcc3 = Integer.parseInt(this.mImsi.substring(0, 3));
                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc3);
                                                log("setting7 mMncLength=" + this.mMncLength);
                                            } catch (NumberFormatException e3) {
                                                this.mMncLength = 0;
                                                loge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                            }
                                        } else {
                                            this.mMncLength = 0;
                                            log("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                        }
                                    }
                                    if (this.mImsi != null && this.mMncLength != 0) {
                                        log("update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
                                        updateConfiguration(this.mImsi.substring(0, this.mMncLength + 3));
                                    }
                                } else {
                                    if ((data5[0] & 1) == 1 && (data5[2] & 1) == 1) {
                                        log("SIMRecords: Cipher is enable");
                                    }
                                    if (data5.length == 3) {
                                        log("MNC length not present in EF_AD");
                                        if (this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) {
                                            if (this.mImsi != null && this.mImsi.length() >= 6) {
                                                String mccmncCode4 = this.mImsi.substring(0, 6);
                                                log("mccmncCode=" + mccmncCode4);
                                                String[] strArr4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                                int i11 = 0;
                                                int length5 = strArr4.length;
                                                while (true) {
                                                    if (i11 < length5) {
                                                        String mccmnc4 = strArr4[i11];
                                                        if (mccmnc4.equals(mccmncCode4)) {
                                                            this.mMncLength = 3;
                                                            log("setting6 mMncLength=" + this.mMncLength);
                                                        } else {
                                                            i11++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (this.mMncLength == 0 || this.mMncLength == -1) {
                                            if (this.mImsi != null) {
                                                try {
                                                    int mcc4 = Integer.parseInt(this.mImsi.substring(0, 3));
                                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc4);
                                                    log("setting7 mMncLength=" + this.mMncLength);
                                                } catch (NumberFormatException e4) {
                                                    this.mMncLength = 0;
                                                    loge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                                }
                                            } else {
                                                this.mMncLength = 0;
                                                log("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                            }
                                        }
                                        if (this.mImsi != null && this.mMncLength != 0) {
                                            log("update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
                                            updateConfiguration(this.mImsi.substring(0, this.mMncLength + 3));
                                        }
                                    } else {
                                        this.mMncLength = data5[3] & 15;
                                        log("setting4 mMncLength=" + this.mMncLength);
                                        if (this.mMncLength == 15) {
                                            this.mMncLength = 0;
                                            log("setting5 mMncLength=" + this.mMncLength);
                                        } else if (this.mMncLength != 2 && this.mMncLength != 3) {
                                            this.mMncLength = -1;
                                            log("setting5 mMncLength=" + this.mMncLength);
                                        }
                                        if (this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) {
                                            if (this.mImsi != null && this.mImsi.length() >= 6) {
                                                String mccmncCode5 = this.mImsi.substring(0, 6);
                                                log("mccmncCode=" + mccmncCode5);
                                                String[] strArr5 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                                int i12 = 0;
                                                int length6 = strArr5.length;
                                                while (true) {
                                                    if (i12 < length6) {
                                                        String mccmnc5 = strArr5[i12];
                                                        if (mccmnc5.equals(mccmncCode5)) {
                                                            this.mMncLength = 3;
                                                            log("setting6 mMncLength=" + this.mMncLength);
                                                        } else {
                                                            i12++;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (this.mMncLength == 0 || this.mMncLength == -1) {
                                            if (this.mImsi != null) {
                                                try {
                                                    int mcc5 = Integer.parseInt(this.mImsi.substring(0, 3));
                                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(mcc5);
                                                    log("setting7 mMncLength=" + this.mMncLength);
                                                } catch (NumberFormatException e5) {
                                                    this.mMncLength = 0;
                                                    loge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                                }
                                            } else {
                                                this.mMncLength = 0;
                                                log("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                            }
                                        }
                                        if (this.mImsi != null && this.mMncLength != 0) {
                                            log("update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
                                            updateConfiguration(this.mImsi.substring(0, this.mMncLength + 3));
                                        }
                                    }
                                }
                            }
                            break;
                        } finally {
                            if (i != i2) {
                                if (i7 == 0) {
                                    if (str != null) {
                                        if (length >= i3) {
                                            while (true) {
                                            }
                                        }
                                    }
                                }
                            }
                            if (i4 != 0) {
                                if (i5 == i6) {
                                    if (str2 != null) {
                                        try {
                                            break;
                                        } catch (NumberFormatException e6) {
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case 10:
                        isRecordLoadResponse = true;
                        AsyncResult ar8 = (AsyncResult) msg.obj;
                        if (ar8.exception != null) {
                            loge("Invalid or missing EF[MSISDN]");
                        } else {
                            AdnRecord adn2 = (AdnRecord) ar8.result;
                            this.mMsisdn = adn2.getNumber();
                            this.mMsisdnTag = adn2.getAlphaTag();
                            this.mRecordsEventsRegistrants.notifyResult(100);
                            log("MSISDN: xxxxxxx");
                        }
                        break;
                    case 12:
                        log("EF_SPN loaded and try to extract: ");
                        isRecordLoadResponse = true;
                        AsyncResult ar9 = (AsyncResult) msg.obj;
                        if (ar9 == null || ar9.exception != null) {
                            loge(": read spn fail!");
                            this.mSpnDisplayCondition = -1;
                        } else {
                            log("getSpnFsm, Got data from EF_SPN");
                            byte[] data6 = (byte[]) ar9.result;
                            this.mSpnDisplayCondition = data6[0] & PplMessageManager.Type.INVALID;
                            if (this.mSpnDisplayCondition == 255) {
                                this.mSpnDisplayCondition = -1;
                            }
                            setServiceProviderName(IccUtils.adnStringFieldToString(data6, 1, data6.length - 1));
                            this.mSpNameInEfSpn = getServiceProviderName();
                            if (this.mSpNameInEfSpn != null && this.mSpNameInEfSpn.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                                log("set spNameInEfSpn to null because parsing result is empty");
                                this.mSpNameInEfSpn = null;
                            }
                            log("Load EF_SPN: " + getServiceProviderName() + " spnDisplayCondition: " + this.mSpnDisplayCondition);
                            this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), getServiceProviderName());
                        }
                        break;
                    case 13:
                        isRecordLoadResponse = true;
                        AsyncResult ar10 = (AsyncResult) msg.obj;
                        byte[] data7 = (byte[]) ar10.result;
                        if (ar10.exception == null) {
                            parseEfSpdi(data7);
                        }
                        break;
                    case 14:
                        AsyncResult ar11 = (AsyncResult) msg.obj;
                        if (ar11.exception != null) {
                            logw("update failed. ", ar11.exception);
                        }
                        break;
                    case 15:
                        isRecordLoadResponse = true;
                        AsyncResult ar12 = (AsyncResult) msg.obj;
                        if (ar12.exception == null) {
                            parseEFpnn((ArrayList) ar12.result);
                        }
                        break;
                    case 17:
                        isRecordLoadResponse = true;
                        AsyncResult ar13 = (AsyncResult) msg.obj;
                        byte[] data8 = (byte[]) ar13.result;
                        if (ar13.exception == null) {
                            this.mUsimServiceTable = new UsimServiceTable(data8);
                            log("SST: " + this.mUsimServiceTable);
                            this.mEfSST = data8;
                            fetchMbiRecords();
                            fetchMwisRecords();
                            fetchPnnAndOpl();
                            fetchSpn();
                            fetchSmsp();
                            fetchGbaRecords();
                        }
                        break;
                    case 18:
                        isRecordLoadResponse = true;
                        AsyncResult ar14 = (AsyncResult) msg.obj;
                        if (ar14.exception == null) {
                            handleSmses((ArrayList) ar14.result);
                        }
                        break;
                    case 19:
                        Rlog.i("ENF", "marked read: sms " + msg.arg1);
                        break;
                    case 20:
                        isRecordLoadResponse = CRASH_RIL;
                        AsyncResult ar15 = (AsyncResult) msg.obj;
                        log("EVENT_SET_MBDN_DONE ex:" + ar15.exception);
                        if (ar15.exception == null) {
                            this.mVoiceMailNum = this.mNewVoiceMailNum;
                            this.mVoiceMailTag = this.mNewVoiceMailTag;
                        }
                        if (isCphsMailboxEnabled()) {
                            AdnRecord adn3 = new AdnRecord(this.mVoiceMailTag, this.mVoiceMailNum);
                            Message onCphsCompleted = (Message) ar15.userObj;
                            if (ar15.exception == null && ar15.userObj != null) {
                                AsyncResult.forMessage((Message) ar15.userObj).exception = null;
                                ((Message) ar15.userObj).sendToTarget();
                                log("Callback with MBDN successful.");
                                onCphsCompleted = null;
                            }
                            new AdnRecordLoader(this.mFh).updateEF(adn3, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, onCphsCompleted));
                        } else if (ar15.userObj != null) {
                            Resources resource = Resources.getSystem();
                            if (ar15.exception == null || !resource.getBoolean(R.^attr-private.maxItems)) {
                                AsyncResult.forMessage((Message) ar15.userObj).exception = ar15.exception;
                            } else {
                                AsyncResult.forMessage((Message) ar15.userObj).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
                            }
                            ((Message) ar15.userObj).sendToTarget();
                        }
                        break;
                    case 21:
                        isRecordLoadResponse = CRASH_RIL;
                        AsyncResult ar16 = (AsyncResult) msg.obj;
                        int[] index = (int[]) ar16.result;
                        if (ar16.exception == null && index.length == 1) {
                            log("READ EF_SMS RECORD index=" + index[0]);
                            this.mFh.loadEFLinearFixed(IccConstants.EF_SMS, index[0], obtainMessage(22));
                        } else {
                            loge("Error on SMS_ON_SIM with exp " + ar16.exception + " length " + index.length);
                        }
                        break;
                    case 22:
                        isRecordLoadResponse = CRASH_RIL;
                        AsyncResult ar17 = (AsyncResult) msg.obj;
                        if (ar17.exception == null) {
                            handleSms((byte[]) ar17.result);
                        } else {
                            loge("Error on GET_SMS with exp " + ar17.exception);
                        }
                        break;
                    case 24:
                        isRecordLoadResponse = true;
                        AsyncResult ar18 = (AsyncResult) msg.obj;
                        byte[] data9 = (byte[]) ar18.result;
                        if (ar18.exception != null) {
                            this.mEfCff = null;
                        } else {
                            log("EF_CFF_CPHS: " + IccUtils.bytesToHexString(data9));
                            this.mEfCff = data9;
                        }
                        break;
                    case 25:
                        isRecordLoadResponse = CRASH_RIL;
                        AsyncResult ar19 = (AsyncResult) msg.obj;
                        if (ar19.exception == null) {
                            this.mVoiceMailNum = this.mNewVoiceMailNum;
                            this.mVoiceMailTag = this.mNewVoiceMailTag;
                        } else {
                            loge("Set CPHS MailBox with exception: " + ar19.exception);
                        }
                        if (ar19.userObj != null) {
                            log("Callback with CPHS MB successful.");
                            AsyncResult.forMessage((Message) ar19.userObj).exception = ar19.exception;
                            ((Message) ar19.userObj).sendToTarget();
                        }
                        break;
                    case 26:
                        isRecordLoadResponse = true;
                        AsyncResult ar20 = (AsyncResult) msg.obj;
                        if (ar20.exception == null) {
                            this.mCphsInfo = (byte[]) ar20.result;
                            log("iCPHS: " + IccUtils.bytesToHexString(this.mCphsInfo));
                            if (!this.isValidMBI && isCphsMailboxEnabled()) {
                                this.mRecordsToLoad++;
                                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                            }
                        }
                        break;
                    case 30:
                        isRecordLoadResponse = CRASH_RIL;
                        AsyncResult ar21 = (AsyncResult) msg.obj;
                        if (ar21.exception == null) {
                            this.mMsisdn = this.mNewMsisdn;
                            this.mMsisdnTag = this.mNewMsisdnTag;
                            this.mRecordsEventsRegistrants.notifyResult(100);
                            log("Success to update EF[MSISDN]");
                        }
                        if (ar21.userObj != null) {
                            AsyncResult.forMessage((Message) ar21.userObj).exception = ar21.exception;
                            ((Message) ar21.userObj).sendToTarget();
                        }
                        break;
                    case 31:
                        isRecordLoadResponse = CRASH_RIL;
                        AsyncResult ar22 = (AsyncResult) msg.obj;
                        log("Sim REFRESH with exception: " + ar22.exception);
                        if (ar22.exception == null) {
                            handleSimRefresh((IccRefreshResponse) ar22.result);
                        }
                        break;
                    case 32:
                        isRecordLoadResponse = true;
                        AsyncResult ar23 = (AsyncResult) msg.obj;
                        byte[] data10 = (byte[]) ar23.result;
                        if (ar23.exception != null) {
                            this.mEfCfis = null;
                        } else {
                            log("EF_CFIS: " + IccUtils.bytesToHexString(data10));
                            this.mEfCfis = data10;
                        }
                        break;
                    case 33:
                        isRecordLoadResponse = true;
                        AsyncResult ar24 = (AsyncResult) msg.obj;
                        if (ar24.exception != null) {
                            loge("Exception in fetching EF_CSP data " + ar24.exception);
                        } else {
                            byte[] data11 = (byte[]) ar24.result;
                            log("EF_CSP: " + IccUtils.bytesToHexString(data11));
                            handleEfCspData(data11);
                        }
                        break;
                    case 34:
                        isRecordLoadResponse = true;
                        AsyncResult ar25 = (AsyncResult) msg.obj;
                        byte[] data12 = (byte[]) ar25.result;
                        if (ar25.exception != null) {
                            loge("Exception in get GID1 " + ar25.exception);
                            this.mGid1 = null;
                        } else {
                            this.mGid1 = IccUtils.bytesToHexString(data12);
                            log("GID1: " + this.mGid1);
                        }
                        break;
                    case 35:
                        onLocked();
                        break;
                    case 36:
                        isRecordLoadResponse = true;
                        AsyncResult ar26 = (AsyncResult) msg.obj;
                        byte[] data13 = (byte[]) ar26.result;
                        if (ar26.exception != null) {
                            loge("Exception in get GID2 " + ar26.exception);
                            this.mGid2 = null;
                        } else {
                            this.mGid2 = IccUtils.bytesToHexString(data13);
                            log("GID2: " + this.mGid2);
                        }
                        break;
                    case 37:
                        handleCarrierNameOverride();
                        break;
                    case 41:
                        if (this.mTelephonyExt.isSetLanguageBySIM()) {
                            fetchLanguageIndicator();
                        }
                        this.mMsisdn = UsimPBMemInfo.STRING_NOT_SET;
                        this.mRecordsEventsRegistrants.notifyResult(100);
                        break;
                    case 42:
                        AsyncResult ar27 = (AsyncResult) msg.obj;
                        byte[] data14 = (byte[]) ar27.result;
                        if (ar27.exception == null) {
                            log("EF_LI: " + IccUtils.bytesToHexString(data14));
                            this.mEfLi = data14;
                        }
                        onLanguageFileLoaded();
                        break;
                    case 43:
                        AsyncResult ar28 = (AsyncResult) msg.obj;
                        byte[] data15 = (byte[]) ar28.result;
                        if (ar28.exception == null) {
                            log("EF_ELP: " + IccUtils.bytesToHexString(data15));
                            this.mEfELP = data15;
                        }
                        onLanguageFileLoaded();
                        break;
                    case 53:
                        log("[sume receive response message");
                        isRecordLoadResponse = true;
                        AsyncResult ar29 = (AsyncResult) msg.obj;
                        if (ar29 == null || ar29.exception != null) {
                            if (ar29.exception != null) {
                                loge("[sume exception in AsyncResult: " + ar29.exception.getClass().getName());
                            } else {
                                log("[sume null AsyncResult");
                            }
                            this.mMenuTitleFromEf = null;
                        } else {
                            byte[] data16 = (byte[]) ar29.result;
                            if (data16 != null && data16.length >= 2) {
                                int tag = data16[0] & PplMessageManager.Type.INVALID;
                                int len = data16[1] & PplMessageManager.Type.INVALID;
                                log("[sume tag = " + tag + ", len = " + len);
                                this.mMenuTitleFromEf = IccUtils.adnStringFieldToString(data16, 2, len);
                                log("[sume menu title is " + this.mMenuTitleFromEf);
                            }
                        }
                        break;
                    case 102:
                        log("handleMessage (EVENT_GET_SIM_ECC_DONE)");
                        AsyncResult ar30 = (AsyncResult) msg.obj;
                        if (ar30.exception != null) {
                            loge("Get SIM ecc with exception: " + ar30.exception);
                        } else {
                            this.mEfEcc = UsimPBMemInfo.STRING_NOT_SET;
                            byte[] data17 = (byte[]) ar30.result;
                            for (int i13 = 0; i13 + 2 < data17.length; i13 += 3) {
                                String eccNum = IccUtils.bcdToString(data17, i13, 3);
                                if (eccNum != null && !eccNum.equals(UsimPBMemInfo.STRING_NOT_SET) && !this.mEfEcc.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                                    this.mEfEcc += ";";
                                }
                                this.mEfEcc += eccNum + ",0";
                            }
                            this.mEfEcc += ";112,0;911,0";
                            log("SIM mEfEcc is " + this.mEfEcc);
                            SystemProperties.set(this.SIM_RECORDS_PROPERTY_ECC_LIST[this.mSlotId], this.mEfEcc);
                        }
                        break;
                    case EVENT_GET_USIM_ECC_DONE:
                        log("handleMessage (EVENT_GET_USIM_ECC_DONE)");
                        AsyncResult ar31 = (AsyncResult) msg.obj;
                        if (ar31.exception != null) {
                            loge("Get USIM ecc with exception: " + ar31.exception);
                        } else {
                            ArrayList eccRecords = (ArrayList) ar31.result;
                            int count = eccRecords.size();
                            this.mEfEcc = UsimPBMemInfo.STRING_NOT_SET;
                            for (int i14 = 0; i14 < count; i14++) {
                                byte[] data18 = (byte[]) eccRecords.get(i14);
                                log("USIM EF_ECC record " + count + ": " + IccUtils.bytesToHexString(data18));
                                String eccNum2 = IccUtils.bcdToString(data18, 0, 3);
                                if (eccNum2 != null && !eccNum2.equals(UsimPBMemInfo.STRING_NOT_SET) && !this.mEfEcc.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                                    this.mEfEcc += ";";
                                }
                                this.mEfEcc += eccNum2 + ",0";
                            }
                            this.mEfEcc += ";112,0;911,0";
                            log("USIM mEfEcc is " + this.mEfEcc);
                            SystemProperties.set(this.SIM_RECORDS_PROPERTY_ECC_LIST[this.mSlotId], this.mEfEcc);
                        }
                        break;
                    case 104:
                        isRecordLoadResponse = true;
                        AsyncResult ar32 = (AsyncResult) msg.obj;
                        if (ar32.exception == null) {
                            parseEFopl((ArrayList) ar32.result);
                        }
                        break;
                    case 105:
                        log("handleMessage (EVENT_GET_CPHSONS_DONE)");
                        isRecordLoadResponse = true;
                        AsyncResult ar33 = (AsyncResult) msg.obj;
                        if (ar33 != null && ar33.exception == null) {
                            byte[] data19 = (byte[]) ar33.result;
                            this.cphsOnsl = IccUtils.adnStringFieldToString(data19, 0, data19.length);
                            log("Load EF_SPN_CPHS: " + this.cphsOnsl);
                        }
                        break;
                    case 106:
                        log("handleMessage (EVENT_GET_SHORT_CPHSONS_DONE)");
                        isRecordLoadResponse = true;
                        AsyncResult ar34 = (AsyncResult) msg.obj;
                        if (ar34 != null && ar34.exception == null) {
                            byte[] data20 = (byte[]) ar34.result;
                            this.cphsOnss = IccUtils.adnStringFieldToString(data20, 0, data20.length);
                            log("Load EF_SPN_SHORT_CPHS: " + this.cphsOnss);
                        }
                        break;
                    case 200:
                        boolean isReady = isPhbReady();
                        log("[EVENT_DELAYED_SEND_PHB_CHANGE] isReady : " + isReady);
                        broadcastPhbStateChangedIntent(isReady);
                        break;
                    case EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED:
                        AsyncResult ar35 = (AsyncResult) msg.obj;
                        if (ar35 != null && ar35.exception == null) {
                            processEfCspPlmnModeBitUrc(((int[]) ar35.result)[0]);
                        }
                        break;
                    case EVENT_GET_RAT_DONE:
                        log("handleMessage (EVENT_GET_RAT_DONE)");
                        isRecordLoadResponse = true;
                        AsyncResult ar36 = (AsyncResult) msg.obj;
                        this.mEfRatLoaded = true;
                        if (ar36 == null || ar36.exception != null) {
                            log("load EF_RAT fail");
                            this.mEfRat = null;
                            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                                boradcastEfRatContentNotify(256);
                            } else {
                                boradcastEfRatContentNotify(512);
                            }
                        } else {
                            this.mEfRat = (byte[]) ar36.result;
                            log("load EF_RAT complete: " + ((int) this.mEfRat[0]));
                            boradcastEfRatContentNotify(512);
                        }
                        break;
                    case EVENT_GET_PSISMSC_DONE:
                        isRecordLoadResponse = true;
                        AsyncResult ar37 = (AsyncResult) msg.obj;
                        byte[] data21 = (byte[]) ar37.result;
                        if (ar37.exception == null) {
                            log("EF_PSISMSC: " + IccUtils.bytesToHexString(data21));
                            if (data21 != null) {
                                this.mEfPsismsc = data21;
                            }
                        }
                        break;
                    case 208:
                        isRecordLoadResponse = true;
                        AsyncResult ar38 = (AsyncResult) msg.obj;
                        byte[] data22 = (byte[]) ar38.result;
                        if (ar38.exception == null) {
                            log("EF_SMSP: " + IccUtils.bytesToHexString(data22));
                            if (data22 != null) {
                                this.mEfSmsp = data22;
                            }
                        }
                        break;
                    case EVENT_GET_GBABP_DONE:
                        isRecordLoadResponse = true;
                        AsyncResult ar39 = (AsyncResult) msg.obj;
                        if (ar39.exception == null) {
                            this.mGbabp = IccUtils.bytesToHexString((byte[]) ar39.result);
                            log("EF_GBABP=" + this.mGbabp);
                        } else {
                            loge("Error on GET_GBABP with exp " + ar39.exception);
                        }
                        break;
                    case EVENT_GET_GBANL_DONE:
                        isRecordLoadResponse = true;
                        AsyncResult ar40 = (AsyncResult) msg.obj;
                        if (ar40.exception == null) {
                            this.mEfGbanlList = (ArrayList) ar40.result;
                            log("GET_GBANL record count: " + this.mEfGbanlList.size());
                        } else {
                            loge("Error on GET_GBANL with exp " + ar40.exception);
                        }
                        break;
                    case 211:
                        AsyncResult ar41 = (AsyncResult) msg.obj;
                        if (ar41 != null && ar41.exception == null && ar41.result != null) {
                            int[] cfuResult = (int[]) ar41.result;
                            log("handle EVENT_CFU_IND, setVoiceCallForwardingFlag:" + cfuResult[0]);
                            this.mPhone.setVoiceCallForwardingFlag(1, cfuResult[0] == 1 ? true : CRASH_RIL, null);
                        }
                        break;
                    case 212:
                        log("handleMessage (EVENT_IMSI_REFRESH_QUERY) mImsi= " + this.mImsi);
                        this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(213));
                        break;
                    case 213:
                        log("handleMessage (EVENT_IMSI_REFRESH_QUERY_DONE)");
                        AsyncResult ar42 = (AsyncResult) msg.obj;
                        if (ar42.exception != null) {
                            loge("Exception querying IMSI, Exception:" + ar42.exception);
                        } else {
                            this.mImsi = (String) ar42.result;
                            if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                                loge("invalid IMSI " + this.mImsi);
                                this.mImsi = null;
                            }
                            log("IMSI: mMncLength=" + this.mMncLength);
                            log("IMSI: " + this.mImsi.substring(0, 6) + "xxxxxxx");
                            setSystemProperty("gsm.sim.operator.imsi", this.mImsi);
                            if (this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) {
                                if (this.mImsi != null && this.mImsi.length() >= 6) {
                                    String mccmncRefresh = this.mImsi.substring(0, 6);
                                    String[] strArr6 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                    int i15 = 0;
                                    int length7 = strArr6.length;
                                    while (true) {
                                        if (i15 < length7) {
                                            String mccmncR = strArr6[i15];
                                            if (mccmncR.equals(mccmncRefresh)) {
                                                this.mMncLength = 3;
                                                log("IMSI: setting1 mMncLength=" + this.mMncLength);
                                            } else {
                                                i15++;
                                            }
                                        }
                                    }
                                }
                            }
                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                try {
                                    int mccR = Integer.parseInt(this.mImsi.substring(0, 3));
                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(mccR);
                                    log("setting2 mMncLength=" + this.mMncLength);
                                } catch (NumberFormatException e7) {
                                    this.mMncLength = 0;
                                    loge("Corrupt IMSI! setting3 mMncLength=" + this.mMncLength);
                                }
                            }
                            if (this.mMncLength != 0 && this.mMncLength != -1) {
                                log("update mccmnc=" + this.mImsi.substring(0, this.mMncLength + 3));
                                updateConfiguration(this.mImsi.substring(0, this.mMncLength + 3));
                            }
                            if (!this.mImsi.equals(this.mSimImsi)) {
                                this.mSimImsi = this.mImsi;
                                this.mImsiReadyRegistrants.notifyRegistrants();
                                log("SimRecords: mImsiReadyRegistrants.notifyRegistrants");
                            }
                            if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
                                onAllRecordsLoaded();
                            }
                        }
                        break;
                    case EVENT_MELOCK_CHANGED:
                        log("handleMessage (EVENT_MELOCK_CHANGED)");
                        AsyncResult ar43 = (AsyncResult) msg.obj;
                        if (ar43 != null && ar43.exception == null && ar43.result != null) {
                            int[] simMelockEvent = (int[]) ar43.result;
                            log("sim melock event = " + simMelockEvent[0]);
                            RebootClickListener listener = new RebootClickListener(this, null);
                            if (simMelockEvent[0] == 0) {
                                AlertDialog alertDialog = new AlertDialog.Builder(this.mContext).setTitle("Unlock Phone").setMessage("Please restart the phone now since unlock setting has changed.").setPositiveButton("OK", listener).create();
                                alertDialog.setCancelable(CRASH_RIL);
                                alertDialog.setCanceledOnTouchOutside(CRASH_RIL);
                                alertDialog.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_RECEIVE);
                                alertDialog.show();
                            }
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
                if (isRecordLoadResponse) {
                    onRecordLoaded();
                }
            } catch (RuntimeException exc) {
                logw("Exception parsing SIM record", exc);
                if (0 != 0) {
                    onRecordLoaded();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                onRecordLoaded();
            }
            throw th;
        }
    }

    private class EfPlLoaded implements IccRecords.IccRecordLoaded {
        EfPlLoaded(SIMRecords this$0, EfPlLoaded efPlLoaded) {
            this();
        }

        private EfPlLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_PL";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            SIMRecords.this.mEfPl = (byte[]) ar.result;
            SIMRecords.this.log("EF_PL=" + IccUtils.bytesToHexString(SIMRecords.this.mEfPl));
        }
    }

    private class EfUsimLiLoaded implements IccRecords.IccRecordLoaded {
        EfUsimLiLoaded(SIMRecords this$0, EfUsimLiLoaded efUsimLiLoaded) {
            this();
        }

        private EfUsimLiLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_LI";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            SIMRecords.this.mEfLi = (byte[]) ar.result;
            SIMRecords.this.log("EF_LI=" + IccUtils.bytesToHexString(SIMRecords.this.mEfLi));
        }
    }

    private void handleFileUpdate(int efid) {
        switch (efid) {
            case IccConstants.EF_PBR:
            case 28474:
            case IccConstants.EF_SDN:
                break;
            case IccConstants.EF_CFF_CPHS:
                this.mRecordsToLoad++;
                log("SIM Refresh called for EF_CFF_CPHS");
                this.mFh.loadEFTransparent(IccConstants.EF_CFF_CPHS, obtainMessage(24));
                return;
            case IccConstants.EF_CSP_CPHS:
                this.mRecordsToLoad++;
                log("[CSP] SIM Refresh for EF_CSP_CPHS");
                this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
                return;
            case IccConstants.EF_MAILBOX_CPHS:
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                return;
            case IccConstants.EF_FDN:
                log("SIM Refresh called for EF_FDN");
                this.mParentApp.queryFdn();
                break;
            case IccConstants.EF_MSISDN:
                this.mRecordsToLoad++;
                log("SIM Refresh called for EF_MSISDN");
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
                return;
            case IccConstants.EF_MBDN:
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                return;
            case IccConstants.EF_CFIS:
                this.mRecordsToLoad++;
                log("SIM Refresh called for EF_CFIS");
                this.mFh.loadEFLinearFixed(IccConstants.EF_CFIS, 1, obtainMessage(32));
                return;
            default:
                log("handleFileUpdate default");
                if (this.mAdnCache.isUsimPhbEfAndNeedReset(efid) && !this.mIsPhbEfResetDone) {
                    this.mIsPhbEfResetDone = true;
                    this.mAdnCache.reset();
                    setPhbReady(CRASH_RIL);
                }
                fetchSimRecords();
                return;
        }
        if (this.mIsPhbEfResetDone) {
            return;
        }
        this.mIsPhbEfResetDone = true;
        this.mAdnCache.reset();
        log("handleFileUpdate ADN like");
        setPhbReady(CRASH_RIL);
    }

    private void handleSimRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleSimRefresh received without input");
            return;
        }
        if (refreshResponse.aid != null && !TextUtils.isEmpty(refreshResponse.aid) && !refreshResponse.aid.equals(this.mParentApp.getAid())) {
            log("handleSimRefresh, refreshResponse.aid = " + refreshResponse.aid + ", mParentApp.getAid() = " + this.mParentApp.getAid());
            return;
        }
        switch (refreshResponse.refreshResult) {
            case 0:
                log("handleSimRefresh with SIM_REFRESH_FILE_UPDATED");
                handleFileUpdate(refreshResponse.efId);
                this.mIsPhbEfResetDone = CRASH_RIL;
                break;
            case 1:
                log("handleSimRefresh with SIM_REFRESH_INIT");
                setPhbReady(CRASH_RIL);
                onIccRefreshInit();
                break;
            case 2:
                log("handleSimRefresh with SIM_REFRESH_RESET");
                TelephonyManager.MultiSimVariants mSimVar = TelephonyManager.getDefault().getMultiSimConfiguration();
                log("mSimVar : " + mSimVar);
                if (!SystemProperties.get("ro.sim_refresh_reset_by_modem").equals("1")) {
                    log("sim_refresh_reset_by_modem false");
                    this.mCi.resetRadio(null);
                } else {
                    log("Sim reset by modem!");
                }
                setPhbReady(CRASH_RIL);
                onIccRefreshInit();
                break;
            case 3:
            default:
                log("handleSimRefresh with unknown operation");
                break;
            case 4:
                log("handleSimRefresh with REFRESH_INIT_FULL_FILE_UPDATED");
                setPhbReady(CRASH_RIL);
                onIccRefreshInit();
                break;
            case 5:
                log("handleSimRefresh with REFRESH_INIT_FILE_UPDATED, EFID = " + refreshResponse.efId);
                handleFileUpdate(refreshResponse.efId);
                this.mIsPhbEfResetDone = CRASH_RIL;
                if (this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                    sendMessage(obtainMessage(1));
                }
                break;
            case 6:
                log("handleSimRefresh with REFRESH_SESSION_RESET");
                onIccRefreshInit();
                break;
        }
        if (refreshResponse.refreshResult != 1 && refreshResponse.refreshResult != 2 && refreshResponse.refreshResult != 4 && refreshResponse.refreshResult != 5 && refreshResponse.refreshResult != 3) {
            return;
        }
        log("notify stk app to remove the idle text");
        Intent intent = new Intent("android.intent.aciton.stk.REMOVE_IDLE_TEXT");
        intent.putExtra(KEY_SIM_ID, this.mSlotId);
        this.mContext.sendBroadcast(intent);
    }

    private int dispatchGsmMessage(SmsMessage message) {
        this.mNewSmsRegistrants.notifyResult(message);
        return 0;
    }

    private void handleSms(byte[] ba) {
        if (ba[0] != 0) {
            Rlog.d("ENF", "status : " + ((int) ba[0]));
        }
        if (ba[0] != 3) {
            return;
        }
        int n = ba.length;
        byte[] pdu = new byte[n - 1];
        System.arraycopy(ba, 1, pdu, 0, n - 1);
        SmsMessage message = SmsMessage.createFromPdu(pdu, SmsMessage.FORMAT_3GPP);
        dispatchGsmMessage(message);
    }

    private void handleSmses(ArrayList<byte[]> messages) {
        int count = messages.size();
        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);
            if (ba[0] != 0) {
                Rlog.i("ENF", "status " + i + ": " + ((int) ba[0]));
            }
            if (ba[0] == 3) {
                int n = ba.length;
                byte[] pdu = new byte[n - 1];
                System.arraycopy(ba, 1, pdu, 0, n - 1);
                SmsMessage message = SmsMessage.createFromPdu(pdu, SmsMessage.FORMAT_3GPP);
                dispatchGsmMessage(message);
                ba[0] = 1;
            }
        }
    }

    private String findBestLanguage(byte[] languages) {
        String[] locales = this.mContext.getAssets().getLocales();
        if (languages == null || locales == null) {
            return null;
        }
        for (int i = 0; i + 1 < languages.length; i += 2) {
            try {
                String lang = new String(languages, i, 2, "ISO-8859-1");
                log("languages from sim = " + lang);
                for (int j = 0; j < locales.length; j++) {
                    if (locales[j] != null && locales[j].length() >= 2 && locales[j].substring(0, 2).equalsIgnoreCase(lang)) {
                        return lang;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                log("Failed to parse USIM language records" + e);
            }
        }
        return null;
    }

    @Override
    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        log("onRecordLoaded " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
        if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
            onAllRecordsLoaded();
        } else {
            if (this.mRecordsToLoad >= 0) {
                return;
            }
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    private void setVoiceCallForwardingFlagFromSimRecords() {
        if (validEfCfis(this.mEfCfis)) {
            this.mCallForwardingStatus = this.mEfCfis[1] & 1;
            log("EF_CFIS: callForwardingEnabled=" + this.mCallForwardingStatus);
        } else if (this.mEfCff != null) {
            this.mCallForwardingStatus = (this.mEfCff[0] & 15) != 10 ? 0 : 1;
            log("EF_CFF: callForwardingEnabled=" + this.mCallForwardingStatus);
        } else {
            this.mCallForwardingStatus = -1;
            log("EF_CFIS and EF_CFF not valid. callForwardingEnabled=" + this.mCallForwardingStatus);
        }
    }

    @Override
    protected void onAllRecordsLoaded() {
        String strCountryCodeForMcc;
        log("record load complete");
        Resources resource = Resources.getSystem();
        if (resource.getBoolean(R.^attr-private.mtpReserve)) {
            setSimLanguage(this.mEfLi, this.mEfPl);
        } else {
            log("Not using EF LI/EF PL");
        }
        setVoiceCallForwardingFlagFromSimRecords();
        if (this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_PIN || this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_PUK || this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
            this.mRecordsRequested = CRASH_RIL;
            return;
        }
        String operator = getOperatorNumeric();
        if (!TextUtils.isEmpty(operator)) {
            log("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='" + operator + "'");
            log("update icc_operator_numeric=" + operator);
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), operator);
            SubscriptionController subController = SubscriptionController.getInstance();
            subController.setMccMnc(operator, subController.getDefaultSubId());
        } else {
            log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
        }
        if (!TextUtils.isEmpty(this.mImsi)) {
            log("onAllRecordsLoaded set mcc imsi" + UsimPBMemInfo.STRING_NOT_SET);
            try {
                strCountryCodeForMcc = MccTable.countryCodeForMcc(Integer.parseInt(this.mImsi.substring(0, 3)));
            } catch (NumberFormatException e) {
                strCountryCodeForMcc = null;
                loge("SIMRecords: Corrupt IMSI!");
            }
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), strCountryCodeForMcc);
        } else {
            log("onAllRecordsLoaded empty imsi skipping setting mcc");
        }
        setVoiceMailByCountry(operator);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        log("imsi = " + this.mImsi + " operator = " + operator);
        if (operator == null) {
            return;
        }
        if (operator.equals("46002") || operator.equals("46007")) {
            operator = "46000";
        }
        String newName = SpnOverride.getInstance().lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mParentApp.getPhoneId()), operator, true, this.mContext);
        setSystemProperty("gsm.sim.operator.default-name", newName);
    }

    private void handleCarrierNameOverride() {
        CarrierConfigManager configLoader = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configLoader != null && configLoader.getConfig().getBoolean("carrier_name_override_bool")) {
            String carrierName = configLoader.getConfig().getString("carrier_name_string");
            setServiceProviderName(carrierName);
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), carrierName);
            return;
        }
        setSpnFromConfig(getOperatorNumeric());
    }

    private void setSpnFromConfig(String carrier) {
        if (!TextUtils.isEmpty(getServiceProviderName()) || !this.mSpnOverride.containsCarrier(carrier)) {
            return;
        }
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), this.mSpnOverride.getSpn(carrier));
    }

    private void setVoiceMailByCountry(String spn) {
        if (!this.mVmConfig.containsCarrier(spn)) {
            return;
        }
        log("setVoiceMailByCountry");
        this.mIsVoiceMailFixed = true;
        this.mVoiceMailNum = this.mVmConfig.getVoiceMailNumber(spn);
        this.mVoiceMailTag = this.mVmConfig.getVoiceMailTag(spn);
    }

    @Override
    public void onReady() {
        fetchSimRecords();
    }

    private void onLocked() {
        log("only fetch EF_LI and EF_PL in lock state");
        loadEfLiAndEfPl();
    }

    private void loadEfLiAndEfPl() {
        EfUsimLiLoaded efUsimLiLoaded = null;
        Object[] objArr = 0;
        if (this.mParentApp.getType() != IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            return;
        }
        this.mRecordsRequested = true;
        this.mFh.loadEFTransparent(IccConstants.EF_LI, obtainMessage(100, new EfUsimLiLoaded(this, efUsimLiLoaded)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(12037, obtainMessage(100, new EfPlLoaded(this, objArr == true ? 1 : 0)));
        this.mRecordsToLoad++;
    }

    private void loadCallForwardingRecords() {
        this.mRecordsRequested = true;
        this.mFh.loadEFLinearFixed(IccConstants.EF_CFIS, 1, obtainMessage(32));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CFF_CPHS, obtainMessage(24));
        this.mRecordsToLoad++;
    }

    protected void fetchSimRecords() {
        this.mRecordsRequested = true;
        log("fetchSimRecords " + this.mRecordsToLoad);
        this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(3));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(4));
        this.mRecordsToLoad++;
        new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_AD, obtainMessage(9));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, obtainMessage(8));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_SPDI, obtainMessage(13));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_SST, obtainMessage(17));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_INFO_CPHS, obtainMessage(26));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_GID1, obtainMessage(34));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_GID2, obtainMessage(36));
        this.mRecordsToLoad++;
        loadEfLiAndEfPl();
        if (this.mTelephonyExt != null) {
            if (this.mTelephonyExt.isSetLanguageBySIM()) {
                this.mFh.loadEFTransparent(IccConstants.EF_SUME, obtainMessage(53));
                this.mRecordsToLoad++;
            }
        } else {
            loge("fetchSimRecords(): mTelephonyExt is null!!!");
        }
        fetchCPHSOns();
        log("fetchSimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
        fetchRatBalancing();
    }

    @Override
    public int getDisplayRule(String plmn) {
        boolean bSpnActive = CRASH_RIL;
        String spn = getServiceProviderName();
        if (this.mEfSST != null && this.mParentApp != null) {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                if (this.mEfSST.length >= 3 && (this.mEfSST[2] & 4) == 4) {
                    bSpnActive = true;
                    log("getDisplayRule USIM mEfSST is " + IccUtils.bytesToHexString(this.mEfSST) + " set bSpnActive to true");
                }
            } else if (this.mEfSST.length >= 5 && (this.mEfSST[4] & 2) == 2) {
                bSpnActive = true;
                log("getDisplayRule SIM mEfSST is " + IccUtils.bytesToHexString(this.mEfSST) + " set bSpnActive to true");
            }
        }
        log("getDisplayRule mParentApp is " + (this.mParentApp != null ? this.mParentApp : "null"));
        if (this.mParentApp != null && this.mParentApp.getUiccCard() != null && this.mParentApp.getUiccCard().getOperatorBrandOverride() != null) {
            log("getDisplayRule, getOperatorBrandOverride is not null");
            return 2;
        }
        if (!bSpnActive || TextUtils.isEmpty(spn) || spn.equals(UsimPBMemInfo.STRING_NOT_SET) || this.mSpnDisplayCondition == -1) {
            log("getDisplayRule, no EF_SPN");
            return 2;
        }
        if (isOnMatchingPlmn(plmn)) {
            if ((this.mSpnDisplayCondition & 1) != 1) {
                return 1;
            }
            return 3;
        }
        if ((this.mSpnDisplayCondition & 2) != 0) {
            return 2;
        }
        return 3;
    }

    private boolean isOnMatchingPlmn(String plmn) {
        if (plmn == null) {
            return CRASH_RIL;
        }
        if (isHPlmn(plmn)) {
            return true;
        }
        if (this.mSpdiNetworks != null) {
            for (String spdiNet : this.mSpdiNetworks) {
                if (plmn.equals(spdiNet)) {
                    return true;
                }
            }
        }
        return CRASH_RIL;
    }

    private enum GetSpnFsmState {
        IDLE,
        INIT,
        READ_SPN_3GPP,
        READ_SPN_CPHS,
        READ_SPN_SHORT_CPHS;

        public static GetSpnFsmState[] valuesCustom() {
            return values();
        }
    }

    private void getSpnFsm(boolean start, AsyncResult ar) {
        if (start) {
            if (this.mSpnState == GetSpnFsmState.READ_SPN_3GPP || this.mSpnState == GetSpnFsmState.READ_SPN_CPHS || this.mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS || this.mSpnState == GetSpnFsmState.INIT) {
                this.mSpnState = GetSpnFsmState.INIT;
            }
            this.mSpnState = GetSpnFsmState.INIT;
        }
        switch (m510xf89fffa2()[this.mSpnState.ordinal()]) {
            case 1:
                this.mFh.loadEFTransparent(IccConstants.EF_SPN, obtainMessage(12));
                this.mRecordsToLoad++;
                this.mSpnState = GetSpnFsmState.READ_SPN_3GPP;
                break;
            case 2:
                if (ar != null && ar.exception == null) {
                    byte[] data = (byte[]) ar.result;
                    this.mSpnDisplayCondition = data[0] & PplMessageManager.Type.INVALID;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data, 1, data.length - 1));
                    String spn = getServiceProviderName();
                    if (spn == null || spn.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                    } else {
                        log("Load EF_SPN: " + spn + " spnDisplayCondition: " + this.mSpnDisplayCondition);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn);
                        this.mSpnState = GetSpnFsmState.IDLE;
                    }
                } else {
                    this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                }
                if (this.mSpnState == GetSpnFsmState.READ_SPN_CPHS) {
                    this.mFh.loadEFTransparent(IccConstants.EF_SPN_CPHS, obtainMessage(12));
                    this.mRecordsToLoad++;
                    this.mSpnDisplayCondition = -1;
                }
                break;
            case 3:
                if (ar != null && ar.exception == null) {
                    byte[] data2 = (byte[]) ar.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data2, 0, data2.length));
                    String spn2 = getServiceProviderName();
                    if (spn2 == null || spn2.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                    } else {
                        this.mSpnDisplayCondition = 2;
                        log("Load EF_SPN_CPHS: " + spn2);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn2);
                        this.mSpnState = GetSpnFsmState.IDLE;
                    }
                } else {
                    this.mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                }
                if (this.mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS) {
                    this.mFh.loadEFTransparent(IccConstants.EF_SPN_SHORT_CPHS, obtainMessage(12));
                    this.mRecordsToLoad++;
                }
                break;
            case 4:
                if (ar != null && ar.exception == null) {
                    byte[] data3 = (byte[]) ar.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(data3, 0, data3.length));
                    String spn3 = getServiceProviderName();
                    if (spn3 == null || spn3.length() == 0) {
                        log("No SPN loaded in either CHPS or 3GPP");
                    } else {
                        this.mSpnDisplayCondition = 2;
                        log("Load EF_SPN_SHORT_CPHS: " + spn3);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), spn3);
                    }
                } else {
                    setServiceProviderName(null);
                    log("No SPN loaded in either CHPS or 3GPP");
                }
                this.mSpnState = GetSpnFsmState.IDLE;
                break;
            default:
                this.mSpnState = GetSpnFsmState.IDLE;
                break;
        }
    }

    private void parseEfSpdi(byte[] data) {
        SimTlv tlv = new SimTlv(data, 0, data.length);
        byte[] plmnEntries = null;
        while (true) {
            if (!tlv.isValidObject()) {
                break;
            }
            if (tlv.getTag() == 163) {
                tlv = new SimTlv(tlv.getData(), 0, tlv.getData().length);
            }
            if (tlv.getTag() != 128) {
                tlv.nextObject();
            } else {
                plmnEntries = tlv.getData();
                break;
            }
        }
        if (plmnEntries == null) {
            return;
        }
        this.mSpdiNetworks = new ArrayList<>(plmnEntries.length / 3);
        for (int i = 0; i + 2 < plmnEntries.length; i += 3) {
            String plmnCode = IccUtils.parsePlmnToString(plmnEntries, i, 3);
            if (plmnCode.length() >= 5) {
                log("EF_SPDI network: " + plmnCode);
                this.mSpdiNetworks.add(plmnCode);
            }
        }
    }

    private boolean isCphsMailboxEnabled() {
        if (this.mCphsInfo != null && (this.mCphsInfo[1] & 48) == 48) {
            return true;
        }
        return CRASH_RIL;
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[SIMRecords] " + s + " (slot " + this.mSlotId + ")");
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[SIMRecords] " + s + " (slot " + this.mSlotId + ")");
    }

    protected void logw(String s, Throwable tr) {
        Rlog.w(LOG_TAG, "[SIMRecords] " + s + " (slot " + this.mSlotId + ")", tr);
    }

    protected void logv(String s) {
        Rlog.v(LOG_TAG, "[SIMRecords] " + s + " (slot " + this.mSlotId + ")");
    }

    @Override
    public boolean isCspPlmnEnabled() {
        return this.mCspPlmnEnabled;
    }

    private void handleEfCspData(byte[] data) {
        int usedCspGroups = data.length / 2;
        this.mCspPlmnEnabled = true;
        for (int i = 0; i < usedCspGroups; i++) {
            if (data[i * 2] == -64) {
                log("[CSP] found ValueAddedServicesGroup, value " + ((int) data[(i * 2) + 1]));
                if ((data[(i * 2) + 1] & BipUtils.TCP_STATUS_ESTABLISHED) == 128) {
                    this.mCspPlmnEnabled = true;
                    return;
                }
                this.mCspPlmnEnabled = CRASH_RIL;
                log("[CSP] Set Automatic Network Selection");
                this.mNetworkSelectionModeAutomaticRegistrants.notifyRegistrants();
                return;
            }
        }
        log("[CSP] Value Added Service Group (0xC0), not found!");
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SIMRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.println(" mVmConfig=" + this.mVmConfig);
        pw.println(" mSpnOverride=" + this.mSpnOverride);
        pw.println(" mCallForwardingStatus=" + this.mCallForwardingStatus);
        pw.println(" mSpnState=" + this.mSpnState);
        pw.println(" mCphsInfo=" + this.mCphsInfo);
        pw.println(" mCspPlmnEnabled=" + this.mCspPlmnEnabled);
        pw.println(" mEfMWIS[]=" + Arrays.toString(this.mEfMWIS));
        pw.println(" mEfCPHS_MWI[]=" + Arrays.toString(this.mEfCPHS_MWI));
        pw.println(" mEfCff[]=" + Arrays.toString(this.mEfCff));
        pw.println(" mEfCfis[]=" + Arrays.toString(this.mEfCfis));
        pw.println(" mSpnDisplayCondition=" + this.mSpnDisplayCondition);
        pw.println(" mSpdiNetworks[]=" + this.mSpdiNetworks);
        pw.println(" mPnnHomeName=" + this.mPnnHomeName);
        pw.println(" mUsimServiceTable=" + this.mUsimServiceTable);
        pw.println(" mGid1=" + this.mGid1);
        pw.println(" mGid2=" + this.mGid2);
        pw.flush();
    }

    @Override
    public String getSpNameInEfSpn() {
        log("getSpNameInEfSpn(): " + this.mSpNameInEfSpn);
        return this.mSpNameInEfSpn;
    }

    @Override
    public String isOperatorMvnoForImsi() {
        SpnOverride spnOverride = SpnOverride.getInstance();
        String imsiPattern = spnOverride.isOperatorMvnoForImsi(getOperatorNumeric(), getIMSI());
        String mccmnc = getOperatorNumeric();
        log("isOperatorMvnoForImsi(), imsiPattern: " + imsiPattern + ", mccmnc: " + mccmnc);
        if (imsiPattern == null || mccmnc == null) {
            return null;
        }
        String result = imsiPattern.substring(mccmnc.length(), imsiPattern.length());
        log("isOperatorMvnoForImsi(): " + result);
        return result;
    }

    @Override
    public String getFirstFullNameInEfPnn() {
        if (this.mPnnNetworkNames == null || this.mPnnNetworkNames.size() == 0) {
            log("getFirstFullNameInEfPnn(): empty");
            return null;
        }
        OperatorName opName = this.mPnnNetworkNames.get(0);
        log("getFirstFullNameInEfPnn(): first fullname: " + opName.sFullName);
        if (opName.sFullName != null) {
            return new String(opName.sFullName);
        }
        return null;
    }

    @Override
    public String isOperatorMvnoForEfPnn() {
        String MCCMNC = getOperatorNumeric();
        String PNN = getFirstFullNameInEfPnn();
        log("isOperatorMvnoForEfPnn(): mccmnc = " + MCCMNC + ", pnn = " + PNN);
        if (SpnOverride.getInstance().getSpnByEfPnn(MCCMNC, PNN) != null) {
            return PNN;
        }
        return null;
    }

    @Override
    public String getMvnoMatchType() {
        String IMSI = getIMSI();
        String SPN = getSpNameInEfSpn();
        String PNN = getFirstFullNameInEfPnn();
        String GID1 = getGid1();
        String MCCMNC = getOperatorNumeric();
        log("getMvnoMatchType(): imsi = " + IMSI + ", mccmnc = " + MCCMNC + ", spn = " + SPN);
        if (SpnOverride.getInstance().getSpnByEfSpn(MCCMNC, SPN) != null) {
            return Telephony.Carriers.SPN;
        }
        if (SpnOverride.getInstance().getSpnByImsi(MCCMNC, IMSI) != null) {
            return Telephony.Carriers.IMSI;
        }
        if (SpnOverride.getInstance().getSpnByEfPnn(MCCMNC, PNN) != null) {
            return Telephony.Carriers.PNN;
        }
        if (SpnOverride.getInstance().getSpnByEfGid1(MCCMNC, GID1) != null) {
            return "gid";
        }
        return UsimPBMemInfo.STRING_NOT_SET;
    }

    private class SIMBroadCastReceiver extends BroadcastReceiver {
        SIMBroadCastReceiver(SIMRecords this$0, SIMBroadCastReceiver sIMBroadCastReceiver) {
            this();
        }

        private SIMBroadCastReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.mediatek.dm.LAWMO_WIPE")) {
                SIMRecords.this.wipeAllSIMContacts();
                return;
            }
            if (action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO)) {
                SIMRecords.this.processShutdownIPO();
                SystemProperties.set(SIMRecords.this.SIM_RECORDS_PROPERTY_ECC_LIST[SIMRecords.this.mSlotId], (String) null);
                SIMRecords.this.log("wipeAllSIMContacts ACTION_SHUTDOWN_IPO: reset mCspPlmnEnabled");
                SIMRecords.this.mCspPlmnEnabled = true;
                if (SIMRecords.this.mTelephonyExt.isSetLanguageBySIM()) {
                    SIMRecords.this.mEfRatLoaded = SIMRecords.CRASH_RIL;
                    SIMRecords.this.mEfRat = null;
                }
                SIMRecords.this.mAdnCache.reset();
                SIMRecords.this.log("wipeAllSIMContacts ACTION_SHUTDOWN_IPO");
                return;
            }
            if (!action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                return;
            }
            String reasonExtra = intent.getStringExtra("reason");
            int slot = intent.getIntExtra("slot", 0);
            String simState = intent.getStringExtra("ss");
            SIMRecords.this.log("SIM_STATE_CHANGED: slot = " + slot + ",reason = " + reasonExtra + ",simState = " + simState);
            if ("PUK".equals(reasonExtra) && slot == SIMRecords.this.mSlotId) {
                String strPuk1Count = SystemProperties.get(SIMRecords.SIMRECORD_PROPERTY_RIL_PUK1[SIMRecords.this.mSlotId], "0");
                SIMRecords.this.log("SIM_STATE_CHANGED: strPuk1Count = " + strPuk1Count);
                SIMRecords.this.mMsisdn = UsimPBMemInfo.STRING_NOT_SET;
                SIMRecords.this.mRecordsEventsRegistrants.notifyResult(100);
            }
            if (slot != SIMRecords.this.mSlotId) {
                return;
            }
            String strPhbReady = SystemProperties.get(SIMRecords.SIMRECORD_PROPERTY_RIL_PHB_READY[SIMRecords.this.mSlotId], "false");
            SIMRecords.this.log("sim state: " + simState + ", mPhbReady: " + SIMRecords.this.mPhbReady + ",strPhbReady: " + strPhbReady.equals("true"));
            if (!"READY".equals(simState)) {
                return;
            }
            if (!SIMRecords.this.mPhbReady && strPhbReady.equals("true")) {
                SIMRecords.this.mPhbReady = true;
                SIMRecords.this.broadcastPhbStateChangedIntent(SIMRecords.this.mPhbReady);
            } else {
                if (!SIMRecords.this.mPhbWaitSub || !strPhbReady.equals("true")) {
                    return;
                }
                SIMRecords.this.log("mPhbWaitSub is " + SIMRecords.this.mPhbWaitSub + ", broadcast if need");
                SIMRecords.this.mPhbWaitSub = SIMRecords.CRASH_RIL;
                SIMRecords.this.broadcastPhbStateChangedIntent(SIMRecords.this.mPhbReady);
            }
        }
    }

    private class SubBroadCastReceiver extends BroadcastReceiver {
        SubBroadCastReceiver(SIMRecords this$0, SubBroadCastReceiver subBroadCastReceiver) {
            this();
        }

        private SubBroadCastReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (!SIMRecords.this.mPhbWaitSub || !action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                return;
            }
            SIMRecords.this.log("SubBroadCastReceiver receive ACTION_SUBINFO_RECORD_UPDATED");
            SIMRecords.this.mPhbWaitSub = SIMRecords.CRASH_RIL;
            SIMRecords.this.broadcastPhbStateChangedIntent(SIMRecords.this.mPhbReady);
        }
    }

    private void wipeAllSIMContacts() {
        log("wipeAllSIMContacts");
        this.mAdnCache.reset();
        log("wipeAllSIMContacts after reset");
    }

    private void processShutdownIPO() {
        this.hasQueryIccId = CRASH_RIL;
        this.iccIdQueryState = -1;
        this.mIccId = null;
        this.mImsi = null;
        this.mSpNameInEfSpn = null;
    }

    private void fetchEccList() {
        int eccFromModemUrc = SystemProperties.getInt("ril.ef.ecc.support", 0);
        log("fetchEccList(), eccFromModemUrc:" + eccFromModemUrc);
        if (eccFromModemUrc != 0) {
            return;
        }
        this.mEfEcc = UsimPBMemInfo.STRING_NOT_SET;
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            this.mFh.loadEFLinearFixedAll(IccConstants.EF_ECC, obtainMessage(EVENT_GET_USIM_ECC_DONE));
        } else {
            this.mFh.loadEFTransparent(IccConstants.EF_ECC, obtainMessage(102));
        }
    }

    private void updateConfiguration(String numeric) {
        if (!TextUtils.isEmpty(numeric) && !this.mOldMccMnc.equals(numeric)) {
            this.mOldMccMnc = numeric;
            MccTable.updateMccMncConfiguration(this.mContext, this.mOldMccMnc, CRASH_RIL);
        } else {
            log("Do not update configuration if mcc mnc no change.");
        }
    }

    private void parseEFpnn(ArrayList messages) {
        int count = messages.size();
        log("parseEFpnn(): pnn has " + count + " records");
        this.mPnnNetworkNames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] data = (byte[]) messages.get(i);
            log("parseEFpnn(): pnn record " + i + " content is " + IccUtils.bytesToHexString(data));
            SimTlv tlv = new SimTlv(data, 0, data.length);
            OperatorName opName = new OperatorName();
            while (tlv.isValidObject()) {
                if (tlv.getTag() == TAG_FULL_NETWORK_NAME) {
                    opName.sFullName = IccUtils.networkNameToString(tlv.getData(), 0, tlv.getData().length);
                    log("parseEFpnn(): pnn sFullName is " + opName.sFullName);
                } else if (tlv.getTag() == 69) {
                    opName.sShortName = IccUtils.networkNameToString(tlv.getData(), 0, tlv.getData().length);
                    log("parseEFpnn(): pnn sShortName is " + opName.sShortName);
                }
                tlv.nextObject();
            }
            this.mPnnNetworkNames.add(opName);
        }
    }

    private void fetchPnnAndOpl() {
        log("fetchPnnAndOpl()");
        boolean bPnnActive = CRASH_RIL;
        boolean bOplActive = CRASH_RIL;
        if (this.mEfSST != null) {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                if (this.mEfSST.length >= 6) {
                    bPnnActive = (this.mEfSST[5] & PplControlData.STATUS_WIPE_REQUESTED) == 16;
                    if (bPnnActive) {
                        bOplActive = (this.mEfSST[5] & 32) == 32 ? true : CRASH_RIL;
                    }
                }
            } else if (this.mEfSST.length >= 13) {
                bPnnActive = (this.mEfSST[12] & 48) == 48;
                if (bPnnActive) {
                    bOplActive = (this.mEfSST[12] & 192) == 192 ? true : CRASH_RIL;
                }
            }
        }
        log("bPnnActive = " + bPnnActive + ", bOplActive = " + bOplActive);
        if (!bPnnActive) {
            return;
        }
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PNN, obtainMessage(15));
        this.mRecordsToLoad++;
        if (!bOplActive) {
            return;
        }
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_OPL, obtainMessage(104));
        this.mRecordsToLoad++;
    }

    private void fetchSpn() {
        log("fetchSpn()");
        IccRecords.IccServiceStatus iccSerStatus = getSIMServiceStatus(IccRecords.IccService.SPN);
        if (iccSerStatus == IccRecords.IccServiceStatus.ACTIVATED) {
            setServiceProviderName(null);
            this.mFh.loadEFTransparent(IccConstants.EF_SPN, obtainMessage(12));
            this.mRecordsToLoad++;
            return;
        }
        log("[SIMRecords] SPN service is not activated  ");
    }

    @Override
    public IccRecords.IccServiceStatus getSIMServiceStatus(IccRecords.IccService enService) {
        int nbit;
        int nbit2;
        int nServiceNum = enService.getIndex();
        IccRecords.IccServiceStatus simServiceStatus = IccRecords.IccServiceStatus.UNKNOWN;
        log("getSIMServiceStatus enService is " + enService + " Service Index is " + nServiceNum);
        if (nServiceNum >= 0 && nServiceNum < IccRecords.IccService.UNSUPPORTED_SERVICE.getIndex() && this.mEfSST != null) {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                int nUSTIndex = usimServiceNumber[nServiceNum];
                if (nUSTIndex <= 0) {
                    simServiceStatus = IccRecords.IccServiceStatus.NOT_EXIST_IN_USIM;
                } else {
                    int nbyte = nUSTIndex / 8;
                    int nbit3 = nUSTIndex % 8;
                    if (nbit3 == 0) {
                        nbit2 = 7;
                        nbyte--;
                    } else {
                        nbit2 = nbit3 - 1;
                    }
                    log("getSIMServiceStatus USIM nbyte: " + nbyte + " nbit: " + nbit2);
                    simServiceStatus = (this.mEfSST.length <= nbyte || (this.mEfSST[nbyte] & (1 << nbit2)) <= 0) ? IccRecords.IccServiceStatus.INACTIVATED : IccRecords.IccServiceStatus.ACTIVATED;
                }
            } else {
                int nSSTIndex = simServiceNumber[nServiceNum];
                if (nSSTIndex <= 0) {
                    simServiceStatus = IccRecords.IccServiceStatus.NOT_EXIST_IN_SIM;
                } else {
                    int nbyte2 = nSSTIndex / 4;
                    int nbit4 = nSSTIndex % 4;
                    if (nbit4 == 0) {
                        nbit = 3;
                        nbyte2--;
                    } else {
                        nbit = nbit4 - 1;
                    }
                    int nMask = 2 << (nbit * 2);
                    log("getSIMServiceStatus SIM nbyte: " + nbyte2 + " nbit: " + nbit + " nMask: " + nMask);
                    simServiceStatus = (this.mEfSST.length <= nbyte2 || (this.mEfSST[nbyte2] & nMask) != nMask) ? IccRecords.IccServiceStatus.INACTIVATED : IccRecords.IccServiceStatus.ACTIVATED;
                }
            }
        }
        log("getSIMServiceStatus simServiceStatus: " + simServiceStatus);
        return simServiceStatus;
    }

    private void fetchSmsp() {
        log("fetchSmsp()");
        if (this.mUsimServiceTable == null || this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM || !this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.SM_SERVICE_PARAMS)) {
            return;
        }
        log("SMSP support.");
        this.mFh.loadEFLinearFixed(IccConstants.EF_SMSP, 1, obtainMessage(208));
        this.mRecordsToLoad++;
        if (!this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.SM_OVER_IP)) {
            return;
        }
        log("PSISMSP support.");
        this.mFh.loadEFLinearFixed(28645, 1, obtainMessage(EVENT_GET_PSISMSC_DONE));
        this.mRecordsToLoad++;
    }

    private void fetchGbaRecords() {
        log("fetchGbaRecords");
        if (this.mUsimServiceTable == null || this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM || !this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.GBA)) {
            return;
        }
        log("GBA support.");
        this.mFh.loadEFTransparent(IccConstants.EF_ISIM_GBABP, obtainMessage(EVENT_GET_GBABP_DONE));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_ISIM_GBANL, obtainMessage(EVENT_GET_GBANL_DONE));
        this.mRecordsToLoad++;
    }

    private void fetchMbiRecords() {
        log("fetchMbiRecords");
        if (this.mUsimServiceTable == null || this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM || !this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.MBDN)) {
            return;
        }
        log("MBI/MBDN support.");
        this.mFh.loadEFLinearFixed(IccConstants.EF_MBI, 1, obtainMessage(5));
        this.mRecordsToLoad++;
    }

    private void fetchMwisRecords() {
        log("fetchMwisRecords");
        if (this.mUsimServiceTable == null || this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM || !this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.MWI_STATUS)) {
            return;
        }
        log("MWIS support.");
        this.mFh.loadEFLinearFixed(IccConstants.EF_MWIS, 1, obtainMessage(7));
        this.mRecordsToLoad++;
    }

    private void parseEFopl(ArrayList messages) {
        int count = messages.size();
        log("parseEFopl(): opl has " + count + " records");
        this.mOperatorList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] data = (byte[]) messages.get(i);
            OplRecord oplRec = new OplRecord();
            oplRec.sPlmn = IccUtils.parsePlmnToStringForEfOpl(data, 0, 3);
            byte[] minLac = {data[3], data[4]};
            oplRec.nMinLAC = Integer.parseInt(IccUtils.bytesToHexString(minLac), 16);
            byte[] maxLAC = {data[5], data[6]};
            oplRec.nMaxLAC = Integer.parseInt(IccUtils.bytesToHexString(maxLAC), 16);
            byte[] pnnRecordIndex = {data[7]};
            oplRec.nPnnIndex = Integer.parseInt(IccUtils.bytesToHexString(pnnRecordIndex), 16);
            log("parseEFopl(): record=" + i + " content=" + IccUtils.bytesToHexString(data) + " sPlmn=" + oplRec.sPlmn + " nMinLAC=" + oplRec.nMinLAC + " nMaxLAC=" + oplRec.nMaxLAC + " nPnnIndex=" + oplRec.nPnnIndex);
            this.mOperatorList.add(oplRec);
        }
    }

    private void boradcastEfRatContentNotify(int item) {
        Intent intent = new Intent("android.intent.action.ACTION_EF_RAT_CONTENT_NOTIFY");
        intent.putExtra("ef_rat_status", item);
        intent.putExtra("slot", this.mSlotId);
        log("broadCast intent ACTION_EF_RAT_CONTENT_NOTIFY: item: " + item + ", simId: " + this.mSlotId);
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
    }

    private void processEfCspPlmnModeBitUrc(int bit) {
        log("processEfCspPlmnModeBitUrc: bit = " + bit);
        if (bit == 0) {
            this.mCspPlmnEnabled = CRASH_RIL;
        } else {
            this.mCspPlmnEnabled = true;
        }
        Intent intent = new Intent("android.intent.action.ACTION_EF_CSP_CONTENT_NOTIFY");
        intent.putExtra("plmn_mode_bit", bit);
        intent.putExtra("slot", this.mSlotId);
        log("broadCast intent ACTION_EF_CSP_CONTENT_NOTIFY, EXTRA_PLMN_MODE_BIT: " + bit);
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
    }

    private void fetchLanguageIndicator() {
        log("fetchLanguageIndicator ");
        String l = SystemProperties.get("persist.sys.language");
        String c = SystemProperties.get("persist.sys.country");
        String oldSimLang = SystemProperties.get("persist.sys.simlanguage");
        if (l == null || l.length() == 0) {
            if (c != null && c.length() != 0) {
                return;
            }
            if (oldSimLang != null && oldSimLang.length() != 0) {
                return;
            }
            if (this.mEfLi == null) {
                this.mFh.loadEFTransparent(IccConstants.EF_LI, obtainMessage(42));
                this.efLanguageToLoad++;
            }
            this.mFh.loadEFTransparent(12037, obtainMessage(43));
            this.efLanguageToLoad++;
        }
    }

    private void onLanguageFileLoaded() {
        this.efLanguageToLoad--;
        log("onLanguageFileLoaded efLanguageToLoad is " + this.efLanguageToLoad);
        if (this.efLanguageToLoad != 0) {
            return;
        }
        log("onLanguageFileLoaded all language file loaded");
        if (this.mEfLi != null || this.mEfELP != null) {
            setLanguageFromSIM();
        } else {
            log("onLanguageFileLoaded all language file are not exist!");
        }
    }

    private void setLanguageFromSIM() {
        boolean bMatched;
        log("setLanguageFromSIM ");
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            bMatched = getMatchedLocaleByLI(this.mEfLi);
        } else {
            bMatched = getMatchedLocaleByLP(this.mEfLi);
        }
        if (!bMatched && this.mEfELP != null) {
            getMatchedLocaleByLI(this.mEfELP);
        }
        log("setLanguageFromSIM End");
    }

    private boolean getMatchedLocaleByLI(byte[] data) {
        boolean ret = CRASH_RIL;
        if (data == null) {
            return CRASH_RIL;
        }
        int lenOfLI = data.length;
        for (int i = 0; i + 2 <= lenOfLI; i += 2) {
            String lang = IccUtils.parseLanguageIndicator(data, i, 2);
            log("USIM language in language indicator: i is " + i + " language is " + lang);
            if (lang == null || lang.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                log("USIM language in language indicator: i is " + i + " language is empty");
                break;
            }
            ret = matchLangToLocale(lang.toLowerCase());
            if (ret) {
                break;
            }
        }
        return ret;
    }

    private boolean getMatchedLocaleByLP(byte[] data) {
        boolean ret = CRASH_RIL;
        if (data == null) {
            return CRASH_RIL;
        }
        int lenOfLP = data.length;
        String lang = null;
        for (int i = 0; i < lenOfLP; i++) {
            int index = this.mEfLi[0] & PplMessageManager.Type.INVALID;
            if (index >= 0 && index <= 15) {
                lang = LANGUAGE_CODE_FOR_LP[index];
            } else if (32 <= index && index <= 47) {
                lang = LANGUAGE_CODE_FOR_LP[index - 16];
            }
            log("SIM language in language preference: i is " + i + " language is " + lang);
            if (lang == null || lang.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                log("SIM language in language preference: i is " + i + " language is empty");
                break;
            }
            ret = matchLangToLocale(lang);
            if (ret) {
                break;
            }
        }
        return ret;
    }

    private boolean matchLangToLocale(String lang) {
        String[] locals = this.mContext.getAssets().getLocales();
        int localsSize = locals.length;
        for (int i = 0; i < localsSize; i++) {
            String s = locals[i];
            int len = s.length();
            if (len == 5) {
                String language = s.substring(0, 2);
                log("Supported languages: the i" + i + " th is " + language);
                if (lang.equals(language)) {
                    log("Matched! lang: " + lang + ", country is " + s.substring(3, 5));
                    return true;
                }
            }
        }
        return CRASH_RIL;
    }

    @Override
    public String getMenuTitleFromEf() {
        return this.mMenuTitleFromEf;
    }

    private void fetchCPHSOns() {
        log("fetchCPHSOns()");
        this.cphsOnsl = null;
        this.cphsOnss = null;
        this.mFh.loadEFTransparent(IccConstants.EF_SPN_CPHS, obtainMessage(105));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_SPN_SHORT_CPHS, obtainMessage(106));
        this.mRecordsToLoad++;
    }

    private void fetchRatBalancing() {
        if (this.mTelephonyExt.isSetLanguageBySIM()) {
            return;
        }
        log("support MTK_RAT_BALANCING");
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            log("start loading EF_RAT");
            this.mFh.loadEFTransparent(IccConstants.EF_RAT, obtainMessage(EVENT_GET_RAT_DONE));
            this.mRecordsToLoad++;
        } else {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
                log("loading EF_RAT fail, because of SIM");
                this.mEfRatLoaded = CRASH_RIL;
                this.mEfRat = null;
                boradcastEfRatContentNotify(512);
                return;
            }
            log("loading EF_RAT fail, because of +EUSIM");
        }
    }

    @Override
    public int getEfRatBalancing() {
        log("getEfRatBalancing: iccCardType = " + this.mParentApp.getType() + ", mEfRatLoaded = " + this.mEfRatLoaded + ", mEfRat is null = " + (this.mEfRat == null ? true : CRASH_RIL));
        return (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM && this.mEfRatLoaded && this.mEfRat == null) ? 256 : 512;
    }

    public boolean isHPlmn(String plmn) {
        ServiceStateTracker sst = this.mPhone.getServiceStateTracker();
        if (sst != null) {
            return sst.isHPlmn(plmn);
        }
        log("can't get sst");
        return CRASH_RIL;
    }

    private boolean isMatchingPlmnForEfOpl(String simPlmn, String bcchPlmn) {
        if (simPlmn == null || simPlmn.equals(UsimPBMemInfo.STRING_NOT_SET) || bcchPlmn == null || bcchPlmn.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return CRASH_RIL;
        }
        log("isMatchingPlmnForEfOpl(): simPlmn = " + simPlmn + ", bcchPlmn = " + bcchPlmn);
        int simPlmnLen = simPlmn.length();
        int bcchPlmnLen = bcchPlmn.length();
        if (simPlmnLen < 5 || bcchPlmnLen < 5) {
            return CRASH_RIL;
        }
        for (int i = 0; i < 5; i++) {
            if (simPlmn.charAt(i) != 'd' && simPlmn.charAt(i) != bcchPlmn.charAt(i)) {
                return CRASH_RIL;
            }
        }
        if (simPlmnLen == 6 && bcchPlmnLen == 6) {
            if (simPlmn.charAt(5) == 'd' || simPlmn.charAt(5) == bcchPlmn.charAt(5)) {
                return true;
            }
            return CRASH_RIL;
        }
        if (bcchPlmnLen == 6 && bcchPlmn.charAt(5) != '0' && bcchPlmn.charAt(5) != 'd') {
            return CRASH_RIL;
        }
        if (simPlmnLen != 6 || simPlmn.charAt(5) == '0' || simPlmn.charAt(5) == 'd') {
            return true;
        }
        return CRASH_RIL;
    }

    private boolean isPlmnEqualsSimNumeric(String plmn) {
        String mccmnc = getOperatorNumeric();
        if (plmn == null) {
            return CRASH_RIL;
        }
        if (mccmnc == null || mccmnc.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            log("isPlmnEqualsSimNumeric: getOperatorNumeric error: " + mccmnc);
            return CRASH_RIL;
        }
        if (plmn.equals(mccmnc)) {
            return true;
        }
        if (plmn.length() == 5 && mccmnc.length() == 6 && plmn.equals(mccmnc.substring(0, 5))) {
            return true;
        }
        return CRASH_RIL;
    }

    public String getEonsIfExist(String plmn, int nLac, boolean bLongNameRequired) {
        log("EONS getEonsIfExist: plmn is " + plmn + " nLac is " + nLac + " bLongNameRequired: " + bLongNameRequired);
        if (plmn == null || this.mPnnNetworkNames == null || this.mPnnNetworkNames.size() == 0) {
            return null;
        }
        int nPnnIndex = -1;
        boolean isHPLMN = isPlmnEqualsSimNumeric(plmn);
        if (this.mOperatorList != null) {
            for (int i = 0; i < this.mOperatorList.size(); i++) {
                OplRecord oplRec = this.mOperatorList.get(i);
                if (ENGDEBUG) {
                    log("getEonsIfExist: record number is " + i + " sPlmn: " + oplRec.sPlmn + " nMinLAC: " + oplRec.nMinLAC + " nMaxLAC: " + oplRec.nMaxLAC + " PnnIndex " + oplRec.nPnnIndex);
                }
                if (isMatchingPlmnForEfOpl(oplRec.sPlmn, plmn) && ((oplRec.nMinLAC == 0 && oplRec.nMaxLAC == 65534) || (oplRec.nMinLAC <= nLac && oplRec.nMaxLAC >= nLac))) {
                    log("getEonsIfExist: find it in EF_OPL");
                    if (oplRec.nPnnIndex == 0) {
                        log("getEonsIfExist: oplRec.nPnnIndex is 0, from other sources");
                        return null;
                    }
                    nPnnIndex = oplRec.nPnnIndex;
                }
            }
        } else {
            if (!isHPLMN) {
                log("getEonsIfExist: Plmn is not HPLMN and no mOperatorList, return null");
                return null;
            }
            log("getEonsIfExist: Plmn is HPLMN, return PNN's first record");
            nPnnIndex = 1;
        }
        if (nPnnIndex == -1 && isHPLMN && this.mOperatorList.size() == 1) {
            log("getEonsIfExist: not find it in EF_OPL, but Plmn is HPLMN, return PNN's first record");
            nPnnIndex = 1;
        } else if (nPnnIndex > 1 && nPnnIndex > this.mPnnNetworkNames.size() && isHPLMN) {
            log("getEonsIfExist: find it in EF_OPL, but index in EF_OPL > EF_PNN list length & Plmn is HPLMN, return PNN's first record");
            nPnnIndex = 1;
        } else if (nPnnIndex > 1 && nPnnIndex > this.mPnnNetworkNames.size() && !isHPLMN) {
            log("getEonsIfExist: find it in EF_OPL, but index in EF_OPL > EF_PNN list length & Plmn is not HPLMN, return PNN's first record");
            nPnnIndex = -1;
        }
        String str = null;
        if (nPnnIndex >= 1) {
            OperatorName opName = this.mPnnNetworkNames.get(nPnnIndex - 1);
            if (bLongNameRequired) {
                if (opName.sFullName != null) {
                    str = new String(opName.sFullName);
                } else if (opName.sShortName != null) {
                    str = new String(opName.sShortName);
                }
            } else if (!bLongNameRequired) {
                if (opName.sShortName != null) {
                    str = new String(opName.sShortName);
                } else if (opName.sFullName != null) {
                    str = new String(opName.sFullName);
                }
            }
        }
        log("getEonsIfExist: sEons is " + str);
        return str;
    }

    @Override
    public String getEfGbabp() {
        log("GBABP = " + this.mGbabp);
        return this.mGbabp;
    }

    @Override
    public void setEfGbabp(String gbabp, Message onComplete) {
        byte[] data = IccUtils.hexStringToBytes(gbabp);
        log("setEfGbabp data = " + data);
        this.mFh.updateEFTransparent(IccConstants.EF_GBABP, data, onComplete);
    }

    @Override
    public byte[] getEfPsismsc() {
        log("PSISMSC = " + this.mEfPsismsc);
        return this.mEfPsismsc;
    }

    @Override
    public byte[] getEfSmsp() {
        log("mEfSmsp = " + this.mEfPsismsc);
        return this.mEfSmsp;
    }

    @Override
    public int getMncLength() {
        log("mncLength = " + this.mMncLength);
        return this.mMncLength;
    }

    private class RebootClickListener implements DialogInterface.OnClickListener {
        RebootClickListener(SIMRecords this$0, RebootClickListener rebootClickListener) {
            this();
        }

        private RebootClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            SIMRecords.this.log("Unlock Phone onClick");
            PowerManager pm = (PowerManager) SIMRecords.this.mContext.getSystemService("power");
            pm.reboot("Unlock state changed");
        }
    }

    public void broadcastPhbStateChangedIntent(boolean isReady) {
        if (this.mPhone.getPhoneType() != 1) {
            log("broadcastPhbStateChangedIntent, Not active Phone.");
            return;
        }
        log("broadcastPhbStateChangedIntent, mPhbReady " + this.mPhbReady);
        if (isReady) {
            int phoneId = this.mParentApp.getPhoneId();
            this.mSubId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
            String strAllSimState = SystemProperties.get("gsm.sim.state");
            String strCurSimState = UsimPBMemInfo.STRING_NOT_SET;
            if (strAllSimState != null && strAllSimState.length() > 0) {
                String[] values = strAllSimState.split(",");
                if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                    strCurSimState = values[phoneId];
                }
            }
            if (this.mSubId <= 0 || strCurSimState.equals("NOT_READY")) {
                log("broadcastPhbStateChangedIntent, mSubId " + this.mSubId + ", sim state " + strAllSimState);
                this.mPhbWaitSub = true;
                return;
            }
        } else if (this.mSubId <= 0) {
            log("broadcastPhbStateChangedIntent, isReady == false and mSubId <= 0");
            return;
        }
        Intent intent = new Intent("android.intent.action.PHB_STATE_CHANGED");
        intent.putExtra("ready", isReady);
        intent.putExtra("subscription", this.mSubId);
        log("Broadcasting intent ACTION_PHB_STATE_CHANGED " + isReady + " sub id " + this.mSubId + " phoneId " + this.mParentApp.getPhoneId());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (isReady) {
            return;
        }
        this.mSubId = -1;
    }

    @Override
    public boolean isPhbReady() {
        boolean isSimLocked;
        log("isPhbReady(): cached mPhbReady = " + (this.mPhbReady ? "true" : "false"));
        String strCurSimState = UsimPBMemInfo.STRING_NOT_SET;
        int phoneId = this.mParentApp.getPhoneId();
        String strPhbReady = SystemProperties.get(SIMRECORD_PROPERTY_RIL_PHB_READY[this.mParentApp.getSlotId()], "false");
        String strAllSimState = SystemProperties.get("gsm.sim.state");
        if (strAllSimState != null && strAllSimState.length() > 0) {
            String[] values = strAllSimState.split(",");
            if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                strCurSimState = values[phoneId];
            }
        }
        if (strCurSimState.equals("NETWORK_LOCKED")) {
            isSimLocked = true;
        } else {
            isSimLocked = strCurSimState.equals("PIN_REQUIRED");
        }
        if (strPhbReady.equals("true") && !isSimLocked) {
            this.mPhbReady = true;
        } else {
            this.mPhbReady = CRASH_RIL;
        }
        log("isPhbReady(): mPhbReady = " + (this.mPhbReady ? "true" : "false") + ", strCurSimState = " + strCurSimState);
        return this.mPhbReady;
    }

    public void setPhbReady(boolean isReady) {
        log("setPhbReady(): isReady = " + (isReady ? "true" : "false"));
        if (this.mPhbReady == isReady) {
            return;
        }
        String strPhbReady = isReady ? "true" : "false";
        this.mPhbReady = isReady;
        SystemProperties.set(SIMRECORD_PROPERTY_RIL_PHB_READY[this.mParentApp.getSlotId()], strPhbReady);
        broadcastPhbStateChangedIntent(this.mPhbReady);
    }

    @Override
    public boolean isRadioAvailable() {
        if (this.mCi != null) {
            return this.mCi.getRadioState().isAvailable();
        }
        return CRASH_RIL;
    }

    private class RadioTechnologyChangedReceiver extends BroadcastReceiver {
        RadioTechnologyChangedReceiver(SIMRecords this$0, RadioTechnologyChangedReceiver radioTechnologyChangedReceiver) {
            this();
        }

        private RadioTechnologyChangedReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                return;
            }
            int phoneid = intent.getIntExtra("phone", -1);
            SIMRecords.this.log("[ACTION_RADIO_TECHNOLOGY_CHANGED] phoneid : " + phoneid);
            if (SIMRecords.this.mParentApp == null || SIMRecords.this.mParentApp.getPhoneId() != phoneid) {
                return;
            }
            String activePhoneName = intent.getStringExtra("phoneName");
            int subid = intent.getIntExtra("subscription", -1);
            SIMRecords.this.log("[ACTION_RADIO_TECHNOLOGY_CHANGED] activePhoneName : " + activePhoneName + " | subid : " + subid);
            if ("CDMA".equals(activePhoneName)) {
                return;
            }
            SIMRecords.this.sendMessageDelayed(SIMRecords.this.obtainMessage(200), 500L);
            SIMRecords.this.mAdnCache.reset();
        }
    }

    @Override
    protected int getChildPhoneId() {
        int phoneId = this.mParentApp.getPhoneId();
        log("[getChildPhoneId] phoneId = " + phoneId);
        return phoneId;
    }

    @Override
    protected void updatePHBStatus(int status, boolean isSimLocked) {
        log("[updatePHBStatus] status : " + status + " | isSimLocked : " + isSimLocked + " | mPhbReady : " + this.mPhbReady);
        if (status == 1) {
            if (!isSimLocked) {
                if (this.mPhbReady) {
                    return;
                }
                this.mPhbReady = true;
                broadcastPhbStateChangedIntent(this.mPhbReady);
                return;
            }
            log("phb ready but sim is not ready.");
            return;
        }
        if (status != 0 || !this.mPhbReady) {
            return;
        }
        this.mAdnCache.reset();
        this.mPhbReady = CRASH_RIL;
        broadcastPhbStateChangedIntent(this.mPhbReady);
    }
}
