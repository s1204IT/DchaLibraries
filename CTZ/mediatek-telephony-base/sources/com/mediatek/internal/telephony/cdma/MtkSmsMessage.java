package com.mediatek.internal.telephony.cdma;

import android.R;
import android.content.res.Resources;
import android.telephony.Rlog;
import android.telephony.SmsCbEtwsInfo;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsAddress;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.util.HexDump;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MtkSmsMessage extends SmsMessage {
    private static final String LOGGABLE_TAG = "CDMA:SMS";
    private static final String LOG_TAG = "MtkCdmaSmsMessage";
    private static final int RETURN_ACK = 1;
    private static IPlusCodeUtils sPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();

    public static SmsMessage createFromEfRecord(int i, byte[] bArr) {
        try {
            MtkSmsMessage mtkSmsMessage = new MtkSmsMessage();
            ((SmsMessage) mtkSmsMessage).mIndexOnIcc = i;
            if ((bArr[0] & 1) == 0) {
                Rlog.w(LOG_TAG, "SMS parsing failed: Trying to parse a free record");
                return null;
            }
            ((SmsMessage) mtkSmsMessage).mStatusOnIcc = bArr[0] & 7;
            int i2 = bArr[1] & 255;
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, 2, bArr2, 0, i2);
            mtkSmsMessage.parsePduFromEfRecord(bArr2);
            return mtkSmsMessage;
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "SMS PDU parsing failed: ", e);
            return null;
        }
    }

    public void parseSms() {
        try {
            super.parseSms();
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Unsupported message type: 2")) {
                if (this.mMessageBody != null) {
                    parseMessageBody();
                    return;
                }
                return;
            }
            throw e;
        }
    }

    public String getOriginatingAddress() {
        replaceIddNddWithPluscode(this.mOriginatingAddress);
        return super.getOriginatingAddress();
    }

    public SmsCbMessage parseBroadcastSms() {
        BearerData bearerDataDecode = MtkBearerData.decode(this.mEnvelope.bearerData, this.mEnvelope.serviceCategory);
        if (bearerDataDecode == null) {
            Rlog.w(LOG_TAG, "BearerData.decode() returned null");
            return null;
        }
        if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
            Rlog.d(LOG_TAG, "MT raw BearerData = " + HexDump.toHexString(this.mEnvelope.bearerData));
        }
        return new SmsCbMessage(2, 1, bearerDataDecode.messageId, new SmsCbLocation(TelephonyManager.getDefault().getNetworkOperator()), this.mEnvelope.serviceCategory, bearerDataDecode.getLanguage(), bearerDataDecode.userData.payloadStr, bearerDataDecode.priority, (SmsCbEtwsInfo) null, bearerDataDecode.cmasWarningInfo);
    }

    public static SmsMessage.SubmitPdu getSubmitPdu(String str, String str2, int i, int i2, byte[] bArr, boolean z) {
        SmsHeader.PortAddrs portAddrs = new SmsHeader.PortAddrs();
        portAddrs.destPort = i;
        portAddrs.origPort = i2;
        portAddrs.areEightBits = false;
        SmsHeader smsHeader = new SmsHeader();
        smsHeader.portAddrs = portAddrs;
        UserData userData = new UserData();
        if (i2 == 0) {
            userData.userDataHeader = null;
            Rlog.d(LOG_TAG, "getSubmitPdu(with dest&original port), clear the header.");
        } else {
            userData.userDataHeader = smsHeader;
        }
        userData.msgEncoding = 0;
        userData.msgEncodingSet = true;
        userData.payload = bArr;
        return privateGetSubmitPdu(str2, z, userData);
    }

    public static SmsMessage.SubmitPdu getSubmitPdu(String str, UserData userData, boolean z) {
        return privateGetPdu(str, z, userData, 0L, -1, -1);
    }

    public static SmsMessage.SubmitPdu createEfPdu(String str, String str2, long j) {
        if (str == null || str2 == null) {
            return null;
        }
        UserData userData = new UserData();
        userData.payloadStr = str2;
        userData.userDataHeader = null;
        if (j > 0) {
            Rlog.d(LOG_TAG, "createEfPdu, input timeStamp = " + j + ", out scTimeMillis = " + j);
        } else {
            Rlog.d(LOG_TAG, "createEfPdu, input timeStamp = " + j + ", dont assign time zone to this invalid value");
        }
        return privateGetPdu(str, false, userData, j, -1, -1);
    }

    private static SmsMessage.SubmitPdu privateGetPdu(String str, boolean z, UserData userData, long j, int i, int i2) {
        CdmaSmsAddress cdmaSmsAddress = CdmaSmsAddress.parse(MtkPhoneNumberUtils.cdmaCheckAndProcessPlusCodeForSms(str));
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
        bearerData.userData = userData;
        if (j > 0) {
            bearerData.msgCenterTimeStamp = new BearerData.TimeStamp();
            bearerData.msgCenterTimeStamp.set(j);
        }
        if (i >= 0) {
            bearerData.validityPeriodRelativeSet = true;
            bearerData.validityPeriodRelative = i;
        } else {
            bearerData.validityPeriodRelativeSet = false;
        }
        if (i2 >= 0) {
            bearerData.priorityIndicatorSet = true;
            bearerData.priority = i2;
        } else {
            bearerData.priorityIndicatorSet = false;
        }
        byte[] bArrEncode = MtkBearerData.encode(bearerData);
        if (Rlog.isLoggable(LOGGABLE_TAG, 2)) {
            Rlog.d(LOG_TAG, "MO (encoded) BearerData = " + bearerData);
            if (bArrEncode != null) {
                Rlog.d(LOG_TAG, "MO raw BearerData = '" + HexDump.toHexString(bArrEncode) + "'");
            }
        }
        if (bArrEncode == null) {
            return null;
        }
        int i3 = bearerData.hasUserDataHeader ? 4101 : 4098;
        SmsEnvelope smsEnvelope = new SmsEnvelope();
        smsEnvelope.messageType = 0;
        smsEnvelope.teleService = i3;
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
            SmsMessage.SubmitPdu submitPdu = new SmsMessage.SubmitPdu();
            submitPdu.encodedMessage = byteArrayOutputStream.toByteArray();
            submitPdu.encodedScAddress = null;
            return submitPdu;
        } catch (IOException e) {
            Rlog.e(LOG_TAG, "creating SubmitPdu failed: " + e);
            return null;
        }
    }

    public static SmsMessage.SubmitPdu getSubmitPdu(String str, String str2, String str3, boolean z, SmsHeader smsHeader, int i, int i2, int i3, boolean z2) {
        if (str2 == null || str3 == null) {
            Log.e(LOG_TAG, "getSubmitPdu, null sms text or destination address. do nothing.");
            return null;
        }
        if (str2.isEmpty()) {
            Log.e(LOG_TAG, "getSubmitPdu, destination address is empty. do nothing.");
            return null;
        }
        if (str3.isEmpty()) {
            Log.e(LOG_TAG, "getSubmitPdu, message text is empty. do nothing.");
            return null;
        }
        int i4 = (i2 <= 244 || i2 > 255) ? i2 : 244;
        UserData userData = new UserData();
        userData.payloadStr = str3;
        userData.userDataHeader = smsHeader;
        int i5 = 2;
        if (i == 1) {
            if (!z2) {
                i5 = 9;
            }
            userData.msgEncoding = i5;
        } else if (i == 2) {
            userData.msgEncoding = 0;
        } else {
            userData.msgEncoding = 4;
        }
        userData.msgEncodingSet = true;
        return privateGetPdu(str2, z, userData, 0L, i4, i3);
    }

    public static GsmAlphabet.TextEncodingDetails calculateLength(CharSequence charSequence, boolean z, int i) {
        if (Resources.getSystem().getBoolean(R.^attr-private.notificationHeaderTextAppearance)) {
            Rlog.d(LOG_TAG, "here use BearerData.calcTextEncodingDetails, but divide in parent class will use Sms7BitEncodingTranslator.translate(messageBody) returned string instead again in this case, Caution!!");
            Rlog.d(LOG_TAG, "search calculateLengthCDMA for help!", new Throwable());
        }
        return MtkBearerData.calcTextEncodingDetails(charSequence, z, i);
    }

    public static MtkSmsMessage newMtkSmsMessage(SmsMessage smsMessage) {
        if (smsMessage == null) {
            return null;
        }
        MtkSmsMessage mtkSmsMessage = new MtkSmsMessage();
        mtkSmsMessage.mScAddress = smsMessage.getServiceCenterAddress();
        mtkSmsMessage.mOriginatingAddress = smsMessage.mOriginatingAddress;
        mtkSmsMessage.mMessageBody = smsMessage.getMessageBody();
        mtkSmsMessage.mPseudoSubject = smsMessage.getPseudoSubject();
        mtkSmsMessage.mEmailFrom = smsMessage.getEmailFrom();
        mtkSmsMessage.mEmailBody = smsMessage.getEmailBody();
        mtkSmsMessage.mIsEmail = smsMessage.isEmail();
        mtkSmsMessage.mScTimeMillis = smsMessage.getTimestampMillis();
        mtkSmsMessage.mPdu = smsMessage.getPdu();
        mtkSmsMessage.mUserData = smsMessage.getUserData();
        mtkSmsMessage.mUserDataHeader = smsMessage.getUserDataHeader();
        mtkSmsMessage.mIsMwi = false;
        mtkSmsMessage.mMwiSense = false;
        mtkSmsMessage.mMwiDontStore = false;
        mtkSmsMessage.mStatusOnIcc = smsMessage.getStatusOnIcc();
        mtkSmsMessage.mIndexOnIcc = smsMessage.getIndexOnIcc();
        mtkSmsMessage.mMessageRef = smsMessage.mMessageRef;
        mtkSmsMessage.status = smsMessage.getStatus() >> 16;
        mtkSmsMessage.mEnvelope = smsMessage.mEnvelope;
        mtkSmsMessage.mBearerData = smsMessage.mBearerData;
        return mtkSmsMessage;
    }

    private static String handlePlusCodeInternal(int i, String str) {
        String strRemoveIddNddAddPlusCodeForSms = sPlusCodeUtils.removeIddNddAddPlusCodeForSms(str);
        if (TextUtils.isEmpty(strRemoveIddNddAddPlusCodeForSms)) {
            return null;
        }
        if (i == 1 && str.charAt(0) != '+') {
            strRemoveIddNddAddPlusCodeForSms = "+" + strRemoveIddNddAddPlusCodeForSms;
        }
        Rlog.d(LOG_TAG, "handlePlusCodeInternal, after handled, the address = " + strRemoveIddNddAddPlusCodeForSms);
        return strRemoveIddNddAddPlusCodeForSms;
    }

    private static void replaceIddNddWithPluscode(SmsAddress smsAddress) {
        String strHandlePlusCodeInternal = handlePlusCodeInternal(smsAddress.ton, new String(smsAddress.origBytes));
        if (!TextUtils.isEmpty(strHandlePlusCodeInternal) && (!strHandlePlusCodeInternal.equals(r0))) {
            smsAddress.origBytes = strHandlePlusCodeInternal.getBytes();
            smsAddress.address = strHandlePlusCodeInternal;
        }
    }

    public String getDestinationAddress() {
        return getOriginatingAddress();
    }
}
