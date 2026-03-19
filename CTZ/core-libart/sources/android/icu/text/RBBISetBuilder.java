package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.Trie2Writable;
import android.icu.impl.Trie2_16;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.number.Padder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class RBBISetBuilder {
    Trie2_16 fFrozenTrie;
    int fGroupCount;
    RBBIRuleBuilder fRB;
    RangeDescriptor fRangeList;
    boolean fSawBOF;
    Trie2Writable fTrie;

    static class RangeDescriptor {
        int fEndChar;
        List<RBBINode> fIncludesSets;
        RangeDescriptor fNext;
        int fNum;
        int fStartChar;

        RangeDescriptor() {
            this.fIncludesSets = new ArrayList();
        }

        RangeDescriptor(RangeDescriptor rangeDescriptor) {
            this.fStartChar = rangeDescriptor.fStartChar;
            this.fEndChar = rangeDescriptor.fEndChar;
            this.fNum = rangeDescriptor.fNum;
            this.fIncludesSets = new ArrayList(rangeDescriptor.fIncludesSets);
        }

        void split(int i) {
            Assert.assrt(i > this.fStartChar && i <= this.fEndChar);
            RangeDescriptor rangeDescriptor = new RangeDescriptor(this);
            rangeDescriptor.fStartChar = i;
            this.fEndChar = i - 1;
            rangeDescriptor.fNext = this.fNext;
            this.fNext = rangeDescriptor;
        }

        void setDictionaryFlag() {
            RBBINode rBBINode;
            for (int i = 0; i < this.fIncludesSets.size(); i++) {
                String str = "";
                RBBINode rBBINode2 = this.fIncludesSets.get(i).fParent;
                if (rBBINode2 != null && (rBBINode = rBBINode2.fParent) != null && rBBINode.fType == 2) {
                    str = rBBINode.fText;
                }
                if (str.equals("dictionary")) {
                    this.fNum |= 16384;
                    return;
                }
            }
        }
    }

    RBBISetBuilder(RBBIRuleBuilder rBBIRuleBuilder) {
        this.fRB = rBBIRuleBuilder;
    }

    void build() {
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("usets") >= 0) {
            printSets();
        }
        this.fRangeList = new RangeDescriptor();
        this.fRangeList.fStartChar = 0;
        this.fRangeList.fEndChar = 1114111;
        for (RBBINode rBBINode : this.fRB.fUSetNodes) {
            UnicodeSet unicodeSet = rBBINode.fInputSet;
            int rangeCount = unicodeSet.getRangeCount();
            RangeDescriptor rangeDescriptor = this.fRangeList;
            int i = 0;
            while (i < rangeCount) {
                int rangeStart = unicodeSet.getRangeStart(i);
                int rangeEnd = unicodeSet.getRangeEnd(i);
                while (rangeDescriptor.fEndChar < rangeStart) {
                    rangeDescriptor = rangeDescriptor.fNext;
                }
                if (rangeDescriptor.fStartChar < rangeStart) {
                    rangeDescriptor.split(rangeStart);
                } else {
                    if (rangeDescriptor.fEndChar > rangeEnd) {
                        rangeDescriptor.split(rangeEnd + 1);
                    }
                    if (rangeDescriptor.fIncludesSets.indexOf(rBBINode) == -1) {
                        rangeDescriptor.fIncludesSets.add(rBBINode);
                    }
                    if (rangeEnd == rangeDescriptor.fEndChar) {
                        i++;
                    }
                    rangeDescriptor = rangeDescriptor.fNext;
                }
            }
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("range") >= 0) {
            printRanges();
        }
        for (RangeDescriptor rangeDescriptor2 = this.fRangeList; rangeDescriptor2 != null; rangeDescriptor2 = rangeDescriptor2.fNext) {
            RangeDescriptor rangeDescriptor3 = this.fRangeList;
            while (true) {
                if (rangeDescriptor3 == rangeDescriptor2) {
                    break;
                }
                if (!rangeDescriptor2.fIncludesSets.equals(rangeDescriptor3.fIncludesSets)) {
                    rangeDescriptor3 = rangeDescriptor3.fNext;
                } else {
                    rangeDescriptor2.fNum = rangeDescriptor3.fNum;
                    break;
                }
            }
            if (rangeDescriptor2.fNum == 0) {
                this.fGroupCount++;
                rangeDescriptor2.fNum = this.fGroupCount + 2;
                rangeDescriptor2.setDictionaryFlag();
                addValToSets(rangeDescriptor2.fIncludesSets, this.fGroupCount + 2);
            }
        }
        for (RBBINode rBBINode2 : this.fRB.fUSetNodes) {
            UnicodeSet unicodeSet2 = rBBINode2.fInputSet;
            if (unicodeSet2.contains("eof")) {
                addValToSet(rBBINode2, 1);
            }
            if (unicodeSet2.contains("bof")) {
                addValToSet(rBBINode2, 2);
                this.fSawBOF = true;
            }
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("rgroup") >= 0) {
            printRangeGroups();
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("esets") >= 0) {
            printSets();
        }
        this.fTrie = new Trie2Writable(0, 0);
        for (RangeDescriptor rangeDescriptor4 = this.fRangeList; rangeDescriptor4 != null; rangeDescriptor4 = rangeDescriptor4.fNext) {
            this.fTrie.setRange(rangeDescriptor4.fStartChar, rangeDescriptor4.fEndChar, rangeDescriptor4.fNum, true);
        }
    }

    int getTrieSize() {
        if (this.fFrozenTrie == null) {
            this.fFrozenTrie = this.fTrie.toTrie2_16();
            this.fTrie = null;
        }
        return this.fFrozenTrie.getSerializedLength();
    }

    void serializeTrie(OutputStream outputStream) throws IOException {
        if (this.fFrozenTrie == null) {
            this.fFrozenTrie = this.fTrie.toTrie2_16();
            this.fTrie = null;
        }
        this.fFrozenTrie.serialize(outputStream);
    }

    void addValToSets(List<RBBINode> list, int i) {
        Iterator<RBBINode> it = list.iterator();
        while (it.hasNext()) {
            addValToSet(it.next(), i);
        }
    }

    void addValToSet(RBBINode rBBINode, int i) {
        RBBINode rBBINode2 = new RBBINode(3);
        rBBINode2.fVal = i;
        if (rBBINode.fLeftChild == null) {
            rBBINode.fLeftChild = rBBINode2;
            rBBINode2.fParent = rBBINode;
            return;
        }
        RBBINode rBBINode3 = new RBBINode(9);
        rBBINode3.fLeftChild = rBBINode.fLeftChild;
        rBBINode3.fRightChild = rBBINode2;
        rBBINode3.fLeftChild.fParent = rBBINode3;
        rBBINode3.fRightChild.fParent = rBBINode3;
        rBBINode.fLeftChild = rBBINode3;
        rBBINode3.fParent = rBBINode;
    }

    int getNumCharCategories() {
        return this.fGroupCount + 3;
    }

    boolean sawBOF() {
        return this.fSawBOF;
    }

    int getFirstChar(int i) {
        for (RangeDescriptor rangeDescriptor = this.fRangeList; rangeDescriptor != null; rangeDescriptor = rangeDescriptor.fNext) {
            if (rangeDescriptor.fNum == i) {
                return rangeDescriptor.fStartChar;
            }
        }
        return -1;
    }

    void printRanges() {
        RBBINode rBBINode;
        System.out.print("\n\n Nonoverlapping Ranges ...\n");
        for (RangeDescriptor rangeDescriptor = this.fRangeList; rangeDescriptor != null; rangeDescriptor = rangeDescriptor.fNext) {
            System.out.print(Padder.FALLBACK_PADDING_STRING + rangeDescriptor.fNum + "   " + rangeDescriptor.fStartChar + LanguageTag.SEP + rangeDescriptor.fEndChar);
            for (int i = 0; i < rangeDescriptor.fIncludesSets.size(); i++) {
                String str = "anon";
                RBBINode rBBINode2 = rangeDescriptor.fIncludesSets.get(i).fParent;
                if (rBBINode2 != null && (rBBINode = rBBINode2.fParent) != null && rBBINode.fType == 2) {
                    str = rBBINode.fText;
                }
                System.out.print(str);
                System.out.print("  ");
            }
            System.out.println("");
        }
    }

    void printRangeGroups() {
        RBBINode rBBINode;
        System.out.print("\nRanges grouped by Unicode Set Membership...\n");
        int i = 0;
        for (RangeDescriptor rangeDescriptor = this.fRangeList; rangeDescriptor != null; rangeDescriptor = rangeDescriptor.fNext) {
            int i2 = rangeDescriptor.fNum & 49151;
            if (i2 > i) {
                if (i2 < 10) {
                    System.out.print(Padder.FALLBACK_PADDING_STRING);
                }
                System.out.print(i2 + Padder.FALLBACK_PADDING_STRING);
                if ((rangeDescriptor.fNum & 16384) != 0) {
                    System.out.print(" <DICT> ");
                }
                for (int i3 = 0; i3 < rangeDescriptor.fIncludesSets.size(); i3++) {
                    String str = "anon";
                    RBBINode rBBINode2 = rangeDescriptor.fIncludesSets.get(i3).fParent;
                    if (rBBINode2 != null && (rBBINode = rBBINode2.fParent) != null && rBBINode.fType == 2) {
                        str = rBBINode.fText;
                    }
                    System.out.print(str);
                    System.out.print(Padder.FALLBACK_PADDING_STRING);
                }
                int i4 = 0;
                for (RangeDescriptor rangeDescriptor2 = rangeDescriptor; rangeDescriptor2 != null; rangeDescriptor2 = rangeDescriptor2.fNext) {
                    if (rangeDescriptor2.fNum == rangeDescriptor.fNum) {
                        int i5 = i4 + 1;
                        if (i4 % 5 == 0) {
                            System.out.print("\n    ");
                        }
                        RBBINode.printHex(rangeDescriptor2.fStartChar, -1);
                        System.out.print(LanguageTag.SEP);
                        RBBINode.printHex(rangeDescriptor2.fEndChar, 0);
                        i4 = i5;
                    }
                }
                System.out.print("\n");
                i = i2;
            }
        }
        System.out.print("\n");
    }

    void printSets() {
        RBBINode rBBINode;
        System.out.print("\n\nUnicode Sets List\n------------------\n");
        for (int i = 0; i < this.fRB.fUSetNodes.size(); i++) {
            RBBINode rBBINode2 = this.fRB.fUSetNodes.get(i);
            RBBINode.printInt(2, i);
            String str = "anonymous";
            RBBINode rBBINode3 = rBBINode2.fParent;
            if (rBBINode3 != null && (rBBINode = rBBINode3.fParent) != null && rBBINode.fType == 2) {
                str = rBBINode.fText;
            }
            System.out.print("  " + str);
            System.out.print("   ");
            System.out.print(rBBINode2.fText);
            System.out.print("\n");
            if (rBBINode2.fLeftChild != null) {
                rBBINode2.fLeftChild.printTree(true);
            }
        }
        System.out.print("\n");
    }
}
