package com.android.org.conscrypt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import javax.net.ssl.SSLSocketFactory;

final class OpenSSLSocketFactoryImpl extends SSLSocketFactory {
    private static boolean useEngineSocketByDefault = SSLUtils.USE_ENGINE_SOCKET_BY_DEFAULT;
    private final IOException instantiationException;
    private final SSLParametersImpl sslParameters;
    private boolean useEngineSocket;

    OpenSSLSocketFactoryImpl() throws KeyManagementException {
        IOException iOException;
        this.useEngineSocket = useEngineSocketByDefault;
        SSLParametersImpl sSLParametersImpl = null;
        try {
            iOException = null;
            sSLParametersImpl = SSLParametersImpl.getDefault();
        } catch (KeyManagementException e) {
            iOException = new IOException("Delayed instantiation exception:", e);
        }
        this.sslParameters = sSLParametersImpl;
        this.instantiationException = iOException;
    }

    OpenSSLSocketFactoryImpl(SSLParametersImpl sSLParametersImpl) {
        this.useEngineSocket = useEngineSocketByDefault;
        this.sslParameters = sSLParametersImpl;
        this.instantiationException = null;
    }

    static void setUseEngineSocketByDefault(boolean z) {
        useEngineSocketByDefault = z;
    }

    void setUseEngineSocket(boolean z) {
        this.useEngineSocket = z;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return this.sslParameters.getEnabledCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return NativeCrypto.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        if (this.instantiationException != null) {
            throw this.instantiationException;
        }
        if (this.useEngineSocket) {
            return Platform.createEngineSocket((SSLParametersImpl) this.sslParameters.clone());
        }
        return Platform.createFileDescriptorSocket((SSLParametersImpl) this.sslParameters.clone());
    }

    @Override
    public Socket createSocket(String str, int i) throws IOException {
        if (this.useEngineSocket) {
            return Platform.createEngineSocket(str, i, (SSLParametersImpl) this.sslParameters.clone());
        }
        return Platform.createFileDescriptorSocket(str, i, (SSLParametersImpl) this.sslParameters.clone());
    }

    @Override
    public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) throws IOException {
        if (this.useEngineSocket) {
            return Platform.createEngineSocket(str, i, inetAddress, i2, (SSLParametersImpl) this.sslParameters.clone());
        }
        return Platform.createFileDescriptorSocket(str, i, inetAddress, i2, (SSLParametersImpl) this.sslParameters.clone());
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        if (this.useEngineSocket) {
            return Platform.createEngineSocket(inetAddress, i, (SSLParametersImpl) this.sslParameters.clone());
        }
        return Platform.createFileDescriptorSocket(inetAddress, i, (SSLParametersImpl) this.sslParameters.clone());
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        if (this.useEngineSocket) {
            return Platform.createEngineSocket(inetAddress, i, inetAddress2, i2, (SSLParametersImpl) this.sslParameters.clone());
        }
        return Platform.createFileDescriptorSocket(inetAddress, i, inetAddress2, i2, (SSLParametersImpl) this.sslParameters.clone());
    }

    @Override
    public Socket createSocket(Socket socket, String str, int i, boolean z) throws IOException {
        Preconditions.checkNotNull(socket, "socket");
        if (!socket.isConnected()) {
            throw new SocketException("Socket is not connected.");
        }
        if (hasFileDescriptor(socket) && !this.useEngineSocket) {
            return Platform.createFileDescriptorSocket(socket, str, i, z, (SSLParametersImpl) this.sslParameters.clone());
        }
        return Platform.createEngineSocket(socket, str, i, z, (SSLParametersImpl) this.sslParameters.clone());
    }

    private boolean hasFileDescriptor(Socket socket) {
        try {
            Platform.getFileDescriptor(socket);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
