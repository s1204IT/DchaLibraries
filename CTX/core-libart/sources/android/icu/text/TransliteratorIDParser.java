package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.util.CaseInsensitiveString;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransliteratorIDParser {
    private static final String ANY = "Any";
    private static final char CLOSE_REV = ')';
    private static final int FORWARD = 0;
    private static final char ID_DELIM = ';';
    private static final char OPEN_REV = '(';
    private static final int REVERSE = 1;
    private static final Map<CaseInsensitiveString, String> SPECIAL_INVERSES = Collections.synchronizedMap(new HashMap());
    private static final char TARGET_SEP = '-';
    private static final char VARIANT_SEP = '/';

    TransliteratorIDParser() {
    }

    private static class Specs {
        public String filter;
        public boolean sawSource;
        public String source;
        public String target;
        public String variant;

        Specs(String str, String str2, String str3, boolean z, String str4) {
            this.source = str;
            this.target = str2;
            this.variant = str3;
            this.sawSource = z;
            this.filter = str4;
        }
    }

    static class SingleID {
        public String basicID;
        public String canonID;
        public String filter;

        SingleID(String str, String str2, String str3) {
            this.canonID = str;
            this.basicID = str2;
            this.filter = str3;
        }

        SingleID(String str, String str2) {
            this(str, str2, null);
        }

        Transliterator getInstance() {
            Transliterator basicInstance;
            if (this.basicID == null || this.basicID.length() == 0) {
                basicInstance = Transliterator.getBasicInstance("Any-Null", this.canonID);
            } else {
                basicInstance = Transliterator.getBasicInstance(this.basicID, this.canonID);
            }
            if (basicInstance != null && this.filter != null) {
                basicInstance.setFilter(new UnicodeSet(this.filter));
            }
            return basicInstance;
        }
    }

    public static SingleID parseFilterID(String str, int[] iArr) {
        int i = iArr[0];
        Specs filterID = parseFilterID(str, iArr, true);
        if (filterID == null) {
            iArr[0] = i;
            return null;
        }
        SingleID singleIDSpecsToID = specsToID(filterID, 0);
        singleIDSpecsToID.filter = filterID.filter;
        return singleIDSpecsToID;
    }

    public static SingleID parseSingleID(String str, int[] iArr, int i) {
        boolean z;
        SingleID singleIDSpecsToSpecialInverse;
        int i2 = iArr[0];
        Specs specs = null;
        Specs filterID = null;
        int i3 = 1;
        while (true) {
            if (i3 <= 2) {
                if (i3 == 2 && (filterID = parseFilterID(str, iArr, true)) == null) {
                    iArr[0] = i2;
                    return null;
                }
                if (!Utility.parseChar(str, iArr, OPEN_REV)) {
                    i3++;
                } else if (!Utility.parseChar(str, iArr, CLOSE_REV)) {
                    Specs filterID2 = parseFilterID(str, iArr, true);
                    if (filterID2 == null || !Utility.parseChar(str, iArr, CLOSE_REV)) {
                        iArr[0] = i2;
                        return null;
                    }
                    z = true;
                    specs = filterID2;
                } else {
                    z = true;
                }
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            if (i == 0) {
                singleIDSpecsToSpecialInverse = specsToID(filterID, 0);
                singleIDSpecsToSpecialInverse.canonID += OPEN_REV + specsToID(specs, 0).canonID + CLOSE_REV;
                if (filterID != null) {
                    singleIDSpecsToSpecialInverse.filter = filterID.filter;
                }
            } else {
                singleIDSpecsToSpecialInverse = specsToID(specs, 0);
                singleIDSpecsToSpecialInverse.canonID += OPEN_REV + specsToID(filterID, 0).canonID + CLOSE_REV;
                if (specs != null) {
                    singleIDSpecsToSpecialInverse.filter = specs.filter;
                }
            }
        } else {
            if (i == 0) {
                singleIDSpecsToSpecialInverse = specsToID(filterID, 0);
            } else {
                singleIDSpecsToSpecialInverse = specsToSpecialInverse(filterID);
                if (singleIDSpecsToSpecialInverse == null) {
                    singleIDSpecsToSpecialInverse = specsToID(filterID, 1);
                }
            }
            singleIDSpecsToSpecialInverse.filter = filterID.filter;
        }
        return singleIDSpecsToSpecialInverse;
    }

    public static UnicodeSet parseGlobalFilter(String str, int[] iArr, int i, int[] iArr2, StringBuffer stringBuffer) {
        int i2 = iArr[0];
        if (iArr2[0] == -1) {
            iArr2[0] = Utility.parseChar(str, iArr, OPEN_REV) ? 1 : 0;
        } else if (iArr2[0] == 1 && !Utility.parseChar(str, iArr, OPEN_REV)) {
            iArr[0] = i2;
            return null;
        }
        iArr[0] = PatternProps.skipWhiteSpace(str, iArr[0]);
        if (!UnicodeSet.resemblesPattern(str, iArr[0])) {
            return null;
        }
        ParsePosition parsePosition = new ParsePosition(iArr[0]);
        try {
            UnicodeSet unicodeSet = new UnicodeSet(str, parsePosition, null);
            String strSubstring = str.substring(iArr[0], parsePosition.getIndex());
            iArr[0] = parsePosition.getIndex();
            if (iArr2[0] == 1 && !Utility.parseChar(str, iArr, CLOSE_REV)) {
                iArr[0] = i2;
                return null;
            }
            if (stringBuffer != null) {
                if (i == 0) {
                    if (iArr2[0] == 1) {
                        strSubstring = String.valueOf(OPEN_REV) + strSubstring + CLOSE_REV;
                    }
                    stringBuffer.append(strSubstring + ID_DELIM);
                } else {
                    if (iArr2[0] == 0) {
                        strSubstring = String.valueOf(OPEN_REV) + strSubstring + CLOSE_REV;
                    }
                    stringBuffer.insert(0, strSubstring + ID_DELIM);
                }
            }
            return unicodeSet;
        } catch (IllegalArgumentException e) {
            iArr[0] = i2;
            return null;
        }
    }

    public static boolean parseCompoundID(String str, int i, StringBuffer stringBuffer, List<SingleID> list, UnicodeSet[] unicodeSetArr) {
        boolean z;
        int[] iArr = {0};
        list.clear();
        unicodeSetArr[0] = null;
        stringBuffer.setLength(0);
        int[] iArr2 = {0};
        UnicodeSet globalFilter = parseGlobalFilter(str, iArr, i, iArr2, stringBuffer);
        if (globalFilter != null) {
            if (!Utility.parseChar(str, iArr, ID_DELIM)) {
                stringBuffer.setLength(0);
                iArr[0] = 0;
            }
            if (i == 0) {
                unicodeSetArr[0] = globalFilter;
            }
        }
        while (true) {
            SingleID singleID = parseSingleID(str, iArr, i);
            if (singleID != null) {
                if (i == 0) {
                    list.add(singleID);
                } else {
                    list.add(0, singleID);
                }
                if (!Utility.parseChar(str, iArr, ID_DELIM)) {
                    z = false;
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        if (list.size() == 0) {
            return false;
        }
        for (int i2 = 0; i2 < list.size(); i2++) {
            stringBuffer.append(list.get(i2).canonID);
            if (i2 != list.size() - 1) {
                stringBuffer.append(ID_DELIM);
            }
        }
        if (z) {
            iArr2[0] = 1;
            UnicodeSet globalFilter2 = parseGlobalFilter(str, iArr, i, iArr2, stringBuffer);
            if (globalFilter2 != null) {
                Utility.parseChar(str, iArr, ID_DELIM);
                if (i == 1) {
                    unicodeSetArr[0] = globalFilter2;
                }
            }
        }
        iArr[0] = PatternProps.skipWhiteSpace(str, iArr[0]);
        return iArr[0] == str.length();
    }

    static List<Transliterator> instantiateList(List<SingleID> list) {
        ArrayList arrayList = new ArrayList();
        for (SingleID singleID : list) {
            if (singleID.basicID.length() != 0) {
                Transliterator singleID2 = singleID.getInstance();
                if (singleID2 == null) {
                    throw new IllegalArgumentException("Illegal ID " + singleID.canonID);
                }
                arrayList.add(singleID2);
            }
        }
        if (arrayList.size() == 0) {
            Transliterator basicInstance = Transliterator.getBasicInstance("Any-Null", null);
            if (basicInstance == null) {
                throw new IllegalArgumentException("Internal error; cannot instantiate Any-Null");
            }
            arrayList.add(basicInstance);
        }
        return arrayList;
    }

    public static String[] IDtoSTV(String str) {
        boolean z;
        String strSubstring;
        String strSubstring2;
        String strSubstring3 = ANY;
        int iIndexOf = str.indexOf(45);
        int iIndexOf2 = str.indexOf(47);
        if (iIndexOf2 < 0) {
            iIndexOf2 = str.length();
        }
        if (iIndexOf < 0) {
            strSubstring = str.substring(0, iIndexOf2);
            strSubstring2 = str.substring(iIndexOf2);
            z = false;
        } else if (iIndexOf < iIndexOf2) {
            if (iIndexOf <= 0) {
                z = false;
            } else {
                strSubstring3 = str.substring(0, iIndexOf);
                z = true;
            }
            strSubstring = str.substring(iIndexOf + 1, iIndexOf2);
            strSubstring2 = str.substring(iIndexOf2);
        } else {
            if (iIndexOf2 <= 0) {
                z = false;
            } else {
                strSubstring3 = str.substring(0, iIndexOf2);
                z = true;
            }
            int i = iIndexOf + 1;
            String strSubstring4 = str.substring(iIndexOf2, iIndexOf);
            strSubstring = str.substring(i);
            strSubstring2 = strSubstring4;
        }
        if (strSubstring2.length() > 0) {
            strSubstring2 = strSubstring2.substring(1);
        }
        String[] strArr = new String[4];
        strArr[0] = strSubstring3;
        strArr[1] = strSubstring;
        strArr[2] = strSubstring2;
        strArr[3] = z ? "" : null;
        return strArr;
    }

    public static String STVtoID(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder(str);
        if (sb.length() == 0) {
            sb.append(ANY);
        }
        sb.append(TARGET_SEP);
        sb.append(str2);
        if (str3 != null && str3.length() != 0) {
            sb.append(VARIANT_SEP);
            sb.append(str3);
        }
        return sb.toString();
    }

    public static void registerSpecialInverse(String str, String str2, boolean z) {
        SPECIAL_INVERSES.put(new CaseInsensitiveString(str), str2);
        if (z && !str.equalsIgnoreCase(str2)) {
            SPECIAL_INVERSES.put(new CaseInsensitiveString(str2), str);
        }
    }

    private static Specs parseFilterID(String str, int[] iArr, boolean z) {
        String str2;
        boolean z2;
        String unicodeIdentifier;
        char cCharAt;
        int i = iArr[0];
        char c = 0;
        int i2 = 0;
        String str3 = null;
        String str4 = null;
        String str5 = null;
        String str6 = null;
        while (true) {
            iArr[0] = PatternProps.skipWhiteSpace(str, iArr[0]);
            if (iArr[0] == str.length()) {
                break;
            }
            if (z && str6 == null && UnicodeSet.resemblesPattern(str, iArr[0])) {
                ParsePosition parsePosition = new ParsePosition(iArr[0]);
                new UnicodeSet(str, parsePosition, null);
                String strSubstring = str.substring(iArr[0], parsePosition.getIndex());
                iArr[0] = parsePosition.getIndex();
                str6 = strSubstring;
            } else if (c != 0 || (((cCharAt = str.charAt(iArr[0])) != '-' || str3 != null) && (cCharAt != '/' || str4 != null))) {
                if ((c == 0 && i2 > 0) || (unicodeIdentifier = Utility.parseUnicodeIdentifier(str, iArr)) == null) {
                    break;
                }
                if (c == 0) {
                    str5 = unicodeIdentifier;
                } else if (c == '-') {
                    str3 = unicodeIdentifier;
                } else if (c == '/') {
                    str4 = unicodeIdentifier;
                }
                i2++;
                c = 0;
            } else {
                iArr[0] = iArr[0] + 1;
                c = cCharAt;
            }
        }
        if (str5 == null) {
            str5 = null;
        } else if (str3 == null) {
            str3 = str5;
            str5 = null;
        }
        if (str5 == null && str3 == null) {
            iArr[0] = i;
            return null;
        }
        if (str5 == null) {
            str2 = ANY;
            z2 = false;
        } else {
            str2 = str5;
            z2 = true;
        }
        return new Specs(str2, str3 == null ? ANY : str3, str4, z2, str6);
    }

    private static SingleID specsToID(Specs specs, int i) {
        String string = "";
        String str = "";
        String str2 = "";
        if (specs != null) {
            StringBuilder sb = new StringBuilder();
            if (i == 0) {
                if (specs.sawSource) {
                    sb.append(specs.source);
                    sb.append(TARGET_SEP);
                } else {
                    str2 = specs.source + TARGET_SEP;
                }
                sb.append(specs.target);
            } else {
                sb.append(specs.target);
                sb.append(TARGET_SEP);
                sb.append(specs.source);
            }
            if (specs.variant != null) {
                sb.append(VARIANT_SEP);
                sb.append(specs.variant);
            }
            str = str2 + sb.toString();
            if (specs.filter != null) {
                sb.insert(0, specs.filter);
            }
            string = sb.toString();
        }
        return new SingleID(string, str);
    }

    private static SingleID specsToSpecialInverse(Specs specs) {
        String str;
        if (!specs.source.equalsIgnoreCase(ANY) || (str = SPECIAL_INVERSES.get(new CaseInsensitiveString(specs.target))) == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (specs.filter != null) {
            sb.append(specs.filter);
        }
        if (specs.sawSource) {
            sb.append(ANY);
            sb.append(TARGET_SEP);
        }
        sb.append(str);
        String str2 = "Any-" + str;
        if (specs.variant != null) {
            sb.append(VARIANT_SEP);
            sb.append(specs.variant);
            str2 = str2 + VARIANT_SEP + specs.variant;
        }
        return new SingleID(sb.toString(), str2);
    }
}
