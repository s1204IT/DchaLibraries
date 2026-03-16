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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ProcessorLRE extends ProcessorTemplateElem {
    static final long serialVersionUID = -1490218021772101404L;

    @Override
    public void startElement(StylesheetHandler handler, String uri, String localName, String rawName, Attributes attributes) throws SAXException {
        try {
            ElemTemplateElement p = handler.getElemTemplateElement();
            boolean excludeXSLDecl = false;
            boolean isLREAsStyleSheet = false;
            if (p == null) {
                XSLTElementProcessor lreProcessor = handler.popProcessor();
                XSLTElementProcessor stylesheetProcessor = handler.getProcessorFor(Constants.S_XSLNAMESPACEURL, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, "xsl:stylesheet");
                handler.pushProcessor(lreProcessor);
                try {
                    Stylesheet stylesheet = getStylesheetRoot(handler);
                    SAXSourceLocator slocator = new SAXSourceLocator();
                    Locator locator = handler.getLocator();
                    if (locator != null) {
                        slocator.setLineNumber(locator.getLineNumber());
                        slocator.setColumnNumber(locator.getColumnNumber());
                        slocator.setPublicId(locator.getPublicId());
                        slocator.setSystemId(locator.getSystemId());
                    }
                    stylesheet.setLocaterInfo(slocator);
                    stylesheet.setPrefixes(handler.getNamespaceSupport());
                    handler.pushStylesheet(stylesheet);
                    isLREAsStyleSheet = true;
                    AttributesImpl stylesheetAttrs = new AttributesImpl();
                    AttributesImpl lreAttrs = new AttributesImpl();
                    int n = attributes.getLength();
                    for (int i = 0; i < n; i++) {
                        String attrLocalName = attributes.getLocalName(i);
                        String attrUri = attributes.getURI(i);
                        String value = attributes.getValue(i);
                        if (attrUri != null && attrUri.equals(Constants.S_XSLNAMESPACEURL)) {
                            stylesheetAttrs.addAttribute(null, attrLocalName, attrLocalName, attributes.getType(i), attributes.getValue(i));
                        } else if ((!attrLocalName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS) && !attrLocalName.equals("xmlns")) || !value.equals(Constants.S_XSLNAMESPACEURL)) {
                            lreAttrs.addAttribute(attrUri, attrLocalName, attributes.getQName(i), attributes.getType(i), attributes.getValue(i));
                        }
                    }
                    attributes = lreAttrs;
                    try {
                        stylesheetProcessor.setPropertiesFromAttributes(handler, org.apache.xalan.templates.Constants.ELEMNAME_STYLESHEET_STRING, stylesheetAttrs, stylesheet);
                        handler.pushElemTemplateElement(stylesheet);
                        ElemTemplate template = new ElemTemplate();
                        if (slocator != null) {
                            template.setLocaterInfo(slocator);
                        }
                        appendAndPush(handler, template);
                        XPath rootMatch = new XPath(PsuedoNames.PSEUDONAME_ROOT, stylesheet, stylesheet, 1, handler.getStylesheetProcessor().getErrorListener());
                        template.setMatch(rootMatch);
                        stylesheet.setTemplate(template);
                        p = handler.getElemTemplateElement();
                        excludeXSLDecl = true;
                    } catch (Exception e) {
                        if (stylesheet.getDeclaredPrefixes() == null || !declaredXSLNS(stylesheet)) {
                            throw new SAXException(XSLMessages.createWarning(XSLTErrorResources.WG_OLD_XSLT_NS, null));
                        }
                        throw new SAXException(e);
                    }
                } catch (TransformerConfigurationException tfe) {
                    throw new TransformerException(tfe);
                }
            }
            XSLTElementDef def = getElemDef();
            Class classObject = def.getClassObject();
            boolean isExtension = false;
            boolean isComponentDecl = false;
            boolean isUnknownTopLevel = false;
            while (p != null) {
                if (p instanceof ElemLiteralResult) {
                    ElemLiteralResult parentElem = (ElemLiteralResult) p;
                    isExtension = parentElem.containsExtensionElementURI(uri);
                } else if (p instanceof Stylesheet) {
                    Stylesheet parentElem2 = (Stylesheet) p;
                    isExtension = parentElem2.containsExtensionElementURI(uri);
                    if (!isExtension && uri != null && (uri.equals("http://xml.apache.org/xalan") || uri.equals(Constants.S_BUILTIN_OLD_EXTENSIONS_URL))) {
                        isComponentDecl = true;
                    } else {
                        isUnknownTopLevel = true;
                    }
                }
                if (isExtension) {
                    break;
                } else {
                    p = p.getParentElem();
                }
            }
            ElemTemplateElement elem = null;
            try {
                if (isExtension) {
                    elem = new ElemExtensionCall();
                } else if (!isComponentDecl && isUnknownTopLevel) {
                    elem = (ElemTemplateElement) classObject.newInstance();
                } else {
                    elem = (ElemTemplateElement) classObject.newInstance();
                }
                elem.setDOMBackPointer(handler.getOriginatingNode());
                elem.setLocaterInfo(handler.getLocator());
                elem.setPrefixes(handler.getNamespaceSupport(), excludeXSLDecl);
                if (elem instanceof ElemLiteralResult) {
                    ((ElemLiteralResult) elem).setNamespace(uri);
                    ((ElemLiteralResult) elem).setLocalName(localName);
                    ((ElemLiteralResult) elem).setRawName(rawName);
                    ((ElemLiteralResult) elem).setIsLiteralResultAsStylesheet(isLREAsStyleSheet);
                }
            } catch (IllegalAccessException iae) {
                handler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, iae);
            } catch (InstantiationException ie) {
                handler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMLITRSLT, null, ie);
            }
            setPropertiesFromAttributes(handler, rawName, attributes, elem);
            if (!isExtension && (elem instanceof ElemLiteralResult)) {
                boolean isExtension2 = ((ElemLiteralResult) elem).containsExtensionElementURI(uri);
                if (isExtension2) {
                    elem = new ElemExtensionCall();
                    elem.setLocaterInfo(handler.getLocator());
                    elem.setPrefixes(handler.getNamespaceSupport());
                    ((ElemLiteralResult) elem).setNamespace(uri);
                    ((ElemLiteralResult) elem).setLocalName(localName);
                    ((ElemLiteralResult) elem).setRawName(rawName);
                    setPropertiesFromAttributes(handler, rawName, attributes, elem);
                }
            }
            appendAndPush(handler, elem);
        } catch (TransformerException te) {
            throw new SAXException(te);
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
        ElemTemplateElement elem = handler.getElemTemplateElement();
        if ((elem instanceof ElemLiteralResult) && ((ElemLiteralResult) elem).getIsLiteralResultAsStylesheet()) {
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
