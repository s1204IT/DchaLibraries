package org.apache.xpath.functions;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.PredicatedNodeTest;
import org.apache.xpath.axes.SubContextList;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.patterns.StepPattern;

public class FuncCurrent extends Function {
    static final long serialVersionUID = 5715316804877715008L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int contextNode;
        SubContextList currentNodeList = xPathContext.getCurrentNodeList();
        if (currentNodeList != null) {
            if (currentNodeList instanceof PredicatedNodeTest) {
                contextNode = ((PredicatedNodeTest) currentNodeList).getLocPathIterator().getCurrentContextNode();
            } else {
                if (currentNodeList instanceof StepPattern) {
                    throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSOR_ERROR, null));
                }
                contextNode = -1;
            }
        } else {
            contextNode = xPathContext.getContextNode();
        }
        return new XNodeSet(contextNode, xPathContext.getDTMManager());
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
    }
}
