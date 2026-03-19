package org.apache.xpath.jaxp;

import java.io.IOException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XPathImpl implements XPath {
    private static Document d = null;
    private boolean featureSecureProcessing;
    private XPathFunctionResolver functionResolver;
    private NamespaceContext namespaceContext;
    private XPathFunctionResolver origFunctionResolver;
    private XPathVariableResolver origVariableResolver;
    private JAXPPrefixResolver prefixResolver;
    private XPathVariableResolver variableResolver;

    XPathImpl(XPathVariableResolver xPathVariableResolver, XPathFunctionResolver xPathFunctionResolver) {
        this.namespaceContext = null;
        this.featureSecureProcessing = false;
        this.variableResolver = xPathVariableResolver;
        this.origVariableResolver = xPathVariableResolver;
        this.functionResolver = xPathFunctionResolver;
        this.origFunctionResolver = xPathFunctionResolver;
    }

    XPathImpl(XPathVariableResolver xPathVariableResolver, XPathFunctionResolver xPathFunctionResolver, boolean z) {
        this.namespaceContext = null;
        this.featureSecureProcessing = false;
        this.variableResolver = xPathVariableResolver;
        this.origVariableResolver = xPathVariableResolver;
        this.functionResolver = xPathFunctionResolver;
        this.origFunctionResolver = xPathFunctionResolver;
        this.featureSecureProcessing = z;
    }

    @Override
    public void setXPathVariableResolver(XPathVariableResolver xPathVariableResolver) {
        if (xPathVariableResolver == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPathVariableResolver"}));
        }
        this.variableResolver = xPathVariableResolver;
    }

    @Override
    public XPathVariableResolver getXPathVariableResolver() {
        return this.variableResolver;
    }

    @Override
    public void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        if (xPathFunctionResolver == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPathFunctionResolver"}));
        }
        this.functionResolver = xPathFunctionResolver;
    }

    @Override
    public XPathFunctionResolver getXPathFunctionResolver() {
        return this.functionResolver;
    }

    @Override
    public void setNamespaceContext(NamespaceContext namespaceContext) {
        if (namespaceContext == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"NamespaceContext"}));
        }
        this.namespaceContext = namespaceContext;
        this.prefixResolver = new JAXPPrefixResolver(namespaceContext);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.namespaceContext;
    }

    private static DocumentBuilder getParser() {
        try {
            DocumentBuilderFactory documentBuilderFactoryNewInstance = DocumentBuilderFactory.newInstance();
            documentBuilderFactoryNewInstance.setNamespaceAware(true);
            documentBuilderFactoryNewInstance.setValidating(false);
            return documentBuilderFactoryNewInstance.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error(e.toString());
        }
    }

    private static Document getDummyDocument() {
        if (d == null) {
            d = getParser().getDOMImplementation().createDocument("http://java.sun.com/jaxp/xpath", "dummyroot", null);
        }
        return d;
    }

    private XObject eval(String str, Object obj) throws TransformerException {
        XPathContext xPathContext;
        org.apache.xpath.XPath xPath = new org.apache.xpath.XPath(str, null, this.prefixResolver, 0);
        if (this.functionResolver != null) {
            xPathContext = new XPathContext(new JAXPExtensionsProvider(this.functionResolver, this.featureSecureProcessing), false);
        } else {
            xPathContext = new XPathContext(false);
        }
        xPathContext.setVarStack(new JAXPVariableStack(this.variableResolver));
        if (obj instanceof Node) {
            return xPath.execute(xPathContext, (Node) obj, this.prefixResolver);
        }
        return xPath.execute(xPathContext, -1, this.prefixResolver);
    }

    @Override
    public Object evaluate(String str, Object obj, QName qName) throws XPathExpressionException {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPath expression"}));
        }
        if (qName == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"returnType"}));
        }
        if (!isSupported(qName)) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{qName.toString()}));
        }
        try {
            return getResultAsType(eval(str, obj), qName);
        } catch (NullPointerException e) {
            throw new XPathExpressionException(e);
        } catch (TransformerException e2) {
            Throwable exception = e2.getException();
            if (exception instanceof XPathFunctionException) {
                throw ((XPathFunctionException) exception);
            }
            throw new XPathExpressionException(e2);
        }
    }

    private boolean isSupported(QName qName) {
        if (qName.equals(XPathConstants.STRING) || qName.equals(XPathConstants.NUMBER) || qName.equals(XPathConstants.BOOLEAN) || qName.equals(XPathConstants.NODE) || qName.equals(XPathConstants.NODESET)) {
            return true;
        }
        return false;
    }

    private Object getResultAsType(XObject xObject, QName qName) throws TransformerException {
        if (qName.equals(XPathConstants.STRING)) {
            return xObject.str();
        }
        if (qName.equals(XPathConstants.NUMBER)) {
            return new Double(xObject.num());
        }
        if (qName.equals(XPathConstants.BOOLEAN)) {
            return new Boolean(xObject.bool());
        }
        if (qName.equals(XPathConstants.NODESET)) {
            return xObject.nodelist();
        }
        if (qName.equals(XPathConstants.NODE)) {
            return xObject.nodeset().nextNode();
        }
        throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{qName.toString()}));
    }

    @Override
    public String evaluate(String str, Object obj) throws XPathExpressionException {
        return (String) evaluate(str, obj, XPathConstants.STRING);
    }

    @Override
    public XPathExpression compile(String str) throws XPathExpressionException {
        if (str == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPath expression"}));
        }
        try {
            return new XPathExpressionImpl(new org.apache.xpath.XPath(str, null, this.prefixResolver, 0), this.prefixResolver, this.functionResolver, this.variableResolver, this.featureSecureProcessing);
        } catch (TransformerException e) {
            throw new XPathExpressionException(e);
        }
    }

    @Override
    public Object evaluate(String str, InputSource inputSource, QName qName) throws XPathExpressionException {
        if (inputSource == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"source"}));
        }
        if (str == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPath expression"}));
        }
        if (qName == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"returnType"}));
        }
        if (!isSupported(qName)) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{qName.toString()}));
        }
        try {
            return getResultAsType(eval(str, getParser().parse(inputSource)), qName);
        } catch (IOException e) {
            throw new XPathExpressionException(e);
        } catch (TransformerException e2) {
            Throwable exception = e2.getException();
            if (exception instanceof XPathFunctionException) {
                throw ((XPathFunctionException) exception);
            }
            throw new XPathExpressionException(e2);
        } catch (SAXException e3) {
            throw new XPathExpressionException(e3);
        }
    }

    @Override
    public String evaluate(String str, InputSource inputSource) throws XPathExpressionException {
        return (String) evaluate(str, inputSource, XPathConstants.STRING);
    }

    @Override
    public void reset() {
        this.variableResolver = this.origVariableResolver;
        this.functionResolver = this.origFunctionResolver;
        this.namespaceContext = null;
    }
}
