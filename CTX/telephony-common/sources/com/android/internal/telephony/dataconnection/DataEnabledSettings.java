package com.android.internal.telephony.dataconnection;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Pair;
import com.android.internal.telephony.Phone;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DataEnabledSettings {
    private static final String LOG_TAG = "DataEnabledSettings";
    public static final int REASON_DATA_ENABLED_BY_CARRIER = 4;
    public static final int REASON_INTERNAL_DATA_ENABLED = 1;
    public static final int REASON_POLICY_DATA_ENABLED = 3;
    public static final int REASON_REGISTERED = 0;
    public static final int REASON_USER_DATA_ENABLED = 2;
    private Phone mPhone;
    private ContentResolver mResolver;
    private boolean mInternalDataEnabled = true;
    private boolean mPolicyDataEnabled = true;
    private boolean mCarrierDataEnabled = true;
    private final RegistrantList mDataEnabledChangedRegistrants = new RegistrantList();
    private final LocalLog mSettingChangeLocalLog = new LocalLog(50);

    public String toString() {
        return "[mInternalDataEnabled=" + this.mInternalDataEnabled + ", isUserDataEnabled=" + isUserDataEnabled() + ", isProvisioningDataEnabled=" + isProvisioningDataEnabled() + ", mPolicyDataEnabled=" + this.mPolicyDataEnabled + ", mCarrierDataEnabled=" + this.mCarrierDataEnabled + "]";
    }

    public DataEnabledSettings(Phone phone) {
        this.mPhone = null;
        this.mResolver = null;
        this.mPhone = phone;
        this.mResolver = this.mPhone.getContext().getContentResolver();
    }

    public synchronized void setInternalDataEnabled(boolean z) {
        localLog("InternalDataEnabled", z);
        boolean zIsDataEnabled = isDataEnabled();
        this.mInternalDataEnabled = z;
        if (zIsDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!zIsDataEnabled, 1);
        }
    }

    public synchronized boolean isInternalDataEnabled() {
        return this.mInternalDataEnabled;
    }

    public synchronized void setUserDataEnabled(boolean z) {
        localLog("UserDataEnabled", z);
        boolean zIsDataEnabled = isDataEnabled();
        Settings.Global.putInt(this.mResolver, getMobileDataSettingName(), z ? 1 : 0);
        if (zIsDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!zIsDataEnabled, 2);
        }
    }

    public synchronized boolean isUserDataEnabled() {
        return Settings.Global.getInt(this.mResolver, getMobileDataSettingName(), "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true")) ? 1 : 0) != 0;
    }

    private String getMobileDataSettingName() {
        int subId = this.mPhone.getSubId();
        if (TelephonyManager.getDefault().getSimCount() == 1 || !SubscriptionManager.isValidSubscriptionId(subId)) {
            return "mobile_data";
        }
        return "mobile_data" + this.mPhone.getSubId();
    }

    public synchronized void setPolicyDataEnabled(boolean z) {
        localLog("PolicyDataEnabled", z);
        boolean zIsDataEnabled = isDataEnabled();
        this.mPolicyDataEnabled = z;
        if (zIsDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!zIsDataEnabled, 3);
        }
    }

    public synchronized boolean isPolicyDataEnabled() {
        return this.mPolicyDataEnabled;
    }

    public synchronized void setCarrierDataEnabled(boolean z) {
        localLog("CarrierDataEnabled", z);
        boolean zIsDataEnabled = isDataEnabled();
        this.mCarrierDataEnabled = z;
        if (zIsDataEnabled != isDataEnabled()) {
            notifyDataEnabledChanged(!zIsDataEnabled, 4);
        }
    }

    public synchronized boolean isCarrierDataEnabled() {
        return this.mCarrierDataEnabled;
    }

    public synchronized boolean isDataEnabled() {
        if (isProvisioning()) {
            return isProvisioningDataEnabled();
        }
        return this.mInternalDataEnabled && isUserDataEnabled() && this.mPolicyDataEnabled && this.mCarrierDataEnabled;
    }

    public boolean isProvisioning() {
        return Settings.Global.getInt(this.mResolver, "device_provisioned", 0) == 0;
    }

    public boolean isProvisioningDataEnabled() {
        String str = SystemProperties.get("ro.com.android.prov_mobiledata", "false");
        int i = Settings.Global.getInt(this.mResolver, "device_provisioning_mobile_data", "true".equalsIgnoreCase(str) ? 1 : 0);
        boolean z = i != 0;
        log("getDataEnabled during provisioning retVal=" + z + " - (" + str + ", " + i + ")");
        return z;
    }

    private void notifyDataEnabledChanged(boolean z, int i) {
        this.mDataEnabledChangedRegistrants.notifyResult(new Pair(Boolean.valueOf(z), Integer.valueOf(i)));
    }

    public void registerForDataEnabledChanged(Handler handler, int i, Object obj) {
        this.mDataEnabledChangedRegistrants.addUnique(handler, i, obj);
        notifyDataEnabledChanged(isDataEnabled(), 0);
    }

    public void unregisterForDataEnabledChanged(Handler handler) {
        this.mDataEnabledChangedRegistrants.remove(handler);
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void localLog(String str, boolean z) {
        this.mSettingChangeLocalLog.log(str + " change to " + z);
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(" DataEnabledSettings=");
        this.mSettingChangeLocalLog.dump(fileDescriptor, printWriter, strArr);
    }
}
