package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XString;
import org.apache.xpath.res.XPATHErrorResources;

public class FunctionDef1Arg extends FunctionOneArg {
    static final long serialVersionUID = 2325189412814149264L;

    protected int getArg0AsNode(XPathContext xctxt) throws TransformerException {
        return this.m_arg0 == null ? xctxt.getCurrentNode() : this.m_arg0.asNode(xctxt);
    }

    public boolean Arg0IsNodesetExpr() {
        if (this.m_arg0 == null) {
            return true;
        }
        return this.m_arg0.isNodesetExpr();
    }

    protected XMLString getArg0AsString(XPathContext xctxt) throws TransformerException {
        if (this.m_arg0 == null) {
            int currentNode = xctxt.getCurrentNode();
            if (-1 == currentNode) {
                return XString.EMPTYSTRING;
            }
            DTM dtm = xctxt.getDTM(currentNode);
            return dtm.getStringValue(currentNode);
        }
        return this.m_arg0.execute(xctxt).xstr();
    }

    protected double getArg0AsNumber(XPathContext xctxt) throws TransformerException {
        if (this.m_arg0 == null) {
            int currentNode = xctxt.getCurrentNode();
            if (-1 == currentNode) {
                return XPath.MATCH_SCORE_QNAME;
            }
            DTM dtm = xctxt.getDTM(currentNode);
            XMLString str = dtm.getStringValue(currentNode);
            return str.toDouble();
        }
        return this.m_arg0.execute(xctxt).num();
    }

    @Override
    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
        if (argNum > 1) {
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
