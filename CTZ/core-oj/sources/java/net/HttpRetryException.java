package java.net;

import java.io.IOException;

public class HttpRetryException extends IOException {
    private static final long serialVersionUID = -9186022286469111381L;
    private String location;
    private int responseCode;

    public HttpRetryException(String str, int i) {
        super(str);
        this.responseCode = i;
    }

    public HttpRetryException(String str, int i, String str2) {
        super(str);
        this.responseCode = i;
        this.location = str2;
    }

    public int responseCode() {
        return this.responseCode;
    }

    public String getReason() {
        return super.getMessage();
    }

    public String getLocation() {
        return this.location;
    }
}
