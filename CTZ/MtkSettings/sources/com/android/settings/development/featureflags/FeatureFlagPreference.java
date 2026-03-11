package com.android.settings.development.featureflags;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.FeatureFlagUtils;

public class FeatureFlagPreference extends SwitchPreference {
    private final String mKey;

    public FeatureFlagPreference(Context context, String str) {
        super(context);
        this.mKey = str;
        setKey(str);
        setTitle(str);
        setCheckedInternal(FeatureFlagUtils.isEnabled(context, this.mKey));
    }

    @Override
    public void setChecked(boolean z) {
        setCheckedInternal(z);
        FeatureFlagUtils.setEnabled(getContext(), this.mKey, z);
    }

    private void setCheckedInternal(boolean z) {
        super.setChecked(z);
        setSummary(Boolean.toString(z));
    }
}
