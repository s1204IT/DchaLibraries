package org.xml.sax.helpers;

import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DefaultHandler implements EntityResolver, DTDHandler, ContentHandler, ErrorHandler {
    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException, IOException {
        return null;
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        throw sAXParseException;
    }
}
