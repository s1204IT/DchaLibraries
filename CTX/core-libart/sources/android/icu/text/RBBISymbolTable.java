package android.icu.text;

import android.icu.lang.UCharacter;
import java.text.ParsePosition;
import java.util.HashMap;

class RBBISymbolTable implements SymbolTable {
    UnicodeSet fCachedSetLookup;
    RBBIRuleScanner fRuleScanner;
    HashMap<String, RBBISymbolTableEntry> fHashTable = new HashMap<>();
    String ffffString = "\uffff";

    static class RBBISymbolTableEntry {
        String key;
        RBBINode val;

        RBBISymbolTableEntry() {
        }
    }

    RBBISymbolTable(RBBIRuleScanner rBBIRuleScanner) {
        this.fRuleScanner = rBBIRuleScanner;
    }

    @Override
    public char[] lookup(String str) {
        String str2;
        RBBISymbolTableEntry rBBISymbolTableEntry = this.fHashTable.get(str);
        if (rBBISymbolTableEntry == null) {
            return null;
        }
        RBBINode rBBINode = rBBISymbolTableEntry.val;
        while (rBBINode.fLeftChild.fType == 2) {
            rBBINode = rBBINode.fLeftChild;
        }
        RBBINode rBBINode2 = rBBINode.fLeftChild;
        if (rBBINode2.fType == 0) {
            this.fCachedSetLookup = rBBINode2.fLeftChild.fInputSet;
            str2 = this.ffffString;
        } else {
            this.fRuleScanner.error(66063);
            str2 = rBBINode2.fText;
            this.fCachedSetLookup = null;
        }
        return str2.toCharArray();
    }

    @Override
    public UnicodeMatcher lookupMatcher(int i) {
        if (i != 65535) {
            return null;
        }
        UnicodeSet unicodeSet = this.fCachedSetLookup;
        this.fCachedSetLookup = null;
        return unicodeSet;
    }

    @Override
    public String parseReference(String str, ParsePosition parsePosition, int i) {
        int index = parsePosition.getIndex();
        int charCount = index;
        while (charCount < i) {
            int iCharAt = UTF16.charAt(str, charCount);
            if ((charCount == index && !UCharacter.isUnicodeIdentifierStart(iCharAt)) || !UCharacter.isUnicodeIdentifierPart(iCharAt)) {
                break;
            }
            charCount += UTF16.getCharCount(iCharAt);
        }
        if (charCount == index) {
            return "";
        }
        parsePosition.setIndex(charCount);
        return str.substring(index, charCount);
    }

    RBBINode lookupNode(String str) {
        RBBISymbolTableEntry rBBISymbolTableEntry = this.fHashTable.get(str);
        if (rBBISymbolTableEntry != null) {
            return rBBISymbolTableEntry.val;
        }
        return null;
    }

    void addEntry(String str, RBBINode rBBINode) {
        if (this.fHashTable.get(str) != null) {
            this.fRuleScanner.error(66055);
            return;
        }
        RBBISymbolTableEntry rBBISymbolTableEntry = new RBBISymbolTableEntry();
        rBBISymbolTableEntry.key = str;
        rBBISymbolTableEntry.val = rBBINode;
        this.fHashTable.put(rBBISymbolTableEntry.key, rBBISymbolTableEntry);
    }

    void rbbiSymtablePrint() {
        System.out.print("Variable Definitions\nName               Node Val     String Val\n----------------------------------------------------------------------\n");
        RBBISymbolTableEntry[] rBBISymbolTableEntryArr = (RBBISymbolTableEntry[]) this.fHashTable.values().toArray(new RBBISymbolTableEntry[0]);
        for (RBBISymbolTableEntry rBBISymbolTableEntry : rBBISymbolTableEntryArr) {
            System.out.print("  " + rBBISymbolTableEntry.key + "  ");
            System.out.print("  " + rBBISymbolTableEntry.val + "  ");
            System.out.print(rBBISymbolTableEntry.val.fLeftChild.fText);
            System.out.print("\n");
        }
        System.out.println("\nParsed Variable Definitions\n");
        for (RBBISymbolTableEntry rBBISymbolTableEntry2 : rBBISymbolTableEntryArr) {
            System.out.print(rBBISymbolTableEntry2.key);
            rBBISymbolTableEntry2.val.fLeftChild.printTree(true);
            System.out.print("\n");
        }
    }
}
