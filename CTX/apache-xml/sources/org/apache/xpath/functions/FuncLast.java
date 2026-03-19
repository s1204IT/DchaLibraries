package org.apache.xpath.functions;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.XPathContext;
import org.apache.xpath.axes.SubContextList;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FuncLast extends Function {
    static final long serialVersionUID = 9205812403085432943L;
    private boolean m_isTopLevel;

    @Override
    public void postCompileStep(Compiler compiler) {
        this.m_isTopLevel = compiler.getLocationPathDepth() == -1;
    }

    public int getCountOfContextNodeList(XPathContext xPathContext) throws TransformerException {
        SubContextList subContextList = this.m_isTopLevel ? null : xPathContext.getSubContextList();
        if (subContextList != null) {
            return subContextList.getLastPos(xPathContext);
        }
        DTMIterator contextNodeList = xPathContext.getContextNodeList();
        if (contextNodeList != null) {
            return contextNodeList.getLength();
        }
        return 0;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return new XNumber(getCountOfContextNodeList(xPathContext));
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
    }
}
