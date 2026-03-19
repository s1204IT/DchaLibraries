package com.mediatek.net.dhcp;

import android.content.Context;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.dhcp.DhcpClient;
import android.net.metrics.DhcpClientEvent;
import android.net.metrics.DhcpErrorEvent;
import android.net.metrics.IpConnectivityLog;
import android.net.util.InterfaceParams;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.mediatek.net.arp.ArpPeer;
import com.mediatek.net.dhcp.DhcpPacket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import libcore.io.IoBridge;

public class MtkDhcpClient extends DhcpClient {
    public static final int CMD_CLEAR_LINKADDRESS = 196615;
    public static final int CMD_CONFIGURE_LINKADDRESS = 196616;
    private static final int CMD_EXPIRE_DHCP = 196714;
    private static final int CMD_IP_RECOVER = 196739;
    private static final int CMD_KICK = 196709;
    public static final int CMD_ON_QUIT = 196613;
    public static final int CMD_POST_DHCP_ACTION = 196612;
    public static final int CMD_PRE_DHCP_ACTION = 196611;
    public static final int CMD_PRE_DHCP_ACTION_COMPLETE = 196614;
    private static final int CMD_REBIND_DHCP = 196713;
    private static final int CMD_RECEIVED_PACKET = 196710;
    private static final int CMD_RENEW_DHCP = 196712;
    public static final int CMD_START_DHCP = 196609;
    public static final int CMD_STOP_DHCP = 196610;
    private static final int CMD_TIMEOUT = 196711;
    public static final int DHCP_FAILURE = 2;
    private static final String DHCP_LEASE_FILE = "/data/misc/wifi/dhcp_lease.conf";
    public static final int DHCP_SUCCESS = 1;
    private static final int DHCP_TIMEOUT_MS = 36000;
    private static final boolean DO_UNICAST = false;
    public static final int EVENT_LINKADDRESS_CONFIGURED = 196617;
    private static final int FIRST_TIMEOUT_MS = 500;
    private static final int MAX_TIMEOUT_MS = 128000;
    private static final boolean MSG_DBG = false;
    private static final boolean PACKET_DBG = false;
    private static final int PRIVATE_BASE = 196708;
    private static final int PUBLIC_BASE = 196608;
    private static final int SECONDS = 1000;
    private static final boolean STATE_DBG = false;
    private static final String TAG = "MtkDhcpClient";
    private State mConfiguringInterfaceState;
    private final Context mContext;
    private final StateMachine mController;
    private int mDADResult;
    private State mDhcpBoundState;
    private State mDhcpHaveLeaseState;
    private State mDhcpInitRebootState;
    private State mDhcpInitState;
    private DhcpResults mDhcpLease;
    private long mDhcpLeaseExpiry;
    private State mDhcpRebindingState;
    private State mDhcpRebootingState;
    private State mDhcpRenewingState;
    private State mDhcpRequestingState;
    private State mDhcpSelectingState;
    private State mDhcpState;
    private final WakeupMessage mExpiryAlarm;
    private int mGWDResult;
    private byte[] mHwAddr;
    private InterfaceParams mIface;
    private final String mIfaceName;
    private PacketSocketAddress mInterfaceBroadcastAddr;
    private boolean mIsAutoIpEnabled;
    private boolean mIsIpRecoverEnabled;
    private final WakeupMessage mKickAlarm;
    private long mLastBoundExitTime;
    private long mLastInitEnterTime;
    private final IpConnectivityLog mMetricsLog;
    private DhcpResults mOffer;
    private FileDescriptor mPacketSock;
    private DhcpResults mPastDhcpLease;
    private final Random mRandom;
    private final WakeupMessage mRebindAlarm;
    private ReceiveThread mReceiveThread;
    private boolean mRegisteredForPreDhcpNotification;
    private final WakeupMessage mRenewAlarm;
    private State mStoppedState;
    private final WakeupMessage mTimeoutAlarm;
    private int mTransactionId;
    private long mTransactionStartMillis;
    private FileDescriptor mUdpSock;
    private State mWaitBeforeInitRebootState;
    private State mWaitBeforeRenewalState;
    private State mWaitBeforeStartState;
    private static final boolean DBG = true;
    private static final boolean SEN_DBG = Build.IS_USER ^ DBG;
    private static final Class[] sMessageClasses = {DhcpClient.class};
    private static final SparseArray<String> sMessageNames = MessageUtils.findMessageNames(sMessageClasses);
    static final byte[] REQUESTED_PARAMS = {1, 3, 6, 15, 26, 28, 51, 58, 59, 43};

    private WakeupMessage makeWakeupMessage(String str, int i) {
        return new WakeupMessage(this.mContext, getHandler(), DhcpClient.class.getSimpleName() + "." + this.mIfaceName + "." + str, i);
    }

    private MtkDhcpClient(Context context, StateMachine stateMachine, String str) {
        super(TAG, stateMachine.getHandler());
        this.mMetricsLog = new IpConnectivityLog();
        this.mIsAutoIpEnabled = false;
        this.mIsIpRecoverEnabled = false;
        this.mDADResult = 0;
        this.mGWDResult = 0;
        this.mStoppedState = new StoppedState();
        this.mDhcpState = new DhcpState();
        this.mDhcpInitState = new DhcpInitState();
        this.mDhcpSelectingState = new DhcpSelectingState();
        this.mDhcpRequestingState = new DhcpRequestingState();
        this.mDhcpHaveLeaseState = new DhcpHaveLeaseState();
        this.mConfiguringInterfaceState = new ConfiguringInterfaceState();
        this.mDhcpBoundState = new DhcpBoundState();
        this.mDhcpRenewingState = new DhcpRenewingState();
        this.mDhcpRebindingState = new DhcpRebindingState();
        this.mDhcpInitRebootState = new DhcpInitRebootState();
        this.mDhcpRebootingState = new DhcpRebootingState();
        this.mWaitBeforeStartState = new WaitBeforeStartState(this.mDhcpInitState);
        this.mWaitBeforeRenewalState = new WaitBeforeRenewalState(this.mDhcpRenewingState);
        this.mWaitBeforeInitRebootState = new WaitBeforeInitRebootState(this.mDhcpInitRebootState);
        this.mContext = context;
        this.mController = stateMachine;
        this.mIfaceName = str;
        addState(this.mStoppedState);
        addState(this.mDhcpState);
        addState(this.mDhcpInitState, this.mDhcpState);
        addState(this.mWaitBeforeStartState, this.mDhcpState);
        addState(this.mDhcpSelectingState, this.mDhcpState);
        addState(this.mDhcpRequestingState, this.mDhcpState);
        addState(this.mDhcpHaveLeaseState, this.mDhcpState);
        addState(this.mConfiguringInterfaceState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpBoundState, this.mDhcpHaveLeaseState);
        addState(this.mWaitBeforeRenewalState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpRenewingState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpRebindingState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpInitRebootState, this.mDhcpState);
        addState(this.mDhcpRebootingState, this.mDhcpState);
        addState(this.mWaitBeforeInitRebootState, this.mDhcpState);
        setInitialState(this.mStoppedState);
        this.mRandom = new Random();
        this.mKickAlarm = makeWakeupMessage("KICK", CMD_KICK);
        this.mTimeoutAlarm = makeWakeupMessage("TIMEOUT", CMD_TIMEOUT);
        this.mRenewAlarm = makeWakeupMessage("RENEW", CMD_RENEW_DHCP);
        this.mRebindAlarm = makeWakeupMessage("REBIND", CMD_REBIND_DHCP);
        this.mExpiryAlarm = makeWakeupMessage("EXPIRY", CMD_EXPIRE_DHCP);
        if (sDhcpResultMap == null) {
            sDhcpResultMap = new HashMap();
        }
    }

    public void registerForPreDhcpNotification() {
        this.mRegisteredForPreDhcpNotification = DBG;
    }

    public static DhcpClient makeDhcpClient(Context context, StateMachine stateMachine, InterfaceParams interfaceParams) {
        MtkDhcpClient mtkDhcpClient = new MtkDhcpClient(context, stateMachine, interfaceParams.name);
        mtkDhcpClient.mIface = interfaceParams;
        mtkDhcpClient.start();
        return mtkDhcpClient;
    }

    private boolean initInterface() {
        if (this.mIface == null) {
            this.mIface = InterfaceParams.getByName(this.mIfaceName);
        }
        if (this.mIface == null) {
            Log.e(TAG, "Can't determine InterfaceParams for " + this.mIfaceName);
            return false;
        }
        this.mHwAddr = this.mIface.macAddr.toByteArray();
        this.mInterfaceBroadcastAddr = new PacketSocketAddress(this.mIface.index, DhcpPacket.ETHER_BROADCAST);
        this.mInterfaceBroadcastAddr.sll_protocol = (short) 2048;
        return DBG;
    }

    private void startNewTransaction() {
        this.mTransactionId = this.mRandom.nextInt();
        this.mTransactionStartMillis = SystemClock.elapsedRealtime();
    }

    private boolean initSockets() {
        if (initPacketSocket() && initUdpSocket()) {
            return DBG;
        }
        return false;
    }

    private boolean initPacketSocket() {
        try {
            this.mPacketSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IP);
            Os.bind(this.mPacketSock, new PacketSocketAddress((short) OsConstants.ETH_P_IP, this.mIface.index));
            NetworkUtils.attachDhcpFilter(this.mPacketSock);
            return DBG;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating packet socket", e);
            if (this.mPacketSock != null) {
                closeQuietly(this.mPacketSock);
                return false;
            }
            return false;
        }
    }

    private boolean initUdpSocket() {
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-192);
        try {
            this.mUdpSock = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, 1);
            Os.setsockoptIfreq(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, this.mIfaceName);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, 1);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 0);
            Os.bind(this.mUdpSock, Inet4Address.ANY, 68);
            NetworkUtils.protectFromVpn(this.mUdpSock);
            return DBG;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating UDP socket", e);
            if (this.mUdpSock != null) {
                closeQuietly(this.mUdpSock);
            }
            return false;
        } finally {
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        }
    }

    private boolean connectUdpSock(Inet4Address inet4Address) {
        try {
            Os.connect(this.mUdpSock, inet4Address, 67);
            return DBG;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error connecting UDP socket", e);
            return false;
        }
    }

    private static void closeQuietly(FileDescriptor fileDescriptor) {
        try {
            IoBridge.closeAndSignalBlockedThreads(fileDescriptor);
        } catch (IOException e) {
        }
    }

    private void closeSockets() {
        closeQuietly(this.mUdpSock);
        closeQuietly(this.mPacketSock);
    }

    class ReceiveThread extends Thread {
        private final byte[] mPacket = new byte[1500];
        private volatile boolean mStopped = false;

        ReceiveThread() {
        }

        public void halt() {
            this.mStopped = MtkDhcpClient.DBG;
            MtkDhcpClient.this.closeSockets();
        }

        @Override
        public void run() {
            Log.d(MtkDhcpClient.TAG, "Receive thread started");
            while (!this.mStopped) {
                try {
                    DhcpPacket dhcpPacketDecodeFullPacket = DhcpPacket.decodeFullPacket(this.mPacket, Os.read(MtkDhcpClient.this.mPacketSock, this.mPacket, 0, this.mPacket.length), 0);
                    Log.d(MtkDhcpClient.TAG, "Received packet: " + dhcpPacketDecodeFullPacket);
                    MtkDhcpClient.this.sendMessage(MtkDhcpClient.CMD_RECEIVED_PACKET, dhcpPacketDecodeFullPacket);
                } catch (ErrnoException | IOException e) {
                    if (!this.mStopped) {
                        Log.e(MtkDhcpClient.TAG, "Read error", e);
                        MtkDhcpClient.this.logError(DhcpErrorEvent.RECEIVE_ERROR);
                    }
                } catch (DhcpPacket.ParseException e2) {
                    Log.e(MtkDhcpClient.TAG, "Can't parse packet: " + e2.getMessage());
                    if (e2.errorCode == DhcpErrorEvent.DHCP_NO_COOKIE) {
                        EventLog.writeEvent(1397638484, "31850211", -1, DhcpPacket.ParseException.class.getName());
                    }
                    MtkDhcpClient.this.logError(e2.errorCode);
                }
            }
            Log.d(MtkDhcpClient.TAG, "Receive thread stopped");
        }
    }

    private short getSecs() {
        return (short) ((SystemClock.elapsedRealtime() - this.mTransactionStartMillis) / 1000);
    }

    private boolean transmitPacket(ByteBuffer byteBuffer, String str, int i, Inet4Address inet4Address) {
        try {
            if (i == 0) {
                Log.d(TAG, "Broadcasting " + str);
                Os.sendto(this.mPacketSock, byteBuffer.array(), 0, byteBuffer.limit(), 0, this.mInterfaceBroadcastAddr);
            } else if (i == 2 && inet4Address.equals(DhcpPacket.INADDR_BROADCAST)) {
                Log.d(TAG, "Broadcasting " + str);
                Os.sendto(this.mUdpSock, byteBuffer, 0, inet4Address, 67);
            } else {
                Log.d(TAG, String.format("Unicasting %s to %s", str, Os.getpeername(this.mUdpSock)));
                Os.write(this.mUdpSock, byteBuffer);
            }
            return DBG;
        } catch (ErrnoException | IOException e) {
            Log.e(TAG, "Can't send packet: ", e);
            return false;
        }
    }

    private boolean sendDiscoverPacket() {
        return transmitPacket(DhcpPacket.buildDiscoverPacket(0, this.mTransactionId, getSecs(), this.mHwAddr, false, REQUESTED_PARAMS), "DHCPDISCOVER", 0, DhcpPacket.INADDR_BROADCAST);
    }

    private boolean sendRequestPacket(Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4) {
        int i = DhcpPacket.INADDR_ANY.equals(inet4Address) ? 0 : 2;
        return transmitPacket(DhcpPacket.buildRequestPacket(i, this.mTransactionId, getSecs(), inet4Address, false, this.mHwAddr, inet4Address2, inet4Address3, REQUESTED_PARAMS, null), "DHCPREQUEST ciaddr=" + inet4Address.getHostAddress() + " request=" + inet4Address2.getHostAddress() + " serverid=" + (inet4Address3 != null ? inet4Address3.getHostAddress() : null), i, inet4Address4);
    }

    private void scheduleLeaseTimers() {
        if (this.mDhcpLeaseExpiry == 0) {
            Log.d(TAG, "Infinite lease, no timer scheduling needed");
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j = this.mDhcpLeaseExpiry - jElapsedRealtime;
        long j2 = j / 2;
        long j3 = (7 * j) / 8;
        this.mRenewAlarm.schedule(jElapsedRealtime + j2);
        this.mRebindAlarm.schedule(jElapsedRealtime + j3);
        this.mExpiryAlarm.schedule(jElapsedRealtime + j);
        Log.d(TAG, "Scheduling renewal in " + (j2 / 1000) + "s");
        Log.d(TAG, "Scheduling rebind in " + (j3 / 1000) + "s");
        Log.d(TAG, "Scheduling expiry in " + (j / 1000) + "s");
    }

    private void notifySuccess() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 1, 0, new DhcpResults(this.mDhcpLease));
    }

    private void notifyFailure() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 2, 0, (Object) null);
    }

    private void acceptDhcpResults(DhcpResults dhcpResults, String str) {
        this.mDhcpLease = dhcpResults;
        this.mOffer = null;
        Log.d(TAG, str + " lease: " + this.mDhcpLease);
        notifySuccess();
    }

    private void clearDhcpState() {
        this.mDhcpLease = null;
        this.mDhcpLeaseExpiry = 0L;
        this.mOffer = null;
    }

    private WifiConfiguration getCurrentWifiConfigurationWithTimeout() {
        Future futureSubmit = Executors.newCachedThreadPool().submit(new Callable<Object>() {
            @Override
            public Object call() {
                return MtkDhcpClient.this.getCurrentWifiConfiguration();
            }
        });
        try {
            try {
                return (WifiConfiguration) futureSubmit.get(3L, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e(TAG, "getCurrentWifiConfigurationWithTimeout:" + e);
                futureSubmit.cancel(DBG);
                return null;
            }
        } finally {
            futureSubmit.cancel(DBG);
        }
    }

    private WifiConfiguration getCurrentWifiConfiguration() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo == null) {
            Log.e(TAG, "wifi info is nul");
            return null;
        }
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        int size = configuredNetworks.size();
        for (int i = 0; i < size; i++) {
            if (configuredNetworks.get(i).networkId == connectionInfo.getNetworkId()) {
                return configuredNetworks.get(i);
            }
        }
        return null;
    }

    public void doQuit() {
        Log.d(TAG, "doQuit");
        quit();
    }

    protected void onQuitting() {
        Log.d(TAG, "onQuitting");
        this.mController.sendMessage(CMD_ON_QUIT);
    }

    abstract class LoggingState extends State {
        private long mEnterTimeMs;

        LoggingState() {
        }

        public void enter() {
            this.mEnterTimeMs = SystemClock.elapsedRealtime();
        }

        public void exit() {
            MtkDhcpClient.this.logState(getName(), (int) (SystemClock.elapsedRealtime() - this.mEnterTimeMs));
        }

        private String messageName(int i) {
            return (String) MtkDhcpClient.sMessageNames.get(i, Integer.toString(i));
        }

        private String messageToString(Message message) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            StringBuilder sb = new StringBuilder(" ");
            TimeUtils.formatDuration(message.getWhen() - jUptimeMillis, sb);
            sb.append(" ");
            sb.append(messageName(message.what));
            sb.append(" ");
            sb.append(message.arg1);
            sb.append(" ");
            sb.append(message.arg2);
            sb.append(" ");
            sb.append(message.obj);
            return sb.toString();
        }

        public boolean processMessage(Message message) {
            return false;
        }

        public String getName() {
            return getClass().getSimpleName();
        }
    }

    abstract class WaitBeforeOtherState extends LoggingState {
        protected State mOtherState;

        WaitBeforeOtherState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcpClient.this.mController.sendMessage(MtkDhcpClient.CMD_PRE_DHCP_ACTION);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196614) {
                MtkDhcpClient.this.transitionTo(this.mOtherState);
                return MtkDhcpClient.DBG;
            }
            return false;
        }
    }

    class StoppedState extends State {
        String reqIP = null;
        String reqGW = null;
        String reqDNS = null;
        String reqDomain = null;
        String srvIP = null;
        String wifiConfigKey = null;

        StoppedState() {
        }

        public boolean processMessage(Message message) throws Throwable {
            if (message.what != 196609) {
                return false;
            }
            if ("wlan0".equals(MtkDhcpClient.this.mIfaceName)) {
                MtkDhcpClient.this.mIsIpRecoverEnabled = MtkDhcpClient.DBG;
                try {
                    WifiConfiguration currentWifiConfigurationWithTimeout = MtkDhcpClient.this.getCurrentWifiConfigurationWithTimeout();
                    checkPastLease();
                    checkIpRecovery(currentWifiConfigurationWithTimeout);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (MtkDhcpClient.SEN_DBG) {
                    Log.d(MtkDhcpClient.TAG, "IP recover:past lease after check:\n\t" + MtkDhcpClient.this.mPastDhcpLease);
                }
                if (SystemProperties.get("persist.vendor.net.dhcp.renew").equals("1") && MtkDhcpClient.this.mPastDhcpLease != null) {
                    if (MtkDhcpClient.this.mRegisteredForPreDhcpNotification) {
                        MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mWaitBeforeInitRebootState);
                    } else {
                        MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitRebootState);
                    }
                    return MtkDhcpClient.DBG;
                }
            } else {
                MtkDhcpClient.this.mIsIpRecoverEnabled = false;
            }
            if (MtkDhcpClient.this.mRegisteredForPreDhcpNotification) {
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mWaitBeforeStartState);
            } else {
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
            }
            return MtkDhcpClient.DBG;
        }

        private void checkPastLease() throws Throwable {
            if (MtkDhcpClient.this.mPastDhcpLease == null) {
                getLeaseFromFile();
                if (this.reqIP == null || this.reqGW == null || this.reqDNS == null || this.srvIP == null) {
                    if (MtkDhcpClient.SEN_DBG) {
                        Log.e(MtkDhcpClient.TAG, "checkPastLease(): past dhcp lease was not valid, request IP = " + this.reqIP + ", request Gateway = " + this.reqGW + ", request DNS = " + this.reqDNS + ", server IP = " + this.srvIP);
                        return;
                    }
                    Log.e(MtkDhcpClient.TAG, "checkPastLease(): past dhcp lease was not valid !");
                    return;
                }
                DhcpResults dhcpResults = new DhcpResults();
                try {
                    dhcpResults.ipAddress = new LinkAddress(this.reqIP);
                    dhcpResults.gateway = InetAddress.getByName(this.reqGW);
                    dhcpResults.dnsServers.add((Inet4Address) InetAddress.getByName(this.reqDNS));
                    dhcpResults.domains = this.reqDomain;
                    dhcpResults.serverAddress = (Inet4Address) InetAddress.getByName(this.srvIP);
                } catch (Exception e) {
                    Log.e(MtkDhcpClient.TAG, "checkPastLease(): past dhcp lease some IP was not valid, " + e);
                    dhcpResults = null;
                }
                if (dhcpResults != null && MtkDhcpClient.sDhcpResultMap != null && this.wifiConfigKey != null) {
                    if (MtkDhcpClient.SEN_DBG) {
                        Log.d(MtkDhcpClient.TAG, "IP recover: record put-->" + this.wifiConfigKey + " with " + dhcpResults);
                    } else {
                        Log.d(MtkDhcpClient.TAG, "IP recover: record put...");
                    }
                    MtkDhcpClient.sDhcpResultMap.put(this.wifiConfigKey, dhcpResults);
                }
            }
        }

        private void getLeaseFromFile() throws Throwable {
            Throwable th;
            BufferedReader bufferedReader;
            Exception e;
            String str;
            StringBuilder sb;
            ?? r1 = MtkDhcpClient.DHCP_LEASE_FILE;
            if (!new File(MtkDhcpClient.DHCP_LEASE_FILE).exists()) {
                Log.e(MtkDhcpClient.TAG, "getLeaseFromFile(): file not existed");
                return;
            }
            try {
                try {
                    bufferedReader = new BufferedReader(new FileReader(MtkDhcpClient.DHCP_LEASE_FILE));
                    try {
                        for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                            if (line.startsWith("IP")) {
                                String[] strArrSplit = line.split("=");
                                this.reqIP = strArrSplit.length != 2 ? null : strArrSplit[1];
                            } else if (line.startsWith("Gateway")) {
                                String[] strArrSplit2 = line.split("=");
                                this.reqGW = strArrSplit2.length != 2 ? null : strArrSplit2[1];
                            } else if (line.startsWith("DNS")) {
                                String[] strArrSplit3 = line.split("=");
                                this.reqDNS = strArrSplit3.length != 2 ? null : strArrSplit3[1];
                            } else if (line.startsWith("Domain")) {
                                String[] strArrSplit4 = line.split("=");
                                this.reqDomain = strArrSplit4.length != 2 ? null : strArrSplit4[1];
                            } else if (line.startsWith("Server")) {
                                String[] strArrSplit5 = line.split("=");
                                this.srvIP = strArrSplit5.length != 2 ? null : strArrSplit5[1];
                            } else if (line.startsWith("WifiConfigKey")) {
                                String[] strArrSplit6 = line.split("=");
                                this.wifiConfigKey = strArrSplit6.length != 2 ? null : strArrSplit6[1];
                            }
                        }
                        try {
                            bufferedReader.close();
                        } catch (IOException e2) {
                            e = e2;
                            str = MtkDhcpClient.TAG;
                            sb = new StringBuilder();
                            sb.append("getLeaseFromFile()-02: ");
                            sb.append(e);
                            Log.e(str, sb.toString());
                        }
                    } catch (Exception e3) {
                        e = e3;
                        Log.e(MtkDhcpClient.TAG, "getLeaseFromFile()-01: " + e);
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e4) {
                                e = e4;
                                str = MtkDhcpClient.TAG;
                                sb = new StringBuilder();
                                sb.append("getLeaseFromFile()-02: ");
                                sb.append(e);
                                Log.e(str, sb.toString());
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (r1 != 0) {
                        try {
                            r1.close();
                        } catch (IOException e5) {
                            Log.e(MtkDhcpClient.TAG, "getLeaseFromFile()-02: " + e5);
                        }
                    }
                    throw th;
                }
            } catch (Exception e6) {
                bufferedReader = null;
                e = e6;
            } catch (Throwable th3) {
                r1 = 0;
                th = th3;
                if (r1 != 0) {
                }
                throw th;
            }
        }

        private void checkIpRecovery(WifiConfiguration wifiConfiguration) {
            if (wifiConfiguration != null) {
                if (MtkDhcpClient.sDhcpResultMap == null) {
                    Log.e(MtkDhcpClient.TAG, "sDhcpResultMap is null");
                    return;
                }
                String strConfigKey = wifiConfiguration.configKey();
                DhcpResults dhcpResults = (DhcpResults) MtkDhcpClient.sDhcpResultMap.get(strConfigKey);
                if (MtkDhcpClient.SEN_DBG) {
                    Log.d(MtkDhcpClient.TAG, "IP recover(" + MtkDhcpClient.sDhcpResultMap.size() + ") get DhcpResult for configKey-->" + strConfigKey + ", record-->" + dhcpResults);
                } else {
                    Log.d(MtkDhcpClient.TAG, "IP recover(" + MtkDhcpClient.sDhcpResultMap.size() + ")");
                }
                if (dhcpResults != null) {
                    MtkDhcpClient.this.mPastDhcpLease = dhcpResults;
                }
            }
        }
    }

    class WaitBeforeStartState extends WaitBeforeOtherState {
        public WaitBeforeStartState(State state) {
            super();
            this.mOtherState = state;
        }
    }

    class WaitBeforeRenewalState extends WaitBeforeOtherState {
        public WaitBeforeRenewalState(State state) {
            super();
            this.mOtherState = state;
        }
    }

    class WaitBeforeInitRebootState extends WaitBeforeOtherState {
        public WaitBeforeInitRebootState(State state) {
            super();
            this.mOtherState = state;
        }
    }

    class DhcpState extends State {
        DhcpState() {
        }

        public void enter() {
            MtkDhcpClient.this.clearDhcpState();
            if (!MtkDhcpClient.this.initInterface() || !MtkDhcpClient.this.initSockets()) {
                MtkDhcpClient.this.notifyFailure();
                MtkDhcpClient.this.sendMessage(MtkDhcpClient.CMD_STOP_DHCP);
            } else {
                MtkDhcpClient.this.mReceiveThread = MtkDhcpClient.this.new ReceiveThread();
                MtkDhcpClient.this.mReceiveThread.start();
            }
        }

        public void exit() {
            if (MtkDhcpClient.this.mReceiveThread != null) {
                MtkDhcpClient.this.mReceiveThread.halt();
                MtkDhcpClient.this.mReceiveThread = null;
            }
            MtkDhcpClient.this.clearDhcpState();
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196610) {
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mStoppedState);
                return MtkDhcpClient.DBG;
            }
            return false;
        }
    }

    public boolean isValidPacket(DhcpPacket dhcpPacket) {
        int transactionId = dhcpPacket.getTransactionId();
        if (transactionId != this.mTransactionId) {
            Log.d(TAG, "Unexpected transaction ID " + transactionId + ", expected " + this.mTransactionId);
            return false;
        }
        if (!Arrays.equals(dhcpPacket.getClientMac(), this.mHwAddr)) {
            Log.d(TAG, "MAC addr mismatch: got " + HexDump.toHexString(dhcpPacket.getClientMac()) + ", expected " + HexDump.toHexString(this.mHwAddr));
            return false;
        }
        return DBG;
    }

    public void setDhcpLeaseExpiry(DhcpPacket dhcpPacket) {
        long leaseTimeMillis = dhcpPacket.getLeaseTimeMillis();
        this.mDhcpLeaseExpiry = leaseTimeMillis > 0 ? SystemClock.elapsedRealtime() + leaseTimeMillis : 0L;
    }

    abstract class PacketRetransmittingState extends LoggingState {
        protected int mTimeout;
        protected int mTimer;

        protected abstract void receivePacket(DhcpPacket dhcpPacket);

        protected abstract boolean sendPacket();

        PacketRetransmittingState() {
            super();
            this.mTimeout = 0;
        }

        @Override
        public void enter() {
            super.enter();
            initTimer();
            maybeInitTimeout();
            MtkDhcpClient.this.sendMessage(MtkDhcpClient.CMD_KICK);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case MtkDhcpClient.CMD_KICK:
                    sendPacket();
                    scheduleKick();
                    break;
                case MtkDhcpClient.CMD_RECEIVED_PACKET:
                    receivePacket((DhcpPacket) message.obj);
                    break;
                case MtkDhcpClient.CMD_TIMEOUT:
                    timeout();
                    break;
            }
            return MtkDhcpClient.DBG;
        }

        @Override
        public void exit() {
            super.exit();
            MtkDhcpClient.this.mKickAlarm.cancel();
            MtkDhcpClient.this.mTimeoutAlarm.cancel();
        }

        protected void timeout() {
        }

        protected void initTimer() {
            this.mTimer = MtkDhcpClient.FIRST_TIMEOUT_MS;
        }

        protected int jitterTimer(int i) {
            int i2 = i / 10;
            return i + (MtkDhcpClient.this.mRandom.nextInt(2 * i2) - i2);
        }

        protected void scheduleKick() {
            MtkDhcpClient.this.mKickAlarm.schedule(SystemClock.elapsedRealtime() + ((long) jitterTimer(this.mTimer)));
            this.mTimer *= 2;
            if (this.mTimer > MtkDhcpClient.MAX_TIMEOUT_MS) {
                this.mTimer = MtkDhcpClient.MAX_TIMEOUT_MS;
            }
        }

        protected void maybeInitTimeout() {
            if (this.mTimeout > 0) {
                long jElapsedRealtime = SystemClock.elapsedRealtime() + ((long) this.mTimeout);
                Log.d(MtkDhcpClient.TAG, "maybeInitTimeout:" + this.mTimeout);
                MtkDhcpClient.this.mTimeoutAlarm.schedule(jElapsedRealtime);
            }
        }
    }

    class DhcpInitState extends PacketRetransmittingState {
        public DhcpInitState() {
            super();
            this.mTimeout = 12000;
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcpClient.this.startNewTransaction();
            MtkDhcpClient.this.mLastInitEnterTime = SystemClock.elapsedRealtime();
        }

        @Override
        protected boolean sendPacket() {
            return MtkDhcpClient.this.sendDiscoverPacket();
        }

        @Override
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (MtkDhcpClient.this.isValidPacket(dhcpPacket) && (dhcpPacket instanceof DhcpOfferPacket)) {
                MtkDhcpClient.this.mOffer = dhcpPacket.toDhcpResults();
                if (MtkDhcpClient.this.mOffer != null) {
                    Log.d(MtkDhcpClient.TAG, "Got pending lease: " + MtkDhcpClient.this.mOffer);
                    MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpRequestingState);
                }
            }
        }

        @Override
        protected void timeout() throws Throwable {
            if ("bt-pan".equals(MtkDhcpClient.this.mIfaceName)) {
                MtkDhcpClient.this.performArpRequestForBt();
            } else if (MtkDhcpClient.this.doIpRecover()) {
                MtkDhcpClient.this.sendMessageDelayed(MtkDhcpClient.CMD_IP_RECOVER, 1700L);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == MtkDhcpClient.CMD_IP_RECOVER) {
                if (MtkDhcpClient.this.mDADResult != 2 || MtkDhcpClient.this.mGWDResult != 1) {
                    if (MtkDhcpClient.this.mDADResult == 1 || MtkDhcpClient.this.mGWDResult == 2) {
                        Log.d(MtkDhcpClient.TAG, "ip recover: bad result!");
                    } else {
                        Log.d(MtkDhcpClient.TAG, "ip recover: no full result yet");
                        MtkDhcpClient.this.sendMessageDelayed(MtkDhcpClient.CMD_IP_RECOVER, 1700L);
                    }
                } else {
                    Log.d(MtkDhcpClient.TAG, "ip recover: good result!");
                    MtkDhcpClient.this.acceptDhcpResults(MtkDhcpClient.this.mPastDhcpLease, "Confirmed");
                    MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mConfiguringInterfaceState);
                }
                return MtkDhcpClient.DBG;
            }
            return false;
        }

        @Override
        protected void scheduleKick() {
            long jJitterTimer = jitterTimer(this.mTimer);
            Log.d(MtkDhcpClient.TAG, "scheduleKick()@DhcpInitState timeout=" + jJitterTimer);
            MtkDhcpClient.this.sendMessageDelayed(MtkDhcpClient.CMD_KICK, jJitterTimer);
            this.mTimer = this.mTimer * 2;
            if (this.mTimer > MtkDhcpClient.MAX_TIMEOUT_MS) {
                this.mTimer = MtkDhcpClient.MAX_TIMEOUT_MS;
            }
        }
    }

    class DhcpSelectingState extends LoggingState {
        DhcpSelectingState() {
            super();
        }
    }

    class DhcpRequestingState extends PacketRetransmittingState {
        public DhcpRequestingState() {
            super();
            this.mTimeout = 18000;
        }

        @Override
        protected boolean sendPacket() {
            return MtkDhcpClient.this.sendRequestPacket(DhcpPacket.INADDR_ANY, (Inet4Address) MtkDhcpClient.this.mOffer.ipAddress.getAddress(), MtkDhcpClient.this.mOffer.serverAddress, DhcpPacket.INADDR_BROADCAST);
        }

        @Override
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (MtkDhcpClient.this.isValidPacket(dhcpPacket)) {
                if (!(dhcpPacket instanceof DhcpAckPacket)) {
                    if (dhcpPacket instanceof DhcpNakPacket) {
                        Log.d(MtkDhcpClient.TAG, "Received NAK, returning to INIT");
                        MtkDhcpClient.this.mOffer = null;
                        MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
                        return;
                    }
                    return;
                }
                DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                if (dhcpResults != null) {
                    MtkDhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                    MtkDhcpClient.this.acceptDhcpResults(dhcpResults, "Confirmed");
                    MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mConfiguringInterfaceState);
                }
            }
        }

        @Override
        protected void timeout() {
            if (!MtkDhcpClient.this.mIsAutoIpEnabled || !MtkDhcpClient.this.performAutoIP()) {
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
                return;
            }
            MtkDhcpClient.this.mOffer = null;
            MtkDhcpClient.this.notifySuccess();
            MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mConfiguringInterfaceState);
        }
    }

    class DhcpHaveLeaseState extends State {
        DhcpHaveLeaseState() {
        }

        public void enter() throws Throwable {
            if (MtkDhcpClient.this.mIsIpRecoverEnabled) {
                try {
                    String strSaveDhcpResult = saveDhcpResult(MtkDhcpClient.this.getCurrentWifiConfigurationWithTimeout());
                    if (strSaveDhcpResult != null) {
                        putLeaseToFile(strSaveDhcpResult);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void putLeaseToFile(String str) throws Throwable {
            BufferedWriter bufferedWriter;
            String str2;
            StringBuilder sb;
            BufferedWriter bufferedWriter2 = null;
            try {
                try {
                    bufferedWriter = new BufferedWriter(new FileWriter(MtkDhcpClient.DHCP_LEASE_FILE));
                } catch (Exception e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
                bufferedWriter = bufferedWriter2;
            }
            try {
                if (MtkDhcpClient.this.mDhcpLease.ipAddress != null && MtkDhcpClient.this.mDhcpLease.ipAddress.getAddress() != null) {
                    bufferedWriter.write("IP=" + MtkDhcpClient.this.mDhcpLease.ipAddress + "\n");
                }
                if (MtkDhcpClient.this.mDhcpLease.gateway != null && MtkDhcpClient.this.mDhcpLease.gateway.getAddress() != null) {
                    bufferedWriter.write("Gateway=" + MtkDhcpClient.this.mDhcpLease.gateway.getHostAddress() + "\n");
                }
                if (MtkDhcpClient.this.mDhcpLease.dnsServers != null) {
                    Iterator it = MtkDhcpClient.this.mDhcpLease.dnsServers.iterator();
                    if (it.hasNext()) {
                        bufferedWriter.write("DNS=" + ((InetAddress) it.next()).getHostAddress() + "\n");
                    }
                }
                if (MtkDhcpClient.this.mDhcpLease.domains != null) {
                    bufferedWriter.write("Domain=" + MtkDhcpClient.this.mDhcpLease.domains + "\n");
                }
                if (MtkDhcpClient.this.mDhcpLease.serverAddress != null) {
                    bufferedWriter.write("Server=" + MtkDhcpClient.this.mDhcpLease.serverAddress.getHostAddress() + "\n");
                }
                if (str != null) {
                    bufferedWriter.write("WifiConfigKey=" + str + "\n");
                }
                try {
                    bufferedWriter.close();
                } catch (IOException e2) {
                    e = e2;
                    str2 = MtkDhcpClient.TAG;
                    sb = new StringBuilder();
                    sb.append("putLeaseToFile()-02: ");
                    sb.append(e);
                    Log.e(str2, sb.toString());
                }
            } catch (Exception e3) {
                e = e3;
                bufferedWriter2 = bufferedWriter;
                Log.e(MtkDhcpClient.TAG, "putLeaseToFile()-01: " + e);
                if (bufferedWriter2 != null) {
                    try {
                        bufferedWriter2.close();
                    } catch (IOException e4) {
                        e = e4;
                        str2 = MtkDhcpClient.TAG;
                        sb = new StringBuilder();
                        sb.append("putLeaseToFile()-02: ");
                        sb.append(e);
                        Log.e(str2, sb.toString());
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e5) {
                        Log.e(MtkDhcpClient.TAG, "putLeaseToFile()-02: " + e5);
                    }
                }
                throw th;
            }
        }

        private String saveDhcpResult(WifiConfiguration wifiConfiguration) {
            if (wifiConfiguration != null) {
                if (MtkDhcpClient.sDhcpResultMap == null) {
                    Log.e(MtkDhcpClient.TAG, "sDhcpResultMap is null");
                    return null;
                }
                if (wifiConfiguration.allowedKeyManagement.get(0) && wifiConfiguration.wepTxKeyIndex >= 0 && wifiConfiguration.wepTxKeyIndex < wifiConfiguration.wepKeys.length && wifiConfiguration.wepKeys[wifiConfiguration.wepTxKeyIndex] != null) {
                    Log.d(MtkDhcpClient.TAG, "Skip SECURITY_WEP");
                    return null;
                }
                String strConfigKey = wifiConfiguration.configKey();
                if (MtkDhcpClient.SEN_DBG) {
                    Log.d(MtkDhcpClient.TAG, "IP recover:record put " + strConfigKey + " with " + MtkDhcpClient.this.mDhcpLease);
                }
                MtkDhcpClient.sDhcpResultMap.put(strConfigKey, MtkDhcpClient.this.mDhcpLease);
                return strConfigKey;
            }
            Log.e(MtkDhcpClient.TAG, "wifiCfg is null");
            return null;
        }

        public boolean processMessage(Message message) {
            if (message.what == MtkDhcpClient.CMD_EXPIRE_DHCP) {
                Log.d(MtkDhcpClient.TAG, "Lease expired!");
                MtkDhcpClient.this.notifyFailure();
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
                return MtkDhcpClient.DBG;
            }
            return false;
        }

        public void exit() {
            MtkDhcpClient.this.mRenewAlarm.cancel();
            MtkDhcpClient.this.mRebindAlarm.cancel();
            MtkDhcpClient.this.mExpiryAlarm.cancel();
            MtkDhcpClient.this.clearDhcpState();
            MtkDhcpClient.this.mController.sendMessage(MtkDhcpClient.CMD_CLEAR_LINKADDRESS);
        }
    }

    class ConfiguringInterfaceState extends LoggingState {
        ConfiguringInterfaceState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcpClient.this.mController.sendMessage(MtkDhcpClient.CMD_CONFIGURE_LINKADDRESS, MtkDhcpClient.this.mDhcpLease.ipAddress);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196617) {
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpBoundState);
                return MtkDhcpClient.DBG;
            }
            return false;
        }
    }

    class DhcpBoundState extends LoggingState {
        DhcpBoundState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            if (MtkDhcpClient.this.mDhcpLease.serverAddress != null && !MtkDhcpClient.this.connectUdpSock(MtkDhcpClient.this.mDhcpLease.serverAddress)) {
                MtkDhcpClient.this.notifyFailure();
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mStoppedState);
            }
            MtkDhcpClient.this.scheduleLeaseTimers();
            logTimeToBoundState();
        }

        @Override
        public void exit() {
            super.exit();
            MtkDhcpClient.this.mLastBoundExitTime = SystemClock.elapsedRealtime();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == MtkDhcpClient.CMD_RENEW_DHCP) {
                if (MtkDhcpClient.this.mRegisteredForPreDhcpNotification) {
                    MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mWaitBeforeRenewalState);
                    return MtkDhcpClient.DBG;
                }
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpRenewingState);
                return MtkDhcpClient.DBG;
            }
            return false;
        }

        private void logTimeToBoundState() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (MtkDhcpClient.this.mLastBoundExitTime > MtkDhcpClient.this.mLastInitEnterTime) {
                MtkDhcpClient.this.logState("RenewingBoundState", (int) (jElapsedRealtime - MtkDhcpClient.this.mLastBoundExitTime));
            } else {
                MtkDhcpClient.this.logState("InitialBoundState", (int) (jElapsedRealtime - MtkDhcpClient.this.mLastInitEnterTime));
            }
        }
    }

    abstract class DhcpReacquiringState extends PacketRetransmittingState {
        protected String mLeaseMsg;

        protected abstract Inet4Address packetDestination();

        DhcpReacquiringState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcpClient.this.startNewTransaction();
        }

        @Override
        protected boolean sendPacket() {
            return MtkDhcpClient.this.sendRequestPacket((Inet4Address) MtkDhcpClient.this.mDhcpLease.ipAddress.getAddress(), DhcpPacket.INADDR_ANY, null, packetDestination());
        }

        @Override
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (MtkDhcpClient.this.isValidPacket(dhcpPacket)) {
                if (!(dhcpPacket instanceof DhcpAckPacket)) {
                    if (dhcpPacket instanceof DhcpNakPacket) {
                        Log.d(MtkDhcpClient.TAG, "Received NAK, returning to INIT");
                        MtkDhcpClient.this.notifyFailure();
                        MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
                        return;
                    }
                    return;
                }
                DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                if (dhcpResults != null) {
                    if (!MtkDhcpClient.this.mDhcpLease.ipAddress.equals(dhcpResults.ipAddress)) {
                        Log.d(MtkDhcpClient.TAG, "Renewed lease not for our current IP address!");
                        MtkDhcpClient.this.notifyFailure();
                        MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
                    }
                    MtkDhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                    MtkDhcpClient.this.acceptDhcpResults(dhcpResults, this.mLeaseMsg);
                    MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpBoundState);
                }
            }
        }
    }

    class DhcpRenewingState extends DhcpReacquiringState {
        public DhcpRenewingState() {
            super();
            this.mLeaseMsg = "Renewed";
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return MtkDhcpClient.DBG;
            }
            if (message.what == MtkDhcpClient.CMD_REBIND_DHCP) {
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpRebindingState);
                return MtkDhcpClient.DBG;
            }
            return false;
        }

        @Override
        protected Inet4Address packetDestination() {
            return MtkDhcpClient.this.mDhcpLease.serverAddress != null ? MtkDhcpClient.this.mDhcpLease.serverAddress : DhcpPacket.INADDR_BROADCAST;
        }
    }

    class DhcpRebindingState extends DhcpReacquiringState {
        public DhcpRebindingState() {
            super();
            this.mLeaseMsg = "Rebound";
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcpClient.closeQuietly(MtkDhcpClient.this.mUdpSock);
            if (!MtkDhcpClient.this.initUdpSocket()) {
                Log.e(MtkDhcpClient.TAG, "Failed to recreate UDP socket");
                MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
            }
        }

        @Override
        protected Inet4Address packetDestination() {
            return DhcpPacket.INADDR_BROADCAST;
        }
    }

    class DhcpInitRebootState extends DhcpReacquiringState {
        public DhcpInitRebootState() {
            super();
            this.mLeaseMsg = "Init-Reboot";
            this.mTimeout = 6000;
        }

        @Override
        public void enter() {
            super.enter();
            if (MtkDhcpClient.this.mPastDhcpLease != null) {
                MtkDhcpClient.this.mDhcpLease = MtkDhcpClient.this.mPastDhcpLease;
                MtkDhcpClient.this.mOffer = null;
                Log.d(MtkDhcpClient.TAG, "Configure mDhcpLease for DHCP init");
            }
        }

        @Override
        protected boolean sendPacket() {
            return MtkDhcpClient.this.sendRequestPacket(DhcpPacket.INADDR_ANY, (Inet4Address) MtkDhcpClient.this.mDhcpLease.ipAddress.getAddress(), null, DhcpPacket.INADDR_BROADCAST);
        }

        @Override
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (MtkDhcpClient.this.isValidPacket(dhcpPacket)) {
                if (!(dhcpPacket instanceof DhcpAckPacket)) {
                    if (dhcpPacket instanceof DhcpNakPacket) {
                        Log.d(MtkDhcpClient.TAG, "Received NAK, returning to INIT");
                        MtkDhcpClient.this.mPastDhcpLease = null;
                        MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
                        return;
                    }
                    return;
                }
                DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                if (dhcpResults != null) {
                    MtkDhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                    MtkDhcpClient.this.acceptDhcpResults(dhcpResults, this.mLeaseMsg);
                    MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mConfiguringInterfaceState);
                }
            }
        }

        @Override
        protected Inet4Address packetDestination() {
            return DhcpPacket.INADDR_BROADCAST;
        }

        @Override
        protected void timeout() {
            Log.d(MtkDhcpClient.TAG, "Failed to obtain IP @DhcpInitRebootState");
            MtkDhcpClient.this.transitionTo(MtkDhcpClient.this.mDhcpInitState);
        }

        @Override
        protected void scheduleKick() {
            long jJitterTimer = jitterTimer(this.mTimer);
            Log.d(MtkDhcpClient.TAG, "scheduleKick()@DhcpInitRebootState timeout=" + jJitterTimer);
            MtkDhcpClient.this.sendMessageDelayed(MtkDhcpClient.CMD_KICK, jJitterTimer);
            this.mTimer = this.mTimer * 2;
            if (this.mTimer > MtkDhcpClient.MAX_TIMEOUT_MS) {
                this.mTimer = MtkDhcpClient.MAX_TIMEOUT_MS;
            }
        }
    }

    class DhcpRebootingState extends LoggingState {
        DhcpRebootingState() {
            super();
        }
    }

    private void logError(int i) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpErrorEvent(i));
    }

    private void logState(String str, int i) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpClientEvent(str, i));
    }

    private String logDumpIpv4(int i, String str) {
        if (str == null) {
            return null;
        }
        String[] strArrSplit = str.split("\\.");
        if (strArrSplit.length != 4) {
            return str;
        }
        StringBuilder sb = new StringBuilder(16);
        for (int i2 = 4 - i; i2 < 4; i2++) {
            try {
                if (strArrSplit[i2].length() > 3) {
                    return str;
                }
                sb.append(Integer.parseInt(strArrSplit[i2]));
                if (i2 < 3) {
                    sb.append('K');
                }
            } catch (NumberFormatException e) {
                return str;
            }
        }
        return sb.toString();
    }

    private boolean setIpAddress(LinkAddress linkAddress) {
        InterfaceConfiguration interfaceConfiguration = new InterfaceConfiguration();
        interfaceConfiguration.setLinkAddress(linkAddress);
        try {
            INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management")).setInterfaceConfig(this.mIfaceName, interfaceConfiguration);
            return DBG;
        } catch (RemoteException | IllegalStateException e) {
            if (SEN_DBG) {
                Log.e(TAG, "setIpAddress(): configured IP address " + linkAddress + ": ", e);
                return false;
            }
            return false;
        }
    }

    private void performArpRequestForBt() throws Throwable {
        ArpPeer arpPeer;
        ArpPeer arpPeer2 = null;
        try {
            try {
                InetAddress byAddress = InetAddress.getByAddress(new byte[]{-87, -2, -128, -126});
                Log.d(TAG, "performArpRequestForBt() = oooKxxxK" + logDumpIpv4(2, byAddress.getHostAddress()));
                arpPeer = new ArpPeer(this.mIfaceName, Inet4Address.ANY, byAddress);
            } catch (ErrnoException | IllegalArgumentException | SocketException | UnknownHostException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            arpPeer.doArp(0);
            arpPeer.close();
        } catch (ErrnoException | IllegalArgumentException | SocketException | UnknownHostException e2) {
            e = e2;
            arpPeer2 = arpPeer;
            Log.e(TAG, "performArpRequestForBt(): meet " + e);
            if (arpPeer2 != null) {
                arpPeer2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            arpPeer2 = arpPeer;
            if (arpPeer2 != null) {
                arpPeer2.close();
            }
            throw th;
        }
    }

    private boolean performAutoIP() throws Throwable {
        ArpPeer arpPeer;
        Object e;
        InetAddress byAddress;
        Random random = new Random();
        byte[] bArr = {-87, -2, 10, 10};
        ArpPeer arpPeer2 = null;
        for (int i = 0; i < 5; i++) {
            bArr[2] = new Integer(random.nextInt(256)).byteValue();
            bArr[3] = new Integer(random.nextInt(254) + 1).byteValue();
            try {
                byAddress = InetAddress.getByAddress(bArr);
                Log.d(TAG, "performAutoIP(" + i + ") = oooKxxxK" + logDumpIpv4(2, byAddress.getHostAddress()));
                arpPeer = new ArpPeer(this.mIfaceName, Inet4Address.ANY, byAddress);
                try {
                    try {
                    } catch (Throwable th) {
                        th = th;
                        if (arpPeer != null) {
                            arpPeer.close();
                        }
                        throw th;
                    }
                } catch (ErrnoException | IllegalArgumentException | SocketException | UnknownHostException e2) {
                    e = e2;
                    Log.e(TAG, "performAutoIP(): meet " + e);
                    if (arpPeer != null) {
                    }
                    arpPeer2 = arpPeer;
                }
            } catch (ErrnoException | IllegalArgumentException | SocketException | UnknownHostException e3) {
                arpPeer = arpPeer2;
                e = e3;
            } catch (Throwable th2) {
                th = th2;
                arpPeer = arpPeer2;
            }
            if (arpPeer.doArp(5000) == null) {
                this.mDhcpLease = new DhcpResults();
                this.mDhcpLease.ipAddress = new LinkAddress(byAddress, 16);
                this.mDhcpLease.leaseDuration = -1;
                setIpAddress(this.mDhcpLease.ipAddress);
                Log.d(TAG, "performAutoIP(): done");
                arpPeer.close();
                return DBG;
            }
            Log.d(TAG, "performAutoIP(): DAD detected!!");
            arpPeer.close();
            arpPeer2 = arpPeer;
        }
        return false;
    }

    private boolean doIpRecover() {
        if (!this.mIsIpRecoverEnabled) {
            Log.d(TAG, "IP recover: it was disabled");
            return false;
        }
        if (this.mPastDhcpLease == null) {
            Log.d(TAG, "IP recover: mPastDhcpLease is empty");
            return false;
        }
        if (SEN_DBG) {
            Log.d(TAG, "IP recover: mPastDhcpLease = " + this.mPastDhcpLease);
        }
        Log.d(TAG, "IP recover: reCaculatedLeaseMillis = -1");
        this.mDhcpLeaseExpiry = 0L;
        Log.e(TAG, "IP recover: lease had been expired! configure to infinite lease");
        new Thread() {
            @Override
            public void run() throws Throwable {
                String str;
                StringBuilder sb;
                ArpPeer arpPeer;
                ArpPeer arpPeer2 = null;
                try {
                    try {
                        InetAddress address = MtkDhcpClient.this.mPastDhcpLease.ipAddress.getAddress();
                        Log.d(MtkDhcpClient.TAG, "IP recover: DAD arp address = #$%K" + MtkDhcpClient.this.logDumpIpv4(3, address.getHostAddress()));
                        MtkDhcpClient.this.mDADResult = 0;
                        arpPeer = new ArpPeer(MtkDhcpClient.this.mIfaceName, Inet4Address.ANY, address);
                    } catch (ErrnoException | IllegalArgumentException | SocketException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    byte[] bArrDoArp = arpPeer.doArp(5000);
                    arpPeer.close();
                    if (bArrDoArp == null) {
                        MtkDhcpClient.this.mDADResult = 2;
                    } else {
                        MtkDhcpClient.this.mDADResult = 1;
                    }
                    str = MtkDhcpClient.TAG;
                    sb = new StringBuilder();
                } catch (ErrnoException | IllegalArgumentException | SocketException e2) {
                    e = e2;
                    arpPeer2 = arpPeer;
                    Log.e(MtkDhcpClient.TAG, "IP recover: DAD err :" + e);
                    if (arpPeer2 != null) {
                        arpPeer2.close();
                    }
                    MtkDhcpClient.this.mDADResult = 2;
                    str = MtkDhcpClient.TAG;
                    sb = new StringBuilder();
                } catch (Throwable th2) {
                    th = th2;
                    arpPeer2 = arpPeer;
                    if (arpPeer2 != null) {
                        arpPeer2.close();
                    }
                    MtkDhcpClient.this.mDADResult = 2;
                    Log.d(MtkDhcpClient.TAG, "IP recover: DAD result = " + MtkDhcpClient.this.mDADResult);
                    throw th;
                }
                sb.append("IP recover: DAD result = ");
                sb.append(MtkDhcpClient.this.mDADResult);
                Log.d(str, sb.toString());
            }
        }.start();
        new Thread() {
            @Override
            public void run() throws Throwable {
                String str;
                StringBuilder sb;
                ArpPeer arpPeer;
                ArpPeer arpPeer2 = null;
                try {
                    try {
                        Thread.sleep(100L);
                        InetAddress inetAddress = MtkDhcpClient.this.mPastDhcpLease.gateway;
                        Log.d(MtkDhcpClient.TAG, "IP recover: GWD arp address = #$%K" + MtkDhcpClient.this.logDumpIpv4(3, inetAddress.getHostAddress()));
                        MtkDhcpClient.this.mGWDResult = 0;
                        arpPeer = new ArpPeer(MtkDhcpClient.this.mIfaceName, Inet4Address.ANY, inetAddress);
                    } catch (ErrnoException | IllegalArgumentException | InterruptedException | SocketException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    byte[] bArrDoArp = arpPeer.doArp(5000);
                    arpPeer.close();
                    if (bArrDoArp == null) {
                        MtkDhcpClient.this.mGWDResult = 2;
                    } else {
                        MtkDhcpClient.this.mGWDResult = 1;
                    }
                    str = MtkDhcpClient.TAG;
                    sb = new StringBuilder();
                } catch (ErrnoException | IllegalArgumentException | InterruptedException | SocketException e2) {
                    e = e2;
                    arpPeer2 = arpPeer;
                    Log.e(MtkDhcpClient.TAG, "IP recover: GWD err :" + e);
                    if (arpPeer2 != null) {
                        arpPeer2.close();
                    }
                    MtkDhcpClient.this.mGWDResult = 2;
                    str = MtkDhcpClient.TAG;
                    sb = new StringBuilder();
                } catch (Throwable th2) {
                    th = th2;
                    arpPeer2 = arpPeer;
                    if (arpPeer2 != null) {
                        arpPeer2.close();
                    }
                    MtkDhcpClient.this.mGWDResult = 2;
                    Log.d(MtkDhcpClient.TAG, "IP recover: GWD result = " + MtkDhcpClient.this.mGWDResult);
                    throw th;
                }
                sb.append("IP recover: GWD result = ");
                sb.append(MtkDhcpClient.this.mGWDResult);
                Log.d(str, sb.toString());
            }
        }.start();
        return DBG;
    }
}
