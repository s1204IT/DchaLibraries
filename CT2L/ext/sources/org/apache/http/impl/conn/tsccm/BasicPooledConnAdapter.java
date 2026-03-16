package org.apache.http.impl.conn.tsccm;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.AbstractPoolEntry;
import org.apache.http.impl.conn.AbstractPooledConnAdapter;

@Deprecated
public class BasicPooledConnAdapter extends AbstractPooledConnAdapter {
    protected BasicPooledConnAdapter(ThreadSafeClientConnManager tsccm, AbstractPoolEntry entry) {
        super(tsccm, entry);
        markReusable();
    }

    @Override
    protected ClientConnectionManager getManager() {
        return super.getManager();
    }

    protected AbstractPoolEntry getPoolEntry() {
        return this.poolEntry;
    }

    @Override
    protected void detach() {
        super.detach();
    }
}
