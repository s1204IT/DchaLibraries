package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XString;
import org.apache.xpath.res.XPATHErrorResources;

public class FunctionDef1Arg extends FunctionOneArg {
    static final long serialVersionUID = 2325189412814149264L;

    protected int getArg0AsNode(XPathContext xPathContext) throws TransformerException {
        return this.m_arg0 == null ? xPathContext.getCurrentNode() : this.m_arg0.asNode(xPathContext);
    }

    public boolean Arg0IsNodesetExpr() {
        if (this.m_arg0 == null) {
            return true;
        }
        return this.m_arg0.isNodesetExpr();
    }

    protected XMLString getArg0AsString(XPathContext xPathContext) throws TransformerException {
        if (this.m_arg0 == null) {
            int currentNode = xPathContext.getCurrentNode();
            if (-1 == currentNode) {
                return XString.EMPTYSTRING;
            }
            return xPathContext.getDTM(currentNode).getStringValue(currentNode);
        }
        return this.m_arg0.execute(xPathContext).xstr();
    }

    protected double getArg0AsNumber(XPathContext xPathContext) throws TransformerException {
        if (this.m_arg0 == null) {
            int currentNode = xPathContext.getCurrentNode();
            if (-1 == currentNode) {
                return XPath.MATCH_SCORE_QNAME;
            }
            return xPathContext.getDTM(currentNode).getStringValue(currentNode).toDouble();
        }
        return this.m_arg0.execute(xPathContext).num();
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i > 1) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ZERO_OR_ONE, null));
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (this.m_arg0 == null) {
            return false;
        }
        return super.canTraverseOutsideSubtree();
    }
}
