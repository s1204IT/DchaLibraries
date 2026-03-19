package com.android.quickstep;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.util.TraceHelper;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.DeferredTouchConsumer;
import com.android.quickstep.TouchInteractionService;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;

@TargetApi(26)
public class TouchInteractionService extends Service {
    public static final int EDGE_NAV_BAR = 256;
    private static final String TAG = "TouchInteractionService";
    private static boolean sConnected;
    private static final SparseArray<String> sMotionEventNames = new SparseArray<>(3);
    private static HandlerThread sRemoteUiThread;
    private ActivityManagerWrapper mAM;
    private Choreographer mBackgroundThreadChoreographer;
    private MotionEventQueue mEventQueue;
    private ISystemUiProxy mISystemUiProxy;
    private Choreographer mMainThreadChoreographer;
    private MainThreadExecutor mMainThreadExecutor;
    private final IBinder mMyBinder = new IOverviewProxy.Stub() {
        @Override
        public void onPreMotionEvent(int i) throws RemoteException {
            TraceHelper.beginSection("SysUiBinder");
            TouchInteractionService.this.setupTouchConsumer(i);
            TraceHelper.partitionSection("SysUiBinder", "Down target " + i);
        }

        @Override
        public void onMotionEvent(MotionEvent motionEvent) {
            TouchInteractionService.this.mEventQueue.queue(motionEvent);
            String str = (String) TouchInteractionService.sMotionEventNames.get(motionEvent.getActionMasked());
            if (str != null) {
                TraceHelper.partitionSection("SysUiBinder", str);
            }
        }

        @Override
        public void onBind(ISystemUiProxy iSystemUiProxy) {
            TouchInteractionService.this.mISystemUiProxy = iSystemUiProxy;
            TouchInteractionService.this.mRecentsModel.setSystemUiProxy(TouchInteractionService.this.mISystemUiProxy);
            TouchInteractionService.this.mOverviewInteractionState.setSystemUiProxy(TouchInteractionService.this.mISystemUiProxy);
        }

        @Override
        public void onQuickScrubStart() {
            TouchInteractionService.this.mEventQueue.onQuickScrubStart();
            TraceHelper.partitionSection("SysUiBinder", "onQuickScrubStart");
        }

        @Override
        public void onQuickScrubProgress(float f) {
            TouchInteractionService.this.mEventQueue.onQuickScrubProgress(f);
        }

        @Override
        public void onQuickScrubEnd() {
            TouchInteractionService.this.mEventQueue.onQuickScrubEnd();
            TraceHelper.endSection("SysUiBinder", "onQuickScrubEnd");
        }

        @Override
        public void onOverviewToggle() {
            TouchInteractionService.this.mOverviewCommandHelper.onOverviewToggle();
        }

        @Override
        public void onOverviewShown(boolean z) {
            if (z) {
                TouchInteractionService.this.setupTouchConsumer(0);
                TouchInteractionService.this.mEventQueue.onOverviewShownFromAltTab();
            } else {
                TouchInteractionService.this.mOverviewCommandHelper.onOverviewShown();
            }
        }

        @Override
        public void onOverviewHidden(boolean z, boolean z2) {
            if (z && !z2) {
                TouchInteractionService.this.mEventQueue.onQuickScrubEnd();
            }
        }

        @Override
        public void onQuickStep(MotionEvent motionEvent) {
            TouchInteractionService.this.mEventQueue.onQuickStep(motionEvent);
            TraceHelper.endSection("SysUiBinder", "onQuickStep");
        }

        @Override
        public void onTip(int i, int i2) {
            TouchInteractionService.this.mOverviewCommandHelper.onTip(i, i2);
        }
    };
    private final TouchConsumer mNoOpTouchConsumer = new TouchConsumer() {
        @Override
        public final void accept(MotionEvent motionEvent) {
            TouchInteractionService.lambda$new$0(motionEvent);
        }
    };
    private OverviewCallbacks mOverviewCallbacks;
    private OverviewCommandHelper mOverviewCommandHelper;
    private OverviewInteractionState mOverviewInteractionState;
    private RecentsModel mRecentsModel;

    static {
        sMotionEventNames.put(0, "ACTION_DOWN");
        sMotionEventNames.put(1, "ACTION_UP");
        sMotionEventNames.put(3, "ACTION_CANCEL");
        sConnected = false;
    }

    static void lambda$new$0(MotionEvent motionEvent) {
    }

    public static boolean isConnected() {
        return sConnected;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mAM = ActivityManagerWrapper.getInstance();
        this.mRecentsModel = RecentsModel.getInstance(this);
        this.mRecentsModel.setPreloadTasksInBackground(true);
        this.mMainThreadExecutor = new MainThreadExecutor();
        this.mOverviewCommandHelper = new OverviewCommandHelper(this);
        this.mMainThreadChoreographer = Choreographer.getInstance();
        this.mEventQueue = new MotionEventQueue(this.mMainThreadChoreographer, this.mNoOpTouchConsumer);
        this.mOverviewInteractionState = OverviewInteractionState.getInstance(this);
        this.mOverviewCallbacks = OverviewCallbacks.get(this);
        sConnected = true;
        initBackgroundChoreographer();
    }

    @Override
    public void onDestroy() {
        this.mOverviewCommandHelper.onDestroy();
        sConnected = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Touch service connected");
        return this.mMyBinder;
    }

    private void setupTouchConsumer(final int i) {
        this.mEventQueue.reset();
        final TouchConsumer consumer = this.mEventQueue.getConsumer();
        if (consumer.deferNextEventToMainThread()) {
            this.mEventQueue = new MotionEventQueue(this.mMainThreadChoreographer, new DeferredTouchConsumer(new DeferredTouchConsumer.DeferredTouchProvider() {
                @Override
                public final TouchConsumer createTouchConsumer(VelocityTracker velocityTracker) {
                    return this.f$0.getCurrentTouchConsumer(i, consumer.forceToLauncherConsumer(), velocityTracker);
                }
            }));
            this.mEventQueue.deferInit();
        } else {
            this.mEventQueue = new MotionEventQueue(this.mMainThreadChoreographer, getCurrentTouchConsumer(i, false, null));
        }
    }

    private TouchConsumer getCurrentTouchConsumer(int i, boolean z, VelocityTracker velocityTracker) {
        ActivityManager.RunningTaskInfo runningTask = this.mAM.getRunningTask(0);
        if (runningTask == null && !z) {
            return this.mNoOpTouchConsumer;
        }
        if (z || runningTask.topActivity.equals(this.mOverviewCommandHelper.overviewComponent)) {
            return getOverviewConsumer();
        }
        return new OtherActivityTouchConsumer(this, runningTask, this.mRecentsModel, this.mOverviewCommandHelper.overviewIntent, this.mOverviewCommandHelper.getActivityControlHelper(), this.mMainThreadExecutor, this.mBackgroundThreadChoreographer, i, this.mOverviewCallbacks, velocityTracker == null ? VelocityTracker.obtain() : velocityTracker);
    }

    private TouchConsumer getOverviewConsumer() {
        ActivityControlHelper activityControlHelper = this.mOverviewCommandHelper.getActivityControlHelper();
        BaseDraggingActivity createdActivity = activityControlHelper.getCreatedActivity();
        if (createdActivity == null) {
            return this.mNoOpTouchConsumer;
        }
        return new OverviewTouchConsumer(activityControlHelper, createdActivity);
    }

    private static class OverviewTouchConsumer<T extends BaseDraggingActivity> implements TouchConsumer {
        private final T mActivity;
        private final ActivityControlHelper<T> mActivityHelper;
        private final QuickScrubController mQuickScrubController;
        private final BaseDragLayer mTarget;
        private final int mTouchSlop;
        private final int[] mLocationOnScreen = new int[2];
        private final PointF mDownPos = new PointF();
        private boolean mTrackingStarted = false;
        private boolean mInvalidated = false;
        private float mLastProgress = 0.0f;
        private boolean mStartPending = false;
        private boolean mEndPending = false;

        OverviewTouchConsumer(ActivityControlHelper<T> activityControlHelper, T t) {
            this.mActivityHelper = activityControlHelper;
            this.mActivity = t;
            this.mTarget = t.getDragLayer();
            this.mTouchSlop = ViewConfiguration.get(this.mTarget.getContext()).getScaledTouchSlop();
            this.mQuickScrubController = ((RecentsView) this.mActivity.getOverviewPanel()).getQuickScrubController();
        }

        @Override
        public void accept(MotionEvent motionEvent) {
            if (this.mInvalidated) {
                return;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                this.mTrackingStarted = false;
                this.mDownPos.set(motionEvent.getX(), motionEvent.getY());
            } else if (!this.mTrackingStarted) {
                if (actionMasked != 2) {
                    switch (actionMasked) {
                        case 5:
                        case 6:
                            if (!this.mTrackingStarted) {
                                this.mInvalidated = true;
                            }
                            break;
                    }
                } else if (Math.abs(motionEvent.getY() - this.mDownPos.y) >= this.mTouchSlop) {
                    this.mTarget.getLocationOnScreen(this.mLocationOnScreen);
                    MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
                    motionEventObtain.setAction(0);
                    sendEvent(motionEventObtain);
                    motionEventObtain.recycle();
                    this.mTrackingStarted = true;
                }
            }
            if (this.mTrackingStarted) {
                sendEvent(motionEvent);
            }
            if (actionMasked == 1 || actionMasked == 3) {
                this.mInvalidated = true;
            }
        }

        private void sendEvent(MotionEvent motionEvent) {
            int edgeFlags = motionEvent.getEdgeFlags();
            motionEvent.setEdgeFlags(edgeFlags | 256);
            motionEvent.offsetLocation(-this.mLocationOnScreen[0], -this.mLocationOnScreen[1]);
            if (!this.mTrackingStarted) {
                this.mTarget.onInterceptTouchEvent(motionEvent);
            }
            this.mTarget.onTouchEvent(motionEvent);
            motionEvent.offsetLocation(this.mLocationOnScreen[0], this.mLocationOnScreen[1]);
            motionEvent.setEdgeFlags(edgeFlags);
        }

        @Override
        public void onQuickStep(MotionEvent motionEvent) {
            if (this.mInvalidated) {
                return;
            }
            OverviewCallbacks.get(this.mActivity).closeAllWindows();
            ActivityManagerWrapper.getInstance().closeSystemWindows(ActivityManagerWrapper.CLOSE_SYSTEM_WINDOWS_REASON_RECENTS);
        }

        @Override
        public void updateTouchTracking(int i) {
            if (!this.mInvalidated && i == 1) {
                if (!this.mQuickScrubController.prepareQuickScrub(TouchInteractionService.TAG)) {
                    this.mInvalidated = true;
                    return;
                }
                OverviewCallbacks.get(this.mActivity).closeAllWindows();
                ActivityManagerWrapper.getInstance().closeSystemWindows(ActivityManagerWrapper.CLOSE_SYSTEM_WINDOWS_REASON_RECENTS);
                this.mStartPending = true;
                this.mActivityHelper.executeOnWindowAvailable(this.mActivity, new Runnable() {
                    @Override
                    public final void run() {
                        TouchInteractionService.OverviewTouchConsumer.lambda$updateTouchTracking$0(this.f$0);
                    }
                });
            }
        }

        public static void lambda$updateTouchTracking$0(OverviewTouchConsumer overviewTouchConsumer) {
            if (!overviewTouchConsumer.mQuickScrubController.prepareQuickScrub(TouchInteractionService.TAG)) {
                overviewTouchConsumer.mInvalidated = true;
                return;
            }
            overviewTouchConsumer.mActivityHelper.onQuickInteractionStart(overviewTouchConsumer.mActivity, null, true);
            overviewTouchConsumer.mQuickScrubController.onQuickScrubProgress(overviewTouchConsumer.mLastProgress);
            overviewTouchConsumer.mStartPending = false;
            if (overviewTouchConsumer.mEndPending) {
                overviewTouchConsumer.mQuickScrubController.onQuickScrubEnd();
                overviewTouchConsumer.mEndPending = false;
            }
        }

        @Override
        public void onQuickScrubEnd() {
            if (this.mInvalidated) {
                return;
            }
            if (this.mStartPending) {
                this.mEndPending = true;
            } else {
                this.mQuickScrubController.onQuickScrubEnd();
            }
        }

        @Override
        public void onQuickScrubProgress(float f) {
            this.mLastProgress = f;
            if (this.mInvalidated || this.mStartPending) {
                return;
            }
            this.mQuickScrubController.onQuickScrubProgress(f);
        }
    }

    private void initBackgroundChoreographer() {
        if (sRemoteUiThread == null) {
            sRemoteUiThread = new HandlerThread("remote-ui");
            sRemoteUiThread.start();
        }
        new Handler(sRemoteUiThread.getLooper()).post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mBackgroundThreadChoreographer = Choreographer.getInstance();
            }
        });
    }
}
