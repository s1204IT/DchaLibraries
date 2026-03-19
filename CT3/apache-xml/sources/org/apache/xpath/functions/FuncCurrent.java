package org.apache.xpath.functions;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.LocPathIterator;
import org.apache.xpath.axes.PredicatedNodeTest;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.patterns.StepPattern;

public class FuncCurrent extends Function {
    static final long serialVersionUID = 5715316804877715008L;

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        ?? currentNodeList = xctxt.getCurrentNodeList();
        int currentNode = -1;
        if (currentNodeList != 0) {
            if (currentNodeList instanceof PredicatedNodeTest) {
                LocPathIterator iter = currentNodeList.getLocPathIterator();
                currentNode = iter.getCurrentContextNode();
            } else if (currentNodeList instanceof StepPattern) {
                throw new RuntimeException(XSLMessages.createMessage(XSLTErrorResources.ER_PROCESSOR_ERROR, null));
            }
        } else {
            currentNode = xctxt.getContextNode();
        }
        return new XNodeSet(currentNode, xctxt.getDTMManager());
    }

    @Override
    public void fixupVariables(Vector vars, int globalsSize) {
    }
}
