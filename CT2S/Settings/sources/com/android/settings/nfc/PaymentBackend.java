package com.android.settings.nfc;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;

public class PaymentBackend {
    private final NfcAdapter mAdapter;
    private final CardEmulation mCardEmuManager;
    private final Context mContext;

    public static class PaymentAppInfo {
        Drawable banner;
        CharSequence caption;
        public ComponentName componentName;
        boolean isDefault;
    }

    public PaymentBackend(Context context) {
        this.mContext = context;
        this.mAdapter = NfcAdapter.getDefaultAdapter(context);
        this.mCardEmuManager = CardEmulation.getInstance(this.mAdapter);
    }

    public List<PaymentAppInfo> getPaymentAppInfos() {
        PackageManager pm = this.mContext.getPackageManager();
        List<ApduServiceInfo> serviceInfos = this.mCardEmuManager.getServices("payment");
        List<PaymentAppInfo> appInfos = new ArrayList<>();
        if (serviceInfos != null) {
            ComponentName defaultApp = getDefaultPaymentApp();
            for (ApduServiceInfo service : serviceInfos) {
                PaymentAppInfo appInfo = new PaymentAppInfo();
                appInfo.banner = service.loadBanner(pm);
                appInfo.caption = service.getDescription();
                if (appInfo.caption == null) {
                    appInfo.caption = service.loadLabel(pm);
                }
                appInfo.isDefault = service.getComponent().equals(defaultApp);
                appInfo.componentName = service.getComponent();
                appInfos.add(appInfo);
            }
        }
        return appInfos;
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
    }
}
