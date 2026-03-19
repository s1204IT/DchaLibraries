package com.android.internal.telephony.uicc;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cat.BipUtils;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.util.BitwiseInputStream;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.internal.telephony.ppl.PplControlData;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class RuimRecords extends IccRecords {
    private static final int EVENT_DELAYED_SEND_PHB_CHANGE = 200;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    private static final int EVENT_GET_CDMA_ECC_DONE = 105;
    private static final int EVENT_GET_CDMA_SUBSCRIPTION_DONE = 10;
    private static final int EVENT_GET_DEVICE_IDENTITY_DONE = 4;
    private static final int EVENT_GET_EST_DONE = 117;
    private static final int EVENT_GET_ICCID_DONE = 5;
    private static final int EVENT_GET_IMSI_DONE = 3;
    private static final int EVENT_GET_SMS_DONE = 22;
    private static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    protected static final int EVENT_RADIO_STATE_CHANGED = 2;
    private static final int EVENT_RUIM_REFRESH = 31;
    private static final int EVENT_SMS_ON_RUIM = 21;
    private static final int EVENT_UPDATE_DONE = 14;
    static final String LOG_TAG = "RuimRecords";
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};
    static final String[] SIMRECORD_PROPERTY_RIL_PHB_READY = {"cdma.sim.ril.phbready", "cdma.sim.ril.phbready.2", "cdma.sim.ril.phbready.3", "cdma.sim.ril.phbready.4"};
    private String[] RUIMRECORDS_PROPERTY_ECC_LIST;
    boolean mCsimSpnDisplayCondition;
    private boolean mDispose;
    private byte[] mEFli;
    private byte[] mEFpl;
    String mEfEcc;
    private byte[] mEnableService;
    private final BroadcastReceiver mHandlePhbReadyReceiver;
    private String mHomeNetworkId;
    private String mHomeSystemId;
    private String mMdn;
    private String mMin;
    private String mMin2Min1;
    private String mMyMobileNumber;
    private String mNai;
    private boolean mOtaCommited;
    private boolean mPhbReady;
    private boolean mPhbWaitSub;
    private Phone mPhone;
    int mPhoneId;
    private String mPrlVersion;
    private RadioTechnologyChangedReceiver mRTC;
    private String mRuimImsi;
    private byte[] mSimService;
    private BroadcastReceiver mSubReceiver;

    @Override
    public String toString() {
        return "RuimRecords: " + super.toString() + " m_ota_commited" + this.mOtaCommited + " mMyMobileNumber=xxxx mMin2Min1=" + this.mMin2Min1 + " mPrlVersion=" + this.mPrlVersion + " mEFpl=" + this.mEFpl + " mEFli=" + this.mEFli + " mCsimSpnDisplayCondition=" + this.mCsimSpnDisplayCondition + " mMdn=" + this.mMdn + " mMin=" + this.mMin + " mHomeSystemId=" + this.mHomeSystemId + " mHomeNetworkId=" + this.mHomeNetworkId;
    }

    public RuimRecords(UiccCardApplication uiccCardApplication, Context context, CommandsInterface commandsInterface) {
        super(uiccCardApplication, context, commandsInterface);
        this.mOtaCommited = false;
        this.mEFpl = null;
        this.mEFli = null;
        this.mCsimSpnDisplayCondition = false;
        this.mRuimImsi = null;
        this.mEfEcc = UsimPBMemInfo.STRING_NOT_SET;
        this.RUIMRECORDS_PROPERTY_ECC_LIST = new String[]{"cdma.ril.ecclist", "cdma.ril.ecclist1", "cdma.ril.ecclist2", "cdma.ril.ecclist3"};
        this.mPhbReady = false;
        this.mPhbWaitSub = false;
        this.mDispose = false;
        this.mHandlePhbReadyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                RuimRecords.this.log("Receive action " + action);
                if (!action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO)) {
                    return;
                }
                RuimRecords.this.log("ACTION_SHUTDOWN_IPO: clear PHB_READY systemproperties");
                if (RuimRecords.this.mParentApp == null) {
                    return;
                }
                SystemProperties.set(RuimRecords.SIMRECORD_PROPERTY_RIL_PHB_READY[RuimRecords.this.mParentApp.getSlotId()], "false");
                RuimRecords.this.mPhbReady = false;
            }
        };
        this.mPhoneId = uiccCardApplication.getSlotId();
        this.mPhone = PhoneFactory.getPhone(uiccCardApplication.getPhoneId());
        this.mAdnCache = new AdnRecordCache(this.mFh, commandsInterface, uiccCardApplication);
        this.mRecordsRequested = false;
        this.mRecordsToLoad = 0;
        this.mCi.registerForIccRefresh(this, 31, null);
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        log("RuimRecords X ctor this=" + this);
        this.mCi.registerForPhbReady(this, 410, null);
        this.mCi.registerForRadioStateChanged(this, 2, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IWorldPhone.ACTION_SHUTDOWN_IPO);
        this.mContext.registerReceiver(this.mHandlePhbReadyReceiver, intentFilter);
        this.mAdnCache.reset();
        this.mSubReceiver = new SubBroadCastReceiver(this, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        this.mContext.registerReceiver(this.mSubReceiver, intentFilter2);
        this.mRTC = new RadioTechnologyChangedReceiver(this, 0 == true ? 1 : 0);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.mRTC, intentFilter3);
        log("RuimRecords updateIccRecords in IccPhoneBookeInterfaceManager");
        if (this.mPhone.getIccPhoneBookInterfaceManager() != null) {
            this.mPhone.getIccPhoneBookInterfaceManager().updateIccRecords(this);
        }
        if (!isPhbReady()) {
            return;
        }
        log("RuimRecords : Phonebook is ready.");
        broadcastPhbStateChangedIntent(isPhbReady());
    }

    @Override
    public void dispose() {
        log("Disposing RuimRecords " + this);
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp.unregisterForReady(this);
        resetRecords();
        if (!isCdma4GDualModeCard()) {
            log("dispose, reset operator numeric, name and country iso");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
        }
        if (this.mParentApp != null) {
            log("Disposing RuimRecords mPhbReady  = " + this.mPhbReady);
            if (this.mPhbReady) {
                log("Disposing RuimRecords set PHB unready");
                SystemProperties.set(SIMRECORD_PROPERTY_RIL_PHB_READY[this.mParentApp.getSlotId()], "false");
                this.mPhbReady = false;
                broadcastPhbStateChangedIntent(this.mPhbReady);
            } else {
                log("dispose() " + this.mPhbReady + " is not true");
            }
            this.mParentApp.unregisterForReady(this);
        }
        this.mPhbWaitSub = false;
        this.mCi.unregisterForRadioStateChanged(this);
        this.mContext.unregisterReceiver(this.mRTC);
        this.mContext.unregisterReceiver(this.mHandlePhbReadyReceiver);
        this.mContext.unregisterReceiver(this.mSubReceiver);
        this.mAdnCache.reset();
        super.dispose();
    }

    protected void finalize() {
        log("RuimRecords finalized");
    }

    protected void resetRecords() {
        this.mMncLength = -1;
        log("setting0 mMncLength" + this.mMncLength);
        this.mIccId = null;
        this.mFullIccId = null;
        this.mRecordsRequested = false;
    }

    @Override
    public String getIMSI() {
        return this.mImsi;
    }

    public String getMdnNumber() {
        return this.mMyMobileNumber;
    }

    public String getCdmaMin() {
        return this.mMin2Min1;
    }

    public String getPrlVersion() {
        return this.mPrlVersion;
    }

    @Override
    public String getNAI() {
        return this.mNai;
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
        AsyncResult.forMessage(onComplete).exception = new IccException("setVoiceMailNumber not implemented");
        onComplete.sendToTarget();
        loge("method setVoiceMailNumber is not implemented");
    }

    @Override
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (!fileChanged) {
            return;
        }
        fetchRuimRecords();
    }

    private int adjstMinDigits(int digits) {
        int digits2 = digits + 111;
        if (digits2 % 10 == 0) {
            digits2 -= 10;
        }
        if ((digits2 / 10) % 10 == 0) {
            digits2 -= 100;
        }
        return (digits2 / 100) % 10 == 0 ? digits2 - 1000 : digits2;
    }

    public String getRUIMOperatorNumeric() {
        if (this.mImsi == null) {
            return null;
        }
        if (this.mMncLength != -1 && this.mMncLength != 0) {
            return this.mImsi.substring(0, this.mMncLength + 3);
        }
        int mcc = Integer.parseInt(this.mImsi.substring(0, 3));
        return this.mImsi.substring(0, MccTable.smallestDigitsMccForMnc(mcc) + 3);
    }

    @Override
    public String getOperatorNumeric() {
        if (this.mImsi == null) {
            return null;
        }
        if (this.mMncLength != -1 && this.mMncLength != 0) {
            return this.mImsi.substring(0, this.mMncLength + 3);
        }
        int mcc = Integer.parseInt(this.mImsi.substring(0, 3));
        return this.mImsi.substring(0, MccTable.smallestDigitsMccForMnc(mcc) + 3);
    }

    private class EfPlLoaded implements IccRecords.IccRecordLoaded {
        EfPlLoaded(RuimRecords this$0, EfPlLoaded efPlLoaded) {
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
            RuimRecords.this.mEFpl = (byte[]) ar.result;
            RuimRecords.this.log("EF_PL=" + IccUtils.bytesToHexString(RuimRecords.this.mEFpl));
        }
    }

    private class EfCsimLiLoaded implements IccRecords.IccRecordLoaded {
        EfCsimLiLoaded(RuimRecords this$0, EfCsimLiLoaded efCsimLiLoaded) {
            this();
        }

        private EfCsimLiLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_LI";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            RuimRecords.this.mEFli = (byte[]) ar.result;
            for (int i = 0; i < RuimRecords.this.mEFli.length; i += 2) {
                switch (RuimRecords.this.mEFli[i + 1]) {
                    case 1:
                        RuimRecords.this.mEFli[i] = 101;
                        RuimRecords.this.mEFli[i + 1] = 110;
                        break;
                    case 2:
                        RuimRecords.this.mEFli[i] = 102;
                        RuimRecords.this.mEFli[i + 1] = 114;
                        break;
                    case 3:
                        RuimRecords.this.mEFli[i] = 101;
                        RuimRecords.this.mEFli[i + 1] = 115;
                        break;
                    case 4:
                        RuimRecords.this.mEFli[i] = 106;
                        RuimRecords.this.mEFli[i + 1] = 97;
                        break;
                    case 5:
                        RuimRecords.this.mEFli[i] = 107;
                        RuimRecords.this.mEFli[i + 1] = 111;
                        break;
                    case 6:
                        RuimRecords.this.mEFli[i] = 122;
                        RuimRecords.this.mEFli[i + 1] = 104;
                        break;
                    case 7:
                        RuimRecords.this.mEFli[i] = 104;
                        RuimRecords.this.mEFli[i + 1] = 101;
                        break;
                    default:
                        RuimRecords.this.mEFli[i] = 32;
                        RuimRecords.this.mEFli[i + 1] = 32;
                        break;
                }
            }
            RuimRecords.this.log("EF_LI=" + IccUtils.bytesToHexString(RuimRecords.this.mEFli));
        }
    }

    private class EfCsimSpnLoaded implements IccRecords.IccRecordLoaded {
        EfCsimSpnLoaded(RuimRecords this$0, EfCsimSpnLoaded efCsimSpnLoaded) {
            this();
        }

        private EfCsimSpnLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_SPN";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            RuimRecords.this.log("CSIM_SPN=" + IccUtils.bytesToHexString(data));
            RuimRecords.this.mCsimSpnDisplayCondition = (data[0] & 1) != 0;
            int encoding = data[1];
            byte b = data[2];
            byte[] spnData = new byte[32];
            int len = data.length + (-3) < 32 ? data.length - 3 : 32;
            System.arraycopy(data, 3, spnData, 0, len);
            int numBytes = 0;
            while (numBytes < spnData.length && (spnData[numBytes] & PplMessageManager.Type.INVALID) != 255) {
                numBytes++;
            }
            if (numBytes == 0) {
                RuimRecords.this.setServiceProviderName(UsimPBMemInfo.STRING_NOT_SET);
                return;
            }
            try {
                switch (encoding) {
                    case 0:
                    case 8:
                        RuimRecords.this.setServiceProviderName(new String(spnData, 0, numBytes, "ISO-8859-1"));
                        break;
                    case 1:
                    case 5:
                    case 6:
                    case 7:
                    default:
                        RuimRecords.this.log("SPN encoding not supported");
                        break;
                    case 2:
                        String spn = new String(spnData, 0, numBytes, "US-ASCII");
                        if (TextUtils.isPrintableAsciiOnly(spn)) {
                            RuimRecords.this.setServiceProviderName(spn);
                        } else {
                            RuimRecords.this.log("Some corruption in SPN decoding = " + spn);
                            RuimRecords.this.log("Using ENCODING_GSM_7BIT_ALPHABET scheme...");
                            RuimRecords.this.setServiceProviderName(GsmAlphabet.gsm7BitPackedToString(spnData, 0, (numBytes * 8) / 7));
                        }
                        break;
                    case 3:
                    case 9:
                        RuimRecords.this.setServiceProviderName(GsmAlphabet.gsm7BitPackedToString(spnData, 0, (numBytes * 8) / 7));
                        break;
                    case 4:
                        RuimRecords.this.setServiceProviderName(new String(spnData, 0, numBytes, CharacterSets.MIMENAME_UTF_16));
                        break;
                }
            } catch (Exception e) {
                RuimRecords.this.log("spn decode error: " + e);
            }
            RuimRecords.this.log("spn=" + RuimRecords.this.getServiceProviderName());
            RuimRecords.this.log("spnCondition=" + RuimRecords.this.mCsimSpnDisplayCondition);
            RuimRecords.this.mTelephonyManager.setSimOperatorNameForPhone(RuimRecords.this.mParentApp.getPhoneId(), RuimRecords.this.getServiceProviderName());
        }
    }

    private class EfCsimMdnLoaded implements IccRecords.IccRecordLoaded {
        EfCsimMdnLoaded(RuimRecords this$0, EfCsimMdnLoaded efCsimMdnLoaded) {
            this();
        }

        private EfCsimMdnLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_MDN";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            RuimRecords.this.log("CSIM_MDN=" + IccUtils.bytesToHexString(data));
            int mdnDigitsNum = data[0] & 15;
            RuimRecords.this.mMdn = IccUtils.cdmaBcdToString(data, 1, mdnDigitsNum);
            RuimRecords.this.log("CSIM MDN=" + RuimRecords.this.mMdn);
        }
    }

    private class EfCsimImsimLoaded implements IccRecords.IccRecordLoaded {
        EfCsimImsimLoaded(RuimRecords this$0, EfCsimImsimLoaded efCsimImsimLoaded) {
            this();
        }

        private EfCsimImsimLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_IMSIM";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            boolean provisioned = (data[7] & BipUtils.TCP_STATUS_ESTABLISHED) == 128;
            if (provisioned) {
                int first3digits = ((data[2] & 3) << 8) + (data[1] & PplMessageManager.Type.INVALID);
                int second3digits = (((data[5] & PplMessageManager.Type.INVALID) << 8) | (data[4] & PplMessageManager.Type.INVALID)) >> 6;
                int digit7 = (data[4] >> 2) & 15;
                if (digit7 > 9) {
                    digit7 = 0;
                }
                int last3digits = ((data[4] & 3) << 8) | (data[3] & PplMessageManager.Type.INVALID);
                RuimRecords.this.mMin = String.format(Locale.US, "%03d", Integer.valueOf(RuimRecords.this.adjstMinDigits(first3digits))) + String.format(Locale.US, "%03d", Integer.valueOf(RuimRecords.this.adjstMinDigits(second3digits))) + String.format(Locale.US, "%d", Integer.valueOf(digit7)) + String.format(Locale.US, "%03d", Integer.valueOf(RuimRecords.this.adjstMinDigits(last3digits)));
                RuimRecords.this.log(new StringBuilder().append("min present=").append(RuimRecords.this.mMin).toString());
                return;
            }
            RuimRecords.this.log("min not present");
        }
    }

    private class EfCsimCdmaHomeLoaded implements IccRecords.IccRecordLoaded {
        EfCsimCdmaHomeLoaded(RuimRecords this$0, EfCsimCdmaHomeLoaded efCsimCdmaHomeLoaded) {
            this();
        }

        private EfCsimCdmaHomeLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_CDMAHOME";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> dataList = (ArrayList) ar.result;
            RuimRecords.this.log("CSIM_CDMAHOME data size=" + dataList.size());
            if (dataList.isEmpty()) {
                return;
            }
            StringBuilder sidBuf = new StringBuilder();
            StringBuilder nidBuf = new StringBuilder();
            for (byte[] data : dataList) {
                if (data.length == 5) {
                    int sid = ((data[1] & PplMessageManager.Type.INVALID) << 8) | (data[0] & PplMessageManager.Type.INVALID);
                    int nid = ((data[3] & PplMessageManager.Type.INVALID) << 8) | (data[2] & PplMessageManager.Type.INVALID);
                    sidBuf.append(sid).append(',');
                    nidBuf.append(nid).append(',');
                }
            }
            sidBuf.setLength(sidBuf.length() - 1);
            nidBuf.setLength(nidBuf.length() - 1);
            RuimRecords.this.mHomeSystemId = sidBuf.toString();
            RuimRecords.this.mHomeNetworkId = nidBuf.toString();
        }
    }

    private class EfCsimEprlLoaded implements IccRecords.IccRecordLoaded {
        EfCsimEprlLoaded(RuimRecords this$0, EfCsimEprlLoaded efCsimEprlLoaded) {
            this();
        }

        private EfCsimEprlLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_EPRL";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            RuimRecords.this.onGetCSimEprlDone(ar);
        }
    }

    private void onGetCSimEprlDone(AsyncResult ar) {
        byte[] data = (byte[]) ar.result;
        log("CSIM_EPRL=" + IccUtils.bytesToHexString(data));
        if (data.length > 3) {
            int prlId = ((data[2] & PplMessageManager.Type.INVALID) << 8) | (data[3] & PplMessageManager.Type.INVALID);
            this.mPrlVersion = Integer.toString(prlId);
        }
        log("CSIM PRL version=" + this.mPrlVersion);
    }

    private class EfCsimMipUppLoaded implements IccRecords.IccRecordLoaded {
        EfCsimMipUppLoaded(RuimRecords this$0, EfCsimMipUppLoaded efCsimMipUppLoaded) {
            this();
        }

        private EfCsimMipUppLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_MIPUPP";
        }

        boolean checkLengthLegal(int length, int expectLength) {
            if (length < expectLength) {
                Log.e(RuimRecords.LOG_TAG, "CSIM MIPUPP format error, length = " + length + "expected length at least =" + expectLength);
                return false;
            }
            return true;
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            if (data.length < 1) {
                Log.e(RuimRecords.LOG_TAG, "MIPUPP read error");
                return;
            }
            BitwiseInputStream bitStream = new BitwiseInputStream(data);
            try {
                int mipUppLength = bitStream.read(8) << 3;
                if (!checkLengthLegal(mipUppLength, 1)) {
                    return;
                }
                int retryInfoInclude = bitStream.read(1);
                int mipUppLength2 = mipUppLength - 1;
                if (retryInfoInclude == 1) {
                    if (!checkLengthLegal(mipUppLength2, 11)) {
                        return;
                    }
                    bitStream.skip(11);
                    mipUppLength2 -= 11;
                }
                if (!checkLengthLegal(mipUppLength2, 4)) {
                    return;
                }
                int numNai = bitStream.read(4);
                int mipUppLength3 = mipUppLength2 - 4;
                for (int index = 0; index < numNai && checkLengthLegal(mipUppLength3, 4); index++) {
                    int naiEntryIndex = bitStream.read(4);
                    int mipUppLength4 = mipUppLength3 - 4;
                    if (!checkLengthLegal(mipUppLength4, 8)) {
                        return;
                    }
                    int naiLength = bitStream.read(8);
                    int mipUppLength5 = mipUppLength4 - 8;
                    if (naiEntryIndex == 0) {
                        if (!checkLengthLegal(mipUppLength5, naiLength << 3)) {
                            return;
                        }
                        char[] naiCharArray = new char[naiLength];
                        for (int index1 = 0; index1 < naiLength; index1++) {
                            naiCharArray[index1] = (char) (bitStream.read(8) & 255);
                        }
                        RuimRecords.this.mNai = new String(naiCharArray);
                        if (Log.isLoggable(RuimRecords.LOG_TAG, 2)) {
                            Log.v(RuimRecords.LOG_TAG, "MIPUPP Nai = " + RuimRecords.this.mNai);
                            return;
                        }
                        return;
                    }
                    if (!checkLengthLegal(mipUppLength5, (naiLength << 3) + CallFailCause.RECOVERY_ON_TIMER_EXPIRY)) {
                        return;
                    }
                    bitStream.skip((naiLength << 3) + 101);
                    int mnAaaSpiIndicator = bitStream.read(1);
                    int mipUppLength6 = mipUppLength5 - ((naiLength << 3) + CallFailCause.RECOVERY_ON_TIMER_EXPIRY);
                    if (mnAaaSpiIndicator == 1) {
                        if (!checkLengthLegal(mipUppLength6, 32)) {
                            return;
                        }
                        bitStream.skip(32);
                        mipUppLength6 -= 32;
                    }
                    if (!checkLengthLegal(mipUppLength6, 5)) {
                        return;
                    }
                    bitStream.skip(4);
                    int mnHaSpiIndicator = bitStream.read(1);
                    mipUppLength3 = (mipUppLength6 - 4) - 1;
                    if (mnHaSpiIndicator == 1) {
                        if (!checkLengthLegal(mipUppLength3, 32)) {
                            return;
                        }
                        bitStream.skip(32);
                        mipUppLength3 -= 32;
                    }
                }
            } catch (Exception e) {
                Log.e(RuimRecords.LOG_TAG, "MIPUPP read Exception error!");
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean isRecordLoadResponse = false;
        try {
            if (this.mDestroyed.get()) {
                loge("Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
                return;
            }
            try {
                switch (msg.what) {
                    case 1:
                        onReady();
                        log("handleMessage (EVENT_APP_READY)");
                        fetchEccList();
                        break;
                    case 3:
                        isRecordLoadResponse = true;
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            this.mImsi = (String) ar.result;
                            if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                                loge("invalid IMSI " + this.mImsi);
                                this.mImsi = null;
                            }
                            if (this.mImsi != null) {
                                log("IMSI: " + this.mImsi.substring(0, 6) + "xxxxxxxxx");
                            }
                            String operatorNumeric = getRUIMOperatorNumeric();
                            if (operatorNumeric != null && operatorNumeric.length() <= 6) {
                                log("update mccmnc=" + operatorNumeric);
                                MccTable.updateMccMncConfiguration(this.mContext, operatorNumeric, false);
                            }
                            if (this.mImsi != null && !this.mImsi.equals(UsimPBMemInfo.STRING_NOT_SET) && this.mImsi.length() >= 3) {
                                SystemProperties.set("cdma.icc.operator.mcc", this.mImsi.substring(0, 3));
                            }
                            if (!this.mImsi.equals(this.mRuimImsi)) {
                                this.mRuimImsi = this.mImsi;
                                this.mImsiReadyRegistrants.notifyRegistrants();
                                log("RuimRecords: mImsiReadyRegistrants.notifyRegistrants");
                            }
                        } else {
                            loge("Exception querying IMSI, Exception:" + ar.exception);
                        }
                        break;
                    case 4:
                        log("Event EVENT_GET_DEVICE_IDENTITY_DONE Received");
                        break;
                    case 5:
                        isRecordLoadResponse = true;
                        AsyncResult ar2 = (AsyncResult) msg.obj;
                        byte[] data = (byte[]) ar2.result;
                        if (ar2.exception == null) {
                            this.mIccId = IccUtils.bcdToString(data, 0, data.length);
                            this.mFullIccId = IccUtils.bchToString(data, 0, data.length);
                            log("iccid: " + SubscriptionInfo.givePrintableIccid(this.mFullIccId));
                        }
                        break;
                    case 10:
                        AsyncResult ar3 = (AsyncResult) msg.obj;
                        String[] localTemp = (String[]) ar3.result;
                        if (ar3.exception == null) {
                            this.mMyMobileNumber = localTemp[0];
                            this.mMin2Min1 = localTemp[3];
                            this.mPrlVersion = localTemp[4];
                            log("MDN: " + this.mMyMobileNumber + " MIN: " + this.mMin2Min1);
                        }
                        break;
                    case 14:
                        AsyncResult ar4 = (AsyncResult) msg.obj;
                        if (ar4.exception != null) {
                            Rlog.i(LOG_TAG, "RuimRecords update failed", ar4.exception);
                        }
                        break;
                    case 17:
                        log("Event EVENT_GET_SST_DONE Received");
                        isRecordLoadResponse = true;
                        AsyncResult ar5 = (AsyncResult) msg.obj;
                        if (ar5.exception == null) {
                            this.mSimService = (byte[]) ar5.result;
                            new String(this.mSimService);
                            log("mSimService[0]: " + ((int) this.mSimService[0]) + ", data.length: " + this.mSimService.length);
                            log("update FDN status after load SST done");
                            updateIccFdnStatus();
                        } else {
                            Rlog.i(LOG_TAG, "EVENT_GET_SST_DONE failed", ar5.exception);
                        }
                        break;
                    case 18:
                    case 19:
                    case 21:
                    case 22:
                        Rlog.w(LOG_TAG, "Event not supported: " + msg.what);
                        break;
                    case 31:
                        isRecordLoadResponse = false;
                        AsyncResult ar6 = (AsyncResult) msg.obj;
                        if (ar6.exception == null) {
                            handleRuimRefresh((IccRefreshResponse) ar6.result);
                        }
                        break;
                    case 105:
                        log("handleMessage (EVENT_GET_CDMA_ECC_DONE)");
                        AsyncResult ar7 = (AsyncResult) msg.obj;
                        if (ar7.exception == null) {
                            byte[] data2 = (byte[]) ar7.result;
                            int i = 0;
                            while (true) {
                                if (i + 2 < data2.length) {
                                    if ((data2[i] & 15) > 9) {
                                        log("Skip tailing byte");
                                    } else {
                                        String eccNum = IccUtils.cdmaBcdToString(data2, i, 3);
                                        if (eccNum != null && !eccNum.equals(UsimPBMemInfo.STRING_NOT_SET) && !this.mEfEcc.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                                            this.mEfEcc += ",";
                                        }
                                        this.mEfEcc += eccNum;
                                        i += 3;
                                    }
                                }
                            }
                            log("CDMA mEfEcc is " + this.mEfEcc);
                            SystemProperties.set(this.RUIMRECORDS_PROPERTY_ECC_LIST[this.mPhoneId], this.mEfEcc);
                        } else {
                            loge("Get CDMA ecc with exception: " + ar7.exception);
                        }
                        break;
                    case EVENT_GET_EST_DONE:
                        isRecordLoadResponse = true;
                        log("Event EVENT_GET_EST_DONE Received");
                        AsyncResult ar8 = (AsyncResult) msg.obj;
                        if (ar8.exception == null) {
                            this.mEnableService = (byte[]) ar8.result;
                            new String(this.mEnableService);
                            log("mEnableService[0]: " + ((int) this.mEnableService[0]) + ", mEnableService.length: " + this.mEnableService.length);
                            log("update FDN status after load EST done");
                            updateIccFdnStatus();
                        } else {
                            Rlog.i(LOG_TAG, "EVENT_GET_EST_DONE failed", ar8.exception);
                        }
                        break;
                    case 200:
                        boolean isReady = isPhbReady();
                        log("[EVENT_DELAYED_SEND_PHB_CHANGE] isReady : " + isReady);
                        broadcastPhbStateChangedIntent(isReady);
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
                if (isRecordLoadResponse) {
                    onRecordLoaded();
                }
            } catch (RuntimeException exc) {
                Rlog.w(LOG_TAG, "Exception parsing RUIM record", exc);
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

    private static String[] getAssetLanguages(Context ctx) {
        String[] locales = ctx.getAssets().getLocales();
        String[] localeLangs = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            String localeStr = locales[i];
            int separator = localeStr.indexOf(45);
            if (separator < 0) {
                localeLangs[i] = localeStr;
            } else {
                localeLangs[i] = localeStr.substring(0, separator);
            }
        }
        return localeLangs;
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

    @Override
    protected void onAllRecordsLoaded() {
        log("record load complete");
        if (this.mPhone.getPhoneType() == 2 && isCdmaOnly()) {
            String operator = getRUIMOperatorNumeric();
            if (!TextUtils.isEmpty(operator)) {
                log("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='" + operator + "'");
                log("update icc_operator_numeric=" + operator);
                this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), operator);
            } else {
                log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
            }
            if (!TextUtils.isEmpty(this.mImsi)) {
                log("onAllRecordsLoaded set mcc imsi=" + UsimPBMemInfo.STRING_NOT_SET);
                this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), MccTable.countryCodeForMcc(Integer.parseInt(this.mImsi.substring(0, 3))));
            } else {
                log("onAllRecordsLoaded empty imsi skipping setting mcc");
            }
        }
        Resources resource = Resources.getSystem();
        if (resource.getBoolean(R.^attr-private.mtpReserve)) {
            setSimLanguage(this.mEFli, this.mEFpl);
        }
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        if (TextUtils.isEmpty(this.mMdn)) {
            return;
        }
        int phoneId = this.mParentApp.getUiccCard().getPhoneId();
        int[] subIds = SubscriptionController.getInstance().getSubId(phoneId);
        if (subIds != null) {
            SubscriptionManager.from(this.mContext).setDisplayNumber(this.mMdn, subIds[0]);
        } else {
            log("Cannot call setDisplayNumber: invalid subId");
        }
    }

    @Override
    public void onReady() {
        fetchRuimRecords();
        this.mCi.getCDMASubscription(obtainMessage(10));
    }

    private void fetchRuimRecords() {
        this.mRecordsRequested = true;
        log("fetchRuimRecords " + this.mRecordsToLoad);
        this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(3));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(5));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(12037, obtainMessage(100, new EfPlLoaded(this, null)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28474, obtainMessage(100, new EfCsimLiLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28481, obtainMessage(100, new EfCsimSpnLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_CSIM_MDN, 1, obtainMessage(100, new EfCsimMdnLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSIM_IMSIM, obtainMessage(100, new EfCsimImsimLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_CSIM_CDMAHOME, obtainMessage(100, new EfCsimCdmaHomeLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSIM_EPRL, 4, obtainMessage(100, new EfCsimEprlLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSIM_MIPUPP, obtainMessage(100, new EfCsimMipUppLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        log("fetchRuimRecords mParentApp.getType() = " + this.mParentApp.getType());
        if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
            this.mFh.loadEFTransparent(IccConstants.EF_CST, obtainMessage(17));
            this.mRecordsToLoad++;
        } else if (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            this.mFh.loadEFTransparent(IccConstants.EF_CST, obtainMessage(17));
            this.mRecordsToLoad++;
            this.mFh.loadEFTransparent(IccConstants.EF_EST, obtainMessage(EVENT_GET_EST_DONE));
            this.mRecordsToLoad++;
        }
        log("fetchRuimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    @Override
    public int getDisplayRule(String plmn) {
        String spn = getServiceProviderName();
        log("getDisplayRule mParentApp is " + (this.mParentApp != null ? this.mParentApp : "null"));
        if (this.mParentApp != null && this.mParentApp.getUiccCard() != null && this.mParentApp.getUiccCard().getOperatorBrandOverride() != null) {
            log("getDisplayRule, getOperatorBrandOverride is not null");
            return 2;
        }
        if (!this.mCsimSpnDisplayCondition) {
            log("getDisplayRule, no EF_SPN");
            return 2;
        }
        if (!TextUtils.isEmpty(spn) && !spn.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            log("getDisplayRule, show spn");
            return 1;
        }
        log("getDisplayRule, show plmn");
        return 2;
    }

    @Override
    public boolean isProvisioned() {
        if (SystemProperties.getBoolean("persist.radio.test-csim", false)) {
            return true;
        }
        if (this.mParentApp == null) {
            return false;
        }
        return (this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_CSIM && (this.mMdn == null || this.mMin == null)) ? false : true;
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        log("RuimRecords:setVoiceMessageWaiting - NOP for CDMA");
    }

    @Override
    public int getVoiceMessageCount() {
        log("RuimRecords:getVoiceMessageCount - NOP for CDMA");
        return 0;
    }

    private void handleRuimRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleRuimRefresh received without input");
        }
        if (refreshResponse.aid != null && !refreshResponse.aid.equals(this.mParentApp.getAid())) {
            return;
        }
        switch (refreshResponse.refreshResult) {
            case 0:
                log("handleRuimRefresh with SIM_REFRESH_FILE_UPDATED");
                this.mAdnCache.reset();
                fetchRuimRecords();
                break;
            case 1:
                log("handleRuimRefresh with SIM_REFRESH_INIT");
                onIccRefreshInit();
                break;
            case 2:
                log("handleRuimRefresh with SIM_REFRESH_RESET");
                break;
            default:
                log("handleRuimRefresh with unknown operation");
                break;
        }
    }

    public String getMdn() {
        return this.mMdn;
    }

    public String getMin() {
        return this.mMin;
    }

    public String getSid() {
        return this.mHomeSystemId;
    }

    public String getNid() {
        return this.mHomeNetworkId;
    }

    public boolean getCsimSpnDisplayCondition() {
        return this.mCsimSpnDisplayCondition;
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[RuimRecords] " + s + " (phoneId " + this.mPhoneId + ")");
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[RuimRecords] " + s + " (phoneId " + this.mPhoneId + ")");
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("RuimRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.println(" mOtaCommited=" + this.mOtaCommited);
        pw.println(" mMyMobileNumber=" + this.mMyMobileNumber);
        pw.println(" mMin2Min1=" + this.mMin2Min1);
        pw.println(" mPrlVersion=" + this.mPrlVersion);
        pw.println(" mEFpl[]=" + Arrays.toString(this.mEFpl));
        pw.println(" mEFli[]=" + Arrays.toString(this.mEFli));
        pw.println(" mCsimSpnDisplayCondition=" + this.mCsimSpnDisplayCondition);
        pw.println(" mMdn=" + this.mMdn);
        pw.println(" mMin=" + this.mMin);
        pw.println(" mHomeSystemId=" + this.mHomeSystemId);
        pw.println(" mHomeNetworkId=" + this.mHomeNetworkId);
        pw.flush();
    }

    private void fetchEccList() {
        int eccFromModemUrc = SystemProperties.getInt("ril.ef.ecc.support", 0);
        log("fetchEccList(), eccFromModemUrc:" + eccFromModemUrc);
        if (eccFromModemUrc != 0) {
            return;
        }
        this.mEfEcc = UsimPBMemInfo.STRING_NOT_SET;
        this.mFh.loadEFTransparent(IccConstants.EF_CDMA_ECC, obtainMessage(105));
    }

    public boolean isCdmaOnly() {
        String[] values = null;
        if (this.mPhoneId < 0 || this.mPhoneId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            log("isCdmaOnly: invalid PhoneId " + this.mPhoneId);
            return false;
        }
        String prop = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[this.mPhoneId]);
        if (prop != null && prop.length() > 0) {
            values = prop.split(",");
        }
        log("isCdmaOnly PhoneId " + this.mPhoneId + ", prop value= " + prop + ", size= " + (values != null ? values.length : 0));
        return (values == null || Arrays.asList(values).contains("USIM") || Arrays.asList(values).contains("SIM")) ? false : true;
    }

    public boolean isCdma4GDualModeCard() {
        String[] values = null;
        if (this.mPhoneId < 0 || this.mPhoneId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            log("isCdma4GDualModeCard: invalid PhoneId " + this.mPhoneId);
            return false;
        }
        String prop = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[this.mPhoneId]);
        if (prop != null && prop.length() > 0) {
            values = prop.split(",");
        }
        log("isCdma4GDualModeCard PhoneId " + this.mPhoneId + ", prop value= " + prop + ", size= " + (values != null ? values.length : 0));
        if (values == null || !Arrays.asList(values).contains("USIM")) {
            return false;
        }
        return Arrays.asList(values).contains("CSIM");
    }

    public void broadcastPhbStateChangedIntent(boolean isReady) {
        if (this.mPhone.getPhoneType() != 2) {
            log("broadcastPhbStateChangedIntent, Not active Phone.");
            return;
        }
        log("broadcastPhbStateChangedIntent, mPhbReady " + this.mPhbReady);
        if (isReady) {
            this.mSubId = SubscriptionManager.getSubIdUsingPhoneId(this.mParentApp.getPhoneId());
            if (this.mSubId <= 0) {
                log("broadcastPhbStateChangedIntent, mSubId <= 0");
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
            this.mPhbReady = false;
        }
        log("isPhbReady(): mPhbReady = " + (this.mPhbReady ? "true" : "false") + ", strCurSimState = " + strCurSimState);
        return this.mPhbReady;
    }

    private class SubBroadCastReceiver extends BroadcastReceiver {
        SubBroadCastReceiver(RuimRecords this$0, SubBroadCastReceiver subBroadCastReceiver) {
            this();
        }

        private SubBroadCastReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            RuimRecords.this.log("SubBroadCastReceiver action is " + action);
            if (!action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED") || RuimRecords.this.mParentApp == null) {
                return;
            }
            RuimRecords.this.log("SubBroadCastReceiver receive ACTION_SUBINFO_RECORD_UPDATED mPhbWaitSub = " + RuimRecords.this.mPhbWaitSub);
            RuimRecords.this.log(RuimRecords.SIMRECORD_PROPERTY_RIL_PHB_READY[RuimRecords.this.mParentApp.getSlotId()] + " = " + SystemProperties.get(RuimRecords.SIMRECORD_PROPERTY_RIL_PHB_READY[RuimRecords.this.mParentApp.getSlotId()], "false"));
            if (!RuimRecords.this.mPhbWaitSub) {
                return;
            }
            RuimRecords.this.mPhbWaitSub = false;
            RuimRecords.this.broadcastPhbStateChangedIntent(RuimRecords.this.mPhbReady);
        }
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

    private class RadioTechnologyChangedReceiver extends BroadcastReceiver {
        RadioTechnologyChangedReceiver(RuimRecords this$0, RadioTechnologyChangedReceiver radioTechnologyChangedReceiver) {
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
            RuimRecords.this.log("[ACTION_RADIO_TECHNOLOGY_CHANGED] phoneid : " + phoneid);
            if (RuimRecords.this.mParentApp == null || RuimRecords.this.mParentApp.getPhoneId() != phoneid) {
                return;
            }
            String activePhoneName = intent.getStringExtra("phoneName");
            int subid = intent.getIntExtra("subscription", -1);
            RuimRecords.this.log("[ACTION_RADIO_TECHNOLOGY_CHANGED] activePhoneName : " + activePhoneName + " | subid : " + subid);
            if (!"CDMA".equals(activePhoneName)) {
                return;
            }
            RuimRecords.this.sendMessageDelayed(RuimRecords.this.obtainMessage(200), 500L);
            RuimRecords.this.mAdnCache.reset();
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
        if (status == 3) {
            if (!isSimLocked) {
                if (!this.mPhbReady) {
                    this.mPhbReady = true;
                    broadcastPhbStateChangedIntent(this.mPhbReady);
                    return;
                } else {
                    broadcastPhbStateChangedIntent(this.mPhbReady);
                    return;
                }
            }
            log("phb ready but sim is not ready.");
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
            return;
        }
        if (status != 2) {
            return;
        }
        if (this.mPhbReady) {
            this.mAdnCache.reset();
            this.mPhbReady = false;
            broadcastPhbStateChangedIntent(this.mPhbReady);
            return;
        }
        broadcastPhbStateChangedIntent(this.mPhbReady);
    }

    @Override
    protected void updateIccFdnStatus() {
        log(" updateIccFdnStatus mParentAPP: " + this.mParentApp);
        log(" phbready=" + isPhbReady() + " mParentAPP=" + this.mParentApp + "  getSIMServiceStatus(Phone.IccService.FDN)=" + getSIMServiceStatus(IccRecords.IccService.FDN) + "  IccServiceStatus.ACTIVATE=" + IccRecords.IccServiceStatus.ACTIVATED);
        if (isPhbReady() && this.mParentApp != null && getSIMServiceStatus(IccRecords.IccService.FDN) == IccRecords.IccServiceStatus.ACTIVATED) {
            this.mParentApp.queryFdn();
        }
    }

    @Override
    public IccRecords.IccServiceStatus getSIMServiceStatus(IccRecords.IccService enService) {
        IccRecords.IccServiceStatus simServiceStatus = IccRecords.IccServiceStatus.UNKNOWN;
        log("getSIMServiceStatus enService: " + enService + ", mParentApp.getType(): " + this.mParentApp.getType());
        if (enService == IccRecords.IccService.FDN && this.mSimService != null && this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
            log("getSIMServiceStatus mSimService[0]: " + ((int) this.mSimService[0]));
            if ((this.mSimService[0] & 48) == 48) {
                return IccRecords.IccServiceStatus.ACTIVATED;
            }
            if ((this.mSimService[0] & PplControlData.STATUS_WIPE_REQUESTED) == 0) {
                return IccRecords.IccServiceStatus.INACTIVATED;
            }
            return simServiceStatus;
        }
        if (enService == IccRecords.IccService.FDN && this.mSimService != null && this.mEnableService != null && this.mParentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_CSIM) {
            log("getSIMServiceStatus mSimService[0]: " + ((int) this.mSimService[0]));
            log("getSIMServiceStatus mEnableService[0]: " + ((int) this.mEnableService[0]));
            if ((this.mSimService[0] & 2) == 2 && (this.mEnableService[0] & 1) == 1) {
                return IccRecords.IccServiceStatus.ACTIVATED;
            }
            if ((this.mSimService[0] & 2) == 0) {
                return IccRecords.IccServiceStatus.INACTIVATED;
            }
            return simServiceStatus;
        }
        return simServiceStatus;
    }
}
