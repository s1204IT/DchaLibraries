package com.android.settings.inputmethod;

import android.app.Activity;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.util.Preconditions;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class KeyboardLayoutPickerFragment2 extends SettingsPreferenceFragment implements InputManager.InputDeviceListener {
    private InputManager mIm;
    private InputMethodInfo mImi;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private KeyboardLayout[] mKeyboardLayouts;
    private InputMethodSubtype mSubtype;
    private int mInputDeviceId = -1;
    private Map<Preference, KeyboardLayout> mPreferenceMap = new HashMap();

    @Override
    protected int getMetricsCategory() {
        return 58;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Activity activity = (Activity) Preconditions.checkNotNull(getActivity());
        this.mInputDeviceIdentifier = activity.getIntent().getParcelableExtra("input_device_identifier");
        this.mImi = (InputMethodInfo) activity.getIntent().getParcelableExtra("input_method_info");
        this.mSubtype = (InputMethodSubtype) activity.getIntent().getParcelableExtra("input_method_subtype");
        if (this.mInputDeviceIdentifier == null || this.mImi == null) {
            activity.finish();
        }
        this.mIm = (InputManager) activity.getSystemService(InputManager.class);
        this.mKeyboardLayouts = this.mIm.getKeyboardLayoutsForInputDevice(this.mInputDeviceIdentifier);
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
        }
    }

    @Override
    public void onPause() {
        this.mIm.unregisterInputDeviceListener(this);
        this.mInputDeviceId = -1;
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        KeyboardLayout layout = this.mPreferenceMap.get(preference);
        if (layout != null) {
            this.mIm.setKeyboardLayoutForInputDevice(this.mInputDeviceIdentifier, this.mImi, this.mSubtype, layout.getDescriptor());
            getActivity().finish();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (this.mInputDeviceId < 0 || deviceId != this.mInputDeviceId) {
            return;
        }
        getActivity().finish();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        for (KeyboardLayout layout : this.mKeyboardLayouts) {
            Preference pref = new Preference(getPrefContext());
            pref.setTitle(layout.getLabel());
            pref.setSummary(layout.getCollection());
            root.addPreference(pref);
            this.mPreferenceMap.put(pref, layout);
        }
        root.setTitle(PhysicalKeyboardFragment.KeyboardInfoPreference.getDisplayName(getContext(), this.mImi, this.mSubtype));
        return root;
    }
}
