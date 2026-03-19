package org.apache.xml.dtm.ref;

import javax.xml.transform.Source;
import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.XMLStringFactory;

public abstract class DTMDefaultBaseTraversers extends DTMDefaultBase {
    public DTMDefaultBaseTraversers(DTMManager mgr, Source source, int dtmIdentity, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory, boolean doIndexing) {
        super(mgr, source, dtmIdentity, whiteSpaceFilter, xstringfactory, doIndexing);
    }

    public DTMDefaultBaseTraversers(DTMManager mgr, Source source, int dtmIdentity, DTMWSFilter whiteSpaceFilter, XMLStringFactory xstringfactory, boolean doIndexing, int blocksize, boolean usePrevsib, boolean newNameTable) {
        super(mgr, source, dtmIdentity, whiteSpaceFilter, xstringfactory, doIndexing, blocksize, usePrevsib, newNameTable);
    }

    @Override
    public DTMAxisTraverser getAxisTraverser(int i) {
        DTMAxisTraverser rootTraverser;
        AncestorTraverser ancestorTraverser = null;
        Object[] objArr = 0;
        Object[] objArr2 = 0;
        Object[] objArr3 = 0;
        Object[] objArr4 = 0;
        Object[] objArr5 = 0;
        Object[] objArr6 = 0;
        Object[] objArr7 = 0;
        Object[] objArr8 = 0;
        Object[] objArr9 = 0;
        Object[] objArr10 = 0;
        Object[] objArr11 = 0;
        Object[] objArr12 = 0;
        Object[] objArr13 = 0;
        Object[] objArr14 = 0;
        Object[] objArr15 = 0;
        Object[] objArr16 = 0;
        Object[] objArr17 = 0;
        Object[] objArr18 = 0;
        Object[] objArr19 = 0;
        if (this.m_traversers == null) {
            this.m_traversers = new DTMAxisTraverser[Axis.getNamesLength()];
        } else {
            DTMAxisTraverser dTMAxisTraverser = this.m_traversers[i];
            if (dTMAxisTraverser != null) {
                return dTMAxisTraverser;
            }
        }
        switch (i) {
            case 0:
                rootTraverser = new AncestorTraverser(this, ancestorTraverser);
                break;
            case 1:
                rootTraverser = new AncestorOrSelfTraverser(this, objArr19 == true ? 1 : 0);
                break;
            case 2:
                rootTraverser = new AttributeTraverser(this, objArr18 == true ? 1 : 0);
                break;
            case 3:
                rootTraverser = new ChildTraverser(this, objArr17 == true ? 1 : 0);
                break;
            case 4:
                rootTraverser = new DescendantTraverser(this, objArr16 == true ? 1 : 0);
                break;
            case 5:
                rootTraverser = new DescendantOrSelfTraverser(this, objArr15 == true ? 1 : 0);
                break;
            case 6:
                rootTraverser = new FollowingTraverser(this, objArr14 == true ? 1 : 0);
                break;
            case 7:
                rootTraverser = new FollowingSiblingTraverser(this, objArr13 == true ? 1 : 0);
                break;
            case 8:
                rootTraverser = new NamespaceDeclsTraverser(this, objArr11 == true ? 1 : 0);
                break;
            case 9:
                rootTraverser = new NamespaceTraverser(this, objArr12 == true ? 1 : 0);
                break;
            case 10:
                rootTraverser = new ParentTraverser(this, objArr10 == true ? 1 : 0);
                break;
            case 11:
                rootTraverser = new PrecedingTraverser(this, objArr9 == true ? 1 : 0);
                break;
            case 12:
                rootTraverser = new PrecedingSiblingTraverser(this, objArr8 == true ? 1 : 0);
                break;
            case 13:
                rootTraverser = new SelfTraverser(this, objArr7 == true ? 1 : 0);
                break;
            case 14:
                rootTraverser = new AllFromNodeTraverser(this, objArr5 == true ? 1 : 0);
                break;
            case 15:
                rootTraverser = new PrecedingAndAncestorTraverser(this, objArr4 == true ? 1 : 0);
                break;
            case 16:
                rootTraverser = new AllFromRootTraverser(this, objArr6 == true ? 1 : 0);
                break;
            case 17:
                rootTraverser = new DescendantFromRootTraverser(this, objArr3 == true ? 1 : 0);
                break;
            case 18:
                rootTraverser = new DescendantOrSelfFromRootTraverser(this, objArr2 == true ? 1 : 0);
                break;
            case 19:
                rootTraverser = new RootTraverser(this, objArr == true ? 1 : 0);
                break;
            case 20:
                return null;
            default:
                throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_UNKNOWN_AXIS_TYPE, new Object[]{Integer.toString(i)}));
        }
        if (rootTraverser == null) {
            throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_AXIS_TRAVERSER_NOT_SUPPORTED, new Object[]{Axis.getNames(i)}));
        }
        this.m_traversers[i] = rootTraverser;
        return rootTraverser;
    }

    private class AncestorTraverser extends DTMAxisTraverser {
        AncestorTraverser(DTMDefaultBaseTraversers this$0, AncestorTraverser ancestorTraverser) {
            this();
        }

        private AncestorTraverser() {
        }

        @Override
        public int next(int context, int current) {
            return DTMDefaultBaseTraversers.this.getParent(current);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current);
            do {
                current2 = DTMDefaultBaseTraversers.this.m_parent.elementAt(current2);
                if (-1 == current2) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.m_exptype.elementAt(current2) != expandedTypeID);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
        }
    }

    private class AncestorOrSelfTraverser extends AncestorTraverser {
        AncestorOrSelfTraverser(DTMDefaultBaseTraversers this$0, AncestorOrSelfTraverser ancestorOrSelfTraverser) {
            this();
        }

        private AncestorOrSelfTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        public int first(int context) {
            return context;
        }

        @Override
        public int first(int context, int expandedTypeID) {
            return DTMDefaultBaseTraversers.this.getExpandedTypeID(context) == expandedTypeID ? context : next(context, context, expandedTypeID);
        }
    }

    private class AttributeTraverser extends DTMAxisTraverser {
        AttributeTraverser(DTMDefaultBaseTraversers this$0, AttributeTraverser attributeTraverser) {
            this();
        }

        private AttributeTraverser() {
        }

        @Override
        public int next(int context, int current) {
            return context == current ? DTMDefaultBaseTraversers.this.getFirstAttribute(context) : DTMDefaultBaseTraversers.this.getNextAttribute(current);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int current2 = context == current ? DTMDefaultBaseTraversers.this.getFirstAttribute(context) : DTMDefaultBaseTraversers.this.getNextAttribute(current);
            while (DTMDefaultBaseTraversers.this.getExpandedTypeID(current2) != expandedTypeID) {
                current2 = DTMDefaultBaseTraversers.this.getNextAttribute(current2);
                if (-1 == current2) {
                    return -1;
                }
            }
            return current2;
        }
    }

    private class ChildTraverser extends DTMAxisTraverser {
        ChildTraverser(DTMDefaultBaseTraversers this$0, ChildTraverser childTraverser) {
            this();
        }

        private ChildTraverser() {
        }

        protected int getNextIndexed(int axisRoot, int nextPotential, int expandedTypeID) {
            int nsIndex = DTMDefaultBaseTraversers.this.m_expandedNameTable.getNamespaceID(expandedTypeID);
            int lnIndex = DTMDefaultBaseTraversers.this.m_expandedNameTable.getLocalNameID(expandedTypeID);
            while (true) {
                int nextID = DTMDefaultBaseTraversers.this.findElementFromIndex(nsIndex, lnIndex, nextPotential);
                if (-2 != nextID) {
                    int parentID = DTMDefaultBaseTraversers.this.m_parent.elementAt(nextID);
                    if (parentID == axisRoot) {
                        return nextID;
                    }
                    if (parentID < axisRoot) {
                        return -1;
                    }
                    do {
                        parentID = DTMDefaultBaseTraversers.this.m_parent.elementAt(parentID);
                        if (parentID < axisRoot) {
                            return -1;
                        }
                    } while (parentID > axisRoot);
                    nextPotential = nextID + 1;
                } else {
                    DTMDefaultBaseTraversers.this.nextNode();
                    if (DTMDefaultBaseTraversers.this.m_nextsib.elementAt(axisRoot) != -2) {
                        return -1;
                    }
                }
            }
        }

        @Override
        public int first(int context) {
            return DTMDefaultBaseTraversers.this.getFirstChild(context);
        }

        @Override
        public int first(int context, int expandedTypeID) {
            int identity = DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            int firstMatch = getNextIndexed(identity, DTMDefaultBaseTraversers.this._firstch(identity), expandedTypeID);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(firstMatch);
        }

        @Override
        public int next(int context, int current) {
            return DTMDefaultBaseTraversers.this.getNextSibling(current);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int current2 = DTMDefaultBaseTraversers.this._nextsib(DTMDefaultBaseTraversers.this.makeNodeIdentity(current));
            while (-1 != current2) {
                if (DTMDefaultBaseTraversers.this.m_exptype.elementAt(current2) != expandedTypeID) {
                    current2 = DTMDefaultBaseTraversers.this._nextsib(current2);
                } else {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
            return -1;
        }
    }

    private abstract class IndexedDTMAxisTraverser extends DTMAxisTraverser {
        IndexedDTMAxisTraverser(DTMDefaultBaseTraversers this$0, IndexedDTMAxisTraverser indexedDTMAxisTraverser) {
            this();
        }

        protected abstract boolean axisHasBeenProcessed(int i);

        protected abstract boolean isAfterAxis(int i, int i2);

        private IndexedDTMAxisTraverser() {
        }

        protected final boolean isIndexed(int expandedTypeID) {
            return DTMDefaultBaseTraversers.this.m_indexing && 1 == DTMDefaultBaseTraversers.this.m_expandedNameTable.getType(expandedTypeID);
        }

        protected int getNextIndexed(int axisRoot, int nextPotential, int expandedTypeID) {
            int nsIndex = DTMDefaultBaseTraversers.this.m_expandedNameTable.getNamespaceID(expandedTypeID);
            int lnIndex = DTMDefaultBaseTraversers.this.m_expandedNameTable.getLocalNameID(expandedTypeID);
            while (true) {
                int next = DTMDefaultBaseTraversers.this.findElementFromIndex(nsIndex, lnIndex, nextPotential);
                if (-2 != next) {
                    if (isAfterAxis(axisRoot, next)) {
                        return -1;
                    }
                    return next;
                }
                if (axisHasBeenProcessed(axisRoot)) {
                    return -1;
                }
                DTMDefaultBaseTraversers.this.nextNode();
            }
        }
    }

    private class DescendantTraverser extends IndexedDTMAxisTraverser {
        DescendantTraverser(DTMDefaultBaseTraversers this$0, DescendantTraverser descendantTraverser) {
            this();
        }

        private DescendantTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        protected int getFirstPotential(int identity) {
            return identity + 1;
        }

        @Override
        protected boolean axisHasBeenProcessed(int axisRoot) {
            return DTMDefaultBaseTraversers.this.m_nextsib.elementAt(axisRoot) != -2;
        }

        protected int getSubtreeRoot(int handle) {
            return DTMDefaultBaseTraversers.this.makeNodeIdentity(handle);
        }

        protected boolean isDescendant(int subtreeRootIdentity, int identity) {
            return DTMDefaultBaseTraversers.this._parent(identity) >= subtreeRootIdentity;
        }

        @Override
        protected boolean isAfterAxis(int axisRoot, int identity) {
            while (identity != axisRoot) {
                identity = DTMDefaultBaseTraversers.this.m_parent.elementAt(identity);
                if (identity < axisRoot) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int first(int context, int expandedTypeID) {
            if (isIndexed(expandedTypeID)) {
                int identity = getSubtreeRoot(context);
                int firstPotential = getFirstPotential(identity);
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(identity, firstPotential, expandedTypeID));
            }
            return next(context, context, expandedTypeID);
        }

        @Override
        public int next(int context, int current) {
            int subtreeRootIdent = getSubtreeRoot(context);
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) + 1;
            while (true) {
                int type = DTMDefaultBaseTraversers.this._type(current2);
                if (!isDescendant(subtreeRootIdent, current2)) {
                    return -1;
                }
                if (2 == type || 13 == type) {
                    current2++;
                } else {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int subtreeRootIdent = getSubtreeRoot(context);
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) + 1;
            if (isIndexed(expandedTypeID)) {
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(subtreeRootIdent, current2, expandedTypeID));
            }
            while (true) {
                int exptype = DTMDefaultBaseTraversers.this._exptype(current2);
                if (!isDescendant(subtreeRootIdent, current2)) {
                    return -1;
                }
                if (exptype != expandedTypeID) {
                    current2++;
                } else {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
        }
    }

    private class DescendantOrSelfTraverser extends DescendantTraverser {
        DescendantOrSelfTraverser(DTMDefaultBaseTraversers this$0, DescendantOrSelfTraverser descendantOrSelfTraverser) {
            this();
        }

        private DescendantOrSelfTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        protected int getFirstPotential(int identity) {
            return identity;
        }

        @Override
        public int first(int context) {
            return context;
        }
    }

    private class AllFromNodeTraverser extends DescendantOrSelfTraverser {
        AllFromNodeTraverser(DTMDefaultBaseTraversers this$0, AllFromNodeTraverser allFromNodeTraverser) {
            this();
        }

        private AllFromNodeTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        public int next(int context, int current) {
            int subtreeRootIdent = DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) + 1;
            DTMDefaultBaseTraversers.this._exptype(current2);
            if (!isDescendant(subtreeRootIdent, current2)) {
                return -1;
            }
            return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
        }
    }

    private class FollowingTraverser extends DescendantTraverser {
        FollowingTraverser(DTMDefaultBaseTraversers this$0, FollowingTraverser followingTraverser) {
            this();
        }

        private FollowingTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        public int first(int context) {
            int first;
            int first2;
            int context2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            int type = DTMDefaultBaseTraversers.this._type(context2);
            if ((2 == type || 13 == type) && -1 != (first = DTMDefaultBaseTraversers.this._firstch((context2 = DTMDefaultBaseTraversers.this._parent(context2))))) {
                return DTMDefaultBaseTraversers.this.makeNodeHandle(first);
            }
            do {
                first2 = DTMDefaultBaseTraversers.this._nextsib(context2);
                if (-1 == first2) {
                    context2 = DTMDefaultBaseTraversers.this._parent(context2);
                }
                if (-1 != first2) {
                    break;
                }
            } while (-1 != context2);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(first2);
        }

        @Override
        public int first(int context, int expandedTypeID) {
            int first;
            int first2;
            int type = DTMDefaultBaseTraversers.this.getNodeType(context);
            if ((2 == type || 13 == type) && -1 != (first = DTMDefaultBaseTraversers.this.getFirstChild((context = DTMDefaultBaseTraversers.this.getParent(context))))) {
                if (DTMDefaultBaseTraversers.this.getExpandedTypeID(first) == expandedTypeID) {
                    return first;
                }
                return next(context, first, expandedTypeID);
            }
            do {
                first2 = DTMDefaultBaseTraversers.this.getNextSibling(context);
                if (-1 == first2) {
                    context = DTMDefaultBaseTraversers.this.getParent(context);
                    if (-1 != first2) {
                        break;
                    }
                } else {
                    if (DTMDefaultBaseTraversers.this.getExpandedTypeID(first2) == expandedTypeID) {
                        return first2;
                    }
                    return next(context, first2, expandedTypeID);
                }
            } while (-1 != context);
            return first2;
        }

        @Override
        public int next(int context, int current) {
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current);
            while (true) {
                current2++;
                int type = DTMDefaultBaseTraversers.this._type(current2);
                if (-1 == type) {
                    return -1;
                }
                if (2 != type && 13 != type) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int etype;
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current);
            do {
                current2++;
                etype = DTMDefaultBaseTraversers.this._exptype(current2);
                if (-1 == etype) {
                    return -1;
                }
            } while (etype != expandedTypeID);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
        }
    }

    private class FollowingSiblingTraverser extends DTMAxisTraverser {
        FollowingSiblingTraverser(DTMDefaultBaseTraversers this$0, FollowingSiblingTraverser followingSiblingTraverser) {
            this();
        }

        private FollowingSiblingTraverser() {
        }

        @Override
        public int next(int context, int current) {
            return DTMDefaultBaseTraversers.this.getNextSibling(current);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            do {
                current = DTMDefaultBaseTraversers.this.getNextSibling(current);
                if (-1 == current) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.getExpandedTypeID(current) != expandedTypeID);
            return current;
        }
    }

    private class NamespaceDeclsTraverser extends DTMAxisTraverser {
        NamespaceDeclsTraverser(DTMDefaultBaseTraversers this$0, NamespaceDeclsTraverser namespaceDeclsTraverser) {
            this();
        }

        private NamespaceDeclsTraverser() {
        }

        @Override
        public int next(int context, int current) {
            if (context == current) {
                return DTMDefaultBaseTraversers.this.getFirstNamespaceNode(context, false);
            }
            return DTMDefaultBaseTraversers.this.getNextNamespaceNode(context, current, false);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int current2;
            if (context == current) {
                current2 = DTMDefaultBaseTraversers.this.getFirstNamespaceNode(context, false);
            } else {
                current2 = DTMDefaultBaseTraversers.this.getNextNamespaceNode(context, current, false);
            }
            while (DTMDefaultBaseTraversers.this.getExpandedTypeID(current2) != expandedTypeID) {
                current2 = DTMDefaultBaseTraversers.this.getNextNamespaceNode(context, current2, false);
                if (-1 == current2) {
                    return -1;
                }
            }
            return current2;
        }
    }

    private class NamespaceTraverser extends DTMAxisTraverser {
        NamespaceTraverser(DTMDefaultBaseTraversers this$0, NamespaceTraverser namespaceTraverser) {
            this();
        }

        private NamespaceTraverser() {
        }

        @Override
        public int next(int context, int current) {
            if (context == current) {
                return DTMDefaultBaseTraversers.this.getFirstNamespaceNode(context, true);
            }
            return DTMDefaultBaseTraversers.this.getNextNamespaceNode(context, current, true);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int current2;
            if (context == current) {
                current2 = DTMDefaultBaseTraversers.this.getFirstNamespaceNode(context, true);
            } else {
                current2 = DTMDefaultBaseTraversers.this.getNextNamespaceNode(context, current, true);
            }
            while (DTMDefaultBaseTraversers.this.getExpandedTypeID(current2) != expandedTypeID) {
                current2 = DTMDefaultBaseTraversers.this.getNextNamespaceNode(context, current2, true);
                if (-1 == current2) {
                    return -1;
                }
            }
            return current2;
        }
    }

    private class ParentTraverser extends DTMAxisTraverser {
        ParentTraverser(DTMDefaultBaseTraversers this$0, ParentTraverser parentTraverser) {
            this();
        }

        private ParentTraverser() {
        }

        @Override
        public int first(int context) {
            return DTMDefaultBaseTraversers.this.getParent(context);
        }

        @Override
        public int first(int current, int expandedTypeID) {
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current);
            do {
                current2 = DTMDefaultBaseTraversers.this.m_parent.elementAt(current2);
                if (-1 == current2) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.m_exptype.elementAt(current2) != expandedTypeID);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
        }

        @Override
        public int next(int context, int current) {
            return -1;
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            return -1;
        }
    }

    private class PrecedingTraverser extends DTMAxisTraverser {
        PrecedingTraverser(DTMDefaultBaseTraversers this$0, PrecedingTraverser precedingTraverser) {
            this();
        }

        private PrecedingTraverser() {
        }

        protected boolean isAncestor(int contextIdent, int currentIdent) {
            int contextIdent2 = DTMDefaultBaseTraversers.this.m_parent.elementAt(contextIdent);
            while (-1 != contextIdent2) {
                if (contextIdent2 != currentIdent) {
                    contextIdent2 = DTMDefaultBaseTraversers.this.m_parent.elementAt(contextIdent2);
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int next(int context, int current) {
            int subtreeRootIdent = DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            for (int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) - 1; current2 >= 0; current2--) {
                short type = DTMDefaultBaseTraversers.this._type(current2);
                if (2 != type && 13 != type && !isAncestor(subtreeRootIdent, current2)) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
            return -1;
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            int subtreeRootIdent = DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            for (int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) - 1; current2 >= 0; current2--) {
                int exptype = DTMDefaultBaseTraversers.this.m_exptype.elementAt(current2);
                if (exptype == expandedTypeID && !isAncestor(subtreeRootIdent, current2)) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
            return -1;
        }
    }

    private class PrecedingAndAncestorTraverser extends DTMAxisTraverser {
        PrecedingAndAncestorTraverser(DTMDefaultBaseTraversers this$0, PrecedingAndAncestorTraverser precedingAndAncestorTraverser) {
            this();
        }

        private PrecedingAndAncestorTraverser() {
        }

        @Override
        public int next(int context, int current) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            for (int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) - 1; current2 >= 0; current2--) {
                short type = DTMDefaultBaseTraversers.this._type(current2);
                if (2 != type && 13 != type) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
            return -1;
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            for (int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) - 1; current2 >= 0; current2--) {
                int exptype = DTMDefaultBaseTraversers.this.m_exptype.elementAt(current2);
                if (exptype == expandedTypeID) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
            return -1;
        }
    }

    private class PrecedingSiblingTraverser extends DTMAxisTraverser {
        PrecedingSiblingTraverser(DTMDefaultBaseTraversers this$0, PrecedingSiblingTraverser precedingSiblingTraverser) {
            this();
        }

        private PrecedingSiblingTraverser() {
        }

        @Override
        public int next(int context, int current) {
            return DTMDefaultBaseTraversers.this.getPreviousSibling(current);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            do {
                current = DTMDefaultBaseTraversers.this.getPreviousSibling(current);
                if (-1 == current) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.getExpandedTypeID(current) != expandedTypeID);
            return current;
        }
    }

    private class SelfTraverser extends DTMAxisTraverser {
        SelfTraverser(DTMDefaultBaseTraversers this$0, SelfTraverser selfTraverser) {
            this();
        }

        private SelfTraverser() {
        }

        @Override
        public int first(int context) {
            return context;
        }

        @Override
        public int first(int context, int expandedTypeID) {
            if (DTMDefaultBaseTraversers.this.getExpandedTypeID(context) == expandedTypeID) {
                return context;
            }
            return -1;
        }

        @Override
        public int next(int context, int current) {
            return -1;
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            return -1;
        }
    }

    private class AllFromRootTraverser extends AllFromNodeTraverser {
        AllFromRootTraverser(DTMDefaultBaseTraversers this$0, AllFromRootTraverser allFromRootTraverser) {
            this();
        }

        private AllFromRootTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        public int first(int context) {
            return DTMDefaultBaseTraversers.this.getDocumentRoot(context);
        }

        @Override
        public int first(int context, int expandedTypeID) {
            return DTMDefaultBaseTraversers.this.getExpandedTypeID(DTMDefaultBaseTraversers.this.getDocumentRoot(context)) == expandedTypeID ? context : next(context, context, expandedTypeID);
        }

        @Override
        public int next(int context, int current) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) + 1;
            int type = DTMDefaultBaseTraversers.this._type(current2);
            if (type == -1) {
                return -1;
            }
            return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(context);
            int current2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(current) + 1;
            while (true) {
                int exptype = DTMDefaultBaseTraversers.this._exptype(current2);
                if (exptype == -1) {
                    return -1;
                }
                if (exptype != expandedTypeID) {
                    current2++;
                } else {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(current2);
                }
            }
        }
    }

    private class RootTraverser extends AllFromRootTraverser {
        RootTraverser(DTMDefaultBaseTraversers this$0, RootTraverser rootTraverser) {
            this();
        }

        private RootTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        public int first(int context, int expandedTypeID) {
            int root = DTMDefaultBaseTraversers.this.getDocumentRoot(context);
            if (DTMDefaultBaseTraversers.this.getExpandedTypeID(root) == expandedTypeID) {
                return root;
            }
            return -1;
        }

        @Override
        public int next(int context, int current) {
            return -1;
        }

        @Override
        public int next(int context, int current, int expandedTypeID) {
            return -1;
        }
    }

    private class DescendantOrSelfFromRootTraverser extends DescendantTraverser {
        DescendantOrSelfFromRootTraverser(DTMDefaultBaseTraversers this$0, DescendantOrSelfFromRootTraverser descendantOrSelfFromRootTraverser) {
            this();
        }

        private DescendantOrSelfFromRootTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        protected int getFirstPotential(int identity) {
            return identity;
        }

        @Override
        protected int getSubtreeRoot(int handle) {
            return DTMDefaultBaseTraversers.this.makeNodeIdentity(DTMDefaultBaseTraversers.this.getDocument());
        }

        @Override
        public int first(int context) {
            return DTMDefaultBaseTraversers.this.getDocumentRoot(context);
        }

        @Override
        public int first(int context, int expandedTypeID) {
            if (isIndexed(expandedTypeID)) {
                int firstPotential = getFirstPotential(0);
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(0, firstPotential, expandedTypeID));
            }
            int root = first(context);
            return next(root, root, expandedTypeID);
        }
    }

    private class DescendantFromRootTraverser extends DescendantTraverser {
        DescendantFromRootTraverser(DTMDefaultBaseTraversers this$0, DescendantFromRootTraverser descendantFromRootTraverser) {
            this();
        }

        private DescendantFromRootTraverser() {
            super(DTMDefaultBaseTraversers.this, null);
        }

        @Override
        protected int getFirstPotential(int identity) {
            return DTMDefaultBaseTraversers.this._firstch(0);
        }

        @Override
        protected int getSubtreeRoot(int handle) {
            return 0;
        }

        @Override
        public int first(int context) {
            return DTMDefaultBaseTraversers.this.makeNodeHandle(DTMDefaultBaseTraversers.this._firstch(0));
        }

        @Override
        public int first(int context, int expandedTypeID) {
            if (isIndexed(expandedTypeID)) {
                int firstPotential = getFirstPotential(0);
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(0, firstPotential, expandedTypeID));
            }
            int root = DTMDefaultBaseTraversers.this.getDocumentRoot(context);
            return next(root, root, expandedTypeID);
        }
    }
}
