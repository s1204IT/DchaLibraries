package com.android.internal.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.imsphone.ImsPhoneFactory;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.IndentingPrintWriter;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.dataconnection.DataConnectionHelper;
import com.mediatek.internal.telephony.dataconnection.DataSubSelector;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import com.mediatek.internal.telephony.worldphone.WorldMode;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import com.mediatek.internal.telephony.worldphone.WorldPhoneWrapper;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class PhoneFactory {
    static final String LOG_TAG = "PhoneFactory";
    public static final int MAX_ACTIVE_PHONES = 1;
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    static final int SOCKET_OPEN_RETRY_MILLIS = 2000;
    private static Context sContext;
    private static PhoneNotifier sPhoneNotifier;
    private static PhoneSwitcher sPhoneSwitcher;
    private static ProxyController sProxyController;
    private static SubscriptionMonitor sSubscriptionMonitor;
    private static TelephonyNetworkFactory[] sTelephonyNetworkFactories;
    private static UiccController sUiccController;
    static final Object sLockProxyPhones = new Object();
    private static Phone[] sPhones = null;
    private static Phone sPhone = null;
    private static CommandsInterface[] sCommandsInterfaces = null;
    private static CommandsInterface sCommandsInterface = null;
    private static SubscriptionInfoUpdater sSubInfoRecordUpdater = null;
    static final boolean DBG = false;
    private static boolean sMadeDefaults = DBG;
    private static IWorldPhone sWorldPhone = null;
    private static DataSubSelector sDataSubSelector = null;
    private static final HashMap<String, LocalLog> sLocalLogs = new HashMap<>();

    public static void makeDefaultPhones(Context context) {
        makeDefaultPhone(context);
    }

    public static void makeDefaultPhone(Context context) {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                sContext = context;
                TelephonyDevController.create();
                int retryCount = 0;
                while (true) {
                    boolean hasException = DBG;
                    retryCount++;
                    try {
                        new LocalServerSocket("com.android.internal.telephony");
                    } catch (IOException e) {
                        hasException = true;
                    }
                    if (hasException) {
                        if (retryCount > 3) {
                            throw new RuntimeException("PhoneFactory probably already running");
                        }
                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException e2) {
                        }
                    } else {
                        sPhoneNotifier = new DefaultPhoneNotifier();
                        int cdmaSubscription = CdmaSubscriptionSourceManager.getDefault(context);
                        Rlog.i(LOG_TAG, "Cdma Subscription set to " + cdmaSubscription);
                        int numPhones = TelephonyManager.getDefault().getPhoneCount();
                        int[] networkModes = new int[numPhones];
                        sPhones = new Phone[numPhones];
                        sCommandsInterfaces = new RIL[numPhones];
                        sTelephonyNetworkFactories = new TelephonyNetworkFactory[numPhones];
                        for (int i = 0; i < numPhones; i++) {
                            networkModes[i] = RILConstants.PREFERRED_NETWORK_MODE;
                            Rlog.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkModes[i]));
                            sCommandsInterfaces[i] = new RIL(context, networkModes[i], cdmaSubscription, Integer.valueOf(i));
                        }
                        Rlog.i(LOG_TAG, "Creating SubscriptionController");
                        SubscriptionController.init(context, sCommandsInterfaces);
                        RadioManager.init(context, numPhones, sCommandsInterfaces);
                        sUiccController = UiccController.make(context, sCommandsInterfaces);
                        for (int i2 = 0; i2 < numPhones; i2++) {
                            GsmCdmaPhone gsmCdmaPhone = null;
                            int phoneType = TelephonyManager.getPhoneType(networkModes[i2]);
                            if (phoneType == 1) {
                                gsmCdmaPhone = new GsmCdmaPhone(context, sCommandsInterfaces[i2], sPhoneNotifier, i2, 1, TelephonyComponentFactory.getInstance());
                            } else if (phoneType == 2) {
                                gsmCdmaPhone = new GsmCdmaPhone(context, sCommandsInterfaces[i2], sPhoneNotifier, i2, 6, TelephonyComponentFactory.getInstance());
                            }
                            Rlog.i(LOG_TAG, "Creating Phone with type = " + phoneType + " sub = " + i2);
                            sPhones[i2] = gsmCdmaPhone;
                        }
                        sPhone = sPhones[0];
                        sCommandsInterface = sCommandsInterfaces[0];
                        ComponentName componentName = SmsApplication.getDefaultSmsApplication(context, true);
                        String packageName = "NONE";
                        if (componentName != null) {
                            packageName = componentName.getPackageName();
                        }
                        Rlog.i(LOG_TAG, "defaultSmsApplication: " + packageName);
                        SmsApplication.initSmsPackageMonitor(context);
                        sMadeDefaults = true;
                        Rlog.i(LOG_TAG, "Creating SubInfoRecordUpdater ");
                        sSubInfoRecordUpdater = new SubscriptionInfoUpdater(context, sPhones, sCommandsInterfaces);
                        SubscriptionController.getInstance().updatePhonesAvailability(sPhones);
                        sDataSubSelector = new DataSubSelector(context, numPhones);
                        for (int i3 = 0; i3 < numPhones; i3++) {
                            sPhones[i3].startMonitoringImsService();
                        }
                        ITelephonyRegistry tr = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
                        SubscriptionController sc = SubscriptionController.getInstance();
                        sSubscriptionMonitor = new SubscriptionMonitor(tr, sContext, sc, numPhones);
                        sPhoneSwitcher = new PhoneSwitcher(1, numPhones, sContext, sc, Looper.myLooper(), tr, sCommandsInterfaces, sPhones);
                        sProxyController = ProxyController.getInstance(context, sPhones, sUiccController, sCommandsInterfaces, sPhoneSwitcher);
                        sTelephonyNetworkFactories = new TelephonyNetworkFactory[numPhones];
                        for (int i4 = 0; i4 < numPhones; i4++) {
                            sTelephonyNetworkFactories[i4] = new TelephonyNetworkFactory(sPhoneSwitcher, sc, sSubscriptionMonitor, Looper.myLooper(), sContext, i4, sPhones[i4].mDcTracker);
                        }
                        DataConnectionHelper.makeDataConnectionHelper(context, sPhones, sPhoneSwitcher);
                        if (WorldPhoneUtil.isWorldModeSupport() && WorldPhoneUtil.isWorldPhoneSupport()) {
                            Rlog.i(LOG_TAG, "World mode support");
                            WorldMode.init();
                        } else if (WorldPhoneUtil.isWorldPhoneSupport()) {
                            Rlog.i(LOG_TAG, "World phone support");
                            sWorldPhone = WorldPhoneWrapper.getWorldPhoneInstance();
                        } else {
                            Rlog.i(LOG_TAG, "World phone not support");
                        }
                    }
                }
            }
        }
    }

    public static Phone getDefaultPhone() {
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            phone = sPhone;
        }
        return phone;
    }

    public static Phone getPhone(int phoneId) {
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            if (phoneId == Integer.MAX_VALUE) {
                phone = sPhone;
            } else {
                phone = (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) ? null : sPhones[phoneId];
            }
        }
        return phone;
    }

    public static Phone[] getPhones() {
        Phone[] phoneArr;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            phoneArr = sPhones;
        }
        return phoneArr;
    }

    public static IWorldPhone getWorldPhone() {
        if (sWorldPhone == null) {
            Rlog.d(LOG_TAG, "sWorldPhone is null");
        }
        return sWorldPhone;
    }

    public static SipPhone makeSipPhone(String sipUri) {
        return SipPhoneFactory.makePhone(sipUri, sContext, sPhoneNotifier);
    }

    public static int calculatePreferredNetworkType(Context context, int phoneSubId) {
        int networkType = Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + phoneSubId, RILConstants.PREFERRED_NETWORK_MODE);
        Rlog.d(LOG_TAG, "calculatePreferredNetworkType: phoneSubId = " + phoneSubId + " networkType = " + networkType);
        int nwType = Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + phoneSubId, -1);
        if (nwType == -1) {
            Rlog.d(LOG_TAG, "check persist.radio.lte.chip : " + SystemProperties.get("persist.radio.lte.chip"));
            if (SystemProperties.get("persist.radio.lte.chip").equals(Phone.ACT_TYPE_UTRAN)) {
                if (SystemProperties.get("ro.boot.opt_c2k_support").equals("1")) {
                    networkType = 7;
                } else {
                    networkType = 0;
                }
                Rlog.d(LOG_TAG, "REFERRED_NETWORK_MODE + " + phoneSubId + " don't have init value yet, force to " + networkType);
            }
        }
        return networkType;
    }

    public static int getDefaultSubscription() {
        return SubscriptionController.getInstance().getDefaultSubId();
    }

    public static boolean isSMSPromptEnabled() {
        int value = 0;
        try {
            value = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_sms_prompt");
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        boolean prompt = value == 0 ? DBG : true;
        Rlog.d(LOG_TAG, "SMS Prompt option:" + prompt);
        return prompt;
    }

    public static Phone makeImsPhone(PhoneNotifier phoneNotifier, Phone defaultPhone) {
        return ImsPhoneFactory.makePhone(sContext, phoneNotifier, defaultPhone);
    }

    public static void addLocalLog(String key, int size) {
        synchronized (sLocalLogs) {
            if (sLocalLogs.containsKey(key)) {
                throw new IllegalArgumentException("key " + key + " already present");
            }
            sLocalLogs.put(key, new LocalLog(size));
        }
    }

    public static void localLog(String key, String log) {
        synchronized (sLocalLogs) {
            if (!sLocalLogs.containsKey(key)) {
                throw new IllegalArgumentException("key " + key + " not found");
            }
            sLocalLogs.get(key).log(log);
        }
    }

    public static void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(printwriter, "  ");
        indentingPrintWriter.println("PhoneFactory:");
        indentingPrintWriter.println(" sMadeDefaults=" + sMadeDefaults);
        sPhoneSwitcher.dump(fd, indentingPrintWriter, args);
        indentingPrintWriter.println();
        Phone[] phones = getPhones();
        for (int i = 0; i < phones.length; i++) {
            indentingPrintWriter.increaseIndent();
            Phone phone = phones[i];
            try {
                phone.dump(fd, indentingPrintWriter, args);
                indentingPrintWriter.flush();
                indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
                sTelephonyNetworkFactories[i].dump(fd, indentingPrintWriter, args);
                indentingPrintWriter.flush();
                indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
                try {
                    ((IccCardProxy) phone.getIccCard()).dump(fd, indentingPrintWriter, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                indentingPrintWriter.flush();
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
            } catch (Exception e2) {
                indentingPrintWriter.println("Telephony DebugService: Could not get Phone[" + i + "] e=" + e2);
            }
        }
        indentingPrintWriter.println("SubscriptionMonitor:");
        indentingPrintWriter.increaseIndent();
        try {
            sSubscriptionMonitor.dump(fd, indentingPrintWriter, args);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("UiccController:");
        indentingPrintWriter.increaseIndent();
        try {
            sUiccController.dump(fd, indentingPrintWriter, args);
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("SubscriptionController:");
        indentingPrintWriter.increaseIndent();
        try {
            SubscriptionController.getInstance().dump(fd, indentingPrintWriter, args);
        } catch (Exception e5) {
            e5.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("SubInfoRecordUpdater:");
        indentingPrintWriter.increaseIndent();
        try {
            sSubInfoRecordUpdater.dump(fd, indentingPrintWriter, args);
        } catch (Exception e6) {
            e6.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("LocalLogs:");
        indentingPrintWriter.increaseIndent();
        synchronized (sLocalLogs) {
            for (String key : sLocalLogs.keySet()) {
                indentingPrintWriter.println(key);
                indentingPrintWriter.increaseIndent();
                sLocalLogs.get(key).dump(fd, indentingPrintWriter, args);
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.flush();
        }
        indentingPrintWriter.decreaseIndent();
    }
}
