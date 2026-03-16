package org.ksoap2.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ksoap2.HeaderProperty;

public class ServiceConnectionSE implements ServiceConnection {
    private HttpURLConnection connection;

    public ServiceConnectionSE(String url) throws IOException {
        this(null, url, ServiceConnection.DEFAULT_TIMEOUT);
    }

    public ServiceConnectionSE(Proxy proxy, String url) throws IOException {
        this(proxy, url, ServiceConnection.DEFAULT_TIMEOUT);
    }

    public ServiceConnectionSE(String url, int timeout) throws IOException {
        this(null, url, timeout);
    }

    public ServiceConnectionSE(Proxy proxy, String url, int timeout) throws IOException {
        this.connection = proxy == null ? (HttpURLConnection) new URL(url).openConnection() : (HttpURLConnection) new URL(url).openConnection(proxy);
        this.connection.setUseCaches(false);
        this.connection.setDoOutput(true);
        this.connection.setDoInput(true);
        this.connection.setConnectTimeout(timeout);
        this.connection.setReadTimeout(timeout);
    }

    @Override
    public void connect() throws IOException {
        this.connection.connect();
    }

    @Override
    public void disconnect() {
        this.connection.disconnect();
    }

    @Override
    public List getResponseProperties() {
        Map<String, List<String>> headerFields = this.connection.getHeaderFields();
        Set<String> setKeySet = headerFields.keySet();
        List retList = new LinkedList();
        for (String key : setKeySet) {
            List<String> list = headerFields.get(key);
            for (int j = 0; j < list.size(); j++) {
                retList.add(new HeaderProperty(key, list.get(j)));
            }
        }
        return retList;
    }

    @Override
    public void setRequestProperty(String string, String soapAction) {
        this.connection.setRequestProperty(string, soapAction);
    }

    @Override
    public void setRequestMethod(String requestMethod) throws IOException {
        this.connection.setRequestMethod(requestMethod);
    }

    @Override
    public void setFixedLengthStreamingMode(int contentLength) {
        this.connection.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return this.connection.getOutputStream();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return this.connection.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return this.connection.getErrorStream();
    }

    @Override
    public String getHost() {
        return this.connection.getURL().getHost();
    }

    @Override
    public int getPort() {
        return this.connection.getURL().getPort();
    }

    @Override
    public String getPath() {
        return this.connection.getURL().getPath();
    }
}
