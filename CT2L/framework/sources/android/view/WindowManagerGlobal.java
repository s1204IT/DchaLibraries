package android.view;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowSessionCallback;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class WindowManagerGlobal {
    public static final int ADD_APP_EXITING = -4;
    public static final int ADD_BAD_APP_TOKEN = -1;
    public static final int ADD_BAD_SUBWINDOW_TOKEN = -2;
    public static final int ADD_DUPLICATE_ADD = -5;
    public static final int ADD_FLAG_APP_VISIBLE = 2;
    public static final int ADD_FLAG_IN_TOUCH_MODE = 1;
    public static final int ADD_INVALID_DISPLAY = -9;
    public static final int ADD_INVALID_TYPE = -10;
    public static final int ADD_MULTIPLE_SINGLETON = -7;
    public static final int ADD_NOT_APP_TOKEN = -3;
    public static final int ADD_OKAY = 0;
    public static final int ADD_PERMISSION_DENIED = -8;
    public static final int ADD_STARTING_NOT_NEEDED = -6;
    public static final int RELAYOUT_DEFER_SURFACE_DESTROY = 2;
    public static final int RELAYOUT_INSETS_PENDING = 1;
    public static final int RELAYOUT_RES_ANIMATING = 8;
    public static final int RELAYOUT_RES_FIRST_TIME = 2;
    public static final int RELAYOUT_RES_IN_TOUCH_MODE = 1;
    public static final int RELAYOUT_RES_SURFACE_CHANGED = 4;
    private static final String TAG = "WindowManager";
    private static WindowManagerGlobal sDefaultWindowManager;
    private static IWindowManager sWindowManagerService;
    private static IWindowSession sWindowSession;
    private Runnable mSystemPropertyUpdater;
    private final Object mLock = new Object();
    private final ArrayList<View> mViews = new ArrayList<>();
    private final ArrayList<ViewRootImpl> mRoots = new ArrayList<>();
    private final ArrayList<WindowManager.LayoutParams> mParams = new ArrayList<>();
    private final ArraySet<View> mDyingViews = new ArraySet<>();

    private WindowManagerGlobal() {
    }

    public static void initialize() {
        getWindowManagerService();
    }

    public static WindowManagerGlobal getInstance() {
        WindowManagerGlobal windowManagerGlobal;
        synchronized (WindowManagerGlobal.class) {
            if (sDefaultWindowManager == null) {
                sDefaultWindowManager = new WindowManagerGlobal();
            }
            windowManagerGlobal = sDefaultWindowManager;
        }
        return windowManagerGlobal;
    }

    public static IWindowManager getWindowManagerService() {
        IWindowManager iWindowManager;
        synchronized (WindowManagerGlobal.class) {
            if (sWindowManagerService == null) {
                sWindowManagerService = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
                try {
                    sWindowManagerService = getWindowManagerService();
                    ValueAnimator.setDurationScale(sWindowManagerService.getCurrentAnimatorScale());
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to get WindowManagerService, cannot set animator scale", e);
                }
                iWindowManager = sWindowManagerService;
            } else {
                iWindowManager = sWindowManagerService;
            }
        }
        return iWindowManager;
    }

    public static IWindowSession getWindowSession() {
        IWindowSession iWindowSession;
        synchronized (WindowManagerGlobal.class) {
            if (sWindowSession == null) {
                try {
                    InputMethodManager imm = InputMethodManager.getInstance();
                    IWindowManager windowManager = getWindowManagerService();
                    sWindowSession = windowManager.openSession(new IWindowSessionCallback.Stub() {
                        @Override
                        public void onAnimatorScaleChanged(float scale) {
                            ValueAnimator.setDurationScale(scale);
                        }
                    }, imm.getClient(), imm.getInputContext());
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to open window session", e);
                }
                iWindowSession = sWindowSession;
            } else {
                iWindowSession = sWindowSession;
            }
        }
        return iWindowSession;
    }

    public static IWindowSession peekWindowSession() {
        IWindowSession iWindowSession;
        synchronized (WindowManagerGlobal.class) {
            iWindowSession = sWindowSession;
        }
        return iWindowSession;
    }

    public String[] getViewRootNames() {
        String[] mViewRoots;
        synchronized (this.mLock) {
            int numRoots = this.mRoots.size();
            mViewRoots = new String[numRoots];
            for (int i = 0; i < numRoots; i++) {
                mViewRoots[i] = getWindowName(this.mRoots.get(i));
            }
        }
        return mViewRoots;
    }

    public View getRootView(String name) {
        synchronized (this.mLock) {
            for (int i = this.mRoots.size() - 1; i >= 0; i--) {
                ViewRootImpl root = this.mRoots.get(i);
                if (name.equals(getWindowName(root))) {
                    return root.getView();
                }
            }
            return null;
        }
    }

    public void addView(View view, ViewGroup.LayoutParams params, Display display, Window parentWindow) {
        ViewRootImpl root;
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        if (display == null) {
            throw new IllegalArgumentException("display must not be null");
        }
        if (!(params instanceof WindowManager.LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        if (parentWindow != null) {
            parentWindow.adjustLayoutParamsForSubWindow(wparams);
        } else {
            Context context = view.getContext();
            if (context != null && context.getApplicationInfo().targetSdkVersion >= 21) {
                wparams.flags |= 16777216;
            }
        }
        View panelParentView = null;
        synchronized (this.mLock) {
            if (this.mSystemPropertyUpdater == null) {
                this.mSystemPropertyUpdater = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (WindowManagerGlobal.this.mLock) {
                            for (int i = WindowManagerGlobal.this.mRoots.size() - 1; i >= 0; i--) {
                                ((ViewRootImpl) WindowManagerGlobal.this.mRoots.get(i)).loadSystemProperties();
                            }
                        }
                    }
                };
                SystemProperties.addChangeCallback(this.mSystemPropertyUpdater);
            }
            int index = findViewLocked(view, false);
            if (index >= 0) {
                if (this.mDyingViews.contains(view)) {
                    this.mRoots.get(index).doDie();
                } else {
                    throw new IllegalStateException("View " + view + " has already been added to the window manager.");
                }
            }
            if (wparams.type >= 1000 && wparams.type <= 1999) {
                int count = this.mViews.size();
                for (int i = 0; i < count; i++) {
                    if (this.mRoots.get(i).mWindow.asBinder() == wparams.token) {
                        panelParentView = this.mViews.get(i);
                    }
                }
            }
            root = new ViewRootImpl(view.getContext(), display);
            view.setLayoutParams(wparams);
            this.mViews.add(view);
            this.mRoots.add(root);
            this.mParams.add(wparams);
        }
        try {
            root.setView(view, wparams, panelParentView);
        } catch (RuntimeException e) {
            synchronized (this.mLock) {
                int index2 = findViewLocked(view, false);
                if (index2 >= 0) {
                    removeViewLocked(index2, true);
                }
                throw e;
            }
        }
    }

    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        if (!(params instanceof WindowManager.LayoutParams)) {
            throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");
        }
        WindowManager.LayoutParams wparams = (WindowManager.LayoutParams) params;
        view.setLayoutParams(wparams);
        synchronized (this.mLock) {
            int index = findViewLocked(view, true);
            ViewRootImpl root = this.mRoots.get(index);
            this.mParams.remove(index);
            this.mParams.add(index, wparams);
            root.setLayoutParams(wparams, false);
        }
    }

    public void removeView(View view, boolean immediate) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        synchronized (this.mLock) {
            int index = findViewLocked(view, true);
            View curView = this.mRoots.get(index).getView();
            removeViewLocked(index, immediate);
            if (curView != view) {
                throw new IllegalStateException("Calling with view " + view + " but the ViewAncestor is attached to " + curView);
            }
        }
    }

    public void closeAll(IBinder token, String who, String what) {
        synchronized (this.mLock) {
            int count = this.mViews.size();
            for (int i = 0; i < count; i++) {
                if (token == null || this.mParams.get(i).token == token) {
                    ViewRootImpl root = this.mRoots.get(i);
                    if (who != null) {
                        WindowLeaked leak = new WindowLeaked(what + " " + who + " has leaked window " + root.getView() + " that was originally added here");
                        leak.setStackTrace(root.getLocation().getStackTrace());
                        Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, leak);
                    }
                    removeViewLocked(i, false);
                }
            }
        }
    }

    private void removeViewLocked(int index, boolean immediate) {
        InputMethodManager imm;
        ViewRootImpl root = this.mRoots.get(index);
        View view = root.getView();
        if (view != null && (imm = InputMethodManager.getInstance()) != null) {
            imm.windowDismissed(this.mViews.get(index).getWindowToken());
        }
        boolean deferred = root.die(immediate);
        if (view != null) {
            view.assignParent(null);
            if (deferred) {
                this.mDyingViews.add(view);
            }
        }
    }

    void doRemoveView(ViewRootImpl root) {
        synchronized (this.mLock) {
            int index = this.mRoots.indexOf(root);
            if (index >= 0) {
                this.mRoots.remove(index);
                this.mParams.remove(index);
                View view = this.mViews.remove(index);
                this.mDyingViews.remove(view);
            }
        }
        if (HardwareRenderer.sTrimForeground && HardwareRenderer.isAvailable()) {
            doTrimForeground();
        }
    }

    private int findViewLocked(View view, boolean required) {
        int index = this.mViews.indexOf(view);
        if (required && index < 0) {
            throw new IllegalArgumentException("View=" + view + " not attached to window manager");
        }
        return index;
    }

    public static boolean shouldDestroyEglContext(int trimLevel) {
        if (trimLevel >= 80) {
            return true;
        }
        return trimLevel >= 60 && !ActivityManager.isHighEndGfx();
    }

    public void trimMemory(int level) {
        if (HardwareRenderer.isAvailable()) {
            if (shouldDestroyEglContext(level)) {
                synchronized (this.mLock) {
                    for (int i = this.mRoots.size() - 1; i >= 0; i--) {
                        this.mRoots.get(i).destroyHardwareResources();
                    }
                }
                level = 80;
            }
            HardwareRenderer.trimMemory(level);
            if (HardwareRenderer.sTrimForeground) {
                doTrimForeground();
            }
        }
    }

    public static void trimForeground() {
        if (HardwareRenderer.sTrimForeground && HardwareRenderer.isAvailable()) {
            WindowManagerGlobal wm = getInstance();
            wm.doTrimForeground();
        }
    }

    private void doTrimForeground() {
        boolean hasVisibleWindows = false;
        synchronized (this.mLock) {
            for (int i = this.mRoots.size() - 1; i >= 0; i--) {
                ViewRootImpl root = this.mRoots.get(i);
                if (root.mView != null && root.getHostVisibility() == 0 && root.mAttachInfo.mHardwareRenderer != null) {
                    hasVisibleWindows = true;
                } else {
                    root.destroyHardwareResources();
                }
            }
        }
        if (!hasVisibleWindows) {
            HardwareRenderer.trimMemory(80);
        }
    }

    public void dumpGfxInfo(FileDescriptor fd) {
        FileOutputStream fout = new FileOutputStream(fd);
        PrintWriter pw = new FastPrintWriter(fout);
        try {
            synchronized (this.mLock) {
                int count = this.mViews.size();
                pw.println("Profile data in ms:");
                for (int i = 0; i < count; i++) {
                    ViewRootImpl root = this.mRoots.get(i);
                    String name = getWindowName(root);
                    pw.printf("\n\t%s (visibility=%d)", name, Integer.valueOf(root.getHostVisibility()));
                    HardwareRenderer renderer = root.getView().mAttachInfo.mHardwareRenderer;
                    if (renderer != null) {
                        renderer.dumpGfxInfo(pw, fd);
                    }
                }
                pw.println("\nView hierarchy:\n");
                int viewsCount = 0;
                int displayListsSize = 0;
                int[] info = new int[2];
                for (int i2 = 0; i2 < count; i2++) {
                    ViewRootImpl root2 = this.mRoots.get(i2);
                    root2.dumpGfxInfo(info);
                    String name2 = getWindowName(root2);
                    pw.printf("  %s\n  %d views, %.2f kB of display lists", name2, Integer.valueOf(info[0]), Float.valueOf(info[1] / 1024.0f));
                    pw.printf("\n\n", new Object[0]);
                    viewsCount += info[0];
                    displayListsSize += info[1];
                }
                pw.printf("\nTotal ViewRootImpl: %d\n", Integer.valueOf(count));
                pw.printf("Total Views:        %d\n", Integer.valueOf(viewsCount));
                pw.printf("Total DisplayList:  %.2f kB\n\n", Float.valueOf(displayListsSize / 1024.0f));
            }
        } finally {
            pw.flush();
        }
    }

    private static String getWindowName(ViewRootImpl root) {
        return ((Object) root.mWindowAttributes.getTitle()) + "/" + root.getClass().getName() + '@' + Integer.toHexString(root.hashCode());
    }

    public void setStoppedState(IBinder token, boolean stopped) {
        synchronized (this.mLock) {
            int count = this.mViews.size();
            for (int i = 0; i < count; i++) {
                if (token == null || this.mParams.get(i).token == token) {
                    ViewRootImpl root = this.mRoots.get(i);
                    root.setStopped(stopped);
                }
            }
        }
    }

    public void reportNewConfiguration(Configuration config) {
        synchronized (this.mLock) {
            try {
                int count = this.mViews.size();
                Configuration config2 = new Configuration(config);
                for (int i = 0; i < count; i++) {
                    try {
                        ViewRootImpl root = this.mRoots.get(i);
                        root.requestUpdateConfiguration(config2);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void changeCanvasOpacity(IBinder token, boolean opaque) {
        if (token != null) {
            synchronized (this.mLock) {
                int i = this.mParams.size() - 1;
                while (true) {
                    if (i < 0) {
                        break;
                    }
                    if (this.mParams.get(i).token != token) {
                        i--;
                    } else {
                        this.mRoots.get(i).changeCanvasOpacity(opaque);
                        break;
                    }
                }
            }
        }
    }
}
