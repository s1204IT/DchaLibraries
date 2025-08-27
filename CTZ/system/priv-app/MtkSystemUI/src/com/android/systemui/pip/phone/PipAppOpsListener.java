package com.android.systemui.pip.phone;

import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Pair;

/* loaded from: classes.dex */
public class PipAppOpsListener {
    private static final String TAG = PipAppOpsListener.class.getSimpleName();
    private IActivityManager mActivityManager;
    private AppOpsManager.OnOpChangedListener mAppOpsChangedListener = new AnonymousClass1();
    private AppOpsManager mAppOpsManager;
    private Context mContext;
    private Handler mHandler;
    private PipMotionHelper mMotionHelper;

    /* renamed from: com.android.systemui.pip.phone.PipAppOpsListener$1, reason: invalid class name */
    class AnonymousClass1 implements AppOpsManager.OnOpChangedListener {
        AnonymousClass1() {
        }

        @Override // android.app.AppOpsManager.OnOpChangedListener
        public void onOpChanged(String str, String str2) {
            try {
                Pair<ComponentName, Integer> topPinnedActivity = PipUtils.getTopPinnedActivity(PipAppOpsListener.this.mContext, PipAppOpsListener.this.mActivityManager);
                if (topPinnedActivity.first != null) {
                    ApplicationInfo applicationInfoAsUser = PipAppOpsListener.this.mContext.getPackageManager().getApplicationInfoAsUser(str2, 0, ((Integer) topPinnedActivity.second).intValue());
                    if (applicationInfoAsUser.packageName.equals(((ComponentName) topPinnedActivity.first).getPackageName()) && PipAppOpsListener.this.mAppOpsManager.checkOpNoThrow(67, applicationInfoAsUser.uid, str2) != 0) {
                        PipAppOpsListener.this.mHandler.post(new Runnable() { // from class: com.android.systemui.pip.phone.-$$Lambda$PipAppOpsListener$1$UK38MrwiG74h0N6r_NQ6zq34Mqo
                            @Override // java.lang.Runnable
                            public final void run() {
                                PipAppOpsListener.this.mMotionHelper.dismissPip();
                            }
                        });
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                PipAppOpsListener.this.unregisterAppOpsListener();
            }
        }
    }

    public PipAppOpsListener(Context context, IActivityManager iActivityManager, PipMotionHelper pipMotionHelper) {
        this.mContext = context;
        this.mHandler = new Handler(this.mContext.getMainLooper());
        this.mActivityManager = iActivityManager;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mMotionHelper = pipMotionHelper;
    }

    public void onActivityPinned(String str) {
        registerAppOpsListener(str);
    }

    public void onActivityUnpinned() {
        unregisterAppOpsListener();
    }

    private void registerAppOpsListener(String str) {
        this.mAppOpsManager.startWatchingMode(67, str, this.mAppOpsChangedListener);
    }

    private void unregisterAppOpsListener() {
        this.mAppOpsManager.stopWatchingMode(this.mAppOpsChangedListener);
    }
}
