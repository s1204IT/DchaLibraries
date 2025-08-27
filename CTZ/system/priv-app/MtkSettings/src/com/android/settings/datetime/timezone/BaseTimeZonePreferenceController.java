package com.android.settings.datetime.timezone;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.BasePreferenceController;
import com.google.common.base.Objects;

/* loaded from: classes.dex */
public abstract class BaseTimeZonePreferenceController extends BasePreferenceController {
    private OnPreferenceClickListener mOnClickListener;

    protected BaseTimeZonePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (this.mOnClickListener == null || !Objects.equal(getPreferenceKey(), preference.getKey())) {
            return false;
        }
        this.mOnClickListener.onClick();
        return true;
    }

    public void setOnClickListener(OnPreferenceClickListener onPreferenceClickListener) {
        this.mOnClickListener = onPreferenceClickListener;
    }
}
