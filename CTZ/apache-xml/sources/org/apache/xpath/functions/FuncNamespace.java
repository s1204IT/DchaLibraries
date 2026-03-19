package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncNamespace extends FunctionDef1Arg {
    static final long serialVersionUID = -4695674566722321237L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String namespaceURI;
        int arg0AsNode = getArg0AsNode(xPathContext);
        if (arg0AsNode != -1) {
            DTM dtm = xPathContext.getDTM(arg0AsNode);
            short nodeType = dtm.getNodeType(arg0AsNode);
            if (nodeType == 1) {
                namespaceURI = dtm.getNamespaceURI(arg0AsNode);
            } else if (nodeType == 2) {
                String nodeName = dtm.getNodeName(arg0AsNode);
                if (nodeName.startsWith(Constants.ATTRNAME_XMLNS) || nodeName.equals("xmlns")) {
                    return XString.EMPTYSTRING;
                }
                namespaceURI = dtm.getNamespaceURI(arg0AsNode);
            } else {
                return XString.EMPTYSTRING;
            }
            return namespaceURI == null ? XString.EMPTYSTRING : new XString(namespaceURI);
        }
        return XString.EMPTYSTRING;
    }
}
