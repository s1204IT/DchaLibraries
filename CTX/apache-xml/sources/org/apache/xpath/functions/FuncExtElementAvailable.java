package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.Constants;
import org.apache.xml.utils.QName;
import org.apache.xpath.ExtensionsProvider;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class FuncExtElementAvailable extends FunctionOneArg {
    static final long serialVersionUID = -472533699257968546L;

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        String str;
        String str2 = this.m_arg0.execute(xPathContext).str();
        int iIndexOf = str2.indexOf(58);
        if (iIndexOf < 0) {
            str = Constants.S_XSLNAMESPACEURL;
        } else {
            String namespaceForPrefix = xPathContext.getNamespaceContext().getNamespaceForPrefix(str2.substring(0, iIndexOf));
            if (namespaceForPrefix == null) {
                return XBoolean.S_FALSE;
            }
            str2 = str2.substring(iIndexOf + 1);
            str = namespaceForPrefix;
        }
        if (!str.equals(Constants.S_XSLNAMESPACEURL) && !str.equals("http://xml.apache.org/xalan")) {
            return ((ExtensionsProvider) xPathContext.getOwnerObject()).elementAvailable(str, str2) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
        }
        try {
            return ((TransformerImpl) xPathContext.getOwnerObject()).getStylesheet().getAvailableElements().containsKey(new QName(str, str2)) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
        } catch (Exception e) {
            return XBoolean.S_FALSE;
        }
    }
}
