package com.android.server.policy;

import android.os.IBinder;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;

public class StatusBarController extends BarController {
    private final WindowManagerInternal.AppTransitionListener mAppTransitionListener;

    public StatusBarController() {
        super("StatusBar", 67108864, 268435456, 1073741824, 1, 67108864, 8);
        this.mAppTransitionListener = new WindowManagerInternal.AppTransitionListener() {
            @Override
            public void onAppTransitionPendingLocked() {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusBarInternal = StatusBarController.this.getStatusBarInternal();
                        if (statusBarInternal != null) {
                            statusBarInternal.appTransitionPending();
                        }
                    }
                });
            }

            @Override
            public int onAppTransitionStartingLocked(int i, IBinder iBinder, IBinder iBinder2, long j, final long j2, final long j3) {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusBarInternal = StatusBarController.this.getStatusBarInternal();
                        if (statusBarInternal != null) {
                            statusBarInternal.appTransitionStarting(j2, j3);
                        }
                    }
                });
                return 0;
            }

            @Override
            public void onAppTransitionCancelledLocked(int i) {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusBarInternal = StatusBarController.this.getStatusBarInternal();
                        if (statusBarInternal != null) {
                            statusBarInternal.appTransitionCancelled();
                        }
                    }
                });
            }

            @Override
            public void onAppTransitionFinishedLocked(IBinder iBinder) {
                StatusBarController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        StatusBarManagerInternal statusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                        if (statusBarManagerInternal != null) {
                            statusBarManagerInternal.appTransitionFinished();
                        }
                    }
                });
            }
        };
    }

    public void setTopAppHidesStatusBar(boolean z) {
        StatusBarManagerInternal statusBarInternal = getStatusBarInternal();
        if (statusBarInternal != null) {
            statusBarInternal.setTopAppHidesStatusBar(z);
        }
    }

    @Override
    protected boolean skipAnimation() {
        return this.mWin.getAttrs().height == -1;
    }

    public WindowManagerInternal.AppTransitionListener getAppTransitionListener() {
        return this.mAppTransitionListener;
    }
}
