package com.android.phone;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.service.carrier.CarrierIdentifier;
import android.service.carrier.ICarrierService;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.ICarrierConfigLoader;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyPermissions;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class CarrierConfigLoader extends ICarrierConfigLoader.Stub {
    private static final int BIND_TIMEOUT_MILLIS = 30000;
    private static final int EVENT_BIND_CARRIER_TIMEOUT = 11;
    private static final int EVENT_BIND_DEFAULT_TIMEOUT = 10;
    private static final int EVENT_CHECK_SYSTEM_UPDATE = 12;
    private static final int EVENT_CLEAR_CONFIG = 0;
    private static final int EVENT_CONNECTED_TO_CARRIER = 4;
    private static final int EVENT_CONNECTED_TO_DEFAULT = 3;
    private static final int EVENT_DO_FETCH_CARRIER = 8;
    private static final int EVENT_DO_FETCH_DEFAULT = 7;
    private static final int EVENT_FETCH_CARRIER_DONE = 6;
    private static final int EVENT_FETCH_CARRIER_TIMEOUT = 15;
    private static final int EVENT_FETCH_DEFAULT_DONE = 5;
    private static final int EVENT_FETCH_DEFAULT_TIMEOUT = 14;
    private static final int EVENT_PACKAGE_CHANGED = 9;
    private static final int EVENT_SYSTEM_UNLOCKED = 13;
    private static final String KEY_FINGERPRINT = "build_fingerprint";
    private static final String LOG_TAG = "CarrierConfigLoader";
    private static final String TAG_BUNDLE = "bundle_data";
    private static final String TAG_DOCUMENT = "carrier_config";
    private static final String TAG_VERSION = "package_version";
    private static CarrierConfigLoader sInstance;
    private final BroadcastReceiver mBootReceiver;
    private PersistableBundle[] mConfigFromCarrierApp;
    private PersistableBundle[] mConfigFromDefaultApp;
    private Context mContext;
    private final Handler mHandler;
    private boolean[] mHasSentConfigChange;
    private final BroadcastReceiver mPackageReceiver;
    private final String mPlatformCarrierConfigPackage;
    private CarrierServiceConnection[] mServiceConnection;
    private int mTimeoutId;

    private class ConfigHandler extends Handler {
        private ConfigHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            final int i = message.arg1;
            CarrierConfigLoader.log("mHandler: " + message.what + " phoneId: " + i);
            int phoneCount = TelephonyManager.from(CarrierConfigLoader.this.mContext).getPhoneCount();
            int i2 = message.what;
            int i3 = 0;
            if (i2 == 0) {
                Phone phone = PhoneFactory.getPhone(i);
                if ((phone == null || !phone.isShuttingDown()) && phoneCount != 0) {
                    if (CarrierConfigLoader.this.mConfigFromDefaultApp[i] != null || CarrierConfigLoader.this.mConfigFromCarrierApp[i] != null) {
                        CarrierConfigLoader.this.mConfigFromDefaultApp[i] = null;
                        CarrierConfigLoader.this.mConfigFromCarrierApp[i] = null;
                        CarrierConfigLoader.this.mServiceConnection[i] = null;
                        CarrierConfigLoader.this.broadcastConfigChangedIntent(i, false);
                        return;
                    }
                    return;
                }
                return;
            }
            switch (i2) {
                case 3:
                    removeMessages(10);
                    final CarrierServiceConnection carrierServiceConnection = (CarrierServiceConnection) message.obj;
                    if (CarrierConfigLoader.this.mServiceConnection[i] != carrierServiceConnection || carrierServiceConnection.service == null) {
                        CarrierConfigLoader.this.mContext.unbindService(carrierServiceConnection);
                        return;
                    }
                    CarrierIdentifier carrierIdForPhoneId = CarrierConfigLoader.this.getCarrierIdForPhoneId(i);
                    final String iccIdForPhoneId = CarrierConfigLoader.this.getIccIdForPhoneId(i);
                    try {
                        ICarrierService.Stub.asInterface(carrierServiceConnection.service).getCarrierConfig(carrierIdForPhoneId, new ResultReceiver(this) {
                            @Override
                            public void onReceiveResult(int i4, Bundle bundle) {
                                try {
                                    CarrierConfigLoader.this.mContext.unbindService(carrierServiceConnection);
                                    if (CarrierConfigLoader.this.mServiceConnection[i] != carrierServiceConnection) {
                                        CarrierConfigLoader.loge("Received response for stale request.");
                                        return;
                                    }
                                    ConfigHandler.this.removeMessages(14);
                                    if (i4 != 1 && bundle != null) {
                                        PersistableBundle persistableBundle = (PersistableBundle) bundle.getParcelable("config_bundle");
                                        CarrierConfigLoader.this.saveConfigToXml(CarrierConfigLoader.this.mPlatformCarrierConfigPackage, iccIdForPhoneId, persistableBundle);
                                        CarrierConfigLoader.this.mConfigFromDefaultApp[i] = persistableBundle;
                                        ConfigHandler.this.sendMessage(ConfigHandler.this.obtainMessage(5, i, -1));
                                        return;
                                    }
                                    CarrierConfigLoader.loge("Failed to get carrier config");
                                    CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                                } catch (IllegalArgumentException e) {
                                    CarrierConfigLoader.loge("Exception: " + e.toString());
                                }
                            }
                        });
                        sendMessageDelayed(obtainMessage(14, i, -1), 30000L);
                        return;
                    } catch (RemoteException e) {
                        CarrierConfigLoader.loge("Failed to get carrier config: " + e.toString());
                        CarrierConfigLoader.this.mContext.unbindService(carrierServiceConnection);
                        return;
                    }
                case 4:
                    removeMessages(11);
                    final CarrierServiceConnection carrierServiceConnection2 = (CarrierServiceConnection) message.obj;
                    if (CarrierConfigLoader.this.mServiceConnection[i] != carrierServiceConnection2 || carrierServiceConnection2.service == null) {
                        CarrierConfigLoader.this.mContext.unbindService(carrierServiceConnection2);
                        return;
                    }
                    CarrierIdentifier carrierIdForPhoneId2 = CarrierConfigLoader.this.getCarrierIdForPhoneId(i);
                    final String iccIdForPhoneId2 = CarrierConfigLoader.this.getIccIdForPhoneId(i);
                    try {
                        ICarrierService.Stub.asInterface(carrierServiceConnection2.service).getCarrierConfig(carrierIdForPhoneId2, new ResultReceiver(this) {
                            @Override
                            public void onReceiveResult(int i4, Bundle bundle) {
                                CarrierConfigLoader.this.mContext.unbindService(carrierServiceConnection2);
                                if (CarrierConfigLoader.this.mServiceConnection[i] != carrierServiceConnection2) {
                                    CarrierConfigLoader.loge("Received response for stale request.");
                                    return;
                                }
                                ConfigHandler.this.removeMessages(15);
                                if (i4 == 1 || bundle == null) {
                                    CarrierConfigLoader.loge("Failed to get carrier config");
                                    CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                                } else {
                                    PersistableBundle persistableBundle = (PersistableBundle) bundle.getParcelable("config_bundle");
                                    CarrierConfigLoader.this.saveConfigToXml(CarrierConfigLoader.this.getCarrierPackageForPhoneId(i), iccIdForPhoneId2, persistableBundle);
                                    CarrierConfigLoader.this.mConfigFromCarrierApp[i] = persistableBundle;
                                    ConfigHandler.this.sendMessage(ConfigHandler.this.obtainMessage(6, i, -1));
                                }
                            }
                        });
                        sendMessageDelayed(obtainMessage(15, i, -1), 30000L);
                        return;
                    } catch (RemoteException e2) {
                        CarrierConfigLoader.loge("Failed to get carrier config: " + e2.toString());
                        CarrierConfigLoader.this.mContext.unbindService(carrierServiceConnection2);
                        return;
                    }
                case 5:
                    if (message.getData().getBoolean("loaded_from_xml", false) || CarrierConfigLoader.this.mServiceConnection[i] != null) {
                        String carrierPackageForPhoneId = CarrierConfigLoader.this.getCarrierPackageForPhoneId(i);
                        if (carrierPackageForPhoneId == null) {
                            CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                            CarrierConfigLoader.this.mServiceConnection[i] = null;
                            return;
                        }
                        CarrierConfigLoader.log("Found carrier config app: " + carrierPackageForPhoneId);
                        sendMessage(obtainMessage(8, Integer.valueOf(i)));
                        return;
                    }
                    return;
                case 6:
                    if (message.getData().getBoolean("loaded_from_xml", false) || CarrierConfigLoader.this.mServiceConnection[i] != null) {
                        CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                        CarrierConfigLoader.this.mServiceConnection[i] = null;
                        return;
                    }
                    return;
                case 7:
                    PersistableBundle persistableBundleRestoreConfigFromXml = CarrierConfigLoader.this.restoreConfigFromXml(CarrierConfigLoader.this.mPlatformCarrierConfigPackage, CarrierConfigLoader.this.getIccIdForPhoneId(i));
                    if (persistableBundleRestoreConfigFromXml == null) {
                        if (!CarrierConfigLoader.this.bindToConfigPackage(CarrierConfigLoader.this.mPlatformCarrierConfigPackage, i, 3)) {
                            CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                            return;
                        } else {
                            sendMessageDelayed(obtainMessage(10, i, -1), 30000L);
                            CarrierConfigLoader.this.mTimeoutId = 10;
                            return;
                        }
                    }
                    CarrierConfigLoader.log("Loaded config from XML. package=" + CarrierConfigLoader.this.mPlatformCarrierConfigPackage + " phoneId=" + i);
                    CarrierConfigLoader.this.mConfigFromDefaultApp[i] = persistableBundleRestoreConfigFromXml;
                    Message messageObtainMessage = obtainMessage(5, i, -1);
                    messageObtainMessage.getData().putBoolean("loaded_from_xml", true);
                    CarrierConfigLoader.this.mHandler.sendMessage(messageObtainMessage);
                    return;
                case 8:
                    String carrierPackageForPhoneId2 = CarrierConfigLoader.this.getCarrierPackageForPhoneId(i);
                    PersistableBundle persistableBundleRestoreConfigFromXml2 = CarrierConfigLoader.this.restoreConfigFromXml(carrierPackageForPhoneId2, CarrierConfigLoader.this.getIccIdForPhoneId(i));
                    if (persistableBundleRestoreConfigFromXml2 != null) {
                        CarrierConfigLoader.log("Loaded config from XML. package=" + carrierPackageForPhoneId2 + " phoneId=" + i);
                        CarrierConfigLoader.this.mConfigFromCarrierApp[i] = persistableBundleRestoreConfigFromXml2;
                        Message messageObtainMessage2 = obtainMessage(6, i, -1);
                        messageObtainMessage2.getData().putBoolean("loaded_from_xml", true);
                        sendMessage(messageObtainMessage2);
                        return;
                    }
                    if (carrierPackageForPhoneId2 == null || !CarrierConfigLoader.this.bindToConfigPackage(carrierPackageForPhoneId2, i, 4)) {
                        CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                        return;
                    } else {
                        sendMessageDelayed(obtainMessage(11, i, -1), 30000L);
                        CarrierConfigLoader.this.mTimeoutId = 11;
                        return;
                    }
                case 9:
                    if (CarrierConfigLoader.this.clearCachedConfigForPackage((String) message.obj)) {
                        while (i3 < phoneCount) {
                            CarrierConfigLoader.this.updateConfigForPhoneId(i3);
                            i3++;
                        }
                        return;
                    }
                    return;
                case 10:
                case 14:
                    if (CarrierConfigLoader.this.mServiceConnection[i] != null) {
                        CarrierConfigLoader.this.mContext.unbindService(CarrierConfigLoader.this.mServiceConnection[i]);
                    }
                    removeMessages(14);
                    CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                    CarrierConfigLoader.this.mServiceConnection[i] = null;
                    return;
                case 11:
                case 15:
                    if (CarrierConfigLoader.this.mServiceConnection[i] != null) {
                        CarrierConfigLoader.this.mContext.unbindService(CarrierConfigLoader.this.mServiceConnection[i]);
                    }
                    removeMessages(15);
                    CarrierConfigLoader.this.broadcastConfigChangedIntent(i);
                    CarrierConfigLoader.this.mServiceConnection[i] = null;
                    return;
                case 12:
                    SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(CarrierConfigLoader.this.mContext);
                    String string = defaultSharedPreferences.getString(CarrierConfigLoader.KEY_FINGERPRINT, null);
                    if (!Build.FINGERPRINT.equals(string)) {
                        CarrierConfigLoader.log("Build fingerprint changed. old: " + string + " new: " + Build.FINGERPRINT);
                        CarrierConfigLoader.this.clearCachedConfigForPackage(null);
                        defaultSharedPreferences.edit().putString(CarrierConfigLoader.KEY_FINGERPRINT, Build.FINGERPRINT).apply();
                        return;
                    }
                    return;
                case 13:
                    break;
                default:
                    return;
            }
            while (i3 < TelephonyManager.from(CarrierConfigLoader.this.mContext).getPhoneCount()) {
                if (CarrierConfigLoader.this.mHasSentConfigChange[i3]) {
                    CarrierConfigLoader.this.updateConfigForPhoneId(i3);
                }
                i3++;
            }
        }
    }

    private CarrierConfigLoader(Context context) {
        this.mBootReceiver = new ConfigLoaderBroadcastReceiver();
        this.mPackageReceiver = new ConfigLoaderBroadcastReceiver();
        this.mContext = context;
        this.mPlatformCarrierConfigPackage = this.mContext.getString(R.string.platform_carrier_config_package);
        this.mHandler = new ConfigHandler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBootReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter2.addDataScheme("package");
        context.registerReceiverAsUser(this.mPackageReceiver, UserHandle.ALL, intentFilter2, null, null);
        int phoneCount = TelephonyManager.from(context).getPhoneCount();
        this.mConfigFromDefaultApp = new PersistableBundle[phoneCount];
        this.mConfigFromCarrierApp = new PersistableBundle[phoneCount];
        this.mServiceConnection = new CarrierServiceConnection[phoneCount];
        this.mHasSentConfigChange = new boolean[phoneCount];
        ServiceManager.addService(TAG_DOCUMENT, this);
        log("CarrierConfigLoader has started");
        this.mHandler.sendEmptyMessage(12);
    }

    static CarrierConfigLoader init(Context context) {
        CarrierConfigLoader carrierConfigLoader;
        synchronized (CarrierConfigLoader.class) {
            if (sInstance == null) {
                sInstance = new CarrierConfigLoader(context);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            carrierConfigLoader = sInstance;
        }
        return carrierConfigLoader;
    }

    private void broadcastConfigChangedIntent(int i) {
        broadcastConfigChangedIntent(i, true);
    }

    private void broadcastConfigChangedIntent(int i, boolean z) {
        Intent intent = new Intent("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intent.addFlags(83886080);
        int simApplicationState = TelephonyManager.from(this.mContext).getSimApplicationState();
        if (z && simApplicationState != 0 && simApplicationState != 6) {
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, i);
        }
        intent.putExtra("android.telephony.extra.SLOT_INDEX", i);
        ActivityManager.broadcastStickyIntent(intent, -1);
        this.mHasSentConfigChange[i] = true;
    }

    private boolean bindToConfigPackage(String str, int i, int i2) {
        log("Binding to " + str + " for phone " + i);
        Intent intent = new Intent("android.service.carrier.CarrierService");
        intent.setPackage(str);
        this.mServiceConnection[i] = new CarrierServiceConnection(i, i2);
        try {
            return this.mContext.bindService(intent, this.mServiceConnection[i], 1);
        } catch (SecurityException e) {
            return false;
        }
    }

    private CarrierIdentifier getCarrierIdForPhoneId(int i) {
        String str;
        String str2;
        String strSubstring = "";
        String strSubstring2 = "";
        String subscriberId = "";
        String groupIdLevel1 = "";
        String groupIdLevel2 = "";
        String simOperatorNameForPhone = TelephonyManager.from(this.mContext).getSimOperatorNameForPhone(i);
        TelephonyManager.from(this.mContext).getSimOperatorNumericForPhone(i);
        if (i == 0) {
            str = "vendor.gsm.ril.uicc.mccmnc";
            str2 = "vendor.cdma.ril.uicc.mccmnc";
        } else {
            str = "vendor.gsm.ril.uicc.mccmnc." + i;
            str2 = "vendor.cdma.ril.uicc.mccmnc." + i;
        }
        String str3 = SystemProperties.get(str, "");
        String str4 = SystemProperties.get(str2, "");
        if (TelephonyManager.from(this.mContext).getCurrentPhoneTypeForSlot(i) == 2 && str4 != "") {
            str3 = str4;
        }
        log("[getCarrierIdForPhoneId]PhoneType:" + TelephonyManager.from(this.mContext).getCurrentPhoneTypeForSlot(i) + " mccmnc:" + str3);
        if (str3 != null && str3.length() >= 3) {
            strSubstring = str3.substring(0, 3);
            strSubstring2 = str3.substring(3);
        }
        String str5 = strSubstring;
        String str6 = strSubstring2;
        Phone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            subscriberId = phone.getSubscriberId();
            groupIdLevel1 = phone.getGroupIdLevel1();
            groupIdLevel2 = phone.getGroupIdLevel2();
        }
        return new CarrierIdentifier(str5, str6, simOperatorNameForPhone, subscriberId, groupIdLevel1, groupIdLevel2);
    }

    private String getCarrierPackageForPhoneId(int i) {
        List carrierPackageNamesForIntentAndPhone = TelephonyManager.from(this.mContext).getCarrierPackageNamesForIntentAndPhone(new Intent("android.service.carrier.CarrierService"), i);
        if (carrierPackageNamesForIntentAndPhone != null && carrierPackageNamesForIntentAndPhone.size() > 0) {
            return (String) carrierPackageNamesForIntentAndPhone.get(0);
        }
        return null;
    }

    private String getIccIdForPhoneId(int i) {
        Phone phone;
        if (SubscriptionManager.isValidPhoneId(i) && (phone = PhoneFactory.getPhone(i)) != null) {
            return phone.getIccSerialNumber();
        }
        return null;
    }

    private void saveConfigToXml(String str, String str2, PersistableBundle persistableBundle) {
        if (str == null || str2 == null) {
            loge("Cannot save config with null packageName or iccid.");
            return;
        }
        if (persistableBundle == null || persistableBundle.isEmpty()) {
            return;
        }
        String packageVersion = getPackageVersion(str);
        if (packageVersion == null) {
            loge("Failed to get package version for: " + str);
            return;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(this.mContext.getFilesDir(), getFilenameForConfig(str, str2)));
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStream, "utf-8");
            fastXmlSerializer.startDocument("utf-8", true);
            fastXmlSerializer.startTag((String) null, TAG_DOCUMENT);
            fastXmlSerializer.startTag((String) null, TAG_VERSION);
            fastXmlSerializer.text(packageVersion);
            fastXmlSerializer.endTag((String) null, TAG_VERSION);
            fastXmlSerializer.startTag((String) null, TAG_BUNDLE);
            persistableBundle.saveToXml(fastXmlSerializer);
            fastXmlSerializer.endTag((String) null, TAG_BUNDLE);
            fastXmlSerializer.endTag((String) null, TAG_DOCUMENT);
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            loge(e.toString());
        } catch (XmlPullParserException e2) {
            loge(e2.toString());
        }
    }

    private PersistableBundle restoreConfigFromXml(String str, String str2) {
        String packageVersion = getPackageVersion(str);
        PersistableBundle persistableBundleRestoreFromXml = null;
        if (packageVersion == null) {
            loge("Failed to get package version for: " + str);
            return null;
        }
        if (str == null || str2 == null) {
            loge("Cannot restore config with null packageName or iccid.");
            return null;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(this.mContext.getFilesDir(), getFilenameForConfig(str, str2)));
            XmlPullParser xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStream, "utf-8");
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next == 1) {
                    break;
                }
                if (next == 2 && TAG_VERSION.equals(xmlPullParserNewPullParser.getName())) {
                    String strNextText = xmlPullParserNewPullParser.nextText();
                    if (!packageVersion.equals(strNextText)) {
                        log("Saved version mismatch: " + packageVersion + " vs " + strNextText);
                        break;
                    }
                }
                if (next == 2 && TAG_BUNDLE.equals(xmlPullParserNewPullParser.getName())) {
                    persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParserNewPullParser);
                }
            }
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            loge(e.toString());
        } catch (IOException e2) {
            loge(e2.toString());
        } catch (XmlPullParserException e3) {
            loge(e3.toString());
        }
        return persistableBundleRestoreFromXml;
    }

    private boolean clearCachedConfigForPackage(final String str) {
        File[] fileArrListFiles = this.mContext.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String str2) {
                if (str != null) {
                    return str2.startsWith("carrierconfig-" + str + "-");
                }
                return str2.startsWith("carrierconfig-");
            }
        });
        if (fileArrListFiles == null || fileArrListFiles.length < 1) {
            return false;
        }
        for (File file : fileArrListFiles) {
            log("deleting " + file.getName());
            file.delete();
        }
        return true;
    }

    private String getFilenameForConfig(String str, String str2) {
        return "carrierconfig-" + str + "-" + str2 + ".xml";
    }

    private String getPackageVersion(String str) {
        try {
            return Long.toString(this.mContext.getPackageManager().getPackageInfo(str, 0).getLongVersionCode());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void updateConfigForPhoneId(int i) {
        if (this.mConfigFromCarrierApp[i] != null && getCarrierPackageForPhoneId(i) == null) {
            this.mConfigFromCarrierApp[i] = null;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7, i, -1));
    }

    public PersistableBundle getConfigForSubId(int i) {
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", null);
        } catch (SecurityException e) {
            try {
                this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
            } catch (SecurityException e2) {
                if (!SubscriptionManager.isValidSubscriptionId(i)) {
                    throw e2;
                }
                TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(i, (String) null);
            }
        }
        int phoneId = SubscriptionManager.getPhoneId(i);
        PersistableBundle defaultConfig = CarrierConfigManager.getDefaultConfig();
        if (TelephonyManager.from(this.mContext).getPhoneCount() > 0 && SubscriptionManager.isValidPhoneId(phoneId)) {
            PersistableBundle persistableBundle = this.mConfigFromDefaultApp[phoneId];
            if (persistableBundle != null) {
                defaultConfig.putAll(persistableBundle);
                defaultConfig.putBoolean("carrier_config_applied_bool", true);
            }
            PersistableBundle persistableBundle2 = this.mConfigFromCarrierApp[phoneId];
            if (persistableBundle2 != null) {
                defaultConfig.putAll(persistableBundle2);
                defaultConfig.putBoolean("carrier_config_applied_bool", true);
            }
        }
        return defaultConfig;
    }

    public void notifyConfigChangedForSubId(int i) {
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            log("Ignore invalid phoneId: " + phoneId + " for subId: " + i);
            return;
        }
        String nameForUid = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (TelephonyManager.from(this.mContext).checkCarrierPrivilegesForPackage(nameForUid) != 1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "Require carrier privileges or MODIFY_PHONE_STATE permission.");
        }
        clearCachedConfigForPackage(nameForUid);
        updateConfigForPhoneId(phoneId);
    }

    public void updateConfigForPhoneId(int i, String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        log("update config for phoneId: " + i + " simState: " + str);
        if (!SubscriptionManager.isValidPhoneId(i)) {
        }
        switch (str) {
            case "ABSENT":
            case "CARD_IO_ERROR":
            case "CARD_RESTRICTED":
            case "UNKNOWN":
                this.mHandler.sendMessage(this.mHandler.obtainMessage(0, i, -1));
                break;
            case "LOADED":
            case "LOCKED":
                updateConfigForPhoneId(i);
                break;
        }
    }

    public String getDefaultCarrierServicePackageName() {
        return this.mPlatformCarrierConfigPackage;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump carrierconfig from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        printWriter.println("CarrierConfigLoader: " + this);
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            printWriter.println("Phone Id = " + i);
            printConfig(CarrierConfigManager.getDefaultConfig(), printWriter, "Default Values from CarrierConfigManager");
            printWriter.println("");
            printConfig(this.mConfigFromDefaultApp[i], printWriter, "mConfigFromDefaultApp");
            printWriter.println("");
            printConfig(this.mConfigFromCarrierApp[i], printWriter, "mConfigFromCarrierApp");
        }
    }

    private void printConfig(PersistableBundle persistableBundle, PrintWriter printWriter, String str) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "    ");
        if (persistableBundle == null) {
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(str + " : null ");
            return;
        }
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println(str + " : ");
        ArrayList<String> arrayList = new ArrayList(persistableBundle.keySet());
        Collections.sort(arrayList);
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.increaseIndent();
        for (String str2 : arrayList) {
            if (persistableBundle.get(str2) != null && (persistableBundle.get(str2) instanceof Object[])) {
                indentingPrintWriter.println(str2 + " = " + Arrays.toString((Object[]) persistableBundle.get(str2)));
            } else if (persistableBundle.get(str2) != null && (persistableBundle.get(str2) instanceof int[])) {
                indentingPrintWriter.println(str2 + " = " + Arrays.toString((int[]) persistableBundle.get(str2)));
            } else {
                indentingPrintWriter.println(str2 + " = " + persistableBundle.get(str2));
            }
        }
    }

    private class CarrierServiceConnection implements ServiceConnection {
        int eventId;
        int phoneId;
        IBinder service;

        public CarrierServiceConnection(int i, int i2) {
            this.phoneId = i;
            this.eventId = i2;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarrierConfigLoader.log("Connected to config app: " + componentName.flattenToString());
            this.service = iBinder;
            CarrierConfigLoader.this.mHandler.sendMessage(CarrierConfigLoader.this.mHandler.obtainMessage(this.eventId, this.phoneId, -1, this));
            CarrierConfigLoader.log("Remove timeout message since service is connected. id = " + CarrierConfigLoader.this.mTimeoutId);
            CarrierConfigLoader.this.mHandler.removeMessages(CarrierConfigLoader.this.mTimeoutId);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            this.service = null;
        }
    }

    private class ConfigLoaderBroadcastReceiver extends BroadcastReceiver {
        private ConfigLoaderBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            byte b = 0;
            if (intent.getBooleanExtra("android.intent.extra.REPLACING", false) && !"android.intent.action.PACKAGE_REPLACED".equals(action)) {
            }
            int iHashCode = action.hashCode();
            if (iHashCode != -810471698) {
                if (iHashCode != 525384130) {
                    if (iHashCode != 798292259) {
                        b = (iHashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) ? (byte) 1 : (byte) -1;
                    } else if (!action.equals("android.intent.action.BOOT_COMPLETED")) {
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    b = 2;
                }
            } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                b = 3;
            }
            switch (b) {
                case 0:
                    CarrierConfigLoader.this.mHandler.sendMessage(CarrierConfigLoader.this.mHandler.obtainMessage(13, null));
                    break;
                case 1:
                case 2:
                case 3:
                    String nameForUid = CarrierConfigLoader.this.mContext.getPackageManager().getNameForUid(intent.getIntExtra("android.intent.extra.UID", -1));
                    if (nameForUid != null) {
                        CarrierConfigLoader.this.mHandler.sendMessage(CarrierConfigLoader.this.mHandler.obtainMessage(9, nameForUid));
                    }
                    break;
            }
        }
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    private static void loge(String str) {
        Log.e(LOG_TAG, str);
    }
}
