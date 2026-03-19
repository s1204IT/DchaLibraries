package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;

public class XUnresolvedVariable extends XObject {
    static final long serialVersionUID = -256779804767950188L;
    private transient int m_context;
    private transient boolean m_doneEval;
    private boolean m_isGlobal;
    private transient TransformerImpl m_transformer;
    private transient int m_varStackContext;
    private transient int m_varStackPos;

    public XUnresolvedVariable(ElemVariable elemVariable, int i, TransformerImpl transformerImpl, int i2, int i3, boolean z) {
        super(elemVariable);
        this.m_varStackPos = -1;
        this.m_doneEval = true;
        this.m_context = i;
        this.m_transformer = transformerImpl;
        this.m_varStackPos = i2;
        this.m_varStackContext = i3;
        this.m_isGlobal = z;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        if (!this.m_doneEval) {
            this.m_transformer.getMsgMgr().error(xPathContext.getSAXLocator(), XSLTErrorResources.ER_REFERENCING_ITSELF, new Object[]{((ElemVariable) object()).getName().getLocalName()});
        }
        VariableStack varStack = xPathContext.getVarStack();
        int stackFrame = varStack.getStackFrame();
        ElemVariable elemVariable = (ElemVariable) this.m_obj;
        try {
            this.m_doneEval = false;
            if (-1 != elemVariable.m_frameSize) {
                varStack.link(elemVariable.m_frameSize);
            }
            XObject value = elemVariable.getValue(this.m_transformer, this.m_context);
            this.m_doneEval = true;
            return value;
        } finally {
            if (-1 != elemVariable.m_frameSize) {
                varStack.unlink(stackFrame);
            }
        }
    }

    public void setVarStackPos(int i) {
        this.m_varStackPos = i;
    }

    public void setVarStackContext(int i) {
        this.m_varStackContext = i;
    }

    @Override
    public int getType() {
        return XObject.CLASS_UNRESOLVEDVARIABLE;
    }

    @Override
    public String getTypeString() {
        return "XUnresolvedVariable (" + object().getClass().getName() + ")";
    }
}
