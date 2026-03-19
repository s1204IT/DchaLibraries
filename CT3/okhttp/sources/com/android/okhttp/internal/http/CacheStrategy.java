package com.android.okhttp.internal.http;

import com.android.okhttp.CacheControl;
import com.android.okhttp.Headers;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class CacheStrategy {
    public final Response cacheResponse;
    public final Request networkRequest;

    CacheStrategy(Request networkRequest, Response cacheResponse, CacheStrategy cacheStrategy) {
        this(networkRequest, cacheResponse);
    }

    private CacheStrategy(Request networkRequest, Response cacheResponse) {
        this.networkRequest = networkRequest;
        this.cacheResponse = cacheResponse;
    }

    public static boolean isCacheable(Response response, Request request) {
        switch (response.code()) {
            case 200:
            case 203:
            case 204:
            case 300:
            case 301:
            case StatusLine.HTTP_PERM_REDIRECT:
            case 404:
            case 405:
            case 410:
            case 414:
            case 501:
                break;
            case 302:
            case StatusLine.HTTP_TEMP_REDIRECT:
                if (response.header("Expires") == null) {
                    if (response.cacheControl().maxAgeSeconds() == -1) {
                        if (!response.cacheControl().isPublic()) {
                        }
                    }
                    break;
                }
            default:
                return false;
        }
        return (response.cacheControl().noStore() || request.cacheControl().noStore()) ? false : true;
    }

    public static class Factory {
        private int ageSeconds;
        final Response cacheResponse;
        private String etag;
        private Date expires;
        private Date lastModified;
        private String lastModifiedString;
        final long nowMillis;
        private long receivedResponseMillis;
        final Request request;
        private long sentRequestMillis;
        private Date servedDate;
        private String servedDateString;

        public Factory(long nowMillis, Request request, Response cacheResponse) {
            this.ageSeconds = -1;
            this.nowMillis = nowMillis;
            this.request = request;
            this.cacheResponse = cacheResponse;
            if (cacheResponse == null) {
                return;
            }
            Headers headers = cacheResponse.headers();
            int size = headers.size();
            for (int i = 0; i < size; i++) {
                String fieldName = headers.name(i);
                String value = headers.value(i);
                if ("Date".equalsIgnoreCase(fieldName)) {
                    this.servedDate = HttpDate.parse(value);
                    this.servedDateString = value;
                } else if ("Expires".equalsIgnoreCase(fieldName)) {
                    this.expires = HttpDate.parse(value);
                } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
                    this.lastModified = HttpDate.parse(value);
                    this.lastModifiedString = value;
                } else if ("ETag".equalsIgnoreCase(fieldName)) {
                    this.etag = value;
                } else if ("Age".equalsIgnoreCase(fieldName)) {
                    this.ageSeconds = HeaderParser.parseSeconds(value, -1);
                } else if (OkHeaders.SENT_MILLIS.equalsIgnoreCase(fieldName)) {
                    this.sentRequestMillis = Long.parseLong(value);
                } else if (OkHeaders.RECEIVED_MILLIS.equalsIgnoreCase(fieldName)) {
                    this.receivedResponseMillis = Long.parseLong(value);
                }
            }
        }

        public CacheStrategy get() {
            Request request = null;
            Object[] objArr = 0;
            Object[] objArr2 = 0;
            CacheStrategy candidate = getCandidate();
            if (candidate.networkRequest != null && this.request.cacheControl().onlyIfCached()) {
                return new CacheStrategy(request, objArr2 == true ? 1 : 0, objArr == true ? 1 : 0);
            }
            return candidate;
        }

        private CacheStrategy getCandidate() {
            if (this.cacheResponse == null) {
                return new CacheStrategy(this.request, null, null);
            }
            if (this.request.isHttps() && this.cacheResponse.handshake() == null) {
                return new CacheStrategy(this.request, null, null);
            }
            if (!CacheStrategy.isCacheable(this.cacheResponse, this.request)) {
                return new CacheStrategy(this.request, null, null);
            }
            CacheControl requestCaching = this.request.cacheControl();
            if (requestCaching.noCache() || hasConditions(this.request)) {
                return new CacheStrategy(this.request, null, null);
            }
            long ageMillis = cacheResponseAge();
            long freshMillis = computeFreshnessLifetime();
            if (requestCaching.maxAgeSeconds() != -1) {
                freshMillis = Math.min(freshMillis, TimeUnit.SECONDS.toMillis(requestCaching.maxAgeSeconds()));
            }
            long minFreshMillis = 0;
            if (requestCaching.minFreshSeconds() != -1) {
                minFreshMillis = TimeUnit.SECONDS.toMillis(requestCaching.minFreshSeconds());
            }
            long maxStaleMillis = 0;
            CacheControl responseCaching = this.cacheResponse.cacheControl();
            if (!responseCaching.mustRevalidate() && requestCaching.maxStaleSeconds() != -1) {
                maxStaleMillis = TimeUnit.SECONDS.toMillis(requestCaching.maxStaleSeconds());
            }
            if (!responseCaching.noCache() && ageMillis + minFreshMillis < freshMillis + maxStaleMillis) {
                Response.Builder builder = this.cacheResponse.newBuilder();
                if (ageMillis + minFreshMillis >= freshMillis) {
                    builder.addHeader("Warning", "110 HttpURLConnection \"Response is stale\"");
                }
                if (ageMillis > 86400000 && isFreshnessLifetimeHeuristic()) {
                    builder.addHeader("Warning", "113 HttpURLConnection \"Heuristic expiration\"");
                }
                return new CacheStrategy(null, builder.build(), null);
            }
            Request.Builder conditionalRequestBuilder = this.request.newBuilder();
            if (this.etag != null) {
                conditionalRequestBuilder.header("If-None-Match", this.etag);
            } else if (this.lastModified != null) {
                conditionalRequestBuilder.header("If-Modified-Since", this.lastModifiedString);
            } else if (this.servedDate != null) {
                conditionalRequestBuilder.header("If-Modified-Since", this.servedDateString);
            }
            Request conditionalRequest = conditionalRequestBuilder.build();
            if (hasConditions(conditionalRequest)) {
                return new CacheStrategy(conditionalRequest, this.cacheResponse, null);
            }
            return new CacheStrategy(conditionalRequest, null, null);
        }

        private long computeFreshnessLifetime() {
            long servedMillis;
            long servedMillis2;
            CacheControl responseCaching = this.cacheResponse.cacheControl();
            if (responseCaching.maxAgeSeconds() != -1) {
                return TimeUnit.SECONDS.toMillis(responseCaching.maxAgeSeconds());
            }
            if (this.expires != null) {
                if (this.servedDate != null) {
                    servedMillis2 = this.servedDate.getTime();
                } else {
                    servedMillis2 = this.receivedResponseMillis;
                }
                long delta = this.expires.getTime() - servedMillis2;
                if (delta > 0) {
                    return delta;
                }
                return 0L;
            }
            if (this.lastModified == null || this.cacheResponse.request().httpUrl().query() != null) {
                return 0L;
            }
            if (this.servedDate != null) {
                servedMillis = this.servedDate.getTime();
            } else {
                servedMillis = this.sentRequestMillis;
            }
            long delta2 = servedMillis - this.lastModified.getTime();
            if (delta2 > 0) {
                return delta2 / 10;
            }
            return 0L;
        }

        private long cacheResponseAge() {
            long apparentReceivedAge;
            long receivedAge;
            if (this.servedDate != null) {
                apparentReceivedAge = Math.max(0L, this.receivedResponseMillis - this.servedDate.getTime());
            } else {
                apparentReceivedAge = 0;
            }
            if (this.ageSeconds != -1) {
                receivedAge = Math.max(apparentReceivedAge, TimeUnit.SECONDS.toMillis(this.ageSeconds));
            } else {
                receivedAge = apparentReceivedAge;
            }
            long responseDuration = this.receivedResponseMillis - this.sentRequestMillis;
            long residentDuration = this.nowMillis - this.receivedResponseMillis;
            return receivedAge + responseDuration + residentDuration;
        }

        private boolean isFreshnessLifetimeHeuristic() {
            return this.cacheResponse.cacheControl().maxAgeSeconds() == -1 && this.expires == null;
        }

        private static boolean hasConditions(Request request) {
            return (request.header("If-Modified-Since") == null && request.header("If-None-Match") == null) ? false : true;
        }
    }
}
