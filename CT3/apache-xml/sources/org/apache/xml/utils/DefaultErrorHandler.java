package org.apache.xml.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DefaultErrorHandler implements ErrorHandler, ErrorListener {
    PrintWriter m_pw;
    boolean m_throwExceptionOnError;

    public DefaultErrorHandler(PrintWriter pw) {
        this.m_throwExceptionOnError = true;
        this.m_pw = pw;
    }

    public DefaultErrorHandler(PrintStream pw) {
        this.m_throwExceptionOnError = true;
        this.m_pw = new PrintWriter((OutputStream) pw, true);
    }

    public DefaultErrorHandler() {
        this(true);
    }

    public DefaultErrorHandler(boolean throwExceptionOnError) {
        this.m_throwExceptionOnError = true;
        this.m_throwExceptionOnError = throwExceptionOnError;
    }

    public PrintWriter getErrorWriter() {
        if (this.m_pw == null) {
            this.m_pw = new PrintWriter((OutputStream) System.err, true);
        }
        return this.m_pw;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        PrintWriter pw = getErrorWriter();
        printLocation(pw, exception);
        pw.println("Parser warning: " + exception.getMessage());
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }

    @Override
    public void warning(TransformerException exception) throws TransformerException {
        PrintWriter pw = getErrorWriter();
        printLocation(pw, exception);
        pw.println(exception.getMessage());
    }

    @Override
    public void error(TransformerException exception) throws TransformerException {
        if (this.m_throwExceptionOnError) {
            throw exception;
        }
        PrintWriter pw = getErrorWriter();
        printLocation(pw, exception);
        pw.println(exception.getMessage());
    }

    @Override
    public void fatalError(TransformerException exception) throws TransformerException {
        if (this.m_throwExceptionOnError) {
            throw exception;
        }
        PrintWriter pw = getErrorWriter();
        printLocation(pw, exception);
        pw.println(exception.getMessage());
    }

    public static void ensureLocationSet(TransformerException exception) {
        SourceLocator causeLocator;
        SourceLocator locator = null;
        ?? exception2 = exception;
        do {
            if (exception2 instanceof SAXParseException) {
                locator = new SAXSourceLocator((SAXParseException) exception2);
            } else if ((exception2 instanceof TransformerException) && (causeLocator = exception2.getLocator()) != null) {
                locator = causeLocator;
            }
            if (exception2 instanceof TransformerException) {
                exception2 = exception2.getCause();
            } else if (exception2 instanceof SAXException) {
                exception2 = ((SAXException) exception2).getException();
            } else {
                exception2 = 0;
            }
        } while (exception2 != 0);
        exception.setLocator(locator);
    }

    public static void printLocation(PrintStream pw, TransformerException exception) {
        printLocation(new PrintWriter(pw), exception);
    }

    public static void printLocation(PrintStream pw, SAXParseException exception) {
        printLocation(new PrintWriter(pw), exception);
    }

    public static void printLocation(PrintWriter pw, Throwable exception) {
        SourceLocator causeLocator;
        SourceLocator locator = null;
        ?? cause = exception;
        do {
            if (cause instanceof SAXParseException) {
                locator = new SAXSourceLocator((SAXParseException) cause);
            } else if ((cause instanceof TransformerException) && (causeLocator = cause.getLocator()) != null) {
                locator = causeLocator;
            }
            cause = cause instanceof TransformerException ? ((TransformerException) cause).getCause() : cause instanceof WrappedRuntimeException ? cause.getException() : cause instanceof SAXException ? cause.getException() : 0;
        } while (cause != 0);
        if (locator == null) {
            pw.print("(" + XMLMessages.createXMLMessage(XMLErrorResources.ER_LOCATION_UNKNOWN, null) + ")");
        } else {
            String id = locator.getPublicId() != null ? locator.getPublicId() : locator.getSystemId() != null ? locator.getSystemId() : XMLMessages.createXMLMessage(XMLErrorResources.ER_SYSTEMID_UNKNOWN, null);
            pw.print(id + "; " + XMLMessages.createXMLMessage("line", null) + locator.getLineNumber() + "; " + XMLMessages.createXMLMessage("column", null) + locator.getColumnNumber() + "; ");
        }
    }
}
