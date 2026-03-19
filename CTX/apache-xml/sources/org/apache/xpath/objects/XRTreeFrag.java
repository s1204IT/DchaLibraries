package org.apache.xpath.objects;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionNode;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.RTFIterator;
import org.w3c.dom.NodeList;

public class XRTreeFrag extends XObject implements Cloneable {
    static final long serialVersionUID = -3201553822254911567L;
    private DTMXRTreeFrag m_DTMXRTreeFrag;
    protected boolean m_allowRelease;
    private int m_dtmRoot;
    private XMLString m_xmlStr;

    public XRTreeFrag(int i, XPathContext xPathContext, ExpressionNode expressionNode) {
        super(null);
        this.m_dtmRoot = -1;
        this.m_allowRelease = false;
        this.m_xmlStr = null;
        exprSetParent(expressionNode);
        initDTM(i, xPathContext);
    }

    public XRTreeFrag(int i, XPathContext xPathContext) {
        super(null);
        this.m_dtmRoot = -1;
        this.m_allowRelease = false;
        this.m_xmlStr = null;
        initDTM(i, xPathContext);
    }

    private final void initDTM(int i, XPathContext xPathContext) {
        this.m_dtmRoot = i;
        DTM dtm = xPathContext.getDTM(i);
        if (dtm != null) {
            this.m_DTMXRTreeFrag = xPathContext.getDTMXRTreeFrag(xPathContext.getDTMIdentity(dtm));
        }
    }

    @Override
    public Object object() {
        if (this.m_DTMXRTreeFrag.getXPathContext() != null) {
            return new DTMNodeIterator(new NodeSetDTM(this.m_dtmRoot, this.m_DTMXRTreeFrag.getXPathContext().getDTMManager()));
        }
        return super.object();
    }

    public XRTreeFrag(Expression expression) {
        super(expression);
        this.m_dtmRoot = -1;
        this.m_allowRelease = false;
        this.m_xmlStr = null;
    }

    @Override
    public void allowDetachToRelease(boolean z) {
        this.m_allowRelease = z;
    }

    @Override
    public void detach() {
        if (this.m_allowRelease) {
            this.m_DTMXRTreeFrag.destruct();
            setObject(null);
        }
    }

    @Override
    public int getType() {
        return 5;
    }

    @Override
    public String getTypeString() {
        return "#RTREEFRAG";
    }

    @Override
    public double num() throws TransformerException {
        return xstr().toDouble();
    }

    @Override
    public boolean bool() {
        return true;
    }

    @Override
    public XMLString xstr() {
        if (this.m_xmlStr == null) {
            this.m_xmlStr = this.m_DTMXRTreeFrag.getDTM().getStringValue(this.m_dtmRoot);
        }
        return this.m_xmlStr;
    }

    @Override
    public void appendToFsb(FastStringBuffer fastStringBuffer) {
        ((XString) xstr()).appendToFsb(fastStringBuffer);
    }

    @Override
    public String str() {
        String string = this.m_DTMXRTreeFrag.getDTM().getStringValue(this.m_dtmRoot).toString();
        return string == null ? "" : string;
    }

    @Override
    public int rtf() {
        return this.m_dtmRoot;
    }

    public DTMIterator asNodeIterator() {
        return new RTFIterator(this.m_dtmRoot, this.m_DTMXRTreeFrag.getXPathContext().getDTMManager());
    }

    public NodeList convertToNodeset() {
        if (this.m_obj instanceof NodeList) {
            return (NodeList) this.m_obj;
        }
        return new DTMNodeList(asNodeIterator());
    }

    @Override
    public boolean equals(XObject xObject) {
        try {
            if (4 == xObject.getType()) {
                return xObject.equals((XObject) this);
            }
            if (1 == xObject.getType()) {
                return bool() == xObject.bool();
            }
            if (2 == xObject.getType()) {
                return num() == xObject.num();
            }
            if (4 == xObject.getType()) {
                return xstr().equals(xObject.xstr());
            }
            if (3 == xObject.getType()) {
                return xstr().equals(xObject.xstr());
            }
            if (5 == xObject.getType()) {
                return xstr().equals(xObject.xstr());
            }
            return super.equals(xObject);
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
