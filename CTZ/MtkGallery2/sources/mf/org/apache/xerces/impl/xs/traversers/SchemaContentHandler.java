package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.xs.opti.SchemaDOMParser;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.SAXLocatorWrapper;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLAttributesImpl;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import mf.org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

final class SchemaContentHandler implements ContentHandler {
    private boolean fNeedPushNSContext;
    private SchemaDOMParser fSchemaDOMParser;
    private SymbolTable fSymbolTable;
    private final SAXLocatorWrapper fSAXLocatorWrapper = new SAXLocatorWrapper();
    private NamespaceSupport fNamespaceContext = new NamespaceSupport();
    private boolean fNamespacePrefixes = false;
    private boolean fStringsInternalized = false;
    private final QName fElementQName = new QName();
    private final QName fAttributeQName = new QName();
    private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
    private final XMLString fTempString = new XMLString();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    public Document getDocument() {
        return this.fSchemaDOMParser.getDocument();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.fSAXLocatorWrapper.setLocator(locator);
    }

    @Override
    public void startDocument() throws Exception {
        this.fNeedPushNSContext = true;
        this.fNamespaceContext.reset();
        try {
            this.fSchemaDOMParser.startDocument(this.fSAXLocatorWrapper, null, this.fNamespaceContext, null);
        } catch (XMLParseException e) {
            convertToSAXParseException(e);
        } catch (XNIException e2) {
            convertToSAXException(e2);
        }
    }

    @Override
    public void endDocument() throws Exception {
        this.fSAXLocatorWrapper.setLocator(null);
        try {
            this.fSchemaDOMParser.endDocument(null);
        } catch (XMLParseException e) {
            convertToSAXParseException(e);
        } catch (XNIException e2) {
            convertToSAXException(e2);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (this.fNeedPushNSContext) {
            this.fNeedPushNSContext = false;
            this.fNamespaceContext.pushContext();
        }
        if (!this.fStringsInternalized) {
            prefix = prefix != null ? this.fSymbolTable.addSymbol(prefix) : XMLSymbols.EMPTY_STRING;
            uri = (uri == null || uri.length() <= 0) ? null : this.fSymbolTable.addSymbol(uri);
        } else {
            if (prefix == null) {
                prefix = XMLSymbols.EMPTY_STRING;
            }
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
        }
        this.fNamespaceContext.declarePrefix(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws Exception {
        int prefixCount;
        if (this.fNeedPushNSContext) {
            this.fNamespaceContext.pushContext();
        }
        this.fNeedPushNSContext = true;
        fillQName(this.fElementQName, uri, localName, qName);
        fillXMLAttributes(atts);
        if (!this.fNamespacePrefixes && (prefixCount = this.fNamespaceContext.getDeclaredPrefixCount()) > 0) {
            addNamespaceDeclarations(prefixCount);
        }
        try {
            this.fSchemaDOMParser.startElement(this.fElementQName, this.fAttributes, null);
        } catch (XMLParseException e) {
            convertToSAXParseException(e);
        } catch (XNIException e2) {
            convertToSAXException(e2);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        fillQName(this.fElementQName, uri, localName, qName);
        try {
            try {
                this.fSchemaDOMParser.endElement(this.fElementQName, null);
            } catch (XMLParseException e) {
                convertToSAXParseException(e);
            } catch (XNIException e2) {
                convertToSAXException(e2);
            }
        } finally {
            this.fNamespaceContext.popContext();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws Exception {
        try {
            this.fTempString.setValues(ch, start, length);
            this.fSchemaDOMParser.characters(this.fTempString, null);
        } catch (XMLParseException e) {
            convertToSAXParseException(e);
        } catch (XNIException e2) {
            convertToSAXException(e2);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws Exception {
        try {
            this.fTempString.setValues(ch, start, length);
            this.fSchemaDOMParser.ignorableWhitespace(this.fTempString, null);
        } catch (XMLParseException e) {
            convertToSAXParseException(e);
        } catch (XNIException e2) {
            convertToSAXException(e2);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws Exception {
        try {
            this.fTempString.setValues(data.toCharArray(), 0, data.length());
            this.fSchemaDOMParser.processingInstruction(target, this.fTempString, null);
        } catch (XMLParseException e) {
            convertToSAXParseException(e);
        } catch (XNIException e2) {
            convertToSAXException(e2);
        }
    }

    @Override
    public void skippedEntity(String arg) throws SAXException {
    }

    private void fillQName(QName toFill, String uri, String localpart, String rawname) {
        if (!this.fStringsInternalized) {
            uri = (uri == null || uri.length() <= 0) ? null : this.fSymbolTable.addSymbol(uri);
            localpart = localpart != null ? this.fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
            rawname = rawname != null ? this.fSymbolTable.addSymbol(rawname) : XMLSymbols.EMPTY_STRING;
        } else {
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
            if (localpart == null) {
                localpart = XMLSymbols.EMPTY_STRING;
            }
            if (rawname == null) {
                rawname = XMLSymbols.EMPTY_STRING;
            }
        }
        String prefix = XMLSymbols.EMPTY_STRING;
        int prefixIdx = rawname.indexOf(58);
        if (prefixIdx != -1) {
            prefix = this.fSymbolTable.addSymbol(rawname.substring(0, prefixIdx));
            if (localpart == XMLSymbols.EMPTY_STRING) {
                localpart = this.fSymbolTable.addSymbol(rawname.substring(prefixIdx + 1));
            }
        } else if (localpart == XMLSymbols.EMPTY_STRING) {
            localpart = rawname;
        }
        toFill.setValues(prefix, localpart, rawname, uri);
    }

    private void fillXMLAttributes(Attributes atts) {
        this.fAttributes.removeAllAttributes();
        int attrCount = atts.getLength();
        for (int i = 0; i < attrCount; i++) {
            fillQName(this.fAttributeQName, atts.getURI(i), atts.getLocalName(i), atts.getQName(i));
            String type = atts.getType(i);
            this.fAttributes.addAttributeNS(this.fAttributeQName, type != null ? type : XMLSymbols.fCDATASymbol, atts.getValue(i));
            this.fAttributes.setSpecified(i, true);
        }
    }

    private void addNamespaceDeclarations(int prefixCount) {
        String prefix;
        String localpart;
        String rawname;
        String str;
        for (int i = 0; i < prefixCount; i++) {
            String nsPrefix = this.fNamespaceContext.getDeclaredPrefixAt(i);
            String nsURI = this.fNamespaceContext.getURI(nsPrefix);
            if (nsPrefix.length() > 0) {
                prefix = XMLSymbols.PREFIX_XMLNS;
                localpart = nsPrefix;
                this.fStringBuffer.clear();
                this.fStringBuffer.append(prefix);
                this.fStringBuffer.append(':');
                this.fStringBuffer.append(localpart);
                rawname = this.fSymbolTable.addSymbol(this.fStringBuffer.ch, this.fStringBuffer.offset, this.fStringBuffer.length);
            } else {
                prefix = XMLSymbols.EMPTY_STRING;
                localpart = XMLSymbols.PREFIX_XMLNS;
                rawname = XMLSymbols.PREFIX_XMLNS;
            }
            this.fAttributeQName.setValues(prefix, localpart, rawname, NamespaceContext.XMLNS_URI);
            XMLAttributesImpl xMLAttributesImpl = this.fAttributes;
            QName qName = this.fAttributeQName;
            String str2 = XMLSymbols.fCDATASymbol;
            if (nsURI != null) {
                str = nsURI;
            } else {
                str = XMLSymbols.EMPTY_STRING;
            }
            xMLAttributesImpl.addAttribute(qName, str2, str);
        }
    }

    public void reset(SchemaDOMParser schemaDOMParser, SymbolTable symbolTable, boolean namespacePrefixes, boolean stringsInternalized) {
        this.fSchemaDOMParser = schemaDOMParser;
        this.fSymbolTable = symbolTable;
        this.fNamespacePrefixes = namespacePrefixes;
        this.fStringsInternalized = stringsInternalized;
    }

    static void convertToSAXParseException(XMLParseException e) throws Exception {
        Exception ex = e.getException();
        if (ex == null) {
            LocatorImpl locatorImpl = new LocatorImpl();
            locatorImpl.setPublicId(e.getPublicId());
            locatorImpl.setSystemId(e.getExpandedSystemId());
            locatorImpl.setLineNumber(e.getLineNumber());
            locatorImpl.setColumnNumber(e.getColumnNumber());
            throw new SAXParseException(e.getMessage(), locatorImpl);
        }
        if (ex instanceof SAXException) {
            throw ex;
        }
        throw new SAXException(ex);
    }

    static void convertToSAXException(XNIException e) throws Exception {
        Exception ex = e.getException();
        if (ex == null) {
            throw new SAXException(e.getMessage());
        }
        if (ex instanceof SAXException) {
            throw ex;
        }
        throw new SAXException(ex);
    }
}
