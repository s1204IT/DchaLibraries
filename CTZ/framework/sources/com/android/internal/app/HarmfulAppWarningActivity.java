package com.android.internal.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.app.AlertController;

public class HarmfulAppWarningActivity extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String EXTRA_HARMFUL_APP_WARNING = "harmful_app_warning";
    private static final String TAG = HarmfulAppWarningActivity.class.getSimpleName();
    private String mHarmfulAppWarning;
    private String mPackageName;
    private IntentSender mTarget;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        this.mPackageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        this.mTarget = (IntentSender) intent.getParcelableExtra(Intent.EXTRA_INTENT);
        this.mHarmfulAppWarning = intent.getStringExtra(EXTRA_HARMFUL_APP_WARNING);
        if (this.mPackageName == null || this.mTarget == null || this.mHarmfulAppWarning == null) {
            Log.wtf(TAG, "Invalid intent: " + intent.toString());
            finish();
        }
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(this.mPackageName, 0);
            AlertController.AlertParams alertParams = this.mAlertParams;
            alertParams.mTitle = getString(R.string.harmful_app_warning_title);
            alertParams.mView = createView(applicationInfo);
            alertParams.mPositiveButtonText = getString(R.string.harmful_app_warning_uninstall);
            alertParams.mPositiveButtonListener = this;
            alertParams.mNegativeButtonText = getString(R.string.harmful_app_warning_open_anyway);
            alertParams.mNegativeButtonListener = this;
            this.mAlert.installContent(this.mAlertParams);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not show warning because package does not exist ", e);
            finish();
        }
    }

    private View createView(ApplicationInfo applicationInfo) {
        View viewInflate = getLayoutInflater().inflate(R.layout.harmful_app_warning_dialog, (ViewGroup) null);
        ((TextView) viewInflate.findViewById(R.id.app_name_text)).setText(applicationInfo.loadSafeLabel(getPackageManager()));
        ((TextView) viewInflate.findViewById(16908299)).setText(this.mHarmfulAppWarning);
        return viewInflate;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                getPackageManager().setHarmfulAppWarning(this.mPackageName, null);
                try {
                    startIntentSenderForResult((IntentSender) getIntent().getParcelableExtra(Intent.EXTRA_INTENT), -1, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Error while starting intent sender", e);
                }
                EventLogTags.writeHarmfulAppWarningLaunchAnyway(this.mPackageName);
                finish();
                break;
            case -1:
                getPackageManager().deletePackage(this.mPackageName, null, 0);
                EventLogTags.writeHarmfulAppWarningUninstall(this.mPackageName);
                finish();
                break;
        }
    }

    public static Intent createHarmfulAppWarningIntent(Context context, String str, IntentSender intentSender, CharSequence charSequence) {
        Intent intent = new Intent();
        intent.setClass(context, HarmfulAppWarningActivity.class);
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, str);
        intent.putExtra(Intent.EXTRA_INTENT, intentSender);
        intent.putExtra(EXTRA_HARMFUL_APP_WARNING, charSequence);
        return intent;
    }
}
