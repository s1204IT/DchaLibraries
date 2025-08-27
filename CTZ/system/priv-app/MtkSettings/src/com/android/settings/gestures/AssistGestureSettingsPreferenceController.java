package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

/* loaded from: classes.dex */
public class AssistGestureSettingsPreferenceController extends GesturePreferenceController {
    private static final int OFF = 0;
    private static final int ON = 1;
    private static final String PREF_KEY_VIDEO = "gesture_assist_video";
    private static final String SECURE_KEY_ASSIST = "assist_gesture_enabled";
    private static final String SECURE_KEY_SILENCE = "assist_gesture_silence_alerts_enabled";
    private final String mAssistGesturePrefKey;

    @VisibleForTesting
    boolean mAssistOnly;
    private final AssistGestureFeatureProvider mFeatureProvider;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private boolean mWasAvailable;

    public AssistGestureSettingsPreferenceController(Context context, String str) {
        super(context, str);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
        this.mWasAvailable = isAvailable();
        this.mAssistGesturePrefKey = str;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return this.mAssistOnly ? this.mFeatureProvider.isSupported(this.mContext) : this.mFeatureProvider.isSensorAvailable(this.mContext) ? 0 : 2;
    }

    @Override // com.android.settings.gestures.GesturePreferenceController, com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mScreen = preferenceScreen;
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        super.displayPreference(preferenceScreen);
    }

    @Override // com.android.settings.gestures.GesturePreferenceController, com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mWasAvailable != isAvailable()) {
            updatePreference();
            this.mWasAvailable = isAvailable();
        }
    }

    public AssistGestureSettingsPreferenceController setAssistOnly(boolean z) {
        this.mAssistOnly = z;
        return this;
    }

    private void updatePreference() {
        if (this.mPreference == null) {
            return;
        }
        if (isAvailable()) {
            if (this.mScreen.findPreference(getPreferenceKey()) == null) {
                this.mScreen.addPreference(this.mPreference);
                return;
            }
            return;
        }
        this.mScreen.removePreference(this.mPreference);
    }

    private boolean isAssistGestureEnabled() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), SECURE_KEY_ASSIST, 1) != 0;
    }

    private boolean isSilenceGestureEnabled() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), SECURE_KEY_SILENCE, 1) != 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return Settings.Secure.putInt(this.mContext.getContentResolver(), SECURE_KEY_ASSIST, z ? 1 : 0);
    }

    @Override // com.android.settings.gestures.GesturePreferenceController
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override // com.android.settings.gestures.GesturePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        boolean z = isAssistGestureEnabled() && this.mFeatureProvider.isSupported(this.mContext);
        if (!this.mAssistOnly) {
            z = z || isSilenceGestureEnabled();
        }
        return this.mContext.getText(z ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), SECURE_KEY_ASSIST, 0) == 1;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload(SECURE_KEY_ASSIST, 2, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, AssistGestureSettings.class.getName(), this.mAssistGesturePrefKey, this.mContext.getString(R.string.display_settings)), isAvailable(), 1);
    }
}
