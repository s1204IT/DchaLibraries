package android.os;

import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TransactionTracker {
    private Map<String, Long> mTraces;

    private void resetTraces() {
        synchronized (this) {
            this.mTraces = new HashMap();
        }
    }

    TransactionTracker() {
        resetTraces();
    }

    public void addTrace() {
        String trace = Log.getStackTraceString(new Throwable());
        synchronized (this) {
            if (this.mTraces.containsKey(trace)) {
                this.mTraces.put(trace, Long.valueOf(this.mTraces.get(trace).longValue() + 1));
            } else {
                this.mTraces.put(trace, 1L);
            }
        }
    }

    public void writeTracesToFile(ParcelFileDescriptor fd) {
        if (this.mTraces.isEmpty()) {
            return;
        }
        FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fd.getFileDescriptor()));
        synchronized (this) {
            for (String trace : this.mTraces.keySet()) {
                fastPrintWriter.println("Count: " + this.mTraces.get(trace));
                fastPrintWriter.println("Trace: " + trace);
                fastPrintWriter.println();
            }
        }
        fastPrintWriter.flush();
    }

    public void clearTraces() {
        resetTraces();
    }
}
