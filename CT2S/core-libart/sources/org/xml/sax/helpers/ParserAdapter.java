package org.xml.sax.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.xml.XMLConstants;
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
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class ParserAdapter implements XMLReader, DocumentHandler {
    private static final String FEATURES = "http://xml.org/sax/features/";
    private static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    private static final String XMLNS_URIs = "http://xml.org/sax/features/xmlns-uris";
    private AttributeListAdapter attAdapter;
    private AttributesImpl atts;
    ContentHandler contentHandler;
    DTDHandler dtdHandler;
    EntityResolver entityResolver;
    ErrorHandler errorHandler;
    Locator locator;
    private String[] nameParts;
    private boolean namespaces;
    private NamespaceSupport nsSupport;
    private Parser parser;
    private boolean parsing;
    private boolean prefixes;
    private boolean uris;

    public ParserAdapter() throws SAXException {
        this.parsing = false;
        this.nameParts = new String[3];
        this.parser = null;
        this.atts = null;
        this.namespaces = true;
        this.prefixes = false;
        this.uris = false;
        this.entityResolver = null;
        this.dtdHandler = null;
        this.contentHandler = null;
        this.errorHandler = null;
        String driver = System.getProperty("org.xml.sax.parser");
        try {
            setup(ParserFactory.makeParser());
        } catch (ClassCastException e) {
            throw new SAXException("SAX1 driver class " + driver + " does not implement org.xml.sax.Parser");
        } catch (ClassNotFoundException e1) {
            throw new SAXException("Cannot find SAX1 driver class " + driver, e1);
        } catch (IllegalAccessException e2) {
            throw new SAXException("SAX1 driver class " + driver + " found but cannot be loaded", e2);
        } catch (InstantiationException e3) {
            throw new SAXException("SAX1 driver class " + driver + " loaded but cannot be instantiated", e3);
        } catch (NullPointerException e4) {
            throw new SAXException("System property org.xml.sax.parser not specified");
        }
    }

    public ParserAdapter(Parser parser) {
        this.parsing = false;
        this.nameParts = new String[3];
        this.parser = null;
        this.atts = null;
        this.namespaces = true;
        this.prefixes = false;
        this.uris = false;
        this.entityResolver = null;
        this.dtdHandler = null;
        this.contentHandler = null;
        this.errorHandler = null;
        setup(parser);
    }

    private void setup(Parser parser) {
        if (parser == null) {
            throw new NullPointerException("Parser argument must not be null");
        }
        this.parser = parser;
        this.atts = new AttributesImpl();
        this.nsSupport = new NamespaceSupport();
        this.attAdapter = new AttributeListAdapter();
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            checkNotParsing("feature", name);
            this.namespaces = value;
            if (!this.namespaces && !this.prefixes) {
                this.prefixes = true;
                return;
            }
            return;
        }
        if (name.equals(NAMESPACE_PREFIXES)) {
            checkNotParsing("feature", name);
            this.prefixes = value;
            if (!this.prefixes && !this.namespaces) {
                this.namespaces = true;
                return;
            }
            return;
        }
        if (name.equals(XMLNS_URIs)) {
            checkNotParsing("feature", name);
            this.uris = value;
            return;
        }
        throw new SAXNotRecognizedException("Feature: " + name);
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(NAMESPACES)) {
            return this.namespaces;
        }
        if (name.equals(NAMESPACE_PREFIXES)) {
            return this.prefixes;
        }
        if (name.equals(XMLNS_URIs)) {
            return this.uris;
        }
        throw new SAXNotRecognizedException("Feature: " + name);
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Property: " + name);
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
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
    public void parse(String systemId) throws SAXException, IOException {
        parse(new InputSource(systemId));
    }

    @Override
    public void parse(InputSource input) throws SAXException, IOException {
        if (this.parsing) {
            throw new SAXException("Parser is already in use");
        }
        setupParser();
        this.parsing = true;
        try {
            this.parser.parse(input);
            this.parsing = false;
        } finally {
            this.parsing = false;
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
    public void startElement(String qName, AttributeList qAtts) throws SAXException {
        String prefix;
        String prefix2;
        ArrayList<SAXParseException> exceptions = null;
        if (!this.namespaces) {
            if (this.contentHandler != null) {
                this.attAdapter.setAttributeList(qAtts);
                this.contentHandler.startElement("", "", qName.intern(), this.attAdapter);
                return;
            }
            return;
        }
        this.nsSupport.pushContext();
        int length = qAtts.getLength();
        for (int i = 0; i < length; i++) {
            String attQName = qAtts.getName(i);
            if (attQName.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                int n = attQName.indexOf(58);
                if (n == -1 && attQName.length() == 5) {
                    prefix2 = "";
                } else if (n == 5) {
                    prefix2 = attQName.substring(n + 1);
                }
                String value = qAtts.getValue(i);
                if (!this.nsSupport.declarePrefix(prefix2, value)) {
                    reportError("Illegal Namespace prefix: " + prefix2);
                } else if (this.contentHandler != null) {
                    this.contentHandler.startPrefixMapping(prefix2, value);
                }
            }
        }
        this.atts.clear();
        for (int i2 = 0; i2 < length; i2++) {
            String attQName2 = qAtts.getName(i2);
            String type = qAtts.getType(i2);
            String value2 = qAtts.getValue(i2);
            if (attQName2.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                int n2 = attQName2.indexOf(58);
                if (n2 == -1 && attQName2.length() == 5) {
                    prefix = "";
                } else if (n2 != 5) {
                    prefix = null;
                } else {
                    prefix = attQName2.substring(6);
                }
                if (prefix != null) {
                    if (this.prefixes) {
                        if (this.uris) {
                            AttributesImpl attributesImpl = this.atts;
                            NamespaceSupport namespaceSupport = this.nsSupport;
                            attributesImpl.addAttribute("http://www.w3.org/XML/1998/namespace", prefix, attQName2.intern(), type, value2);
                        } else {
                            this.atts.addAttribute("", "", attQName2.intern(), type, value2);
                        }
                    }
                }
            } else {
                try {
                    String[] attName = processName(attQName2, true, true);
                    this.atts.addAttribute(attName[0], attName[1], attName[2], type, value2);
                } catch (SAXException e) {
                    if (exceptions == null) {
                        exceptions = new ArrayList<>();
                    }
                    exceptions.add((SAXParseException) e);
                    this.atts.addAttribute("", attQName2, attQName2, type, value2);
                }
            }
        }
        if (exceptions != null && this.errorHandler != null) {
            for (SAXParseException ex : exceptions) {
                this.errorHandler.error(ex);
            }
        }
        if (this.contentHandler != null) {
            String[] name = processName(qName, false, false);
            this.contentHandler.startElement(name[0], name[1], name[2], this.atts);
        }
    }

    @Override
    public void endElement(String qName) throws SAXException {
        if (!this.namespaces) {
            if (this.contentHandler != null) {
                this.contentHandler.endElement("", "", qName.intern());
                return;
            }
            return;
        }
        String[] names = processName(qName, false, false);
        if (this.contentHandler != null) {
            this.contentHandler.endElement(names[0], names[1], names[2]);
            Enumeration prefixes = this.nsSupport.getDeclaredPrefixes();
            while (prefixes.hasMoreElements()) {
                String prefix = (String) prefixes.nextElement();
                this.contentHandler.endPrefixMapping(prefix);
            }
        }
        this.nsSupport.popContext();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.characters(ch, start, length);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.contentHandler != null) {
            this.contentHandler.processingInstruction(target, data);
        }
    }

    private void setupParser() {
        if (!this.prefixes && !this.namespaces) {
            throw new IllegalStateException();
        }
        this.nsSupport.reset();
        if (this.uris) {
            this.nsSupport.setNamespaceDeclUris(true);
        }
        if (this.entityResolver != null) {
            this.parser.setEntityResolver(this.entityResolver);
        }
        if (this.dtdHandler != null) {
            this.parser.setDTDHandler(this.dtdHandler);
        }
        if (this.errorHandler != null) {
            this.parser.setErrorHandler(this.errorHandler);
        }
        this.parser.setDocumentHandler(this);
        this.locator = null;
    }

    private String[] processName(String qName, boolean isAttribute, boolean useException) throws SAXException {
        String[] parts = this.nsSupport.processName(qName, this.nameParts, isAttribute);
        if (parts == null) {
            if (useException) {
                throw makeException("Undeclared prefix: " + qName);
            }
            reportError("Undeclared prefix: " + qName);
            return new String[]{"", "", qName.intern()};
        }
        return parts;
    }

    void reportError(String message) throws SAXException {
        if (this.errorHandler != null) {
            this.errorHandler.error(makeException(message));
        }
    }

    private SAXParseException makeException(String message) {
        return this.locator != null ? new SAXParseException(message, this.locator) : new SAXParseException(message, null, null, -1, -1);
    }

    private void checkNotParsing(String type, String name) throws SAXNotSupportedException {
        if (this.parsing) {
            throw new SAXNotSupportedException("Cannot change " + type + ' ' + name + " while parsing");
        }
    }

    final class AttributeListAdapter implements Attributes {
        private AttributeList qAtts;

        AttributeListAdapter() {
        }

        void setAttributeList(AttributeList qAtts) {
            this.qAtts = qAtts;
        }

        @Override
        public int getLength() {
            return this.qAtts.getLength();
        }

        @Override
        public String getURI(int i) {
            return "";
        }

        @Override
        public String getLocalName(int i) {
            return "";
        }

        @Override
        public String getQName(int i) {
            return this.qAtts.getName(i).intern();
        }

        @Override
        public String getType(int i) {
            return this.qAtts.getType(i).intern();
        }

        @Override
        public String getValue(int i) {
            return this.qAtts.getValue(i);
        }

        @Override
        public int getIndex(String uri, String localName) {
            return -1;
        }

        @Override
        public int getIndex(String qName) {
            int max = ParserAdapter.this.atts.getLength();
            for (int i = 0; i < max; i++) {
                if (this.qAtts.getName(i).equals(qName)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public String getType(String uri, String localName) {
            return null;
        }

        @Override
        public String getType(String qName) {
            return this.qAtts.getType(qName).intern();
        }

        @Override
        public String getValue(String uri, String localName) {
            return null;
        }

        @Override
        public String getValue(String qName) {
            return this.qAtts.getValue(qName);
        }
    }
}
