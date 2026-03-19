package android.net.http;

import javax.net.ssl.SSLException;

class SSLConnectionClosedByUserException extends SSLException {
    public SSLConnectionClosedByUserException(String str) {
        super(str);
    }
}
