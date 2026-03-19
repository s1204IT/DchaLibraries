package com.android.internal.telephony.cdma;

import android.content.res.Resources;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaSmsCbProgramData;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.Sms7BitEncodingTranslator;
import com.android.internal.telephony.SmsAddress;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.CdmaSmsSubaddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.util.BitwiseInputStream;
import com.android.internal.util.HexDump;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class SmsMessage extends SmsMessageBase {
    private static final byte BEARER_DATA = 8;
    private static final byte BEARER_REPLY_OPTION = 6;
    private static final byte CAUSE_CODES = 7;
    private static final byte DESTINATION_ADDRESS = 4;
    private static final byte DESTINATION_SUB_ADDRESS = 5;
    private static final String LOGGABLE_TAG = "CDMA:SMS";
    static final String LOG_TAG = "SmsMessage";
    private static final byte ORIGINATING_ADDRESS = 2;
    private static final byte ORIGINATING_SUB_ADDRESS = 3;
    private static final int PRIORITY_EMERGENCY = 3;
    private static final int PRIORITY_INTERACTIVE = 1;
    private static final int PRIORITY_NORMAL = 0;
    private static final int PRIORITY_URGENT = 2;
    private static final int RETURN_ACK = 1;
    private static final int RETURN_NO_ACK = 0;
    private static final byte SERVICE_CATEGORY = 1;
    private static final byte TELESERVICE_IDENTIFIER = 0;
    private static final boolean VDBG = false;
    public BearerData mBearerData;
    public SmsEnvelope mEnvelope;
    public int status;

    public static class SubmitPdu extends SmsMessageBase.SubmitPduBase {
    }

    public SmsMessage(SmsAddress smsAddress, SmsEnvelope smsEnvelope) {
        this.mOriginatingAddress = smsAddress;
        this.mEnvelope = smsEnvelope;
        createPdu();
    }

    public SmsMessage() {
    }

    public static SmsMessage createFromPdu(byte[] bArr) {
        SmsMessage smsMessage = new SmsMessage();
        try {
            smsMessage.parsePdu(bArr);
            return smsMessage;
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "SMS PDU parsing failed with out of memory: ", e);
            return null;
        } catch (RuntimeException e2) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", e2);
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
            int i2 = bArr[1] & 255;
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, 2, bArr2, 0, i2);
            smsMessage.parsePduFromEfRecord(bArr2);
            return smsMessage;
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", e);
            return null;
        }
    }

    public static int getTPLayerLengthForPDU(String str) {
        Rlog.w(LOG_TAG, "getTPLayerLengthForPDU: is not supported in CDMA mode.");
        return 0;
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader) {
        return getSubmitPdu(str, str2, str3, z, smsHeader, -1);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i) {
        if (str3 == null || str2 == null) {
            return null;
        }
        UserData userData = new UserData();
        userData.payloadStr = str3;
        userData.userDataHeader = smsHeader;
        return privateGetSubmitPdu(str2, z, userData, i);
    }

    public static SubmitPdu getSubmitPdu(String str, String str2, int i, byte[] bArr, boolean z) {
        SmsHeader.PortAddrs portAddrs = new SmsHeader.PortAddrs();
        portAddrs.destPort = i;
        portAddrs.origPort = 0;
        portAddrs.areEightBits = false;
        SmsHeader smsHeader = new SmsHeader();
        smsHeader.portAddrs = portAddrs;
        UserData userData = new UserData();
        userData.userDataHeader = smsHeader;
        userData.msgEncoding = 0;
        userData.msgEncodingSet = true;
        userData.payload = bArr;
        return privateGetSubmitPdu(str2, z, userData);
    }

    public static SubmitPdu getSubmitPdu(String str, UserData userData, boolean z) {
        return privateGetSubmitPdu(str, z, userData);
    }

    public static SubmitPdu getSubmitPdu(String str, UserData userData, boolean z, int i) {
        return privateGetSubmitPdu(str, z, userData, i);
    }

    @Override
    public int getProtocolIdentifier() {
        Rlog.w(LOG_TAG, "getProtocolIdentifier: is not supported in CDMA mode.");
        return 0;
    }

    @Override
    public boolean isReplace() {
        Rlog.w(LOG_TAG, "isReplace: is not supported in CDMA mode.");
        return false;
    }

    @Override
    public boolean isCphsMwiMessage() {
        Rlog.w(LOG_TAG, "isCphsMwiMessage: is not supported in CDMA mode.");
        return false;
    }

    @Override
    public boolean isMWIClearMessage() {
        return this.mBearerData != null && this.mBearerData.numberOfMessages == 0;
    }

    @Override
    public boolean isMWISetMessage() {
        return this.mBearerData != null && this.mBearerData.numberOfMessages > 0;
    }

    @Override
    public boolean isMwiDontStore() {
        return this.mBearerData != null && this.mBearerData.numberOfMessages > 0 && this.mBearerData.userData == null;
    }

    @Override
    public int getStatus() {
        return this.status << 16;
    }

    @Override
    public boolean isStatusReportMessage() {
        return this.mBearerData != null && this.mBearerData.messageType == 4;
    }

    @Override
    public boolean isReplyPathPresent() {
        Rlog.w(LOG_TAG, "isReplyPathPresent: is not supported in CDMA mode.");
        return false;
    }

    public static GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z, boolean z2) {
        String strTranslate;
        if (Resources.getSystem().getBoolean(R.bool.config_sms_force_7bit_encoding)) {
            strTranslate = Sms7BitEncodingTranslator.translate(charSequence);
        } else {
            strTranslate = null;
        }
        if (!TextUtils.isEmpty(strTranslate)) {
            charSequence = strTranslate;
        }
        return BearerData.calcTextEncodingDetails(charSequence, z, z2);
    }

    public int getTeleService() {
        return this.mEnvelope.teleService;
    }

    public int getMessageType() {
        if (this.mEnvelope.serviceCategory != 0) {
            return 1;
        }
        return 0;
    }

    private void parsePdu(byte[] bArr) {
        int unsignedByte;
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArr));
        SmsEnvelope smsEnvelope = new SmsEnvelope();
        CdmaSmsAddress cdmaSmsAddress = new CdmaSmsAddress();
        CdmaSmsSubaddress cdmaSmsSubaddress = new CdmaSmsSubaddress();
        try {
            smsEnvelope.messageType = dataInputStream.readInt();
            smsEnvelope.teleService = dataInputStream.readInt();
            smsEnvelope.serviceCategory = dataInputStream.readInt();
            cdmaSmsAddress.digitMode = dataInputStream.readByte();
            cdmaSmsAddress.numberMode = dataInputStream.readByte();
            cdmaSmsAddress.ton = dataInputStream.readByte();
            cdmaSmsAddress.numberPlan = dataInputStream.readByte();
            unsignedByte = dataInputStream.readUnsignedByte();
            cdmaSmsAddress.numberOfDigits = unsignedByte;
        } catch (IOException e) {
            throw new RuntimeException("createFromPdu: conversion from byte array to object failed: " + e, e);
        } catch (Exception e2) {
            Rlog.e(LOG_TAG, "createFromPdu: conversion from byte array to object failed: " + e2);
        }
        if (unsignedByte > bArr.length) {
            throw new RuntimeException("createFromPdu: Invalid pdu, addr.numberOfDigits " + unsignedByte + " > pdu len " + bArr.length);
        }
        cdmaSmsAddress.origBytes = new byte[unsignedByte];
        dataInputStream.read(cdmaSmsAddress.origBytes, 0, unsignedByte);
        smsEnvelope.bearerReply = dataInputStream.readInt();
        smsEnvelope.replySeqNo = dataInputStream.readByte();
        smsEnvelope.errorClass = dataInputStream.readByte();
        smsEnvelope.causeCode = dataInputStream.readByte();
        int i = dataInputStream.readInt();
        if (i > bArr.length) {
            throw new RuntimeException("createFromPdu: Invalid pdu, bearerDataLength " + i + " > pdu len " + bArr.length);
        }
        smsEnvelope.bearerData = new byte[i];
        dataInputStream.read(smsEnvelope.bearerData, 0, i);
        dataInputStream.close();
        this.mOriginatingAddress = cdmaSmsAddress;
        smsEnvelope.origAddress = cdmaSmsAddress;
        smsEnvelope.origSubaddress = cdmaSmsSubaddress;
        this.mEnvelope = smsEnvelope;
        this.mPdu = bArr;
        parseSms();
    }

    public void parsePduFromEfRecord(byte[] bArr) {
        int i;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        SmsEnvelope smsEnvelope = new SmsEnvelope();
        CdmaSmsAddress cdmaSmsAddress = new CdmaSmsAddress();
        CdmaSmsSubaddress cdmaSmsSubaddress = new CdmaSmsSubaddress();
        try {
            smsEnvelope.messageType = dataInputStream.readByte();
            while (dataInputStream.available() > 0) {
                byte b = dataInputStream.readByte();
                int unsignedByte = dataInputStream.readUnsignedByte();
                byte[] bArr2 = new byte[unsignedByte];
                int i2 = 0;
                switch (b) {
                    case 0:
                        smsEnvelope.teleService = dataInputStream.readUnsignedShort();
                        Rlog.i(LOG_TAG, "teleservice = " + smsEnvelope.teleService);
                        break;
                    case 1:
                        smsEnvelope.serviceCategory = dataInputStream.readUnsignedShort();
                        break;
                    case 2:
                    case 4:
                        dataInputStream.read(bArr2, 0, unsignedByte);
                        BitwiseInputStream bitwiseInputStream = new BitwiseInputStream(bArr2);
                        cdmaSmsAddress.digitMode = bitwiseInputStream.read(1);
                        cdmaSmsAddress.numberMode = bitwiseInputStream.read(1);
                        if (cdmaSmsAddress.digitMode == 1) {
                            i = bitwiseInputStream.read(3);
                            cdmaSmsAddress.ton = i;
                            if (cdmaSmsAddress.numberMode == 0) {
                                cdmaSmsAddress.numberPlan = bitwiseInputStream.read(4);
                            }
                        } else {
                            i = 0;
                        }
                        cdmaSmsAddress.numberOfDigits = bitwiseInputStream.read(8);
                        byte[] bArr3 = new byte[cdmaSmsAddress.numberOfDigits];
                        if (cdmaSmsAddress.digitMode == 0) {
                            while (i2 < cdmaSmsAddress.numberOfDigits) {
                                bArr3[i2] = convertDtmfToAscii((byte) (15 & bitwiseInputStream.read(4)));
                                i2++;
                            }
                        } else if (cdmaSmsAddress.digitMode == 1) {
                            if (cdmaSmsAddress.numberMode == 0) {
                                while (i2 < cdmaSmsAddress.numberOfDigits) {
                                    bArr3[i2] = (byte) (bitwiseInputStream.read(8) & 255);
                                    i2++;
                                }
                            } else if (cdmaSmsAddress.numberMode == 1) {
                                if (i == 2) {
                                    Rlog.e(LOG_TAG, "TODO: Originating Addr is email id");
                                } else {
                                    Rlog.e(LOG_TAG, "TODO: Originating Addr is data network address");
                                }
                            } else {
                                Rlog.e(LOG_TAG, "Originating Addr is of incorrect type");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "Incorrect Digit mode");
                        }
                        cdmaSmsAddress.origBytes = bArr3;
                        Rlog.i(LOG_TAG, "Originating Addr=" + cdmaSmsAddress.toString());
                        break;
                    case 3:
                    case 5:
                        dataInputStream.read(bArr2, 0, unsignedByte);
                        BitwiseInputStream bitwiseInputStream2 = new BitwiseInputStream(bArr2);
                        cdmaSmsSubaddress.type = bitwiseInputStream2.read(3);
                        cdmaSmsSubaddress.odd = bitwiseInputStream2.readByteArray(1)[0];
                        int i3 = bitwiseInputStream2.read(8);
                        byte[] bArr4 = new byte[i3];
                        while (i2 < i3) {
                            bArr4[i2] = convertDtmfToAscii((byte) (bitwiseInputStream2.read(4) & 255));
                            i2++;
                        }
                        cdmaSmsSubaddress.origBytes = bArr4;
                        break;
                    case 6:
                        dataInputStream.read(bArr2, 0, unsignedByte);
                        smsEnvelope.bearerReply = new BitwiseInputStream(bArr2).read(6);
                        break;
                    case 7:
                        dataInputStream.read(bArr2, 0, unsignedByte);
                        BitwiseInputStream bitwiseInputStream3 = new BitwiseInputStream(bArr2);
                        smsEnvelope.replySeqNo = bitwiseInputStream3.readByteArray(6)[0];
                        smsEnvelope.errorClass = bitwiseInputStream3.readByteArray(2)[0];
                        if (smsEnvelope.errorClass != 0) {
                            smsEnvelope.causeCode = bitwiseInputStream3.readByteArray(8)[0];
                        }
                        break;
                    case 8:
                        dataInputStream.read(bArr2, 0, unsignedByte);
                        smsEnvelope.bearerData = bArr2;
                        break;
                    default:
                        throw new Exception("unsupported parameterId (" + ((int) b) + ")");
                }
            }
            byteArrayInputStream.close();
            dataInputStream.close();
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "parsePduFromEfRecord: conversion from pdu to SmsMessage failed" + e);
        }
        this.mOriginatingAddress = cdmaSmsAddress;
        smsEnvelope.origAddress = cdmaSmsAddress;
        smsEnvelope.origSubaddress = cdmaSmsSubaddress;
        this.mEnvelope = smsEnvelope;
        this.mPdu = bArr;
        parseSms();
    }

    public void parseSms() {
        if (this.mEnvelope.teleService == 262144) {
            this.mBearerData = new BearerData();
            if (this.mEnvelope.bearerData != null) {
                this.mBearerData.numberOfMessages = this.mEnvelope.bearerData[0] & 255;
                return;
            }
            return;
        }
        this.mBearerData = BearerData.decode(this.mEnvelope.bearerData);
        if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
            Rlog.d(LOG_TAG, "MT raw BearerData = '" + HexDump.toHexString(this.mEnvelope.bearerData) + "'");
            StringBuilder sb = new StringBuilder();
            sb.append("MT (decoded) BearerData = ");
            sb.append(this.mBearerData);
            Rlog.d(LOG_TAG, sb.toString());
        }
        if (this.mBearerData == null) {
            return;
        }
        this.mMessageRef = this.mBearerData.messageId;
        if (this.mBearerData.userData != null) {
            this.mUserData = this.mBearerData.userData.payload;
            this.mUserDataHeader = this.mBearerData.userData.userDataHeader;
            this.mMessageBody = this.mBearerData.userData.payloadStr;
        }
        if (this.mOriginatingAddress != null) {
            this.mOriginatingAddress.address = new String(this.mOriginatingAddress.origBytes);
            if (this.mOriginatingAddress.ton == 1 && this.mOriginatingAddress.address.charAt(0) != '+') {
                this.mOriginatingAddress.address = "+" + this.mOriginatingAddress.address;
            }
        }
        if (this.mBearerData.msgCenterTimeStamp != null) {
            this.mScTimeMillis = this.mBearerData.msgCenterTimeStamp.toMillis(true);
        }
        if (this.mBearerData.messageType == 4) {
            if (!this.mBearerData.messageStatusSet) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("DELIVERY_ACK message without msgStatus (");
                sb2.append(this.mUserData == null ? "also missing" : "does have");
                sb2.append(" userData).");
                Rlog.d(LOG_TAG, sb2.toString());
                this.status = 0;
            } else {
                this.status = this.mBearerData.errorClass << 8;
                this.status |= this.mBearerData.messageStatus;
            }
        } else if (this.mBearerData.messageType != 1) {
            throw new RuntimeException("Unsupported message type: " + this.mBearerData.messageType);
        }
        if (this.mMessageBody != null) {
            parseMessageBody();
        } else {
            byte[] bArr = this.mUserData;
        }
    }

    public SmsCbMessage parseBroadcastSms() {
        BearerData bearerDataDecode = BearerData.decode(this.mEnvelope.bearerData, this.mEnvelope.serviceCategory);
        if (bearerDataDecode == null) {
            Rlog.w(LOG_TAG, "BearerData.decode() returned null");
            return null;
        }
        if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
            Rlog.d(LOG_TAG, "MT raw BearerData = " + HexDump.toHexString(this.mEnvelope.bearerData));
        }
        return new SmsCbMessage(2, 1, bearerDataDecode.messageId, new SmsCbLocation(TelephonyManager.getDefault().getNetworkOperator()), this.mEnvelope.serviceCategory, bearerDataDecode.getLanguage(), bearerDataDecode.userData.payloadStr, bearerDataDecode.priority, null, bearerDataDecode.cmasWarningInfo);
    }

    @Override
    public SmsConstants.MessageClass getMessageClass() {
        if (this.mBearerData != null && this.mBearerData.displayMode == 0) {
            return SmsConstants.MessageClass.CLASS_0;
        }
        return SmsConstants.MessageClass.UNKNOWN;
    }

    public static synchronized int getNextMessageId() {
        int i;
        i = SystemProperties.getInt(TelephonyProperties.PROPERTY_CDMA_MSG_ID, 1);
        String string = Integer.toString((i % 65535) + 1);
        try {
            SystemProperties.set(TelephonyProperties.PROPERTY_CDMA_MSG_ID, string);
            if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
                Rlog.d(LOG_TAG, "next persist.radio.cdma.msgid = " + string);
                Rlog.d(LOG_TAG, "readback gets " + SystemProperties.get(TelephonyProperties.PROPERTY_CDMA_MSG_ID));
            }
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "set nextMessage ID failed: " + e);
        }
        return i;
    }

    public static SubmitPdu privateGetSubmitPdu(String str, boolean z, UserData userData) {
        return privateGetSubmitPdu(str, z, userData, -1);
    }

    private static SubmitPdu privateGetSubmitPdu(String str, boolean z, UserData userData, int i) {
        CdmaSmsAddress cdmaSmsAddress = CdmaSmsAddress.parse(PhoneNumberUtils.cdmaCheckAndProcessPlusCodeForSms(str));
        if (cdmaSmsAddress == null) {
            return null;
        }
        if (cdmaSmsAddress.numberOfDigits > 36) {
            Rlog.d(LOG_TAG, "number of digit exceeds the SMS_ADDRESS_MAX");
            return null;
        }
        BearerData bearerData = new BearerData();
        bearerData.messageType = 2;
        bearerData.messageId = getNextMessageId();
        bearerData.deliveryAckReq = z;
        bearerData.userAckReq = false;
        bearerData.readAckReq = false;
        bearerData.reportReq = false;
        if (i >= 0 && i <= 3) {
            bearerData.priorityIndicatorSet = true;
            bearerData.priority = i;
        }
        bearerData.userData = userData;
        byte[] bArrEncode = BearerData.encode(bearerData);
        if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
            Rlog.d(LOG_TAG, "MO (encoded) BearerData = " + bearerData);
            Rlog.d(LOG_TAG, "MO raw BearerData = '" + HexDump.toHexString(bArrEncode) + "'");
        }
        if (bArrEncode == null) {
            return null;
        }
        int i2 = bearerData.hasUserDataHeader ? 4101 : 4098;
        SmsEnvelope smsEnvelope = new SmsEnvelope();
        smsEnvelope.messageType = 0;
        smsEnvelope.teleService = i2;
        smsEnvelope.destAddress = cdmaSmsAddress;
        smsEnvelope.bearerReply = 1;
        smsEnvelope.bearerData = bArrEncode;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(100);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(smsEnvelope.teleService);
            dataOutputStream.writeInt(0);
            dataOutputStream.writeInt(0);
            dataOutputStream.write(cdmaSmsAddress.digitMode);
            dataOutputStream.write(cdmaSmsAddress.numberMode);
            dataOutputStream.write(cdmaSmsAddress.ton);
            dataOutputStream.write(cdmaSmsAddress.numberPlan);
            dataOutputStream.write(cdmaSmsAddress.numberOfDigits);
            dataOutputStream.write(cdmaSmsAddress.origBytes, 0, cdmaSmsAddress.origBytes.length);
            dataOutputStream.write(0);
            dataOutputStream.write(0);
            dataOutputStream.write(0);
            dataOutputStream.write(bArrEncode.length);
            dataOutputStream.write(bArrEncode, 0, bArrEncode.length);
            dataOutputStream.close();
            SubmitPdu submitPdu = new SubmitPdu();
            submitPdu.encodedMessage = byteArrayOutputStream.toByteArray();
            submitPdu.encodedScAddress = null;
            return submitPdu;
        } catch (IOException e) {
            Rlog.e(LOG_TAG, "creating SubmitPdu failed: " + e);
            return null;
        }
    }

    public void createPdu() {
        SmsEnvelope smsEnvelope = this.mEnvelope;
        CdmaSmsAddress cdmaSmsAddress = smsEnvelope.origAddress;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(100);
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));
        try {
            dataOutputStream.writeInt(smsEnvelope.messageType);
            dataOutputStream.writeInt(smsEnvelope.teleService);
            dataOutputStream.writeInt(smsEnvelope.serviceCategory);
            dataOutputStream.writeByte(cdmaSmsAddress.digitMode);
            dataOutputStream.writeByte(cdmaSmsAddress.numberMode);
            dataOutputStream.writeByte(cdmaSmsAddress.ton);
            dataOutputStream.writeByte(cdmaSmsAddress.numberPlan);
            dataOutputStream.writeByte(cdmaSmsAddress.numberOfDigits);
            dataOutputStream.write(cdmaSmsAddress.origBytes, 0, cdmaSmsAddress.origBytes.length);
            dataOutputStream.writeInt(smsEnvelope.bearerReply);
            dataOutputStream.writeByte(smsEnvelope.replySeqNo);
            dataOutputStream.writeByte(smsEnvelope.errorClass);
            dataOutputStream.writeByte(smsEnvelope.causeCode);
            dataOutputStream.writeInt(smsEnvelope.bearerData.length);
            dataOutputStream.write(smsEnvelope.bearerData, 0, smsEnvelope.bearerData.length);
            dataOutputStream.close();
            this.mPdu = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Rlog.e(LOG_TAG, "createPdu: conversion from object to byte array failed: " + e);
        }
    }

    public static byte convertDtmfToAscii(byte b) {
        switch (b) {
            case 0:
                return (byte) 68;
            case 1:
                return (byte) 49;
            case 2:
                return (byte) 50;
            case 3:
                return (byte) 51;
            case 4:
                return (byte) 52;
            case 5:
                return (byte) 53;
            case 6:
                return (byte) 54;
            case 7:
                return (byte) 55;
            case 8:
                return (byte) 56;
            case 9:
                return (byte) 57;
            case 10:
                return (byte) 48;
            case 11:
                return (byte) 42;
            case 12:
                return (byte) 35;
            case 13:
                return (byte) 65;
            case 14:
                return (byte) 66;
            case 15:
                return (byte) 67;
            default:
                return (byte) 32;
        }
    }

    public int getNumOfVoicemails() {
        if (this.mBearerData != null) {
            return this.mBearerData.numberOfMessages;
        }
        return 0;
    }

    public byte[] getIncomingSmsFingerprint() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(this.mEnvelope.serviceCategory);
        byteArrayOutputStream.write(this.mEnvelope.teleService);
        byteArrayOutputStream.write(this.mEnvelope.origAddress.origBytes, 0, this.mEnvelope.origAddress.origBytes.length);
        byteArrayOutputStream.write(this.mEnvelope.bearerData, 0, this.mEnvelope.bearerData.length);
        if (this.mEnvelope.origSubaddress != null && this.mEnvelope.origSubaddress.origBytes != null) {
            byteArrayOutputStream.write(this.mEnvelope.origSubaddress.origBytes, 0, this.mEnvelope.origSubaddress.origBytes.length);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public ArrayList<CdmaSmsCbProgramData> getSmsCbProgramData() {
        if (this.mBearerData != null) {
            return this.mBearerData.serviceCategoryProgramData;
        }
        return null;
    }
}
