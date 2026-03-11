package com.android.settings.accounts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.List;

public class EmergencyInfoPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public EmergencyInfoPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(this.mContext.getString(R.string.emergency_info_summary, ((UserManager) this.mContext.getSystemService(UserManager.class)).getUserInfo(UserHandle.myUserId()).name));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("emergency_info".equals(preference.getKey())) {
            Intent intent = new Intent("android.settings.EDIT_EMERGENCY_INFO");
            intent.setFlags(67108864);
            this.mContext.startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        List<ResolveInfo> listQueryIntentActivities = this.mContext.getPackageManager().queryIntentActivities(new Intent("android.settings.EDIT_EMERGENCY_INFO").setPackage("com.android.emergency"), 0);
        return (listQueryIntentActivities == null || listQueryIntentActivities.isEmpty()) ? false : true;
    }

    @Override
    public String getPreferenceKey() {
        return "emergency_info";
    }
}
