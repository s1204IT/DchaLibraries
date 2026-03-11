package com.android.launcher3.util;

import android.view.View;
import android.view.ViewTreeObserver;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executor;

public class ViewOnDrawExecutor implements Executor, ViewTreeObserver.OnDrawListener, Runnable, View.OnAttachStateChangeListener {
    private View mAttachedView;
    private boolean mCompleted;
    private boolean mFirstDrawCompleted;
    private Launcher mLauncher;
    private boolean mLoadAnimationCompleted;
    private final ArrayList<Runnable> mTasks = new ArrayList<>();

    public void attachTo(Launcher launcher) {
        attachTo(launcher, launcher.getWorkspace(), true);
    }

    public void attachTo(Launcher launcher, View view, boolean z) {
        this.mLauncher = launcher;
        this.mAttachedView = view;
        this.mAttachedView.addOnAttachStateChangeListener(this);
        if (!z) {
            this.mLoadAnimationCompleted = true;
        }
        attachObserver();
    }

    private void attachObserver() {
        if (!this.mCompleted) {
            this.mAttachedView.getViewTreeObserver().addOnDrawListener(this);
        }
    }

    @Override
    public void execute(Runnable runnable) {
        this.mTasks.add(runnable);
        LauncherModel.setWorkerPriority(10);
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        attachObserver();
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
    }

    @Override
    public void onDraw() {
        this.mFirstDrawCompleted = true;
        this.mAttachedView.post(this);
    }

    public void onLoadAnimationCompleted() {
        this.mLoadAnimationCompleted = true;
        if (this.mAttachedView != null) {
            this.mAttachedView.post(this);
        }
    }

    @Override
    public void run() {
        if (this.mLoadAnimationCompleted && this.mFirstDrawCompleted && !this.mCompleted) {
            runAllTasks();
        }
    }

    public void markCompleted() {
        this.mTasks.clear();
        this.mCompleted = true;
        if (this.mAttachedView != null) {
            this.mAttachedView.getViewTreeObserver().removeOnDrawListener(this);
            this.mAttachedView.removeOnAttachStateChangeListener(this);
        }
        if (this.mLauncher != null) {
            this.mLauncher.clearPendingExecutor(this);
        }
        LauncherModel.setWorkerPriority(0);
    }

    protected boolean isCompleted() {
        return this.mCompleted;
    }

    protected void runAllTasks() {
        Iterator<Runnable> it = this.mTasks.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        markCompleted();
    }
}
