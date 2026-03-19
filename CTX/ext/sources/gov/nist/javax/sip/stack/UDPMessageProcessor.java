package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.ThreadAuditor;
import gov.nist.javax.sip.address.ParameterNames;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import org.ccil.cowan.tagsoup.HTMLModels;

public class UDPMessageProcessor extends MessageProcessor {
    private static final int HIGHWAT = 5000;
    private static final int LOWAT = 2500;
    protected boolean isRunning;
    protected LinkedList messageChannels;
    protected LinkedList messageQueue;
    private int port;
    protected DatagramSocket sock;
    protected int threadPoolSize;

    protected UDPMessageProcessor(InetAddress inetAddress, SIPTransactionStack sIPTransactionStack, int i) throws IOException {
        super(inetAddress, i, ParameterNames.UDP, sIPTransactionStack);
        this.sipStack = sIPTransactionStack;
        this.messageQueue = new LinkedList();
        this.port = i;
        try {
            this.sock = sIPTransactionStack.getNetworkLayer().createDatagramSocket(i, inetAddress);
            this.sock.setReceiveBufferSize(sIPTransactionStack.getReceiveUdpBufferSize());
            this.sock.setSendBufferSize(sIPTransactionStack.getSendUdpBufferSize());
            if (sIPTransactionStack.getThreadAuditor().isEnabled()) {
                this.sock.setSoTimeout((int) sIPTransactionStack.getThreadAuditor().getPingIntervalInMillisecs());
            }
            if (inetAddress.getHostAddress().equals("0.0.0.0") || inetAddress.getHostAddress().equals("::0")) {
                super.setIpAddress(this.sock.getLocalAddress());
            }
        } catch (SocketException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void start() throws IOException {
        this.isRunning = true;
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("UDPMessageProcessorThread");
        thread.setPriority(10);
        thread.start();
    }

    @Override
    public void run() {
        DatagramPacket datagramPacket;
        this.messageChannels = new LinkedList();
        if (this.sipStack.threadPoolSize != -1) {
            for (int i = 0; i < this.sipStack.threadPoolSize; i++) {
                this.messageChannels.add(new UDPMessageChannel(this.sipStack, this));
            }
        }
        ThreadAuditor.ThreadHandle threadHandleAddCurrentThread = this.sipStack.getThreadAuditor().addCurrentThread();
        while (this.isRunning) {
            try {
                threadHandleAddCurrentThread.ping();
                int receiveBufferSize = this.sock.getReceiveBufferSize();
                datagramPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
                this.sock.receive(datagramPacket);
            } catch (SocketException e) {
                if (this.sipStack.isLoggingEnabled()) {
                    getSIPStack().getStackLogger().logDebug("UDPMessageProcessor: Stopping");
                }
                this.isRunning = false;
                synchronized (this.messageQueue) {
                    this.messageQueue.notifyAll();
                }
            } catch (SocketTimeoutException e2) {
            } catch (IOException e3) {
                this.isRunning = false;
                e3.printStackTrace();
                if (this.sipStack.isLoggingEnabled()) {
                    getSIPStack().getStackLogger().logDebug("UDPMessageProcessor: Got an IO Exception");
                }
            } catch (Exception e4) {
                if (this.sipStack.isLoggingEnabled()) {
                    getSIPStack().getStackLogger().logDebug("UDPMessageProcessor: Unexpected Exception - quitting");
                }
                InternalErrorHandler.handleException(e4);
                return;
            }
            if (this.sipStack.stackDoesCongestionControl) {
                if (this.messageQueue.size() >= HIGHWAT) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Dropping message -- queue length exceeded");
                    }
                } else if (this.messageQueue.size() > LOWAT && this.messageQueue.size() < HIGHWAT) {
                    double size = 1.0d - ((double) ((this.messageQueue.size() - LOWAT) / 2500.0f));
                    if (Math.random() > size) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Dropping message with probability  " + size);
                        }
                    }
                }
            }
            if (this.sipStack.threadPoolSize != -1) {
                synchronized (this.messageQueue) {
                    this.messageQueue.add(datagramPacket);
                    this.messageQueue.notify();
                }
            } else {
                new UDPMessageChannel(this.sipStack, this, datagramPacket);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (this.messageQueue) {
            this.isRunning = false;
            this.messageQueue.notifyAll();
            this.sock.close();
        }
    }

    @Override
    public String getTransport() {
        return ParameterNames.UDP;
    }

    @Override
    public SIPTransactionStack getSIPStack() {
        return this.sipStack;
    }

    @Override
    public MessageChannel createMessageChannel(HostPort hostPort) throws UnknownHostException {
        return new UDPMessageChannel(hostPort.getInetAddress(), hostPort.getPort(), this.sipStack, this);
    }

    @Override
    public MessageChannel createMessageChannel(InetAddress inetAddress, int i) throws IOException {
        return new UDPMessageChannel(inetAddress, i, this.sipStack, this);
    }

    @Override
    public int getDefaultTargetPort() {
        return 5060;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public int getMaximumMessageSize() {
        return HTMLModels.M_LEGEND;
    }

    @Override
    public boolean inUse() {
        boolean z;
        synchronized (this.messageQueue) {
            z = this.messageQueue.size() != 0;
        }
        return z;
    }
}
