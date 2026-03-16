package org.apache.http.impl;

import gov.nist.core.Separators;
import java.util.Locale;
import org.apache.http.HttpStatus;
import org.apache.http.ReasonPhraseCatalog;

@Deprecated
public class EnglishReasonPhraseCatalog implements ReasonPhraseCatalog {
    public static final EnglishReasonPhraseCatalog INSTANCE = new EnglishReasonPhraseCatalog();
    private static final String[][] REASON_PHRASES = {null, new String[3], new String[8], new String[8], new String[25], new String[8]};

    static {
        setReason(200, "OK");
        setReason(HttpStatus.SC_CREATED, "Created");
        setReason(202, "Accepted");
        setReason(HttpStatus.SC_NO_CONTENT, "No Content");
        setReason(301, "Moved Permanently");
        setReason(302, "Moved Temporarily");
        setReason(HttpStatus.SC_NOT_MODIFIED, "Not Modified");
        setReason(400, "Bad Request");
        setReason(401, "Unauthorized");
        setReason(403, "Forbidden");
        setReason(404, "Not Found");
        setReason(500, "Internal Server Error");
        setReason(501, "Not Implemented");
        setReason(502, "Bad Gateway");
        setReason(503, "Service Unavailable");
        setReason(100, "Continue");
        setReason(HttpStatus.SC_TEMPORARY_REDIRECT, "Temporary Redirect");
        setReason(405, "Method Not Allowed");
        setReason(HttpStatus.SC_CONFLICT, "Conflict");
        setReason(412, "Precondition Failed");
        setReason(413, "Request Too Long");
        setReason(414, "Request-URI Too Long");
        setReason(415, "Unsupported Media Type");
        setReason(300, "Multiple Choices");
        setReason(HttpStatus.SC_SEE_OTHER, "See Other");
        setReason(305, "Use Proxy");
        setReason(402, "Payment Required");
        setReason(406, "Not Acceptable");
        setReason(407, "Proxy Authentication Required");
        setReason(408, "Request Timeout");
        setReason(HttpStatus.SC_SWITCHING_PROTOCOLS, "Switching Protocols");
        setReason(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Non Authoritative Information");
        setReason(HttpStatus.SC_RESET_CONTENT, "Reset Content");
        setReason(HttpStatus.SC_PARTIAL_CONTENT, "Partial Content");
        setReason(504, "Gateway Timeout");
        setReason(505, "Http Version Not Supported");
        setReason(410, "Gone");
        setReason(HttpStatus.SC_LENGTH_REQUIRED, "Length Required");
        setReason(416, "Requested Range Not Satisfiable");
        setReason(HttpStatus.SC_EXPECTATION_FAILED, "Expectation Failed");
        setReason(HttpStatus.SC_PROCESSING, "Processing");
        setReason(HttpStatus.SC_MULTI_STATUS, "Multi-Status");
        setReason(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Unprocessable Entity");
        setReason(HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE, "Insufficient Space On Resource");
        setReason(420, "Method Failure");
        setReason(423, "Locked");
        setReason(HttpStatus.SC_INSUFFICIENT_STORAGE, "Insufficient Storage");
        setReason(HttpStatus.SC_FAILED_DEPENDENCY, "Failed Dependency");
    }

    protected EnglishReasonPhraseCatalog() {
    }

    @Override
    public String getReason(int status, Locale loc) {
        if (status < 100 || status >= 600) {
            throw new IllegalArgumentException("Unknown category for status code " + status + Separators.DOT);
        }
        int category = status / 100;
        int subcode = status - (category * 100);
        if (REASON_PHRASES[category].length <= subcode) {
            return null;
        }
        String reason = REASON_PHRASES[category][subcode];
        return reason;
    }

    private static void setReason(int status, String reason) {
        int category = status / 100;
        int subcode = status - (category * 100);
        REASON_PHRASES[category][subcode] = reason;
    }
}
