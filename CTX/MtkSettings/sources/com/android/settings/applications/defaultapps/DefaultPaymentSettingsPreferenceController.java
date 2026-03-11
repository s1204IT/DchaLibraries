package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.nfc.PaymentBackend;
import com.android.settingslib.core.AbstractPreferenceController;

public class DefaultPaymentSettingsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final NfcAdapter mNfcAdapter;
    private final PackageManager mPackageManager;
    private PaymentBackend mPaymentBackend;
    private final UserManager mUserManager;

    public DefaultPaymentSettingsPreferenceController(Context context) {
        super(context);
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this.mContext);
    }

    @Override
    public boolean isAvailable() {
        return this.mPackageManager.hasSystemFeature("android.hardware.nfc") && this.mPackageManager.hasSystemFeature("android.hardware.nfc.hce") && this.mUserManager.isAdminUser() && this.mNfcAdapter != null && this.mNfcAdapter.isEnabled();
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mPaymentBackend == null) {
            if (this.mNfcAdapter != null) {
                this.mPaymentBackend = new PaymentBackend(this.mContext);
            } else {
                this.mPaymentBackend = null;
            }
        }
        if (this.mPaymentBackend == null) {
            return;
        }
        this.mPaymentBackend.refresh();
        PaymentBackend.PaymentAppInfo defaultApp = this.mPaymentBackend.getDefaultApp();
        if (defaultApp != null) {
            preference.setSummary(defaultApp.label);
        } else {
            preference.setSummary(R.string.app_list_preference_none);
        }
    }

    @Override
    public String getPreferenceKey() {
        return "default_payment_app";
    }
}
