package org.apache.http.protocol;

@Deprecated
public final class DefaultedHttpContext implements HttpContext {
    private final HttpContext defaults;
    private final HttpContext local;

    public DefaultedHttpContext(HttpContext httpContext, HttpContext httpContext2) {
        if (httpContext == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        this.local = httpContext;
        this.defaults = httpContext2;
    }

    @Override
    public Object getAttribute(String str) {
        Object attribute = this.local.getAttribute(str);
        if (attribute == null) {
            return this.defaults.getAttribute(str);
        }
        return attribute;
    }

    @Override
    public Object removeAttribute(String str) {
        return this.local.removeAttribute(str);
    }

    @Override
    public void setAttribute(String str, Object obj) {
        this.local.setAttribute(str, obj);
    }

    public HttpContext getDefaults() {
        return this.defaults;
    }
}
