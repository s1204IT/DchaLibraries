package com.android.packageinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.packageinstaller.InstallFailed;
import com.android.packageinstaller.PackageUtil;
import java.io.File;

public class InstallFailed extends Activity {
    private static final String LOG_TAG = InstallFailed.class.getSimpleName();
    private CharSequence mLabel;

    private int getExplanationFromErrorCode(int i) {
        Log.d(LOG_TAG, "Installation status code: " + i);
        if (i == 2) {
            return R.string.install_failed_blocked;
        }
        if (i == 7) {
            return R.string.install_failed_incompatible;
        }
        switch (i) {
            case 4:
                return R.string.install_failed_invalid_apk;
            case 5:
                return R.string.install_failed_conflict;
            default:
                return R.string.install_failed;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        PackageUtil.AppSnippet appSnippet;
        super.onCreate(bundle);
        int intExtra = getIntent().getIntExtra("android.content.pm.extra.STATUS", 1);
        if (getIntent().getBooleanExtra("android.intent.extra.RETURN_RESULT", false)) {
            int intExtra2 = getIntent().getIntExtra("android.content.pm.extra.LEGACY_STATUS", -110);
            Intent intent = new Intent();
            intent.putExtra("android.intent.extra.INSTALL_RESULT", intExtra2);
            setResult(1, intent);
            finish();
            return;
        }
        Intent intent2 = getIntent();
        ApplicationInfo applicationInfo = (ApplicationInfo) intent2.getParcelableExtra("com.android.packageinstaller.applicationInfo");
        Uri data = intent2.getData();
        setContentView(R.layout.install_failed);
        PackageManager packageManager = getPackageManager();
        if ("package".equals(data.getScheme())) {
            appSnippet = new PackageUtil.AppSnippet(packageManager.getApplicationLabel(applicationInfo), packageManager.getApplicationIcon(applicationInfo));
        } else {
            appSnippet = PackageUtil.getAppSnippet(this, applicationInfo, new File(data.getPath()));
        }
        this.mLabel = appSnippet.label;
        PackageUtil.initSnippetForNewApp(this, appSnippet, R.id.app_snippet);
        if (intExtra == 6) {
            new OutOfSpaceDialog().show(getFragmentManager(), "outofspace");
        }
        ((TextView) findViewById(R.id.simple_status)).setText(getExplanationFromErrorCode(intExtra));
        findViewById(R.id.done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.finish();
            }
        });
    }

    public static class OutOfSpaceDialog extends DialogFragment {
        private InstallFailed mActivity;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            this.mActivity = (InstallFailed) context;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(this.mActivity).setTitle(R.string.out_of_space_dlg_title).setMessage(getString(R.string.out_of_space_dlg_text, new Object[]{this.mActivity.mLabel})).setPositiveButton(R.string.manage_applications, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    InstallFailed.OutOfSpaceDialog.lambda$onCreateDialog$0(this.f$0, dialogInterface, i);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.mActivity.finish();
                }
            }).create();
        }

        public static void lambda$onCreateDialog$0(OutOfSpaceDialog outOfSpaceDialog, DialogInterface dialogInterface, int i) {
            if (BenesseExtension.getDchaState() == 0) {
                outOfSpaceDialog.startActivity(new Intent("android.intent.action.MANAGE_PACKAGE_STORAGE"));
            }
            outOfSpaceDialog.mActivity.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            super.onCancel(dialogInterface);
            this.mActivity.finish();
        }
    }
}
