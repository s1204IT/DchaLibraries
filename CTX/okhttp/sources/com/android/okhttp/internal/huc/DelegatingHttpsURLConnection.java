package com.android.okhttp.internal.huc;

import com.android.okhttp.Handshake;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

abstract class DelegatingHttpsURLConnection extends HttpsURLConnection {
    private final HttpURLConnection delegate;

    @Override
    public abstract HostnameVerifier getHostnameVerifier();

    @Override
    public abstract SSLSocketFactory getSSLSocketFactory();

    protected abstract Handshake handshake();

    @Override
    public abstract void setHostnameVerifier(HostnameVerifier hostnameVerifier);

    @Override
    public abstract void setSSLSocketFactory(SSLSocketFactory sSLSocketFactory);

    public DelegatingHttpsURLConnection(HttpURLConnection httpURLConnection) {
        super(httpURLConnection.getURL());
        this.delegate = httpURLConnection;
    }

    @Override
    public String getCipherSuite() {
        Handshake handshake = handshake();
        if (handshake != null) {
            return handshake.cipherSuite();
        }
        return null;
    }

    @Override
    public Certificate[] getLocalCertificates() {
        Handshake handshake = handshake();
        if (handshake == null) {
            return null;
        }
        List<Certificate> listLocalCertificates = handshake.localCertificates();
        if (listLocalCertificates.isEmpty()) {
            return null;
        }
        return (Certificate[]) listLocalCertificates.toArray(new Certificate[listLocalCertificates.size()]);
    }

    @Override
    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        Handshake handshake = handshake();
        if (handshake == null) {
            return null;
        }
        List<Certificate> listPeerCertificates = handshake.peerCertificates();
        if (listPeerCertificates.isEmpty()) {
            return null;
        }
        return (Certificate[]) listPeerCertificates.toArray(new Certificate[listPeerCertificates.size()]);
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        Handshake handshake = handshake();
        if (handshake != null) {
            return handshake.peerPrincipal();
        }
        return null;
    }

    @Override
    public Principal getLocalPrincipal() {
        Handshake handshake = handshake();
        if (handshake != null) {
            return handshake.localPrincipal();
        }
        return null;
    }

    @Override
    public void connect() throws IOException {
        this.connected = true;
        this.delegate.connect();
    }

    @Override
    public void disconnect() {
        this.delegate.disconnect();
    }

    @Override
    public InputStream getErrorStream() {
        return this.delegate.getErrorStream();
    }

    @Override
    public String getRequestMethod() {
        return this.delegate.getRequestMethod();
    }

    @Override
    public int getResponseCode() throws IOException {
        return this.delegate.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return this.delegate.getResponseMessage();
    }

    @Override
    public void setRequestMethod(String str) throws ProtocolException {
        this.delegate.setRequestMethod(str);
    }

    @Override
    public boolean usingProxy() {
        return this.delegate.usingProxy();
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return this.delegate.getInstanceFollowRedirects();
    }

    @Override
    public void setInstanceFollowRedirects(boolean z) {
        this.delegate.setInstanceFollowRedirects(z);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return this.delegate.getAllowUserInteraction();
    }

    @Override
    public Object getContent() throws IOException {
        return this.delegate.getContent();
    }

    @Override
    public Object getContent(Class[] clsArr) throws IOException {
        return this.delegate.getContent(clsArr);
    }

    @Override
    public String getContentEncoding() {
        return this.delegate.getContentEncoding();
    }

    @Override
    public int getContentLength() {
        return this.delegate.getContentLength();
    }

    @Override
    public String getContentType() {
        return this.delegate.getContentType();
    }

    @Override
    public long getDate() {
        return this.delegate.getDate();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return this.delegate.getDefaultUseCaches();
    }

    @Override
    public boolean getDoInput() {
        return this.delegate.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
        return this.delegate.getDoOutput();
    }

    @Override
    public long getExpiration() {
        return this.delegate.getExpiration();
    }

    @Override
    public String getHeaderField(int i) {
        return this.delegate.getHeaderField(i);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return this.delegate.getHeaderFields();
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return this.delegate.getRequestProperties();
    }

    @Override
    public void addRequestProperty(String str, String str2) {
        this.delegate.addRequestProperty(str, str2);
    }

    @Override
    public String getHeaderField(String str) {
        return this.delegate.getHeaderField(str);
    }

    @Override
    public long getHeaderFieldDate(String str, long j) {
        return this.delegate.getHeaderFieldDate(str, j);
    }

    @Override
    public int getHeaderFieldInt(String str, int i) {
        return this.delegate.getHeaderFieldInt(str, i);
    }

    @Override
    public String getHeaderFieldKey(int i) {
        return this.delegate.getHeaderFieldKey(i);
    }

    @Override
    public long getIfModifiedSince() {
        return this.delegate.getIfModifiedSince();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.delegate.getInputStream();
    }

    @Override
    public long getLastModified() {
        return this.delegate.getLastModified();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return this.delegate.getOutputStream();
    }

    @Override
    public Permission getPermission() throws IOException {
        return this.delegate.getPermission();
    }

    @Override
    public String getRequestProperty(String str) {
        return this.delegate.getRequestProperty(str);
    }

    @Override
    public URL getURL() {
        return this.delegate.getURL();
    }

    @Override
    public boolean getUseCaches() {
        return this.delegate.getUseCaches();
    }

    @Override
    public void setAllowUserInteraction(boolean z) {
        this.delegate.setAllowUserInteraction(z);
    }

    @Override
    public void setDefaultUseCaches(boolean z) {
        this.delegate.setDefaultUseCaches(z);
    }

    @Override
    public void setDoInput(boolean z) {
        this.delegate.setDoInput(z);
    }

    @Override
    public void setDoOutput(boolean z) {
        this.delegate.setDoOutput(z);
    }

    @Override
    public void setIfModifiedSince(long j) {
        this.delegate.setIfModifiedSince(j);
    }

    @Override
    public void setRequestProperty(String str, String str2) {
        this.delegate.setRequestProperty(str, str2);
    }

    @Override
    public void setUseCaches(boolean z) {
        this.delegate.setUseCaches(z);
    }

    @Override
    public void setConnectTimeout(int i) {
        this.delegate.setConnectTimeout(i);
    }

    @Override
    public int getConnectTimeout() {
        return this.delegate.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int i) {
        this.delegate.setReadTimeout(i);
    }

    @Override
    public int getReadTimeout() {
        return this.delegate.getReadTimeout();
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    @Override
    public void setFixedLengthStreamingMode(int i) {
        this.delegate.setFixedLengthStreamingMode(i);
    }

    @Override
    public void setChunkedStreamingMode(int i) {
        this.delegate.setChunkedStreamingMode(i);
    }
}
