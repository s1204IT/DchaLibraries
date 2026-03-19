package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.RBBIRuleParseTable;
import java.text.ParsePosition;
import java.util.HashMap;

class RBBIRuleScanner {
    static final int chLS = 8232;
    static final int chNEL = 133;
    private static final int kStackSize = 100;
    int fCharNum;
    int fLastChar;
    boolean fLookAheadRule;
    int fNextIndex;
    int fNodeStackPtr;
    int fOptionStart;
    boolean fQuoteMode;
    RBBIRuleBuilder fRB;
    boolean fReverseRule;
    int fRuleNum;
    int fScanIndex;
    int fStackPtr;
    RBBISymbolTable fSymbolTable;
    String fVarName;
    private static String gRuleSet_rule_char_pattern = "[^[\\p{Z}\\u0020-\\u007f]-[\\p{L}]-[\\p{N}]]";
    private static String gRuleSet_name_char_pattern = "[_\\p{L}\\p{N}]";
    private static String gRuleSet_digit_char_pattern = "[0-9]";
    private static String gRuleSet_name_start_char_pattern = "[_\\p{L}]";
    private static String gRuleSet_white_space_pattern = "[\\p{Pattern_White_Space}]";
    private static String kAny = "any";
    RBBIRuleChar fC = new RBBIRuleChar();
    short[] fStack = new short[100];
    RBBINode[] fNodeStack = new RBBINode[100];
    HashMap<String, RBBISetTableEl> fSetTable = new HashMap<>();
    UnicodeSet[] fRuleSets = new UnicodeSet[10];
    int fLineNum = 1;

    static class RBBIRuleChar {
        int fChar;
        boolean fEscaped;

        RBBIRuleChar() {
        }
    }

    RBBIRuleScanner(RBBIRuleBuilder rb) {
        this.fRB = rb;
        this.fRuleSets[3] = new UnicodeSet(gRuleSet_rule_char_pattern);
        this.fRuleSets[4] = new UnicodeSet(gRuleSet_white_space_pattern);
        this.fRuleSets[1] = new UnicodeSet(gRuleSet_name_char_pattern);
        this.fRuleSets[2] = new UnicodeSet(gRuleSet_name_start_char_pattern);
        this.fRuleSets[0] = new UnicodeSet(gRuleSet_digit_char_pattern);
        this.fSymbolTable = new RBBISymbolTable(this, rb.fRules);
    }

    boolean doParseActions(int action) {
        switch (action) {
            case 1:
                if (this.fNodeStack[this.fNodeStackPtr].fLeftChild != null) {
                    return true;
                }
                error(66058);
                return false;
            case 2:
                RBBINode n = pushNewNode(0);
                findSetFor(kAny, n, null);
                n.fFirstPos = this.fScanIndex;
                n.fLastPos = this.fNextIndex;
                n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
                return true;
            case 3:
                fixOpStack(1);
                RBBINode startExprNode = this.fNodeStack[this.fNodeStackPtr - 2];
                RBBINode varRefNode = this.fNodeStack[this.fNodeStackPtr - 1];
                RBBINode RHSExprNode = this.fNodeStack[this.fNodeStackPtr];
                RHSExprNode.fFirstPos = startExprNode.fFirstPos;
                RHSExprNode.fLastPos = this.fScanIndex;
                RHSExprNode.fText = this.fRB.fRules.substring(RHSExprNode.fFirstPos, RHSExprNode.fLastPos);
                varRefNode.fLeftChild = RHSExprNode;
                RHSExprNode.fParent = varRefNode;
                this.fSymbolTable.addEntry(varRefNode.fText, varRefNode);
                this.fNodeStackPtr -= 3;
                return true;
            case 4:
                fixOpStack(1);
                if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("rtree") >= 0) {
                    printNodeStack("end of rule");
                }
                Assert.assrt(this.fNodeStackPtr == 1);
                if (this.fLookAheadRule) {
                    RBBINode thisRule = this.fNodeStack[this.fNodeStackPtr];
                    RBBINode endNode = pushNewNode(6);
                    RBBINode catNode = pushNewNode(8);
                    this.fNodeStackPtr -= 2;
                    catNode.fLeftChild = thisRule;
                    catNode.fRightChild = endNode;
                    this.fNodeStack[this.fNodeStackPtr] = catNode;
                    endNode.fVal = this.fRuleNum;
                    endNode.fLookAheadEnd = true;
                }
                int destRules = this.fReverseRule ? 1 : this.fRB.fDefaultTree;
                if (this.fRB.fTreeRoots[destRules] != null) {
                    RBBINode thisRule2 = this.fNodeStack[this.fNodeStackPtr];
                    RBBINode prevRules = this.fRB.fTreeRoots[destRules];
                    RBBINode orNode = pushNewNode(9);
                    orNode.fLeftChild = prevRules;
                    prevRules.fParent = orNode;
                    orNode.fRightChild = thisRule2;
                    thisRule2.fParent = orNode;
                    this.fRB.fTreeRoots[destRules] = orNode;
                } else {
                    this.fRB.fTreeRoots[destRules] = this.fNodeStack[this.fNodeStackPtr];
                }
                this.fReverseRule = false;
                this.fLookAheadRule = false;
                this.fNodeStackPtr = 0;
                return true;
            case 5:
                RBBINode n2 = this.fNodeStack[this.fNodeStackPtr];
                if (n2 == null || n2.fType != 2) {
                    error(66049);
                    return true;
                }
                n2.fLastPos = this.fScanIndex;
                n2.fText = this.fRB.fRules.substring(n2.fFirstPos + 1, n2.fLastPos);
                n2.fLeftChild = this.fSymbolTable.lookupNode(n2.fText);
                return true;
            case 6:
                return false;
            case 7:
                fixOpStack(4);
                RBBINode[] rBBINodeArr = this.fNodeStack;
                int i = this.fNodeStackPtr;
                this.fNodeStackPtr = i - 1;
                RBBINode operandNode = rBBINodeArr[i];
                RBBINode catNode2 = pushNewNode(8);
                catNode2.fLeftChild = operandNode;
                operandNode.fParent = catNode2;
                return true;
            case 8:
            case 13:
                return true;
            case 9:
                fixOpStack(4);
                RBBINode[] rBBINodeArr2 = this.fNodeStack;
                int i2 = this.fNodeStackPtr;
                this.fNodeStackPtr = i2 - 1;
                RBBINode operandNode2 = rBBINodeArr2[i2];
                RBBINode orNode2 = pushNewNode(9);
                orNode2.fLeftChild = operandNode2;
                operandNode2.fParent = orNode2;
                return true;
            case 10:
                fixOpStack(2);
                return true;
            case 11:
                pushNewNode(7);
                this.fRuleNum++;
                return true;
            case 12:
                pushNewNode(15);
                return true;
            case 14:
                String opt = this.fRB.fRules.substring(this.fOptionStart, this.fScanIndex);
                if (opt.equals("chain")) {
                    this.fRB.fChainRules = true;
                    return true;
                }
                if (opt.equals("LBCMNoChain")) {
                    this.fRB.fLBCMNoChain = true;
                    return true;
                }
                if (opt.equals("forward")) {
                    this.fRB.fDefaultTree = 0;
                    return true;
                }
                if (opt.equals("reverse")) {
                    this.fRB.fDefaultTree = 1;
                    return true;
                }
                if (opt.equals("safe_forward")) {
                    this.fRB.fDefaultTree = 2;
                    return true;
                }
                if (opt.equals("safe_reverse")) {
                    this.fRB.fDefaultTree = 3;
                    return true;
                }
                if (opt.equals("lookAheadHardBreak")) {
                    this.fRB.fLookAheadHardBreak = true;
                    return true;
                }
                error(66061);
                return true;
            case 15:
                this.fOptionStart = this.fScanIndex;
                return true;
            case 16:
                this.fReverseRule = true;
                return true;
            case 17:
                RBBINode n3 = pushNewNode(0);
                String s = String.valueOf((char) this.fC.fChar);
                findSetFor(s, n3, null);
                n3.fFirstPos = this.fScanIndex;
                n3.fLastPos = this.fNextIndex;
                n3.fText = this.fRB.fRules.substring(n3.fFirstPos, n3.fLastPos);
                return true;
            case 18:
                error(66052);
                return false;
            case 19:
                error(66054);
                return false;
            case 20:
                scanSet();
                return true;
            case 21:
                RBBINode n4 = pushNewNode(4);
                n4.fVal = this.fRuleNum;
                n4.fFirstPos = this.fScanIndex;
                n4.fLastPos = this.fNextIndex;
                n4.fText = this.fRB.fRules.substring(n4.fFirstPos, n4.fLastPos);
                this.fLookAheadRule = true;
                return true;
            case 22:
                this.fNodeStack[this.fNodeStackPtr - 1].fFirstPos = this.fNextIndex;
                pushNewNode(7);
                return true;
            case 23:
                RBBINode n5 = pushNewNode(5);
                n5.fVal = 0;
                n5.fFirstPos = this.fScanIndex;
                n5.fLastPos = this.fNextIndex;
                return true;
            case 24:
                pushNewNode(2).fFirstPos = this.fScanIndex;
                return true;
            case 25:
                RBBINode n6 = this.fNodeStack[this.fNodeStackPtr];
                int v = UCharacter.digit((char) this.fC.fChar, 10);
                n6.fVal = (n6.fVal * 10) + v;
                return true;
            case 26:
                error(66062);
                return false;
            case 27:
                RBBINode n7 = this.fNodeStack[this.fNodeStackPtr];
                n7.fLastPos = this.fNextIndex;
                n7.fText = this.fRB.fRules.substring(n7.fFirstPos, n7.fLastPos);
                return true;
            case 28:
                RBBINode[] rBBINodeArr3 = this.fNodeStack;
                int i3 = this.fNodeStackPtr;
                this.fNodeStackPtr = i3 - 1;
                RBBINode operandNode3 = rBBINodeArr3[i3];
                RBBINode plusNode = pushNewNode(11);
                plusNode.fLeftChild = operandNode3;
                operandNode3.fParent = plusNode;
                return true;
            case 29:
                RBBINode[] rBBINodeArr4 = this.fNodeStack;
                int i4 = this.fNodeStackPtr;
                this.fNodeStackPtr = i4 - 1;
                RBBINode operandNode4 = rBBINodeArr4[i4];
                RBBINode qNode = pushNewNode(12);
                qNode.fLeftChild = operandNode4;
                operandNode4.fParent = qNode;
                return true;
            case 30:
                RBBINode[] rBBINodeArr5 = this.fNodeStack;
                int i5 = this.fNodeStackPtr;
                this.fNodeStackPtr = i5 - 1;
                RBBINode operandNode5 = rBBINodeArr5[i5];
                RBBINode starNode = pushNewNode(10);
                starNode.fLeftChild = operandNode5;
                operandNode5.fParent = starNode;
                return true;
            case 31:
                error(66052);
                return true;
            default:
                error(66049);
                return false;
        }
    }

    void error(int e) {
        String s = "Error " + e + " at line " + this.fLineNum + " column " + this.fCharNum;
        IllegalArgumentException ex = new IllegalArgumentException(s);
        throw ex;
    }

    void fixOpStack(int p) {
        RBBINode n;
        while (true) {
            n = this.fNodeStack[this.fNodeStackPtr - 1];
            if (n.fPrecedence == 0) {
                System.out.print("RBBIRuleScanner.fixOpStack, bad operator node");
                error(66049);
                return;
            } else {
                if (n.fPrecedence < p || n.fPrecedence <= 2) {
                    break;
                }
                n.fRightChild = this.fNodeStack[this.fNodeStackPtr];
                this.fNodeStack[this.fNodeStackPtr].fParent = n;
                this.fNodeStackPtr--;
            }
        }
        if (p > 2) {
            return;
        }
        if (n.fPrecedence != p) {
            error(66056);
        }
        this.fNodeStack[this.fNodeStackPtr - 1] = this.fNodeStack[this.fNodeStackPtr];
        this.fNodeStackPtr--;
    }

    static class RBBISetTableEl {
        String key;
        RBBINode val;

        RBBISetTableEl() {
        }
    }

    void findSetFor(String s, RBBINode node, UnicodeSet setToAdopt) {
        RBBISetTableEl el = this.fSetTable.get(s);
        if (el != null) {
            node.fLeftChild = el.val;
            Assert.assrt(node.fLeftChild.fType == 1);
            return;
        }
        if (setToAdopt == null) {
            if (s.equals(kAny)) {
                setToAdopt = new UnicodeSet(0, 1114111);
            } else {
                int c = UTF16.charAt(s, 0);
                setToAdopt = new UnicodeSet(c, c);
            }
        }
        RBBINode usetNode = new RBBINode(1);
        usetNode.fInputSet = setToAdopt;
        usetNode.fParent = node;
        node.fLeftChild = usetNode;
        usetNode.fText = s;
        this.fRB.fUSetNodes.add(usetNode);
        RBBISetTableEl el2 = new RBBISetTableEl();
        el2.key = s;
        el2.val = usetNode;
        this.fSetTable.put(el2.key, el2);
    }

    static String stripRules(String rules) {
        int idx;
        int idx2;
        StringBuilder strippedRules = new StringBuilder();
        int rulesLength = rules.length();
        for (int idx3 = 0; idx3 < rulesLength; idx3 = idx) {
            idx = idx3 + 1;
            char ch = rules.charAt(idx3);
            if (ch == '#') {
                while (true) {
                    idx2 = idx;
                    if (idx2 >= rulesLength || ch == '\r' || ch == '\n' || ch == 133) {
                        break;
                    }
                    idx = idx2 + 1;
                    ch = rules.charAt(idx2);
                }
                idx = idx2;
            }
            if (!UCharacter.isISOControl(ch)) {
                strippedRules.append(ch);
            }
        }
        return strippedRules.toString();
    }

    int nextCharLL() {
        if (this.fNextIndex >= this.fRB.fRules.length()) {
            return -1;
        }
        int ch = UTF16.charAt(this.fRB.fRules, this.fNextIndex);
        this.fNextIndex = UTF16.moveCodePointOffset(this.fRB.fRules, this.fNextIndex, 1);
        if (ch == 13 || ch == 133 || ch == chLS || (ch == 10 && this.fLastChar != 13)) {
            this.fLineNum++;
            this.fCharNum = 0;
            if (this.fQuoteMode) {
                error(66057);
                this.fQuoteMode = false;
            }
        } else if (ch != 10) {
            this.fCharNum++;
        }
        this.fLastChar = ch;
        return ch;
    }

    void nextChar(RBBIRuleChar c) {
        this.fScanIndex = this.fNextIndex;
        c.fChar = nextCharLL();
        c.fEscaped = false;
        if (c.fChar == 39) {
            if (UTF16.charAt(this.fRB.fRules, this.fNextIndex) == 39) {
                c.fChar = nextCharLL();
                c.fEscaped = true;
            } else {
                this.fQuoteMode = this.fQuoteMode ? false : true;
                if (this.fQuoteMode) {
                    c.fChar = 40;
                } else {
                    c.fChar = 41;
                }
                c.fEscaped = false;
                return;
            }
        }
        if (this.fQuoteMode) {
            c.fEscaped = true;
            return;
        }
        if (c.fChar == 35) {
            do {
                c.fChar = nextCharLL();
                if (c.fChar == -1 || c.fChar == 13 || c.fChar == 10 || c.fChar == 133) {
                    break;
                }
            } while (c.fChar != chLS);
        }
        if (c.fChar == -1 || c.fChar != 92) {
            return;
        }
        c.fEscaped = true;
        int[] unescapeIndex = {this.fNextIndex};
        c.fChar = Utility.unescapeAt(this.fRB.fRules, unescapeIndex);
        if (unescapeIndex[0] == this.fNextIndex) {
            error(66050);
        }
        this.fCharNum += unescapeIndex[0] - this.fNextIndex;
        this.fNextIndex = unescapeIndex[0];
    }

    void parse() {
        RBBIRuleParseTable.RBBIRuleTableElement tableEl;
        short s = 1;
        nextChar(this.fC);
        while (s != 0) {
            RBBIRuleParseTable.RBBIRuleTableElement tableEl2 = RBBIRuleParseTable.gRuleParseStateTable[s];
            if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                System.out.println("char, line, col = ('" + ((char) this.fC.fChar) + "', " + this.fLineNum + ", " + this.fCharNum + "    state = " + tableEl2.fStateName);
            }
            int tableRow = s;
            while (true) {
                tableEl = RBBIRuleParseTable.gRuleParseStateTable[tableRow];
                if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                    System.out.print(".");
                }
                if ((tableEl.fCharClass < 127 && !this.fC.fEscaped && tableEl.fCharClass == this.fC.fChar) || tableEl.fCharClass == 255 || ((tableEl.fCharClass == 254 && this.fC.fEscaped) || ((tableEl.fCharClass == 253 && this.fC.fEscaped && (this.fC.fChar == 80 || this.fC.fChar == 112)) || (tableEl.fCharClass == 252 && this.fC.fChar == -1)))) {
                    break;
                }
                if (tableEl.fCharClass >= 128 && tableEl.fCharClass < 240 && !this.fC.fEscaped && this.fC.fChar != -1) {
                    UnicodeSet uniset = this.fRuleSets[tableEl.fCharClass - 128];
                    if (uniset.contains(this.fC.fChar)) {
                        break;
                    }
                }
                tableRow++;
            }
            if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                System.out.println("");
            }
            if (!doParseActions(tableEl.fAction)) {
                break;
            }
            if (tableEl.fPushState != 0) {
                this.fStackPtr++;
                if (this.fStackPtr >= 100) {
                    System.out.println("RBBIRuleScanner.parse() - state stack overflow.");
                    error(66049);
                }
                this.fStack[this.fStackPtr] = tableEl.fPushState;
            }
            if (tableEl.fNextChar) {
                nextChar(this.fC);
            }
            if (tableEl.fNextState != 255) {
                s = tableEl.fNextState;
            } else {
                s = this.fStack[this.fStackPtr];
                this.fStackPtr--;
                if (this.fStackPtr < 0) {
                    System.out.println("RBBIRuleScanner.parse() - state stack underflow.");
                    error(66049);
                }
            }
        }
        if (this.fRB.fTreeRoots[1] == null) {
            this.fRB.fTreeRoots[1] = pushNewNode(10);
            RBBINode operand = pushNewNode(0);
            findSetFor(kAny, operand, null);
            this.fRB.fTreeRoots[1].fLeftChild = operand;
            operand.fParent = this.fRB.fTreeRoots[1];
            this.fNodeStackPtr -= 2;
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("symbols") >= 0) {
            this.fSymbolTable.rbbiSymtablePrint();
        }
        if (this.fRB.fDebugEnv == null || this.fRB.fDebugEnv.indexOf("ptree") < 0) {
            return;
        }
        System.out.println("Completed Forward Rules Parse Tree...");
        this.fRB.fTreeRoots[0].printTree(true);
        System.out.println("\nCompleted Reverse Rules Parse Tree...");
        this.fRB.fTreeRoots[1].printTree(true);
        System.out.println("\nCompleted Safe Point Forward Rules Parse Tree...");
        if (this.fRB.fTreeRoots[2] == null) {
            System.out.println("  -- null -- ");
        } else {
            this.fRB.fTreeRoots[2].printTree(true);
        }
        System.out.println("\nCompleted Safe Point Reverse Rules Parse Tree...");
        if (this.fRB.fTreeRoots[3] == null) {
            System.out.println("  -- null -- ");
        } else {
            this.fRB.fTreeRoots[3].printTree(true);
        }
    }

    void printNodeStack(String title) {
        System.out.println(title + ".  Dumping node stack...\n");
        for (int i = this.fNodeStackPtr; i > 0; i--) {
            this.fNodeStack[i].printTree(true);
        }
    }

    RBBINode pushNewNode(int nodeType) {
        this.fNodeStackPtr++;
        if (this.fNodeStackPtr >= 100) {
            System.out.println("RBBIRuleScanner.pushNewNode - stack overflow.");
            error(66049);
        }
        this.fNodeStack[this.fNodeStackPtr] = new RBBINode(nodeType);
        return this.fNodeStack[this.fNodeStackPtr];
    }

    void scanSet() {
        UnicodeSet uset = null;
        ParsePosition pos = new ParsePosition(this.fScanIndex);
        int startPos = this.fScanIndex;
        try {
            UnicodeSet uset2 = new UnicodeSet(this.fRB.fRules, pos, this.fSymbolTable, 1);
            uset = uset2;
        } catch (Exception e) {
            error(66063);
        }
        if (uset.isEmpty()) {
            error(66060);
        }
        int i = pos.getIndex();
        while (this.fNextIndex < i) {
            nextCharLL();
        }
        RBBINode n = pushNewNode(0);
        n.fFirstPos = startPos;
        n.fLastPos = this.fNextIndex;
        n.fText = this.fRB.fRules.substring(n.fFirstPos, n.fLastPos);
        findSetFor(n.fText, n, uset);
    }
}
