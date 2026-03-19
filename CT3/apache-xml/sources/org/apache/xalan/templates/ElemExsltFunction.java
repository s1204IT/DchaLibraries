package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.extensions.ExtensionNamespaceSupport;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.NodeList;

public class ElemExsltFunction extends ElemTemplate {
    static final long serialVersionUID = 272154954793534771L;

    @Override
    public int getXSLToken() {
        return 88;
    }

    @Override
    public String getNodeName() {
        return Constants.EXSLT_ELEMNAME_FUNCTION_STRING;
    }

    public void execute(TransformerImpl transformer, XObject[] args) throws TransformerException {
        XPathContext xctxt = transformer.getXPathContext();
        VariableStack vars = xctxt.getVarStack();
        int thisFrame = vars.getStackFrame();
        int nextFrame = vars.link(this.m_frameSize);
        if (this.m_inArgsSize < args.length) {
            throw new TransformerException("function called with too many args");
        }
        if (this.m_inArgsSize > 0) {
            vars.clearLocalSlots(0, this.m_inArgsSize);
            if (args.length > 0) {
                vars.setStackFrame(thisFrame);
                NodeList children = getChildNodes();
                for (int i = 0; i < args.length; i++) {
                    children.item(i);
                    if (children.item(i) instanceof ElemParam) {
                        ElemParam param = (ElemParam) children.item(i);
                        vars.setLocalVariable(param.getIndex(), args[i], nextFrame);
                    }
                }
                vars.setStackFrame(nextFrame);
            }
        }
        vars.setStackFrame(nextFrame);
        transformer.executeChildTemplates((ElemTemplateElement) this, true);
        vars.unlink(thisFrame);
    }

    @Override
    public void compose(StylesheetRoot sroot) throws TransformerException {
        super.compose(sroot);
        String namespace = getName().getNamespace();
        String handlerClass = sroot.getExtensionHandlerClass();
        Object[] args = {namespace, sroot};
        ExtensionNamespaceSupport extNsSpt = new ExtensionNamespaceSupport(namespace, handlerClass, args);
        sroot.getExtensionNamespacesManager().registerExtension(extNsSpt);
        if (namespace.equals(org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL)) {
            return;
        }
        Object[] args2 = {org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL, sroot};
        ExtensionNamespaceSupport extNsSpt2 = new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL, handlerClass, args2);
        sroot.getExtensionNamespacesManager().registerExtension(extNsSpt2);
    }
}
