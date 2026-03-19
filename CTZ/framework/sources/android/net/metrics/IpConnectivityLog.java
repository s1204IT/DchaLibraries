package android.net.metrics;

import android.net.ConnectivityMetricsEvent;
import android.net.IIpConnectivityMetrics;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.BitUtils;

public class IpConnectivityLog {
    private static final boolean DBG = false;
    public static final String SERVICE_NAME = "connmetrics";
    private static final String TAG = IpConnectivityLog.class.getSimpleName();
    private IIpConnectivityMetrics mService;

    public IpConnectivityLog() {
    }

    @VisibleForTesting
    public IpConnectivityLog(IIpConnectivityMetrics iIpConnectivityMetrics) {
        this.mService = iIpConnectivityMetrics;
    }

    private boolean checkLoggerService() {
        if (this.mService != null) {
            return true;
        }
        IIpConnectivityMetrics iIpConnectivityMetricsAsInterface = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService(SERVICE_NAME));
        if (iIpConnectivityMetricsAsInterface == null) {
            return false;
        }
        this.mService = iIpConnectivityMetricsAsInterface;
        return true;
    }

    public boolean log(ConnectivityMetricsEvent connectivityMetricsEvent) {
        if (!checkLoggerService()) {
            return false;
        }
        if (connectivityMetricsEvent.timestamp == 0) {
            connectivityMetricsEvent.timestamp = System.currentTimeMillis();
        }
        try {
            return this.mService.logEvent(connectivityMetricsEvent) >= 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Error logging event", e);
            return false;
        }
    }

    public boolean log(long j, Parcelable parcelable) {
        ConnectivityMetricsEvent connectivityMetricsEventMakeEv = makeEv(parcelable);
        connectivityMetricsEventMakeEv.timestamp = j;
        return log(connectivityMetricsEventMakeEv);
    }

    public boolean log(String str, Parcelable parcelable) {
        ConnectivityMetricsEvent connectivityMetricsEventMakeEv = makeEv(parcelable);
        connectivityMetricsEventMakeEv.ifname = str;
        return log(connectivityMetricsEventMakeEv);
    }

    public boolean log(int i, int[] iArr, Parcelable parcelable) {
        ConnectivityMetricsEvent connectivityMetricsEventMakeEv = makeEv(parcelable);
        connectivityMetricsEventMakeEv.netId = i;
        connectivityMetricsEventMakeEv.transports = BitUtils.packBits(iArr);
        return log(connectivityMetricsEventMakeEv);
    }

    public boolean log(Parcelable parcelable) {
        return log(makeEv(parcelable));
    }

    private static ConnectivityMetricsEvent makeEv(Parcelable parcelable) {
        ConnectivityMetricsEvent connectivityMetricsEvent = new ConnectivityMetricsEvent();
        connectivityMetricsEvent.data = parcelable;
        return connectivityMetricsEvent;
    }
}
