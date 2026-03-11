package com.android.settings;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.SmsApplication;

public final class SmsDefaultDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private SmsApplication.SmsApplicationData mNewSmsApplicationData;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String packageName = intent.getStringExtra("package");
        setResult(0);
        if (!buildDialog(packageName)) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -1:
                SmsApplication.setDefaultApplication(this.mNewSmsApplicationData.mPackageName, this);
                setResult(-1);
                break;
        }
    }

    private boolean buildDialog(String packageName) {
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        if (!tm.isSmsCapable()) {
            return false;
        }
        this.mNewSmsApplicationData = SmsApplication.getSmsApplicationData(packageName, this);
        if (this.mNewSmsApplicationData == null) {
            return false;
        }
        SmsApplication.SmsApplicationData oldSmsApplicationData = null;
        ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(this, true);
        if (oldSmsComponent != null) {
            oldSmsApplicationData = SmsApplication.getSmsApplicationData(oldSmsComponent.getPackageName(), this);
            if (oldSmsApplicationData.mPackageName.equals(this.mNewSmsApplicationData.mPackageName)) {
                return false;
            }
        }
        AlertController.AlertParams p = this.mAlertParams;
        p.mTitle = getString(R.string.sms_change_default_dialog_title);
        if (oldSmsApplicationData != null) {
            p.mMessage = getString(R.string.sms_change_default_dialog_text, new Object[]{this.mNewSmsApplicationData.mApplicationName, oldSmsApplicationData.mApplicationName});
        } else {
            p.mMessage = getString(R.string.sms_change_default_no_previous_dialog_text, new Object[]{this.mNewSmsApplicationData.mApplicationName});
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();
        return true;
    }
}
