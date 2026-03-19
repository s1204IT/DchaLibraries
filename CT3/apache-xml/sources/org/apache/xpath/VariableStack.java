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

    public VariableStack(int initStackSize) {
        reset(initStackSize, initStackSize * 2);
    }

    public synchronized Object clone() throws CloneNotSupportedException {
        VariableStack vs;
        vs = (VariableStack) super.clone();
        vs._stackFrames = (XObject[]) this._stackFrames.clone();
        vs._links = (int[]) this._links.clone();
        return vs;
    }

    public XObject elementAt(int i) {
        return this._stackFrames[i];
    }

    public int size() {
        return this._frameTop;
    }

    public void reset() {
        int linksSize = this._links == null ? 4096 : this._links.length;
        int varArraySize = this._stackFrames == null ? WalkerFactory.BIT_ANCESTOR : this._stackFrames.length;
        reset(linksSize, varArraySize);
    }

    protected void reset(int linksSize, int varArraySize) {
        this._frameTop = 0;
        this._linksTop = 0;
        if (this._links == null) {
            this._links = new int[linksSize];
        }
        int[] iArr = this._links;
        int i = this._linksTop;
        this._linksTop = i + 1;
        iArr[i] = 0;
        this._stackFrames = new XObject[varArraySize];
    }

    public void setStackFrame(int sf) {
        this._currentFrameBottom = sf;
    }

    public int getStackFrame() {
        return this._currentFrameBottom;
    }

    public int link(int size) {
        this._currentFrameBottom = this._frameTop;
        this._frameTop += size;
        if (this._frameTop >= this._stackFrames.length) {
            XObject[] newsf = new XObject[this._stackFrames.length + 4096 + size];
            System.arraycopy(this._stackFrames, 0, newsf, 0, this._stackFrames.length);
            this._stackFrames = newsf;
        }
        if (this._linksTop + 1 >= this._links.length) {
            int[] newlinks = new int[this._links.length + DTMFilter.SHOW_NOTATION];
            System.arraycopy(this._links, 0, newlinks, 0, this._links.length);
            this._links = newlinks;
        }
        int[] iArr = this._links;
        int i = this._linksTop;
        this._linksTop = i + 1;
        iArr[i] = this._currentFrameBottom;
        return this._currentFrameBottom;
    }

    public void unlink() {
        int[] iArr = this._links;
        int i = this._linksTop - 1;
        this._linksTop = i;
        this._frameTop = iArr[i];
        this._currentFrameBottom = this._links[this._linksTop - 1];
    }

    public void unlink(int currentFrame) {
        int[] iArr = this._links;
        int i = this._linksTop - 1;
        this._linksTop = i;
        this._frameTop = iArr[i];
        this._currentFrameBottom = currentFrame;
    }

    public void setLocalVariable(int index, XObject val) {
        this._stackFrames[this._currentFrameBottom + index] = val;
    }

    public void setLocalVariable(int index, XObject val, int stackFrame) {
        this._stackFrames[index + stackFrame] = val;
    }

    public XObject getLocalVariable(XPathContext xctxt, int index) throws TransformerException {
        int index2 = index + this._currentFrameBottom;
        XObject val = this._stackFrames[index2];
        if (val == null) {
            throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VARIABLE_ACCESSED_BEFORE_BIND, null), xctxt.getSAXLocator());
        }
        if (val.getType() == 600) {
            XObject xObjectExecute = val.execute(xctxt);
            this._stackFrames[index2] = xObjectExecute;
            return xObjectExecute;
        }
        return val;
    }

    public XObject getLocalVariable(int index, int frame) throws TransformerException {
        XObject val = this._stackFrames[index + frame];
        return val;
    }

    public XObject getLocalVariable(XPathContext xctxt, int index, boolean destructiveOK) throws TransformerException {
        int index2 = index + this._currentFrameBottom;
        XObject val = this._stackFrames[index2];
        if (val == null) {
            throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VARIABLE_ACCESSED_BEFORE_BIND, null), xctxt.getSAXLocator());
        }
        if (val.getType() != 600) {
            return destructiveOK ? val : val.getFresh();
        }
        XObject xObjectExecute = val.execute(xctxt);
        this._stackFrames[index2] = xObjectExecute;
        return xObjectExecute;
    }

    public boolean isLocalSet(int index) throws TransformerException {
        return this._stackFrames[this._currentFrameBottom + index] != null;
    }

    public void clearLocalSlots(int start, int len) {
        System.arraycopy(m_nulls, 0, this._stackFrames, start + this._currentFrameBottom, len);
    }

    public void setGlobalVariable(int index, XObject val) {
        this._stackFrames[index] = val;
    }

    public XObject getGlobalVariable(XPathContext xctxt, int index) throws TransformerException {
        XObject val = this._stackFrames[index];
        if (val.getType() == 600) {
            XObject xObjectExecute = val.execute(xctxt);
            this._stackFrames[index] = xObjectExecute;
            return xObjectExecute;
        }
        return val;
    }

    public XObject getGlobalVariable(XPathContext xctxt, int index, boolean destructiveOK) throws TransformerException {
        XObject val = this._stackFrames[index];
        if (val.getType() != 600) {
            return destructiveOK ? val : val.getFresh();
        }
        XObject xObjectExecute = val.execute(xctxt);
        this._stackFrames[index] = xObjectExecute;
        return xObjectExecute;
    }

    public XObject getVariableOrParam(XPathContext xPathContext, QName qName) throws TransformerException {
        PrefixResolver namespaceContext = xPathContext.getNamespaceContext();
        if (namespaceContext instanceof ElemTemplateElement) {
            ?? previousSiblingElem = namespaceContext;
            boolean z = previousSiblingElem instanceof Stylesheet;
            previousSiblingElem = previousSiblingElem;
            if (!z) {
                while (!(previousSiblingElem.getParentNode() instanceof Stylesheet)) {
                    ?? r2 = previousSiblingElem;
                    while (true) {
                        previousSiblingElem = previousSiblingElem.getPreviousSiblingElem();
                        if (previousSiblingElem != 0) {
                            if ((previousSiblingElem instanceof ElemVariable) && previousSiblingElem.getName().equals(qName)) {
                                return getLocalVariable(xPathContext, previousSiblingElem.getIndex());
                            }
                        }
                    }
                }
            }
            ElemVariable variableOrParamComposed = previousSiblingElem.getStylesheetRoot().getVariableOrParamComposed(qName);
            if (variableOrParamComposed != null) {
                return getGlobalVariable(xPathContext, variableOrParamComposed.getIndex());
            }
        }
        throw new TransformerException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_VAR_NOT_RESOLVABLE, new Object[]{qName.toString()}));
    }
}
