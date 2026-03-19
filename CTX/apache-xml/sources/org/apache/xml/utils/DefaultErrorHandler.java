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

    public DefaultErrorHandler(PrintWriter printWriter) {
        this.m_throwExceptionOnError = true;
        this.m_pw = printWriter;
    }

    public DefaultErrorHandler(PrintStream printStream) {
        this.m_throwExceptionOnError = true;
        this.m_pw = new PrintWriter((OutputStream) printStream, true);
    }

    public DefaultErrorHandler() {
        this(true);
    }

    public DefaultErrorHandler(boolean z) {
        this.m_throwExceptionOnError = true;
        this.m_throwExceptionOnError = z;
    }

    public PrintWriter getErrorWriter() {
        if (this.m_pw == null) {
            this.m_pw = new PrintWriter((OutputStream) System.err, true);
        }
        return this.m_pw;
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        PrintWriter errorWriter = getErrorWriter();
        printLocation(errorWriter, sAXParseException);
        errorWriter.println("Parser warning: " + sAXParseException.getMessage());
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        throw sAXParseException;
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        throw sAXParseException;
    }

    @Override
    public void warning(TransformerException transformerException) throws TransformerException {
        PrintWriter errorWriter = getErrorWriter();
        printLocation(errorWriter, transformerException);
        errorWriter.println(transformerException.getMessage());
    }

    @Override
    public void error(TransformerException transformerException) throws TransformerException {
        if (this.m_throwExceptionOnError) {
            throw transformerException;
        }
        PrintWriter errorWriter = getErrorWriter();
        printLocation(errorWriter, transformerException);
        errorWriter.println(transformerException.getMessage());
    }

    @Override
    public void fatalError(TransformerException transformerException) throws TransformerException {
        if (this.m_throwExceptionOnError) {
            throw transformerException;
        }
        PrintWriter errorWriter = getErrorWriter();
        printLocation(errorWriter, transformerException);
        errorWriter.println(transformerException.getMessage());
    }

    public static void ensureLocationSet(TransformerException transformerException) {
        SourceLocator locator;
        Throwable exception = transformerException;
        SourceLocator sAXSourceLocator = null;
        do {
            if (exception instanceof SAXParseException) {
                sAXSourceLocator = new SAXSourceLocator((SAXParseException) exception);
            } else if ((exception instanceof TransformerException) && (locator = ((TransformerException) exception).getLocator()) != null) {
                sAXSourceLocator = locator;
            }
            if (exception instanceof TransformerException) {
                exception = ((TransformerException) exception).getCause();
            } else if (exception instanceof SAXException) {
                exception = ((SAXException) exception).getException();
            } else {
                exception = null;
            }
        } while (exception != null);
        transformerException.setLocator(sAXSourceLocator);
    }

    public static void printLocation(PrintStream printStream, TransformerException transformerException) {
        printLocation(new PrintWriter(printStream), transformerException);
    }

    public static void printLocation(PrintStream printStream, SAXParseException sAXParseException) {
        printLocation(new PrintWriter(printStream), sAXParseException);
    }

    public static void printLocation(PrintWriter printWriter, Throwable th) {
        SourceLocator locator;
        String strCreateXMLMessage;
        SourceLocator sAXSourceLocator = null;
        do {
            if (th instanceof SAXParseException) {
                sAXSourceLocator = new SAXSourceLocator((SAXParseException) th);
            } else if ((th instanceof TransformerException) && (locator = ((TransformerException) th).getLocator()) != null) {
                sAXSourceLocator = locator;
            }
            if (th instanceof TransformerException) {
                th = ((TransformerException) th).getCause();
            } else if (th instanceof WrappedRuntimeException) {
                th = ((WrappedRuntimeException) th).getException();
            } else if (th instanceof SAXException) {
                th = ((SAXException) th).getException();
            } else {
                th = null;
            }
        } while (th != null);
        if (sAXSourceLocator != null) {
            if (sAXSourceLocator.getPublicId() != null) {
                strCreateXMLMessage = sAXSourceLocator.getPublicId();
            } else if (sAXSourceLocator.getSystemId() == null) {
                strCreateXMLMessage = XMLMessages.createXMLMessage(XMLErrorResources.ER_SYSTEMID_UNKNOWN, null);
            } else {
                strCreateXMLMessage = sAXSourceLocator.getSystemId();
            }
            printWriter.print(strCreateXMLMessage + "; " + XMLMessages.createXMLMessage("line", null) + sAXSourceLocator.getLineNumber() + "; " + XMLMessages.createXMLMessage("column", null) + sAXSourceLocator.getColumnNumber() + "; ");
            return;
        }
        printWriter.print("(" + XMLMessages.createXMLMessage(XMLErrorResources.ER_LOCATION_UNKNOWN, null) + ")");
    }
}
