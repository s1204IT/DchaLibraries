package com.android.internal.telephony.cdma;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.radio.V1_0.DataCallFailCause;
import android.os.Message;
import android.telephony.SmsCbMessage;
import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.util.HexDump;
import java.util.Arrays;

public class CdmaInboundSmsHandler extends InboundSmsHandler {
    private final boolean mCheckForDuplicatePortsInOmadmWapPush;
    private byte[] mLastAcknowledgedSmsFingerprint;
    private byte[] mLastDispatchedSmsFingerprint;
    private final CdmaServiceCategoryProgramHandler mServiceCategoryProgramHandler;
    private final CdmaSMSDispatcher mSmsDispatcher;

    public CdmaInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CdmaSMSDispatcher cdmaSMSDispatcher) {
        super("CdmaInboundSmsHandler", context, smsStorageMonitor, phone, CellBroadcastHandler.makeCellBroadcastHandler(context, phone));
        this.mCheckForDuplicatePortsInOmadmWapPush = Resources.getSystem().getBoolean(R.^attr-private.findOnPageNextDrawable);
        this.mSmsDispatcher = cdmaSMSDispatcher;
        this.mServiceCategoryProgramHandler = CdmaServiceCategoryProgramHandler.makeScpHandler(context, phone.mCi);
        phone.mCi.setOnNewCdmaSms(getHandler(), 1, null);
    }

    public CdmaInboundSmsHandler(String str, Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CdmaSMSDispatcher cdmaSMSDispatcher) {
        super(str, context, smsStorageMonitor, phone, CellBroadcastHandler.makeCellBroadcastHandler(context, phone), null);
        this.mCheckForDuplicatePortsInOmadmWapPush = Resources.getSystem().getBoolean(R.^attr-private.findOnPageNextDrawable);
        this.mSmsDispatcher = cdmaSMSDispatcher;
        this.mServiceCategoryProgramHandler = CdmaServiceCategoryProgramHandler.makeScpHandler(context, phone.mCi);
        phone.mCi.setOnNewCdmaSms(getHandler(), 1, null);
    }

    @Override
    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewCdmaSms(getHandler());
        this.mCellBroadcastHandler.dispose();
        log("unregistered for 3GPP2 SMS");
        super.onQuitting();
    }

    public static CdmaInboundSmsHandler makeInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CdmaSMSDispatcher cdmaSMSDispatcher) {
        CdmaInboundSmsHandler cdmaInboundSmsHandlerMakeCdmaInboundSmsHandler = TelephonyComponentFactory.getInstance().makeCdmaInboundSmsHandler(context, smsStorageMonitor, phone, cdmaSMSDispatcher);
        cdmaInboundSmsHandlerMakeCdmaInboundSmsHandler.start();
        return cdmaInboundSmsHandlerMakeCdmaInboundSmsHandler;
    }

    @Override
    protected boolean is3gpp2() {
        return true;
    }

    @Override
    protected int dispatchMessageRadioSpecific(SmsMessageBase smsMessageBase) {
        boolean z;
        SmsMessage smsMessage = (SmsMessage) smsMessageBase;
        if (1 != smsMessage.getMessageType()) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            log("Broadcast type message");
            SmsCbMessage broadcastSms = smsMessage.parseBroadcastSms();
            if (broadcastSms != null) {
                this.mCellBroadcastHandler.dispatchSmsMessage(broadcastSms);
            } else {
                loge("error trying to parse broadcast SMS");
            }
            return 1;
        }
        this.mLastDispatchedSmsFingerprint = smsMessage.getIncomingSmsFingerprint();
        if (this.mLastAcknowledgedSmsFingerprint != null && Arrays.equals(this.mLastDispatchedSmsFingerprint, this.mLastAcknowledgedSmsFingerprint)) {
            return 1;
        }
        smsMessage.parseSms();
        int teleService = smsMessage.getTeleService();
        if (teleService != 262144) {
            switch (teleService) {
                case DataCallFailCause.OEM_DCFAILCAUSE_2:
                case DataCallFailCause.OEM_DCFAILCAUSE_5:
                    if (smsMessage.isStatusReportMessage()) {
                        this.mSmsDispatcher.sendStatusReportMessage(smsMessage);
                        return 1;
                    }
                    break;
                case DataCallFailCause.OEM_DCFAILCAUSE_3:
                    break;
                case DataCallFailCause.OEM_DCFAILCAUSE_4:
                    break;
                case DataCallFailCause.OEM_DCFAILCAUSE_6:
                    this.mServiceCategoryProgramHandler.dispatchSmsMessage(smsMessage);
                    return 1;
                default:
                    loge("unsupported teleservice 0x" + Integer.toHexString(teleService));
                    return 4;
            }
            if (!this.mStorageMonitor.isStorageAvailable() && smsMessage.getMessageClass() != SmsConstants.MessageClass.CLASS_0) {
                return 3;
            }
            if (4100 == teleService) {
                return processCdmaWapPdu(smsMessage.getUserData(), smsMessage.mMessageRef, smsMessage.getOriginatingAddress(), smsMessage.getDisplayOriginatingAddress(), smsMessage.getTimestampMillis());
            }
            return dispatchNormalMessage(smsMessageBase);
        }
        handleVoicemailTeleservice(smsMessage);
        return 1;
    }

    @Override
    protected void acknowledgeLastIncomingSms(boolean z, int i, Message message) {
        int iResultToCause = resultToCause(i);
        this.mPhone.mCi.acknowledgeLastIncomingCdmaSms(z, iResultToCause, message);
        if (iResultToCause == 0) {
            this.mLastAcknowledgedSmsFingerprint = this.mLastDispatchedSmsFingerprint;
        }
        this.mLastDispatchedSmsFingerprint = null;
    }

    @Override
    protected void onUpdatePhoneObject(Phone phone) {
        super.onUpdatePhoneObject(phone);
        this.mCellBroadcastHandler.updatePhoneObject(phone);
    }

    private static int resultToCause(int i) {
        if (i == -1 || i == 1) {
            return 0;
        }
        switch (i) {
            case 3:
                return 35;
            case 4:
                return 4;
            default:
                return 39;
        }
    }

    private void handleVoicemailTeleservice(SmsMessage smsMessage) {
        int numOfVoicemails = smsMessage.getNumOfVoicemails();
        log("Voicemail count=" + numOfVoicemails);
        if (numOfVoicemails < 0) {
            numOfVoicemails = -1;
        } else if (numOfVoicemails > 99) {
            numOfVoicemails = 99;
        }
        this.mPhone.setVoiceMessageCount(numOfVoicemails);
    }

    private int processCdmaWapPdu(byte[] bArr, int i, String str, String str2, long j) {
        int i2;
        int i3;
        int i4 = bArr[0] & 255;
        if (i4 == 0) {
            int i5 = bArr[1] & 255;
            int i6 = 3;
            int i7 = bArr[2] & 255;
            if (i7 >= i5) {
                loge("WDP bad segment #" + i7 + " expecting 0-" + (i5 - 1));
                return 1;
            }
            if (i7 != 0) {
                i2 = 0;
                i3 = 0;
            } else {
                i2 = (bArr[4] & 255) | ((bArr[3] & 255) << 8);
                int i8 = (255 & bArr[6]) | ((bArr[5] & 255) << 8);
                if (!this.mCheckForDuplicatePortsInOmadmWapPush || !checkDuplicatePortOmadmWapPush(bArr, 7)) {
                    i3 = i8;
                    i6 = 7;
                } else {
                    i6 = 11;
                    i3 = i8;
                }
            }
            log("Received WAP PDU. Type = " + i4 + ", originator = " + str + ", src-port = " + i2 + ", dst-port = " + i3 + ", ID = " + i + ", segment# = " + i7 + '/' + i5);
            byte[] bArr2 = new byte[bArr.length - i6];
            System.arraycopy(bArr, i6, bArr2, 0, bArr.length - i6);
            return addTrackerToRawTableAndSendMessage(TelephonyComponentFactory.getInstance().makeInboundSmsTracker(bArr2, j, i3, true, str, str2, i, i7, i5, true, HexDump.toHexString(bArr2)), false);
        }
        log("Received a WAP SMS which is not WDP. Discard.");
        return 1;
    }

    private static boolean checkDuplicatePortOmadmWapPush(byte[] bArr, int i) {
        int i2 = i + 4;
        byte[] bArr2 = new byte[bArr.length - i2];
        System.arraycopy(bArr, i2, bArr2, 0, bArr2.length);
        WspTypeDecoder wspTypeDecoder = new WspTypeDecoder(bArr2);
        if (wspTypeDecoder.decodeUintvarInteger(2) && wspTypeDecoder.decodeContentType(2 + wspTypeDecoder.getDecodedDataLength())) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SYNCML_NOTI.equals(wspTypeDecoder.getValueString());
        }
        return false;
    }
}
