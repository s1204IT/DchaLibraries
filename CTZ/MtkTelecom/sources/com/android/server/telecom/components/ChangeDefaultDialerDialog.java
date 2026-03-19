package com.android.server.telecom.components;

import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telecom.DefaultDialerManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.server.telecom.R;

public class ChangeDefaultDialerDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = ChangeDefaultDialerDialog.class.getSimpleName();
    private String mNewPackage;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(this);
        this.mNewPackage = getIntent().getStringExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME");
        if (!canChangeToProvidedPackage(defaultDialerApplication, this.mNewPackage)) {
            setResult(0);
            finish();
        }
        buildDialog(this.mNewPackage);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                setResult(0);
                break;
            case -1:
                TelecomManager.from(this).setDefaultDialer(this.mNewPackage);
                setResult(-1);
                break;
        }
    }

    public void onStart() {
        super.onStart();
        getWindow().addPrivateFlags(524288);
    }

    public void onStop() {
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.privateFlags &= -524289;
        window.setAttributes(attributes);
        super.onStop();
    }

    private boolean canChangeToProvidedPackage(String str, String str2) {
        if (!((TelephonyManager) getSystemService("phone")).isVoiceCapable()) {
            Log.w(TAG, "Dialog launched but device is not voice capable.");
            return false;
        }
        if (!DefaultDialerManager.getInstalledDialerApplications(this).contains(str2)) {
            Log.w(TAG, "Provided package name does not correspond to an installed Phone application.");
            return false;
        }
        if (!TextUtils.isEmpty(str) && TextUtils.equals(str, str2)) {
            Log.w(TAG, "Provided package name is already the current default Phone application.");
            return false;
        }
        return true;
    }

    private boolean buildDialog(String str) {
        String applicationLabelForPackageName = getApplicationLabelForPackageName(getPackageManager(), str);
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mTitle = getString(R.string.change_default_dialer_dialog_title, new Object[]{applicationLabelForPackageName});
        alertParams.mMessage = getString(R.string.change_default_dialer_warning_message, new Object[]{applicationLabelForPackageName});
        alertParams.mPositiveButtonText = getString(R.string.change_default_dialer_dialog_affirmative);
        alertParams.mNegativeButtonText = getString(R.string.change_default_dialer_dialog_negative);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonListener = this;
        setupAlert();
        return true;
    }

    private String getApplicationLabelForPackageName(PackageManager packageManager, String str) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Application info not found for packageName " + str);
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return str;
        }
        return applicationInfo.loadLabel(packageManager).toString();
    }
}
