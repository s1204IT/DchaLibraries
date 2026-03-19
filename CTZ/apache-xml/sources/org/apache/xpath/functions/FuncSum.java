package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FuncSum extends FunctionOneArg {
    static final long serialVersionUID = -2719049259574677519L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        DTMIterator dTMIteratorAsIterator = this.m_arg0.asIterator(xPathContext, xPathContext.getCurrentNode());
        double d = XPath.MATCH_SCORE_QNAME;
        while (true) {
            int iNextNode = dTMIteratorAsIterator.nextNode();
            if (-1 != iNextNode) {
                XMLString stringValue = dTMIteratorAsIterator.getDTM(iNextNode).getStringValue(iNextNode);
                if (stringValue != null) {
                    d += stringValue.toDouble();
                }
            } else {
                dTMIteratorAsIterator.detach();
                return new XNumber(d);
            }
        }
    }
}
