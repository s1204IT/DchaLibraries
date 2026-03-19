package com.android.okhttp.internal;

import com.android.okhttp.Protocol;
import com.android.okhttp.internal.tls.RealTrustRootIndex;
import com.android.okhttp.internal.tls.TrustRootIndex;
import com.android.okhttp.okio.Buffer;
import dalvik.system.SocketTagger;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class Platform {
    private static final AtomicReference<Platform> INSTANCE_HOLDER = new AtomicReference<>(new Platform());
    private static final OptionalMethod<Socket> SET_USE_SESSION_TICKETS = new OptionalMethod<>(null, "setUseSessionTickets", Boolean.TYPE);
    private static final OptionalMethod<Socket> SET_HOSTNAME = new OptionalMethod<>(null, "setHostname", String.class);
    private static final OptionalMethod<Socket> GET_ALPN_SELECTED_PROTOCOL = new OptionalMethod<>(byte[].class, "getAlpnSelectedProtocol", new Class[0]);
    private static final OptionalMethod<Socket> SET_ALPN_PROTOCOLS = new OptionalMethod<>(null, "setAlpnProtocols", byte[].class);

    protected Platform() {
    }

    public static Platform get() {
        return INSTANCE_HOLDER.get();
    }

    public static Platform getAndSetForTest(Platform platform) {
        if (platform == null) {
            throw new NullPointerException();
        }
        return INSTANCE_HOLDER.getAndSet(platform);
    }

    public void logW(String str) {
        System.logW(str);
    }

    public void tagSocket(Socket socket) throws SocketException {
        SocketTagger.get().tag(socket);
    }

    public void untagSocket(Socket socket) throws SocketException {
        SocketTagger.get().untag(socket);
    }

    public void configureTlsExtensions(SSLSocket sSLSocket, String str, List<Protocol> list) {
        if (str != null) {
            SET_USE_SESSION_TICKETS.invokeOptionalWithoutCheckedException(sSLSocket, true);
            SET_HOSTNAME.invokeOptionalWithoutCheckedException(sSLSocket, str);
        }
        boolean zIsSupported = SET_ALPN_PROTOCOLS.isSupported(sSLSocket);
        if (!zIsSupported) {
            return;
        }
        Object[] objArr = {concatLengthPrefixed(list)};
        if (zIsSupported) {
            SET_ALPN_PROTOCOLS.invokeWithoutCheckedException(sSLSocket, objArr);
        }
    }

    public void afterHandshake(SSLSocket sSLSocket) {
    }

    public String getSelectedProtocol(SSLSocket sSLSocket) {
        byte[] bArr;
        if (!GET_ALPN_SELECTED_PROTOCOL.isSupported(sSLSocket) || (bArr = (byte[]) GET_ALPN_SELECTED_PROTOCOL.invokeWithoutCheckedException(sSLSocket, new Object[0])) == null) {
            return null;
        }
        return new String(bArr, Util.UTF_8);
    }

    public void connectSocket(Socket socket, InetSocketAddress inetSocketAddress, int i) throws IOException {
        socket.connect(inetSocketAddress, i);
    }

    public String getPrefix() {
        return "X-Android";
    }

    public X509TrustManager trustManager(SSLSocketFactory sSLSocketFactory) {
        try {
            return (X509TrustManager) readFieldOrNull(readFieldOrNull(sSLSocketFactory, Class.forName("com.android.org.conscrypt.SSLParametersImpl"), "sslParameters"), X509TrustManager.class, "x509TrustManager");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public TrustRootIndex trustRootIndex(X509TrustManager x509TrustManager) {
        return new RealTrustRootIndex(x509TrustManager.getAcceptedIssuers());
    }

    private static <T> T readFieldOrNull(Object obj, Class<T> cls, String str) {
        Object fieldOrNull;
        for (Class<?> superclass = obj.getClass(); superclass != Object.class; superclass = superclass.getSuperclass()) {
            try {
                Field declaredField = superclass.getDeclaredField(str);
                declaredField.setAccessible(true);
                Object obj2 = declaredField.get(obj);
                if (obj2 != null && cls.isInstance(obj2)) {
                    return cls.cast(obj2);
                }
                return null;
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (NoSuchFieldException e2) {
            }
        }
        if (str.equals("delegate") || (fieldOrNull = readFieldOrNull(obj, Object.class, "delegate")) == null) {
            return null;
        }
        return (T) readFieldOrNull(fieldOrNull, cls, str);
    }

    static byte[] concatLengthPrefixed(List<Protocol> list) {
        Buffer buffer = new Buffer();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Protocol protocol = list.get(i);
            if (protocol != Protocol.HTTP_1_0) {
                buffer.writeByte(protocol.toString().length());
                buffer.writeUtf8(protocol.toString());
            }
        }
        return buffer.readByteArray();
    }
}
