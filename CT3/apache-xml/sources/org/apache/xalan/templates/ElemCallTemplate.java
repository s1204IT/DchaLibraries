package org.apache.xalan.templates;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.utils.QName;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;

public class ElemCallTemplate extends ElemForEach {
    static final long serialVersionUID = 5009634612916030591L;
    public QName m_templateName = null;
    private ElemTemplate m_template = null;
    protected ElemWithParam[] m_paramElems = null;

    public void setName(QName name) {
        this.m_templateName = name;
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
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        int length = getParamElemCount();
        for (int i = 0; i < length; i++) {
            getParamElem(i).compose(sroot);
        }
        if (this.m_templateName == null || this.m_template != null) {
            return;
        }
        this.m_template = getStylesheetRoot().getTemplateComposed(this.m_templateName);
        if (this.m_template == null) {
            String themsg = XSLMessages.createMessage(XSLTErrorResources.ER_ELEMTEMPLATEELEM_ERR, new Object[]{this.m_templateName});
            throw new TransformerException(themsg, this);
        }
        int length2 = getParamElemCount();
        for (int i2 = 0; i2 < length2; i2++) {
            ElemWithParam ewp = getParamElem(i2);
            ewp.m_index = -1;
            int etePos = 0;
            for (ElemTemplateElement ete = this.m_template.getFirstChildElem(); ete != null && ete.getXSLToken() == 41; ete = ete.getNextSiblingElem()) {
                ElemParam ep = (ElemParam) ete;
                if (ep.getName().equals(ewp.getName())) {
                    ewp.m_index = etePos;
                }
                etePos++;
            }
        }
    }

    @Override
    public void endCompose(StylesheetRoot sroot) throws TransformerException {
        int length = getParamElemCount();
        for (int i = 0; i < length; i++) {
            ElemWithParam ewp = getParamElem(i);
            ewp.endCompose(sroot);
        }
        super.endCompose(sroot);
    }

    @Override
    public void execute(TransformerImpl transformer) throws TransformerException {
        if (this.m_template != null) {
            XPathContext xctxt = transformer.getXPathContext();
            VariableStack vars = xctxt.getVarStack();
            int thisframe = vars.getStackFrame();
            int nextFrame = vars.link(this.m_template.m_frameSize);
            if (this.m_template.m_inArgsSize > 0) {
                vars.clearLocalSlots(0, this.m_template.m_inArgsSize);
                if (this.m_paramElems != null) {
                    int currentNode = xctxt.getCurrentNode();
                    vars.setStackFrame(thisframe);
                    int size = this.m_paramElems.length;
                    for (int i = 0; i < size; i++) {
                        ElemWithParam ewp = this.m_paramElems[i];
                        if (ewp.m_index >= 0) {
                            XObject obj = ewp.getValue(transformer, currentNode);
                            vars.setLocalVariable(ewp.m_index, obj, nextFrame);
                        }
                    }
                    vars.setStackFrame(nextFrame);
                }
            }
            SourceLocator savedLocator = xctxt.getSAXLocator();
            try {
                xctxt.setSAXLocator(this.m_template);
                transformer.pushElemTemplateElement(this.m_template);
                this.m_template.execute(transformer);
                return;
            } finally {
                transformer.popElemTemplateElement();
                xctxt.setSAXLocator(savedLocator);
                vars.unlink(thisframe);
            }
        }
        transformer.getMsgMgr().error(this, XSLTErrorResources.ER_TEMPLATE_NOT_FOUND, new Object[]{this.m_templateName});
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

    public void setParamElem(ElemWithParam ParamElem) {
        if (this.m_paramElems == null) {
            this.m_paramElems = new ElemWithParam[1];
            this.m_paramElems[0] = ParamElem;
            return;
        }
        int length = this.m_paramElems.length;
        ElemWithParam[] ewp = new ElemWithParam[length + 1];
        System.arraycopy(this.m_paramElems, 0, ewp, 0, length);
        this.m_paramElems = ewp;
        ewp[length] = ParamElem;
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement newChild) {
        int type = newChild.getXSLToken();
        if (2 == type) {
            setParamElem((ElemWithParam) newChild);
        }
        return super.appendChild(newChild);
    }

    @Override
    public void callChildVisitors(XSLTVisitor visitor, boolean callAttrs) {
        super.callChildVisitors(visitor, callAttrs);
    }
}
