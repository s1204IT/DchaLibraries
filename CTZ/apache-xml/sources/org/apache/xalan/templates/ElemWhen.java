package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPath;

public class ElemWhen extends ElemTemplateElement {
    static final long serialVersionUID = 5984065730262071360L;
    private XPath m_test;

    public void setTest(XPath xPath) {
        this.m_test = xPath;
    }

    public XPath getTest() {
        return this.m_test;
    }

    @Override
    public int getXSLToken() {
        return 38;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        Vector variableNames = stylesheetRoot.getComposeState().getVariableNames();
        if (this.m_test != null) {
            this.m_test.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_WHEN_STRING;
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z) {
            this.m_test.getExpression().callVisitors(this.m_test, xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
    }
}
