package com.android.systemui.shortcut;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
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
    private IActivityManager mActivityManager = ActivityManagerNative.getDefault();
    protected final long META_MASK = 281474976710656L;
    protected final long ALT_MASK = 8589934592L;
    protected final long CTRL_MASK = 17592186044416L;
    protected final long SHIFT_MASK = 4294967296L;
    protected final long SC_DOCK_LEFT = 281474976710727L;
    protected final long SC_DOCK_RIGHT = 281474976710728L;

    public void registerShortcutKey(long shortcutCode) {
        try {
            this.mWindowManagerService.registerShortcutKey(shortcutCode, this.mShortcutKeyServiceProxy);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onShortcutKeyPressed(long shortcutCode) {
        int orientation = this.mContext.getResources().getConfiguration().orientation;
        if ((shortcutCode != 281474976710727L && shortcutCode != 281474976710728L) || orientation != 2) {
            return;
        }
        handleDockKey(shortcutCode);
    }

    @Override
    public void start() {
        registerShortcutKey(281474976710727L);
        registerShortcutKey(281474976710728L);
    }

    private void handleDockKey(long shortcutCode) {
        int dockMode;
        try {
            int dockSide = this.mWindowManagerService.getDockedStackSide();
            if (dockSide == -1) {
                Recents recents = (Recents) getComponent(Recents.class);
                if (shortcutCode == 281474976710727L) {
                    dockMode = 0;
                } else {
                    dockMode = 1;
                }
                recents.dockTopTask(-1, dockMode, null, 352);
            } else {
                DividerView dividerView = ((Divider) getComponent(Divider.class)).getView();
                DividerSnapAlgorithm snapAlgorithm = dividerView.getSnapAlgorithm();
                int dividerPosition = dividerView.getCurrentPosition();
                DividerSnapAlgorithm.SnapTarget currentTarget = snapAlgorithm.calculateNonDismissingSnapTarget(dividerPosition);
                int increment = shortcutCode == 281474976710727L ? -1 : 1;
                DividerSnapAlgorithm.SnapTarget target = snapAlgorithm.cycleNonDismissTarget(currentTarget, increment);
                dividerView.startDragging(true, false);
                dividerView.stopDragging(target.position, 0.0f, true, true);
            }
        } catch (RemoteException e) {
            Log.e("ShortcutKeyDispatcher", "handleDockKey() failed.");
        }
    }
}
