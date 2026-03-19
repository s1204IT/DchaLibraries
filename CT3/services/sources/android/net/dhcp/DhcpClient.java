package android.net.dhcp;

import android.content.Context;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.arp.ArpPeer;
import android.net.dhcp.DhcpPacket;
import android.net.metrics.DhcpClientEvent;
import android.net.metrics.DhcpErrorEvent;
import android.net.netlink.StructNlMsgHdr;
import android.net.wifi.WifiConfiguration;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import libcore.io.IoBridge;

public class DhcpClient extends StateMachine {
    public static final int CMD_CLEAR_LINKADDRESS = 196615;
    public static final int CMD_CONFIGURE_LINKADDRESS = 196616;
    private static final int CMD_EXPIRE_DHCP = 196714;
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
    private static final boolean DBG = true;
    public static final int DHCP_FAILURE = 2;
    public static final int DHCP_SUCCESS = 1;
    private static final int DHCP_TIMEOUT_MS = 36000;
    private static final boolean DO_UNICAST = false;
    public static final int EVENT_LINKADDRESS_CONFIGURED = 196617;
    private static final int FIRST_TIMEOUT_MS = 500;
    private static final int MAX_TIMEOUT_MS = 128000;
    private static final boolean MSG_DBG = true;
    private static final boolean PACKET_DBG = true;
    private static final int PRIVATE_BASE = 196708;
    private static final int PUBLIC_BASE = 196608;
    private static final int SECONDS = 1000;
    private static final boolean STATE_DBG = true;
    private static final String TAG = "DhcpClient";
    private State mConfiguringInterfaceState;
    private final Context mContext;
    private final StateMachine mController;
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
    private byte[] mHwAddr;
    private NetworkInterface mIface;
    private final String mIfaceName;
    private PacketSocketAddress mInterfaceBroadcastAddr;
    private boolean mIsAutoIpEnabled;
    private boolean mIsIpRecoverEnabled;
    private final WakeupMessage mKickAlarm;
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
    private State mWaitBeforeRenewalState;
    private State mWaitBeforeStartState;
    private static final Class[] sMessageClasses = {DhcpClient.class};
    private static final SparseArray<String> sMessageNames = MessageUtils.findMessageNames(sMessageClasses);
    static final byte[] REQUESTED_PARAMS = {1, 33, 3, 6, 15, 28, 51, 58, 59, 43};

    private WakeupMessage makeWakeupMessage(String cmdName, int cmd) {
        return new WakeupMessage(this.mContext, getHandler(), DhcpClient.class.getSimpleName() + "." + this.mIfaceName + "." + cmdName, cmd);
    }

    private DhcpClient(Context context, StateMachine controller, String iface) {
        super(TAG);
        this.mIsAutoIpEnabled = false;
        this.mIsIpRecoverEnabled = true;
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
        this.mContext = context;
        this.mController = controller;
        this.mIfaceName = iface;
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
        setInitialState(this.mStoppedState);
        this.mRandom = new Random();
        this.mKickAlarm = makeWakeupMessage("KICK", CMD_KICK);
        this.mTimeoutAlarm = makeWakeupMessage("TIMEOUT", CMD_TIMEOUT);
        this.mRenewAlarm = makeWakeupMessage("RENEW", CMD_RENEW_DHCP);
        this.mRebindAlarm = makeWakeupMessage("REBIND", CMD_REBIND_DHCP);
        this.mExpiryAlarm = makeWakeupMessage("EXPIRY", CMD_EXPIRE_DHCP);
    }

    public void registerForPreDhcpNotification() {
        this.mRegisteredForPreDhcpNotification = true;
    }

    public static DhcpClient makeDhcpClient(Context context, StateMachine controller, String intf) {
        DhcpClient client = new DhcpClient(context, controller, intf);
        client.start();
        return client;
    }

    private boolean initInterface() {
        try {
            this.mIface = NetworkInterface.getByName(this.mIfaceName);
            this.mHwAddr = this.mIface.getHardwareAddress();
            this.mInterfaceBroadcastAddr = new PacketSocketAddress(this.mIface.getIndex(), DhcpPacket.ETHER_BROADCAST);
            this.mInterfaceBroadcastAddr.sll_protocol = StructNlMsgHdr.NLM_F_APPEND;
            return true;
        } catch (NullPointerException | SocketException e) {
            Log.e(TAG, "Can't determine ifindex or MAC address for " + this.mIfaceName, e);
            Log.e(TAG, "mIface = " + this.mIface);
            return false;
        }
    }

    private void startNewTransaction() {
        this.mTransactionId = this.mRandom.nextInt();
        this.mTransactionStartMillis = SystemClock.elapsedRealtime();
    }

    private boolean initSockets() {
        if (initPacketSocket()) {
            return initUdpSocket();
        }
        return false;
    }

    private boolean initPacketSocket() {
        try {
            this.mPacketSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IP);
            PacketSocketAddress addr = new PacketSocketAddress((short) OsConstants.ETH_P_IP, this.mIface.getIndex());
            Os.bind(this.mPacketSock, addr);
            NetworkUtils.attachDhcpFilter(this.mPacketSock);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating packet socket", e);
            return false;
        }
    }

    private boolean initUdpSocket() {
        try {
            this.mUdpSock = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, 1);
            Os.setsockoptIfreq(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, this.mIfaceName);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, 1);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 0);
            Os.bind(this.mUdpSock, Inet4Address.ANY, 68);
            NetworkUtils.protectFromVpn(this.mUdpSock);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating UDP socket", e);
            return false;
        }
    }

    private boolean connectUdpSock(Inet4Address to) {
        try {
            Os.connect(this.mUdpSock, to, 67);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error connecting UDP socket", e);
            return false;
        }
    }

    private static void closeQuietly(FileDescriptor fd) {
        try {
            IoBridge.closeAndSignalBlockedThreads(fd);
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
            this.mStopped = true;
            DhcpClient.this.closeSockets();
        }

        @Override
        public void run() {
            Log.d(DhcpClient.TAG, "Receive thread started");
            while (!this.mStopped) {
                int length = 0;
                try {
                    length = Os.read(DhcpClient.this.mPacketSock, this.mPacket, 0, this.mPacket.length);
                    DhcpPacket packet = DhcpPacket.decodeFullPacket(this.mPacket, length, 0);
                    Log.d(DhcpClient.TAG, "Received packet: " + packet);
                    DhcpClient.this.sendMessage(DhcpClient.CMD_RECEIVED_PACKET, packet);
                } catch (DhcpPacket.ParseException e) {
                    Log.e(DhcpClient.TAG, "Can't parse packet: " + e.getMessage());
                    Log.d(DhcpClient.TAG, HexDump.dumpHexString(this.mPacket, 0, length));
                    DhcpErrorEvent.logParseError(DhcpClient.this.mIfaceName, e.errorCode);
                } catch (ErrnoException | IOException e2) {
                    if (!this.mStopped) {
                        Log.e(DhcpClient.TAG, "Read error", e2);
                        DhcpErrorEvent.logReceiveError(DhcpClient.this.mIfaceName);
                    }
                }
            }
            Log.d(DhcpClient.TAG, "Receive thread stopped");
        }
    }

    private short getSecs() {
        return (short) ((SystemClock.elapsedRealtime() - this.mTransactionStartMillis) / 1000);
    }

    private boolean transmitPacket(ByteBuffer buf, String description, int encap, Inet4Address to) {
        try {
            if (encap == 0) {
                Log.d(TAG, "Broadcasting " + description);
                Os.sendto(this.mPacketSock, buf.array(), 0, buf.limit(), 0, this.mInterfaceBroadcastAddr);
            } else if (encap == 2 && to.equals(DhcpPacket.INADDR_BROADCAST)) {
                Log.d(TAG, "Broadcasting " + description);
                Os.sendto(this.mUdpSock, buf, 0, to, 67);
            } else {
                Log.d(TAG, String.format("Unicasting %s to %s", description, Os.getpeername(this.mUdpSock)));
                Os.write(this.mUdpSock, buf);
            }
            return true;
        } catch (ErrnoException | IOException e) {
            Log.e(TAG, "Can't send packet: ", e);
            return false;
        }
    }

    private boolean sendDiscoverPacket() {
        ByteBuffer packet = DhcpPacket.buildDiscoverPacket(0, this.mTransactionId, getSecs(), this.mHwAddr, false, REQUESTED_PARAMS);
        return transmitPacket(packet, "DHCPDISCOVER", 0, DhcpPacket.INADDR_BROADCAST);
    }

    private boolean sendRequestPacket(Inet4Address clientAddress, Inet4Address requestedAddress, Inet4Address serverAddress, Inet4Address to) {
        int encap = DhcpPacket.INADDR_ANY.equals(clientAddress) ? 0 : 2;
        ByteBuffer packet = DhcpPacket.buildRequestPacket(encap, this.mTransactionId, getSecs(), clientAddress, false, this.mHwAddr, requestedAddress, serverAddress, REQUESTED_PARAMS, null);
        String description = "DHCPREQUEST ciaddr=" + clientAddress.getHostAddress() + " request=" + requestedAddress.getHostAddress() + " serverid=" + (serverAddress != null ? serverAddress.getHostAddress() : null);
        return transmitPacket(packet, description, encap, to);
    }

    private void scheduleLeaseTimers() {
        if (this.mDhcpLeaseExpiry == 0) {
            Log.d(TAG, "Infinite lease, no timer scheduling needed");
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long remainingDelay = this.mDhcpLeaseExpiry - now;
        long renewDelay = remainingDelay / 2;
        long rebindDelay = (7 * remainingDelay) / 8;
        this.mRenewAlarm.schedule(now + renewDelay);
        this.mRebindAlarm.schedule(now + rebindDelay);
        this.mExpiryAlarm.schedule(now + remainingDelay);
        Log.d(TAG, "Scheduling renewal in " + (renewDelay / 1000) + "s");
        Log.d(TAG, "Scheduling rebind in " + (rebindDelay / 1000) + "s");
        Log.d(TAG, "Scheduling expiry in " + (remainingDelay / 1000) + "s");
    }

    private void notifySuccess() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 1, 0, new DhcpResults(this.mDhcpLease));
    }

    private void notifyFailure() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 2, 0, (Object) null);
    }

    private void acceptDhcpResults(DhcpResults results, String msg) {
        results.setSystemExpiredTime(this.mDhcpLeaseExpiry);
        this.mDhcpLease = results;
        this.mOffer = null;
        Log.d(TAG, msg + " lease: " + this.mDhcpLease);
        notifySuccess();
    }

    private void clearDhcpState() {
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
            Log.d(DhcpClient.TAG, "Entering state " + getName());
            DhcpClientEvent.logStateEvent(DhcpClient.this.mIfaceName, getName());
        }

        private String messageName(int what) {
            return (String) DhcpClient.sMessageNames.get(what, Integer.toString(what));
        }

        private String messageToString(Message message) {
            long now = SystemClock.uptimeMillis();
            StringBuilder b = new StringBuilder(" ");
            TimeUtils.formatDuration(message.getWhen() - now, b);
            b.append(" ").append(messageName(message.what)).append(" ").append(message.arg1).append(" ").append(message.arg2).append(" ").append(message.obj);
            return b.toString();
        }

        public boolean processMessage(Message message) {
            Log.d(DhcpClient.TAG, getName() + messageToString(message));
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
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE:
                    DhcpClient.this.mIsIpRecoverEnabled = true;
                    if (message.obj != null) {
                        WifiConfiguration wifiConfig = (WifiConfiguration) message.obj;
                        if (wifiConfig.allowedKeyManagement.get(0) && wifiConfig.wepTxKeyIndex >= 0 && wifiConfig.wepTxKeyIndex < wifiConfig.wepKeys.length && wifiConfig.wepKeys[wifiConfig.wepTxKeyIndex] != null) {
                            DhcpClient.this.mIsIpRecoverEnabled = false;
                        }
                    }
                    Log.d(DhcpClient.TAG, "IP recover: mIsIpRecoverEnabled@CMD_PRE_DHCP_ACTION_COMPLETE = " + DhcpClient.this.mIsIpRecoverEnabled);
                    DhcpClient.this.transitionTo(this.mOtherState);
                    return true;
                default:
                    return false;
            }
        }
    }

    class StoppedState extends LoggingState {
        StoppedState() {
            super();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_START_DHCP:
                    DhcpClient.this.mPastDhcpLease = (DhcpResults) message.obj;
                    DhcpClient.this.mIsIpRecoverEnabled = true;
                    Log.d(DhcpClient.TAG, "mPastDhcpLease@StoppedState = " + DhcpClient.this.mPastDhcpLease);
                    if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                        DhcpClient.this.transitionTo(DhcpClient.this.mWaitBeforeStartState);
                    } else {
                        DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    class WaitBeforeStartState extends WaitBeforeOtherState {
        public WaitBeforeStartState(State otherState) {
            super();
            this.mOtherState = otherState;
        }
    }

    class WaitBeforeRenewalState extends WaitBeforeOtherState {
        public WaitBeforeRenewalState(State otherState) {
            super();
            this.mOtherState = otherState;
        }
    }

    class DhcpState extends LoggingState {
        DhcpState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            DhcpClient.this.clearDhcpState();
            if (DhcpClient.this.initInterface() && DhcpClient.this.initSockets()) {
                DhcpClient.this.mReceiveThread = DhcpClient.this.new ReceiveThread();
                DhcpClient.this.mReceiveThread.start();
            } else {
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
            }
        }

        public void exit() {
            if (DhcpClient.this.mReceiveThread != null) {
                DhcpClient.this.mReceiveThread.halt();
                DhcpClient.this.mReceiveThread = null;
            }
            DhcpClient.this.clearDhcpState();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_STOP_DHCP:
                    DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
                    return true;
                default:
                    return false;
            }
        }
    }

    public boolean isValidPacket(DhcpPacket packet) {
        int xid = packet.getTransactionId();
        if (xid != this.mTransactionId) {
            Log.d(TAG, "Unexpected transaction ID " + xid + ", expected " + this.mTransactionId);
            return false;
        }
        if (Arrays.equals(packet.getClientMac(), this.mHwAddr)) {
            return true;
        }
        Log.d(TAG, "MAC addr mismatch: got " + HexDump.toHexString(packet.getClientMac()) + ", expected " + HexDump.toHexString(this.mHwAddr));
        return false;
    }

    public void setDhcpLeaseExpiry(DhcpPacket packet) {
        long leaseTimeMillis = packet.getLeaseTimeMillis();
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
            DhcpClient.this.sendMessage(DhcpClient.CMD_KICK);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_KICK:
                    sendPacket();
                    scheduleKick();
                    break;
                case DhcpClient.CMD_RECEIVED_PACKET:
                    receivePacket((DhcpPacket) message.obj);
                    break;
                case DhcpClient.CMD_TIMEOUT:
                    timeout();
                    break;
            }
            return true;
        }

        public void exit() {
            DhcpClient.this.mKickAlarm.cancel();
            DhcpClient.this.mTimeoutAlarm.cancel();
        }

        protected void timeout() {
        }

        protected void initTimer() {
            this.mTimer = 500;
        }

        protected int jitterTimer(int baseTimer) {
            int maxJitter = baseTimer / 10;
            int jitter = DhcpClient.this.mRandom.nextInt(maxJitter * 2) - maxJitter;
            return baseTimer + jitter;
        }

        protected void scheduleKick() {
            long now = SystemClock.elapsedRealtime();
            long timeout = jitterTimer(this.mTimer);
            long alarmTime = now + timeout;
            DhcpClient.this.mKickAlarm.schedule(alarmTime);
            this.mTimer *= 2;
            if (this.mTimer <= DhcpClient.MAX_TIMEOUT_MS) {
                return;
            }
            this.mTimer = DhcpClient.MAX_TIMEOUT_MS;
        }

        protected void maybeInitTimeout() {
            if (this.mTimeout <= 0) {
                return;
            }
            long alarmTime = SystemClock.elapsedRealtime() + ((long) this.mTimeout);
            DhcpClient.this.mTimeoutAlarm.schedule(alarmTime);
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
            DhcpClient.this.startNewTransaction();
        }

        @Override
        protected boolean sendPacket() {
            return DhcpClient.this.sendDiscoverPacket();
        }

        @Override
        protected void receivePacket(DhcpPacket packet) {
            if (DhcpClient.this.isValidPacket(packet) && (packet instanceof DhcpOfferPacket)) {
                DhcpClient.this.mOffer = packet.toDhcpResults();
                if (DhcpClient.this.mOffer == null) {
                    return;
                }
                Log.d(DhcpClient.TAG, "Got pending lease: " + DhcpClient.this.mOffer);
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRequestingState);
            }
        }

        @Override
        protected void timeout() {
            if (!DhcpClient.this.doIpRecover()) {
                return;
            }
            DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
        }

        @Override
        protected void scheduleKick() {
            long timeout = jitterTimer(this.mTimer);
            Log.d(DhcpClient.TAG, "scheduleKick()@DhcpInitState timeout=" + timeout);
            DhcpClient.this.sendMessageDelayed(DhcpClient.CMD_KICK, timeout);
            this.mTimer *= 2;
            if (this.mTimer <= DhcpClient.MAX_TIMEOUT_MS) {
                return;
            }
            this.mTimer = DhcpClient.MAX_TIMEOUT_MS;
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
            return DhcpClient.this.sendRequestPacket(DhcpPacket.INADDR_ANY, (Inet4Address) DhcpClient.this.mOffer.ipAddress.getAddress(), DhcpClient.this.mOffer.serverAddress, DhcpPacket.INADDR_BROADCAST);
        }

        @Override
        protected void receivePacket(DhcpPacket packet) {
            if (DhcpClient.this.isValidPacket(packet)) {
                if (packet instanceof DhcpAckPacket) {
                    DhcpResults results = packet.toDhcpResults();
                    if (results == null) {
                        return;
                    }
                    DhcpClient.this.setDhcpLeaseExpiry(packet);
                    DhcpClient.this.acceptDhcpResults(results, "Confirmed");
                    DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
                    return;
                }
                if (!(packet instanceof DhcpNakPacket)) {
                    return;
                }
                Log.d(DhcpClient.TAG, "Received NAK, returning to INIT");
                DhcpClient.this.mOffer = null;
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
            }
        }

        @Override
        protected void timeout() {
            if (DhcpClient.this.mIsAutoIpEnabled && DhcpClient.this.performAutoIP()) {
                DhcpClient.this.mOffer = null;
                DhcpClient.this.notifySuccess();
                DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
                return;
            }
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
        }
    }

    class DhcpHaveLeaseState extends LoggingState {
        DhcpHaveLeaseState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_EXPIRE_DHCP:
                    Log.d(DhcpClient.TAG, "Lease expired!");
                    DhcpClient.this.notifyFailure();
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            DhcpClient.this.mRenewAlarm.cancel();
            DhcpClient.this.mRebindAlarm.cancel();
            DhcpClient.this.mExpiryAlarm.cancel();
            DhcpClient.this.clearDhcpState();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_CLEAR_LINKADDRESS);
        }
    }

    class ConfiguringInterfaceState extends LoggingState {
        ConfiguringInterfaceState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_CONFIGURE_LINKADDRESS, DhcpClient.this.mDhcpLease.ipAddress);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.EVENT_LINKADDRESS_CONFIGURED:
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
                    return true;
                default:
                    return false;
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
            if (DhcpClient.this.mDhcpLease.serverAddress != null && !DhcpClient.this.connectUdpSock(DhcpClient.this.mDhcpLease.serverAddress)) {
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
            }
            DhcpClient.this.scheduleLeaseTimers();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_RENEW_DHCP:
                    if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                        DhcpClient.this.transitionTo(DhcpClient.this.mWaitBeforeRenewalState);
                        return true;
                    }
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRenewingState);
                    return true;
                default:
                    return false;
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
            DhcpClient.this.startNewTransaction();
        }

        @Override
        protected boolean sendPacket() {
            return DhcpClient.this.sendRequestPacket((Inet4Address) DhcpClient.this.mDhcpLease.ipAddress.getAddress(), DhcpPacket.INADDR_ANY, null, packetDestination());
        }

        @Override
        protected void receivePacket(DhcpPacket packet) {
            if (DhcpClient.this.isValidPacket(packet)) {
                if (packet instanceof DhcpAckPacket) {
                    DhcpResults results = packet.toDhcpResults();
                    if (results == null) {
                        return;
                    }
                    if (!DhcpClient.this.mDhcpLease.ipAddress.equals(results.ipAddress)) {
                        Log.d(DhcpClient.TAG, "Renewed lease not for our current IP address!");
                        DhcpClient.this.notifyFailure();
                        DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                    }
                    DhcpClient.this.setDhcpLeaseExpiry(packet);
                    DhcpClient.this.acceptDhcpResults(results, this.mLeaseMsg);
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
                    return;
                }
                if (!(packet instanceof DhcpNakPacket)) {
                    return;
                }
                Log.d(DhcpClient.TAG, "Received NAK, returning to INIT");
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
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
                return true;
            }
            switch (message.what) {
                case DhcpClient.CMD_REBIND_DHCP:
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRebindingState);
                    break;
            }
            return true;
        }

        @Override
        protected Inet4Address packetDestination() {
            return DhcpClient.this.mDhcpLease.serverAddress != null ? DhcpClient.this.mDhcpLease.serverAddress : DhcpPacket.INADDR_BROADCAST;
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
            DhcpClient.closeQuietly(DhcpClient.this.mUdpSock);
            if (DhcpClient.this.initUdpSocket()) {
                return;
            }
            Log.e(DhcpClient.TAG, "Failed to recreate UDP socket");
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
        }

        @Override
        protected Inet4Address packetDestination() {
            return DhcpPacket.INADDR_BROADCAST;
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

    private boolean performAutoIP() throws Throwable {
        InetAddress ipAddress;
        ArpPeer ap;
        Random random = new Random();
        byte[] autoIp = {-87, -2, 10, 10};
        for (int i = 0; i < 5; i++) {
            autoIp[2] = new Integer(random.nextInt(256)).byteValue();
            autoIp[3] = new Integer(random.nextInt(254) + 1).byteValue();
            ArpPeer ap2 = null;
            try {
                try {
                    ipAddress = InetAddress.getByAddress(autoIp);
                    Log.d(TAG, "performAutoIP(" + i + ") = " + ipAddress.getHostAddress());
                    ap = new ArpPeer(this.mIfaceName, Inet4Address.ANY, ipAddress);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (ErrnoException e) {
                ee = e;
            } catch (IllegalArgumentException e2) {
                ie = e2;
            } catch (SocketException e3) {
                se = e3;
            } catch (UnknownHostException e4) {
                ue = e4;
            }
            try {
                byte[] arpResult = ap.doArp(5000);
                if (arpResult == null) {
                    this.mDhcpLease = new DhcpResults();
                    this.mDhcpLease.ipAddress = new LinkAddress(ipAddress, 16);
                    this.mDhcpLease.leaseDuration = -1;
                    setIpAddress(this.mDhcpLease.ipAddress);
                    Log.d(TAG, "performAutoIP done");
                    if (ap != null) {
                        ap.close();
                    }
                    return true;
                }
                Log.d(TAG, "DAD detected!!");
                if (ap != null) {
                    ap.close();
                }
            } catch (ErrnoException e5) {
                ee = e5;
                ap2 = ap;
                Log.d(TAG, "err :" + ee);
                if (ap2 != null) {
                    ap2.close();
                }
            } catch (IllegalArgumentException e6) {
                ie = e6;
                ap2 = ap;
                Log.d(TAG, "err :" + ie);
                if (ap2 != null) {
                    ap2.close();
                }
            } catch (SocketException e7) {
                se = e7;
                ap2 = ap;
                Log.d(TAG, "err :" + se);
                if (ap2 != null) {
                    ap2.close();
                }
            } catch (UnknownHostException e8) {
                ue = e8;
                ap2 = ap;
                Log.d(TAG, "err :" + ue);
                if (ap2 != null) {
                    ap2.close();
                }
            } catch (Throwable th2) {
                th = th2;
                ap2 = ap;
                if (ap2 != null) {
                    ap2.close();
                }
                throw th;
            }
        }
        return false;
    }

    private boolean setIpAddress(LinkAddress address) {
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        ifcg.setLinkAddress(address);
        try {
            IBinder b = ServiceManager.getService("network_management");
            INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
            service.setInterfaceConfig(this.mIfaceName, ifcg);
            return true;
        } catch (RemoteException | IllegalStateException e) {
            Log.e(TAG, "Error configuring IP address " + address + ": ", e);
            return false;
        }
    }

    private boolean doIpRecover() throws Throwable {
        ArpPeer ap;
        byte[] arpResult;
        if (!this.mIsIpRecoverEnabled) {
            Log.d(TAG, "IP recover: it was disabled");
            return false;
        }
        if (this.mPastDhcpLease == null) {
            Log.d(TAG, "IP recover: mPastDhcpLease is empty");
            return false;
        }
        Log.d(TAG, "IP recover: mPastDhcpLease = " + this.mPastDhcpLease);
        long reCaculatedLeaseMillis = this.mPastDhcpLease.systemExpiredTime - SystemClock.elapsedRealtime();
        Log.d(TAG, "IP recover: reCaculatedLeaseMillis = " + reCaculatedLeaseMillis);
        if (reCaculatedLeaseMillis < 0) {
            return false;
        }
        this.mDhcpLeaseExpiry = SystemClock.elapsedRealtime() + reCaculatedLeaseMillis;
        Log.d(TAG, "IP recover: mDhcpLeaseExpiry = " + this.mDhcpLeaseExpiry);
        ArpPeer ap2 = null;
        try {
            try {
                InetAddress ipAddress = this.mPastDhcpLease.ipAddress.getAddress();
                Log.d(TAG, "IP recover: arp address = " + ipAddress.getHostAddress());
                ap = new ArpPeer(this.mIfaceName, Inet4Address.ANY, ipAddress);
            } catch (Throwable th) {
                th = th;
            }
        } catch (ErrnoException e) {
            ee = e;
        } catch (IllegalArgumentException e2) {
            ie = e2;
        } catch (SocketException e3) {
            se = e3;
        }
        try {
            arpResult = ap.doArp(5000);
        } catch (ErrnoException e4) {
            ee = e4;
            ap2 = ap;
            Log.d(TAG, "err :" + ee);
            if (ap2 != null) {
                ap2.close();
            }
        } catch (IllegalArgumentException e5) {
            ie = e5;
            ap2 = ap;
            Log.d(TAG, "err :" + ie);
            if (ap2 != null) {
                ap2.close();
            }
        } catch (SocketException e6) {
            se = e6;
            ap2 = ap;
            Log.d(TAG, "err :" + se);
            if (ap2 != null) {
                ap2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            ap2 = ap;
            if (ap2 != null) {
                ap2.close();
            }
            throw th;
        }
        if (arpResult != null) {
            Log.d(TAG, "doIpRecover DAD detected!!");
            if (ap != null) {
                ap.close();
            }
            ap2 = ap;
            return false;
        }
        int leaseDuration = ((int) reCaculatedLeaseMillis) / 1000;
        this.mPastDhcpLease.setLeaseDuration(leaseDuration);
        acceptDhcpResults(this.mPastDhcpLease, "Confirmed");
        Log.d(TAG, "doIpRecover no arp response, IP can be reused");
        if (ap != null) {
            ap.close();
        }
        return true;
    }
}
