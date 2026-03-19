package mf.org.apache.xerces.impl.xs.models;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Vector;
import mf.org.apache.xerces.impl.dtd.models.CMNode;
import mf.org.apache.xerces.impl.dtd.models.CMStateSet;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.SubstitutionGroupHandler;
import mf.org.apache.xerces.impl.xs.XMLSchemaException;
import mf.org.apache.xerces.impl.xs.XSConstraints;
import mf.org.apache.xerces.impl.xs.XSElementDecl;
import mf.org.apache.xerces.impl.xs.XSWildcardDecl;
import mf.org.apache.xerces.xni.QName;

public class XSDFACM implements XSCMValidator {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_VALIDATE_CONTENT = false;
    private static long time = 0;
    private boolean fIsCompactedForUPA;
    private int fLeafCount;
    private Object[] fElemMap = null;
    private int[] fElemMapType = null;
    private int[] fElemMapId = null;
    private int fElemMapSize = 0;
    private boolean[] fFinalStateFlags = null;
    private CMStateSet[] fFollowList = null;
    private CMNode fHeadNode = null;
    private XSCMLeaf[] fLeafList = null;
    private int[] fLeafListType = null;
    private int[][] fTransTable = null;
    private Occurence[] fCountingStates = null;
    private int fTransTableSize = 0;

    static final class Occurence {
        final int elemIndex;
        final int maxOccurs;
        final int minOccurs;

        public Occurence(XSCMRepeatingLeaf leaf, int elemIndex) {
            this.minOccurs = leaf.getMinOccurs();
            this.maxOccurs = leaf.getMaxOccurs();
            this.elemIndex = elemIndex;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("minOccurs=");
            sb.append(this.minOccurs);
            sb.append(";maxOccurs=");
            sb.append(this.maxOccurs != -1 ? Integer.toString(this.maxOccurs) : SchemaSymbols.ATTVAL_UNBOUNDED);
            return sb.toString();
        }
    }

    public XSDFACM(CMNode syntaxTree, int leafCount) {
        this.fLeafCount = 0;
        this.fLeafCount = leafCount;
        this.fIsCompactedForUPA = syntaxTree.isCompactedForUPA();
        buildDFA(syntaxTree);
    }

    public boolean isFinalState(int state) {
        if (state < 0) {
            return false;
        }
        return this.fFinalStateFlags[state];
    }

    @Override
    public Object oneTransition(QName curElem, int[] state, SubstitutionGroupHandler subGroupHandler) {
        int curState = state[0];
        if (curState == -1 || curState == -2) {
            if (curState == -1) {
                state[0] = -2;
            }
            return findMatchingDecl(curElem, subGroupHandler);
        }
        int nextState = 0;
        int elemIndex = 0;
        Object matchingDecl = null;
        while (true) {
            if (elemIndex >= this.fElemMapSize) {
                break;
            }
            nextState = this.fTransTable[curState][elemIndex];
            if (nextState != -1) {
                int type = this.fElemMapType[elemIndex];
                if (type == 1) {
                    matchingDecl = subGroupHandler.getMatchingElemDecl(curElem, (XSElementDecl) this.fElemMap[elemIndex]);
                    if (matchingDecl != null) {
                        break;
                    }
                } else if (type == 2 && ((XSWildcardDecl) this.fElemMap[elemIndex]).allowNamespace(curElem.uri)) {
                    matchingDecl = this.fElemMap[elemIndex];
                    break;
                }
            }
            elemIndex++;
        }
        if (elemIndex == this.fElemMapSize) {
            state[1] = state[0];
            state[0] = -1;
            return findMatchingDecl(curElem, subGroupHandler);
        }
        if (this.fCountingStates != null) {
            Occurence o = this.fCountingStates[curState];
            if (o != null) {
                if (curState == nextState) {
                    int i = state[2] + 1;
                    state[2] = i;
                    if (i > o.maxOccurs && o.maxOccurs != -1) {
                        return findMatchingDecl(curElem, state, subGroupHandler, elemIndex);
                    }
                } else {
                    if (state[2] < o.minOccurs) {
                        state[1] = state[0];
                        state[0] = -1;
                        return findMatchingDecl(curElem, subGroupHandler);
                    }
                    Occurence o2 = this.fCountingStates[nextState];
                    if (o2 != null) {
                        state[2] = elemIndex != o2.elemIndex ? 0 : 1;
                    }
                }
            } else {
                Occurence o3 = this.fCountingStates[nextState];
                if (o3 != null) {
                    state[2] = elemIndex != o3.elemIndex ? 0 : 1;
                }
            }
        }
        state[0] = nextState;
        return matchingDecl;
    }

    Object findMatchingDecl(QName curElem, SubstitutionGroupHandler subGroupHandler) {
        for (int elemIndex = 0; elemIndex < this.fElemMapSize; elemIndex++) {
            int type = this.fElemMapType[elemIndex];
            if (type == 1) {
                Object matchingDecl = subGroupHandler.getMatchingElemDecl(curElem, (XSElementDecl) this.fElemMap[elemIndex]);
                if (matchingDecl != null) {
                    return matchingDecl;
                }
            } else if (type == 2 && ((XSWildcardDecl) this.fElemMap[elemIndex]).allowNamespace(curElem.uri)) {
                return this.fElemMap[elemIndex];
            }
        }
        return null;
    }

    Object findMatchingDecl(QName curElem, int[] state, SubstitutionGroupHandler subGroupHandler, int elemIndex) {
        int i = 0;
        int curState = state[0];
        int nextState = 0;
        Object matchingDecl = null;
        while (true) {
            elemIndex++;
            if (elemIndex >= this.fElemMapSize) {
                break;
            }
            nextState = this.fTransTable[curState][elemIndex];
            if (nextState != -1) {
                int type = this.fElemMapType[elemIndex];
                if (type == 1) {
                    matchingDecl = subGroupHandler.getMatchingElemDecl(curElem, (XSElementDecl) this.fElemMap[elemIndex]);
                    if (matchingDecl != null) {
                        break;
                    }
                } else if (type == 2 && ((XSWildcardDecl) this.fElemMap[elemIndex]).allowNamespace(curElem.uri)) {
                    matchingDecl = this.fElemMap[elemIndex];
                    break;
                }
            }
        }
        if (elemIndex == this.fElemMapSize) {
            state[1] = state[0];
            state[0] = -1;
            return findMatchingDecl(curElem, subGroupHandler);
        }
        state[0] = nextState;
        Occurence o = this.fCountingStates[nextState];
        if (o != null) {
            if (elemIndex == o.elemIndex) {
                i = 1;
            }
            state[2] = i;
        }
        return matchingDecl;
    }

    @Override
    public int[] startContentModel() {
        return new int[3];
    }

    @Override
    public boolean endContentModel(int[] state) {
        Occurence o;
        int curState = state[0];
        if (!this.fFinalStateFlags[curState]) {
            return false;
        }
        if (this.fCountingStates != null && (o = this.fCountingStates[curState]) != null && state[2] < o.minOccurs) {
            return false;
        }
        return true;
    }

    private void buildDFA(CMNode syntaxTree) {
        XSCMLeaf nodeEOC;
        int EOCPos;
        int[] transEntry;
        HashMap stateTable;
        int fSortCount;
        int unmarkedState;
        int EOCPos2 = this.fLeafCount;
        int i = this.fLeafCount;
        this.fLeafCount = i + 1;
        XSCMLeaf nodeEOC2 = null;
        XSCMLeaf nodeEOC3 = new XSCMLeaf(1, null, -1, i);
        this.fHeadNode = new XSCMBinOp(102, syntaxTree, nodeEOC3);
        this.fLeafList = new XSCMLeaf[this.fLeafCount];
        this.fLeafListType = new int[this.fLeafCount];
        postTreeBuildInit(this.fHeadNode);
        this.fFollowList = new CMStateSet[this.fLeafCount];
        int index = 0;
        while (index < this.fLeafCount) {
            this.fFollowList[index] = new CMStateSet(this.fLeafCount);
            index++;
            nodeEOC2 = nodeEOC2;
            nodeEOC3 = nodeEOC3;
            EOCPos2 = EOCPos2;
        }
        calcFollowList(this.fHeadNode);
        this.fElemMap = new Object[this.fLeafCount];
        this.fElemMapType = new int[this.fLeafCount];
        this.fElemMapId = new int[this.fLeafCount];
        boolean z = false;
        this.fElemMapSize = 0;
        int outIndex = 0;
        Occurence[] elemOccurenceMap = null;
        while (outIndex < this.fLeafCount) {
            int EOCPos3 = EOCPos2;
            XSCMLeaf nodeEOC4 = nodeEOC3;
            boolean z2 = z;
            this.fElemMap[outIndex] = null;
            int inIndex = 0;
            int id = this.fLeafList[outIndex].getParticleId();
            while (inIndex < this.fElemMapSize && id != this.fElemMapId[inIndex]) {
                inIndex++;
            }
            if (inIndex == this.fElemMapSize) {
                XSCMRepeatingLeaf xSCMRepeatingLeaf = this.fLeafList[outIndex];
                this.fElemMap[this.fElemMapSize] = xSCMRepeatingLeaf.getLeaf();
                if (xSCMRepeatingLeaf instanceof XSCMRepeatingLeaf) {
                    if (elemOccurenceMap == null) {
                        elemOccurenceMap = new Occurence[this.fLeafCount];
                    }
                    elemOccurenceMap[this.fElemMapSize] = new Occurence(xSCMRepeatingLeaf, this.fElemMapSize);
                }
                this.fElemMapType[this.fElemMapSize] = this.fLeafListType[outIndex];
                this.fElemMapId[this.fElemMapSize] = id;
                this.fElemMapSize++;
            }
            outIndex++;
            z = z2;
            nodeEOC3 = nodeEOC4;
            EOCPos2 = EOCPos3;
        }
        this.fElemMapSize--;
        int[] fLeafSorter = new int[this.fLeafCount + this.fElemMapSize];
        int fSortCount2 = 0;
        int elemIndex = 0;
        while (elemIndex < this.fElemMapSize) {
            int EOCPos4 = EOCPos2;
            XSCMLeaf nodeEOC5 = nodeEOC3;
            boolean z3 = z;
            int fSortCount3 = fSortCount2;
            int id2 = this.fElemMapId[elemIndex];
            for (int leafIndex = 0; leafIndex < this.fLeafCount; leafIndex++) {
                if (id2 == this.fLeafList[leafIndex].getParticleId()) {
                    fLeafSorter[fSortCount3] = leafIndex;
                    fSortCount3++;
                }
            }
            fSortCount2 = fSortCount3 + 1;
            fLeafSorter[fSortCount3] = -1;
            elemIndex++;
            z = z3;
            nodeEOC3 = nodeEOC5;
            EOCPos2 = EOCPos4;
        }
        int elemIndex2 = this.fLeafCount;
        int curArraySize = elemIndex2 * 4;
        CMStateSet[] statesToDo = new CMStateSet[curArraySize];
        this.fFinalStateFlags = new boolean[curArraySize];
        this.fTransTable = new int[curArraySize][];
        CMStateSet setT = this.fHeadNode.firstPos();
        int unmarkedState2 = 0;
        this.fTransTable[0] = makeDefStateList();
        statesToDo[0] = setT;
        int curState = 0 + 1;
        HashMap stateTable2 = new HashMap();
        while (unmarkedState2 < curState) {
            XSCMLeaf nodeEOC6 = nodeEOC3;
            CMStateSet setT2 = statesToDo[unmarkedState2];
            int[] transEntry2 = this.fTransTable[unmarkedState2];
            this.fFinalStateFlags[unmarkedState2] = setT2.getBit(EOCPos2);
            unmarkedState2++;
            int curArraySize2 = curArraySize;
            int sorterIndex = 0;
            CMStateSet newSet = null;
            int elemIndex3 = 0;
            while (elemIndex3 < this.fElemMapSize) {
                if (newSet == null) {
                    EOCPos = EOCPos2;
                    int EOCPos5 = this.fLeafCount;
                    newSet = new CMStateSet(EOCPos5);
                } else {
                    EOCPos = EOCPos2;
                    newSet.zeroBits();
                }
                CMStateSet newSet2 = newSet;
                int leafIndex2 = fLeafSorter[sorterIndex];
                sorterIndex++;
                while (leafIndex2 != -1) {
                    int[] transEntry3 = transEntry2;
                    HashMap stateTable3 = stateTable2;
                    int fSortCount4 = fSortCount2;
                    int unmarkedState3 = unmarkedState2;
                    int curArraySize3 = curArraySize2;
                    if (setT2.getBit(leafIndex2)) {
                        newSet2.union(this.fFollowList[leafIndex2]);
                    }
                    leafIndex2 = fLeafSorter[sorterIndex];
                    sorterIndex++;
                    curArraySize2 = curArraySize3;
                    transEntry2 = transEntry3;
                    stateTable2 = stateTable3;
                    unmarkedState2 = unmarkedState3;
                    fSortCount2 = fSortCount4;
                }
                if (newSet2.isEmpty()) {
                    transEntry = transEntry2;
                    stateTable = stateTable2;
                    fSortCount = fSortCount2;
                    unmarkedState = unmarkedState2;
                    newSet = newSet2;
                } else {
                    Integer stateObj = (Integer) stateTable2.get(newSet2);
                    int stateIndex = stateObj == null ? curState : stateObj.intValue();
                    if (stateIndex == curState) {
                        statesToDo[curState] = newSet2;
                        this.fTransTable[curState] = makeDefStateList();
                        stateTable2.put(newSet2, new Integer(curState));
                        curState++;
                        newSet2 = null;
                    }
                    transEntry2[elemIndex3] = stateIndex;
                    int curArraySize4 = curArraySize2;
                    if (curState == curArraySize4) {
                        CMStateSet newSet3 = newSet2;
                        transEntry = transEntry2;
                        int newSize = (int) (((double) curArraySize4) * 1.5d);
                        CMStateSet[] newToDo = new CMStateSet[newSize];
                        boolean[] newFinalFlags = new boolean[newSize];
                        stateTable = stateTable2;
                        int[][] newTransTable = new int[newSize][];
                        unmarkedState = unmarkedState2;
                        System.arraycopy(statesToDo, 0, newToDo, 0, curArraySize4);
                        fSortCount = fSortCount2;
                        System.arraycopy(this.fFinalStateFlags, 0, newFinalFlags, 0, curArraySize4);
                        System.arraycopy(this.fTransTable, 0, newTransTable, 0, curArraySize4);
                        curArraySize2 = newSize;
                        statesToDo = newToDo;
                        this.fFinalStateFlags = newFinalFlags;
                        this.fTransTable = newTransTable;
                        newSet = newSet3;
                    } else {
                        transEntry = transEntry2;
                        stateTable = stateTable2;
                        fSortCount = fSortCount2;
                        unmarkedState = unmarkedState2;
                        curArraySize2 = curArraySize4;
                        newSet = newSet2;
                    }
                }
                elemIndex3++;
                EOCPos2 = EOCPos;
                transEntry2 = transEntry;
                stateTable2 = stateTable;
                unmarkedState2 = unmarkedState;
                fSortCount2 = fSortCount;
            }
            nodeEOC3 = nodeEOC6;
            curArraySize = curArraySize2;
        }
        if (elemOccurenceMap != null) {
            this.fCountingStates = new Occurence[curState];
            int i2 = 0;
            while (i2 < curState) {
                int[] transitions = this.fTransTable[i2];
                int j = 0;
                while (true) {
                    nodeEOC = nodeEOC3;
                    if (j < transitions.length) {
                        if (i2 != transitions[j]) {
                            j++;
                            nodeEOC3 = nodeEOC;
                        } else {
                            this.fCountingStates[i2] = elemOccurenceMap[j];
                            break;
                        }
                    }
                }
                i2++;
                nodeEOC3 = nodeEOC;
            }
        }
        this.fHeadNode = null;
        this.fLeafList = null;
        this.fFollowList = null;
        this.fLeafListType = null;
        this.fElemMapId = null;
    }

    private void calcFollowList(CMNode nodeCur) {
        if (nodeCur.type() == 101) {
            calcFollowList(((XSCMBinOp) nodeCur).getLeft());
            calcFollowList(((XSCMBinOp) nodeCur).getRight());
            return;
        }
        if (nodeCur.type() == 102) {
            calcFollowList(((XSCMBinOp) nodeCur).getLeft());
            calcFollowList(((XSCMBinOp) nodeCur).getRight());
            CMStateSet last = ((XSCMBinOp) nodeCur).getLeft().lastPos();
            CMStateSet first = ((XSCMBinOp) nodeCur).getRight().firstPos();
            for (int index = 0; index < this.fLeafCount; index++) {
                if (last.getBit(index)) {
                    this.fFollowList[index].union(first);
                }
            }
            return;
        }
        if (nodeCur.type() == 4 || nodeCur.type() == 6) {
            calcFollowList(((XSCMUniOp) nodeCur).getChild());
            CMStateSet first2 = nodeCur.firstPos();
            CMStateSet last2 = nodeCur.lastPos();
            for (int index2 = 0; index2 < this.fLeafCount; index2++) {
                if (last2.getBit(index2)) {
                    this.fFollowList[index2].union(first2);
                }
            }
            return;
        }
        if (nodeCur.type() == 5) {
            calcFollowList(((XSCMUniOp) nodeCur).getChild());
        }
    }

    private void dumpTree(CMNode nodeCur, int level) {
        for (int index = 0; index < level; index++) {
            System.out.print("   ");
        }
        int type = nodeCur.type();
        switch (type) {
            case 1:
                System.out.print("Leaf: (pos=" + ((XSCMLeaf) nodeCur).getPosition() + "), (elemIndex=" + ((XSCMLeaf) nodeCur).getLeaf() + ") ");
                if (nodeCur.isNullable()) {
                    System.out.print(" Nullable ");
                }
                System.out.print("firstPos=");
                System.out.print(nodeCur.firstPos().toString());
                System.out.print(" lastPos=");
                System.out.println(nodeCur.lastPos().toString());
                return;
            case 2:
                System.out.print("Any Node: ");
                System.out.print("firstPos=");
                System.out.print(nodeCur.firstPos().toString());
                System.out.print(" lastPos=");
                System.out.println(nodeCur.lastPos().toString());
                return;
            case 4:
            case 5:
            case 6:
                System.out.print("Rep Node ");
                if (nodeCur.isNullable()) {
                    System.out.print("Nullable ");
                }
                System.out.print("firstPos=");
                System.out.print(nodeCur.firstPos().toString());
                System.out.print(" lastPos=");
                System.out.println(nodeCur.lastPos().toString());
                dumpTree(((XSCMUniOp) nodeCur).getChild(), level + 1);
                return;
            case 101:
            case 102:
                if (type == 101) {
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
                dumpTree(((XSCMBinOp) nodeCur).getLeft(), level + 1);
                dumpTree(((XSCMBinOp) nodeCur).getRight(), level + 1);
                return;
            default:
                throw new RuntimeException("ImplementationMessages.VAL_NIICM");
        }
    }

    private int[] makeDefStateList() {
        int[] retArray = new int[this.fElemMapSize];
        for (int index = 0; index < this.fElemMapSize; index++) {
            retArray[index] = -1;
        }
        return retArray;
    }

    private void postTreeBuildInit(CMNode nodeCur) throws RuntimeException {
        nodeCur.setMaxStates(this.fLeafCount);
        if (nodeCur.type() == 2) {
            XSCMLeaf leaf = (XSCMLeaf) nodeCur;
            int pos = leaf.getPosition();
            this.fLeafList[pos] = leaf;
            this.fLeafListType[pos] = 2;
            return;
        }
        if (nodeCur.type() == 101 || nodeCur.type() == 102) {
            postTreeBuildInit(((XSCMBinOp) nodeCur).getLeft());
            postTreeBuildInit(((XSCMBinOp) nodeCur).getRight());
            return;
        }
        if (nodeCur.type() == 4 || nodeCur.type() == 6 || nodeCur.type() == 5) {
            postTreeBuildInit(((XSCMUniOp) nodeCur).getChild());
        } else {
            if (nodeCur.type() == 1) {
                XSCMLeaf leaf2 = (XSCMLeaf) nodeCur;
                int pos2 = leaf2.getPosition();
                this.fLeafList[pos2] = leaf2;
                this.fLeafListType[pos2] = 1;
                return;
            }
            throw new RuntimeException("ImplementationMessages.VAL_NIICM");
        }
    }

    @Override
    public boolean checkUniqueParticleAttribution(SubstitutionGroupHandler subGroupHandler) throws XMLSchemaException {
        Occurence o;
        byte[][] conflictTable = (byte[][]) Array.newInstance((Class<?>) byte.class, this.fElemMapSize, this.fElemMapSize);
        int i = 0;
        while (i < this.fTransTable.length && this.fTransTable[i] != null) {
            for (int j = 0; j < this.fElemMapSize; j++) {
                for (int k = j + 1; k < this.fElemMapSize; k++) {
                    if (this.fTransTable[i][j] != -1 && this.fTransTable[i][k] != -1 && conflictTable[j][k] == 0) {
                        if (XSConstraints.overlapUPA(this.fElemMap[j], this.fElemMap[k], subGroupHandler)) {
                            if (this.fCountingStates != null && (o = this.fCountingStates[i]) != null) {
                                if (((this.fTransTable[i][j] == i) ^ (this.fTransTable[i][k] == i)) && o.minOccurs == o.maxOccurs) {
                                    conflictTable[j][k] = -1;
                                }
                            } else {
                                conflictTable[j][k] = 1;
                            }
                        } else {
                            conflictTable[j][k] = -1;
                        }
                    }
                }
            }
            i++;
        }
        for (int i2 = 0; i2 < this.fElemMapSize; i2++) {
            for (int j2 = 0; j2 < this.fElemMapSize; j2++) {
                if (conflictTable[i2][j2] == 1) {
                    throw new XMLSchemaException("cos-nonambig", new Object[]{this.fElemMap[i2].toString(), this.fElemMap[j2].toString()});
                }
            }
        }
        for (int i3 = 0; i3 < this.fElemMapSize; i3++) {
            if (this.fElemMapType[i3] == 2) {
                XSWildcardDecl wildcard = (XSWildcardDecl) this.fElemMap[i3];
                if (wildcard.fType == 3 || wildcard.fType == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Vector whatCanGoHere(int[] state) {
        int curState = state[0];
        if (curState < 0) {
            curState = state[1];
        }
        Occurence o = this.fCountingStates != null ? this.fCountingStates[curState] : null;
        int count = state[2];
        Vector ret = new Vector();
        for (int elemIndex = 0; elemIndex < this.fElemMapSize; elemIndex++) {
            int nextState = this.fTransTable[curState][elemIndex];
            if (nextState != -1) {
                if (o != null) {
                    if (curState == nextState) {
                        if (count < o.maxOccurs || o.maxOccurs == -1) {
                        }
                    } else if (count < o.minOccurs) {
                    }
                } else {
                    ret.addElement(this.fElemMap[elemIndex]);
                }
            }
        }
        return ret;
    }

    @Override
    public int[] occurenceInfo(int[] state) {
        if (this.fCountingStates != null) {
            int curState = state[0];
            if (curState < 0) {
                curState = state[1];
            }
            Occurence o = this.fCountingStates[curState];
            if (o != null) {
                int[] occurenceInfo = {o.minOccurs, o.maxOccurs, state[2], o.elemIndex};
                return occurenceInfo;
            }
            return null;
        }
        return null;
    }

    @Override
    public String getTermName(int termId) {
        Object term = this.fElemMap[termId];
        if (term != null) {
            return term.toString();
        }
        return null;
    }

    @Override
    public boolean isCompactedForUPA() {
        return this.fIsCompactedForUPA;
    }
}
