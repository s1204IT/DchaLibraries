package com.android.systemui.shortcut;

import android.os.RemoteException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.shortcut.ShortcutKeyServiceProxy;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.DividerView;

public class ShortcutKeyDispatcher extends SystemUI implements ShortcutKeyServiceProxy.Callbacks {
    private ShortcutKeyServiceProxy mShortcutKeyServiceProxy = new ShortcutKeyServiceProxy(this);
    private IWindowManager mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
    protected final long META_MASK = 281474976710656L;
    protected final long ALT_MASK = 8589934592L;
    protected final long CTRL_MASK = 17592186044416L;
    protected final long SHIFT_MASK = 4294967296L;
    protected final long SC_DOCK_LEFT = 281474976710727L;
    protected final long SC_DOCK_RIGHT = 281474976710728L;

    public void registerShortcutKey(long j) {
        try {
            this.mWindowManagerService.registerShortcutKey(j, this.mShortcutKeyServiceProxy);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onShortcutKeyPressed(long j) {
        int i = this.mContext.getResources().getConfiguration().orientation;
        if ((j == 281474976710727L || j == 281474976710728L) && i == 2) {
            handleDockKey(j);
        }
    }

    @Override
    public void start() {
        registerShortcutKey(281474976710727L);
        registerShortcutKey(281474976710728L);
    }

    private void handleDockKey(long j) {
        DividerSnapAlgorithm.SnapTarget nextTarget;
        try {
            if (this.mWindowManagerService.getDockedStackSide() == -1) {
                ((Recents) getComponent(Recents.class)).splitPrimaryTask(-1, j == 281474976710727L ? 0 : 1, null, -1);
                return;
            }
            DividerView view = ((Divider) getComponent(Divider.class)).getView();
            DividerSnapAlgorithm snapAlgorithm = view.getSnapAlgorithm();
            DividerSnapAlgorithm.SnapTarget snapTargetCalculateNonDismissingSnapTarget = snapAlgorithm.calculateNonDismissingSnapTarget(view.getCurrentPosition());
            if (j == 281474976710727L) {
                nextTarget = snapAlgorithm.getPreviousTarget(snapTargetCalculateNonDismissingSnapTarget);
            } else {
                nextTarget = snapAlgorithm.getNextTarget(snapTargetCalculateNonDismissingSnapTarget);
            }
            view.startDragging(true, false);
            view.stopDragging(nextTarget.position, 0.0f, false, true);
        } catch (RemoteException e) {
            Log.e("ShortcutKeyDispatcher", "handleDockKey() failed.");
        }
    }
}
