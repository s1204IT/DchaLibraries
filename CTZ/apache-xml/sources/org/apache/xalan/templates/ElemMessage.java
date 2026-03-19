package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;

public class ElemMessage extends ElemTemplateElement {
    static final long serialVersionUID = 1530472462155060023L;
    private boolean m_terminate = false;

    public void setTerminate(boolean z) {
        this.m_terminate = z;
    }

    public boolean getTerminate() {
        return this.m_terminate;
    }

    @Override
    public int getXSLToken() {
        return 75;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_MESSAGE_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        transformerImpl.getMsgMgr().message(this, transformerImpl.transformToString(this), this.m_terminate);
        if (this.m_terminate) {
            transformerImpl.getErrorListener().fatalError(new TransformerException(XSLMessages.createMessage(XSLTErrorResources.ER_STYLESHEET_DIRECTED_TERMINATION, null)));
        }
    }
}
