package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.number.Padder;
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

        RBBIStateDescriptor(int i) {
            this.fDtran = new int[i + 1];
        }
    }

    RBBITableBuilder(RBBIRuleBuilder rBBIRuleBuilder, int i) {
        this.fRootIx = i;
        this.fRB = rBBIRuleBuilder;
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
            RBBINode rBBINode = new RBBINode(8);
            RBBINode rBBINode2 = new RBBINode(3);
            rBBINode.fLeftChild = rBBINode2;
            rBBINode.fRightChild = this.fRB.fTreeRoots[this.fRootIx];
            rBBINode2.fParent = rBBINode;
            rBBINode2.fVal = 2;
            this.fRB.fTreeRoots[this.fRootIx] = rBBINode;
        }
        RBBINode rBBINode3 = new RBBINode(8);
        rBBINode3.fLeftChild = this.fRB.fTreeRoots[this.fRootIx];
        this.fRB.fTreeRoots[this.fRootIx].fParent = rBBINode3;
        rBBINode3.fRightChild = new RBBINode(6);
        rBBINode3.fRightChild.fParent = rBBINode3;
        this.fRB.fTreeRoots[this.fRootIx] = rBBINode3;
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

    void calcNullable(RBBINode rBBINode) {
        if (rBBINode == null) {
            return;
        }
        if (rBBINode.fType == 0 || rBBINode.fType == 6) {
            rBBINode.fNullable = false;
            return;
        }
        if (rBBINode.fType == 4 || rBBINode.fType == 5) {
            rBBINode.fNullable = true;
            return;
        }
        calcNullable(rBBINode.fLeftChild);
        calcNullable(rBBINode.fRightChild);
        if (rBBINode.fType == 9) {
            rBBINode.fNullable = rBBINode.fLeftChild.fNullable || rBBINode.fRightChild.fNullable;
            return;
        }
        if (rBBINode.fType == 8) {
            if (rBBINode.fLeftChild.fNullable && rBBINode.fRightChild.fNullable) {
                z = true;
            }
            rBBINode.fNullable = z;
            return;
        }
        if (rBBINode.fType == 10 || rBBINode.fType == 12) {
            rBBINode.fNullable = true;
        } else {
            rBBINode.fNullable = false;
        }
    }

    void calcFirstPos(RBBINode rBBINode) {
        if (rBBINode == null) {
            return;
        }
        if (rBBINode.fType == 3 || rBBINode.fType == 6 || rBBINode.fType == 4 || rBBINode.fType == 5) {
            rBBINode.fFirstPosSet.add(rBBINode);
            return;
        }
        calcFirstPos(rBBINode.fLeftChild);
        calcFirstPos(rBBINode.fRightChild);
        if (rBBINode.fType == 9) {
            rBBINode.fFirstPosSet.addAll(rBBINode.fLeftChild.fFirstPosSet);
            rBBINode.fFirstPosSet.addAll(rBBINode.fRightChild.fFirstPosSet);
            return;
        }
        if (rBBINode.fType == 8) {
            rBBINode.fFirstPosSet.addAll(rBBINode.fLeftChild.fFirstPosSet);
            if (rBBINode.fLeftChild.fNullable) {
                rBBINode.fFirstPosSet.addAll(rBBINode.fRightChild.fFirstPosSet);
                return;
            }
            return;
        }
        if (rBBINode.fType == 10 || rBBINode.fType == 12 || rBBINode.fType == 11) {
            rBBINode.fFirstPosSet.addAll(rBBINode.fLeftChild.fFirstPosSet);
        }
    }

    void calcLastPos(RBBINode rBBINode) {
        if (rBBINode == null) {
            return;
        }
        if (rBBINode.fType == 3 || rBBINode.fType == 6 || rBBINode.fType == 4 || rBBINode.fType == 5) {
            rBBINode.fLastPosSet.add(rBBINode);
            return;
        }
        calcLastPos(rBBINode.fLeftChild);
        calcLastPos(rBBINode.fRightChild);
        if (rBBINode.fType == 9) {
            rBBINode.fLastPosSet.addAll(rBBINode.fLeftChild.fLastPosSet);
            rBBINode.fLastPosSet.addAll(rBBINode.fRightChild.fLastPosSet);
            return;
        }
        if (rBBINode.fType == 8) {
            rBBINode.fLastPosSet.addAll(rBBINode.fRightChild.fLastPosSet);
            if (rBBINode.fRightChild.fNullable) {
                rBBINode.fLastPosSet.addAll(rBBINode.fLeftChild.fLastPosSet);
                return;
            }
            return;
        }
        if (rBBINode.fType == 10 || rBBINode.fType == 12 || rBBINode.fType == 11) {
            rBBINode.fLastPosSet.addAll(rBBINode.fLeftChild.fLastPosSet);
        }
    }

    void calcFollowPos(RBBINode rBBINode) {
        if (rBBINode == null || rBBINode.fType == 3 || rBBINode.fType == 6) {
            return;
        }
        calcFollowPos(rBBINode.fLeftChild);
        calcFollowPos(rBBINode.fRightChild);
        if (rBBINode.fType == 8) {
            Iterator<RBBINode> it = rBBINode.fLeftChild.fLastPosSet.iterator();
            while (it.hasNext()) {
                it.next().fFollowPos.addAll(rBBINode.fRightChild.fFirstPosSet);
            }
        }
        if (rBBINode.fType == 10 || rBBINode.fType == 11) {
            Iterator<RBBINode> it2 = rBBINode.fLastPosSet.iterator();
            while (it2.hasNext()) {
                it2.next().fFollowPos.addAll(rBBINode.fFirstPosSet);
            }
        }
    }

    void addRuleRootNodes(List<RBBINode> list, RBBINode rBBINode) {
        if (rBBINode == null) {
            return;
        }
        if (rBBINode.fRuleRoot) {
            list.add(rBBINode);
        } else {
            addRuleRootNodes(list, rBBINode.fLeftChild);
            addRuleRootNodes(list, rBBINode.fRightChild);
        }
    }

    void calcChainedFollowPos(RBBINode rBBINode) {
        int firstChar;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        rBBINode.findNodes(arrayList, 6);
        rBBINode.findNodes(arrayList2, 3);
        ArrayList arrayList3 = new ArrayList();
        addRuleRootNodes(arrayList3, rBBINode);
        HashSet<RBBINode> hashSet = new HashSet();
        for (RBBINode rBBINode2 : arrayList3) {
            if (rBBINode2.fChainIn) {
                hashSet.addAll(rBBINode2.fFirstPosSet);
            }
        }
        for (RBBINode rBBINode3 : arrayList2) {
            Iterator<RBBINode> it = arrayList.iterator();
            while (true) {
                if (it.hasNext()) {
                    if (rBBINode3.fFollowPos.contains(it.next())) {
                        break;
                    }
                } else {
                    rBBINode3 = null;
                    break;
                }
            }
            if (rBBINode3 != null && (!this.fRB.fLBCMNoChain || (firstChar = this.fRB.fSetBuilder.getFirstChar(rBBINode3.fVal)) == -1 || UCharacter.getIntPropertyValue(firstChar, UProperty.LINE_BREAK) != 9)) {
                for (RBBINode rBBINode4 : hashSet) {
                    if (rBBINode4.fType == 3 && rBBINode3.fVal == rBBINode4.fVal) {
                        rBBINode3.fFollowPos.addAll(rBBINode4.fFollowPos);
                    }
                }
            }
        }
    }

    void bofFixup() {
        RBBINode rBBINode = this.fRB.fTreeRoots[this.fRootIx].fLeftChild.fLeftChild;
        Assert.assrt(rBBINode.fType == 3);
        Assert.assrt(rBBINode.fVal == 2);
        for (RBBINode rBBINode2 : this.fRB.fTreeRoots[this.fRootIx].fLeftChild.fRightChild.fFirstPosSet) {
            if (rBBINode2.fType == 3 && rBBINode2.fVal == rBBINode.fVal) {
                rBBINode.fFollowPos.addAll(rBBINode2.fFollowPos);
            }
        }
    }

    void buildStateTable() {
        RBBIStateDescriptor rBBIStateDescriptor;
        int numCharCategories = this.fRB.fSetBuilder.getNumCharCategories() - 1;
        this.fDStates.add(new RBBIStateDescriptor(numCharCategories));
        RBBIStateDescriptor rBBIStateDescriptor2 = new RBBIStateDescriptor(numCharCategories);
        rBBIStateDescriptor2.fPositions.addAll(this.fRB.fTreeRoots[this.fRootIx].fFirstPosSet);
        this.fDStates.add(rBBIStateDescriptor2);
        while (true) {
            int i = 1;
            while (true) {
                if (i < this.fDStates.size()) {
                    rBBIStateDescriptor = this.fDStates.get(i);
                    if (!rBBIStateDescriptor.fMarked) {
                        break;
                    } else {
                        i++;
                    }
                } else {
                    rBBIStateDescriptor = null;
                    break;
                }
            }
            if (rBBIStateDescriptor != null) {
                rBBIStateDescriptor.fMarked = true;
                for (int i2 = 1; i2 <= numCharCategories; i2++) {
                    Set<RBBINode> hashSet = null;
                    for (RBBINode rBBINode : rBBIStateDescriptor.fPositions) {
                        if (rBBINode.fType == 3 && rBBINode.fVal == i2) {
                            if (hashSet == null) {
                                hashSet = new HashSet<>();
                            }
                            hashSet.addAll(rBBINode.fFollowPos);
                        }
                    }
                    if (hashSet != null) {
                        boolean z = false;
                        Assert.assrt(hashSet.size() > 0);
                        int size = 0;
                        while (true) {
                            if (size < this.fDStates.size()) {
                                RBBIStateDescriptor rBBIStateDescriptor3 = this.fDStates.get(size);
                                if (!hashSet.equals(rBBIStateDescriptor3.fPositions)) {
                                    size++;
                                } else {
                                    hashSet = rBBIStateDescriptor3.fPositions;
                                    z = true;
                                    break;
                                }
                            } else {
                                size = 0;
                                break;
                            }
                        }
                        if (!z) {
                            RBBIStateDescriptor rBBIStateDescriptor4 = new RBBIStateDescriptor(numCharCategories);
                            rBBIStateDescriptor4.fPositions = hashSet;
                            this.fDStates.add(rBBIStateDescriptor4);
                            size = this.fDStates.size() - 1;
                        }
                        rBBIStateDescriptor.fDtran[i2] = size;
                    }
                }
            } else {
                return;
            }
        }
    }

    void flagAcceptingStates() {
        ArrayList arrayList = new ArrayList();
        this.fRB.fTreeRoots[this.fRootIx].findNodes(arrayList, 6);
        for (int i = 0; i < arrayList.size(); i++) {
            RBBINode rBBINode = (RBBINode) arrayList.get(i);
            for (int i2 = 0; i2 < this.fDStates.size(); i2++) {
                RBBIStateDescriptor rBBIStateDescriptor = this.fDStates.get(i2);
                if (rBBIStateDescriptor.fPositions.contains(rBBINode)) {
                    if (rBBIStateDescriptor.fAccepting == 0) {
                        rBBIStateDescriptor.fAccepting = rBBINode.fVal;
                        if (rBBIStateDescriptor.fAccepting == 0) {
                            rBBIStateDescriptor.fAccepting = -1;
                        }
                    }
                    if (rBBIStateDescriptor.fAccepting == -1 && rBBINode.fVal != 0) {
                        rBBIStateDescriptor.fAccepting = rBBINode.fVal;
                    }
                    if (rBBINode.fLookAheadEnd) {
                        rBBIStateDescriptor.fLookAhead = rBBIStateDescriptor.fAccepting;
                    }
                }
            }
        }
    }

    void flagLookAheadStates() {
        ArrayList arrayList = new ArrayList();
        this.fRB.fTreeRoots[this.fRootIx].findNodes(arrayList, 4);
        for (int i = 0; i < arrayList.size(); i++) {
            RBBINode rBBINode = (RBBINode) arrayList.get(i);
            for (int i2 = 0; i2 < this.fDStates.size(); i2++) {
                RBBIStateDescriptor rBBIStateDescriptor = this.fDStates.get(i2);
                if (rBBIStateDescriptor.fPositions.contains(rBBINode)) {
                    rBBIStateDescriptor.fLookAhead = rBBINode.fVal;
                }
            }
        }
    }

    void flagTaggedStates() {
        ArrayList arrayList = new ArrayList();
        this.fRB.fTreeRoots[this.fRootIx].findNodes(arrayList, 5);
        for (int i = 0; i < arrayList.size(); i++) {
            RBBINode rBBINode = (RBBINode) arrayList.get(i);
            for (int i2 = 0; i2 < this.fDStates.size(); i2++) {
                RBBIStateDescriptor rBBIStateDescriptor = this.fDStates.get(i2);
                if (rBBIStateDescriptor.fPositions.contains(rBBINode)) {
                    rBBIStateDescriptor.fTagVals.add(Integer.valueOf(rBBINode.fVal));
                }
            }
        }
    }

    void mergeRuleStatusVals() {
        if (this.fRB.fRuleStatusVals.size() == 0) {
            this.fRB.fRuleStatusVals.add(1);
            this.fRB.fRuleStatusVals.add(0);
            TreeSet treeSet = new TreeSet();
            this.fRB.fStatusSets.put(treeSet, 0);
            new TreeSet().add(0);
            this.fRB.fStatusSets.put(treeSet, 0);
        }
        for (int i = 0; i < this.fDStates.size(); i++) {
            RBBIStateDescriptor rBBIStateDescriptor = this.fDStates.get(i);
            SortedSet<Integer> sortedSet = rBBIStateDescriptor.fTagVals;
            Integer numValueOf = this.fRB.fStatusSets.get(sortedSet);
            if (numValueOf == null) {
                numValueOf = Integer.valueOf(this.fRB.fRuleStatusVals.size());
                this.fRB.fStatusSets.put(sortedSet, numValueOf);
                this.fRB.fRuleStatusVals.add(Integer.valueOf(sortedSet.size()));
                this.fRB.fRuleStatusVals.addAll(sortedSet);
            }
            rBBIStateDescriptor.fTagsIdx = numValueOf.intValue();
        }
    }

    void printPosSets(RBBINode rBBINode) {
        if (rBBINode == null) {
            return;
        }
        RBBINode.printNode(rBBINode);
        System.out.print("         Nullable:  " + rBBINode.fNullable);
        System.out.print("         firstpos:  ");
        printSet(rBBINode.fFirstPosSet);
        System.out.print("         lastpos:   ");
        printSet(rBBINode.fLastPosSet);
        System.out.print("         followpos: ");
        printSet(rBBINode.fFollowPos);
        printPosSets(rBBINode.fLeftChild);
        printPosSets(rBBINode.fRightChild);
    }

    int getTableSize() {
        if (this.fRB.fTreeRoots[this.fRootIx] == null) {
            return 0;
        }
        int size = 16 + (this.fDStates.size() * (8 + (2 * this.fRB.fSetBuilder.getNumCharCategories())));
        while (size % 8 > 0) {
            size++;
        }
        return size;
    }

    short[] exportTable() {
        if (this.fRB.fTreeRoots[this.fRootIx] == null) {
            return new short[0];
        }
        Assert.assrt(this.fRB.fSetBuilder.getNumCharCategories() < 32767 && this.fDStates.size() < 32767);
        int size = this.fDStates.size();
        int numCharCategories = this.fRB.fSetBuilder.getNumCharCategories() + 4;
        short[] sArr = new short[getTableSize() / 2];
        sArr[0] = (short) (size >>> 16);
        sArr[1] = (short) (size & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
        sArr[2] = (short) (numCharCategories >>> 16);
        sArr[3] = (short) (numCharCategories & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
        int i = this.fRB.fLookAheadHardBreak ? 1 : 0;
        if (this.fRB.fSetBuilder.sawBOF()) {
            i |= 2;
        }
        sArr[4] = (short) (i >>> 16);
        sArr[5] = (short) (i & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
        int numCharCategories2 = this.fRB.fSetBuilder.getNumCharCategories();
        for (int i2 = 0; i2 < size; i2++) {
            RBBIStateDescriptor rBBIStateDescriptor = this.fDStates.get(i2);
            int i3 = 8 + (i2 * numCharCategories);
            Assert.assrt(-32768 < rBBIStateDescriptor.fAccepting && rBBIStateDescriptor.fAccepting <= 32767);
            Assert.assrt(-32768 < rBBIStateDescriptor.fLookAhead && rBBIStateDescriptor.fLookAhead <= 32767);
            sArr[i3 + 0] = (short) rBBIStateDescriptor.fAccepting;
            sArr[i3 + 1] = (short) rBBIStateDescriptor.fLookAhead;
            sArr[i3 + 2] = (short) rBBIStateDescriptor.fTagsIdx;
            for (int i4 = 0; i4 < numCharCategories2; i4++) {
                sArr[i3 + 4 + i4] = (short) rBBIStateDescriptor.fDtran[i4];
            }
        }
        return sArr;
    }

    void printSet(Collection<RBBINode> collection) {
        Iterator<RBBINode> it = collection.iterator();
        while (it.hasNext()) {
            RBBINode.printInt(it.next().fSerialNum, 8);
        }
        System.out.println();
    }

    void printStates() {
        System.out.print("state |           i n p u t     s y m b o l s \n");
        System.out.print("      | Acc  LA    Tag");
        for (int i = 0; i < this.fRB.fSetBuilder.getNumCharCategories(); i++) {
            RBBINode.printInt(i, 3);
        }
        System.out.print("\n");
        System.out.print("      |---------------");
        for (int i2 = 0; i2 < this.fRB.fSetBuilder.getNumCharCategories(); i2++) {
            System.out.print("---");
        }
        System.out.print("\n");
        for (int i3 = 0; i3 < this.fDStates.size(); i3++) {
            RBBIStateDescriptor rBBIStateDescriptor = this.fDStates.get(i3);
            RBBINode.printInt(i3, 5);
            System.out.print(" | ");
            RBBINode.printInt(rBBIStateDescriptor.fAccepting, 3);
            RBBINode.printInt(rBBIStateDescriptor.fLookAhead, 4);
            RBBINode.printInt(rBBIStateDescriptor.fTagsIdx, 6);
            System.out.print(Padder.FALLBACK_PADDING_STRING);
            for (int i4 = 0; i4 < this.fRB.fSetBuilder.getNumCharCategories(); i4++) {
                RBBINode.printInt(rBBIStateDescriptor.fDtran[i4], 3);
            }
            System.out.print("\n");
        }
        System.out.print("\n\n");
    }

    void printRuleStatusTable() {
        List<Integer> list = this.fRB.fRuleStatusVals;
        System.out.print("index |  tags \n");
        System.out.print("-------------------\n");
        int i = 0;
        while (i < list.size()) {
            int iIntValue = list.get(i).intValue() + i + 1;
            RBBINode.printInt(i, 7);
            while (true) {
                i++;
                if (i < iIntValue) {
                    RBBINode.printInt(list.get(i).intValue(), 7);
                }
            }
            System.out.print("\n");
            i = iIntValue;
        }
        System.out.print("\n\n");
    }
}
