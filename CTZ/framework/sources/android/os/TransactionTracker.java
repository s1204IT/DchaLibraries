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

    public void addTrace(Throwable th) {
        String stackTraceString = Log.getStackTraceString(th);
        synchronized (this) {
            if (this.mTraces.containsKey(stackTraceString)) {
                this.mTraces.put(stackTraceString, Long.valueOf(this.mTraces.get(stackTraceString).longValue() + 1));
            } else {
                this.mTraces.put(stackTraceString, 1L);
            }
        }
    }

    public void writeTracesToFile(ParcelFileDescriptor parcelFileDescriptor) {
        if (this.mTraces.isEmpty()) {
            return;
        }
        FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
        synchronized (this) {
            for (String str : this.mTraces.keySet()) {
                fastPrintWriter.println("Count: " + this.mTraces.get(str));
                fastPrintWriter.println("Trace: " + str);
                fastPrintWriter.println();
            }
        }
        fastPrintWriter.flush();
    }

    public void clearTraces() {
        resetTraces();
    }
}
