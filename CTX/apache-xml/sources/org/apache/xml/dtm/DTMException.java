package org.apache.xml.dtm;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.xml.transform.SourceLocator;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;

public class DTMException extends RuntimeException {
    static final long serialVersionUID = -775576419181334734L;
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
        if (this.containedException == null && th != null) {
            throw new IllegalStateException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CANNOT_OVERWRITE_CAUSE, null));
        }
        if (th == this) {
            throw new IllegalArgumentException(XMLMessages.createXMLMessage(XMLErrorResources.ER_SELF_CAUSATION_NOT_PERMITTED, null));
        }
        this.containedException = th;
        return this;
    }

    public DTMException(String str) {
        super(str);
        this.containedException = null;
        this.locator = null;
    }

    public DTMException(Throwable th) {
        super(th.getMessage());
        this.containedException = th;
        this.locator = null;
    }

    public DTMException(String str, Throwable th) {
        super((str == null || str.length() == 0) ? th.getMessage() : str);
        this.containedException = th;
        this.locator = null;
    }

    public DTMException(String str, SourceLocator sourceLocator) {
        super(str);
        this.containedException = null;
        this.locator = sourceLocator;
    }

    public DTMException(String str, SourceLocator sourceLocator, Throwable th) {
        super(str);
        this.containedException = th;
        this.locator = sourceLocator;
    }

    public String getMessageAndLocation() {
        StringBuffer stringBuffer = new StringBuffer();
        String message = super.getMessage();
        if (message != null) {
            stringBuffer.append(message);
        }
        if (this.locator != null) {
            String systemId = this.locator.getSystemId();
            int lineNumber = this.locator.getLineNumber();
            int columnNumber = this.locator.getColumnNumber();
            if (systemId != null) {
                stringBuffer.append("; SystemID: ");
                stringBuffer.append(systemId);
            }
            if (lineNumber != 0) {
                stringBuffer.append("; Line#: ");
                stringBuffer.append(lineNumber);
            }
            if (columnNumber != 0) {
                stringBuffer.append("; Column#: ");
                stringBuffer.append(columnNumber);
            }
        }
        return stringBuffer.toString();
    }

    public String getLocationAsString() {
        if (this.locator != null) {
            StringBuffer stringBuffer = new StringBuffer();
            String systemId = this.locator.getSystemId();
            int lineNumber = this.locator.getLineNumber();
            int columnNumber = this.locator.getColumnNumber();
            if (systemId != null) {
                stringBuffer.append("; SystemID: ");
                stringBuffer.append(systemId);
            }
            if (lineNumber != 0) {
                stringBuffer.append("; Line#: ");
                stringBuffer.append(lineNumber);
            }
            if (columnNumber != 0) {
                stringBuffer.append("; Column#: ");
                stringBuffer.append(columnNumber);
            }
            return stringBuffer.toString();
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
        boolean z;
        Throwable th;
        String locationAsString;
        if (printWriter == null) {
            printWriter = new PrintWriter((OutputStream) System.err, true);
        }
        try {
            String locationAsString2 = getLocationAsString();
            if (locationAsString2 != null) {
                printWriter.println(locationAsString2);
            }
            super.printStackTrace(printWriter);
        } catch (Throwable th2) {
        }
        try {
            Throwable.class.getMethod("getCause", (Class) null);
            z = true;
        } catch (NoSuchMethodException e) {
            z = false;
        }
        if (!z) {
            Throwable exception = getException();
            for (int i = 0; i < 10 && exception != null; i++) {
                printWriter.println("---------");
                try {
                    if ((exception instanceof DTMException) && (locationAsString = ((DTMException) exception).getLocationAsString()) != null) {
                        printWriter.println(locationAsString);
                    }
                    exception.printStackTrace(printWriter);
                } catch (Throwable th3) {
                    printWriter.println("Could not print stack trace...");
                }
                try {
                    Method method = exception.getClass().getMethod("getException", (Class) null);
                    if (method != null) {
                        th = (Throwable) method.invoke(exception, (Class) null);
                        if (exception == th) {
                            return;
                        }
                    } else {
                        th = null;
                    }
                    exception = th;
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e2) {
                    exception = null;
                }
            }
        }
    }
}
