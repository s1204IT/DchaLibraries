package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.Window;
import com.android.server.utils.AppInstallerUtil;

public class DeprecatedTargetSdkVersionDialog {
    private static final String TAG = "ActivityManager";
    private final AlertDialog mDialog;
    private final String mPackageName;

    public DeprecatedTargetSdkVersionDialog(final AppWarnings appWarnings, final Context context, ApplicationInfo applicationInfo) {
        this.mPackageName = applicationInfo.packageName;
        AlertDialog.Builder title = new AlertDialog.Builder(context).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                appWarnings.setPackageFlag(this.f$0.mPackageName, 4, true);
            }
        }).setMessage(context.getString(R.string.biometric_error_hw_unavailable)).setTitle(applicationInfo.loadSafeLabel(context.getPackageManager()));
        final Intent intentCreateIntent = AppInstallerUtil.createIntent(context, applicationInfo.packageName);
        if (intentCreateIntent != null) {
            title.setNeutralButton(R.string.biometric_error_generic, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    context.startActivity(intentCreateIntent);
                }
            });
        }
        this.mDialog = title.create();
        this.mDialog.create();
        Window window = this.mDialog.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("DeprecatedTargetSdkVersionDialog");
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void show() {
        Log.w(TAG, "Showing SDK deprecation warning for package " + this.mPackageName);
        this.mDialog.show();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
