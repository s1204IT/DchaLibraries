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

    public XMLFilterImpl(XMLReader parent) {
        setParent(parent);
    }

    @Override
    public void setParent(XMLReader parent) {
        this.parent = parent;
    }

    @Override
    public XMLReader getParent() {
        return this.parent;
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            this.parent.setFeature(name, value);
            return;
        }
        throw new SAXNotRecognizedException("Feature: " + name);
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            return this.parent.getFeature(name);
        }
        throw new SAXNotRecognizedException("Feature: " + name);
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            this.parent.setProperty(name, value);
            return;
        }
        throw new SAXNotRecognizedException("Property: " + name);
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (this.parent != null) {
            return this.parent.getProperty(name);
        }
        throw new SAXNotRecognizedException("Property: " + name);
    }

    @Override
    public void setEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return this.entityResolver;
    }

    @Override
    public void setDTDHandler(DTDHandler handler) {
        this.dtdHandler = handler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return this.dtdHandler;
    }

    @Override
    public void setContentHandler(ContentHandler handler) {
        this.contentHandler = handler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return this.contentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Override
    public void parse(InputSource input) throws SAXException, IOException {
        setupParse();
        this.parent.parse(input);
    }

    @Override
    public void parse(String systemId) throws SAXException, IOException {
        parse(new InputSource(systemId));
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (this.entityResolver != null) {
            return this.entityResolver.resolveEntity(publicId, systemId);
        }
        return null;
    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        if (this.dtdHandler == null) {
            return;
        }
        this.dtdHandler.notationDecl(name, publicId, systemId);
    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        if (this.dtdHandler == null) {
            return;
        }
        this.dtdHandler.unparsedEntityDecl(name, publicId, systemId, notationName);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (this.contentHandler == null) {
            return;
        }
        this.contentHandler.skippedEntity(name);
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        if (this.errorHandler == null) {
            return;
        }
        this.errorHandler.warning(e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        if (this.errorHandler == null) {
            return;
        }
        this.errorHandler.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        if (this.errorHandler == null) {
            return;
        }
        this.errorHandler.fatalError(e);
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
