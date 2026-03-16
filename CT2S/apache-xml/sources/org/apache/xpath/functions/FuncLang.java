package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class FuncLang extends FunctionOneArg {
    static final long serialVersionUID = -7868705139354872185L;

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        int langAttr;
        int valLen;
        String lang = this.m_arg0.execute(xctxt).str();
        int parent = xctxt.getCurrentNode();
        boolean isLang = false;
        DTM dtm = xctxt.getDTM(parent);
        while (true) {
            if (-1 == parent) {
                break;
            }
            if (1 == dtm.getNodeType(parent) && -1 != (langAttr = dtm.getAttributeNode(parent, "http://www.w3.org/XML/1998/namespace", "lang"))) {
                String langVal = dtm.getNodeValue(langAttr);
                if (langVal.toLowerCase().startsWith(lang.toLowerCase()) && (langVal.length() == (valLen = lang.length()) || langVal.charAt(valLen) == '-')) {
                    isLang = true;
                }
            } else {
                parent = dtm.getParent(parent);
            }
        }
        return isLang ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
}
