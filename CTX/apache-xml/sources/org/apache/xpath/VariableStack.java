package org.apache.xpath;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.templates.ElemVariable;
import org.apache.xalan.templates.Stylesheet;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;

public class VariableStack implements Cloneable {
    public static final int CLEARLIMITATION = 1024;
    private static XObject[] m_nulls = new XObject[1024];
    private int _currentFrameBottom;
    int _frameTop;
    int[] _links;
    int _linksTop;
    XObject[] _stackFrames;

    public VariableStack() {
        reset();
    }

    public VariableStack(int i) {
        reset(i, i * 2);
    }

    public synchronized Object clone() throws CloneNotSupportedException {
        VariableStack variableStack;
        variableStack = (VariableStack) super.clone();
        variableStack._stackFrames = (XObject[]) this._stackFrames.clone();
        variableStack._links = (int[]) this._links.clone();
        return variableStack;
    }

    public XObject elementAt(int i) {
        return this._stackFrames[i];
    }

    public int size() {
        return this._frameTop;
    }

    public void reset() {
        reset(this._links == null ? 4096 : this._links.length, this._stackFrames == null ? WalkerFactory.BIT_ANCESTOR : this._stackFrames.length);
    }

    protected void reset(int i, int i2) {
        this._frameTop = 0;
        this._linksTop = 0;
        if (this._links == null) {
            this._links = new int[i];
        }
        int[] iArr = this._links;
        int i3 = this._linksTop;
        this._linksTop = i3 + 1;
        iArr[i3] = 0;
        this._stackFrames = new XObject[i2];
    }

    public void setStackFrame(int i) {
        this._currentFrameBottom = i;
    }

    public int getStackFrame() {
        return this._currentFrameBottom;
    }

    public int link(int i) {
        this._currentFrameBottom = this._frameTop;
        this._frameTop += i;
        if (this._frameTop >= this._stackFrames.length) {
            XObject[] xObjectArr = new XObject[this._stackFrames.length + 4096 + i];
            System.arraycopy(this._stackFrames, 0, xObjectArr, 0, this._stackFrames.length);
            this._stackFrames = xObjectArr;
        }
        if (this._linksTop + 1 >= this._links.length) {
            int[] iArr = new int[this._links.length + DTMFilter.SHOW_NOTATION];
            System.arraycopy(this._links, 0, iArr, 0, this._links.length);
            this._links = iArr;
        }
        int[] iArr2 = this._links;
        int i2 = this._linksTop;
        this._linksTop = i2 + 1;
        iArr2[i2] = this._currentFrameBottom;
        return this._currentFrameBottom;
    }

    public void unlink() {
        int[] iArr = this._links;
        int i = this._linksTop - 1;
        this._linksTop = i;
        this._frameTop = iArr[i];
        this._currentFrameBottom = this._links[this._linksTop - 1];
    }

    public void unlink(int i) {
        int[] iArr = this._links;
        int i2 = this._linksTop - 1;
        this._linksTop = i2;
        this._frameTop = iArr[i2];
        this._currentFrameBottom = i;
    }

    public void setLocalVariable(int i, XObject xObject) {
        this._stackFrames[i + this._currentFrameBottom] = xObject;
    }

    public void setLocalVariable(int i, XObject xObject, int i2) {
        this._stackFrames[i + i2] = xObject;
    }

    public XObject getLocalVariable(XPathContext xPathContext, int i) throws TransformerException {
        int i2 = i + this._currentFrameBottom;
        XObject xObject = this._stackFrames[i2];
        if (xObject == null) {
            throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VARIABLE_ACCESSED_BEFORE_BIND, null), xPathContext.getSAXLocator());
        }
        if (xObject.getType() == 600) {
            XObject[] xObjectArr = this._stackFrames;
            XObject xObjectExecute = xObject.execute(xPathContext);
            xObjectArr[i2] = xObjectExecute;
            return xObjectExecute;
        }
        return xObject;
    }

    public XObject getLocalVariable(int i, int i2) throws TransformerException {
        return this._stackFrames[i + i2];
    }

    public XObject getLocalVariable(XPathContext xPathContext, int i, boolean z) throws TransformerException {
        int i2 = i + this._currentFrameBottom;
        XObject xObject = this._stackFrames[i2];
        if (xObject == null) {
            throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VARIABLE_ACCESSED_BEFORE_BIND, null), xPathContext.getSAXLocator());
        }
        if (xObject.getType() != 600) {
            return z ? xObject : xObject.getFresh();
        }
        XObject[] xObjectArr = this._stackFrames;
        XObject xObjectExecute = xObject.execute(xPathContext);
        xObjectArr[i2] = xObjectExecute;
        return xObjectExecute;
    }

    public boolean isLocalSet(int i) throws TransformerException {
        return this._stackFrames[i + this._currentFrameBottom] != null;
    }

    public void clearLocalSlots(int i, int i2) {
        System.arraycopy(m_nulls, 0, this._stackFrames, i + this._currentFrameBottom, i2);
    }

    public void setGlobalVariable(int i, XObject xObject) {
        this._stackFrames[i] = xObject;
    }

    public XObject getGlobalVariable(XPathContext xPathContext, int i) throws TransformerException {
        XObject xObject = this._stackFrames[i];
        if (xObject.getType() == 600) {
            XObject[] xObjectArr = this._stackFrames;
            XObject xObjectExecute = xObject.execute(xPathContext);
            xObjectArr[i] = xObjectExecute;
            return xObjectExecute;
        }
        return xObject;
    }

    public XObject getGlobalVariable(XPathContext xPathContext, int i, boolean z) throws TransformerException {
        XObject xObject = this._stackFrames[i];
        if (xObject.getType() != 600) {
            return z ? xObject : xObject.getFresh();
        }
        XObject[] xObjectArr = this._stackFrames;
        XObject xObjectExecute = xObject.execute(xPathContext);
        xObjectArr[i] = xObjectExecute;
        return xObjectExecute;
    }

    public XObject getVariableOrParam(XPathContext xPathContext, QName qName) throws TransformerException {
        PrefixResolver namespaceContext = xPathContext.getNamespaceContext();
        if (namespaceContext instanceof ElemTemplateElement) {
            ElemTemplateElement parentElem = (ElemTemplateElement) namespaceContext;
            if (!(parentElem instanceof Stylesheet)) {
                while (!(parentElem.getParentNode() instanceof Stylesheet)) {
                    ElemTemplateElement previousSiblingElem = parentElem;
                    while (true) {
                        previousSiblingElem = previousSiblingElem.getPreviousSiblingElem();
                        if (previousSiblingElem != null) {
                            if (previousSiblingElem instanceof ElemVariable) {
                                ElemVariable elemVariable = (ElemVariable) previousSiblingElem;
                                if (elemVariable.getName().equals(qName)) {
                                    return getLocalVariable(xPathContext, elemVariable.getIndex());
                                }
                            }
                        }
                    }
                }
            }
            ElemVariable variableOrParamComposed = parentElem.getStylesheetRoot().getVariableOrParamComposed(qName);
            if (variableOrParamComposed != null) {
                return getGlobalVariable(xPathContext, variableOrParamComposed.getIndex());
            }
        }
        throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VAR_NOT_RESOLVABLE, new Object[]{qName.toString()}));
    }
}
