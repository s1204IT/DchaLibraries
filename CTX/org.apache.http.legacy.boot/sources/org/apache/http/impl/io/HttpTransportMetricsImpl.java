package org.apache.http.impl.io;

import org.apache.http.io.HttpTransportMetrics;

@Deprecated
public class HttpTransportMetricsImpl implements HttpTransportMetrics {
    private long bytesTransferred = 0;

    @Override
    public long getBytesTransferred() {
        return this.bytesTransferred;
    }

    public void setBytesTransferred(long j) {
        this.bytesTransferred = j;
    }

    public void incrementBytesTransferred(long j) {
        this.bytesTransferred += j;
    }

    @Override
    public void reset() {
        this.bytesTransferred = 0L;
    }
}
