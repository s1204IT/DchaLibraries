package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.IntTrieBuilder;
import android.icu.impl.TrieBuilder;
import android.icu.impl.locale.LanguageTag;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

class RBBISetBuilder {
    RBBIDataManipulate dm = new RBBIDataManipulate();
    int fGroupCount;
    RBBIRuleBuilder fRB;
    RangeDescriptor fRangeList;
    boolean fSawBOF;
    IntTrieBuilder fTrie;
    int fTrieSize;

    static class RangeDescriptor {
        int fEndChar;
        List<RBBINode> fIncludesSets;
        RangeDescriptor fNext;
        int fNum;
        int fStartChar;

        RangeDescriptor() {
            this.fIncludesSets = new ArrayList();
        }

        RangeDescriptor(RangeDescriptor other) {
            this.fStartChar = other.fStartChar;
            this.fEndChar = other.fEndChar;
            this.fNum = other.fNum;
            this.fIncludesSets = new ArrayList(other.fIncludesSets);
        }

        void split(int where) {
            boolean z = false;
            if (where > this.fStartChar && where <= this.fEndChar) {
                z = true;
            }
            Assert.assrt(z);
            RangeDescriptor nr = new RangeDescriptor(this);
            nr.fStartChar = where;
            this.fEndChar = where - 1;
            nr.fNext = this.fNext;
            this.fNext = nr;
        }

        void setDictionaryFlag() {
            RBBINode varRef;
            for (int i = 0; i < this.fIncludesSets.size(); i++) {
                RBBINode usetNode = this.fIncludesSets.get(i);
                String setName = "";
                RBBINode setRef = usetNode.fParent;
                if (setRef != null && (varRef = setRef.fParent) != null && varRef.fType == 2) {
                    setName = varRef.fText;
                }
                if (setName.equals("dictionary")) {
                    this.fNum |= 16384;
                    return;
                }
            }
        }
    }

    RBBISetBuilder(RBBIRuleBuilder rb) {
        this.fRB = rb;
    }

    void build() {
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("usets") >= 0) {
            printSets();
        }
        this.fRangeList = new RangeDescriptor();
        this.fRangeList.fStartChar = 0;
        this.fRangeList.fEndChar = 1114111;
        for (RBBINode usetNode : this.fRB.fUSetNodes) {
            UnicodeSet inputSet = usetNode.fInputSet;
            int inputSetRangeCount = inputSet.getRangeCount();
            int inputSetRangeIndex = 0;
            RangeDescriptor rlRange = this.fRangeList;
            while (inputSetRangeIndex < inputSetRangeCount) {
                int inputSetRangeBegin = inputSet.getRangeStart(inputSetRangeIndex);
                int inputSetRangeEnd = inputSet.getRangeEnd(inputSetRangeIndex);
                while (rlRange.fEndChar < inputSetRangeBegin) {
                    rlRange = rlRange.fNext;
                }
                if (rlRange.fStartChar < inputSetRangeBegin) {
                    rlRange.split(inputSetRangeBegin);
                } else {
                    if (rlRange.fEndChar > inputSetRangeEnd) {
                        rlRange.split(inputSetRangeEnd + 1);
                    }
                    if (rlRange.fIncludesSets.indexOf(usetNode) == -1) {
                        rlRange.fIncludesSets.add(usetNode);
                    }
                    if (inputSetRangeEnd == rlRange.fEndChar) {
                        inputSetRangeIndex++;
                    }
                    rlRange = rlRange.fNext;
                }
            }
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("range") >= 0) {
            printRanges();
        }
        for (RangeDescriptor rlRange2 = this.fRangeList; rlRange2 != null; rlRange2 = rlRange2.fNext) {
            RangeDescriptor rangeDescriptor = this.fRangeList;
            while (true) {
                RangeDescriptor rlSearchRange = rangeDescriptor;
                if (rlSearchRange == rlRange2) {
                    break;
                }
                if (!rlRange2.fIncludesSets.equals(rlSearchRange.fIncludesSets)) {
                    rangeDescriptor = rlSearchRange.fNext;
                } else {
                    rlRange2.fNum = rlSearchRange.fNum;
                    break;
                }
            }
            if (rlRange2.fNum == 0) {
                this.fGroupCount++;
                rlRange2.fNum = this.fGroupCount + 2;
                rlRange2.setDictionaryFlag();
                addValToSets(rlRange2.fIncludesSets, this.fGroupCount + 2);
            }
        }
        for (RBBINode usetNode2 : this.fRB.fUSetNodes) {
            UnicodeSet inputSet2 = usetNode2.fInputSet;
            if (inputSet2.contains("eof")) {
                addValToSet(usetNode2, 1);
            }
            if (inputSet2.contains("bof")) {
                addValToSet(usetNode2, 2);
                this.fSawBOF = true;
            }
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("rgroup") >= 0) {
            printRangeGroups();
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("esets") >= 0) {
            printSets();
        }
        this.fTrie = new IntTrieBuilder(null, 100000, 0, 0, true);
        for (RangeDescriptor rlRange3 = this.fRangeList; rlRange3 != null; rlRange3 = rlRange3.fNext) {
            this.fTrie.setRange(rlRange3.fStartChar, rlRange3.fEndChar + 1, rlRange3.fNum, true);
        }
    }

    class RBBIDataManipulate implements TrieBuilder.DataManipulate {
        RBBIDataManipulate() {
        }

        @Override
        public int getFoldedValue(int start, int offset) {
            boolean[] inBlockZero = new boolean[1];
            int limit = start + 1024;
            while (start < limit) {
                int value = RBBISetBuilder.this.fTrie.getValue(start, inBlockZero);
                if (inBlockZero[0]) {
                    start += 32;
                } else {
                    if (value != 0) {
                        return 32768 | offset;
                    }
                    start++;
                }
            }
            return 0;
        }
    }

    int getTrieSize() {
        try {
            int size = this.fTrie.serialize(null, true, this.dm);
            return size;
        } catch (IOException e) {
            Assert.assrt(false);
            return 0;
        }
    }

    void serializeTrie(OutputStream os) throws IOException {
        this.fTrie.serialize(os, true, this.dm);
    }

    void addValToSets(List<RBBINode> sets, int val) {
        for (RBBINode usetNode : sets) {
            addValToSet(usetNode, val);
        }
    }

    void addValToSet(RBBINode usetNode, int val) {
        RBBINode leafNode = new RBBINode(3);
        leafNode.fVal = val;
        if (usetNode.fLeftChild == null) {
            usetNode.fLeftChild = leafNode;
            leafNode.fParent = usetNode;
            return;
        }
        RBBINode orNode = new RBBINode(9);
        orNode.fLeftChild = usetNode.fLeftChild;
        orNode.fRightChild = leafNode;
        orNode.fLeftChild.fParent = orNode;
        orNode.fRightChild.fParent = orNode;
        usetNode.fLeftChild = orNode;
        orNode.fParent = usetNode;
    }

    int getNumCharCategories() {
        return this.fGroupCount + 3;
    }

    boolean sawBOF() {
        return this.fSawBOF;
    }

    int getFirstChar(int category) {
        for (RangeDescriptor rlRange = this.fRangeList; rlRange != null; rlRange = rlRange.fNext) {
            if (rlRange.fNum == category) {
                int retVal = rlRange.fStartChar;
                return retVal;
            }
        }
        return -1;
    }

    void printRanges() {
        RBBINode varRef;
        System.out.print("\n\n Nonoverlapping Ranges ...\n");
        for (RangeDescriptor rlRange = this.fRangeList; rlRange != null; rlRange = rlRange.fNext) {
            System.out.print(" " + rlRange.fNum + "   " + rlRange.fStartChar + LanguageTag.SEP + rlRange.fEndChar);
            for (int i = 0; i < rlRange.fIncludesSets.size(); i++) {
                RBBINode usetNode = rlRange.fIncludesSets.get(i);
                String setName = "anon";
                RBBINode setRef = usetNode.fParent;
                if (setRef != null && (varRef = setRef.fParent) != null && varRef.fType == 2) {
                    setName = varRef.fText;
                }
                System.out.print(setName);
                System.out.print("  ");
            }
            System.out.println("");
        }
    }

    void printRangeGroups() {
        int i;
        RBBINode varRef;
        int lastPrintedGroupNum = 0;
        System.out.print("\nRanges grouped by Unicode Set Membership...\n");
        for (RangeDescriptor rlRange = this.fRangeList; rlRange != null; rlRange = rlRange.fNext) {
            int groupNum = rlRange.fNum & 49151;
            if (groupNum > lastPrintedGroupNum) {
                lastPrintedGroupNum = groupNum;
                if (groupNum < 10) {
                    System.out.print(" ");
                }
                System.out.print(groupNum + " ");
                if ((rlRange.fNum & 16384) != 0) {
                    System.out.print(" <DICT> ");
                }
                for (int i2 = 0; i2 < rlRange.fIncludesSets.size(); i2++) {
                    RBBINode usetNode = rlRange.fIncludesSets.get(i2);
                    String setName = "anon";
                    RBBINode setRef = usetNode.fParent;
                    if (setRef != null && (varRef = setRef.fParent) != null && varRef.fType == 2) {
                        setName = varRef.fText;
                    }
                    System.out.print(setName);
                    System.out.print(" ");
                }
                RangeDescriptor tRange = rlRange;
                int i3 = 0;
                while (tRange != null) {
                    if (tRange.fNum == rlRange.fNum) {
                        i = i3 + 1;
                        if (i3 % 5 == 0) {
                            System.out.print("\n    ");
                        }
                        RBBINode.printHex(tRange.fStartChar, -1);
                        System.out.print(LanguageTag.SEP);
                        RBBINode.printHex(tRange.fEndChar, 0);
                    } else {
                        i = i3;
                    }
                    tRange = tRange.fNext;
                    i3 = i;
                }
                System.out.print("\n");
            }
        }
        System.out.print("\n");
    }

    void printSets() {
        RBBINode varRef;
        System.out.print("\n\nUnicode Sets List\n------------------\n");
        for (int i = 0; i < this.fRB.fUSetNodes.size(); i++) {
            RBBINode usetNode = this.fRB.fUSetNodes.get(i);
            RBBINode.printInt(2, i);
            String setName = "anonymous";
            RBBINode setRef = usetNode.fParent;
            if (setRef != null && (varRef = setRef.fParent) != null && varRef.fType == 2) {
                setName = varRef.fText;
            }
            System.out.print("  " + setName);
            System.out.print("   ");
            System.out.print(usetNode.fText);
            System.out.print("\n");
            if (usetNode.fLeftChild != null) {
                usetNode.fLeftChild.printTree(true);
            }
        }
        System.out.print("\n");
    }
}
