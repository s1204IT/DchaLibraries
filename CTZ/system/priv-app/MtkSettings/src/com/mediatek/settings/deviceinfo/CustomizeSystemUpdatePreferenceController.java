package com.mediatek.settings.deviceinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;
import java.util.List;

/* loaded from: classes.dex */
public class CustomizeSystemUpdatePreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final UserManager mUm;

    public CustomizeSystemUpdatePreferenceController(Context context, UserManager userManager) {
        super(context);
        this.mUm = userManager;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mUm.isAdminUser() && isCustomizedSystemUpdateAvalible();
    }

    public static boolean isCustomizedSystemUpdateAvalible() {
        return FeatureOption.MTK_SYSTEM_UPDATE_SUPPORT;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "mtk_system_update";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        if (isAvailable()) {
            updatePreferenceToSpecificActivityOrRemove(this.mContext, preferenceScreen, getPreferenceKey());
        } else {
            setVisible(preferenceScreen, getPreferenceKey(), false);
        }
    }

    private void updatePreferenceToSpecificActivityOrRemove(Context context, PreferenceScreen preferenceScreen, String str) {
        Intent intent;
        Preference preferenceFindPreference = preferenceScreen.findPreference(str);
        if (preferenceFindPreference == null) {
            return;
        }
        if (FeatureOption.MTK_SYSTEM_UPDATE_SUPPORT) {
            intent = new Intent("android.intent.action.MAIN", (Uri) null);
            intent.setClassName("com.mediatek.systemupdate", "com.mediatek.systemupdate.MainEntry");
        } else {
            intent = null;
        }
        if (intent != null) {
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 0);
            int size = listQueryIntentActivities.size();
            for (int i = 0; i < size; i++) {
                ResolveInfo resolveInfo = listQueryIntentActivities.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    preferenceFindPreference.setIntent(new Intent().setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
                    CharSequence charSequenceLoadLabel = resolveInfo.loadLabel(packageManager);
                    preferenceFindPreference.setTitle(charSequenceLoadLabel);
                    Log.d("CustSysUpdatePrefContr", "KEY_MTK_SYSTEM_UPDATE : " + ((Object) charSequenceLoadLabel));
                    return;
                }
            }
        }
        preferenceScreen.removePreference(preferenceFindPreference);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("mtk_system_update".equals(preference.getKey())) {
            systemUpdateEntrance(preference);
            return true;
        }
        return false;
    }

    private void systemUpdateEntrance(Preference preference) {
        if (FeatureOption.MTK_SYSTEM_UPDATE_SUPPORT) {
            startActivity("com.mediatek.systemupdate", "com.mediatek.systemupdate.MainEntry");
        }
    }

    private void startActivity(String str, String str2) {
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        intent.setComponent(new ComponentName(str, str2));
        if (this.mContext.getPackageManager().resolveActivity(intent, 0) != null) {
            this.mContext.startActivity(intent);
            return;
        }
        Log.d("CustSysUpdatePrefContr", "Unable to start activity " + intent.toString());
    }
}
