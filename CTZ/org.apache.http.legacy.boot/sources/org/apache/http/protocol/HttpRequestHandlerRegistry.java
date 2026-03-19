package org.apache.http.protocol;

import java.util.Map;

@Deprecated
public class HttpRequestHandlerRegistry implements HttpRequestHandlerResolver {
    private final UriPatternMatcher matcher = new UriPatternMatcher();

    public void register(String str, HttpRequestHandler httpRequestHandler) {
        this.matcher.register(str, httpRequestHandler);
    }

    public void unregister(String str) {
        this.matcher.unregister(str);
    }

    public void setHandlers(Map map) {
        this.matcher.setHandlers(map);
    }

    @Override
    public HttpRequestHandler lookup(String str) {
        return (HttpRequestHandler) this.matcher.lookup(str);
    }

    @Deprecated
    protected boolean matchUriRequestPattern(String str, String str2) {
        return this.matcher.matchUriRequestPattern(str, str2);
    }
}
