package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xml.utils.Constants;
import org.apache.xpath.ExtensionsProvider;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.FunctionTable;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

public class FuncExtFunctionAvailable extends FunctionOneArg {
    static final long serialVersionUID = 5118814314918592241L;
    private transient FunctionTable m_functionTable = null;

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
        if (!str.equals(Constants.S_XSLNAMESPACEURL)) {
            return ((ExtensionsProvider) xPathContext.getOwnerObject()).functionAvailable(str, str2) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
        }
        try {
            if (this.m_functionTable == null) {
                this.m_functionTable = new FunctionTable();
            }
            return this.m_functionTable.functionAvailable(str2) ? XBoolean.S_TRUE : XBoolean.S_FALSE;
        } catch (Exception e) {
            return XBoolean.S_FALSE;
        }
    }

    public void setFunctionTable(FunctionTable functionTable) {
        this.m_functionTable = functionTable;
    }
}
