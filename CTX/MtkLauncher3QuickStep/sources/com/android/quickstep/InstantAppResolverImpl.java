package com.android.quickstep;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstantAppInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.android.launcher3.AppInfo;
import com.android.launcher3.util.InstantAppResolver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InstantAppResolverImpl extends InstantAppResolver {
    public static final String COMPONENT_CLASS_MARKER = "@instantapp";
    private static final String TAG = "InstantAppResolverImpl";
    private final PackageManager mPM;

    public InstantAppResolverImpl(Context context) throws NoSuchMethodException, ClassNotFoundException {
        this.mPM = context.getPackageManager();
    }

    @Override
    public boolean isInstantApp(ApplicationInfo applicationInfo) {
        return applicationInfo.isInstantApp();
    }

    @Override
    public boolean isInstantApp(AppInfo appInfo) {
        ComponentName targetComponent = appInfo.getTargetComponent();
        return targetComponent != null && targetComponent.getClassName().equals(COMPONENT_CLASS_MARKER);
    }

    @Override
    public List<ApplicationInfo> getInstantApps() {
        try {
            ArrayList arrayList = new ArrayList();
            Iterator it = this.mPM.getInstantApps().iterator();
            while (it.hasNext()) {
                ApplicationInfo applicationInfo = ((InstantAppInfo) it.next()).getApplicationInfo();
                if (applicationInfo != null) {
                    arrayList.add(applicationInfo);
                }
            }
            return arrayList;
        } catch (SecurityException e) {
            Log.w(TAG, "getInstantApps failed. Launcher may not be the default home app.", e);
            return super.getInstantApps();
        } catch (Exception e2) {
            Log.e(TAG, "Error calling API: getInstantApps", e2);
            return super.getInstantApps();
        }
    }
}
