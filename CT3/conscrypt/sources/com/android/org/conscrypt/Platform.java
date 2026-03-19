package com.android.org.conscrypt;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Collections;
import java.util.List;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import sun.security.x509.AlgorithmId;

class Platform {
    Platform(Platform platform) {
        this();
    }

    private static class NoPreloadHolder {
        public static final Platform MAPPER = new Platform(null);

        private NoPreloadHolder() {
        }
    }

    public static void setup() {
        NoPreloadHolder.MAPPER.ping();
    }

    private void ping() {
    }

    private Platform() {
    }

    public static FileDescriptor getFileDescriptor(Socket s) {
        return s.getFileDescriptor$();
    }

    public static FileDescriptor getFileDescriptorFromSSLSocket(OpenSSLSocketImpl openSSLSocketImpl) {
        try {
            Field f_impl = Socket.class.getDeclaredField("impl");
            f_impl.setAccessible(true);
            Object socketImpl = f_impl.get(openSSLSocketImpl);
            Field f_fd = SocketImpl.class.getDeclaredField("fd");
            f_fd.setAccessible(true);
            return (FileDescriptor) f_fd.get(socketImpl);
        } catch (Exception e) {
            throw new RuntimeException("Can't get FileDescriptor from socket", e);
        }
    }

    public static String getCurveName(ECParameterSpec spec) {
        return spec.getCurveName();
    }

    public static void setCurveName(ECParameterSpec spec, String curveName) {
        spec.setCurveName(curveName);
    }

    public static void setSocketWriteTimeout(Socket s, long timeoutMillis) throws SocketException {
        StructTimeval tv = StructTimeval.fromMillis(timeoutMillis);
        try {
            Os.setsockoptTimeval(s.getFileDescriptor$(), OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, tv);
        } catch (ErrnoException errnoException) {
            throw errnoException.rethrowAsSocketException();
        }
    }

    public static void setSSLParameters(SSLParameters params, SSLParametersImpl impl, OpenSSLSocketImpl socket) {
        impl.setEndpointIdentificationAlgorithm(params.getEndpointIdentificationAlgorithm());
        impl.setUseCipherSuitesOrder(params.getUseCipherSuitesOrder());
        List<SNIServerName> serverNames = params.getServerNames();
        if (serverNames == null) {
            return;
        }
        for (SNIServerName serverName : serverNames) {
            if (serverName.getType() == 0) {
                socket.setHostname(((SNIHostName) serverName).getAsciiName());
                return;
            }
        }
    }

    public static void getSSLParameters(SSLParameters params, SSLParametersImpl impl, OpenSSLSocketImpl socket) {
        params.setEndpointIdentificationAlgorithm(impl.getEndpointIdentificationAlgorithm());
        params.setUseCipherSuitesOrder(impl.getUseCipherSuitesOrder());
        if (!impl.getUseSni() || !AddressUtils.isValidSniHostname(socket.getHostname())) {
            return;
        }
        params.setServerNames(Collections.singletonList(new SNIHostName(socket.getHostname())));
    }

    private static boolean checkTrusted(String methodName, X509TrustManager tm, X509Certificate[] chain, String authType, Class<?> argumentClass, Object argumentInstance) throws CertificateException {
        try {
            Method method = tm.getClass().getMethod(methodName, X509Certificate[].class, String.class, argumentClass);
            method.invoke(tm, chain, authType, argumentInstance);
            return true;
        } catch (IllegalAccessException | NoSuchMethodException e) {
            return false;
        } catch (InvocationTargetException e2) {
            if (e2.getCause() instanceof CertificateException) {
                throw ((CertificateException) e2.getCause());
            }
            throw new RuntimeException(e2.getCause());
        }
    }

    public static void checkClientTrusted(X509TrustManager tm, X509Certificate[] chain, String authType, OpenSSLSocketImpl socket) throws CertificateException {
        if (tm instanceof X509ExtendedTrustManager) {
            X509ExtendedTrustManager x509etm = (X509ExtendedTrustManager) tm;
            x509etm.checkClientTrusted(chain, authType, socket);
        } else {
            if (checkTrusted("checkClientTrusted", tm, chain, authType, Socket.class, socket) || checkTrusted("checkClientTrusted", tm, chain, authType, String.class, socket.getHandshakeSession().getPeerHost())) {
                return;
            }
            tm.checkClientTrusted(chain, authType);
        }
    }

    public static void checkServerTrusted(X509TrustManager tm, X509Certificate[] chain, String authType, OpenSSLSocketImpl socket) throws CertificateException {
        if (tm instanceof X509ExtendedTrustManager) {
            X509ExtendedTrustManager x509etm = (X509ExtendedTrustManager) tm;
            x509etm.checkServerTrusted(chain, authType, socket);
        } else {
            if (checkTrusted("checkServerTrusted", tm, chain, authType, Socket.class, socket) || checkTrusted("checkServerTrusted", tm, chain, authType, String.class, socket.getHandshakeSession().getPeerHost())) {
                return;
            }
            tm.checkServerTrusted(chain, authType);
        }
    }

    public static void checkClientTrusted(X509TrustManager tm, X509Certificate[] chain, String authType, OpenSSLEngineImpl engine) throws CertificateException {
        if (tm instanceof X509ExtendedTrustManager) {
            X509ExtendedTrustManager x509etm = (X509ExtendedTrustManager) tm;
            x509etm.checkClientTrusted(chain, authType, engine);
        } else {
            if (checkTrusted("checkClientTrusted", tm, chain, authType, SSLEngine.class, engine) || checkTrusted("checkClientTrusted", tm, chain, authType, String.class, engine.getHandshakeSession().getPeerHost())) {
                return;
            }
            tm.checkClientTrusted(chain, authType);
        }
    }

    public static void checkServerTrusted(X509TrustManager tm, X509Certificate[] chain, String authType, OpenSSLEngineImpl engine) throws CertificateException {
        if (tm instanceof X509ExtendedTrustManager) {
            X509ExtendedTrustManager x509etm = (X509ExtendedTrustManager) tm;
            x509etm.checkServerTrusted(chain, authType, engine);
        } else {
            if (checkTrusted("checkServerTrusted", tm, chain, authType, SSLEngine.class, engine) || checkTrusted("checkServerTrusted", tm, chain, authType, String.class, engine.getHandshakeSession().getPeerHost())) {
                return;
            }
            tm.checkServerTrusted(chain, authType);
        }
    }

    public static OpenSSLKey wrapRsaKey(PrivateKey key) {
        return null;
    }

    public static void logEvent(String message) {
        try {
            Class<?> cls = Class.forName("android.os.Process");
            Object processInstance = cls.newInstance();
            Method myUidMethod = cls.getMethod("myUid", (Class[]) null);
            int uid = ((Integer) myUidMethod.invoke(processInstance, new Object[0])).intValue();
            Class<?> cls2 = Class.forName("android.util.EventLog");
            Object eventLogInstance = cls2.newInstance();
            Method writeEventMethod = cls2.getMethod("writeEvent", Integer.TYPE, Object[].class);
            writeEventMethod.invoke(eventLogInstance, 1397638484, new Object[]{"conscrypt", Integer.valueOf(uid), message});
        } catch (Exception e) {
        }
    }

    public static boolean isLiteralIpAddress(String hostname) {
        return InetAddress.isNumeric(hostname);
    }

    public static SSLSocketFactory wrapSocketFactoryIfNeeded(OpenSSLSocketFactoryImpl factory) {
        return factory;
    }

    public static GCMParameters fromGCMParameterSpec(AlgorithmParameterSpec params) {
        if (params instanceof GCMParameterSpec) {
            GCMParameterSpec gcmParams = (GCMParameterSpec) params;
            return new GCMParameters(gcmParams.getTLen(), gcmParams.getIV());
        }
        return null;
    }

    public static AlgorithmParameterSpec toGCMParameterSpec(int tagLenInBits, byte[] iv) {
        return new GCMParameterSpec(tagLenInBits, iv);
    }

    public static CloseGuard closeGuardGet() {
        return CloseGuard.get();
    }

    public static void closeGuardOpen(Object guardObj, String message) {
        CloseGuard guard = (CloseGuard) guardObj;
        guard.open(message);
    }

    public static void closeGuardClose(Object guardObj) {
        CloseGuard guard = (CloseGuard) guardObj;
        guard.close();
    }

    public static void closeGuardWarnIfOpen(Object guardObj) {
        CloseGuard guard = (CloseGuard) guardObj;
        guard.warnIfOpen();
    }

    public static void blockGuardOnNetwork() {
        BlockGuard.getThreadPolicy().onNetwork();
    }

    public static String oidToAlgorithmName(String oid) {
        try {
            return AlgorithmId.get(oid).getName();
        } catch (NoSuchAlgorithmException e) {
            return oid;
        }
    }

    public static SSLSession wrapSSLSession(OpenSSLSessionImpl sslSession) {
        return new OpenSSLExtendedSessionImpl(sslSession);
    }

    public static String getHostStringFromInetSocketAddress(InetSocketAddress addr) {
        return addr.getHostString();
    }
}
