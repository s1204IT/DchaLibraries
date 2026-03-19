package mf.org.apache.xerces.util;

import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ErrorHandlerWrapper implements XMLErrorHandler {
    protected ErrorHandler fErrorHandler;

    public ErrorHandlerWrapper() {
    }

    public ErrorHandlerWrapper(ErrorHandler errorHandler) {
        setErrorHandler(errorHandler);
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.fErrorHandler = errorHandler;
    }

    public ErrorHandler getErrorHandler() {
        return this.fErrorHandler;
    }

    @Override
    public void warning(String domain, String key, XMLParseException exception) throws XNIException {
        if (this.fErrorHandler != null) {
            SAXParseException saxException = createSAXParseException(exception);
            try {
                this.fErrorHandler.warning(saxException);
            } catch (SAXParseException e) {
                throw createXMLParseException(e);
            } catch (SAXException e2) {
                throw createXNIException(e2);
            }
        }
    }

    @Override
    public void error(String domain, String key, XMLParseException exception) throws XNIException {
        if (this.fErrorHandler != null) {
            SAXParseException saxException = createSAXParseException(exception);
            try {
                this.fErrorHandler.error(saxException);
            } catch (SAXParseException e) {
                throw createXMLParseException(e);
            } catch (SAXException e2) {
                throw createXNIException(e2);
            }
        }
    }

    @Override
    public void fatalError(String domain, String key, XMLParseException exception) throws XNIException {
        if (this.fErrorHandler != null) {
            SAXParseException saxException = createSAXParseException(exception);
            try {
                this.fErrorHandler.fatalError(saxException);
            } catch (SAXParseException e) {
                throw createXMLParseException(e);
            } catch (SAXException e2) {
                throw createXNIException(e2);
            }
        }
    }

    protected static SAXParseException createSAXParseException(XMLParseException exception) {
        return new SAXParseException(exception.getMessage(), exception.getPublicId(), exception.getExpandedSystemId(), exception.getLineNumber(), exception.getColumnNumber(), exception.getException());
    }

    protected static XMLParseException createXMLParseException(SAXParseException exception) {
        final String fPublicId = exception.getPublicId();
        final String fExpandedSystemId = exception.getSystemId();
        final int fLineNumber = exception.getLineNumber();
        final int fColumnNumber = exception.getColumnNumber();
        XMLLocator location = new XMLLocator() {
            @Override
            public String getPublicId() {
                return fPublicId;
            }

            @Override
            public String getExpandedSystemId() {
                return fExpandedSystemId;
            }

            @Override
            public String getBaseSystemId() {
                return null;
            }

            @Override
            public String getLiteralSystemId() {
                return null;
            }

            @Override
            public int getColumnNumber() {
                return fColumnNumber;
            }

            @Override
            public int getLineNumber() {
                return fLineNumber;
            }

            @Override
            public int getCharacterOffset() {
                return -1;
            }

            @Override
            public String getEncoding() {
                return null;
            }

            @Override
            public String getXMLVersion() {
                return null;
            }
        };
        return new XMLParseException(location, exception.getMessage(), exception);
    }

    protected static XNIException createXNIException(SAXException exception) {
        return new XNIException(exception.getMessage(), exception);
    }
}
