package org.apache.http.params;

import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;

@Deprecated
public final class HttpProtocolParams implements CoreProtocolPNames {
    private HttpProtocolParams() {
    }

    public static String getHttpElementCharset(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        String str = (String) httpParams.getParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET);
        if (str == null) {
            return "US-ASCII";
        }
        return str;
    }

    public static void setHttpElementCharset(HttpParams httpParams, String str) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET, str);
    }

    public static String getContentCharset(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        String str = (String) httpParams.getParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET);
        if (str == null) {
            return "ISO-8859-1";
        }
        return str;
    }

    public static void setContentCharset(HttpParams httpParams, String str) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, str);
    }

    public static ProtocolVersion getVersion(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        Object parameter = httpParams.getParameter(CoreProtocolPNames.PROTOCOL_VERSION);
        if (parameter == null) {
            return HttpVersion.HTTP_1_1;
        }
        return (ProtocolVersion) parameter;
    }

    public static void setVersion(HttpParams httpParams, ProtocolVersion protocolVersion) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, protocolVersion);
    }

    public static String getUserAgent(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return (String) httpParams.getParameter(CoreProtocolPNames.USER_AGENT);
    }

    public static void setUserAgent(HttpParams httpParams, String str) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setParameter(CoreProtocolPNames.USER_AGENT, str);
    }

    public static boolean useExpectContinue(HttpParams httpParams) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        return httpParams.getBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
    }

    public static void setUseExpectContinue(HttpParams httpParams, boolean z) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        httpParams.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, z);
    }
}
