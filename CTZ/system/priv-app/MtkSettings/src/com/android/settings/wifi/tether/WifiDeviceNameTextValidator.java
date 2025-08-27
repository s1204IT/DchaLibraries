package com.android.settings.wifi.tether;

import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settings.wifi.WifiUtils;

/* loaded from: classes.dex */
public class WifiDeviceNameTextValidator implements ValidatedEditTextPreference.Validator {
    @Override // com.android.settings.widget.ValidatedEditTextPreference.Validator
    public boolean isTextValid(String str) {
        return (WifiUtils.isSSIDTooLong(str) || WifiUtils.isSSIDTooShort(str)) ? false : true;
    }
}
