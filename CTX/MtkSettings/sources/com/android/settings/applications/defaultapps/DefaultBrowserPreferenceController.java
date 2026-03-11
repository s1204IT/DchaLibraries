package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.List;

public class DefaultBrowserPreferenceController extends DefaultAppPreferenceController {
    static final Intent BROWSE_PROBE = new Intent().setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse("http:"));

    public DefaultBrowserPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        List<ResolveInfo> candidates = getCandidates();
        return (candidates == null || candidates.isEmpty()) ? false : true;
    }

    @Override
    public String getPreferenceKey() {
        return "default_browser";
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        CharSequence defaultAppLabel = getDefaultAppLabel();
        if (!TextUtils.isEmpty(defaultAppLabel)) {
            preference.setSummary(defaultAppLabel);
        }
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        try {
            String defaultBrowserPackageNameAsUser = this.mPackageManager.getDefaultBrowserPackageNameAsUser(this.mUserId);
            Log.d("BrowserPrefCtrl", "Get default browser package: " + defaultBrowserPackageNameAsUser);
            return new DefaultAppInfo(this.mContext, this.mPackageManager, this.mPackageManager.getPackageManager().getApplicationInfo(defaultBrowserPackageNameAsUser, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public CharSequence getDefaultAppLabel() {
        if (!isAvailable()) {
            return null;
        }
        DefaultAppInfo defaultAppInfo = getDefaultAppInfo();
        CharSequence charSequenceLoadLabel = defaultAppInfo != null ? defaultAppInfo.loadLabel() : null;
        if (!TextUtils.isEmpty(charSequenceLoadLabel)) {
            return charSequenceLoadLabel;
        }
        return getOnlyAppLabel();
    }

    @Override
    public Drawable getDefaultAppIcon() {
        if (!isAvailable()) {
            return null;
        }
        DefaultAppInfo defaultAppInfo = getDefaultAppInfo();
        if (defaultAppInfo != null) {
            return defaultAppInfo.loadIcon();
        }
        return getOnlyAppIcon();
    }

    private List<ResolveInfo> getCandidates() {
        return this.mPackageManager.queryIntentActivitiesAsUser(BROWSE_PROBE, 131072, this.mUserId);
    }

    private String getOnlyAppLabel() {
        List<ResolveInfo> candidates = getCandidates();
        if (candidates == null || candidates.size() != 1) {
            return null;
        }
        ResolveInfo resolveInfo = candidates.get(0);
        String string = resolveInfo.loadLabel(this.mPackageManager.getPackageManager()).toString();
        ComponentInfo componentInfo = resolveInfo.getComponentInfo();
        Log.d("BrowserPrefCtrl", "Getting label for the only browser app: " + (componentInfo != null ? componentInfo.packageName : null) + string);
        return string;
    }

    private Drawable getOnlyAppIcon() {
        String str;
        List<ResolveInfo> candidates = getCandidates();
        if (candidates == null || candidates.size() != 1) {
            return null;
        }
        ComponentInfo componentInfo = candidates.get(0).getComponentInfo();
        if (componentInfo != null) {
            str = componentInfo.packageName;
        } else {
            str = null;
        }
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getPackageManager().getApplicationInfo(str, 0);
            Log.d("BrowserPrefCtrl", "Getting icon for the only browser app: " + str);
            return IconDrawableFactory.newInstance(this.mContext).getBadgedIcon(componentInfo, applicationInfo, this.mUserId);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("BrowserPrefCtrl", "Error getting app info for " + str);
            return null;
        }
    }

    public static boolean hasBrowserPreference(String str, Context context) {
        Intent intent = new Intent(BROWSE_PROBE);
        intent.setPackage(str);
        List<ResolveInfo> listQueryIntentActivities = context.getPackageManager().queryIntentActivities(intent, 0);
        return (listQueryIntentActivities == null || listQueryIntentActivities.size() == 0) ? false : true;
    }

    public boolean isBrowserDefault(String str, int i) {
        String defaultBrowserPackageNameAsUser = this.mPackageManager.getDefaultBrowserPackageNameAsUser(i);
        if (defaultBrowserPackageNameAsUser != null) {
            return defaultBrowserPackageNameAsUser.equals(str);
        }
        List<ResolveInfo> listQueryIntentActivitiesAsUser = this.mPackageManager.queryIntentActivitiesAsUser(BROWSE_PROBE, 131072, i);
        return listQueryIntentActivitiesAsUser != null && listQueryIntentActivitiesAsUser.size() == 1;
    }
}
