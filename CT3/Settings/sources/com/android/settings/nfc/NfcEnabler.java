package com.android.settings.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class NfcEnabler implements Preference.OnPreferenceChangeListener {
    private final RestrictedPreference mAndroidBeam;
    private boolean mBeamDisallowedBySystem;
    private final Context mContext;
    private final IntentFilter mIntentFilter;
    private final NfcAdapter mNfcAdapter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.nfc.action.ADAPTER_STATE_CHANGED".equals(action)) {
                return;
            }
            NfcEnabler.this.handleNfcStateChanged(intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1));
        }
    };
    private final SwitchPreference mSwitch;

    public NfcEnabler(Context context, SwitchPreference switchPreference, RestrictedPreference androidBeam) {
        this.mContext = context;
        this.mSwitch = switchPreference;
        this.mAndroidBeam = androidBeam;
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        this.mBeamDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(context, "no_outgoing_beam", UserHandle.myUserId());
        if (this.mNfcAdapter == null) {
            this.mSwitch.setEnabled(false);
            this.mAndroidBeam.setEnabled(false);
            this.mIntentFilter = null;
        } else {
            if (this.mBeamDisallowedBySystem) {
                this.mAndroidBeam.setEnabled(false);
            }
            this.mIntentFilter = new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED");
        }
    }

    public void resume() {
        if (this.mNfcAdapter == null) {
            return;
        }
        handleNfcStateChanged(this.mNfcAdapter.getAdapterState());
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        this.mSwitch.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        if (this.mNfcAdapter == null) {
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
        this.mSwitch.setOnPreferenceChangeListener(null);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean desiredState = ((Boolean) value).booleanValue();
        this.mSwitch.setEnabled(false);
        if (desiredState) {
            this.mNfcAdapter.enable();
        } else {
            this.mNfcAdapter.disable();
        }
        return false;
    }

    public void handleNfcStateChanged(int newState) {
        switch (newState) {
            case DefaultWfcSettingsExt.PAUSE:
                this.mSwitch.setChecked(false);
                this.mSwitch.setEnabled(true);
                this.mAndroidBeam.setEnabled(false);
                this.mAndroidBeam.setSummary(R.string.android_beam_disabled_summary);
                break;
            case DefaultWfcSettingsExt.CREATE:
                this.mSwitch.setChecked(true);
                this.mSwitch.setEnabled(false);
                this.mAndroidBeam.setEnabled(false);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                this.mSwitch.setChecked(true);
                this.mSwitch.setEnabled(true);
                if (this.mBeamDisallowedBySystem) {
                    this.mAndroidBeam.setDisabledByAdmin(null);
                    this.mAndroidBeam.setEnabled(false);
                } else {
                    this.mAndroidBeam.checkRestrictionAndSetDisabled("no_outgoing_beam");
                }
                if (this.mNfcAdapter.isNdefPushEnabled() && this.mAndroidBeam.isEnabled()) {
                    this.mAndroidBeam.setSummary(R.string.android_beam_on_summary);
                } else {
                    this.mAndroidBeam.setSummary(R.string.android_beam_off_summary);
                }
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                this.mSwitch.setChecked(false);
                this.mSwitch.setEnabled(false);
                this.mAndroidBeam.setEnabled(false);
                break;
        }
    }
}
