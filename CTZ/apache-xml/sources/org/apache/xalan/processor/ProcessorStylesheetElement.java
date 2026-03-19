package org.apache.xalan.processor;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.StylesheetComposed;
import org.apache.xalan.templates.StylesheetRoot;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProcessorStylesheetElement extends XSLTElementProcessor {
    static final long serialVersionUID = -877798927447840792L;

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        Stylesheet stylesheetRoot;
        super.startElement(stylesheetHandler, str, str2, str3, attributes);
        try {
            int stylesheetType = stylesheetHandler.getStylesheetType();
            if (stylesheetType == 1) {
                try {
                    stylesheetRoot = getStylesheetRoot(stylesheetHandler);
                } catch (TransformerConfigurationException e) {
                    throw new TransformerException(e);
                }
            } else {
                Stylesheet stylesheet = stylesheetHandler.getStylesheet();
                if (stylesheetType == 3) {
                    StylesheetComposed stylesheetComposed = new StylesheetComposed(stylesheet);
                    stylesheet.setImport(stylesheetComposed);
                    stylesheetRoot = stylesheetComposed;
                } else {
                    Stylesheet stylesheet2 = new Stylesheet(stylesheet);
                    stylesheet.setInclude(stylesheet2);
                    stylesheetRoot = stylesheet2;
                }
            }
            stylesheetRoot.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
            stylesheetRoot.setLocaterInfo(stylesheetHandler.getLocator());
            stylesheetRoot.setPrefixes(stylesheetHandler.getNamespaceSupport());
            stylesheetHandler.pushStylesheet(stylesheetRoot);
            setPropertiesFromAttributes(stylesheetHandler, str3, attributes, stylesheetHandler.getStylesheet());
            stylesheetHandler.pushElemTemplateElement(stylesheetHandler.getStylesheet());
        } catch (TransformerException e2) {
            throw new SAXException(e2);
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
        super.endElement(stylesheetHandler, str, str2, str3);
        stylesheetHandler.popElemTemplateElement();
        stylesheetHandler.popStylesheet();
    }
}
