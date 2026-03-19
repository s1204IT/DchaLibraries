package com.android.server.wm;

import android.R;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.TypedValue;
import com.android.server.wm.DimLayer;
import java.io.PrintWriter;

class DimLayerController {
    private static final float DEFAULT_DIM_AMOUNT_DEAD_WINDOW = 0.5f;
    private static final int DEFAULT_DIM_DURATION = 200;
    private static final String TAG = "WindowManager";
    private static final String TAG_LOCAL = "DimLayerController";
    private DisplayContent mDisplayContent;
    private DimLayer mSharedFullScreenDimLayer;
    private ArrayMap<DimLayer.DimLayerUser, DimLayerState> mState = new ArrayMap<>();
    private Rect mTmpBounds = new Rect();

    DimLayerController(DisplayContent displayContent) {
        this.mDisplayContent = displayContent;
    }

    void updateDimLayer(DimLayer.DimLayerUser dimLayerUser) {
        DimLayer newDimLayer;
        DimLayerState state = getOrCreateDimLayerState(dimLayerUser);
        boolean previousFullscreen = state.dimLayer != null && state.dimLayer == this.mSharedFullScreenDimLayer;
        int displayId = this.mDisplayContent.getDisplayId();
        if (dimLayerUser.dimFullscreen()) {
            if (previousFullscreen && this.mSharedFullScreenDimLayer != null) {
                this.mSharedFullScreenDimLayer.setBoundsForFullscreen();
                return;
            }
            newDimLayer = this.mSharedFullScreenDimLayer;
            if (newDimLayer == null) {
                if (state.dimLayer != null) {
                    newDimLayer = state.dimLayer;
                } else {
                    newDimLayer = new DimLayer(this.mDisplayContent.mService, dimLayerUser, displayId, getDimLayerTag(dimLayerUser));
                }
                dimLayerUser.getDimBounds(this.mTmpBounds);
                newDimLayer.setBounds(this.mTmpBounds);
                this.mSharedFullScreenDimLayer = newDimLayer;
            } else if (state.dimLayer != null) {
                state.dimLayer.destroySurface();
            }
        } else {
            if (state.dimLayer == null || previousFullscreen) {
                newDimLayer = new DimLayer(this.mDisplayContent.mService, dimLayerUser, displayId, getDimLayerTag(dimLayerUser));
            } else {
                newDimLayer = state.dimLayer;
            }
            dimLayerUser.getDimBounds(this.mTmpBounds);
            newDimLayer.setBounds(this.mTmpBounds);
        }
        state.dimLayer = newDimLayer;
    }

    private static String getDimLayerTag(DimLayer.DimLayerUser dimLayerUser) {
        return "DimLayerController/" + dimLayerUser.toShortString();
    }

    private DimLayerState getOrCreateDimLayerState(DimLayer.DimLayerUser dimLayerUser) {
        DimLayerState dimLayerState = null;
        if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
            Slog.v(TAG, "getOrCreateDimLayerState, dimLayerUser=" + dimLayerUser.toShortString());
        }
        DimLayerState state = this.mState.get(dimLayerUser);
        if (state == null) {
            DimLayerState state2 = new DimLayerState(dimLayerState);
            this.mState.put(dimLayerUser, state2);
            return state2;
        }
        return state;
    }

    private void setContinueDimming(DimLayer.DimLayerUser dimLayerUser) {
        DimLayerState state = this.mState.get(dimLayerUser);
        if (state == null) {
            if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
                Slog.w(TAG, "setContinueDimming, no state for: " + dimLayerUser.toShortString());
                return;
            }
            return;
        }
        state.continueDimming = true;
    }

    boolean isDimming() {
        for (int i = this.mState.size() - 1; i >= 0; i--) {
            DimLayerState state = this.mState.valueAt(i);
            if (state.dimLayer != null && state.dimLayer.isDimming()) {
                return true;
            }
        }
        return false;
    }

    void resetDimming() {
        for (int i = this.mState.size() - 1; i >= 0; i--) {
            this.mState.valueAt(i).continueDimming = false;
        }
    }

    private boolean getContinueDimming(DimLayer.DimLayerUser dimLayerUser) {
        DimLayerState state = this.mState.get(dimLayerUser);
        if (state != null) {
            return state.continueDimming;
        }
        return false;
    }

    void startDimmingIfNeeded(DimLayer.DimLayerUser dimLayerUser, WindowStateAnimator newWinAnimator, boolean aboveApp) {
        DimLayerState state = getOrCreateDimLayerState(dimLayerUser);
        state.dimAbove = aboveApp;
        if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
            Slog.v(TAG, "startDimmingIfNeeded, dimLayerUser=" + dimLayerUser.toShortString() + " newWinAnimator=" + newWinAnimator + " state.animator=" + state.animator);
        }
        if (newWinAnimator.getShown()) {
            if (state.animator == null || !state.animator.getShown() || state.animator.mAnimLayer <= newWinAnimator.mAnimLayer) {
                state.animator = newWinAnimator;
                if (state.animator.mWin.mAppToken != null || dimLayerUser.dimFullscreen()) {
                    dimLayerUser.getDimBounds(this.mTmpBounds);
                } else {
                    this.mDisplayContent.getLogicalDisplayRect(this.mTmpBounds);
                }
                state.dimLayer.setBounds(this.mTmpBounds);
            }
        }
    }

    void stopDimmingIfNeeded() {
        if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
            Slog.v(TAG, "stopDimmingIfNeeded, mState.size()=" + this.mState.size());
        }
        for (int i = this.mState.size() - 1; i >= 0; i--) {
            DimLayer.DimLayerUser dimLayerUser = this.mState.keyAt(i);
            stopDimmingIfNeeded(dimLayerUser);
        }
    }

    private void stopDimmingIfNeeded(DimLayer.DimLayerUser dimLayerUser) {
        DimLayerState state = this.mState.get(dimLayerUser);
        if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
            Slog.v(TAG, "stopDimmingIfNeeded, dimLayerUser=" + dimLayerUser.toShortString() + " state.continueDimming=" + state.continueDimming + " state.dimLayer.isDimming=" + state.dimLayer.isDimming());
        }
        if ((state.animator == null || !state.animator.mWin.mWillReplaceWindow) && !state.continueDimming && state.dimLayer.isDimming()) {
            state.animator = null;
            dimLayerUser.getDimBounds(this.mTmpBounds);
            state.dimLayer.setBounds(this.mTmpBounds);
        }
    }

    boolean animateDimLayers() {
        int fullScreen = -1;
        int fullScreenAndDimming = -1;
        boolean result = false;
        for (int i = this.mState.size() - 1; i >= 0; i--) {
            DimLayer.DimLayerUser user = this.mState.keyAt(i);
            DimLayerState state = this.mState.valueAt(i);
            if (user.dimFullscreen() && state.dimLayer == this.mSharedFullScreenDimLayer) {
                fullScreen = i;
                if (this.mState.valueAt(i).continueDimming) {
                    fullScreenAndDimming = i;
                }
            } else {
                result |= animateDimLayers(user);
            }
        }
        if (fullScreenAndDimming != -1) {
            return result | animateDimLayers(this.mState.keyAt(fullScreenAndDimming));
        }
        if (fullScreen != -1) {
            return result | animateDimLayers(this.mState.keyAt(fullScreen));
        }
        return result;
    }

    private boolean animateDimLayers(DimLayer.DimLayerUser dimLayerUser) {
        int dimLayer;
        float dimAmount;
        DimLayerState state = this.mState.get(dimLayerUser);
        if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
            Slog.v(TAG, "animateDimLayers, dimLayerUser=" + dimLayerUser.toShortString() + " state.animator=" + state.animator + " state.continueDimming=" + state.continueDimming);
        }
        if (state.animator == null) {
            dimLayer = state.dimLayer.getLayer();
            dimAmount = 0.0f;
        } else if (state.dimAbove) {
            dimLayer = state.animator.mAnimLayer + 1;
            dimAmount = 0.5f;
        } else {
            dimLayer = state.animator.mAnimLayer - 1;
            dimAmount = state.animator.mWin.mAttrs.dimAmount;
        }
        float targetAlpha = state.dimLayer.getTargetAlpha();
        if (targetAlpha != dimAmount) {
            if (state.animator == null) {
                state.dimLayer.hide(200L);
            } else {
                long duration = (!state.animator.mAnimating || state.animator.mAnimation == null) ? 200L : state.animator.mAnimation.computeDurationHint();
                if (targetAlpha > dimAmount) {
                    duration = getDimLayerFadeDuration(duration);
                }
                state.dimLayer.show(dimLayer, dimAmount, duration);
            }
        } else if (state.dimLayer.getLayer() != dimLayer) {
            state.dimLayer.setLayer(dimLayer);
        }
        if (!state.dimLayer.isAnimating()) {
            return false;
        }
        if (this.mDisplayContent.mService.okToDisplay()) {
            return state.dimLayer.stepAnimation();
        }
        state.dimLayer.show();
        return false;
    }

    boolean isDimming(DimLayer.DimLayerUser dimLayerUser, WindowStateAnimator winAnimator) {
        DimLayerState state = this.mState.get(dimLayerUser);
        if (state == null || state.animator != winAnimator) {
            return false;
        }
        return state.dimLayer.isDimming();
    }

    private long getDimLayerFadeDuration(long duration) {
        TypedValue tv = new TypedValue();
        this.mDisplayContent.mService.mContext.getResources().getValue(R.fraction.config_autoBrightnessAdjustmentMaxGamma, tv, true);
        if (tv.type == 6) {
            return (long) tv.getFraction(duration, duration);
        }
        if (tv.type >= 16 && tv.type <= 31) {
            return tv.data;
        }
        return duration;
    }

    void close() {
        for (int i = this.mState.size() - 1; i >= 0; i--) {
            DimLayerState state = this.mState.valueAt(i);
            state.dimLayer.destroySurface();
        }
        this.mState.clear();
        this.mSharedFullScreenDimLayer = null;
    }

    void removeDimLayerUser(DimLayer.DimLayerUser dimLayerUser) {
        DimLayerState state = this.mState.get(dimLayerUser);
        if (state == null) {
            return;
        }
        if (state.dimLayer != this.mSharedFullScreenDimLayer) {
            state.dimLayer.destroySurface();
        }
        this.mState.remove(dimLayerUser);
    }

    void applyDimBehind(DimLayer.DimLayerUser dimLayerUser, WindowStateAnimator animator) {
        applyDim(dimLayerUser, animator, false);
    }

    void applyDimAbove(DimLayer.DimLayerUser dimLayerUser, WindowStateAnimator animator) {
        applyDim(dimLayerUser, animator, true);
    }

    void applyDim(DimLayer.DimLayerUser dimLayerUser, WindowStateAnimator animator, boolean aboveApp) {
        if (dimLayerUser == null) {
            Slog.e(TAG, "Trying to apply dim layer for: " + this + ", but no dim layer user found.");
            return;
        }
        if (getContinueDimming(dimLayerUser)) {
            return;
        }
        setContinueDimming(dimLayerUser);
        if (isDimming(dimLayerUser, animator)) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_DIM_LAYER) {
            Slog.v(TAG, "Win " + this + " start dimming.");
        }
        startDimmingIfNeeded(dimLayerUser, animator, aboveApp);
    }

    private static class DimLayerState {
        WindowStateAnimator animator;
        boolean continueDimming;
        boolean dimAbove;
        DimLayer dimLayer;

        DimLayerState(DimLayerState dimLayerState) {
            this();
        }

        private DimLayerState() {
        }
    }

    void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG_LOCAL);
        String prefixPlusDoubleSpace = prefix + "  ";
        int n = this.mState.size();
        for (int i = 0; i < n; i++) {
            pw.println(prefixPlusDoubleSpace + this.mState.keyAt(i).toShortString());
            DimLayerState state = this.mState.valueAt(i);
            pw.println(prefixPlusDoubleSpace + "  dimLayer=" + (state.dimLayer == this.mSharedFullScreenDimLayer ? "shared" : state.dimLayer) + ", animator=" + state.animator + ", continueDimming=" + state.continueDimming);
            if (state.dimLayer != null) {
                state.dimLayer.printTo(prefixPlusDoubleSpace + "  ", pw);
            }
        }
    }
}
