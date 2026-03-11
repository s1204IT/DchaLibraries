package com.android.settings.nfc;

import android.content.Context;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend;

public class NfcForegroundPreference extends DropDownPreference implements Preference.OnPreferenceChangeListener, PaymentBackend.Callback {
    private final PaymentBackend mPaymentBackend;

    public NfcForegroundPreference(Context context, PaymentBackend paymentBackend) {
        super(context);
        this.mPaymentBackend = paymentBackend;
        this.mPaymentBackend.registerCallback(this);
        setTitle(getContext().getString(R.string.nfc_payment_use_default));
        setEntries(new CharSequence[]{getContext().getString(R.string.nfc_payment_favor_open), getContext().getString(R.string.nfc_payment_favor_default)});
        setEntryValues(new CharSequence[]{"1", "0"});
        refresh();
        setOnPreferenceChangeListener(this);
    }

    @Override
    public void onPaymentAppsChanged() {
        refresh();
    }

    void refresh() {
        if (this.mPaymentBackend.isForegroundMode()) {
            setValue("1");
        } else {
            setValue("0");
        }
        setSummary(getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String str = (String) obj;
        setSummary(getEntries()[findIndexOfValue(str)]);
        this.mPaymentBackend.setForegroundMode(Integer.parseInt(str) != 0);
        return true;
    }
}
