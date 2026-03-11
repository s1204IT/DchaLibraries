package jp.co.benesse.dcha.systemsettings;

import jp.co.benesse.dcha.util.Logger;

public class HttpResponse {
    private StringBuffer sb;
    private int statusCode;

    public HttpResponse(int statusCode, StringBuffer sb) {
        this.sb = null;
        Logger.d("HttpResponse", "HttpResponse 0001");
        this.sb = new StringBuffer();
        this.sb.append(sb);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        Logger.d("HttpResponse", "getStatusCode 0001");
        return this.statusCode;
    }

    public String getEntity() {
        Logger.d("HttpResponse", "getEntity 0001");
        if (this.sb != null && this.sb.length() > 0) {
            Logger.d("HttpResponse", "getEntity 0002");
            return this.sb.toString();
        }
        Logger.d("HttpResponse", "getEntity 0003");
        return null;
    }
}
