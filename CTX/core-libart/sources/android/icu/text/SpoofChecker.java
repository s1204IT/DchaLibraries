package android.icu.text;

import android.icu.impl.ICUBinary;
import android.icu.impl.Utility;
import android.icu.impl.number.Padder;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.lang.UScript;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpoofChecker {
    public static final int ALL_CHECKS = -1;

    @Deprecated
    public static final int ANY_CASE = 8;
    public static final int CHAR_LIMIT = 64;
    public static final int CONFUSABLE = 7;
    public static final int INVISIBLE = 32;
    public static final int MIXED_NUMBERS = 128;
    public static final int MIXED_SCRIPT_CONFUSABLE = 2;
    public static final int RESTRICTION_LEVEL = 16;

    @Deprecated
    public static final int SINGLE_SCRIPT = 16;
    public static final int SINGLE_SCRIPT_CONFUSABLE = 1;
    public static final int WHOLE_SCRIPT_CONFUSABLE = 4;
    private UnicodeSet fAllowedCharsSet;
    private Set<ULocale> fAllowedLocales;
    private int fChecks;
    private RestrictionLevel fRestrictionLevel;
    private SpoofData fSpoofData;
    public static final UnicodeSet INCLUSION = new UnicodeSet("['\\-.\\:\\u00B7\\u0375\\u058A\\u05F3\\u05F4\\u06FD\\u06FE\\u0F0B\\u200C\\u200D\\u2010\\u2019\\u2027\\u30A0\\u30FB]").freeze();
    public static final UnicodeSet RECOMMENDED = new UnicodeSet("[0-9A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u0131\\u0134-\\u013E\\u0141-\\u0148\\u014A-\\u017E\\u018F\\u01A0\\u01A1\\u01AF\\u01B0\\u01CD-\\u01DC\\u01DE-\\u01E3\\u01E6-\\u01F0\\u01F4\\u01F5\\u01F8-\\u021B\\u021E\\u021F\\u0226-\\u0233\\u0259\\u02BB\\u02BC\\u02EC\\u0300-\\u0304\\u0306-\\u030C\\u030F-\\u0311\\u0313\\u0314\\u031B\\u0323-\\u0328\\u032D\\u032E\\u0330\\u0331\\u0335\\u0338\\u0339\\u0342\\u0345\\u037B-\\u037D\\u0386\\u0388-\\u038A\\u038C\\u038E-\\u03A1\\u03A3-\\u03CE\\u03FC-\\u045F\\u048A-\\u0529\\u052E\\u052F\\u0531-\\u0556\\u0559\\u0561-\\u0586\\u05B4\\u05D0-\\u05EA\\u05F0-\\u05F2\\u0620-\\u063F\\u0641-\\u0655\\u0660-\\u0669\\u0670-\\u0672\\u0674\\u0679-\\u068D\\u068F-\\u06D3\\u06D5\\u06E5\\u06E6\\u06EE-\\u06FC\\u06FF\\u0750-\\u07B1\\u08A0-\\u08AC\\u08B2\\u08B6-\\u08BD\\u0901-\\u094D\\u094F\\u0950\\u0956\\u0957\\u0960-\\u0963\\u0966-\\u096F\\u0971-\\u0977\\u0979-\\u097F\\u0981-\\u0983\\u0985-\\u098C\\u098F\\u0990\\u0993-\\u09A8\\u09AA-\\u09B0\\u09B2\\u09B6-\\u09B9\\u09BC-\\u09C4\\u09C7\\u09C8\\u09CB-\\u09CE\\u09D7\\u09E0-\\u09E3\\u09E6-\\u09F1\\u0A01-\\u0A03\\u0A05-\\u0A0A\\u0A0F\\u0A10\\u0A13-\\u0A28\\u0A2A-\\u0A30\\u0A32\\u0A35\\u0A38\\u0A39\\u0A3C\\u0A3E-\\u0A42\\u0A47\\u0A48\\u0A4B-\\u0A4D\\u0A5C\\u0A66-\\u0A74\\u0A81-\\u0A83\\u0A85-\\u0A8D\\u0A8F-\\u0A91\\u0A93-\\u0AA8\\u0AAA-\\u0AB0\\u0AB2\\u0AB3\\u0AB5-\\u0AB9\\u0ABC-\\u0AC5\\u0AC7-\\u0AC9\\u0ACB-\\u0ACD\\u0AD0\\u0AE0-\\u0AE3\\u0AE6-\\u0AEF\\u0B01-\\u0B03\\u0B05-\\u0B0C\\u0B0F\\u0B10\\u0B13-\\u0B28\\u0B2A-\\u0B30\\u0B32\\u0B33\\u0B35-\\u0B39\\u0B3C-\\u0B43\\u0B47\\u0B48\\u0B4B-\\u0B4D\\u0B56\\u0B57\\u0B5F-\\u0B61\\u0B66-\\u0B6F\\u0B71\\u0B82\\u0B83\\u0B85-\\u0B8A\\u0B8E-\\u0B90\\u0B92-\\u0B95\\u0B99\\u0B9A\\u0B9C\\u0B9E\\u0B9F\\u0BA3\\u0BA4\\u0BA8-\\u0BAA\\u0BAE-\\u0BB9\\u0BBE-\\u0BC2\\u0BC6-\\u0BC8\\u0BCA-\\u0BCD\\u0BD0\\u0BD7\\u0BE6-\\u0BEF\\u0C01-\\u0C03\\u0C05-\\u0C0C\\u0C0E-\\u0C10\\u0C12-\\u0C28\\u0C2A-\\u0C33\\u0C35-\\u0C39\\u0C3D-\\u0C44\\u0C46-\\u0C48\\u0C4A-\\u0C4D\\u0C55\\u0C56\\u0C60\\u0C61\\u0C66-\\u0C6F\\u0C80\\u0C82\\u0C83\\u0C85-\\u0C8C\\u0C8E-\\u0C90\\u0C92-\\u0CA8\\u0CAA-\\u0CB3\\u0CB5-\\u0CB9\\u0CBC-\\u0CC4\\u0CC6-\\u0CC8\\u0CCA-\\u0CCD\\u0CD5\\u0CD6\\u0CE0-\\u0CE3\\u0CE6-\\u0CEF\\u0CF1\\u0CF2\\u0D02\\u0D03\\u0D05-\\u0D0C\\u0D0E-\\u0D10\\u0D12-\\u0D3A\\u0D3D-\\u0D43\\u0D46-\\u0D48\\u0D4A-\\u0D4E\\u0D54-\\u0D57\\u0D60\\u0D61\\u0D66-\\u0D6F\\u0D7A-\\u0D7F\\u0D82\\u0D83\\u0D85-\\u0D8E\\u0D91-\\u0D96\\u0D9A-\\u0DA5\\u0DA7-\\u0DB1\\u0DB3-\\u0DBB\\u0DBD\\u0DC0-\\u0DC6\\u0DCA\\u0DCF-\\u0DD4\\u0DD6\\u0DD8-\\u0DDE\\u0DF2\\u0E01-\\u0E32\\u0E34-\\u0E3A\\u0E40-\\u0E4E\\u0E50-\\u0E59\\u0E81\\u0E82\\u0E84\\u0E87\\u0E88\\u0E8A\\u0E8D\\u0E94-\\u0E97\\u0E99-\\u0E9F\\u0EA1-\\u0EA3\\u0EA5\\u0EA7\\u0EAA\\u0EAB\\u0EAD-\\u0EB2\\u0EB4-\\u0EB9\\u0EBB-\\u0EBD\\u0EC0-\\u0EC4\\u0EC6\\u0EC8-\\u0ECD\\u0ED0-\\u0ED9\\u0EDE\\u0EDF\\u0F00\\u0F20-\\u0F29\\u0F35\\u0F37\\u0F3E-\\u0F42\\u0F44-\\u0F47\\u0F49-\\u0F4C\\u0F4E-\\u0F51\\u0F53-\\u0F56\\u0F58-\\u0F5B\\u0F5D-\\u0F68\\u0F6A-\\u0F6C\\u0F71\\u0F72\\u0F74\\u0F7A-\\u0F80\\u0F82-\\u0F84\\u0F86-\\u0F92\\u0F94-\\u0F97\\u0F99-\\u0F9C\\u0F9E-\\u0FA1\\u0FA3-\\u0FA6\\u0FA8-\\u0FAB\\u0FAD-\\u0FB8\\u0FBA-\\u0FBC\\u0FC6\\u1000-\\u1049\\u1050-\\u109D\\u10C7\\u10CD\\u10D0-\\u10F0\\u10F7-\\u10FA\\u10FD-\\u10FF\\u1200-\\u1248\\u124A-\\u124D\\u1250-\\u1256\\u1258\\u125A-\\u125D\\u1260-\\u1288\\u128A-\\u128D\\u1290-\\u12B0\\u12B2-\\u12B5\\u12B8-\\u12BE\\u12C0\\u12C2-\\u12C5\\u12C8-\\u12D6\\u12D8-\\u1310\\u1312-\\u1315\\u1318-\\u135A\\u135D-\\u135F\\u1380-\\u138F\\u1780-\\u17A2\\u17A5-\\u17A7\\u17A9-\\u17B3\\u17B6-\\u17CA\\u17D2\\u17D7\\u17DC\\u17E0-\\u17E9\\u1C80-\\u1C88\\u1E00-\\u1E99\\u1E9E\\u1EA0-\\u1EF9\\u1F00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F70\\u1F72\\u1F74\\u1F76\\u1F78\\u1F7A\\u1F7C\\u1F80-\\u1FB4\\u1FB6-\\u1FBA\\u1FBC\\u1FC2-\\u1FC4\\u1FC6-\\u1FC8\\u1FCA\\u1FCC\\u1FD0-\\u1FD2\\u1FD6-\\u1FDA\\u1FE0-\\u1FE2\\u1FE4-\\u1FEA\\u1FEC\\u1FF2-\\u1FF4\\u1FF6-\\u1FF8\\u1FFA\\u1FFC\\u2D27\\u2D2D\\u2D80-\\u2D96\\u2DA0-\\u2DA6\\u2DA8-\\u2DAE\\u2DB0-\\u2DB6\\u2DB8-\\u2DBE\\u2DC0-\\u2DC6\\u2DC8-\\u2DCE\\u2DD0-\\u2DD6\\u2DD8-\\u2DDE\\u3005-\\u3007\\u3041-\\u3096\\u3099\\u309A\\u309D\\u309E\\u30A1-\\u30FA\\u30FC-\\u30FE\\u3105-\\u312D\\u31A0-\\u31BA\\u3400-\\u4DB5\\u4E00-\\u9FD5\\uA660\\uA661\\uA674-\\uA67B\\uA67F\\uA69F\\uA717-\\uA71F\\uA788\\uA78D\\uA78E\\uA790-\\uA793\\uA7A0-\\uA7AA\\uA7AE\\uA7FA\\uA9E7-\\uA9FE\\uAA60-\\uAA76\\uAA7A-\\uAA7F\\uAB01-\\uAB06\\uAB09-\\uAB0E\\uAB11-\\uAB16\\uAB20-\\uAB26\\uAB28-\\uAB2E\\uAC00-\\uD7A3\\uFA0E\\uFA0F\\uFA11\\uFA13\\uFA14\\uFA1F\\uFA21\\uFA23\\uFA24\\uFA27-\\uFA29\\U00020000-\\U0002A6D6\\U0002A700-\\U0002B734\\U0002B740-\\U0002B81D\\U0002B820-\\U0002CEA1]").freeze();
    static final UnicodeSet ASCII = new UnicodeSet(0, 127).freeze();
    private static Normalizer2 nfdNormalizer = Normalizer2.getNFDInstance();

    public enum RestrictionLevel {
        ASCII,
        SINGLE_SCRIPT_RESTRICTIVE,
        HIGHLY_RESTRICTIVE,
        MODERATELY_RESTRICTIVE,
        MINIMALLY_RESTRICTIVE,
        UNRESTRICTIVE
    }

    private SpoofChecker() {
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

        public Builder(SpoofChecker spoofChecker) {
            this.fAllowedCharsSet = new UnicodeSet(0, 1114111);
            this.fAllowedLocales = new LinkedHashSet();
            this.fChecks = spoofChecker.fChecks;
            this.fSpoofData = spoofChecker.fSpoofData;
            this.fAllowedCharsSet.set(spoofChecker.fAllowedCharsSet);
            this.fAllowedLocales.addAll(spoofChecker.fAllowedLocales);
            this.fRestrictionLevel = spoofChecker.fRestrictionLevel;
        }

        public SpoofChecker build() {
            if (this.fSpoofData == null) {
                this.fSpoofData = SpoofData.getDefault();
            }
            SpoofChecker spoofChecker = new SpoofChecker();
            spoofChecker.fChecks = this.fChecks;
            spoofChecker.fSpoofData = this.fSpoofData;
            spoofChecker.fAllowedCharsSet = (UnicodeSet) this.fAllowedCharsSet.clone();
            spoofChecker.fAllowedCharsSet.freeze();
            spoofChecker.fAllowedLocales = new HashSet(this.fAllowedLocales);
            spoofChecker.fRestrictionLevel = this.fRestrictionLevel;
            return spoofChecker;
        }

        public Builder setData(Reader reader) throws IOException, ParseException {
            this.fSpoofData = new SpoofData();
            ConfusabledataBuilder.buildConfusableData(reader, this.fSpoofData);
            return this;
        }

        @Deprecated
        public Builder setData(Reader reader, Reader reader2) throws IOException, ParseException {
            setData(reader);
            return this;
        }

        public Builder setChecks(int i) {
            if ((i & 0) != 0) {
                throw new IllegalArgumentException("Bad Spoof Checks value.");
            }
            this.fChecks = i & (-1);
            return this;
        }

        public Builder setAllowedLocales(Set<ULocale> set) {
            this.fAllowedCharsSet.clear();
            Iterator<ULocale> it = set.iterator();
            while (it.hasNext()) {
                addScriptChars(it.next(), this.fAllowedCharsSet);
            }
            this.fAllowedLocales.clear();
            if (set.size() == 0) {
                this.fAllowedCharsSet.add(0, 1114111);
                this.fChecks &= -65;
                return this;
            }
            UnicodeSet unicodeSet = new UnicodeSet();
            unicodeSet.applyIntPropertyValue(UProperty.SCRIPT, 0);
            this.fAllowedCharsSet.addAll(unicodeSet);
            unicodeSet.applyIntPropertyValue(UProperty.SCRIPT, 1);
            this.fAllowedCharsSet.addAll(unicodeSet);
            this.fAllowedLocales.clear();
            this.fAllowedLocales.addAll(set);
            this.fChecks |= 64;
            return this;
        }

        public Builder setAllowedJavaLocales(Set<Locale> set) {
            HashSet hashSet = new HashSet(set.size());
            Iterator<Locale> it = set.iterator();
            while (it.hasNext()) {
                hashSet.add(ULocale.forLocale(it.next()));
            }
            return setAllowedLocales(hashSet);
        }

        private void addScriptChars(ULocale uLocale, UnicodeSet unicodeSet) {
            int[] code = UScript.getCode(uLocale);
            if (code != null) {
                UnicodeSet unicodeSet2 = new UnicodeSet();
                for (int i : code) {
                    unicodeSet2.applyIntPropertyValue(UProperty.SCRIPT, i);
                    unicodeSet.addAll(unicodeSet2);
                }
            }
        }

        public Builder setAllowedChars(UnicodeSet unicodeSet) {
            this.fAllowedCharsSet.set(unicodeSet);
            this.fAllowedLocales.clear();
            this.fChecks |= 64;
            return this;
        }

        public Builder setRestrictionLevel(RestrictionLevel restrictionLevel) {
            this.fRestrictionLevel = restrictionLevel;
            this.fChecks |= 144;
            return this;
        }

        private static class ConfusabledataBuilder {
            static final boolean $assertionsDisabled = false;
            private int fLineNum;
            private Pattern fParseHexNum;
            private Pattern fParseLine;
            private StringBuffer fStringTable;
            private Hashtable<Integer, SPUString> fTable = new Hashtable<>();
            private UnicodeSet fKeySet = new UnicodeSet();
            private ArrayList<Integer> fKeyVec = new ArrayList<>();
            private ArrayList<Integer> fValueVec = new ArrayList<>();
            private SPUStringPool stringPool = new SPUStringPool();

            ConfusabledataBuilder() {
            }

            void build(Reader reader, SpoofData spoofData) throws IOException, ParseException {
                StringBuffer stringBuffer = new StringBuffer();
                LineNumberReader lineNumberReader = new LineNumberReader(reader);
                while (true) {
                    String line = lineNumberReader.readLine();
                    if (line == null) {
                        break;
                    }
                    stringBuffer.append(line);
                    stringBuffer.append('\n');
                }
                this.fParseLine = Pattern.compile("(?m)^[ \\t]*([0-9A-Fa-f]+)[ \\t]+;[ \\t]*([0-9A-Fa-f]+(?:[ \\t]+[0-9A-Fa-f]+)*)[ \\t]*;\\s*(?:(SL)|(SA)|(ML)|(MA))[ \\t]*(?:#.*?)?$|^([ \\t]*(?:#.*?)?)$|^(.*?)$");
                this.fParseHexNum = Pattern.compile("\\s*([0-9A-F]+)");
                int i = 0;
                if (stringBuffer.charAt(0) == 65279) {
                    stringBuffer.setCharAt(0, ' ');
                }
                Matcher matcher = this.fParseLine.matcher(stringBuffer);
                while (matcher.find()) {
                    this.fLineNum++;
                    if (matcher.start(7) < 0) {
                        if (matcher.start(8) >= 0) {
                            throw new ParseException("Confusables, line " + this.fLineNum + ": Unrecognized Line: " + matcher.group(8), matcher.start(8));
                        }
                        int i2 = Integer.parseInt(matcher.group(1), 16);
                        if (i2 > 1114111) {
                            throw new ParseException("Confusables, line " + this.fLineNum + ": Bad code point: " + matcher.group(1), matcher.start(1));
                        }
                        Matcher matcher2 = this.fParseHexNum.matcher(matcher.group(2));
                        StringBuilder sb = new StringBuilder();
                        while (matcher2.find()) {
                            int i3 = Integer.parseInt(matcher2.group(1), 16);
                            if (i3 > 1114111) {
                                throw new ParseException("Confusables, line " + this.fLineNum + ": Bad code point: " + Integer.toString(i3, 16), matcher.start(2));
                            }
                            sb.appendCodePoint(i3);
                        }
                        this.fTable.put(Integer.valueOf(i2), this.stringPool.addString(sb.toString()));
                        this.fKeySet.add(i2);
                    }
                }
                this.stringPool.sort();
                this.fStringTable = new StringBuffer();
                int size = this.stringPool.size();
                for (int i4 = 0; i4 < size; i4++) {
                    SPUString byIndex = this.stringPool.getByIndex(i4);
                    int length = byIndex.fStr.length();
                    int length2 = this.fStringTable.length();
                    if (length == 1) {
                        byIndex.fCharOrStrTableIndex = byIndex.fStr.charAt(0);
                    } else {
                        byIndex.fCharOrStrTableIndex = length2;
                        this.fStringTable.append(byIndex.fStr);
                    }
                }
                Iterator<String> it = this.fKeySet.iterator();
                while (it.hasNext()) {
                    int iCodePointAt = it.next().codePointAt(0);
                    SPUString sPUString = this.fTable.get(Integer.valueOf(iCodePointAt));
                    if (sPUString.fStr.length() > 256) {
                        throw new IllegalArgumentException("Confusable prototypes cannot be longer than 256 entries.");
                    }
                    int iCodePointAndLengthToKey = ConfusableDataUtils.codePointAndLengthToKey(iCodePointAt, sPUString.fStr.length());
                    int i5 = sPUString.fCharOrStrTableIndex;
                    this.fKeyVec.add(Integer.valueOf(iCodePointAndLengthToKey));
                    this.fValueVec.add(Integer.valueOf(i5));
                }
                int size2 = this.fKeyVec.size();
                spoofData.fCFUKeys = new int[size2];
                for (int i6 = 0; i6 < size2; i6++) {
                    int iIntValue = this.fKeyVec.get(i6).intValue();
                    ConfusableDataUtils.keyToCodePoint(iIntValue);
                    spoofData.fCFUKeys[i6] = iIntValue;
                }
                spoofData.fCFUValues = new short[this.fValueVec.size()];
                Iterator<Integer> it2 = this.fValueVec.iterator();
                while (it2.hasNext()) {
                    spoofData.fCFUValues[i] = (short) it2.next().intValue();
                    i++;
                }
                spoofData.fCFUStrings = this.fStringTable.toString();
            }

            public static void buildConfusableData(Reader reader, SpoofData spoofData) throws IOException, ParseException {
                new ConfusabledataBuilder().build(reader, spoofData);
            }

            private static class SPUString {
                int fCharOrStrTableIndex = 0;
                String fStr;

                SPUString(String str) {
                    this.fStr = str;
                }
            }

            private static class SPUStringComparator implements Comparator<SPUString> {
                static final SPUStringComparator INSTANCE = new SPUStringComparator();

                private SPUStringComparator() {
                }

                @Override
                public int compare(SPUString sPUString, SPUString sPUString2) {
                    int length = sPUString.fStr.length();
                    int length2 = sPUString2.fStr.length();
                    if (length < length2) {
                        return -1;
                    }
                    if (length > length2) {
                        return 1;
                    }
                    return sPUString.fStr.compareTo(sPUString2.fStr);
                }
            }

            private static class SPUStringPool {
                private Vector<SPUString> fVec = new Vector<>();
                private Hashtable<String, SPUString> fHash = new Hashtable<>();

                public int size() {
                    return this.fVec.size();
                }

                public SPUString getByIndex(int i) {
                    return this.fVec.elementAt(i);
                }

                public SPUString addString(String str) {
                    SPUString sPUString = this.fHash.get(str);
                    if (sPUString == null) {
                        SPUString sPUString2 = new SPUString(str);
                        this.fHash.put(str, sPUString2);
                        this.fVec.addElement(sPUString2);
                        return sPUString2;
                    }
                    return sPUString;
                }

                public void sort() {
                    Collections.sort(this.fVec, SPUStringComparator.INSTANCE);
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
        HashSet hashSet = new HashSet(this.fAllowedLocales.size());
        Iterator<ULocale> it = this.fAllowedLocales.iterator();
        while (it.hasNext()) {
            hashSet.add(it.next().toLocale());
        }
        return hashSet;
    }

    public UnicodeSet getAllowedChars() {
        return this.fAllowedCharsSet;
    }

    public static class CheckResult {
        public UnicodeSet numerics;
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
            sb.append(", numerics: ");
            sb.append(this.numerics.toPattern(false));
            sb.append(", position: ");
            sb.append(this.position);
            sb.append(", restrictionLevel: ");
            sb.append(this.restrictionLevel);
            return sb.toString();
        }
    }

    public boolean failsChecks(String str, CheckResult checkResult) {
        int i;
        int length = str.length();
        if (checkResult != null) {
            checkResult.position = 0;
            checkResult.numerics = null;
            checkResult.restrictionLevel = null;
        }
        if ((this.fChecks & 16) != 0) {
            RestrictionLevel restrictionLevel = getRestrictionLevel(str);
            i = restrictionLevel.compareTo(this.fRestrictionLevel) <= 0 ? 0 : 16;
            if (checkResult != null) {
                checkResult.restrictionLevel = restrictionLevel;
            }
        } else {
            i = 0;
        }
        if ((this.fChecks & 128) != 0) {
            UnicodeSet unicodeSet = new UnicodeSet();
            getNumerics(str, unicodeSet);
            if (unicodeSet.size() > 1) {
                i |= 128;
            }
            if (checkResult != null) {
                checkResult.numerics = unicodeSet;
            }
        }
        if ((this.fChecks & 64) != 0) {
            int iOffsetByCodePoints = 0;
            while (true) {
                if (iOffsetByCodePoints >= length) {
                    break;
                }
                int iCodePointAt = Character.codePointAt(str, iOffsetByCodePoints);
                iOffsetByCodePoints = Character.offsetByCodePoints(str, iOffsetByCodePoints, 1);
                if (!this.fAllowedCharsSet.contains(iCodePointAt)) {
                    i |= 64;
                    break;
                }
            }
        }
        if ((this.fChecks & 32) != 0) {
            String strNormalize = nfdNormalizer.normalize(str);
            UnicodeSet unicodeSet2 = new UnicodeSet();
            int iOffsetByCodePoints2 = 0;
            int i2 = 0;
            loop1: while (true) {
                int i3 = i2;
                while (true) {
                    if (iOffsetByCodePoints2 >= length) {
                        break loop1;
                    }
                    int iCodePointAt2 = Character.codePointAt(strNormalize, iOffsetByCodePoints2);
                    iOffsetByCodePoints2 = Character.offsetByCodePoints(strNormalize, iOffsetByCodePoints2, 1);
                    if (Character.getType(iCodePointAt2) != 6) {
                        if (i2 != 0) {
                            break;
                        }
                        i3 = 0;
                    } else if (i3 == 0) {
                        i3 = iCodePointAt2;
                    } else {
                        if (i2 == 0) {
                            unicodeSet2.add(i3);
                            i2 = 1;
                        }
                        if (unicodeSet2.contains(iCodePointAt2)) {
                            i |= 32;
                            break loop1;
                        }
                        unicodeSet2.add(iCodePointAt2);
                    }
                }
                unicodeSet2.clear();
                i2 = 0;
            }
        }
        if (checkResult != null) {
            checkResult.checks = i;
        }
        return i != 0;
    }

    public boolean failsChecks(String str) {
        return failsChecks(str, null);
    }

    public int areConfusable(String str, String str2) {
        int i;
        if ((this.fChecks & 7) == 0) {
            throw new IllegalArgumentException("No confusable checks are enabled.");
        }
        if (!getSkeleton(str).equals(getSkeleton(str2))) {
            return 0;
        }
        ScriptSet scriptSet = new ScriptSet();
        getResolvedScriptSet(str, scriptSet);
        ScriptSet scriptSet2 = new ScriptSet();
        getResolvedScriptSet(str2, scriptSet2);
        if (scriptSet.intersects(scriptSet2)) {
            i = 1;
        } else if (!scriptSet.isEmpty() && !scriptSet2.isEmpty()) {
            i = 6;
        } else {
            i = 2;
        }
        return i & this.fChecks;
    }

    public String getSkeleton(CharSequence charSequence) {
        String strNormalize = nfdNormalizer.normalize(charSequence);
        int length = strNormalize.length();
        StringBuilder sb = new StringBuilder();
        int iCharCount = 0;
        while (iCharCount < length) {
            int iCodePointAt = Character.codePointAt(strNormalize, iCharCount);
            iCharCount += Character.charCount(iCodePointAt);
            this.fSpoofData.confusableLookup(iCodePointAt, sb);
        }
        return nfdNormalizer.normalize(sb.toString());
    }

    @Deprecated
    public String getSkeleton(int i, String str) {
        return getSkeleton(str);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SpoofChecker)) {
            return false;
        }
        SpoofChecker spoofChecker = (SpoofChecker) obj;
        if ((this.fSpoofData != spoofChecker.fSpoofData && this.fSpoofData != null && !this.fSpoofData.equals(spoofChecker.fSpoofData)) || this.fChecks != spoofChecker.fChecks) {
            return false;
        }
        if (this.fAllowedLocales == spoofChecker.fAllowedLocales || this.fAllowedLocales == null || this.fAllowedLocales.equals(spoofChecker.fAllowedLocales)) {
            return (this.fAllowedCharsSet == spoofChecker.fAllowedCharsSet || this.fAllowedCharsSet == null || this.fAllowedCharsSet.equals(spoofChecker.fAllowedCharsSet)) && this.fRestrictionLevel == spoofChecker.fRestrictionLevel;
        }
        return false;
    }

    public int hashCode() {
        return (((this.fChecks ^ this.fSpoofData.hashCode()) ^ this.fAllowedLocales.hashCode()) ^ this.fAllowedCharsSet.hashCode()) ^ this.fRestrictionLevel.ordinal();
    }

    private static void getAugmentedScriptSet(int i, ScriptSet scriptSet) {
        scriptSet.clear();
        UScript.getScriptExtensions(i, scriptSet);
        if (scriptSet.get(17)) {
            scriptSet.set(172);
            scriptSet.set(105);
            scriptSet.set(119);
        }
        if (scriptSet.get(20)) {
            scriptSet.set(105);
        }
        if (scriptSet.get(22)) {
            scriptSet.set(105);
        }
        if (scriptSet.get(18)) {
            scriptSet.set(119);
        }
        if (scriptSet.get(5)) {
            scriptSet.set(172);
        }
        if (scriptSet.get(0) || scriptSet.get(1)) {
            scriptSet.setAll();
        }
    }

    private void getResolvedScriptSet(CharSequence charSequence, ScriptSet scriptSet) {
        getResolvedScriptSetWithout(charSequence, 178, scriptSet);
    }

    private void getResolvedScriptSetWithout(CharSequence charSequence, int i, ScriptSet scriptSet) {
        scriptSet.setAll();
        ScriptSet scriptSet2 = new ScriptSet();
        int iCharCount = 0;
        while (iCharCount < charSequence.length()) {
            int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
            iCharCount += Character.charCount(iCodePointAt);
            getAugmentedScriptSet(iCodePointAt, scriptSet2);
            if (i == 178 || !scriptSet2.get(i)) {
                scriptSet.and(scriptSet2);
            }
        }
    }

    private void getNumerics(String str, UnicodeSet unicodeSet) {
        unicodeSet.clear();
        int iCharCount = 0;
        while (iCharCount < str.length()) {
            int iCodePointAt = Character.codePointAt(str, iCharCount);
            iCharCount += Character.charCount(iCodePointAt);
            if (UCharacter.getType(iCodePointAt) == 9) {
                unicodeSet.add(iCodePointAt - UCharacter.getNumericValue(iCodePointAt));
            }
        }
    }

    private RestrictionLevel getRestrictionLevel(String str) {
        if (!this.fAllowedCharsSet.containsAll(str)) {
            return RestrictionLevel.UNRESTRICTIVE;
        }
        if (ASCII.containsAll(str)) {
            return RestrictionLevel.ASCII;
        }
        ScriptSet scriptSet = new ScriptSet();
        getResolvedScriptSet(str, scriptSet);
        if (!scriptSet.isEmpty()) {
            return RestrictionLevel.SINGLE_SCRIPT_RESTRICTIVE;
        }
        ScriptSet scriptSet2 = new ScriptSet();
        getResolvedScriptSetWithout(str, 25, scriptSet2);
        if (scriptSet2.get(172) || scriptSet2.get(105) || scriptSet2.get(119)) {
            return RestrictionLevel.HIGHLY_RESTRICTIVE;
        }
        if (!scriptSet2.isEmpty() && !scriptSet2.get(8) && !scriptSet2.get(14) && !scriptSet2.get(6)) {
            return RestrictionLevel.MODERATELY_RESTRICTIVE;
        }
        return RestrictionLevel.MINIMALLY_RESTRICTIVE;
    }

    private static final class ConfusableDataUtils {
        static final boolean $assertionsDisabled = false;
        public static final int FORMAT_VERSION = 2;

        private ConfusableDataUtils() {
        }

        public static final int keyToCodePoint(int i) {
            return i & 16777215;
        }

        public static final int keyToLength(int i) {
            return ((i & (-16777216)) >> 24) + 1;
        }

        public static final int codePointAndLengthToKey(int i, int i2) {
            return i | ((i2 - 1) << 24);
        }
    }

    private static class SpoofData {
        private static final int DATA_FORMAT = 1130788128;
        private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
        int[] fCFUKeys;
        String fCFUStrings;
        short[] fCFUValues;

        private static final class IsAcceptable implements ICUBinary.Authenticate {
            private IsAcceptable() {
            }

            @Override
            public boolean isDataVersionAcceptable(byte[] bArr) {
                return (bArr[0] != 2 && bArr[1] == 0 && bArr[2] == 0 && bArr[3] == 0) ? false : true;
            }
        }

        private static final class DefaultData {
            private static IOException EXCEPTION;
            private static SpoofData INSTANCE;

            private DefaultData() {
            }

            static {
                INSTANCE = null;
                EXCEPTION = null;
                try {
                    INSTANCE = new SpoofData(ICUBinary.getRequiredData("confusables.cfu"));
                } catch (IOException e) {
                    EXCEPTION = e;
                }
            }
        }

        public static SpoofData getDefault() {
            if (DefaultData.EXCEPTION == null) {
                return DefaultData.INSTANCE;
            }
            throw new MissingResourceException("Could not load default confusables data: " + DefaultData.EXCEPTION.getMessage(), "SpoofChecker", "");
        }

        private SpoofData() {
        }

        private SpoofData(ByteBuffer byteBuffer) throws IOException {
            ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
            byteBuffer.mark();
            readData(byteBuffer);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SpoofData)) {
                return false;
            }
            SpoofData spoofData = (SpoofData) obj;
            if (Arrays.equals(this.fCFUKeys, spoofData.fCFUKeys) && Arrays.equals(this.fCFUValues, spoofData.fCFUValues)) {
                return Utility.sameObjects(this.fCFUStrings, spoofData.fCFUStrings) || this.fCFUStrings == null || this.fCFUStrings.equals(spoofData.fCFUStrings);
            }
            return false;
        }

        public int hashCode() {
            return (Arrays.hashCode(this.fCFUKeys) ^ Arrays.hashCode(this.fCFUValues)) ^ this.fCFUStrings.hashCode();
        }

        private void readData(ByteBuffer byteBuffer) throws IOException {
            if (byteBuffer.getInt() != 944111087) {
                throw new IllegalArgumentException("Bad Spoof Check Data.");
            }
            byteBuffer.getInt();
            byteBuffer.getInt();
            int i = byteBuffer.getInt();
            int i2 = byteBuffer.getInt();
            int i3 = byteBuffer.getInt();
            int i4 = byteBuffer.getInt();
            int i5 = byteBuffer.getInt();
            int i6 = byteBuffer.getInt();
            byteBuffer.reset();
            ICUBinary.skipBytes(byteBuffer, i);
            this.fCFUKeys = ICUBinary.getInts(byteBuffer, i2, 0);
            byteBuffer.reset();
            ICUBinary.skipBytes(byteBuffer, i3);
            this.fCFUValues = ICUBinary.getShorts(byteBuffer, i4, 0);
            byteBuffer.reset();
            ICUBinary.skipBytes(byteBuffer, i5);
            this.fCFUStrings = ICUBinary.getString(byteBuffer, i6, 0);
        }

        public void confusableLookup(int i, StringBuilder sb) {
            int length = length();
            int i2 = 0;
            while (true) {
                int i3 = (i2 + length) / 2;
                if (codePointAt(i3) <= i) {
                    if (codePointAt(i3) < i) {
                        i2 = i3;
                    } else {
                        i2 = i3;
                        break;
                    }
                } else {
                    length = i3;
                }
                if (length - i2 <= 1) {
                    break;
                }
            }
            if (codePointAt(i2) != i) {
                sb.appendCodePoint(i);
            } else {
                appendValueTo(i2, sb);
            }
        }

        public int length() {
            return this.fCFUKeys.length;
        }

        public int codePointAt(int i) {
            return ConfusableDataUtils.keyToCodePoint(this.fCFUKeys[i]);
        }

        public void appendValueTo(int i, StringBuilder sb) {
            int iKeyToLength = ConfusableDataUtils.keyToLength(this.fCFUKeys[i]);
            short s = this.fCFUValues[i];
            if (iKeyToLength == 1) {
                sb.append((char) s);
            } else {
                sb.append((CharSequence) this.fCFUStrings, (int) s, iKeyToLength + s);
            }
        }
    }

    static class ScriptSet extends BitSet {
        private static final long serialVersionUID = 1;

        ScriptSet() {
        }

        public void and(int i) {
            clear(0, i);
            clear(i + 1, 178);
        }

        public void setAll() {
            set(0, 178);
        }

        public boolean isFull() {
            return cardinality() == 178;
        }

        public void appendStringTo(StringBuilder sb) {
            sb.append("{ ");
            if (isEmpty()) {
                sb.append("- ");
            } else if (isFull()) {
                sb.append("* ");
            } else {
                for (int i = 0; i < 178; i++) {
                    if (get(i)) {
                        sb.append(UScript.getShortName(i));
                        sb.append(Padder.FALLBACK_PADDING_STRING);
                    }
                }
            }
            sb.append("}");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<ScriptSet ");
            appendStringTo(sb);
            sb.append(">");
            return sb.toString();
        }
    }
}
