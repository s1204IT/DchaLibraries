package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class CarrierActionAgent extends Handler {
    public static final int CARRIER_ACTION_REPORT_DEFAULT_NETWORK_STATUS = 3;
    public static final int CARRIER_ACTION_RESET = 2;
    public static final int CARRIER_ACTION_SET_METERED_APNS_ENABLED = 0;
    public static final int CARRIER_ACTION_SET_RADIO_ENABLED = 1;
    private static final boolean DBG = true;
    public static final int EVENT_APM_SETTINGS_CHANGED = 4;
    public static final int EVENT_APN_SETTINGS_CHANGED = 8;
    public static final int EVENT_DATA_ROAMING_OFF = 6;
    public static final int EVENT_MOBILE_DATA_SETTINGS_CHANGED = 5;
    public static final int EVENT_SIM_STATE_CHANGED = 7;
    private static final String LOG_TAG = "CarrierActionAgent";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private final Phone mPhone;
    private final SettingsObserver mSettingsObserver;
    private RegistrantList mMeteredApnEnableRegistrants = new RegistrantList();
    private RegistrantList mRadioEnableRegistrants = new RegistrantList();
    private RegistrantList mDefaultNetworkReportRegistrants = new RegistrantList();
    private LocalLog mMeteredApnEnabledLog = new LocalLog(10);
    private LocalLog mRadioEnabledLog = new LocalLog(10);
    private LocalLog mReportDefaultNetworkStatusLog = new LocalLog(10);
    private Boolean mCarrierActionOnMeteredApnEnabled = true;
    private Boolean mCarrierActionOnRadioEnabled = true;
    private Boolean mCarrierActionReportDefaultNetworkStatus = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("ss");
            if (!"android.intent.action.SIM_STATE_CHANGED".equals(action) || intent.getBooleanExtra("rebroadcastOnUnlock", false)) {
                return;
            }
            CarrierActionAgent.this.sendMessage(CarrierActionAgent.this.obtainMessage(7, stringExtra));
        }
    };

    public CarrierActionAgent(Phone phone) {
        this.mPhone = phone;
        this.mPhone.getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        this.mSettingsObserver = new SettingsObserver(this.mPhone.getContext(), this);
        log("Creating CarrierActionAgent");
    }

    @Override
    public void handleMessage(Message message) {
        Boolean carrierActionEnabled = getCarrierActionEnabled(message.what);
        if (carrierActionEnabled == null || carrierActionEnabled.booleanValue() != ((Boolean) message.obj).booleanValue()) {
            switch (message.what) {
                case 0:
                    this.mCarrierActionOnMeteredApnEnabled = Boolean.valueOf(((Boolean) message.obj).booleanValue());
                    log("SET_METERED_APNS_ENABLED: " + this.mCarrierActionOnMeteredApnEnabled);
                    this.mMeteredApnEnabledLog.log("SET_METERED_APNS_ENABLED: " + this.mCarrierActionOnMeteredApnEnabled);
                    this.mMeteredApnEnableRegistrants.notifyRegistrants(new AsyncResult((Object) null, this.mCarrierActionOnMeteredApnEnabled, (Throwable) null));
                    break;
                case 1:
                    this.mCarrierActionOnRadioEnabled = Boolean.valueOf(((Boolean) message.obj).booleanValue());
                    log("SET_RADIO_ENABLED: " + this.mCarrierActionOnRadioEnabled);
                    this.mRadioEnabledLog.log("SET_RADIO_ENABLED: " + this.mCarrierActionOnRadioEnabled);
                    this.mRadioEnableRegistrants.notifyRegistrants(new AsyncResult((Object) null, this.mCarrierActionOnRadioEnabled, (Throwable) null));
                    break;
                case 2:
                    log("CARRIER_ACTION_RESET");
                    carrierActionReset();
                    break;
                case 3:
                    this.mCarrierActionReportDefaultNetworkStatus = Boolean.valueOf(((Boolean) message.obj).booleanValue());
                    log("CARRIER_ACTION_REPORT_AT_DEFAULT_NETWORK_STATUS: " + this.mCarrierActionReportDefaultNetworkStatus);
                    this.mReportDefaultNetworkStatusLog.log("REGISTER_DEFAULT_NETWORK_STATUS: " + this.mCarrierActionReportDefaultNetworkStatus);
                    this.mDefaultNetworkReportRegistrants.notifyRegistrants(new AsyncResult((Object) null, this.mCarrierActionReportDefaultNetworkStatus, (Throwable) null));
                    break;
                case 4:
                    log("EVENT_APM_SETTINGS_CHANGED");
                    if (Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0) != 0) {
                        carrierActionReset();
                    }
                    break;
                case 5:
                    log("EVENT_MOBILE_DATA_SETTINGS_CHANGED");
                    if (!this.mPhone.isUserDataEnabled()) {
                        carrierActionReset();
                    }
                    break;
                case 6:
                    log("EVENT_DATA_ROAMING_OFF");
                    carrierActionReset();
                    break;
                case 7:
                    String str = (String) message.obj;
                    if ("LOADED".equals(str)) {
                        log("EVENT_SIM_STATE_CHANGED status: " + str);
                        carrierActionReset();
                        String str2 = TelephonyManager.getDefault().getSimCount() != 1 ? "mobile_data" + this.mPhone.getSubId() : "mobile_data";
                        this.mSettingsObserver.unobserve();
                        this.mSettingsObserver.observe(Settings.Global.getUriFor(str2), 5);
                        this.mSettingsObserver.observe(Settings.Global.getUriFor("airplane_mode_on"), 4);
                        this.mSettingsObserver.observe(Telephony.Carriers.CONTENT_URI, 8);
                        if (this.mPhone.getServiceStateTracker() != null) {
                            this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
                            this.mPhone.getServiceStateTracker().registerForDataRoamingOff(this, 6, null, false);
                        }
                    } else if ("ABSENT".equals(str)) {
                        log("EVENT_SIM_STATE_CHANGED status: " + str);
                        carrierActionReset();
                        this.mSettingsObserver.unobserve();
                        if (this.mPhone.getServiceStateTracker() != null) {
                            this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
                        }
                    }
                    break;
                case 8:
                    log("EVENT_APN_SETTINGS_CHANGED");
                    carrierActionReset();
                    break;
                default:
                    loge("Unknown carrier action: " + message.what);
                    break;
            }
        }
    }

    public void carrierActionSetRadioEnabled(boolean z) {
        sendMessage(obtainMessage(1, Boolean.valueOf(z)));
    }

    public void carrierActionSetMeteredApnsEnabled(boolean z) {
        sendMessage(obtainMessage(0, Boolean.valueOf(z)));
    }

    public void carrierActionReportDefaultNetworkStatus(boolean z) {
        sendMessage(obtainMessage(3, Boolean.valueOf(z)));
    }

    private void carrierActionReset() {
        carrierActionReportDefaultNetworkStatus(false);
        carrierActionSetMeteredApnsEnabled(true);
        carrierActionSetRadioEnabled(true);
        this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(new Intent("com.android.internal.telephony.CARRIER_SIGNAL_RESET"));
    }

    private RegistrantList getRegistrantsFromAction(int i) {
        if (i != 3) {
            switch (i) {
                case 0:
                    return this.mMeteredApnEnableRegistrants;
                case 1:
                    return this.mRadioEnableRegistrants;
                default:
                    loge("Unsupported action: " + i);
                    return null;
            }
        }
        return this.mDefaultNetworkReportRegistrants;
    }

    private Boolean getCarrierActionEnabled(int i) {
        if (i != 3) {
            switch (i) {
                case 0:
                    return this.mCarrierActionOnMeteredApnEnabled;
                case 1:
                    return this.mCarrierActionOnRadioEnabled;
                default:
                    loge("Unsupported action: " + i);
                    return null;
            }
        }
        return this.mCarrierActionReportDefaultNetworkStatus;
    }

    public void registerForCarrierAction(int i, Handler handler, int i2, Object obj, boolean z) {
        Boolean carrierActionEnabled = getCarrierActionEnabled(i);
        if (carrierActionEnabled == null) {
            throw new IllegalArgumentException("invalid carrier action: " + i);
        }
        RegistrantList registrantsFromAction = getRegistrantsFromAction(i);
        Registrant registrant = new Registrant(handler, i2, obj);
        registrantsFromAction.add(registrant);
        if (z) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, carrierActionEnabled, (Throwable) null));
        }
    }

    public void unregisterForCarrierAction(Handler handler, int i) {
        RegistrantList registrantsFromAction = getRegistrantsFromAction(i);
        if (registrantsFromAction == null) {
            throw new IllegalArgumentException("invalid carrier action: " + i);
        }
        registrantsFromAction.remove(handler);
    }

    @VisibleForTesting
    public ContentObserver getContentObserver() {
        return this.mSettingsObserver;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        printWriter.println(" mCarrierActionOnMeteredApnsEnabled Log:");
        indentingPrintWriter.increaseIndent();
        this.mMeteredApnEnabledLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        printWriter.println(" mCarrierActionOnRadioEnabled Log:");
        indentingPrintWriter.increaseIndent();
        this.mRadioEnabledLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        printWriter.println(" mCarrierActionReportDefaultNetworkStatus Log:");
        indentingPrintWriter.increaseIndent();
        this.mReportDefaultNetworkStatusLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }
}
