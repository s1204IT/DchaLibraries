package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class CryptKeeperSettings extends InstrumentedFragment {
    private View mBatteryWarning;
    private View mContentView;
    private Button mInitiateButton;
    private IntentFilter mIntentFilter;
    private View mPowerWarning;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.intent.action.BATTERY_CHANGED")) {
                return;
            }
            int level = intent.getIntExtra("level", 0);
            int plugged = intent.getIntExtra("plugged", 0);
            int invalidCharger = intent.getIntExtra("invalid_charger", 0);
            boolean levelOk = level >= 80;
            boolean pluggedOk = (plugged & 7) != 0 && invalidCharger == 0;
            CryptKeeperSettings.this.mInitiateButton.setEnabled(levelOk ? pluggedOk : false);
            CryptKeeperSettings.this.mPowerWarning.setVisibility(pluggedOk ? 8 : 0);
            CryptKeeperSettings.this.mBatteryWarning.setVisibility(levelOk ? 8 : 0);
        }
    };
    private View.OnClickListener mInitiateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (CryptKeeperSettings.this.runKeyguardConfirmation(55)) {
                return;
            }
            new AlertDialog.Builder(CryptKeeperSettings.this.getActivity()).setTitle(R.string.crypt_keeper_dialog_need_password_title).setMessage(R.string.crypt_keeper_dialog_need_password_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create().show();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        this.mContentView = inflater.inflate(R.layout.crypt_keeper_settings, (ViewGroup) null);
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mInitiateButton = (Button) this.mContentView.findViewById(R.id.initiate_encrypt);
        this.mInitiateButton.setOnClickListener(this.mInitiateListener);
        this.mInitiateButton.setEnabled(false);
        this.mPowerWarning = this.mContentView.findViewById(R.id.warning_unplugged);
        this.mBatteryWarning = this.mContentView.findViewById(R.id.warning_low_charge);
        return this.mContentView;
    }

    @Override
    protected int getMetricsCategory() {
        return 32;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(this.mIntentReceiver, this.mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mIntentReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        DevicePolicyManager dpm;
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        if (!"android.app.action.START_ENCRYPTION".equals(intent.getAction()) || (dpm = (DevicePolicyManager) activity.getSystemService("device_policy")) == null) {
            return;
        }
        int status = dpm.getStorageEncryptionStatus();
        if (status == 1) {
            return;
        }
        activity.finish();
    }

    public boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
        if (helper.utils().getKeyguardStoredPasswordQuality(UserHandle.myUserId()) == 0) {
            showFinalConfirmation(1, "");
            return true;
        }
        return helper.launchConfirmationActivity(request, res.getText(R.string.crypt_keeper_encrypt_title), true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 55 || resultCode != -1 || data == null) {
            return;
        }
        int type = data.getIntExtra("type", -1);
        String password = data.getStringExtra("password");
        if (TextUtils.isEmpty(password)) {
            return;
        }
        showFinalConfirmation(type, password);
    }

    private void showFinalConfirmation(int type, String password) {
        Preference preference = new Preference(getPreferenceManager().getContext());
        preference.setFragment(CryptKeeperConfirm.class.getName());
        preference.setTitle(R.string.crypt_keeper_confirm_title);
        addEncryptionInfoToPreference(preference, type, password);
        ((SettingsActivity) getActivity()).onPreferenceStartFragment(null, preference);
    }

    private void addEncryptionInfoToPreference(Preference preference, int type, String password) {
        Activity activity = getActivity();
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService("device_policy");
        if (dpm.getDoNotAskCredentialsOnBoot()) {
            preference.getExtras().putInt("type", 1);
            preference.getExtras().putString("password", "");
        } else {
            preference.getExtras().putInt("type", type);
            preference.getExtras().putString("password", password);
        }
    }
}
