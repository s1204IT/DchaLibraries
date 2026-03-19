package org.apache.xpath.patterns;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class NodeTest extends Expression {
    public static final int SHOW_BYFUNCTION = 65536;
    public static final String SUPPORTS_PRE_STRIPPING = "http://xml.apache.org/xpath/features/whitespace-pre-stripping";
    public static final String WILD = "*";
    static final long serialVersionUID = -5736721866747906182L;
    private boolean m_isTotallyWild;
    protected String m_name;
    String m_namespace;
    XNumber m_score;
    protected int m_whatToShow;
    public static final XNumber SCORE_NODETEST = new XNumber(-0.5d);
    public static final XNumber SCORE_NSWILD = new XNumber(-0.25d);
    public static final XNumber SCORE_QNAME = new XNumber(XPath.MATCH_SCORE_QNAME);
    public static final XNumber SCORE_OTHER = new XNumber(0.5d);
    public static final XNumber SCORE_NONE = new XNumber(Double.NEGATIVE_INFINITY);

    public int getWhatToShow() {
        return this.m_whatToShow;
    }

    public void setWhatToShow(int i) {
        this.m_whatToShow = i;
    }

    public String getNamespace() {
        return this.m_namespace;
    }

    public void setNamespace(String str) {
        this.m_namespace = str;
    }

    public String getLocalName() {
        return this.m_name == null ? "" : this.m_name;
    }

    public void setLocalName(String str) {
        this.m_name = str;
    }

    public NodeTest(int i, String str, String str2) {
        initNodeTest(i, str, str2);
    }

    public NodeTest(int i) {
        initNodeTest(i);
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!isSameClass(expression)) {
            return false;
        }
        NodeTest nodeTest = (NodeTest) expression;
        if (nodeTest.m_name != null) {
            if (this.m_name == null || !nodeTest.m_name.equals(this.m_name)) {
                return false;
            }
        } else if (this.m_name != null) {
            return false;
        }
        if (nodeTest.m_namespace != null) {
            if (this.m_namespace == null || !nodeTest.m_namespace.equals(this.m_namespace)) {
                return false;
            }
        } else if (this.m_namespace != null) {
            return false;
        }
        return this.m_whatToShow == nodeTest.m_whatToShow && this.m_isTotallyWild == nodeTest.m_isTotallyWild;
    }

    public NodeTest() {
    }

    public void initNodeTest(int i) {
        this.m_whatToShow = i;
        calcScore();
    }

    public void initNodeTest(int i, String str, String str2) {
        this.m_whatToShow = i;
        this.m_namespace = str;
        this.m_name = str2;
        calcScore();
    }

    public XNumber getStaticScore() {
        return this.m_score;
    }

    public void setStaticScore(XNumber xNumber) {
        this.m_score = xNumber;
    }

    protected void calcScore() {
        if (this.m_namespace == null && this.m_name == null) {
            this.m_score = SCORE_NODETEST;
        } else if ((this.m_namespace == "*" || this.m_namespace == null) && this.m_name == "*") {
            this.m_score = SCORE_NODETEST;
        } else if (this.m_namespace != "*" && this.m_name == "*") {
            this.m_score = SCORE_NSWILD;
        } else {
            this.m_score = SCORE_QNAME;
        }
        this.m_isTotallyWild = this.m_namespace == null && this.m_name == "*";
    }

    public double getDefaultScore() {
        return this.m_score.num();
    }

    public static int getNodeTypeTest(int i) {
        if ((i & 1) != 0) {
            return 1;
        }
        if ((i & 2) != 0) {
            return 2;
        }
        if ((i & 4) != 0) {
            return 3;
        }
        if ((i & DTMFilter.SHOW_DOCUMENT) != 0) {
            return 9;
        }
        if ((i & 1024) != 0) {
            return 11;
        }
        if ((i & 4096) != 0) {
            return 13;
        }
        if ((i & 128) != 0) {
            return 8;
        }
        if ((i & 64) != 0) {
            return 7;
        }
        if ((i & 512) != 0) {
            return 10;
        }
        if ((i & 32) != 0) {
            return 6;
        }
        if ((i & 16) != 0) {
            return 5;
        }
        if ((i & DTMFilter.SHOW_NOTATION) != 0) {
            return 12;
        }
        if ((i & 8) != 0) {
            return 4;
        }
        return 0;
    }

    public static void debugWhatToShow(int i) {
        Vector vector = new Vector();
        if ((i & 2) != 0) {
            vector.addElement("SHOW_ATTRIBUTE");
        }
        if ((i & 4096) != 0) {
            vector.addElement("SHOW_NAMESPACE");
        }
        if ((i & 8) != 0) {
            vector.addElement("SHOW_CDATA_SECTION");
        }
        if ((i & 128) != 0) {
            vector.addElement("SHOW_COMMENT");
        }
        if ((i & DTMFilter.SHOW_DOCUMENT) != 0) {
            vector.addElement("SHOW_DOCUMENT");
        }
        if ((i & 1024) != 0) {
            vector.addElement("SHOW_DOCUMENT_FRAGMENT");
        }
        if ((i & 512) != 0) {
            vector.addElement("SHOW_DOCUMENT_TYPE");
        }
        if ((i & 1) != 0) {
            vector.addElement("SHOW_ELEMENT");
        }
        if ((i & 32) != 0) {
            vector.addElement("SHOW_ENTITY");
        }
        if ((i & 16) != 0) {
            vector.addElement("SHOW_ENTITY_REFERENCE");
        }
        if ((i & DTMFilter.SHOW_NOTATION) != 0) {
            vector.addElement("SHOW_NOTATION");
        }
        if ((i & 64) != 0) {
            vector.addElement("SHOW_PROCESSING_INSTRUCTION");
        }
        if ((i & 4) != 0) {
            vector.addElement("SHOW_TEXT");
        }
        int size = vector.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (i2 > 0) {
                System.out.print(" | ");
            }
            System.out.print(vector.elementAt(i2));
        }
        if (size == 0) {
            System.out.print("empty whatToShow: " + i);
        }
        System.out.println();
    }

    private static final boolean subPartMatch(String str, String str2) {
        return str == str2 || (str != null && (str2 == "*" || str.equals(str2)));
    }

    private static final boolean subPartMatchNS(String str, String str2) {
        return str == str2 || (str != null && (str.length() <= 0 ? str2 == null : str2 == "*" || str.equals(str2)));
    }

    @Override
    public XObject execute(XPathContext xPathContext, int i) throws TransformerException {
        DTM dtm = xPathContext.getDTM(i);
        short nodeType = dtm.getNodeType(i);
        if (this.m_whatToShow == -1) {
            return this.m_score;
        }
        int i2 = (1 << (nodeType - 1)) & this.m_whatToShow;
        if (i2 == 4 || i2 == 8) {
            return this.m_score;
        }
        if (i2 == 64) {
            return subPartMatch(dtm.getNodeName(i), this.m_name) ? this.m_score : SCORE_NONE;
        }
        if (i2 == 128) {
            return this.m_score;
        }
        if (i2 == 256 || i2 == 1024) {
            return SCORE_OTHER;
        }
        if (i2 == 4096) {
            return subPartMatch(dtm.getLocalName(i), this.m_name) ? this.m_score : SCORE_NONE;
        }
        switch (i2) {
            case 1:
            case 2:
                return (this.m_isTotallyWild || (subPartMatchNS(dtm.getNamespaceURI(i), this.m_namespace) && subPartMatch(dtm.getLocalName(i), this.m_name))) ? this.m_score : SCORE_NONE;
            default:
                return SCORE_NONE;
        }
    }

    @Override
    public XObject execute(XPathContext xPathContext, int i, DTM dtm, int i2) throws TransformerException {
        if (this.m_whatToShow == -1) {
            return this.m_score;
        }
        int nodeType = this.m_whatToShow & (1 << (dtm.getNodeType(i) - 1));
        if (nodeType == 4 || nodeType == 8) {
            return this.m_score;
        }
        if (nodeType == 64) {
            return subPartMatch(dtm.getNodeName(i), this.m_name) ? this.m_score : SCORE_NONE;
        }
        if (nodeType == 128) {
            return this.m_score;
        }
        if (nodeType == 256 || nodeType == 1024) {
            return SCORE_OTHER;
        }
        if (nodeType == 4096) {
            return subPartMatch(dtm.getLocalName(i), this.m_name) ? this.m_score : SCORE_NONE;
        }
        switch (nodeType) {
            case 1:
            case 2:
                return (this.m_isTotallyWild || (subPartMatchNS(dtm.getNamespaceURI(i), this.m_namespace) && subPartMatch(dtm.getLocalName(i), this.m_name))) ? this.m_score : SCORE_NONE;
            default:
                return SCORE_NONE;
        }
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext, xPathContext.getCurrentNode());
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
    }

    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        assertion(false, "callVisitors should not be called for this object!!!");
    }
}
