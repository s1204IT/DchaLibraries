package org.apache.http.impl.client;

import gov.nist.core.Separators;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.params.AbstractHttpParams;
import org.apache.http.params.HttpParams;

@Deprecated
public class ClientParamsStack extends AbstractHttpParams {
    protected final HttpParams applicationParams;
    protected final HttpParams clientParams;
    private final Log log;
    protected final HttpParams overrideParams;
    protected final HttpParams requestParams;

    public ClientParamsStack(HttpParams aparams, HttpParams cparams, HttpParams rparams, HttpParams oparams) {
        this.log = LogFactory.getLog(getClass());
        this.applicationParams = aparams;
        this.clientParams = cparams;
        this.requestParams = rparams;
        this.overrideParams = oparams;
    }

    public ClientParamsStack(ClientParamsStack stack) {
        this(stack.getApplicationParams(), stack.getClientParams(), stack.getRequestParams(), stack.getOverrideParams());
    }

    public ClientParamsStack(ClientParamsStack stack, HttpParams aparams, HttpParams cparams, HttpParams rparams, HttpParams oparams) {
        this(aparams == null ? stack.getApplicationParams() : aparams, cparams == null ? stack.getClientParams() : cparams, rparams == null ? stack.getRequestParams() : rparams, oparams == null ? stack.getOverrideParams() : oparams);
    }

    public final HttpParams getApplicationParams() {
        return this.applicationParams;
    }

    public final HttpParams getClientParams() {
        return this.clientParams;
    }

    public final HttpParams getRequestParams() {
        return this.requestParams;
    }

    public final HttpParams getOverrideParams() {
        return this.overrideParams;
    }

    @Override
    public Object getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name must not be null.");
        }
        Object result = null;
        if (this.overrideParams != null) {
            result = this.overrideParams.getParameter(name);
        }
        if (result == null && this.requestParams != null) {
            result = this.requestParams.getParameter(name);
        }
        if (result == null && this.clientParams != null) {
            result = this.clientParams.getParameter(name);
        }
        if (result == null && this.applicationParams != null) {
            result = this.applicationParams.getParameter(name);
        }
        if (this.log.isDebugEnabled()) {
            this.log.debug(Separators.QUOTE + name + "': " + result);
        }
        return result;
    }

    @Override
    public HttpParams setParameter(String name, Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Setting parameters in a stack is not supported.");
    }

    @Override
    public boolean removeParameter(String name) {
        throw new UnsupportedOperationException("Removing parameters in a stack is not supported.");
    }

    @Override
    public HttpParams copy() {
        return this;
    }
}
