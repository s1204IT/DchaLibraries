package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.wrapper.OverlayManagerWrapper;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import java.util.List;

public class EmulateDisplayCutoutPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final boolean mAvailable;
    private final OverlayManagerWrapper mOverlayManager;
    private PackageManager mPackageManager;
    private ListPreference mPreference;

    EmulateDisplayCutoutPreferenceController(Context context, PackageManager packageManager, OverlayManagerWrapper overlayManagerWrapper) {
        super(context);
        this.mOverlayManager = overlayManagerWrapper;
        this.mPackageManager = packageManager;
        this.mAvailable = overlayManagerWrapper != null && getOverlayInfos().length > 0;
    }

    public EmulateDisplayCutoutPreferenceController(Context context) {
        this(context, context.getPackageManager(), new OverlayManagerWrapper());
    }

    @Override
    public boolean isAvailable() {
        return this.mAvailable;
    }

    @Override
    public String getPreferenceKey() {
        return "display_cutout_emulation";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        setPreference((ListPreference) preferenceScreen.findPreference(getPreferenceKey()));
    }

    void setPreference(ListPreference listPreference) {
        this.mPreference = listPreference;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return setEmulationOverlay((String) obj);
    }

    private boolean setEmulationOverlay(String str) {
        boolean enabledExclusiveInCategory;
        String str2 = null;
        for (OverlayManagerWrapper.OverlayInfo overlayInfo : getOverlayInfos()) {
            if (overlayInfo.isEnabled()) {
                str2 = overlayInfo.packageName;
            }
        }
        if ((TextUtils.isEmpty(str) && TextUtils.isEmpty(str2)) || TextUtils.equals(str, str2)) {
            return true;
        }
        if (TextUtils.isEmpty(str)) {
            enabledExclusiveInCategory = this.mOverlayManager.setEnabled(str2, false, 0);
        } else {
            enabledExclusiveInCategory = this.mOverlayManager.setEnabledExclusiveInCategory(str, 0);
        }
        updateState(this.mPreference);
        return enabledExclusiveInCategory;
    }

    @Override
    public void updateState(Preference preference) {
        OverlayManagerWrapper.OverlayInfo[] overlayInfos = getOverlayInfos();
        CharSequence[] charSequenceArr = new CharSequence[overlayInfos.length + 1];
        CharSequence[] charSequenceArr2 = new CharSequence[charSequenceArr.length];
        charSequenceArr[0] = "";
        charSequenceArr2[0] = this.mContext.getString(R.string.display_cutout_emulation_none);
        int i = 0;
        int i2 = 0;
        while (i < overlayInfos.length) {
            OverlayManagerWrapper.OverlayInfo overlayInfo = overlayInfos[i];
            i++;
            charSequenceArr[i] = overlayInfo.packageName;
            if (overlayInfo.isEnabled()) {
                i2 = i;
            }
        }
        for (int i3 = 1; i3 < charSequenceArr.length; i3++) {
            try {
                charSequenceArr2[i3] = this.mPackageManager.getApplicationInfo(charSequenceArr[i3].toString(), 0).loadLabel(this.mPackageManager);
            } catch (PackageManager.NameNotFoundException e) {
                charSequenceArr2[i3] = charSequenceArr[i3];
            }
        }
        this.mPreference.setEntries(charSequenceArr2);
        this.mPreference.setEntryValues(charSequenceArr);
        this.mPreference.setValueIndex(i2);
        this.mPreference.setSummary(charSequenceArr2[i2]);
    }

    private OverlayManagerWrapper.OverlayInfo[] getOverlayInfos() {
        List<OverlayManagerWrapper.OverlayInfo> overlayInfosForTarget = this.mOverlayManager.getOverlayInfosForTarget("android", 0);
        for (int size = overlayInfosForTarget.size() - 1; size >= 0; size--) {
            if (!"com.android.internal.display_cutout_emulation".equals(overlayInfosForTarget.get(size).category)) {
                overlayInfosForTarget.remove(size);
            }
        }
        return (OverlayManagerWrapper.OverlayInfo[]) overlayInfosForTarget.toArray(new OverlayManagerWrapper.OverlayInfo[overlayInfosForTarget.size()]);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        setEmulationOverlay("");
        updateState(this.mPreference);
    }
}
