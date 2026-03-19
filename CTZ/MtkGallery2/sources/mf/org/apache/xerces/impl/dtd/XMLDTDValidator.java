package mf.org.apache.xerces.impl.dtd;

import java.io.IOException;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.RevalidationHandler;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dtd.models.ContentModelValidator;
import mf.org.apache.xerces.impl.dv.DTDDVFactory;
import mf.org.apache.xerces.impl.dv.DatatypeValidator;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.impl.validation.ValidationState;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.jaxp.JAXPConstants;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.NamespaceContext;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XMLDocumentHandler;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XMLString;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDocumentFilter;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;

public class XMLDTDValidator implements RevalidationHandler, XMLDTDValidatorFilter, XMLComponent, XMLDocumentFilter {
    protected static final String DATATYPE_VALIDATOR_FACTORY = "http://apache.org/xml/properties/internal/datatype-validator-factory";
    private static final boolean DEBUG_ATTRIBUTES = false;
    private static final boolean DEBUG_ELEMENT_CHILDREN = false;
    protected static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    private static final Boolean[] FEATURE_DEFAULTS;
    protected static final String GRAMMAR_POOL = "http://apache.org/xml/properties/internal/grammar-pool";
    protected static final String PARSER_SETTINGS = "http://apache.org/xml/features/internal/parser-settings";
    private static final Object[] PROPERTY_DEFAULTS;
    private static final String[] RECOGNIZED_PROPERTIES;
    protected static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    private static final int TOP_LEVEL_SCOPE = -1;
    protected static final String VALIDATION_MANAGER = "http://apache.org/xml/properties/internal/validation-manager";
    protected static final String WARN_ON_DUPLICATE_ATTDEF = "http://apache.org/xml/features/validation/warn-on-duplicate-attdef";
    protected boolean fBalanceSyntaxTrees;
    protected DTDGrammar fDTDGrammar;
    protected boolean fDTDValidation;
    protected DTDDVFactory fDatatypeValidatorFactory;
    protected XMLLocator fDocLocation;
    protected XMLDocumentHandler fDocumentHandler;
    protected XMLDocumentSource fDocumentSource;
    protected boolean fDynamicValidation;
    protected XMLErrorReporter fErrorReporter;
    protected DTDGrammarBucket fGrammarBucket;
    protected XMLGrammarPool fGrammarPool;
    protected boolean fNamespaces;
    private boolean fPerformValidation;
    private String fSchemaType;
    protected SymbolTable fSymbolTable;
    protected DatatypeValidator fValENTITIES;
    protected DatatypeValidator fValENTITY;
    protected DatatypeValidator fValID;
    protected DatatypeValidator fValIDRef;
    protected DatatypeValidator fValIDRefs;
    protected DatatypeValidator fValNMTOKEN;
    protected DatatypeValidator fValNMTOKENS;
    protected DatatypeValidator fValNOTATION;
    protected boolean fValidation;
    protected boolean fWarnDuplicateAttdef;
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    protected static final String VALIDATION = "http://xml.org/sax/features/validation";
    protected static final String DYNAMIC_VALIDATION = "http://apache.org/xml/features/validation/dynamic";
    protected static final String BALANCE_SYNTAX_TREES = "http://apache.org/xml/features/validation/balance-syntax-trees";
    private static final String[] RECOGNIZED_FEATURES = {NAMESPACES, VALIDATION, DYNAMIC_VALIDATION, BALANCE_SYNTAX_TREES};
    protected ValidationManager fValidationManager = null;
    protected final ValidationState fValidationState = new ValidationState();
    protected NamespaceContext fNamespaceContext = null;
    protected boolean fSeenDoctypeDecl = false;
    private final QName fCurrentElement = new QName();
    private int fCurrentElementIndex = -1;
    private int fCurrentContentSpecType = -1;
    private final QName fRootElement = new QName();
    private boolean fInCDATASection = false;
    private int[] fElementIndexStack = new int[8];
    private int[] fContentSpecTypeStack = new int[8];
    private QName[] fElementQNamePartsStack = new QName[8];
    private QName[] fElementChildren = new QName[32];
    private int fElementChildrenLength = 0;
    private int[] fElementChildrenOffsetStack = new int[32];
    private int fElementDepth = -1;
    private boolean fSeenRootElement = false;
    private boolean fInElementContent = false;
    private XMLElementDecl fTempElementDecl = new XMLElementDecl();
    private final XMLAttributeDecl fTempAttDecl = new XMLAttributeDecl();
    private final XMLEntityDecl fEntityDecl = new XMLEntityDecl();
    private final QName fTempQName = new QName();
    private final StringBuffer fBuffer = new StringBuffer();

    static {
        Boolean[] boolArr = new Boolean[4];
        boolArr[2] = Boolean.FALSE;
        boolArr[3] = Boolean.FALSE;
        FEATURE_DEFAULTS = boolArr;
        RECOGNIZED_PROPERTIES = new String[]{"http://apache.org/xml/properties/internal/symbol-table", "http://apache.org/xml/properties/internal/error-reporter", "http://apache.org/xml/properties/internal/grammar-pool", DATATYPE_VALIDATOR_FACTORY, VALIDATION_MANAGER};
        PROPERTY_DEFAULTS = new Object[5];
    }

    public XMLDTDValidator() {
        for (int i = 0; i < this.fElementQNamePartsStack.length; i++) {
            this.fElementQNamePartsStack[i] = new QName();
        }
        this.fGrammarBucket = new DTDGrammarBucket();
    }

    DTDGrammarBucket getGrammarBucket() {
        return this.fGrammarBucket;
    }

    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        boolean parser_settings;
        this.fDTDGrammar = null;
        this.fSeenDoctypeDecl = false;
        this.fInCDATASection = false;
        this.fSeenRootElement = false;
        this.fInElementContent = false;
        this.fCurrentElementIndex = -1;
        this.fCurrentContentSpecType = -1;
        this.fRootElement.clear();
        this.fValidationState.resetIDTables();
        this.fGrammarBucket.clear();
        this.fElementDepth = -1;
        this.fElementChildrenLength = 0;
        try {
            parser_settings = componentManager.getFeature(PARSER_SETTINGS);
        } catch (XMLConfigurationException e) {
            parser_settings = true;
        }
        if (!parser_settings) {
            this.fValidationManager.addValidationState(this.fValidationState);
            return;
        }
        try {
            this.fNamespaces = componentManager.getFeature(NAMESPACES);
        } catch (XMLConfigurationException e2) {
            this.fNamespaces = true;
        }
        try {
            this.fValidation = componentManager.getFeature(VALIDATION);
        } catch (XMLConfigurationException e3) {
            this.fValidation = false;
        }
        try {
            this.fDTDValidation = !componentManager.getFeature("http://apache.org/xml/features/validation/schema");
        } catch (XMLConfigurationException e4) {
            this.fDTDValidation = true;
        }
        try {
            this.fDynamicValidation = componentManager.getFeature(DYNAMIC_VALIDATION);
        } catch (XMLConfigurationException e5) {
            this.fDynamicValidation = false;
        }
        try {
            this.fBalanceSyntaxTrees = componentManager.getFeature(BALANCE_SYNTAX_TREES);
        } catch (XMLConfigurationException e6) {
            this.fBalanceSyntaxTrees = false;
        }
        try {
            this.fWarnDuplicateAttdef = componentManager.getFeature(WARN_ON_DUPLICATE_ATTDEF);
        } catch (XMLConfigurationException e7) {
            this.fWarnDuplicateAttdef = false;
        }
        try {
            this.fSchemaType = (String) componentManager.getProperty(JAXPConstants.JAXP_SCHEMA_LANGUAGE);
        } catch (XMLConfigurationException e8) {
            this.fSchemaType = null;
        }
        this.fValidationManager = (ValidationManager) componentManager.getProperty(VALIDATION_MANAGER);
        this.fValidationManager.addValidationState(this.fValidationState);
        this.fValidationState.setUsingNamespaces(this.fNamespaces);
        this.fErrorReporter = (XMLErrorReporter) componentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        this.fSymbolTable = (SymbolTable) componentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        try {
            this.fGrammarPool = (XMLGrammarPool) componentManager.getProperty("http://apache.org/xml/properties/internal/grammar-pool");
        } catch (XMLConfigurationException e9) {
            this.fGrammarPool = null;
        }
        this.fDatatypeValidatorFactory = (DTDDVFactory) componentManager.getProperty(DATATYPE_VALIDATOR_FACTORY);
        init();
    }

    @Override
    public String[] getRecognizedFeatures() {
        return (String[]) RECOGNIZED_FEATURES.clone();
    }

    @Override
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
    }

    @Override
    public String[] getRecognizedProperties() {
        return (String[]) RECOGNIZED_PROPERTIES.clone();
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
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
    public void setDocumentHandler(XMLDocumentHandler documentHandler) {
        this.fDocumentHandler = documentHandler;
    }

    @Override
    public XMLDocumentHandler getDocumentHandler() {
        return this.fDocumentHandler;
    }

    @Override
    public void setDocumentSource(XMLDocumentSource source) {
        this.fDocumentSource = source;
    }

    @Override
    public XMLDocumentSource getDocumentSource() {
        return this.fDocumentSource;
    }

    @Override
    public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
        if (this.fGrammarPool != null) {
            Grammar[] grammars = this.fGrammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_DTD);
            int length = grammars != null ? grammars.length : 0;
            for (int i = 0; i < length; i++) {
                this.fGrammarBucket.putGrammar((DTDGrammar) grammars[i]);
            }
        }
        this.fDocLocation = locator;
        this.fNamespaceContext = namespaceContext;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startDocument(locator, encoding, namespaceContext, augs);
        }
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        this.fGrammarBucket.setStandalone(standalone != null && standalone.equals("yes"));
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws URI.MalformedURIException, XNIException {
        this.fSeenDoctypeDecl = true;
        this.fRootElement.setValues(null, rootElement, rootElement, null);
        String eid = null;
        try {
            eid = XMLEntityManager.expandSystemId(systemId, this.fDocLocation.getExpandedSystemId(), false);
        } catch (IOException e) {
        }
        XMLDTDDescription grammarDesc = new XMLDTDDescription(publicId, systemId, this.fDocLocation.getExpandedSystemId(), eid, rootElement);
        this.fDTDGrammar = this.fGrammarBucket.getGrammar(grammarDesc);
        if (this.fDTDGrammar == null && this.fGrammarPool != null && (systemId != null || publicId != null)) {
            this.fDTDGrammar = (DTDGrammar) this.fGrammarPool.retrieveGrammar(grammarDesc);
        }
        if (this.fDTDGrammar != null) {
            this.fValidationManager.setCachedDTD(true);
        } else if (!this.fBalanceSyntaxTrees) {
            this.fDTDGrammar = new DTDGrammar(this.fSymbolTable, grammarDesc);
        } else {
            this.fDTDGrammar = new BalancedDTDGrammar(this.fSymbolTable, grammarDesc);
        }
        this.fGrammarBucket.setActiveGrammar(this.fDTDGrammar);
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.doctypeDecl(rootElement, publicId, systemId, augs);
        }
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        handleStartElement(element, attributes, augs);
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startElement(element, attributes, augs);
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        boolean removed = handleStartElement(element, attributes, augs);
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.emptyElement(element, attributes, augs);
        }
        if (!removed) {
            handleEndElement(element, augs, true);
        }
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        boolean callNextCharacters = true;
        boolean allWhiteSpace = true;
        int i = text.offset;
        while (true) {
            if (i >= text.offset + text.length) {
                break;
            }
            if (isSpace(text.ch[i])) {
                i++;
            } else {
                allWhiteSpace = false;
                break;
            }
        }
        if (this.fInElementContent && allWhiteSpace && !this.fInCDATASection && this.fDocumentHandler != null) {
            this.fDocumentHandler.ignorableWhitespace(text, augs);
            callNextCharacters = false;
        }
        if (this.fPerformValidation) {
            if (this.fInElementContent) {
                if (this.fGrammarBucket.getStandalone() && this.fDTDGrammar.getElementDeclIsExternal(this.fCurrentElementIndex) && allWhiteSpace) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE", null, (short) 1);
                }
                if (!allWhiteSpace) {
                    charDataInContent();
                }
                if (augs != null && augs.getItem(Constants.CHAR_REF_PROBABLE_WS) == Boolean.TRUE) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_CONTENT_INVALID_SPECIFIED", new Object[]{this.fCurrentElement.rawname, this.fDTDGrammar.getContentSpecAsString(this.fElementDepth), "character reference"}, (short) 1);
                }
            }
            if (this.fCurrentContentSpecType == 1) {
                charDataInContent();
            }
        }
        if (callNextCharacters && this.fDocumentHandler != null) {
            this.fDocumentHandler.characters(text, augs);
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.ignorableWhitespace(text, augs);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        handleEndElement(element, augs, false);
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        if (this.fPerformValidation && this.fInElementContent) {
            charDataInContent();
        }
        this.fInCDATASection = true;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startCDATA(augs);
        }
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        this.fInCDATASection = false;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endCDATA(augs);
        }
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endDocument(augs);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (this.fPerformValidation && this.fElementDepth >= 0 && this.fDTDGrammar != null) {
            this.fDTDGrammar.getElementDecl(this.fCurrentElementIndex, this.fTempElementDecl);
            if (this.fTempElementDecl.type == 1) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_CONTENT_INVALID_SPECIFIED", new Object[]{this.fCurrentElement.rawname, "EMPTY", "comment"}, (short) 1);
            }
        }
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.comment(text, augs);
        }
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        if (this.fPerformValidation && this.fElementDepth >= 0 && this.fDTDGrammar != null) {
            this.fDTDGrammar.getElementDecl(this.fCurrentElementIndex, this.fTempElementDecl);
            if (this.fTempElementDecl.type == 1) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_CONTENT_INVALID_SPECIFIED", new Object[]{this.fCurrentElement.rawname, "EMPTY", "processing instruction"}, (short) 1);
            }
        }
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.processingInstruction(target, data, augs);
        }
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        if (this.fPerformValidation && this.fElementDepth >= 0 && this.fDTDGrammar != null) {
            this.fDTDGrammar.getElementDecl(this.fCurrentElementIndex, this.fTempElementDecl);
            if (this.fTempElementDecl.type == 1) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_CONTENT_INVALID_SPECIFIED", new Object[]{this.fCurrentElement.rawname, "EMPTY", SchemaSymbols.ATTVAL_ENTITY}, (short) 1);
            }
            if (this.fGrammarBucket.getStandalone()) {
                XMLDTDLoader.checkStandaloneEntityRef(name, this.fDTDGrammar, this.fEntityDecl, this.fErrorReporter);
            }
        }
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs);
        }
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endGeneralEntity(name, augs);
        }
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.textDecl(version, encoding, augs);
        }
    }

    @Override
    public final boolean hasGrammar() {
        return this.fDTDGrammar != null;
    }

    @Override
    public final boolean validate() {
        if (this.fSchemaType == Constants.NS_XMLSCHEMA) {
            return false;
        }
        if ((this.fDynamicValidation || !this.fValidation) && !(this.fDynamicValidation && this.fSeenDoctypeDecl)) {
            return false;
        }
        return this.fDTDValidation || this.fSeenDoctypeDecl;
    }

    protected void addDTDDefaultAttrsAndValidate(QName qName, int i, XMLAttributes xMLAttributes) throws XNIException {
        char c;
        int i2;
        int i3;
        short s;
        String nonNormalizedValue;
        String externalEntityRefInAttrValue;
        int i4;
        String str;
        String strAddSymbol;
        int i5 = i;
        int i6 = -1;
        if (i5 == -1 || this.fDTDGrammar == null) {
            return;
        }
        int firstAttributeDeclIndex = this.fDTDGrammar.getFirstAttributeDeclIndex(i5);
        while (true) {
            c = 0;
            i2 = 1;
            if (firstAttributeDeclIndex == i6) {
                break;
            }
            this.fDTDGrammar.getAttributeDecl(firstAttributeDeclIndex, this.fTempAttDecl);
            String str2 = this.fTempAttDecl.name.prefix;
            String str3 = this.fTempAttDecl.name.localpart;
            String str4 = this.fTempAttDecl.name.rawname;
            String attributeTypeName = getAttributeTypeName(this.fTempAttDecl);
            short s2 = this.fTempAttDecl.simpleType.defaultType;
            String str5 = null;
            if (this.fTempAttDecl.simpleType.defaultValue != null) {
                str5 = this.fTempAttDecl.simpleType.defaultValue;
            }
            boolean z = false;
            boolean z2 = s2 == 2;
            if (!(attributeTypeName == XMLSymbols.fCDATASymbol) || z2 || str5 != null) {
                int length = xMLAttributes.getLength();
                int i7 = 0;
                while (true) {
                    if (i7 >= length) {
                        break;
                    }
                    if (xMLAttributes.getQName(i7) != str4) {
                        i7++;
                        str2 = str2;
                        str3 = str3;
                    } else {
                        z = true;
                        break;
                    }
                }
            }
            if (z) {
                i4 = -1;
            } else {
                if (z2) {
                    if (this.fPerformValidation) {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED", new Object[]{qName.localpart, str4}, (short) 1);
                    }
                    i4 = -1;
                } else {
                    String str6 = str2;
                    String str7 = str3;
                    if (str5 != null) {
                        if (this.fPerformValidation && this.fGrammarBucket.getStandalone() && this.fDTDGrammar.getAttributeDeclIsExternal(firstAttributeDeclIndex)) {
                            this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_DEFAULTED_ATTRIBUTE_NOT_SPECIFIED", new Object[]{qName.localpart, str4}, (short) 1);
                        }
                        if (this.fNamespaces) {
                            int iIndexOf = str4.indexOf(58);
                            i4 = -1;
                            if (iIndexOf != -1) {
                                String strAddSymbol2 = this.fSymbolTable.addSymbol(str4.substring(0, iIndexOf));
                                strAddSymbol = this.fSymbolTable.addSymbol(str4.substring(iIndexOf + 1));
                                str = strAddSymbol2;
                            }
                            this.fTempQName.setValues(str, strAddSymbol, str4, this.fTempAttDecl.name.uri);
                            xMLAttributes.addAttribute(this.fTempQName, attributeTypeName, str5);
                        } else {
                            i4 = -1;
                        }
                        str = str6;
                        strAddSymbol = str7;
                        this.fTempQName.setValues(str, strAddSymbol, str4, this.fTempAttDecl.name.uri);
                        xMLAttributes.addAttribute(this.fTempQName, attributeTypeName, str5);
                    }
                }
                i4 = -1;
            }
            firstAttributeDeclIndex = this.fDTDGrammar.getNextAttributeDeclIndex(firstAttributeDeclIndex);
            i6 = i4;
            i5 = i;
        }
        int length2 = xMLAttributes.getLength();
        int i8 = 0;
        while (i8 < length2) {
            String qName2 = xMLAttributes.getQName(i8);
            boolean z3 = false;
            if (this.fPerformValidation && this.fGrammarBucket.getStandalone() && (nonNormalizedValue = xMLAttributes.getNonNormalizedValue(i8)) != null && (externalEntityRefInAttrValue = getExternalEntityRefInAttrValue(nonNormalizedValue)) != null) {
                XMLErrorReporter xMLErrorReporter = this.fErrorReporter;
                i3 = length2;
                Object[] objArr = new Object[i2];
                objArr[c] = externalEntityRefInAttrValue;
                xMLErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE", objArr, i2);
            } else {
                i3 = length2;
            }
            int i9 = -1;
            int firstAttributeDeclIndex2 = this.fDTDGrammar.getFirstAttributeDeclIndex(i5);
            ?? r8 = i2;
            while (true) {
                if (firstAttributeDeclIndex2 == -1) {
                    break;
                }
                this.fDTDGrammar.getAttributeDecl(firstAttributeDeclIndex2, this.fTempAttDecl);
                if (this.fTempAttDecl.name.rawname == qName2) {
                    i9 = firstAttributeDeclIndex2;
                    z3 = true;
                    break;
                } else {
                    firstAttributeDeclIndex2 = this.fDTDGrammar.getNextAttributeDeclIndex(firstAttributeDeclIndex2);
                    c = 0;
                    r8 = 1;
                }
            }
            if (!z3) {
                if (this.fPerformValidation) {
                    Object[] objArr2 = new Object[2];
                    objArr2[c] = qName.rawname;
                    objArr2[r8] = qName2;
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_ATTRIBUTE_NOT_DECLARED", objArr2, r8);
                }
            } else {
                String attributeTypeName2 = getAttributeTypeName(this.fTempAttDecl);
                xMLAttributes.setType(i8, attributeTypeName2);
                xMLAttributes.getAugmentations(i8).putItem(Constants.ATTRIBUTE_DECLARED, Boolean.TRUE);
                String value = xMLAttributes.getValue(i8);
                String value2 = value;
                if (xMLAttributes.isSpecified(i8) && attributeTypeName2 != XMLSymbols.fCDATASymbol) {
                    boolean zNormalizeAttrValue = normalizeAttrValue(xMLAttributes, i8);
                    value2 = xMLAttributes.getValue(i8);
                    if (this.fPerformValidation && this.fGrammarBucket.getStandalone() && zNormalizeAttrValue && this.fDTDGrammar.getAttributeDeclIsExternal(firstAttributeDeclIndex2)) {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_ATTVALUE_CHANGED_DURING_NORMALIZATION_WHEN_STANDALONE", new Object[]{qName2, value, value2}, (short) 1);
                    }
                    if (this.fPerformValidation) {
                    }
                } else if (this.fPerformValidation) {
                    if (this.fTempAttDecl.simpleType.defaultType == 1) {
                        String str8 = this.fTempAttDecl.simpleType.defaultValue;
                        if (!value2.equals(str8)) {
                            Object[] objArr3 = {qName.localpart, qName2, value2, str8};
                            s = 1;
                            this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_FIXED_ATTVALUE_INVALID", objArr3, (short) 1);
                        } else {
                            s = 1;
                        }
                    } else {
                        s = 1;
                    }
                    if (this.fTempAttDecl.simpleType.type == s || this.fTempAttDecl.simpleType.type == 2 || this.fTempAttDecl.simpleType.type == 3 || this.fTempAttDecl.simpleType.type == 4 || this.fTempAttDecl.simpleType.type == 5 || this.fTempAttDecl.simpleType.type == 6) {
                        validateDTDattribute(qName, value2, this.fTempAttDecl);
                    }
                }
            }
            i8++;
            length2 = i3;
            i5 = i;
            c = 0;
            i2 = 1;
        }
    }

    protected String getExternalEntityRefInAttrValue(String nonNormalizedValue) {
        int valLength = nonNormalizedValue.length();
        int ampIndex = nonNormalizedValue.indexOf(38);
        while (ampIndex != -1) {
            if (ampIndex + 1 < valLength && nonNormalizedValue.charAt(ampIndex + 1) != '#') {
                int semicolonIndex = nonNormalizedValue.indexOf(59, ampIndex + 1);
                String entityName = this.fSymbolTable.addSymbol(nonNormalizedValue.substring(ampIndex + 1, semicolonIndex));
                int entIndex = this.fDTDGrammar.getEntityDeclIndex(entityName);
                if (entIndex > -1) {
                    this.fDTDGrammar.getEntityDecl(entIndex, this.fEntityDecl);
                    if (!this.fEntityDecl.inExternal) {
                        String externalEntityRefInAttrValue = getExternalEntityRefInAttrValue(this.fEntityDecl.value);
                        entityName = externalEntityRefInAttrValue;
                        if (externalEntityRefInAttrValue != null) {
                        }
                    }
                    return entityName;
                }
                continue;
            }
            ampIndex = nonNormalizedValue.indexOf(38, ampIndex + 1);
        }
        return null;
    }

    protected void validateDTDattribute(QName element, String attValue, XMLAttributeDecl attributeDecl) throws XNIException {
        switch (attributeDecl.simpleType.type) {
            case 1:
                try {
                    if (attributeDecl.simpleType.list) {
                        this.fValENTITIES.validate(attValue, this.fValidationState);
                    } else {
                        this.fValENTITY.validate(attValue, this.fValidationState);
                    }
                } catch (InvalidDatatypeValueException ex) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", ex.getKey(), ex.getArgs(), (short) 1);
                    return;
                }
                break;
            case 2:
            case 6:
                boolean found = false;
                String[] enumVals = attributeDecl.simpleType.enumeration;
                if (enumVals == null) {
                    found = false;
                } else {
                    for (int i = 0; i < enumVals.length; i++) {
                        if (attValue == enumVals[i] || attValue.equals(enumVals[i])) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    StringBuffer enumValueString = new StringBuffer();
                    if (enumVals != null) {
                        for (String str : enumVals) {
                            enumValueString.append(String.valueOf(str) + " ");
                        }
                    }
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_ATTRIBUTE_VALUE_NOT_IN_LIST", new Object[]{attributeDecl.name.rawname, attValue, enumValueString}, (short) 1);
                }
                break;
            case 3:
                try {
                    this.fValID.validate(attValue, this.fValidationState);
                } catch (InvalidDatatypeValueException ex2) {
                    this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", ex2.getKey(), ex2.getArgs(), (short) 1);
                    return;
                }
                break;
            case 4:
                boolean isAlistAttribute = attributeDecl.simpleType.list;
                try {
                    if (isAlistAttribute) {
                        this.fValIDRefs.validate(attValue, this.fValidationState);
                    } else {
                        this.fValIDRef.validate(attValue, this.fValidationState);
                    }
                    break;
                } catch (InvalidDatatypeValueException ex3) {
                    if (isAlistAttribute) {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "IDREFSInvalid", new Object[]{attValue}, (short) 1);
                        return;
                    } else {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", ex3.getKey(), ex3.getArgs(), (short) 1);
                        return;
                    }
                }
                break;
            case 5:
                boolean isAlistAttribute2 = attributeDecl.simpleType.list;
                try {
                    if (isAlistAttribute2) {
                        this.fValNMTOKENS.validate(attValue, this.fValidationState);
                    } else {
                        this.fValNMTOKEN.validate(attValue, this.fValidationState);
                    }
                    break;
                } catch (InvalidDatatypeValueException e) {
                    if (isAlistAttribute2) {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "NMTOKENSInvalid", new Object[]{attValue}, (short) 1);
                        return;
                    } else {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "NMTOKENInvalid", new Object[]{attValue}, (short) 1);
                        return;
                    }
                }
                break;
        }
    }

    protected boolean invalidStandaloneAttDef(QName element, QName attribute) {
        return true;
    }

    private boolean normalizeAttrValue(XMLAttributes attributes, int index) {
        boolean leadingSpace = true;
        boolean spaceStart = false;
        boolean readingNonSpace = false;
        int count = 0;
        int eaten = 0;
        String attrValue = attributes.getValue(index);
        char[] attValue = new char[attrValue.length()];
        this.fBuffer.setLength(0);
        attrValue.getChars(0, attrValue.length(), attValue, 0);
        for (int i = 0; i < attValue.length; i++) {
            if (attValue[i] == ' ') {
                if (readingNonSpace) {
                    spaceStart = true;
                    readingNonSpace = false;
                }
                if (spaceStart && !leadingSpace) {
                    spaceStart = false;
                    this.fBuffer.append(attValue[i]);
                    count++;
                } else if (leadingSpace || !spaceStart) {
                    eaten++;
                }
            } else {
                readingNonSpace = true;
                spaceStart = false;
                leadingSpace = false;
                this.fBuffer.append(attValue[i]);
                count++;
            }
        }
        if (count > 0 && this.fBuffer.charAt(count - 1) == ' ') {
            this.fBuffer.setLength(count - 1);
        }
        String newValue = this.fBuffer.toString();
        attributes.setValue(index, newValue);
        return !attrValue.equals(newValue);
    }

    private final void rootElementSpecified(QName rootElement) throws XNIException {
        if (this.fPerformValidation) {
            String root1 = this.fRootElement.rawname;
            String root2 = rootElement.rawname;
            if (root1 == null || !root1.equals(root2)) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "RootElementTypeMustMatchDoctypedecl", new Object[]{root1, root2}, (short) 1);
            }
        }
    }

    private int checkContent(int elementIndex, QName[] children, int childOffset, int childCount) throws XNIException {
        this.fDTDGrammar.getElementDecl(elementIndex, this.fTempElementDecl);
        String str = this.fCurrentElement.rawname;
        int contentType = this.fCurrentContentSpecType;
        if (contentType == 1) {
            if (childCount != 0) {
                return 0;
            }
            return -1;
        }
        if (contentType != 0) {
            if (contentType == 2 || contentType == 3) {
                ContentModelValidator cmElem = this.fTempElementDecl.contentModelValidator;
                int result = cmElem.validate(children, childOffset, childCount);
                return result;
            }
            return -1;
        }
        return -1;
    }

    private int getContentSpecType(int elementIndex) {
        if (elementIndex <= -1 || !this.fDTDGrammar.getElementDecl(elementIndex, this.fTempElementDecl)) {
            return -1;
        }
        int contentSpecType = this.fTempElementDecl.type;
        return contentSpecType;
    }

    private void charDataInContent() {
        if (this.fElementChildren.length <= this.fElementChildrenLength) {
            QName[] newarray = new QName[this.fElementChildren.length * 2];
            System.arraycopy(this.fElementChildren, 0, newarray, 0, this.fElementChildren.length);
            this.fElementChildren = newarray;
        }
        QName qname = this.fElementChildren[this.fElementChildrenLength];
        if (qname == null) {
            for (int i = this.fElementChildrenLength; i < this.fElementChildren.length; i++) {
                this.fElementChildren[i] = new QName();
            }
            qname = this.fElementChildren[this.fElementChildrenLength];
        }
        qname.clear();
        this.fElementChildrenLength++;
    }

    private String getAttributeTypeName(XMLAttributeDecl attrDecl) {
        switch (attrDecl.simpleType.type) {
            case 1:
                return attrDecl.simpleType.list ? XMLSymbols.fENTITIESSymbol : XMLSymbols.fENTITYSymbol;
            case 2:
                StringBuffer buffer = new StringBuffer();
                buffer.append('(');
                for (int i = 0; i < attrDecl.simpleType.enumeration.length; i++) {
                    if (i > 0) {
                        buffer.append('|');
                    }
                    buffer.append(attrDecl.simpleType.enumeration[i]);
                }
                buffer.append(')');
                return this.fSymbolTable.addSymbol(buffer.toString());
            case 3:
                return XMLSymbols.fIDSymbol;
            case 4:
                return attrDecl.simpleType.list ? XMLSymbols.fIDREFSSymbol : XMLSymbols.fIDREFSymbol;
            case 5:
                return attrDecl.simpleType.list ? XMLSymbols.fNMTOKENSSymbol : XMLSymbols.fNMTOKENSymbol;
            case 6:
                return XMLSymbols.fNOTATIONSymbol;
            default:
                return XMLSymbols.fCDATASymbol;
        }
    }

    protected void init() {
        if (this.fValidation || this.fDynamicValidation) {
            try {
                this.fValID = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fIDSymbol);
                this.fValIDRef = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fIDREFSymbol);
                this.fValIDRefs = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fIDREFSSymbol);
                this.fValENTITY = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fENTITYSymbol);
                this.fValENTITIES = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fENTITIESSymbol);
                this.fValNMTOKEN = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fNMTOKENSymbol);
                this.fValNMTOKENS = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fNMTOKENSSymbol);
                this.fValNOTATION = this.fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fNOTATIONSymbol);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    private void ensureStackCapacity(int newElementDepth) {
        if (newElementDepth == this.fElementQNamePartsStack.length) {
            QName[] newStackOfQueue = new QName[newElementDepth * 2];
            System.arraycopy(this.fElementQNamePartsStack, 0, newStackOfQueue, 0, newElementDepth);
            this.fElementQNamePartsStack = newStackOfQueue;
            QName qname = this.fElementQNamePartsStack[newElementDepth];
            if (qname == null) {
                for (int i = newElementDepth; i < this.fElementQNamePartsStack.length; i++) {
                    this.fElementQNamePartsStack[i] = new QName();
                }
            }
            int i2 = newElementDepth * 2;
            int[] newStack = new int[i2];
            System.arraycopy(this.fElementIndexStack, 0, newStack, 0, newElementDepth);
            this.fElementIndexStack = newStack;
            int[] newStack2 = new int[newElementDepth * 2];
            System.arraycopy(this.fContentSpecTypeStack, 0, newStack2, 0, newElementDepth);
            this.fContentSpecTypeStack = newStack2;
        }
    }

    protected boolean handleStartElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        if (!this.fSeenRootElement) {
            this.fPerformValidation = validate();
            this.fSeenRootElement = true;
            this.fValidationManager.setEntityState(this.fDTDGrammar);
            this.fValidationManager.setGrammarFound(this.fSeenDoctypeDecl);
            rootElementSpecified(element);
        }
        if (this.fDTDGrammar == null) {
            if (!this.fPerformValidation) {
                this.fCurrentElementIndex = -1;
                this.fCurrentContentSpecType = -1;
                this.fInElementContent = false;
            }
            if (this.fPerformValidation) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_GRAMMAR_NOT_FOUND", new Object[]{element.rawname}, (short) 1);
            }
            if (this.fDocumentSource != null) {
                this.fDocumentSource.setDocumentHandler(this.fDocumentHandler);
                if (this.fDocumentHandler != null) {
                    this.fDocumentHandler.setDocumentSource(this.fDocumentSource);
                }
                return true;
            }
        } else {
            this.fCurrentElementIndex = this.fDTDGrammar.getElementDeclIndex(element);
            this.fCurrentContentSpecType = this.fDTDGrammar.getContentSpecType(this.fCurrentElementIndex);
            if (this.fCurrentContentSpecType == -1 && this.fPerformValidation) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_ELEMENT_NOT_DECLARED", new Object[]{element.rawname}, (short) 1);
            }
            addDTDDefaultAttrsAndValidate(element, this.fCurrentElementIndex, attributes);
        }
        this.fInElementContent = this.fCurrentContentSpecType == 3;
        this.fElementDepth++;
        if (this.fPerformValidation) {
            if (this.fElementChildrenOffsetStack.length <= this.fElementDepth) {
                int[] newarray = new int[this.fElementChildrenOffsetStack.length * 2];
                System.arraycopy(this.fElementChildrenOffsetStack, 0, newarray, 0, this.fElementChildrenOffsetStack.length);
                this.fElementChildrenOffsetStack = newarray;
            }
            int[] newarray2 = this.fElementChildrenOffsetStack;
            newarray2[this.fElementDepth] = this.fElementChildrenLength;
            if (this.fElementChildren.length <= this.fElementChildrenLength) {
                QName[] newarray3 = new QName[this.fElementChildrenLength * 2];
                System.arraycopy(this.fElementChildren, 0, newarray3, 0, this.fElementChildren.length);
                this.fElementChildren = newarray3;
            }
            QName[] newarray4 = this.fElementChildren;
            QName qname = newarray4[this.fElementChildrenLength];
            if (qname == null) {
                for (int i = this.fElementChildrenLength; i < this.fElementChildren.length; i++) {
                    this.fElementChildren[i] = new QName();
                }
                qname = this.fElementChildren[this.fElementChildrenLength];
            }
            qname.setValues(element);
            this.fElementChildrenLength++;
        }
        this.fCurrentElement.setValues(element);
        ensureStackCapacity(this.fElementDepth);
        this.fElementQNamePartsStack[this.fElementDepth].setValues(this.fCurrentElement);
        this.fElementIndexStack[this.fElementDepth] = this.fCurrentElementIndex;
        this.fContentSpecTypeStack[this.fElementDepth] = this.fCurrentContentSpecType;
        startNamespaceScope(element, attributes, augs);
        return false;
    }

    protected void startNamespaceScope(QName element, XMLAttributes attributes, Augmentations augs) {
    }

    protected void handleEndElement(QName element, Augmentations augs, boolean isEmpty) throws XNIException {
        String value;
        this.fElementDepth--;
        if (this.fPerformValidation) {
            int elementIndex = this.fCurrentElementIndex;
            if (elementIndex != -1 && this.fCurrentContentSpecType != -1) {
                QName[] children = this.fElementChildren;
                int childrenOffset = this.fElementChildrenOffsetStack[this.fElementDepth + 1] + 1;
                int childrenLength = this.fElementChildrenLength - childrenOffset;
                int result = checkContent(elementIndex, children, childrenOffset, childrenLength);
                if (result != -1) {
                    this.fDTDGrammar.getElementDecl(elementIndex, this.fTempElementDecl);
                    if (this.fTempElementDecl.type == 1) {
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_CONTENT_INVALID", new Object[]{element.rawname, "EMPTY"}, (short) 1);
                    } else {
                        String messageKey = result != childrenLength ? "MSG_CONTENT_INVALID" : "MSG_CONTENT_INCOMPLETE";
                        this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", messageKey, new Object[]{element.rawname, this.fDTDGrammar.getContentSpecAsString(elementIndex)}, (short) 1);
                    }
                }
            }
            this.fElementChildrenLength = this.fElementChildrenOffsetStack[this.fElementDepth + 1] + 1;
        }
        endNamespaceScope(this.fCurrentElement, augs, isEmpty);
        if (this.fElementDepth < -1) {
            throw new RuntimeException("FWK008 Element stack underflow");
        }
        if (this.fElementDepth < 0) {
            this.fCurrentElement.clear();
            this.fCurrentElementIndex = -1;
            this.fCurrentContentSpecType = -1;
            this.fInElementContent = false;
            if (this.fPerformValidation && (value = this.fValidationState.checkIDRefID()) != null) {
                this.fErrorReporter.reportError("http://www.w3.org/TR/1998/REC-xml-19980210", "MSG_ELEMENT_WITH_ID_REQUIRED", new Object[]{value}, (short) 1);
                return;
            }
            return;
        }
        this.fCurrentElement.setValues(this.fElementQNamePartsStack[this.fElementDepth]);
        this.fCurrentElementIndex = this.fElementIndexStack[this.fElementDepth];
        this.fCurrentContentSpecType = this.fContentSpecTypeStack[this.fElementDepth];
        this.fInElementContent = this.fCurrentContentSpecType == 3;
    }

    protected void endNamespaceScope(QName element, Augmentations augs, boolean isEmpty) {
        if (this.fDocumentHandler != null && !isEmpty) {
            this.fDocumentHandler.endElement(this.fCurrentElement, augs);
        }
    }

    protected boolean isSpace(int c) {
        return XMLChar.isSpace(c);
    }

    @Override
    public boolean characterData(String data, Augmentations augs) {
        characters(new XMLString(data.toCharArray(), 0, data.length()), augs);
        return true;
    }
}
