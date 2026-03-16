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
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
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

    XPathImpl(XPathVariableResolver vr, XPathFunctionResolver fr) {
        this.namespaceContext = null;
        this.featureSecureProcessing = false;
        this.variableResolver = vr;
        this.origVariableResolver = vr;
        this.functionResolver = fr;
        this.origFunctionResolver = fr;
    }

    XPathImpl(XPathVariableResolver vr, XPathFunctionResolver fr, boolean featureSecureProcessing) {
        this.namespaceContext = null;
        this.featureSecureProcessing = false;
        this.variableResolver = vr;
        this.origVariableResolver = vr;
        this.functionResolver = fr;
        this.origFunctionResolver = fr;
        this.featureSecureProcessing = featureSecureProcessing;
    }

    @Override
    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        if (resolver == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPathVariableResolver"});
            throw new NullPointerException(fmsg);
        }
        this.variableResolver = resolver;
    }

    @Override
    public XPathVariableResolver getXPathVariableResolver() {
        return this.variableResolver;
    }

    @Override
    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        if (resolver == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPathFunctionResolver"});
            throw new NullPointerException(fmsg);
        }
        this.functionResolver = resolver;
    }

    @Override
    public XPathFunctionResolver getXPathFunctionResolver() {
        return this.functionResolver;
    }

    @Override
    public void setNamespaceContext(NamespaceContext nsContext) {
        if (nsContext == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"NamespaceContext"});
            throw new NullPointerException(fmsg);
        }
        this.namespaceContext = nsContext;
        this.prefixResolver = new JAXPPrefixResolver(nsContext);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.namespaceContext;
    }

    private static DocumentBuilder getParser() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error(e.toString());
        }
    }

    private static Document getDummyDocument() {
        if (d == null) {
            DOMImplementation dim = getParser().getDOMImplementation();
            d = dim.createDocument("http://java.sun.com/jaxp/xpath", "dummyroot", null);
        }
        return d;
    }

    private XObject eval(String expression, Object contextItem) throws TransformerException {
        XPathContext xpathSupport;
        org.apache.xpath.XPath xpath = new org.apache.xpath.XPath(expression, null, this.prefixResolver, 0);
        if (this.functionResolver != null) {
            JAXPExtensionsProvider jep = new JAXPExtensionsProvider(this.functionResolver, this.featureSecureProcessing);
            xpathSupport = new XPathContext(jep, false);
        } else {
            xpathSupport = new XPathContext(false);
        }
        xpathSupport.setVarStack(new JAXPVariableStack(this.variableResolver));
        if (contextItem instanceof Node) {
            XObject xobj = xpath.execute(xpathSupport, (Node) contextItem, this.prefixResolver);
            return xobj;
        }
        XObject xobj2 = xpath.execute(xpathSupport, -1, this.prefixResolver);
        return xobj2;
    }

    @Override
    public Object evaluate(String expression, Object item, QName returnType) throws Throwable {
        if (expression == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPath expression"});
            throw new NullPointerException(fmsg);
        }
        if (returnType == null) {
            String fmsg2 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"returnType"});
            throw new NullPointerException(fmsg2);
        }
        if (!isSupported(returnType)) {
            String fmsg3 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{returnType.toString()});
            throw new IllegalArgumentException(fmsg3);
        }
        try {
            XObject resultObject = eval(expression, item);
            return getResultAsType(resultObject, returnType);
        } catch (NullPointerException npe) {
            throw new XPathExpressionException(npe);
        } catch (TransformerException te) {
            Throwable nestedException = te.getException();
            if (nestedException instanceof XPathFunctionException) {
                throw ((XPathFunctionException) nestedException);
            }
            throw new XPathExpressionException(te);
        }
    }

    private boolean isSupported(QName returnType) {
        return returnType.equals(XPathConstants.STRING) || returnType.equals(XPathConstants.NUMBER) || returnType.equals(XPathConstants.BOOLEAN) || returnType.equals(XPathConstants.NODE) || returnType.equals(XPathConstants.NODESET);
    }

    private Object getResultAsType(XObject resultObject, QName returnType) throws TransformerException {
        if (returnType.equals(XPathConstants.STRING)) {
            return resultObject.str();
        }
        if (returnType.equals(XPathConstants.NUMBER)) {
            return new Double(resultObject.num());
        }
        if (returnType.equals(XPathConstants.BOOLEAN)) {
            return new Boolean(resultObject.bool());
        }
        if (returnType.equals(XPathConstants.NODESET)) {
            return resultObject.nodelist();
        }
        if (returnType.equals(XPathConstants.NODE)) {
            NodeIterator ni = resultObject.nodeset();
            return ni.nextNode();
        }
        String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{returnType.toString()});
        throw new IllegalArgumentException(fmsg);
    }

    @Override
    public String evaluate(String expression, Object item) throws XPathExpressionException {
        return (String) evaluate(expression, item, XPathConstants.STRING);
    }

    @Override
    public XPathExpression compile(String expression) throws XPathExpressionException {
        if (expression == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPath expression"});
            throw new NullPointerException(fmsg);
        }
        try {
            org.apache.xpath.XPath xpath = new org.apache.xpath.XPath(expression, null, this.prefixResolver, 0);
            XPathExpressionImpl ximpl = new XPathExpressionImpl(xpath, this.prefixResolver, this.functionResolver, this.variableResolver, this.featureSecureProcessing);
            return ximpl;
        } catch (TransformerException te) {
            throw new XPathExpressionException(te);
        }
    }

    @Override
    public Object evaluate(String expression, InputSource source, QName returnType) throws Throwable {
        if (source == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"source"});
            throw new NullPointerException(fmsg);
        }
        if (expression == null) {
            String fmsg2 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"XPath expression"});
            throw new NullPointerException(fmsg2);
        }
        if (returnType == null) {
            String fmsg3 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"returnType"});
            throw new NullPointerException(fmsg3);
        }
        if (!isSupported(returnType)) {
            String fmsg4 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{returnType.toString()});
            throw new IllegalArgumentException(fmsg4);
        }
        try {
            Document document = getParser().parse(source);
            XObject resultObject = eval(expression, document);
            return getResultAsType(resultObject, returnType);
        } catch (IOException e) {
            throw new XPathExpressionException(e);
        } catch (TransformerException te) {
            Throwable nestedException = te.getException();
            if (nestedException instanceof XPathFunctionException) {
                throw ((XPathFunctionException) nestedException);
            }
            throw new XPathExpressionException(te);
        } catch (SAXException e2) {
            throw new XPathExpressionException(e2);
        }
    }

    @Override
    public String evaluate(String expression, InputSource source) throws XPathExpressionException {
        return (String) evaluate(expression, source, XPathConstants.STRING);
    }

    @Override
    public void reset() {
        this.variableResolver = this.origVariableResolver;
        this.functionResolver = this.origFunctionResolver;
        this.namespaceContext = null;
    }
}
