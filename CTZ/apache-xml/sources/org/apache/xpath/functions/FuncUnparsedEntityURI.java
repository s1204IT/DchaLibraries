package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncUnparsedEntityURI extends FunctionOneArg {
    static final long serialVersionUID = 845309759097448178L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String str = this.m_arg0.execute(xPathContext).str();
        DTM dtm = xPathContext.getDTM(xPathContext.getCurrentNode());
        dtm.getDocument();
        return new XString(dtm.getUnparsedEntityURI(str));
    }
}
