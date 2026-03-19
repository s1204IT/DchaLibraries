package android.icu.text;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2;
import android.icu.impl.Trie2Writable;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.lang.UScript;
import android.icu.util.ULocale;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpoofChecker {

    static final boolean f91assertionsDisabled;
    public static final int ALL_CHECKS = -1;
    public static final int ANY_CASE = 8;
    public static final int CHAR_LIMIT = 64;

    @Deprecated
    public static final UnicodeSet INCLUSION;
    public static final int INVISIBLE = 32;
    static final int KEY_LENGTH_SHIFT = 29;
    static final int KEY_MULTIPLE_VALUES = 268435456;
    static final int MAGIC = 944111087;
    static final int MA_TABLE_FLAG = 134217728;

    @Deprecated
    public static final int MIXED_NUMBERS = 128;
    public static final int MIXED_SCRIPT_CONFUSABLE = 2;
    static final int ML_TABLE_FLAG = 67108864;

    @Deprecated
    public static final UnicodeSet RECOMMENDED;

    @Deprecated
    public static final int RESTRICTION_LEVEL = 16;
    static final int SA_TABLE_FLAG = 33554432;

    @Deprecated
    public static final int SINGLE_SCRIPT = 16;
    public static final int SINGLE_SCRIPT_CONFUSABLE = 1;
    static final int SL_TABLE_FLAG = 16777216;
    public static final int WHOLE_SCRIPT_CONFUSABLE = 4;
    private static Normalizer2 nfdNormalizer;
    private UnicodeSet fAllowedCharsSet;
    private Set<ULocale> fAllowedLocales;
    private IdentifierInfo fCachedIdentifierInfo;
    private int fChecks;
    private RestrictionLevel fRestrictionLevel;
    private SpoofData fSpoofData;

    SpoofChecker(SpoofChecker spoofChecker) {
        this();
    }

    public enum RestrictionLevel {
        ASCII,
        SINGLE_SCRIPT_RESTRICTIVE,
        HIGHLY_RESTRICTIVE,
        MODERATELY_RESTRICTIVE,
        MINIMALLY_RESTRICTIVE,
        UNRESTRICTIVE;

        public static RestrictionLevel[] valuesCustom() {
            return values();
        }
    }

    static {
        f91assertionsDisabled = !SpoofChecker.class.desiredAssertionStatus();
        INCLUSION = new UnicodeSet("[\\u0027\\u002D-\\u002E\\u003A\\u00B7\\u0375\\u058A\\u05F3-\\u05F4\\u06FD-\\u06FE\\u0F0B\\u200C-\\u200D\\u2010\\u2019\\u2027\\u30A0\\u30FB]").freeze();
        RECOMMENDED = new UnicodeSet("[\\u0030-\\u0039\\u0041-\\u005A\\u005F\\u0061-\\u007A\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u0131\\u0134-\\u013E\\u0141-\\u0148\\u014A-\\u017E\\u018F\\u01A0-\\u01A1\\u01AF-\\u01B0\\u01CD-\\u01DC\\u01DE-\\u01E3\\u01E6-\\u01F0\\u01F4-\\u01F5\\u01F8-\\u021B\\u021E-\\u021F\\u0226-\\u0233\\u0259\\u02BB-\\u02BC\\u02EC\\u0300-\\u0304\\u0306-\\u030C\\u030F-\\u0311\\u0313-\\u0314\\u031B\\u0323-\\u0328\\u032D-\\u032E\\u0330-\\u0331\\u0335\\u0338-\\u0339\\u0342\\u0345\\u037B-\\u037D\\u0386\\u0388-\\u038A\\u038C\\u038E-\\u03A1\\u03A3-\\u03CE\\u03FC-\\u045F\\u048A-\\u0529\\u052E-\\u052F\\u0531-\\u0556\\u0559\\u0561-\\u0586\\u05B4\\u05D0-\\u05EA\\u05F0-\\u05F2\\u0620-\\u063F\\u0641-\\u0655\\u0660-\\u0669\\u0670-\\u0672\\u0674\\u0679-\\u068D\\u068F-\\u06D3\\u06D5\\u06E5-\\u06E6\\u06EE-\\u06FC\\u06FF\\u0750-\\u07B1\\u08A0-\\u08AC\\u08B2\\u0901-\\u094D\\u094F-\\u0950\\u0956-\\u0957\\u0960-\\u0963\\u0966-\\u096F\\u0971-\\u0977\\u0979-\\u097F\\u0981-\\u0983\\u0985-\\u098C\\u098F-\\u0990\\u0993-\\u09A8\\u09AA-\\u09B0\\u09B2\\u09B6-\\u09B9\\u09BC-\\u09C4\\u09C7-\\u09C8\\u09CB-\\u09CE\\u09D7\\u09E0-\\u09E3\\u09E6-\\u09F1\\u0A01-\\u0A03\\u0A05-\\u0A0A\\u0A0F-\\u0A10\\u0A13-\\u0A28\\u0A2A-\\u0A30\\u0A32\\u0A35\\u0A38-\\u0A39\\u0A3C\\u0A3E-\\u0A42\\u0A47-\\u0A48\\u0A4B-\\u0A4D\\u0A5C\\u0A66-\\u0A74\\u0A81-\\u0A83\\u0A85-\\u0A8D\\u0A8F-\\u0A91\\u0A93-\\u0AA8\\u0AAA-\\u0AB0\\u0AB2-\\u0AB3\\u0AB5-\\u0AB9\\u0ABC-\\u0AC5\\u0AC7-\\u0AC9\\u0ACB-\\u0ACD\\u0AD0\\u0AE0-\\u0AE3\\u0AE6-\\u0AEF\\u0B01-\\u0B03\\u0B05-\\u0B0C\\u0B0F-\\u0B10\\u0B13-\\u0B28\\u0B2A-\\u0B30\\u0B32-\\u0B33\\u0B35-\\u0B39\\u0B3C-\\u0B43\\u0B47-\\u0B48\\u0B4B-\\u0B4D\\u0B56-\\u0B57\\u0B5F-\\u0B61\\u0B66-\\u0B6F\\u0B71\\u0B82-\\u0B83\\u0B85-\\u0B8A\\u0B8E-\\u0B90\\u0B92-\\u0B95\\u0B99-\\u0B9A\\u0B9C\\u0B9E-\\u0B9F\\u0BA3-\\u0BA4\\u0BA8-\\u0BAA\\u0BAE-\\u0BB9\\u0BBE-\\u0BC2\\u0BC6-\\u0BC8\\u0BCA-\\u0BCD\\u0BD0\\u0BD7\\u0BE6-\\u0BEF\\u0C01-\\u0C03\\u0C05-\\u0C0C\\u0C0E-\\u0C10\\u0C12-\\u0C28\\u0C2A-\\u0C33\\u0C35-\\u0C39\\u0C3D-\\u0C44\\u0C46-\\u0C48\\u0C4A-\\u0C4D\\u0C55-\\u0C56\\u0C60-\\u0C61\\u0C66-\\u0C6F\\u0C82-\\u0C83\\u0C85-\\u0C8C\\u0C8E-\\u0C90\\u0C92-\\u0CA8\\u0CAA-\\u0CB3\\u0CB5-\\u0CB9\\u0CBC-\\u0CC4\\u0CC6-\\u0CC8\\u0CCA-\\u0CCD\\u0CD5-\\u0CD6\\u0CE0-\\u0CE3\\u0CE6-\\u0CEF\\u0CF1-\\u0CF2\\u0D02-\\u0D03\\u0D05-\\u0D0C\\u0D0E-\\u0D10\\u0D12-\\u0D3A\\u0D3D-\\u0D43\\u0D46-\\u0D48\\u0D4A-\\u0D4E\\u0D57\\u0D60-\\u0D61\\u0D66-\\u0D6F\\u0D7A-\\u0D7F\\u0D82-\\u0D83\\u0D85-\\u0D8E\\u0D91-\\u0D96\\u0D9A-\\u0DA5\\u0DA7-\\u0DB1\\u0DB3-\\u0DBB\\u0DBD\\u0DC0-\\u0DC6\\u0DCA\\u0DCF-\\u0DD4\\u0DD6\\u0DD8-\\u0DDE\\u0DF2\\u0E01-\\u0E32\\u0E34-\\u0E3A\\u0E40-\\u0E4E\\u0E50-\\u0E59\\u0E81-\\u0E82\\u0E84\\u0E87-\\u0E88\\u0E8A\\u0E8D\\u0E94-\\u0E97\\u0E99-\\u0E9F\\u0EA1-\\u0EA3\\u0EA5\\u0EA7\\u0EAA-\\u0EAB\\u0EAD-\\u0EB2\\u0EB4-\\u0EB9\\u0EBB-\\u0EBD\\u0EC0-\\u0EC4\\u0EC6\\u0EC8-\\u0ECD\\u0ED0-\\u0ED9\\u0EDE-\\u0EDF\\u0F00\\u0F20-\\u0F29\\u0F35\\u0F37\\u0F3E-\\u0F42\\u0F44-\\u0F47\\u0F49-\\u0F4C\\u0F4E-\\u0F51\\u0F53-\\u0F56\\u0F58-\\u0F5B\\u0F5D-\\u0F68\\u0F6A-\\u0F6C\\u0F71-\\u0F72\\u0F74\\u0F7A-\\u0F80\\u0F82-\\u0F84\\u0F86-\\u0F92\\u0F94-\\u0F97\\u0F99-\\u0F9C\\u0F9E-\\u0FA1\\u0FA3-\\u0FA6\\u0FA8-\\u0FAB\\u0FAD-\\u0FB8\\u0FBA-\\u0FBC\\u0FC6\\u1000-\\u1049\\u1050-\\u109D\\u10C7\\u10CD\\u10D0-\\u10F0\\u10F7-\\u10FA\\u10FD-\\u10FF\\u1200-\\u1248\\u124A-\\u124D\\u1250-\\u1256\\u1258\\u125A-\\u125D\\u1260-\\u1288\\u128A-\\u128D\\u1290-\\u12B0\\u12B2-\\u12B5\\u12B8-\\u12BE\\u12C0\\u12C2-\\u12C5\\u12C8-\\u12D6\\u12D8-\\u1310\\u1312-\\u1315\\u1318-\\u135A\\u135D-\\u135F\\u1380-\\u138F\\u1780-\\u17A2\\u17A5-\\u17A7\\u17A9-\\u17B3\\u17B6-\\u17CA\\u17D2\\u17D7\\u17DC\\u17E0-\\u17E9\\u1E00-\\u1E99\\u1E9E\\u1EA0-\\u1EF9\\u1F00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F70\\u1F72\\u1F74\\u1F76\\u1F78\\u1F7A\\u1F7C\\u1F80-\\u1FB4\\u1FB6-\\u1FBA\\u1FBC\\u1FC2-\\u1FC4\\u1FC6-\\u1FC8\\u1FCA\\u1FCC\\u1FD0-\\u1FD2\\u1FD6-\\u1FDA\\u1FE0-\\u1FE2\\u1FE4-\\u1FEA\\u1FEC\\u1FF2-\\u1FF4\\u1FF6-\\u1FF8\\u1FFA\\u1FFC\\u2D27\\u2D2D\\u2D80-\\u2D96\\u2DA0-\\u2DA6\\u2DA8-\\u2DAE\\u2DB0-\\u2DB6\\u2DB8-\\u2DBE\\u2DC0-\\u2DC6\\u2DC8-\\u2DCE\\u2DD0-\\u2DD6\\u2DD8-\\u2DDE\\u3005-\\u3007\\u3041-\\u3096\\u3099-\\u309A\\u309D-\\u309E\\u30A1-\\u30FA\\u30FC-\\u30FE\\u3105-\\u312D\\u31A0-\\u31BA\\u3400-\\u4DB5\\u4E00-\\u9FD5\\uA660-\\uA661\\uA674-\\uA67B\\uA67F\\uA69F\\uA717-\\uA71F\\uA788\\uA78D-\\uA78E\\uA790-\\uA793\\uA7A0-\\uA7AA\\uA7FA\\uA9E7-\\uA9FE\\uAA60-\\uAA76\\uAA7A-\\uAA7F\\uAB01-\\uAB06\\uAB09-\\uAB0E\\uAB11-\\uAB16\\uAB20-\\uAB26\\uAB28-\\uAB2E\\uAC00-\\uD7A3\\uFA0E-\\uFA0F\\uFA11\\uFA13-\\uFA14\\uFA1F\\uFA21\\uFA23-\\uFA24\\uFA27-\\uFA29\\U00020000-\\U0002A6D6\\U0002A700-\\U0002B734\\U0002B740-\\U0002B81D\\U0002B820-\\U0002CEA1]").freeze();
        nfdNormalizer = Normalizer2.getNFDInstance();
    }

    private SpoofChecker() {
        this.fCachedIdentifierInfo = null;
    }

    public static class Builder {
        final UnicodeSet fAllowedCharsSet;
        final Set<ULocale> fAllowedLocales;
        int fChecks;
        private RestrictionLevel fRestrictionLevel;
        SpoofData fSpoofData;

        public Builder() {
            this.fAllowedCharsSet = new UnicodeSet(0, 1114111);
            this.fAllowedLocales = new LinkedHashSet();
            this.fChecks = -1;
            this.fSpoofData = null;
            this.fRestrictionLevel = RestrictionLevel.HIGHLY_RESTRICTIVE;
        }

        public Builder(SpoofChecker src) {
            this.fAllowedCharsSet = new UnicodeSet(0, 1114111);
            this.fAllowedLocales = new LinkedHashSet();
            this.fChecks = src.fChecks;
            this.fSpoofData = src.fSpoofData;
            this.fAllowedCharsSet.set(src.fAllowedCharsSet);
            this.fAllowedLocales.addAll(src.fAllowedLocales);
            this.fRestrictionLevel = src.fRestrictionLevel;
        }

        public SpoofChecker build() {
            SpoofChecker spoofChecker = null;
            if (this.fSpoofData == null) {
                this.fSpoofData = SpoofData.getDefault();
            }
            SpoofChecker result = new SpoofChecker(spoofChecker);
            result.fChecks = this.fChecks;
            result.fSpoofData = this.fSpoofData;
            result.fAllowedCharsSet = (UnicodeSet) this.fAllowedCharsSet.clone();
            result.fAllowedCharsSet.freeze();
            result.fAllowedLocales = new HashSet(this.fAllowedLocales);
            result.fRestrictionLevel = this.fRestrictionLevel;
            return result;
        }

        public Builder setData(Reader confusables, Reader confusablesWholeScript) throws IOException, ParseException {
            this.fSpoofData = new SpoofData();
            ConfusabledataBuilder.buildConfusableData(confusables, this.fSpoofData);
            WSConfusableDataBuilder.buildWSConfusableData(confusablesWholeScript, this.fSpoofData);
            return this;
        }

        public Builder setChecks(int checks) {
            if ((checks & 0) != 0) {
                throw new IllegalArgumentException("Bad Spoof Checks value.");
            }
            this.fChecks = checks & (-1);
            return this;
        }

        public Builder setAllowedLocales(Set<ULocale> locales) {
            this.fAllowedCharsSet.clear();
            for (ULocale locale : locales) {
                addScriptChars(locale, this.fAllowedCharsSet);
            }
            this.fAllowedLocales.clear();
            if (locales.size() == 0) {
                this.fAllowedCharsSet.add(0, 1114111);
                this.fChecks &= -65;
                return this;
            }
            UnicodeSet tempSet = new UnicodeSet();
            tempSet.applyIntPropertyValue(UProperty.SCRIPT, 0);
            this.fAllowedCharsSet.addAll(tempSet);
            tempSet.applyIntPropertyValue(UProperty.SCRIPT, 1);
            this.fAllowedCharsSet.addAll(tempSet);
            this.fAllowedLocales.clear();
            this.fAllowedLocales.addAll(locales);
            this.fChecks |= 64;
            return this;
        }

        public Builder setAllowedJavaLocales(Set<Locale> locales) {
            HashSet<ULocale> ulocales = new HashSet<>(locales.size());
            for (Locale locale : locales) {
                ulocales.add(ULocale.forLocale(locale));
            }
            return setAllowedLocales(ulocales);
        }

        private void addScriptChars(ULocale locale, UnicodeSet allowedChars) {
            int[] scripts = UScript.getCode(locale);
            UnicodeSet tmpSet = new UnicodeSet();
            for (int i : scripts) {
                tmpSet.applyIntPropertyValue(UProperty.SCRIPT, i);
                allowedChars.addAll(tmpSet);
            }
        }

        public Builder setAllowedChars(UnicodeSet chars) {
            this.fAllowedCharsSet.set(chars);
            this.fAllowedLocales.clear();
            this.fChecks |= 64;
            return this;
        }

        @Deprecated
        public Builder setRestrictionLevel(RestrictionLevel restrictionLevel) {
            this.fRestrictionLevel = restrictionLevel;
            this.fChecks |= 16;
            return this;
        }

        private static class WSConfusableDataBuilder {

            static final boolean f93assertionsDisabled;
            static String parseExp;

            private WSConfusableDataBuilder() {
            }

            static {
                f93assertionsDisabled = !WSConfusableDataBuilder.class.desiredAssertionStatus();
                parseExp = "(?m)^([ \\t]*(?:#.*?)?)$|^(?:\\s*([0-9A-F]{4,})(?:..([0-9A-F]{4,}))?\\s*;\\s*([A-Za-z]+)\\s*;\\s*([A-Za-z]+)\\s*;\\s*(?:(A)|(L))[ \\t]*(?:#.*?)?)$|^(.*?)$";
            }

            static void readWholeFileToString(Reader reader, StringBuffer buffer) throws IOException {
                LineNumberReader lnr = new LineNumberReader(reader);
                while (true) {
                    String line = lnr.readLine();
                    if (line == null) {
                        return;
                    }
                    buffer.append(line);
                    buffer.append('\n');
                }
            }

            static void buildWSConfusableData(Reader confusablesWS, SpoofData dest) throws IOException, ParseException {
                BuilderScriptSet bsset;
                StringBuffer input = new StringBuffer();
                int lineNum = 0;
                Trie2Writable anyCaseTrie = new Trie2Writable(0, 0);
                Trie2Writable lowerCaseTrie = new Trie2Writable(0, 0);
                ArrayList<BuilderScriptSet> scriptSets = new ArrayList<>();
                scriptSets.add(null);
                scriptSets.add(null);
                readWholeFileToString(confusablesWS, input);
                Pattern parseRegexp = Pattern.compile(parseExp);
                if (input.charAt(0) == 65279) {
                    input.setCharAt(0, ' ');
                }
                Matcher matcher = parseRegexp.matcher(input);
                while (matcher.find()) {
                    lineNum++;
                    if (matcher.start(1) < 0) {
                        if (matcher.start(8) >= 0) {
                            throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": Unrecognized input: " + matcher.group(), matcher.start());
                        }
                        int startCodePoint = Integer.parseInt(matcher.group(2), 16);
                        if (startCodePoint > 1114111) {
                            throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": out of range code point: " + matcher.group(2), matcher.start(2));
                        }
                        int endCodePoint = startCodePoint;
                        if (matcher.start(3) >= 0) {
                            endCodePoint = Integer.parseInt(matcher.group(3), 16);
                        }
                        if (endCodePoint > 1114111) {
                            throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": out of range code point: " + matcher.group(3), matcher.start(3));
                        }
                        String srcScriptName = matcher.group(4);
                        String targScriptName = matcher.group(5);
                        int srcScript = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, srcScriptName);
                        int targScript = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, targScriptName);
                        if (srcScript == -1) {
                            throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": Invalid script code t: " + matcher.group(4), matcher.start(4));
                        }
                        if (targScript == -1) {
                            throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": Invalid script code t: " + matcher.group(5), matcher.start(5));
                        }
                        Trie2Writable table = anyCaseTrie;
                        if (matcher.start(7) >= 0) {
                            table = lowerCaseTrie;
                        }
                        for (int cp = startCodePoint; cp <= endCodePoint; cp++) {
                            int setIndex = table.get(cp);
                            if (setIndex > 0) {
                                if (!f93assertionsDisabled) {
                                    if (!(setIndex < scriptSets.size())) {
                                        throw new AssertionError();
                                    }
                                }
                                bsset = scriptSets.get(setIndex);
                            } else {
                                bsset = new BuilderScriptSet();
                                bsset.codePoint = cp;
                                bsset.trie = table;
                                bsset.sset = new ScriptSet();
                                int setIndex2 = scriptSets.size();
                                bsset.index = setIndex2;
                                bsset.rindex = 0;
                                scriptSets.add(bsset);
                                table.set(cp, setIndex2);
                            }
                            bsset.sset.Union(targScript);
                            bsset.sset.Union(srcScript);
                            int cpScript = UScript.getScript(cp);
                            if (cpScript != srcScript) {
                                throw new ParseException("ConfusablesWholeScript, line " + lineNum + ": Mismatch between source script and code point " + Integer.toString(cp, 16), matcher.start(5));
                            }
                        }
                    }
                }
                int rtScriptSetsCount = 2;
                for (int outeri = 2; outeri < scriptSets.size(); outeri++) {
                    BuilderScriptSet outerSet = scriptSets.get(outeri);
                    if (outerSet.index == outeri) {
                        int rtScriptSetsCount2 = rtScriptSetsCount + 1;
                        outerSet.rindex = rtScriptSetsCount;
                        for (int inneri = outeri + 1; inneri < scriptSets.size(); inneri++) {
                            BuilderScriptSet innerSet = scriptSets.get(inneri);
                            if (outerSet.sset.equals(innerSet.sset) && outerSet.sset != innerSet.sset) {
                                innerSet.sset = outerSet.sset;
                                innerSet.index = outeri;
                                innerSet.rindex = outerSet.rindex;
                            }
                        }
                        rtScriptSetsCount = rtScriptSetsCount2;
                    }
                }
                for (int i = 2; i < scriptSets.size(); i++) {
                    BuilderScriptSet bSet = scriptSets.get(i);
                    if (bSet.rindex != i) {
                        bSet.trie.set(bSet.codePoint, bSet.rindex);
                    }
                }
                UnicodeSet ignoreSet = new UnicodeSet();
                ignoreSet.applyIntPropertyValue(UProperty.SCRIPT, 0);
                UnicodeSet inheritedSet = new UnicodeSet();
                inheritedSet.applyIntPropertyValue(UProperty.SCRIPT, 1);
                ignoreSet.addAll(inheritedSet);
                for (int rn = 0; rn < ignoreSet.getRangeCount(); rn++) {
                    int rangeStart = ignoreSet.getRangeStart(rn);
                    int rangeEnd = ignoreSet.getRangeEnd(rn);
                    anyCaseTrie.setRange(rangeStart, rangeEnd, 1, true);
                    lowerCaseTrie.setRange(rangeStart, rangeEnd, 1, true);
                }
                dest.fAnyCaseTrie = anyCaseTrie.toTrie2_16();
                dest.fLowerCaseTrie = lowerCaseTrie.toTrie2_16();
                dest.fScriptSets = new ScriptSet[rtScriptSetsCount];
                dest.fScriptSets[0] = new ScriptSet();
                dest.fScriptSets[1] = new ScriptSet();
                int rindex = 2;
                for (int i2 = 2; i2 < scriptSets.size(); i2++) {
                    BuilderScriptSet bSet2 = scriptSets.get(i2);
                    if (bSet2.rindex >= rindex) {
                        if (!f93assertionsDisabled) {
                            if (!(rindex == bSet2.rindex)) {
                                throw new AssertionError();
                            }
                        }
                        dest.fScriptSets[rindex] = bSet2.sset;
                        rindex++;
                    }
                }
            }

            static class BuilderScriptSet {
                int codePoint = -1;
                Trie2Writable trie = null;
                ScriptSet sset = null;
                int index = 0;
                int rindex = 0;

                BuilderScriptSet() {
                }
            }
        }

        private static class ConfusabledataBuilder {

            static final boolean f92assertionsDisabled;
            private int fLineNum;
            private Pattern fParseHexNum;
            private Pattern fParseLine;
            private ArrayList<Integer> fStringLengthsTable;
            private StringBuffer fStringTable;
            private Hashtable<Integer, SPUString> fSLTable = new Hashtable<>();
            private Hashtable<Integer, SPUString> fSATable = new Hashtable<>();
            private Hashtable<Integer, SPUString> fMLTable = new Hashtable<>();
            private Hashtable<Integer, SPUString> fMATable = new Hashtable<>();
            private UnicodeSet fKeySet = new UnicodeSet();
            private ArrayList<Integer> fKeyVec = new ArrayList<>();
            private ArrayList<Integer> fValueVec = new ArrayList<>();
            private SPUStringPool stringPool = new SPUStringPool();

            static {
                f92assertionsDisabled = !ConfusabledataBuilder.class.desiredAssertionStatus();
            }

            ConfusabledataBuilder() {
            }

            void build(Reader confusables, SpoofData dest) throws IOException, ParseException {
                Hashtable<Integer, SPUString> table;
                StringBuffer fInput = new StringBuffer();
                WSConfusableDataBuilder.readWholeFileToString(confusables, fInput);
                this.fParseLine = Pattern.compile("(?m)^[ \\t]*([0-9A-Fa-f]+)[ \\t]+;[ \\t]*([0-9A-Fa-f]+(?:[ \\t]+[0-9A-Fa-f]+)*)[ \\t]*;\\s*(?:(SL)|(SA)|(ML)|(MA))[ \\t]*(?:#.*?)?$|^([ \\t]*(?:#.*?)?)$|^(.*?)$");
                this.fParseHexNum = Pattern.compile("\\s*([0-9A-F]+)");
                if (fInput.charAt(0) == 65279) {
                    fInput.setCharAt(0, ' ');
                }
                Matcher matcher = this.fParseLine.matcher(fInput);
                while (matcher.find()) {
                    this.fLineNum++;
                    if (matcher.start(7) < 0) {
                        if (matcher.start(8) >= 0) {
                            throw new ParseException("Confusables, line " + this.fLineNum + ": Unrecognized Line: " + matcher.group(8), matcher.start(8));
                        }
                        int keyChar = Integer.parseInt(matcher.group(1), 16);
                        if (keyChar > 1114111) {
                            throw new ParseException("Confusables, line " + this.fLineNum + ": Bad code point: " + matcher.group(1), matcher.start(1));
                        }
                        Matcher m = this.fParseHexNum.matcher(matcher.group(2));
                        StringBuilder mapString = new StringBuilder();
                        while (m.find()) {
                            int c = Integer.parseInt(m.group(1), 16);
                            if (keyChar > 1114111) {
                                throw new ParseException("Confusables, line " + this.fLineNum + ": Bad code point: " + Integer.toString(c, 16), matcher.start(2));
                            }
                            mapString.appendCodePoint(c);
                        }
                        if (!f92assertionsDisabled) {
                            if (!(mapString.length() >= 1)) {
                                throw new AssertionError();
                            }
                        }
                        SPUString smapString = this.stringPool.addString(mapString.toString());
                        if (matcher.start(3) >= 0) {
                            table = this.fSLTable;
                        } else if (matcher.start(4) >= 0) {
                            table = this.fSATable;
                        } else if (matcher.start(5) >= 0) {
                            table = this.fMLTable;
                        } else {
                            table = matcher.start(6) >= 0 ? this.fMATable : null;
                        }
                        if (!f92assertionsDisabled) {
                            if (!(table != null)) {
                                throw new AssertionError();
                            }
                        }
                        if (table != this.fMATable) {
                            throw new ParseException("Confusables, line " + this.fLineNum + ": Table must be 'MA'.", 0);
                        }
                        this.fSLTable.put(Integer.valueOf(keyChar), smapString);
                        this.fSATable.put(Integer.valueOf(keyChar), smapString);
                        this.fMLTable.put(Integer.valueOf(keyChar), smapString);
                        this.fMATable.put(Integer.valueOf(keyChar), smapString);
                        this.fKeySet.add(keyChar);
                    }
                }
                this.stringPool.sort();
                this.fStringTable = new StringBuffer();
                this.fStringLengthsTable = new ArrayList<>();
                int previousStringLength = 0;
                int previousStringIndex = 0;
                int poolSize = this.stringPool.size();
                for (int i = 0; i < poolSize; i++) {
                    SPUString s = this.stringPool.getByIndex(i);
                    int strLen = s.fStr.length();
                    int strIndex = this.fStringTable.length();
                    if (!f92assertionsDisabled) {
                        if (!(strLen >= previousStringLength)) {
                            throw new AssertionError();
                        }
                    }
                    if (strLen == 1) {
                        s.fStrTableIndex = s.fStr.charAt(0);
                    } else {
                        if (strLen > previousStringLength && previousStringLength >= 4) {
                            this.fStringLengthsTable.add(Integer.valueOf(previousStringIndex));
                            this.fStringLengthsTable.add(Integer.valueOf(previousStringLength));
                        }
                        s.fStrTableIndex = strIndex;
                        this.fStringTable.append(s.fStr);
                    }
                    previousStringLength = strLen;
                    previousStringIndex = strIndex;
                }
                if (previousStringLength >= 4) {
                    this.fStringLengthsTable.add(Integer.valueOf(previousStringIndex));
                    this.fStringLengthsTable.add(Integer.valueOf(previousStringLength));
                }
                for (String keyCharStr : this.fKeySet) {
                    int keyChar2 = keyCharStr.codePointAt(0);
                    addKeyEntry(keyChar2, this.fSLTable, 16777216);
                    addKeyEntry(keyChar2, this.fSATable, SpoofChecker.SA_TABLE_FLAG);
                    addKeyEntry(keyChar2, this.fMLTable, 67108864);
                    addKeyEntry(keyChar2, this.fMATable, 134217728);
                }
                int numKeys = this.fKeyVec.size();
                dest.fCFUKeys = new int[numKeys];
                int previousKey = 0;
                for (int i2 = 0; i2 < numKeys; i2++) {
                    int key = this.fKeyVec.get(i2).intValue();
                    if (!f92assertionsDisabled) {
                        if (!((16777215 & key) >= (16777215 & previousKey))) {
                            throw new AssertionError();
                        }
                    }
                    if (!f92assertionsDisabled) {
                        if (!(((-16777216) & key) != 0)) {
                            throw new AssertionError();
                        }
                    }
                    dest.fCFUKeys[i2] = key;
                    previousKey = key;
                }
                int numValues = this.fValueVec.size();
                if (!f92assertionsDisabled) {
                    if (!(numKeys == numValues)) {
                        throw new AssertionError();
                    }
                }
                dest.fCFUValues = new short[numValues];
                int i3 = 0;
                Iterator value$iterator = this.fValueVec.iterator();
                while (value$iterator.hasNext()) {
                    int value = ((Integer) value$iterator.next()).intValue();
                    if (!f92assertionsDisabled) {
                        if (!(value < 65535)) {
                            throw new AssertionError();
                        }
                    }
                    dest.fCFUValues[i3] = (short) value;
                    i3++;
                }
                dest.fCFUStrings = this.fStringTable.toString();
                int lengthTableLength = this.fStringLengthsTable.size();
                int previousLength = 0;
                int stringLengthsSize = lengthTableLength / 2;
                dest.fCFUStringLengths = new SpoofData.SpoofStringLengthsElement[stringLengthsSize];
                for (int i4 = 0; i4 < stringLengthsSize; i4++) {
                    int offset = this.fStringLengthsTable.get(i4 * 2).intValue();
                    int length = this.fStringLengthsTable.get((i4 * 2) + 1).intValue();
                    if (!f92assertionsDisabled) {
                        if (!(offset < dest.fCFUStrings.length())) {
                            throw new AssertionError();
                        }
                    }
                    if (!f92assertionsDisabled) {
                        if (!(length < 40)) {
                            throw new AssertionError();
                        }
                    }
                    if (!f92assertionsDisabled) {
                        if (!(length > previousLength)) {
                            throw new AssertionError();
                        }
                    }
                    dest.fCFUStringLengths[i4] = new SpoofData.SpoofStringLengthsElement();
                    dest.fCFUStringLengths[i4].fLastString = offset;
                    dest.fCFUStringLengths[i4].fStrLength = length;
                    previousLength = length;
                }
            }

            void addKeyEntry(int keyChar, Hashtable<Integer, SPUString> table, int tableFlag) {
                SPUString targetMapping = table.get(Integer.valueOf(keyChar));
                if (targetMapping == null) {
                    return;
                }
                boolean keyHasMultipleValues = false;
                for (int i = this.fKeyVec.size() - 1; i >= 0; i--) {
                    int key = this.fKeyVec.get(i).intValue();
                    if ((16777215 & key) != keyChar) {
                        break;
                    }
                    String mapping = getMapping(i);
                    if (mapping.equals(targetMapping.fStr)) {
                        this.fKeyVec.set(i, Integer.valueOf(key | tableFlag));
                        return;
                    }
                    keyHasMultipleValues = true;
                }
                int newKey = keyChar | tableFlag;
                if (keyHasMultipleValues) {
                    newKey |= 268435456;
                }
                int adjustedMappingLength = targetMapping.fStr.length() - 1;
                if (adjustedMappingLength > 3) {
                    adjustedMappingLength = 3;
                }
                int newData = targetMapping.fStrTableIndex;
                this.fKeyVec.add(Integer.valueOf(newKey | (adjustedMappingLength << 29)));
                this.fValueVec.add(Integer.valueOf(newData));
                if (!keyHasMultipleValues) {
                    return;
                }
                int previousKeyIndex = this.fKeyVec.size() - 2;
                int previousKey = this.fKeyVec.get(previousKeyIndex).intValue();
                this.fKeyVec.set(previousKeyIndex, Integer.valueOf(previousKey | 268435456));
            }

            String getMapping(int index) {
                int key = this.fKeyVec.get(index).intValue();
                int value = this.fValueVec.get(index).intValue();
                int length = SpoofChecker.getKeyLength(key);
                switch (length) {
                    case 0:
                        char[] cs = {(char) value};
                        return new String(cs);
                    case 1:
                    case 2:
                        return this.fStringTable.substring(value, value + length + 1);
                    case 3:
                        int length2 = 0;
                        int i = 0;
                        while (true) {
                            if (i < this.fStringLengthsTable.size()) {
                                int lastIndexWithLen = this.fStringLengthsTable.get(i).intValue();
                                if (value > lastIndexWithLen) {
                                    i += 2;
                                } else {
                                    length2 = this.fStringLengthsTable.get(i + 1).intValue();
                                }
                            }
                        }
                        if (!f92assertionsDisabled) {
                            if (!(length2 >= 3)) {
                                throw new AssertionError();
                            }
                        }
                        return this.fStringTable.substring(value, value + length2);
                    default:
                        if (f92assertionsDisabled) {
                            return "";
                        }
                        throw new AssertionError();
                }
            }

            public static void buildConfusableData(Reader confusables, SpoofData dest) throws IOException, ParseException {
                ConfusabledataBuilder builder = new ConfusabledataBuilder();
                builder.build(confusables, dest);
            }

            private static class SPUString {
                String fStr;
                int fStrTableIndex = 0;

                SPUString(String s) {
                    this.fStr = s;
                }
            }

            private static class SPUStringComparator implements Comparator<SPUString> {
                SPUStringComparator(SPUStringComparator sPUStringComparator) {
                    this();
                }

                private SPUStringComparator() {
                }

                @Override
                public int compare(SPUString sL, SPUString sR) {
                    int lenL = sL.fStr.length();
                    int lenR = sR.fStr.length();
                    if (lenL < lenR) {
                        return -1;
                    }
                    if (lenL > lenR) {
                        return 1;
                    }
                    return sL.fStr.compareTo(sR.fStr);
                }
            }

            private static class SPUStringPool {
                private Vector<SPUString> fVec = new Vector<>();
                private Hashtable<String, SPUString> fHash = new Hashtable<>();

                public int size() {
                    return this.fVec.size();
                }

                public SPUString getByIndex(int index) {
                    SPUString retString = this.fVec.elementAt(index);
                    return retString;
                }

                public SPUString addString(String src) {
                    SPUString hashedString = this.fHash.get(src);
                    if (hashedString == null) {
                        SPUString hashedString2 = new SPUString(src);
                        this.fHash.put(src, hashedString2);
                        this.fVec.addElement(hashedString2);
                        return hashedString2;
                    }
                    return hashedString;
                }

                public void sort() {
                    Collections.sort(this.fVec, new SPUStringComparator(null));
                }
            }
        }
    }

    @Deprecated
    public RestrictionLevel getRestrictionLevel() {
        return this.fRestrictionLevel;
    }

    public int getChecks() {
        return this.fChecks;
    }

    public Set<ULocale> getAllowedLocales() {
        return Collections.unmodifiableSet(this.fAllowedLocales);
    }

    public Set<Locale> getAllowedJavaLocales() {
        HashSet<Locale> locales = new HashSet<>(this.fAllowedLocales.size());
        for (ULocale uloc : this.fAllowedLocales) {
            locales.add(uloc.toLocale());
        }
        return locales;
    }

    public UnicodeSet getAllowedChars() {
        return this.fAllowedCharsSet;
    }

    public static class CheckResult {

        @Deprecated
        public UnicodeSet numerics;

        @Deprecated
        public RestrictionLevel restrictionLevel;
        public int checks = 0;

        @Deprecated
        public int position = 0;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("checks:");
            if (this.checks == 0) {
                sb.append(" none");
            } else if (this.checks == -1) {
                sb.append(" all");
            } else {
                if ((this.checks & 1) != 0) {
                    sb.append(" SINGLE_SCRIPT_CONFUSABLE");
                }
                if ((this.checks & 2) != 0) {
                    sb.append(" MIXED_SCRIPT_CONFUSABLE");
                }
                if ((this.checks & 4) != 0) {
                    sb.append(" WHOLE_SCRIPT_CONFUSABLE");
                }
                if ((this.checks & 8) != 0) {
                    sb.append(" ANY_CASE");
                }
                if ((this.checks & 16) != 0) {
                    sb.append(" RESTRICTION_LEVEL");
                }
                if ((this.checks & 32) != 0) {
                    sb.append(" INVISIBLE");
                }
                if ((this.checks & 64) != 0) {
                    sb.append(" CHAR_LIMIT");
                }
                if ((this.checks & 128) != 0) {
                    sb.append(" MIXED_NUMBERS");
                }
            }
            sb.append(", numerics: ").append(this.numerics.toPattern(false));
            sb.append(", position: ").append(this.position);
            sb.append(", restrictionLevel: ").append(this.restrictionLevel);
            return sb.toString();
        }
    }

    public boolean failsChecks(String text, CheckResult checkResult) {
        int length = text.length();
        int result = 0;
        if (checkResult != null) {
            checkResult.position = 0;
            checkResult.numerics = null;
            checkResult.restrictionLevel = null;
        }
        IdentifierInfo identifierInfo = null;
        if ((this.fChecks & 144) != 0) {
            identifierInfo = getIdentifierInfo().setIdentifier(text).setIdentifierProfile(this.fAllowedCharsSet);
        }
        if ((this.fChecks & 16) != 0) {
            RestrictionLevel textRestrictionLevel = identifierInfo.getRestrictionLevel();
            if (textRestrictionLevel.compareTo(this.fRestrictionLevel) > 0) {
                result = 16;
            }
            if (checkResult != null) {
                checkResult.restrictionLevel = textRestrictionLevel;
            }
        }
        if ((this.fChecks & 128) != 0) {
            UnicodeSet numerics = identifierInfo.getNumerics();
            if (numerics.size() > 1) {
                result |= 128;
            }
            if (checkResult != null) {
                checkResult.numerics = numerics;
            }
        }
        if ((this.fChecks & 64) != 0) {
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                int c = Character.codePointAt(text, i);
                i = Character.offsetByCodePoints(text, i, 1);
                if (!this.fAllowedCharsSet.contains(c)) {
                    result |= 64;
                    break;
                }
            }
        }
        if ((this.fChecks & 38) != 0) {
            String nfdText = nfdNormalizer.normalize(text);
            if ((this.fChecks & 32) != 0) {
                int firstNonspacingMark = 0;
                boolean haveMultipleMarks = false;
                UnicodeSet marksSeenSoFar = new UnicodeSet();
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        break;
                    }
                    int c2 = Character.codePointAt(nfdText, i2);
                    i2 = Character.offsetByCodePoints(nfdText, i2, 1);
                    if (Character.getType(c2) != 6) {
                        firstNonspacingMark = 0;
                        if (haveMultipleMarks) {
                            marksSeenSoFar.clear();
                            haveMultipleMarks = false;
                        }
                    } else if (firstNonspacingMark == 0) {
                        firstNonspacingMark = c2;
                    } else {
                        if (!haveMultipleMarks) {
                            marksSeenSoFar.add(firstNonspacingMark);
                            haveMultipleMarks = true;
                        }
                        if (marksSeenSoFar.contains(c2)) {
                            result |= 32;
                            break;
                        }
                        marksSeenSoFar.add(c2);
                    }
                }
            }
            if ((this.fChecks & 6) != 0) {
                if (identifierInfo == null) {
                    identifierInfo = getIdentifierInfo();
                    identifierInfo.setIdentifier(text);
                }
                int scriptCount = identifierInfo.getScriptCount();
                ScriptSet scripts = new ScriptSet();
                wholeScriptCheck(nfdText, scripts);
                int confusableScriptCount = scripts.countMembers();
                if ((this.fChecks & 4) != 0 && confusableScriptCount >= 2 && scriptCount == 1) {
                    result |= 4;
                }
                if ((this.fChecks & 2) != 0 && confusableScriptCount >= 1 && scriptCount > 1) {
                    result |= 2;
                }
            }
        }
        if (checkResult != null) {
            checkResult.checks = result;
        }
        releaseIdentifierInfo(identifierInfo);
        return result != 0;
    }

    public boolean failsChecks(String text) {
        return failsChecks(text, null);
    }

    public int areConfusable(String s1, String s2) {
        if ((this.fChecks & 7) == 0) {
            throw new IllegalArgumentException("No confusable checks are enabled.");
        }
        int flagsForSkeleton = this.fChecks & 8;
        int result = 0;
        IdentifierInfo identifierInfo = getIdentifierInfo();
        identifierInfo.setIdentifier(s1);
        int s1ScriptCount = identifierInfo.getScriptCount();
        int s1FirstScript = identifierInfo.getScripts().nextSetBit(0);
        identifierInfo.setIdentifier(s2);
        int s2ScriptCount = identifierInfo.getScriptCount();
        int s2FirstScript = identifierInfo.getScripts().nextSetBit(0);
        releaseIdentifierInfo(identifierInfo);
        if ((this.fChecks & 1) != 0 && s1ScriptCount <= 1 && s2ScriptCount <= 1 && s1FirstScript == s2FirstScript) {
            flagsForSkeleton |= 1;
            String s1Skeleton = getSkeleton(flagsForSkeleton, s1);
            String s2Skeleton = getSkeleton(flagsForSkeleton, s2);
            if (s1Skeleton.equals(s2Skeleton)) {
                result = 1;
            }
        }
        if ((result & 1) != 0) {
            return result;
        }
        boolean possiblyWholeScriptConfusables = s1ScriptCount <= 1 && s2ScriptCount <= 1 && (this.fChecks & 4) != 0;
        if ((this.fChecks & 2) != 0 || possiblyWholeScriptConfusables) {
            int flagsForSkeleton2 = flagsForSkeleton & (-2);
            String s1Skeleton2 = getSkeleton(flagsForSkeleton2, s1);
            String s2Skeleton2 = getSkeleton(flagsForSkeleton2, s2);
            if (s1Skeleton2.equals(s2Skeleton2)) {
                int result2 = result | 2;
                if (possiblyWholeScriptConfusables) {
                    return result2 | 4;
                }
                return result2;
            }
            return result;
        }
        return result;
    }

    public String getSkeleton(int type, String id) {
        int tableMask;
        switch (type) {
            case 0:
                tableMask = 67108864;
                break;
            case 1:
                tableMask = 16777216;
                break;
            case 8:
                tableMask = 134217728;
                break;
            case 9:
                tableMask = SA_TABLE_FLAG;
                break;
            default:
                throw new IllegalArgumentException("SpoofChecker.getSkeleton(), bad type value.");
        }
        String nfdId = nfdNormalizer.normalize(id);
        int normalizedLen = nfdId.length();
        StringBuilder skelSB = new StringBuilder();
        int inputIndex = 0;
        while (inputIndex < normalizedLen) {
            int c = Character.codePointAt(nfdId, inputIndex);
            inputIndex += Character.charCount(c);
            confusableLookup(c, tableMask, skelSB);
        }
        String skelStr = skelSB.toString();
        return nfdNormalizer.normalize(skelStr);
    }

    @Deprecated
    public boolean equals(Object obj) {
        if (!(obj instanceof SpoofChecker)) {
            return false;
        }
        if ((this.fSpoofData != obj.fSpoofData && this.fSpoofData != null && !this.fSpoofData.equals(obj.fSpoofData)) || this.fChecks != obj.fChecks) {
            return false;
        }
        if (this.fAllowedLocales == obj.fAllowedLocales || this.fAllowedLocales == null || this.fAllowedLocales.equals(obj.fAllowedLocales)) {
            return (this.fAllowedCharsSet == obj.fAllowedCharsSet || this.fAllowedCharsSet == null || this.fAllowedCharsSet.equals(obj.fAllowedCharsSet)) && this.fRestrictionLevel == obj.fRestrictionLevel;
        }
        return false;
    }

    @Deprecated
    public int hashCode() {
        if (f91assertionsDisabled) {
            return 1234;
        }
        throw new AssertionError();
    }

    private void confusableLookup(int inChar, int tableMask, StringBuilder dest) {
        int mid;
        int low = 0;
        int limit = this.fSpoofData.fCFUKeys.length;
        boolean foundChar = false;
        while (true) {
            int delta = (limit - low) / 2;
            mid = low + delta;
            int midc = this.fSpoofData.fCFUKeys[mid] & DictionaryData.TRANSFORM_OFFSET_MASK;
            if (inChar == midc) {
                foundChar = true;
                break;
            }
            if (inChar < midc) {
                limit = mid;
            } else {
                low = mid + 1;
            }
            if (low >= limit) {
                break;
            }
        }
        if (!foundChar) {
            dest.appendCodePoint(inChar);
            return;
        }
        boolean foundKey = false;
        int keyFlags = this.fSpoofData.fCFUKeys[mid] & (-16777216);
        if ((keyFlags & tableMask) == 0) {
            if ((268435456 & keyFlags) != 0) {
                int altMid = mid - 1;
                while (true) {
                    if ((this.fSpoofData.fCFUKeys[altMid] & 16777215) != inChar) {
                        break;
                    }
                    keyFlags = this.fSpoofData.fCFUKeys[altMid] & (-16777216);
                    if ((keyFlags & tableMask) == 0) {
                        altMid--;
                    } else {
                        mid = altMid;
                        foundKey = true;
                        break;
                    }
                }
                if (!foundKey) {
                    int altMid2 = mid + 1;
                    while (true) {
                        if ((this.fSpoofData.fCFUKeys[altMid2] & 16777215) != inChar) {
                            break;
                        }
                        keyFlags = this.fSpoofData.fCFUKeys[altMid2] & (-16777216);
                        if ((keyFlags & tableMask) == 0) {
                            altMid2++;
                        } else {
                            mid = altMid2;
                            foundKey = true;
                            break;
                        }
                    }
                }
            }
            if (!foundKey) {
                dest.appendCodePoint(inChar);
                return;
            }
        }
        int stringLen = getKeyLength(keyFlags) + 1;
        int keyTableIndex = mid;
        short value = this.fSpoofData.fCFUValues[keyTableIndex];
        if (stringLen == 1) {
            dest.append((char) value);
            return;
        }
        if (stringLen == 4) {
            boolean dataOK = false;
            SpoofData.SpoofStringLengthsElement[] spoofStringLengthsElementArr = this.fSpoofData.fCFUStringLengths;
            int i = 0;
            int length = spoofStringLengthsElementArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                SpoofData.SpoofStringLengthsElement el = spoofStringLengthsElementArr[i];
                if (el.fLastString < value) {
                    i++;
                } else {
                    stringLen = el.fStrLength;
                    dataOK = true;
                    break;
                }
            }
            if (!f91assertionsDisabled && !dataOK) {
                throw new AssertionError();
            }
        }
        dest.append((CharSequence) this.fSpoofData.fCFUStrings, (int) value, value + stringLen);
    }

    private void wholeScriptCheck(CharSequence text, ScriptSet result) {
        int inputIdx = 0;
        Trie2 table = (this.fChecks & 8) != 0 ? this.fSpoofData.fAnyCaseTrie : this.fSpoofData.fLowerCaseTrie;
        result.setAll();
        while (inputIdx < text.length()) {
            int c = Character.codePointAt(text, inputIdx);
            inputIdx = Character.offsetByCodePoints(text, inputIdx, 1);
            int index = table.get(c);
            if (index == 0) {
                int cpScript = UScript.getScript(c);
                if (!f91assertionsDisabled) {
                    if (!(cpScript > 1)) {
                        throw new AssertionError();
                    }
                }
                result.intersect(cpScript);
            } else if (index != 1) {
                result.intersect(this.fSpoofData.fScriptSets[index]);
            }
        }
    }

    private IdentifierInfo getIdentifierInfo() {
        IdentifierInfo returnIdInfo;
        synchronized (this) {
            returnIdInfo = this.fCachedIdentifierInfo;
            this.fCachedIdentifierInfo = null;
        }
        if (returnIdInfo == null) {
            return new IdentifierInfo();
        }
        return returnIdInfo;
    }

    private void releaseIdentifierInfo(IdentifierInfo idInfo) {
        if (idInfo == null) {
            return;
        }
        synchronized (this) {
            if (this.fCachedIdentifierInfo == null) {
                this.fCachedIdentifierInfo = idInfo;
            }
        }
    }

    static final int getKeyLength(int x) {
        return (x >> 29) & 3;
    }

    private static class SpoofData {
        private static final int DATA_FORMAT = 1130788128;
        private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable(null);
        Trie2 fAnyCaseTrie;
        int[] fCFUKeys;
        SpoofStringLengthsElement[] fCFUStringLengths;
        String fCFUStrings;
        short[] fCFUValues;
        Trie2 fLowerCaseTrie;
        ScriptSet[] fScriptSets;

        static class SpoofStringLengthsElement {
            int fLastString;
            int fStrLength;

            SpoofStringLengthsElement() {
            }

            public boolean equals(Object obj) {
                return (obj instanceof SpoofStringLengthsElement) && this.fLastString == obj.fLastString && this.fStrLength == obj.fStrLength;
            }
        }

        private static final class IsAcceptable implements ICUBinary.Authenticate {
            IsAcceptable(IsAcceptable isAcceptable) {
                this();
            }

            private IsAcceptable() {
            }

            @Override
            public boolean isDataVersionAcceptable(byte[] version) {
                return version[0] == 1;
            }
        }

        private static final class DefaultData {
            private static SpoofData INSTANCE;

            private DefaultData() {
            }

            static {
                INSTANCE = null;
                try {
                    INSTANCE = new SpoofData(ICUBinary.getRequiredData("confusables.cfu"));
                } catch (IOException e) {
                }
            }
        }

        static SpoofData getDefault() {
            return DefaultData.INSTANCE;
        }

        SpoofData() {
        }

        SpoofData(ByteBuffer bytes) throws IOException {
            ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
            bytes.mark();
            readData(bytes);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SpoofData) || !Arrays.equals(this.fCFUKeys, obj.fCFUKeys) || !Arrays.equals(this.fCFUValues, obj.fCFUValues) || !Arrays.deepEquals(this.fCFUStringLengths, obj.fCFUStringLengths)) {
                return false;
            }
            if (this.fCFUStrings != obj.fCFUStrings && this.fCFUStrings != null && !this.fCFUStrings.equals(obj.fCFUStrings)) {
                return false;
            }
            if (this.fAnyCaseTrie == obj.fAnyCaseTrie || this.fAnyCaseTrie == null || this.fAnyCaseTrie.equals(obj.fAnyCaseTrie)) {
                return (this.fLowerCaseTrie == obj.fLowerCaseTrie || this.fLowerCaseTrie == null || this.fLowerCaseTrie.equals(obj.fLowerCaseTrie)) && Arrays.deepEquals(this.fScriptSets, obj.fScriptSets);
            }
            return false;
        }

        void readData(ByteBuffer bytes) throws IOException {
            int magic = bytes.getInt();
            if (magic != SpoofChecker.MAGIC) {
                throw new IllegalArgumentException("Bad Spoof Check Data.");
            }
            bytes.getInt();
            bytes.getInt();
            int CFUKeysOffset = bytes.getInt();
            int CFUKeysSize = bytes.getInt();
            int CFUValuesOffset = bytes.getInt();
            int CFUValuesSize = bytes.getInt();
            int CFUStringTableOffset = bytes.getInt();
            int CFUStringTableSize = bytes.getInt();
            int CFUStringLengthsOffset = bytes.getInt();
            int CFUStringLengthsSize = bytes.getInt();
            int anyCaseTrieOffset = bytes.getInt();
            bytes.getInt();
            int lowerCaseTrieOffset = bytes.getInt();
            bytes.getInt();
            int scriptSetsOffset = bytes.getInt();
            int scriptSetslength = bytes.getInt();
            this.fCFUKeys = null;
            this.fCFUValues = null;
            this.fCFUStringLengths = null;
            this.fCFUStrings = null;
            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUKeysOffset);
            this.fCFUKeys = ICUBinary.getInts(bytes, CFUKeysSize, 0);
            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUValuesOffset);
            this.fCFUValues = ICUBinary.getShorts(bytes, CFUValuesSize, 0);
            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUStringTableOffset);
            this.fCFUStrings = ICUBinary.getString(bytes, CFUStringTableSize, 0);
            bytes.reset();
            ICUBinary.skipBytes(bytes, CFUStringLengthsOffset);
            this.fCFUStringLengths = new SpoofStringLengthsElement[CFUStringLengthsSize];
            for (int i = 0; i < CFUStringLengthsSize; i++) {
                this.fCFUStringLengths[i] = new SpoofStringLengthsElement();
                this.fCFUStringLengths[i].fLastString = bytes.getShort();
                this.fCFUStringLengths[i].fStrLength = bytes.getShort();
            }
            bytes.reset();
            ICUBinary.skipBytes(bytes, anyCaseTrieOffset);
            this.fAnyCaseTrie = Trie2.createFromSerialized(bytes);
            bytes.reset();
            ICUBinary.skipBytes(bytes, lowerCaseTrieOffset);
            this.fLowerCaseTrie = Trie2.createFromSerialized(bytes);
            bytes.reset();
            ICUBinary.skipBytes(bytes, scriptSetsOffset);
            this.fScriptSets = new ScriptSet[scriptSetslength];
            for (int i2 = 0; i2 < scriptSetslength; i2++) {
                this.fScriptSets[i2] = new ScriptSet(bytes);
            }
        }
    }

    static class ScriptSet {

        static final boolean f94assertionsDisabled;
        private int[] bits;

        static {
            f94assertionsDisabled = !ScriptSet.class.desiredAssertionStatus();
        }

        public ScriptSet() {
            this.bits = new int[6];
        }

        public ScriptSet(ByteBuffer bytes) throws IOException {
            this.bits = new int[6];
            for (int j = 0; j < this.bits.length; j++) {
                this.bits[j] = bytes.getInt();
            }
        }

        public void output(DataOutputStream os) throws IOException {
            for (int i = 0; i < this.bits.length; i++) {
                os.writeInt(this.bits[i]);
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ScriptSet)) {
                return false;
            }
            return Arrays.equals(this.bits, obj.bits);
        }

        public void Union(int script) {
            int index = script / 32;
            int bit = 1 << (script & 31);
            if (!f94assertionsDisabled) {
                if (!(index < (this.bits.length * 4) * 4)) {
                    throw new AssertionError();
                }
            }
            int[] iArr = this.bits;
            iArr[index] = iArr[index] | bit;
        }

        public void Union(ScriptSet other) {
            for (int i = 0; i < this.bits.length; i++) {
                int[] iArr = this.bits;
                iArr[i] = iArr[i] | other.bits[i];
            }
        }

        public void intersect(ScriptSet other) {
            for (int i = 0; i < this.bits.length; i++) {
                int[] iArr = this.bits;
                iArr[i] = iArr[i] & other.bits[i];
            }
        }

        public void intersect(int script) {
            int index = script / 32;
            int bit = 1 << (script & 31);
            if (!f94assertionsDisabled) {
                if (!(index < (this.bits.length * 4) * 4)) {
                    throw new AssertionError();
                }
            }
            for (int i = 0; i < index; i++) {
                this.bits[i] = 0;
            }
            int[] iArr = this.bits;
            iArr[index] = iArr[index] & bit;
            for (int i2 = index + 1; i2 < this.bits.length; i2++) {
                this.bits[i2] = 0;
            }
        }

        public void setAll() {
            for (int i = 0; i < this.bits.length; i++) {
                this.bits[i] = -1;
            }
        }

        public void resetAll() {
            for (int i = 0; i < this.bits.length; i++) {
                this.bits[i] = 0;
            }
        }

        public int countMembers() {
            int count = 0;
            for (int i = 0; i < this.bits.length; i++) {
                for (int x = this.bits[i]; x != 0; x &= x - 1) {
                    count++;
                }
            }
            return count;
        }
    }
}
