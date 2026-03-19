package mf.org.apache.xerces.jaxp.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import mf.javax.xml.stream.XMLEventReader;
import mf.javax.xml.transform.Source;
import mf.javax.xml.transform.dom.DOMSource;
import mf.javax.xml.transform.sax.SAXSource;
import mf.javax.xml.transform.stax.StAXSource;
import mf.javax.xml.transform.stream.StreamSource;
import mf.javax.xml.validation.Schema;
import mf.javax.xml.validation.SchemaFactory;
import mf.org.apache.xerces.impl.xs.XMLSchemaLoader;
import mf.org.apache.xerces.util.DOMEntityResolverWrapper;
import mf.org.apache.xerces.util.DOMInputSource;
import mf.org.apache.xerces.util.ErrorHandlerWrapper;
import mf.org.apache.xerces.util.SAXInputSource;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import mf.org.apache.xerces.util.SecurityManager;
import mf.org.apache.xerces.util.StAXInputSource;
import mf.org.apache.xerces.util.XMLGrammarPoolImpl;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.Grammar;
import mf.org.apache.xerces.xni.grammars.XMLGrammarDescription;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

public final class XMLSchemaFactory extends SchemaFactory {
    private static final String JAXP_SOURCE_FEATURE_PREFIX = "http://javax.xml.transform";
    private static final String SCHEMA_FULL_CHECKING = "http://apache.org/xml/features/validation/schema-full-checking";
    private static final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";
    private static final String USE_GRAMMAR_POOL_ONLY = "http://apache.org/xml/features/internal/validation/schema/use-grammar-pool-only";
    private static final String XMLGRAMMAR_POOL = "http://apache.org/xml/properties/internal/grammar-pool";
    private ErrorHandler fErrorHandler;
    private LSResourceResolver fLSResourceResolver;
    private SecurityManager fSecurityManager;
    private boolean fUseGrammarPoolOnly;
    private final XMLSchemaLoader fXMLSchemaLoader = new XMLSchemaLoader();
    private final ErrorHandlerWrapper fErrorHandlerWrapper = new ErrorHandlerWrapper(DraconianErrorHandler.getInstance());
    private final DOMEntityResolverWrapper fDOMEntityResolverWrapper = new DOMEntityResolverWrapper();
    private final XMLGrammarPoolWrapper fXMLGrammarPoolWrapper = new XMLGrammarPoolWrapper();

    public XMLSchemaFactory() {
        this.fXMLSchemaLoader.setFeature(SCHEMA_FULL_CHECKING, true);
        this.fXMLSchemaLoader.setProperty("http://apache.org/xml/properties/internal/grammar-pool", this.fXMLGrammarPoolWrapper);
        this.fXMLSchemaLoader.setEntityResolver(this.fDOMEntityResolverWrapper);
        this.fXMLSchemaLoader.setErrorHandler(this.fErrorHandlerWrapper);
        this.fUseGrammarPoolOnly = true;
    }

    @Override
    public boolean isSchemaLanguageSupported(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "SchemaLanguageNull", null));
        }
        if (schemaLanguage.length() == 0) {
            throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "SchemaLanguageLengthZero", null));
        }
        return schemaLanguage.equals("http://www.w3.org/2001/XMLSchema");
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return this.fLSResourceResolver;
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        this.fLSResourceResolver = resourceResolver;
        this.fDOMEntityResolverWrapper.setEntityResolver(resourceResolver);
        this.fXMLSchemaLoader.setEntityResolver(this.fDOMEntityResolverWrapper);
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.fErrorHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.fErrorHandler = errorHandler;
        this.fErrorHandlerWrapper.setErrorHandler(errorHandler != null ? errorHandler : DraconianErrorHandler.getInstance());
        this.fXMLSchemaLoader.setErrorHandler(this.fErrorHandlerWrapper);
    }

    @Override
    public Schema newSchema(Source[] sourceArr) throws SAXException {
        AbstractXMLSchema schema;
        XMLGrammarPoolImplExtension pool = new XMLGrammarPoolImplExtension();
        this.fXMLGrammarPoolWrapper.setGrammarPool(pool);
        XMLInputSource[] xmlInputSources = new XMLInputSource[sourceArr.length];
        for (int i = 0; i < sourceArr.length; i++) {
            ?? r3 = sourceArr[i];
            if (r3 instanceof StreamSource) {
                String publicId = r3.getPublicId();
                String systemId = r3.getSystemId();
                InputStream inputStream = r3.getInputStream();
                Reader reader = r3.getReader();
                XMLInputSource xmlInputSource = new XMLInputSource(publicId, systemId, null);
                xmlInputSource.setByteStream(inputStream);
                xmlInputSource.setCharacterStream(reader);
                xmlInputSources[i] = xmlInputSource;
            } else if (r3 instanceof SAXSource) {
                InputSource inputSource = r3.getInputSource();
                if (inputSource == null) {
                    throw new SAXException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "SAXSourceNullInputSource", null));
                }
                xmlInputSources[i] = new SAXInputSource(r3.getXMLReader(), inputSource);
            } else if (r3 instanceof DOMSource) {
                Node node = r3.getNode();
                String systemID = r3.getSystemId();
                xmlInputSources[i] = new DOMInputSource(node, systemID);
            } else if (r3 instanceof StAXSource) {
                XMLEventReader eventReader = r3.getXMLEventReader();
                if (eventReader != null) {
                    xmlInputSources[i] = new StAXInputSource(eventReader);
                } else {
                    xmlInputSources[i] = new StAXInputSource(r3.getXMLStreamReader());
                }
            } else {
                if (r3 == 0) {
                    throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "SchemaSourceArrayMemberNull", null));
                }
                throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "SchemaFactorySourceUnrecognized", new Object[]{r3.getClass().getName()}));
            }
        }
        try {
            this.fXMLSchemaLoader.loadGrammar(xmlInputSources);
            this.fXMLGrammarPoolWrapper.setGrammarPool(null);
            int grammarCount = pool.getGrammarCount();
            if (this.fUseGrammarPoolOnly) {
                if (grammarCount > 1) {
                    schema = new XMLSchema(new ReadOnlyGrammarPool(pool));
                } else if (grammarCount == 1) {
                    Grammar[] grammars = pool.retrieveInitialGrammarSet("http://www.w3.org/2001/XMLSchema");
                    schema = new SimpleXMLSchema(grammars[0]);
                } else {
                    schema = new EmptyXMLSchema();
                }
            } else {
                schema = new XMLSchema(new ReadOnlyGrammarPool(pool), false);
            }
            propagateFeatures(schema);
            return schema;
        } catch (IOException e) {
            SAXParseException se = new SAXParseException(e.getMessage(), null, e);
            if (this.fErrorHandler != null) {
                this.fErrorHandler.error(se);
                throw se;
            }
            throw se;
        } catch (XNIException e2) {
            throw Util.toSAXException(e2);
        }
    }

    @Override
    public Schema newSchema() throws SAXException {
        AbstractXMLSchema schema = new WeakReferenceXMLSchema();
        propagateFeatures(schema);
        return schema;
    }

    public Schema newSchema(XMLGrammarPool pool) throws SAXException {
        AbstractXMLSchema schema;
        if (this.fUseGrammarPoolOnly) {
            schema = new XMLSchema(new ReadOnlyGrammarPool(pool));
        } else {
            schema = new XMLSchema(pool, false);
        }
        propagateFeatures(schema);
        return schema;
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "FeatureNameNull", null));
        }
        if (name.startsWith(JAXP_SOURCE_FEATURE_PREFIX) && (name.equals("http://javax.xml.transform.stream.StreamSource/feature") || name.equals("http://javax.xml.transform.sax.SAXSource/feature") || name.equals("http://javax.xml.transform.dom.DOMSource/feature") || name.equals("http://javax.xml.transform.stax.StAXSource/feature"))) {
            return true;
        }
        if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            return this.fSecurityManager != null;
        }
        if (name.equals(USE_GRAMMAR_POOL_ONLY)) {
            return this.fUseGrammarPoolOnly;
        }
        try {
            return this.fXMLSchemaLoader.getFeature(name);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "ProperyNameNull", null));
        }
        if (name.equals(SECURITY_MANAGER)) {
            return this.fSecurityManager;
        }
        if (name.equals("http://apache.org/xml/properties/internal/grammar-pool")) {
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "property-not-supported", new Object[]{name}));
        }
        try {
            return this.fXMLSchemaLoader.getProperty(name);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "FeatureNameNull", null));
        }
        if (name.startsWith(JAXP_SOURCE_FEATURE_PREFIX) && (name.equals("http://javax.xml.transform.stream.StreamSource/feature") || name.equals("http://javax.xml.transform.sax.SAXSource/feature") || name.equals("http://javax.xml.transform.dom.DOMSource/feature") || name.equals("http://javax.xml.transform.stax.StAXSource/feature"))) {
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "feature-read-only", new Object[]{name}));
        }
        if (name.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            this.fSecurityManager = value ? new SecurityManager() : null;
            this.fXMLSchemaLoader.setProperty(SECURITY_MANAGER, this.fSecurityManager);
        } else {
            if (name.equals(USE_GRAMMAR_POOL_ONLY)) {
                this.fUseGrammarPoolOnly = value;
                return;
            }
            try {
                this.fXMLSchemaLoader.setFeature(name, value);
            } catch (XMLConfigurationException e) {
                String identifier = e.getIdentifier();
                if (e.getType() == 0) {
                    throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "feature-not-recognized", new Object[]{identifier}));
                }
                throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "feature-not-supported", new Object[]{identifier}));
            }
        }
    }

    @Override
    public void setProperty(String name, Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "ProperyNameNull", null));
        }
        if (name.equals(SECURITY_MANAGER)) {
            this.fSecurityManager = (SecurityManager) object;
            this.fXMLSchemaLoader.setProperty(SECURITY_MANAGER, this.fSecurityManager);
        } else {
            if (name.equals("http://apache.org/xml/properties/internal/grammar-pool")) {
                throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "property-not-supported", new Object[]{name}));
            }
            try {
                this.fXMLSchemaLoader.setProperty(name, object);
            } catch (XMLConfigurationException e) {
                String identifier = e.getIdentifier();
                if (e.getType() == 0) {
                    throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "property-not-recognized", new Object[]{identifier}));
                }
                throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fXMLSchemaLoader.getLocale(), "property-not-supported", new Object[]{identifier}));
            }
        }
    }

    private void propagateFeatures(AbstractXMLSchema schema) {
        schema.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", this.fSecurityManager != null);
        String[] features = this.fXMLSchemaLoader.getRecognizedFeatures();
        for (int i = 0; i < features.length; i++) {
            boolean state = this.fXMLSchemaLoader.getFeature(features[i]);
            schema.setFeature(features[i], state);
        }
    }

    static class XMLGrammarPoolImplExtension extends XMLGrammarPoolImpl {
        public XMLGrammarPoolImplExtension() {
        }

        public XMLGrammarPoolImplExtension(int initialCapacity) {
            super(initialCapacity);
        }

        int getGrammarCount() {
            return this.fGrammarCount;
        }
    }

    static class XMLGrammarPoolWrapper implements XMLGrammarPool {
        private XMLGrammarPool fGrammarPool;

        XMLGrammarPoolWrapper() {
        }

        @Override
        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            return this.fGrammarPool.retrieveInitialGrammarSet(grammarType);
        }

        @Override
        public void cacheGrammars(String grammarType, Grammar[] grammars) {
            this.fGrammarPool.cacheGrammars(grammarType, grammars);
        }

        @Override
        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            return this.fGrammarPool.retrieveGrammar(desc);
        }

        @Override
        public void lockPool() {
            this.fGrammarPool.lockPool();
        }

        @Override
        public void unlockPool() {
            this.fGrammarPool.unlockPool();
        }

        @Override
        public void clear() {
            this.fGrammarPool.clear();
        }

        void setGrammarPool(XMLGrammarPool grammarPool) {
            this.fGrammarPool = grammarPool;
        }

        XMLGrammarPool getGrammarPool() {
            return this.fGrammarPool;
        }
    }
}
