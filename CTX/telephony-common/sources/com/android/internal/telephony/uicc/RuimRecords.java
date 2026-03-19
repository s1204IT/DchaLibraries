package com.android.internal.telephony.uicc;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.util.BitwiseInputStream;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class RuimRecords extends IccRecords {
    protected static final int EVENT_APP_LOCKED = 32;
    private static final int EVENT_APP_NETWORK_LOCKED = 33;
    private static final int EVENT_GET_ALL_SMS_DONE = 18;
    private static final int EVENT_GET_CDMA_SUBSCRIPTION_DONE = 10;
    private static final int EVENT_GET_DEVICE_IDENTITY_DONE = 4;
    private static final int EVENT_GET_ICCID_DONE = 5;
    private static final int EVENT_GET_IMSI_DONE = 3;
    private static final int EVENT_GET_SMS_DONE = 22;
    protected static final int EVENT_GET_SST_DONE = 17;
    private static final int EVENT_MARK_SMS_READ_DONE = 19;
    private static final int EVENT_SMS_ON_RUIM = 21;
    private static final int EVENT_UPDATE_DONE = 14;
    static final String LOG_TAG = "RuimRecords";
    protected boolean mCsimSpnDisplayCondition;
    private byte[] mEFli;
    private byte[] mEFpl;
    private String mHomeNetworkId;
    private String mHomeSystemId;
    private String mMdn;
    private String mMin;
    private String mMin2Min1;
    private String mMyMobileNumber;
    private String mNai;
    private boolean mOtaCommited;
    private String mPrlVersion;

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
        this.mAdnCache = new AdnRecordCache(this.mFh);
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mRecordsToLoad = 0;
        resetRecords();
        this.mParentApp.registerForReady(this, 1, null);
        this.mParentApp.registerForLocked(this, 32, null);
        this.mParentApp.registerForNetworkLocked(this, 33, null);
        log("RuimRecords X ctor this=" + this);
    }

    @Override
    public void dispose() {
        log("Disposing RuimRecords " + this);
        this.mParentApp.unregisterForReady(this);
        this.mParentApp.unregisterForLocked(this);
        this.mParentApp.unregisterForNetworkLocked(this);
        resetRecords();
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
        this.mAdnCache.reset();
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mLoaded.set(false);
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
    public void setVoiceMailNumber(String str, String str2, Message message) {
        AsyncResult.forMessage(message).exception = new IccException("setVoiceMailNumber not implemented");
        message.sendToTarget();
        loge("method setVoiceMailNumber is not implemented");
    }

    @Override
    public void onRefresh(boolean z, int[] iArr) {
        if (z) {
            fetchRuimRecords();
        }
    }

    private int adjstMinDigits(int i) {
        int i2 = i + 111;
        if (i2 % 10 == 0) {
            i2 -= 10;
        }
        if ((i2 / 10) % 10 == 0) {
            i2 -= 100;
        }
        return (i2 / 100) % 10 == 0 ? i2 - 1000 : i2;
    }

    public String getRUIMOperatorNumeric() {
        String imsi = getIMSI();
        if (imsi == null) {
            return null;
        }
        if (this.mMncLength != -1 && this.mMncLength != 0) {
            return imsi.substring(0, 3 + this.mMncLength);
        }
        return imsi.substring(0, 3 + MccTable.smallestDigitsMccForMnc(Integer.parseInt(imsi.substring(0, 3))));
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
            RuimRecords.this.mEFpl = (byte[]) asyncResult.result;
            RuimRecords.this.log("EF_PL=" + IccUtils.bytesToHexString(RuimRecords.this.mEFpl));
        }
    }

    private class EfCsimLiLoaded implements IccRecords.IccRecordLoaded {
        private EfCsimLiLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_LI";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            RuimRecords.this.mEFli = (byte[]) asyncResult.result;
            for (int i = 0; i < RuimRecords.this.mEFli.length; i += 2) {
                int i2 = i + 1;
                switch (RuimRecords.this.mEFli[i2]) {
                    case 1:
                        RuimRecords.this.mEFli[i] = 101;
                        RuimRecords.this.mEFli[i2] = 110;
                        break;
                    case 2:
                        RuimRecords.this.mEFli[i] = 102;
                        RuimRecords.this.mEFli[i2] = 114;
                        break;
                    case 3:
                        RuimRecords.this.mEFli[i] = 101;
                        RuimRecords.this.mEFli[i2] = 115;
                        break;
                    case 4:
                        RuimRecords.this.mEFli[i] = 106;
                        RuimRecords.this.mEFli[i2] = 97;
                        break;
                    case 5:
                        RuimRecords.this.mEFli[i] = 107;
                        RuimRecords.this.mEFli[i2] = 111;
                        break;
                    case 6:
                        RuimRecords.this.mEFli[i] = 122;
                        RuimRecords.this.mEFli[i2] = 104;
                        break;
                    case 7:
                        RuimRecords.this.mEFli[i] = 104;
                        RuimRecords.this.mEFli[i2] = 101;
                        break;
                    default:
                        RuimRecords.this.mEFli[i] = 32;
                        RuimRecords.this.mEFli[i2] = 32;
                        break;
                }
            }
            RuimRecords.this.log("EF_LI=" + IccUtils.bytesToHexString(RuimRecords.this.mEFli));
        }
    }

    private class EfCsimSpnLoaded implements IccRecords.IccRecordLoaded {
        private EfCsimSpnLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_SPN";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            RuimRecords.this.log("CSIM_SPN=" + IccUtils.bytesToHexString(bArr));
            RuimRecords.this.mCsimSpnDisplayCondition = (bArr[0] & 1) != 0;
            byte b = bArr[1];
            byte b2 = bArr[2];
            byte[] bArr2 = new byte[32];
            System.arraycopy(bArr, 3, bArr2, 0, bArr.length - 3 < 32 ? bArr.length - 3 : 32);
            int i = 0;
            while (i < bArr2.length && (bArr2[i] & 255) != 255) {
                i++;
            }
            if (i == 0) {
                RuimRecords.this.setServiceProviderName("");
                return;
            }
            try {
                if (b != 0) {
                    switch (b) {
                        case 2:
                            String str = new String(bArr2, 0, i, "US-ASCII");
                            if (TextUtils.isPrintableAsciiOnly(str)) {
                                RuimRecords.this.setServiceProviderName(str);
                            } else {
                                RuimRecords.this.log("Some corruption in SPN decoding = " + str);
                                RuimRecords.this.log("Using ENCODING_GSM_7BIT_ALPHABET scheme...");
                                RuimRecords.this.setServiceProviderName(GsmAlphabet.gsm7BitPackedToString(bArr2, 0, (i * 8) / 7));
                            }
                            break;
                        case 3:
                            RuimRecords.this.setServiceProviderName(GsmAlphabet.gsm7BitPackedToString(bArr2, 0, (i * 8) / 7));
                            break;
                        case 4:
                            RuimRecords.this.setServiceProviderName(new String(bArr2, 0, i, CharacterSets.MIMENAME_UTF_16));
                            break;
                        default:
                            switch (b) {
                                case 8:
                                    RuimRecords.this.setServiceProviderName(new String(bArr2, 0, i, "ISO-8859-1"));
                                    break;
                                case 9:
                                    break;
                                default:
                                    RuimRecords.this.log("SPN encoding not supported");
                                    break;
                            }
                            break;
                    }
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
        private EfCsimMdnLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_MDN";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            RuimRecords.this.log("CSIM_MDN=" + IccUtils.bytesToHexString(bArr));
            RuimRecords.this.mMdn = IccUtils.cdmaBcdToString(bArr, 1, bArr[0] & 15);
            RuimRecords.this.log("CSIM MDN=" + RuimRecords.this.mMdn);
        }
    }

    private class EfCsimImsimLoaded implements IccRecords.IccRecordLoaded {
        private EfCsimImsimLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_IMSIM";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            if ((bArr[7] & 128) == 128) {
                int i = ((bArr[2] & 3) << 8) + (bArr[1] & 255);
                int i2 = (((bArr[5] & 255) << 8) | (bArr[4] & 255)) >> 6;
                int i3 = (bArr[4] >> 2) & 15;
                if (i3 > 9) {
                    i3 = 0;
                }
                int i4 = (bArr[3] & 255) | ((bArr[4] & 3) << 8);
                RuimRecords.this.mMin = String.format(Locale.US, "%03d", Integer.valueOf(RuimRecords.this.adjstMinDigits(i))) + String.format(Locale.US, "%03d", Integer.valueOf(RuimRecords.this.adjstMinDigits(i2))) + String.format(Locale.US, "%d", Integer.valueOf(i3)) + String.format(Locale.US, "%03d", Integer.valueOf(RuimRecords.this.adjstMinDigits(i4)));
                RuimRecords ruimRecords = RuimRecords.this;
                StringBuilder sb = new StringBuilder();
                sb.append("min present=");
                sb.append(RuimRecords.this.mMin);
                ruimRecords.log(sb.toString());
                return;
            }
            RuimRecords.this.log("min not present");
        }
    }

    private class EfCsimCdmaHomeLoaded implements IccRecords.IccRecordLoaded {
        private EfCsimCdmaHomeLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_CDMAHOME";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            ArrayList<byte[]> arrayList = (ArrayList) asyncResult.result;
            RuimRecords.this.log("CSIM_CDMAHOME data size=" + arrayList.size());
            if (arrayList.isEmpty()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            for (byte[] bArr : arrayList) {
                if (bArr.length == 5) {
                    int i = ((bArr[1] & 255) << 8) | (bArr[0] & 255);
                    int i2 = (bArr[2] & 255) | ((bArr[3] & 255) << 8);
                    sb.append(i);
                    sb.append(',');
                    sb2.append(i2);
                    sb2.append(',');
                }
            }
            sb.setLength(sb.length() - 1);
            sb2.setLength(sb2.length() - 1);
            RuimRecords.this.mHomeSystemId = sb.toString();
            RuimRecords.this.mHomeNetworkId = sb2.toString();
        }
    }

    private class EfCsimEprlLoaded implements IccRecords.IccRecordLoaded {
        private EfCsimEprlLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_EPRL";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            RuimRecords.this.onGetCSimEprlDone(asyncResult);
        }
    }

    private void onGetCSimEprlDone(AsyncResult asyncResult) {
        byte[] bArr = (byte[]) asyncResult.result;
        log("CSIM_EPRL=" + IccUtils.bytesToHexString(bArr));
        if (bArr.length > 3) {
            this.mPrlVersion = Integer.toString((bArr[3] & 255) | ((bArr[2] & 255) << 8));
        }
        log("CSIM PRL version=" + this.mPrlVersion);
    }

    private class EfCsimMipUppLoaded implements IccRecords.IccRecordLoaded {
        private EfCsimMipUppLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_CSIM_MIPUPP";
        }

        boolean checkLengthLegal(int i, int i2) {
            if (i < i2) {
                Log.e(RuimRecords.LOG_TAG, "CSIM MIPUPP format error, length = " + i + "expected length at least =" + i2);
                return false;
            }
            return true;
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            if (bArr.length < 1) {
                Log.e(RuimRecords.LOG_TAG, "MIPUPP read error");
                return;
            }
            BitwiseInputStream bitwiseInputStream = new BitwiseInputStream(bArr);
            try {
                int i = bitwiseInputStream.read(8) << 3;
                if (!checkLengthLegal(i, 1)) {
                    return;
                }
                int i2 = i - 1;
                if (bitwiseInputStream.read(1) == 1) {
                    if (!checkLengthLegal(i2, 11)) {
                        return;
                    }
                    bitwiseInputStream.skip(11);
                    i2 -= 11;
                }
                if (!checkLengthLegal(i2, 4)) {
                    return;
                }
                int i3 = bitwiseInputStream.read(4);
                int i4 = i2 - 4;
                for (int i5 = 0; i5 < i3 && checkLengthLegal(i4, 4); i5++) {
                    int i6 = bitwiseInputStream.read(4);
                    int i7 = i4 - 4;
                    if (!checkLengthLegal(i7, 8)) {
                        return;
                    }
                    int i8 = bitwiseInputStream.read(8);
                    int i9 = i7 - 8;
                    if (i6 == 0) {
                        if (!checkLengthLegal(i9, i8 << 3)) {
                            return;
                        }
                        char[] cArr = new char[i8];
                        for (int i10 = 0; i10 < i8; i10++) {
                            cArr[i10] = (char) (bitwiseInputStream.read(8) & 255);
                        }
                        RuimRecords.this.mNai = new String(cArr);
                        if (Log.isLoggable(RuimRecords.LOG_TAG, 2)) {
                            Log.v(RuimRecords.LOG_TAG, "MIPUPP Nai = " + RuimRecords.this.mNai);
                            return;
                        }
                        return;
                    }
                    int i11 = i8 << 3;
                    int i12 = i11 + 102;
                    if (!checkLengthLegal(i9, i12)) {
                        return;
                    }
                    bitwiseInputStream.skip(i11 + 101);
                    int i13 = i9 - i12;
                    if (bitwiseInputStream.read(1) == 1) {
                        if (!checkLengthLegal(i13, 32)) {
                            return;
                        }
                        bitwiseInputStream.skip(32);
                        i13 -= 32;
                    }
                    if (!checkLengthLegal(i13, 5)) {
                        return;
                    }
                    bitwiseInputStream.skip(4);
                    i4 = (i13 - 4) - 1;
                    if (bitwiseInputStream.read(1) == 1) {
                        if (!checkLengthLegal(i4, 32)) {
                            return;
                        }
                        bitwiseInputStream.skip(32);
                        i4 -= 32;
                    }
                }
            } catch (Exception e) {
                Log.e(RuimRecords.LOG_TAG, "MIPUPP read Exception error!");
            }
        }
    }

    @Override
    public void handleMessage(Message message) throws Throwable {
        if (this.mDestroyed.get()) {
            loge("Received message " + message + "[" + message.what + "] while being destroyed. Ignoring.");
            return;
        }
        boolean z = true;
        try {
            try {
                try {
                    switch (message.what) {
                        case 1:
                            onReady();
                            z = false;
                            break;
                        case 3:
                            AsyncResult asyncResult = (AsyncResult) message.obj;
                            if (asyncResult.exception == null) {
                                this.mImsi = (String) asyncResult.result;
                                if (this.mImsi != null && (this.mImsi.length() < 6 || this.mImsi.length() > 15)) {
                                    loge("invalid IMSI " + this.mImsi);
                                    this.mImsi = null;
                                }
                                log("NO update mccmnc=" + getRUIMOperatorNumeric());
                                onGetImsiDone(this.mImsi);
                            } else {
                                loge("Exception querying IMSI, Exception:" + asyncResult.exception);
                            }
                            break;
                        case 4:
                            log("Event EVENT_GET_DEVICE_IDENTITY_DONE Received");
                            z = false;
                            break;
                        case 5:
                            AsyncResult asyncResult2 = (AsyncResult) message.obj;
                            byte[] bArr = (byte[]) asyncResult2.result;
                            if (asyncResult2.exception == null) {
                                this.mIccId = IccUtils.bcdToString(bArr, 0, bArr.length);
                                this.mFullIccId = IccUtils.bchToString(bArr, 0, bArr.length);
                                log("iccid: " + SubscriptionInfo.givePrintableIccid(this.mFullIccId));
                            }
                            break;
                        case 10:
                            AsyncResult asyncResult3 = (AsyncResult) message.obj;
                            String[] strArr = (String[]) asyncResult3.result;
                            if (asyncResult3.exception == null) {
                                this.mMyMobileNumber = strArr[0];
                                this.mMin2Min1 = strArr[3];
                                this.mPrlVersion = strArr[4];
                                log("MDN: " + this.mMyMobileNumber + " MIN: " + this.mMin2Min1);
                            }
                            z = false;
                            break;
                        case 14:
                            AsyncResult asyncResult4 = (AsyncResult) message.obj;
                            if (asyncResult4.exception != null) {
                                Rlog.i(LOG_TAG, "RuimRecords update failed", asyncResult4.exception);
                            }
                            z = false;
                            break;
                        case 17:
                            log("Event EVENT_GET_SST_DONE Received");
                            z = false;
                            break;
                        case 18:
                        case 19:
                        case 21:
                        case 22:
                            Rlog.w(LOG_TAG, "Event not supported: " + message.what);
                            z = false;
                            break;
                        case 32:
                        case 33:
                            onLocked(message.what);
                            z = false;
                            break;
                        default:
                            super.handleMessage(message);
                            z = false;
                            break;
                    }
                    if (!z) {
                        return;
                    }
                } catch (RuntimeException e) {
                    e = e;
                    Rlog.w(LOG_TAG, "Exception parsing RUIM record", e);
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
        } catch (Throwable th2) {
            th = th2;
            z = false;
            if (z) {
            }
            throw th;
        }
        onRecordLoaded();
    }

    private static String[] getAssetLanguages(Context context) {
        String[] locales = context.getAssets().getLocales();
        String[] strArr = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            String str = locales[i];
            int iIndexOf = str.indexOf(45);
            if (iIndexOf < 0) {
                strArr[i] = str;
            } else {
                strArr[i] = str.substring(0, iIndexOf);
            }
        }
        return strArr;
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

    private void onLockedAllRecordsLoaded() {
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
        if (checkCdma3gCard()) {
            String rUIMOperatorNumeric = getRUIMOperatorNumeric();
            if (!TextUtils.isEmpty(rUIMOperatorNumeric)) {
                log("onAllRecordsLoaded set 'gsm.sim.operator.numeric' to operator='" + rUIMOperatorNumeric + "'");
                StringBuilder sb = new StringBuilder();
                sb.append("update icc_operator_numeric=");
                sb.append(rUIMOperatorNumeric);
                log(sb.toString());
                this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), rUIMOperatorNumeric);
            } else {
                log("onAllRecordsLoaded empty 'gsm.sim.operator.numeric' skipping");
            }
            String imsi = getIMSI();
            if (!TextUtils.isEmpty(imsi)) {
                log("onAllRecordsLoaded set mcc imsi=");
                this.mTelephonyManager.setSimCountryIsoForPhone(this.mParentApp.getPhoneId(), MccTable.countryCodeForMcc(Integer.parseInt(imsi.substring(0, 3))));
            } else {
                log("onAllRecordsLoaded empty imsi skipping setting mcc");
            }
        }
        if (Resources.getSystem().getBoolean(R.^attr-private.pointerIconVerticalText)) {
            setSimLanguage(this.mEFli, this.mEFpl);
        }
        this.mLoaded.set(true);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        if (!TextUtils.isEmpty(this.mMdn)) {
            int subIdUsingPhoneId = SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mParentApp.getUiccProfile().getPhoneId());
            if (SubscriptionManager.isValidSubscriptionId(subIdUsingPhoneId)) {
                SubscriptionManager.from(this.mContext).setDisplayNumber(this.mMdn, subIdUsingPhoneId);
            } else {
                log("Cannot call setDisplayNumber: invalid subId");
            }
        }
    }

    @Override
    public void onReady() {
        fetchRuimRecords();
        this.mCi.getCDMASubscription(obtainMessage(10));
    }

    protected void onLocked(int i) {
        log("only fetch EF_ICCID in locked state");
        this.mLockedRecordsReqReason = i == 32 ? 1 : 2;
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(5));
        this.mRecordsToLoad++;
    }

    protected void fetchRuimRecords() {
        this.mRecordsRequested = true;
        log("fetchRuimRecords " + this.mRecordsToLoad);
        this.mCi.getIMSIForApp(this.mParentApp.getAid(), obtainMessage(3));
        this.mRecordsToLoad = this.mRecordsToLoad + 1;
        this.mFh.loadEFTransparent(IccConstants.EF_ICCID, obtainMessage(5));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_PL, obtainMessage(100, new EfPlLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28474, obtainMessage(100, new EfCsimLiLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28481, obtainMessage(100, new EfCsimSpnLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixed(IccConstants.EF_CSIM_MDN, 1, obtainMessage(100, new EfCsimMdnLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSIM_IMSIM, obtainMessage(100, new EfCsimImsimLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_CSIM_CDMAHOME, obtainMessage(100, new EfCsimCdmaHomeLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSIM_EPRL, 4, obtainMessage(100, new EfCsimEprlLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_CSIM_MIPUPP, obtainMessage(100, new EfCsimMipUppLoaded()));
        this.mRecordsToLoad++;
        log("fetchRuimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    @Override
    public int getDisplayRule(ServiceState serviceState) {
        return 0;
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
    public void setVoiceMessageWaiting(int i, int i2) {
        log("RuimRecords:setVoiceMessageWaiting - NOP for CDMA");
    }

    @Override
    public int getVoiceMessageCount() {
        log("RuimRecords:getVoiceMessageCount - NOP for CDMA");
        return 0;
    }

    @Override
    protected void handleFileUpdate(int i) {
        this.mAdnCache.reset();
        fetchRuimRecords();
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
    protected void log(String str) {
        Rlog.d(LOG_TAG, "[RuimRecords] " + str);
    }

    @Override
    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[RuimRecords] " + str);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("RuimRecords: " + this);
        printWriter.println(" extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println(" mOtaCommited=" + this.mOtaCommited);
        printWriter.println(" mMyMobileNumber=" + this.mMyMobileNumber);
        printWriter.println(" mMin2Min1=" + this.mMin2Min1);
        printWriter.println(" mPrlVersion=" + this.mPrlVersion);
        printWriter.println(" mEFpl[]=" + Arrays.toString(this.mEFpl));
        printWriter.println(" mEFli[]=" + Arrays.toString(this.mEFli));
        printWriter.println(" mCsimSpnDisplayCondition=" + this.mCsimSpnDisplayCondition);
        printWriter.println(" mMdn=" + this.mMdn);
        printWriter.println(" mMin=" + this.mMin);
        printWriter.println(" mHomeSystemId=" + this.mHomeSystemId);
        printWriter.println(" mHomeNetworkId=" + this.mHomeNetworkId);
        printWriter.flush();
    }

    protected void onGetImsiDone(String str) {
    }

    protected boolean checkCdma3gCard() {
        return false;
    }
}
