package org.apache.xalan.processor;

import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemExtensionCall;
import org.apache.xalan.templates.ElemLiteralResult;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.templates.XMLNSDecl;
import org.apache.xml.utils.Constants;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.XPath;
import org.apache.xpath.compiler.PsuedoNames;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ProcessorLRE extends ProcessorTemplateElem {
    static final long serialVersionUID = -1490218021772101404L;

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        Attributes attributes2;
        boolean z;
        boolean z2;
        ElemTemplateElement elemExtensionCall;
        int i;
        AttributesImpl attributesImpl;
        int i2;
        try {
            ElemTemplateElement elemTemplateElement = stylesheetHandler.getElemTemplateElement();
            if (elemTemplateElement == null) {
                XSLTElementProcessor xSLTElementProcessorPopProcessor = stylesheetHandler.popProcessor();
                XSLTElementProcessor processorFor = stylesheetHandler.getProcessorFor(Constants.S_XSLNAMESPACEURL, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, "xsl:stylesheet");
                stylesheetHandler.pushProcessor(xSLTElementProcessorPopProcessor);
                try {
                    Stylesheet stylesheetRoot = getStylesheetRoot(stylesheetHandler);
                    SAXSourceLocator sAXSourceLocator = new SAXSourceLocator();
                    SAXSourceLocator locator = stylesheetHandler.getLocator();
                    if (locator != null) {
                        sAXSourceLocator.setLineNumber(locator.getLineNumber());
                        sAXSourceLocator.setColumnNumber(locator.getColumnNumber());
                        sAXSourceLocator.setPublicId(locator.getPublicId());
                        sAXSourceLocator.setSystemId(locator.getSystemId());
                    }
                    stylesheetRoot.setLocaterInfo(sAXSourceLocator);
                    stylesheetRoot.setPrefixes(stylesheetHandler.getNamespaceSupport());
                    stylesheetHandler.pushStylesheet(stylesheetRoot);
                    AttributesImpl attributesImpl2 = new AttributesImpl();
                    AttributesImpl attributesImpl3 = new AttributesImpl();
                    int length = attributes.getLength();
                    int i3 = 0;
                    while (i3 < length) {
                        String localName = attributes.getLocalName(i3);
                        String uri = attributes.getURI(i3);
                        String value = attributes.getValue(i3);
                        if (uri != null) {
                            i = length;
                            if (uri.equals(Constants.S_XSLNAMESPACEURL)) {
                                attributesImpl = attributesImpl3;
                                attributesImpl2.addAttribute(null, localName, localName, attributes.getType(i3), attributes.getValue(i3));
                                i2 = i3;
                            }
                            i3 = i2 + 1;
                            length = i;
                            attributesImpl3 = attributesImpl;
                        } else {
                            i = length;
                        }
                        attributesImpl = attributesImpl3;
                        int i4 = i3;
                        if ((localName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS) || localName.equals("xmlns")) && value.equals(Constants.S_XSLNAMESPACEURL)) {
                            i2 = i4;
                        } else {
                            i2 = i4;
                            attributesImpl.addAttribute(uri, localName, attributes.getQName(i4), attributes.getType(i4), attributes.getValue(i4));
                        }
                        i3 = i2 + 1;
                        length = i;
                        attributesImpl3 = attributesImpl;
                    }
                    Attributes attributes3 = attributesImpl3;
                    try {
                        processorFor.setPropertiesFromAttributes(stylesheetHandler, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, attributesImpl2, stylesheetRoot);
                        stylesheetHandler.pushElemTemplateElement(stylesheetRoot);
                        ElemTemplate elemTemplate = new ElemTemplate();
                        elemTemplate.setLocaterInfo(sAXSourceLocator);
                        appendAndPush(stylesheetHandler, elemTemplate);
                        elemTemplate.setMatch(new XPath(PsuedoNames.PSEUDONAME_ROOT, stylesheetRoot, stylesheetRoot, 1, stylesheetHandler.getStylesheetProcessor().getErrorListener()));
                        stylesheetRoot.setTemplate(elemTemplate);
                        elemTemplateElement = stylesheetHandler.getElemTemplateElement();
                        attributes2 = attributes3;
                        z = true;
                        z2 = true;
                    } catch (Exception e) {
                        if (stylesheetRoot.getDeclaredPrefixes() != null && declaredXSLNS(stylesheetRoot)) {
                            throw new SAXException(e);
                        }
                        throw new SAXException(XSLMessages.createWarning(XSLTErrorResources.WG_OLD_XSLT_NS, null));
                    }
                } catch (TransformerConfigurationException e2) {
                    throw new TransformerException(e2);
                }
            } else {
                attributes2 = attributes;
                z = false;
                z2 = false;
            }
            Class classObject = getElemDef().getClassObject();
            boolean z3 = false;
            boolean z4 = false;
            boolean zContainsExtensionElementURI = false;
            while (elemTemplateElement != null) {
                if (elemTemplateElement instanceof ElemLiteralResult) {
                    zContainsExtensionElementURI = ((ElemLiteralResult) elemTemplateElement).containsExtensionElementURI(str);
                } else if (elemTemplateElement instanceof Stylesheet) {
                    boolean zContainsExtensionElementURI2 = ((Stylesheet) elemTemplateElement).containsExtensionElementURI(str);
                    if (zContainsExtensionElementURI2 || str == null || !(str.equals("http://xml.apache.org/xalan") || str.equals(Constants.S_BUILTIN_OLD_EXTENSIONS_URL))) {
                        zContainsExtensionElementURI = zContainsExtensionElementURI2;
                        z4 = true;
                    } else {
                        zContainsExtensionElementURI = zContainsExtensionElementURI2;
                        z3 = true;
                    }
                }
                if (zContainsExtensionElementURI) {
                    break;
                } else {
                    elemTemplateElement = elemTemplateElement.getParentElem();
                }
            }
            try {
                elemExtensionCall = zContainsExtensionElementURI ? new ElemExtensionCall() : (!z3 && z4) ? (ElemTemplateElement) classObject.newInstance() : (ElemTemplateElement) classObject.newInstance();
                try {
                    elemExtensionCall.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
                    elemExtensionCall.setLocaterInfo(stylesheetHandler.getLocator());
                    elemExtensionCall.setPrefixes(stylesheetHandler.getNamespaceSupport(), z);
                    if (elemExtensionCall instanceof ElemLiteralResult) {
                        ((ElemLiteralResult) elemExtensionCall).setNamespace(str);
                        ((ElemLiteralResult) elemExtensionCall).setLocalName(str2);
                        ((ElemLiteralResult) elemExtensionCall).setRawName(str3);
                        ((ElemLiteralResult) elemExtensionCall).setIsLiteralResultAsStylesheet(z2);
                    }
                } catch (IllegalAccessException e3) {
                    e = e3;
                    stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, e);
                } catch (InstantiationException e4) {
                    e = e4;
                    stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, e);
                }
            } catch (IllegalAccessException e5) {
                e = e5;
                elemExtensionCall = null;
            } catch (InstantiationException e6) {
                e = e6;
                elemExtensionCall = null;
            }
            setPropertiesFromAttributes(stylesheetHandler, str3, attributes2, elemExtensionCall);
            if (!zContainsExtensionElementURI && (elemExtensionCall instanceof ElemLiteralResult) && ((ElemLiteralResult) elemExtensionCall).containsExtensionElementURI(str)) {
                elemExtensionCall = new ElemExtensionCall();
                elemExtensionCall.setLocaterInfo(stylesheetHandler.getLocator());
                elemExtensionCall.setPrefixes(stylesheetHandler.getNamespaceSupport());
                ((ElemLiteralResult) elemExtensionCall).setNamespace(str);
                ((ElemLiteralResult) elemExtensionCall).setLocalName(str2);
                ((ElemLiteralResult) elemExtensionCall).setRawName(str3);
                setPropertiesFromAttributes(stylesheetHandler, str3, attributes2, elemExtensionCall);
            }
            appendAndPush(stylesheetHandler, elemExtensionCall);
        } catch (TransformerException e7) {
            throw new SAXException(e7);
        }
    }

    protected Stylesheet getStylesheetRoot(StylesheetHandler stylesheetHandler) throws TransformerConfigurationException {
        StylesheetRoot stylesheetRoot = new StylesheetRoot(stylesheetHandler.getSchema(), stylesheetHandler.getStylesheetProcessor().getErrorListener());
        if (stylesheetHandler.getStylesheetProcessor().isSecureProcessing()) {
            stylesheetRoot.setSecureProcessing(true);
        }
        return stylesheetRoot;
    }

    @Override
    public void endElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3) throws SAXException {
        ElemTemplateElement elemTemplateElement = stylesheetHandler.getElemTemplateElement();
        if ((elemTemplateElement instanceof ElemLiteralResult) && ((ElemLiteralResult) elemTemplateElement).getIsLiteralResultAsStylesheet()) {
            stylesheetHandler.popStylesheet();
        }
        super.endElement(stylesheetHandler, str, str2, str3);
    }

    private boolean declaredXSLNS(Stylesheet stylesheet) {
        List declaredPrefixes = stylesheet.getDeclaredPrefixes();
        int size = declaredPrefixes.size();
        for (int i = 0; i < size; i++) {
            if (((XMLNSDecl) declaredPrefixes.get(i)).getURI().equals(Constants.S_XSLNAMESPACEURL)) {
                return true;
            }
        }
        return false;
    }
}
