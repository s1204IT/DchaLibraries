package com.android.server.wm;

import android.content.res.Configuration;
import android.os.Binder;
import android.util.Slog;
import android.view.Display;
import com.mediatek.server.wm.WmsExt;

public class DisplayWindowController extends WindowContainerController<DisplayContent, WindowContainerListener> {
    private final int mDisplayId;

    public DisplayWindowController(Display display, WindowContainerListener windowContainerListener) {
        super(windowContainerListener, WindowManagerService.getInstance());
        this.mDisplayId = display.getDisplayId();
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mRoot.createDisplayContent(display, this);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    if (this.mContainer == 0) {
                        throw new IllegalArgumentException("Trying to add display=" + display + " dc=" + this.mRoot.getDisplayContent(this.mDisplayId));
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            } catch (Throwable th2) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th2;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public void removeContainer() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                        Slog.i(WmsExt.TAG, "removeDisplay: could not find displayId=" + this.mDisplayId);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((DisplayContent) this.mContainer).removeIfPossible();
                super.removeContainer();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
    }

    public void positionChildAt(StackWindowController stackWindowController, int i) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i(WmsExt.TAG, "positionTaskStackAt: positioning stack=" + stackWindowController + " at " + i);
                }
                if (this.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.i(WmsExt.TAG, "positionTaskStackAt: could not find display=" + this.mContainer);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (stackWindowController.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.i(WmsExt.TAG, "positionTaskStackAt: could not find stack=" + this);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((DisplayContent) this.mContainer).positionStackAt(i, (TaskStack) stackWindowController.mContainer);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void deferUpdateImeTarget() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(this.mDisplayId);
                if (displayContent != null) {
                    displayContent.deferUpdateImeTarget();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void continueUpdateImeTarget() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(this.mDisplayId);
                if (displayContent != null) {
                    displayContent.continueUpdateImeTarget();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public String toString() {
        return "{DisplayWindowController displayId=" + this.mDisplayId + "}";
    }
}
