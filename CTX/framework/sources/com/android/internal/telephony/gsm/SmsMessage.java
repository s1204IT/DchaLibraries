package com.android.internal.telephony.gsm;

import android.content.res.Resources;
import android.net.wifi.WifiEnterpriseConfig;
import android.telephony.PhoneNumberUtils;
import android.telephony.PreciseDisconnectCause;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.text.format.Time;
import com.android.internal.R;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.Sms7BitEncodingTranslator;
import com.android.internal.telephony.SmsAddress;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class SmsMessage extends SmsMessageBase {
    protected static final int INVALID_VALIDITY_PERIOD = -1;
    static final String LOG_TAG = "SmsMessage";
    protected static final int VALIDITY_PERIOD_FORMAT_ABSOLUTE = 3;
    protected static final int VALIDITY_PERIOD_FORMAT_ENHANCED = 1;
    protected static final int VALIDITY_PERIOD_FORMAT_NONE = 0;
    protected static final int VALIDITY_PERIOD_FORMAT_RELATIVE = 2;
    private static final int VALIDITY_PERIOD_MAX = 635040;
    private static final int VALIDITY_PERIOD_MIN = 5;
    private static final boolean VDBG = false;
    protected int mDataCodingScheme;
    private int mMti;
    private int mProtocolIdentifier;
    protected GsmSmsAddress mRecipientAddress;
    private int mStatus;
    protected SmsConstants.MessageClass messageClass;
    private boolean mReplyPathPresent = false;
    private boolean mIsStatusReportMessage = false;
    protected int mVoiceMailCount = 0;

    public static class SubmitPdu extends SmsMessageBase.SubmitPduBase {
    }

    public static SmsMessage createFromPdu(byte[] bArr) {
        try {
            SmsMessage smsMessage = new SmsMessage();
            smsMessage.parsePdu(bArr);
            return smsMessage;
        } catch (OutOfMemoryError e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed with out of memory: ", e);
            return null;
        } catch (RuntimeException e2) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", e2);
            return null;
        }
    }

    public boolean isTypeZero() {
        return this.mProtocolIdentifier == 64;
    }

    public static SmsMessage newFromCMT(byte[] bArr) {
        try {
            SmsMessage smsMessage = new SmsMessage();
            smsMessage.parsePdu(bArr);
            return smsMessage;
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", e);
            return null;
        }
    }

    public static SmsMessage newFromCDS(byte[] bArr) {
        try {
            SmsMessage smsMessage = new SmsMessage();
            smsMessage.parsePdu(bArr);
            return smsMessage;
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "CDS SMS PDU parsing failed: ", e);
            return null;
        }
    }

    public static SmsMessage createFromEfRecord(int i, byte[] bArr) {
        try {
            SmsMessage smsMessage = new SmsMessage();
            smsMessage.mIndexOnIcc = i;
            if ((bArr[0] & 1) == 0) {
                Rlog.w(LOG_TAG, "SMS parsing failed: Trying to parse a free record");
                return null;
            }
            smsMessage.mStatusOnIcc = bArr[0] & 7;
            int length = bArr.length - 1;
            byte[] bArr2 = new byte[length];
            System.arraycopy(bArr, 1, bArr2, 0, length);
            smsMessage.parsePdu(bArr2);
            return smsMessage;
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", e);
            return null;
        }
    }

    public static int getTPLayerLengthForPDU(String str) {
        return ((str.length() / 2) - Integer.parseInt(str.substring(0, 2), 16)) - 1;
    }

    public static int getRelativeValidityPeriod(int i) {
        if (i < 5 || i > VALIDITY_PERIOD_MAX) {
            Rlog.e(LOG_TAG, "Invalid Validity Period" + i);
            return -1;
        }
        if (i <= 720) {
            return (i / 5) - 1;
        }
        if (i <= 1440) {
            return ((i - MetricsProto.MetricsEvent.ACTION_PERMISSION_DENIED_RECEIVE_WAP_PUSH) / 30) + 143;
        }
        if (i <= 43200) {
            return (i / MetricsProto.MetricsEvent.ACTION_HUSH_GESTURE) + 166;
        }
        if (i <= VALIDITY_PERIOD_MAX) {
            return (i / 10080) + 192;
        }
        return -1;
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, byte[] bArr) {
        return getSubmitPdu(str, str2, str3, z, bArr, 0, 0, 0);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, byte[] bArr, int i, int i2, int i3) {
        return getSubmitPdu(str, str2, str3, z, bArr, i, i2, i3, -1);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, byte[] bArr, int i, int i2, int i3, int i4) {
        byte[] byteArray;
        int i5;
        int i6;
        int i7;
        byte[] bArrEncodeUCS2;
        if (str3 == null || str2 == null) {
            return null;
        }
        if (i == 0) {
            GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength = calculateLength(str3, false);
            i5 = textEncodingDetailsCalculateLength.codeUnitSize;
            i6 = textEncodingDetailsCalculateLength.languageTable;
            i7 = textEncodingDetailsCalculateLength.languageShiftTable;
            if (i5 == 1 && (i6 != 0 || i7 != 0)) {
                if (bArr != null) {
                    SmsHeader smsHeaderFromByteArray = SmsHeader.fromByteArray(bArr);
                    if (smsHeaderFromByteArray.languageTable != i6 || smsHeaderFromByteArray.languageShiftTable != i7) {
                        Rlog.w(LOG_TAG, "Updating language table in SMS header: " + smsHeaderFromByteArray.languageTable + " -> " + i6 + ", " + smsHeaderFromByteArray.languageShiftTable + " -> " + i7);
                        smsHeaderFromByteArray.languageTable = i6;
                        smsHeaderFromByteArray.languageShiftTable = i7;
                        byteArray = SmsHeader.toByteArray(smsHeaderFromByteArray);
                    } else {
                        byteArray = bArr;
                    }
                } else {
                    SmsHeader smsHeader = new SmsHeader();
                    smsHeader.languageTable = i6;
                    smsHeader.languageShiftTable = i7;
                    byteArray = SmsHeader.toByteArray(smsHeader);
                }
            } else {
                byteArray = bArr;
            }
        } else {
            byteArray = bArr;
            i5 = i;
            i6 = i2;
            i7 = i3;
        }
        SubmitPdu submitPdu = new SubmitPdu();
        int relativeValidityPeriod = getRelativeValidityPeriod(i4);
        int i8 = relativeValidityPeriod >= 0 ? 2 : 0;
        ByteArrayOutputStream submitPduHead = getSubmitPduHead(str, str2, (byte) ((i8 << 3) | 1 | (byteArray != null ? 64 : 0)), z, submitPdu);
        if (submitPduHead == null) {
            return submitPdu;
        }
        try {
            if (i5 == 1) {
                bArrEncodeUCS2 = GsmAlphabet.stringToGsm7BitPackedWithHeader(str3, byteArray, i6, i7);
            } else {
                try {
                    bArrEncodeUCS2 = encodeUCS2(str3, byteArray);
                } catch (UnsupportedEncodingException e) {
                    Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", e);
                    return null;
                }
            }
        } catch (EncodeException e2) {
            try {
                bArrEncodeUCS2 = encodeUCS2(str3, byteArray);
                i5 = 3;
            } catch (UnsupportedEncodingException e3) {
                Rlog.e(LOG_TAG, "Implausible UnsupportedEncodingException ", e3);
                return null;
            }
        }
        if (i5 == 1) {
            if ((bArrEncodeUCS2[0] & 255) > 160) {
                Rlog.e(LOG_TAG, "Message too long (" + (bArrEncodeUCS2[0] & 255) + " septets)");
                return null;
            }
            submitPduHead.write(0);
        } else {
            if ((bArrEncodeUCS2[0] & 255) > 140) {
                Rlog.e(LOG_TAG, "Message too long (" + (bArrEncodeUCS2[0] & 255) + " bytes)");
                return null;
            }
            submitPduHead.write(8);
        }
        if (i8 == 2) {
            submitPduHead.write(relativeValidityPeriod);
        }
        submitPduHead.write(bArrEncodeUCS2, 0, bArrEncodeUCS2.length);
        submitPdu.encodedMessage = submitPduHead.toByteArray();
        return submitPdu;
    }

    protected static byte[] encodeUCS2(String str, byte[] bArr) throws UnsupportedEncodingException {
        byte[] bytes = str.getBytes("utf-16be");
        if (bArr != null) {
            byte[] bArr2 = new byte[bArr.length + bytes.length + 1];
            bArr2[0] = (byte) bArr.length;
            System.arraycopy(bArr, 0, bArr2, 1, bArr.length);
            System.arraycopy(bytes, 0, bArr2, bArr.length + 1, bytes.length);
            bytes = bArr2;
        }
        byte[] bArr3 = new byte[bytes.length + 1];
        bArr3[0] = (byte) (bytes.length & 255);
        System.arraycopy(bytes, 0, bArr3, 1, bytes.length);
        return bArr3;
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z) {
        return getSubmitPdu(str, str2, str3, z, (byte[]) null);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, int i) {
        return getSubmitPdu(str, str2, str3, z, null, 0, 0, 0, i);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z) {
        SmsHeader.PortAddrs portAddrs = new SmsHeader.PortAddrs();
        portAddrs.destPort = i;
        portAddrs.origPort = 0;
        portAddrs.areEightBits = false;
        SmsHeader smsHeader = new SmsHeader();
        smsHeader.portAddrs = portAddrs;
        byte[] byteArray = SmsHeader.toByteArray(smsHeader);
        if (bArr.length + byteArray.length + 1 > 140) {
            StringBuilder sb = new StringBuilder();
            sb.append("SMS data message may only contain ");
            sb.append((140 - byteArray.length) - 1);
            sb.append(" bytes");
            Rlog.e(LOG_TAG, sb.toString());
            return null;
        }
        SubmitPdu submitPdu = new SubmitPdu();
        ByteArrayOutputStream submitPduHead = getSubmitPduHead(str, str2, (byte) 65, z, submitPdu);
        if (submitPduHead == null) {
            return submitPdu;
        }
        submitPduHead.write(4);
        submitPduHead.write(bArr.length + byteArray.length + 1);
        submitPduHead.write(byteArray.length);
        submitPduHead.write(byteArray, 0, byteArray.length);
        submitPduHead.write(bArr, 0, bArr.length);
        submitPdu.encodedMessage = submitPduHead.toByteArray();
        return submitPdu;
    }

    protected static ByteArrayOutputStream getSubmitPduHead(String str, String str2, byte b, boolean z, SubmitPdu submitPdu) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(180);
        if (str == null) {
            submitPdu.encodedScAddress = null;
        } else {
            submitPdu.encodedScAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(str);
        }
        if (z) {
            b = (byte) (b | 32);
        }
        byteArrayOutputStream.write(b);
        byteArrayOutputStream.write(0);
        byte[] bArrNetworkPortionToCalledPartyBCD = PhoneNumberUtils.networkPortionToCalledPartyBCD(str2);
        if (bArrNetworkPortionToCalledPartyBCD == null) {
            return null;
        }
        if (bArrNetworkPortionToCalledPartyBCD != null) {
            byteArrayOutputStream.write(((bArrNetworkPortionToCalledPartyBCD.length - 1) * 2) - ((bArrNetworkPortionToCalledPartyBCD[bArrNetworkPortionToCalledPartyBCD.length - 1] & 240) != 240 ? 0 : 1));
            byteArrayOutputStream.write(bArrNetworkPortionToCalledPartyBCD, 0, bArrNetworkPortionToCalledPartyBCD.length);
        } else {
            Rlog.d(LOG_TAG, "write an empty address for submit pdu");
            byteArrayOutputStream.write(0);
            byteArrayOutputStream.write(129);
        }
        byteArrayOutputStream.write(0);
        return byteArrayOutputStream;
    }

    protected static class PduParser {
        byte[] mPdu;
        byte[] mUserData;
        SmsHeader mUserDataHeader;
        int mCur = 0;
        int mUserDataSeptetPadding = 0;

        PduParser(byte[] bArr) {
            this.mPdu = bArr;
        }

        String getSCAddress() {
            int i = getByte();
            String strCalledPartyBCDToString = null;
            if (i != 0) {
                try {
                    strCalledPartyBCDToString = PhoneNumberUtils.calledPartyBCDToString(this.mPdu, this.mCur, i, 2);
                } catch (RuntimeException e) {
                    Rlog.d(SmsMessage.LOG_TAG, "invalid SC address: ", e);
                }
            }
            this.mCur += i;
            return strCalledPartyBCDToString;
        }

        int getByte() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            return bArr[i] & 255;
        }

        GsmSmsAddress getAddress() {
            int i = 2 + (((this.mPdu[this.mCur] & 255) + 1) / 2);
            try {
                GsmSmsAddress gsmSmsAddress = new GsmSmsAddress(this.mPdu, this.mCur, i);
                this.mCur += i;
                return gsmSmsAddress;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        long getSCTimestampMillis() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            int iGsmBcdByteToInt = IccUtils.gsmBcdByteToInt(bArr[i]);
            byte[] bArr2 = this.mPdu;
            int i2 = this.mCur;
            this.mCur = i2 + 1;
            int iGsmBcdByteToInt2 = IccUtils.gsmBcdByteToInt(bArr2[i2]);
            byte[] bArr3 = this.mPdu;
            int i3 = this.mCur;
            this.mCur = i3 + 1;
            int iGsmBcdByteToInt3 = IccUtils.gsmBcdByteToInt(bArr3[i3]);
            byte[] bArr4 = this.mPdu;
            int i4 = this.mCur;
            this.mCur = i4 + 1;
            int iGsmBcdByteToInt4 = IccUtils.gsmBcdByteToInt(bArr4[i4]);
            byte[] bArr5 = this.mPdu;
            int i5 = this.mCur;
            this.mCur = i5 + 1;
            int iGsmBcdByteToInt5 = IccUtils.gsmBcdByteToInt(bArr5[i5]);
            byte[] bArr6 = this.mPdu;
            int i6 = this.mCur;
            this.mCur = i6 + 1;
            int iGsmBcdByteToInt6 = IccUtils.gsmBcdByteToInt(bArr6[i6]);
            byte[] bArr7 = this.mPdu;
            int i7 = this.mCur;
            this.mCur = i7 + 1;
            byte b = bArr7[i7];
            int iGsmBcdByteToInt7 = IccUtils.gsmBcdByteToInt((byte) (b & (-9)));
            if ((b & 8) != 0) {
                iGsmBcdByteToInt7 = -iGsmBcdByteToInt7;
            }
            Time time = new Time(Time.TIMEZONE_UTC);
            time.year = iGsmBcdByteToInt >= 90 ? iGsmBcdByteToInt + PreciseDisconnectCause.ECBM_NOT_SUPPORTED : iGsmBcdByteToInt + 2000;
            time.month = iGsmBcdByteToInt2 - 1;
            time.monthDay = iGsmBcdByteToInt3;
            time.hour = iGsmBcdByteToInt4;
            time.minute = iGsmBcdByteToInt5;
            time.second = iGsmBcdByteToInt6;
            return time.toMillis(true) - ((long) (((iGsmBcdByteToInt7 * 15) * 60) * 1000));
        }

        public int constructUserData(boolean z, boolean z2) {
            int i;
            int i2;
            int length;
            int i3 = this.mCur;
            int i4 = i3 + 1;
            int i5 = this.mPdu[i3] & 255;
            if (z) {
                int i6 = i4 + 1;
                int i7 = this.mPdu[i4] & 255;
                byte[] bArr = new byte[i7];
                System.arraycopy(this.mPdu, i6, bArr, 0, i7);
                this.mUserDataHeader = SmsHeader.fromByteArray(bArr);
                int i8 = i6 + i7;
                int i9 = (i7 + 1) * 8;
                i2 = (i9 / 7) + (i9 % 7 > 0 ? 1 : 0);
                this.mUserDataSeptetPadding = (i2 * 7) - i9;
                i = i7;
                i4 = i8;
            } else {
                i = 0;
                i2 = 0;
            }
            if (z2) {
                length = this.mPdu.length - i4;
            } else {
                length = i5 - (z ? i + 1 : 0);
                if (length < 0) {
                    length = 0;
                }
            }
            this.mUserData = new byte[length];
            System.arraycopy(this.mPdu, i4, this.mUserData, 0, this.mUserData.length);
            this.mCur = i4;
            if (z2) {
                int i10 = i5 - i2;
                if (i10 < 0) {
                    return 0;
                }
                return i10;
            }
            return this.mUserData.length;
        }

        public byte[] getUserData() {
            return this.mUserData;
        }

        public SmsHeader getUserDataHeader() {
            return this.mUserDataHeader;
        }

        public String getUserDataGSM7Bit(int i, int i2, int i3) {
            String strGsm7BitPackedToString = GsmAlphabet.gsm7BitPackedToString(this.mPdu, this.mCur, i, this.mUserDataSeptetPadding, i2, i3);
            this.mCur += (i * 7) / 8;
            return strGsm7BitPackedToString;
        }

        public String getUserDataGSM8bit(int i) {
            String strGsm8BitUnpackedToString = GsmAlphabet.gsm8BitUnpackedToString(this.mPdu, this.mCur, i);
            this.mCur += i;
            return strGsm8BitUnpackedToString;
        }

        public String getUserDataUCS2(int i) {
            String str;
            try {
                str = new String(this.mPdu, this.mCur, i, "utf-16");
            } catch (UnsupportedEncodingException e) {
                Rlog.e(SmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", e);
                str = "";
            }
            this.mCur += i;
            return str;
        }

        public String getUserDataKSC5601(int i) {
            String str;
            try {
                str = new String(this.mPdu, this.mCur, i, "KSC5601");
            } catch (UnsupportedEncodingException e) {
                Rlog.e(SmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", e);
                str = "";
            }
            this.mCur += i;
            return str;
        }

        boolean moreDataPresent() {
            return this.mPdu.length > this.mCur;
        }
    }

    public static GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z) {
        String strTranslate;
        if (Resources.getSystem().getBoolean(R.bool.config_sms_force_7bit_encoding)) {
            strTranslate = Sms7BitEncodingTranslator.translate(charSequence);
        } else {
            strTranslate = null;
        }
        if (!TextUtils.isEmpty(strTranslate)) {
            charSequence = strTranslate;
        }
        GsmAlphabet.TextEncodingDetails textEncodingDetailsCountGsmSeptets = GsmAlphabet.countGsmSeptets(charSequence, z);
        if (textEncodingDetailsCountGsmSeptets == null) {
            return SmsMessageBase.calcUnicodeEncodingDetails(charSequence);
        }
        return textEncodingDetailsCountGsmSeptets;
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
        return ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageClear() || ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet();
    }

    @Override
    public boolean isMWIClearMessage() {
        if (!this.mIsMwi || this.mMwiSense) {
            return this.mOriginatingAddress != null && ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageClear();
        }
        return true;
    }

    @Override
    public boolean isMWISetMessage() {
        if (this.mIsMwi && this.mMwiSense) {
            return true;
        }
        return this.mOriginatingAddress != null && ((GsmSmsAddress) this.mOriginatingAddress).isCphsVoiceMessageSet();
    }

    @Override
    public boolean isMwiDontStore() {
        if (this.mIsMwi && this.mMwiDontStore) {
            return true;
        }
        return isCphsMwiMessage() && WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER.equals(getMessageBody());
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

    protected void parsePdu(byte[] bArr) {
        this.mPdu = bArr;
        PduParser pduParser = new PduParser(bArr);
        this.mScAddress = pduParser.getSCAddress();
        String str = this.mScAddress;
        int i = pduParser.getByte();
        this.mMti = i & 3;
        switch (this.mMti) {
            case 0:
            case 3:
                parseSmsDeliver(pduParser, i);
                return;
            case 1:
                parseSmsSubmit(pduParser, i);
                return;
            case 2:
                parseSmsStatusReport(pduParser, i);
                return;
            default:
                throw new RuntimeException("Unsupported message type");
        }
    }

    protected void parseSmsStatusReport(PduParser pduParser, int i) {
        this.mIsStatusReportMessage = true;
        this.mMessageRef = pduParser.getByte();
        this.mRecipientAddress = pduParser.getAddress();
        this.mScTimeMillis = pduParser.getSCTimestampMillis();
        pduParser.getSCTimestampMillis();
        this.mStatus = pduParser.getByte();
        if (pduParser.moreDataPresent()) {
            int i2 = pduParser.getByte();
            int i3 = i2;
            while ((i3 & 128) != 0 && pduParser.moreDataPresent()) {
                i3 = pduParser.getByte();
            }
            if ((i2 & 120) == 0) {
                if ((i2 & 1) != 0) {
                    this.mProtocolIdentifier = pduParser.getByte();
                }
                if ((i2 & 2) != 0) {
                    this.mDataCodingScheme = pduParser.getByte();
                }
                if ((i2 & 4) != 0) {
                    parseUserData(pduParser, (i & 64) == 64);
                }
            }
        }
    }

    private void parseSmsDeliver(PduParser pduParser, int i) {
        this.mReplyPathPresent = (i & 128) == 128;
        this.mOriginatingAddress = pduParser.getAddress();
        SmsAddress smsAddress = this.mOriginatingAddress;
        this.mProtocolIdentifier = pduParser.getByte();
        this.mDataCodingScheme = pduParser.getByte();
        this.mScTimeMillis = pduParser.getSCTimestampMillis();
        parseUserData(pduParser, (i & 64) == 64);
    }

    protected void parseSmsSubmit(PduParser pduParser, int i) {
        this.mReplyPathPresent = (i & 128) == 128;
        this.mMessageRef = pduParser.getByte();
        this.mRecipientAddress = pduParser.getAddress();
        GsmSmsAddress gsmSmsAddress = this.mRecipientAddress;
        this.mProtocolIdentifier = pduParser.getByte();
        this.mDataCodingScheme = pduParser.getByte();
        int i2 = (i >> 3) & 3;
        int i3 = i2 == 0 ? 0 : 2 == i2 ? 1 : 7;
        while (true) {
            int i4 = i3 - 1;
            if (i3 <= 0) {
                break;
            }
            pduParser.getByte();
            i3 = i4;
        }
        parseUserData(pduParser, (i & 64) == 64);
    }

    protected void parseUserData(PduParser pduParser, boolean z) {
        char c;
        char c2;
        int i = 128;
        char c3 = 4;
        char c4 = 2;
        if ((this.mDataCodingScheme & 128) == 0) {
            boolean z2 = (this.mDataCodingScheme & 32) != 0;
            char c5 = (this.mDataCodingScheme & 16) != 0 ? (char) 1 : (char) 0;
            if (z2) {
                Rlog.w(LOG_TAG, "4 - Unsupported SMS data coding scheme (compression) " + (this.mDataCodingScheme & 255));
                c = c5;
                c3 = (char) 0;
            } else {
                switch ((this.mDataCodingScheme >> 2) & 3) {
                    case 0:
                        c4 = 1;
                        break;
                    case 1:
                        if (!Resources.getSystem().getBoolean(R.bool.config_sms_decode_gsm_8bit_data)) {
                            Rlog.w(LOG_TAG, "1 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
                        }
                        break;
                    case 2:
                        c4 = 3;
                        break;
                    case 3:
                        break;
                    default:
                        c4 = 0;
                        break;
                }
                c = c5;
                c3 = c4;
            }
        } else if ((this.mDataCodingScheme & 240) != 240) {
            if ((this.mDataCodingScheme & 240) == 192 || (this.mDataCodingScheme & 240) == 208 || (this.mDataCodingScheme & 240) == 224) {
                c3 = (this.mDataCodingScheme & 240) == 224 ? (char) 3 : (char) 1;
                boolean z3 = (this.mDataCodingScheme & 8) == 8;
                if ((this.mDataCodingScheme & 3) == 0) {
                    this.mIsMwi = true;
                    this.mMwiSense = z3;
                    this.mMwiDontStore = (this.mDataCodingScheme & 240) == 192;
                    if (z3) {
                        this.mVoiceMailCount = -1;
                    } else {
                        this.mVoiceMailCount = 0;
                    }
                    Rlog.w(LOG_TAG, "MWI in DCS for Vmail. DCS = " + (this.mDataCodingScheme & 255) + " Dont store = " + this.mMwiDontStore + " vmail count = " + this.mVoiceMailCount);
                } else {
                    this.mIsMwi = false;
                    Rlog.w(LOG_TAG, "MWI in DCS for fax/email/other: " + (this.mDataCodingScheme & 255));
                }
            } else {
                if ((this.mDataCodingScheme & 192) != 128) {
                    Rlog.w(LOG_TAG, "3 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
                } else if (this.mDataCodingScheme != 132) {
                    Rlog.w(LOG_TAG, "5 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
                }
                c = 0;
                c3 = c;
            }
            c = 0;
        } else if ((this.mDataCodingScheme & 4) == 0) {
            c = 1;
            c3 = c;
        } else {
            c3 = 2;
            c = 1;
        }
        int iConstructUserData = pduParser.constructUserData(z, c3 == 1);
        this.mUserData = pduParser.getUserData();
        this.mUserDataHeader = pduParser.getUserDataHeader();
        if (z && this.mUserDataHeader.specialSmsMsgList.size() != 0) {
            for (SmsHeader.SpecialSmsMsg specialSmsMsg : this.mUserDataHeader.specialSmsMsgList) {
                int i2 = specialSmsMsg.msgIndType & 255;
                if (i2 == 0 || i2 == i) {
                    this.mIsMwi = true;
                    if (i2 == i) {
                        this.mMwiDontStore = false;
                    } else {
                        if (!this.mMwiDontStore) {
                            if ((this.mDataCodingScheme & 240) != 208) {
                                c2 = 224;
                                if ((this.mDataCodingScheme & 240) != 224) {
                                    this.mMwiDontStore = true;
                                }
                            } else {
                                c2 = 224;
                            }
                            if ((this.mDataCodingScheme & 3) != 0) {
                            }
                        }
                        this.mVoiceMailCount = specialSmsMsg.msgCount & 255;
                        if (this.mVoiceMailCount <= 0) {
                            this.mMwiSense = true;
                        } else {
                            this.mMwiSense = false;
                        }
                        Rlog.w(LOG_TAG, "MWI in TP-UDH for Vmail. Msg Ind = " + i2 + " Dont store = " + this.mMwiDontStore + " Vmail count = " + this.mVoiceMailCount);
                    }
                    c2 = 224;
                    this.mVoiceMailCount = specialSmsMsg.msgCount & 255;
                    if (this.mVoiceMailCount <= 0) {
                    }
                    Rlog.w(LOG_TAG, "MWI in TP-UDH for Vmail. Msg Ind = " + i2 + " Dont store = " + this.mMwiDontStore + " Vmail count = " + this.mVoiceMailCount);
                } else {
                    Rlog.w(LOG_TAG, "TP_UDH fax/email/extended msg/multisubscriber profile. Msg Ind = " + i2);
                    c2 = 224;
                }
                i = 128;
            }
        }
        switch (c3) {
            case 0:
                this.mMessageBody = null;
                break;
            case 1:
                this.mMessageBody = pduParser.getUserDataGSM7Bit(iConstructUserData, z ? this.mUserDataHeader.languageTable : 0, z ? this.mUserDataHeader.languageShiftTable : 0);
                break;
            case 2:
                if (Resources.getSystem().getBoolean(R.bool.config_sms_decode_gsm_8bit_data)) {
                    this.mMessageBody = pduParser.getUserDataGSM8bit(iConstructUserData);
                } else {
                    this.mMessageBody = null;
                }
                break;
            case 3:
                this.mMessageBody = pduParser.getUserDataUCS2(iConstructUserData);
                break;
            case 4:
                this.mMessageBody = pduParser.getUserDataKSC5601(iConstructUserData);
                break;
        }
        if (this.mMessageBody != null) {
            parseMessageBody();
        }
        if (c == 0) {
            this.messageClass = SmsConstants.MessageClass.UNKNOWN;
            return;
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
        return this.messageClass == SmsConstants.MessageClass.CLASS_2 && (this.mProtocolIdentifier == 127 || this.mProtocolIdentifier == 124);
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

    protected static SmsHeader makeSmsHeader() {
        try {
            SmsHeader smsHeader = (SmsHeader) Class.forName("com.mediatek.internal.telephony.MtkSmsHeader").getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            Rlog.d(LOG_TAG, "Make MtkSmsHeader successfully!");
            return smsHeader;
        } catch (Exception e) {
            SmsHeader smsHeader2 = new SmsHeader();
            Rlog.d(LOG_TAG, "MtkSmsHeader does not exist!");
            return smsHeader2;
        }
    }
}
