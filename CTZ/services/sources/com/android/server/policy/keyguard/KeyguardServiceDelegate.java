package com.android.server.policy.keyguard;

import android.R;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.server.UiThread;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import java.io.PrintWriter;

public class KeyguardServiceDelegate {
    private static final boolean DEBUG = false;
    private static final int INTERACTIVE_STATE_AWAKE = 2;
    private static final int INTERACTIVE_STATE_GOING_TO_SLEEP = 3;
    private static final int INTERACTIVE_STATE_SLEEP = 0;
    private static final int INTERACTIVE_STATE_WAKING = 1;
    private static final int SCREEN_STATE_OFF = 0;
    private static final int SCREEN_STATE_ON = 2;
    private static final int SCREEN_STATE_TURNING_OFF = 3;
    private static final int SCREEN_STATE_TURNING_ON = 1;
    private static final String TAG = "KeyguardServiceDelegate";
    private final KeyguardStateMonitor.StateCallback mCallback;
    private final Context mContext;
    private DrawnListener mDrawnListenerWhenConnect;
    protected KeyguardServiceWrapper mKeyguardService;
    private final KeyguardState mKeyguardState = new KeyguardState();
    private final ServiceConnection mKeyguardConnection = new AnonymousClass1();
    private final Handler mHandler = UiThread.getHandler();

    public interface DrawnListener {
        void onDrawn();
    }

    private static final class KeyguardState {
        public boolean bootCompleted;
        public int currentUser;
        boolean deviceHasKeyguard;
        boolean dreaming;
        public boolean enabled;
        boolean inputRestricted;
        public int interactiveState;
        boolean occluded;
        public int offReason;
        public int screenState;
        boolean secure;
        boolean showing;
        boolean showingAndNotOccluded;
        boolean systemIsReady;

        KeyguardState() {
            reset();
        }

        private void reset() {
            this.showing = true;
            this.showingAndNotOccluded = true;
            this.secure = true;
            this.deviceHasKeyguard = true;
            this.enabled = true;
            this.currentUser = -10000;
        }
    }

    private final class KeyguardShowDelegate extends IKeyguardDrawnCallback.Stub {
        private DrawnListener mDrawnListener;

        KeyguardShowDelegate(DrawnListener drawnListener) {
            this.mDrawnListener = drawnListener;
        }

        public void onDrawn() throws RemoteException {
            if (this.mDrawnListener != null) {
                this.mDrawnListener.onDrawn();
            }
        }
    }

    private final class KeyguardExitDelegate extends IKeyguardExitCallback.Stub {
        private WindowManagerPolicy.OnKeyguardExitResult mOnKeyguardExitResult;

        KeyguardExitDelegate(WindowManagerPolicy.OnKeyguardExitResult onKeyguardExitResult) {
            this.mOnKeyguardExitResult = onKeyguardExitResult;
        }

        public void onKeyguardExitResult(boolean z) throws RemoteException {
            if (this.mOnKeyguardExitResult != null) {
                this.mOnKeyguardExitResult.onKeyguardExitResult(z);
            }
        }
    }

    public KeyguardServiceDelegate(Context context, KeyguardStateMonitor.StateCallback stateCallback) {
        this.mContext = context;
        this.mCallback = stateCallback;
    }

    public void bindService(Context context) {
        Intent intent = new Intent();
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(context.getApplicationContext().getResources().getString(R.string.alternative_fp_setup_notification_content));
        intent.addFlags(256);
        intent.setComponent(componentNameUnflattenFromString);
        if (!context.bindServiceAsUser(intent, this.mKeyguardConnection, 1, this.mHandler, UserHandle.SYSTEM)) {
            Log.v(TAG, "*** Keyguard: can't bind to " + componentNameUnflattenFromString);
            this.mKeyguardState.showing = false;
            this.mKeyguardState.showingAndNotOccluded = false;
            this.mKeyguardState.secure = false;
            synchronized (this.mKeyguardState) {
                this.mKeyguardState.deviceHasKeyguard = false;
            }
        }
    }

    class AnonymousClass1 implements ServiceConnection {
        AnonymousClass1() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            KeyguardServiceDelegate.this.mKeyguardService = new KeyguardServiceWrapper(KeyguardServiceDelegate.this.mContext, IKeyguardService.Stub.asInterface(iBinder), KeyguardServiceDelegate.this.mCallback);
            if (KeyguardServiceDelegate.this.mKeyguardState.systemIsReady) {
                KeyguardServiceDelegate.this.mKeyguardService.onSystemReady();
                if (KeyguardServiceDelegate.this.mKeyguardState.currentUser != -10000) {
                    KeyguardServiceDelegate.this.mKeyguardService.setCurrentUser(KeyguardServiceDelegate.this.mKeyguardState.currentUser);
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 2 || KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 1) {
                    KeyguardServiceDelegate.this.mKeyguardService.onStartedWakingUp();
                }
                if (KeyguardServiceDelegate.this.mKeyguardState.interactiveState == 2) {
                    KeyguardServiceDelegate.this.mKeyguardService.onFinishedWakingUp();
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
            if (KeyguardServiceDelegate.this.mKeyguardState.occluded) {
                KeyguardServiceDelegate.this.mKeyguardService.setOccluded(KeyguardServiceDelegate.this.mKeyguardState.occluded, false);
            }
            if (!KeyguardServiceDelegate.this.mKeyguardState.enabled) {
                KeyguardServiceDelegate.this.mKeyguardService.setKeyguardEnabled(KeyguardServiceDelegate.this.mKeyguardState.enabled);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            KeyguardServiceDelegate.this.mKeyguardService = null;
            KeyguardServiceDelegate.this.mKeyguardState.reset();
            KeyguardServiceDelegate.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    ActivityManager.getService().setLockScreenShown(true, false, -1);
                }
            });
        }
    }

    public boolean isShowing() {
        if (this.mKeyguardService != null) {
            this.mKeyguardState.showing = this.mKeyguardService.isShowing();
        }
        return this.mKeyguardState.showing;
    }

    public boolean isTrusted() {
        if (this.mKeyguardService != null) {
            return this.mKeyguardService.isTrusted();
        }
        return false;
    }

    public boolean hasLockscreenWallpaper() {
        if (this.mKeyguardService != null) {
            return this.mKeyguardService.hasLockscreenWallpaper();
        }
        return false;
    }

    public boolean hasKeyguard() {
        return this.mKeyguardState.deviceHasKeyguard;
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

    public void setOccluded(boolean z, boolean z2) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setOccluded(z, z2);
        }
        this.mKeyguardState.occluded = z;
    }

    public void dismiss(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.dismiss(iKeyguardDismissCallback, charSequence);
        }
    }

    public boolean isSecure(int i) {
        if (this.mKeyguardService != null) {
            this.mKeyguardState.secure = this.mKeyguardService.isSecure(i);
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
            this.mKeyguardService.onStartedWakingUp();
        }
        this.mKeyguardState.interactiveState = 1;
    }

    public void onFinishedWakingUp() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onFinishedWakingUp();
        }
        this.mKeyguardState.interactiveState = 2;
    }

    public void onScreenTurningOff() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onScreenTurningOff();
        }
        this.mKeyguardState.screenState = 3;
    }

    public void onScreenTurnedOff() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onScreenTurnedOff();
        }
        this.mKeyguardState.screenState = 0;
    }

    public void onScreenTurningOn(DrawnListener drawnListener) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onScreenTurningOn(new KeyguardShowDelegate(drawnListener));
        } else {
            Slog.w(TAG, "onScreenTurningOn(): no keyguard service!");
            this.mDrawnListenerWhenConnect = drawnListener;
        }
        this.mKeyguardState.screenState = 1;
    }

    public void onScreenTurnedOn() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onScreenTurnedOn();
        }
        this.mKeyguardState.screenState = 2;
    }

    public void onStartedGoingToSleep(int i) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onStartedGoingToSleep(i);
        }
        this.mKeyguardState.offReason = i;
        this.mKeyguardState.interactiveState = 3;
    }

    public void onFinishedGoingToSleep(int i, boolean z) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onFinishedGoingToSleep(i, z);
        }
        this.mKeyguardState.interactiveState = 0;
    }

    public void setKeyguardEnabled(boolean z) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setKeyguardEnabled(z);
        }
        this.mKeyguardState.enabled = z;
    }

    public void onSystemReady() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onSystemReady();
        } else {
            this.mKeyguardState.systemIsReady = true;
        }
    }

    public void doKeyguardTimeout(Bundle bundle) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.doKeyguardTimeout(bundle);
        }
    }

    public void setCurrentUser(int i) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setCurrentUser(i);
        }
        this.mKeyguardState.currentUser = i;
    }

    public void setSwitchingUser(boolean z) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.setSwitchingUser(z);
        }
    }

    public void startKeyguardExitAnimation(long j, long j2) {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.startKeyguardExitAnimation(j, j2);
        }
    }

    public void onBootCompleted() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onBootCompleted();
        }
        this.mKeyguardState.bootCompleted = true;
    }

    public void onShortPowerPressedGoHome() {
        if (this.mKeyguardService != null) {
            this.mKeyguardService.onShortPowerPressedGoHome();
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1133871366145L, this.mKeyguardState.showing);
        protoOutputStream.write(1133871366146L, this.mKeyguardState.occluded);
        protoOutputStream.write(1133871366147L, this.mKeyguardState.secure);
        protoOutputStream.write(1159641169924L, this.mKeyguardState.screenState);
        protoOutputStream.write(1159641169925L, this.mKeyguardState.interactiveState);
        protoOutputStream.end(jStart);
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.println(str + TAG);
        String str2 = str + "  ";
        printWriter.println(str2 + "showing=" + this.mKeyguardState.showing);
        printWriter.println(str2 + "showingAndNotOccluded=" + this.mKeyguardState.showingAndNotOccluded);
        printWriter.println(str2 + "inputRestricted=" + this.mKeyguardState.inputRestricted);
        printWriter.println(str2 + "occluded=" + this.mKeyguardState.occluded);
        printWriter.println(str2 + "secure=" + this.mKeyguardState.secure);
        printWriter.println(str2 + "dreaming=" + this.mKeyguardState.dreaming);
        printWriter.println(str2 + "systemIsReady=" + this.mKeyguardState.systemIsReady);
        printWriter.println(str2 + "deviceHasKeyguard=" + this.mKeyguardState.deviceHasKeyguard);
        printWriter.println(str2 + "enabled=" + this.mKeyguardState.enabled);
        printWriter.println(str2 + "offReason=" + WindowManagerPolicyConstants.offReasonToString(this.mKeyguardState.offReason));
        printWriter.println(str2 + "currentUser=" + this.mKeyguardState.currentUser);
        printWriter.println(str2 + "bootCompleted=" + this.mKeyguardState.bootCompleted);
        printWriter.println(str2 + "screenState=" + screenStateToString(this.mKeyguardState.screenState));
        printWriter.println(str2 + "interactiveState=" + interactiveStateToString(this.mKeyguardState.interactiveState));
        if (this.mKeyguardService != null) {
            this.mKeyguardService.dump(str2, printWriter);
        }
    }

    private static String screenStateToString(int i) {
        switch (i) {
            case 0:
                return "SCREEN_STATE_OFF";
            case 1:
                return "SCREEN_STATE_TURNING_ON";
            case 2:
                return "SCREEN_STATE_ON";
            case 3:
                return "SCREEN_STATE_TURNING_OFF";
            default:
                return Integer.toString(i);
        }
    }

    private static String interactiveStateToString(int i) {
        switch (i) {
            case 0:
                return "INTERACTIVE_STATE_SLEEP";
            case 1:
                return "INTERACTIVE_STATE_WAKING";
            case 2:
                return "INTERACTIVE_STATE_AWAKE";
            case 3:
                return "INTERACTIVE_STATE_GOING_TO_SLEEP";
            default:
                return Integer.toString(i);
        }
    }
}
