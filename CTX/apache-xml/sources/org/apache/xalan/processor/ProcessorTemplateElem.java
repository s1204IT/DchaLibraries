package org.apache.xalan.processor;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemTemplateElement;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProcessorTemplateElem extends XSLTElementProcessor {
    static final long serialVersionUID = 8344994001943407235L;

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        ElemTemplateElement elemTemplateElement;
        super.startElement(stylesheetHandler, str, str2, str3, attributes);
        try {
            try {
                elemTemplateElement = (ElemTemplateElement) getElemDef().getClassObject().newInstance();
            } catch (IllegalAccessException e) {
                e = e;
                elemTemplateElement = null;
            } catch (InstantiationException e2) {
                e = e2;
                elemTemplateElement = null;
            }
            try {
                elemTemplateElement.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
                elemTemplateElement.setLocaterInfo(stylesheetHandler.getLocator());
                elemTemplateElement.setPrefixes(stylesheetHandler.getNamespaceSupport());
            } catch (IllegalAccessException e3) {
                e = e3;
                stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMTMPL, null, e);
            } catch (InstantiationException e4) {
                e = e4;
                stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMTMPL, null, e);
            }
            setPropertiesFromAttributes(stylesheetHandler, str3, attributes, elemTemplateElement);
            appendAndPush(stylesheetHandler, elemTemplateElement);
        } catch (TransformerException e5) {
            throw new SAXException(e5);
        }
    }

    protected void appendAndPush(StylesheetHandler stylesheetHandler, ElemTemplateElement elemTemplateElement) throws SAXException {
        ElemTemplateElement elemTemplateElement2 = stylesheetHandler.getElemTemplateElement();
        if (elemTemplateElement2 != null) {
            elemTemplateElement2.appendChild(elemTemplateElement);
            stylesheetHandler.pushElemTemplateElement(elemTemplateElement);
        }
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        super.endElement(stylesheetHandler, str, str2, str3);
        stylesheetHandler.popElemTemplateElement().setEndLocaterInfo(stylesheetHandler.getLocator());
    }
}
