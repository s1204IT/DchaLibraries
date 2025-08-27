package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telephony.TelephonyManager;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultPhonePreferenceController extends DefaultAppPreferenceController {
    public DefaultPhonePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        List<String> candidates;
        return (!((TelephonyManager) this.mContext.getSystemService("phone")).isVoiceCapable() || ((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_outgoing_calls") || (candidates = getCandidates()) == null || candidates.isEmpty()) ? false : true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "default_phone_app";
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected DefaultAppInfo getDefaultAppInfo() {
        try {
            return new DefaultAppInfo(this.mContext, this.mPackageManager, this.mPackageManager.getPackageManager().getApplicationInfo(DefaultDialerManager.getDefaultDialerApplication(this.mContext, this.mUserId), 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private List<String> getCandidates() {
        return DefaultDialerManager.getInstalledDialerApplications(this.mContext, this.mUserId);
    }

    public static boolean hasPhonePreference(String str, Context context) {
        return DefaultDialerManager.getInstalledDialerApplications(context, UserHandle.myUserId()).contains(str);
    }

    public static boolean isPhoneDefault(String str, Context context) {
        String defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(context, UserHandle.myUserId());
        return defaultDialerApplication != null && defaultDialerApplication.equals(str);
    }
}
