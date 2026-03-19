package com.android.internal.telephony.cdma;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Message;
import android.telephony.SmsCbMessage;
import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.util.HexDump;
import com.mediatek.common.MPlugin;
import com.mediatek.common.sms.IInboundAutoRegSmsFwkExt;
import com.mediatek.internal.telephony.cdma.CdmaOmhSmsUtils;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Arrays;

public class CdmaInboundSmsHandler extends InboundSmsHandler {
    private final boolean mCheckForDuplicatePortsInOmadmWapPush;
    private IInboundAutoRegSmsFwkExt mInboundAutoRegSmsFwkExt;
    private byte[] mLastAcknowledgedSmsFingerprint;
    private byte[] mLastDispatchedSmsFingerprint;
    private final CdmaServiceCategoryProgramHandler mServiceCategoryProgramHandler;
    private final CdmaSMSDispatcher mSmsDispatcher;

    private CdmaInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor, Phone phone, CdmaSMSDispatcher smsDispatcher) {
        super("CdmaMoSms", context, storageMonitor, phone, CellBroadcastHandler.makeCellBroadcastHandler(context, phone));
        this.mInboundAutoRegSmsFwkExt = null;
        this.mCheckForDuplicatePortsInOmadmWapPush = Resources.getSystem().getBoolean(R.^attr-private.headerTextColor);
        this.mSmsDispatcher = smsDispatcher;
        this.mServiceCategoryProgramHandler = CdmaServiceCategoryProgramHandler.makeScpHandler(context, phone.mCi);
        phone.mCi.setOnNewCdmaSms(getHandler(), 1, null);
        this.mInboundAutoRegSmsFwkExt = (IInboundAutoRegSmsFwkExt) MPlugin.createInstance(IInboundAutoRegSmsFwkExt.class.getName());
        if (this.mInboundAutoRegSmsFwkExt != null) {
            return;
        }
        log("Create mInboundAutoRegSmsFwkExt fail.");
    }

    @Override
    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewCdmaSms(getHandler());
        this.mCellBroadcastHandler.dispose();
        log("unregistered for 3GPP2 SMS");
        super.onQuitting();
    }

    public static CdmaInboundSmsHandler makeInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor, Phone phone, CdmaSMSDispatcher smsDispatcher) {
        CdmaInboundSmsHandler handler = new CdmaInboundSmsHandler(context, storageMonitor, phone, smsDispatcher);
        handler.start();
        return handler;
    }

    @Override
    protected boolean is3gpp2() {
        return true;
    }

    @Override
    protected int dispatchMessageRadioSpecific(SmsMessageBase smsb) {
        SmsMessage sms = (SmsMessage) smsb;
        boolean isBroadcastType = 1 == sms.getMessageType();
        if (isBroadcastType) {
            log("Broadcast type message");
            SmsCbMessage cbMessage = sms.parseBroadcastSms();
            if (cbMessage != null) {
                if (CdmaOmhSmsUtils.isOmhCard(this.mPhone.getSubId())) {
                    int check = CdmaOmhSmsUtils.getBcsmsCfgFromRuim(this.mPhone.getSubId(), cbMessage.getServiceCategory(), cbMessage.getMessagePriority());
                    if (check == 0) {
                        return 1;
                    }
                }
                this.mCellBroadcastHandler.dispatchSmsMessage(cbMessage);
            } else {
                loge("error trying to parse broadcast SMS");
            }
            return 1;
        }
        this.mLastDispatchedSmsFingerprint = sms.getIncomingSmsFingerprint();
        if (this.mLastAcknowledgedSmsFingerprint != null && Arrays.equals(this.mLastDispatchedSmsFingerprint, this.mLastAcknowledgedSmsFingerprint)) {
            return 1;
        }
        sms.parseSms();
        int teleService = sms.getTeleService();
        switch (teleService) {
            case 4098:
            case SmsEnvelope.TELESERVICE_WEMT:
                if (sms.isStatusReportMessage()) {
                    this.mSmsDispatcher.sendStatusReportMessage(sms);
                    return 1;
                }
                break;
            case 4099:
            case SmsEnvelope.TELESERVICE_MWI:
                handleVoicemailTeleservice(sms);
                return 1;
            case 4100:
                break;
            case SmsEnvelope.TELESERVICE_SCPT:
                this.mServiceCategoryProgramHandler.dispatchSmsMessage(sms);
                return 1;
            default:
                if (this.mInboundAutoRegSmsFwkExt != null) {
                    boolean result = this.mInboundAutoRegSmsFwkExt.handleAutoRegMessage(this.mContext, teleService, sms.getPdu(), this.mPhone.getSubId());
                    if (result) {
                        log("handled auto register sms.");
                        return 1;
                    }
                }
                loge("unsupported teleservice 0x" + Integer.toHexString(teleService));
                return 4;
        }
        if (!this.mStorageMonitor.isStorageAvailable() && sms.getMessageClass() != SmsConstants.MessageClass.CLASS_0) {
            return 3;
        }
        if (4100 == teleService) {
            return processCdmaWapPdu(sms.getUserData(), sms.mMessageRef, sms.getOriginatingAddress(), sms.getTimestampMillis());
        }
        return dispatchNormalMessage(smsb);
    }

    @Override
    protected void acknowledgeLastIncomingSms(boolean success, int result, Message response) {
        int causeCode = resultToCause(result);
        this.mPhone.mCi.acknowledgeLastIncomingCdmaSms(success, causeCode, response);
        if (causeCode == 0) {
            this.mLastAcknowledgedSmsFingerprint = this.mLastDispatchedSmsFingerprint;
        }
        this.mLastDispatchedSmsFingerprint = null;
    }

    @Override
    protected void onUpdatePhoneObject(Phone phone) {
        super.onUpdatePhoneObject(phone);
        this.mCellBroadcastHandler.updatePhoneObject(phone);
    }

    private static int resultToCause(int rc) {
        switch (rc) {
            case -1:
            case 1:
                return 0;
            case 0:
            case 2:
            default:
                return 39;
            case 3:
                return 35;
            case 4:
                return 4;
        }
    }

    private void handleVoicemailTeleservice(SmsMessage sms) {
        int voicemailCount = sms.getNumOfVoicemails();
        log("Voicemail count=" + voicemailCount);
        if (voicemailCount < 0) {
            voicemailCount = -1;
        } else if (voicemailCount > 99) {
            voicemailCount = 99;
        }
        this.mPhone.setVoiceMessageCount(voicemailCount);
    }

    private int processCdmaWapPdu(byte[] pdu, int referenceNumber, String address, long timestamp) {
        int msgType = pdu[0] & PplMessageManager.Type.INVALID;
        if (msgType != 0) {
            log("Received a WAP SMS which is not WDP. Discard.");
            return 1;
        }
        int index = 1 + 1;
        int totalSegments = pdu[1] & PplMessageManager.Type.INVALID;
        int index2 = index + 1;
        int segment = pdu[index] & PplMessageManager.Type.INVALID;
        if (segment >= totalSegments) {
            loge("WDP bad segment #" + segment + " expecting 0-" + (totalSegments - 1));
            return 1;
        }
        int sourcePort = 0;
        int destinationPort = 0;
        if (segment == 0) {
            int index3 = index2 + 1;
            int sourcePort2 = (pdu[index2] & PplMessageManager.Type.INVALID) << 8;
            int index4 = index3 + 1;
            sourcePort = sourcePort2 | (pdu[index3] & PplMessageManager.Type.INVALID);
            int index5 = index4 + 1;
            int destinationPort2 = (pdu[index4] & PplMessageManager.Type.INVALID) << 8;
            index2 = index5 + 1;
            destinationPort = destinationPort2 | (pdu[index5] & PplMessageManager.Type.INVALID);
            if (this.mCheckForDuplicatePortsInOmadmWapPush && checkDuplicatePortOmadmWapPush(pdu, index2)) {
                index2 += 4;
            }
        }
        log("Received WAP PDU. Type = " + msgType + ", originator = " + address + ", src-port = " + sourcePort + ", dst-port = " + destinationPort + ", ID = " + referenceNumber + ", segment# = " + segment + '/' + totalSegments);
        byte[] userData = new byte[pdu.length - index2];
        System.arraycopy(pdu, index2, userData, 0, pdu.length - index2);
        InboundSmsTracker tracker = TelephonyComponentFactory.getInstance().makeInboundSmsTracker(this.mPhone.getSubId(), userData, timestamp, destinationPort, true, address, referenceNumber, segment, totalSegments, true, HexDump.toHexString(userData));
        return addTrackerToRawTableAndSendMessage(tracker, false);
    }

    private static boolean checkDuplicatePortOmadmWapPush(byte[] origPdu, int index) {
        int index2 = index + 4;
        byte[] omaPdu = new byte[origPdu.length - index2];
        System.arraycopy(origPdu, index2, omaPdu, 0, omaPdu.length);
        WspTypeDecoder pduDecoder = new WspTypeDecoder(omaPdu);
        if (!pduDecoder.decodeUintvarInteger(2)) {
            return false;
        }
        int wspIndex = pduDecoder.getDecodedDataLength() + 2;
        if (!pduDecoder.decodeContentType(wspIndex)) {
            return false;
        }
        String mimeType = pduDecoder.getValueString();
        return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SYNCML_NOTI.equals(mimeType);
    }
}
