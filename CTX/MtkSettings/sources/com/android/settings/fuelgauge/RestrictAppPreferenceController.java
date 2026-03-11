package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipUtils;
import java.util.List;

public class RestrictAppPreferenceController extends BasePreferenceController {
    static final String KEY_RESTRICT_APP = "restricted_app";
    List<AppInfo> mAppInfos;
    private AppOpsManager mAppOpsManager;
    private InstrumentedPreferenceFragment mPreferenceFragment;
    private UserManager mUserManager;

    public RestrictAppPreferenceController(Context context) {
        super(context, KEY_RESTRICT_APP);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
    }

    public RestrictAppPreferenceController(InstrumentedPreferenceFragment instrumentedPreferenceFragment) {
        this(instrumentedPreferenceFragment.getContext());
        this.mPreferenceFragment = instrumentedPreferenceFragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mAppInfos = BatteryTipUtils.getRestrictedAppsList(this.mAppOpsManager, this.mUserManager);
        int size = this.mAppInfos.size();
        preference.setVisible(size > 0);
        preference.setSummary(this.mContext.getResources().getQuantityString(R.plurals.restricted_app_summary, size, Integer.valueOf(size)));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            RestrictedAppDetails.startRestrictedAppDetails(this.mPreferenceFragment, this.mAppInfos);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
