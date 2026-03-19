package com.android.internal.app;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.ResolverActivity;
import com.android.internal.app.ResolverComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ResolverListController {
    private static final boolean DEBUG = false;
    private static final String TAG = "ResolverListController";
    private boolean isComputed = false;
    private final Context mContext;
    private final int mLaunchedFromUid;
    private final String mReferrerPackage;
    private ResolverComparator mResolverComparator;
    private final Intent mTargetIntent;
    private final PackageManager mpm;

    public ResolverListController(Context context, PackageManager packageManager, Intent intent, String str, int i) {
        this.mContext = context;
        this.mpm = packageManager;
        this.mLaunchedFromUid = i;
        this.mTargetIntent = intent;
        this.mReferrerPackage = str;
        this.mResolverComparator = new ResolverComparator(this.mContext, this.mTargetIntent, this.mReferrerPackage, null);
    }

    @VisibleForTesting
    public ResolveInfo getLastChosen() throws RemoteException {
        return AppGlobals.getPackageManager().getLastChosenActivity(this.mTargetIntent, this.mTargetIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 65536);
    }

    @VisibleForTesting
    public void setLastChosen(Intent intent, IntentFilter intentFilter, int i) throws RemoteException {
        AppGlobals.getPackageManager().setLastChosenActivity(intent, intent.resolveType(this.mContext.getContentResolver()), 65536, intentFilter, i, intent.getComponent());
    }

    @VisibleForTesting
    public List<ResolverActivity.ResolvedComponentInfo> getResolversForIntent(boolean z, boolean z2, List<Intent> list) {
        int size = list.size();
        ArrayList arrayList = null;
        for (int i = 0; i < size; i++) {
            Intent intent = list.get(i);
            int i2 = 65536 | (z ? 64 : 0) | (z2 ? 128 : 0);
            if (intent.isWebIntent() || (intent.getFlags() & 2048) != 0) {
                i2 |= 8388608;
            }
            List<ResolveInfo> listQueryIntentActivities = this.mpm.queryIntentActivities(intent, i2);
            for (int size2 = listQueryIntentActivities.size() - 1; size2 >= 0; size2--) {
                ResolveInfo resolveInfo = listQueryIntentActivities.get(size2);
                if (resolveInfo.activityInfo != null && !resolveInfo.activityInfo.exported) {
                    listQueryIntentActivities.remove(size2);
                }
            }
            if (listQueryIntentActivities != null) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                addResolveListDedupe(arrayList, intent, listQueryIntentActivities);
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    public void addResolveListDedupe(List<ResolverActivity.ResolvedComponentInfo> list, Intent intent, List<ResolveInfo> list2) {
        boolean z;
        int size = list2.size();
        int size2 = list.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = list2.get(i);
            int i2 = 0;
            while (true) {
                if (i2 < size2) {
                    ResolverActivity.ResolvedComponentInfo resolvedComponentInfo = list.get(i2);
                    if (!isSameResolvedComponent(resolveInfo, resolvedComponentInfo)) {
                        i2++;
                    } else {
                        resolvedComponentInfo.add(intent, resolveInfo);
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                ResolverActivity.ResolvedComponentInfo resolvedComponentInfo2 = new ResolverActivity.ResolvedComponentInfo(componentName, intent, resolveInfo);
                resolvedComponentInfo2.setPinned(isComponentPinned(componentName));
                list.add(resolvedComponentInfo2);
            }
        }
    }

    @VisibleForTesting
    public ArrayList<ResolverActivity.ResolvedComponentInfo> filterIneligibleActivities(List<ResolverActivity.ResolvedComponentInfo> list, boolean z) {
        ArrayList<ResolverActivity.ResolvedComponentInfo> arrayList = null;
        for (int size = list.size() - 1; size >= 0; size--) {
            boolean z2 = false;
            ActivityInfo activityInfo = list.get(size).getResolveInfoAt(0).activityInfo;
            int iCheckComponentPermission = ActivityManager.checkComponentPermission(activityInfo.permission, this.mLaunchedFromUid, activityInfo.applicationInfo.uid, activityInfo.exported);
            if ((activityInfo.applicationInfo.flags & 1073741824) != 0) {
                z2 = true;
            }
            if (iCheckComponentPermission != 0 || z2 || isComponentFiltered(activityInfo.getComponentName())) {
                if (z && arrayList == null) {
                    arrayList = new ArrayList<>(list);
                }
                list.remove(size);
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    public ArrayList<ResolverActivity.ResolvedComponentInfo> filterLowPriority(List<ResolverActivity.ResolvedComponentInfo> list, boolean z) {
        ResolveInfo resolveInfoAt = list.get(0).getResolveInfoAt(0);
        int size = list.size();
        ArrayList<ResolverActivity.ResolvedComponentInfo> arrayList = null;
        for (int i = 1; i < size; i++) {
            ResolveInfo resolveInfoAt2 = list.get(i).getResolveInfoAt(0);
            if (resolveInfoAt.priority != resolveInfoAt2.priority || resolveInfoAt.isDefault != resolveInfoAt2.isDefault) {
                while (i < size) {
                    if (z && arrayList == null) {
                        arrayList = new ArrayList<>(list);
                    }
                    list.remove(i);
                    size--;
                }
            }
        }
        return arrayList;
    }

    private class ComputeCallback implements ResolverComparator.AfterCompute {
        private CountDownLatch mFinishComputeSignal;

        public ComputeCallback(CountDownLatch countDownLatch) {
            this.mFinishComputeSignal = countDownLatch;
        }

        @Override
        public void afterCompute() {
            this.mFinishComputeSignal.countDown();
        }
    }

    @VisibleForTesting
    public void sort(List<ResolverActivity.ResolvedComponentInfo> list) {
        if (this.mResolverComparator == null) {
            Log.d(TAG, "Comparator has already been destroyed; skipped.");
            return;
        }
        try {
            System.currentTimeMillis();
            if (!this.isComputed) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                this.mResolverComparator.setCallBack(new ComputeCallback(countDownLatch));
                this.mResolverComparator.compute(list);
                countDownLatch.await();
                this.isComputed = true;
            }
            Collections.sort(list, this.mResolverComparator);
            System.currentTimeMillis();
        } catch (InterruptedException e) {
            Log.e(TAG, "Compute & Sort was interrupted: " + e);
        }
    }

    private static boolean isSameResolvedComponent(ResolveInfo resolveInfo, ResolverActivity.ResolvedComponentInfo resolvedComponentInfo) {
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        return activityInfo.packageName.equals(resolvedComponentInfo.name.getPackageName()) && activityInfo.name.equals(resolvedComponentInfo.name.getClassName());
    }

    boolean isComponentPinned(ComponentName componentName) {
        return false;
    }

    boolean isComponentFiltered(ComponentName componentName) {
        return false;
    }

    @VisibleForTesting
    public float getScore(ResolverActivity.DisplayResolveInfo displayResolveInfo) {
        return this.mResolverComparator.getScore(displayResolveInfo.getResolvedComponentName());
    }

    public void updateModel(ComponentName componentName) {
        this.mResolverComparator.updateModel(componentName);
    }

    public void updateChooserCounts(String str, int i, String str2) {
        this.mResolverComparator.updateChooserCounts(str, i, str2);
    }

    public void destroy() {
        this.mResolverComparator.destroy();
    }
}
