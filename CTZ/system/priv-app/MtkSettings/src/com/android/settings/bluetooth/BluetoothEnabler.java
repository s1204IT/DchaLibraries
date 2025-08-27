package com.android.settings.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.wifi.AccessPoint;

/* loaded from: classes.dex */
public final class BluetoothEnabler implements SwitchWidgetController.OnSwitchChangeListener {
    private SwitchWidgetController.OnSwitchChangeListener mCallback;
    private Context mContext;
    private final IntentFilter mIntentFilter;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final int mMetricsEvent;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final RestrictionUtils mRestrictionUtils;
    private final SwitchWidgetController mSwitchController;
    private boolean mValidListener;
    private boolean mUpdateStatusOnly = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.settings.bluetooth.BluetoothEnabler.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", AccessPoint.UNREACHABLE_RSSI);
            Log.d("BluetoothEnabler", "BluetoothAdapter state changed to" + intExtra);
            BluetoothEnabler.this.handleStateChanged(intExtra);
        }
    };

    public BluetoothEnabler(Context context, SwitchWidgetController switchWidgetController, MetricsFeatureProvider metricsFeatureProvider, LocalBluetoothManager localBluetoothManager, int i, RestrictionUtils restrictionUtils) {
        this.mContext = context;
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        this.mSwitchController = switchWidgetController;
        this.mSwitchController.setListener(this);
        this.mValidListener = false;
        this.mMetricsEvent = i;
        if (localBluetoothManager == null) {
            this.mLocalAdapter = null;
            this.mSwitchController.setEnabled(false);
        } else {
            this.mLocalAdapter = localBluetoothManager.getBluetoothAdapter();
        }
        this.mIntentFilter = new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED");
        this.mRestrictionUtils = restrictionUtils;
    }

    public void resume(Context context) {
        if (this.mContext != context) {
            this.mContext = context;
        }
        boolean zMaybeEnforceRestrictions = maybeEnforceRestrictions();
        if (this.mLocalAdapter == null) {
            this.mSwitchController.setEnabled(false);
            return;
        }
        if (!zMaybeEnforceRestrictions) {
            handleStateChanged(this.mLocalAdapter.getBluetoothState());
        }
        this.mSwitchController.startListening();
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        this.mValidListener = true;
    }

    public void pause() {
        if (this.mLocalAdapter != null && this.mValidListener) {
            this.mSwitchController.stopListening();
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mValidListener = false;
        }
    }

    void handleStateChanged(int i) {
        switch (i) {
            case AccessPoint.Speed.MODERATE /* 10 */:
                this.mUpdateStatusOnly = true;
                Log.d("BluetoothEnabler", "Begin update status: turn off set mUpdateStatusOnly to true");
                setChecked(false);
                this.mSwitchController.setEnabled(true);
                this.mUpdateStatusOnly = false;
                Log.d("BluetoothEnabler", "End update status: turn off set mUpdateStatusOnly to false");
                break;
            case 11:
                this.mSwitchController.setEnabled(false);
                break;
            case 12:
                this.mUpdateStatusOnly = true;
                Log.d("BluetoothEnabler", "Begin update status: turn on set mUpdateStatusOnly to true");
                setChecked(true);
                this.mSwitchController.setEnabled(true);
                this.mUpdateStatusOnly = false;
                Log.d("BluetoothEnabler", "End update status: turn on set mUpdateStatusOnly to false");
                break;
            case 13:
                this.mSwitchController.setEnabled(false);
                break;
            default:
                setChecked(false);
                this.mSwitchController.setEnabled(true);
                break;
        }
    }

    private void setChecked(boolean z) {
        if (z != this.mSwitchController.isChecked()) {
            if (this.mValidListener) {
                this.mSwitchController.stopListening();
            }
            this.mSwitchController.setChecked(z);
            if (this.mValidListener) {
                this.mSwitchController.startListening();
            }
        }
    }

    @Override // com.android.settings.widget.SwitchWidgetController.OnSwitchChangeListener
    public boolean onSwitchToggled(boolean z) {
        if (maybeEnforceRestrictions()) {
            triggerParentPreferenceCallback(z);
            return true;
        }
        Log.d("BluetoothEnabler", "onSwitchChanged to " + z);
        if (z && !WirelessUtils.isRadioAllowed(this.mContext, "bluetooth")) {
            Toast.makeText(this.mContext, R.string.wifi_in_airplane_mode, 0).show();
            this.mSwitchController.setChecked(false);
            triggerParentPreferenceCallback(false);
            return false;
        }
        this.mMetricsFeatureProvider.action(this.mContext, this.mMetricsEvent, z);
        Log.d("BluetoothEnabler", "mUpdateStatusOnly is " + this.mUpdateStatusOnly);
        if (this.mLocalAdapter != null && !this.mUpdateStatusOnly) {
            boolean bluetoothEnabled = this.mLocalAdapter.setBluetoothEnabled(z);
            if (z && !bluetoothEnabled) {
                this.mSwitchController.setChecked(false);
                this.mSwitchController.setEnabled(true);
                this.mSwitchController.updateTitle(false);
                triggerParentPreferenceCallback(false);
                return false;
            }
        }
        this.mSwitchController.setEnabled(false);
        triggerParentPreferenceCallback(z);
        return true;
    }

    public void setToggleCallback(SwitchWidgetController.OnSwitchChangeListener onSwitchChangeListener) {
        this.mCallback = onSwitchChangeListener;
    }

    @VisibleForTesting
    boolean maybeEnforceRestrictions() {
        RestrictedLockUtils.EnforcedAdmin enforcedAdmin = getEnforcedAdmin(this.mRestrictionUtils, this.mContext);
        this.mSwitchController.setDisabledByAdmin(enforcedAdmin);
        if (enforcedAdmin != null) {
            this.mSwitchController.setChecked(false);
            this.mSwitchController.setEnabled(false);
        }
        return enforcedAdmin != null;
    }

    public static RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(RestrictionUtils restrictionUtils, Context context) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = restrictionUtils.checkIfRestrictionEnforced(context, "no_bluetooth");
        if (enforcedAdminCheckIfRestrictionEnforced == null) {
            return restrictionUtils.checkIfRestrictionEnforced(context, "no_config_bluetooth");
        }
        return enforcedAdminCheckIfRestrictionEnforced;
    }

    private void triggerParentPreferenceCallback(boolean z) {
        if (this.mCallback != null) {
            this.mCallback.onSwitchToggled(z);
        }
    }
}
