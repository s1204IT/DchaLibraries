package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.Message;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.VisualVoicemailSmsFilter;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;

public class GsmInboundSmsHandler extends InboundSmsHandler {
    protected final UsimDataDownloadHandler mDataDownloadHandler;

    private GsmInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        super("GsmInboundSmsHandler", context, smsStorageMonitor, phone, TelephonyComponentFactory.getInstance().makeGsmCellBroadcastHandler(context, phone));
        phone.mCi.setOnNewGsmSms(getHandler(), 1, null);
        this.mDataDownloadHandler = new UsimDataDownloadHandler(phone.mCi);
    }

    protected GsmInboundSmsHandler(String str, Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        super(str, context, smsStorageMonitor, phone, TelephonyComponentFactory.getInstance().makeGsmCellBroadcastHandler(context, phone), null);
        phone.mCi.setOnNewGsmSms(getHandler(), 1, null);
        this.mDataDownloadHandler = new UsimDataDownloadHandler(phone.mCi);
    }

    @Override
    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewGsmSms(getHandler());
        this.mCellBroadcastHandler.dispose();
        log("unregistered for 3GPP SMS");
        super.onQuitting();
    }

    public static GsmInboundSmsHandler makeInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        GsmInboundSmsHandler gsmInboundSmsHandler = new GsmInboundSmsHandler(context, smsStorageMonitor, phone);
        gsmInboundSmsHandler.start();
        return gsmInboundSmsHandler;
    }

    @Override
    protected boolean is3gpp2() {
        return false;
    }

    @Override
    protected int dispatchMessageRadioSpecific(SmsMessageBase smsMessageBase) {
        SmsMessage smsMessage = (SmsMessage) smsMessageBase;
        boolean zIsMwiDontStore = false;
        if (smsMessage.isTypeZero()) {
            int i = -1;
            SmsHeader userDataHeader = smsMessage.getUserDataHeader();
            if (userDataHeader != null && userDataHeader.portAddrs != null) {
                i = userDataHeader.portAddrs.destPort;
            }
            VisualVoicemailSmsFilter.filter(this.mContext, new byte[][]{smsMessage.getPdu()}, "3gpp", i, this.mPhone.getSubId());
            log("Received short message type 0, Don't display or store it. Send Ack");
            return 1;
        }
        if (smsMessage.isUsimDataDownload()) {
            return this.mDataDownloadHandler.handleUsimDataDownload(this.mPhone.getUsimServiceTable(), smsMessage);
        }
        if (smsMessage.isMWISetMessage()) {
            updateMessageWaitingIndicator(smsMessage.getNumOfVoicemails());
            zIsMwiDontStore = smsMessage.isMwiDontStore();
            StringBuilder sb = new StringBuilder();
            sb.append("Received voice mail indicator set SMS shouldStore=");
            sb.append(!zIsMwiDontStore);
            log(sb.toString());
        } else if (smsMessage.isMWIClearMessage()) {
            updateMessageWaitingIndicator(0);
            zIsMwiDontStore = smsMessage.isMwiDontStore();
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Received voice mail indicator clear SMS shouldStore=");
            sb2.append(!zIsMwiDontStore);
            log(sb2.toString());
        }
        if (zIsMwiDontStore) {
            return 1;
        }
        if (!this.mStorageMonitor.isStorageAvailable() && smsMessage.getMessageClass() != SmsConstants.MessageClass.CLASS_0) {
            return 3;
        }
        return dispatchNormalMessage(smsMessageBase);
    }

    private void updateMessageWaitingIndicator(int i) {
        if (i < 0) {
            i = -1;
        } else if (i > 255) {
            i = 255;
        }
        this.mPhone.setVoiceMessageCount(i);
        IccRecords iccRecords = UiccController.getInstance().getIccRecords(this.mPhone.getPhoneId(), 1);
        if (iccRecords != null) {
            log("updateMessageWaitingIndicator: updating SIM Records");
            iccRecords.setVoiceMessageWaiting(1, i);
        } else {
            log("updateMessageWaitingIndicator: SIM Records not found");
        }
    }

    @Override
    protected void acknowledgeLastIncomingSms(boolean z, int i, Message message) {
        this.mPhone.mCi.acknowledgeLastIncomingGsmSms(z, resultToCause(i), message);
    }

    @Override
    protected void onUpdatePhoneObject(Phone phone) {
        super.onUpdatePhoneObject(phone);
        log("onUpdatePhoneObject: dispose of old CellBroadcastHandler and make a new one");
        this.mCellBroadcastHandler.dispose();
        this.mCellBroadcastHandler = TelephonyComponentFactory.getInstance().makeGsmCellBroadcastHandler(this.mContext, phone);
    }

    private static int resultToCause(int i) {
        if (i == -1 || i == 1) {
            return 0;
        }
        if (i == 3) {
            return 211;
        }
        return 255;
    }
}
