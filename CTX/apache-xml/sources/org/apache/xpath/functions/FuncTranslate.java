package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncTranslate extends Function3Args {
    static final long serialVersionUID = -1672834340026116482L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String str = this.m_arg0.execute(xPathContext).str();
        String str2 = this.m_arg1.execute(xPathContext).str();
        String str3 = this.m_arg2.execute(xPathContext).str();
        int length = str.length();
        int length2 = str3.length();
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            int iIndexOf = str2.indexOf(cCharAt);
            if (iIndexOf < 0) {
                stringBuffer.append(cCharAt);
            } else if (iIndexOf < length2) {
                stringBuffer.append(str3.charAt(iIndexOf));
            }
        }
        return new XString(stringBuffer.toString());
    }
}
