package mf.org.apache.xerces.util;

import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class ErrorHandlerProxy implements ErrorHandler {
    protected abstract XMLErrorHandler getErrorHandler();

    @Override
    public void error(SAXParseException e) throws SAXException {
        ?? errorHandler = getErrorHandler();
        if (errorHandler instanceof ErrorHandlerWrapper) {
            errorHandler.fErrorHandler.error(e);
        } else {
            errorHandler.error("", "", ErrorHandlerWrapper.createXMLParseException(e));
        }
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        ?? errorHandler = getErrorHandler();
        if (errorHandler instanceof ErrorHandlerWrapper) {
            errorHandler.fErrorHandler.fatalError(e);
        } else {
            errorHandler.fatalError("", "", ErrorHandlerWrapper.createXMLParseException(e));
        }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        ?? errorHandler = getErrorHandler();
        if (errorHandler instanceof ErrorHandlerWrapper) {
            errorHandler.fErrorHandler.warning(e);
        } else {
            errorHandler.warning("", "", ErrorHandlerWrapper.createXMLParseException(e));
        }
    }
}
