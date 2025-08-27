package com.android.settings.notification;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.service.notification.ZenModeConfig;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeBehaviorFooterPreferenceController extends AbstractZenModePreferenceController {
    private final int mTitleRes;

    public ZenModeBehaviorFooterPreferenceController(Context context, Lifecycle lifecycle, int i) {
        super(context, "footer_preference", lifecycle);
        this.mTitleRes = i;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "footer_preference";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setTitle(getFooterText());
    }

    protected String getFooterText() {
        ComponentName componentName;
        if (isDeprecatedZenMode(getZenMode())) {
            ZenModeConfig zenModeConfig = getZenModeConfig();
            if (zenModeConfig.manualRule != null && isDeprecatedZenMode(zenModeConfig.manualRule.zenMode)) {
                Uri uri = zenModeConfig.manualRule.conditionId;
                if (zenModeConfig.manualRule.enabler != null) {
                    String ownerCaption = mZenModeConfigWrapper.getOwnerCaption(zenModeConfig.manualRule.enabler);
                    if (!ownerCaption.isEmpty()) {
                        return this.mContext.getString(R.string.zen_mode_app_set_behavior, ownerCaption);
                    }
                } else {
                    return this.mContext.getString(R.string.zen_mode_qs_set_behavior);
                }
            }
            for (ZenModeConfig.ZenRule zenRule : zenModeConfig.automaticRules.values()) {
                if (zenRule.isAutomaticActive() && isDeprecatedZenMode(zenRule.zenMode) && (componentName = zenRule.component) != null) {
                    return this.mContext.getString(R.string.zen_mode_app_set_behavior, componentName.getPackageName());
                }
            }
            return this.mContext.getString(R.string.zen_mode_unknown_app_set_behavior);
        }
        return this.mContext.getString(this.mTitleRes);
    }

    private boolean isDeprecatedZenMode(int i) {
        switch (i) {
            case 2:
            case 3:
                return true;
            default:
                return false;
        }
    }
}
