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
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

public class XMLFilterImpl implements XMLFilter, EntityResolver, DTDHandler, ContentHandler, ErrorHandler {
    private XMLReader parent = null;
    private Locator locator = null;
    private EntityResolver entityResolver = null;
    private DTDHandler dtdHandler = null;
    private ContentHandler contentHandler = null;
    private ErrorHandler errorHandler = null;

    public XMLFilterImpl() {
    }

    public XMLFilterImpl(XMLReader xMLReader) {
        setParent(xMLReader);
    }

    @Override
    public void setParent(XMLReader xMLReader) {
        this.parent = xMLReader;
    }

    @Override
    public XMLReader getParent() {
        return this.parent;
    }

    @Override
    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            this.parent.setFeature(str, z);
            return;
        }
        throw new SAXNotRecognizedException("Feature: " + str);
    }

    @Override
    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            return this.parent.getFeature(str);
        }
        throw new SAXNotRecognizedException("Feature: " + str);
    }

    @Override
    public void setProperty(String str, Object obj) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            this.parent.setProperty(str, obj);
            return;
        }
        throw new SAXNotRecognizedException("Property: " + str);
    }

    @Override
    public Object getProperty(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            return this.parent.getProperty(str);
        }
        throw new SAXNotRecognizedException("Property: " + str);
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return this.entityResolver;
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
        this.dtdHandler = dTDHandler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return this.dtdHandler;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.contentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        setupParse();
        this.parent.parse(inputSource);
    }

    @Override
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
    }

    @Override
    public InputSource resolveEntity(String str, String str2) throws SAXException, IOException {
        if (this.entityResolver != null) {
            return this.entityResolver.resolveEntity(str, str2);
        }
        return null;
    }

    @Override
    public void notationDecl(String str, String str2, String str3) throws SAXException {
        if (this.dtdHandler != null) {
            this.dtdHandler.notationDecl(str, str2, str3);
        }
    }

    @Override
    public void unparsedEntityDecl(String str, String str2, String str3, String str4) throws SAXException {
        if (this.dtdHandler != null) {
            this.dtdHandler.unparsedEntityDecl(str, str2, str3, str4);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        if (this.contentHandler != null) {
            this.contentHandler.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.endDocument();
        }
    }

    @Override
    public void startPrefixMapping(String str, String str2) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.startPrefixMapping(str, str2);
        }
    }

    @Override
    public void endPrefixMapping(String str) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.endPrefixMapping(str);
        }
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.startElement(str, str2, str3, attributes);
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.endElement(str, str2, str3);
        }
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.characters(cArr, i, i2);
        }
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.ignorableWhitespace(cArr, i, i2);
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.processingInstruction(str, str2);
        }
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.skippedEntity(str);
        }
    }

    @Override
    public void warning(SAXParseException sAXParseException) throws SAXException {
        if (this.errorHandler != null) {
            this.errorHandler.warning(sAXParseException);
        }
    }

    @Override
    public void error(SAXParseException sAXParseException) throws SAXException {
        if (this.errorHandler != null) {
            this.errorHandler.error(sAXParseException);
        }
    }

    @Override
    public void fatalError(SAXParseException sAXParseException) throws SAXException {
        if (this.errorHandler != null) {
            this.errorHandler.fatalError(sAXParseException);
        }
    }

    private void setupParse() {
        if (this.parent == null) {
            throw new NullPointerException("No parent for filter");
        }
        this.parent.setEntityResolver(this);
        this.parent.setDTDHandler(this);
        this.parent.setContentHandler(this);
        this.parent.setErrorHandler(this);
    }
}
