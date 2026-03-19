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
import android.view.animation.DecelerateInterpolator;
import com.android.internal.os.SomeArgs;
import com.android.server.pm.DumpState;
import com.android.server.wm.AccessibilityController;
import com.android.server.wm.WindowManagerInternal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

final class AccessibilityController {
    private static final float[] sTempFloats = new float[9];
    private DisplayMagnifier mDisplayMagnifier;
    private final WindowManagerService mService;
    private WindowsForAccessibilityObserver mWindowsForAccessibilityObserver;

    public AccessibilityController(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
    }

    public void setMagnificationCallbacksLocked(WindowManagerInternal.MagnificationCallbacks magnificationCallbacks) {
        if (magnificationCallbacks != null) {
            if (this.mDisplayMagnifier != null) {
                throw new IllegalStateException("Magnification callbacks already set!");
            }
            this.mDisplayMagnifier = new DisplayMagnifier(this.mService, magnificationCallbacks);
        } else {
            if (this.mDisplayMagnifier == null) {
                throw new IllegalStateException("Magnification callbacks already cleared!");
            }
            this.mDisplayMagnifier.destroyLocked();
            this.mDisplayMagnifier = null;
        }
    }

    public void setWindowsForAccessibilityCallback(WindowManagerInternal.WindowsForAccessibilityCallback windowsForAccessibilityCallback) {
        if (windowsForAccessibilityCallback != null) {
            if (this.mWindowsForAccessibilityObserver != null) {
                throw new IllegalStateException("Windows for accessibility callback already set!");
            }
            this.mWindowsForAccessibilityObserver = new WindowsForAccessibilityObserver(this.mService, windowsForAccessibilityCallback);
        } else {
            if (this.mWindowsForAccessibilityObserver == null) {
                throw new IllegalStateException("Windows for accessibility callback already cleared!");
            }
            this.mWindowsForAccessibilityObserver = null;
        }
    }

    public void performComputeChangedWindowsNotLocked() {
        WindowsForAccessibilityObserver windowsForAccessibilityObserver;
        synchronized (this.mService) {
            windowsForAccessibilityObserver = this.mWindowsForAccessibilityObserver;
        }
        if (windowsForAccessibilityObserver != null) {
            windowsForAccessibilityObserver.performComputeChangedWindowsNotLocked();
        }
    }

    public void setMagnificationSpecLocked(MagnificationSpec magnificationSpec) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.setMagnificationSpecLocked(magnificationSpec);
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void getMagnificationRegionLocked(Region region) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.getMagnificationRegionLocked(region);
        }
    }

    public void onRectangleOnScreenRequestedLocked(Rect rect) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onRectangleOnScreenRequestedLocked(rect);
        }
    }

    public void onWindowLayersChangedLocked() {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onWindowLayersChangedLocked();
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void onRotationChangedLocked(DisplayContent displayContent) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onRotationChangedLocked(displayContent);
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void onAppWindowTransitionLocked(WindowState windowState, int i) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onAppWindowTransitionLocked(windowState, i);
        }
    }

    public void onWindowTransitionLocked(WindowState windowState, int i) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.onWindowTransitionLocked(windowState, i);
        }
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void onWindowFocusChangedNotLocked() {
        WindowsForAccessibilityObserver windowsForAccessibilityObserver;
        synchronized (this.mService) {
            windowsForAccessibilityObserver = this.mWindowsForAccessibilityObserver;
        }
        if (windowsForAccessibilityObserver != null) {
            windowsForAccessibilityObserver.performComputeChangedWindowsNotLocked();
        }
    }

    public void onSomeWindowResizedOrMovedLocked() {
        if (this.mWindowsForAccessibilityObserver != null) {
            this.mWindowsForAccessibilityObserver.scheduleComputeChangedWindowsLocked();
        }
    }

    public void drawMagnifiedRegionBorderIfNeededLocked() {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.drawMagnifiedRegionBorderIfNeededLocked();
        }
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

    public void setForceShowMagnifiableBoundsLocked(boolean z) {
        if (this.mDisplayMagnifier != null) {
            this.mDisplayMagnifier.setForceShowMagnifiableBoundsLocked(z);
            this.mDisplayMagnifier.showMagnificationBoundsIfNeeded();
        }
    }

    private static void populateTransformationMatrixLocked(WindowState windowState, Matrix matrix) {
        windowState.getTransformationMatrix(sTempFloats, matrix);
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
        private final WindowManagerService mService;
        private final Rect mTempRect1 = new Rect();
        private final Rect mTempRect2 = new Rect();
        private final Region mTempRegion1 = new Region();
        private final Region mTempRegion2 = new Region();
        private final Region mTempRegion3 = new Region();
        private final Region mTempRegion4 = new Region();
        private boolean mForceShowMagnifiableBounds = false;
        private final MagnifiedViewport mMagnifedViewport = new MagnifiedViewport();

        public DisplayMagnifier(WindowManagerService windowManagerService, WindowManagerInternal.MagnificationCallbacks magnificationCallbacks) {
            this.mContext = windowManagerService.mContext;
            this.mService = windowManagerService;
            this.mCallbacks = magnificationCallbacks;
            this.mHandler = new MyHandler(this.mService.mH.getLooper());
            this.mLongAnimationDuration = this.mContext.getResources().getInteger(R.integer.config_longAnimTime);
        }

        public void setMagnificationSpecLocked(MagnificationSpec magnificationSpec) {
            this.mMagnifedViewport.updateMagnificationSpecLocked(magnificationSpec);
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mService.applyMagnificationSpec(magnificationSpec);
            this.mService.scheduleAnimationLocked();
        }

        public void setForceShowMagnifiableBoundsLocked(boolean z) {
            this.mForceShowMagnifiableBounds = z;
            this.mMagnifedViewport.setMagnifiedRegionBorderShownLocked(z, true);
        }

        public boolean isForceShowingMagnifiableBoundsLocked() {
            return this.mForceShowMagnifiableBounds;
        }

        public void onRectangleOnScreenRequestedLocked(Rect rect) {
            if (!this.mMagnifedViewport.isMagnifyingLocked()) {
                return;
            }
            Rect rect2 = this.mTempRect2;
            this.mMagnifedViewport.getMagnifiedFrameInContentCoordsLocked(rect2);
            if (rect2.contains(rect)) {
                return;
            }
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.argi1 = rect.left;
            someArgsObtain.argi2 = rect.top;
            someArgsObtain.argi3 = rect.right;
            someArgsObtain.argi4 = rect.bottom;
            this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
        }

        public void onWindowLayersChangedLocked() {
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mService.scheduleAnimationLocked();
        }

        public void onRotationChangedLocked(DisplayContent displayContent) {
            this.mMagnifedViewport.onRotationChangedLocked();
            this.mHandler.sendEmptyMessage(4);
        }

        public void onAppWindowTransitionLocked(WindowState windowState, int i) {
            if (this.mMagnifedViewport.isMagnifyingLocked()) {
                if (i != 6 && i != 8 && i != 10) {
                    switch (i) {
                    }
                }
                this.mHandler.sendEmptyMessage(3);
            }
        }

        public void onWindowTransitionLocked(com.android.server.wm.WindowState r4, int r5) {
            r0 = r3.mMagnifedViewport.isMagnifyingLocked();
            r1 = r4.mAttrs.type;
            if ((r5 == 1 || r5 == 3) && r0) {
                if (r1 != 2 && r1 != 4 && r1 != 1005 && r1 != 2020 && r1 != 2024 && r1 != 2035 && r1 != 2038) {
                    switch (r1) {
                        case 1000:
                        case com.android.server.connectivity.NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE:
                        case 1002:
                        case 1003:
                        default:
                            switch (r1) {
                                case 2001:
                                case 2002:
                                case 2003:
                                    break;
                                default:
                                    switch (r1) {
                                    }
                                    return;
                            }
                    }
                }
                r5 = r3.mTempRect2;
                r3.mMagnifedViewport.getMagnifiedFrameInContentCoordsLocked(r5);
                r0 = r3.mTempRect1;
                r4.getTouchableRegion(r3.mTempRegion1);
                r3.mTempRegion1.getBounds(r0);
                if (!r5.intersect(r0)) {
                    r3.mCallbacks.onRectangleOnScreenRequested(r0.left, r0.top, r0.right, r0.bottom);
                    return;
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        public MagnificationSpec getMagnificationSpecForWindowLocked(WindowState windowState) {
            MagnificationSpec magnificationSpecLocked = this.mMagnifedViewport.getMagnificationSpecLocked();
            if (magnificationSpecLocked != null && !magnificationSpecLocked.isNop() && !windowState.shouldMagnify()) {
                return null;
            }
            return magnificationSpecLocked;
        }

        public void getMagnificationRegionLocked(Region region) {
            this.mMagnifedViewport.recomputeBoundsLocked();
            this.mMagnifedViewport.getMagnificationRegionLocked(region);
        }

        public void destroyLocked() {
            this.mMagnifedViewport.destroyWindow();
        }

        public void showMagnificationBoundsIfNeeded() {
            this.mHandler.obtainMessage(5).sendToTarget();
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
            private int mTempLayer = 0;

            public MagnifiedViewport() {
                this.mWindowManager = (WindowManager) DisplayMagnifier.this.mContext.getSystemService("window");
                this.mBorderWidth = DisplayMagnifier.this.mContext.getResources().getDimension(R.dimen.config_restrictedIconSize);
                this.mHalfBorderWidth = (int) Math.ceil(this.mBorderWidth / 2.0f);
                this.mDrawBorderInset = ((int) this.mBorderWidth) / 2;
                this.mWindow = new ViewportWindow(DisplayMagnifier.this.mContext);
                if (DisplayMagnifier.this.mContext.getResources().getConfiguration().isScreenRound()) {
                    this.mCircularPath = new Path();
                    this.mWindowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                    float f = this.mTempPoint.x / 2;
                    this.mCircularPath.addCircle(f, f, f, Path.Direction.CW);
                } else {
                    this.mCircularPath = null;
                }
                recomputeBoundsLocked();
            }

            public void getMagnificationRegionLocked(Region region) {
                region.set(this.mMagnificationRegion);
            }

            public void updateMagnificationSpecLocked(MagnificationSpec magnificationSpec) {
                if (magnificationSpec != null) {
                    this.mMagnificationSpec.initialize(magnificationSpec.scale, magnificationSpec.offsetX, magnificationSpec.offsetY);
                } else {
                    this.mMagnificationSpec.clear();
                }
                if (!DisplayMagnifier.this.mHandler.hasMessages(5)) {
                    setMagnifiedRegionBorderShownLocked(isMagnifyingLocked() || DisplayMagnifier.this.isForceShowingMagnifiableBoundsLocked(), true);
                }
            }

            public void recomputeBoundsLocked() {
                this.mWindowManager.getDefaultDisplay().getRealSize(this.mTempPoint);
                int i = this.mTempPoint.x;
                int i2 = this.mTempPoint.y;
                this.mMagnificationRegion.set(0, 0, 0, 0);
                Region region = DisplayMagnifier.this.mTempRegion1;
                region.set(0, 0, i, i2);
                if (this.mCircularPath != null) {
                    region.setPath(this.mCircularPath, region);
                }
                Region region2 = DisplayMagnifier.this.mTempRegion4;
                region2.set(0, 0, 0, 0);
                SparseArray<WindowState> sparseArray = this.mTempWindowStates;
                sparseArray.clear();
                populateWindowsOnScreenLocked(sparseArray);
                for (int size = sparseArray.size() - 1; size >= 0; size--) {
                    WindowState windowStateValueAt = sparseArray.valueAt(size);
                    if (windowStateValueAt.mAttrs.type != 2027 && (windowStateValueAt.mAttrs.privateFlags & DumpState.DUMP_DEXOPT) == 0) {
                        Matrix matrix = this.mTempMatrix;
                        AccessibilityController.populateTransformationMatrixLocked(windowStateValueAt, matrix);
                        Region region3 = DisplayMagnifier.this.mTempRegion3;
                        windowStateValueAt.getTouchableRegion(region3);
                        Rect rect = DisplayMagnifier.this.mTempRect1;
                        region3.getBounds(rect);
                        RectF rectF = this.mTempRectF;
                        rectF.set(rect);
                        rectF.offset(-windowStateValueAt.mFrame.left, -windowStateValueAt.mFrame.top);
                        matrix.mapRect(rectF);
                        Region region4 = DisplayMagnifier.this.mTempRegion2;
                        region4.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
                        Region region5 = DisplayMagnifier.this.mTempRegion3;
                        region5.set(this.mMagnificationRegion);
                        region5.op(region2, Region.Op.UNION);
                        region4.op(region5, Region.Op.DIFFERENCE);
                        if (windowStateValueAt.shouldMagnify()) {
                            this.mMagnificationRegion.op(region4, Region.Op.UNION);
                            this.mMagnificationRegion.op(region, Region.Op.INTERSECT);
                        } else {
                            region2.op(region4, Region.Op.UNION);
                            region.op(region4, Region.Op.DIFFERENCE);
                        }
                        Region region6 = DisplayMagnifier.this.mTempRegion2;
                        region6.set(this.mMagnificationRegion);
                        region6.op(region2, Region.Op.UNION);
                        region6.op(0, 0, i, i2, Region.Op.INTERSECT);
                        if (region6.isRect()) {
                            Rect rect2 = DisplayMagnifier.this.mTempRect1;
                            region6.getBounds(rect2);
                            if (rect2.width() == i && rect2.height() == i2) {
                                break;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                sparseArray.clear();
                this.mMagnificationRegion.op(this.mDrawBorderInset, this.mDrawBorderInset, i - this.mDrawBorderInset, i2 - this.mDrawBorderInset, Region.Op.INTERSECT);
                if (!this.mOldMagnificationRegion.equals(this.mMagnificationRegion)) {
                    this.mWindow.setBounds(this.mMagnificationRegion);
                    Rect rect3 = DisplayMagnifier.this.mTempRect1;
                    if (!this.mFullRedrawNeeded) {
                        Region region7 = DisplayMagnifier.this.mTempRegion3;
                        region7.set(this.mMagnificationRegion);
                        region7.op(this.mOldMagnificationRegion, Region.Op.UNION);
                        region7.op(region2, Region.Op.INTERSECT);
                        region7.getBounds(rect3);
                        this.mWindow.invalidate(rect3);
                    } else {
                        this.mFullRedrawNeeded = false;
                        rect3.set(this.mDrawBorderInset, this.mDrawBorderInset, i - this.mDrawBorderInset, i2 - this.mDrawBorderInset);
                        this.mWindow.invalidate(rect3);
                    }
                    this.mOldMagnificationRegion.set(this.mMagnificationRegion);
                    SomeArgs someArgsObtain = SomeArgs.obtain();
                    someArgsObtain.arg1 = Region.obtain(this.mMagnificationRegion);
                    DisplayMagnifier.this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
                }
            }

            public void onRotationChangedLocked() {
                if (isMagnifyingLocked() || DisplayMagnifier.this.isForceShowingMagnifiableBoundsLocked()) {
                    setMagnifiedRegionBorderShownLocked(false, false);
                    DisplayMagnifier.this.mHandler.sendMessageDelayed(DisplayMagnifier.this.mHandler.obtainMessage(5), (long) (DisplayMagnifier.this.mLongAnimationDuration * DisplayMagnifier.this.mService.getWindowAnimationScaleLocked()));
                }
                recomputeBoundsLocked();
                this.mWindow.updateSize();
            }

            public void setMagnifiedRegionBorderShownLocked(boolean z, boolean z2) {
                if (z) {
                    this.mFullRedrawNeeded = true;
                    this.mOldMagnificationRegion.set(0, 0, 0, 0);
                }
                this.mWindow.setShown(z, z2);
            }

            public void getMagnifiedFrameInContentCoordsLocked(Rect rect) {
                MagnificationSpec magnificationSpec = this.mMagnificationSpec;
                this.mMagnificationRegion.getBounds(rect);
                rect.offset((int) (-magnificationSpec.offsetX), (int) (-magnificationSpec.offsetY));
                rect.scale(1.0f / magnificationSpec.scale);
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

            private void populateWindowsOnScreenLocked(final SparseArray<WindowState> sparseArray) {
                DisplayContent defaultDisplayContentLocked = DisplayMagnifier.this.mService.getDefaultDisplayContentLocked();
                this.mTempLayer = 0;
                defaultDisplayContentLocked.forAllWindows(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        AccessibilityController.DisplayMagnifier.MagnifiedViewport.lambda$populateWindowsOnScreenLocked$0(this.f$0, sparseArray, (WindowState) obj);
                    }
                }, false);
            }

            public static void lambda$populateWindowsOnScreenLocked$0(MagnifiedViewport magnifiedViewport, SparseArray sparseArray, WindowState windowState) {
                if (windowState.isOnScreen() && windowState.isVisibleLw() && windowState.mAttrs.alpha != 0.0f && !windowState.mWinAnimator.mEnterAnimationPending) {
                    magnifiedViewport.mTempLayer++;
                    sparseArray.put(magnifiedViewport.mTempLayer, windowState);
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
                    SurfaceControl surfaceControlBuild;
                    try {
                        MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getRealSize(MagnifiedViewport.this.mTempPoint);
                        surfaceControlBuild = DisplayMagnifier.this.mService.getDefaultDisplayContentLocked().makeOverlay().setName(SURFACE_TITLE).setSize(MagnifiedViewport.this.mTempPoint.x, MagnifiedViewport.this.mTempPoint.y).setFormat(-3).build();
                    } catch (Surface.OutOfResourcesException e) {
                        surfaceControlBuild = null;
                    }
                    this.mSurfaceControl = surfaceControlBuild;
                    this.mSurfaceControl.setLayer(DisplayMagnifier.this.mService.mPolicy.getWindowLayerFromTypeLw(2027) * 10000);
                    this.mSurfaceControl.setPosition(0.0f, 0.0f);
                    this.mSurface.copyFrom(this.mSurfaceControl);
                    this.mAnimationController = new AnimationController(context, DisplayMagnifier.this.mService.mH.getLooper());
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(R.attr.colorActivatedHighlight, typedValue, true);
                    int color = context.getColor(typedValue.resourceId);
                    this.mPaint.setStyle(Paint.Style.STROKE);
                    this.mPaint.setStrokeWidth(MagnifiedViewport.this.mBorderWidth);
                    this.mPaint.setColor(color);
                    this.mInvalidated = true;
                }

                public void setShown(boolean z, boolean z2) {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mShown == z) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mShown = z;
                            this.mAnimationController.onFrameShownStateChanged(z, z2);
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                public int getAlpha() {
                    int i;
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            i = this.mAlpha;
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return i;
                }

                public void setAlpha(int i) {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mAlpha == i) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mAlpha = i;
                            invalidate(null);
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                public void setBounds(Region region) {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mBounds.equals(region)) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mBounds.set(region);
                            invalidate(this.mDirtyRect);
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                public void updateSize() {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            MagnifiedViewport.this.mWindowManager.getDefaultDisplay().getRealSize(MagnifiedViewport.this.mTempPoint);
                            this.mSurfaceControl.setSize(MagnifiedViewport.this.mTempPoint.x, MagnifiedViewport.this.mTempPoint.y);
                            invalidate(this.mDirtyRect);
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }

                public void invalidate(Rect rect) {
                    if (rect != null) {
                        this.mDirtyRect.set(rect);
                    } else {
                        this.mDirtyRect.setEmpty();
                    }
                    this.mInvalidated = true;
                    DisplayMagnifier.this.mService.scheduleAnimationLocked();
                }

                public void drawIfNeeded() {
                    synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (!this.mInvalidated) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mInvalidated = false;
                            if (this.mAlpha > 0) {
                                Canvas canvasLockCanvas = null;
                                try {
                                    if (this.mDirtyRect.isEmpty()) {
                                        this.mBounds.getBounds(this.mDirtyRect);
                                    }
                                    this.mDirtyRect.inset(-MagnifiedViewport.this.mHalfBorderWidth, -MagnifiedViewport.this.mHalfBorderWidth);
                                    canvasLockCanvas = this.mSurface.lockCanvas(this.mDirtyRect);
                                } catch (Surface.OutOfResourcesException e) {
                                } catch (IllegalArgumentException e2) {
                                }
                                if (canvasLockCanvas == null) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                                canvasLockCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                                this.mPaint.setAlpha(this.mAlpha);
                                canvasLockCanvas.drawPath(this.mBounds.getBoundaryPath(), this.mPaint);
                                this.mSurface.unlockCanvasAndPost(canvasLockCanvas);
                                this.mSurfaceControl.show();
                            } else {
                                this.mSurfaceControl.hide();
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
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
                        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(2.5f);
                        long integer = context.getResources().getInteger(R.integer.config_longAnimTime);
                        this.mShowHideFrameAnimator.setInterpolator(decelerateInterpolator);
                        this.mShowHideFrameAnimator.setDuration(integer);
                    }

                    public void onFrameShownStateChanged(boolean z, boolean z2) {
                        obtainMessage(1, z ? 1 : 0, z2 ? 1 : 0).sendToTarget();
                    }

                    @Override
                    public void handleMessage(Message message) {
                        if (message.what == 1) {
                            boolean z = message.arg1 == 1;
                            if (message.arg2 == 1) {
                                if (this.mShowHideFrameAnimator.isRunning()) {
                                    this.mShowHideFrameAnimator.reverse();
                                    return;
                                } else if (z) {
                                    this.mShowHideFrameAnimator.start();
                                    return;
                                } else {
                                    this.mShowHideFrameAnimator.reverse();
                                    return;
                                }
                            }
                            this.mShowHideFrameAnimator.cancel();
                            if (z) {
                                ViewportWindow.this.setAlpha(255);
                            } else {
                                ViewportWindow.this.setAlpha(0);
                            }
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
                        Region region = (Region) ((SomeArgs) message.obj).arg1;
                        DisplayMagnifier.this.mCallbacks.onMagnificationRegionChanged(region);
                        region.recycle();
                        return;
                    case 2:
                        SomeArgs someArgs = (SomeArgs) message.obj;
                        DisplayMagnifier.this.mCallbacks.onRectangleOnScreenRequested(someArgs.argi1, someArgs.argi2, someArgs.argi3, someArgs.argi4);
                        someArgs.recycle();
                        return;
                    case 3:
                        DisplayMagnifier.this.mCallbacks.onUserContextChanged();
                        return;
                    case 4:
                        DisplayMagnifier.this.mCallbacks.onRotationChanged(message.arg1);
                        return;
                    case 5:
                        synchronized (DisplayMagnifier.this.mService.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                if (DisplayMagnifier.this.mMagnifedViewport.isMagnifyingLocked() || DisplayMagnifier.this.isForceShowingMagnifiableBoundsLocked()) {
                                    DisplayMagnifier.this.mMagnifedViewport.setMagnifiedRegionBorderShownLocked(true, true);
                                    DisplayMagnifier.this.mService.scheduleAnimationLocked();
                                }
                            } catch (Throwable th) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                            break;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
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
        private final WindowManagerService mService;
        private final SparseArray<WindowState> mTempWindowStates = new SparseArray<>();
        private final List<WindowInfo> mOldWindows = new ArrayList();
        private final Set<IBinder> mTempBinderSet = new ArraySet();
        private final RectF mTempRectF = new RectF();
        private final Matrix mTempMatrix = new Matrix();
        private final Point mTempPoint = new Point();
        private final Rect mTempRect = new Rect();
        private final Region mTempRegion = new Region();
        private final Region mTempRegion1 = new Region();
        private int mTempLayer = 0;
        private final long mRecurringAccessibilityEventsIntervalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();

        public WindowsForAccessibilityObserver(WindowManagerService windowManagerService, WindowManagerInternal.WindowsForAccessibilityCallback windowsForAccessibilityCallback) {
            this.mContext = windowManagerService.mContext;
            this.mService = windowManagerService;
            this.mCallback = windowsForAccessibilityCallback;
            this.mHandler = new MyHandler(this.mService.mH.getLooper());
            computeChangedWindows();
        }

        public void performComputeChangedWindowsNotLocked() {
            this.mHandler.removeMessages(1);
            computeChangedWindows();
        }

        public void scheduleComputeChangedWindowsLocked() {
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, this.mRecurringAccessibilityEventsIntervalMillis);
            }
        }

        public void computeChangedWindows() {
            ArrayList arrayList = new ArrayList();
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mService.mCurrentFocus == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRealSize(this.mTempPoint);
                    int i = this.mTempPoint.x;
                    int i2 = this.mTempPoint.y;
                    Region region = this.mTempRegion;
                    region.set(0, 0, i, i2);
                    SparseArray<WindowState> sparseArray = this.mTempWindowStates;
                    populateVisibleWindowsOnScreenLocked(sparseArray);
                    Set<IBinder> set = this.mTempBinderSet;
                    set.clear();
                    int size = sparseArray.size();
                    HashSet hashSet = new HashSet();
                    boolean z = true;
                    int i3 = size - 1;
                    boolean z2 = false;
                    int i4 = i3;
                    while (i4 >= 0) {
                        WindowState windowStateValueAt = sparseArray.valueAt(i4);
                        int i5 = windowStateValueAt.mAttrs.flags;
                        Task task = windowStateValueAt.getTask();
                        if ((task == null || !hashSet.contains(Integer.valueOf(task.mTaskId))) && ((i5 & 16) == 0 || windowStateValueAt.mAttrs.type == 2034)) {
                            Rect rect = this.mTempRect;
                            computeWindowBoundsInScreen(windowStateValueAt, rect);
                            if (!region.quickReject(rect)) {
                                if (isReportedWindowType(windowStateValueAt.mAttrs.type)) {
                                    addPopulatedWindowInfo(windowStateValueAt, rect, arrayList, set);
                                    if (windowStateValueAt.isFocused()) {
                                        z2 = z;
                                    }
                                }
                                if (windowStateValueAt.mAttrs.type != 2032) {
                                    region.op(rect, region, Region.Op.REVERSE_DIFFERENCE);
                                    if ((i5 & 40) == 0) {
                                        region.op(windowStateValueAt.getDisplayFrameLw(), region, Region.Op.REVERSE_DIFFERENCE);
                                        if (task == null) {
                                            break;
                                        } else {
                                            hashSet.add(Integer.valueOf(task.mTaskId));
                                        }
                                    }
                                }
                                if (region.isEmpty()) {
                                    break;
                                }
                            }
                        }
                        i4--;
                        z = true;
                    }
                    if (!z2) {
                        while (true) {
                            if (i3 < 0) {
                                break;
                            }
                            WindowState windowStateValueAt2 = sparseArray.valueAt(i3);
                            if (windowStateValueAt2.isFocused()) {
                                Rect rect2 = this.mTempRect;
                                computeWindowBoundsInScreen(windowStateValueAt2, rect2);
                                addPopulatedWindowInfo(windowStateValueAt2, rect2, arrayList, set);
                                break;
                            }
                            i3--;
                        }
                    }
                    int size2 = arrayList.size();
                    for (int i6 = 0; i6 < size2; i6++) {
                        WindowInfo windowInfo = arrayList.get(i6);
                        if (!set.contains(windowInfo.parentToken)) {
                            windowInfo.parentToken = null;
                        }
                        if (windowInfo.childTokens != null) {
                            for (int size3 = windowInfo.childTokens.size() - 1; size3 >= 0; size3--) {
                                if (!set.contains(windowInfo.childTokens.get(size3))) {
                                    windowInfo.childTokens.remove(size3);
                                }
                            }
                        }
                    }
                    boolean z3 = true;
                    sparseArray.clear();
                    set.clear();
                    if (this.mOldWindows.size() == arrayList.size()) {
                        if (!this.mOldWindows.isEmpty() || !arrayList.isEmpty()) {
                            for (int i7 = 0; i7 < size2; i7++) {
                                if (windowChangedNoLayer(this.mOldWindows.get(i7), arrayList.get(i7))) {
                                    break;
                                }
                            }
                            z3 = false;
                        } else {
                            z3 = false;
                        }
                    }
                    if (z3) {
                        cacheWindows(arrayList);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (z3) {
                        this.mCallback.onWindowsForAccessibilityChanged(arrayList);
                    }
                    clearAndRecycleWindows(arrayList);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        private void computeWindowBoundsInScreen(WindowState windowState, Rect rect) {
            Region region = this.mTempRegion1;
            windowState.getTouchableRegion(region);
            Rect rect2 = this.mTempRect;
            region.getBounds(rect2);
            RectF rectF = this.mTempRectF;
            rectF.set(rect2);
            rectF.offset(-windowState.mFrame.left, -windowState.mFrame.top);
            Matrix matrix = this.mTempMatrix;
            AccessibilityController.populateTransformationMatrixLocked(windowState, matrix);
            matrix.mapRect(rectF);
            rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
        }

        private static void addPopulatedWindowInfo(WindowState windowState, Rect rect, List<WindowInfo> list, Set<IBinder> set) {
            WindowInfo windowInfo = windowState.getWindowInfo();
            windowInfo.boundsInScreen.set(rect);
            windowInfo.layer = set.size();
            list.add(windowInfo);
            set.add(windowInfo.token);
        }

        private void cacheWindows(List<WindowInfo> list) {
            for (int size = this.mOldWindows.size() - 1; size >= 0; size--) {
                this.mOldWindows.remove(size).recycle();
            }
            int size2 = list.size();
            for (int i = 0; i < size2; i++) {
                this.mOldWindows.add(WindowInfo.obtain(list.get(i)));
            }
        }

        private boolean windowChangedNoLayer(WindowInfo windowInfo, WindowInfo windowInfo2) {
            if (windowInfo == windowInfo2) {
                return false;
            }
            if (windowInfo == null || windowInfo2 == null || windowInfo.type != windowInfo2.type || windowInfo.focused != windowInfo2.focused) {
                return true;
            }
            if (windowInfo.token == null) {
                if (windowInfo2.token != null) {
                    return true;
                }
            } else if (!windowInfo.token.equals(windowInfo2.token)) {
                return true;
            }
            if (windowInfo.parentToken == null) {
                if (windowInfo2.parentToken != null) {
                    return true;
                }
            } else if (!windowInfo.parentToken.equals(windowInfo2.parentToken)) {
                return true;
            }
            if (!windowInfo.boundsInScreen.equals(windowInfo2.boundsInScreen)) {
                return true;
            }
            if ((windowInfo.childTokens == null || windowInfo2.childTokens == null || windowInfo.childTokens.equals(windowInfo2.childTokens)) && TextUtils.equals(windowInfo.title, windowInfo2.title) && windowInfo.accessibilityIdOfAnchor == windowInfo2.accessibilityIdOfAnchor) {
                return false;
            }
            return true;
        }

        private static void clearAndRecycleWindows(List<WindowInfo> list) {
            for (int size = list.size() - 1; size >= 0; size--) {
                list.remove(size).recycle();
            }
        }

        private static boolean isReportedWindowType(int i) {
            return (i == 2013 || i == 2021 || i == 2026 || i == 2016 || i == 2022 || i == 2018 || i == 2027 || i == 1004 || i == 2015 || i == 2030) ? false : true;
        }

        private void populateVisibleWindowsOnScreenLocked(final SparseArray<WindowState> sparseArray) {
            DisplayContent defaultDisplayContentLocked = this.mService.getDefaultDisplayContentLocked();
            this.mTempLayer = 0;
            defaultDisplayContentLocked.forAllWindows(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    AccessibilityController.WindowsForAccessibilityObserver.lambda$populateVisibleWindowsOnScreenLocked$0(this.f$0, sparseArray, (WindowState) obj);
                }
            }, false);
        }

        public static void lambda$populateVisibleWindowsOnScreenLocked$0(WindowsForAccessibilityObserver windowsForAccessibilityObserver, SparseArray sparseArray, WindowState windowState) {
            if (windowState.isVisibleLw()) {
                int i = windowsForAccessibilityObserver.mTempLayer;
                windowsForAccessibilityObserver.mTempLayer = i + 1;
                sparseArray.put(i, windowState);
            }
        }

        private class MyHandler extends Handler {
            public static final int MESSAGE_COMPUTE_CHANGED_WINDOWS = 1;

            public MyHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    WindowsForAccessibilityObserver.this.computeChangedWindows();
                }
            }
        }
    }
}
