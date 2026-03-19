package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.ParameterNames;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import org.ccil.cowan.tagsoup.HTMLModels;

class IOHandler {
    private static String TCP = ParameterNames.TCP;
    private static String TLS = ParameterNames.TLS;
    private SipStackImpl sipStack;
    private Semaphore ioSemaphore = new Semaphore(1);
    private ConcurrentHashMap<String, Socket> socketTable = new ConcurrentHashMap<>();

    protected static String makeKey(InetAddress addr, int port) {
        return addr.getHostAddress() + Separators.COLON + port;
    }

    protected IOHandler(SIPTransactionStack sipStack) {
        this.sipStack = (SipStackImpl) sipStack;
    }

    protected void putSocket(String key, Socket sock) {
        this.socketTable.put(key, sock);
    }

    protected Socket getSocket(String key) {
        return this.socketTable.get(key);
    }

    protected void removeSocket(String key) {
        this.socketTable.remove(key);
    }

    private void writeChunks(OutputStream outputStream, byte[] bytes, int length) throws IOException {
        int chunk;
        synchronized (outputStream) {
            for (int p = 0; p < length; p += HTMLModels.M_LEGEND) {
                if (p + HTMLModels.M_LEGEND < length) {
                    chunk = HTMLModels.M_LEGEND;
                } else {
                    chunk = length - p;
                }
                outputStream.write(bytes, p, chunk);
            }
        }
        outputStream.flush();
    }

    public SocketAddress obtainLocalAddress(InetAddress dst, int dstPort, InetAddress localAddress, int localPort) throws IOException {
        String key = makeKey(dst, dstPort);
        Socket clientSock = getSocket(key);
        if (clientSock == null) {
            clientSock = this.sipStack.getNetworkLayer().createSocket(dst, dstPort, localAddress, localPort);
            putSocket(key, clientSock);
        }
        return clientSock.getLocalSocketAddress();
    }

    public Socket sendBytes(InetAddress inetAddress, InetAddress inetAddress2, int i, String str, byte[] bArr, boolean z, MessageChannel messageChannel) throws IOException {
        int i2 = 0;
        int i3 = z ? 2 : 1;
        int length = bArr.length;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendBytes " + str + " inAddr " + inetAddress2.getHostAddress() + " port = " + i + " length = " + length);
        }
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        if (str.compareToIgnoreCase(TCP) == 0) {
            String strMakeKey = makeKey(inetAddress2, i);
            try {
                if (!this.ioSemaphore.tryAcquire(10000L, TimeUnit.MILLISECONDS)) {
                    throw new IOException("Could not acquire IO Semaphore after 10 seconds -- giving up ");
                }
                Socket socket = getSocket(strMakeKey);
                while (true) {
                    if (i2 >= i3) {
                        break;
                    }
                    if (socket == null) {
                        break;
                    }
                    try {
                        try {
                            writeChunks(socket.getOutputStream(), bArr, length);
                            break;
                        } catch (IOException e) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("IOException occured retryCount " + i2);
                            }
                            removeSocket(strMakeKey);
                            try {
                                socket.close();
                            } catch (Exception e2) {
                            }
                            socket = null;
                            i2++;
                        }
                    } finally {
                    }
                }
                if (socket == null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug(this.socketTable.toString());
                        this.sipStack.getStackLogger().logError("Could not connect to " + inetAddress2 + Separators.COLON + i);
                    }
                    throw new IOException("Could not connect to " + inetAddress2 + Separators.COLON + i);
                }
                return socket;
            } catch (InterruptedException e3) {
                throw new IOException("exception in acquiring sem");
            }
        }
        if (str.compareToIgnoreCase(TLS) == 0) {
            String strMakeKey2 = makeKey(inetAddress2, i);
            try {
                if (!this.ioSemaphore.tryAcquire(10000L, TimeUnit.MILLISECONDS)) {
                    throw new IOException("Timeout acquiring IO SEM");
                }
                SSLSocket sSLSocketCreateSSLSocket = getSocket(strMakeKey2);
                while (true) {
                    if (i2 >= i3) {
                        break;
                    }
                    if (sSLSocketCreateSSLSocket == 0) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("inaddr = " + inetAddress2);
                            this.sipStack.getStackLogger().logDebug("port = " + i);
                        }
                        sSLSocketCreateSSLSocket = this.sipStack.getNetworkLayer().createSSLSocket(inetAddress2, i, inetAddress);
                        HandshakeCompletedListenerImpl handshakeCompletedListenerImpl = new HandshakeCompletedListenerImpl((TLSMessageChannel) messageChannel);
                        ((TLSMessageChannel) messageChannel).setHandshakeCompletedListener(handshakeCompletedListenerImpl);
                        sSLSocketCreateSSLSocket.addHandshakeCompletedListener(handshakeCompletedListenerImpl);
                        sSLSocketCreateSSLSocket.setEnabledProtocols(this.sipStack.getEnabledProtocols());
                        sSLSocketCreateSSLSocket.startHandshake();
                        writeChunks(sSLSocketCreateSSLSocket.getOutputStream(), bArr, length);
                        putSocket(strMakeKey2, sSLSocketCreateSSLSocket);
                    } else {
                        try {
                            writeChunks(sSLSocketCreateSSLSocket.getOutputStream(), bArr, length);
                            break;
                        } catch (IOException e4) {
                            try {
                                if (this.sipStack.isLoggingEnabled()) {
                                    this.sipStack.getStackLogger().logException(e4);
                                }
                                removeSocket(strMakeKey2);
                                try {
                                    sSLSocketCreateSSLSocket.close();
                                } catch (Exception e5) {
                                }
                                sSLSocketCreateSSLSocket = 0;
                                i2++;
                            } finally {
                            }
                        }
                    }
                }
                if (sSLSocketCreateSSLSocket == 0) {
                    throw new IOException("Could not connect to " + inetAddress2 + Separators.COLON + i);
                }
                return sSLSocketCreateSSLSocket;
            } catch (InterruptedException e6) {
                throw new IOException("exception in acquiring sem");
            }
        }
        DatagramSocket datagramSocketCreateDatagramSocket = this.sipStack.getNetworkLayer().createDatagramSocket();
        datagramSocketCreateDatagramSocket.connect(inetAddress2, i);
        datagramSocketCreateDatagramSocket.send(new DatagramPacket(bArr, 0, length, inetAddress2, i));
        datagramSocketCreateDatagramSocket.close();
        return null;
    }

    public void closeAll() {
        Enumeration<Socket> values = this.socketTable.elements();
        while (values.hasMoreElements()) {
            Socket s = values.nextElement();
            try {
                s.close();
            } catch (IOException e) {
            }
        }
    }
}
