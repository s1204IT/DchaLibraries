package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncLocalPart extends FunctionDef1Arg {
    static final long serialVersionUID = 7591798770325814746L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int arg0AsNode = getArg0AsNode(xPathContext);
        if (-1 == arg0AsNode) {
            return XString.EMPTYSTRING;
        }
        String localName = arg0AsNode != -1 ? xPathContext.getDTM(arg0AsNode).getLocalName(arg0AsNode) : "";
        if (localName.startsWith("#") || localName.equals("xmlns")) {
            return XString.EMPTYSTRING;
        }
        return new XString(localName);
    }
}
