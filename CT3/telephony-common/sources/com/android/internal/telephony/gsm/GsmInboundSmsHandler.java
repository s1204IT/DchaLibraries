package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.Message;
import android.os.SystemProperties;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsConstants;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UsimServiceTable;
import com.mediatek.common.MPlugin;
import com.mediatek.common.sms.IDupSmsFilterExt;

public class GsmInboundSmsHandler extends InboundSmsHandler {
    private final UsimDataDownloadHandler mDataDownloadHandler;
    private IDupSmsFilterExt mDupSmsFilterExt;

    private GsmInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor, Phone phone) {
        super("GsmInboundSmsHandler", context, storageMonitor, phone, GsmCellBroadcastHandler.makeGsmCellBroadcastHandler(context, phone));
        this.mDupSmsFilterExt = null;
        phone.mCi.setOnNewGsmSms(getHandler(), 1, null);
        this.mDataDownloadHandler = new UsimDataDownloadHandler(phone.mCi);
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        this.mDupSmsFilterExt = (IDupSmsFilterExt) MPlugin.createInstance(IDupSmsFilterExt.class.getName(), context);
        if (this.mDupSmsFilterExt != null) {
            this.mDupSmsFilterExt.setPhoneId(this.mPhone.getPhoneId());
            String actualClassName = this.mDupSmsFilterExt.getClass().getName();
            log("initial IDupSmsFilterExt done, actual class name is " + actualClassName);
            return;
        }
        log("FAIL! intial IDupSmsFilterExt");
    }

    @Override
    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewGsmSms(getHandler());
        this.mCellBroadcastHandler.dispose();
        log("unregistered for 3GPP SMS");
        super.onQuitting();
    }

    public static GsmInboundSmsHandler makeInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor, Phone phone) {
        GsmInboundSmsHandler handler = new GsmInboundSmsHandler(context, storageMonitor, phone);
        handler.start();
        return handler;
    }

    @Override
    protected boolean is3gpp2() {
        return false;
    }

    @Override
    protected int dispatchMessageRadioSpecific(SmsMessageBase smsb) {
        SmsMessage sms = (SmsMessage) smsb;
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && this.mDupSmsFilterExt.containDupSms(sms.getPdu())) {
            log("discard dup sms");
            return 1;
        }
        if (sms.isTypeZero()) {
            log("Received short message type 0, Don't display or store it. Send Ack");
            return 1;
        }
        if (sms.isUsimDataDownload()) {
            UsimServiceTable ust = this.mPhone.getUsimServiceTable();
            return this.mDataDownloadHandler.handleUsimDataDownload(ust, sms);
        }
        boolean handled = false;
        if (sms.isMWISetMessage()) {
            updateMessageWaitingIndicator(sms.getNumOfVoicemails());
            handled = sms.isMwiDontStore();
            log("Received voice mail indicator set SMS shouldStore=" + (handled ? false : true));
        } else if (sms.isMWIClearMessage()) {
            updateMessageWaitingIndicator(0);
            handled = sms.isMwiDontStore();
            log("Received voice mail indicator clear SMS shouldStore=" + (handled ? false : true));
        }
        if (handled) {
            return 1;
        }
        if (!this.mStorageMonitor.isStorageAvailable() && sms.getMessageClass() != SmsConstants.MessageClass.CLASS_0) {
            return 3;
        }
        return dispatchNormalMessage(smsb);
    }

    private void updateMessageWaitingIndicator(int voicemailCount) {
        if (voicemailCount < 0) {
            voicemailCount = -1;
        } else if (voicemailCount > 255) {
            voicemailCount = 255;
        }
        this.mPhone.setVoiceMessageCount(voicemailCount);
        IccRecords records = UiccController.getInstance().getIccRecords(this.mPhone.getPhoneId(), 1);
        if (records != null) {
            log("updateMessageWaitingIndicator: updating SIM Records");
            records.setVoiceMessageWaiting(1, voicemailCount);
        } else {
            log("updateMessageWaitingIndicator: SIM Records not found");
        }
    }

    @Override
    protected void acknowledgeLastIncomingSms(boolean success, int result, Message response) {
        this.mPhone.mCi.acknowledgeLastIncomingGsmSms(success, resultToCause(result), response);
    }

    @Override
    protected void onUpdatePhoneObject(Phone phone) {
        super.onUpdatePhoneObject(phone);
        log("onUpdatePhoneObject: dispose of old CellBroadcastHandler and make a new one");
        this.mCellBroadcastHandler.dispose();
        this.mCellBroadcastHandler = GsmCellBroadcastHandler.makeGsmCellBroadcastHandler(this.mContext, phone);
    }

    private static int resultToCause(int rc) {
        switch (rc) {
            case -1:
            case 1:
                return 0;
            case 0:
            case 2:
            default:
                return 255;
            case 3:
                return 211;
        }
    }
}
