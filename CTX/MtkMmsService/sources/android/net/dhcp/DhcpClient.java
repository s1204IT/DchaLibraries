package android.net.dhcp;

import android.content.Context;
import android.net.DhcpResults;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.dhcp.DhcpPacket;
import android.net.metrics.DhcpClientEvent;
import android.net.metrics.DhcpErrorEvent;
import android.net.metrics.IpConnectivityLog;
import android.net.util.InterfaceParams;
import android.os.Message;
import android.os.SystemClock;
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
import dalvik.system.PathClassLoader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import libcore.io.IoBridge;

public class DhcpClient extends StateMachine {
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
    private InterfaceParams mIface;
    private final String mIfaceName;
    private PacketSocketAddress mInterfaceBroadcastAddr;
    private final WakeupMessage mKickAlarm;
    private long mLastBoundExitTime;
    private long mLastInitEnterTime;
    private final IpConnectivityLog mMetricsLog;
    private DhcpResults mOffer;
    private FileDescriptor mPacketSock;
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
    static final byte[] REQUESTED_PARAMS = {1, 3, 6, 15, 26, 28, 51, 58, 59, 43};

    private WakeupMessage makeWakeupMessage(String str, int i) {
        return new WakeupMessage(this.mContext, getHandler(), DhcpClient.class.getSimpleName() + "." + this.mIfaceName + "." + str, i);
    }

    private DhcpClient(Context context, StateMachine stateMachine, String str) {
        super("DhcpClient", stateMachine.getHandler());
        this.mMetricsLog = new IpConnectivityLog();
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
        setInitialState(this.mStoppedState);
        this.mRandom = new Random();
        this.mKickAlarm = makeWakeupMessage("KICK", 196709);
        this.mTimeoutAlarm = makeWakeupMessage("TIMEOUT", 196711);
        this.mRenewAlarm = makeWakeupMessage("RENEW", 196712);
        this.mRebindAlarm = makeWakeupMessage("REBIND", 196713);
        this.mExpiryAlarm = makeWakeupMessage("EXPIRY", 196714);
    }

    public void registerForPreDhcpNotification() {
        this.mRegisteredForPreDhcpNotification = true;
    }

    public static DhcpClient makeDhcpClient(Context context, StateMachine stateMachine, InterfaceParams interfaceParams) {
        try {
            Class clsLoadClass = new PathClassLoader("/system/framework/mediatek-framework-net.jar", context.getClassLoader()).loadClass("com.mediatek.net.dhcp.MtkDhcpClient");
            Method declaredMethod = clsLoadClass.getDeclaredMethod("makeDhcpClient", Context.class, StateMachine.class, InterfaceParams.class);
            declaredMethod.setAccessible(true);
            return (DhcpClient) declaredMethod.invoke(clsLoadClass, context, stateMachine, interfaceParams);
        } catch (Exception e) {
            Log.e("DhcpClient", "No MtkDhcpClient! Used AOSP for instead! %s", e);
            DhcpClient dhcpClient = new DhcpClient(context, stateMachine, interfaceParams.name);
            dhcpClient.mIface = interfaceParams;
            dhcpClient.start();
            return dhcpClient;
        }
    }

    private boolean initInterface() {
        if (this.mIface == null) {
            this.mIface = InterfaceParams.getByName(this.mIfaceName);
        }
        if (this.mIface == null) {
            Log.e("DhcpClient", "Can't determine InterfaceParams for " + this.mIfaceName);
            return false;
        }
        this.mHwAddr = this.mIface.macAddr.toByteArray();
        this.mInterfaceBroadcastAddr = new PacketSocketAddress(this.mIface.index, DhcpPacket.ETHER_BROADCAST);
        return true;
    }

    private void startNewTransaction() {
        this.mTransactionId = this.mRandom.nextInt();
        this.mTransactionStartMillis = SystemClock.elapsedRealtime();
    }

    private boolean initSockets() {
        return initPacketSocket() && initUdpSocket();
    }

    private boolean initPacketSocket() {
        try {
            this.mPacketSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IP);
            Os.bind(this.mPacketSock, new PacketSocketAddress((short) OsConstants.ETH_P_IP, this.mIface.index));
            NetworkUtils.attachDhcpFilter(this.mPacketSock);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e("DhcpClient", "Error creating packet socket", e);
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
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e("DhcpClient", "Error creating UDP socket", e);
            return false;
        } finally {
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        }
    }

    private boolean connectUdpSock(Inet4Address inet4Address) {
        try {
            Os.connect(this.mUdpSock, inet4Address, 67);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e("DhcpClient", "Error connecting UDP socket", e);
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
            this.mStopped = true;
            DhcpClient.this.closeSockets();
        }

        @Override
        public void run() {
            int i;
            Log.d("DhcpClient", "Receive thread started");
            while (!this.mStopped) {
                try {
                    try {
                        i = Os.read(DhcpClient.this.mPacketSock, this.mPacket, 0, this.mPacket.length);
                    } catch (DhcpPacket.ParseException e) {
                        e = e;
                        i = 0;
                    }
                    try {
                        DhcpPacket dhcpPacketDecodeFullPacket = DhcpPacket.decodeFullPacket(this.mPacket, i, 0);
                        Log.d("DhcpClient", "Received packet: " + dhcpPacketDecodeFullPacket);
                        DhcpClient.this.sendMessage(196710, dhcpPacketDecodeFullPacket);
                    } catch (DhcpPacket.ParseException e2) {
                        e = e2;
                        Log.e("DhcpClient", "Can't parse packet: " + e.getMessage());
                        Log.d("DhcpClient", HexDump.dumpHexString(this.mPacket, 0, i));
                        if (e.errorCode == DhcpErrorEvent.DHCP_NO_COOKIE) {
                            EventLog.writeEvent(1397638484, "31850211", -1, DhcpPacket.ParseException.class.getName());
                        }
                        DhcpClient.this.logError(e.errorCode);
                    }
                } catch (ErrnoException | IOException e3) {
                    if (!this.mStopped) {
                        Log.e("DhcpClient", "Read error", e3);
                        DhcpClient.this.logError(DhcpErrorEvent.RECEIVE_ERROR);
                    }
                }
            }
            Log.d("DhcpClient", "Receive thread stopped");
        }
    }

    private short getSecs() {
        return (short) ((SystemClock.elapsedRealtime() - this.mTransactionStartMillis) / 1000);
    }

    private boolean transmitPacket(ByteBuffer byteBuffer, String str, int i, Inet4Address inet4Address) {
        try {
            if (i == 0) {
                Log.d("DhcpClient", "Broadcasting " + str);
                Os.sendto(this.mPacketSock, byteBuffer.array(), 0, byteBuffer.limit(), 0, this.mInterfaceBroadcastAddr);
            } else if (i == 2 && inet4Address.equals(DhcpPacket.INADDR_BROADCAST)) {
                Log.d("DhcpClient", "Broadcasting " + str);
                Os.sendto(this.mUdpSock, byteBuffer, 0, inet4Address, 67);
            } else {
                Log.d("DhcpClient", String.format("Unicasting %s to %s", str, Os.getpeername(this.mUdpSock)));
                Os.write(this.mUdpSock, byteBuffer);
            }
            return true;
        } catch (ErrnoException | IOException e) {
            Log.e("DhcpClient", "Can't send packet: ", e);
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
            Log.d("DhcpClient", "Infinite lease, no timer scheduling needed");
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j = this.mDhcpLeaseExpiry - jElapsedRealtime;
        long j2 = j / 2;
        long j3 = (7 * j) / 8;
        this.mRenewAlarm.schedule(jElapsedRealtime + j2);
        this.mRebindAlarm.schedule(jElapsedRealtime + j3);
        this.mExpiryAlarm.schedule(jElapsedRealtime + j);
        Log.d("DhcpClient", "Scheduling renewal in " + (j2 / 1000) + "s");
        Log.d("DhcpClient", "Scheduling rebind in " + (j3 / 1000) + "s");
        Log.d("DhcpClient", "Scheduling expiry in " + (j / 1000) + "s");
    }

    private void notifySuccess() {
        this.mController.sendMessage(196612, 1, 0, new DhcpResults(this.mDhcpLease));
    }

    private void notifyFailure() {
        this.mController.sendMessage(196612, 2, 0, (Object) null);
    }

    private void acceptDhcpResults(DhcpResults dhcpResults, String str) {
        this.mDhcpLease = dhcpResults;
        this.mOffer = null;
        Log.d("DhcpClient", str + " lease: " + this.mDhcpLease);
        notifySuccess();
    }

    private void clearDhcpState() {
        this.mDhcpLease = null;
        this.mDhcpLeaseExpiry = 0L;
        this.mOffer = null;
    }

    public void doQuit() {
        Log.d("DhcpClient", "doQuit");
        quit();
    }

    protected void onQuitting() {
        Log.d("DhcpClient", "onQuitting");
        this.mController.sendMessage(196613);
    }

    abstract class LoggingState extends State {
        private long mEnterTimeMs;

        LoggingState() {
        }

        public void enter() {
            Log.d("DhcpClient", "Entering state " + getName());
            this.mEnterTimeMs = SystemClock.elapsedRealtime();
        }

        public void exit() {
            DhcpClient.this.logState(getName(), (int) (SystemClock.elapsedRealtime() - this.mEnterTimeMs));
        }

        private String messageName(int i) {
            return (String) DhcpClient.sMessageNames.get(i, Integer.toString(i));
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
            Log.d("DhcpClient", getName() + messageToString(message));
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
            DhcpClient.this.mController.sendMessage(196611);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196614) {
                DhcpClient.this.transitionTo(this.mOtherState);
                return true;
            }
            return false;
        }
    }

    class StoppedState extends State {
        StoppedState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 196609) {
                if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                    DhcpClient.this.transitionTo(DhcpClient.this.mWaitBeforeStartState);
                    return true;
                }
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                return true;
            }
            return false;
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

    class DhcpState extends State {
        DhcpState() {
        }

        public void enter() {
            DhcpClient.this.clearDhcpState();
            if (!DhcpClient.this.initInterface() || !DhcpClient.this.initSockets()) {
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
            } else {
                DhcpClient.this.mReceiveThread = DhcpClient.this.new ReceiveThread();
                DhcpClient.this.mReceiveThread.start();
            }
        }

        public void exit() {
            if (DhcpClient.this.mReceiveThread != null) {
                DhcpClient.this.mReceiveThread.halt();
                DhcpClient.this.mReceiveThread = null;
            }
            DhcpClient.this.clearDhcpState();
        }

        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196610) {
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
                return true;
            }
            return false;
        }
    }

    public boolean isValidPacket(DhcpPacket dhcpPacket) {
        int transactionId = dhcpPacket.getTransactionId();
        if (transactionId != this.mTransactionId) {
            Log.d("DhcpClient", "Unexpected transaction ID " + transactionId + ", expected " + this.mTransactionId);
            return false;
        }
        if (!Arrays.equals(dhcpPacket.getClientMac(), this.mHwAddr)) {
            Log.d("DhcpClient", "MAC addr mismatch: got " + HexDump.toHexString(dhcpPacket.getClientMac()) + ", expected " + HexDump.toHexString(dhcpPacket.getClientMac()));
            return false;
        }
        return true;
    }

    public void setDhcpLeaseExpiry(DhcpPacket dhcpPacket) {
        long leaseTimeMillis = dhcpPacket.getLeaseTimeMillis();
        this.mDhcpLeaseExpiry = leaseTimeMillis > 0 ? SystemClock.elapsedRealtime() + leaseTimeMillis : 0L;
    }

    abstract class PacketRetransmittingState extends LoggingState {
        protected int mTimeout;
        private int mTimer;

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
            DhcpClient.this.sendMessage(196709);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case 196709:
                    sendPacket();
                    scheduleKick();
                    break;
                case 196710:
                    receivePacket((DhcpPacket) message.obj);
                    break;
                case 196711:
                    timeout();
                    break;
            }
            return true;
        }

        @Override
        public void exit() {
            super.exit();
            DhcpClient.this.mKickAlarm.cancel();
            DhcpClient.this.mTimeoutAlarm.cancel();
        }

        protected void timeout() {
        }

        protected void initTimer() {
            this.mTimer = 2000;
        }

        protected int jitterTimer(int i) {
            int i2 = i / 10;
            return i + (DhcpClient.this.mRandom.nextInt(2 * i2) - i2);
        }

        protected void scheduleKick() {
            DhcpClient.this.mKickAlarm.schedule(SystemClock.elapsedRealtime() + ((long) jitterTimer(this.mTimer)));
            this.mTimer *= 2;
            if (this.mTimer > 128000) {
                this.mTimer = 128000;
            }
        }

        protected void maybeInitTimeout() {
            if (this.mTimeout > 0) {
                DhcpClient.this.mTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeout));
            }
        }
    }

    class DhcpInitState extends PacketRetransmittingState {
        public DhcpInitState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            DhcpClient.this.startNewTransaction();
            DhcpClient.this.mLastInitEnterTime = SystemClock.elapsedRealtime();
        }

        @Override
        protected boolean sendPacket() {
            return DhcpClient.this.sendDiscoverPacket();
        }

        @Override
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (DhcpClient.this.isValidPacket(dhcpPacket) && (dhcpPacket instanceof DhcpOfferPacket)) {
                DhcpClient.this.mOffer = dhcpPacket.toDhcpResults();
                if (DhcpClient.this.mOffer != null) {
                    Log.d("DhcpClient", "Got pending lease: " + DhcpClient.this.mOffer);
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRequestingState);
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
            return DhcpClient.this.sendRequestPacket(DhcpPacket.INADDR_ANY, (Inet4Address) DhcpClient.this.mOffer.ipAddress.getAddress(), DhcpClient.this.mOffer.serverAddress, DhcpPacket.INADDR_BROADCAST);
        }

        @Override
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (DhcpClient.this.isValidPacket(dhcpPacket)) {
                if (!(dhcpPacket instanceof DhcpAckPacket)) {
                    if (dhcpPacket instanceof DhcpNakPacket) {
                        Log.d("DhcpClient", "Received NAK, returning to INIT");
                        DhcpClient.this.mOffer = null;
                        DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                        return;
                    }
                    return;
                }
                DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                if (dhcpResults != null) {
                    DhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                    DhcpClient.this.acceptDhcpResults(dhcpResults, "Confirmed");
                    DhcpClient.this.transitionTo(DhcpClient.this.mConfiguringInterfaceState);
                }
            }
        }

        @Override
        protected void timeout() {
            DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
        }
    }

    class DhcpHaveLeaseState extends State {
        DhcpHaveLeaseState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 196714) {
                Log.d("DhcpClient", "Lease expired!");
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                return true;
            }
            return false;
        }

        public void exit() {
            DhcpClient.this.mRenewAlarm.cancel();
            DhcpClient.this.mRebindAlarm.cancel();
            DhcpClient.this.mExpiryAlarm.cancel();
            DhcpClient.this.clearDhcpState();
            DhcpClient.this.mController.sendMessage(196615);
        }
    }

    class ConfiguringInterfaceState extends LoggingState {
        ConfiguringInterfaceState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            DhcpClient.this.mController.sendMessage(196616, DhcpClient.this.mDhcpLease.ipAddress);
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196617) {
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
                return true;
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
            if (DhcpClient.this.mDhcpLease.serverAddress != null && !DhcpClient.this.connectUdpSock(DhcpClient.this.mDhcpLease.serverAddress)) {
                DhcpClient.this.notifyFailure();
                DhcpClient.this.transitionTo(DhcpClient.this.mStoppedState);
            }
            DhcpClient.this.scheduleLeaseTimers();
            logTimeToBoundState();
        }

        @Override
        public void exit() {
            super.exit();
            DhcpClient.this.mLastBoundExitTime = SystemClock.elapsedRealtime();
        }

        @Override
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what == 196712) {
                if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                    DhcpClient.this.transitionTo(DhcpClient.this.mWaitBeforeRenewalState);
                    return true;
                }
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRenewingState);
                return true;
            }
            return false;
        }

        private void logTimeToBoundState() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (DhcpClient.this.mLastBoundExitTime > DhcpClient.this.mLastInitEnterTime) {
                DhcpClient.this.logState("RenewingBoundState", (int) (jElapsedRealtime - DhcpClient.this.mLastBoundExitTime));
            } else {
                DhcpClient.this.logState("InitialBoundState", (int) (jElapsedRealtime - DhcpClient.this.mLastInitEnterTime));
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
        protected void receivePacket(DhcpPacket dhcpPacket) {
            if (DhcpClient.this.isValidPacket(dhcpPacket)) {
                if (!(dhcpPacket instanceof DhcpAckPacket)) {
                    if (dhcpPacket instanceof DhcpNakPacket) {
                        Log.d("DhcpClient", "Received NAK, returning to INIT");
                        DhcpClient.this.notifyFailure();
                        DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                        return;
                    }
                    return;
                }
                DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                if (dhcpResults != null) {
                    if (!DhcpClient.this.mDhcpLease.ipAddress.equals(dhcpResults.ipAddress)) {
                        Log.d("DhcpClient", "Renewed lease not for our current IP address!");
                        DhcpClient.this.notifyFailure();
                        DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
                    }
                    DhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                    DhcpClient.this.acceptDhcpResults(dhcpResults, this.mLeaseMsg);
                    DhcpClient.this.transitionTo(DhcpClient.this.mDhcpBoundState);
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
                return true;
            }
            if (message.what == 196713) {
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpRebindingState);
                return true;
            }
            return false;
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
            if (!DhcpClient.this.initUdpSocket()) {
                Log.e("DhcpClient", "Failed to recreate UDP socket");
                DhcpClient.this.transitionTo(DhcpClient.this.mDhcpInitState);
            }
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

    private void logError(int i) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpErrorEvent(i));
    }

    private void logState(String str, int i) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpClientEvent(str, i));
    }
}
