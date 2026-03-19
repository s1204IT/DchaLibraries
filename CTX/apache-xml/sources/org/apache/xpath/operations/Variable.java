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

    public void setIndex(int i) {
        this.m_index = i;
    }

    public int getIndex() {
        return this.m_index;
    }

    public void setIsGlobal(boolean z) {
        this.m_isGlobal = z;
    }

    public boolean getGlobal() {
        return this.m_isGlobal;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        this.m_fixUpWasCalled = true;
        vector.size();
        for (int size = vector.size() - 1; size >= 0; size--) {
            if (((QName) vector.elementAt(size)).equals(this.m_qname)) {
                if (size < i) {
                    this.m_isGlobal = true;
                    this.m_index = size;
                    return;
                } else {
                    this.m_index = size - i;
                    return;
                }
            }
        }
        throw new WrappedRuntimeException(new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_COULD_NOT_FIND_VAR, new Object[]{this.m_qname.toString()}), this));
    }

    public void setQName(QName qName) {
        this.m_qname = qName;
    }

    public QName getQName() {
        return this.m_qname;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext, false);
    }

    @Override
    public XObject execute(XPathContext xPathContext, boolean z) throws TransformerException {
        XObject variableOrParam;
        xPathContext.getNamespaceContext();
        if (this.m_fixUpWasCalled) {
            if (this.m_isGlobal) {
                variableOrParam = xPathContext.getVarStack().getGlobalVariable(xPathContext, this.m_index, z);
            } else {
                variableOrParam = xPathContext.getVarStack().getLocalVariable(xPathContext, this.m_index, z);
            }
        } else {
            variableOrParam = xPathContext.getVarStack().getVariableOrParam(xPathContext, this.m_qname);
        }
        if (variableOrParam == null) {
            warn(xPathContext, XPATHErrorResources.WG_ILLEGAL_VARIABLE_REFERENCE, new Object[]{this.m_qname.getLocalPart()});
            return new XNodeSet(xPathContext.getDTMManager());
        }
        return variableOrParam;
    }

    public ElemVariable getElemVariable() {
        ExpressionNode expressionOwner = getExpressionOwner();
        if (expressionOwner == null || !(expressionOwner instanceof ElemTemplateElement)) {
            return null;
        }
        ElemTemplateElement parentElem = (ElemTemplateElement) expressionOwner;
        if (!(parentElem instanceof Stylesheet)) {
            while (parentElem != null && !(parentElem.getParentNode() instanceof Stylesheet)) {
                ElemTemplateElement previousSiblingElem = parentElem;
                while (true) {
                    previousSiblingElem = previousSiblingElem.getPreviousSiblingElem();
                    if (previousSiblingElem != null) {
                        if (previousSiblingElem instanceof ElemVariable) {
                            ElemVariable elemVariable = (ElemVariable) previousSiblingElem;
                            if (elemVariable.getName().equals(this.m_qname)) {
                                return elemVariable;
                            }
                        }
                    }
                }
            }
        }
        if (parentElem == null) {
            return null;
        }
        return parentElem.getStylesheetRoot().getVariableOrParamComposed(this.m_qname);
    }

    @Override
    public boolean isStableNumber() {
        return true;
    }

    @Override
    public int getAnalysisBits() {
        XPath select;
        ExpressionNode expression;
        ElemVariable elemVariable = getElemVariable();
        if (elemVariable != null && (select = elemVariable.getSelect()) != null && (expression = select.getExpression()) != null && (expression instanceof PathComponent)) {
            return ((PathComponent) expression).getAnalysisBits();
        }
        return WalkerFactory.BIT_FILTER;
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        xPathVisitor.visitVariableRef(expressionOwner, this);
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!isSameClass(expression)) {
            return false;
        }
        Variable variable = (Variable) expression;
        return this.m_qname.equals(variable.m_qname) && getElemVariable() == variable.getElemVariable();
    }

    public boolean isPsuedoVarRef() {
        java.lang.String namespaceURI = this.m_qname.getNamespaceURI();
        if (namespaceURI != null && namespaceURI.equals(PSUEDOVARNAMESPACE) && this.m_qname.getLocalName().startsWith("#")) {
            return true;
        }
        return false;
    }
}
