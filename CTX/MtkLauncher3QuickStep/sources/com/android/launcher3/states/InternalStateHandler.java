package com.android.launcher3.states;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.MainThreadExecutor;
import java.lang.ref.WeakReference;

public abstract class InternalStateHandler extends Binder {
    public static final String EXTRA_STATE_HANDLER = "launcher.state_handler";
    private static final Scheduler sScheduler = new Scheduler();

    protected abstract boolean init(Launcher launcher, boolean z);

    public final Intent addToIntent(Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putBinder(EXTRA_STATE_HANDLER, this);
        intent.putExtras(bundle);
        return intent;
    }

    public final void initWhenReady() {
        sScheduler.schedule(this);
    }

    public boolean clearReference() {
        return sScheduler.clearReference(this);
    }

    public static boolean hasPending() {
        return sScheduler.hasPending();
    }

    public static boolean handleCreate(Launcher launcher, Intent intent) {
        return handleIntent(launcher, intent, false, false);
    }

    public static boolean handleNewIntent(Launcher launcher, Intent intent, boolean z) {
        return handleIntent(launcher, intent, z, true);
    }

    private static boolean handleIntent(Launcher launcher, Intent intent, boolean z, boolean z2) {
        boolean z3;
        if (intent != null && intent.getExtras() != null) {
            IBinder binder = intent.getExtras().getBinder(EXTRA_STATE_HANDLER);
            if (binder instanceof InternalStateHandler) {
                if (!((InternalStateHandler) binder).init(launcher, z)) {
                    intent.getExtras().remove(EXTRA_STATE_HANDLER);
                }
                z3 = true;
            }
        } else {
            z3 = false;
        }
        if (!z3 && !z2) {
            return sScheduler.initIfPending(launcher, z);
        }
        return z3;
    }

    private static class Scheduler implements Runnable {
        private MainThreadExecutor mMainThreadExecutor;
        private WeakReference<InternalStateHandler> mPendingHandler;

        private Scheduler() {
            this.mPendingHandler = new WeakReference<>(null);
        }

        public synchronized void schedule(InternalStateHandler internalStateHandler) {
            this.mPendingHandler = new WeakReference<>(internalStateHandler);
            if (this.mMainThreadExecutor == null) {
                this.mMainThreadExecutor = new MainThreadExecutor();
            }
            this.mMainThreadExecutor.execute(this);
        }

        @Override
        public void run() {
            LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
            if (instanceNoCreate == null) {
                return;
            }
            LauncherModel.Callbacks callback = instanceNoCreate.getModel().getCallback();
            if (!(callback instanceof Launcher)) {
                return;
            }
            Launcher launcher = (Launcher) callback;
            initIfPending(launcher, launcher.isStarted());
        }

        public synchronized boolean initIfPending(Launcher launcher, boolean z) {
            InternalStateHandler internalStateHandler = this.mPendingHandler.get();
            if (internalStateHandler != null) {
                if (!internalStateHandler.init(launcher, z)) {
                    this.mPendingHandler.clear();
                }
                return true;
            }
            return false;
        }

        public synchronized boolean clearReference(InternalStateHandler internalStateHandler) {
            if (this.mPendingHandler.get() == internalStateHandler) {
                this.mPendingHandler.clear();
                return true;
            }
            return false;
        }

        public boolean hasPending() {
            return this.mPendingHandler.get() != null;
        }
    }
}
