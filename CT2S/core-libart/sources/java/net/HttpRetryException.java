package java.net;

import java.io.IOException;

public class HttpRetryException extends IOException {
    private static final long serialVersionUID = -9186022286469111381L;
    private String location;
    private int responseCode;

    public HttpRetryException(String detail, int code) {
        super(detail);
        this.location = null;
        this.responseCode = code;
    }

    public HttpRetryException(String detail, int code, String location) {
        super(detail);
        this.location = null;
        this.responseCode = code;
        this.location = location;
    }

    public String getLocation() {
        return this.location;
    }

    public String getReason() {
        return getMessage();
    }

    public int responseCode() {
        return this.responseCode;
    }
}
