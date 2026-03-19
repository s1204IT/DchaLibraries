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
    boolean fNoChainInRule;
    int fNodeStackPtr;
    int fOptionStart;
    boolean fQuoteMode;
    RBBIRuleBuilder fRB;
    boolean fReverseRule;
    int fRuleNum;
    int fScanIndex;
    int fStackPtr;
    RBBISymbolTable fSymbolTable;
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

    RBBIRuleScanner(RBBIRuleBuilder rBBIRuleBuilder) {
        this.fRB = rBBIRuleBuilder;
        this.fRuleSets[3] = new UnicodeSet(gRuleSet_rule_char_pattern);
        this.fRuleSets[4] = new UnicodeSet(gRuleSet_white_space_pattern);
        this.fRuleSets[1] = new UnicodeSet(gRuleSet_name_char_pattern);
        this.fRuleSets[2] = new UnicodeSet(gRuleSet_name_start_char_pattern);
        this.fRuleSets[0] = new UnicodeSet(gRuleSet_digit_char_pattern);
        this.fSymbolTable = new RBBISymbolTable(this);
    }

    boolean doParseActions(int i) {
        int i2;
        switch (i) {
            case 1:
                if (this.fNodeStack[this.fNodeStackPtr].fLeftChild == null) {
                    error(66058);
                    return false;
                }
                break;
            case 2:
                RBBINode rBBINodePushNewNode = pushNewNode(0);
                findSetFor(kAny, rBBINodePushNewNode, null);
                rBBINodePushNewNode.fFirstPos = this.fScanIndex;
                rBBINodePushNewNode.fLastPos = this.fNextIndex;
                rBBINodePushNewNode.fText = this.fRB.fRules.substring(rBBINodePushNewNode.fFirstPos, rBBINodePushNewNode.fLastPos);
                break;
            case 3:
                fixOpStack(1);
                RBBINode rBBINode = this.fNodeStack[this.fNodeStackPtr - 2];
                RBBINode rBBINode2 = this.fNodeStack[this.fNodeStackPtr - 1];
                RBBINode rBBINode3 = this.fNodeStack[this.fNodeStackPtr];
                rBBINode3.fFirstPos = rBBINode.fFirstPos;
                rBBINode3.fLastPos = this.fScanIndex;
                rBBINode3.fText = this.fRB.fRules.substring(rBBINode3.fFirstPos, rBBINode3.fLastPos);
                rBBINode2.fLeftChild = rBBINode3;
                rBBINode3.fParent = rBBINode2;
                this.fSymbolTable.addEntry(rBBINode2.fText, rBBINode2);
                this.fNodeStackPtr -= 3;
                break;
            case 4:
                fixOpStack(1);
                if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("rtree") >= 0) {
                    printNodeStack("end of rule");
                }
                Assert.assrt(this.fNodeStackPtr == 1);
                RBBINode rBBINode4 = this.fNodeStack[this.fNodeStackPtr];
                if (this.fLookAheadRule) {
                    RBBINode rBBINodePushNewNode2 = pushNewNode(6);
                    RBBINode rBBINodePushNewNode3 = pushNewNode(8);
                    this.fNodeStackPtr -= 2;
                    rBBINodePushNewNode3.fLeftChild = rBBINode4;
                    rBBINodePushNewNode3.fRightChild = rBBINodePushNewNode2;
                    this.fNodeStack[this.fNodeStackPtr] = rBBINodePushNewNode3;
                    rBBINodePushNewNode2.fVal = this.fRuleNum;
                    rBBINodePushNewNode2.fLookAheadEnd = true;
                    rBBINode4 = rBBINodePushNewNode3;
                }
                rBBINode4.fRuleRoot = true;
                if (this.fRB.fChainRules && !this.fNoChainInRule) {
                    rBBINode4.fChainIn = true;
                }
                if (!this.fReverseRule) {
                    i2 = this.fRB.fDefaultTree;
                } else {
                    i2 = 1;
                }
                if (this.fRB.fTreeRoots[i2] != null) {
                    RBBINode rBBINode5 = this.fNodeStack[this.fNodeStackPtr];
                    RBBINode rBBINode6 = this.fRB.fTreeRoots[i2];
                    RBBINode rBBINodePushNewNode4 = pushNewNode(9);
                    rBBINodePushNewNode4.fLeftChild = rBBINode6;
                    rBBINode6.fParent = rBBINodePushNewNode4;
                    rBBINodePushNewNode4.fRightChild = rBBINode5;
                    rBBINode5.fParent = rBBINodePushNewNode4;
                    this.fRB.fTreeRoots[i2] = rBBINodePushNewNode4;
                } else {
                    this.fRB.fTreeRoots[i2] = this.fNodeStack[this.fNodeStackPtr];
                }
                this.fReverseRule = false;
                this.fLookAheadRule = false;
                this.fNoChainInRule = false;
                this.fNodeStackPtr = 0;
                break;
            case 5:
                RBBINode rBBINode7 = this.fNodeStack[this.fNodeStackPtr];
                if (rBBINode7 == null || rBBINode7.fType != 2) {
                    error(66049);
                } else {
                    rBBINode7.fLastPos = this.fScanIndex;
                    rBBINode7.fText = this.fRB.fRules.substring(rBBINode7.fFirstPos + 1, rBBINode7.fLastPos);
                    rBBINode7.fLeftChild = this.fSymbolTable.lookupNode(rBBINode7.fText);
                }
                break;
            case 6:
                return false;
            case 7:
                fixOpStack(4);
                RBBINode[] rBBINodeArr = this.fNodeStack;
                int i3 = this.fNodeStackPtr;
                this.fNodeStackPtr = i3 - 1;
                RBBINode rBBINode8 = rBBINodeArr[i3];
                RBBINode rBBINodePushNewNode5 = pushNewNode(8);
                rBBINodePushNewNode5.fLeftChild = rBBINode8;
                rBBINode8.fParent = rBBINodePushNewNode5;
                break;
            case 8:
            case 13:
                break;
            case 9:
                fixOpStack(4);
                RBBINode[] rBBINodeArr2 = this.fNodeStack;
                int i4 = this.fNodeStackPtr;
                this.fNodeStackPtr = i4 - 1;
                RBBINode rBBINode9 = rBBINodeArr2[i4];
                RBBINode rBBINodePushNewNode6 = pushNewNode(9);
                rBBINodePushNewNode6.fLeftChild = rBBINode9;
                rBBINode9.fParent = rBBINodePushNewNode6;
                break;
            case 10:
                fixOpStack(2);
                break;
            case 11:
                pushNewNode(7);
                this.fRuleNum++;
                break;
            case 12:
                pushNewNode(15);
                break;
            case 14:
                this.fNoChainInRule = true;
                break;
            case 15:
                String strSubstring = this.fRB.fRules.substring(this.fOptionStart, this.fScanIndex);
                if (strSubstring.equals("chain")) {
                    this.fRB.fChainRules = true;
                } else if (strSubstring.equals("LBCMNoChain")) {
                    this.fRB.fLBCMNoChain = true;
                } else if (strSubstring.equals("forward")) {
                    this.fRB.fDefaultTree = 0;
                } else if (strSubstring.equals("reverse")) {
                    this.fRB.fDefaultTree = 1;
                } else if (strSubstring.equals("safe_forward")) {
                    this.fRB.fDefaultTree = 2;
                } else if (strSubstring.equals("safe_reverse")) {
                    this.fRB.fDefaultTree = 3;
                } else if (strSubstring.equals("lookAheadHardBreak")) {
                    this.fRB.fLookAheadHardBreak = true;
                } else if (strSubstring.equals("quoted_literals_only")) {
                    this.fRuleSets[3].clear();
                } else if (strSubstring.equals("unquoted_literals")) {
                    this.fRuleSets[3].applyPattern(gRuleSet_rule_char_pattern);
                } else {
                    error(66061);
                }
                break;
            case 16:
                this.fOptionStart = this.fScanIndex;
                break;
            case 17:
                this.fReverseRule = true;
                break;
            case 18:
                RBBINode rBBINodePushNewNode7 = pushNewNode(0);
                findSetFor(String.valueOf((char) this.fC.fChar), rBBINodePushNewNode7, null);
                rBBINodePushNewNode7.fFirstPos = this.fScanIndex;
                rBBINodePushNewNode7.fLastPos = this.fNextIndex;
                rBBINodePushNewNode7.fText = this.fRB.fRules.substring(rBBINodePushNewNode7.fFirstPos, rBBINodePushNewNode7.fLastPos);
                break;
            case 19:
                error(66052);
                return false;
            case 20:
                error(66054);
                return false;
            case 21:
                scanSet();
                break;
            case 22:
                RBBINode rBBINodePushNewNode8 = pushNewNode(4);
                rBBINodePushNewNode8.fVal = this.fRuleNum;
                rBBINodePushNewNode8.fFirstPos = this.fScanIndex;
                rBBINodePushNewNode8.fLastPos = this.fNextIndex;
                rBBINodePushNewNode8.fText = this.fRB.fRules.substring(rBBINodePushNewNode8.fFirstPos, rBBINodePushNewNode8.fLastPos);
                this.fLookAheadRule = true;
                break;
            case 23:
                this.fNodeStack[this.fNodeStackPtr - 1].fFirstPos = this.fNextIndex;
                pushNewNode(7);
                break;
            case 24:
                RBBINode rBBINodePushNewNode9 = pushNewNode(5);
                rBBINodePushNewNode9.fVal = 0;
                rBBINodePushNewNode9.fFirstPos = this.fScanIndex;
                rBBINodePushNewNode9.fLastPos = this.fNextIndex;
                break;
            case 25:
                pushNewNode(2).fFirstPos = this.fScanIndex;
                break;
            case 26:
                RBBINode rBBINode10 = this.fNodeStack[this.fNodeStackPtr];
                rBBINode10.fVal = (rBBINode10.fVal * 10) + UCharacter.digit((char) this.fC.fChar, 10);
                break;
            case 27:
                error(66062);
                return false;
            case 28:
                RBBINode rBBINode11 = this.fNodeStack[this.fNodeStackPtr];
                rBBINode11.fLastPos = this.fNextIndex;
                rBBINode11.fText = this.fRB.fRules.substring(rBBINode11.fFirstPos, rBBINode11.fLastPos);
                break;
            case 29:
                RBBINode[] rBBINodeArr3 = this.fNodeStack;
                int i5 = this.fNodeStackPtr;
                this.fNodeStackPtr = i5 - 1;
                RBBINode rBBINode12 = rBBINodeArr3[i5];
                RBBINode rBBINodePushNewNode10 = pushNewNode(11);
                rBBINodePushNewNode10.fLeftChild = rBBINode12;
                rBBINode12.fParent = rBBINodePushNewNode10;
                break;
            case 30:
                RBBINode[] rBBINodeArr4 = this.fNodeStack;
                int i6 = this.fNodeStackPtr;
                this.fNodeStackPtr = i6 - 1;
                RBBINode rBBINode13 = rBBINodeArr4[i6];
                RBBINode rBBINodePushNewNode11 = pushNewNode(12);
                rBBINodePushNewNode11.fLeftChild = rBBINode13;
                rBBINode13.fParent = rBBINodePushNewNode11;
                break;
            case 31:
                RBBINode[] rBBINodeArr5 = this.fNodeStack;
                int i7 = this.fNodeStackPtr;
                this.fNodeStackPtr = i7 - 1;
                RBBINode rBBINode14 = rBBINodeArr5[i7];
                RBBINode rBBINodePushNewNode12 = pushNewNode(10);
                rBBINodePushNewNode12.fLeftChild = rBBINode14;
                rBBINode14.fParent = rBBINodePushNewNode12;
                break;
            case 32:
                error(66052);
                break;
            default:
                error(66049);
                return false;
        }
        return true;
    }

    void error(int i) {
        throw new IllegalArgumentException("Error " + i + " at line " + this.fLineNum + " column " + this.fCharNum);
    }

    void fixOpStack(int i) {
        RBBINode rBBINode;
        while (true) {
            rBBINode = this.fNodeStack[this.fNodeStackPtr - 1];
            if (rBBINode.fPrecedence == 0) {
                System.out.print("RBBIRuleScanner.fixOpStack, bad operator node");
                error(66049);
                return;
            } else {
                if (rBBINode.fPrecedence < i || rBBINode.fPrecedence <= 2) {
                    break;
                }
                rBBINode.fRightChild = this.fNodeStack[this.fNodeStackPtr];
                this.fNodeStack[this.fNodeStackPtr].fParent = rBBINode;
                this.fNodeStackPtr--;
            }
        }
        if (i <= 2) {
            if (rBBINode.fPrecedence != i) {
                error(66056);
            }
            this.fNodeStack[this.fNodeStackPtr - 1] = this.fNodeStack[this.fNodeStackPtr];
            this.fNodeStackPtr--;
        }
    }

    static class RBBISetTableEl {
        String key;
        RBBINode val;

        RBBISetTableEl() {
        }
    }

    void findSetFor(String str, RBBINode rBBINode, UnicodeSet unicodeSet) {
        RBBISetTableEl rBBISetTableEl = this.fSetTable.get(str);
        if (rBBISetTableEl != null) {
            rBBINode.fLeftChild = rBBISetTableEl.val;
            Assert.assrt(rBBINode.fLeftChild.fType == 1);
            return;
        }
        if (unicodeSet == null) {
            if (str.equals(kAny)) {
                unicodeSet = new UnicodeSet(0, 1114111);
            } else {
                int iCharAt = UTF16.charAt(str, 0);
                unicodeSet = new UnicodeSet(iCharAt, iCharAt);
            }
        }
        RBBINode rBBINode2 = new RBBINode(1);
        rBBINode2.fInputSet = unicodeSet;
        rBBINode2.fParent = rBBINode;
        rBBINode.fLeftChild = rBBINode2;
        rBBINode2.fText = str;
        this.fRB.fUSetNodes.add(rBBINode2);
        RBBISetTableEl rBBISetTableEl2 = new RBBISetTableEl();
        rBBISetTableEl2.key = str;
        rBBISetTableEl2.val = rBBINode2;
        this.fSetTable.put(rBBISetTableEl2.key, rBBISetTableEl2);
    }

    static String stripRules(String str) {
        int i;
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i2 = 0; i2 < length; i2 = i) {
            i = i2 + 1;
            char cCharAt = str.charAt(i2);
            if (cCharAt == '#') {
                while (i < length && cCharAt != '\r' && cCharAt != '\n' && cCharAt != 133) {
                    int i3 = i + 1;
                    char cCharAt2 = str.charAt(i);
                    i = i3;
                    cCharAt = cCharAt2;
                }
            }
            if (!UCharacter.isISOControl(cCharAt)) {
                sb.append(cCharAt);
            }
        }
        return sb.toString();
    }

    int nextCharLL() {
        if (this.fNextIndex >= this.fRB.fRules.length()) {
            return -1;
        }
        int iCharAt = UTF16.charAt(this.fRB.fRules, this.fNextIndex);
        this.fNextIndex = UTF16.moveCodePointOffset(this.fRB.fRules, this.fNextIndex, 1);
        if (iCharAt == 13 || iCharAt == 133 || iCharAt == chLS || (iCharAt == 10 && this.fLastChar != 13)) {
            this.fLineNum++;
            this.fCharNum = 0;
            if (this.fQuoteMode) {
                error(66057);
                this.fQuoteMode = false;
            }
        } else if (iCharAt != 10) {
            this.fCharNum++;
        }
        this.fLastChar = iCharAt;
        return iCharAt;
    }

    void nextChar(RBBIRuleChar rBBIRuleChar) {
        this.fScanIndex = this.fNextIndex;
        rBBIRuleChar.fChar = nextCharLL();
        rBBIRuleChar.fEscaped = false;
        if (rBBIRuleChar.fChar == 39) {
            if (UTF16.charAt(this.fRB.fRules, this.fNextIndex) == 39) {
                rBBIRuleChar.fChar = nextCharLL();
                rBBIRuleChar.fEscaped = true;
            } else {
                this.fQuoteMode = !this.fQuoteMode;
                if (this.fQuoteMode) {
                    rBBIRuleChar.fChar = 40;
                } else {
                    rBBIRuleChar.fChar = 41;
                }
                rBBIRuleChar.fEscaped = false;
                return;
            }
        }
        if (this.fQuoteMode) {
            rBBIRuleChar.fEscaped = true;
            return;
        }
        if (rBBIRuleChar.fChar == 35) {
            do {
                rBBIRuleChar.fChar = nextCharLL();
                if (rBBIRuleChar.fChar == -1 || rBBIRuleChar.fChar == 13 || rBBIRuleChar.fChar == 10 || rBBIRuleChar.fChar == 133) {
                    break;
                }
            } while (rBBIRuleChar.fChar != chLS);
        }
        if (rBBIRuleChar.fChar != -1 && rBBIRuleChar.fChar == 92) {
            rBBIRuleChar.fEscaped = true;
            int[] iArr = {this.fNextIndex};
            rBBIRuleChar.fChar = Utility.unescapeAt(this.fRB.fRules, iArr);
            if (iArr[0] == this.fNextIndex) {
                error(66050);
            }
            this.fCharNum += iArr[0] - this.fNextIndex;
            this.fNextIndex = iArr[0];
        }
    }

    void parse() {
        RBBIRuleParseTable.RBBIRuleTableElement rBBIRuleTableElement;
        nextChar(this.fC);
        short s = 1;
        while (s != 0) {
            RBBIRuleParseTable.RBBIRuleTableElement rBBIRuleTableElement2 = RBBIRuleParseTable.gRuleParseStateTable[s];
            if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                System.out.println("char, line, col = ('" + ((char) this.fC.fChar) + "', " + this.fLineNum + ", " + this.fCharNum + "    state = " + rBBIRuleTableElement2.fStateName);
            }
            while (true) {
                rBBIRuleTableElement = RBBIRuleParseTable.gRuleParseStateTable[s];
                if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                    System.out.print(".");
                }
                if ((rBBIRuleTableElement.fCharClass < 127 && !this.fC.fEscaped && rBBIRuleTableElement.fCharClass == this.fC.fChar) || rBBIRuleTableElement.fCharClass == 255 || ((rBBIRuleTableElement.fCharClass == 254 && this.fC.fEscaped) || ((rBBIRuleTableElement.fCharClass == 253 && this.fC.fEscaped && (this.fC.fChar == 80 || this.fC.fChar == 112)) || ((rBBIRuleTableElement.fCharClass == 252 && this.fC.fChar == -1) || (rBBIRuleTableElement.fCharClass >= 128 && rBBIRuleTableElement.fCharClass < 240 && !this.fC.fEscaped && this.fC.fChar != -1 && this.fRuleSets[rBBIRuleTableElement.fCharClass - 128].contains(this.fC.fChar)))))) {
                    break;
                } else {
                    s++;
                }
            }
            if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("scan") >= 0) {
                System.out.println("");
            }
            if (!doParseActions(rBBIRuleTableElement.fAction)) {
                break;
            }
            if (rBBIRuleTableElement.fPushState != 0) {
                this.fStackPtr++;
                if (this.fStackPtr >= 100) {
                    System.out.println("RBBIRuleScanner.parse() - state stack overflow.");
                    error(66049);
                }
                this.fStack[this.fStackPtr] = rBBIRuleTableElement.fPushState;
            }
            if (rBBIRuleTableElement.fNextChar) {
                nextChar(this.fC);
            }
            if (rBBIRuleTableElement.fNextState != 255) {
                s = rBBIRuleTableElement.fNextState;
            } else {
                s = this.fStack[this.fStackPtr];
                this.fStackPtr--;
                if (this.fStackPtr < 0) {
                    System.out.println("RBBIRuleScanner.parse() - state stack underflow.");
                    error(66049);
                }
            }
        }
        if (this.fRB.fTreeRoots[0] == null) {
            error(66052);
        }
        if (this.fRB.fTreeRoots[1] == null) {
            this.fRB.fTreeRoots[1] = pushNewNode(10);
            RBBINode rBBINodePushNewNode = pushNewNode(0);
            findSetFor(kAny, rBBINodePushNewNode, null);
            this.fRB.fTreeRoots[1].fLeftChild = rBBINodePushNewNode;
            rBBINodePushNewNode.fParent = this.fRB.fTreeRoots[1];
            this.fNodeStackPtr -= 2;
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("symbols") >= 0) {
            this.fSymbolTable.rbbiSymtablePrint();
        }
        if (this.fRB.fDebugEnv != null && this.fRB.fDebugEnv.indexOf("ptree") >= 0) {
            System.out.println("Completed Forward Rules Parse Tree...");
            this.fRB.fTreeRoots[0].printTree(true);
            System.out.println("\nCompleted Reverse Rules Parse Tree...");
            this.fRB.fTreeRoots[1].printTree(true);
            System.out.println("\nCompleted Safe Point Forward Rules Parse Tree...");
            if (this.fRB.fTreeRoots[2] != null) {
                this.fRB.fTreeRoots[2].printTree(true);
            } else {
                System.out.println("  -- null -- ");
            }
            System.out.println("\nCompleted Safe Point Reverse Rules Parse Tree...");
            if (this.fRB.fTreeRoots[3] != null) {
                this.fRB.fTreeRoots[3].printTree(true);
            } else {
                System.out.println("  -- null -- ");
            }
        }
    }

    void printNodeStack(String str) {
        System.out.println(str + ".  Dumping node stack...\n");
        for (int i = this.fNodeStackPtr; i > 0; i--) {
            this.fNodeStack[i].printTree(true);
        }
    }

    RBBINode pushNewNode(int i) {
        this.fNodeStackPtr++;
        if (this.fNodeStackPtr >= 100) {
            System.out.println("RBBIRuleScanner.pushNewNode - stack overflow.");
            error(66049);
        }
        this.fNodeStack[this.fNodeStackPtr] = new RBBINode(i);
        return this.fNodeStack[this.fNodeStackPtr];
    }

    void scanSet() {
        UnicodeSet unicodeSet;
        ParsePosition parsePosition = new ParsePosition(this.fScanIndex);
        int i = this.fScanIndex;
        try {
            unicodeSet = new UnicodeSet(this.fRB.fRules, parsePosition, this.fSymbolTable, 1);
        } catch (Exception e) {
            error(66063);
            unicodeSet = null;
        }
        if (unicodeSet.isEmpty()) {
            error(66060);
        }
        int index = parsePosition.getIndex();
        while (this.fNextIndex < index) {
            nextCharLL();
        }
        RBBINode rBBINodePushNewNode = pushNewNode(0);
        rBBINodePushNewNode.fFirstPos = i;
        rBBINodePushNewNode.fLastPos = this.fNextIndex;
        rBBINodePushNewNode.fText = this.fRB.fRules.substring(rBBINodePushNewNode.fFirstPos, rBBINodePushNewNode.fLastPos);
        findSetFor(rBBINodePushNewNode.fText, rBBINodePushNewNode, unicodeSet);
    }
}
