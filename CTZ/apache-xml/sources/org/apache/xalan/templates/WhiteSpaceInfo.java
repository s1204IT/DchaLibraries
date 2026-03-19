package org.apache.xalan.templates;

import org.apache.xpath.XPath;

public class WhiteSpaceInfo extends ElemTemplate {
    static final long serialVersionUID = 6389208261999943836L;
    private boolean m_shouldStripSpace;

    public boolean getShouldStripSpace() {
        return this.m_shouldStripSpace;
    }

    public WhiteSpaceInfo(Stylesheet stylesheet) {
        setStylesheet(stylesheet);
    }

    public WhiteSpaceInfo(XPath xPath, boolean z, Stylesheet stylesheet) {
        this.m_shouldStripSpace = z;
        setMatch(xPath);
        setStylesheet(stylesheet);
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeWhiteSpaceInfo(this);
    }
}
