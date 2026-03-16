package android.service.dreams;

import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.dreams.IDreamManager;
import android.service.dreams.IDreamService;
import android.util.MathUtils;
import android.util.Slog;
import android.view.ActionMode;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.R;
import com.android.internal.policy.PolicyManager;
import com.android.internal.util.DumpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DreamService extends Service implements Window.Callback {
    public static final String DREAM_META_DATA = "android.service.dream";
    public static final String DREAM_SERVICE = "dreams";
    public static final String SERVICE_INTERFACE = "android.service.dreams.DreamService";
    private boolean mCanDoze;
    private boolean mDozing;
    private boolean mFinished;
    private boolean mFullscreen;
    private boolean mInteractive;
    private boolean mStarted;
    private boolean mWaking;
    private Window mWindow;
    private IBinder mWindowToken;
    private boolean mWindowless;
    private final String TAG = DreamService.class.getSimpleName() + "[" + getClass().getSimpleName() + "]";
    private final Handler mHandler = new Handler();
    private boolean mLowProfile = true;
    private boolean mScreenBright = true;
    private int mDozeScreenState = 0;
    private int mDozeScreenBrightness = -1;
    private boolean mDebug = false;
    private final IDreamManager mSandman = IDreamManager.Stub.asInterface(ServiceManager.getService(DREAM_SERVICE));

    public void setDebug(boolean dbg) {
        this.mDebug = dbg;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!this.mInteractive) {
            if (this.mDebug) {
                Slog.v(this.TAG, "Waking up on keyEvent");
            }
            wakeUp();
            return true;
        }
        if (event.getKeyCode() == 4) {
            if (this.mDebug) {
                Slog.v(this.TAG, "Waking up on back key");
            }
            wakeUp();
            return true;
        }
        return this.mWindow.superDispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        if (this.mInteractive) {
            return this.mWindow.superDispatchKeyShortcutEvent(event);
        }
        if (this.mDebug) {
            Slog.v(this.TAG, "Waking up on keyShortcutEvent");
        }
        wakeUp();
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (this.mInteractive) {
            return this.mWindow.superDispatchTouchEvent(event);
        }
        if (this.mDebug) {
            Slog.v(this.TAG, "Waking up on touchEvent");
        }
        wakeUp();
        return true;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        if (this.mInteractive) {
            return this.mWindow.superDispatchTrackballEvent(event);
        }
        if (this.mDebug) {
            Slog.v(this.TAG, "Waking up on trackballEvent");
        }
        wakeUp();
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (this.mInteractive || event.getDeviceId() == 4) {
            return this.mWindow.superDispatchGenericMotionEvent(event);
        }
        if (this.mDebug) {
            Slog.v(this.TAG, "Waking up on genericMotionEvent");
        }
        wakeUp();
        return true;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    @Override
    public View onCreatePanelView(int featureId) {
        return null;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        return false;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return false;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return false;
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
    }

    @Override
    public void onContentChanged() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override
    public void onAttachedToWindow() {
    }

    @Override
    public void onDetachedFromWindow() {
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return null;
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
    }

    public WindowManager getWindowManager() {
        if (this.mWindow != null) {
            return this.mWindow.getWindowManager();
        }
        return null;
    }

    public Window getWindow() {
        return this.mWindow;
    }

    public void setContentView(int layoutResID) {
        getWindow().setContentView(layoutResID);
    }

    public void setContentView(View view) {
        getWindow().setContentView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getWindow().setContentView(view, params);
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getWindow().addContentView(view, params);
    }

    public View findViewById(int id) {
        return getWindow().findViewById(id);
    }

    public void setInteractive(boolean interactive) {
        this.mInteractive = interactive;
    }

    public boolean isInteractive() {
        return this.mInteractive;
    }

    public void setLowProfile(boolean lowProfile) {
        int i;
        if (this.mLowProfile != lowProfile) {
            this.mLowProfile = lowProfile;
            if (this.mLowProfile) {
                i = 1;
            } else {
                i = 0;
            }
            applySystemUiVisibilityFlags(i, 1);
        }
    }

    public boolean isLowProfile() {
        return getSystemUiVisibilityFlagValue(1, this.mLowProfile);
    }

    public void setFullscreen(boolean fullscreen) {
        int i;
        if (this.mFullscreen != fullscreen) {
            this.mFullscreen = fullscreen;
            if (this.mFullscreen) {
                i = 1024;
            } else {
                i = 0;
            }
            applyWindowFlags(i, 1024);
        }
    }

    public boolean isFullscreen() {
        return this.mFullscreen;
    }

    public void setScreenBright(boolean screenBright) {
        int i;
        if (this.mScreenBright != screenBright) {
            this.mScreenBright = screenBright;
            if (this.mScreenBright) {
                i = 128;
            } else {
                i = 0;
            }
            applyWindowFlags(i, 128);
        }
    }

    public boolean isScreenBright() {
        return getWindowFlagValue(128, this.mScreenBright);
    }

    public void setWindowless(boolean windowless) {
        this.mWindowless = windowless;
    }

    public boolean isWindowless() {
        return this.mWindowless;
    }

    public boolean canDoze() {
        return this.mCanDoze;
    }

    public void startDozing() {
        if (this.mCanDoze && !this.mDozing) {
            this.mDozing = true;
            updateDoze();
        }
    }

    private void updateDoze() {
        if (this.mDozing) {
            try {
                this.mSandman.startDozing(this.mWindowToken, this.mDozeScreenState, this.mDozeScreenBrightness);
            } catch (RemoteException e) {
            }
        }
    }

    public void stopDozing() {
        if (this.mDozing) {
            this.mDozing = false;
            try {
                this.mSandman.stopDozing(this.mWindowToken);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public int getDozeScreenState() {
        return this.mDozeScreenState;
    }

    public void setDozeScreenState(int state) {
        if (this.mDozeScreenState != state) {
            this.mDozeScreenState = state;
            updateDoze();
        }
    }

    public int getDozeScreenBrightness() {
        return this.mDozeScreenBrightness;
    }

    public void setDozeScreenBrightness(int brightness) {
        if (brightness != -1) {
            brightness = clampAbsoluteBrightness(brightness);
        }
        if (this.mDozeScreenBrightness != brightness) {
            this.mDozeScreenBrightness = brightness;
            updateDoze();
        }
    }

    @Override
    public void onCreate() {
        if (this.mDebug) {
            Slog.v(this.TAG, "onCreate()");
        }
        super.onCreate();
    }

    public void onDreamingStarted() {
        if (this.mDebug) {
            Slog.v(this.TAG, "onDreamingStarted()");
        }
    }

    public void onDreamingStopped() {
        if (this.mDebug) {
            Slog.v(this.TAG, "onDreamingStopped()");
        }
    }

    public void onWakeUp() {
        finish();
    }

    @Override
    public final IBinder onBind(Intent intent) {
        if (this.mDebug) {
            Slog.v(this.TAG, "onBind() intent = " + intent);
        }
        return new DreamServiceWrapper();
    }

    public final void finish() {
        if (this.mDebug) {
            Slog.v(this.TAG, "finish(): mFinished=" + this.mFinished);
        }
        if (!this.mFinished) {
            this.mFinished = true;
            if (this.mWindowToken == null) {
                Slog.w(this.TAG, "Finish was called before the dream was attached.");
            } else {
                try {
                    this.mSandman.finishSelf(this.mWindowToken, true);
                } catch (RemoteException e) {
                }
            }
            stopSelf();
        }
    }

    public final void wakeUp() {
        wakeUp(false);
    }

    private void wakeUp(boolean fromSystem) {
        if (this.mDebug) {
            Slog.v(this.TAG, "wakeUp(): fromSystem=" + fromSystem + ", mWaking=" + this.mWaking + ", mFinished=" + this.mFinished);
        }
        if (!this.mWaking && !this.mFinished) {
            this.mWaking = true;
            onWakeUp();
            if (!fromSystem && !this.mFinished) {
                if (this.mWindowToken == null) {
                    Slog.w(this.TAG, "WakeUp was called before the dream was attached.");
                } else {
                    try {
                        this.mSandman.finishSelf(this.mWindowToken, false);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (this.mDebug) {
            Slog.v(this.TAG, "onDestroy()");
        }
        detach();
        super.onDestroy();
    }

    private final void detach() {
        if (this.mStarted) {
            if (this.mDebug) {
                Slog.v(this.TAG, "detach(): Calling onDreamingStopped()");
            }
            this.mStarted = false;
            onDreamingStopped();
        }
        if (this.mWindow != null) {
            if (this.mDebug) {
                Slog.v(this.TAG, "detach(): Removing window from window manager");
            }
            this.mWindow.getWindowManager().removeViewImmediate(this.mWindow.getDecorView());
            this.mWindow = null;
        }
        if (this.mWindowToken != null) {
            WindowManagerGlobal.getInstance().closeAll(this.mWindowToken, getClass().getName(), "Dream");
            this.mWindowToken = null;
            this.mCanDoze = false;
        }
    }

    private final void attach(IBinder windowToken, boolean canDoze) {
        if (this.mWindowToken != null) {
            Slog.e(this.TAG, "attach() called when already attached with token=" + this.mWindowToken);
            return;
        }
        if (this.mFinished || this.mWaking) {
            Slog.w(this.TAG, "attach() called after dream already finished");
            try {
                this.mSandman.finishSelf(windowToken, true);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        this.mWindowToken = windowToken;
        this.mCanDoze = canDoze;
        if (this.mWindowless && !this.mCanDoze) {
            throw new IllegalStateException("Only doze dreams can be windowless");
        }
        if (!this.mWindowless) {
            this.mWindow = PolicyManager.makeNewWindow(this);
            this.mWindow.setCallback(this);
            this.mWindow.requestFeature(1);
            this.mWindow.setBackgroundDrawable(new ColorDrawable(-16777216));
            this.mWindow.setFormat(-1);
            if (this.mDebug) {
                Slog.v(this.TAG, String.format("Attaching window token: %s to window of type %s", windowToken, Integer.valueOf(WindowManager.LayoutParams.TYPE_DREAM)));
            }
            WindowManager.LayoutParams lp = this.mWindow.getAttributes();
            lp.type = WindowManager.LayoutParams.TYPE_DREAM;
            lp.token = windowToken;
            lp.windowAnimations = R.style.Animation_Dream;
            lp.flags = (this.mScreenBright ? 128 : 0) | 4784385 | (this.mFullscreen ? 1024 : 0) | lp.flags;
            this.mWindow.setAttributes(lp);
            this.mWindow.clearFlags(Integer.MIN_VALUE);
            this.mWindow.setWindowManager(null, windowToken, "dream", true);
            applySystemUiVisibilityFlags(this.mLowProfile ? 1 : 0, 1);
            try {
                getWindowManager().addView(this.mWindow.getDecorView(), this.mWindow.getAttributes());
            } catch (WindowManager.BadTokenException e2) {
                Slog.i(this.TAG, "attach() called after window token already removed, dream will finish soon");
                this.mWindow = null;
                return;
            }
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (DreamService.this.mWindow != null || DreamService.this.mWindowless) {
                    if (DreamService.this.mDebug) {
                        Slog.v(DreamService.this.TAG, "Calling onDreamingStarted()");
                    }
                    DreamService.this.mStarted = true;
                    DreamService.this.onDreamingStarted();
                }
            }
        });
    }

    private boolean getWindowFlagValue(int flag, boolean defaultValue) {
        return this.mWindow == null ? defaultValue : (this.mWindow.getAttributes().flags & flag) != 0;
    }

    private void applyWindowFlags(int flags, int mask) {
        if (this.mWindow != null) {
            WindowManager.LayoutParams lp = this.mWindow.getAttributes();
            lp.flags = applyFlags(lp.flags, flags, mask);
            this.mWindow.setAttributes(lp);
            this.mWindow.getWindowManager().updateViewLayout(this.mWindow.getDecorView(), lp);
        }
    }

    private boolean getSystemUiVisibilityFlagValue(int flag, boolean defaultValue) {
        View v = this.mWindow == null ? null : this.mWindow.getDecorView();
        return v == null ? defaultValue : (v.getSystemUiVisibility() & flag) != 0;
    }

    private void applySystemUiVisibilityFlags(int flags, int mask) {
        View v = this.mWindow == null ? null : this.mWindow.getDecorView();
        if (v != null) {
            v.setSystemUiVisibility(applyFlags(v.getSystemUiVisibility(), flags, mask));
        }
    }

    private int applyFlags(int oldFlags, int flags, int mask) {
        return ((mask ^ (-1)) & oldFlags) | (flags & mask);
    }

    @Override
    protected void dump(final FileDescriptor fd, PrintWriter pw, final String[] args) {
        DumpUtils.dumpAsync(this.mHandler, new DumpUtils.Dump() {
            @Override
            public void dump(PrintWriter pw2) {
                DreamService.this.dumpOnHandler(fd, pw2, args);
            }
        }, pw, 1000L);
    }

    protected void dumpOnHandler(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print(this.TAG + ": ");
        if (this.mWindowToken == null) {
            pw.println("stopped");
        } else {
            pw.println("running (token=" + this.mWindowToken + ")");
        }
        pw.println("  window: " + this.mWindow);
        pw.print("  flags:");
        if (isInteractive()) {
            pw.print(" interactive");
        }
        if (isLowProfile()) {
            pw.print(" lowprofile");
        }
        if (isFullscreen()) {
            pw.print(" fullscreen");
        }
        if (isScreenBright()) {
            pw.print(" bright");
        }
        if (isWindowless()) {
            pw.print(" windowless");
        }
        if (isDozing()) {
            pw.print(" dozing");
        } else if (canDoze()) {
            pw.print(" candoze");
        }
        pw.println();
        if (canDoze()) {
            pw.println("  doze screen state: " + Display.stateToString(this.mDozeScreenState));
            pw.println("  doze screen brightness: " + this.mDozeScreenBrightness);
        }
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, 255);
    }

    private final class DreamServiceWrapper extends IDreamService.Stub {
        private DreamServiceWrapper() {
        }

        @Override
        public void attach(final IBinder windowToken, final boolean canDoze) {
            DreamService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamService.this.attach(windowToken, canDoze);
                }
            });
        }

        @Override
        public void detach() {
            DreamService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamService.this.detach();
                }
            });
        }

        @Override
        public void wakeUp() {
            DreamService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamService.this.wakeUp(true);
                }
            });
        }
    }
}
