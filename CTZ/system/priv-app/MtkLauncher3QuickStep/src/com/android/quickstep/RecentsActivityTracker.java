package com.android.quickstep;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import com.android.launcher3.MainThreadExecutor;
import com.android.quickstep.ActivityControlHelper;
import com.android.quickstep.util.RemoteAnimationProvider;
import java.lang.ref.WeakReference;
import java.util.function.BiPredicate;

@TargetApi(28)
/* loaded from: classes.dex */
public class RecentsActivityTracker implements ActivityControlHelper.ActivityInitListener {
    private static WeakReference<RecentsActivity> sCurrentActivity = new WeakReference<>(null);
    private static final Scheduler sScheduler = new Scheduler();
    private final BiPredicate<RecentsActivity, Boolean> mOnInitListener;

    public RecentsActivityTracker(BiPredicate<RecentsActivity, Boolean> biPredicate) {
        this.mOnInitListener = biPredicate;
    }

    @Override // com.android.quickstep.ActivityControlHelper.ActivityInitListener
    public void register() {
        sScheduler.schedule(this);
    }

    @Override // com.android.quickstep.ActivityControlHelper.ActivityInitListener
    public void unregister() {
        sScheduler.clearReference(this);
    }

    private boolean init(RecentsActivity recentsActivity, boolean z) {
        return this.mOnInitListener.test(recentsActivity, Boolean.valueOf(z));
    }

    public static RecentsActivity getCurrentActivity() {
        return sCurrentActivity.get();
    }

    @Override // com.android.quickstep.ActivityControlHelper.ActivityInitListener
    public void registerAndStartActivity(Intent intent, RemoteAnimationProvider remoteAnimationProvider, Context context, Handler handler, long j) {
        register();
        context.startActivity(intent, remoteAnimationProvider.toActivityOptions(handler, j).toBundle());
    }

    public static void onRecentsActivityCreate(RecentsActivity recentsActivity) {
        sCurrentActivity = new WeakReference<>(recentsActivity);
        sScheduler.initIfPending(recentsActivity, false);
    }

    public static void onRecentsActivityNewIntent(RecentsActivity recentsActivity) {
        sScheduler.initIfPending(recentsActivity, recentsActivity.isStarted());
    }

    public static void onRecentsActivityDestroy(RecentsActivity recentsActivity) {
        if (sCurrentActivity.get() == recentsActivity) {
            sCurrentActivity.clear();
        }
    }

    private static class Scheduler implements Runnable {
        private MainThreadExecutor mMainThreadExecutor;
        private WeakReference<RecentsActivityTracker> mPendingTracker;

        private Scheduler() {
            this.mPendingTracker = new WeakReference<>(null);
        }

        /* synthetic */ Scheduler(AnonymousClass1 anonymousClass1) {
            this();
        }

        public synchronized void schedule(RecentsActivityTracker recentsActivityTracker) {
            this.mPendingTracker = new WeakReference<>(recentsActivityTracker);
            if (this.mMainThreadExecutor == null) {
                this.mMainThreadExecutor = new MainThreadExecutor();
            }
            this.mMainThreadExecutor.execute(this);
        }

        @Override // java.lang.Runnable
        public void run() {
            RecentsActivity recentsActivity = (RecentsActivity) RecentsActivityTracker.sCurrentActivity.get();
            if (recentsActivity != null) {
                initIfPending(recentsActivity, recentsActivity.isStarted());
            }
        }

        public synchronized boolean initIfPending(RecentsActivity recentsActivity, boolean z) {
            RecentsActivityTracker recentsActivityTracker = this.mPendingTracker.get();
            if (recentsActivityTracker != null) {
                if (!recentsActivityTracker.init(recentsActivity, z)) {
                    this.mPendingTracker.clear();
                }
                return true;
            }
            return false;
        }

        public synchronized boolean clearReference(RecentsActivityTracker recentsActivityTracker) {
            if (this.mPendingTracker.get() == recentsActivityTracker) {
                this.mPendingTracker.clear();
                return true;
            }
            return false;
        }
    }
}
