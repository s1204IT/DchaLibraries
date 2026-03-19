package java.sql;

import java.util.Map;

public class SQLClientInfoException extends SQLException {
    private static final long serialVersionUID = -4319604256824655880L;
    private Map<String, ClientInfoStatus> failedProperties;

    public SQLClientInfoException() {
        this.failedProperties = null;
    }

    public SQLClientInfoException(Map<String, ClientInfoStatus> map) {
        this.failedProperties = map;
    }

    public SQLClientInfoException(Map<String, ClientInfoStatus> map, Throwable th) {
        super(th != null ? th.toString() : null);
        initCause(th);
        this.failedProperties = map;
    }

    public SQLClientInfoException(String str, Map<String, ClientInfoStatus> map) {
        super(str);
        this.failedProperties = map;
    }

    public SQLClientInfoException(String str, Map<String, ClientInfoStatus> map, Throwable th) {
        super(str);
        initCause(th);
        this.failedProperties = map;
    }

    public SQLClientInfoException(String str, String str2, Map<String, ClientInfoStatus> map) {
        super(str, str2);
        this.failedProperties = map;
    }

    public SQLClientInfoException(String str, String str2, Map<String, ClientInfoStatus> map, Throwable th) {
        super(str, str2);
        initCause(th);
        this.failedProperties = map;
    }

    public SQLClientInfoException(String str, String str2, int i, Map<String, ClientInfoStatus> map) {
        super(str, str2, i);
        this.failedProperties = map;
    }

    public SQLClientInfoException(String str, String str2, int i, Map<String, ClientInfoStatus> map, Throwable th) {
        super(str, str2, i);
        initCause(th);
        this.failedProperties = map;
    }

    public Map<String, ClientInfoStatus> getFailedProperties() {
        return this.failedProperties;
    }
}
