package org.apache.xalan.processor;

import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.NamespaceAlias;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorNamespaceAlias extends XSLTElementProcessor {
    static final long serialVersionUID = -6309867839007018964L;

    ProcessorNamespaceAlias() {
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        String namespaceForPrefix;
        NamespaceAlias namespaceAlias = new NamespaceAlias(stylesheetHandler.nextUid());
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, namespaceAlias);
        String stylesheetPrefix = namespaceAlias.getStylesheetPrefix();
        if (stylesheetPrefix.equals("#default")) {
            stylesheetPrefix = "";
            namespaceAlias.setStylesheetPrefix("");
        }
        namespaceAlias.setStylesheetNamespace(stylesheetHandler.getNamespaceForPrefix(stylesheetPrefix));
        String resultPrefix = namespaceAlias.getResultPrefix();
        if (resultPrefix.equals("#default")) {
            namespaceAlias.setResultPrefix("");
            namespaceForPrefix = stylesheetHandler.getNamespaceForPrefix("");
            if (namespaceForPrefix == null) {
                stylesheetHandler.error(XSLTErrorResources.ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX_FOR_DEFAULT, null, null);
            }
        } else {
            String namespaceForPrefix2 = stylesheetHandler.getNamespaceForPrefix(resultPrefix);
            if (namespaceForPrefix2 == null) {
                stylesheetHandler.error(XSLTErrorResources.ER_INVALID_NAMESPACE_URI_VALUE_FOR_RESULT_PREFIX, new Object[]{resultPrefix}, null);
            }
            namespaceForPrefix = namespaceForPrefix2;
        }
        namespaceAlias.setResultNamespace(namespaceForPrefix);
        stylesheetHandler.getStylesheet().setNamespaceAlias(namespaceAlias);
        stylesheetHandler.getStylesheet().appendChild((ElemTemplateElement) namespaceAlias);
    }
}
