package android.icu.text;

import android.icu.impl.Assert;
import android.icu.impl.ICUBinary;
import android.icu.impl.ICUDebug;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RBBIRuleBuilder {
    static final boolean $assertionsDisabled = false;
    static final int U_BRK_ASSIGN_ERROR = 66054;
    static final int U_BRK_ERROR_LIMIT = 66064;
    static final int U_BRK_ERROR_START = 66048;
    static final int U_BRK_HEX_DIGITS_EXPECTED = 66050;
    static final int U_BRK_INIT_ERROR = 66059;
    static final int U_BRK_INTERNAL_ERROR = 66049;
    static final int U_BRK_MALFORMED_RULE_TAG = 66062;
    static final int U_BRK_MALFORMED_SET = 66063;
    static final int U_BRK_MISMATCHED_PAREN = 66056;
    static final int U_BRK_NEW_LINE_IN_QUOTED_STRING = 66057;
    static final int U_BRK_RULE_EMPTY_SET = 66060;
    static final int U_BRK_RULE_SYNTAX = 66052;
    static final int U_BRK_SEMICOLON_EXPECTED = 66051;
    static final int U_BRK_UNCLOSED_SET = 66053;
    static final int U_BRK_UNDEFINED_VARIABLE = 66058;
    static final int U_BRK_UNRECOGNIZED_OPTION = 66061;
    static final int U_BRK_VARIABLE_REDFINITION = 66055;
    static final int fForwardTree = 0;
    static final int fReverseTree = 1;
    static final int fSafeFwdTree = 2;
    static final int fSafeRevTree = 3;
    boolean fChainRules;
    String fDebugEnv;
    RBBITableBuilder fForwardTables;
    boolean fLBCMNoChain;
    boolean fLookAheadHardBreak;
    RBBITableBuilder fReverseTables;
    List<Integer> fRuleStatusVals;
    String fRules;
    RBBITableBuilder fSafeFwdTables;
    RBBITableBuilder fSafeRevTables;
    RBBIRuleScanner fScanner;
    RBBISetBuilder fSetBuilder;
    List<RBBINode> fUSetNodes;
    RBBINode[] fTreeRoots = new RBBINode[4];
    int fDefaultTree = 0;
    Map<Set<Integer>, Integer> fStatusSets = new HashMap();

    RBBIRuleBuilder(String str) {
        this.fDebugEnv = ICUDebug.enabled("rbbi") ? ICUDebug.value("rbbi") : null;
        this.fRules = str;
        this.fUSetNodes = new ArrayList();
        this.fRuleStatusVals = new ArrayList();
        this.fScanner = new RBBIRuleScanner(this);
        this.fSetBuilder = new RBBISetBuilder(this);
    }

    static final int align8(int i) {
        return (i + 7) & (-8);
    }

    void flattenData(OutputStream outputStream) throws IOException {
        short[] sArrExportTable;
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        String strStripRules = RBBIRuleScanner.stripRules(this.fRules);
        int iAlign8 = align8(this.fForwardTables.getTableSize());
        int iAlign82 = align8(this.fReverseTables.getTableSize());
        int iAlign83 = align8(this.fSafeRevTables.getTableSize());
        int iAlign84 = align8(this.fSetBuilder.getTrieSize());
        int iAlign85 = align8(this.fRuleStatusVals.size() * 4);
        int iAlign86 = 96 + iAlign8 + 0 + 0 + (iAlign83 > 0 ? iAlign83 : iAlign82) + iAlign85 + iAlign84 + align8(strStripRules.length() * 2);
        ICUBinary.writeHeader(1114794784, 67108864, 0, dataOutputStream);
        int[] iArr = new int[24];
        iArr[0] = 45472;
        iArr[1] = 67108864;
        iArr[2] = iAlign86;
        iArr[3] = this.fSetBuilder.getNumCharCategories();
        iArr[4] = 96;
        iArr[5] = iAlign8;
        iArr[6] = iArr[4] + iAlign8;
        iArr[7] = 0;
        iArr[8] = iArr[6] + 0;
        iArr[9] = 0;
        iArr[10] = iArr[8] + 0;
        if (iAlign83 > 0) {
            iArr[11] = iAlign83;
        } else {
            iArr[11] = iAlign82;
        }
        iArr[12] = iArr[10] + iArr[11];
        iArr[13] = this.fSetBuilder.getTrieSize();
        iArr[16] = iArr[12] + iArr[13];
        iArr[17] = iAlign85;
        iArr[14] = iArr[16] + iAlign85;
        iArr[15] = strStripRules.length() * 2;
        int i = 0;
        for (int i2 : iArr) {
            dataOutputStream.writeInt(i2);
            i += 4;
        }
        short[] sArrExportTable2 = this.fForwardTables.exportTable();
        Assert.assrt(i == iArr[4]);
        for (short s : sArrExportTable2) {
            dataOutputStream.writeShort(s);
            i += 2;
        }
        Assert.assrt(i == iArr[10]);
        if (iAlign83 > 0) {
            sArrExportTable = this.fSafeRevTables.exportTable();
        } else {
            sArrExportTable = this.fReverseTables.exportTable();
        }
        for (short s2 : sArrExportTable) {
            dataOutputStream.writeShort(s2);
            i += 2;
        }
        Assert.assrt(i == iArr[12]);
        this.fSetBuilder.serializeTrie(outputStream);
        int i3 = i + iArr[13];
        while (i3 % 8 != 0) {
            dataOutputStream.write(0);
            i3++;
        }
        Assert.assrt(i3 == iArr[16]);
        Iterator<Integer> it = this.fRuleStatusVals.iterator();
        while (it.hasNext()) {
            dataOutputStream.writeInt(it.next().intValue());
            i3 += 4;
        }
        while (i3 % 8 != 0) {
            dataOutputStream.write(0);
            i3++;
        }
        Assert.assrt(i3 == iArr[14]);
        dataOutputStream.writeChars(strStripRules);
        for (int length = i3 + (strStripRules.length() * 2); length % 8 != 0; length++) {
            dataOutputStream.write(0);
        }
    }

    static void compileRules(String str, OutputStream outputStream) throws IOException {
        RBBIRuleBuilder rBBIRuleBuilder = new RBBIRuleBuilder(str);
        rBBIRuleBuilder.fScanner.parse();
        rBBIRuleBuilder.fSetBuilder.build();
        rBBIRuleBuilder.fForwardTables = new RBBITableBuilder(rBBIRuleBuilder, 0);
        rBBIRuleBuilder.fReverseTables = new RBBITableBuilder(rBBIRuleBuilder, 1);
        rBBIRuleBuilder.fSafeFwdTables = new RBBITableBuilder(rBBIRuleBuilder, 2);
        rBBIRuleBuilder.fSafeRevTables = new RBBITableBuilder(rBBIRuleBuilder, 3);
        rBBIRuleBuilder.fForwardTables.build();
        rBBIRuleBuilder.fReverseTables.build();
        rBBIRuleBuilder.fSafeFwdTables.build();
        rBBIRuleBuilder.fSafeRevTables.build();
        if (rBBIRuleBuilder.fDebugEnv != null && rBBIRuleBuilder.fDebugEnv.indexOf("states") >= 0) {
            rBBIRuleBuilder.fForwardTables.printRuleStatusTable();
        }
        rBBIRuleBuilder.flattenData(outputStream);
    }
}
