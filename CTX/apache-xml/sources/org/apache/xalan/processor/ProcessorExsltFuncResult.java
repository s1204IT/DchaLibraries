package org.apache.xalan.processor;

import org.apache.xalan.templates.ElemExsltFuncResult;
import org.apache.xalan.templates.ElemExsltFunction;
import org.apache.xalan.templates.ElemParam;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemVariable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProcessorExsltFuncResult extends ProcessorTemplateElem {
    static final long serialVersionUID = 6451230911473482423L;

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        super.startElement(stylesheetHandler, str, str2, str3, attributes);
        ElemTemplateElement parentElem = stylesheetHandler.getElemTemplateElement().getParentElem();
        while (parentElem != null && !(parentElem instanceof ElemExsltFunction)) {
            if ((parentElem instanceof ElemVariable) || (parentElem instanceof ElemParam) || (parentElem instanceof ElemExsltFuncResult)) {
                stylesheetHandler.error("func:result cannot appear within a variable, parameter, or another func:result.", new SAXException("func:result cannot appear within a variable, parameter, or another func:result."));
            }
            parentElem = parentElem.getParentElem();
        }
        if (parentElem == null) {
            stylesheetHandler.error("func:result must appear in a func:function element", new SAXException("func:result must appear in a func:function element"));
        }
    }
}
