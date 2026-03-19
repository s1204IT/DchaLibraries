package com.android.server.wm;

import android.R;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MagnificationSpec;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.ViewConfiguration;
import android.view.WindowInfo;
import android.view.WindowManager;
import android.view.WindowManagerInternal;
import android.view.WindowManagerPolicy;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.os.SomeArgs;
import com.mediatek.anrmanager.ANRManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AccessibilityController {
    private static final float[] sTempFloats = new float[9];
    private DisplayMagnifier mDisplayMagnifier;
    private final WindowManagerService mWindowManagerService;
    private WindowsForAccessibilityObserver mWindowsForAccessibilityObserver;

    public AccessibilityController(WindowManagerService service) {
        this.mWindowManagerService = service;
    }

    public void setMagnificationCallbacksLocked(WindowManagerInternal.MagnificationCallbacks callbacks) {
        if (callbacks != null) {
            if (this.mDisplayMagnifier != null) {
                throw new IllegalStateException("Magnification callbacks already set!");
            }
            this.mDisplayMagnifier = new DisplayMagnifier(this.mWindowManagerService, callbacks);
        } else {
            if (this.mDisplayMagnifier == null) {
                throw new IllegalStateException("Magnification callbacks already cleared!");
            }
            this.mDisplayMagnifier.destroyLocked();
            this.mDisplayMagnifier = null;
        }
    }

    public void setWindowsForAccessibilityCallback(WindowManagerInternal.WindowsForAccessibilityCallback callback) {
        if (callback != null) {
            if (this.mWindowsForAccessibilityObserver != null) {
                throw new IllegalStateException("Windows for accessibility callback already set!");
            }
            this.mWindowsForAccessibilityObserver = new WindowsForAccessibilityObserver(this.mWindowManagerService, callback);
        } else {
            if (this.mWindowsForAccessibilityObserver == null) {
                throw new IllegalStateException("Windows for accessibility callback already cleared!");
            }
            this.mWindowsForAccessibilityObserver = null;
        }
    }

    public void setMagnificationSpecLocked(MagnificationSpec spec) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.setMagnificationSpecLocked(spec);
        }
        if (this.mWindowsForAccessibilityObserver == null) {
            return;
        }
        this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
    }

    public void getMagnificationRegionLocked(Region outMagnificationRegion) {
        if (this.mDisplayMagnifier == null) {
            return;
        }
        this.mDisplayMagnifier.getMagnificationRegionLocked(outMagnificationRegion);
    }

    public void onRectangleOnScreenRequestedLocked(Rect rectangle) {
        if (this.mDisplayMagnifier == null) {
            return;
        }
        this.mDisplayMagnifier.onRectangleOnScreenRequestedLocked(rectangle);
    }

    public void onWindowLayersChangedLocked() {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onWindowLayersChangedLocked();
        }
        if (this.mWindowsForAccessibilityObserver == null) {
            return;
        }
        this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
    }

    public void onRotationChangedLocked(DisplayContent displayContent, int rotation) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onRotationChangedLocked(displayContent, rotation);
        }
        if (this.mWindowsForAccessibilityObserver == null) {
            return;
        }
        this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
    }

    public void onAppWindowTransitionLocked(WindowState windowState, int transition) {
        if (this.mDisplayMagnifier == null) {
            return;
        }
        this.mDisplayMagnifier.onAppWindowTransitionLocked(windowState, transition);
    }

    public void onWindowTransitionLocked(WindowState windowState, int transition) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onWindowTransitionLocked(windowState, transition);
        }
        if (this.mWindowsForAccessibilityObserver == null) {
            return;
        }
        this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
    }

    public void onWindowFocusChangedNotLocked() {
        WindowsForAccessibilityObserver observer;
        synchronized (this.mWindowManagerService) {
            observer = this.mWindowsForAccessibilityObserver;
        }
        if (observer == null) {
            return;
        }
        observer.performComputeChangedWindowsNotLocked();
    }

    public void onSomeWindowResizedOrMovedLocked() {
        if (this.mWindowsForAccessibilityObserver == null) {
            return;
        }
        this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
    }

    public void drawMagnifiedRegionBorderIfNeededLocked() {
        if (this.mDisplayMagnifier == null) {
            return;
        }
        this.mDisplayMagnifier.drawMagnifiedRegionBorderIfNeededLocked();
    }

    public MagnificationSpec getMagnificationSpecForWindowLocked(WindowState windowState) {
        if (this.mDisplayMagnifier != null) {
            return this.mDisplayMagnifier.getMagnificationSpecForWindowLocked(windowState);
        }
        return null;
    }

    public boolean hasCallbacksLocked() {
        return (this.mDisplayMagnifier == null && this.mWindowsForAccessibilityObserver == null) ? false : true;
    }

    private static void populateTransformationMatrixLocked(WindowState windowState, Matrix outMatrix) {
        sTempFloats[0] = windowState.mWinAnimator.mDsDx;
        sTempFloats[3] = windowState.mWinAnimator.mDtDx;
        sTempFloats[1] = windowState.mWinAnimator.mDsDy;
        sTempFloats[4] = windowState.mWinAnimator.mDtDy;
        sTempFloats[2] = windowState.mShownPosition.x;
        sTempFloats[5] = windowState.mShownPosition.y;
        sTempFloats[6] = 0.0f;
        sTempFloats[7] = 0.0f;
        sTempFloats[8] = 1.0f;
        outMatrix.setValues(sTempFloats);
    }

    private static final class DisplayMagnifier {
        private static final boolean DEBUG_LAYERS = false;
        private static final boolean DEBUG_RECTANGLE_REQUESTED = false;
        private static final boolean DEBUG_ROTATION = false;
        private static final boolean DEBUG_VIEWPORT_WINDOW = false;
        private static final boolean DEBUG_WINDOW_TRANSITIONS = false;
        private static final String LOG_TAG = "WindowManager";
        private final WindowManagerInternal.MagnificationCallbacks mCallbacks;
        private final Context mContext;
        private final Handler mHandler;
        private final long mLongAnimationDuration;
        private final WindowManagerService mWindowManagerService;
        private final Rect mTempRect1 = new Rect();
        private final Rect mTempRect2 = new Rect();
        private final Region mTempRegion1 = new Region();
        private final Region mTempRegion2 = new Region();
        private final Region mTempRegion3 = new Region();
        private final Region mTempRegion4 = new Region();
        private final MagnifiedViewport mMagnifedViewport = new MagnifiedViewport();

        public DisplayMagnifier(WindowManagerService windowManagerService, WindowManagerInternal.MagnificationCallbacks callbacks) {
            this.mContext = windowManagerService.mContext;
            this.mWindowManagerService = windowManagerService;
            this.mCallbacks = callbacks;
            this.mHandler = new MyHandler(this.mWindowManagerService.mH.getLooper());
            this.mLongAnimationDuration = this.mContext.getResources().getInteger(R.integer.config_longAnimTime);
        }

        public void setMagnificationSpecLocked(MagnificationSpec spec) {
            this.mMagnifedViewport.updateMagnificationSpecLocked(spec);
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mWindowManagerService.scheduleAnimationLocked();
        }

        public void onRectangleOnScreenRequestedLocked(Rect rectangle) {
            if (!this.mMagnifedViewport.isMagnifyingLocked()) {
                return;
            }
            Rect magnifiedRegionBounds = this.mTempRect2;
            this.mMagnifedViewport.getMagnifiedFrameInContentCoordsLocked(magnifiedRegionBounds);
            if (magnifiedRegionBounds.contains(rectangle)) {
                return;
            }
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = rectangle.left;
            args.argi2 = rectangle.top;
            args.argi3 = rectangle.right;
            args.argi4 = rectangle.bottom;
            this.mHandler.obtainMessage(2, args).sendToTarget();
        }

        public void onWindowLayersChangedLocked() {
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mWindowManagerService.scheduleAnimationLocked();
        }

        public void onRotationChangedLocked(DisplayContent displayContent, int rotation) {
            this.mMagnifedViewport.onRotationChangedLocked();
            this.mHandler.sendEmptyMessage(4);
        }

        public void onAppWindowTransitionLocked(WindowState windowState, int transition) {
            boolean magnifying = this.mMagnifedViewport.isMagnifyingLocked();
            if (!magnifying) {
            }
            switch (transition) {
                case 6:
                case 8:
                case 10:
                case 12:
                case 13:
                case 14:
                    this.mHandler.sendEmptyMessage(3);
                    break;
            }
        }

        public void onWindowTransitionLocked(WindowState windowState, int transition) {
            boolean magnifying = this.mMagnifedViewport.isMagnifyingLocked();
            int type = windowState.mAttrs.type;
            switch (transition) {
                case 1:
                case 3:
                    if (magnifying) {
                        switch (type) {
                            case 2:
                            case 1000:
                            case ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                            case ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG:
                            case 1003:
                            case 1005:
                            case 2001:
                            case 2002:
                            case 2003:
                            case 2005:
                            case 2006:
                            case 2007:
                            case 2008:
                            case 2009:
                            case 2010:
                            case 2020:
                            case 2024:
                            case 2035:
                                Rect magnifiedRegionBounds = this.mTempRect2;
                                this.mMagnifedViewport.getMagnifiedFrameInContentCoordsLocked(magnifiedRegionBounds);
                                Rect touchableRegionBounds = this.mTempRect1;
                                windowState.getTouchableRegion(this.mTempRegion1);
                                this.mTempRegion1.getBounds(touchableRegionBounds);
                                if (!magnifiedRegionBounds.intersect(touchableRegionBounds)) {
                                    this.mCallbacks.onRectangleOnScreenRequested(touchableRegionBounds.left, touchableRegionBounds.top, touchableRegionBounds.right, touchableRegionBounds.bottom);
                                }
                        }
                    }
                    break;
            }
        }

        public MagnificationSpec getMagnificationSpecForWindowLocked(WindowState windowState) {
            MagnificationSpec spec = this.mMagnifedViewport.getMagnificationSpecLocked();
            if (spec != null && !spec.isNop()) {
                WindowManagerPolicy policy = this.mWindowManagerService.mPolicy;
                int windowType = windowState.mAttrs.type;
                if ((!policy.isTopLevelWindow(windowType) && windowState.mAttachedWindow != null && !policy.canMagnifyWindow(windowType)) || !policy.canMagnifyWindow(windowState.mAttrs.type)) {
                    return null;
                }
            }
            return spec;
        }

        public void getMagnificationRegionLocked(Region outMagnificationRegion) {
            this.mMagnifedViewport.getMagnificationRegionLocked(outMagnificationRegion);
        }

        public void destroyLocked() {
            this.mMagnifedViewport.destroyWindow();
        }

        public void drawMagnifiedRegionBorderIfNeededLocked() {
            this.mMagnifedViewport.drawWindowIfNeededLocked();
        }

        private final class MagnifiedViewport {
            private final float mBorderWidth;
            private final Path mCircularPath;
            private final int mDrawBorderInset;
            private boolean mFullRedrawNeeded;
            private final int mHalfBorderWidth;
            private final ViewportWindow mWindow;
            private final WindowManager mWindowManager;
            private final SparseArray<WindowState> mTempWindowStates = new SparseArray<>();
            private final RectF mTempRectF = new RectF();
            private final Point mTempPoint = new Point();
            private final Matrix mTempMatrix = new Matrix();
            private final Region mMagnificationRegion = new Region();
            private final Region mOldMagnificationRegion = new Region();
            private final MagnificationSpec mMagnificationSpec = MagnificationSpec.obtain();

            public MagnifiedViewport() {
                this.mWindowManager = (WindowManager) DisplayMagnifier.this.mContext.getSystemService("window");
                this.mBorderWidth = DisplayMagnifier.this.mContext.getResources().getDimension(R.dimen.car_button_radius);
                this.mHalfBorderWidth = (int) Math.ceil(this.mBorderWidth / 2.0f);
                this.mDrawBorderInset = ((int) this.mBorderWidth) / 2;
                this.mWindow = new ViewportWindow(DisplayMagnifier.this.mContext);
                if (DisplayMagnifier.this.mContext.getResources().getConfiguration().isScreenRound()) {
                    this.mCircularPath = new Path();
                    this.mWindowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                    int centerXY = this.mTempPoint.x / 2;
                    this.mCircularPath.addCircle(centerXY, centerXY, centerXY, Path.Direction.CW);
                } else {
                    this.mCircularPath = null;
                }
                recomputeBoundsLocked();
            }

            public void getMagnificationRegionLocked(Region outMagnificationRegion) {
                outMagnificationRegion.set(this.mMagnificationRegion);
            }

            public void updateMagnificationSpecLocked(MagnificationSpec spec) {
                if (spec != null) {
                    this.mMagnificationSpec.initialize(spec.scale, spec.offsetX, spec.offsetY);
                } else {
                    this.mMagnificationSpec.clear();
                }
                if (DisplayMagnifier.this.mHandler.hasMessages(5)) {
                    return;
                }
                setMagnifiedRegionBorderShownLocked(isMagnifyingLocked(), true);
            }

            public void recomputeBoundsLocked() {
                this.mWindowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                int screenWidth = this.mTempPoint.x;
                int screenHeight = this.mTempPoint.y;
                this.mMagnificationRegion.set(0, 0, 0, 0);
                Region availableBounds = DisplayMagnifier.this.mTempRegion1;
                availableBounds.set(0, 0, screenWidth, screenHeight);
                if (this.mCircularPath != null) {
                    availableBounds.setPath(this.mCircularPath, availableBounds);
                }
                Region nonMagnifiedBounds = DisplayMagnifier.this.mTempRegion4;
                nonMagnifiedBounds.set(0, 0, 0, 0);
                SparseArray<WindowState> visibleWindows = this.mTempWindowStates;
                visibleWindows.clear();
                populateWindowsOnScreenLocked(visibleWindows);
                int visibleWindowCount = visibleWindows.size();
                for (int i = visibleWindowCount - 1; i >= 0; i--) {
                    WindowState windowState = visibleWindows.valueAt(i);
                    if (windowState.mAttrs.type != 2027) {
                        Matrix matrix = this.mTempMatrix;
                        AccessibilityController.populateTransformationMatrixLocked(windowState, matrix);
                        Region touchableRegion = DisplayMagnifier.this.mTempRegion3;
                        windowState.getTouchableRegion(touchableRegion);
                        Rect touchableFrame = DisplayMagnifier.this.mTempRect1;
                        touchableRegion.getBounds(touchableFrame);
                        RectF windowFrame = this.mTempRectF;
                        windowFrame.set(touchableFrame);
                        windowFrame.offset(-windowState.mFrame.left, -windowState.mFrame.top);
                        matrix.mapRect(windowFrame);
                        Region windowBounds = DisplayMagnifier.this.mTempRegion2;
                        windowBounds.set((int) windowFrame.left, (int) windowFrame.top, (int) windowFrame.right, (int) windowFrame.bottom);
                        Region portionOfWindowAlreadyAccountedFor = DisplayMagnifier.this.mTempRegion3;
                        portionOfWindowAlreadyAccountedFor.set(this.mMagnificationRegion);
                        portionOfWindowAlreadyAccountedFor.op(nonMagnifiedBounds, Region.Op.UNION);
                        windowBounds.op(portionOfWindowAlreadyAccountedFor, Region.Op.DIFFERENCE);
                        if (DisplayMagnifier.this.mWindowManagerService.mPolicy.canMagnifyWindow(windowState.mAttrs.type)) {
                            this.mMagnificationRegion.op(windowBounds, Region.Op.UNION);
                            this.mMagnificationRegion.op(availableBounds, Region.Op.INTERSECT);
                        } else {
                            nonMagnifiedBounds.op(windowBounds, Region.Op.UNION);
                            availableBounds.op(windowBounds, Region.Op.DIFFERENCE);
                        }
                        Region accountedBounds = DisplayMagnifier.this.mTempRegion2;
                        accountedBounds.set(this.mMagnificationRegion);
                        accountedBounds.op(nonMagnifiedBounds, Region.Op.UNION);
                        accountedBounds.op(0, 0, screenWidth, screenHeight, Region.Op.INTERSECT);
                        if (accountedBounds.isRect()) {
                            Rect accountedFrame = DisplayMagnifier.this.mTempRect1;
                            accountedBounds.getBounds(accountedFrame);
                            if (accountedFrame.width() == screenWidth && accountedFrame.height() == screenHeight) {
                                break;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                visibleWindows.clear();
                this.mMagnificationRegion.op(this.mDrawBorderInset, this.mDrawBorderInset, screenWidth - this.mDrawBorderInset, screenHeight - this.mDrawBorderInset, Region.Op.INTERSECT);
                boolean magnifiedChanged = !this.mOldMagnificationRegion.equals(this.mMagnificationRegion);
                if (!magnifiedChanged) {
                    return;
                }
                this.mWindow.setBounds(this.mMagnificationRegion);
                Rect dirtyRect = DisplayMagnifier.this.mTempRect1;
                if (this.mFullRedrawNeeded) {
                    this.mFullRedrawNeeded = false;
                    dirtyRect.set(this.mDrawBorderInset, this.mDrawBorderInset, screenWidth - this.mDrawBorderInset, screenHeight - this.mDrawBorderInset);
                    this.mWindow.invalidate(dirtyRect);
                } else {
                    Region dirtyRegion = DisplayMagnifier.this.mTempRegion3;
                    dirtyRegion.set(this.mMagnificationRegion);
                    dirtyRegion.op(this.mOldMagnificationRegion, Region.Op.UNION);
                    dirtyRegion.op(nonMagnifiedBounds, Region.Op.INTERSECT);
                    dirtyRegion.getBounds(dirtyRect);
                    this.mWindow.invalidate(dirtyRect);
                }
                this.mOldMagnificationRegion.set(this.mMagnificationRegion);
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = Region.obtain(this.mMagnificationRegion);
                DisplayMagnifier.this.mHandler.obtainMessage(1, args).sendToTarget();
            }

            public void onRotationChangedLocked() {
                if (isMagnifyingLocked()) {
                    setMagnifiedRegionBorderShownLocked(false, false);
                    long delay = (long) (DisplayMagnifier.this.mLongAnimationDuration * DisplayMagnifier.this.mWindowManagerService.getWindowAnimationScaleLocked());
                    Message message = DisplayMagnifier.this.mHandler.obtainMessage(5);
                    DisplayMagnifier.this.mHandler.sendMessageDelayed(message, delay);
                }
                recomputeBoundsLocked();
                this.mWindow.updateSize();
            }

            public void setMagnifiedRegionBorderShownLocked(boolean shown, boolean animate) {
                if (shown) {
                    this.mFullRedrawNeeded = true;
                    this.mOldMagnificationRegion.set(0, 0, 0, 0);
                }
                this.mWindow.setShown(shown, animate);
            }

            public void getMagnifiedFrameInContentCoordsLocked(Rect rect) {
                MagnificationSpec spec = this.mMagnificationSpec;
                this.mMagnificationRegion.getBounds(rect);
                rect.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
                rect.scale(1.0f / spec.scale);
            }

            public boolean isMagnifyingLocked() {
                return this.mMagnificationSpec.scale > 1.0f;
            }

            public MagnificationSpec getMagnificationSpecLocked() {
                return this.mMagnificationSpec;
            }

            public void drawWindowIfNeededLocked() {
                recomputeBoundsLocked();
                this.mWindow.drawIfNeeded();
            }

            public void destroyWindow() {
                this.mWindow.releaseSurface();
            }

            private void populateWindowsOnScreenLocked(SparseArray<WindowState> outWindows) {
                DisplayContent displayContent = DisplayMagnifier.this.mWindowManagerService.getDefaultDisplayContentLocked();
                WindowList windowList = displayContent.getWindowList();
                int windowCount = windowList.size();
                for (int i = 0; i < windowCount; i++) {
                    WindowState windowState = windowList.get(i);
                    if (windowState.isOnScreen() && !windowState.mWinAnimator.mEnterAnimationPending) {
                        outWindows.put(windowState.mLayer, windowState);
                    }
                }
            }

            private final class ViewportWindow {
                private static final String SURFACE_TITLE = "Magnification Overlay";
                private int mAlpha;
                private final AnimationController mAnimationController;
                private boolean mInvalidated;
                private boolean mShown;
                private final SurfaceControl mSurfaceControl;
                private final Region mBounds = new Region();
                private final Rect mDirtyRect = new Rect();
                private final Paint mPaint = new Paint();
                private final Surface mSurface = new Surface();

                public ViewportWindow(Context context) {
                    SurfaceControl surfaceControl;
                    try {
                        MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getRealSize(MagnifiedViewport.this.mTempPoint);
                        surfaceControl = new SurfaceControl(DisplayMagnifier.this.mWindowManagerService.mFxSession, SURFACE_TITLE, MagnifiedViewport.this.mTempPoint.x, MagnifiedViewport.this.mTempPoint.y, -3, 4);
                    } catch (Surface.OutOfResourcesException e) {
                        surfaceControl = null;
                    }
                    this.mSurfaceControl = surfaceControl;
                    this.mSurfaceControl.setLayerStack(MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getLayerStack());
                    this.mSurfaceControl.setLayer(DisplayMagnifier.this.mWindowManagerService.mPolicy.windowTypeToLayerLw(2027) * 10000);
                    this.mSurfaceControl.setPosition(0.0f, 0.0f);
                    this.mSurface.copyFrom(this.mSurfaceControl);
                    this.mAnimationController = new AnimationController(context, DisplayMagnifier.this.mWindowManagerService.mH.getLooper());
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(R.attr.colorActivatedHighlight, typedValue, true);
                    int borderColor = context.getColor(typedValue.resourceId);
                    this.mPaint.setStyle(Paint.Style.STROKE);
                    this.mPaint.setStrokeWidth(MagnifiedViewport.this.mBorderWidth);
                    this.mPaint.setColor(borderColor);
                    this.mInvalidated = true;
                }

                public void setShown(boolean shown, boolean animate) {
                    synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                        if (this.mShown == shown) {
                            return;
                        }
                        this.mShown = shown;
                        this.mAnimationController.onFrameShownStateChanged(shown, animate);
                    }
                }

                public int getAlpha() {
                    int i;
                    synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                        i = this.mAlpha;
                    }
                    return i;
                }

                public void setAlpha(int alpha) {
                    synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                        if (this.mAlpha == alpha) {
                            return;
                        }
                        this.mAlpha = alpha;
                        invalidate(null);
                    }
                }

                public void setBounds(Region bounds) {
                    synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                        if (this.mBounds.equals(bounds)) {
                            return;
                        }
                        this.mBounds.set(bounds);
                        invalidate(this.mDirtyRect);
                    }
                }

                public void updateSize() {
                    synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                        MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getRealSize(MagnifiedViewport.this.mTempPoint);
                        this.mSurfaceControl.setSize(MagnifiedViewport.this.mTempPoint.x, MagnifiedViewport.this.mTempPoint.y);
                        invalidate(this.mDirtyRect);
                    }
                }

                public void invalidate(Rect dirtyRect) {
                    if (dirtyRect != null) {
                        this.mDirtyRect.set(dirtyRect);
                    } else {
                        this.mDirtyRect.setEmpty();
                    }
                    this.mInvalidated = true;
                    DisplayMagnifier.this.mWindowManagerService.scheduleAnimationLocked();
                }

                public void drawIfNeeded() {
                    synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                        if (!this.mInvalidated) {
                            return;
                        }
                        this.mInvalidated = false;
                        Canvas canvas = null;
                        try {
                            if (this.mDirtyRect.isEmpty()) {
                                this.mBounds.getBounds(this.mDirtyRect);
                            }
                            this.mDirtyRect.inset(-MagnifiedViewport.this.mHalfBorderWidth, -MagnifiedViewport.this.mHalfBorderWidth);
                            canvas = this.mSurface.lockCanvas(this.mDirtyRect);
                        } catch (Surface.OutOfResourcesException e) {
                        } catch (IllegalArgumentException e2) {
                        }
                        if (canvas == null) {
                            return;
                        }
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        this.mPaint.setAlpha(this.mAlpha);
                        Path path = this.mBounds.getBoundaryPath();
                        canvas.drawPath(path, this.mPaint);
                        this.mSurface.unlockCanvasAndPost(canvas);
                        if (this.mAlpha > 0) {
                            this.mSurfaceControl.show();
                        } else {
                            this.mSurfaceControl.hide();
                        }
                    }
                }

                public void releaseSurface() {
                    this.mSurfaceControl.release();
                    this.mSurface.release();
                }

                private final class AnimationController extends Handler {
                    private static final int MAX_ALPHA = 255;
                    private static final int MIN_ALPHA = 0;
                    private static final int MSG_FRAME_SHOWN_STATE_CHANGED = 1;
                    private static final String PROPERTY_NAME_ALPHA = "alpha";
                    private final ValueAnimator mShowHideFrameAnimator;

                    public AnimationController(Context context, Looper looper) {
                        super(looper);
                        this.mShowHideFrameAnimator = ObjectAnimator.ofInt(ViewportWindow.this, PROPERTY_NAME_ALPHA, 0, 255);
                        Interpolator interpolator = new DecelerateInterpolator(2.5f);
                        long longAnimationDuration = context.getResources().getInteger(R.integer.config_longAnimTime);
                        this.mShowHideFrameAnimator.setInterpolator(interpolator);
                        this.mShowHideFrameAnimator.setDuration(longAnimationDuration);
                    }

                    public void onFrameShownStateChanged(boolean shown, boolean animate) {
                        obtainMessage(1, shown ? 1 : 0, animate ? 1 : 0).sendToTarget();
                    }

                    @Override
                    public void handleMessage(Message message) {
                        switch (message.what) {
                            case 1:
                                boolean shown = message.arg1 == 1;
                                boolean animate = message.arg2 == 1;
                                if (animate) {
                                    if (this.mShowHideFrameAnimator.isRunning()) {
                                        this.mShowHideFrameAnimator.reverse();
                                    } else if (shown) {
                                        this.mShowHideFrameAnimator.start();
                                    } else {
                                        this.mShowHideFrameAnimator.reverse();
                                    }
                                } else {
                                    this.mShowHideFrameAnimator.cancel();
                                    if (shown) {
                                        ViewportWindow.this.setAlpha(255);
                                    } else {
                                        ViewportWindow.this.setAlpha(0);
                                    }
                                }
                                break;
                        }
                    }
                }
            }
        }

        private class MyHandler extends Handler {
            public static final int MESSAGE_NOTIFY_MAGNIFICATION_REGION_CHANGED = 1;
            public static final int MESSAGE_NOTIFY_RECTANGLE_ON_SCREEN_REQUESTED = 2;
            public static final int MESSAGE_NOTIFY_ROTATION_CHANGED = 4;
            public static final int MESSAGE_NOTIFY_USER_CONTEXT_CHANGED = 3;
            public static final int MESSAGE_SHOW_MAGNIFIED_REGION_BOUNDS_IF_NEEDED = 5;

            public MyHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        Region magnifiedBounds = (Region) ((SomeArgs) message.obj).arg1;
                        DisplayMagnifier.this.mCallbacks.onMagnificationRegionChanged(magnifiedBounds);
                        magnifiedBounds.recycle();
                        return;
                    case 2:
                        SomeArgs args = (SomeArgs) message.obj;
                        int left = args.argi1;
                        int top = args.argi2;
                        int right = args.argi3;
                        int bottom = args.argi4;
                        DisplayMagnifier.this.mCallbacks.onRectangleOnScreenRequested(left, top, right, bottom);
                        args.recycle();
                        return;
                    case 3:
                        DisplayMagnifier.this.mCallbacks.onUserContextChanged();
                        return;
                    case 4:
                        int rotation = message.arg1;
                        DisplayMagnifier.this.mCallbacks.onRotationChanged(rotation);
                        return;
                    case 5:
                        synchronized (DisplayMagnifier.this.mWindowManagerService.mWindowMap) {
                            if (DisplayMagnifier.this.mMagnifedViewport.isMagnifyingLocked()) {
                                DisplayMagnifier.this.mMagnifedViewport.setMagnifiedRegionBorderShownLocked(true, true);
                                DisplayMagnifier.this.mWindowManagerService.scheduleAnimationLocked();
                            }
                            break;
                        }
                        return;
                    default:
                        return;
                }
            }
        }
    }

    private static final class WindowsForAccessibilityObserver {
        private static final boolean DEBUG = false;
        private static final String LOG_TAG = "WindowManager";
        private final WindowManagerInternal.WindowsForAccessibilityCallback mCallback;
        private final Context mContext;
        private final Handler mHandler;
        private final WindowManagerService mWindowManagerService;
        private final SparseArray<WindowState> mTempWindowStates = new SparseArray<>();
        private final List<WindowInfo> mOldWindows = new ArrayList();
        private final Set<IBinder> mTempBinderSet = new ArraySet();
        private final RectF mTempRectF = new RectF();
        private final Matrix mTempMatrix = new Matrix();
        private final Point mTempPoint = new Point();
        private final Rect mTempRect = new Rect();
        private final Region mTempRegion = new Region();
        private final Region mTempRegion1 = new Region();
        private final long mRecurringAccessibilityEventsIntervalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();

        public WindowsForAccessibilityObserver(WindowManagerService windowManagerService, WindowManagerInternal.WindowsForAccessibilityCallback callback) {
            this.mContext = windowManagerService.mContext;
            this.mWindowManagerService = windowManagerService;
            this.mCallback = callback;
            this.mHandler = new MyHandler(this.mWindowManagerService.mH.getLooper());
            computeChangedWindows();
        }

        public void performComputeChangedWindowsNotLocked() {
            this.mHandler.removeMessages(1);
            computeChangedWindows();
        }

        public void scheduleComputeChangedWindowsLocked() {
            if (this.mHandler.hasMessages(1)) {
                return;
            }
            this.mHandler.sendEmptyMessageDelayed(1, this.mRecurringAccessibilityEventsIntervalMillis);
        }

        public void computeChangedWindows() {
            boolean windowsChanged = false;
            List<WindowInfo> windows = new ArrayList<>();
            synchronized (this.mWindowManagerService.mWindowMap) {
                if (this.mWindowManagerService.mCurrentFocus == null) {
                    return;
                }
                WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
                windowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                int screenWidth = this.mTempPoint.x;
                int screenHeight = this.mTempPoint.y;
                Region unaccountedSpace = this.mTempRegion;
                unaccountedSpace.set(0, 0, screenWidth, screenHeight);
                SparseArray<WindowState> visibleWindows = this.mTempWindowStates;
                populateVisibleWindowsOnScreenLocked(visibleWindows);
                Set<IBinder> addedWindows = this.mTempBinderSet;
                addedWindows.clear();
                boolean focusedWindowAdded = false;
                int visibleWindowCount = visibleWindows.size();
                HashSet<Integer> skipRemainingWindowsForTasks = new HashSet<>();
                for (int i = visibleWindowCount - 1; i >= 0; i--) {
                    WindowState windowState = visibleWindows.valueAt(i);
                    int flags = windowState.mAttrs.flags;
                    Task task = windowState.getTask();
                    if ((task == null || !skipRemainingWindowsForTasks.contains(Integer.valueOf(task.mTaskId))) && (flags & 16) == 0) {
                        Rect boundsInScreen = this.mTempRect;
                        computeWindowBoundsInScreen(windowState, boundsInScreen);
                        if (!unaccountedSpace.quickReject(boundsInScreen)) {
                            if (isReportedWindowType(windowState.mAttrs.type)) {
                                WindowInfo window = obtainPopulatedWindowInfo(windowState, boundsInScreen);
                                addedWindows.add(window.token);
                                windows.add(window);
                                if (windowState.isFocused()) {
                                    focusedWindowAdded = true;
                                }
                            }
                            if (windowState.mAttrs.type != 2032) {
                                unaccountedSpace.op(boundsInScreen, unaccountedSpace, Region.Op.REVERSE_DIFFERENCE);
                            }
                            if (unaccountedSpace.isEmpty()) {
                                break;
                            }
                            if ((flags & 40) != 0) {
                                continue;
                            } else if (task == null) {
                                break;
                            } else {
                                skipRemainingWindowsForTasks.add(Integer.valueOf(task.mTaskId));
                            }
                        } else {
                            continue;
                        }
                    }
                }
                if (!focusedWindowAdded) {
                    int i2 = visibleWindowCount - 1;
                    while (true) {
                        if (i2 < 0) {
                            break;
                        }
                        WindowState windowState2 = visibleWindows.valueAt(i2);
                        if (windowState2.isFocused()) {
                            Rect boundsInScreen2 = this.mTempRect;
                            computeWindowBoundsInScreen(windowState2, boundsInScreen2);
                            WindowInfo window2 = obtainPopulatedWindowInfo(windowState2, boundsInScreen2);
                            addedWindows.add(window2.token);
                            windows.add(window2);
                            break;
                        }
                        i2--;
                    }
                }
                int windowCount = windows.size();
                for (int i3 = 0; i3 < windowCount; i3++) {
                    WindowInfo window3 = windows.get(i3);
                    if (!addedWindows.contains(window3.parentToken)) {
                        window3.parentToken = null;
                    }
                    if (window3.childTokens != null) {
                        int childTokenCount = window3.childTokens.size();
                        for (int j = childTokenCount - 1; j >= 0; j--) {
                            if (!addedWindows.contains(window3.childTokens.get(j))) {
                                window3.childTokens.remove(j);
                            }
                        }
                    }
                }
                visibleWindows.clear();
                addedWindows.clear();
                if (this.mOldWindows.size() != windows.size()) {
                    windowsChanged = true;
                } else if (!this.mOldWindows.isEmpty() || !windows.isEmpty()) {
                    int i4 = 0;
                    while (true) {
                        if (i4 >= windowCount) {
                            break;
                        }
                        WindowInfo oldWindow = this.mOldWindows.get(i4);
                        WindowInfo newWindow = windows.get(i4);
                        if (!windowChangedNoLayer(oldWindow, newWindow)) {
                            i4++;
                        } else {
                            windowsChanged = true;
                            break;
                        }
                    }
                }
                if (windowsChanged) {
                    cacheWindows(windows);
                }
                if (windowsChanged) {
                    this.mCallback.onWindowsForAccessibilityChanged(windows);
                }
                clearAndRecycleWindows(windows);
            }
        }

        private void computeWindowBoundsInScreen(WindowState windowState, Rect outBounds) {
            Region touchableRegion = this.mTempRegion1;
            windowState.getTouchableRegion(touchableRegion);
            Rect touchableFrame = this.mTempRect;
            touchableRegion.getBounds(touchableFrame);
            RectF windowFrame = this.mTempRectF;
            windowFrame.set(touchableFrame);
            windowFrame.offset(-windowState.mFrame.left, -windowState.mFrame.top);
            Matrix matrix = this.mTempMatrix;
            AccessibilityController.populateTransformationMatrixLocked(windowState, matrix);
            matrix.mapRect(windowFrame);
            outBounds.set((int) windowFrame.left, (int) windowFrame.top, (int) windowFrame.right, (int) windowFrame.bottom);
        }

        private static WindowInfo obtainPopulatedWindowInfo(WindowState windowState, Rect boundsInScreen) {
            WindowInfo window = WindowInfo.obtain();
            window.type = windowState.mAttrs.type;
            window.layer = windowState.mLayer;
            window.token = windowState.mClient.asBinder();
            window.title = windowState.mAttrs.accessibilityTitle;
            window.accessibilityIdOfAnchor = windowState.mAttrs.accessibilityIdOfAnchor;
            WindowState attachedWindow = windowState.mAttachedWindow;
            if (attachedWindow != null) {
                window.parentToken = attachedWindow.mClient.asBinder();
            }
            window.focused = windowState.isFocused();
            window.boundsInScreen.set(boundsInScreen);
            int childCount = windowState.mChildWindows.size();
            if (childCount > 0) {
                if (window.childTokens == null) {
                    window.childTokens = new ArrayList();
                }
                for (int j = 0; j < childCount; j++) {
                    WindowState child = windowState.mChildWindows.get(j);
                    window.childTokens.add(child.mClient.asBinder());
                }
            }
            return window;
        }

        private void cacheWindows(List<WindowInfo> windows) {
            int oldWindowCount = this.mOldWindows.size();
            for (int i = oldWindowCount - 1; i >= 0; i--) {
                this.mOldWindows.remove(i).recycle();
            }
            int newWindowCount = windows.size();
            for (int i2 = 0; i2 < newWindowCount; i2++) {
                WindowInfo newWindow = windows.get(i2);
                this.mOldWindows.add(WindowInfo.obtain(newWindow));
            }
        }

        private boolean windowChangedNoLayer(WindowInfo oldWindow, WindowInfo newWindow) {
            if (oldWindow == newWindow) {
                return false;
            }
            if (oldWindow == null || newWindow == null || oldWindow.type != newWindow.type || oldWindow.focused != newWindow.focused) {
                return true;
            }
            if (oldWindow.token == null) {
                if (newWindow.token != null) {
                    return true;
                }
            } else if (!oldWindow.token.equals(newWindow.token)) {
                return true;
            }
            if (oldWindow.parentToken == null) {
                if (newWindow.parentToken != null) {
                    return true;
                }
            } else if (!oldWindow.parentToken.equals(newWindow.parentToken)) {
                return true;
            }
            if (oldWindow.boundsInScreen.equals(newWindow.boundsInScreen)) {
                return ((oldWindow.childTokens == null || newWindow.childTokens == null || oldWindow.childTokens.equals(newWindow.childTokens)) && TextUtils.equals(oldWindow.title, newWindow.title) && oldWindow.accessibilityIdOfAnchor == newWindow.accessibilityIdOfAnchor) ? false : true;
            }
            return true;
        }

        private static void clearAndRecycleWindows(List<WindowInfo> windows) {
            int windowCount = windows.size();
            for (int i = windowCount - 1; i >= 0; i--) {
                windows.remove(i).recycle();
            }
        }

        private static boolean isReportedWindowType(int windowType) {
            return (windowType == 2029 || windowType == 2013 || windowType == 2021 || windowType == 2026 || windowType == 2016 || windowType == 2022 || windowType == 2018 || windowType == 2027 || windowType == 1004 || windowType == 2015 || windowType == 2030) ? false : true;
        }

        private void populateVisibleWindowsOnScreenLocked(SparseArray<WindowState> outWindows) {
            DisplayContent displayContent = this.mWindowManagerService.getDefaultDisplayContentLocked();
            WindowList windowList = displayContent.getWindowList();
            int windowCount = windowList.size();
            for (int i = 0; i < windowCount; i++) {
                WindowState windowState = windowList.get(i);
                if (windowState.isVisibleLw()) {
                    outWindows.put(windowState.mLayer, windowState);
                }
            }
        }

        private class MyHandler extends Handler {
            public static final int MESSAGE_COMPUTE_CHANGED_WINDOWS = 1;

            public MyHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        WindowsForAccessibilityObserver.this.computeChangedWindows();
                        break;
                }
            }
        }
    }
}
