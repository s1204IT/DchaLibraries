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
    public DTMDefaultBaseTraversers(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z) {
        super(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z);
    }

    public DTMDefaultBaseTraversers(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z, int i2, boolean z2, boolean z3) {
        super(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, i2, z2, z3);
    }

    @Override
    public DTMAxisTraverser getAxisTraverser(int i) {
        DTMAxisTraverser ancestorTraverser;
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
                ancestorTraverser = new AncestorTraverser();
                break;
            case 1:
                ancestorTraverser = new AncestorOrSelfTraverser();
                break;
            case 2:
                ancestorTraverser = new AttributeTraverser();
                break;
            case 3:
                ancestorTraverser = new ChildTraverser();
                break;
            case 4:
                ancestorTraverser = new DescendantTraverser();
                break;
            case 5:
                ancestorTraverser = new DescendantOrSelfTraverser();
                break;
            case 6:
                ancestorTraverser = new FollowingTraverser();
                break;
            case 7:
                ancestorTraverser = new FollowingSiblingTraverser();
                break;
            case 8:
                ancestorTraverser = new NamespaceDeclsTraverser();
                break;
            case 9:
                ancestorTraverser = new NamespaceTraverser();
                break;
            case 10:
                ancestorTraverser = new ParentTraverser();
                break;
            case 11:
                ancestorTraverser = new PrecedingTraverser();
                break;
            case 12:
                ancestorTraverser = new PrecedingSiblingTraverser();
                break;
            case 13:
                ancestorTraverser = new SelfTraverser();
                break;
            case 14:
                ancestorTraverser = new AllFromNodeTraverser();
                break;
            case 15:
                ancestorTraverser = new PrecedingAndAncestorTraverser();
                break;
            case 16:
                ancestorTraverser = new AllFromRootTraverser();
                break;
            case 17:
                ancestorTraverser = new DescendantFromRootTraverser();
                break;
            case 18:
                ancestorTraverser = new DescendantOrSelfFromRootTraverser();
                break;
            case 19:
                ancestorTraverser = new RootTraverser();
                break;
            case 20:
                return null;
            default:
                throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_UNKNOWN_AXIS_TYPE, new Object[]{Integer.toString(i)}));
        }
        this.m_traversers[i] = ancestorTraverser;
        return ancestorTraverser;
    }

    private class AncestorTraverser extends DTMAxisTraverser {
        private AncestorTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            return DTMDefaultBaseTraversers.this.getParent(i2);
        }

        @Override
        public int next(int i, int i2, int i3) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2);
            do {
                iMakeNodeIdentity = DTMDefaultBaseTraversers.this.m_parent.elementAt(iMakeNodeIdentity);
                if (-1 == iMakeNodeIdentity) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.m_exptype.elementAt(iMakeNodeIdentity) != i3);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
        }
    }

    private class AncestorOrSelfTraverser extends AncestorTraverser {
        private AncestorOrSelfTraverser() {
            super();
        }

        @Override
        public int first(int i) {
            return i;
        }

        @Override
        public int first(int i, int i2) {
            return DTMDefaultBaseTraversers.this.getExpandedTypeID(i) == i2 ? i : next(i, i, i2);
        }
    }

    private class AttributeTraverser extends DTMAxisTraverser {
        private AttributeTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            return i == i2 ? DTMDefaultBaseTraversers.this.getFirstAttribute(i) : DTMDefaultBaseTraversers.this.getNextAttribute(i2);
        }

        @Override
        public int next(int i, int i2, int i3) {
            int firstAttribute = i == i2 ? DTMDefaultBaseTraversers.this.getFirstAttribute(i) : DTMDefaultBaseTraversers.this.getNextAttribute(i2);
            while (DTMDefaultBaseTraversers.this.getExpandedTypeID(firstAttribute) != i3) {
                firstAttribute = DTMDefaultBaseTraversers.this.getNextAttribute(firstAttribute);
                if (-1 == firstAttribute) {
                    return -1;
                }
            }
            return firstAttribute;
        }
    }

    private class ChildTraverser extends DTMAxisTraverser {
        private ChildTraverser() {
        }

        protected int getNextIndexed(int i, int i2, int i3) {
            int namespaceID = DTMDefaultBaseTraversers.this.m_expandedNameTable.getNamespaceID(i3);
            int localNameID = DTMDefaultBaseTraversers.this.m_expandedNameTable.getLocalNameID(i3);
            while (true) {
                int iFindElementFromIndex = DTMDefaultBaseTraversers.this.findElementFromIndex(namespaceID, localNameID, i2);
                if (-2 != iFindElementFromIndex) {
                    int iElementAt = DTMDefaultBaseTraversers.this.m_parent.elementAt(iFindElementFromIndex);
                    if (iElementAt == i) {
                        return iFindElementFromIndex;
                    }
                    if (iElementAt < i) {
                        return -1;
                    }
                    do {
                        iElementAt = DTMDefaultBaseTraversers.this.m_parent.elementAt(iElementAt);
                        if (iElementAt < i) {
                            return -1;
                        }
                    } while (iElementAt > i);
                    i2 = iFindElementFromIndex + 1;
                } else {
                    DTMDefaultBaseTraversers.this.nextNode();
                    if (DTMDefaultBaseTraversers.this.m_nextsib.elementAt(i) != -2) {
                        return -1;
                    }
                }
            }
        }

        @Override
        public int first(int i) {
            return DTMDefaultBaseTraversers.this.getFirstChild(i);
        }

        @Override
        public int first(int i, int i2) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(iMakeNodeIdentity, DTMDefaultBaseTraversers.this._firstch(iMakeNodeIdentity), i2));
        }

        @Override
        public int next(int i, int i2) {
            return DTMDefaultBaseTraversers.this.getNextSibling(i2);
        }

        @Override
        public int next(int i, int i2, int i3) {
            int i_nextsib = DTMDefaultBaseTraversers.this._nextsib(DTMDefaultBaseTraversers.this.makeNodeIdentity(i2));
            while (-1 != i_nextsib) {
                if (DTMDefaultBaseTraversers.this.m_exptype.elementAt(i_nextsib) != i3) {
                    i_nextsib = DTMDefaultBaseTraversers.this._nextsib(i_nextsib);
                } else {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(i_nextsib);
                }
            }
            return -1;
        }
    }

    private abstract class IndexedDTMAxisTraverser extends DTMAxisTraverser {
        protected abstract boolean axisHasBeenProcessed(int i);

        protected abstract boolean isAfterAxis(int i, int i2);

        private IndexedDTMAxisTraverser() {
        }

        protected final boolean isIndexed(int i) {
            return DTMDefaultBaseTraversers.this.m_indexing && 1 == DTMDefaultBaseTraversers.this.m_expandedNameTable.getType(i);
        }

        protected int getNextIndexed(int i, int i2, int i3) {
            int namespaceID = DTMDefaultBaseTraversers.this.m_expandedNameTable.getNamespaceID(i3);
            int localNameID = DTMDefaultBaseTraversers.this.m_expandedNameTable.getLocalNameID(i3);
            while (true) {
                int iFindElementFromIndex = DTMDefaultBaseTraversers.this.findElementFromIndex(namespaceID, localNameID, i2);
                if (-2 != iFindElementFromIndex) {
                    if (isAfterAxis(i, iFindElementFromIndex)) {
                        return -1;
                    }
                    return iFindElementFromIndex;
                }
                if (axisHasBeenProcessed(i)) {
                    return -1;
                }
                DTMDefaultBaseTraversers.this.nextNode();
            }
        }
    }

    private class DescendantTraverser extends IndexedDTMAxisTraverser {
        private DescendantTraverser() {
            super();
        }

        protected int getFirstPotential(int i) {
            return i + 1;
        }

        @Override
        protected boolean axisHasBeenProcessed(int i) {
            return DTMDefaultBaseTraversers.this.m_nextsib.elementAt(i) != -2;
        }

        protected int getSubtreeRoot(int i) {
            return DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
        }

        protected boolean isDescendant(int i, int i2) {
            return DTMDefaultBaseTraversers.this._parent(i2) >= i;
        }

        @Override
        protected boolean isAfterAxis(int i, int i2) {
            while (i2 != i) {
                i2 = DTMDefaultBaseTraversers.this.m_parent.elementAt(i2);
                if (i2 < i) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int first(int i, int i2) {
            if (isIndexed(i2)) {
                int subtreeRoot = getSubtreeRoot(i);
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(subtreeRoot, getFirstPotential(subtreeRoot), i2));
            }
            return next(i, i, i2);
        }

        @Override
        public int next(int i, int i2) {
            int subtreeRoot = getSubtreeRoot(i);
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2);
            while (true) {
                iMakeNodeIdentity++;
                short s_type = DTMDefaultBaseTraversers.this._type(iMakeNodeIdentity);
                if (!isDescendant(subtreeRoot, iMakeNodeIdentity)) {
                    return -1;
                }
                if (2 != s_type && 13 != s_type) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
                }
            }
        }

        @Override
        public int next(int i, int i2, int i3) {
            int subtreeRoot = getSubtreeRoot(i);
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) + 1;
            if (isIndexed(i3)) {
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(subtreeRoot, iMakeNodeIdentity, i3));
            }
            while (true) {
                int i_exptype = DTMDefaultBaseTraversers.this._exptype(iMakeNodeIdentity);
                if (!isDescendant(subtreeRoot, iMakeNodeIdentity)) {
                    return -1;
                }
                if (i_exptype != i3) {
                    iMakeNodeIdentity++;
                } else {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
                }
            }
        }
    }

    private class DescendantOrSelfTraverser extends DescendantTraverser {
        private DescendantOrSelfTraverser() {
            super();
        }

        @Override
        protected int getFirstPotential(int i) {
            return i;
        }

        @Override
        public int first(int i) {
            return i;
        }
    }

    private class AllFromNodeTraverser extends DescendantOrSelfTraverser {
        private AllFromNodeTraverser() {
            super();
        }

        @Override
        public int next(int i, int i2) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            int iMakeNodeIdentity2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) + 1;
            DTMDefaultBaseTraversers.this._exptype(iMakeNodeIdentity2);
            if (!isDescendant(iMakeNodeIdentity, iMakeNodeIdentity2)) {
                return -1;
            }
            return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity2);
        }
    }

    private class FollowingTraverser extends DescendantTraverser {
        private FollowingTraverser() {
            super();
        }

        @Override
        public int first(int i) {
            int i_firstch;
            int i_nextsib;
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            short s_type = DTMDefaultBaseTraversers.this._type(iMakeNodeIdentity);
            if ((2 != s_type && 13 != s_type) || -1 == (i_firstch = DTMDefaultBaseTraversers.this._firstch((iMakeNodeIdentity = DTMDefaultBaseTraversers.this._parent(iMakeNodeIdentity))))) {
                do {
                    i_nextsib = DTMDefaultBaseTraversers.this._nextsib(iMakeNodeIdentity);
                    if (-1 == i_nextsib) {
                        iMakeNodeIdentity = DTMDefaultBaseTraversers.this._parent(iMakeNodeIdentity);
                    }
                    if (-1 != i_nextsib) {
                        break;
                    }
                } while (-1 != iMakeNodeIdentity);
                return DTMDefaultBaseTraversers.this.makeNodeHandle(i_nextsib);
            }
            return DTMDefaultBaseTraversers.this.makeNodeHandle(i_firstch);
        }

        @Override
        public int first(int i, int i2) {
            int firstChild;
            int nextSibling;
            short nodeType = DTMDefaultBaseTraversers.this.getNodeType(i);
            if ((2 != nodeType && 13 != nodeType) || -1 == (firstChild = DTMDefaultBaseTraversers.this.getFirstChild((i = DTMDefaultBaseTraversers.this.getParent(i))))) {
                do {
                    nextSibling = DTMDefaultBaseTraversers.this.getNextSibling(i);
                    if (-1 == nextSibling) {
                        i = DTMDefaultBaseTraversers.this.getParent(i);
                        if (-1 != nextSibling) {
                            break;
                        }
                    } else {
                        if (DTMDefaultBaseTraversers.this.getExpandedTypeID(nextSibling) == i2) {
                            return nextSibling;
                        }
                        return next(i, nextSibling, i2);
                    }
                } while (-1 != i);
                return nextSibling;
            }
            if (DTMDefaultBaseTraversers.this.getExpandedTypeID(firstChild) == i2) {
                return firstChild;
            }
            return next(i, firstChild, i2);
        }

        @Override
        public int next(int i, int i2) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2);
            while (true) {
                iMakeNodeIdentity++;
                short s_type = DTMDefaultBaseTraversers.this._type(iMakeNodeIdentity);
                if (-1 == s_type) {
                    return -1;
                }
                if (2 != s_type && 13 != s_type) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
                }
            }
        }

        @Override
        public int next(int i, int i2, int i3) {
            int i_exptype;
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2);
            do {
                iMakeNodeIdentity++;
                i_exptype = DTMDefaultBaseTraversers.this._exptype(iMakeNodeIdentity);
                if (-1 == i_exptype) {
                    return -1;
                }
            } while (i_exptype != i3);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
        }
    }

    private class FollowingSiblingTraverser extends DTMAxisTraverser {
        private FollowingSiblingTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            return DTMDefaultBaseTraversers.this.getNextSibling(i2);
        }

        @Override
        public int next(int i, int i2, int i3) {
            do {
                i2 = DTMDefaultBaseTraversers.this.getNextSibling(i2);
                if (-1 == i2) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.getExpandedTypeID(i2) != i3);
            return i2;
        }
    }

    private class NamespaceDeclsTraverser extends DTMAxisTraverser {
        private NamespaceDeclsTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            return i == i2 ? DTMDefaultBaseTraversers.this.getFirstNamespaceNode(i, false) : DTMDefaultBaseTraversers.this.getNextNamespaceNode(i, i2, false);
        }

        @Override
        public int next(int i, int i2, int i3) {
            int firstNamespaceNode = i == i2 ? DTMDefaultBaseTraversers.this.getFirstNamespaceNode(i, false) : DTMDefaultBaseTraversers.this.getNextNamespaceNode(i, i2, false);
            while (DTMDefaultBaseTraversers.this.getExpandedTypeID(firstNamespaceNode) != i3) {
                firstNamespaceNode = DTMDefaultBaseTraversers.this.getNextNamespaceNode(i, firstNamespaceNode, false);
                if (-1 == firstNamespaceNode) {
                    return -1;
                }
            }
            return firstNamespaceNode;
        }
    }

    private class NamespaceTraverser extends DTMAxisTraverser {
        private NamespaceTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            return i == i2 ? DTMDefaultBaseTraversers.this.getFirstNamespaceNode(i, true) : DTMDefaultBaseTraversers.this.getNextNamespaceNode(i, i2, true);
        }

        @Override
        public int next(int i, int i2, int i3) {
            int firstNamespaceNode = i == i2 ? DTMDefaultBaseTraversers.this.getFirstNamespaceNode(i, true) : DTMDefaultBaseTraversers.this.getNextNamespaceNode(i, i2, true);
            while (DTMDefaultBaseTraversers.this.getExpandedTypeID(firstNamespaceNode) != i3) {
                firstNamespaceNode = DTMDefaultBaseTraversers.this.getNextNamespaceNode(i, firstNamespaceNode, true);
                if (-1 == firstNamespaceNode) {
                    return -1;
                }
            }
            return firstNamespaceNode;
        }
    }

    private class ParentTraverser extends DTMAxisTraverser {
        private ParentTraverser() {
        }

        @Override
        public int first(int i) {
            return DTMDefaultBaseTraversers.this.getParent(i);
        }

        @Override
        public int first(int i, int i2) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            do {
                iMakeNodeIdentity = DTMDefaultBaseTraversers.this.m_parent.elementAt(iMakeNodeIdentity);
                if (-1 == iMakeNodeIdentity) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.m_exptype.elementAt(iMakeNodeIdentity) != i2);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
        }

        @Override
        public int next(int i, int i2) {
            return -1;
        }

        @Override
        public int next(int i, int i2, int i3) {
            return -1;
        }
    }

    private class PrecedingTraverser extends DTMAxisTraverser {
        private PrecedingTraverser() {
        }

        protected boolean isAncestor(int i, int i2) {
            int iElementAt = DTMDefaultBaseTraversers.this.m_parent.elementAt(i);
            while (-1 != iElementAt) {
                if (iElementAt != i2) {
                    iElementAt = DTMDefaultBaseTraversers.this.m_parent.elementAt(iElementAt);
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int next(int i, int i2) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            for (int iMakeNodeIdentity2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) - 1; iMakeNodeIdentity2 >= 0; iMakeNodeIdentity2--) {
                short s_type = DTMDefaultBaseTraversers.this._type(iMakeNodeIdentity2);
                if (2 != s_type && 13 != s_type && !isAncestor(iMakeNodeIdentity, iMakeNodeIdentity2)) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity2);
                }
            }
            return -1;
        }

        @Override
        public int next(int i, int i2, int i3) {
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            for (int iMakeNodeIdentity2 = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) - 1; iMakeNodeIdentity2 >= 0; iMakeNodeIdentity2--) {
                if (DTMDefaultBaseTraversers.this.m_exptype.elementAt(iMakeNodeIdentity2) == i3 && !isAncestor(iMakeNodeIdentity, iMakeNodeIdentity2)) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity2);
                }
            }
            return -1;
        }
    }

    private class PrecedingAndAncestorTraverser extends DTMAxisTraverser {
        private PrecedingAndAncestorTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            for (int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) - 1; iMakeNodeIdentity >= 0; iMakeNodeIdentity--) {
                short s_type = DTMDefaultBaseTraversers.this._type(iMakeNodeIdentity);
                if (2 != s_type && 13 != s_type) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
                }
            }
            return -1;
        }

        @Override
        public int next(int i, int i2, int i3) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            for (int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) - 1; iMakeNodeIdentity >= 0; iMakeNodeIdentity--) {
                if (DTMDefaultBaseTraversers.this.m_exptype.elementAt(iMakeNodeIdentity) == i3) {
                    return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
                }
            }
            return -1;
        }
    }

    private class PrecedingSiblingTraverser extends DTMAxisTraverser {
        private PrecedingSiblingTraverser() {
        }

        @Override
        public int next(int i, int i2) {
            return DTMDefaultBaseTraversers.this.getPreviousSibling(i2);
        }

        @Override
        public int next(int i, int i2, int i3) {
            do {
                i2 = DTMDefaultBaseTraversers.this.getPreviousSibling(i2);
                if (-1 == i2) {
                    return -1;
                }
            } while (DTMDefaultBaseTraversers.this.getExpandedTypeID(i2) != i3);
            return i2;
        }
    }

    private class SelfTraverser extends DTMAxisTraverser {
        private SelfTraverser() {
        }

        @Override
        public int first(int i) {
            return i;
        }

        @Override
        public int first(int i, int i2) {
            if (DTMDefaultBaseTraversers.this.getExpandedTypeID(i) == i2) {
                return i;
            }
            return -1;
        }

        @Override
        public int next(int i, int i2) {
            return -1;
        }

        @Override
        public int next(int i, int i2, int i3) {
            return -1;
        }
    }

    private class AllFromRootTraverser extends AllFromNodeTraverser {
        private AllFromRootTraverser() {
            super();
        }

        @Override
        public int first(int i) {
            return DTMDefaultBaseTraversers.this.getDocumentRoot(i);
        }

        @Override
        public int first(int i, int i2) {
            return DTMDefaultBaseTraversers.this.getExpandedTypeID(DTMDefaultBaseTraversers.this.getDocumentRoot(i)) == i2 ? i : next(i, i, i2);
        }

        @Override
        public int next(int i, int i2) {
            DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2) + 1;
            if (DTMDefaultBaseTraversers.this._type(iMakeNodeIdentity) == -1) {
                return -1;
            }
            return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
        }

        @Override
        public int next(int i, int i2, int i3) {
            int i_exptype;
            DTMDefaultBaseTraversers.this.makeNodeIdentity(i);
            int iMakeNodeIdentity = DTMDefaultBaseTraversers.this.makeNodeIdentity(i2);
            do {
                iMakeNodeIdentity++;
                i_exptype = DTMDefaultBaseTraversers.this._exptype(iMakeNodeIdentity);
                if (i_exptype == -1) {
                    return -1;
                }
            } while (i_exptype != i3);
            return DTMDefaultBaseTraversers.this.makeNodeHandle(iMakeNodeIdentity);
        }
    }

    private class RootTraverser extends AllFromRootTraverser {
        private RootTraverser() {
            super();
        }

        @Override
        public int first(int i, int i2) {
            int documentRoot = DTMDefaultBaseTraversers.this.getDocumentRoot(i);
            if (DTMDefaultBaseTraversers.this.getExpandedTypeID(documentRoot) == i2) {
                return documentRoot;
            }
            return -1;
        }

        @Override
        public int next(int i, int i2) {
            return -1;
        }

        @Override
        public int next(int i, int i2, int i3) {
            return -1;
        }
    }

    private class DescendantOrSelfFromRootTraverser extends DescendantTraverser {
        private DescendantOrSelfFromRootTraverser() {
            super();
        }

        @Override
        protected int getFirstPotential(int i) {
            return i;
        }

        @Override
        protected int getSubtreeRoot(int i) {
            return DTMDefaultBaseTraversers.this.makeNodeIdentity(DTMDefaultBaseTraversers.this.getDocument());
        }

        @Override
        public int first(int i) {
            return DTMDefaultBaseTraversers.this.getDocumentRoot(i);
        }

        @Override
        public int first(int i, int i2) {
            if (isIndexed(i2)) {
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(0, getFirstPotential(0), i2));
            }
            int iFirst = first(i);
            return next(iFirst, iFirst, i2);
        }
    }

    private class DescendantFromRootTraverser extends DescendantTraverser {
        private DescendantFromRootTraverser() {
            super();
        }

        @Override
        protected int getFirstPotential(int i) {
            return DTMDefaultBaseTraversers.this._firstch(0);
        }

        @Override
        protected int getSubtreeRoot(int i) {
            return 0;
        }

        @Override
        public int first(int i) {
            return DTMDefaultBaseTraversers.this.makeNodeHandle(DTMDefaultBaseTraversers.this._firstch(0));
        }

        @Override
        public int first(int i, int i2) {
            if (isIndexed(i2)) {
                return DTMDefaultBaseTraversers.this.makeNodeHandle(getNextIndexed(0, getFirstPotential(0), i2));
            }
            int documentRoot = DTMDefaultBaseTraversers.this.getDocumentRoot(i);
            return next(documentRoot, documentRoot, i2);
        }
    }
}
