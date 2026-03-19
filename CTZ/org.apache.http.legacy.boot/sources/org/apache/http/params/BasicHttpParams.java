package org.apache.http.params;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public final class BasicHttpParams extends AbstractHttpParams implements Serializable, Cloneable {
    private static final long serialVersionUID = -7086398485908701455L;
    private HashMap parameters;

    @Override
    public Object getParameter(String str) {
        if (this.parameters != null) {
            return this.parameters.get(str);
        }
        return null;
    }

    @Override
    public HttpParams setParameter(String str, Object obj) {
        if (this.parameters == null) {
            this.parameters = new HashMap();
        }
        this.parameters.put(str, obj);
        return this;
    }

    @Override
    public boolean removeParameter(String str) {
        if (this.parameters == null || !this.parameters.containsKey(str)) {
            return false;
        }
        this.parameters.remove(str);
        return true;
    }

    public void setParameters(String[] strArr, Object obj) {
        for (String str : strArr) {
            setParameter(str, obj);
        }
    }

    public boolean isParameterSet(String str) {
        return getParameter(str) != null;
    }

    public boolean isParameterSetLocally(String str) {
        return (this.parameters == null || this.parameters.get(str) == null) ? false : true;
    }

    public void clear() {
        this.parameters = null;
    }

    @Override
    public HttpParams copy() {
        BasicHttpParams basicHttpParams = new BasicHttpParams();
        copyParams(basicHttpParams);
        return basicHttpParams;
    }

    public Object clone() throws CloneNotSupportedException {
        BasicHttpParams basicHttpParams = (BasicHttpParams) super.clone();
        copyParams(basicHttpParams);
        return basicHttpParams;
    }

    protected void copyParams(HttpParams httpParams) {
        if (this.parameters == null) {
            return;
        }
        for (Map.Entry entry : this.parameters.entrySet()) {
            if (entry.getKey() instanceof String) {
                httpParams.setParameter((String) entry.getKey(), entry.getValue());
            }
        }
    }
}
