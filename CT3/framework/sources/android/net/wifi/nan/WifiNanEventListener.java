package android.net.wifi.nan;

import android.net.wifi.nan.IWifiNanEventListener;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class WifiNanEventListener {
    private static final boolean DBG = false;
    public static final int LISTEN_CONFIG_COMPLETED = 1;
    public static final int LISTEN_CONFIG_FAILED = 2;
    public static final int LISTEN_IDENTITY_CHANGED = 8;
    public static final int LISTEN_NAN_DOWN = 4;
    private static final String TAG = "WifiNanEventListener";
    private static final boolean VDBG = false;
    public IWifiNanEventListener callback;
    private final Handler mHandler;

    public WifiNanEventListener() {
        this(Looper.myLooper());
    }

    public WifiNanEventListener(Looper looper) {
        this.callback = new IWifiNanEventListener.Stub() {
            @Override
            public void onConfigCompleted(ConfigRequest completedConfig) {
                Message msg = WifiNanEventListener.this.mHandler.obtainMessage(1);
                msg.obj = completedConfig;
                WifiNanEventListener.this.mHandler.sendMessage(msg);
            }

            @Override
            public void onConfigFailed(ConfigRequest failedConfig, int reason) {
                Message msg = WifiNanEventListener.this.mHandler.obtainMessage(2);
                msg.arg1 = reason;
                msg.obj = failedConfig;
                WifiNanEventListener.this.mHandler.sendMessage(msg);
            }

            @Override
            public void onNanDown(int reason) {
                Message msg = WifiNanEventListener.this.mHandler.obtainMessage(4);
                msg.arg1 = reason;
                WifiNanEventListener.this.mHandler.sendMessage(msg);
            }

            @Override
            public void onIdentityChanged() {
                Message msg = WifiNanEventListener.this.mHandler.obtainMessage(8);
                WifiNanEventListener.this.mHandler.sendMessage(msg);
            }
        };
        this.mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        WifiNanEventListener.this.onConfigCompleted((ConfigRequest) msg.obj);
                        break;
                    case 2:
                        WifiNanEventListener.this.onConfigFailed((ConfigRequest) msg.obj, msg.arg1);
                        break;
                    case 4:
                        WifiNanEventListener.this.onNanDown(msg.arg1);
                        break;
                    case 8:
                        WifiNanEventListener.this.onIdentityChanged();
                        break;
                }
            }
        };
    }

    public void onConfigCompleted(ConfigRequest completedConfig) {
        Log.w(TAG, "onConfigCompleted: called in stub - override if interested or disable");
    }

    public void onConfigFailed(ConfigRequest failedConfig, int reason) {
        Log.w(TAG, "onConfigFailed: called in stub - override if interested or disable");
    }

    public void onNanDown(int reason) {
        Log.w(TAG, "onNanDown: called in stub - override if interested or disable");
    }

    public void onIdentityChanged() {
    }
}
