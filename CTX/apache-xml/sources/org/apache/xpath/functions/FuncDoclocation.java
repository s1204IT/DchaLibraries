package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncDoclocation extends FunctionDef1Arg {
    static final long serialVersionUID = 7469213946343568769L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String documentBaseURI;
        int arg0AsNode = getArg0AsNode(xPathContext);
        if (-1 != arg0AsNode) {
            DTM dtm = xPathContext.getDTM(arg0AsNode);
            if (11 == dtm.getNodeType(arg0AsNode)) {
                arg0AsNode = dtm.getFirstChild(arg0AsNode);
            }
            if (-1 != arg0AsNode) {
                documentBaseURI = dtm.getDocumentBaseURI();
            } else {
                documentBaseURI = null;
            }
        }
        if (documentBaseURI == null) {
            documentBaseURI = "";
        }
        return new XString(documentBaseURI);
    }
}
