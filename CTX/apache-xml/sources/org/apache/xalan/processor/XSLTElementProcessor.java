package org.apache.xalan.processor;

import java.util.ArrayList;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xml.utils.IntStack;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class XSLTElementProcessor extends ElemTemplateElement {
    static final long serialVersionUID = 5597421564955304421L;
    private XSLTElementDef m_elemDef;
    private IntStack m_savedLastOrder;

    XSLTElementProcessor() {
    }

    XSLTElementDef getElemDef() {
        return this.m_elemDef;
    }

    void setElemDef(XSLTElementDef xSLTElementDef) {
        this.m_elemDef = xSLTElementDef;
    }

    public InputSource resolveEntity(StylesheetHandler stylesheetHandler, String str, String str2) throws SAXException {
        return null;
    }

    public void notationDecl(StylesheetHandler stylesheetHandler, String str, String str2, String str3) {
    }

    public void unparsedEntityDecl(StylesheetHandler stylesheetHandler, String str, String str2, String str3, String str4) {
    }

    public void startNonText(StylesheetHandler stylesheetHandler) throws SAXException {
    }

    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        if (this.m_savedLastOrder == null) {
            this.m_savedLastOrder = new IntStack();
        }
        this.m_savedLastOrder.push(getElemDef().getLastOrder());
        getElemDef().setLastOrder(-1);
    }

    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        if (this.m_savedLastOrder != null && !this.m_savedLastOrder.empty()) {
            getElemDef().setLastOrder(this.m_savedLastOrder.pop());
        }
        if (!getElemDef().getRequiredFound()) {
            stylesheetHandler.error(XSLTErrorResources.ER_REQUIRED_ELEM_NOT_FOUND, new Object[]{getElemDef().getRequiredElem()}, null);
        }
    }

    public void characters(StylesheetHandler stylesheetHandler, char[] cArr, int i, int i2) throws SAXException {
        stylesheetHandler.error(XSLTErrorResources.ER_CHARS_NOT_ALLOWED, null, null);
    }

    public void ignorableWhitespace(StylesheetHandler stylesheetHandler, char[] cArr, int i, int i2) throws SAXException {
    }

    public void processingInstruction(StylesheetHandler stylesheetHandler, String str, String str2) throws SAXException {
    }

    public void skippedEntity(StylesheetHandler stylesheetHandler, String str) throws SAXException {
    }

    void setPropertiesFromAttributes(StylesheetHandler stylesheetHandler, String str, Attributes attributes, ElemTemplateElement elemTemplateElement) throws NoSuchMethodException, SAXException {
        setPropertiesFromAttributes(stylesheetHandler, str, attributes, elemTemplateElement, true);
    }

    Attributes setPropertiesFromAttributes(StylesheetHandler stylesheetHandler, String str, Attributes attributes, ElemTemplateElement elemTemplateElement, boolean z) throws NoSuchMethodException, SAXException {
        int i;
        int i2;
        ArrayList arrayList;
        ArrayList arrayList2;
        Attributes attributes2 = attributes;
        XSLTElementDef elemDef = getElemDef();
        char c = 0;
        char c2 = 1;
        boolean z2 = (stylesheetHandler.getStylesheet() != null && stylesheetHandler.getStylesheet().getCompatibleMode()) || !z;
        AttributesImpl attributesImpl = z2 ? new AttributesImpl() : null;
        ArrayList arrayList3 = new ArrayList();
        ArrayList arrayList4 = new ArrayList();
        int length = attributes.getLength();
        int i3 = 0;
        while (i3 < length) {
            String uri = attributes2.getURI(i3);
            if (uri != null && uri.length() == 0 && (attributes2.getQName(i3).startsWith(Constants.ATTRNAME_XMLNS) || attributes2.getQName(i3).equals("xmlns"))) {
                uri = "http://www.w3.org/XML/1998/namespace";
            }
            String localName = attributes2.getLocalName(i3);
            XSLTAttributeDef attributeDef = elemDef.getAttributeDef(uri, localName);
            if (attributeDef != null) {
                i = i3;
                i2 = length;
                if (stylesheetHandler.getStylesheetProcessor() == null) {
                    System.out.println("stylesheet processor null");
                }
                if (attributeDef.getName().compareTo("*") == 0 && stylesheetHandler.getStylesheetProcessor().isSecureProcessing()) {
                    Object[] objArr = new Object[2];
                    objArr[c] = attributes2.getQName(i);
                    objArr[1] = str;
                    stylesheetHandler.error(XSLTErrorResources.ER_ATTR_NOT_ALLOWED, objArr, null);
                    arrayList = arrayList3;
                    arrayList2 = arrayList4;
                } else {
                    String qName = attributes2.getQName(i);
                    String value = attributes2.getValue(i);
                    arrayList = arrayList3;
                    arrayList2 = arrayList4;
                    if (attributeDef.setAttrValue(stylesheetHandler, uri, localName, qName, value, elemTemplateElement)) {
                        arrayList.add(attributeDef);
                    } else {
                        arrayList2.add(attributeDef);
                    }
                }
            } else if (z2) {
                i = i3;
                i2 = length;
                attributesImpl.addAttribute(uri, localName, attributes2.getQName(i3), attributes2.getType(i3), attributes2.getValue(i3));
                arrayList = arrayList3;
                arrayList2 = arrayList4;
            } else {
                Object[] objArr2 = new Object[2];
                objArr2[c] = attributes2.getQName(i3);
                objArr2[c2] = str;
                stylesheetHandler.error(XSLTErrorResources.ER_ATTR_NOT_ALLOWED, objArr2, null);
                arrayList = arrayList3;
                i = i3;
                i2 = length;
                arrayList2 = arrayList4;
            }
            i3 = i + 1;
            arrayList4 = arrayList2;
            arrayList3 = arrayList;
            length = i2;
            attributes2 = attributes;
            c = 0;
            c2 = 1;
        }
        ArrayList arrayList5 = arrayList3;
        ArrayList arrayList6 = arrayList4;
        for (XSLTAttributeDef xSLTAttributeDef : elemDef.getAttributes()) {
            if (xSLTAttributeDef.getDefault() != null && !arrayList5.contains(xSLTAttributeDef)) {
                xSLTAttributeDef.setDefAttrValue(stylesheetHandler, elemTemplateElement);
            }
            if (xSLTAttributeDef.getRequired() && !arrayList5.contains(xSLTAttributeDef) && !arrayList6.contains(xSLTAttributeDef)) {
                stylesheetHandler.error(XSLMessages.createMessage(XSLTErrorResources.ER_REQUIRES_ATTRIB, new Object[]{str, xSLTAttributeDef.getName()}), null);
            }
        }
        return attributesImpl;
    }
}
