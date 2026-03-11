package jp.co.benesse.dcha.setupwizard.http;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import jp.co.benesse.dcha.setupwizard.http.Request;
import jp.co.benesse.dcha.util.Logger;

public class HttpThread extends Thread {
    private static final String TAG = HttpThread.class.getSimpleName();
    protected volatile boolean mIsRunning = false;
    protected Condition mListCondition;
    protected Lock mListLock;
    protected Collection<Request> mRequestList;
    protected Request.ResponseListener mResponseListener;
    protected Condition mRetryWaitCondition;
    protected Lock mRetryWaitLock;

    public HttpThread() {
        Logger.d(TAG, "HttpThread 0001");
        this.mRequestList = new ArrayList();
        this.mListLock = new ReentrantLock();
        this.mListCondition = this.mListLock.newCondition();
        this.mRetryWaitLock = new ReentrantLock();
        this.mRetryWaitCondition = this.mRetryWaitLock.newCondition();
        this.mResponseListener = null;
        Logger.d(TAG, "HttpThread 0002");
    }

    @Override
    public void start() {
        this.mIsRunning = true;
        super.start();
    }

    @Override
    public void run() {
        Request next;
        HttpURLConnection httpURLConnectionOpenConnection;
        Throwable th;
        Lock lock;
        Logger.d(TAG, "run 0001");
        super.run();
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        do {
            Response responseReceiveResponse = null;
            try {
                try {
                    if (this.mListLock.tryLock(100L, TimeUnit.MILLISECONDS)) {
                        Logger.d(TAG, "run 0002");
                        if (isRunning() && this.mRequestList.isEmpty()) {
                            Logger.d(TAG, "run 0003");
                            this.mListCondition.await();
                        }
                        Iterator<Request> it = this.mRequestList.iterator();
                        if (it.hasNext()) {
                            Logger.d(TAG, "run 0004");
                            next = it.next();
                        } else {
                            next = null;
                        }
                    }
                } finally {
                }
            } catch (InterruptedException unused) {
                Logger.d(TAG, "run 0005");
                this.mListLock.unlock();
                next = null;
            }
            if (next != null) {
                Logger.d(TAG, "run 0006");
                int i = 0;
                while (true) {
                    try {
                        if (!isRunning() || next.isCancelled()) {
                            break;
                        }
                        Logger.d(TAG, "run 0007");
                        try {
                            httpURLConnectionOpenConnection = openConnection(next.url);
                            try {
                                try {
                                    sendRequest(httpURLConnectionOpenConnection, next);
                                    responseReceiveResponse = receiveResponse(httpURLConnectionOpenConnection, next);
                                    disconnect(httpURLConnectionOpenConnection);
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                    disconnect(httpURLConnectionOpenConnection);
                                    throw th;
                                }
                            } catch (Exception e) {
                                e = e;
                                Logger.d(TAG, "run 0008");
                                Logger.d(TAG, "run", e);
                                disconnect(httpURLConnectionOpenConnection);
                                i++;
                                if (i > next.maxNumRetries) {
                                    Logger.d(TAG, "run 0009");
                                    break;
                                }
                                if (next.retryInterval > 0) {
                                    try {
                                        try {
                                            Logger.d(TAG, "run 0010");
                                            this.mRetryWaitLock.lock();
                                            this.mRetryWaitCondition.await(next.retryInterval * ((long) i), TimeUnit.MILLISECONDS);
                                            lock = this.mRetryWaitLock;
                                        } catch (InterruptedException unused2) {
                                            Logger.d(TAG, "run 0011");
                                            lock = this.mRetryWaitLock;
                                        }
                                        lock.unlock();
                                    } catch (Throwable th3) {
                                        this.mRetryWaitLock.unlock();
                                        throw th3;
                                    }
                                }
                            }
                        } catch (Exception e2) {
                            e = e2;
                            httpURLConnectionOpenConnection = responseReceiveResponse;
                        } catch (Throwable th4) {
                            httpURLConnectionOpenConnection = responseReceiveResponse;
                            th = th4;
                        }
                    } catch (Throwable th5) {
                        try {
                            this.mListLock.lock();
                            this.mRequestList.remove(next);
                            throw th5;
                        } finally {
                        }
                    }
                }
                if (isRunning() && next.responseListener != null) {
                    if (next.isCancelled()) {
                        Logger.d(TAG, "run 0012");
                        next.responseListener.onHttpCancelled(next);
                    } else if (responseReceiveResponse == null) {
                        Logger.d(TAG, "run 0013");
                        next.responseListener.onHttpError(next);
                    } else {
                        Logger.d(TAG, "run 0014");
                        next.responseListener.onHttpResponse(responseReceiveResponse);
                    }
                }
                try {
                    this.mListLock.lock();
                    this.mRequestList.remove(next);
                } finally {
                }
            }
        } while (isRunning());
        Logger.d(TAG, "run 0015");
    }

    public boolean isRunning() {
        Logger.d(TAG, "isRunning 0001");
        return this.mIsRunning;
    }

    public void stopRunning() {
        Logger.d(TAG, "stopRunning 0001");
        this.mIsRunning = false;
        cancel();
    }

    public void cancel() {
        Logger.d(TAG, "cancel 0001");
        try {
            this.mListLock.lock();
            Iterator<Request> it = this.mRequestList.iterator();
            while (it.hasNext()) {
                Logger.d(TAG, "cancel 0002");
                it.next().cancel();
            }
            this.mListCondition.signal();
            try {
                this.mRetryWaitLock.lock();
                this.mRetryWaitCondition.signal();
                this.mRetryWaitLock.unlock();
                Logger.d(TAG, "cancel 0003");
            } catch (Throwable th) {
                this.mRetryWaitLock.unlock();
                throw th;
            }
        } finally {
            this.mListLock.unlock();
        }
    }

    public void postRequest(Request request) {
        Logger.d(TAG, "postRequest 0001");
        try {
            this.mListLock.lock();
            request.responseListener = this.mResponseListener;
            this.mRequestList.add(request);
            this.mListCondition.signal();
            this.mListLock.unlock();
            Logger.d(TAG, "postRequest 0002");
        } catch (Throwable th) {
            this.mListLock.unlock();
            throw th;
        }
    }

    public void setResponseListener(Request.ResponseListener responseListener) {
        Logger.d(TAG, "setResponseListener 0001");
        this.mResponseListener = responseListener;
        Logger.d(TAG, "setResponseListener 0002");
    }

    protected void sendRequest(HttpURLConnection httpURLConnection, Request request) throws IOException {
        Logger.d(TAG, "sendRequest 0001");
        httpURLConnection.setReadTimeout(request.readTimeout);
        httpURLConnection.setConnectTimeout(request.connectTimeout);
        httpURLConnection.setRequestMethod(request.method);
        httpURLConnection.setInstanceFollowRedirects(request.followRedirects);
        httpURLConnection.setDoInput(request.doInput);
        httpURLConnection.setDoOutput(request.doOutput);
        httpURLConnection.setAllowUserInteraction(request.allowUserInteraction);
        httpURLConnection.setUseCaches(request.useCaches);
        httpURLConnection.setRequestProperty("Connection", "close");
        for (Map.Entry<String, String> entry : request.requestProperty.entrySet()) {
            Logger.d(TAG, "sendRequest 0002 key:" + entry.getKey() + " value:" + entry.getValue());
            httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        httpURLConnection.connect();
        request.onSendData(httpURLConnection);
        Logger.d(TAG, "sendRequest 0003");
    }

    protected Response receiveResponse(HttpURLConnection httpURLConnection, Request request) throws IOException {
        Logger.d(TAG, "receiveResponse 0001");
        Response responseNewResponseInstance = newResponseInstance(request.getResponseClass());
        responseNewResponseInstance.request = request;
        responseNewResponseInstance.responseCode = httpURLConnection.getResponseCode();
        if (responseNewResponseInstance.isSuccess()) {
            Logger.d(TAG, "receiveResponse 0002");
            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            responseNewResponseInstance.contentLength = getContentLength(headerFields);
            responseNewResponseInstance.contentType = getContentType(headerFields);
            responseNewResponseInstance.onReceiveData(httpURLConnection);
        }
        Logger.d(TAG, "receiveResponse 0003");
        return responseNewResponseInstance;
    }

    protected Response newResponseInstance(Class<? extends Response> cls) {
        Response responseNewInstance;
        Logger.d(TAG, "newResponseInstance 0001");
        try {
            responseNewInstance = cls.getConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            Logger.d(TAG, "newResponseInstance 0002");
            Logger.d(TAG, "newResponseInstance", e);
            responseNewInstance = null;
        }
        Logger.d(TAG, "newResponseInstance 0003");
        return responseNewInstance;
    }

    protected long getContentLength(Map<String, List<String>> map) {
        long j;
        Logger.d(TAG, "getContentLength 0001");
        List<String> list = map.get("Content-Length");
        if (list == null || list.isEmpty()) {
            j = 0;
        } else {
            Logger.d(TAG, "getContentLength 0002");
            j = Long.parseLong(list.get(0));
        }
        Logger.d(TAG, "getContentLength 0003");
        return j;
    }

    protected String getContentType(Map<String, List<String>> map) {
        String str;
        Logger.d(TAG, "getContentType 0001");
        List<String> list = map.get("Content-Type");
        if (list == null || list.isEmpty()) {
            str = null;
        } else {
            Logger.d(TAG, "getContentType 0002");
            str = list.get(0);
        }
        Logger.d(TAG, "getContentType 0003");
        return str;
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    protected void disconnect(HttpURLConnection httpURLConnection) {
        if (httpURLConnection != null) {
            httpURLConnection.disconnect();
        }
    }
}
