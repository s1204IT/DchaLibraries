package com.android.internal.telephony;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Message;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SmsCbMessage;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.metrics.TelephonyMetrics;

public class CellBroadcastHandler extends WakeLockStateMachine {
    private CellBroadcastHandler(Context context, Phone phone) {
        this("CellBroadcastHandler", context, phone);
    }

    protected CellBroadcastHandler(String str, Context context, Phone phone) {
        super(str, context, phone);
    }

    protected CellBroadcastHandler(String str, Context context, Phone phone, Object obj) {
        super(str, context, phone, obj);
    }

    public static CellBroadcastHandler makeCellBroadcastHandler(Context context, Phone phone) {
        CellBroadcastHandler cellBroadcastHandler = new CellBroadcastHandler(context, phone);
        cellBroadcastHandler.start();
        return cellBroadcastHandler;
    }

    @Override
    protected boolean handleSmsMessage(Message message) {
        if (message.obj instanceof SmsCbMessage) {
            handleBroadcastSms((SmsCbMessage) message.obj);
            return true;
        }
        loge("handleMessage got object of type: " + message.obj.getClass().getName());
        return false;
    }

    protected void handleBroadcastSms(SmsCbMessage smsCbMessage) {
        Intent intent;
        String str;
        int i;
        String string;
        TelephonyMetrics.getInstance().writeNewCBSms(this.mPhone.getPhoneId(), smsCbMessage.getMessageFormat(), smsCbMessage.getMessagePriority(), smsCbMessage.isCmasMessage(), smsCbMessage.isEtwsMessage(), smsCbMessage.getServiceCategory());
        if (smsCbMessage.isEmergencyMessage()) {
            log("Dispatching emergency SMS CB, SmsCbMessage is: " + smsCbMessage);
            intent = new Intent("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED");
            intent.setPackage(this.mContext.getResources().getString(R.string.action_bar_home_description_format));
            str = "android.permission.RECEIVE_EMERGENCY_BROADCAST";
            i = 17;
        } else {
            log("Dispatching SMS CB, SmsCbMessage is: " + smsCbMessage);
            intent = new Intent("android.provider.Telephony.SMS_CB_RECEIVED");
            intent.addFlags(16777216);
            str = "android.permission.RECEIVE_SMS";
            i = 16;
        }
        Intent intent2 = intent;
        intent2.putExtra("message", (Parcelable) smsCbMessage);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, this.mPhone.getPhoneId());
        if (Build.IS_DEBUGGABLE && (string = Settings.Secure.getString(this.mContext.getContentResolver(), "cmas_additional_broadcast_pkg")) != null) {
            Intent intent3 = new Intent(intent2);
            intent3.setPackage(string);
            this.mContext.sendOrderedBroadcastAsUser(intent3, UserHandle.ALL, str, i, null, getHandler(), -1, null, null);
        }
        this.mContext.sendOrderedBroadcastAsUser(intent2, UserHandle.ALL, str, i, this.mReceiver, getHandler(), -1, null, null);
    }
}
