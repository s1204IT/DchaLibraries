package com.android.server.wm;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.TokenWatcher;
import android.util.Log;
import android.util.Pair;
import com.android.server.policy.WindowManagerPolicy;

public class KeyguardDisableHandler extends Handler {
    private static final int ALLOW_DISABLE_NO = 0;
    private static final int ALLOW_DISABLE_UNKNOWN = -1;
    private static final int ALLOW_DISABLE_YES = 1;
    static final int KEYGUARD_DISABLE = 1;
    static final int KEYGUARD_POLICY_CHANGED = 3;
    static final int KEYGUARD_REENABLE = 2;
    private static final String TAG = "WindowManager";
    private int mAllowDisableKeyguard = -1;
    final Context mContext;
    KeyguardTokenWatcher mKeyguardTokenWatcher;
    final WindowManagerPolicy mPolicy;

    public KeyguardDisableHandler(Context context, WindowManagerPolicy windowManagerPolicy) {
        this.mContext = context;
        this.mPolicy = windowManagerPolicy;
    }

    @Override
    public void handleMessage(Message message) {
        if (this.mKeyguardTokenWatcher == null) {
            this.mKeyguardTokenWatcher = new KeyguardTokenWatcher(this);
        }
        switch (message.what) {
            case 1:
                Pair pair = (Pair) message.obj;
                this.mKeyguardTokenWatcher.acquire((IBinder) pair.first, (String) pair.second);
                break;
            case 2:
                this.mKeyguardTokenWatcher.release((IBinder) message.obj);
                break;
            case 3:
                this.mAllowDisableKeyguard = -1;
                if (this.mKeyguardTokenWatcher.isAcquired()) {
                    this.mKeyguardTokenWatcher.updateAllowState();
                    if (this.mAllowDisableKeyguard != 1) {
                        this.mPolicy.enableKeyguard(true);
                    }
                } else {
                    this.mPolicy.enableKeyguard(true);
                }
                break;
        }
    }

    class KeyguardTokenWatcher extends TokenWatcher {
        public KeyguardTokenWatcher(Handler handler) {
            super(handler, "WindowManager");
        }

        public void updateAllowState() {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) KeyguardDisableHandler.this.mContext.getSystemService("device_policy");
            if (devicePolicyManager != null) {
                try {
                    KeyguardDisableHandler.this.mAllowDisableKeyguard = devicePolicyManager.getPasswordQuality(null, ActivityManager.getService().getCurrentUser().id) == 0 ? 1 : 0;
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void acquired() {
            if (KeyguardDisableHandler.this.mAllowDisableKeyguard == -1) {
                updateAllowState();
            }
            if (KeyguardDisableHandler.this.mAllowDisableKeyguard == 1) {
                KeyguardDisableHandler.this.mPolicy.enableKeyguard(false);
            } else {
                Log.v("WindowManager", "Not disabling keyguard since device policy is enforced");
            }
        }

        @Override
        public void released() {
            KeyguardDisableHandler.this.mPolicy.enableKeyguard(true);
        }
    }
}
