package org.apache.http.impl.client;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.params.HttpProtocolParams;

@Deprecated
public class RequestWrapper extends AbstractHttpMessage implements HttpUriRequest {
    private int execCount;
    private String method;
    private final HttpRequest original;
    private URI uri;
    private ProtocolVersion version;

    public RequestWrapper(HttpRequest httpRequest) throws ProtocolException {
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        this.original = httpRequest;
        setParams(httpRequest.getParams());
        if (httpRequest instanceof HttpUriRequest) {
            HttpUriRequest httpUriRequest = (HttpUriRequest) httpRequest;
            this.uri = httpUriRequest.getURI();
            this.method = httpUriRequest.getMethod();
            this.version = null;
        } else {
            RequestLine requestLine = httpRequest.getRequestLine();
            try {
                this.uri = new URI(requestLine.getUri());
                this.method = requestLine.getMethod();
                this.version = httpRequest.getProtocolVersion();
            } catch (URISyntaxException e) {
                throw new ProtocolException("Invalid request URI: " + requestLine.getUri(), e);
            }
        }
        this.execCount = 0;
    }

    public void resetHeaders() {
        this.headergroup.clear();
        setHeaders(this.original.getAllHeaders());
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    public void setMethod(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Method name may not be null");
        }
        this.method = str;
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        if (this.version != null) {
            return this.version;
        }
        return HttpProtocolParams.getVersion(getParams());
    }

    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.version = protocolVersion;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    @Override
    public RequestLine getRequestLine() {
        String aSCIIString;
        String method = getMethod();
        ProtocolVersion protocolVersion = getProtocolVersion();
        if (this.uri != null) {
            aSCIIString = this.uri.toASCIIString();
        } else {
            aSCIIString = null;
        }
        if (aSCIIString == null || aSCIIString.length() == 0) {
            aSCIIString = "/";
        }
        return new BasicRequestLine(method, aSCIIString, protocolVersion);
    }

    @Override
    public void abort() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAborted() {
        return false;
    }

    public HttpRequest getOriginal() {
        return this.original;
    }

    public boolean isRepeatable() {
        return true;
    }

    public int getExecCount() {
        return this.execCount;
    }

    public void incrementExecCount() {
        this.execCount++;
    }
}
