package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;

public class ElemIf extends ElemTemplateElement {
    static final long serialVersionUID = 2158774632427453022L;
    private XPath m_test = null;

    public void setTest(XPath xPath) {
        this.m_test = xPath;
    }

    public XPath getTest() {
        return this.m_test;
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
    public int getXSLToken() {
        return 36;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_IF_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        XPathContext xPathContext = transformerImpl.getXPathContext();
        if (this.m_test.bool(xPathContext, xPathContext.getCurrentNode(), this)) {
            transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
        }
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z) {
            this.m_test.getExpression().callVisitors(this.m_test, xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
    }
}
