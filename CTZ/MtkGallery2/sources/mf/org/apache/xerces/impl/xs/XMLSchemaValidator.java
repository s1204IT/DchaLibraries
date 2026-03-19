package mf.org.apache.xerces.impl.xs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.RevalidationHandler;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dv.DatatypeException;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.impl.validation.ConfigurableValidationState;
import mf.org.apache.xerces.impl.validation.ValidationManager;
import mf.org.apache.xerces.impl.validation.ValidationState;
import mf.org.apache.xerces.impl.xs.XMLSchemaLoader;
import mf.org.apache.xerces.impl.xs.identity.Field;
import mf.org.apache.xerces.impl.xs.identity.FieldActivator;
import mf.org.apache.xerces.impl.xs.identity.IdentityConstraint;
import mf.org.apache.xerces.impl.xs.identity.KeyRef;
import mf.org.apache.xerces.impl.xs.identity.Selector;
import mf.org.apache.xerces.impl.xs.identity.UniqueOrKey;
import mf.org.apache.xerces.impl.xs.identity.ValueStore;
import mf.org.apache.xerces.impl.xs.identity.XPathMatcher;
import mf.org.apache.xerces.impl.xs.models.CMBuilder;
import mf.org.apache.xerces.impl.xs.models.CMNodeFactory;
import mf.org.apache.xerces.impl.xs.models.XSCMValidator;
import mf.org.apache.xerces.util.AugmentationsImpl;
import mf.org.apache.xerces.util.IntStack;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.util.XMLAttributesImpl;
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
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLDocumentFilter;
import mf.org.apache.xerces.xni.parser.XMLDocumentSource;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xs.ShortList;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public class XMLSchemaValidator implements RevalidationHandler, XSElementDeclHelper, FieldActivator, XMLComponent, XMLDocumentFilter {
    private static final int BUFFER_SIZE = 20;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_NORMALIZATION = false;
    protected static final String ENTITY_MANAGER = "http://apache.org/xml/properties/internal/entity-manager";
    public static final String ENTITY_RESOLVER = "http://apache.org/xml/properties/internal/entity-resolver";
    public static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS = "http://apache.org/xml/features/generate-synthetic-annotations";
    protected static final int ID_CONSTRAINT_NUM = 1;
    static final int INC_STACK_SIZE = 8;
    static final int INITIAL_STACK_SIZE = 8;
    protected static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    protected static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    protected static final String NORMALIZE_DATA = "http://apache.org/xml/features/validation/schema/normalized-value";
    protected static final String PARSER_SETTINGS = "http://apache.org/xml/features/internal/parser-settings";
    protected static final String SCHEMA_AUGMENT_PSVI = "http://apache.org/xml/features/validation/schema/augment-psvi";
    protected static final String SCHEMA_ELEMENT_DEFAULT = "http://apache.org/xml/features/validation/schema/element-default";
    public static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    public static final String XMLGRAMMAR_POOL = "http://apache.org/xml/properties/internal/grammar-pool";
    private int[] fCurrCMState;
    private XSCMValidator fCurrentCM;
    private XSElementDecl fCurrentElemDecl;
    private XSTypeDefinition fCurrentType;
    protected XMLString fDefaultValue;
    protected XMLDocumentHandler fDocumentHandler;
    protected XMLDocumentSource fDocumentSource;
    private int fElementDepth;
    protected XMLEntityResolver fEntityResolver;
    protected XMLGrammarPool fGrammarPool;
    private boolean fIDCChecking;
    private int fIgnoreXSITypeDepth;
    private XMLLocator fLocator;
    private int fNFullValidationDepth;
    private int fNNoneValidationDepth;
    private boolean fNil;
    private XSNotationDecl fNotation;
    private int fSkipValidationDepth;
    private boolean fSubElement;
    protected SymbolTable fSymbolTable;
    private String fValidationRoot;
    protected static final String VALIDATION = "http://xml.org/sax/features/validation";
    protected static final String SCHEMA_VALIDATION = "http://apache.org/xml/features/validation/schema";
    protected static final String DYNAMIC_VALIDATION = "http://apache.org/xml/features/validation/dynamic";
    protected static final String SCHEMA_FULL_CHECKING = "http://apache.org/xml/features/validation/schema-full-checking";
    protected static final String ALLOW_JAVA_ENCODINGS = "http://apache.org/xml/features/allow-java-encodings";
    protected static final String CONTINUE_AFTER_FATAL_ERROR = "http://apache.org/xml/features/continue-after-fatal-error";
    protected static final String STANDARD_URI_CONFORMANT_FEATURE = "http://apache.org/xml/features/standard-uri-conformant";
    protected static final String VALIDATE_ANNOTATIONS = "http://apache.org/xml/features/validate-annotations";
    protected static final String HONOUR_ALL_SCHEMALOCATIONS = "http://apache.org/xml/features/honour-all-schemaLocations";
    protected static final String USE_GRAMMAR_POOL_ONLY = "http://apache.org/xml/features/internal/validation/schema/use-grammar-pool-only";
    protected static final String IGNORE_XSI_TYPE = "http://apache.org/xml/features/validation/schema/ignore-xsi-type-until-elemdecl";
    protected static final String ID_IDREF_CHECKING = "http://apache.org/xml/features/validation/id-idref-checking";
    protected static final String IDENTITY_CONSTRAINT_CHECKING = "http://apache.org/xml/features/validation/identity-constraint-checking";
    protected static final String UNPARSED_ENTITY_CHECKING = "http://apache.org/xml/features/validation/unparsed-entity-checking";
    protected static final String NAMESPACE_GROWTH = "http://apache.org/xml/features/namespace-growth";
    protected static final String TOLERATE_DUPLICATES = "http://apache.org/xml/features/internal/tolerate-duplicates";
    private static final String[] RECOGNIZED_FEATURES = {VALIDATION, SCHEMA_VALIDATION, DYNAMIC_VALIDATION, SCHEMA_FULL_CHECKING, ALLOW_JAVA_ENCODINGS, CONTINUE_AFTER_FATAL_ERROR, STANDARD_URI_CONFORMANT_FEATURE, "http://apache.org/xml/features/generate-synthetic-annotations", VALIDATE_ANNOTATIONS, HONOUR_ALL_SCHEMALOCATIONS, USE_GRAMMAR_POOL_ONLY, IGNORE_XSI_TYPE, ID_IDREF_CHECKING, IDENTITY_CONSTRAINT_CHECKING, UNPARSED_ENTITY_CHECKING, NAMESPACE_GROWTH, TOLERATE_DUPLICATES};
    private static final Boolean[] FEATURE_DEFAULTS = new Boolean[17];
    protected static final String VALIDATION_MANAGER = "http://apache.org/xml/properties/internal/validation-manager";
    protected static final String SCHEMA_LOCATION = "http://apache.org/xml/properties/schema/external-schemaLocation";
    protected static final String SCHEMA_NONS_LOCATION = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    protected static final String ROOT_TYPE_DEF = "http://apache.org/xml/properties/validation/schema/root-type-definition";
    protected static final String ROOT_ELEMENT_DECL = "http://apache.org/xml/properties/validation/schema/root-element-declaration";
    protected static final String SCHEMA_DV_FACTORY = "http://apache.org/xml/properties/internal/validation/schema/dv-factory";
    private static final String[] RECOGNIZED_PROPERTIES = {"http://apache.org/xml/properties/internal/symbol-table", "http://apache.org/xml/properties/internal/error-reporter", "http://apache.org/xml/properties/internal/entity-resolver", VALIDATION_MANAGER, SCHEMA_LOCATION, SCHEMA_NONS_LOCATION, "http://java.sun.com/xml/jaxp/properties/schemaSource", "http://java.sun.com/xml/jaxp/properties/schemaLanguage", ROOT_TYPE_DEF, ROOT_ELEMENT_DECL, SCHEMA_DV_FACTORY};
    private static final Object[] PROPERTY_DEFAULTS = new Object[11];
    static final XSAttributeDecl XSI_TYPE = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_TYPE);
    static final XSAttributeDecl XSI_NIL = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_NIL);
    static final XSAttributeDecl XSI_SCHEMALOCATION = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_SCHEMALOCATION);
    static final XSAttributeDecl XSI_NONAMESPACESCHEMALOCATION = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_NONAMESPACESCHEMALOCATION);
    private static final Hashtable EMPTY_TABLE = new Hashtable();
    protected ElementPSVImpl fCurrentPSVI = new ElementPSVImpl();
    protected final AugmentationsImpl fAugmentations = new AugmentationsImpl();
    protected boolean fDynamicValidation = false;
    protected boolean fSchemaDynamicValidation = false;
    protected boolean fDoValidation = false;
    protected boolean fFullChecking = false;
    protected boolean fNormalizeData = true;
    protected boolean fSchemaElementDefault = true;
    protected boolean fAugPSVI = true;
    protected boolean fIdConstraint = false;
    protected boolean fUseGrammarPoolOnly = false;
    protected boolean fNamespaceGrowth = false;
    private String fSchemaType = null;
    protected boolean fEntityRef = false;
    protected boolean fInCDATA = false;
    protected final XSIErrorReporter fXSIErrorReporter = new XSIErrorReporter();
    protected ValidationManager fValidationManager = null;
    protected ConfigurableValidationState fValidationState = new ConfigurableValidationState();
    protected String fExternalSchemas = null;
    protected String fExternalNoNamespaceSchema = null;
    protected Object fJaxpSchemaSource = null;
    protected final XSDDescription fXSDDescription = new XSDDescription();
    protected final Hashtable fLocationPairs = new Hashtable();
    protected final Hashtable fExpandedLocationPairs = new Hashtable();
    protected final ArrayList fUnparsedLocations = new ArrayList();
    private final XMLString fEmptyXMLStr = new XMLString(null, 0, -1);
    private final XMLString fNormalizedStr = new XMLString();
    private boolean fFirstChunk = true;
    private boolean fTrailing = false;
    private short fWhiteSpace = -1;
    private boolean fUnionType = false;
    private final XSGrammarBucket fGrammarBucket = new XSGrammarBucket();
    private final SubstitutionGroupHandler fSubGroupHandler = new SubstitutionGroupHandler(this);
    private final XSSimpleType fQNameDV = (XSSimpleType) SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_QNAME);
    private final CMNodeFactory nodeFactory = new CMNodeFactory();
    private final CMBuilder fCMBuilder = new CMBuilder(this.nodeFactory);
    private final XMLSchemaLoader fSchemaLoader = new XMLSchemaLoader(this.fXSIErrorReporter.fErrorReporter, this.fGrammarBucket, this.fSubGroupHandler, this.fCMBuilder);
    private boolean[] fSubElementStack = new boolean[8];
    private XSElementDecl[] fElemDeclStack = new XSElementDecl[8];
    private boolean[] fNilStack = new boolean[8];
    private XSNotationDecl[] fNotationStack = new XSNotationDecl[8];
    private XSTypeDefinition[] fTypeStack = new XSTypeDefinition[8];
    private XSCMValidator[] fCMStack = new XSCMValidator[8];
    private int[][] fCMStateStack = new int[8][];
    private boolean fStrictAssess = true;
    private boolean[] fStrictAssessStack = new boolean[8];
    private final StringBuffer fBuffer = new StringBuffer();
    private boolean fAppendBuffer = true;
    private boolean fSawText = false;
    private boolean[] fSawTextStack = new boolean[8];
    private boolean fSawCharacters = false;
    private boolean[] fStringContent = new boolean[8];
    private final QName fTempQName = new QName();
    private javax.xml.namespace.QName fRootTypeQName = null;
    private XSTypeDefinition fRootTypeDefinition = null;
    private javax.xml.namespace.QName fRootElementDeclQName = null;
    private XSElementDecl fRootElementDeclaration = null;
    private ValidatedInfo fValidatedInfo = new ValidatedInfo();
    private ValidationState fState4XsiType = new ValidationState();
    private ValidationState fState4ApplyDefault = new ValidationState();
    protected XPathMatcherStack fMatcherStack = new XPathMatcherStack();
    protected ValueStoreCache fValueStoreCache = new ValueStoreCache();

    protected final class XSIErrorReporter {
        int fContextCount;
        XMLErrorReporter fErrorReporter;
        Vector fErrors = new Vector();
        int[] fContext = new int[8];

        protected XSIErrorReporter() {
        }

        public void reset(XMLErrorReporter errorReporter) {
            this.fErrorReporter = errorReporter;
            this.fErrors.removeAllElements();
            this.fContextCount = 0;
        }

        public void pushContext() {
            if (!XMLSchemaValidator.this.fAugPSVI) {
                return;
            }
            if (this.fContextCount == this.fContext.length) {
                int newSize = this.fContextCount + 8;
                int[] newArray = new int[newSize];
                System.arraycopy(this.fContext, 0, newArray, 0, this.fContextCount);
                this.fContext = newArray;
            }
            int[] iArr = this.fContext;
            int i = this.fContextCount;
            this.fContextCount = i + 1;
            iArr[i] = this.fErrors.size();
        }

        public String[] popContext() {
            if (!XMLSchemaValidator.this.fAugPSVI) {
                return null;
            }
            int[] iArr = this.fContext;
            int i = this.fContextCount - 1;
            this.fContextCount = i;
            int contextPos = iArr[i];
            int size = this.fErrors.size() - contextPos;
            if (size == 0) {
                return null;
            }
            String[] errors = new String[size];
            for (int i2 = 0; i2 < size; i2++) {
                errors[i2] = (String) this.fErrors.elementAt(contextPos + i2);
            }
            this.fErrors.setSize(contextPos);
            return errors;
        }

        public String[] mergeContext() {
            if (!XMLSchemaValidator.this.fAugPSVI) {
                return null;
            }
            int[] iArr = this.fContext;
            int i = this.fContextCount - 1;
            this.fContextCount = i;
            int contextPos = iArr[i];
            int size = this.fErrors.size() - contextPos;
            if (size == 0) {
                return null;
            }
            String[] errors = new String[size];
            for (int i2 = 0; i2 < size; i2++) {
                errors[i2] = (String) this.fErrors.elementAt(contextPos + i2);
            }
            return errors;
        }

        public void reportError(String domain, String key, Object[] arguments, short severity) throws XNIException {
            String message = this.fErrorReporter.reportError(domain, key, arguments, severity);
            if (XMLSchemaValidator.this.fAugPSVI) {
                this.fErrors.addElement(key);
                this.fErrors.addElement(message);
            }
        }

        public void reportError(XMLLocator location, String domain, String key, Object[] arguments, short severity) throws XNIException {
            String message = this.fErrorReporter.reportError(location, domain, key, arguments, severity);
            if (XMLSchemaValidator.this.fAugPSVI) {
                this.fErrors.addElement(key);
                this.fErrors.addElement(message);
            }
        }
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
    public void setProperty(String propertyId, Object obj) throws XMLConfigurationException {
        if (propertyId.equals(ROOT_TYPE_DEF)) {
            if (obj == 0) {
                this.fRootTypeQName = null;
                this.fRootTypeDefinition = null;
                return;
            } else if (obj instanceof javax.xml.namespace.QName) {
                this.fRootTypeQName = obj;
                this.fRootTypeDefinition = null;
                return;
            } else {
                this.fRootTypeDefinition = (XSTypeDefinition) obj;
                this.fRootTypeQName = null;
                return;
            }
        }
        if (propertyId.equals(ROOT_ELEMENT_DECL)) {
            if (obj == 0) {
                this.fRootElementDeclQName = null;
                this.fRootElementDeclaration = null;
            } else if (obj instanceof javax.xml.namespace.QName) {
                this.fRootElementDeclQName = obj;
                this.fRootElementDeclaration = null;
            } else {
                this.fRootElementDeclaration = (XSElementDecl) obj;
                this.fRootElementDeclQName = null;
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
        this.fValidationState.setNamespaceSupport(namespaceContext);
        this.fState4XsiType.setNamespaceSupport(namespaceContext);
        this.fState4ApplyDefault.setNamespaceSupport(namespaceContext);
        this.fLocator = locator;
        handleStartDocument(locator, encoding);
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startDocument(locator, encoding, namespaceContext, augs);
        }
    }

    @Override
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    }

    @Override
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.doctypeDecl(rootElement, publicId, systemId, augs);
        }
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        Augmentations modifiedAugs = handleStartElement(element, attributes, augs);
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startElement(element, attributes, modifiedAugs);
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws URI.MalformedURIException, InvalidDatatypeValueException, XNIException {
        Augmentations modifiedAugs = handleStartElement(element, attributes, augs);
        this.fDefaultValue = null;
        if (this.fElementDepth != -2) {
            modifiedAugs = handleEndElement(element, modifiedAugs);
        }
        if (this.fDocumentHandler != null) {
            if (!this.fSchemaElementDefault || this.fDefaultValue == null) {
                this.fDocumentHandler.emptyElement(element, attributes, modifiedAugs);
                return;
            }
            this.fDocumentHandler.startElement(element, attributes, modifiedAugs);
            this.fDocumentHandler.characters(this.fDefaultValue, null);
            this.fDocumentHandler.endElement(element, modifiedAugs);
        }
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        XMLString text2 = handleCharacters(text);
        if (this.fDocumentHandler != null) {
            if (this.fNormalizeData && this.fUnionType) {
                if (augs != null) {
                    this.fDocumentHandler.characters(this.fEmptyXMLStr, augs);
                    return;
                }
                return;
            }
            this.fDocumentHandler.characters(text2, augs);
        }
    }

    @Override
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        handleIgnorableWhitespace(text);
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.ignorableWhitespace(text, augs);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws InvalidDatatypeValueException, XNIException {
        this.fDefaultValue = null;
        Augmentations modifiedAugs = handleEndElement(element, augs);
        if (this.fDocumentHandler != null) {
            if (this.fSchemaElementDefault && this.fDefaultValue != null) {
                this.fDocumentHandler.characters(this.fDefaultValue, null);
                this.fDocumentHandler.endElement(element, modifiedAugs);
            } else {
                this.fDocumentHandler.endElement(element, modifiedAugs);
            }
        }
    }

    @Override
    public void startCDATA(Augmentations augs) throws XNIException {
        this.fInCDATA = true;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startCDATA(augs);
        }
    }

    @Override
    public void endCDATA(Augmentations augs) throws XNIException {
        this.fInCDATA = false;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endCDATA(augs);
        }
    }

    @Override
    public void endDocument(Augmentations augs) throws XNIException {
        handleEndDocument();
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endDocument(augs);
        }
        this.fLocator = null;
    }

    @Override
    public boolean characterData(String data, Augmentations augs) {
        this.fSawText = this.fSawText || data.length() > 0;
        if (this.fNormalizeData && this.fWhiteSpace != -1 && this.fWhiteSpace != 0) {
            normalizeWhitespace(data, this.fWhiteSpace == 2);
            this.fBuffer.append(this.fNormalizedStr.ch, this.fNormalizedStr.offset, this.fNormalizedStr.length);
        } else if (this.fAppendBuffer) {
            this.fBuffer.append(data);
        }
        if (this.fCurrentType == null || this.fCurrentType.getTypeCategory() != 15) {
            return true;
        }
        XSComplexTypeDecl ctype = (XSComplexTypeDecl) this.fCurrentType;
        if (ctype.fContentType != 2) {
            return true;
        }
        for (int i = 0; i < data.length(); i++) {
            if (!XMLChar.isSpace(data.charAt(i))) {
                this.fSawCharacters = true;
                return false;
            }
        }
        return true;
    }

    public void elementDefault(String data) {
    }

    @Override
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
        this.fEntityRef = true;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs);
        }
    }

    @Override
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.textDecl(version, encoding, augs);
        }
    }

    @Override
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.comment(text, augs);
        }
    }

    @Override
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.processingInstruction(target, data, augs);
        }
    }

    @Override
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        this.fEntityRef = false;
        if (this.fDocumentHandler != null) {
            this.fDocumentHandler.endGeneralEntity(name, augs);
        }
    }

    public XMLSchemaValidator() {
        this.fState4XsiType.setExtraChecking(false);
        this.fState4ApplyDefault.setFacetChecking(false);
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        boolean parser_settings;
        boolean ignoreXSIType;
        int i = 0;
        this.fIdConstraint = false;
        this.fLocationPairs.clear();
        this.fExpandedLocationPairs.clear();
        this.fValidationState.resetIDTables();
        this.fSchemaLoader.reset(componentManager);
        this.fCurrentElemDecl = null;
        this.fCurrentCM = null;
        this.fCurrCMState = null;
        this.fSkipValidationDepth = -1;
        this.fNFullValidationDepth = -1;
        this.fNNoneValidationDepth = -1;
        this.fElementDepth = -1;
        this.fSubElement = false;
        this.fSchemaDynamicValidation = false;
        this.fEntityRef = false;
        this.fInCDATA = false;
        this.fMatcherStack.clear();
        this.fXSIErrorReporter.reset((XMLErrorReporter) componentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter"));
        try {
            parser_settings = componentManager.getFeature(PARSER_SETTINGS);
        } catch (XMLConfigurationException e) {
            parser_settings = true;
        }
        if (!parser_settings) {
            this.fValidationManager.addValidationState(this.fValidationState);
            this.nodeFactory.reset();
            XMLSchemaLoader.processExternalHints(this.fExternalSchemas, this.fExternalNoNamespaceSchema, this.fLocationPairs, this.fXSIErrorReporter.fErrorReporter);
            return;
        }
        this.nodeFactory.reset(componentManager);
        SymbolTable symbolTable = (SymbolTable) componentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        if (symbolTable != this.fSymbolTable) {
            this.fSymbolTable = symbolTable;
        }
        try {
            this.fNamespaceGrowth = componentManager.getFeature(NAMESPACE_GROWTH);
        } catch (XMLConfigurationException e2) {
            this.fNamespaceGrowth = false;
        }
        try {
            this.fDynamicValidation = componentManager.getFeature(DYNAMIC_VALIDATION);
        } catch (XMLConfigurationException e3) {
            this.fDynamicValidation = false;
        }
        if (this.fDynamicValidation) {
            this.fDoValidation = true;
        } else {
            try {
                this.fDoValidation = componentManager.getFeature(VALIDATION);
            } catch (XMLConfigurationException e4) {
                this.fDoValidation = false;
            }
        }
        if (this.fDoValidation) {
            try {
                this.fDoValidation = componentManager.getFeature(SCHEMA_VALIDATION);
            } catch (XMLConfigurationException e5) {
            }
        }
        try {
            this.fFullChecking = componentManager.getFeature(SCHEMA_FULL_CHECKING);
        } catch (XMLConfigurationException e6) {
            this.fFullChecking = false;
        }
        try {
            this.fNormalizeData = componentManager.getFeature(NORMALIZE_DATA);
        } catch (XMLConfigurationException e7) {
            this.fNormalizeData = false;
        }
        try {
            this.fSchemaElementDefault = componentManager.getFeature(SCHEMA_ELEMENT_DEFAULT);
        } catch (XMLConfigurationException e8) {
            this.fSchemaElementDefault = false;
        }
        try {
            this.fAugPSVI = componentManager.getFeature(SCHEMA_AUGMENT_PSVI);
        } catch (XMLConfigurationException e9) {
            this.fAugPSVI = true;
        }
        try {
            this.fSchemaType = (String) componentManager.getProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage");
        } catch (XMLConfigurationException e10) {
            this.fSchemaType = null;
        }
        try {
            this.fUseGrammarPoolOnly = componentManager.getFeature(USE_GRAMMAR_POOL_ONLY);
        } catch (XMLConfigurationException e11) {
            this.fUseGrammarPoolOnly = false;
        }
        this.fEntityResolver = (XMLEntityResolver) componentManager.getProperty(ENTITY_MANAGER);
        this.fValidationManager = (ValidationManager) componentManager.getProperty(VALIDATION_MANAGER);
        this.fValidationManager.addValidationState(this.fValidationState);
        this.fValidationState.setSymbolTable(this.fSymbolTable);
        try {
            ?? property = componentManager.getProperty(ROOT_TYPE_DEF);
            if (property == 0) {
                this.fRootTypeQName = null;
                this.fRootTypeDefinition = null;
            } else if (property instanceof javax.xml.namespace.QName) {
                this.fRootTypeQName = property;
                this.fRootTypeDefinition = null;
            } else {
                this.fRootTypeDefinition = (XSTypeDefinition) property;
                this.fRootTypeQName = null;
            }
        } catch (XMLConfigurationException e12) {
            this.fRootTypeQName = null;
            this.fRootTypeDefinition = null;
        }
        try {
            ?? property2 = componentManager.getProperty(ROOT_ELEMENT_DECL);
            if (property2 == 0) {
                this.fRootElementDeclQName = null;
                this.fRootElementDeclaration = null;
            } else if (property2 instanceof javax.xml.namespace.QName) {
                this.fRootElementDeclQName = property2;
                this.fRootElementDeclaration = null;
            } else {
                this.fRootElementDeclaration = (XSElementDecl) property2;
                this.fRootElementDeclQName = null;
            }
        } catch (XMLConfigurationException e13) {
            this.fRootElementDeclQName = null;
            this.fRootElementDeclaration = null;
        }
        try {
            ignoreXSIType = componentManager.getFeature(IGNORE_XSI_TYPE);
        } catch (XMLConfigurationException e14) {
            ignoreXSIType = false;
        }
        if (!ignoreXSIType) {
            i = -1;
        }
        this.fIgnoreXSITypeDepth = i;
        try {
            this.fIDCChecking = componentManager.getFeature(IDENTITY_CONSTRAINT_CHECKING);
        } catch (XMLConfigurationException e15) {
            this.fIDCChecking = true;
        }
        try {
            this.fValidationState.setIdIdrefChecking(componentManager.getFeature(ID_IDREF_CHECKING));
        } catch (XMLConfigurationException e16) {
            this.fValidationState.setIdIdrefChecking(true);
        }
        try {
            this.fValidationState.setUnparsedEntityChecking(componentManager.getFeature(UNPARSED_ENTITY_CHECKING));
        } catch (XMLConfigurationException e17) {
            this.fValidationState.setUnparsedEntityChecking(true);
        }
        try {
            this.fExternalSchemas = (String) componentManager.getProperty(SCHEMA_LOCATION);
            this.fExternalNoNamespaceSchema = (String) componentManager.getProperty(SCHEMA_NONS_LOCATION);
        } catch (XMLConfigurationException e18) {
            this.fExternalSchemas = null;
            this.fExternalNoNamespaceSchema = null;
        }
        XMLSchemaLoader.processExternalHints(this.fExternalSchemas, this.fExternalNoNamespaceSchema, this.fLocationPairs, this.fXSIErrorReporter.fErrorReporter);
        try {
            this.fJaxpSchemaSource = componentManager.getProperty("http://java.sun.com/xml/jaxp/properties/schemaSource");
        } catch (XMLConfigurationException e19) {
            this.fJaxpSchemaSource = null;
        }
        try {
            this.fGrammarPool = (XMLGrammarPool) componentManager.getProperty("http://apache.org/xml/properties/internal/grammar-pool");
        } catch (XMLConfigurationException e20) {
            this.fGrammarPool = null;
        }
        this.fState4XsiType.setSymbolTable(symbolTable);
        this.fState4ApplyDefault.setSymbolTable(symbolTable);
    }

    @Override
    public void startValueScopeFor(IdentityConstraint identityConstraint, int initialDepth) {
        ValueStoreBase valueStore = this.fValueStoreCache.getValueStoreFor(identityConstraint, initialDepth);
        valueStore.startValueScope();
    }

    @Override
    public XPathMatcher activateField(Field field, int initialDepth) {
        ValueStore valueStore = this.fValueStoreCache.getValueStoreFor(field.getIdentityConstraint(), initialDepth);
        XPathMatcher matcher = field.createMatcher(valueStore);
        this.fMatcherStack.addMatcher(matcher);
        matcher.startDocumentFragment();
        return matcher;
    }

    @Override
    public void endValueScopeFor(IdentityConstraint identityConstraint, int initialDepth) {
        ValueStoreBase valueStore = this.fValueStoreCache.getValueStoreFor(identityConstraint, initialDepth);
        valueStore.endValueScope();
    }

    private void activateSelectorFor(IdentityConstraint ic) {
        Selector selector = ic.getSelector();
        if (selector != null) {
            XPathMatcher matcher = selector.createMatcher(this, this.fElementDepth);
            this.fMatcherStack.addMatcher(matcher);
            matcher.startDocumentFragment();
        }
    }

    @Override
    public XSElementDecl getGlobalElementDecl(QName element) {
        SchemaGrammar sGrammar = findSchemaGrammar((short) 5, element.uri, null, element, null);
        if (sGrammar != null) {
            return sGrammar.getGlobalElementDecl(element.localpart);
        }
        return null;
    }

    void ensureStackCapacity() {
        if (this.fElementDepth == this.fElemDeclStack.length) {
            int newSize = this.fElementDepth + 8;
            boolean[] newArrayB = new boolean[newSize];
            System.arraycopy(this.fSubElementStack, 0, newArrayB, 0, this.fElementDepth);
            this.fSubElementStack = newArrayB;
            XSElementDecl[] newArrayE = new XSElementDecl[newSize];
            System.arraycopy(this.fElemDeclStack, 0, newArrayE, 0, this.fElementDepth);
            this.fElemDeclStack = newArrayE;
            boolean[] newArrayB2 = new boolean[newSize];
            System.arraycopy(this.fNilStack, 0, newArrayB2, 0, this.fElementDepth);
            this.fNilStack = newArrayB2;
            XSNotationDecl[] newArrayN = new XSNotationDecl[newSize];
            System.arraycopy(this.fNotationStack, 0, newArrayN, 0, this.fElementDepth);
            this.fNotationStack = newArrayN;
            XSTypeDefinition[] newArrayT = new XSTypeDefinition[newSize];
            System.arraycopy(this.fTypeStack, 0, newArrayT, 0, this.fElementDepth);
            this.fTypeStack = newArrayT;
            XSCMValidator[] newArrayC = new XSCMValidator[newSize];
            System.arraycopy(this.fCMStack, 0, newArrayC, 0, this.fElementDepth);
            this.fCMStack = newArrayC;
            boolean[] newArrayB3 = new boolean[newSize];
            System.arraycopy(this.fSawTextStack, 0, newArrayB3, 0, this.fElementDepth);
            this.fSawTextStack = newArrayB3;
            boolean[] newArrayB4 = new boolean[newSize];
            System.arraycopy(this.fStringContent, 0, newArrayB4, 0, this.fElementDepth);
            this.fStringContent = newArrayB4;
            boolean[] newArrayB5 = new boolean[newSize];
            System.arraycopy(this.fStrictAssessStack, 0, newArrayB5, 0, this.fElementDepth);
            this.fStrictAssessStack = newArrayB5;
            int[][] newArrayIA = new int[newSize][];
            System.arraycopy(this.fCMStateStack, 0, newArrayIA, 0, this.fElementDepth);
            this.fCMStateStack = newArrayIA;
        }
    }

    void handleStartDocument(XMLLocator locator, String encoding) {
        if (this.fIDCChecking) {
            this.fValueStoreCache.startDocument();
        }
        if (this.fAugPSVI) {
            this.fCurrentPSVI.fGrammars = null;
            this.fCurrentPSVI.fSchemaInformation = null;
        }
    }

    void handleEndDocument() {
        if (this.fIDCChecking) {
            this.fValueStoreCache.endDocument();
        }
    }

    XMLString handleCharacters(XMLString text) {
        if (this.fSkipValidationDepth >= 0) {
            return text;
        }
        this.fSawText = this.fSawText || text.length > 0;
        if (this.fNormalizeData && this.fWhiteSpace != -1 && this.fWhiteSpace != 0) {
            normalizeWhitespace(text, this.fWhiteSpace == 2);
            text = this.fNormalizedStr;
        }
        if (this.fAppendBuffer) {
            this.fBuffer.append(text.ch, text.offset, text.length);
        }
        if (this.fCurrentType != null && this.fCurrentType.getTypeCategory() == 15) {
            XSComplexTypeDecl ctype = (XSComplexTypeDecl) this.fCurrentType;
            if (ctype.fContentType == 2) {
                int i = text.offset;
                while (true) {
                    if (i >= text.offset + text.length) {
                        break;
                    }
                    if (XMLChar.isSpace(text.ch[i])) {
                        i++;
                    } else {
                        this.fSawCharacters = true;
                        break;
                    }
                }
            }
        }
        return text;
    }

    private void normalizeWhitespace(XMLString value, boolean collapse) {
        boolean skipSpace = collapse;
        boolean sawNonWS = false;
        boolean leading = false;
        boolean trailing = false;
        int size = value.offset + value.length;
        if (this.fNormalizedStr.ch == null || this.fNormalizedStr.ch.length < value.length + 1) {
            this.fNormalizedStr.ch = new char[value.length + 1];
        }
        this.fNormalizedStr.offset = 1;
        this.fNormalizedStr.length = 1;
        for (int i = value.offset; i < size; i++) {
            char c = value.ch[i];
            if (XMLChar.isSpace(c)) {
                if (!skipSpace) {
                    char[] cArr = this.fNormalizedStr.ch;
                    XMLString xMLString = this.fNormalizedStr;
                    int i2 = xMLString.length;
                    xMLString.length = i2 + 1;
                    cArr[i2] = ' ';
                    skipSpace = collapse;
                }
                if (!sawNonWS) {
                    leading = true;
                }
            } else {
                char[] cArr2 = this.fNormalizedStr.ch;
                XMLString xMLString2 = this.fNormalizedStr;
                int i3 = xMLString2.length;
                xMLString2.length = i3 + 1;
                cArr2[i3] = c;
                skipSpace = false;
                sawNonWS = true;
            }
        }
        if (skipSpace) {
            if (this.fNormalizedStr.length > 1) {
                this.fNormalizedStr.length--;
                trailing = true;
            } else if (leading && !this.fFirstChunk) {
                trailing = true;
            }
        }
        if (this.fNormalizedStr.length > 1 && !this.fFirstChunk && this.fWhiteSpace == 2 && (this.fTrailing || leading)) {
            this.fNormalizedStr.offset = 0;
            this.fNormalizedStr.ch[0] = ' ';
        }
        this.fNormalizedStr.length -= this.fNormalizedStr.offset;
        this.fTrailing = trailing;
        if (trailing || sawNonWS) {
            this.fFirstChunk = false;
        }
    }

    private void normalizeWhitespace(String value, boolean collapse) {
        boolean skipSpace = collapse;
        int size = value.length();
        if (this.fNormalizedStr.ch == null || this.fNormalizedStr.ch.length < size) {
            this.fNormalizedStr.ch = new char[size];
        }
        this.fNormalizedStr.offset = 0;
        this.fNormalizedStr.length = 0;
        for (int i = 0; i < size; i++) {
            char c = value.charAt(i);
            if (XMLChar.isSpace(c)) {
                if (!skipSpace) {
                    char[] cArr = this.fNormalizedStr.ch;
                    XMLString xMLString = this.fNormalizedStr;
                    int i2 = xMLString.length;
                    xMLString.length = i2 + 1;
                    cArr[i2] = ' ';
                    skipSpace = collapse;
                }
            } else {
                char[] cArr2 = this.fNormalizedStr.ch;
                XMLString xMLString2 = this.fNormalizedStr;
                int i3 = xMLString2.length;
                xMLString2.length = i3 + 1;
                cArr2[i3] = c;
                skipSpace = false;
            }
        }
        if (skipSpace && this.fNormalizedStr.length != 0) {
            XMLString xMLString3 = this.fNormalizedStr;
            xMLString3.length--;
        }
    }

    void handleIgnorableWhitespace(XMLString text) {
        if (this.fSkipValidationDepth >= 0) {
        }
    }

    Augmentations handleStartElement(QName element, XMLAttributes attributes, Augmentations augs) throws URI.MalformedURIException {
        Object decl;
        boolean z;
        boolean z2;
        SchemaGrammar sGrammar;
        Object decl2;
        if (this.fElementDepth == -1 && this.fValidationManager.isGrammarFound() && this.fSchemaType == null) {
            this.fSchemaDynamicValidation = true;
        }
        if (!this.fUseGrammarPoolOnly) {
            String sLocation = attributes.getValue(SchemaSymbols.URI_XSI, SchemaSymbols.XSI_SCHEMALOCATION);
            String nsLocation = attributes.getValue(SchemaSymbols.URI_XSI, SchemaSymbols.XSI_NONAMESPACESCHEMALOCATION);
            storeLocations(sLocation, nsLocation);
        }
        if (this.fSkipValidationDepth >= 0) {
            this.fElementDepth++;
            if (!this.fAugPSVI) {
                return augs;
            }
            return getEmptyAugs(augs);
        }
        if (this.fCurrentCM == null) {
            decl = null;
        } else {
            Object decl3 = this.fCurrentCM.oneTransition(element, this.fCurrCMState, this.fSubGroupHandler);
            if (this.fCurrCMState[0] == -1) {
                XSComplexTypeDecl ctype = (XSComplexTypeDecl) this.fCurrentType;
                if (ctype.fParticle != null) {
                    Vector next = this.fCurrentCM.whatCanGoHere(this.fCurrCMState);
                    if (next.size() > 0) {
                        String expected = expectedStr(next);
                        int[] occurenceInfo = this.fCurrentCM.occurenceInfo(this.fCurrCMState);
                        if (occurenceInfo != null) {
                            int minOccurs = occurenceInfo[0];
                            int maxOccurs = occurenceInfo[1];
                            int count = occurenceInfo[2];
                            if (count < minOccurs) {
                                int required = minOccurs - count;
                                if (required > 1) {
                                    decl2 = decl3;
                                    Object decl4 = element.rawname;
                                    reportSchemaError("cvc-complex-type.2.4.h", new Object[]{decl4, this.fCurrentCM.getTermName(occurenceInfo[3]), Integer.toString(minOccurs), Integer.toString(required)});
                                } else {
                                    decl2 = decl3;
                                    reportSchemaError("cvc-complex-type.2.4.g", new Object[]{element.rawname, this.fCurrentCM.getTermName(occurenceInfo[3]), Integer.toString(minOccurs)});
                                }
                            } else {
                                decl2 = decl3;
                                if (count >= maxOccurs && maxOccurs != -1) {
                                    reportSchemaError("cvc-complex-type.2.4.e", new Object[]{element.rawname, expected, Integer.toString(maxOccurs)});
                                } else {
                                    reportSchemaError("cvc-complex-type.2.4.a", new Object[]{element.rawname, expected});
                                }
                            }
                        } else {
                            decl2 = decl3;
                            reportSchemaError("cvc-complex-type.2.4.a", new Object[]{element.rawname, expected});
                        }
                    } else {
                        decl2 = decl3;
                        int[] occurenceInfo2 = this.fCurrentCM.occurenceInfo(this.fCurrCMState);
                        if (occurenceInfo2 != null) {
                            int maxOccurs2 = occurenceInfo2[1];
                            int count2 = occurenceInfo2[2];
                            if (count2 >= maxOccurs2 && maxOccurs2 != -1) {
                                reportSchemaError("cvc-complex-type.2.4.f", new Object[]{element.rawname, Integer.toString(maxOccurs2)});
                            } else {
                                reportSchemaError("cvc-complex-type.2.4.d", new Object[]{element.rawname});
                            }
                        } else {
                            reportSchemaError("cvc-complex-type.2.4.d", new Object[]{element.rawname});
                        }
                    }
                }
            } else {
                decl2 = decl3;
            }
            decl = decl2;
        }
        if (this.fElementDepth != -1) {
            ensureStackCapacity();
            this.fSubElementStack[this.fElementDepth] = true;
            this.fSubElement = false;
            this.fElemDeclStack[this.fElementDepth] = this.fCurrentElemDecl;
            this.fNilStack[this.fElementDepth] = this.fNil;
            this.fNotationStack[this.fElementDepth] = this.fNotation;
            this.fTypeStack[this.fElementDepth] = this.fCurrentType;
            this.fStrictAssessStack[this.fElementDepth] = this.fStrictAssess;
            this.fCMStack[this.fElementDepth] = this.fCurrentCM;
            this.fCMStateStack[this.fElementDepth] = this.fCurrCMState;
            this.fSawTextStack[this.fElementDepth] = this.fSawText;
            this.fStringContent[this.fElementDepth] = this.fSawCharacters;
        }
        this.fElementDepth++;
        this.fCurrentElemDecl = null;
        XSWildcardDecl wildcard = null;
        this.fCurrentType = null;
        this.fStrictAssess = true;
        this.fNil = false;
        this.fNotation = null;
        this.fBuffer.setLength(0);
        this.fSawText = false;
        this.fSawCharacters = false;
        if (decl != null) {
            if (decl instanceof XSElementDecl) {
                this.fCurrentElemDecl = decl;
            } else {
                wildcard = (XSWildcardDecl) decl;
            }
        }
        XSWildcardDecl wildcard2 = wildcard;
        if (wildcard2 != null && wildcard2.fProcessContents == 2) {
            this.fSkipValidationDepth = this.fElementDepth;
            if (!this.fAugPSVI) {
                return augs;
            }
            return getEmptyAugs(augs);
        }
        if (this.fElementDepth == 0) {
            if (this.fRootElementDeclaration != null) {
                this.fCurrentElemDecl = this.fRootElementDeclaration;
                checkElementMatchesRootElementDecl(this.fCurrentElemDecl, element);
            } else if (this.fRootElementDeclQName != null) {
                processRootElementDeclQName(this.fRootElementDeclQName, element);
            } else if (this.fRootTypeDefinition != null) {
                this.fCurrentType = this.fRootTypeDefinition;
            } else if (this.fRootTypeQName != null) {
                processRootTypeQName(this.fRootTypeQName);
            }
        }
        if (this.fCurrentType == null) {
            if (this.fCurrentElemDecl == null && (sGrammar = findSchemaGrammar((short) 5, element.uri, null, element, attributes)) != null) {
                this.fCurrentElemDecl = sGrammar.getGlobalElementDecl(element.localpart);
            }
            if (this.fCurrentElemDecl != null) {
                this.fCurrentType = this.fCurrentElemDecl.fType;
            }
        }
        if (this.fElementDepth == this.fIgnoreXSITypeDepth && this.fCurrentElemDecl == null) {
            this.fIgnoreXSITypeDepth++;
        }
        String xsiType = null;
        if (this.fElementDepth >= this.fIgnoreXSITypeDepth) {
            xsiType = attributes.getValue(SchemaSymbols.URI_XSI, SchemaSymbols.XSI_TYPE);
        }
        String xsiType2 = xsiType;
        if (this.fCurrentType == null && xsiType2 == null) {
            if (this.fElementDepth == 0) {
                if (this.fDynamicValidation || this.fSchemaDynamicValidation) {
                    if (this.fDocumentSource != null) {
                        this.fDocumentSource.setDocumentHandler(this.fDocumentHandler);
                        if (this.fDocumentHandler != null) {
                            this.fDocumentHandler.setDocumentSource(this.fDocumentSource);
                        }
                        this.fElementDepth = -2;
                        return augs;
                    }
                    this.fSkipValidationDepth = this.fElementDepth;
                    if (!this.fAugPSVI) {
                        return augs;
                    }
                    return getEmptyAugs(augs);
                }
                this.fXSIErrorReporter.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "cvc-elt.1.a", new Object[]{element.rawname}, (short) 1);
            } else {
                if (wildcard2 != null && wildcard2.fProcessContents == 1) {
                    z2 = false;
                    reportSchemaError("cvc-complex-type.2.4.c", new Object[]{element.rawname});
                }
                this.fCurrentType = SchemaGrammar.fAnyType;
                this.fStrictAssess = z2;
                this.fNFullValidationDepth = this.fElementDepth;
                this.fAppendBuffer = z2;
                this.fXSIErrorReporter.pushContext();
            }
            z2 = false;
            this.fCurrentType = SchemaGrammar.fAnyType;
            this.fStrictAssess = z2;
            this.fNFullValidationDepth = this.fElementDepth;
            this.fAppendBuffer = z2;
            this.fXSIErrorReporter.pushContext();
        } else {
            this.fXSIErrorReporter.pushContext();
            if (xsiType2 != null) {
                XSTypeDefinition oldType = this.fCurrentType;
                this.fCurrentType = getAndCheckXsiType(element, xsiType2, attributes);
                if (this.fCurrentType == null) {
                    if (oldType == null) {
                        this.fCurrentType = SchemaGrammar.fAnyType;
                    } else {
                        this.fCurrentType = oldType;
                    }
                }
            }
            this.fNNoneValidationDepth = this.fElementDepth;
            if ((this.fCurrentElemDecl != null && this.fCurrentElemDecl.getConstraintType() == 2) || this.fCurrentType.getTypeCategory() == 16) {
                this.fAppendBuffer = true;
            } else {
                XSComplexTypeDecl ctype2 = (XSComplexTypeDecl) this.fCurrentType;
                this.fAppendBuffer = ctype2.fContentType == 1;
            }
        }
        if (this.fCurrentElemDecl != null && this.fCurrentElemDecl.getAbstract()) {
            reportSchemaError("cvc-elt.2", new Object[]{element.rawname});
        }
        if (this.fElementDepth == 0) {
            this.fValidationRoot = element.rawname;
        }
        if (this.fNormalizeData) {
            this.fFirstChunk = true;
            this.fTrailing = false;
            this.fUnionType = false;
            this.fWhiteSpace = (short) -1;
        }
        if (this.fCurrentType.getTypeCategory() == 15) {
            XSComplexTypeDecl ctype3 = (XSComplexTypeDecl) this.fCurrentType;
            if (ctype3.getAbstract()) {
                z = true;
                reportSchemaError("cvc-type.2", new Object[]{element.rawname});
            } else {
                z = true;
            }
            if (this.fNormalizeData && ctype3.fContentType == z) {
                if (ctype3.fXSSimpleType.getVariety() == 3) {
                    this.fUnionType = z;
                } else {
                    try {
                        this.fWhiteSpace = ctype3.fXSSimpleType.getWhitespace();
                    } catch (DatatypeException e) {
                    }
                }
            }
        } else if (this.fNormalizeData) {
            XSSimpleType dv = (XSSimpleType) this.fCurrentType;
            if (dv.getVariety() == 3) {
                this.fUnionType = true;
            } else {
                try {
                    this.fWhiteSpace = dv.getWhitespace();
                } catch (DatatypeException e2) {
                }
            }
        }
        this.fCurrentCM = null;
        if (this.fCurrentType.getTypeCategory() == 15) {
            this.fCurrentCM = ((XSComplexTypeDecl) this.fCurrentType).getContentModel(this.fCMBuilder);
        }
        this.fCurrCMState = null;
        if (this.fCurrentCM != null) {
            this.fCurrCMState = this.fCurrentCM.startContentModel();
        }
        String xsiNil = attributes.getValue(SchemaSymbols.URI_XSI, SchemaSymbols.XSI_NIL);
        if (xsiNil != null && this.fCurrentElemDecl != null) {
            this.fNil = getXsiNil(element, xsiNil);
        }
        XSAttributeGroupDecl attrGrp = null;
        if (this.fCurrentType.getTypeCategory() == 15) {
            XSComplexTypeDecl ctype4 = (XSComplexTypeDecl) this.fCurrentType;
            attrGrp = ctype4.getAttrGrp();
        }
        if (this.fIDCChecking) {
            this.fValueStoreCache.startElement();
            this.fMatcherStack.pushContext();
            if (this.fCurrentElemDecl != null && this.fCurrentElemDecl.fIDCPos > 0) {
                this.fIdConstraint = true;
                this.fValueStoreCache.initValueStoresFor(this.fCurrentElemDecl, this);
            }
        }
        processAttributes(element, attributes, attrGrp);
        if (attrGrp != null) {
            addDefaultAttributes(element, attributes, attrGrp);
        }
        int count3 = this.fMatcherStack.getMatcherCount();
        for (int i = 0; i < count3; i++) {
            XPathMatcher matcher = this.fMatcherStack.getMatcherAt(i);
            matcher.startElement(element, attributes);
        }
        if (!this.fAugPSVI) {
            return augs;
        }
        Augmentations augs2 = getEmptyAugs(augs);
        this.fCurrentPSVI.fValidationContext = this.fValidationRoot;
        this.fCurrentPSVI.fDeclaration = this.fCurrentElemDecl;
        this.fCurrentPSVI.fTypeDecl = this.fCurrentType;
        this.fCurrentPSVI.fNotation = this.fNotation;
        this.fCurrentPSVI.fNil = this.fNil;
        return augs2;
    }

    Augmentations handleEndElement(QName element, Augmentations augs) throws InvalidDatatypeValueException {
        IdentityConstraint id;
        ValueStoreBase values;
        IdentityConstraint id2;
        Object obj;
        short s;
        ShortList shortList;
        if (this.fSkipValidationDepth >= 0) {
            if (this.fSkipValidationDepth == this.fElementDepth && this.fSkipValidationDepth > 0) {
                this.fNFullValidationDepth = this.fSkipValidationDepth - 1;
                this.fSkipValidationDepth = -1;
                this.fElementDepth--;
                this.fSubElement = this.fSubElementStack[this.fElementDepth];
                this.fCurrentElemDecl = this.fElemDeclStack[this.fElementDepth];
                this.fNil = this.fNilStack[this.fElementDepth];
                this.fNotation = this.fNotationStack[this.fElementDepth];
                this.fCurrentType = this.fTypeStack[this.fElementDepth];
                this.fCurrentCM = this.fCMStack[this.fElementDepth];
                this.fStrictAssess = this.fStrictAssessStack[this.fElementDepth];
                this.fCurrCMState = this.fCMStateStack[this.fElementDepth];
                this.fSawText = this.fSawTextStack[this.fElementDepth];
                this.fSawCharacters = this.fStringContent[this.fElementDepth];
            } else {
                this.fElementDepth--;
            }
            if (this.fElementDepth == -1 && this.fFullChecking && !this.fUseGrammarPoolOnly) {
                XSConstraints.fullSchemaChecking(this.fGrammarBucket, this.fSubGroupHandler, this.fCMBuilder, this.fXSIErrorReporter.fErrorReporter);
            }
            if (this.fAugPSVI) {
                return getEmptyAugs(augs);
            }
            return augs;
        }
        processElementContent(element);
        if (this.fIDCChecking) {
            int oldCount = this.fMatcherStack.getMatcherCount();
            for (int i = oldCount - 1; i >= 0; i--) {
                XPathMatcher matcher = this.fMatcherStack.getMatcherAt(i);
                if (this.fCurrentElemDecl == null) {
                    matcher.endElement(element, this.fCurrentType, false, this.fValidatedInfo.actualValue, this.fValidatedInfo.actualValueType, this.fValidatedInfo.itemValueTypes);
                } else {
                    XSTypeDefinition xSTypeDefinition = this.fCurrentType;
                    boolean nillable = this.fCurrentElemDecl.getNillable();
                    if (this.fDefaultValue == null) {
                        obj = this.fValidatedInfo.actualValue;
                    } else {
                        obj = this.fCurrentElemDecl.fDefault.actualValue;
                    }
                    Object obj2 = obj;
                    if (this.fDefaultValue == null) {
                        s = this.fValidatedInfo.actualValueType;
                    } else {
                        s = this.fCurrentElemDecl.fDefault.actualValueType;
                    }
                    short s2 = s;
                    if (this.fDefaultValue == null) {
                        shortList = this.fValidatedInfo.itemValueTypes;
                    } else {
                        shortList = this.fCurrentElemDecl.fDefault.itemValueTypes;
                    }
                    matcher.endElement(element, xSTypeDefinition, nillable, obj2, s2, shortList);
                }
            }
            if (this.fMatcherStack.size() > 0) {
                this.fMatcherStack.popContext();
            }
            int newCount = this.fMatcherStack.getMatcherCount();
            for (int i2 = oldCount - 1; i2 >= newCount; i2--) {
                ?? matcherAt = this.fMatcherStack.getMatcherAt(i2);
                if ((matcherAt instanceof Selector.Matcher) && (id2 = matcherAt.getIdentityConstraint()) != null && id2.getCategory() != 2) {
                    this.fValueStoreCache.transplant(id2, matcherAt.getInitialDepth());
                }
            }
            for (int i3 = oldCount - 1; i3 >= newCount; i3--) {
                ?? matcherAt2 = this.fMatcherStack.getMatcherAt(i3);
                if ((matcherAt2 instanceof Selector.Matcher) && (id = matcherAt2.getIdentityConstraint()) != null && id.getCategory() == 2 && (values = this.fValueStoreCache.getValueStoreFor(id, matcherAt2.getInitialDepth())) != null) {
                    values.endDocumentFragment();
                }
            }
            this.fValueStoreCache.endElement();
        }
        if (this.fElementDepth < this.fIgnoreXSITypeDepth) {
            this.fIgnoreXSITypeDepth--;
        }
        if (this.fElementDepth == 0) {
            String invIdRef = this.fValidationState.checkIDRefID();
            this.fValidationState.resetIDTables();
            if (invIdRef != null) {
                reportSchemaError("cvc-id.1", new Object[]{invIdRef});
            }
            if (this.fFullChecking && !this.fUseGrammarPoolOnly) {
                XSConstraints.fullSchemaChecking(this.fGrammarBucket, this.fSubGroupHandler, this.fCMBuilder, this.fXSIErrorReporter.fErrorReporter);
            }
            SchemaGrammar[] grammars = this.fGrammarBucket.getGrammars();
            if (this.fGrammarPool != null) {
                for (SchemaGrammar schemaGrammar : grammars) {
                    schemaGrammar.setImmutable(true);
                }
                this.fGrammarPool.cacheGrammars("http://www.w3.org/2001/XMLSchema", grammars);
            }
            return endElementPSVI(true, grammars, augs);
        }
        Augmentations augs2 = endElementPSVI(false, null, augs);
        this.fElementDepth--;
        this.fSubElement = this.fSubElementStack[this.fElementDepth];
        this.fCurrentElemDecl = this.fElemDeclStack[this.fElementDepth];
        this.fNil = this.fNilStack[this.fElementDepth];
        this.fNotation = this.fNotationStack[this.fElementDepth];
        this.fCurrentType = this.fTypeStack[this.fElementDepth];
        this.fCurrentCM = this.fCMStack[this.fElementDepth];
        this.fStrictAssess = this.fStrictAssessStack[this.fElementDepth];
        this.fCurrCMState = this.fCMStateStack[this.fElementDepth];
        this.fSawText = this.fSawTextStack[this.fElementDepth];
        this.fSawCharacters = this.fStringContent[this.fElementDepth];
        this.fWhiteSpace = (short) -1;
        this.fAppendBuffer = false;
        this.fUnionType = false;
        return augs2;
    }

    final Augmentations endElementPSVI(boolean root, SchemaGrammar[] grammars, Augmentations augs) {
        if (this.fAugPSVI) {
            augs = getEmptyAugs(augs);
            this.fCurrentPSVI.fDeclaration = this.fCurrentElemDecl;
            this.fCurrentPSVI.fTypeDecl = this.fCurrentType;
            this.fCurrentPSVI.fNotation = this.fNotation;
            this.fCurrentPSVI.fValidationContext = this.fValidationRoot;
            this.fCurrentPSVI.fNil = this.fNil;
            if (this.fElementDepth > this.fNFullValidationDepth) {
                this.fCurrentPSVI.fValidationAttempted = (short) 2;
            } else if (this.fElementDepth > this.fNNoneValidationDepth) {
                this.fCurrentPSVI.fValidationAttempted = (short) 0;
            } else {
                this.fCurrentPSVI.fValidationAttempted = (short) 1;
            }
            if (this.fNFullValidationDepth == this.fElementDepth) {
                this.fNFullValidationDepth = this.fElementDepth - 1;
            }
            if (this.fNNoneValidationDepth == this.fElementDepth) {
                this.fNNoneValidationDepth = this.fElementDepth - 1;
            }
            if (this.fDefaultValue != null) {
                this.fCurrentPSVI.fSpecified = true;
            }
            this.fCurrentPSVI.fValue.copyFrom(this.fValidatedInfo);
            if (this.fStrictAssess) {
                String[] errors = this.fXSIErrorReporter.mergeContext();
                this.fCurrentPSVI.fErrors = errors;
                this.fCurrentPSVI.fValidity = errors != null ? (short) 1 : (short) 2;
            } else {
                this.fCurrentPSVI.fValidity = (short) 0;
                this.fXSIErrorReporter.popContext();
            }
            if (root) {
                this.fCurrentPSVI.fGrammars = grammars;
                this.fCurrentPSVI.fSchemaInformation = null;
            }
        }
        return augs;
    }

    Augmentations getEmptyAugs(Augmentations augs) {
        if (augs == null) {
            augs = this.fAugmentations;
            augs.removeAllItems();
        }
        augs.putItem(Constants.ELEMENT_PSVI, this.fCurrentPSVI);
        this.fCurrentPSVI.reset();
        return augs;
    }

    void storeLocations(String sLocation, String nsLocation) throws URI.MalformedURIException {
        if (sLocation != null) {
            if (!XMLSchemaLoader.tokenizeSchemaLocationStr(sLocation, this.fLocationPairs, this.fLocator == null ? null : this.fLocator.getExpandedSystemId())) {
                this.fXSIErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "SchemaLocation", new Object[]{sLocation}, (short) 0);
            }
        }
        if (nsLocation != null) {
            XMLSchemaLoader.LocationArray la = (XMLSchemaLoader.LocationArray) this.fLocationPairs.get(XMLSymbols.EMPTY_STRING);
            if (la == null) {
                la = new XMLSchemaLoader.LocationArray();
                this.fLocationPairs.put(XMLSymbols.EMPTY_STRING, la);
            }
            if (this.fLocator != null) {
                try {
                    nsLocation = XMLEntityManager.expandSystemId(nsLocation, this.fLocator.getExpandedSystemId(), false);
                } catch (URI.MalformedURIException e) {
                }
            }
            la.addLocation(nsLocation);
        }
    }

    SchemaGrammar findSchemaGrammar(short contextType, String namespace, QName enclosingElement, QName triggeringComponent, XMLAttributes attributes) {
        SchemaGrammar grammar = this.fGrammarBucket.getGrammar(namespace);
        if (grammar == null) {
            this.fXSDDescription.setNamespace(namespace);
            if (this.fGrammarPool != null && (grammar = (SchemaGrammar) this.fGrammarPool.retrieveGrammar(this.fXSDDescription)) != null && !this.fGrammarBucket.putGrammar(grammar, true, this.fNamespaceGrowth)) {
                this.fXSIErrorReporter.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "GrammarConflict", null, (short) 0);
                grammar = null;
            }
        }
        SchemaGrammar grammar2 = grammar;
        if (this.fUseGrammarPoolOnly || (grammar2 != null && (!this.fNamespaceGrowth || hasSchemaComponent(grammar2, contextType, triggeringComponent)))) {
            return grammar2;
        }
        this.fXSDDescription.reset();
        this.fXSDDescription.fContextType = contextType;
        this.fXSDDescription.setNamespace(namespace);
        this.fXSDDescription.fEnclosedElementName = enclosingElement;
        this.fXSDDescription.fTriggeringComponent = triggeringComponent;
        this.fXSDDescription.fAttributes = attributes;
        if (this.fLocator != null) {
            this.fXSDDescription.setBaseSystemId(this.fLocator.getExpandedSystemId());
        }
        Hashtable locationPairs = this.fLocationPairs;
        Object locationArray = locationPairs.get(namespace == null ? XMLSymbols.EMPTY_STRING : namespace);
        if (locationArray != null) {
            String[] temp = ((XMLSchemaLoader.LocationArray) locationArray).getLocationArray();
            if (temp.length != 0) {
                setLocationHints(this.fXSDDescription, temp, grammar2);
            }
        }
        if (grammar2 != null && this.fXSDDescription.fLocationHints == null) {
            return grammar2;
        }
        boolean toParseSchema = true;
        if (grammar2 != null) {
            locationPairs = EMPTY_TABLE;
        }
        try {
            XMLInputSource xis = XMLSchemaLoader.resolveDocument(this.fXSDDescription, locationPairs, this.fEntityResolver);
            if (grammar2 != null && this.fNamespaceGrowth) {
                try {
                    if (grammar2.getDocumentLocations().contains(XMLEntityManager.expandSystemId(xis.getSystemId(), xis.getBaseSystemId(), false))) {
                        toParseSchema = false;
                    }
                } catch (URI.MalformedURIException e) {
                }
            }
            return toParseSchema ? this.fSchemaLoader.loadSchema(this.fXSDDescription, xis, this.fLocationPairs) : grammar2;
        } catch (IOException ex) {
            String[] locationHints = this.fXSDDescription.getLocationHints();
            XMLErrorReporter xMLErrorReporter = this.fXSIErrorReporter.fErrorReporter;
            Object[] objArr = new Object[1];
            objArr[0] = locationHints != null ? locationHints[0] : XMLSymbols.EMPTY_STRING;
            xMLErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "schema_reference.4", objArr, (short) 0, (Exception) ex);
            return grammar2;
        }
    }

    private boolean hasSchemaComponent(SchemaGrammar grammar, short contextType, QName triggeringComponent) {
        String localName;
        if (grammar != null && triggeringComponent != null && (localName = triggeringComponent.localpart) != null && localName.length() > 0) {
            switch (contextType) {
                case 5:
                    if (grammar.getElementDeclaration(localName) == null) {
                        return false;
                    }
                    return true;
                case 6:
                    if (grammar.getAttributeDeclaration(localName) == null) {
                        return false;
                    }
                    return true;
                case 7:
                    if (grammar.getTypeDefinition(localName) == null) {
                        return false;
                    }
                    return true;
            }
        }
        return false;
    }

    private void setLocationHints(XSDDescription desc, String[] locations, SchemaGrammar grammar) {
        int length = locations.length;
        if (grammar == null) {
            this.fXSDDescription.fLocationHints = new String[length];
            System.arraycopy(locations, 0, this.fXSDDescription.fLocationHints, 0, length);
            return;
        }
        setLocationHints(desc, locations, grammar.getDocumentLocations());
    }

    private void setLocationHints(XSDDescription desc, String[] locations, StringList docLocations) {
        int length = locations.length;
        String[] hints = new String[length];
        int counter = 0;
        for (int i = 0; i < length; i++) {
            if (!docLocations.contains(locations[i])) {
                hints[counter] = locations[i];
                counter++;
            }
        }
        if (counter > 0) {
            if (counter == length) {
                this.fXSDDescription.fLocationHints = hints;
                return;
            }
            this.fXSDDescription.fLocationHints = new String[counter];
            System.arraycopy(hints, 0, this.fXSDDescription.fLocationHints, 0, counter);
        }
    }

    XSTypeDefinition getAndCheckXsiType(QName element, String xsiType, XMLAttributes attributes) {
        SchemaGrammar grammar;
        try {
            QName typeName = (QName) this.fQNameDV.validate(xsiType, (ValidationContext) this.fValidationState, (ValidatedInfo) null);
            XSTypeDefinition type = null;
            if (typeName.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA) {
                type = SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(typeName.localpart);
            }
            XSTypeDefinition type2 = type;
            if (type2 == null && (grammar = findSchemaGrammar((short) 7, typeName.uri, element, typeName, attributes)) != null) {
                type2 = grammar.getGlobalTypeDecl(typeName.localpart);
            }
            if (type2 == null) {
                reportSchemaError("cvc-elt.4.2", new Object[]{element.rawname, xsiType});
                return null;
            }
            if (this.fCurrentType != null) {
                short block = 0;
                if (this.fCurrentElemDecl != null) {
                    block = this.fCurrentElemDecl.fBlock;
                }
                if (this.fCurrentType.getTypeCategory() == 15) {
                    block = (short) (((XSComplexTypeDecl) this.fCurrentType).fBlock | block);
                }
                if (!XSConstraints.checkTypeDerivationOk(type2, this.fCurrentType, block)) {
                    reportSchemaError("cvc-elt.4.3", new Object[]{element.rawname, xsiType, this.fCurrentType.getName()});
                }
            }
            return type2;
        } catch (InvalidDatatypeValueException e) {
            reportSchemaError(e.getKey(), e.getArgs());
            reportSchemaError("cvc-elt.4.1", new Object[]{element.rawname, String.valueOf(SchemaSymbols.URI_XSI) + "," + SchemaSymbols.XSI_TYPE, xsiType});
            return null;
        }
    }

    boolean getXsiNil(QName element, String xsiNil) {
        if (this.fCurrentElemDecl != null && !this.fCurrentElemDecl.getNillable()) {
            reportSchemaError("cvc-elt.3.1", new Object[]{element.rawname, String.valueOf(SchemaSymbols.URI_XSI) + "," + SchemaSymbols.XSI_NIL});
        } else {
            String value = XMLChar.trim(xsiNil);
            if (value.equals(SchemaSymbols.ATTVAL_TRUE) || value.equals(SchemaSymbols.ATTVAL_TRUE_1)) {
                if (this.fCurrentElemDecl != null && this.fCurrentElemDecl.getConstraintType() == 2) {
                    reportSchemaError("cvc-elt.3.2.2", new Object[]{element.rawname, String.valueOf(SchemaSymbols.URI_XSI) + "," + SchemaSymbols.XSI_NIL});
                }
                return true;
            }
        }
        return false;
    }

    void processAttributes(QName element, XMLAttributes attributes, XSAttributeGroupDecl attrGrp) {
        int index;
        int useCount;
        XSWildcardDecl attrWildcard;
        XSObjectList attrUses;
        char c;
        XSWildcardDecl attrWildcard2;
        XSObjectList attrUses2;
        char c2;
        int useCount2;
        int useCount3;
        XSAttributeUseImpl currUse;
        XSObjectList attrUses3;
        XSWildcardDecl attrWildcard3;
        XSAttributeUseImpl currUse2;
        char c3;
        String wildcardIDName;
        XSAttributeDecl currDecl;
        XSAttributeDecl attrDecl;
        XSAttributeDecl attrDecl2;
        int attCount = attributes.getLength();
        Augmentations augs = null;
        AttributePSVImpl attrPSVI = null;
        char c4 = 0;
        boolean isSimple = this.fCurrentType == null || this.fCurrentType.getTypeCategory() == 16;
        XSObjectList attrUses4 = null;
        int useCount4 = 0;
        XSWildcardDecl attrWildcard4 = null;
        if (!isSimple) {
            attrUses4 = attrGrp.getAttributeUses();
            useCount4 = attrUses4.getLength();
            attrWildcard4 = attrGrp.fAttributeWC;
        }
        XSObjectList attrUses5 = attrUses4;
        int index2 = 0;
        String wildcardIDName2 = null;
        while (index2 < attCount) {
            attributes.getName(index2, this.fTempQName);
            if (this.fAugPSVI || this.fIdConstraint) {
                augs = attributes.getAugmentations(index2);
                attrPSVI = (AttributePSVImpl) augs.getItem(Constants.ATTRIBUTE_PSVI);
                if (attrPSVI != null) {
                    attrPSVI.reset();
                } else {
                    attrPSVI = new AttributePSVImpl();
                    augs.putItem(Constants.ATTRIBUTE_PSVI, attrPSVI);
                }
                attrPSVI.fValidationContext = this.fValidationRoot;
            }
            Augmentations augs2 = augs;
            AttributePSVImpl attrPSVI2 = attrPSVI;
            if (this.fTempQName.uri != SchemaSymbols.URI_XSI) {
                index = index2;
                useCount = useCount4;
                attrWildcard = attrWildcard4;
                attrUses = attrUses5;
                if (this.fTempQName.rawname == XMLSymbols.PREFIX_XMLNS) {
                    c = c4;
                    attrWildcard2 = attrWildcard;
                    attrUses2 = attrUses;
                    c2 = 16;
                    useCount2 = useCount;
                } else if (this.fTempQName.rawname.startsWith("xmlns:")) {
                    c = c4;
                    attrWildcard2 = attrWildcard;
                    attrUses2 = attrUses;
                    c2 = 16;
                    useCount2 = useCount;
                } else if (isSimple) {
                    Object[] objArr = new Object[2];
                    objArr[c4] = element.rawname;
                    objArr[1] = this.fTempQName.rawname;
                    reportSchemaError("cvc-type.3.1.1", objArr);
                    c = c4;
                    attrWildcard2 = attrWildcard;
                    attrUses2 = attrUses;
                    c2 = 16;
                    useCount2 = useCount;
                } else {
                    int i = 0;
                    while (true) {
                        useCount3 = useCount;
                        if (i < useCount3) {
                            attrUses3 = attrUses;
                            XSAttributeUseImpl oneUse = (XSAttributeUseImpl) attrUses3.item(i);
                            if (oneUse.fAttrDecl.fName == this.fTempQName.localpart && oneUse.fAttrDecl.fTargetNamespace == this.fTempQName.uri) {
                                currUse = oneUse;
                                break;
                            }
                            i++;
                            attrUses = attrUses3;
                            useCount = useCount3;
                            attrWildcard = attrWildcard;
                            c4 = c4;
                        } else {
                            currUse = null;
                            attrUses3 = attrUses;
                            break;
                        }
                    }
                    if (currUse == null) {
                        attrWildcard3 = attrWildcard;
                        if (attrWildcard3 == null || !attrWildcard3.allowNamespace(this.fTempQName.uri)) {
                            Object[] objArr2 = new Object[2];
                            objArr2[c4] = element.rawname;
                            objArr2[1] = this.fTempQName.rawname;
                            reportSchemaError("cvc-complex-type.3.2.2", objArr2);
                            this.fNFullValidationDepth = this.fElementDepth;
                        }
                        attrUses2 = attrUses3;
                        useCount2 = useCount3;
                        c = c4;
                        c2 = 16;
                        attrWildcard2 = attrWildcard3;
                    } else {
                        attrWildcard3 = attrWildcard;
                    }
                    if (currUse != null) {
                        currUse2 = currUse;
                        attrUses2 = attrUses3;
                        c = c4;
                        wildcardIDName = wildcardIDName2;
                        c3 = 16;
                        currDecl = currUse.fAttrDecl;
                        attrWildcard2 = attrWildcard3;
                    } else if (attrWildcard3.fProcessContents == 2) {
                        attrUses2 = attrUses3;
                        useCount2 = useCount3;
                        c = c4;
                        c2 = 16;
                        attrWildcard2 = attrWildcard3;
                    } else {
                        attrWildcard2 = attrWildcard3;
                        currUse2 = currUse;
                        attrUses2 = attrUses3;
                        SchemaGrammar grammar = findSchemaGrammar((short) 6, this.fTempQName.uri, element, this.fTempQName, attributes);
                        XSAttributeDecl currDecl2 = grammar != null ? grammar.getGlobalAttributeDecl(this.fTempQName.localpart) : null;
                        if (currDecl2 == null) {
                            if (attrWildcard2.fProcessContents == 1) {
                                reportSchemaError("cvc-complex-type.3.2.2", new Object[]{element.rawname, this.fTempQName.rawname});
                            }
                            useCount2 = useCount3;
                            c = 0;
                            c2 = 16;
                        } else {
                            c3 = 16;
                            if (currDecl2.fType.getTypeCategory() != 16 || !currDecl2.fType.isIDType()) {
                                c = 0;
                            } else if (wildcardIDName2 != null) {
                                c = 0;
                                reportSchemaError("cvc-complex-type.5.1", new Object[]{element.rawname, currDecl2.fName, wildcardIDName2});
                            } else {
                                c = 0;
                                String wildcardIDName3 = currDecl2.fName;
                                wildcardIDName = wildcardIDName3;
                                currDecl = currDecl2;
                            }
                            wildcardIDName = wildcardIDName2;
                            currDecl = currDecl2;
                        }
                    }
                    c2 = c3;
                    useCount2 = useCount3;
                    processOneAttribute(element, attributes, index, currDecl, currUse2, attrPSVI2);
                    wildcardIDName2 = wildcardIDName;
                }
            } else {
                if (this.fTempQName.localpart == SchemaSymbols.XSI_TYPE) {
                    attrDecl2 = XSI_TYPE;
                } else if (this.fTempQName.localpart == SchemaSymbols.XSI_NIL) {
                    attrDecl2 = XSI_NIL;
                } else if (this.fTempQName.localpart == SchemaSymbols.XSI_SCHEMALOCATION) {
                    attrDecl2 = XSI_SCHEMALOCATION;
                } else if (this.fTempQName.localpart == SchemaSymbols.XSI_NONAMESPACESCHEMALOCATION) {
                    attrDecl2 = XSI_NONAMESPACESCHEMALOCATION;
                } else {
                    attrDecl = null;
                    if (attrDecl == null) {
                        index = index2;
                        useCount = useCount4;
                        attrWildcard = attrWildcard4;
                        attrUses = attrUses5;
                        processOneAttribute(element, attributes, index2, attrDecl, null, attrPSVI2);
                    }
                    c = c4;
                    attrWildcard2 = attrWildcard;
                    attrUses2 = attrUses;
                    c2 = 16;
                    useCount2 = useCount;
                }
                attrDecl = attrDecl2;
                if (attrDecl == null) {
                }
                c = c4;
                attrWildcard2 = attrWildcard;
                attrUses2 = attrUses;
                c2 = 16;
                useCount2 = useCount;
            }
            index2 = index + 1;
            attrPSVI = attrPSVI2;
            attrWildcard4 = attrWildcard2;
            c4 = c;
            augs = augs2;
            attrUses5 = attrUses2;
            useCount4 = useCount2;
        }
        if (isSimple || attrGrp.fIDAttrName == null || wildcardIDName2 == null) {
            return;
        }
        Object[] objArr3 = new Object[3];
        objArr3[c4] = element.rawname;
        objArr3[1] = wildcardIDName2;
        objArr3[2] = attrGrp.fIDAttrName;
        reportSchemaError("cvc-complex-type.5.2", objArr3);
    }

    void processOneAttribute(QName element, XMLAttributes attributes, int index, XSAttributeDecl currDecl, XSAttributeUseImpl currUse, AttributePSVImpl attrPSVI) {
        String attrValue = attributes.getValue(index);
        this.fXSIErrorReporter.pushContext();
        ?? r7 = currDecl.fType;
        Object actualValue = null;
        try {
            actualValue = r7.validate(attrValue, this.fValidationState, this.fValidatedInfo);
            if (this.fNormalizeData) {
                try {
                    attributes.setValue(index, this.fValidatedInfo.normalizedValue);
                } catch (InvalidDatatypeValueException e) {
                    idve = e;
                    reportSchemaError(idve.getKey(), idve.getArgs());
                    Object[] objArr = new Object[4];
                    objArr[0] = element.rawname;
                    objArr[1] = this.fTempQName.rawname;
                    objArr[2] = attrValue;
                    objArr[3] = r7 instanceof XSSimpleTypeDecl ? r7.getTypeName() : r7.getName();
                    reportSchemaError("cvc-attribute.3", objArr);
                }
            }
            if (r7.getVariety() == 1 && r7.getPrimitiveKind() == 20) {
                QName qName = (QName) actualValue;
                SchemaGrammar grammar = this.fGrammarBucket.getGrammar(qName.uri);
                if (grammar != null) {
                    this.fNotation = grammar.getGlobalNotationDecl(qName.localpart);
                }
            }
        } catch (InvalidDatatypeValueException e2) {
            idve = e2;
        }
        if (actualValue != null && currDecl.getConstraintType() == 2 && (!ValidatedInfo.isComparable(this.fValidatedInfo, currDecl.fDefault) || !actualValue.equals(currDecl.fDefault.actualValue))) {
            reportSchemaError("cvc-attribute.4", new Object[]{element.rawname, this.fTempQName.rawname, attrValue, currDecl.fDefault.stringValue()});
        }
        if (actualValue != null && currUse != null && currUse.fConstraintType == 2 && (!ValidatedInfo.isComparable(this.fValidatedInfo, currUse.fDefault) || !actualValue.equals(currUse.fDefault.actualValue))) {
            reportSchemaError("cvc-complex-type.3.1", new Object[]{element.rawname, this.fTempQName.rawname, attrValue, currUse.fDefault.stringValue()});
        }
        if (this.fIdConstraint) {
            attrPSVI.fValue.copyFrom(this.fValidatedInfo);
        }
        if (this.fAugPSVI) {
            attrPSVI.fDeclaration = currDecl;
            attrPSVI.fTypeDecl = r7;
            attrPSVI.fValue.copyFrom(this.fValidatedInfo);
            short s = 2;
            attrPSVI.fValidationAttempted = (short) 2;
            this.fNNoneValidationDepth = this.fElementDepth;
            String[] errors = this.fXSIErrorReporter.mergeContext();
            attrPSVI.fErrors = errors;
            if (errors != null) {
                s = 1;
            }
            attrPSVI.fValidity = s;
        }
    }

    void addDefaultAttributes(QName element, XMLAttributes attributes, XSAttributeGroupDecl attrGrp) {
        int attrIndex;
        XMLAttributes xMLAttributes = attributes;
        XSObjectList attrUses = attrGrp.getAttributeUses();
        int useCount = attrUses.getLength();
        int i = 0;
        while (i < useCount) {
            XSAttributeUseImpl currUse = (XSAttributeUseImpl) attrUses.item(i);
            XSAttributeDecl currDecl = currUse.fAttrDecl;
            short constType = currUse.fConstraintType;
            ValidatedInfo defaultValue = currUse.fDefault;
            if (constType == 0) {
                constType = currDecl.getConstraintType();
                defaultValue = currDecl.fDefault;
            }
            boolean isSpecified = xMLAttributes.getValue(currDecl.fTargetNamespace, currDecl.fName) != null;
            if (currUse.fUse == 1 && !isSpecified) {
                reportSchemaError("cvc-complex-type.4", new Object[]{element.rawname, currDecl.fName});
            }
            if (!isSpecified && constType != 0) {
                QName attName = new QName(null, currDecl.fName, currDecl.fName, currDecl.fTargetNamespace);
                String normalized = defaultValue != null ? defaultValue.stringValue() : "";
                if (xMLAttributes instanceof XMLAttributesImpl) {
                    XMLAttributesImpl xMLAttributesImpl = xMLAttributes;
                    attrIndex = xMLAttributesImpl.getLength();
                    xMLAttributesImpl.addAttributeNS(attName, "CDATA", normalized);
                } else {
                    attrIndex = xMLAttributes.addAttribute(attName, "CDATA", normalized);
                }
                int attrIndex2 = attrIndex;
                if (this.fAugPSVI) {
                    Augmentations augs = xMLAttributes.getAugmentations(attrIndex2);
                    AttributePSVImpl attrPSVI = new AttributePSVImpl();
                    augs.putItem(Constants.ATTRIBUTE_PSVI, attrPSVI);
                    attrPSVI.fDeclaration = currDecl;
                    attrPSVI.fTypeDecl = currDecl.fType;
                    attrPSVI.fValue.copyFrom(defaultValue);
                    attrPSVI.fValidationContext = this.fValidationRoot;
                    attrPSVI.fValidity = (short) 2;
                    attrPSVI.fValidationAttempted = (short) 2;
                    attrPSVI.fSpecified = true;
                }
            }
            i++;
            xMLAttributes = attributes;
        }
    }

    void processElementContent(QName element) throws InvalidDatatypeValueException {
        if (this.fCurrentElemDecl != null && this.fCurrentElemDecl.fDefault != null && !this.fSawText && !this.fSubElement && !this.fNil) {
            String strv = this.fCurrentElemDecl.fDefault.stringValue();
            int bufLen = strv.length();
            if (this.fNormalizedStr.ch == null || this.fNormalizedStr.ch.length < bufLen) {
                this.fNormalizedStr.ch = new char[bufLen];
            }
            strv.getChars(0, bufLen, this.fNormalizedStr.ch, 0);
            this.fNormalizedStr.offset = 0;
            this.fNormalizedStr.length = bufLen;
            this.fDefaultValue = this.fNormalizedStr;
        }
        this.fValidatedInfo.normalizedValue = null;
        if (this.fNil && (this.fSubElement || this.fSawText)) {
            reportSchemaError("cvc-elt.3.2.1", new Object[]{element.rawname, String.valueOf(SchemaSymbols.URI_XSI) + "," + SchemaSymbols.XSI_NIL});
        }
        this.fValidatedInfo.reset();
        if (this.fCurrentElemDecl != null && this.fCurrentElemDecl.getConstraintType() != 0 && !this.fSubElement && !this.fSawText && !this.fNil) {
            if (this.fCurrentType != this.fCurrentElemDecl.fType && XSConstraints.ElementDefaultValidImmediate(this.fCurrentType, this.fCurrentElemDecl.fDefault.stringValue(), this.fState4XsiType, null) == null) {
                reportSchemaError("cvc-elt.5.1.1", new Object[]{element.rawname, this.fCurrentType.getName(), this.fCurrentElemDecl.fDefault.stringValue()});
            }
            elementLocallyValidType(element, this.fCurrentElemDecl.fDefault.stringValue());
        } else {
            Object actualValue = elementLocallyValidType(element, this.fBuffer);
            if (this.fCurrentElemDecl != null && this.fCurrentElemDecl.getConstraintType() == 2 && !this.fNil) {
                String content = this.fBuffer.toString();
                if (this.fSubElement) {
                    reportSchemaError("cvc-elt.5.2.2.1", new Object[]{element.rawname});
                }
                if (this.fCurrentType.getTypeCategory() == 15) {
                    XSComplexTypeDecl ctype = (XSComplexTypeDecl) this.fCurrentType;
                    if (ctype.fContentType == 3) {
                        if (!this.fCurrentElemDecl.fDefault.normalizedValue.equals(content)) {
                            reportSchemaError("cvc-elt.5.2.2.2.1", new Object[]{element.rawname, content, this.fCurrentElemDecl.fDefault.normalizedValue});
                        }
                    } else if (ctype.fContentType == 1 && actualValue != null && (!ValidatedInfo.isComparable(this.fValidatedInfo, this.fCurrentElemDecl.fDefault) || !actualValue.equals(this.fCurrentElemDecl.fDefault.actualValue))) {
                        reportSchemaError("cvc-elt.5.2.2.2.2", new Object[]{element.rawname, content, this.fCurrentElemDecl.fDefault.stringValue()});
                    }
                } else if (this.fCurrentType.getTypeCategory() == 16 && actualValue != null && (!ValidatedInfo.isComparable(this.fValidatedInfo, this.fCurrentElemDecl.fDefault) || !actualValue.equals(this.fCurrentElemDecl.fDefault.actualValue))) {
                    reportSchemaError("cvc-elt.5.2.2.2.2", new Object[]{element.rawname, content, this.fCurrentElemDecl.fDefault.stringValue()});
                }
            }
        }
        if (this.fDefaultValue == null && this.fNormalizeData && this.fDocumentHandler != null && this.fUnionType) {
            String content2 = this.fValidatedInfo.normalizedValue;
            if (content2 == null) {
                content2 = this.fBuffer.toString();
            }
            int bufLen2 = content2.length();
            if (this.fNormalizedStr.ch == null || this.fNormalizedStr.ch.length < bufLen2) {
                this.fNormalizedStr.ch = new char[bufLen2];
            }
            content2.getChars(0, bufLen2, this.fNormalizedStr.ch, 0);
            this.fNormalizedStr.offset = 0;
            this.fNormalizedStr.length = bufLen2;
            this.fDocumentHandler.characters(this.fNormalizedStr, null);
        }
    }

    Object elementLocallyValidType(QName element, Object textContent) throws InvalidDatatypeValueException {
        if (this.fCurrentType == null) {
            return null;
        }
        if (this.fCurrentType.getTypeCategory() == 16) {
            if (this.fSubElement) {
                reportSchemaError("cvc-type.3.1.2", new Object[]{element.rawname});
            }
            if (this.fNil) {
                return null;
            }
            XSSimpleType dv = (XSSimpleType) this.fCurrentType;
            try {
                if (!this.fNormalizeData || this.fUnionType) {
                    this.fValidationState.setNormalizationRequired(true);
                }
                Object retValue = dv.validate(textContent, this.fValidationState, this.fValidatedInfo);
                return retValue;
            } catch (InvalidDatatypeValueException e) {
                reportSchemaError(e.getKey(), e.getArgs());
                reportSchemaError("cvc-type.3.1.3", new Object[]{element.rawname, textContent});
                return null;
            }
        }
        Object retValue2 = elementLocallyValidComplexType(element, textContent);
        return retValue2;
    }

    Object elementLocallyValidComplexType(QName element, Object textContent) throws InvalidDatatypeValueException {
        int minOccurs;
        int count;
        Object actualValue = null;
        XSComplexTypeDecl ctype = (XSComplexTypeDecl) this.fCurrentType;
        if (!this.fNil) {
            if (ctype.fContentType == 0 && (this.fSubElement || this.fSawText)) {
                reportSchemaError("cvc-complex-type.2.1", new Object[]{element.rawname});
            } else if (ctype.fContentType == 1) {
                if (this.fSubElement) {
                    reportSchemaError("cvc-complex-type.2.2", new Object[]{element.rawname});
                }
                XSSimpleType dv = ctype.fXSSimpleType;
                try {
                    if (!this.fNormalizeData || this.fUnionType) {
                        this.fValidationState.setNormalizationRequired(true);
                    }
                } catch (InvalidDatatypeValueException e) {
                    e = e;
                }
                try {
                    actualValue = dv.validate(textContent, this.fValidationState, this.fValidatedInfo);
                } catch (InvalidDatatypeValueException e2) {
                    e = e2;
                    reportSchemaError(e.getKey(), e.getArgs());
                    reportSchemaError("cvc-complex-type.2.2", new Object[]{element.rawname});
                }
            } else if (ctype.fContentType == 2 && this.fSawCharacters) {
                reportSchemaError("cvc-complex-type.2.3", new Object[]{element.rawname});
            }
            if ((ctype.fContentType == 2 || ctype.fContentType == 3) && this.fCurrCMState[0] >= 0 && !this.fCurrentCM.endContentModel(this.fCurrCMState)) {
                String expected = expectedStr(this.fCurrentCM.whatCanGoHere(this.fCurrCMState));
                int[] occurenceInfo = this.fCurrentCM.occurenceInfo(this.fCurrCMState);
                if (occurenceInfo != null && (count = occurenceInfo[2]) < (minOccurs = occurenceInfo[0])) {
                    int required = minOccurs - count;
                    if (required > 1) {
                        reportSchemaError("cvc-complex-type.2.4.j", new Object[]{element.rawname, this.fCurrentCM.getTermName(occurenceInfo[3]), Integer.toString(minOccurs), Integer.toString(required)});
                    } else {
                        reportSchemaError("cvc-complex-type.2.4.i", new Object[]{element.rawname, this.fCurrentCM.getTermName(occurenceInfo[3]), Integer.toString(minOccurs)});
                    }
                } else {
                    reportSchemaError("cvc-complex-type.2.4.b", new Object[]{element.rawname, expected});
                }
            }
        }
        return actualValue;
    }

    void processRootTypeQName(javax.xml.namespace.QName rootTypeQName) {
        String typeName;
        String rootTypeNamespace = rootTypeQName.getNamespaceURI();
        if (rootTypeNamespace != null && rootTypeNamespace.equals("")) {
            rootTypeNamespace = null;
        }
        if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(rootTypeNamespace)) {
            this.fCurrentType = SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(rootTypeQName.getLocalPart());
        } else {
            SchemaGrammar grammarForRootType = findSchemaGrammar((short) 5, rootTypeNamespace, null, null, null);
            if (grammarForRootType != null) {
                this.fCurrentType = grammarForRootType.getGlobalTypeDecl(rootTypeQName.getLocalPart());
            }
        }
        if (this.fCurrentType == null) {
            if (rootTypeQName.getPrefix().equals("")) {
                typeName = rootTypeQName.getLocalPart();
            } else {
                typeName = String.valueOf(rootTypeQName.getPrefix()) + ":" + rootTypeQName.getLocalPart();
            }
            reportSchemaError("cvc-type.1", new Object[]{typeName});
        }
    }

    void processRootElementDeclQName(javax.xml.namespace.QName rootElementDeclQName, QName element) {
        String declName;
        String rootElementDeclNamespace = rootElementDeclQName.getNamespaceURI();
        if (rootElementDeclNamespace != null && rootElementDeclNamespace.equals("")) {
            rootElementDeclNamespace = null;
        }
        SchemaGrammar grammarForRootElement = findSchemaGrammar((short) 5, rootElementDeclNamespace, null, null, null);
        if (grammarForRootElement != null) {
            this.fCurrentElemDecl = grammarForRootElement.getGlobalElementDecl(rootElementDeclQName.getLocalPart());
        }
        if (this.fCurrentElemDecl == null) {
            if (rootElementDeclQName.getPrefix().equals("")) {
                declName = rootElementDeclQName.getLocalPart();
            } else {
                declName = String.valueOf(rootElementDeclQName.getPrefix()) + ":" + rootElementDeclQName.getLocalPart();
            }
            reportSchemaError("cvc-elt.1.a", new Object[]{declName});
            return;
        }
        checkElementMatchesRootElementDecl(this.fCurrentElemDecl, element);
    }

    void checkElementMatchesRootElementDecl(XSElementDecl rootElementDecl, QName element) {
        if (element.localpart != rootElementDecl.fName || element.uri != rootElementDecl.fTargetNamespace) {
            reportSchemaError("cvc-elt.1.b", new Object[]{element.rawname, rootElementDecl.fName});
        }
    }

    void reportSchemaError(String key, Object[] arguments) {
        if (this.fDoValidation) {
            this.fXSIErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, key, arguments, (short) 1);
        }
    }

    private String expectedStr(Vector expected) {
        StringBuffer ret = new StringBuffer("{");
        int size = expected.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(expected.elementAt(i).toString());
        }
        ret.append('}');
        return ret.toString();
    }

    protected static class XPathMatcherStack {
        protected int fMatchersCount;
        protected XPathMatcher[] fMatchers = new XPathMatcher[4];
        protected IntStack fContextStack = new IntStack();

        public void clear() {
            for (int i = 0; i < this.fMatchersCount; i++) {
                this.fMatchers[i] = null;
            }
            this.fMatchersCount = 0;
            this.fContextStack.clear();
        }

        public int size() {
            return this.fContextStack.size();
        }

        public int getMatcherCount() {
            return this.fMatchersCount;
        }

        public void addMatcher(XPathMatcher matcher) {
            ensureMatcherCapacity();
            XPathMatcher[] xPathMatcherArr = this.fMatchers;
            int i = this.fMatchersCount;
            this.fMatchersCount = i + 1;
            xPathMatcherArr[i] = matcher;
        }

        public XPathMatcher getMatcherAt(int index) {
            return this.fMatchers[index];
        }

        public void pushContext() {
            this.fContextStack.push(this.fMatchersCount);
        }

        public void popContext() {
            this.fMatchersCount = this.fContextStack.pop();
        }

        private void ensureMatcherCapacity() {
            if (this.fMatchersCount == this.fMatchers.length) {
                XPathMatcher[] array = new XPathMatcher[this.fMatchers.length * 2];
                System.arraycopy(this.fMatchers, 0, array, 0, this.fMatchers.length);
                this.fMatchers = array;
            }
        }
    }

    protected abstract class ValueStoreBase implements ValueStore {
        protected int fFieldCount;
        protected Field[] fFields;
        protected IdentityConstraint fIdentityConstraint;
        protected ShortList[] fLocalItemValueTypes;
        protected short[] fLocalValueTypes;
        protected Object[] fLocalValues;
        protected int fValuesCount;
        public final Vector fValues = new Vector();
        public ShortVector fValueTypes = null;
        public Vector fItemValueTypes = null;
        private boolean fUseValueTypeVector = false;
        private int fValueTypesLength = 0;
        private short fValueType = 0;
        private boolean fUseItemValueTypeVector = false;
        private int fItemValueTypesLength = 0;
        private ShortList fItemValueType = null;
        final StringBuffer fTempBuffer = new StringBuffer();

        protected ValueStoreBase(IdentityConstraint identityConstraint) {
            this.fFieldCount = 0;
            this.fFields = null;
            this.fLocalValues = null;
            this.fLocalValueTypes = null;
            this.fLocalItemValueTypes = null;
            this.fIdentityConstraint = identityConstraint;
            this.fFieldCount = this.fIdentityConstraint.getFieldCount();
            this.fFields = new Field[this.fFieldCount];
            this.fLocalValues = new Object[this.fFieldCount];
            this.fLocalValueTypes = new short[this.fFieldCount];
            this.fLocalItemValueTypes = new ShortList[this.fFieldCount];
            for (int i = 0; i < this.fFieldCount; i++) {
                this.fFields[i] = this.fIdentityConstraint.getFieldAt(i);
            }
        }

        public void clear() {
            this.fValuesCount = 0;
            this.fUseValueTypeVector = false;
            this.fValueTypesLength = 0;
            this.fValueType = (short) 0;
            this.fUseItemValueTypeVector = false;
            this.fItemValueTypesLength = 0;
            this.fItemValueType = null;
            this.fValues.setSize(0);
            if (this.fValueTypes != null) {
                this.fValueTypes.clear();
            }
            if (this.fItemValueTypes != null) {
                this.fItemValueTypes.setSize(0);
            }
        }

        public void append(ValueStoreBase newVal) {
            for (int i = 0; i < newVal.fValues.size(); i++) {
                this.fValues.addElement(newVal.fValues.elementAt(i));
            }
        }

        public void startValueScope() {
            this.fValuesCount = 0;
            for (int i = 0; i < this.fFieldCount; i++) {
                this.fLocalValues[i] = null;
                this.fLocalValueTypes[i] = 0;
                this.fLocalItemValueTypes[i] = null;
            }
        }

        public void endValueScope() {
            if (this.fValuesCount == 0) {
                if (this.fIdentityConstraint.getCategory() == 1) {
                    String eName = this.fIdentityConstraint.getElementName();
                    String cName = this.fIdentityConstraint.getIdentityConstraintName();
                    XMLSchemaValidator.this.reportSchemaError("AbsentKeyValue", new Object[]{eName, cName});
                    return;
                }
                return;
            }
            if (this.fValuesCount != this.fFieldCount && this.fIdentityConstraint.getCategory() == 1) {
                UniqueOrKey key = (UniqueOrKey) this.fIdentityConstraint;
                String eName2 = this.fIdentityConstraint.getElementName();
                String cName2 = key.getIdentityConstraintName();
                XMLSchemaValidator.this.reportSchemaError("KeyNotEnoughValues", new Object[]{eName2, cName2});
            }
        }

        public void endDocumentFragment() {
        }

        public void endDocument() {
        }

        @Override
        public void reportError(String key, Object[] args) {
            XMLSchemaValidator.this.reportSchemaError(key, args);
        }

        @Override
        public void addValue(Field field, boolean mayMatch, Object actualValue, short valueType, ShortList itemValueType) {
            int i = this.fFieldCount - 1;
            while (i > -1 && this.fFields[i] != field) {
                i--;
            }
            if (i == -1) {
                String eName = this.fIdentityConstraint.getElementName();
                String cName = this.fIdentityConstraint.getIdentityConstraintName();
                XMLSchemaValidator.this.reportSchemaError("UnknownField", new Object[]{field.toString(), eName, cName});
                return;
            }
            if (mayMatch) {
                this.fValuesCount++;
            } else {
                String cName2 = this.fIdentityConstraint.getIdentityConstraintName();
                XMLSchemaValidator.this.reportSchemaError("FieldMultipleMatch", new Object[]{field.toString(), cName2});
            }
            this.fLocalValues[i] = actualValue;
            this.fLocalValueTypes[i] = valueType;
            this.fLocalItemValueTypes[i] = itemValueType;
            if (this.fValuesCount == this.fFieldCount) {
                checkDuplicateValues();
                for (int i2 = 0; i2 < this.fFieldCount; i2++) {
                    this.fValues.addElement(this.fLocalValues[i2]);
                    addValueType(this.fLocalValueTypes[i2]);
                    addItemValueType(this.fLocalItemValueTypes[i2]);
                }
            }
        }

        public boolean contains() {
            int size = this.fValues.size();
            int i = 0;
            while (i < size) {
                int next = i + this.fFieldCount;
                for (int j = 0; j < this.fFieldCount; j++) {
                    Object value1 = this.fLocalValues[j];
                    Object value2 = this.fValues.elementAt(i);
                    short valueType1 = this.fLocalValueTypes[j];
                    short valueType2 = getValueTypeAt(i);
                    if (value1 == null || value2 == null || valueType1 != valueType2 || !value1.equals(value2)) {
                        break;
                    }
                    if (valueType1 == 44 || valueType1 == 43) {
                        ShortList list1 = this.fLocalItemValueTypes[j];
                        ShortList list2 = getItemValueTypeAt(i);
                        if (list1 == null || list2 == null || !list1.equals(list2)) {
                            break;
                        }
                    }
                    i++;
                    i = next;
                }
                return true;
            }
            return false;
        }

        public int contains(ValueStoreBase vsb) {
            Vector values = vsb.fValues;
            int size1 = values.size();
            if (this.fFieldCount <= 1) {
                for (int i = 0; i < size1; i++) {
                    short val = vsb.getValueTypeAt(i);
                    if (!valueTypeContains(val) || !this.fValues.contains(values.elementAt(i))) {
                        return i;
                    }
                    if ((val == 44 || val == 43) && !itemValueTypeContains(vsb.getItemValueTypeAt(i))) {
                        return i;
                    }
                }
                return -1;
            }
            int size2 = this.fValues.size();
            int i2 = 0;
            while (i2 < size1) {
                int j = 0;
                while (j < size2) {
                    for (int k = 0; k < this.fFieldCount; k++) {
                        Object value1 = values.elementAt(i2 + k);
                        Object value2 = this.fValues.elementAt(j + k);
                        short valueType1 = vsb.getValueTypeAt(i2 + k);
                        short valueType2 = getValueTypeAt(j + k);
                        if (value1 == value2 || (valueType1 == valueType2 && value1 != null && value1.equals(value2))) {
                            if (valueType1 == 44 || valueType1 == 43) {
                                ShortList list1 = vsb.getItemValueTypeAt(i2 + k);
                                ShortList list2 = getItemValueTypeAt(j + k);
                                if (list1 == null || list2 == null || !list1.equals(list2)) {
                                    break;
                                }
                            }
                        }
                        int k2 = this.fFieldCount;
                        j += k2;
                    }
                }
                return i2;
            }
            return -1;
        }

        protected void checkDuplicateValues() {
        }

        protected String toString(Object[] values) {
            int size = values.length;
            if (size == 0) {
                return "";
            }
            this.fTempBuffer.setLength(0);
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    this.fTempBuffer.append(',');
                }
                this.fTempBuffer.append(values[i]);
            }
            return this.fTempBuffer.toString();
        }

        protected String toString(Vector values, int start, int length) {
            if (length == 0) {
                return "";
            }
            if (length == 1) {
                return String.valueOf(values.elementAt(start));
            }
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    str.append(',');
                }
                str.append(values.elementAt(start + i));
            }
            return str.toString();
        }

        public String toString() {
            String s = super.toString();
            int index1 = s.lastIndexOf(36);
            if (index1 != -1) {
                s = s.substring(index1 + 1);
            }
            int index2 = s.lastIndexOf(46);
            if (index2 != -1) {
                s = s.substring(index2 + 1);
            }
            return String.valueOf(s) + '[' + this.fIdentityConstraint + ']';
        }

        private void addValueType(short type) {
            if (this.fUseValueTypeVector) {
                this.fValueTypes.add(type);
                return;
            }
            int i = this.fValueTypesLength;
            this.fValueTypesLength = i + 1;
            if (i == 0) {
                this.fValueType = type;
                return;
            }
            if (this.fValueType != type) {
                this.fUseValueTypeVector = true;
                if (this.fValueTypes == null) {
                    this.fValueTypes = new ShortVector(this.fValueTypesLength * 2);
                }
                for (int i2 = 1; i2 < this.fValueTypesLength; i2++) {
                    this.fValueTypes.add(this.fValueType);
                }
                this.fValueTypes.add(type);
            }
        }

        private short getValueTypeAt(int index) {
            if (this.fUseValueTypeVector) {
                return this.fValueTypes.valueAt(index);
            }
            return this.fValueType;
        }

        private boolean valueTypeContains(short value) {
            if (this.fUseValueTypeVector) {
                return this.fValueTypes.contains(value);
            }
            return this.fValueType == value;
        }

        private void addItemValueType(ShortList itemValueType) {
            if (this.fUseItemValueTypeVector) {
                this.fItemValueTypes.add(itemValueType);
                return;
            }
            int i = this.fItemValueTypesLength;
            this.fItemValueTypesLength = i + 1;
            if (i == 0) {
                this.fItemValueType = itemValueType;
                return;
            }
            if (this.fItemValueType != itemValueType) {
                if (this.fItemValueType == null || !this.fItemValueType.equals(itemValueType)) {
                    this.fUseItemValueTypeVector = true;
                    if (this.fItemValueTypes == null) {
                        this.fItemValueTypes = new Vector(this.fItemValueTypesLength * 2);
                    }
                    for (int i2 = 1; i2 < this.fItemValueTypesLength; i2++) {
                        this.fItemValueTypes.add(this.fItemValueType);
                    }
                    this.fItemValueTypes.add(itemValueType);
                }
            }
        }

        private ShortList getItemValueTypeAt(int index) {
            if (this.fUseItemValueTypeVector) {
                return (ShortList) this.fItemValueTypes.elementAt(index);
            }
            return this.fItemValueType;
        }

        private boolean itemValueTypeContains(ShortList value) {
            if (this.fUseItemValueTypeVector) {
                return this.fItemValueTypes.contains(value);
            }
            if (this.fItemValueType != value) {
                return this.fItemValueType != null && this.fItemValueType.equals(value);
            }
            return true;
        }
    }

    protected class UniqueValueStore extends ValueStoreBase {
        public UniqueValueStore(UniqueOrKey unique) {
            super(unique);
        }

        @Override
        protected void checkDuplicateValues() {
            if (contains()) {
                String value = toString(this.fLocalValues);
                String eName = this.fIdentityConstraint.getElementName();
                String cName = this.fIdentityConstraint.getIdentityConstraintName();
                XMLSchemaValidator.this.reportSchemaError("DuplicateUnique", new Object[]{value, eName, cName});
            }
        }
    }

    protected class KeyValueStore extends ValueStoreBase {
        public KeyValueStore(UniqueOrKey key) {
            super(key);
        }

        @Override
        protected void checkDuplicateValues() {
            if (contains()) {
                String value = toString(this.fLocalValues);
                String eName = this.fIdentityConstraint.getElementName();
                String cName = this.fIdentityConstraint.getIdentityConstraintName();
                XMLSchemaValidator.this.reportSchemaError("DuplicateKey", new Object[]{value, eName, cName});
            }
        }
    }

    protected class KeyRefValueStore extends ValueStoreBase {
        protected ValueStoreBase fKeyValueStore;

        public KeyRefValueStore(KeyRef keyRef, KeyValueStore keyValueStore) {
            super(keyRef);
            this.fKeyValueStore = keyValueStore;
        }

        @Override
        public void endDocumentFragment() {
            super.endDocumentFragment();
            this.fKeyValueStore = (ValueStoreBase) XMLSchemaValidator.this.fValueStoreCache.fGlobalIDConstraintMap.get(((KeyRef) this.fIdentityConstraint).getKey());
            if (this.fKeyValueStore == null) {
                String value = this.fIdentityConstraint.toString();
                XMLSchemaValidator.this.reportSchemaError("KeyRefOutOfScope", new Object[]{value});
                return;
            }
            int errorIndex = this.fKeyValueStore.contains(this);
            if (errorIndex != -1) {
                String values = toString(this.fValues, errorIndex, this.fFieldCount);
                String element = this.fIdentityConstraint.getElementName();
                String name = this.fIdentityConstraint.getName();
                XMLSchemaValidator.this.reportSchemaError("KeyNotFound", new Object[]{name, values, element});
            }
        }

        @Override
        public void endDocument() {
            super.endDocument();
        }
    }

    protected class ValueStoreCache {
        final LocalIDKey fLocalId = new LocalIDKey();
        protected final ArrayList fValueStores = new ArrayList();
        protected final HashMap fIdentityConstraint2ValueStoreMap = new HashMap();
        protected final Stack fGlobalMapStack = new Stack();
        protected final HashMap fGlobalIDConstraintMap = new HashMap();

        public ValueStoreCache() {
        }

        public void startDocument() {
            this.fValueStores.clear();
            this.fIdentityConstraint2ValueStoreMap.clear();
            this.fGlobalIDConstraintMap.clear();
            this.fGlobalMapStack.removeAllElements();
        }

        public void startElement() {
            if (this.fGlobalIDConstraintMap.size() > 0) {
                this.fGlobalMapStack.push(this.fGlobalIDConstraintMap.clone());
            } else {
                this.fGlobalMapStack.push(null);
            }
            this.fGlobalIDConstraintMap.clear();
        }

        public void endElement() {
            HashMap oldMap;
            if (this.fGlobalMapStack.isEmpty() || (oldMap = (HashMap) this.fGlobalMapStack.pop()) == null) {
                return;
            }
            for (Map.Entry entry : oldMap.entrySet()) {
                IdentityConstraint id = (IdentityConstraint) entry.getKey();
                ValueStoreBase oldVal = (ValueStoreBase) entry.getValue();
                if (oldVal != null) {
                    ValueStoreBase currVal = (ValueStoreBase) this.fGlobalIDConstraintMap.get(id);
                    if (currVal == null) {
                        this.fGlobalIDConstraintMap.put(id, oldVal);
                    } else if (currVal != oldVal) {
                        currVal.append(oldVal);
                    }
                }
            }
        }

        public void initValueStoresFor(XSElementDecl eDecl, FieldActivator activator) {
            IdentityConstraint[] icArray = eDecl.fIDConstraints;
            int icCount = eDecl.fIDCPos;
            for (int i = 0; i < icCount; i++) {
                switch (icArray[i].getCategory()) {
                    case 1:
                        UniqueOrKey key = (UniqueOrKey) icArray[i];
                        LocalIDKey toHash = new LocalIDKey(key, XMLSchemaValidator.this.fElementDepth);
                        KeyValueStore keyValueStore = (KeyValueStore) this.fIdentityConstraint2ValueStoreMap.get(toHash);
                        if (keyValueStore == null) {
                            keyValueStore = XMLSchemaValidator.this.new KeyValueStore(key);
                            this.fIdentityConstraint2ValueStoreMap.put(toHash, keyValueStore);
                        } else {
                            keyValueStore.clear();
                        }
                        this.fValueStores.add(keyValueStore);
                        XMLSchemaValidator.this.activateSelectorFor(icArray[i]);
                        break;
                    case 2:
                        KeyRef keyRef = (KeyRef) icArray[i];
                        LocalIDKey toHash2 = new LocalIDKey(keyRef, XMLSchemaValidator.this.fElementDepth);
                        KeyRefValueStore keyRefValueStore = (KeyRefValueStore) this.fIdentityConstraint2ValueStoreMap.get(toHash2);
                        if (keyRefValueStore == null) {
                            keyRefValueStore = XMLSchemaValidator.this.new KeyRefValueStore(keyRef, null);
                            this.fIdentityConstraint2ValueStoreMap.put(toHash2, keyRefValueStore);
                        } else {
                            keyRefValueStore.clear();
                        }
                        this.fValueStores.add(keyRefValueStore);
                        XMLSchemaValidator.this.activateSelectorFor(icArray[i]);
                        break;
                    case 3:
                        UniqueOrKey unique = (UniqueOrKey) icArray[i];
                        LocalIDKey toHash3 = new LocalIDKey(unique, XMLSchemaValidator.this.fElementDepth);
                        UniqueValueStore uniqueValueStore = (UniqueValueStore) this.fIdentityConstraint2ValueStoreMap.get(toHash3);
                        if (uniqueValueStore == null) {
                            uniqueValueStore = XMLSchemaValidator.this.new UniqueValueStore(unique);
                            this.fIdentityConstraint2ValueStoreMap.put(toHash3, uniqueValueStore);
                        } else {
                            uniqueValueStore.clear();
                        }
                        this.fValueStores.add(uniqueValueStore);
                        XMLSchemaValidator.this.activateSelectorFor(icArray[i]);
                        break;
                }
            }
        }

        public ValueStoreBase getValueStoreFor(IdentityConstraint id, int initialDepth) {
            this.fLocalId.fDepth = initialDepth;
            this.fLocalId.fId = id;
            return (ValueStoreBase) this.fIdentityConstraint2ValueStoreMap.get(this.fLocalId);
        }

        public ValueStoreBase getGlobalValueStoreFor(IdentityConstraint id) {
            return (ValueStoreBase) this.fGlobalIDConstraintMap.get(id);
        }

        public void transplant(IdentityConstraint id, int initialDepth) {
            this.fLocalId.fDepth = initialDepth;
            this.fLocalId.fId = id;
            ValueStoreBase newVals = (ValueStoreBase) this.fIdentityConstraint2ValueStoreMap.get(this.fLocalId);
            if (id.getCategory() == 2) {
                return;
            }
            ValueStoreBase currVals = (ValueStoreBase) this.fGlobalIDConstraintMap.get(id);
            if (currVals != null) {
                currVals.append(newVals);
                this.fGlobalIDConstraintMap.put(id, currVals);
            } else {
                this.fGlobalIDConstraintMap.put(id, newVals);
            }
        }

        public void endDocument() {
            int count = this.fValueStores.size();
            for (int i = 0; i < count; i++) {
                ValueStoreBase valueStore = (ValueStoreBase) this.fValueStores.get(i);
                valueStore.endDocument();
            }
        }

        public String toString() {
            String s = super.toString();
            int index1 = s.lastIndexOf(36);
            if (index1 != -1) {
                return s.substring(index1 + 1);
            }
            int index2 = s.lastIndexOf(46);
            if (index2 != -1) {
                return s.substring(index2 + 1);
            }
            return s;
        }
    }

    protected static final class LocalIDKey {
        public int fDepth;
        public IdentityConstraint fId;

        public LocalIDKey() {
        }

        public LocalIDKey(IdentityConstraint id, int depth) {
            this.fId = id;
            this.fDepth = depth;
        }

        public int hashCode() {
            return this.fId.hashCode() + this.fDepth;
        }

        public boolean equals(Object localIDKey) {
            if (!(localIDKey instanceof LocalIDKey)) {
                return false;
            }
            LocalIDKey lIDKey = (LocalIDKey) localIDKey;
            return lIDKey.fId == this.fId && lIDKey.fDepth == this.fDepth;
        }
    }

    protected static final class ShortVector {
        private short[] fData;
        private int fLength;

        public ShortVector() {
        }

        public ShortVector(int initialCapacity) {
            this.fData = new short[initialCapacity];
        }

        public int length() {
            return this.fLength;
        }

        public void add(short value) {
            ensureCapacity(this.fLength + 1);
            short[] sArr = this.fData;
            int i = this.fLength;
            this.fLength = i + 1;
            sArr[i] = value;
        }

        public short valueAt(int position) {
            return this.fData[position];
        }

        public void clear() {
            this.fLength = 0;
        }

        public boolean contains(short value) {
            for (int i = 0; i < this.fLength; i++) {
                if (this.fData[i] == value) {
                    return true;
                }
            }
            return false;
        }

        private void ensureCapacity(int size) {
            if (this.fData == null) {
                this.fData = new short[8];
            } else if (this.fData.length <= size) {
                short[] newdata = new short[this.fData.length * 2];
                System.arraycopy(this.fData, 0, newdata, 0, this.fData.length);
                this.fData = newdata;
            }
        }
    }
}
