package com.mediatek.keyguard.AntiTheft;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.R;
import com.mediatek.common.ppl.IPplManager;
import vendor.mediatek.hardware.pplagent.V1_0.IPplAgent;

public class AntiTheftManager {
    public static final String ANTITHEFT_NONEED_PRINT_TEXT = "AntiTheft Noneed Print Text";
    private static final int MSG_ANTITHEFT_KEYGUARD_UPDATE = 1001;
    public static final String PPL_LOCK = "com.mediatek.ppl.NOTIFY_LOCK";
    public static final String PPL_UNLOCK = "com.mediatek.ppl.NOTIFY_UNLOCK";
    public static final String RESET_FOR_ANTITHEFT_LOCK = "antitheftlock_reset";
    private static final String TAG = "AntiTheftManager";
    private static Context mContext;
    private static IPplManager mIPplManager;
    private static AntiTheftManager sInstance;
    protected KeyguardSecurityCallback mKeyguardSecurityCallback;
    private LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityModel mSecurityModel;
    private ViewMediatorCallback mViewMediatorCallback;
    private static int mAntiTheftLockEnabled = 0;
    private static int mKeypadNeeded = 0;
    private static int mDismissable = 0;
    private static final boolean DEBUG = false;
    private static boolean mAntiTheftAutoTestNotShowUI = DEBUG;
    private final int MSG_ARG_LOCK = 0;
    private final int MSG_ARG_UNLOCK = 1;

    @VisibleForTesting
    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(AntiTheftManager.TAG, "handleAntiTheftViewUpdate() - action = " + action);
            if (AntiTheftManager.PPL_LOCK.equals(action)) {
                Log.d(AntiTheftManager.TAG, "receive PPL_LOCK");
                if (!KeyguardUtils.isSystemEncrypted()) {
                    AntiTheftManager.this.sendAntiTheftUpdateMsg(2, 0);
                    return;
                } else {
                    Log.d(AntiTheftManager.TAG, "Currently system needs to be decrypted. Not show PPL.");
                    return;
                }
            }
            if (AntiTheftManager.PPL_UNLOCK.equals(action)) {
                Log.d(AntiTheftManager.TAG, "receive PPL_UNLOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(2, 1);
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == AntiTheftManager.MSG_ANTITHEFT_KEYGUARD_UPDATE) {
                AntiTheftManager.this.handleAntiTheftViewUpdate(message.arg1, message.arg2 == 0 ? true : AntiTheftManager.DEBUG);
            }
        }
    };

    @VisibleForTesting
    protected ServiceConnection mPplServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(AntiTheftManager.TAG, "onServiceConnected() -- PPL");
            IPplManager unused = AntiTheftManager.mIPplManager = IPplManager.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(AntiTheftManager.TAG, "onServiceDisconnected()");
            IPplManager unused = AntiTheftManager.mIPplManager = null;
        }
    };

    public AntiTheftManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        Log.d(TAG, "AntiTheftManager() is called.");
        mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityModel = new KeyguardSecurityModel(mContext);
        IntentFilter intentFilter = new IntentFilter();
        if (KeyguardUtils.isPrivacyProtectionLockSupport()) {
            Log.d(TAG, "MTK_PRIVACY_PROTECTION_LOCK is enabled.");
            setKeypadNeeded(2, true);
            setDismissable(2, true);
            intentFilter.addAction(PPL_LOCK);
            intentFilter.addAction(PPL_UNLOCK);
        }
        mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    public static AntiTheftManager getInstance(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        if (sInstance == null) {
            Log.d(TAG, "getInstance(...) create one.");
            sInstance = new AntiTheftManager(context, viewMediatorCallback, lockPatternUtils);
        }
        return sInstance;
    }

    public static String getAntiTheftModeName(int i) {
        if (i != 0 && i == 2) {
            return "AntiTheftMode.PplLock";
        }
        return "AntiTheftMode.None";
    }

    public static int getCurrentAntiTheftMode() {
        if (!isAntiTheftLocked()) {
            return 0;
        }
        for (int i = 0; i < 32; i++) {
            int i2 = mAntiTheftLockEnabled & (1 << i);
            if (i2 != 0) {
                return i2;
            }
        }
        return 0;
    }

    public static boolean isKeypadNeeded() {
        int currentAntiTheftMode = getCurrentAntiTheftMode();
        Log.d(TAG, "getCurrentAntiTheftMode() = " + getAntiTheftModeName(currentAntiTheftMode));
        boolean z = (currentAntiTheftMode & mKeypadNeeded) != 0 ? true : DEBUG;
        Log.d(TAG, "isKeypadNeeded() = " + z);
        return z;
    }

    public static void setKeypadNeeded(int i, boolean z) {
        if (z) {
            mKeypadNeeded = i | mKeypadNeeded;
        } else {
            mKeypadNeeded = (~i) & mKeypadNeeded;
        }
    }

    public static boolean isAntiTheftLocked() {
        if (mAntiTheftLockEnabled != 0) {
            return true;
        }
        return DEBUG;
    }

    private static boolean isNeedUpdate(int i, boolean z) {
        if (z && (mAntiTheftLockEnabled & i) != 0) {
            Log.d(TAG, "isNeedUpdate() - lockMode( " + i + " ) is already enabled, no need update");
            return DEBUG;
        }
        if (!z && (mAntiTheftLockEnabled & i) == 0) {
            Log.d(TAG, "isNeedUpdate() - lockMode( " + i + " ) is already disabled, no need update");
            return DEBUG;
        }
        return true;
    }

    private void setAntiTheftLocked(int i, boolean z) {
        if (z) {
            mAntiTheftLockEnabled = i | mAntiTheftLockEnabled;
        } else {
            mAntiTheftLockEnabled = (~i) & mAntiTheftLockEnabled;
        }
    }

    public static boolean isDismissable() {
        int currentAntiTheftMode = getCurrentAntiTheftMode();
        if (currentAntiTheftMode == 0 || (currentAntiTheftMode & mDismissable) != 0) {
            return true;
        }
        return DEBUG;
    }

    public static void setDismissable(int i, boolean z) {
        Log.d(TAG, "mDismissable is " + mDismissable + " before");
        if (z) {
            mDismissable = i | mDismissable;
        } else {
            mDismissable = (~i) & mDismissable;
        }
        Log.d(TAG, "mDismissable is " + mDismissable + " after");
    }

    public static boolean isAntiTheftPriorToSecMode(KeyguardSecurityModel.SecurityMode securityMode) {
        getCurrentAntiTheftMode();
        if (isAntiTheftLocked()) {
            switch (securityMode) {
                case SimPinPukMe1:
                case SimPinPukMe2:
                case SimPinPukMe3:
                case SimPinPukMe4:
                case AlarmBoot:
                    break;
                default:
                    return true;
            }
        }
        return DEBUG;
    }

    public static int getAntiTheftViewId() {
        return R.id.keyguard_antitheft_lock_view;
    }

    public static int getAntiTheftLayoutId() {
        return R.layout.mtk_keyguard_anti_theft_lock_view;
    }

    public static int getPrompt() {
        return R.string.ppl_prompt;
    }

    public static String getAntiTheftMessageAreaText(CharSequence charSequence, CharSequence charSequence2) {
        StringBuilder sb = new StringBuilder();
        if (charSequence != null && charSequence.length() > 0 && !charSequence.toString().equals(ANTITHEFT_NONEED_PRINT_TEXT)) {
            sb.append(charSequence);
            sb.append(charSequence2);
        }
        sb.append(mContext.getText(getPrompt()));
        return sb.toString();
    }

    public static boolean isAntiTheftAutoTestNotShowUI() {
        return mAntiTheftAutoTestNotShowUI;
    }

    public boolean checkPassword(String str) {
        boolean zDoPplCheckPassword;
        int currentAntiTheftMode = getCurrentAntiTheftMode();
        Log.d(TAG, "checkPassword, mode is " + getAntiTheftModeName(currentAntiTheftMode));
        if (currentAntiTheftMode == 2) {
            zDoPplCheckPassword = doPplCheckPassword(str);
        } else {
            zDoPplCheckPassword = DEBUG;
        }
        Log.d(TAG, "checkPassword, unlockSuccess is " + zDoPplCheckPassword);
        return zDoPplCheckPassword;
    }

    private void sendAntiTheftUpdateMsg(int i, int i2) {
        Message messageObtainMessage = this.mHandler.obtainMessage(MSG_ANTITHEFT_KEYGUARD_UPDATE);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.arg2 = i2;
        messageObtainMessage.sendToTarget();
    }

    private void handleAntiTheftViewUpdate(int i, boolean z) {
        if (isNeedUpdate(i, z)) {
            setAntiTheftLocked(i, z);
            if (z) {
                Log.d(TAG, "handleAntiTheftViewUpdate() - locked, !isShowing = " + (true ^ this.mViewMediatorCallback.isShowing()) + " isKeyguardDoneOnGoing = " + this.mViewMediatorCallback.isKeyguardDoneOnGoing());
                if (!this.mViewMediatorCallback.isShowing() || this.mViewMediatorCallback.isKeyguardDoneOnGoing()) {
                    this.mViewMediatorCallback.showLocked(null);
                } else if (isAntiTheftPriorToSecMode(this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser()))) {
                    Log.d(TAG, "handleAntiTheftViewUpdate() - call resetStateLocked().");
                    this.mViewMediatorCallback.resetStateLocked();
                } else {
                    Log.d(TAG, "No need to reset the security view to show AntiTheft,since current view should show above antitheft view.");
                }
            } else if (this.mKeyguardSecurityCallback != null) {
                this.mKeyguardSecurityCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
            } else {
                Log.d(TAG, "mKeyguardSecurityCallback is null !");
            }
            adjustStatusBarLocked();
        }
    }

    public void doBindAntiThftLockServices() {
        Log.d(TAG, "doBindAntiThftLockServices() is called.");
        if (KeyguardUtils.isPrivacyProtectionLockSupport()) {
            bindPplService();
        }
    }

    public void doAntiTheftLockCheck() {
        if ("unencrypted".equalsIgnoreCase(SystemProperties.get("ro.crypto.state", "unsupported"))) {
            doPplLockCheck();
        }
    }

    private void doPplLockCheck() {
        if (mAntiTheftLockEnabled == 2) {
            setAntiTheftLocked(2, true);
        }
    }

    public static void checkPplStatus() {
        boolean z = true;
        boolean z2 = !KeyguardUtils.isSystemEncrypted();
        try {
            IPplAgent service = IPplAgent.getService();
            if (service != null) {
                boolean z3 = service.needLock() == 1;
                StringBuilder sb = new StringBuilder();
                sb.append("PplCheckLocked, the lock flag is:");
                if (!z3 || !z2) {
                    z = false;
                }
                sb.append(z);
                Log.i(TAG, sb.toString());
                if (z3 && z2) {
                    mAntiTheftLockEnabled |= 2;
                }
                return;
            }
            Log.i(TAG, "PplCheckLocked, PPLAgent doesn't exit");
        } catch (Exception e) {
            Log.e(TAG, "doPplLockCheck() - error in get PPLAgent service.");
        }
    }

    private void bindPplService() {
        Log.e(TAG, "binPplService() is called.");
        if (mIPplManager == null) {
            try {
                Intent intent = new Intent("com.mediatek.ppl.service");
                intent.setClassName("com.mediatek.ppl", "com.mediatek.ppl.PplService");
                mContext.bindService(intent, this.mPplServiceConnection, 1);
                return;
            } catch (SecurityException e) {
                Log.e(TAG, "bindPplService() - error in bind ppl service.");
                return;
            }
        }
        Log.d(TAG, "bindPplService() -- the ppl service is already bound.");
    }

    private boolean doPplCheckPassword(String str) {
        if (mIPplManager != null) {
            try {
                boolean zUnlock = mIPplManager.unlock(str);
                try {
                    Log.i(TAG, "doPplCheckPassword, unlockSuccess is " + zUnlock);
                    if (zUnlock) {
                        setAntiTheftLocked(2, DEBUG);
                    }
                    return zUnlock;
                } catch (RemoteException e) {
                    return zUnlock;
                }
            } catch (RemoteException e2) {
                return DEBUG;
            }
        }
        Log.i(TAG, "doPplCheckPassword() mIPplManager == null !!??");
        return DEBUG;
    }

    public void adjustStatusBarLocked() {
        this.mViewMediatorCallback.adjustStatusBarLocked();
    }

    public void setSecurityViewCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        Log.d(TAG, "setSecurityViewCallback(" + keyguardSecurityCallback + ")");
        this.mKeyguardSecurityCallback = keyguardSecurityCallback;
    }

    @VisibleForTesting
    public IPplManager getPPLManagerInstance() {
        return mIPplManager;
    }

    @VisibleForTesting
    public BroadcastReceiver getPPLBroadcastReceiverInstance() {
        return this.mBroadcastReceiver;
    }

    @VisibleForTesting
    public Handler getHandlerInstance() {
        return this.mHandler;
    }
}
