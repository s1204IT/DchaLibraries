package com.android.internal.telephony;

import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.ISms;
import java.util.Iterator;
import java.util.List;

public class UiccSmsController extends ISms.Stub {
    static final String LOG_TAG = "RIL_UiccSmsController";

    protected UiccSmsController() {
        if (ServiceManager.getService("isms") == null) {
            ServiceManager.addService("isms", this);
        }
    }

    private Phone getPhone(int i) {
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
        if (phone == null) {
            return PhoneFactory.getDefaultPhone();
        }
        return phone;
    }

    public boolean updateMessageOnIccEfForSubscriber(int i, String str, int i2, int i3, byte[] bArr) throws RemoteException {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.updateMessageOnIccEf(str, i2, i3, bArr);
        }
        Rlog.e(LOG_TAG, "updateMessageOnIccEfForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        return false;
    }

    public boolean copyMessageToIccEfForSubscriber(int i, String str, int i2, byte[] bArr, byte[] bArr2) throws RemoteException {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.copyMessageToIccEf(str, i2, bArr, bArr2);
        }
        Rlog.e(LOG_TAG, "copyMessageToIccEfForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        return false;
    }

    public List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int i, String str) throws RemoteException {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getAllMessagesFromIccEf(str);
        }
        Rlog.e(LOG_TAG, "getAllMessagesFromIccEfForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        return null;
    }

    public void sendDataForSubscriber(int i, String str, String str2, String str3, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendData(str, str2, str3, i2, bArr, pendingIntent, pendingIntent2);
            return;
        }
        Rlog.e(LOG_TAG, "sendDataForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        sendErrorInPendingIntent(pendingIntent, 1);
    }

    public void sendDataForSubscriberWithSelfPermissions(int i, String str, String str2, String str3, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendDataWithSelfPermissions(str, str2, str3, i2, bArr, pendingIntent, pendingIntent2);
            return;
        }
        Rlog.e(LOG_TAG, "sendText iccSmsIntMgr is null for Subscription: " + i);
    }

    public void sendText(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        sendTextForSubscriber(getPreferredSmsSubscription(), str, str2, str3, str4, pendingIntent, pendingIntent2, true);
    }

    public void sendTextForSubscriber(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendText(str, str2, str3, str4, pendingIntent, pendingIntent2, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendTextForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        sendErrorInPendingIntent(pendingIntent, 1);
    }

    public void sendTextForSubscriberWithSelfPermissions(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendTextWithSelfPermissions(str, str2, str3, str4, pendingIntent, pendingIntent2, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendText iccSmsIntMgr is null for Subscription: " + i);
    }

    public void sendTextForSubscriberWithOptions(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i2, boolean z2, int i3) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendTextWithOptions(str, str2, str3, str4, pendingIntent, pendingIntent2, z, i2, z2, i3);
            return;
        }
        Rlog.e(LOG_TAG, "sendTextWithOptions iccSmsIntMgr is null for Subscription: " + i);
    }

    public void sendMultipartText(String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3) throws RemoteException {
        sendMultipartTextForSubscriber(getPreferredSmsSubscription(), str, str2, str3, list, list2, list3, true);
    }

    public void sendMultipartTextForSubscriber(int i, String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) throws RemoteException {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendMultipartText(str, str2, str3, list, list2, list3, z);
            return;
        }
        Rlog.e(LOG_TAG, "sendMultipartTextForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        sendErrorInPendingIntents(list2, 1);
    }

    public void sendMultipartTextForSubscriberWithOptions(int i, String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z, int i2, boolean z2, int i3) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendMultipartTextWithOptions(str, str2, str3, list, list2, list3, z, i2, z2, i3);
            return;
        }
        Rlog.e(LOG_TAG, "sendMultipartTextWithOptions iccSmsIntMgr is null for Subscription: " + i);
    }

    public boolean enableCellBroadcastForSubscriber(int i, int i2, int i3) throws RemoteException {
        return enableCellBroadcastRangeForSubscriber(i, i2, i2, i3);
    }

    public boolean enableCellBroadcastRangeForSubscriber(int i, int i2, int i3, int i4) throws RemoteException {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.enableCellBroadcastRange(i2, i3, i4);
        }
        Rlog.e(LOG_TAG, "enableCellBroadcastRangeForSubscriber iccSmsIntMgr is null for Subscription: " + i);
        return false;
    }

    public boolean disableCellBroadcastForSubscriber(int i, int i2, int i3) throws RemoteException {
        return disableCellBroadcastRangeForSubscriber(i, i2, i2, i3);
    }

    public boolean disableCellBroadcastRangeForSubscriber(int i, int i2, int i3, int i4) throws RemoteException {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.disableCellBroadcastRange(i2, i3, i4);
        }
        Rlog.e(LOG_TAG, "disableCellBroadcastRangeForSubscriber iccSmsIntMgr is null for Subscription:" + i);
        return false;
    }

    public int getPremiumSmsPermission(String str) {
        return getPremiumSmsPermissionForSubscriber(getPreferredSmsSubscription(), str);
    }

    public int getPremiumSmsPermissionForSubscriber(int i, String str) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getPremiumSmsPermission(str);
        }
        Rlog.e(LOG_TAG, "getPremiumSmsPermissionForSubscriber iccSmsIntMgr is null");
        return 0;
    }

    public void setPremiumSmsPermission(String str, int i) {
        setPremiumSmsPermissionForSubscriber(getPreferredSmsSubscription(), str, i);
    }

    public void setPremiumSmsPermissionForSubscriber(int i, String str, int i2) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.setPremiumSmsPermission(str, i2);
        } else {
            Rlog.e(LOG_TAG, "setPremiumSmsPermissionForSubscriber iccSmsIntMgr is null");
        }
    }

    public boolean isImsSmsSupportedForSubscriber(int i) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.isImsSmsSupported();
        }
        Rlog.e(LOG_TAG, "isImsSmsSupportedForSubscriber iccSmsIntMgr is null");
        return false;
    }

    public boolean isSmsSimPickActivityNeeded(int i) {
        Context applicationContext = ActivityThread.currentApplication().getApplicationContext();
        TelephonyManager telephonyManager = (TelephonyManager) applicationContext.getSystemService("phone");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(applicationContext).getActiveSubscriptionInfoList();
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (activeSubscriptionInfoList != null) {
                int size = activeSubscriptionInfoList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i2);
                    if (subscriptionInfo != null && subscriptionInfo.getSubscriptionId() == i) {
                        return false;
                    }
                }
                if (size > 0 && telephonyManager.getSimCount() > 1) {
                    return true;
                }
            }
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public String getImsSmsFormatForSubscriber(int i) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            return iccSmsInterfaceManager.getImsSmsFormat();
        }
        Rlog.e(LOG_TAG, "getImsSmsFormatForSubscriber iccSmsIntMgr is null");
        return null;
    }

    public void injectSmsPduForSubscriber(int i, byte[] bArr, String str, PendingIntent pendingIntent) {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.injectSmsPdu(bArr, str, pendingIntent);
        } else {
            Rlog.e(LOG_TAG, "injectSmsPduForSubscriber iccSmsIntMgr is null");
            sendErrorInPendingIntent(pendingIntent, 2);
        }
    }

    private IccSmsInterfaceManager getIccSmsInterfaceManager(int i) {
        return getPhone(i).getIccSmsInterfaceManager();
    }

    public int getPreferredSmsSubscription() {
        return SubscriptionController.getInstance().getDefaultSmsSubId();
    }

    public boolean isSMSPromptEnabled() {
        return PhoneFactory.isSMSPromptEnabled();
    }

    public void sendStoredText(int i, String str, Uri uri, String str2, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws Throwable {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendStoredText(str, uri, str2, pendingIntent, pendingIntent2);
            return;
        }
        Rlog.e(LOG_TAG, "sendStoredText iccSmsIntMgr is null for subscription: " + i);
        sendErrorInPendingIntent(pendingIntent, 1);
    }

    public void sendStoredMultipartText(int i, String str, Uri uri, String str2, List<PendingIntent> list, List<PendingIntent> list2) throws Throwable {
        IccSmsInterfaceManager iccSmsInterfaceManager = getIccSmsInterfaceManager(i);
        if (iccSmsInterfaceManager != null) {
            iccSmsInterfaceManager.sendStoredMultipartText(str, uri, str2, list, list2);
            return;
        }
        Rlog.e(LOG_TAG, "sendStoredMultipartText iccSmsIntMgr is null for subscription: " + i);
        sendErrorInPendingIntents(list, 1);
    }

    public String createAppSpecificSmsToken(int i, String str, PendingIntent pendingIntent) {
        return getPhone(i).getAppSmsManager().createAppSpecificSmsToken(str, pendingIntent);
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
}
