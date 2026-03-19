package org.apache.xpath.patterns;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.objects.XObject;

public class ContextMatchStepPattern extends StepPattern {
    static final long serialVersionUID = -1888092779313211942L;

    public ContextMatchStepPattern(int i, int i2) {
        super(-1, i, i2);
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        if (xPathContext.getIteratorRoot() == xPathContext.getCurrentNode()) {
            return getStaticScore();
        }
        return SCORE_NONE;
    }

    public XObject executeRelativePathPattern(XPathContext xPathContext, StepPattern stepPattern) throws TransformerException {
        XObject xObjectExecute = NodeTest.SCORE_NONE;
        int currentNode = xPathContext.getCurrentNode();
        DTM dtm = xPathContext.getDTM(currentNode);
        if (dtm != null) {
            xPathContext.getCurrentNode();
            int i = this.m_axis;
            boolean zIsDownwardAxisOfMany = WalkerFactory.isDownwardAxisOfMany(i);
            boolean z = dtm.getNodeType(xPathContext.getIteratorRoot()) == 2;
            if (11 == i && z) {
                i = 15;
            }
            DTMAxisTraverser axisTraverser = dtm.getAxisTraverser(i);
            for (int iFirst = axisTraverser.first(currentNode); -1 != iFirst; iFirst = axisTraverser.next(currentNode, iFirst)) {
                try {
                    xPathContext.pushCurrentNode(iFirst);
                    xObjectExecute = execute(xPathContext);
                    if (xObjectExecute != NodeTest.SCORE_NONE) {
                        if (executePredicates(xPathContext, dtm, currentNode)) {
                            return xObjectExecute;
                        }
                        xObjectExecute = NodeTest.SCORE_NONE;
                    }
                    if (zIsDownwardAxisOfMany && z && 1 == dtm.getNodeType(iFirst)) {
                        XObject xObjectExecute2 = xObjectExecute;
                        int i2 = 2;
                        for (int i3 = 0; i3 < 2; i3++) {
                            DTMAxisTraverser axisTraverser2 = dtm.getAxisTraverser(i2);
                            for (int iFirst2 = axisTraverser2.first(iFirst); -1 != iFirst2; iFirst2 = axisTraverser2.next(iFirst, iFirst2)) {
                                xPathContext.pushCurrentNode(iFirst2);
                                xObjectExecute2 = execute(xPathContext);
                                if (xObjectExecute2 != NodeTest.SCORE_NONE && xObjectExecute2 != NodeTest.SCORE_NONE) {
                                    xPathContext.popCurrentNode();
                                    return xObjectExecute2;
                                }
                                xPathContext.popCurrentNode();
                            }
                            i2 = 9;
                        }
                        xObjectExecute = xObjectExecute2;
                    }
                    xPathContext.popCurrentNode();
                } catch (Throwable th) {
                    throw th;
                } finally {
                    xPathContext.popCurrentNode();
                }
            }
        }
        return xObjectExecute;
    }
}
