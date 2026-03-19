package org.apache.http.params;

@Deprecated
public final class DefaultedHttpParams extends AbstractHttpParams {
    private final HttpParams defaults;
    private final HttpParams local;

    public DefaultedHttpParams(HttpParams httpParams, HttpParams httpParams2) {
        if (httpParams == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.local = httpParams;
        this.defaults = httpParams2;
    }

    @Override
    public HttpParams copy() {
        return new DefaultedHttpParams(this.local.copy(), this.defaults);
    }

    @Override
    public Object getParameter(String str) {
        Object parameter = this.local.getParameter(str);
        if (parameter == null && this.defaults != null) {
            return this.defaults.getParameter(str);
        }
        return parameter;
    }

    @Override
    public boolean removeParameter(String str) {
        return this.local.removeParameter(str);
    }

    @Override
    public HttpParams setParameter(String str, Object obj) {
        return this.local.setParameter(str, obj);
    }

    public HttpParams getDefaults() {
        return this.defaults;
    }
}
