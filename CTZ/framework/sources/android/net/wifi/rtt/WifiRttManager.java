package android.net.wifi.rtt;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.wifi.rtt.IRttCallback;
import android.os.Binder;
import android.os.RemoteException;
import android.os.WorkSource;
import java.util.List;
import java.util.concurrent.Executor;

public class WifiRttManager {
    public static final String ACTION_WIFI_RTT_STATE_CHANGED = "android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED";
    private static final String TAG = "WifiRttManager";
    private static final boolean VDBG = false;
    private final Context mContext;
    private final IWifiRttManager mService;

    public WifiRttManager(Context context, IWifiRttManager iWifiRttManager) {
        this.mContext = context;
        this.mService = iWifiRttManager;
    }

    public boolean isAvailable() {
        try {
            return this.mService.isAvailable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startRanging(RangingRequest rangingRequest, Executor executor, RangingResultCallback rangingResultCallback) {
        startRanging(null, rangingRequest, executor, rangingResultCallback);
    }

    @SystemApi
    public void startRanging(WorkSource workSource, RangingRequest rangingRequest, Executor executor, RangingResultCallback rangingResultCallback) {
        if (executor == null) {
            throw new IllegalArgumentException("Null executor provided");
        }
        if (rangingResultCallback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }
        try {
            this.mService.startRanging(new Binder(), this.mContext.getOpPackageName(), workSource, rangingRequest, new AnonymousClass1(executor, rangingResultCallback));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass1 extends IRttCallback.Stub {
        final RangingResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass1(Executor executor, RangingResultCallback rangingResultCallback) {
            this.val$executor = executor;
            this.val$callback = rangingResultCallback;
        }

        @Override
        public void onRangingFailure(final int i) throws RemoteException {
            clearCallingIdentity();
            Executor executor = this.val$executor;
            final RangingResultCallback rangingResultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    rangingResultCallback.onRangingFailure(i);
                }
            });
        }

        @Override
        public void onRangingResults(final List<RangingResult> list) throws RemoteException {
            clearCallingIdentity();
            Executor executor = this.val$executor;
            final RangingResultCallback rangingResultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    rangingResultCallback.onRangingResults(list);
                }
            });
        }
    }

    @SystemApi
    public void cancelRanging(WorkSource workSource) {
        try {
            this.mService.cancelRanging(workSource);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
