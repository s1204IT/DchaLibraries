package com.android.settings.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class WifiScanModeActivity extends Activity {
    private String mApp;
    private DialogFragment mDialog;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (bundle == null) {
            if (intent != null && intent.getAction().equals("android.net.wifi.action.REQUEST_SCAN_ALWAYS_AVAILABLE")) {
                this.mApp = getCallingPackage();
                try {
                    PackageManager packageManager = getPackageManager();
                    this.mApp = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(this.mApp, 0));
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                finish();
                return;
            }
        } else {
            this.mApp = bundle.getString("app");
        }
        createDialog();
    }

    private void createDialog() {
        if (this.mDialog == null) {
            this.mDialog = AlertDialogFragment.newInstance(this.mApp);
            this.mDialog.show(getFragmentManager(), "dialog");
        }
    }

    private void dismissDialog() {
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
    }

    private void doPositiveClick() {
        Settings.Global.putInt(getContentResolver(), "wifi_scan_always_enabled", 1);
        setResult(-1);
        finish();
    }

    private void doNegativeClick() {
        setResult(0);
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("app", this.mApp);
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissDialog();
    }

    @Override
    public void onResume() {
        super.onResume();
        createDialog();
    }

    public static class AlertDialogFragment extends InstrumentedDialogFragment {
        private final String mApp;

        static AlertDialogFragment newInstance(String str) {
            return new AlertDialogFragment(str);
        }

        public AlertDialogFragment(String str) {
            this.mApp = str;
        }

        public AlertDialogFragment() {
            this.mApp = null;
        }

        @Override
        public int getMetricsCategory() {
            return 543;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.wifi_scan_always_turnon_message, new Object[]{this.mApp})).setPositiveButton(R.string.wifi_scan_always_confirm_allow, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ((WifiScanModeActivity) AlertDialogFragment.this.getActivity()).doPositiveClick();
                }
            }).setNegativeButton(R.string.wifi_scan_always_confirm_deny, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ((WifiScanModeActivity) AlertDialogFragment.this.getActivity()).doNegativeClick();
                }
            }).create();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            ((WifiScanModeActivity) getActivity()).doNegativeClick();
        }
    }
}
