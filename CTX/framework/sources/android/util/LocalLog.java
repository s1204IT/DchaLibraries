package android.util;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public final class LocalLog {
    private final Deque<String> mLog;
    private final int mMaxLines;

    public LocalLog(int i) {
        this.mMaxLines = Math.max(0, i);
        this.mLog = new ArrayDeque(this.mMaxLines);
    }

    public void log(String str) {
        if (this.mMaxLines <= 0) {
            return;
        }
        append(String.format("%s - %s", LocalDateTime.now(), str));
    }

    private synchronized void append(String str) {
        while (this.mLog.size() >= this.mMaxLines) {
            this.mLog.remove();
        }
        this.mLog.add(str);
    }

    public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Iterator<String> it = this.mLog.iterator();
        while (it.hasNext()) {
            printWriter.println(it.next());
        }
    }

    public synchronized void reverseDump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Iterator<String> itDescendingIterator = this.mLog.descendingIterator();
        while (itDescendingIterator.hasNext()) {
            printWriter.println(itDescendingIterator.next());
        }
    }

    public static class ReadOnlyLocalLog {
        private final LocalLog mLog;

        ReadOnlyLocalLog(LocalLog localLog) {
            this.mLog = localLog;
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            this.mLog.dump(fileDescriptor, printWriter, strArr);
        }

        public void reverseDump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            this.mLog.reverseDump(fileDescriptor, printWriter, strArr);
        }
    }

    public ReadOnlyLocalLog readOnlyLocalLog() {
        return new ReadOnlyLocalLog(this);
    }
}
