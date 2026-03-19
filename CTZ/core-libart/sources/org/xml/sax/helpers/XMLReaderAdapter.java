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

    public XMLReaderAdapter(XMLReader xMLReader) {
        setup(xMLReader);
    }

    private void setup(XMLReader xMLReader) {
        if (xMLReader == null) {
            throw new NullPointerException("XMLReader must not be null");
        }
        this.xmlReader = xMLReader;
        this.qAtts = new AttributesAdapter();
    }

    @Override
    public void setLocale(Locale locale) throws SAXException {
        throw new SAXNotSupportedException("setLocale not supported");
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        this.xmlReader.setEntityResolver(entityResolver);
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
        this.xmlReader.setDTDHandler(dTDHandler);
    }

    @Override
    public void setDocumentHandler(DocumentHandler documentHandler) {
        this.documentHandler = documentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.xmlReader.setErrorHandler(errorHandler);
    }

    @Override
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        setupXMLReader();
        this.xmlReader.parse(inputSource);
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
        if (this.documentHandler != null) {
            this.documentHandler.setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        if (this.documentHandler != null) {
            this.documentHandler.startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.documentHandler != null) {
            this.documentHandler.endDocument();
        }
    }

    @Override
    public void startPrefixMapping(String str, String str2) {
    }

    @Override
    public void endPrefixMapping(String str) {
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.documentHandler != null) {
            this.qAtts.setAttributes(attributes);
            this.documentHandler.startElement(str3, this.qAtts);
        }
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        if (this.documentHandler != null) {
            this.documentHandler.endElement(str3);
        }
    }

    @Override
    public void characters(char[] cArr, int i, int i2) throws SAXException {
        if (this.documentHandler != null) {
            this.documentHandler.characters(cArr, i, i2);
        }
    }

    @Override
    public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
        if (this.documentHandler != null) {
            this.documentHandler.ignorableWhitespace(cArr, i, i2);
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        if (this.documentHandler != null) {
            this.documentHandler.processingInstruction(str, str2);
        }
    }

    @Override
    public void skippedEntity(String str) throws SAXException {
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
        public String getType(String str) {
            return this.attributes.getType(str);
        }

        @Override
        public String getValue(String str) {
            return this.attributes.getValue(str);
        }
    }
}
