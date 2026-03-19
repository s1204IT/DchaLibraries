package org.apache.xpath.jaxp;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.res.XPATHErrorResources;

public class XPathFactoryImpl extends XPathFactory {
    private static final String CLASS_NAME = "XPathFactoryImpl";
    private XPathFunctionResolver xPathFunctionResolver = null;
    private XPathVariableResolver xPathVariableResolver = null;
    private boolean featureSecureProcessing = false;

    @Override
    public boolean isObjectModelSupported(String str) {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_OBJECT_MODEL_NULL, new Object[]{getClass().getName()}));
        }
        if (str.length() != 0) {
            return str.equals("http://java.sun.com/jaxp/xpath/dom");
        }
        throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_OBJECT_MODEL_EMPTY, new Object[]{getClass().getName()}));
    }

    @Override
    public XPath newXPath() {
        return new XPathImpl(this.xPathVariableResolver, this.xPathFunctionResolver, this.featureSecureProcessing);
    }

    @Override
    public void setFeature(String str, boolean z) throws XPathFactoryConfigurationException {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FEATURE_NAME_NULL, new Object[]{CLASS_NAME, new Boolean(z)}));
        }
        if (str.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            this.featureSecureProcessing = z;
            return;
        }
        throw new XPathFactoryConfigurationException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FEATURE_UNKNOWN, new Object[]{str, CLASS_NAME, new Boolean(z)}));
    }

    @Override
    public boolean getFeature(String str) throws XPathFactoryConfigurationException {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_GETTING_NULL_FEATURE, new Object[]{CLASS_NAME}));
        }
        if (str.equals("http://javax.xml.XMLConstants/feature/secure-processing")) {
            return this.featureSecureProcessing;
        }
        throw new XPathFactoryConfigurationException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_GETTING_UNKNOWN_FEATURE, new Object[]{str, CLASS_NAME}));
    }

    @Override
    public void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        if (xPathFunctionResolver == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NULL_XPATH_FUNCTION_RESOLVER, new Object[]{CLASS_NAME}));
        }
        this.xPathFunctionResolver = xPathFunctionResolver;
    }

    @Override
    public void setXPathVariableResolver(XPathVariableResolver xPathVariableResolver) {
        if (xPathVariableResolver == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NULL_XPATH_VARIABLE_RESOLVER, new Object[]{CLASS_NAME}));
        }
        this.xPathVariableResolver = xPathVariableResolver;
    }
}
