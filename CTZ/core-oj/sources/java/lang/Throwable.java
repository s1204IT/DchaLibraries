package java.lang;

import dalvik.annotation.optimization.FastNative;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import libcore.util.EmptyArray;

public class Throwable implements Serializable {
    private static final String CAUSE_CAPTION = "Caused by: ";
    private static Throwable[] EMPTY_THROWABLE_ARRAY = null;
    private static final String NULL_CAUSE_MESSAGE = "Cannot suppress a null exception.";
    private static final String SELF_SUPPRESSION_MESSAGE = "Self-suppression not permitted";
    private static final String SUPPRESSED_CAPTION = "Suppressed: ";
    private static final long serialVersionUID = -3042686055658047285L;
    private volatile transient Object backtrace;
    private Throwable cause;
    private String detailMessage;
    private StackTraceElement[] stackTrace;
    private List<Throwable> suppressedExceptions;

    @FastNative
    private static native Object nativeFillInStackTrace();

    @FastNative
    private static native StackTraceElement[] nativeGetStackTrace(Object obj);

    private static class SentinelHolder {
        public static final StackTraceElement STACK_TRACE_ELEMENT_SENTINEL = new StackTraceElement("", "", null, Integer.MIN_VALUE);
        public static final StackTraceElement[] STACK_TRACE_SENTINEL = {STACK_TRACE_ELEMENT_SENTINEL};

        private SentinelHolder() {
        }
    }

    public Throwable() {
        this.cause = this;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        this.suppressedExceptions = Collections.emptyList();
        fillInStackTrace();
    }

    public Throwable(String str) {
        this.cause = this;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        this.suppressedExceptions = Collections.emptyList();
        fillInStackTrace();
        this.detailMessage = str;
    }

    public Throwable(String str, Throwable th) {
        this.cause = this;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        this.suppressedExceptions = Collections.emptyList();
        fillInStackTrace();
        this.detailMessage = str;
        this.cause = th;
    }

    public Throwable(Throwable th) {
        this.cause = this;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        this.suppressedExceptions = Collections.emptyList();
        fillInStackTrace();
        this.detailMessage = th == null ? null : th.toString();
        this.cause = th;
    }

    protected Throwable(String str, Throwable th, boolean z, boolean z2) {
        this.cause = this;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        this.suppressedExceptions = Collections.emptyList();
        if (z2) {
            fillInStackTrace();
        } else {
            this.stackTrace = null;
        }
        this.detailMessage = str;
        this.cause = th;
        if (!z) {
            this.suppressedExceptions = null;
        }
    }

    public String getMessage() {
        return this.detailMessage;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public synchronized Throwable getCause() {
        return this.cause == this ? null : this.cause;
    }

    public synchronized Throwable initCause(Throwable th) {
        if (this.cause != this) {
            throw new IllegalStateException("Can't overwrite cause with " + Objects.toString(th, "a null"), this);
        }
        if (th == this) {
            throw new IllegalArgumentException("Self-causation not permitted", this);
        }
        this.cause = th;
        return this;
    }

    public String toString() {
        String name = getClass().getName();
        String localizedMessage = getLocalizedMessage();
        if (localizedMessage == null) {
            return name;
        }
        return name + ": " + localizedMessage;
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream printStream) {
        printStackTrace(new WrappedPrintStream(printStream));
    }

    private void printStackTrace(PrintStreamOrWriter printStreamOrWriter) {
        Set<Throwable> setNewSetFromMap = Collections.newSetFromMap(new IdentityHashMap());
        setNewSetFromMap.add(this);
        synchronized (printStreamOrWriter.lock()) {
            printStreamOrWriter.println(this);
            StackTraceElement[] ourStackTrace = getOurStackTrace();
            for (StackTraceElement stackTraceElement : ourStackTrace) {
                printStreamOrWriter.println("\tat " + ((Object) stackTraceElement));
            }
            for (Throwable th : getSuppressed()) {
                th.printEnclosedStackTrace(printStreamOrWriter, ourStackTrace, SUPPRESSED_CAPTION, "\t", setNewSetFromMap);
            }
            Throwable cause = getCause();
            if (cause != null) {
                cause.printEnclosedStackTrace(printStreamOrWriter, ourStackTrace, CAUSE_CAPTION, "", setNewSetFromMap);
            }
        }
    }

    private void printEnclosedStackTrace(PrintStreamOrWriter printStreamOrWriter, StackTraceElement[] stackTraceElementArr, String str, String str2, Set<Throwable> set) {
        if (set.contains(this)) {
            printStreamOrWriter.println("\t[CIRCULAR REFERENCE:" + ((Object) this) + "]");
            return;
        }
        set.add(this);
        StackTraceElement[] ourStackTrace = getOurStackTrace();
        int length = ourStackTrace.length - 1;
        for (int length2 = stackTraceElementArr.length - 1; length >= 0 && length2 >= 0 && ourStackTrace[length].equals(stackTraceElementArr[length2]); length2--) {
            length--;
        }
        int length3 = (ourStackTrace.length - 1) - length;
        printStreamOrWriter.println(str2 + str + ((Object) this));
        for (int i = 0; i <= length; i++) {
            printStreamOrWriter.println(str2 + "\tat " + ((Object) ourStackTrace[i]));
        }
        if (length3 != 0) {
            printStreamOrWriter.println(str2 + "\t... " + length3 + " more");
        }
        for (Throwable th : getSuppressed()) {
            th.printEnclosedStackTrace(printStreamOrWriter, ourStackTrace, SUPPRESSED_CAPTION, str2 + "\t", set);
        }
        Throwable cause = getCause();
        if (cause != null) {
            cause.printEnclosedStackTrace(printStreamOrWriter, ourStackTrace, CAUSE_CAPTION, str2, set);
        }
    }

    public void printStackTrace(PrintWriter printWriter) {
        printStackTrace(new WrappedPrintWriter(printWriter));
    }

    private static abstract class PrintStreamOrWriter {
        abstract Object lock();

        abstract void println(Object obj);

        private PrintStreamOrWriter() {
        }
    }

    private static class WrappedPrintStream extends PrintStreamOrWriter {
        private final PrintStream printStream;

        WrappedPrintStream(PrintStream printStream) {
            super();
            this.printStream = printStream;
        }

        @Override
        Object lock() {
            return this.printStream;
        }

        @Override
        void println(Object obj) {
            this.printStream.println(obj);
        }
    }

    private static class WrappedPrintWriter extends PrintStreamOrWriter {
        private final PrintWriter printWriter;

        WrappedPrintWriter(PrintWriter printWriter) {
            super();
            this.printWriter = printWriter;
        }

        @Override
        Object lock() {
            return this.printWriter;
        }

        @Override
        void println(Object obj) {
            this.printWriter.println(obj);
        }
    }

    public synchronized Throwable fillInStackTrace() {
        if (this.stackTrace != null || this.backtrace != null) {
            this.backtrace = nativeFillInStackTrace();
            this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        }
        return this;
    }

    public StackTraceElement[] getStackTrace() {
        return (StackTraceElement[]) getOurStackTrace().clone();
    }

    private synchronized StackTraceElement[] getOurStackTrace() {
        if (this.stackTrace == EmptyArray.STACK_TRACE_ELEMENT || (this.stackTrace == null && this.backtrace != null)) {
            this.stackTrace = nativeGetStackTrace(this.backtrace);
            this.backtrace = null;
        }
        if (this.stackTrace == null) {
            return EmptyArray.STACK_TRACE_ELEMENT;
        }
        return this.stackTrace;
    }

    public void setStackTrace(StackTraceElement[] stackTraceElementArr) {
        StackTraceElement[] stackTraceElementArr2 = (StackTraceElement[]) stackTraceElementArr.clone();
        for (int i = 0; i < stackTraceElementArr2.length; i++) {
            if (stackTraceElementArr2[i] == null) {
                throw new NullPointerException("stackTrace[" + i + "]");
            }
        }
        synchronized (this) {
            if (this.stackTrace == null && this.backtrace == null) {
                return;
            }
            this.stackTrace = stackTraceElementArr2;
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        List<Throwable> arrayList;
        objectInputStream.defaultReadObject();
        if (this.suppressedExceptions != null) {
            if (this.suppressedExceptions.isEmpty()) {
                arrayList = Collections.emptyList();
            } else {
                arrayList = new ArrayList<>(1);
                for (Throwable th : this.suppressedExceptions) {
                    if (th == null) {
                        throw new NullPointerException(NULL_CAUSE_MESSAGE);
                    }
                    if (th == this) {
                        throw new IllegalArgumentException(SELF_SUPPRESSION_MESSAGE);
                    }
                    arrayList.add(th);
                }
            }
            this.suppressedExceptions = arrayList;
        }
        if (this.stackTrace != null) {
            if (this.stackTrace.length != 0) {
                if (this.stackTrace.length == 1 && SentinelHolder.STACK_TRACE_ELEMENT_SENTINEL.equals(this.stackTrace[0])) {
                    this.stackTrace = null;
                    return;
                }
                for (StackTraceElement stackTraceElement : this.stackTrace) {
                    if (stackTraceElement == null) {
                        throw new NullPointerException("null StackTraceElement in serial stream. ");
                    }
                }
                return;
            }
            return;
        }
        this.stackTrace = new StackTraceElement[0];
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        getOurStackTrace();
        StackTraceElement[] stackTraceElementArr = this.stackTrace;
        try {
            if (this.stackTrace == null) {
                this.stackTrace = SentinelHolder.STACK_TRACE_SENTINEL;
            }
            objectOutputStream.defaultWriteObject();
        } finally {
            this.stackTrace = stackTraceElementArr;
        }
    }

    public final synchronized void addSuppressed(Throwable th) {
        if (th == this) {
            throw new IllegalArgumentException(SELF_SUPPRESSION_MESSAGE, th);
        }
        if (th == null) {
            throw new NullPointerException(NULL_CAUSE_MESSAGE);
        }
        if (this.suppressedExceptions == null) {
            return;
        }
        if (this.suppressedExceptions.isEmpty()) {
            this.suppressedExceptions = new ArrayList(1);
        }
        this.suppressedExceptions.add(th);
    }

    public final synchronized Throwable[] getSuppressed() {
        if (EMPTY_THROWABLE_ARRAY == null) {
            EMPTY_THROWABLE_ARRAY = new Throwable[0];
        }
        if (this.suppressedExceptions != null && !this.suppressedExceptions.isEmpty()) {
            return (Throwable[]) this.suppressedExceptions.toArray(EMPTY_THROWABLE_ARRAY);
        }
        return EMPTY_THROWABLE_ARRAY;
    }
}
