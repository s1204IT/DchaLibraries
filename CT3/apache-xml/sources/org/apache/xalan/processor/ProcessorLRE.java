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
        ElemTemplateElement elemTemplateElement;
        try {
            ElemTemplateElement elemTemplateElement2 = stylesheetHandler.getElemTemplateElement();
            boolean z = false;
            boolean z2 = false;
            if (elemTemplateElement2 == null) {
                XSLTElementProcessor xSLTElementProcessorPopProcessor = stylesheetHandler.popProcessor();
                ?? processorFor = stylesheetHandler.getProcessorFor(Constants.S_XSLNAMESPACEURL, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, "xsl:stylesheet");
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
                    z2 = true;
                    AttributesImpl attributesImpl = new AttributesImpl();
                    AttributesImpl attributesImpl2 = new AttributesImpl();
                    int length = attributes.getLength();
                    for (int i = 0; i < length; i++) {
                        String localName = attributes.getLocalName(i);
                        String uri = attributes.getURI(i);
                        String value = attributes.getValue(i);
                        if (uri != null && uri.equals(Constants.S_XSLNAMESPACEURL)) {
                            attributesImpl.addAttribute(null, localName, localName, attributes.getType(i), attributes.getValue(i));
                        } else if ((!localName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS) && !localName.equals("xmlns")) || !value.equals(Constants.S_XSLNAMESPACEURL)) {
                            attributesImpl2.addAttribute(uri, localName, attributes.getQName(i), attributes.getType(i), attributes.getValue(i));
                        }
                    }
                    attributes = attributesImpl2;
                    try {
                        processorFor.setPropertiesFromAttributes(stylesheetHandler, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, attributesImpl, stylesheetRoot);
                        stylesheetHandler.pushElemTemplateElement(stylesheetRoot);
                        ElemTemplate elemTemplate = new ElemTemplate();
                        if (sAXSourceLocator != null) {
                            elemTemplate.setLocaterInfo(sAXSourceLocator);
                        }
                        appendAndPush(stylesheetHandler, elemTemplate);
                        elemTemplate.setMatch(new XPath(PsuedoNames.PSEUDONAME_ROOT, stylesheetRoot, stylesheetRoot, 1, stylesheetHandler.getStylesheetProcessor().getErrorListener()));
                        stylesheetRoot.setTemplate(elemTemplate);
                        elemTemplateElement2 = stylesheetHandler.getElemTemplateElement();
                        z = true;
                    } catch (Exception e) {
                        if (stylesheetRoot.getDeclaredPrefixes() == null || !declaredXSLNS(stylesheetRoot)) {
                            throw new SAXException(XSLMessages.createWarning(XSLTErrorResources.WG_OLD_XSLT_NS, null));
                        }
                        throw new SAXException(e);
                    }
                } catch (TransformerConfigurationException e2) {
                    throw new TransformerException(e2);
                }
            }
            Class classObject = getElemDef().getClassObject();
            boolean zContainsExtensionElementURI = false;
            boolean z3 = false;
            boolean z4 = false;
            while (elemTemplateElement2 != null) {
                if (elemTemplateElement2 instanceof ElemLiteralResult) {
                    zContainsExtensionElementURI = elemTemplateElement2.containsExtensionElementURI(str);
                } else if (elemTemplateElement2 instanceof Stylesheet) {
                    zContainsExtensionElementURI = elemTemplateElement2.containsExtensionElementURI(str);
                    if (!zContainsExtensionElementURI && str != null && (str.equals("http://xml.apache.org/xalan") || str.equals(Constants.S_BUILTIN_OLD_EXTENSIONS_URL))) {
                        z3 = true;
                    } else {
                        z4 = true;
                    }
                }
                if (zContainsExtensionElementURI) {
                    break;
                } else {
                    elemTemplateElement2 = elemTemplateElement2.getParentElem();
                }
            }
            ElemTemplateElement elemExtensionCall = null;
            elemExtensionCall = null;
            try {
                if (zContainsExtensionElementURI) {
                    elemExtensionCall = new ElemExtensionCall();
                } else if (!z3 && z4) {
                    elemExtensionCall = (ElemTemplateElement) classObject.newInstance();
                } else {
                    elemExtensionCall = (ElemTemplateElement) classObject.newInstance();
                }
                elemExtensionCall.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
                elemExtensionCall.setLocaterInfo(stylesheetHandler.getLocator());
                elemExtensionCall.setPrefixes(stylesheetHandler.getNamespaceSupport(), z);
                boolean z5 = elemExtensionCall instanceof ElemLiteralResult;
                elemTemplateElement = elemExtensionCall;
                if (z5) {
                    elemExtensionCall.setNamespace(str);
                    elemExtensionCall.setLocalName(str2);
                    elemExtensionCall.setRawName(str3);
                    elemExtensionCall.setIsLiteralResultAsStylesheet(z2);
                    elemTemplateElement = elemExtensionCall;
                }
            } catch (IllegalAccessException e3) {
                stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, e3);
                elemTemplateElement = elemExtensionCall;
            } catch (InstantiationException e4) {
                stylesheetHandler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, e4);
                elemTemplateElement = elemExtensionCall;
            }
            setPropertiesFromAttributes(stylesheetHandler, str3, attributes, elemTemplateElement);
            ElemTemplateElement elemTemplateElement3 = elemTemplateElement;
            if (!zContainsExtensionElementURI) {
                boolean z6 = elemTemplateElement instanceof ElemLiteralResult;
                elemTemplateElement3 = elemTemplateElement;
                if (z6) {
                    boolean zContainsExtensionElementURI2 = elemTemplateElement.containsExtensionElementURI(str);
                    elemTemplateElement3 = elemTemplateElement;
                    if (zContainsExtensionElementURI2) {
                        ElemExtensionCall elemExtensionCall2 = new ElemExtensionCall();
                        elemExtensionCall2.setLocaterInfo(stylesheetHandler.getLocator());
                        elemExtensionCall2.setPrefixes(stylesheetHandler.getNamespaceSupport());
                        elemExtensionCall2.setNamespace(str);
                        elemExtensionCall2.setLocalName(str2);
                        elemExtensionCall2.setRawName(str3);
                        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, elemExtensionCall2);
                        elemTemplateElement3 = elemExtensionCall2;
                    }
                }
            }
            appendAndPush(stylesheetHandler, elemTemplateElement3);
        } catch (TransformerException e5) {
            throw new SAXException(e5);
        }
    }

    protected Stylesheet getStylesheetRoot(StylesheetHandler handler) throws TransformerConfigurationException {
        StylesheetRoot stylesheet = new StylesheetRoot(handler.getSchema(), handler.getStylesheetProcessor().getErrorListener());
        if (handler.getStylesheetProcessor().isSecureProcessing()) {
            stylesheet.setSecureProcessing(true);
        }
        return stylesheet;
    }

    @Override
    public void endElement(StylesheetHandler handler, String uri, String localName, String rawName) throws SAXException {
        ?? elemTemplateElement = handler.getElemTemplateElement();
        if ((elemTemplateElement instanceof ElemLiteralResult) && elemTemplateElement.getIsLiteralResultAsStylesheet()) {
            handler.popStylesheet();
        }
        super.endElement(handler, uri, localName, rawName);
    }

    private boolean declaredXSLNS(Stylesheet stylesheet) {
        List declaredPrefixes = stylesheet.getDeclaredPrefixes();
        int n = declaredPrefixes.size();
        for (int i = 0; i < n; i++) {
            XMLNSDecl decl = (XMLNSDecl) declaredPrefixes.get(i);
            if (decl.getURI().equals(Constants.S_XSLNAMESPACEURL)) {
                return true;
            }
        }
        return false;
    }
}
