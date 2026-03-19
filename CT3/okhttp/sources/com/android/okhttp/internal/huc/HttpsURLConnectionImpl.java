package com.android.okhttp.internal.huc;

import com.android.okhttp.Handshake;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.internal.URLFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public final class HttpsURLConnectionImpl extends DelegatingHttpsURLConnection {
    private final HttpURLConnectionImpl delegate;

    @Override
    public void addRequestProperty(String field, String newValue) {
        super.addRequestProperty(field, newValue);
    }

    @Override
    public void connect() throws IOException {
        super.connect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    public boolean getAllowUserInteraction() {
        return super.getAllowUserInteraction();
    }

    @Override
    public String getCipherSuite() {
        return super.getCipherSuite();
    }

    @Override
    public int getConnectTimeout() {
        return super.getConnectTimeout();
    }

    @Override
    public Object getContent() {
        return super.getContent();
    }

    @Override
    public Object getContent(Class[] types) {
        return super.getContent(types);
    }

    @Override
    public String getContentEncoding() {
        return super.getContentEncoding();
    }

    @Override
    public int getContentLength() {
        return super.getContentLength();
    }

    @Override
    public String getContentType() {
        return super.getContentType();
    }

    @Override
    public long getDate() {
        return super.getDate();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return super.getDefaultUseCaches();
    }

    @Override
    public boolean getDoInput() {
        return super.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
        return super.getDoOutput();
    }

    @Override
    public InputStream getErrorStream() {
        return super.getErrorStream();
    }

    @Override
    public long getExpiration() {
        return super.getExpiration();
    }

    @Override
    public String getHeaderField(int pos) {
        return super.getHeaderField(pos);
    }

    @Override
    public String getHeaderField(String key) {
        return super.getHeaderField(key);
    }

    @Override
    public long getHeaderFieldDate(String field, long defaultValue) {
        return super.getHeaderFieldDate(field, defaultValue);
    }

    @Override
    public int getHeaderFieldInt(String field, int defaultValue) {
        return super.getHeaderFieldInt(field, defaultValue);
    }

    @Override
    public String getHeaderFieldKey(int position) {
        return super.getHeaderFieldKey(position);
    }

    @Override
    public Map getHeaderFields() {
        return super.getHeaderFields();
    }

    @Override
    public long getIfModifiedSince() {
        return super.getIfModifiedSince();
    }

    @Override
    public InputStream getInputStream() {
        return super.getInputStream();
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return super.getInstanceFollowRedirects();
    }

    @Override
    public long getLastModified() {
        return super.getLastModified();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return super.getLocalCertificates();
    }

    @Override
    public Principal getLocalPrincipal() {
        return super.getLocalPrincipal();
    }

    @Override
    public OutputStream getOutputStream() {
        return super.getOutputStream();
    }

    @Override
    public Principal getPeerPrincipal() {
        return super.getPeerPrincipal();
    }

    @Override
    public Permission getPermission() {
        return super.getPermission();
    }

    @Override
    public int getReadTimeout() {
        return super.getReadTimeout();
    }

    @Override
    public String getRequestMethod() {
        return super.getRequestMethod();
    }

    @Override
    public Map getRequestProperties() {
        return super.getRequestProperties();
    }

    @Override
    public String getRequestProperty(String field) {
        return super.getRequestProperty(field);
    }

    @Override
    public int getResponseCode() {
        return super.getResponseCode();
    }

    @Override
    public String getResponseMessage() {
        return super.getResponseMessage();
    }

    @Override
    public Certificate[] getServerCertificates() {
        return super.getServerCertificates();
    }

    @Override
    public URL getURL() {
        return super.getURL();
    }

    @Override
    public boolean getUseCaches() {
        return super.getUseCaches();
    }

    @Override
    public void setAllowUserInteraction(boolean newValue) {
        super.setAllowUserInteraction(newValue);
    }

    @Override
    public void setChunkedStreamingMode(int chunkLength) {
        super.setChunkedStreamingMode(chunkLength);
    }

    @Override
    public void setConnectTimeout(int timeoutMillis) {
        super.setConnectTimeout(timeoutMillis);
    }

    @Override
    public void setDefaultUseCaches(boolean newValue) {
        super.setDefaultUseCaches(newValue);
    }

    @Override
    public void setDoInput(boolean newValue) {
        super.setDoInput(newValue);
    }

    @Override
    public void setDoOutput(boolean newValue) {
        super.setDoOutput(newValue);
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
        super.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setIfModifiedSince(long newValue) {
        super.setIfModifiedSince(newValue);
    }

    @Override
    public void setInstanceFollowRedirects(boolean followRedirects) {
        super.setInstanceFollowRedirects(followRedirects);
    }

    @Override
    public void setReadTimeout(int timeoutMillis) {
        super.setReadTimeout(timeoutMillis);
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        super.setRequestMethod(method);
    }

    @Override
    public void setRequestProperty(String field, String newValue) {
        super.setRequestProperty(field, newValue);
    }

    @Override
    public void setUseCaches(boolean newValue) {
        super.setUseCaches(newValue);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean usingProxy() {
        return super.usingProxy();
    }

    public HttpsURLConnectionImpl(URL url, OkHttpClient client) {
        this(new HttpURLConnectionImpl(url, client));
    }

    public HttpsURLConnectionImpl(URL url, OkHttpClient client, URLFilter filter) {
        this(new HttpURLConnectionImpl(url, client, filter));
    }

    public HttpsURLConnectionImpl(HttpURLConnectionImpl delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    protected Handshake handshake() {
        if (this.delegate.httpEngine == null) {
            throw new IllegalStateException("Connection has not yet been established");
        }
        if (this.delegate.httpEngine.hasResponse()) {
            return this.delegate.httpEngine.getResponse().handshake();
        }
        return this.delegate.handshake;
    }

    @Override
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.delegate.client.setHostnameVerifier(hostnameVerifier);
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return this.delegate.client.getHostnameVerifier();
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.delegate.client.setSslSocketFactory(sslSocketFactory);
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return this.delegate.client.getSslSocketFactory();
    }

    @Override
    public void setFixedLengthStreamingMode(long contentLength) {
        this.delegate.setFixedLengthStreamingMode(contentLength);
    }
}
