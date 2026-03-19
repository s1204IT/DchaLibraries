package com.android.server.accessibility;

import android.R;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
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
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.MathUtils;
import android.util.Property;
import android.view.MagnificationSpec;
import android.view.WindowManagerInternal;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import java.util.Locale;

class MagnificationController {
    private static final boolean DEBUG_SET_MAGNIFICATION_SPEC = false;
    private static final float DEFAULT_MAGNIFICATION_SCALE = 2.0f;
    private static final int DEFAULT_SCREEN_MAGNIFICATION_AUTO_UPDATE = 1;
    private static final int INVALID_ID = -1;
    private static final String LOG_TAG = "MagnificationController";
    private static final float MAX_SCALE = 5.0f;
    private static final float MIN_PERSISTED_SCALE = 2.0f;
    private static final float MIN_SCALE = 1.0f;
    private final AccessibilityManagerService mAms;
    private final ContentResolver mContentResolver;
    private final Object mLock;
    private boolean mRegistered;
    private final ScreenStateObserver mScreenStateObserver;
    private final SpecAnimationBridge mSpecAnimationBridge;
    private boolean mUnregisterPending;
    private int mUserId;
    private final WindowStateObserver mWindowStateObserver;
    private final MagnificationSpec mCurrentMagnificationSpec = MagnificationSpec.obtain();
    private final Region mMagnificationRegion = Region.obtain();
    private final Rect mMagnificationBounds = new Rect();
    private final Rect mTempRect = new Rect();
    private final Rect mTempRect1 = new Rect();
    private int mIdOfLastServiceToMagnify = -1;

    public MagnificationController(Context context, AccessibilityManagerService ams, Object lock) {
        this.mAms = ams;
        this.mContentResolver = context.getContentResolver();
        this.mScreenStateObserver = new ScreenStateObserver(context, this);
        this.mWindowStateObserver = new WindowStateObserver(context, this);
        this.mLock = lock;
        this.mSpecAnimationBridge = new SpecAnimationBridge(context, this.mLock, null);
    }

    public void register() {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                this.mScreenStateObserver.register();
                this.mWindowStateObserver.register();
                this.mSpecAnimationBridge.setEnabled(true);
                this.mWindowStateObserver.getMagnificationRegion(this.mMagnificationRegion);
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
            this.mWindowStateObserver.unregister();
            this.mMagnificationRegion.setEmpty();
            this.mRegistered = false;
        }
        this.mUnregisterPending = false;
    }

    public boolean isMagnifying() {
        return this.mCurrentMagnificationSpec.scale > MIN_SCALE;
    }

    private void onMagnificationRegionChanged(Region magnified, boolean updateSpec) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return;
            }
            boolean magnificationChanged = false;
            boolean boundsChanged = false;
            if (!this.mMagnificationRegion.equals(magnified)) {
                this.mMagnificationRegion.set(magnified);
                this.mMagnificationRegion.getBounds(this.mMagnificationBounds);
                boundsChanged = true;
            }
            if (updateSpec) {
                MagnificationSpec sentSpec = this.mSpecAnimationBridge.mSentMagnificationSpec;
                float scale = sentSpec.scale;
                float offsetX = sentSpec.offsetX;
                float offsetY = sentSpec.offsetY;
                float centerX = (((this.mMagnificationBounds.width() / 2.0f) + this.mMagnificationBounds.left) - offsetX) / scale;
                float centerY = (((this.mMagnificationBounds.height() / 2.0f) + this.mMagnificationBounds.top) - offsetY) / scale;
                magnificationChanged = setScaleAndCenterLocked(scale, centerX, centerY, false, -1);
            }
            if (boundsChanged && updateSpec && !magnificationChanged) {
                onMagnificationChangedLocked();
            }
        }
    }

    public boolean magnificationRegionContains(float x, float y) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = this.mMagnificationRegion.contains((int) x, (int) y);
        }
        return zContains;
    }

    public void getMagnificationBounds(Rect outBounds) {
        synchronized (this.mLock) {
            outBounds.set(this.mMagnificationBounds);
        }
    }

    public void getMagnificationRegion(Region outRegion) {
        synchronized (this.mLock) {
            outRegion.set(this.mMagnificationRegion);
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
            fWidth = (((this.mMagnificationBounds.width() / 2.0f) + this.mMagnificationBounds.left) - getOffsetX()) / getScale();
        }
        return fWidth;
    }

    public float getOffsetY() {
        return this.mCurrentMagnificationSpec.offsetY;
    }

    public float getCenterY() {
        float fHeight;
        synchronized (this.mLock) {
            fHeight = (((this.mMagnificationBounds.height() / 2.0f) + this.mMagnificationBounds.top) - getOffsetY()) / getScale();
        }
        return fHeight;
    }

    public float getSentScale() {
        return this.mSpecAnimationBridge.mSentMagnificationSpec.scale;
    }

    public float getSentOffsetX() {
        return this.mSpecAnimationBridge.mSentMagnificationSpec.offsetX;
    }

    public float getSentOffsetY() {
        return this.mSpecAnimationBridge.mSentMagnificationSpec.offsetY;
    }

    public boolean reset(boolean animate) {
        boolean zResetLocked;
        synchronized (this.mLock) {
            zResetLocked = resetLocked(animate);
        }
        return zResetLocked;
    }

    private boolean resetLocked(boolean animate) {
        if (!this.mRegistered) {
            return false;
        }
        MagnificationSpec spec = this.mCurrentMagnificationSpec;
        boolean changed = spec.isNop() ? false : true;
        if (changed) {
            spec.clear();
            onMagnificationChangedLocked();
        }
        this.mIdOfLastServiceToMagnify = -1;
        this.mSpecAnimationBridge.updateSentSpec(spec, animate);
        return changed;
    }

    public boolean setScale(float scale, float pivotX, float pivotY, boolean animate, int id) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return false;
            }
            float scale2 = MathUtils.constrain(scale, MIN_SCALE, MAX_SCALE);
            Rect viewport = this.mTempRect;
            this.mMagnificationRegion.getBounds(viewport);
            MagnificationSpec spec = this.mCurrentMagnificationSpec;
            float oldScale = spec.scale;
            float oldCenterX = ((viewport.width() / 2.0f) - spec.offsetX) / oldScale;
            float oldCenterY = ((viewport.height() / 2.0f) - spec.offsetY) / oldScale;
            float normPivotX = (pivotX - spec.offsetX) / oldScale;
            float normPivotY = (pivotY - spec.offsetY) / oldScale;
            float offsetX = (oldCenterX - normPivotX) * (oldScale / scale2);
            float offsetY = (oldCenterY - normPivotY) * (oldScale / scale2);
            float centerX = normPivotX + offsetX;
            float centerY = normPivotY + offsetY;
            this.mIdOfLastServiceToMagnify = id;
            return setScaleAndCenterLocked(scale2, centerX, centerY, animate, id);
        }
    }

    public boolean setCenter(float centerX, float centerY, boolean animate, int id) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return false;
            }
            return setScaleAndCenterLocked(Float.NaN, centerX, centerY, animate, id);
        }
    }

    public boolean setScaleAndCenter(float scale, float centerX, float centerY, boolean animate, int id) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return false;
            }
            return setScaleAndCenterLocked(scale, centerX, centerY, animate, id);
        }
    }

    private boolean setScaleAndCenterLocked(float scale, float centerX, float centerY, boolean animate, int id) {
        boolean changed = updateMagnificationSpecLocked(scale, centerX, centerY);
        this.mSpecAnimationBridge.updateSentSpec(this.mCurrentMagnificationSpec, animate);
        if (isMagnifying() && id != -1) {
            this.mIdOfLastServiceToMagnify = id;
        }
        return changed;
    }

    public void offsetMagnifiedRegionCenter(float offsetX, float offsetY, int id) {
        synchronized (this.mLock) {
            if (!this.mRegistered) {
                return;
            }
            MagnificationSpec currSpec = this.mCurrentMagnificationSpec;
            float nonNormOffsetX = currSpec.offsetX - offsetX;
            currSpec.offsetX = MathUtils.constrain(nonNormOffsetX, getMinOffsetXLocked(), 0.0f);
            float nonNormOffsetY = currSpec.offsetY - offsetY;
            currSpec.offsetY = MathUtils.constrain(nonNormOffsetY, getMinOffsetYLocked(), 0.0f);
            if (id != -1) {
                this.mIdOfLastServiceToMagnify = id;
            }
            this.mSpecAnimationBridge.updateSentSpec(currSpec, false);
        }
    }

    public int getIdOfLastServiceToMagnify() {
        return this.mIdOfLastServiceToMagnify;
    }

    private void onMagnificationChangedLocked() {
        this.mAms.onMagnificationStateChanged();
        this.mAms.notifyMagnificationChanged(this.mMagnificationRegion, getScale(), getCenterX(), getCenterY());
        if (!this.mUnregisterPending || isMagnifying()) {
            return;
        }
        unregisterInternalLocked();
    }

    public void persistScale() {
        final float scale = this.mCurrentMagnificationSpec.scale;
        final int userId = this.mUserId;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Settings.Secure.putFloatForUser(MagnificationController.this.mContentResolver, "accessibility_display_magnification_scale", scale, userId);
                return null;
            }
        }.execute(new Void[0]);
    }

    public float getPersistedScale() {
        return Settings.Secure.getFloatForUser(this.mContentResolver, "accessibility_display_magnification_scale", 2.0f, this.mUserId);
    }

    private boolean updateMagnificationSpecLocked(float scale, float centerX, float centerY) {
        if (Float.isNaN(centerX)) {
            centerX = getCenterX();
        }
        if (Float.isNaN(centerY)) {
            centerY = getCenterY();
        }
        if (Float.isNaN(scale)) {
            scale = getScale();
        }
        if (!magnificationRegionContains(centerX, centerY)) {
            return false;
        }
        MagnificationSpec currSpec = this.mCurrentMagnificationSpec;
        boolean changed = false;
        float normScale = MathUtils.constrain(scale, MIN_SCALE, MAX_SCALE);
        if (Float.compare(currSpec.scale, normScale) != 0) {
            currSpec.scale = normScale;
            changed = true;
        }
        float nonNormOffsetX = ((this.mMagnificationBounds.width() / 2.0f) + this.mMagnificationBounds.left) - (centerX * scale);
        float offsetX = MathUtils.constrain(nonNormOffsetX, getMinOffsetXLocked(), 0.0f);
        if (Float.compare(currSpec.offsetX, offsetX) != 0) {
            currSpec.offsetX = offsetX;
            changed = true;
        }
        float nonNormOffsetY = ((this.mMagnificationBounds.height() / 2.0f) + this.mMagnificationBounds.top) - (centerY * scale);
        float offsetY = MathUtils.constrain(nonNormOffsetY, getMinOffsetYLocked(), 0.0f);
        if (Float.compare(currSpec.offsetY, offsetY) != 0) {
            currSpec.offsetY = offsetY;
            changed = true;
        }
        if (changed) {
            onMagnificationChangedLocked();
        }
        return changed;
    }

    private float getMinOffsetXLocked() {
        float viewportWidth = this.mMagnificationBounds.width();
        return viewportWidth - (this.mCurrentMagnificationSpec.scale * viewportWidth);
    }

    private float getMinOffsetYLocked() {
        float viewportHeight = this.mMagnificationBounds.height();
        return viewportHeight - (this.mCurrentMagnificationSpec.scale * viewportHeight);
    }

    public void setUserId(int userId) {
        if (this.mUserId == userId) {
            return;
        }
        this.mUserId = userId;
        synchronized (this.mLock) {
            if (isMagnifying()) {
                reset(false);
            }
        }
    }

    private boolean isScreenMagnificationAutoUpdateEnabled() {
        return Settings.Secure.getInt(this.mContentResolver, "accessibility_display_magnification_auto_update", 1) == 1;
    }

    boolean resetIfNeeded(boolean animate) {
        synchronized (this.mLock) {
            if (isMagnifying() && isScreenMagnificationAutoUpdateEnabled()) {
                reset(animate);
                return true;
            }
            return false;
        }
    }

    private void getMagnifiedFrameInContentCoordsLocked(Rect outFrame) {
        float scale = getSentScale();
        float offsetX = getSentOffsetX();
        float offsetY = getSentOffsetY();
        getMagnificationBounds(outFrame);
        outFrame.offset((int) (-offsetX), (int) (-offsetY));
        outFrame.scale(MIN_SCALE / scale);
    }

    private void requestRectangleOnScreen(int left, int top, int right, int bottom) {
        float scrollX;
        float scrollY;
        synchronized (this.mLock) {
            Rect magnifiedFrame = this.mTempRect;
            getMagnificationBounds(magnifiedFrame);
            if (!magnifiedFrame.intersects(left, top, right, bottom)) {
                return;
            }
            Rect magnifFrameInScreenCoords = this.mTempRect1;
            getMagnifiedFrameInContentCoordsLocked(magnifFrameInScreenCoords);
            if (right - left > magnifFrameInScreenCoords.width()) {
                int direction = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
                if (direction == 0) {
                    scrollX = left - magnifFrameInScreenCoords.left;
                } else {
                    scrollX = right - magnifFrameInScreenCoords.right;
                }
            } else if (left < magnifFrameInScreenCoords.left) {
                scrollX = left - magnifFrameInScreenCoords.left;
            } else if (right > magnifFrameInScreenCoords.right) {
                scrollX = right - magnifFrameInScreenCoords.right;
            } else {
                scrollX = 0.0f;
            }
            if (bottom - top > magnifFrameInScreenCoords.height() || top < magnifFrameInScreenCoords.top) {
                scrollY = top - magnifFrameInScreenCoords.top;
            } else if (bottom > magnifFrameInScreenCoords.bottom) {
                scrollY = bottom - magnifFrameInScreenCoords.bottom;
            } else {
                scrollY = 0.0f;
            }
            float scale = getScale();
            offsetMagnifiedRegionCenter(scrollX * scale, scrollY * scale, -1);
        }
    }

    private static class SpecAnimationBridge {
        private static final int ACTION_UPDATE_SPEC = 1;

        @GuardedBy("mLock")
        private boolean mEnabled;
        private final Handler mHandler;
        private final Object mLock;
        private final long mMainThreadId;
        private final MagnificationSpec mSentMagnificationSpec;
        private final ValueAnimator mTransformationAnimator;
        private final WindowManagerInternal mWindowManager;

        SpecAnimationBridge(Context context, Object lock, SpecAnimationBridge specAnimationBridge) {
            this(context, lock);
        }

        private SpecAnimationBridge(Context context, Object lock) {
            this.mSentMagnificationSpec = MagnificationSpec.obtain();
            this.mEnabled = false;
            this.mLock = lock;
            Looper mainLooper = context.getMainLooper();
            this.mMainThreadId = mainLooper.getThread().getId();
            this.mHandler = new UpdateHandler(context);
            this.mWindowManager = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            MagnificationSpecProperty property = new MagnificationSpecProperty();
            MagnificationSpecEvaluator evaluator = new MagnificationSpecEvaluator(null);
            long animationDuration = context.getResources().getInteger(R.integer.config_longAnimTime);
            this.mTransformationAnimator = ObjectAnimator.ofObject(this, property, evaluator, this.mSentMagnificationSpec);
            this.mTransformationAnimator.setDuration(animationDuration);
            this.mTransformationAnimator.setInterpolator(new DecelerateInterpolator(2.5f));
        }

        public void setEnabled(boolean enabled) {
            synchronized (this.mLock) {
                if (enabled != this.mEnabled) {
                    this.mEnabled = enabled;
                    if (!this.mEnabled) {
                        this.mSentMagnificationSpec.clear();
                        this.mWindowManager.setMagnificationSpec(this.mSentMagnificationSpec);
                    }
                }
            }
        }

        public void updateSentSpec(MagnificationSpec spec, boolean animate) {
            if (Thread.currentThread().getId() == this.mMainThreadId) {
                updateSentSpecInternal(spec, animate);
            } else {
                this.mHandler.obtainMessage(1, animate ? 1 : 0, 0, spec).sendToTarget();
            }
        }

        private void updateSentSpecInternal(MagnificationSpec spec, boolean animate) {
            if (this.mTransformationAnimator.isRunning()) {
                this.mTransformationAnimator.cancel();
            }
            synchronized (this.mLock) {
                boolean changed = !this.mSentMagnificationSpec.equals(spec);
                if (changed) {
                    if (animate) {
                        animateMagnificationSpecLocked(spec);
                    } else {
                        setMagnificationSpecLocked(spec);
                    }
                }
            }
        }

        private void animateMagnificationSpecLocked(MagnificationSpec toSpec) {
            this.mTransformationAnimator.setObjectValues(this.mSentMagnificationSpec, toSpec);
            this.mTransformationAnimator.start();
        }

        private void setMagnificationSpecLocked(MagnificationSpec spec) {
            if (!this.mEnabled) {
                return;
            }
            this.mSentMagnificationSpec.setTo(spec);
            this.mWindowManager.setMagnificationSpec(spec);
        }

        private class UpdateHandler extends Handler {
            public UpdateHandler(Context context) {
                super(context.getMainLooper());
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        boolean animate = msg.arg1 == 1;
                        MagnificationSpec spec = (MagnificationSpec) msg.obj;
                        SpecAnimationBridge.this.updateSentSpecInternal(spec, animate);
                        break;
                }
            }
        }

        private static class MagnificationSpecProperty extends Property<SpecAnimationBridge, MagnificationSpec> {
            public MagnificationSpecProperty() {
                super(MagnificationSpec.class, "spec");
            }

            @Override
            public MagnificationSpec get(SpecAnimationBridge object) {
                MagnificationSpec magnificationSpec;
                synchronized (object.mLock) {
                    magnificationSpec = object.mSentMagnificationSpec;
                }
                return magnificationSpec;
            }

            @Override
            public void set(SpecAnimationBridge object, MagnificationSpec value) {
                synchronized (object.mLock) {
                    object.setMagnificationSpecLocked(value);
                }
            }
        }

        private static class MagnificationSpecEvaluator implements TypeEvaluator<MagnificationSpec> {
            private final MagnificationSpec mTempSpec;

            MagnificationSpecEvaluator(MagnificationSpecEvaluator magnificationSpecEvaluator) {
                this();
            }

            private MagnificationSpecEvaluator() {
                this.mTempSpec = MagnificationSpec.obtain();
            }

            @Override
            public MagnificationSpec evaluate(float fraction, MagnificationSpec fromSpec, MagnificationSpec toSpec) {
                MagnificationSpec result = this.mTempSpec;
                result.scale = fromSpec.scale + ((toSpec.scale - fromSpec.scale) * fraction);
                result.offsetX = fromSpec.offsetX + ((toSpec.offsetX - fromSpec.offsetX) * fraction);
                result.offsetY = fromSpec.offsetY + ((toSpec.offsetY - fromSpec.offsetY) * fraction);
                return result;
            }
        }
    }

    private static class ScreenStateObserver extends BroadcastReceiver {
        private static final int MESSAGE_ON_SCREEN_STATE_CHANGE = 1;
        private final Context mContext;
        private final MagnificationController mController;
        private final Handler mHandler;

        public ScreenStateObserver(Context context, MagnificationController controller) {
            this.mContext = context;
            this.mController = controller;
            this.mHandler = new StateChangeHandler(context);
        }

        public void register() {
            this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.SCREEN_OFF"));
        }

        public void unregister() {
            this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mHandler.obtainMessage(1, intent.getAction()).sendToTarget();
        }

        private void handleOnScreenStateChange() {
            this.mController.resetIfNeeded(false);
        }

        private class StateChangeHandler extends Handler {
            public StateChangeHandler(Context context) {
                super(context.getMainLooper());
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        ScreenStateObserver.this.handleOnScreenStateChange();
                        break;
                }
            }
        }
    }

    private static class WindowStateObserver implements WindowManagerInternal.MagnificationCallbacks {
        private static final int MESSAGE_ON_MAGNIFIED_BOUNDS_CHANGED = 1;
        private static final int MESSAGE_ON_RECTANGLE_ON_SCREEN_REQUESTED = 2;
        private static final int MESSAGE_ON_ROTATION_CHANGED = 4;
        private static final int MESSAGE_ON_USER_CONTEXT_CHANGED = 3;
        private final MagnificationController mController;
        private final Handler mHandler;
        private boolean mSpecIsDirty;
        private final WindowManagerInternal mWindowManager = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);

        public WindowStateObserver(Context context, MagnificationController controller) {
            this.mController = controller;
            this.mHandler = new CallbackHandler(context);
        }

        public void register() {
            this.mWindowManager.setMagnificationCallbacks(this);
        }

        public void unregister() {
            this.mWindowManager.setMagnificationCallbacks((WindowManagerInternal.MagnificationCallbacks) null);
        }

        public void onMagnificationRegionChanged(Region magnificationRegion) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Region.obtain(magnificationRegion);
            this.mHandler.obtainMessage(1, args).sendToTarget();
        }

        private void handleOnMagnifiedBoundsChanged(Region magnificationRegion) {
            this.mController.onMagnificationRegionChanged(magnificationRegion, this.mSpecIsDirty);
            this.mSpecIsDirty = false;
        }

        public void onRectangleOnScreenRequested(int left, int top, int right, int bottom) {
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = left;
            args.argi2 = top;
            args.argi3 = right;
            args.argi4 = bottom;
            this.mHandler.obtainMessage(2, args).sendToTarget();
        }

        private void handleOnRectangleOnScreenRequested(int left, int top, int right, int bottom) {
            this.mController.requestRectangleOnScreen(left, top, right, bottom);
        }

        public void onRotationChanged(int rotation) {
            this.mHandler.obtainMessage(4, rotation, 0).sendToTarget();
        }

        private void handleOnRotationChanged() {
            this.mSpecIsDirty = this.mController.resetIfNeeded(true) ? false : true;
        }

        public void onUserContextChanged() {
            this.mHandler.sendEmptyMessage(3);
        }

        private void handleOnUserContextChanged() {
            this.mController.resetIfNeeded(true);
        }

        public void getMagnificationRegion(Region outMagnificationRegion) {
            this.mWindowManager.getMagnificationRegion(outMagnificationRegion);
        }

        private class CallbackHandler extends Handler {
            public CallbackHandler(Context context) {
                super(context.getMainLooper());
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        Region magnifiedBounds = (Region) ((SomeArgs) message.obj).arg1;
                        WindowStateObserver.this.handleOnMagnifiedBoundsChanged(magnifiedBounds);
                        magnifiedBounds.recycle();
                        break;
                    case 2:
                        SomeArgs args = (SomeArgs) message.obj;
                        int left = args.argi1;
                        int top = args.argi2;
                        int right = args.argi3;
                        int bottom = args.argi4;
                        WindowStateObserver.this.handleOnRectangleOnScreenRequested(left, top, right, bottom);
                        args.recycle();
                        break;
                    case 3:
                        WindowStateObserver.this.handleOnUserContextChanged();
                        break;
                    case 4:
                        WindowStateObserver.this.handleOnRotationChanged();
                        break;
                }
            }
        }
    }
}
