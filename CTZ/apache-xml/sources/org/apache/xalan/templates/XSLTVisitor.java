package org.apache.xalan.templates;

import org.apache.xpath.XPathVisitor;

public class XSLTVisitor extends XPathVisitor {
    public boolean visitInstruction(ElemTemplateElement elemTemplateElement) {
        return true;
    }

    public boolean visitStylesheet(ElemTemplateElement elemTemplateElement) {
        return true;
    }

    public boolean visitTopLevelInstruction(ElemTemplateElement elemTemplateElement) {
        return true;
    }

    public boolean visitTopLevelVariableOrParamDecl(ElemTemplateElement elemTemplateElement) {
        return true;
    }

    public boolean visitVariableOrParamDecl(ElemVariable elemVariable) {
        return true;
    }

    public boolean visitLiteralResultElement(ElemLiteralResult elemLiteralResult) {
        return true;
    }

    public boolean visitAVT(AVT avt) {
        return true;
    }

    public boolean visitExtensionElement(ElemExtensionCall elemExtensionCall) {
        return true;
    }
}
