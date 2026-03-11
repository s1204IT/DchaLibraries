package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.internal.telephony.SmsApplication;
import com.android.settings.AppListPreference;
import com.android.settings.SelfAvailablePreference;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISmsPreferenceExt;
import java.util.Collection;
import java.util.Objects;

public class DefaultSmsPreference extends AppListPreference implements SelfAvailablePreference {
    private ISmsPreferenceExt mExt;

    public DefaultSmsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mExt = UtilsExt.getSmsPreferencePlugin(getContext());
        this.mExt.createBroadcastReceiver(getContext(), this);
        loadSmsApps();
    }

    private void loadSmsApps() {
        Collection<SmsApplication.SmsApplicationData> smsApplications = SmsApplication.getApplicationCollection(getContext());
        int count = smsApplications.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (SmsApplication.SmsApplicationData smsApplicationData : smsApplications) {
            packageNames[i] = smsApplicationData.mPackageName;
            i++;
        }
        setPackageNames(packageNames, getDefaultPackage());
    }

    private String getDefaultPackage() {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(getContext(), true);
        if (appName != null) {
            return appName.getPackageName();
        }
        return null;
    }

    @Override
    protected boolean persistString(String value) {
        if (!TextUtils.isEmpty(value) && !Objects.equals(value, getDefaultPackage()) && this.mExt.getBroadcastIntent(getContext(), value)) {
            SmsApplication.setDefaultApplication(value, getContext());
        }
        if (this.mExt.canSetSummary()) {
            setSummary(getEntry());
            return true;
        }
        return true;
    }

    @Override
    public boolean isAvailable(Context context) {
        boolean isRestrictedUser = UserManager.get(context).getUserInfo(UserHandle.myUserId()).isRestricted();
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (isRestrictedUser) {
            return false;
        }
        return tm.isSmsCapable();
    }

    public static boolean hasSmsPreference(String pkg, Context context) {
        Collection<SmsApplication.SmsApplicationData> smsApplications = SmsApplication.getApplicationCollection(context);
        for (SmsApplication.SmsApplicationData data : smsApplications) {
            if (data.mPackageName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSmsDefault(String pkg, Context context) {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(context, true);
        if (appName != null) {
            return appName.getPackageName().equals(pkg);
        }
        return false;
    }
}
