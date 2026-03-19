package com.android.internal.telephony;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Looper;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.euicc.EuiccCardController;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.ims.ImsResolver;
import com.android.internal.telephony.imsphone.ImsPhoneFactory;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PhoneFactory {
    static final boolean DBG = false;
    static final String LOG_TAG = "PhoneFactory";
    public static final int MAX_ACTIVE_PHONES = 1;
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    static final int SOCKET_OPEN_RETRY_MILLIS = 2000;
    private static Context sContext;
    private static EuiccCardController sEuiccCardController;
    private static EuiccController sEuiccController;
    private static ImsResolver sImsResolver;
    private static IntentBroadcaster sIntentBroadcaster;
    private static NotificationChannelController sNotificationChannelController;
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
    private static boolean sMadeDefaults = false;
    private static final HashMap<String, LocalLog> sLocalLogs = new HashMap<>();

    public static void makeDefaultPhones(Context context) {
        makeDefaultPhone(context);
    }

    public static void makeDefaultPhone(Context context) {
        boolean z;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                sContext = context;
                TelephonyDevController.create();
                int i = 0;
                while (true) {
                    i++;
                    try {
                        new LocalServerSocket("com.android.internal.telephony");
                        z = false;
                    } catch (IOException e) {
                        z = true;
                    }
                    if (z) {
                        if (i > 3) {
                            throw new RuntimeException("PhoneFactory probably already running");
                        }
                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException e2) {
                        }
                    } else {
                        TelephonyComponentFactory telephonyComponentFactory = TelephonyComponentFactory.getInstance();
                        sPhoneNotifier = telephonyComponentFactory.makeDefaultPhoneNotifier();
                        int i2 = CdmaSubscriptionSourceManager.getDefault(context);
                        Rlog.i(LOG_TAG, "Cdma Subscription set to " + i2);
                        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
                        boolean z2 = sContext.getResources().getBoolean(R.^attr-private.findOnPagePreviousDrawable);
                        String string = sContext.getResources().getString(R.string.allow);
                        Rlog.i(LOG_TAG, "ImsResolver: defaultImsPackage: " + string);
                        sImsResolver = new ImsResolver(sContext, string, phoneCount, z2);
                        sImsResolver.initPopulateCacheAndStartBind();
                        int[] iArr = new int[phoneCount];
                        sPhones = new Phone[phoneCount];
                        sCommandsInterfaces = new RIL[phoneCount];
                        sTelephonyNetworkFactories = new TelephonyNetworkFactory[phoneCount];
                        for (int i3 = 0; i3 < phoneCount; i3++) {
                            iArr[i3] = RILConstants.PREFERRED_NETWORK_MODE;
                            Rlog.i(LOG_TAG, "Network Mode set to " + Integer.toString(iArr[i3]));
                            sCommandsInterfaces[i3] = telephonyComponentFactory.makeRil(context, iArr[i3], i2, Integer.valueOf(i3));
                        }
                        Rlog.i(LOG_TAG, "Creating SubscriptionController");
                        SubscriptionController.init(context, sCommandsInterfaces);
                        telephonyComponentFactory.initRadioManager(context, phoneCount, sCommandsInterfaces);
                        telephonyComponentFactory.initEmbmsAdaptor(context, sCommandsInterfaces);
                        sUiccController = UiccController.make(context, sCommandsInterfaces);
                        if (context.getPackageManager().hasSystemFeature("android.hardware.telephony.euicc")) {
                            sEuiccController = EuiccController.init(context);
                            sEuiccCardController = EuiccCardController.init(context);
                        }
                        for (int i4 = 0; i4 < phoneCount; i4++) {
                            GsmCdmaPhone gsmCdmaPhoneMakePhone = null;
                            int phoneType = TelephonyManager.getPhoneType(iArr[i4]);
                            if (phoneType == 1) {
                                gsmCdmaPhoneMakePhone = telephonyComponentFactory.makePhone(context, sCommandsInterfaces[i4], sPhoneNotifier, i4, 1, TelephonyComponentFactory.getInstance());
                            } else if (phoneType == 2) {
                                gsmCdmaPhoneMakePhone = telephonyComponentFactory.makePhone(context, sCommandsInterfaces[i4], sPhoneNotifier, i4, 6, TelephonyComponentFactory.getInstance());
                            }
                            Rlog.i(LOG_TAG, "Creating Phone with type = " + phoneType + " sub = " + i4);
                            sPhones[i4] = gsmCdmaPhoneMakePhone;
                        }
                        sPhone = sPhones[0];
                        sCommandsInterface = sCommandsInterfaces[0];
                        ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(context, true);
                        String packageName = "NONE";
                        if (defaultSmsApplication != null) {
                            packageName = defaultSmsApplication.getPackageName();
                        }
                        Rlog.i(LOG_TAG, "defaultSmsApplication: " + packageName);
                        SmsApplication.initSmsPackageMonitor(context);
                        sMadeDefaults = true;
                        telephonyComponentFactory.makeNetworkStatusUpdater(sPhones, phoneCount);
                        Rlog.i(LOG_TAG, "Creating SubInfoRecordUpdater ");
                        sSubInfoRecordUpdater = telephonyComponentFactory.makeSubscriptionInfoUpdater(BackgroundThread.get().getLooper(), context, sPhones, sCommandsInterfaces);
                        SubscriptionController.getInstance().updatePhonesAvailability(sPhones);
                        telephonyComponentFactory.makeDataSubSelector(sContext, phoneCount);
                        telephonyComponentFactory.makeSmartDataSwitchAssistant(sContext, sPhones);
                        telephonyComponentFactory.makeSuppServManager(sContext, sPhones);
                        telephonyComponentFactory.makeDcHelper(sContext, sPhones);
                        for (int i5 = 0; i5 < phoneCount; i5++) {
                            sPhones[i5].startMonitoringImsService();
                        }
                        ITelephonyRegistry iTelephonyRegistryAsInterface = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
                        SubscriptionController subscriptionController = SubscriptionController.getInstance();
                        sSubscriptionMonitor = new SubscriptionMonitor(iTelephonyRegistryAsInterface, sContext, subscriptionController, phoneCount);
                        sPhoneSwitcher = telephonyComponentFactory.makePhoneSwitcher(1, phoneCount, sContext, subscriptionController, Looper.myLooper(), iTelephonyRegistryAsInterface, sCommandsInterfaces, sPhones);
                        sProxyController = ProxyController.getInstance(context, sPhones, sUiccController, sCommandsInterfaces, sPhoneSwitcher);
                        sIntentBroadcaster = IntentBroadcaster.getInstance(context);
                        sNotificationChannelController = new NotificationChannelController(context);
                        sTelephonyNetworkFactories = new TelephonyNetworkFactory[phoneCount];
                        for (int i6 = 0; i6 < phoneCount; i6++) {
                            sTelephonyNetworkFactories[i6] = telephonyComponentFactory.makeTelephonyNetworkFactories(sPhoneSwitcher, subscriptionController, sSubscriptionMonitor, Looper.myLooper(), sContext, i6, sPhones[i6].mDcTracker);
                        }
                        telephonyComponentFactory.makeWorldPhoneManager();
                        telephonyComponentFactory.initCarrierExpress();
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

    public static Phone getPhone(int i) {
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            if (i == Integer.MAX_VALUE) {
                phone = sPhone;
            } else {
                phone = (i < 0 || i >= TelephonyManager.getDefault().getPhoneCount()) ? null : sPhones[i];
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

    public static SubscriptionInfoUpdater getSubscriptionInfoUpdater() {
        return sSubInfoRecordUpdater;
    }

    public static ImsResolver getImsResolver() {
        return sImsResolver;
    }

    public static SipPhone makeSipPhone(String str) {
        return SipPhoneFactory.makePhone(str, sContext, sPhoneNotifier);
    }

    public static int calculatePreferredNetworkType(Context context, int i) {
        int i2 = Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + i, -1);
        Rlog.d(LOG_TAG, "calculatePreferredNetworkType: phoneSubId = " + i + " networkType = " + i2);
        if (i2 == -1) {
            int i3 = RILConstants.PREFERRED_NETWORK_MODE;
            try {
                return TelephonyManager.getIntAtIndex(context.getContentResolver(), "preferred_network_mode", SubscriptionController.getInstance().getPhoneId(i));
            } catch (Settings.SettingNotFoundException e) {
                Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for Settings.Global.PREFERRED_NETWORK_MODE");
                return i3;
            }
        }
        return i2;
    }

    public static int getDefaultSubscription() {
        return SubscriptionController.getInstance().getDefaultSubId();
    }

    public static boolean isSMSPromptEnabled() {
        int i;
        try {
            i = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_sms_prompt");
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
            i = 0;
        }
        boolean z = i != 0;
        Rlog.d(LOG_TAG, "SMS Prompt option:" + z);
        return z;
    }

    public static Phone makeImsPhone(PhoneNotifier phoneNotifier, Phone phone) {
        return ImsPhoneFactory.makePhone(sContext, phoneNotifier, phone);
    }

    public static void requestEmbeddedSubscriptionInfoListRefresh(Runnable runnable) {
        sSubInfoRecordUpdater.requestEmbeddedSubscriptionInfoListRefresh(runnable);
    }

    public static void addLocalLog(String str, int i) {
        synchronized (sLocalLogs) {
            if (sLocalLogs.containsKey(str)) {
                throw new IllegalArgumentException("key " + str + " already present");
            }
            sLocalLogs.put(str, new LocalLog(i));
        }
    }

    public static void localLog(String str, String str2) {
        synchronized (sLocalLogs) {
            if (!sLocalLogs.containsKey(str)) {
                throw new IllegalArgumentException("key " + str + " not found");
            }
            sLocalLogs.get(str).log(str2);
        }
    }

    public static void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("PhoneFactory:");
        indentingPrintWriter.println(" sMadeDefaults=" + sMadeDefaults);
        sPhoneSwitcher.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.println();
        Phone[] phones = getPhones();
        for (int i = 0; i < phones.length; i++) {
            indentingPrintWriter.increaseIndent();
            Phone phone = phones[i];
            try {
                phone.dump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.flush();
                indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
                sTelephonyNetworkFactories[i].dump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.flush();
                indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
                try {
                    UiccProfile uiccProfile = (UiccProfile) phone.getIccCard();
                    if (uiccProfile != null) {
                        uiccProfile.dump(fileDescriptor, indentingPrintWriter, strArr);
                    }
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
            sSubscriptionMonitor.dump(fileDescriptor, indentingPrintWriter, strArr);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("UiccController:");
        indentingPrintWriter.increaseIndent();
        try {
            sUiccController.dump(fileDescriptor, indentingPrintWriter, strArr);
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        if (sEuiccController != null) {
            indentingPrintWriter.println("EuiccController:");
            indentingPrintWriter.increaseIndent();
            try {
                sEuiccController.dump(fileDescriptor, indentingPrintWriter, strArr);
                sEuiccCardController.dump(fileDescriptor, indentingPrintWriter, strArr);
            } catch (Exception e5) {
                e5.printStackTrace();
            }
            indentingPrintWriter.flush();
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        }
        indentingPrintWriter.println("SubscriptionController:");
        indentingPrintWriter.increaseIndent();
        try {
            SubscriptionController.getInstance().dump(fileDescriptor, indentingPrintWriter, strArr);
        } catch (Exception e6) {
            e6.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("SubInfoRecordUpdater:");
        indentingPrintWriter.increaseIndent();
        try {
            sSubInfoRecordUpdater.dump(fileDescriptor, indentingPrintWriter, strArr);
        } catch (Exception e7) {
            e7.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("LocalLogs:");
        indentingPrintWriter.increaseIndent();
        synchronized (sLocalLogs) {
            for (String str : sLocalLogs.keySet()) {
                indentingPrintWriter.println(str);
                indentingPrintWriter.increaseIndent();
                sLocalLogs.get(str).dump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.flush();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("++++++++++++++++++++++++++++++++");
        indentingPrintWriter.println("SharedPreferences:");
        indentingPrintWriter.increaseIndent();
        try {
            if (sContext != null) {
                Map<String, ?> all = PreferenceManager.getDefaultSharedPreferences(sContext).getAll();
                for (String str2 : all.keySet()) {
                    indentingPrintWriter.println(((Object) str2) + " : " + all.get(str2));
                }
            }
        } catch (Exception e8) {
            e8.printStackTrace();
        }
        indentingPrintWriter.flush();
        indentingPrintWriter.decreaseIndent();
    }
}
