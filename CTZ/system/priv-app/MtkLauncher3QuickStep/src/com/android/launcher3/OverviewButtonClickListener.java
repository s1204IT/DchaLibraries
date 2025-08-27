package com.android.launcher3;

import android.view.View;

/* loaded from: classes.dex */
public abstract class OverviewButtonClickListener implements View.OnClickListener, View.OnLongClickListener {
    private int mControlType;

    public abstract void handleViewClick(View view);

    public OverviewButtonClickListener(int i) {
        this.mControlType = i;
    }

    public void attachTo(View view) {
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (shouldPerformClick(view)) {
            handleViewClick(view, 0);
        }
    }

    @Override // android.view.View.OnLongClickListener
    public boolean onLongClick(View view) {
        if (shouldPerformClick(view)) {
            handleViewClick(view, 1);
        }
        return true;
    }

    private boolean shouldPerformClick(View view) {
        return !Launcher.getLauncher(view.getContext()).getWorkspace().isSwitchingState();
    }

    private void handleViewClick(View view, int i) {
        handleViewClick(view);
        Launcher.getLauncher(view.getContext()).getUserEventDispatcher().logActionOnControl(i, this.mControlType);
    }
}
