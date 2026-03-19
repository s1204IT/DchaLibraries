package com.android.internal.telephony;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CarrierSignalAgent extends Handler {
    private static final String CARRIER_SIGNAL_DELIMITER = "\\s*,\\s*";
    private static final String COMPONENT_NAME_DELIMITER = "\\s*:\\s*";
    private static final boolean DBG = true;
    private static final int EVENT_REGISTER_DEFAULT_NETWORK_AVAIL = 0;
    private static final boolean NO_WAKE = false;
    private static final boolean WAKE = true;
    private boolean mDefaultNetworkAvail;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private final Phone mPhone;
    private static final String LOG_TAG = CarrierSignalAgent.class.getSimpleName();
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private Map<String, Set<ComponentName>> mCachedWakeSignalConfigs = new HashMap();
    private Map<String, Set<ComponentName>> mCachedNoWakeSignalConfigs = new HashMap();
    private final Set<String> mCarrierSignalList = new HashSet(Arrays.asList("com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE", "com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED", "com.android.internal.telephony.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED", "com.android.internal.telephony.CARRIER_SIGNAL_RESET", "com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE"));
    private final LocalLog mErrorLocalLog = new LocalLog(20);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            CarrierSignalAgent.this.log("CarrierSignalAgent receiver action: " + action);
            if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                CarrierSignalAgent.this.loadCarrierConfig();
            }
        }
    };

    public CarrierSignalAgent(Phone phone) {
        this.mPhone = phone;
        loadCarrierConfig();
        this.mPhone.getContext().registerReceiver(this.mReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        this.mPhone.getCarrierActionAgent().registerForCarrierAction(3, this, 0, null, false);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 0) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Rlog.e(LOG_TAG, "Register default network exception: " + asyncResult.exception);
                return;
            }
            ConnectivityManager connectivityManagerFrom = ConnectivityManager.from(this.mPhone.getContext());
            if (((Boolean) asyncResult.result).booleanValue()) {
                this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        if (!CarrierSignalAgent.this.mDefaultNetworkAvail) {
                            CarrierSignalAgent.this.log("Default network available: " + network);
                            Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE");
                            intent.putExtra("defaultNetworkAvailable", true);
                            CarrierSignalAgent.this.notifyCarrierSignalReceivers(intent);
                            CarrierSignalAgent.this.mDefaultNetworkAvail = true;
                        }
                    }

                    @Override
                    public void onLost(Network network) {
                        CarrierSignalAgent.this.log("Default network lost: " + network);
                        Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE");
                        intent.putExtra("defaultNetworkAvailable", false);
                        CarrierSignalAgent.this.notifyCarrierSignalReceivers(intent);
                        CarrierSignalAgent.this.mDefaultNetworkAvail = false;
                    }
                };
                connectivityManagerFrom.registerDefaultNetworkCallback(this.mNetworkCallback, this.mPhone);
                log("Register default network");
            } else if (this.mNetworkCallback != null) {
                connectivityManagerFrom.unregisterNetworkCallback(this.mNetworkCallback);
                this.mNetworkCallback = null;
                this.mDefaultNetworkAvail = false;
                log("unregister default network");
            }
        }
    }

    private void loadCarrierConfig() {
        PersistableBundle config;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            config = carrierConfigManager.getConfig();
        } else {
            config = null;
        }
        if (config != null) {
            synchronized (this.mCachedWakeSignalConfigs) {
                log("Loading carrier config: carrier_app_wake_signal_config");
                Map<String, Set<ComponentName>> andCache = parseAndCache(config.getStringArray("carrier_app_wake_signal_config"));
                if (!this.mCachedWakeSignalConfigs.isEmpty() && !andCache.equals(this.mCachedWakeSignalConfigs)) {
                    if (VDBG) {
                        log("carrier config changed, reset receivers from old config");
                    }
                    this.mPhone.getCarrierActionAgent().sendEmptyMessage(2);
                }
                this.mCachedWakeSignalConfigs = andCache;
            }
            synchronized (this.mCachedNoWakeSignalConfigs) {
                log("Loading carrier config: carrier_app_no_wake_signal_config");
                Map<String, Set<ComponentName>> andCache2 = parseAndCache(config.getStringArray("carrier_app_no_wake_signal_config"));
                if (!this.mCachedNoWakeSignalConfigs.isEmpty() && !andCache2.equals(this.mCachedNoWakeSignalConfigs)) {
                    if (VDBG) {
                        log("carrier config changed, reset receivers from old config");
                    }
                    this.mPhone.getCarrierActionAgent().sendEmptyMessage(2);
                }
                this.mCachedNoWakeSignalConfigs = andCache2;
            }
        }
    }

    private Map<String, Set<ComponentName>> parseAndCache(String[] strArr) {
        HashMap map = new HashMap();
        if (!ArrayUtils.isEmpty(strArr)) {
            for (String str : strArr) {
                if (!TextUtils.isEmpty(str)) {
                    String[] strArrSplit = str.trim().split(COMPONENT_NAME_DELIMITER, 2);
                    if (strArrSplit.length == 2) {
                        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(strArrSplit[0]);
                        if (componentNameUnflattenFromString == null) {
                            loge("Invalid component name: " + strArrSplit[0]);
                        } else {
                            for (String str2 : strArrSplit[1].split(CARRIER_SIGNAL_DELIMITER)) {
                                if (this.mCarrierSignalList.contains(str2)) {
                                    Set hashSet = (Set) map.get(str2);
                                    if (hashSet == null) {
                                        hashSet = new HashSet();
                                        map.put(str2, hashSet);
                                    }
                                    hashSet.add(componentNameUnflattenFromString);
                                    if (VDBG) {
                                        logv("Add config {signal: " + str2 + " componentName: " + componentNameUnflattenFromString + "}");
                                    }
                                } else {
                                    loge("Invalid signal name: " + str2);
                                }
                            }
                        }
                    } else {
                        loge("invalid config format: " + str);
                    }
                }
            }
        }
        return map;
    }

    public boolean hasRegisteredReceivers(String str) {
        return this.mCachedWakeSignalConfigs.containsKey(str) || this.mCachedNoWakeSignalConfigs.containsKey(str);
    }

    private void broadcast(Intent intent, Set<ComponentName> set, boolean z) {
        PackageManager packageManager = this.mPhone.getContext().getPackageManager();
        for (ComponentName componentName : set) {
            Intent intent2 = new Intent(intent);
            intent2.setComponent(componentName);
            if (z && packageManager.queryBroadcastReceivers(intent2, 65536).isEmpty()) {
                loge("Carrier signal receivers are configured but unavailable: " + intent2.getComponent());
                return;
            }
            if (!z && !packageManager.queryBroadcastReceivers(intent2, 65536).isEmpty()) {
                loge("Runtime signals shouldn't be configured in Manifest: " + intent2.getComponent());
                return;
            }
            intent2.putExtra("subscription", this.mPhone.getSubId());
            intent2.addFlags(268435456);
            if (!z) {
                intent2.setFlags(16);
            }
            try {
                this.mPhone.getContext().sendBroadcast(intent2);
                StringBuilder sb = new StringBuilder();
                sb.append("Sending signal ");
                sb.append(intent2.getAction());
                sb.append(intent2.getComponent() != null ? " to the carrier signal receiver: " + intent2.getComponent() : "");
                log(sb.toString());
            } catch (ActivityNotFoundException e) {
                loge("Send broadcast failed: " + e);
            }
        }
    }

    public void notifyCarrierSignalReceivers(Intent intent) {
        synchronized (this.mCachedWakeSignalConfigs) {
            Set<ComponentName> set = this.mCachedWakeSignalConfigs.get(intent.getAction());
            if (!ArrayUtils.isEmpty(set)) {
                broadcast(intent, set, true);
            }
        }
        synchronized (this.mCachedNoWakeSignalConfigs) {
            Set<ComponentName> set2 = this.mCachedNoWakeSignalConfigs.get(intent.getAction());
            if (!ArrayUtils.isEmpty(set2)) {
                broadcast(intent, set2, false);
            }
        }
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void loge(String str) {
        this.mErrorLocalLog.log(str);
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        printWriter.println("mCachedWakeSignalConfigs:");
        indentingPrintWriter.increaseIndent();
        for (Map.Entry<String, Set<ComponentName>> entry : this.mCachedWakeSignalConfigs.entrySet()) {
            printWriter.println("signal: " + entry.getKey() + " componentName list: " + entry.getValue());
        }
        indentingPrintWriter.decreaseIndent();
        printWriter.println("mCachedNoWakeSignalConfigs:");
        indentingPrintWriter.increaseIndent();
        for (Map.Entry<String, Set<ComponentName>> entry2 : this.mCachedNoWakeSignalConfigs.entrySet()) {
            printWriter.println("signal: " + entry2.getKey() + " componentName list: " + entry2.getValue());
        }
        indentingPrintWriter.decreaseIndent();
        printWriter.println("mDefaultNetworkAvail: " + this.mDefaultNetworkAvail);
        printWriter.println("error log:");
        indentingPrintWriter.increaseIndent();
        this.mErrorLocalLog.dump(fileDescriptor, printWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }
}
