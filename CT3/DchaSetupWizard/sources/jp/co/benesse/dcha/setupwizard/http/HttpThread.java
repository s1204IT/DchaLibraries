package jp.co.benesse.dcha.setupwizard.http;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
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
    protected boolean mIsRunning = false;
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
    public synchronized void start() {
        super.start();
        this.mIsRunning = true;
    }

    @Override
    public void run() {
        Logger.d(TAG, "run 0001");
        super.run();
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cm);
        do {
            HttpURLConnection conn = null;
            Request request = null;
            try {
                if (this.mListLock.tryLock(100L, TimeUnit.MILLISECONDS)) {
                    Logger.d(TAG, "run 0002");
                    if (this.mRequestList.isEmpty()) {
                        Logger.d(TAG, "run 0003");
                        this.mListCondition.await();
                    }
                    Iterator<Request> it = this.mRequestList.iterator();
                    if (it.hasNext()) {
                        Logger.d(TAG, "run 0004");
                        request = it.next();
                    }
                }
            } catch (InterruptedException e) {
                Logger.d(TAG, "run 0005");
            } finally {
            }
            if (request != null) {
                Logger.d(TAG, "run 0006");
                if (request.url == null) {
                    Logger.d(TAG, "run 0007");
                    throw new IllegalArgumentException("Request.url is null");
                }
                Response response = null;
                int requestCount = 0;
                while (true) {
                    if (!isRunning() || request.isCancelled()) {
                        break;
                    }
                    Logger.d(TAG, "run 0008");
                    try {
                        try {
                            conn = (HttpURLConnection) request.url.openConnection();
                            sendRequest(conn, request);
                            response = receiveResponse(conn, request);
                        } catch (Exception e2) {
                            Logger.d(TAG, "run 0010");
                            if (conn != null) {
                                conn.disconnect();
                                conn = null;
                            }
                        }
                        if (response != null) {
                            Logger.d(TAG, "run 0009");
                            if (conn != null) {
                                conn.disconnect();
                            }
                        } else {
                            if (conn != null) {
                                conn.disconnect();
                                conn = null;
                            }
                            requestCount++;
                            if (requestCount > request.maxNumRetries) {
                                Logger.d(TAG, "run 0011");
                                break;
                            }
                            try {
                                this.mRetryWaitLock.lock();
                                this.mRetryWaitCondition.await(request.retryInterval * ((long) requestCount), TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e3) {
                                Logger.d(TAG, "run 0012");
                            } finally {
                                this.mRetryWaitLock.unlock();
                            }
                        }
                    } catch (Throwable th) {
                        if (conn != null) {
                            conn.disconnect();
                        }
                        throw th;
                    }
                }
                if (isRunning() && request.responseListener != null) {
                    Logger.d(TAG, "run 0013");
                    if (request.isCancelled()) {
                        Logger.d(TAG, "run 0014");
                        request.responseListener.onHttpCancelled(request);
                    } else if (response == null) {
                        Logger.d(TAG, "run 0015");
                        request.responseListener.onHttpError(request);
                    } else {
                        Logger.d(TAG, "run 0016");
                        request.responseListener.onHttpResponse(response);
                    }
                }
                try {
                    this.mListLock.lock();
                    this.mRequestList.remove(request);
                } finally {
                }
            }
        } while (isRunning());
        Logger.d(TAG, "run 0017");
    }

    public synchronized boolean isRunning() {
        Logger.d(TAG, "isRunning 0001");
        return this.mIsRunning;
    }

    public synchronized void stopRunning() {
        Logger.d(TAG, "stopRunning 0001");
        this.mIsRunning = false;
        cancel();
    }

    public synchronized void cancel() {
        Logger.d(TAG, "cancel 0001");
        try {
            this.mListLock.lock();
            for (Request request : this.mRequestList) {
                Logger.d(TAG, "cancel 0002");
                request.cancel();
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

    public void setResponseListener(Request.ResponseListener listener) {
        Logger.d(TAG, "setResponseListener 0001");
        this.mResponseListener = listener;
        Logger.d(TAG, "setResponseListener 0002");
    }

    protected void sendRequest(HttpURLConnection conn, Request request) throws IOException {
        Logger.d(TAG, "sendRequest 0001");
        conn.setReadTimeout(request.readTimeout);
        conn.setConnectTimeout(request.connectTimeout);
        conn.setRequestMethod(request.method);
        conn.setInstanceFollowRedirects(request.followRedirects);
        conn.setDoInput(request.doInput);
        conn.setDoOutput(request.doOutput);
        conn.setAllowUserInteraction(request.allowUserInteraction);
        conn.setUseCaches(request.useCaches);
        conn.setRequestProperty("Connection", "close");
        for (Map.Entry<String, String> entry : request.requestProperty.entrySet()) {
            Logger.d(TAG, "sendRequest 0002");
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.connect();
        request.onSendData(conn);
        Logger.d(TAG, "sendRequest 0003");
    }

    protected Response receiveResponse(HttpURLConnection conn, Request request) throws IOException {
        Logger.d(TAG, "receiveResponse 0001");
        Response response = newResponseInstance(request.getResponseClass());
        response.request = request;
        response.responseCode = conn.getResponseCode();
        if (response.isSuccess()) {
            Logger.d(TAG, "receiveResponse 0002");
            Map<String, List<String>> headers = conn.getHeaderFields();
            response.contentLength = getContentLength(headers);
            response.contentType = getContentType(headers);
            response = request.onReceiveData(conn, response);
        }
        Logger.d(TAG, "receiveResponse 0003");
        return response;
    }

    protected Response newResponseInstance(Class<? extends Response> cls) {
        Logger.d(TAG, "newResponseInstance 0001");
        Response response = null;
        try {
            Object[] args = new Object[0];
            Constructor<? extends Response> constructor = cls.getConstructor(new Class[0]);
            response = constructor.newInstance(args);
        } catch (Exception e) {
            Logger.d(TAG, "newResponseInstance 0002");
            Logger.d(TAG, "newResponseInstance", e);
        }
        Logger.d(TAG, "newResponseInstance 0003");
        return response;
    }

    protected long getContentLength(Map<String, List<String>> headers) {
        Logger.d(TAG, "getContentLength 0001");
        long result = 0;
        List<String> list = headers.get("Content-Length");
        if (list != null && !list.isEmpty()) {
            Logger.d(TAG, "getContentLength 0002");
            result = Long.parseLong(list.get(0));
        }
        Logger.d(TAG, "getContentLength 0003");
        return result;
    }

    protected String getContentType(Map<String, List<String>> headers) {
        Logger.d(TAG, "getContentType 0001");
        String result = null;
        List<String> list = headers.get("Content-Type");
        if (list != null && !list.isEmpty()) {
            Logger.d(TAG, "getContentType 0002");
            result = list.get(0);
        }
        Logger.d(TAG, "getContentType 0003");
        return result;
    }
}
