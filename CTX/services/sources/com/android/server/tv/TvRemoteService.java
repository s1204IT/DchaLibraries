package com.android.server.tv;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.tv.TvRemoteProviderProxy;
import com.android.server.tv.TvRemoteProviderWatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class TvRemoteService extends SystemService implements Watchdog.Monitor {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_KEYS = false;
    private static final String TAG = "TvRemoteService";
    private Map<IBinder, UinputBridge> mBridgeMap;
    public final UserHandler mHandler;
    private final Object mLock;
    private ArrayList<TvRemoteProviderProxy> mProviderList;
    private Map<IBinder, TvRemoteProviderProxy> mProviderMap;

    public TvRemoteService(Context context) {
        super(context);
        this.mBridgeMap = new ArrayMap();
        this.mProviderMap = new ArrayMap();
        this.mProviderList = new ArrayList<>();
        this.mLock = new Object();
        this.mHandler = new UserHandler(new UserProvider(this), context);
        Watchdog.getInstance().addMonitor(this);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 600) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private void informInputBridgeConnected(IBinder iBinder) {
        this.mHandler.obtainMessage(2, 0, 0, iBinder).sendToTarget();
    }

    private void openInputBridgeInternalLocked(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, String str, int i, int i2, int i3) {
        try {
            if (this.mBridgeMap.containsKey(iBinder)) {
                informInputBridgeConnected(iBinder);
                return;
            }
            this.mBridgeMap.put(iBinder, new UinputBridge(iBinder, str, i, i2, i3));
            this.mProviderMap.put(iBinder, tvRemoteProviderProxy);
            informInputBridgeConnected(iBinder);
        } catch (IOException e) {
            Slog.e(TAG, "Cannot create device for " + str);
        }
    }

    private void closeInputBridgeInternalLocked(IBinder iBinder) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.close(iBinder);
        }
        this.mBridgeMap.remove(iBinder);
    }

    private void clearInputBridgeInternalLocked(IBinder iBinder) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.clear(iBinder);
        }
    }

    private void sendTimeStampInternalLocked(IBinder iBinder, long j) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.sendTimestamp(iBinder, j);
        }
    }

    private void sendKeyDownInternalLocked(IBinder iBinder, int i) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.sendKeyDown(iBinder, i);
        }
    }

    private void sendKeyUpInternalLocked(IBinder iBinder, int i) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.sendKeyUp(iBinder, i);
        }
    }

    private void sendPointerDownInternalLocked(IBinder iBinder, int i, int i2, int i3) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.sendPointerDown(iBinder, i, i2, i3);
        }
    }

    private void sendPointerUpInternalLocked(IBinder iBinder, int i) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.sendPointerUp(iBinder, i);
        }
    }

    private void sendPointerSyncInternalLocked(IBinder iBinder) {
        UinputBridge uinputBridge = this.mBridgeMap.get(iBinder);
        if (uinputBridge != null) {
            uinputBridge.sendPointerSync(iBinder);
        }
    }

    private final class UserHandler extends Handler {
        public static final int MSG_INPUT_BRIDGE_CONNECTED = 2;
        public static final int MSG_START = 1;
        private boolean mRunning;
        private final TvRemoteProviderWatcher mWatcher;

        public UserHandler(UserProvider userProvider, Context context) {
            super(Looper.getMainLooper(), null, true);
            this.mWatcher = new TvRemoteProviderWatcher(context, userProvider, this);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    start();
                    break;
                case 2:
                    IBinder iBinder = (IBinder) message.obj;
                    TvRemoteProviderProxy tvRemoteProviderProxy = (TvRemoteProviderProxy) TvRemoteService.this.mProviderMap.get(iBinder);
                    if (tvRemoteProviderProxy != null) {
                        tvRemoteProviderProxy.inputBridgeConnected(iBinder);
                    }
                    break;
            }
        }

        private void start() {
            if (!this.mRunning) {
                this.mRunning = true;
                this.mWatcher.start();
            }
        }
    }

    private final class UserProvider implements TvRemoteProviderWatcher.ProviderMethods, TvRemoteProviderProxy.ProviderMethods {
        private final TvRemoteService mService;

        public UserProvider(TvRemoteService tvRemoteService) {
            this.mService = tvRemoteService;
        }

        @Override
        public void openInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, String str, int i, int i2, int i3) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.openInputBridgeInternalLocked(tvRemoteProviderProxy, iBinder, str, i, i2, i3);
                }
            }
        }

        @Override
        public void closeInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.closeInputBridgeInternalLocked(iBinder);
                    TvRemoteService.this.mProviderMap.remove(iBinder);
                }
            }
        }

        @Override
        public void clearInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.clearInputBridgeInternalLocked(iBinder);
                }
            }
        }

        @Override
        public void sendTimeStamp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, long j) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.sendTimeStampInternalLocked(iBinder, j);
                }
            }
        }

        @Override
        public void sendKeyDown(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.sendKeyDownInternalLocked(iBinder, i);
                }
            }
        }

        @Override
        public void sendKeyUp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.sendKeyUpInternalLocked(iBinder, i);
                }
            }
        }

        @Override
        public void sendPointerDown(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i, int i2, int i3) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.sendPointerDownInternalLocked(iBinder, i, i2, i3);
                }
            }
        }

        @Override
        public void sendPointerUp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.sendPointerUpInternalLocked(iBinder, i);
                }
            }
        }

        @Override
        public void sendPointerSync(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder) {
            synchronized (TvRemoteService.this.mLock) {
                if (TvRemoteService.this.mProviderList.contains(tvRemoteProviderProxy)) {
                    this.mService.sendPointerSyncInternalLocked(iBinder);
                }
            }
        }

        @Override
        public void addProvider(TvRemoteProviderProxy tvRemoteProviderProxy) {
            synchronized (TvRemoteService.this.mLock) {
                tvRemoteProviderProxy.setProviderSink(this);
                TvRemoteService.this.mProviderList.add(tvRemoteProviderProxy);
                Slog.d(TvRemoteService.TAG, "provider: " + tvRemoteProviderProxy.toString());
            }
        }

        @Override
        public void removeProvider(TvRemoteProviderProxy tvRemoteProviderProxy) {
            synchronized (TvRemoteService.this.mLock) {
                if (!TvRemoteService.this.mProviderList.remove(tvRemoteProviderProxy)) {
                    Slog.e(TvRemoteService.TAG, "Unknown provider " + tvRemoteProviderProxy);
                }
            }
        }
    }
}
