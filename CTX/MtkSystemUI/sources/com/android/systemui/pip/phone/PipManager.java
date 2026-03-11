package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.pip.BasePipManager;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.InputConsumerController;
import java.io.PrintWriter;

public class PipManager implements BasePipManager {
    private static PipManager sPipController;
    private IActivityManager mActivityManager;
    private PipAppOpsListener mAppOpsListener;
    private Context mContext;
    private InputConsumerController mInputConsumerController;
    private PipMediaController mMediaController;
    private PipMenuActivityController mMenuController;
    private PipTouchHandler mTouchHandler;
    private IWindowManager mWindowManager;
    private Handler mHandler = new Handler();
    private final PinnedStackListener mPinnedStackListener = new PinnedStackListener();
    SysUiTaskStackChangeListener mTaskStackListener = new SysUiTaskStackChangeListener() {
        @Override
        public void onActivityPinned(String str, int i, int i2, int i3) {
            PipManager.this.mTouchHandler.onActivityPinned();
            PipManager.this.mMediaController.onActivityPinned();
            PipManager.this.mMenuController.onActivityPinned();
            PipManager.this.mAppOpsListener.onActivityPinned(str);
            SystemServicesProxy.getInstance(PipManager.this.mContext).setPipVisibility(true);
        }

        @Override
        public void onActivityUnpinned() {
            Pair<ComponentName, Integer> topPinnedActivity = PipUtils.getTopPinnedActivity(PipManager.this.mContext, PipManager.this.mActivityManager);
            ComponentName componentName = (ComponentName) topPinnedActivity.first;
            if (componentName != null) {
                ((Integer) topPinnedActivity.second).intValue();
            }
            PipManager.this.mMenuController.onActivityUnpinned();
            PipManager.this.mTouchHandler.onActivityUnpinned(componentName);
            PipManager.this.mAppOpsListener.onActivityUnpinned();
            SystemServicesProxy.getInstance(PipManager.this.mContext).setPipVisibility(componentName != null);
        }

        @Override
        public void onPinnedStackAnimationStarted() {
            PipManager.this.mTouchHandler.setTouchEnabled(false);
        }

        @Override
        public void onPinnedStackAnimationEnded() {
            PipManager.this.mTouchHandler.setTouchEnabled(true);
            PipManager.this.mTouchHandler.onPinnedStackAnimationEnded();
            PipManager.this.mMenuController.onPinnedStackAnimationEnded();
        }

        @Override
        public void onPinnedActivityRestartAttempt(boolean z) {
            PipManager.this.mTouchHandler.getMotionHelper().expandPip(z);
        }
    };

    class PinnedStackListener extends IPinnedStackListener.Stub {
        private PinnedStackListener() {
        }

        public void onListenerRegistered(final IPinnedStackController iPinnedStackController) {
            PipManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PipManager.this.mTouchHandler.setPinnedStackController(iPinnedStackController);
                }
            });
        }

        public void onImeVisibilityChanged(final boolean z, final int i) {
            PipManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PipManager.this.mTouchHandler.onImeVisibilityChanged(z, i);
                }
            });
        }

        public void onShelfVisibilityChanged(final boolean z, final int i) {
            PipManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PipManager.this.mTouchHandler.onShelfVisibilityChanged(z, i);
                }
            });
        }

        public void onMinimizedStateChanged(final boolean z) {
            PipManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PipManager.this.mTouchHandler.setMinimizedState(z, true);
                }
            });
        }

        public void onMovementBoundsChanged(final Rect rect, final Rect rect2, final Rect rect3, final boolean z, final boolean z2, final int i) {
            PipManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PipManager.this.mTouchHandler.onMovementBoundsChanged(rect, rect2, rect3, z, z2, i);
                }
            });
        }

        public void onActionsChanged(final ParceledListSlice parceledListSlice) {
            PipManager.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PipManager.this.mMenuController.setAppActions(parceledListSlice);
                }
            });
        }
    }

    private PipManager() {
    }

    @Override
    public void initialize(Context context) {
        this.mContext = context;
        this.mActivityManager = ActivityManager.getService();
        this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        try {
            this.mWindowManager.registerPinnedStackListener(0, this.mPinnedStackListener);
        } catch (RemoteException e) {
            Log.e("PipManager", "Failed to register pinned stack listener", e);
        }
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        this.mInputConsumerController = InputConsumerController.getPipInputConsumer();
        this.mInputConsumerController.registerInputConsumer();
        this.mMediaController = new PipMediaController(context, this.mActivityManager);
        this.mMenuController = new PipMenuActivityController(context, this.mActivityManager, this.mMediaController, this.mInputConsumerController);
        this.mTouchHandler = new PipTouchHandler(context, this.mActivityManager, this.mMenuController, this.mInputConsumerController);
        this.mAppOpsListener = new PipAppOpsListener(context, this.mActivityManager, this.mTouchHandler.getMotionHelper());
        EventBus.getDefault().register(this);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        this.mTouchHandler.onConfigurationChanged();
    }

    public final void onBusEvent(ExpandPipEvent expandPipEvent) {
        this.mTouchHandler.getMotionHelper().expandPip(false);
    }

    @Override
    public void showPictureInPictureMenu() {
        this.mTouchHandler.showPictureInPictureMenu();
    }

    public static PipManager getInstance() {
        if (sPipController == null) {
            sPipController = new PipManager();
        }
        return sPipController;
    }

    @Override
    public void dump(PrintWriter printWriter) {
        printWriter.println("PipManager");
        this.mInputConsumerController.dump(printWriter, "  ");
        this.mMenuController.dump(printWriter, "  ");
        this.mTouchHandler.dump(printWriter, "  ");
    }
}
