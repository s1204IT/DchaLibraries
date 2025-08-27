package com.android.settings.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.widget.FooterPreference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class LocationFooterPreferenceController extends LocationBasePreferenceController implements LifecycleObserver, OnPause {
    private static final Intent INJECT_INTENT = new Intent("com.android.settings.location.DISPLAYED_FOOTER");
    private final Context mContext;
    private Collection<ComponentName> mFooterInjectors;
    private final PackageManager mPackageManager;

    public LocationFooterPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        this.mContext = context;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mFooterInjectors = new ArrayList();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "location_footer";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        PreferenceCategory preferenceCategory = (PreferenceCategory) preference;
        preferenceCategory.removeAll();
        this.mFooterInjectors.clear();
        for (FooterData footerData : getFooterData()) {
            FooterPreference footerPreference = new FooterPreference(preference.getContext());
            try {
                footerPreference.setTitle(this.mPackageManager.getResourcesForApplication(footerData.applicationInfo).getString(footerData.footerStringRes));
                preferenceCategory.addPreference(footerPreference);
                sendBroadcastFooterDisplayed(footerData.componentName);
                this.mFooterInjectors.add(footerData.componentName);
            } catch (PackageManager.NameNotFoundException e) {
                if (Log.isLoggable("LocationFooter", 5)) {
                    Log.w("LocationFooter", "Resources not found for application " + footerData.applicationInfo.packageName);
                }
            }
        }
    }

    @Override // com.android.settings.location.LocationEnabler.LocationModeChangeListener
    public void onLocationModeChanged(int i, boolean z) {
    }

    @Override // com.android.settings.location.LocationBasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !getFooterData().isEmpty();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        for (ComponentName componentName : this.mFooterInjectors) {
            Intent intent = new Intent("com.android.settings.location.REMOVED_FOOTER");
            intent.setComponent(componentName);
            this.mContext.sendBroadcast(intent);
        }
    }

    void sendBroadcastFooterDisplayed(ComponentName componentName) {
        Intent intent = new Intent("com.android.settings.location.DISPLAYED_FOOTER");
        intent.setComponent(componentName);
        this.mContext.sendBroadcast(intent);
    }

    private Collection<FooterData> getFooterData() {
        List<ResolveInfo> listQueryBroadcastReceivers = this.mPackageManager.queryBroadcastReceivers(INJECT_INTENT, 128);
        if (listQueryBroadcastReceivers != null) {
            if (Log.isLoggable("LocationFooter", 3)) {
                Log.d("LocationFooter", "Found broadcast receivers: " + listQueryBroadcastReceivers);
            }
        } else if (Log.isLoggable("LocationFooter", 6)) {
            Log.e("LocationFooter", "Unable to resolve intent " + INJECT_INTENT);
            return Collections.emptyList();
        }
        ArrayList arrayList = new ArrayList(listQueryBroadcastReceivers.size());
        for (ResolveInfo resolveInfo : listQueryBroadcastReceivers) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            ApplicationInfo applicationInfo = activityInfo.applicationInfo;
            if ((applicationInfo.flags & 1) == 0 && Log.isLoggable("LocationFooter", 5)) {
                Log.w("LocationFooter", "Ignoring attempt to inject footer from app not in system image: " + resolveInfo);
            } else if (activityInfo.metaData == null && Log.isLoggable("LocationFooter", 3)) {
                Log.d("LocationFooter", "No METADATA in broadcast receiver " + activityInfo.name);
            } else {
                int i = activityInfo.metaData.getInt("com.android.settings.location.FOOTER_STRING");
                if (i == 0) {
                    if (Log.isLoggable("LocationFooter", 5)) {
                        Log.w("LocationFooter", "No mapping of integer exists for com.android.settings.location.FOOTER_STRING");
                    }
                } else {
                    arrayList.add(new FooterData(i, applicationInfo, new ComponentName(activityInfo.packageName, activityInfo.name)));
                }
            }
        }
        return arrayList;
    }

    private static class FooterData {
        final ApplicationInfo applicationInfo;
        final ComponentName componentName;
        final int footerStringRes;

        FooterData(int i, ApplicationInfo applicationInfo, ComponentName componentName) {
            this.footerStringRes = i;
            this.applicationInfo = applicationInfo;
            this.componentName = componentName;
        }
    }
}
