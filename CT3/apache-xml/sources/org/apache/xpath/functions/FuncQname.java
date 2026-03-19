package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncQname extends FunctionDef1Arg {
    static final long serialVersionUID = -1532307875532617380L;

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        int context = getArg0AsNode(xctxt);
        if (-1 != context) {
            DTM dtm = xctxt.getDTM(context);
            String qname = dtm.getNodeNameX(context);
            if (qname != null) {
                XObject val = new XString(qname);
                return val;
            }
            XObject val2 = XString.EMPTYSTRING;
            return val2;
        }
        XObject val3 = XString.EMPTYSTRING;
        return val3;
    }
}
