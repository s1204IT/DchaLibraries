package com.android.keyguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import com.android.internal.policy.IFaceLockCallback;
import com.android.internal.policy.IFaceLockInterface;
import com.android.internal.widget.LockPatternUtils;

public class FaceUnlock implements Handler.Callback, BiometricSensorUnlock {
    private final Context mContext;
    private View mFaceUnlockView;
    KeyguardSecurityCallback mKeyguardScreenCallback;
    private final LockPatternUtils mLockPatternUtils;
    private IFaceLockInterface mService;
    private boolean mServiceRunning = false;
    private final Object mServiceRunningLock = new Object();
    private boolean mBoundToService = false;
    private final int MSG_SERVICE_CONNECTED = 0;
    private final int MSG_SERVICE_DISCONNECTED = 1;
    private final int MSG_UNLOCK = 2;
    private final int MSG_CANCEL = 3;
    private final int MSG_REPORT_FAILED_ATTEMPT = 4;
    private final int MSG_POKE_WAKELOCK = 5;
    private volatile boolean mIsRunning = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder iservice) {
            Log.d("FULLockscreen", "Connected to Face Unlock service");
            FaceUnlock.this.mService = IFaceLockInterface.Stub.asInterface(iservice);
            FaceUnlock.this.mHandler.sendEmptyMessage(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.e("FULLockscreen", "Unexpected disconnect from Face Unlock service");
            FaceUnlock.this.mHandler.sendEmptyMessage(1);
        }
    };
    private final IFaceLockCallback mFaceUnlockCallback = new IFaceLockCallback.Stub() {
        public void unlock() {
            Message message = FaceUnlock.this.mHandler.obtainMessage(2, UserHandle.getCallingUserId(), -1);
            FaceUnlock.this.mHandler.sendMessage(message);
        }

        public void cancel() {
            FaceUnlock.this.mHandler.sendEmptyMessage(3);
        }

        public void reportFailedAttempt() {
            FaceUnlock.this.mHandler.sendEmptyMessage(4);
        }

        public void pokeWakelock(int millis) {
            Message message = FaceUnlock.this.mHandler.obtainMessage(5, millis, -1);
            FaceUnlock.this.mHandler.sendMessage(message);
        }
    };
    private Handler mHandler = new Handler(this);

    public FaceUnlock(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    public void setKeyguardCallback(KeyguardSecurityCallback keyguardScreenCallback) {
        this.mKeyguardScreenCallback = keyguardScreenCallback;
    }

    @Override
    public void initializeView(View biometricUnlockView) {
        Log.d("FULLockscreen", "initializeView()");
        this.mFaceUnlockView = biometricUnlockView;
    }

    @Override
    public void stopAndShowBackup() {
        this.mHandler.sendEmptyMessage(3);
    }

    @Override
    public boolean start() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            Log.e("FULLockscreen", "start() called off of the UI thread");
        }
        if (this.mIsRunning) {
            Log.w("FULLockscreen", "start() called when already running");
        }
        if (!this.mBoundToService) {
            Log.d("FULLockscreen", "Binding to Face Unlock service for user=" + this.mLockPatternUtils.getCurrentUser());
            this.mContext.bindServiceAsUser(new Intent(IFaceLockInterface.class.getName()).setPackage("com.android.facelock"), this.mConnection, 1, new UserHandle(this.mLockPatternUtils.getCurrentUser()));
            this.mBoundToService = true;
        } else {
            Log.w("FULLockscreen", "Attempt to bind to Face Unlock when already bound");
        }
        this.mIsRunning = true;
        return true;
    }

    @Override
    public boolean stop() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            Log.e("FULLockscreen", "stop() called from non-UI thread");
        }
        this.mHandler.removeMessages(0);
        boolean mWasRunning = this.mIsRunning;
        stopUi();
        if (this.mBoundToService) {
            if (this.mService != null) {
                try {
                    this.mService.unregisterCallback(this.mFaceUnlockCallback);
                } catch (RemoteException e) {
                }
            }
            Log.d("FULLockscreen", "Unbinding from Face Unlock service");
            this.mContext.unbindService(this.mConnection);
            this.mBoundToService = false;
        }
        this.mIsRunning = false;
        return mWasRunning;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                handleServiceConnected();
                return true;
            case 1:
                handleServiceDisconnected();
                return true;
            case 2:
                handleUnlock(msg.arg1);
                return true;
            case 3:
                handleCancel();
                return true;
            case 4:
                handleReportFailedAttempt();
                return true;
            case 5:
                handlePokeWakelock(msg.arg1);
                return true;
            default:
                Log.e("FULLockscreen", "Unhandled message");
                return false;
        }
    }

    void handleServiceConnected() {
        Log.d("FULLockscreen", "handleServiceConnected()");
        if (!this.mBoundToService) {
            Log.d("FULLockscreen", "Dropping startUi() in handleServiceConnected() because no longer bound");
            return;
        }
        try {
            this.mService.registerCallback(this.mFaceUnlockCallback);
            if (this.mFaceUnlockView != null) {
                IBinder windowToken = this.mFaceUnlockView.getWindowToken();
                if (windowToken != null) {
                    this.mKeyguardScreenCallback.userActivity();
                    int[] position = new int[2];
                    this.mFaceUnlockView.getLocationInWindow(position);
                    startUi(windowToken, position[0], position[1], this.mFaceUnlockView.getWidth(), this.mFaceUnlockView.getHeight());
                    return;
                }
                Log.e("FULLockscreen", "windowToken is null in handleServiceConnected()");
            }
        } catch (RemoteException e) {
            Log.e("FULLockscreen", "Caught exception connecting to Face Unlock: " + e.toString());
            this.mService = null;
            this.mBoundToService = false;
            this.mIsRunning = false;
        }
    }

    void handleServiceDisconnected() {
        Log.e("FULLockscreen", "handleServiceDisconnected()");
        synchronized (this.mServiceRunningLock) {
            this.mService = null;
            this.mServiceRunning = false;
        }
        this.mBoundToService = false;
        this.mIsRunning = false;
    }

    void handleUnlock(int authenticatedUserId) {
        stop();
        int currentUserId = this.mLockPatternUtils.getCurrentUser();
        if (authenticatedUserId == currentUserId) {
            this.mKeyguardScreenCallback.reportUnlockAttempt(true);
            this.mKeyguardScreenCallback.dismiss(true);
        } else {
            Log.d("FULLockscreen", "Ignoring unlock for authenticated user (" + authenticatedUserId + ") because the current user is " + currentUserId);
        }
    }

    void handleCancel() {
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(false);
        this.mKeyguardScreenCallback.showBackupSecurity();
        stop();
        this.mKeyguardScreenCallback.userActivity();
    }

    void handleReportFailedAttempt() {
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(false);
        this.mKeyguardScreenCallback.reportUnlockAttempt(false);
    }

    void handlePokeWakelock(int millis) {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager.isScreenOn()) {
            this.mKeyguardScreenCallback.userActivity();
        }
    }

    private void startUi(IBinder windowToken, int x, int y, int w, int h) {
        synchronized (this.mServiceRunningLock) {
            if (!this.mServiceRunning) {
                Log.d("FULLockscreen", "Starting Face Unlock");
                try {
                    this.mService.startUi(windowToken, x, y, w, h, this.mLockPatternUtils.isBiometricWeakLivelinessEnabled());
                    this.mServiceRunning = true;
                } catch (RemoteException e) {
                    Log.e("FULLockscreen", "Caught exception starting Face Unlock: " + e.toString());
                }
            } else {
                Log.w("FULLockscreen", "startUi() attempted while running");
            }
        }
    }

    private void stopUi() {
        synchronized (this.mServiceRunningLock) {
            if (this.mServiceRunning) {
                Log.d("FULLockscreen", "Stopping Face Unlock");
                try {
                    this.mService.stopUi();
                } catch (RemoteException e) {
                    Log.e("FULLockscreen", "Caught exception stopping Face Unlock: " + e.toString());
                }
                this.mServiceRunning = false;
            }
        }
    }
}
