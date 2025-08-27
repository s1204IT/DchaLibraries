package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.webview.WebViewUpdateServiceWrapper;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public class WebViewAppPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    private final PackageManagerWrapper mPackageManager;
    private final WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    public WebViewAppPreferenceController(Context context) {
        super(context);
        this.mPackageManager = new PackageManagerWrapper(context.getPackageManager());
        this.mWebViewUpdateServiceWrapper = new WebViewUpdateServiceWrapper();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "select_webview_provider";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        CharSequence defaultAppLabel = getDefaultAppLabel();
        if (!TextUtils.isEmpty(defaultAppLabel)) {
            this.mPreference.setSummary(defaultAppLabel);
        } else {
            Log.d("WebViewAppPrefCtrl", "No default app");
            this.mPreference.setSummary(R.string.app_list_preference_none);
        }
    }

    DefaultAppInfo getDefaultAppInfo() {
        PackageInfo currentWebViewPackage = this.mWebViewUpdateServiceWrapper.getCurrentWebViewPackage();
        return new DefaultAppInfo(this.mContext, this.mPackageManager, currentWebViewPackage == null ? null : currentWebViewPackage.applicationInfo);
    }

    private CharSequence getDefaultAppLabel() {
        return getDefaultAppInfo().loadLabel();
    }
}
