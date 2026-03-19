package com.mediatek.internal.telephony;

import android.app.ActivityThread;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.SubscriptionController;
import com.mediatek.internal.telephony.IMtkSms;
import java.util.Iterator;
import java.util.List;
import mediatek.telephony.MtkSimSmsInsertStatus;
import mediatek.telephony.MtkSmsParameters;

public class MtkUiccSmsController extends IMtkSms.Stub {
    static final String LOG_TAG = "Mtk_RIL_UiccSmsController";
    protected Phone[] mPhone;

    protected MtkUiccSmsController(Phone[] phoneArr) {
        this.mPhone = phoneArr;
        if (ServiceManager.getService("imtksms") == null) {
            ServiceManager.addService("imtksms", this);
        }
    }

    private void sendErrorInPendingIntent(PendingIntent pendingIntent, int i) {
        if (pendingIntent != null) {
            try {
                pendingIntent.send(i);
            } catch (PendingIntent.CanceledException e) {
            }
        }
    }

    private void sendErrorInPendingIntents(List<PendingIntent> list, int i) {
        Iterator<PendingIntent> it = list.iterator();
        while (it.hasNext()) {
            sendErrorInPendingIntent(it.next(), i);
        }
    }

    private boolean isActiveSubId(int i) {
        return SubscriptionController.getInstance().isActiveSubId(i);
    }

    private MtkIccSmsInterfaceManager getIccSmsInterfaceManager(int i) {
        if (!isActiveSubId(i)) {
            Rlog.e(LOG_TAG, "Subscription " + i + " is inactive.");
            return null;
        }
        int phoneId = SubscriptionController.getInstance().getPhoneId(i);
        if (!SubscriptionManager.isValidPhoneId(phoneId) || phoneId == Integer.MAX_VALUE) {
            phoneId = 0;
        }
        try {
            return (MtkIccSmsInterfaceManager) this.mPhone[phoneId].getIccSmsInterfaceManager();
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "Exception is :" + e.toString() + " For subscription :" + i);
            e.printStackTrace();
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(LOG_TAG, "Exception is :" + e2.toString() + " For subscription :" + i);
            e2.printStackTrace();
            return null;
        }
    }

    public List<SmsRawData> getAllMessagesFromIccEfByModeForSubscriber(int i, String str, int i2) {
        if (!isSmsReadyForSubscriber(i)) {
            Rlog.e(LOG_TAG, "getAllMessagesFromIccEf SMS not ready");
            return null;
        }
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getAllMessagesFromIccEfByMode(str, i2);
        }
        Rlog.e(LOG_TAG, "getAllMessagesFromIccEfByModeForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        return null;
    }

    public int copyTextMessageToIccCardForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, long j) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.copyTextMessageToIccCard(str, str2, str3, list, i2, j);
        }
        Rlog.e(LOG_TAG, "sendStoredMultipartText iccSmsIntMgr is null for subscription: " + i);
        return 1;
    }

    public void sendDataWithOriginalPortForSubscriber(int i, String str, String str2, String str3, int i2, int i3, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        sendDataInternal(i, str, str2, str3, i2, i3, bArr, pendingIntent, pendingIntent2, true);
    }

    public void sendData(int i, String str, String str2, int i2, int i3, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        sendDataInternal(i, ActivityThread.currentPackageName(), str, str2, i2, i3, bArr, pendingIntent, pendingIntent2, false);
    }

    private void sendDataInternal(int i, String str, String str2, String str3, int i2, int i3, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendDataWithOriginalPort(str, str2, str3, i2, i3, bArr, pendingIntent, pendingIntent2, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendDataInternal iccSmsIntMgr is null forsubscription: " + i);
    }

    public boolean isSmsReadyForSubscriber(int i) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.isSmsReady();
        }
        Rlog.e(LOG_TAG, "isSmsReady iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }

    public void setSmsMemoryStatusForSubscriber(int i, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.setSmsMemoryStatus(z);
            return;
        }
        Rlog.e(LOG_TAG, "setSmsMemoryStatus iccSmsIntMgr is null forsubscription: " + i);
    }

    public MtkIccSmsStorageStatus getSmsSimMemoryStatusForSubscriber(int i, String str) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getSmsSimMemoryStatus(str);
        }
        Rlog.e(LOG_TAG, "setSmsMemoryStatus iccSmsIntMgr is null forsubscription: " + i);
        return null;
    }

    public void sendTextWithEncodingTypeForSubscriber(int i, String str, String str2, String str3, String str4, int i2, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendTextWithEncodingType(str, str2, str3, str4, i2, pendingIntent, pendingIntent2, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendTextWithEncodingTypeForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        sendErrorInPendingIntent(pendingIntent, 1);
    }

    public void sendMultipartTextWithEncodingTypeForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendMultipartTextWithEncodingType(str, str2, str3, list, i2, list2, list3, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendMultipartTextWithEncodingTypeForSubscriber iccSmsIntMgr is null for subscription: " + i);
        sendErrorInPendingIntents(list2, 1);
    }

    public MtkSimSmsInsertStatus insertTextMessageToIccCardForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, long j) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.insertTextMessageToIccCard(str, str2, str3, list, i2, j);
            return null;
        }
        Rlog.e(LOG_TAG, "sendMultipartTextWithEncodingTypeForSubscriber iccSmsIntMgr is null for subscription: " + i);
        return null;
    }

    public MtkSimSmsInsertStatus insertRawMessageToIccCardForSubscriber(int i, String str, int i2, byte[] bArr, byte[] bArr2) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.insertRawMessageToIccCard(str, i2, bArr, bArr2);
        }
        Rlog.e(LOG_TAG, "insertRawMessageToIccCardForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return null;
    }

    public void sendTextWithExtraParamsForSubscriber(int i, String str, String str2, String str3, String str4, Bundle bundle, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendTextWithExtraParams(str, str2, str3, str4, bundle, pendingIntent, pendingIntent2, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendTextWithExtraParamsForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        sendErrorInPendingIntent(pendingIntent, 1);
    }

    public void sendMultipartTextWithExtraParamsForSubscriber(int i, String str, String str2, String str3, List<String> list, Bundle bundle, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendMultipartTextWithExtraParams(str, str2, str3, list, bundle, list2, list3, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendTextWithExtraParamsForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        sendErrorInPendingIntents(list2, 1);
    }

    public MtkSmsParameters getSmsParametersForSubscriber(int i, String str) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getSmsParameters(str);
        }
        Rlog.e(LOG_TAG, "getSmsParametersForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return null;
    }

    public boolean setSmsParametersForSubscriber(int i, String str, MtkSmsParameters mtkSmsParameters) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.setSmsParameters(str, mtkSmsParameters);
        }
        Rlog.e(LOG_TAG, "setSmsParametersForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }

    public SmsRawData getMessageFromIccEfForSubscriber(int i, String str, int i2) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getMessageFromIccEf(str, i2);
        }
        Rlog.e(LOG_TAG, "getMessageFromIccEfForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return null;
    }

    public boolean queryCellBroadcastSmsActivationForSubscriber(int i) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.queryCellBroadcastSmsActivation();
        }
        Rlog.e(LOG_TAG, "setCellBroadcastSmsConfigForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }

    public boolean activateCellBroadcastSmsForSubscriber(int i, boolean z) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.activateCellBroadcastSms(z);
        }
        Rlog.e(LOG_TAG, "activateCellBroadcastSmsForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }

    public boolean removeCellBroadcastMsgForSubscriber(int i, int i2, int i3) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.removeCellBroadcastMsg(i2, i3);
        }
        Rlog.e(LOG_TAG, "removeCellBroadcastMsg iccSmsIntMgr is null for subscription: " + i);
        return false;
    }

    public boolean setEtwsConfigForSubscriber(int i, int i2) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.setEtwsConfig(i2);
        }
        Rlog.e(LOG_TAG, "setEtwsConfigForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }

    public String getCellBroadcastRangesForSubscriber(int i) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getCellBroadcastRanges();
        }
        Rlog.e(LOG_TAG, "getCellBroadcastRangesForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return "";
    }

    public boolean setCellBroadcastLangsForSubscriber(int i, String str) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.setCellBroadcastLangs(str);
        }
        Rlog.e(LOG_TAG, "setCellBroadcastLangsForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }

    public String getCellBroadcastLangsForSubscriber(int i) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getCellBroadcastLangs();
        }
        Rlog.e(LOG_TAG, "getCellBroadcastLangsForSubscriber iccSmsIntMgr is null forsubscription: " + i);
        return "";
    }

    public String getScAddressForSubscriber(int i) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getScAddress();
        }
        Rlog.e(LOG_TAG, "getScAddress iccSmsIntMgr is null forsubscription: " + i);
        return null;
    }

    public Bundle getScAddressWithErrorCodeForSubscriber(int i) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getScAddressWithErrorCode();
        }
        Rlog.e(LOG_TAG, "getScAddressWithErrorCode iccSmsIntMgr is null forsubscription: " + i);
        return null;
    }

    public boolean setScAddressForSubscriber(int i, String str) {
        MtkIccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.setScAddress(str);
        }
        Rlog.e(LOG_TAG, "setScAddress iccSmsIntMgr is null forsubscription: " + i);
        return false;
    }
}
