package com.android.server.wm;

import android.app.RemoteAction;
import android.graphics.Rect;
import java.util.List;

public class PinnedStackWindowController extends StackWindowController {
    private Rect mTmpFromBounds;
    private Rect mTmpToBounds;

    public PinnedStackWindowController(int i, PinnedStackWindowListener pinnedStackWindowListener, int i2, boolean z, Rect rect, WindowManagerService windowManagerService) {
        super(i, pinnedStackWindowListener, i2, z, rect, windowManagerService);
        this.mTmpFromBounds = new Rect();
        this.mTmpToBounds = new Rect();
    }

    public Rect getPictureInPictureBounds(float f, Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mSupportsPictureInPicture && this.mContainer != 0) {
                    DisplayContent displayContent = ((TaskStack) this.mContainer).getDisplayContent();
                    if (displayContent == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    PinnedStackController pinnedStackController = displayContent.getPinnedStackController();
                    if (rect == null) {
                        rect = pinnedStackController.getDefaultOrLastSavedBounds();
                    }
                    if (!pinnedStackController.isValidPictureInPictureAspectRatio(f)) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return rect;
                    }
                    Rect rectTransformBoundsToAspectRatio = pinnedStackController.transformBoundsToAspectRatio(rect, f, true);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return rectTransformBoundsToAspectRatio;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void animateResizePinnedStack(Rect rect, Rect rect2, final int i, final boolean z) {
        final Rect rect3;
        final int i2;
        Rect rect4;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    throw new IllegalArgumentException("Pinned stack container not found :(");
                }
                final Rect rect5 = new Rect();
                ((TaskStack) this.mContainer).getBounds(rect5);
                int i3 = 0;
                final boolean z2 = rect == null;
                if (z2) {
                    if (z) {
                        throw new IllegalArgumentException("Should not defer scheduling PiP mode change on animation to fullscreen.");
                    }
                    this.mService.getStackBounds(1, 1, this.mTmpToBounds);
                    if (!this.mTmpToBounds.isEmpty()) {
                        rect4 = new Rect(this.mTmpToBounds);
                    } else {
                        rect4 = new Rect();
                        ((TaskStack) this.mContainer).getDisplayContent().getBounds(rect4);
                    }
                    rect3 = rect4;
                    i2 = 1;
                } else {
                    if (z) {
                        i3 = 2;
                    }
                    rect3 = rect;
                    i2 = i3;
                }
                ((TaskStack) this.mContainer).setAnimationFinalBounds(rect2, rect3, z2);
                this.mService.mBoundsAnimationController.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        PinnedStackWindowController.lambda$animateResizePinnedStack$0(this.f$0, rect5, rect3, i, i2, z, z2);
                    }
                });
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public static void lambda$animateResizePinnedStack$0(PinnedStackWindowController pinnedStackWindowController, Rect rect, Rect rect2, int i, int i2, boolean z, boolean z2) {
        if (pinnedStackWindowController.mContainer == 0) {
            return;
        }
        pinnedStackWindowController.mService.mBoundsAnimationController.animateBounds((BoundsAnimationTarget) pinnedStackWindowController.mContainer, rect, rect2, i, i2, z, z2);
    }

    public void setPictureInPictureAspectRatio(float f) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mSupportsPictureInPicture && this.mContainer != 0) {
                    PinnedStackController pinnedStackController = ((TaskStack) this.mContainer).getDisplayContent().getPinnedStackController();
                    if (Float.compare(f, pinnedStackController.getAspectRatio()) != 0) {
                        ((TaskStack) this.mContainer).getAnimationOrCurrentBounds(this.mTmpFromBounds);
                        this.mTmpToBounds.set(this.mTmpFromBounds);
                        getPictureInPictureBounds(f, this.mTmpToBounds);
                        if (!this.mTmpToBounds.equals(this.mTmpFromBounds)) {
                            animateResizePinnedStack(this.mTmpToBounds, null, -1, false);
                        }
                        if (!pinnedStackController.isValidPictureInPictureAspectRatio(f)) {
                            f = -1.0f;
                        }
                        pinnedStackController.setAspectRatio(f);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setPictureInPictureActions(List<RemoteAction> list) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mSupportsPictureInPicture && this.mContainer != 0) {
                    ((TaskStack) this.mContainer).getDisplayContent().getPinnedStackController().setActions(list);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean deferScheduleMultiWindowModeChanged() {
        boolean zDeferScheduleMultiWindowModeChanged;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                zDeferScheduleMultiWindowModeChanged = ((TaskStack) this.mContainer).deferScheduleMultiWindowModeChanged();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return zDeferScheduleMultiWindowModeChanged;
    }

    public boolean isAnimatingBoundsToFullscreen() {
        boolean zIsAnimatingBoundsToFullscreen;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                zIsAnimatingBoundsToFullscreen = ((TaskStack) this.mContainer).isAnimatingBoundsToFullscreen();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return zIsAnimatingBoundsToFullscreen;
    }

    public boolean pinnedStackResizeDisallowed() {
        boolean zPinnedStackResizeDisallowed;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                zPinnedStackResizeDisallowed = ((TaskStack) this.mContainer).pinnedStackResizeDisallowed();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return zPinnedStackResizeDisallowed;
    }

    public void updatePictureInPictureModeForPinnedStackAnimation(Rect rect, boolean z) {
        if (this.mListener != 0) {
            ((PinnedStackWindowListener) this.mListener).updatePictureInPictureModeForPinnedStackAnimation(rect, z);
        }
    }
}
