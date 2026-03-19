package mf.org.apache.xerces.impl;

import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import mf.org.apache.xerces.impl.XMLDocumentFragmentScannerImpl;
import mf.org.apache.xerces.impl.dtd.XMLDTDDescription;
import mf.org.apache.xerces.impl.io.MalformedByteSequenceException;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDTDScanner;
import mf.org.apache.xerces.xni.parser.XMLInputSource;

public class XMLDocumentScannerImpl extends XMLDocumentFragmentScannerImpl {
    protected static final int SCANNER_STATE_DTD_EXTERNAL = 18;
    protected static final int SCANNER_STATE_DTD_EXTERNAL_DECLS = 19;
    protected static final int SCANNER_STATE_DTD_INTERNAL_DECLS = 17;
    protected static final int SCANNER_STATE_PROLOG = 5;
    protected static final int SCANNER_STATE_TRAILING_MISC = 12;
    protected static final int SCANNER_STATE_XML_DECL = 0;
    protected XMLDTDScanner fDTDScanner;
    protected String fDoctypeName;
    protected String fDoctypePublicId;
    protected String fDoctypeSystemId;
    protected boolean fScanningDTD;
    protected boolean fSeenDoctypeDecl;
    protected ValidationManager fValidationManager;
    protected static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    protected static final String DISALLOW_DOCTYPE_DECL_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String[] RECOGNIZED_FEATURES = {LOAD_EXTERNAL_DTD, DISALLOW_DOCTYPE_DECL_FEATURE};
    private static final Boolean[] FEATURE_DEFAULTS = {Boolean.TRUE, Boolean.FALSE};
    protected static final String DTD_SCANNER = "http://apache.org/xml/properties/internal/dtd-scanner";
    protected static final String VALIDATION_MANAGER = "http://apache.org/xml/properties/internal/validation-manager";
    protected static final String NAMESPACE_CONTEXT = "http://apache.org/xml/properties/internal/namespace-context";
    private static final String[] RECOGNIZED_PROPERTIES = {DTD_SCANNER, VALIDATION_MANAGER, NAMESPACE_CONTEXT};
    private static final Object[] PROPERTY_DEFAULTS = new Object[3];
    protected NamespaceContext fNamespaceContext = new NamespaceSupport();
    protected boolean fLoadExternalDTD = true;
    protected boolean fDisallowDoctype = false;
    protected final XMLDocumentFragmentScannerImpl.Dispatcher fXMLDeclDispatcher = new XMLDeclDispatcher();
    protected final XMLDocumentFragmentScannerImpl.Dispatcher fPrologDispatcher = new PrologDispatcher();
    protected final XMLDocumentFragmentScannerImpl.Dispatcher fDTDDispatcher = new DTDDispatcher();
    protected final XMLDocumentFragmentScannerImpl.Dispatcher fTrailingMiscDispatcher = new TrailingMiscDispatcher();
    private final String[] fStrings = new String[3];
    private final XMLString fString = new XMLString();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();
    private XMLInputSource fExternalSubsetSource = null;
    private final XMLDTDDescription fDTDDescription = new XMLDTDDescription(null, null, null, null, null);

    @Override
    public void setInputSource(XMLInputSource inputSource) throws IOException {
        this.fEntityManager.setEntityHandler(this);
        this.fEntityManager.startDocumentEntity(inputSource);
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        super.reset(componentManager);
        this.fDoctypeName = null;
        this.fDoctypePublicId = null;
        this.fDoctypeSystemId = null;
        this.fSeenDoctypeDecl = false;
        this.fScanningDTD = false;
        this.fExternalSubsetSource = null;
        if (!this.fParserSettings) {
            this.fNamespaceContext.reset();
            setScannerState(0);
            setDispatcher(this.fXMLDeclDispatcher);
            return;
        }
        try {
            this.fLoadExternalDTD = componentManager.getFeature(LOAD_EXTERNAL_DTD);
        } catch (XMLConfigurationException e) {
            this.fLoadExternalDTD = true;
        }
        try {
            this.fDisallowDoctype = componentManager.getFeature(DISALLOW_DOCTYPE_DECL_FEATURE);
        } catch (XMLConfigurationException e2) {
            this.fDisallowDoctype = false;
        }
        this.fDTDScanner = (XMLDTDScanner) componentManager.getProperty(DTD_SCANNER);
        try {
            this.fValidationManager = (ValidationManager) componentManager.getProperty(VALIDATION_MANAGER);
        } catch (XMLConfigurationException e3) {
            this.fValidationManager = null;
        }
        try {
            this.fNamespaceContext = (NamespaceContext) componentManager.getProperty(NAMESPACE_CONTEXT);
        } catch (XMLConfigurationException e4) {
        }
        if (this.fNamespaceContext == null) {
            this.fNamespaceContext = new NamespaceSupport();
        }
        this.fNamespaceContext.reset();
        setScannerState(0);
        setDispatcher(this.fXMLDeclDispatcher);
    }

    @Override
    public String[] getRecognizedFeatures() {
        String[] featureIds = super.getRecognizedFeatures();
        int length = featureIds != null ? featureIds.length : 0;
        String[] combinedFeatureIds = new String[RECOGNIZED_FEATURES.length + length];
        if (featureIds != null) {
            System.arraycopy(featureIds, 0, combinedFeatureIds, 0, featureIds.length);
        }
        System.arraycopy(RECOGNIZED_FEATURES, 0, combinedFeatureIds, length, RECOGNIZED_FEATURES.length);
        return combinedFeatureIds;
    }

    @Override
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
        super.setFeature(featureId, state);
        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();
            if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length() && featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
                this.fLoadExternalDTD = state;
            } else if (suffixLength == Constants.DISALLOW_DOCTYPE_DECL_FEATURE.length() && featureId.endsWith(Constants.DISALLOW_DOCTYPE_DECL_FEATURE)) {
                this.fDisallowDoctype = state;
            }
        }
    }

    @Override
    public String[] getRecognizedProperties() {
        String[] propertyIds = super.getRecognizedProperties();
        int length = propertyIds != null ? propertyIds.length : 0;
        String[] combinedPropertyIds = new String[RECOGNIZED_PROPERTIES.length + length];
        if (propertyIds != null) {
            System.arraycopy(propertyIds, 0, combinedPropertyIds, 0, propertyIds.length);
        }
        System.arraycopy(RECOGNIZED_PROPERTIES, 0, combinedPropertyIds, length, RECOGNIZED_PROPERTIES.length);
        return combinedPropertyIds;
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
        super.setProperty(propertyId, value);
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();
            if (suffixLength == Constants.DTD_SCANNER_PROPERTY.length() && propertyId.endsWith(Constants.DTD_SCANNER_PROPERTY)) {
                this.fDTDScanner = (XMLDTDScanner) value;
            }
            if (suffixLength == Constants.NAMESPACE_CONTEXT_PROPERTY.length() && propertyId.endsWith(Constants.NAMESPACE_CONTEXT_PROPERTY) && value != null) {
                this.fNamespaceContext = (NamespaceContext) value;
            }
        }
    }

    @Override
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return super.getFeatureDefault(featureId);
    }

    @Override
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return super.getPropertyDefault(propertyId);
    }

    @Override
    public void startEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        super.startEntity(name, identifier, encoding, augs);
        if (!name.equals("[xml]") && this.fEntityScanner.isExternal()) {
            setScannerState(16);
        }
        if (this.fDocumentHandler != null && name.equals("[xml]")) {
            this.fDocumentHandler.startDocument(this.fEntityScanner, encoding, this.fNamespaceContext, null);
        }
    }

    @Override
    public void endEntity(String name, Augmentations augs) throws XNIException {
        super.endEntity(name, augs);
        if (this.fDocumentHandler != null && name.equals("[xml]")) {
            this.fDocumentHandler.endDocument(null);
        }
    }

    @Override
    protected XMLDocumentFragmentScannerImpl.Dispatcher createContentDispatcher() {
        return new ContentDispatcher();
    }

    protected boolean scanDoctypeDecl() throws IOException, XNIException {
        if (!this.fEntityScanner.skipSpaces()) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL", null);
        }
        this.fDoctypeName = this.fEntityScanner.scanName();
        if (this.fDoctypeName == null) {
            reportFatalError("MSG_ROOT_ELEMENT_TYPE_REQUIRED", null);
        }
        if (this.fEntityScanner.skipSpaces()) {
            scanExternalID(this.fStrings, false);
            this.fDoctypeSystemId = this.fStrings[0];
            this.fDoctypePublicId = this.fStrings[1];
            this.fEntityScanner.skipSpaces();
        }
        this.fHasExternalDTD = this.fDoctypeSystemId != null;
        if (!this.fHasExternalDTD && this.fExternalSubsetResolver != null) {
            this.fDTDDescription.setValues(null, null, this.fEntityManager.getCurrentResourceIdentifier().getExpandedSystemId(), null);
            this.fDTDDescription.setRootName(this.fDoctypeName);
            this.fExternalSubsetSource = this.fExternalSubsetResolver.getExternalSubset(this.fDTDDescription);
            this.fHasExternalDTD = this.fExternalSubsetSource != null;
        }
        if (this.fDocumentHandler != null) {
            if (this.fExternalSubsetSource == null) {
                this.fDocumentHandler.doctypeDecl(this.fDoctypeName, this.fDoctypePublicId, this.fDoctypeSystemId, null);
            } else {
                this.fDocumentHandler.doctypeDecl(this.fDoctypeName, this.fExternalSubsetSource.getPublicId(), this.fExternalSubsetSource.getSystemId(), null);
            }
        }
        boolean internalSubset = true;
        if (!this.fEntityScanner.skipChar(91)) {
            internalSubset = false;
            this.fEntityScanner.skipSpaces();
            if (!this.fEntityScanner.skipChar(62)) {
                reportFatalError("DoctypedeclUnterminated", new Object[]{this.fDoctypeName});
            }
            this.fMarkupDepth--;
        }
        return internalSubset;
    }

    @Override
    protected String getScannerStateName(int state) {
        if (state == 0) {
            return "SCANNER_STATE_XML_DECL";
        }
        if (state == 5) {
            return "SCANNER_STATE_PROLOG";
        }
        if (state == 12) {
            return "SCANNER_STATE_TRAILING_MISC";
        }
        switch (state) {
            case 17:
                return "SCANNER_STATE_DTD_INTERNAL_DECLS";
            case 18:
                return "SCANNER_STATE_DTD_EXTERNAL";
            case 19:
                return "SCANNER_STATE_DTD_EXTERNAL_DECLS";
            default:
                return super.getScannerStateName(state);
        }
    }

    protected final class XMLDeclDispatcher implements XMLDocumentFragmentScannerImpl.Dispatcher {
        protected XMLDeclDispatcher() {
        }

        @Override
        public boolean dispatch(boolean complete) throws IOException, XNIException {
            XMLDocumentScannerImpl.this.setScannerState(5);
            XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fPrologDispatcher);
            try {
                if (XMLDocumentScannerImpl.this.fEntityScanner.skipString("<?xml")) {
                    XMLDocumentScannerImpl.this.fMarkupDepth++;
                    if (XMLChar.isName(XMLDocumentScannerImpl.this.fEntityScanner.peekChar())) {
                        XMLDocumentScannerImpl.this.fStringBuffer.clear();
                        XMLDocumentScannerImpl.this.fStringBuffer.append("xml");
                        if (XMLDocumentScannerImpl.this.fNamespaces) {
                            while (XMLChar.isNCName(XMLDocumentScannerImpl.this.fEntityScanner.peekChar())) {
                                XMLDocumentScannerImpl.this.fStringBuffer.append((char) XMLDocumentScannerImpl.this.fEntityScanner.scanChar());
                            }
                        } else {
                            while (XMLChar.isName(XMLDocumentScannerImpl.this.fEntityScanner.peekChar())) {
                                XMLDocumentScannerImpl.this.fStringBuffer.append((char) XMLDocumentScannerImpl.this.fEntityScanner.scanChar());
                            }
                        }
                        String target = XMLDocumentScannerImpl.this.fSymbolTable.addSymbol(XMLDocumentScannerImpl.this.fStringBuffer.ch, XMLDocumentScannerImpl.this.fStringBuffer.offset, XMLDocumentScannerImpl.this.fStringBuffer.length);
                        XMLDocumentScannerImpl.this.scanPIData(target, XMLDocumentScannerImpl.this.fString);
                    } else {
                        XMLDocumentScannerImpl.this.scanXMLDeclOrTextDecl(false);
                    }
                }
                XMLDocumentScannerImpl.this.fEntityManager.fCurrentEntity.mayReadChunks = true;
                return true;
            } catch (EOFException e) {
                XMLDocumentScannerImpl.this.reportFatalError("PrematureEOF", null);
                return false;
            } catch (MalformedByteSequenceException e2) {
                XMLDocumentScannerImpl.this.fErrorReporter.reportError(e2.getDomain(), e2.getKey(), e2.getArguments(), (short) 2, (Exception) e2);
                return false;
            } catch (CharConversionException e3) {
                XMLDocumentScannerImpl.this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "CharConversionFailure", (Object[]) null, (short) 2, (Exception) e3);
                return false;
            }
        }
    }

    protected final class PrologDispatcher implements XMLDocumentFragmentScannerImpl.Dispatcher {
        protected PrologDispatcher() {
        }

        @Override
        public boolean dispatch(boolean complete) throws IOException, XNIException {
            while (true) {
                boolean again = false;
                try {
                    switch (XMLDocumentScannerImpl.this.fScannerState) {
                        case 1:
                            XMLDocumentScannerImpl.this.fMarkupDepth++;
                            if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(33)) {
                                if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(45)) {
                                    if (!XMLDocumentScannerImpl.this.fEntityScanner.skipChar(45)) {
                                        XMLDocumentScannerImpl.this.reportFatalError("InvalidCommentStart", null);
                                    }
                                    XMLDocumentScannerImpl.this.setScannerState(2);
                                    again = true;
                                } else if (XMLDocumentScannerImpl.this.fEntityScanner.skipString("DOCTYPE")) {
                                    XMLDocumentScannerImpl.this.setScannerState(4);
                                    again = true;
                                } else {
                                    XMLDocumentScannerImpl.this.reportFatalError("MarkupNotRecognizedInProlog", null);
                                }
                            } else {
                                if (XMLDocumentScannerImpl.this.isValidNameStartChar(XMLDocumentScannerImpl.this.fEntityScanner.peekChar())) {
                                    XMLDocumentScannerImpl.this.setScannerState(6);
                                    XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fContentDispatcher);
                                } else if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(63)) {
                                    XMLDocumentScannerImpl.this.setScannerState(3);
                                    again = true;
                                } else if (XMLDocumentScannerImpl.this.isValidNameStartHighSurrogate(XMLDocumentScannerImpl.this.fEntityScanner.peekChar())) {
                                    XMLDocumentScannerImpl.this.setScannerState(6);
                                    XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fContentDispatcher);
                                } else {
                                    XMLDocumentScannerImpl.this.reportFatalError("MarkupNotRecognizedInProlog", null);
                                }
                                break;
                            }
                            if (!complete && !again) {
                                if (complete) {
                                    if (XMLDocumentScannerImpl.this.fEntityScanner.scanChar() != 60) {
                                        XMLDocumentScannerImpl.this.reportFatalError("RootElementRequired", null);
                                    }
                                    XMLDocumentScannerImpl.this.setScannerState(6);
                                    XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fContentDispatcher);
                                }
                            }
                            break;
                        case 2:
                            XMLDocumentScannerImpl.this.scanComment();
                            XMLDocumentScannerImpl.this.setScannerState(5);
                            if (!complete) {
                            }
                            break;
                        case 3:
                            XMLDocumentScannerImpl.this.scanPI();
                            XMLDocumentScannerImpl.this.setScannerState(5);
                            if (!complete) {
                            }
                            break;
                        case 4:
                            if (XMLDocumentScannerImpl.this.fDisallowDoctype) {
                                XMLDocumentScannerImpl.this.reportFatalError("DoctypeNotAllowed", null);
                            }
                            if (XMLDocumentScannerImpl.this.fSeenDoctypeDecl) {
                                XMLDocumentScannerImpl.this.reportFatalError("AlreadySeenDoctype", null);
                            }
                            XMLDocumentScannerImpl.this.fSeenDoctypeDecl = true;
                            if (XMLDocumentScannerImpl.this.scanDoctypeDecl()) {
                                XMLDocumentScannerImpl.this.setScannerState(17);
                                XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fDTDDispatcher);
                            } else {
                                if (XMLDocumentScannerImpl.this.fDoctypeSystemId == null) {
                                    if (XMLDocumentScannerImpl.this.fExternalSubsetSource != null) {
                                        XMLDocumentScannerImpl.this.fIsEntityDeclaredVC = !XMLDocumentScannerImpl.this.fStandalone;
                                        if ((XMLDocumentScannerImpl.this.fValidation || XMLDocumentScannerImpl.this.fLoadExternalDTD) && (XMLDocumentScannerImpl.this.fValidationManager == null || !XMLDocumentScannerImpl.this.fValidationManager.isCachedDTD())) {
                                        }
                                    }
                                } else {
                                    XMLDocumentScannerImpl.this.fIsEntityDeclaredVC = !XMLDocumentScannerImpl.this.fStandalone;
                                    if ((XMLDocumentScannerImpl.this.fValidation || XMLDocumentScannerImpl.this.fLoadExternalDTD) && (XMLDocumentScannerImpl.this.fValidationManager == null || !XMLDocumentScannerImpl.this.fValidationManager.isCachedDTD())) {
                                    }
                                }
                                XMLDocumentScannerImpl.this.fDTDScanner.setInputSource(null);
                                XMLDocumentScannerImpl.this.setScannerState(5);
                                if (!complete) {
                                }
                            }
                            break;
                        case 5:
                            XMLDocumentScannerImpl.this.fEntityScanner.skipSpaces();
                            if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(60)) {
                                XMLDocumentScannerImpl.this.setScannerState(1);
                                again = true;
                            } else if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(38)) {
                                XMLDocumentScannerImpl.this.setScannerState(8);
                                again = true;
                            } else {
                                XMLDocumentScannerImpl.this.setScannerState(7);
                                again = true;
                            }
                            if (!complete) {
                            }
                            break;
                        case 6:
                        default:
                            if (!complete) {
                            }
                            break;
                        case 7:
                            XMLDocumentScannerImpl.this.reportFatalError("ContentIllegalInProlog", null);
                            XMLDocumentScannerImpl.this.fEntityScanner.scanChar();
                        case 8:
                            XMLDocumentScannerImpl.this.reportFatalError("ReferenceIllegalInProlog", null);
                            if (!complete) {
                            }
                            break;
                    }
                    return true;
                } catch (CharConversionException e) {
                    XMLDocumentScannerImpl.this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "CharConversionFailure", (Object[]) null, (short) 2, (Exception) e);
                    return false;
                } catch (EOFException e2) {
                    XMLDocumentScannerImpl.this.reportFatalError("PrematureEOF", null);
                    return false;
                } catch (MalformedByteSequenceException e3) {
                    XMLDocumentScannerImpl.this.fErrorReporter.reportError(e3.getDomain(), e3.getKey(), e3.getArguments(), (short) 2, (Exception) e3);
                    return false;
                }
            }
        }
    }

    protected final class DTDDispatcher implements XMLDocumentFragmentScannerImpl.Dispatcher {
        protected DTDDispatcher() {
        }

        @Override
        public boolean dispatch(boolean complete) throws IOException, XNIException {
            XMLDocumentScannerImpl.this.fEntityManager.setEntityHandler(null);
            while (true) {
                boolean again = false;
                try {
                    switch (XMLDocumentScannerImpl.this.fScannerState) {
                        case 17:
                            boolean readExternalSubset = (XMLDocumentScannerImpl.this.fValidation || XMLDocumentScannerImpl.this.fLoadExternalDTD) && (XMLDocumentScannerImpl.this.fValidationManager == null || !XMLDocumentScannerImpl.this.fValidationManager.isCachedDTD());
                            boolean moreToScan = XMLDocumentScannerImpl.this.fDTDScanner.scanDTDInternalSubset(true, XMLDocumentScannerImpl.this.fStandalone, XMLDocumentScannerImpl.this.fHasExternalDTD && readExternalSubset);
                            if (!moreToScan) {
                                if (!XMLDocumentScannerImpl.this.fEntityScanner.skipChar(93)) {
                                    XMLDocumentScannerImpl.this.reportFatalError("EXPECTED_SQUARE_BRACKET_TO_CLOSE_INTERNAL_SUBSET", null);
                                }
                                XMLDocumentScannerImpl.this.fEntityScanner.skipSpaces();
                                if (!XMLDocumentScannerImpl.this.fEntityScanner.skipChar(62)) {
                                    XMLDocumentScannerImpl.this.reportFatalError("DoctypedeclUnterminated", new Object[]{XMLDocumentScannerImpl.this.fDoctypeName});
                                }
                                XMLDocumentScannerImpl.this.fMarkupDepth--;
                                if (XMLDocumentScannerImpl.this.fDoctypeSystemId != null) {
                                    XMLDocumentScannerImpl.this.fIsEntityDeclaredVC = !XMLDocumentScannerImpl.this.fStandalone;
                                    if (readExternalSubset) {
                                        XMLDocumentScannerImpl.this.setScannerState(18);
                                    }
                                } else if (XMLDocumentScannerImpl.this.fExternalSubsetSource == null) {
                                    XMLDocumentScannerImpl.this.fIsEntityDeclaredVC = XMLDocumentScannerImpl.this.fEntityManager.hasPEReferences() && !XMLDocumentScannerImpl.this.fStandalone;
                                } else {
                                    XMLDocumentScannerImpl.this.fIsEntityDeclaredVC = !XMLDocumentScannerImpl.this.fStandalone;
                                    if (readExternalSubset) {
                                        XMLDocumentScannerImpl.this.fDTDScanner.setInputSource(XMLDocumentScannerImpl.this.fExternalSubsetSource);
                                        XMLDocumentScannerImpl.this.fExternalSubsetSource = null;
                                        XMLDocumentScannerImpl.this.setScannerState(19);
                                    }
                                }
                            }
                            break;
                        case 18:
                            XMLDocumentScannerImpl.this.fDTDDescription.setValues(XMLDocumentScannerImpl.this.fDoctypePublicId, XMLDocumentScannerImpl.this.fDoctypeSystemId, null, null);
                            XMLDocumentScannerImpl.this.fDTDDescription.setRootName(XMLDocumentScannerImpl.this.fDoctypeName);
                            XMLInputSource xmlInputSource = XMLDocumentScannerImpl.this.fEntityManager.resolveEntity(XMLDocumentScannerImpl.this.fDTDDescription);
                            XMLDocumentScannerImpl.this.fDTDScanner.setInputSource(xmlInputSource);
                            XMLDocumentScannerImpl.this.setScannerState(19);
                            again = true;
                            break;
                        case 19:
                            boolean moreToScan2 = XMLDocumentScannerImpl.this.fDTDScanner.scanDTDExternalSubset(true);
                            if (!moreToScan2) {
                                XMLDocumentScannerImpl.this.setScannerState(5);
                                XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fPrologDispatcher);
                                XMLDocumentScannerImpl.this.fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this);
                                return true;
                            }
                            break;
                        default:
                            throw new XNIException("DTDDispatcher#dispatch: scanner state=" + XMLDocumentScannerImpl.this.fScannerState + " (" + XMLDocumentScannerImpl.this.getScannerStateName(XMLDocumentScannerImpl.this.fScannerState) + ')');
                    }
                    if (!complete && !again) {
                        return true;
                    }
                } catch (EOFException e) {
                    XMLDocumentScannerImpl.this.reportFatalError("PrematureEOF", null);
                    return false;
                } catch (MalformedByteSequenceException e2) {
                    XMLDocumentScannerImpl.this.fErrorReporter.reportError(e2.getDomain(), e2.getKey(), e2.getArguments(), (short) 2, (Exception) e2);
                    return false;
                } catch (CharConversionException e3) {
                    XMLDocumentScannerImpl.this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "CharConversionFailure", (Object[]) null, (short) 2, (Exception) e3);
                    return false;
                } finally {
                    XMLDocumentScannerImpl.this.fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this);
                }
            }
        }
    }

    protected class ContentDispatcher extends XMLDocumentFragmentScannerImpl.FragmentContentDispatcher {
        protected ContentDispatcher() {
            super();
        }

        @Override
        protected boolean scanForDoctypeHook() throws IOException, XNIException {
            if (XMLDocumentScannerImpl.this.fEntityScanner.skipString("DOCTYPE")) {
                XMLDocumentScannerImpl.this.setScannerState(4);
                return true;
            }
            return false;
        }

        @Override
        protected boolean elementDepthIsZeroHook() throws IOException, XNIException {
            XMLDocumentScannerImpl.this.setScannerState(12);
            XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fTrailingMiscDispatcher);
            return true;
        }

        @Override
        protected boolean scanRootElementHook() throws IOException, XNIException {
            if (XMLDocumentScannerImpl.this.fExternalSubsetResolver != null && !XMLDocumentScannerImpl.this.fSeenDoctypeDecl && !XMLDocumentScannerImpl.this.fDisallowDoctype && (XMLDocumentScannerImpl.this.fValidation || XMLDocumentScannerImpl.this.fLoadExternalDTD)) {
                XMLDocumentScannerImpl.this.scanStartElementName();
                resolveExternalSubsetAndRead();
                if (XMLDocumentScannerImpl.this.scanStartElementAfterName()) {
                    XMLDocumentScannerImpl.this.setScannerState(12);
                    XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fTrailingMiscDispatcher);
                    return true;
                }
                return false;
            }
            if (XMLDocumentScannerImpl.this.scanStartElement()) {
                XMLDocumentScannerImpl.this.setScannerState(12);
                XMLDocumentScannerImpl.this.setDispatcher(XMLDocumentScannerImpl.this.fTrailingMiscDispatcher);
                return true;
            }
            return false;
        }

        @Override
        protected void endOfFileHook(EOFException e) throws IOException, XNIException {
            XMLDocumentScannerImpl.this.reportFatalError("PrematureEOF", null);
        }

        protected void resolveExternalSubsetAndRead() throws IOException, XNIException {
            XMLDocumentScannerImpl.this.fDTDDescription.setValues(null, null, XMLDocumentScannerImpl.this.fEntityManager.getCurrentResourceIdentifier().getExpandedSystemId(), null);
            XMLDocumentScannerImpl.this.fDTDDescription.setRootName(XMLDocumentScannerImpl.this.fElementQName.rawname);
            XMLInputSource src = XMLDocumentScannerImpl.this.fExternalSubsetResolver.getExternalSubset(XMLDocumentScannerImpl.this.fDTDDescription);
            if (src != null) {
                XMLDocumentScannerImpl.this.fDoctypeName = XMLDocumentScannerImpl.this.fElementQName.rawname;
                XMLDocumentScannerImpl.this.fDoctypePublicId = src.getPublicId();
                XMLDocumentScannerImpl.this.fDoctypeSystemId = src.getSystemId();
                if (XMLDocumentScannerImpl.this.fDocumentHandler != null) {
                    XMLDocumentScannerImpl.this.fDocumentHandler.doctypeDecl(XMLDocumentScannerImpl.this.fDoctypeName, XMLDocumentScannerImpl.this.fDoctypePublicId, XMLDocumentScannerImpl.this.fDoctypeSystemId, null);
                }
                try {
                    if (XMLDocumentScannerImpl.this.fValidationManager == null || !XMLDocumentScannerImpl.this.fValidationManager.isCachedDTD()) {
                        XMLDocumentScannerImpl.this.fDTDScanner.setInputSource(src);
                        while (XMLDocumentScannerImpl.this.fDTDScanner.scanDTDExternalSubset(true)) {
                        }
                    } else {
                        XMLDocumentScannerImpl.this.fDTDScanner.setInputSource(null);
                    }
                } finally {
                    XMLDocumentScannerImpl.this.fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this);
                }
            }
        }
    }

    protected final class TrailingMiscDispatcher implements XMLDocumentFragmentScannerImpl.Dispatcher {
        protected TrailingMiscDispatcher() {
        }

        @Override
        public boolean dispatch(boolean complete) throws IOException, XNIException {
            while (true) {
                boolean again = false;
                try {
                    int i = XMLDocumentScannerImpl.this.fScannerState;
                    if (i != 12) {
                        if (i == 14) {
                            return false;
                        }
                        switch (i) {
                            case 1:
                                XMLDocumentScannerImpl.this.fMarkupDepth++;
                                if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(63)) {
                                    XMLDocumentScannerImpl.this.setScannerState(3);
                                    again = true;
                                } else if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(33)) {
                                    XMLDocumentScannerImpl.this.setScannerState(2);
                                    again = true;
                                } else if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(47)) {
                                    XMLDocumentScannerImpl.this.reportFatalError("MarkupNotRecognizedInMisc", null);
                                    again = true;
                                } else if (XMLDocumentScannerImpl.this.isValidNameStartChar(XMLDocumentScannerImpl.this.fEntityScanner.peekChar()) || XMLDocumentScannerImpl.this.isValidNameStartHighSurrogate(XMLDocumentScannerImpl.this.fEntityScanner.peekChar())) {
                                    XMLDocumentScannerImpl.this.reportFatalError("MarkupNotRecognizedInMisc", null);
                                    XMLDocumentScannerImpl.this.scanStartElement();
                                    XMLDocumentScannerImpl.this.setScannerState(7);
                                } else {
                                    XMLDocumentScannerImpl.this.reportFatalError("MarkupNotRecognizedInMisc", null);
                                }
                                break;
                            case 2:
                                if (!XMLDocumentScannerImpl.this.fEntityScanner.skipString("--")) {
                                    XMLDocumentScannerImpl.this.reportFatalError("InvalidCommentStart", null);
                                }
                                XMLDocumentScannerImpl.this.scanComment();
                                XMLDocumentScannerImpl.this.setScannerState(12);
                                break;
                            case 3:
                                XMLDocumentScannerImpl.this.scanPI();
                                XMLDocumentScannerImpl.this.setScannerState(12);
                                break;
                            default:
                                switch (i) {
                                    case 7:
                                        int ch = XMLDocumentScannerImpl.this.fEntityScanner.peekChar();
                                        if (ch == -1) {
                                            XMLDocumentScannerImpl.this.setScannerState(14);
                                        } else {
                                            XMLDocumentScannerImpl.this.reportFatalError("ContentIllegalInTrailingMisc", null);
                                            XMLDocumentScannerImpl.this.fEntityScanner.scanChar();
                                            XMLDocumentScannerImpl.this.setScannerState(12);
                                        }
                                        break;
                                    case 8:
                                        XMLDocumentScannerImpl.this.reportFatalError("ReferenceIllegalInTrailingMisc", null);
                                        XMLDocumentScannerImpl.this.setScannerState(12);
                                        break;
                                }
                                break;
                        }
                        return false;
                    }
                    XMLDocumentScannerImpl.this.fEntityScanner.skipSpaces();
                    if (XMLDocumentScannerImpl.this.fEntityScanner.skipChar(60)) {
                        XMLDocumentScannerImpl.this.setScannerState(1);
                        again = true;
                    } else {
                        XMLDocumentScannerImpl.this.setScannerState(7);
                        again = true;
                    }
                    if (!complete && !again) {
                        return true;
                    }
                } catch (MalformedByteSequenceException e) {
                    XMLDocumentScannerImpl.this.fErrorReporter.reportError(e.getDomain(), e.getKey(), e.getArguments(), (short) 2, (Exception) e);
                    return false;
                } catch (CharConversionException e2) {
                    XMLDocumentScannerImpl.this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "CharConversionFailure", (Object[]) null, (short) 2, (Exception) e2);
                    return false;
                } catch (EOFException e3) {
                    if (XMLDocumentScannerImpl.this.fMarkupDepth == 0) {
                        XMLDocumentScannerImpl.this.setScannerState(14);
                        return false;
                    }
                    XMLDocumentScannerImpl.this.reportFatalError("PrematureEOF", null);
                    return false;
                }
            }
        }
    }
}
