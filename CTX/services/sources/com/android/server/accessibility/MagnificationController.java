package com.android.server.accessibility;

import android.R;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.MathUtils;
import android.view.MagnificationSpec;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import java.util.Locale;

public class MagnificationController implements Handler.Callback {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SET_MAGNIFICATION_SPEC = false;
    private static final float DEFAULT_MAGNIFICATION_SCALE = 2.0f;
    private static final int INVALID_ID = -1;
    private static final String LOG_TAG = "MagnificationController";
    public static final float MAX_SCALE = 5.0f;
    public static final float MIN_SCALE = 1.0f;
    private static final int MSG_ON_MAGNIFIED_BOUNDS_CHANGED = 3;
    private static final int MSG_ON_RECTANGLE_ON_SCREEN_REQUESTED = 4;
    private static final int MSG_ON_USER_CONTEXT_CHANGED = 5;
    private static final int MSG_SCREEN_TURNED_OFF = 2;
    private static final int MSG_SEND_SPEC_TO_ANIMATION = 1;
    private final AccessibilityManagerService mAms;
    private final MagnificationSpec mCurrentMagnificationSpec;
    private Handler mHandler;
    private int mIdOfLastServiceToMagnify;
    private final Object mLock;
    private final Rect mMagnificationBounds;
    private final Region mMagnificationRegion;
    private final long mMainThreadId;

    @VisibleForTesting
    boolean mRegistered;
    private final ScreenStateObserver mScreenStateObserver;
    private final SettingsBridge mSettingsBridge;
    private final SpecAnimationBridge mSpecAnimationBridge;
    private final Rect mTempRect;
    private final Rect mTempRect1;
    private boolean mUnregisterPending;
    private int mUserId;
    private final WindowManagerInternal.MagnificationCallbacks mWMCallbacks;
    private final WindowManagerInternal mWindowManager;

    public MagnificationController(Context context, AccessibilityManagerService accessibilityManagerService, Object obj) {
        this(context, accessibilityManagerService, obj, null, (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class), new ValueAnimator(), new SettingsBridge(context.getContentResolver()));
        this.mHandler = new Handler(context.getMainLooper(), this);
    }

    public MagnificationController(Context context, AccessibilityManagerService accessibilityManagerService, Object obj, Handler handler, WindowManagerInternal windowManagerInternal, ValueAnimator valueAnimator, SettingsBridge settingsBridge) {
        this.mCurrentMagnificationSpec = MagnificationSpec.obtain();
        this.mMagnificationRegion = Region.obtain();
        this.mMagnificationBounds = new Rect();
        this.mTempRect = new Rect();
        this.mTempRect1 = new Rect();
        this.mWMCallbacks = new WindowManagerInternal.MagnificationCallbacks() {
            @Override
            public void onMagnificationRegionChanged(Region region) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = Region.obtain(region);
                MagnificationController.this.mHandler.obtainMessage(3, someArgsObtain).sendToTarget();
            }

            @Override
            public void onRectangleOnScreenRequested(int i, int i2, int i3, int i4) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.argi1 = i;
                someArgsObtain.argi2 = i2;
                someArgsObtain.argi3 = i3;
                someArgsObtain.argi4 = i4;
                MagnificationController.this.mHandler.obtainMessage(4, someArgsObtain).sendToTarget();
            }

            @Override
            public void onRotationChanged(int i) {
                MagnificationController.this.mHandler.sendEmptyMessage(5);
            }

            @Override
            public void onUserContextChanged() {
                MagnificationController.this.mHandler.sendEmptyMessage(5);
            }
        };
        this.mIdOfLastServiceToMagnify = -1;
        this.mHandler = handler;
        this.mWindowManager = windowManagerInternal;
        this.mMainThreadId = context.getMainLooper().getThread().getId();
        this.mAms = accessibilityManagerService;
        this.mScreenStateObserver = new ScreenStateObserver(context, this);
        this.mLock = obj;
        this.mSpecAnimationBridge = new SpecAnimationBridge(context, this.mLock, this.mWindowManager, valueAnimator);
        this.mSettingsBridge = settingsBridge;
    }

    public void register() {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                this.mScreenStateObserver.register();
                this.mWindowManager.setMagnificationCallbacks(this.mWMCallbacks);
                this.mSpecAnimationBridge.setEnabled(true);
                this.mWindowManager.getMagnificationRegion(this.mMagnificationRegion);
                this.mMagnificationRegion.getBounds(this.mMagnificationBounds);
                this.mRegistered = true;
            }
        }
    }

    public void unregister() {
        synchronized (this.mLock) {
            if (!isMagnifying()) {
                unregisterInternalLocked();
            } else {
                this.mUnregisterPending = true;
                resetLocked(true);
            }
        }
    }

    public boolean isRegisteredLocked() {
        return this.mRegistered;
    }

    private void unregisterInternalLocked() {
        if (this.mRegistered) {
            this.mSpecAnimationBridge.setEnabled(false);
            this.mScreenStateObserver.unregister();
            this.mWindowManager.setMagnificationCallbacks(null);
            this.mMagnificationRegion.setEmpty();
            this.mRegistered = false;
        }
        this.mUnregisterPending = false;
    }

    public boolean isMagnifying() {
        return this.mCurrentMagnificationSpec.scale > 1.0f;
    }

    private void onMagnificationRegionChanged(Region region) {
        synchronized (this.mLock) {
            if (this.mRegistered) {
                if (!this.mMagnificationRegion.equals(region)) {
                    this.mMagnificationRegion.set(region);
                    this.mMagnificationRegion.getBounds(this.mMagnificationBounds);
                    if (updateCurrentSpecWithOffsetsLocked(this.mCurrentMagnificationSpec.offsetX, this.mCurrentMagnificationSpec.offsetY)) {
                        sendSpecToAnimation(this.mCurrentMagnificationSpec, false);
                    }
                    onMagnificationChangedLocked();
                }
            }
        }
    }

    public boolean magnificationRegionContains(float f, float f2) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = this.mMagnificationRegion.contains((int) f, (int) f2);
        }
        return zContains;
    }

    public void getMagnificationBounds(Rect rect) {
        synchronized (this.mLock) {
            rect.set(this.mMagnificationBounds);
        }
    }

    public void getMagnificationRegion(Region region) {
        synchronized (this.mLock) {
            region.set(this.mMagnificationRegion);
        }
    }

    public float getScale() {
        return this.mCurrentMagnificationSpec.scale;
    }

    public float getOffsetX() {
        return this.mCurrentMagnificationSpec.offsetX;
    }

    public float getCenterX() {
        float fWidth;
        synchronized (this.mLock) {
            fWidth = (((this.mMagnificationBounds.width() / DEFAULT_MAGNIFICATION_SCALE) + this.mMagnificationBounds.left) - getOffsetX()) / getScale();
        }
        return fWidth;
    }

    public float getOffsetY() {
        return this.mCurrentMagnificationSpec.offsetY;
    }

    public float getCenterY() {
        float fHeight;
        synchronized (this.mLock) {
            fHeight = (((this.mMagnificationBounds.height() / DEFAULT_MAGNIFICATION_SCALE) + this.mMagnificationBounds.top) - getOffsetY()) / getScale();
        }
        return fHeight;
    }

    private float getSentScale() {
        return this.mSpecAnimationBridge.mSentMagnificationSpec.scale;
    }

    private float getSentOffsetX() {
        return this.mSpecAnimationBridge.mSentMagnificationSpec.offsetX;
    }

    private float getSentOffsetY() {
        return this.mSpecAnimationBridge.mSentMagnificationSpec.offsetY;
    }

    public boolean reset(boolean z) {
        boolean zResetLocked;
        synchronized (this.mLock) {
            zResetLocked = resetLocked(z);
        }
        return zResetLocked;
    }

    private boolean resetLocked(boolean z) {
        if (!this.mRegistered) {
            return false;
        }
        MagnificationSpec magnificationSpec = this.mCurrentMagnificationSpec;
        boolean z2 = !magnificationSpec.isNop();
        if (z2) {
            magnificationSpec.clear();
            onMagnificationChangedLocked();
        }
        this.mIdOfLastServiceToMagnify = -1;
        sendSpecToAnimation(magnificationSpec, z);
        return z2;
    }

    public boolean setScale(float f, float f2, float f3, boolean z, int i) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return false;
            }
            float fConstrain = MathUtils.constrain(f, 1.0f, 5.0f);
            this.mMagnificationRegion.getBounds(this.mTempRect);
            MagnificationSpec magnificationSpec = this.mCurrentMagnificationSpec;
            float f4 = magnificationSpec.scale;
            float fWidth = (((r10.width() / DEFAULT_MAGNIFICATION_SCALE) - magnificationSpec.offsetX) + r10.left) / f4;
            float fHeight = (((r10.height() / DEFAULT_MAGNIFICATION_SCALE) - magnificationSpec.offsetY) + r10.top) / f4;
            float f5 = (f2 - magnificationSpec.offsetX) / f4;
            float f6 = (f3 - magnificationSpec.offsetY) / f4;
            float f7 = f4 / fConstrain;
            this.mIdOfLastServiceToMagnify = i;
            return setScaleAndCenterLocked(fConstrain, f5 + ((fWidth - f5) * f7), ((fHeight - f6) * f7) + f6, z, i);
        }
    }

    public boolean setCenter(float f, float f2, boolean z, int i) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return false;
            }
            return setScaleAndCenterLocked(Float.NaN, f, f2, z, i);
        }
    }

    public boolean setScaleAndCenter(float f, float f2, float f3, boolean z, int i) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return false;
            }
            return setScaleAndCenterLocked(f, f2, f3, z, i);
        }
    }

    private boolean setScaleAndCenterLocked(float f, float f2, float f3, boolean z, int i) {
        boolean zUpdateMagnificationSpecLocked = updateMagnificationSpecLocked(f, f2, f3);
        sendSpecToAnimation(this.mCurrentMagnificationSpec, z);
        if (isMagnifying() && i != -1) {
            this.mIdOfLastServiceToMagnify = i;
        }
        return zUpdateMagnificationSpecLocked;
    }

    public void offsetMagnifiedRegion(float f, float f2, int i) {
        synchronized (this.mLock) {
            if (this.mRegistered) {
                if (updateCurrentSpecWithOffsetsLocked(this.mCurrentMagnificationSpec.offsetX - f, this.mCurrentMagnificationSpec.offsetY - f2)) {
                    onMagnificationChangedLocked();
                }
                if (i != -1) {
                    this.mIdOfLastServiceToMagnify = i;
                }
                sendSpecToAnimation(this.mCurrentMagnificationSpec, false);
            }
        }
    }

    public int getIdOfLastServiceToMagnify() {
        return this.mIdOfLastServiceToMagnify;
    }

    private void onMagnificationChangedLocked() {
        this.mAms.notifyMagnificationChanged(this.mMagnificationRegion, getScale(), getCenterX(), getCenterY());
        if (this.mUnregisterPending && !isMagnifying()) {
            unregisterInternalLocked();
        }
    }

    public void persistScale() {
        final float f = this.mCurrentMagnificationSpec.scale;
        final int i = this.mUserId;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                MagnificationController.this.mSettingsBridge.putMagnificationScale(f, i);
                return null;
            }
        }.execute(new Void[0]);
    }

    public float getPersistedScale() {
        return this.mSettingsBridge.getMagnificationScale(this.mUserId);
    }

    private boolean updateMagnificationSpecLocked(float f, float f2, float f3) {
        if (Float.isNaN(f2)) {
            f2 = getCenterX();
        }
        if (Float.isNaN(f3)) {
            f3 = getCenterY();
        }
        if (Float.isNaN(f)) {
            f = getScale();
        }
        boolean z = false;
        float fConstrain = MathUtils.constrain(f, 1.0f, 5.0f);
        if (Float.compare(this.mCurrentMagnificationSpec.scale, fConstrain) != 0) {
            this.mCurrentMagnificationSpec.scale = fConstrain;
            z = true;
        }
        boolean zUpdateCurrentSpecWithOffsetsLocked = updateCurrentSpecWithOffsetsLocked(((this.mMagnificationBounds.width() / DEFAULT_MAGNIFICATION_SCALE) + this.mMagnificationBounds.left) - (f2 * fConstrain), ((this.mMagnificationBounds.height() / DEFAULT_MAGNIFICATION_SCALE) + this.mMagnificationBounds.top) - (f3 * fConstrain)) | z;
        if (zUpdateCurrentSpecWithOffsetsLocked) {
            onMagnificationChangedLocked();
        }
        return zUpdateCurrentSpecWithOffsetsLocked;
    }

    private boolean updateCurrentSpecWithOffsetsLocked(float f, float f2) {
        boolean z;
        float fConstrain = MathUtils.constrain(f, getMinOffsetXLocked(), 0.0f);
        if (Float.compare(this.mCurrentMagnificationSpec.offsetX, fConstrain) != 0) {
            this.mCurrentMagnificationSpec.offsetX = fConstrain;
            z = true;
        } else {
            z = false;
        }
        float fConstrain2 = MathUtils.constrain(f2, getMinOffsetYLocked(), 0.0f);
        if (Float.compare(this.mCurrentMagnificationSpec.offsetY, fConstrain2) == 0) {
            return z;
        }
        this.mCurrentMagnificationSpec.offsetY = fConstrain2;
        return true;
    }

    private float getMinOffsetXLocked() {
        float fWidth = this.mMagnificationBounds.width();
        return fWidth - (this.mCurrentMagnificationSpec.scale * fWidth);
    }

    private float getMinOffsetYLocked() {
        float fHeight = this.mMagnificationBounds.height();
        return fHeight - (this.mCurrentMagnificationSpec.scale * fHeight);
    }

    public void setUserId(int i) {
        if (this.mUserId != i) {
            this.mUserId = i;
            synchronized (this.mLock) {
                if (isMagnifying()) {
                    reset(false);
                }
            }
        }
    }

    boolean resetIfNeeded(boolean z) {
        synchronized (this.mLock) {
            if (isMagnifying()) {
                reset(z);
                return true;
            }
            return false;
        }
    }

    void setForceShowMagnifiableBounds(boolean z) {
        if (this.mRegistered) {
            this.mWindowManager.setForceShowMagnifiableBounds(z);
        }
    }

    private void getMagnifiedFrameInContentCoordsLocked(Rect rect) {
        float sentScale = getSentScale();
        float sentOffsetX = getSentOffsetX();
        float sentOffsetY = getSentOffsetY();
        getMagnificationBounds(rect);
        rect.offset((int) (-sentOffsetX), (int) (-sentOffsetY));
        rect.scale(1.0f / sentScale);
    }

    private void requestRectangleOnScreen(int i, int i2, int i3, int i4) {
        float f;
        synchronized (this.mLock) {
            Rect rect = this.mTempRect;
            getMagnificationBounds(rect);
            if (rect.intersects(i, i2, i3, i4)) {
                Rect rect2 = this.mTempRect1;
                getMagnifiedFrameInContentCoordsLocked(rect2);
                float f2 = 0.0f;
                if (i3 - i > rect2.width()) {
                    if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 0) {
                        f = i - rect2.left;
                    } else {
                        f = i3 - rect2.right;
                    }
                } else if (i < rect2.left) {
                    f = i - rect2.left;
                } else if (i3 > rect2.right) {
                    f = i3 - rect2.right;
                } else {
                    f = 0.0f;
                }
                if (i4 - i2 > rect2.height() || i2 < rect2.top) {
                    f2 = i2 - rect2.top;
                } else if (i4 > rect2.bottom) {
                    f2 = i4 - rect2.bottom;
                }
                float scale = getScale();
                offsetMagnifiedRegion(f * scale, f2 * scale, -1);
            }
        }
    }

    private void sendSpecToAnimation(MagnificationSpec magnificationSpec, boolean z) {
        if (Thread.currentThread().getId() == this.mMainThreadId) {
            this.mSpecAnimationBridge.updateSentSpecMainThread(magnificationSpec, z);
        } else {
            this.mHandler.obtainMessage(1, z ? 1 : 0, 0, magnificationSpec).sendToTarget();
        }
    }

    private void onScreenTurnedOff() {
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 1:
                this.mSpecAnimationBridge.updateSentSpecMainThread((MagnificationSpec) message.obj, message.arg1 == 1);
                return true;
            case 2:
                resetIfNeeded(false);
                return true;
            case 3:
                SomeArgs someArgs = (SomeArgs) message.obj;
                Region region = (Region) someArgs.arg1;
                onMagnificationRegionChanged(region);
                region.recycle();
                someArgs.recycle();
                return true;
            case 4:
                SomeArgs someArgs2 = (SomeArgs) message.obj;
                requestRectangleOnScreen(someArgs2.argi1, someArgs2.argi2, someArgs2.argi3, someArgs2.argi4);
                someArgs2.recycle();
                return true;
            case 5:
                resetIfNeeded(true);
                return true;
            default:
                return true;
        }
    }

    public String toString() {
        return "MagnificationController{mCurrentMagnificationSpec=" + this.mCurrentMagnificationSpec + ", mMagnificationRegion=" + this.mMagnificationRegion + ", mMagnificationBounds=" + this.mMagnificationBounds + ", mUserId=" + this.mUserId + ", mIdOfLastServiceToMagnify=" + this.mIdOfLastServiceToMagnify + ", mRegistered=" + this.mRegistered + ", mUnregisterPending=" + this.mUnregisterPending + '}';
    }

    private static class SpecAnimationBridge implements ValueAnimator.AnimatorUpdateListener {

        @GuardedBy("mLock")
        private boolean mEnabled;
        private final MagnificationSpec mEndMagnificationSpec;
        private final Object mLock;
        private final MagnificationSpec mSentMagnificationSpec;
        private final MagnificationSpec mStartMagnificationSpec;
        private final MagnificationSpec mTmpMagnificationSpec;
        private final ValueAnimator mValueAnimator;
        private final WindowManagerInternal mWindowManager;

        private SpecAnimationBridge(Context context, Object obj, WindowManagerInternal windowManagerInternal, ValueAnimator valueAnimator) {
            this.mSentMagnificationSpec = MagnificationSpec.obtain();
            this.mStartMagnificationSpec = MagnificationSpec.obtain();
            this.mEndMagnificationSpec = MagnificationSpec.obtain();
            this.mTmpMagnificationSpec = MagnificationSpec.obtain();
            this.mEnabled = false;
            this.mLock = obj;
            this.mWindowManager = windowManagerInternal;
            long integer = context.getResources().getInteger(R.integer.config_longAnimTime);
            this.mValueAnimator = valueAnimator;
            this.mValueAnimator.setDuration(integer);
            this.mValueAnimator.setInterpolator(new DecelerateInterpolator(2.5f));
            this.mValueAnimator.setFloatValues(0.0f, 1.0f);
            this.mValueAnimator.addUpdateListener(this);
        }

        public void setEnabled(boolean z) {
            synchronized (this.mLock) {
                if (z != this.mEnabled) {
                    this.mEnabled = z;
                    if (!this.mEnabled) {
                        this.mSentMagnificationSpec.clear();
                        this.mWindowManager.setMagnificationSpec(this.mSentMagnificationSpec);
                    }
                }
            }
        }

        public void updateSentSpecMainThread(MagnificationSpec magnificationSpec, boolean z) {
            if (this.mValueAnimator.isRunning()) {
                this.mValueAnimator.cancel();
            }
            synchronized (this.mLock) {
                if (!this.mSentMagnificationSpec.equals(magnificationSpec)) {
                    if (z) {
                        animateMagnificationSpecLocked(magnificationSpec);
                    } else {
                        setMagnificationSpecLocked(magnificationSpec);
                    }
                }
            }
        }

        @GuardedBy("mLock")
        private void setMagnificationSpecLocked(MagnificationSpec magnificationSpec) {
            if (this.mEnabled) {
                this.mSentMagnificationSpec.setTo(magnificationSpec);
                this.mWindowManager.setMagnificationSpec(magnificationSpec);
            }
        }

        private void animateMagnificationSpecLocked(MagnificationSpec magnificationSpec) {
            this.mEndMagnificationSpec.setTo(magnificationSpec);
            this.mStartMagnificationSpec.setTo(this.mSentMagnificationSpec);
            this.mValueAnimator.start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            synchronized (this.mLock) {
                if (this.mEnabled) {
                    float animatedFraction = valueAnimator.getAnimatedFraction();
                    this.mTmpMagnificationSpec.scale = this.mStartMagnificationSpec.scale + ((this.mEndMagnificationSpec.scale - this.mStartMagnificationSpec.scale) * animatedFraction);
                    this.mTmpMagnificationSpec.offsetX = this.mStartMagnificationSpec.offsetX + ((this.mEndMagnificationSpec.offsetX - this.mStartMagnificationSpec.offsetX) * animatedFraction);
                    this.mTmpMagnificationSpec.offsetY = this.mStartMagnificationSpec.offsetY + ((this.mEndMagnificationSpec.offsetY - this.mStartMagnificationSpec.offsetY) * animatedFraction);
                    synchronized (this.mLock) {
                        setMagnificationSpecLocked(this.mTmpMagnificationSpec);
                    }
                }
            }
        }
    }

    private static class ScreenStateObserver extends BroadcastReceiver {
        private final Context mContext;
        private final MagnificationController mController;

        public ScreenStateObserver(Context context, MagnificationController magnificationController) {
            this.mContext = context;
            this.mController = magnificationController;
        }

        public void register() {
            this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.SCREEN_OFF"));
        }

        public void unregister() {
            this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mController.onScreenTurnedOff();
        }
    }

    public static class SettingsBridge {
        private final ContentResolver mContentResolver;

        public SettingsBridge(ContentResolver contentResolver) {
            this.mContentResolver = contentResolver;
        }

        public void putMagnificationScale(float f, int i) {
            Settings.Secure.putFloatForUser(this.mContentResolver, "accessibility_display_magnification_scale", f, i);
        }

        public float getMagnificationScale(int i) {
            return Settings.Secure.getFloatForUser(this.mContentResolver, "accessibility_display_magnification_scale", MagnificationController.DEFAULT_MAGNIFICATION_SCALE, i);
        }
    }
}
