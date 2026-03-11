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
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityCallback;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.R$id;
import com.android.keyguard.R$layout;
import com.android.keyguard.R$string;
import com.android.keyguard.ViewMediatorCallback;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.common.ppl.IPplManager;
import com.mediatek.internal.telephony.ppl.IPplAgent;

public class AntiTheftManager {

    private static final int[] f9x1cbe7e58 = null;
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
    private static boolean mAntiTheftAutoTestNotShowUI = false;
    private final int MSG_ARG_LOCK = 0;
    private final int MSG_ARG_UNLOCK = 1;
    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("AntiTheftManager", "handleAntiTheftViewUpdate() - action = " + action);
            if ("com.mediatek.dm.LAWMO_LOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive OMADM_LAWMO_LOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(1, 0);
                return;
            }
            if ("com.mediatek.dm.LAWMO_UNLOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive OMADM_LAWMO_UNLOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(1, 1);
                return;
            }
            if ("com.mediatek.ppl.NOTIFY_LOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive PPL_LOCK");
                if (KeyguardUtils.isSystemEncrypted()) {
                    Log.d("AntiTheftManager", "Currently system needs to be decrypted. Not show PPL.");
                    return;
                } else {
                    AntiTheftManager.this.sendAntiTheftUpdateMsg(2, 0);
                    return;
                }
            }
            if ("com.mediatek.ppl.NOTIFY_UNLOCK".equals(action)) {
                Log.d("AntiTheftManager", "receive PPL_UNLOCK");
                AntiTheftManager.this.sendAntiTheftUpdateMsg(2, 1);
            } else {
                if (!"android.intent.action.ACTION_PREBOOT_IPO".equals(action)) {
                    return;
                }
                AntiTheftManager.this.doBindAntiThftLockServices();
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    AntiTheftManager.this.handleAntiTheftViewUpdate(msg.arg1, msg.arg2 == 0);
                    break;
            }
        }
    };
    protected ServiceConnection mPplServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("AntiTheftManager", "onServiceConnected() -- PPL");
            IPplManager unused = AntiTheftManager.mIPplManager = IPplManager.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("AntiTheftManager", "onServiceDisconnected()");
            IPplManager unused = AntiTheftManager.mIPplManager = null;
        }
    };

    private static int[] m1979xec5d63fc() {
        if (f9x1cbe7e58 != null) {
            return f9x1cbe7e58;
        }
        int[] iArr = new int[KeyguardSecurityModel.SecurityMode.valuesCustom().length];
        try {
            iArr[KeyguardSecurityModel.SecurityMode.AlarmBoot.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.AntiTheft.ordinal()] = 6;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Biometric.ordinal()] = 7;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Invalid.ordinal()] = 8;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.None.ordinal()] = 9;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.PIN.ordinal()] = 10;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Password.ordinal()] = 11;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Pattern.ordinal()] = 12;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe1.ordinal()] = 2;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe2.ordinal()] = 3;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe3.ordinal()] = 4;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.SimPinPukMe4.ordinal()] = 5;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[KeyguardSecurityModel.SecurityMode.Voice.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        f9x1cbe7e58 = iArr;
        return iArr;
    }

    public AntiTheftManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        Log.d("AntiTheftManager", "AntiTheftManager() is called.");
        mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityModel = new KeyguardSecurityModel(mContext);
        IntentFilter filter = new IntentFilter();
        setKeypadNeeded(1, false);
        setDismissable(1, false);
        filter.addAction("com.mediatek.dm.LAWMO_LOCK");
        filter.addAction("com.mediatek.dm.LAWMO_UNLOCK");
        if (KeyguardUtils.isPrivacyProtectionLockSupport()) {
            Log.d("AntiTheftManager", "MTK_PRIVACY_PROTECTION_LOCK is enabled.");
            setKeypadNeeded(2, true);
            setDismissable(2, true);
            filter.addAction("com.mediatek.ppl.NOTIFY_LOCK");
            filter.addAction("com.mediatek.ppl.NOTIFY_UNLOCK");
        }
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    public static AntiTheftManager getInstance(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        Log.d("AntiTheftManager", "getInstance(...) is called.");
        if (sInstance == null) {
            Log.d("AntiTheftManager", "getInstance(...) create one.");
            sInstance = new AntiTheftManager(context, viewMediatorCallback, lockPatternUtils);
        }
        return sInstance;
    }

    public static String getAntiTheftModeName(int mode) {
        switch (mode) {
        }
        return "AntiTheftMode.None";
    }

    public static int getCurrentAntiTheftMode() {
        Log.d("AntiTheftManager", "getCurrentAntiTheftMode() is called.");
        if (!isAntiTheftLocked()) {
            return 0;
        }
        for (int shift = 0; shift < 32; shift++) {
            int mode = mAntiTheftLockEnabled & (1 << shift);
            if (mode != 0) {
                return mode;
            }
        }
        return 0;
    }

    public static boolean isKeypadNeeded() {
        int mode = getCurrentAntiTheftMode();
        Log.d("AntiTheftManager", "getCurrentAntiTheftMode() = " + getAntiTheftModeName(mode));
        boolean needKeypad = (mKeypadNeeded & mode) != 0;
        Log.d("AntiTheftManager", "isKeypadNeeded() = " + needKeypad);
        return needKeypad;
    }

    public static void setKeypadNeeded(int lockMode, boolean need) {
        if (need) {
            mKeypadNeeded |= lockMode;
        } else {
            mKeypadNeeded &= ~lockMode;
        }
    }

    public static boolean isAntiTheftLocked() {
        return mAntiTheftLockEnabled != 0;
    }

    private static boolean isNeedUpdate(int lockMode, boolean enable) {
        if (enable && (mAntiTheftLockEnabled & lockMode) != 0) {
            Log.d("AntiTheftManager", "isNeedUpdate() - lockMode( " + lockMode + " ) is already enabled, no need update");
            return false;
        }
        if (enable || (mAntiTheftLockEnabled & lockMode) != 0) {
            return true;
        }
        Log.d("AntiTheftManager", "isNeedUpdate() - lockMode( " + lockMode + " ) is already disabled, no need update");
        return false;
    }

    private void setAntiTheftLocked(int lockMode, boolean enable) {
        if (enable) {
            mAntiTheftLockEnabled |= lockMode;
        } else {
            mAntiTheftLockEnabled &= ~lockMode;
        }
        this.mViewMediatorCallback.updateAntiTheftLocked();
    }

    public static boolean isDismissable() {
        int mode = getCurrentAntiTheftMode();
        if (mode != 0 && (mDismissable & mode) == 0) {
            return false;
        }
        return true;
    }

    public static void setDismissable(int lockMode, boolean canBeDismissed) {
        Log.d("AntiTheftManager", "mDismissable is " + mDismissable + " before");
        if (canBeDismissed) {
            mDismissable |= lockMode;
        } else {
            mDismissable &= ~lockMode;
        }
        Log.d("AntiTheftManager", "mDismissable is " + mDismissable + " after");
    }

    public static boolean isAntiTheftPriorToSecMode(KeyguardSecurityModel.SecurityMode mode) {
        int currentAntiTheftType = getCurrentAntiTheftMode();
        if (!isAntiTheftLocked()) {
            return false;
        }
        if (currentAntiTheftType != 1) {
            switch (m1979xec5d63fc()[mode.ordinal()]) {
            }
            return false;
        }
        return true;
    }

    public static int getAntiTheftViewId() {
        return R$id.keyguard_antitheft_lock_view;
    }

    public static int getAntiTheftLayoutId() {
        return R$layout.mtk_keyguard_anti_theft_lock_view;
    }

    public static int getPrompt() {
        int mode = getCurrentAntiTheftMode();
        if (mode == 1) {
            return R$string.dm_prompt;
        }
        return R$string.ppl_prompt;
    }

    public static String getAntiTheftMessageAreaText(CharSequence text, CharSequence seperator) {
        StringBuilder b = new StringBuilder();
        if (text != null && text.length() > 0 && !text.toString().equals("AntiTheft Noneed Print Text")) {
            b.append(text);
            b.append(seperator);
        }
        b.append(mContext.getText(getPrompt()));
        return b.toString();
    }

    public boolean checkPassword(String pw) {
        boolean unlockSuccess = false;
        int mode = getCurrentAntiTheftMode();
        Log.d("AntiTheftManager", "checkPassword, mode is " + getAntiTheftModeName(mode));
        switch (mode) {
            case 2:
                unlockSuccess = doPplCheckPassword(pw);
                break;
        }
        Log.d("AntiTheftManager", "checkPassword, unlockSuccess is " + unlockSuccess);
        return unlockSuccess;
    }

    public void sendAntiTheftUpdateMsg(int antiTheftLockType, int lock) {
        Message msg = this.mHandler.obtainMessage(1001);
        msg.arg1 = antiTheftLockType;
        msg.arg2 = lock;
        msg.sendToTarget();
    }

    public void handleAntiTheftViewUpdate(int antiTheftLockType, boolean lock) {
        if (isNeedUpdate(antiTheftLockType, lock)) {
            setAntiTheftLocked(antiTheftLockType, lock);
            if (lock) {
                Log.d("AntiTheftManager", "handleAntiTheftViewUpdate() - locked, !isShowing = " + (this.mViewMediatorCallback.isShowing() ? false : true) + " isKeyguardDoneOnGoing = " + this.mViewMediatorCallback.isKeyguardDoneOnGoing());
                if (!this.mViewMediatorCallback.isShowing() || this.mViewMediatorCallback.isKeyguardDoneOnGoing()) {
                    this.mViewMediatorCallback.showLocked(null);
                } else {
                    boolean needToRest = isAntiTheftPriorToSecMode(this.mSecurityModel.getSecurityMode());
                    if (needToRest) {
                        Log.d("AntiTheftManager", "handleAntiTheftViewUpdate() - call resetStateLocked().");
                        this.mViewMediatorCallback.resetStateLocked();
                    } else {
                        Log.d("AntiTheftManager", "No need to reset the security view to show AntiTheft,since current view should show above antitheft view.");
                    }
                }
            } else if (this.mKeyguardSecurityCallback != null) {
                this.mKeyguardSecurityCallback.dismiss(true);
            } else {
                Log.d("AntiTheftManager", "mKeyguardSecurityCallback is null !");
            }
            adjustStatusBarLocked();
        }
    }

    public void doBindAntiThftLockServices() {
        Log.d("AntiTheftManager", "doBindAntiThftLockServices() is called.");
        if (!KeyguardUtils.isPrivacyProtectionLockSupport()) {
            return;
        }
        bindPplService();
    }

    public void doAntiTheftLockCheck() {
        String status = SystemProperties.get("ro.crypto.state", "unsupported");
        if (!"unencrypted".equalsIgnoreCase(status)) {
            return;
        }
        doPplLockCheck();
        doDmLockCheck();
    }

    private void doDmLockCheck() {
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            if (binder != null) {
                DmAgent agent = DmAgent.Stub.asInterface(binder);
                boolean flag = agent.isLockFlagSet();
                Log.i("AntiTheftManager", "dmCheckLocked, the lock flag is:" + flag);
                setAntiTheftLocked(1, flag);
            } else {
                Log.i("AntiTheftManager", "dmCheckLocked, DmAgent doesn't exit");
            }
        } catch (RemoteException e) {
            Log.e("AntiTheftManager", "doDmLockCheck() - error in get DMAgent service.");
        }
    }

    private void doPplLockCheck() {
        if (mAntiTheftLockEnabled != 2) {
            return;
        }
        setAntiTheftLocked(2, true);
    }

    public static void checkPplStatus() {
        boolean isUnEncrypted = !KeyguardUtils.isSystemEncrypted();
        try {
            IBinder binder = ServiceManager.getService("PPLAgent");
            if (binder != null) {
                IPplAgent agent = IPplAgent.Stub.asInterface(binder);
                boolean flag = agent.needLock() == 1;
                Log.i("AntiTheftManager", "PplCheckLocked, the lock flag is:" + (flag ? isUnEncrypted : false));
                if (!flag || !isUnEncrypted) {
                    return;
                }
                mAntiTheftLockEnabled |= 2;
                return;
            }
            Log.i("AntiTheftManager", "PplCheckLocked, PPLAgent doesn't exit");
        } catch (RemoteException e) {
            Log.e("AntiTheftManager", "doPplLockCheck() - error in get PPLAgent service.");
        }
    }

    private void bindPplService() {
        Log.e("AntiTheftManager", "binPplService() is called.");
        if (mIPplManager == null) {
            try {
                Intent intent = new Intent("com.mediatek.ppl.service");
                intent.setClassName("com.mediatek.ppl", "com.mediatek.ppl.PplService");
                mContext.bindService(intent, this.mPplServiceConnection, 1);
                return;
            } catch (SecurityException e) {
                Log.e("AntiTheftManager", "bindPplService() - error in bind ppl service.");
                return;
            }
        }
        Log.d("AntiTheftManager", "bindPplService() -- the ppl service is already bound.");
    }

    private boolean doPplCheckPassword(String pw) {
        boolean unlockSuccess = false;
        if (mIPplManager != null) {
            try {
                unlockSuccess = mIPplManager.unlock(pw);
                Log.i("AntiTheftManager", "doPplCheckPassword, unlockSuccess is " + unlockSuccess);
                if (unlockSuccess) {
                    setAntiTheftLocked(2, false);
                }
            } catch (RemoteException e) {
            }
        } else {
            Log.i("AntiTheftManager", "doPplCheckPassword() mIPplManager == null !!??");
        }
        return unlockSuccess;
    }

    public void adjustStatusBarLocked() {
        this.mViewMediatorCallback.adjustStatusBarLocked();
    }

    public void setSecurityViewCallback(KeyguardSecurityCallback callback) {
        Log.d("AntiTheftManager", "setSecurityViewCallback(" + callback + ")");
        this.mKeyguardSecurityCallback = callback;
    }

    public IPplManager getPPLManagerInstance() {
        return mIPplManager;
    }

    public BroadcastReceiver getPPLBroadcastReceiverInstance() {
        return this.mBroadcastReceiver;
    }

    public Handler getHandlerInstance() {
        return this.mHandler;
    }
}
