package com.android.settings.webview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.webkit.UserPackage;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class WebViewAppPicker extends DefaultAppPickerFragment {
    private WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    private WebViewUpdateServiceWrapper getWebViewUpdateServiceWrapper() {
        if (this.mWebViewUpdateServiceWrapper == null) {
            setWebViewUpdateServiceWrapper(createDefaultWebViewUpdateServiceWrapper());
        }
        return this.mWebViewUpdateServiceWrapper;
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPickerFragment, com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!this.mUserManager.isAdminUser()) {
            getActivity().finish();
        }
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.webview_app_settings;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<DefaultAppInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        Context context = getContext();
        WebViewUpdateServiceWrapper webViewUpdateServiceWrapper = getWebViewUpdateServiceWrapper();
        for (ApplicationInfo applicationInfo : webViewUpdateServiceWrapper.getValidWebViewApplicationInfos(context)) {
            arrayList.add(createDefaultAppInfo(context, this.mPm, applicationInfo, getDisabledReason(webViewUpdateServiceWrapper, context, applicationInfo.packageName)));
        }
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        PackageInfo currentWebViewPackage = getWebViewUpdateServiceWrapper().getCurrentWebViewPackage();
        if (currentWebViewPackage == null) {
            return null;
        }
        return currentWebViewPackage.packageName;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        return getWebViewUpdateServiceWrapper().setWebViewProvider(str);
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected void onSelectionPerformed(boolean z) {
        if (z) {
            Activity activity = getActivity();
            Intent intent = activity == null ? null : activity.getIntent();
            if (intent != null && "android.settings.WEBVIEW_SETTINGS".equals(intent.getAction())) {
                getActivity().finish();
                return;
            }
            return;
        }
        getWebViewUpdateServiceWrapper().showInvalidChoiceToast(getActivity());
        updateCandidates();
    }

    private WebViewUpdateServiceWrapper createDefaultWebViewUpdateServiceWrapper() {
        return new WebViewUpdateServiceWrapper();
    }

    void setWebViewUpdateServiceWrapper(WebViewUpdateServiceWrapper webViewUpdateServiceWrapper) {
        this.mWebViewUpdateServiceWrapper = webViewUpdateServiceWrapper;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 405;
    }

    private static class WebViewAppInfo extends DefaultAppInfo {
        public WebViewAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, PackageItemInfo packageItemInfo, String str, boolean z) {
            super(context, packageManagerWrapper, packageItemInfo, str, z);
        }

        @Override // com.android.settingslib.applications.DefaultAppInfo, com.android.settingslib.widget.CandidateInfo
        public CharSequence loadLabel() {
            String str = "";
            try {
                str = this.mPm.getPackageManager().getPackageInfo(this.packageItemInfo.packageName, 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
            }
            return String.format("%s %s", super.loadLabel(), str);
        }
    }

    DefaultAppInfo createDefaultAppInfo(Context context, PackageManagerWrapper packageManagerWrapper, PackageItemInfo packageItemInfo, String str) {
        return new WebViewAppInfo(context, packageManagerWrapper, packageItemInfo, str, TextUtils.isEmpty(str));
    }

    String getDisabledReason(WebViewUpdateServiceWrapper webViewUpdateServiceWrapper, Context context, String str) {
        for (UserPackage userPackage : webViewUpdateServiceWrapper.getPackageInfosAllUsers(context, str)) {
            if (!userPackage.isInstalledPackage()) {
                return context.getString(R.string.webview_uninstalled_for_user, userPackage.getUserInfo().name);
            }
            if (!userPackage.isEnabledPackage()) {
                return context.getString(R.string.webview_disabled_for_user, userPackage.getUserInfo().name);
            }
        }
        return null;
    }
}
