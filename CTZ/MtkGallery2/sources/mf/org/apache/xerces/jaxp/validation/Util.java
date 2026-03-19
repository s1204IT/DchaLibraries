package mf.org.apache.xerces.jaxp.validation;

import mf.javax.xml.transform.stream.StreamSource;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

final class Util {
    Util() {
    }

    public static final XMLInputSource toXMLInputSource(StreamSource in) {
        if (in.getReader() != null) {
            return new XMLInputSource(in.getPublicId(), in.getSystemId(), in.getSystemId(), in.getReader(), (String) null);
        }
        if (in.getInputStream() != null) {
            return new XMLInputSource(in.getPublicId(), in.getSystemId(), in.getSystemId(), in.getInputStream(), (String) null);
        }
        return new XMLInputSource(in.getPublicId(), in.getSystemId(), in.getSystemId());
    }

    public static SAXException toSAXException(XNIException xNIException) {
        if (xNIException instanceof XMLParseException) {
            return toSAXParseException(xNIException);
        }
        if (xNIException.getException() instanceof SAXException) {
            return (SAXException) xNIException.getException();
        }
        return new SAXException(xNIException.getMessage(), xNIException.getException());
    }

    public static SAXParseException toSAXParseException(XMLParseException e) {
        if (e.getException() instanceof SAXParseException) {
            return (SAXParseException) e.getException();
        }
        return new SAXParseException(e.getMessage(), e.getPublicId(), e.getExpandedSystemId(), e.getLineNumber(), e.getColumnNumber(), e.getException());
    }
}
