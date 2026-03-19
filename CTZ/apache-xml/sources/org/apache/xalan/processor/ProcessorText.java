package org.apache.xalan.processor;

import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemText;
import org.xml.sax.SAXException;

public class ProcessorText extends ProcessorTemplateElem {
    static final long serialVersionUID = 5170229307201307523L;

    @Override
    protected void appendAndPush(StylesheetHandler stylesheetHandler, ElemTemplateElement elemTemplateElement) throws SAXException {
        ((ProcessorCharacters) stylesheetHandler.getProcessorFor(null, "text()", "text")).setXslTextElement((ElemText) elemTemplateElement);
        stylesheetHandler.getElemTemplateElement().appendChild(elemTemplateElement);
        elemTemplateElement.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        ((ProcessorCharacters) stylesheetHandler.getProcessorFor(null, "text()", "text")).setXslTextElement(null);
    }
}
