package com.android.settings.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.settings.utils.ManagedServiceSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ZenServiceListing {
    private final ManagedServiceSettings.Config mConfig;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final Set<ServiceInfo> mApprovedServices = new ArraySet();
    private final List<Callback> mZenCallbacks = new ArrayList();

    public interface Callback {
        void onServicesReloaded(Set<ServiceInfo> set);
    }

    public ZenServiceListing(Context context, ManagedServiceSettings.Config config) {
        this.mContext = context;
        this.mConfig = config;
        this.mContentResolver = context.getContentResolver();
    }

    public ServiceInfo findService(ComponentName cn) {
        for (ServiceInfo service : this.mApprovedServices) {
            ComponentName serviceCN = new ComponentName(service.packageName, service.name);
            if (serviceCN.equals(cn)) {
                return service;
            }
        }
        return null;
    }

    public void addZenCallback(Callback callback) {
        this.mZenCallbacks.add(callback);
    }

    public void removeZenCallback(Callback callback) {
        this.mZenCallbacks.remove(callback);
    }

    public void reloadApprovedServices() {
        this.mApprovedServices.clear();
        String[] settings = {this.mConfig.setting, this.mConfig.secondarySetting};
        for (String setting : settings) {
            if (!TextUtils.isEmpty(setting)) {
                String flat = Settings.Secure.getString(this.mContentResolver, setting);
                if (!TextUtils.isEmpty(flat)) {
                    List<String> names = Arrays.asList(flat.split(":"));
                    List<ServiceInfo> services = new ArrayList<>();
                    getServices(this.mConfig, services, this.mContext.getPackageManager());
                    for (ServiceInfo service : services) {
                        if (matchesApprovedPackage(names, service.getComponentName())) {
                            this.mApprovedServices.add(service);
                        }
                    }
                }
            }
        }
        if (this.mApprovedServices.isEmpty()) {
            return;
        }
        for (Callback callback : this.mZenCallbacks) {
            callback.onServicesReloaded(this.mApprovedServices);
        }
    }

    private boolean matchesApprovedPackage(List<String> approved, ComponentName serviceOwner) {
        ComponentName approvedComponent;
        String flatCn = serviceOwner.flattenToString();
        if (approved.contains(flatCn) || approved.contains(serviceOwner.getPackageName())) {
            return true;
        }
        for (String entry : approved) {
            if (!TextUtils.isEmpty(entry) && (approvedComponent = ComponentName.unflattenFromString(entry)) != null && approvedComponent.getPackageName().equals(serviceOwner.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private static int getServices(ManagedServiceSettings.Config c, List<ServiceInfo> list, PackageManager pm) {
        int services = 0;
        if (list != null) {
            list.clear();
        }
        int user = ActivityManager.getCurrentUser();
        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(new Intent(c.intentAction), 132, user);
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo info = resolveInfo.serviceInfo;
            if (c.permission.equals(info.permission)) {
                if (list != null) {
                    list.add(info);
                }
                services++;
            } else {
                Slog.w(c.tag, "Skipping " + c.noun + " service " + info.packageName + "/" + info.name + ": it does not require the permission " + c.permission);
            }
        }
        return services;
    }
}
