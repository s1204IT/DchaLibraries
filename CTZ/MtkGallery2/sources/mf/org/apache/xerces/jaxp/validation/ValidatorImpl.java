package mf.org.apache.xerces.jaxp.validation;

import java.io.IOException;
import mf.javax.xml.transform.Result;
import mf.javax.xml.transform.Source;
import mf.javax.xml.transform.dom.DOMSource;
import mf.javax.xml.transform.sax.SAXSource;
import mf.javax.xml.transform.stax.StAXSource;
import mf.javax.xml.transform.stream.StreamSource;
import mf.javax.xml.validation.Validator;
import mf.org.apache.xerces.util.SAXMessageFormatter;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.ElementPSVI;
import mf.org.apache.xerces.xs.PSVIProvider;
import mf.org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

final class ValidatorImpl extends Validator implements PSVIProvider {
    private static final String CURRENT_ELEMENT_NODE = "http://apache.org/xml/properties/dom/current-element-node";
    private static final String JAXP_SOURCE_RESULT_FEATURE_PREFIX = "http://javax.xml.transform";
    private final XMLSchemaValidatorComponentManager fComponentManager;
    private DOMValidatorHelper fDOMValidatorHelper;
    private ValidatorHandlerImpl fSAXValidatorHelper;
    private StAXValidatorHelper fStAXValidatorHelper;
    private StreamValidatorHelper fStreamValidatorHelper;
    private boolean fConfigurationChanged = false;
    private boolean fErrorHandlerChanged = false;
    private boolean fResourceResolverChanged = false;

    public ValidatorImpl(XSGrammarPoolContainer grammarContainer) {
        this.fComponentManager = new XMLSchemaValidatorComponentManager(grammarContainer);
        setErrorHandler(null);
        setResourceResolver(null);
    }

    @Override
    public void validate(Source source, Result result) throws SAXException, IOException {
        if (source instanceof SAXSource) {
            if (this.fSAXValidatorHelper == null) {
                this.fSAXValidatorHelper = new ValidatorHandlerImpl(this.fComponentManager);
            }
            this.fSAXValidatorHelper.validate(source, result);
            return;
        }
        if (source instanceof DOMSource) {
            if (this.fDOMValidatorHelper == null) {
                this.fDOMValidatorHelper = new DOMValidatorHelper(this.fComponentManager);
            }
            this.fDOMValidatorHelper.validate(source, result);
        } else if (source instanceof StAXSource) {
            if (this.fStAXValidatorHelper == null) {
                this.fStAXValidatorHelper = new StAXValidatorHelper(this.fComponentManager);
            }
            this.fStAXValidatorHelper.validate(source, result);
        } else if (source instanceof StreamSource) {
            if (this.fStreamValidatorHelper == null) {
                this.fStreamValidatorHelper = new StreamValidatorHelper(this.fComponentManager);
            }
            this.fStreamValidatorHelper.validate(source, result);
        } else {
            if (source == null) {
                throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "SourceParameterNull", null));
            }
            throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "SourceNotAccepted", new Object[]{source.getClass().getName()}));
        }
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.fErrorHandlerChanged = errorHandler != null;
        this.fComponentManager.setErrorHandler(errorHandler);
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.fComponentManager.getErrorHandler();
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        this.fResourceResolverChanged = resourceResolver != null;
        this.fComponentManager.setResourceResolver(resourceResolver);
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return this.fComponentManager.getResourceResolver();
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "FeatureNameNull", null));
        }
        if (name.startsWith(JAXP_SOURCE_RESULT_FEATURE_PREFIX) && (name.equals("http://javax.xml.transform.stream.StreamSource/feature") || name.equals("http://javax.xml.transform.sax.SAXSource/feature") || name.equals("http://javax.xml.transform.dom.DOMSource/feature") || name.equals("http://javax.xml.transform.stax.StAXSource/feature") || name.equals("http://javax.xml.transform.stream.StreamResult/feature") || name.equals("http://javax.xml.transform.sax.SAXResult/feature") || name.equals("http://javax.xml.transform.dom.DOMResult/feature") || name.equals("http://javax.xml.transform.stax.StAXResult/feature"))) {
            return true;
        }
        try {
            return this.fComponentManager.getFeature(name);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "FeatureNameNull", null));
        }
        if (name.startsWith(JAXP_SOURCE_RESULT_FEATURE_PREFIX) && (name.equals("http://javax.xml.transform.stream.StreamSource/feature") || name.equals("http://javax.xml.transform.sax.SAXSource/feature") || name.equals("http://javax.xml.transform.dom.DOMSource/feature") || name.equals("http://javax.xml.transform.stax.StAXSource/feature") || name.equals("http://javax.xml.transform.stream.StreamResult/feature") || name.equals("http://javax.xml.transform.sax.SAXResult/feature") || name.equals("http://javax.xml.transform.dom.DOMResult/feature") || name.equals("http://javax.xml.transform.stax.StAXResult/feature"))) {
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-read-only", new Object[]{name}));
        }
        try {
            this.fComponentManager.setFeature(name, value);
            this.fConfigurationChanged = true;
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "feature-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "ProperyNameNull", null));
        }
        if (CURRENT_ELEMENT_NODE.equals(name)) {
            if (this.fDOMValidatorHelper == null) {
                return null;
            }
            return this.fDOMValidatorHelper.getCurrentElement();
        }
        try {
            return this.fComponentManager.getProperty(name);
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public void setProperty(String name, Object object) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "ProperyNameNull", null));
        }
        if (CURRENT_ELEMENT_NODE.equals(name)) {
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-read-only", new Object[]{name}));
        }
        try {
            this.fComponentManager.setProperty(name, object);
            this.fConfigurationChanged = true;
        } catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == 0) {
                throw new SAXNotRecognizedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-recognized", new Object[]{identifier}));
            }
            throw new SAXNotSupportedException(SAXMessageFormatter.formatMessage(this.fComponentManager.getLocale(), "property-not-supported", new Object[]{identifier}));
        }
    }

    @Override
    public void reset() {
        if (this.fConfigurationChanged) {
            this.fComponentManager.restoreInitialState();
            setErrorHandler(null);
            setResourceResolver(null);
            this.fConfigurationChanged = false;
            this.fErrorHandlerChanged = false;
            this.fResourceResolverChanged = false;
            return;
        }
        if (this.fErrorHandlerChanged) {
            setErrorHandler(null);
            this.fErrorHandlerChanged = false;
        }
        if (this.fResourceResolverChanged) {
            setResourceResolver(null);
            this.fResourceResolverChanged = false;
        }
    }

    @Override
    public ElementPSVI getElementPSVI() {
        if (this.fSAXValidatorHelper != null) {
            return this.fSAXValidatorHelper.getElementPSVI();
        }
        return null;
    }

    @Override
    public AttributePSVI getAttributePSVI(int index) {
        if (this.fSAXValidatorHelper != null) {
            return this.fSAXValidatorHelper.getAttributePSVI(index);
        }
        return null;
    }

    @Override
    public AttributePSVI getAttributePSVIByName(String uri, String localname) {
        if (this.fSAXValidatorHelper != null) {
            return this.fSAXValidatorHelper.getAttributePSVIByName(uri, localname);
        }
        return null;
    }
}
