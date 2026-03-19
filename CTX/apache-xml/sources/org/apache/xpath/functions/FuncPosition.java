package org.apache.xpath.functions;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.SubContextList;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FuncPosition extends Function {
    static final long serialVersionUID = -9092846348197271582L;
    private boolean m_isTopLevel;

    @Override
    public void postCompileStep(Compiler compiler) {
        this.m_isTopLevel = compiler.getLocationPathDepth() == -1;
    }

    public int getPositionInContextNodeList(XPathContext xPathContext) {
        int iNextNode;
        SubContextList subContextList = this.m_isTopLevel ? null : xPathContext.getSubContextList();
        if (subContextList != null) {
            return subContextList.getProximityPosition(xPathContext);
        }
        DTMIterator contextNodeList = xPathContext.getContextNodeList();
        if (contextNodeList == null) {
            return -1;
        }
        if (contextNodeList.getCurrentNode() == -1) {
            if (contextNodeList.getCurrentPos() == 0) {
                return 0;
            }
            try {
                contextNodeList = contextNodeList.cloneWithReset();
                int contextNode = xPathContext.getContextNode();
                do {
                    iNextNode = contextNodeList.nextNode();
                    if (-1 == iNextNode) {
                        break;
                    }
                } while (iNextNode != contextNode);
            } catch (CloneNotSupportedException e) {
                throw new WrappedRuntimeException(e);
            }
        }
        return contextNodeList.getCurrentPos();
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return new XNumber(getPositionInContextNodeList(xPathContext));
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
    }
}
