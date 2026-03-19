package com.android.internal.telephony.gsm;

import android.R;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.text.format.Time;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.Sms7BitEncodingTranslator;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.uicc.IccUtils;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.internal.telephony.ppl.PplControlData;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class SmsMessage extends SmsMessageBase {
    public static final int ENCODING_7BIT_LOCKING = 12;
    public static final int ENCODING_7BIT_LOCKING_SINGLE = 13;
    public static final int ENCODING_7BIT_SINGLE = 11;
    static final String LOG_TAG = "SmsMessage";
    public static final int MASK_MESSAGE_TYPE_INDICATOR = 3;
    public static final int MASK_USER_DATA_HEADER_INDICATOR = 64;
    public static final int MASK_VALIDITY_PERIOD_FORMAT = 24;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_ABSOLUTE = 24;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_ENHANCED = 8;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_NONE = 0;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_RELATIVE = 16;
    private static final boolean VDBG = false;
    private int mDataCodingScheme;
    private int mMti;
    private int mProtocolIdentifier;
    private GsmSmsAddress mRecipientAddress;
    private int mStatus;
    private SmsConstants.MessageClass messageClass;
    private boolean mReplyPathPresent = false;
    private boolean mIsStatusReportMessage = false;
    private int mVoiceMailCount = 0;
    private int mEncodingType = 0;

    public static class DeliverPdu extends SmsMessageBase.DeliverPduBase {
    }

    public static class SubmitPdu extends SmsMessageBase.SubmitPduBase {
    }

    public static SmsMessage createFromPdu(byte[] pdu) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(pdu);
            return msg;
        } catch (OutOfMemoryError e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed with out of memory: ", e);
            return null;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public boolean isTypeZero() {
        return this.mProtocolIdentifier == 64;
    }

    public static SmsMessage newFromCMT(String[] lines) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(IccUtils.hexStringToBytes(lines[1]));
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public static SmsMessage newFromCDS(String line) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(IccUtils.hexStringToBytes(line));
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "CDS SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public static SmsMessage createFromEfRecord(int index, byte[] data) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.mIndexOnIcc = index;
            if ((data[0] & 1) == 0) {
                Rlog.w(LOG_TAG, "SMS parsing failed: Trying to parse a free record");
                return null;
            }
            msg.mStatusOnIcc = data[0] & 7;
            int size = data.length - 1;
            byte[] pdu = new byte[size];
            System.arraycopy(data, 1, pdu, 0, size);
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    public static int getTPLayerLengthForPDU(String pdu) {
        int len = pdu.length() / 2;
        int smscLen = Integer.parseInt(pdu.substring(0, 2), 16);
        return (len - smscLen) - 1;
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, header, 0, 0, 0);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header, int encoding, int languageTable, int languageShiftTable) {
        byte[] userData;
        if (message == null || destinationAddress == null) {
            return null;
        }
        if (encoding == 0) {
            GsmAlphabet.TextEncodingDetails ted = calculateLength(message, false);
            encoding = ted.codeUnitSize;
            languageTable = ted.languageTable;
            languageShiftTable = ted.languageShiftTable;
            if (encoding == 1 && (languageTable != 0 || languageShiftTable != 0)) {
                if (header != null) {
                    SmsHeader smsHeader = SmsHeader.fromByteArray(header);
                    if (smsHeader.languageTable != languageTable || smsHeader.languageShiftTable != languageShiftTable) {
                        Rlog.w(LOG_TAG, "Updating language table in SMS header: " + smsHeader.languageTable + " -> " + languageTable + ", " + smsHeader.languageShiftTable + " -> " + languageShiftTable);
                        smsHeader.languageTable = languageTable;
                        smsHeader.languageShiftTable = languageShiftTable;
                        header = SmsHeader.toByteArray(smsHeader);
                    }
                } else {
                    SmsHeader smsHeader2 = new SmsHeader();
                    smsHeader2.languageTable = languageTable;
                    smsHeader2.languageShiftTable = languageShiftTable;
                    header = SmsHeader.toByteArray(smsHeader2);
                }
            }
        }
        SubmitPdu ret = new SubmitPdu();
        byte mtiByte = (byte) ((header != null ? 64 : 0) | 1);
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, destinationAddress, mtiByte, statusReportRequested, ret);
        try {
            if (encoding == 1) {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, languageTable, languageShiftTable);
            } else {
                try {
                    userData = encodeUCS2(message, header);
                } catch (UnsupportedEncodingException uex) {
                    Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex);
                    return null;
                }
            }
        } catch (EncodeException e) {
            try {
                userData = encodeUCS2(message, header);
                encoding = 3;
            } catch (UnsupportedEncodingException uex2) {
                Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex2);
                return null;
            }
        }
        if (encoding == 1) {
            if ((userData[0] & PplMessageManager.Type.INVALID) > 160) {
                Rlog.e(LOG_TAG, "Message too long (" + (userData[0] & PplMessageManager.Type.INVALID) + " septets)");
                return null;
            }
            bo.write(0);
        } else {
            if ((userData[0] & PplMessageManager.Type.INVALID) > 140) {
                Rlog.e(LOG_TAG, "Message too long (" + (userData[0] & PplMessageManager.Type.INVALID) + " bytes)");
                return null;
            }
            bo.write(8);
        }
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    private static byte[] encodeUCS2(String message, byte[] header) throws UnsupportedEncodingException {
        byte[] userData;
        byte[] textPart = message.getBytes("utf-16be");
        if (header != null) {
            userData = new byte[header.length + textPart.length + 1];
            userData[0] = (byte) header.length;
            System.arraycopy(header, 0, userData, 1, header.length);
            System.arraycopy(textPart, 0, userData, header.length + 1, textPart.length);
        } else {
            userData = textPart;
        }
        byte[] ret = new byte[userData.length + 1];
        ret[0] = (byte) (userData.length & 255);
        System.arraycopy(userData, 0, ret, 1, userData.length);
        return ret;
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, (byte[]) null);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, int destinationPort, byte[] data, boolean statusReportRequested) {
        SmsHeader.PortAddrs portAddrs = new SmsHeader.PortAddrs();
        portAddrs.destPort = destinationPort;
        portAddrs.origPort = 0;
        portAddrs.areEightBits = false;
        SmsHeader smsHeader = new SmsHeader();
        smsHeader.portAddrs = portAddrs;
        byte[] smsHeaderData = SmsHeader.toByteArray(smsHeader);
        if (data.length + smsHeaderData.length + 1 > 140) {
            Rlog.e(LOG_TAG, "SMS data message may only contain " + ((140 - smsHeaderData.length) - 1) + " bytes");
            return null;
        }
        SubmitPdu ret = new SubmitPdu();
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, destinationAddress, (byte) 65, statusReportRequested, ret);
        bo.write(4);
        bo.write(data.length + smsHeaderData.length + 1);
        bo.write(smsHeaderData.length);
        bo.write(smsHeaderData, 0, smsHeaderData.length);
        bo.write(data, 0, data.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    private static ByteArrayOutputStream getSubmitPduHead(String scAddress, String destinationAddress, byte mtiByte, boolean statusReportRequested, SubmitPdu ret) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(PduHeaders.RECOMMENDED_RETRIEVAL_MODE);
        if (scAddress == null) {
            ret.encodedScAddress = null;
        } else {
            ret.encodedScAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(scAddress);
        }
        if (statusReportRequested) {
            mtiByte = (byte) (mtiByte | 32);
        }
        bo.write(mtiByte);
        bo.write(0);
        byte[] daBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(destinationAddress);
        if (daBytes != null) {
            bo.write(((daBytes.length - 1) * 2) - ((daBytes[daBytes.length + (-1)] & 240) == 240 ? 1 : 0));
            bo.write(daBytes, 0, daBytes.length);
        } else {
            Rlog.d(LOG_TAG, "write an empty address for submit pdu");
            bo.write(0);
            bo.write(129);
        }
        bo.write(0);
        return bo;
    }

    private static class PduParser {
        byte[] mPdu;
        byte[] mUserData;
        SmsHeader mUserDataHeader;
        int mCur = 0;
        int mUserDataSeptetPadding = 0;

        PduParser(byte[] pdu) {
            this.mPdu = pdu;
        }

        String getSCAddress() {
            String strCalledPartyBCDToString;
            int len = getByte();
            if (len == 0) {
                strCalledPartyBCDToString = null;
            } else {
                try {
                    strCalledPartyBCDToString = PhoneNumberUtils.calledPartyBCDToString(this.mPdu, this.mCur, len);
                } catch (RuntimeException tr) {
                    Rlog.d(SmsMessage.LOG_TAG, "invalid SC address: ", tr);
                    strCalledPartyBCDToString = null;
                }
            }
            this.mCur += len;
            return strCalledPartyBCDToString;
        }

        int getByte() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            return bArr[i] & PplMessageManager.Type.INVALID;
        }

        GsmSmsAddress getAddress() {
            int addressLength = this.mPdu[this.mCur] & PplMessageManager.Type.INVALID;
            int lengthBytes = ((addressLength + 1) / 2) + 2;
            try {
                GsmSmsAddress ret = new GsmSmsAddress(this.mPdu, this.mCur, lengthBytes);
                this.mCur += lengthBytes;
                return ret;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        long getSCTimestampMillis() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            int year = IccUtils.gsmBcdByteToInt(bArr[i]);
            byte[] bArr2 = this.mPdu;
            int i2 = this.mCur;
            this.mCur = i2 + 1;
            int month = IccUtils.gsmBcdByteToInt(bArr2[i2]);
            byte[] bArr3 = this.mPdu;
            int i3 = this.mCur;
            this.mCur = i3 + 1;
            int day = IccUtils.gsmBcdByteToInt(bArr3[i3]);
            byte[] bArr4 = this.mPdu;
            int i4 = this.mCur;
            this.mCur = i4 + 1;
            int hour = IccUtils.gsmBcdByteToInt(bArr4[i4]);
            byte[] bArr5 = this.mPdu;
            int i5 = this.mCur;
            this.mCur = i5 + 1;
            int minute = IccUtils.gsmBcdByteToInt(bArr5[i5]);
            byte[] bArr6 = this.mPdu;
            int i6 = this.mCur;
            this.mCur = i6 + 1;
            int second = IccUtils.gsmBcdByteToInt(bArr6[i6]);
            byte[] bArr7 = this.mPdu;
            int i7 = this.mCur;
            this.mCur = i7 + 1;
            byte tzByte = bArr7[i7];
            int timezoneOffset = IccUtils.gsmBcdByteToInt((byte) (tzByte & (-9)));
            if ((tzByte & 8) != 0) {
                timezoneOffset = -timezoneOffset;
            }
            Time time = new Time("UTC");
            time.year = year >= 90 ? year + 1900 : year + ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT;
            time.month = month - 1;
            time.monthDay = day;
            time.hour = hour;
            time.minute = minute;
            time.second = second;
            return time.toMillis(true) - ((long) (((timezoneOffset * 15) * 60) * 1000));
        }

        int constructUserData(boolean hasUserDataHeader, boolean dataInSeptets) {
            int offset;
            int bufferLen;
            int offset2 = this.mCur;
            int offset3 = offset2 + 1;
            int userDataLength = this.mPdu[offset2] & 255;
            int headerSeptets = 0;
            int userDataHeaderLength = 0;
            if (hasUserDataHeader) {
                int offset4 = offset3 + 1;
                userDataHeaderLength = this.mPdu[offset3] & 255;
                byte[] udh = new byte[userDataHeaderLength];
                System.arraycopy(this.mPdu, offset4, udh, 0, userDataHeaderLength);
                this.mUserDataHeader = SmsHeader.fromByteArray(udh);
                offset = offset4 + userDataHeaderLength;
                int headerBits = (userDataHeaderLength + 1) * 8;
                int headerSeptets2 = headerBits / 7;
                headerSeptets = headerSeptets2 + (headerBits % 7 > 0 ? 1 : 0);
                this.mUserDataSeptetPadding = (headerSeptets * 7) - headerBits;
            } else {
                offset = offset3;
            }
            if (dataInSeptets) {
                bufferLen = this.mPdu.length - offset;
            } else {
                bufferLen = userDataLength - (hasUserDataHeader ? userDataHeaderLength + 1 : 0);
                if (bufferLen < 0) {
                    bufferLen = 0;
                }
            }
            this.mUserData = new byte[bufferLen];
            System.arraycopy(this.mPdu, offset, this.mUserData, 0, this.mUserData.length);
            this.mCur = offset;
            if (dataInSeptets) {
                int count = userDataLength - headerSeptets;
                if (count < 0) {
                    return 0;
                }
                return count;
            }
            return this.mUserData.length;
        }

        byte[] getUserData() {
            return this.mUserData;
        }

        SmsHeader getUserDataHeader() {
            return this.mUserDataHeader;
        }

        String getUserDataGSM7Bit(int septetCount, int languageTable, int languageShiftTable) {
            String ret = GsmAlphabet.gsm7BitPackedToString(this.mPdu, this.mCur, septetCount, this.mUserDataSeptetPadding, languageTable, languageShiftTable);
            this.mCur += (septetCount * 7) / 8;
            return ret;
        }

        String getUserDataGSM8bit(int byteCount) {
            String ret = GsmAlphabet.gsm8BitUnpackedToString(this.mPdu, this.mCur, byteCount);
            this.mCur += byteCount;
            return ret;
        }

        String getUserDataUCS2(int byteCount) {
            String ret;
            try {
                ret = new String(this.mPdu, this.mCur, byteCount, CharacterSets.MIMENAME_UTF_16);
            } catch (UnsupportedEncodingException ex) {
                ret = UsimPBMemInfo.STRING_NOT_SET;
                Rlog.e(SmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }
            this.mCur += byteCount;
            return ret;
        }

        String getUserDataKSC5601(int byteCount) {
            String ret;
            try {
                ret = new String(this.mPdu, this.mCur, byteCount, "KSC5601");
            } catch (UnsupportedEncodingException ex) {
                ret = UsimPBMemInfo.STRING_NOT_SET;
                Rlog.e(SmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }
            this.mCur += byteCount;
            return ret;
        }

        boolean moreDataPresent() {
            return this.mPdu.length > this.mCur;
        }
    }

    public static GsmAlphabet.TextEncodingDetails calculateLength(CharSequence msgBody, boolean use7bitOnly) {
        CharSequence newMsgBody = null;
        Resources r = Resources.getSystem();
        if (r.getBoolean(R.^attr-private.modifier)) {
            newMsgBody = Sms7BitEncodingTranslator.translate(msgBody);
        }
        if (TextUtils.isEmpty(newMsgBody)) {
            newMsgBody = msgBody;
        }
        GsmAlphabet.TextEncodingDetails ted = GsmAlphabet.countGsmSeptets(newMsgBody, use7bitOnly);
        if (ted == null) {
            return SmsMessageBase.calcUnicodeEncodingDetails(newMsgBody);
        }
        return ted;
    }

    @Override
    public int getProtocolIdentifier() {
        return this.mProtocolIdentifier;
    }

    int getDataCodingScheme() {
        return this.mDataCodingScheme;
    }

    @Override
    public boolean isReplace() {
        return (this.mProtocolIdentifier & 192) == 64 && (this.mProtocolIdentifier & 63) > 0 && (this.mProtocolIdentifier & 63) < 8;
    }

    @Override
    public boolean isCphsMwiMessage() {
        if (((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageClear()) {
            return true;
        }
        return ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet();
    }

    @Override
    public boolean isMWIClearMessage() {
        if (this.mIsMwi && !this.mMwiSense) {
            return true;
        }
        if (this.mOriginatingAddress == null) {
            return false;
        }
        return ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageClear();
    }

    @Override
    public boolean isMWISetMessage() {
        if (this.mIsMwi && this.mMwiSense) {
            return true;
        }
        if (this.mOriginatingAddress != null) {
            return ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet();
        }
        return false;
    }

    @Override
    public boolean isMwiDontStore() {
        if (this.mIsMwi && this.mMwiDontStore) {
            return true;
        }
        return isCphsMwiMessage() && " ".equals(getMessageBody());
    }

    @Override
    public int getStatus() {
        return this.mStatus;
    }

    @Override
    public boolean isStatusReportMessage() {
        return this.mIsStatusReportMessage;
    }

    @Override
    public boolean isReplyPathPresent() {
        return this.mReplyPathPresent;
    }

    private void parsePdu(byte[] pdu) {
        this.mPdu = pdu;
        PduParser p = new PduParser(pdu);
        this.mScAddress = p.getSCAddress();
        if (this.mScAddress != null) {
        }
        int firstByte = p.getByte();
        this.mMti = firstByte & 3;
        switch (this.mMti) {
            case 0:
            case 3:
                parseSmsDeliver(p, firstByte);
                return;
            case 1:
                parseSmsSubmit(p, firstByte);
                return;
            case 2:
                parseSmsStatusReport(p, firstByte);
                return;
            default:
                throw new RuntimeException("Unsupported message type");
        }
    }

    private void parseSmsStatusReport(PduParser p, int firstByte) {
        this.mIsStatusReportMessage = true;
        this.mMessageRef = p.getByte();
        this.mRecipientAddress = p.getAddress();
        this.mScTimeMillis = p.getSCTimestampMillis();
        p.getSCTimestampMillis();
        this.mStatus = p.getByte();
        this.mMessageBody = UsimPBMemInfo.STRING_NOT_SET;
        if (!p.moreDataPresent()) {
            return;
        }
        int extraParams = p.getByte();
        int moreExtraParams = extraParams;
        while ((moreExtraParams & 128) != 0 && p.moreDataPresent()) {
            moreExtraParams = p.getByte();
        }
        if ((extraParams & 120) != 0) {
            return;
        }
        if ((extraParams & 1) != 0) {
            this.mProtocolIdentifier = p.getByte();
        }
        if ((extraParams & 2) != 0) {
            this.mDataCodingScheme = p.getByte();
        }
        if ((extraParams & 4) == 0) {
            return;
        }
        boolean hasUserDataHeader = (firstByte & 64) == 64;
        parseUserData(p, hasUserDataHeader);
    }

    private void parseSmsDeliver(PduParser p, int firstByte) {
        this.mReplyPathPresent = (firstByte & 128) == 128;
        this.mOriginatingAddress = p.getAddress();
        if (this.mOriginatingAddress != null) {
        }
        this.mProtocolIdentifier = p.getByte();
        this.mDataCodingScheme = p.getByte();
        this.mScTimeMillis = p.getSCTimestampMillis();
        boolean hasUserDataHeader = (firstByte & 64) == 64;
        parseUserData(p, hasUserDataHeader);
    }

    private void parseSmsSubmit(PduParser p, int firstByte) {
        int validityPeriodLength;
        this.mReplyPathPresent = (firstByte & 128) == 128;
        this.mMessageRef = p.getByte();
        this.mRecipientAddress = p.getAddress();
        this.destinationAddress = this.mRecipientAddress;
        if (this.mRecipientAddress != null) {
        }
        this.mProtocolIdentifier = p.getByte();
        this.mDataCodingScheme = p.getByte();
        int validityPeriodFormat = (firstByte >> 3) & 3;
        if (validityPeriodFormat == 0) {
            validityPeriodLength = 0;
        } else if (2 == validityPeriodFormat) {
            validityPeriodLength = 1;
        } else {
            validityPeriodLength = 7;
        }
        while (true) {
            int validityPeriodLength2 = validityPeriodLength;
            validityPeriodLength = validityPeriodLength2 - 1;
            if (validityPeriodLength2 <= 0) {
                break;
            } else {
                p.getByte();
            }
        }
        boolean hasUserDataHeader = (firstByte & 64) == 64;
        parseUserData(p, hasUserDataHeader);
    }

    private void parseUserData(PduParser p, boolean hasUserDataHeader) {
        boolean hasMessageClass = false;
        int encodingType = 0;
        if ((this.mDataCodingScheme & 128) == 0) {
            boolean userDataCompressed = (this.mDataCodingScheme & 32) != 0;
            hasMessageClass = (this.mDataCodingScheme & 16) != 0;
            if (!userDataCompressed) {
                switch ((this.mDataCodingScheme >> 2) & 3) {
                    case 0:
                        encodingType = 1;
                        break;
                    case 1:
                        Resources r = Resources.getSystem();
                        if (r.getBoolean(R.^attr-private.maxCollapsedHeightSmall)) {
                            encodingType = 2;
                        } else {
                            Rlog.w(LOG_TAG, "1 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
                            encodingType = 2;
                        }
                        break;
                    case 2:
                        encodingType = 3;
                        break;
                }
            } else {
                Rlog.w(LOG_TAG, "4 - Unsupported SMS data coding scheme (compression) " + (this.mDataCodingScheme & 255));
            }
        } else if ((this.mDataCodingScheme & CallFailCause.CALL_BARRED) == 240) {
            hasMessageClass = true;
            encodingType = (this.mDataCodingScheme & 4) == 0 ? 1 : 2;
        } else if ((this.mDataCodingScheme & CallFailCause.CALL_BARRED) == 192 || (this.mDataCodingScheme & CallFailCause.CALL_BARRED) == 208 || (this.mDataCodingScheme & CallFailCause.CALL_BARRED) == 224) {
            encodingType = (this.mDataCodingScheme & CallFailCause.CALL_BARRED) == 224 ? 3 : 1;
            boolean active = (this.mDataCodingScheme & 8) == 8;
            if ((this.mDataCodingScheme & 3) == 0) {
                this.mIsMwi = true;
                this.mMwiSense = active;
                this.mMwiDontStore = (this.mDataCodingScheme & CallFailCause.CALL_BARRED) == 192;
                if (active) {
                    this.mVoiceMailCount = -1;
                } else {
                    this.mVoiceMailCount = 0;
                }
                Rlog.w(LOG_TAG, "MWI in DCS for Vmail. DCS = " + (this.mDataCodingScheme & 255) + " Dont store = " + this.mMwiDontStore + " vmail count = " + this.mVoiceMailCount);
            } else {
                this.mIsMwi = false;
                Rlog.w(LOG_TAG, "MWI in DCS for fax/email/other: " + (this.mDataCodingScheme & 255));
            }
        } else if ((this.mDataCodingScheme & 192) != 128) {
            Rlog.w(LOG_TAG, "3 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
        } else if (this.mDataCodingScheme == 132) {
            encodingType = 4;
        } else {
            Rlog.w(LOG_TAG, "5 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
        }
        int count = p.constructUserData(hasUserDataHeader, encodingType == 1);
        this.mUserData = p.getUserData();
        this.mUserDataHeader = p.getUserDataHeader();
        this.mEncodingType = encodingType;
        if (hasUserDataHeader && this.mUserDataHeader.specialSmsMsgList.size() != 0) {
            for (SmsHeader.SpecialSmsMsg msg : this.mUserDataHeader.specialSmsMsgList) {
                int msgInd = msg.msgIndType & 255;
                if (msgInd == 0 || msgInd == 128) {
                    this.mIsMwi = true;
                    if (msgInd == 128) {
                        this.mMwiDontStore = false;
                    } else if (!this.mMwiDontStore && (((this.mDataCodingScheme & CallFailCause.CALL_BARRED) != 208 && (this.mDataCodingScheme & CallFailCause.CALL_BARRED) != 224) || (this.mDataCodingScheme & 3) != 0)) {
                        this.mMwiDontStore = true;
                    }
                    this.mVoiceMailCount = msg.msgCount & 255;
                    if (this.mVoiceMailCount > 0) {
                        this.mMwiSense = true;
                    } else {
                        this.mMwiSense = false;
                    }
                    Rlog.w(LOG_TAG, "MWI in TP-UDH for Vmail. Msg Ind = " + msgInd + " Dont store = " + this.mMwiDontStore + " Vmail count = " + this.mVoiceMailCount);
                } else {
                    Rlog.w(LOG_TAG, "TP_UDH fax/email/extended msg/multisubscriber profile. Msg Ind = " + msgInd);
                }
            }
        }
        switch (encodingType) {
            case 0:
                this.mMessageBody = null;
                break;
            case 1:
                this.mMessageBody = p.getUserDataGSM7Bit(count, hasUserDataHeader ? this.mUserDataHeader.languageTable : 0, hasUserDataHeader ? this.mUserDataHeader.languageShiftTable : 0);
                break;
            case 2:
                Resources r2 = Resources.getSystem();
                if (r2.getBoolean(R.^attr-private.maxCollapsedHeightSmall)) {
                    this.mMessageBody = p.getUserDataGSM8bit(count);
                } else {
                    this.mMessageBody = null;
                }
                break;
            case 3:
                this.mMessageBody = p.getUserDataUCS2(count);
                break;
            case 4:
                this.mMessageBody = p.getUserDataKSC5601(count);
                break;
        }
        if (this.mMessageBody != null) {
            parseMessageBody();
        }
        if (!hasMessageClass) {
            this.messageClass = SmsConstants.MessageClass.UNKNOWN;
        }
        switch (this.mDataCodingScheme & 3) {
            case 0:
                this.messageClass = SmsConstants.MessageClass.CLASS_0;
                break;
            case 1:
                this.messageClass = SmsConstants.MessageClass.CLASS_1;
                break;
            case 2:
                this.messageClass = SmsConstants.MessageClass.CLASS_2;
                break;
            case 3:
                this.messageClass = SmsConstants.MessageClass.CLASS_3;
                break;
        }
    }

    @Override
    public SmsConstants.MessageClass getMessageClass() {
        return this.messageClass;
    }

    boolean isUsimDataDownload() {
        if (this.messageClass == SmsConstants.MessageClass.CLASS_2) {
            return this.mProtocolIdentifier == 127 || this.mProtocolIdentifier == 124;
        }
        return false;
    }

    public int getNumOfVoicemails() {
        if (!this.mIsMwi && isCphsMwiMessage()) {
            if (this.mOriginatingAddress != null && ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet()) {
                this.mVoiceMailCount = 255;
            } else {
                this.mVoiceMailCount = 0;
            }
            Rlog.v(LOG_TAG, "CPHS voice mail message");
        }
        return this.mVoiceMailCount;
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, int destinationPort, int originalPort, byte[] data, boolean statusReportRequested) {
        byte[] smsHeaderData = SmsHeader.getSubmitPduHeader(destinationPort, originalPort);
        if (smsHeaderData == null) {
            return null;
        }
        return getSubmitPdu(scAddress, destinationAddress, data, smsHeaderData, statusReportRequested);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, int destPort, boolean statusReportRequested) {
        int encoding;
        int language = getCurrentSysLanguage();
        int singleId = -1;
        int lockingId = -1;
        GsmAlphabet.TextEncodingDetails ted = new GsmAlphabet.TextEncodingDetails();
        if (encodeStringWithSpecialLang(message, language, ted)) {
            if (ted.useLockingShift && ted.useSingleShift) {
                encoding = 13;
                lockingId = language;
                singleId = language;
            } else if (ted.useLockingShift) {
                encoding = 12;
                lockingId = language;
            } else if (ted.useSingleShift) {
                encoding = 11;
                singleId = language;
            } else {
                encoding = 1;
                language = -1;
            }
        } else {
            encoding = 3;
        }
        byte[] smsHeaderData = SmsHeader.getSubmitPduHeaderWithLang(destPort, singleId, lockingId);
        return getSubmitPduWithLang(scAddress, destinationAddress, message, statusReportRequested, smsHeaderData, encoding, language);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, byte[] data, byte[] smsHeaderData, boolean statusReportRequested) {
        if (data.length + smsHeaderData.length + 1 > 140) {
            Rlog.e(LOG_TAG, "SMS data message may only contain " + ((140 - smsHeaderData.length) - 1) + " bytes");
            return null;
        }
        SubmitPdu ret = new SubmitPdu();
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, destinationAddress, (byte) 65, statusReportRequested, ret);
        bo.write(4);
        bo.write(data.length + smsHeaderData.length + 1);
        bo.write(smsHeaderData.length);
        bo.write(smsHeaderData, 0, smsHeaderData.length);
        bo.write(data, 0, data.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    public static SubmitPdu getSubmitPduWithLang(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header, int encoding, int language) {
        byte[] userData;
        Rlog.d(LOG_TAG, "SmsMessage: get submit pdu");
        if (message == null || destinationAddress == null) {
            return null;
        }
        SubmitPdu ret = new SubmitPdu();
        Rlog.d(LOG_TAG, "SmsMessage: UDHI = " + (header != null));
        byte mtiByte = (byte) ((header != null ? 64 : 0) | 1);
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, destinationAddress, mtiByte, statusReportRequested, ret);
        if (encoding == 0) {
            encoding = 1;
        }
        try {
            Rlog.d(LOG_TAG, "Get SubmitPdu with Lang " + encoding + " " + language);
            if (encoding == 1) {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
            } else if (language > 0 && encoding != 3) {
                if (encoding == 12) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, language);
                } else if (encoding == 11) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, 0);
                } else if (encoding == 13) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, language);
                } else {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
                }
                encoding = 1;
            } else {
                try {
                    userData = encodeUCS2(message, header);
                } catch (UnsupportedEncodingException uex) {
                    Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex);
                    return null;
                }
            }
        } catch (EncodeException e) {
            try {
                userData = encodeUCS2(message, header);
                encoding = 3;
            } catch (UnsupportedEncodingException uex2) {
                Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex2);
                return null;
            }
        }
        if (encoding == 1) {
            if ((userData[0] & PplMessageManager.Type.INVALID) > 160) {
                return null;
            }
            bo.write(0);
        } else {
            if ((userData[0] & PplMessageManager.Type.INVALID) > 140) {
                return null;
            }
            bo.write(8);
        }
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    public static DeliverPdu getDeliverPduWithLang(String scAddress, String originalAddress, String message, byte[] header, long timestamp, int encoding, int language) {
        byte[] userData;
        Rlog.d(LOG_TAG, "SmsMessage: get deliver pdu");
        if (message == null || originalAddress == null) {
            return null;
        }
        DeliverPdu ret = new DeliverPdu();
        Rlog.d(LOG_TAG, "SmsMessage: UDHI = " + (header != null));
        byte mtiByte = (byte) ((header != null ? 64 : 0) | 0);
        ByteArrayOutputStream bo = getDeliverPduHead(scAddress, originalAddress, mtiByte, ret);
        if (encoding == 0) {
            encoding = 1;
        }
        try {
            Rlog.d(LOG_TAG, "Get SubmitPdu with Lang " + encoding + " " + language);
            if (encoding == 1) {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
            } else if (language > 0 && encoding != 3) {
                if (encoding == 12) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, language);
                } else if (encoding == 11) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, 0);
                } else if (encoding == 13) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, language);
                } else {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
                }
                encoding = 1;
            } else {
                try {
                    userData = encodeUCS2(message, header);
                } catch (UnsupportedEncodingException uex) {
                    Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex);
                    return null;
                }
            }
        } catch (EncodeException e) {
            try {
                userData = encodeUCS2(message, header);
                encoding = 3;
            } catch (UnsupportedEncodingException uex2) {
                Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex2);
                return null;
            }
        }
        if (userData != null && (userData[0] & PplMessageManager.Type.INVALID) > 160) {
            Rlog.d(LOG_TAG, "SmsMessage: message is too long");
            return null;
        }
        if (encoding == 1) {
            bo.write(0);
        } else {
            bo.write(8);
        }
        byte[] scts = parseSCTimestamp(timestamp);
        if (scts != null) {
            bo.write(scts, 0, scts.length);
        } else {
            for (int i = 0; i < 7; i++) {
                bo.write(0);
            }
        }
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    private static byte[] parseSCTimestamp(long millis) {
        Time t = new Time("UTC");
        t.set(millis);
        byte[] scts = {intToGsmBCDByte(t.year), intToGsmBCDByte(t.month + 1), intToGsmBCDByte(t.monthDay), intToGsmBCDByte(t.hour), intToGsmBCDByte(t.minute), intToGsmBCDByte(t.second), intToGsmBCDByte(0)};
        return scts;
    }

    private static byte intToGsmBCDByte(int value) {
        if (value < 0) {
            Rlog.d(LOG_TAG, "[time invalid value: " + value);
            return (byte) 0;
        }
        int value2 = value % 100;
        Rlog.d(LOG_TAG, "[time value: " + value2);
        byte b = (byte) (((value2 / 10) & 15) | (((value2 % 10) << 4) & CallFailCause.CALL_BARRED));
        Rlog.d(LOG_TAG, "[time bcd value: " + ((int) b));
        return b;
    }

    private static ByteArrayOutputStream getDeliverPduHead(String scAddress, String originalAddress, byte mtiByte, DeliverPdu ret) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(PduHeaders.RECOMMENDED_RETRIEVAL_MODE);
        if (scAddress == null) {
            ret.encodedScAddress = null;
        } else {
            ret.encodedScAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(scAddress);
        }
        bo.write(mtiByte);
        byte[] oaBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(originalAddress);
        if (oaBytes != null) {
            bo.write(((oaBytes.length - 1) * 2) - ((oaBytes[oaBytes.length + (-1)] & 240) == 240 ? 1 : 0));
            bo.write(oaBytes, 0, oaBytes.length);
        } else {
            Rlog.d(LOG_TAG, "write a empty address for deliver pdu");
            bo.write(0);
            bo.write(145);
        }
        bo.write(0);
        return bo;
    }

    private static boolean encodeStringWithSpecialLang(CharSequence msgBody, int language, GsmAlphabet.TextEncodingDetails ted) {
        int septets = GsmAlphabet.countGsmSeptetsUsingTables(msgBody, true, 0, 0);
        if (septets != -1) {
            ted.codeUnitCount = septets;
            if (septets > 160) {
                ted.msgCount = (septets / 153) + 1;
                ted.codeUnitsRemaining = 153 - (septets % 153);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = 160 - septets;
            }
            ted.codeUnitSize = 1;
            ted.shiftLangId = -1;
            Rlog.d(LOG_TAG, "Try Default: " + language + " " + ted);
            return true;
        }
        int septets2 = GsmAlphabet.countGsmSeptetsUsingTables(msgBody, true, 0, language);
        if (septets2 != -1) {
            int[] headerElt = {37, CallFailCause.ERROR_UNSPECIFIED};
            int maxLength = computeRemainUserDataLength(true, headerElt);
            ted.codeUnitCount = septets2;
            if (septets2 > maxLength) {
                headerElt[1] = 0;
                int maxLength2 = computeRemainUserDataLength(true, headerElt);
                ted.msgCount = (septets2 / maxLength2) + 1;
                ted.codeUnitsRemaining = maxLength2 - (septets2 % maxLength2);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = maxLength - septets2;
            }
            ted.codeUnitSize = 1;
            ted.useLockingShift = true;
            ted.shiftLangId = language;
            Rlog.d(LOG_TAG, "Try Locking Shift: " + language + " " + ted);
            return true;
        }
        int septets3 = GsmAlphabet.countGsmSeptetsUsingTables(msgBody, true, language, 0);
        if (septets3 != -1) {
            int[] headerElt2 = {36, CallFailCause.ERROR_UNSPECIFIED};
            int maxLength3 = computeRemainUserDataLength(true, headerElt2);
            ted.codeUnitCount = septets3;
            if (septets3 > maxLength3) {
                headerElt2[1] = 0;
                int maxLength4 = computeRemainUserDataLength(true, headerElt2);
                ted.msgCount = (septets3 / maxLength4) + 1;
                ted.codeUnitsRemaining = maxLength4 - (septets3 % maxLength4);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = maxLength3 - septets3;
            }
            ted.codeUnitSize = 1;
            ted.useSingleShift = true;
            ted.shiftLangId = language;
            Rlog.d(LOG_TAG, "Try Single Shift: " + language + " " + ted);
            return true;
        }
        int septets4 = GsmAlphabet.countGsmSeptetsUsingTables(msgBody, true, language, language);
        if (septets4 != -1) {
            int[] headerElt3 = {37, 36, CallFailCause.ERROR_UNSPECIFIED};
            int maxLength5 = computeRemainUserDataLength(true, headerElt3);
            ted.codeUnitCount = septets4;
            if (septets4 > maxLength5) {
                headerElt3[2] = 0;
                int maxLength6 = computeRemainUserDataLength(true, headerElt3);
                ted.msgCount = (septets4 / maxLength6) + 1;
                ted.codeUnitsRemaining = maxLength6 - (septets4 % maxLength6);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = maxLength5 - septets4;
            }
            ted.codeUnitSize = 1;
            ted.useLockingShift = true;
            ted.useSingleShift = true;
            ted.shiftLangId = language;
            Rlog.d(LOG_TAG, "Try Locking & Single Shift: " + language + " " + ted);
            return true;
        }
        Rlog.d(LOG_TAG, "Use UCS2" + language + " " + ted);
        return false;
    }

    private static int getCurrentSysLanguage() {
        String language = SystemProperties.get("persist.sys.language", (String) null);
        if (language == null) {
            language = SystemProperties.get("ro.product.locale.language", (String) null);
        }
        if (language.equals("tr")) {
            return -1;
        }
        return -1;
    }

    public static int computeRemainUserDataLength(boolean inSeptets, int[] headerElt) {
        int headerBytes = 0;
        for (int i : headerElt) {
            switch (i) {
                case 0:
                    headerBytes += 5;
                    break;
                case 36:
                    headerBytes += 3;
                    break;
                case 37:
                    headerBytes += 3;
                    break;
            }
        }
        if (headerBytes != 0) {
            headerBytes++;
        }
        int count = 140 - headerBytes;
        if (inSeptets) {
            return (count * 8) / 7;
        }
        return count;
    }

    public static GsmAlphabet.TextEncodingDetails calculateLength(CharSequence msgBody, boolean use7bitOnly, int encodingType) {
        CharSequence newMsgBody = null;
        Resources r = Resources.getSystem();
        if (r.getBoolean(R.^attr-private.modifier)) {
            newMsgBody = Sms7BitEncodingTranslator.translate(msgBody);
        }
        if (TextUtils.isEmpty(newMsgBody)) {
            newMsgBody = msgBody;
        }
        GsmAlphabet.TextEncodingDetails ted = GsmAlphabet.countGsmSeptets(newMsgBody, use7bitOnly);
        if (encodingType == 3) {
            Rlog.d(LOG_TAG, "input mode is unicode");
            ted = null;
        }
        if (ted == null) {
            Rlog.d(LOG_TAG, "7-bit encoding fail");
            return SmsMessageBase.calcUnicodeEncodingDetails(newMsgBody);
        }
        return ted;
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested, byte[] header, int encoding, int languageTable, int languageShiftTable, int validityPeriod) {
        byte[] userData;
        if (message == null || destinationAddress == null) {
            return null;
        }
        if (encoding == 0) {
            GsmAlphabet.TextEncodingDetails ted = calculateLength(message, false);
            encoding = ted.codeUnitSize;
            languageTable = ted.languageTable;
            languageShiftTable = ted.languageShiftTable;
            if (encoding == 1 && (languageTable != 0 || languageShiftTable != 0)) {
                if (header != null) {
                    SmsHeader smsHeader = SmsHeader.fromByteArray(header);
                    if (smsHeader.languageTable != languageTable || smsHeader.languageShiftTable != languageShiftTable) {
                        Rlog.w(LOG_TAG, "Updating language table in SMS header: " + smsHeader.languageTable + " -> " + languageTable + ", " + smsHeader.languageShiftTable + " -> " + languageShiftTable);
                        smsHeader.languageTable = languageTable;
                        smsHeader.languageShiftTable = languageShiftTable;
                        header = SmsHeader.toByteArray(smsHeader);
                    }
                } else {
                    SmsHeader smsHeader2 = new SmsHeader();
                    smsHeader2.languageTable = languageTable;
                    smsHeader2.languageShiftTable = languageShiftTable;
                    header = SmsHeader.toByteArray(smsHeader2);
                }
            }
        }
        SubmitPdu ret = new SubmitPdu();
        byte mtiByte = (byte) ((header != null ? 64 : 0) | 1);
        if (validityPeriod < 0 || validityPeriod > 255) {
            Rlog.d(LOG_TAG, "invalid VP: " + validityPeriod);
        } else {
            mtiByte = (byte) (mtiByte | PplControlData.STATUS_WIPE_REQUESTED);
        }
        ByteArrayOutputStream bo = getSubmitPduHead(scAddress, destinationAddress, mtiByte, statusReportRequested, ret);
        try {
            if (encoding == 1) {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, languageTable, languageShiftTable);
            } else {
                try {
                    userData = encodeUCS2(message, header);
                } catch (UnsupportedEncodingException uex) {
                    Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex);
                    return null;
                }
            }
        } catch (EncodeException e) {
            try {
                userData = encodeUCS2(message, header);
                encoding = 3;
            } catch (UnsupportedEncodingException uex2) {
                Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", uex2);
                return null;
            }
        }
        if (encoding == 1) {
            if ((userData[0] & PplMessageManager.Type.INVALID) > 160) {
                Rlog.e(LOG_TAG, "Message too long (" + (userData[0] & PplMessageManager.Type.INVALID) + " septets)");
                return null;
            }
            bo.write(0);
        } else {
            if ((userData[0] & PplMessageManager.Type.INVALID) > 140) {
                Rlog.e(LOG_TAG, "Message too long (" + (userData[0] & PplMessageManager.Type.INVALID) + " bytes)");
                return null;
            }
            bo.write(8);
        }
        if (validityPeriod >= 0 && validityPeriod <= 255) {
            Rlog.d(LOG_TAG, "write validity period into pdu: " + validityPeriod);
            bo.write(validityPeriod);
        }
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    @Override
    public int getEncodingType() {
        return this.mEncodingType;
    }
}
