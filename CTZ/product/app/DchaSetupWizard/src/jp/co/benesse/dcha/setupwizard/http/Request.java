package jp.co.benesse.dcha.setupwizard.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public abstract class Request {
    private static final String TAG = Request.class.getSimpleName();
    ResponseListener responseListener;
    public URL url = null;
    public String method = "GET";
    public boolean followRedirects = true;
    public boolean doOutput = false;
    public boolean doInput = true;
    public boolean allowUserInteraction = false;
    public boolean useCaches = false;
    public int readTimeout = 180000;
    public int connectTimeout = 60000;
    public Map<String, String> requestProperty = new HashMap();
    private boolean cancel = false;
    int maxNumRetries = 0;
    long retryInterval = 0;

    public interface ResponseListener extends EventListener {
        void onHttpCancelled(Request request);

        void onHttpError(Request request);

        void onHttpProgress(Response response);

        void onHttpResponse(Response response);
    }

    abstract Class<? extends Response> getResponseClass();

    abstract void onSendData(HttpURLConnection httpURLConnection) throws IOException;

    protected synchronized void cancel() {
        Logger.d(TAG, "cancel 0001");
        this.cancel = true;
    }

    protected synchronized boolean isCancelled() {
        Logger.d(TAG, "isCancelled 0001");
        return this.cancel;
    }
}
