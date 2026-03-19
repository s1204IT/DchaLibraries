package mf.org.apache.xerces.impl.xs.traversers;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;
import mf.javax.xml.stream.XMLEventReader;
import mf.javax.xml.stream.XMLStreamException;
import mf.javax.xml.stream.XMLStreamReader;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dv.SchemaDVFactory;
import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaNamespaceSupport;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XMLSchemaException;
import mf.org.apache.xerces.impl.xs.XMLSchemaLoader;
import mf.org.apache.xerces.impl.xs.XSAttributeDecl;
import mf.org.apache.xerces.impl.xs.XSAttributeGroupDecl;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.impl.xs.XSDDescription;
import mf.org.apache.xerces.impl.xs.XSDeclarationPool;
import mf.org.apache.xerces.impl.xs.XSElementDecl;
import mf.org.apache.xerces.impl.xs.XSGrammarBucket;
import mf.org.apache.xerces.impl.xs.XSGroupDecl;
import mf.org.apache.xerces.impl.xs.XSMessageFormatter;
import mf.org.apache.xerces.impl.xs.XSModelGroupImpl;
import mf.org.apache.xerces.impl.xs.XSNotationDecl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.impl.xs.identity.IdentityConstraint;
import mf.org.apache.xerces.impl.xs.opti.ElementImpl;
import mf.org.apache.xerces.impl.xs.opti.SchemaDOM;
import mf.org.apache.xerces.impl.xs.opti.SchemaDOMParser;
import mf.org.apache.xerces.impl.xs.opti.SchemaParsingConfig;
import mf.org.apache.xerces.impl.xs.util.SimpleLocator;
import mf.org.apache.xerces.impl.xs.util.XSInputSource;
import mf.org.apache.xerces.parsers.SAXParser;
import mf.org.apache.xerces.parsers.XML11Configuration;
import mf.org.apache.xerces.util.DOMInputSource;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.DefaultErrorHandler;
import mf.org.apache.xerces.util.ErrorHandlerWrapper;
import mf.org.apache.xerces.util.SAXInputSource;
import mf.org.apache.xerces.util.StAXInputSource;
import mf.org.apache.xerces.util.StAXLocationWrapper;
import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;
import mf.org.apache.xerces.xni.grammars.XMLSchemaDescription;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSAttributeDeclaration;
import mf.org.apache.xerces.xs.XSAttributeGroupDefinition;
import mf.org.apache.xerces.xs.XSAttributeUse;
import mf.org.apache.xerces.xs.XSElementDeclaration;
import mf.org.apache.xerces.xs.XSModelGroup;
import mf.org.apache.xerces.xs.XSModelGroupDefinition;
import mf.org.apache.xerces.xs.XSNamedMap;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSParticle;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTerm;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.apache.xerces.xs.datatypes.ObjectList;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XSDHandler {
    protected static final String ALLOW_JAVA_ENCODINGS = "http://apache.org/xml/features/allow-java-encodings";
    static final int ATTRIBUTEGROUP_TYPE = 2;
    static final int ATTRIBUTE_TYPE = 1;
    private static final String[] CIRCULAR_CODES;
    private static final String[] COMP_TYPE;
    protected static final String CONTINUE_AFTER_FATAL_ERROR = "http://apache.org/xml/features/continue-after-fatal-error";
    protected static final boolean DEBUG_NODE_POOL = false;
    protected static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    static final int ELEMENT_TYPE = 3;
    private static final String[] ELE_ERROR_CODES;
    private static final Hashtable EMPTY_TABLE = new Hashtable();
    protected static final String ENTITY_MANAGER = "http://apache.org/xml/properties/internal/entity-manager";
    public static final String ENTITY_RESOLVER = "http://apache.org/xml/properties/internal/entity-resolver";
    protected static final String ERROR_HANDLER = "http://apache.org/xml/properties/internal/error-handler";
    public static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS = "http://apache.org/xml/features/generate-synthetic-annotations";
    static final int GROUP_TYPE = 4;
    protected static final String HONOUR_ALL_SCHEMALOCATIONS = "http://apache.org/xml/features/honour-all-schemaLocations";
    static final int IDENTITYCONSTRAINT_TYPE = 5;
    private static final int INC_KEYREF_STACK_AMOUNT = 2;
    private static final int INC_STACK_SIZE = 10;
    private static final int INIT_KEYREF_STACK = 2;
    private static final int INIT_STACK_SIZE = 30;
    protected static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    protected static final String LOCALE = "http://apache.org/xml/properties/locale";
    protected static final String NAMESPACE_GROWTH = "http://apache.org/xml/features/namespace-growth";
    private static final String NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    static final int NOTATION_TYPE = 6;
    private static final String[][] NS_ERROR_CODES;
    public static final String REDEF_IDENTIFIER = "_fn3dktizrknc9pi";
    protected static final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";
    protected static final String STANDARD_URI_CONFORMANT_FEATURE = "http://apache.org/xml/features/standard-uri-conformant";
    protected static final String STRING_INTERNING = "http://xml.org/sax/features/string-interning";
    public static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    protected static final String TOLERATE_DUPLICATES = "http://apache.org/xml/features/internal/tolerate-duplicates";
    static final int TYPEDECL_TYPE = 7;
    protected static final String VALIDATE_ANNOTATIONS = "http://apache.org/xml/features/validate-annotations";
    protected static final String VALIDATION = "http://xml.org/sax/features/validation";
    public static final String XMLGRAMMAR_POOL = "http://apache.org/xml/properties/internal/grammar-pool";
    protected static final String XMLSCHEMA_VALIDATION = "http://apache.org/xml/features/validation/schema";
    private int[] fAllContext;
    private Vector fAllTNSs;
    XML11Configuration fAnnotationValidator;
    private XSAttributeChecker fAttributeChecker;
    XSDAttributeGroupTraverser fAttributeGroupTraverser;
    XSDAttributeTraverser fAttributeTraverser;
    XSDComplexTypeTraverser fComplexTypeTraverser;
    SchemaDVFactory fDVFactory;
    protected XSDeclarationPool fDeclPool;
    private Hashtable fDependencyMap;
    private Hashtable fDoc2SystemId;
    private Hashtable fDoc2XSDocumentMap;
    XSDElementTraverser fElementTraverser;
    private XMLEntityResolver fEntityResolver;
    private XMLErrorReporter fErrorReporter;
    SymbolHash fGlobalAttrDecls;
    SymbolHash fGlobalAttrGrpDecls;
    SymbolHash fGlobalElemDecls;
    SymbolHash fGlobalGroupDecls;
    SymbolHash fGlobalIDConstraintDecls;
    SymbolHash fGlobalNotationDecls;
    SymbolHash fGlobalTypeDecls;
    private XSGrammarBucket fGrammarBucket;
    XSAnnotationGrammarPool fGrammarBucketAdapter;
    private XMLGrammarPool fGrammarPool;
    XSDGroupTraverser fGroupTraverser;
    Hashtable fHiddenNodes;
    private boolean fHonourAllSchemaLocations;
    private Hashtable fImportMap;
    private XSElementDecl[] fKeyrefElems;
    private String[][] fKeyrefNamespaceContext;
    private int fKeyrefStackPos;
    XSDKeyrefTraverser fKeyrefTraverser;
    private Element[] fKeyrefs;
    private XSDocumentInfo[] fKeyrefsMapXSDocumentInfo;
    private boolean fLastSchemaWasDuplicate;
    private String[][] fLocalElemNamespaceContext;
    private int fLocalElemStackPos;
    private Element[] fLocalElementDecl;
    private XSDocumentInfo[] fLocalElementDecl_schema;
    private Hashtable fLocationPairs;
    boolean fNamespaceGrowth;
    protected Hashtable fNotationRegistry;
    XSDNotationTraverser fNotationTraverser;
    private XSObject[] fParent;
    private XSParticleDecl[] fParticle;
    private Hashtable fRedefine2NSSupport;
    private Hashtable fRedefine2XSDMap;
    private Hashtable fRedefinedRestrictedAttributeGroupRegistry;
    private Hashtable fRedefinedRestrictedGroupRegistry;
    private Vector fReportedTNS;
    private XSDocumentInfo fRoot;
    private XSDDescription fSchemaGrammarDescription;
    SchemaDOMParser fSchemaParser;
    XSDSimpleTypeTraverser fSimpleTypeTraverser;
    StAXSchemaParser fStAXSchemaParser;
    private SymbolTable fSymbolTable;
    boolean fTolerateDuplicates;
    private Hashtable fTraversed;
    XSDUniqueOrKeyTraverser fUniqueOrKeyTraverser;
    private Hashtable fUnparsedAttributeGroupRegistry;
    private Hashtable fUnparsedAttributeGroupRegistrySub;
    private Hashtable fUnparsedAttributeRegistry;
    private Hashtable fUnparsedAttributeRegistrySub;
    private Hashtable fUnparsedElementRegistry;
    private Hashtable fUnparsedElementRegistrySub;
    private Hashtable fUnparsedGroupRegistry;
    private Hashtable fUnparsedGroupRegistrySub;
    private Hashtable fUnparsedIdentityConstraintRegistry;
    private Hashtable fUnparsedIdentityConstraintRegistrySub;
    private Hashtable fUnparsedNotationRegistry;
    private Hashtable fUnparsedNotationRegistrySub;
    private Hashtable[] fUnparsedRegistriesExt;
    private Hashtable fUnparsedTypeRegistry;
    private Hashtable fUnparsedTypeRegistrySub;
    private boolean fValidateAnnotations;
    XSDWildcardTraverser fWildCardTraverser;
    SchemaContentHandler fXSContentHandler;
    private Hashtable fXSDocumentInfoRegistry;
    private SimpleLocator xl;

    static {
        String[][] strArr = new String[8][];
        strArr[0] = new String[]{"src-include.2.1", "src-include.2.1"};
        strArr[1] = new String[]{"src-redefine.3.1", "src-redefine.3.1"};
        strArr[2] = new String[]{"src-import.3.1", "src-import.3.2"};
        strArr[4] = new String[]{"TargetNamespace.1", "TargetNamespace.2"};
        strArr[5] = new String[]{"TargetNamespace.1", "TargetNamespace.2"};
        strArr[6] = new String[]{"TargetNamespace.1", "TargetNamespace.2"};
        strArr[7] = new String[]{"TargetNamespace.1", "TargetNamespace.2"};
        NS_ERROR_CODES = strArr;
        ELE_ERROR_CODES = new String[]{"src-include.1", "src-redefine.2", "src-import.2", "schema_reference.4", "schema_reference.4", "schema_reference.4", "schema_reference.4", "schema_reference.4"};
        String[] strArr2 = new String[8];
        strArr2[1] = "attribute declaration";
        strArr2[2] = "attribute group";
        strArr2[3] = "element declaration";
        strArr2[4] = "group";
        strArr2[5] = "identity constraint";
        strArr2[6] = "notation";
        strArr2[7] = "type definition";
        COMP_TYPE = strArr2;
        CIRCULAR_CODES = new String[]{"Internal-Error", "Internal-Error", "src-attribute_group.3", "e-props-correct.6", "mg-props-correct.2", "Internal-Error", "Internal-Error", "st-props-correct.2"};
    }

    private String null2EmptyString(String ns) {
        return ns == null ? XMLSymbols.EMPTY_STRING : ns;
    }

    private String emptyString2Null(String ns) {
        if (ns == XMLSymbols.EMPTY_STRING) {
            return null;
        }
        return ns;
    }

    private String doc2SystemId(Element ele) {
        String documentURI = null;
        if (ele.getOwnerDocument() instanceof SchemaDOM) {
            documentURI = ((SchemaDOM) ele.getOwnerDocument()).getDocumentURI();
        }
        return documentURI != null ? documentURI : (String) this.fDoc2SystemId.get(ele);
    }

    public XSDHandler() {
        this.fNotationRegistry = new Hashtable();
        this.fDeclPool = null;
        this.fUnparsedAttributeRegistry = new Hashtable();
        this.fUnparsedAttributeGroupRegistry = new Hashtable();
        this.fUnparsedElementRegistry = new Hashtable();
        this.fUnparsedGroupRegistry = new Hashtable();
        this.fUnparsedIdentityConstraintRegistry = new Hashtable();
        this.fUnparsedNotationRegistry = new Hashtable();
        this.fUnparsedTypeRegistry = new Hashtable();
        this.fUnparsedAttributeRegistrySub = new Hashtable();
        this.fUnparsedAttributeGroupRegistrySub = new Hashtable();
        this.fUnparsedElementRegistrySub = new Hashtable();
        this.fUnparsedGroupRegistrySub = new Hashtable();
        this.fUnparsedIdentityConstraintRegistrySub = new Hashtable();
        this.fUnparsedNotationRegistrySub = new Hashtable();
        this.fUnparsedTypeRegistrySub = new Hashtable();
        Hashtable[] hashtableArr = new Hashtable[8];
        hashtableArr[1] = new Hashtable();
        hashtableArr[2] = new Hashtable();
        hashtableArr[3] = new Hashtable();
        hashtableArr[4] = new Hashtable();
        hashtableArr[5] = new Hashtable();
        hashtableArr[6] = new Hashtable();
        hashtableArr[7] = new Hashtable();
        this.fUnparsedRegistriesExt = hashtableArr;
        this.fXSDocumentInfoRegistry = new Hashtable();
        this.fDependencyMap = new Hashtable();
        this.fImportMap = new Hashtable();
        this.fAllTNSs = new Vector();
        this.fLocationPairs = null;
        this.fHiddenNodes = null;
        this.fTraversed = new Hashtable();
        this.fDoc2SystemId = new Hashtable();
        this.fRoot = null;
        this.fDoc2XSDocumentMap = new Hashtable();
        this.fRedefine2XSDMap = new Hashtable();
        this.fRedefine2NSSupport = new Hashtable();
        this.fRedefinedRestrictedAttributeGroupRegistry = new Hashtable();
        this.fRedefinedRestrictedGroupRegistry = new Hashtable();
        this.fValidateAnnotations = false;
        this.fHonourAllSchemaLocations = false;
        this.fNamespaceGrowth = false;
        this.fTolerateDuplicates = false;
        this.fLocalElemStackPos = 0;
        this.fParticle = new XSParticleDecl[30];
        this.fLocalElementDecl = new Element[30];
        this.fLocalElementDecl_schema = new XSDocumentInfo[30];
        this.fAllContext = new int[30];
        this.fParent = new XSObject[30];
        this.fLocalElemNamespaceContext = (String[][]) Array.newInstance((Class<?>) String.class, 30, 1);
        this.fKeyrefStackPos = 0;
        this.fKeyrefs = new Element[2];
        this.fKeyrefsMapXSDocumentInfo = new XSDocumentInfo[2];
        this.fKeyrefElems = new XSElementDecl[2];
        this.fKeyrefNamespaceContext = (String[][]) Array.newInstance((Class<?>) String.class, 2, 1);
        this.fGlobalAttrDecls = new SymbolHash();
        this.fGlobalAttrGrpDecls = new SymbolHash();
        this.fGlobalElemDecls = new SymbolHash();
        this.fGlobalGroupDecls = new SymbolHash();
        this.fGlobalNotationDecls = new SymbolHash();
        this.fGlobalIDConstraintDecls = new SymbolHash();
        this.fGlobalTypeDecls = new SymbolHash();
        this.fReportedTNS = null;
        this.xl = new SimpleLocator();
        this.fHiddenNodes = new Hashtable();
        this.fSchemaParser = new SchemaDOMParser(new SchemaParsingConfig());
    }

    public XSDHandler(XSGrammarBucket gBucket) {
        this();
        this.fGrammarBucket = gBucket;
        this.fSchemaGrammarDescription = new XSDDescription();
    }

    public SchemaGrammar parseSchema(XMLInputSource xMLInputSource, XSDDescription desc, Hashtable locationPairs) throws IOException {
        SchemaGrammar grammar;
        String schemaNamespace;
        Element schemaRoot;
        SchemaGrammar grammar2;
        this.fLocationPairs = locationPairs;
        this.fSchemaParser.resetNodePool();
        short referType = desc.getContextType();
        if (referType == 3) {
            grammar = null;
            schemaNamespace = null;
        } else {
            if (this.fHonourAllSchemaLocations && referType == 2 && isExistingGrammar(desc, this.fNamespaceGrowth)) {
                grammar2 = this.fGrammarBucket.getGrammar(desc.getTargetNamespace());
            } else {
                grammar2 = findGrammar(desc, this.fNamespaceGrowth);
            }
            SchemaGrammar grammar3 = grammar2;
            if (grammar3 != null) {
                if (!this.fNamespaceGrowth) {
                    return grammar3;
                }
                try {
                    if (grammar3.getDocumentLocations().contains(XMLEntityManager.expandSystemId(xMLInputSource.getSystemId(), xMLInputSource.getBaseSystemId(), false))) {
                        return grammar3;
                    }
                } catch (URI.MalformedURIException e) {
                }
            }
            schemaNamespace = desc.getTargetNamespace();
            if (schemaNamespace != null) {
                schemaNamespace = this.fSymbolTable.addSymbol(schemaNamespace);
            }
            grammar = grammar3;
        }
        prepareForParse();
        if (xMLInputSource instanceof DOMInputSource) {
            schemaRoot = getSchemaDocument(schemaNamespace, (DOMInputSource) xMLInputSource, referType == 3, referType, (Element) null);
        } else if (xMLInputSource instanceof SAXInputSource) {
            schemaRoot = getSchemaDocument(schemaNamespace, (SAXInputSource) xMLInputSource, referType == 3, referType, (Element) null);
        } else if (xMLInputSource instanceof StAXInputSource) {
            schemaRoot = getSchemaDocument(schemaNamespace, (StAXInputSource) xMLInputSource, referType == 3, referType, (Element) null);
        } else if (xMLInputSource instanceof XSInputSource) {
            schemaRoot = getSchemaDocument(xMLInputSource, desc);
        } else {
            schemaRoot = getSchemaDocument(schemaNamespace, xMLInputSource, referType == 3, referType, (Element) null);
        }
        if (schemaRoot == null) {
            if (xMLInputSource instanceof XSInputSource) {
                return this.fGrammarBucket.getGrammar(desc.getTargetNamespace());
            }
            return grammar;
        }
        if (referType == 3) {
            Element schemaElem = schemaRoot;
            String schemaNamespace2 = DOMUtil.getAttrValue(schemaElem, SchemaSymbols.ATT_TARGETNAMESPACE);
            if (schemaNamespace2 != null && schemaNamespace2.length() > 0) {
                schemaNamespace = this.fSymbolTable.addSymbol(schemaNamespace2);
                desc.setTargetNamespace(schemaNamespace);
            } else {
                schemaNamespace = null;
            }
            grammar = findGrammar(desc, this.fNamespaceGrowth);
            String schemaId = XMLEntityManager.expandSystemId(xMLInputSource.getSystemId(), xMLInputSource.getBaseSystemId(), false);
            if (grammar != null && (!this.fNamespaceGrowth || (schemaId != null && grammar.getDocumentLocations().contains(schemaId)))) {
                return grammar;
            }
            XSDKey key = new XSDKey(schemaId, referType, schemaNamespace);
            this.fTraversed.put(key, schemaRoot);
            if (schemaId != null) {
                this.fDoc2SystemId.put(schemaRoot, schemaId);
            }
        }
        prepareForTraverse();
        this.fRoot = constructTrees(schemaRoot, xMLInputSource.getSystemId(), desc, grammar != null);
        if (this.fRoot == null) {
            return null;
        }
        buildGlobalNameRegistries();
        ArrayList annotationInfo = this.fValidateAnnotations ? new ArrayList() : null;
        traverseSchemas(annotationInfo);
        traverseLocalElements();
        resolveKeyRefs();
        for (int i = this.fAllTNSs.size() - 1; i >= 0; i--) {
            String tns = (String) this.fAllTNSs.elementAt(i);
            Vector ins = (Vector) this.fImportMap.get(tns);
            SchemaGrammar sg = this.fGrammarBucket.getGrammar(emptyString2Null(tns));
            if (sg != null) {
                int count = 0;
                int j = 0;
                while (j < ins.size()) {
                    XSGrammarBucket xSGrammarBucket = this.fGrammarBucket;
                    String schemaNamespace3 = schemaNamespace;
                    String schemaNamespace4 = (String) ins.elementAt(j);
                    SchemaGrammar isg = xSGrammarBucket.getGrammar(schemaNamespace4);
                    if (isg != null) {
                        ins.setElementAt(isg, count);
                        count++;
                    }
                    j++;
                    schemaNamespace = schemaNamespace3;
                }
                ins.setSize(count);
                sg.setImportedGrammars(ins);
            }
        }
        if (this.fValidateAnnotations && annotationInfo.size() > 0) {
            validateAnnotations(annotationInfo);
        }
        return this.fGrammarBucket.getGrammar(this.fRoot.fTargetNamespace);
    }

    private void validateAnnotations(ArrayList annotationInfo) {
        if (this.fAnnotationValidator == null) {
            createAnnotationValidator();
        }
        int size = annotationInfo.size();
        XMLInputSource src = new XMLInputSource(null, null, null);
        this.fGrammarBucketAdapter.refreshGrammars(this.fGrammarBucket);
        for (int i = 0; i < size; i += 2) {
            src.setSystemId((String) annotationInfo.get(i));
            for (XSAnnotationInfo annotation = (XSAnnotationInfo) annotationInfo.get(i + 1); annotation != null; annotation = annotation.next) {
                src.setCharacterStream(new StringReader(annotation.fAnnotation));
                try {
                    this.fAnnotationValidator.parse(src);
                } catch (IOException e) {
                }
            }
        }
    }

    private void createAnnotationValidator() {
        this.fAnnotationValidator = new XML11Configuration();
        this.fGrammarBucketAdapter = new XSAnnotationGrammarPool(null);
        this.fAnnotationValidator.setFeature(VALIDATION, true);
        this.fAnnotationValidator.setFeature(XMLSCHEMA_VALIDATION, true);
        this.fAnnotationValidator.setProperty("http://apache.org/xml/properties/internal/grammar-pool", this.fGrammarBucketAdapter);
        XMLErrorHandler errorHandler = this.fErrorReporter.getErrorHandler();
        this.fAnnotationValidator.setProperty(ERROR_HANDLER, errorHandler != null ? errorHandler : new DefaultErrorHandler());
        Locale locale = this.fErrorReporter.getLocale();
        this.fAnnotationValidator.setProperty("http://apache.org/xml/properties/locale", locale);
    }

    SchemaGrammar getGrammar(String tns) {
        return this.fGrammarBucket.getGrammar(tns);
    }

    protected SchemaGrammar findGrammar(XSDDescription desc, boolean ignoreConflict) {
        SchemaGrammar sg = this.fGrammarBucket.getGrammar(desc.getTargetNamespace());
        if (sg == null && this.fGrammarPool != null) {
            SchemaGrammar sg2 = (SchemaGrammar) this.fGrammarPool.retrieveGrammar(desc);
            if (sg2 != null && !this.fGrammarBucket.putGrammar(sg2, true, ignoreConflict)) {
                reportSchemaWarning("GrammarConflict", null, null);
                return null;
            }
            return sg2;
        }
        return sg;
    }

    protected XSDocumentInfo constructTrees(Element schemaRoot, String locationHint, XSDDescription desc, boolean nsCollision) throws IOException {
        SchemaGrammar sg;
        Vector dependencies;
        Element child;
        Element rootNode;
        char c;
        String callerTNS;
        String schemaHint;
        Element newSchemaRoot;
        boolean importCollision;
        String schemaHint2;
        XSDocumentInfo newSchemaInfo;
        String schemaNamespace;
        Vector dependencies2;
        if (schemaRoot == null) {
            return null;
        }
        String callerTNS2 = desc.getTargetNamespace();
        short referType = desc.getContextType();
        boolean z = true;
        try {
            XSDocumentInfo currSchemaInfo = new XSDocumentInfo(schemaRoot, this.fAttributeChecker, this.fSymbolTable);
            if (currSchemaInfo.fTargetNamespace != null && currSchemaInfo.fTargetNamespace.length() == 0) {
                reportSchemaWarning("EmptyTargetNamespace", new Object[]{locationHint}, schemaRoot);
                currSchemaInfo.fTargetNamespace = null;
            }
            if (callerTNS2 != null) {
                if (referType == 0 || referType == 1) {
                    if (currSchemaInfo.fTargetNamespace == null) {
                        currSchemaInfo.fTargetNamespace = callerTNS2;
                        currSchemaInfo.fIsChameleonSchema = true;
                    } else if (callerTNS2 != currSchemaInfo.fTargetNamespace) {
                        reportSchemaError(NS_ERROR_CODES[referType][0], new Object[]{callerTNS2, currSchemaInfo.fTargetNamespace}, schemaRoot);
                        return null;
                    }
                } else if (referType != 3 && callerTNS2 != currSchemaInfo.fTargetNamespace) {
                    reportSchemaError(NS_ERROR_CODES[referType][0], new Object[]{callerTNS2, currSchemaInfo.fTargetNamespace}, schemaRoot);
                    return null;
                }
            } else if (currSchemaInfo.fTargetNamespace != null) {
                if (referType == 3) {
                    desc.setTargetNamespace(currSchemaInfo.fTargetNamespace);
                    callerTNS2 = currSchemaInfo.fTargetNamespace;
                } else {
                    reportSchemaError(NS_ERROR_CODES[referType][1], new Object[]{callerTNS2, currSchemaInfo.fTargetNamespace}, schemaRoot);
                    return null;
                }
            }
            currSchemaInfo.addAllowedNS(currSchemaInfo.fTargetNamespace);
            if (nsCollision) {
                SchemaGrammar sg2 = this.fGrammarBucket.getGrammar(currSchemaInfo.fTargetNamespace);
                if (sg2.isImmutable()) {
                    sg = new SchemaGrammar(sg2);
                    this.fGrammarBucket.putGrammar(sg);
                    updateImportListWith(sg);
                } else {
                    sg = sg2;
                }
                updateImportListFor(sg);
            } else if (referType == 0 || referType == 1) {
                sg = this.fGrammarBucket.getGrammar(currSchemaInfo.fTargetNamespace);
            } else if (this.fHonourAllSchemaLocations && referType == 2) {
                sg = findGrammar(desc, false);
                if (sg == null) {
                    sg = new SchemaGrammar(currSchemaInfo.fTargetNamespace, desc.makeClone(), this.fSymbolTable);
                    this.fGrammarBucket.putGrammar(sg);
                }
            } else {
                sg = new SchemaGrammar(currSchemaInfo.fTargetNamespace, desc.makeClone(), this.fSymbolTable);
                this.fGrammarBucket.putGrammar(sg);
            }
            SchemaGrammar sg3 = sg;
            sg3.addDocument(null, (String) this.fDoc2SystemId.get(currSchemaInfo.fSchemaElement));
            this.fDoc2XSDocumentMap.put(schemaRoot, currSchemaInfo);
            Vector dependencies3 = new Vector();
            Element rootNode2 = schemaRoot;
            Element child2 = DOMUtil.getFirstChildElement(rootNode2);
            Element newSchemaRoot2 = null;
            while (true) {
                if (child2 == null) {
                    dependencies = dependencies3;
                    break;
                }
                String localName = DOMUtil.getLocalName(child2);
                boolean importCollision2 = false;
                if (localName.equals(SchemaSymbols.ELT_ANNOTATION)) {
                    child = child2;
                    rootNode = rootNode2;
                    dependencies = dependencies3;
                } else {
                    if (localName.equals(SchemaSymbols.ELT_IMPORT)) {
                        Object[] importAttrs = this.fAttributeChecker.checkAttributes(child2, z, currSchemaInfo);
                        schemaHint2 = (String) importAttrs[XSAttributeChecker.ATTIDX_SCHEMALOCATION];
                        String schemaNamespace2 = (String) importAttrs[XSAttributeChecker.ATTIDX_NAMESPACE];
                        if (schemaNamespace2 != null) {
                            schemaNamespace2 = this.fSymbolTable.addSymbol(schemaNamespace2);
                        }
                        String schemaNamespace3 = schemaNamespace2;
                        Element importChild = DOMUtil.getFirstChildElement(child2);
                        if (importChild != null) {
                            String importComponentType = DOMUtil.getLocalName(importChild);
                            Element element = rootNode2;
                            if (importComponentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                                dependencies2 = dependencies3;
                                sg3.addAnnotation(this.fElementTraverser.traverseAnnotationDecl(importChild, importAttrs, true, currSchemaInfo));
                            } else {
                                dependencies2 = dependencies3;
                                reportSchemaError("s4s-elt-must-match.1", new Object[]{localName, "annotation?", importComponentType}, child2);
                            }
                            if (DOMUtil.getNextSiblingElement(importChild) != null) {
                                reportSchemaError("s4s-elt-must-match.1", new Object[]{localName, "annotation?", DOMUtil.getLocalName(DOMUtil.getNextSiblingElement(importChild))}, child2);
                            }
                            child = child2;
                            rootNode = element;
                            dependencies = dependencies2;
                        } else {
                            Element rootNode3 = rootNode2;
                            Vector dependencies4 = dependencies3;
                            String text = DOMUtil.getSyntheticAnnotation(child2);
                            if (text != null) {
                                Element child3 = child2;
                                rootNode = rootNode3;
                                child = child3;
                                dependencies = dependencies4;
                                sg3.addAnnotation(this.fElementTraverser.traverseSyntheticAnnotation(child3, text, importAttrs, true, currSchemaInfo));
                            } else {
                                child = child2;
                                rootNode = rootNode3;
                                dependencies = dependencies4;
                            }
                        }
                        this.fAttributeChecker.returnAttrArray(importAttrs, currSchemaInfo);
                        if (schemaNamespace3 == currSchemaInfo.fTargetNamespace) {
                            reportSchemaError(schemaNamespace3 != null ? "src-import.1.1" : "src-import.1.2", new Object[]{schemaNamespace3}, child);
                        } else {
                            if (currSchemaInfo.isAllowedNS(schemaNamespace3)) {
                                if (this.fHonourAllSchemaLocations || this.fNamespaceGrowth) {
                                }
                            } else {
                                currSchemaInfo.addAllowedNS(schemaNamespace3);
                            }
                            String tns = null2EmptyString(currSchemaInfo.fTargetNamespace);
                            Vector ins = (Vector) this.fImportMap.get(tns);
                            if (ins == null) {
                                this.fAllTNSs.addElement(tns);
                                ins = new Vector();
                                this.fImportMap.put(tns, ins);
                                ins.addElement(schemaNamespace3);
                            } else if (!ins.contains(schemaNamespace3)) {
                                ins.addElement(schemaNamespace3);
                            }
                            this.fSchemaGrammarDescription.reset();
                            this.fSchemaGrammarDescription.setContextType((short) 2);
                            this.fSchemaGrammarDescription.setBaseSystemId(doc2SystemId(schemaRoot));
                            this.fSchemaGrammarDescription.setLiteralSystemId(schemaHint2);
                            this.fSchemaGrammarDescription.setLocationHints(new String[]{schemaHint2});
                            this.fSchemaGrammarDescription.setTargetNamespace(schemaNamespace3);
                            SchemaGrammar isg = findGrammar(this.fSchemaGrammarDescription, this.fNamespaceGrowth);
                            if (isg != null) {
                                if (this.fNamespaceGrowth) {
                                    try {
                                        schemaNamespace = schemaNamespace3;
                                        try {
                                            if (!isg.getDocumentLocations().contains(XMLEntityManager.expandSystemId(schemaHint2, this.fSchemaGrammarDescription.getBaseSystemId(), false))) {
                                                importCollision2 = true;
                                            }
                                        } catch (URI.MalformedURIException e) {
                                        }
                                    } catch (URI.MalformedURIException e2) {
                                        schemaNamespace = schemaNamespace3;
                                    }
                                } else {
                                    schemaNamespace = schemaNamespace3;
                                    if (this.fHonourAllSchemaLocations) {
                                    }
                                }
                                child2 = DOMUtil.getNextSiblingElement(child);
                                dependencies3 = dependencies;
                                rootNode2 = rootNode;
                                callerTNS2 = callerTNS;
                                z = true;
                            } else {
                                schemaNamespace = schemaNamespace3;
                            }
                            Element newSchemaRoot3 = resolveSchema(this.fSchemaGrammarDescription, false, child, isg == null);
                            callerTNS = callerTNS2;
                            newSchemaRoot = newSchemaRoot3;
                            importCollision = importCollision2;
                            c = 3;
                        }
                    } else {
                        child = child2;
                        rootNode = rootNode2;
                        dependencies = dependencies3;
                        if (!localName.equals(SchemaSymbols.ELT_INCLUDE) && !localName.equals(SchemaSymbols.ELT_REDEFINE)) {
                            break;
                        }
                        Object[] includeAttrs = this.fAttributeChecker.checkAttributes(child, true, currSchemaInfo);
                        String schemaHint3 = (String) includeAttrs[XSAttributeChecker.ATTIDX_SCHEMALOCATION];
                        if (localName.equals(SchemaSymbols.ELT_REDEFINE)) {
                            this.fRedefine2NSSupport.put(child, new SchemaNamespaceSupport(currSchemaInfo.fNamespaceSupport));
                        }
                        if (localName.equals(SchemaSymbols.ELT_INCLUDE)) {
                            Element includeChild = DOMUtil.getFirstChildElement(child);
                            if (includeChild != null) {
                                String includeComponentType = DOMUtil.getLocalName(includeChild);
                                if (includeComponentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                                    sg3.addAnnotation(this.fElementTraverser.traverseAnnotationDecl(includeChild, includeAttrs, true, currSchemaInfo));
                                } else {
                                    reportSchemaError("s4s-elt-must-match.1", new Object[]{localName, "annotation?", includeComponentType}, child);
                                }
                                if (DOMUtil.getNextSiblingElement(includeChild) != null) {
                                    reportSchemaError("s4s-elt-must-match.1", new Object[]{localName, "annotation?", DOMUtil.getLocalName(DOMUtil.getNextSiblingElement(includeChild))}, child);
                                }
                            } else {
                                String text2 = DOMUtil.getSyntheticAnnotation(child);
                                if (text2 != null) {
                                    c = 3;
                                    sg3.addAnnotation(this.fElementTraverser.traverseSyntheticAnnotation(child, text2, includeAttrs, true, currSchemaInfo));
                                }
                            }
                            c = 3;
                        } else {
                            c = 3;
                            Element redefinedChild = DOMUtil.getFirstChildElement(child);
                            while (redefinedChild != null) {
                                String callerTNS3 = callerTNS2;
                                String schemaHint4 = schemaHint3;
                                String redefinedComponentType = DOMUtil.getLocalName(redefinedChild);
                                if (redefinedComponentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                                    sg3.addAnnotation(this.fElementTraverser.traverseAnnotationDecl(redefinedChild, includeAttrs, true, currSchemaInfo));
                                    DOMUtil.setHidden(redefinedChild, this.fHiddenNodes);
                                } else {
                                    String text3 = DOMUtil.getSyntheticAnnotation(child);
                                    if (text3 != null) {
                                        sg3.addAnnotation(this.fElementTraverser.traverseSyntheticAnnotation(child, text3, includeAttrs, true, currSchemaInfo));
                                    }
                                }
                                redefinedChild = DOMUtil.getNextSiblingElement(redefinedChild);
                                callerTNS2 = callerTNS3;
                                schemaHint3 = schemaHint4;
                            }
                        }
                        this.fAttributeChecker.returnAttrArray(includeAttrs, currSchemaInfo);
                        if (schemaHint3 == null) {
                            reportSchemaError("s4s-att-must-appear", new Object[]{"<include> or <redefine>", "schemaLocation"}, child);
                        }
                        boolean mustResolve = false;
                        short refType = 0;
                        if (localName.equals(SchemaSymbols.ELT_REDEFINE)) {
                            mustResolve = nonAnnotationContent(child);
                            refType = 1;
                        }
                        boolean mustResolve2 = mustResolve;
                        this.fSchemaGrammarDescription.reset();
                        this.fSchemaGrammarDescription.setContextType(refType);
                        this.fSchemaGrammarDescription.setBaseSystemId(doc2SystemId(schemaRoot));
                        this.fSchemaGrammarDescription.setLocationHints(new String[]{schemaHint3});
                        this.fSchemaGrammarDescription.setTargetNamespace(callerTNS2);
                        boolean alreadyTraversed = false;
                        callerTNS = callerTNS2;
                        XMLInputSource schemaSource = resolveSchemaSource(this.fSchemaGrammarDescription, mustResolve2, child, true);
                        if (this.fNamespaceGrowth && refType == 0) {
                            try {
                                schemaHint = schemaHint3;
                                try {
                                    String schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                                    alreadyTraversed = sg3.getDocumentLocations().contains(schemaId);
                                } catch (URI.MalformedURIException e3) {
                                }
                            } catch (URI.MalformedURIException e4) {
                                schemaHint = schemaHint3;
                            }
                        } else {
                            schemaHint = schemaHint3;
                        }
                        if (!alreadyTraversed) {
                            newSchemaRoot2 = resolveSchema(schemaSource, this.fSchemaGrammarDescription, mustResolve2, child);
                            String str = currSchemaInfo.fTargetNamespace;
                        } else {
                            this.fLastSchemaWasDuplicate = true;
                        }
                        newSchemaRoot = newSchemaRoot2;
                        importCollision = false;
                        schemaHint2 = schemaHint;
                    }
                    if (this.fLastSchemaWasDuplicate) {
                        newSchemaInfo = newSchemaRoot == null ? null : (XSDocumentInfo) this.fDoc2XSDocumentMap.get(newSchemaRoot);
                    } else {
                        newSchemaInfo = constructTrees(newSchemaRoot, schemaHint2, this.fSchemaGrammarDescription, importCollision);
                    }
                    if (localName.equals(SchemaSymbols.ELT_REDEFINE) && newSchemaInfo != null) {
                        this.fRedefine2XSDMap.put(child, newSchemaInfo);
                    }
                    if (newSchemaRoot != null) {
                        if (newSchemaInfo != null) {
                            dependencies.addElement(newSchemaInfo);
                        }
                        newSchemaRoot = null;
                    }
                    newSchemaRoot2 = newSchemaRoot;
                    child2 = DOMUtil.getNextSiblingElement(child);
                    dependencies3 = dependencies;
                    rootNode2 = rootNode;
                    callerTNS2 = callerTNS;
                    z = true;
                }
                callerTNS = callerTNS2;
                c = 3;
                child2 = DOMUtil.getNextSiblingElement(child);
                dependencies3 = dependencies;
                rootNode2 = rootNode;
                callerTNS2 = callerTNS;
                z = true;
            }
            this.fDependencyMap.put(currSchemaInfo, dependencies);
            return currSchemaInfo;
        } catch (XMLSchemaException e5) {
            reportSchemaError(ELE_ERROR_CODES[referType], new Object[]{locationHint}, schemaRoot);
            return null;
        }
    }

    private boolean isExistingGrammar(XSDDescription desc, boolean ignoreConflict) {
        SchemaGrammar sg = this.fGrammarBucket.getGrammar(desc.getTargetNamespace());
        if (sg == null) {
            return findGrammar(desc, ignoreConflict) != null;
        }
        if (sg.isImmutable()) {
            return true;
        }
        try {
            return sg.getDocumentLocations().contains(XMLEntityManager.expandSystemId(desc.getLiteralSystemId(), desc.getBaseSystemId(), false));
        } catch (URI.MalformedURIException e) {
            return false;
        }
    }

    private void updateImportListFor(SchemaGrammar grammar) {
        Vector importedGrammars = grammar.getImportedGrammars();
        if (importedGrammars != null) {
            for (int i = 0; i < importedGrammars.size(); i++) {
                SchemaGrammar isg1 = (SchemaGrammar) importedGrammars.elementAt(i);
                SchemaGrammar isg2 = this.fGrammarBucket.getGrammar(isg1.getTargetNamespace());
                if (isg2 != null && isg1 != isg2) {
                    importedGrammars.set(i, isg2);
                }
            }
        }
    }

    private void updateImportListWith(SchemaGrammar newGrammar) {
        Vector importedGrammars;
        SchemaGrammar[] schemaGrammars = this.fGrammarBucket.getGrammars();
        for (SchemaGrammar sg : schemaGrammars) {
            if (sg != newGrammar && (importedGrammars = sg.getImportedGrammars()) != null) {
                int j = 0;
                while (true) {
                    if (j >= importedGrammars.size()) {
                        break;
                    }
                    SchemaGrammar isg = (SchemaGrammar) importedGrammars.elementAt(j);
                    if (!null2EmptyString(isg.getTargetNamespace()).equals(null2EmptyString(newGrammar.getTargetNamespace()))) {
                        j++;
                    } else if (isg != newGrammar) {
                        importedGrammars.set(j, newGrammar);
                    }
                }
            }
        }
    }

    protected void buildGlobalNameRegistries() {
        Stack schemasToProcess;
        Stack schemasToProcess2;
        Element redefineComp;
        Stack schemasToProcess3 = new Stack();
        schemasToProcess3.push(this.fRoot);
        while (!schemasToProcess3.empty()) {
            XSDocumentInfo currSchemaDoc = (XSDocumentInfo) schemasToProcess3.pop();
            Element currDoc = currSchemaDoc.fSchemaElement;
            if (!DOMUtil.isHidden(currDoc, this.fHiddenNodes)) {
                Element globalComp = DOMUtil.getFirstChildElement(currDoc);
                boolean dependenciesCanOccur = true;
                Element globalComp2 = globalComp;
                while (globalComp2 != null) {
                    if (DOMUtil.getLocalName(globalComp2).equals(SchemaSymbols.ELT_ANNOTATION)) {
                        schemasToProcess = schemasToProcess3;
                    } else if (DOMUtil.getLocalName(globalComp2).equals(SchemaSymbols.ELT_INCLUDE) || DOMUtil.getLocalName(globalComp2).equals(SchemaSymbols.ELT_IMPORT)) {
                        schemasToProcess = schemasToProcess3;
                        if (!dependenciesCanOccur) {
                            reportSchemaError("s4s-elt-invalid-content.3", new Object[]{DOMUtil.getLocalName(globalComp2)}, globalComp2);
                        }
                        DOMUtil.setHidden(globalComp2, this.fHiddenNodes);
                    } else if (DOMUtil.getLocalName(globalComp2).equals(SchemaSymbols.ELT_REDEFINE)) {
                        if (!dependenciesCanOccur) {
                            reportSchemaError("s4s-elt-invalid-content.3", new Object[]{DOMUtil.getLocalName(globalComp2)}, globalComp2);
                        }
                        Element redefineComp2 = DOMUtil.getFirstChildElement(globalComp2);
                        Element redefineComp3 = redefineComp2;
                        while (redefineComp3 != null) {
                            String lName = DOMUtil.getAttrValue(redefineComp3, SchemaSymbols.ATT_NAME);
                            if (lName.length() == 0) {
                                redefineComp = redefineComp3;
                                schemasToProcess2 = schemasToProcess3;
                            } else {
                                String qName = currSchemaDoc.fTargetNamespace == null ? "," + lName : String.valueOf(currSchemaDoc.fTargetNamespace) + "," + lName;
                                String componentType = DOMUtil.getLocalName(redefineComp3);
                                if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                                    schemasToProcess2 = schemasToProcess3;
                                    checkForDuplicateNames(qName, 2, this.fUnparsedAttributeGroupRegistry, this.fUnparsedAttributeGroupRegistrySub, redefineComp3, currSchemaDoc);
                                    renameRedefiningComponents(currSchemaDoc, redefineComp3, SchemaSymbols.ELT_ATTRIBUTEGROUP, lName, String.valueOf(DOMUtil.getAttrValue(redefineComp3, SchemaSymbols.ATT_NAME)) + REDEF_IDENTIFIER);
                                    redefineComp = redefineComp3;
                                } else {
                                    schemasToProcess2 = schemasToProcess3;
                                    if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE) || componentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                                        redefineComp = redefineComp3;
                                        checkForDuplicateNames(qName, 7, this.fUnparsedTypeRegistry, this.fUnparsedTypeRegistrySub, redefineComp, currSchemaDoc);
                                        String targetLName = String.valueOf(DOMUtil.getAttrValue(redefineComp, SchemaSymbols.ATT_NAME)) + REDEF_IDENTIFIER;
                                        if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                                            renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_COMPLEXTYPE, lName, targetLName);
                                        } else {
                                            renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_SIMPLETYPE, lName, targetLName);
                                        }
                                    } else if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
                                        redefineComp = redefineComp3;
                                        checkForDuplicateNames(qName, 4, this.fUnparsedGroupRegistry, this.fUnparsedGroupRegistrySub, redefineComp3, currSchemaDoc);
                                        renameRedefiningComponents(currSchemaDoc, redefineComp, SchemaSymbols.ELT_GROUP, lName, String.valueOf(DOMUtil.getAttrValue(redefineComp, SchemaSymbols.ATT_NAME)) + REDEF_IDENTIFIER);
                                    } else {
                                        redefineComp = redefineComp3;
                                    }
                                }
                            }
                            redefineComp3 = DOMUtil.getNextSiblingElement(redefineComp);
                            schemasToProcess3 = schemasToProcess2;
                        }
                        schemasToProcess = schemasToProcess3;
                    } else {
                        schemasToProcess = schemasToProcess3;
                        String lName2 = DOMUtil.getAttrValue(globalComp2, SchemaSymbols.ATT_NAME);
                        if (lName2.length() != 0) {
                            String qName2 = currSchemaDoc.fTargetNamespace == null ? "," + lName2 : String.valueOf(currSchemaDoc.fTargetNamespace) + "," + lName2;
                            String componentType2 = DOMUtil.getLocalName(globalComp2);
                            if (componentType2.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                                checkForDuplicateNames(qName2, 1, this.fUnparsedAttributeRegistry, this.fUnparsedAttributeRegistrySub, globalComp2, currSchemaDoc);
                            } else if (componentType2.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                                checkForDuplicateNames(qName2, 2, this.fUnparsedAttributeGroupRegistry, this.fUnparsedAttributeGroupRegistrySub, globalComp2, currSchemaDoc);
                            } else if (componentType2.equals(SchemaSymbols.ELT_COMPLEXTYPE) || componentType2.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                                checkForDuplicateNames(qName2, 7, this.fUnparsedTypeRegistry, this.fUnparsedTypeRegistrySub, globalComp2, currSchemaDoc);
                            } else if (componentType2.equals(SchemaSymbols.ELT_ELEMENT)) {
                                checkForDuplicateNames(qName2, 3, this.fUnparsedElementRegistry, this.fUnparsedElementRegistrySub, globalComp2, currSchemaDoc);
                            } else if (componentType2.equals(SchemaSymbols.ELT_GROUP)) {
                                checkForDuplicateNames(qName2, 4, this.fUnparsedGroupRegistry, this.fUnparsedGroupRegistrySub, globalComp2, currSchemaDoc);
                            } else if (componentType2.equals(SchemaSymbols.ELT_NOTATION)) {
                                checkForDuplicateNames(qName2, 6, this.fUnparsedNotationRegistry, this.fUnparsedNotationRegistrySub, globalComp2, currSchemaDoc);
                            }
                        }
                        dependenciesCanOccur = false;
                    }
                    globalComp2 = DOMUtil.getNextSiblingElement(globalComp2);
                    schemasToProcess3 = schemasToProcess;
                }
                DOMUtil.setHidden(currDoc, this.fHiddenNodes);
                Vector currSchemaDepends = (Vector) this.fDependencyMap.get(currSchemaDoc);
                for (int i = 0; i < currSchemaDepends.size(); i++) {
                    schemasToProcess3.push(currSchemaDepends.elementAt(i));
                }
            }
        }
    }

    protected void traverseSchemas(ArrayList annotationInfo) {
        XSAnnotationInfo info;
        String text;
        setSchemasVisible(this.fRoot);
        Stack schemasToProcess = new Stack();
        schemasToProcess.push(this.fRoot);
        while (!schemasToProcess.empty()) {
            XSDocumentInfo currSchemaDoc = (XSDocumentInfo) schemasToProcess.pop();
            Element currDoc = currSchemaDoc.fSchemaElement;
            SchemaGrammar currSG = this.fGrammarBucket.getGrammar(currSchemaDoc.fTargetNamespace);
            if (!DOMUtil.isHidden(currDoc, this.fHiddenNodes)) {
                Element globalComp = DOMUtil.getFirstVisibleChildElement(currDoc, this.fHiddenNodes);
                boolean sawAnnotation = false;
                while (globalComp != null) {
                    DOMUtil.setHidden(globalComp, this.fHiddenNodes);
                    String componentType = DOMUtil.getLocalName(globalComp);
                    if (DOMUtil.getLocalName(globalComp).equals(SchemaSymbols.ELT_REDEFINE)) {
                        currSchemaDoc.backupNSSupport((SchemaNamespaceSupport) this.fRedefine2NSSupport.get(globalComp));
                        Element redefinedComp = DOMUtil.getFirstVisibleChildElement(globalComp, this.fHiddenNodes);
                        while (redefinedComp != null) {
                            String redefinedComponentType = DOMUtil.getLocalName(redefinedComp);
                            DOMUtil.setHidden(redefinedComp, this.fHiddenNodes);
                            if (redefinedComponentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                                this.fAttributeGroupTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                            } else if (redefinedComponentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                                this.fComplexTypeTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                            } else if (redefinedComponentType.equals(SchemaSymbols.ELT_GROUP)) {
                                this.fGroupTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                            } else if (redefinedComponentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                                this.fSimpleTypeTraverser.traverseGlobal(redefinedComp, currSchemaDoc, currSG);
                            } else {
                                reportSchemaError("s4s-elt-must-match.1", new Object[]{DOMUtil.getLocalName(globalComp), "(annotation | (simpleType | complexType | group | attributeGroup))*", redefinedComponentType}, redefinedComp);
                            }
                            redefinedComp = DOMUtil.getNextVisibleSiblingElement(redefinedComp, this.fHiddenNodes);
                        }
                        currSchemaDoc.restoreNSSupport();
                    } else if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                        this.fAttributeTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                        this.fAttributeGroupTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                        this.fComplexTypeTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_ELEMENT)) {
                        this.fElementTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
                        this.fGroupTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_NOTATION)) {
                        this.fNotationTraverser.traverse(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                        this.fSimpleTypeTraverser.traverseGlobal(globalComp, currSchemaDoc, currSG);
                    } else if (componentType.equals(SchemaSymbols.ELT_ANNOTATION)) {
                        currSG.addAnnotation(this.fElementTraverser.traverseAnnotationDecl(globalComp, currSchemaDoc.getSchemaAttrs(), true, currSchemaDoc));
                        sawAnnotation = true;
                    } else {
                        reportSchemaError("s4s-elt-invalid-content.1", new Object[]{SchemaSymbols.ELT_SCHEMA, DOMUtil.getLocalName(globalComp)}, globalComp);
                    }
                    globalComp = DOMUtil.getNextVisibleSiblingElement(globalComp, this.fHiddenNodes);
                }
                if (!sawAnnotation && (text = DOMUtil.getSyntheticAnnotation(currDoc)) != null) {
                    currSG.addAnnotation(this.fElementTraverser.traverseSyntheticAnnotation(currDoc, text, currSchemaDoc.getSchemaAttrs(), true, currSchemaDoc));
                }
                if (annotationInfo != null && (info = currSchemaDoc.getAnnotations()) != null) {
                    annotationInfo.add(doc2SystemId(currDoc));
                    annotationInfo.add(info);
                }
                currSchemaDoc.returnSchemaAttrs();
                DOMUtil.setHidden(currDoc, this.fHiddenNodes);
                Vector currSchemaDepends = (Vector) this.fDependencyMap.get(currSchemaDoc);
                for (int i = 0; i < currSchemaDepends.size(); i++) {
                    schemasToProcess.push(currSchemaDepends.elementAt(i));
                }
            }
        }
    }

    private final boolean needReportTNSError(String uri) {
        if (this.fReportedTNS == null) {
            this.fReportedTNS = new Vector();
        } else if (this.fReportedTNS.contains(uri)) {
            return false;
        }
        this.fReportedTNS.addElement(uri);
        return true;
    }

    void addGlobalAttributeDecl(XSAttributeDecl decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getName());
        String declKey = sb.toString();
        if (this.fGlobalAttrDecls.get(declKey) == null) {
            this.fGlobalAttrDecls.put(declKey, decl);
        }
    }

    void addGlobalAttributeGroupDecl(XSAttributeGroupDecl decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getName());
        String declKey = sb.toString();
        if (this.fGlobalAttrGrpDecls.get(declKey) == null) {
            this.fGlobalAttrGrpDecls.put(declKey, decl);
        }
    }

    void addGlobalElementDecl(XSElementDecl decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getName());
        String declKey = sb.toString();
        if (this.fGlobalElemDecls.get(declKey) == null) {
            this.fGlobalElemDecls.put(declKey, decl);
        }
    }

    void addGlobalGroupDecl(XSGroupDecl decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getName());
        String declKey = sb.toString();
        if (this.fGlobalGroupDecls.get(declKey) == null) {
            this.fGlobalGroupDecls.put(declKey, decl);
        }
    }

    void addGlobalNotationDecl(XSNotationDecl decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getName());
        String declKey = sb.toString();
        if (this.fGlobalNotationDecls.get(declKey) == null) {
            this.fGlobalNotationDecls.put(declKey, decl);
        }
    }

    void addGlobalTypeDecl(XSTypeDefinition decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getName());
        String declKey = sb.toString();
        if (this.fGlobalTypeDecls.get(declKey) == null) {
            this.fGlobalTypeDecls.put(declKey, decl);
        }
    }

    void addIDConstraintDecl(IdentityConstraint decl) {
        StringBuilder sb;
        String namespace = decl.getNamespace();
        if (namespace == null || namespace.length() == 0) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(decl.getIdentityConstraintName());
        String declKey = sb.toString();
        if (this.fGlobalIDConstraintDecls.get(declKey) == null) {
            this.fGlobalIDConstraintDecls.put(declKey, decl);
        }
    }

    private XSAttributeDecl getGlobalAttributeDecl(String declKey) {
        return (XSAttributeDecl) this.fGlobalAttrDecls.get(declKey);
    }

    private XSAttributeGroupDecl getGlobalAttributeGroupDecl(String declKey) {
        return (XSAttributeGroupDecl) this.fGlobalAttrGrpDecls.get(declKey);
    }

    private XSElementDecl getGlobalElementDecl(String declKey) {
        return (XSElementDecl) this.fGlobalElemDecls.get(declKey);
    }

    private XSGroupDecl getGlobalGroupDecl(String declKey) {
        return (XSGroupDecl) this.fGlobalGroupDecls.get(declKey);
    }

    private XSNotationDecl getGlobalNotationDecl(String declKey) {
        return (XSNotationDecl) this.fGlobalNotationDecls.get(declKey);
    }

    private XSTypeDefinition getGlobalTypeDecl(String declKey) {
        return (XSTypeDefinition) this.fGlobalTypeDecls.get(declKey);
    }

    private IdentityConstraint getIDConstraintDecl(String declKey) {
        return (IdentityConstraint) this.fGlobalIDConstraintDecls.get(declKey);
    }

    protected Object getGlobalDecl(XSDocumentInfo currSchema, int declType, QName declToTraverse, Element elmNode) {
        String declKey;
        Object retObj;
        if (declToTraverse.uri != null && declToTraverse.uri == SchemaSymbols.URI_SCHEMAFORSCHEMA && declType == 7 && (retObj = SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(declToTraverse.localpart)) != null) {
            return retObj;
        }
        if (!currSchema.isAllowedNS(declToTraverse.uri) && currSchema.needReportTNSError(declToTraverse.uri)) {
            reportSchemaError(declToTraverse.uri == null ? "src-resolve.4.1" : "src-resolve.4.2", new Object[]{this.fDoc2SystemId.get(currSchema.fSchemaElement), declToTraverse.uri, declToTraverse.rawname}, elmNode);
        }
        SchemaGrammar sGrammar = this.fGrammarBucket.getGrammar(declToTraverse.uri);
        if (sGrammar == null) {
            if (needReportTNSError(declToTraverse.uri)) {
                reportSchemaError("src-resolve", new Object[]{declToTraverse.rawname, COMP_TYPE[declType]}, elmNode);
                return null;
            }
            return null;
        }
        Object retObj2 = getGlobalDeclFromGrammar(sGrammar, declType, declToTraverse.localpart);
        if (declToTraverse.uri == null) {
            declKey = "," + declToTraverse.localpart;
        } else {
            declKey = String.valueOf(declToTraverse.uri) + "," + declToTraverse.localpart;
        }
        if (!this.fTolerateDuplicates) {
            if (retObj2 != null) {
                return retObj2;
            }
        } else {
            Object retObj22 = getGlobalDecl(declKey, declType);
            if (retObj22 != null) {
                return retObj22;
            }
        }
        Element decl = null;
        XSDocumentInfo declDoc = null;
        switch (declType) {
            case 1:
                decl = (Element) this.fUnparsedAttributeRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedAttributeRegistrySub.get(declKey);
                break;
            case 2:
                decl = (Element) this.fUnparsedAttributeGroupRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedAttributeGroupRegistrySub.get(declKey);
                break;
            case 3:
                decl = (Element) this.fUnparsedElementRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedElementRegistrySub.get(declKey);
                break;
            case 4:
                decl = (Element) this.fUnparsedGroupRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedGroupRegistrySub.get(declKey);
                break;
            case 5:
                decl = (Element) this.fUnparsedIdentityConstraintRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedIdentityConstraintRegistrySub.get(declKey);
                break;
            case 6:
                decl = (Element) this.fUnparsedNotationRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedNotationRegistrySub.get(declKey);
                break;
            case 7:
                decl = (Element) this.fUnparsedTypeRegistry.get(declKey);
                declDoc = (XSDocumentInfo) this.fUnparsedTypeRegistrySub.get(declKey);
                break;
            default:
                reportSchemaError("Internal-Error", new Object[]{"XSDHandler asked to locate component of type " + declType + "; it does not recognize this type!"}, elmNode);
                break;
        }
        if (decl == null) {
            if (retObj2 == null) {
                reportSchemaError("src-resolve", new Object[]{declToTraverse.rawname, COMP_TYPE[declType]}, elmNode);
            }
            return retObj2;
        }
        XSDocumentInfo schemaWithDecl = findXSDocumentForDecl(currSchema, decl, declDoc);
        if (schemaWithDecl == null) {
            if (retObj2 == null) {
                reportSchemaError(declToTraverse.uri == null ? "src-resolve.4.1" : "src-resolve.4.2", new Object[]{this.fDoc2SystemId.get(currSchema.fSchemaElement), declToTraverse.uri, declToTraverse.rawname}, elmNode);
            }
            return retObj2;
        }
        if (DOMUtil.isHidden(decl, this.fHiddenNodes)) {
            if (retObj2 == null) {
                String code = CIRCULAR_CODES[declType];
                if (declType == 7 && SchemaSymbols.ELT_COMPLEXTYPE.equals(DOMUtil.getLocalName(decl))) {
                    code = "ct-props-correct.3";
                }
                reportSchemaError(code, new Object[]{String.valueOf(declToTraverse.prefix) + ":" + declToTraverse.localpart}, elmNode);
            }
            return retObj2;
        }
        return traverseGlobalDecl(declType, decl, schemaWithDecl, sGrammar);
    }

    protected Object getGlobalDecl(String declKey, int declType) {
        switch (declType) {
            case 1:
                Object retObj = getGlobalAttributeDecl(declKey);
                return retObj;
            case 2:
                Object retObj2 = getGlobalAttributeGroupDecl(declKey);
                return retObj2;
            case 3:
                Object retObj3 = getGlobalElementDecl(declKey);
                return retObj3;
            case 4:
                Object retObj4 = getGlobalGroupDecl(declKey);
                return retObj4;
            case 5:
                Object retObj5 = getIDConstraintDecl(declKey);
                return retObj5;
            case 6:
                Object retObj6 = getGlobalNotationDecl(declKey);
                return retObj6;
            case 7:
                Object retObj7 = getGlobalTypeDecl(declKey);
                return retObj7;
            default:
                return null;
        }
    }

    protected Object getGlobalDeclFromGrammar(SchemaGrammar sGrammar, int declType, String localpart) {
        switch (declType) {
            case 1:
                Object retObj = sGrammar.getGlobalAttributeDecl(localpart);
                return retObj;
            case 2:
                Object retObj2 = sGrammar.getGlobalAttributeGroupDecl(localpart);
                return retObj2;
            case 3:
                Object retObj3 = sGrammar.getGlobalElementDecl(localpart);
                return retObj3;
            case 4:
                Object retObj4 = sGrammar.getGlobalGroupDecl(localpart);
                return retObj4;
            case 5:
                Object retObj5 = sGrammar.getIDConstraintDecl(localpart);
                return retObj5;
            case 6:
                Object retObj6 = sGrammar.getGlobalNotationDecl(localpart);
                return retObj6;
            case 7:
                Object retObj7 = sGrammar.getGlobalTypeDecl(localpart);
                return retObj7;
            default:
                return null;
        }
    }

    protected Object getGlobalDeclFromGrammar(SchemaGrammar sGrammar, int declType, String localpart, String schemaLoc) {
        switch (declType) {
            case 1:
                Object retObj = sGrammar.getGlobalAttributeDecl(localpart, schemaLoc);
                return retObj;
            case 2:
                Object retObj2 = sGrammar.getGlobalAttributeGroupDecl(localpart, schemaLoc);
                return retObj2;
            case 3:
                Object retObj3 = sGrammar.getGlobalElementDecl(localpart, schemaLoc);
                return retObj3;
            case 4:
                Object retObj4 = sGrammar.getGlobalGroupDecl(localpart, schemaLoc);
                return retObj4;
            case 5:
                Object retObj5 = sGrammar.getIDConstraintDecl(localpart, schemaLoc);
                return retObj5;
            case 6:
                Object retObj6 = sGrammar.getGlobalNotationDecl(localpart, schemaLoc);
                return retObj6;
            case 7:
                Object retObj7 = sGrammar.getGlobalTypeDecl(localpart, schemaLoc);
                return retObj7;
            default:
                return null;
        }
    }

    protected Object traverseGlobalDecl(int declType, Element decl, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object retObj = null;
        DOMUtil.setHidden(decl, this.fHiddenNodes);
        SchemaNamespaceSupport nsSupport = null;
        Element parent = DOMUtil.getParent(decl);
        if (DOMUtil.getLocalName(parent).equals(SchemaSymbols.ELT_REDEFINE)) {
            nsSupport = (SchemaNamespaceSupport) this.fRedefine2NSSupport.get(parent);
        }
        schemaDoc.backupNSSupport(nsSupport);
        switch (declType) {
            case 1:
                retObj = this.fAttributeTraverser.traverseGlobal(decl, schemaDoc, grammar);
                break;
            case 2:
                retObj = this.fAttributeGroupTraverser.traverseGlobal(decl, schemaDoc, grammar);
                break;
            case 3:
                retObj = this.fElementTraverser.traverseGlobal(decl, schemaDoc, grammar);
                break;
            case 4:
                retObj = this.fGroupTraverser.traverseGlobal(decl, schemaDoc, grammar);
                break;
            case 6:
                retObj = this.fNotationTraverser.traverse(decl, schemaDoc, grammar);
                break;
            case 7:
                if (DOMUtil.getLocalName(decl).equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                    retObj = this.fComplexTypeTraverser.traverseGlobal(decl, schemaDoc, grammar);
                } else {
                    retObj = this.fSimpleTypeTraverser.traverseGlobal(decl, schemaDoc, grammar);
                }
                break;
        }
        schemaDoc.restoreNSSupport();
        return retObj;
    }

    public String schemaDocument2SystemId(XSDocumentInfo schemaDoc) {
        return (String) this.fDoc2SystemId.get(schemaDoc.fSchemaElement);
    }

    Object getGrpOrAttrGrpRedefinedByRestriction(int type, QName name, XSDocumentInfo currSchema, Element elmNode) {
        String realName;
        String nameToFind;
        String strSubstring;
        if (name.uri != null) {
            realName = String.valueOf(name.uri) + "," + name.localpart;
        } else {
            realName = "," + name.localpart;
        }
        if (type != 2) {
            if (type != 4) {
                return null;
            }
            nameToFind = (String) this.fRedefinedRestrictedGroupRegistry.get(realName);
        } else {
            nameToFind = (String) this.fRedefinedRestrictedAttributeGroupRegistry.get(realName);
        }
        if (nameToFind == null) {
            return null;
        }
        int commaPos = nameToFind.indexOf(",");
        String str = XMLSymbols.EMPTY_STRING;
        String strSubstring2 = nameToFind.substring(commaPos + 1);
        String strSubstring3 = nameToFind.substring(commaPos);
        if (commaPos == 0) {
            strSubstring = null;
        } else {
            strSubstring = nameToFind.substring(0, commaPos);
        }
        QName qNameToFind = new QName(str, strSubstring2, strSubstring3, strSubstring);
        Object retObj = getGlobalDecl(currSchema, type, qNameToFind, elmNode);
        if (retObj != null) {
            return retObj;
        }
        if (type == 2) {
            reportSchemaError("src-redefine.7.2.1", new Object[]{name.localpart}, elmNode);
        } else if (type == 4) {
            reportSchemaError("src-redefine.6.2.1", new Object[]{name.localpart}, elmNode);
        }
        return null;
    }

    protected void resolveKeyRefs() {
        for (int i = 0; i < this.fKeyrefStackPos; i++) {
            XSDocumentInfo keyrefSchemaDoc = this.fKeyrefsMapXSDocumentInfo[i];
            keyrefSchemaDoc.fNamespaceSupport.makeGlobal();
            keyrefSchemaDoc.fNamespaceSupport.setEffectiveContext(this.fKeyrefNamespaceContext[i]);
            SchemaGrammar keyrefGrammar = this.fGrammarBucket.getGrammar(keyrefSchemaDoc.fTargetNamespace);
            DOMUtil.setHidden(this.fKeyrefs[i], this.fHiddenNodes);
            this.fKeyrefTraverser.traverse(this.fKeyrefs[i], this.fKeyrefElems[i], keyrefSchemaDoc, keyrefGrammar);
        }
    }

    protected Hashtable getIDRegistry() {
        return this.fUnparsedIdentityConstraintRegistry;
    }

    protected Hashtable getIDRegistry_sub() {
        return this.fUnparsedIdentityConstraintRegistrySub;
    }

    protected void storeKeyRef(Element keyrefToStore, XSDocumentInfo schemaDoc, XSElementDecl currElemDecl) {
        StringBuilder sb;
        String keyrefName = DOMUtil.getAttrValue(keyrefToStore, SchemaSymbols.ATT_NAME);
        if (keyrefName.length() != 0) {
            if (schemaDoc.fTargetNamespace == null) {
                sb = new StringBuilder(",");
            } else {
                sb = new StringBuilder(String.valueOf(schemaDoc.fTargetNamespace));
                sb.append(",");
            }
            sb.append(keyrefName);
            String keyrefQName = sb.toString();
            checkForDuplicateNames(keyrefQName, 5, this.fUnparsedIdentityConstraintRegistry, this.fUnparsedIdentityConstraintRegistrySub, keyrefToStore, schemaDoc);
        }
        if (this.fKeyrefStackPos == this.fKeyrefs.length) {
            Element[] elemArray = new Element[this.fKeyrefStackPos + 2];
            System.arraycopy(this.fKeyrefs, 0, elemArray, 0, this.fKeyrefStackPos);
            this.fKeyrefs = elemArray;
            XSElementDecl[] declArray = new XSElementDecl[this.fKeyrefStackPos + 2];
            System.arraycopy(this.fKeyrefElems, 0, declArray, 0, this.fKeyrefStackPos);
            this.fKeyrefElems = declArray;
            String[][] stringArray = new String[this.fKeyrefStackPos + 2][];
            System.arraycopy(this.fKeyrefNamespaceContext, 0, stringArray, 0, this.fKeyrefStackPos);
            this.fKeyrefNamespaceContext = stringArray;
            XSDocumentInfo[] xsDocumentInfo = new XSDocumentInfo[this.fKeyrefStackPos + 2];
            System.arraycopy(this.fKeyrefsMapXSDocumentInfo, 0, xsDocumentInfo, 0, this.fKeyrefStackPos);
            this.fKeyrefsMapXSDocumentInfo = xsDocumentInfo;
        }
        this.fKeyrefs[this.fKeyrefStackPos] = keyrefToStore;
        this.fKeyrefElems[this.fKeyrefStackPos] = currElemDecl;
        this.fKeyrefNamespaceContext[this.fKeyrefStackPos] = schemaDoc.fNamespaceSupport.getEffectiveLocalContext();
        XSDocumentInfo[] xSDocumentInfoArr = this.fKeyrefsMapXSDocumentInfo;
        int i = this.fKeyrefStackPos;
        this.fKeyrefStackPos = i + 1;
        xSDocumentInfoArr[i] = schemaDoc;
    }

    private Element resolveSchema(XSDDescription desc, boolean mustResolve, Element referElement, boolean usePairs) throws IOException {
        XMLInputSource schemaSource;
        try {
            Hashtable pairs = usePairs ? this.fLocationPairs : EMPTY_TABLE;
            schemaSource = XMLSchemaLoader.resolveDocument(desc, pairs, this.fEntityResolver);
        } catch (IOException e) {
            if (mustResolve) {
                reportSchemaError("schema_reference.4", new Object[]{desc.getLocationHints()[0]}, referElement);
            } else {
                reportSchemaWarning("schema_reference.4", new Object[]{desc.getLocationHints()[0]}, referElement);
            }
            schemaSource = null;
        }
        if (schemaSource instanceof DOMInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (DOMInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        }
        if (schemaSource instanceof SAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (SAXInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        }
        if (schemaSource instanceof StAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (StAXInputSource) schemaSource, mustResolve, desc.getContextType(), referElement);
        }
        if (schemaSource instanceof XSInputSource) {
            return getSchemaDocument(schemaSource, desc);
        }
        return getSchemaDocument(desc.getTargetNamespace(), schemaSource, mustResolve, desc.getContextType(), referElement);
    }

    private Element resolveSchema(XMLInputSource xMLInputSource, XSDDescription desc, boolean mustResolve, Element referElement) {
        if (xMLInputSource instanceof DOMInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (DOMInputSource) xMLInputSource, mustResolve, desc.getContextType(), referElement);
        }
        if (xMLInputSource instanceof SAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (SAXInputSource) xMLInputSource, mustResolve, desc.getContextType(), referElement);
        }
        if (xMLInputSource instanceof StAXInputSource) {
            return getSchemaDocument(desc.getTargetNamespace(), (StAXInputSource) xMLInputSource, mustResolve, desc.getContextType(), referElement);
        }
        if (xMLInputSource instanceof XSInputSource) {
            return getSchemaDocument(xMLInputSource, desc);
        }
        return getSchemaDocument(desc.getTargetNamespace(), xMLInputSource, mustResolve, desc.getContextType(), referElement);
    }

    private XMLInputSource resolveSchemaSource(XSDDescription desc, boolean mustResolve, Element referElement, boolean usePairs) {
        try {
            Hashtable pairs = usePairs ? this.fLocationPairs : EMPTY_TABLE;
            XMLInputSource schemaSource = XMLSchemaLoader.resolveDocument(desc, pairs, this.fEntityResolver);
            return schemaSource;
        } catch (IOException e) {
            if (mustResolve) {
                reportSchemaError("schema_reference.4", new Object[]{desc.getLocationHints()[0]}, referElement);
                return null;
            }
            reportSchemaWarning("schema_reference.4", new Object[]{desc.getLocationHints()[0]}, referElement);
            return null;
        }
    }

    private Element getSchemaDocument(String schemaNamespace, XMLInputSource schemaSource, boolean mustResolve, short referType, Element referElement) {
        IOException exception;
        boolean hasInput;
        if (schemaSource != null) {
            try {
                if (schemaSource.getSystemId() != null || schemaSource.getByteStream() != null || schemaSource.getCharacterStream() != null) {
                    XSDKey key = null;
                    String schemaId = null;
                    if (referType != 3) {
                        schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                        try {
                            key = new XSDKey(schemaId, referType, schemaNamespace);
                            Element schemaElement = (Element) this.fTraversed.get(key);
                            if (schemaElement != null) {
                                this.fLastSchemaWasDuplicate = true;
                                return schemaElement;
                            }
                        } catch (IOException e) {
                            ex = e;
                            exception = ex;
                            hasInput = true;
                            return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
                        }
                    }
                    this.fSchemaParser.parse(schemaSource);
                    Document schemaDocument = this.fSchemaParser.getDocument();
                    return getSchemaDocument0(key, schemaId, schemaDocument != null ? DOMUtil.getRoot(schemaDocument) : null);
                }
                hasInput = false;
                exception = null;
            } catch (IOException e2) {
                ex = e2;
            }
        } else {
            hasInput = false;
            exception = null;
        }
        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
    }

    private Element getSchemaDocument(String schemaNamespace, SAXInputSource schemaSource, boolean mustResolve, short referType, Element referElement) throws SAXNotRecognizedException, SAXNotSupportedException {
        IOException exception;
        boolean hasInput;
        Element schemaElement;
        XMLReader parser;
        Object securityManager;
        XMLReader parser2 = schemaSource.getXMLReader();
        InputSource inputSource = schemaSource.getInputSource();
        Element schemaElement2 = null;
        if (inputSource != null) {
            try {
                if (inputSource.getSystemId() != null || inputSource.getByteStream() != null || inputSource.getCharacterStream() != null) {
                    XSDKey key = null;
                    String schemaId = null;
                    if (referType != 3) {
                        schemaId = XMLEntityManager.expandSystemId(inputSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                        try {
                            key = new XSDKey(schemaId, referType, schemaNamespace);
                            Element element = (Element) this.fTraversed.get(key);
                            schemaElement2 = element;
                            if (element != null) {
                                this.fLastSchemaWasDuplicate = true;
                                return schemaElement2;
                            }
                        } catch (IOException e) {
                            ioe = e;
                            exception = ioe;
                            hasInput = true;
                            return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
                        } catch (SAXParseException e2) {
                            spe = e2;
                            throw SAX2XNIUtil.createXMLParseException0(spe);
                        } catch (SAXException e3) {
                            se = e3;
                            throw SAX2XNIUtil.createXNIException0(se);
                        }
                    }
                    Element schemaElement3 = schemaElement2;
                    XSDKey key2 = key;
                    boolean namespacePrefixes = false;
                    try {
                        if (parser2 != null) {
                            try {
                                namespacePrefixes = parser2.getFeature(NAMESPACE_PREFIXES);
                            } catch (SAXException e4) {
                            }
                        } else {
                            try {
                                parser = XMLReaderFactory.createXMLReader();
                            } catch (SAXException e5) {
                                try {
                                    parser = new SAXParser();
                                } catch (SAXException e6) {
                                    se = e6;
                                    throw SAX2XNIUtil.createXNIException0(se);
                                }
                            }
                            parser2 = parser;
                            try {
                                parser2.setFeature(NAMESPACE_PREFIXES, true);
                                namespacePrefixes = true;
                                if ((parser2 instanceof SAXParser) && (securityManager = this.fSchemaParser.getProperty(SECURITY_MANAGER)) != null) {
                                    parser2.setProperty(SECURITY_MANAGER, securityManager);
                                }
                            } catch (SAXException e7) {
                            }
                        }
                        boolean stringsInternalized = false;
                        try {
                            stringsInternalized = parser2.getFeature(STRING_INTERNING);
                        } catch (SAXException e8) {
                        }
                        if (this.fXSContentHandler == null) {
                            this.fXSContentHandler = new SchemaContentHandler();
                        }
                        this.fXSContentHandler.reset(this.fSchemaParser, this.fSymbolTable, namespacePrefixes, stringsInternalized);
                        parser2.setContentHandler(this.fXSContentHandler);
                        parser2.setErrorHandler(this.fErrorReporter.getSAXErrorHandler());
                        parser2.parse(inputSource);
                        try {
                            parser2.setContentHandler(null);
                            parser2.setErrorHandler(null);
                        } catch (Exception e9) {
                        }
                        Document schemaDocument = this.fXSContentHandler.getDocument();
                        schemaElement = schemaDocument != null ? DOMUtil.getRoot(schemaDocument) : null;
                    } catch (IOException e10) {
                        ioe = e10;
                        schemaElement2 = schemaElement3;
                    } catch (SAXParseException e11) {
                        spe = e11;
                    }
                    try {
                        return getSchemaDocument0(key2, schemaId, schemaElement);
                    } catch (IOException e12) {
                        ioe = e12;
                        schemaElement2 = schemaElement;
                        exception = ioe;
                        hasInput = true;
                        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
                    } catch (SAXParseException e13) {
                        spe = e13;
                        throw SAX2XNIUtil.createXMLParseException0(spe);
                    } catch (SAXException e14) {
                        se = e14;
                        throw SAX2XNIUtil.createXNIException0(se);
                    }
                }
                hasInput = false;
                exception = null;
            } catch (IOException e15) {
                ioe = e15;
            } catch (SAXParseException e16) {
                spe = e16;
            } catch (SAXException e17) {
                se = e17;
            }
        } else {
            hasInput = false;
            exception = null;
        }
        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
    }

    private Element getSchemaDocument(String schemaNamespace, DOMInputSource schemaSource, boolean mustResolve, short referType, Element referElement) {
        boolean hasInput;
        IOException exception;
        Node parent;
        Element schemaRootElement = null;
        Node node = schemaSource.getNode();
        short nodeType = -1;
        if (node != null) {
            nodeType = node.getNodeType();
            if (nodeType == 9) {
                schemaRootElement = DOMUtil.getRoot((Document) node);
            } else if (nodeType == 1) {
                schemaRootElement = (Element) node;
            }
        }
        Element schemaRootElement2 = schemaRootElement;
        short nodeType2 = nodeType;
        if (schemaRootElement2 != null) {
            XSDKey key = null;
            String schemaId = null;
            if (referType != 3) {
                try {
                    schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                    boolean isDocument = nodeType2 == 9;
                    if (!isDocument && (parent = schemaRootElement2.getParentNode()) != null) {
                        isDocument = parent.getNodeType() == 9;
                    }
                    if (isDocument) {
                        try {
                            key = new XSDKey(schemaId, referType, schemaNamespace);
                            Element schemaElement = (Element) this.fTraversed.get(key);
                            if (schemaElement != null) {
                                this.fLastSchemaWasDuplicate = true;
                                return schemaElement;
                            }
                        } catch (IOException e) {
                            exception = e;
                            exception = exception;
                            hasInput = true;
                            return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
                        }
                    }
                } catch (IOException e2) {
                    exception = e2;
                }
            }
            return getSchemaDocument0(key, schemaId, schemaRootElement2);
        }
        hasInput = false;
        exception = null;
        return getSchemaDocument1(mustResolve, hasInput, schemaSource, referElement, exception);
    }

    private Element getSchemaDocument(String schemaNamespace, StAXInputSource schemaSource, boolean mustResolve, short referType, Element referElement) {
        try {
            boolean consumeRemainingContent = schemaSource.shouldConsumeRemainingContent();
            XMLStreamReader streamReader = schemaSource.getXMLStreamReader();
            XMLEventReader eventReader = schemaSource.getXMLEventReader();
            XSDKey key = null;
            String schemaId = null;
            if (referType != 3) {
                schemaId = XMLEntityManager.expandSystemId(schemaSource.getSystemId(), schemaSource.getBaseSystemId(), false);
                boolean isDocument = consumeRemainingContent;
                if (!isDocument) {
                    if (streamReader != null) {
                        isDocument = streamReader.getEventType() == 7;
                    } else {
                        isDocument = eventReader.peek().isStartDocument();
                    }
                }
                if (isDocument) {
                    try {
                        key = new XSDKey(schemaId, referType, schemaNamespace);
                        Element schemaElement = (Element) this.fTraversed.get(key);
                        if (schemaElement != null) {
                            this.fLastSchemaWasDuplicate = true;
                            return schemaElement;
                        }
                    } catch (IOException e) {
                        e = e;
                        return getSchemaDocument1(mustResolve, true, schemaSource, referElement, e);
                    } catch (XMLStreamException e2) {
                        e = e2;
                        Throwable t = e.getNestedException();
                        if (!(t instanceof IOException)) {
                            StAXLocationWrapper slw = new StAXLocationWrapper();
                            slw.setLocation(e.getLocation());
                            throw new XMLParseException(slw, e.getMessage(), e);
                        }
                        e = t;
                        return getSchemaDocument1(mustResolve, true, schemaSource, referElement, e);
                    }
                }
            }
            if (this.fStAXSchemaParser == null) {
                this.fStAXSchemaParser = new StAXSchemaParser();
            }
            this.fStAXSchemaParser.reset(this.fSchemaParser, this.fSymbolTable);
            if (streamReader != null) {
                this.fStAXSchemaParser.parse(streamReader);
                if (consumeRemainingContent) {
                    while (streamReader.hasNext()) {
                        streamReader.next();
                    }
                }
            } else {
                this.fStAXSchemaParser.parse(eventReader);
                if (consumeRemainingContent) {
                    while (eventReader.hasNext()) {
                        eventReader.nextEvent();
                    }
                }
            }
            Document schemaDocument = this.fStAXSchemaParser.getDocument();
            return getSchemaDocument0(key, schemaId, schemaDocument != null ? DOMUtil.getRoot(schemaDocument) : null);
        } catch (IOException e3) {
            e = e3;
        } catch (XMLStreamException e4) {
            e = e4;
        }
    }

    private Element getSchemaDocument0(XSDKey key, String schemaId, Element schemaElement) {
        if (key != null) {
            this.fTraversed.put(key, schemaElement);
        }
        if (schemaId != null) {
            this.fDoc2SystemId.put(schemaElement, schemaId);
        }
        this.fLastSchemaWasDuplicate = false;
        return schemaElement;
    }

    private Element getSchemaDocument1(boolean mustResolve, boolean hasInput, XMLInputSource schemaSource, Element referElement, IOException ioe) {
        if (mustResolve) {
            if (hasInput) {
                reportSchemaError("schema_reference.4", new Object[]{schemaSource.getSystemId()}, referElement, ioe);
            } else {
                Object[] objArr = new Object[1];
                objArr[0] = schemaSource == null ? "" : schemaSource.getSystemId();
                reportSchemaError("schema_reference.4", objArr, referElement, ioe);
            }
        } else if (hasInput) {
            reportSchemaWarning("schema_reference.4", new Object[]{schemaSource.getSystemId()}, referElement, ioe);
        }
        this.fLastSchemaWasDuplicate = false;
        return null;
    }

    private Element getSchemaDocument(XSInputSource schemaSource, XSDDescription desc) {
        SchemaGrammar[] grammars = schemaSource.getGrammars();
        short referType = desc.getContextType();
        if (grammars != null && grammars.length > 0) {
            Vector expandedGrammars = expandGrammars(grammars);
            if (this.fNamespaceGrowth || !existingGrammars(expandedGrammars)) {
                addGrammars(expandedGrammars);
                if (referType == 3) {
                    desc.setTargetNamespace(grammars[0].getTargetNamespace());
                    return null;
                }
                return null;
            }
            return null;
        }
        XSObject[] components = schemaSource.getComponents();
        if (components != null && components.length > 0) {
            Hashtable importDependencies = new Hashtable();
            Vector expandedComponents = expandComponents(components, importDependencies);
            if (this.fNamespaceGrowth || canAddComponents(expandedComponents)) {
                addGlobalComponents(expandedComponents, importDependencies);
                if (referType == 3) {
                    desc.setTargetNamespace(components[0].getNamespace());
                    return null;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private Vector expandGrammars(SchemaGrammar[] grammars) {
        Vector currGrammars = new Vector();
        for (int i = 0; i < grammars.length; i++) {
            if (!currGrammars.contains(grammars[i])) {
                currGrammars.add(grammars[i]);
            }
        }
        for (int i2 = 0; i2 < i; i2++) {
            SchemaGrammar sg1 = (SchemaGrammar) currGrammars.elementAt(i2);
            Vector gs = sg1.getImportedGrammars();
            if (gs != null) {
                for (int j = gs.size() - 1; j >= 0; j--) {
                    SchemaGrammar sg2 = (SchemaGrammar) gs.elementAt(j);
                    if (!currGrammars.contains(sg2)) {
                        currGrammars.addElement(sg2);
                    }
                }
            }
        }
        return currGrammars;
    }

    private boolean existingGrammars(Vector grammars) {
        int length = grammars.size();
        XSDDescription desc = new XSDDescription();
        for (int i = 0; i < length; i++) {
            SchemaGrammar sg1 = (SchemaGrammar) grammars.elementAt(i);
            desc.setNamespace(sg1.getTargetNamespace());
            SchemaGrammar sg2 = findGrammar(desc, false);
            if (sg2 != null) {
                return true;
            }
        }
        return false;
    }

    private boolean canAddComponents(Vector components) {
        int size = components.size();
        XSDDescription desc = new XSDDescription();
        for (int i = 0; i < size; i++) {
            XSObject component = (XSObject) components.elementAt(i);
            if (!canAddComponent(component, desc)) {
                return false;
            }
        }
        return true;
    }

    private boolean canAddComponent(XSObject component, XSDDescription desc) {
        desc.setNamespace(component.getNamespace());
        SchemaGrammar sg = findGrammar(desc, false);
        if (sg == null) {
            return true;
        }
        if (sg.isImmutable()) {
            return false;
        }
        short componentType = component.getType();
        String name = component.getName();
        if (componentType != 11) {
            switch (componentType) {
                case 1:
                    if (sg.getGlobalAttributeDecl(name) == component) {
                        return true;
                    }
                    break;
                case 2:
                    if (sg.getGlobalElementDecl(name) == component) {
                        return true;
                    }
                    break;
                case 3:
                    if (sg.getGlobalTypeDecl(name) == component) {
                        return true;
                    }
                    break;
                default:
                    switch (componentType) {
                        case 5:
                            if (sg.getGlobalAttributeDecl(name) == component) {
                                return true;
                            }
                            break;
                        case 6:
                            if (sg.getGlobalGroupDecl(name) == component) {
                                return true;
                            }
                            break;
                        default:
                            return true;
                    }
                    break;
            }
        } else if (sg.getGlobalNotationDecl(name) == component) {
            return true;
        }
        return false;
    }

    private void addGrammars(Vector grammars) {
        int length = grammars.size();
        XSDDescription desc = new XSDDescription();
        for (int i = 0; i < length; i++) {
            SchemaGrammar sg1 = (SchemaGrammar) grammars.elementAt(i);
            desc.setNamespace(sg1.getTargetNamespace());
            SchemaGrammar sg2 = findGrammar(desc, this.fNamespaceGrowth);
            if (sg1 != sg2) {
                addGrammarComponents(sg1, sg2);
            }
        }
    }

    private void addGrammarComponents(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        if (dstGrammar == null) {
            createGrammarFrom(srcGrammar);
            return;
        }
        SchemaGrammar tmpGrammar = dstGrammar;
        if (tmpGrammar.isImmutable()) {
            tmpGrammar = createGrammarFrom(dstGrammar);
        }
        addNewGrammarLocations(srcGrammar, tmpGrammar);
        addNewImportedGrammars(srcGrammar, tmpGrammar);
        addNewGrammarComponents(srcGrammar, tmpGrammar);
    }

    private SchemaGrammar createGrammarFrom(SchemaGrammar grammar) {
        SchemaGrammar newGrammar = new SchemaGrammar(grammar);
        this.fGrammarBucket.putGrammar(newGrammar);
        updateImportListWith(newGrammar);
        updateImportListFor(newGrammar);
        return newGrammar;
    }

    private void addNewGrammarLocations(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        StringList locations = srcGrammar.getDocumentLocations();
        int locSize = locations.size();
        StringList locations2 = dstGrammar.getDocumentLocations();
        for (int i = 0; i < locSize; i++) {
            String loc = locations.item(i);
            if (!locations2.contains(loc)) {
                dstGrammar.addDocument(null, loc);
            }
        }
    }

    private void addNewImportedGrammars(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        Vector igs1 = srcGrammar.getImportedGrammars();
        if (igs1 != null) {
            Vector igs2 = dstGrammar.getImportedGrammars();
            if (igs2 == null) {
                dstGrammar.setImportedGrammars((Vector) igs1.clone());
            } else {
                updateImportList(igs1, igs2);
            }
        }
    }

    private void updateImportList(Vector importedSrc, Vector importedDst) {
        int size = importedSrc.size();
        for (int i = 0; i < size; i++) {
            SchemaGrammar sg = (SchemaGrammar) importedSrc.elementAt(i);
            if (!containedImportedGrammar(importedDst, sg)) {
                importedDst.add(sg);
            }
        }
    }

    private void addNewGrammarComponents(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        dstGrammar.resetComponents();
        addGlobalElementDecls(srcGrammar, dstGrammar);
        addGlobalAttributeDecls(srcGrammar, dstGrammar);
        addGlobalAttributeGroupDecls(srcGrammar, dstGrammar);
        addGlobalGroupDecls(srcGrammar, dstGrammar);
        addGlobalTypeDecls(srcGrammar, dstGrammar);
        addGlobalNotationDecls(srcGrammar, dstGrammar);
    }

    private void addGlobalElementDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents((short) 2);
        int len = components.getLength();
        for (int i = 0; i < len; i++) {
            XSElementDecl srcDecl = (XSElementDecl) components.item(i);
            XSElementDecl dstDecl = dstGrammar.getGlobalElementDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalElementDecl(srcDecl);
            }
        }
        ObjectList componentsExt = srcGrammar.getComponentsExt((short) 2);
        int len2 = componentsExt.getLength();
        for (int i2 = 0; i2 < len2; i2 += 2) {
            String key = (String) componentsExt.item(i2);
            int index = key.indexOf(44);
            String location = key.substring(0, index);
            String name = key.substring(index + 1, key.length());
            XSElementDecl srcDecl2 = (XSElementDecl) componentsExt.item(i2 + 1);
            XSElementDecl dstDecl2 = dstGrammar.getGlobalElementDecl(name, location);
            if (dstDecl2 == null) {
                dstGrammar.addGlobalElementDecl(srcDecl2, location);
            }
        }
    }

    private void addGlobalAttributeDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents((short) 1);
        int len = components.getLength();
        for (int i = 0; i < len; i++) {
            XSAttributeDecl srcDecl = (XSAttributeDecl) components.item(i);
            XSAttributeDecl dstDecl = dstGrammar.getGlobalAttributeDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalAttributeDecl(srcDecl);
            } else if (dstDecl != srcDecl && !this.fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }
        ObjectList componentsExt = srcGrammar.getComponentsExt((short) 1);
        int len2 = componentsExt.getLength();
        for (int i2 = 0; i2 < len2; i2 += 2) {
            String key = (String) componentsExt.item(i2);
            int index = key.indexOf(44);
            String location = key.substring(0, index);
            String name = key.substring(index + 1, key.length());
            XSAttributeDecl srcDecl2 = (XSAttributeDecl) componentsExt.item(i2 + 1);
            if (dstGrammar.getGlobalAttributeDecl(name, location) == null) {
                dstGrammar.addGlobalAttributeDecl(srcDecl2, location);
            }
        }
    }

    private void addGlobalAttributeGroupDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents((short) 5);
        int len = components.getLength();
        for (int i = 0; i < len; i++) {
            XSAttributeGroupDecl srcDecl = (XSAttributeGroupDecl) components.item(i);
            XSAttributeGroupDecl dstDecl = dstGrammar.getGlobalAttributeGroupDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalAttributeGroupDecl(srcDecl);
            } else if (dstDecl != srcDecl && !this.fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }
        ObjectList componentsExt = srcGrammar.getComponentsExt((short) 5);
        int len2 = componentsExt.getLength();
        for (int i2 = 0; i2 < len2; i2 += 2) {
            String key = (String) componentsExt.item(i2);
            int index = key.indexOf(44);
            String location = key.substring(0, index);
            String name = key.substring(index + 1, key.length());
            XSAttributeGroupDecl srcDecl2 = (XSAttributeGroupDecl) componentsExt.item(i2 + 1);
            if (dstGrammar.getGlobalAttributeGroupDecl(name, location) == null) {
                dstGrammar.addGlobalAttributeGroupDecl(srcDecl2, location);
            }
        }
    }

    private void addGlobalNotationDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents((short) 11);
        int len = components.getLength();
        for (int i = 0; i < len; i++) {
            XSNotationDecl srcDecl = (XSNotationDecl) components.item(i);
            XSNotationDecl dstDecl = dstGrammar.getGlobalNotationDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalNotationDecl(srcDecl);
            } else if (dstDecl != srcDecl && !this.fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }
        ObjectList componentsExt = srcGrammar.getComponentsExt((short) 11);
        int len2 = componentsExt.getLength();
        for (int i2 = 0; i2 < len2; i2 += 2) {
            String key = (String) componentsExt.item(i2);
            int index = key.indexOf(44);
            String location = key.substring(0, index);
            String name = key.substring(index + 1, key.length());
            XSNotationDecl srcDecl2 = (XSNotationDecl) componentsExt.item(i2 + 1);
            if (dstGrammar.getGlobalNotationDecl(name, location) == null) {
                dstGrammar.addGlobalNotationDecl(srcDecl2, location);
            }
        }
    }

    private void addGlobalGroupDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents((short) 6);
        int len = components.getLength();
        for (int i = 0; i < len; i++) {
            XSGroupDecl srcDecl = (XSGroupDecl) components.item(i);
            XSGroupDecl dstDecl = dstGrammar.getGlobalGroupDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalGroupDecl(srcDecl);
            } else if (srcDecl != dstDecl && !this.fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }
        ObjectList componentsExt = srcGrammar.getComponentsExt((short) 6);
        int len2 = componentsExt.getLength();
        for (int i2 = 0; i2 < len2; i2 += 2) {
            String key = (String) componentsExt.item(i2);
            int index = key.indexOf(44);
            String location = key.substring(0, index);
            String name = key.substring(index + 1, key.length());
            XSGroupDecl srcDecl2 = (XSGroupDecl) componentsExt.item(i2 + 1);
            if (dstGrammar.getGlobalGroupDecl(name, location) == null) {
                dstGrammar.addGlobalGroupDecl(srcDecl2, location);
            }
        }
    }

    private void addGlobalTypeDecls(SchemaGrammar srcGrammar, SchemaGrammar dstGrammar) {
        XSNamedMap components = srcGrammar.getComponents((short) 3);
        int len = components.getLength();
        for (int i = 0; i < len; i++) {
            XSTypeDefinition srcDecl = (XSTypeDefinition) components.item(i);
            XSTypeDefinition dstDecl = dstGrammar.getGlobalTypeDecl(srcDecl.getName());
            if (dstDecl == null) {
                dstGrammar.addGlobalTypeDecl(srcDecl);
            } else if (dstDecl != srcDecl && !this.fTolerateDuplicates) {
                reportSharingError(srcDecl.getNamespace(), srcDecl.getName());
            }
        }
        ObjectList componentsExt = srcGrammar.getComponentsExt((short) 3);
        int len2 = componentsExt.getLength();
        for (int i2 = 0; i2 < len2; i2 += 2) {
            String key = (String) componentsExt.item(i2);
            int index = key.indexOf(44);
            String location = key.substring(0, index);
            String name = key.substring(index + 1, key.length());
            XSTypeDefinition srcDecl2 = (XSTypeDefinition) componentsExt.item(i2 + 1);
            if (dstGrammar.getGlobalTypeDecl(name, location) == null) {
                dstGrammar.addGlobalTypeDecl(srcDecl2, location);
            }
        }
    }

    private Vector expandComponents(XSObject[] components, Hashtable dependencies) {
        Vector newComponents = new Vector();
        for (int i = 0; i < components.length; i++) {
            if (!newComponents.contains(components[i])) {
                newComponents.add(components[i]);
            }
        }
        for (int i2 = 0; i2 < newComponents.size(); i2++) {
            XSObject component = (XSObject) newComponents.elementAt(i2);
            expandRelatedComponents(component, newComponents, dependencies);
        }
        return newComponents;
    }

    private void expandRelatedComponents(XSObject component, Vector componentList, Hashtable dependencies) {
        short componentType = component.getType();
        switch (componentType) {
            case 1:
                expandRelatedAttributeComponents((XSAttributeDeclaration) component, componentList, component.getNamespace(), dependencies);
                return;
            case 2:
                break;
            case 3:
                expandRelatedTypeComponents((XSTypeDefinition) component, componentList, component.getNamespace(), dependencies);
                return;
            case 4:
            default:
                return;
            case 5:
                expandRelatedAttributeGroupComponents((XSAttributeGroupDefinition) component, componentList, component.getNamespace(), dependencies);
                break;
            case 6:
                expandRelatedModelGroupDefinitionComponents((XSModelGroupDefinition) component, componentList, component.getNamespace(), dependencies);
                return;
        }
        expandRelatedElementComponents((XSElementDeclaration) component, componentList, component.getNamespace(), dependencies);
    }

    private void expandRelatedAttributeComponents(XSAttributeDeclaration decl, Vector componentList, String namespace, Hashtable dependencies) {
        addRelatedType(decl.getTypeDefinition(), componentList, namespace, dependencies);
    }

    private void expandRelatedElementComponents(XSElementDeclaration decl, Vector componentList, String namespace, Hashtable dependencies) {
        addRelatedType(decl.getTypeDefinition(), componentList, namespace, dependencies);
        XSElementDeclaration subElemDecl = decl.getSubstitutionGroupAffiliation();
        if (subElemDecl != null) {
            addRelatedElement(subElemDecl, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedTypeComponents(XSTypeDefinition xSTypeDefinition, Vector componentList, String namespace, Hashtable dependencies) {
        if (xSTypeDefinition instanceof XSComplexTypeDecl) {
            expandRelatedComplexTypeComponents(xSTypeDefinition, componentList, namespace, dependencies);
        } else if (xSTypeDefinition instanceof XSSimpleTypeDecl) {
            expandRelatedSimpleTypeComponents(xSTypeDefinition, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedModelGroupDefinitionComponents(XSModelGroupDefinition modelGroupDef, Vector componentList, String namespace, Hashtable dependencies) {
        expandRelatedModelGroupComponents(modelGroupDef.getModelGroup(), componentList, namespace, dependencies);
    }

    private void expandRelatedAttributeGroupComponents(XSAttributeGroupDefinition attrGroup, Vector componentList, String namespace, Hashtable dependencies) {
        expandRelatedAttributeUsesComponents(attrGroup.getAttributeUses(), componentList, namespace, dependencies);
    }

    private void expandRelatedComplexTypeComponents(XSComplexTypeDecl type, Vector componentList, String namespace, Hashtable dependencies) {
        addRelatedType(type.getBaseType(), componentList, namespace, dependencies);
        expandRelatedAttributeUsesComponents(type.getAttributeUses(), componentList, namespace, dependencies);
        XSParticle particle = type.getParticle();
        if (particle != null) {
            expandRelatedParticleComponents(particle, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedSimpleTypeComponents(XSSimpleTypeDefinition type, Vector componentList, String namespace, Hashtable dependencies) {
        XSTypeDefinition baseType = type.getBaseType();
        if (baseType != null) {
            addRelatedType(baseType, componentList, namespace, dependencies);
        }
        XSTypeDefinition itemType = type.getItemType();
        if (itemType != null) {
            addRelatedType(itemType, componentList, namespace, dependencies);
        }
        XSTypeDefinition primitiveType = type.getPrimitiveType();
        if (primitiveType != null) {
            addRelatedType(primitiveType, componentList, namespace, dependencies);
        }
        XSObjectList memberTypes = type.getMemberTypes();
        if (memberTypes.size() > 0) {
            for (int i = 0; i < memberTypes.size(); i++) {
                addRelatedType((XSTypeDefinition) memberTypes.item(i), componentList, namespace, dependencies);
            }
        }
    }

    private void expandRelatedAttributeUsesComponents(XSObjectList attrUses, Vector componentList, String namespace, Hashtable dependencies) {
        int attrUseSize = attrUses == null ? 0 : attrUses.size();
        for (int i = 0; i < attrUseSize; i++) {
            expandRelatedAttributeUseComponents((XSAttributeUse) attrUses.item(i), componentList, namespace, dependencies);
        }
    }

    private void expandRelatedAttributeUseComponents(XSAttributeUse component, Vector componentList, String namespace, Hashtable dependencies) {
        addRelatedAttribute(component.getAttrDeclaration(), componentList, namespace, dependencies);
    }

    private void expandRelatedParticleComponents(XSParticle component, Vector componentList, String namespace, Hashtable dependencies) {
        XSTerm term = component.getTerm();
        short type = term.getType();
        if (type == 2) {
            addRelatedElement((XSElementDeclaration) term, componentList, namespace, dependencies);
        } else if (type == 7) {
            expandRelatedModelGroupComponents((XSModelGroup) term, componentList, namespace, dependencies);
        }
    }

    private void expandRelatedModelGroupComponents(XSModelGroup modelGroup, Vector componentList, String namespace, Hashtable dependencies) {
        XSObjectList particles = modelGroup.getParticles();
        int length = particles == null ? 0 : particles.getLength();
        for (int i = 0; i < length; i++) {
            expandRelatedParticleComponents((XSParticle) particles.item(i), componentList, namespace, dependencies);
        }
    }

    private void addRelatedType(XSTypeDefinition type, Vector componentList, String namespace, Hashtable dependencies) {
        if (!type.getAnonymous()) {
            if (!type.getNamespace().equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && !componentList.contains(type)) {
                Vector importedNamespaces = findDependentNamespaces(namespace, dependencies);
                addNamespaceDependency(namespace, type.getNamespace(), importedNamespaces);
                componentList.add(type);
                return;
            }
            return;
        }
        expandRelatedTypeComponents(type, componentList, namespace, dependencies);
    }

    private void addRelatedElement(XSElementDeclaration decl, Vector componentList, String namespace, Hashtable dependencies) {
        if (decl.getScope() == 1) {
            if (!componentList.contains(decl)) {
                Vector importedNamespaces = findDependentNamespaces(namespace, dependencies);
                addNamespaceDependency(namespace, decl.getNamespace(), importedNamespaces);
                componentList.add(decl);
                return;
            }
            return;
        }
        expandRelatedElementComponents(decl, componentList, namespace, dependencies);
    }

    private void addRelatedAttribute(XSAttributeDeclaration decl, Vector componentList, String namespace, Hashtable dependencies) {
        if (decl.getScope() == 1) {
            if (!componentList.contains(decl)) {
                Vector importedNamespaces = findDependentNamespaces(namespace, dependencies);
                addNamespaceDependency(namespace, decl.getNamespace(), importedNamespaces);
                componentList.add(decl);
                return;
            }
            return;
        }
        expandRelatedAttributeComponents(decl, componentList, namespace, dependencies);
    }

    private void addGlobalComponents(Vector components, Hashtable importDependencies) {
        XSDDescription desc = new XSDDescription();
        int size = components.size();
        for (int i = 0; i < size; i++) {
            addGlobalComponent((XSObject) components.elementAt(i), desc);
        }
        updateImportDependencies(importDependencies);
    }

    private void addGlobalComponent(XSObject component, XSDDescription desc) {
        String namespace = component.getNamespace();
        desc.setNamespace(namespace);
        SchemaGrammar sg = getSchemaGrammar(desc);
        short componentType = component.getType();
        String name = component.getName();
        if (componentType != 11) {
            switch (componentType) {
                case 1:
                    if (((XSAttributeDecl) component).getScope() == 1) {
                        if (sg.getGlobalAttributeDecl(name) == null) {
                            sg.addGlobalAttributeDecl((XSAttributeDecl) component);
                        }
                        if (sg.getGlobalAttributeDecl(name, "") == null) {
                            sg.addGlobalAttributeDecl((XSAttributeDecl) component, "");
                        }
                    }
                    break;
                case 2:
                    if (((XSElementDecl) component).getScope() == 1) {
                        sg.addGlobalElementDeclAll((XSElementDecl) component);
                        if (sg.getGlobalElementDecl(name) == null) {
                            sg.addGlobalElementDecl((XSElementDecl) component);
                        }
                        if (sg.getGlobalElementDecl(name, "") == null) {
                            sg.addGlobalElementDecl((XSElementDecl) component, "");
                        }
                    }
                    break;
                case 3:
                    if (!((XSTypeDefinition) component).getAnonymous()) {
                        if (sg.getGlobalTypeDecl(name) == null) {
                            sg.addGlobalTypeDecl((XSTypeDefinition) component);
                        }
                        if (sg.getGlobalTypeDecl(name, "") == null) {
                            sg.addGlobalTypeDecl((XSTypeDefinition) component, "");
                        }
                    }
                    break;
                default:
                    switch (componentType) {
                        case 5:
                            if (sg.getGlobalAttributeDecl(name) == null) {
                                sg.addGlobalAttributeGroupDecl((XSAttributeGroupDecl) component);
                            }
                            if (sg.getGlobalAttributeDecl(name, "") == null) {
                                sg.addGlobalAttributeGroupDecl((XSAttributeGroupDecl) component, "");
                            }
                            break;
                        case 6:
                            if (sg.getGlobalGroupDecl(name) == null) {
                                sg.addGlobalGroupDecl((XSGroupDecl) component);
                            }
                            if (sg.getGlobalGroupDecl(name, "") == null) {
                                sg.addGlobalGroupDecl((XSGroupDecl) component, "");
                            }
                            break;
                    }
                    break;
            }
        }
        if (sg.getGlobalNotationDecl(name) == null) {
            sg.addGlobalNotationDecl((XSNotationDecl) component);
        }
        if (sg.getGlobalNotationDecl(name, "") == null) {
            sg.addGlobalNotationDecl((XSNotationDecl) component, "");
        }
    }

    private void updateImportDependencies(Hashtable table) {
        Enumeration keys = table.keys();
        while (keys.hasMoreElements()) {
            String namespace = (String) keys.nextElement();
            Vector importList = (Vector) table.get(null2EmptyString(namespace));
            if (importList.size() > 0) {
                expandImportList(namespace, importList);
            }
        }
    }

    private void expandImportList(String namespace, Vector namespaceList) {
        SchemaGrammar sg = this.fGrammarBucket.getGrammar(namespace);
        if (sg != null) {
            Vector isgs = sg.getImportedGrammars();
            if (isgs == null) {
                Vector isgs2 = new Vector();
                addImportList(sg, isgs2, namespaceList);
                sg.setImportedGrammars(isgs2);
                return;
            }
            updateImportList(sg, isgs, namespaceList);
        }
    }

    private void addImportList(SchemaGrammar sg, Vector importedGrammars, Vector namespaceList) {
        int size = namespaceList.size();
        for (int i = 0; i < size; i++) {
            SchemaGrammar isg = this.fGrammarBucket.getGrammar((String) namespaceList.elementAt(i));
            if (isg != null) {
                importedGrammars.add(isg);
            }
        }
    }

    private void updateImportList(SchemaGrammar sg, Vector importedGrammars, Vector namespaceList) {
        int size = namespaceList.size();
        for (int i = 0; i < size; i++) {
            SchemaGrammar isg = this.fGrammarBucket.getGrammar((String) namespaceList.elementAt(i));
            if (isg != null && !containedImportedGrammar(importedGrammars, isg)) {
                importedGrammars.add(isg);
            }
        }
    }

    private boolean containedImportedGrammar(Vector importedGrammar, SchemaGrammar grammar) {
        int size = importedGrammar.size();
        for (int i = 0; i < size; i++) {
            SchemaGrammar sg = (SchemaGrammar) importedGrammar.elementAt(i);
            if (null2EmptyString(sg.getTargetNamespace()).equals(null2EmptyString(grammar.getTargetNamespace()))) {
                return true;
            }
        }
        return false;
    }

    private SchemaGrammar getSchemaGrammar(XSDDescription desc) {
        SchemaGrammar sg = findGrammar(desc, this.fNamespaceGrowth);
        if (sg == null) {
            SchemaGrammar sg2 = new SchemaGrammar(desc.getNamespace(), desc.makeClone(), this.fSymbolTable);
            this.fGrammarBucket.putGrammar(sg2);
            return sg2;
        }
        if (sg.isImmutable()) {
            return createGrammarFrom(sg);
        }
        return sg;
    }

    private Vector findDependentNamespaces(String namespace, Hashtable table) {
        String ns = null2EmptyString(namespace);
        Vector namespaceList = (Vector) table.get(ns);
        if (namespaceList == null) {
            Vector namespaceList2 = new Vector();
            table.put(ns, namespaceList2);
            return namespaceList2;
        }
        return namespaceList;
    }

    private void addNamespaceDependency(String namespace1, String namespace2, Vector list) {
        String ns1 = null2EmptyString(namespace1);
        String ns2 = null2EmptyString(namespace2);
        if (!ns1.equals(ns2) && !list.contains(ns2)) {
            list.add(ns2);
        }
    }

    private void reportSharingError(String namespace, String name) {
        StringBuilder sb;
        if (namespace == null) {
            sb = new StringBuilder(",");
        } else {
            sb = new StringBuilder(String.valueOf(namespace));
            sb.append(",");
        }
        sb.append(name);
        String qName = sb.toString();
        reportSchemaError("sch-props-correct.2", new Object[]{qName}, null);
    }

    private void createTraversers() {
        this.fAttributeChecker = new XSAttributeChecker(this);
        this.fAttributeGroupTraverser = new XSDAttributeGroupTraverser(this, this.fAttributeChecker);
        this.fAttributeTraverser = new XSDAttributeTraverser(this, this.fAttributeChecker);
        this.fComplexTypeTraverser = new XSDComplexTypeTraverser(this, this.fAttributeChecker);
        this.fElementTraverser = new XSDElementTraverser(this, this.fAttributeChecker);
        this.fGroupTraverser = new XSDGroupTraverser(this, this.fAttributeChecker);
        this.fKeyrefTraverser = new XSDKeyrefTraverser(this, this.fAttributeChecker);
        this.fNotationTraverser = new XSDNotationTraverser(this, this.fAttributeChecker);
        this.fSimpleTypeTraverser = new XSDSimpleTypeTraverser(this, this.fAttributeChecker);
        this.fUniqueOrKeyTraverser = new XSDUniqueOrKeyTraverser(this, this.fAttributeChecker);
        this.fWildCardTraverser = new XSDWildcardTraverser(this, this.fAttributeChecker);
    }

    void prepareForParse() {
        this.fTraversed.clear();
        this.fDoc2SystemId.clear();
        this.fHiddenNodes.clear();
        this.fLastSchemaWasDuplicate = false;
    }

    void prepareForTraverse() {
        this.fUnparsedAttributeRegistry.clear();
        this.fUnparsedAttributeGroupRegistry.clear();
        this.fUnparsedElementRegistry.clear();
        this.fUnparsedGroupRegistry.clear();
        this.fUnparsedIdentityConstraintRegistry.clear();
        this.fUnparsedNotationRegistry.clear();
        this.fUnparsedTypeRegistry.clear();
        this.fUnparsedAttributeRegistrySub.clear();
        this.fUnparsedAttributeGroupRegistrySub.clear();
        this.fUnparsedElementRegistrySub.clear();
        this.fUnparsedGroupRegistrySub.clear();
        this.fUnparsedIdentityConstraintRegistrySub.clear();
        this.fUnparsedNotationRegistrySub.clear();
        this.fUnparsedTypeRegistrySub.clear();
        for (int i = 1; i <= 7; i++) {
            this.fUnparsedRegistriesExt[i].clear();
        }
        this.fXSDocumentInfoRegistry.clear();
        this.fDependencyMap.clear();
        this.fDoc2XSDocumentMap.clear();
        this.fRedefine2XSDMap.clear();
        this.fRedefine2NSSupport.clear();
        this.fAllTNSs.removeAllElements();
        this.fImportMap.clear();
        this.fRoot = null;
        for (int i2 = 0; i2 < this.fLocalElemStackPos; i2++) {
            this.fParticle[i2] = null;
            this.fLocalElementDecl[i2] = null;
            this.fLocalElementDecl_schema[i2] = null;
            this.fLocalElemNamespaceContext[i2] = null;
        }
        this.fLocalElemStackPos = 0;
        for (int i3 = 0; i3 < this.fKeyrefStackPos; i3++) {
            this.fKeyrefs[i3] = null;
            this.fKeyrefElems[i3] = null;
            this.fKeyrefNamespaceContext[i3] = null;
            this.fKeyrefsMapXSDocumentInfo[i3] = null;
        }
        this.fKeyrefStackPos = 0;
        if (this.fAttributeChecker == null) {
            createTraversers();
        }
        Locale locale = this.fErrorReporter.getLocale();
        this.fAttributeChecker.reset(this.fSymbolTable);
        this.fAttributeGroupTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fAttributeTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fComplexTypeTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fElementTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fGroupTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fKeyrefTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fNotationTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fSimpleTypeTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fUniqueOrKeyTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fWildCardTraverser.reset(this.fSymbolTable, this.fValidateAnnotations, locale);
        this.fRedefinedRestrictedAttributeGroupRegistry.clear();
        this.fRedefinedRestrictedGroupRegistry.clear();
        this.fGlobalAttrDecls.clear();
        this.fGlobalAttrGrpDecls.clear();
        this.fGlobalElemDecls.clear();
        this.fGlobalGroupDecls.clear();
        this.fGlobalNotationDecls.clear();
        this.fGlobalIDConstraintDecls.clear();
        this.fGlobalTypeDecls.clear();
    }

    public void setDeclPool(XSDeclarationPool declPool) {
        this.fDeclPool = declPool;
    }

    public void setDVFactory(SchemaDVFactory dvFactory) {
        this.fDVFactory = dvFactory;
    }

    public void reset(XMLComponentManager componentManager) {
        this.fSymbolTable = (SymbolTable) componentManager.getProperty("http://apache.org/xml/properties/internal/symbol-table");
        this.fEntityResolver = (XMLEntityResolver) componentManager.getProperty(ENTITY_MANAGER);
        XMLEntityResolver er = (XMLEntityResolver) componentManager.getProperty("http://apache.org/xml/properties/internal/entity-resolver");
        if (er != null) {
            this.fSchemaParser.setEntityResolver(er);
        }
        this.fErrorReporter = (XMLErrorReporter) componentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        try {
            XMLErrorHandler currErrorHandler = this.fErrorReporter.getErrorHandler();
            if (currErrorHandler != this.fSchemaParser.getProperty(ERROR_HANDLER)) {
                this.fSchemaParser.setProperty(ERROR_HANDLER, currErrorHandler != null ? currErrorHandler : new DefaultErrorHandler());
                if (this.fAnnotationValidator != null) {
                    this.fAnnotationValidator.setProperty(ERROR_HANDLER, currErrorHandler != null ? currErrorHandler : new DefaultErrorHandler());
                }
            }
            Locale currentLocale = this.fErrorReporter.getLocale();
            if (currentLocale != this.fSchemaParser.getProperty("http://apache.org/xml/properties/locale")) {
                this.fSchemaParser.setProperty("http://apache.org/xml/properties/locale", currentLocale);
                if (this.fAnnotationValidator != null) {
                    this.fAnnotationValidator.setProperty("http://apache.org/xml/properties/locale", currentLocale);
                }
            }
        } catch (XMLConfigurationException e) {
        }
        try {
            this.fValidateAnnotations = componentManager.getFeature(VALIDATE_ANNOTATIONS);
        } catch (XMLConfigurationException e2) {
            this.fValidateAnnotations = false;
        }
        try {
            this.fHonourAllSchemaLocations = componentManager.getFeature(HONOUR_ALL_SCHEMALOCATIONS);
        } catch (XMLConfigurationException e3) {
            this.fHonourAllSchemaLocations = false;
        }
        try {
            this.fNamespaceGrowth = componentManager.getFeature(NAMESPACE_GROWTH);
        } catch (XMLConfigurationException e4) {
            this.fNamespaceGrowth = false;
        }
        try {
            this.fTolerateDuplicates = componentManager.getFeature(TOLERATE_DUPLICATES);
        } catch (XMLConfigurationException e5) {
            this.fTolerateDuplicates = false;
        }
        try {
            this.fSchemaParser.setFeature(CONTINUE_AFTER_FATAL_ERROR, this.fErrorReporter.getFeature(CONTINUE_AFTER_FATAL_ERROR));
        } catch (XMLConfigurationException e6) {
        }
        try {
            this.fSchemaParser.setFeature(ALLOW_JAVA_ENCODINGS, componentManager.getFeature(ALLOW_JAVA_ENCODINGS));
        } catch (XMLConfigurationException e7) {
        }
        try {
            this.fSchemaParser.setFeature(STANDARD_URI_CONFORMANT_FEATURE, componentManager.getFeature(STANDARD_URI_CONFORMANT_FEATURE));
        } catch (XMLConfigurationException e8) {
        }
        try {
            this.fGrammarPool = (XMLGrammarPool) componentManager.getProperty("http://apache.org/xml/properties/internal/grammar-pool");
        } catch (XMLConfigurationException e9) {
            this.fGrammarPool = null;
        }
        try {
            this.fSchemaParser.setFeature(DISALLOW_DOCTYPE, componentManager.getFeature(DISALLOW_DOCTYPE));
        } catch (XMLConfigurationException e10) {
        }
        try {
            Object security = componentManager.getProperty(SECURITY_MANAGER);
            if (security != null) {
                this.fSchemaParser.setProperty(SECURITY_MANAGER, security);
            }
        } catch (XMLConfigurationException e11) {
        }
    }

    void traverseLocalElements() {
        this.fElementTraverser.fDeferTraversingLocalElements = false;
        for (int i = 0; i < this.fLocalElemStackPos; i++) {
            Element currElem = this.fLocalElementDecl[i];
            XSDocumentInfo currSchema = this.fLocalElementDecl_schema[i];
            SchemaGrammar currGrammar = this.fGrammarBucket.getGrammar(currSchema.fTargetNamespace);
            this.fElementTraverser.traverseLocal(this.fParticle[i], currElem, currSchema, currGrammar, this.fAllContext[i], this.fParent[i], this.fLocalElemNamespaceContext[i]);
            if (this.fParticle[i].fType == 0) {
                XSModelGroupImpl group = null;
                if (this.fParent[i] instanceof XSComplexTypeDecl) {
                    XSParticle p = ((XSComplexTypeDecl) this.fParent[i]).getParticle();
                    if (p != null) {
                        group = (XSModelGroupImpl) p.getTerm();
                    }
                } else {
                    group = ((XSGroupDecl) this.fParent[i]).fModelGroup;
                }
                if (group != null) {
                    removeParticle(group, this.fParticle[i]);
                }
            }
        }
    }

    private boolean removeParticle(XSModelGroupImpl group, XSParticleDecl particle) {
        for (int i = 0; i < group.fParticleCount; i++) {
            XSParticleDecl member = group.fParticles[i];
            if (member == particle) {
                for (int j = i; j < group.fParticleCount - 1; j++) {
                    group.fParticles[j] = group.fParticles[j + 1];
                }
                int j2 = group.fParticleCount;
                group.fParticleCount = j2 - 1;
                return true;
            }
            int j3 = member.fType;
            if (j3 == 3 && removeParticle((XSModelGroupImpl) member.fValue, particle)) {
                return true;
            }
        }
        return false;
    }

    void fillInLocalElemInfo(Element elmDecl, XSDocumentInfo schemaDoc, int allContextFlags, XSObject parent, XSParticleDecl particle) {
        if (this.fParticle.length == this.fLocalElemStackPos) {
            XSParticleDecl[] newStackP = new XSParticleDecl[this.fLocalElemStackPos + 10];
            System.arraycopy(this.fParticle, 0, newStackP, 0, this.fLocalElemStackPos);
            this.fParticle = newStackP;
            Element[] newStackE = new Element[this.fLocalElemStackPos + 10];
            System.arraycopy(this.fLocalElementDecl, 0, newStackE, 0, this.fLocalElemStackPos);
            this.fLocalElementDecl = newStackE;
            XSDocumentInfo[] newStackE_schema = new XSDocumentInfo[this.fLocalElemStackPos + 10];
            System.arraycopy(this.fLocalElementDecl_schema, 0, newStackE_schema, 0, this.fLocalElemStackPos);
            this.fLocalElementDecl_schema = newStackE_schema;
            int[] newStackI = new int[this.fLocalElemStackPos + 10];
            System.arraycopy(this.fAllContext, 0, newStackI, 0, this.fLocalElemStackPos);
            this.fAllContext = newStackI;
            XSObject[] newStackC = new XSObject[this.fLocalElemStackPos + 10];
            System.arraycopy(this.fParent, 0, newStackC, 0, this.fLocalElemStackPos);
            this.fParent = newStackC;
            String[][] newStackN = new String[this.fLocalElemStackPos + 10][];
            System.arraycopy(this.fLocalElemNamespaceContext, 0, newStackN, 0, this.fLocalElemStackPos);
            this.fLocalElemNamespaceContext = newStackN;
        }
        this.fParticle[this.fLocalElemStackPos] = particle;
        this.fLocalElementDecl[this.fLocalElemStackPos] = elmDecl;
        this.fLocalElementDecl_schema[this.fLocalElemStackPos] = schemaDoc;
        this.fAllContext[this.fLocalElemStackPos] = allContextFlags;
        this.fParent[this.fLocalElemStackPos] = parent;
        String[][] strArr = this.fLocalElemNamespaceContext;
        int i = this.fLocalElemStackPos;
        this.fLocalElemStackPos = i + 1;
        strArr[i] = schemaDoc.fNamespaceSupport.getEffectiveLocalContext();
    }

    void checkForDuplicateNames(String qName, int declType, Hashtable registry, Hashtable registry_sub, Element currComp, XSDocumentInfo currSchema) {
        Object objElem = registry.get(qName);
        if (objElem == null) {
            if (this.fNamespaceGrowth && !this.fTolerateDuplicates) {
                checkForDuplicateNames(qName, declType, currComp);
            }
            registry.put(qName, currComp);
            registry_sub.put(qName, currSchema);
        } else {
            Element collidingElem = (Element) objElem;
            XSDocumentInfo collidingElemSchema = (XSDocumentInfo) registry_sub.get(qName);
            if (collidingElem == currComp) {
                return;
            }
            XSDocumentInfo redefinedSchema = null;
            boolean collidedWithRedefine = true;
            Element elemParent = DOMUtil.getParent(collidingElem);
            if (DOMUtil.getLocalName(elemParent).equals(SchemaSymbols.ELT_REDEFINE)) {
                redefinedSchema = (XSDocumentInfo) this.fRedefine2XSDMap.get(elemParent);
            } else if (DOMUtil.getLocalName(DOMUtil.getParent(currComp)).equals(SchemaSymbols.ELT_REDEFINE)) {
                redefinedSchema = collidingElemSchema;
                collidedWithRedefine = false;
            }
            XSDocumentInfo redefinedSchema2 = redefinedSchema;
            boolean collidedWithRedefine2 = collidedWithRedefine;
            if (redefinedSchema2 == null) {
                if (!this.fTolerateDuplicates || this.fUnparsedRegistriesExt[declType].get(qName) == currSchema) {
                    reportSchemaError("sch-props-correct.2", new Object[]{qName}, currComp);
                }
            } else {
                if (collidingElemSchema == currSchema) {
                    reportSchemaError("sch-props-correct.2", new Object[]{qName}, currComp);
                    return;
                }
                String newName = String.valueOf(qName.substring(qName.lastIndexOf(44) + 1)) + REDEF_IDENTIFIER;
                if (redefinedSchema2 == currSchema) {
                    currComp.setAttribute(SchemaSymbols.ATT_NAME, newName);
                    if (currSchema.fTargetNamespace == null) {
                        registry.put("," + newName, currComp);
                        registry_sub.put("," + newName, currSchema);
                    } else {
                        registry.put(String.valueOf(currSchema.fTargetNamespace) + "," + newName, currComp);
                        registry_sub.put(String.valueOf(currSchema.fTargetNamespace) + "," + newName, currSchema);
                    }
                    if (currSchema.fTargetNamespace != null) {
                        checkForDuplicateNames(String.valueOf(currSchema.fTargetNamespace) + "," + newName, declType, registry, registry_sub, currComp, currSchema);
                    } else {
                        checkForDuplicateNames("," + newName, declType, registry, registry_sub, currComp, currSchema);
                    }
                } else if (collidedWithRedefine2) {
                    if (currSchema.fTargetNamespace == null) {
                        checkForDuplicateNames("," + newName, declType, registry, registry_sub, currComp, currSchema);
                    } else {
                        checkForDuplicateNames(String.valueOf(currSchema.fTargetNamespace) + "," + newName, declType, registry, registry_sub, currComp, currSchema);
                    }
                } else {
                    reportSchemaError("sch-props-correct.2", new Object[]{qName}, currComp);
                }
            }
        }
        if (this.fTolerateDuplicates) {
            this.fUnparsedRegistriesExt[declType].put(qName, currSchema);
        }
    }

    void checkForDuplicateNames(String qName, int declType, Element currComp) {
        int namespaceEnd = qName.indexOf(44);
        String namespace = qName.substring(0, namespaceEnd);
        SchemaGrammar grammar = this.fGrammarBucket.getGrammar(emptyString2Null(namespace));
        if (grammar != null) {
            Object obj = getGlobalDeclFromGrammar(grammar, declType, qName.substring(namespaceEnd + 1));
            if (obj != null) {
                reportSchemaError("sch-props-correct.2", new Object[]{qName}, currComp);
            }
        }
    }

    private void renameRedefiningComponents(XSDocumentInfo currSchema, Element child, String componentType, String oldName, String newName) {
        StringBuilder sb;
        StringBuilder sb2;
        if (componentType.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
            Element grandKid = DOMUtil.getFirstChildElement(child);
            if (grandKid == null) {
                reportSchemaError("src-redefine.5.a.a", null, child);
                return;
            }
            if (DOMUtil.getLocalName(grandKid).equals(SchemaSymbols.ELT_ANNOTATION)) {
                grandKid = DOMUtil.getNextSiblingElement(grandKid);
            }
            if (grandKid == null) {
                reportSchemaError("src-redefine.5.a.a", null, child);
                return;
            }
            String grandKidName = DOMUtil.getLocalName(grandKid);
            if (!grandKidName.equals(SchemaSymbols.ELT_RESTRICTION)) {
                reportSchemaError("src-redefine.5.a.b", new Object[]{grandKidName}, child);
                return;
            }
            Object[] attrs = this.fAttributeChecker.checkAttributes(grandKid, false, currSchema);
            QName derivedBase = (QName) attrs[XSAttributeChecker.ATTIDX_BASE];
            if (derivedBase == null || derivedBase.uri != currSchema.fTargetNamespace || !derivedBase.localpart.equals(oldName)) {
                Object[] objArr = new Object[2];
                objArr[0] = grandKidName;
                objArr[1] = String.valueOf(currSchema.fTargetNamespace == null ? "" : currSchema.fTargetNamespace) + "," + oldName;
                reportSchemaError("src-redefine.5.a.c", objArr, child);
            } else if (derivedBase.prefix == null || derivedBase.prefix.length() <= 0) {
                grandKid.setAttribute(SchemaSymbols.ATT_BASE, newName);
            } else {
                grandKid.setAttribute(SchemaSymbols.ATT_BASE, String.valueOf(derivedBase.prefix) + ":" + newName);
            }
            this.fAttributeChecker.returnAttrArray(attrs, currSchema);
            return;
        }
        if (componentType.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
            Element grandKid2 = DOMUtil.getFirstChildElement(child);
            if (grandKid2 == null) {
                reportSchemaError("src-redefine.5.b.a", null, child);
                return;
            }
            if (DOMUtil.getLocalName(grandKid2).equals(SchemaSymbols.ELT_ANNOTATION)) {
                grandKid2 = DOMUtil.getNextSiblingElement(grandKid2);
            }
            if (grandKid2 == null) {
                reportSchemaError("src-redefine.5.b.a", null, child);
                return;
            }
            Element greatGrandKid = DOMUtil.getFirstChildElement(grandKid2);
            if (greatGrandKid == null) {
                reportSchemaError("src-redefine.5.b.b", null, grandKid2);
                return;
            }
            if (DOMUtil.getLocalName(greatGrandKid).equals(SchemaSymbols.ELT_ANNOTATION)) {
                greatGrandKid = DOMUtil.getNextSiblingElement(greatGrandKid);
            }
            if (greatGrandKid == null) {
                reportSchemaError("src-redefine.5.b.b", null, grandKid2);
                return;
            }
            String greatGrandKidName = DOMUtil.getLocalName(greatGrandKid);
            if (!greatGrandKidName.equals(SchemaSymbols.ELT_RESTRICTION) && !greatGrandKidName.equals(SchemaSymbols.ELT_EXTENSION)) {
                reportSchemaError("src-redefine.5.b.c", new Object[]{greatGrandKidName}, greatGrandKid);
                return;
            }
            QName derivedBase2 = (QName) this.fAttributeChecker.checkAttributes(greatGrandKid, false, currSchema)[XSAttributeChecker.ATTIDX_BASE];
            if (derivedBase2 == null || derivedBase2.uri != currSchema.fTargetNamespace || !derivedBase2.localpart.equals(oldName)) {
                Object[] objArr2 = new Object[2];
                objArr2[0] = greatGrandKidName;
                objArr2[1] = String.valueOf(currSchema.fTargetNamespace == null ? "" : currSchema.fTargetNamespace) + "," + oldName;
                reportSchemaError("src-redefine.5.b.d", objArr2, greatGrandKid);
                return;
            }
            if (derivedBase2.prefix != null && derivedBase2.prefix.length() > 0) {
                greatGrandKid.setAttribute(SchemaSymbols.ATT_BASE, String.valueOf(derivedBase2.prefix) + ":" + newName);
                return;
            }
            greatGrandKid.setAttribute(SchemaSymbols.ATT_BASE, newName);
            return;
        }
        if (componentType.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
            if (currSchema.fTargetNamespace == null) {
                sb2 = new StringBuilder(",");
            } else {
                sb2 = new StringBuilder(String.valueOf(currSchema.fTargetNamespace));
                sb2.append(",");
            }
            sb2.append(oldName);
            String processedBaseName = sb2.toString();
            int attGroupRefsCount = changeRedefineGroup(processedBaseName, componentType, newName, child, currSchema);
            if (attGroupRefsCount > 1) {
                reportSchemaError("src-redefine.7.1", new Object[]{new Integer(attGroupRefsCount)}, child);
                return;
            }
            if (attGroupRefsCount != 1) {
                if (currSchema.fTargetNamespace == null) {
                    this.fRedefinedRestrictedAttributeGroupRegistry.put(processedBaseName, "," + newName);
                    return;
                }
                this.fRedefinedRestrictedAttributeGroupRegistry.put(processedBaseName, String.valueOf(currSchema.fTargetNamespace) + "," + newName);
                return;
            }
            return;
        }
        if (componentType.equals(SchemaSymbols.ELT_GROUP)) {
            if (currSchema.fTargetNamespace == null) {
                sb = new StringBuilder(",");
            } else {
                sb = new StringBuilder(String.valueOf(currSchema.fTargetNamespace));
                sb.append(",");
            }
            sb.append(oldName);
            String processedBaseName2 = sb.toString();
            int groupRefsCount = changeRedefineGroup(processedBaseName2, componentType, newName, child, currSchema);
            if (groupRefsCount > 1) {
                reportSchemaError("src-redefine.6.1.1", new Object[]{new Integer(groupRefsCount)}, child);
                return;
            }
            if (groupRefsCount != 1) {
                if (currSchema.fTargetNamespace == null) {
                    this.fRedefinedRestrictedGroupRegistry.put(processedBaseName2, "," + newName);
                    return;
                }
                this.fRedefinedRestrictedGroupRegistry.put(processedBaseName2, String.valueOf(currSchema.fTargetNamespace) + "," + newName);
                return;
            }
            return;
        }
        reportSchemaError("Internal-Error", new Object[]{"could not handle this particular <redefine>; please submit your schemas and instance document in a bug report!"}, child);
    }

    private String findQName(String name, XSDocumentInfo schemaDoc) {
        SchemaNamespaceSupport currNSMap = schemaDoc.fNamespaceSupport;
        int colonPtr = name.indexOf(58);
        String prefix = XMLSymbols.EMPTY_STRING;
        if (colonPtr > 0) {
            prefix = name.substring(0, colonPtr);
        }
        String uri = currNSMap.getURI(this.fSymbolTable.addSymbol(prefix));
        String localpart = colonPtr == 0 ? name : name.substring(colonPtr + 1);
        if (prefix == XMLSymbols.EMPTY_STRING && uri == null && schemaDoc.fIsChameleonSchema) {
            uri = schemaDoc.fTargetNamespace;
        }
        if (uri == null) {
            return "," + localpart;
        }
        return String.valueOf(uri) + "," + localpart;
    }

    private int changeRedefineGroup(String originalQName, String elementSought, String newName, Element curr, XSDocumentInfo schemaDoc) {
        Element child = DOMUtil.getFirstChildElement(curr);
        int result = 0;
        for (Element child2 = child; child2 != null; child2 = DOMUtil.getNextSiblingElement(child2)) {
            String name = DOMUtil.getLocalName(child2);
            if (!name.equals(elementSought)) {
                result += changeRedefineGroup(originalQName, elementSought, newName, child2, schemaDoc);
            } else {
                String ref = child2.getAttribute(SchemaSymbols.ATT_REF);
                if (ref.length() != 0) {
                    String processedRef = findQName(ref, schemaDoc);
                    if (originalQName.equals(processedRef)) {
                        String str = XMLSymbols.EMPTY_STRING;
                        int colonptr = ref.indexOf(":");
                        if (colonptr > 0) {
                            String prefix = ref.substring(0, colonptr);
                            child2.setAttribute(SchemaSymbols.ATT_REF, String.valueOf(prefix) + ":" + newName);
                        } else {
                            child2.setAttribute(SchemaSymbols.ATT_REF, newName);
                        }
                        result++;
                        if (elementSought.equals(SchemaSymbols.ELT_GROUP)) {
                            String minOccurs = child2.getAttribute(SchemaSymbols.ATT_MINOCCURS);
                            String maxOccurs = child2.getAttribute(SchemaSymbols.ATT_MAXOCCURS);
                            if ((maxOccurs.length() != 0 && !maxOccurs.equals(SchemaSymbols.ATTVAL_TRUE_1)) || (minOccurs.length() != 0 && !minOccurs.equals(SchemaSymbols.ATTVAL_TRUE_1))) {
                                reportSchemaError("src-redefine.6.1.2", new Object[]{ref}, child2);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private XSDocumentInfo findXSDocumentForDecl(XSDocumentInfo currSchema, Element decl, XSDocumentInfo decl_Doc) {
        if (decl_Doc == null) {
            return null;
        }
        return decl_Doc;
    }

    private boolean nonAnnotationContent(Element elem) {
        for (Element child = DOMUtil.getFirstChildElement(elem); child != null; child = DOMUtil.getNextSiblingElement(child)) {
            if (!DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }

    private void setSchemasVisible(XSDocumentInfo startSchema) {
        if (DOMUtil.isHidden(startSchema.fSchemaElement, this.fHiddenNodes)) {
            DOMUtil.setVisible(startSchema.fSchemaElement, this.fHiddenNodes);
            Vector dependingSchemas = (Vector) this.fDependencyMap.get(startSchema);
            for (int i = 0; i < dependingSchemas.size(); i++) {
                setSchemasVisible((XSDocumentInfo) dependingSchemas.elementAt(i));
            }
        }
    }

    public SimpleLocator element2Locator(Element e) {
        if (!(e instanceof ElementImpl)) {
            return null;
        }
        SimpleLocator l = new SimpleLocator();
        if (element2Locator(e, l)) {
            return l;
        }
        return null;
    }

    public boolean element2Locator(Element element, SimpleLocator l) {
        if (l == null || !(element instanceof ElementImpl)) {
            return false;
        }
        Document doc = element.getOwnerDocument();
        String sid = (String) this.fDoc2SystemId.get(DOMUtil.getRoot(doc));
        int line = element.getLineNumber();
        int column = element.getColumnNumber();
        l.setValues(sid, sid, line, column, element.getCharacterOffset());
        return true;
    }

    void reportSchemaError(String key, Object[] args, Element ele) {
        reportSchemaError(key, args, ele, null);
    }

    void reportSchemaError(String key, Object[] args, Element ele, Exception exception) {
        if (element2Locator(ele, this.xl)) {
            this.fErrorReporter.reportError(this.xl, XSMessageFormatter.SCHEMA_DOMAIN, key, args, (short) 1, exception);
        } else {
            this.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, key, args, (short) 1, exception);
        }
    }

    void reportSchemaWarning(String key, Object[] args, Element ele) {
        reportSchemaWarning(key, args, ele, null);
    }

    void reportSchemaWarning(String key, Object[] args, Element ele, Exception exception) {
        if (element2Locator(ele, this.xl)) {
            this.fErrorReporter.reportError(this.xl, XSMessageFormatter.SCHEMA_DOMAIN, key, args, (short) 0, exception);
        } else {
            this.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, key, args, (short) 0, exception);
        }
    }

    private static class XSAnnotationGrammarPool implements XMLGrammarPool {
        private XSGrammarBucket fGrammarBucket;
        private Grammar[] fInitialGrammarSet;

        private XSAnnotationGrammarPool() {
        }

        XSAnnotationGrammarPool(XSAnnotationGrammarPool xSAnnotationGrammarPool) {
            this();
        }

        @Override
        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            if (grammarType == "http://www.w3.org/2001/XMLSchema") {
                if (this.fInitialGrammarSet == null) {
                    if (this.fGrammarBucket == null) {
                        this.fInitialGrammarSet = new Grammar[]{SchemaGrammar.Schema4Annotations.INSTANCE};
                    } else {
                        SchemaGrammar[] schemaGrammars = this.fGrammarBucket.getGrammars();
                        for (SchemaGrammar schemaGrammar : schemaGrammars) {
                            if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(schemaGrammar.getTargetNamespace())) {
                                this.fInitialGrammarSet = schemaGrammars;
                                return this.fInitialGrammarSet;
                            }
                        }
                        int i = schemaGrammars.length;
                        Grammar[] grammars = new Grammar[i + 1];
                        System.arraycopy(schemaGrammars, 0, grammars, 0, schemaGrammars.length);
                        grammars[grammars.length - 1] = SchemaGrammar.Schema4Annotations.INSTANCE;
                        this.fInitialGrammarSet = grammars;
                    }
                }
                return this.fInitialGrammarSet;
            }
            return new Grammar[0];
        }

        @Override
        public void cacheGrammars(String grammarType, Grammar[] grammars) {
        }

        @Override
        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            Grammar grammar;
            if (desc.getGrammarType() == "http://www.w3.org/2001/XMLSchema") {
                String tns = ((XMLSchemaDescription) desc).getTargetNamespace();
                if (this.fGrammarBucket != null && (grammar = this.fGrammarBucket.getGrammar(tns)) != null) {
                    return grammar;
                }
                if (SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(tns)) {
                    return SchemaGrammar.Schema4Annotations.INSTANCE;
                }
                return null;
            }
            return null;
        }

        public void refreshGrammars(XSGrammarBucket gBucket) {
            this.fGrammarBucket = gBucket;
            this.fInitialGrammarSet = null;
        }

        @Override
        public void lockPool() {
        }

        @Override
        public void unlockPool() {
        }

        @Override
        public void clear() {
        }
    }

    private static class XSDKey {
        String referNS;
        short referType;
        String systemId;

        XSDKey(String systemId, short referType, String referNS) {
            this.systemId = systemId;
            this.referType = referType;
            this.referNS = referNS;
        }

        public int hashCode() {
            if (this.referNS == null) {
                return 0;
            }
            return this.referNS.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof XSDKey)) {
                return false;
            }
            XSDKey key = (XSDKey) obj;
            return this.referNS == key.referNS && this.systemId != null && this.systemId.equals(key.systemId);
        }
    }

    private static final class SAX2XNIUtil extends ErrorHandlerWrapper {
        private SAX2XNIUtil() {
        }

        public static XMLParseException createXMLParseException0(SAXParseException exception) {
            return createXMLParseException(exception);
        }

        public static XNIException createXNIException0(SAXException exception) {
            return createXNIException(exception);
        }
    }

    public void setGenerateSyntheticAnnotations(boolean state) {
        this.fSchemaParser.setFeature("http://apache.org/xml/features/generate-synthetic-annotations", state);
    }
}
