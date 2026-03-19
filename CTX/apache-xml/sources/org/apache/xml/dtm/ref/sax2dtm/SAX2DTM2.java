package org.apache.xml.dtm.ref.sax2dtm;

import java.util.Vector;
import javax.xml.transform.Source;
import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTMWSFilter;
import org.apache.xml.dtm.ref.DTMDefaultBaseIterators;
import org.apache.xml.dtm.ref.ExtendedType;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.apache.xml.serializer.SerializationHandler;
import org.apache.xml.utils.FastStringBuffer;
import org.apache.xml.utils.SuballocatedIntVector;
import org.apache.xml.utils.XMLString;
import org.apache.xml.utils.XMLStringDefault;
import org.apache.xml.utils.XMLStringFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class SAX2DTM2 extends SAX2DTM {
    private static final String EMPTY_STR = "";
    private static final XMLString EMPTY_XML_STR = new XMLStringDefault("");
    protected static final int TEXT_LENGTH_BITS = 10;
    protected static final int TEXT_LENGTH_MAX = 1023;
    protected static final int TEXT_OFFSET_BITS = 21;
    protected static final int TEXT_OFFSET_MAX = 2097151;
    protected int m_MASK;
    protected int m_SHIFT;
    protected int m_blocksize;
    protected boolean m_buildIdIndex;
    private int[][] m_exptype_map;
    private int[] m_exptype_map0;
    protected ExtendedType[] m_extendedTypes;
    private int[][] m_firstch_map;
    private int[] m_firstch_map0;
    private int m_maxNodeIndex;
    private int[][] m_nextsib_map;
    private int[] m_nextsib_map0;
    private int[][] m_parent_map;
    private int[] m_parent_map0;
    private int m_valueIndex;
    protected Vector m_values;

    public final class ChildrenIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        public ChildrenIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int i_firstch2 = -1;
                if (i != -1) {
                    i_firstch2 = SAX2DTM2.this._firstch2(SAX2DTM2.this.makeNodeIdentity(i));
                }
                this._currentNode = i_firstch2;
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
            this._currentNode = SAX2DTM2.this._nextsib2(i);
            return returnNode(SAX2DTM2.this.makeNodeHandle(i));
        }
    }

    public final class ParentIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        private int _nodeType;

        public ParentIterator() {
            super();
            this._nodeType = -1;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                if (i != -1) {
                    this._currentNode = SAX2DTM2.this._parent2(SAX2DTM2.this.makeNodeIdentity(i));
                } else {
                    this._currentNode = -1;
                }
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
            if (i == -1) {
                return -1;
            }
            if (this._nodeType == -1) {
                this._currentNode = -1;
                return returnNode(SAX2DTM2.this.makeNodeHandle(i));
            }
            if (this._nodeType >= 14) {
                if (this._nodeType == SAX2DTM2.this._exptype2(i)) {
                    this._currentNode = -1;
                    return returnNode(SAX2DTM2.this.makeNodeHandle(i));
                }
            } else if (this._nodeType == SAX2DTM2.this._type2(i)) {
                this._currentNode = -1;
                return returnNode(SAX2DTM2.this.makeNodeHandle(i));
            }
            return -1;
        }
    }

    public final class TypedChildrenIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        private final int _nodeType;

        public TypedChildrenIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int i_firstch2 = -1;
                if (i != -1) {
                    i_firstch2 = SAX2DTM2.this._firstch2(SAX2DTM2.this.makeNodeIdentity(this._startNode));
                }
                this._currentNode = i_firstch2;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i_nextsib2 = this._currentNode;
            if (i_nextsib2 == -1) {
                return -1;
            }
            int i = this._nodeType;
            if (i != 1) {
                while (i_nextsib2 != -1 && SAX2DTM2.this._exptype2(i_nextsib2) != i) {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
            } else {
                while (i_nextsib2 != -1 && SAX2DTM2.this._exptype2(i_nextsib2) < 14) {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
            }
            if (i_nextsib2 == -1) {
                this._currentNode = -1;
                return -1;
            }
            this._currentNode = SAX2DTM2.this._nextsib2(i_nextsib2);
            return returnNode(SAX2DTM2.this.makeNodeHandle(i_nextsib2));
        }

        @Override
        public int getNodeByPosition(int i) {
            if (i <= 0) {
                return -1;
            }
            int i_nextsib2 = this._currentNode;
            int i2 = 0;
            int i3 = this._nodeType;
            if (i3 != 1) {
                while (i_nextsib2 != -1) {
                    if (SAX2DTM2.this._exptype2(i_nextsib2) == i3 && (i2 = i2 + 1) == i) {
                        return SAX2DTM2.this.makeNodeHandle(i_nextsib2);
                    }
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
                return -1;
            }
            while (i_nextsib2 != -1) {
                if (SAX2DTM2.this._exptype2(i_nextsib2) >= 14 && (i2 = i2 + 1) == i) {
                    return SAX2DTM2.this.makeNodeHandle(i_nextsib2);
                }
                i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
            }
            return -1;
        }
    }

    public class TypedRootIterator extends DTMDefaultBaseIterators.RootIterator {
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
            int i = this._startNode;
            int i_exptype2 = SAX2DTM2.this._exptype2(SAX2DTM2.this.makeNodeIdentity(i));
            this._currentNode = i;
            if (this._nodeType >= 14) {
                if (this._nodeType == i_exptype2) {
                    return returnNode(i);
                }
            } else if (i_exptype2 < 14) {
                if (i_exptype2 == this._nodeType) {
                    return returnNode(i);
                }
            } else if (SAX2DTM2.this.m_extendedTypes[i_exptype2].getNodeType() == this._nodeType) {
                return returnNode(i);
            }
            return -1;
        }
    }

    public class FollowingSiblingIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        public FollowingSiblingIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = SAX2DTM2.this.makeNodeIdentity(i);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            this._currentNode = this._currentNode != -1 ? SAX2DTM2.this._nextsib2(this._currentNode) : -1;
            return returnNode(SAX2DTM2.this.makeNodeHandle(this._currentNode));
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
            int i_nextsib2 = this._currentNode;
            int i = this._nodeType;
            if (i == 1) {
                do {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                    if (i_nextsib2 == -1) {
                        break;
                    }
                } while (SAX2DTM2.this._exptype2(i_nextsib2) < 14);
            } else {
                do {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                    if (i_nextsib2 == -1) {
                        break;
                    }
                } while (SAX2DTM2.this._exptype2(i_nextsib2) != i);
            }
            this._currentNode = i_nextsib2;
            if (i_nextsib2 == -1) {
                return -1;
            }
            return returnNode(SAX2DTM2.this.makeNodeHandle(i_nextsib2));
        }
    }

    public final class AttributeIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        public AttributeIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = SAX2DTM2.this.getFirstAttributeIdentity(SAX2DTM2.this.makeNodeIdentity(i));
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
            this._currentNode = SAX2DTM2.this.getNextAttributeIdentity(i);
            return returnNode(SAX2DTM2.this.makeNodeHandle(i));
        }
    }

    public final class TypedAttributeIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        private final int _nodeType;

        public TypedAttributeIterator(int i) {
            super();
            this._nodeType = i;
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (this._isRestartable) {
                this._startNode = i;
                this._currentNode = SAX2DTM2.this.getTypedAttribute(i, this._nodeType);
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

    public class PrecedingSiblingIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
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
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
                this._startNodeID = iMakeNodeIdentity;
                if (iMakeNodeIdentity == -1) {
                    this._currentNode = iMakeNodeIdentity;
                    return resetPosition();
                }
                int i_type2 = SAX2DTM2.this._type2(iMakeNodeIdentity);
                if (2 == i_type2 || 13 == i_type2) {
                    this._currentNode = iMakeNodeIdentity;
                } else {
                    this._currentNode = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    if (-1 != this._currentNode) {
                        this._currentNode = SAX2DTM2.this._firstch2(this._currentNode);
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
            this._currentNode = SAX2DTM2.this._nextsib2(i);
            return returnNode(SAX2DTM2.this.makeNodeHandle(i));
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
            int i_nextsib2 = this._currentNode;
            int i = this._nodeType;
            int i2 = this._startNodeID;
            if (i != 1) {
                while (i_nextsib2 != -1 && i_nextsib2 != i2 && SAX2DTM2.this._exptype2(i_nextsib2) != i) {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
            } else {
                while (i_nextsib2 != -1 && i_nextsib2 != i2 && SAX2DTM2.this._exptype2(i_nextsib2) < 14) {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
            }
            if (i_nextsib2 == -1 || i_nextsib2 == i2) {
                this._currentNode = -1;
                return -1;
            }
            this._currentNode = SAX2DTM2.this._nextsib2(i_nextsib2);
            return returnNode(SAX2DTM2.this.makeNodeHandle(i_nextsib2));
        }

        @Override
        public int getLast() {
            if (this._last != -1) {
                return this._last;
            }
            setMark();
            int i_nextsib2 = this._currentNode;
            int i = this._nodeType;
            int i2 = this._startNodeID;
            int i3 = 0;
            if (i != 1) {
                while (i_nextsib2 != -1 && i_nextsib2 != i2) {
                    if (SAX2DTM2.this._exptype2(i_nextsib2) == i) {
                        i3++;
                    }
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
            } else {
                while (i_nextsib2 != -1 && i_nextsib2 != i2) {
                    if (SAX2DTM2.this._exptype2(i_nextsib2) >= 14) {
                        i3++;
                    }
                    i_nextsib2 = SAX2DTM2.this._nextsib2(i_nextsib2);
                }
            }
            gotoMark();
            this._last = i3;
            return i3;
        }
    }

    public class PrecedingIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
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
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
                if (SAX2DTM2.this._type2(iMakeNodeIdentity) == 2) {
                    iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                }
                this._startNode = iMakeNodeIdentity;
                this._stack[0] = iMakeNodeIdentity;
                int i2 = 0;
                while (true) {
                    iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    if (iMakeNodeIdentity == -1) {
                        break;
                    }
                    i2++;
                    if (i2 == this._stack.length) {
                        int[] iArr = new int[i2 * 2];
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
            int i = this._currentNode;
            while (true) {
                this._currentNode = i + 1;
                if (this._sp >= 0) {
                    if (this._currentNode < this._stack[this._sp]) {
                        int i_type2 = SAX2DTM2.this._type2(this._currentNode);
                        if (i_type2 != 2 && i_type2 != 13) {
                            return returnNode(SAX2DTM2.this.makeNodeHandle(this._currentNode));
                        }
                    } else {
                        this._sp--;
                    }
                    i = this._currentNode;
                } else {
                    return -1;
                }
            }
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
                        int i_exptype2 = SAX2DTM2.this._exptype2(i);
                        if (i_exptype2 < 14) {
                            if (i_exptype2 == i2) {
                                break;
                            }
                        } else if (SAX2DTM2.this.m_extendedTypes[i_exptype2].getNodeType() == i2) {
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
                    } else if (SAX2DTM2.this._exptype2(i) == i2) {
                        break;
                    }
                }
            }
            this._currentNode = i;
            if (i == -1) {
                return -1;
            }
            return returnNode(SAX2DTM2.this.makeNodeHandle(i));
        }
    }

    public class FollowingIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        public FollowingIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            int i_firstch2;
            int i_nextsib2;
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                this._startNode = i;
                int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
                int i_type2 = SAX2DTM2.this._type2(iMakeNodeIdentity);
                if ((2 == i_type2 || 13 == i_type2) && -1 != (i_firstch2 = SAX2DTM2.this._firstch2((iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity))))) {
                    this._currentNode = SAX2DTM2.this.makeNodeHandle(i_firstch2);
                    return resetPosition();
                }
                do {
                    i_nextsib2 = SAX2DTM2.this._nextsib2(iMakeNodeIdentity);
                    if (-1 == i_nextsib2) {
                        iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    }
                    if (-1 != i_nextsib2) {
                        break;
                    }
                } while (-1 != iMakeNodeIdentity);
                this._currentNode = SAX2DTM2.this.makeNodeHandle(i_nextsib2);
                return resetPosition();
            }
            return this;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
            while (true) {
                iMakeNodeIdentity++;
                int i_type2 = SAX2DTM2.this._type2(iMakeNodeIdentity);
                if (-1 == i_type2) {
                    this._currentNode = -1;
                    return returnNode(i);
                }
                if (2 != i_type2 && 13 != i_type2) {
                    this._currentNode = SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity);
                    return returnNode(i);
                }
            }
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
            int i_type2;
            int i_type22;
            int i2 = this._nodeType;
            int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(this._currentNode);
            if (i2 >= 14) {
                while (true) {
                    i = iMakeNodeIdentity;
                    while (true) {
                        i++;
                        i_type22 = SAX2DTM2.this._type2(i);
                        if (i_type22 == -1 || (2 != i_type22 && 13 != i_type22)) {
                            break;
                        }
                    }
                    if (i_type22 == -1) {
                        i = -1;
                    }
                    if (iMakeNodeIdentity == -1 || SAX2DTM2.this._exptype2(iMakeNodeIdentity) == i2) {
                        break;
                    }
                    iMakeNodeIdentity = i;
                }
            } else {
                while (true) {
                    i = iMakeNodeIdentity;
                    while (true) {
                        i++;
                        i_type2 = SAX2DTM2.this._type2(i);
                        if (i_type2 == -1 || (2 != i_type2 && 13 != i_type2)) {
                            break;
                        }
                    }
                    if (i_type2 == -1) {
                        i = -1;
                    }
                    if (iMakeNodeIdentity == -1 || SAX2DTM2.this._exptype2(iMakeNodeIdentity) == i2 || SAX2DTM2.this._type2(iMakeNodeIdentity) == i2) {
                        break;
                    }
                    iMakeNodeIdentity = i;
                }
            }
            this._currentNode = SAX2DTM2.this.makeNodeHandle(i);
            if (iMakeNodeIdentity == -1) {
                return -1;
            }
            return returnNode(SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity));
        }
    }

    public class AncestorIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        private static final int m_blocksize = 32;
        int[] m_ancestors;
        int m_ancestorsPos;
        int m_markedPos;
        int m_realStartNode;
        int m_size;

        public AncestorIterator() {
            super();
            this.m_ancestors = new int[32];
            this.m_size = 0;
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
                i = SAX2DTM2.this.getDocument();
            }
            this.m_realStartNode = i;
            if (this._isRestartable) {
                int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
                this.m_size = 0;
                int i2 = -1;
                if (iMakeNodeIdentity == -1) {
                    this._currentNode = -1;
                    this.m_ancestorsPos = 0;
                    return this;
                }
                if (!this._includeSelf) {
                    iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    i = SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity);
                }
                this._startNode = i;
                while (iMakeNodeIdentity != -1) {
                    if (this.m_size >= this.m_ancestors.length) {
                        int[] iArr = new int[this.m_size * 2];
                        System.arraycopy(this.m_ancestors, 0, iArr, 0, this.m_ancestors.length);
                        this.m_ancestors = iArr;
                    }
                    int[] iArr2 = this.m_ancestors;
                    int i3 = this.m_size;
                    this.m_size = i3 + 1;
                    iArr2[i3] = i;
                    iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    i = SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity);
                }
                this.m_ancestorsPos = this.m_size - 1;
                if (this.m_ancestorsPos >= 0) {
                    i2 = this.m_ancestors[this.m_ancestorsPos];
                }
                this._currentNode = i2;
                return resetPosition();
            }
            return this;
        }

        @Override
        public DTMAxisIterator reset() {
            this.m_ancestorsPos = this.m_size - 1;
            this._currentNode = this.m_ancestorsPos >= 0 ? this.m_ancestors[this.m_ancestorsPos] : -1;
            return resetPosition();
        }

        @Override
        public int next() {
            int i = this._currentNode;
            int i2 = this.m_ancestorsPos - 1;
            this.m_ancestorsPos = i2;
            this._currentNode = i2 >= 0 ? this.m_ancestors[this.m_ancestorsPos] : -1;
            return returnNode(i);
        }

        @Override
        public void setMark() {
            this.m_markedPos = this.m_ancestorsPos;
        }

        @Override
        public void gotoMark() {
            this.m_ancestorsPos = this.m_markedPos;
            this._currentNode = this.m_ancestorsPos >= 0 ? this.m_ancestors[this.m_ancestorsPos] : -1;
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
                i = SAX2DTM2.this.getDocument();
            }
            this.m_realStartNode = i;
            if (this._isRestartable) {
                int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
                this.m_size = 0;
                int i2 = -1;
                if (iMakeNodeIdentity == -1) {
                    this._currentNode = -1;
                    this.m_ancestorsPos = 0;
                    return this;
                }
                int i3 = this._nodeType;
                if (!this._includeSelf) {
                    iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    i = SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity);
                }
                this._startNode = i;
                if (i3 >= 14) {
                    while (iMakeNodeIdentity != -1) {
                        if (SAX2DTM2.this._exptype2(iMakeNodeIdentity) == i3) {
                            if (this.m_size >= this.m_ancestors.length) {
                                int[] iArr = new int[this.m_size * 2];
                                System.arraycopy(this.m_ancestors, 0, iArr, 0, this.m_ancestors.length);
                                this.m_ancestors = iArr;
                            }
                            int[] iArr2 = this.m_ancestors;
                            int i4 = this.m_size;
                            this.m_size = i4 + 1;
                            iArr2[i4] = SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity);
                        }
                        iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    }
                } else {
                    while (iMakeNodeIdentity != -1) {
                        int i_exptype2 = SAX2DTM2.this._exptype2(iMakeNodeIdentity);
                        if ((i_exptype2 < 14 && i_exptype2 == i3) || (i_exptype2 >= 14 && SAX2DTM2.this.m_extendedTypes[i_exptype2].getNodeType() == i3)) {
                            if (this.m_size >= this.m_ancestors.length) {
                                int[] iArr3 = new int[this.m_size * 2];
                                System.arraycopy(this.m_ancestors, 0, iArr3, 0, this.m_ancestors.length);
                                this.m_ancestors = iArr3;
                            }
                            int[] iArr4 = this.m_ancestors;
                            int i5 = this.m_size;
                            this.m_size = i5 + 1;
                            iArr4[i5] = SAX2DTM2.this.makeNodeHandle(iMakeNodeIdentity);
                        }
                        iMakeNodeIdentity = SAX2DTM2.this._parent2(iMakeNodeIdentity);
                    }
                }
                this.m_ancestorsPos = this.m_size - 1;
                if (this.m_ancestorsPos >= 0) {
                    i2 = this.m_ancestors[this.m_ancestorsPos];
                }
                this._currentNode = i2;
                return resetPosition();
            }
            return this;
        }

        @Override
        public int getNodeByPosition(int i) {
            if (i > 0 && i <= this.m_size) {
                return this.m_ancestors[i - 1];
            }
            return -1;
        }

        @Override
        public int getLast() {
            return this.m_size;
        }
    }

    public class DescendantIterator extends DTMDefaultBaseIterators.InternalAxisIteratorBase {
        public DescendantIterator() {
            super();
        }

        @Override
        public DTMAxisIterator setStartNode(int i) {
            if (i == 0) {
                i = SAX2DTM2.this.getDocument();
            }
            if (this._isRestartable) {
                int iMakeNodeIdentity = SAX2DTM2.this.makeNodeIdentity(i);
                this._startNode = iMakeNodeIdentity;
                if (this._includeSelf) {
                    iMakeNodeIdentity--;
                }
                this._currentNode = iMakeNodeIdentity;
                return resetPosition();
            }
            return this;
        }

        protected final boolean isDescendant(int i) {
            return SAX2DTM2.this._parent2(i) >= this._startNode || this._startNode == i;
        }

        @Override
        public int next() {
            int nodeType;
            int i = this._startNode;
            if (i == -1) {
                return -1;
            }
            if (this._includeSelf && this._currentNode + 1 == i) {
                SAX2DTM2 sax2dtm2 = SAX2DTM2.this;
                int i2 = this._currentNode + 1;
                this._currentNode = i2;
                return returnNode(sax2dtm2.makeNodeHandle(i2));
            }
            int i3 = this._currentNode;
            if (i != 0) {
                while (true) {
                    i3++;
                    int i_type2 = SAX2DTM2.this._type2(i3);
                    if (-1 != i_type2 && isDescendant(i3)) {
                        if (2 != i_type2 && 3 != i_type2 && 13 != i_type2) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } else {
                while (true) {
                    i3++;
                    int i_exptype2 = SAX2DTM2.this._exptype2(i3);
                    if (-1 == i_exptype2) {
                        this._currentNode = -1;
                        return -1;
                    }
                    if (i_exptype2 != 3 && (nodeType = SAX2DTM2.this.m_extendedTypes[i_exptype2].getNodeType()) != 2 && nodeType != 13) {
                        break;
                    }
                }
            }
            this._currentNode = i3;
            return returnNode(SAX2DTM2.this.makeNodeHandle(i3));
        }

        @Override
        public DTMAxisIterator reset() {
            boolean z = this._isRestartable;
            this._isRestartable = true;
            setStartNode(SAX2DTM2.this.makeNodeHandle(this._startNode));
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
            int i_exptype2;
            int i = this._startNode;
            if (this._startNode == -1) {
                return -1;
            }
            int i2 = this._currentNode;
            int i3 = this._nodeType;
            if (i3 != 1) {
                do {
                    i2++;
                    i_exptype2 = SAX2DTM2.this._exptype2(i2);
                    if (-1 == i_exptype2 || (SAX2DTM2.this._parent2(i2) < i && i != i2)) {
                        this._currentNode = -1;
                        return -1;
                    }
                } while (i_exptype2 != i3);
            } else {
                if (i != 0) {
                    while (true) {
                        i2++;
                        int i_exptype22 = SAX2DTM2.this._exptype2(i2);
                        if (-1 != i_exptype22 && (SAX2DTM2.this._parent2(i2) >= i || i == i2)) {
                            if (i_exptype22 >= 14 && SAX2DTM2.this.m_extendedTypes[i_exptype22].getNodeType() == 1) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    this._currentNode = -1;
                    return -1;
                }
                while (true) {
                    i2++;
                    int i_exptype23 = SAX2DTM2.this._exptype2(i2);
                    if (-1 == i_exptype23) {
                        this._currentNode = -1;
                        return -1;
                    }
                    if (i_exptype23 >= 14 && SAX2DTM2.this.m_extendedTypes[i_exptype23].getNodeType() == 1) {
                        break;
                    }
                }
            }
            this._currentNode = i2;
            return returnNode(SAX2DTM2.this.makeNodeHandle(i2));
        }
    }

    public final class TypedSingletonIterator extends DTMDefaultBaseIterators.SingletonIterator {
        private final int _nodeType;

        public TypedSingletonIterator(int i) {
            super(SAX2DTM2.this);
            this._nodeType = i;
        }

        @Override
        public int next() {
            int i = this._currentNode;
            if (i == -1) {
                return -1;
            }
            this._currentNode = -1;
            if (this._nodeType >= 14) {
                if (SAX2DTM2.this._exptype2(SAX2DTM2.this.makeNodeIdentity(i)) == this._nodeType) {
                    return returnNode(i);
                }
            } else if (SAX2DTM2.this._type2(SAX2DTM2.this.makeNodeIdentity(i)) == this._nodeType) {
                return returnNode(i);
            }
            return -1;
        }
    }

    public SAX2DTM2(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z) {
        this(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, 512, true, true, false);
    }

    public SAX2DTM2(DTMManager dTMManager, Source source, int i, DTMWSFilter dTMWSFilter, XMLStringFactory xMLStringFactory, boolean z, int i2, boolean z2, boolean z3, boolean z4) {
        super(dTMManager, source, i, dTMWSFilter, xMLStringFactory, z, i2, z2, z4);
        this.m_valueIndex = 0;
        this.m_buildIdIndex = true;
        int i3 = 0;
        int i4 = i2;
        while (true) {
            i4 >>>= 1;
            if (i4 == 0) {
                this.m_blocksize = 1 << i3;
                this.m_SHIFT = i3;
                this.m_MASK = this.m_blocksize - 1;
                this.m_buildIdIndex = z3;
                this.m_values = new Vector(32, 512);
                this.m_maxNodeIndex = 65536;
                this.m_exptype_map0 = this.m_exptype.getMap0();
                this.m_nextsib_map0 = this.m_nextsib.getMap0();
                this.m_firstch_map0 = this.m_firstch.getMap0();
                this.m_parent_map0 = this.m_parent.getMap0();
                return;
            }
            i3++;
        }
    }

    @Override
    public final int _exptype(int i) {
        return this.m_exptype.elementAt(i);
    }

    public final int _exptype2(int i) {
        if (i < this.m_blocksize) {
            return this.m_exptype_map0[i];
        }
        return this.m_exptype_map[i >>> this.m_SHIFT][i & this.m_MASK];
    }

    public final int _nextsib2(int i) {
        if (i < this.m_blocksize) {
            return this.m_nextsib_map0[i];
        }
        return this.m_nextsib_map[i >>> this.m_SHIFT][i & this.m_MASK];
    }

    public final int _firstch2(int i) {
        if (i < this.m_blocksize) {
            return this.m_firstch_map0[i];
        }
        return this.m_firstch_map[i >>> this.m_SHIFT][i & this.m_MASK];
    }

    public final int _parent2(int i) {
        if (i < this.m_blocksize) {
            return this.m_parent_map0[i];
        }
        return this.m_parent_map[i >>> this.m_SHIFT][i & this.m_MASK];
    }

    public final int _type2(int i) {
        int i2;
        if (i < this.m_blocksize) {
            i2 = this.m_exptype_map0[i];
        } else {
            i2 = this.m_exptype_map[i >>> this.m_SHIFT][i & this.m_MASK];
        }
        if (-1 == i2) {
            return -1;
        }
        return this.m_extendedTypes[i2].getNodeType();
    }

    public final int getExpandedTypeID2(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return -1;
        }
        if (iMakeNodeIdentity < this.m_blocksize) {
            return this.m_exptype_map0[iMakeNodeIdentity];
        }
        return this.m_exptype_map[iMakeNodeIdentity >>> this.m_SHIFT][iMakeNodeIdentity & this.m_MASK];
    }

    public final int _exptype2Type(int i) {
        if (-1 == i) {
            return -1;
        }
        return this.m_extendedTypes[i].getNodeType();
    }

    @Override
    public int getIdForNamespace(String str) {
        int iIndexOf = this.m_values.indexOf(str);
        if (iIndexOf < 0) {
            this.m_values.addElement(str);
            int i = this.m_valueIndex;
            this.m_valueIndex = i + 1;
            return i;
        }
        return iIndexOf;
    }

    @Override
    public void startElement(String str, String str2, String str3, Attributes attributes) throws SAXException {
        int iStringToIndex;
        int i;
        charactersFlush();
        boolean shouldStripWhitespace = true;
        int expandedTypeID = this.m_expandedNameTable.getExpandedTypeID(str, str2, 1);
        if (str3.length() != str2.length()) {
            iStringToIndex = this.m_valuesOrPrefixes.stringToIndex(str3);
        } else {
            iStringToIndex = 0;
        }
        int iAddNode = addNode(1, expandedTypeID, this.m_parents.peek(), this.m_previous, iStringToIndex, true);
        if (this.m_indexing) {
            indexNode(expandedTypeID, iAddNode);
        }
        this.m_parents.push(iAddNode);
        int size = this.m_prefixMappings.size();
        if (!this.m_pastFirstElement) {
            int expandedTypeID2 = this.m_expandedNameTable.getExpandedTypeID(null, "xml", 13);
            this.m_values.addElement("http://www.w3.org/XML/1998/namespace");
            int i2 = this.m_valueIndex;
            this.m_valueIndex = i2 + 1;
            addNode(13, expandedTypeID2, iAddNode, -1, i2, false);
            this.m_pastFirstElement = true;
        }
        for (int iPeek = this.m_contextIndexes.peek(); iPeek < size; iPeek += 2) {
            String str4 = (String) this.m_prefixMappings.elementAt(iPeek);
            if (str4 != null) {
                String str5 = (String) this.m_prefixMappings.elementAt(iPeek + 1);
                int expandedTypeID3 = this.m_expandedNameTable.getExpandedTypeID(null, str4, 13);
                this.m_values.addElement(str5);
                int i3 = this.m_valueIndex;
                this.m_valueIndex = i3 + 1;
                addNode(13, expandedTypeID3, iAddNode, -1, i3, false);
            }
        }
        int length = attributes.getLength();
        for (int i4 = 0; i4 < length; i4++) {
            String uri = attributes.getURI(i4);
            String qName = attributes.getQName(i4);
            String value = attributes.getValue(i4);
            String localName = attributes.getLocalName(i4);
            if (qName != null && (qName.equals("xmlns") || qName.startsWith(Constants.ATTRNAME_XMLNS))) {
                if (!declAlreadyDeclared(getPrefix(qName, uri))) {
                    i = 13;
                }
            } else {
                if (this.m_buildIdIndex && attributes.getType(i4).equalsIgnoreCase("ID")) {
                    setIDAttribute(value, iAddNode);
                }
                i = 2;
            }
            if (value == null) {
                value = "";
            }
            this.m_values.addElement(value);
            int i5 = this.m_valueIndex;
            this.m_valueIndex = i5 + 1;
            if (localName.length() != qName.length()) {
                int iStringToIndex2 = this.m_valuesOrPrefixes.stringToIndex(qName);
                int size2 = this.m_data.size();
                this.m_data.addElement(iStringToIndex2);
                this.m_data.addElement(i5);
                i5 = -size2;
            }
            addNode(i, this.m_expandedNameTable.getExpandedTypeID(uri, localName, i), iAddNode, -1, i5, false);
        }
        if (this.m_wsfilter != null) {
            short shouldStripSpace = this.m_wsfilter.getShouldStripSpace(makeNodeHandle(iAddNode), this);
            if (3 == shouldStripSpace) {
                shouldStripWhitespace = getShouldStripWhitespace();
            } else if (2 != shouldStripSpace) {
                shouldStripWhitespace = false;
            }
            pushShouldStripWhitespace(shouldStripWhitespace);
        }
        this.m_previous = -1;
        this.m_contextIndexes.push(this.m_prefixMappings.size());
    }

    @Override
    public void endElement(String str, String str2, String str3) throws SAXException {
        charactersFlush();
        this.m_contextIndexes.quickPop(1);
        int iPeek = this.m_contextIndexes.peek();
        if (iPeek != this.m_prefixMappings.size()) {
            this.m_prefixMappings.setSize(iPeek);
        }
        this.m_previous = this.m_parents.pop();
        popShouldStripWhitespace();
    }

    @Override
    public void comment(char[] cArr, int i, int i2) throws SAXException {
        if (this.m_insideDTD) {
            return;
        }
        charactersFlush();
        this.m_values.addElement(new String(cArr, i, i2));
        int i3 = this.m_valueIndex;
        this.m_valueIndex = i3 + 1;
        this.m_previous = addNode(8, 8, this.m_parents.peek(), this.m_previous, i3, false);
    }

    @Override
    public void startDocument() throws SAXException {
        this.m_parents.push(addNode(9, 9, -1, -1, 0, true));
        this.m_previous = -1;
        this.m_contextIndexes.push(this.m_prefixMappings.size());
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        this.m_exptype.addElement(-1);
        this.m_parent.addElement(-1);
        this.m_nextsib.addElement(-1);
        this.m_firstch.addElement(-1);
        this.m_extendedTypes = this.m_expandedNameTable.getExtendedTypes();
        this.m_exptype_map = this.m_exptype.getMap();
        this.m_nextsib_map = this.m_nextsib.getMap();
        this.m_firstch_map = this.m_firstch.getMap();
        this.m_parent_map = this.m_parent.getMap();
    }

    @Override
    protected final int addNode(int i, int i2, int i3, int i4, int i5, boolean z) {
        int i6 = this.m_size;
        this.m_size = i6 + 1;
        if (i6 == this.m_maxNodeIndex) {
            addNewDTMID(i6);
            this.m_maxNodeIndex += 65536;
        }
        this.m_firstch.addElement(-1);
        this.m_nextsib.addElement(-1);
        this.m_parent.addElement(i3);
        this.m_exptype.addElement(i2);
        this.m_dataOrQName.addElement(i5);
        if (this.m_prevsib != null) {
            this.m_prevsib.addElement(i4);
        }
        if (this.m_locator != null && this.m_useSourceLocationProperty) {
            setSourceLocation();
        }
        if (i != 2) {
            if (i == 13) {
                declareNamespaceInContext(i3, i6);
            } else if (-1 != i4) {
                this.m_nextsib.setElementAt(i6, i4);
            } else if (-1 != i3) {
                this.m_firstch.setElementAt(i6, i3);
            }
        }
        return i6;
    }

    @Override
    protected final void charactersFlush() {
        if (this.m_textPendingStart >= 0) {
            int size = this.m_chars.size() - this.m_textPendingStart;
            boolean zIsWhitespace = false;
            if (getShouldStripWhitespace()) {
                zIsWhitespace = this.m_chars.isWhitespace(this.m_textPendingStart, size);
            }
            if (zIsWhitespace) {
                this.m_chars.setLength(this.m_textPendingStart);
            } else if (size > 0) {
                if (size <= TEXT_LENGTH_MAX && this.m_textPendingStart <= TEXT_OFFSET_MAX) {
                    this.m_previous = addNode(this.m_coalescedTextType, 3, this.m_parents.peek(), this.m_previous, size + (this.m_textPendingStart << 10), false);
                } else {
                    this.m_previous = addNode(this.m_coalescedTextType, 3, this.m_parents.peek(), this.m_previous, -this.m_data.size(), false);
                    this.m_data.addElement(this.m_textPendingStart);
                    this.m_data.addElement(size);
                }
            }
            this.m_textPendingStart = -1;
            this.m_coalescedTextType = 3;
            this.m_textType = 3;
        }
    }

    @Override
    public void processingInstruction(String str, String str2) throws SAXException {
        charactersFlush();
        this.m_previous = addNode(7, 7, this.m_parents.peek(), this.m_previous, -this.m_data.size(), false);
        this.m_data.addElement(this.m_valuesOrPrefixes.stringToIndex(str));
        this.m_values.addElement(str2);
        SuballocatedIntVector suballocatedIntVector = this.m_data;
        int i = this.m_valueIndex;
        this.m_valueIndex = i + 1;
        suballocatedIntVector.addElement(i);
    }

    @Override
    public final int getFirstAttribute(int i) {
        int i_type2;
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity != -1 && 1 == _type2(iMakeNodeIdentity)) {
            do {
                iMakeNodeIdentity++;
                i_type2 = _type2(iMakeNodeIdentity);
                if (i_type2 == 2) {
                    return makeNodeHandle(iMakeNodeIdentity);
                }
            } while (13 == i_type2);
        }
        return -1;
    }

    @Override
    protected int getFirstAttributeIdentity(int i) {
        int i_type2;
        if (i != -1 && 1 == _type2(i)) {
            do {
                i++;
                i_type2 = _type2(i);
                if (i_type2 == 2) {
                    return i;
                }
            } while (13 == i_type2);
        }
        return -1;
    }

    @Override
    protected int getNextAttributeIdentity(int i) {
        int i_type2;
        do {
            i++;
            i_type2 = _type2(i);
            if (i_type2 == 2) {
                return i;
            }
        } while (i_type2 == 13);
        return -1;
    }

    @Override
    protected final int getTypedAttribute(int i, int i2) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity != -1 && 1 == _type2(iMakeNodeIdentity)) {
            while (true) {
                iMakeNodeIdentity++;
                int i_exptype2 = _exptype2(iMakeNodeIdentity);
                if (i_exptype2 == -1) {
                    return -1;
                }
                int nodeType = this.m_extendedTypes[i_exptype2].getNodeType();
                if (nodeType == 2) {
                    if (i_exptype2 == i2) {
                        return makeNodeHandle(iMakeNodeIdentity);
                    }
                } else if (13 != nodeType) {
                    break;
                }
            }
        }
        return -1;
    }

    @Override
    public String getLocalName(int i) {
        int i_exptype = _exptype(makeNodeIdentity(i));
        if (i_exptype == 7) {
            return this.m_valuesOrPrefixes.indexToString(this.m_data.elementAt(-_dataOrQName(makeNodeIdentity(i))));
        }
        return this.m_expandedNameTable.getLocalName(i_exptype);
    }

    @Override
    public final String getNodeNameX(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        int i_exptype2 = _exptype2(iMakeNodeIdentity);
        if (i_exptype2 == 7) {
            return this.m_valuesOrPrefixes.indexToString(this.m_data.elementAt(-_dataOrQName(iMakeNodeIdentity)));
        }
        ExtendedType extendedType = this.m_extendedTypes[i_exptype2];
        if (extendedType.getNamespace().length() == 0) {
            return extendedType.getLocalName();
        }
        int iElementAt = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
        if (iElementAt == 0) {
            return extendedType.getLocalName();
        }
        if (iElementAt < 0) {
            iElementAt = this.m_data.elementAt(-iElementAt);
        }
        return this.m_valuesOrPrefixes.indexToString(iElementAt);
    }

    @Override
    public String getNodeName(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        ExtendedType extendedType = this.m_extendedTypes[_exptype2(iMakeNodeIdentity)];
        if (extendedType.getNamespace().length() == 0) {
            int nodeType = extendedType.getNodeType();
            String localName = extendedType.getLocalName();
            if (nodeType == 13) {
                if (localName.length() == 0) {
                    return "xmlns";
                }
                return Constants.ATTRNAME_XMLNS + localName;
            }
            if (nodeType == 7) {
                return this.m_valuesOrPrefixes.indexToString(this.m_data.elementAt(-_dataOrQName(iMakeNodeIdentity)));
            }
            if (localName.length() == 0) {
                return getFixedNames(nodeType);
            }
            return localName;
        }
        int iElementAt = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
        if (iElementAt == 0) {
            return extendedType.getLocalName();
        }
        if (iElementAt < 0) {
            iElementAt = this.m_data.elementAt(-iElementAt);
        }
        return this.m_valuesOrPrefixes.indexToString(iElementAt);
    }

    @Override
    public XMLString getStringValue(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return EMPTY_XML_STR;
        }
        int i_type2 = _type2(iMakeNodeIdentity);
        if (i_type2 == 1 || i_type2 == 9) {
            int i_firstch2 = _firstch2(iMakeNodeIdentity);
            if (-1 != i_firstch2) {
                int iElementAt = 0;
                int iElementAt2 = -1;
                do {
                    int i_exptype2 = _exptype2(i_firstch2);
                    if (i_exptype2 == 3 || i_exptype2 == 4) {
                        int iElementAt3 = this.m_dataOrQName.elementAt(i_firstch2);
                        if (iElementAt3 >= 0) {
                            if (-1 == iElementAt2) {
                                iElementAt2 = iElementAt3 >>> 10;
                            }
                            iElementAt += iElementAt3 & TEXT_LENGTH_MAX;
                        } else {
                            if (-1 == iElementAt2) {
                                iElementAt2 = this.m_data.elementAt(-iElementAt3);
                            }
                            iElementAt += this.m_data.elementAt((-iElementAt3) + 1);
                        }
                    }
                    i_firstch2++;
                } while (_parent2(i_firstch2) >= iMakeNodeIdentity);
                if (iElementAt > 0) {
                    if (this.m_xstrf != null) {
                        return this.m_xstrf.newstr(this.m_chars, iElementAt2, iElementAt);
                    }
                    return new XMLStringDefault(this.m_chars.getString(iElementAt2, iElementAt));
                }
                return EMPTY_XML_STR;
            }
            return EMPTY_XML_STR;
        }
        if (3 == i_type2 || 4 == i_type2) {
            int iElementAt4 = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
            if (iElementAt4 >= 0) {
                if (this.m_xstrf != null) {
                    return this.m_xstrf.newstr(this.m_chars, iElementAt4 >>> 10, iElementAt4 & TEXT_LENGTH_MAX);
                }
                return new XMLStringDefault(this.m_chars.getString(iElementAt4 >>> 10, iElementAt4 & TEXT_LENGTH_MAX));
            }
            if (this.m_xstrf != null) {
                int i2 = -iElementAt4;
                return this.m_xstrf.newstr(this.m_chars, this.m_data.elementAt(i2), this.m_data.elementAt(i2 + 1));
            }
            int i3 = -iElementAt4;
            return new XMLStringDefault(this.m_chars.getString(this.m_data.elementAt(i3), this.m_data.elementAt(i3 + 1)));
        }
        int iElementAt5 = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
        if (iElementAt5 < 0) {
            iElementAt5 = this.m_data.elementAt((-iElementAt5) + 1);
        }
        if (this.m_xstrf != null) {
            return this.m_xstrf.newstr((String) this.m_values.elementAt(iElementAt5));
        }
        return new XMLStringDefault((String) this.m_values.elementAt(iElementAt5));
    }

    public final String getStringValueX(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return "";
        }
        int i_type2 = _type2(iMakeNodeIdentity);
        if (i_type2 == 1 || i_type2 == 9) {
            int i_firstch2 = _firstch2(iMakeNodeIdentity);
            if (-1 != i_firstch2) {
                int iElementAt = 0;
                int iElementAt2 = -1;
                do {
                    int i_exptype2 = _exptype2(i_firstch2);
                    if (i_exptype2 == 3 || i_exptype2 == 4) {
                        int iElementAt3 = this.m_dataOrQName.elementAt(i_firstch2);
                        if (iElementAt3 >= 0) {
                            if (-1 == iElementAt2) {
                                iElementAt2 = iElementAt3 >>> 10;
                            }
                            iElementAt += iElementAt3 & TEXT_LENGTH_MAX;
                        } else {
                            if (-1 == iElementAt2) {
                                iElementAt2 = this.m_data.elementAt(-iElementAt3);
                            }
                            iElementAt += this.m_data.elementAt((-iElementAt3) + 1);
                        }
                    }
                    i_firstch2++;
                } while (_parent2(i_firstch2) >= iMakeNodeIdentity);
                if (iElementAt > 0) {
                    return this.m_chars.getString(iElementAt2, iElementAt);
                }
                return "";
            }
            return "";
        }
        if (3 == i_type2 || 4 == i_type2) {
            int iElementAt4 = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
            if (iElementAt4 >= 0) {
                return this.m_chars.getString(iElementAt4 >>> 10, iElementAt4 & TEXT_LENGTH_MAX);
            }
            int i2 = -iElementAt4;
            return this.m_chars.getString(this.m_data.elementAt(i2), this.m_data.elementAt(i2 + 1));
        }
        int iElementAt5 = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
        if (iElementAt5 < 0) {
            iElementAt5 = this.m_data.elementAt((-iElementAt5) + 1);
        }
        return (String) this.m_values.elementAt(iElementAt5);
    }

    public String getStringValue() {
        int i_firstch2 = _firstch2(0);
        if (i_firstch2 == -1) {
            return "";
        }
        if (_exptype2(i_firstch2) == 3 && _nextsib2(i_firstch2) == -1) {
            int iElementAt = this.m_dataOrQName.elementAt(i_firstch2);
            if (iElementAt >= 0) {
                return this.m_chars.getString(iElementAt >>> 10, iElementAt & TEXT_LENGTH_MAX);
            }
            int i = -iElementAt;
            return this.m_chars.getString(this.m_data.elementAt(i), this.m_data.elementAt(i + 1));
        }
        return getStringValueX(getDocument());
    }

    @Override
    public final void dispatchCharactersEvents(int i, ContentHandler contentHandler, boolean z) throws SAXException {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        if (iMakeNodeIdentity == -1) {
            return;
        }
        int i_type2 = _type2(iMakeNodeIdentity);
        if (i_type2 == 1 || i_type2 == 9) {
            int i_firstch2 = _firstch2(iMakeNodeIdentity);
            if (-1 != i_firstch2) {
                int iElementAt = 0;
                int iElementAt2 = -1;
                do {
                    int i_exptype2 = _exptype2(i_firstch2);
                    if (i_exptype2 == 3 || i_exptype2 == 4) {
                        int iElementAt3 = this.m_dataOrQName.elementAt(i_firstch2);
                        if (iElementAt3 >= 0) {
                            if (-1 == iElementAt2) {
                                iElementAt2 = iElementAt3 >>> 10;
                            }
                            iElementAt += iElementAt3 & TEXT_LENGTH_MAX;
                        } else {
                            if (-1 == iElementAt2) {
                                iElementAt2 = this.m_data.elementAt(-iElementAt3);
                            }
                            iElementAt += this.m_data.elementAt((-iElementAt3) + 1);
                        }
                    }
                    i_firstch2++;
                } while (_parent2(i_firstch2) >= iMakeNodeIdentity);
                if (iElementAt > 0) {
                    if (z) {
                        this.m_chars.sendNormalizedSAXcharacters(contentHandler, iElementAt2, iElementAt);
                        return;
                    } else {
                        this.m_chars.sendSAXcharacters(contentHandler, iElementAt2, iElementAt);
                        return;
                    }
                }
                return;
            }
            return;
        }
        if (3 == i_type2 || 4 == i_type2) {
            int iElementAt4 = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
            if (iElementAt4 >= 0) {
                if (z) {
                    this.m_chars.sendNormalizedSAXcharacters(contentHandler, iElementAt4 >>> 10, iElementAt4 & TEXT_LENGTH_MAX);
                    return;
                } else {
                    this.m_chars.sendSAXcharacters(contentHandler, iElementAt4 >>> 10, iElementAt4 & TEXT_LENGTH_MAX);
                    return;
                }
            }
            if (z) {
                int i2 = -iElementAt4;
                this.m_chars.sendNormalizedSAXcharacters(contentHandler, this.m_data.elementAt(i2), this.m_data.elementAt(i2 + 1));
                return;
            } else {
                int i3 = -iElementAt4;
                this.m_chars.sendSAXcharacters(contentHandler, this.m_data.elementAt(i3), this.m_data.elementAt(i3 + 1));
                return;
            }
        }
        int iElementAt5 = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
        if (iElementAt5 < 0) {
            iElementAt5 = this.m_data.elementAt((-iElementAt5) + 1);
        }
        String str = (String) this.m_values.elementAt(iElementAt5);
        if (z) {
            FastStringBuffer.sendNormalizedSAXcharacters(str.toCharArray(), 0, str.length(), contentHandler);
        } else {
            contentHandler.characters(str.toCharArray(), 0, str.length());
        }
    }

    @Override
    public String getNodeValue(int i) {
        int iMakeNodeIdentity = makeNodeIdentity(i);
        int i_type2 = _type2(iMakeNodeIdentity);
        if (i_type2 == 3 || i_type2 == 4) {
            int i_dataOrQName = _dataOrQName(iMakeNodeIdentity);
            if (i_dataOrQName > 0) {
                return this.m_chars.getString(i_dataOrQName >>> 10, i_dataOrQName & TEXT_LENGTH_MAX);
            }
            int i2 = -i_dataOrQName;
            return this.m_chars.getString(this.m_data.elementAt(i2), this.m_data.elementAt(i2 + 1));
        }
        if (1 == i_type2 || 11 == i_type2 || 9 == i_type2) {
            return null;
        }
        int iElementAt = this.m_dataOrQName.elementAt(iMakeNodeIdentity);
        if (iElementAt < 0) {
            iElementAt = this.m_data.elementAt((-iElementAt) + 1);
        }
        return (String) this.m_values.elementAt(iElementAt);
    }

    protected final void copyTextNode(int i, SerializationHandler serializationHandler) throws SAXException {
        if (i != -1) {
            int iElementAt = this.m_dataOrQName.elementAt(i);
            if (iElementAt >= 0) {
                this.m_chars.sendSAXcharacters(serializationHandler, iElementAt >>> 10, iElementAt & TEXT_LENGTH_MAX);
            } else {
                int i2 = -iElementAt;
                this.m_chars.sendSAXcharacters(serializationHandler, this.m_data.elementAt(i2), this.m_data.elementAt(i2 + 1));
            }
        }
    }

    protected final String copyElement(int i, int i2, SerializationHandler serializationHandler) throws SAXException {
        String strSubstring;
        ExtendedType extendedType = this.m_extendedTypes[i2];
        String namespace = extendedType.getNamespace();
        String localName = extendedType.getLocalName();
        if (namespace.length() == 0) {
            serializationHandler.startElement(localName);
            return localName;
        }
        int iElementAt = this.m_dataOrQName.elementAt(i);
        if (iElementAt == 0) {
            serializationHandler.startElement(localName);
            serializationHandler.namespaceAfterStartElement("", namespace);
            return localName;
        }
        if (iElementAt < 0) {
            iElementAt = this.m_data.elementAt(-iElementAt);
        }
        String strIndexToString = this.m_valuesOrPrefixes.indexToString(iElementAt);
        serializationHandler.startElement(strIndexToString);
        int iIndexOf = strIndexToString.indexOf(58);
        if (iIndexOf > 0) {
            strSubstring = strIndexToString.substring(0, iIndexOf);
        } else {
            strSubstring = null;
        }
        serializationHandler.namespaceAfterStartElement(strSubstring, namespace);
        return strIndexToString;
    }

    protected final void copyNS(int i, SerializationHandler serializationHandler, boolean z) throws SAXException {
        int nextNamespaceNode2;
        if (this.m_namespaceDeclSetElements != null && this.m_namespaceDeclSetElements.size() == 1 && this.m_namespaceDeclSets != null && ((SuballocatedIntVector) this.m_namespaceDeclSets.elementAt(0)).size() == 1) {
            return;
        }
        SuballocatedIntVector suballocatedIntVectorFindNamespaceContext = null;
        if (z) {
            suballocatedIntVectorFindNamespaceContext = findNamespaceContext(i);
            if (suballocatedIntVectorFindNamespaceContext == null || suballocatedIntVectorFindNamespaceContext.size() < 1) {
                return;
            } else {
                nextNamespaceNode2 = makeNodeIdentity(suballocatedIntVectorFindNamespaceContext.elementAt(0));
            }
        } else {
            nextNamespaceNode2 = getNextNamespaceNode2(i);
        }
        int i2 = 1;
        while (nextNamespaceNode2 != -1) {
            String localName = this.m_extendedTypes[_exptype2(nextNamespaceNode2)].getLocalName();
            int iElementAt = this.m_dataOrQName.elementAt(nextNamespaceNode2);
            if (iElementAt < 0) {
                iElementAt = this.m_data.elementAt((-iElementAt) + 1);
            }
            serializationHandler.namespaceAfterStartElement(localName, (String) this.m_values.elementAt(iElementAt));
            if (z) {
                if (i2 < suballocatedIntVectorFindNamespaceContext.size()) {
                    nextNamespaceNode2 = makeNodeIdentity(suballocatedIntVectorFindNamespaceContext.elementAt(i2));
                    i2++;
                } else {
                    return;
                }
            } else {
                nextNamespaceNode2 = getNextNamespaceNode2(nextNamespaceNode2);
            }
        }
    }

    protected final int getNextNamespaceNode2(int i) {
        int i_type2;
        do {
            i++;
            i_type2 = _type2(i);
        } while (i_type2 == 2);
        if (i_type2 == 13) {
            return i;
        }
        return -1;
    }

    protected final void copyAttributes(int i, SerializationHandler serializationHandler) throws SAXException {
        int firstAttributeIdentity = getFirstAttributeIdentity(i);
        while (firstAttributeIdentity != -1) {
            copyAttribute(firstAttributeIdentity, _exptype2(firstAttributeIdentity), serializationHandler);
            firstAttributeIdentity = getNextAttributeIdentity(firstAttributeIdentity);
        }
    }

    protected final void copyAttribute(int i, int i2, SerializationHandler serializationHandler) throws SAXException {
        String strIndexToString;
        ExtendedType extendedType = this.m_extendedTypes[i2];
        String namespace = extendedType.getNamespace();
        String localName = extendedType.getLocalName();
        int i_dataOrQName = _dataOrQName(i);
        String strSubstring = null;
        if (i_dataOrQName <= 0) {
            int i3 = -i_dataOrQName;
            int iElementAt = this.m_data.elementAt(i3);
            i_dataOrQName = this.m_data.elementAt(i3 + 1);
            strIndexToString = this.m_valuesOrPrefixes.indexToString(iElementAt);
            int iIndexOf = strIndexToString.indexOf(58);
            if (iIndexOf > 0) {
                strSubstring = strIndexToString.substring(0, iIndexOf);
            }
        } else {
            strIndexToString = null;
        }
        if (namespace.length() != 0) {
            serializationHandler.namespaceAfterStartElement(strSubstring, namespace);
        }
        if (strSubstring != null) {
            localName = strIndexToString;
        }
        serializationHandler.addAttribute(localName, (String) this.m_values.elementAt(i_dataOrQName));
    }
}
