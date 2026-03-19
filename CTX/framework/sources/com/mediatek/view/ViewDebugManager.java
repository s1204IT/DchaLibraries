package com.mediatek.view;

import android.os.Build;
import android.os.SystemProperties;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;

public class ViewDebugManager {
    public static final String INPUT_DISPATCH_STATE_DELIVER_EVENT = "4: Deliver input event";
    public static final String INPUT_DISPATCH_STATE_EARLY_POST_IME_STAGE = "8: Early post IME stage";
    public static final String INPUT_DISPATCH_STATE_ENQUEUE_EVENT = "2: Enqueue input event";
    public static final String INPUT_DISPATCH_STATE_FINISHED = "0: Finish handle input event";
    public static final String INPUT_DISPATCH_STATE_IME_STAGE = "7: IME stage";
    public static final String INPUT_DISPATCH_STATE_NATIVE_POST_IME_STAGE = "9: Native post IME stage";
    public static final String INPUT_DISPATCH_STATE_NATIVE_PRE_IME_STAGE = "5: Native pre IME stage";
    public static final String INPUT_DISPATCH_STATE_PROCESS_EVENT = "3 1: Process input event";
    public static final String INPUT_DISPATCH_STATE_SCHEDULE_EVENT = "3 2: Schedule process input event";
    public static final String INPUT_DISPATCH_STATE_STARTED = "1: Start event from input";
    public static final String INPUT_DISPATCH_STATE_SYNTHETC_INPUT_STAGE = "11: Synthetic input stage";
    public static final String INPUT_DISPATCH_STATE_VIEW_POST_IME_STAGE = "10: View Post IME stage";
    public static final String INPUT_DISPATCH_STATE_VIEW_PRE_IME_STAGE = "6: View pre IME stage";
    private static ViewDebugManager sInstance;
    private static Object lock = new Object();
    public static boolean DEBUG_CHOREOGRAPHER_JANK = SystemProperties.getBoolean("debug.choreographer.janklog", false);
    public static boolean DEBUG_CHOREOGRAPHER_FRAMES = SystemProperties.getBoolean("debug.choreographer.frameslog", false);
    public static final boolean DEBUG_ENG = "eng".equals(Build.TYPE);
    public static boolean DEBUG_USER = false;
    public static boolean DBG = false;
    public static boolean LOCAL_LOGV = false;
    public static boolean DEBUG_DRAW = false;
    public static boolean DEBUG_LAYOUT = false;
    public static boolean DEBUG_DIALOG = false;
    public static boolean DEBUG_INPUT_RESIZE = false;
    public static boolean DEBUG_ORIENTATION = false;
    public static boolean DEBUG_TRACKBALL = false;
    public static boolean DEBUG_IMF = false;
    public static boolean DEBUG_CONFIGURATION = false;
    public static boolean DEBUG_FPS = false;
    public static boolean DEBUG_INPUT_STAGES = false;
    public static boolean DEBUG_KEEP_SCREEN_ON = false;
    public static boolean DEBUG_HWUI = false;
    public static boolean DEBUG_INPUT = false;
    public static boolean DEBUG_KEY = false;
    public static boolean DEBUG_MOTION = false;
    public static boolean DEBUG_IME_ANR = false;
    public static boolean DEBUG_LIFECYCLE = false;
    public static boolean DEBUG_REQUESTLAYOUT = false;
    public static boolean DEBUG_INVALIDATE = false;
    public static boolean DEBUG_SCHEDULETRAVERSALS = false;
    public static boolean DEBUG_TOUCHMODE = false;
    public static boolean DEBUG_TOUCH = false;
    public static boolean DEBUG_FOCUS = false;
    public static boolean DEBUG_SYSTRACE_MEASURE = false;
    public static boolean DEBUG_SYSTRACE_LAYOUT = false;
    public static boolean DEBUG_SYSTRACE_DRAW = false;
    public static boolean DEBUG_MET_TRACE = false;
    public static boolean DBG_TRANSP = false;
    protected static int DBG_TIMEOUT_VALUE = 400;

    public static ViewDebugManager getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    try {
                        sInstance = (ViewDebugManager) Class.forName("com.mediatek.view.impl.ViewDebugManagerImpl").getConstructor(new Class[0]).newInstance(new Object[0]);
                    } catch (Exception e) {
                        sInstance = new ViewDebugManager();
                    }
                }
            }
        }
        return sInstance;
    }

    public void debugKeyDispatch(View view, KeyEvent keyEvent) {
    }

    public void debugEventHandled(View view, InputEvent inputEvent, String str) {
    }

    public void debugTouchDispatched(View view, MotionEvent motionEvent) {
    }

    public void warningParentToNull(View view) {
    }

    public void debugOnDrawDone(View view, long j) {
    }

    public long debugOnMeasureStart(View view, int i, int i2, int i3, int i4) {
        return -1L;
    }

    public void debugOnMeasureEnd(View view, long j) {
    }

    public void debugOnLayoutEnd(View view, long j) {
    }

    public void debugViewRemoved(View view, ViewGroup viewGroup, Thread thread) {
    }

    public void debugViewGroupChildMeasure(View view, View view2, ViewGroup.MarginLayoutParams marginLayoutParams, int i, int i2) {
    }

    public void debugViewGroupChildMeasure(View view, View view2, ViewGroup.LayoutParams layoutParams, int i, int i2) {
    }

    public void debugViewRootConstruct(String str, Object obj, Object obj2, Object obj3, Object obj4, ViewRootImpl viewRootImpl) {
    }

    public void dumpInputDispatchingStatus(String str) {
    }

    public void debugInputStageDeliverd(Object obj, long j) {
    }

    public void debugInputEventStart(InputEvent inputEvent) {
    }

    public void debugInputEventFinished(String str, boolean z, InputEvent inputEvent, ViewRootImpl viewRootImpl) {
    }

    public void debugTraveralDone(Object obj, Object obj2, boolean z, ViewRootImpl viewRootImpl, boolean z2, boolean z3, String str) {
    }

    public void debugInputDispatchState(InputEvent inputEvent, String str) {
    }

    public boolean debugForceHWDraw(boolean z) {
        return z;
    }

    public int debugForceHWLayer(int i) {
        return i;
    }
}
