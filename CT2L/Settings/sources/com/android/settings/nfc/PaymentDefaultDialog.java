package com.android.settings.nfc;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.nfc.PaymentBackend;
import java.util.List;

public final class PaymentDefaultDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private PaymentBackend mBackend;
    private ComponentName mNewDefault;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mBackend = new PaymentBackend(this);
        Intent intent = getIntent();
        ComponentName component = (ComponentName) intent.getParcelableExtra("component");
        String category = intent.getStringExtra("category");
        setResult(0);
        if (!buildDialog(component, category)) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -1:
                this.mBackend.setDefaultPaymentApp(this.mNewDefault);
                setResult(-1);
                break;
        }
    }

    private boolean buildDialog(ComponentName component, String category) {
        if (component == null || category == null) {
            Log.e("PaymentDefaultDialog", "Component or category are null");
            return false;
        }
        if (!"payment".equals(category)) {
            Log.e("PaymentDefaultDialog", "Don't support defaults for category " + category);
            return false;
        }
        PaymentBackend.PaymentAppInfo requestedPaymentApp = null;
        PaymentBackend.PaymentAppInfo defaultPaymentApp = null;
        List<PaymentBackend.PaymentAppInfo> services = this.mBackend.getPaymentAppInfos();
        for (PaymentBackend.PaymentAppInfo service : services) {
            if (component.equals(service.componentName)) {
                requestedPaymentApp = service;
            }
            if (service.isDefault) {
                defaultPaymentApp = service;
            }
        }
        if (requestedPaymentApp == null) {
            Log.e("PaymentDefaultDialog", "Component " + component + " is not a registered payment service.");
            return false;
        }
        ComponentName defaultComponent = this.mBackend.getDefaultPaymentApp();
        if (defaultComponent != null && defaultComponent.equals(component)) {
            Log.e("PaymentDefaultDialog", "Component " + component + " is already default.");
            return false;
        }
        this.mNewDefault = component;
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.nfc_payment_set_default_label);
        if (defaultPaymentApp == null) {
            String formatString = getString(R.string.nfc_payment_set_default);
            String msg = String.format(formatString, sanitizePaymentAppCaption(requestedPaymentApp.caption.toString()));
            p.mMessage = msg;
        } else {
            String formatString2 = getString(R.string.nfc_payment_set_default_instead_of);
            String msg2 = String.format(formatString2, sanitizePaymentAppCaption(requestedPaymentApp.caption.toString()), sanitizePaymentAppCaption(defaultPaymentApp.caption.toString()));
            p.mMessage = msg2;
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();
        return true;
    }

    private String sanitizePaymentAppCaption(String input) {
        String sanitizedString = input.replace('\n', ' ').replace('\r', ' ').trim();
        if (sanitizedString.length() > 40) {
            return sanitizedString.substring(0, 40);
        }
        return sanitizedString;
    }
}
