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
import android.provider.Settings;
import com.android.internal.content.PackageMonitor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class PaymentBackend {
    private final NfcAdapter mAdapter;
    private ArrayList<PaymentAppInfo> mAppInfos;
    private final CardEmulation mCardEmuManager;
    private final Context mContext;
    private PaymentAppInfo mDefaultAppInfo;
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();
    private ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler() { // from class: com.android.settings.nfc.PaymentBackend.1
        @Override // android.os.Handler
        public void dispatchMessage(Message message) {
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
        public CharSequence label;
        public ComponentName settingsComponent;
    }

    public PaymentBackend(Context context) {
        this.mContext = context;
        this.mAdapter = NfcAdapter.getDefaultAdapter(context);
        this.mCardEmuManager = CardEmulation.getInstance(this.mAdapter);
        refresh();
    }

    public void onPause() {
        this.mSettingsPackageMonitor.unregister();
    }

    public void onResume() {
        this.mSettingsPackageMonitor.register(this.mContext, this.mContext.getMainLooper(), false);
        refresh();
    }

    public void refresh() {
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ApduServiceInfo> services = this.mCardEmuManager.getServices("payment");
        ArrayList<PaymentAppInfo> arrayList = new ArrayList<>();
        if (services == null) {
            makeCallbacks();
            return;
        }
        ComponentName defaultPaymentApp = getDefaultPaymentApp();
        PaymentAppInfo paymentAppInfo = null;
        for (ApduServiceInfo apduServiceInfo : services) {
            PaymentAppInfo paymentAppInfo2 = new PaymentAppInfo();
            paymentAppInfo2.label = apduServiceInfo.loadLabel(packageManager);
            if (paymentAppInfo2.label == null) {
                paymentAppInfo2.label = apduServiceInfo.loadAppLabel(packageManager);
            }
            paymentAppInfo2.isDefault = apduServiceInfo.getComponent().equals(defaultPaymentApp);
            if (paymentAppInfo2.isDefault) {
                paymentAppInfo = paymentAppInfo2;
            }
            paymentAppInfo2.componentName = apduServiceInfo.getComponent();
            String settingsActivityName = apduServiceInfo.getSettingsActivityName();
            if (settingsActivityName != null) {
                paymentAppInfo2.settingsComponent = new ComponentName(paymentAppInfo2.componentName.getPackageName(), settingsActivityName);
            } else {
                paymentAppInfo2.settingsComponent = null;
            }
            paymentAppInfo2.description = apduServiceInfo.getDescription();
            paymentAppInfo2.banner = apduServiceInfo.loadBanner(packageManager);
            arrayList.add(paymentAppInfo2);
        }
        this.mAppInfos = arrayList;
        this.mDefaultAppInfo = paymentAppInfo;
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
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onPaymentAppsChanged();
        }
    }

    boolean isForegroundMode() {
        try {
            return Settings.Secure.getInt(this.mContext.getContentResolver(), "nfc_payment_foreground") != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    void setForegroundMode(boolean z) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "nfc_payment_foreground", z ? 1 : 0);
    }

    ComponentName getDefaultPaymentApp() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "nfc_payment_default_component");
        if (string != null) {
            return ComponentName.unflattenFromString(string);
        }
        return null;
    }

    public void setDefaultPaymentApp(ComponentName componentName) {
        Settings.Secure.putString(this.mContext.getContentResolver(), "nfc_payment_default_component", componentName != null ? componentName.flattenToString() : null);
        refresh();
    }

    private class SettingsPackageMonitor extends PackageMonitor {
        private SettingsPackageMonitor() {
        }

        public void onPackageAdded(String str, int i) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageAppeared(String str, int i) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageDisappeared(String str, int i) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }

        public void onPackageRemoved(String str, int i) {
            PaymentBackend.this.mHandler.obtainMessage().sendToTarget();
        }
    }
}
