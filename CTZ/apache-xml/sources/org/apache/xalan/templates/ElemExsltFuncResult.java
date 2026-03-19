package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;

public class ElemExsltFuncResult extends ElemVariable {
    static final long serialVersionUID = -3478311949388304563L;
    private boolean m_isResultSet = false;
    private XObject m_result = null;
    private int m_callerFrameSize = 0;

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        XPathContext xPathContext = transformerImpl.getXPathContext();
        if (transformerImpl.currentFuncResultSeen()) {
            throw new TransformerException("An EXSLT function cannot set more than one result!");
        }
        XObject value = getValue(transformerImpl, xPathContext.getCurrentNode());
        transformerImpl.popCurrentFuncResult();
        transformerImpl.pushCurrentFuncResult(value);
    }

    @Override
    public int getXSLToken() {
        return 89;
    }

    @Override
    public String getNodeName() {
        return Constants.EXSLT_ELEMNAME_FUNCRESULT_STRING;
    }
}
