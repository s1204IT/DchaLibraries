package com.android.settings.inputmethod;

import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.InputDevice;
import com.android.settings.SettingsPreferenceFragment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class KeyboardLayoutPickerFragment extends SettingsPreferenceFragment implements InputManager.InputDeviceListener {
    private InputManager mIm;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private KeyboardLayout[] mKeyboardLayouts;
    private int mInputDeviceId = -1;
    private HashMap<CheckBoxPreference, KeyboardLayout> mPreferenceMap = new HashMap<>();

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 58;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mInputDeviceIdentifier = getActivity().getIntent().getParcelableExtra("input_device_identifier");
        if (this.mInputDeviceIdentifier == null) {
            getActivity().finish();
        }
        this.mIm = (InputManager) getSystemService("input");
        this.mKeyboardLayouts = this.mIm.getKeyboardLayoutsForInputDevice(this.mInputDeviceIdentifier);
        Arrays.sort(this.mKeyboardLayouts);
        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mIm.registerInputDeviceListener(this, null);
        InputDevice inputDeviceByDescriptor = this.mIm.getInputDeviceByDescriptor(this.mInputDeviceIdentifier.getDescriptor());
        if (inputDeviceByDescriptor == null) {
            getActivity().finish();
        } else {
            this.mInputDeviceId = inputDeviceByDescriptor.getId();
            updateCheckedState();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        this.mIm.unregisterInputDeviceListener(this);
        this.mInputDeviceId = -1;
        super.onPause();
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        CheckBoxPreference checkBoxPreference;
        KeyboardLayout keyboardLayout;
        if ((preference instanceof CheckBoxPreference) && (keyboardLayout = this.mPreferenceMap.get((checkBoxPreference = (CheckBoxPreference) preference))) != null) {
            if (checkBoxPreference.isChecked()) {
                this.mIm.addKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier, keyboardLayout.getDescriptor());
                return true;
            }
            this.mIm.removeKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier, keyboardLayout.getDescriptor());
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceAdded(int i) {
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceChanged(int i) {
        if (this.mInputDeviceId >= 0 && i == this.mInputDeviceId) {
            updateCheckedState();
        }
    }

    @Override // android.hardware.input.InputManager.InputDeviceListener
    public void onInputDeviceRemoved(int i) {
        if (this.mInputDeviceId >= 0 && i == this.mInputDeviceId) {
            getActivity().finish();
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        for (KeyboardLayout keyboardLayout : this.mKeyboardLayouts) {
            CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getPrefContext());
            checkBoxPreference.setTitle(keyboardLayout.getLabel());
            checkBoxPreference.setSummary(keyboardLayout.getCollection());
            preferenceScreenCreatePreferenceScreen.addPreference(checkBoxPreference);
            this.mPreferenceMap.put(checkBoxPreference, keyboardLayout);
        }
        return preferenceScreenCreatePreferenceScreen;
    }

    private void updateCheckedState() {
        String[] enabledKeyboardLayoutsForInputDevice = this.mIm.getEnabledKeyboardLayoutsForInputDevice(this.mInputDeviceIdentifier);
        Arrays.sort(enabledKeyboardLayoutsForInputDevice);
        for (Map.Entry<CheckBoxPreference, KeyboardLayout> entry : this.mPreferenceMap.entrySet()) {
            entry.getKey().setChecked(Arrays.binarySearch(enabledKeyboardLayoutsForInputDevice, entry.getValue().getDescriptor()) >= 0);
        }
    }
}
