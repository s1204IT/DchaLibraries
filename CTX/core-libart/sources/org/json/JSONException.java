package org.json;

public class JSONException extends Exception {
    public JSONException(String str) {
        super(str);
    }

    public JSONException(String str, Throwable th) {
        super(str, th);
    }

    public JSONException(Throwable th) {
        super(th);
    }
}
