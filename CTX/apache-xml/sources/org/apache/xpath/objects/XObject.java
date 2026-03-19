package org.apache.xpath.objects;

import java.io.Serializable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionNode;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathException;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class XObject extends Expression implements Serializable, Cloneable {
    public static final int CLASS_BOOLEAN = 1;
    public static final int CLASS_NODESET = 4;
    public static final int CLASS_NULL = -1;
    public static final int CLASS_NUMBER = 2;
    public static final int CLASS_RTREEFRAG = 5;
    public static final int CLASS_STRING = 3;
    public static final int CLASS_UNKNOWN = 0;
    public static final int CLASS_UNRESOLVEDVARIABLE = 600;
    static final long serialVersionUID = -821887098985662951L;
    protected Object m_obj;

    public XObject() {
    }

    public XObject(Object obj) {
        setObject(obj);
    }

    protected void setObject(Object obj) {
        this.m_obj = obj;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return this;
    }

    public void allowDetachToRelease(boolean z) {
    }

    public void detach() {
    }

    public void destruct() {
        if (this.m_obj != null) {
            allowDetachToRelease(true);
            detach();
            setObject(null);
        }
    }

    public void reset() {
    }

    public void dispatchCharactersEvents(ContentHandler contentHandler) throws SAXException {
        xstr().dispatchCharactersEvents(contentHandler);
    }

    public static XObject create(Object obj) {
        return XObjectFactory.create(obj);
    }

    public static XObject create(Object obj, XPathContext xPathContext) {
        return XObjectFactory.create(obj, xPathContext);
    }

    public int getType() {
        return 0;
    }

    public String getTypeString() {
        return "#UNKNOWN (" + object().getClass().getName() + ")";
    }

    public double num() throws TransformerException {
        error(XPATHErrorResources.ER_CANT_CONVERT_TO_NUMBER, new Object[]{getTypeString()});
        return XPath.MATCH_SCORE_QNAME;
    }

    public double numWithSideEffects() throws TransformerException {
        return num();
    }

    public boolean bool() throws TransformerException {
        error(XPATHErrorResources.ER_CANT_CONVERT_TO_NUMBER, new Object[]{getTypeString()});
        return false;
    }

    public boolean boolWithSideEffects() throws TransformerException {
        return bool();
    }

    public XMLString xstr() {
        return XMLStringFactoryImpl.getFactory().newstr(str());
    }

    public String str() {
        return this.m_obj != null ? this.m_obj.toString() : "";
    }

    public String toString() {
        return str();
    }

    public int rtf(XPathContext xPathContext) {
        int iRtf = rtf();
        if (-1 == iRtf) {
            DTM dtmCreateDocumentFragment = xPathContext.createDocumentFragment();
            dtmCreateDocumentFragment.appendTextChild(str());
            return dtmCreateDocumentFragment.getDocument();
        }
        return iRtf;
    }

    public DocumentFragment rtree(XPathContext xPathContext) {
        int iRtf = rtf();
        if (-1 == iRtf) {
            DTM dtmCreateDocumentFragment = xPathContext.createDocumentFragment();
            dtmCreateDocumentFragment.appendTextChild(str());
            return (DocumentFragment) dtmCreateDocumentFragment.getNode(dtmCreateDocumentFragment.getDocument());
        }
        DTM dtm = xPathContext.getDTM(iRtf);
        return (DocumentFragment) dtm.getNode(dtm.getDocument());
    }

    public DocumentFragment rtree() {
        return null;
    }

    public int rtf() {
        return -1;
    }

    public Object object() {
        return this.m_obj;
    }

    public DTMIterator iter() throws TransformerException {
        error(XPATHErrorResources.ER_CANT_CONVERT_TO_NODELIST, new Object[]{getTypeString()});
        return null;
    }

    public XObject getFresh() {
        return this;
    }

    public NodeIterator nodeset() throws TransformerException {
        error(XPATHErrorResources.ER_CANT_CONVERT_TO_NODELIST, new Object[]{getTypeString()});
        return null;
    }

    public NodeList nodelist() throws TransformerException {
        error(XPATHErrorResources.ER_CANT_CONVERT_TO_NODELIST, new Object[]{getTypeString()});
        return null;
    }

    public NodeSetDTM mutableNodeset() throws TransformerException {
        error(XPATHErrorResources.ER_CANT_CONVERT_TO_MUTABLENODELIST, new Object[]{getTypeString()});
        return (NodeSetDTM) this.m_obj;
    }

    public Object castToType(int i, XPathContext xPathContext) throws TransformerException {
        switch (i) {
            case 0:
                return this.m_obj;
            case 1:
                return new Boolean(bool());
            case 2:
                return new Double(num());
            case 3:
                return str();
            case 4:
                return iter();
            default:
                error(XPATHErrorResources.ER_CANT_CONVERT_TO_TYPE, new Object[]{getTypeString(), Integer.toString(i)});
                return null;
        }
    }

    public boolean lessThan(XObject xObject) throws TransformerException {
        if (xObject.getType() == 4) {
            return xObject.greaterThan(this);
        }
        return num() < xObject.num();
    }

    public boolean lessThanOrEqual(XObject xObject) throws TransformerException {
        if (xObject.getType() == 4) {
            return xObject.greaterThanOrEqual(this);
        }
        return num() <= xObject.num();
    }

    public boolean greaterThan(XObject xObject) throws TransformerException {
        if (xObject.getType() == 4) {
            return xObject.lessThan(this);
        }
        return num() > xObject.num();
    }

    public boolean greaterThanOrEqual(XObject xObject) throws TransformerException {
        if (xObject.getType() == 4) {
            return xObject.lessThanOrEqual(this);
        }
        return num() >= xObject.num();
    }

    public boolean equals(XObject xObject) {
        if (xObject.getType() == 4) {
            return xObject.equals(this);
        }
        if (this.m_obj != null) {
            return this.m_obj.equals(xObject.m_obj);
        }
        return xObject.m_obj == null;
    }

    public boolean notEquals(XObject xObject) throws TransformerException {
        if (xObject.getType() == 4) {
            return xObject.notEquals(this);
        }
        return !equals(xObject);
    }

    protected void error(String str) throws TransformerException {
        error(str, null);
    }

    protected void error(String str, Object[] objArr) throws TransformerException {
        throw new XPathException(XSLMessages.createXPATHMessage(str, objArr), (ExpressionNode) this);
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
    }

    public void appendToFsb(FastStringBuffer fastStringBuffer) {
        fastStringBuffer.append(str());
    }

    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        assertion(false, "callVisitors should not be called for this object!!!");
    }

    @Override
    public boolean deepEquals(Expression expression) {
        return isSameClass(expression) && equals((XObject) expression);
    }
}
