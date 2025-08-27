package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.List;

/* loaded from: classes.dex */
public class DefaultEmergencyPreferenceController extends DefaultAppPreferenceController {
    public static final Intent QUERY_INTENT = new Intent("android.telephony.action.EMERGENCY_ASSISTANCE");

    public DefaultEmergencyPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "default_emergency_app";
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected DefaultAppInfo getDefaultAppInfo() {
        return null;
    }

    public static boolean hasEmergencyPreference(String str, Context context) {
        Intent intent = new Intent(QUERY_INTENT);
        intent.setPackage(str);
        List<ResolveInfo> listQueryIntentActivities = context.getPackageManager().queryIntentActivities(intent, 0);
        return (listQueryIntentActivities == null || listQueryIntentActivities.size() == 0) ? false : true;
    }

    public static boolean isEmergencyDefault(String str, Context context) {
        String string = Settings.Secure.getString(context.getContentResolver(), "emergency_assistance_application");
        return string != null && string.equals(str);
    }
}
