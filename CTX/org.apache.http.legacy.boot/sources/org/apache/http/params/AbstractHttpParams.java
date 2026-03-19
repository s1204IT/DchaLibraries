package org.apache.http.params;

@Deprecated
public abstract class AbstractHttpParams implements HttpParams {
    protected AbstractHttpParams() {
    }

    @Override
    public long getLongParameter(String str, long j) {
        Object parameter = getParameter(str);
        if (parameter == null) {
            return j;
        }
        return ((Long) parameter).longValue();
    }

    @Override
    public HttpParams setLongParameter(String str, long j) {
        setParameter(str, new Long(j));
        return this;
    }

    @Override
    public int getIntParameter(String str, int i) {
        Object parameter = getParameter(str);
        if (parameter == null) {
            return i;
        }
        return ((Integer) parameter).intValue();
    }

    @Override
    public HttpParams setIntParameter(String str, int i) {
        setParameter(str, new Integer(i));
        return this;
    }

    @Override
    public double getDoubleParameter(String str, double d) {
        Object parameter = getParameter(str);
        if (parameter == null) {
            return d;
        }
        return ((Double) parameter).doubleValue();
    }

    @Override
    public HttpParams setDoubleParameter(String str, double d) {
        setParameter(str, new Double(d));
        return this;
    }

    @Override
    public boolean getBooleanParameter(String str, boolean z) {
        Object parameter = getParameter(str);
        if (parameter == null) {
            return z;
        }
        return ((Boolean) parameter).booleanValue();
    }

    @Override
    public HttpParams setBooleanParameter(String str, boolean z) {
        setParameter(str, z ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    @Override
    public boolean isParameterTrue(String str) {
        return getBooleanParameter(str, false);
    }

    @Override
    public boolean isParameterFalse(String str) {
        return !getBooleanParameter(str, false);
    }
}
