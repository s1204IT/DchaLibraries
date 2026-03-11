package jp.co.benesse.dcha.setupwizard.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import jp.co.benesse.dcha.util.Logger;

public abstract class Response {
    private static final String TAG = Response.class.getSimpleName();
    public long contentLength;
    public String contentType;
    public long receiveLength;
    public Request request;
    public int responseCode;

    abstract void onReceiveData(HttpURLConnection httpURLConnection) throws IOException;

    public Response() {
        this.request = null;
        this.responseCode = -1;
        this.contentLength = -1L;
        this.contentType = null;
        this.receiveLength = 0L;
    }

    public Response(Response response) {
        this.request = null;
        this.responseCode = -1;
        this.contentLength = -1L;
        this.contentType = null;
        this.receiveLength = 0L;
        Logger.d(TAG, "Response 0001");
        this.request = response.request;
        this.responseCode = response.responseCode;
        this.contentLength = response.contentLength;
        this.contentType = response.contentType;
        this.receiveLength = response.receiveLength;
        Logger.d(TAG, "Response 0002");
    }

    public boolean isSuccess() {
        int i = this.responseCode;
        return (i >= 200 && i < 300) || this.responseCode == 304;
    }
}
