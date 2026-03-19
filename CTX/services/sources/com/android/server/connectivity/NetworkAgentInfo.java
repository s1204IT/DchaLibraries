package com.android.server.connectivity;

import android.content.Context;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.WakeupMessage;
import com.android.server.ConnectivityService;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class NetworkAgentInfo implements Comparable<NetworkAgentInfo> {
    private static final boolean ADD = true;
    public static final int EVENT_NETWORK_LINGER_COMPLETE = 1001;
    private static final boolean REMOVE = false;
    private static final String TAG = ConnectivityService.class.getSimpleName();
    private static final boolean VDBG = false;
    public final AsyncChannel asyncChannel;
    public boolean avoidUnvalidated;
    public Nat464Xlat clatd;
    public boolean created;
    private int currentScore;
    public boolean everCaptivePortalDetected;
    public boolean everConnected;
    public boolean everValidated;
    public boolean lastCaptivePortalDetected;
    public boolean lastValidated;
    public LinkProperties linkProperties;
    private final ConnectivityService mConnService;
    private final Context mContext;
    private final Handler mHandler;
    private long mLingerExpiryMs;
    private WakeupMessage mLingerMessage;
    private boolean mLingering;
    public final Messenger messenger;
    public final Network network;
    public NetworkCapabilities networkCapabilities;
    public NetworkInfo networkInfo;
    public final NetworkMisc networkMisc;
    public final NetworkMonitor networkMonitor;
    private final SortedSet<LingerTimer> mLingerTimers = new TreeSet();
    private final SparseArray<LingerTimer> mLingerTimerForRequest = new SparseArray<>();
    private final SparseArray<NetworkRequest> mNetworkRequests = new SparseArray<>();
    private int mNumRequestNetworkRequests = 0;
    private int mNumBackgroundNetworkRequests = 0;

    public static class LingerTimer implements Comparable<LingerTimer> {
        public final long expiryMs;
        public final NetworkRequest request;

        public LingerTimer(NetworkRequest networkRequest, long j) {
            this.request = networkRequest;
            this.expiryMs = j;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof LingerTimer)) {
                return false;
            }
            LingerTimer lingerTimer = (LingerTimer) obj;
            return this.request.requestId == lingerTimer.request.requestId && this.expiryMs == lingerTimer.expiryMs;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.request.requestId), Long.valueOf(this.expiryMs));
        }

        @Override
        public int compareTo(LingerTimer lingerTimer) {
            if (this.expiryMs != lingerTimer.expiryMs) {
                return Long.compare(this.expiryMs, lingerTimer.expiryMs);
            }
            return Integer.compare(this.request.requestId, lingerTimer.request.requestId);
        }

        public String toString() {
            return String.format("%s, expires %dms", this.request.toString(), Long.valueOf(this.expiryMs - SystemClock.elapsedRealtime()));
        }
    }

    public NetworkAgentInfo(Messenger messenger, AsyncChannel asyncChannel, Network network, NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i, Context context, Handler handler, NetworkMisc networkMisc, NetworkRequest networkRequest, ConnectivityService connectivityService) {
        this.messenger = messenger;
        this.asyncChannel = asyncChannel;
        this.network = network;
        this.networkInfo = networkInfo;
        this.linkProperties = linkProperties;
        this.networkCapabilities = networkCapabilities;
        this.currentScore = i;
        this.mConnService = connectivityService;
        this.mContext = context;
        this.mHandler = handler;
        this.networkMonitor = this.mConnService.createNetworkMonitor(context, handler, this, networkRequest);
        this.networkMisc = networkMisc;
    }

    public ConnectivityService connService() {
        return this.mConnService;
    }

    public Handler handler() {
        return this.mHandler;
    }

    public Network network() {
        return this.network;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$net$NetworkRequest$Type = new int[NetworkRequest.Type.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkRequest$Type[NetworkRequest.Type.REQUEST.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkRequest$Type[NetworkRequest.Type.BACKGROUND_REQUEST.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$NetworkRequest$Type[NetworkRequest.Type.TRACK_DEFAULT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$NetworkRequest$Type[NetworkRequest.Type.LISTEN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$NetworkRequest$Type[NetworkRequest.Type.NONE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private void updateRequestCounts(boolean z, NetworkRequest networkRequest) {
        int i = z ? 1 : -1;
        switch (AnonymousClass1.$SwitchMap$android$net$NetworkRequest$Type[networkRequest.type.ordinal()]) {
            case 1:
                this.mNumRequestNetworkRequests += i;
                break;
            case 2:
                this.mNumRequestNetworkRequests += i;
                this.mNumBackgroundNetworkRequests += i;
                break;
            case 3:
            case 4:
                break;
            default:
                Log.wtf(TAG, "Unhandled request type " + networkRequest.type);
                break;
        }
    }

    public boolean addRequest(NetworkRequest networkRequest) {
        NetworkRequest networkRequest2 = this.mNetworkRequests.get(networkRequest.requestId);
        if (networkRequest2 == networkRequest) {
            return false;
        }
        if (networkRequest2 != null) {
            Log.wtf(TAG, String.format("Duplicate requestId for %s and %s on %s", networkRequest, networkRequest2, name()));
            updateRequestCounts(false, networkRequest2);
        }
        this.mNetworkRequests.put(networkRequest.requestId, networkRequest);
        updateRequestCounts(true, networkRequest);
        return true;
    }

    public void removeRequest(int i) {
        NetworkRequest networkRequest = this.mNetworkRequests.get(i);
        if (networkRequest == null) {
            return;
        }
        updateRequestCounts(false, networkRequest);
        this.mNetworkRequests.remove(i);
        if (networkRequest.isRequest()) {
            unlingerRequest(networkRequest);
        }
    }

    public boolean isSatisfyingRequest(int i) {
        return this.mNetworkRequests.get(i) != null;
    }

    public NetworkRequest requestAt(int i) {
        return this.mNetworkRequests.valueAt(i);
    }

    public int numRequestNetworkRequests() {
        return this.mNumRequestNetworkRequests;
    }

    public int numBackgroundNetworkRequests() {
        return this.mNumBackgroundNetworkRequests;
    }

    public int numForegroundNetworkRequests() {
        return this.mNumRequestNetworkRequests - this.mNumBackgroundNetworkRequests;
    }

    public int numNetworkRequests() {
        return this.mNetworkRequests.size();
    }

    public boolean isBackgroundNetwork() {
        return !isVPN() && numForegroundNetworkRequests() == 0 && this.mNumBackgroundNetworkRequests > 0 && !isLingering();
    }

    public boolean isSuspended() {
        return this.networkInfo.getState() == NetworkInfo.State.SUSPENDED;
    }

    public boolean satisfies(NetworkRequest networkRequest) {
        return this.created && networkRequest.networkCapabilities.satisfiedByNetworkCapabilities(this.networkCapabilities);
    }

    public boolean satisfiesImmutableCapabilitiesOf(NetworkRequest networkRequest) {
        return this.created && networkRequest.networkCapabilities.satisfiedByImmutableNetworkCapabilities(this.networkCapabilities);
    }

    public boolean isVPN() {
        return this.networkCapabilities.hasTransport(4);
    }

    private int getCurrentScore(boolean z) {
        if (this.networkMisc.explicitlySelected && (this.networkMisc.acceptUnvalidated || z)) {
            return 100;
        }
        int i = this.currentScore;
        if (!this.lastValidated && !z && !ignoreWifiUnvalidationPenalty() && !isVPN()) {
            i -= 40;
        }
        if (i < 0) {
            return 0;
        }
        return i;
    }

    private boolean ignoreWifiUnvalidationPenalty() {
        return (this.networkCapabilities.hasTransport(1) && this.networkCapabilities.hasCapability(12)) && !(this.mConnService.avoidBadWifi() || this.avoidUnvalidated) && this.everValidated;
    }

    public int getCurrentScore() {
        return getCurrentScore(false);
    }

    public int getCurrentScoreAsValidated() {
        return getCurrentScore(true);
    }

    public void setCurrentScore(int i) {
        this.currentScore = i;
    }

    public NetworkState getNetworkState() {
        NetworkState networkState;
        synchronized (this) {
            networkState = new NetworkState(new NetworkInfo(this.networkInfo), new LinkProperties(this.linkProperties), new NetworkCapabilities(this.networkCapabilities), this.network, this.networkMisc != null ? this.networkMisc.subscriberId : null, (String) null);
        }
        return networkState;
    }

    public void lingerRequest(NetworkRequest networkRequest, long j, long j2) {
        if (this.mLingerTimerForRequest.get(networkRequest.requestId) != null) {
            Log.wtf(TAG, name() + ": request " + networkRequest.requestId + " already lingered");
        }
        LingerTimer lingerTimer = new LingerTimer(networkRequest, j + j2);
        this.mLingerTimers.add(lingerTimer);
        this.mLingerTimerForRequest.put(networkRequest.requestId, lingerTimer);
    }

    public boolean unlingerRequest(NetworkRequest networkRequest) {
        LingerTimer lingerTimer = this.mLingerTimerForRequest.get(networkRequest.requestId);
        if (lingerTimer != null) {
            this.mLingerTimers.remove(lingerTimer);
            this.mLingerTimerForRequest.remove(networkRequest.requestId);
            return true;
        }
        return false;
    }

    public long getLingerExpiry() {
        return this.mLingerExpiryMs;
    }

    public void updateLingerTimer() {
        long j;
        if (!this.mLingerTimers.isEmpty()) {
            j = this.mLingerTimers.last().expiryMs;
        } else {
            j = 0;
        }
        if (j == this.mLingerExpiryMs) {
            return;
        }
        if (this.mLingerMessage != null) {
            this.mLingerMessage.cancel();
            this.mLingerMessage = null;
        }
        if (j > 0) {
            this.mLingerMessage = this.mConnService.makeWakeupMessage(this.mContext, this.mHandler, "NETWORK_LINGER_COMPLETE." + this.network.netId, EVENT_NETWORK_LINGER_COMPLETE, this);
            this.mLingerMessage.schedule(j);
        }
        this.mLingerExpiryMs = j;
    }

    public void linger() {
        this.mLingering = true;
    }

    public void unlinger() {
        this.mLingering = false;
    }

    public boolean isLingering() {
        return this.mLingering;
    }

    public void clearLingerState() {
        if (this.mLingerMessage != null) {
            this.mLingerMessage.cancel();
            this.mLingerMessage = null;
        }
        this.mLingerTimers.clear();
        this.mLingerTimerForRequest.clear();
        updateLingerTimer();
        this.mLingering = false;
    }

    public void dumpLingerTimers(PrintWriter printWriter) {
        Iterator<LingerTimer> it = this.mLingerTimers.iterator();
        while (it.hasNext()) {
            printWriter.println(it.next());
        }
    }

    public void updateClat(INetworkManagementService iNetworkManagementService) {
        if (Nat464Xlat.requiresClat(this)) {
            maybeStartClat(iNetworkManagementService);
        } else {
            maybeStopClat();
        }
    }

    public void maybeStartClat(INetworkManagementService iNetworkManagementService) {
        if (this.clatd != null && this.clatd.isStarted()) {
            return;
        }
        this.clatd = new Nat464Xlat(iNetworkManagementService, this);
        this.clatd.start();
    }

    public void maybeStopClat() {
        if (this.clatd == null) {
            return;
        }
        this.clatd.stop();
        this.clatd = null;
    }

    public String toString() {
        return "NetworkAgentInfo{ ni{" + this.networkInfo + "}  network{" + this.network + "}  nethandle{" + this.network.getNetworkHandle() + "}  lp{" + this.linkProperties + "}  nc{" + this.networkCapabilities + "}  Score{" + getCurrentScore() + "}  everValidated{" + this.everValidated + "}  lastValidated{" + this.lastValidated + "}  created{" + this.created + "} lingering{" + isLingering() + "} explicitlySelected{" + this.networkMisc.explicitlySelected + "} acceptUnvalidated{" + this.networkMisc.acceptUnvalidated + "} everCaptivePortalDetected{" + this.everCaptivePortalDetected + "} lastCaptivePortalDetected{" + this.lastCaptivePortalDetected + "} clat{" + this.clatd + "} }";
    }

    public String name() {
        NetworkInfo networkInfo = this.networkInfo;
        if (networkInfo == null) {
            return "NetworkAgentInfo [null - " + Objects.toString(this.network) + "]";
        }
        return "NetworkAgentInfo [" + networkInfo.getTypeName() + " (" + networkInfo.getSubtypeName() + ") - " + Objects.toString(this.network) + "]";
    }

    @Override
    public int compareTo(NetworkAgentInfo networkAgentInfo) {
        return networkAgentInfo.getCurrentScore() - getCurrentScore();
    }
}
