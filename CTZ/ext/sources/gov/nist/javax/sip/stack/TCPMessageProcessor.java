package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.address.ParameterNames;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import javax.sip.ListeningPoint;

public class TCPMessageProcessor extends MessageProcessor {
    private ArrayList<TCPMessageChannel> incomingTcpMessageChannels;
    private boolean isRunning;
    protected int nConnections;
    private ServerSocket sock;
    private Hashtable tcpMessageChannels;
    protected int useCount;

    protected TCPMessageProcessor(InetAddress inetAddress, SIPTransactionStack sIPTransactionStack, int i) {
        super(inetAddress, i, ParameterNames.TCP, sIPTransactionStack);
        this.sipStack = sIPTransactionStack;
        this.tcpMessageChannels = new Hashtable();
        this.incomingTcpMessageChannels = new ArrayList<>();
    }

    @Override
    public void start() throws IOException {
        Thread thread = new Thread(this);
        thread.setName("TCPMessageProcessorThread");
        thread.setPriority(10);
        thread.setDaemon(true);
        this.sock = this.sipStack.getNetworkLayer().createServerSocket(getPort(), 0, getIpAddress());
        if (getIpAddress().getHostAddress().equals("0.0.0.0") || getIpAddress().getHostAddress().equals("::0")) {
            super.setIpAddress(this.sock.getInetAddress());
        }
        this.isRunning = true;
        thread.start();
    }

    @Override
    public void run() {
        while (this.isRunning) {
            try {
            } catch (SocketException e) {
                this.isRunning = false;
            } catch (IOException e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    getSIPStack().getStackLogger().logException(e2);
                }
            } catch (Exception e3) {
                InternalErrorHandler.handleException(e3);
            }
            synchronized (this) {
                while (this.sipStack.maxConnections != -1 && this.nConnections >= this.sipStack.maxConnections) {
                    try {
                        wait();
                        if (!this.isRunning) {
                            return;
                        }
                    } catch (InterruptedException e4) {
                    }
                    this.isRunning = false;
                }
                this.nConnections++;
            }
            Socket socketAccept = this.sock.accept();
            if (this.sipStack.isLoggingEnabled()) {
                getSIPStack().getStackLogger().logDebug("Accepting new connection!");
            }
            this.incomingTcpMessageChannels.add(new TCPMessageChannel(socketAccept, this.sipStack, this));
        }
    }

    @Override
    public String getTransport() {
        return ParameterNames.TCP;
    }

    @Override
    public SIPTransactionStack getSIPStack() {
        return this.sipStack;
    }

    @Override
    public synchronized void stop() {
        this.isRunning = false;
        try {
            this.sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Iterator it = this.tcpMessageChannels.values().iterator();
        while (it.hasNext()) {
            ((TCPMessageChannel) it.next()).close();
        }
        Iterator<TCPMessageChannel> it2 = this.incomingTcpMessageChannels.iterator();
        while (it2.hasNext()) {
            it2.next().close();
        }
        notify();
    }

    protected synchronized void remove(TCPMessageChannel tCPMessageChannel) {
        String key = tCPMessageChannel.getKey();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(Thread.currentThread() + " removing " + key);
        }
        if (this.tcpMessageChannels.get(key) == tCPMessageChannel) {
            this.tcpMessageChannels.remove(key);
        }
        this.incomingTcpMessageChannels.remove(tCPMessageChannel);
    }

    @Override
    public synchronized MessageChannel createMessageChannel(HostPort hostPort) throws IOException {
        String key = MessageChannel.getKey(hostPort, ListeningPoint.TCP);
        if (this.tcpMessageChannels.get(key) != null) {
            return (TCPMessageChannel) this.tcpMessageChannels.get(key);
        }
        TCPMessageChannel tCPMessageChannel = new TCPMessageChannel(hostPort.getInetAddress(), hostPort.getPort(), this.sipStack, this);
        this.tcpMessageChannels.put(key, tCPMessageChannel);
        tCPMessageChannel.isCached = true;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("key " + key);
            this.sipStack.getStackLogger().logDebug("Creating " + tCPMessageChannel);
        }
        return tCPMessageChannel;
    }

    protected synchronized void cacheMessageChannel(TCPMessageChannel tCPMessageChannel) {
        String key = tCPMessageChannel.getKey();
        TCPMessageChannel tCPMessageChannel2 = (TCPMessageChannel) this.tcpMessageChannels.get(key);
        if (tCPMessageChannel2 != null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Closing " + key);
            }
            tCPMessageChannel2.close();
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Caching " + key);
        }
        this.tcpMessageChannels.put(key, tCPMessageChannel);
    }

    @Override
    public synchronized MessageChannel createMessageChannel(InetAddress inetAddress, int i) throws IOException {
        try {
            String key = MessageChannel.getKey(inetAddress, i, ListeningPoint.TCP);
            if (this.tcpMessageChannels.get(key) != null) {
                return (TCPMessageChannel) this.tcpMessageChannels.get(key);
            }
            TCPMessageChannel tCPMessageChannel = new TCPMessageChannel(inetAddress, i, this.sipStack, this);
            this.tcpMessageChannels.put(key, tCPMessageChannel);
            tCPMessageChannel.isCached = true;
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("key " + key);
                this.sipStack.getStackLogger().logDebug("Creating " + tCPMessageChannel);
            }
            return tCPMessageChannel;
        } catch (UnknownHostException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public int getMaximumMessageSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean inUse() {
        return this.useCount != 0;
    }

    @Override
    public int getDefaultTargetPort() {
        return 5060;
    }

    @Override
    public boolean isSecure() {
        return false;
    }
}
