package com.android.settings.notification;

import android.content.Context;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.RestrictedPreference;

/* loaded from: classes.dex */
public abstract class AdjustVolumeRestrictedPreferenceController extends SliderPreferenceController implements PreferenceControllerMixin {
    private AccountRestrictionHelper mHelper;

    public AdjustVolumeRestrictedPreferenceController(Context context, String str) {
        this(context, new AccountRestrictionHelper(context), str);
    }

    @VisibleForTesting
    AdjustVolumeRestrictedPreferenceController(Context context, AccountRestrictionHelper accountRestrictionHelper, String str) {
        super(context, str);
        this.mHelper = accountRestrictionHelper;
    }

    @Override // com.android.settings.core.SliderPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        this.mHelper.enforceRestrictionOnPreference((RestrictedPreference) preference, "no_adjust_volume", UserHandle.myUserId());
    }

    @Override // com.android.settings.core.BasePreferenceController
    public IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        intentFilter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
        intentFilter.addAction("android.media.MASTER_MUTE_CHANGED_ACTION");
        return intentFilter;
    }
}
