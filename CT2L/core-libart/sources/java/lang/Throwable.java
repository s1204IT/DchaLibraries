package java.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import libcore.util.EmptyArray;

public class Throwable implements Serializable {
    private static final long serialVersionUID = -3042686055658047285L;
    private Throwable cause;
    private String detailMessage;
    private volatile transient Object stackState;
    private StackTraceElement[] stackTrace;
    private List<Throwable> suppressedExceptions;

    private static native Object nativeFillInStackTrace();

    private static native StackTraceElement[] nativeGetStackTrace(Object obj);

    public Throwable() {
        this.cause = this;
        this.suppressedExceptions = Collections.emptyList();
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        fillInStackTrace();
    }

    public Throwable(String detailMessage) {
        this.cause = this;
        this.suppressedExceptions = Collections.emptyList();
        this.detailMessage = detailMessage;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        fillInStackTrace();
    }

    public Throwable(String detailMessage, Throwable cause) {
        this.cause = this;
        this.suppressedExceptions = Collections.emptyList();
        this.detailMessage = detailMessage;
        this.cause = cause;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        fillInStackTrace();
    }

    public Throwable(Throwable cause) {
        this.cause = this;
        this.suppressedExceptions = Collections.emptyList();
        this.detailMessage = cause == null ? null : cause.toString();
        this.cause = cause;
        this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        fillInStackTrace();
    }

    protected Throwable(String detailMessage, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        this.cause = this;
        this.suppressedExceptions = Collections.emptyList();
        this.detailMessage = detailMessage;
        this.cause = cause;
        if (!enableSuppression) {
            this.suppressedExceptions = null;
        }
        if (writableStackTrace) {
            this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
            fillInStackTrace();
        } else {
            this.stackTrace = null;
        }
    }

    public Throwable fillInStackTrace() {
        if (this.stackTrace != null) {
            this.stackState = nativeFillInStackTrace();
            this.stackTrace = EmptyArray.STACK_TRACE_ELEMENT;
        }
        return this;
    }

    public String getMessage() {
        return this.detailMessage;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public StackTraceElement[] getStackTrace() {
        return (StackTraceElement[]) getInternalStackTrace().clone();
    }

    public void setStackTrace(StackTraceElement[] trace) {
        if (this.stackTrace != null) {
            StackTraceElement[] newTrace = (StackTraceElement[]) trace.clone();
            for (int i = 0; i < newTrace.length; i++) {
                if (newTrace[i] == null) {
                    throw new NullPointerException("trace[" + i + "] == null");
                }
            }
            this.stackTrace = newTrace;
        }
    }

    public void printStackTrace() {
        printStackTrace(System.err);
    }

    private static int countDuplicates(StackTraceElement[] currentStack, StackTraceElement[] parentStack) {
        int duplicates = 0;
        int parentIndex = parentStack.length;
        int i = currentStack.length;
        while (true) {
            i--;
            if (i >= 0 && parentIndex - 1 >= 0) {
                StackTraceElement parentFrame = parentStack[parentIndex];
                if (!parentFrame.equals(currentStack[i])) {
                    break;
                }
                duplicates++;
            } else {
                break;
            }
        }
        return duplicates;
    }

    private StackTraceElement[] getInternalStackTrace() {
        if (this.stackTrace == EmptyArray.STACK_TRACE_ELEMENT) {
            this.stackTrace = nativeGetStackTrace(this.stackState);
            this.stackState = null;
            return this.stackTrace;
        }
        if (this.stackTrace == null) {
            return EmptyArray.STACK_TRACE_ELEMENT;
        }
        return this.stackTrace;
    }

    public void printStackTrace(PrintStream err) {
        try {
            printStackTrace(err, "", null);
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public void printStackTrace(PrintWriter err) {
        try {
            printStackTrace(err, "", null);
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    private void printStackTrace(Appendable err, String indent, StackTraceElement[] parentStack) throws IOException {
        err.append(toString());
        err.append("\n");
        StackTraceElement[] stack = getInternalStackTrace();
        if (stack != null) {
            int duplicates = parentStack != null ? countDuplicates(stack, parentStack) : 0;
            for (int i = 0; i < stack.length - duplicates; i++) {
                err.append(indent);
                err.append("\tat ");
                err.append(stack[i].toString());
                err.append("\n");
            }
            if (duplicates > 0) {
                err.append(indent);
                err.append("\t... ");
                err.append(Integer.toString(duplicates));
                err.append(" more\n");
            }
        }
        if (this.suppressedExceptions != null) {
            for (Throwable throwable : this.suppressedExceptions) {
                err.append(indent);
                err.append("\tSuppressed: ");
                throwable.printStackTrace(err, indent + "\t", stack);
            }
        }
        Throwable cause = getCause();
        if (cause != null) {
            err.append(indent);
            err.append("Caused by: ");
            cause.printStackTrace(err, indent, stack);
        }
    }

    public String toString() {
        String msg = getLocalizedMessage();
        String name = getClass().getName();
        return msg == null ? name : name + ": " + msg;
    }

    public Throwable initCause(Throwable throwable) {
        if (this.cause != this) {
            throw new IllegalStateException("Cause already initialized");
        }
        if (throwable == this) {
            throw new IllegalArgumentException("throwable == this");
        }
        this.cause = throwable;
        return this;
    }

    public Throwable getCause() {
        if (this.cause == this) {
            return null;
        }
        return this.cause;
    }

    public final void addSuppressed(Throwable throwable) {
        if (throwable == this) {
            throw new IllegalArgumentException("throwable == this");
        }
        if (throwable == null) {
            throw new NullPointerException("throwable == null");
        }
        if (this.suppressedExceptions != null) {
            if (this.suppressedExceptions.isEmpty()) {
                this.suppressedExceptions = new ArrayList(1);
            }
            this.suppressedExceptions.add(throwable);
        }
    }

    public final Throwable[] getSuppressed() {
        return (this.suppressedExceptions == null || this.suppressedExceptions.isEmpty()) ? EmptyArray.THROWABLE : (Throwable[]) this.suppressedExceptions.toArray(new Throwable[this.suppressedExceptions.size()]);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        getInternalStackTrace();
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.suppressedExceptions != null) {
            this.suppressedExceptions = new ArrayList(this.suppressedExceptions);
        }
    }
}
