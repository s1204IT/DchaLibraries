package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.extensions.ExtensionNamespaceSupport;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.VariableStack;
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

    public void execute(TransformerImpl transformerImpl, XObject[] xObjectArr) throws TransformerException {
        VariableStack varStack = transformerImpl.getXPathContext().getVarStack();
        int stackFrame = varStack.getStackFrame();
        int iLink = varStack.link(this.m_frameSize);
        if (this.m_inArgsSize < xObjectArr.length) {
            throw new TransformerException("function called with too many args");
        }
        if (this.m_inArgsSize > 0) {
            varStack.clearLocalSlots(0, this.m_inArgsSize);
            if (xObjectArr.length > 0) {
                varStack.setStackFrame(stackFrame);
                NodeList childNodes = getChildNodes();
                for (int i = 0; i < xObjectArr.length; i++) {
                    childNodes.item(i);
                    if (childNodes.item(i) instanceof ElemParam) {
                        varStack.setLocalVariable(((ElemParam) childNodes.item(i)).getIndex(), xObjectArr[i], iLink);
                    }
                }
                varStack.setStackFrame(iLink);
            }
        }
        varStack.setStackFrame(iLink);
        transformerImpl.executeChildTemplates((ElemTemplateElement) this, true);
        varStack.unlink(stackFrame);
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        String namespace = getName().getNamespace();
        String extensionHandlerClass = stylesheetRoot.getExtensionHandlerClass();
        stylesheetRoot.getExtensionNamespacesManager().registerExtension(new ExtensionNamespaceSupport(namespace, extensionHandlerClass, new Object[]{namespace, stylesheetRoot}));
        if (!namespace.equals(org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL)) {
            stylesheetRoot.getExtensionNamespacesManager().registerExtension(new ExtensionNamespaceSupport(org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL, extensionHandlerClass, new Object[]{org.apache.xml.utils.Constants.S_EXSLT_FUNCTIONS_URL, stylesheetRoot}));
        }
    }
}
