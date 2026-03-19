package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncQname extends FunctionDef1Arg {
    static final long serialVersionUID = -1532307875532617380L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int arg0AsNode = getArg0AsNode(xPathContext);
        if (-1 != arg0AsNode) {
            String nodeNameX = xPathContext.getDTM(arg0AsNode).getNodeNameX(arg0AsNode);
            return nodeNameX == null ? XString.EMPTYSTRING : new XString(nodeNameX);
        }
        return XString.EMPTYSTRING;
    }
}
