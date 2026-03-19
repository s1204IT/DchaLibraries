package javax.xml.transform;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class TransformerException extends Exception {
    private static final long serialVersionUID = 975798773772956428L;
    Throwable containedException;
    SourceLocator locator;

    public SourceLocator getLocator() {
        return this.locator;
    }

    public void setLocator(SourceLocator sourceLocator) {
        this.locator = sourceLocator;
    }

    public Throwable getException() {
        return this.containedException;
    }

    @Override
    public Throwable getCause() {
        if (this.containedException == this) {
            return null;
        }
        return this.containedException;
    }

    @Override
    public synchronized Throwable initCause(Throwable th) {
        if (this.containedException != null) {
            throw new IllegalStateException("Can't overwrite cause");
        }
        if (th == this) {
            throw new IllegalArgumentException("Self-causation not permitted");
        }
        this.containedException = th;
        return this;
    }

    public TransformerException(String str) {
        super(str);
        this.containedException = null;
        this.locator = null;
    }

    public TransformerException(Throwable th) {
        super(th.toString());
        this.containedException = th;
        this.locator = null;
    }

    public TransformerException(String str, Throwable th) {
        super((str == null || str.length() == 0) ? th.toString() : str);
        this.containedException = th;
        this.locator = null;
    }

    public TransformerException(String str, SourceLocator sourceLocator) {
        super(str);
        this.containedException = null;
        this.locator = sourceLocator;
    }

    public TransformerException(String str, SourceLocator sourceLocator, Throwable th) {
        super(str);
        this.containedException = th;
        this.locator = sourceLocator;
    }

    public String getMessageAndLocation() {
        StringBuilder sb = new StringBuilder();
        String message = super.getMessage();
        if (message != null) {
            sb.append(message);
        }
        if (this.locator != null) {
            String systemId = this.locator.getSystemId();
            int lineNumber = this.locator.getLineNumber();
            int columnNumber = this.locator.getColumnNumber();
            if (systemId != null) {
                sb.append("; SystemID: ");
                sb.append(systemId);
            }
            if (lineNumber != 0) {
                sb.append("; Line#: ");
                sb.append(lineNumber);
            }
            if (columnNumber != 0) {
                sb.append("; Column#: ");
                sb.append(columnNumber);
            }
        }
        return sb.toString();
    }

    public String getLocationAsString() {
        if (this.locator != null) {
            StringBuilder sb = new StringBuilder();
            String systemId = this.locator.getSystemId();
            int lineNumber = this.locator.getLineNumber();
            int columnNumber = this.locator.getColumnNumber();
            if (systemId != null) {
                sb.append("; SystemID: ");
                sb.append(systemId);
            }
            if (lineNumber != 0) {
                sb.append("; Line#: ");
                sb.append(lineNumber);
            }
            if (columnNumber != 0) {
                sb.append("; Column#: ");
                sb.append(columnNumber);
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(new PrintWriter((OutputStream) System.err, true));
    }

    @Override
    public void printStackTrace(PrintStream printStream) {
        printStackTrace(new PrintWriter(printStream));
    }

    @Override
    public void printStackTrace(PrintWriter printWriter) {
        if (printWriter == null) {
            printWriter = new PrintWriter((OutputStream) System.err, true);
        }
        try {
            String locationAsString = getLocationAsString();
            if (locationAsString != null) {
                printWriter.println(locationAsString);
            }
            super.printStackTrace(printWriter);
        } catch (Throwable th) {
        }
    }
}
