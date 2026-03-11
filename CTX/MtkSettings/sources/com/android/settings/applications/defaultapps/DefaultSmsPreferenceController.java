package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.SmsApplication;
import com.android.settingslib.applications.DefaultAppInfo;
import java.util.Iterator;

public class DefaultSmsPreferenceController extends DefaultAppPreferenceController {
    public DefaultSmsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !this.mUserManager.getUserInfo(this.mUserId).isRestricted() && ((TelephonyManager) this.mContext.getSystemService("phone")).isSmsCapable();
    }

    @Override
    public String getPreferenceKey() {
        return "default_sms_app";
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(this.mContext, true);
        if (defaultSmsApplication != null) {
            return new DefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, defaultSmsApplication);
        }
        return null;
    }

    public static boolean hasSmsPreference(String str, Context context) {
        Iterator it = SmsApplication.getApplicationCollection(context).iterator();
        while (it.hasNext()) {
            if (((SmsApplication.SmsApplicationData) it.next()).mPackageName.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSmsDefault(String str, Context context) {
        ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(context, true);
        return defaultSmsApplication != null && defaultSmsApplication.getPackageName().equals(str);
    }
}
