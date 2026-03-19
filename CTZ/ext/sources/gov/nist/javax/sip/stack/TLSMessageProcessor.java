package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;
import gov.nist.javax.sip.SipStackImpl;
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
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.sip.ListeningPoint;

public class TLSMessageProcessor extends MessageProcessor {
    private ArrayList<TLSMessageChannel> incomingTlsMessageChannels;
    private boolean isRunning;
    protected int nConnections;
    private ServerSocket sock;
    private Hashtable<String, TLSMessageChannel> tlsMessageChannels;
    protected int useCount;

    protected TLSMessageProcessor(InetAddress inetAddress, SIPTransactionStack sIPTransactionStack, int i) {
        super(inetAddress, i, ParameterNames.TLS, sIPTransactionStack);
        this.useCount = 0;
        this.sipStack = sIPTransactionStack;
        this.tlsMessageChannels = new Hashtable<>();
        this.incomingTlsMessageChannels = new ArrayList<>();
    }

    @Override
    public void start() throws IOException {
        Thread thread = new Thread(this);
        thread.setName("TLSMessageProcessorThread");
        thread.setPriority(10);
        thread.setDaemon(true);
        this.sock = this.sipStack.getNetworkLayer().createSSLServerSocket(getPort(), 0, getIpAddress());
        ((SSLServerSocket) this.sock).setNeedClientAuth(false);
        ((SSLServerSocket) this.sock).setUseClientMode(false);
        ((SSLServerSocket) this.sock).setWantClientAuth(true);
        ((SSLServerSocket) this.sock).setEnabledCipherSuites(((SipStackImpl) this.sipStack).getEnabledCipherSuites());
        ((SSLServerSocket) this.sock).setWantClientAuth(true);
        this.isRunning = true;
        thread.start();
    }

    @Override
    public void run() {
        while (this.isRunning) {
            try {
            } catch (SocketException e) {
                if (!this.isRunning) {
                }
            } catch (SSLException e2) {
                this.isRunning = false;
                this.sipStack.getStackLogger().logError("Fatal - SSSLException occured while Accepting connection", e2);
                return;
            } catch (IOException e3) {
                this.sipStack.getStackLogger().logError("Problem Accepting Connection", e3);
            } catch (Exception e4) {
                this.sipStack.getStackLogger().logError("Unexpected Exception!", e4);
            }
            synchronized (this) {
                while (this.sipStack.maxConnections != -1 && this.nConnections >= this.sipStack.maxConnections) {
                    try {
                        wait();
                        if (!this.isRunning) {
                            return;
                        }
                    } catch (InterruptedException e5) {
                    }
                    if (!this.isRunning) {
                        this.sipStack.getStackLogger().logError("Fatal - SocketException occured while Accepting connection", e);
                        this.isRunning = false;
                        return;
                    }
                }
                this.nConnections++;
            }
            Socket socketAccept = this.sock.accept();
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Accepting new connection!");
            }
            this.incomingTlsMessageChannels.add(new TLSMessageChannel(socketAccept, this.sipStack, this));
        }
    }

    @Override
    public SIPTransactionStack getSIPStack() {
        return this.sipStack;
    }

    @Override
    public synchronized void stop() {
        if (this.isRunning) {
            this.isRunning = false;
            try {
                this.sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Iterator<TLSMessageChannel> it = this.tlsMessageChannels.values().iterator();
            while (it.hasNext()) {
                it.next().close();
            }
            Iterator<TLSMessageChannel> it2 = this.incomingTlsMessageChannels.iterator();
            while (it2.hasNext()) {
                it2.next().close();
            }
            notify();
        }
    }

    protected synchronized void remove(TLSMessageChannel tLSMessageChannel) {
        String key = tLSMessageChannel.getKey();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(Thread.currentThread() + " removing " + key);
        }
        if (this.tlsMessageChannels.get(key) == tLSMessageChannel) {
            this.tlsMessageChannels.remove(key);
        }
        this.incomingTlsMessageChannels.remove(tLSMessageChannel);
    }

    @Override
    public synchronized MessageChannel createMessageChannel(HostPort hostPort) throws IOException {
        String key = MessageChannel.getKey(hostPort, ListeningPoint.TLS);
        if (this.tlsMessageChannels.get(key) != null) {
            return this.tlsMessageChannels.get(key);
        }
        TLSMessageChannel tLSMessageChannel = new TLSMessageChannel(hostPort.getInetAddress(), hostPort.getPort(), this.sipStack, this);
        this.tlsMessageChannels.put(key, tLSMessageChannel);
        tLSMessageChannel.isCached = true;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("key " + key);
            this.sipStack.getStackLogger().logDebug("Creating " + tLSMessageChannel);
        }
        return tLSMessageChannel;
    }

    protected synchronized void cacheMessageChannel(TLSMessageChannel tLSMessageChannel) {
        String key = tLSMessageChannel.getKey();
        TLSMessageChannel tLSMessageChannel2 = this.tlsMessageChannels.get(key);
        if (tLSMessageChannel2 != null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Closing " + key);
            }
            tLSMessageChannel2.close();
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Caching " + key);
        }
        this.tlsMessageChannels.put(key, tLSMessageChannel);
    }

    @Override
    public synchronized MessageChannel createMessageChannel(InetAddress inetAddress, int i) throws IOException {
        try {
            String key = MessageChannel.getKey(inetAddress, i, ListeningPoint.TLS);
            if (this.tlsMessageChannels.get(key) != null) {
                return this.tlsMessageChannels.get(key);
            }
            TLSMessageChannel tLSMessageChannel = new TLSMessageChannel(inetAddress, i, this.sipStack, this);
            this.tlsMessageChannels.put(key, tLSMessageChannel);
            tLSMessageChannel.isCached = true;
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("key " + key);
                this.sipStack.getStackLogger().logDebug("Creating " + tLSMessageChannel);
            }
            return tLSMessageChannel;
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
        return 5061;
    }

    @Override
    public boolean isSecure() {
        return true;
    }
}
