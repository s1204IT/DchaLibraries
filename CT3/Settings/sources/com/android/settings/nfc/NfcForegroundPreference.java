package com.android.settings.nfc;

import android.content.Context;
import android.support.v7.preference.DropDownPreference;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend;

public class NfcForegroundPreference extends DropDownPreference implements PaymentBackend.Callback {
    private final PaymentBackend mPaymentBackend;

    public NfcForegroundPreference(Context context, PaymentBackend backend) {
        super(context);
        this.mPaymentBackend = backend;
        this.mPaymentBackend.registerCallback(this);
        refresh();
    }

    @Override
    public void onPaymentAppsChanged() {
        refresh();
    }

    void refresh() {
        this.mPaymentBackend.getDefaultApp();
        boolean foregroundMode = this.mPaymentBackend.isForegroundMode();
        setPersistent(false);
        setTitle(getContext().getString(R.string.nfc_payment_use_default));
        setEntries(new CharSequence[]{getContext().getString(R.string.nfc_payment_favor_open), getContext().getString(R.string.nfc_payment_favor_default)});
        setEntryValues(new CharSequence[]{"1", "0"});
        if (foregroundMode) {
            setValue("1");
        } else {
            setValue("0");
        }
    }

    @Override
    protected boolean persistString(String value) {
        this.mPaymentBackend.setForegroundMode(Integer.parseInt(value) != 0);
        return true;
    }
}
