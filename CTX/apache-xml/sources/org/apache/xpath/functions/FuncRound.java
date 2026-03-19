package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FuncRound extends FunctionOneArg {
    static final long serialVersionUID = -7970583902573826611L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        double dNum = this.m_arg0.execute(xPathContext).num();
        return (dNum < -0.5d || dNum >= XPath.MATCH_SCORE_QNAME) ? dNum == XPath.MATCH_SCORE_QNAME ? new XNumber(dNum) : new XNumber(Math.floor(dNum + 0.5d)) : new XNumber(-0.0d);
    }
}
