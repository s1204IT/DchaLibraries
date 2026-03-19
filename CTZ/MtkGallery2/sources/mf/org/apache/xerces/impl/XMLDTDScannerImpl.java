package mf.org.apache.xerces.impl;

import java.io.IOException;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLStringBuffer;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.XMLDTDContentModelHandler;
import mf.org.apache.xerces.xni.XMLDTDHandler;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDTDScanner;
import mf.org.apache.xerces.xni.parser.XMLInputSource;

public class XMLDTDScannerImpl extends XMLScanner implements XMLEntityHandler, XMLComponent, XMLDTDScanner {
    private static final boolean DEBUG_SCANNER_STATE = false;
    private static final Boolean[] FEATURE_DEFAULTS;
    private static final Object[] PROPERTY_DEFAULTS;
    private static final String[] RECOGNIZED_FEATURES = {"http://xml.org/sax/features/validation", "http://apache.org/xml/features/scanner/notify-char-refs"};
    private static final String[] RECOGNIZED_PROPERTIES;
    protected static final int SCANNER_STATE_END_OF_INPUT = 0;
    protected static final int SCANNER_STATE_MARKUP_DECL = 2;
    protected static final int SCANNER_STATE_TEXT_DECL = 1;
    private int fContentDepth;
    private int[] fContentStack;
    protected XMLDTDContentModelHandler fDTDContentModelHandler;
    protected XMLDTDHandler fDTDHandler;
    private String[] fEnumeration;
    private int fEnumerationCount;
    private int fExtEntityDepth;
    private final XMLStringBuffer fIgnoreConditionalBuffer;
    private int fIncludeSectDepth;
    private final XMLString fLiteral;
    private final XMLString fLiteral2;
    private int fMarkUpDepth;
    private int fPEDepth;
    private boolean[] fPEReport;
    private int[] fPEStack;
    protected int fScannerState;
    protected boolean fSeenExternalDTD;
    protected boolean fSeenPEReferences;
    protected boolean fStandalone;
    private boolean fStartDTDCalled;
    private final XMLString fString;
    private final XMLStringBuffer fStringBuffer;
    private final XMLStringBuffer fStringBuffer2;
    private final String[] fStrings;

    static {
        Boolean[] boolArr = new Boolean[2];
        boolArr[1] = Boolean.FALSE;
        FEATURE_DEFAULTS = boolArr;
        RECOGNIZED_PROPERTIES = new String[]{"http://apache.org/xml/properties/internal/symbol-table", "http://apache.org/xml/properties/internal/error-reporter", "http://apache.org/xml/properties/internal/entity-manager"};
        PROPERTY_DEFAULTS = new Object[3];
    }

    public XMLDTDScannerImpl() {
        this.fContentStack = new int[5];
        this.fPEStack = new int[5];
        this.fPEReport = new boolean[5];
        this.fStrings = new String[3];
        this.fString = new XMLString();
        this.fStringBuffer = new XMLStringBuffer();
        this.fStringBuffer2 = new XMLStringBuffer();
        this.fLiteral = new XMLString();
        this.fLiteral2 = new XMLString();
        this.fEnumeration = new String[5];
        this.fIgnoreConditionalBuffer = new XMLStringBuffer(128);
    }

    public XMLDTDScannerImpl(SymbolTable symbolTable, XMLErrorReporter errorReporter, XMLEntityManager entityManager) {
        this.fContentStack = new int[5];
        this.fPEStack = new int[5];
        this.fPEReport = new boolean[5];
        this.fStrings = new String[3];
        this.fString = new XMLString();
        this.fStringBuffer = new XMLStringBuffer();
        this.fStringBuffer2 = new XMLStringBuffer();
        this.fLiteral = new XMLString();
        this.fLiteral2 = new XMLString();
        this.fEnumeration = new String[5];
        this.fIgnoreConditionalBuffer = new XMLStringBuffer(128);
        this.fSymbolTable = symbolTable;
        this.fErrorReporter = errorReporter;
        this.fEntityManager = entityManager;
        entityManager.setProperty("http://apache.org/xml/properties/internal/symbol-table", this.fSymbolTable);
    }

    @Override
    public void setInputSource(XMLInputSource inputSource) throws IOException {
        if (inputSource == null) {
            if (this.fDTDHandler != null) {
                this.fDTDHandler.startDTD(null, null);
                this.fDTDHandler.endDTD(null);
                return;
            }
            return;
        }
        this.fEntityManager.setEntityHandler(this);
        this.fEntityManager.startDTDEntity(inputSource);
    }

    @Override
    public boolean scanDTDExternalSubset(boolean complete) throws IOException, XNIException {
        this.fEntityManager.setEntityHandler(this);
        if (this.fScannerState == 1) {
            this.fSeenExternalDTD = true;
            boolean textDecl = scanTextDecl();
            if (this.fScannerState == 0) {
                return false;
            }
            setScannerState(2);
            if (textDecl && !complete) {
                return true;
            }
        }
        while (textDecl) {
            if (!complete) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean scanDTDInternalSubset(boolean complete, boolean standalone, boolean hasExternalSubset) throws IOException, XNIException {
        this.fEntityScanner = this.fEntityManager.getEntityScanner();
        this.fEntityManager.setEntityHandler(this);
        this.fStandalone = standalone;
        if (this.fScannerState == 1) {
            if (this.fDTDHandler != null) {
                this.fDTDHandler.startDTD(this.fEntityScanner, null);
                this.fStartDTDCalled = true;
            }
            setScannerState(2);
        }
        while (scanDecls(complete)) {
            if (!complete) {
                return true;
            }
        }
        if (this.fDTDHandler != null && !hasExternalSubset) {
            this.fDTDHandler.endDTD(null);
        }
        setScannerState(1);
        return false;
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        super.reset(componentManager);
        init();
    }

    @Override
    public void reset() {
        super.reset();
        init();
    }

    @Override
    public String[] getRecognizedFeatures() {
        return (String[]) RECOGNIZED_FEATURES.clone();
    }

    @Override
    public String[] getRecognizedProperties() {
        return (String[]) RECOGNIZED_PROPERTIES.clone();
    }

    @Override
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    }

    @Override
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    }

    @Override
    public void setDTDHandler(XMLDTDHandler dtdHandler) {
        this.fDTDHandler = dtdHandler;
    }

    @Override
    public XMLDTDHandler getDTDHandler() {
        return this.fDTDHandler;
    }

    @Override
    public void setDTDContentModelHandler(XMLDTDContentModelHandler dtdContentModelHandler) {
        this.fDTDContentModelHandler = dtdContentModelHandler;
    }

    @Override
    public XMLDTDContentModelHandler getDTDContentModelHandler() {
        return this.fDTDContentModelHandler;
    }

    @Override
    public void startEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        super.startEntity(name, identifier, encoding, augs);
        boolean dtdEntity = name.equals("[dtd]");
        if (dtdEntity) {
            if (this.fDTDHandler != null && !this.fStartDTDCalled) {
                this.fDTDHandler.startDTD(this.fEntityScanner, null);
            }
            if (this.fDTDHandler != null) {
                this.fDTDHandler.startExternalSubset(identifier, null);
            }
            this.fEntityManager.startExternalSubset();
            this.fExtEntityDepth++;
        } else if (name.charAt(0) == '%') {
            pushPEStack(this.fMarkUpDepth, this.fReportEntity);
            if (this.fEntityScanner.isExternal()) {
                this.fExtEntityDepth++;
            }
        }
        if (this.fDTDHandler != null && !dtdEntity && this.fReportEntity) {
            this.fDTDHandler.startParameterEntity(name, identifier, encoding, augs);
        }
    }

    @Override
    public void endEntity(String name, Augmentations augs) throws XNIException {
        super.endEntity(name, augs);
        if (this.fScannerState == 0) {
            return;
        }
        boolean z = this.fReportEntity;
        if (name.startsWith("%")) {
            boolean reportEntity = peekReportEntity();
            int startMarkUpDepth = popPEStack();
            if (startMarkUpDepth == 0 && startMarkUpDepth < this.fMarkUpDepth) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "ILL_FORMED_PARAMETER_ENTITY_WHEN_USED_IN_DECL", new Object[]{this.fEntityManager.fCurrentEntity.name}, (short) 2);
            }
            if (startMarkUpDepth != this.fMarkUpDepth) {
                reportEntity = false;
                if (this.fValidation) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "ImproperDeclarationNesting", new Object[]{name}, (short) 1);
                }
            }
            if (this.fEntityScanner.isExternal()) {
                this.fExtEntityDepth--;
            }
            if (this.fDTDHandler != null && reportEntity) {
                this.fDTDHandler.endParameterEntity(name, augs);
                return;
            }
            return;
        }
        if (name.equals("[dtd]")) {
            if (this.fIncludeSectDepth != 0) {
                reportFatalError("IncludeSectUnterminated", null);
            }
            this.fScannerState = 0;
            this.fEntityManager.endExternalSubset();
            if (this.fDTDHandler != null) {
                this.fDTDHandler.endExternalSubset(null);
                this.fDTDHandler.endDTD(null);
            }
            this.fExtEntityDepth--;
        }
    }

    protected final void setScannerState(int state) {
        this.fScannerState = state;
    }

    private static String getScannerStateName(int state) {
        return "??? (" + state + ')';
    }

    protected final boolean scanningInternalSubset() {
        return this.fExtEntityDepth == 0;
    }

    protected void startPE(String name, boolean literal) throws IOException, XNIException {
        int depth = this.fPEDepth;
        String pName = "%" + name;
        if (!this.fSeenPEReferences) {
            this.fSeenPEReferences = true;
            this.fEntityManager.notifyHasPEReferences();
        }
        if (this.fValidation && !this.fEntityManager.isDeclaredEntity(pName)) {
            this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "EntityNotDeclared", new Object[]{name}, (short) 1);
        }
        this.fEntityManager.startEntity(this.fSymbolTable.addSymbol(pName), literal);
        if (depth != this.fPEDepth && this.fEntityScanner.isExternal()) {
            scanTextDecl();
        }
    }

    protected final boolean scanTextDecl() throws IOException, XNIException {
        boolean textDecl = false;
        if (this.fEntityScanner.skipString("<?xml")) {
            this.fMarkUpDepth++;
            if (isValidNameChar(this.fEntityScanner.peekChar())) {
                this.fStringBuffer.clear();
                this.fStringBuffer.append("xml");
                if (this.fNamespaces) {
                    while (isValidNCName(this.fEntityScanner.peekChar())) {
                        this.fStringBuffer.append((char) this.fEntityScanner.scanChar());
                    }
                } else {
                    while (isValidNameChar(this.fEntityScanner.peekChar())) {
                        this.fStringBuffer.append((char) this.fEntityScanner.scanChar());
                    }
                }
                String target = this.fSymbolTable.addSymbol(this.fStringBuffer.ch, this.fStringBuffer.offset, this.fStringBuffer.length);
                scanPIData(target, this.fString);
            } else {
                scanXMLDeclOrTextDecl(true, this.fStrings);
                textDecl = true;
                this.fMarkUpDepth--;
                String version = this.fStrings[0];
                String encoding = this.fStrings[1];
                this.fEntityScanner.setXMLVersion(version);
                if (!this.fEntityScanner.fCurrentEntity.isEncodingExternallySpecified()) {
                    this.fEntityScanner.setEncoding(encoding);
                }
                if (this.fDTDHandler != null) {
                    this.fDTDHandler.textDecl(version, encoding, null);
                }
            }
        }
        this.fEntityManager.fCurrentEntity.mayReadChunks = true;
        return textDecl;
    }

    @Override
    protected final void scanPIData(String target, XMLString data) throws IOException, XNIException {
        super.scanPIData(target, data);
        this.fMarkUpDepth--;
        if (this.fDTDHandler != null) {
            this.fDTDHandler.processingInstruction(target, data, null);
        }
    }

    protected final void scanComment() throws IOException, XNIException {
        this.fReportEntity = false;
        scanComment(this.fStringBuffer);
        this.fMarkUpDepth--;
        if (this.fDTDHandler != null) {
            this.fDTDHandler.comment(this.fStringBuffer, null);
        }
        this.fReportEntity = true;
    }

    protected final void scanElementDecl() throws IOException, XNIException {
        String contentModel;
        this.fReportEntity = false;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL", null);
        }
        String name = this.fEntityScanner.scanName();
        if (name == null) {
            reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL", null);
        }
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL", new Object[]{name});
        }
        if (this.fDTDContentModelHandler != null) {
            this.fDTDContentModelHandler.startContentModel(name, null);
        }
        this.fReportEntity = true;
        if (this.fEntityScanner.skipString("EMPTY")) {
            contentModel = "EMPTY";
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.empty(null);
            }
        } else if (this.fEntityScanner.skipString("ANY")) {
            contentModel = "ANY";
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.any(null);
            }
        } else {
            if (!this.fEntityScanner.skipChar(40)) {
                reportFatalError("MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN", new Object[]{name});
            }
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.startGroup(null);
            }
            this.fStringBuffer.clear();
            this.fStringBuffer.append('(');
            this.fMarkUpDepth++;
            skipSeparator(false, !scanningInternalSubset());
            if (this.fEntityScanner.skipString("#PCDATA")) {
                scanMixed(name);
            } else {
                scanChildren(name);
            }
            contentModel = this.fStringBuffer.toString();
        }
        if (this.fDTDContentModelHandler != null) {
            this.fDTDContentModelHandler.endContentModel(null);
        }
        this.fReportEntity = false;
        skipSeparator(false, !scanningInternalSubset());
        if (!this.fEntityScanner.skipChar(62)) {
            reportFatalError("ElementDeclUnterminated", new Object[]{name});
        }
        this.fReportEntity = true;
        this.fMarkUpDepth--;
        if (this.fDTDHandler != null) {
            this.fDTDHandler.elementDecl(name, contentModel, null);
        }
    }

    private final void scanMixed(String elName) throws IOException, XNIException {
        String childName = null;
        this.fStringBuffer.append("#PCDATA");
        if (this.fDTDContentModelHandler != null) {
            this.fDTDContentModelHandler.pcdata(null);
        }
        skipSeparator(false, !scanningInternalSubset());
        while (this.fEntityScanner.skipChar(124)) {
            this.fStringBuffer.append('|');
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.separator((short) 0, null);
            }
            skipSeparator(false, !scanningInternalSubset());
            childName = this.fEntityScanner.scanName();
            if (childName == null) {
                reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT", new Object[]{elName});
            }
            this.fStringBuffer.append(childName);
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.element(childName, null);
            }
            skipSeparator(false, !scanningInternalSubset());
        }
        if (this.fEntityScanner.skipString(")*")) {
            this.fStringBuffer.append(")*");
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.endGroup(null);
                this.fDTDContentModelHandler.occurrence((short) 3, null);
            }
        } else if (childName != null) {
            reportFatalError("MixedContentUnterminated", new Object[]{elName});
        } else if (this.fEntityScanner.skipChar(41)) {
            this.fStringBuffer.append(')');
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.endGroup(null);
            }
        } else {
            reportFatalError("MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN", new Object[]{elName});
        }
        this.fMarkUpDepth--;
    }

    private final void scanChildren(String elName) throws IOException, XNIException {
        short oc;
        this.fContentDepth = 0;
        pushContentStack(0);
        int currentOp = 0;
        while (true) {
            if (!this.fEntityScanner.skipChar(40)) {
                skipSeparator(false, !scanningInternalSubset());
                String childName = this.fEntityScanner.scanName();
                if (childName == null) {
                    reportFatalError("MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN", new Object[]{elName});
                    return;
                }
                if (this.fDTDContentModelHandler != null) {
                    this.fDTDContentModelHandler.element(childName, null);
                }
                this.fStringBuffer.append(childName);
                int c = this.fEntityScanner.peekChar();
                if (c == 63 || c == 42 || c == 43) {
                    if (this.fDTDContentModelHandler != null) {
                        if (c == 63) {
                            oc = 2;
                        } else if (c == 42) {
                            oc = 3;
                        } else {
                            oc = 4;
                        }
                        this.fDTDContentModelHandler.occurrence(oc, null);
                    }
                    this.fEntityScanner.scanChar();
                    this.fStringBuffer.append((char) c);
                }
                do {
                    skipSeparator(false, !scanningInternalSubset());
                    int c2 = this.fEntityScanner.peekChar();
                    if (c2 == 44 && currentOp != 124) {
                        currentOp = c2;
                        if (this.fDTDContentModelHandler != null) {
                            this.fDTDContentModelHandler.separator((short) 1, null);
                        }
                        this.fEntityScanner.scanChar();
                        this.fStringBuffer.append(',');
                    } else if (c2 == 124 && currentOp != 44) {
                        currentOp = c2;
                        if (this.fDTDContentModelHandler != null) {
                            this.fDTDContentModelHandler.separator((short) 0, null);
                        }
                        this.fEntityScanner.scanChar();
                        this.fStringBuffer.append('|');
                    } else {
                        if (c2 != 41) {
                            reportFatalError("MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN", new Object[]{elName});
                        }
                        if (this.fDTDContentModelHandler != null) {
                            this.fDTDContentModelHandler.endGroup(null);
                        }
                        currentOp = popContentStack();
                        if (this.fEntityScanner.skipString(")?")) {
                            this.fStringBuffer.append(")?");
                            if (this.fDTDContentModelHandler != null) {
                                this.fDTDContentModelHandler.occurrence((short) 2, null);
                            }
                        } else if (this.fEntityScanner.skipString(")+")) {
                            this.fStringBuffer.append(")+");
                            if (this.fDTDContentModelHandler != null) {
                                this.fDTDContentModelHandler.occurrence((short) 4, null);
                            }
                        } else if (this.fEntityScanner.skipString(")*")) {
                            this.fStringBuffer.append(")*");
                            if (this.fDTDContentModelHandler != null) {
                                this.fDTDContentModelHandler.occurrence((short) 3, null);
                            }
                        } else {
                            this.fEntityScanner.scanChar();
                            this.fStringBuffer.append(')');
                        }
                        this.fMarkUpDepth--;
                    }
                    skipSeparator(false, !scanningInternalSubset());
                } while (this.fContentDepth != 0);
                return;
            }
            this.fMarkUpDepth++;
            this.fStringBuffer.append('(');
            if (this.fDTDContentModelHandler != null) {
                this.fDTDContentModelHandler.startGroup(null);
            }
            pushContentStack(currentOp);
            currentOp = 0;
            skipSeparator(false, !scanningInternalSubset());
        }
    }

    protected final void scanAttlistDecl() throws IOException, XNIException {
        int i;
        String elName;
        String name;
        this.fReportEntity = false;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL", null);
        }
        String elName2 = this.fEntityScanner.scanName();
        if (elName2 == null) {
            reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_ATTLISTDECL", null);
        }
        if (this.fDTDHandler != null) {
            this.fDTDHandler.startAttlist(elName2, null);
        }
        int i2 = 62;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            if (this.fEntityScanner.skipChar(62)) {
                if (this.fDTDHandler != null) {
                    this.fDTDHandler.endAttlist(null);
                }
                this.fMarkUpDepth--;
                return;
            }
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF", new Object[]{elName2});
        }
        while (!this.fEntityScanner.skipChar(i2)) {
            String name2 = this.fEntityScanner.scanName();
            if (name2 == null) {
                reportFatalError("AttNameRequiredInAttDef", new Object[]{elName2});
            }
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF", new Object[]{elName2, name2});
            }
            String type = scanAttType(elName2, name2);
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF", new Object[]{elName2, name2});
            }
            String defaultType = scanAttDefaultDecl(elName2, name2, type, this.fLiteral, this.fLiteral2);
            if (this.fDTDHandler != null) {
                String[] enumeration = null;
                if (this.fEnumerationCount != 0) {
                    enumeration = new String[this.fEnumerationCount];
                    System.arraycopy(this.fEnumeration, 0, enumeration, 0, this.fEnumerationCount);
                }
                if (defaultType != null) {
                    if (defaultType.equals("#REQUIRED") || defaultType.equals("#IMPLIED")) {
                        i = i2;
                        elName = elName2;
                        this.fDTDHandler.attributeDecl(elName2, name2, type, enumeration, defaultType, null, null, null);
                    } else {
                        name = name2;
                        i = i2;
                        elName = elName2;
                    }
                } else {
                    name = name2;
                    i = i2;
                    elName = elName2;
                }
                this.fDTDHandler.attributeDecl(elName, name, type, enumeration, defaultType, this.fLiteral, this.fLiteral2, null);
            } else {
                i = i2;
                elName = elName2;
            }
            skipSeparator(false, !scanningInternalSubset());
            i2 = i;
            elName2 = elName;
        }
        if (this.fDTDHandler != null) {
            this.fDTDHandler.endAttlist(null);
        }
        this.fMarkUpDepth--;
        this.fReportEntity = true;
    }

    private final String scanAttType(String elName, String atName) throws IOException, XNIException {
        int c;
        int c2;
        this.fEnumerationCount = 0;
        if (this.fEntityScanner.skipString("CDATA")) {
            return "CDATA";
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_IDREFS)) {
            return SchemaSymbols.ATTVAL_IDREFS;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_IDREF)) {
            return SchemaSymbols.ATTVAL_IDREF;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_ID)) {
            return SchemaSymbols.ATTVAL_ID;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_ENTITY)) {
            return SchemaSymbols.ATTVAL_ENTITY;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_ENTITIES)) {
            return SchemaSymbols.ATTVAL_ENTITIES;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_NMTOKENS)) {
            return SchemaSymbols.ATTVAL_NMTOKENS;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_NMTOKEN)) {
            return SchemaSymbols.ATTVAL_NMTOKEN;
        }
        if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_NOTATION)) {
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_AFTER_NOTATION_IN_NOTATIONTYPE", new Object[]{elName, atName});
            }
            int c3 = this.fEntityScanner.scanChar();
            if (c3 != 40) {
                reportFatalError("MSG_OPEN_PAREN_REQUIRED_IN_NOTATIONTYPE", new Object[]{elName, atName});
            }
            this.fMarkUpDepth++;
            do {
                skipSeparator(false, !scanningInternalSubset());
                String aName = this.fEntityScanner.scanName();
                if (aName == null) {
                    reportFatalError("MSG_NAME_REQUIRED_IN_NOTATIONTYPE", new Object[]{elName, atName});
                    c2 = skipInvalidEnumerationValue();
                    if (c2 != 124) {
                        break;
                    }
                } else {
                    ensureEnumerationSize(this.fEnumerationCount + 1);
                    String[] strArr = this.fEnumeration;
                    int i = this.fEnumerationCount;
                    this.fEnumerationCount = i + 1;
                    strArr[i] = aName;
                    skipSeparator(false, !scanningInternalSubset());
                    c2 = this.fEntityScanner.scanChar();
                }
            } while (c2 == 124);
            if (c2 != 41) {
                reportFatalError("NotationTypeUnterminated", new Object[]{elName, atName});
            }
            this.fMarkUpDepth--;
            return SchemaSymbols.ATTVAL_NOTATION;
        }
        int c4 = this.fEntityScanner.scanChar();
        if (c4 != 40) {
            reportFatalError("AttTypeRequiredInAttDef", new Object[]{elName, atName});
        }
        this.fMarkUpDepth++;
        do {
            skipSeparator(false, !scanningInternalSubset());
            String token = this.fEntityScanner.scanNmtoken();
            if (token == null) {
                reportFatalError("MSG_NMTOKEN_REQUIRED_IN_ENUMERATION", new Object[]{elName, atName});
                c = skipInvalidEnumerationValue();
                if (c != 124) {
                    break;
                }
            } else {
                ensureEnumerationSize(this.fEnumerationCount + 1);
                String[] strArr2 = this.fEnumeration;
                int i2 = this.fEnumerationCount;
                this.fEnumerationCount = i2 + 1;
                strArr2[i2] = token;
                skipSeparator(false, !scanningInternalSubset());
                c = this.fEntityScanner.scanChar();
            }
        } while (c == 124);
        if (c != 41) {
            reportFatalError("EnumerationUnterminated", new Object[]{elName, atName});
        }
        this.fMarkUpDepth--;
        return "ENUMERATION";
    }

    protected final String scanAttDefaultDecl(String elName, String atName, String type, XMLString defaultVal, XMLString nonNormalizedDefaultVal) throws IOException, XNIException {
        String defaultType = null;
        this.fString.clear();
        defaultVal.clear();
        if (this.fEntityScanner.skipString("#REQUIRED")) {
            return "#REQUIRED";
        }
        if (this.fEntityScanner.skipString("#IMPLIED")) {
            return "#IMPLIED";
        }
        if (this.fEntityScanner.skipString("#FIXED")) {
            defaultType = "#FIXED";
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_AFTER_FIXED_IN_DEFAULTDECL", new Object[]{elName, atName});
            }
        }
        boolean isVC = !this.fStandalone && (this.fSeenExternalDTD || this.fSeenPEReferences);
        scanAttributeValue(defaultVal, nonNormalizedDefaultVal, atName, isVC, elName);
        return defaultType;
    }

    private final void scanEntityDecl() throws IOException, XNIException {
        String name;
        String notation;
        String baseSystemId;
        boolean isPEDecl = false;
        boolean sawPERef = false;
        this.fReportEntity = false;
        if (this.fEntityScanner.skipSpaces()) {
            if (this.fEntityScanner.skipChar(37)) {
                if (skipSeparator(true, !scanningInternalSubset())) {
                    isPEDecl = true;
                } else if (scanningInternalSubset()) {
                    reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_PEDECL", null);
                    isPEDecl = true;
                } else if (this.fEntityScanner.peekChar() == 37) {
                    skipSeparator(false, !scanningInternalSubset());
                    isPEDecl = true;
                } else {
                    sawPERef = true;
                }
            } else {
                isPEDecl = false;
            }
        } else if (scanningInternalSubset() || !this.fEntityScanner.skipChar(37)) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL", null);
            isPEDecl = false;
        } else if (this.fEntityScanner.skipSpaces()) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL", null);
            isPEDecl = false;
        } else {
            sawPERef = true;
        }
        if (sawPERef) {
            while (true) {
                String peName = this.fEntityScanner.scanName();
                if (peName == null) {
                    reportFatalError("NameRequiredInPEReference", null);
                } else if (!this.fEntityScanner.skipChar(59)) {
                    reportFatalError("SemicolonRequiredInPEReference", new Object[]{peName});
                } else {
                    startPE(peName, false);
                }
                this.fEntityScanner.skipSpaces();
                if (!this.fEntityScanner.skipChar(37)) {
                    break;
                }
                if (!isPEDecl) {
                    if (skipSeparator(true, !scanningInternalSubset())) {
                        isPEDecl = true;
                        break;
                    }
                    isPEDecl = this.fEntityScanner.skipChar(37);
                }
            }
        }
        if (this.fNamespaces) {
            name = this.fEntityScanner.scanNCName();
        } else {
            name = this.fEntityScanner.scanName();
        }
        if (name == null) {
            reportFatalError("MSG_ENTITY_NAME_REQUIRED_IN_ENTITYDECL", null);
        }
        if (!skipSeparator(true, !scanningInternalSubset())) {
            if (this.fNamespaces && this.fEntityScanner.peekChar() == 58) {
                this.fEntityScanner.scanChar();
                XMLStringBuffer colonName = new XMLStringBuffer(name);
                colonName.append(':');
                String str = this.fEntityScanner.scanName();
                if (str != null) {
                    colonName.append(str);
                }
                reportFatalError("ColonNotLegalWithNS", new Object[]{colonName.toString()});
                if (!skipSeparator(true, !scanningInternalSubset())) {
                    reportFatalError("MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL", new Object[]{name});
                }
            } else {
                reportFatalError("MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL", new Object[]{name});
            }
        }
        scanExternalID(this.fStrings, false);
        String systemId = this.fStrings[0];
        String publicId = this.fStrings[1];
        String notation2 = null;
        boolean sawSpace = skipSeparator(true, !scanningInternalSubset());
        if (!isPEDecl && this.fEntityScanner.skipString("NDATA")) {
            if (!sawSpace) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL", new Object[]{name});
            }
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL", new Object[]{name});
            }
            notation2 = this.fEntityScanner.scanName();
            if (notation2 == null) {
                reportFatalError("MSG_NOTATION_NAME_REQUIRED_FOR_UNPARSED_ENTITYDECL", new Object[]{name});
            }
        }
        String notation3 = notation2;
        if (systemId == null) {
            scanEntityValue(this.fLiteral, this.fLiteral2);
            this.fStringBuffer.clear();
            this.fStringBuffer2.clear();
            this.fStringBuffer.append(this.fLiteral.ch, this.fLiteral.offset, this.fLiteral.length);
            this.fStringBuffer2.append(this.fLiteral2.ch, this.fLiteral2.offset, this.fLiteral2.length);
        }
        skipSeparator(false, !scanningInternalSubset());
        if (!this.fEntityScanner.skipChar(62)) {
            reportFatalError("EntityDeclUnterminated", new Object[]{name});
        }
        this.fMarkUpDepth--;
        if (isPEDecl) {
            name = "%" + name;
        }
        if (systemId == null) {
            this.fEntityManager.addInternalEntity(name, this.fStringBuffer.toString());
            if (this.fDTDHandler != null) {
                this.fDTDHandler.internalEntityDecl(name, this.fStringBuffer, this.fStringBuffer2, null);
            }
        } else {
            String baseSystemId2 = this.fEntityScanner.getBaseSystemId();
            if (notation3 != null) {
                notation = notation3;
                this.fEntityManager.addUnparsedEntity(name, publicId, systemId, baseSystemId2, notation3);
                baseSystemId = baseSystemId2;
            } else {
                notation = notation3;
                baseSystemId = baseSystemId2;
                this.fEntityManager.addExternalEntity(name, publicId, systemId, baseSystemId);
            }
            if (this.fDTDHandler != null) {
                this.fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, baseSystemId, false));
                String notation4 = notation;
                if (notation4 == null) {
                    this.fDTDHandler.externalEntityDecl(name, this.fResourceIdentifier, null);
                } else {
                    this.fDTDHandler.unparsedEntityDecl(name, this.fResourceIdentifier, notation4, null);
                }
            }
        }
        this.fReportEntity = true;
    }

    protected final void scanEntityValue(XMLString value, XMLString nonNormalizedValue) throws IOException, XNIException {
        int quote = this.fEntityScanner.scanChar();
        if (quote != 39 && quote != 34) {
            reportFatalError("OpenQuoteMissingInDecl", null);
        }
        int entityDepth = this.fEntityDepth;
        XMLString literal = this.fString;
        XMLString literal2 = this.fString;
        if (this.fEntityScanner.scanLiteral(quote, this.fString) != quote) {
            this.fStringBuffer.clear();
            this.fStringBuffer2.clear();
            do {
                this.fStringBuffer.append(this.fString);
                this.fStringBuffer2.append(this.fString);
                if (this.fEntityScanner.skipChar(38)) {
                    if (this.fEntityScanner.skipChar(35)) {
                        this.fStringBuffer2.append("&#");
                        scanCharReferenceValue(this.fStringBuffer, this.fStringBuffer2);
                    } else {
                        this.fStringBuffer.append('&');
                        this.fStringBuffer2.append('&');
                        String eName = this.fEntityScanner.scanName();
                        if (eName == null) {
                            reportFatalError("NameRequiredInReference", null);
                        } else {
                            this.fStringBuffer.append(eName);
                            this.fStringBuffer2.append(eName);
                        }
                        if (!this.fEntityScanner.skipChar(59)) {
                            reportFatalError("SemicolonRequiredInReference", new Object[]{eName});
                        } else {
                            this.fStringBuffer.append(';');
                            this.fStringBuffer2.append(';');
                        }
                    }
                } else if (this.fEntityScanner.skipChar(37)) {
                    do {
                        this.fStringBuffer2.append('%');
                        String peName = this.fEntityScanner.scanName();
                        if (peName == null) {
                            reportFatalError("NameRequiredInPEReference", null);
                        } else if (!this.fEntityScanner.skipChar(59)) {
                            reportFatalError("SemicolonRequiredInPEReference", new Object[]{peName});
                        } else {
                            if (scanningInternalSubset()) {
                                reportFatalError("PEReferenceWithinMarkup", new Object[]{peName});
                            }
                            this.fStringBuffer2.append(peName);
                            this.fStringBuffer2.append(';');
                        }
                        startPE(peName, true);
                        this.fEntityScanner.skipSpaces();
                    } while (this.fEntityScanner.skipChar(37));
                } else {
                    int c = this.fEntityScanner.peekChar();
                    if (XMLChar.isHighSurrogate(c)) {
                        scanSurrogates(this.fStringBuffer2);
                    } else if (isInvalidLiteral(c)) {
                        reportFatalError("InvalidCharInLiteral", new Object[]{Integer.toHexString(c)});
                        this.fEntityScanner.scanChar();
                    } else if (c != quote || entityDepth != this.fEntityDepth) {
                        this.fStringBuffer.append((char) c);
                        this.fStringBuffer2.append((char) c);
                        this.fEntityScanner.scanChar();
                    }
                }
            } while (this.fEntityScanner.scanLiteral(quote, this.fString) != quote);
            this.fStringBuffer.append(this.fString);
            this.fStringBuffer2.append(this.fString);
            literal = this.fStringBuffer;
            literal2 = this.fStringBuffer2;
        }
        value.setValues(literal);
        nonNormalizedValue.setValues(literal2);
        if (!this.fEntityScanner.skipChar(quote)) {
            reportFatalError("CloseQuoteMissingInDecl", null);
        }
    }

    private final void scanNotationDecl() throws IOException, XNIException {
        String name;
        this.fReportEntity = false;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL", null);
        }
        if (this.fNamespaces) {
            name = this.fEntityScanner.scanNCName();
        } else {
            name = this.fEntityScanner.scanName();
        }
        if (name == null) {
            reportFatalError("MSG_NOTATION_NAME_REQUIRED_IN_NOTATIONDECL", null);
        }
        if (!skipSeparator(true, !scanningInternalSubset())) {
            if (this.fNamespaces && this.fEntityScanner.peekChar() == 58) {
                this.fEntityScanner.scanChar();
                XMLStringBuffer colonName = new XMLStringBuffer(name);
                colonName.append(':');
                colonName.append(this.fEntityScanner.scanName());
                reportFatalError("ColonNotLegalWithNS", new Object[]{colonName.toString()});
                skipSeparator(true, !scanningInternalSubset());
            } else {
                reportFatalError("MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL", new Object[]{name});
            }
        }
        scanExternalID(this.fStrings, true);
        String systemId = this.fStrings[0];
        String publicId = this.fStrings[1];
        String baseSystemId = this.fEntityScanner.getBaseSystemId();
        if (systemId == null && publicId == null) {
            reportFatalError("ExternalIDorPublicIDRequired", new Object[]{name});
        }
        skipSeparator(false, !scanningInternalSubset());
        if (!this.fEntityScanner.skipChar(62)) {
            reportFatalError("NotationDeclUnterminated", new Object[]{name});
        }
        this.fMarkUpDepth--;
        if (this.fDTDHandler != null) {
            this.fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, baseSystemId, false));
            this.fDTDHandler.notationDecl(name, this.fResourceIdentifier, null);
        }
        this.fReportEntity = true;
    }

    private final void scanConditionalSect(int currPEDepth) throws IOException, XNIException {
        this.fReportEntity = false;
        skipSeparator(false, !scanningInternalSubset());
        if (this.fEntityScanner.skipString("INCLUDE")) {
            skipSeparator(false, !scanningInternalSubset());
            if (currPEDepth != this.fPEDepth && this.fValidation) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "INVALID_PE_IN_CONDITIONAL", new Object[]{this.fEntityManager.fCurrentEntity.name}, (short) 1);
            }
            if (!this.fEntityScanner.skipChar(91)) {
                reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
            }
            if (this.fDTDHandler != null) {
                this.fDTDHandler.startConditional((short) 0, null);
            }
            this.fIncludeSectDepth++;
            this.fReportEntity = true;
            return;
        }
        if (this.fEntityScanner.skipString("IGNORE")) {
            skipSeparator(false, !scanningInternalSubset());
            if (currPEDepth != this.fPEDepth && this.fValidation) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "INVALID_PE_IN_CONDITIONAL", new Object[]{this.fEntityManager.fCurrentEntity.name}, (short) 1);
            }
            if (this.fDTDHandler != null) {
                this.fDTDHandler.startConditional((short) 1, null);
            }
            if (!this.fEntityScanner.skipChar(91)) {
                reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
            }
            this.fReportEntity = true;
            int initialDepth = this.fIncludeSectDepth + 1;
            this.fIncludeSectDepth = initialDepth;
            if (this.fDTDHandler != null) {
                this.fIgnoreConditionalBuffer.clear();
            }
            while (true) {
                if (this.fEntityScanner.skipChar(60)) {
                    if (this.fDTDHandler != null) {
                        this.fIgnoreConditionalBuffer.append('<');
                    }
                    if (this.fEntityScanner.skipChar(33)) {
                        if (this.fEntityScanner.skipChar(91)) {
                            if (this.fDTDHandler != null) {
                                this.fIgnoreConditionalBuffer.append("![");
                            }
                            this.fIncludeSectDepth++;
                        } else if (this.fDTDHandler != null) {
                            this.fIgnoreConditionalBuffer.append("!");
                        }
                    }
                } else if (this.fEntityScanner.skipChar(93)) {
                    if (this.fDTDHandler != null) {
                        this.fIgnoreConditionalBuffer.append(']');
                    }
                    if (this.fEntityScanner.skipChar(93)) {
                        if (this.fDTDHandler != null) {
                            this.fIgnoreConditionalBuffer.append(']');
                        }
                        while (this.fEntityScanner.skipChar(93)) {
                            if (this.fDTDHandler != null) {
                                this.fIgnoreConditionalBuffer.append(']');
                            }
                        }
                        if (this.fEntityScanner.skipChar(62)) {
                            int i = this.fIncludeSectDepth;
                            this.fIncludeSectDepth = i - 1;
                            if (i == initialDepth) {
                                this.fMarkUpDepth--;
                                if (this.fDTDHandler != null) {
                                    this.fLiteral.setValues(this.fIgnoreConditionalBuffer.ch, 0, this.fIgnoreConditionalBuffer.length - 2);
                                    this.fDTDHandler.ignoredCharacters(this.fLiteral, null);
                                    this.fDTDHandler.endConditional(null);
                                    return;
                                }
                                return;
                            }
                            if (this.fDTDHandler != null) {
                                this.fIgnoreConditionalBuffer.append('>');
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    int c = this.fEntityScanner.scanChar();
                    if (this.fScannerState == 0) {
                        reportFatalError("IgnoreSectUnterminated", null);
                        return;
                    } else if (this.fDTDHandler != null) {
                        this.fIgnoreConditionalBuffer.append((char) c);
                    }
                }
            }
        } else {
            reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
        }
    }

    protected final boolean scanDecls(boolean complete) throws IOException, XNIException {
        int ch;
        skipSeparator(false, true);
        boolean again = true;
        while (again && this.fScannerState == 2) {
            again = complete;
            if (this.fEntityScanner.skipChar(60)) {
                this.fMarkUpDepth++;
                if (this.fEntityScanner.skipChar(63)) {
                    scanPI();
                } else if (this.fEntityScanner.skipChar(33)) {
                    if (this.fEntityScanner.skipChar(45)) {
                        if (!this.fEntityScanner.skipChar(45)) {
                            reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
                        } else {
                            scanComment();
                        }
                    } else if (this.fEntityScanner.skipString("ELEMENT")) {
                        scanElementDecl();
                    } else if (this.fEntityScanner.skipString("ATTLIST")) {
                        scanAttlistDecl();
                    } else if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_ENTITY)) {
                        scanEntityDecl();
                    } else if (this.fEntityScanner.skipString(SchemaSymbols.ATTVAL_NOTATION)) {
                        scanNotationDecl();
                    } else if (this.fEntityScanner.skipChar(91) && !scanningInternalSubset()) {
                        scanConditionalSect(this.fPEDepth);
                    } else {
                        this.fMarkUpDepth--;
                        reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
                    }
                } else {
                    this.fMarkUpDepth--;
                    reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
                }
            } else if (this.fIncludeSectDepth > 0 && this.fEntityScanner.skipChar(93)) {
                if (!this.fEntityScanner.skipChar(93) || !this.fEntityScanner.skipChar(62)) {
                    reportFatalError("IncludeSectUnterminated", null);
                }
                if (this.fDTDHandler != null) {
                    this.fDTDHandler.endConditional(null);
                }
                this.fIncludeSectDepth--;
                this.fMarkUpDepth--;
            } else {
                if (scanningInternalSubset() && this.fEntityScanner.peekChar() == 93) {
                    return false;
                }
                if (!this.fEntityScanner.skipSpaces()) {
                    reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
                    do {
                        this.fEntityScanner.scanChar();
                        skipSeparator(false, true);
                        ch = this.fEntityScanner.peekChar();
                        if (ch == 60 || ch == 93) {
                            break;
                        }
                    } while (!XMLChar.isSpace(ch));
                }
            }
            skipSeparator(false, true);
        }
        return this.fScannerState != 0;
    }

    private boolean skipSeparator(boolean spaceRequired, boolean lookForPERefs) throws IOException, XNIException {
        int depth = this.fPEDepth;
        boolean sawSpace = this.fEntityScanner.skipSpaces();
        if (!lookForPERefs || !this.fEntityScanner.skipChar(37)) {
            return (spaceRequired && !sawSpace && depth == this.fPEDepth) ? false : true;
        }
        do {
            String name = this.fEntityScanner.scanName();
            if (name == null) {
                reportFatalError("NameRequiredInPEReference", null);
            } else if (!this.fEntityScanner.skipChar(59)) {
                reportFatalError("SemicolonRequiredInPEReference", new Object[]{name});
            }
            startPE(name, false);
            this.fEntityScanner.skipSpaces();
        } while (this.fEntityScanner.skipChar(37));
        return true;
    }

    private final void pushContentStack(int c) {
        if (this.fContentStack.length == this.fContentDepth) {
            int[] newStack = new int[this.fContentDepth * 2];
            System.arraycopy(this.fContentStack, 0, newStack, 0, this.fContentDepth);
            this.fContentStack = newStack;
        }
        int[] newStack2 = this.fContentStack;
        int i = this.fContentDepth;
        this.fContentDepth = i + 1;
        newStack2[i] = c;
    }

    private final int popContentStack() {
        int[] iArr = this.fContentStack;
        int i = this.fContentDepth - 1;
        this.fContentDepth = i;
        return iArr[i];
    }

    private final void pushPEStack(int depth, boolean report) {
        if (this.fPEStack.length == this.fPEDepth) {
            int[] newIntStack = new int[this.fPEDepth * 2];
            System.arraycopy(this.fPEStack, 0, newIntStack, 0, this.fPEDepth);
            this.fPEStack = newIntStack;
            boolean[] newBooleanStack = new boolean[this.fPEDepth * 2];
            System.arraycopy(this.fPEReport, 0, newBooleanStack, 0, this.fPEDepth);
            this.fPEReport = newBooleanStack;
        }
        this.fPEReport[this.fPEDepth] = report;
        int[] iArr = this.fPEStack;
        int i = this.fPEDepth;
        this.fPEDepth = i + 1;
        iArr[i] = depth;
    }

    private final int popPEStack() {
        int[] iArr = this.fPEStack;
        int i = this.fPEDepth - 1;
        this.fPEDepth = i;
        return iArr[i];
    }

    private final boolean peekReportEntity() {
        return this.fPEReport[this.fPEDepth - 1];
    }

    private final void ensureEnumerationSize(int size) {
        if (this.fEnumeration.length == size) {
            String[] newEnum = new String[size * 2];
            System.arraycopy(this.fEnumeration, 0, newEnum, 0, size);
            this.fEnumeration = newEnum;
        }
    }

    private void init() {
        this.fStartDTDCalled = false;
        this.fExtEntityDepth = 0;
        this.fIncludeSectDepth = 0;
        this.fMarkUpDepth = 0;
        this.fPEDepth = 0;
        this.fStandalone = false;
        this.fSeenExternalDTD = false;
        this.fSeenPEReferences = false;
        setScannerState(1);
    }

    private int skipInvalidEnumerationValue() throws IOException {
        int c;
        do {
            c = this.fEntityScanner.scanChar();
            if (c == 124) {
                break;
            }
        } while (c != 41);
        ensureEnumerationSize(this.fEnumerationCount + 1);
        String[] strArr = this.fEnumeration;
        int i = this.fEnumerationCount;
        this.fEnumerationCount = i + 1;
        strArr[i] = XMLSymbols.EMPTY_STRING;
        return c;
    }
}
