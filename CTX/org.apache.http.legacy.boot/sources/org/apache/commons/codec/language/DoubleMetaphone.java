package org.apache.commons.codec.language;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;

@Deprecated
public class DoubleMetaphone implements StringEncoder {
    private static final String VOWELS = "AEIOUY";
    protected int maxCodeLen = 4;
    private static final String[] SILENT_START = {"GN", "KN", "PN", "WR", "PS"};
    private static final String[] L_R_N_M_B_H_F_V_W_SPACE = {"L", "R", "N", "M", "B", "H", "F", "V", "W", " "};
    private static final String[] ES_EP_EB_EL_EY_IB_IL_IN_IE_EI_ER = {"ES", "EP", "EB", "EL", "EY", "IB", "IL", "IN", "IE", "EI", "ER"};
    private static final String[] L_T_K_S_N_M_B_Z = {"L", "T", "K", "S", "N", "M", "B", "Z"};

    public String doubleMetaphone(String str) {
        return doubleMetaphone(str, false);
    }

    public String doubleMetaphone(String str, boolean z) {
        ?? CleanInput = cleanInput(str);
        if (CleanInput == 0) {
            return null;
        }
        boolean zIsSlavoGermanic = isSlavoGermanic(CleanInput);
        ?? IsSilentStart = isSilentStart(CleanInput);
        DoubleMetaphoneResult doubleMetaphoneResult = new DoubleMetaphoneResult(getMaxCodeLen());
        while (!doubleMetaphoneResult.isComplete() && IsSilentStart <= CleanInput.length() - 1) {
            char cCharAt = CleanInput.charAt(IsSilentStart);
            if (cCharAt == 199) {
                doubleMetaphoneResult.append('S');
                IsSilentStart++;
            } else if (cCharAt != 209) {
                switch (cCharAt) {
                    case 'A':
                    case 'E':
                    case 'I':
                    case 'O':
                    case 'U':
                    case 'Y':
                        IsSilentStart = handleAEIOUY(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'B':
                        doubleMetaphoneResult.append('P');
                        int i = IsSilentStart + 1;
                        IsSilentStart = charAt(CleanInput, i) != 'B' ? i : IsSilentStart + 2;
                        break;
                    case 'C':
                        IsSilentStart = handleC(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'D':
                        IsSilentStart = handleD(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'F':
                        doubleMetaphoneResult.append('F');
                        int i2 = IsSilentStart + 1;
                        IsSilentStart = charAt(CleanInput, i2) != 'F' ? i2 : IsSilentStart + 2;
                        break;
                    case 'G':
                        IsSilentStart = handleG(CleanInput, doubleMetaphoneResult, IsSilentStart, zIsSlavoGermanic);
                        break;
                    case 'H':
                        IsSilentStart = handleH(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'J':
                        IsSilentStart = handleJ(CleanInput, doubleMetaphoneResult, IsSilentStart, zIsSlavoGermanic);
                        break;
                    case 'K':
                        doubleMetaphoneResult.append('K');
                        int i3 = IsSilentStart + 1;
                        IsSilentStart = charAt(CleanInput, i3) != 'K' ? i3 : IsSilentStart + 2;
                        break;
                    case 'L':
                        IsSilentStart = handleL(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'M':
                        doubleMetaphoneResult.append('M');
                        IsSilentStart = !conditionM0(CleanInput, IsSilentStart) ? IsSilentStart + 1 : IsSilentStart + 2;
                        break;
                    case 'N':
                        doubleMetaphoneResult.append('N');
                        int i4 = IsSilentStart + 1;
                        IsSilentStart = charAt(CleanInput, i4) != 'N' ? i4 : IsSilentStart + 2;
                        break;
                    case 'P':
                        IsSilentStart = handleP(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'Q':
                        doubleMetaphoneResult.append('K');
                        int i5 = IsSilentStart + 1;
                        IsSilentStart = charAt(CleanInput, i5) != 'Q' ? i5 : IsSilentStart + 2;
                        break;
                    case 'R':
                        IsSilentStart = handleR(CleanInput, doubleMetaphoneResult, IsSilentStart, zIsSlavoGermanic);
                        break;
                    case 'S':
                        IsSilentStart = handleS(CleanInput, doubleMetaphoneResult, IsSilentStart, zIsSlavoGermanic);
                        break;
                    case 'T':
                        IsSilentStart = handleT(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'V':
                        doubleMetaphoneResult.append('F');
                        int i6 = IsSilentStart + 1;
                        IsSilentStart = charAt(CleanInput, i6) != 'V' ? i6 : IsSilentStart + 2;
                        break;
                    case 'W':
                        IsSilentStart = handleW(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'X':
                        IsSilentStart = handleX(CleanInput, doubleMetaphoneResult, IsSilentStart);
                        break;
                    case 'Z':
                        IsSilentStart = handleZ(CleanInput, doubleMetaphoneResult, IsSilentStart, zIsSlavoGermanic);
                        break;
                    default:
                        IsSilentStart++;
                        break;
                }
            } else {
                doubleMetaphoneResult.append('N');
                IsSilentStart++;
            }
        }
        return z ? doubleMetaphoneResult.getAlternate() : doubleMetaphoneResult.getPrimary();
    }

    @Override
    public Object encode(Object obj) throws EncoderException {
        if (obj instanceof String) {
            return doubleMetaphone(obj);
        }
        throw new EncoderException("DoubleMetaphone encode parameter is not of type String");
    }

    @Override
    public String encode(String str) {
        return doubleMetaphone(str);
    }

    public boolean isDoubleMetaphoneEqual(String str, String str2) {
        return isDoubleMetaphoneEqual(str, str2, false);
    }

    public boolean isDoubleMetaphoneEqual(String str, String str2, boolean z) {
        return doubleMetaphone(str, z).equals(doubleMetaphone(str2, z));
    }

    public int getMaxCodeLen() {
        return this.maxCodeLen;
    }

    public void setMaxCodeLen(int i) {
        this.maxCodeLen = i;
    }

    private int handleAEIOUY(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (i == 0) {
            doubleMetaphoneResult.append('A');
        }
        return i + 1;
    }

    private int handleC(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (!conditionC0(str, i)) {
            if (i != 0 || !contains(str, i, 6, "CAESAR")) {
                if (!contains(str, i, 2, "CH")) {
                    if (contains(str, i, 2, "CZ") && !contains(str, i - 2, 4, "WICZ")) {
                        doubleMetaphoneResult.append('S', 'X');
                        return i + 2;
                    }
                    int i2 = i + 1;
                    if (!contains(str, i2, 3, "CIA")) {
                        if (!contains(str, i, 2, "CC") || (i == 1 && charAt(str, 0) == 'M')) {
                            if (!contains(str, i, 2, "CK", "CG", "CQ")) {
                                if (contains(str, i, 2, "CI", "CE", "CY")) {
                                    if (contains(str, i, 3, "CIO", "CIE", "CIA")) {
                                        doubleMetaphoneResult.append('S', 'X');
                                    } else {
                                        doubleMetaphoneResult.append('S');
                                    }
                                    return i + 2;
                                }
                                doubleMetaphoneResult.append('K');
                                if (contains(str, i2, 2, " C", " Q", " G")) {
                                    return i + 3;
                                }
                                if (contains(str, i2, 1, "C", "K", "Q") && !contains(str, i2, 2, "CE", "CI")) {
                                    return i + 2;
                                }
                                return i2;
                            }
                            doubleMetaphoneResult.append('K');
                            return i + 2;
                        }
                        return handleCC(str, doubleMetaphoneResult, i);
                    }
                    doubleMetaphoneResult.append('X');
                    return i + 3;
                }
                return handleCH(str, doubleMetaphoneResult, i);
            }
            doubleMetaphoneResult.append('S');
            return i + 2;
        }
        doubleMetaphoneResult.append('K');
        return i + 2;
    }

    private int handleCC(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        int i2 = i + 2;
        if (contains(str, i2, 1, "I", "E", "H") && !contains(str, i2, 2, "HU")) {
            if ((i == 1 && charAt(str, i - 1) == 'A') || contains(str, i - 1, 5, "UCCEE", "UCCES")) {
                doubleMetaphoneResult.append("KS");
            } else {
                doubleMetaphoneResult.append('X');
            }
            return i + 3;
        }
        doubleMetaphoneResult.append('K');
        return i2;
    }

    private int handleCH(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (i > 0 && contains(str, i, 4, "CHAE")) {
            doubleMetaphoneResult.append('K', 'X');
            return i + 2;
        }
        if (conditionCH0(str, i)) {
            doubleMetaphoneResult.append('K');
            return i + 2;
        }
        if (conditionCH1(str, i)) {
            doubleMetaphoneResult.append('K');
            return i + 2;
        }
        if (i > 0) {
            if (contains(str, 0, 2, "MC")) {
                doubleMetaphoneResult.append('K');
            } else {
                doubleMetaphoneResult.append('X', 'K');
            }
        } else {
            doubleMetaphoneResult.append('X');
        }
        return i + 2;
    }

    private int handleD(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (contains(str, i, 2, "DG")) {
            int i2 = i + 2;
            if (contains(str, i2, 1, "I", "E", "Y")) {
                doubleMetaphoneResult.append('J');
                return i + 3;
            }
            doubleMetaphoneResult.append("TK");
            return i2;
        }
        if (contains(str, i, 2, "DT", "DD")) {
            doubleMetaphoneResult.append('T');
            return i + 2;
        }
        doubleMetaphoneResult.append('T');
        return i + 1;
    }

    private int handleG(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i, boolean z) {
        int i2;
        int i3 = i + 1;
        if (charAt(str, i3) == 'H') {
            return handleGH(str, doubleMetaphoneResult, i);
        }
        if (charAt(str, i3) == 'N') {
            if (i == 1 && isVowel(charAt(str, 0)) && !z) {
                doubleMetaphoneResult.append("KN", "N");
            } else if (!contains(str, i + 2, 2, "EY") && charAt(str, i3) != 'Y' && !z) {
                doubleMetaphoneResult.append("N", "KN");
            } else {
                doubleMetaphoneResult.append("KN");
            }
            return i + 2;
        }
        if (contains(str, i3, 2, "LI") && !z) {
            doubleMetaphoneResult.append("KL", "L");
            return i + 2;
        }
        if (i == 0 && (charAt(str, i3) == 'Y' || contains(str, i3, 2, ES_EP_EB_EL_EY_IB_IL_IN_IE_EI_ER))) {
            doubleMetaphoneResult.append('K', 'J');
            return i + 2;
        }
        if (contains(str, i3, 2, "ER") || charAt(str, i3) == 'Y') {
            i2 = 3;
            if (!contains(str, 0, 6, "DANGER", "RANGER", "MANGER")) {
                int i4 = i - 1;
                if (!contains(str, i4, 1, "E", "I") && !contains(str, i4, 3, "RGY", "OGY")) {
                    doubleMetaphoneResult.append('K', 'J');
                    return i + 2;
                }
            }
        } else {
            i2 = 3;
        }
        if (contains(str, i3, 1, "E", "I", "Y") || contains(str, i - 1, 4, "AGGI", "OGGI")) {
            if (contains(str, 0, 4, "VAN ", "VON ") || contains(str, 0, i2, "SCH") || contains(str, i3, 2, "ET")) {
                doubleMetaphoneResult.append('K');
            } else if (contains(str, i3, 4, "IER")) {
                doubleMetaphoneResult.append('J');
            } else {
                doubleMetaphoneResult.append('J', 'K');
            }
            return i + 2;
        }
        if (charAt(str, i3) == 'G') {
            int i5 = i + 2;
            doubleMetaphoneResult.append('K');
            return i5;
        }
        doubleMetaphoneResult.append('K');
        return i3;
    }

    private int handleGH(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (i <= 0 || isVowel(charAt(str, i - 1))) {
            if (i != 0) {
                if ((i > 1 && contains(str, i - 2, 1, "B", "H", "D")) || ((i > 2 && contains(str, i - 3, 1, "B", "H", "D")) || (i > 3 && contains(str, i - 4, 1, "B", "H")))) {
                    return i + 2;
                }
                if (i > 2 && charAt(str, i - 1) == 'U' && contains(str, i - 3, 1, "C", "G", "L", "R", "T")) {
                    doubleMetaphoneResult.append('F');
                } else if (i > 0 && charAt(str, i - 1) != 'I') {
                    doubleMetaphoneResult.append('K');
                }
                return i + 2;
            }
            int i2 = i + 2;
            if (charAt(str, i2) == 'I') {
                doubleMetaphoneResult.append('J');
            } else {
                doubleMetaphoneResult.append('K');
            }
            return i2;
        }
        doubleMetaphoneResult.append('K');
        return i + 2;
    }

    private int handleH(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if ((i == 0 || isVowel(charAt(str, i - 1))) && isVowel(charAt(str, i + 1))) {
            doubleMetaphoneResult.append('H');
            return i + 2;
        }
        return i + 1;
    }

    private int handleJ(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i, boolean z) {
        if (contains(str, i, 4, "JOSE") || contains(str, 0, 4, "SAN ")) {
            if ((i == 0 && charAt(str, i + 4) == ' ') || str.length() == 4 || contains(str, 0, 4, "SAN ")) {
                doubleMetaphoneResult.append('H');
            } else {
                doubleMetaphoneResult.append('J', 'H');
            }
            return i + 1;
        }
        if (i == 0 && !contains(str, i, 4, "JOSE")) {
            doubleMetaphoneResult.append('J', 'A');
        } else {
            int i2 = i - 1;
            if (isVowel(charAt(str, i2)) && !z) {
                int i3 = i + 1;
                if (charAt(str, i3) == 'A' || charAt(str, i3) == 'O') {
                    doubleMetaphoneResult.append('J', 'H');
                }
            } else if (i == str.length() - 1) {
                doubleMetaphoneResult.append('J', ' ');
            } else if (!contains(str, i + 1, 1, L_T_K_S_N_M_B_Z) && !contains(str, i2, 1, "S", "K", "L")) {
                doubleMetaphoneResult.append('J');
            }
        }
        int i4 = i + 1;
        if (charAt(str, i4) == 'J') {
            return i + 2;
        }
        return i4;
    }

    private int handleL(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        doubleMetaphoneResult.append('L');
        int i2 = i + 1;
        if (charAt(str, i2) == 'L') {
            if (conditionL0(str, i)) {
                doubleMetaphoneResult.appendAlternate(' ');
            }
            return i + 2;
        }
        return i2;
    }

    private int handleP(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        int i2 = i + 1;
        if (charAt(str, i2) == 'H') {
            doubleMetaphoneResult.append('F');
            return i + 2;
        }
        doubleMetaphoneResult.append('P');
        if (contains(str, i2, 1, "P", "B")) {
            i2 = i + 2;
        }
        return i2;
    }

    private int handleR(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i, boolean z) {
        if (i == str.length() - 1 && !z && contains(str, i - 2, 2, "IE") && !contains(str, i - 4, 2, "ME", "MA")) {
            doubleMetaphoneResult.appendAlternate('R');
        } else {
            doubleMetaphoneResult.append('R');
        }
        int i2 = i + 1;
        return charAt(str, i2) == 'R' ? i + 2 : i2;
    }

    private int handleS(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i, boolean z) {
        if (contains(str, i - 1, 3, "ISL", "YSL")) {
            return i + 1;
        }
        if (i == 0 && contains(str, i, 5, "SUGAR")) {
            doubleMetaphoneResult.append('X', 'S');
            return i + 1;
        }
        if (contains(str, i, 2, "SH")) {
            if (contains(str, i + 1, 4, "HEIM", "HOEK", "HOLM", "HOLZ")) {
                doubleMetaphoneResult.append('S');
            } else {
                doubleMetaphoneResult.append('X');
            }
            return i + 2;
        }
        if (contains(str, i, 3, "SIO", "SIA") || contains(str, i, 4, "SIAN")) {
            if (z) {
                doubleMetaphoneResult.append('S');
            } else {
                doubleMetaphoneResult.append('S', 'X');
            }
            return i + 3;
        }
        if (i != 0 || !contains(str, i + 1, 1, "M", "N", "L", "W")) {
            int i2 = i + 1;
            if (!contains(str, i2, 1, "Z")) {
                if (contains(str, i, 2, "SC")) {
                    return handleSC(str, doubleMetaphoneResult, i);
                }
                if (i == str.length() - 1 && contains(str, i - 2, 2, "AI", "OI")) {
                    doubleMetaphoneResult.appendAlternate('S');
                } else {
                    doubleMetaphoneResult.append('S');
                }
                return contains(str, i2, 1, "S", "Z") ? i + 2 : i2;
            }
        }
        doubleMetaphoneResult.append('S', 'X');
        int i3 = i + 1;
        return contains(str, i3, 1, "Z") ? i + 2 : i3;
    }

    private int handleSC(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        int i2 = i + 2;
        if (charAt(str, i2) == 'H') {
            int i3 = i + 3;
            if (contains(str, i3, 2, "OO", "ER", "EN", "UY", "ED", "EM")) {
                if (contains(str, i3, 2, "ER", "EN")) {
                    doubleMetaphoneResult.append("X", "SK");
                } else {
                    doubleMetaphoneResult.append("SK");
                }
            } else if (i == 0 && !isVowel(charAt(str, 3)) && charAt(str, 3) != 'W') {
                doubleMetaphoneResult.append('X', 'S');
            } else {
                doubleMetaphoneResult.append('X');
            }
        } else if (contains(str, i2, 1, "I", "E", "Y")) {
            doubleMetaphoneResult.append('S');
        } else {
            doubleMetaphoneResult.append("SK");
        }
        return i + 3;
    }

    private int handleT(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (contains(str, i, 4, "TION")) {
            doubleMetaphoneResult.append('X');
            return i + 3;
        }
        if (contains(str, i, 3, "TIA", "TCH")) {
            doubleMetaphoneResult.append('X');
            return i + 3;
        }
        if (contains(str, i, 2, "TH") || contains(str, i, 3, "TTH")) {
            int i2 = i + 2;
            if (contains(str, i2, 2, "OM", "AM") || contains(str, 0, 4, "VAN ", "VON ") || contains(str, 0, 3, "SCH")) {
                doubleMetaphoneResult.append('T');
                return i2;
            }
            doubleMetaphoneResult.append('0', 'T');
            return i2;
        }
        doubleMetaphoneResult.append('T');
        int i3 = i + 1;
        return contains(str, i3, 1, "T", "D") ? i + 2 : i3;
    }

    private int handleW(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (contains(str, i, 2, "WR")) {
            doubleMetaphoneResult.append('R');
            return i + 2;
        }
        if (i == 0) {
            int i2 = i + 1;
            if (isVowel(charAt(str, i2)) || contains(str, i, 2, "WH")) {
                if (isVowel(charAt(str, i2))) {
                    doubleMetaphoneResult.append('A', 'F');
                } else {
                    doubleMetaphoneResult.append('A');
                }
                return i2;
            }
        }
        if ((i == str.length() - 1 && isVowel(charAt(str, i - 1))) || contains(str, i - 1, 5, "EWSKI", "EWSKY", "OWSKI", "OWSKY") || contains(str, 0, 3, "SCH")) {
            doubleMetaphoneResult.appendAlternate('F');
            return i + 1;
        }
        if (contains(str, i, 4, "WICZ", "WITZ")) {
            doubleMetaphoneResult.append("TS", "FX");
            return i + 4;
        }
        return i + 1;
    }

    private int handleX(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i) {
        if (i != 0) {
            if (i != str.length() - 1 || (!contains(str, i - 3, 3, "IAU", "EAU") && !contains(str, i - 2, 2, "AU", "OU"))) {
                doubleMetaphoneResult.append("KS");
            }
            int i2 = i + 1;
            return contains(str, i2, 1, "C", "X") ? i + 2 : i2;
        }
        doubleMetaphoneResult.append('S');
        return i + 1;
    }

    private int handleZ(String str, DoubleMetaphoneResult doubleMetaphoneResult, int i, boolean z) {
        int i2 = i + 1;
        if (charAt(str, i2) == 'H') {
            doubleMetaphoneResult.append('J');
            return i + 2;
        }
        if (contains(str, i2, 2, "ZO", "ZI", "ZA") || (z && i > 0 && charAt(str, i - 1) != 'T')) {
            doubleMetaphoneResult.append("S", "TS");
        } else {
            doubleMetaphoneResult.append('S');
        }
        if (charAt(str, i2) == 'Z') {
            i2 = i + 2;
        }
        return i2;
    }

    private boolean conditionC0(String str, int i) {
        if (contains(str, i, 4, "CHIA")) {
            return true;
        }
        if (i <= 1) {
            return false;
        }
        int i2 = i - 2;
        if (isVowel(charAt(str, i2)) || !contains(str, i - 1, 3, "ACH")) {
            return false;
        }
        char cCharAt = charAt(str, i + 2);
        return !(cCharAt == 'I' || cCharAt == 'E') || contains(str, i2, 6, "BACHER", "MACHER");
    }

    private boolean conditionCH0(String str, int i) {
        if (i != 0) {
            return false;
        }
        int i2 = i + 1;
        if ((!contains(str, i2, 5, "HARAC", "HARIS") && !contains(str, i2, 3, "HOR", "HYM", "HIA", "HEM")) || contains(str, 0, 5, "CHORE")) {
            return false;
        }
        return true;
    }

    private boolean conditionCH1(String str, int i) {
        if (contains(str, 0, 4, "VAN ", "VON ") || contains(str, 0, 3, "SCH") || contains(str, i - 2, 6, "ORCHES", "ARCHIT", "ORCHID")) {
            return true;
        }
        int i2 = i + 2;
        if (contains(str, i2, 1, "T", "S")) {
            return true;
        }
        return (contains(str, i + (-1), 1, "A", "O", "U", "E") || i == 0) && (contains(str, i2, 1, L_R_N_M_B_H_F_V_W_SPACE) || i + 1 == str.length() - 1);
    }

    private boolean conditionL0(String str, int i) {
        if (i == str.length() - 3 && contains(str, i - 1, 4, "ILLO", "ILLA", "ALLE")) {
            return true;
        }
        int i2 = i - 1;
        return (contains(str, i2, 2, "AS", "OS") || contains(str, str.length() - 1, 1, "A", "O")) && contains(str, i2, 4, "ALLE");
    }

    private boolean conditionM0(String str, int i) {
        int i2 = i + 1;
        if (charAt(str, i2) == 'M') {
            return true;
        }
        return contains(str, i + (-1), 3, "UMB") && (i2 == str.length() - 1 || contains(str, i + 2, 2, "ER"));
    }

    private boolean isSlavoGermanic(String str) {
        return str.indexOf(87) > -1 || str.indexOf(75) > -1 || str.indexOf("CZ") > -1 || str.indexOf("WITZ") > -1;
    }

    private boolean isVowel(char c) {
        return VOWELS.indexOf(c) != -1;
    }

    private boolean isSilentStart(String str) {
        for (int i = 0; i < SILENT_START.length; i++) {
            if (str.startsWith(SILENT_START[i])) {
                return true;
            }
        }
        return false;
    }

    private String cleanInput(String str) {
        if (str == null) {
            return null;
        }
        String strTrim = str.trim();
        if (strTrim.length() == 0) {
            return null;
        }
        return strTrim.toUpperCase();
    }

    protected char charAt(String str, int i) {
        if (i < 0 || i >= str.length()) {
            return (char) 0;
        }
        return str.charAt(i);
    }

    private static boolean contains(String str, int i, int i2, String str2) {
        return contains(str, i, i2, new String[]{str2});
    }

    private static boolean contains(String str, int i, int i2, String str2, String str3) {
        return contains(str, i, i2, new String[]{str2, str3});
    }

    private static boolean contains(String str, int i, int i2, String str2, String str3, String str4) {
        return contains(str, i, i2, new String[]{str2, str3, str4});
    }

    private static boolean contains(String str, int i, int i2, String str2, String str3, String str4, String str5) {
        return contains(str, i, i2, new String[]{str2, str3, str4, str5});
    }

    private static boolean contains(String str, int i, int i2, String str2, String str3, String str4, String str5, String str6) {
        return contains(str, i, i2, new String[]{str2, str3, str4, str5, str6});
    }

    private static boolean contains(String str, int i, int i2, String str2, String str3, String str4, String str5, String str6, String str7) {
        return contains(str, i, i2, new String[]{str2, str3, str4, str5, str6, str7});
    }

    protected static boolean contains(String str, int i, int i2, String[] strArr) {
        int i3;
        if (i < 0 || (i3 = i2 + i) > str.length()) {
            return false;
        }
        String strSubstring = str.substring(i, i3);
        for (String str2 : strArr) {
            if (strSubstring.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    public class DoubleMetaphoneResult {
        private StringBuffer alternate;
        private int maxLength;
        private StringBuffer primary;

        public DoubleMetaphoneResult(int i) {
            this.primary = new StringBuffer(DoubleMetaphone.this.getMaxCodeLen());
            this.alternate = new StringBuffer(DoubleMetaphone.this.getMaxCodeLen());
            this.maxLength = i;
        }

        public void append(char c) {
            appendPrimary(c);
            appendAlternate(c);
        }

        public void append(char c, char c2) {
            appendPrimary(c);
            appendAlternate(c2);
        }

        public void appendPrimary(char c) {
            if (this.primary.length() < this.maxLength) {
                this.primary.append(c);
            }
        }

        public void appendAlternate(char c) {
            if (this.alternate.length() < this.maxLength) {
                this.alternate.append(c);
            }
        }

        public void append(String str) {
            appendPrimary(str);
            appendAlternate(str);
        }

        public void append(String str, String str2) {
            appendPrimary(str);
            appendAlternate(str2);
        }

        public void appendPrimary(String str) {
            int length = this.maxLength - this.primary.length();
            if (str.length() <= length) {
                this.primary.append(str);
            } else {
                this.primary.append(str.substring(0, length));
            }
        }

        public void appendAlternate(String str) {
            int length = this.maxLength - this.alternate.length();
            if (str.length() <= length) {
                this.alternate.append(str);
            } else {
                this.alternate.append(str.substring(0, length));
            }
        }

        public String getPrimary() {
            return this.primary.toString();
        }

        public String getAlternate() {
            return this.alternate.toString();
        }

        public boolean isComplete() {
            return this.primary.length() >= this.maxLength && this.alternate.length() >= this.maxLength;
        }
    }
}
