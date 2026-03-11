package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.overlay.FeatureFactory;

public class AppSettingPreferenceController extends AppInfoPreferenceControllerBase {
    private String mPackageName;

    public AppSettingPreferenceController(Context context, String str) {
        super(context, str);
    }

    public AppSettingPreferenceController setPackageName(String str) {
        this.mPackageName = str;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return (TextUtils.isEmpty(this.mPackageName) || this.mParent == null || resolveIntent(new Intent("android.intent.action.APPLICATION_PREFERENCES").setPackage(this.mPackageName)) == null) ? 1 : 0;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        Intent intentResolveIntent;
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey()) || (intentResolveIntent = resolveIntent(new Intent("android.intent.action.APPLICATION_PREFERENCES").setPackage(this.mPackageName))) == null) {
            return false;
        }
        FeatureFactory.getFactory(this.mContext).getMetricsFeatureProvider().actionWithSource(this.mContext, this.mParent.getMetricsCategory(), 1017);
        this.mContext.startActivity(intentResolveIntent);
        return true;
    }

    private Intent resolveIntent(Intent intent) {
        ResolveInfo resolveInfoResolveActivity = this.mContext.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfoResolveActivity != null) {
            return new Intent(intent.getAction()).setClassName(resolveInfoResolveActivity.activityInfo.packageName, resolveInfoResolveActivity.activityInfo.name);
        }
        return null;
    }
}
