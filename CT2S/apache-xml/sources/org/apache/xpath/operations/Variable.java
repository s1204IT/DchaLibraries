package org.apache.xpath.operations;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionNode;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.axes.PathComponent;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;

public class Variable extends Expression implements PathComponent {
    static final java.lang.String PSUEDOVARNAMESPACE = "http://xml.apache.org/xalan/psuedovar";
    static final long serialVersionUID = -4334975375609297049L;
    protected int m_index;
    protected QName m_qname;
    private boolean m_fixUpWasCalled = false;
    protected boolean m_isGlobal = false;

    public void setIndex(int index) {
        this.m_index = index;
    }

    public int getIndex() {
        return this.m_index;
    }

    public void setIsGlobal(boolean isGlobal) {
        this.m_isGlobal = isGlobal;
    }

    public boolean getGlobal() {
        return this.m_isGlobal;
    }

    @Override
    public void fixupVariables(Vector vars, int globalsSize) {
        this.m_fixUpWasCalled = true;
        vars.size();
        for (int i = vars.size() - 1; i >= 0; i--) {
            QName qn = (QName) vars.elementAt(i);
            if (qn.equals(this.m_qname)) {
                if (i < globalsSize) {
                    this.m_isGlobal = true;
                    this.m_index = i;
                    return;
                } else {
                    this.m_index = i - globalsSize;
                    return;
                }
            }
        }
        java.lang.String msg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_COULD_NOT_FIND_VAR, new Object[]{this.m_qname.toString()});
        TransformerException te = new TransformerException(msg, this);
        throw new WrappedRuntimeException(te);
    }

    public void setQName(QName qname) {
        this.m_qname = qname;
    }

    public QName getQName() {
        return this.m_qname;
    }

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        return execute(xctxt, false);
    }

    @Override
    public XObject execute(XPathContext xctxt, boolean destructiveOK) throws TransformerException {
        XObject result;
        xctxt.getNamespaceContext();
        if (this.m_fixUpWasCalled) {
            if (this.m_isGlobal) {
                result = xctxt.getVarStack().getGlobalVariable(xctxt, this.m_index, destructiveOK);
            } else {
                result = xctxt.getVarStack().getLocalVariable(xctxt, this.m_index, destructiveOK);
            }
        } else {
            result = xctxt.getVarStack().getVariableOrParam(xctxt, this.m_qname);
        }
        if (result == null) {
            warn(xctxt, XPATHErrorResources.WG_ILLEGAL_VARIABLE_REFERENCE, new Object[]{this.m_qname.getLocalPart()});
            XObject result2 = new XNodeSet(xctxt.getDTMManager());
            return result2;
        }
        return result;
    }

    public ElemVariable getElemVariable() {
        ElemVariable vvar = null;
        ExpressionNode owner = getExpressionOwner();
        if (owner != null && (owner instanceof ElemTemplateElement)) {
            ElemTemplateElement prev = (ElemTemplateElement) owner;
            if (!(prev instanceof Stylesheet)) {
                while (prev != null && !(prev.getParentNode() instanceof Stylesheet)) {
                    ElemTemplateElement savedprev = prev;
                    while (true) {
                        prev = prev.getPreviousSiblingElem();
                        if (prev != null) {
                            if (prev instanceof ElemVariable) {
                                ElemVariable vvar2 = (ElemVariable) prev;
                                if (vvar2.getName().equals(this.m_qname)) {
                                    return vvar2;
                                }
                                vvar = null;
                            }
                        }
                    }
                }
            }
            if (prev != null) {
                vvar = prev.getStylesheetRoot().getVariableOrParamComposed(this.m_qname);
            }
        }
        return vvar;
    }

    @Override
    public boolean isStableNumber() {
        return true;
    }

    @Override
    public int getAnalysisBits() {
        XPath xpath;
        ExpressionNode expression;
        ElemVariable vvar = getElemVariable();
        return (vvar == null || (xpath = vvar.getSelect()) == null || (expression = xpath.getExpression()) == null || !(expression instanceof PathComponent)) ? WalkerFactory.BIT_FILTER : ((PathComponent) expression).getAnalysisBits();
    }

    @Override
    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        visitor.visitVariableRef(owner, this);
    }

    @Override
    public boolean deepEquals(Expression expr) {
        return isSameClass(expr) && this.m_qname.equals(((Variable) expr).m_qname) && getElemVariable() == ((Variable) expr).getElemVariable();
    }

    public boolean isPsuedoVarRef() {
        java.lang.String ns = this.m_qname.getNamespaceURI();
        return ns != null && ns.equals(PSUEDOVARNAMESPACE) && this.m_qname.getLocalName().startsWith("#");
    }
}
