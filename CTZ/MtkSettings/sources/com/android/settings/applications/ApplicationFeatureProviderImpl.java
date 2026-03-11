package com.android.settings.applications;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.text.TextUtils;
import android.util.ArraySet;
import com.android.internal.telephony.SmsApplication;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ApplicationFeatureProviderImpl implements ApplicationFeatureProvider {
    private final Context mContext;
    private final DevicePolicyManager mDpm;
    private final PackageManagerWrapper mPm;
    private final IPackageManager mPms;
    private final UserManager mUm;

    public ApplicationFeatureProviderImpl(Context context, PackageManagerWrapper packageManagerWrapper, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager) {
        this.mContext = context.getApplicationContext();
        this.mPm = packageManagerWrapper;
        this.mPms = iPackageManager;
        this.mDpm = devicePolicyManager;
        this.mUm = UserManager.get(this.mContext);
    }

    @Override
    public void calculateNumberOfPolicyInstalledApps(boolean z, ApplicationFeatureProvider.NumberOfAppsCallback numberOfAppsCallback) {
        CurrentUserAndManagedProfilePolicyInstalledAppCounter currentUserAndManagedProfilePolicyInstalledAppCounter = new CurrentUserAndManagedProfilePolicyInstalledAppCounter(this.mContext, this.mPm, numberOfAppsCallback);
        if (z) {
            currentUserAndManagedProfilePolicyInstalledAppCounter.execute(new Void[0]);
        } else {
            currentUserAndManagedProfilePolicyInstalledAppCounter.executeInForeground();
        }
    }

    @Override
    public void listPolicyInstalledApps(ApplicationFeatureProvider.ListOfAppsCallback listOfAppsCallback) {
        new CurrentUserPolicyInstalledAppLister(this.mPm, this.mUm, listOfAppsCallback).execute(new Void[0]);
    }

    @Override
    public void calculateNumberOfAppsWithAdminGrantedPermissions(String[] strArr, boolean z, ApplicationFeatureProvider.NumberOfAppsCallback numberOfAppsCallback) {
        CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter currentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter = new CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter(this.mContext, strArr, this.mPm, this.mPms, this.mDpm, numberOfAppsCallback);
        if (z) {
            currentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter.execute(new Void[0]);
        } else {
            currentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter.executeInForeground();
        }
    }

    @Override
    public void listAppsWithAdminGrantedPermissions(String[] strArr, ApplicationFeatureProvider.ListOfAppsCallback listOfAppsCallback) {
        new CurrentUserAppWithAdminGrantedPermissionsLister(strArr, this.mPm, this.mPms, this.mDpm, this.mUm, listOfAppsCallback).execute(new Void[0]);
    }

    @Override
    public List<UserAppInfo> findPersistentPreferredActivities(int i, Intent[] intentArr) {
        ArrayList arrayList = new ArrayList();
        ArraySet arraySet = new ArraySet();
        UserInfo userInfo = this.mUm.getUserInfo(i);
        for (Intent intent : intentArr) {
            try {
                ResolveInfo resolveInfoFindPersistentPreferredActivity = this.mPms.findPersistentPreferredActivity(intent, i);
                if (resolveInfoFindPersistentPreferredActivity != null) {
                    ComponentInfo componentInfo = null;
                    if (resolveInfoFindPersistentPreferredActivity.activityInfo != null) {
                        componentInfo = resolveInfoFindPersistentPreferredActivity.activityInfo;
                    } else if (resolveInfoFindPersistentPreferredActivity.serviceInfo != null) {
                        componentInfo = resolveInfoFindPersistentPreferredActivity.serviceInfo;
                    } else if (resolveInfoFindPersistentPreferredActivity.providerInfo != null) {
                        componentInfo = resolveInfoFindPersistentPreferredActivity.providerInfo;
                    }
                    if (componentInfo != null) {
                        UserAppInfo userAppInfo = new UserAppInfo(userInfo, componentInfo.applicationInfo);
                        if (arraySet.add(userAppInfo)) {
                            arrayList.add(userAppInfo);
                        }
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return arrayList;
    }

    @Override
    public Set<String> getKeepEnabledPackages() {
        ArraySet arraySet = new ArraySet();
        String defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(this.mContext);
        if (!TextUtils.isEmpty(defaultDialerApplication)) {
            arraySet.add(defaultDialerApplication);
        }
        ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(this.mContext, true);
        if (defaultSmsApplication != null) {
            arraySet.add(defaultSmsApplication.getPackageName());
        }
        return arraySet;
    }

    private static class CurrentUserAndManagedProfilePolicyInstalledAppCounter extends InstalledAppCounter {
        private ApplicationFeatureProvider.NumberOfAppsCallback mCallback;

        CurrentUserAndManagedProfilePolicyInstalledAppCounter(Context context, PackageManagerWrapper packageManagerWrapper, ApplicationFeatureProvider.NumberOfAppsCallback numberOfAppsCallback) {
            super(context, 1, packageManagerWrapper);
            this.mCallback = numberOfAppsCallback;
        }

        @Override
        protected void onCountComplete(int i) {
            this.mCallback.onNumberOfAppsResult(i);
        }
    }

    private static class CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter extends AppWithAdminGrantedPermissionsCounter {
        private ApplicationFeatureProvider.NumberOfAppsCallback mCallback;

        CurrentUserAndManagedProfileAppWithAdminGrantedPermissionsCounter(Context context, String[] strArr, PackageManagerWrapper packageManagerWrapper, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager, ApplicationFeatureProvider.NumberOfAppsCallback numberOfAppsCallback) {
            super(context, strArr, packageManagerWrapper, iPackageManager, devicePolicyManager);
            this.mCallback = numberOfAppsCallback;
        }

        @Override
        protected void onCountComplete(int i) {
            this.mCallback.onNumberOfAppsResult(i);
        }
    }

    private static class CurrentUserPolicyInstalledAppLister extends InstalledAppLister {
        private ApplicationFeatureProvider.ListOfAppsCallback mCallback;

        CurrentUserPolicyInstalledAppLister(PackageManagerWrapper packageManagerWrapper, UserManager userManager, ApplicationFeatureProvider.ListOfAppsCallback listOfAppsCallback) {
            super(packageManagerWrapper, userManager);
            this.mCallback = listOfAppsCallback;
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            this.mCallback.onListOfAppsResult(list);
        }
    }

    private static class CurrentUserAppWithAdminGrantedPermissionsLister extends AppWithAdminGrantedPermissionsLister {
        private ApplicationFeatureProvider.ListOfAppsCallback mCallback;

        CurrentUserAppWithAdminGrantedPermissionsLister(String[] strArr, PackageManagerWrapper packageManagerWrapper, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager, UserManager userManager, ApplicationFeatureProvider.ListOfAppsCallback listOfAppsCallback) {
            super(strArr, packageManagerWrapper, iPackageManager, devicePolicyManager, userManager);
            this.mCallback = listOfAppsCallback;
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            this.mCallback.onListOfAppsResult(list);
        }
    }
}
