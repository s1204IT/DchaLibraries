package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.InputDevice;
import com.android.settings.SettingsPreferenceFragment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyboardLayoutPickerFragment extends SettingsPreferenceFragment implements InputManager.InputDeviceListener {
    private InputManager mIm;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private KeyboardLayout[] mKeyboardLayouts;
    private int mInputDeviceId = -1;
    private HashMap<CheckBoxPreference, KeyboardLayout> mPreferenceMap = new HashMap<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mInputDeviceIdentifier = getActivity().getIntent().getParcelableExtra("input_device_identifier");
        if (this.mInputDeviceIdentifier == null) {
            getActivity().finish();
        }
        this.mIm = (InputManager) getSystemService("input");
        this.mKeyboardLayouts = this.mIm.getKeyboardLayouts();
        Arrays.sort(this.mKeyboardLayouts);
        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIm.registerInputDeviceListener(this, null);
        InputDevice inputDevice = this.mIm.getInputDeviceByDescriptor(this.mInputDeviceIdentifier.getDescriptor());
        if (inputDevice == null) {
            getActivity().finish();
        } else {
            this.mInputDeviceId = inputDevice.getId();
            updateCheckedState();
        }
    }

    @Override
    public void onPause() {
        this.mIm.unregisterInputDeviceListener(this);
        this.mInputDeviceId = -1;
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        CheckBoxPreference checkboxPref;
        KeyboardLayout layout;
        if ((preference instanceof CheckBoxPreference) && (layout = this.mPreferenceMap.get((checkboxPref = (CheckBoxPreference) preference))) != null) {
            boolean checked = checkboxPref.isChecked();
            if (checked) {
                this.mIm.addKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier, layout.getDescriptor());
            } else {
                this.mIm.removeKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier, layout.getDescriptor());
            }
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        if (this.mInputDeviceId >= 0 && deviceId == this.mInputDeviceId) {
            updateCheckedState();
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (this.mInputDeviceId >= 0 && deviceId == this.mInputDeviceId) {
            getActivity().finish();
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        Context context = getActivity();
        KeyboardLayout[] arr$ = this.mKeyboardLayouts;
        for (KeyboardLayout layout : arr$) {
            CheckBoxPreference pref = new CheckBoxPreference(context);
            pref.setTitle(layout.getLabel());
            pref.setSummary(layout.getCollection());
            root.addPreference(pref);
            this.mPreferenceMap.put(pref, layout);
        }
        return root;
    }

    private void updateCheckedState() {
        String[] enabledKeyboardLayouts = this.mIm.getKeyboardLayoutsForInputDevice(this.mInputDeviceIdentifier);
        Arrays.sort(enabledKeyboardLayouts);
        for (Map.Entry<CheckBoxPreference, KeyboardLayout> entry : this.mPreferenceMap.entrySet()) {
            entry.getKey().setChecked(Arrays.binarySearch(enabledKeyboardLayouts, entry.getValue().getDescriptor()) >= 0);
        }
    }
}
