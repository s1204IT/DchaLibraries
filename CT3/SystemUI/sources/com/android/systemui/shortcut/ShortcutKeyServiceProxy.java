package com.android.systemui.shortcut;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import com.android.internal.policy.IShortcutService;

public class ShortcutKeyServiceProxy extends IShortcutService.Stub {
    private Callbacks mCallbacks;
    private final Object mLock = new Object();
    private final Handler mHandler = new H(this, null);

    public interface Callbacks {
        void onShortcutKeyPressed(long j);
    }

    public ShortcutKeyServiceProxy(Callbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    public void notifyShortcutKeyPressed(long shortcutCode) throws RemoteException {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1, Long.valueOf(shortcutCode)).sendToTarget();
        }
    }

    private final class H extends Handler {
        H(ShortcutKeyServiceProxy this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 1:
                    ShortcutKeyServiceProxy.this.mCallbacks.onShortcutKeyPressed(((Long) msg.obj).longValue());
                    break;
            }
        }
    }
}
