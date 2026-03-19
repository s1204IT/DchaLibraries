package mf.org.apache.xerces.impl.xs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import mf.org.apache.xerces.dom.DOMErrorImpl;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.dom.DOMStringListImpl;
import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.XMLEntityManager;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.SchemaDVFactory;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.xs.SchemaDVFactoryImpl;
import mf.org.apache.xerces.impl.xs.models.CMBuilder;
import mf.org.apache.xerces.impl.xs.models.CMNodeFactory;
import mf.org.apache.xerces.impl.xs.traversers.XSDHandler;
import mf.org.apache.xerces.util.DOMEntityResolverWrapper;
import mf.org.apache.xerces.util.DOMErrorHandlerWrapper;
import mf.org.apache.xerces.util.DefaultErrorHandler;
import mf.org.apache.xerces.util.MessageFormatter;
import mf.org.apache.xerces.util.ParserConfigurationSettings;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.URI;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarLoader;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;
import mf.org.apache.xerces.xni.grammars.XSGrammar;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xs.LSInputList;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSLoader;
import mf.org.apache.xerces.xs.XSModel;
import mf.org.w3c.dom.DOMConfiguration;
import mf.org.w3c.dom.DOMErrorHandler;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMStringList;
import mf.org.w3c.dom.ls.LSInput;
import mf.org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;

public class XMLSchemaLoader implements XSElementDeclHelper, XMLGrammarLoader, XMLComponent, XSLoader, DOMConfiguration {
    public static final String ENTITY_RESOLVER = "http://apache.org/xml/properties/internal/entity-resolver";
    public static final String ERROR_REPORTER = "http://apache.org/xml/properties/internal/error-reporter";
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS = "http://apache.org/xml/features/generate-synthetic-annotations";
    protected static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    protected static final String LOCALE = "http://apache.org/xml/properties/locale";
    protected static final String PARSER_SETTINGS = "http://apache.org/xml/features/internal/parser-settings";
    public static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    public static final String XMLGRAMMAR_POOL = "http://apache.org/xml/properties/internal/grammar-pool";
    private CMBuilder fCMBuilder;
    private XSDeclarationPool fDeclPool;
    private SchemaDVFactory fDefaultSchemaDVFactory;
    private XMLEntityManager fEntityManager;
    private DOMErrorHandlerWrapper fErrorHandler;
    private XMLErrorReporter fErrorReporter;
    private String fExternalNoNSSchema;
    private String fExternalSchemas;
    private XSGrammarBucket fGrammarBucket;
    private XMLGrammarPool fGrammarPool;
    private boolean fIsCheckedFully;
    private WeakHashMap fJAXPCache;
    private boolean fJAXPProcessed;
    private Object fJAXPSource;
    private final ParserConfigurationSettings fLoaderConfig;
    private Locale fLocale;
    private DOMStringList fRecognizedParameters;
    private DOMEntityResolverWrapper fResourceResolver;
    private XSDHandler fSchemaHandler;
    private boolean fSettingsChanged;
    private SubstitutionGroupHandler fSubGroupHandler;
    private XMLEntityResolver fUserEntityResolver;
    private XSDDescription fXSDDescription;
    protected static final String SCHEMA_FULL_CHECKING = "http://apache.org/xml/features/validation/schema-full-checking";
    protected static final String AUGMENT_PSVI = "http://apache.org/xml/features/validation/schema/augment-psvi";
    protected static final String CONTINUE_AFTER_FATAL_ERROR = "http://apache.org/xml/features/continue-after-fatal-error";
    protected static final String ALLOW_JAVA_ENCODINGS = "http://apache.org/xml/features/allow-java-encodings";
    protected static final String STANDARD_URI_CONFORMANT_FEATURE = "http://apache.org/xml/features/standard-uri-conformant";
    protected static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    protected static final String VALIDATE_ANNOTATIONS = "http://apache.org/xml/features/validate-annotations";
    protected static final String HONOUR_ALL_SCHEMALOCATIONS = "http://apache.org/xml/features/honour-all-schemaLocations";
    protected static final String NAMESPACE_GROWTH = "http://apache.org/xml/features/namespace-growth";
    protected static final String TOLERATE_DUPLICATES = "http://apache.org/xml/features/internal/tolerate-duplicates";
    private static final String[] RECOGNIZED_FEATURES = {SCHEMA_FULL_CHECKING, AUGMENT_PSVI, CONTINUE_AFTER_FATAL_ERROR, ALLOW_JAVA_ENCODINGS, STANDARD_URI_CONFORMANT_FEATURE, DISALLOW_DOCTYPE, "http://apache.org/xml/features/generate-synthetic-annotations", VALIDATE_ANNOTATIONS, HONOUR_ALL_SCHEMALOCATIONS, NAMESPACE_GROWTH, TOLERATE_DUPLICATES};
    protected static final String ENTITY_MANAGER = "http://apache.org/xml/properties/internal/entity-manager";
    protected static final String ERROR_HANDLER = "http://apache.org/xml/properties/internal/error-handler";
    protected static final String SCHEMA_LOCATION = "http://apache.org/xml/properties/schema/external-schemaLocation";
    protected static final String SCHEMA_NONS_LOCATION = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    protected static final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";
    protected static final String SCHEMA_DV_FACTORY = "http://apache.org/xml/properties/internal/validation/schema/dv-factory";
    private static final String[] RECOGNIZED_PROPERTIES = {ENTITY_MANAGER, "http://apache.org/xml/properties/internal/symbol-table", "http://apache.org/xml/properties/internal/error-reporter", ERROR_HANDLER, "http://apache.org/xml/properties/internal/entity-resolver", "http://apache.org/xml/properties/internal/grammar-pool", SCHEMA_LOCATION, SCHEMA_NONS_LOCATION, "http://java.sun.com/xml/jaxp/properties/schemaSource", SECURITY_MANAGER, "http://apache.org/xml/properties/locale", SCHEMA_DV_FACTORY};

    public XMLSchemaLoader() {
        this(new SymbolTable(), null, new XMLEntityManager(), null, null, null);
    }

    public XMLSchemaLoader(SymbolTable symbolTable) {
        this(symbolTable, null, new XMLEntityManager(), null, null, null);
    }

    XMLSchemaLoader(XMLErrorReporter errorReporter, XSGrammarBucket grammarBucket, SubstitutionGroupHandler sHandler, CMBuilder builder) {
        this(null, errorReporter, null, grammarBucket, sHandler, builder);
    }

    XMLSchemaLoader(SymbolTable symbolTable, XMLErrorReporter errorReporter, XMLEntityManager entityResolver, XSGrammarBucket grammarBucket, SubstitutionGroupHandler sHandler, CMBuilder builder) {
        this.fLoaderConfig = new ParserConfigurationSettings();
        this.fErrorReporter = new XMLErrorReporter();
        this.fEntityManager = null;
        this.fUserEntityResolver = null;
        this.fGrammarPool = null;
        this.fExternalSchemas = null;
        this.fExternalNoNSSchema = null;
        this.fJAXPSource = null;
        this.fIsCheckedFully = false;
        this.fJAXPProcessed = false;
        this.fSettingsChanged = true;
        this.fDeclPool = null;
        this.fXSDDescription = new XSDDescription();
        this.fLocale = Locale.getDefault();
        this.fRecognizedParameters = null;
        this.fErrorHandler = null;
        this.fResourceResolver = null;
        this.fLoaderConfig.addRecognizedFeatures(RECOGNIZED_FEATURES);
        this.fLoaderConfig.addRecognizedProperties(RECOGNIZED_PROPERTIES);
        if (symbolTable != null) {
            this.fLoaderConfig.setProperty("http://apache.org/xml/properties/internal/symbol-table", symbolTable);
        }
        if (errorReporter == null) {
            errorReporter = new XMLErrorReporter();
            errorReporter.setLocale(this.fLocale);
            errorReporter.setProperty(ERROR_HANDLER, new DefaultErrorHandler());
        }
        this.fErrorReporter = errorReporter;
        if (this.fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN) == null) {
            this.fErrorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, new XSMessageFormatter());
        }
        this.fLoaderConfig.setProperty("http://apache.org/xml/properties/internal/error-reporter", this.fErrorReporter);
        this.fEntityManager = entityResolver;
        if (this.fEntityManager != null) {
            this.fLoaderConfig.setProperty(ENTITY_MANAGER, this.fEntityManager);
        }
        this.fLoaderConfig.setFeature(AUGMENT_PSVI, true);
        this.fGrammarBucket = grammarBucket == null ? new XSGrammarBucket() : grammarBucket;
        this.fSubGroupHandler = sHandler == null ? new SubstitutionGroupHandler(this) : sHandler;
        CMNodeFactory nodeFactory = new CMNodeFactory();
        this.fCMBuilder = builder == null ? new CMBuilder(nodeFactory) : builder;
        System.out.println("flag1");
        this.fSchemaHandler = new XSDHandler(this.fGrammarBucket);
        this.fJAXPCache = new WeakHashMap();
        this.fSettingsChanged = true;
    }

    @Override
    public String[] getRecognizedFeatures() {
        return (String[]) RECOGNIZED_FEATURES.clone();
    }

    @Override
    public boolean getFeature(String featureId) throws XMLConfigurationException {
        return this.fLoaderConfig.getFeature(featureId);
    }

    @Override
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
        this.fSettingsChanged = true;
        if (featureId.equals(CONTINUE_AFTER_FATAL_ERROR)) {
            this.fErrorReporter.setFeature(CONTINUE_AFTER_FATAL_ERROR, state);
        } else if (featureId.equals("http://apache.org/xml/features/generate-synthetic-annotations")) {
            this.fSchemaHandler.setGenerateSyntheticAnnotations(state);
        }
        this.fLoaderConfig.setFeature(featureId, state);
    }

    @Override
    public String[] getRecognizedProperties() {
        return (String[]) RECOGNIZED_PROPERTIES.clone();
    }

    @Override
    public Object getProperty(String propertyId) throws XMLConfigurationException {
        return this.fLoaderConfig.getProperty(propertyId);
    }

    @Override
    public void setProperty(String propertyId, Object state) throws XMLConfigurationException {
        this.fSettingsChanged = true;
        this.fLoaderConfig.setProperty(propertyId, state);
        if (propertyId.equals("http://java.sun.com/xml/jaxp/properties/schemaSource")) {
            this.fJAXPSource = state;
            this.fJAXPProcessed = false;
            return;
        }
        if (propertyId.equals("http://apache.org/xml/properties/internal/grammar-pool")) {
            this.fGrammarPool = (XMLGrammarPool) state;
            return;
        }
        if (propertyId.equals(SCHEMA_LOCATION)) {
            this.fExternalSchemas = (String) state;
            return;
        }
        if (propertyId.equals(SCHEMA_NONS_LOCATION)) {
            this.fExternalNoNSSchema = (String) state;
            return;
        }
        if (propertyId.equals("http://apache.org/xml/properties/locale")) {
            setLocale((Locale) state);
            return;
        }
        if (propertyId.equals("http://apache.org/xml/properties/internal/entity-resolver")) {
            this.fEntityManager.setProperty("http://apache.org/xml/properties/internal/entity-resolver", state);
        } else if (propertyId.equals("http://apache.org/xml/properties/internal/error-reporter")) {
            this.fErrorReporter = (XMLErrorReporter) state;
            if (this.fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN) == null) {
                this.fErrorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, new XSMessageFormatter());
            }
        }
    }

    @Override
    public void setLocale(Locale locale) {
        this.fLocale = locale;
        this.fErrorReporter.setLocale(locale);
    }

    @Override
    public Locale getLocale() {
        return this.fLocale;
    }

    @Override
    public void setErrorHandler(XMLErrorHandler errorHandler) {
        this.fErrorReporter.setProperty(ERROR_HANDLER, errorHandler);
    }

    @Override
    public XMLErrorHandler getErrorHandler() {
        return this.fErrorReporter.getErrorHandler();
    }

    @Override
    public void setEntityResolver(XMLEntityResolver entityResolver) {
        this.fUserEntityResolver = entityResolver;
        this.fLoaderConfig.setProperty("http://apache.org/xml/properties/internal/entity-resolver", entityResolver);
        this.fEntityManager.setProperty("http://apache.org/xml/properties/internal/entity-resolver", entityResolver);
    }

    @Override
    public XMLEntityResolver getEntityResolver() {
        return this.fUserEntityResolver;
    }

    public void loadGrammar(XMLInputSource[] source) throws IOException, XNIException {
        for (XMLInputSource xMLInputSource : source) {
            loadGrammar(xMLInputSource);
        }
    }

    @Override
    public Grammar loadGrammar(XMLInputSource source) throws IOException, XNIException {
        reset(this.fLoaderConfig);
        this.fSettingsChanged = false;
        XSDDescription desc = new XSDDescription();
        desc.fContextType = (short) 3;
        desc.setBaseSystemId(source.getBaseSystemId());
        desc.setLiteralSystemId(source.getSystemId());
        Hashtable locationPairs = new Hashtable();
        processExternalHints(this.fExternalSchemas, this.fExternalNoNSSchema, locationPairs, this.fErrorReporter);
        SchemaGrammar grammar = loadSchema(desc, source, locationPairs);
        if (grammar != null && this.fGrammarPool != null) {
            this.fGrammarPool.cacheGrammars("http://www.w3.org/2001/XMLSchema", this.fGrammarBucket.getGrammars());
            if (this.fIsCheckedFully && this.fJAXPCache.get(grammar) != grammar) {
                XSConstraints.fullSchemaChecking(this.fGrammarBucket, this.fSubGroupHandler, this.fCMBuilder, this.fErrorReporter);
            }
        }
        return grammar;
    }

    SchemaGrammar loadSchema(XSDDescription desc, XMLInputSource source, Hashtable locationPairs) throws IOException, XNIException {
        if (!this.fJAXPProcessed) {
            processJAXPSchemaSource(locationPairs);
        }
        SchemaGrammar grammar = this.fSchemaHandler.parseSchema(source, desc, locationPairs);
        return grammar;
    }

    public static XMLInputSource resolveDocument(XSDDescription desc, Hashtable locationPairs, XMLEntityResolver entityResolver) throws IOException {
        String[] hints;
        String loc = null;
        if (desc.getContextType() == 2 || desc.fromInstance()) {
            String namespace = desc.getTargetNamespace();
            String ns = namespace == null ? XMLSymbols.EMPTY_STRING : namespace;
            LocationArray tempLA = (LocationArray) locationPairs.get(ns);
            if (tempLA != null) {
                loc = tempLA.getFirstLocation();
            }
        }
        if (loc == null && (hints = desc.getLocationHints()) != null && hints.length > 0) {
            loc = hints[0];
        }
        String expandedLoc = XMLEntityManager.expandSystemId(loc, desc.getBaseSystemId(), false);
        desc.setLiteralSystemId(loc);
        desc.setExpandedSystemId(expandedLoc);
        return entityResolver.resolveEntity(desc);
    }

    public static void processExternalHints(String sl, String nsl, Hashtable locations, XMLErrorReporter er) {
        if (sl != null) {
            try {
                XSAttributeDecl attrDecl = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_SCHEMALOCATION);
                attrDecl.fType.validate(sl, (ValidationContext) null, (ValidatedInfo) null);
                if (!tokenizeSchemaLocationStr(sl, locations, null)) {
                    er.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "SchemaLocation", new Object[]{sl}, (short) 0);
                }
            } catch (InvalidDatatypeValueException ex) {
                er.reportError(XSMessageFormatter.SCHEMA_DOMAIN, ex.getKey(), ex.getArgs(), (short) 0);
            }
        }
        if (nsl != null) {
            try {
                XSAttributeDecl attrDecl2 = SchemaGrammar.SG_XSI.getGlobalAttributeDecl(SchemaSymbols.XSI_NONAMESPACESCHEMALOCATION);
                attrDecl2.fType.validate(nsl, (ValidationContext) null, (ValidatedInfo) null);
                LocationArray la = (LocationArray) locations.get(XMLSymbols.EMPTY_STRING);
                if (la == null) {
                    la = new LocationArray();
                    locations.put(XMLSymbols.EMPTY_STRING, la);
                }
                la.addLocation(nsl);
            } catch (InvalidDatatypeValueException ex2) {
                er.reportError(XSMessageFormatter.SCHEMA_DOMAIN, ex2.getKey(), ex2.getArgs(), (short) 0);
            }
        }
    }

    public static boolean tokenizeSchemaLocationStr(String schemaStr, Hashtable locations, String base) throws URI.MalformedURIException {
        if (schemaStr != null) {
            StringTokenizer t = new StringTokenizer(schemaStr, " \n\t\r");
            while (t.hasMoreTokens()) {
                String namespace = t.nextToken();
                if (!t.hasMoreTokens()) {
                    return false;
                }
                String location = t.nextToken();
                LocationArray la = (LocationArray) locations.get(namespace);
                if (la == null) {
                    la = new LocationArray();
                    locations.put(namespace, la);
                }
                if (base != null) {
                    try {
                        location = XMLEntityManager.expandSystemId(location, base, false);
                    } catch (URI.MalformedURIException e) {
                    }
                }
                la.addLocation(location);
            }
            return true;
        }
        return true;
    }

    private void processJAXPSchemaSource(Hashtable locationPairs) throws IOException {
        SchemaGrammar g;
        SchemaGrammar g2;
        int i = 1;
        this.fJAXPProcessed = true;
        if (this.fJAXPSource == null) {
            return;
        }
        Class<?> componentType = this.fJAXPSource.getClass().getComponentType();
        if (componentType == null) {
            if (((this.fJAXPSource instanceof InputStream) || (this.fJAXPSource instanceof InputSource)) && (g2 = (SchemaGrammar) this.fJAXPCache.get(this.fJAXPSource)) != null) {
                this.fGrammarBucket.putGrammar(g2);
                return;
            }
            this.fXSDDescription.reset();
            XMLInputSource xis = xsdToXMLInputSource(this.fJAXPSource);
            String sid = xis.getSystemId();
            this.fXSDDescription.fContextType = (short) 3;
            if (sid != null) {
                this.fXSDDescription.setBaseSystemId(xis.getBaseSystemId());
                this.fXSDDescription.setLiteralSystemId(sid);
                this.fXSDDescription.setExpandedSystemId(sid);
                this.fXSDDescription.fLocationHints = new String[]{sid};
            }
            SchemaGrammar g3 = loadSchema(this.fXSDDescription, xis, locationPairs);
            if (g3 != null) {
                if ((this.fJAXPSource instanceof InputStream) || (this.fJAXPSource instanceof InputSource)) {
                    this.fJAXPCache.put(this.fJAXPSource, g3);
                    if (this.fIsCheckedFully) {
                        XSConstraints.fullSchemaChecking(this.fGrammarBucket, this.fSubGroupHandler, this.fCMBuilder, this.fErrorReporter);
                    }
                }
                this.fGrammarBucket.putGrammar(g3);
                return;
            }
            return;
        }
        if (componentType != Object.class && componentType != String.class && componentType != File.class && componentType != InputStream.class && componentType != InputSource.class && !File.class.isAssignableFrom(componentType) && !InputStream.class.isAssignableFrom(componentType) && !InputSource.class.isAssignableFrom(componentType) && !componentType.isInterface()) {
            MessageFormatter mf2 = this.fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN);
            throw new XMLConfigurationException((short) 1, mf2.formatMessage(this.fErrorReporter.getLocale(), "jaxp12-schema-source-type.2", new Object[]{componentType.getName()}));
        }
        Object[] objArr = (Object[]) this.fJAXPSource;
        ArrayList jaxpSchemaSourceNamespaces = new ArrayList();
        int i2 = 0;
        while (i2 < objArr.length) {
            if (((objArr[i2] instanceof InputStream) || (objArr[i2] instanceof InputSource)) && (g = (SchemaGrammar) this.fJAXPCache.get(objArr[i2])) != null) {
                this.fGrammarBucket.putGrammar(g);
            } else {
                this.fXSDDescription.reset();
                XMLInputSource xis2 = xsdToXMLInputSource(objArr[i2]);
                String sid2 = xis2.getSystemId();
                this.fXSDDescription.fContextType = (short) 3;
                if (sid2 != null) {
                    this.fXSDDescription.setBaseSystemId(xis2.getBaseSystemId());
                    this.fXSDDescription.setLiteralSystemId(sid2);
                    this.fXSDDescription.setExpandedSystemId(sid2);
                    XSDDescription xSDDescription = this.fXSDDescription;
                    String[] strArr = new String[i];
                    strArr[0] = sid2;
                    xSDDescription.fLocationHints = strArr;
                }
                SchemaGrammar grammar = this.fSchemaHandler.parseSchema(xis2, this.fXSDDescription, locationPairs);
                if (this.fIsCheckedFully) {
                    XSConstraints.fullSchemaChecking(this.fGrammarBucket, this.fSubGroupHandler, this.fCMBuilder, this.fErrorReporter);
                }
                if (grammar != null) {
                    String targetNamespace = grammar.getTargetNamespace();
                    if (jaxpSchemaSourceNamespaces.contains(targetNamespace)) {
                        MessageFormatter mf3 = this.fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN);
                        throw new IllegalArgumentException(mf3.formatMessage(this.fErrorReporter.getLocale(), "jaxp12-schema-source-ns", null));
                    }
                    jaxpSchemaSourceNamespaces.add(targetNamespace);
                    if ((objArr[i2] instanceof InputStream) || (objArr[i2] instanceof InputSource)) {
                        this.fJAXPCache.put(objArr[i2], grammar);
                    }
                    this.fGrammarBucket.putGrammar(grammar);
                } else {
                    continue;
                }
            }
            i2++;
            i = 1;
        }
    }

    private XMLInputSource xsdToXMLInputSource(Object obj) throws IOException {
        InputStream is;
        if (obj instanceof String) {
            String loc = (String) obj;
            this.fXSDDescription.reset();
            this.fXSDDescription.setValues(null, loc, null, null);
            XMLInputSource xis = null;
            try {
                xis = this.fEntityManager.resolveEntity(this.fXSDDescription);
            } catch (IOException e) {
                this.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "schema_reference.4", new Object[]{loc}, (short) 1);
            }
            if (xis == null) {
                return new XMLInputSource(null, loc, null);
            }
            return xis;
        }
        if (obj instanceof InputSource) {
            return saxToXMLInputSource(obj);
        }
        if (obj instanceof InputStream) {
            return new XMLInputSource((String) null, (String) null, (String) null, (InputStream) obj, (String) null);
        }
        if (obj instanceof File) {
            String escapedURI = FilePathToURI.filepath2URI(obj.getAbsolutePath());
            try {
                is = new BufferedInputStream(new FileInputStream((File) obj));
            } catch (FileNotFoundException e2) {
                this.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "schema_reference.4", new Object[]{obj.toString()}, (short) 1);
                is = null;
            }
            return new XMLInputSource((String) null, escapedURI, (String) null, is, (String) null);
        }
        MessageFormatter mf2 = this.fErrorReporter.getMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN);
        Locale locale = this.fErrorReporter.getLocale();
        Object[] objArr = new Object[1];
        objArr[0] = obj != 0 ? obj.getClass().getName() : "null";
        throw new XMLConfigurationException((short) 1, mf2.formatMessage(locale, "jaxp12-schema-source-type.1", objArr));
    }

    private static XMLInputSource saxToXMLInputSource(InputSource sis) {
        String publicId = sis.getPublicId();
        String systemId = sis.getSystemId();
        Reader charStream = sis.getCharacterStream();
        if (charStream != null) {
            return new XMLInputSource(publicId, systemId, (String) null, charStream, (String) null);
        }
        InputStream byteStream = sis.getByteStream();
        if (byteStream != null) {
            return new XMLInputSource(publicId, systemId, (String) null, byteStream, sis.getEncoding());
        }
        return new XMLInputSource(publicId, systemId, null);
    }

    static class LocationArray {
        int length;
        String[] locations = new String[2];

        LocationArray() {
        }

        public void resize(int oldLength, int newLength) {
            String[] temp = new String[newLength];
            System.arraycopy(this.locations, 0, temp, 0, Math.min(oldLength, newLength));
            this.locations = temp;
            this.length = Math.min(oldLength, newLength);
        }

        public void addLocation(String location) {
            if (this.length >= this.locations.length) {
                resize(this.length, Math.max(1, this.length * 2));
            }
            String[] strArr = this.locations;
            int i = this.length;
            this.length = i + 1;
            strArr[i] = location;
        }

        public String[] getLocationArray() {
            if (this.length < this.locations.length) {
                resize(this.locations.length, this.length);
            }
            return this.locations;
        }

        public String getFirstLocation() {
            if (this.length > 0) {
                return this.locations[0];
            }
            return null;
        }

        public int getLength() {
            return this.length;
        }
    }

    @Override
    public Boolean getFeatureDefault(String featureId) {
        if (featureId.equals(AUGMENT_PSVI)) {
            return Boolean.TRUE;
        }
        return null;
    }

    @Override
    public Object getPropertyDefault(String propertyId) {
        return null;
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
        this.fGrammarBucket.reset();
        this.fSubGroupHandler.reset();
        if (!this.fSettingsChanged || !parserSettingsUpdated(componentManager)) {
            this.fJAXPProcessed = false;
            initGrammarBucket();
            if (this.fDeclPool != null) {
                this.fDeclPool.reset();
                return;
            }
            return;
        }
        this.fEntityManager = (XMLEntityManager) componentManager.getProperty(ENTITY_MANAGER);
        this.fErrorReporter = (XMLErrorReporter) componentManager.getProperty("http://apache.org/xml/properties/internal/error-reporter");
        SchemaDVFactory dvFactory = null;
        try {
            dvFactory = (SchemaDVFactory) componentManager.getProperty(SCHEMA_DV_FACTORY);
        } catch (XMLConfigurationException e) {
        }
        if (dvFactory == null) {
            if (this.fDefaultSchemaDVFactory == null) {
                this.fDefaultSchemaDVFactory = SchemaDVFactory.getInstance();
            }
            dvFactory = this.fDefaultSchemaDVFactory;
        }
        this.fSchemaHandler.setDVFactory(dvFactory);
        try {
            this.fExternalSchemas = (String) componentManager.getProperty(SCHEMA_LOCATION);
            this.fExternalNoNSSchema = (String) componentManager.getProperty(SCHEMA_NONS_LOCATION);
        } catch (XMLConfigurationException e2) {
            this.fExternalSchemas = null;
            this.fExternalNoNSSchema = null;
        }
        try {
            this.fJAXPSource = componentManager.getProperty("http://java.sun.com/xml/jaxp/properties/schemaSource");
            this.fJAXPProcessed = false;
        } catch (XMLConfigurationException e3) {
            this.fJAXPSource = null;
            this.fJAXPProcessed = false;
        }
        try {
            this.fGrammarPool = (XMLGrammarPool) componentManager.getProperty("http://apache.org/xml/properties/internal/grammar-pool");
        } catch (XMLConfigurationException e4) {
            this.fGrammarPool = null;
        }
        initGrammarBucket();
        try {
            componentManager.getFeature(AUGMENT_PSVI);
        } catch (XMLConfigurationException e5) {
        }
        this.fCMBuilder.setDeclPool(null);
        this.fSchemaHandler.setDeclPool(null);
        if (dvFactory instanceof SchemaDVFactoryImpl) {
            dvFactory.setDeclPool(null);
        }
        try {
            boolean fatalError = componentManager.getFeature(CONTINUE_AFTER_FATAL_ERROR);
            this.fErrorReporter.setFeature(CONTINUE_AFTER_FATAL_ERROR, fatalError);
        } catch (XMLConfigurationException e6) {
        }
        try {
            this.fIsCheckedFully = componentManager.getFeature(SCHEMA_FULL_CHECKING);
        } catch (XMLConfigurationException e7) {
            this.fIsCheckedFully = false;
        }
        try {
            this.fSchemaHandler.setGenerateSyntheticAnnotations(componentManager.getFeature("http://apache.org/xml/features/generate-synthetic-annotations"));
        } catch (XMLConfigurationException e8) {
            this.fSchemaHandler.setGenerateSyntheticAnnotations(false);
        }
        this.fSchemaHandler.reset(componentManager);
    }

    private boolean parserSettingsUpdated(XMLComponentManager componentManager) {
        if (componentManager != this.fLoaderConfig) {
            try {
                return componentManager.getFeature(PARSER_SETTINGS);
            } catch (XMLConfigurationException e) {
                return true;
            }
        }
        return true;
    }

    private void initGrammarBucket() {
        if (this.fGrammarPool != null) {
            Grammar[] initialGrammars = this.fGrammarPool.retrieveInitialGrammarSet("http://www.w3.org/2001/XMLSchema");
            int length = initialGrammars != null ? initialGrammars.length : 0;
            for (int i = 0; i < length; i++) {
                if (!this.fGrammarBucket.putGrammar((SchemaGrammar) initialGrammars[i], true)) {
                    this.fErrorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, "GrammarConflict", null, (short) 0);
                }
            }
        }
    }

    @Override
    public DOMConfiguration getConfig() {
        return this;
    }

    @Override
    public XSModel load(LSInput is) {
        try {
            Grammar g = loadGrammar(dom2xmlInputSource(is));
            return ((XSGrammar) g).toXSModel();
        } catch (Exception e) {
            reportDOMFatalError(e);
            return null;
        }
    }

    @Override
    public XSModel loadInputList(LSInputList is) {
        int length = is.getLength();
        SchemaGrammar[] gs = new SchemaGrammar[length];
        for (int i = 0; i < length; i++) {
            try {
                gs[i] = (SchemaGrammar) loadGrammar(dom2xmlInputSource(is.item(i)));
            } catch (Exception e) {
                reportDOMFatalError(e);
                return null;
            }
        }
        return new XSModelImpl(gs);
    }

    @Override
    public XSModel loadURI(String uri) {
        try {
            Grammar g = loadGrammar(new XMLInputSource(null, uri, null));
            return ((XSGrammar) g).toXSModel();
        } catch (Exception e) {
            reportDOMFatalError(e);
            return null;
        }
    }

    @Override
    public XSModel loadURIList(StringList uriList) {
        int length = uriList.getLength();
        SchemaGrammar[] gs = new SchemaGrammar[length];
        for (int i = 0; i < length; i++) {
            try {
                gs[i] = (SchemaGrammar) loadGrammar(new XMLInputSource(null, uriList.item(i), null));
            } catch (Exception e) {
                reportDOMFatalError(e);
                return null;
            }
        }
        return new XSModelImpl(gs);
    }

    void reportDOMFatalError(Exception e) {
        if (this.fErrorHandler != null) {
            DOMErrorImpl error = new DOMErrorImpl();
            error.fException = e;
            error.fMessage = e.getMessage();
            error.fSeverity = (short) 3;
            this.fErrorHandler.getErrorHandler().handleError(error);
        }
    }

    public boolean canSetParameter(String name, Object value) {
        return value instanceof Boolean ? name.equals(Constants.DOM_VALIDATE) || name.equals(SCHEMA_FULL_CHECKING) || name.equals(VALIDATE_ANNOTATIONS) || name.equals(CONTINUE_AFTER_FATAL_ERROR) || name.equals(ALLOW_JAVA_ENCODINGS) || name.equals(STANDARD_URI_CONFORMANT_FEATURE) || name.equals("http://apache.org/xml/features/generate-synthetic-annotations") || name.equals(HONOUR_ALL_SCHEMALOCATIONS) || name.equals(NAMESPACE_GROWTH) || name.equals(TOLERATE_DUPLICATES) : name.equals(Constants.DOM_ERROR_HANDLER) || name.equals(Constants.DOM_RESOURCE_RESOLVER) || name.equals("http://apache.org/xml/properties/internal/symbol-table") || name.equals("http://apache.org/xml/properties/internal/error-reporter") || name.equals(ERROR_HANDLER) || name.equals("http://apache.org/xml/properties/internal/entity-resolver") || name.equals("http://apache.org/xml/properties/internal/grammar-pool") || name.equals(SCHEMA_LOCATION) || name.equals(SCHEMA_NONS_LOCATION) || name.equals("http://java.sun.com/xml/jaxp/properties/schemaSource") || name.equals(SCHEMA_DV_FACTORY);
    }

    public Object getParameter(String name) throws DOMException {
        if (name.equals(Constants.DOM_ERROR_HANDLER)) {
            if (this.fErrorHandler != null) {
                return this.fErrorHandler.getErrorHandler();
            }
            return null;
        }
        if (name.equals(Constants.DOM_RESOURCE_RESOLVER)) {
            if (this.fResourceResolver != null) {
                return this.fResourceResolver.getEntityResolver();
            }
            return null;
        }
        try {
            boolean feature = getFeature(name);
            return feature ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception e) {
            try {
                Object property = getProperty(name);
                return property;
            } catch (Exception e2) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "FEATURE_NOT_SUPPORTED", new Object[]{name});
                throw new DOMException((short) 9, msg);
            }
        }
    }

    public DOMStringList getParameterNames() {
        if (this.fRecognizedParameters == null) {
            ArrayList v = new ArrayList();
            v.add(Constants.DOM_VALIDATE);
            v.add(Constants.DOM_ERROR_HANDLER);
            v.add(Constants.DOM_RESOURCE_RESOLVER);
            v.add("http://apache.org/xml/properties/internal/symbol-table");
            v.add("http://apache.org/xml/properties/internal/error-reporter");
            v.add(ERROR_HANDLER);
            v.add("http://apache.org/xml/properties/internal/entity-resolver");
            v.add("http://apache.org/xml/properties/internal/grammar-pool");
            v.add(SCHEMA_LOCATION);
            v.add(SCHEMA_NONS_LOCATION);
            v.add("http://java.sun.com/xml/jaxp/properties/schemaSource");
            v.add(SCHEMA_FULL_CHECKING);
            v.add(CONTINUE_AFTER_FATAL_ERROR);
            v.add(ALLOW_JAVA_ENCODINGS);
            v.add(STANDARD_URI_CONFORMANT_FEATURE);
            v.add(VALIDATE_ANNOTATIONS);
            v.add("http://apache.org/xml/features/generate-synthetic-annotations");
            v.add(HONOUR_ALL_SCHEMALOCATIONS);
            v.add(NAMESPACE_GROWTH);
            v.add(TOLERATE_DUPLICATES);
            this.fRecognizedParameters = new DOMStringListImpl(v);
        }
        return this.fRecognizedParameters;
    }

    public void setParameter(String name, Object value) throws DOMException {
        if (value instanceof Boolean) {
            boolean state = ((Boolean) value).booleanValue();
            if (name.equals(Constants.DOM_VALIDATE) && state) {
                return;
            }
            try {
                setFeature(name, state);
                return;
            } catch (Exception e) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "FEATURE_NOT_SUPPORTED", new Object[]{name});
                throw new DOMException((short) 9, msg);
            }
        }
        if (name.equals(Constants.DOM_ERROR_HANDLER)) {
            if (value instanceof DOMErrorHandler) {
                try {
                    this.fErrorHandler = new DOMErrorHandlerWrapper((DOMErrorHandler) value);
                    setErrorHandler(this.fErrorHandler);
                    return;
                } catch (XMLConfigurationException e2) {
                    return;
                }
            }
            String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "FEATURE_NOT_SUPPORTED", new Object[]{name});
            throw new DOMException((short) 9, msg2);
        }
        if (name.equals(Constants.DOM_RESOURCE_RESOLVER)) {
            if (value instanceof LSResourceResolver) {
                try {
                    this.fResourceResolver = new DOMEntityResolverWrapper((LSResourceResolver) value);
                    setEntityResolver(this.fResourceResolver);
                    return;
                } catch (XMLConfigurationException e3) {
                    return;
                }
            }
            String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "FEATURE_NOT_SUPPORTED", new Object[]{name});
            throw new DOMException((short) 9, msg3);
        }
        try {
            setProperty(name, value);
        } catch (Exception e4) {
            String msg4 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "FEATURE_NOT_SUPPORTED", new Object[]{name});
            throw new DOMException((short) 9, msg4);
        }
    }

    XMLInputSource dom2xmlInputSource(LSInput is) {
        if (is.getCharacterStream() != null) {
            XMLInputSource xis = new XMLInputSource(is.getPublicId(), is.getSystemId(), is.getBaseURI(), is.getCharacterStream(), "UTF-16");
            return xis;
        }
        if (is.getByteStream() != null) {
            XMLInputSource xis2 = new XMLInputSource(is.getPublicId(), is.getSystemId(), is.getBaseURI(), is.getByteStream(), is.getEncoding());
            return xis2;
        }
        if (is.getStringData() != null && is.getStringData().length() != 0) {
            XMLInputSource xis3 = new XMLInputSource(is.getPublicId(), is.getSystemId(), is.getBaseURI(), new StringReader(is.getStringData()), "UTF-16");
            return xis3;
        }
        XMLInputSource xis4 = new XMLInputSource(is.getPublicId(), is.getSystemId(), is.getBaseURI());
        return xis4;
    }

    @Override
    public XSElementDecl getGlobalElementDecl(QName element) {
        SchemaGrammar sGrammar = this.fGrammarBucket.getGrammar(element.uri);
        if (sGrammar != null) {
            return sGrammar.getGlobalElementDecl(element.localpart);
        }
        return null;
    }
}
