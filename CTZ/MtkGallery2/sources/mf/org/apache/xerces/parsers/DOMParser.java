package mf.org.apache.xerces.parsers;

import java.io.CharConversionException;
import java.io.IOException;
import mf.org.apache.xerces.dom.DOMMessageFormatter;
import mf.org.apache.xerces.util.EntityResolver2Wrapper;
import mf.org.apache.xerces.util.EntityResolverWrapper;
import mf.org.apache.xerces.util.ErrorHandlerWrapper;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.XMLGrammarPool;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLEntityResolver;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import mf.org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.helpers.LocatorImpl;

public class DOMParser extends AbstractDOMParser {
    private static final String[] RECOGNIZED_PROPERTIES = {"http://apache.org/xml/properties/internal/symbol-table", "http://apache.org/xml/properties/internal/grammar-pool"};
    protected static final String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
    protected static final String USE_ENTITY_RESOLVER2 = "http://xml.org/sax/features/use-entity-resolver2";
    protected static final String XMLGRAMMAR_POOL = "http://apache.org/xml/properties/internal/grammar-pool";
    protected boolean fUseEntityResolver2;

    public DOMParser(XMLParserConfiguration config) {
        super(config);
        this.fUseEntityResolver2 = true;
    }

    public DOMParser() {
        this(null, null);
    }

    public DOMParser(SymbolTable symbolTable) {
        this(symbolTable, null);
    }

    public DOMParser(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
        super((XMLParserConfiguration) ObjectFactory.createObject("mf.org.apache.xerces.xni.parser.XMLParserConfiguration", "mf.org.apache.xerces.parsers.XIncludeAwareParserConfiguration"));
        this.fUseEntityResolver2 = true;
        this.fConfiguration.addRecognizedProperties(RECOGNIZED_PROPERTIES);
        if (symbolTable != null) {
            this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/symbol-table", symbolTable);
        }
        if (grammarPool != null) {
            this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/grammar-pool", grammarPool);
        }
    }

    public void parse(String systemId) throws Exception {
        XMLInputSource source = new XMLInputSource(null, systemId, null);
        try {
            parse(source);
        } catch (XMLParseException e) {
            Exception ex = e.getException();
            if (ex == null || (ex instanceof CharConversionException)) {
                LocatorImpl locatorImpl = new LocatorImpl();
                locatorImpl.setPublicId(e.getPublicId());
                locatorImpl.setSystemId(e.getExpandedSystemId());
                locatorImpl.setLineNumber(e.getLineNumber());
                locatorImpl.setColumnNumber(e.getColumnNumber());
                if (ex == null) {
                    throw new SAXParseException(e.getMessage(), locatorImpl);
                }
                throw new SAXParseException(e.getMessage(), locatorImpl, ex);
            }
            if (ex instanceof SAXException) {
                throw ex;
            }
            if (ex instanceof IOException) {
                throw ex;
            }
            throw new SAXException(ex);
        } catch (XNIException e2) {
            e2.printStackTrace();
            Exception ex2 = e2.getException();
            if (ex2 == null) {
                throw new SAXException(e2.getMessage());
            }
            if (ex2 instanceof SAXException) {
                throw ex2;
            }
            if (ex2 instanceof IOException) {
                throw ex2;
            }
            throw new SAXException(ex2);
        }
    }

    public void parse(InputSource inputSource) throws Exception {
        try {
            XMLInputSource xmlInputSource = new XMLInputSource(inputSource.getPublicId(), inputSource.getSystemId(), null);
            xmlInputSource.setByteStream(inputSource.getByteStream());
            xmlInputSource.setCharacterStream(inputSource.getCharacterStream());
            xmlInputSource.setEncoding(inputSource.getEncoding());
            parse(xmlInputSource);
        } catch (XMLParseException e) {
            Exception ex = e.getException();
            if (ex == null || (ex instanceof CharConversionException)) {
                LocatorImpl locatorImpl = new LocatorImpl();
                locatorImpl.setPublicId(e.getPublicId());
                locatorImpl.setSystemId(e.getExpandedSystemId());
                locatorImpl.setLineNumber(e.getLineNumber());
                locatorImpl.setColumnNumber(e.getColumnNumber());
                if (ex == null) {
                    throw new SAXParseException(e.getMessage(), locatorImpl);
                }
                throw new SAXParseException(e.getMessage(), locatorImpl, ex);
            }
            if (ex instanceof SAXException) {
                throw ex;
            }
            if (ex instanceof IOException) {
                throw ex;
            }
            throw new SAXException(ex);
        } catch (XNIException e2) {
            Exception ex2 = e2.getException();
            if (ex2 == null) {
                throw new SAXException(e2.getMessage());
            }
            if (ex2 instanceof SAXException) {
                throw ex2;
            }
            if (ex2 instanceof IOException) {
                throw ex2;
            }
            throw new SAXException(ex2);
        }
    }

    public void setEntityResolver(EntityResolver resolver) {
        try {
            ?? r0 = (XMLEntityResolver) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/entity-resolver");
            if (this.fUseEntityResolver2 && (resolver instanceof EntityResolver2)) {
                if (r0 instanceof EntityResolver2Wrapper) {
                    r0.setEntityResolver((EntityResolver2) resolver);
                } else {
                    this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/entity-resolver", new EntityResolver2Wrapper((EntityResolver2) resolver));
                }
            } else if (r0 instanceof EntityResolverWrapper) {
                r0.setEntityResolver(resolver);
            } else {
                this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/entity-resolver", new EntityResolverWrapper(resolver));
            }
        } catch (XMLConfigurationException e) {
        }
    }

    public EntityResolver getEntityResolver() {
        EntityResolver entityResolver = null;
        try {
            ?? r1 = (XMLEntityResolver) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/entity-resolver");
            if (r1 != 0) {
                if (r1 instanceof EntityResolverWrapper) {
                    entityResolver = r1.getEntityResolver();
                } else if (r1 instanceof EntityResolver2Wrapper) {
                    entityResolver = r1.getEntityResolver();
                }
            }
        } catch (XMLConfigurationException e) {
        }
        return entityResolver;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        try {
            ?? r0 = (XMLErrorHandler) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/error-handler");
            if (r0 instanceof ErrorHandlerWrapper) {
                r0.setErrorHandler(errorHandler);
            } else {
                this.fConfiguration.setProperty("http://apache.org/xml/properties/internal/error-handler", new ErrorHandlerWrapper(errorHandler));
            }
        } catch (XMLConfigurationException e) {
        }
    }

    public ErrorHandler getErrorHandler() {
        try {
            ?? r1 = (XMLErrorHandler) this.fConfiguration.getProperty("http://apache.org/xml/properties/internal/error-handler");
            if (r1 == 0 || !(r1 instanceof ErrorHandlerWrapper)) {
                return null;
            }
            ErrorHandler errorHandler = r1.getErrorHandler();
            return errorHandler;
        } catch (XMLConfigurationException e) {
            return null;
        }
    }

    public void setFeature(String featureId, boolean state) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (featureId.equals(USE_ENTITY_RESOLVER2)) {
                if (state != this.fUseEntityResolver2) {
                    this.fUseEntityResolver2 = state;
                    setEntityResolver(getEntityResolver());
                    return;
                }
                return;
            }
            this.fConfiguration.setFeature(featureId, state);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    public boolean getFeature(String featureId) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            if (featureId.equals(USE_ENTITY_RESOLVER2)) {
                return this.fUseEntityResolver2;
            }
            return this.fConfiguration.getFeature(featureId);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    public void setProperty(String propertyId, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        try {
            this.fConfiguration.setProperty(propertyId, value);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    public Object getProperty(String propertyId) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (propertyId.equals("http://apache.org/xml/properties/dom/current-element-node")) {
            boolean deferred = false;
            try {
                deferred = getFeature("http://apache.org/xml/features/dom/defer-node-expansion");
            } catch (XMLConfigurationException e) {
            }
            if (deferred) {
                throw new SAXNotSupportedException(DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "CannotQueryDeferredNode", null));
            }
            if (this.fCurrentNode == null || this.fCurrentNode.getNodeType() != 1) {
                return null;
            }
            return this.fCurrentNode;
        }
        try {
            return this.fConfiguration.getProperty(propertyId);
        } catch (XMLConfigurationException e2) {
            String identifier = e2.getIdentifier();
            if (e2.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fConfiguration.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    public XMLParserConfiguration getXMLParserConfiguration() {
        return this.fConfiguration;
    }
}
