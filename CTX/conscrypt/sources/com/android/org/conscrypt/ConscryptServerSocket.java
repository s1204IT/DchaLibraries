package com.android.org.conscrypt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLServerSocket;

final class ConscryptServerSocket extends SSLServerSocket {
    private boolean channelIdEnabled;
    private final SSLParametersImpl sslParameters;
    private boolean useEngineSocket;

    ConscryptServerSocket(SSLParametersImpl sSLParametersImpl) throws IOException {
        this.sslParameters = sSLParametersImpl;
    }

    ConscryptServerSocket(int i, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(i);
        this.sslParameters = sSLParametersImpl;
    }

    ConscryptServerSocket(int i, int i2, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(i, i2);
        this.sslParameters = sSLParametersImpl;
    }

    ConscryptServerSocket(int i, int i2, InetAddress inetAddress, SSLParametersImpl sSLParametersImpl) throws IOException {
        super(i, i2, inetAddress);
        this.sslParameters = sSLParametersImpl;
    }

    ConscryptServerSocket setUseEngineSocket(boolean z) {
        this.useEngineSocket = z;
        return this;
    }

    @Override
    public boolean getEnableSessionCreation() {
        return this.sslParameters.getEnableSessionCreation();
    }

    @Override
    public void setEnableSessionCreation(boolean z) {
        this.sslParameters.setEnableSessionCreation(z);
    }

    @Override
    public String[] getSupportedProtocols() {
        return NativeCrypto.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return this.sslParameters.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] strArr) {
        this.sslParameters.setEnabledProtocols(strArr);
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return NativeCrypto.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return this.sslParameters.getEnabledCipherSuites();
    }

    void setChannelIdEnabled(boolean z) {
        this.channelIdEnabled = z;
    }

    boolean isChannelIdEnabled() {
        return this.channelIdEnabled;
    }

    @Override
    public void setEnabledCipherSuites(String[] strArr) {
        this.sslParameters.setEnabledCipherSuites(strArr);
    }

    @Override
    public boolean getWantClientAuth() {
        return this.sslParameters.getWantClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean z) {
        this.sslParameters.setWantClientAuth(z);
    }

    @Override
    public boolean getNeedClientAuth() {
        return this.sslParameters.getNeedClientAuth();
    }

    @Override
    public void setNeedClientAuth(boolean z) {
        this.sslParameters.setNeedClientAuth(z);
    }

    @Override
    public void setUseClientMode(boolean z) {
        this.sslParameters.setUseClientMode(z);
    }

    @Override
    public boolean getUseClientMode() {
        return this.sslParameters.getUseClientMode();
    }

    @Override
    public Socket accept() throws IOException {
        AbstractConscryptSocket abstractConscryptSocketCreateFileDescriptorSocket;
        if (this.useEngineSocket) {
            abstractConscryptSocketCreateFileDescriptorSocket = Platform.createEngineSocket(this.sslParameters);
        } else {
            abstractConscryptSocketCreateFileDescriptorSocket = Platform.createFileDescriptorSocket(this.sslParameters);
        }
        abstractConscryptSocketCreateFileDescriptorSocket.setChannelIdEnabled(this.channelIdEnabled);
        implAccept(abstractConscryptSocketCreateFileDescriptorSocket);
        return abstractConscryptSocketCreateFileDescriptorSocket;
    }
}
