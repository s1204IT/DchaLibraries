package com.mediatek.location;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LocalSocketAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.LocationManagerService;
import com.mediatek.location.Agps2FrameworkInterface;
import com.mediatek.location.Framework2AgpsInterface;
import com.mediatek.socket.base.UdpClient;
import com.mediatek.socket.base.UdpServer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class AgpsHelper extends Thread {
    private static final String CHANNEL_IN = "mtk_agps2framework";
    private static final String CHANNEL_OUT = "mtk_framework2agps";
    private static final int CMD_NET_TIMEOUT = 102;
    private static final int CMD_QUERY_DNS = 101;
    private static final int CMD_RELEASE_NET = 103;
    private static final int CMD_REMOVE_GPS_ICON = 105;
    private static final int CMD_REQUEST_GPS_ICON = 104;
    private static final int CMD_REQUEST_NET = 100;
    private static final boolean DEBUG = LocationManagerService.D;
    private static final long NET_REQ_TIMEOUT = 10000;
    private static final String TAG = "MtkAgpsHelper";
    private static final String WAKELOCK_KEY = "MtkAgps";
    private final ConnectivityManager mConnManager;
    private final Context mContext;
    private Framework2AgpsInterface.Framework2AgpsInterfaceSender mFwkToAgps;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private final LocationExt mLocExt;
    private NetworkRequest mNetReqEmergency;
    private NetworkRequest mNetReqIms;
    private NetworkRequest mNetReqSupl;
    private UdpServer mNodeIn;
    private UdpClient mNodeOut;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private final ArrayList<AgpsNetReq> mAgpsNetReqs = new ArrayList<>(2);
    private final byte[] mEmptyIpv6 = new byte[16];
    private Agps2FrameworkInterface.Agps2FrameworkInterfaceReceiver mReceiver = new Agps2FrameworkInterface.Agps2FrameworkInterfaceReceiver() {
        @Override
        public void isExist() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("isExist()");
            }
        }

        @Override
        public void acquireWakeLock() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("acquireWakeLock()");
            }
            AgpsHelper.this.mWakeLock.acquire();
        }

        @Override
        public void releaseWakeLock() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("releaseWakeLock()");
            }
            AgpsHelper.this.mWakeLock.release();
        }

        @Override
        public void requestDedicatedApnAndDnsQuery(String fqdn, boolean isEsupl, boolean isSuplApn) {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("requestDedicatedApnAndDnsQuery() fqdn=" + fqdn + " isEsupl=" + isEsupl + " isSuplApn=" + isSuplApn);
            }
            AgpsNetReq agpsNetReq = AgpsHelper.this.new AgpsNetReq(fqdn, isEsupl, isSuplApn);
            AgpsHelper.this.sendMessage(100, agpsNetReq);
        }

        @Override
        public void releaseDedicatedApn() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("releaseDedicatedApn()");
            }
            AgpsHelper.this.sendMessage(103, null);
        }

        @Override
        public void requestGpsIcon() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("requestGpsIcon");
            }
            AgpsHelper.this.sendMessage(104, null);
        }

        @Override
        public void removeGpsIcon() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("removeGpsIcon()");
            }
            AgpsHelper.this.sendMessage(105, null);
        }
    };

    public static void log(String msg) {
        Log.d(TAG, msg);
    }

    public AgpsHelper(LocationExt locExt, Context context, ConnectivityManager connMgr) {
        if (DEBUG) {
            log("AgpsHelper constructor");
        }
        this.mLocExt = locExt;
        this.mContext = context;
        this.mConnManager = connMgr;
        new Thread("MtkAgpsSocket") {
            @Override
            public void run() {
                if (AgpsHelper.DEBUG) {
                    AgpsHelper.log("SocketThread.run()");
                }
                AgpsHelper.this.waitForAgpsCommands();
            }
        }.start();
    }

    protected void setup() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, WAKELOCK_KEY);
        this.mWakeLock.setReferenceCounted(true);
        NetworkRequest.Builder nrBuilder = new NetworkRequest.Builder();
        this.mNetReqEmergency = nrBuilder.addTransportType(0).addCapability(10).build();
        this.mNetReqIms = nrBuilder.removeCapability(10).addCapability(4).build();
        this.mNetReqSupl = nrBuilder.removeCapability(4).addCapability(1).build();
        this.mHandlerThread = new HandlerThread("MtkAgpsHandler");
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 100:
                        AgpsHelper.this.handleRequestNet((AgpsNetReq) msg.obj);
                        break;
                    case 101:
                        AgpsHelper.this.handleDnsQuery((AgpsNetReq) msg.obj);
                        break;
                    case 102:
                        AgpsHelper.this.handleNetTimeout((AgpsNetReq) msg.obj);
                        break;
                    case 103:
                        AgpsHelper.this.handleReleaseNet((AgpsNetReq) msg.obj);
                        break;
                    case 104:
                        AgpsHelper.this.handleRequestGpsIcon();
                        break;
                    case 105:
                        AgpsHelper.this.handleRemoveGpsIcon();
                        break;
                }
            }
        };
    }

    protected void waitForAgpsCommands() {
        setup();
        try {
            try {
                this.mNodeOut = new UdpClient(CHANNEL_OUT, LocalSocketAddress.Namespace.ABSTRACT, 35);
                this.mNodeIn = new UdpServer(CHANNEL_IN, LocalSocketAddress.Namespace.ABSTRACT, Agps2FrameworkInterface.MAX_BUFF_SIZE);
                this.mFwkToAgps = new Framework2AgpsInterface.Framework2AgpsInterfaceSender();
                while (true) {
                    this.mReceiver.readAndDecode(this.mNodeIn);
                }
            } catch (Exception e) {
                log(e.toString());
                if (this.mNodeIn != null) {
                    this.mNodeIn.close();
                    this.mNodeIn = null;
                }
                this.mReceiver = null;
            }
        } catch (Throwable th) {
            if (this.mNodeIn != null) {
                this.mNodeIn.close();
                this.mNodeIn = null;
            }
            this.mReceiver = null;
            throw th;
        }
    }

    void sendMessage(int what, Object obj) {
        this.mHandler.obtainMessage(what, 0, 0, obj).sendToTarget();
    }

    void sendMessageDelayed(int what, Object obj, long delayMillis) {
        Message msg = this.mHandler.obtainMessage(what, 0, 0, obj);
        this.mHandler.sendMessageDelayed(msg, delayMillis);
    }

    void removeMessages(int what, Object obj) {
        this.mHandler.removeMessages(what, obj);
    }

    void doReleaseNet(AgpsNetReq req) {
        if (DEBUG) {
            log("doReleaseNet");
        }
        this.mAgpsNetReqs.remove(req);
        req.releaseNet();
    }

    void handleRequestNet(AgpsNetReq req) {
        if (DEBUG) {
            log("handleRequestNet");
        }
        while (this.mAgpsNetReqs.size() >= 2) {
            if (DEBUG) {
                log("remove potential leak of AgpsNetReq");
            }
            doReleaseNet(this.mAgpsNetReqs.get(0));
        }
        this.mAgpsNetReqs.add(req);
        req.requestNet();
    }

    void handleDnsQuery(AgpsNetReq req) {
        if (DEBUG) {
            log("handleDnsQuery");
        }
        req.queryDns();
    }

    void handleNetTimeout(AgpsNetReq req) {
        if (DEBUG) {
            log("handleNetTimeout");
        }
        req.queryDns();
    }

    void handleReleaseNet(AgpsNetReq req) {
        if (DEBUG) {
            log("handleReleaseNet");
        }
        if (req != null) {
            doReleaseNet(req);
        } else {
            if (this.mAgpsNetReqs.isEmpty()) {
                return;
            }
            doReleaseNet(this.mAgpsNetReqs.get(0));
        }
    }

    void handleRequestGpsIcon() {
        Intent intent = new Intent("android.location.HIGH_POWER_REQUEST_CHANGE");
        intent.putExtra("requestGpsByNi", true);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    void handleRemoveGpsIcon() {
        Intent intent = new Intent("android.location.HIGH_POWER_REQUEST_CHANGE");
        intent.putExtra("requestGpsByNi", false);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    class AgpsNetReq {
        String mFqdn;
        boolean mIsEsupl;
        boolean mIsSuplApn;
        boolean mIsQueried = false;
        int mRouteType = -1;
        NetworkRequest mNetReq = null;
        Network mNet = null;
        ConnectivityManager.NetworkCallback mNetworkCallback = null;

        AgpsNetReq(String fqdn, boolean isEsupl, boolean isSuplApn) {
            this.mFqdn = fqdn;
            this.mIsEsupl = isEsupl;
            this.mIsSuplApn = isSuplApn;
        }

        void decideRoute() {
            Network netEmergemcy = null;
            Network netIms = null;
            Network netSupl = null;
            Network[] nets = AgpsHelper.this.mConnManager.getAllNetworks();
            if (nets != null) {
                for (Network net : nets) {
                    NetworkCapabilities netCap = AgpsHelper.this.mConnManager.getNetworkCapabilities(net);
                    if (AgpsHelper.DEBUG) {
                        AgpsHelper.log("checking net=" + net + " cap=" + netCap);
                    }
                    if (netEmergemcy == null && netCap != null && netCap.hasCapability(10)) {
                        netEmergemcy = net;
                        if (AgpsHelper.DEBUG) {
                            AgpsHelper.log("NetEmergemcy");
                        }
                    }
                    if (netIms == null && netCap != null && netCap.hasCapability(4)) {
                        netIms = net;
                        if (AgpsHelper.DEBUG) {
                            AgpsHelper.log("NetIms");
                        }
                    }
                    if (netSupl == null && netCap != null && netCap.hasCapability(1)) {
                        netSupl = net;
                        if (AgpsHelper.DEBUG) {
                            AgpsHelper.log("NetSupl");
                        }
                    }
                }
            }
            if (this.mIsEsupl) {
                if (netEmergemcy != null) {
                    if (AgpsHelper.DEBUG) {
                        AgpsHelper.log("to use NetEmergemcy");
                    }
                    this.mRouteType = 15;
                    this.mNet = netEmergemcy;
                    this.mNetReq = AgpsHelper.this.mNetReqEmergency;
                    return;
                }
                if (netIms != null) {
                    if (AgpsHelper.DEBUG) {
                        AgpsHelper.log("to use NetIms");
                    }
                    this.mRouteType = 11;
                    this.mNet = netIms;
                    this.mNetReq = AgpsHelper.this.mNetReqIms;
                    return;
                }
            }
            if (!this.mIsSuplApn || !AgpsHelper.this.mLocExt.hasIccCard() || AgpsHelper.this.mLocExt.isAirplaneModeOn()) {
                return;
            }
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("try to use NetSupl");
            }
            this.mRouteType = 3;
            this.mNet = netSupl;
            this.mNetReq = AgpsHelper.this.mNetReqSupl;
        }

        void requestNet() {
            boolean isDirectDns = false;
            decideRoute();
            if (this.mNetReq != null) {
                this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network net) {
                        if (AgpsHelper.DEBUG) {
                            AgpsHelper.log("onAvailable: network=" + net);
                        }
                        synchronized (AgpsNetReq.this) {
                            if (AgpsNetReq.this.mNet == null) {
                                AgpsNetReq.this.mNet = net;
                                AgpsHelper.this.removeMessages(102, AgpsNetReq.this);
                                AgpsHelper.this.sendMessage(101, AgpsNetReq.this);
                            }
                        }
                    }

                    @Override
                    public void onLost(Network net) {
                        if (AgpsHelper.DEBUG) {
                            AgpsHelper.log("onLost: network=" + net);
                        }
                    }
                };
                synchronized (this) {
                    if (AgpsHelper.DEBUG) {
                        AgpsHelper.log("request net:" + this.mNetReq);
                    }
                    AgpsHelper.this.mConnManager.requestNetwork(this.mNetReq, this.mNetworkCallback);
                    if (this.mNet == null) {
                        if (AgpsHelper.DEBUG) {
                            AgpsHelper.log("wait for net callback");
                        }
                        AgpsHelper.this.sendMessageDelayed(102, this, 10000L);
                    } else {
                        isDirectDns = true;
                    }
                }
            } else {
                isDirectDns = true;
            }
            if (!isDirectDns) {
                return;
            }
            queryDns();
        }

        void getDefaultNet() {
            Network net = AgpsHelper.this.mConnManager.getActiveNetwork();
            if (net == null) {
                return;
            }
            this.mNet = net;
            NetworkCapabilities netCap = AgpsHelper.this.mConnManager.getNetworkCapabilities(net);
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("default network=" + net + " cap=" + netCap);
            }
            if (netCap != null && netCap.hasTransport(1)) {
                if (AgpsHelper.DEBUG) {
                    AgpsHelper.log("to use NetWiFi");
                }
                this.mRouteType = 1;
            } else {
                if (AgpsHelper.DEBUG) {
                    AgpsHelper.log("to use NetMobile");
                }
                this.mRouteType = 0;
            }
        }

        void queryDns() {
            InetAddress[] ias;
            if (this.mIsQueried) {
                return;
            }
            this.mIsQueried = true;
            boolean hasIpv4 = false;
            boolean hasIpv6 = false;
            int ipv4 = 0;
            byte[] ipv6 = AgpsHelper.this.mEmptyIpv6;
            try {
                if (this.mNet != null) {
                    ias = this.mNet.getAllByName(this.mFqdn);
                } else {
                    getDefaultNet();
                    ias = InetAddress.getAllByName(this.mFqdn);
                }
                for (InetAddress ia : ias) {
                    byte[] addr = ia.getAddress();
                    AgpsHelper.log("ia=" + ia.toString() + " bytes=" + Arrays.toString(addr) + " network=" + this.mNet);
                    if (addr.length == 4 && !hasIpv4) {
                        hasIpv4 = true;
                        ipv4 = ((((((addr[3] & 255) << 8) | (addr[2] & 255)) << 8) | (addr[1] & 255)) << 8) | (addr[0] & 255);
                        requestRoute(ia);
                    } else if (addr.length == 16 && !hasIpv6) {
                        hasIpv6 = true;
                        ipv6 = addr;
                        requestRoute(ia);
                    }
                }
            } catch (UnknownHostException e) {
                AgpsHelper.log("UnknownHostException for fqdn=" + this.mFqdn);
            }
            boolean z = !hasIpv4 ? hasIpv6 : true;
            boolean ret = AgpsHelper.this.mFwkToAgps.DnsQueryResult(AgpsHelper.this.mNodeOut, z, hasIpv4, ipv4, hasIpv6, ipv6);
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("DnsQueryResult() fqdn=" + this.mFqdn + " isSuccess=" + z + " hasIpv4=" + hasIpv4 + " ipv4=" + Integer.toHexString(ipv4) + " hasIpv6=" + hasIpv6 + " ipv6=" + Arrays.toString(ipv6) + " ret=" + ret);
            }
            if (z) {
                return;
            }
            AgpsHelper.this.doReleaseNet(this);
        }

        void requestRoute(InetAddress ia) {
            if (-1 == this.mRouteType) {
                return;
            }
            boolean result = AgpsHelper.this.mConnManager.requestRouteToHostAddress(this.mRouteType, ia);
            if (!result) {
                AgpsHelper.log("Error requesting route (" + this.mRouteType + ") to host: " + ia);
                if (this.mRouteType != 3) {
                    return;
                }
                boolean result2 = AgpsHelper.this.mConnManager.requestRouteToHostAddress(0, ia);
                if (!result2) {
                    AgpsHelper.log("Error requesting route (TYPE_MOBILE) to host: " + ia);
                    return;
                } else {
                    if (!AgpsHelper.DEBUG) {
                        return;
                    }
                    AgpsHelper.log("Requesting route (TYPE_MOBILE) to host: " + ia);
                    return;
                }
            }
            if (!AgpsHelper.DEBUG) {
                return;
            }
            AgpsHelper.log("Requesting route (" + this.mRouteType + ") to host: " + ia);
        }

        synchronized void releaseNet() {
            if (AgpsHelper.DEBUG) {
                AgpsHelper.log("releaseNet() fqdn=" + this.mFqdn + " eSupl=" + this.mIsEsupl + " suplApn=" + this.mIsSuplApn);
            }
            if (this.mNetworkCallback != null) {
                if (AgpsHelper.DEBUG) {
                    AgpsHelper.log("remove net callback");
                }
                AgpsHelper.this.mConnManager.unregisterNetworkCallback(this.mNetworkCallback);
                this.mNetworkCallback = null;
                AgpsHelper.this.removeMessages(102, this);
            }
            this.mIsQueried = true;
            this.mNetReq = null;
            this.mNet = null;
            this.mFqdn = null;
        }
    }
}
