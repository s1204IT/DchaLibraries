package mf.org.apache.xml.resolver.readers;

import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParserHandler extends DefaultHandler {
    private EntityResolver er = null;
    private ContentHandler ch = null;

    public void setEntityResolver(EntityResolver er) {
        this.er = er;
    }

    public void setContentHandler(ContentHandler ch) {
        this.ch = ch;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        if (this.er == null) {
            return null;
        }
        try {
            return this.er.resolveEntity(publicId, systemId);
        } catch (IOException e) {
            System.out.println("resolveEntity threw IOException!");
            return null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.ch != null) {
            this.ch.characters(ch, start, length);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (this.ch != null) {
            this.ch.endDocument();
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (this.ch != null) {
            this.ch.endElement(namespaceURI, localName, qName);
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (this.ch != null) {
            this.ch.endPrefixMapping(prefix);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.ch != null) {
            this.ch.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.ch != null) {
            this.ch.processingInstruction(target, data);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        if (this.ch != null) {
            this.ch.setDocumentLocator(locator);
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (this.ch != null) {
            this.ch.skippedEntity(name);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        if (this.ch != null) {
            this.ch.startDocument();
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (this.ch != null) {
            this.ch.startElement(namespaceURI, localName, qName, atts);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (this.ch != null) {
            this.ch.startPrefixMapping(prefix, uri);
        }
    }
}
