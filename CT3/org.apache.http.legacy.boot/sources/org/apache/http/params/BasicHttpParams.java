package org.apache.http.params;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public final class BasicHttpParams extends AbstractHttpParams implements Serializable, Cloneable {
    private static final long serialVersionUID = -7086398485908701455L;
    private HashMap parameters;

    @Override
    public Object getParameter(String name) {
        if (this.parameters == null) {
            return null;
        }
        Object param = this.parameters.get(name);
        return param;
    }

    @Override
    public HttpParams setParameter(String name, Object value) {
        if (this.parameters == null) {
            this.parameters = new HashMap();
        }
        this.parameters.put(name, value);
        return this;
    }

    @Override
    public boolean removeParameter(String name) {
        if (this.parameters == null || !this.parameters.containsKey(name)) {
            return false;
        }
        this.parameters.remove(name);
        return true;
    }

    public void setParameters(String[] names, Object value) {
        for (String str : names) {
            setParameter(str, value);
        }
    }

    public boolean isParameterSet(String name) {
        return getParameter(name) != null;
    }

    public boolean isParameterSetLocally(String name) {
        return (this.parameters == null || this.parameters.get(name) == null) ? false : true;
    }

    public void clear() {
        this.parameters = null;
    }

    @Override
    public HttpParams copy() {
        BasicHttpParams clone = new BasicHttpParams();
        copyParams(clone);
        return clone;
    }

    public Object clone() throws CloneNotSupportedException {
        BasicHttpParams clone = (BasicHttpParams) super.clone();
        copyParams(clone);
        return clone;
    }

    protected void copyParams(HttpParams target) {
        if (this.parameters == null) {
            return;
        }
        for (Map.Entry me : this.parameters.entrySet()) {
            if (me.getKey() instanceof String) {
                target.setParameter((String) me.getKey(), me.getValue());
            }
        }
    }
}
