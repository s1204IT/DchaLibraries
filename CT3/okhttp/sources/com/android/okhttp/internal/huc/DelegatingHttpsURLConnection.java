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

    public DelegatingHttpsURLConnection(HttpURLConnection delegate) {
        super(delegate.getURL());
        this.delegate = delegate;
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
        List<Certificate> result = handshake.localCertificates();
        if (result.isEmpty()) {
            return null;
        }
        return (Certificate[]) result.toArray(new Certificate[result.size()]);
    }

    @Override
    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        Handshake handshake = handshake();
        if (handshake == null) {
            return null;
        }
        List<Certificate> result = handshake.peerCertificates();
        if (result.isEmpty()) {
            return null;
        }
        return (Certificate[]) result.toArray(new Certificate[result.size()]);
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
    public void setRequestMethod(String method) throws ProtocolException {
        this.delegate.setRequestMethod(method);
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
    public void setInstanceFollowRedirects(boolean followRedirects) {
        this.delegate.setInstanceFollowRedirects(followRedirects);
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
    public Object getContent(Class[] types) throws IOException {
        return this.delegate.getContent(types);
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
    public String getHeaderField(int pos) {
        return this.delegate.getHeaderField(pos);
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
    public void addRequestProperty(String field, String newValue) {
        this.delegate.addRequestProperty(field, newValue);
    }

    @Override
    public String getHeaderField(String key) {
        return this.delegate.getHeaderField(key);
    }

    @Override
    public long getHeaderFieldDate(String field, long defaultValue) {
        return this.delegate.getHeaderFieldDate(field, defaultValue);
    }

    @Override
    public int getHeaderFieldInt(String field, int defaultValue) {
        return this.delegate.getHeaderFieldInt(field, defaultValue);
    }

    @Override
    public String getHeaderFieldKey(int position) {
        return this.delegate.getHeaderFieldKey(position);
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
    public String getRequestProperty(String field) {
        return this.delegate.getRequestProperty(field);
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
    public void setAllowUserInteraction(boolean newValue) {
        this.delegate.setAllowUserInteraction(newValue);
    }

    @Override
    public void setDefaultUseCaches(boolean newValue) {
        this.delegate.setDefaultUseCaches(newValue);
    }

    @Override
    public void setDoInput(boolean newValue) {
        this.delegate.setDoInput(newValue);
    }

    @Override
    public void setDoOutput(boolean newValue) {
        this.delegate.setDoOutput(newValue);
    }

    @Override
    public void setIfModifiedSince(long newValue) {
        this.delegate.setIfModifiedSince(newValue);
    }

    @Override
    public void setRequestProperty(String field, String newValue) {
        this.delegate.setRequestProperty(field, newValue);
    }

    @Override
    public void setUseCaches(boolean newValue) {
        this.delegate.setUseCaches(newValue);
    }

    @Override
    public void setConnectTimeout(int timeoutMillis) {
        this.delegate.setConnectTimeout(timeoutMillis);
    }

    @Override
    public int getConnectTimeout() {
        return this.delegate.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(int timeoutMillis) {
        this.delegate.setReadTimeout(timeoutMillis);
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
    public void setFixedLengthStreamingMode(int contentLength) {
        this.delegate.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setChunkedStreamingMode(int chunkLength) {
        this.delegate.setChunkedStreamingMode(chunkLength);
    }
}
