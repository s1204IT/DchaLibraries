package com.android.settings.applications.appinfo;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.overlay.FeatureFactory;

public class PictureInPictureDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener {
    private SwitchPreference mSwitchPref;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.picture_in_picture_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mSwitchPref.setTitle(R.string.picture_in_picture_app_detail_switch);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mSwitchPref) {
            Boolean bool = (Boolean) obj;
            logSpecialPermissionChange(bool.booleanValue(), this.mPackageName);
            setEnterPipStateForPackage(getActivity(), this.mPackageInfo.applicationInfo.uid, this.mPackageName, bool.booleanValue());
            return true;
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        this.mSwitchPref.setChecked(getEnterPipStateForPackage(getActivity(), this.mPackageInfo.applicationInfo.uid, this.mPackageName));
        return true;
    }

    @Override
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return 812;
    }

    static void setEnterPipStateForPackage(Context context, int i, String str, boolean z) {
        ((AppOpsManager) context.getSystemService(AppOpsManager.class)).setMode(67, i, str, z ? 0 : 2);
    }

    static boolean getEnterPipStateForPackage(Context context, int i, String str) {
        return ((AppOpsManager) context.getSystemService(AppOpsManager.class)).checkOpNoThrow(67, i, str) == 0;
    }

    public static int getPreferenceSummary(Context context, int i, String str) {
        return getEnterPipStateForPackage(context, i, str) ? R.string.app_permission_summary_allowed : R.string.app_permission_summary_not_allowed;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean z, String str) {
        int i;
        if (z) {
            i = 813;
        } else {
            i = 814;
        }
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), i, str, new Pair[0]);
    }
}
