package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UsimServiceTable;

public class UsimDataDownloadHandler extends Handler {
    private static final int BER_SMS_PP_DOWNLOAD_TAG = 209;
    private static final int DEV_ID_NETWORK = 131;
    private static final int DEV_ID_UICC = 129;
    private static final int EVENT_SEND_ENVELOPE_RESPONSE = 2;
    private static final int EVENT_START_DATA_DOWNLOAD = 1;
    private static final int EVENT_WRITE_SMS_COMPLETE = 3;
    private static final String TAG = "UsimDataDownloadHandler";
    private final CommandsInterface mCi;

    public UsimDataDownloadHandler(CommandsInterface commandsInterface) {
        this.mCi = commandsInterface;
    }

    int handleUsimDataDownload(UsimServiceTable usimServiceTable, SmsMessage smsMessage) {
        if (usimServiceTable != null && usimServiceTable.isAvailable(UsimServiceTable.UsimService.DATA_DL_VIA_SMS_PP)) {
            Rlog.d(TAG, "Received SMS-PP data download, sending to UICC.");
            return startDataDownload(smsMessage);
        }
        Rlog.d(TAG, "DATA_DL_VIA_SMS_PP service not available, storing message to UICC.");
        this.mCi.writeSmsToSim(3, IccUtils.bytesToHexString(PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(smsMessage.getServiceCenterAddress())), IccUtils.bytesToHexString(smsMessage.getPdu()), obtainMessage(3));
        return -1;
    }

    public int startDataDownload(SmsMessage smsMessage) {
        if (sendMessage(obtainMessage(1, smsMessage))) {
            return -1;
        }
        Rlog.e(TAG, "startDataDownload failed to send message to start data download.");
        return 2;
    }

    private void handleDataDownload(SmsMessage smsMessage) {
        int i;
        int dataCodingScheme = smsMessage.getDataCodingScheme();
        int protocolIdentifier = smsMessage.getProtocolIdentifier();
        byte[] pdu = smsMessage.getPdu();
        int i2 = pdu[0] & 255;
        int i3 = i2 + 1;
        int length = pdu.length - i3;
        int envelopeBodyLength = getEnvelopeBodyLength(i2, length);
        byte[] bArr = new byte[envelopeBodyLength + 1 + (envelopeBodyLength > 127 ? 2 : 1)];
        bArr[0] = -47;
        if (envelopeBodyLength > 127) {
            bArr[1] = -127;
            i = 2;
        } else {
            i = 1;
        }
        int i4 = i + 1;
        bArr[i] = (byte) envelopeBodyLength;
        int i5 = i4 + 1;
        bArr[i4] = (byte) (128 | ComprehensionTlvTag.DEVICE_IDENTITIES.value());
        int i6 = i5 + 1;
        bArr[i5] = 2;
        int i7 = i6 + 1;
        bArr[i6] = -125;
        int i8 = i7 + 1;
        bArr[i7] = -127;
        if (i2 != 0) {
            int i9 = i8 + 1;
            bArr[i8] = (byte) ComprehensionTlvTag.ADDRESS.value();
            int i10 = i9 + 1;
            bArr[i9] = (byte) i2;
            System.arraycopy(pdu, 1, bArr, i10, i2);
            i8 = i10 + i2;
        }
        int i11 = i8 + 1;
        bArr[i8] = (byte) (128 | ComprehensionTlvTag.SMS_TPDU.value());
        if (length > 127) {
            bArr[i11] = -127;
            i11++;
        }
        int i12 = i11 + 1;
        bArr[i11] = (byte) length;
        System.arraycopy(pdu, i3, bArr, i12, length);
        if (i12 + length != bArr.length) {
            Rlog.e(TAG, "startDataDownload() calculated incorrect envelope length, aborting.");
            acknowledgeSmsWithError(255);
        } else {
            this.mCi.sendEnvelopeWithStatus(IccUtils.bytesToHexString(bArr), obtainMessage(2, new int[]{dataCodingScheme, protocolIdentifier}));
        }
    }

    private static int getEnvelopeBodyLength(int i, int i2) {
        int i3 = i2 + 5 + (i2 > 127 ? 2 : 1);
        if (i != 0) {
            return i3 + 2 + i;
        }
        return i3;
    }

    private void sendSmsAckForEnvelopeResponse(IccIoResult iccIoResult, int i, int i2) {
        boolean z;
        byte[] bArr;
        int i3;
        int i4;
        int i5 = iccIoResult.sw1;
        int i6 = iccIoResult.sw2;
        if ((i5 == 144 && i6 == 0) || i5 == 145) {
            Rlog.d(TAG, "USIM data download succeeded: " + iccIoResult.toString());
            z = true;
        } else {
            if (i5 == 147 && i6 == 0) {
                Rlog.e(TAG, "USIM data download failed: Toolkit busy");
                acknowledgeSmsWithError(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_APP_TOOLKIT_BUSY);
                return;
            }
            if (i5 == 98 || i5 == 99) {
                Rlog.e(TAG, "USIM data download failed: " + iccIoResult.toString());
            } else {
                Rlog.e(TAG, "Unexpected SW1/SW2 response from UICC: " + iccIoResult.toString());
            }
            z = false;
        }
        byte[] bArr2 = iccIoResult.payload;
        if (bArr2 == null || bArr2.length == 0) {
            if (z) {
                this.mCi.acknowledgeLastIncomingGsmSms(true, 0, null);
                return;
            } else {
                acknowledgeSmsWithError(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_DATA_DOWNLOAD_ERROR);
                return;
            }
        }
        if (z) {
            bArr = new byte[bArr2.length + 5];
            bArr[0] = 0;
            bArr[1] = 7;
            i3 = 2;
        } else {
            bArr = new byte[bArr2.length + 6];
            bArr[0] = 0;
            bArr[1] = -43;
            i3 = 3;
            bArr[2] = 7;
        }
        int i7 = i3 + 1;
        bArr[i3] = (byte) i2;
        int i8 = i7 + 1;
        bArr[i7] = (byte) i;
        if (is7bitDcs(i)) {
            i4 = i8 + 1;
            bArr[i8] = (byte) ((bArr2.length * 8) / 7);
        } else {
            i4 = i8 + 1;
            bArr[i8] = (byte) bArr2.length;
        }
        System.arraycopy(bArr2, 0, bArr, i4, bArr2.length);
        this.mCi.acknowledgeIncomingGsmSmsWithPdu(z, IccUtils.bytesToHexString(bArr), null);
    }

    private void acknowledgeSmsWithError(int i) {
        this.mCi.acknowledgeLastIncomingGsmSms(false, i, null);
    }

    private static boolean is7bitDcs(int i) {
        return (i & 140) == 0 || (i & 244) == 240;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                handleDataDownload((SmsMessage) message.obj);
                break;
            case 2:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception != null) {
                    Rlog.e(TAG, "UICC Send Envelope failure, exception: " + asyncResult.exception);
                    acknowledgeSmsWithError(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_DATA_DOWNLOAD_ERROR);
                } else {
                    int[] iArr = (int[]) asyncResult.userObj;
                    sendSmsAckForEnvelopeResponse((IccIoResult) asyncResult.result, iArr[0], iArr[1]);
                }
                break;
            case 3:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null) {
                    Rlog.d(TAG, "Successfully wrote SMS-PP message to UICC");
                    this.mCi.acknowledgeLastIncomingGsmSms(true, 0, null);
                } else {
                    Rlog.d(TAG, "Failed to write SMS-PP message to UICC", asyncResult2.exception);
                    this.mCi.acknowledgeLastIncomingGsmSms(false, 255, null);
                }
                break;
            default:
                Rlog.e(TAG, "Ignoring unexpected message, what=" + message.what);
                break;
        }
    }
}
