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
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public final class HttpsURLConnectionImpl extends DelegatingHttpsURLConnection {
    private final HttpURLConnectionImpl delegate;

    @Override
    public void addRequestProperty(String str, String str2) {
        super.addRequestProperty(str, str2);
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
    public Object getContent() throws IOException {
        return super.getContent();
    }

    @Override
    public Object getContent(Class[] clsArr) throws IOException {
        return super.getContent(clsArr);
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
    public String getHeaderField(int i) {
        return super.getHeaderField(i);
    }

    @Override
    public String getHeaderField(String str) {
        return super.getHeaderField(str);
    }

    @Override
    public long getHeaderFieldDate(String str, long j) {
        return super.getHeaderFieldDate(str, j);
    }

    @Override
    public int getHeaderFieldInt(String str, int i) {
        return super.getHeaderFieldInt(str, i);
    }

    @Override
    public String getHeaderFieldKey(int i) {
        return super.getHeaderFieldKey(i);
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
    public InputStream getInputStream() throws IOException {
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
    public OutputStream getOutputStream() throws IOException {
        return super.getOutputStream();
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return super.getPeerPrincipal();
    }

    @Override
    public Permission getPermission() throws IOException {
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
    public String getRequestProperty(String str) {
        return super.getRequestProperty(str);
    }

    @Override
    public int getResponseCode() throws IOException {
        return super.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return super.getResponseMessage();
    }

    @Override
    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
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
    public void setAllowUserInteraction(boolean z) {
        super.setAllowUserInteraction(z);
    }

    @Override
    public void setChunkedStreamingMode(int i) {
        super.setChunkedStreamingMode(i);
    }

    @Override
    public void setConnectTimeout(int i) {
        super.setConnectTimeout(i);
    }

    @Override
    public void setDefaultUseCaches(boolean z) {
        super.setDefaultUseCaches(z);
    }

    @Override
    public void setDoInput(boolean z) {
        super.setDoInput(z);
    }

    @Override
    public void setDoOutput(boolean z) {
        super.setDoOutput(z);
    }

    @Override
    public void setFixedLengthStreamingMode(int i) {
        super.setFixedLengthStreamingMode(i);
    }

    @Override
    public void setIfModifiedSince(long j) {
        super.setIfModifiedSince(j);
    }

    @Override
    public void setInstanceFollowRedirects(boolean z) {
        super.setInstanceFollowRedirects(z);
    }

    @Override
    public void setReadTimeout(int i) {
        super.setReadTimeout(i);
    }

    @Override
    public void setRequestMethod(String str) throws ProtocolException {
        super.setRequestMethod(str);
    }

    @Override
    public void setRequestProperty(String str, String str2) {
        super.setRequestProperty(str, str2);
    }

    @Override
    public void setUseCaches(boolean z) {
        super.setUseCaches(z);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean usingProxy() {
        return super.usingProxy();
    }

    public HttpsURLConnectionImpl(URL url, OkHttpClient okHttpClient) {
        this(new HttpURLConnectionImpl(url, okHttpClient));
    }

    public HttpsURLConnectionImpl(URL url, OkHttpClient okHttpClient, URLFilter uRLFilter) {
        this(new HttpURLConnectionImpl(url, okHttpClient, uRLFilter));
    }

    public HttpsURLConnectionImpl(HttpURLConnectionImpl httpURLConnectionImpl) {
        super(httpURLConnectionImpl);
        this.delegate = httpURLConnectionImpl;
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
    public void setSSLSocketFactory(SSLSocketFactory sSLSocketFactory) {
        this.delegate.client.setSslSocketFactory(sSLSocketFactory);
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return this.delegate.client.getSslSocketFactory();
    }

    @Override
    public long getContentLengthLong() {
        return this.delegate.getContentLengthLong();
    }

    @Override
    public void setFixedLengthStreamingMode(long j) {
        this.delegate.setFixedLengthStreamingMode(j);
    }

    @Override
    public long getHeaderFieldLong(String str, long j) {
        return this.delegate.getHeaderFieldLong(str, j);
    }
}
