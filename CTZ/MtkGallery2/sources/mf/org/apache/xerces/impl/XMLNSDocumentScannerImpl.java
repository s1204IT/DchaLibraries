package mf.org.apache.xerces.impl;

import java.io.IOException;
import mf.org.apache.xerces.impl.XMLDocumentFragmentScannerImpl;
import mf.org.apache.xerces.impl.XMLDocumentScannerImpl;
import mf.org.apache.xerces.impl.dtd.XMLDTDValidatorFilter;
import mf.org.apache.xerces.impl.msg.XMLMessageFormatter;
import mf.org.apache.xerces.util.XMLAttributesImpl;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;

public class XMLNSDocumentScannerImpl extends XMLDocumentScannerImpl {
    protected boolean fBindNamespaces;
    private XMLDTDValidatorFilter fDTDValidator;
    protected boolean fPerformValidation;
    private boolean fSawSpace;

    public void setDTDValidator(XMLDTDValidatorFilter dtdValidator) {
        this.fDTDValidator = dtdValidator;
    }

    @Override
    protected boolean scanStartElement() throws IOException, XNIException {
        QName name;
        this.fEntityScanner.scanQName(this.fElementQName);
        String rawname = this.fElementQName.rawname;
        if (this.fBindNamespaces) {
            this.fNamespaceContext.pushContext();
            if (this.fScannerState == 6 && this.fPerformValidation) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_GRAMMAR_NOT_FOUND", new Object[]{rawname}, (short) 1);
                if (this.fDoctypeName == null || !this.fDoctypeName.equals(rawname)) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "RootElementTypeMustMatchDoctypedecl", new Object[]{this.fDoctypeName, rawname}, (short) 1);
                }
            }
        }
        this.fCurrentElement = this.fElementStack.pushElement(this.fElementQName);
        boolean empty = false;
        this.fAttributes.removeAllAttributes();
        while (true) {
            boolean sawSpace = this.fEntityScanner.skipSpaces();
            int c = this.fEntityScanner.peekChar();
            if (c == 62) {
                this.fEntityScanner.scanChar();
                break;
            }
            if (c == 47) {
                this.fEntityScanner.scanChar();
                if (!this.fEntityScanner.skipChar(62)) {
                    reportFatalError("ElementUnterminated", new Object[]{rawname});
                }
                empty = true;
            } else {
                if (!isValidNameStartChar(c) || !sawSpace) {
                    reportFatalError("ElementUnterminated", new Object[]{rawname});
                }
                scanAttribute(this.fAttributes);
            }
        }
        boolean sawSpace2 = this.fBindNamespaces;
        if (sawSpace2) {
            if (this.fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
                this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementXMLNSPrefix", new Object[]{this.fElementQName.rawname}, (short) 2);
            }
            String prefix = this.fElementQName.prefix != null ? this.fElementQName.prefix : XMLSymbols.EMPTY_STRING;
            this.fElementQName.uri = this.fNamespaceContext.getURI(prefix);
            this.fCurrentElement.uri = this.fElementQName.uri;
            if (this.fElementQName.prefix == null && this.fElementQName.uri != null) {
                this.fElementQName.prefix = XMLSymbols.EMPTY_STRING;
                this.fCurrentElement.prefix = XMLSymbols.EMPTY_STRING;
            }
            if (this.fElementQName.prefix != null && this.fElementQName.uri == null) {
                this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementPrefixUnbound", new Object[]{this.fElementQName.prefix, this.fElementQName.rawname}, (short) 2);
            }
            int length = this.fAttributes.getLength();
            for (int i = 0; i < length; i++) {
                this.fAttributes.getName(i, this.fAttributeQName);
                String aprefix = this.fAttributeQName.prefix != null ? this.fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
                String uri = this.fNamespaceContext.getURI(aprefix);
                if ((this.fAttributeQName.uri == null || this.fAttributeQName.uri != uri) && aprefix != XMLSymbols.EMPTY_STRING) {
                    this.fAttributeQName.uri = uri;
                    if (uri == null) {
                        this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributePrefixUnbound", new Object[]{this.fElementQName.rawname, this.fAttributeQName.rawname, aprefix}, (short) 2);
                    }
                    this.fAttributes.setURI(i, uri);
                }
            }
            if (length > 1 && (name = this.fAttributes.checkDuplicatesNS()) != null) {
                if (name.uri != null) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNSNotUnique", new Object[]{this.fElementQName.rawname, name.localpart, name.uri}, (short) 2);
                } else {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNotUnique", new Object[]{this.fElementQName.rawname, name.rawname}, (short) 2);
                }
            }
        }
        if (this.fDocumentHandler != null) {
            if (!empty) {
                this.fDocumentHandler.startElement(this.fElementQName, this.fAttributes, null);
            } else {
                this.fMarkupDepth--;
                if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
                    reportFatalError("ElementEntityMismatch", new Object[]{this.fCurrentElement.rawname});
                }
                this.fDocumentHandler.emptyElement(this.fElementQName, this.fAttributes, null);
                if (this.fBindNamespaces) {
                    this.fNamespaceContext.popContext();
                }
                this.fElementStack.popElement(this.fElementQName);
            }
        }
        return empty;
    }

    @Override
    protected void scanStartElementName() throws IOException, XNIException {
        this.fEntityScanner.scanQName(this.fElementQName);
        this.fSawSpace = this.fEntityScanner.skipSpaces();
    }

    @Override
    protected boolean scanStartElementAfterName() throws IOException, XNIException {
        QName name;
        String rawname = this.fElementQName.rawname;
        if (this.fBindNamespaces) {
            this.fNamespaceContext.pushContext();
            if (this.fScannerState == 6 && this.fPerformValidation) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_GRAMMAR_NOT_FOUND", new Object[]{rawname}, (short) 1);
                if (this.fDoctypeName == null || !this.fDoctypeName.equals(rawname)) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "RootElementTypeMustMatchDoctypedecl", new Object[]{this.fDoctypeName, rawname}, (short) 1);
                }
            }
        }
        this.fCurrentElement = this.fElementStack.pushElement(this.fElementQName);
        boolean empty = false;
        this.fAttributes.removeAllAttributes();
        while (true) {
            int c = this.fEntityScanner.peekChar();
            if (c == 62) {
                this.fEntityScanner.scanChar();
                break;
            }
            if (c == 47) {
                this.fEntityScanner.scanChar();
                if (!this.fEntityScanner.skipChar(62)) {
                    reportFatalError("ElementUnterminated", new Object[]{rawname});
                }
                empty = true;
            } else {
                if (!isValidNameStartChar(c) || !this.fSawSpace) {
                    reportFatalError("ElementUnterminated", new Object[]{rawname});
                }
                scanAttribute(this.fAttributes);
                this.fSawSpace = this.fEntityScanner.skipSpaces();
            }
        }
        if (this.fBindNamespaces) {
            if (this.fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
                this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementXMLNSPrefix", new Object[]{this.fElementQName.rawname}, (short) 2);
            }
            String prefix = this.fElementQName.prefix != null ? this.fElementQName.prefix : XMLSymbols.EMPTY_STRING;
            this.fElementQName.uri = this.fNamespaceContext.getURI(prefix);
            this.fCurrentElement.uri = this.fElementQName.uri;
            if (this.fElementQName.prefix == null && this.fElementQName.uri != null) {
                this.fElementQName.prefix = XMLSymbols.EMPTY_STRING;
                this.fCurrentElement.prefix = XMLSymbols.EMPTY_STRING;
            }
            if (this.fElementQName.prefix != null && this.fElementQName.uri == null) {
                this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementPrefixUnbound", new Object[]{this.fElementQName.prefix, this.fElementQName.rawname}, (short) 2);
            }
            int length = this.fAttributes.getLength();
            for (int i = 0; i < length; i++) {
                this.fAttributes.getName(i, this.fAttributeQName);
                String aprefix = this.fAttributeQName.prefix != null ? this.fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
                String uri = this.fNamespaceContext.getURI(aprefix);
                if ((this.fAttributeQName.uri == null || this.fAttributeQName.uri != uri) && aprefix != XMLSymbols.EMPTY_STRING) {
                    this.fAttributeQName.uri = uri;
                    if (uri == null) {
                        this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributePrefixUnbound", new Object[]{this.fElementQName.rawname, this.fAttributeQName.rawname, aprefix}, (short) 2);
                    }
                    this.fAttributes.setURI(i, uri);
                }
            }
            if (length > 1 && (name = this.fAttributes.checkDuplicatesNS()) != null) {
                if (name.uri != null) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNSNotUnique", new Object[]{this.fElementQName.rawname, name.localpart, name.uri}, (short) 2);
                } else {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "AttributeNotUnique", new Object[]{this.fElementQName.rawname, name.rawname}, (short) 2);
                }
            }
        }
        if (this.fDocumentHandler != null) {
            if (!empty) {
                this.fDocumentHandler.startElement(this.fElementQName, this.fAttributes, null);
            } else {
                this.fMarkupDepth--;
                if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
                    reportFatalError("ElementEntityMismatch", new Object[]{this.fCurrentElement.rawname});
                }
                this.fDocumentHandler.emptyElement(this.fElementQName, this.fAttributes, null);
                if (this.fBindNamespaces) {
                    this.fNamespaceContext.popContext();
                }
                this.fElementStack.popElement(this.fElementQName);
            }
        }
        return empty;
    }

    protected void scanAttribute(XMLAttributesImpl attributes) throws IOException, XNIException {
        int attrIndex;
        this.fEntityScanner.scanQName(this.fAttributeQName);
        this.fEntityScanner.skipSpaces();
        if (!this.fEntityScanner.skipChar(61)) {
            reportFatalError("EqRequiredInAttribute", new Object[]{this.fCurrentElement.rawname, this.fAttributeQName.rawname});
        }
        this.fEntityScanner.skipSpaces();
        if (this.fBindNamespaces) {
            int attrIndex2 = attributes.getLength();
            attributes.addAttributeNS(this.fAttributeQName, XMLSymbols.fCDATASymbol, null);
            attrIndex = attrIndex2;
        } else {
            int oldLen = attributes.getLength();
            int attrIndex3 = attributes.addAttribute(this.fAttributeQName, XMLSymbols.fCDATASymbol, null);
            if (oldLen == attributes.getLength()) {
                reportFatalError("AttributeNotUnique", new Object[]{this.fCurrentElement.rawname, this.fAttributeQName.rawname});
            }
            attrIndex = attrIndex3;
        }
        boolean isSameNormalizedAttr = scanAttributeValue(this.fTempString, this.fTempString2, this.fAttributeQName.rawname, this.fIsEntityDeclaredVC, this.fCurrentElement.rawname);
        String value = this.fTempString.toString();
        attributes.setValue(attrIndex, value);
        if (!isSameNormalizedAttr) {
            attributes.setNonNormalizedValue(attrIndex, this.fTempString2.toString());
        }
        attributes.setSpecified(attrIndex, true);
        if (this.fBindNamespaces) {
            String localpart = this.fAttributeQName.localpart;
            String prefix = this.fAttributeQName.prefix != null ? this.fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
            if (prefix == XMLSymbols.PREFIX_XMLNS || (prefix == XMLSymbols.EMPTY_STRING && localpart == XMLSymbols.PREFIX_XMLNS)) {
                String uri = this.fSymbolTable.addSymbol(value);
                if (prefix == XMLSymbols.PREFIX_XMLNS && localpart == XMLSymbols.PREFIX_XMLNS) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXMLNS", new Object[]{this.fAttributeQName}, (short) 2);
                }
                if (uri == NamespaceContext.XMLNS_URI) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXMLNS", new Object[]{this.fAttributeQName}, (short) 2);
                }
                if (localpart == XMLSymbols.PREFIX_XML) {
                    if (uri != NamespaceContext.XML_URI) {
                        this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXML", new Object[]{this.fAttributeQName}, (short) 2);
                    }
                } else if (uri == NamespaceContext.XML_URI) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "CantBindXML", new Object[]{this.fAttributeQName}, (short) 2);
                }
                String prefix2 = localpart != XMLSymbols.PREFIX_XMLNS ? localpart : XMLSymbols.EMPTY_STRING;
                if (uri == XMLSymbols.EMPTY_STRING && localpart != XMLSymbols.PREFIX_XMLNS) {
                    this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "EmptyPrefixedAttName", new Object[]{this.fAttributeQName}, (short) 2);
                }
                this.fNamespaceContext.declarePrefix(prefix2, uri.length() != 0 ? uri : null);
                attributes.setURI(attrIndex, this.fNamespaceContext.getURI(XMLSymbols.PREFIX_XMLNS));
                return;
            }
            if (this.fAttributeQName.prefix != null) {
                attributes.setURI(attrIndex, this.fNamespaceContext.getURI(this.fAttributeQName.prefix));
            }
        }
    }

    @Override
    protected int scanEndElement() throws IOException, XNIException {
        this.fElementStack.popElement(this.fElementQName);
        if (!this.fEntityScanner.skipString(this.fElementQName.rawname)) {
            reportFatalError("ETagRequired", new Object[]{this.fElementQName.rawname});
        }
        this.fEntityScanner.skipSpaces();
        if (!this.fEntityScanner.skipChar(62)) {
            reportFatalError("ETagUnterminated", new Object[]{this.fElementQName.rawname});
        }
        this.fMarkupDepth--;
        this.fMarkupDepth--;
        if (this.fMarkupDepth < this.fEntityStack[this.fEntityDepth - 1]) {
            reportFatalError("ElementEntityMismatch", new Object[]{this.fCurrentElement.rawname});
        }
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endElement(this.fElementQName, null);
            if (this.fBindNamespaces) {
                this.fNamespaceContext.popContext();
            }
        }
        return this.fMarkupDepth;
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        super.reset(componentManager);
        this.fPerformValidation = false;
        this.fBindNamespaces = false;
    }

    @Override
    protected XMLDocumentFragmentScannerImpl.Dispatcher createContentDispatcher() {
        return new NSContentDispatcher();
    }

    protected final class NSContentDispatcher extends XMLDocumentScannerImpl.ContentDispatcher {
        protected NSContentDispatcher() {
            super();
        }

        @Override
        protected boolean scanRootElementHook() throws IOException, XNIException {
            if (XMLNSDocumentScannerImpl.this.fExternalSubsetResolver != null && !XMLNSDocumentScannerImpl.this.fSeenDoctypeDecl && !XMLNSDocumentScannerImpl.this.fDisallowDoctype && (XMLNSDocumentScannerImpl.this.fValidation || XMLNSDocumentScannerImpl.this.fLoadExternalDTD)) {
                XMLNSDocumentScannerImpl.this.scanStartElementName();
                resolveExternalSubsetAndRead();
                reconfigurePipeline();
                if (XMLNSDocumentScannerImpl.this.scanStartElementAfterName()) {
                    XMLNSDocumentScannerImpl.this.setScannerState(12);
                    XMLNSDocumentScannerImpl.this.setDispatcher(XMLNSDocumentScannerImpl.this.fTrailingMiscDispatcher);
                    return true;
                }
                return false;
            }
            reconfigurePipeline();
            if (XMLNSDocumentScannerImpl.this.scanStartElement()) {
                XMLNSDocumentScannerImpl.this.setScannerState(12);
                XMLNSDocumentScannerImpl.this.setDispatcher(XMLNSDocumentScannerImpl.this.fTrailingMiscDispatcher);
                return true;
            }
            return false;
        }

        private void reconfigurePipeline() {
            if (XMLNSDocumentScannerImpl.this.fDTDValidator != null) {
                if (!XMLNSDocumentScannerImpl.this.fDTDValidator.hasGrammar()) {
                    XMLNSDocumentScannerImpl.this.fBindNamespaces = true;
                    XMLNSDocumentScannerImpl.this.fPerformValidation = XMLNSDocumentScannerImpl.this.fDTDValidator.validate();
                    XMLDocumentSource source = XMLNSDocumentScannerImpl.this.fDTDValidator.getDocumentSource();
                    XMLDocumentHandler handler = XMLNSDocumentScannerImpl.this.fDTDValidator.getDocumentHandler();
                    source.setDocumentHandler(handler);
                    if (handler != null) {
                        handler.setDocumentSource(source);
                    }
                    XMLNSDocumentScannerImpl.this.fDTDValidator.setDocumentSource(null);
                    XMLNSDocumentScannerImpl.this.fDTDValidator.setDocumentHandler(null);
                    return;
                }
                return;
            }
            XMLNSDocumentScannerImpl.this.fBindNamespaces = true;
        }
    }
}
