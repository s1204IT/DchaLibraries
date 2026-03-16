package com.android.bluetooth.map;

import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.UserData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class BluetoothMapSmsPdu {
    private static final String TAG = "BluetoothMapSmsPdu";
    private static final boolean V = false;
    private static int INVALID_VALUE = -1;
    public static int SMS_TYPE_GSM = 1;
    public static int SMS_TYPE_CDMA = 2;
    private static int sConcatenatedRef = new Random().nextInt(256);

    public static class SmsPdu {
        private static final byte BEARER_DATA = 8;
        private static final byte BEARER_DATA_MSG_ID = 0;
        private static final byte BEARER_REPLY_OPTION = 6;
        private static final byte CAUSE_CODES = 7;
        private static final byte DESTINATION_ADDRESS = 4;
        private static final byte DESTINATION_SUB_ADDRESS = 5;
        private static final byte ORIGINATING_ADDRESS = 2;
        private static final byte ORIGINATING_SUB_ADDRESS = 3;
        private static final byte SERVICE_CATEGORY = 1;
        private static final byte TELESERVICE_IDENTIFIER = 0;
        private static final byte TP_MIT_DELIVER = 0;
        private static final byte TP_MMS_NO_MORE = 4;
        private static final byte TP_RP_NO_REPLY_PATH = 0;
        private static final byte TP_SRI_NO_REPORT = 0;
        private static final byte TP_UDHI_MASK = 64;
        private byte[] mData;
        private int mEncoding;
        private int mLanguageShiftTable;
        private int mLanguageTable;
        private int mMsgSeptetCount;
        private byte[] mScAddress;
        private int mType;
        private int mUserDataMsgOffset;
        private int mUserDataSeptetPadding;

        SmsPdu(byte[] data, int type) {
            this.mScAddress = new byte[]{0};
            this.mUserDataMsgOffset = 0;
            this.mUserDataSeptetPadding = BluetoothMapSmsPdu.INVALID_VALUE;
            this.mMsgSeptetCount = 0;
            this.mData = data;
            this.mEncoding = BluetoothMapSmsPdu.INVALID_VALUE;
            this.mType = type;
            this.mLanguageTable = BluetoothMapSmsPdu.INVALID_VALUE;
            this.mLanguageShiftTable = BluetoothMapSmsPdu.INVALID_VALUE;
            this.mUserDataMsgOffset = gsmSubmitGetTpUdOffset();
        }

        SmsPdu(byte[] data, int encoding, int type, int languageTable) {
            this.mScAddress = new byte[]{0};
            this.mUserDataMsgOffset = 0;
            this.mUserDataSeptetPadding = BluetoothMapSmsPdu.INVALID_VALUE;
            this.mMsgSeptetCount = 0;
            this.mData = data;
            this.mEncoding = encoding;
            this.mType = type;
            this.mLanguageTable = languageTable;
        }

        public byte[] getData() {
            return this.mData;
        }

        public byte[] getScAddress() {
            return this.mScAddress;
        }

        public void setEncoding(int encoding) {
            this.mEncoding = encoding;
        }

        public int getEncoding() {
            return this.mEncoding;
        }

        public int getType() {
            return this.mType;
        }

        public int getUserDataMsgOffset() {
            return this.mUserDataMsgOffset;
        }

        public int getUserDataMsgSize() {
            return this.mData.length - this.mUserDataMsgOffset;
        }

        public int getLanguageShiftTable() {
            return this.mLanguageShiftTable;
        }

        public int getLanguageTable() {
            return this.mLanguageTable;
        }

        public int getUserDataSeptetPadding() {
            return this.mUserDataSeptetPadding;
        }

        public int getMsgSeptetCount() {
            return this.mMsgSeptetCount;
        }

        private int cdmaGetParameterOffset(byte parameterId) {
            ByteArrayInputStream pdu = new ByteArrayInputStream(this.mData);
            int offset = 0;
            boolean found = false;
            try {
                pdu.skip(1L);
                while (true) {
                    if (pdu.available() <= 0) {
                        break;
                    }
                    int currentId = pdu.read();
                    int currentLen = pdu.read();
                    if (currentId == parameterId) {
                        found = true;
                        break;
                    }
                    pdu.skip(currentLen);
                    offset += currentLen + 2;
                }
                pdu.close();
            } catch (Exception e) {
                Log.e(BluetoothMapSmsPdu.TAG, "cdmaGetParameterOffset: ", e);
            }
            if (found) {
                return offset;
            }
            return 0;
        }

        private int cdmaGetSubParameterOffset(byte subParameterId) {
            ByteArrayInputStream pdu = new ByteArrayInputStream(this.mData);
            boolean found = false;
            int offset = cdmaGetParameterOffset(BEARER_DATA) + 2;
            pdu.skip(offset);
            while (true) {
                try {
                    if (pdu.available() <= 0) {
                        break;
                    }
                    int currentId = pdu.read();
                    int currentLen = pdu.read();
                    if (currentId == subParameterId) {
                        found = true;
                        break;
                    }
                    pdu.skip(currentLen);
                    offset += currentLen + 2;
                } catch (Exception e) {
                    Log.e(BluetoothMapSmsPdu.TAG, "cdmaGetParameterOffset: ", e);
                }
            }
            pdu.close();
            if (found) {
                return offset;
            }
            return 0;
        }

        public void cdmaChangeToDeliverPdu(long date) {
            if (this.mData == null) {
                throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
            }
            int offset = cdmaGetParameterOffset((byte) 4);
            if (this.mData.length < offset) {
                throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
            }
            this.mData[offset] = 2;
            int offset2 = cdmaGetParameterOffset(DESTINATION_SUB_ADDRESS);
            if (this.mData.length < offset2) {
                throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
            }
            this.mData[offset2] = 3;
            int offset3 = cdmaGetSubParameterOffset((byte) 0);
            if (this.mData.length > offset3 + 2) {
                int tmp = this.mData[offset3 + 2] & 255;
                this.mData[offset3 + 2] = (byte) ((tmp & 15) | 16);
                return;
            }
            throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
        }

        private int gsmSubmitGetTpPidOffset() {
            int offset = (((this.mData[2] + 1) & 255) / 2) + 2 + 2;
            if (offset > this.mData.length || offset > 14) {
                throw new IllegalArgumentException("wrongly formatted gsm submit PDU. offset = " + offset);
            }
            return offset;
        }

        public int gsmSubmitGetTpDcs() {
            return this.mData[gsmSubmitGetTpDcsOffset()] & 255;
        }

        public boolean gsmSubmitHasUserDataHeader() {
            return ((this.mData[0] & 255) & 64) == 64;
        }

        private int gsmSubmitGetTpDcsOffset() {
            return gsmSubmitGetTpPidOffset() + 1;
        }

        private int gsmSubmitGetTpUdlOffset() {
            switch (((this.mData[0] & 255) & 12) >> 2) {
                case 0:
                    return gsmSubmitGetTpPidOffset() + 2;
                case 1:
                    return gsmSubmitGetTpPidOffset() + 2 + 1;
                default:
                    return gsmSubmitGetTpPidOffset() + 2 + 7;
            }
        }

        private int gsmSubmitGetTpUdOffset() {
            return gsmSubmitGetTpUdlOffset() + 1;
        }

        public void gsmDecodeUserDataHeader() {
            ByteArrayInputStream pdu = new ByteArrayInputStream(this.mData);
            pdu.skip(gsmSubmitGetTpUdlOffset());
            int userDataLength = pdu.read();
            if (gsmSubmitHasUserDataHeader()) {
                int userDataHeaderLength = pdu.read();
                if (this.mEncoding == 1) {
                    byte[] udh = new byte[userDataHeaderLength];
                    try {
                        pdu.read(udh);
                    } catch (IOException e) {
                        Log.w(BluetoothMapSmsPdu.TAG, "unable to read userDataHeader", e);
                    }
                    SmsHeader userDataHeader = SmsHeader.fromByteArray(udh);
                    this.mLanguageTable = userDataHeader.languageTable;
                    this.mLanguageShiftTable = userDataHeader.languageShiftTable;
                    int headerBits = (userDataHeaderLength + 1) * 8;
                    int headerSeptets = (headerBits / 7) + (headerBits % 7 <= 0 ? 0 : 1);
                    this.mUserDataSeptetPadding = (headerSeptets * 7) - headerBits;
                    this.mMsgSeptetCount = userDataLength - headerSeptets;
                }
                this.mUserDataMsgOffset = gsmSubmitGetTpUdOffset() + userDataHeaderLength + 1;
                return;
            }
            this.mUserDataSeptetPadding = 0;
            this.mMsgSeptetCount = userDataLength;
            this.mUserDataMsgOffset = gsmSubmitGetTpUdOffset();
        }

        private void gsmWriteDate(ByteArrayOutputStream header, long time) throws UnsupportedEncodingException {
            SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss");
            Date date = new Date(time);
            String timeStr = format.format(date);
            byte[] timeChars = timeStr.getBytes("US-ASCII");
            int n = timeStr.length();
            for (int i = 0; i < n; i += 2) {
                header.write(((timeChars[i + 1] - 48) << 4) | (timeChars[i] - 48));
            }
            Calendar cal = Calendar.getInstance();
            int offset = (cal.get(15) + cal.get(16)) / 900000;
            if (offset < 0) {
                String offsetString = String.format("%1$02d", Integer.valueOf(-offset));
                char[] offsetChars = offsetString.toCharArray();
                header.write(((offsetChars[1] - '0') << 4) | 64 | (offsetChars[0] - '0'));
            } else {
                String offsetString2 = String.format("%1$02d", Integer.valueOf(offset));
                char[] offsetChars2 = offsetString2.toCharArray();
                header.write(((offsetChars2[1] - '0') << 4) | (offsetChars2[0] - '0'));
            }
        }

        public void gsmChangeToDeliverPdu(long date, String originator) {
            ByteArrayOutputStream newPdu = new ByteArrayOutputStream(22);
            try {
                newPdu.write((this.mData[0] & 255 & 64) | 4);
                byte[] encodedAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(originator);
                if (encodedAddress != null) {
                    int padding = (encodedAddress[encodedAddress.length + (-1)] & 240) == 240 ? 1 : 0;
                    encodedAddress[0] = (byte) (((encodedAddress[0] - 1) * 2) - padding);
                    newPdu.write(encodedAddress);
                } else {
                    newPdu.write(0);
                    newPdu.write(BluetoothMapContent.MMS_BCC);
                }
                newPdu.write(this.mData[gsmSubmitGetTpPidOffset()]);
                newPdu.write(this.mData[gsmSubmitGetTpDcsOffset()]);
                gsmWriteDate(newPdu, date);
                int userDataLength = this.mData[gsmSubmitGetTpUdlOffset()] & 255;
                newPdu.write(userDataLength);
                newPdu.write(this.mData, gsmSubmitGetTpUdOffset(), this.mData.length - gsmSubmitGetTpUdOffset());
                this.mData = newPdu.toByteArray();
            } catch (IOException e) {
                Log.e(BluetoothMapSmsPdu.TAG, "", e);
                throw new IllegalArgumentException("Failed to change type to deliver PDU.");
            }
        }

        public String getEncodingString() {
            if (this.mType == BluetoothMapSmsPdu.SMS_TYPE_GSM) {
                switch (this.mEncoding) {
                    case 1:
                        if (this.mLanguageTable == 0) {
                            return "G-7BIT";
                        }
                        return "G-7BITEXT";
                    case 2:
                        return "G-8BIT";
                    case 3:
                        return "G-16BIT";
                    default:
                        return "";
                }
            }
            switch (this.mEncoding) {
                case 1:
                    return "C-7ASCII";
                case 2:
                    return "C-8BIT";
                case 3:
                    return "C-UNICODE";
                case 4:
                    return "C-KOREAN";
                default:
                    return "";
            }
        }
    }

    protected static int getNextConcatenatedRef() {
        sConcatenatedRef++;
        return sConcatenatedRef;
    }

    public static ArrayList<SmsPdu> getSubmitPdus(String messageText, String address) {
        byte[] data;
        int activePhone = TelephonyManager.getDefault().getCurrentPhoneType();
        GsmAlphabet.TextEncodingDetails ted = 2 == activePhone ? SmsMessage.calculateLength(messageText, false, true) : com.android.internal.telephony.gsm.SmsMessage.calculateLength(messageText, false);
        int msgCount = ted.msgCount;
        int refNumber = getNextConcatenatedRef() & 255;
        ArrayList<String> smsFragments = android.telephony.SmsMessage.fragmentText(messageText);
        ArrayList<SmsPdu> pdus = new ArrayList<>(msgCount);
        int phoneType = activePhone == 2 ? SMS_TYPE_CDMA : SMS_TYPE_GSM;
        int encoding = ted.codeUnitSize;
        int languageTable = ted.languageTable;
        int languageShiftTable = ted.languageShiftTable;
        String destinationAddress = PhoneNumberUtils.stripSeparators(address);
        if (destinationAddress == null || destinationAddress.length() < 2) {
            destinationAddress = "12";
        }
        if (msgCount == 1) {
            byte[] data2 = android.telephony.SmsMessage.getSubmitPdu(null, destinationAddress, smsFragments.get(0), false).encodedMessage;
            SmsPdu newPdu = new SmsPdu(data2, encoding, phoneType, languageTable);
            pdus.add(newPdu);
        } else {
            for (int i = 0; i < msgCount; i++) {
                SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
                concatRef.refNumber = refNumber;
                concatRef.seqNumber = i + 1;
                concatRef.msgCount = msgCount;
                concatRef.isEightBits = true;
                SmsHeader smsHeader = new SmsHeader();
                smsHeader.concatRef = concatRef;
                if (encoding == 1) {
                    smsHeader.languageTable = languageTable;
                    smsHeader.languageShiftTable = languageShiftTable;
                }
                if (phoneType == SMS_TYPE_GSM) {
                    data = com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu((String) null, destinationAddress, smsFragments.get(i), false, SmsHeader.toByteArray(smsHeader), encoding, languageTable, languageShiftTable).encodedMessage;
                } else {
                    UserData uData = new UserData();
                    uData.payloadStr = smsFragments.get(i);
                    uData.userDataHeader = smsHeader;
                    if (encoding == 1) {
                        uData.msgEncoding = 9;
                    } else {
                        uData.msgEncoding = 4;
                    }
                    uData.msgEncodingSet = true;
                    data = SmsMessage.getSubmitPdu(destinationAddress, uData, false).encodedMessage;
                }
                SmsPdu newPdu2 = new SmsPdu(data, encoding, phoneType, languageTable);
                pdus.add(newPdu2);
            }
        }
        return pdus;
    }

    public static ArrayList<SmsPdu> getDeliverPdus(String messageText, String address, long date) {
        ArrayList<SmsPdu> deliverPdus = getSubmitPdus(messageText, address);
        for (SmsPdu currentPdu : deliverPdus) {
            if (currentPdu.getType() == SMS_TYPE_CDMA) {
                currentPdu.cdmaChangeToDeliverPdu(date);
            } else {
                currentPdu.gsmChangeToDeliverPdu(date, address);
            }
        }
        return deliverPdus;
    }

    public static String decodePdu(byte[] data, int type) {
        if (type == SMS_TYPE_CDMA) {
            String ret = SmsMessage.createFromEfRecord(0, data).getMessageBody();
            return ret;
        }
        String ret2 = gsmParseSubmitPdu(data);
        return ret2;
    }

    private static byte[] gsmStripOffScAddress(byte[] data) {
        int addressLength = data[0] & 255;
        if (addressLength >= data.length) {
            throw new IllegalArgumentException("Length of address exeeds the length of the PDU data.");
        }
        int pduLength = data.length - (addressLength + 1);
        byte[] newData = new byte[pduLength];
        System.arraycopy(data, addressLength + 1, newData, 0, pduLength);
        return newData;
    }

    private static String gsmParseSubmitPdu(byte[] data) {
        String messageBody;
        SmsPdu pdu = new SmsPdu(gsmStripOffScAddress(data), SMS_TYPE_GSM);
        int dataCodingScheme = pdu.gsmSubmitGetTpDcs();
        int encodingType = 0;
        String messageBody2 = null;
        if ((dataCodingScheme & 128) == 0) {
            boolean userDataCompressed = (dataCodingScheme & 32) != 0;
            if (userDataCompressed) {
                Log.w(TAG, "4 - Unsupported SMS data coding scheme (compression) " + (dataCodingScheme & 255));
            } else {
                switch ((dataCodingScheme >> 2) & 3) {
                    case 0:
                        encodingType = 1;
                        break;
                    case 1:
                    case 3:
                        Log.w(TAG, "1 - Unsupported SMS data coding scheme " + (dataCodingScheme & 255));
                        encodingType = 2;
                        break;
                    case 2:
                        encodingType = 3;
                        break;
                }
            }
        } else if ((dataCodingScheme & 240) == 240) {
            encodingType = (dataCodingScheme & 4) == 0 ? 1 : 2;
        } else if ((dataCodingScheme & 240) == 192 || (dataCodingScheme & 240) == 208 || (dataCodingScheme & 240) == 224) {
            if ((dataCodingScheme & 240) == 224) {
                encodingType = 3;
            } else {
                encodingType = 1;
            }
        } else if ((dataCodingScheme & BluetoothShare.STATUS_RUNNING) == 128) {
            if (dataCodingScheme == 132) {
                encodingType = 4;
            } else {
                Log.w(TAG, "5 - Unsupported SMS data coding scheme " + (dataCodingScheme & 255));
            }
        } else {
            Log.w(TAG, "3 - Unsupported SMS data coding scheme " + (dataCodingScheme & 255));
        }
        pdu.setEncoding(encodingType);
        pdu.gsmDecodeUserDataHeader();
        try {
            try {
                switch (encodingType) {
                    case 0:
                    case 2:
                        Log.w(TAG, "Unknown encoding type: " + encodingType);
                        messageBody2 = null;
                        return messageBody2;
                    case 1:
                        messageBody2 = GsmAlphabet.gsm7BitPackedToString(pdu.getData(), pdu.getUserDataMsgOffset(), pdu.getMsgSeptetCount(), pdu.getUserDataSeptetPadding(), pdu.getLanguageTable(), pdu.getLanguageShiftTable());
                        Log.i(TAG, "Decoded as 7BIT: " + messageBody2);
                        return messageBody2;
                    case 3:
                        messageBody = new String(pdu.getData(), pdu.getUserDataMsgOffset(), pdu.getUserDataMsgSize(), "utf-16");
                        Log.i(TAG, "Decoded as 16BIT: " + messageBody);
                        messageBody2 = messageBody;
                        return messageBody2;
                    case 4:
                        messageBody = new String(pdu.getData(), pdu.getUserDataMsgOffset(), pdu.getUserDataMsgSize(), "KSC5601");
                        Log.i(TAG, "Decoded as KSC5601: " + messageBody);
                        messageBody2 = messageBody;
                        return messageBody2;
                    default:
                        return messageBody2;
                }
            } catch (UnsupportedEncodingException e) {
                e = e;
                Log.e(TAG, "Unsupported encoding type???", e);
                return null;
            }
        } catch (UnsupportedEncodingException e2) {
            e = e2;
            Log.e(TAG, "Unsupported encoding type???", e);
            return null;
        }
    }
}
