package com.android.quicksearchbox.util;

import java.io.IOException;
import java.util.Map;

public interface HttpHelper {

    public static class GetRequest {
        private Map<String, String> mHeaders;
        private String mUrl;

        public GetRequest() {
        }

        public GetRequest(String str) {
            this.mUrl = str;
        }

        public Map<String, String> getHeaders() {
            return this.mHeaders;
        }

        public String getUrl() {
            return this.mUrl;
        }
    }

    public static class HttpException extends IOException {
        private final String mReasonPhrase;
        private final int mStatusCode;

        public HttpException(int i, String str) {
            super(i + " " + str);
            this.mStatusCode = i;
            this.mReasonPhrase = str;
        }
    }

    public interface UrlRewriter {
        String rewrite(String str);
    }

    String get(GetRequest getRequest) throws IOException;
}
