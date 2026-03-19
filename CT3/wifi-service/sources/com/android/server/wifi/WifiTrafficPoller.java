package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class WifiTrafficPoller {
    private static final int ADD_CLIENT = 3;
    private static final int ENABLE_TRAFFIC_STATS_POLL = 1;
    private static final int POLL_TRAFFIC_STATS_INTERVAL_MSECS = 1000;
    private static final int REMOVE_CLIENT = 4;
    private static final String TAG = "WifiTrafficPoller";
    private static final int TRAFFIC_STATS_POLL = 2;
    private int mDataActivity;
    private final String mInterface;
    private NetworkInfo mNetworkInfo;
    private long mRxPkts;
    private final TrafficHandler mTrafficHandler;
    private long mTxPkts;
    private boolean DBG = false;
    private boolean VDBG = false;
    private boolean mEnableTrafficStatsPoll = false;
    private int mTrafficStatsPollToken = 0;
    private final List<Messenger> mClients = new ArrayList();
    private AtomicBoolean mScreenOn = new AtomicBoolean(true);

    WifiTrafficPoller(Context context, Looper looper, String iface) {
        this.mInterface = iface;
        this.mTrafficHandler = new TrafficHandler(looper);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent == null) {
                    return;
                }
                if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    WifiTrafficPoller.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    WifiTrafficPoller.this.mScreenOn.set(false);
                } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                    WifiTrafficPoller.this.mScreenOn.set(true);
                }
                WifiTrafficPoller.this.evaluateTrafficStatsPolling();
            }
        }, filter);
    }

    void addClient(Messenger client) {
        Message.obtain(this.mTrafficHandler, 3, client).sendToTarget();
    }

    void removeClient(Messenger client) {
        Message.obtain(this.mTrafficHandler, 4, client).sendToTarget();
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.DBG = true;
        } else {
            this.DBG = false;
        }
    }

    private class TrafficHandler extends Handler {
        public TrafficHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiTrafficPoller.this.mEnableTrafficStatsPoll = msg.arg1 == 1;
                    if (WifiTrafficPoller.this.DBG) {
                        Log.e(WifiTrafficPoller.TAG, "ENABLE_TRAFFIC_STATS_POLL " + WifiTrafficPoller.this.mEnableTrafficStatsPoll + " Token " + Integer.toString(WifiTrafficPoller.this.mTrafficStatsPollToken));
                    }
                    WifiTrafficPoller.this.mTrafficStatsPollToken++;
                    if (WifiTrafficPoller.this.mEnableTrafficStatsPoll) {
                        WifiTrafficPoller.this.notifyOnDataActivity();
                        sendMessageDelayed(Message.obtain(this, 2, WifiTrafficPoller.this.mTrafficStatsPollToken, 0), 1000L);
                    }
                    break;
                case 2:
                    if (WifiTrafficPoller.this.VDBG) {
                        Log.e(WifiTrafficPoller.TAG, "TRAFFIC_STATS_POLL " + WifiTrafficPoller.this.mEnableTrafficStatsPoll + " Token " + Integer.toString(WifiTrafficPoller.this.mTrafficStatsPollToken) + " num clients " + WifiTrafficPoller.this.mClients.size());
                    }
                    if (msg.arg1 == WifiTrafficPoller.this.mTrafficStatsPollToken) {
                        WifiTrafficPoller.this.notifyOnDataActivity();
                        sendMessageDelayed(Message.obtain(this, 2, WifiTrafficPoller.this.mTrafficStatsPollToken, 0), 1000L);
                    }
                    break;
                case 3:
                    WifiTrafficPoller.this.mClients.add((Messenger) msg.obj);
                    if (WifiTrafficPoller.this.DBG) {
                        Log.e(WifiTrafficPoller.TAG, "ADD_CLIENT: " + Integer.toString(WifiTrafficPoller.this.mClients.size()));
                    }
                    break;
                case 4:
                    WifiTrafficPoller.this.mClients.remove(msg.obj);
                    break;
            }
        }
    }

    private void evaluateTrafficStatsPolling() {
        Message msg;
        if (this.mNetworkInfo == null) {
            return;
        }
        if (this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && this.mScreenOn.get()) {
            msg = Message.obtain(this.mTrafficHandler, 1, 1, 0);
        } else {
            msg = Message.obtain(this.mTrafficHandler, 1, 0, 0);
        }
        msg.sendToTarget();
    }

    private void notifyOnDataActivity() {
        long preTxPkts = this.mTxPkts;
        long preRxPkts = this.mRxPkts;
        this.mTxPkts = TrafficStats.getTxPackets(this.mInterface);
        this.mRxPkts = TrafficStats.getRxPackets(this.mInterface);
        if (this.VDBG) {
            Log.e(TAG, " packet count Tx=" + Long.toString(this.mTxPkts) + " Rx=" + Long.toString(this.mRxPkts));
        }
        if (preTxPkts > 0 || preRxPkts > 0) {
            long sent = this.mTxPkts - preTxPkts;
            long received = this.mRxPkts - preRxPkts;
            int dataActivity = sent > 0 ? 2 : 0;
            if (received > 0) {
                dataActivity |= 1;
            }
            if (dataActivity == this.mDataActivity || !this.mScreenOn.get()) {
                return;
            }
            this.mDataActivity = dataActivity;
            if (this.DBG) {
                Log.e(TAG, "notifying of data activity " + Integer.toString(this.mDataActivity));
            }
            for (Messenger client : this.mClients) {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.arg1 = this.mDataActivity;
                try {
                    client.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mEnableTrafficStatsPoll " + this.mEnableTrafficStatsPoll);
        pw.println("mTrafficStatsPollToken " + this.mTrafficStatsPollToken);
        pw.println("mTxPkts " + this.mTxPkts);
        pw.println("mRxPkts " + this.mRxPkts);
        pw.println("mDataActivity " + this.mDataActivity);
    }
}
