package com.android.internal.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.DctController;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneFactory;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.sip.SipPhoneFactory;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.UiccController;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

public class PhoneFactory {
    static final String LOG_TAG = "PhoneFactory";
    static final int SOCKET_OPEN_MAX_RETRY = 3;
    static final int SOCKET_OPEN_RETRY_MILLIS = 2000;
    private static ProxyController mProxyController;
    private static UiccController mUiccController;
    private static Context sContext;
    private static PhoneNotifier sPhoneNotifier;
    static final Object sLockProxyPhones = new Object();
    private static PhoneProxy[] sProxyPhones = null;
    private static PhoneProxy sProxyPhone = null;
    private static CommandsInterface[] sCommandsInterfaces = null;
    private static CommandsInterface sCommandsInterface = null;
    private static SubscriptionInfoUpdater sSubInfoRecordUpdater = null;
    private static boolean sMadeDefaults = false;

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
                    boolean hasException = false;
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
                        sProxyPhones = new PhoneProxy[numPhones];
                        sCommandsInterfaces = new RIL[numPhones];
                        for (int i = 0; i < numPhones; i++) {
                            networkModes[i] = calculatePreferredNetworkTypeByPhoneId(context, i);
                            Rlog.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkModes[i]));
                            sCommandsInterfaces[i] = new RIL(context, networkModes[i], cdmaSubscription, Integer.valueOf(i));
                        }
                        Rlog.i(LOG_TAG, "Creating SubscriptionController");
                        SubscriptionController.init(context, sCommandsInterfaces);
                        mUiccController = UiccController.make(context, sCommandsInterfaces);
                        for (int i2 = 0; i2 < numPhones; i2++) {
                            PhoneBase phone = null;
                            int phoneType = TelephonyManager.getPhoneType(networkModes[i2]);
                            if (phoneType == 1) {
                                phone = new GSMPhone(context, sCommandsInterfaces[i2], sPhoneNotifier, i2);
                            } else if (phoneType == 2) {
                                phone = new CDMALTEPhone(context, sCommandsInterfaces[i2], sPhoneNotifier, i2);
                            }
                            Rlog.i(LOG_TAG, "Creating Phone with type = " + phoneType + " sub = " + i2);
                            sProxyPhones[i2] = new PhoneProxy(phone);
                        }
                        mProxyController = ProxyController.getInstance(context, sProxyPhones, mUiccController, sCommandsInterfaces);
                        sProxyPhone = sProxyPhones[0];
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
                        sSubInfoRecordUpdater = new SubscriptionInfoUpdater(context, sProxyPhones, sCommandsInterfaces);
                        SubscriptionController.getInstance().updatePhonesAvailability(sProxyPhones);
                    }
                }
            }
        }
    }

    public static Phone getCdmaPhone(int phoneId) {
        Phone phone;
        synchronized (PhoneProxy.lockForRadioTechnologyChange) {
            phone = new CDMALTEPhone(sContext, sCommandsInterfaces[phoneId], sPhoneNotifier, phoneId);
        }
        return phone;
    }

    public static Phone getGsmPhone(int phoneId) {
        Phone phone;
        synchronized (PhoneProxy.lockForRadioTechnologyChange) {
            phone = new GSMPhone(sContext, sCommandsInterfaces[phoneId], sPhoneNotifier, phoneId);
        }
        return phone;
    }

    public static Phone getDefaultPhone() {
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            phone = getPhone(Dsds.defaultSimId(sContext).ordinal());
        }
        return phone;
    }

    public static Phone getPhone(int phoneId) {
        String dbgInfo;
        Phone phone;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            if (phoneId == Integer.MAX_VALUE) {
                dbgInfo = "phoneId == DEFAULT_PHONE_ID return sProxyPhone";
                phone = sProxyPhone;
            } else {
                dbgInfo = "phoneId != DEFAULT_PHONE_ID return sProxyPhones[phoneId]";
                phone = (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) ? null : sProxyPhones[phoneId];
            }
            if (phoneId == Integer.MAX_VALUE || phone == null) {
                Rlog.d(LOG_TAG, "getPhone:- " + dbgInfo + " phoneId=" + phoneId + " phone=" + phone);
            }
        }
        return phone;
    }

    public static Phone[] getPhones() {
        PhoneProxy[] phoneProxyArr;
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                throw new IllegalStateException("Default phones haven't been made yet!");
            }
            phoneProxyArr = sProxyPhones;
        }
        return phoneProxyArr;
    }

    public static SipPhone makeSipPhone(String sipUri) {
        return SipPhoneFactory.makePhone(sipUri, sContext, sPhoneNotifier);
    }

    public static void setDefaultSubscription(int subId) {
        SystemProperties.set("persist.radio.default.sub", Integer.toString(subId));
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        synchronized (sLockProxyPhones) {
            if (phoneId >= 0) {
                if (phoneId < sProxyPhones.length) {
                    sProxyPhone = sProxyPhones[phoneId];
                    sCommandsInterface = sCommandsInterfaces[phoneId];
                    sMadeDefaults = true;
                }
            }
        }
        String defaultMccMnc = TelephonyManager.getDefault().getSimOperatorNumericForPhone(phoneId);
        Rlog.d(LOG_TAG, "update mccmnc=" + defaultMccMnc);
        MccTable.updateMccMncConfiguration(sContext, defaultMccMnc, false);
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED");
        intent.addFlags(536870912);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId);
        Rlog.d(LOG_TAG, "setDefaultSubscription : " + subId + " Broadcasting Default Subscription Changed...");
        sContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public static int calculatePreferredNetworkType(Context context, int phoneSubId) {
        int phoneId = SubscriptionManager.getPhoneId(phoneSubId);
        return calculatePreferredNetworkTypeByPhoneId(context, phoneId);
    }

    public static int calculatePreferredNetworkTypeByPhoneId(Context context, int phoneId) {
        int preferredNetworkType = RILConstants.PREFERRED_NETWORK_MODE;
        if ((Dsds.isSim2Master() && phoneId != PhoneConstants.SimId.SIM2.ordinal()) || (!Dsds.isSim2Master() && phoneId == PhoneConstants.SimId.SIM2.ordinal())) {
            preferredNetworkType = 1;
        }
        if (TelephonyManager.getLteOnCdmaModeStatic() == 1) {
            preferredNetworkType = 7;
        }
        int networkType = preferredNetworkType;
        try {
            int networkType2 = TelephonyManager.getIntAtIndex(context.getContentResolver(), "preferred_network_mode", phoneId);
            return networkType2;
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Value At Index for Settings.Global.PREFERRED_NETWORK_MODE");
            return networkType;
        }
    }

    public static void saveNetworkMode(Context context, int phoneId, int networkMode) {
        TelephonyManager tm = TelephonyManager.from(context);
        int phoneCount = tm.getPhoneCount();
        TelephonyManager.putIntAtIndex(context.getContentResolver(), "preferred_network_mode", phoneId, networkMode);
        TelephonyManager.MultiSimVariants multiSimType = tm.getMultiSimConfiguration();
        if (networkMode != 1 && multiSimType == TelephonyManager.MultiSimVariants.DSDS) {
            for (int i = 0; i < phoneCount; i++) {
                if (i != phoneId) {
                    TelephonyManager.putIntAtIndex(context.getContentResolver(), "preferred_network_mode", i, 1);
                }
            }
        }
    }

    public static int getDefaultSubscription() {
        return SubscriptionController.getInstance().getDefaultSubId();
    }

    public static int getVoiceSubscription() {
        try {
            int subId = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_voice_call");
            return subId;
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Call Values");
            return -1;
        }
    }

    public static boolean isPromptEnabled() {
        int value = 0;
        try {
            value = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_voice_prompt");
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Voice Prompt Values");
        }
        boolean prompt = value != 0;
        Rlog.d(LOG_TAG, "Prompt option:" + prompt);
        return prompt;
    }

    public static void setPromptEnabled(boolean enabled) {
        int value = !enabled ? 0 : 1;
        Settings.Global.putInt(sContext.getContentResolver(), "multi_sim_voice_prompt", value);
        Rlog.d(LOG_TAG, "setVoicePromptOption to " + enabled);
    }

    public static boolean isSMSPromptEnabled() {
        int value = 0;
        try {
            value = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_sms_prompt");
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        boolean prompt = value != 0;
        Rlog.d(LOG_TAG, "SMS Prompt option:" + prompt);
        return prompt;
    }

    public static void setSMSPromptEnabled(boolean enabled) {
        int value = !enabled ? 0 : 1;
        Settings.Global.putInt(sContext.getContentResolver(), "multi_sim_sms_prompt", value);
        Rlog.d(LOG_TAG, "setSMSPromptOption to " + enabled);
    }

    public static long getDataSubscription() {
        int subId = -1;
        try {
            subId = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_data_call");
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim Data Call Values");
        }
        return subId;
    }

    public static int getSMSSubscription() {
        try {
            int subId = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_sms");
            return subId;
        } catch (Settings.SettingNotFoundException e) {
            Rlog.e(LOG_TAG, "Settings Exception Reading Dual Sim SMS Values");
            return -1;
        }
    }

    public static ImsPhone makeImsPhone(PhoneNotifier phoneNotifier, Phone defaultPhone) {
        return ImsPhoneFactory.makePhone(sContext, phoneNotifier, defaultPhone);
    }

    public static void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("PhoneFactory:");
        PhoneProxy[] phones = (PhoneProxy[]) getPhones();
        int i = -1;
        for (PhoneProxy phoneProxy : phones) {
            i++;
            try {
                PhoneBase phoneBase = (PhoneBase) phoneProxy.getActivePhone();
                phoneBase.dump(fd, pw, args);
                pw.flush();
                pw.println("++++++++++++++++++++++++++++++++");
                try {
                    ((IccCardProxy) phoneProxy.getIccCard()).dump(fd, pw, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pw.flush();
                pw.println("++++++++++++++++++++++++++++++++");
            } catch (Exception e2) {
                pw.println("Telephony DebugService: Could not get Phone[" + i + "] e=" + e2);
            }
        }
        try {
            DctController.getInstance().dump(fd, pw, args);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        try {
            mUiccController.dump(fd, pw, args);
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            SubscriptionController.getInstance().dump(fd, pw, args);
        } catch (Exception e5) {
            e5.printStackTrace();
        }
        pw.flush();
    }
}
