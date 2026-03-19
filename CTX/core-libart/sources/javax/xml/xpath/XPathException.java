package javax.xml.xpath;

import java.io.PrintStream;
import java.io.PrintWriter;

public class XPathException extends Exception {
    private static final long serialVersionUID = -1837080260374986980L;
    private final Throwable cause;

    public XPathException(String str) {
        super(str);
        if (str == null) {
            throw new NullPointerException("message == null");
        }
        this.cause = null;
    }

    public XPathException(Throwable th) {
        super(th == null ? null : th.toString());
        this.cause = th;
        if (th == null) {
            throw new NullPointerException("cause == null");
        }
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }

    @Override
    public void printStackTrace(PrintStream printStream) {
        if (getCause() != null) {
            getCause().printStackTrace(printStream);
            printStream.println("--------------- linked to ------------------");
        }
        super.printStackTrace(printStream);
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintWriter printWriter) {
        if (getCause() != null) {
            getCause().printStackTrace(printWriter);
            printWriter.println("--------------- linked to ------------------");
        }
        super.printStackTrace(printWriter);
    }
}
