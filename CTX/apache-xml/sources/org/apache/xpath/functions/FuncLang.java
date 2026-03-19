package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class FuncLang extends FunctionOneArg {
    static final long serialVersionUID = -7868705139354872185L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        boolean z;
        int attributeNode;
        int length;
        String str = this.m_arg0.execute(xPathContext).str();
        int currentNode = xPathContext.getCurrentNode();
        DTM dtm = xPathContext.getDTM(currentNode);
        while (true) {
            z = false;
            if (-1 == currentNode) {
                break;
            }
            if (1 == dtm.getNodeType(currentNode) && -1 != (attributeNode = dtm.getAttributeNode(currentNode, "http://www.w3.org/XML/1998/namespace", "lang"))) {
                String nodeValue = dtm.getNodeValue(attributeNode);
                if (nodeValue.toLowerCase().startsWith(str.toLowerCase()) && (nodeValue.length() == (length = str.length()) || nodeValue.charAt(length) == '-')) {
                    z = true;
                }
            } else {
                currentNode = dtm.getParent(currentNode);
            }
        }
        return z ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
}
