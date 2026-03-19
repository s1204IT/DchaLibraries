package org.apache.http.protocol;

import java.io.IOException;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.UnsupportedHttpVersionException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EncodingUtils;

@Deprecated
public class HttpService {
    private HttpParams params = null;
    private HttpProcessor processor = null;
    private HttpRequestHandlerResolver handlerResolver = null;
    private ConnectionReuseStrategy connStrategy = null;
    private HttpResponseFactory responseFactory = null;
    private HttpExpectationVerifier expectationVerifier = null;

    public HttpService(HttpProcessor httpProcessor, ConnectionReuseStrategy connectionReuseStrategy, HttpResponseFactory httpResponseFactory) {
        setHttpProcessor(httpProcessor);
        setConnReuseStrategy(connectionReuseStrategy);
        setResponseFactory(httpResponseFactory);
    }

    public void setHttpProcessor(HttpProcessor httpProcessor) {
        if (httpProcessor == null) {
            throw new IllegalArgumentException("HTTP processor may not be null.");
        }
        this.processor = httpProcessor;
    }

    public void setConnReuseStrategy(ConnectionReuseStrategy connectionReuseStrategy) {
        if (connectionReuseStrategy == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null");
        }
        this.connStrategy = connectionReuseStrategy;
    }

    public void setResponseFactory(HttpResponseFactory httpResponseFactory) {
        if (httpResponseFactory == null) {
            throw new IllegalArgumentException("Response factory may not be null");
        }
        this.responseFactory = httpResponseFactory;
    }

    public void setHandlerResolver(HttpRequestHandlerResolver httpRequestHandlerResolver) {
        this.handlerResolver = httpRequestHandlerResolver;
    }

    public void setExpectationVerifier(HttpExpectationVerifier httpExpectationVerifier) {
        this.expectationVerifier = httpExpectationVerifier;
    }

    public HttpParams getParams() {
        return this.params;
    }

    public void setParams(HttpParams httpParams) {
        this.params = httpParams;
    }

    public void handleRequest(HttpServerConnection httpServerConnection, HttpContext httpContext) throws HttpException, IOException {
        HttpResponse httpResponseNewHttpResponse;
        HttpEntity entity;
        httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, httpServerConnection);
        try {
            HttpRequest httpRequestReceiveRequestHeader = httpServerConnection.receiveRequestHeader();
            httpRequestReceiveRequestHeader.setParams(new DefaultedHttpParams(httpRequestReceiveRequestHeader.getParams(), this.params));
            ProtocolVersion protocolVersion = httpRequestReceiveRequestHeader.getRequestLine().getProtocolVersion();
            if (!protocolVersion.lessEquals(HttpVersion.HTTP_1_1)) {
                protocolVersion = HttpVersion.HTTP_1_1;
            }
            httpResponseNewHttpResponse = null;
            if (httpRequestReceiveRequestHeader instanceof HttpEntityEnclosingRequest) {
                if (((HttpEntityEnclosingRequest) httpRequestReceiveRequestHeader).expectContinue()) {
                    HttpResponse httpResponseNewHttpResponse2 = this.responseFactory.newHttpResponse(protocolVersion, 100, httpContext);
                    httpResponseNewHttpResponse2.setParams(new DefaultedHttpParams(httpResponseNewHttpResponse2.getParams(), this.params));
                    if (this.expectationVerifier != null) {
                        try {
                            this.expectationVerifier.verify(httpRequestReceiveRequestHeader, httpResponseNewHttpResponse2, httpContext);
                        } catch (HttpException e) {
                            HttpResponse httpResponseNewHttpResponse3 = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_0, HttpStatus.SC_INTERNAL_SERVER_ERROR, httpContext);
                            httpResponseNewHttpResponse3.setParams(new DefaultedHttpParams(httpResponseNewHttpResponse3.getParams(), this.params));
                            handleException(e, httpResponseNewHttpResponse3);
                            httpResponseNewHttpResponse2 = httpResponseNewHttpResponse3;
                        }
                    }
                    if (httpResponseNewHttpResponse2.getStatusLine().getStatusCode() < 200) {
                        httpServerConnection.sendResponseHeader(httpResponseNewHttpResponse2);
                        httpServerConnection.flush();
                        httpServerConnection.receiveRequestEntity((HttpEntityEnclosingRequest) httpRequestReceiveRequestHeader);
                    } else {
                        httpResponseNewHttpResponse = httpResponseNewHttpResponse2;
                    }
                } else {
                    httpServerConnection.receiveRequestEntity((HttpEntityEnclosingRequest) httpRequestReceiveRequestHeader);
                }
            }
            if (httpResponseNewHttpResponse == null) {
                httpResponseNewHttpResponse = this.responseFactory.newHttpResponse(protocolVersion, HttpStatus.SC_OK, httpContext);
                httpResponseNewHttpResponse.setParams(new DefaultedHttpParams(httpResponseNewHttpResponse.getParams(), this.params));
                httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, httpRequestReceiveRequestHeader);
                httpContext.setAttribute(ExecutionContext.HTTP_RESPONSE, httpResponseNewHttpResponse);
                this.processor.process(httpRequestReceiveRequestHeader, httpContext);
                doService(httpRequestReceiveRequestHeader, httpResponseNewHttpResponse, httpContext);
            }
            if ((httpRequestReceiveRequestHeader instanceof HttpEntityEnclosingRequest) && (entity = ((HttpEntityEnclosingRequest) httpRequestReceiveRequestHeader).getEntity()) != null) {
                entity.consumeContent();
            }
        } catch (HttpException e2) {
            httpResponseNewHttpResponse = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_0, HttpStatus.SC_INTERNAL_SERVER_ERROR, httpContext);
            httpResponseNewHttpResponse.setParams(new DefaultedHttpParams(httpResponseNewHttpResponse.getParams(), this.params));
            handleException(e2, httpResponseNewHttpResponse);
        }
        this.processor.process(httpResponseNewHttpResponse, httpContext);
        httpServerConnection.sendResponseHeader(httpResponseNewHttpResponse);
        httpServerConnection.sendResponseEntity(httpResponseNewHttpResponse);
        httpServerConnection.flush();
        if (!this.connStrategy.keepAlive(httpResponseNewHttpResponse, httpContext)) {
            httpServerConnection.close();
        }
    }

    protected void handleException(HttpException httpException, HttpResponse httpResponse) {
        if (httpException instanceof MethodNotSupportedException) {
            httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } else if (httpException instanceof UnsupportedHttpVersionException) {
            httpResponse.setStatusCode(HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED);
        } else if (httpException instanceof ProtocolException) {
            httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        } else {
            httpResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        ByteArrayEntity byteArrayEntity = new ByteArrayEntity(EncodingUtils.getAsciiBytes(httpException.getMessage()));
        byteArrayEntity.setContentType("text/plain; charset=US-ASCII");
        httpResponse.setEntity(byteArrayEntity);
    }

    protected void doService(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
        HttpRequestHandler httpRequestHandlerLookup;
        if (this.handlerResolver != null) {
            httpRequestHandlerLookup = this.handlerResolver.lookup(httpRequest.getRequestLine().getUri());
        } else {
            httpRequestHandlerLookup = null;
        }
        if (httpRequestHandlerLookup != null) {
            httpRequestHandlerLookup.handle(httpRequest, httpResponse, httpContext);
        } else {
            httpResponse.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
}
