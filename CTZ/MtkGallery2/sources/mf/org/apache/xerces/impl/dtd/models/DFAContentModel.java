package mf.org.apache.xerces.impl.dtd.models;

import java.util.HashMap;
import mf.org.apache.xerces.xni.QName;

public class DFAContentModel implements ContentModelValidator {
    private static final boolean DEBUG_VALIDATE_CONTENT = false;
    private static String fEOCString;
    private static String fEpsilonString;
    private int fLeafCount;
    private boolean fMixed;
    private QName[] fElemMap = null;
    private int[] fElemMapType = null;
    private int fElemMapSize = 0;
    private int fEOCPos = 0;
    private boolean[] fFinalStateFlags = null;
    private CMStateSet[] fFollowList = null;
    private CMNode fHeadNode = null;
    private CMLeaf[] fLeafList = null;
    private int[] fLeafListType = null;
    private int[][] fTransTable = null;
    private int fTransTableSize = 0;
    private boolean fEmptyContentIsValid = false;
    private final QName fQName = new QName();

    static {
        fEpsilonString = "<<CMNODE_EPSILON>>";
        fEOCString = "<<CMNODE_EOC>>";
        fEpsilonString = fEpsilonString.intern();
        fEOCString = fEOCString.intern();
    }

    public DFAContentModel(CMNode syntaxTree, int leafCount, boolean mixed) {
        this.fLeafCount = 0;
        this.fLeafCount = leafCount;
        this.fMixed = mixed;
        buildDFA(syntaxTree);
    }

    @Override
    public int validate(QName[] children, int offset, int length) {
        if (length == 0) {
            return this.fEmptyContentIsValid ? -1 : 0;
        }
        int curState = 0;
        for (int childIndex = 0; childIndex < length; childIndex++) {
            QName curElem = children[offset + childIndex];
            if (!this.fMixed || curElem.localpart != null) {
                int elemIndex = 0;
                while (elemIndex < this.fElemMapSize) {
                    int type = this.fElemMapType[elemIndex] & 15;
                    if (type == 0) {
                        if (this.fElemMap[elemIndex].rawname == curElem.rawname) {
                            break;
                        }
                        elemIndex++;
                    } else if (type == 6) {
                        String uri = this.fElemMap[elemIndex].uri;
                        if (uri == null || uri == curElem.uri) {
                            break;
                        }
                        elemIndex++;
                    } else if (type == 8) {
                        if (curElem.uri == null) {
                            break;
                        }
                        elemIndex++;
                    } else {
                        if (type == 7 && this.fElemMap[elemIndex].uri != curElem.uri) {
                            break;
                        }
                        elemIndex++;
                    }
                }
                if (elemIndex == this.fElemMapSize) {
                    return childIndex;
                }
                curState = this.fTransTable[curState][elemIndex];
                if (curState == -1) {
                    return childIndex;
                }
            }
        }
        if (this.fFinalStateFlags[curState]) {
            return -1;
        }
        return length;
    }

    private void buildDFA(CMNode cMNode) {
        int i;
        CMLeaf cMLeaf;
        int i2;
        int i3;
        boolean z = false;
        this.fQName.setValues(null, fEOCString, fEOCString, null);
        CMLeaf cMLeaf2 = new CMLeaf(this.fQName);
        this.fHeadNode = new CMBinOp(5, cMNode, cMLeaf2);
        this.fEOCPos = this.fLeafCount;
        int i4 = this.fLeafCount;
        this.fLeafCount = i4 + 1;
        cMLeaf2.setPosition(i4);
        this.fLeafList = new CMLeaf[this.fLeafCount];
        this.fLeafListType = new int[this.fLeafCount];
        int i5 = 0;
        postTreeBuildInit(this.fHeadNode, 0);
        this.fFollowList = new CMStateSet[this.fLeafCount];
        int i6 = 0;
        while (i6 < this.fLeafCount) {
            this.fFollowList[i6] = new CMStateSet(this.fLeafCount);
            i6++;
            i5 = i5;
            cMLeaf2 = cMLeaf2;
            z = false;
        }
        calcFollowList(this.fHeadNode);
        this.fElemMap = new QName[this.fLeafCount];
        this.fElemMapType = new int[this.fLeafCount];
        this.fElemMapSize = i5;
        int i7 = 0;
        boolean z2 = z;
        while (i7 < this.fLeafCount) {
            CMLeaf cMLeaf3 = cMLeaf2;
            int i8 = i5;
            this.fElemMap[i7] = new QName();
            QName element = this.fLeafList[i7].getElement();
            int i9 = 0;
            while (i9 < this.fElemMapSize && this.fElemMap[i9].rawname != element.rawname) {
                i9++;
            }
            if (i9 == this.fElemMapSize) {
                this.fElemMap[this.fElemMapSize].setValues(element);
                this.fElemMapType[this.fElemMapSize] = this.fLeafListType[i7];
                this.fElemMapSize++;
            }
            i7++;
            i5 = i8;
            cMLeaf2 = cMLeaf3;
            z2 = false;
        }
        int[] iArr = new int[this.fLeafCount + this.fElemMapSize];
        int i10 = 0;
        int i11 = 0;
        boolean z3 = z2;
        while (i11 < this.fElemMapSize) {
            CMLeaf cMLeaf4 = cMLeaf2;
            int i12 = i5;
            int i13 = i10;
            for (int i14 = 0; i14 < this.fLeafCount; i14++) {
                if (this.fLeafList[i14].getElement().rawname == this.fElemMap[i11].rawname) {
                    iArr[i13] = i14;
                    i13++;
                }
            }
            i10 = i13 + 1;
            iArr[i13] = -1;
            i11++;
            i5 = i12;
            cMLeaf2 = cMLeaf4;
            z3 = false;
        }
        int i15 = this.fLeafCount * 4;
        CMStateSet[] cMStateSetArr = new CMStateSet[i15];
        this.fFinalStateFlags = new boolean[i15];
        this.fTransTable = new int[i15][];
        CMStateSet cMStateSetFirstPos = this.fHeadNode.firstPos();
        int i16 = 0;
        this.fTransTable[0] = makeDefStateList();
        cMStateSetArr[0] = cMStateSetFirstPos;
        HashMap map = new HashMap();
        ?? r4 = z3;
        for (int i17 = 0 + 1; i16 < i17; i17 = i) {
            CMStateSet cMStateSet = cMStateSetArr[i16];
            int[] iArr2 = this.fTransTable[i16];
            this.fFinalStateFlags[i16] = cMStateSet.getBit(this.fEOCPos);
            i16++;
            int i18 = 0;
            i = i17;
            int i19 = i15;
            CMStateSet cMStateSet2 = null;
            int i20 = 0;
            while (i20 < this.fElemMapSize) {
                if (cMStateSet2 == null) {
                    cMLeaf = cMLeaf2;
                    cMStateSet2 = new CMStateSet(this.fLeafCount);
                } else {
                    cMLeaf = cMLeaf2;
                    cMStateSet2.zeroBits();
                }
                CMStateSet cMStateSet3 = cMStateSet2;
                int i21 = iArr[i18];
                i18++;
                while (i21 != -1) {
                    int i22 = i10;
                    int i23 = i16;
                    if (cMStateSet.getBit(i21)) {
                        cMStateSet3.union(this.fFollowList[i21]);
                    }
                    i21 = iArr[i18];
                    i18++;
                    i10 = i22;
                    i16 = i23;
                }
                if (cMStateSet3.isEmpty()) {
                    i2 = i10;
                    i3 = i16;
                    cMStateSet2 = cMStateSet3;
                } else {
                    Integer num = (Integer) map.get(cMStateSet3);
                    int iIntValue = num == null ? i : num.intValue();
                    if (iIntValue == i) {
                        cMStateSetArr[i] = cMStateSet3;
                        i2 = i10;
                        this.fTransTable[i] = makeDefStateList();
                        map.put(cMStateSet3, new Integer(i));
                        i++;
                        cMStateSet3 = null;
                    } else {
                        i2 = i10;
                    }
                    iArr2[i20] = iIntValue;
                    if (i == i19) {
                        int i24 = (int) (((double) i19) * 1.5d);
                        CMStateSet[] cMStateSetArr2 = new CMStateSet[i24];
                        CMStateSet cMStateSet4 = cMStateSet3;
                        boolean[] zArr = new boolean[i24];
                        int i25 = i;
                        int[][] iArr3 = new int[i24][];
                        i3 = i16;
                        System.arraycopy(cMStateSetArr, 0, cMStateSetArr2, 0, i19);
                        System.arraycopy(this.fFinalStateFlags, 0, zArr, 0, i19);
                        System.arraycopy(this.fTransTable, 0, iArr3, 0, i19);
                        i19 = i24;
                        cMStateSetArr = cMStateSetArr2;
                        this.fFinalStateFlags = zArr;
                        this.fTransTable = iArr3;
                        cMStateSet2 = cMStateSet4;
                        i = i25;
                    } else {
                        i3 = i16;
                        cMStateSet2 = cMStateSet3;
                    }
                }
                i20++;
                cMLeaf2 = cMLeaf;
                i10 = i2;
                i16 = i3;
            }
            i15 = i19;
            r4 = 0;
        }
        this.fEmptyContentIsValid = ((CMBinOp) this.fHeadNode).getLeft().isNullable();
        this.fHeadNode = r4;
        this.fLeafList = r4;
        this.fFollowList = r4;
    }

    private void calcFollowList(CMNode nodeCur) {
        if (nodeCur.type() == 4) {
            calcFollowList(((CMBinOp) nodeCur).getLeft());
            calcFollowList(((CMBinOp) nodeCur).getRight());
            return;
        }
        if (nodeCur.type() == 5) {
            calcFollowList(((CMBinOp) nodeCur).getLeft());
            calcFollowList(((CMBinOp) nodeCur).getRight());
            CMStateSet last = ((CMBinOp) nodeCur).getLeft().lastPos();
            CMStateSet first = ((CMBinOp) nodeCur).getRight().firstPos();
            for (int index = 0; index < this.fLeafCount; index++) {
                if (last.getBit(index)) {
                    this.fFollowList[index].union(first);
                }
            }
            return;
        }
        if (nodeCur.type() == 2 || nodeCur.type() == 3) {
            calcFollowList(((CMUniOp) nodeCur).getChild());
            CMStateSet first2 = nodeCur.firstPos();
            CMStateSet last2 = nodeCur.lastPos();
            for (int index2 = 0; index2 < this.fLeafCount; index2++) {
                if (last2.getBit(index2)) {
                    this.fFollowList[index2].union(first2);
                }
            }
            return;
        }
        if (nodeCur.type() == 1) {
            calcFollowList(((CMUniOp) nodeCur).getChild());
        }
    }

    private void dumpTree(CMNode nodeCur, int level) {
        for (int index = 0; index < level; index++) {
            System.out.print("   ");
        }
        int type = nodeCur.type();
        if (type == 4 || type == 5) {
            if (type == 4) {
                System.out.print("Choice Node ");
            } else {
                System.out.print("Seq Node ");
            }
            if (nodeCur.isNullable()) {
                System.out.print("Nullable ");
            }
            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());
            dumpTree(((CMBinOp) nodeCur).getLeft(), level + 1);
            dumpTree(((CMBinOp) nodeCur).getRight(), level + 1);
            return;
        }
        if (nodeCur.type() == 2) {
            System.out.print("Rep Node ");
            if (nodeCur.isNullable()) {
                System.out.print("Nullable ");
            }
            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());
            dumpTree(((CMUniOp) nodeCur).getChild(), level + 1);
            return;
        }
        if (nodeCur.type() == 0) {
            System.out.print("Leaf: (pos=" + ((CMLeaf) nodeCur).getPosition() + "), " + ((CMLeaf) nodeCur).getElement() + "(elemIndex=" + ((CMLeaf) nodeCur).getElement() + ") ");
            if (nodeCur.isNullable()) {
                System.out.print(" Nullable ");
            }
            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());
            return;
        }
        throw new RuntimeException("ImplementationMessages.VAL_NIICM");
    }

    private int[] makeDefStateList() {
        int[] retArray = new int[this.fElemMapSize];
        for (int index = 0; index < this.fElemMapSize; index++) {
            retArray[index] = -1;
        }
        return retArray;
    }

    private int postTreeBuildInit(CMNode nodeCur, int curIndex) {
        nodeCur.setMaxStates(this.fLeafCount);
        if ((nodeCur.type() & 15) == 6 || (nodeCur.type() & 15) == 8 || (nodeCur.type() & 15) == 7) {
            QName qname = new QName(null, null, null, ((CMAny) nodeCur).getURI());
            this.fLeafList[curIndex] = new CMLeaf(qname, ((CMAny) nodeCur).getPosition());
            this.fLeafListType[curIndex] = nodeCur.type();
            return curIndex + 1;
        }
        if (nodeCur.type() == 4 || nodeCur.type() == 5) {
            return postTreeBuildInit(((CMBinOp) nodeCur).getRight(), postTreeBuildInit(((CMBinOp) nodeCur).getLeft(), curIndex));
        }
        if (nodeCur.type() == 2 || nodeCur.type() == 3 || nodeCur.type() == 1) {
            return postTreeBuildInit(((CMUniOp) nodeCur).getChild(), curIndex);
        }
        if (nodeCur.type() == 0) {
            QName node = ((CMLeaf) nodeCur).getElement();
            if (node.localpart != fEpsilonString) {
                this.fLeafList[curIndex] = (CMLeaf) nodeCur;
                this.fLeafListType[curIndex] = 0;
                return curIndex + 1;
            }
            return curIndex;
        }
        throw new RuntimeException("ImplementationMessages.VAL_NIICM: type=" + nodeCur.type());
    }
}
