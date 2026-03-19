package android.icu.text;

import android.icu.impl.UBiDiProps;

public final class ArabicShaping {
    private static final int ALEFTYPE = 32;
    private static final int DESHAPE_MODE = 1;
    public static final int DIGITS_AN2EN = 64;
    public static final int DIGITS_EN2AN = 32;
    public static final int DIGITS_EN2AN_INIT_AL = 128;
    public static final int DIGITS_EN2AN_INIT_LR = 96;
    public static final int DIGITS_MASK = 224;
    public static final int DIGITS_NOOP = 0;
    public static final int DIGIT_TYPE_AN = 0;
    public static final int DIGIT_TYPE_AN_EXTENDED = 256;
    public static final int DIGIT_TYPE_MASK = 256;
    private static final char HAMZA06_CHAR = 1569;
    private static final char HAMZAFE_CHAR = 65152;
    private static final int IRRELEVANT = 4;
    public static final int LAMALEF_AUTO = 65536;
    public static final int LAMALEF_BEGIN = 3;
    public static final int LAMALEF_END = 2;
    public static final int LAMALEF_MASK = 65539;
    public static final int LAMALEF_NEAR = 1;
    public static final int LAMALEF_RESIZE = 0;
    private static final char LAMALEF_SPACE_SUB = 65535;
    private static final int LAMTYPE = 16;
    private static final char LAM_CHAR = 1604;
    public static final int LENGTH_FIXED_SPACES_AT_BEGINNING = 3;
    public static final int LENGTH_FIXED_SPACES_AT_END = 2;
    public static final int LENGTH_FIXED_SPACES_NEAR = 1;
    public static final int LENGTH_GROW_SHRINK = 0;
    public static final int LENGTH_MASK = 65539;
    public static final int LETTERS_MASK = 24;
    public static final int LETTERS_NOOP = 0;
    public static final int LETTERS_SHAPE = 8;
    public static final int LETTERS_SHAPE_TASHKEEL_ISOLATED = 24;
    public static final int LETTERS_UNSHAPE = 16;
    private static final int LINKL = 2;
    private static final int LINKR = 1;
    private static final int LINK_MASK = 3;
    private static final char NEW_TAIL_CHAR = 65139;
    private static final char OLD_TAIL_CHAR = 8203;
    public static final int SEEN_MASK = 7340032;
    public static final int SEEN_TWOCELL_NEAR = 2097152;
    private static final char SHADDA06_CHAR = 1617;
    private static final char SHADDA_CHAR = 65148;
    private static final char SHADDA_TATWEEL_CHAR = 65149;
    private static final int SHAPE_MODE = 0;
    public static final int SHAPE_TAIL_NEW_UNICODE = 134217728;
    public static final int SHAPE_TAIL_TYPE_MASK = 134217728;
    public static final int SPACES_RELATIVE_TO_TEXT_BEGIN_END = 67108864;
    public static final int SPACES_RELATIVE_TO_TEXT_MASK = 67108864;
    private static final char SPACE_CHAR = ' ';
    public static final int TASHKEEL_BEGIN = 262144;
    public static final int TASHKEEL_END = 393216;
    public static final int TASHKEEL_MASK = 917504;
    public static final int TASHKEEL_REPLACE_BY_TATWEEL = 786432;
    public static final int TASHKEEL_RESIZE = 524288;
    private static final char TASHKEEL_SPACE_SUB = 65534;
    private static final char TATWEEL_CHAR = 1600;
    public static final int TEXT_DIRECTION_LOGICAL = 0;
    public static final int TEXT_DIRECTION_MASK = 4;
    public static final int TEXT_DIRECTION_VISUAL_LTR = 4;
    public static final int TEXT_DIRECTION_VISUAL_RTL = 0;
    public static final int YEHHAMZA_MASK = 58720256;
    public static final int YEHHAMZA_TWOCELL_NEAR = 16777216;
    private static final char YEH_HAMZAFE_CHAR = 65161;
    private static final char YEH_HAMZA_CHAR = 1574;
    private boolean isLogical;
    private final int options;
    private boolean spacesRelativeToTextBeginEnd;
    private char tailChar;
    private static final int[] irrelevantPos = {0, 2, 4, 6, 8, 10, 12, 14};
    private static final int[] tailFamilyIsolatedFinal = {1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1};
    private static final int[] tashkeelMedial = {0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1};
    private static final char[] yehHamzaToYeh = {65263, 65264};
    private static final char[] convertNormalizedLamAlef = {1570, 1571, 1573, 1575};
    private static final int[] araLink = {4385, 4897, 5377, 5921, 6403, 7457, 7939, 8961, 9475, 10499, 11523, 12547, 13571, 14593, 15105, 15617, 16129, 16643, 17667, 18691, 19715, 20739, 21763, 22787, 23811, 0, 0, 0, 0, 0, 3, 24835, 25859, 26883, 27923, 28931, 29955, 30979, 32001, 32513, 33027, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0, 0, 0, 0, 0, 34049, 34561, 35073, 35585, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 33, 33, 0, 33, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 3, 3, 3, 1, 1};
    private static final int[] presLink = {3, 3, 3, 0, 3, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 0, 32, 33, 32, 33, 0, 1, 32, 33, 0, 2, 3, 1, 32, 33, 0, 2, 3, 1, 0, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 16, 18, 19, 17, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 1, 0, 1, 0, 2, 3, 1, 0, 1, 0, 1, 0, 1, 0, 1};
    private static int[] convertFEto06 = {1611, 1611, 1612, 1612, 1613, 1613, 1614, 1614, 1615, 1615, 1616, 1616, 1617, 1617, 1618, 1618, 1569, 1570, 1570, 1571, 1571, 1572, 1572, 1573, 1573, 1574, 1574, 1574, 1574, 1575, 1575, 1576, 1576, 1576, 1576, 1577, 1577, 1578, 1578, 1578, 1578, 1579, 1579, 1579, 1579, 1580, 1580, 1580, 1580, 1581, 1581, 1581, 1581, 1582, 1582, 1582, 1582, 1583, 1583, 1584, 1584, 1585, 1585, 1586, 1586, 1587, 1587, 1587, 1587, 1588, 1588, 1588, 1588, 1589, 1589, 1589, 1589, 1590, 1590, 1590, 1590, 1591, 1591, 1591, 1591, 1592, 1592, 1592, 1592, 1593, 1593, 1593, 1593, 1594, 1594, 1594, 1594, 1601, 1601, 1601, 1601, 1602, 1602, 1602, 1602, 1603, 1603, 1603, 1603, 1604, 1604, 1604, 1604, 1605, 1605, 1605, 1605, 1606, 1606, 1606, 1606, 1607, 1607, 1607, 1607, 1608, 1608, 1609, 1609, 1610, 1610, 1610, 1610, 1628, 1628, 1629, 1629, 1630, 1630, 1631, 1631};
    private static final int[][][] shapeTable = {new int[][]{new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}, new int[]{0, 1, 0, 3}, new int[]{0, 1, 0, 1}}, new int[][]{new int[]{0, 0, 2, 2}, new int[]{0, 0, 1, 2}, new int[]{0, 1, 1, 2}, new int[]{0, 1, 1, 3}}, new int[][]{new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}, new int[]{0, 1, 0, 3}, new int[]{0, 1, 0, 3}}, new int[][]{new int[]{0, 0, 1, 2}, new int[]{0, 0, 1, 2}, new int[]{0, 1, 1, 2}, new int[]{0, 1, 1, 3}}};

    public int shape(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4) throws ArabicShapingException {
        if (cArr == null) {
            throw new IllegalArgumentException("source can not be null");
        }
        if (i < 0 || i2 < 0 || i + i2 > cArr.length) {
            throw new IllegalArgumentException("bad source start (" + i + ") or length (" + i2 + ") for buffer of length " + cArr.length);
        }
        if (cArr2 == null && i4 != 0) {
            throw new IllegalArgumentException("null dest requires destSize == 0");
        }
        if (i4 != 0 && (i3 < 0 || i4 < 0 || i3 + i4 > cArr2.length)) {
            throw new IllegalArgumentException("bad dest start (" + i3 + ") or size (" + i4 + ") for buffer of length " + cArr2.length);
        }
        if ((this.options & TASHKEEL_MASK) != 0 && (this.options & TASHKEEL_MASK) != 262144 && (this.options & TASHKEEL_MASK) != 393216 && (this.options & TASHKEEL_MASK) != 524288 && (this.options & TASHKEEL_MASK) != 786432) {
            throw new IllegalArgumentException("Wrong Tashkeel argument");
        }
        if ((this.options & 65539) != 0 && (this.options & 65539) != 3 && (this.options & 65539) != 2 && (this.options & 65539) != 0 && (this.options & 65539) != 65536 && (this.options & 65539) != 1) {
            throw new IllegalArgumentException("Wrong Lam Alef argument");
        }
        if ((this.options & TASHKEEL_MASK) != 0 && (this.options & 24) == 16) {
            throw new IllegalArgumentException("Tashkeel replacement should not be enabled in deshaping mode ");
        }
        return internalShape(cArr, i, i2, cArr2, i3, i4);
    }

    public void shape(char[] cArr, int i, int i2) throws ArabicShapingException {
        if ((this.options & 65539) == 0) {
            throw new ArabicShapingException("Cannot shape in place with length option resize.");
        }
        shape(cArr, i, i2, cArr, i, i2);
    }

    public String shape(String str) throws ArabicShapingException {
        char[] cArr;
        char[] charArray = str.toCharArray();
        if ((this.options & 65539) == 0 && (this.options & 24) == 16) {
            cArr = new char[charArray.length * 2];
        } else {
            cArr = charArray;
        }
        return new String(cArr, 0, shape(charArray, 0, charArray.length, cArr, 0, cArr.length));
    }

    public ArabicShaping(int i) {
        this.options = i;
        if ((i & 224) > 128) {
            throw new IllegalArgumentException("bad DIGITS options");
        }
        this.isLogical = (i & 4) == 0;
        this.spacesRelativeToTextBeginEnd = (i & 67108864) == 67108864;
        if ((i & 134217728) == 134217728) {
            this.tailChar = NEW_TAIL_CHAR;
        } else {
            this.tailChar = OLD_TAIL_CHAR;
        }
    }

    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == ArabicShaping.class && this.options == ((ArabicShaping) obj).options;
    }

    public int hashCode() {
        return this.options;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append('[');
        int i = this.options & 65539;
        if (i != 65536) {
            switch (i) {
                case 0:
                    sb.append("LamAlef resize");
                    break;
                case 1:
                    sb.append("LamAlef spaces at near");
                    break;
                case 2:
                    sb.append("LamAlef spaces at end");
                    break;
                case 3:
                    sb.append("LamAlef spaces at begin");
                    break;
            }
        } else {
            sb.append("lamAlef auto");
        }
        int i2 = this.options & 4;
        if (i2 == 0) {
            sb.append(", logical");
        } else if (i2 == 4) {
            sb.append(", visual");
        }
        int i3 = this.options & 24;
        if (i3 == 0) {
            sb.append(", no letter shaping");
        } else if (i3 == 8) {
            sb.append(", shape letters");
        } else if (i3 == 16) {
            sb.append(", unshape letters");
        } else if (i3 == 24) {
            sb.append(", shape letters tashkeel isolated");
        }
        if ((this.options & SEEN_MASK) == 2097152) {
            sb.append(", Seen at near");
        }
        if ((this.options & YEHHAMZA_MASK) == 16777216) {
            sb.append(", Yeh Hamza at near");
        }
        int i4 = this.options & TASHKEEL_MASK;
        if (i4 == 262144) {
            sb.append(", Tashkeel at begin");
        } else if (i4 == 393216) {
            sb.append(", Tashkeel at end");
        } else if (i4 == 524288) {
            sb.append(", Tashkeel resize");
        } else if (i4 == 786432) {
            sb.append(", Tashkeel replace with tatweel");
        }
        int i5 = this.options & 224;
        if (i5 == 0) {
            sb.append(", no digit shaping");
        } else if (i5 == 32) {
            sb.append(", shape digits to AN");
        } else if (i5 == 64) {
            sb.append(", shape digits to EN");
        } else if (i5 == 96) {
            sb.append(", shape digits to AN contextually: default EN");
        } else if (i5 == 128) {
            sb.append(", shape digits to AN contextually: default AL");
        }
        int i6 = this.options & 256;
        if (i6 == 0) {
            sb.append(", standard Arabic-Indic digits");
        } else if (i6 == 256) {
            sb.append(", extended Arabic-Indic digits");
        }
        sb.append("]");
        return sb.toString();
    }

    private void shapeToArabicDigitsWithContext(char[] cArr, int i, int i2, char c, boolean z) {
        UBiDiProps uBiDiProps = UBiDiProps.INSTANCE;
        char c2 = (char) (c - '0');
        int i3 = i2 + i;
        while (true) {
            i3--;
            if (i3 >= i) {
                char c3 = cArr[i3];
                int i4 = uBiDiProps.getClass(c3);
                if (i4 != 13) {
                    switch (i4) {
                        case 0:
                        case 1:
                            z = false;
                            break;
                        case 2:
                            if (z && c3 <= '9') {
                                cArr[i3] = (char) (c3 + c2);
                            }
                            break;
                    }
                } else {
                    z = true;
                }
            } else {
                return;
            }
        }
    }

    private static void invertBuffer(char[] cArr, int i, int i2) {
        for (int i3 = (i2 + i) - 1; i < i3; i3--) {
            char c = cArr[i];
            cArr[i] = cArr[i3];
            cArr[i3] = c;
            i++;
        }
    }

    private static char changeLamAlef(char c) {
        switch (c) {
            case 1570:
                return (char) 1628;
            case 1571:
                return (char) 1629;
            case 1572:
            case 1574:
            default:
                return (char) 0;
            case 1573:
                return (char) 1630;
            case 1575:
                return (char) 1631;
        }
    }

    private static int specialChar(char c) {
        if ((c > 1569 && c < 1574) || c == 1575) {
            return 1;
        }
        if (c > 1582 && c < 1587) {
            return 1;
        }
        if ((c > 1607 && c < 1610) || c == 1577) {
            return 1;
        }
        if (c >= 1611 && c <= 1618) {
            return 2;
        }
        if ((c >= 1619 && c <= 1621) || c == 1648) {
            return 3;
        }
        if (c >= 65136 && c <= 65151) {
            return 3;
        }
        return 0;
    }

    private static int getLink(char c) {
        if (c >= 1570 && c <= 1747) {
            return araLink[c - 1570];
        }
        if (c == 8205) {
            return 3;
        }
        if (c >= 8301 && c <= 8303) {
            return 4;
        }
        if (c >= 65136 && c <= 65276) {
            return presLink[c - 65136];
        }
        return 0;
    }

    private static int countSpacesLeft(char[] cArr, int i, int i2) {
        int i3 = i + i2;
        for (int i4 = i; i4 < i3; i4++) {
            if (cArr[i4] != ' ') {
                return i4 - i;
            }
        }
        return i2;
    }

    private static int countSpacesRight(char[] cArr, int i, int i2) {
        int i3 = i + i2;
        int i4 = i3;
        do {
            i4--;
            if (i4 < i) {
                return i2;
            }
        } while (cArr[i4] == ' ');
        return (i3 - 1) - i4;
    }

    private static boolean isTashkeelChar(char c) {
        return c >= 1611 && c <= 1618;
    }

    private static int isSeenTailFamilyChar(char c) {
        if (c >= 65201 && c < 65215) {
            return tailFamilyIsolatedFinal[c - 65201];
        }
        return 0;
    }

    private static int isSeenFamilyChar(char c) {
        if (c >= 1587 && c <= 1590) {
            return 1;
        }
        return 0;
    }

    private static boolean isTailChar(char c) {
        if (c == 8203 || c == 65139) {
            return true;
        }
        return false;
    }

    private static boolean isAlefMaksouraChar(char c) {
        return c == 65263 || c == 65264 || c == 1609;
    }

    private static boolean isYehHamzaChar(char c) {
        if (c == 65161 || c == 65162) {
            return true;
        }
        return false;
    }

    private static boolean isTashkeelCharFE(char c) {
        return c != 65141 && c >= 65136 && c <= 65151;
    }

    private static int isTashkeelOnTatweelChar(char c) {
        if (c >= 65136 && c <= 65151 && c != 65139 && c != 65141 && c != 65149) {
            return tashkeelMedial[c - 65136];
        }
        if ((c >= 64754 && c <= 64756) || c == 65149) {
            return 2;
        }
        return 0;
    }

    private static int isIsolatedTashkeelChar(char c) {
        if (c >= 65136 && c <= 65151 && c != 65139 && c != 65141) {
            return 1 - tashkeelMedial[c - 65136];
        }
        if (c >= 64606 && c <= 64611) {
            return 1;
        }
        return 0;
    }

    private static boolean isAlefChar(char c) {
        return c == 1570 || c == 1571 || c == 1573 || c == 1575;
    }

    private static boolean isLamAlefChar(char c) {
        return c >= 65269 && c <= 65276;
    }

    private static boolean isNormalizedLamAlefChar(char c) {
        return c >= 1628 && c <= 1631;
    }

    private int calculateSize(char[] cArr, int i, int i2) {
        int i3 = this.options & 24;
        if (i3 == 8) {
            if (this.isLogical) {
                int i4 = (i + i2) - 1;
                while (i < i4) {
                    if ((cArr[i] == 1604 && isAlefChar(cArr[i + 1])) || isTashkeelCharFE(cArr[i])) {
                        i2--;
                    }
                    i++;
                }
            } else {
                int i5 = i + i2;
                for (int i6 = i + 1; i6 < i5; i6++) {
                    if ((cArr[i6] == 1604 && isAlefChar(cArr[i6 - 1])) || isTashkeelCharFE(cArr[i6])) {
                        i2--;
                    }
                }
            }
        } else if (i3 == 16) {
            int i7 = i + i2;
            while (i < i7) {
                if (isLamAlefChar(cArr[i])) {
                    i2++;
                }
                i++;
            }
        } else if (i3 == 24) {
        }
        return i2;
    }

    private static int countSpaceSub(char[] cArr, int i, char c) {
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            if (cArr[i3] == c) {
                i2++;
            }
        }
        return i2;
    }

    private static void shiftArray(char[] cArr, int i, int i2, char c) {
        int i3 = i2;
        while (true) {
            i2--;
            if (i2 >= i) {
                char c2 = cArr[i2];
                if (c2 != c && i3 - 1 != i2) {
                    cArr[i3] = c2;
                }
            } else {
                return;
            }
        }
    }

    private static int flipArray(char[] cArr, int i, int i2, int i3) {
        if (i3 > i) {
            while (i3 < i2) {
                cArr[i] = cArr[i3];
                i++;
                i3++;
            }
            return i;
        }
        return i2;
    }

    private static int handleTashkeelWithTatweel(char[] cArr, int i) {
        for (int i2 = 0; i2 < i; i2++) {
            if (isTashkeelOnTatweelChar(cArr[i2]) == 1) {
                cArr[i2] = TATWEEL_CHAR;
            } else if (isTashkeelOnTatweelChar(cArr[i2]) == 2) {
                cArr[i2] = SHADDA_TATWEEL_CHAR;
            } else if (isIsolatedTashkeelChar(cArr[i2]) == 1 && cArr[i2] != 65148) {
                cArr[i2] = SPACE_CHAR;
            }
        }
        return i;
    }

    private int handleGeneratedSpaces(char[] cArr, int i, int i2) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        int i3 = i;
        int i4 = i2;
        int i5 = this.options & 65539;
        int i6 = this.options & TASHKEEL_MASK;
        if ((!this.spacesRelativeToTextBeginEnd) & (!this.isLogical)) {
            switch (i5) {
                case 2:
                    i5 = 3;
                    break;
                case 3:
                    i5 = 2;
                    break;
            }
            if (i6 == 262144) {
                i6 = 393216;
            } else if (i6 == 393216) {
                i6 = 262144;
            }
        }
        if (i5 == 1) {
            int i7 = i3 + i4;
            while (i3 < i7) {
                if (cArr[i3] == 65535) {
                    cArr[i3] = SPACE_CHAR;
                }
                i3++;
            }
        } else {
            int i8 = i3 + i4;
            int iCountSpaceSub = countSpaceSub(cArr, i4, (char) 65535);
            int iCountSpaceSub2 = countSpaceSub(cArr, i4, TASHKEEL_SPACE_SUB);
            boolean z5 = false;
            if (i5 != 2) {
                z = false;
            } else {
                z = true;
            }
            if (i6 != 393216) {
                z2 = false;
            } else {
                z2 = true;
            }
            if (z && i5 == 2) {
                shiftArray(cArr, i3, i8, (char) 65535);
                while (iCountSpaceSub > i3) {
                    iCountSpaceSub--;
                    cArr[iCountSpaceSub] = SPACE_CHAR;
                }
            }
            if (z2 && i6 == 393216) {
                shiftArray(cArr, i3, i8, TASHKEEL_SPACE_SUB);
                while (iCountSpaceSub2 > i3) {
                    iCountSpaceSub2--;
                    cArr[iCountSpaceSub2] = SPACE_CHAR;
                }
            }
            if (i5 != 0) {
                z3 = false;
            } else {
                z3 = true;
            }
            if (i6 != 524288) {
                z4 = false;
            } else {
                z4 = true;
            }
            if (z3 && i5 == 0) {
                shiftArray(cArr, i3, i8, (char) 65535);
                iCountSpaceSub = flipArray(cArr, i3, i8, iCountSpaceSub);
                i4 = iCountSpaceSub - i3;
            }
            if (z4 && i6 == 524288) {
                shiftArray(cArr, i3, i8, TASHKEEL_SPACE_SUB);
                iCountSpaceSub2 = flipArray(cArr, i3, i8, iCountSpaceSub2);
                i4 = iCountSpaceSub2 - i3;
            }
            boolean z6 = i5 == 3 || i5 == 65536;
            if (i6 == 262144) {
                z5 = true;
            }
            if (z6 && (i5 == 3 || i5 == 65536)) {
                shiftArray(cArr, i3, i8, (char) 65535);
                for (int iFlipArray = flipArray(cArr, i3, i8, iCountSpaceSub); iFlipArray < i8; iFlipArray++) {
                    cArr[iFlipArray] = SPACE_CHAR;
                }
            }
            if (z5 && i6 == 262144) {
                shiftArray(cArr, i3, i8, TASHKEEL_SPACE_SUB);
                for (int iFlipArray2 = flipArray(cArr, i3, i8, iCountSpaceSub2); iFlipArray2 < i8; iFlipArray2++) {
                    cArr[iFlipArray2] = SPACE_CHAR;
                }
            }
        }
        return i4;
    }

    private boolean expandCompositCharAtBegin(char[] cArr, int i, int i2, int i3) {
        if (i3 > countSpacesRight(cArr, i, i2)) {
            return true;
        }
        int i4 = i2 + i;
        int i5 = i4 - i3;
        while (true) {
            i5--;
            if (i5 >= i) {
                char c = cArr[i5];
                if (isNormalizedLamAlefChar(c)) {
                    int i6 = i4 - 1;
                    cArr[i6] = LAM_CHAR;
                    i4 = i6 - 1;
                    cArr[i4] = convertNormalizedLamAlef[c - 1628];
                } else {
                    i4--;
                    cArr[i4] = c;
                }
            } else {
                return false;
            }
        }
    }

    private boolean expandCompositCharAtEnd(char[] cArr, int i, int i2, int i3) {
        if (i3 > countSpacesLeft(cArr, i, i2)) {
            return true;
        }
        int i4 = i2 + i;
        for (int i5 = i3 + i; i5 < i4; i5++) {
            char c = cArr[i5];
            if (isNormalizedLamAlefChar(c)) {
                int i6 = i + 1;
                cArr[i] = convertNormalizedLamAlef[c - 1628];
                i = i6 + 1;
                cArr[i6] = LAM_CHAR;
            } else {
                cArr[i] = c;
                i++;
            }
        }
        return false;
    }

    private boolean expandCompositCharAtNear(char[] cArr, int i, int i2, int i3, int i4, int i5) {
        if (isNormalizedLamAlefChar(cArr[i])) {
            return true;
        }
        int i6 = i2 + i;
        while (true) {
            i6--;
            if (i6 >= i) {
                char c = cArr[i6];
                if (i5 == 1 && isNormalizedLamAlefChar(c)) {
                    if (i6 <= i || cArr[i6 - 1] != ' ') {
                        break;
                    }
                    cArr[i6] = LAM_CHAR;
                    i6--;
                    cArr[i6] = convertNormalizedLamAlef[c - 1628];
                } else if (i4 == 1 && isSeenTailFamilyChar(c) == 1) {
                    if (i6 <= i) {
                        break;
                    }
                    int i7 = i6 - 1;
                    if (cArr[i7] != ' ') {
                        break;
                    }
                    cArr[i7] = this.tailChar;
                } else if (i3 == 1 && isYehHamzaChar(c)) {
                    if (i6 <= i) {
                        break;
                    }
                    int i8 = i6 - 1;
                    if (cArr[i8] != ' ') {
                        break;
                    }
                    cArr[i6] = yehHamzaToYeh[c - YEH_HAMZAFE_CHAR];
                    cArr[i8] = HAMZAFE_CHAR;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private int expandCompositChar(char[] cArr, int i, int i2, int i3, int i4) throws ArabicShapingException {
        int i5 = this.options & 65539;
        int i6 = this.options & SEEN_MASK;
        int i7 = this.options & YEHHAMZA_MASK;
        if (!this.isLogical && !this.spacesRelativeToTextBeginEnd) {
            switch (i5) {
                case 2:
                    i5 = 3;
                    break;
                case 3:
                    i5 = 2;
                    break;
            }
        }
        if (i4 == 1) {
            if (i5 == 65536) {
                if (this.isLogical) {
                    boolean zExpandCompositCharAtEnd = expandCompositCharAtEnd(cArr, i, i2, i3);
                    if (zExpandCompositCharAtEnd) {
                        zExpandCompositCharAtEnd = expandCompositCharAtBegin(cArr, i, i2, i3);
                    }
                    if (zExpandCompositCharAtEnd) {
                        zExpandCompositCharAtEnd = expandCompositCharAtNear(cArr, i, i2, 0, 0, 1);
                    }
                    if (zExpandCompositCharAtEnd) {
                        throw new ArabicShapingException("No spacefor lamalef");
                    }
                    return i2;
                }
                boolean zExpandCompositCharAtBegin = expandCompositCharAtBegin(cArr, i, i2, i3);
                if (zExpandCompositCharAtBegin) {
                    zExpandCompositCharAtBegin = expandCompositCharAtEnd(cArr, i, i2, i3);
                }
                if (zExpandCompositCharAtBegin) {
                    zExpandCompositCharAtBegin = expandCompositCharAtNear(cArr, i, i2, 0, 0, 1);
                }
                if (zExpandCompositCharAtBegin) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
                return i2;
            }
            if (i5 == 2) {
                if (expandCompositCharAtEnd(cArr, i, i2, i3)) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
                return i2;
            }
            if (i5 == 3) {
                if (expandCompositCharAtBegin(cArr, i, i2, i3)) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
                return i2;
            }
            if (i5 == 1) {
                if (expandCompositCharAtNear(cArr, i, i2, 0, 0, 1)) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
                return i2;
            }
            if (i5 == 0) {
                int i8 = i + i2;
                int i9 = i8 + i3;
                while (true) {
                    i8--;
                    if (i8 >= i) {
                        char c = cArr[i8];
                        if (isNormalizedLamAlefChar(c)) {
                            int i10 = i9 - 1;
                            cArr[i10] = LAM_CHAR;
                            i9 = i10 - 1;
                            cArr[i9] = convertNormalizedLamAlef[c - 1628];
                        } else {
                            i9--;
                            cArr[i9] = c;
                        }
                    } else {
                        return i2 + i3;
                    }
                }
            } else {
                return i2;
            }
        } else {
            if (i6 == 2097152 && expandCompositCharAtNear(cArr, i, i2, 0, 1, 0)) {
                throw new ArabicShapingException("No space for Seen tail expansion");
            }
            if (i7 == 16777216 && expandCompositCharAtNear(cArr, i, i2, 1, 0, 0)) {
                throw new ArabicShapingException("No space for YehHamza expansion");
            }
            return i2;
        }
    }

    private int normalize(char[] cArr, int i, int i2) {
        int i3 = i2 + i;
        int i4 = 0;
        while (i < i3) {
            char c = cArr[i];
            if (c >= 65136 && c <= 65276) {
                if (isLamAlefChar(c)) {
                    i4++;
                }
                cArr[i] = (char) convertFEto06[c - 65136];
            }
            i++;
        }
        return i4;
    }

    private int deshapeNormalize(char[] cArr, int i, int i2) {
        int i3 = 0;
        boolean z = (this.options & YEHHAMZA_MASK) == 16777216;
        boolean z2 = (this.options & SEEN_MASK) == 2097152;
        int i4 = i + i2;
        while (i < i4) {
            char c = cArr[i];
            if (z && ((c == 1569 || c == 65152) && i < i2 - 1)) {
                int i5 = i + 1;
                if (isAlefMaksouraChar(cArr[i5])) {
                    cArr[i] = SPACE_CHAR;
                    cArr[i5] = YEH_HAMZA_CHAR;
                }
            } else if (z2 && isTailChar(c) && i < i2 - 1 && isSeenTailFamilyChar(cArr[i + 1]) == 1) {
                cArr[i] = SPACE_CHAR;
            } else if (c >= 65136 && c <= 65276) {
                if (isLamAlefChar(c)) {
                    i3++;
                }
                cArr[i] = (char) convertFEto06[c - 65136];
            }
            i++;
        }
        return i3;
    }

    private int shapeUnicode(char[] cArr, int i, int i2, int i3, int i4) throws ArabicShapingException {
        int iHandleGeneratedSpaces;
        int iSpecialChar;
        int i5;
        int iNormalize = normalize(cArr, i, i2);
        int i6 = (i + i2) - 1;
        int link = getLink(cArr[i6]);
        int i7 = i6;
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        int i8 = 0;
        int i9 = 0;
        int link2 = 0;
        loop0: while (true) {
            int i10 = -2;
            while (i6 >= 0) {
                int i11 = -1;
                if ((link & 65280) != 0 || isTashkeelChar(cArr[i6])) {
                    int i12 = i6 - 1;
                    int i13 = -2;
                    while (i13 < 0) {
                        if (i12 == i11) {
                            i13 = Integer.MAX_VALUE;
                            link2 = 0;
                        } else {
                            link2 = getLink(cArr[i12]);
                            if ((link2 & 4) == 0) {
                                i13 = i12;
                            } else {
                                i12--;
                            }
                            i11 = -1;
                        }
                    }
                    if ((link & 32) > 0 && (i8 & 16) > 0) {
                        char cChangeLamAlef = changeLamAlef(cArr[i6]);
                        if (cChangeLamAlef != 0) {
                            cArr[i6] = 65535;
                            cArr[i7] = cChangeLamAlef;
                            i6 = i7;
                        }
                        link = getLink(cChangeLamAlef);
                        i8 = i9;
                        z = true;
                    }
                    if (i6 > 0 && cArr[i6 - 1] == ' ') {
                        if (isSeenFamilyChar(cArr[i6]) != 1) {
                            if (cArr[i6] == 1574) {
                            }
                        }
                        iSpecialChar = specialChar(cArr[i6]);
                        int i14 = shapeTable[link2 & 3][i8 & 3][link & 3];
                        int i15 = i13;
                        if (iSpecialChar != 1) {
                        }
                        if (iSpecialChar != 2) {
                        }
                        i5 = i15;
                    } else {
                        if (i6 == 0) {
                            if (isSeenFamilyChar(cArr[i6]) != 1) {
                                if (cArr[i6] == 1574) {
                                    z4 = true;
                                }
                            } else {
                                z3 = true;
                            }
                        }
                        iSpecialChar = specialChar(cArr[i6]);
                        int i142 = shapeTable[link2 & 3][i8 & 3][link & 3];
                        int i152 = i13;
                        if (iSpecialChar != 1) {
                            i142 &= 1;
                        } else if (iSpecialChar == 2) {
                            i142 = ((i4 != 0 || (i8 & 2) == 0 || (link2 & 1) == 0 || cArr[i6] == 1612 || cArr[i6] == 1613 || ((link2 & 32) == 32 && (i8 & 16) == 16)) && !(i4 == 2 && cArr[i6] == 1617)) ? 0 : 1;
                        }
                        if (iSpecialChar != 2) {
                            if (i4 == 2 && cArr[i6] != 1617) {
                                cArr[i6] = TASHKEEL_SPACE_SUB;
                                i5 = i152;
                                z2 = true;
                            } else {
                                cArr[i6] = (char) (65136 + irrelevantPos[cArr[i6] - 1611] + i142);
                            }
                        } else {
                            cArr[i6] = (char) (65136 + (link >> 8) + i142);
                        }
                        i5 = i152;
                    }
                } else {
                    i5 = i10;
                }
                if ((link & 4) == 0) {
                    i7 = i6;
                    i9 = i8;
                    i8 = link;
                }
                i6--;
                if (i6 == i5) {
                    break;
                }
                if (i6 != -1) {
                    link = getLink(cArr[i6]);
                }
                i10 = i5;
            }
            link = link2;
        }
        if (z || z2) {
            iHandleGeneratedSpaces = handleGeneratedSpaces(cArr, i, i2);
        } else {
            iHandleGeneratedSpaces = i2;
        }
        if (z3 || z4) {
            return expandCompositChar(cArr, i, iHandleGeneratedSpaces, iNormalize, 0);
        }
        return iHandleGeneratedSpaces;
    }

    private int deShapeUnicode(char[] cArr, int i, int i2, int i3) throws ArabicShapingException {
        int iDeshapeNormalize = deshapeNormalize(cArr, i, i2);
        if (iDeshapeNormalize != 0) {
            return expandCompositChar(cArr, i, i2, iDeshapeNormalize, 1);
        }
        return i2;
    }

    private int internalShape(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4) throws ArabicShapingException {
        char c;
        char c2;
        int i5;
        if (i2 == 0) {
            return 0;
        }
        if (i4 == 0) {
            if ((this.options & 24) != 0 && (this.options & 65539) == 0) {
                return calculateSize(cArr, i, i2);
            }
            return i2;
        }
        char[] cArr3 = new char[i2 * 2];
        System.arraycopy(cArr, i, cArr3, 0, i2);
        if (this.isLogical) {
            invertBuffer(cArr3, 0, i2);
        }
        int i6 = this.options & 24;
        if (i6 != 8) {
            if (i6 != 16) {
                if (i6 == 24) {
                    i2 = shapeUnicode(cArr3, 0, i2, i4, 1);
                }
            } else {
                i2 = deShapeUnicode(cArr3, 0, i2, i4);
            }
        } else if ((this.options & TASHKEEL_MASK) != 0 && (this.options & TASHKEEL_MASK) != 786432) {
            i2 = shapeUnicode(cArr3, 0, i2, i4, 2);
        } else {
            int iShapeUnicode = shapeUnicode(cArr3, 0, i2, i4, 0);
            if ((917504 & this.options) == 786432) {
                i2 = handleTashkeelWithTatweel(cArr3, i2);
            } else {
                i2 = iShapeUnicode;
            }
        }
        if (i2 > i4) {
            throw new ArabicShapingException("not enough room for result data");
        }
        if ((this.options & 224) != 0) {
            int i7 = this.options & 256;
            if (i7 == 0) {
                c = 1632;
            } else if (i7 == 256) {
                c = 1776;
            } else {
                c2 = '0';
                i5 = this.options & 224;
                if (i5 != 32) {
                    int i8 = c2 - '0';
                    for (int i9 = 0; i9 < i2; i9++) {
                        char c3 = cArr3[i9];
                        if (c3 <= '9' && c3 >= '0') {
                            cArr3[i9] = (char) (cArr3[i9] + i8);
                        }
                    }
                } else if (i5 == 64) {
                    char c4 = (char) (c2 + '\t');
                    int i10 = '0' - c2;
                    for (int i11 = 0; i11 < i2; i11++) {
                        char c5 = cArr3[i11];
                        if (c5 <= c4 && c5 >= c2) {
                            cArr3[i11] = (char) (cArr3[i11] + i10);
                        }
                    }
                } else if (i5 == 96) {
                    shapeToArabicDigitsWithContext(cArr3, 0, i2, c2, false);
                } else if (i5 == 128) {
                    shapeToArabicDigitsWithContext(cArr3, 0, i2, c2, true);
                }
            }
            c2 = c;
            i5 = this.options & 224;
            if (i5 != 32) {
            }
        }
        if (this.isLogical) {
            invertBuffer(cArr3, 0, i2);
        }
        System.arraycopy(cArr3, 0, cArr2, i3, i2);
        return i2;
    }
}
