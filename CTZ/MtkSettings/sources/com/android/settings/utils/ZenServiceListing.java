package com.android.settings.utils;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.util.ArraySet;
import android.util.Slog;
import com.android.settings.utils.ManagedServiceSettings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ZenServiceListing {
    private final ManagedServiceSettings.Config mConfig;
    private final Context mContext;
    private final NotificationManager mNm;
    private final Set<ServiceInfo> mApprovedServices = new ArraySet();
    private final List<Callback> mZenCallbacks = new ArrayList();

    public interface Callback {
        void onServicesReloaded(Set<ServiceInfo> set);
    }

    public ZenServiceListing(Context context, ManagedServiceSettings.Config config) {
        this.mContext = context;
        this.mConfig = config;
        this.mNm = (NotificationManager) context.getSystemService("notification");
    }

    public ServiceInfo findService(ComponentName componentName) {
        for (ServiceInfo serviceInfo : this.mApprovedServices) {
            if (new ComponentName(serviceInfo.packageName, serviceInfo.name).equals(componentName)) {
                return serviceInfo;
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
        List enabledNotificationListenerPackages = this.mNm.getEnabledNotificationListenerPackages();
        ArrayList<ServiceInfo> arrayList = new ArrayList();
        getServices(this.mConfig, arrayList, this.mContext.getPackageManager());
        for (ServiceInfo serviceInfo : arrayList) {
            String packageName = serviceInfo.getComponentName().getPackageName();
            if (this.mNm.isNotificationPolicyAccessGrantedForPackage(packageName) || enabledNotificationListenerPackages.contains(packageName)) {
                this.mApprovedServices.add(serviceInfo);
            }
        }
        if (!this.mApprovedServices.isEmpty()) {
            Iterator<Callback> it = this.mZenCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onServicesReloaded(this.mApprovedServices);
            }
        }
    }

    private static int getServices(ManagedServiceSettings.Config config, List<ServiceInfo> list, PackageManager packageManager) {
        if (list != null) {
            list.clear();
        }
        List listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent(config.intentAction), 132, ActivityManager.getCurrentUser());
        int size = listQueryIntentServicesAsUser.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            ServiceInfo serviceInfo = ((ResolveInfo) listQueryIntentServicesAsUser.get(i2)).serviceInfo;
            if (!config.permission.equals(serviceInfo.permission)) {
                Slog.w(config.tag, "Skipping " + config.noun + " service " + serviceInfo.packageName + "/" + serviceInfo.name + ": it does not require the permission " + config.permission);
            } else {
                if (list != null) {
                    list.add(serviceInfo);
                }
                i++;
            }
        }
        return i;
    }
}
