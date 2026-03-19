package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncGenerateId extends FunctionDef1Arg {
    static final long serialVersionUID = 973544842091724273L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int arg0AsNode = getArg0AsNode(xPathContext);
        if (-1 != arg0AsNode) {
            return new XString("N" + Integer.toHexString(arg0AsNode).toUpperCase());
        }
        return XString.EMPTYSTRING;
    }
}
