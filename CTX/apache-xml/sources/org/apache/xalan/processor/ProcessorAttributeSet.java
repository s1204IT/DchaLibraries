package org.apache.xalan.processor;

import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.ElemAttributeSet;
import org.apache.xalan.templates.ElemTemplateElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorAttributeSet extends XSLTElementProcessor {
    static final long serialVersionUID = -6473739251316787552L;

    ProcessorAttributeSet() {
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        ElemAttributeSet elemAttributeSet = new ElemAttributeSet();
        elemAttributeSet.setLocaterInfo(stylesheetHandler.getLocator());
        try {
            elemAttributeSet.setPrefixes(stylesheetHandler.getNamespaceSupport());
            elemAttributeSet.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
            setPropertiesFromAttributes(stylesheetHandler, str3, attributes, elemAttributeSet);
            stylesheetHandler.getStylesheet().setAttributeSet(elemAttributeSet);
            stylesheetHandler.getElemTemplateElement().appendChild((ElemTemplateElement) elemAttributeSet);
            stylesheetHandler.pushElemTemplateElement(elemAttributeSet);
        } catch (TransformerException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        stylesheetHandler.popElemTemplateElement();
    }
}
