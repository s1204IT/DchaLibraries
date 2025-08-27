package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.RadioButtonPreference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class UsbDetailsFunctionsController extends UsbDetailsController implements RadioButtonPreference.OnClickListener {
    static final Map<Long, Integer> FUNCTIONS_MAP = new LinkedHashMap();
    private PreferenceCategory mProfilesContainer;

    static {
        FUNCTIONS_MAP.put(4L, Integer.valueOf(R.string.usb_use_file_transfers));
        FUNCTIONS_MAP.put(32L, Integer.valueOf(R.string.usb_use_tethering));
        FUNCTIONS_MAP.put(8L, Integer.valueOf(R.string.usb_use_MIDI));
        FUNCTIONS_MAP.put(16L, Integer.valueOf(R.string.usb_use_photo_transfers));
        FUNCTIONS_MAP.put(0L, Integer.valueOf(R.string.usb_use_charging_only));
    }

    public UsbDetailsFunctionsController(Context context, UsbDetailsFragment usbDetailsFragment, UsbBackend usbBackend) {
        super(context, usbDetailsFragment, usbBackend);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mProfilesContainer = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
    }

    private RadioButtonPreference getProfilePreference(String str, int i) {
        RadioButtonPreference radioButtonPreference = (RadioButtonPreference) this.mProfilesContainer.findPreference(str);
        if (radioButtonPreference == null) {
            RadioButtonPreference radioButtonPreference2 = new RadioButtonPreference(this.mProfilesContainer.getContext());
            radioButtonPreference2.setKey(str);
            radioButtonPreference2.setTitle(i);
            radioButtonPreference2.setOnClickListener(this);
            this.mProfilesContainer.addPreference(radioButtonPreference2);
            return radioButtonPreference2;
        }
        return radioButtonPreference;
    }

    @Override // com.android.settings.connecteddevice.usb.UsbDetailsController
    protected void refresh(boolean z, long j, int i, int i2) {
        if (!z || i2 != 2) {
            this.mProfilesContainer.setEnabled(false);
        } else {
            this.mProfilesContainer.setEnabled(true);
        }
        Iterator<Long> it = FUNCTIONS_MAP.keySet().iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            RadioButtonPreference profilePreference = getProfilePreference(UsbBackend.usbFunctionsToString(jLongValue), FUNCTIONS_MAP.get(Long.valueOf(jLongValue)).intValue());
            if (this.mUsbBackend.areFunctionsSupported(jLongValue)) {
                profilePreference.setChecked(j == jLongValue);
            } else {
                this.mProfilesContainer.removePreference(profilePreference);
            }
        }
    }

    @Override // com.android.settings.widget.RadioButtonPreference.OnClickListener
    public void onRadioButtonClicked(RadioButtonPreference radioButtonPreference) {
        long jUsbFunctionsFromString = UsbBackend.usbFunctionsFromString(radioButtonPreference.getKey());
        if (jUsbFunctionsFromString != this.mUsbBackend.getCurrentFunctions() && !Utils.isMonkeyRunning()) {
            this.mUsbBackend.setCurrentFunctions(jUsbFunctionsFromString);
        }
    }

    @Override // com.android.settings.connecteddevice.usb.UsbDetailsController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !Utils.isMonkeyRunning();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "usb_details_functions";
    }
}
