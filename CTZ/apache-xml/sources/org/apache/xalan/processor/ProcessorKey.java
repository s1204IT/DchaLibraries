package org.apache.xalan.processor;

import java.util.ArrayList;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.KeyDeclaration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorKey extends XSLTElementProcessor {
    static final long serialVersionUID = 4285205417566822979L;

    ProcessorKey() {
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws NoSuchMethodException, SAXException {
        KeyDeclaration keyDeclaration = new KeyDeclaration(stylesheetHandler.getStylesheet(), stylesheetHandler.nextUid());
        keyDeclaration.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
        keyDeclaration.setLocaterInfo(stylesheetHandler.getLocator());
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, keyDeclaration);
        stylesheetHandler.getStylesheet().setKey(keyDeclaration);
    }

    @Override
    void setPropertiesFromAttributes(StylesheetHandler stylesheetHandler, String str, Attributes attributes, ElemTemplateElement elemTemplateElement) throws NoSuchMethodException, SAXException {
        XSLTElementDef elemDef = getElemDef();
        ArrayList arrayList = new ArrayList();
        int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            String uri = attributes.getURI(i);
            String localName = attributes.getLocalName(i);
            XSLTAttributeDef attributeDef = elemDef.getAttributeDef(uri, localName);
            if (attributeDef == null) {
                stylesheetHandler.error(attributes.getQName(i) + "attribute is not allowed on the " + str + " element!", null);
            } else {
                if (attributes.getValue(i).indexOf("key(") >= 0) {
                    stylesheetHandler.error(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_KEY_CALL, null), null);
                }
                arrayList.add(attributeDef);
                attributeDef.setAttrValue(stylesheetHandler, uri, localName, attributes.getQName(i), attributes.getValue(i), elemTemplateElement);
            }
        }
        for (XSLTAttributeDef xSLTAttributeDef : elemDef.getAttributes()) {
            if (xSLTAttributeDef.getDefault() != null && !arrayList.contains(xSLTAttributeDef)) {
                xSLTAttributeDef.setDefAttrValue(stylesheetHandler, elemTemplateElement);
            }
            if (xSLTAttributeDef.getRequired() && !arrayList.contains(xSLTAttributeDef)) {
                stylesheetHandler.error(XSLMessages.createMessage(XSLTErrorResources.ER_REQUIRES_ATTRIB, new Object[]{str, xSLTAttributeDef.getName()}), null);
            }
        }
    }
}
