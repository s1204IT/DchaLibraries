package org.apache.xalan.templates;

public class NamespaceAlias extends ElemTemplateElement {
    static final long serialVersionUID = 456173966637810718L;
    private String m_ResultNamespace;
    private String m_ResultPrefix;
    private String m_StylesheetNamespace;
    private String m_StylesheetPrefix;

    public NamespaceAlias(int i) {
        this.m_docOrderNumber = i;
    }

    public void setStylesheetPrefix(String str) {
        this.m_StylesheetPrefix = str;
    }

    public String getStylesheetPrefix() {
        return this.m_StylesheetPrefix;
    }

    public void setStylesheetNamespace(String str) {
        this.m_StylesheetNamespace = str;
    }

    public String getStylesheetNamespace() {
        return this.m_StylesheetNamespace;
    }

    public void setResultPrefix(String str) {
        this.m_ResultPrefix = str;
    }

    public String getResultPrefix() {
        return this.m_ResultPrefix;
    }

    public void setResultNamespace(String str) {
        this.m_ResultNamespace = str;
    }

    public String getResultNamespace() {
        return this.m_ResultNamespace;
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeNamespaceAliases(this);
    }
}
