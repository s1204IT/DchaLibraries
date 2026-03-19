package org.apache.xml.dtm.ref;

import javax.xml.transform.Source;
import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.utils.NodeVector;
import org.apache.xml.utils.XMLStringFactory;
import org.apache.xpath.axes.WalkerFactory;

public abstract class DTMDefaultBaseIterators extends DTMDefaultBaseTraversers {
    public DTMDefaultBaseIterators(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z) {
        super(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z);
    }

    public DTMDefaultBaseIterators(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z, int i2, boolean z2, boolean z3) {
        super(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, i2, z2, z3);
    }

    @Override
    public DTMAxisIterator getTypedAxisIterator(int i, int i2) {
        if (i != 19) {
            switch (i) {
                case 0:
                    return new TypedAncestorIterator(i2);
                case 1:
                    return new TypedAncestorIterator(i2).includeSelf();
                case 2:
                    return new TypedAttributeIterator(i2);
                case 3:
                    return new TypedChildrenIterator(i2);
                case 4:
                    return new TypedDescendantIterator(i2);
                case 5:
                    return new TypedDescendantIterator(i2).includeSelf();
                case 6:
                    return new TypedFollowingIterator(i2);
                case 7:
                    return new TypedFollowingSiblingIterator(i2);
                default:
                    switch (i) {
                        case 9:
                            return new TypedNamespaceIterator(i2);
                        case 10:
                            return new ParentIterator().setNodeType(i2);
                        case 11:
                            return new TypedPrecedingIterator(i2);
                        case 12:
                            return new TypedPrecedingSiblingIterator(i2);
                        case 13:
                            return new TypedSingletonIterator(i2);
                        default:
                            throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED, new Object[]{Axis.getNames(i)}));
                    }
            }
        }
        return new TypedRootIterator(i2);
    }

    @Override
    public DTMAxisIterator getAxisIterator(int i) {
        if (i != 19) {
            switch (i) {
                case 0:
                    return new AncestorIterator();
                case 1:
                    return new AncestorIterator().includeSelf();
                case 2:
                    return new AttributeIterator();
                case 3:
                    return new ChildrenIterator();
                case 4:
                    return new DescendantIterator();
                case 5:
                    return new DescendantIterator().includeSelf();
                case 6:
                    return new FollowingIterator();
                case 7:
                    return new FollowingSiblingIterator();
                default:
                    switch (i) {
                        case 9:
                            return new NamespaceIterator();
                        case 10:
                            return new ParentIterator();
                        case 11:
                            return new PrecedingIterator();
                        case 12:
                            return new PrecedingSiblingIterator();
                        case 13:
                            return new SingletonIterator(this);
                        default:
                            throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ITERATOR_AXIS_NOT_IMPLEMENTED, new Object[]{Axis.getNames(i)}));
                    }
            }
        }
        return new RootIterator();
    }

    public abstract class InternalAxisIteratorBase extends DTMAxisIteratorBase {
        protected int _currentNode;

        public InternalAxisIteratorBase() {
        }

        @Override
        public void setMark() {
            this._markedNode = this._currentNode;
        }

        @Override
        public void gotoMark() {
            this._currentNode = this._markedNode;
        }
    }

    public final class ChildrenIterator extends InternalAxisIteratorBase {
        public ChildrenIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int i_firstch = -1;
                if (i != -1) {
                    i_firstch = DTMDefaultBaseIterators.this._firstch(DTMDefaultBaseIterators.this.makeNodeIdentity(i));
                }
                this._currentNode = i_firstch;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            if (this._currentNode == -1) {
                return -1;
            }
            int i = this._currentNode;
            this._currentNode = DTMDefaultBaseIterators.this._nextsib(i);
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i));
        }
    }

    public final class ParentIterator extends InternalAxisIteratorBase {
        private int _nodeType;

        public ParentIterator() {
            super();
            this._nodeType = -1;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = DTMDefaultBaseIterators.this.getParent(i);
                return resetPosition();
            }
            return this;
        }

        public DTMAxisIterator setNodeType(int i) {
            this._nodeType = i;
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            if (this._nodeType < 14 ? !(this._nodeType == -1 || this._nodeType == DTMDefaultBaseIterators.this.getNodeType(this._currentNode)) : this._nodeType != DTMDefaultBaseIterators.this.getExpandedTypeID(this._currentNode)) {
                i = -1;
            }
            this._currentNode = -1;
            return returnNode(i);
        }
    }

    public final class TypedChildrenIterator extends InternalAxisIteratorBase {
        private final int _nodeType;

        public TypedChildrenIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int i_firstch = -1;
                if (i != -1) {
                    i_firstch = DTMDefaultBaseIterators.this._firstch(DTMDefaultBaseIterators.this.makeNodeIdentity(this._startNode));
                }
                this._currentNode = i_firstch;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i_nextsib = this._currentNode;
            int i = this._nodeType;
            if (i >= 14) {
                while (i_nextsib != -1 && DTMDefaultBaseIterators.this._exptype(i_nextsib) != i) {
                    i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                }
            } else {
                while (i_nextsib != -1) {
                    int i_exptype = DTMDefaultBaseIterators.this._exptype(i_nextsib);
                    if (i_exptype < 14) {
                        if (i_exptype == i) {
                            break;
                        }
                        i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    } else {
                        if (DTMDefaultBaseIterators.this.m_expandedNameTable.getType(i_exptype) == i) {
                            break;
                        }
                        i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    }
                }
            }
            if (i_nextsib == -1) {
                this._currentNode = -1;
                return -1;
            }
            this._currentNode = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i_nextsib));
        }
    }

    public final class NamespaceChildrenIterator extends InternalAxisIteratorBase {
        private final int _nsType;

        public NamespaceChildrenIterator(int i) {
            super();
            this._nsType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = i != -1 ? -2 : -1;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i_nextsib;
            if (this._currentNode != -1) {
                if (-2 == this._currentNode) {
                    i_nextsib = DTMDefaultBaseIterators.this._firstch(DTMDefaultBaseIterators.this.makeNodeIdentity(this._startNode));
                } else {
                    i_nextsib = DTMDefaultBaseIterators.this._nextsib(this._currentNode);
                }
                while (i_nextsib != -1) {
                    if (DTMDefaultBaseIterators.this.m_expandedNameTable.getNamespaceID(DTMDefaultBaseIterators.this._exptype(i_nextsib)) != this._nsType) {
                        i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    } else {
                        this._currentNode = i_nextsib;
                        return returnNode(i_nextsib);
                    }
                }
            }
            return -1;
        }
    }

    public class NamespaceIterator extends InternalAxisIteratorBase {
        public NamespaceIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = DTMDefaultBaseIterators.this.getFirstNamespaceNode(i, true);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            if (-1 != i) {
                this._currentNode = DTMDefaultBaseIterators.this.getNextNamespaceNode(this._startNode, i, true);
            }
            return returnNode(i);
        }
    }

    public class TypedNamespaceIterator extends NamespaceIterator {
        private final int _nodeType;

        public TypedNamespaceIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            int nextNamespaceNode = this._currentNode;
            while (nextNamespaceNode != -1) {
                if (DTMDefaultBaseIterators.this.getExpandedTypeID(nextNamespaceNode) != this._nodeType && DTMDefaultBaseIterators.this.getNodeType(nextNamespaceNode) != this._nodeType && DTMDefaultBaseIterators.this.getNamespaceType(nextNamespaceNode) != this._nodeType) {
                    nextNamespaceNode = DTMDefaultBaseIterators.this.getNextNamespaceNode(this._startNode, nextNamespaceNode, true);
                } else {
                    this._currentNode = nextNamespaceNode;
                    return returnNode(nextNamespaceNode);
                }
            }
            this._currentNode = -1;
            return -1;
        }
    }

    public class RootIterator extends InternalAxisIteratorBase {
        public RootIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (this._isRestartable) {
                this._startNode = DTMDefaultBaseIterators.this.getDocumentRoot(i);
                this._currentNode = -1;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            if (this._startNode == this._currentNode) {
                return -1;
            }
            this._currentNode = this._startNode;
            return returnNode(this._startNode);
        }
    }

    public class TypedRootIterator extends RootIterator {
        private final int _nodeType;

        public TypedRootIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            if (this._startNode == this._currentNode) {
                return -1;
            }
            int i = this._nodeType;
            int i2 = this._startNode;
            int expandedTypeID = DTMDefaultBaseIterators.this.getExpandedTypeID(i2);
            this._currentNode = i2;
            if (i >= 14) {
                if (i == expandedTypeID) {
                    return returnNode(i2);
                }
            } else if (expandedTypeID < 14) {
                if (expandedTypeID == i) {
                    return returnNode(i2);
                }
            } else if (DTMDefaultBaseIterators.this.m_expandedNameTable.getType(expandedTypeID) == i) {
                return returnNode(i2);
            }
            return -1;
        }
    }

    public final class NamespaceAttributeIterator extends InternalAxisIteratorBase {
        private final int _nsType;

        public NamespaceAttributeIterator(int i) {
            super();
            this._nsType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = DTMDefaultBaseIterators.this.getFirstNamespaceNode(i, false);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            if (-1 != i) {
                this._currentNode = DTMDefaultBaseIterators.this.getNextNamespaceNode(this._startNode, i, false);
            }
            return returnNode(i);
        }
    }

    public class FollowingSiblingIterator extends InternalAxisIteratorBase {
        public FollowingSiblingIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = DTMDefaultBaseIterators.this.makeNodeIdentity(i);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            this._currentNode = this._currentNode != -1 ? DTMDefaultBaseIterators.this._nextsib(this._currentNode) : -1;
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(this._currentNode));
        }
    }

    public final class TypedFollowingSiblingIterator extends FollowingSiblingIterator {
        private final int _nodeType;

        public TypedFollowingSiblingIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            if (this._currentNode == -1) {
                return -1;
            }
            int i_nextsib = this._currentNode;
            int i = this._nodeType;
            if (i >= 14) {
                do {
                    i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    if (i_nextsib == -1) {
                        break;
                    }
                } while (DTMDefaultBaseIterators.this._exptype(i_nextsib) != i);
            } else {
                while (true) {
                    i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    if (i_nextsib == -1) {
                        break;
                    }
                    int i_exptype = DTMDefaultBaseIterators.this._exptype(i_nextsib);
                    if (i_exptype < 14) {
                        if (i_exptype == i) {
                            break;
                        }
                    } else if (DTMDefaultBaseIterators.this.m_expandedNameTable.getType(i_exptype) == i) {
                        break;
                    }
                }
            }
            this._currentNode = i_nextsib;
            if (this._currentNode == -1) {
                return -1;
            }
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(this._currentNode));
        }
    }

    public final class AttributeIterator extends InternalAxisIteratorBase {
        public AttributeIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = DTMDefaultBaseIterators.this.getFirstAttributeIdentity(DTMDefaultBaseIterators.this.makeNodeIdentity(i));
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            if (i == -1) {
                return -1;
            }
            this._currentNode = DTMDefaultBaseIterators.this.getNextAttributeIdentity(i);
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i));
        }
    }

    public final class TypedAttributeIterator extends InternalAxisIteratorBase {
        private final int _nodeType;

        public TypedAttributeIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = DTMDefaultBaseIterators.this.getTypedAttribute(i, this._nodeType);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            this._currentNode = -1;
            return returnNode(i);
        }
    }

    public class PrecedingSiblingIterator extends InternalAxisIteratorBase {
        protected int _startNodeID;

        public PrecedingSiblingIterator() {
            super();
        }

        @Override
        public boolean isReverse() {
            return true;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int iMakeNodeIdentity = DTMDefaultBaseIterators.this.makeNodeIdentity(i);
                this._startNodeID = iMakeNodeIdentity;
                if (iMakeNodeIdentity == -1) {
                    this._currentNode = iMakeNodeIdentity;
                    return resetPosition();
                }
                short type = DTMDefaultBaseIterators.this.m_expandedNameTable.getType(DTMDefaultBaseIterators.this._exptype(iMakeNodeIdentity));
                if (2 == type || 13 == type) {
                    this._currentNode = iMakeNodeIdentity;
                } else {
                    this._currentNode = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                    if (-1 != this._currentNode) {
                        this._currentNode = DTMDefaultBaseIterators.this._firstch(this._currentNode);
                    } else {
                        this._currentNode = iMakeNodeIdentity;
                    }
                }
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            if (this._currentNode == this._startNodeID || this._currentNode == -1) {
                return -1;
            }
            int i = this._currentNode;
            this._currentNode = DTMDefaultBaseIterators.this._nextsib(i);
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i));
        }
    }

    public final class TypedPrecedingSiblingIterator extends PrecedingSiblingIterator {
        private final int _nodeType;

        public TypedPrecedingSiblingIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            int i_nextsib = this._currentNode;
            int i = this._nodeType;
            int i2 = this._startNodeID;
            if (i >= 14) {
                while (i_nextsib != -1 && i_nextsib != i2 && DTMDefaultBaseIterators.this._exptype(i_nextsib) != i) {
                    i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                }
            } else {
                while (i_nextsib != -1 && i_nextsib != i2) {
                    int i_exptype = DTMDefaultBaseIterators.this._exptype(i_nextsib);
                    if (i_exptype < 14) {
                        if (i_exptype == i) {
                            break;
                        }
                        i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    } else {
                        if (DTMDefaultBaseIterators.this.m_expandedNameTable.getType(i_exptype) == i) {
                            break;
                        }
                        i_nextsib = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
                    }
                }
            }
            if (i_nextsib == -1 || i_nextsib == this._startNodeID) {
                this._currentNode = -1;
                return -1;
            }
            this._currentNode = DTMDefaultBaseIterators.this._nextsib(i_nextsib);
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i_nextsib));
        }
    }

    public class PrecedingIterator extends InternalAxisIteratorBase {
        protected int _markedDescendant;
        protected int _markedNode;
        protected int _markedsp;
        private final int _maxAncestors;
        protected int _oldsp;
        protected int _sp;
        protected int[] _stack;

        public PrecedingIterator() {
            super();
            this._maxAncestors = 8;
            this._stack = new int[8];
        }

        @Override
        public boolean isReverse() {
            return true;
        }

        @Override
        public DTMAxisIterator cloneIterator() {
            this._isRestartable = false;
            try {
                PrecedingIterator precedingIterator = (PrecedingIterator) super.clone();
                int[] iArr = new int[this._stack.length];
                System.arraycopy(this._stack, 0, iArr, 0, this._stack.length);
                precedingIterator._stack = iArr;
                return precedingIterator;
            } catch (CloneNotSupportedException e) {
                throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ITERATOR_CLONE_NOT_SUPPORTED, null));
            }
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                int iMakeNodeIdentity = DTMDefaultBaseIterators.this.makeNodeIdentity(i);
                if (DTMDefaultBaseIterators.this._type(iMakeNodeIdentity) == 2) {
                    iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                }
                this._startNode = iMakeNodeIdentity;
                this._stack[0] = iMakeNodeIdentity;
                int i2 = 0;
                while (true) {
                    iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                    if (iMakeNodeIdentity == -1) {
                        break;
                    }
                    i2++;
                    if (i2 == this._stack.length) {
                        int[] iArr = new int[i2 + 4];
                        System.arraycopy(this._stack, 0, iArr, 0, i2);
                        this._stack = iArr;
                    }
                    this._stack[i2] = iMakeNodeIdentity;
                }
                if (i2 > 0) {
                    i2--;
                }
                this._currentNode = this._stack[i2];
                this._sp = i2;
                this._oldsp = i2;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            this._currentNode++;
            while (this._sp >= 0) {
                if (this._currentNode < this._stack[this._sp]) {
                    if (DTMDefaultBaseIterators.this._type(this._currentNode) != 2 && DTMDefaultBaseIterators.this._type(this._currentNode) != 13) {
                        return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(this._currentNode));
                    }
                } else {
                    this._sp--;
                }
                this._currentNode++;
            }
            return -1;
        }

        @Override
        public DTMAxisIterator reset() {
            this._sp = this._oldsp;
            return resetPosition();
        }

        @Override
        public void setMark() {
            this._markedsp = this._sp;
            this._markedNode = this._currentNode;
            this._markedDescendant = this._stack[0];
        }

        @Override
        public void gotoMark() {
            this._sp = this._markedsp;
            this._currentNode = this._markedNode;
        }
    }

    public final class TypedPrecedingIterator extends PrecedingIterator {
        private final int _nodeType;

        public TypedPrecedingIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            int i2 = this._nodeType;
            if (i2 < 14) {
                while (true) {
                    i++;
                    if (this._sp < 0) {
                        break;
                    }
                    if (i >= this._stack[this._sp]) {
                        int i3 = this._sp - 1;
                        this._sp = i3;
                        if (i3 < 0) {
                            break;
                        }
                    } else {
                        int i_exptype = DTMDefaultBaseIterators.this._exptype(i);
                        if (i_exptype < 14) {
                            if (i_exptype == i2) {
                                break;
                            }
                        } else if (DTMDefaultBaseIterators.this.m_expandedNameTable.getType(i_exptype) == i2) {
                            break;
                        }
                    }
                }
            } else {
                while (true) {
                    i++;
                    if (this._sp < 0) {
                        break;
                    }
                    if (i >= this._stack[this._sp]) {
                        int i4 = this._sp - 1;
                        this._sp = i4;
                        if (i4 < 0) {
                            break;
                        }
                    } else if (DTMDefaultBaseIterators.this._exptype(i) == i2) {
                        break;
                    }
                }
            }
            this._currentNode = i;
            if (i == -1) {
                return -1;
            }
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i));
        }
    }

    public class FollowingIterator extends InternalAxisIteratorBase {
        DTMAxisTraverser m_traverser;

        public FollowingIterator() {
            super();
            this.m_traverser = DTMDefaultBaseIterators.this.getAxisTraverser(6);
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = this.m_traverser.first(i);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            this._currentNode = this.m_traverser.next(this._startNode, this._currentNode);
            return returnNode(i);
        }
    }

    public final class TypedFollowingIterator extends FollowingIterator {
        private final int _nodeType;

        public TypedFollowingIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            int i;
            do {
                i = this._currentNode;
                this._currentNode = this.m_traverser.next(this._startNode, this._currentNode);
                if (i == -1 || DTMDefaultBaseIterators.this.getExpandedTypeID(i) == this._nodeType) {
                    break;
                }
            } while (DTMDefaultBaseIterators.this.getNodeType(i) != this._nodeType);
            if (i == -1) {
                return -1;
            }
            return returnNode(i);
        }
    }

    public class AncestorIterator extends InternalAxisIteratorBase {
        NodeVector m_ancestors;
        int m_ancestorsPos;
        int m_markedPos;
        int m_realStartNode;

        public AncestorIterator() {
            super();
            this.m_ancestors = new NodeVector();
        }

        @Override
        public int getStartNode() {
            return this.m_realStartNode;
        }

        @Override
        public final boolean isReverse() {
            return true;
        }

        @Override
        public DTMAxisIterator cloneIterator() {
            this._isRestartable = false;
            try {
                AncestorIterator ancestorIterator = (AncestorIterator) super.clone();
                ancestorIterator._startNode = this._startNode;
                return ancestorIterator;
            } catch (CloneNotSupportedException e) {
                throw new DTMException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ITERATOR_CLONE_NOT_SUPPORTED, null));
            }
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            this.m_realStartNode = i;
            if (this._isRestartable) {
                int iMakeNodeIdentity = DTMDefaultBaseIterators.this.makeNodeIdentity(i);
                if (!this._includeSelf && i != -1) {
                    iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                    i = DTMDefaultBaseIterators.this.makeNodeHandle(iMakeNodeIdentity);
                }
                this._startNode = i;
                while (iMakeNodeIdentity != -1) {
                    this.m_ancestors.addElement(i);
                    iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                    i = DTMDefaultBaseIterators.this.makeNodeHandle(iMakeNodeIdentity);
                }
                this.m_ancestorsPos = this.m_ancestors.size() - 1;
                this._currentNode = this.m_ancestorsPos >= 0 ? this.m_ancestors.elementAt(this.m_ancestorsPos) : -1;
                return resetPosition();
            }
            return this;
        }

        @Override
        public DTMAxisIterator reset() {
            this.m_ancestorsPos = this.m_ancestors.size() - 1;
            this._currentNode = this.m_ancestorsPos >= 0 ? this.m_ancestors.elementAt(this.m_ancestorsPos) : -1;
            return resetPosition();
        }

        @Override
        public int next() {
            int i = this._currentNode;
            int i2 = this.m_ancestorsPos - 1;
            this.m_ancestorsPos = i2;
            this._currentNode = i2 >= 0 ? this.m_ancestors.elementAt(this.m_ancestorsPos) : -1;
            return returnNode(i);
        }

        @Override
        public void setMark() {
            this.m_markedPos = this.m_ancestorsPos;
        }

        @Override
        public void gotoMark() {
            this.m_ancestorsPos = this.m_markedPos;
            this._currentNode = this.m_ancestorsPos >= 0 ? this.m_ancestors.elementAt(this.m_ancestorsPos) : -1;
        }
    }

    public final class TypedAncestorIterator extends AncestorIterator {
        private final int _nodeType;

        public TypedAncestorIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            this.m_realStartNode = i;
            if (this._isRestartable) {
                int iMakeNodeIdentity = DTMDefaultBaseIterators.this.makeNodeIdentity(i);
                int i2 = this._nodeType;
                if (!this._includeSelf && i != -1) {
                    iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                }
                this._startNode = i;
                if (i2 >= 14) {
                    while (iMakeNodeIdentity != -1) {
                        if (DTMDefaultBaseIterators.this._exptype(iMakeNodeIdentity) == i2) {
                            this.m_ancestors.addElement(DTMDefaultBaseIterators.this.makeNodeHandle(iMakeNodeIdentity));
                        }
                        iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                    }
                } else {
                    while (iMakeNodeIdentity != -1) {
                        int i_exptype = DTMDefaultBaseIterators.this._exptype(iMakeNodeIdentity);
                        if ((i_exptype >= 14 && DTMDefaultBaseIterators.this.m_expandedNameTable.getType(i_exptype) == i2) || (i_exptype < 14 && i_exptype == i2)) {
                            this.m_ancestors.addElement(DTMDefaultBaseIterators.this.makeNodeHandle(iMakeNodeIdentity));
                        }
                        iMakeNodeIdentity = DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity);
                    }
                }
                this.m_ancestorsPos = this.m_ancestors.size() - 1;
                this._currentNode = this.m_ancestorsPos >= 0 ? this.m_ancestors.elementAt(this.m_ancestorsPos) : -1;
                return resetPosition();
            }
            return this;
        }
    }

    public class DescendantIterator extends InternalAxisIteratorBase {
        public DescendantIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isRestartable) {
                int iMakeNodeIdentity = DTMDefaultBaseIterators.this.makeNodeIdentity(i);
                this._startNode = iMakeNodeIdentity;
                if (this._includeSelf) {
                    iMakeNodeIdentity--;
                }
                this._currentNode = iMakeNodeIdentity;
                return resetPosition();
            }
            return this;
        }

        protected boolean isDescendant(int i) {
            return DTMDefaultBaseIterators.this._parent(i) >= this._startNode || this._startNode == i;
        }

        @Override
        public int next() {
            if (this._startNode == -1) {
                return -1;
            }
            if (this._includeSelf && this._currentNode + 1 == this._startNode) {
                DTMDefaultBaseIterators dTMDefaultBaseIterators = DTMDefaultBaseIterators.this;
                int i = this._currentNode + 1;
                this._currentNode = i;
                return returnNode(dTMDefaultBaseIterators.makeNodeHandle(i));
            }
            int i2 = this._currentNode;
            while (true) {
                i2++;
                short s_type = DTMDefaultBaseIterators.this._type(i2);
                if (-1 == s_type || !isDescendant(i2)) {
                    break;
                }
                if (2 != s_type && 3 != s_type && 13 != s_type) {
                    this._currentNode = i2;
                    return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i2));
                }
            }
        }

        @Override
        public DTMAxisIterator reset() {
            boolean z = this._isRestartable;
            this._isRestartable = true;
            setStartNode(DTMDefaultBaseIterators.this.makeNodeHandle(this._startNode));
            this._isRestartable = z;
            return this;
        }
    }

    public final class TypedDescendantIterator extends DescendantIterator {
        private final int _nodeType;

        public TypedDescendantIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public int next() {
            if (this._startNode == -1) {
                return -1;
            }
            int i = this._currentNode;
            do {
                i++;
                short s_type = DTMDefaultBaseIterators.this._type(i);
                if (-1 == s_type || !isDescendant(i)) {
                    this._currentNode = -1;
                    return -1;
                }
                if (s_type == this._nodeType) {
                    break;
                }
            } while (DTMDefaultBaseIterators.this._exptype(i) != this._nodeType);
            this._currentNode = i;
            return returnNode(DTMDefaultBaseIterators.this.makeNodeHandle(i));
        }
    }

    public class NthDescendantIterator extends DescendantIterator {
        int _pos;

        public NthDescendantIterator(int i) {
            super();
            this._pos = i;
        }

        @Override
        public int next() {
            int iMakeNodeIdentity;
            int i_firstch;
            do {
                int next = super.next();
                if (next == -1) {
                    return -1;
                }
                iMakeNodeIdentity = DTMDefaultBaseIterators.this.makeNodeIdentity(next);
                i_firstch = DTMDefaultBaseIterators.this._firstch(DTMDefaultBaseIterators.this._parent(iMakeNodeIdentity));
                int i = 0;
                do {
                    if (1 == DTMDefaultBaseIterators.this._type(i_firstch)) {
                        i++;
                    }
                    if (i >= this._pos) {
                        break;
                    }
                    i_firstch = DTMDefaultBaseIterators.this._nextsib(i_firstch);
                } while (i_firstch != -1);
            } while (iMakeNodeIdentity != i_firstch);
            return iMakeNodeIdentity;
        }
    }

    public class SingletonIterator extends InternalAxisIteratorBase {
        private boolean _isConstant;

        public SingletonIterator(DTMDefaultBaseIterators dTMDefaultBaseIterators) {
            this(WalkerFactory.BIT_MATCH_PATTERN, false);
        }

        public SingletonIterator(DTMDefaultBaseIterators dTMDefaultBaseIterators, int i) {
            this(i, false);
        }

        public SingletonIterator(int i, boolean z) {
            super();
            this._startNode = i;
            this._currentNode = i;
            this._isConstant = z;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = DTMDefaultBaseIterators.this.getDocument();
            }
            if (this._isConstant) {
                this._currentNode = this._startNode;
                return resetPosition();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = i;
                return resetPosition();
            }
            return this;
        }

        @Override
        public DTMAxisIterator reset() {
            if (this._isConstant) {
                this._currentNode = this._startNode;
                return resetPosition();
            }
            boolean z = this._isRestartable;
            this._isRestartable = true;
            setStartNode(this._startNode);
            this._isRestartable = z;
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            this._currentNode = -1;
            return returnNode(i);
        }
    }

    public final class TypedSingletonIterator extends SingletonIterator {
        private final int _nodeType;

        public TypedSingletonIterator(int i) {
            super(DTMDefaultBaseIterators.this);
            this._nodeType = i;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            int i2 = this._nodeType;
            this._currentNode = -1;
            if (i2 >= 14) {
                if (DTMDefaultBaseIterators.this.getExpandedTypeID(i) == i2) {
                    return returnNode(i);
                }
            } else if (DTMDefaultBaseIterators.this.getNodeType(i) == i2) {
                return returnNode(i);
            }
            return -1;
        }
    }
}
