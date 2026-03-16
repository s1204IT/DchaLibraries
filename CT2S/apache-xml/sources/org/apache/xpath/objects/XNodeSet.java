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

    public XNodeSet(DTMIterator val) {
        if (val instanceof XNodeSet) {
            XNodeSet nodeSet = (XNodeSet) val;
            setIter(nodeSet.m_iter);
            this.m_dtmMgr = nodeSet.m_dtmMgr;
            this.m_last = nodeSet.m_last;
            if (!nodeSet.hasCache()) {
                nodeSet.setShouldCacheNodes(true);
            }
            setObject(nodeSet.getIteratorCache());
            return;
        }
        setIter(val);
    }

    public XNodeSet(XNodeSet val) {
        setIter(val.m_iter);
        this.m_dtmMgr = val.m_dtmMgr;
        this.m_last = val.m_last;
        if (!val.hasCache()) {
            val.setShouldCacheNodes(true);
        }
        setObject(val.m_obj);
    }

    public XNodeSet(DTMManager dtmMgr) {
        this(-1, dtmMgr);
    }

    public XNodeSet(int n, DTMManager dtmMgr) {
        super(new NodeSetDTM(dtmMgr));
        this.m_dtmMgr = dtmMgr;
        if (-1 != n) {
            ((NodeSetDTM) this.m_obj).addNode(n);
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

    public double getNumberFromNode(int n) {
        XMLString xstr = this.m_dtmMgr.getDTM(n).getStringValue(n);
        return xstr.toDouble();
    }

    @Override
    public double num() {
        int node = item(0);
        if (node != -1) {
            return getNumberFromNode(node);
        }
        return Double.NaN;
    }

    @Override
    public double numWithSideEffects() {
        int node = nextNode();
        if (node != -1) {
            return getNumberFromNode(node);
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

    public XMLString getStringFromNode(int n) {
        return -1 != n ? this.m_dtmMgr.getDTM(n).getStringValue(n) : XString.EMPTYSTRING;
    }

    @Override
    public void dispatchCharactersEvents(ContentHandler ch) throws SAXException {
        int node = item(0);
        if (node != -1) {
            this.m_dtmMgr.getDTM(node).dispatchCharactersEvents(node, ch, false);
        }
    }

    @Override
    public XMLString xstr() {
        int node = item(0);
        return node != -1 ? getStringFromNode(node) : XString.EMPTYSTRING;
    }

    @Override
    public void appendToFsb(FastStringBuffer fsb) {
        XString xstring = (XString) xstr();
        xstring.appendToFsb(fsb);
    }

    @Override
    public String str() {
        int node = item(0);
        return node != -1 ? getStringFromNode(node).toString() : "";
    }

    @Override
    public Object object() {
        return this.m_obj == null ? this : this.m_obj;
    }

    @Override
    public NodeIterator nodeset() throws TransformerException {
        return new DTMNodeIterator(iter());
    }

    @Override
    public NodeList nodelist() throws TransformerException {
        DTMNodeList nodelist = new DTMNodeList(this);
        XNodeSet clone = (XNodeSet) nodelist.getDTMIterator();
        SetVector(clone.getVector());
        return nodelist;
    }

    public DTMIterator iterRaw() {
        return this;
    }

    public void release(DTMIterator iter) {
    }

    @Override
    public DTMIterator iter() {
        try {
            if (hasCache()) {
                return cloneWithReset();
            }
            return this;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse.getMessage());
        }
    }

    @Override
    public XObject getFresh() {
        try {
            return hasCache() ? (XObject) cloneWithReset() : this;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse.getMessage());
        }
    }

    @Override
    public NodeSetDTM mutableNodeset() {
        if (this.m_obj instanceof NodeSetDTM) {
            return (NodeSetDTM) this.m_obj;
        }
        NodeSetDTM mnl = new NodeSetDTM(iter());
        setObject(mnl);
        setCurrentPos(0);
        return mnl;
    }

    public boolean compare(XObject obj2, Comparator comparator) throws TransformerException {
        boolean result = false;
        int type = obj2.getType();
        if (4 == type) {
            DTMIterator list1 = iterRaw();
            DTMIterator list2 = ((XNodeSet) obj2).iterRaw();
            Vector node2Strings = null;
            while (true) {
                int node1 = list1.nextNode();
                if (-1 != node1) {
                    XMLString s1 = getStringFromNode(node1);
                    if (node2Strings == null) {
                        while (true) {
                            int node2 = list2.nextNode();
                            if (-1 != node2) {
                                XMLString s2 = getStringFromNode(node2);
                                if (comparator.compareStrings(s1, s2)) {
                                    result = true;
                                    break;
                                }
                                if (node2Strings == null) {
                                    node2Strings = new Vector();
                                }
                                node2Strings.addElement(s2);
                            }
                        }
                    } else {
                        int n = node2Strings.size();
                        int i = 0;
                        while (true) {
                            if (i >= n) {
                                break;
                            }
                            if (!comparator.compareStrings(s1, (XMLString) node2Strings.elementAt(i))) {
                                i++;
                            } else {
                                result = true;
                                break;
                            }
                        }
                    }
                } else {
                    list1.reset();
                    list2.reset();
                    return result;
                }
            }
        } else {
            if (1 == type) {
                double num1 = bool() ? 1.0d : XPath.MATCH_SCORE_QNAME;
                double num2 = obj2.num();
                return comparator.compareNumbers(num1, num2);
            }
            if (2 == type) {
                DTMIterator list12 = iterRaw();
                double num22 = obj2.num();
                while (true) {
                    int node = list12.nextNode();
                    if (-1 == node) {
                        break;
                    }
                    double num12 = getNumberFromNode(node);
                    if (comparator.compareNumbers(num12, num22)) {
                        result = true;
                        break;
                    }
                }
                list12.reset();
                return result;
            }
            if (5 == type) {
                XMLString s22 = obj2.xstr();
                DTMIterator list13 = iterRaw();
                while (true) {
                    int node3 = list13.nextNode();
                    if (-1 == node3) {
                        break;
                    }
                    XMLString s12 = getStringFromNode(node3);
                    if (comparator.compareStrings(s12, s22)) {
                        result = true;
                        break;
                    }
                }
                list13.reset();
                return result;
            }
            if (3 == type) {
                XMLString s23 = obj2.xstr();
                DTMIterator list14 = iterRaw();
                while (true) {
                    int node4 = list14.nextNode();
                    if (-1 == node4) {
                        break;
                    }
                    XMLString s13 = getStringFromNode(node4);
                    if (comparator.compareStrings(s13, s23)) {
                        result = true;
                        break;
                    }
                }
                list14.reset();
                return result;
            }
            return comparator.compareNumbers(num(), obj2.num());
        }
    }

    @Override
    public boolean lessThan(XObject obj2) throws TransformerException {
        return compare(obj2, S_LT);
    }

    @Override
    public boolean lessThanOrEqual(XObject obj2) throws TransformerException {
        return compare(obj2, S_LTE);
    }

    @Override
    public boolean greaterThan(XObject obj2) throws TransformerException {
        return compare(obj2, S_GT);
    }

    @Override
    public boolean greaterThanOrEqual(XObject obj2) throws TransformerException {
        return compare(obj2, S_GTE);
    }

    @Override
    public boolean equals(XObject obj2) {
        try {
            return compare(obj2, S_EQ);
        } catch (TransformerException te) {
            throw new WrappedRuntimeException(te);
        }
    }

    @Override
    public boolean notEquals(XObject obj2) throws TransformerException {
        return compare(obj2, S_NEQ);
    }
}
