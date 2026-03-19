package com.android.server.pm;

import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ICrossProfileApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import java.util.ArrayList;
import java.util.List;

public class CrossProfileAppsServiceImpl extends ICrossProfileApps.Stub {
    private static final String TAG = "CrossProfileAppsService";
    private Context mContext;
    private Injector mInjector;

    @VisibleForTesting
    public interface Injector {
        long clearCallingIdentity();

        ActivityManagerInternal getActivityManagerInternal();

        AppOpsManager getAppOpsManager();

        int getCallingUid();

        UserHandle getCallingUserHandle();

        int getCallingUserId();

        PackageManager getPackageManager();

        PackageManagerInternal getPackageManagerInternal();

        UserManager getUserManager();

        void restoreCallingIdentity(long j);
    }

    public CrossProfileAppsServiceImpl(Context context) {
        this(context, new InjectorImpl(context));
    }

    @VisibleForTesting
    CrossProfileAppsServiceImpl(Context context, Injector injector) {
        this.mContext = context;
        this.mInjector = injector;
    }

    public List<UserHandle> getTargetUserProfiles(String str) {
        Preconditions.checkNotNull(str);
        verifyCallingPackage(str);
        return getTargetUserProfilesUnchecked(str, this.mInjector.getCallingUserId());
    }

    public void startActivityAsUser(IApplicationThread iApplicationThread, String str, ComponentName componentName, UserHandle userHandle) throws RemoteException {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(componentName);
        Preconditions.checkNotNull(userHandle);
        verifyCallingPackage(str);
        if (!getTargetUserProfilesUnchecked(str, this.mInjector.getCallingUserId()).contains(userHandle)) {
            throw new SecurityException(str + " cannot access unrelated user " + userHandle.getIdentifier());
        }
        if (!str.equals(componentName.getPackageName())) {
            throw new SecurityException(str + " attempts to start an activity in other package - " + componentName.getPackageName());
        }
        int callingUid = this.mInjector.getCallingUid();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.addFlags(270532608);
        intent.setPackage(componentName.getPackageName());
        verifyActivityCanHandleIntentAndExported(intent, componentName, callingUid, userHandle);
        intent.setPackage(null);
        intent.setComponent(componentName);
        this.mInjector.getActivityManagerInternal().startActivityAsUser(iApplicationThread, str, intent, ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle(), userHandle.getIdentifier());
    }

    private List<UserHandle> getTargetUserProfilesUnchecked(String str, int i) {
        long jClearCallingIdentity = this.mInjector.clearCallingIdentity();
        try {
            int[] enabledProfileIds = this.mInjector.getUserManager().getEnabledProfileIds(i);
            ArrayList arrayList = new ArrayList();
            for (int i2 : enabledProfileIds) {
                if (i2 != i && isPackageEnabled(str, i2)) {
                    arrayList.add(UserHandle.of(i2));
                }
            }
            return arrayList;
        } finally {
            this.mInjector.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isPackageEnabled(String str, int i) {
        boolean z;
        int callingUid = this.mInjector.getCallingUid();
        long jClearCallingIdentity = this.mInjector.clearCallingIdentity();
        try {
            PackageInfo packageInfo = this.mInjector.getPackageManagerInternal().getPackageInfo(str, 786432, callingUid, i);
            if (packageInfo != null) {
                z = packageInfo.applicationInfo.enabled;
            }
            return z;
        } finally {
            this.mInjector.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void verifyActivityCanHandleIntentAndExported(Intent intent, ComponentName componentName, int i, UserHandle userHandle) {
        long jClearCallingIdentity = this.mInjector.clearCallingIdentity();
        try {
            List listQueryIntentActivities = this.mInjector.getPackageManagerInternal().queryIntentActivities(intent, 786432, i, userHandle.getIdentifier());
            int size = listQueryIntentActivities.size();
            for (int i2 = 0; i2 < size; i2++) {
                ActivityInfo activityInfo = ((ResolveInfo) listQueryIntentActivities.get(i2)).activityInfo;
                if (TextUtils.equals(activityInfo.packageName, componentName.getPackageName()) && TextUtils.equals(activityInfo.name, componentName.getClassName()) && activityInfo.exported) {
                    return;
                }
            }
            throw new SecurityException("Attempt to launch activity without  category Intent.CATEGORY_LAUNCHER or activity is not exported" + componentName);
        } finally {
            this.mInjector.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void verifyCallingPackage(String str) {
        this.mInjector.getAppOpsManager().checkPackage(this.mInjector.getCallingUid(), str);
    }

    private static class InjectorImpl implements Injector {
        private Context mContext;

        public InjectorImpl(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCallingUid() {
            return Binder.getCallingUid();
        }

        @Override
        public int getCallingUserId() {
            return UserHandle.getCallingUserId();
        }

        @Override
        public UserHandle getCallingUserHandle() {
            return Binder.getCallingUserHandle();
        }

        @Override
        public long clearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        @Override
        public void restoreCallingIdentity(long j) {
            Binder.restoreCallingIdentity(j);
        }

        @Override
        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService(UserManager.class);
        }

        @Override
        public PackageManagerInternal getPackageManagerInternal() {
            return (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }

        @Override
        public PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        @Override
        public AppOpsManager getAppOpsManager() {
            return (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        }

        @Override
        public ActivityManagerInternal getActivityManagerInternal() {
            return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }
    }
}
