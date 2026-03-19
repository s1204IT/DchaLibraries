package org.ccil.cowan.tagsoup.jaxp;

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

public class SAX1ParserAdapter implements Parser {
    final XMLReader xmlReader;

    public SAX1ParserAdapter(XMLReader xMLReader) {
        this.xmlReader = xMLReader;
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException {
        try {
            this.xmlReader.parse(inputSource);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void parse(String str) throws SAXException {
        try {
            this.xmlReader.parse(str);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void setDocumentHandler(DocumentHandler documentHandler) {
        this.xmlReader.setContentHandler(new DocHandlerWrapper(documentHandler));
    }

    @Override
    public void setDTDHandler(DTDHandler dTDHandler) {
        this.xmlReader.setDTDHandler(dTDHandler);
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {
        this.xmlReader.setEntityResolver(entityResolver);
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.xmlReader.setErrorHandler(errorHandler);
    }

    @Override
    public void setLocale(Locale locale) throws SAXException {
        throw new SAXNotSupportedException("TagSoup does not implement setLocale() method");
    }

    static final class DocHandlerWrapper implements ContentHandler {
        final DocumentHandler docHandler;
        final AttributesWrapper mAttrWrapper = new AttributesWrapper();

        DocHandlerWrapper(DocumentHandler documentHandler) {
            this.docHandler = documentHandler;
        }

        @Override
        public void characters(char[] cArr, int i, int i2) throws SAXException {
            this.docHandler.characters(cArr, i, i2);
        }

        @Override
        public void endDocument() throws SAXException {
            this.docHandler.endDocument();
        }

        @Override
        public void endElement(String str, String str2, String str3) throws SAXException {
            if (str3 != null) {
                str2 = str3;
            }
            this.docHandler.endElement(str2);
        }

        @Override
        public void endPrefixMapping(String str) {
        }

        @Override
        public void ignorableWhitespace(char[] cArr, int i, int i2) throws SAXException {
            this.docHandler.ignorableWhitespace(cArr, i, i2);
        }

        @Override
        public void processingInstruction(String str, String str2) throws SAXException {
            this.docHandler.processingInstruction(str, str2);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.docHandler.setDocumentLocator(locator);
        }

        @Override
        public void skippedEntity(String str) {
        }

        @Override
        public void startDocument() throws SAXException {
            this.docHandler.startDocument();
        }

        @Override
        public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
            if (str3 != null) {
                str2 = str3;
            }
            this.mAttrWrapper.setAttributes(attributes);
            this.docHandler.startElement(str2, this.mAttrWrapper);
        }

        @Override
        public void startPrefixMapping(String str, String str2) {
        }
    }

    static final class AttributesWrapper implements AttributeList {
        Attributes attrs;

        public void setAttributes(Attributes attributes) {
            this.attrs = attributes;
        }

        @Override
        public int getLength() {
            return this.attrs.getLength();
        }

        @Override
        public String getName(int i) {
            String qName = this.attrs.getQName(i);
            return qName == null ? this.attrs.getLocalName(i) : qName;
        }

        @Override
        public String getType(int i) {
            return this.attrs.getType(i);
        }

        @Override
        public String getType(String str) {
            return this.attrs.getType(str);
        }

        @Override
        public String getValue(int i) {
            return this.attrs.getValue(i);
        }

        @Override
        public String getValue(String str) {
            return this.attrs.getValue(str);
        }
    }
}
