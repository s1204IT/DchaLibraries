package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.number.Padder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class RBBINode {
    static final int endMark = 6;
    static int gLastSerial = 0;
    static final int leafChar = 3;
    static final int lookAhead = 4;
    static final int nodeTypeLimit = 16;
    static final String[] nodeTypeNames = {"setRef", "uset", "varRef", "leafChar", "lookAhead", "tag", "endMark", "opStart", "opCat", "opOr", "opStar", "opPlus", "opQuestion", "opBreak", "opReverse", "opLParen"};
    static final int opBreak = 13;
    static final int opCat = 8;
    static final int opLParen = 15;
    static final int opOr = 9;
    static final int opPlus = 11;
    static final int opQuestion = 12;
    static final int opReverse = 14;
    static final int opStar = 10;
    static final int opStart = 7;
    static final int precLParen = 2;
    static final int precOpCat = 4;
    static final int precOpOr = 3;
    static final int precStart = 1;
    static final int precZero = 0;
    static final int setRef = 0;
    static final int tag = 5;
    static final int uset = 1;
    static final int varRef = 2;
    boolean fChainIn;
    int fFirstPos;
    Set<RBBINode> fFirstPosSet;
    Set<RBBINode> fFollowPos;
    UnicodeSet fInputSet;
    int fLastPos;
    Set<RBBINode> fLastPosSet;
    RBBINode fLeftChild;
    boolean fLookAheadEnd;
    boolean fNullable;
    RBBINode fParent;
    int fPrecedence;
    RBBINode fRightChild;
    boolean fRuleRoot;
    int fSerialNum;
    String fText;
    int fType;
    int fVal;

    RBBINode(int i) {
        boolean z;
        this.fPrecedence = 0;
        if (i >= 16) {
            z = false;
        } else {
            z = true;
        }
        Assert.assrt(z);
        int i2 = gLastSerial + 1;
        gLastSerial = i2;
        this.fSerialNum = i2;
        this.fType = i;
        this.fFirstPosSet = new HashSet();
        this.fLastPosSet = new HashSet();
        this.fFollowPos = new HashSet();
        if (i == 8) {
            this.fPrecedence = 4;
            return;
        }
        if (i == 9) {
            this.fPrecedence = 3;
            return;
        }
        if (i == 7) {
            this.fPrecedence = 1;
        } else if (i == 15) {
            this.fPrecedence = 2;
        } else {
            this.fPrecedence = 0;
        }
    }

    RBBINode(RBBINode rBBINode) {
        this.fPrecedence = 0;
        int i = gLastSerial + 1;
        gLastSerial = i;
        this.fSerialNum = i;
        this.fType = rBBINode.fType;
        this.fInputSet = rBBINode.fInputSet;
        this.fPrecedence = rBBINode.fPrecedence;
        this.fText = rBBINode.fText;
        this.fFirstPos = rBBINode.fFirstPos;
        this.fLastPos = rBBINode.fLastPos;
        this.fNullable = rBBINode.fNullable;
        this.fVal = rBBINode.fVal;
        this.fRuleRoot = false;
        this.fChainIn = rBBINode.fChainIn;
        this.fFirstPosSet = new HashSet(rBBINode.fFirstPosSet);
        this.fLastPosSet = new HashSet(rBBINode.fLastPosSet);
        this.fFollowPos = new HashSet(rBBINode.fFollowPos);
    }

    RBBINode cloneTree() {
        if (this.fType == 2) {
            return this.fLeftChild.cloneTree();
        }
        if (this.fType != 1) {
            RBBINode rBBINode = new RBBINode(this);
            if (this.fLeftChild != null) {
                rBBINode.fLeftChild = this.fLeftChild.cloneTree();
                rBBINode.fLeftChild.fParent = rBBINode;
            }
            if (this.fRightChild != null) {
                rBBINode.fRightChild = this.fRightChild.cloneTree();
                rBBINode.fRightChild.fParent = rBBINode;
                return rBBINode;
            }
            return rBBINode;
        }
        return this;
    }

    RBBINode flattenVariables() {
        if (this.fType == 2) {
            RBBINode rBBINodeCloneTree = this.fLeftChild.cloneTree();
            rBBINodeCloneTree.fRuleRoot = this.fRuleRoot;
            rBBINodeCloneTree.fChainIn = this.fChainIn;
            return rBBINodeCloneTree;
        }
        if (this.fLeftChild != null) {
            this.fLeftChild = this.fLeftChild.flattenVariables();
            this.fLeftChild.fParent = this;
        }
        if (this.fRightChild != null) {
            this.fRightChild = this.fRightChild.flattenVariables();
            this.fRightChild.fParent = this;
        }
        return this;
    }

    void flattenSets() {
        Assert.assrt(this.fType != 0);
        if (this.fLeftChild != null) {
            if (this.fLeftChild.fType == 0) {
                this.fLeftChild = this.fLeftChild.fLeftChild.fLeftChild.cloneTree();
                this.fLeftChild.fParent = this;
            } else {
                this.fLeftChild.flattenSets();
            }
        }
        if (this.fRightChild != null) {
            if (this.fRightChild.fType == 0) {
                this.fRightChild = this.fRightChild.fLeftChild.fLeftChild.cloneTree();
                this.fRightChild.fParent = this;
            } else {
                this.fRightChild.flattenSets();
            }
        }
    }

    void findNodes(List<RBBINode> list, int i) {
        if (this.fType == i) {
            list.add(this);
        }
        if (this.fLeftChild != null) {
            this.fLeftChild.findNodes(list, i);
        }
        if (this.fRightChild != null) {
            this.fRightChild.findNodes(list, i);
        }
    }

    static void printNode(RBBINode rBBINode) {
        if (rBBINode == null) {
            System.out.print(" -- null --\n");
        } else {
            printInt(rBBINode.fSerialNum, 10);
            printString(nodeTypeNames[rBBINode.fType], 11);
            printInt(rBBINode.fParent == null ? 0 : rBBINode.fParent.fSerialNum, 11);
            printInt(rBBINode.fLeftChild == null ? 0 : rBBINode.fLeftChild.fSerialNum, 11);
            printInt(rBBINode.fRightChild != null ? rBBINode.fRightChild.fSerialNum : 0, 12);
            printInt(rBBINode.fFirstPos, 12);
            printInt(rBBINode.fVal, 7);
            if (rBBINode.fType == 2) {
                System.out.print(Padder.FALLBACK_PADDING_STRING + rBBINode.fText);
            }
        }
        System.out.println("");
    }

    static void printString(String str, int i) {
        for (int i2 = i; i2 < 0; i2++) {
            System.out.print(' ');
        }
        for (int length = str.length(); length < i; length++) {
            System.out.print(' ');
        }
        System.out.print(str);
    }

    static void printInt(int i, int i2) {
        String string = Integer.toString(i);
        printString(string, Math.max(i2, string.length() + 1));
    }

    static void printHex(int i, int i2) {
        String string = Integer.toString(i, 16);
        printString("00000".substring(0, Math.max(0, 5 - string.length())) + string, i2);
    }

    void printTree(boolean z) {
        if (z) {
            System.out.println("-------------------------------------------------------------------");
            System.out.println("    Serial       type     Parent  LeftChild  RightChild    position  value");
        }
        printNode(this);
        if (this.fType != 2) {
            if (this.fLeftChild != null) {
                this.fLeftChild.printTree(false);
            }
            if (this.fRightChild != null) {
                this.fRightChild.printTree(false);
            }
        }
    }
}
