package java.sql;

import java.util.HashMap;
import java.util.Map;

public class SQLClientInfoException extends SQLException {
    private static final long serialVersionUID = -4319604256824655880L;
    private final Map<String, ClientInfoStatus> failedProperties;

    public SQLClientInfoException() {
        this.failedProperties = null;
    }

    public SQLClientInfoException(Map<String, ClientInfoStatus> failedProperties) {
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(cause);
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(String reason, Map<String, ClientInfoStatus> failedProperties) {
        super(reason);
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(String reason, Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(reason, cause);
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(String reason, String sqlState, int vendorCode, Map<String, ClientInfoStatus> failedProperties) {
        super(reason, sqlState, vendorCode);
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(String reason, String sqlState, int vendorCode, Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(reason, sqlState, vendorCode, cause);
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(String reason, String sqlState, Map<String, ClientInfoStatus> failedProperties) {
        super(reason, sqlState);
        this.failedProperties = new HashMap(failedProperties);
    }

    public SQLClientInfoException(String reason, String sqlState, Map<String, ClientInfoStatus> failedProperties, Throwable cause) {
        super(reason, sqlState, cause);
        this.failedProperties = new HashMap(failedProperties);
    }

    public Map<String, ClientInfoStatus> getFailedProperties() {
        return this.failedProperties;
    }
}
