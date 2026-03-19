package org.apache.http.message;

import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.params.HttpProtocolParams;

@Deprecated
public class BasicHttpRequest extends AbstractHttpMessage implements HttpRequest {
    private final String method;
    private final RequestLine requestline;
    private final String uri;

    public BasicHttpRequest(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("Method name may not be null");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("Request URI may not be null");
        }
        this.method = str;
        this.uri = str2;
        this.requestline = null;
    }

    public BasicHttpRequest(String str, String str2, ProtocolVersion protocolVersion) {
        this(new BasicRequestLine(str, str2, protocolVersion));
    }

    public BasicHttpRequest(RequestLine requestLine) {
        if (requestLine == null) {
            throw new IllegalArgumentException("Request line may not be null");
        }
        this.requestline = requestLine;
        this.method = requestLine.getMethod();
        this.uri = requestLine.getUri();
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        if (this.requestline != null) {
            return this.requestline.getProtocolVersion();
        }
        return HttpProtocolParams.getVersion(getParams());
    }

    @Override
    public RequestLine getRequestLine() {
        if (this.requestline != null) {
            return this.requestline;
        }
        return new BasicRequestLine(this.method, this.uri, HttpProtocolParams.getVersion(getParams()));
    }
}
