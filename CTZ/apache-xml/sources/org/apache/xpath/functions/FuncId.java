package org.apache.xpath.functions;

import java.util.StringTokenizer;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.StringVector;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;

public class FuncId extends FunctionOneArg {
    static final long serialVersionUID = 8930573966143567310L;

    private StringVector getNodesByID(XPathContext xPathContext, int i, String str, StringVector stringVector, NodeSetDTM nodeSetDTM, boolean z) {
        if (str != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(str);
            boolean zHasMoreTokens = stringTokenizer.hasMoreTokens();
            DTM dtm = xPathContext.getDTM(i);
            while (zHasMoreTokens) {
                String strNextToken = stringTokenizer.nextToken();
                boolean zHasMoreTokens2 = stringTokenizer.hasMoreTokens();
                if (stringVector == null || !stringVector.contains(strNextToken)) {
                    int elementById = dtm.getElementById(strNextToken);
                    if (-1 != elementById) {
                        nodeSetDTM.addNodeInDocOrder(elementById, xPathContext);
                    }
                    if (strNextToken != null && (zHasMoreTokens2 || z)) {
                        if (stringVector == null) {
                            stringVector = new StringVector();
                        }
                        stringVector.addElement(strNextToken);
                    }
                }
                zHasMoreTokens = zHasMoreTokens2;
            }
        }
        return stringVector;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int document = xPathContext.getDTM(xPathContext.getCurrentNode()).getDocument();
        if (-1 == document) {
            error(xPathContext, XPATHErrorResources.ER_CONTEXT_HAS_NO_OWNERDOC, null);
        }
        XObject xObjectExecute = this.m_arg0.execute(xPathContext);
        int type = xObjectExecute.getType();
        XNodeSet xNodeSet = new XNodeSet(xPathContext.getDTMManager());
        NodeSetDTM nodeSetDTMMutableNodeset = xNodeSet.mutableNodeset();
        if (4 == type) {
            DTMIterator dTMIteratorIter = xObjectExecute.iter();
            int iNextNode = dTMIteratorIter.nextNode();
            StringVector nodesByID = null;
            while (-1 != iNextNode) {
                String string = dTMIteratorIter.getDTM(iNextNode).getStringValue(iNextNode).toString();
                int iNextNode2 = dTMIteratorIter.nextNode();
                nodesByID = getNodesByID(xPathContext, document, string, nodesByID, nodeSetDTMMutableNodeset, -1 != iNextNode2);
                iNextNode = iNextNode2;
            }
        } else {
            if (-1 == type) {
                return xNodeSet;
            }
            getNodesByID(xPathContext, document, xObjectExecute.str(), null, nodeSetDTMMutableNodeset, false);
        }
        return xNodeSet;
    }
}
