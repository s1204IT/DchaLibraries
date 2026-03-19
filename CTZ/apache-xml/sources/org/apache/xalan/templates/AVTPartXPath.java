package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathFactory;
import org.apache.xpath.compiler.XPathParser;
import org.apache.xpath.objects.XObject;

public class AVTPartXPath extends AVTPart {
    static final long serialVersionUID = -4460373807550527675L;
    private XPath m_xpath;

    @Override
    public void fixupVariables(Vector vector, int i) {
        this.m_xpath.fixupVariables(vector, i);
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        return this.m_xpath.getExpression().canTraverseOutsideSubtree();
    }

    public AVTPartXPath(XPath xPath) {
        this.m_xpath = xPath;
    }

    public AVTPartXPath(String str, PrefixResolver prefixResolver, XPathParser xPathParser, XPathFactory xPathFactory, XPathContext xPathContext) throws TransformerException {
        this.m_xpath = new XPath(str, null, prefixResolver, 0, xPathContext.getErrorListener());
    }

    @Override
    public String getSimpleString() {
        return "{" + this.m_xpath.getPatternString() + "}";
    }

    @Override
    public void evaluate(XPathContext xPathContext, FastStringBuffer fastStringBuffer, int i, PrefixResolver prefixResolver) throws TransformerException {
        XObject xObjectExecute = this.m_xpath.execute(xPathContext, i, prefixResolver);
        if (xObjectExecute != null) {
            xObjectExecute.appendToFsb(fastStringBuffer);
        }
    }

    @Override
    public void callVisitors(XSLTVisitor xSLTVisitor) {
        this.m_xpath.getExpression().callVisitors(this.m_xpath, xSLTVisitor);
    }
}
