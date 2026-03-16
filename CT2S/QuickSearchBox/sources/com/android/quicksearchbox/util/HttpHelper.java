package com.android.quicksearchbox.util;

import java.io.IOException;
import java.util.Map;

public interface HttpHelper {

    public interface UrlRewriter {
        String rewrite(String str);
    }

    String get(GetRequest getRequest) throws IOException;

    public static class GetRequest {
        private Map<String, String> mHeaders;
        private String mUrl;

        public GetRequest() {
        }

        public GetRequest(String url) {
            this.mUrl = url;
        }

        public String getUrl() {
            return this.mUrl;
        }

        public Map<String, String> getHeaders() {
            return this.mHeaders;
        }
    }

    public static class HttpException extends IOException {
        private final String mReasonPhrase;
        private final int mStatusCode;

        public HttpException(int statusCode, String reasonPhrase) {
            super(statusCode + " " + reasonPhrase);
            this.mStatusCode = statusCode;
            this.mReasonPhrase = reasonPhrase;
        }
    }
}
