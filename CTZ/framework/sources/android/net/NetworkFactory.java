package android.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class NetworkFactory extends Handler {
    private static final int BASE = 536576;
    public static final int CMD_CANCEL_REQUEST = 536577;
    public static final int CMD_REQUEST_NETWORK = 536576;
    private static final int CMD_SET_FILTER = 536579;
    private static final int CMD_SET_SCORE = 536578;
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private final String LOG_TAG;
    private NetworkCapabilities mCapabilityFilter;
    private final Context mContext;
    private Messenger mMessenger;
    private final SparseArray<NetworkRequestInfo> mNetworkRequests;
    private int mRefCount;
    private int mScore;

    public NetworkFactory(Looper looper, Context context, String str, NetworkCapabilities networkCapabilities) {
        super(looper);
        this.mNetworkRequests = new SparseArray<>();
        this.mRefCount = 0;
        this.mMessenger = null;
        this.LOG_TAG = str;
        this.mContext = context;
        this.mCapabilityFilter = networkCapabilities;
    }

    public void register() {
        log("Registering NetworkFactory");
        if (this.mMessenger == null) {
            this.mMessenger = new Messenger(this);
            ConnectivityManager.from(this.mContext).registerNetworkFactory(this.mMessenger, this.LOG_TAG);
        }
    }

    public void unregister() {
        log("Unregistering NetworkFactory");
        if (this.mMessenger != null) {
            ConnectivityManager.from(this.mContext).unregisterNetworkFactory(this.mMessenger);
            this.mMessenger = null;
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 536576:
                handleAddRequest((NetworkRequest) message.obj, message.arg1);
                break;
            case CMD_CANCEL_REQUEST:
                handleRemoveRequest((NetworkRequest) message.obj);
                break;
            case CMD_SET_SCORE:
                handleSetScore(message.arg1);
                break;
            case CMD_SET_FILTER:
                handleSetFilter((NetworkCapabilities) message.obj);
                break;
        }
    }

    private class NetworkRequestInfo {
        public final NetworkRequest request;
        public boolean requested = false;
        public int score;

        public NetworkRequestInfo(NetworkRequest networkRequest, int i) {
            this.request = networkRequest;
            this.score = i;
        }

        public String toString() {
            return "{" + this.request + ", score=" + this.score + ", requested=" + this.requested + "}";
        }
    }

    @VisibleForTesting
    protected void handleAddRequest(NetworkRequest networkRequest, int i) {
        NetworkRequestInfo networkRequestInfo = this.mNetworkRequests.get(networkRequest.requestId);
        if (networkRequestInfo == null) {
            log("got request " + networkRequest + " with score " + i);
            networkRequestInfo = new NetworkRequestInfo(networkRequest, i);
            this.mNetworkRequests.put(networkRequestInfo.request.requestId, networkRequestInfo);
        } else {
            networkRequestInfo.score = i;
        }
        evalRequest(networkRequestInfo);
    }

    @VisibleForTesting
    protected void handleRemoveRequest(NetworkRequest networkRequest) {
        NetworkRequestInfo networkRequestInfo = this.mNetworkRequests.get(networkRequest.requestId);
        if (networkRequestInfo != null) {
            this.mNetworkRequests.remove(networkRequest.requestId);
            if (networkRequestInfo.requested) {
                releaseNetworkFor(networkRequestInfo.request);
            }
        }
    }

    private void handleSetScore(int i) {
        this.mScore = i;
        evalRequests();
    }

    private void handleSetFilter(NetworkCapabilities networkCapabilities) {
        this.mCapabilityFilter = networkCapabilities;
        evalRequests();
    }

    public boolean acceptRequest(NetworkRequest networkRequest, int i) {
        return true;
    }

    private void evalRequest(NetworkRequestInfo networkRequestInfo) {
        if (!networkRequestInfo.requested && networkRequestInfo.score < this.mScore && networkRequestInfo.request.networkCapabilities.satisfiedByNetworkCapabilities(this.mCapabilityFilter) && acceptRequest(networkRequestInfo.request, networkRequestInfo.score)) {
            needNetworkFor(networkRequestInfo.request, networkRequestInfo.score);
            networkRequestInfo.requested = true;
        } else if (networkRequestInfo.requested) {
            if (networkRequestInfo.score > this.mScore || !networkRequestInfo.request.networkCapabilities.satisfiedByNetworkCapabilities(this.mCapabilityFilter) || !acceptRequest(networkRequestInfo.request, networkRequestInfo.score)) {
                releaseNetworkFor(networkRequestInfo.request);
                networkRequestInfo.requested = false;
            }
        }
    }

    private void evalRequests() {
        for (int i = 0; i < this.mNetworkRequests.size(); i++) {
            evalRequest(this.mNetworkRequests.valueAt(i));
        }
    }

    protected void reevaluateAllRequests() {
        post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.evalRequests();
            }
        });
    }

    protected void startNetwork() {
    }

    protected void stopNetwork() {
    }

    protected void needNetworkFor(NetworkRequest networkRequest, int i) {
        int i2 = this.mRefCount + 1;
        this.mRefCount = i2;
        if (i2 == 1) {
            startNetwork();
        }
    }

    protected void releaseNetworkFor(NetworkRequest networkRequest) {
        int i = this.mRefCount - 1;
        this.mRefCount = i;
        if (i == 0) {
            stopNetwork();
        }
    }

    public void addNetworkRequest(NetworkRequest networkRequest, int i) {
        sendMessage(obtainMessage(536576, new NetworkRequestInfo(networkRequest, i)));
    }

    public void removeNetworkRequest(NetworkRequest networkRequest) {
        sendMessage(obtainMessage(CMD_CANCEL_REQUEST, networkRequest));
    }

    public void setScoreFilter(int i) {
        sendMessage(obtainMessage(CMD_SET_SCORE, i, 0));
    }

    public void setCapabilityFilter(NetworkCapabilities networkCapabilities) {
        sendMessage(obtainMessage(CMD_SET_FILTER, new NetworkCapabilities(networkCapabilities)));
    }

    @VisibleForTesting
    protected int getRequestCount() {
        return this.mNetworkRequests.size();
    }

    protected void log(String str) {
        Log.d(this.LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println(toString());
        indentingPrintWriter.increaseIndent();
        for (int i = 0; i < this.mNetworkRequests.size(); i++) {
            indentingPrintWriter.println(this.mNetworkRequests.valueAt(i));
        }
        indentingPrintWriter.decreaseIndent();
    }

    @Override
    public String toString() {
        return "{" + this.LOG_TAG + " - ScoreFilter=" + this.mScore + ", Filter=" + this.mCapabilityFilter + ", requests=" + this.mNetworkRequests.size() + ", refCount=" + this.mRefCount + "}";
    }
}
