package com.android.settingslib.accessibility;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/* loaded from: classes.dex */
public class AccessibilityUtils {
    static final TextUtils.SimpleStringSplitter sStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

    public static Set<ComponentName> getEnabledServicesFromSettings(Context context) {
        return getEnabledServicesFromSettings(context, UserHandle.myUserId());
    }

    public static boolean hasServiceCrashed(String str, String str2, List<AccessibilityServiceInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            AccessibilityServiceInfo accessibilityServiceInfo = list.get(i);
            ServiceInfo serviceInfo = list.get(i).getResolveInfo().serviceInfo;
            if (TextUtils.equals(serviceInfo.packageName, str) && TextUtils.equals(serviceInfo.name, str2)) {
                return accessibilityServiceInfo.crashed;
            }
        }
        return false;
    }

    public static Set<ComponentName> getEnabledServicesFromSettings(Context context, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), "enabled_accessibility_services", i);
        if (stringForUser == null) {
            return Collections.emptySet();
        }
        HashSet hashSet = new HashSet();
        TextUtils.SimpleStringSplitter simpleStringSplitter = sStringColonSplitter;
        simpleStringSplitter.setString(stringForUser);
        while (simpleStringSplitter.hasNext()) {
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(simpleStringSplitter.next());
            if (componentNameUnflattenFromString != null) {
                hashSet.add(componentNameUnflattenFromString);
            }
        }
        return hashSet;
    }

    public static CharSequence getTextForLocale(Context context, Locale locale, int i) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration).getText(i);
    }

    public static void setAccessibilityServiceState(Context context, ComponentName componentName, boolean z) {
        setAccessibilityServiceState(context, componentName, z, UserHandle.myUserId());
    }

    public static void setAccessibilityServiceState(Context context, ComponentName componentName, boolean z, int i) {
        Set enabledServicesFromSettings = getEnabledServicesFromSettings(context, i);
        if (enabledServicesFromSettings.isEmpty()) {
            enabledServicesFromSettings = new ArraySet(1);
        }
        if (z) {
            enabledServicesFromSettings.add(componentName);
        } else {
            enabledServicesFromSettings.remove(componentName);
            Set<ComponentName> installedServices = getInstalledServices(context);
            Iterator it = enabledServicesFromSettings.iterator();
            while (it.hasNext() && !installedServices.contains((ComponentName) it.next())) {
            }
        }
        StringBuilder sb = new StringBuilder();
        Iterator it2 = enabledServicesFromSettings.iterator();
        while (it2.hasNext()) {
            sb.append(((ComponentName) it2.next()).flattenToString());
            sb.append(':');
        }
        int length = sb.length();
        if (length > 0) {
            sb.deleteCharAt(length - 1);
        }
        Settings.Secure.putStringForUser(context.getContentResolver(), "enabled_accessibility_services", sb.toString(), i);
    }

    public static String getShortcutTargetServiceComponentNameString(Context context, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), "accessibility_shortcut_target_service", i);
        if (stringForUser != null) {
            return stringForUser;
        }
        return context.getString(R.string.adb_active_notification_message);
    }

    public static boolean isShortcutEnabled(Context context, int i) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "accessibility_shortcut_enabled", 1, i) == 1;
    }

    private static Set<ComponentName> getInstalledServices(Context context) {
        HashSet hashSet = new HashSet();
        hashSet.clear();
        List<AccessibilityServiceInfo> installedAccessibilityServiceList = AccessibilityManager.getInstance(context).getInstalledAccessibilityServiceList();
        if (installedAccessibilityServiceList == null) {
            return hashSet;
        }
        Iterator<AccessibilityServiceInfo> it = installedAccessibilityServiceList.iterator();
        while (it.hasNext()) {
            ResolveInfo resolveInfo = it.next().getResolveInfo();
            hashSet.add(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
        }
        return hashSet;
    }
}
