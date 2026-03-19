package org.apache.xalan.processor;

import org.apache.xalan.templates.ElemParam;
import org.apache.xalan.templates.ElemTemplateElement;
import org.xml.sax.SAXException;

class ProcessorGlobalParamDecl extends ProcessorTemplateElem {
    static final long serialVersionUID = 1900450872353587350L;

    ProcessorGlobalParamDecl() {
    }

    @Override
    protected void appendAndPush(StylesheetHandler stylesheetHandler, ElemTemplateElement elemTemplateElement) throws SAXException {
        stylesheetHandler.pushElemTemplateElement(elemTemplateElement);
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        ElemParam elemParam = (ElemParam) stylesheetHandler.getElemTemplateElement();
        stylesheetHandler.getStylesheet().appendChild((ElemTemplateElement) elemParam);
        stylesheetHandler.getStylesheet().setParam(elemParam);
        super.endElement(stylesheetHandler, str, str2, str3);
    }
}
