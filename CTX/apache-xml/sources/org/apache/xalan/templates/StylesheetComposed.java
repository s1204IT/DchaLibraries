package org.apache.xalan.templates;

import java.util.Vector;
import javax.xml.transform.TransformerException;

public class StylesheetComposed extends Stylesheet {
    static final long serialVersionUID = -3444072247410233923L;
    private int m_endImportCountComposed;
    private int m_importCountComposed;
    private int m_importNumber;
    private transient Vector m_includesComposed;

    public StylesheetComposed(Stylesheet stylesheet) {
        super(stylesheet);
        this.m_importNumber = -1;
    }

    @Override
    public boolean isAggregatedType() {
        return true;
    }

    public void recompose(Vector vector) throws TransformerException {
        int includeCountComposed = getIncludeCountComposed();
        for (int i = -1; i < includeCountComposed; i++) {
            Stylesheet includeComposed = getIncludeComposed(i);
            int outputCount = includeComposed.getOutputCount();
            for (int i2 = 0; i2 < outputCount; i2++) {
                vector.addElement(includeComposed.getOutput(i2));
            }
            int attributeSetCount = includeComposed.getAttributeSetCount();
            for (int i3 = 0; i3 < attributeSetCount; i3++) {
                vector.addElement(includeComposed.getAttributeSet(i3));
            }
            int decimalFormatCount = includeComposed.getDecimalFormatCount();
            for (int i4 = 0; i4 < decimalFormatCount; i4++) {
                vector.addElement(includeComposed.getDecimalFormat(i4));
            }
            int keyCount = includeComposed.getKeyCount();
            for (int i5 = 0; i5 < keyCount; i5++) {
                vector.addElement(includeComposed.getKey(i5));
            }
            int namespaceAliasCount = includeComposed.getNamespaceAliasCount();
            for (int i6 = 0; i6 < namespaceAliasCount; i6++) {
                vector.addElement(includeComposed.getNamespaceAlias(i6));
            }
            int templateCount = includeComposed.getTemplateCount();
            for (int i7 = 0; i7 < templateCount; i7++) {
                vector.addElement(includeComposed.getTemplate(i7));
            }
            int variableOrParamCount = includeComposed.getVariableOrParamCount();
            for (int i8 = 0; i8 < variableOrParamCount; i8++) {
                vector.addElement(includeComposed.getVariableOrParam(i8));
            }
            int stripSpaceCount = includeComposed.getStripSpaceCount();
            for (int i9 = 0; i9 < stripSpaceCount; i9++) {
                vector.addElement(includeComposed.getStripSpace(i9));
            }
            int preserveSpaceCount = includeComposed.getPreserveSpaceCount();
            for (int i10 = 0; i10 < preserveSpaceCount; i10++) {
                vector.addElement(includeComposed.getPreserveSpace(i10));
            }
        }
    }

    void recomposeImports() {
        this.m_importNumber = getStylesheetRoot().getImportNumber(this);
        this.m_importCountComposed = (getStylesheetRoot().getGlobalImportCount() - this.m_importNumber) - 1;
        int importCount = getImportCount();
        if (importCount > 0) {
            this.m_endImportCountComposed += importCount;
            while (importCount > 0) {
                importCount--;
                this.m_endImportCountComposed += getImport(importCount).getEndImportCountComposed();
            }
        }
        int includeCountComposed = getIncludeCountComposed();
        while (includeCountComposed > 0) {
            includeCountComposed--;
            int importCount2 = getIncludeComposed(includeCountComposed).getImportCount();
            this.m_endImportCountComposed += importCount2;
            while (importCount2 > 0) {
                importCount2--;
                this.m_endImportCountComposed += getIncludeComposed(includeCountComposed).getImport(importCount2).getEndImportCountComposed();
            }
        }
    }

    public StylesheetComposed getImportComposed(int i) throws ArrayIndexOutOfBoundsException {
        return getStylesheetRoot().getGlobalImport(1 + this.m_importNumber + i);
    }

    public int getImportCountComposed() {
        return this.m_importCountComposed;
    }

    public int getEndImportCountComposed() {
        return this.m_endImportCountComposed;
    }

    void recomposeIncludes(Stylesheet stylesheet) {
        int includeCount = stylesheet.getIncludeCount();
        if (includeCount > 0) {
            if (this.m_includesComposed == null) {
                this.m_includesComposed = new Vector();
            }
            for (int i = 0; i < includeCount; i++) {
                Stylesheet include = stylesheet.getInclude(i);
                this.m_includesComposed.addElement(include);
                recomposeIncludes(include);
            }
        }
    }

    public Stylesheet getIncludeComposed(int i) throws ArrayIndexOutOfBoundsException {
        if (-1 == i) {
            return this;
        }
        if (this.m_includesComposed == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (Stylesheet) this.m_includesComposed.elementAt(i);
    }

    public int getIncludeCountComposed() {
        if (this.m_includesComposed != null) {
            return this.m_includesComposed.size();
        }
        return 0;
    }

    public void recomposeTemplates(boolean z) throws TransformerException {
    }
}
