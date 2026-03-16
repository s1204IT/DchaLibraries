package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FuncRound extends FunctionOneArg {
    static final long serialVersionUID = -7970583902573826611L;

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        XObject obj = this.m_arg0.execute(xctxt);
        double val = obj.num();
        return (val < -0.5d || val >= XPath.MATCH_SCORE_QNAME) ? val == XPath.MATCH_SCORE_QNAME ? new XNumber(val) : new XNumber(Math.floor(0.5d + val)) : new XNumber(-0.0d);
    }
}
