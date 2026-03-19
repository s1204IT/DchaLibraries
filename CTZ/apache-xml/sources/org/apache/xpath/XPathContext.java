package org.apache.xpath;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xml.dtm.ref.sax2dtm.SAX2RTFDTM;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.IntStack;
import org.apache.xml.utils.NodeVector;
import org.apache.xml.utils.ObjectStack;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.axes.OneStepIteratorForward;
import org.apache.xpath.axes.SubContextList;
import org.apache.xpath.objects.DTMXRTreeFrag;
import org.apache.xpath.objects.XMLStringFactoryImpl;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.XMLReader;

public class XPathContext extends DTMManager {
    public static final int RECURSIONLIMIT = 4096;
    XPathExpressionContext expressionContext;
    private HashMap m_DTMXRTreeFrags;
    private Stack m_axesIteratorStack;
    private Stack m_contextNodeLists;
    private IntStack m_currentExpressionNodes;
    private IntStack m_currentNodes;
    private ErrorListener m_defaultErrorListener;
    protected DTMManager m_dtmManager;
    private ErrorListener m_errorListener;
    private SAX2RTFDTM m_global_rtfdtm;
    private boolean m_isSecureProcessing;
    private NodeVector m_iteratorRoots;
    IntStack m_last_pushed_rtfdtm;
    private Object m_owner;
    private Method m_ownerGetErrorListener;
    private IntStack m_predicatePos;
    private NodeVector m_predicateRoots;
    private ObjectStack m_prefixResolvers;
    public XMLReader m_primaryReader;
    private Vector m_rtfdtm_stack;
    ObjectStack m_saxLocations;
    private SourceTreeManager m_sourceTreeManager;
    private URIResolver m_uriResolver;
    private VariableStack m_variableStacks;
    private int m_which_rtfdtm;

    public DTMManager getDTMManager() {
        return this.m_dtmManager;
    }

    public void setSecureProcessing(boolean z) {
        this.m_isSecureProcessing = z;
    }

    public boolean isSecureProcessing() {
        return this.m_isSecureProcessing;
    }

    @Override
    public DTM getDTM(Source source, boolean z, DTMWSFilter dTMWSFilter, boolean z2, boolean z3) {
        return this.m_dtmManager.getDTM(source, z, dTMWSFilter, z2, z3);
    }

    @Override
    public DTM getDTM(int i) {
        return this.m_dtmManager.getDTM(i);
    }

    @Override
    public int getDTMHandleFromNode(Node node) {
        return this.m_dtmManager.getDTMHandleFromNode(node);
    }

    @Override
    public int getDTMIdentity(DTM dtm) {
        return this.m_dtmManager.getDTMIdentity(dtm);
    }

    @Override
    public DTM createDocumentFragment() {
        return this.m_dtmManager.createDocumentFragment();
    }

    @Override
    public boolean release(DTM dtm, boolean z) {
        if (this.m_rtfdtm_stack != null && this.m_rtfdtm_stack.contains(dtm)) {
            return false;
        }
        return this.m_dtmManager.release(dtm, z);
    }

    @Override
    public DTMIterator createDTMIterator(Object obj, int i) {
        return this.m_dtmManager.createDTMIterator(obj, i);
    }

    @Override
    public DTMIterator createDTMIterator(String str, PrefixResolver prefixResolver) {
        return this.m_dtmManager.createDTMIterator(str, prefixResolver);
    }

    @Override
    public DTMIterator createDTMIterator(int i, DTMFilter dTMFilter, boolean z) {
        return this.m_dtmManager.createDTMIterator(i, dTMFilter, z);
    }

    @Override
    public DTMIterator createDTMIterator(int i) {
        OneStepIteratorForward oneStepIteratorForward = new OneStepIteratorForward(13);
        oneStepIteratorForward.setRoot(i, this);
        return oneStepIteratorForward;
    }

    public XPathContext() {
        this(true);
    }

    public XPathContext(boolean z) {
        this.m_last_pushed_rtfdtm = new IntStack();
        this.m_rtfdtm_stack = null;
        this.m_which_rtfdtm = -1;
        this.m_global_rtfdtm = null;
        this.m_DTMXRTreeFrags = null;
        this.m_isSecureProcessing = false;
        this.m_dtmManager = DTMManager.newInstance(XMLStringFactoryImpl.getFactory());
        this.m_saxLocations = new ObjectStack(4096);
        this.m_sourceTreeManager = new SourceTreeManager();
        this.m_contextNodeLists = new Stack();
        this.m_currentNodes = new IntStack(4096);
        this.m_iteratorRoots = new NodeVector();
        this.m_predicateRoots = new NodeVector();
        this.m_currentExpressionNodes = new IntStack(4096);
        this.m_predicatePos = new IntStack();
        this.m_prefixResolvers = new ObjectStack(4096);
        this.m_axesIteratorStack = new Stack();
        this.expressionContext = new XPathExpressionContext();
        this.m_prefixResolvers.push(null);
        this.m_currentNodes.push(-1);
        this.m_currentExpressionNodes.push(-1);
        this.m_saxLocations.push(null);
        this.m_variableStacks = z ? new VariableStack() : new VariableStack(1);
    }

    public XPathContext(Object obj) {
        this(obj, true);
    }

    public XPathContext(Object obj, boolean z) {
        this(z);
        this.m_owner = obj;
        try {
            this.m_ownerGetErrorListener = this.m_owner.getClass().getMethod("getErrorListener", new Class[0]);
        } catch (NoSuchMethodException e) {
        }
    }

    public void reset() {
        releaseDTMXRTreeFrags();
        if (this.m_rtfdtm_stack != null) {
            Enumeration enumerationElements = this.m_rtfdtm_stack.elements();
            while (enumerationElements.hasMoreElements()) {
                this.m_dtmManager.release((DTM) enumerationElements.nextElement(), true);
            }
        }
        this.m_rtfdtm_stack = null;
        this.m_which_rtfdtm = -1;
        if (this.m_global_rtfdtm != null) {
            this.m_dtmManager.release(this.m_global_rtfdtm, true);
        }
        this.m_global_rtfdtm = null;
        this.m_dtmManager = DTMManager.newInstance(XMLStringFactoryImpl.getFactory());
        this.m_saxLocations.removeAllElements();
        this.m_axesIteratorStack.removeAllElements();
        this.m_contextNodeLists.removeAllElements();
        this.m_currentExpressionNodes.removeAllElements();
        this.m_currentNodes.removeAllElements();
        this.m_iteratorRoots.RemoveAllNoClear();
        this.m_predicatePos.removeAllElements();
        this.m_predicateRoots.RemoveAllNoClear();
        this.m_prefixResolvers.removeAllElements();
        this.m_prefixResolvers.push(null);
        this.m_currentNodes.push(-1);
        this.m_currentExpressionNodes.push(-1);
        this.m_saxLocations.push(null);
    }

    public void setSAXLocator(SourceLocator sourceLocator) {
        this.m_saxLocations.setTop(sourceLocator);
    }

    public void pushSAXLocator(SourceLocator sourceLocator) {
        this.m_saxLocations.push(sourceLocator);
    }

    public void pushSAXLocatorNull() {
        this.m_saxLocations.push(null);
    }

    public void popSAXLocator() {
        this.m_saxLocations.pop();
    }

    public SourceLocator getSAXLocator() {
        return (SourceLocator) this.m_saxLocations.peek();
    }

    public Object getOwnerObject() {
        return this.m_owner;
    }

    public final VariableStack getVarStack() {
        return this.m_variableStacks;
    }

    public final void setVarStack(VariableStack variableStack) {
        this.m_variableStacks = variableStack;
    }

    public final SourceTreeManager getSourceTreeManager() {
        return this.m_sourceTreeManager;
    }

    public void setSourceTreeManager(SourceTreeManager sourceTreeManager) {
        this.m_sourceTreeManager = sourceTreeManager;
    }

    public final ErrorListener getErrorListener() {
        if (this.m_errorListener != null) {
            return this.m_errorListener;
        }
        ErrorListener errorListener = null;
        try {
            if (this.m_ownerGetErrorListener != null) {
                errorListener = (ErrorListener) this.m_ownerGetErrorListener.invoke(this.m_owner, new Object[0]);
            }
        } catch (Exception e) {
        }
        if (errorListener == null) {
            if (this.m_defaultErrorListener == null) {
                this.m_defaultErrorListener = new DefaultErrorHandler();
            }
            return this.m_defaultErrorListener;
        }
        return errorListener;
    }

    public void setErrorListener(ErrorListener errorListener) throws IllegalArgumentException {
        if (errorListener == null) {
            throw new IllegalArgumentException(XSLMessages.createXPATHMessage("ER_NULL_ERROR_HANDLER", null));
        }
        this.m_errorListener = errorListener;
    }

    public final URIResolver getURIResolver() {
        return this.m_uriResolver;
    }

    public void setURIResolver(URIResolver uRIResolver) {
        this.m_uriResolver = uRIResolver;
    }

    public final XMLReader getPrimaryReader() {
        return this.m_primaryReader;
    }

    public void setPrimaryReader(XMLReader xMLReader) {
        this.m_primaryReader = xMLReader;
    }

    private void assertion(boolean z, String str) throws TransformerException {
        ErrorListener errorListener;
        if (!z && (errorListener = getErrorListener()) != null) {
            errorListener.fatalError(new TransformerException(XSLMessages.createMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{str}), (SAXSourceLocator) getSAXLocator()));
        }
    }

    public Stack getContextNodeListsStack() {
        return this.m_contextNodeLists;
    }

    public void setContextNodeListsStack(Stack stack) {
        this.m_contextNodeLists = stack;
    }

    public final DTMIterator getContextNodeList() {
        if (this.m_contextNodeLists.size() > 0) {
            return (DTMIterator) this.m_contextNodeLists.peek();
        }
        return null;
    }

    public final void pushContextNodeList(DTMIterator dTMIterator) {
        this.m_contextNodeLists.push(dTMIterator);
    }

    public final void popContextNodeList() {
        if (this.m_contextNodeLists.isEmpty()) {
            System.err.println("Warning: popContextNodeList when stack is empty!");
        } else {
            this.m_contextNodeLists.pop();
        }
    }

    public IntStack getCurrentNodeStack() {
        return this.m_currentNodes;
    }

    public void setCurrentNodeStack(IntStack intStack) {
        this.m_currentNodes = intStack;
    }

    public final int getCurrentNode() {
        return this.m_currentNodes.peek();
    }

    public final void pushCurrentNodeAndExpression(int i, int i2) {
        this.m_currentNodes.push(i);
        this.m_currentExpressionNodes.push(i);
    }

    public final void popCurrentNodeAndExpression() {
        this.m_currentNodes.quickPop(1);
        this.m_currentExpressionNodes.quickPop(1);
    }

    public final void pushExpressionState(int i, int i2, PrefixResolver prefixResolver) {
        this.m_currentNodes.push(i);
        this.m_currentExpressionNodes.push(i);
        this.m_prefixResolvers.push(prefixResolver);
    }

    public final void popExpressionState() {
        this.m_currentNodes.quickPop(1);
        this.m_currentExpressionNodes.quickPop(1);
        this.m_prefixResolvers.pop();
    }

    public final void pushCurrentNode(int i) {
        this.m_currentNodes.push(i);
    }

    public final void popCurrentNode() {
        this.m_currentNodes.quickPop(1);
    }

    public final void pushPredicateRoot(int i) {
        this.m_predicateRoots.push(i);
    }

    public final void popPredicateRoot() {
        this.m_predicateRoots.popQuick();
    }

    public final int getPredicateRoot() {
        return this.m_predicateRoots.peepOrNull();
    }

    public final void pushIteratorRoot(int i) {
        this.m_iteratorRoots.push(i);
    }

    public final void popIteratorRoot() {
        this.m_iteratorRoots.popQuick();
    }

    public final int getIteratorRoot() {
        return this.m_iteratorRoots.peepOrNull();
    }

    public IntStack getCurrentExpressionNodeStack() {
        return this.m_currentExpressionNodes;
    }

    public void setCurrentExpressionNodeStack(IntStack intStack) {
        this.m_currentExpressionNodes = intStack;
    }

    public final int getPredicatePos() {
        return this.m_predicatePos.peek();
    }

    public final void pushPredicatePos(int i) {
        this.m_predicatePos.push(i);
    }

    public final void popPredicatePos() {
        this.m_predicatePos.pop();
    }

    public final int getCurrentExpressionNode() {
        return this.m_currentExpressionNodes.peek();
    }

    public final void pushCurrentExpressionNode(int i) {
        this.m_currentExpressionNodes.push(i);
    }

    public final void popCurrentExpressionNode() {
        this.m_currentExpressionNodes.quickPop(1);
    }

    public final PrefixResolver getNamespaceContext() {
        return (PrefixResolver) this.m_prefixResolvers.peek();
    }

    public final void setNamespaceContext(PrefixResolver prefixResolver) {
        this.m_prefixResolvers.setTop(prefixResolver);
    }

    public final void pushNamespaceContext(PrefixResolver prefixResolver) {
        this.m_prefixResolvers.push(prefixResolver);
    }

    public final void pushNamespaceContextNull() {
        this.m_prefixResolvers.push(null);
    }

    public final void popNamespaceContext() {
        this.m_prefixResolvers.pop();
    }

    public Stack getAxesIteratorStackStacks() {
        return this.m_axesIteratorStack;
    }

    public void setAxesIteratorStackStacks(Stack stack) {
        this.m_axesIteratorStack = stack;
    }

    public final void pushSubContextList(SubContextList subContextList) {
        this.m_axesIteratorStack.push(subContextList);
    }

    public final void popSubContextList() {
        this.m_axesIteratorStack.pop();
    }

    public SubContextList getSubContextList() {
        if (this.m_axesIteratorStack.isEmpty()) {
            return null;
        }
        return (SubContextList) this.m_axesIteratorStack.peek();
    }

    public SubContextList getCurrentNodeList() {
        if (this.m_axesIteratorStack.isEmpty()) {
            return null;
        }
        return (SubContextList) this.m_axesIteratorStack.elementAt(0);
    }

    public final int getContextNode() {
        return getCurrentNode();
    }

    public final DTMIterator getContextNodes() {
        try {
            DTMIterator contextNodeList = getContextNodeList();
            if (contextNodeList == null) {
                return null;
            }
            return contextNodeList.cloneWithReset();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public ExpressionContext getExpressionContext() {
        return this.expressionContext;
    }

    public class XPathExpressionContext implements ExpressionContext {
        public XPathExpressionContext() {
        }

        @Override
        public XPathContext getXPathContext() {
            return XPathContext.this;
        }

        public DTMManager getDTMManager() {
            return XPathContext.this.m_dtmManager;
        }

        @Override
        public Node getContextNode() {
            int currentNode = XPathContext.this.getCurrentNode();
            return XPathContext.this.getDTM(currentNode).getNode(currentNode);
        }

        @Override
        public NodeIterator getContextNodes() {
            return new DTMNodeIterator(XPathContext.this.getContextNodeList());
        }

        @Override
        public ErrorListener getErrorListener() {
            return XPathContext.this.getErrorListener();
        }

        @Override
        public double toNumber(Node node) {
            int dTMHandleFromNode = XPathContext.this.getDTMHandleFromNode(node);
            return ((XString) XPathContext.this.getDTM(dTMHandleFromNode).getStringValue(dTMHandleFromNode)).num();
        }

        @Override
        public String toString(Node node) {
            int dTMHandleFromNode = XPathContext.this.getDTMHandleFromNode(node);
            return XPathContext.this.getDTM(dTMHandleFromNode).getStringValue(dTMHandleFromNode).toString();
        }

        @Override
        public final XObject getVariableOrParam(QName qName) throws TransformerException {
            return XPathContext.this.m_variableStacks.getVariableOrParam(XPathContext.this, qName);
        }
    }

    public DTM getGlobalRTFDTM() {
        if (this.m_global_rtfdtm == null || this.m_global_rtfdtm.isTreeIncomplete()) {
            this.m_global_rtfdtm = (SAX2RTFDTM) this.m_dtmManager.getDTM(null, true, null, false, false);
        }
        return this.m_global_rtfdtm;
    }

    public DTM getRTFDTM() {
        if (this.m_rtfdtm_stack == null) {
            this.m_rtfdtm_stack = new Vector();
            SAX2RTFDTM sax2rtfdtm = (SAX2RTFDTM) this.m_dtmManager.getDTM(null, true, null, false, false);
            this.m_rtfdtm_stack.addElement(sax2rtfdtm);
            this.m_which_rtfdtm++;
            return sax2rtfdtm;
        }
        if (this.m_which_rtfdtm < 0) {
            Vector vector = this.m_rtfdtm_stack;
            int i = this.m_which_rtfdtm + 1;
            this.m_which_rtfdtm = i;
            return (SAX2RTFDTM) vector.elementAt(i);
        }
        SAX2RTFDTM sax2rtfdtm2 = (SAX2RTFDTM) this.m_rtfdtm_stack.elementAt(this.m_which_rtfdtm);
        if (sax2rtfdtm2.isTreeIncomplete()) {
            int i2 = this.m_which_rtfdtm + 1;
            this.m_which_rtfdtm = i2;
            if (i2 < this.m_rtfdtm_stack.size()) {
                return (SAX2RTFDTM) this.m_rtfdtm_stack.elementAt(this.m_which_rtfdtm);
            }
            SAX2RTFDTM sax2rtfdtm3 = (SAX2RTFDTM) this.m_dtmManager.getDTM(null, true, null, false, false);
            this.m_rtfdtm_stack.addElement(sax2rtfdtm3);
            return sax2rtfdtm3;
        }
        return sax2rtfdtm2;
    }

    public void pushRTFContext() {
        this.m_last_pushed_rtfdtm.push(this.m_which_rtfdtm);
        if (this.m_rtfdtm_stack != null) {
            ((SAX2RTFDTM) getRTFDTM()).pushRewindMark();
        }
    }

    public void popRTFContext() {
        int iPop = this.m_last_pushed_rtfdtm.pop();
        if (this.m_rtfdtm_stack == null) {
            return;
        }
        if (this.m_which_rtfdtm == iPop) {
            if (iPop >= 0) {
                ((SAX2RTFDTM) this.m_rtfdtm_stack.elementAt(iPop)).popRewindMark();
            }
        } else {
            while (this.m_which_rtfdtm != iPop) {
                ((SAX2RTFDTM) this.m_rtfdtm_stack.elementAt(this.m_which_rtfdtm)).popRewindMark();
                this.m_which_rtfdtm--;
            }
        }
    }

    public DTMXRTreeFrag getDTMXRTreeFrag(int i) {
        if (this.m_DTMXRTreeFrags == null) {
            this.m_DTMXRTreeFrags = new HashMap();
        }
        if (this.m_DTMXRTreeFrags.containsKey(new Integer(i))) {
            return (DTMXRTreeFrag) this.m_DTMXRTreeFrags.get(new Integer(i));
        }
        DTMXRTreeFrag dTMXRTreeFrag = new DTMXRTreeFrag(i, this);
        this.m_DTMXRTreeFrags.put(new Integer(i), dTMXRTreeFrag);
        return dTMXRTreeFrag;
    }

    private final void releaseDTMXRTreeFrags() {
        if (this.m_DTMXRTreeFrags == null) {
            return;
        }
        Iterator it = this.m_DTMXRTreeFrags.values().iterator();
        while (it.hasNext()) {
            ((DTMXRTreeFrag) it.next()).destruct();
            it.remove();
        }
        this.m_DTMXRTreeFrags = null;
    }
}
