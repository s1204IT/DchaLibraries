package android.app;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.input.InputManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import dalvik.system.CloseGuard;
import java.util.List;

public class ActivityView extends ViewGroup {
    private static final String DISPLAY_NAME = "ActivityViewVirtualDisplay";
    private static final String TAG = "ActivityView";
    private IActivityManager mActivityManager;
    private StateCallback mActivityViewCallback;
    private final CloseGuard mGuard;
    private IInputForwarder mInputForwarder;
    private final int[] mLocationOnScreen;
    private boolean mOpened;
    private Surface mSurface;
    private final SurfaceCallback mSurfaceCallback;
    private final SurfaceView mSurfaceView;
    private TaskStackListener mTaskStackListener;
    private VirtualDisplay mVirtualDisplay;

    public ActivityView(Context context) {
        this(context, null);
    }

    public ActivityView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ActivityView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLocationOnScreen = new int[2];
        this.mGuard = CloseGuard.get();
        this.mActivityManager = ActivityManager.getService();
        this.mSurfaceView = new SurfaceView(context);
        this.mSurfaceCallback = new SurfaceCallback();
        this.mSurfaceView.getHolder().addCallback(this.mSurfaceCallback);
        addView(this.mSurfaceView);
        this.mOpened = true;
        this.mGuard.open("release");
    }

    public static abstract class StateCallback {
        public abstract void onActivityViewDestroyed(ActivityView activityView);

        public abstract void onActivityViewReady(ActivityView activityView);

        public void onTaskMovedToFront(ActivityManager.StackInfo stackInfo) {
        }
    }

    public void setCallback(StateCallback stateCallback) {
        this.mActivityViewCallback = stateCallback;
        if (this.mVirtualDisplay != null && this.mActivityViewCallback != null) {
            this.mActivityViewCallback.onActivityViewReady(this);
        }
    }

    public void startActivity(Intent intent) {
        getContext().startActivity(intent, prepareActivityOptions().toBundle());
    }

    public void startActivity(Intent intent, UserHandle userHandle) {
        getContext().startActivityAsUser(intent, prepareActivityOptions().toBundle(), userHandle);
    }

    public void startActivity(PendingIntent pendingIntent) {
        try {
            pendingIntent.send(null, 0, null, null, null, null, prepareActivityOptions().toBundle());
        } catch (PendingIntent.CanceledException e) {
            throw new RuntimeException(e);
        }
    }

    private ActivityOptions prepareActivityOptions() {
        if (this.mVirtualDisplay == null) {
            throw new IllegalStateException("Trying to start activity before ActivityView is ready.");
        }
        ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
        activityOptionsMakeBasic.setLaunchDisplayId(this.mVirtualDisplay.getDisplay().getDisplayId());
        return activityOptionsMakeBasic;
    }

    public void release() {
        if (this.mVirtualDisplay == null) {
            throw new IllegalStateException("Trying to release container that is not initialized.");
        }
        performRelease();
    }

    public void onLocationChanged() {
        updateLocation();
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mSurfaceView.layout(0, 0, i3 - i, i4 - i2);
    }

    private void updateLocation() {
        try {
            getLocationOnScreen(this.mLocationOnScreen);
            WindowManagerGlobal.getWindowSession().updateTapExcludeRegion(getWindow(), hashCode(), this.mLocationOnScreen[0], this.mLocationOnScreen[1], getWidth(), getHeight());
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return injectInputEvent(motionEvent) || super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (motionEvent.isFromSource(2) && injectInputEvent(motionEvent)) {
            return true;
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    private boolean injectInputEvent(InputEvent inputEvent) {
        if (this.mInputForwarder != null) {
            try {
                return this.mInputForwarder.forwardEvent(inputEvent);
            } catch (RemoteException e) {
                e.rethrowAsRuntimeException();
                return false;
            }
        }
        return false;
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        private SurfaceCallback() {
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            ActivityView.this.mSurface = ActivityView.this.mSurfaceView.getHolder().getSurface();
            if (ActivityView.this.mVirtualDisplay == null) {
                ActivityView.this.initVirtualDisplay();
                if (ActivityView.this.mVirtualDisplay != null && ActivityView.this.mActivityViewCallback != null) {
                    ActivityView.this.mActivityViewCallback.onActivityViewReady(ActivityView.this);
                }
            } else {
                ActivityView.this.mVirtualDisplay.setSurface(surfaceHolder.getSurface());
            }
            ActivityView.this.updateLocation();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            if (ActivityView.this.mVirtualDisplay != null) {
                ActivityView.this.mVirtualDisplay.resize(i2, i3, ActivityView.this.getBaseDisplayDensity());
            }
            ActivityView.this.updateLocation();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            ActivityView.this.mSurface.release();
            ActivityView.this.mSurface = null;
            if (ActivityView.this.mVirtualDisplay != null) {
                ActivityView.this.mVirtualDisplay.setSurface(null);
            }
            ActivityView.this.cleanTapExcludeRegion();
        }
    }

    private void initVirtualDisplay() {
        if (this.mVirtualDisplay != null) {
            throw new IllegalStateException("Trying to initialize for the second time.");
        }
        int width = this.mSurfaceView.getWidth();
        int height = this.mSurfaceView.getHeight();
        this.mVirtualDisplay = ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).createVirtualDisplay("ActivityViewVirtualDisplay@" + System.identityHashCode(this), width, height, getBaseDisplayDensity(), this.mSurface, 9);
        if (this.mVirtualDisplay == null) {
            Log.e(TAG, "Failed to initialize ActivityView");
            return;
        }
        int displayId = this.mVirtualDisplay.getDisplay().getDisplayId();
        try {
            WindowManagerGlobal.getWindowManagerService().dontOverrideDisplayInfo(displayId);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
        this.mInputForwarder = InputManager.getInstance().createInputForwarder(displayId);
        this.mTaskStackListener = new TaskStackListenerImpl();
        try {
            this.mActivityManager.registerTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to register task stack listener", e2);
        }
    }

    private void performRelease() {
        boolean z;
        if (!this.mOpened) {
            return;
        }
        this.mSurfaceView.getHolder().removeCallback(this.mSurfaceCallback);
        if (this.mInputForwarder != null) {
            this.mInputForwarder = null;
        }
        cleanTapExcludeRegion();
        if (this.mTaskStackListener != null) {
            try {
                this.mActivityManager.unregisterTaskStackListener(this.mTaskStackListener);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to unregister task stack listener", e);
            }
            this.mTaskStackListener = null;
        }
        if (this.mVirtualDisplay != null) {
            this.mVirtualDisplay.release();
            this.mVirtualDisplay = null;
            z = true;
        } else {
            z = false;
        }
        if (this.mSurface != null) {
            this.mSurface.release();
            this.mSurface = null;
        }
        if (z && this.mActivityViewCallback != null) {
            this.mActivityViewCallback.onActivityViewDestroyed(this);
        }
        this.mGuard.close();
        this.mOpened = false;
    }

    private void cleanTapExcludeRegion() {
        try {
            WindowManagerGlobal.getWindowSession().updateTapExcludeRegion(getWindow(), hashCode(), 0, 0, 0, 0);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    private int getBaseDisplayDensity() {
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.densityDpi;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGuard != null) {
                this.mGuard.warnIfOpen();
                performRelease();
            }
        } finally {
            super.finalize();
        }
    }

    private class TaskStackListenerImpl extends TaskStackListener {
        private TaskStackListenerImpl() {
        }

        @Override
        public void onTaskDescriptionChanged(int i, ActivityManager.TaskDescription taskDescription) throws RemoteException {
            ActivityManager.StackInfo topMostStackInfo;
            if (ActivityView.this.mVirtualDisplay != null && (topMostStackInfo = getTopMostStackInfo()) != null && i == topMostStackInfo.taskIds[topMostStackInfo.taskIds.length - 1]) {
                ActivityView.this.mSurfaceView.setResizeBackgroundColor(taskDescription.getBackgroundColor());
            }
        }

        @Override
        public void onTaskMovedToFront(int i) throws RemoteException {
            ActivityManager.StackInfo topMostStackInfo;
            if (ActivityView.this.mActivityViewCallback != null && (topMostStackInfo = getTopMostStackInfo()) != null && i == topMostStackInfo.taskIds[topMostStackInfo.taskIds.length - 1]) {
                ActivityView.this.mActivityViewCallback.onTaskMovedToFront(topMostStackInfo);
            }
        }

        private ActivityManager.StackInfo getTopMostStackInfo() throws RemoteException {
            int displayId = ActivityView.this.mVirtualDisplay.getDisplay().getDisplayId();
            List<ActivityManager.StackInfo> allStackInfos = ActivityView.this.mActivityManager.getAllStackInfos();
            int size = allStackInfos.size();
            for (int i = 0; i < size; i++) {
                ActivityManager.StackInfo stackInfo = allStackInfos.get(i);
                if (stackInfo.displayId == displayId) {
                    return stackInfo;
                }
            }
            return null;
        }
    }
}
