package com.android.okhttp.internal;

import com.android.okhttp.Protocol;
import com.android.okhttp.okio.Buffer;
import dalvik.system.SocketTagger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import javax.net.ssl.SSLSocket;

public final class Platform {
    private static final Platform PLATFORM = new Platform();
    private static final OptionalMethod<Socket> SET_USE_SESSION_TICKETS = new OptionalMethod<>(null, "setUseSessionTickets", Boolean.TYPE);
    private static final OptionalMethod<Socket> SET_HOSTNAME = new OptionalMethod<>(null, "setHostname", String.class);
    private static final OptionalMethod<Socket> GET_ALPN_SELECTED_PROTOCOL = new OptionalMethod<>(byte[].class, "getAlpnSelectedProtocol", new Class[0]);
    private static final OptionalMethod<Socket> SET_ALPN_PROTOCOLS = new OptionalMethod<>(null, "setAlpnProtocols", byte[].class);

    public static Platform get() {
        return PLATFORM;
    }

    public void logW(String warning) {
        System.logW(warning);
    }

    public void tagSocket(Socket socket) throws SocketException {
        SocketTagger.get().tag(socket);
    }

    public void untagSocket(Socket socket) throws SocketException {
        SocketTagger.get().untag(socket);
    }

    public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> list) throws Throwable {
        if (hostname != null) {
            SET_USE_SESSION_TICKETS.invokeOptionalWithoutCheckedException(sslSocket, true);
            SET_HOSTNAME.invokeOptionalWithoutCheckedException(sslSocket, hostname);
        }
        boolean alpnSupported = SET_ALPN_PROTOCOLS.isSupported(sslSocket);
        if (!alpnSupported) {
            return;
        }
        Object[] parameters = {concatLengthPrefixed(list)};
        if (!alpnSupported) {
            return;
        }
        SET_ALPN_PROTOCOLS.invokeWithoutCheckedException(sslSocket, parameters);
    }

    public void afterHandshake(SSLSocket sslSocket) {
    }

    public String getSelectedProtocol(SSLSocket socket) {
        byte[] alpnResult;
        boolean alpnSupported = GET_ALPN_SELECTED_PROTOCOL.isSupported(socket);
        if (alpnSupported && (alpnResult = (byte[]) GET_ALPN_SELECTED_PROTOCOL.invokeWithoutCheckedException(socket, new Object[0])) != null) {
            return new String(alpnResult, Util.UTF_8);
        }
        return null;
    }

    public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
        socket.connect(address, connectTimeout);
    }

    public String getPrefix() {
        return "X-Android";
    }

    static byte[] concatLengthPrefixed(List<Protocol> list) {
        Buffer result = new Buffer();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Protocol protocol = list.get(i);
            if (protocol != Protocol.HTTP_1_0) {
                result.writeByte(protocol.toString().length());
                result.writeUtf8(protocol.toString());
            }
        }
        return result.readByteArray();
    }
}
