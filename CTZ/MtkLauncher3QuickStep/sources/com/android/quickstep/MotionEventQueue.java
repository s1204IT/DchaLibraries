package com.android.quickstep;

import android.annotation.TargetApi;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import com.android.systemui.shared.system.ChoreographerCompat;
import java.util.ArrayList;

@TargetApi(26)
public class MotionEventQueue {
    private static final int ACTION_COMMAND = 2302;
    private static final int ACTION_DEFER_INIT = 1534;
    private static final int ACTION_QUICK_SCRUB_END = 1022;
    private static final int ACTION_QUICK_SCRUB_PROGRESS = 766;
    private static final int ACTION_QUICK_SCRUB_START = 510;
    private static final int ACTION_QUICK_STEP = 2046;
    private static final int ACTION_RESET = 1278;
    private static final int ACTION_SHOW_OVERVIEW_FROM_ALT_TAB = 1790;
    private static final int ACTION_VIRTUAL = 254;
    private static final String TAG = "MotionEventQueue";
    private final TouchConsumer mConsumer;
    private Choreographer mCurrentChoreographer;
    private Choreographer mInterimChoreographer;
    private final Choreographer mMainChoreographer;
    private final EventArray mEmptyArray = new EventArray();
    private final Object mExecutionLock = new Object();
    private final EventArray[] mArrays = {new EventArray(), new EventArray()};
    private int mCurrentIndex = 0;
    private final Runnable mMainFrameCallback = new Runnable() {
        @Override
        public final void run() {
            this.f$0.frameCallbackForMainChoreographer();
        }
    };
    private final Runnable mInterimFrameCallback = new Runnable() {
        @Override
        public final void run() {
            this.f$0.frameCallbackForInterimChoreographer();
        }
    };
    private Runnable mCurrentRunnable = this.mMainFrameCallback;

    public MotionEventQueue(Choreographer choreographer, TouchConsumer touchConsumer) {
        this.mMainChoreographer = choreographer;
        this.mConsumer = touchConsumer;
        this.mCurrentChoreographer = this.mMainChoreographer;
        setInterimChoreographer(touchConsumer.getIntrimChoreographer(this));
    }

    public void setInterimChoreographer(Choreographer choreographer) {
        synchronized (this.mExecutionLock) {
            synchronized (this.mArrays) {
                setInterimChoreographerLocked(choreographer);
                ChoreographerCompat.postInputFrame(this.mCurrentChoreographer, this.mCurrentRunnable);
            }
        }
    }

    private void setInterimChoreographerLocked(Choreographer choreographer) {
        this.mInterimChoreographer = choreographer;
        if (choreographer == null) {
            this.mCurrentChoreographer = this.mMainChoreographer;
            this.mCurrentRunnable = this.mMainFrameCallback;
        } else {
            this.mCurrentChoreographer = this.mInterimChoreographer;
            this.mCurrentRunnable = this.mInterimFrameCallback;
        }
    }

    public void queue(MotionEvent motionEvent) {
        this.mConsumer.preProcessMotionEvent(motionEvent);
        queueNoPreProcess(motionEvent);
    }

    private void queueNoPreProcess(MotionEvent motionEvent) {
        synchronized (this.mArrays) {
            EventArray eventArray = this.mArrays[this.mCurrentIndex];
            if (eventArray.isEmpty()) {
                ChoreographerCompat.postInputFrame(this.mCurrentChoreographer, this.mCurrentRunnable);
            }
            int action = motionEvent.getAction();
            if (action == 2 && eventArray.lastEventAction == 2) {
                eventArray.set(eventArray.size() - 1, motionEvent).recycle();
            } else {
                eventArray.add(motionEvent);
                eventArray.lastEventAction = action;
            }
        }
    }

    public void frameCallbackForMainChoreographer() {
        runFor(this.mMainChoreographer);
    }

    public void frameCallbackForInterimChoreographer() {
        runFor(this.mInterimChoreographer);
    }

    private void runFor(Choreographer choreographer) {
        synchronized (this.mExecutionLock) {
            EventArray eventArraySwapAndGetCurrentArray = swapAndGetCurrentArray(choreographer);
            int size = eventArraySwapAndGetCurrentArray.size();
            for (int i = 0; i < size; i++) {
                MotionEvent motionEvent = eventArraySwapAndGetCurrentArray.get(i);
                if (motionEvent.getActionMasked() == ACTION_VIRTUAL) {
                    int action = motionEvent.getAction();
                    if (action == ACTION_QUICK_SCRUB_START) {
                        this.mConsumer.updateTouchTracking(1);
                    } else if (action == ACTION_QUICK_SCRUB_PROGRESS) {
                        this.mConsumer.onQuickScrubProgress(motionEvent.getX());
                    } else if (action == ACTION_QUICK_SCRUB_END) {
                        this.mConsumer.onQuickScrubEnd();
                    } else if (action == ACTION_RESET) {
                        this.mConsumer.reset();
                    } else if (action == ACTION_DEFER_INIT) {
                        this.mConsumer.deferInit();
                    } else if (action == ACTION_SHOW_OVERVIEW_FROM_ALT_TAB) {
                        this.mConsumer.onShowOverviewFromAltTab();
                        this.mConsumer.updateTouchTracking(1);
                    } else if (action == ACTION_QUICK_STEP) {
                        this.mConsumer.onQuickStep(motionEvent);
                    } else if (action == ACTION_COMMAND) {
                        this.mConsumer.onCommand(motionEvent.getSource());
                    } else {
                        Log.e(TAG, "Invalid virtual event: " + motionEvent.getAction());
                    }
                } else {
                    this.mConsumer.accept(motionEvent);
                }
                motionEvent.recycle();
            }
            eventArraySwapAndGetCurrentArray.clear();
            eventArraySwapAndGetCurrentArray.lastEventAction = 3;
        }
    }

    private EventArray swapAndGetCurrentArray(Choreographer choreographer) {
        synchronized (this.mArrays) {
            if (choreographer != this.mCurrentChoreographer) {
                return this.mEmptyArray;
            }
            EventArray eventArray = this.mArrays[this.mCurrentIndex];
            this.mCurrentIndex ^= 1;
            return eventArray;
        }
    }

    private void queueVirtualAction(int i, float f) {
        queueNoPreProcess(MotionEvent.obtain(0L, 0L, i, f, 0.0f, 0));
    }

    public void onQuickScrubStart() {
        queueVirtualAction(ACTION_QUICK_SCRUB_START, 0.0f);
    }

    public void onOverviewShownFromAltTab() {
        queueVirtualAction(ACTION_SHOW_OVERVIEW_FROM_ALT_TAB, 0.0f);
    }

    public void onQuickScrubProgress(float f) {
        queueVirtualAction(ACTION_QUICK_SCRUB_PROGRESS, f);
    }

    public void onQuickScrubEnd() {
        queueVirtualAction(ACTION_QUICK_SCRUB_END, 0.0f);
    }

    public void onQuickStep(MotionEvent motionEvent) {
        motionEvent.setAction(ACTION_QUICK_STEP);
        queueNoPreProcess(motionEvent);
    }

    public void reset() {
        queueVirtualAction(ACTION_RESET, 0.0f);
    }

    public void deferInit() {
        queueVirtualAction(ACTION_DEFER_INIT, 0.0f);
    }

    public void onCommand(int i) {
        MotionEvent motionEventObtain = MotionEvent.obtain(0L, 0L, ACTION_COMMAND, 0.0f, 0.0f, 0);
        motionEventObtain.setSource(i);
        queueNoPreProcess(motionEventObtain);
    }

    public TouchConsumer getConsumer() {
        return this.mConsumer;
    }

    private static class EventArray extends ArrayList<MotionEvent> {
        public int lastEventAction;

        public EventArray() {
            super(4);
            this.lastEventAction = 3;
        }
    }
}
