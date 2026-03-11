package com.android.settings.nfc;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.mediatek.nfcgsma_extras.GSMAOffHostAppInfo;
import com.mediatek.nfcgsma_extras.INfcAdapterGsmaExtras;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class PaymentBackend {
    private final NfcAdapter mAdapter;
    private ArrayList<PaymentAppInfo> mAppInfos;
    private final CardEmulation mCardEmuManager;
    private final Context mContext;
    private PaymentAppInfo mDefaultAppInfo;
    private final INfcAdapterGsmaExtras mGsmaEx;
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor(this, null);
    private ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            PaymentBackend.this.refresh();
        }
    };

    public interface Callback {
        void onPaymentAppsChanged();
    }

    public static class PaymentAppInfo {
        Drawable banner;
        public ComponentName componentName;
        CharSequence description;
        boolean isDefault;
        CharSequence label;
        public ComponentName settingsComponent;
    }

    public PaymentBackend(Context context) {
        this.mContext = context;
        this.mAdapter = NfcAdapter.getDefaultAdapter(context);
        this.mCardEmuManager = CardEmulation.getInstance(this.mAdapter);
        this.mGsmaEx = this.mAdapter.getNfcAdapterGsmaExtrasInterface();
        refresh();
    }

    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
    }

    public void onResume() {
        this.mSettingsPackageMonitor.register(this.mContext, this.mContext.getMainLooper(), false);
    }

    public void refresh() {
        PackageManager pm = this.mContext.getPackageManager();
        List<ApduServiceInfo> serviceInfos = this.mCardEmuManager.getServices("payment");
        ArrayList<PaymentAppInfo> appInfos = new ArrayList<>();
        if (serviceInfos == null) {
            makeCallbacks();
            return;
        }
        ComponentName defaultAppName = getDefaultPaymentApp();
        PaymentAppInfo foundDefaultApp = null;
        for (ApduServiceInfo service : serviceInfos) {
            PaymentAppInfo appInfo = new PaymentAppInfo();
            appInfo.label = service.loadLabel(pm);
            if (appInfo.label == null) {
                appInfo.label = service.loadAppLabel(pm);
            }
            appInfo.isDefault = service.getComponent().equals(defaultAppName);
            if (appInfo.isDefault) {
                foundDefaultApp = appInfo;
            }
            appInfo.componentName = service.getComponent();
            String settingsActivity = service.getSettingsActivityName();
            if (settingsActivity != null) {
                appInfo.settingsComponent = new ComponentName(appInfo.componentName.getPackageName(), settingsActivity);
            } else {
                appInfo.settingsComponent = null;
            }
            appInfo.description = service.getDescription();
            appInfo.banner = service.loadBanner(pm);
            appInfos.add(appInfo);
        }
        if (FeatureOption.MTK_NFC_GSMA_SUPPORT) {
            try {
                List<GSMAOffHostAppInfo> GSMAOffHostAppInfos = this.mGsmaEx.getGSMAOffHostAppInfos();
                for (GSMAOffHostAppInfo offHostAppInfo : GSMAOffHostAppInfos) {
                    PaymentAppInfo gsmaPaymentAppInfo = new PaymentAppInfo();
                    gsmaPaymentAppInfo.label = offHostAppInfo.getLabel();
                    gsmaPaymentAppInfo.description = offHostAppInfo.getDescription();
                    gsmaPaymentAppInfo.banner = offHostAppInfo.getBanner();
                    gsmaPaymentAppInfo.componentName = offHostAppInfo.getComponentName();
                    gsmaPaymentAppInfo.isDefault = gsmaPaymentAppInfo.componentName.equals(defaultAppName);
                    if (gsmaPaymentAppInfo.isDefault) {
                        foundDefaultApp = gsmaPaymentAppInfo;
                    }
                    gsmaPaymentAppInfo.settingsComponent = null;
                    appInfos.add(gsmaPaymentAppInfo);
                }
            } catch (RemoteException e) {
                Log.e("Settings.PaymentBackend", "Fail: GSMAOffHostAppInfo - " + e);
            }
        }
        this.mAppInfos = appInfos;
        this.mDefaultAppInfo = foundDefaultApp;
        makeCallbacks();
    }

    public void registerCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public List<PaymentAppInfo> getPaymentAppInfos() {
        return this.mAppInfos;
    }

    public PaymentAppInfo getDefaultApp() {
        return this.mDefaultAppInfo;
    }

    void makeCallbacks() {
        for (Callback callback : this.mCallbacks) {
            callback.onPaymentAppsChanged();
        }
    }

    boolean isForegroundMode() {
        try {
            return Settings.Secure.getInt(this.mContext.getContentResolver(), "nfc_payment_foreground") != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    void setForegroundMode(boolean foreground) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "nfc_payment_foreground", foreground ? 1 : 0);
    }

    ComponentName getDefaultPaymentApp() {
        String componentString = Settings.Secure.getString(this.mContext.getContentResolver(), "nfc_payment_default_component");
        if (componentString != null) {
            return ComponentName.unflattenFromString(componentString);
        }
        return null;
    }

    public void setDefaultPaymentApp(ComponentName app) {
        Settings.Secure.putString(this.mContext.getContentResolver(), "nfc_payment_default_component", app != null ? app.flattenToString() : null);
        refresh();
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        SettingsPackageMonitor(PaymentBackend this$0, SettingsPackageMonitor settingsPackageMonitor) {
            this();
        }

        private SettingsPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int uid) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageAppeared(String packageName, int reason) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageDisappeared(String packageName, int reason) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageRemoved(String packageName, int uid) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }
    }
}
