package com.android.okhttp;

import com.android.okhttp.Interceptor;
import com.android.okhttp.Request;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.NamedRunnable;
import com.android.okhttp.internal.http.HttpEngine;
import com.android.okhttp.internal.http.RequestException;
import com.android.okhttp.internal.http.RouteException;
import com.android.okhttp.internal.http.StreamAllocation;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.logging.Level;

public class Call {
    volatile boolean canceled;
    private final OkHttpClient client;
    HttpEngine engine;
    private boolean executed;
    Request originalRequest;

    protected Call(OkHttpClient okHttpClient, Request request) {
        this.client = okHttpClient.copyWithDefaults();
        this.originalRequest = request;
    }

    public Response execute() throws IOException {
        synchronized (this) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        try {
            this.client.getDispatcher().executed(this);
            Response responseWithInterceptorChain = getResponseWithInterceptorChain(false);
            if (responseWithInterceptorChain == null) {
                throw new IOException("Canceled");
            }
            return responseWithInterceptorChain;
        } finally {
            this.client.getDispatcher().finished(this);
        }
    }

    Object tag() {
        return this.originalRequest.tag();
    }

    public void enqueue(Callback callback) {
        enqueue(callback, false);
    }

    void enqueue(Callback callback, boolean z) {
        synchronized (this) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        this.client.getDispatcher().enqueue(new AsyncCall(callback, z));
    }

    public void cancel() {
        this.canceled = true;
        if (this.engine != null) {
            this.engine.cancel();
        }
    }

    public synchronized boolean isExecuted() {
        return this.executed;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    final class AsyncCall extends NamedRunnable {
        private final boolean forWebSocket;
        private final Callback responseCallback;

        private AsyncCall(Callback callback, boolean z) {
            super("OkHttp %s", Call.this.originalRequest.urlString());
            this.responseCallback = callback;
            this.forWebSocket = z;
        }

        String host() {
            return Call.this.originalRequest.httpUrl().host();
        }

        Request request() {
            return Call.this.originalRequest;
        }

        Object tag() {
            return Call.this.originalRequest.tag();
        }

        void cancel() {
            Call.this.cancel();
        }

        Call get() {
            return Call.this;
        }

        @Override
        protected void execute() {
            IOException e;
            boolean z = true;
            try {
                try {
                    Response responseWithInterceptorChain = Call.this.getResponseWithInterceptorChain(this.forWebSocket);
                    try {
                        if (Call.this.canceled) {
                            this.responseCallback.onFailure(Call.this.originalRequest, new IOException("Canceled"));
                        } else {
                            this.responseCallback.onResponse(responseWithInterceptorChain);
                        }
                    } catch (IOException e2) {
                        e = e2;
                        if (z) {
                            Internal.logger.log(Level.INFO, "Callback failure for " + Call.this.toLoggableString(), (Throwable) e);
                        } else {
                            this.responseCallback.onFailure(Call.this.engine == null ? Call.this.originalRequest : Call.this.engine.getRequest(), e);
                        }
                    }
                } finally {
                    Call.this.client.getDispatcher().finished(this);
                }
            } catch (IOException e3) {
                e = e3;
                z = false;
            }
        }
    }

    private String toLoggableString() {
        return (this.canceled ? "canceled call" : "call") + " to " + this.originalRequest.httpUrl().resolve("/...");
    }

    private Response getResponseWithInterceptorChain(boolean z) throws IOException {
        return new ApplicationInterceptorChain(0, this.originalRequest, z).proceed(this.originalRequest);
    }

    class ApplicationInterceptorChain implements Interceptor.Chain {
        private final boolean forWebSocket;
        private final int index;
        private final Request request;

        ApplicationInterceptorChain(int i, Request request, boolean z) {
            this.index = i;
            this.request = request;
            this.forWebSocket = z;
        }

        @Override
        public Connection connection() {
            return null;
        }

        @Override
        public Request request() {
            return this.request;
        }

        @Override
        public Response proceed(Request request) throws IOException {
            if (this.index < Call.this.client.interceptors().size()) {
                ApplicationInterceptorChain applicationInterceptorChain = Call.this.new ApplicationInterceptorChain(this.index + 1, request, this.forWebSocket);
                Interceptor interceptor = Call.this.client.interceptors().get(this.index);
                Response responseIntercept = interceptor.intercept(applicationInterceptorChain);
                if (responseIntercept == null) {
                    throw new NullPointerException("application interceptor " + interceptor + " returned null");
                }
                return responseIntercept;
            }
            return Call.this.getResponse(request, this.forWebSocket);
        }
    }

    Response getResponse(Request request, boolean z) throws Throwable {
        Throwable th;
        Response response;
        Request requestFollowUpRequest;
        StreamAllocation streamAllocation;
        RequestBody requestBodyBody = request.body();
        if (requestBodyBody != null) {
            Request.Builder builderNewBuilder = request.newBuilder();
            MediaType mediaTypeContentType = requestBodyBody.contentType();
            if (mediaTypeContentType != null) {
                builderNewBuilder.header("Content-Type", mediaTypeContentType.toString());
            }
            long jContentLength = requestBodyBody.contentLength();
            if (jContentLength != -1) {
                builderNewBuilder.header("Content-Length", Long.toString(jContentLength));
                builderNewBuilder.removeHeader("Transfer-Encoding");
            } else {
                builderNewBuilder.header("Transfer-Encoding", "chunked");
                builderNewBuilder.removeHeader("Content-Length");
            }
            request = builderNewBuilder.build();
        }
        this.engine = new HttpEngine(this.client, request, false, false, z, null, null, null);
        int i = 0;
        while (!this.canceled) {
            boolean z2 = true;
            try {
                try {
                    try {
                        this.engine.sendRequest();
                        this.engine.readResponse();
                        response = this.engine.getResponse();
                        requestFollowUpRequest = this.engine.followUpRequest();
                    } catch (RequestException e) {
                        throw e.getCause();
                    }
                } catch (RouteException e2) {
                    HttpEngine httpEngineRecover = this.engine.recover(e2);
                    if (httpEngineRecover == null) {
                        throw e2.getLastConnectException();
                    }
                    this.engine = httpEngineRecover;
                } catch (IOException e3) {
                    HttpEngine httpEngineRecover2 = this.engine.recover(e3, null);
                    if (httpEngineRecover2 == null) {
                        throw e3;
                    }
                    try {
                        this.engine = httpEngineRecover2;
                    } catch (Throwable th2) {
                        z2 = false;
                        th = th2;
                        if (z2) {
                            this.engine.close().release();
                        }
                        throw th;
                    }
                }
                if (requestFollowUpRequest == null) {
                    if (!z) {
                        this.engine.releaseStreamAllocation();
                    }
                    return response;
                }
                StreamAllocation streamAllocationClose = this.engine.close();
                i++;
                if (i > 20) {
                    streamAllocationClose.release();
                    throw new ProtocolException("Too many follow-up requests: " + i);
                }
                if (this.engine.sameConnection(requestFollowUpRequest.httpUrl())) {
                    streamAllocation = streamAllocationClose;
                } else {
                    streamAllocationClose.release();
                    streamAllocation = null;
                }
                this.engine = new HttpEngine(this.client, requestFollowUpRequest, false, false, z, streamAllocation, null, response);
            } catch (Throwable th3) {
                th = th3;
                if (z2) {
                }
                throw th;
            }
        }
        this.engine.releaseStreamAllocation();
        throw new IOException("Canceled");
    }
}
