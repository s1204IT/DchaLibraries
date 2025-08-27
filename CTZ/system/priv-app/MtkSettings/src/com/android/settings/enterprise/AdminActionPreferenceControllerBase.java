package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.format.DateUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.Date;

/* loaded from: classes.dex */
public abstract class AdminActionPreferenceControllerBase extends AbstractPreferenceController implements PreferenceControllerMixin {
    protected final EnterprisePrivacyFeatureProvider mFeatureProvider;

    protected abstract Date getAdminActionTimestamp();

    public AdminActionPreferenceControllerBase(Context context) {
        super(context);
        this.mFeatureProvider = FeatureFactory.getFactory(context).getEnterprisePrivacyFeatureProvider(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        String dateTime;
        Date adminActionTimestamp = getAdminActionTimestamp();
        if (adminActionTimestamp == null) {
            dateTime = this.mContext.getString(R.string.enterprise_privacy_none);
        } else {
            dateTime = DateUtils.formatDateTime(this.mContext, adminActionTimestamp.getTime(), 17);
        }
        preference.setSummary(dateTime);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }
}
