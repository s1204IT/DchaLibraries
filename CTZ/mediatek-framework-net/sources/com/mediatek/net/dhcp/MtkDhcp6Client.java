package com.mediatek.net.dhcp;

import android.app.AlarmManager;
import android.content.Context;
import android.net.DhcpResults;
import android.net.INetd;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.net.TrafficStats;
import android.net.util.NetdService;
import android.os.Build;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Random;
import java.util.TimeZone;
import libcore.io.IoBridge;

public class MtkDhcp6Client extends StateMachine {
    private static final int BASE = 196708;
    public static final int CMD_CLEAR_LINKADDRESS = 196815;
    public static final int CMD_CONFIGURE_DNSV6 = 196816;
    private static final int CMD_IPV6_PREFIX = 196714;
    private static final int CMD_KICK = 196709;
    public static final int CMD_ON_QUIT = 196813;
    private static final int CMD_POLL_CHECK = 196713;
    public static final int CMD_POST_DHCP_ACTION = 196812;
    public static final int CMD_PRE_DHCP_ACTION = 196811;
    public static final int CMD_PRE_DHCP_ACTION_COMPLETE = 196814;
    private static final int CMD_RECEIVED_PACKET = 196710;
    private static final int CMD_RENEW_DHCP = 196712;
    public static final int CMD_START_DHCP = 196809;
    public static final int CMD_STOP_DHCP = 196810;
    private static final int CMD_TIMEOUT = 196711;
    private static final int DHCP_POLL_TOTAL_COUNTER = 8;
    private static final int DHCP_TIMEOUT_MS = 36000;
    private static final boolean DO_UNICAST = false;
    public static final int EVENT_LINKADDRESS_CONFIGURED = 196817;
    private static final int FIRST_TIMEOUT_MS = 2000;
    private static final int MAX_RETRY_COUNTER = 1;
    private static final int MAX_TIMEOUT_MS = 128000;
    private static final boolean MSG_DBG = false;
    private static final boolean PACKET_DBG = false;
    private static final int PUBLIC_BASE = 196808;
    private static final short RTM_NEWPREFIX = 52;
    private static final int SECONDS = 1000;
    private static final int STATEFUL_DHCPV6 = 2;
    private static final int STATELESS_DHCPV6 = 1;
    private static final boolean STATE_DBG = false;
    private static final String TAG = "MtkDhcp6Client";
    private static MtkDhcp6Client sDhcp6Client;
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private final StateMachine mController;
    private State mDhcpBoundState;
    private State mDhcpCheckState;
    private State mDhcpHaveAddressState;
    private State mDhcpInitRebootState;
    private State mDhcpInitState;
    private DhcpResults mDhcpLease;
    private long mDhcpLeaseExpiry;
    private int mDhcpPollCount;
    private State mDhcpRebindingState;
    private State mDhcpRebootingState;
    private State mDhcpRenewingState;
    private State mDhcpRequestingState;
    private State mDhcpSelectingState;
    private int mDhcpServerType;
    private State mDhcpState;
    private byte[] mHwAddr;
    private NetworkInterface mIface;
    private final String mIfaceName;
    private int mInterfaceIndex;
    private final WakeupMessage mKickAlarm;
    private final Object mLock;
    private final INetworkManagementService mNMService;
    private DhcpResults mOffer;
    private final Random mRandom;
    private ReceiveThread mReceiveThread;
    private boolean mRegisteredForPreDhcpNotification;
    private final WakeupMessage mRenewAlarm;
    private boolean mRunning;
    private byte[] mServerIdentifier;
    private Inet6Address mServerIpAddress;
    private State mStoppedState;
    private final WakeupMessage mTimeoutAlarm;
    private byte[] mTransactionId;
    private long mTransactionStartMillis;
    private FileDescriptor mUdpSock;
    private State mWaitBeforeRenewalState;
    private State mWaitBeforeStartState;
    private static final boolean DBG = !Build.IS_USER;
    private static byte[] sTimeStamp = null;
    private static final Class[] sMessageClasses = {MtkDhcp6Client.class};
    private static final SparseArray<String> sMessageNames = MessageUtils.findMessageNames(sMessageClasses);
    private static final short[] REQUESTED_PARAMS = {23, 24};

    static int access$610(MtkDhcp6Client mtkDhcp6Client) {
        int i = mtkDhcp6Client.mDhcpPollCount;
        mtkDhcp6Client.mDhcpPollCount = i - 1;
        return i;
    }

    private WakeupMessage makeWakeupMessage(String str, int i) {
        return new WakeupMessage(this.mContext, getHandler(), MtkDhcp6Client.class.getSimpleName() + "." + this.mIfaceName + "." + str, i);
    }

    private MtkDhcp6Client(Context context, StateMachine stateMachine, String str) {
        super(TAG);
        this.mLock = new Object();
        this.mStoppedState = new StoppedState();
        this.mDhcpCheckState = new DhcpCheckState();
        this.mDhcpState = new DhcpState();
        this.mDhcpInitState = new DhcpInitState();
        this.mDhcpSelectingState = new DhcpSelectingState();
        this.mDhcpRequestingState = new DhcpRequestingState();
        this.mDhcpHaveAddressState = new DhcpHaveAddressState();
        this.mDhcpBoundState = new DhcpBoundState();
        this.mDhcpRenewingState = new DhcpRenewingState();
        this.mDhcpRebindingState = new DhcpRebindingState();
        this.mDhcpInitRebootState = new DhcpInitRebootState();
        this.mDhcpRebootingState = new DhcpRebootingState();
        this.mWaitBeforeStartState = new WaitBeforeStartState(this.mDhcpInitState);
        this.mWaitBeforeRenewalState = new WaitBeforeRenewalState(this.mDhcpRenewingState);
        this.mContext = context;
        this.mController = stateMachine;
        this.mIfaceName = str;
        addState(this.mStoppedState);
        addState(this.mDhcpCheckState);
        addState(this.mDhcpState);
        addState(this.mDhcpInitState, this.mDhcpState);
        addState(this.mWaitBeforeStartState, this.mDhcpState);
        addState(this.mDhcpSelectingState, this.mDhcpState);
        addState(this.mDhcpRequestingState, this.mDhcpState);
        addState(this.mDhcpHaveAddressState, this.mDhcpState);
        addState(this.mDhcpBoundState, this.mDhcpHaveAddressState);
        addState(this.mWaitBeforeRenewalState, this.mDhcpHaveAddressState);
        addState(this.mDhcpRenewingState, this.mDhcpHaveAddressState);
        addState(this.mDhcpRebindingState, this.mDhcpHaveAddressState);
        addState(this.mDhcpInitRebootState, this.mDhcpState);
        addState(this.mDhcpRebootingState, this.mDhcpState);
        setInitialState(this.mStoppedState);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mNMService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mRandom = new Random();
        this.mKickAlarm = makeWakeupMessage("KICK", CMD_KICK);
        this.mTimeoutAlarm = makeWakeupMessage("TIMEOUT", CMD_TIMEOUT);
        this.mRenewAlarm = makeWakeupMessage("RENEW", CMD_RENEW_DHCP);
    }

    public void registerForPreDhcpNotification() {
        this.mRegisteredForPreDhcpNotification = true;
    }

    public static MtkDhcp6Client makeDhcp6Client(Context context, StateMachine stateMachine, String str) {
        MtkDhcp6Client mtkDhcp6Client = new MtkDhcp6Client(context, stateMachine, str);
        mtkDhcp6Client.start();
        sDhcp6Client = mtkDhcp6Client;
        Log.i(TAG, "makeDhcp6Client");
        return mtkDhcp6Client;
    }

    private boolean initInterface() {
        try {
            this.mIface = NetworkInterface.getByName(this.mIfaceName);
            this.mHwAddr = this.mIface.getHardwareAddress();
            this.mInterfaceIndex = this.mIface.getIndex();
            return true;
        } catch (NullPointerException | SocketException e) {
            Log.e(TAG, "Can't determine ifindex or MAC address for " + this.mIfaceName, e);
            Log.e(TAG, "mIface = " + this.mIface);
            return false;
        }
    }

    private void startNewTransaction() {
        this.mTransactionId = intToByteArray(this.mRandom.nextInt());
        this.mTransactionStartMillis = SystemClock.elapsedRealtime();
    }

    private InetAddress getIpv6LinkLocalAddress(NetworkInterface networkInterface) {
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddressNextElement = inetAddresses.nextElement();
            if (inetAddressNextElement.isLinkLocalAddress()) {
                if (DBG) {
                    Log.i(TAG, "Source address:" + inetAddressNextElement);
                } else {
                    Log.i(TAG, "Source address: ...");
                }
                return inetAddressNextElement;
            }
        }
        return Inet6Address.ANY;
    }

    private boolean initSockets() {
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-192);
        try {
            this.mUdpSock = Os.socket(OsConstants.AF_INET6, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, 1);
            Os.setsockoptIfreq(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, this.mIfaceName);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, 1);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 0);
            Os.bind(this.mUdpSock, getIpv6LinkLocalAddress(this.mIface), 546);
            NetworkUtils.protectFromVpn(this.mUdpSock);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating UDP socket", e);
            if (this.mUdpSock != null) {
                closeSockets();
            }
            return false;
        } finally {
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        }
    }

    private boolean connectUdpSock(Inet6Address inet6Address) {
        try {
            Os.connect(this.mUdpSock, inet6Address, 547);
            return true;
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
    }

    private boolean setIpAddress(LinkAddress linkAddress) {
        INetd netdService = NetdService.getInstance();
        if (netdService == null) {
            Log.e(TAG, "No netd service instance available;not setting local IPv6 addresses");
            return false;
        }
        try {
            netdService.interfaceAddAddress(this.mIfaceName, linkAddress.getAddress().getHostAddress(), linkAddress.getPrefixLength());
            return true;
        } catch (ServiceSpecificException | RemoteException e) {
            Log.e(TAG, "Error configuring IP address :" + linkAddress + ": " + e);
            return false;
        }
    }

    class ReceiveThread extends Thread {
        private final byte[] mPacket = new byte[1500];
        private boolean mStopped = false;

        ReceiveThread() {
        }

        public void halt() {
            this.mStopped = true;
            MtkDhcp6Client.this.closeSockets();
        }

        @Override
        public void run() {
            if (MtkDhcp6Client.DBG) {
                Log.d(MtkDhcp6Client.TAG, "Receive thread started");
            }
            while (!this.mStopped) {
                try {
                    MtkDhcp6Packet mtkDhcp6PacketDecodeFullPacket = MtkDhcp6Packet.decodeFullPacket(this.mPacket, Os.read(MtkDhcp6Client.this.mUdpSock, this.mPacket, 0, this.mPacket.length));
                    if (mtkDhcp6PacketDecodeFullPacket != null) {
                        if (MtkDhcp6Client.DBG) {
                            Log.d(MtkDhcp6Client.TAG, "Received packet: " + mtkDhcp6PacketDecodeFullPacket);
                        }
                        MtkDhcp6Client.this.sendMessage(MtkDhcp6Client.CMD_RECEIVED_PACKET, mtkDhcp6PacketDecodeFullPacket);
                    }
                } catch (Exception e) {
                    if (!this.mStopped) {
                        Log.e(MtkDhcp6Client.TAG, "Read error", e);
                    }
                }
            }
            if (MtkDhcp6Client.DBG) {
                Log.d(MtkDhcp6Client.TAG, "Receive thread stopped");
            }
        }
    }

    private short getSecs() {
        return (short) ((SystemClock.elapsedRealtime() - this.mTransactionStartMillis) / 1000);
    }

    private boolean transmitPacket(ByteBuffer byteBuffer, String str, Inet6Address inet6Address) {
        try {
            if (DBG) {
                Log.d(TAG, "Sending " + str + " to " + inet6Address.getHostAddress());
            }
            Os.sendto(this.mUdpSock, byteBuffer.array(), 0, byteBuffer.limit(), 0, inet6Address, 547);
            return true;
        } catch (ErrnoException | IOException e) {
            Log.e(TAG, "Can't send packet: ", e);
            return false;
        }
    }

    private boolean sendSolicitPacket() {
        return transmitPacket(MtkDhcp6Packet.buildSolicitPacket(this.mTransactionId, getSecs(), this.mHwAddr, REQUESTED_PARAMS), "DHCPSOLICIT", MtkDhcp6Packet.INADDR_BROADCAST_ROUTER);
    }

    private boolean sendInfoRequestPacket() {
        return transmitPacket(MtkDhcp6Packet.buildInfoRequestPacket(this.mTransactionId, getSecs(), this.mHwAddr, REQUESTED_PARAMS), "DHCP_INFO_REQUEST", MtkDhcp6Packet.INADDR_BROADCAST_ROUTER);
    }

    private boolean sendRequestPacket(Inet6Address inet6Address, Inet6Address inet6Address2, Inet6Address inet6Address3, Inet6Address inet6Address4) {
        return transmitPacket(MtkDhcp6Packet.buildRequestPacket(this.mTransactionId, getSecs(), inet6Address, this.mHwAddr, inet6Address2, this.mServerIdentifier, REQUESTED_PARAMS), "DHCPREQUEST  request=" + inet6Address2.getHostAddress(), MtkDhcp6Packet.INADDR_BROADCAST_ROUTER);
    }

    private void scheduleRenew() {
        if (this.mDhcpLeaseExpiry != 0) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long j = this.mDhcpLeaseExpiry + jElapsedRealtime;
            this.mRenewAlarm.schedule(j);
            Log.d(TAG, "Scheduling renewal in " + ((j - jElapsedRealtime) / 1000) + "s");
            return;
        }
        Log.d(TAG, "Infinite lease, no renewal needed");
    }

    private void clearDhcpState() {
        this.mReceiveThread = null;
        this.mDhcpLease = null;
        this.mDhcpLeaseExpiry = 0L;
        this.mOffer = null;
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
        LoggingState() {
        }

        public void enter() {
        }

        private String messageName(int i) {
            return (String) MtkDhcp6Client.sMessageNames.get(i, Integer.toString(i));
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
    }

    abstract class WaitBeforeOtherState extends LoggingState {
        protected State mOtherState;

        WaitBeforeOtherState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcp6Client.this.mController.sendMessage(MtkDhcp6Client.CMD_PRE_DHCP_ACTION);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196814) {
                MtkDhcp6Client.this.transitionTo(this.mOtherState);
                return true;
            }
            return false;
        }
    }

    class StoppedState extends LoggingState {
        StoppedState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196809) {
                if (MtkDhcp6Client.this.mRegisteredForPreDhcpNotification) {
                    MtkDhcp6Client.this.mDhcpPollCount = 1;
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpCheckState);
                } else {
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpInitState);
                }
                return true;
            }
            return false;
        }
    }

    class DhcpCheckState extends LoggingState {
        boolean mIsPreDhcpComplete;

        DhcpCheckState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            this.mIsPreDhcpComplete = false;
            if (!MtkDhcp6Client.this.initInterface()) {
                MtkDhcp6Client.this.sendMessage(MtkDhcp6Client.CMD_STOP_DHCP);
            } else {
                MtkDhcp6Client.this.sendMessage(MtkDhcp6Client.CMD_POLL_CHECK);
                MtkDhcp6Client.access$610(MtkDhcp6Client.this);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case MtkDhcp6Client.CMD_POLL_CHECK:
                    MtkDhcp6Client.this.mDhcpServerType = 2;
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpInitState);
                    break;
                case MtkDhcp6Client.CMD_IPV6_PREFIX:
                    MtkDhcp6Client.this.mDhcpServerType = 2;
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpInitState);
                    break;
                case MtkDhcp6Client.CMD_STOP_DHCP:
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mStoppedState);
                    break;
                case MtkDhcp6Client.CMD_PRE_DHCP_ACTION_COMPLETE:
                    this.mIsPreDhcpComplete = true;
                    break;
            }
            return true;
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

    class DhcpState extends LoggingState {
        DhcpState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcp6Client.this.clearDhcpState();
            if (MtkDhcp6Client.this.initSockets()) {
                MtkDhcp6Client.this.mReceiveThread = MtkDhcp6Client.this.new ReceiveThread();
                MtkDhcp6Client.this.mReceiveThread.start();
                return;
            }
            MtkDhcp6Client.this.sendMessage(MtkDhcp6Client.CMD_STOP_DHCP);
        }

        public void exit() {
            if (MtkDhcp6Client.this.mReceiveThread != null) {
                MtkDhcp6Client.this.mReceiveThread.halt();
                MtkDhcp6Client.this.mReceiveThread = null;
            }
            MtkDhcp6Client.this.clearDhcpState();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196810) {
                MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mStoppedState);
                return true;
            }
            return false;
        }
    }

    public boolean isValidPacket(MtkDhcp6Packet mtkDhcp6Packet) {
        byte[] transactionId = mtkDhcp6Packet.getTransactionId();
        if (!Arrays.equals(transactionId, this.mTransactionId)) {
            Log.d(TAG, "Unexpected transaction ID " + HexDump.toHexString(transactionId) + ", expected " + HexDump.toHexString(this.mTransactionId));
            return false;
        }
        if (mtkDhcp6Packet.getClientMac() == null || !Arrays.equals(mtkDhcp6Packet.getClientMac(), this.mHwAddr)) {
            Log.d(TAG, "MAC addr mismatch: got " + HexDump.toHexString(mtkDhcp6Packet.getClientMac()) + ", expected " + HexDump.toHexString(this.mHwAddr));
            return false;
        }
        return true;
    }

    public void setDhcpLeaseExpiry(MtkDhcp6Packet mtkDhcp6Packet) {
        long leaseTimeMillis = mtkDhcp6Packet.getLeaseTimeMillis();
        this.mDhcpLeaseExpiry = leaseTimeMillis > 0 ? SystemClock.elapsedRealtime() + leaseTimeMillis : 0L;
    }

    abstract class PacketRetransmittingState extends LoggingState {
        protected int mTimeout;
        private int mTimer;

        protected abstract void receivePacket(MtkDhcp6Packet mtkDhcp6Packet);

        protected abstract boolean sendPacket();

        PacketRetransmittingState() {
            super();
            this.mTimeout = 0;
        }

        @Override
        public void enter() {
            super.enter();
            if (MtkDhcp6Client.this.mReceiveThread == null) {
                MtkDhcp6Client.this.sendMessage(MtkDhcp6Client.CMD_STOP_DHCP);
                return;
            }
            initTimer();
            maybeInitTimeout();
            MtkDhcp6Client.this.sendMessage(MtkDhcp6Client.CMD_KICK);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case MtkDhcp6Client.CMD_KICK:
                    try {
                        sendPacket();
                        scheduleKick();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case MtkDhcp6Client.CMD_RECEIVED_PACKET:
                    try {
                        receivePacket((MtkDhcp6Packet) message.obj);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    break;
                case MtkDhcp6Client.CMD_TIMEOUT:
                    try {
                        timeout();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                    break;
            }
            return true;
            return true;
            return true;
        }

        public void exit() {
            MtkDhcp6Client.this.mKickAlarm.cancel();
            MtkDhcp6Client.this.mTimeoutAlarm.cancel();
        }

        protected void timeout() {
        }

        protected void initTimer() {
            this.mTimer = MtkDhcp6Client.FIRST_TIMEOUT_MS;
        }

        protected int jitterTimer(int i) {
            int i2 = i / 10;
            return i + (MtkDhcp6Client.this.mRandom.nextInt(2 * i2) - i2);
        }

        protected void scheduleKick() {
            MtkDhcp6Client.this.mKickAlarm.schedule(SystemClock.elapsedRealtime() + ((long) jitterTimer(this.mTimer)));
            this.mTimer *= 2;
            if (this.mTimer > MtkDhcp6Client.MAX_TIMEOUT_MS) {
                this.mTimer = MtkDhcp6Client.MAX_TIMEOUT_MS;
            }
        }

        protected void maybeInitTimeout() {
            if (this.mTimeout > 0) {
                MtkDhcp6Client.this.mTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeout));
            }
        }
    }

    class DhcpInitState extends PacketRetransmittingState {
        public DhcpInitState() {
            super();
            this.mTimeout = 18000;
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcp6Client.this.startNewTransaction();
        }

        @Override
        protected void timeout() {
            if (MtkDhcp6Client.this.mDhcpPollCount > 0) {
                MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpCheckState);
            } else {
                MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mStoppedState);
            }
        }

        @Override
        protected boolean sendPacket() {
            if (MtkDhcp6Client.this.mDhcpServerType == 2) {
                return MtkDhcp6Client.this.sendSolicitPacket();
            }
            if (MtkDhcp6Client.this.mDhcpServerType == 2) {
                return MtkDhcp6Client.this.sendInfoRequestPacket();
            }
            return false;
        }

        @Override
        protected void receivePacket(MtkDhcp6Packet mtkDhcp6Packet) {
            DhcpResults dhcpResults;
            if (MtkDhcp6Client.this.isValidPacket(mtkDhcp6Packet)) {
                if (MtkDhcp6Client.this.mDhcpServerType == 2) {
                    if (mtkDhcp6Packet instanceof MtkDhcp6AdvertisePacket) {
                        if (mtkDhcp6Packet.mStatusCode == 0) {
                            MtkDhcp6Client.this.mOffer = mtkDhcp6Packet.toDhcpResults();
                            if (MtkDhcp6Client.this.mOffer != null) {
                                MtkDhcp6Client.this.mServerIdentifier = mtkDhcp6Packet.mServerIdentifier;
                                MtkDhcp6Client.this.mServerIpAddress = mtkDhcp6Packet.mServerAddress;
                                Log.d(MtkDhcp6Client.TAG, "Got pending lease");
                                if (((StaticIpConfiguration) MtkDhcp6Client.this.mOffer).dnsServers.size() != 0) {
                                    MtkDhcp6Client.this.mController.sendMessage(MtkDhcp6Client.CMD_CONFIGURE_DNSV6, 0, 0, ((StaticIpConfiguration) MtkDhcp6Client.this.mOffer).dnsServers);
                                }
                                MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpRequestingState);
                                return;
                            }
                            return;
                        }
                        Log.e(MtkDhcp6Client.TAG, "Status Code is " + mtkDhcp6Packet.mStatusCode);
                        MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mStoppedState);
                        return;
                    }
                    return;
                }
                if ((mtkDhcp6Packet instanceof MtkDhcp6ReplyPacket) && (dhcpResults = mtkDhcp6Packet.toDhcpResults()) != null) {
                    MtkDhcp6Client.this.mDhcpLease = dhcpResults;
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpBoundState);
                }
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
            return MtkDhcp6Client.this.sendRequestPacket(MtkDhcp6Packet.INADDR_ANY, (Inet6Address) ((StaticIpConfiguration) MtkDhcp6Client.this.mOffer).ipAddress.getAddress(), MtkDhcp6Client.this.mServerIpAddress, MtkDhcp6Packet.INADDR_BROADCAST_ROUTER);
        }

        @Override
        protected void receivePacket(MtkDhcp6Packet mtkDhcp6Packet) {
            if (MtkDhcp6Client.this.isValidPacket(mtkDhcp6Packet)) {
                if (!(mtkDhcp6Packet instanceof MtkDhcp6ReplyPacket)) {
                    if (mtkDhcp6Packet instanceof MtkDhcp6NakPacket) {
                        Log.d(MtkDhcp6Client.TAG, "Received NAK, returning to INIT");
                        MtkDhcp6Client.this.mOffer = null;
                        MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpInitState);
                        return;
                    }
                    return;
                }
                DhcpResults dhcpResults = mtkDhcp6Packet.toDhcpResults();
                if (dhcpResults != null) {
                    MtkDhcp6Client.this.mDhcpLease = dhcpResults;
                    if (((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).dnsServers.size() == 0 && ((StaticIpConfiguration) MtkDhcp6Client.this.mOffer).dnsServers.size() != 0) {
                        Log.d(MtkDhcp6Client.TAG, "Get DNS server address from Advertise message");
                        ((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).dnsServers.addAll(((StaticIpConfiguration) MtkDhcp6Client.this.mOffer).dnsServers);
                    }
                    MtkDhcp6Client.this.mOffer = null;
                    MtkDhcp6Client.this.mServerIdentifier = mtkDhcp6Packet.mServerIdentifier;
                    MtkDhcp6Client.this.mServerIpAddress = mtkDhcp6Packet.mServerAddress;
                    Log.d(MtkDhcp6Client.TAG, "Confirmed lease: " + MtkDhcp6Client.this.mDhcpLease);
                    MtkDhcp6Client.this.setDhcpLeaseExpiry(mtkDhcp6Packet);
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpBoundState);
                }
            }
        }

        @Override
        protected void timeout() {
            MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpInitState);
        }
    }

    class DhcpHaveAddressState extends LoggingState {
        DhcpHaveAddressState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            if (MtkDhcp6Client.this.setIpAddress(((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).ipAddress)) {
                if (MtkDhcp6Client.DBG) {
                    Log.d(MtkDhcp6Client.TAG, "Configured IPv6 address " + ((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).ipAddress);
                }
                if (((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).dnsServers != null) {
                    MtkDhcp6Client.this.mController.sendMessage(MtkDhcp6Client.CMD_CONFIGURE_DNSV6, 0, 0, ((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).dnsServers);
                    return;
                }
                return;
            }
            Log.e(MtkDhcp6Client.TAG, "Failed to configure IPv6 address " + ((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).ipAddress);
            MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mStoppedState);
        }

        public void exit() {
            if (MtkDhcp6Client.DBG) {
                Log.d(MtkDhcp6Client.TAG, "Clearing IPv6 address");
            }
        }
    }

    class DhcpBoundState extends LoggingState {
        DhcpBoundState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            if (MtkDhcp6Client.this.mDhcpServerType == 2) {
                MtkDhcp6Client.this.scheduleRenew();
            }
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == MtkDhcp6Client.CMD_RENEW_DHCP) {
                if (MtkDhcp6Client.this.mRegisteredForPreDhcpNotification) {
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mWaitBeforeRenewalState);
                    return true;
                }
                MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpRenewingState);
                return true;
            }
            return false;
        }
    }

    class DhcpRenewingState extends PacketRetransmittingState {
        public DhcpRenewingState() {
            super();
            this.mTimeout = MtkDhcp6Client.DHCP_TIMEOUT_MS;
        }

        @Override
        public void enter() {
            super.enter();
            MtkDhcp6Client.this.startNewTransaction();
        }

        @Override
        protected boolean sendPacket() {
            return MtkDhcp6Client.this.sendRequestPacket((Inet6Address) ((StaticIpConfiguration) MtkDhcp6Client.this.mDhcpLease).ipAddress.getAddress(), MtkDhcp6Packet.INADDR_ANY, MtkDhcp6Packet.INADDR_ANY, MtkDhcp6Client.this.mServerIpAddress);
        }

        @Override
        protected void receivePacket(MtkDhcp6Packet mtkDhcp6Packet) {
            if (MtkDhcp6Client.this.isValidPacket(mtkDhcp6Packet)) {
                if (mtkDhcp6Packet instanceof MtkDhcp6ReplyPacket) {
                    MtkDhcp6Client.this.setDhcpLeaseExpiry(mtkDhcp6Packet);
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpBoundState);
                } else if (mtkDhcp6Packet instanceof MtkDhcp6NakPacket) {
                    MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mDhcpInitState);
                }
            }
        }

        @Override
        protected void timeout() {
            MtkDhcp6Client.this.transitionTo(MtkDhcp6Client.this.mStoppedState);
        }
    }

    class DhcpRebindingState extends LoggingState {
        DhcpRebindingState() {
            super();
        }
    }

    class DhcpInitRebootState extends LoggingState {
        DhcpInitRebootState() {
            super();
        }
    }

    class DhcpRebootingState extends LoggingState {
        DhcpRebootingState() {
            super();
        }
    }

    private static final byte[] intToByteArray(int i) {
        return new byte[]{(byte) (i >>> 16), (byte) (i >>> DHCP_POLL_TOTAL_COUNTER), (byte) i};
    }

    public static byte[] getTimeStamp() {
        byte[] bArr;
        synchronized (MtkDhcp6Client.class) {
            if (sTimeStamp == null) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.set(FIRST_TIMEOUT_MS, 0, 1, 0, 0, 0);
                Long lValueOf = Long.valueOf(((Long.valueOf(Calendar.getInstance().getTimeInMillis()).longValue() - Long.valueOf(calendar.getTimeInMillis()).longValue()) / 1000) % 4294967296L);
                ByteBuffer byteBufferAllocate = ByteBuffer.allocate(4);
                byteBufferAllocate.clear();
                byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
                byteBufferAllocate.putInt(lValueOf.intValue());
                sTimeStamp = byteBufferAllocate.array();
            }
            bArr = sTimeStamp;
        }
        return bArr;
    }

    private boolean stillRunning() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mRunning;
        }
        return z;
    }
}
