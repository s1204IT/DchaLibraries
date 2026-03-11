package com.android.settings.development.featureflags;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.FooterPreferenceMixin;

public class FeatureFlagFooterPreferenceController extends BasePreferenceController implements LifecycleObserver, OnStart {
    private FooterPreferenceMixin mFooterMixin;

    public FeatureFlagFooterPreferenceController(Context context) {
        super(context, "feature_flag_footer_pref");
    }

    public void setFooterMixin(FooterPreferenceMixin footerPreferenceMixin) {
        this.mFooterMixin = footerPreferenceMixin;
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public void onStart() {
        this.mFooterMixin.createFooterPreference().setTitle(R.string.experimental_category_title);
    }
}
