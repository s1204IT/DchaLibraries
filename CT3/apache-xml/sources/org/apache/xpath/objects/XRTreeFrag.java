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

    public XRTreeFrag(int root, XPathContext xctxt, ExpressionNode parent) {
        super(null);
        this.m_dtmRoot = -1;
        this.m_allowRelease = false;
        this.m_xmlStr = null;
        exprSetParent(parent);
        initDTM(root, xctxt);
    }

    public XRTreeFrag(int root, XPathContext xctxt) {
        super(null);
        this.m_dtmRoot = -1;
        this.m_allowRelease = false;
        this.m_xmlStr = null;
        initDTM(root, xctxt);
    }

    private final void initDTM(int root, XPathContext xctxt) {
        this.m_dtmRoot = root;
        DTM dtm = xctxt.getDTM(root);
        if (dtm == null) {
            return;
        }
        this.m_DTMXRTreeFrag = xctxt.getDTMXRTreeFrag(xctxt.getDTMIdentity(dtm));
    }

    @Override
    public Object object() {
        if (this.m_DTMXRTreeFrag.getXPathContext() != null) {
            return new DTMNodeIterator(new NodeSetDTM(this.m_dtmRoot, this.m_DTMXRTreeFrag.getXPathContext().getDTMManager()));
        }
        return super.object();
    }

    public XRTreeFrag(Expression expr) {
        super(expr);
        this.m_dtmRoot = -1;
        this.m_allowRelease = false;
        this.m_xmlStr = null;
    }

    @Override
    public void allowDetachToRelease(boolean allowRelease) {
        this.m_allowRelease = allowRelease;
    }

    @Override
    public void detach() {
        if (!this.m_allowRelease) {
            return;
        }
        this.m_DTMXRTreeFrag.destruct();
        setObject(null);
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
        XMLString s = xstr();
        return s.toDouble();
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
    public void appendToFsb(FastStringBuffer fsb) {
        XString xstring = (XString) xstr();
        xstring.appendToFsb(fsb);
    }

    @Override
    public String str() {
        String str = this.m_DTMXRTreeFrag.getDTM().getStringValue(this.m_dtmRoot).toString();
        return str == null ? "" : str;
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
    public boolean equals(XObject obj2) {
        try {
            if (4 == obj2.getType()) {
                return obj2.equals((XObject) this);
            }
            if (1 == obj2.getType()) {
                return bool() == obj2.bool();
            }
            if (2 == obj2.getType()) {
                return num() == obj2.num();
            }
            if (4 == obj2.getType()) {
                return xstr().equals(obj2.xstr());
            }
            if (3 == obj2.getType()) {
                return xstr().equals(obj2.xstr());
            }
            if (5 == obj2.getType()) {
                return xstr().equals(obj2.xstr());
            }
            return super.equals(obj2);
        } catch (TransformerException te) {
            throw new WrappedRuntimeException(te);
        }
    }
}
