package com.android.internal.telephony.uicc;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.test.SimulatedCommands;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class SIMRecords extends IccRecords {
    protected static final int CFF_LINE1_MASK = 15;
    protected static final int CFF_LINE1_RESET = 240;
    protected static final int CFF_UNCONDITIONAL_ACTIVE = 10;
    protected static final int CFF_UNCONDITIONAL_DEACTIVE = 5;
    protected static final int CFIS_ADN_CAPABILITY_ID_OFFSET = 14;
    protected static final int CFIS_ADN_EXTENSION_ID_OFFSET = 15;
    protected static final int CFIS_BCD_NUMBER_LENGTH_OFFSET = 2;
    protected static final int CFIS_TON_NPI_OFFSET = 3;
    private static final int CPHS_SST_MBN_ENABLED = 48;
    private static final int CPHS_SST_MBN_MASK = 48;
    private static final boolean CRASH_RIL = false;
    protected static final int EVENT_APP_LOCKED = 258;
    private static final int EVENT_APP_NETWORK_LOCKED = 259;
    protected static final int EVENT_GET_AD_DONE = 9;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    protected static final int EVENT_GET_CFF_DONE = 24;
    protected static final int EVENT_GET_CFIS_DONE = 32;
    protected static final int EVENT_GET_CPHS_MAILBOX_DONE = 11;
    protected static final int EVENT_GET_CSP_CPHS_DONE = 33;
    private static final int EVENT_GET_EHPLMN_DONE = 40;
    private static final int EVENT_GET_FPLMN_DONE = 41;
    private static final int EVENT_GET_GID1_DONE = 34;
    private static final int EVENT_GET_GID2_DONE = 36;
    private static final int EVENT_GET_HPLMN_W_ACT_DONE = 39;
    private static final int EVENT_GET_ICCID_DONE = 4;
    protected static final int EVENT_GET_IMSI_DONE = 3;
    protected static final int EVENT_GET_INFO_CPHS_DONE = 26;
    protected static final int EVENT_GET_MBDN_DONE = 6;
    protected static final int EVENT_GET_MBI_DONE = 5;
    protected static final int EVENT_GET_MSISDN_DONE = 10;
    protected static final int EVENT_GET_MWIS_DONE = 7;
    private static final int EVENT_GET_OPLMN_W_ACT_DONE = 38;
    private static final int EVENT_GET_PLMN_W_ACT_DONE = 37;
    private static final int EVENT_GET_PNN_DONE = 15;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_SPDI_DONE = 13;
    protected static final int EVENT_GET_SPN_DONE = 12;
    protected static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE = 8;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_SET_CPHS_MAILBOX_DONE = 25;
    private static final int EVENT_SET_MBDN_DONE = 20;
    protected static final int EVENT_SET_MSISDN_DONE = 30;
    private static final int EVENT_SMS_ON_SIM = 21;
    protected static final int EVENT_UPDATE_DONE = 14;
    protected static final String LOG_TAG = "SIMRecords";
    protected static final String[] MCCMNC_CODES_HAVING_3DIGITS_MNC = {"302370", "302720", SimulatedCommands.FAKE_MCC_MNC, "405025", "405026", "405027", "405028", "405029", "405030", "405031", "405032", "405033", "405034", "405035", "405036", "405037", "405038", "405039", "405040", "405041", "405042", "405043", "405044", "405045", "405046", "405047", "405750", "405751", "405752", "405753", "405754", "405755", "405756", "405799", "405800", "405801", "405802", "405803", "405804", "405805", "405806", "405807", "405808", "405809", "405810", "405811", "405812", "405813", "405814", "405815", "405816", "405817", "405818", "405819", "405820", "405821", "405822", "405823", "405824", "405825", "405826", "405827", "405828", "405829", "405830", "405831", "405832", "405833", "405834", "405835", "405836", "405837", "405838", "405839", "405840", "405841", "405842", "405843", "405844", "405845", "405846", "405847", "405848", "405849", "405850", "405851", "405852", "405853", "405854", "405855", "405856", "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864", "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872", "405873", "405874", "405875", "405876", "405877", "405878", "405879", "405880", "405881", "405882", "405883", "405884", "405885", "405886", "405908", "405909", "405910", "405911", "405912", "405913", "405914", "405915", "405916", "405917", "405918", "405919", "405920", "405921", "405922", "405923", "405924", "405925", "405926", "405927", "405928", "405929", "405930", "405931", "405932", "502142", "502143", "502145", "502146", "502147", "502148"};
    private static final int SIM_RECORD_EVENT_BASE = 0;
    private static final int SYSTEM_EVENT_BASE = 256;
    protected static final int TAG_FULL_NETWORK_NAME = 67;
    protected static final int TAG_SHORT_NETWORK_NAME = 69;
    protected static final int TAG_SPDI = 163;
    protected static final int TAG_SPDI_PLMN_LIST = 128;
    private static final boolean VDBG = false;
    protected int mCallForwardingStatus;
    protected byte[] mCphsInfo;
    protected boolean mCspPlmnEnabled;
    byte[] mEfCPHS_MWI;
    protected byte[] mEfCff;
    protected byte[] mEfCfis;
    byte[] mEfLi;
    byte[] mEfMWIS;
    byte[] mEfPl;
    protected ArrayList<String> mSpdiNetworks;
    protected int mSpnDisplayCondition;
    protected GetSpnFsmState mSpnState;
    protected UsimServiceTable mUsimServiceTable;
    protected VoiceMailConstants mVmConfig;

    protected enum GetSpnFsmState {
        IDLE,
        INIT,
        READ_SPN_3GPP,
        READ_SPN_CPHS,
        READ_SPN_SHORT_CPHS
    }

    @Override
    public String toString() {
        return "SimRecords: " + super.toString() + " mVmConfig" + this.mVmConfig + " callForwardingEnabled=" + this.mCallForwardingStatus + " spnState=" + this.mSpnState + " mCphsInfo=" + this.mCphsInfo + " mCspPlmnEnabled=" + this.mCspPlmnEnabled + " efMWIS=" + this.mEfMWIS + " efCPHS_MWI=" + this.mEfCPHS_MWI + " mEfCff=" + this.mEfCff + " mEfCfis=" + this.mEfCfis + " getOperatorNumeric=" + getOperatorNumeric();
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
        this.mAdnCache = new AdnRecordCache(this.mFh);
        this.mVmConfig = new VoiceMailConstants();
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mRecordsToLoad = 0;
        this.mCi.setOnSmsOnSim(this, 21, null);
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        this.mParentApp.registerForLocked(this, 258, null);
        this.mParentApp.registerForNetworkLocked(this, 259, null);
        log("SIMRecords X ctor this=" + this);
    }

    @Override
    public void dispose() {
        log("Disposing SIMRecords this=" + this);
        this.mCi.unSetOnSmsOnSim(this);
        this.mParentApp.unregisterForReady(this);
        this.mParentApp.unregisterForLocked(this);
        this.mParentApp.unregisterForNetworkLocked(this);
        resetRecords();
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
        this.mPlmnActRecords = null;
        this.mOplmnActRecords = null;
        this.mHplmnActRecords = null;
        this.mFplmns = null;
        this.mEhplmns = null;
        this.mAdnCache.reset();
        log("SIMRecords: onRadioOffOrNotAvailable set 'gsm.sim.operator.numeric' to operator=null");
        log("update icc_operator_numeric=" + ((Object) null));
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), "");
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), "");
        this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), "");
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mLoaded.set(false);
    }

    @Override
    public String getMsisdnNumber() {
        return this.mMsisdn;
    }

    @Override
    public UsimServiceTable getUsimServiceTable() {
        return this.mUsimServiceTable;
    }

    protected int getExtFromEf(int i) {
        if (i != 28480 || this.mParentApp.getType() != IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            return IccConstants.EF_EXT1;
        }
        return IccConstants.EF_EXT5;
    }

    @Override
    public void setMsisdnNumber(String str, String str2, Message message) {
        this.mNewMsisdn = str2;
        this.mNewMsisdnTag = str;
        log("Set MSISDN: " + this.mNewMsisdnTag + " " + Rlog.pii(LOG_TAG, this.mNewMsisdn));
        new AdnRecordLoader(this.mFh).updateEF(new AdnRecord(this.mNewMsisdnTag, this.mNewMsisdn), IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, null, obtainMessage(30, message));
    }

    @Override
    public String getMsisdnAlphaTag() {
        return this.mMsisdnTag;
    }

    @Override
    public String getVoiceMailNumber() {
        return this.mVoiceMailNum;
    }

    @Override
    public void setVoiceMailNumber(String str, String str2, Message message) {
        if (this.mIsVoiceMailFixed) {
            AsyncResult.forMessage(message).exception = new IccVmFixedException("Voicemail number is fixed by operator");
            message.sendToTarget();
            return;
        }
        this.mNewVoiceMailNum = str2;
        this.mNewVoiceMailTag = str;
        AdnRecord adnRecord = new AdnRecord(this.mNewVoiceMailTag, this.mNewVoiceMailNum);
        if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
            new AdnRecordLoader(this.mFh).updateEF(adnRecord, IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, null, obtainMessage(20, message));
        } else {
            if (isCphsMailboxEnabled()) {
                new AdnRecordLoader(this.mFh).updateEF(adnRecord, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, message));
                return;
            }
            AsyncResult.forMessage(message).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
            message.sendToTarget();
        }
    }

    @Override
    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    @Override
    public void setVoiceMessageWaiting(int i, int i2) {
        if (i != 1) {
            return;
        }
        try {
            if (this.mEfMWIS != null) {
                this.mEfMWIS[0] = (byte) ((this.mEfMWIS[0] & 254) | (i2 == 0 ? 0 : 1));
                if (i2 < 0) {
                    this.mEfMWIS[1] = 0;
                } else {
                    this.mEfMWIS[1] = (byte) i2;
                }
                this.mFh.updateEFLinearFixed(IccConstants.EF_MWIS, 1, this.mEfMWIS, null, obtainMessage(14, IccConstants.EF_MWIS, 0));
            }
            if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
                log("[setVoiceMessageWaiting] It is USIM card, skip write CPHS file");
            } else if (this.mEfCPHS_MWI != null) {
                this.mEfCPHS_MWI[0] = (byte) ((i2 == 0 ? 5 : 10) | (this.mEfCPHS_MWI[0] & 240));
                this.mFh.updateEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, this.mEfCPHS_MWI, obtainMessage(14, Integer.valueOf(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS)));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logw("Error saving voice mail state to SIM. Probably malformed SIM record", e);
        }
    }

    protected boolean validEfCfis(byte[] bArr) {
        if (bArr != null) {
            if (bArr[0] < 1 || bArr[0] > 4) {
                logw("MSP byte: " + ((int) bArr[0]) + " is not between 1 and 4", null);
            }
            for (byte b : bArr) {
                if (b != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getVoiceMessageCount() {
        int i = -2;
        if (this.mEfMWIS != null) {
            boolean z = (this.mEfMWIS[0] & 1) != 0;
            int i2 = this.mEfMWIS[1] & 255;
            i = (z && (i2 == 0 || i2 == 255)) ? -1 : i2;
            log(" VoiceMessageCount from SIM MWIS = " + i);
        } else if (this.mEfCPHS_MWI != null) {
            int i3 = this.mEfCPHS_MWI[0] & 15;
            if (i3 != 10) {
                if (i3 == 5) {
                    i = 0;
                }
            } else {
                i = -1;
            }
            log(" VoiceMessageCount from SIM CPHS = " + i);
        }
        return i;
    }

    @Override
    public int getVoiceCallForwardingFlag() {
        return this.mCallForwardingStatus;
    }

    @Override
    public void setVoiceCallForwardingFlag(int i, boolean z, String str) {
        int i2;
        if (i != 1) {
            return;
        }
        if (!z) {
            i2 = 0;
        } else {
            i2 = 1;
        }
        this.mCallForwardingStatus = i2;
        this.mRecordsEventsRegistrants.notifyResult(1);
        try {
            if (validEfCfis(this.mEfCfis)) {
                if (z) {
                    byte[] bArr = this.mEfCfis;
                    bArr[1] = (byte) (bArr[1] | 1);
                } else {
                    byte[] bArr2 = this.mEfCfis;
                    bArr2[1] = (byte) (bArr2[1] & 254);
                }
                log("setVoiceCallForwardingFlag: enable=" + z + " mEfCfis=" + IccUtils.bytesToHexString(this.mEfCfis));
                if (z && !TextUtils.isEmpty(str)) {
                    logv("EF_CFIS: updating cf number, " + Rlog.pii(LOG_TAG, str));
                    byte[] bArrNumberToCalledPartyBCD = PhoneNumberUtils.numberToCalledPartyBCD(str, 1);
                    System.arraycopy(bArrNumberToCalledPartyBCD, 0, this.mEfCfis, 3, bArrNumberToCalledPartyBCD.length);
                    this.mEfCfis[2] = (byte) bArrNumberToCalledPartyBCD.length;
                    this.mEfCfis[14] = -1;
                    this.mEfCfis[15] = -1;
                }
                this.mFh.updateEFLinearFixed(IccConstants.EF_CFIS, 1, this.mEfCfis, null, obtainMessage(14, Integer.valueOf(IccConstants.EF_CFIS)));
            } else {
                log("setVoiceCallForwardingFlag: ignoring enable=" + z + " invalid mEfCfis=" + IccUtils.bytesToHexString(this.mEfCfis));
            }
            if (this.mEfCff != null) {
                if (z) {
                    this.mEfCff[0] = (byte) ((this.mEfCff[0] & 240) | 10);
                } else {
                    this.mEfCff[0] = (byte) ((this.mEfCff[0] & 240) | 5);
                }
                this.mFh.updateEFTransparent(IccConstants.EF_CFF_CPHS, this.mEfCff, obtainMessage(14, Integer.valueOf(IccConstants.EF_CFF_CPHS)));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logw("Error saving call forwarding flag to SIM. Probably malformed SIM record", e);
        }
    }

    @Override
    public void onRefresh(boolean z, int[] iArr) {
        if (z) {
            fetchSimRecords();
        }
    }

    @Override
    public String getOperatorNumeric() {
        String imsi = getIMSI();
        if (imsi == null) {
            log("getOperatorNumeric: IMSI == null");
            return null;
        }
        if (this.mMncLength == -1 || this.mMncLength == 0) {
            log("getSIMOperatorNumeric: bad mncLength");
            return null;
        }
        if (imsi.length() < this.mMncLength + 3) {
            return null;
        }
        return imsi.substring(0, 3 + this.mMncLength);
    }

    @Override
    public void handleMessage(Message message) throws Throwable {
        int i;
        String imsi;
        int i2;
        int length;
        int i3;
        int i4;
        int i5;
        int i6;
        Context context;
        String strSubstring;
        if (this.mDestroyed.get()) {
            loge("Received message " + message + "[" + message.what + "]  while being destroyed. Ignoring.");
            return;
        }
        boolean z = true;
        boolean z2 = false;
        z2 = false;
        z2 = false;
        try {
            try {
                i = message.what;
            } catch (Throwable th) {
                th = th;
                if (z) {
                    onRecordLoaded();
                }
                throw th;
            }
        } catch (RuntimeException e) {
            e = e;
            z = false;
        } catch (Throwable th2) {
            th = th2;
            z = false;
            if (z) {
            }
            throw th;
        }
        if (i == 1) {
            onReady();
        } else if (i != 30) {
            try {
            } catch (RuntimeException e2) {
                e = e2;
                logw("Exception parsing SIM record", e);
                if (!z) {
                    return;
                }
            }
            switch (i) {
                case 3:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        loge("Exception querying IMSI, Exception:" + asyncResult.exception);
                    } else {
                        this.mImsi = (String) asyncResult.result;
                        if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                            loge("invalid IMSI " + this.mImsi);
                            this.mImsi = null;
                        }
                        log("IMSI: mMncLength=" + this.mMncLength);
                        if (this.mImsi != null && this.mImsi.length() >= 6) {
                            log("IMSI: " + this.mImsi.substring(0, 6) + Rlog.pii(LOG_TAG, this.mImsi.substring(6)));
                        }
                        String imsi2 = getIMSI();
                        if ((this.mMncLength == 0 || this.mMncLength == 2) && imsi2 != null && imsi2.length() >= 6) {
                            String strSubstring2 = imsi2.substring(0, 6);
                            String[] strArr = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                            int length2 = strArr.length;
                            int i7 = 0;
                            while (true) {
                                if (i7 < length2) {
                                    if (strArr[i7].equals(strSubstring2)) {
                                        this.mMncLength = 3;
                                        log("IMSI: setting1 mMncLength=" + this.mMncLength);
                                    } else {
                                        i7++;
                                    }
                                }
                            }
                        }
                        if (this.mMncLength == 0) {
                            try {
                                this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi2.substring(0, 3)));
                                log("setting2 mMncLength=" + this.mMncLength);
                            } catch (NumberFormatException e3) {
                                this.mMncLength = 0;
                                loge("Corrupt IMSI! setting3 mMncLength=" + this.mMncLength);
                            }
                        }
                        if (this.mMncLength != 0 && this.mMncLength != -1 && imsi2.length() >= this.mMncLength + 3) {
                            log("update mccmnc=" + imsi2.substring(0, this.mMncLength + 3));
                            MccTable.updateMccMncConfiguration(this.mContext, imsi2.substring(0, 3 + this.mMncLength), false);
                        }
                        this.mImsiReadyRegistrants.notifyRegistrants();
                    }
                    if (!z) {
                        return;
                    }
                    onRecordLoaded();
                    break;
                case 4:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    byte[] bArr = (byte[]) asyncResult2.result;
                    if (asyncResult2.exception == null) {
                        this.mIccId = IccUtils.bcdToString(bArr, 0, bArr.length);
                        this.mFullIccId = IccUtils.bchToString(bArr, 0, bArr.length);
                        log("iccid: " + SubscriptionInfo.givePrintableIccid(this.mFullIccId));
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 5:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    byte[] bArr2 = (byte[]) asyncResult3.result;
                    if (asyncResult3.exception == null) {
                        log("EF_MBI: " + IccUtils.bytesToHexString(bArr2));
                        this.mMailboxIndex = bArr2[0] & 255;
                        if (this.mMailboxIndex != 0 && this.mMailboxIndex != 255) {
                            log("Got valid mailbox number for MBDN");
                            z2 = true;
                        }
                    }
                    this.mRecordsToLoad++;
                    if (z2) {
                        new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                    } else {
                        new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 6:
                case 11:
                    this.mVoiceMailNum = null;
                    this.mVoiceMailTag = null;
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    if (asyncResult4.exception != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Invalid or missing EF");
                        sb.append(message.what == 11 ? "[MAILBOX]" : "[MBDN]");
                        log(sb.toString());
                        if (message.what == 6) {
                            this.mRecordsToLoad++;
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                        }
                    } else {
                        AdnRecord adnRecord = (AdnRecord) asyncResult4.result;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("VM: ");
                        sb2.append(adnRecord);
                        sb2.append(message.what == 11 ? " EF[MAILBOX]" : " EF[MBDN]");
                        log(sb2.toString());
                        if (adnRecord.isEmpty() && message.what == 6) {
                            this.mRecordsToLoad++;
                            new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                        } else {
                            this.mVoiceMailNum = adnRecord.getNumber();
                            this.mVoiceMailTag = adnRecord.getAlphaTag();
                        }
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 7:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    byte[] bArr3 = (byte[]) asyncResult5.result;
                    log("EF_MWIS : " + IccUtils.bytesToHexString(bArr3));
                    if (asyncResult5.exception != null) {
                        log("EVENT_GET_MWIS_DONE exception = " + asyncResult5.exception);
                    } else if ((bArr3[0] & 255) == 255) {
                        log("SIMRecords: Uninitialized record MWIS");
                    } else {
                        this.mEfMWIS = bArr3;
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 8:
                    AsyncResult asyncResult6 = (AsyncResult) message.obj;
                    byte[] bArr4 = (byte[]) asyncResult6.result;
                    log("EF_CPHS_MWI: " + IccUtils.bytesToHexString(bArr4));
                    if (asyncResult6.exception != null) {
                        log("EVENT_GET_VOICE_MAIL_INDICATOR_CPHS_DONE exception = " + asyncResult6.exception);
                    } else {
                        this.mEfCPHS_MWI = bArr4;
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 9:
                    try {
                        if (!this.mCarrierTestOverride.isInTestMode() || getIMSI() == null) {
                            AsyncResult asyncResult7 = (AsyncResult) message.obj;
                            byte[] bArr5 = (byte[]) asyncResult7.result;
                            if (asyncResult7.exception != null) {
                                String imsi3 = getIMSI();
                                if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi3 != null && imsi3.length() >= 6) {
                                    String strSubstring3 = imsi3.substring(0, 6);
                                    log("mccmncCode=" + strSubstring3);
                                    String[] strArr2 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                    int length3 = strArr2.length;
                                    int i8 = 0;
                                    while (true) {
                                        if (i8 < length3) {
                                            if (strArr2[i8].equals(strSubstring3)) {
                                                this.mMncLength = 3;
                                                log("setting6 mMncLength=" + this.mMncLength);
                                            } else {
                                                i8++;
                                            }
                                        }
                                    }
                                }
                                if (this.mMncLength == 0 || this.mMncLength == -1) {
                                    if (imsi3 != null) {
                                        try {
                                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi3.substring(0, 3)));
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
                                if (imsi3 != null && this.mMncLength != 0 && imsi3.length() >= this.mMncLength + 3) {
                                    log("update mccmnc=" + imsi3.substring(0, this.mMncLength + 3));
                                    context = this.mContext;
                                    strSubstring = imsi3.substring(0, 3 + this.mMncLength);
                                    MccTable.updateMccMncConfiguration(context, strSubstring, false);
                                }
                                if (!z) {
                                }
                            } else {
                                log("EF_AD: " + IccUtils.bytesToHexString(bArr5));
                                if (bArr5.length < 3) {
                                    log("Corrupt AD data on SIM");
                                    String imsi4 = getIMSI();
                                    if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi4 != null && imsi4.length() >= 6) {
                                        String strSubstring4 = imsi4.substring(0, 6);
                                        log("mccmncCode=" + strSubstring4);
                                        String[] strArr3 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                        int length4 = strArr3.length;
                                        int i9 = 0;
                                        while (true) {
                                            if (i9 < length4) {
                                                if (strArr3[i9].equals(strSubstring4)) {
                                                    this.mMncLength = 3;
                                                    log("setting6 mMncLength=" + this.mMncLength);
                                                } else {
                                                    i9++;
                                                }
                                            }
                                        }
                                    }
                                    if (this.mMncLength == 0 || this.mMncLength == -1) {
                                        if (imsi4 != null) {
                                            try {
                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi4.substring(0, 3)));
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
                                    if (imsi4 != null && this.mMncLength != 0 && imsi4.length() >= this.mMncLength + 3) {
                                        log("update mccmnc=" + imsi4.substring(0, this.mMncLength + 3));
                                        context = this.mContext;
                                        strSubstring = imsi4.substring(0, 3 + this.mMncLength);
                                        MccTable.updateMccMncConfiguration(context, strSubstring, false);
                                    }
                                    if (!z) {
                                    }
                                } else if (bArr5.length == 3) {
                                    log("MNC length not present in EF_AD");
                                    String imsi5 = getIMSI();
                                    if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi5 != null && imsi5.length() >= 6) {
                                        String strSubstring5 = imsi5.substring(0, 6);
                                        log("mccmncCode=" + strSubstring5);
                                        String[] strArr4 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                                        int length5 = strArr4.length;
                                        int i10 = 0;
                                        while (true) {
                                            if (i10 < length5) {
                                                if (strArr4[i10].equals(strSubstring5)) {
                                                    this.mMncLength = 3;
                                                    log("setting6 mMncLength=" + this.mMncLength);
                                                } else {
                                                    i10++;
                                                }
                                            }
                                        }
                                    }
                                    if (this.mMncLength == 0 || this.mMncLength == -1) {
                                        if (imsi5 != null) {
                                            try {
                                                this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi5.substring(0, 3)));
                                                log("setting7 mMncLength=" + this.mMncLength);
                                            } catch (NumberFormatException e6) {
                                                this.mMncLength = 0;
                                                loge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                            }
                                        } else {
                                            this.mMncLength = 0;
                                            log("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                                        }
                                    }
                                    if (imsi5 != null && this.mMncLength != 0 && imsi5.length() >= this.mMncLength + 3) {
                                        log("update mccmnc=" + imsi5.substring(0, this.mMncLength + 3));
                                        context = this.mContext;
                                        strSubstring = imsi5.substring(0, 3 + this.mMncLength);
                                        MccTable.updateMccMncConfiguration(context, strSubstring, false);
                                    }
                                    if (!z) {
                                    }
                                } else {
                                    this.mMncLength = bArr5[3] & 15;
                                    log("setting4 mMncLength=" + this.mMncLength);
                                }
                            }
                            onRecordLoaded();
                        }
                        try {
                            this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(getIMSI().substring(0, 3)));
                            log("[TestMode] mMncLength=" + this.mMncLength);
                        } catch (NumberFormatException e7) {
                            this.mMncLength = 0;
                            loge("[TestMode] Corrupt IMSI! mMncLength=" + this.mMncLength);
                        }
                        break;
                        if (this.mMncLength == 15) {
                            this.mMncLength = 0;
                            log("setting5 mMncLength=" + this.mMncLength);
                        } else if (this.mMncLength != 2 && this.mMncLength != 3) {
                            this.mMncLength = -1;
                            log("setting5 mMncLength=" + this.mMncLength);
                        }
                        String imsi6 = getIMSI();
                        if ((this.mMncLength == -1 || this.mMncLength == 0 || this.mMncLength == 2) && imsi6 != null && imsi6.length() >= 6) {
                            String strSubstring6 = imsi6.substring(0, 6);
                            log("mccmncCode=" + strSubstring6);
                            String[] strArr5 = MCCMNC_CODES_HAVING_3DIGITS_MNC;
                            int length6 = strArr5.length;
                            int i11 = 0;
                            while (true) {
                                if (i11 < length6) {
                                    if (strArr5[i11].equals(strSubstring6)) {
                                        this.mMncLength = 3;
                                        log("setting6 mMncLength=" + this.mMncLength);
                                    } else {
                                        i11++;
                                    }
                                }
                            }
                        }
                        if (this.mMncLength == 0 || this.mMncLength == -1) {
                            if (imsi6 != null) {
                                try {
                                    this.mMncLength = MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi6.substring(0, 3)));
                                    log("setting7 mMncLength=" + this.mMncLength);
                                } catch (NumberFormatException e8) {
                                    this.mMncLength = 0;
                                    loge("Corrupt IMSI! setting8 mMncLength=" + this.mMncLength);
                                }
                            } else {
                                this.mMncLength = 0;
                                log("MNC length not present in EF_AD setting9 mMncLength=" + this.mMncLength);
                            }
                        }
                        if (imsi6 != null && this.mMncLength != 0 && imsi6.length() >= this.mMncLength + 3) {
                            log("update mccmnc=" + imsi6.substring(0, this.mMncLength + 3));
                            MccTable.updateMccMncConfiguration(this.mContext, imsi6.substring(0, 3 + this.mMncLength), false);
                        }
                        if (!z) {
                        }
                        onRecordLoaded();
                        break;
                    } finally {
                        if (i2 != i) {
                            if (i5 != 0) {
                                if (i6 == i) {
                                    if (imsi != null) {
                                        if (length >= i) {
                                            while (true) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (i3 != 0) {
                            if (i4 == i) {
                                if (imsi != null) {
                                    try {
                                        break;
                                    } catch (NumberFormatException e9) {
                                    }
                                }
                            }
                        }
                    }
                case 10:
                    AsyncResult asyncResult8 = (AsyncResult) message.obj;
                    if (asyncResult8.exception != null) {
                        log("Invalid or missing EF[MSISDN]");
                    } else {
                        AdnRecord adnRecord2 = (AdnRecord) asyncResult8.result;
                        this.mMsisdn = adnRecord2.getNumber();
                        this.mMsisdnTag = adnRecord2.getAlphaTag();
                        log("MSISDN: " + Rlog.pii(LOG_TAG, this.mMsisdn));
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 12:
                    getSpnFsm(false, (AsyncResult) message.obj);
                    if (!z) {
                    }
                    onRecordLoaded();
                case 13:
                    AsyncResult asyncResult9 = (AsyncResult) message.obj;
                    byte[] bArr6 = (byte[]) asyncResult9.result;
                    if (asyncResult9.exception == null) {
                        parseEfSpdi(bArr6);
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                case 14:
                    AsyncResult asyncResult10 = (AsyncResult) message.obj;
                    if (asyncResult10.exception != null) {
                        logw("update failed. ", asyncResult10.exception);
                    }
                    break;
                case 15:
                    AsyncResult asyncResult11 = (AsyncResult) message.obj;
                    byte[] bArr7 = (byte[]) asyncResult11.result;
                    if (asyncResult11.exception == null) {
                        SimTlv simTlv = new SimTlv(bArr7, 0, bArr7.length);
                        while (true) {
                            if (simTlv.isValidObject()) {
                                if (simTlv.getTag() == 67) {
                                    this.mPnnHomeName = IccUtils.networkNameToString(simTlv.getData(), 0, simTlv.getData().length);
                                    log("PNN: " + this.mPnnHomeName);
                                } else {
                                    simTlv.nextObject();
                                }
                            }
                        }
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
                default:
                    switch (i) {
                        case 17:
                            AsyncResult asyncResult12 = (AsyncResult) message.obj;
                            byte[] bArr8 = (byte[]) asyncResult12.result;
                            if (asyncResult12.exception == null) {
                                this.mUsimServiceTable = new UsimServiceTable(bArr8);
                                log("SST: " + this.mUsimServiceTable);
                            }
                            break;
                        case 18:
                            AsyncResult asyncResult13 = (AsyncResult) message.obj;
                            if (asyncResult13.exception == null) {
                                handleSmses((ArrayList) asyncResult13.result);
                            }
                            break;
                        case 19:
                            Rlog.i("ENF", "marked read: sms " + message.arg1);
                            break;
                        case 20:
                            AsyncResult asyncResult14 = (AsyncResult) message.obj;
                            log("EVENT_SET_MBDN_DONE ex:" + asyncResult14.exception);
                            if (asyncResult14.exception == null) {
                                this.mVoiceMailNum = this.mNewVoiceMailNum;
                                this.mVoiceMailTag = this.mNewVoiceMailTag;
                            }
                            if (isCphsMailboxEnabled()) {
                                AdnRecord adnRecord3 = new AdnRecord(this.mVoiceMailTag, this.mVoiceMailNum);
                                Message message2 = (Message) asyncResult14.userObj;
                                if (asyncResult14.exception == null && asyncResult14.userObj != null) {
                                    AsyncResult.forMessage((Message) asyncResult14.userObj).exception = null;
                                    ((Message) asyncResult14.userObj).sendToTarget();
                                    log("Callback with MBDN successful.");
                                    message2 = null;
                                }
                                new AdnRecordLoader(this.mFh).updateEF(adnRecord3, IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, null, obtainMessage(25, message2));
                            } else if (asyncResult14.userObj != null) {
                                CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
                                if (asyncResult14.exception == null || carrierConfigManager == null || !carrierConfigManager.getConfig().getBoolean("editable_voicemail_number_bool")) {
                                    AsyncResult.forMessage((Message) asyncResult14.userObj).exception = asyncResult14.exception;
                                } else {
                                    AsyncResult.forMessage((Message) asyncResult14.userObj).exception = new IccVmNotSupportedException("Update SIM voice mailbox error");
                                }
                                ((Message) asyncResult14.userObj).sendToTarget();
                            }
                            break;
                        case 21:
                            AsyncResult asyncResult15 = (AsyncResult) message.obj;
                            Integer num = (Integer) asyncResult15.result;
                            if (asyncResult15.exception == null && num != null) {
                                log("READ EF_SMS RECORD index=" + num);
                                this.mFh.loadEFLinearFixed(IccConstants.EF_SMS, num.intValue(), obtainMessage(22));
                            } else {
                                loge("Error on SMS_ON_SIM with exp " + asyncResult15.exception + " index " + num);
                            }
                            break;
                        case 22:
                            AsyncResult asyncResult16 = (AsyncResult) message.obj;
                            if (asyncResult16.exception != null) {
                                loge("Error on GET_SMS with exp " + asyncResult16.exception);
                            } else {
                                handleSms((byte[]) asyncResult16.result);
                            }
                            break;
                        default:
                            switch (i) {
                                case 24:
                                    AsyncResult asyncResult17 = (AsyncResult) message.obj;
                                    byte[] bArr9 = (byte[]) asyncResult17.result;
                                    if (asyncResult17.exception == null) {
                                        log("EF_CFF_CPHS: " + IccUtils.bytesToHexString(bArr9));
                                        this.mEfCff = bArr9;
                                    } else {
                                        this.mEfCff = null;
                                    }
                                    break;
                                case 25:
                                    AsyncResult asyncResult18 = (AsyncResult) message.obj;
                                    if (asyncResult18.exception == null) {
                                        this.mVoiceMailNum = this.mNewVoiceMailNum;
                                        this.mVoiceMailTag = this.mNewVoiceMailTag;
                                    } else {
                                        log("Set CPHS MailBox with exception: " + asyncResult18.exception);
                                    }
                                    if (asyncResult18.userObj != null) {
                                        log("Callback with CPHS MB successful.");
                                        AsyncResult.forMessage((Message) asyncResult18.userObj).exception = asyncResult18.exception;
                                        ((Message) asyncResult18.userObj).sendToTarget();
                                    }
                                    break;
                                case 26:
                                    AsyncResult asyncResult19 = (AsyncResult) message.obj;
                                    if (asyncResult19.exception == null) {
                                        this.mCphsInfo = (byte[]) asyncResult19.result;
                                        log("iCPHS: " + IccUtils.bytesToHexString(this.mCphsInfo));
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case 32:
                                            AsyncResult asyncResult20 = (AsyncResult) message.obj;
                                            byte[] bArr10 = (byte[]) asyncResult20.result;
                                            if (asyncResult20.exception == null) {
                                                log("EF_CFIS: " + IccUtils.bytesToHexString(bArr10));
                                                this.mEfCfis = bArr10;
                                            } else {
                                                this.mEfCfis = null;
                                            }
                                            break;
                                        case 33:
                                            AsyncResult asyncResult21 = (AsyncResult) message.obj;
                                            if (asyncResult21.exception == null) {
                                                byte[] bArr11 = (byte[]) asyncResult21.result;
                                                log("EF_CSP: " + IccUtils.bytesToHexString(bArr11));
                                                handleEfCspData(bArr11);
                                            } else {
                                                loge("Exception in fetching EF_CSP data " + asyncResult21.exception);
                                            }
                                            break;
                                        case 34:
                                            AsyncResult asyncResult22 = (AsyncResult) message.obj;
                                            byte[] bArr12 = (byte[]) asyncResult22.result;
                                            if (asyncResult22.exception == null) {
                                                this.mGid1 = IccUtils.bytesToHexString(bArr12);
                                                log("GID1: " + this.mGid1);
                                            } else {
                                                loge("Exception in get GID1 " + asyncResult22.exception);
                                                this.mGid1 = null;
                                            }
                                            break;
                                        default:
                                            switch (i) {
                                                case 36:
                                                    AsyncResult asyncResult23 = (AsyncResult) message.obj;
                                                    byte[] bArr13 = (byte[]) asyncResult23.result;
                                                    if (asyncResult23.exception == null) {
                                                        this.mGid2 = IccUtils.bytesToHexString(bArr13);
                                                        log("GID2: " + this.mGid2);
                                                    } else {
                                                        loge("Exception in get GID2 " + asyncResult23.exception);
                                                        this.mGid2 = null;
                                                    }
                                                    break;
                                                case 37:
                                                    AsyncResult asyncResult24 = (AsyncResult) message.obj;
                                                    byte[] bArr14 = (byte[]) asyncResult24.result;
                                                    if (asyncResult24.exception == null && bArr14 != null) {
                                                        log("Received a PlmnActRecord, raw=" + IccUtils.bytesToHexString(bArr14));
                                                        this.mPlmnActRecords = PlmnActRecord.getRecords(bArr14);
                                                    } else {
                                                        loge("Failed getting User PLMN with Access Tech Records: " + asyncResult24.exception);
                                                    }
                                                    break;
                                                case 38:
                                                    AsyncResult asyncResult25 = (AsyncResult) message.obj;
                                                    byte[] bArr15 = (byte[]) asyncResult25.result;
                                                    if (asyncResult25.exception == null && bArr15 != null) {
                                                        log("Received a PlmnActRecord, raw=" + IccUtils.bytesToHexString(bArr15));
                                                        this.mOplmnActRecords = PlmnActRecord.getRecords(bArr15);
                                                    } else {
                                                        loge("Failed getting Operator PLMN with Access Tech Records: " + asyncResult25.exception);
                                                    }
                                                    break;
                                                case 39:
                                                    AsyncResult asyncResult26 = (AsyncResult) message.obj;
                                                    byte[] bArr16 = (byte[]) asyncResult26.result;
                                                    if (asyncResult26.exception == null && bArr16 != null) {
                                                        log("Received a PlmnActRecord, raw=" + IccUtils.bytesToHexString(bArr16));
                                                        this.mHplmnActRecords = PlmnActRecord.getRecords(bArr16);
                                                        log("HplmnActRecord[]=" + Arrays.toString(this.mHplmnActRecords));
                                                    } else {
                                                        loge("Failed getting Home PLMN with Access Tech Records: " + asyncResult26.exception);
                                                    }
                                                    break;
                                                case 40:
                                                    AsyncResult asyncResult27 = (AsyncResult) message.obj;
                                                    byte[] bArr17 = (byte[]) asyncResult27.result;
                                                    if (asyncResult27.exception == null && bArr17 != null) {
                                                        this.mEhplmns = parseBcdPlmnList(bArr17, "Equivalent Home");
                                                    } else {
                                                        loge("Failed getting Equivalent Home PLMNs: " + asyncResult27.exception);
                                                    }
                                                    break;
                                                case 41:
                                                    AsyncResult asyncResult28 = (AsyncResult) message.obj;
                                                    byte[] bArr18 = (byte[]) asyncResult28.result;
                                                    if (asyncResult28.exception == null && bArr18 != null) {
                                                        this.mFplmns = parseBcdPlmnList(bArr18, "Forbidden");
                                                        if (message.arg1 == 1238273) {
                                                            Message messageRetrievePendingResponseMessage = retrievePendingResponseMessage(Integer.valueOf(message.arg2));
                                                            if (messageRetrievePendingResponseMessage == null) {
                                                                loge("Failed to retrieve a response message for FPLMN");
                                                            } else {
                                                                AsyncResult.forMessage(messageRetrievePendingResponseMessage, Arrays.copyOf(this.mFplmns, this.mFplmns.length), (Throwable) null);
                                                                messageRetrievePendingResponseMessage.sendToTarget();
                                                            }
                                                        }
                                                    } else {
                                                        loge("Failed getting Forbidden PLMNs: " + asyncResult28.exception);
                                                    }
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case 258:
                                                        case 259:
                                                            onLocked(message.what);
                                                            break;
                                                        default:
                                                            super.handleMessage(message);
                                                            break;
                                                    }
                                                    break;
                                            }
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                    if (!z) {
                    }
                    onRecordLoaded();
            }
        } else {
            AsyncResult asyncResult29 = (AsyncResult) message.obj;
            if (asyncResult29.exception == null) {
                this.mMsisdn = this.mNewMsisdn;
                this.mMsisdnTag = this.mNewMsisdnTag;
                log("Success to update EF[MSISDN]");
            }
            if (asyncResult29.userObj != null) {
                AsyncResult.forMessage((Message) asyncResult29.userObj).exception = asyncResult29.exception;
                ((Message) asyncResult29.userObj).sendToTarget();
            }
        }
        z = false;
        if (!z) {
        }
        onRecordLoaded();
    }

    private class EfPlLoaded implements IccRecords.IccRecordLoaded {
        private EfPlLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_PL";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            SIMRecords.this.mEfPl = (byte[]) asyncResult.result;
            SIMRecords.this.log("EF_PL=" + IccUtils.bytesToHexString(SIMRecords.this.mEfPl));
        }
    }

    private class EfUsimLiLoaded implements IccRecords.IccRecordLoaded {
        private EfUsimLiLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_LI";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            SIMRecords.this.mEfLi = (byte[]) asyncResult.result;
            SIMRecords.this.log("EF_LI=" + IccUtils.bytesToHexString(SIMRecords.this.mEfLi));
        }
    }

    @Override
    protected void handleFileUpdate(int i) {
        if (i != 28435) {
            if (i == 28437) {
                this.mRecordsToLoad++;
                log("[CSP] SIM Refresh for EF_CSP_CPHS");
                this.mFh.loadEFTransparent(IccConstants.EF_CSP_CPHS, obtainMessage(33));
                return;
            }
            if (i == 28439) {
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MAILBOX_CPHS, IccConstants.EF_EXT1, 1, obtainMessage(11));
                return;
            }
            if (i == 28475) {
                log("SIM Refresh called for EF_FDN");
                this.mParentApp.queryFdn();
                this.mAdnCache.reset();
                return;
            } else if (i == 28480) {
                this.mRecordsToLoad++;
                log("SIM Refresh called for EF_MSISDN");
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
                return;
            } else if (i == 28615) {
                this.mRecordsToLoad++;
                new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MBDN, IccConstants.EF_EXT6, this.mMailboxIndex, obtainMessage(6));
                return;
            } else if (i != 28619) {
                this.mAdnCache.reset();
                fetchSimRecords();
                return;
            }
        }
        log("SIM Refresh called for EF_CFIS or EF_CFF_CPHS");
        loadCallForwardingRecords();
    }

    private int dispatchGsmMessage(SmsMessage smsMessage) {
        this.mNewSmsRegistrants.notifyResult(smsMessage);
        return 0;
    }

    private void handleSms(byte[] bArr) {
        if (bArr[0] != 0) {
            Rlog.d("ENF", "status : " + ((int) bArr[0]));
        }
        if (bArr[0] == 3) {
            int length = bArr.length - 1;
            byte[] bArr2 = new byte[length];
            System.arraycopy(bArr, 1, bArr2, 0, length);
            dispatchGsmMessage(SmsMessage.createFromPdu(bArr2, "3gpp"));
        }
    }

    private void handleSmses(ArrayList<byte[]> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            byte[] bArr = arrayList.get(i);
            if (bArr[0] != 0) {
                Rlog.i("ENF", "status " + i + ": " + ((int) bArr[0]));
            }
            if (bArr[0] == 3) {
                int length = bArr.length - 1;
                byte[] bArr2 = new byte[length];
                System.arraycopy(bArr, 1, bArr2, 0, length);
                dispatchGsmMessage(SmsMessage.createFromPdu(bArr2, "3gpp"));
                bArr[0] = 1;
            }
        }
    }

    @Override
    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        log("onRecordLoaded " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
        if (getRecordsLoaded()) {
            onAllRecordsLoaded();
            return;
        }
        if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    protected void setVoiceCallForwardingFlagFromSimRecords() {
        if (validEfCfis(this.mEfCfis)) {
            this.mCallForwardingStatus = this.mEfCfis[1] & 1;
            log("EF_CFIS: callForwardingEnabled=" + this.mCallForwardingStatus);
            return;
        }
        if (this.mEfCff != null) {
            this.mCallForwardingStatus = (this.mEfCff[0] & 15) != 10 ? 0 : 1;
            log("EF_CFF: callForwardingEnabled=" + this.mCallForwardingStatus);
            return;
        }
        this.mCallForwardingStatus = -1;
        log("EF_CFIS and EF_CFF not valid. callForwardingEnabled=" + this.mCallForwardingStatus);
    }

    protected void setSimLanguageFromEF() {
        if (Resources.getSystem().getBoolean(R.^attr-private.pointerIconVerticalText)) {
            setSimLanguage(this.mEfLi, this.mEfPl);
        } else {
            log("Not using EF LI/EF PL");
        }
    }

    private void onLockedAllRecordsLoaded() {
        setSimLanguageFromEF();
        if (this.mLockedRecordsReqReason == 1) {
            this.mLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            return;
        }
        if (this.mLockedRecordsReqReason == 2) {
            this.mNetworkLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            return;
        }
        loge("onLockedAllRecordsLoaded: unexpected mLockedRecordsReqReason " + this.mLockedRecordsReqReason);
    }

    @Override
    protected void onAllRecordsLoaded() {
        log("record load complete");
        setSimLanguageFromEF();
        setVoiceCallForwardingFlagFromSimRecords();
        String operatorNumeric = getOperatorNumeric();
        if (!TextUtils.isEmpty(operatorNumeric) && checkCdma3gCard()) {
            log("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='" + operatorNumeric + "'");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), operatorNumeric);
        } else {
            log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
        }
        String imsi = getIMSI();
        if (!TextUtils.isEmpty(imsi) && imsi.length() >= 3 && checkCdma3gCard()) {
            log("onAllRecordsLoaded set mcc imsi");
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), MccTable.countryCodeForMcc(Integer.parseInt(imsi.substring(0, 3))));
        } else {
            log("onAllRecordsLoaded empty imsi skipping setting mcc");
        }
        setVoiceMailByCountry(operatorNumeric);
        this.mLoaded.set(true);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    protected void setVoiceMailByCountry(String str) {
        if (this.mVmConfig.containsCarrier(str)) {
            this.mIsVoiceMailFixed = true;
            this.mVoiceMailNum = this.mVmConfig.getVoiceMailNumber(str);
            this.mVoiceMailTag = this.mVmConfig.getVoiceMailTag(str);
        }
    }

    public void getForbiddenPlmns(Message message) {
        this.mFh.loadEFTransparent(IccConstants.EF_FPLMN, obtainMessage(41, 1238273, storePendingResponseMessage(message)));
    }

    @Override
    public void onReady() {
        fetchSimRecords();
    }

    protected void onLocked(int i) {
        log("only fetch EF_LI, EF_PL and EF_ICCID in locked state");
        this.mLockedRecordsReqReason = i == 258 ? 1 : 2;
        loadEfLiAndEfPl();
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(4));
        this.mRecordsToLoad++;
    }

    private void loadEfLiAndEfPl() {
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            this.mFh.loadEFTransparent(IccConstants.EF_LI, obtainMessage(100, new EfUsimLiLoaded()));
            this.mRecordsToLoad++;
            this.mFh.loadEFTransparent(IccConstants.EF_PL, obtainMessage(100, new EfPlLoaded()));
            this.mRecordsToLoad++;
        }
    }

    protected void loadCallForwardingRecords() {
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
        this.mRecordsToLoad = this.mRecordsToLoad + 1;
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(4));
        this.mRecordsToLoad++;
        new AdnRecordLoader(this.mFh).loadFromEF(IccConstants.EF_MSISDN, getExtFromEf(IccConstants.EF_MSISDN), 1, obtainMessage(10));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_MBI, 1, obtainMessage(5));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_AD, obtainMessage(9));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_MWIS, 1, obtainMessage(7));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_VOICE_MAIL_INDICATOR_CPHS, obtainMessage(8));
        this.mRecordsToLoad++;
        loadCallForwardingRecords();
        getSpnFsm(true, null);
        this.mFh.loadEFTransparent(IccConstants.EF_SPDI, obtainMessage(13));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_PNN, 1, obtainMessage(15));
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
        this.mFh.loadEFTransparent(IccConstants.EF_PLMN_W_ACT, obtainMessage(37));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_OPLMN_W_ACT, obtainMessage(38));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_HPLMN_W_ACT, obtainMessage(39));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_EHPLMN, obtainMessage(40));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_FPLMN, obtainMessage(41, 1238272, -1));
        this.mRecordsToLoad++;
        loadEfLiAndEfPl();
        log("fetchSimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    @Override
    public int getDisplayRule(ServiceState serviceState) {
        if ((this.mParentApp != null && this.mParentApp.getUiccProfile() != null && this.mParentApp.getUiccProfile().getOperatorBrandOverride() != null) || !isSpnActive() || TextUtils.isEmpty(getServiceProviderName()) || this.mSpnDisplayCondition == -1) {
            return 2;
        }
        if (!useRoamingFromServiceState() ? isOnMatchingPlmn(serviceState.getOperatorNumeric()) : !serviceState.getRoaming()) {
            if ((this.mSpnDisplayCondition & 1) != 1) {
                return 1;
            }
        } else if ((this.mSpnDisplayCondition & 2) != 0) {
            return 2;
        }
        return 3;
    }

    private boolean useRoamingFromServiceState() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mParentApp.getPhoneId()))) != null && configForSubId.getBoolean("spn_display_rule_use_roaming_from_service_state_bool")) {
            return true;
        }
        return false;
    }

    private boolean isOnMatchingPlmn(String str) {
        if (str == null) {
            return false;
        }
        if (str.equals(getOperatorNumeric())) {
            return true;
        }
        if (this.mSpdiNetworks != null) {
            Iterator<String> it = this.mSpdiNetworks.iterator();
            while (it.hasNext()) {
                if (str.equals(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void getSpnFsm(boolean z, AsyncResult asyncResult) {
        if (z) {
            if (this.mSpnState == GetSpnFsmState.READ_SPN_3GPP || this.mSpnState == GetSpnFsmState.READ_SPN_CPHS || this.mSpnState == GetSpnFsmState.READ_SPN_SHORT_CPHS || this.mSpnState == GetSpnFsmState.INIT) {
                this.mSpnState = GetSpnFsmState.INIT;
            }
            this.mSpnState = GetSpnFsmState.INIT;
        }
        switch (this.mSpnState) {
            case INIT:
                setServiceProviderName(null);
                this.mFh.loadEFTransparent(IccConstants.EF_SPN, obtainMessage(12));
                this.mRecordsToLoad++;
                this.mSpnState = GetSpnFsmState.READ_SPN_3GPP;
                break;
            case READ_SPN_3GPP:
                if (asyncResult != null && asyncResult.exception == null) {
                    byte[] bArr = (byte[]) asyncResult.result;
                    this.mSpnDisplayCondition = 255 & bArr[0];
                    setServiceProviderName(IccUtils.adnStringFieldToString(bArr, 1, bArr.length - 1));
                    String serviceProviderName = getServiceProviderName();
                    if (serviceProviderName == null || serviceProviderName.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_CPHS;
                    } else {
                        log("Load EF_SPN: " + serviceProviderName + " spnDisplayCondition: " + this.mSpnDisplayCondition);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), serviceProviderName);
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
            case READ_SPN_CPHS:
                if (asyncResult != null && asyncResult.exception == null) {
                    byte[] bArr2 = (byte[]) asyncResult.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(bArr2, 0, bArr2.length));
                    String serviceProviderName2 = getServiceProviderName();
                    if (serviceProviderName2 == null || serviceProviderName2.length() == 0) {
                        this.mSpnState = GetSpnFsmState.READ_SPN_SHORT_CPHS;
                    } else {
                        this.mSpnDisplayCondition = 2;
                        log("Load EF_SPN_CPHS: " + serviceProviderName2);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), serviceProviderName2);
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
            case READ_SPN_SHORT_CPHS:
                if (asyncResult != null && asyncResult.exception == null) {
                    byte[] bArr3 = (byte[]) asyncResult.result;
                    setServiceProviderName(IccUtils.adnStringFieldToString(bArr3, 0, bArr3.length));
                    String serviceProviderName3 = getServiceProviderName();
                    if (serviceProviderName3 == null || serviceProviderName3.length() == 0) {
                        log("No SPN loaded in either CHPS or 3GPP");
                    } else {
                        this.mSpnDisplayCondition = 2;
                        log("Load EF_SPN_SHORT_CPHS: " + serviceProviderName3);
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), serviceProviderName3);
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

    private void parseEfSpdi(byte[] bArr) {
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
        this.mSpdiNetworks = new ArrayList<>(data.length / 3);
        for (int i = 0; i + 2 < data.length; i += 3) {
            String strBcdPlmnToString = IccUtils.bcdPlmnToString(data, i);
            if (strBcdPlmnToString != null && strBcdPlmnToString.length() >= 5) {
                log("EF_SPDI network: " + strBcdPlmnToString);
                this.mSpdiNetworks.add(strBcdPlmnToString);
            }
        }
    }

    private String[] parseBcdPlmnList(byte[] bArr, String str) {
        log("Received " + str + " PLMNs, raw=" + IccUtils.bytesToHexString(bArr));
        if (bArr.length == 0 || bArr.length % 3 != 0) {
            loge("Received invalid " + str + " PLMN list");
            return null;
        }
        int length = bArr.length / 3;
        String[] strArr = new String[length];
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            strArr[i] = IccUtils.bcdPlmnToString(bArr, i2 * 3);
            if (!TextUtils.isEmpty(strArr[i])) {
                i++;
            }
        }
        return (String[]) Arrays.copyOf(strArr, i);
    }

    protected boolean isCphsMailboxEnabled() {
        return this.mCphsInfo != null && (this.mCphsInfo[1] & 48) == 48;
    }

    @Override
    protected void log(String str) {
        Rlog.d(LOG_TAG, "[SIMRecords] " + str);
    }

    @Override
    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[SIMRecords] " + str);
    }

    protected void logw(String str, Throwable th) {
        Rlog.w(LOG_TAG, "[SIMRecords] " + str, th);
    }

    protected void logv(String str) {
        Rlog.v(LOG_TAG, "[SIMRecords] " + str);
    }

    @Override
    public boolean isCspPlmnEnabled() {
        return this.mCspPlmnEnabled;
    }

    private void handleEfCspData(byte[] bArr) {
        int length = bArr.length / 2;
        this.mCspPlmnEnabled = true;
        for (int i = 0; i < length; i++) {
            int i2 = 2 * i;
            if (bArr[i2] == -64) {
                StringBuilder sb = new StringBuilder();
                sb.append("[CSP] found ValueAddedServicesGroup, value ");
                int i3 = i2 + 1;
                sb.append((int) bArr[i3]);
                log(sb.toString());
                if ((bArr[i3] & 128) == 128) {
                    this.mCspPlmnEnabled = true;
                    return;
                }
                this.mCspPlmnEnabled = false;
                log("[CSP] Set Automatic Network Selection");
                this.mNetworkSelectionModeAutomaticRegistrants.notifyRegistrants();
                return;
            }
        }
        log("[CSP] Value Added Service Group (0xC0), not found!");
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("SIMRecords: " + this);
        printWriter.println(" extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println(" mVmConfig=" + this.mVmConfig);
        printWriter.println(" mCallForwardingStatus=" + this.mCallForwardingStatus);
        printWriter.println(" mSpnState=" + this.mSpnState);
        printWriter.println(" mCphsInfo=" + this.mCphsInfo);
        printWriter.println(" mCspPlmnEnabled=" + this.mCspPlmnEnabled);
        printWriter.println(" mEfMWIS[]=" + Arrays.toString(this.mEfMWIS));
        printWriter.println(" mEfCPHS_MWI[]=" + Arrays.toString(this.mEfCPHS_MWI));
        printWriter.println(" mEfCff[]=" + Arrays.toString(this.mEfCff));
        printWriter.println(" mEfCfis[]=" + Arrays.toString(this.mEfCfis));
        printWriter.println(" mSpnDisplayCondition=" + this.mSpnDisplayCondition);
        printWriter.println(" mSpdiNetworks[]=" + this.mSpdiNetworks);
        printWriter.println(" mUsimServiceTable=" + this.mUsimServiceTable);
        printWriter.println(" mGid1=" + this.mGid1);
        if (this.mCarrierTestOverride.isInTestMode()) {
            printWriter.println(" mFakeGid1=" + this.mCarrierTestOverride.getFakeGid1());
        }
        printWriter.println(" mGid2=" + this.mGid2);
        if (this.mCarrierTestOverride.isInTestMode()) {
            printWriter.println(" mFakeGid2=" + this.mCarrierTestOverride.getFakeGid2());
        }
        printWriter.println(" mPnnHomeName=" + this.mPnnHomeName);
        if (this.mCarrierTestOverride.isInTestMode()) {
            printWriter.println(" mFakePnnHomeName=" + this.mCarrierTestOverride.getFakePnnHomeName());
        }
        printWriter.println(" mPlmnActRecords[]=" + Arrays.toString(this.mPlmnActRecords));
        printWriter.println(" mOplmnActRecords[]=" + Arrays.toString(this.mOplmnActRecords));
        printWriter.println(" mHplmnActRecords[]=" + Arrays.toString(this.mHplmnActRecords));
        printWriter.println(" mFplmns[]=" + Arrays.toString(this.mFplmns));
        printWriter.println(" mEhplmns[]=" + Arrays.toString(this.mEhplmns));
        printWriter.flush();
    }

    protected boolean checkCdma3gCard() {
        return true;
    }

    protected boolean isSpnActive() {
        return true;
    }
}
