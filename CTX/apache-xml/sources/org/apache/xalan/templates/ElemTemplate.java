package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;

public class ElemTemplate extends ElemTemplateElement {
    static final long serialVersionUID = -5283056789965384058L;
    private int[] m_argsQNameIDs;
    public int m_frameSize;
    int m_inArgsSize;
    private QName m_mode;
    private String m_publicId;
    private Stylesheet m_stylesheet;
    private String m_systemId;
    private XPath m_matchPattern = null;
    private QName m_name = null;
    private double m_priority = Double.NEGATIVE_INFINITY;

    @Override
    public String getPublicId() {
        return this.m_publicId;
    }

    @Override
    public String getSystemId() {
        return this.m_systemId;
    }

    @Override
    public void setLocaterInfo(SourceLocator sourceLocator) {
        this.m_publicId = sourceLocator.getPublicId();
        this.m_systemId = sourceLocator.getSystemId();
        super.setLocaterInfo(sourceLocator);
    }

    @Override
    public StylesheetComposed getStylesheetComposed() {
        return this.m_stylesheet.getStylesheetComposed();
    }

    @Override
    public Stylesheet getStylesheet() {
        return this.m_stylesheet;
    }

    public void setStylesheet(Stylesheet stylesheet) {
        this.m_stylesheet = stylesheet;
    }

    @Override
    public StylesheetRoot getStylesheetRoot() {
        return this.m_stylesheet.getStylesheetRoot();
    }

    public void setMatch(XPath xPath) {
        this.m_matchPattern = xPath;
    }

    public XPath getMatch() {
        return this.m_matchPattern;
    }

    public void setName(QName qName) {
        this.m_name = qName;
    }

    public QName getName() {
        return this.m_name;
    }

    public void setMode(QName qName) {
        this.m_mode = qName;
    }

    public QName getMode() {
        return this.m_mode;
    }

    public void setPriority(double d) {
        this.m_priority = d;
    }

    public double getPriority() {
        return this.m_priority;
    }

    @Override
    public int getXSLToken() {
        return 19;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_TEMPLATE_STRING;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        Vector variableNames = composeState.getVariableNames();
        if (this.m_matchPattern != null) {
            this.m_matchPattern.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        }
        composeState.resetStackFrameSize();
        this.m_inArgsSize = 0;
    }

    @Override
    public void endCompose(StylesheetRoot stylesheetRoot) throws TransformerException {
        StylesheetRoot.ComposeState composeState = stylesheetRoot.getComposeState();
        super.endCompose(stylesheetRoot);
        this.m_frameSize = composeState.getFrameSize();
        composeState.resetStackFrameSize();
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        XPathContext xPathContext = transformerImpl.getXPathContext();
        xPathContext.pushRTFContext();
        transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
        xPathContext.popRTFContext();
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeTemplates(this);
    }
}
