package org.apache.xpath.axes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class LocPathIterator extends PredicatedNodeTest implements Cloneable, DTMIterator, Serializable, PathComponent {
    static final long serialVersionUID = -4602476357268405754L;
    protected boolean m_allowDetach;
    protected transient DTM m_cdtm;
    protected transient IteratorPool m_clones;
    protected transient int m_context;
    protected transient int m_currentContextNode;
    protected transient XPathContext m_execContext;
    private boolean m_isTopLevel;
    public transient int m_lastFetched;
    protected transient int m_length;
    protected transient int m_pos;
    private PrefixResolver m_prefixResolver;
    transient int m_stackFrame;

    public abstract int nextNode();

    protected LocPathIterator() {
        this.m_allowDetach = true;
        this.m_clones = new IteratorPool(this);
        this.m_stackFrame = -1;
        this.m_isTopLevel = false;
        this.m_lastFetched = -1;
        this.m_context = -1;
        this.m_currentContextNode = -1;
        this.m_pos = 0;
        this.m_length = -1;
    }

    protected LocPathIterator(PrefixResolver prefixResolver) {
        this.m_allowDetach = true;
        this.m_clones = new IteratorPool(this);
        this.m_stackFrame = -1;
        this.m_isTopLevel = false;
        this.m_lastFetched = -1;
        this.m_context = -1;
        this.m_currentContextNode = -1;
        this.m_pos = 0;
        this.m_length = -1;
        setLocPathIterator(this);
        this.m_prefixResolver = prefixResolver;
    }

    protected LocPathIterator(Compiler compiler, int i, int i2) throws TransformerException {
        this(compiler, i, i2, true);
    }

    protected LocPathIterator(Compiler compiler, int i, int i2, boolean z) throws TransformerException {
        this.m_allowDetach = true;
        this.m_clones = new IteratorPool(this);
        this.m_stackFrame = -1;
        this.m_isTopLevel = false;
        this.m_lastFetched = -1;
        this.m_context = -1;
        this.m_currentContextNode = -1;
        this.m_pos = 0;
        this.m_length = -1;
        setLocPathIterator(this);
    }

    public int getAnalysisBits() {
        return WalkerFactory.getAnalysisBitFromAxes(getAxis());
    }

    private void readObject(ObjectInputStream objectInputStream) throws TransformerException, IOException {
        try {
            objectInputStream.defaultReadObject();
            this.m_clones = new IteratorPool(this);
        } catch (ClassNotFoundException e) {
            throw new TransformerException(e);
        }
    }

    public void setEnvironment(Object obj) {
    }

    @Override
    public DTM getDTM(int i) {
        return this.m_execContext.getDTM(i);
    }

    @Override
    public DTMManager getDTMManager() {
        return this.m_execContext.getDTMManager();
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        XNodeSet xNodeSet = new XNodeSet((LocPathIterator) this.m_clones.getInstance());
        xNodeSet.setRoot(xPathContext.getCurrentNode(), xPathContext);
        return xNodeSet;
    }

    @Override
    public void executeCharsToContentHandler(XPathContext xPathContext, ContentHandler contentHandler) throws TransformerException, SAXException {
        LocPathIterator locPathIterator = (LocPathIterator) this.m_clones.getInstance();
        locPathIterator.setRoot(xPathContext.getCurrentNode(), xPathContext);
        int iNextNode = locPathIterator.nextNode();
        DTM dtm = locPathIterator.getDTM(iNextNode);
        locPathIterator.detach();
        if (iNextNode != -1) {
            dtm.dispatchCharactersEvents(iNextNode, contentHandler, false);
        }
    }

    @Override
    public DTMIterator asIterator(XPathContext xPathContext, int i) throws TransformerException {
        XNodeSet xNodeSet = new XNodeSet((LocPathIterator) this.m_clones.getInstance());
        xNodeSet.setRoot(i, xPathContext);
        return xNodeSet;
    }

    @Override
    public boolean isNodesetExpr() {
        return true;
    }

    @Override
    public int asNode(XPathContext xPathContext) throws TransformerException {
        DTMIterator iteratorPool = this.m_clones.getInstance();
        iteratorPool.setRoot(xPathContext.getCurrentNode(), xPathContext);
        int iNextNode = iteratorPool.nextNode();
        iteratorPool.detach();
        return iNextNode;
    }

    @Override
    public boolean bool(XPathContext xPathContext) throws TransformerException {
        return asNode(xPathContext) != -1;
    }

    public void setIsTopLevel(boolean z) {
        this.m_isTopLevel = z;
    }

    public boolean getIsTopLevel() {
        return this.m_isTopLevel;
    }

    public void setRoot(int i, Object obj) {
        this.m_context = i;
        XPathContext xPathContext = (XPathContext) obj;
        this.m_execContext = xPathContext;
        this.m_cdtm = xPathContext.getDTM(i);
        this.m_currentContextNode = i;
        if (this.m_prefixResolver == null) {
            this.m_prefixResolver = xPathContext.getNamespaceContext();
        }
        this.m_lastFetched = -1;
        this.m_foundLast = false;
        this.m_pos = 0;
        this.m_length = -1;
        if (this.m_isTopLevel) {
            this.m_stackFrame = xPathContext.getVarStack().getStackFrame();
        }
    }

    protected void setNextPosition(int i) {
        assertion(false, "setNextPosition not supported in this iterator!");
    }

    @Override
    public final int getCurrentPos() {
        return this.m_pos;
    }

    @Override
    public void setShouldCacheNodes(boolean z) {
        assertion(false, "setShouldCacheNodes not supported by this iterater!");
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void setCurrentPos(int i) {
        assertion(false, "setCurrentPos not supported by this iterator!");
    }

    public void incrementCurrentPos() {
        this.m_pos++;
    }

    public int size() {
        assertion(false, "size() not supported by this iterator!");
        return 0;
    }

    @Override
    public int item(int i) {
        assertion(false, "item(int index) not supported by this iterator!");
        return 0;
    }

    @Override
    public void setItem(int i, int i2) {
        assertion(false, "setItem not supported by this iterator!");
    }

    @Override
    public int getLength() {
        boolean z;
        if (this != this.m_execContext.getSubContextList()) {
            z = false;
        } else {
            z = true;
        }
        int predicateCount = getPredicateCount();
        if (-1 != this.m_length && z && this.m_predicateIndex < 1) {
            return this.m_length;
        }
        if (this.m_foundLast) {
            return this.m_pos;
        }
        int proximityPosition = this.m_predicateIndex >= 0 ? getProximityPosition() : this.m_pos;
        try {
            LocPathIterator locPathIterator = (LocPathIterator) clone();
            if (predicateCount > 0 && z) {
                locPathIterator.m_predCount = this.m_predicateIndex;
            }
            while (-1 != locPathIterator.nextNode()) {
                proximityPosition++;
            }
            if (z && this.m_predicateIndex < 1) {
                this.m_length = proximityPosition;
            }
            return proximityPosition;
        } catch (CloneNotSupportedException e) {
            return -1;
        }
    }

    @Override
    public boolean isFresh() {
        return this.m_pos == 0;
    }

    @Override
    public int previousNode() {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESETDTM_CANNOT_ITERATE, null));
    }

    @Override
    public int getWhatToShow() {
        return -17;
    }

    public DTMFilter getFilter() {
        return null;
    }

    @Override
    public int getRoot() {
        return this.m_context;
    }

    @Override
    public boolean getExpandEntityReferences() {
        return true;
    }

    @Override
    public void allowDetachToRelease(boolean z) {
        this.m_allowDetach = z;
    }

    public void detach() {
        if (this.m_allowDetach) {
            this.m_execContext = null;
            this.m_cdtm = null;
            this.m_length = -1;
            this.m_pos = 0;
            this.m_lastFetched = -1;
            this.m_context = -1;
            this.m_currentContextNode = -1;
            this.m_clones.freeInstance(this);
        }
    }

    @Override
    public void reset() {
        assertion(false, "This iterator can not reset!");
    }

    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        LocPathIterator locPathIterator = (LocPathIterator) this.m_clones.getInstanceOrThrow();
        locPathIterator.m_execContext = this.m_execContext;
        locPathIterator.m_cdtm = this.m_cdtm;
        locPathIterator.m_context = this.m_context;
        locPathIterator.m_currentContextNode = this.m_currentContextNode;
        locPathIterator.m_stackFrame = this.m_stackFrame;
        return locPathIterator;
    }

    protected int returnNextNode(int i) {
        if (-1 != i) {
            this.m_pos++;
        }
        this.m_lastFetched = i;
        if (-1 == i) {
            this.m_foundLast = true;
        }
        return i;
    }

    @Override
    public int getCurrentNode() {
        return this.m_lastFetched;
    }

    @Override
    public void runTo(int i) {
        if (this.m_foundLast) {
            return;
        }
        if (i >= 0 && i <= getCurrentPos()) {
            return;
        }
        if (-1 == i) {
            while (-1 != nextNode()) {
            }
        } else {
            while (-1 != nextNode() && getCurrentPos() < i) {
            }
        }
    }

    public final boolean getFoundLast() {
        return this.m_foundLast;
    }

    public final XPathContext getXPathContext() {
        return this.m_execContext;
    }

    public final int getContext() {
        return this.m_context;
    }

    public final int getCurrentContextNode() {
        return this.m_currentContextNode;
    }

    public final void setCurrentContextNode(int i) {
        this.m_currentContextNode = i;
    }

    public final PrefixResolver getPrefixResolver() {
        if (this.m_prefixResolver == null) {
            this.m_prefixResolver = (PrefixResolver) getExpressionOwner();
        }
        return this.m_prefixResolver;
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitLocationPath(expressionOwner, this)) {
            xPathVisitor.visitStep(expressionOwner, this);
            callPredicateVisitors(xPathVisitor);
        }
    }

    public boolean isDocOrdered() {
        return true;
    }

    public int getAxis() {
        return -1;
    }

    @Override
    public int getLastPos(XPathContext xPathContext) {
        return getLength();
    }
}
