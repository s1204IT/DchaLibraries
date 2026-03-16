package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;

public class ElemIf extends ElemTemplateElement {
    static final long serialVersionUID = 2158774632427453022L;
    private XPath m_test = null;

    public void setTest(XPath v) {
        this.m_test = v;
    }

    public XPath getTest() {
        return this.m_test;
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        Vector vnames = sroot.getComposeState().getVariableNames();
        if (this.m_test != null) {
            this.m_test.fixupVariables(vnames, sroot.getComposeState().getGlobalsSize());
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
    public void execute(TransformerImpl transformer) throws TransformerException {
        XPathContext xctxt = transformer.getXPathContext();
        int sourceNode = xctxt.getCurrentNode();
        if (this.m_test.bool(xctxt, sourceNode, this)) {
            transformer.executeChildTemplates((ElemTemplateElement) this, true);
        }
    }

    @Override
    protected void callChildVisitors(XSLTVisitor visitor, boolean callAttrs) {
        if (callAttrs) {
            this.m_test.getExpression().callVisitors(this.m_test, visitor);
        }
        super.callChildVisitors(visitor, callAttrs);
    }
}
