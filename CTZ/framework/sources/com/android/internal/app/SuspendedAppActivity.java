package com.android.internal.app;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.notification.ZenModeConfig;
import android.util.Slog;
import com.android.internal.R;
import com.android.internal.app.AlertController;

public class SuspendedAppActivity extends AlertActivity implements DialogInterface.OnClickListener {
    public static final String EXTRA_DIALOG_MESSAGE = "SuspendedAppActivity.extra.DIALOG_MESSAGE";
    public static final String EXTRA_SUSPENDED_PACKAGE = "SuspendedAppActivity.extra.SUSPENDED_PACKAGE";
    public static final String EXTRA_SUSPENDING_PACKAGE = "SuspendedAppActivity.extra.SUSPENDING_PACKAGE";
    private static final String TAG = "SuspendedAppActivity";
    private Intent mMoreDetailsIntent;
    private PackageManager mPm;
    private int mUserId;

    private CharSequence getAppLabel(String str) {
        try {
            return this.mPm.getApplicationInfoAsUser(str, 0, this.mUserId).loadLabel(this.mPm);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Package " + str + " not found", e);
            return str;
        }
    }

    private Intent getMoreDetailsActivity(String str, String str2, int i) {
        Intent intent = new Intent(Intent.ACTION_SHOW_SUSPENDED_APP_DETAILS).setPackage(str);
        ResolveInfo resolveInfoResolveActivityAsUser = this.mPm.resolveActivityAsUser(intent, 0, i);
        if (resolveInfoResolveActivityAsUser != null && resolveInfoResolveActivityAsUser.activityInfo != null && Manifest.permission.SEND_SHOW_SUSPENDED_APP_DETAILS.equals(resolveInfoResolveActivityAsUser.activityInfo.permission)) {
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, str2).setFlags(335544320);
            return intent;
        }
        return null;
    }

    @Override
    public void onCreate(Bundle bundle) {
        String string;
        super.onCreate(bundle);
        this.mPm = getPackageManager();
        getWindow().setType(2008);
        Intent intent = getIntent();
        this.mUserId = intent.getIntExtra(Intent.EXTRA_USER_ID, -1);
        if (this.mUserId < 0) {
            Slog.wtf(TAG, "Invalid user: " + this.mUserId);
            finish();
            return;
        }
        String stringExtra = intent.getStringExtra(EXTRA_DIALOG_MESSAGE);
        String stringExtra2 = intent.getStringExtra(EXTRA_SUSPENDED_PACKAGE);
        String stringExtra3 = intent.getStringExtra(EXTRA_SUSPENDING_PACKAGE);
        CharSequence appLabel = getAppLabel(stringExtra2);
        if (stringExtra != null) {
            string = String.format(getResources().getConfiguration().getLocales().get(0), stringExtra, appLabel);
        } else {
            string = getString(R.string.app_suspended_default_message, appLabel, getAppLabel(stringExtra3));
        }
        AlertController.AlertParams alertParams = this.mAlertParams;
        alertParams.mTitle = getString(R.string.app_suspended_title);
        alertParams.mMessage = string;
        alertParams.mPositiveButtonText = getString(17039370);
        this.mMoreDetailsIntent = getMoreDetailsActivity(stringExtra3, stringExtra2, this.mUserId);
        if (this.mMoreDetailsIntent != null) {
            alertParams.mNeutralButtonText = getString(R.string.app_suspended_more_details);
        }
        alertParams.mNeutralButtonListener = this;
        alertParams.mPositiveButtonListener = this;
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -3) {
            startActivityAsUser(this.mMoreDetailsIntent, UserHandle.of(this.mUserId));
            Slog.i(TAG, "Started more details activity");
        }
        finish();
    }

    public static Intent createSuspendedAppInterceptIntent(String str, String str2, String str3, int i) {
        return new Intent().setClassName(ZenModeConfig.SYSTEM_AUTHORITY, SuspendedAppActivity.class.getName()).putExtra(EXTRA_SUSPENDED_PACKAGE, str).putExtra(EXTRA_DIALOG_MESSAGE, str3).putExtra(EXTRA_SUSPENDING_PACKAGE, str2).putExtra(Intent.EXTRA_USER_ID, i).setFlags(276824064);
    }
}
