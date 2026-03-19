package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

public class FuncSubstringBefore extends Function2Args {
    static final long serialVersionUID = 4110547161672431775L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String str = this.m_arg0.execute(xPathContext).str();
        int iIndexOf = str.indexOf(this.m_arg1.execute(xPathContext).str());
        return -1 == iIndexOf ? XString.EMPTYSTRING : new XString(str.substring(0, iIndexOf));
    }
}
