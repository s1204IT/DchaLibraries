package org.apache.xalan.templates;

import java.io.Serializable;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xalan.extensions.ExtensionNamespacesManager;
import org.apache.xalan.processor.XSLTSchema;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.ref.ExpandedNameTable;
import org.apache.xml.utils.IntStack;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.PsuedoNames;

public class StylesheetRoot extends StylesheetComposed implements Serializable, Templates {
    static final long serialVersionUID = 3875353123529147855L;
    private HashMap m_attrSets;
    private HashMap m_availElems;
    private transient ComposeState m_composeState;
    private Hashtable m_decimalFormatSymbols;
    private ElemTemplate m_defaultRootRule;
    private ElemTemplate m_defaultRule;
    private ElemTemplate m_defaultTextRule;
    private transient ExtensionNamespacesManager m_extNsMgr;
    private String m_extensionHandlerClass;
    private StylesheetComposed[] m_globalImportList;
    private boolean m_incremental;
    private boolean m_isSecureProcessing;
    private Vector m_keyDecls;
    private Hashtable m_namespaceAliasComposed;
    private boolean m_optimizer;
    private boolean m_outputMethodSet;
    private OutputProperties m_outputProperties;
    XPath m_selectDefault;
    private boolean m_source_location;
    private ElemTemplate m_startRule;
    private TemplateList m_templateList;
    private Vector m_variables;
    private TemplateList m_whiteSpaceInfoList;

    public StylesheetRoot(ErrorListener errorListener) throws TransformerConfigurationException {
        super(null);
        this.m_optimizer = true;
        this.m_incremental = false;
        this.m_source_location = false;
        this.m_isSecureProcessing = false;
        this.m_extNsMgr = null;
        this.m_outputMethodSet = false;
        this.m_extensionHandlerClass = "org.apache.xalan.extensions.ExtensionHandlerExsltFunction";
        setStylesheetRoot(this);
        try {
            this.m_selectDefault = new XPath("node()", this, this, 0, errorListener);
            initDefaultRule(errorListener);
        } catch (TransformerException e) {
            throw new TransformerConfigurationException(XSLMessages.createMessage(XSLTErrorResources.ER_CANNOT_INIT_DEFAULT_TEMPLATES, null), e);
        }
    }

    public StylesheetRoot(XSLTSchema xSLTSchema, ErrorListener errorListener) throws TransformerConfigurationException {
        this(errorListener);
        this.m_availElems = xSLTSchema.getElemsAvailable();
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    public void setSecureProcessing(boolean z) {
        this.m_isSecureProcessing = z;
    }

    public boolean isSecureProcessing() {
        return this.m_isSecureProcessing;
    }

    public HashMap getAvailableElements() {
        return this.m_availElems;
    }

    public ExtensionNamespacesManager getExtensionNamespacesManager() {
        if (this.m_extNsMgr == null) {
            this.m_extNsMgr = new ExtensionNamespacesManager();
        }
        return this.m_extNsMgr;
    }

    public Vector getExtensions() {
        if (this.m_extNsMgr != null) {
            return this.m_extNsMgr.getExtensions();
        }
        return null;
    }

    @Override
    public Transformer newTransformer() {
        return new TransformerImpl(this);
    }

    public Properties getDefaultOutputProps() {
        return this.m_outputProperties.getProperties();
    }

    @Override
    public Properties getOutputProperties() {
        return (Properties) getDefaultOutputProps().clone();
    }

    public void recompose() throws TransformerException {
        Vector vector = new Vector();
        if (this.m_globalImportList == null) {
            Vector vector2 = new Vector();
            addImports(this, true, vector2);
            this.m_globalImportList = new StylesheetComposed[vector2.size()];
            int size = vector2.size() - 1;
            int i = 0;
            while (i < vector2.size()) {
                this.m_globalImportList[size] = (StylesheetComposed) vector2.elementAt(i);
                this.m_globalImportList[size].recomposeIncludes(this.m_globalImportList[size]);
                this.m_globalImportList[size].recomposeImports();
                i++;
                size--;
            }
        }
        int globalImportCount = getGlobalImportCount();
        for (int i2 = 0; i2 < globalImportCount; i2++) {
            getGlobalImport(i2).recompose(vector);
        }
        QuickSort2(vector, 0, vector.size() - 1);
        this.m_outputProperties = new OutputProperties("");
        this.m_attrSets = new HashMap();
        this.m_decimalFormatSymbols = new Hashtable();
        this.m_keyDecls = new Vector();
        this.m_namespaceAliasComposed = new Hashtable();
        this.m_templateList = new TemplateList();
        this.m_variables = new Vector();
        for (int size2 = vector.size() - 1; size2 >= 0; size2--) {
            ((ElemTemplateElement) vector.elementAt(size2)).recompose(this);
        }
        initComposeState();
        this.m_templateList.compose(this);
        this.m_outputProperties.compose(this);
        this.m_outputProperties.endCompose(this);
        int globalImportCount2 = getGlobalImportCount();
        for (int i3 = 0; i3 < globalImportCount2; i3++) {
            StylesheetComposed globalImport = getGlobalImport(i3);
            int includeCountComposed = globalImport.getIncludeCountComposed();
            for (int i4 = -1; i4 < includeCountComposed; i4++) {
                composeTemplates(globalImport.getIncludeComposed(i4));
            }
        }
        if (this.m_extNsMgr != null) {
            this.m_extNsMgr.registerUnregisteredNamespaces();
        }
        clearComposeState();
    }

    void composeTemplates(ElemTemplateElement elemTemplateElement) throws TransformerException {
        elemTemplateElement.compose(this);
        for (ElemTemplateElement firstChildElem = elemTemplateElement.getFirstChildElem(); firstChildElem != null; firstChildElem = firstChildElem.getNextSiblingElem()) {
            composeTemplates(firstChildElem);
        }
        elemTemplateElement.endCompose(this);
    }

    protected void addImports(Stylesheet stylesheet, boolean z, Vector vector) {
        int importCount = stylesheet.getImportCount();
        if (importCount > 0) {
            for (int i = 0; i < importCount; i++) {
                addImports(stylesheet.getImport(i), true, vector);
            }
        }
        int includeCount = stylesheet.getIncludeCount();
        if (includeCount > 0) {
            for (int i2 = 0; i2 < includeCount; i2++) {
                addImports(stylesheet.getInclude(i2), false, vector);
            }
        }
        if (z) {
            vector.addElement(stylesheet);
        }
    }

    public StylesheetComposed getGlobalImport(int i) {
        return this.m_globalImportList[i];
    }

    public int getGlobalImportCount() {
        if (this.m_globalImportList != null) {
            return this.m_globalImportList.length;
        }
        return 1;
    }

    public int getImportNumber(StylesheetComposed stylesheetComposed) {
        if (this == stylesheetComposed) {
            return 0;
        }
        int globalImportCount = getGlobalImportCount();
        for (int i = 0; i < globalImportCount; i++) {
            if (stylesheetComposed == getGlobalImport(i)) {
                return i;
            }
        }
        return -1;
    }

    void recomposeOutput(OutputProperties outputProperties) throws TransformerException {
        this.m_outputProperties.copyFrom(outputProperties);
    }

    public OutputProperties getOutputComposed() {
        return this.m_outputProperties;
    }

    public boolean isOutputMethodSet() {
        return this.m_outputMethodSet;
    }

    void recomposeAttributeSets(ElemAttributeSet elemAttributeSet) {
        ArrayList arrayList = (ArrayList) this.m_attrSets.get(elemAttributeSet.getName());
        if (arrayList == null) {
            arrayList = new ArrayList();
            this.m_attrSets.put(elemAttributeSet.getName(), arrayList);
        }
        arrayList.add(elemAttributeSet);
    }

    public ArrayList getAttributeSetComposed(QName qName) throws ArrayIndexOutOfBoundsException {
        return (ArrayList) this.m_attrSets.get(qName);
    }

    void recomposeDecimalFormats(DecimalFormatProperties decimalFormatProperties) {
        String strCreateWarning;
        DecimalFormatSymbols decimalFormatSymbols = (DecimalFormatSymbols) this.m_decimalFormatSymbols.get(decimalFormatProperties.getName());
        if (decimalFormatSymbols == null) {
            this.m_decimalFormatSymbols.put(decimalFormatProperties.getName(), decimalFormatProperties.getDecimalFormatSymbols());
        } else if (!decimalFormatProperties.getDecimalFormatSymbols().equals(decimalFormatSymbols)) {
            if (decimalFormatProperties.getName().equals(new QName(""))) {
                strCreateWarning = XSLMessages.createWarning(XSLTErrorResources.WG_ONE_DEFAULT_XSLDECIMALFORMAT_ALLOWED, new Object[0]);
            } else {
                strCreateWarning = XSLMessages.createWarning(XSLTErrorResources.WG_XSLDECIMALFORMAT_NAMES_MUST_BE_UNIQUE, new Object[]{decimalFormatProperties.getName()});
            }
            error(strCreateWarning);
        }
    }

    public DecimalFormatSymbols getDecimalFormatComposed(QName qName) {
        return (DecimalFormatSymbols) this.m_decimalFormatSymbols.get(qName);
    }

    void recomposeKeys(KeyDeclaration keyDeclaration) {
        this.m_keyDecls.addElement(keyDeclaration);
    }

    public Vector getKeysComposed() {
        return this.m_keyDecls;
    }

    void recomposeNamespaceAliases(NamespaceAlias namespaceAlias) {
        this.m_namespaceAliasComposed.put(namespaceAlias.getStylesheetNamespace(), namespaceAlias);
    }

    public NamespaceAlias getNamespaceAliasComposed(String str) {
        return (NamespaceAlias) (this.m_namespaceAliasComposed == null ? null : this.m_namespaceAliasComposed.get(str));
    }

    void recomposeTemplates(ElemTemplate elemTemplate) {
        this.m_templateList.setTemplate(elemTemplate);
    }

    public final TemplateList getTemplateListComposed() {
        return this.m_templateList;
    }

    public final void setTemplateListComposed(TemplateList templateList) {
        this.m_templateList = templateList;
    }

    public ElemTemplate getTemplateComposed(XPathContext xPathContext, int i, QName qName, boolean z, DTM dtm) throws TransformerException {
        return this.m_templateList.getTemplate(xPathContext, i, qName, z, dtm);
    }

    public ElemTemplate getTemplateComposed(XPathContext xPathContext, int i, QName qName, int i2, int i3, boolean z, DTM dtm) throws TransformerException {
        return this.m_templateList.getTemplate(xPathContext, i, qName, i2, i3, z, dtm);
    }

    public ElemTemplate getTemplateComposed(QName qName) {
        return this.m_templateList.getTemplate(qName);
    }

    void recomposeVariables(ElemVariable elemVariable) {
        if (getVariableOrParamComposed(elemVariable.getName()) == null) {
            elemVariable.setIsTopLevel(true);
            elemVariable.setIndex(this.m_variables.size());
            this.m_variables.addElement(elemVariable);
        }
    }

    public ElemVariable getVariableOrParamComposed(QName qName) {
        if (this.m_variables != null) {
            int size = this.m_variables.size();
            for (int i = 0; i < size; i++) {
                ElemVariable elemVariable = (ElemVariable) this.m_variables.elementAt(i);
                if (elemVariable.getName().equals(qName)) {
                    return elemVariable;
                }
            }
            return null;
        }
        return null;
    }

    public Vector getVariablesAndParamsComposed() {
        return this.m_variables;
    }

    void recomposeWhiteSpaceInfo(WhiteSpaceInfo whiteSpaceInfo) {
        if (this.m_whiteSpaceInfoList == null) {
            this.m_whiteSpaceInfoList = new TemplateList();
        }
        this.m_whiteSpaceInfoList.setTemplate(whiteSpaceInfo);
    }

    public boolean shouldCheckWhitespace() {
        return this.m_whiteSpaceInfoList != null;
    }

    public WhiteSpaceInfo getWhiteSpaceInfo(XPathContext xPathContext, int i, DTM dtm) throws TransformerException {
        if (this.m_whiteSpaceInfoList != null) {
            return (WhiteSpaceInfo) this.m_whiteSpaceInfoList.getTemplate(xPathContext, i, null, false, dtm);
        }
        return null;
    }

    public boolean shouldStripWhiteSpace(XPathContext xPathContext, int i) throws TransformerException {
        if (this.m_whiteSpaceInfoList != null) {
            while (-1 != i) {
                DTM dtm = xPathContext.getDTM(i);
                WhiteSpaceInfo whiteSpaceInfo = (WhiteSpaceInfo) this.m_whiteSpaceInfoList.getTemplate(xPathContext, i, null, false, dtm);
                if (whiteSpaceInfo != null) {
                    return whiteSpaceInfo.getShouldStripSpace();
                }
                i = dtm.getParent(i);
                if (-1 == i || 1 != dtm.getNodeType(i)) {
                    i = -1;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean canStripWhiteSpace() {
        return this.m_whiteSpaceInfoList != null;
    }

    public final ElemTemplate getDefaultTextRule() {
        return this.m_defaultTextRule;
    }

    public final ElemTemplate getDefaultRule() {
        return this.m_defaultRule;
    }

    public final ElemTemplate getDefaultRootRule() {
        return this.m_defaultRootRule;
    }

    public final ElemTemplate getStartRule() {
        return this.m_startRule;
    }

    private void initDefaultRule(ErrorListener errorListener) throws TransformerException {
        this.m_defaultRule = new ElemTemplate();
        this.m_defaultRule.setStylesheet(this);
        this.m_defaultRule.setMatch(new XPath("*", this, this, 1, errorListener));
        ElemApplyTemplates elemApplyTemplates = new ElemApplyTemplates();
        elemApplyTemplates.setIsDefaultTemplate(true);
        elemApplyTemplates.setSelect(this.m_selectDefault);
        this.m_defaultRule.appendChild((ElemTemplateElement) elemApplyTemplates);
        this.m_startRule = this.m_defaultRule;
        this.m_defaultTextRule = new ElemTemplate();
        this.m_defaultTextRule.setStylesheet(this);
        this.m_defaultTextRule.setMatch(new XPath("text() | @*", this, this, 1, errorListener));
        ElemValueOf elemValueOf = new ElemValueOf();
        this.m_defaultTextRule.appendChild((ElemTemplateElement) elemValueOf);
        elemValueOf.setSelect(new XPath(Constants.ATTRVAL_THIS, this, this, 0, errorListener));
        this.m_defaultRootRule = new ElemTemplate();
        this.m_defaultRootRule.setStylesheet(this);
        this.m_defaultRootRule.setMatch(new XPath(PsuedoNames.PSEUDONAME_ROOT, this, this, 1, errorListener));
        ElemApplyTemplates elemApplyTemplates2 = new ElemApplyTemplates();
        elemApplyTemplates2.setIsDefaultTemplate(true);
        this.m_defaultRootRule.appendChild((ElemTemplateElement) elemApplyTemplates2);
        elemApplyTemplates2.setSelect(this.m_selectDefault);
    }

    private void QuickSort2(Vector vector, int i, int i2) {
        if (i2 > i) {
            ElemTemplateElement elemTemplateElement = (ElemTemplateElement) vector.elementAt((i + i2) / 2);
            int i3 = i;
            int i4 = i2;
            while (i3 <= i4) {
                while (i3 < i2 && ((ElemTemplateElement) vector.elementAt(i3)).compareTo(elemTemplateElement) < 0) {
                    i3++;
                }
                while (i4 > i && ((ElemTemplateElement) vector.elementAt(i4)).compareTo(elemTemplateElement) > 0) {
                    i4--;
                }
                if (i3 <= i4) {
                    ElemTemplateElement elemTemplateElement2 = (ElemTemplateElement) vector.elementAt(i3);
                    vector.setElementAt(vector.elementAt(i4), i3);
                    vector.setElementAt(elemTemplateElement2, i4);
                    i3++;
                    i4--;
                }
            }
            if (i < i4) {
                QuickSort2(vector, i, i4);
            }
            if (i3 < i2) {
                QuickSort2(vector, i3, i2);
            }
        }
    }

    void initComposeState() {
        this.m_composeState = new ComposeState();
    }

    ComposeState getComposeState() {
        return this.m_composeState;
    }

    private void clearComposeState() {
        this.m_composeState = null;
    }

    public String setExtensionHandlerClass(String str) {
        String str2 = this.m_extensionHandlerClass;
        this.m_extensionHandlerClass = str;
        return str2;
    }

    public String getExtensionHandlerClass() {
        return this.m_extensionHandlerClass;
    }

    class ComposeState {
        private int m_maxStackFrameSize;
        private ExpandedNameTable m_ent = new ExpandedNameTable();
        private Vector m_variableNames = new Vector();
        IntStack m_marks = new IntStack();

        ComposeState() {
            int size = StylesheetRoot.this.m_variables.size();
            for (int i = 0; i < size; i++) {
                this.m_variableNames.addElement(((ElemVariable) StylesheetRoot.this.m_variables.elementAt(i)).getName());
            }
        }

        public int getQNameID(QName qName) {
            return this.m_ent.getExpandedTypeID(qName.getNamespace(), qName.getLocalName(), 1);
        }

        int addVariableName(QName qName) {
            int size = this.m_variableNames.size();
            this.m_variableNames.addElement(qName);
            if (this.m_variableNames.size() - getGlobalsSize() > this.m_maxStackFrameSize) {
                this.m_maxStackFrameSize++;
            }
            return size;
        }

        void resetStackFrameSize() {
            this.m_maxStackFrameSize = 0;
        }

        int getFrameSize() {
            return this.m_maxStackFrameSize;
        }

        int getCurrentStackFrameSize() {
            return this.m_variableNames.size();
        }

        void setCurrentStackFrameSize(int i) {
            this.m_variableNames.setSize(i);
        }

        int getGlobalsSize() {
            return StylesheetRoot.this.m_variables.size();
        }

        void pushStackMark() {
            this.m_marks.push(getCurrentStackFrameSize());
        }

        void popStackMark() {
            setCurrentStackFrameSize(this.m_marks.pop());
        }

        Vector getVariableNames() {
            return this.m_variableNames;
        }
    }

    public boolean getOptimizer() {
        return this.m_optimizer;
    }

    public void setOptimizer(boolean z) {
        this.m_optimizer = z;
    }

    public boolean getIncremental() {
        return this.m_incremental;
    }

    public boolean getSource_location() {
        return this.m_source_location;
    }

    public void setIncremental(boolean z) {
        this.m_incremental = z;
    }

    public void setSource_location(boolean z) {
        this.m_source_location = z;
    }
}
