package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.ims.ImsManager;
import com.android.ims.ImsSms;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
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
    private ImsSms mImsSms = null;
    private final PhoneBase mPhone;

    public UsimDataDownloadHandler(PhoneBase phone) {
        this.mPhone = phone;
        this.mCi = phone.mCi;
    }

    int handleUsimDataDownload(UsimServiceTable ust, SmsMessage smsMessage) {
        int protocolIdentifier = smsMessage.getProtocolIdentifier();
        if (ust != null && ust.isAvailable(UsimServiceTable.UsimService.DATA_DL_VIA_SMS_PP) && (protocolIdentifier == 127 || protocolIdentifier == 124)) {
            Rlog.d(TAG, "Received SMS-PP data download, sending to UICC.");
            return startDataDownload(smsMessage);
        }
        Rlog.d(TAG, "DATA_DL_VIA_SMS_PP service not available, storing message to UICC.");
        byte[] pdu = smsMessage.getPdu();
        String smsc = IccUtils.bytesToHexString(PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(smsMessage.getServiceCenterAddress()));
        byte[] deliverPdu = new byte[pdu.length - (smsc.length() / 2)];
        System.arraycopy(pdu, smsc.length() / 2, deliverPdu, 0, deliverPdu.length);
        this.mCi.writeSmsToSim(3, smsc, IccUtils.bytesToHexString(deliverPdu), obtainMessage(3));
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
        int index;
        int index2;
        int index3;
        int dcs = smsMessage.getDataCodingScheme();
        int pid = smsMessage.getProtocolIdentifier();
        byte[] pdu = smsMessage.getPdu();
        int scAddressLength = pdu[0] & 255;
        int tpduIndex = scAddressLength + 1;
        int tpduLength = pdu.length - tpduIndex;
        int bodyLength = getEnvelopeBodyLength(scAddressLength, tpduLength);
        int totalLength = bodyLength + 1 + (bodyLength > 127 ? 2 : 1);
        byte[] envelope = new byte[totalLength];
        int index4 = 0 + 1;
        envelope[0] = -47;
        if (bodyLength > 127) {
            index = index4 + 1;
            envelope[index4] = -127;
        } else {
            index = index4;
        }
        int index5 = index + 1;
        envelope[index] = (byte) bodyLength;
        int index6 = index5 + 1;
        envelope[index5] = (byte) (ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128);
        int index7 = index6 + 1;
        envelope[index6] = 2;
        int index8 = index7 + 1;
        envelope[index7] = -125;
        int index9 = index8 + 1;
        envelope[index8] = -127;
        if (scAddressLength != 0) {
            int index10 = index9 + 1;
            envelope[index9] = (byte) ComprehensionTlvTag.ADDRESS.value();
            int index11 = index10 + 1;
            envelope[index10] = (byte) scAddressLength;
            System.arraycopy(pdu, 1, envelope, index11, scAddressLength);
            index2 = index11 + scAddressLength;
        } else {
            index2 = index9;
        }
        int index12 = index2 + 1;
        envelope[index2] = (byte) (ComprehensionTlvTag.SMS_TPDU.value() | 128);
        if (tpduLength > 127) {
            index3 = index12 + 1;
            envelope[index12] = -127;
        } else {
            index3 = index12;
        }
        int index13 = index3 + 1;
        envelope[index3] = (byte) tpduLength;
        System.arraycopy(pdu, tpduIndex, envelope, index13, tpduLength);
        if (index13 + tpduLength != envelope.length) {
            Rlog.e(TAG, "startDataDownload() calculated incorrect envelope length, aborting.");
            acknowledgeSmsWithError(255);
        } else {
            String encodedEnvelope = IccUtils.bytesToHexString(envelope);
            this.mCi.sendEnvelopeWithStatus(encodedEnvelope, obtainMessage(2, new int[]{dcs, pid}));
        }
    }

    private static int getEnvelopeBodyLength(int scAddressLength, int tpduLength) {
        int length = tpduLength + 5 + (tpduLength > 127 ? 2 : 1);
        if (scAddressLength != 0) {
            return length + 2 + scAddressLength;
        }
        return length;
    }

    private boolean checkImsStatus() {
        boolean ret = false;
        ImsManager imsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsManager != null) {
            this.mImsSms = imsManager.getSmsInterface();
        }
        if (this.mImsSms != null && imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            ret = true;
        }
        Rlog.d(TAG, "checkImsStatus ret= " + ret);
        return ret;
    }

    private void sendSmsAckForEnvelopeResponse(IccIoResult response, int dcs, int pid) {
        boolean success;
        byte[] smsAckPdu;
        byte[] smsAckPdu2;
        int index;
        int index2;
        int sw1 = response.sw1;
        int sw2 = response.sw2;
        if ((sw1 == 144 && sw2 == 0) || sw1 == 145) {
            Rlog.d(TAG, "USIM data download succeeded: " + response.toString());
            success = true;
        } else if (sw1 == 147 && sw2 == 0) {
            Rlog.e(TAG, "USIM data download failed: Toolkit busy");
            acknowledgeSmsWithError(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_APP_TOOLKIT_BUSY);
            return;
        } else if (sw1 == 98 || sw1 == 99) {
            Rlog.e(TAG, "USIM data download failed: " + response.toString());
            success = false;
        } else {
            Rlog.e(TAG, "Unexpected SW1/SW2 response from UICC: " + response.toString());
            success = false;
        }
        byte[] responseBytes = response.payload;
        if (responseBytes == null || responseBytes.length == 0) {
            if (success) {
                smsAckPdu = new byte[4];
                int index3 = 0 + 1;
                smsAckPdu[0] = 0;
                int index4 = index3 + 1;
                smsAckPdu[index3] = 3;
                int index5 = index4 + 1;
                smsAckPdu[index4] = (byte) pid;
                int i = index5 + 1;
                smsAckPdu[index5] = (byte) dcs;
            } else {
                smsAckPdu = new byte[5];
                int index6 = 0 + 1;
                smsAckPdu[0] = 0;
                int index7 = index6 + 1;
                smsAckPdu[index6] = -43;
                int index8 = index7 + 1;
                smsAckPdu[index7] = 3;
                int index9 = index8 + 1;
                smsAckPdu[index8] = (byte) pid;
                int i2 = index9 + 1;
                smsAckPdu[index9] = (byte) dcs;
            }
            if (checkImsStatus()) {
                this.mImsSms.acknowledgeIncomingGsmSmsWithPdu(success, IccUtils.bytesToHexString(smsAckPdu), (Message) null);
                return;
            } else {
                this.mCi.acknowledgeIncomingGsmSmsWithPdu(success, IccUtils.bytesToHexString(smsAckPdu), null);
                return;
            }
        }
        if (success) {
            smsAckPdu2 = new byte[responseBytes.length + 5];
            int index10 = 0 + 1;
            smsAckPdu2[0] = 0;
            index = index10 + 1;
            smsAckPdu2[index10] = 7;
        } else {
            smsAckPdu2 = new byte[responseBytes.length + 6];
            int index11 = 0 + 1;
            smsAckPdu2[0] = 0;
            int index12 = index11 + 1;
            smsAckPdu2[index11] = -43;
            smsAckPdu2[index12] = 7;
            index = index12 + 1;
        }
        int index13 = index + 1;
        smsAckPdu2[index] = (byte) pid;
        int index14 = index13 + 1;
        smsAckPdu2[index13] = (byte) dcs;
        if (is7bitDcs(dcs)) {
            int septetCount = (responseBytes.length * 8) / 7;
            smsAckPdu2[index14] = (byte) septetCount;
            index2 = index14 + 1;
        } else {
            smsAckPdu2[index14] = (byte) responseBytes.length;
            index2 = index14 + 1;
        }
        System.arraycopy(responseBytes, 0, smsAckPdu2, index2, responseBytes.length);
        if (checkImsStatus()) {
            this.mImsSms.acknowledgeIncomingGsmSmsWithPdu(success, IccUtils.bytesToHexString(smsAckPdu2), (Message) null);
        } else {
            this.mCi.acknowledgeIncomingGsmSmsWithPdu(success, IccUtils.bytesToHexString(smsAckPdu2), null);
        }
    }

    private void acknowledgeSmsWithError(int cause) {
        if (checkImsStatus()) {
            this.mImsSms.acknowledgeLastIncomingGsmSms(false, cause, (Message) null);
        } else {
            this.mCi.acknowledgeLastIncomingGsmSms(false, cause, null);
        }
    }

    private static boolean is7bitDcs(int dcs) {
        return (dcs & 140) == 0 || (dcs & 244) == 240;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                handleDataDownload((SmsMessage) msg.obj);
                break;
            case 2:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Rlog.e(TAG, "UICC Send Envelope failure, exception: " + ar.exception);
                    acknowledgeSmsWithError(CommandsInterface.GSM_SMS_FAIL_CAUSE_USIM_DATA_DOWNLOAD_ERROR);
                } else {
                    int[] dcsPid = (int[]) ar.userObj;
                    sendSmsAckForEnvelopeResponse((IccIoResult) ar.result, dcsPid[0], dcsPid[1]);
                }
                break;
            case 3:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception == null) {
                    Rlog.d(TAG, "Successfully wrote SMS-PP message to UICC");
                    if (checkImsStatus()) {
                        this.mImsSms.acknowledgeLastIncomingGsmSms(true, 0, (Message) null);
                    } else {
                        this.mCi.acknowledgeLastIncomingGsmSms(true, 0, null);
                    }
                } else {
                    Rlog.d(TAG, "Failed to write SMS-PP message to UICC", ar2.exception);
                    if (checkImsStatus()) {
                        this.mImsSms.acknowledgeLastIncomingGsmSms(false, 255, (Message) null);
                    } else {
                        this.mCi.acknowledgeLastIncomingGsmSms(false, 255, null);
                    }
                }
                break;
            default:
                Rlog.e(TAG, "Ignoring unexpected message, what=" + msg.what);
                break;
        }
    }
}
