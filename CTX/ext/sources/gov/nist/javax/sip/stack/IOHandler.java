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

    protected static String makeKey(InetAddress inetAddress, int i) {
        return inetAddress.getHostAddress() + Separators.COLON + i;
    }

    protected IOHandler(SIPTransactionStack sIPTransactionStack) {
        this.sipStack = (SipStackImpl) sIPTransactionStack;
    }

    protected void putSocket(String str, Socket socket) {
        this.socketTable.put(str, socket);
    }

    protected Socket getSocket(String str) {
        return this.socketTable.get(str);
    }

    protected void removeSocket(String str) {
        this.socketTable.remove(str);
    }

    private void writeChunks(OutputStream outputStream, byte[] bArr, int i) throws IOException {
        int i2;
        synchronized (outputStream) {
            int i3 = 0;
            while (i3 < i) {
                int i4 = i3 + HTMLModels.M_LEGEND;
                if (i4 >= i) {
                    i2 = i - i3;
                } else {
                    i2 = 8192;
                }
                outputStream.write(bArr, i3, i2);
                i3 = i4;
            }
        }
        outputStream.flush();
    }

    public SocketAddress obtainLocalAddress(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        String strMakeKey = makeKey(inetAddress, i);
        Socket socket = getSocket(strMakeKey);
        if (socket == null) {
            socket = this.sipStack.getNetworkLayer().createSocket(inetAddress, i, inetAddress2, i2);
            putSocket(strMakeKey, socket);
        }
        return socket.getLocalSocketAddress();
    }

    public Socket sendBytes(InetAddress inetAddress, InetAddress inetAddress2, int i, String str, byte[] bArr, boolean z, MessageChannel messageChannel) throws IOException {
        int i2 = z ? 2 : 1;
        int length = bArr.length;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendBytes " + str + " inAddr " + inetAddress2.getHostAddress() + " port = " + i + " length = " + length);
        }
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        int i3 = 0;
        if (str.compareToIgnoreCase(TCP) == 0) {
            String strMakeKey = makeKey(inetAddress2, i);
            try {
                if (!this.ioSemaphore.tryAcquire(10000L, TimeUnit.MILLISECONDS)) {
                    throw new IOException("Could not acquire IO Semaphore after 10 seconds -- giving up ");
                }
                Socket socket = getSocket(strMakeKey);
                while (true) {
                    if (i3 >= i2) {
                        break;
                    }
                    if (socket == null) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("inaddr = " + inetAddress2);
                            this.sipStack.getStackLogger().logDebug("port = " + i);
                        }
                        socket = this.sipStack.getNetworkLayer().createSocket(inetAddress2, i, inetAddress);
                        writeChunks(socket.getOutputStream(), bArr, length);
                        putSocket(strMakeKey, socket);
                    } else {
                        try {
                            writeChunks(socket.getOutputStream(), bArr, length);
                            break;
                        } catch (IOException e) {
                            try {
                                if (this.sipStack.isLoggingEnabled()) {
                                    this.sipStack.getStackLogger().logDebug("IOException occured retryCount " + i3);
                                }
                                removeSocket(strMakeKey);
                                try {
                                    socket.close();
                                } catch (Exception e2) {
                                }
                                i3++;
                                socket = null;
                            } finally {
                            }
                        }
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
                Socket socket2 = getSocket(strMakeKey2);
                while (true) {
                    if (i3 >= i2) {
                        break;
                    }
                    if (socket2 == null) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("inaddr = " + inetAddress2);
                            this.sipStack.getStackLogger().logDebug("port = " + i);
                        }
                        socket2 = this.sipStack.getNetworkLayer().createSSLSocket(inetAddress2, i, inetAddress);
                        SSLSocket sSLSocket = (SSLSocket) socket2;
                        HandshakeCompletedListenerImpl handshakeCompletedListenerImpl = new HandshakeCompletedListenerImpl((TLSMessageChannel) messageChannel);
                        ((TLSMessageChannel) messageChannel).setHandshakeCompletedListener(handshakeCompletedListenerImpl);
                        sSLSocket.addHandshakeCompletedListener(handshakeCompletedListenerImpl);
                        sSLSocket.setEnabledProtocols(this.sipStack.getEnabledProtocols());
                        sSLSocket.startHandshake();
                        writeChunks(socket2.getOutputStream(), bArr, length);
                        putSocket(strMakeKey2, socket2);
                    } else {
                        try {
                            writeChunks(socket2.getOutputStream(), bArr, length);
                            break;
                        } catch (IOException e4) {
                            try {
                                if (this.sipStack.isLoggingEnabled()) {
                                    this.sipStack.getStackLogger().logException(e4);
                                }
                                removeSocket(strMakeKey2);
                                try {
                                    socket2.close();
                                } catch (Exception e5) {
                                }
                                i3++;
                                socket2 = null;
                            } finally {
                            }
                        }
                    }
                }
                if (socket2 == null) {
                    throw new IOException("Could not connect to " + inetAddress2 + Separators.COLON + i);
                }
                return socket2;
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
        Enumeration<Socket> enumerationElements = this.socketTable.elements();
        while (enumerationElements.hasMoreElements()) {
            try {
                enumerationElements.nextElement().close();
            } catch (IOException e) {
            }
        }
    }
}
