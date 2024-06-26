package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.InputDevice;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
/* loaded from: classes.dex */
public class GameControllerPreferenceController extends TogglePreferenceController implements InputManager.InputDeviceListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private final InputManager mIm;
    private Preference mPreference;

    public GameControllerPreferenceController(Context context, String str) {
        super(context, str);
        this.mIm = (InputManager) context.getSystemService("input");
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        this.mIm.registerInputDeviceListener(this, null);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mIm.unregisterInputDeviceListener(this);
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (!this.mContext.getResources().getBoolean(R.bool.config_show_vibrate_input_devices)) {
            return 2;
        }
        for (int i : this.mIm.getInputDeviceIds()) {
            InputDevice inputDevice = this.mIm.getInputDevice(i);
            if (inputDevice != null && !inputDevice.isVirtual() && inputDevice.getVibrator().hasVibrator()) {
                return 0;
            }
        }
        return 1;
    }

    @Override // com.android.settings.core.TogglePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        this.mPreference.setVisible(isAvailable());
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_input_devices", 1) > 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return Settings.System.putInt(this.mContext.getContentResolver(), "vibrate_input_devices", z ? 1 : 0);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int i) {
        updateState(this.mPreference);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int i) {
        updateState(this.mPreference);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int i) {
        updateState(this.mPreference);
    }
}
