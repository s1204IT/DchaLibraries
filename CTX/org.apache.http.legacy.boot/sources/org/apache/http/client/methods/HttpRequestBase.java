package org.apache.http.client.methods;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.CloneUtils;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionReleaseTrigger;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

@Deprecated
public abstract class HttpRequestBase extends AbstractHttpMessage implements HttpUriRequest, AbortableHttpRequest, Cloneable {
    private Lock abortLock = new ReentrantLock();
    private boolean aborted;
    private ClientConnectionRequest connRequest;
    private ConnectionReleaseTrigger releaseTrigger;
    private URI uri;

    public abstract String getMethod();

    @Override
    public ProtocolVersion getProtocolVersion() {
        return HttpProtocolParams.getVersion(getParams());
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public RequestLine getRequestLine() {
        String aSCIIString;
        String method = getMethod();
        ProtocolVersion protocolVersion = getProtocolVersion();
        URI uri = getURI();
        if (uri != null) {
            aSCIIString = uri.toASCIIString();
        } else {
            aSCIIString = null;
        }
        if (aSCIIString == null || aSCIIString.length() == 0) {
            aSCIIString = "/";
        }
        return new BasicRequestLine(method, aSCIIString, protocolVersion);
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    @Override
    public void setConnectionRequest(ClientConnectionRequest clientConnectionRequest) throws IOException {
        this.abortLock.lock();
        try {
            if (this.aborted) {
                throw new IOException("Request already aborted");
            }
            this.releaseTrigger = null;
            this.connRequest = clientConnectionRequest;
        } finally {
            this.abortLock.unlock();
        }
    }

    @Override
    public void setReleaseTrigger(ConnectionReleaseTrigger connectionReleaseTrigger) throws IOException {
        this.abortLock.lock();
        try {
            if (this.aborted) {
                throw new IOException("Request already aborted");
            }
            this.connRequest = null;
            this.releaseTrigger = connectionReleaseTrigger;
        } finally {
            this.abortLock.unlock();
        }
    }

    @Override
    public void abort() {
        this.abortLock.lock();
        try {
            if (this.aborted) {
                return;
            }
            this.aborted = true;
            ClientConnectionRequest clientConnectionRequest = this.connRequest;
            ConnectionReleaseTrigger connectionReleaseTrigger = this.releaseTrigger;
            if (clientConnectionRequest != null) {
                clientConnectionRequest.abortRequest();
            }
            if (connectionReleaseTrigger != null) {
                try {
                    connectionReleaseTrigger.abortConnection();
                } catch (IOException e) {
                }
            }
        } finally {
            this.abortLock.unlock();
        }
    }

    @Override
    public boolean isAborted() {
        return this.aborted;
    }

    public Object clone() throws CloneNotSupportedException {
        HttpRequestBase httpRequestBase = (HttpRequestBase) super.clone();
        httpRequestBase.abortLock = new ReentrantLock();
        httpRequestBase.aborted = false;
        httpRequestBase.releaseTrigger = null;
        httpRequestBase.connRequest = null;
        httpRequestBase.headergroup = (HeaderGroup) CloneUtils.clone(this.headergroup);
        httpRequestBase.params = (HttpParams) CloneUtils.clone(this.params);
        return httpRequestBase;
    }
}
