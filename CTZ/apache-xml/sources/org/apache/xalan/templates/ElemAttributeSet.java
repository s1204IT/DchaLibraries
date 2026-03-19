package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;

public class ElemAttributeSet extends ElemUse {
    static final long serialVersionUID = -426740318278164496L;
    public QName m_qname = null;

    public void setName(QName qName) {
        this.m_qname = qName;
    }

    public QName getName() {
        return this.m_qname;
    }

    @Override
    public int getXSLToken() {
        return 40;
    }

    @Override
    public String getNodeName() {
        return "attribute-set";
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        if (transformerImpl.isRecursiveAttrSet(this)) {
            throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_XSLATTRSET_USED_ITSELF, new Object[]{this.m_qname.getLocalPart()}));
        }
        transformerImpl.pushElemAttributeSet(this);
        super.execute(transformerImpl);
        for (ElemAttribute elemAttribute = (ElemAttribute) getFirstChildElem(); elemAttribute != null; elemAttribute = (ElemAttribute) elemAttribute.getNextSiblingElem()) {
            elemAttribute.execute(transformerImpl);
        }
        transformerImpl.popElemAttributeSet();
    }

    public ElemTemplateElement appendChildElem(ElemTemplateElement elemTemplateElement) {
        if (elemTemplateElement.getXSLToken() != 48) {
            error(XSLTErrorResources.ER_CANNOT_ADD, new Object[]{elemTemplateElement.getNodeName(), getNodeName()});
        }
        return super.appendChild(elemTemplateElement);
    }

    @Override
    public void recompose(StylesheetRoot stylesheetRoot) {
        stylesheetRoot.recomposeAttributeSets(this);
    }
}
