package org.apache.xalan.templates;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;

public class ElemCallTemplate extends ElemForEach {
    static final long serialVersionUID = 5009634612916030591L;
    public QName m_templateName = null;
    private ElemTemplate m_template = null;
    protected ElemWithParam[] m_paramElems = null;

    public void setName(QName qName) {
        this.m_templateName = qName;
    }

    public QName getName() {
        return this.m_templateName;
    }

    @Override
    public int getXSLToken() {
        return 17;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_CALLTEMPLATE_STRING;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        int paramElemCount = getParamElemCount();
        for (int i = 0; i < paramElemCount; i++) {
            getParamElem(i).compose(stylesheetRoot);
        }
        if (this.m_templateName != null && this.m_template == null) {
            this.m_template = getStylesheetRoot().getTemplateComposed(this.m_templateName);
            if (this.m_template == null) {
                throw new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_ELEMTEMPLATEELEM_ERR, new Object[]{this.m_templateName}), this);
            }
            int paramElemCount2 = getParamElemCount();
            for (int i2 = 0; i2 < paramElemCount2; i2++) {
                ElemWithParam paramElem = getParamElem(i2);
                paramElem.m_index = -1;
                int i3 = 0;
                for (ElemTemplateElement firstChildElem = this.m_template.getFirstChildElem(); firstChildElem != null && firstChildElem.getXSLToken() == 41; firstChildElem = firstChildElem.getNextSiblingElem()) {
                    if (((ElemParam) firstChildElem).getName().equals(paramElem.getName())) {
                        paramElem.m_index = i3;
                    }
                    i3++;
                }
            }
        }
    }

    @Override
    public void endCompose(StylesheetRoot stylesheetRoot) throws TransformerException {
        int paramElemCount = getParamElemCount();
        for (int i = 0; i < paramElemCount; i++) {
            getParamElem(i).endCompose(stylesheetRoot);
        }
        super.endCompose(stylesheetRoot);
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        if (this.m_template != null) {
            XPathContext xPathContext = transformerImpl.getXPathContext();
            VariableStack varStack = xPathContext.getVarStack();
            int stackFrame = varStack.getStackFrame();
            int iLink = varStack.link(this.m_template.m_frameSize);
            if (this.m_template.m_inArgsSize > 0) {
                varStack.clearLocalSlots(0, this.m_template.m_inArgsSize);
                if (this.m_paramElems != null) {
                    int currentNode = xPathContext.getCurrentNode();
                    varStack.setStackFrame(stackFrame);
                    int length = this.m_paramElems.length;
                    for (int i = 0; i < length; i++) {
                        ElemWithParam elemWithParam = this.m_paramElems[i];
                        if (elemWithParam.m_index >= 0) {
                            varStack.setLocalVariable(elemWithParam.m_index, elemWithParam.getValue(transformerImpl, currentNode), iLink);
                        }
                    }
                    varStack.setStackFrame(iLink);
                }
            }
            SourceLocator sAXLocator = xPathContext.getSAXLocator();
            try {
                xPathContext.setSAXLocator(this.m_template);
                transformerImpl.pushElemTemplateElement(this.m_template);
                this.m_template.execute(transformerImpl);
                return;
            } finally {
                transformerImpl.popElemTemplateElement();
                xPathContext.setSAXLocator(sAXLocator);
                varStack.unlink(stackFrame);
            }
        }
        transformerImpl.getMsgMgr().error(this, XSLTErrorResources.ER_TEMPLATE_NOT_FOUND, new Object[]{this.m_templateName});
    }

    public int getParamElemCount() {
        if (this.m_paramElems == null) {
            return 0;
        }
        return this.m_paramElems.length;
    }

    public ElemWithParam getParamElem(int i) {
        return this.m_paramElems[i];
    }

    public void setParamElem(ElemWithParam elemWithParam) {
        if (this.m_paramElems == null) {
            this.m_paramElems = new ElemWithParam[1];
            this.m_paramElems[0] = elemWithParam;
            return;
        }
        int length = this.m_paramElems.length;
        ElemWithParam[] elemWithParamArr = new ElemWithParam[length + 1];
        System.arraycopy(this.m_paramElems, 0, elemWithParamArr, 0, length);
        this.m_paramElems = elemWithParamArr;
        elemWithParamArr[length] = elemWithParam;
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        if (2 == elemTemplateElement.getXSLToken()) {
            setParamElem((ElemWithParam) elemTemplateElement);
        }
        return super.appendChild(elemTemplateElement);
    }

    @Override
    public void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        super.callChildVisitors(xSLTVisitor, z);
    }
}
