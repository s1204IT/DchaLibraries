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
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
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

    protected XPathExpressionImpl(XPath xpath, JAXPPrefixResolver prefixResolver, XPathFunctionResolver functionResolver, XPathVariableResolver variableResolver) {
        this.featureSecureProcessing = false;
        this.xpath = xpath;
        this.prefixResolver = prefixResolver;
        this.functionResolver = functionResolver;
        this.variableResolver = variableResolver;
        this.featureSecureProcessing = false;
    }

    protected XPathExpressionImpl(XPath xpath, JAXPPrefixResolver prefixResolver, XPathFunctionResolver functionResolver, XPathVariableResolver variableResolver, boolean featureSecureProcessing) {
        this.featureSecureProcessing = false;
        this.xpath = xpath;
        this.prefixResolver = prefixResolver;
        this.functionResolver = functionResolver;
        this.variableResolver = variableResolver;
        this.featureSecureProcessing = featureSecureProcessing;
    }

    public void setXPath(XPath xpath) {
        this.xpath = xpath;
    }

    public Object eval(Object item, QName returnType) throws TransformerException {
        XObject resultObject = eval(item);
        return getResultAsType(resultObject, returnType);
    }

    private XObject eval(Object contextItem) throws TransformerException {
        XPathContext xpathSupport;
        if (this.functionResolver != null) {
            JAXPExtensionsProvider jep = new JAXPExtensionsProvider(this.functionResolver, this.featureSecureProcessing);
            xpathSupport = new XPathContext(jep, false);
        } else {
            xpathSupport = new XPathContext(false);
        }
        xpathSupport.setVarStack(new JAXPVariableStack(this.variableResolver));
        Node contextNode = (Node) contextItem;
        if (contextNode == null) {
            contextNode = getDummyDocument();
        }
        XObject xobj = this.xpath.execute(xpathSupport, contextNode, this.prefixResolver);
        return xobj;
    }

    @Override
    public Object evaluate(Object item, QName returnType) throws Throwable {
        if (returnType == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ARG_CANNOT_BE_NULL, new Object[]{"returnType"});
            throw new NullPointerException(fmsg);
        }
        if (!isSupported(returnType)) {
            String fmsg2 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{returnType.toString()});
            throw new IllegalArgumentException(fmsg2);
        }
        try {
            return eval(item, returnType);
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

    @Override
    public String evaluate(Object item) throws XPathExpressionException {
        return (String) evaluate(item, XPathConstants.STRING);
    }

    @Override
    public Object evaluate(InputSource source, QName returnType) throws XPathExpressionException {
        if (source == null || returnType == null) {
            String fmsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_SOURCE_RETURN_TYPE_CANNOT_BE_NULL, null);
            throw new NullPointerException(fmsg);
        }
        if (!isSupported(returnType)) {
            String fmsg2 = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE, new Object[]{returnType.toString()});
            throw new IllegalArgumentException(fmsg2);
        }
        try {
            if (dbf == null) {
                dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
            }
            db = dbf.newDocumentBuilder();
            Document document = db.parse(source);
            return eval(document, returnType);
        } catch (Exception e) {
            throw new XPathExpressionException(e);
        }
    }

    @Override
    public String evaluate(InputSource source) throws XPathExpressionException {
        return (String) evaluate(source, XPathConstants.STRING);
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

    private static Document getDummyDocument() {
        try {
            if (dbf == null) {
                dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setValidating(false);
            }
            db = dbf.newDocumentBuilder();
            DOMImplementation dim = db.getDOMImplementation();
            d = dim.createDocument("http://java.sun.com/jaxp/xpath", "dummyroot", null);
            return d;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
