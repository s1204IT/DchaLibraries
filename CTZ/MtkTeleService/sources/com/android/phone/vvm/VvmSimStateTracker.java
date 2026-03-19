package com.android.phone.vvm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.phone.PhoneUtils;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class VvmSimStateTracker extends BroadcastReceiver {
    private static Map<PhoneAccountHandle, ServiceStateListener> sListeners = new ArrayMap();
    private static Set<PhoneAccountHandle> sPreBootHandles = new ArraySet();

    private class ServiceStateListener extends PhoneStateListener {
        private final Context mContext;
        private final PhoneAccountHandle mPhoneAccountHandle;

        public ServiceStateListener(Context context, PhoneAccountHandle phoneAccountHandle) {
            this.mContext = context;
            this.mPhoneAccountHandle = phoneAccountHandle;
        }

        public void listen() {
            TelephonyManager telephonyManager = VvmSimStateTracker.getTelephonyManager(this.mContext, this.mPhoneAccountHandle);
            if (telephonyManager == null) {
                VvmLog.e("VvmSimStateTracker", "Cannot create TelephonyManager from " + this.mPhoneAccountHandle);
                return;
            }
            telephonyManager.listen(this, 1);
        }

        public void unlisten() {
            ((TelephonyManager) this.mContext.getSystemService(TelephonyManager.class)).listen(this, 0);
            VvmSimStateTracker.sListeners.put(this.mPhoneAccountHandle, null);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (serviceState.getState() == 0) {
                VvmLog.i("VvmSimStateTracker", "in service");
                VvmSimStateTracker.this.sendConnected(this.mContext, this.mPhoneAccountHandle);
                unlisten();
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        byte b;
        String action = intent.getAction();
        if (action == null) {
            VvmLog.w("VvmSimStateTracker", "Null action for intent.");
        }
        VvmLog.i("VvmSimStateTracker", action);
        int iHashCode = action.hashCode();
        if (iHashCode != -1138588223) {
            if (iHashCode != -229777127) {
                b = (iHashCode == 798292259 && action.equals("android.intent.action.BOOT_COMPLETED")) ? (byte) 0 : (byte) -1;
            } else if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                b = 1;
            }
        } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
            b = 2;
        }
        switch (b) {
            case 0:
                onBootCompleted(context);
                break;
            case 1:
                if ("ABSENT".equals(intent.getStringExtra("ss"))) {
                    checkRemovedSim(context);
                }
                break;
            case 2:
                int intExtra = intent.getIntExtra("subscription", -1);
                if (!SubscriptionManager.isValidSubscriptionId(intExtra)) {
                    VvmLog.i("VvmSimStateTracker", "Received SIM change for invalid subscription id.");
                    checkRemovedSim(context);
                } else {
                    PhoneAccountHandle phoneAccountHandleFromSubId = PhoneAccountHandleConverter.fromSubId(intExtra);
                    if ("null".equals(phoneAccountHandleFromSubId.getId())) {
                        VvmLog.e("VvmSimStateTracker", "null phone account handle ID, possible modem crash. Ignoring carrier config changed event");
                    } else {
                        onCarrierConfigChanged(context, phoneAccountHandleFromSubId);
                    }
                }
                break;
        }
    }

    private void onBootCompleted(Context context) {
        for (PhoneAccountHandle phoneAccountHandle : sPreBootHandles) {
            TelephonyManager telephonyManager = getTelephonyManager(context, phoneAccountHandle);
            if (telephonyManager != null) {
                if (telephonyManager.getServiceState().getState() == 0) {
                    sListeners.put(phoneAccountHandle, null);
                    sendConnected(context, phoneAccountHandle);
                } else {
                    listenToAccount(context, phoneAccountHandle);
                }
            }
        }
        sPreBootHandles.clear();
    }

    private void sendConnected(Context context, PhoneAccountHandle phoneAccountHandle) {
        VvmLog.i("VvmSimStateTracker", "Service connected on " + phoneAccountHandle);
        RemoteVvmTaskManager.startCellServiceConnected(context, phoneAccountHandle);
    }

    private void checkRemovedSim(Context context) {
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(context);
        if (!isBootCompleted()) {
            for (PhoneAccountHandle phoneAccountHandle : sPreBootHandles) {
                if (phoneAccountHandle != null && !PhoneUtils.isPhoneAccountActive(subscriptionManagerFrom, phoneAccountHandle)) {
                    sPreBootHandles.remove(phoneAccountHandle);
                }
            }
            return;
        }
        ArraySet arraySet = new ArraySet();
        for (PhoneAccountHandle phoneAccountHandle2 : sListeners.keySet()) {
            if (!PhoneUtils.isPhoneAccountActive(subscriptionManagerFrom, phoneAccountHandle2)) {
                arraySet.add(phoneAccountHandle2);
                ServiceStateListener serviceStateListener = sListeners.get(phoneAccountHandle2);
                if (serviceStateListener != null) {
                    serviceStateListener.unlisten();
                }
                sendSimRemoved(context, phoneAccountHandle2);
            }
        }
        Iterator it = arraySet.iterator();
        while (it.hasNext()) {
            sListeners.remove((PhoneAccountHandle) it.next());
        }
    }

    private boolean isBootCompleted() {
        return SystemProperties.getBoolean("sys.boot_completed", false);
    }

    private void sendSimRemoved(Context context, PhoneAccountHandle phoneAccountHandle) {
        VvmLog.i("VvmSimStateTracker", "Sim removed on " + phoneAccountHandle);
        RemoteVvmTaskManager.startSimRemoved(context, phoneAccountHandle);
    }

    private void onCarrierConfigChanged(Context context, PhoneAccountHandle phoneAccountHandle) {
        if (!isBootCompleted()) {
            sPreBootHandles.add(phoneAccountHandle);
            return;
        }
        TelephonyManager telephonyManager = getTelephonyManager(context, phoneAccountHandle);
        if (telephonyManager == null) {
            VvmLog.e("VvmSimStateTracker", "Cannot create TelephonyManager from " + phoneAccountHandle + ", subId=" + ((TelephonyManager) context.getSystemService(TelephonyManager.class)).getSubIdForPhoneAccount(((TelecomManager) context.getSystemService(TelecomManager.class)).getPhoneAccount(phoneAccountHandle)));
            return;
        }
        if (telephonyManager.getServiceState().getState() == 0) {
            sendConnected(context, phoneAccountHandle);
            sListeners.put(phoneAccountHandle, null);
        } else {
            listenToAccount(context, phoneAccountHandle);
        }
    }

    private void listenToAccount(Context context, PhoneAccountHandle phoneAccountHandle) {
        ServiceStateListener serviceStateListener = new ServiceStateListener(context, phoneAccountHandle);
        serviceStateListener.listen();
        sListeners.put(phoneAccountHandle, serviceStateListener);
    }

    private static TelephonyManager getTelephonyManager(Context context, PhoneAccountHandle phoneAccountHandle) {
        return ((TelephonyManager) context.getSystemService(TelephonyManager.class)).createForPhoneAccountHandle(phoneAccountHandle);
    }
}
