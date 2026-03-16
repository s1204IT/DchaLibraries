package com.android.server.connectivity;

import android.content.Context;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Messenger;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import java.util.ArrayList;

public class NetworkAgentInfo {
    private static final int EXPLICITLY_SELECTED_NETWORK_SCORE = 100;
    private static final int UNVALIDATED_SCORE_PENALTY = 40;
    public final AsyncChannel asyncChannel;
    public Nat464Xlat clatd;
    private int currentScore;
    public LinkProperties linkProperties;
    public final Messenger messenger;
    public NetworkCapabilities networkCapabilities;
    public NetworkInfo networkInfo;
    public final NetworkMisc networkMisc;
    public final NetworkMonitor networkMonitor;
    public final SparseArray<NetworkRequest> networkRequests = new SparseArray<>();
    public final ArrayList<NetworkRequest> networkLingered = new ArrayList<>();
    public Network network = null;
    public boolean created = false;
    public boolean everValidated = false;
    public boolean lastValidated = false;

    public NetworkAgentInfo(Messenger messenger, AsyncChannel ac, NetworkInfo info, LinkProperties lp, NetworkCapabilities nc, int score, Context context, Handler handler, NetworkMisc misc, NetworkRequest defaultRequest) {
        this.messenger = messenger;
        this.asyncChannel = ac;
        this.networkInfo = info;
        this.linkProperties = lp;
        this.networkCapabilities = nc;
        this.currentScore = score;
        this.networkMonitor = new NetworkMonitor(context, handler, this, defaultRequest);
        this.networkMisc = misc;
    }

    public void addRequest(NetworkRequest networkRequest) {
        this.networkRequests.put(networkRequest.requestId, networkRequest);
    }

    public boolean satisfies(NetworkRequest request) {
        return this.created && request.networkCapabilities.satisfiedByNetworkCapabilities(this.networkCapabilities);
    }

    public boolean isVPN() {
        return this.networkCapabilities.hasTransport(4);
    }

    private int getCurrentScore(boolean pretendValidated) {
        int score = this.currentScore;
        if (!this.everValidated && !pretendValidated) {
            score -= 40;
        }
        if (score < 0) {
            score = 0;
        }
        if (this.networkMisc.explicitlySelected) {
            return 100;
        }
        return score;
    }

    public int getCurrentScore() {
        return getCurrentScore(false);
    }

    public int getCurrentScoreAsValidated() {
        return getCurrentScore(true);
    }

    public void setCurrentScore(int newScore) {
        this.currentScore = newScore;
    }

    public String toString() {
        return "NetworkAgentInfo{ ni{" + this.networkInfo + "}  network{" + this.network + "}  lp{" + this.linkProperties + "}  nc{" + this.networkCapabilities + "}  Score{" + getCurrentScore() + "}  everValidated{" + this.everValidated + "}  lastValidated{" + this.lastValidated + "}  created{" + this.created + "}  explicitlySelected{" + this.networkMisc.explicitlySelected + "} }";
    }

    public String name() {
        return "NetworkAgentInfo [" + this.networkInfo.getTypeName() + " (" + this.networkInfo.getSubtypeName() + ") - " + (this.network == null ? "null" : this.network.toString()) + "]";
    }
}
