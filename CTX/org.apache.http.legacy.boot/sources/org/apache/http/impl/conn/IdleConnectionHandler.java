package org.apache.http.impl.conn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpConnection;

@Deprecated
public class IdleConnectionHandler {
    private final Log log = LogFactory.getLog(getClass());
    private final Map<HttpConnection, TimeValues> connectionToTimes = new HashMap();

    public void add(HttpConnection httpConnection, long j, TimeUnit timeUnit) {
        Long lValueOf = Long.valueOf(System.currentTimeMillis());
        if (this.log.isDebugEnabled()) {
            this.log.debug("Adding connection at: " + lValueOf);
        }
        this.connectionToTimes.put(httpConnection, new TimeValues(lValueOf.longValue(), j, timeUnit));
    }

    public boolean remove(HttpConnection httpConnection) {
        TimeValues timeValuesRemove = this.connectionToTimes.remove(httpConnection);
        if (timeValuesRemove != null) {
            return System.currentTimeMillis() <= timeValuesRemove.timeExpires;
        }
        this.log.warn("Removing a connection that never existed!");
        return true;
    }

    public void removeAll() {
        this.connectionToTimes.clear();
    }

    public void closeIdleConnections(long j) {
        long jCurrentTimeMillis = System.currentTimeMillis() - j;
        if (this.log.isDebugEnabled()) {
            this.log.debug("Checking for connections, idleTimeout: " + jCurrentTimeMillis);
        }
        Iterator<HttpConnection> it = this.connectionToTimes.keySet().iterator();
        while (it.hasNext()) {
            HttpConnection next = it.next();
            Long lValueOf = Long.valueOf(this.connectionToTimes.get(next).timeAdded);
            if (lValueOf.longValue() <= jCurrentTimeMillis) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Closing connection, connection time: " + lValueOf);
                }
                it.remove();
                try {
                    next.close();
                } catch (IOException e) {
                    this.log.debug("I/O error closing connection", e);
                }
            }
        }
    }

    public void closeExpiredConnections() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.log.isDebugEnabled()) {
            this.log.debug("Checking for expired connections, now: " + jCurrentTimeMillis);
        }
        Iterator<HttpConnection> it = this.connectionToTimes.keySet().iterator();
        while (it.hasNext()) {
            HttpConnection next = it.next();
            TimeValues timeValues = this.connectionToTimes.get(next);
            if (timeValues.timeExpires <= jCurrentTimeMillis) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Closing connection, expired @: " + timeValues.timeExpires);
                }
                it.remove();
                try {
                    next.close();
                } catch (IOException e) {
                    this.log.debug("I/O error closing connection", e);
                }
            }
        }
    }

    private static class TimeValues {
        private final long timeAdded;
        private final long timeExpires;

        TimeValues(long j, long j2, TimeUnit timeUnit) {
            this.timeAdded = j;
            if (j2 > 0) {
                this.timeExpires = j + timeUnit.toMillis(j2);
            } else {
                this.timeExpires = Long.MAX_VALUE;
            }
        }
    }
}
