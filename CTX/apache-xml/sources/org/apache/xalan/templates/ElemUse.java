package org.apache.xalan.templates;

import java.util.ArrayList;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;

public class ElemUse extends ElemTemplateElement {
    static final long serialVersionUID = 5830057200289299736L;
    private QName[] m_attributeSetsNames = null;

    public void setUseAttributeSets(Vector vector) {
        int size = vector.size();
        this.m_attributeSetsNames = new QName[size];
        for (int i = 0; i < size; i++) {
            this.m_attributeSetsNames[i] = (QName) vector.elementAt(i);
        }
    }

    public void setUseAttributeSets(QName[] qNameArr) {
        this.m_attributeSetsNames = qNameArr;
    }

    public QName[] getUseAttributeSets() {
        return this.m_attributeSetsNames;
    }

    public void applyAttrSets(TransformerImpl transformerImpl, StylesheetRoot stylesheetRoot) throws TransformerException {
        applyAttrSets(transformerImpl, stylesheetRoot, this.m_attributeSetsNames);
    }

    private void applyAttrSets(TransformerImpl transformerImpl, StylesheetRoot stylesheetRoot, QName[] qNameArr) throws TransformerException {
        if (qNameArr != null) {
            for (QName qName : qNameArr) {
                ArrayList attributeSetComposed = stylesheetRoot.getAttributeSetComposed(qName);
                if (attributeSetComposed != null) {
                    for (int size = attributeSetComposed.size() - 1; size >= 0; size--) {
                        ((ElemAttributeSet) attributeSetComposed.get(size)).execute(transformerImpl);
                    }
                } else {
                    throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_NO_ATTRIB_SET, new Object[]{qName}), this);
                }
            }
        }
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        if (this.m_attributeSetsNames != null) {
            applyAttrSets(transformerImpl, getStylesheetRoot(), this.m_attributeSetsNames);
        }
    }
}
