package com.android.org.conscrypt;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.system.StructTimeval;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidParameterSpecException;
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
import libcore.io.Libcore;
import libcore.net.NetworkSecurityPolicy;
import sun.security.x509.AlgorithmId;

final class Platform {

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
    }

    static String getDefaultProviderName() {
        return "AndroidOpenSSL";
    }

    static FileDescriptor getFileDescriptor(Socket socket) {
        return socket.getFileDescriptor$();
    }

    static FileDescriptor getFileDescriptorFromSSLSocket(AbstractConscryptSocket abstractConscryptSocket) {
        try {
            Field declaredField = Socket.class.getDeclaredField("impl");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(abstractConscryptSocket);
            Field declaredField2 = SocketImpl.class.getDeclaredField("fd");
            declaredField2.setAccessible(true);
            return (FileDescriptor) declaredField2.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("Can't get FileDescriptor from socket", e);
        }
    }

    static String getCurveName(ECParameterSpec eCParameterSpec) {
        return eCParameterSpec.getCurveName();
    }

    static void setCurveName(ECParameterSpec eCParameterSpec, String str) {
        eCParameterSpec.setCurveName(str);
    }

    static void setSocketWriteTimeout(Socket socket, long j) throws SocketException {
        try {
            Libcore.os.setsockoptTimeval(socket.getFileDescriptor$(), OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(j));
        } catch (ErrnoException e) {
            throw e.rethrowAsSocketException();
        }
    }

    static void setSSLParameters(SSLParameters sSLParameters, SSLParametersImpl sSLParametersImpl, AbstractConscryptSocket abstractConscryptSocket) {
        sSLParametersImpl.setEndpointIdentificationAlgorithm(sSLParameters.getEndpointIdentificationAlgorithm());
        sSLParametersImpl.setUseCipherSuitesOrder(sSLParameters.getUseCipherSuitesOrder());
        List<SNIServerName> serverNames = sSLParameters.getServerNames();
        if (serverNames != null) {
            for (SNIServerName sNIServerName : serverNames) {
                if (sNIServerName.getType() == 0) {
                    abstractConscryptSocket.setHostname(((SNIHostName) sNIServerName).getAsciiName());
                    return;
                }
            }
        }
    }

    static void getSSLParameters(SSLParameters sSLParameters, SSLParametersImpl sSLParametersImpl, AbstractConscryptSocket abstractConscryptSocket) {
        sSLParameters.setEndpointIdentificationAlgorithm(sSLParametersImpl.getEndpointIdentificationAlgorithm());
        sSLParameters.setUseCipherSuitesOrder(sSLParametersImpl.getUseCipherSuitesOrder());
        if (sSLParametersImpl.getUseSni() && AddressUtils.isValidSniHostname(abstractConscryptSocket.getHostname())) {
            sSLParameters.setServerNames(Collections.singletonList(new SNIHostName(abstractConscryptSocket.getHostname())));
        }
    }

    static void setSSLParameters(SSLParameters sSLParameters, SSLParametersImpl sSLParametersImpl, ConscryptEngine conscryptEngine) {
        sSLParametersImpl.setEndpointIdentificationAlgorithm(sSLParameters.getEndpointIdentificationAlgorithm());
        sSLParametersImpl.setUseCipherSuitesOrder(sSLParameters.getUseCipherSuitesOrder());
        List<SNIServerName> serverNames = sSLParameters.getServerNames();
        if (serverNames != null) {
            for (SNIServerName sNIServerName : serverNames) {
                if (sNIServerName.getType() == 0) {
                    conscryptEngine.setHostname(((SNIHostName) sNIServerName).getAsciiName());
                    return;
                }
            }
        }
    }

    static void getSSLParameters(SSLParameters sSLParameters, SSLParametersImpl sSLParametersImpl, ConscryptEngine conscryptEngine) {
        sSLParameters.setEndpointIdentificationAlgorithm(sSLParametersImpl.getEndpointIdentificationAlgorithm());
        sSLParameters.setUseCipherSuitesOrder(sSLParametersImpl.getUseCipherSuitesOrder());
        if (sSLParametersImpl.getUseSni() && AddressUtils.isValidSniHostname(conscryptEngine.getHostname())) {
            sSLParameters.setServerNames(Collections.singletonList(new SNIHostName(conscryptEngine.getHostname())));
        }
    }

    private static boolean checkTrusted(String str, X509TrustManager x509TrustManager, X509Certificate[] x509CertificateArr, String str2, Class<?> cls, Object obj) throws CertificateException {
        try {
            x509TrustManager.getClass().getMethod(str, X509Certificate[].class, String.class, cls).invoke(x509TrustManager, x509CertificateArr, str2, obj);
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

    static void checkClientTrusted(X509TrustManager x509TrustManager, X509Certificate[] x509CertificateArr, String str, AbstractConscryptSocket abstractConscryptSocket) throws CertificateException {
        if (x509TrustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) x509TrustManager).checkClientTrusted(x509CertificateArr, str, abstractConscryptSocket);
        } else if (!checkTrusted("checkClientTrusted", x509TrustManager, x509CertificateArr, str, Socket.class, abstractConscryptSocket) && !checkTrusted("checkClientTrusted", x509TrustManager, x509CertificateArr, str, String.class, abstractConscryptSocket.getHandshakeSession().getPeerHost())) {
            x509TrustManager.checkClientTrusted(x509CertificateArr, str);
        }
    }

    static void checkServerTrusted(X509TrustManager x509TrustManager, X509Certificate[] x509CertificateArr, String str, AbstractConscryptSocket abstractConscryptSocket) throws CertificateException {
        if (x509TrustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) x509TrustManager).checkServerTrusted(x509CertificateArr, str, abstractConscryptSocket);
        } else if (!checkTrusted("checkServerTrusted", x509TrustManager, x509CertificateArr, str, Socket.class, abstractConscryptSocket) && !checkTrusted("checkServerTrusted", x509TrustManager, x509CertificateArr, str, String.class, abstractConscryptSocket.getHandshakeSession().getPeerHost())) {
            x509TrustManager.checkServerTrusted(x509CertificateArr, str);
        }
    }

    static void checkClientTrusted(X509TrustManager x509TrustManager, X509Certificate[] x509CertificateArr, String str, ConscryptEngine conscryptEngine) throws CertificateException {
        if (x509TrustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) x509TrustManager).checkClientTrusted(x509CertificateArr, str, conscryptEngine);
        } else if (!checkTrusted("checkClientTrusted", x509TrustManager, x509CertificateArr, str, SSLEngine.class, conscryptEngine) && !checkTrusted("checkClientTrusted", x509TrustManager, x509CertificateArr, str, String.class, conscryptEngine.getHandshakeSession().getPeerHost())) {
            x509TrustManager.checkClientTrusted(x509CertificateArr, str);
        }
    }

    static void checkServerTrusted(X509TrustManager x509TrustManager, X509Certificate[] x509CertificateArr, String str, ConscryptEngine conscryptEngine) throws CertificateException {
        if (x509TrustManager instanceof X509ExtendedTrustManager) {
            ((X509ExtendedTrustManager) x509TrustManager).checkServerTrusted(x509CertificateArr, str, conscryptEngine);
        } else if (!checkTrusted("checkServerTrusted", x509TrustManager, x509CertificateArr, str, SSLEngine.class, conscryptEngine) && !checkTrusted("checkServerTrusted", x509TrustManager, x509CertificateArr, str, String.class, conscryptEngine.getHandshakeSession().getPeerHost())) {
            x509TrustManager.checkServerTrusted(x509CertificateArr, str);
        }
    }

    static OpenSSLKey wrapRsaKey(PrivateKey privateKey) {
        return null;
    }

    static void logEvent(String str) {
        try {
            Class<?> cls = Class.forName("android.os.Process");
            int iIntValue = ((Integer) cls.getMethod("myUid", (Class[]) null).invoke(cls.newInstance(), new Object[0])).intValue();
            Class<?> cls2 = Class.forName("android.util.EventLog");
            cls2.getMethod("writeEvent", Integer.TYPE, Object[].class).invoke(cls2.newInstance(), 1397638484, new Object[]{"conscrypt", Integer.valueOf(iIntValue), str});
        } catch (Exception e) {
        }
    }

    static boolean isLiteralIpAddress(String str) {
        return InetAddress.isNumeric(str);
    }

    static SSLEngine wrapEngine(ConscryptEngine conscryptEngine) {
        return new Java8EngineWrapper(conscryptEngine);
    }

    static SSLEngine unwrapEngine(SSLEngine sSLEngine) {
        return Java8EngineWrapper.getDelegate(sSLEngine);
    }

    static ConscryptEngineSocket createEngineSocket(SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8EngineSocket(sSLParametersImpl);
    }

    static ConscryptEngineSocket createEngineSocket(String str, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8EngineSocket(str, i, sSLParametersImpl);
    }

    static ConscryptEngineSocket createEngineSocket(InetAddress inetAddress, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8EngineSocket(inetAddress, i, sSLParametersImpl);
    }

    static ConscryptEngineSocket createEngineSocket(String str, int i, InetAddress inetAddress, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8EngineSocket(str, i, inetAddress, i2, sSLParametersImpl);
    }

    static ConscryptEngineSocket createEngineSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8EngineSocket(inetAddress, i, inetAddress2, i2, sSLParametersImpl);
    }

    static ConscryptEngineSocket createEngineSocket(Socket socket, String str, int i, boolean z, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8EngineSocket(socket, str, i, z, sSLParametersImpl);
    }

    static ConscryptFileDescriptorSocket createFileDescriptorSocket(SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8FileDescriptorSocket(sSLParametersImpl);
    }

    static ConscryptFileDescriptorSocket createFileDescriptorSocket(String str, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8FileDescriptorSocket(str, i, sSLParametersImpl);
    }

    static ConscryptFileDescriptorSocket createFileDescriptorSocket(InetAddress inetAddress, int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8FileDescriptorSocket(inetAddress, i, sSLParametersImpl);
    }

    static ConscryptFileDescriptorSocket createFileDescriptorSocket(String str, int i, InetAddress inetAddress, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8FileDescriptorSocket(str, i, inetAddress, i2, sSLParametersImpl);
    }

    static ConscryptFileDescriptorSocket createFileDescriptorSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8FileDescriptorSocket(inetAddress, i, inetAddress2, i2, sSLParametersImpl);
    }

    static ConscryptFileDescriptorSocket createFileDescriptorSocket(Socket socket, String str, int i, boolean z, SSLParametersImpl sSLParametersImpl) throws IOException {
        return new Java8FileDescriptorSocket(socket, str, i, z, sSLParametersImpl);
    }

    static SSLSocketFactory wrapSocketFactoryIfNeeded(OpenSSLSocketFactoryImpl openSSLSocketFactoryImpl) {
        return openSSLSocketFactoryImpl;
    }

    static GCMParameters fromGCMParameterSpec(AlgorithmParameterSpec algorithmParameterSpec) {
        if (algorithmParameterSpec instanceof GCMParameterSpec) {
            GCMParameterSpec gCMParameterSpec = (GCMParameterSpec) algorithmParameterSpec;
            return new GCMParameters(gCMParameterSpec.getTLen(), gCMParameterSpec.getIV());
        }
        return null;
    }

    static AlgorithmParameterSpec fromGCMParameters(AlgorithmParameters algorithmParameters) {
        try {
            return algorithmParameters.getParameterSpec(GCMParameterSpec.class);
        } catch (InvalidParameterSpecException e) {
            return null;
        }
    }

    static AlgorithmParameterSpec toGCMParameterSpec(int i, byte[] bArr) {
        return new GCMParameterSpec(i, bArr);
    }

    static CloseGuard closeGuardGet() {
        return CloseGuard.get();
    }

    static void closeGuardOpen(Object obj, String str) {
        ((CloseGuard) obj).open(str);
    }

    static void closeGuardClose(Object obj) {
        ((CloseGuard) obj).close();
    }

    static void closeGuardWarnIfOpen(Object obj) {
        ((CloseGuard) obj).warnIfOpen();
    }

    static void blockGuardOnNetwork() {
        BlockGuard.getThreadPolicy().onNetwork();
    }

    static String oidToAlgorithmName(String str) {
        try {
            return AlgorithmId.get(str).getName();
        } catch (NoSuchAlgorithmException e) {
            return str;
        }
    }

    static SSLSession wrapSSLSession(ConscryptSession conscryptSession) {
        return new Java8ExtendedSSLSession(conscryptSession);
    }

    public static String getOriginalHostNameFromInetAddress(InetAddress inetAddress) {
        try {
            Method declaredMethod = InetAddress.class.getDeclaredMethod("holder", new Class[0]);
            declaredMethod.setAccessible(true);
            Method declaredMethod2 = Class.forName("java.net.InetAddress$InetAddressHolder").getDeclaredMethod("getOriginalHostName", new Class[0]);
            declaredMethod2.setAccessible(true);
            String str = (String) declaredMethod2.invoke(declaredMethod.invoke(inetAddress, new Object[0]), new Object[0]);
            if (str == null) {
                return inetAddress.getHostAddress();
            }
            return str;
        } catch (ClassNotFoundException e) {
            return inetAddress.getHostAddress();
        } catch (IllegalAccessException e2) {
            return inetAddress.getHostAddress();
        } catch (NoSuchMethodException e3) {
            return inetAddress.getHostAddress();
        } catch (InvocationTargetException e4) {
            throw new RuntimeException("Failed to get originalHostName", e4);
        }
    }

    static String getHostStringFromInetSocketAddress(InetSocketAddress inetSocketAddress) {
        return inetSocketAddress.getHostString();
    }

    static boolean isCTVerificationRequired(String str) {
        return NetworkSecurityPolicy.getInstance().isCertificateTransparencyVerificationRequired(str);
    }
}
