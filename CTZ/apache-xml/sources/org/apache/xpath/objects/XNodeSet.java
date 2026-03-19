package org.apache.xpath.objects;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPath;
import org.apache.xpath.axes.NodeSequence;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class XNodeSet extends NodeSequence {
    static final long serialVersionUID = 1916026368035639667L;
    static final LessThanComparator S_LT = new LessThanComparator();
    static final LessThanOrEqualComparator S_LTE = new LessThanOrEqualComparator();
    static final GreaterThanComparator S_GT = new GreaterThanComparator();
    static final GreaterThanOrEqualComparator S_GTE = new GreaterThanOrEqualComparator();
    static final EqualComparator S_EQ = new EqualComparator();
    static final NotEqualComparator S_NEQ = new NotEqualComparator();

    protected XNodeSet() {
    }

    public XNodeSet(DTMIterator dTMIterator) {
        if (dTMIterator instanceof XNodeSet) {
            XNodeSet xNodeSet = (XNodeSet) dTMIterator;
            setIter(xNodeSet.m_iter);
            this.m_dtmMgr = xNodeSet.m_dtmMgr;
            this.m_last = xNodeSet.m_last;
            if (!xNodeSet.hasCache()) {
                xNodeSet.setShouldCacheNodes(true);
            }
            setObject(xNodeSet.getIteratorCache());
            return;
        }
        setIter(dTMIterator);
    }

    public XNodeSet(XNodeSet xNodeSet) {
        setIter(xNodeSet.m_iter);
        this.m_dtmMgr = xNodeSet.m_dtmMgr;
        this.m_last = xNodeSet.m_last;
        if (!xNodeSet.hasCache()) {
            xNodeSet.setShouldCacheNodes(true);
        }
        setObject(xNodeSet.m_obj);
    }

    public XNodeSet(DTMManager dTMManager) {
        this(-1, dTMManager);
    }

    public XNodeSet(int i, DTMManager dTMManager) {
        super(new NodeSetDTM(dTMManager));
        this.m_dtmMgr = dTMManager;
        if (-1 != i) {
            ((NodeSetDTM) this.m_obj).addNode(i);
            this.m_last = 1;
        } else {
            this.m_last = 0;
        }
    }

    @Override
    public int getType() {
        return 4;
    }

    @Override
    public String getTypeString() {
        return "#NODESET";
    }

    public double getNumberFromNode(int i) {
        return this.m_dtmMgr.getDTM(i).getStringValue(i).toDouble();
    }

    @Override
    public double num() {
        int iItem = item(0);
        if (iItem != -1) {
            return getNumberFromNode(iItem);
        }
        return Double.NaN;
    }

    @Override
    public double numWithSideEffects() {
        int iNextNode = nextNode();
        if (iNextNode != -1) {
            return getNumberFromNode(iNextNode);
        }
        return Double.NaN;
    }

    @Override
    public boolean bool() {
        return item(0) != -1;
    }

    @Override
    public boolean boolWithSideEffects() {
        return nextNode() != -1;
    }

    public XMLString getStringFromNode(int i) {
        if (-1 != i) {
            return this.m_dtmMgr.getDTM(i).getStringValue(i);
        }
        return XString.EMPTYSTRING;
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler contentHandler) throws SAXException {
        int iItem = item(0);
        if (iItem != -1) {
            this.m_dtmMgr.getDTM(iItem).dispatchCharactersEvents(iItem, contentHandler, false);
        }
    }

    @Override
    public XMLString xstr() {
        int iItem = item(0);
        return iItem != -1 ? getStringFromNode(iItem) : XString.EMPTYSTRING;
    }

    @Override
    public void appendToFsb(FastStringBuffer fastStringBuffer) {
        ((XString) xstr()).appendToFsb(fastStringBuffer);
    }

    @Override
    public String str() {
        int iItem = item(0);
        return iItem != -1 ? getStringFromNode(iItem).toString() : "";
    }

    @Override
    public Object object() {
        if (this.m_obj == null) {
            return this;
        }
        return this.m_obj;
    }

    @Override
    public NodeIterator nodeset() throws TransformerException {
        return new DTMNodeIterator(iter());
    }

    @Override
    public NodeList nodelist() throws TransformerException {
        DTMNodeList dTMNodeList = new DTMNodeList(this);
        SetVector(((XNodeSet) dTMNodeList.getDTMIterator()).getVector());
        return dTMNodeList;
    }

    public DTMIterator iterRaw() {
        return this;
    }

    public void release(DTMIterator dTMIterator) {
    }

    @Override
    public DTMIterator iter() {
        try {
            if (hasCache()) {
                return cloneWithReset();
            }
            return this;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public XObject getFresh() {
        try {
            if (hasCache()) {
                return (XObject) cloneWithReset();
            }
            return this;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public NodeSetDTM mutableNodeset() {
        if (this.m_obj instanceof NodeSetDTM) {
            return (NodeSetDTM) this.m_obj;
        }
        NodeSetDTM nodeSetDTM = new NodeSetDTM(iter());
        setObject(nodeSetDTM);
        setCurrentPos(0);
        return nodeSetDTM;
    }

    public boolean compare(XObject xObject, Comparator comparator) throws TransformerException {
        boolean z;
        boolean z2;
        boolean z3;
        int type = xObject.getType();
        if (4 == type) {
            DTMIterator dTMIteratorIterRaw = iterRaw();
            DTMIterator dTMIteratorIterRaw2 = ((XNodeSet) xObject).iterRaw();
            Vector vector = null;
            boolean z4 = false;
            while (true) {
                int iNextNode = dTMIteratorIterRaw.nextNode();
                if (-1 != iNextNode) {
                    XMLString stringFromNode = getStringFromNode(iNextNode);
                    if (vector == null) {
                        while (true) {
                            int iNextNode2 = dTMIteratorIterRaw2.nextNode();
                            if (-1 != iNextNode2) {
                                XMLString stringFromNode2 = getStringFromNode(iNextNode2);
                                if (!comparator.compareStrings(stringFromNode, stringFromNode2)) {
                                    if (vector == null) {
                                        vector = new Vector();
                                    }
                                    vector.addElement(stringFromNode2);
                                } else {
                                    z4 = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        int size = vector.size();
                        int i = 0;
                        while (true) {
                            if (i >= size) {
                                break;
                            }
                            if (!comparator.compareStrings(stringFromNode, (XMLString) vector.elementAt(i))) {
                                i++;
                            } else {
                                z4 = true;
                                break;
                            }
                        }
                    }
                } else {
                    dTMIteratorIterRaw.reset();
                    dTMIteratorIterRaw2.reset();
                    return z4;
                }
            }
        } else {
            if (1 == type) {
                return comparator.compareNumbers(bool() ? 1.0d : XPath.MATCH_SCORE_QNAME, xObject.num());
            }
            if (2 == type) {
                DTMIterator dTMIteratorIterRaw3 = iterRaw();
                double dNum = xObject.num();
                while (true) {
                    int iNextNode3 = dTMIteratorIterRaw3.nextNode();
                    if (-1 != iNextNode3) {
                        if (comparator.compareNumbers(getNumberFromNode(iNextNode3), dNum)) {
                            z3 = true;
                            break;
                        }
                    } else {
                        z3 = false;
                        break;
                    }
                }
                dTMIteratorIterRaw3.reset();
                return z3;
            }
            if (5 == type) {
                XMLString xMLStringXstr = xObject.xstr();
                DTMIterator dTMIteratorIterRaw4 = iterRaw();
                while (true) {
                    int iNextNode4 = dTMIteratorIterRaw4.nextNode();
                    if (-1 != iNextNode4) {
                        if (comparator.compareStrings(getStringFromNode(iNextNode4), xMLStringXstr)) {
                            z2 = true;
                            break;
                        }
                    } else {
                        z2 = false;
                        break;
                    }
                }
                dTMIteratorIterRaw4.reset();
                return z2;
            }
            if (3 == type) {
                XMLString xMLStringXstr2 = xObject.xstr();
                DTMIterator dTMIteratorIterRaw5 = iterRaw();
                while (true) {
                    int iNextNode5 = dTMIteratorIterRaw5.nextNode();
                    if (-1 != iNextNode5) {
                        if (comparator.compareStrings(getStringFromNode(iNextNode5), xMLStringXstr2)) {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                dTMIteratorIterRaw5.reset();
                return z;
            }
            return comparator.compareNumbers(num(), xObject.num());
        }
    }

    @Override
    public boolean lessThan(XObject xObject) throws TransformerException {
        return compare(xObject, S_LT);
    }

    @Override
    public boolean lessThanOrEqual(XObject xObject) throws TransformerException {
        return compare(xObject, S_LTE);
    }

    @Override
    public boolean greaterThan(XObject xObject) throws TransformerException {
        return compare(xObject, S_GT);
    }

    @Override
    public boolean greaterThanOrEqual(XObject xObject) throws TransformerException {
        return compare(xObject, S_GTE);
    }

    @Override
    public boolean equals(XObject xObject) {
        try {
            return compare(xObject, S_EQ);
        } catch (TransformerException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @Override
    public boolean notEquals(XObject xObject) throws TransformerException {
        return compare(xObject, S_NEQ);
    }
}
