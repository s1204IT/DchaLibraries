package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPath;

public class KeyDeclaration extends ElemTemplateElement {
    static final long serialVersionUID = 7724030248631137918L;
    private XPath m_matchPattern = null;
    private QName m_name;
    private XPath m_use;

    public KeyDeclaration(Stylesheet stylesheet, int i) {
        this.m_parentNode = stylesheet;
        setUid(i);
    }

    public void setName(QName qName) {
        this.m_name = qName;
    }

    public QName getName() {
        return this.m_name;
    }

    @Override
    public String getNodeName() {
        return "key";
    }

    public void setMatch(XPath xPath) {
        this.m_matchPattern = xPath;
    }

    public XPath getMatch() {
        return this.m_matchPattern;
    }

    public void setUse(XPath xPath) {
        this.m_use = xPath;
    }

    public XPath getUse() {
        return this.m_use;
    }

    @Override
    public int getXSLToken() {
        return 31;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        Vector variableNames = stylesheetRoot.getComposeState().getVariableNames();
        if (this.m_matchPattern != null) {
            this.m_matchPattern.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
        if (this.m_use != null) {
            this.m_use.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeKeys(this);
    }
}
