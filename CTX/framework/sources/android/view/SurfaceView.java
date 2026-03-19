package android.view;

import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import com.android.internal.view.SurfaceCallbackHelper;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class SurfaceView extends View implements ViewRootImpl.WindowStoppedCallback {
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.surfaceview.log", false);
    private static final String TAG = "SurfaceView";
    private boolean mAttachedToWindow;
    final ArrayList<SurfaceHolder.Callback> mCallbacks;
    final Configuration mConfiguration;
    SurfaceControl mDeferredDestroySurfaceControl;
    boolean mDrawFinished;
    private final ViewTreeObserver.OnPreDrawListener mDrawListener;
    boolean mDrawingStopped;
    int mFormat;
    private boolean mGlobalListenersAdded;
    boolean mHaveFrame;
    boolean mIsCreating;
    long mLastLockTime;
    int mLastSurfaceHeight;
    int mLastSurfaceWidth;
    boolean mLastWindowVisibility;
    final int[] mLocation;
    private int mPendingReportDraws;
    private Rect mRTLastReportedPosition;
    int mRequestedFormat;
    int mRequestedHeight;
    boolean mRequestedVisible;
    int mRequestedWidth;
    private volatile boolean mRtHandlingPositionUpdates;
    private SurfaceControl.Transaction mRtTransaction;
    final Rect mScreenRect;
    private final ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;
    int mSubLayer;
    final Surface mSurface;
    SurfaceControlWithBackground mSurfaceControl;
    boolean mSurfaceCreated;
    private int mSurfaceFlags;
    final Rect mSurfaceFrame;
    int mSurfaceHeight;
    private final SurfaceHolder mSurfaceHolder;
    final ReentrantLock mSurfaceLock;
    SurfaceSession mSurfaceSession;
    int mSurfaceWidth;
    final Rect mTmpRect;
    private CompatibilityInfo.Translator mTranslator;
    boolean mViewVisibility;
    boolean mVisible;
    int mWindowSpaceLeft;
    int mWindowSpaceTop;
    boolean mWindowStopped;
    boolean mWindowVisibility;

    public SurfaceView(Context context) {
        this(context, null);
    }

    public SurfaceView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SurfaceView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SurfaceView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mCallbacks = new ArrayList<>();
        this.mLocation = new int[2];
        this.mSurfaceLock = new ReentrantLock(true);
        this.mSurface = new Surface();
        this.mDrawingStopped = true;
        this.mDrawFinished = false;
        this.mScreenRect = new Rect();
        this.mTmpRect = new Rect();
        this.mConfiguration = new Configuration();
        this.mSubLayer = -2;
        this.mIsCreating = false;
        this.mRtHandlingPositionUpdates = false;
        this.mScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                SurfaceView.this.updateSurface();
            }
        };
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                SurfaceView.this.mHaveFrame = SurfaceView.this.getWidth() > 0 && SurfaceView.this.getHeight() > 0;
                SurfaceView.this.updateSurface();
                return true;
            }
        };
        this.mRequestedVisible = false;
        this.mWindowVisibility = false;
        this.mLastWindowVisibility = false;
        this.mViewVisibility = false;
        this.mWindowStopped = false;
        this.mRequestedWidth = -1;
        this.mRequestedHeight = -1;
        this.mRequestedFormat = 4;
        this.mHaveFrame = false;
        this.mSurfaceCreated = false;
        this.mLastLockTime = 0L;
        this.mVisible = false;
        this.mWindowSpaceLeft = -1;
        this.mWindowSpaceTop = -1;
        this.mSurfaceWidth = -1;
        this.mSurfaceHeight = -1;
        this.mFormat = -1;
        this.mSurfaceFrame = new Rect();
        this.mLastSurfaceWidth = -1;
        this.mLastSurfaceHeight = -1;
        this.mSurfaceFlags = 4;
        this.mRtTransaction = new SurfaceControl.Transaction();
        this.mRTLastReportedPosition = new Rect();
        this.mSurfaceHolder = new AnonymousClass3();
        this.mRenderNode.requestPositionUpdates(this);
        setWillNotDraw(true);
    }

    public SurfaceHolder getHolder() {
        return this.mSurfaceHolder;
    }

    private void updateRequestedVisibility() {
        this.mRequestedVisible = this.mViewVisibility && this.mWindowVisibility && !this.mWindowStopped;
    }

    @Override
    public void windowStopped(boolean z) {
        if (DEBUG) {
            Log.i(TAG, "windowStopped,mWindowStopped:" + this.mWindowStopped + ",stopped:" + z);
        }
        this.mWindowStopped = z;
        updateRequestedVisibility();
        updateSurface();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.i(TAG, "onAttachedToWindow");
        }
        getViewRootImpl().addWindowStoppedCallback(this);
        this.mWindowStopped = false;
        this.mViewVisibility = getVisibility() == 0;
        updateRequestedVisibility();
        this.mAttachedToWindow = true;
        this.mParent.requestTransparentRegion(this);
        if (!this.mGlobalListenersAdded) {
            ViewTreeObserver viewTreeObserver = getViewTreeObserver();
            viewTreeObserver.addOnScrollChangedListener(this.mScrollChangedListener);
            viewTreeObserver.addOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = true;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        this.mWindowVisibility = i == 0;
        updateRequestedVisibility();
        updateSurface();
    }

    @Override
    public void setVisibility(int i) {
        super.setVisibility(i);
        boolean z = false;
        this.mViewVisibility = i == 0;
        if (this.mWindowVisibility && this.mViewVisibility && !this.mWindowStopped) {
            z = true;
        }
        if (z != this.mRequestedVisible) {
            requestLayout();
        }
        this.mRequestedVisible = z;
        updateSurface();
    }

    private void performDrawFinished() {
        if (this.mPendingReportDraws > 0) {
            this.mDrawFinished = true;
            if (this.mAttachedToWindow) {
                notifyDrawFinished();
                invalidate();
                return;
            }
            return;
        }
        Log.e(TAG, System.identityHashCode(this) + "finished drawing but no pending report draw (extra call to draw completion runnable?)");
    }

    void notifyDrawFinished() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.pendingDrawFinished();
        }
        this.mPendingReportDraws--;
    }

    @Override
    protected void onDetachedFromWindow() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.removeWindowStoppedCallback(this);
        }
        if (DEBUG) {
            Log.i(TAG, "onDetachedFromWindow");
        }
        this.mAttachedToWindow = false;
        if (this.mGlobalListenersAdded) {
            ViewTreeObserver viewTreeObserver = getViewTreeObserver();
            viewTreeObserver.removeOnScrollChangedListener(this.mScrollChangedListener);
            viewTreeObserver.removeOnPreDrawListener(this.mDrawListener);
            this.mGlobalListenersAdded = false;
        }
        while (this.mPendingReportDraws > 0) {
            notifyDrawFinished();
        }
        this.mRequestedVisible = false;
        updateSurface();
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.destroy();
        }
        this.mSurfaceControl = null;
        this.mHaveFrame = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int defaultSize;
        int defaultSize2;
        if (this.mRequestedWidth >= 0) {
            defaultSize = resolveSizeAndState(this.mRequestedWidth, i, 0);
        } else {
            defaultSize = getDefaultSize(0, i);
        }
        if (this.mRequestedHeight >= 0) {
            defaultSize2 = resolveSizeAndState(this.mRequestedHeight, i2, 0);
        } else {
            defaultSize2 = getDefaultSize(0, i2);
        }
        setMeasuredDimension(defaultSize, defaultSize2);
    }

    @Override
    protected boolean setFrame(int i, int i2, int i3, int i4) {
        boolean frame = super.setFrame(i, i2, i3, i4);
        updateSurface();
        return frame;
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        boolean zGatherTransparentRegion;
        if (isAboveParent() || !this.mDrawFinished) {
            return super.gatherTransparentRegion(region);
        }
        if ((this.mPrivateFlags & 128) == 0) {
            zGatherTransparentRegion = super.gatherTransparentRegion(region);
        } else {
            if (region != null) {
                int width = getWidth();
                int height = getHeight();
                if (width > 0 && height > 0) {
                    getLocationInWindow(this.mLocation);
                    int i = this.mLocation[0];
                    int i2 = this.mLocation[1];
                    region.op(i, i2, i + width, i2 + height, Region.Op.UNION);
                }
            }
            zGatherTransparentRegion = true;
        }
        if (PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            return false;
        }
        return zGatherTransparentRegion;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mDrawFinished && !isAboveParent() && (this.mPrivateFlags & 128) == 0) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (this.mDrawFinished && !isAboveParent() && (this.mPrivateFlags & 128) == 128) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }
        super.dispatchDraw(canvas);
    }

    public void setZOrderMediaOverlay(boolean z) {
        this.mSubLayer = z ? -1 : -2;
    }

    public void setZOrderOnTop(boolean z) {
        if (z) {
            this.mSubLayer = 1;
        } else {
            this.mSubLayer = -2;
        }
    }

    public void setSecure(boolean z) {
        if (z) {
            this.mSurfaceFlags |= 128;
        } else {
            this.mSurfaceFlags &= -129;
        }
    }

    private void updateOpaqueFlag() {
        if (!PixelFormat.formatHasAlpha(this.mRequestedFormat)) {
            this.mSurfaceFlags |= 1024;
        } else {
            this.mSurfaceFlags &= -1025;
        }
    }

    private Rect getParentSurfaceInsets() {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl == null) {
            return null;
        }
        return viewRootImpl.mWindowAttributes.surfaceInsets;
    }

    protected void updateSurface() {
        ViewRootImpl viewRootImpl;
        boolean z;
        SurfaceHolder.Callback[] surfaceCallbacks;
        if (this.mHaveFrame && (viewRootImpl = getViewRootImpl()) != null && viewRootImpl.mSurface != null && viewRootImpl.mSurface.isValid()) {
            this.mTranslator = viewRootImpl.mTranslator;
            if (this.mTranslator != null) {
                this.mSurface.setCompatibilityTranslator(this.mTranslator);
            }
            int width = this.mRequestedWidth;
            if (width <= 0) {
                width = getWidth();
            }
            int height = this.mRequestedHeight;
            if (height <= 0) {
                height = getHeight();
            }
            boolean z2 = this.mFormat != this.mRequestedFormat;
            boolean z3 = this.mVisible != this.mRequestedVisible;
            boolean z4 = (this.mSurfaceControl == null || z2 || z3) && this.mRequestedVisible;
            boolean z5 = (this.mSurfaceWidth == width && this.mSurfaceHeight == height) ? false : true;
            boolean z6 = this.mWindowVisibility != this.mLastWindowVisibility;
            if (z4 || z2 || z5 || z3 || z6) {
                getLocationInWindow(this.mLocation);
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(System.identityHashCode(this));
                    sb.append(" Changes: creating=");
                    sb.append(z4);
                    sb.append(" format=");
                    sb.append(z2);
                    sb.append(" size=");
                    sb.append(z5);
                    sb.append(" visible=");
                    sb.append(z3);
                    sb.append(" left=");
                    sb.append(this.mWindowSpaceLeft != this.mLocation[0]);
                    sb.append(" top=");
                    sb.append(this.mWindowSpaceTop != this.mLocation[1]);
                    Log.i(TAG, sb.toString());
                }
                try {
                    z = this.mRequestedVisible;
                    this.mVisible = z;
                    this.mWindowSpaceLeft = this.mLocation[0];
                    this.mWindowSpaceTop = this.mLocation[1];
                    this.mSurfaceWidth = width;
                    this.mSurfaceHeight = height;
                    this.mFormat = this.mRequestedFormat;
                    this.mLastWindowVisibility = this.mWindowVisibility;
                    this.mScreenRect.left = this.mWindowSpaceLeft;
                    this.mScreenRect.top = this.mWindowSpaceTop;
                    this.mScreenRect.right = this.mWindowSpaceLeft + getWidth();
                    this.mScreenRect.bottom = this.mWindowSpaceTop + getHeight();
                    if (this.mTranslator != null) {
                        this.mTranslator.translateRectInAppWindowToScreen(this.mScreenRect);
                    }
                    Rect parentSurfaceInsets = getParentSurfaceInsets();
                    this.mScreenRect.offset(parentSurfaceInsets.left, parentSurfaceInsets.top);
                    if (z4) {
                        this.mSurfaceSession = new SurfaceSession(viewRootImpl.mSurface);
                        this.mDeferredDestroySurfaceControl = this.mSurfaceControl;
                        updateOpaqueFlag();
                        this.mSurfaceControl = new SurfaceControlWithBackground("SurfaceView - " + viewRootImpl.getTitle().toString(), (this.mSurfaceFlags & 1024) != 0, new SurfaceControl.Builder(this.mSurfaceSession).setSize(this.mSurfaceWidth, this.mSurfaceHeight).setFormat(this.mFormat).setFlags(this.mSurfaceFlags));
                    } else if (this.mSurfaceControl == null) {
                        return;
                    }
                    this.mSurfaceLock.lock();
                    try {
                        this.mDrawingStopped = !z;
                        if (DEBUG) {
                            Log.i(TAG, System.identityHashCode(this) + " Cur surface: " + this.mSurface);
                        }
                        SurfaceControl.openTransaction();
                    } finally {
                        this.mSurfaceLock.unlock();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception configuring surface", e);
                }
                try {
                    this.mSurfaceControl.setLayer(this.mSubLayer);
                    if (this.mViewVisibility) {
                        this.mSurfaceControl.show();
                    } else {
                        this.mSurfaceControl.hide();
                    }
                    if (z5 || z4 || !this.mRtHandlingPositionUpdates) {
                        this.mSurfaceControl.setPosition(this.mScreenRect.left, this.mScreenRect.top);
                        this.mSurfaceControl.setMatrix(this.mScreenRect.width() / this.mSurfaceWidth, 0.0f, 0.0f, this.mScreenRect.height() / this.mSurfaceHeight);
                    }
                    if (z5) {
                        this.mSurfaceControl.setSize(this.mSurfaceWidth, this.mSurfaceHeight);
                    }
                    boolean z7 = z5 || z4;
                    this.mSurfaceFrame.left = 0;
                    this.mSurfaceFrame.top = 0;
                    if (this.mTranslator == null) {
                        this.mSurfaceFrame.right = this.mSurfaceWidth;
                        this.mSurfaceFrame.bottom = this.mSurfaceHeight;
                    } else {
                        float f = this.mTranslator.applicationInvertedScale;
                        this.mSurfaceFrame.right = (int) ((this.mSurfaceWidth * f) + 0.5f);
                        this.mSurfaceFrame.bottom = (int) ((this.mSurfaceHeight * f) + 0.5f);
                    }
                    int i = this.mSurfaceFrame.right;
                    int i2 = this.mSurfaceFrame.bottom;
                    boolean z8 = (this.mLastSurfaceWidth == i && this.mLastSurfaceHeight == i2) ? false : true;
                    this.mLastSurfaceWidth = i;
                    this.mLastSurfaceHeight = i2;
                    if (z) {
                        try {
                            boolean z9 = !this.mDrawFinished;
                            boolean z10 = z7 | z9;
                            if (this.mSurfaceCreated && (z4 || (!z && z3))) {
                                this.mSurfaceCreated = false;
                                if (this.mSurface.isValid()) {
                                    if (DEBUG) {
                                        Log.i(TAG, System.identityHashCode(this) + " visibleChanged -- surfaceDestroyed");
                                    }
                                    surfaceCallbacks = getSurfaceCallbacks();
                                    for (SurfaceHolder.Callback callback : surfaceCallbacks) {
                                        callback.surfaceDestroyed(this.mSurfaceHolder);
                                    }
                                    if (this.mSurface.isValid()) {
                                        this.mSurface.forceScopedDisconnect();
                                    }
                                }
                                if (z4) {
                                }
                                if (z5) {
                                    this.mSurface.createFrom(this.mSurfaceControl);
                                }
                                if (z) {
                                    if (!this.mSurfaceCreated) {
                                        this.mSurfaceCreated = true;
                                        this.mIsCreating = true;
                                        if (DEBUG) {
                                        }
                                        if (surfaceCallbacks == null) {
                                        }
                                        while (i < r7) {
                                        }
                                    }
                                    if (!z4) {
                                        if (DEBUG) {
                                        }
                                        if (surfaceCallbacks == null) {
                                        }
                                        while (i < r4) {
                                        }
                                        if (z10) {
                                        }
                                    }
                                }
                                this.mIsCreating = false;
                                if (this.mSurfaceControl != null) {
                                    this.mSurface.release();
                                    this.mSurfaceControl.destroy();
                                    this.mSurfaceControl = null;
                                }
                            } else {
                                surfaceCallbacks = null;
                                if (z4) {
                                    this.mSurface.copyFrom(this.mSurfaceControl);
                                }
                                if (z5 && getContext().getApplicationInfo().targetSdkVersion < 26) {
                                    this.mSurface.createFrom(this.mSurfaceControl);
                                }
                                if (z && this.mSurface.isValid()) {
                                    if (!this.mSurfaceCreated && (z4 || z3)) {
                                        this.mSurfaceCreated = true;
                                        this.mIsCreating = true;
                                        if (DEBUG) {
                                            Log.i(TAG, System.identityHashCode(this) + " visibleChanged -- surfaceCreated");
                                        }
                                        if (surfaceCallbacks == null) {
                                            surfaceCallbacks = getSurfaceCallbacks();
                                        }
                                        for (SurfaceHolder.Callback callback2 : surfaceCallbacks) {
                                            callback2.surfaceCreated(this.mSurfaceHolder);
                                        }
                                    }
                                    if (!z4 || z2 || z5 || z3 || z8) {
                                        if (DEBUG) {
                                            Log.i(TAG, System.identityHashCode(this) + " surfaceChanged -- format=" + this.mFormat + " w=" + width + " h=" + height);
                                        }
                                        if (surfaceCallbacks == null) {
                                            surfaceCallbacks = getSurfaceCallbacks();
                                        }
                                        for (SurfaceHolder.Callback callback3 : surfaceCallbacks) {
                                            callback3.surfaceChanged(this.mSurfaceHolder, this.mFormat, width, height);
                                        }
                                    }
                                    if (z10) {
                                        if (DEBUG) {
                                            Log.i(TAG, System.identityHashCode(this) + " surfaceRedrawNeeded");
                                        }
                                        if (surfaceCallbacks == null) {
                                            surfaceCallbacks = getSurfaceCallbacks();
                                        }
                                        this.mPendingReportDraws++;
                                        viewRootImpl.drawPending();
                                        new SurfaceCallbackHelper(new Runnable() {
                                            @Override
                                            public final void run() {
                                                this.f$0.onDrawFinished();
                                            }
                                        }).dispatchSurfaceRedrawNeededAsync(this.mSurfaceHolder, surfaceCallbacks);
                                    }
                                }
                                this.mIsCreating = false;
                                if (this.mSurfaceControl != null && !this.mSurfaceCreated) {
                                    this.mSurface.release();
                                    this.mSurfaceControl.destroy();
                                    this.mSurfaceControl = null;
                                }
                            }
                        } catch (Throwable th) {
                            this.mIsCreating = false;
                            if (this.mSurfaceControl != null && !this.mSurfaceCreated) {
                                this.mSurface.release();
                                this.mSurfaceControl.destroy();
                                this.mSurfaceControl = null;
                            }
                            throw th;
                        }
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Layout: x=" + this.mScreenRect.left + " y=" + this.mScreenRect.top + " w=" + this.mScreenRect.width() + " h=" + this.mScreenRect.height() + ", frame=" + this.mSurfaceFrame);
                        return;
                    }
                    return;
                } finally {
                    SurfaceControl.closeTransaction();
                }
            }
            getLocationInSurface(this.mLocation);
            boolean z11 = (this.mWindowSpaceLeft == this.mLocation[0] && this.mWindowSpaceTop == this.mLocation[1]) ? false : true;
            boolean z12 = (getWidth() == this.mScreenRect.width() && getHeight() == this.mScreenRect.height()) ? false : true;
            if (z11 || z12) {
                this.mWindowSpaceLeft = this.mLocation[0];
                this.mWindowSpaceTop = this.mLocation[1];
                this.mLocation[0] = getWidth();
                this.mLocation[1] = getHeight();
                this.mScreenRect.set(this.mWindowSpaceLeft, this.mWindowSpaceTop, this.mWindowSpaceLeft + this.mLocation[0], this.mWindowSpaceTop + this.mLocation[1]);
                if (this.mTranslator != null) {
                    this.mTranslator.translateRectInAppWindowToScreen(this.mScreenRect);
                }
                if (this.mSurfaceControl == null) {
                    return;
                }
                if (!isHardwareAccelerated() || !this.mRtHandlingPositionUpdates) {
                    try {
                        if (DEBUG) {
                            Log.d(TAG, String.format("%d updateSurfacePosition UI, postion = [%d, %d, %d, %d]", Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(this.mScreenRect.left), Integer.valueOf(this.mScreenRect.top), Integer.valueOf(this.mScreenRect.right), Integer.valueOf(this.mScreenRect.bottom)));
                        }
                        setParentSpaceRectangle(this.mScreenRect, -1L);
                    } catch (Exception e2) {
                        Log.e(TAG, "Exception configuring surface", e2);
                    }
                }
            }
        }
    }

    private void onDrawFinished() {
        if (DEBUG) {
            Log.i(TAG, System.identityHashCode(this) + " finishedDrawing");
        }
        if (this.mDeferredDestroySurfaceControl != null) {
            this.mDeferredDestroySurfaceControl.destroy();
            this.mDeferredDestroySurfaceControl = null;
        }
        runOnUiThread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.performDrawFinished();
            }
        });
    }

    protected void applyChildSurfaceTransaction_renderWorker(SurfaceControl.Transaction transaction, Surface surface, long j) {
    }

    private void applySurfaceTransforms(SurfaceControl surfaceControl, Rect rect, long j) {
        if (j > 0) {
            this.mRtTransaction.deferTransactionUntilSurface(surfaceControl, getViewRootImpl().mSurface, j);
        }
        this.mRtTransaction.setPosition(surfaceControl, rect.left, rect.top);
        this.mRtTransaction.setMatrix(surfaceControl, rect.width() / this.mSurfaceWidth, 0.0f, 0.0f, rect.height() / this.mSurfaceHeight);
    }

    private void setParentSpaceRectangle(Rect rect, long j) {
        ViewRootImpl viewRootImpl = getViewRootImpl();
        applySurfaceTransforms(this.mSurfaceControl, rect, j);
        applySurfaceTransforms(this.mSurfaceControl.mBackgroundControl, rect, j);
        applyChildSurfaceTransaction_renderWorker(this.mRtTransaction, viewRootImpl.mSurface, j);
        this.mRtTransaction.apply();
    }

    public final void updateSurfacePosition_renderWorker(long j, int i, int i2, int i3, int i4) {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mRtHandlingPositionUpdates = true;
        if (this.mRTLastReportedPosition.left == i && this.mRTLastReportedPosition.top == i2 && this.mRTLastReportedPosition.right == i3 && this.mRTLastReportedPosition.bottom == i4) {
            return;
        }
        try {
            if (DEBUG) {
                Log.d(TAG, String.format("%d updateSurfacePosition RenderWorker, frameNr = %d, postion = [%d, %d, %d, %d]", Integer.valueOf(System.identityHashCode(this)), Long.valueOf(j), Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)));
            }
            this.mRTLastReportedPosition.set(i, i2, i3, i4);
            setParentSpaceRectangle(this.mRTLastReportedPosition, j);
        } catch (Exception e) {
            Log.e(TAG, "Exception from repositionChild", e);
        }
    }

    public final void surfacePositionLost_uiRtSync(long j) {
        if (DEBUG) {
            Log.d(TAG, String.format("%d windowPositionLost, frameNr = %d", Integer.valueOf(System.identityHashCode(this)), Long.valueOf(j)));
        }
        this.mRTLastReportedPosition.setEmpty();
        if (this.mSurfaceControl != null && this.mRtHandlingPositionUpdates) {
            this.mRtHandlingPositionUpdates = false;
            if (!this.mScreenRect.isEmpty() && !this.mScreenRect.equals(this.mRTLastReportedPosition)) {
                try {
                    if (DEBUG) {
                        Log.d(TAG, String.format("%d updateSurfacePosition, postion = [%d, %d, %d, %d]", Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(this.mScreenRect.left), Integer.valueOf(this.mScreenRect.top), Integer.valueOf(this.mScreenRect.right), Integer.valueOf(this.mScreenRect.bottom)));
                    }
                    setParentSpaceRectangle(this.mScreenRect, j);
                } catch (Exception e) {
                    Log.e(TAG, "Exception configuring surface", e);
                }
            }
        }
    }

    private SurfaceHolder.Callback[] getSurfaceCallbacks() {
        SurfaceHolder.Callback[] callbackArr;
        synchronized (this.mCallbacks) {
            callbackArr = new SurfaceHolder.Callback[this.mCallbacks.size()];
            this.mCallbacks.toArray(callbackArr);
        }
        return callbackArr;
    }

    private void runOnUiThread(Runnable runnable) {
        Handler handler = getHandler();
        if (handler != null && handler.getLooper() != Looper.myLooper()) {
            handler.post(runnable);
        } else {
            runnable.run();
        }
    }

    public boolean isFixedSize() {
        return (this.mRequestedWidth == -1 && this.mRequestedHeight == -1) ? false : true;
    }

    private boolean isAboveParent() {
        return this.mSubLayer >= 0;
    }

    public void setResizeBackgroundColor(int i) {
        this.mSurfaceControl.setBackgroundColor(i);
    }

    class AnonymousClass3 implements SurfaceHolder {
        private static final String LOG_TAG = "SurfaceHolder";

        AnonymousClass3() {
        }

        @Override
        public boolean isCreating() {
            return SurfaceView.this.mIsCreating;
        }

        @Override
        public void addCallback(SurfaceHolder.Callback callback) {
            synchronized (SurfaceView.this.mCallbacks) {
                if (!SurfaceView.this.mCallbacks.contains(callback)) {
                    SurfaceView.this.mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void removeCallback(SurfaceHolder.Callback callback) {
            synchronized (SurfaceView.this.mCallbacks) {
                SurfaceView.this.mCallbacks.remove(callback);
            }
        }

        @Override
        public void setFixedSize(int i, int i2) {
            if (SurfaceView.this.mRequestedWidth != i || SurfaceView.this.mRequestedHeight != i2) {
                SurfaceView.this.mRequestedWidth = i;
                SurfaceView.this.mRequestedHeight = i2;
                SurfaceView.this.requestLayout();
            }
        }

        @Override
        public void setSizeFromLayout() {
            if (SurfaceView.this.mRequestedWidth != -1 || SurfaceView.this.mRequestedHeight != -1) {
                SurfaceView surfaceView = SurfaceView.this;
                SurfaceView.this.mRequestedHeight = -1;
                surfaceView.mRequestedWidth = -1;
                SurfaceView.this.requestLayout();
            }
        }

        @Override
        public void setFormat(int i) {
            if (i == -1) {
                i = 4;
            }
            SurfaceView.this.mRequestedFormat = i;
            if (SurfaceView.this.mSurfaceControl != null) {
                SurfaceView.this.updateSurface();
            }
        }

        @Override
        @Deprecated
        public void setType(int i) {
        }

        @Override
        public void setKeepScreenOn(final boolean z) {
            SurfaceView.this.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    SurfaceView.this.setKeepScreenOn(z);
                }
            });
        }

        @Override
        public Canvas lockCanvas() {
            return internalLockCanvas(null, false);
        }

        @Override
        public Canvas lockCanvas(Rect rect) {
            return internalLockCanvas(rect, false);
        }

        @Override
        public Canvas lockHardwareCanvas() {
            return internalLockCanvas(null, true);
        }

        private Canvas internalLockCanvas(Rect rect, boolean z) {
            Canvas canvasLockCanvas;
            SurfaceView.this.mSurfaceLock.lock();
            if (SurfaceView.DEBUG) {
                Log.i(SurfaceView.TAG, System.identityHashCode(this) + " Locking canvas... stopped=" + SurfaceView.this.mDrawingStopped + ", surfaceControl=" + SurfaceView.this.mSurfaceControl);
            }
            if (!SurfaceView.this.mDrawingStopped && SurfaceView.this.mSurfaceControl != null) {
                try {
                    if (z) {
                        canvasLockCanvas = SurfaceView.this.mSurface.lockHardwareCanvas();
                    } else {
                        canvasLockCanvas = SurfaceView.this.mSurface.lockCanvas(rect);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception locking surface", e);
                    canvasLockCanvas = null;
                }
            } else {
                canvasLockCanvas = null;
            }
            if (SurfaceView.DEBUG) {
                Log.i(SurfaceView.TAG, System.identityHashCode(this) + " Returned canvas: " + canvasLockCanvas);
            }
            if (canvasLockCanvas != null) {
                SurfaceView.this.mLastLockTime = SystemClock.uptimeMillis();
                return canvasLockCanvas;
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            long j = SurfaceView.this.mLastLockTime + 100;
            if (j > jUptimeMillis) {
                try {
                    Thread.sleep(j - jUptimeMillis);
                } catch (InterruptedException e2) {
                }
                jUptimeMillis = SystemClock.uptimeMillis();
            }
            SurfaceView.this.mLastLockTime = jUptimeMillis;
            SurfaceView.this.mSurfaceLock.unlock();
            return null;
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            if (SurfaceView.DEBUG) {
                Log.i(SurfaceView.TAG, System.identityHashCode(this) + "[unlockCanvasAndPost] canvas:" + canvas);
            }
            SurfaceView.this.mSurface.unlockCanvasAndPost(canvas);
            SurfaceView.this.mSurfaceLock.unlock();
        }

        @Override
        public Surface getSurface() {
            return SurfaceView.this.mSurface;
        }

        @Override
        public Rect getSurfaceFrame() {
            return SurfaceView.this.mSurfaceFrame;
        }
    }

    class SurfaceControlWithBackground extends SurfaceControl {
        SurfaceControl mBackgroundControl;
        private boolean mOpaque;
        public boolean mVisible;

        public SurfaceControlWithBackground(String str, boolean z, SurfaceControl.Builder builder) throws Exception {
            super(builder.setName(str).build());
            this.mOpaque = true;
            this.mVisible = false;
            this.mBackgroundControl = builder.setName("Background for -" + str).setFormat(1024).setColorLayer(true).build();
            this.mOpaque = z;
        }

        @Override
        public void setAlpha(float f) {
            super.setAlpha(f);
            this.mBackgroundControl.setAlpha(f);
        }

        @Override
        public void setLayer(int i) {
            super.setLayer(i);
            this.mBackgroundControl.setLayer(-3);
        }

        @Override
        public void setPosition(float f, float f2) {
            super.setPosition(f, f2);
            this.mBackgroundControl.setPosition(f, f2);
        }

        @Override
        public void setSize(int i, int i2) {
            super.setSize(i, i2);
            this.mBackgroundControl.setSize(i, i2);
        }

        @Override
        public void setWindowCrop(Rect rect) {
            super.setWindowCrop(rect);
            this.mBackgroundControl.setWindowCrop(rect);
        }

        @Override
        public void setFinalCrop(Rect rect) {
            super.setFinalCrop(rect);
            this.mBackgroundControl.setFinalCrop(rect);
        }

        @Override
        public void setLayerStack(int i) {
            super.setLayerStack(i);
            this.mBackgroundControl.setLayerStack(i);
        }

        @Override
        public void setOpaque(boolean z) {
            super.setOpaque(z);
            this.mOpaque = z;
            updateBackgroundVisibility();
        }

        @Override
        public void setSecure(boolean z) {
            super.setSecure(z);
        }

        @Override
        public void setMatrix(float f, float f2, float f3, float f4) {
            super.setMatrix(f, f2, f3, f4);
            this.mBackgroundControl.setMatrix(f, f2, f3, f4);
        }

        @Override
        public void hide() {
            super.hide();
            this.mVisible = false;
            updateBackgroundVisibility();
        }

        @Override
        public void show() {
            super.show();
            this.mVisible = true;
            updateBackgroundVisibility();
        }

        @Override
        public void destroy() {
            super.destroy();
            this.mBackgroundControl.destroy();
        }

        @Override
        public void release() {
            super.release();
            this.mBackgroundControl.release();
        }

        @Override
        public void setTransparentRegionHint(Region region) {
            super.setTransparentRegionHint(region);
            this.mBackgroundControl.setTransparentRegionHint(region);
        }

        @Override
        public void deferTransactionUntil(IBinder iBinder, long j) {
            super.deferTransactionUntil(iBinder, j);
            this.mBackgroundControl.deferTransactionUntil(iBinder, j);
        }

        @Override
        public void deferTransactionUntil(Surface surface, long j) {
            super.deferTransactionUntil(surface, j);
            this.mBackgroundControl.deferTransactionUntil(surface, j);
        }

        private void setBackgroundColor(int i) {
            float[] fArr = {Color.red(i) / 255.0f, Color.green(i) / 255.0f, Color.blue(i) / 255.0f};
            SurfaceControl.openTransaction();
            try {
                this.mBackgroundControl.setColor(fArr);
            } finally {
                SurfaceControl.closeTransaction();
            }
        }

        void updateBackgroundVisibility() {
            if (this.mOpaque && this.mVisible) {
                this.mBackgroundControl.show();
            } else {
                this.mBackgroundControl.hide();
            }
        }
    }
}
