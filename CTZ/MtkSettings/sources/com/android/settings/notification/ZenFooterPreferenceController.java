package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenFooterPreferenceController extends AbstractZenModePreferenceController {
    public ZenFooterPreferenceController(Context context, Lifecycle lifecycle, String str) {
        super(context, str, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return this.mBackend.mPolicy.suppressedVisualEffects == 0 || NotificationManager.Policy.areAllVisualEffectsSuppressed(this.mBackend.mPolicy.suppressedVisualEffects);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (this.mBackend.mPolicy.suppressedVisualEffects == 0) {
            preference.setTitle(R.string.zen_mode_restrict_notifications_mute_footer);
        } else if (NotificationManager.Policy.areAllVisualEffectsSuppressed(this.mBackend.mPolicy.suppressedVisualEffects)) {
            preference.setTitle(R.string.zen_mode_restrict_notifications_hide_footer);
        } else {
            preference.setTitle((CharSequence) null);
        }
    }
}
