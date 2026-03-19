package com.mediatek.internal.telephony.uicc;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.radio.V1_0.LastCallFailCause;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordLoader;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UsimServiceTable;
import com.mediatek.internal.telephony.MtkIccUtils;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.MtkSuppServHelper;
import com.mediatek.internal.telephony.MtkSuppServManager;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.phb.MtkAdnRecordCache;
import com.mediatek.internal.telephony.ppl.PplControlData;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.IccServiceInfo;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MtkSIMRecords extends SIMRecords {
    public static final int EF_RAT_FOR_OTHER_CASE = 512;
    public static final int EF_RAT_NOT_EXIST_IN_USIM = 256;
    public static final int EF_RAT_UNDEFINED = -256;
    private static final int EVENT_CFU_IND = 1021;
    private static final int EVENT_DELAYED_SEND_PHB_CHANGE = 1026;
    private static final int EVENT_DUAL_IMSI_READY = 1004;
    private static final int EVENT_EF_CSP_PLMN_MODE_BIT_CHANGED = 1013;
    private static final int EVENT_GET_ALL_OPL_DONE = 1008;
    private static final int EVENT_GET_ALL_PNN_DONE = 1028;
    private static final int EVENT_GET_CPHSONS_DONE = 1009;
    private static final int EVENT_GET_EF_ICCID_DONE = 1024;
    private static final int EVENT_GET_GBABP_DONE = 1019;
    private static final int EVENT_GET_GBANL_DONE = 1020;
    private static final int EVENT_GET_NEW_MSISDN_DONE = 1016;
    private static final int EVENT_GET_PSISMSC_DONE = 1017;
    private static final int EVENT_GET_RAT_DONE = 1014;
    private static final int EVENT_GET_SHORT_CPHSONS_DONE = 1010;
    private static final int EVENT_GET_SMSP_DONE = 1018;
    private static final int EVENT_IMSI_REFRESH_QUERY = 1022;
    private static final int EVENT_IMSI_REFRESH_QUERY_DONE = 1023;
    public static final int EVENT_MSISDN = 100;
    public static final int EVENT_OPL = 101;
    private static final int EVENT_PHB_READY = 1027;
    public static final int EVENT_PNN = 102;
    private static final int EVENT_QUERY_ICCID_DONE = 1011;
    private static final int EVENT_QUERY_ICCID_DONE_FOR_HOT_SWAP = 1015;
    private static final int EVENT_QUERY_MENU_TITLE_DONE = 1005;
    private static final int EVENT_RADIO_AVAILABLE = 1001;
    private static final int EVENT_RADIO_STATE_CHANGED = 1012;
    private static final int GSM_PHB_NOT_READY = 0;
    private static final int GSM_PHB_READY = 1;
    private static final String KEY_SIM_ID = "SIM_ID";
    protected static final String LOG_TAG_EX = "MtkSIMRecords";
    private static final int MTK_SIM_RECORD_EVENT_BASE = 1000;
    private static final String SIMRECORD_PROPERTY_RIL_PHB_READY = "vendor.gsm.sim.ril.phbready";
    private String[] SIM_RECORDS_PROPERTY_MCC_MNC;
    String cphsOnsl;
    String cphsOnss;
    private int efLanguageToLoad;
    private boolean hasQueryIccId;
    private int iccIdQueryState;
    private boolean isDispose;
    private boolean isValidMBI;
    private byte[] mEfELP;
    private ArrayList<byte[]> mEfGbanlList;
    private byte[] mEfPsismsc;
    private byte[] mEfRat;
    private boolean mEfRatLoaded;
    private byte[] mEfSST;
    private byte[] mEfSmsp;
    private String mGbabp;
    private String[] mGbanl;
    private boolean mIsPhbEfResetDone;
    private String mMenuTitleFromEf;
    private IMtkSimHandler mMtkSimHandler;
    protected String mOldMccMnc;
    private String mOldOperatorDefaultName;
    private ArrayList<OplRecord> mOperatorList;
    private boolean mPendingPhbNotify;
    private boolean mPhbReady;
    private PhbBroadCastReceiver mPhbReceiver;
    private boolean mPhbWaitSub;
    private Phone mPhone;
    private ArrayList<OperatorName> mPnnNetworkNames;
    private boolean mReadingOpl;
    private String mSimImsi;
    private BroadcastReceiver mSimReceiver;
    protected int mSlotId;
    private String mSpNameInEfSpn;
    private MtkSpnOverride mSpnOverride;
    private int mSubId;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    private UiccCard mUiccCard;
    private UiccController mUiccController;
    protected static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    protected static final boolean USERDEBUG = TextUtils.equals(Build.TYPE, DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    private static final String[] LANGUAGE_CODE_FOR_LP = {"de", "en", "it", "fr", "es", "nl", "sv", "da", "pt", "fi", "no", "el", "tr", "hu", "pl", "", "cs", "he", "ar", "ru", "is", "", "", "", "", "", "", "", "", "", "", ""};
    static final String[] SIMRECORD_PROPERTY_RIL_PUK1 = {"vendor.gsm.sim.retry.puk1", "vendor.gsm.sim.retry.puk1.2", "vendor.gsm.sim.retry.puk1.3", "vendor.gsm.sim.retry.puk1.4"};
    private static final int[] simServiceNumber = {1, 17, 51, 52, 54, 55, 56, 0, 12, 3, 7, 0, 0};
    private static final int[] usimServiceNumber = {0, 19, 45, 46, 48, 49, 51, 71, 12, 2, 0, 42, 0};

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

    public MtkSIMRecords(MtkUiccCardApplication mtkUiccCardApplication, Context context, CommandsInterface commandsInterface) {
        super(mtkUiccCardApplication, context, commandsInterface);
        this.mSubId = -1;
        this.mPhbReady = false;
        this.mPhbWaitSub = false;
        this.mIsPhbEfResetDone = false;
        this.mPendingPhbNotify = false;
        this.isValidMBI = false;
        this.mEfRatLoaded = false;
        this.mEfRat = null;
        this.iccIdQueryState = -1;
        this.efLanguageToLoad = 0;
        this.mSimImsi = null;
        this.mEfSST = null;
        this.mEfELP = null;
        this.mEfPsismsc = null;
        this.mEfSmsp = null;
        this.SIM_RECORDS_PROPERTY_MCC_MNC = new String[]{"vendor.gsm.ril.uicc.mccmnc", "vendor.gsm.ril.uicc.mccmnc.1", "vendor.gsm.ril.uicc.mccmnc.2", "vendor.gsm.ril.uicc.mccmnc.3"};
        this.mPnnNetworkNames = null;
        this.mOperatorList = null;
        this.mSpNameInEfSpn = null;
        this.mMenuTitleFromEf = null;
        this.isDispose = false;
        this.mSpnOverride = null;
        this.mTelephonyCustomizationFactory = null;
        this.mMtkSimHandler = null;
        this.mOldMccMnc = "";
        this.mOldOperatorDefaultName = null;
        this.mReadingOpl = false;
        mtkLog("MtkSIMRecords constructor");
        this.mSlotId = mtkUiccCardApplication.getPhoneId();
        this.mUiccController = UiccController.getInstance();
        this.mUiccCard = this.mUiccController.getUiccCard(this.mSlotId);
        mtkLog("mUiccCard Instance = " + this.mUiccCard);
        this.mPhone = PhoneFactory.getPhone(mtkUiccCardApplication.getPhoneId());
        this.mSpnOverride = MtkSpnOverride.getInstance();
        this.cphsOnsl = null;
        this.cphsOnss = null;
        this.hasQueryIccId = false;
        this.mCi.registerForCallForwardingInfo(this, EVENT_CFU_IND, null);
        this.mCi.registerForRadioStateChanged(this, 1012, (Object) null);
        this.mCi.registerForAvailable(this, 1001, (Object) null);
        this.mCi.registerForImsiRefreshDone(this, EVENT_IMSI_REFRESH_QUERY, null);
        this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(context);
        if (this.mTelephonyCustomizationFactory != null) {
            this.mMtkSimHandler = this.mTelephonyCustomizationFactory.makeMtkSimHandler(context, commandsInterface);
            this.mMtkSimHandler.setPhoneId(this.mSlotId);
        }
        this.mSimReceiver = new SIMBroadCastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mSimReceiver, intentFilter);
        this.mAdnCache = new MtkAdnRecordCache(this.mFh, commandsInterface, mtkUiccCardApplication);
        this.mCi.registerForPhbReady(this, EVENT_PHB_READY, null);
        this.mPhbReceiver = new PhbBroadCastReceiver();
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intentFilter2.addAction("android.intent.action.RADIO_TECHNOLOGY");
        intentFilter2.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mPhbReceiver, intentFilter2);
        mtkLog("SIMRecords updateIccRecords");
        if (this.mPhone != null && this.mPhone.getIccPhoneBookInterfaceManager() != null) {
            this.mPhone.getIccPhoneBookInterfaceManager().updateIccRecords(this);
        }
        if (isPhbReady()) {
            mtkLog("Phonebook is ready.");
            this.mPhbReady = true;
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
    }

    public void dispose() {
        mtkLog("Disposing MtkSIMRecords this=" + this);
        this.isDispose = true;
        if (this.mMtkSimHandler != null) {
            this.mMtkSimHandler.dispose();
        }
        this.mCi.unregisterForCallForwardingInfo(this);
        this.mCi.unregisterForRadioStateChanged(this);
        this.mContext.unregisterReceiver(this.mSimReceiver);
        this.mIccId = null;
        this.mImsi = null;
        this.mCi.unregisterForPhbReady(this);
        this.mContext.unregisterReceiver(this.mPhbReceiver);
        this.mPhbWaitSub = false;
        this.mPendingPhbNotify = false;
        if (this.mPhbReady) {
            mtkLog("Disposing MtkSIMRecords set PHB unready");
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
        this.mCallForwardingStatus = 0;
        MtkSuppServHelper suppServHelper = MtkSuppServManager.getSuppServHelper(this.mPhone.getPhoneId());
        if (suppServHelper != null) {
            suppServHelper.sendMessage(suppServHelper.obtainMessage(16));
        }
        super.dispose();
    }

    protected void resetRecords() {
        super.resetRecords();
        setSystemProperty("vendor.gsm.sim.operator.default-name", null);
    }

    public boolean checkEfCfis() {
        boolean z = this.mEfCfis != null && this.mEfCfis.length == 16;
        StringBuilder sb = new StringBuilder();
        sb.append("mEfCfis is null? = ");
        sb.append(this.mEfCfis == null);
        mtkLog(sb.toString());
        return z;
    }

    public String getVoiceMailNumber() {
        mtkLog("getVoiceMailNumber " + this.mVoiceMailNum);
        return super.getVoiceMailNumber();
    }

    public void setVoiceMailNumber(String str, String str2, Message message) {
        mtkLog("setVoiceMailNumber, mIsVoiceMailFixed " + this.mIsVoiceMailFixed + ", mMailboxIndex " + this.mMailboxIndex + ", mMailboxIndex " + this.mMailboxIndex + " isCphsMailboxEnabled: " + isCphsMailboxEnabled());
        super.setVoiceMailNumber(str, str2, message);
    }

    public void setVoiceCallForwardingFlag(int i, boolean z, String str) {
        int i2;
        Rlog.d("SIMRecords", "setVoiceCallForwardingFlag: " + z);
        if (i != 1) {
            return;
        }
        if (!z) {
            i2 = 0;
        } else {
            i2 = 1;
        }
        this.mCallForwardingStatus = i2;
        mtkLog(" mRecordsEventsRegistrants: size=" + this.mRecordsEventsRegistrants.size());
        this.mRecordsEventsRegistrants.notifyResult(1);
        setCFUStatusToLocal(z);
        try {
            if (checkEfCfis()) {
                if (z) {
                    byte[] bArr = this.mEfCfis;
                    bArr[1] = (byte) (bArr[1] | 1);
                } else {
                    byte[] bArr2 = this.mEfCfis;
                    bArr2[1] = (byte) (bArr2[1] & 254);
                }
                mtkLog("setVoiceCallForwardingFlag: enable=" + z + " mEfCfis=" + IccUtils.bytesToHexString(this.mEfCfis));
                if (z && !TextUtils.isEmpty(str)) {
                    logv("EF_CFIS: updating cf number, " + Rlog.pii("SIMRecords", str));
                    byte[] bArrNumberToCalledPartyBCD = PhoneNumberUtils.numberToCalledPartyBCD(convertNumberIfContainsPrefix(str));
                    System.arraycopy(bArrNumberToCalledPartyBCD, 0, this.mEfCfis, 3, bArrNumberToCalledPartyBCD.length);
                    this.mEfCfis[2] = (byte) bArrNumberToCalledPartyBCD.length;
                    this.mEfCfis[14] = -1;
                    this.mEfCfis[15] = -1;
                }
                if (this.mFh != null) {
                    this.mFh.updateEFLinearFixed(28619, 1, this.mEfCfis, (String) null, obtainMessage(14, 28619));
                } else {
                    log("setVoiceCallForwardingFlag: mFh is null, skip update EF_CFIS");
                }
            } else {
                mtkLog("setVoiceCallForwardingFlag: ignoring enable=" + z + " invalid mEfCfis=" + IccUtils.bytesToHexString(this.mEfCfis));
            }
            if (this.mEfCff != null) {
                if (z) {
                    this.mEfCff[0] = (byte) ((this.mEfCff[0] & 240) | 10);
                } else {
                    this.mEfCff[0] = (byte) ((this.mEfCff[0] & 240) | 5);
                }
                if (this.mFh != null) {
                    this.mFh.updateEFTransparent(28435, this.mEfCff, obtainMessage(14, 28435));
                } else {
                    log("setVoiceCallForwardingFlag: mFh is null, skip update EF_CFF_CPHS");
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logw("Error saving call forwarding flag to SIM. Probably malformed SIM record", e);
        }
    }

    public String getSIMCPHSOns() {
        if (this.cphsOnsl != null) {
            return this.cphsOnsl;
        }
        return this.cphsOnss;
    }

    public void handleMessage(Message message) throws Throwable {
        String operatorNumeric;
        String str;
        String str2;
        if (this.mDestroyed.get()) {
            if (message.what != 90) {
                mtkLoge("Received message " + message + "[" + message.what + "]  while being destroyed. Ignoring.");
                return;
            }
            mtkLoge("Received message " + message + "[" + message.what + "]  while being destroyed. Keep going!");
        }
        boolean z = true;
        boolean z2 = false;
        try {
            try {
                try {
                } catch (RuntimeException e) {
                    e = e;
                    logw("Exception parsing SIM record", e);
                    if (!z) {
                        return;
                    }
                }
            } catch (Throwable th) {
                th = th;
                if (z) {
                    onRecordLoaded();
                }
                throw th;
            }
        } catch (RuntimeException e2) {
            e = e2;
            z = false;
            logw("Exception parsing SIM record", e);
            if (!z) {
            }
        } catch (Throwable th2) {
            th = th2;
            z = false;
            if (z) {
            }
            throw th;
        }
        switch (message.what) {
            case 1:
                onReady();
                z = false;
                if (!z) {
                    return;
                }
                onRecordLoaded();
                return;
            case 3:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null) {
                    this.mImsi = (String) asyncResult.result;
                    if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                        mtkLoge("invalid IMSI " + this.mImsi);
                        this.mImsi = null;
                    }
                    mtkLog("IMSI: mMncLength=" + this.mMncLength);
                    if (this.mImsi != null) {
                        mtkLog("IMSI: " + this.mImsi.substring(0, 6) + Rlog.pii("SIMRecords", this.mImsi.substring(6)));
                    }
                    String imsi = getIMSI();
                    if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi != null && imsi.length() >= 6) {
                        String strSubstring = imsi.substring(0, 6);
                        String[] strArr = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                        int length = strArr.length;
                        int i = 0;
                        while (true) {
                            if (i < length) {
                                if (strArr[i].equals(strSubstring)) {
                                    this.mMncLength = 3;
                                    mtkLog("IMSI: setting1 mMncLength=" + this.mMncLength);
                                } else {
                                    i++;
                                }
                            }
                        }
                    }
                    if (this.mMncLength == 0 || this.mMncLength == -1) {
                        try {
                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi.substring(0, 3)));
                            mtkLog("setting2 mMncLength=" + this.mMncLength);
                        } catch (NumberFormatException e3) {
                            this.mMncLength = 0;
                            mtkLoge("Corrupt IMSI! setting3 mMncLength=" + this.mMncLength);
                        }
                    }
                    if (this.mMncLength != 0 && this.mMncLength != -1) {
                        mtkLog("update mccmnc=" + imsi.substring(0, this.mMncLength + 3));
                        updateConfiguration(imsi.substring(0, 3 + this.mMncLength));
                    }
                    this.mImsiReadyRegistrants.notifyRegistrants();
                    break;
                } else {
                    mtkLoge("Exception querying IMSI, Exception:" + asyncResult.exception);
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 5:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                byte[] bArr = (byte[]) asyncResult2.result;
                if (asyncResult2.exception == null) {
                    mtkLog("EF_MBI: " + IccUtils.bytesToHexString(bArr));
                    this.mMailboxIndex = bArr[0] & PplMessageManager.Type.INVALID;
                    if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
                        mtkLog("Got valid mailbox number for MBDN");
                        this.isValidMBI = true;
                        z2 = true;
                    }
                }
                this.mRecordsToLoad++;
                if (z2) {
                    mtkLog("EVENT_GET_MBI_DONE, to load EF_MBDN");
                    new AdnRecordLoader(this.mFh).loadFromEF(28615, 28616, this.mMailboxIndex, obtainMessage(6));
                } else if (isCphsMailboxEnabled()) {
                    mtkLog("EVENT_GET_MBI_DONE, to load EF_MAILBOX_CPHS");
                    new AdnRecordLoader(this.mFh).loadFromEF(28439, 28490, 1, obtainMessage(11));
                } else {
                    mtkLog("EVENT_GET_MBI_DONE, do nothing");
                    this.mRecordsToLoad--;
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 9:
                try {
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    byte[] bArr2 = (byte[]) asyncResult3.result;
                    if (asyncResult3.exception == null) {
                        mtkLog("EF_AD: " + IccUtils.bytesToHexString(bArr2));
                        if (bArr2.length < 3) {
                            mtkLog("Corrupt AD data on SIM");
                            String imsi2 = getIMSI();
                            if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi2 != null && imsi2.length() >= 6) {
                                String strSubstring2 = imsi2.substring(0, 6);
                                mtkLog("mccmncCode=" + strSubstring2);
                                String[] strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                int length2 = strArr2.length;
                                int i2 = 0;
                                while (true) {
                                    if (i2 < length2) {
                                        if (strArr2[i2].equals(strSubstring2)) {
                                            this.mMncLength = 3;
                                            mtkLog("setting6 mMncLength=" + this.mMncLength);
                                        } else {
                                            i2++;
                                        }
                                    }
                                }
                            }
                            if (this.mMncLength == 0 || this.mMncLength == -1) {
                                if (imsi2 != null) {
                                    try {
                                        this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi2.substring(0, 3)));
                                        mtkLog("setting7 mMncLength=" + this.mMncLength);
                                    } catch (NumberFormatException e4) {
                                        this.mMncLength = 0;
                                        mtkLoge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                    }
                                } else {
                                    this.mMncLength = 0;
                                    mtkLog("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                }
                            }
                            if (imsi2 != null && this.mMncLength != 0) {
                                mtkLog("update mccmnc=" + imsi2.substring(0, this.mMncLength + 3));
                                updateConfiguration(imsi2.substring(0, 3 + this.mMncLength));
                            }
                            operatorNumeric = getOperatorNumeric();
                            if (TextUtils.isEmpty(operatorNumeric)) {
                                str = "onAllRecordsLoaded empty mccmnc skipping";
                                mtkLog(str);
                            } else {
                                mtkLog("EVENT_GET_AD_DONE set mccmnc to operator='" + operatorNumeric + "'");
                                str2 = this.SIM_RECORDS_PROPERTY_MCC_MNC[this.mSlotId];
                                SystemProperties.set(str2, operatorNumeric);
                            }
                        } else {
                            if ((bArr2[0] & 1) == 1 && (bArr2[2] & 1) == 1) {
                                mtkLog("SIMRecords: Cipher is enable");
                            }
                            if (bArr2.length == 3) {
                                mtkLog("MNC length not present in EF_AD");
                                String imsi3 = getIMSI();
                                if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi3 != null && imsi3.length() >= 6) {
                                    String strSubstring3 = imsi3.substring(0, 6);
                                    mtkLog("mccmncCode=" + strSubstring3);
                                    String[] strArr3 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                    int length3 = strArr3.length;
                                    int i3 = 0;
                                    while (true) {
                                        if (i3 < length3) {
                                            if (strArr3[i3].equals(strSubstring3)) {
                                                this.mMncLength = 3;
                                                mtkLog("setting6 mMncLength=" + this.mMncLength);
                                            } else {
                                                i3++;
                                            }
                                        }
                                    }
                                }
                                if (this.mMncLength == 0 || this.mMncLength == -1) {
                                    if (imsi3 != null) {
                                        try {
                                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi3.substring(0, 3)));
                                            mtkLog("setting7 mMncLength=" + this.mMncLength);
                                        } catch (NumberFormatException e5) {
                                            this.mMncLength = 0;
                                            mtkLoge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                        }
                                    } else {
                                        this.mMncLength = 0;
                                        mtkLog("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                    }
                                }
                                if (imsi3 != null && this.mMncLength != 0) {
                                    mtkLog("update mccmnc=" + imsi3.substring(0, this.mMncLength + 3));
                                    updateConfiguration(imsi3.substring(0, 3 + this.mMncLength));
                                }
                                operatorNumeric = getOperatorNumeric();
                                if (TextUtils.isEmpty(operatorNumeric)) {
                                    str = "onAllRecordsLoaded empty mccmnc skipping";
                                    mtkLog(str);
                                } else {
                                    mtkLog("EVENT_GET_AD_DONE set mccmnc to operator='" + operatorNumeric + "'");
                                    str2 = this.SIM_RECORDS_PROPERTY_MCC_MNC[this.mSlotId];
                                    SystemProperties.set(str2, operatorNumeric);
                                }
                            } else {
                                this.mMncLength = bArr2[3] & 15;
                                mtkLog("setting4 mMncLength=" + this.mMncLength);
                                if (this.mMncLength == 15) {
                                    this.mMncLength = 0;
                                    mtkLog("setting5 mMncLength=" + this.mMncLength);
                                } else if (this.mMncLength != 2 && this.mMncLength != 3) {
                                    this.mMncLength = -1;
                                    mtkLog("setting5 mMncLength=" + this.mMncLength);
                                }
                                String imsi4 = getIMSI();
                                if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi4 != null && imsi4.length() >= 6) {
                                    String strSubstring4 = imsi4.substring(0, 6);
                                    mtkLog("mccmncCode=" + strSubstring4);
                                    String[] strArr4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                    int length4 = strArr4.length;
                                    int i4 = 0;
                                    while (true) {
                                        if (i4 < length4) {
                                            if (strArr4[i4].equals(strSubstring4)) {
                                                this.mMncLength = 3;
                                                mtkLog("setting6 mMncLength=" + this.mMncLength);
                                            } else {
                                                i4++;
                                            }
                                        }
                                    }
                                }
                                if (this.mMncLength == 0 || this.mMncLength == -1) {
                                    if (imsi4 != null) {
                                        try {
                                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi4.substring(0, 3)));
                                            mtkLog("setting7 mMncLength=" + this.mMncLength);
                                        } catch (NumberFormatException e6) {
                                            this.mMncLength = 0;
                                            mtkLoge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                        }
                                    } else {
                                        this.mMncLength = 0;
                                        mtkLog("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                    }
                                }
                                if (imsi4 != null && this.mMncLength != 0) {
                                    mtkLog("update mccmnc=" + imsi4.substring(0, this.mMncLength + 3));
                                    updateConfiguration(imsi4.substring(0, 3 + this.mMncLength));
                                }
                                String operatorNumeric2 = getOperatorNumeric();
                                if (TextUtils.isEmpty(operatorNumeric2)) {
                                    mtkLog("onAllRecordsLoaded empty mccmnc skipping");
                                } else {
                                    mtkLog("EVENT_GET_AD_DONE set mccmnc to operator='" + operatorNumeric2 + "'");
                                    SystemProperties.set(this.SIM_RECORDS_PROPERTY_MCC_MNC[this.mSlotId], operatorNumeric2);
                                }
                            }
                        }
                        break;
                    } else {
                        String imsi5 = getIMSI();
                        if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi5 != null && imsi5.length() >= 6) {
                            String strSubstring5 = imsi5.substring(0, 6);
                            mtkLog("mccmncCode=" + strSubstring5);
                            String[] strArr5 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                            int length5 = strArr5.length;
                            int i5 = 0;
                            while (true) {
                                if (i5 < length5) {
                                    if (strArr5[i5].equals(strSubstring5)) {
                                        this.mMncLength = 3;
                                        mtkLog("setting6 mMncLength=" + this.mMncLength);
                                    } else {
                                        i5++;
                                    }
                                }
                            }
                        }
                        if (this.mMncLength == 0 || this.mMncLength == -1) {
                            if (imsi5 != null) {
                                try {
                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi5.substring(0, 3)));
                                    mtkLog("setting7 mMncLength=" + this.mMncLength);
                                } catch (NumberFormatException e7) {
                                    this.mMncLength = 0;
                                    mtkLoge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                }
                            } else {
                                this.mMncLength = 0;
                                mtkLog("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                            }
                        }
                        if (imsi5 != null && this.mMncLength != 0) {
                            mtkLog("update mccmnc=" + imsi5.substring(0, this.mMncLength + 3));
                            updateConfiguration(imsi5.substring(0, 3 + this.mMncLength));
                        }
                        operatorNumeric = getOperatorNumeric();
                        if (TextUtils.isEmpty(operatorNumeric)) {
                            str = "onAllRecordsLoaded empty mccmnc skipping";
                            mtkLog(str);
                        } else {
                            mtkLog("EVENT_GET_AD_DONE set mccmnc to operator='" + operatorNumeric + "'");
                            str2 = this.SIM_RECORDS_PROPERTY_MCC_MNC[this.mSlotId];
                            SystemProperties.set(str2, operatorNumeric);
                        }
                        break;
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                    return;
                } catch (Throwable th3) {
                    String imsi6 = getIMSI();
                    if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi6 != null && imsi6.length() >= 6) {
                        String strSubstring6 = imsi6.substring(0, 6);
                        mtkLog("mccmncCode=" + strSubstring6);
                        String[] strArr6 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                        int length6 = strArr6.length;
                        int i6 = 0;
                        while (true) {
                            if (i6 < length6) {
                                if (strArr6[i6].equals(strSubstring6)) {
                                    this.mMncLength = 3;
                                    mtkLog("setting6 mMncLength=" + this.mMncLength);
                                } else {
                                    i6++;
                                }
                            }
                        }
                    }
                    if (this.mMncLength == 0 || this.mMncLength == -1) {
                        if (imsi6 != null) {
                            try {
                                this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi6.substring(0, 3)));
                                mtkLog("setting7 mMncLength=" + this.mMncLength);
                            } catch (NumberFormatException e8) {
                                this.mMncLength = 0;
                                mtkLoge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                            }
                        } else {
                            this.mMncLength = 0;
                            mtkLog("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                        }
                        break;
                    }
                    if (imsi6 != null && this.mMncLength != 0) {
                        mtkLog("update mccmnc=" + imsi6.substring(0, this.mMncLength + 3));
                        updateConfiguration(imsi6.substring(0, 3 + this.mMncLength));
                    }
                    String operatorNumeric3 = getOperatorNumeric();
                    if (TextUtils.isEmpty(operatorNumeric3)) {
                        mtkLog("onAllRecordsLoaded empty mccmnc skipping");
                        throw th3;
                    }
                    mtkLog("EVENT_GET_AD_DONE set mccmnc to operator='" + operatorNumeric3 + "'");
                    SystemProperties.set(this.SIM_RECORDS_PROPERTY_MCC_MNC[this.mSlotId], operatorNumeric3);
                    throw th3;
                }
            case 10:
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4.exception != null) {
                    mtkLoge("Invalid or missing EF[MSISDN]");
                } else {
                    AdnRecord adnRecord = (AdnRecord) asyncResult4.result;
                    this.mMsisdn = adnRecord.getNumber();
                    this.mMsisdnTag = adnRecord.getAlphaTag();
                    this.mRecordsEventsRegistrants.notifyResult(100);
                    mtkLog("MSISDN: xxxxxxx");
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 12:
                mtkLog("EF_SPN loaded and try to extract: ");
                AsyncResult asyncResult5 = (AsyncResult) message.obj;
                this.mSpnState = SIMRecords.GetSpnFsmState.IDLE;
                if (asyncResult5 == null || asyncResult5.exception != null) {
                    mtkLoge(": read spn fail!");
                    this.mSpnDisplayCondition = -1;
                } else {
                    mtkLog("getSpnFsm, Got data from EF_SPN");
                    byte[] bArr3 = (byte[]) asyncResult5.result;
                    this.mSpnDisplayCondition = bArr3[0] & PplMessageManager.Type.INVALID;
                    if (this.mSpnDisplayCondition == 255) {
                        this.mSpnDisplayCondition = -1;
                    }
                    setServiceProviderName(IccUtils.adnStringFieldToString(bArr3, 1, bArr3.length - 1));
                    this.mSpNameInEfSpn = getServiceProviderName();
                    if (this.mSpNameInEfSpn != null && this.mSpNameInEfSpn.equals("")) {
                        mtkLog("set spNameInEfSpn to null because parsing result is empty");
                        this.mSpNameInEfSpn = null;
                    }
                    mtkLog("Load EF_SPN: " + getServiceProviderName() + " spnDisplayCondition: " + this.mSpnDisplayCondition);
                    this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), getServiceProviderName());
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 17:
                AsyncResult asyncResult6 = (AsyncResult) message.obj;
                byte[] bArr4 = (byte[]) asyncResult6.result;
                if (asyncResult6.exception == null) {
                    this.mUsimServiceTable = new UsimServiceTable(bArr4);
                    mtkLog("SST: " + this.mUsimServiceTable);
                    this.mEfSST = bArr4;
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 26:
                AsyncResult asyncResult7 = (AsyncResult) message.obj;
                if (asyncResult7.exception == null) {
                    this.mCphsInfo = (byte[]) asyncResult7.result;
                    mtkLog("iCPHS: " + IccUtils.bytesToHexString(this.mCphsInfo));
                    if (!this.isValidMBI && isCphsMailboxEnabled()) {
                        this.mRecordsToLoad++;
                        new AdnRecordLoader(this.mFh).loadFromEF(28439, 28490, 1, obtainMessage(11));
                    }
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 30:
                AsyncResult asyncResult8 = (AsyncResult) message.obj;
                if (asyncResult8.exception == null) {
                    this.mMsisdn = this.mNewMsisdn;
                    this.mMsisdnTag = this.mNewMsisdnTag;
                    this.mRecordsEventsRegistrants.notifyResult(100);
                    mtkLog("Success to update EF[MSISDN]");
                }
                if (asyncResult8.userObj != null) {
                    AsyncResult.forMessage((Message) asyncResult8.userObj).exception = asyncResult8.exception;
                    ((Message) asyncResult8.userObj).sendToTarget();
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case LastCallFailCause.RADIO_RELEASE_NORMAL:
                super.handleMessage(message);
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1001:
                this.mMsisdn = "";
                this.mRecordsEventsRegistrants.notifyResult(100);
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1005:
                mtkLog("[sume receive response message");
                AsyncResult asyncResult9 = (AsyncResult) message.obj;
                if (asyncResult9 == null || asyncResult9.exception != null) {
                    mtkLog("[sume null AsyncResult or exception.");
                    this.mMenuTitleFromEf = null;
                } else {
                    byte[] bArr5 = (byte[]) asyncResult9.result;
                    if (bArr5 != null && bArr5.length >= 2) {
                        int i7 = bArr5[0] & PplMessageManager.Type.INVALID;
                        int i8 = bArr5[1] & PplMessageManager.Type.INVALID;
                        mtkLog("[sume tag = " + i7 + ", len = " + i8);
                        this.mMenuTitleFromEf = IccUtils.adnStringFieldToString(bArr5, 2, i8);
                        StringBuilder sb = new StringBuilder();
                        sb.append("[sume menu title is ");
                        sb.append(this.mMenuTitleFromEf);
                        mtkLog(sb.toString());
                    }
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1008:
                AsyncResult asyncResult10 = (AsyncResult) message.obj;
                if (asyncResult10.exception == null) {
                    parseEFopl((ArrayList) asyncResult10.result);
                    this.mRecordsEventsRegistrants.notifyResult(101);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1009:
                mtkLog("handleMessage (EVENT_GET_CPHSONS_DONE)");
                AsyncResult asyncResult11 = (AsyncResult) message.obj;
                if (asyncResult11 != null && asyncResult11.exception == null) {
                    byte[] bArr6 = (byte[]) asyncResult11.result;
                    this.cphsOnsl = IccUtils.adnStringFieldToString(bArr6, 0, bArr6.length);
                    mtkLog("Load EF_SPN_CPHS: " + this.cphsOnsl);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1010:
                mtkLog("handleMessage (EVENT_GET_SHORT_CPHSONS_DONE)");
                AsyncResult asyncResult12 = (AsyncResult) message.obj;
                if (asyncResult12 != null && asyncResult12.exception == null) {
                    byte[] bArr7 = (byte[]) asyncResult12.result;
                    this.cphsOnss = IccUtils.adnStringFieldToString(bArr7, 0, bArr7.length);
                    mtkLog("Load EF_SPN_SHORT_CPHS: " + this.cphsOnss);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1013:
                AsyncResult asyncResult13 = (AsyncResult) message.obj;
                if (asyncResult13 != null && asyncResult13.exception == null) {
                    processEfCspPlmnModeBitUrc(((int[]) asyncResult13.result)[0]);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1014:
                mtkLog("handleMessage (EVENT_GET_RAT_DONE)");
                AsyncResult asyncResult14 = (AsyncResult) message.obj;
                this.mEfRatLoaded = true;
                if (asyncResult14 == null || asyncResult14.exception != null) {
                    mtkLog("load EF_RAT fail");
                    this.mEfRat = null;
                    if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                        boradcastEfRatContentNotify(256);
                    } else {
                        boradcastEfRatContentNotify(512);
                    }
                } else {
                    this.mEfRat = (byte[]) asyncResult14.result;
                    mtkLog("load EF_RAT complete: " + ((int) this.mEfRat[0]));
                    boradcastEfRatContentNotify(512);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1017:
                AsyncResult asyncResult15 = (AsyncResult) message.obj;
                byte[] bArr8 = (byte[]) asyncResult15.result;
                if (asyncResult15.exception == null) {
                    mtkLog("EF_PSISMSC: " + IccUtils.bytesToHexString(bArr8));
                    if (bArr8 != null) {
                        this.mEfPsismsc = bArr8;
                    }
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1018:
                AsyncResult asyncResult16 = (AsyncResult) message.obj;
                byte[] bArr9 = (byte[]) asyncResult16.result;
                if (asyncResult16.exception == null) {
                    mtkLog("EF_SMSP: " + IccUtils.bytesToHexString(bArr9));
                    if (bArr9 != null) {
                        this.mEfSmsp = bArr9;
                    }
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1019:
                AsyncResult asyncResult17 = (AsyncResult) message.obj;
                if (asyncResult17.exception == null) {
                    this.mGbabp = IccUtils.bytesToHexString((byte[]) asyncResult17.result);
                    mtkLog("EF_GBABP=" + this.mGbabp);
                } else {
                    mtkLoge("Error on GET_GBABP with exp " + asyncResult17.exception);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1020:
                AsyncResult asyncResult18 = (AsyncResult) message.obj;
                if (asyncResult18.exception == null) {
                    this.mEfGbanlList = (ArrayList) asyncResult18.result;
                    mtkLog("GET_GBANL record count: " + this.mEfGbanlList.size());
                } else {
                    mtkLoge("Error on GET_GBANL with exp " + asyncResult18.exception);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case EVENT_CFU_IND:
                AsyncResult asyncResult19 = (AsyncResult) message.obj;
                if (asyncResult19 != null && asyncResult19.exception == null && asyncResult19.result != null) {
                    mtkLog("handle EVENT_CFU_IND: " + ((int[]) asyncResult19.result)[0]);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case EVENT_IMSI_REFRESH_QUERY:
                if (USERDEBUG) {
                    mtkLog("handleMessage (EVENT_IMSI_REFRESH_QUERY)");
                } else {
                    mtkLog("handleMessage (EVENT_IMSI_REFRESH_QUERY) mImsi= " + getIMSI());
                }
                this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(1023));
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 1023:
                mtkLog("handleMessage (EVENT_IMSI_REFRESH_QUERY_DONE)");
                AsyncResult asyncResult20 = (AsyncResult) message.obj;
                if (asyncResult20.exception == null) {
                    this.mMncLength = 0;
                    this.mImsi = (String) asyncResult20.result;
                    if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                        mtkLoge("invalid IMSI " + this.mImsi);
                        this.mImsi = null;
                    }
                    mtkLog("IMSI: mMncLength=" + this.mMncLength);
                    if (this.mImsi != null) {
                        mtkLog("IMSI: " + this.mImsi.substring(0, 6) + Rlog.pii("SIMRecords", this.mImsi.substring(6)));
                    }
                    String imsi7 = getIMSI();
                    if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi7 != null && imsi7.length() >= 6) {
                        String strSubstring7 = imsi7.substring(0, 6);
                        String[] strArr7 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                        int length7 = strArr7.length;
                        int i9 = 0;
                        while (true) {
                            if (i9 < length7) {
                                if (strArr7[i9].equals(strSubstring7)) {
                                    this.mMncLength = 3;
                                    mtkLog("IMSI: setting1 mMncLength=" + this.mMncLength);
                                } else {
                                    i9++;
                                }
                            }
                        }
                    }
                    if (this.mMncLength == 0 || this.mMncLength == -1) {
                        try {
                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi7.substring(0, 3)));
                            mtkLog("setting2 mMncLength=" + this.mMncLength);
                        } catch (NumberFormatException e9) {
                            this.mMncLength = 0;
                            mtkLoge("Corrupt IMSI! setting3 mMncLength=" + this.mMncLength);
                        }
                    }
                    if (this.mMncLength != 0 && this.mMncLength != -1) {
                        mtkLog("update mccmnc=" + imsi7.substring(0, this.mMncLength + 3));
                        updateConfiguration(imsi7.substring(0, 3 + this.mMncLength));
                    }
                    if (!imsi7.equals(this.mSimImsi)) {
                        this.mSimImsi = imsi7;
                        this.mImsiReadyRegistrants.notifyRegistrants();
                        mtkLog("SimRecords: mImsiReadyRegistrants.notifyRegistrants");
                    }
                    if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
                        onAllRecordsLoaded();
                    }
                    break;
                } else {
                    mtkLoge("Exception querying IMSI, Exception:" + asyncResult20.exception);
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case EVENT_DELAYED_SEND_PHB_CHANGE:
                this.mPhbReady = isPhbReady();
                mtkLog("[EVENT_DELAYED_SEND_PHB_CHANGE] isReady : " + this.mPhbReady);
                broadcastPhbStateChangedIntent(this.mPhbReady);
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case EVENT_PHB_READY:
                AsyncResult asyncResult21 = (AsyncResult) message.obj;
                if (asyncResult21 != null && asyncResult21.exception == null && asyncResult21.result != null) {
                    int[] iArr = (int[]) asyncResult21.result;
                    int simStateForSlotIndex = SubscriptionController.getInstance().getSimStateForSlotIndex(this.mSlotId);
                    if (simStateForSlotIndex != 4 && simStateForSlotIndex != 2) {
                        z = false;
                    }
                    mtkLog("isPhbReady=" + iArr[0] + ",curSimState = " + simStateForSlotIndex + ", isSimLocked = " + z);
                    updatePHBStatus(iArr[0], z);
                    updateIccFdnStatus();
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            case EVENT_GET_ALL_PNN_DONE:
                AsyncResult asyncResult22 = (AsyncResult) message.obj;
                if (asyncResult22.exception == null) {
                    parseEFpnn((ArrayList) asyncResult22.result);
                    this.mRecordsEventsRegistrants.notifyResult(102);
                    if (!this.mReadingOpl) {
                        this.mRecordsEventsRegistrants.notifyResult(101);
                    }
                }
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
            default:
                super.handleMessage(message);
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
        }
    }

    protected void handleFileUpdate(int i) {
        if (i != 20272) {
            if (i == 28435) {
                this.mRecordsToLoad++;
                mtkLog("SIM Refresh called for EF_CFF_CPHS");
                this.mFh.loadEFTransparent(28435, obtainMessage(24));
                return;
            }
            if (i == 28437) {
                this.mRecordsToLoad++;
                mtkLog("[CSP] SIM Refresh for EF_CSP_CPHS");
                this.mFh.loadEFTransparent(28437, obtainMessage(33));
                return;
            }
            if (i == 28439) {
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(28439, 28490, 1, obtainMessage(11));
                return;
            }
            if (i == 28480) {
                this.mRecordsToLoad++;
                mtkLog("SIM Refresh called for EF_MSISDN");
                new AdnRecordLoader(this.mFh).loadFromEF(28480, getExtFromEf(28480), 1, obtainMessage(10));
                return;
            }
            if (i != 28489) {
                if (i == 28615) {
                    this.mRecordsToLoad++;
                    new AdnRecordLoader(this.mFh).loadFromEF(28615, 28616, this.mMailboxIndex, obtainMessage(6));
                    return;
                }
                if (i != 28619) {
                    switch (i) {
                        case 28474:
                            break;
                        case 28475:
                            mtkLog("SIM Refresh called for EF_FDN");
                            this.mParentApp.queryFdn();
                            break;
                        default:
                            mtkLog("handleFileUpdate default");
                            if (((MtkAdnRecordCache) this.mAdnCache).isUsimPhbEfAndNeedReset(i) && !this.mIsPhbEfResetDone) {
                                this.mIsPhbEfResetDone = true;
                                this.mAdnCache.reset();
                                setPhbReady(false);
                            }
                            this.mLoaded.set(false);
                            fetchSimRecords();
                            return;
                    }
                } else {
                    this.mRecordsToLoad++;
                    mtkLog("SIM Refresh called for EF_CFIS");
                    this.mFh.loadEFLinearFixed(28619, 1, obtainMessage(32));
                    return;
                }
            }
        }
        if (!this.mIsPhbEfResetDone) {
            this.mIsPhbEfResetDone = true;
            this.mAdnCache.reset();
            mtkLog("handleFileUpdate ADN like");
            setPhbReady(false);
        }
    }

    protected void handleRefresh(IccRefreshResponse iccRefreshResponse) {
        if (iccRefreshResponse == null) {
            mtkLog("handleSimRefresh received without input");
        }
        if (iccRefreshResponse.aid != null && !TextUtils.isEmpty(iccRefreshResponse.aid) && !iccRefreshResponse.aid.equals(this.mParentApp.getAid()) && iccRefreshResponse.refreshResult != 4) {
            mtkLog("handleRefresh, refreshResponse.aid = " + iccRefreshResponse.aid + ", mParentApp.getAid() = " + this.mParentApp.getAid());
            return;
        }
        switch (iccRefreshResponse.refreshResult) {
            case 0:
                mtkLog("handleRefresh with SIM_REFRESH_FILE_UPDATED");
                handleFileUpdate(iccRefreshResponse.efId);
                this.mIsPhbEfResetDone = false;
                break;
            case 1:
                mtkLog("handleRefresh with SIM_REFRESH_INIT");
                handleFileUpdate(-1);
                break;
            case 2:
                mtkLog("handleRefresh with SIM_REFRESH_RESET");
                if (!SystemProperties.get("ro.vendor.sim_refresh_reset_by_modem").equals("1")) {
                    mtkLog("sim_refresh_reset_by_modem false");
                    if (this.mCi != null) {
                        this.mCi.resetRadio((Message) null);
                    }
                } else {
                    mtkLog("Sim reset by modem!");
                }
                setPhbReady(false);
                handleFileUpdate(-1);
                break;
            case 3:
            default:
                mtkLog("handleSimRefresh callback to parent");
                super.handleRefresh(iccRefreshResponse);
                break;
            case 4:
                mtkLog("handleRefresh with REFRESH_INIT_FULL_FILE_UPDATED");
                setPhbReady(false);
                handleFileUpdate(-1);
                break;
            case 5:
                mtkLog("handleRefresh with REFRESH_INIT_FILE_UPDATED, EFID = " + iccRefreshResponse.efId);
                handleFileUpdate(iccRefreshResponse.efId);
                this.mIsPhbEfResetDone = false;
                if (this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                    sendMessage(obtainMessage(1));
                }
                break;
            case 6:
                mtkLog("handleSimRefresh with REFRESH_SESSION_RESET");
                handleFileUpdate(-1);
                break;
        }
    }

    private String findBestLanguage(byte[] bArr) {
        String[] locales = this.mContext.getAssets().getLocales();
        if (bArr == null || locales == null) {
            return null;
        }
        for (int i = 0; i + 1 < bArr.length; i += 2) {
            try {
                String str = new String(bArr, i, 2, "ISO-8859-1");
                mtkLog("languages from sim = " + str);
                for (int i2 = 0; i2 < locales.length; i2++) {
                    if (locales[i2] != null && locales[i2].length() >= 2 && locales[i2].substring(0, 2).equalsIgnoreCase(str)) {
                        return str;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                mtkLog("Failed to parse USIM language records" + e);
            }
        }
        return null;
    }

    protected void onAllRecordsLoaded() {
        if (this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
            this.mRecordsRequested = false;
            return;
        }
        super.onAllRecordsLoaded();
        if (this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_PIN || this.mParentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
            this.mRecordsRequested = false;
            return;
        }
        setSpnFromConfig(getOperatorNumeric());
        String operatorNumeric = getOperatorNumeric();
        if (USERDEBUG) {
            mtkLog("imsi = *** operator = " + operatorNumeric);
        } else {
            mtkLog("imsi = " + getIMSI() + " operator = " + operatorNumeric);
        }
        if (operatorNumeric != null) {
            if (operatorNumeric.equals("46002") || operatorNumeric.equals("46007")) {
                operatorNumeric = "46000";
            }
            setSystemProperty("vendor.gsm.sim.operator.default-name", MtkSpnOverride.getInstance().lookupOperatorName(MtkSubscriptionManager.getSubIdUsingPhoneId(this.mParentApp.getPhoneId()), operatorNumeric, true, this.mContext));
        }
        fetchPnnAndOpl();
        fetchCPHSOns();
        fetchRatBalancing();
        fetchSmsp();
        fetchGbaRecords();
    }

    protected boolean checkCdma3gCard() {
        boolean z = MtkIccUtilsEx.checkCdma3gCard(this.mSlotId) <= 0;
        log("checkCdma3gCard result: " + z);
        return z;
    }

    protected void setSystemProperty(String str, String str2) {
        if ("vendor.gsm.sim.operator.default-name".equals(str)) {
            if ((this.mOldOperatorDefaultName == null && str2 == null) || (this.mOldOperatorDefaultName != null && this.mOldOperatorDefaultName.equals(str2))) {
                log("set PROPERTY_ICC_OPERATOR_DEFAULT_NAME same value. val:" + str2);
                return;
            }
            this.mOldOperatorDefaultName = str2;
        }
        super.setSystemProperty(str, str2);
    }

    protected void setSpnFromConfig(String str) {
        if (TextUtils.isEmpty(getServiceProviderName()) && this.mSpnOverride.containsCarrier(str)) {
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), this.mSpnOverride.getSpn(str));
        }
    }

    protected void setVoiceMailByCountry(String str) {
        super.setVoiceMailByCountry(str);
        if (this.mVmConfig.containsCarrier(str)) {
            mtkLog("setVoiceMailByCountry");
        }
    }

    protected void fetchSimRecords() {
        super.fetchSimRecords();
    }

    protected boolean isSpnActive() {
        getServiceProviderName();
        if (this.mEfSST != null && this.mParentApp != null) {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                if (this.mEfSST.length >= 3 && (this.mEfSST[2] & 4) == 4) {
                    mtkLog("getDisplayRule USIM mEfSST is " + IccUtils.bytesToHexString(this.mEfSST) + " set bSpnActive to true");
                    return true;
                }
            } else if (this.mEfSST.length >= 5 && (this.mEfSST[4] & 2) == 2) {
                mtkLog("getDisplayRule SIM mEfSST is " + IccUtils.bytesToHexString(this.mEfSST) + " set bSpnActive to true");
                return true;
            }
        }
        return false;
    }

    public String getSpNameInEfSpn() {
        mtkLog("getSpNameInEfSpn(): " + this.mSpNameInEfSpn);
        return this.mSpNameInEfSpn;
    }

    public String isOperatorMvnoForImsi() {
        String strIsOperatorMvnoForImsi = MtkSpnOverride.getInstance().isOperatorMvnoForImsi(getOperatorNumeric(), getIMSI());
        String operatorNumeric = getOperatorNumeric();
        mtkLog("isOperatorMvnoForImsi(), imsiPattern: " + strIsOperatorMvnoForImsi + ", mccmnc: " + operatorNumeric);
        if (strIsOperatorMvnoForImsi == null || operatorNumeric == null) {
            return null;
        }
        String strSubstring = strIsOperatorMvnoForImsi.substring(operatorNumeric.length(), strIsOperatorMvnoForImsi.length());
        mtkLog("isOperatorMvnoForImsi(): " + strSubstring);
        return strSubstring;
    }

    public String getFirstFullNameInEfPnn() {
        if (this.mPnnNetworkNames == null || this.mPnnNetworkNames.size() == 0) {
            mtkLog("getFirstFullNameInEfPnn(): empty");
            return null;
        }
        OperatorName operatorName = this.mPnnNetworkNames.get(0);
        mtkLog("getFirstFullNameInEfPnn(): first fullname: " + operatorName.sFullName);
        if (operatorName.sFullName != null) {
            return new String(operatorName.sFullName);
        }
        return null;
    }

    public String isOperatorMvnoForEfPnn() {
        String operatorNumeric = getOperatorNumeric();
        String firstFullNameInEfPnn = getFirstFullNameInEfPnn();
        mtkLog("isOperatorMvnoForEfPnn(): mccmnc = " + operatorNumeric + ", pnn = " + firstFullNameInEfPnn);
        if (MtkSpnOverride.getInstance().getSpnByEfPnn(operatorNumeric, firstFullNameInEfPnn) != null) {
            return firstFullNameInEfPnn;
        }
        return null;
    }

    public String getMvnoMatchType() {
        String imsi = getIMSI();
        String spNameInEfSpn = getSpNameInEfSpn();
        String firstFullNameInEfPnn = getFirstFullNameInEfPnn();
        String gid1 = getGid1();
        String operatorNumeric = getOperatorNumeric();
        if (USERDEBUG) {
            mtkLog("getMvnoMatchType(): imsi = ***, mccmnc = " + operatorNumeric + ", spn = " + spNameInEfSpn);
        } else {
            mtkLog("getMvnoMatchType(): imsi = " + imsi + ", mccmnc = " + operatorNumeric + ", spn = " + spNameInEfSpn);
        }
        if (MtkSpnOverride.getInstance().getSpnByEfSpn(operatorNumeric, spNameInEfSpn) != null) {
            return "spn";
        }
        if (MtkSpnOverride.getInstance().getSpnByImsi(operatorNumeric, imsi) != null) {
            return "imsi";
        }
        if (MtkSpnOverride.getInstance().getSpnByEfPnn(operatorNumeric, firstFullNameInEfPnn) != null) {
            return "pnn";
        }
        if (MtkSpnOverride.getInstance().getSpnByEfGid1(operatorNumeric, gid1) != null) {
            return "gid";
        }
        return "";
    }

    private class SIMBroadCastReceiver extends BroadcastReceiver {
        private SIMBroadCastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED")) {
                String stringExtra = intent.getStringExtra(DataSubConstants.EXTRA_MOBILE_DATA_ENABLE_REASON);
                int intExtra = intent.getIntExtra("slot", 0);
                String stringExtra2 = intent.getStringExtra("ss");
                MtkSIMRecords.this.mtkLog("SIM_STATE_CHANGED: slot = " + intExtra + ",reason = " + stringExtra + ",simState = " + stringExtra2);
                if ("PUK".equals(stringExtra) && intExtra == MtkSIMRecords.this.mSlotId) {
                    String str = SystemProperties.get(MtkSIMRecords.SIMRECORD_PROPERTY_RIL_PUK1[MtkSIMRecords.this.mSlotId], "0");
                    MtkSIMRecords.this.mtkLog("SIM_STATE_CHANGED: strPuk1Count = " + str);
                    MtkSIMRecords.this.mMsisdn = "";
                    MtkSIMRecords.this.mRecordsEventsRegistrants.notifyResult(100);
                }
                if (intExtra == MtkSIMRecords.this.mSlotId) {
                    String telephonyProperty = TelephonyManager.getTelephonyProperty(MtkSIMRecords.this.mSlotId, MtkSIMRecords.SIMRECORD_PROPERTY_RIL_PHB_READY, "false");
                    MtkSIMRecords.this.mtkLog("sim state: " + stringExtra2 + ", mPhbReady: " + MtkSIMRecords.this.mPhbReady + ",strPhbReady: " + telephonyProperty);
                    if ("READY".equals(stringExtra2)) {
                        if (MtkSIMRecords.this.mPhbReady || !telephonyProperty.equals("true")) {
                            if (true == MtkSIMRecords.this.mPhbWaitSub && telephonyProperty.equals("true")) {
                                MtkSIMRecords.this.mtkLog("mPhbWaitSub is " + MtkSIMRecords.this.mPhbWaitSub + ", broadcast if need");
                                MtkSIMRecords.this.mPhbWaitSub = false;
                                MtkSIMRecords.this.broadcastPhbStateChangedIntent(MtkSIMRecords.this.mPhbReady);
                                return;
                            }
                            return;
                        }
                        MtkSIMRecords.this.mPhbReady = true;
                        MtkSIMRecords.this.broadcastPhbStateChangedIntent(MtkSIMRecords.this.mPhbReady);
                    }
                }
            }
        }
    }

    private void updateConfiguration(String str) {
        if (!TextUtils.isEmpty(str) && !this.mOldMccMnc.equals(str)) {
            this.mOldMccMnc = str;
            MccTable.updateMccMncConfiguration(this.mContext, this.mOldMccMnc, false);
        } else {
            mtkLog("Do not update configuration if mcc mnc no change.");
        }
    }

    private void parseEFpnn(ArrayList arrayList) {
        int size = arrayList.size();
        mtkLog("parseEFpnn(): pnn has " + size + " records");
        this.mPnnNetworkNames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            byte[] bArr = (byte[]) arrayList.get(i);
            mtkLog("parseEFpnn(): pnn record " + i + " content is " + IccUtils.bytesToHexString(bArr));
            SimTlv simTlv = new SimTlv(bArr, 0, bArr.length);
            OperatorName operatorName = new OperatorName();
            while (simTlv.isValidObject()) {
                if (simTlv.getTag() == 67) {
                    operatorName.sFullName = IccUtils.networkNameToString(simTlv.getData(), 0, simTlv.getData().length);
                    mtkLog("parseEFpnn(): pnn sFullName is " + operatorName.sFullName);
                } else if (simTlv.getTag() == 69) {
                    operatorName.sShortName = IccUtils.networkNameToString(simTlv.getData(), 0, simTlv.getData().length);
                    mtkLog("parseEFpnn(): pnn sShortName is " + operatorName.sShortName);
                }
                simTlv.nextObject();
            }
            this.mPnnNetworkNames.add(operatorName);
        }
    }

    private void fetchPnnAndOpl() {
        boolean z;
        log("fetchPnnAndOpl()");
        this.mReadingOpl = false;
        if (this.mEfSST != null) {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                if (this.mEfSST.length >= 6) {
                    z = (this.mEfSST[5] & PplControlData.STATUS_WIPE_REQUESTED) == 16;
                    if (z) {
                        this.mReadingOpl = (this.mEfSST[5] & 32) == 32;
                    }
                }
            } else if (this.mEfSST.length >= 13) {
                z = (this.mEfSST[12] & 48) == 48;
                if (z) {
                    this.mReadingOpl = (this.mEfSST[12] & 192) == 192;
                }
            }
        } else {
            z = false;
        }
        log("bPnnActive = " + z + ", bOplActive = " + this.mReadingOpl);
        if (z) {
            this.mFh.loadEFLinearFixedAll(28613, obtainMessage(EVENT_GET_ALL_PNN_DONE));
            if (this.mReadingOpl) {
                this.mFh.loadEFLinearFixedAll(28614, obtainMessage(1008));
            }
        }
    }

    private void fetchSpn() {
        mtkLog("fetchSpn()");
        if (getSIMServiceStatus(IccServiceInfo.IccService.SPN) == IccServiceInfo.IccServiceStatus.ACTIVATED) {
            setServiceProviderName(null);
            this.mFh.loadEFTransparent(28486, obtainMessage(12));
            this.mRecordsToLoad++;
            return;
        }
        mtkLog("[SIMRecords] SPN service is not activated  ");
    }

    public IccServiceInfo.IccServiceStatus getSIMServiceStatus(IccServiceInfo.IccService iccService) {
        int i;
        int i2;
        IccServiceInfo.IccServiceStatus iccServiceStatus;
        int index = iccService.getIndex();
        IccServiceInfo.IccServiceStatus iccServiceStatus2 = IccServiceInfo.IccServiceStatus.UNKNOWN;
        mtkLog("getSIMServiceStatus enService is " + iccService + " Service Index is " + index);
        if (index >= 0 && index < IccServiceInfo.IccService.UNSUPPORTED_SERVICE.getIndex() && this.mEfSST != null) {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                int i3 = usimServiceNumber[index];
                if (i3 <= 0) {
                    iccServiceStatus = IccServiceInfo.IccServiceStatus.NOT_EXIST_IN_USIM;
                } else {
                    int i4 = i3 / 8;
                    int i5 = i3 % 8;
                    if (i5 == 0) {
                        i2 = 7;
                        i4--;
                    } else {
                        i2 = i5 - 1;
                    }
                    mtkLog("getSIMServiceStatus USIM nbyte: " + i4 + " nbit: " + i2);
                    if (this.mEfSST.length > i4) {
                        if (((1 << i2) & this.mEfSST[i4]) > 0) {
                            iccServiceStatus = IccServiceInfo.IccServiceStatus.ACTIVATED;
                        } else {
                            iccServiceStatus = IccServiceInfo.IccServiceStatus.INACTIVATED;
                        }
                    }
                }
                iccServiceStatus2 = iccServiceStatus;
            } else {
                int i6 = simServiceNumber[index];
                if (i6 <= 0) {
                    iccServiceStatus2 = IccServiceInfo.IccServiceStatus.NOT_EXIST_IN_SIM;
                } else {
                    int i7 = i6 / 4;
                    int i8 = i6 % 4;
                    if (i8 == 0) {
                        i = 3;
                        i7--;
                    } else {
                        i = i8 - 1;
                    }
                    int i9 = 2 << (i * 2);
                    mtkLog("getSIMServiceStatus SIM nbyte: " + i7 + " nbit: " + i + " nMask: " + i9);
                    iccServiceStatus2 = (this.mEfSST.length <= i7 || (this.mEfSST[i7] & i9) != i9) ? IccServiceInfo.IccServiceStatus.INACTIVATED : IccServiceInfo.IccServiceStatus.ACTIVATED;
                }
            }
        }
        mtkLog("getSIMServiceStatus simServiceStatus: " + iccServiceStatus2);
        return iccServiceStatus2;
    }

    private void fetchSmsp() {
        mtkLog("fetchSmsp()");
        if (this.mUsimServiceTable != null && this.mParentApp.getType() != IccCardApplicationStatus.AppType.APPTYPE_SIM && this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.SM_SERVICE_PARAMS)) {
            mtkLog("SMSP support.");
            this.mFh.loadEFLinearFixed(MtkIccConstants.EF_SMSP, 1, obtainMessage(1018));
            if (this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.SM_OVER_IP)) {
                mtkLog("PSISMSP support.");
                this.mFh.loadEFLinearFixed(MtkIccConstants.EF_PSISMSC, 1, obtainMessage(1017));
            }
        }
    }

    private void fetchGbaRecords() {
        mtkLog("fetchGbaRecords");
        if (this.mUsimServiceTable != null && this.mParentApp.getType() != IccCardApplicationStatus.AppType.APPTYPE_SIM && this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.GBA)) {
            mtkLog("GBA support.");
            this.mFh.loadEFTransparent(MtkIccConstants.EF_ISIM_GBABP, obtainMessage(1019));
            this.mFh.loadEFLinearFixedAll(MtkIccConstants.EF_ISIM_GBANL, obtainMessage(1020));
        }
    }

    private void fetchMbiRecords() {
        mtkLog("fetchMbiRecords");
        if (this.mUsimServiceTable != null && this.mParentApp.getType() != IccCardApplicationStatus.AppType.APPTYPE_SIM && this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.MBDN)) {
            mtkLog("MBI/MBDN support.");
            this.mFh.loadEFLinearFixed(28617, 1, obtainMessage(5));
            this.mRecordsToLoad++;
        }
    }

    private void fetchMwisRecords() {
        mtkLog("fetchMwisRecords");
        if (this.mUsimServiceTable != null && this.mParentApp.getType() != IccCardApplicationStatus.AppType.APPTYPE_SIM && this.mUsimServiceTable.isAvailable(UsimServiceTable.UsimService.MWI_STATUS)) {
            mtkLog("MWIS support.");
            this.mFh.loadEFLinearFixed(28618, 1, obtainMessage(7));
            this.mRecordsToLoad++;
        }
    }

    private void parseEFopl(ArrayList arrayList) {
        int size = arrayList.size();
        mtkLog("parseEFopl(): opl has " + size + " records");
        this.mOperatorList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            byte[] bArr = (byte[]) arrayList.get(i);
            OplRecord oplRecord = new OplRecord();
            oplRecord.sPlmn = MtkIccUtils.parsePlmnToStringForEfOpl(bArr, 0, 3);
            oplRecord.nMinLAC = Integer.parseInt(IccUtils.bytesToHexString(new byte[]{bArr[3], bArr[4]}), 16);
            oplRecord.nMaxLAC = Integer.parseInt(IccUtils.bytesToHexString(new byte[]{bArr[5], bArr[6]}), 16);
            oplRecord.nPnnIndex = Integer.parseInt(IccUtils.bytesToHexString(new byte[]{bArr[7]}), 16);
            mtkLog("parseEFopl(): record=" + i + " content=" + IccUtils.bytesToHexString(bArr) + " sPlmn=" + oplRecord.sPlmn + " nMinLAC=" + oplRecord.nMinLAC + " nMaxLAC=" + oplRecord.nMaxLAC + " nPnnIndex=" + oplRecord.nPnnIndex);
            this.mOperatorList.add(oplRecord);
        }
    }

    private void boradcastEfRatContentNotify(int i) {
        Intent intent = new Intent("com.mediatek.phone.ACTION_EF_RAT_CONTENT_NOTIFY");
        intent.putExtra("ef_rat_status", i);
        intent.putExtra("slot", this.mSlotId);
        mtkLog("broadCast intent ACTION_EF_RAT_CONTENT_NOTIFY: item: " + i + ", simId: " + this.mSlotId);
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
    }

    private void processEfCspPlmnModeBitUrc(int i) {
        mtkLog("processEfCspPlmnModeBitUrc: bit = " + i);
        if (i == 0) {
            this.mCspPlmnEnabled = false;
        } else {
            this.mCspPlmnEnabled = true;
        }
        Intent intent = new Intent("com.mediatek.phone.ACTION_EF_CSP_CONTENT_NOTIFY");
        intent.putExtra("plmn_mode_bit", i);
        intent.putExtra("slot", this.mSlotId);
        mtkLog("broadCast intent ACTION_EF_CSP_CONTENT_NOTIFY, EXTRA_PLMN_MODE_BIT: " + i);
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
    }

    public String getMenuTitleFromEf() {
        return this.mMenuTitleFromEf;
    }

    private void fetchCPHSOns() {
        mtkLog("fetchCPHSOns()");
        this.cphsOnsl = null;
        this.cphsOnss = null;
        this.mFh.loadEFTransparent(28436, obtainMessage(1009));
        this.mFh.loadEFTransparent(28440, obtainMessage(1010));
    }

    private void fetchRatBalancing() {
        mtkLog("support MTK_RAT_BALANCING");
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            mtkLog("start loading EF_RAT");
            this.mFh.loadEFTransparent(MtkIccConstants.EF_RAT, obtainMessage(1014));
        } else {
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
                mtkLog("loading EF_RAT fail, because of SIM");
                this.mEfRatLoaded = false;
                this.mEfRat = null;
                boradcastEfRatContentNotify(512);
                return;
            }
            mtkLog("loading EF_RAT fail, because of +EUSIM");
        }
    }

    public int getEfRatBalancing() {
        StringBuilder sb = new StringBuilder();
        sb.append("getEfRatBalancing: iccCardType = ");
        sb.append(this.mParentApp.getType());
        sb.append(", mEfRatLoaded = ");
        sb.append(this.mEfRatLoaded);
        sb.append(", mEfRat is null = ");
        sb.append(this.mEfRat == null);
        mtkLog(sb.toString());
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM && this.mEfRatLoaded && this.mEfRat == null) {
            return 256;
        }
        return 512;
    }

    private boolean isMatchingPlmnForEfOpl(String str, String str2) {
        if (str == null || str.equals("") || str2 == null || str2.equals("")) {
            return false;
        }
        mtkLog("isMatchingPlmnForEfOpl(): simPlmn = " + str + ", bcchPlmn = " + str2);
        int length = str.length();
        int length2 = str2.length();
        if (length < 5 || length2 < 5) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            if (str.charAt(i) != 'd' && str.charAt(i) != str2.charAt(i)) {
                return false;
            }
        }
        if (length == 6 && length2 == 6) {
            if (str.charAt(5) != 'd' && str.charAt(5) != str2.charAt(5)) {
                return false;
            }
            return true;
        }
        if (length2 == 6 && str2.charAt(5) != '0' && str2.charAt(5) != 'd') {
            return false;
        }
        if (length == 6 && str.charAt(5) != '0' && str.charAt(5) != 'd') {
            return false;
        }
        return true;
    }

    private boolean isPlmnEqualsSimNumeric(String str) {
        String operatorNumeric = getOperatorNumeric();
        if (str == null) {
            return false;
        }
        if (operatorNumeric == null || operatorNumeric.equals("")) {
            mtkLog("isPlmnEqualsSimNumeric: getOperatorNumeric error: " + operatorNumeric);
            return false;
        }
        if (str.equals(operatorNumeric)) {
            return true;
        }
        return str.length() == 5 && operatorNumeric.length() == 6 && str.equals(operatorNumeric.substring(0, 5));
    }

    public String getEonsIfExist(String str, int i, boolean z) {
        int i2;
        mtkLog("EONS getEonsIfExist: plmn is " + str + " nLac is " + i + " bLongNameRequired: " + z);
        String str2 = null;
        if (str == null || this.mPnnNetworkNames == null || this.mPnnNetworkNames.size() == 0) {
            return null;
        }
        boolean zIsPlmnEqualsSimNumeric = isPlmnEqualsSimNumeric(str);
        if (this.mOperatorList == null) {
            if (zIsPlmnEqualsSimNumeric) {
                mtkLog("getEonsIfExist: Plmn is HPLMN, return PNN's first record");
                i2 = 1;
            } else {
                mtkLog("getEonsIfExist: Plmn is not HPLMN and no mOperatorList, return null");
                return null;
            }
        } else {
            for (int i3 = 0; i3 < this.mOperatorList.size(); i3++) {
                OplRecord oplRecord = this.mOperatorList.get(i3);
                if (isMatchingPlmnForEfOpl(oplRecord.sPlmn, str) && ((oplRecord.nMinLAC == 0 && oplRecord.nMaxLAC == 65534) || (oplRecord.nMinLAC <= i && oplRecord.nMaxLAC >= i))) {
                    mtkLog("getEonsIfExist: find it in EF_OPL");
                    if (oplRecord.nPnnIndex == 0) {
                        mtkLog("getEonsIfExist: oplRec.nPnnIndex is 0, from other sources");
                        return null;
                    }
                    i2 = oplRecord.nPnnIndex;
                }
            }
            i2 = -1;
        }
        if (i2 == -1 && zIsPlmnEqualsSimNumeric && this.mOperatorList.size() == 1) {
            mtkLog("getEonsIfExist: not find it in EF_OPL, but Plmn is HPLMN, return PNN's first record");
        } else if (i2 > 1 && i2 > this.mPnnNetworkNames.size() && zIsPlmnEqualsSimNumeric) {
            mtkLog("getEonsIfExist: find it in EF_OPL, but index in EF_OPL > EF_PNN list length & Plmn is HPLMN, return PNN's first record");
        } else {
            if (i2 > 1 && i2 > this.mPnnNetworkNames.size() && !zIsPlmnEqualsSimNumeric) {
                mtkLog("getEonsIfExist: find it in EF_OPL, but index in EF_OPL > EF_PNN list length & Plmn is not HPLMN, return PNN's first record");
                i2 = -1;
            }
            if (i2 >= 1) {
                OperatorName operatorName = this.mPnnNetworkNames.get(i2 - 1);
                if (z) {
                    if (operatorName.sFullName != null) {
                        str2 = new String(operatorName.sFullName);
                    } else if (operatorName.sShortName != null) {
                        str2 = new String(operatorName.sShortName);
                    }
                } else if (!z) {
                    if (operatorName.sShortName != null) {
                        str2 = new String(operatorName.sShortName);
                    } else if (operatorName.sFullName != null) {
                        str2 = new String(operatorName.sFullName);
                    }
                }
            }
            mtkLog("getEonsIfExist: sEons is " + str2);
            return str2;
        }
        i2 = 1;
        if (i2 >= 1) {
        }
        mtkLog("getEonsIfExist: sEons is " + str2);
        return str2;
    }

    public String getEfGbabp() {
        mtkLog("GBABP = " + this.mGbabp);
        return this.mGbabp;
    }

    public void setEfGbabp(String str, Message message) {
        this.mFh.updateEFTransparent(MtkIccConstants.EF_GBABP, IccUtils.hexStringToBytes(str), message);
    }

    public byte[] getEfPsismsc() {
        return this.mEfPsismsc;
    }

    public byte[] getEfSmsp() {
        return this.mEfSmsp;
    }

    public int getMncLength() {
        mtkLog("mncLength = " + this.mMncLength);
        return this.mMncLength;
    }

    private class RebootClickListener implements DialogInterface.OnClickListener {
        private RebootClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            MtkSIMRecords.this.mtkLog("Unlock Phone onClick");
            ((PowerManager) MtkSIMRecords.this.mContext.getSystemService("power")).reboot("Unlock state changed");
        }
    }

    protected void parseEfSpdi(byte[] bArr) {
        byte[] data;
        SimTlv simTlv = new SimTlv(bArr, 0, bArr.length);
        while (true) {
            if (simTlv.isValidObject()) {
                if (simTlv.getTag() == 163) {
                    simTlv = new SimTlv(simTlv.getData(), 0, simTlv.getData().length);
                }
                if (simTlv.getTag() != 128) {
                    simTlv.nextObject();
                } else {
                    data = simTlv.getData();
                    break;
                }
            } else {
                data = null;
                break;
            }
        }
        if (data == null) {
            return;
        }
        this.mSpdiNetworks = new ArrayList(data.length / 3);
        for (int i = 0; i + 2 < data.length; i += 3) {
            String plmnToString = MtkIccUtils.parsePlmnToString(data, i, 3);
            if (plmnToString.length() >= 5) {
                mtkLog("EF_SPDI network: " + plmnToString);
                this.mSpdiNetworks.add(plmnToString);
            }
        }
    }

    public boolean isRadioAvailable() {
        if (this.mCi != null) {
            return this.mCi.getRadioState().isAvailable();
        }
        return false;
    }

    protected void updateIccFdnStatus() {
    }

    public void broadcastPhbStateChangedIntent(boolean z) {
        if (this.mPhone != null && this.mPhone.getPhoneType() != 1 && (!this.isDispose || z)) {
            mtkLog("broadcastPhbStateChangedIntent, Not active Phone.");
            return;
        }
        mtkLog("broadcastPhbStateChangedIntent, mPhbReady " + this.mPhbReady);
        if (z) {
            this.mSubId = MtkSubscriptionManager.getSubIdUsingPhoneId(this.mSlotId);
            int simStateForSlotIndex = SubscriptionController.getInstance().getSimStateForSlotIndex(this.mSlotId);
            if (this.mSubId <= 0 || simStateForSlotIndex == 0) {
                mtkLog("broadcastPhbStateChangedIntent, mSubId " + this.mSubId + ", sim state " + simStateForSlotIndex);
                this.mPhbWaitSub = true;
                return;
            }
        } else if (this.mSubId <= 0) {
            mtkLog("broadcastPhbStateChangedIntent, isReady == false and mSubId <= 0");
            return;
        }
        if (!SystemProperties.get("sys.boot_completed").equals("1")) {
            mtkLog("broadcastPhbStateChangedIntent, boot not completed");
            this.mPendingPhbNotify = true;
            return;
        }
        Intent intent = new Intent("mediatek.intent.action.PHB_STATE_CHANGED");
        intent.putExtra("ready", z);
        intent.putExtra("subscription", this.mSubId);
        mtkLog("Broadcasting intent ACTION_PHB_STATE_CHANGED " + z + " sub id " + this.mSubId + " phoneId " + this.mParentApp.getPhoneId());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        Intent intent2 = new Intent("mediatek.intent.action.PHB_STATE_CHANGED");
        intent2.putExtra("ready", z);
        intent2.putExtra("subscription", this.mSubId);
        intent2.setPackage("com.mediatek.simprocessor");
        mtkLog("Broadcasting ACTION_PHB_STATE_CHANGED to package: simprocessor");
        this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL);
        if (!z) {
            this.mSubId = -1;
        }
    }

    public boolean isPhbReady() {
        StringBuilder sb = new StringBuilder();
        sb.append("isPhbReady(): cached mPhbReady = ");
        sb.append(this.mPhbReady ? "true" : "false");
        mtkLog(sb.toString());
        String str = "";
        if (this.mParentApp == null || this.mPhone == null) {
            return false;
        }
        String telephonyProperty = TelephonyManager.getTelephonyProperty(this.mSlotId, SIMRECORD_PROPERTY_RIL_PHB_READY, "false");
        String str2 = SystemProperties.get("gsm.sim.state");
        if (str2 != null && str2.length() > 0) {
            String[] strArrSplit = str2.split(",");
            if (this.mSlotId >= 0 && this.mSlotId < strArrSplit.length && strArrSplit[this.mSlotId] != null) {
                str = strArrSplit[this.mSlotId];
            }
        }
        boolean z = str.equals("NETWORK_LOCKED") || str.equals("PIN_REQUIRED");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("isPhbReady(): mPhbReady = ");
        sb2.append(this.mPhbReady ? "true" : "false");
        sb2.append(", strAllSimState = ");
        sb2.append(str2);
        mtkLog(sb2.toString());
        return telephonyProperty.equals("true") && !z;
    }

    public void setPhbReady(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setPhbReady(): isReady = ");
        sb.append(z ? "true" : "false");
        mtkLog(sb.toString());
        if (this.mPhbReady != z) {
            this.mPhbReady = z;
            if (z) {
                this.mCi.setPhonebookReady(1, null);
            } else if (!z) {
                this.mCi.setPhonebookReady(0, null);
            }
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
    }

    private class PhbBroadCastReceiver extends BroadcastReceiver {
        private PhbBroadCastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MtkSIMRecords.this.mPhbWaitSub && action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                MtkSIMRecords.this.mtkLog("SubBroadCastReceiver receive ACTION_SUBINFO_RECORD_UPDATED");
                MtkSIMRecords.this.mPhbWaitSub = false;
                MtkSIMRecords.this.broadcastPhbStateChangedIntent(MtkSIMRecords.this.mPhbReady);
                return;
            }
            if (!action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                    MtkSIMRecords.this.mtkLog("[onReceive] ACTION_BOOT_COMPLETED mPendingPhbNotify : " + MtkSIMRecords.this.mPendingPhbNotify);
                    if (MtkSIMRecords.this.mPendingPhbNotify) {
                        MtkSIMRecords.this.broadcastPhbStateChangedIntent(MtkSIMRecords.this.isPhbReady());
                        MtkSIMRecords.this.mPendingPhbNotify = false;
                        return;
                    }
                    return;
                }
                return;
            }
            int intExtra = intent.getIntExtra("phone", -1);
            MtkSIMRecords.this.mtkLog("[ACTION_RADIO_TECHNOLOGY_CHANGED] phoneid : " + intExtra);
            if (MtkSIMRecords.this.mParentApp != null && MtkSIMRecords.this.mParentApp.getPhoneId() == intExtra) {
                String stringExtra = intent.getStringExtra("phoneName");
                int intExtra2 = intent.getIntExtra("subscription", -1);
                MtkSIMRecords.this.mtkLog("[ACTION_RADIO_TECHNOLOGY_CHANGED] activePhoneName : " + stringExtra + " | subid : " + intExtra2);
                if (!"CDMA".equals(stringExtra)) {
                    MtkSIMRecords.this.broadcastPhbStateChangedIntent(false);
                    MtkSIMRecords.this.sendMessageDelayed(MtkSIMRecords.this.obtainMessage(MtkSIMRecords.EVENT_DELAYED_SEND_PHB_CHANGE), MtkRuimRecords.PHB_DELAY_SEND_TIME);
                    MtkSIMRecords.this.mAdnCache.reset();
                }
            }
        }
    }

    protected void updatePHBStatus(int i, boolean z) {
        mtkLog("[updatePHBStatus] status : " + i + " | isSimLocked : " + z + " | mPhbReady : " + this.mPhbReady);
        if (i == 1) {
            if (!z) {
                if (!this.mPhbReady) {
                    this.mPhbReady = true;
                    broadcastPhbStateChangedIntent(this.mPhbReady);
                    return;
                }
                return;
            }
            mtkLog("phb ready but sim is not ready.");
            return;
        }
        if (i == 0 && this.mPhbReady) {
            this.mAdnCache.reset();
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
        }
    }

    protected void setVoiceCallForwardingFlagFromSimRecords() {
        if (checkEfCfis()) {
            this.mCallForwardingStatus = this.mEfCfis[1] & 1;
            log("EF_CFIS2: callForwardingEnabled=" + this.mCallForwardingStatus);
            this.mRecordsEventsRegistrants.notifyResult(1);
            return;
        }
        if (this.mEfCff != null) {
            this.mCallForwardingStatus = (this.mEfCff[0] & 15) == 10 ? 1 : 0;
            log("EF_CFF2: callForwardingEnabled=" + this.mCallForwardingStatus);
            this.mRecordsEventsRegistrants.notifyResult(1);
            return;
        }
        log("EF_CFIS and EF_CFF not valid. callForwardingEnabled=" + this.mCallForwardingStatus);
    }

    private void setCFUStatusToLocal(boolean z) {
        log("setCFUStatusToLocal: " + z);
        if (z) {
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "enabled_cfu_mode_on");
        } else {
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "enabled_cfu_mode_off");
        }
    }

    private String convertNumberIfContainsPrefix(String str) {
        if (str == null) {
            return str;
        }
        if (!str.startsWith("tel:") && !str.startsWith("sip:") && !str.startsWith("sips:")) {
            return str;
        }
        String strSubstring = str.substring(str.indexOf(":") + 1);
        Rlog.d("SIMRecords", "convertNumberIfContainsPrefix: dialNumber = " + str);
        return strSubstring;
    }

    protected void mtkLog(String str) {
        Rlog.d(LOG_TAG_EX, "[SIMRecords] " + str + " (slot " + this.mSlotId + ")");
    }

    protected void mtkLoge(String str) {
        Rlog.e(LOG_TAG_EX, "[SIMRecords] " + str + " (slot " + this.mSlotId + ")");
    }

    protected void log(String str) {
        Rlog.d("SIMRecords", "[SIMRecords] " + str + " (slot " + this.mSlotId + ")");
    }

    protected void loge(String str) {
        Rlog.e("SIMRecords", "[SIMRecords] " + str + " (slot " + this.mSlotId + ")");
    }

    protected void logw(String str, Throwable th) {
        Rlog.w("SIMRecords", "[SIMRecords] " + str + " (slot " + this.mSlotId + ")", th);
    }

    protected void logv(String str) {
        Rlog.v("SIMRecords", "[SIMRecords] " + str + " (slot " + this.mSlotId + ")");
    }

    public void onReady() {
        this.mLockedRecordsReqReason = 0;
        super.onReady();
    }

    protected void onLocked(int i) {
        this.mRecordsRequested = false;
        this.mLoaded.set(false);
        if (this.mLockedRecordsReqReason != 0) {
            this.mLockedRecordsReqReason = i == 258 ? 1 : 2;
            this.mRecordsToLoad++;
            onRecordLoaded();
            return;
        }
        super.onLocked(i);
    }
}
