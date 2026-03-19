package android.telephony;

import android.content.res.Resources;
import android.os.Binder;
import android.text.TextUtils;
import com.android.internal.R;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.Sms7BitEncodingTranslator;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

public class SmsMessage {
    public static final int ENCODING_16BIT = 3;
    public static final int ENCODING_7BIT = 1;
    public static final int ENCODING_8BIT = 2;
    public static final int ENCODING_KSC5601 = 4;
    public static final int ENCODING_UNKNOWN = 0;
    public static final String FORMAT_3GPP = "3gpp";
    public static final String FORMAT_3GPP2 = "3gpp2";
    private static final String LOG_TAG = "SmsMessage";
    public static final int MAX_USER_DATA_BYTES = 140;
    public static final int MAX_USER_DATA_BYTES_WITH_HEADER = 134;
    public static final int MAX_USER_DATA_SEPTETS = 160;
    public static final int MAX_USER_DATA_SEPTETS_WITH_HEADER = 153;
    protected int mSubId = 0;
    public SmsMessageBase mWrappedSmsMessage;
    private static NoEmsSupportConfig[] mNoEmsSupportConfigList = null;
    private static boolean mIsNoEmsSupportConfigListLoaded = false;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {
    }

    public enum MessageClass {
        UNKNOWN,
        CLASS_0,
        CLASS_1,
        CLASS_2,
        CLASS_3
    }

    public void setSubId(int i) {
        this.mSubId = i;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public static class SubmitPdu {
        public byte[] encodedMessage;
        public byte[] encodedScAddress;

        public String toString() {
            return "SubmitPdu: encodedScAddress = " + Arrays.toString(this.encodedScAddress) + ", encodedMessage = " + Arrays.toString(this.encodedMessage);
        }

        public SubmitPdu(SmsMessageBase.SubmitPduBase submitPduBase) {
            this.encodedMessage = submitPduBase.encodedMessage;
            this.encodedScAddress = submitPduBase.encodedScAddress;
        }
    }

    public SmsMessage(SmsMessageBase smsMessageBase) {
        this.mWrappedSmsMessage = smsMessageBase;
    }

    @Deprecated
    public static SmsMessage createFromPdu(byte[] bArr) {
        int currentPhoneType = TelephonyManager.getDefault().getCurrentPhoneType();
        SmsMessage smsMessageCreateFromPdu = createFromPdu(bArr, 2 == currentPhoneType ? "3gpp2" : "3gpp");
        if (smsMessageCreateFromPdu == null || smsMessageCreateFromPdu.mWrappedSmsMessage == null) {
            return createFromPdu(bArr, 2 == currentPhoneType ? "3gpp" : "3gpp2");
        }
        return smsMessageCreateFromPdu;
    }

    public static SmsMessage createFromPdu(byte[] bArr, String str) {
        SmsMessageBase smsMessageBaseCreateFromPdu;
        if (bArr == null) {
            Rlog.i(LOG_TAG, "createFromPdu(): pdu is null");
            return null;
        }
        if ("3gpp2".equals(str)) {
            smsMessageBaseCreateFromPdu = com.android.internal.telephony.cdma.SmsMessage.createFromPdu(bArr);
        } else if ("3gpp".equals(str)) {
            smsMessageBaseCreateFromPdu = com.android.internal.telephony.gsm.SmsMessage.createFromPdu(bArr);
        } else {
            Rlog.e(LOG_TAG, "createFromPdu(): unsupported message format " + str);
            return null;
        }
        if (smsMessageBaseCreateFromPdu != null) {
            return new SmsMessage(smsMessageBaseCreateFromPdu);
        }
        Rlog.e(LOG_TAG, "createFromPdu(): wrappedMessage is null");
        return null;
    }

    public static SmsMessage newFromCMT(byte[] bArr) {
        com.android.internal.telephony.gsm.SmsMessage smsMessageNewFromCMT = com.android.internal.telephony.gsm.SmsMessage.newFromCMT(bArr);
        if (smsMessageNewFromCMT != null) {
            return new SmsMessage(smsMessageNewFromCMT);
        }
        Rlog.e(LOG_TAG, "newFromCMT(): wrappedMessage is null");
        return null;
    }

    public static SmsMessage createFromEfRecord(int i, byte[] bArr) {
        SmsMessageBase smsMessageBaseCreateFromEfRecord;
        if (isCdmaVoice()) {
            smsMessageBaseCreateFromEfRecord = com.android.internal.telephony.cdma.SmsMessage.createFromEfRecord(i, bArr);
        } else {
            smsMessageBaseCreateFromEfRecord = com.android.internal.telephony.gsm.SmsMessage.createFromEfRecord(i, bArr);
        }
        if (smsMessageBaseCreateFromEfRecord != null) {
            return new SmsMessage(smsMessageBaseCreateFromEfRecord);
        }
        Rlog.e(LOG_TAG, "createFromEfRecord(): wrappedMessage is null");
        return null;
    }

    public static SmsMessage createFromEfRecord(int i, byte[] bArr, int i2) {
        SmsMessageBase smsMessageBaseCreateFromEfRecord;
        if (isCdmaVoice(i2)) {
            smsMessageBaseCreateFromEfRecord = com.android.internal.telephony.cdma.SmsMessage.createFromEfRecord(i, bArr);
        } else {
            smsMessageBaseCreateFromEfRecord = com.android.internal.telephony.gsm.SmsMessage.createFromEfRecord(i, bArr);
        }
        if (smsMessageBaseCreateFromEfRecord != null) {
            return new SmsMessage(smsMessageBaseCreateFromEfRecord);
        }
        return null;
    }

    public static int getTPLayerLengthForPDU(String str) {
        if (isCdmaVoice()) {
            return com.android.internal.telephony.cdma.SmsMessage.getTPLayerLengthForPDU(str);
        }
        return com.android.internal.telephony.gsm.SmsMessage.getTPLayerLengthForPDU(str);
    }

    public static int[] calculateLength(CharSequence charSequence, boolean z) {
        GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength;
        if (useCdmaFormatForMoSms()) {
            textEncodingDetailsCalculateLength = com.android.internal.telephony.cdma.SmsMessage.calculateLength(charSequence, z, true);
        } else {
            textEncodingDetailsCalculateLength = com.android.internal.telephony.gsm.SmsMessage.calculateLength(charSequence, z);
        }
        return new int[]{textEncodingDetailsCalculateLength.msgCount, textEncodingDetailsCalculateLength.codeUnitCount, textEncodingDetailsCalculateLength.codeUnitsRemaining, textEncodingDetailsCalculateLength.codeUnitSize};
    }

    public static ArrayList<String> fragmentText(String str) {
        GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength;
        int i;
        int iFindNextUnicodePosition;
        int i2;
        int i3 = 0;
        if (useCdmaFormatForMoSms()) {
            textEncodingDetailsCalculateLength = com.android.internal.telephony.cdma.SmsMessage.calculateLength(str, false, true);
        } else {
            textEncodingDetailsCalculateLength = com.android.internal.telephony.gsm.SmsMessage.calculateLength(str, false);
        }
        if (textEncodingDetailsCalculateLength.codeUnitSize == 1) {
            if (textEncodingDetailsCalculateLength.languageTable != 0 && textEncodingDetailsCalculateLength.languageShiftTable != 0) {
                i2 = 7;
            } else if (textEncodingDetailsCalculateLength.languageTable != 0 || textEncodingDetailsCalculateLength.languageShiftTable != 0) {
                i2 = 4;
            } else {
                i2 = 0;
            }
            if (textEncodingDetailsCalculateLength.msgCount > 1) {
                i2 += 6;
            }
            if (i2 != 0) {
                i2++;
            }
            i = 160 - i2;
        } else if (textEncodingDetailsCalculateLength.msgCount > 1) {
            i = 134;
            if (!hasEmsSupport() && textEncodingDetailsCalculateLength.msgCount < 10) {
                i = 132;
            }
        } else {
            i = 140;
        }
        String strTranslate = null;
        if (Resources.getSystem().getBoolean(R.bool.config_sms_force_7bit_encoding)) {
            strTranslate = Sms7BitEncodingTranslator.translate(str);
        }
        if (!TextUtils.isEmpty(strTranslate)) {
            str = strTranslate;
        }
        int length = str.length();
        ArrayList<String> arrayList = new ArrayList<>(textEncodingDetailsCalculateLength.msgCount);
        while (i3 < length) {
            if (textEncodingDetailsCalculateLength.codeUnitSize == 1) {
                if (useCdmaFormatForMoSms() && textEncodingDetailsCalculateLength.msgCount == 1) {
                    iFindNextUnicodePosition = Math.min(i, length - i3) + i3;
                } else {
                    iFindNextUnicodePosition = GsmAlphabet.findGsmSeptetLimitIndex(str, i3, i, textEncodingDetailsCalculateLength.languageTable, textEncodingDetailsCalculateLength.languageShiftTable);
                }
            } else {
                iFindNextUnicodePosition = SmsMessageBase.findNextUnicodePosition(i3, i, str);
            }
            if (iFindNextUnicodePosition <= i3 || iFindNextUnicodePosition > length) {
                Rlog.e(LOG_TAG, "fragmentText failed (" + i3 + " >= " + iFindNextUnicodePosition + " or " + iFindNextUnicodePosition + " >= " + length + ")");
                break;
            }
            arrayList.add(str.substring(i3, iFindNextUnicodePosition));
            i3 = iFindNextUnicodePosition;
        }
        return arrayList;
    }

    public static int[] calculateLength(String str, boolean z) {
        return calculateLength((CharSequence) str, z);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z) {
        return getSubmitPdu(str, str2, str3, z, SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, int i) {
        SmsMessageBase.SubmitPduBase submitPdu;
        if (useCdmaFormatForMoSms(i)) {
            submitPdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(str, str2, str3, z, (SmsHeader) null);
        } else {
            submitPdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(str, str2, str3, z);
        }
        return new SubmitPdu(submitPdu);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, short s, byte[] bArr, boolean z) {
        SmsMessageBase.SubmitPduBase submitPdu;
        if (useCdmaFormatForMoSms()) {
            submitPdu = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(str, str2, s, bArr, z);
        } else {
            submitPdu = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(str, str2, s, bArr, z);
        }
        return new SubmitPdu(submitPdu);
    }

    public String getServiceCenterAddress() {
        return this.mWrappedSmsMessage.getServiceCenterAddress();
    }

    public String getOriginatingAddress() {
        return this.mWrappedSmsMessage.getOriginatingAddress();
    }

    public String getDisplayOriginatingAddress() {
        return this.mWrappedSmsMessage.getDisplayOriginatingAddress();
    }

    public String getMessageBody() {
        return this.mWrappedSmsMessage.getMessageBody();
    }

    public MessageClass getMessageClass() {
        switch (this.mWrappedSmsMessage.getMessageClass()) {
            case CLASS_0:
                return MessageClass.CLASS_0;
            case CLASS_1:
                return MessageClass.CLASS_1;
            case CLASS_2:
                return MessageClass.CLASS_2;
            case CLASS_3:
                return MessageClass.CLASS_3;
            default:
                return MessageClass.UNKNOWN;
        }
    }

    public String getDisplayMessageBody() {
        return this.mWrappedSmsMessage.getDisplayMessageBody();
    }

    public String getPseudoSubject() {
        return this.mWrappedSmsMessage.getPseudoSubject();
    }

    public long getTimestampMillis() {
        return this.mWrappedSmsMessage.getTimestampMillis();
    }

    public boolean isEmail() {
        return this.mWrappedSmsMessage.isEmail();
    }

    public String getEmailBody() {
        return this.mWrappedSmsMessage.getEmailBody();
    }

    public String getEmailFrom() {
        return this.mWrappedSmsMessage.getEmailFrom();
    }

    public int getProtocolIdentifier() {
        return this.mWrappedSmsMessage.getProtocolIdentifier();
    }

    public boolean isReplace() {
        return this.mWrappedSmsMessage.isReplace();
    }

    public boolean isCphsMwiMessage() {
        return this.mWrappedSmsMessage.isCphsMwiMessage();
    }

    public boolean isMWIClearMessage() {
        return this.mWrappedSmsMessage.isMWIClearMessage();
    }

    public boolean isMWISetMessage() {
        return this.mWrappedSmsMessage.isMWISetMessage();
    }

    public boolean isMwiDontStore() {
        return this.mWrappedSmsMessage.isMwiDontStore();
    }

    public byte[] getUserData() {
        return this.mWrappedSmsMessage.getUserData();
    }

    public byte[] getPdu() {
        return this.mWrappedSmsMessage.getPdu();
    }

    @Deprecated
    public int getStatusOnSim() {
        return this.mWrappedSmsMessage.getStatusOnIcc();
    }

    public int getStatusOnIcc() {
        return this.mWrappedSmsMessage.getStatusOnIcc();
    }

    @Deprecated
    public int getIndexOnSim() {
        return this.mWrappedSmsMessage.getIndexOnIcc();
    }

    public int getIndexOnIcc() {
        return this.mWrappedSmsMessage.getIndexOnIcc();
    }

    public int getStatus() {
        return this.mWrappedSmsMessage.getStatus();
    }

    public boolean isStatusReportMessage() {
        return this.mWrappedSmsMessage.isStatusReportMessage();
    }

    public boolean isReplyPathPresent() {
        return this.mWrappedSmsMessage.isReplyPathPresent();
    }

    protected static boolean useCdmaFormatForMoSms() {
        return useCdmaFormatForMoSms(SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    protected static boolean useCdmaFormatForMoSms(int i) {
        SmsManager smsManagerForSubscriptionId = SmsManager.getSmsManagerForSubscriptionId(i);
        if (!smsManagerForSubscriptionId.isImsSmsSupported()) {
            return isCdmaVoice(i);
        }
        return "3gpp2".equals(smsManagerForSubscriptionId.getImsSmsFormat());
    }

    protected static boolean isCdmaVoice() {
        return isCdmaVoice(SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    protected static boolean isCdmaVoice(int i) {
        return 2 == TelephonyManager.getDefault().getCurrentPhoneType(i);
    }

    public static boolean hasEmsSupport() {
        if (!isNoEmsSupportConfigListExisted()) {
            return true;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String simOperatorNumeric = TelephonyManager.getDefault().getSimOperatorNumeric();
            String groupIdLevel1 = TelephonyManager.getDefault().getGroupIdLevel1();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (!TextUtils.isEmpty(simOperatorNumeric)) {
                for (NoEmsSupportConfig noEmsSupportConfig : mNoEmsSupportConfigList) {
                    if (simOperatorNumeric.startsWith(noEmsSupportConfig.mOperatorNumber) && (TextUtils.isEmpty(noEmsSupportConfig.mGid1) || (!TextUtils.isEmpty(noEmsSupportConfig.mGid1) && noEmsSupportConfig.mGid1.equalsIgnoreCase(groupIdLevel1)))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public static boolean shouldAppendPageNumberAsPrefix() {
        if (!isNoEmsSupportConfigListExisted()) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String simOperatorNumeric = TelephonyManager.getDefault().getSimOperatorNumeric();
            String groupIdLevel1 = TelephonyManager.getDefault().getGroupIdLevel1();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            for (NoEmsSupportConfig noEmsSupportConfig : mNoEmsSupportConfigList) {
                if (simOperatorNumeric.startsWith(noEmsSupportConfig.mOperatorNumber) && (TextUtils.isEmpty(noEmsSupportConfig.mGid1) || (!TextUtils.isEmpty(noEmsSupportConfig.mGid1) && noEmsSupportConfig.mGid1.equalsIgnoreCase(groupIdLevel1)))) {
                    return noEmsSupportConfig.mIsPrefix;
                }
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private static class NoEmsSupportConfig {
        String mGid1;
        boolean mIsPrefix;
        String mOperatorNumber;

        public NoEmsSupportConfig(String[] strArr) {
            this.mOperatorNumber = strArr[0];
            this.mIsPrefix = "prefix".equals(strArr[1]);
            this.mGid1 = strArr.length > 2 ? strArr[2] : null;
        }

        public String toString() {
            return "NoEmsSupportConfig { mOperatorNumber = " + this.mOperatorNumber + ", mIsPrefix = " + this.mIsPrefix + ", mGid1 = " + this.mGid1 + " }";
        }
    }

    private static boolean isNoEmsSupportConfigListExisted() {
        Resources system;
        if (!mIsNoEmsSupportConfigListLoaded && (system = Resources.getSystem()) != null) {
            String[] stringArray = system.getStringArray(R.array.no_ems_support_sim_operators);
            if (stringArray != null && stringArray.length > 0) {
                mNoEmsSupportConfigList = new NoEmsSupportConfig[stringArray.length];
                for (int i = 0; i < stringArray.length; i++) {
                    mNoEmsSupportConfigList[i] = new NoEmsSupportConfig(stringArray[i].split(";"));
                }
            }
            mIsNoEmsSupportConfigListLoaded = true;
        }
        return (mNoEmsSupportConfigList == null || mNoEmsSupportConfigList.length == 0) ? false : true;
    }
}
