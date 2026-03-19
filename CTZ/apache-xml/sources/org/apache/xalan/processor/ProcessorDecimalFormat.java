package org.apache.xalan.processor;

import org.apache.xalan.templates.DecimalFormatProperties;
import org.apache.xalan.templates.ElemTemplateElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorDecimalFormat extends XSLTElementProcessor {
    static final long serialVersionUID = -5052904382662921627L;

    ProcessorDecimalFormat() {
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        DecimalFormatProperties decimalFormatProperties = new DecimalFormatProperties(stylesheetHandler.nextUid());
        decimalFormatProperties.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
        decimalFormatProperties.setLocaterInfo(stylesheetHandler.getLocator());
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, decimalFormatProperties);
        stylesheetHandler.getStylesheet().setDecimalFormat(decimalFormatProperties);
        stylesheetHandler.getStylesheet().appendChild((ElemTemplateElement) decimalFormatProperties);
    }
}
