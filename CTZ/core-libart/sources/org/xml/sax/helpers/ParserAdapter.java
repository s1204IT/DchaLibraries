package org.xml.sax.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
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
        String property = System.getProperty("org.xml.sax.parser");
        try {
            setup(ParserFactory.makeParser());
        } catch (ClassCastException e) {
            throw new SAXException("SAX1 driver class " + property + " does not implement org.xml.sax.Parser");
        } catch (ClassNotFoundException e2) {
            throw new SAXException("Cannot find SAX1 driver class " + property, e2);
        } catch (IllegalAccessException e3) {
            throw new SAXException("SAX1 driver class " + property + " found but cannot be loaded", e3);
        } catch (InstantiationException e4) {
            throw new SAXException("SAX1 driver class " + property + " loaded but cannot be instantiated", e4);
        } catch (NullPointerException e5) {
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
    public void setFeature(String str, boolean z) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str.equals(NAMESPACES)) {
            checkNotParsing("feature", str);
            this.namespaces = z;
            if (!this.namespaces && !this.prefixes) {
                this.prefixes = true;
                return;
            }
            return;
        }
        if (str.equals(NAMESPACE_PREFIXES)) {
            checkNotParsing("feature", str);
            this.prefixes = z;
            if (!this.prefixes && !this.namespaces) {
                this.namespaces = true;
                return;
            }
            return;
        }
        if (str.equals(XMLNS_URIs)) {
            checkNotParsing("feature", str);
            this.uris = z;
        } else {
            throw new SAXNotRecognizedException("Feature: " + str);
        }
    }

    @Override
    public boolean getFeature(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (str.equals(NAMESPACES)) {
            return this.namespaces;
        }
        if (str.equals(NAMESPACE_PREFIXES)) {
            return this.prefixes;
        }
        if (str.equals(XMLNS_URIs)) {
            return this.uris;
        }
        throw new SAXNotRecognizedException("Feature: " + str);
    }

    @Override
    public void setProperty(String str, Object obj) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Property: " + str);
    }

    @Override
    public Object getProperty(String str) throws SAXNotRecognizedException, SAXNotSupportedException {
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
    public void parse(String str) throws SAXException, IOException {
        parse(new InputSource(str));
    }

    @Override
    public void parse(InputSource inputSource) throws SAXException, IOException {
        if (this.parsing) {
            throw new SAXException("Parser is already in use");
        }
        setupParser();
        this.parsing = true;
        try {
            this.parser.parse(inputSource);
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
    public void startElement(String str, AttributeList attributeList) throws SAXException {
        String strSubstring;
        String str2;
        String strSubstring2;
        if (!this.namespaces) {
            if (this.contentHandler != null) {
                this.attAdapter.setAttributeList(attributeList);
                this.contentHandler.startElement("", "", str.intern(), this.attAdapter);
                return;
            }
            return;
        }
        this.nsSupport.pushContext();
        int length = attributeList.getLength();
        for (int i = 0; i < length; i++) {
            String name = attributeList.getName(i);
            if (name.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                int iIndexOf = name.indexOf(58);
                if (iIndexOf == -1 && name.length() == 5) {
                    strSubstring2 = "";
                } else if (iIndexOf == 5) {
                    strSubstring2 = name.substring(iIndexOf + 1);
                }
                String value = attributeList.getValue(i);
                if (!this.nsSupport.declarePrefix(strSubstring2, value)) {
                    reportError("Illegal Namespace prefix: " + strSubstring2);
                } else if (this.contentHandler != null) {
                    this.contentHandler.startPrefixMapping(strSubstring2, value);
                }
            }
        }
        this.atts.clear();
        ArrayList arrayList = null;
        for (int i2 = 0; i2 < length; i2++) {
            String name2 = attributeList.getName(i2);
            String type = attributeList.getType(i2);
            String value2 = attributeList.getValue(i2);
            if (name2.startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                int iIndexOf2 = name2.indexOf(58);
                if (iIndexOf2 == -1 && name2.length() == 5) {
                    strSubstring = "";
                } else if (iIndexOf2 == 5) {
                    strSubstring = name2.substring(6);
                } else {
                    str2 = null;
                    if (str2 == null) {
                        if (this.prefixes) {
                            if (this.uris) {
                                AttributesImpl attributesImpl = this.atts;
                                NamespaceSupport namespaceSupport = this.nsSupport;
                                attributesImpl.addAttribute("http://www.w3.org/XML/1998/namespace", str2, name2.intern(), type, value2);
                            } else {
                                this.atts.addAttribute("", "", name2.intern(), type, value2);
                            }
                        }
                    }
                }
                str2 = strSubstring;
                if (str2 == null) {
                }
            } else {
                try {
                    String[] strArrProcessName = processName(name2, true, true);
                    this.atts.addAttribute(strArrProcessName[0], strArrProcessName[1], strArrProcessName[2], type, value2);
                } catch (SAXException e) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add((SAXParseException) e);
                    this.atts.addAttribute("", name2, name2, type, value2);
                }
            }
        }
        if (arrayList != null && this.errorHandler != null) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                this.errorHandler.error((SAXParseException) it.next());
            }
        }
        if (this.contentHandler != null) {
            String[] strArrProcessName2 = processName(str, false, false);
            this.contentHandler.startElement(strArrProcessName2[0], strArrProcessName2[1], strArrProcessName2[2], this.atts);
        }
    }

    @Override
    public void endElement(String str) throws SAXException {
        if (!this.namespaces) {
            if (this.contentHandler != null) {
                this.contentHandler.endElement("", "", str.intern());
                return;
            }
            return;
        }
        String[] strArrProcessName = processName(str, false, false);
        if (this.contentHandler != null) {
            this.contentHandler.endElement(strArrProcessName[0], strArrProcessName[1], strArrProcessName[2]);
            Enumeration declaredPrefixes = this.nsSupport.getDeclaredPrefixes();
            while (declaredPrefixes.hasMoreElements()) {
                this.contentHandler.endPrefixMapping((String) declaredPrefixes.nextElement());
            }
        }
        this.nsSupport.popContext();
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

    private String[] processName(String str, boolean z, boolean z2) throws SAXException {
        String[] strArrProcessName = this.nsSupport.processName(str, this.nameParts, z);
        if (strArrProcessName == null) {
            if (z2) {
                throw makeException("Undeclared prefix: " + str);
            }
            reportError("Undeclared prefix: " + str);
            return new String[]{"", "", str.intern()};
        }
        return strArrProcessName;
    }

    void reportError(String str) throws SAXException {
        if (this.errorHandler != null) {
            this.errorHandler.error(makeException(str));
        }
    }

    private SAXParseException makeException(String str) {
        if (this.locator != null) {
            return new SAXParseException(str, this.locator);
        }
        return new SAXParseException(str, null, null, -1, -1);
    }

    private void checkNotParsing(String str, String str2) throws SAXNotSupportedException {
        if (this.parsing) {
            throw new SAXNotSupportedException("Cannot change " + str + ' ' + str2 + " while parsing");
        }
    }

    final class AttributeListAdapter implements Attributes {
        private AttributeList qAtts;

        AttributeListAdapter() {
        }

        void setAttributeList(AttributeList attributeList) {
            this.qAtts = attributeList;
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
        public int getIndex(String str, String str2) {
            return -1;
        }

        @Override
        public int getIndex(String str) {
            int length = ParserAdapter.this.atts.getLength();
            for (int i = 0; i < length; i++) {
                if (this.qAtts.getName(i).equals(str)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public String getType(String str, String str2) {
            return null;
        }

        @Override
        public String getType(String str) {
            return this.qAtts.getType(str).intern();
        }

        @Override
        public String getValue(String str, String str2) {
            return null;
        }

        @Override
        public String getValue(String str) {
            return this.qAtts.getValue(str);
        }
    }
}
