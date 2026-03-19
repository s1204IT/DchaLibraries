package android.telephony;

import android.R;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Parcel;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.Sms7BitEncodingTranslator;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import java.util.ArrayList;
import java.util.Arrays;

public class SmsMessage {

    private static final int[] f0xe0552cb5 = null;
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
    public static final int MWI_EMAIL = 2;
    public static final int MWI_FAX = 1;
    public static final int MWI_OTHER = 3;
    public static final int MWI_VIDEO = 7;
    public static final int MWI_VOICEMAIL = 0;
    private int mSubId;
    public SmsMessageBase mWrappedSmsMessage;
    private static NoEmsSupportConfig[] mNoEmsSupportConfigList = null;
    private static boolean mIsNoEmsSupportConfigListLoaded = false;

    private static int[] m0x492fb91() {
        if (f0xe0552cb5 != null) {
            return f0xe0552cb5;
        }
        int[] iArr = new int[SmsConstants.MessageClass.values().length];
        try {
            iArr[SmsConstants.MessageClass.CLASS_0.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SmsConstants.MessageClass.CLASS_1.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SmsConstants.MessageClass.CLASS_2.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SmsConstants.MessageClass.CLASS_3.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SmsConstants.MessageClass.UNKNOWN.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        f0xe0552cb5 = iArr;
        return iArr;
    }

    public enum MessageClass {
        UNKNOWN,
        CLASS_0,
        CLASS_1,
        CLASS_2,
        CLASS_3;

        public static MessageClass[] valuesCustom() {
            return values();
        }
    }

    public void setSubId(int subId) {
        this.mSubId = subId;
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

        protected SubmitPdu(SmsMessageBase.SubmitPduBase spb) {
            this.encodedMessage = spb.encodedMessage;
            this.encodedScAddress = spb.encodedScAddress;
        }
    }

    private SmsMessage(SmsMessageBase smb) {
        this.mSubId = 0;
        this.mWrappedSmsMessage = smb;
    }

    @Deprecated
    public static SmsMessage createFromPdu(byte[] pdu) {
        int activePhone = TelephonyManager.getDefault().getCurrentPhoneType();
        String format = 2 == activePhone ? FORMAT_3GPP2 : FORMAT_3GPP;
        SmsMessage message = createFromPdu(pdu, format);
        if (message == null || message.mWrappedSmsMessage == null) {
            String format2 = 2 == activePhone ? FORMAT_3GPP : FORMAT_3GPP2;
            return createFromPdu(pdu, format2);
        }
        return message;
    }

    public static SmsMessage createFromPdu(byte[] pdu, String format) {
        SmsMessageBase wrappedMessage;
        if (FORMAT_3GPP2.equals(format)) {
            wrappedMessage = com.android.internal.telephony.cdma.SmsMessage.createFromPdu(pdu);
        } else if (FORMAT_3GPP.equals(format)) {
            wrappedMessage = com.android.internal.telephony.gsm.SmsMessage.createFromPdu(pdu);
        } else {
            Rlog.e(LOG_TAG, "createFromPdu(): unsupported message format " + format);
            return null;
        }
        return new SmsMessage(wrappedMessage);
    }

    public static SmsMessage newFromCMT(String[] lines) {
        SmsMessageBase wrappedMessage = com.android.internal.telephony.gsm.SmsMessage.newFromCMT(lines);
        return new SmsMessage(wrappedMessage);
    }

    public static SmsMessage newFromParcel(Parcel p) {
        SmsMessageBase wrappedMessage = com.android.internal.telephony.cdma.SmsMessage.newFromParcel(p);
        return new SmsMessage(wrappedMessage);
    }

    public static SmsMessage createFromEfRecord(int index, byte[] data) {
        SmsMessageBase wrappedMessage;
        if (isCdmaVoice()) {
            wrappedMessage = com.android.internal.telephony.cdma.SmsMessage.createFromEfRecord(index, data);
        } else {
            wrappedMessage = com.android.internal.telephony.gsm.SmsMessage.createFromEfRecord(index, data);
        }
        if (wrappedMessage != null) {
            return new SmsMessage(wrappedMessage);
        }
        return null;
    }

    public static int getTPLayerLengthForPDU(String pdu) {
        if (isCdmaVoice()) {
            return com.android.internal.telephony.cdma.SmsMessage.getTPLayerLengthForPDU(pdu);
        }
        return com.android.internal.telephony.gsm.SmsMessage.getTPLayerLengthForPDU(pdu);
    }

    public static int[] calculateLength(CharSequence msgBody, boolean use7bitOnly) {
        GsmAlphabet.TextEncodingDetails ted;
        if (useCdmaFormatForMoSms()) {
            ted = com.android.internal.telephony.cdma.SmsMessage.calculateLength(msgBody, use7bitOnly, true);
        } else {
            ted = com.android.internal.telephony.gsm.SmsMessage.calculateLength(msgBody, use7bitOnly);
        }
        int[] ret = {ted.msgCount, ted.codeUnitCount, ted.codeUnitsRemaining, ted.codeUnitSize};
        return ret;
    }

    public static ArrayList<String> fragmentText(String text) {
        GsmAlphabet.TextEncodingDetails ted;
        int limit;
        int nextPos;
        int udhLength;
        if (useCdmaFormatForMoSms()) {
            ted = com.android.internal.telephony.cdma.SmsMessage.calculateLength((CharSequence) text, false, true);
        } else {
            ted = com.android.internal.telephony.gsm.SmsMessage.calculateLength(text, false);
        }
        if (ted.codeUnitSize == 1) {
            if (ted.languageTable != 0 && ted.languageShiftTable != 0) {
                udhLength = 7;
            } else if (ted.languageTable != 0 || ted.languageShiftTable != 0) {
                udhLength = 4;
            } else {
                udhLength = 0;
            }
            if (ted.msgCount > 1) {
                udhLength += 6;
            }
            if (udhLength != 0) {
                udhLength++;
            }
            limit = 160 - udhLength;
        } else if (ted.msgCount > 1) {
            limit = 134;
            if (!hasEmsSupport() && ted.msgCount < 10) {
                limit = 132;
            }
        } else {
            limit = 140;
        }
        String newMsgBody = null;
        Resources r = Resources.getSystem();
        if (r.getBoolean(R.^attr-private.modifier)) {
            newMsgBody = Sms7BitEncodingTranslator.translate(text);
        }
        if (TextUtils.isEmpty(newMsgBody)) {
            newMsgBody = text;
        }
        int pos = 0;
        int textLen = newMsgBody.length();
        ArrayList<String> result = new ArrayList<>(ted.msgCount);
        while (pos < textLen) {
            if (ted.codeUnitSize == 1) {
                if (useCdmaFormatForMoSms() && ted.msgCount == 1) {
                    nextPos = pos + Math.min(limit, textLen - pos);
                } else {
                    nextPos = GsmAlphabet.findGsmSeptetLimitIndex(newMsgBody, pos, limit, ted.languageTable, ted.languageShiftTable);
                }
            } else {
                nextPos = SmsMessageBase.findNextUnicodePosition(pos, limit, newMsgBody);
            }
            if (nextPos <= pos || nextPos > textLen) {
                Rlog.e(LOG_TAG, "fragmentText failed (" + pos + " >= " + nextPos + " or " + nextPos + " >= " + textLen + ")");
                break;
            }
            result.add(newMsgBody.substring(pos, nextPos));
            pos = nextPos;
        }
        return result;
    }

    public static int[] calculateLength(String messageBody, boolean use7bitOnly) {
        return calculateLength((CharSequence) messageBody, use7bitOnly);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested) {
        SmsMessageBase.SubmitPduBase spb;
        if (useCdmaFormatForMoSms()) {
            spb = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, (SmsHeader) null);
        } else {
            spb = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested);
        }
        return new SubmitPdu(spb);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, short destinationPort, byte[] data, boolean statusReportRequested) {
        SmsMessageBase.SubmitPduBase spb;
        if (useCdmaFormatForMoSms()) {
            spb = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddress, destinationAddress, destinationPort, data, statusReportRequested);
        } else {
            spb = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddress, destinationAddress, destinationPort, data, statusReportRequested);
        }
        return new SubmitPdu(spb);
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
        switch (m0x492fb91()[this.mWrappedSmsMessage.getMessageClass().ordinal()]) {
            case 1:
                return MessageClass.CLASS_0;
            case 2:
                return MessageClass.CLASS_1;
            case 3:
                return MessageClass.CLASS_2;
            case 4:
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

    private static boolean useCdmaFormatForMoSms() {
        if (!SmsManager.getDefault().isImsSmsSupported()) {
            return isCdmaVoice();
        }
        return FORMAT_3GPP2.equals(SmsManager.getDefault().getImsSmsFormat());
    }

    private static boolean isCdmaVoice() {
        int activePhone = TelephonyManager.getDefault().getCurrentPhoneType();
        return 2 == activePhone;
    }

    public static boolean hasEmsSupport() {
        if (!isNoEmsSupportConfigListExisted()) {
            return true;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            String simOperator = TelephonyManager.getDefault().getSimOperatorNumeric();
            String gid = TelephonyManager.getDefault().getGroupIdLevel1();
            Binder.restoreCallingIdentity(identity);
            if (!TextUtils.isEmpty(simOperator)) {
                for (NoEmsSupportConfig currentConfig : mNoEmsSupportConfigList) {
                    if (simOperator.startsWith(currentConfig.mOperatorNumber) && (TextUtils.isEmpty(currentConfig.mGid1) || (!TextUtils.isEmpty(currentConfig.mGid1) && currentConfig.mGid1.equalsIgnoreCase(gid)))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public static boolean shouldAppendPageNumberAsPrefix() {
        if (!isNoEmsSupportConfigListExisted()) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            String simOperator = TelephonyManager.getDefault().getSimOperatorNumeric();
            String gid = TelephonyManager.getDefault().getGroupIdLevel1();
            Binder.restoreCallingIdentity(identity);
            for (NoEmsSupportConfig currentConfig : mNoEmsSupportConfigList) {
                if (simOperator.startsWith(currentConfig.mOperatorNumber) && (TextUtils.isEmpty(currentConfig.mGid1) || (!TextUtils.isEmpty(currentConfig.mGid1) && currentConfig.mGid1.equalsIgnoreCase(gid)))) {
                    return currentConfig.mIsPrefix;
                }
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private static class NoEmsSupportConfig {
        String mGid1;
        boolean mIsPrefix;
        String mOperatorNumber;

        public NoEmsSupportConfig(String[] config) {
            this.mOperatorNumber = config[0];
            this.mIsPrefix = "prefix".equals(config[1]);
            this.mGid1 = config.length > 2 ? config[2] : null;
        }

        public String toString() {
            return "NoEmsSupportConfig { mOperatorNumber = " + this.mOperatorNumber + ", mIsPrefix = " + this.mIsPrefix + ", mGid1 = " + this.mGid1 + " }";
        }
    }

    private static boolean isNoEmsSupportConfigListExisted() {
        Resources r;
        if (!mIsNoEmsSupportConfigListLoaded && (r = Resources.getSystem()) != null) {
            String[] listArray = r.getStringArray(R.array.config_defaultNotificationVibePattern);
            if (listArray != null && listArray.length > 0) {
                mNoEmsSupportConfigList = new NoEmsSupportConfig[listArray.length];
                for (int i = 0; i < listArray.length; i++) {
                    mNoEmsSupportConfigList[i] = new NoEmsSupportConfig(listArray[i].split(";"));
                }
            }
            mIsNoEmsSupportConfigListLoaded = true;
        }
        return (mNoEmsSupportConfigList == null || mNoEmsSupportConfigList.length == 0) ? false : true;
    }

    private static final SmsMessageBase getSmsFacility() {
        if (isCdmaVoice()) {
            return new com.android.internal.telephony.cdma.SmsMessage();
        }
        return new com.android.internal.telephony.gsm.SmsMessage();
    }

    public SmsMessage() {
        this(getSmsFacility());
    }

    public static SmsMessage newFromCDS(String line) {
        SmsMessageBase wrappedMessage = com.android.internal.telephony.gsm.SmsMessage.newFromCDS(line);
        return new SmsMessage(wrappedMessage);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header) {
        SmsMessageBase.SubmitPduBase spb;
        if (useCdmaFormatForMoSms()) {
            spb = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, SmsHeader.fromByteArray(header));
        } else {
            spb = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, header);
        }
        return new SubmitPdu(spb);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, short destinationPort, short originalPort, byte[] data, boolean statusReportRequested) {
        SmsMessageBase.SubmitPduBase spb;
        Rlog.d(LOG_TAG, "[xj android.telephony.SmsMessage getSubmitPdu");
        if (useCdmaFormatForMoSms()) {
            spb = com.android.internal.telephony.cdma.SmsMessage.getSubmitPdu(scAddress, destinationAddress, destinationPort, data, statusReportRequested);
        } else {
            spb = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu(scAddress, destinationAddress, destinationPort, originalPort, data, statusReportRequested);
        }
        if (spb != null) {
            return new SubmitPdu(spb);
        }
        return null;
    }

    public String getDestinationAddress() {
        return this.mWrappedSmsMessage.getDestinationAddress();
    }

    public SmsHeader getUserDataHeader() {
        return this.mWrappedSmsMessage.getUserDataHeader();
    }

    public byte[] getSmsc() {
        Rlog.d(LOG_TAG, "getSmsc");
        byte[] pdu = getPdu();
        if (isCdma()) {
            Rlog.d(LOG_TAG, "getSmsc with CDMA and return null");
            return null;
        }
        if (pdu == null) {
            Rlog.d(LOG_TAG, "pdu is null");
            return null;
        }
        int smscLen = (pdu[0] & 255) + 1;
        byte[] smsc = new byte[smscLen];
        try {
            System.arraycopy(pdu, 0, smsc, 0, smsc.length);
            return smsc;
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "Out of boudns");
            return null;
        }
    }

    public byte[] getTpdu() {
        Rlog.d(LOG_TAG, "getTpdu");
        byte[] pdu = getPdu();
        if (isCdma()) {
            Rlog.d(LOG_TAG, "getSmsc with CDMA and return null");
            return pdu;
        }
        if (pdu == null) {
            Rlog.d(LOG_TAG, "pdu is null");
            return null;
        }
        int smscLen = (pdu[0] & 255) + 1;
        int tpduLen = pdu.length - smscLen;
        byte[] tpdu = new byte[tpduLen];
        try {
            System.arraycopy(pdu, smscLen, tpdu, 0, tpdu.length);
            return tpdu;
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "Out of boudns");
            return null;
        }
    }

    public static int[] calculateLength(CharSequence msgBody, boolean use7bitOnly, int encodingType) {
        GsmAlphabet.TextEncodingDetails ted;
        if (useCdmaFormatForMoSms()) {
            ted = com.android.internal.telephony.cdma.SmsMessage.calculateLength(msgBody, use7bitOnly, encodingType);
        } else {
            ted = com.android.internal.telephony.gsm.SmsMessage.calculateLength(msgBody, use7bitOnly, encodingType);
        }
        int[] ret = {ted.msgCount, ted.codeUnitCount, ted.codeUnitsRemaining, ted.codeUnitSize};
        return ret;
    }

    public static ArrayList<String> fragmentText(String text, int encodingType) {
        GsmAlphabet.TextEncodingDetails ted;
        int limit;
        int nextPos;
        int udhLength;
        TelephonyManager.getDefault().getPhoneType();
        if (useCdmaFormatForMoSms()) {
            ted = com.android.internal.telephony.cdma.SmsMessage.calculateLength((CharSequence) text, false, encodingType);
        } else {
            ted = com.android.internal.telephony.gsm.SmsMessage.calculateLength(text, false, encodingType);
        }
        if (ted.codeUnitSize == 1) {
            if (ted.languageTable != 0 && ted.languageShiftTable != 0) {
                udhLength = 7;
            } else if (ted.languageTable != 0 || ted.languageShiftTable != 0) {
                udhLength = 4;
            } else {
                udhLength = 0;
            }
            if (ted.msgCount > 1) {
                udhLength += 6;
            }
            if (udhLength != 0) {
                udhLength++;
            }
            limit = 160 - udhLength;
        } else if (ted.msgCount > 1) {
            limit = 134;
            if (!hasEmsSupport() && ted.msgCount < 10) {
                limit = 132;
            }
        } else {
            limit = 140;
        }
        String newMsgBody = null;
        Resources r = Resources.getSystem();
        if (r.getBoolean(R.^attr-private.modifier)) {
            newMsgBody = Sms7BitEncodingTranslator.translate(text);
        }
        if (TextUtils.isEmpty(newMsgBody)) {
            newMsgBody = text;
        }
        int pos = 0;
        int textLen = newMsgBody.length();
        ArrayList<String> result = new ArrayList<>(ted.msgCount);
        while (pos < textLen) {
            if (ted.codeUnitSize == 1) {
                if (useCdmaFormatForMoSms() && ted.msgCount == 1) {
                    nextPos = pos + Math.min(limit, textLen - pos);
                } else {
                    nextPos = GsmAlphabet.findGsmSeptetLimitIndex(newMsgBody, pos, limit, ted.languageTable, ted.languageShiftTable);
                }
            } else {
                nextPos = SmsMessageBase.findNextUnicodePosition(pos, limit, newMsgBody);
            }
            if (nextPos <= pos || nextPos > textLen) {
                Rlog.e(LOG_TAG, "fragmentText failed (" + pos + " >= " + nextPos + " or " + nextPos + " >= " + textLen + ")");
                break;
            }
            result.add(newMsgBody.substring(pos, nextPos));
            pos = nextPos;
        }
        return result;
    }

    public ArrayList<String> fragmentTextUsingTed(int subId, String text, GsmAlphabet.TextEncodingDetails ted) {
        boolean useCdmaFormat;
        int limit;
        int nextPos;
        int udhLength;
        if (!SmsManager.getSmsManagerForSubscriptionId(subId).isImsSmsSupported()) {
            useCdmaFormat = TelephonyManager.getDefault().getCurrentPhoneType() == 2;
        } else {
            useCdmaFormat = FORMAT_3GPP2.equals(SmsManager.getSmsManagerForSubscriptionId(subId).getImsSmsFormat());
        }
        if (ted.codeUnitSize == 1) {
            if (ted.languageTable != 0 && ted.languageShiftTable != 0) {
                udhLength = 7;
            } else if (ted.languageTable != 0 || ted.languageShiftTable != 0) {
                udhLength = 4;
            } else {
                udhLength = 0;
            }
            if (ted.msgCount > 1) {
                udhLength += 6;
            }
            if (udhLength != 0) {
                udhLength++;
            }
            limit = 160 - udhLength;
        } else if (ted.msgCount > 1) {
            limit = 134;
            if (!hasEmsSupport() && ted.msgCount < 10) {
                limit = 132;
            }
        } else {
            limit = 140;
        }
        String newMsgBody = null;
        Resources r = Resources.getSystem();
        if (r.getBoolean(R.^attr-private.modifier)) {
            newMsgBody = Sms7BitEncodingTranslator.translate(text);
        }
        if (TextUtils.isEmpty(newMsgBody)) {
            newMsgBody = text;
        }
        int pos = 0;
        int textLen = newMsgBody.length();
        ArrayList<String> result = new ArrayList<>(ted.msgCount);
        while (pos < textLen) {
            if (ted.codeUnitSize == 1) {
                if (useCdmaFormat && ted.msgCount == 1) {
                    nextPos = pos + Math.min(limit, textLen - pos);
                } else {
                    nextPos = GsmAlphabet.findGsmSeptetLimitIndex(newMsgBody, pos, limit, ted.languageTable, ted.languageShiftTable);
                }
            } else {
                nextPos = SmsMessageBase.findNextUnicodePosition(pos, limit, newMsgBody);
            }
            if (nextPos <= pos || nextPos > textLen) {
                Rlog.e(LOG_TAG, "fragmentText failed (" + pos + " >= " + nextPos + " or " + nextPos + " >= " + textLen + ")");
                break;
            }
            result.add(newMsgBody.substring(pos, nextPos));
            pos = nextPos;
        }
        return result;
    }

    public static SmsMessage createFromEfRecord(int index, byte[] data, String format) {
        SmsMessageBase wrappedMessage;
        Rlog.d(LOG_TAG, "createFromEfRecord(): format " + format);
        if (FORMAT_3GPP2.equals(format)) {
            wrappedMessage = com.android.internal.telephony.cdma.SmsMessage.createFromEfRecord(index, data);
        } else if (FORMAT_3GPP.equals(format)) {
            wrappedMessage = com.android.internal.telephony.gsm.SmsMessage.createFromEfRecord(index, data);
        } else {
            Rlog.e(LOG_TAG, "createFromEfRecord(): unsupported message format " + format);
            return null;
        }
        if (wrappedMessage != null) {
            return new SmsMessage(wrappedMessage);
        }
        return null;
    }

    private boolean isCdma() {
        int activePhone = TelephonyManager.getDefault().getCurrentPhoneType(this.mSubId);
        return 2 == activePhone;
    }

    public int getEncodingType() {
        return this.mWrappedSmsMessage.getEncodingType();
    }
}
