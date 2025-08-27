package com.android.settings.display;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wrapper.OverlayManagerWrapper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* loaded from: classes.dex */
public class ThemePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final OverlayManagerWrapper mOverlayService;
    private final PackageManager mPackageManager;

    public ThemePreferenceController(Context context) {
        this(context, ServiceManager.getService("overlay") != null ? new OverlayManagerWrapper() : null);
    }

    ThemePreferenceController(Context context, OverlayManagerWrapper overlayManagerWrapper) {
        super(context);
        this.mOverlayService = overlayManagerWrapper;
        this.mPackageManager = context.getPackageManager();
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "theme";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("theme".equals(preference.getKey())) {
            this.mMetricsFeatureProvider.action(this.mContext, 816, new Pair[0]);
        }
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ListPreference listPreference = (ListPreference) preference;
        String[] availableThemes = getAvailableThemes();
        CharSequence[] charSequenceArr = new CharSequence[availableThemes.length];
        int i = 0;
        for (int i2 = 0; i2 < availableThemes.length; i2++) {
            try {
                charSequenceArr[i2] = this.mPackageManager.getApplicationInfo(availableThemes[i2], 0).loadLabel(this.mPackageManager);
            } catch (PackageManager.NameNotFoundException e) {
                charSequenceArr[i2] = availableThemes[i2];
            }
        }
        listPreference.setEntries(charSequenceArr);
        listPreference.setEntryValues(availableThemes);
        String currentTheme = getCurrentTheme();
        CharSequence string = null;
        while (true) {
            if (i >= availableThemes.length) {
                break;
            }
            if (!TextUtils.equals(availableThemes[i], currentTheme)) {
                i++;
            } else {
                string = charSequenceArr[i];
                break;
            }
        }
        if (TextUtils.isEmpty(string)) {
            string = this.mContext.getString(R.string.default_theme);
        }
        listPreference.setSummary(string);
        listPreference.setValue(currentTheme);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (Objects.equals(obj, getTheme())) {
            return true;
        }
        this.mOverlayService.setEnabledExclusiveInCategory((String) obj, UserHandle.myUserId());
        return true;
    }

    private boolean isTheme(OverlayManagerWrapper.OverlayInfo overlayInfo) throws PackageManager.NameNotFoundException {
        if (!"android.theme".equals(overlayInfo.category)) {
            return false;
        }
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(overlayInfo.packageName, 0);
            if (packageInfo != null) {
                return !packageInfo.isStaticOverlayPackage();
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getTheme() {
        List<OverlayManagerWrapper.OverlayInfo> overlayInfosForTarget = this.mOverlayService.getOverlayInfosForTarget("android", UserHandle.myUserId());
        int size = overlayInfosForTarget.size();
        for (int i = 0; i < size; i++) {
            if (overlayInfosForTarget.get(i).isEnabled() && isTheme(overlayInfosForTarget.get(i))) {
                return overlayInfosForTarget.get(i).packageName;
            }
        }
        return null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        String[] availableThemes;
        return (this.mOverlayService == null || (availableThemes = getAvailableThemes()) == null || availableThemes.length <= 1) ? false : true;
    }

    String getCurrentTheme() {
        return getTheme();
    }

    String[] getAvailableThemes() {
        List<OverlayManagerWrapper.OverlayInfo> overlayInfosForTarget = this.mOverlayService.getOverlayInfosForTarget("android", UserHandle.myUserId());
        ArrayList arrayList = new ArrayList(overlayInfosForTarget.size());
        int size = overlayInfosForTarget.size();
        for (int i = 0; i < size; i++) {
            if (isTheme(overlayInfosForTarget.get(i))) {
                arrayList.add(overlayInfosForTarget.get(i).packageName);
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }
}
