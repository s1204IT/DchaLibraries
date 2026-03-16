package com.android.org.conscrypt;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECParameterSpec;
import javax.net.ssl.X509TrustManager;
import org.apache.harmony.security.utils.AlgNameMapper;
import org.apache.harmony.security.utils.AlgNameMapperSource;

class Platform {

    private static class NoPreloadHolder {
        public static final Platform MAPPER = new Platform();

        private NoPreloadHolder() {
        }
    }

    public static void setup() {
        NoPreloadHolder.MAPPER.ping();
    }

    private void ping() {
    }

    private Platform() {
        AlgNameMapper.setSource(new OpenSSLMapper());
    }

    private static class OpenSSLMapper implements AlgNameMapperSource {
        private OpenSSLMapper() {
        }

        public String mapNameToOid(String algName) {
            return NativeCrypto.OBJ_txt2nid_oid(algName);
        }

        public String mapOidToName(String oid) {
            return NativeCrypto.OBJ_txt2nid_longName(oid);
        }
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

    public static void checkServerTrusted(X509TrustManager x509tm, X509Certificate[] chain, String authType, String host) throws CertificateException {
        if (x509tm instanceof TrustManagerImpl) {
            TrustManagerImpl tm = (TrustManagerImpl) x509tm;
            tm.checkServerTrusted(chain, authType, host);
        } else {
            x509tm.checkServerTrusted(chain, authType);
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

    public static boolean isSniEnabledByDefault() {
        return false;
    }
}
