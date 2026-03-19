package com.android.settings.security;

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
import com.android.settings.CryptKeeperConfirm;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.password.ChooseLockSettingsHelper;

public class CryptKeeperSettings extends InstrumentedPreferenceFragment {
    private View mBatteryWarning;
    private View mContentView;
    private Button mInitiateButton;
    private IntentFilter mIntentFilter;
    private View mPowerWarning;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.BATTERY_CHANGED")) {
                int intExtra = intent.getIntExtra("level", 0);
                int intExtra2 = intent.getIntExtra("plugged", 0);
                int intExtra3 = intent.getIntExtra("invalid_charger", 0);
                boolean z = intExtra >= 80;
                boolean z2 = (intExtra2 & 7) != 0 && intExtra3 == 0;
                CryptKeeperSettings.this.mInitiateButton.setEnabled(z && z2);
                CryptKeeperSettings.this.mPowerWarning.setVisibility(z2 ? 8 : 0);
                CryptKeeperSettings.this.mBatteryWarning.setVisibility(z ? 8 : 0);
            }
        }
    };
    private View.OnClickListener mInitiateListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!CryptKeeperSettings.this.runKeyguardConfirmation(55)) {
                new AlertDialog.Builder(CryptKeeperSettings.this.getActivity()).setTitle(R.string.crypt_keeper_dialog_need_password_title).setMessage(R.string.crypt_keeper_dialog_need_password_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create().show();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mContentView = layoutInflater.inflate(R.layout.crypt_keeper_settings, (ViewGroup) null);
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
    public int getMetricsCategory() {
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
    public void onActivityCreated(Bundle bundle) {
        DevicePolicyManager devicePolicyManager;
        super.onActivityCreated(bundle);
        Activity activity = getActivity();
        if ("android.app.action.START_ENCRYPTION".equals(activity.getIntent().getAction()) && (devicePolicyManager = (DevicePolicyManager) activity.getSystemService("device_policy")) != null && devicePolicyManager.getStorageEncryptionStatus() != 1) {
            activity.finish();
        }
        activity.setTitle(R.string.crypt_keeper_encrypt_title);
    }

    private boolean runKeyguardConfirmation(int i) {
        Resources resources = getActivity().getResources();
        ChooseLockSettingsHelper chooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity(), this);
        if (chooseLockSettingsHelper.utils().getKeyguardStoredPasswordQuality(UserHandle.myUserId()) == 0) {
            showFinalConfirmation(1, "");
            return true;
        }
        return chooseLockSettingsHelper.launchConfirmationActivity(i, resources.getText(R.string.crypt_keeper_encrypt_title), true);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 55 && i2 == -1 && intent != null) {
            int intExtra = intent.getIntExtra("type", -1);
            String stringExtra = intent.getStringExtra("password");
            if (!TextUtils.isEmpty(stringExtra)) {
                showFinalConfirmation(intExtra, stringExtra);
            }
        }
    }

    private void showFinalConfirmation(int i, String str) {
        Preference preference = new Preference(getPreferenceManager().getContext());
        preference.setFragment(CryptKeeperConfirm.class.getName());
        preference.setTitle(R.string.crypt_keeper_confirm_title);
        addEncryptionInfoToPreference(preference, i, str);
        ((SettingsActivity) getActivity()).onPreferenceStartFragment(null, preference);
    }

    private void addEncryptionInfoToPreference(Preference preference, int i, String str) {
        if (((DevicePolicyManager) getActivity().getSystemService("device_policy")).getDoNotAskCredentialsOnBoot()) {
            preference.getExtras().putInt("type", 1);
            preference.getExtras().putString("password", "");
        } else {
            preference.getExtras().putInt("type", i);
            preference.getExtras().putString("password", str);
        }
    }
}
