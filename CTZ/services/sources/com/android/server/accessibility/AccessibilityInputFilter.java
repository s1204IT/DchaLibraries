package com.android.server.accessibility;

import android.content.Context;
import android.os.PowerManager;
import android.util.Pools;
import android.util.SparseBooleanArray;
import android.view.Choreographer;
import android.view.InputEvent;
import android.view.InputFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;

class AccessibilityInputFilter extends InputFilter implements EventStreamTransformation {
    private static final boolean DEBUG = false;
    static final int FEATURES_AFFECTING_MOTION_EVENTS = 91;
    static final int FLAG_FEATURE_AUTOCLICK = 8;
    static final int FLAG_FEATURE_CONTROL_SCREEN_MAGNIFIER = 32;
    static final int FLAG_FEATURE_FILTER_KEY_EVENTS = 4;
    static final int FLAG_FEATURE_INJECT_MOTION_EVENTS = 16;
    static final int FLAG_FEATURE_SCREEN_MAGNIFIER = 1;
    static final int FLAG_FEATURE_TOUCH_EXPLORATION = 2;
    static final int FLAG_FEATURE_TRIGGERED_SCREEN_MAGNIFIER = 64;
    private static final String TAG = AccessibilityInputFilter.class.getSimpleName();
    private final AccessibilityManagerService mAms;
    private AutoclickController mAutoclickController;
    private final Choreographer mChoreographer;
    private final Context mContext;
    private int mEnabledFeatures;
    private EventStreamTransformation mEventHandler;
    private MotionEventHolder mEventQueue;
    private boolean mInstalled;
    private KeyboardInterceptor mKeyboardInterceptor;
    private EventStreamState mKeyboardStreamState;
    private MagnificationGestureHandler mMagnificationGestureHandler;
    private MotionEventInjector mMotionEventInjector;
    private EventStreamState mMouseStreamState;
    private final PowerManager mPm;
    private final Runnable mProcessBatchedEventsRunnable;
    private TouchExplorer mTouchExplorer;
    private EventStreamState mTouchScreenStreamState;
    private int mUserId;

    AccessibilityInputFilter(Context context, AccessibilityManagerService accessibilityManagerService) {
        super(context.getMainLooper());
        this.mProcessBatchedEventsRunnable = new Runnable() {
            @Override
            public void run() {
                AccessibilityInputFilter.this.processBatchedEvents(AccessibilityInputFilter.this.mChoreographer.getFrameTimeNanos());
                if (AccessibilityInputFilter.this.mEventQueue != null) {
                    AccessibilityInputFilter.this.scheduleProcessBatchedEvents();
                }
            }
        };
        this.mContext = context;
        this.mAms = accessibilityManagerService;
        this.mPm = (PowerManager) context.getSystemService("power");
        this.mChoreographer = Choreographer.getInstance();
    }

    public void onInstalled() {
        this.mInstalled = true;
        disableFeatures();
        enableFeatures();
        super.onInstalled();
    }

    public void onUninstalled() {
        this.mInstalled = false;
        disableFeatures();
        super.onUninstalled();
    }

    public void onInputEvent(InputEvent inputEvent, int i) {
        if (this.mEventHandler == null) {
            super.onInputEvent(inputEvent, i);
            return;
        }
        EventStreamState eventStreamState = getEventStreamState(inputEvent);
        if (eventStreamState == null) {
            super.onInputEvent(inputEvent, i);
            return;
        }
        int source = inputEvent.getSource();
        if ((1073741824 & i) == 0) {
            eventStreamState.reset();
            this.mEventHandler.clearEvents(source);
            super.onInputEvent(inputEvent, i);
            return;
        }
        if (eventStreamState.updateDeviceId(inputEvent.getDeviceId())) {
            this.mEventHandler.clearEvents(source);
        }
        if (!eventStreamState.deviceIdValid()) {
            super.onInputEvent(inputEvent, i);
            return;
        }
        if (inputEvent instanceof MotionEvent) {
            if ((this.mEnabledFeatures & FEATURES_AFFECTING_MOTION_EVENTS) != 0) {
                processMotionEvent(eventStreamState, (MotionEvent) inputEvent, i);
                return;
            } else {
                super.onInputEvent(inputEvent, i);
                return;
            }
        }
        if (inputEvent instanceof KeyEvent) {
            processKeyEvent(eventStreamState, (KeyEvent) inputEvent, i);
        }
    }

    private EventStreamState getEventStreamState(InputEvent inputEvent) {
        if (inputEvent instanceof MotionEvent) {
            if (inputEvent.isFromSource(UsbACInterface.FORMAT_II_AC3)) {
                if (this.mTouchScreenStreamState == null) {
                    this.mTouchScreenStreamState = new TouchScreenEventStreamState();
                }
                return this.mTouchScreenStreamState;
            }
            if (inputEvent.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                if (this.mMouseStreamState == null) {
                    this.mMouseStreamState = new MouseEventStreamState();
                }
                return this.mMouseStreamState;
            }
            return null;
        }
        if ((inputEvent instanceof KeyEvent) && inputEvent.isFromSource(UsbTerminalTypes.TERMINAL_USB_STREAMING)) {
            if (this.mKeyboardStreamState == null) {
                this.mKeyboardStreamState = new KeyboardEventStreamState();
            }
            return this.mKeyboardStreamState;
        }
        return null;
    }

    private void processMotionEvent(EventStreamState eventStreamState, MotionEvent motionEvent, int i) {
        if (!eventStreamState.shouldProcessScroll() && motionEvent.getActionMasked() == 8) {
            super.onInputEvent(motionEvent, i);
        } else {
            if (!eventStreamState.shouldProcessMotionEvent(motionEvent)) {
                return;
            }
            batchMotionEvent(motionEvent, i);
        }
    }

    private void processKeyEvent(EventStreamState eventStreamState, KeyEvent keyEvent, int i) {
        if (!eventStreamState.shouldProcessKeyEvent(keyEvent)) {
            super.onInputEvent(keyEvent, i);
        } else {
            this.mEventHandler.onKeyEvent(keyEvent, i);
        }
    }

    private void scheduleProcessBatchedEvents() {
        this.mChoreographer.postCallback(0, this.mProcessBatchedEventsRunnable, null);
    }

    private void batchMotionEvent(MotionEvent motionEvent, int i) {
        if (this.mEventQueue == null) {
            this.mEventQueue = MotionEventHolder.obtain(motionEvent, i);
            scheduleProcessBatchedEvents();
        } else {
            if (this.mEventQueue.event.addBatch(motionEvent)) {
                return;
            }
            MotionEventHolder motionEventHolderObtain = MotionEventHolder.obtain(motionEvent, i);
            motionEventHolderObtain.next = this.mEventQueue;
            this.mEventQueue.previous = motionEventHolderObtain;
            this.mEventQueue = motionEventHolderObtain;
        }
    }

    private void processBatchedEvents(long j) {
        MotionEventHolder motionEventHolder = this.mEventQueue;
        if (motionEventHolder == null) {
            return;
        }
        while (motionEventHolder.next != null) {
            motionEventHolder = motionEventHolder.next;
        }
        while (motionEventHolder != null) {
            if (motionEventHolder.event.getEventTimeNano() >= j) {
                motionEventHolder.next = null;
                return;
            }
            handleMotionEvent(motionEventHolder.event, motionEventHolder.policyFlags);
            MotionEventHolder motionEventHolder2 = motionEventHolder.previous;
            motionEventHolder.recycle();
            motionEventHolder = motionEventHolder2;
        }
        this.mEventQueue = null;
    }

    private void handleMotionEvent(MotionEvent motionEvent, int i) {
        if (this.mEventHandler != null) {
            this.mPm.userActivity(motionEvent.getEventTime(), false);
            MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
            this.mEventHandler.onMotionEvent(motionEventObtain, motionEvent, i);
            motionEventObtain.recycle();
        }
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        sendInputEvent(motionEvent, i);
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent, int i) {
        sendInputEvent(keyEvent, i);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void setNext(EventStreamTransformation eventStreamTransformation) {
    }

    @Override
    public EventStreamTransformation getNext() {
        return null;
    }

    @Override
    public void clearEvents(int i) {
    }

    void setUserAndEnabledFeatures(int i, int i2) {
        if (this.mEnabledFeatures == i2 && this.mUserId == i) {
            return;
        }
        if (this.mInstalled) {
            disableFeatures();
        }
        this.mUserId = i;
        this.mEnabledFeatures = i2;
        if (this.mInstalled) {
            enableFeatures();
        }
    }

    void notifyAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (this.mEventHandler != null) {
            this.mEventHandler.onAccessibilityEvent(accessibilityEvent);
        }
    }

    void notifyAccessibilityButtonClicked() {
        if (this.mMagnificationGestureHandler != null) {
            this.mMagnificationGestureHandler.notifyShortcutTriggered();
        }
    }

    private void enableFeatures() {
        resetStreamState();
        if ((this.mEnabledFeatures & 8) != 0) {
            this.mAutoclickController = new AutoclickController(this.mContext, this.mUserId);
            addFirstEventHandler(this.mAutoclickController);
        }
        if ((this.mEnabledFeatures & 2) != 0) {
            this.mTouchExplorer = new TouchExplorer(this.mContext, this.mAms);
            addFirstEventHandler(this.mTouchExplorer);
        }
        if ((this.mEnabledFeatures & 32) != 0 || (this.mEnabledFeatures & 1) != 0 || (this.mEnabledFeatures & 64) != 0) {
            this.mMagnificationGestureHandler = new MagnificationGestureHandler(this.mContext, this.mAms.getMagnificationController(), (this.mEnabledFeatures & 1) != 0, (this.mEnabledFeatures & 64) != 0);
            addFirstEventHandler(this.mMagnificationGestureHandler);
        }
        if ((this.mEnabledFeatures & 16) != 0) {
            this.mMotionEventInjector = new MotionEventInjector(this.mContext.getMainLooper());
            addFirstEventHandler(this.mMotionEventInjector);
            this.mAms.setMotionEventInjector(this.mMotionEventInjector);
        }
        if ((this.mEnabledFeatures & 4) != 0) {
            this.mKeyboardInterceptor = new KeyboardInterceptor(this.mAms, (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class));
            addFirstEventHandler(this.mKeyboardInterceptor);
        }
    }

    private void addFirstEventHandler(EventStreamTransformation eventStreamTransformation) {
        if (this.mEventHandler != null) {
            eventStreamTransformation.setNext(this.mEventHandler);
        } else {
            eventStreamTransformation.setNext(this);
        }
        this.mEventHandler = eventStreamTransformation;
    }

    private void disableFeatures() {
        processBatchedEvents(JobStatus.NO_LATEST_RUNTIME);
        if (this.mMotionEventInjector != null) {
            this.mAms.setMotionEventInjector(null);
            this.mMotionEventInjector.onDestroy();
            this.mMotionEventInjector = null;
        }
        if (this.mAutoclickController != null) {
            this.mAutoclickController.onDestroy();
            this.mAutoclickController = null;
        }
        if (this.mTouchExplorer != null) {
            this.mTouchExplorer.onDestroy();
            this.mTouchExplorer = null;
        }
        if (this.mMagnificationGestureHandler != null) {
            this.mMagnificationGestureHandler.onDestroy();
            this.mMagnificationGestureHandler = null;
        }
        if (this.mKeyboardInterceptor != null) {
            this.mKeyboardInterceptor.onDestroy();
            this.mKeyboardInterceptor = null;
        }
        this.mEventHandler = null;
        resetStreamState();
    }

    void resetStreamState() {
        if (this.mTouchScreenStreamState != null) {
            this.mTouchScreenStreamState.reset();
        }
        if (this.mMouseStreamState != null) {
            this.mMouseStreamState.reset();
        }
        if (this.mKeyboardStreamState != null) {
            this.mKeyboardStreamState.reset();
        }
    }

    @Override
    public void onDestroy() {
    }

    private static class MotionEventHolder {
        private static final int MAX_POOL_SIZE = 32;
        private static final Pools.SimplePool<MotionEventHolder> sPool = new Pools.SimplePool<>(32);
        public MotionEvent event;
        public MotionEventHolder next;
        public int policyFlags;
        public MotionEventHolder previous;

        private MotionEventHolder() {
        }

        public static MotionEventHolder obtain(MotionEvent motionEvent, int i) {
            MotionEventHolder motionEventHolder = (MotionEventHolder) sPool.acquire();
            if (motionEventHolder == null) {
                motionEventHolder = new MotionEventHolder();
            }
            motionEventHolder.event = MotionEvent.obtain(motionEvent);
            motionEventHolder.policyFlags = i;
            return motionEventHolder;
        }

        public void recycle() {
            this.event.recycle();
            this.event = null;
            this.policyFlags = 0;
            this.next = null;
            this.previous = null;
            sPool.release(this);
        }
    }

    private static class EventStreamState {
        private int mDeviceId = -1;

        EventStreamState() {
        }

        public boolean updateDeviceId(int i) {
            if (this.mDeviceId == i) {
                return false;
            }
            reset();
            this.mDeviceId = i;
            return true;
        }

        public boolean deviceIdValid() {
            return this.mDeviceId >= 0;
        }

        public void reset() {
            this.mDeviceId = -1;
        }

        public boolean shouldProcessScroll() {
            return false;
        }

        public boolean shouldProcessMotionEvent(MotionEvent motionEvent) {
            return false;
        }

        public boolean shouldProcessKeyEvent(KeyEvent keyEvent) {
            return false;
        }
    }

    private static class MouseEventStreamState extends EventStreamState {
        private boolean mMotionSequenceStarted;

        public MouseEventStreamState() {
            reset();
        }

        @Override
        public final void reset() {
            super.reset();
            this.mMotionSequenceStarted = false;
        }

        @Override
        public final boolean shouldProcessScroll() {
            return true;
        }

        @Override
        public final boolean shouldProcessMotionEvent(MotionEvent motionEvent) {
            boolean z = true;
            if (this.mMotionSequenceStarted) {
                return true;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked != 0 && actionMasked != 7) {
                z = false;
            }
            this.mMotionSequenceStarted = z;
            return this.mMotionSequenceStarted;
        }
    }

    private static class TouchScreenEventStreamState extends EventStreamState {
        private boolean mHoverSequenceStarted;
        private boolean mTouchSequenceStarted;

        public TouchScreenEventStreamState() {
            reset();
        }

        @Override
        public final void reset() {
            super.reset();
            this.mTouchSequenceStarted = false;
            this.mHoverSequenceStarted = false;
        }

        @Override
        public final boolean shouldProcessMotionEvent(MotionEvent motionEvent) {
            if (motionEvent.isTouchEvent()) {
                if (this.mTouchSequenceStarted) {
                    return true;
                }
                this.mTouchSequenceStarted = motionEvent.getActionMasked() == 0;
                return this.mTouchSequenceStarted;
            }
            if (this.mHoverSequenceStarted) {
                return true;
            }
            this.mHoverSequenceStarted = motionEvent.getActionMasked() == 9;
            return this.mHoverSequenceStarted;
        }
    }

    private static class KeyboardEventStreamState extends EventStreamState {
        private SparseBooleanArray mEventSequenceStartedMap = new SparseBooleanArray();

        public KeyboardEventStreamState() {
            reset();
        }

        @Override
        public final void reset() {
            super.reset();
            this.mEventSequenceStartedMap.clear();
        }

        @Override
        public boolean updateDeviceId(int i) {
            return false;
        }

        @Override
        public boolean deviceIdValid() {
            return true;
        }

        @Override
        public final boolean shouldProcessKeyEvent(KeyEvent keyEvent) {
            int deviceId = keyEvent.getDeviceId();
            if (this.mEventSequenceStartedMap.get(deviceId, false)) {
                return true;
            }
            boolean z = keyEvent.getAction() == 0;
            this.mEventSequenceStartedMap.put(deviceId, z);
            return z;
        }
    }
}
