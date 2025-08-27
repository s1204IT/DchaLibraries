package com.android.settings.display;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

/* loaded from: classes.dex */
public class AmbientDisplayAlwaysOnPreferenceController extends TogglePreferenceController {
    private static final int MY_USER = UserHandle.myUserId();
    private final int OFF;
    private final int ON;
    private OnPreferenceChangedCallback mCallback;
    private AmbientDisplayConfiguration mConfig;

    public interface OnPreferenceChangedCallback {
        void onPreferenceChanged();
    }

    public AmbientDisplayAlwaysOnPreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mConfig == null) {
            this.mConfig = new AmbientDisplayConfiguration(this.mContext);
        }
        return isAvailable(this.mConfig) ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "ambient_display_always_on");
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return this.mConfig.alwaysOnEnabled(MY_USER);
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_always_on", z ? 1 : 0);
        if (this.mCallback != null) {
            this.mCallback.onPreferenceChanged();
            return true;
        }
        return true;
    }

    public AmbientDisplayAlwaysOnPreferenceController setConfig(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        this.mConfig = ambientDisplayConfiguration;
        return this;
    }

    public AmbientDisplayAlwaysOnPreferenceController setCallback(OnPreferenceChangedCallback onPreferenceChangedCallback) {
        this.mCallback = onPreferenceChangedCallback;
        return this;
    }

    public static boolean isAlwaysOnEnabled(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        return ambientDisplayConfiguration.alwaysOnEnabled(MY_USER);
    }

    public static boolean isAvailable(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        return ambientDisplayConfiguration.alwaysOnAvailableForUser(MY_USER);
    }

    public static boolean accessibilityInversionEnabled(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        return ambientDisplayConfiguration.accessibilityInversionEnabled(MY_USER);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("doze_always_on", 2, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, AmbientDisplaySettings.class.getName(), getPreferenceKey(), this.mContext.getString(R.string.ambient_display_screen_title)), isAvailable(), 1);
    }
}
