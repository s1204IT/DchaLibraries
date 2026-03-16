package com.android.internal.policy.impl.keyguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardShowCallback;

public class KeyguardServiceDelegate {
    private static final boolean DEBUG = true;
    public static final String KEYGUARD_CLASS = "com.android.systemui.keyguard.KeyguardService";
    public static final String KEYGUARD_PACKAGE = "com.android.systemui";
    private static final String TAG = "KeyguardServiceDelegate";
    private final Context mContext;
    protected KeyguardServiceWrapper mKeyguardService;
    private final View mScrim;
    private ShowListener mShowListenerWhenConnect;
    private final KeyguardState mKeyguardState = new KeyguardState();
    private final ServiceConnection mKeyguardConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(KeyguardServiceDelegate.TAG, "*** Keyguard connected (yay!)");
            KeyguardServiceDelegate.this.mKeyguardService = new KeyguardServiceWrapper(KeyguardServiceDelegate.this.mContext, IKeyguardService.Stub.asInterface(service));
            if (KeyguardServiceDelegate.this.mKeyguardState.systemIsReady) {
                KeyguardServiceDelegate.this.mKeyguardService.onSystemReady();
                KeyguardServiceDelegate.this.mKeyguardService.onScreenTurnedOn(KeyguardServiceDelegate.this.new KeyguardShowDelegate(KeyguardServiceDelegate.this.mShowListenerWhenConnect));
                KeyguardServiceDelegate.this.mShowListenerWhenConnect = null;
            }
            if (KeyguardServiceDelegate.this.mKeyguardState.bootCompleted) {
                KeyguardServiceDelegate.this.mKeyguardService.onBootCompleted();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(KeyguardServiceDelegate.TAG, "*** Keyguard disconnected (boo!)");
            KeyguardServiceDelegate.this.mKeyguardService = null;
        }
    };

    public interface ShowListener {
        void onShown(IBinder iBinder);
    }

    static final class KeyguardState {
        public boolean bootCompleted;
        public int currentUser;
        public boolean dismissable;
        boolean dreaming;
        public boolean enabled;
        boolean inputRestricted;
        boolean occluded;
        public int offReason;
        public boolean screenIsOn;
        boolean systemIsReady;
        boolean showing = KeyguardServiceDelegate.DEBUG;
        boolean showingAndNotOccluded = KeyguardServiceDelegate.DEBUG;
        boolean secure = KeyguardServiceDelegate.DEBUG;
        boolean deviceHasKeyguard = KeyguardServiceDelegate.DEBUG;

        KeyguardState() {
        }
    }

    private final class KeyguardShowDelegate extends IKeyguardShowCallback.Stub {
        private ShowListener mShowListener;

        KeyguardShowDelegate(ShowListener showListener) {
            this.mShowListener = showListener;
        }

        public void onShown(IBinder windowToken) throws RemoteException {
            Log.v(KeyguardServiceDelegate.TAG, "**** SHOWN CALLED ****");
            if (this.mShowListener != null) {
                this.mShowListener.onShown(windowToken);
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
            if (this.mOnKeyguardExitResult != null) {
                this.mOnKeyguardExitResult.onKeyguardExitResult(success);
            }
        }
    }

    public KeyguardServiceDelegate(Context context) {
        this.mContext = context;
        this.mScrim = createScrim(context);
    }

    public void bindService(Context context) {
        Intent intent = new Intent();
        intent.setClassName(KEYGUARD_PACKAGE, KEYGUARD_CLASS);
        if (!context.bindServiceAsUser(intent, this.mKeyguardConnection, 1, UserHandle.OWNER)) {
            Log.v(TAG, "*** Keyguard: can't bind to com.android.systemui.keyguard.KeyguardService");
            this.mKeyguardState.showing = false;
            this.mKeyguardState.showingAndNotOccluded = false;
            this.mKeyguardState.secure = false;
            this.mKeyguardState.deviceHasKeyguard = false;
            hideScrim();
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
        if (this.mKeyguardService != null) {
            this.mKeyguardService.verifyUnlock(new KeyguardExitDelegate(onKeyguardExitResult));
        }
    }

    public void keyguardDone(boolean authenticated, boolean wakeup) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.keyguardDone(authenticated, wakeup);
        }
    }

    public void setOccluded(boolean isOccluded) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setOccluded(isOccluded);
        }
        this.mKeyguardState.occluded = isOccluded;
    }

    public void dismiss() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.dismiss();
        }
    }

    public boolean isSecure() {
        if (this.mKeyguardService != null) {
            this.mKeyguardState.secure = this.mKeyguardService.isSecure();
        }
        return this.mKeyguardState.secure;
    }

    public void onDreamingStarted() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onDreamingStarted();
        }
        this.mKeyguardState.dreaming = DEBUG;
    }

    public void onDreamingStopped() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onDreamingStopped();
        }
        this.mKeyguardState.dreaming = false;
    }

    public void onScreenTurnedOn(ShowListener showListener) {
        if (this.mKeyguardService != null) {
            Log.v(TAG, "onScreenTurnedOn(showListener = " + showListener + ")");
            this.mKeyguardService.onScreenTurnedOn(new KeyguardShowDelegate(showListener));
        } else {
            Slog.w(TAG, "onScreenTurnedOn(): no keyguard service!");
            this.mShowListenerWhenConnect = showListener;
            showScrim();
        }
        this.mKeyguardState.screenIsOn = DEBUG;
    }

    public void onScreenTurnedOff(int why) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onScreenTurnedOff(why);
        }
        this.mKeyguardState.offReason = why;
        this.mKeyguardState.screenIsOn = false;
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
            this.mKeyguardState.systemIsReady = DEBUG;
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.doKeyguardTimeout(options);
        }
    }

    public void setCurrentUser(int newUserId) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setCurrentUser(newUserId);
        }
        this.mKeyguardState.currentUser = newUserId;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }
    }

    private static final View createScrim(Context context) {
        View view = new View(context);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2029, 1116416, -3);
        lp.softInputMode = 16;
        lp.screenOrientation = 5;
        lp.privateFlags |= 1;
        lp.setTitle("KeyguardScrim");
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.addView(view, lp);
        view.setSystemUiVisibility(56688640);
        return view;
    }

    public void showScrim() {
        if (this.mKeyguardState.deviceHasKeyguard) {
            this.mScrim.post(new Runnable() {
                @Override
                public void run() {
                    KeyguardServiceDelegate.this.mScrim.setVisibility(0);
                }
            });
        }
    }

    public void hideScrim() {
        this.mScrim.post(new Runnable() {
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
        this.mKeyguardState.bootCompleted = DEBUG;
    }

    public void onActivityDrawn() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onActivityDrawn();
        }
    }
}
