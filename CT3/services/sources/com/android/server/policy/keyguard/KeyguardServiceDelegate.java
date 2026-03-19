package com.android.server.policy.keyguard;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.server.UiThread;
import java.io.PrintWriter;

public class KeyguardServiceDelegate {
    private static final boolean DEBUG = true;
    private static final int INTERACTIVE_STATE_AWAKE = 1;
    private static final int INTERACTIVE_STATE_GOING_TO_SLEEP = 2;
    private static final int INTERACTIVE_STATE_SLEEP = 0;
    private static final int SCREEN_STATE_OFF = 0;
    private static final int SCREEN_STATE_ON = 2;
    private static final int SCREEN_STATE_TURNING_ON = 1;
    private static final String TAG = "KeyguardServiceDelegate";
    private final Context mContext;
    private DrawnListener mDrawnListenerWhenConnect;
    protected KeyguardServiceWrapper mKeyguardService;
    private final View mScrim;
    private final KeyguardState mKeyguardState = new KeyguardState();
    private final ServiceConnection mKeyguardConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(KeyguardServiceDelegate.TAG, "*** Keyguard connected (yay!)");
            KeyguardServiceDelegate.this.mKeyguardService = new KeyguardServiceWrapper(KeyguardServiceDelegate.this.mContext, IKeyguardService.Stub.asInterface(service));
            if (KeyguardServiceDelegate.this.mKeyguardState.systemIsReady) {
                KeyguardServiceDelegate.this.mKeyguardService.onSystemReady();
                if (KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 1) {
                    KeyguardServiceDelegate.this.mKeyguardService.onStartedWakingUp();
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.screenState == 2 || KeyguardServiceDelegate.this.mKeyguardState.screenState == 1) {
                    KeyguardServiceDelegate.this.mKeyguardService.onScreenTurningOn(KeyguardServiceDelegate.this.new KeyguardShowDelegate(KeyguardServiceDelegate.this.mDrawnListenerWhenConnect));
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.screenState == 2) {
                    KeyguardServiceDelegate.this.mKeyguardService.onScreenTurnedOn();
                }
                KeyguardServiceDelegate.this.mDrawnListenerWhenConnect = null;
            }
            if (KeyguardServiceDelegate.this.mKeyguardState.bootCompleted) {
                KeyguardServiceDelegate.this.mKeyguardService.onBootCompleted();
            }
            if (!KeyguardServiceDelegate.this.mKeyguardState.occluded) {
                return;
            }
            KeyguardServiceDelegate.this.mKeyguardService.setOccluded(KeyguardServiceDelegate.this.mKeyguardState.occluded);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(KeyguardServiceDelegate.TAG, "*** Keyguard disconnected (boo!)");
            KeyguardServiceDelegate.this.mKeyguardService = null;
        }
    };
    private final Handler mScrimHandler = UiThread.getHandler();

    public interface DrawnListener {
        void onDrawn();
    }

    private static final class KeyguardState {
        public boolean bootCompleted;
        public int currentUser;
        boolean dreaming;
        public boolean enabled;
        boolean inputRestricted;
        public int interactiveState;
        boolean occluded;
        public int offReason;
        public int screenState;
        boolean systemIsReady;
        boolean showing = true;
        boolean showingAndNotOccluded = true;
        boolean secure = true;
        boolean deviceHasKeyguard = true;

        KeyguardState() {
        }
    }

    private final class KeyguardShowDelegate extends IKeyguardDrawnCallback.Stub {
        private DrawnListener mDrawnListener;

        KeyguardShowDelegate(DrawnListener drawnListener) {
            this.mDrawnListener = drawnListener;
        }

        public void onDrawn() throws RemoteException {
            Log.v(KeyguardServiceDelegate.TAG, "**** SHOWN CALLED ****");
            if (this.mDrawnListener != null) {
                this.mDrawnListener.onDrawn();
            }
            KeyguardServiceDelegate.this.hideScrim();
        }
    }

    private final class KeyguardExitDelegate extends IKeyguardExitCallback.Stub {
        private WindowManagerPolicy.OnKeyguardExitResult mOnKeyguardExitResult;

        KeyguardExitDelegate(WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult) {
            this.mOnKeyguardExitResult = onKeyguardExitResult;
        }

        public void onKeyguardExitResult(boolean success) throws RemoteException {
            Log.v(KeyguardServiceDelegate.TAG, "**** onKeyguardExitResult(" + success + ") CALLED ****");
            if (this.mOnKeyguardExitResult == null) {
                return;
            }
            this.mOnKeyguardExitResult.onKeyguardExitResult(success);
        }
    }

    public KeyguardServiceDelegate(Context context) {
        this.mContext = context;
        this.mScrim = createScrim(context, this.mScrimHandler);
    }

    public void bindService(Context context) {
        Intent intent = new Intent();
        Resources resources = context.getApplicationContext().getResources();
        ComponentName keyguardComponent = ComponentName.unflattenFromString(resources.getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_IN_PROGRESS));
        intent.addFlags(256);
        intent.setComponent(keyguardComponent);
        if (!context.bindServiceAsUser(intent, this.mKeyguardConnection, 1, this.mScrimHandler, UserHandle.SYSTEM)) {
            Log.v(TAG, "*** Keyguard: can't bind to " + keyguardComponent);
            this.mKeyguardState.showing = false;
            this.mKeyguardState.showingAndNotOccluded = false;
            this.mKeyguardState.secure = false;
            synchronized (this.mKeyguardState) {
                this.mKeyguardState.deviceHasKeyguard = false;
                hideScrim();
            }
            return;
        }
        Log.v(TAG, "*** Keyguard started");
    }

    public boolean isShowing() {
        if (this.mKeyguardService != null) {
            this.mKeyguardState.showing = this.mKeyguardService.isShowing();
        }
        return this.mKeyguardState.showing;
    }

    public boolean isInputRestricted() {
        if (this.mKeyguardService != null) {
            this.mKeyguardState.inputRestricted = this.mKeyguardService.isInputRestricted();
        }
        return this.mKeyguardState.inputRestricted;
    }

    public void verifyUnlock(WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult) {
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.verifyUnlock(new KeyguardExitDelegate(onKeyguardExitResult));
    }

    public void keyguardDone(boolean authenticated, boolean wakeup) {
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.keyguardDone(authenticated, wakeup);
    }

    public void setOccluded(boolean isOccluded) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setOccluded(isOccluded);
        }
        this.mKeyguardState.occluded = isOccluded;
    }

    public void dismiss() {
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.dismiss();
    }

    public boolean isSecure(int userId) {
        if (this.mKeyguardService != null) {
            this.mKeyguardState.secure = this.mKeyguardService.isSecure(userId);
        }
        return this.mKeyguardState.secure;
    }

    public void onDreamingStarted() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onDreamingStarted();
        }
        this.mKeyguardState.dreaming = true;
    }

    public void onDreamingStopped() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onDreamingStopped();
        }
        this.mKeyguardState.dreaming = false;
    }

    public void onStartedWakingUp() {
        if (this.mKeyguardService != null) {
            Log.v(TAG, "onStartedWakingUp()");
            this.mKeyguardService.onStartedWakingUp();
        }
        this.mKeyguardState.interactiveState = 1;
    }

    public void onScreenTurnedOff() {
        if (this.mKeyguardService != null) {
            Log.v(TAG, "onScreenTurnedOff()");
            this.mKeyguardService.onScreenTurnedOff();
        }
        this.mKeyguardState.screenState = 0;
    }

    public void onScreenTurningOn(DrawnListener drawnListener) {
        if (this.mKeyguardService != null) {
            Log.v(TAG, "onScreenTurnedOn(showListener = " + drawnListener + ")");
            this.mKeyguardService.onScreenTurningOn(new KeyguardShowDelegate(drawnListener));
        } else {
            Slog.w(TAG, "onScreenTurningOn(): no keyguard service!");
            this.mDrawnListenerWhenConnect = drawnListener;
            showScrim();
        }
        this.mKeyguardState.screenState = 1;
    }

    public void onScreenTurnedOn() {
        if (this.mKeyguardService != null) {
            Log.v(TAG, "onScreenTurnedOn()");
            this.mKeyguardService.onScreenTurnedOn();
        }
        this.mKeyguardState.screenState = 2;
    }

    public void onStartedGoingToSleep(int why) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onStartedGoingToSleep(why);
        }
        this.mKeyguardState.offReason = why;
        this.mKeyguardState.interactiveState = 2;
    }

    public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onFinishedGoingToSleep(why, cameraGestureTriggered);
        }
        this.mKeyguardState.interactiveState = 0;
    }

    public void setKeyguardEnabled(boolean enabled) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setKeyguardEnabled(enabled);
        }
        this.mKeyguardState.enabled = enabled;
    }

    public void onSystemReady() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onSystemReady();
        } else {
            this.mKeyguardState.systemIsReady = true;
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.doKeyguardTimeout(options);
    }

    public void setCurrentUser(int newUserId) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setCurrentUser(newUserId);
        }
        this.mKeyguardState.currentUser = newUserId;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.startKeyguardExitAnimation(startTime, fadeoutDuration);
    }

    private static View createScrim(Context context, Handler handler) {
        final View view = new View(context);
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2029, 1116416, -3);
        lp.softInputMode = 16;
        lp.screenOrientation = 5;
        lp.privateFlags |= 1;
        lp.setTitle("KeyguardScrim");
        final WindowManager wm = (WindowManager) context.getSystemService("window");
        view.setSystemUiVisibility(56688640);
        handler.post(new Runnable() {
            @Override
            public void run() {
                wm.addView(view, lp);
            }
        });
        return view;
    }

    public void showScrim() {
        synchronized (this.mKeyguardState) {
            if (this.mKeyguardState.deviceHasKeyguard) {
                this.mScrimHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        KeyguardServiceDelegate.this.mScrim.setVisibility(0);
                    }
                });
            }
        }
    }

    public void hideScrim() {
        this.mScrimHandler.post(new Runnable() {
            @Override
            public void run() {
                KeyguardServiceDelegate.this.mScrim.setVisibility(8);
            }
        });
    }

    public void onBootCompleted() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onBootCompleted();
        }
        this.mKeyguardState.bootCompleted = true;
    }

    public void onActivityDrawn() {
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.onActivityDrawn();
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "showing=" + this.mKeyguardState.showing);
        pw.println(prefix2 + "showingAndNotOccluded=" + this.mKeyguardState.showingAndNotOccluded);
        pw.println(prefix2 + "inputRestricted=" + this.mKeyguardState.inputRestricted);
        pw.println(prefix2 + "occluded=" + this.mKeyguardState.occluded);
        pw.println(prefix2 + "secure=" + this.mKeyguardState.secure);
        pw.println(prefix2 + "dreaming=" + this.mKeyguardState.dreaming);
        pw.println(prefix2 + "systemIsReady=" + this.mKeyguardState.systemIsReady);
        pw.println(prefix2 + "deviceHasKeyguard=" + this.mKeyguardState.deviceHasKeyguard);
        pw.println(prefix2 + "enabled=" + this.mKeyguardState.enabled);
        pw.println(prefix2 + "offReason=" + this.mKeyguardState.offReason);
        pw.println(prefix2 + "currentUser=" + this.mKeyguardState.currentUser);
        pw.println(prefix2 + "bootCompleted=" + this.mKeyguardState.bootCompleted);
        pw.println(prefix2 + "screenState=" + this.mKeyguardState.screenState);
        pw.println(prefix2 + "interactiveState=" + this.mKeyguardState.interactiveState);
        if (this.mKeyguardService == null) {
            return;
        }
        this.mKeyguardService.dump(prefix2, pw);
    }
}
