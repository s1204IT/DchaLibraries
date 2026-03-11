package com.android.settings.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.util.Log;
import com.android.settings.ButtonBarHandler;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.view.NavigationBar;

public class WifiSetupActivity extends WifiPickerActivity implements ButtonBarHandler, NavigationBar.NavigationBarListener {
    private boolean mAutoFinishOnConnection;
    private boolean mIsNetworkRequired;
    private boolean mIsWifiRequired;
    private NavigationBar mNavigationBar;
    private boolean mUserSelectedNetwork;
    private boolean mWifiConnected;
    private IntentFilter mFilter = new IntentFilter();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiSetupActivity.this.refreshConnectionState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mAutoFinishOnConnection = intent.getBooleanExtra("wifi_auto_finish_on_connect", false);
        this.mIsNetworkRequired = intent.getBooleanExtra("is_network_required", false);
        this.mIsWifiRequired = intent.getBooleanExtra("is_wifi_required", false);
        this.mUserSelectedNetwork = intent.getBooleanExtra("wifi_require_user_network_selection", false) ? false : true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("userSelectedNetwork", this.mUserSelectedNetwork);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mUserSelectedNetwork = savedInstanceState.getBoolean("userSelectedNetwork", true);
    }

    private boolean isWifiConnected() {
        boolean wifiConnected;
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService("connectivity");
        if (connectivity == null) {
            wifiConnected = false;
        } else {
            wifiConnected = connectivity.getNetworkInfo(1).isConnected();
        }
        this.mWifiConnected = wifiConnected;
        return wifiConnected;
    }

    public void refreshConnectionState() {
        if (isWifiConnected()) {
            if (this.mAutoFinishOnConnection && this.mUserSelectedNetwork) {
                Log.d("WifiSetupActivity", "Auto-finishing with connection");
                finish(-1);
                this.mUserSelectedNetwork = false;
            }
            setNextButtonText(R.string.suw_next_button_label);
            setNextButtonEnabled(true);
            return;
        }
        if (this.mIsWifiRequired || (this.mIsNetworkRequired && !isNetworkConnected())) {
            setNextButtonText(R.string.skip_label);
            setNextButtonEnabled(false);
        } else {
            setNextButtonText(R.string.skip_label);
            setNextButtonEnabled(true);
        }
    }

    private void setNextButtonEnabled(boolean enabled) {
        if (this.mNavigationBar == null) {
            return;
        }
        this.mNavigationBar.getNextButton().setEnabled(enabled);
    }

    private void setNextButtonText(int resId) {
        if (this.mNavigationBar == null) {
            return;
        }
        this.mNavigationBar.getNextButton().setText(resId);
    }

    void networkSelected() {
        Log.d("WifiSetupActivity", "Network selected by user");
        this.mUserSelectedNetwork = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(this.mReceiver, this.mFilter);
        refreshConnectionState();
    }

    @Override
    public void onPause() {
        unregisterReceiver(this.mReceiver);
        super.onPause();
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        int resid2 = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid2, first);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return WifiSettingsForSetupWizard.class.getName().equals(fragmentName);
    }

    @Override
    Class<? extends PreferenceFragment> getWifiSettingsClass() {
        return WifiSettingsForSetupWizard.class;
    }

    public void finish(int resultCode) {
        Log.d("WifiSetupActivity", "finishing, resultCode=" + resultCode);
        setResult(resultCode);
        finish();
    }

    public void onNavigationBarCreated(NavigationBar bar) {
        this.mNavigationBar = bar;
        bar.setNavigationBarListener(this);
        SetupWizardUtils.setImmersiveMode(this);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        if (this.mWifiConnected) {
            finish(-1);
        } else {
            int message = isNetworkConnected() ? R.string.wifi_skipped_message : R.string.wifi_and_mobile_skipped_message;
            WifiSkipDialog.newInstance(message).show(getFragmentManager(), "dialog");
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo info;
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService("connectivity");
        if (connectivity == null || (info = connectivity.getActiveNetworkInfo()) == null) {
            return false;
        }
        return info.isConnected();
    }

    public static class WifiSkipDialog extends DialogFragment {
        public static WifiSkipDialog newInstance(int messageRes) {
            Bundle args = new Bundle();
            args.putInt("messageRes", messageRes);
            WifiSkipDialog dialog = new WifiSkipDialog();
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageRes = getArguments().getInt("messageRes");
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setMessage(messageRes).setCancelable(false).setPositiveButton(R.string.wifi_skip_anyway, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int id) {
                    WifiSetupActivity activity = (WifiSetupActivity) WifiSkipDialog.this.getActivity();
                    activity.finish(1);
                }
            }).setNegativeButton(R.string.wifi_dont_skip, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int id) {
                }
            }).create();
            SetupWizardUtils.applyImmersiveFlags(dialog);
            return dialog;
        }
    }
}
