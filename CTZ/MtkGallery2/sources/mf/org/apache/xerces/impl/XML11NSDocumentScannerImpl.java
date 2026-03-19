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

public class XML11NSDocumentScannerImpl extends XML11DocumentScannerImpl {
    protected boolean fBindNamespaces;
    private XMLDTDValidatorFilter fDTDValidator;
    protected boolean fPerformValidation;
    private boolean fSawSpace;

    public void setDTDValidator(XMLDTDValidatorFilter validator) {
        this.fDTDValidator = validator;
    }

    @Override
    protected boolean scanStartElement() throws IOException, XNIException {
        String prefix;
        QName name;
        String aprefix;
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
                if ((!isValidNameStartChar(c) || !sawSpace) && (!isValidNameStartHighSurrogate(c) || !sawSpace)) {
                    reportFatalError("ElementUnterminated", new Object[]{rawname});
                }
                scanAttribute(this.fAttributes);
            }
        }
        if (this.fBindNamespaces) {
            if (this.fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
                this.fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN, "ElementXMLNSPrefix", new Object[]{this.fElementQName.rawname}, (short) 2);
            }
            if (this.fElementQName.prefix != null) {
                prefix = this.fElementQName.prefix;
            } else {
                prefix = XMLSymbols.EMPTY_STRING;
            }
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
                if (this.fAttributeQName.prefix != null) {
                    aprefix = this.fAttributeQName.prefix;
                } else {
                    aprefix = XMLSymbols.EMPTY_STRING;
                }
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
        String prefix;
        QName name;
        String aprefix;
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
                if ((!isValidNameStartChar(c) || !this.fSawSpace) && (!isValidNameStartHighSurrogate(c) || !this.fSawSpace)) {
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
            if (this.fElementQName.prefix != null) {
                prefix = this.fElementQName.prefix;
            } else {
                prefix = XMLSymbols.EMPTY_STRING;
            }
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
                if (this.fAttributeQName.prefix != null) {
                    aprefix = this.fAttributeQName.prefix;
                } else {
                    aprefix = XMLSymbols.EMPTY_STRING;
                }
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
        String prefix;
        String str;
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
            if (this.fAttributeQName.prefix != null) {
                prefix = this.fAttributeQName.prefix;
            } else {
                prefix = XMLSymbols.EMPTY_STRING;
            }
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
                if (localpart != XMLSymbols.PREFIX_XMLNS) {
                    str = localpart;
                } else {
                    str = XMLSymbols.EMPTY_STRING;
                }
                String prefix2 = str;
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
        return new NS11ContentDispatcher();
    }

    protected final class NS11ContentDispatcher extends XMLDocumentScannerImpl.ContentDispatcher {
        protected NS11ContentDispatcher() {
            super();
        }

        @Override
        protected boolean scanRootElementHook() throws IOException, XNIException {
            if (XML11NSDocumentScannerImpl.this.fExternalSubsetResolver != null && !XML11NSDocumentScannerImpl.this.fSeenDoctypeDecl && !XML11NSDocumentScannerImpl.this.fDisallowDoctype && (XML11NSDocumentScannerImpl.this.fValidation || XML11NSDocumentScannerImpl.this.fLoadExternalDTD)) {
                XML11NSDocumentScannerImpl.this.scanStartElementName();
                resolveExternalSubsetAndRead();
                reconfigurePipeline();
                if (XML11NSDocumentScannerImpl.this.scanStartElementAfterName()) {
                    XML11NSDocumentScannerImpl.this.setScannerState(12);
                    XML11NSDocumentScannerImpl.this.setDispatcher(XML11NSDocumentScannerImpl.this.fTrailingMiscDispatcher);
                    return true;
                }
                return false;
            }
            reconfigurePipeline();
            if (XML11NSDocumentScannerImpl.this.scanStartElement()) {
                XML11NSDocumentScannerImpl.this.setScannerState(12);
                XML11NSDocumentScannerImpl.this.setDispatcher(XML11NSDocumentScannerImpl.this.fTrailingMiscDispatcher);
                return true;
            }
            return false;
        }

        private void reconfigurePipeline() {
            if (XML11NSDocumentScannerImpl.this.fDTDValidator != null) {
                if (!XML11NSDocumentScannerImpl.this.fDTDValidator.hasGrammar()) {
                    XML11NSDocumentScannerImpl.this.fBindNamespaces = true;
                    XML11NSDocumentScannerImpl.this.fPerformValidation = XML11NSDocumentScannerImpl.this.fDTDValidator.validate();
                    XMLDocumentSource source = XML11NSDocumentScannerImpl.this.fDTDValidator.getDocumentSource();
                    XMLDocumentHandler handler = XML11NSDocumentScannerImpl.this.fDTDValidator.getDocumentHandler();
                    source.setDocumentHandler(handler);
                    if (handler != null) {
                        handler.setDocumentSource(source);
                    }
                    XML11NSDocumentScannerImpl.this.fDTDValidator.setDocumentSource(null);
                    XML11NSDocumentScannerImpl.this.fDTDValidator.setDocumentHandler(null);
                    return;
                }
                return;
            }
            XML11NSDocumentScannerImpl.this.fBindNamespaces = true;
        }
    }
}
