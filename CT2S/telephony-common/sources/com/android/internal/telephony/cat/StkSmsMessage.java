package com.android.internal.telephony.cat;

import android.telephony.Rlog;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.gsm.GsmSmsAddress;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.PduHeaders;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class StkSmsMessage {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "CatService";
    private int mDataCodingScheme;
    private byte[] mEncodeMessage;
    private String mMessageBody;
    private int mMessageRef;
    private int mMti;
    private byte[] mPdu;
    private int mProtocolIdentifier;
    private String mPseudoSubject;
    private GsmSmsAddress mRecipientAddress;
    private byte[] mUserData;
    private SmsHeader mUserDataHeader;
    private SmsConstants.MessageClass messageClass;
    private boolean mReplyPathPresent = false;
    ByteArrayOutputStream bo = new ByteArrayOutputStream(PduHeaders.RECOMMENDED_RETRIEVAL_MODE);

    public static StkSmsMessage createFromPdu(byte[] pdu) {
        try {
            StkSmsMessage msg = new StkSmsMessage();
            msg.parsePdu(pdu);
            return msg;
        } catch (OutOfMemoryError e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed with out of memory: ", e);
            return null;
        } catch (RuntimeException ex) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: " + ex);
            return null;
        }
    }

    public int getDataCodingScheme() {
        return this.mDataCodingScheme;
    }

    public byte[] getEncodeMessage() {
        this.mEncodeMessage = this.bo.toByteArray();
        return this.mEncodeMessage;
    }

    private void parsePdu(byte[] pdu) {
        this.mPdu = pdu;
        PduParser p = new PduParser(pdu);
        int firstByte = p.getByte();
        this.bo.write(firstByte);
        this.mMti = firstByte & 3;
        Rlog.d(LOG_TAG, "mMti is " + this.mMti);
        switch (this.mMti) {
            case 0:
            case 2:
            case 3:
                return;
            case 1:
                parseSmsSubmit(p, firstByte);
                return;
            default:
                throw new RuntimeException("Unsupported message type");
        }
    }

    private class PduParser {
        byte[] mPdu;
        byte[] mUserData;
        SmsHeader mUserDataHeader;
        int mCur = 0;
        int mUserDataSeptetPadding = 0;

        PduParser(byte[] pdu) {
            this.mPdu = pdu;
        }

        int getByte() {
            byte[] bArr = this.mPdu;
            int i = this.mCur;
            this.mCur = i + 1;
            return bArr[i] & 255;
        }

        GsmSmsAddress getAddress() {
            int addressLength = this.mPdu[this.mCur] & 255;
            int lengthBytes = ((addressLength + 1) / 2) + 2;
            StkSmsMessage.this.bo.write(this.mPdu, this.mCur, lengthBytes);
            try {
                GsmSmsAddress ret = new GsmSmsAddress(this.mPdu, this.mCur, lengthBytes);
                this.mCur += lengthBytes;
                return ret;
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage());
            }
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
                StkSmsMessage.this.bo.write(this.mPdu, this.mCur, this.mPdu.length - this.mCur);
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

        String getUserDataGSM8Bit(int byteCount) {
            String ret = GsmAlphabet.gsm8BitUnpackedToString(this.mPdu, this.mCur, byteCount);
            this.mCur += byteCount;
            return ret;
        }

        String getUserDataUCS2(int byteCount) {
            String ret;
            try {
                ret = new String(this.mPdu, this.mCur, byteCount, CharacterSets.MIMENAME_UTF_16);
            } catch (UnsupportedEncodingException ex) {
                ret = "";
                Rlog.e(StkSmsMessage.LOG_TAG, "implausible UnsupportedEncodingException" + ex);
            }
            this.mCur += byteCount;
            return ret;
        }

        String getUserDataKSC5601(int byteCount) {
            String ret;
            try {
                ret = new String(this.mPdu, this.mCur, byteCount, "KSC5601");
            } catch (UnsupportedEncodingException ex) {
                ret = "";
                Rlog.e(StkSmsMessage.LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }
            this.mCur += byteCount;
            return ret;
        }

        boolean moreDataPresent() {
            return this.mPdu.length > this.mCur;
        }
    }

    private void parseSmsSubmit(PduParser p, int firstByte) {
        this.mReplyPathPresent = (firstByte & 128) == 128;
        this.mMessageRef = p.getByte();
        this.bo.write(this.mMessageRef);
        this.mRecipientAddress = p.getAddress();
        if (this.mRecipientAddress != null) {
            Rlog.d(LOG_TAG, "SMS recipient address: " + this.mRecipientAddress.address);
        }
        this.mProtocolIdentifier = p.getByte();
        this.bo.write(this.mProtocolIdentifier);
        this.mDataCodingScheme = p.getByte();
        if ((this.mDataCodingScheme & 4) == 4) {
            this.bo.write(this.mDataCodingScheme & 240);
        } else {
            this.bo.write(this.mDataCodingScheme);
        }
        Rlog.d(LOG_TAG, "SMS TP-PID:" + this.mProtocolIdentifier + " data coding scheme: " + this.mDataCodingScheme);
        boolean hasUserDataHeader = (firstByte & 64) == 64;
        Rlog.d(LOG_TAG, "hasUserDataHeader:" + hasUserDataHeader);
        parseUserData(p, hasUserDataHeader);
    }

    private void parseUserData(PduParser p, boolean hasUserDataHeader) {
        boolean hasMessageClass = false;
        int encodingType = 0;
        if ((this.mDataCodingScheme & 128) == 0) {
            boolean userDataCompressed = (this.mDataCodingScheme & 32) != 0;
            hasMessageClass = (this.mDataCodingScheme & 16) != 0;
            if (userDataCompressed) {
                Rlog.w(LOG_TAG, "4 - Unsupported SMS data coding scheme (compression) " + (this.mDataCodingScheme & 255));
            } else {
                switch ((this.mDataCodingScheme >> 2) & 3) {
                    case 0:
                        encodingType = 1;
                        break;
                    case 1:
                    case 3:
                        Rlog.w(LOG_TAG, "1 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
                        encodingType = 2;
                        break;
                    case 2:
                        encodingType = 3;
                        break;
                }
            }
        } else if ((this.mDataCodingScheme & 240) == 240) {
            hasMessageClass = true;
            encodingType = (this.mDataCodingScheme & 4) == 0 ? 1 : 2;
        } else if ((this.mDataCodingScheme & 240) == 192 || (this.mDataCodingScheme & 240) == 208 || (this.mDataCodingScheme & 240) == 224) {
            if ((this.mDataCodingScheme & 240) == 224) {
                encodingType = 3;
            } else {
                encodingType = 1;
            }
        } else if ((this.mDataCodingScheme & 192) == 128) {
            if (this.mDataCodingScheme == 132) {
                encodingType = 4;
            } else {
                Rlog.w(LOG_TAG, "5 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
            }
        } else {
            Rlog.w(LOG_TAG, "3 - Unsupported SMS data coding scheme " + (this.mDataCodingScheme & 255));
        }
        int count = p.constructUserData(hasUserDataHeader, encodingType == 1);
        this.mUserData = p.getUserData();
        this.mUserDataHeader = p.getUserDataHeader();
        switch (encodingType) {
            case 0:
                this.mMessageBody = null;
                break;
            case 1:
                this.mMessageBody = p.getUserDataGSM7Bit(count, hasUserDataHeader ? this.mUserDataHeader.languageTable : 0, hasUserDataHeader ? this.mUserDataHeader.languageShiftTable : 0);
                break;
            case 2:
                this.mMessageBody = p.getUserDataGSM8Bit(count);
                try {
                    byte[] encode7BitPacking = GsmAlphabet.stringToGsm7BitPacked(this.mMessageBody);
                    this.bo.write(encode7BitPacking, 0, encode7BitPacking.length);
                } catch (EncodeException ex) {
                    Rlog.e(LOG_TAG, "encode exception" + ex);
                }
                break;
            case 3:
                this.mMessageBody = p.getUserDataUCS2(count);
                break;
            case 4:
                this.mMessageBody = p.getUserDataKSC5601(count);
                break;
        }
        Rlog.v(LOG_TAG, "SMS message body (raw): '" + this.mMessageBody + "'");
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
}
