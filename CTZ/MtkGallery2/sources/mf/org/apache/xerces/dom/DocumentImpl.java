package mf.org.apache.xerces.dom;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import mf.org.apache.xerces.dom.events.EventImpl;
import mf.org.apache.xerces.dom.events.MouseEventImpl;
import mf.org.apache.xerces.dom.events.MutationEventImpl;
import mf.org.apache.xerces.dom.events.UIEventImpl;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.DOMImplementation;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.events.Event;
import mf.org.w3c.dom.events.EventException;
import mf.org.w3c.dom.events.EventListener;
import mf.org.w3c.dom.events.MutationEvent;
import mf.org.w3c.dom.ranges.Range;
import mf.org.w3c.dom.traversal.NodeFilter;
import mf.org.w3c.dom.traversal.NodeIterator;
import mf.org.w3c.dom.traversal.TreeWalker;

public class DocumentImpl extends CoreDocumentImpl {
    static final long serialVersionUID = 515687835542616694L;
    protected Hashtable eventListeners;
    protected transient ReferenceQueue iteratorReferenceQueue;
    protected transient List iterators;
    protected boolean mutationEvents;
    protected transient ReferenceQueue rangeReferenceQueue;
    protected transient List ranges;
    EnclosingAttr savedEnclosingAttr;

    public DocumentImpl() {
        this.mutationEvents = false;
    }

    public DocumentImpl(boolean grammarAccess) {
        super(grammarAccess);
        this.mutationEvents = false;
    }

    public DocumentImpl(DocumentType doctype) {
        super(doctype);
        this.mutationEvents = false;
    }

    public DocumentImpl(DocumentType doctype, boolean grammarAccess) {
        super(doctype, grammarAccess);
        this.mutationEvents = false;
    }

    @Override
    public Node cloneNode(boolean deep) {
        DocumentImpl newdoc = new DocumentImpl();
        callUserDataHandlers(this, newdoc, (short) 1);
        cloneNode(newdoc, deep);
        newdoc.mutationEvents = this.mutationEvents;
        return newdoc;
    }

    @Override
    public DOMImplementation getImplementation() {
        return DOMImplementationImpl.getDOMImplementation();
    }

    public NodeIterator createNodeIterator(Node root, short whatToShow, NodeFilter filter) {
        return createNodeIterator(root, whatToShow, filter, true);
    }

    public NodeIterator createNodeIterator(Node root, int whatToShow, NodeFilter filter, boolean entityReferenceExpansion) {
        if (root == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException((short) 9, msg);
        }
        NodeIterator iterator = new NodeIteratorImpl(this, root, whatToShow, filter, entityReferenceExpansion);
        if (this.iterators == null) {
            this.iterators = new LinkedList();
            this.iteratorReferenceQueue = new ReferenceQueue();
        }
        removeStaleIteratorReferences();
        this.iterators.add(new WeakReference(iterator, this.iteratorReferenceQueue));
        return iterator;
    }

    public TreeWalker createTreeWalker(Node root, short whatToShow, NodeFilter filter) {
        return createTreeWalker(root, whatToShow, filter, true);
    }

    public TreeWalker createTreeWalker(Node root, int whatToShow, NodeFilter filter, boolean entityReferenceExpansion) {
        if (root == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException((short) 9, msg);
        }
        return new TreeWalkerImpl(root, whatToShow, filter, entityReferenceExpansion);
    }

    void removeNodeIterator(NodeIterator nodeIterator) {
        if (nodeIterator == null || this.iterators == null) {
            return;
        }
        removeStaleIteratorReferences();
        Iterator i = this.iterators.iterator();
        while (i.hasNext()) {
            Object iterator = ((Reference) i.next()).get();
            if (iterator == nodeIterator) {
                i.remove();
                return;
            } else if (iterator == null) {
                i.remove();
            }
        }
    }

    private void removeStaleIteratorReferences() {
        removeStaleReferences(this.iteratorReferenceQueue, this.iterators);
    }

    private void removeStaleReferences(ReferenceQueue queue, List list) {
        Reference ref = queue.poll();
        int count = 0;
        while (ref != null) {
            count++;
            ref = queue.poll();
        }
        if (count > 0) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                Object o = ((Reference) i.next()).get();
                if (o == null) {
                    i.remove();
                    count--;
                    if (count <= 0) {
                        return;
                    }
                }
            }
        }
    }

    public Range createRange() {
        if (this.ranges == null) {
            this.ranges = new LinkedList();
            this.rangeReferenceQueue = new ReferenceQueue();
        }
        Range range = new RangeImpl(this);
        removeStaleRangeReferences();
        this.ranges.add(new WeakReference(range, this.rangeReferenceQueue));
        return range;
    }

    void removeRange(Range range) {
        if (range == null || this.ranges == null) {
            return;
        }
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            Object otherRange = ((Reference) i.next()).get();
            if (otherRange == range) {
                i.remove();
                return;
            } else if (otherRange == null) {
                i.remove();
            }
        }
    }

    @Override
    void replacedText(CharacterDataImpl node) {
        if (this.ranges != null) {
            notifyRangesReplacedText(node);
        }
    }

    private void notifyRangesReplacedText(CharacterDataImpl node) {
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            RangeImpl range = (RangeImpl) ((Reference) i.next()).get();
            if (range != null) {
                range.receiveReplacedText(node);
            } else {
                i.remove();
            }
        }
    }

    @Override
    void deletedText(CharacterDataImpl node, int offset, int count) {
        if (this.ranges != null) {
            notifyRangesDeletedText(node, offset, count);
        }
    }

    private void notifyRangesDeletedText(CharacterDataImpl node, int offset, int count) {
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            RangeImpl range = (RangeImpl) ((Reference) i.next()).get();
            if (range != null) {
                range.receiveDeletedText(node, offset, count);
            } else {
                i.remove();
            }
        }
    }

    @Override
    void insertedText(CharacterDataImpl node, int offset, int count) {
        if (this.ranges != null) {
            notifyRangesInsertedText(node, offset, count);
        }
    }

    private void notifyRangesInsertedText(CharacterDataImpl node, int offset, int count) {
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            RangeImpl range = (RangeImpl) ((Reference) i.next()).get();
            if (range != null) {
                range.receiveInsertedText(node, offset, count);
            } else {
                i.remove();
            }
        }
    }

    void splitData(Node node, Node newNode, int offset) {
        if (this.ranges != null) {
            notifyRangesSplitData(node, newNode, offset);
        }
    }

    private void notifyRangesSplitData(Node node, Node newNode, int offset) {
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            RangeImpl range = (RangeImpl) ((Reference) i.next()).get();
            if (range != null) {
                range.receiveSplitData(node, newNode, offset);
            } else {
                i.remove();
            }
        }
    }

    private void removeStaleRangeReferences() {
        removeStaleReferences(this.rangeReferenceQueue, this.ranges);
    }

    public Event createEvent(String type) throws DOMException {
        if (type.equalsIgnoreCase("Events") || "Event".equals(type)) {
            return new EventImpl();
        }
        if (type.equalsIgnoreCase("MutationEvents") || "MutationEvent".equals(type)) {
            return new MutationEventImpl();
        }
        if (type.equalsIgnoreCase("UIEvents") || "UIEvent".equals(type)) {
            return new UIEventImpl();
        }
        if (type.equalsIgnoreCase("MouseEvents") || "MouseEvent".equals(type)) {
            return new MouseEventImpl();
        }
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    void setMutationEvents(boolean set) {
        this.mutationEvents = set;
    }

    @Override
    boolean getMutationEvents() {
        return this.mutationEvents;
    }

    protected void setEventListeners(NodeImpl n, Vector listeners) {
        if (this.eventListeners == null) {
            this.eventListeners = new Hashtable();
        }
        if (listeners == null) {
            this.eventListeners.remove(n);
            if (this.eventListeners.isEmpty()) {
                this.mutationEvents = false;
                return;
            }
            return;
        }
        this.eventListeners.put(n, listeners);
        this.mutationEvents = true;
    }

    protected Vector getEventListeners(NodeImpl n) {
        if (this.eventListeners == null) {
            return null;
        }
        return (Vector) this.eventListeners.get(n);
    }

    class LEntry implements Serializable {
        private static final long serialVersionUID = -8426757059492421631L;
        EventListener listener;
        String type;
        boolean useCapture;

        LEntry(String type, EventListener listener, boolean useCapture) {
            this.type = type;
            this.listener = listener;
            this.useCapture = useCapture;
        }
    }

    @Override
    protected void addEventListener(NodeImpl node, String type, EventListener listener, boolean useCapture) {
        if (type == null || type.length() == 0 || listener == null) {
            return;
        }
        removeEventListener(node, type, listener, useCapture);
        Vector nodeListeners = getEventListeners(node);
        if (nodeListeners == null) {
            nodeListeners = new Vector();
            setEventListeners(node, nodeListeners);
        }
        nodeListeners.addElement(new LEntry(type, listener, useCapture));
        LCount lc = LCount.lookup(type);
        if (useCapture) {
            lc.captures++;
            lc.total++;
        } else {
            lc.bubbles++;
            lc.total++;
        }
    }

    @Override
    protected void removeEventListener(NodeImpl node, String type, EventListener listener, boolean useCapture) {
        Vector nodeListeners;
        if (type == null || type.length() == 0 || listener == null || (nodeListeners = getEventListeners(node)) == null) {
            return;
        }
        for (int i = nodeListeners.size() - 1; i >= 0; i--) {
            LEntry le = (LEntry) nodeListeners.elementAt(i);
            if (le.useCapture == useCapture && le.listener == listener && le.type.equals(type)) {
                nodeListeners.removeElementAt(i);
                if (nodeListeners.size() == 0) {
                    setEventListeners(node, null);
                }
                LCount lc = LCount.lookup(type);
                if (useCapture) {
                    lc.captures--;
                    lc.total--;
                    return;
                } else {
                    lc.bubbles--;
                    lc.total--;
                    return;
                }
            }
        }
    }

    @Override
    protected void copyEventListeners(NodeImpl src, NodeImpl tgt) {
        Vector nodeListeners = getEventListeners(src);
        if (nodeListeners == null) {
            return;
        }
        setEventListeners(tgt, (Vector) nodeListeners.clone());
    }

    @Override
    protected boolean dispatchEvent(NodeImpl node, Event event) {
        DocumentImpl documentImpl = this;
        if (event == null) {
            return false;
        }
        EventImpl evt = (EventImpl) event;
        if (!evt.initialized || evt.type == null || evt.type.length() == 0) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "UNSPECIFIED_EVENT_TYPE_ERR", null);
            throw new EventException((short) 0, msg);
        }
        LCount lc = LCount.lookup(evt.getType());
        if (lc.total == 0) {
            return evt.preventDefault;
        }
        evt.target = node;
        evt.stopPropagation = false;
        evt.preventDefault = false;
        ArrayList pv = new ArrayList(10);
        Node n = node.getParentNode();
        while (n != null) {
            pv.add(n);
            n = n.getParentNode();
            documentImpl = this;
        }
        if (lc.captures > 0) {
            evt.eventPhase = (short) 1;
            for (int j = pv.size() - 1; j >= 0 && !evt.stopPropagation; j--) {
                NodeImpl nn = (NodeImpl) pv.get(j);
                evt.currentTarget = nn;
                Vector nodeListeners = documentImpl.getEventListeners(nn);
                if (nodeListeners != null) {
                    Vector nl = (Vector) nodeListeners.clone();
                    int nlsize = nl.size();
                    for (int i = 0; i < nlsize; i++) {
                        LEntry le = (LEntry) nl.elementAt(i);
                        if (le.useCapture && le.type.equals(evt.type) && nodeListeners.contains(le)) {
                            try {
                                le.listener.handleEvent(evt);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
        if (lc.bubbles > 0) {
            evt.eventPhase = (short) 2;
            evt.currentTarget = node;
            Vector nodeListeners2 = getEventListeners(node);
            if (!evt.stopPropagation && nodeListeners2 != null) {
                Vector nl2 = (Vector) nodeListeners2.clone();
                int nlsize2 = nl2.size();
                for (int i2 = 0; i2 < nlsize2; i2++) {
                    LEntry le2 = (LEntry) nl2.elementAt(i2);
                    if (!le2.useCapture && le2.type.equals(evt.type) && nodeListeners2.contains(le2)) {
                        try {
                            le2.listener.handleEvent(evt);
                        } catch (Exception e2) {
                        }
                    }
                }
            }
            if (evt.bubbles) {
                evt.eventPhase = (short) 3;
                int pvsize = pv.size();
                int j2 = 0;
                while (j2 < pvsize && !evt.stopPropagation) {
                    NodeImpl nn2 = (NodeImpl) pv.get(j2);
                    evt.currentTarget = nn2;
                    Vector nodeListeners3 = documentImpl.getEventListeners(nn2);
                    if (nodeListeners3 != null) {
                        Vector nl3 = (Vector) nodeListeners3.clone();
                        int nlsize3 = nl3.size();
                        for (int i3 = 0; i3 < nlsize3; i3++) {
                            LEntry le3 = (LEntry) nl3.elementAt(i3);
                            if (!le3.useCapture && le3.type.equals(evt.type) && nodeListeners3.contains(le3)) {
                                try {
                                    le3.listener.handleEvent(evt);
                                } catch (Exception e3) {
                                }
                            }
                        }
                    }
                    j2++;
                    documentImpl = this;
                }
            }
        }
        if (lc.defaults > 0 && evt.cancelable) {
            boolean z = evt.preventDefault;
        }
        return evt.preventDefault;
    }

    protected void dispatchEventToSubtree(Node n, Event e) {
        ((NodeImpl) n).dispatchEvent(e);
        if (n.getNodeType() == 1) {
            NamedNodeMap a = n.getAttributes();
            for (int i = a.getLength() - 1; i >= 0; i--) {
                dispatchingEventToSubtree(a.item(i), e);
            }
        }
        dispatchingEventToSubtree(n.getFirstChild(), e);
    }

    protected void dispatchingEventToSubtree(Node n, Event e) {
        if (n == null) {
            return;
        }
        ((NodeImpl) n).dispatchEvent(e);
        if (n.getNodeType() == 1) {
            NamedNodeMap a = n.getAttributes();
            for (int i = a.getLength() - 1; i >= 0; i--) {
                dispatchingEventToSubtree(a.item(i), e);
            }
        }
        dispatchingEventToSubtree(n.getFirstChild(), e);
        dispatchingEventToSubtree(n.getNextSibling(), e);
    }

    class EnclosingAttr implements Serializable {
        private static final long serialVersionUID = 5208387723391647216L;
        AttrImpl node;
        String oldvalue;

        EnclosingAttr() {
        }
    }

    protected void dispatchAggregateEvents(NodeImpl node, EnclosingAttr ea) {
        if (ea != null) {
            dispatchAggregateEvents(node, ea.node, ea.oldvalue, (short) 1);
        } else {
            dispatchAggregateEvents(node, null, null, (short) 0);
        }
    }

    protected void dispatchAggregateEvents(NodeImpl node, AttrImpl enclosingAttr, String oldvalue, short change) {
        NodeImpl owner;
        if (enclosingAttr != null) {
            LCount lc = LCount.lookup(MutationEventImpl.DOM_ATTR_MODIFIED);
            owner = (NodeImpl) enclosingAttr.getOwnerElement();
            if (lc.total > 0 && owner != null) {
                MutationEventImpl me = new MutationEventImpl();
                me.initMutationEvent(MutationEventImpl.DOM_ATTR_MODIFIED, true, false, enclosingAttr, oldvalue, enclosingAttr.getNodeValue(), enclosingAttr.getNodeName(), change);
                owner.dispatchEvent(me);
            }
        } else {
            owner = null;
        }
        LCount lc2 = LCount.lookup(MutationEventImpl.DOM_SUBTREE_MODIFIED);
        if (lc2.total > 0) {
            MutationEvent me2 = new MutationEventImpl();
            me2.initMutationEvent(MutationEventImpl.DOM_SUBTREE_MODIFIED, true, false, null, null, null, null, (short) 0);
            if (enclosingAttr != null) {
                dispatchEvent(enclosingAttr, me2);
                if (owner != null) {
                    dispatchEvent(owner, me2);
                }
            } else {
                dispatchEvent(node, me2);
            }
        }
    }

    protected void saveEnclosingAttr(NodeImpl node) {
        this.savedEnclosingAttr = null;
        LCount lc = LCount.lookup(MutationEventImpl.DOM_ATTR_MODIFIED);
        if (lc.total > 0) {
            NodeImpl eventAncestor = node;
            while (eventAncestor != null) {
                int type = eventAncestor.getNodeType();
                if (type == 2) {
                    EnclosingAttr retval = new EnclosingAttr();
                    retval.node = (AttrImpl) eventAncestor;
                    retval.oldvalue = retval.node.getNodeValue();
                    this.savedEnclosingAttr = retval;
                    return;
                }
                if (type == 5) {
                    eventAncestor = eventAncestor.parentNode();
                } else if (type == 3) {
                    eventAncestor = eventAncestor.parentNode();
                } else {
                    return;
                }
            }
        }
    }

    @Override
    void modifyingCharacterData(NodeImpl node, boolean replace) {
        if (this.mutationEvents && !replace) {
            saveEnclosingAttr(node);
        }
    }

    @Override
    void modifiedCharacterData(NodeImpl node, String oldvalue, String value, boolean replace) {
        if (this.mutationEvents) {
            mutationEventsModifiedCharacterData(node, oldvalue, value, replace);
        }
    }

    private void mutationEventsModifiedCharacterData(NodeImpl node, String oldvalue, String value, boolean replace) {
        if (!replace) {
            LCount lc = LCount.lookup(MutationEventImpl.DOM_CHARACTER_DATA_MODIFIED);
            if (lc.total > 0) {
                MutationEvent me = new MutationEventImpl();
                me.initMutationEvent(MutationEventImpl.DOM_CHARACTER_DATA_MODIFIED, true, false, null, oldvalue, value, null, (short) 0);
                dispatchEvent(node, me);
            }
            dispatchAggregateEvents(node, this.savedEnclosingAttr);
        }
    }

    @Override
    void replacedCharacterData(NodeImpl node, String oldvalue, String value) {
        modifiedCharacterData(node, oldvalue, value, false);
    }

    @Override
    void insertingNode(NodeImpl node, boolean replace) {
        if (this.mutationEvents && !replace) {
            saveEnclosingAttr(node);
        }
    }

    @Override
    void insertedNode(NodeImpl node, NodeImpl newInternal, boolean replace) {
        if (this.mutationEvents) {
            mutationEventsInsertedNode(node, newInternal, replace);
        }
        if (this.ranges != null) {
            notifyRangesInsertedNode(newInternal);
        }
    }

    private void mutationEventsInsertedNode(NodeImpl node, NodeImpl newInternal, boolean replace) {
        LCount lc = LCount.lookup(MutationEventImpl.DOM_NODE_INSERTED);
        if (lc.total > 0) {
            MutationEventImpl me = new MutationEventImpl();
            me.initMutationEvent(MutationEventImpl.DOM_NODE_INSERTED, true, false, node, null, null, null, (short) 0);
            dispatchEvent(newInternal, me);
        }
        LCount lc2 = LCount.lookup(MutationEventImpl.DOM_NODE_INSERTED_INTO_DOCUMENT);
        if (lc2.total > 0) {
            NodeImpl eventAncestor = node;
            if (this.savedEnclosingAttr != null) {
                eventAncestor = (NodeImpl) this.savedEnclosingAttr.node.getOwnerElement();
            }
            if (eventAncestor != null) {
                NodeImpl p = eventAncestor;
                while (p != null) {
                    eventAncestor = p;
                    if (p.getNodeType() == 2) {
                        p = (NodeImpl) ((AttrImpl) p).getOwnerElement();
                    } else {
                        p = p.parentNode();
                    }
                }
                if (eventAncestor.getNodeType() == 9) {
                    MutationEventImpl me2 = new MutationEventImpl();
                    me2.initMutationEvent(MutationEventImpl.DOM_NODE_INSERTED_INTO_DOCUMENT, false, false, null, null, null, null, (short) 0);
                    dispatchEventToSubtree(newInternal, me2);
                }
            }
        }
        if (replace) {
            return;
        }
        dispatchAggregateEvents(node, this.savedEnclosingAttr);
    }

    private void notifyRangesInsertedNode(NodeImpl newInternal) {
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            RangeImpl range = (RangeImpl) ((Reference) i.next()).get();
            if (range != null) {
                range.insertedNodeFromDOM(newInternal);
            } else {
                i.remove();
            }
        }
    }

    @Override
    void removingNode(NodeImpl node, NodeImpl oldChild, boolean replace) {
        if (this.iterators != null) {
            notifyIteratorsRemovingNode(oldChild);
        }
        if (this.ranges != null) {
            notifyRangesRemovingNode(oldChild);
        }
        if (this.mutationEvents) {
            mutationEventsRemovingNode(node, oldChild, replace);
        }
    }

    private void notifyIteratorsRemovingNode(NodeImpl oldChild) {
        removeStaleIteratorReferences();
        Iterator i = this.iterators.iterator();
        while (i.hasNext()) {
            NodeIteratorImpl iterator = (NodeIteratorImpl) ((Reference) i.next()).get();
            if (iterator != null) {
                iterator.removeNode(oldChild);
            } else {
                i.remove();
            }
        }
    }

    private void notifyRangesRemovingNode(NodeImpl oldChild) {
        removeStaleRangeReferences();
        Iterator i = this.ranges.iterator();
        while (i.hasNext()) {
            RangeImpl range = (RangeImpl) ((Reference) i.next()).get();
            if (range != null) {
                range.removeNode(oldChild);
            } else {
                i.remove();
            }
        }
    }

    private void mutationEventsRemovingNode(NodeImpl nodeImpl, NodeImpl nodeImpl2, boolean z) {
        if (!z) {
            saveEnclosingAttr(nodeImpl);
        }
        if (LCount.lookup(MutationEventImpl.DOM_NODE_REMOVED).total > 0) {
            MutationEventImpl mutationEventImpl = new MutationEventImpl();
            mutationEventImpl.initMutationEvent(MutationEventImpl.DOM_NODE_REMOVED, true, false, nodeImpl, null, null, null, (short) 0);
            dispatchEvent(nodeImpl2, mutationEventImpl);
        }
        if (LCount.lookup(MutationEventImpl.DOM_NODE_REMOVED_FROM_DOCUMENT).total > 0) {
            ?? r1 = this;
            if (this.savedEnclosingAttr != null) {
                r1 = (NodeImpl) this.savedEnclosingAttr.node.getOwnerElement();
            }
            if (r1 != 0) {
                ?? r12 = r1;
                for (NodeImpl nodeImplParentNode = r1.parentNode(); nodeImplParentNode != null; nodeImplParentNode = nodeImplParentNode.parentNode()) {
                    r12 = nodeImplParentNode;
                }
                if (r12.getNodeType() == 9) {
                    MutationEventImpl mutationEventImpl2 = new MutationEventImpl();
                    mutationEventImpl2.initMutationEvent(MutationEventImpl.DOM_NODE_REMOVED_FROM_DOCUMENT, false, false, null, null, null, null, (short) 0);
                    dispatchEventToSubtree(nodeImpl2, mutationEventImpl2);
                }
            }
        }
    }

    @Override
    void removedNode(NodeImpl node, boolean replace) {
        if (this.mutationEvents && !replace) {
            dispatchAggregateEvents(node, this.savedEnclosingAttr);
        }
    }

    @Override
    void replacingNode(NodeImpl node) {
        if (this.mutationEvents) {
            saveEnclosingAttr(node);
        }
    }

    @Override
    void replacingData(NodeImpl node) {
        if (this.mutationEvents) {
            saveEnclosingAttr(node);
        }
    }

    @Override
    void replacedNode(NodeImpl node) {
        if (this.mutationEvents) {
            dispatchAggregateEvents(node, this.savedEnclosingAttr);
        }
    }

    @Override
    void modifiedAttrValue(AttrImpl attr, String oldvalue) {
        if (this.mutationEvents) {
            dispatchAggregateEvents(attr, attr, oldvalue, (short) 1);
        }
    }

    @Override
    void setAttrNode(AttrImpl attr, AttrImpl previous) {
        if (this.mutationEvents) {
            if (previous == null) {
                dispatchAggregateEvents(attr.ownerNode, attr, null, (short) 2);
            } else {
                dispatchAggregateEvents(attr.ownerNode, attr, previous.getNodeValue(), (short) 1);
            }
        }
    }

    @Override
    void removedAttrNode(AttrImpl attr, NodeImpl oldOwner, String name) {
        if (this.mutationEvents) {
            mutationEventsRemovedAttrNode(attr, oldOwner, name);
        }
    }

    private void mutationEventsRemovedAttrNode(AttrImpl attr, NodeImpl oldOwner, String name) {
        LCount lc = LCount.lookup(MutationEventImpl.DOM_ATTR_MODIFIED);
        if (lc.total > 0) {
            MutationEventImpl me = new MutationEventImpl();
            me.initMutationEvent(MutationEventImpl.DOM_ATTR_MODIFIED, true, false, attr, attr.getNodeValue(), null, name, (short) 3);
            dispatchEvent(oldOwner, me);
        }
        dispatchAggregateEvents(oldOwner, null, null, (short) 0);
    }

    @Override
    void renamedAttrNode(Attr oldAt, Attr newAt) {
    }

    @Override
    void renamedElement(Element oldEl, Element newEl) {
    }
}
