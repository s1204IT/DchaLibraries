package org.apache.xalan.extensions;

import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.functions.FuncExtFunction;
import org.apache.xpath.functions.FuncExtFunctionAvailable;
import org.apache.xpath.functions.Function;

public class ExpressionVisitor extends XPathVisitor {
    private StylesheetRoot m_sroot;

    public ExpressionVisitor(StylesheetRoot stylesheetRoot) {
        this.m_sroot = stylesheetRoot;
    }

    @Override
    public boolean visitFunction(ExpressionOwner expressionOwner, Function function) {
        if (function instanceof FuncExtFunction) {
            this.m_sroot.getExtensionNamespacesManager().registerExtension(((FuncExtFunction) function).getNamespace());
            return true;
        }
        if (function instanceof FuncExtFunctionAvailable) {
            String string = ((FuncExtFunctionAvailable) function).getArg0().toString();
            if (string.indexOf(":") > 0) {
                this.m_sroot.getExtensionNamespacesManager().registerExtension(this.m_sroot.getNamespaceForPrefix(string.substring(0, string.indexOf(":"))));
                return true;
            }
            return true;
        }
        return true;
    }
}
