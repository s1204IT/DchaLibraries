package org.apache.xalan.processor;

import java.util.Vector;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.WhiteSpaceInfo;
import org.apache.xpath.XPath;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorPreserveSpace extends XSLTElementProcessor {
    static final long serialVersionUID = -5552836470051177302L;

    ProcessorPreserveSpace() {
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        Stylesheet stylesheet = stylesheetHandler.getStylesheet();
        WhitespaceInfoPaths whitespaceInfoPaths = new WhitespaceInfoPaths(stylesheet);
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, whitespaceInfoPaths);
        Vector elements = whitespaceInfoPaths.getElements();
        for (int i = 0; i < elements.size(); i++) {
            WhiteSpaceInfo whiteSpaceInfo = new WhiteSpaceInfo((XPath) elements.elementAt(i), false, stylesheet);
            whiteSpaceInfo.setUid(stylesheetHandler.nextUid());
            stylesheet.setPreserveSpaces(whiteSpaceInfo);
        }
        whitespaceInfoPaths.clearElements();
    }
}
