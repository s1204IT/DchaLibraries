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
                    chunk = 8192;
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

    public Socket sendBytes(InetAddress senderAddress, InetAddress receiverAddress, int contactPort, String transport, byte[] bytes, boolean retry, MessageChannel messageChannel) throws IOException {
        int retry_count = 0;
        int max_retry = retry ? 2 : 1;
        int length = bytes.length;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendBytes " + transport + " inAddr " + receiverAddress.getHostAddress() + " port = " + contactPort + " length = " + length);
        }
        if (this.sipStack.isLoggingEnabled() && this.sipStack.isLogStackTraceOnMessageSend()) {
            this.sipStack.getStackLogger().logStackTrace(16);
        }
        if (transport.compareToIgnoreCase(TCP) == 0) {
            String key = makeKey(receiverAddress, contactPort);
            try {
                boolean retval = this.ioSemaphore.tryAcquire(10000L, TimeUnit.MILLISECONDS);
                if (!retval) {
                    throw new IOException("Could not acquire IO Semaphore after 10 seconds -- giving up ");
                }
                Socket clientSock = getSocket(key);
                while (true) {
                    if (retry_count >= max_retry) {
                        break;
                    }
                    if (clientSock == null) {
                        break;
                    }
                    try {
                        try {
                            OutputStream outputStream = clientSock.getOutputStream();
                            writeChunks(outputStream, bytes, length);
                            break;
                        } catch (IOException e) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("IOException occured retryCount " + retry_count);
                            }
                            removeSocket(key);
                            try {
                                clientSock.close();
                            } catch (Exception e2) {
                            }
                            clientSock = null;
                            retry_count++;
                        }
                    } finally {
                    }
                }
                if (clientSock != null) {
                    return clientSock;
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug(this.socketTable.toString());
                    this.sipStack.getStackLogger().logError("Could not connect to " + receiverAddress + Separators.COLON + contactPort);
                }
                throw new IOException("Could not connect to " + receiverAddress + Separators.COLON + contactPort);
            } catch (InterruptedException e3) {
                throw new IOException("exception in acquiring sem");
            }
        }
        if (transport.compareToIgnoreCase(TLS) == 0) {
            String key2 = makeKey(receiverAddress, contactPort);
            try {
                boolean retval2 = this.ioSemaphore.tryAcquire(10000L, TimeUnit.MILLISECONDS);
                if (!retval2) {
                    throw new IOException("Timeout acquiring IO SEM");
                }
                Socket clientSock2 = getSocket(key2);
                while (true) {
                    if (retry_count >= max_retry) {
                        break;
                    }
                    if (clientSock2 == null) {
                        break;
                    }
                    try {
                        try {
                            OutputStream outputStream2 = clientSock2.getOutputStream();
                            writeChunks(outputStream2, bytes, length);
                            break;
                        } catch (IOException ex) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logException(ex);
                            }
                            removeSocket(key2);
                            try {
                                clientSock2.close();
                            } catch (Exception e4) {
                            }
                            clientSock2 = null;
                            retry_count++;
                        }
                    } finally {
                    }
                }
                if (clientSock2 == null) {
                    throw new IOException("Could not connect to " + receiverAddress + Separators.COLON + contactPort);
                }
                return clientSock2;
            } catch (InterruptedException e5) {
                throw new IOException("exception in acquiring sem");
            }
        }
        DatagramSocket datagramSock = this.sipStack.getNetworkLayer().createDatagramSocket();
        datagramSock.connect(receiverAddress, contactPort);
        DatagramPacket dgPacket = new DatagramPacket(bytes, 0, length, receiverAddress, contactPort);
        datagramSock.send(dgPacket);
        datagramSock.close();
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
