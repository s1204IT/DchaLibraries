package android.icu.text;

import android.icu.impl.Assert;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class RBBITableBuilder {
    private List<RBBIStateDescriptor> fDStates = new ArrayList();
    private RBBIRuleBuilder fRB;
    private int fRootIx;

    static class RBBIStateDescriptor {
        int fAccepting;
        int[] fDtran;
        int fLookAhead;
        boolean fMarked;
        int fTagsIdx;
        SortedSet<Integer> fTagVals = new TreeSet();
        Set<RBBINode> fPositions = new HashSet();

        RBBIStateDescriptor(int maxInputSymbol) {
            this.fDtran = new int[maxInputSymbol + 1];
        }
    }

    RBBITableBuilder(RBBIRuleBuilder rb, int rootNodeIx) {
        this.fRootIx = rootNodeIx;
        this.fRB = rb;
    }

    void build() {
        if (this.fRB.fTreeRoots[this.fRootIx] == null) {
            return;
        }
        this.fRB.fTreeRoots[this.fRootIx] = this.fRB.fTreeRoots[this.fRootIx].flattenVariables();
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("ftree") >= 0) {
            System.out.println("Parse tree after flattening variable references.");
            this.fRB.fTreeRoots[this.fRootIx].printTree(true);
        }
        if (this.fRB.fSetBuilder.sawBOF()) {
            RBBINode bofTop = new RBBINode(8);
            RBBINode bofLeaf = new RBBINode(3);
            bofTop.fLeftChild = bofLeaf;
            bofTop.fRightChild = this.fRB.fTreeRoots[this.fRootIx];
            bofLeaf.fParent = bofTop;
            bofLeaf.fVal = 2;
            this.fRB.fTreeRoots[this.fRootIx] = bofTop;
        }
        RBBINode cn = new RBBINode(8);
        cn.fLeftChild = this.fRB.fTreeRoots[this.fRootIx];
        this.fRB.fTreeRoots[this.fRootIx].fParent = cn;
        cn.fRightChild = new RBBINode(6);
        cn.fRightChild.fParent = cn;
        this.fRB.fTreeRoots[this.fRootIx] = cn;
        this.fRB.fTreeRoots[this.fRootIx].flattenSets();
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("stree") >= 0) {
            System.out.println("Parse tree after flattening Unicode Set references.");
            this.fRB.fTreeRoots[this.fRootIx].printTree(true);
        }
        calcNullable(this.fRB.fTreeRoots[this.fRootIx]);
        calcFirstPos(this.fRB.fTreeRoots[this.fRootIx]);
        calcLastPos(this.fRB.fTreeRoots[this.fRootIx]);
        calcFollowPos(this.fRB.fTreeRoots[this.fRootIx]);
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("pos") >= 0) {
            System.out.print("\n");
            printPosSets(this.fRB.fTreeRoots[this.fRootIx]);
        }
        if (this.fRB.fChainRules) {
            calcChainedFollowPos(this.fRB.fTreeRoots[this.fRootIx]);
        }
        if (this.fRB.fSetBuilder.sawBOF()) {
            bofFixup();
        }
        buildStateTable();
        flagAcceptingStates();
        flagLookAheadStates();
        flagTaggedStates();
        mergeRuleStatusVals();
        if (this.fRB.fDebugEnv == null || this.fRB.fDebugEnv.indexOf("states") < 0) {
            return;
        }
        printStates();
    }

    void calcNullable(RBBINode n) {
        if (n == null) {
            return;
        }
        if (n.fType == 0 || n.fType == 6) {
            n.fNullable = false;
            return;
        }
        if (n.fType == 4 || n.fType == 5) {
            n.fNullable = true;
            return;
        }
        calcNullable(n.fLeftChild);
        calcNullable(n.fRightChild);
        if (n.fType == 9) {
            n.fNullable = n.fLeftChild.fNullable ? true : n.fRightChild.fNullable;
            return;
        }
        if (n.fType == 8) {
            n.fNullable = n.fLeftChild.fNullable ? n.fRightChild.fNullable : false;
        } else if (n.fType == 10 || n.fType == 12) {
            n.fNullable = true;
        } else {
            n.fNullable = false;
        }
    }

    void calcFirstPos(RBBINode n) {
        if (n == null) {
            return;
        }
        if (n.fType == 3 || n.fType == 6 || n.fType == 4 || n.fType == 5) {
            n.fFirstPosSet.add(n);
            return;
        }
        calcFirstPos(n.fLeftChild);
        calcFirstPos(n.fRightChild);
        if (n.fType == 9) {
            n.fFirstPosSet.addAll(n.fLeftChild.fFirstPosSet);
            n.fFirstPosSet.addAll(n.fRightChild.fFirstPosSet);
            return;
        }
        if (n.fType == 8) {
            n.fFirstPosSet.addAll(n.fLeftChild.fFirstPosSet);
            if (!n.fLeftChild.fNullable) {
                return;
            }
            n.fFirstPosSet.addAll(n.fRightChild.fFirstPosSet);
            return;
        }
        if (n.fType != 10 && n.fType != 12 && n.fType != 11) {
            return;
        }
        n.fFirstPosSet.addAll(n.fLeftChild.fFirstPosSet);
    }

    void calcLastPos(RBBINode n) {
        if (n == null) {
            return;
        }
        if (n.fType == 3 || n.fType == 6 || n.fType == 4 || n.fType == 5) {
            n.fLastPosSet.add(n);
            return;
        }
        calcLastPos(n.fLeftChild);
        calcLastPos(n.fRightChild);
        if (n.fType == 9) {
            n.fLastPosSet.addAll(n.fLeftChild.fLastPosSet);
            n.fLastPosSet.addAll(n.fRightChild.fLastPosSet);
            return;
        }
        if (n.fType == 8) {
            n.fLastPosSet.addAll(n.fRightChild.fLastPosSet);
            if (!n.fRightChild.fNullable) {
                return;
            }
            n.fLastPosSet.addAll(n.fLeftChild.fLastPosSet);
            return;
        }
        if (n.fType != 10 && n.fType != 12 && n.fType != 11) {
            return;
        }
        n.fLastPosSet.addAll(n.fLeftChild.fLastPosSet);
    }

    void calcFollowPos(RBBINode n) {
        if (n == null || n.fType == 3 || n.fType == 6) {
            return;
        }
        calcFollowPos(n.fLeftChild);
        calcFollowPos(n.fRightChild);
        if (n.fType == 8) {
            for (RBBINode i : n.fLeftChild.fLastPosSet) {
                i.fFollowPos.addAll(n.fRightChild.fFirstPosSet);
            }
        }
        if (n.fType != 10 && n.fType != 11) {
            return;
        }
        for (RBBINode i2 : n.fLastPosSet) {
            i2.fFollowPos.addAll(n.fFirstPosSet);
        }
    }

    void calcChainedFollowPos(RBBINode tree) {
        int c;
        List<RBBINode> endMarkerNodes = new ArrayList<>();
        List<RBBINode> leafNodes = new ArrayList<>();
        tree.findNodes(endMarkerNodes, 6);
        tree.findNodes(leafNodes, 3);
        RBBINode userRuleRoot = tree;
        if (this.fRB.fSetBuilder.sawBOF()) {
            userRuleRoot = tree.fLeftChild.fRightChild;
        }
        Assert.assrt(userRuleRoot != null);
        Set<RBBINode> matchStartNodes = userRuleRoot.fFirstPosSet;
        for (RBBINode tNode : leafNodes) {
            RBBINode endNode = null;
            Iterator endMarkerNode$iterator = endMarkerNodes.iterator();
            while (true) {
                if (!endMarkerNode$iterator.hasNext()) {
                    break;
                }
                RBBINode endMarkerNode = (RBBINode) endMarkerNode$iterator.next();
                if (tNode.fFollowPos.contains(endMarkerNode)) {
                    endNode = tNode;
                    break;
                }
            }
            if (endNode != null) {
                if (this.fRB.fLBCMNoChain && (c = this.fRB.fSetBuilder.getFirstChar(endNode.fVal)) != -1) {
                    int cLBProp = UCharacter.getIntPropertyValue(c, UProperty.LINE_BREAK);
                    if (cLBProp != 9) {
                    }
                }
                for (RBBINode startNode : matchStartNodes) {
                    if (startNode.fType == 3 && endNode.fVal == startNode.fVal) {
                        endNode.fFollowPos.addAll(startNode.fFollowPos);
                    }
                }
            }
        }
    }

    void bofFixup() {
        RBBINode bofNode = this.fRB.fTreeRoots[this.fRootIx].fLeftChild.fLeftChild;
        Assert.assrt(bofNode.fType == 3);
        Assert.assrt(bofNode.fVal == 2);
        Set<RBBINode> matchStartNodes = this.fRB.fTreeRoots[this.fRootIx].fLeftChild.fRightChild.fFirstPosSet;
        for (RBBINode startNode : matchStartNodes) {
            if (startNode.fType == 3 && startNode.fVal == bofNode.fVal) {
                bofNode.fFollowPos.addAll(startNode.fFollowPos);
            }
        }
    }

    void buildStateTable() {
        int lastInputSymbol = this.fRB.fSetBuilder.getNumCharCategories() - 1;
        RBBIStateDescriptor failState = new RBBIStateDescriptor(lastInputSymbol);
        this.fDStates.add(failState);
        RBBIStateDescriptor initialState = new RBBIStateDescriptor(lastInputSymbol);
        initialState.fPositions.addAll(this.fRB.fTreeRoots[this.fRootIx].fFirstPosSet);
        this.fDStates.add(initialState);
        while (true) {
            RBBIStateDescriptor T = null;
            int tx = 1;
            while (true) {
                if (tx >= this.fDStates.size()) {
                    break;
                }
                RBBIStateDescriptor temp = this.fDStates.get(tx);
                if (temp.fMarked) {
                    tx++;
                } else {
                    T = temp;
                    break;
                }
            }
            if (T == null) {
                return;
            }
            T.fMarked = true;
            for (int a = 1; a <= lastInputSymbol; a++) {
                Set<RBBINode> U = null;
                for (RBBINode p : T.fPositions) {
                    if (p.fType == 3 && p.fVal == a) {
                        if (U == null) {
                            U = new HashSet<>();
                        }
                        U.addAll(p.fFollowPos);
                    }
                }
                int ux = 0;
                boolean UinDstates = false;
                if (U != null) {
                    Assert.assrt(U.size() > 0);
                    int ix = 0;
                    while (true) {
                        if (ix >= this.fDStates.size()) {
                            break;
                        }
                        RBBIStateDescriptor temp2 = this.fDStates.get(ix);
                        if (!U.equals(temp2.fPositions)) {
                            ix++;
                        } else {
                            U = temp2.fPositions;
                            ux = ix;
                            UinDstates = true;
                            break;
                        }
                    }
                    if (!UinDstates) {
                        RBBIStateDescriptor newState = new RBBIStateDescriptor(lastInputSymbol);
                        newState.fPositions = U;
                        this.fDStates.add(newState);
                        ux = this.fDStates.size() - 1;
                    }
                    T.fDtran[a] = ux;
                }
            }
        }
    }

    void flagAcceptingStates() {
        List<RBBINode> endMarkerNodes = new ArrayList<>();
        this.fRB.fTreeRoots[this.fRootIx].findNodes(endMarkerNodes, 6);
        for (int i = 0; i < endMarkerNodes.size(); i++) {
            RBBINode endMarker = endMarkerNodes.get(i);
            for (int n = 0; n < this.fDStates.size(); n++) {
                RBBIStateDescriptor sd = this.fDStates.get(n);
                if (sd.fPositions.contains(endMarker)) {
                    if (sd.fAccepting == 0) {
                        sd.fAccepting = endMarker.fVal;
                        if (sd.fAccepting == 0) {
                            sd.fAccepting = -1;
                        }
                    }
                    if (sd.fAccepting == -1 && endMarker.fVal != 0) {
                        sd.fAccepting = endMarker.fVal;
                    }
                    if (endMarker.fLookAheadEnd) {
                        sd.fLookAhead = sd.fAccepting;
                    }
                }
            }
        }
    }

    void flagLookAheadStates() {
        List<RBBINode> lookAheadNodes = new ArrayList<>();
        this.fRB.fTreeRoots[this.fRootIx].findNodes(lookAheadNodes, 4);
        for (int i = 0; i < lookAheadNodes.size(); i++) {
            RBBINode lookAheadNode = lookAheadNodes.get(i);
            for (int n = 0; n < this.fDStates.size(); n++) {
                RBBIStateDescriptor sd = this.fDStates.get(n);
                if (sd.fPositions.contains(lookAheadNode)) {
                    sd.fLookAhead = lookAheadNode.fVal;
                }
            }
        }
    }

    void flagTaggedStates() {
        List<RBBINode> tagNodes = new ArrayList<>();
        this.fRB.fTreeRoots[this.fRootIx].findNodes(tagNodes, 5);
        for (int i = 0; i < tagNodes.size(); i++) {
            RBBINode tagNode = tagNodes.get(i);
            for (int n = 0; n < this.fDStates.size(); n++) {
                RBBIStateDescriptor sd = this.fDStates.get(n);
                if (sd.fPositions.contains(tagNode)) {
                    sd.fTagVals.add(Integer.valueOf(tagNode.fVal));
                }
            }
        }
    }

    void mergeRuleStatusVals() {
        if (this.fRB.fRuleStatusVals.size() == 0) {
            this.fRB.fRuleStatusVals.add(1);
            this.fRB.fRuleStatusVals.add(0);
            SortedSet<Integer> s0 = new TreeSet<>();
            this.fRB.fStatusSets.put(s0, 0);
            SortedSet<Integer> s1 = new TreeSet<>();
            s1.add(0);
            this.fRB.fStatusSets.put(s0, 0);
        }
        for (int n = 0; n < this.fDStates.size(); n++) {
            RBBIStateDescriptor sd = this.fDStates.get(n);
            Set<Integer> statusVals = sd.fTagVals;
            Integer arrayIndexI = this.fRB.fStatusSets.get(statusVals);
            if (arrayIndexI == null) {
                arrayIndexI = Integer.valueOf(this.fRB.fRuleStatusVals.size());
                this.fRB.fStatusSets.put(statusVals, arrayIndexI);
                this.fRB.fRuleStatusVals.add(Integer.valueOf(statusVals.size()));
                this.fRB.fRuleStatusVals.addAll(statusVals);
            }
            sd.fTagsIdx = arrayIndexI.intValue();
        }
    }

    void printPosSets(RBBINode n) {
        if (n == null) {
            return;
        }
        RBBINode.printNode(n);
        System.out.print("         Nullable:  " + n.fNullable);
        System.out.print("         firstpos:  ");
        printSet(n.fFirstPosSet);
        System.out.print("         lastpos:   ");
        printSet(n.fLastPosSet);
        System.out.print("         followpos: ");
        printSet(n.fFollowPos);
        printPosSets(n.fLeftChild);
        printPosSets(n.fRightChild);
    }

    int getTableSize() {
        if (this.fRB.fTreeRoots[this.fRootIx] == null) {
            return 0;
        }
        int numRows = this.fDStates.size();
        int numCols = this.fRB.fSetBuilder.getNumCharCategories();
        int rowSize = (numCols * 2) + 8;
        int size = (numRows * rowSize) + 16;
        while (size % 8 > 0) {
            size++;
        }
        return size;
    }

    short[] exportTable() {
        if (this.fRB.fTreeRoots[this.fRootIx] == null) {
            return new short[0];
        }
        boolean z = this.fRB.fSetBuilder.getNumCharCategories() < 32767 && this.fDStates.size() < 32767;
        Assert.assrt(z);
        int numStates = this.fDStates.size();
        int rowLen = this.fRB.fSetBuilder.getNumCharCategories() + 4;
        int tableSize = getTableSize() / 2;
        short[] table = new short[tableSize];
        table[0] = (short) (numStates >>> 16);
        table[1] = (short) (65535 & numStates);
        table[2] = (short) (rowLen >>> 16);
        table[3] = (short) (65535 & rowLen);
        int flags = 0;
        if (this.fRB.fLookAheadHardBreak) {
            flags = 1;
        }
        if (this.fRB.fSetBuilder.sawBOF()) {
            flags |= 2;
        }
        table[4] = (short) (flags >>> 16);
        table[5] = (short) (65535 & flags);
        int numCharCategories = this.fRB.fSetBuilder.getNumCharCategories();
        for (int state = 0; state < numStates; state++) {
            RBBIStateDescriptor sd = this.fDStates.get(state);
            int row = (state * rowLen) + 8;
            Assert.assrt(-32768 < sd.fAccepting && sd.fAccepting <= 32767);
            Assert.assrt(-32768 < sd.fLookAhead && sd.fLookAhead <= 32767);
            table[row + 0] = (short) sd.fAccepting;
            table[row + 1] = (short) sd.fLookAhead;
            table[row + 2] = (short) sd.fTagsIdx;
            for (int col = 0; col < numCharCategories; col++) {
                table[row + 4 + col] = (short) sd.fDtran[col];
            }
        }
        return table;
    }

    void printSet(Collection<RBBINode> s) {
        for (RBBINode n : s) {
            RBBINode.printInt(n.fSerialNum, 8);
        }
        System.out.println();
    }

    void printStates() {
        System.out.print("state |           i n p u t     s y m b o l s \n");
        System.out.print("      | Acc  LA    Tag");
        for (int c = 0; c < this.fRB.fSetBuilder.getNumCharCategories(); c++) {
            RBBINode.printInt(c, 3);
        }
        System.out.print("\n");
        System.out.print("      |---------------");
        for (int c2 = 0; c2 < this.fRB.fSetBuilder.getNumCharCategories(); c2++) {
            System.out.print("---");
        }
        System.out.print("\n");
        for (int n = 0; n < this.fDStates.size(); n++) {
            RBBIStateDescriptor sd = this.fDStates.get(n);
            RBBINode.printInt(n, 5);
            System.out.print(" | ");
            RBBINode.printInt(sd.fAccepting, 3);
            RBBINode.printInt(sd.fLookAhead, 4);
            RBBINode.printInt(sd.fTagsIdx, 6);
            System.out.print(" ");
            for (int c3 = 0; c3 < this.fRB.fSetBuilder.getNumCharCategories(); c3++) {
                RBBINode.printInt(sd.fDtran[c3], 3);
            }
            System.out.print("\n");
        }
        System.out.print("\n\n");
    }

    void printRuleStatusTable() {
        int nextRecord = 0;
        List<Integer> tbl = this.fRB.fRuleStatusVals;
        System.out.print("index |  tags \n");
        System.out.print("-------------------\n");
        while (nextRecord < tbl.size()) {
            int thisRecord = nextRecord;
            nextRecord = tbl.get(nextRecord).intValue() + thisRecord + 1;
            RBBINode.printInt(thisRecord, 7);
            for (int i = thisRecord + 1; i < nextRecord; i++) {
                int val = tbl.get(i).intValue();
                RBBINode.printInt(val, 7);
            }
            System.out.print("\n");
        }
        System.out.print("\n\n");
    }
}
