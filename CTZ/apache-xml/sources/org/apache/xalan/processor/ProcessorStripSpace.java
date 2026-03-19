package org.apache.xalan.processor;

import java.util.Vector;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xalan.templates.WhiteSpaceInfo;
import org.apache.xpath.XPath;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorStripSpace extends ProcessorPreserveSpace {
    static final long serialVersionUID = -5594493198637899591L;

    ProcessorStripSpace() {
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        Stylesheet stylesheet = stylesheetHandler.getStylesheet();
        WhitespaceInfoPaths whitespaceInfoPaths = new WhitespaceInfoPaths(stylesheet);
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, whitespaceInfoPaths);
        Vector elements = whitespaceInfoPaths.getElements();
        for (int i = 0; i < elements.size(); i++) {
            WhiteSpaceInfo whiteSpaceInfo = new WhiteSpaceInfo((XPath) elements.elementAt(i), true, stylesheet);
            whiteSpaceInfo.setUid(stylesheetHandler.nextUid());
            stylesheet.setStripSpaces(whiteSpaceInfo);
        }
        whitespaceInfoPaths.clearElements();
    }
}
