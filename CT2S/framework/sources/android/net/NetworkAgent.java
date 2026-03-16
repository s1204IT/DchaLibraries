package android.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import java.util.ArrayList;

public abstract class NetworkAgent extends Handler {
    private static final int BASE = 528384;
    public static final int CMD_REPORT_NETWORK_STATUS = 528391;
    public static final int CMD_SUSPECT_BAD = 528384;
    private static final boolean DBG = true;
    public static final int EVENT_NETWORK_CAPABILITIES_CHANGED = 528386;
    public static final int EVENT_NETWORK_INFO_CHANGED = 528385;
    public static final int EVENT_NETWORK_PROPERTIES_CHANGED = 528387;
    public static final int EVENT_NETWORK_SCORE_CHANGED = 528388;
    public static final int EVENT_SET_EXPLICITLY_SELECTED = 528392;
    public static final int EVENT_UID_RANGES_ADDED = 528389;
    public static final int EVENT_UID_RANGES_REMOVED = 528390;
    public static final int INVALID_NETWORK = 2;
    public static final int VALID_NETWORK = 1;
    private static final boolean VDBG = false;
    public static final int WIFI_BASE_SCORE = 60;
    private final String LOG_TAG;
    private volatile AsyncChannel mAsyncChannel;
    private final Context mContext;
    private final ArrayList<Message> mPreConnectedQueue;

    protected abstract void unwanted();

    public NetworkAgent(Looper looper, Context context, String logTag, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score) {
        this(looper, context, logTag, ni, nc, lp, score, null);
    }

    public NetworkAgent(Looper looper, Context context, String logTag, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, NetworkMisc misc) {
        super(looper);
        this.mPreConnectedQueue = new ArrayList<>();
        this.LOG_TAG = logTag;
        this.mContext = context;
        if (ni == null || nc == null || lp == null) {
            throw new IllegalArgumentException();
        }
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.registerNetworkAgent(new Messenger(this), new NetworkInfo(ni), new LinkProperties(lp), new NetworkCapabilities(nc), score, misc);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case AsyncChannel.CMD_CHANNEL_FULL_CONNECTION:
                if (this.mAsyncChannel != null) {
                    log("Received new connection while already connected!");
                    return;
                }
                AsyncChannel ac = new AsyncChannel();
                ac.connected(null, this, msg.replyTo);
                ac.replyToMessage(msg, AsyncChannel.CMD_CHANNEL_FULLY_CONNECTED, 0);
                synchronized (this.mPreConnectedQueue) {
                    this.mAsyncChannel = ac;
                    for (Message m : this.mPreConnectedQueue) {
                        ac.sendMessage(m);
                    }
                    this.mPreConnectedQueue.clear();
                    break;
                }
                return;
            case AsyncChannel.CMD_CHANNEL_DISCONNECT:
                if (this.mAsyncChannel != null) {
                    this.mAsyncChannel.disconnect();
                    return;
                }
                return;
            case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                log("NetworkAgent channel lost");
                unwanted();
                synchronized (this.mPreConnectedQueue) {
                    this.mAsyncChannel = null;
                    break;
                }
                return;
            case 528384:
                log("Unhandled Message " + msg);
                return;
            case CMD_REPORT_NETWORK_STATUS:
                networkStatus(msg.arg1);
                return;
            default:
                return;
        }
    }

    private void queueOrSendMessage(int what, Object obj) {
        synchronized (this.mPreConnectedQueue) {
            if (this.mAsyncChannel != null) {
                this.mAsyncChannel.sendMessage(what, obj);
            } else {
                Message msg = Message.obtain();
                msg.what = what;
                msg.obj = obj;
                this.mPreConnectedQueue.add(msg);
            }
        }
    }

    public void sendLinkProperties(LinkProperties linkProperties) {
        queueOrSendMessage(EVENT_NETWORK_PROPERTIES_CHANGED, new LinkProperties(linkProperties));
    }

    public void sendNetworkInfo(NetworkInfo networkInfo) {
        queueOrSendMessage(EVENT_NETWORK_INFO_CHANGED, new NetworkInfo(networkInfo));
    }

    public void sendNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        queueOrSendMessage(EVENT_NETWORK_CAPABILITIES_CHANGED, new NetworkCapabilities(networkCapabilities));
    }

    public void sendNetworkScore(int score) {
        if (score < 0) {
            throw new IllegalArgumentException("Score must be >= 0");
        }
        queueOrSendMessage(EVENT_NETWORK_SCORE_CHANGED, new Integer(score));
    }

    public void addUidRanges(UidRange[] ranges) {
        queueOrSendMessage(EVENT_UID_RANGES_ADDED, ranges);
    }

    public void removeUidRanges(UidRange[] ranges) {
        queueOrSendMessage(EVENT_UID_RANGES_REMOVED, ranges);
    }

    public void explicitlySelected() {
        queueOrSendMessage(EVENT_SET_EXPLICITLY_SELECTED, 0);
    }

    protected void networkStatus(int status) {
    }

    protected void log(String s) {
        Log.d(this.LOG_TAG, "NetworkAgent: " + s);
    }
}
