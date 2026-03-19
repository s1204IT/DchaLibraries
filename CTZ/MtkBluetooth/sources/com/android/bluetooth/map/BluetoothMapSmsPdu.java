package com.android.bluetooth.map;

import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
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
    private static final int INVALID_VALUE = -1;
    public static final int SMS_TYPE_CDMA = 2;
    public static final int SMS_TYPE_GSM = 1;
    private static final String TAG = "BluetoothMapSmsPdu";
    private static final boolean V = false;
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

        SmsPdu(byte[] bArr, int i) {
            this.mScAddress = new byte[]{0};
            this.mUserDataMsgOffset = 0;
            this.mUserDataSeptetPadding = -1;
            this.mMsgSeptetCount = 0;
            this.mData = bArr;
            this.mEncoding = -1;
            this.mType = i;
            this.mLanguageTable = -1;
            this.mLanguageShiftTable = -1;
            this.mUserDataMsgOffset = gsmSubmitGetTpUdOffset();
        }

        SmsPdu(byte[] bArr, int i, int i2, int i3) {
            this.mScAddress = new byte[]{0};
            this.mUserDataMsgOffset = 0;
            this.mUserDataSeptetPadding = -1;
            this.mMsgSeptetCount = 0;
            this.mData = bArr;
            this.mEncoding = i;
            this.mType = i2;
            this.mLanguageTable = i3;
        }

        public byte[] getData() {
            return this.mData;
        }

        public byte[] getScAddress() {
            return this.mScAddress;
        }

        public void setEncoding(int i) {
            this.mEncoding = i;
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

        private int cdmaGetParameterOffset(byte b) {
            boolean z;
            int i;
            boolean z2;
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.mData);
            try {
                byteArrayInputStream.skip(1L);
                i = 0;
                while (true) {
                    try {
                        if (byteArrayInputStream.available() > 0) {
                            int i2 = byteArrayInputStream.read();
                            int i3 = byteArrayInputStream.read();
                            if (i2 == b) {
                                z2 = true;
                                break;
                            }
                            byteArrayInputStream.skip(i3);
                            i += 2 + i3;
                        } else {
                            z2 = false;
                            break;
                        }
                    } catch (Exception e) {
                        e = e;
                        z = false;
                    }
                }
                try {
                    byteArrayInputStream.close();
                } catch (Exception e2) {
                    z = z2;
                    e = e2;
                    Log.e(BluetoothMapSmsPdu.TAG, "cdmaGetParameterOffset: ", e);
                    z2 = z;
                }
            } catch (Exception e3) {
                e = e3;
                z = false;
                i = 0;
            }
            if (z2) {
                return i;
            }
            return 0;
        }

        private int cdmaGetSubParameterOffset(byte b) {
            boolean z;
            boolean z2;
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.mData);
            int iCdmaGetParameterOffset = cdmaGetParameterOffset((byte) 8) + 2;
            byteArrayInputStream.skip(iCdmaGetParameterOffset);
            while (true) {
                try {
                    if (byteArrayInputStream.available() > 0) {
                        int i = byteArrayInputStream.read();
                        int i2 = byteArrayInputStream.read();
                        if (i == b) {
                            z2 = true;
                            break;
                        }
                        byteArrayInputStream.skip(i2);
                        iCdmaGetParameterOffset += i2 + 2;
                    } else {
                        z2 = false;
                        break;
                    }
                } catch (Exception e) {
                    e = e;
                    z = false;
                }
            }
            try {
                byteArrayInputStream.close();
            } catch (Exception e2) {
                z = z2;
                e = e2;
                Log.e(BluetoothMapSmsPdu.TAG, "cdmaGetParameterOffset: ", e);
                z2 = z;
            }
            if (!z2) {
                return 0;
            }
            return iCdmaGetParameterOffset;
        }

        public void cdmaChangeToDeliverPdu(long j) {
            if (this.mData == null) {
                throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
            }
            int iCdmaGetParameterOffset = cdmaGetParameterOffset((byte) 4);
            if (this.mData.length < iCdmaGetParameterOffset) {
                throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
            }
            this.mData[iCdmaGetParameterOffset] = 2;
            int iCdmaGetParameterOffset2 = cdmaGetParameterOffset(DESTINATION_SUB_ADDRESS);
            if (this.mData.length < iCdmaGetParameterOffset2) {
                throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
            }
            this.mData[iCdmaGetParameterOffset2] = ORIGINATING_SUB_ADDRESS;
            int iCdmaGetSubParameterOffset = 2 + cdmaGetSubParameterOffset((byte) 0);
            if (this.mData.length > iCdmaGetSubParameterOffset) {
                this.mData[iCdmaGetSubParameterOffset] = (byte) ((this.mData[iCdmaGetSubParameterOffset] & 255 & 15) | 16);
                return;
            }
            throw new IllegalArgumentException("Unable to convert PDU to Deliver type");
        }

        private int gsmSubmitGetTpPidOffset() {
            int i = (((this.mData[2] + 1) & 255) / 2) + 2 + 2;
            if (i > this.mData.length || i > 14) {
                throw new IllegalArgumentException("wrongly formatted gsm submit PDU. offset = " + i);
            }
            return i;
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
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.mData);
            byteArrayInputStream.skip(gsmSubmitGetTpUdlOffset());
            int i = byteArrayInputStream.read();
            if (gsmSubmitHasUserDataHeader()) {
                int i2 = byteArrayInputStream.read();
                if (this.mEncoding == 1) {
                    byte[] bArr = new byte[i2];
                    try {
                        byteArrayInputStream.read(bArr);
                    } catch (IOException e) {
                        Log.w(BluetoothMapSmsPdu.TAG, "unable to read userDataHeader", e);
                    }
                    SmsHeader smsHeaderFromByteArray = SmsHeader.fromByteArray(bArr);
                    this.mLanguageTable = smsHeaderFromByteArray.languageTable;
                    this.mLanguageShiftTable = smsHeaderFromByteArray.languageShiftTable;
                    int i3 = (i2 + 1) * 8;
                    int i4 = (i3 / 7) + (i3 % 7 > 0 ? 1 : 0);
                    this.mUserDataSeptetPadding = (i4 * 7) - i3;
                    this.mMsgSeptetCount = i - i4;
                }
                this.mUserDataMsgOffset = gsmSubmitGetTpUdOffset() + i2 + 1;
                return;
            }
            this.mUserDataSeptetPadding = 0;
            this.mMsgSeptetCount = i;
            this.mUserDataMsgOffset = gsmSubmitGetTpUdOffset();
        }

        private void gsmWriteDate(ByteArrayOutputStream byteArrayOutputStream, long j) throws UnsupportedEncodingException {
            String str = new SimpleDateFormat("yyMMddHHmmss").format(new Date(j));
            byte[] bytes = str.getBytes("US-ASCII");
            int length = str.length();
            for (int i = 0; i < length; i += 2) {
                byteArrayOutputStream.write(((bytes[i + 1] - 48) << 4) | (bytes[i] - 48));
            }
            Calendar calendar = Calendar.getInstance();
            int i2 = (calendar.get(15) + calendar.get(16)) / 900000;
            if (i2 < 0) {
                char[] charArray = String.format("%1$02d", Integer.valueOf(-i2)).toCharArray();
                byteArrayOutputStream.write(((charArray[1] - '0') << 4) | 64 | (charArray[0] - '0'));
            } else {
                char[] charArray2 = String.format("%1$02d", Integer.valueOf(i2)).toCharArray();
                byteArrayOutputStream.write(((charArray2[1] - '0') << 4) | (charArray2[0] - '0'));
            }
        }

        public void gsmChangeToDeliverPdu(long j, String str) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(22);
            try {
                byteArrayOutputStream.write(4 | (this.mData[0] & 255 & 64));
                byte[] bArrNetworkPortionToCalledPartyBCDWithLength = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(str);
                if (bArrNetworkPortionToCalledPartyBCDWithLength != null) {
                    bArrNetworkPortionToCalledPartyBCDWithLength[0] = (byte) (((bArrNetworkPortionToCalledPartyBCDWithLength[0] - 1) * 2) - ((bArrNetworkPortionToCalledPartyBCDWithLength[bArrNetworkPortionToCalledPartyBCDWithLength.length - 1] & 240) == 240 ? 1 : 0));
                    byteArrayOutputStream.write(bArrNetworkPortionToCalledPartyBCDWithLength);
                } else {
                    byteArrayOutputStream.write(0);
                    byteArrayOutputStream.write(129);
                }
                byteArrayOutputStream.write(this.mData[gsmSubmitGetTpPidOffset()]);
                byteArrayOutputStream.write(this.mData[gsmSubmitGetTpDcsOffset()]);
                gsmWriteDate(byteArrayOutputStream, j);
                byteArrayOutputStream.write(this.mData[gsmSubmitGetTpUdlOffset()] & 255);
                byteArrayOutputStream.write(this.mData, gsmSubmitGetTpUdOffset(), this.mData.length - gsmSubmitGetTpUdOffset());
                this.mData = byteArrayOutputStream.toByteArray();
            } catch (IOException e) {
                Log.e(BluetoothMapSmsPdu.TAG, "", e);
                throw new IllegalArgumentException("Failed to change type to deliver PDU.");
            }
        }

        public String getEncodingString() {
            if (this.mType == 1) {
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

    public static ArrayList<SmsPdu> getSubmitPdus(String str, String str2) {
        GsmAlphabet.TextEncodingDetails textEncodingDetailsCalculateLength;
        String str3;
        int i;
        int i2;
        int i3;
        boolean z;
        byte[] bArr;
        int currentPhoneType = TelephonyManager.getDefault().getCurrentPhoneType();
        if (2 == currentPhoneType) {
            textEncodingDetailsCalculateLength = SmsMessage.calculateLength(str, false, true);
        } else {
            textEncodingDetailsCalculateLength = com.android.internal.telephony.gsm.SmsMessage.calculateLength(str, false);
        }
        int i4 = textEncodingDetailsCalculateLength.msgCount;
        int nextConcatenatedRef = getNextConcatenatedRef() & 255;
        ArrayList arrayListFragmentText = android.telephony.SmsMessage.fragmentText(str);
        ArrayList<SmsPdu> arrayList = new ArrayList<>(i4);
        int i5 = currentPhoneType == 2 ? 2 : 1;
        int i6 = textEncodingDetailsCalculateLength.codeUnitSize;
        int i7 = textEncodingDetailsCalculateLength.languageTable;
        int i8 = textEncodingDetailsCalculateLength.languageShiftTable;
        String strStripSeparators = PhoneNumberUtils.stripSeparators(str2);
        if (strStripSeparators == null || strStripSeparators.length() < 2) {
            str3 = "12";
        } else {
            str3 = strStripSeparators;
        }
        if (i4 == 1) {
            arrayList.add(new SmsPdu(android.telephony.SmsMessage.getSubmitPdu(null, str3, (String) arrayListFragmentText.get(0), false).encodedMessage, i6, i5, i7));
        } else {
            int i9 = 0;
            while (i9 < i4) {
                SmsHeader.ConcatRef concatRef = new SmsHeader.ConcatRef();
                concatRef.refNumber = nextConcatenatedRef;
                int i10 = i9 + 1;
                concatRef.seqNumber = i10;
                concatRef.msgCount = i4;
                concatRef.isEightBits = true;
                SmsHeader smsHeader = new SmsHeader();
                smsHeader.concatRef = concatRef;
                if (i6 == 1) {
                    smsHeader.languageTable = i7;
                    smsHeader.languageShiftTable = i8;
                }
                if (i5 != 1) {
                    i = i10;
                    i2 = i7;
                    i3 = i6;
                    UserData userData = new UserData();
                    userData.payloadStr = (String) arrayListFragmentText.get(i9);
                    userData.userDataHeader = smsHeader;
                    if (i3 == 1) {
                        userData.msgEncoding = 9;
                    } else {
                        userData.msgEncoding = 4;
                    }
                    userData.msgEncodingSet = true;
                    z = false;
                    bArr = ((SmsMessageBase.SubmitPduBase) SmsMessage.getSubmitPdu(str3, userData, false)).encodedMessage;
                } else {
                    i = i10;
                    i2 = i7;
                    i3 = i6;
                    bArr = ((SmsMessageBase.SubmitPduBase) com.android.internal.telephony.gsm.SmsMessage.getSubmitPdu((String) null, str3, (String) arrayListFragmentText.get(i9), false, SmsHeader.toByteArray(smsHeader), i6, i2, i8)).encodedMessage;
                    z = false;
                }
                int i11 = i2;
                arrayList.add(new SmsPdu(bArr, i3, i5, i11));
                i6 = i3;
                i7 = i11;
                i9 = i;
            }
        }
        return arrayList;
    }

    public static ArrayList<SmsPdu> getDeliverPdus(String str, String str2, long j) {
        ArrayList<SmsPdu> submitPdus = getSubmitPdus(str, str2);
        for (SmsPdu smsPdu : submitPdus) {
            if (smsPdu.getType() == 2) {
                smsPdu.cdmaChangeToDeliverPdu(j);
            } else {
                smsPdu.gsmChangeToDeliverPdu(j, str2);
            }
        }
        return submitPdus;
    }

    public static String decodePdu(byte[] bArr, int i) {
        if (i == 2) {
            return SmsMessage.createFromEfRecord(0, bArr).getMessageBody();
        }
        return gsmParseSubmitPdu(bArr);
    }

    private static byte[] gsmStripOffScAddress(byte[] bArr) {
        int i = bArr[0] & 255;
        if (i >= bArr.length) {
            throw new IllegalArgumentException("Length of address exeeds the length of the PDU data.");
        }
        int i2 = 1 + i;
        int length = bArr.length - i2;
        byte[] bArr2 = new byte[length];
        System.arraycopy(bArr, i2, bArr2, 0, length);
        return bArr2;
    }

    private static String gsmParseSubmitPdu(byte[] bArr) {
        String str;
        int i = 1;
        SmsPdu smsPdu = new SmsPdu(gsmStripOffScAddress(bArr), 1);
        int iGsmSubmitGetTpDcs = smsPdu.gsmSubmitGetTpDcs();
        if ((iGsmSubmitGetTpDcs & 128) == 0) {
            if (!((iGsmSubmitGetTpDcs & 32) != 0)) {
                switch ((iGsmSubmitGetTpDcs >> 2) & 3) {
                    case 0:
                        break;
                    case 1:
                    case 3:
                        Log.w(TAG, "1 - Unsupported SMS data coding scheme " + (iGsmSubmitGetTpDcs & 255));
                        i = 2;
                        break;
                    case 2:
                        i = 3;
                        break;
                    default:
                        i = 0;
                        break;
                }
            } else {
                Log.w(TAG, "4 - Unsupported SMS data coding scheme (compression) " + (iGsmSubmitGetTpDcs & 255));
                i = 0;
            }
        } else {
            int i2 = iGsmSubmitGetTpDcs & 240;
            if (i2 == 240) {
                if ((iGsmSubmitGetTpDcs & 4) != 0) {
                    i = 2;
                }
            } else if (i2 != 192 && i2 != 208 && i2 != 224) {
                if ((iGsmSubmitGetTpDcs & BluetoothShare.STATUS_RUNNING) != 128) {
                    Log.w(TAG, "3 - Unsupported SMS data coding scheme " + (iGsmSubmitGetTpDcs & 255));
                } else if (iGsmSubmitGetTpDcs == 132) {
                    i = 4;
                } else {
                    Log.w(TAG, "5 - Unsupported SMS data coding scheme " + (iGsmSubmitGetTpDcs & 255));
                }
                i = 0;
            } else if (i2 == 224) {
                i = 3;
            }
        }
        smsPdu.setEncoding(i);
        smsPdu.gsmDecodeUserDataHeader();
        try {
            switch (i) {
                case 0:
                case 2:
                    Log.w(TAG, "Unknown encoding type: " + i);
                    return null;
                case 1:
                    String strGsm7BitPackedToString = GsmAlphabet.gsm7BitPackedToString(smsPdu.getData(), smsPdu.getUserDataMsgOffset(), smsPdu.getMsgSeptetCount(), smsPdu.getUserDataSeptetPadding(), smsPdu.getLanguageTable(), smsPdu.getLanguageShiftTable());
                    Log.i(TAG, "Decoded as 7BIT: " + strGsm7BitPackedToString);
                    return strGsm7BitPackedToString;
                case 3:
                    str = new String(smsPdu.getData(), smsPdu.getUserDataMsgOffset(), smsPdu.getUserDataMsgSize(), "utf-16");
                    Log.i(TAG, "Decoded as 16BIT: " + str);
                    break;
                case 4:
                    str = new String(smsPdu.getData(), smsPdu.getUserDataMsgOffset(), smsPdu.getUserDataMsgSize(), "KSC5601");
                    Log.i(TAG, "Decoded as KSC5601: " + str);
                    break;
                default:
                    return null;
            }
            return str;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding type???", e);
            return null;
        }
    }
}
