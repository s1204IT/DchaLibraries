package com.android.settings.display;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public class AmbientDisplayNotificationsPreferenceController extends TogglePreferenceController implements Preference.OnPreferenceChangeListener {
    static final String KEY_AMBIENT_DISPLAY_NOTIFICATIONS = "ambient_display_notification";
    private static final int MY_USER = UserHandle.myUserId();
    private final int OFF;
    private final int ON;
    private AmbientDisplayConfiguration mConfig;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AmbientDisplayNotificationsPreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    public AmbientDisplayNotificationsPreferenceController setConfig(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        this.mConfig = ambientDisplayConfiguration;
        return this;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_DISPLAY_NOTIFICATIONS.equals(preference.getKey())) {
            this.mMetricsFeatureProvider.action(this.mContext, 495, new Pair[0]);
        }
        return false;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return this.mConfig.pulseOnNotificationEnabled(MY_USER);
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_enabled", z ? 1 : 0);
        return true;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mConfig == null) {
            this.mConfig = new AmbientDisplayConfiguration(this.mContext);
        }
        return this.mConfig.pulseOnNotificationAvailable() ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_AMBIENT_DISPLAY_NOTIFICATIONS);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("doze_enabled", 2, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, AmbientDisplaySettings.class.getName(), KEY_AMBIENT_DISPLAY_NOTIFICATIONS, this.mContext.getString(R.string.ambient_display_screen_title)), isAvailable(), 1);
    }
}
