package org.apache.xpath.jaxp;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class XPathExpressionImpl implements XPathExpression {
    private boolean featureSecureProcessing;
    private XPathFunctionResolver functionResolver;
    private JAXPPrefixResolver prefixResolver;
    private XPathVariableResolver variableResolver;
    private XPath xpath;
    static DocumentBuilderFactory dbf = null;
    static DocumentBuilder db = null;
    static Document d = null;

    protected XPathExpressionImpl() {
        this.featureSecureProcessing = false;
    }

    protected XPathExpressionImpl(XPath xPath, JAXPPrefixResolver jAXPPrefixResolver, XPathFunctionResolver xPathFunctionResolver, XPathVariableResolver xPathVariableResolver) {
        this.featureSecureProcessing = false;
        this.xpath = xPath;
        this.prefixResolver = jAXPPrefixResolver;
        this.functionResolver = xPathFunctionResolver;
        this.variableResolver = xPathVariableResolver;
        this.featureSecureProcessing = false;
    }

    protected XPathExpressionImpl(XPath xPath, JAXPPrefixResolver jAXPPrefixResolver, XPathFunctionResolver xPathFunctionResolver, XPathVariableResolver xPathVariableResolver, boolean z) {
        this.featureSecureProcessing = false;
        this.xpath = xPath;
        this.prefixResolver = jAXPPrefixResolver;
        this.functionResolver = xPathFunctionResolver;
        this.variableResolver = xPathVariableResolver;
        this.featureSecureProcessing = z;
    }

    public void setXPath(XPath xPath) {
        this.xpath = xPath;
    }

    public Object eval(Object obj, QName qName) throws TransformerException {
        return getResultAsType(eval(obj), qName);
    }

    private XObject eval(Object obj) throws TransformerException {
        XPathContext xPathContext;
        if (this.functionResolver != null) {
            xPathContext = new XPathContext(new JAXPExtensionsProvider(this.functionResolver, this.featureSecureProcessing), false);
        } else {
            xPathContext = new XPathContext(false);
        }
        xPathContext.setVarStack(new JAXPVariableStack(this.variableResolver));
        Node dummyDocument = (Node) obj;
        if (dummyDocument == null) {
            dummyDocument = getDummyDocument();
        }
        return this.xpath.execute(xPathContext, dummyDocument, this.prefixResolver);
    }

    @Override
    public Object evaluate(Object obj, QName qName) throws XPathExpressionException {
        if (qName == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"returnType"}));
        }
        if (!isSupported(qName)) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{qName.toString()}));
        }
        try {
            return eval(obj, qName);
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

    @Override
    public String evaluate(Object obj) throws XPathExpressionException {
        return (String) evaluate(obj, XPathConstants.STRING);
    }

    @Override
    public Object evaluate(InputSource inputSource, QName qName) throws XPathExpressionException {
        if (inputSource == null || qName == null) {
            throw new NullPointerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL, null));
        }
        if (!isSupported(qName)) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{qName.toString()}));
        }
        try {
            if (dbf == null) {
                dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
            }
            db = dbf.newDocumentBuilder();
            return eval(db.parse(inputSource), qName);
        } catch (Exception e) {
            throw new XPathExpressionException(e);
        }
    }

    @Override
    public String evaluate(InputSource inputSource) throws XPathExpressionException {
        return (String) evaluate(inputSource, XPathConstants.STRING);
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

    private static Document getDummyDocument() {
        try {
            if (dbf == null) {
                dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
            }
            db = dbf.newDocumentBuilder();
            d = db.getDOMImplementation().createDocument("http://java.sun.com/jaxp/xpath", "dummyroot", null);
            return d;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
