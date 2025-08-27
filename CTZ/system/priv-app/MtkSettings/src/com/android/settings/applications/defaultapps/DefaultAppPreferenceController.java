package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public abstract class DefaultAppPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    protected final PackageManagerWrapper mPackageManager;
    protected int mUserId;
    protected final UserManager mUserManager;

    protected abstract DefaultAppInfo getDefaultAppInfo();

    public DefaultAppPreferenceController(Context context) {
        super(context);
        this.mPackageManager = new PackageManagerWrapper(context.getPackageManager());
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mUserId = UserHandle.myUserId();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        DefaultAppInfo defaultAppInfo = getDefaultAppInfo();
        CharSequence defaultAppLabel = getDefaultAppLabel();
        if (preference instanceof TwoTargetPreference) {
            ((TwoTargetPreference) preference).setIconSize(1);
        }
        if (!TextUtils.isEmpty(defaultAppLabel)) {
            preference.setSummary(defaultAppLabel);
            Utils.setSafeIcon(preference, getDefaultAppIcon());
        } else {
            Log.d("DefaultAppPrefControl", "No default app");
            preference.setSummary(R.string.app_list_preference_none);
            preference.setIcon((Drawable) null);
        }
        mayUpdateGearIcon(defaultAppInfo, preference);
    }

    private void mayUpdateGearIcon(DefaultAppInfo defaultAppInfo, Preference preference) {
        if (!(preference instanceof GearPreference)) {
            return;
        }
        final Intent settingIntent = getSettingIntent(defaultAppInfo);
        if (settingIntent != null) {
            ((GearPreference) preference).setOnGearClickListener(new GearPreference.OnGearClickListener() { // from class: com.android.settings.applications.defaultapps.-$$Lambda$DefaultAppPreferenceController$P93yGe3NhKzPqeqQwHkMaXpVB1M
                @Override // com.android.settings.widget.GearPreference.OnGearClickListener
                public final void onGearClick(GearPreference gearPreference) {
                    this.f$0.mContext.startActivity(settingIntent);
                }
            });
        } else {
            ((GearPreference) preference).setOnGearClickListener(null);
        }
    }

    protected Intent getSettingIntent(DefaultAppInfo defaultAppInfo) {
        return null;
    }

    public Drawable getDefaultAppIcon() {
        DefaultAppInfo defaultAppInfo;
        if (isAvailable() && (defaultAppInfo = getDefaultAppInfo()) != null) {
            return defaultAppInfo.loadIcon();
        }
        return null;
    }

    public CharSequence getDefaultAppLabel() {
        DefaultAppInfo defaultAppInfo;
        if (isAvailable() && (defaultAppInfo = getDefaultAppInfo()) != null) {
            return defaultAppInfo.loadLabel();
        }
        return null;
    }
}
