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
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;

public class TLSMessageProcessor extends MessageProcessor {
    private ArrayList<TLSMessageChannel> incomingTlsMessageChannels;
    private boolean isRunning;
    protected int nConnections;
    private ServerSocket sock;
    private Hashtable<String, TLSMessageChannel> tlsMessageChannels;
    protected int useCount;

    protected TLSMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
        super(ipAddress, port, ParameterNames.TLS, sipStack);
        this.useCount = 0;
        this.sipStack = sipStack;
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
        String[] enabledCiphers = ((SipStackImpl) this.sipStack).getEnabledCipherSuites();
        ((SSLServerSocket) this.sock).setEnabledCipherSuites(enabledCiphers);
        ((SSLServerSocket) this.sock).setWantClientAuth(true);
        this.isRunning = true;
        thread.start();
    }

    @Override
    public void run() {
        while (this.isRunning) {
            try {
            } catch (SocketException ex) {
                if (!this.isRunning) {
                }
            } catch (SSLException ex2) {
                this.isRunning = false;
                this.sipStack.getStackLogger().logError("Fatal - SSSLException occured while Accepting connection", ex2);
                return;
            } catch (IOException ex3) {
                this.sipStack.getStackLogger().logError("Problem Accepting Connection", ex3);
            } catch (Exception ex4) {
                this.sipStack.getStackLogger().logError("Unexpected Exception!", ex4);
            }
            synchronized (this) {
                while (this.sipStack.maxConnections != -1 && this.nConnections >= this.sipStack.maxConnections) {
                    try {
                        wait();
                        if (!this.isRunning) {
                            return;
                        }
                    } catch (InterruptedException e) {
                    }
                    if (!this.isRunning) {
                        this.sipStack.getStackLogger().logError("Fatal - SocketException occured while Accepting connection", ex);
                        this.isRunning = false;
                        return;
                    }
                }
                this.nConnections++;
            }
            Socket newsock = this.sock.accept();
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Accepting new connection!");
            }
            this.incomingTlsMessageChannels.add(new TLSMessageChannel(newsock, this.sipStack, this));
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
            for (TLSMessageChannel next : this.tlsMessageChannels.values()) {
                next.close();
            }
            for (TLSMessageChannel next2 : this.incomingTlsMessageChannels) {
                next2.close();
            }
            notify();
        }
    }

    protected synchronized void remove(TLSMessageChannel tlsMessageChannel) {
        String key = tlsMessageChannel.getKey();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(Thread.currentThread() + " removing " + key);
        }
        if (this.tlsMessageChannels.get(key) == tlsMessageChannel) {
            this.tlsMessageChannels.remove(key);
        }
        this.incomingTlsMessageChannels.remove(tlsMessageChannel);
    }

    @Override
    public synchronized MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
        TLSMessageChannel tLSMessageChannel;
        String key = MessageChannel.getKey(targetHostPort, "TLS");
        if (this.tlsMessageChannels.get(key) != null) {
            tLSMessageChannel = this.tlsMessageChannels.get(key);
        } else {
            TLSMessageChannel retval = new TLSMessageChannel(targetHostPort.getInetAddress(), targetHostPort.getPort(), this.sipStack, this);
            this.tlsMessageChannels.put(key, retval);
            retval.isCached = true;
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("key " + key);
                this.sipStack.getStackLogger().logDebug("Creating " + retval);
            }
            tLSMessageChannel = retval;
        }
        return tLSMessageChannel;
    }

    protected synchronized void cacheMessageChannel(TLSMessageChannel messageChannel) {
        String key = messageChannel.getKey();
        TLSMessageChannel currentChannel = this.tlsMessageChannels.get(key);
        if (currentChannel != null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Closing " + key);
            }
            currentChannel.close();
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Caching " + key);
        }
        this.tlsMessageChannels.put(key, messageChannel);
    }

    @Override
    public synchronized MessageChannel createMessageChannel(InetAddress host, int port) throws IOException {
        TLSMessageChannel tLSMessageChannel;
        try {
            String key = MessageChannel.getKey(host, port, "TLS");
            if (this.tlsMessageChannels.get(key) != null) {
                tLSMessageChannel = this.tlsMessageChannels.get(key);
            } else {
                TLSMessageChannel retval = new TLSMessageChannel(host, port, this.sipStack, this);
                this.tlsMessageChannels.put(key, retval);
                retval.isCached = true;
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("key " + key);
                    this.sipStack.getStackLogger().logDebug("Creating " + retval);
                }
                tLSMessageChannel = retval;
            }
        } catch (UnknownHostException ex) {
            throw new IOException(ex.getMessage());
        }
        return tLSMessageChannel;
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
