package com.android.settings.applications;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.settings.AppListPreference;
import com.android.settings.SelfAvailablePreference;
import java.util.List;
import java.util.Objects;

public class DefaultPhonePreference extends AppListPreference implements SelfAvailablePreference {
    private final Context mContext;

    public DefaultPhonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context.getApplicationContext();
        loadDialerApps();
    }

    @Override
    protected boolean persistString(String value) {
        if (!TextUtils.isEmpty(value) && !Objects.equals(value, getDefaultPackage())) {
            DefaultDialerManager.setDefaultDialerApplication(getContext(), value, this.mUserId);
        }
        setSummary(getEntry());
        return true;
    }

    private void loadDialerApps() {
        List<String> dialerPackages = DefaultDialerManager.getInstalledDialerApplications(getContext(), this.mUserId);
        String[] dialers = new String[dialerPackages.size()];
        for (int i = 0; i < dialerPackages.size(); i++) {
            dialers[i] = dialerPackages.get(i);
        }
        setPackageNames(dialers, getDefaultPackage(), getSystemPackage());
    }

    private String getDefaultPackage() {
        return DefaultDialerManager.getDefaultDialerApplication(getContext(), this.mUserId);
    }

    private String getSystemPackage() {
        TelecomManager tm = TelecomManager.from(getContext());
        return tm.getSystemDialerPackage();
    }

    @Override
    public boolean isAvailable(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        if (!tm.isVoiceCapable()) {
            return false;
        }
        UserManager um = (UserManager) context.getSystemService("user");
        return !um.hasUserRestriction("no_outgoing_calls");
    }

    public static boolean hasPhonePreference(String pkg, Context context) {
        List<String> dialerPackages = DefaultDialerManager.getInstalledDialerApplications(context, UserHandle.myUserId());
        return dialerPackages.contains(pkg);
    }

    public static boolean isPhoneDefault(String pkg, Context context) {
        String def = DefaultDialerManager.getDefaultDialerApplication(context, UserHandle.myUserId());
        if (def != null) {
            return def.equals(pkg);
        }
        return false;
    }
}
