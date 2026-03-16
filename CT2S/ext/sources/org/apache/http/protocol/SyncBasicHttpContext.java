package org.apache.http.protocol;

@Deprecated
public class SyncBasicHttpContext extends BasicHttpContext {
    public SyncBasicHttpContext(HttpContext parentContext) {
        super(parentContext);
    }

    @Override
    public synchronized Object getAttribute(String id) {
        return super.getAttribute(id);
    }

    @Override
    public synchronized void setAttribute(String id, Object obj) {
        super.setAttribute(id, obj);
    }

    @Override
    public synchronized Object removeAttribute(String id) {
        return super.removeAttribute(id);
    }
}
