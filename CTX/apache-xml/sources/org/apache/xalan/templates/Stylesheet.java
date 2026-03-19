package org.apache.xalan.templates;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.StringVector;
import org.apache.xml.utils.SystemIDResolver;
import org.apache.xml.utils.WrappedRuntimeException;

public class Stylesheet extends ElemTemplateElement implements Serializable {
    public static final String STYLESHEET_EXT = ".lxc";
    static final long serialVersionUID = 2085337282743043776L;
    Stack m_DecimalFormatDeclarations;
    private StringVector m_ExcludeResultPrefixs;
    private StringVector m_ExtensionElementURIs;
    private String m_Id;
    private Hashtable m_NonXslTopLevel;
    private String m_Version;
    private String m_XmlnsXsl;
    private Vector m_attributeSets;
    private Vector m_imports;
    private Vector m_includes;
    private Vector m_keyDeclarations;
    private Vector m_output;
    private Vector m_prefix_aliases;
    private String m_publicId;
    private Stylesheet m_stylesheetParent;
    private StylesheetRoot m_stylesheetRoot;
    private String m_systemId;
    private Vector m_templates;
    private Vector m_topLevelVariables;
    private Vector m_whitespacePreservingElements;
    private Vector m_whitespaceStrippingElements;
    private boolean m_isCompatibleMode = false;
    private String m_href = null;

    public Stylesheet(Stylesheet stylesheet) {
        if (stylesheet != null) {
            this.m_stylesheetParent = stylesheet;
            this.m_stylesheetRoot = stylesheet.getStylesheetRoot();
        }
    }

    @Override
    public Stylesheet getStylesheet() {
        return this;
    }

    public boolean isAggregatedType() {
        return false;
    }

    public boolean isRoot() {
        return false;
    }

    private void readObject(ObjectInputStream objectInputStream) throws TransformerException, IOException {
        try {
            objectInputStream.defaultReadObject();
        } catch (ClassNotFoundException e) {
            throw new TransformerException(e);
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
    }

    public void setXmlnsXsl(String str) {
        this.m_XmlnsXsl = str;
    }

    public String getXmlnsXsl() {
        return this.m_XmlnsXsl;
    }

    public void setExtensionElementPrefixes(StringVector stringVector) {
        this.m_ExtensionElementURIs = stringVector;
    }

    public String getExtensionElementPrefix(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_ExtensionElementURIs == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.m_ExtensionElementURIs.elementAt(i);
    }

    public int getExtensionElementPrefixCount() {
        if (this.m_ExtensionElementURIs != null) {
            return this.m_ExtensionElementURIs.size();
        }
        return 0;
    }

    public boolean containsExtensionElementURI(String str) {
        if (this.m_ExtensionElementURIs == null) {
            return false;
        }
        return this.m_ExtensionElementURIs.contains(str);
    }

    public void setExcludeResultPrefixes(StringVector stringVector) {
        this.m_ExcludeResultPrefixs = stringVector;
    }

    public String getExcludeResultPrefix(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_ExcludeResultPrefixs == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.m_ExcludeResultPrefixs.elementAt(i);
    }

    public int getExcludeResultPrefixCount() {
        if (this.m_ExcludeResultPrefixs != null) {
            return this.m_ExcludeResultPrefixs.size();
        }
        return 0;
    }

    @Override
    public boolean containsExcludeResultPrefix(String str, String str2) {
        if (this.m_ExcludeResultPrefixs == null || str2 == null) {
            return false;
        }
        for (int i = 0; i < this.m_ExcludeResultPrefixs.size(); i++) {
            if (str2.equals(getNamespaceForPrefix(this.m_ExcludeResultPrefixs.elementAt(i)))) {
                return true;
            }
        }
        return false;
    }

    public void setId(String str) {
        this.m_Id = str;
    }

    public String getId() {
        return this.m_Id;
    }

    public void setVersion(String str) {
        this.m_Version = str;
        this.m_isCompatibleMode = Double.valueOf(str).doubleValue() > 1.0d;
    }

    public boolean getCompatibleMode() {
        return this.m_isCompatibleMode;
    }

    public String getVersion() {
        return this.m_Version;
    }

    public void setImport(StylesheetComposed stylesheetComposed) {
        if (this.m_imports == null) {
            this.m_imports = new Vector();
        }
        this.m_imports.addElement(stylesheetComposed);
    }

    public StylesheetComposed getImport(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_imports == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (StylesheetComposed) this.m_imports.elementAt(i);
    }

    public int getImportCount() {
        if (this.m_imports != null) {
            return this.m_imports.size();
        }
        return 0;
    }

    public void setInclude(Stylesheet stylesheet) {
        if (this.m_includes == null) {
            this.m_includes = new Vector();
        }
        this.m_includes.addElement(stylesheet);
    }

    public Stylesheet getInclude(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_includes == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (Stylesheet) this.m_includes.elementAt(i);
    }

    public int getIncludeCount() {
        if (this.m_includes != null) {
            return this.m_includes.size();
        }
        return 0;
    }

    public void setDecimalFormat(DecimalFormatProperties decimalFormatProperties) {
        if (this.m_DecimalFormatDeclarations == null) {
            this.m_DecimalFormatDeclarations = new Stack();
        }
        this.m_DecimalFormatDeclarations.push(decimalFormatProperties);
    }

    public DecimalFormatProperties getDecimalFormat(QName qName) {
        if (this.m_DecimalFormatDeclarations == null) {
            return null;
        }
        for (int decimalFormatCount = getDecimalFormatCount() - 1; decimalFormatCount >= 0; decimalFormatCount++) {
            DecimalFormatProperties decimalFormat = getDecimalFormat(decimalFormatCount);
            if (decimalFormat.getName().equals(qName)) {
                return decimalFormat;
            }
        }
        return null;
    }

    public DecimalFormatProperties getDecimalFormat(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_DecimalFormatDeclarations == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (DecimalFormatProperties) this.m_DecimalFormatDeclarations.elementAt(i);
    }

    public int getDecimalFormatCount() {
        if (this.m_DecimalFormatDeclarations != null) {
            return this.m_DecimalFormatDeclarations.size();
        }
        return 0;
    }

    public void setStripSpaces(WhiteSpaceInfo whiteSpaceInfo) {
        if (this.m_whitespaceStrippingElements == null) {
            this.m_whitespaceStrippingElements = new Vector();
        }
        this.m_whitespaceStrippingElements.addElement(whiteSpaceInfo);
    }

    public WhiteSpaceInfo getStripSpace(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_whitespaceStrippingElements == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (WhiteSpaceInfo) this.m_whitespaceStrippingElements.elementAt(i);
    }

    public int getStripSpaceCount() {
        if (this.m_whitespaceStrippingElements != null) {
            return this.m_whitespaceStrippingElements.size();
        }
        return 0;
    }

    public void setPreserveSpaces(WhiteSpaceInfo whiteSpaceInfo) {
        if (this.m_whitespacePreservingElements == null) {
            this.m_whitespacePreservingElements = new Vector();
        }
        this.m_whitespacePreservingElements.addElement(whiteSpaceInfo);
    }

    public WhiteSpaceInfo getPreserveSpace(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_whitespacePreservingElements == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (WhiteSpaceInfo) this.m_whitespacePreservingElements.elementAt(i);
    }

    public int getPreserveSpaceCount() {
        if (this.m_whitespacePreservingElements != null) {
            return this.m_whitespacePreservingElements.size();
        }
        return 0;
    }

    public void setOutput(OutputProperties outputProperties) {
        if (this.m_output == null) {
            this.m_output = new Vector();
        }
        this.m_output.addElement(outputProperties);
    }

    public OutputProperties getOutput(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_output == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (OutputProperties) this.m_output.elementAt(i);
    }

    public int getOutputCount() {
        if (this.m_output != null) {
            return this.m_output.size();
        }
        return 0;
    }

    public void setKey(KeyDeclaration keyDeclaration) {
        if (this.m_keyDeclarations == null) {
            this.m_keyDeclarations = new Vector();
        }
        this.m_keyDeclarations.addElement(keyDeclaration);
    }

    public KeyDeclaration getKey(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_keyDeclarations == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (KeyDeclaration) this.m_keyDeclarations.elementAt(i);
    }

    public int getKeyCount() {
        if (this.m_keyDeclarations != null) {
            return this.m_keyDeclarations.size();
        }
        return 0;
    }

    public void setAttributeSet(ElemAttributeSet elemAttributeSet) {
        if (this.m_attributeSets == null) {
            this.m_attributeSets = new Vector();
        }
        this.m_attributeSets.addElement(elemAttributeSet);
    }

    public ElemAttributeSet getAttributeSet(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_attributeSets == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (ElemAttributeSet) this.m_attributeSets.elementAt(i);
    }

    public int getAttributeSetCount() {
        if (this.m_attributeSets != null) {
            return this.m_attributeSets.size();
        }
        return 0;
    }

    public void setVariable(ElemVariable elemVariable) {
        if (this.m_topLevelVariables == null) {
            this.m_topLevelVariables = new Vector();
        }
        this.m_topLevelVariables.addElement(elemVariable);
    }

    public ElemVariable getVariableOrParam(QName qName) {
        if (this.m_topLevelVariables != null) {
            int variableOrParamCount = getVariableOrParamCount();
            for (int i = 0; i < variableOrParamCount; i++) {
                ElemVariable variableOrParam = getVariableOrParam(i);
                if (variableOrParam.getName().equals(qName)) {
                    return variableOrParam;
                }
            }
            return null;
        }
        return null;
    }

    public ElemVariable getVariable(QName qName) {
        if (this.m_topLevelVariables != null) {
            int variableOrParamCount = getVariableOrParamCount();
            for (int i = 0; i < variableOrParamCount; i++) {
                ElemVariable variableOrParam = getVariableOrParam(i);
                if (variableOrParam.getXSLToken() == 73 && variableOrParam.getName().equals(qName)) {
                    return variableOrParam;
                }
            }
            return null;
        }
        return null;
    }

    public ElemVariable getVariableOrParam(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_topLevelVariables == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (ElemVariable) this.m_topLevelVariables.elementAt(i);
    }

    public int getVariableOrParamCount() {
        if (this.m_topLevelVariables != null) {
            return this.m_topLevelVariables.size();
        }
        return 0;
    }

    public void setParam(ElemParam elemParam) {
        setVariable(elemParam);
    }

    public ElemParam getParam(QName qName) {
        if (this.m_topLevelVariables != null) {
            int variableOrParamCount = getVariableOrParamCount();
            for (int i = 0; i < variableOrParamCount; i++) {
                ElemVariable variableOrParam = getVariableOrParam(i);
                if (variableOrParam.getXSLToken() == 41 && variableOrParam.getName().equals(qName)) {
                    return (ElemParam) variableOrParam;
                }
            }
            return null;
        }
        return null;
    }

    public void setTemplate(ElemTemplate elemTemplate) {
        if (this.m_templates == null) {
            this.m_templates = new Vector();
        }
        this.m_templates.addElement(elemTemplate);
        elemTemplate.setStylesheet(this);
    }

    public ElemTemplate getTemplate(int i) throws TransformerException {
        if (this.m_templates == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (ElemTemplate) this.m_templates.elementAt(i);
    }

    public int getTemplateCount() {
        if (this.m_templates != null) {
            return this.m_templates.size();
        }
        return 0;
    }

    public void setNamespaceAlias(NamespaceAlias namespaceAlias) {
        if (this.m_prefix_aliases == null) {
            this.m_prefix_aliases = new Vector();
        }
        this.m_prefix_aliases.addElement(namespaceAlias);
    }

    public NamespaceAlias getNamespaceAlias(int i) throws ArrayIndexOutOfBoundsException {
        if (this.m_prefix_aliases == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (NamespaceAlias) this.m_prefix_aliases.elementAt(i);
    }

    public int getNamespaceAliasCount() {
        if (this.m_prefix_aliases != null) {
            return this.m_prefix_aliases.size();
        }
        return 0;
    }

    public void setNonXslTopLevel(QName qName, Object obj) {
        if (this.m_NonXslTopLevel == null) {
            this.m_NonXslTopLevel = new Hashtable();
        }
        this.m_NonXslTopLevel.put(qName, obj);
    }

    public Object getNonXslTopLevel(QName qName) {
        if (this.m_NonXslTopLevel != null) {
            return this.m_NonXslTopLevel.get(qName);
        }
        return null;
    }

    public String getHref() {
        return this.m_href;
    }

    public void setHref(String str) {
        this.m_href = str;
    }

    @Override
    public void setLocaterInfo(SourceLocator sourceLocator) {
        if (sourceLocator != null) {
            this.m_publicId = sourceLocator.getPublicId();
            this.m_systemId = sourceLocator.getSystemId();
            if (this.m_systemId != null) {
                try {
                    this.m_href = SystemIDResolver.getAbsoluteURI(this.m_systemId, null);
                } catch (TransformerException e) {
                }
            }
            super.setLocaterInfo(sourceLocator);
        }
    }

    @Override
    public StylesheetRoot getStylesheetRoot() {
        return this.m_stylesheetRoot;
    }

    public void setStylesheetRoot(StylesheetRoot stylesheetRoot) {
        this.m_stylesheetRoot = stylesheetRoot;
    }

    public Stylesheet getStylesheetParent() {
        return this.m_stylesheetParent;
    }

    public void setStylesheetParent(Stylesheet stylesheet) {
        this.m_stylesheetParent = stylesheet;
    }

    @Override
    public StylesheetComposed getStylesheetComposed() {
        Stylesheet stylesheetParent = this;
        while (!stylesheetParent.isAggregatedType()) {
            stylesheetParent = stylesheetParent.getStylesheetParent();
        }
        return (StylesheetComposed) stylesheetParent;
    }

    @Override
    public short getNodeType() {
        return (short) 9;
    }

    @Override
    public int getXSLToken() {
        return 25;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_STYLESHEET_STRING;
    }

    public void replaceTemplate(ElemTemplate elemTemplate, int i) throws TransformerException {
        if (this.m_templates == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        replaceChild((ElemTemplateElement) elemTemplate, (ElemTemplateElement) this.m_templates.elementAt(i));
        this.m_templates.setElementAt(elemTemplate, i);
        elemTemplate.setStylesheet(this);
    }

    @Override
    protected void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        int importCount = getImportCount();
        for (int i = 0; i < importCount; i++) {
            getImport(i).callVisitors(xSLTVisitor);
        }
        int includeCount = getIncludeCount();
        for (int i2 = 0; i2 < includeCount; i2++) {
            getInclude(i2).callVisitors(xSLTVisitor);
        }
        int outputCount = getOutputCount();
        for (int i3 = 0; i3 < outputCount; i3++) {
            xSLTVisitor.visitTopLevelInstruction(getOutput(i3));
        }
        int attributeSetCount = getAttributeSetCount();
        for (int i4 = 0; i4 < attributeSetCount; i4++) {
            ElemAttributeSet attributeSet = getAttributeSet(i4);
            if (xSLTVisitor.visitTopLevelInstruction(attributeSet)) {
                attributeSet.callChildVisitors(xSLTVisitor);
            }
        }
        int decimalFormatCount = getDecimalFormatCount();
        for (int i5 = 0; i5 < decimalFormatCount; i5++) {
            xSLTVisitor.visitTopLevelInstruction(getDecimalFormat(i5));
        }
        int keyCount = getKeyCount();
        for (int i6 = 0; i6 < keyCount; i6++) {
            xSLTVisitor.visitTopLevelInstruction(getKey(i6));
        }
        int namespaceAliasCount = getNamespaceAliasCount();
        for (int i7 = 0; i7 < namespaceAliasCount; i7++) {
            xSLTVisitor.visitTopLevelInstruction(getNamespaceAlias(i7));
        }
        int templateCount = getTemplateCount();
        for (int i8 = 0; i8 < templateCount; i8++) {
            try {
                ElemTemplate template = getTemplate(i8);
                if (xSLTVisitor.visitTopLevelInstruction(template)) {
                    template.callChildVisitors(xSLTVisitor);
                }
            } catch (TransformerException e) {
                throw new WrappedRuntimeException(e);
            }
        }
        int variableOrParamCount = getVariableOrParamCount();
        for (int i9 = 0; i9 < variableOrParamCount; i9++) {
            ElemVariable variableOrParam = getVariableOrParam(i9);
            if (xSLTVisitor.visitTopLevelVariableOrParamDecl(variableOrParam)) {
                variableOrParam.callChildVisitors(xSLTVisitor);
            }
        }
        int stripSpaceCount = getStripSpaceCount();
        for (int i10 = 0; i10 < stripSpaceCount; i10++) {
            xSLTVisitor.visitTopLevelInstruction(getStripSpace(i10));
        }
        int preserveSpaceCount = getPreserveSpaceCount();
        for (int i11 = 0; i11 < preserveSpaceCount; i11++) {
            xSLTVisitor.visitTopLevelInstruction(getPreserveSpace(i11));
        }
        if (this.m_NonXslTopLevel != null) {
            Enumeration enumerationElements = this.m_NonXslTopLevel.elements();
            while (enumerationElements.hasMoreElements()) {
                ElemTemplateElement elemTemplateElement = (ElemTemplateElement) enumerationElements.nextElement();
                if (xSLTVisitor.visitTopLevelInstruction(elemTemplateElement)) {
                    elemTemplateElement.callChildVisitors(xSLTVisitor);
                }
            }
        }
    }

    @Override
    protected boolean accept(XSLTVisitor xSLTVisitor) {
        return xSLTVisitor.visitStylesheet(this);
    }
}
