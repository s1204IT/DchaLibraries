package org.xml.sax.helpers;

import java.io.IOException;
import java.util.Locale;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class XMLReaderAdapter implements Parser, ContentHandler {
    DocumentHandler documentHandler;
    AttributesAdapter qAtts;
    XMLReader xmlReader;

    public XMLReaderAdapter() throws SAXException {
        setup(XMLReaderFactory.createXMLReader());
    }

    public XMLReaderAdapter(XMLReader xmlReader) {
        setup(xmlReader);
    }

    private void setup(XMLReader xmlReader) {
        if (xmlReader == null) {
            throw new NullPointerException("XMLReader must not be null");
        }
        this.xmlReader = xmlReader;
        this.qAtts = new AttributesAdapter();
    }

    @Override
    public void setLocale(Locale locale) throws SAXException {
        throw new SAXNotSupportedException("setLocale not supported");
    }

    @Override
    public void setEntityResolver(EntityResolver resolver) {
        this.xmlReader.setEntityResolver(resolver);
    }

    @Override
    public void setDTDHandler(DTDHandler handler) {
        this.xmlReader.setDTDHandler(handler);
    }

    @Override
    public void setDocumentHandler(DocumentHandler handler) {
        this.documentHandler = handler;
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        this.xmlReader.setErrorHandler(handler);
    }

    @Override
    public void parse(String systemId) throws SAXException, IOException {
        parse(new InputSource(systemId));
    }

    @Override
    public void parse(InputSource input) throws SAXException, IOException {
        setupXMLReader();
        this.xmlReader.parse(input);
    }

    private void setupXMLReader() throws SAXException {
        this.xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        try {
            this.xmlReader.setFeature("http://xml.org/sax/features/namespaces", false);
        } catch (SAXException e) {
        }
        this.xmlReader.setContentHandler(this);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
    }

    @Override
    public void endPrefixMapping(String prefix) {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.qAtts.setAttributes(atts);
        this.documentHandler.startElement(qName, this.qAtts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.endElement(qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.documentHandler == null) {
            return;
        }
        this.documentHandler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    static final class AttributesAdapter implements AttributeList {
        private Attributes attributes;

        AttributesAdapter() {
        }

        void setAttributes(Attributes attributes) {
            this.attributes = attributes;
        }

        @Override
        public int getLength() {
            return this.attributes.getLength();
        }

        @Override
        public String getName(int i) {
            return this.attributes.getQName(i);
        }

        @Override
        public String getType(int i) {
            return this.attributes.getType(i);
        }

        @Override
        public String getValue(int i) {
            return this.attributes.getValue(i);
        }

        @Override
        public String getType(String qName) {
            return this.attributes.getType(qName);
        }

        @Override
        public String getValue(String qName) {
            return this.attributes.getValue(qName);
        }
    }
}
