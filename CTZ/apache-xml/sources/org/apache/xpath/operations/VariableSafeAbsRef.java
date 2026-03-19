package org.apache.xpath.operations;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMManager;
import org.apache.xpath.Expression;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class VariableSafeAbsRef extends Variable {
    static final long serialVersionUID = -9174661990819967452L;

    @Override
    public XObject execute(XPathContext xPathContext, boolean z) throws TransformerException {
        XNodeSet xNodeSet = (XNodeSet) super.execute(xPathContext, z);
        DTMManager dTMManager = xPathContext.getDTMManager();
        int contextNode = xPathContext.getContextNode();
        if (dTMManager.getDTM(xNodeSet.getRoot()).getDocument() != dTMManager.getDTM(contextNode).getDocument()) {
            return (XNodeSet) ((Expression) xNodeSet.getContainedIter()).asIterator(xPathContext, contextNode);
        }
        return xNodeSet;
    }
}
