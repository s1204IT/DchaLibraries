package org.apache.xalan.processor;

import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemVariable;
import org.xml.sax.SAXException;

class ProcessorGlobalVariableDecl extends ProcessorTemplateElem {
    static final long serialVersionUID = -5954332402269819582L;

    ProcessorGlobalVariableDecl() {
    }

    @Override
    protected void appendAndPush(StylesheetHandler stylesheetHandler, ElemTemplateElement elemTemplateElement) throws SAXException {
        stylesheetHandler.pushElemTemplateElement(elemTemplateElement);
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        ElemVariable elemVariable = (ElemVariable) stylesheetHandler.getElemTemplateElement();
        stylesheetHandler.getStylesheet().appendChild((ElemTemplateElement) elemVariable);
        stylesheetHandler.getStylesheet().setVariable(elemVariable);
        super.endElement(stylesheetHandler, str, str2, str3);
    }
}
