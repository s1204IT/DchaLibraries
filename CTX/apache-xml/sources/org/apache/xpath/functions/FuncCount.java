package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FuncCount extends FunctionOneArg {
    static final long serialVersionUID = -7116225100474153751L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        DTMIterator dTMIteratorAsIterator = this.m_arg0.asIterator(xPathContext, xPathContext.getCurrentNode());
        int length = dTMIteratorAsIterator.getLength();
        dTMIteratorAsIterator.detach();
        return new XNumber(length);
    }
}
