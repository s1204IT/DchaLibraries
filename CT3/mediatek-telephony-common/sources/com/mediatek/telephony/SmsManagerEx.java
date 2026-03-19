package com.mediatek.telephony;

import android.app.PendingIntent;
import android.os.Bundle;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import java.util.ArrayList;

public class SmsManagerEx {
    private static final String TAG = "SMSEx";
    private static final SmsManagerEx sInstance = new SmsManagerEx();

    public void sendTextMessage(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId) {
        Rlog.d(TAG, "sendTextMessage, text=" + text + ", destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendTextMessage(destinationAddress, scAddress, text, sentIntent, deliveryIntent);
        }
    }

    public ArrayList<String> divideMessage(String text) {
        return SmsManager.getDefault().divideMessage(text);
    }

    public void sendMultipartTextMessage(String destinationAddress, String scAddress, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, int slotId) {
        Rlog.d(TAG, "sendMultipartTextMessage, destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendMultipartTextMessage(destinationAddress, scAddress, parts, sentIntents, deliveryIntents);
        }
    }

    public void sendDataMessage(String destinationAddress, String scAddress, short destinationPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId) {
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendDataMessage(destinationAddress, scAddress, destinationPort, data, sentIntent, deliveryIntent);
        }
    }

    public static SmsManagerEx getDefault() {
        return sInstance;
    }

    private SmsManagerEx() {
    }

    public void sendDataMessage(String destinationAddress, String scAddress, short destinationPort, short originalPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId) {
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendDataMessage(destinationAddress, scAddress, destinationPort, originalPort, data, sentIntent, deliveryIntent);
        }
    }

    public void sendTextMessageWithExtraParams(String destAddr, String scAddr, String text, Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent, int slotId) {
        Rlog.d(TAG, "sendTextMessageWithExtraParams, text=" + text);
        Rlog.d(TAG, "slotId=" + slotId);
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendTextMessageWithExtraParams(destAddr, scAddr, text, extraParams, sentIntent, deliveryIntent);
        }
    }

    public void sendMultipartTextMessageWithExtraParams(String destAddr, String scAddr, ArrayList<String> parts, Bundle extraParams, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, int slotId) {
        Rlog.d(TAG, "sendMultipartTextMessageWithExtraParams");
        Rlog.d(TAG, "slotId=" + slotId);
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
        } else {
            SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendMultipartTextMessageWithExtraParams(destAddr, scAddr, parts, extraParams, sentIntents, deliveryIntents);
        }
    }
}
