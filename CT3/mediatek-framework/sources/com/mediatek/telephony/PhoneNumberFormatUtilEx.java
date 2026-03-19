package com.mediatek.telephony;

import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import com.mediatek.mmsdk.BaseParameters;
import com.mediatek.pq.PictureQuality;
import java.util.Arrays;
import java.util.Locale;

public class PhoneNumberFormatUtilEx {
    public static final boolean DEBUG = false;
    public static final int FORMAT_AUSTRALIA = 21;
    public static final int FORMAT_BRAZIL = 23;
    public static final int FORMAT_CHINA_HONGKONG = 4;
    public static final int FORMAT_CHINA_MACAU = 5;
    public static final int FORMAT_CHINA_MAINLAND = 3;
    public static final int FORMAT_ENGLAND = 7;
    public static final int FORMAT_FRANCE = 8;
    public static final int FORMAT_GERMANY = 10;
    public static final int FORMAT_INDIA = 12;
    public static final int FORMAT_INDONESIA = 16;
    public static final int FORMAT_ITALY = 9;
    public static final int FORMAT_JAPAN = 2;
    public static final int FORMAT_MALAYSIA = 14;
    public static final int FORMAT_NANP = 1;
    public static final int FORMAT_NEW_ZEALAND = 22;
    public static final int FORMAT_POLAND = 20;
    public static final int FORMAT_PORTUGAL = 19;
    public static final int FORMAT_RUSSIAN = 11;
    public static final int FORMAT_SINGAPORE = 15;
    public static final int FORMAT_SPAIN = 13;
    public static final int FORMAT_TAIWAN = 6;
    public static final int FORMAT_THAILAND = 17;
    public static final int FORMAT_TURKEY = 24;
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_VIETNAM = 18;
    public static final String TAG = "PhoneNumberFormatUtilEx";
    private static final String[] NANP_COUNTRIES = {"US", "CA", "AS", "AI", "AG", "BS", "BB", "BM", "VG", "KY", "DM", "DO", "GD", "GU", "JM", "PR", "MS", "MP", "KN", "LC", "VC", "TT", "TC", "VI"};
    public static final String[] NANP_INTERNATIONAL_PREFIXS = {"011"};
    public static final String[] JAPAN_INTERNATIONAL_PREFIXS = {"010", "001", "0041", "0061"};
    public static final String[] HONGKONG_INTERNATIONAL_PREFIXS = {"001", "0080", "0082", "009"};
    public static final String[] TAIWAN_INTERNATIONAL_PREFIXS = {"002", "005", "006", "007", "009", "019"};
    public static final String[] FRANCE_INTERNATIONAL_PREFIXS = {"00", "40", "50", "70", "90"};
    public static final String[] SINGAPORE_INTERNATIONAL_PREFIXS = {"001", "002", "008", "012", "013", "018", "019"};
    public static final String[] INDONESIA_INTERNATIONAL_PREFIXS = {"001", "007", "008", "009"};
    public static final String[] THAILAND_INTERNATIONAL_PREFIXS = {"001", "004", "005", "006", "007", "008", "009"};
    public static final String[] AUSTRALIA_INTERNATIONAL_PREFIXS = {"0011", "0014", "0015", "0016", "0018", "0019"};
    public static final String[] BRAZIL_INTERNATIONAL_PREFIXS = {"0012", "0014", "0015", "0021", "0023", "0025", "0031", "0041"};
    public static String[] FORMAT_COUNTRY_CODES = {BaseParameters.FEATURE_MASK_3DNR_ON, "81", "86", "852", "853", "886", "44", "33", "39", "49", "7", "91", "34", "60", "65", "62", "66", "84", "351", "48", "61", "64", "55", "90"};
    public static final String[] FORMAT_COUNTRY_NAMES = {"US", "JP", "CN", "HK", "MO", "TW", "GB", "FR", "IT", "DE", "RU", "IN", "ES", "MY", "SG", "ID", "TH", "VN", "PT", "PL", "AU", "NZ", "BR", "TR"};
    private static final int[] INDIA_THREE_DIGIG_AREA_CODES = {120, 121, 122, 124, 129, 130, 131, 132, 135, 141, 144, 145, 151, 154, 160, 161, 164, 171, 172, 175, 177, 180, 181, 183, 184, 186, 191, 194, 212, 215, 217, 230, 231, 233, 240, 241, 250, 251, 253, 257, 260, 261, 265, 268, 278, 281, 285, 286, 288, 291, 294, 326, 341, 342, 343, 353, 354, 360, 361, 364, 368, 369, 370, 372, 373, 374, 376, 381, 385, 389, 413, 416, 421, 422, 423, 424, 427, 431, 435, 451, 452, 461, 462, 468, 469, 470, 471, 474, 475, 476, 477, 478, 479, 480, 481, 483, 484, 485, 487, 490, 491, 494, 495, 496, 497, PictureQuality.GAMMA_LUT_SIZE, 515, 522, 532, 535, 542, 548, 551, 562, 565, 571, 581, 591, 595, 612, 621, 631, 641, 651, 657, 661, 663, 671, 674, 680, 712, 721, 724, 731, 733, 734, 744, 747, 751, 755, 761, 771, 788, 816, 820, 821, 824, 831, 832, 836, 861, 863, 866, 870, 877, 878, 883, 884, 891};
    private static final int[] Germany_THREE_PART_REGION_CODES = {202, 203, 208, 209, 212, 214, 221, 228, 234, 249, 310, 335, 340, 345, 365, 375, 385, 395, 457, 458, 459, 700, 709, 710, 728, 729, 749, 759, 769, 778, 779, 786, 787, 788, 789, 792, 798, 799, 800, 872, 875, 879, 900, 902, 903, 906};
    private static final int[] Germany_FOUR_PART_REGION_CODES = {3301, 3302, 3303, 3304, 3306, 3307, 3321, 3322, 3327, 3328, 3329, 3331, 3332, 3334, 3335, 3337, 3338, 3341, 3342, 3344, 3346, 3361, 3362, 3364, 3366, 3371, 3372, 3375, 3377, 3378, 3379, 3381, 3382, 3385, 3386, 3391, 3394, 3395, 3421, 3423, 3425, 3431, 3433, 3435, 3437, 3441, 3443, 3445, 3447, 3448, 3461, 3462, 3464, 3466, 3471, 3473, 3475, 3476, 3491, 3493, 3494, 3496, 3501, 3504, 3521, 3522, 3523, 3525, 3528, 3529, 3531, 3533, 3537, 3541, 3542, 3544, 3546, 3561, 3562, 3563, 3564, 3571, 3573, 3574, 3576, 3578, 3581, 3583, 3585, 3586, 3588, 3591, 3592, 3594, 3596, 3601, 3603, 3605, 3606, 3621, 3622, 3623, 3624, 3626, 3627, 3628, 3629, 3631, 3632, 3634, 3635, 3636, 3641, 3643, 3644, 3647, 3661, 3663, 3671, 3672, 3675, 3677, 3679, 3680, 3681, 3682, 3683, 3685, 3686, 3691, 3693, 3695, 3721, 3722, 3723, 3724, 3725, 3726, 3727, 3731, 3733, 3735, 3737, 3741, 3744, 3745, 3761, 3762, 3763, 3764, 3765, 3771, 3772, 3773, 3774, 3821, 3831, 3834, 3838, 3841, 3843, 3844, 3847, 3871, 3874, 3876, 3877, 3881, 3883, 3886, 3901, 3921, 3923, 3925, 3928, 3931, 3933, 3935, 3937, 3941, 3942, 3943, 3944, 3946, 3947, 3949, 3961, 3962, 3963, 3964, 3965, 3966, 3967, 3968, 3969, 3971, 3973, 3976, 3981, 3984, 3991, 3994, 3996, 3997};
    private static final int[] ITALY_MOBILE_PREFIXS = {328, 329, 330, 333, 334, 335, 336, 337, 338, 339, 347, 348, 349, 360, 368, 380, 388, 389};

    public static int getFormatTypeForLocale(Locale locale) {
        String simIso = getDefaultSimCountryIso();
        log("getFormatTypeForLocale Get sim sio:" + simIso);
        return getFormatTypeFromCountryCode(simIso);
    }

    static String getDefaultSimCountryIso() {
        int simId = 0;
        if (TelephonyManager.getDefault().hasIccCard(0)) {
            simId = 0;
        } else if (TelephonyManager.getDefault().hasIccCard(1)) {
            simId = 1;
        } else if (TelephonyManager.getDefault().hasIccCard(2)) {
            simId = 2;
        } else if (TelephonyManager.getDefault().hasIccCard(3)) {
            simId = 3;
        }
        int[] subId = SubscriptionManager.getSubId(simId);
        if (subId == null || subId.length <= 0) {
            return null;
        }
        String iso = TelephonyManager.getDefault().getSimCountryIso(subId[0]);
        return iso;
    }

    private static int getFormatTypeFromCountryCodeInternal(String country) {
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(country) == 0) {
                return 1;
            }
        }
        return "jp".compareToIgnoreCase(country) == 0 ? 2 : 0;
    }

    public static int getFormatTypeFromCountryCode(String country) {
        int i = 0;
        int type = 0;
        if (country != null && country.length() != 0 && (type = getFormatTypeFromCountryCodeInternal(country)) == 0) {
            int index = 0;
            String[] strArr = FORMAT_COUNTRY_NAMES;
            int length = strArr.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                String name = strArr[i];
                index++;
                if (name.compareToIgnoreCase(country) != 0) {
                    i++;
                } else {
                    type = index;
                    break;
                }
            }
            if (type == 0 && "UK".compareToIgnoreCase(country) == 0) {
                type = 7;
            }
        }
        log("Get Format Type:" + type);
        return type;
    }

    public static String formatNumber(String source) {
        Locale sCachedLocale = Locale.getDefault();
        return formatNumber(source, getFormatTypeForLocale(sCachedLocale));
    }

    public static void formatNumber(Editable text, int defaultFormattingType) {
        String result = formatNumber(text.toString(), defaultFormattingType);
        if (result == null || result.equals(text.toString())) {
            return;
        }
        int oldIndex = Selection.getSelectionStart(text);
        int digitCount = oldIndex;
        for (int i = 0; i < oldIndex; i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '-') {
                digitCount--;
            }
        }
        text.replace(0, text.length(), result);
        int count = 0;
        int i2 = 0;
        while (i2 < text.length() && count < digitCount) {
            char c2 = text.charAt(i2);
            if (c2 != ' ' && c2 != '-') {
                count++;
            }
            i2++;
        }
        Selection.setSelection(text, i2);
    }

    static boolean checkInputNormalNumber(CharSequence text) {
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if ((c < '0' || c > '9') && c != '*' && c != '#' && c != '+' && c != ' ' && c != '-') {
                return false;
            }
        }
        return true;
    }

    public static String formatNumber(String text, int defaultFormattingType) {
        log("MTK Format Number:" + text + " " + defaultFormattingType);
        if (!checkInputNormalNumber(text)) {
            log("Abnormal Number:" + text + ", do nothing.");
            return text;
        }
        String text2 = removeAllDash(new StringBuilder(text));
        int formatType = defaultFormattingType == 0 ? 1 : defaultFormattingType;
        if (text2.length() > 2 && text2.charAt(0) == '+') {
            if (text2.charAt(1) == '1') {
                formatType = 1;
            } else if (text2.length() >= 3 && text2.charAt(1) == '8' && text2.charAt(2) == '1') {
                formatType = 2;
            } else if (formatType == 1 || formatType == 2) {
                String result = mtkFormatNumber(text2, formatType);
                return result;
            }
        }
        log("formatNumber:" + formatType);
        switch (formatType) {
            case 1:
            case 2:
                String result2 = PhoneNumberUtils.formatNumber(text2, formatType);
                return result2;
            default:
                String result3 = mtkFormatNumber(text2, formatType);
                return result3;
        }
    }

    static String mtkFormatNumber(String text, int defaultFormatType) {
        log("MTK Format Number:" + text + " " + defaultFormatType);
        int length = text.length();
        if (length < 6) {
            return text;
        }
        if (text.contains("*") || text.contains("#") || text.contains("@")) {
            return removeAllDash(new StringBuilder(text));
        }
        int formatType = defaultFormatType;
        int[] match = getFormatTypeFromNumber(text, defaultFormatType);
        int startIndex = 0;
        if (match != null && match[1] != 0) {
            formatType = match[1];
            startIndex = match[0];
        }
        if (length < startIndex + 4 || length > startIndex + 15) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        int blankPosition = removeAllDashAndFormatBlank(sb, startIndex);
        if (sb.length() < startIndex + 4 || (sb.length() == startIndex + 4 && sb.charAt(blankPosition + 1) == '0')) {
            return sb.toString();
        }
        switch (formatType) {
            case 1:
                if (blankPosition >= 0) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder(sb.substring(startIndex + 1));
                    PhoneNumberUtils.formatNanpNumber(ssb);
                    String result = sb.substring(0, startIndex + 1).concat(ssb.toString());
                } else {
                    SpannableStringBuilder ssb2 = new SpannableStringBuilder(sb);
                    PhoneNumberUtils.formatNanpNumber(ssb2);
                    String result2 = ssb2.toString();
                }
                break;
            case 2:
                if (blankPosition >= 0) {
                    SpannableStringBuilder ssb22 = new SpannableStringBuilder(sb.substring(startIndex + 1));
                    PhoneNumberUtils.formatJapaneseNumber(ssb22);
                    String result3 = sb.substring(0, startIndex + 1).concat(ssb22.toString());
                } else {
                    SpannableStringBuilder ssb23 = new SpannableStringBuilder(sb);
                    PhoneNumberUtils.formatJapaneseNumber(ssb23);
                    String result4 = ssb23.toString();
                }
                break;
            case 3:
                String result5 = formatChinaNumber(sb, blankPosition);
                break;
            case 4:
            case FORMAT_SINGAPORE:
                String result6 = formatHeightLengthWithoutRegionCodeNumber(sb, blankPosition);
                break;
            case 5:
                String result7 = formatMacauNumber(sb, blankPosition);
                break;
            case 6:
                String result8 = formatTaiwanNumber(sb, blankPosition);
                break;
            case 7:
                String result9 = formatEnglandNumber(sb, blankPosition);
                break;
            case 8:
                String result10 = formatFranceNumber(sb, blankPosition);
                break;
            case FORMAT_ITALY:
                String result11 = formatItalyNumber(sb, blankPosition);
                break;
            case FORMAT_GERMANY:
                String result12 = formatGermanyNumber(sb, blankPosition);
                break;
            case FORMAT_RUSSIAN:
                String result13 = formatRussianNumber(sb, blankPosition);
                break;
            case FORMAT_INDIA:
                String result14 = formatIndiaNumber(sb, blankPosition);
                break;
            case FORMAT_SPAIN:
                String result15 = formatSpainNumber(sb, blankPosition);
                break;
            case FORMAT_MALAYSIA:
                String result16 = formatMalaysiaNumber(sb, blankPosition);
                break;
            case 16:
                String result17 = formatIndonesiaNumber(sb, blankPosition);
                break;
            case FORMAT_THAILAND:
                String result18 = formatThailandNumber(sb, blankPosition);
                break;
            case FORMAT_VIETNAM:
                String result19 = formatVietnamNubmer(sb, blankPosition);
                break;
            case FORMAT_PORTUGAL:
                String result20 = formatPortugalNumber(sb, blankPosition);
                break;
            case 20:
                String result21 = formatPolandNumber(sb, blankPosition);
                break;
            case FORMAT_AUSTRALIA:
                String result22 = formatAustraliaNumber(sb, blankPosition);
                break;
            case FORMAT_NEW_ZEALAND:
                String result23 = formatNewZealandNumber(sb, blankPosition);
                break;
            case FORMAT_BRAZIL:
                String result24 = formatBrazilNumber(sb, blankPosition);
                break;
            case FORMAT_TURKEY:
                String result25 = formatTurkeyNumber(sb, blankPosition);
                break;
            default:
                String result26 = removeAllDash(sb);
                break;
        }
        return text;
    }

    private static int[] getFormatTypeByCommonPrefix(String text) {
        int result = 0;
        int index = 0;
        int startIndex = 0;
        int[] match = new int[2];
        if (text.length() > 0 && text.charAt(0) == '+') {
            startIndex = 1;
        } else if (text.length() > 1 && text.charAt(0) == '0' && text.charAt(1) == '0') {
            startIndex = 2;
        }
        if (startIndex != 0) {
            String[] strArr = FORMAT_COUNTRY_CODES;
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String pattern = strArr[i];
                index++;
                if (!text.startsWith(pattern, startIndex)) {
                    i++;
                } else {
                    result = index;
                    startIndex += pattern.length();
                    break;
                }
            }
        }
        if (result == 0) {
            startIndex = 0;
        }
        match[0] = startIndex;
        match[1] = result;
        return match;
    }

    private static int[] getFormatNumberBySpecialPrefix(String text, String[] prefixs) {
        int result = 0;
        int index = 0;
        int startIndex = 0;
        int[] match = new int[2];
        if (text.charAt(0) == '+') {
            startIndex = 1;
        } else {
            int length = prefixs.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String prefix = prefixs[i];
                if (!text.startsWith(prefix)) {
                    i++;
                } else {
                    startIndex = prefix.length();
                    break;
                }
            }
        }
        if (startIndex > 0) {
            String[] strArr = FORMAT_COUNTRY_CODES;
            int length2 = strArr.length;
            int i2 = 0;
            while (true) {
                if (i2 >= length2) {
                    break;
                }
                String pattern = strArr[i2];
                index++;
                if (!text.startsWith(pattern, startIndex)) {
                    i2++;
                } else {
                    result = index;
                    startIndex += pattern.length();
                    break;
                }
            }
        }
        if (result == 0) {
            startIndex = 0;
        }
        match[0] = startIndex;
        match[1] = result;
        return match;
    }

    private static int[] getFormatTypeFromNumber(String text, int defaultFormatType) {
        switch (defaultFormatType) {
            case 1:
                int[] match = getFormatNumberBySpecialPrefix(text, NANP_INTERNATIONAL_PREFIXS);
                return match;
            case 2:
                int[] match2 = getFormatNumberBySpecialPrefix(text, JAPAN_INTERNATIONAL_PREFIXS);
                return match2;
            case 3:
            case 5:
            case 7:
            case FORMAT_ITALY:
            case FORMAT_GERMANY:
            case FORMAT_RUSSIAN:
            case FORMAT_INDIA:
            case FORMAT_SPAIN:
            case FORMAT_MALAYSIA:
            case FORMAT_VIETNAM:
            case FORMAT_PORTUGAL:
            case 20:
            case FORMAT_NEW_ZEALAND:
            case FORMAT_TURKEY:
                int[] match3 = getFormatTypeByCommonPrefix(text);
                return match3;
            case 4:
                int[] match4 = getFormatNumberBySpecialPrefix(text, HONGKONG_INTERNATIONAL_PREFIXS);
                return match4;
            case 6:
                int[] match5 = getFormatNumberBySpecialPrefix(text, TAIWAN_INTERNATIONAL_PREFIXS);
                return match5;
            case 8:
                int[] match6 = getFormatNumberBySpecialPrefix(text, FRANCE_INTERNATIONAL_PREFIXS);
                return match6;
            case FORMAT_SINGAPORE:
                int[] match7 = getFormatNumberBySpecialPrefix(text, SINGAPORE_INTERNATIONAL_PREFIXS);
                return match7;
            case 16:
                int[] match8 = getFormatNumberBySpecialPrefix(text, INDONESIA_INTERNATIONAL_PREFIXS);
                return match8;
            case FORMAT_THAILAND:
                int[] match9 = getFormatNumberBySpecialPrefix(text, THAILAND_INTERNATIONAL_PREFIXS);
                return match9;
            case FORMAT_AUSTRALIA:
                int[] match10 = getFormatNumberBySpecialPrefix(text, AUSTRALIA_INTERNATIONAL_PREFIXS);
                return match10;
            case FORMAT_BRAZIL:
                int[] match11 = getFormatNumberBySpecialPrefix(text, BRAZIL_INTERNATIONAL_PREFIXS);
                return match11;
            default:
                return null;
        }
    }

    private static String removeAllDash(StringBuilder sb) {
        int p = 0;
        while (p < sb.length()) {
            if (sb.charAt(p) == '-' || sb.charAt(p) == ' ') {
                sb.deleteCharAt(p);
            } else {
                p++;
            }
        }
        return sb.toString();
    }

    private static int removeAllDashAndFormatBlank(StringBuilder sb, int startIndex) {
        int p = 0;
        while (p < sb.length()) {
            if (sb.charAt(p) == '-' || sb.charAt(p) == ' ') {
                sb.deleteCharAt(p);
            } else {
                p++;
            }
        }
        if (startIndex <= 0) {
            return -1;
        }
        sb.replace(startIndex, startIndex, " ");
        return startIndex;
    }

    private static String removeTrailingDashes(StringBuilder sb) {
        for (int len = sb.length(); len > 0 && sb.charAt(len - 1) == '-'; len--) {
            sb.delete(len - 1, len);
        }
        return sb.toString();
    }

    private static String formatChinaNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            if ((c1 == '1' && c2 == '0') || c1 == '2') {
                numDashes2 = 1;
                dashPositions[0] = index + 2;
            } else if (c1 == '1') {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 8) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 7;
                } else {
                    numDashes2 = numDashes;
                }
            } else {
                numDashes2 = 1;
                dashPositions[0] = index + 3;
            }
        } else {
            char c12 = sb.charAt(phoneNumPosition);
            char c22 = sb.charAt(phoneNumPosition + 1);
            if (c12 == '1' && c22 != '0') {
                if (length > phoneNumPosition + 4) {
                    dashPositions[0] = phoneNumPosition + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > phoneNumPosition + 8) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = phoneNumPosition + 7;
                }
            } else if (c12 == '1' && c22 == '0') {
                if (length > phoneNumPosition + 3) {
                    numDashes2 = 1;
                    dashPositions[0] = phoneNumPosition + 2;
                }
            } else if (length > phoneNumPosition + 8) {
                if (c12 == '2') {
                    numDashes2 = 1;
                    dashPositions[0] = phoneNumPosition + 2;
                } else {
                    numDashes2 = 1;
                    dashPositions[0] = phoneNumPosition + 3;
                }
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatTaiwanNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            char c3 = sb.charAt(index + 2);
            if (c1 == '9') {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                } else {
                    numDashes2 = numDashes;
                }
            } else if ((c1 == '8' && c2 == '2' && c3 == '6') || (c1 == '8' && c2 == '3' && c3 == '6')) {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                }
            } else if ((c1 == '3' && c2 == '7') || ((c1 == '4' && c2 == '9') || ((c1 == '8' && c2 == '9') || (c1 == '8' && c2 == '2')))) {
                numDashes2 = 1;
                dashPositions[0] = index + 2;
                if (length > index + 6 && length < index + 10) {
                    int numDashes3 = 1 + 1;
                    dashPositions[1] = index + 5;
                    numDashes2 = numDashes3;
                } else if (length >= index + 10) {
                    int numDashes4 = 1 + 1;
                    dashPositions[1] = index + 6;
                    numDashes2 = numDashes4;
                }
            } else {
                numDashes2 = 1;
                dashPositions[0] = index + 1;
                if (length > index + 6 && length < index + 9) {
                    int numDashes5 = 1 + 1;
                    dashPositions[1] = index + 4;
                    numDashes2 = numDashes5;
                } else if (length >= index + 9) {
                    int numDashes6 = 1 + 1;
                    dashPositions[1] = index + 5;
                    numDashes2 = numDashes6;
                }
            }
        } else if (length > phoneNumPosition + 4 && length < phoneNumPosition + 8) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 3;
        } else if (length >= phoneNumPosition + 8) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 4;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatMacauNumber(StringBuilder sb, int blankPosition) {
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (sb.charAt(phoneNumPosition) == '0' && sb.charAt(phoneNumPosition + 1) == '1') {
            sb.replace(phoneNumPosition + 2, phoneNumPosition + 2, " ");
            return formatHeightLengthWithoutRegionCodeNumber(sb, blankPosition + 3);
        }
        return formatHeightLengthWithoutRegionCodeNumber(sb, blankPosition);
    }

    private static String formatHeightLengthWithoutRegionCodeNumber(StringBuilder sb, int blankPosition) {
        int[] dashPositions = new int[2];
        int numDashes = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (sb.length() >= phoneNumPosition + 6) {
            numDashes = 1;
            dashPositions[0] = phoneNumPosition + 4;
        }
        for (int i = 0; i < numDashes; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return removeTrailingDashes(sb);
    }

    private static String formatVietnamNubmer(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            if (c1 == '4' || c1 == '8') {
                numDashes2 = 1;
                dashPositions[0] = index + 1;
            } else if ((c1 == '2' && (c2 == '1' || c2 == '3' || c2 == '4' || c2 == '8')) || ((c1 == '3' && (c2 == '2' || c2 == '5')) || ((c1 == '6' && c2 == '5') || (c1 == '7' && (c2 == '1' || c2 == '8'))))) {
                if (length > index + 4) {
                    numDashes2 = 1;
                    dashPositions[0] = index + 3;
                }
            } else if (c1 == '9') {
                numDashes2 = 1;
                dashPositions[0] = index + 2;
                if (length > index + 6) {
                    dashPositions[1] = index + 5;
                    numDashes2 = 1 + 1;
                }
            } else if (c1 == '1') {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                } else {
                    numDashes2 = numDashes;
                }
            } else {
                numDashes2 = 1;
                dashPositions[0] = index + 2;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatPortugalNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int numDashes2;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (length <= phoneNumPosition + 4) {
            numDashes = 0;
        } else {
            dashPositions[0] = phoneNumPosition + 2;
            numDashes = 1;
        }
        if (length > phoneNumPosition + 8) {
            numDashes2 = numDashes + 1;
            dashPositions[numDashes] = phoneNumPosition + 5;
        } else {
            numDashes2 = numDashes;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatBrazilNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[5];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) != '0') {
                numDashes = 0;
            } else {
                dashPositions[0] = phoneNumPosition + 1;
                index = phoneNumPosition + 1;
                numDashes = 1;
            }
            if (length > index + 3) {
                dashPositions[numDashes] = index + 2;
                numDashes++;
            }
            if (length > index + 7 && length <= index + 10) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = index + 6;
            } else if (length > index + 10) {
                int numDashes3 = numDashes + 1;
                dashPositions[numDashes] = index + 4;
                dashPositions[numDashes3] = index + 8;
                numDashes2 = numDashes3 + 1;
            } else {
                numDashes2 = numDashes;
            }
        } else if (length > phoneNumPosition + 5) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 4;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatPolandNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int numDashes2;
        int length = sb.length();
        int[] dashPositions = new int[3];
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (sb.charAt(phoneNumPosition) >= '5' && sb.charAt(phoneNumPosition) <= '8') {
            if (length <= phoneNumPosition + 4) {
                numDashes = 0;
            } else {
                dashPositions[0] = phoneNumPosition + 2;
                numDashes = 1;
            }
            if (length > phoneNumPosition + 6) {
                dashPositions[numDashes] = phoneNumPosition + 5;
                numDashes++;
            }
            if (length > phoneNumPosition + 8) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = phoneNumPosition + 7;
            }
        } else {
            if (length <= phoneNumPosition + 5) {
                numDashes = 0;
            } else {
                dashPositions[0] = phoneNumPosition + 3;
                numDashes = 1;
            }
            if (length > phoneNumPosition + 8) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = phoneNumPosition + 6;
            } else {
                numDashes2 = numDashes;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatAustraliaNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            if (sb.charAt(index) == '4') {
                if (length <= index + 5) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                }
                if (length > index + 8) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                } else {
                    numDashes2 = numDashes;
                }
            } else {
                if (length <= index + 4) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 1;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                }
            }
        } else {
            System.out.println(length);
            if (length == phoneNumPosition + 8) {
                numDashes2 = 1;
                dashPositions[0] = phoneNumPosition + 4;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatNewZealandNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            if (sb.charAt(index) == '2' && sb.charAt(index + 1) != '4') {
                if (length <= index + 4) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 2;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                }
            } else {
                if (length <= index + 3) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 1;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 4;
                } else {
                    numDashes2 = numDashes;
                }
            }
        } else {
            System.out.println(length);
            if (length == phoneNumPosition + 7) {
                numDashes2 = 1;
                dashPositions[0] = phoneNumPosition + 3;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatThailandNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            if (sb.charAt(index) == '8') {
                if (length <= index + 4) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 2;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                } else {
                    numDashes2 = numDashes;
                }
            } else if (sb.charAt(index) == '2') {
                if (length <= index + 3) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 1;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 4;
                }
            } else {
                if (length <= index + 4) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 2;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                }
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatIndonesiaNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            char c3 = sb.charAt(index + 2);
            if (c1 == '8') {
                if (length > index + 5) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length >= index + 8 && length <= index + 10) {
                    dashPositions[numDashes] = index + 6;
                    numDashes++;
                }
                if (length > index + 10) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 7;
                } else {
                    numDashes2 = numDashes;
                }
            } else if ((c1 == '2' && (c2 == '1' || c2 == '2' || c2 == '4')) || ((c1 == '3' && c2 == '1') || (c1 == '6' && c2 == '1' && c3 != '9'))) {
                if (length > index + 3) {
                    dashPositions[0] = index + 2;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                }
            } else {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                }
            }
        } else if (length == phoneNumPosition + 7) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 3;
        } else if (length == phoneNumPosition + 8) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 4;
        } else if (sb.charAt(phoneNumPosition) == '8') {
            if (length > phoneNumPosition + 8 && length <= phoneNumPosition + 10) {
                dashPositions[0] = phoneNumPosition + 3;
                dashPositions[1] = phoneNumPosition + 6;
                numDashes2 = 1 + 1;
            } else if (length > phoneNumPosition + 10) {
                dashPositions[0] = phoneNumPosition + 3;
                dashPositions[1] = phoneNumPosition + 7;
                numDashes2 = 1 + 1;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatMalaysiaNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            if ((c1 >= '3' && c1 <= '7') || c1 == '9') {
                if (length > index + 4) {
                    numDashes2 = 1;
                    dashPositions[0] = index + 1;
                }
            } else if (c1 == '8') {
                if (length > index + 4) {
                    numDashes2 = 1;
                    dashPositions[0] = index + 2;
                }
            } else if (c1 == '1') {
                if (length <= index + 4) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 2;
                    numDashes = 1;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                } else {
                    numDashes2 = numDashes;
                }
            } else if (c1 == '2') {
                if (length <= index + 4) {
                    numDashes = 0;
                } else {
                    dashPositions[0] = index + 1;
                    numDashes = 1;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                }
            }
        } else if (sb.charAt(phoneNumPosition) == '2' && length > phoneNumPosition + 8) {
            dashPositions[0] = phoneNumPosition + 1;
            dashPositions[1] = phoneNumPosition + 5;
            numDashes2 = 1 + 1;
        } else if (sb.charAt(phoneNumPosition) == '1' && length > phoneNumPosition + 8) {
            dashPositions[0] = phoneNumPosition + 2;
            dashPositions[1] = phoneNumPosition + 5;
            numDashes2 = 1 + 1;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatSpainNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int numDashes2;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (length <= phoneNumPosition + 5) {
            numDashes = 0;
        } else {
            dashPositions[0] = phoneNumPosition + 3;
            numDashes = 1;
        }
        if (length > phoneNumPosition + 7) {
            numDashes2 = numDashes + 1;
            dashPositions[numDashes] = phoneNumPosition + 6;
        } else {
            numDashes2 = numDashes;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static int checkIndiaNumber(char c1, char c2, char c3, char c4) {
        int result = -1;
        int temp = ((c3 - '0') * 10) + (c4 - '0');
        if (c1 == '9') {
            result = 0;
        } else if (c1 == '8') {
            if ((c2 == '0' && (temp < 20 || ((temp >= 50 && temp <= 60) || temp >= 80))) || ((c2 == '1' && (temp < 10 || ((temp >= 20 && temp <= 29) || (temp >= 40 && temp <= 49)))) || ((c2 == '7' && (temp >= 90 || temp == 69)) || ((c2 == '8' && (temp < 10 || temp == 17 || ((temp >= 25 && temp <= 28) || temp == 44 || temp == 53 || temp >= 90))) || (c3 == '9' && (temp < 10 || temp == 23 || temp == 39 || ((temp >= 50 && temp <= 62) || temp == 67 || temp == 68 || temp >= 70))))))) {
                result = 0;
            }
        } else if (c1 == '7' && (c2 == '0' || ((c2 == '2' && (temp == 0 || ((temp >= 4 && temp <= 9) || temp == 50 || temp == 59 || ((temp >= 75 && temp <= 78) || temp == 93 || temp == 9)))) || ((c2 == '3' && (temp == 73 || temp == 76 || temp == 77 || temp == 96 || temp == 98 || temp == 99)) || ((c2 == '4' && (temp < 10 || temp == 11 || ((temp >= 15 && temp <= 19) || temp == 28 || temp == 29 || temp == 39 || temp == 83 || temp == 88 || temp == 89 || temp == 98 || temp == 99))) || ((c2 == '5' && (temp <= 4 || temp == 49 || temp == 50 || ((temp >= 66 && temp <= 69) || temp == 79 || ((temp >= 87 && temp <= 89) || temp >= 97)))) || ((c2 == '6' && (temp == 0 || temp == 2 || temp == 7 || temp == 20 || temp == 31 || temp == 39 || temp == 54 || temp == 55 || ((temp >= 65 && temp <= 69) || ((temp >= 76 && temp <= 79) || temp >= 96)))) || ((c2 == '7' && (temp == 2 || temp == 8 || temp == 9 || ((temp >= 35 && temp <= 39) || temp == 42 || temp == 60 || temp == 77 || temp >= 95))) || ((c2 == '8' && temp <= 39 && (temp == 0 || ((temp >= 7 && temp <= 9) || temp == 14 || ((temp >= 27 && temp <= 30) || (temp >= 37 && temp <= 39))))) || (c2 == '8' && temp > 39 && (temp == 42 || temp == 45 || temp == 60 || ((temp >= 69 && temp <= 79) || temp >= 90)))))))))))) {
            result = 0;
        }
        if (result == 0) {
            return result;
        }
        if ((c1 == '1' && c2 == '1') || ((c1 == '2' && (c2 == '0' || c2 == '2')) || ((c1 == '3' && c2 == '3') || ((c1 == '4' && (c2 == '0' || c2 == '4')) || ((c1 == '7' && c2 == '9') || (c1 == '8' && c2 == '0')))))) {
            return 2;
        }
        int key = ((c1 - '0') * 100) + ((c2 - '0') * 10) + (c3 - '0');
        if (Arrays.binarySearch(INDIA_THREE_DIGIG_AREA_CODES, key) >= 0) {
            return 3;
        }
        return 4;
    }

    private static String formatIndiaNumber(StringBuilder sb, int blankPosition) {
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        char c = sb.charAt(phoneNumPosition);
        if ((phoneNumPosition > 0 && c != '0') || (c == '0' && length > phoneNumPosition + 4)) {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            char c3 = sb.charAt(index + 2);
            char c4 = sb.charAt(index + 3);
            int type = checkIndiaNumber(c1, c2, c3, c4);
            if (type == 0) {
                numDashes = 1;
                dashPositions[0] = index + 2;
                if (length > index + 7) {
                    dashPositions[1] = index + 4;
                    numDashes = 1 + 1;
                }
            } else if (type == 2) {
                numDashes = 1;
                dashPositions[0] = index + 2;
            } else if (type == 3) {
                numDashes = 1;
                dashPositions[0] = index + 3;
            } else if (length > index + 5) {
                numDashes = 1;
                dashPositions[0] = index + 4;
            }
        } else if (length > phoneNumPosition + 8) {
            dashPositions[0] = phoneNumPosition + 2;
            dashPositions[1] = phoneNumPosition + 4;
            numDashes = 1 + 1;
        }
        for (int i = 0; i < numDashes; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatRussianNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[3];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0) {
            if (length <= phoneNumPosition + 5) {
                numDashes = 0;
            } else {
                dashPositions[0] = phoneNumPosition + 3;
                numDashes = 1;
            }
            if (length > phoneNumPosition + 7) {
                dashPositions[numDashes] = phoneNumPosition + 6;
                numDashes++;
            }
            if (length > phoneNumPosition + 9) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = phoneNumPosition + 8;
            } else {
                numDashes2 = numDashes;
            }
        } else if (length == phoneNumPosition + 6) {
            dashPositions[0] = phoneNumPosition + 2;
            int numDashes3 = 1 + 1;
            dashPositions[1] = phoneNumPosition + 4;
            numDashes2 = numDashes3;
        } else if (length == phoneNumPosition + 7) {
            dashPositions[0] = phoneNumPosition + 3;
            int numDashes4 = 1 + 1;
            dashPositions[1] = phoneNumPosition + 5;
            numDashes2 = numDashes4;
        } else if (length >= phoneNumPosition + 8) {
            dashPositions[0] = phoneNumPosition + 3;
            numDashes = 1 + 1;
            dashPositions[1] = phoneNumPosition + 6;
            if (length > phoneNumPosition + 9) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = phoneNumPosition + 8;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatGermanyNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            if (c1 == '1') {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (c2 != '5' && c2 != '6' && c2 != '7') {
                    numDashes2 = numDashes;
                } else if (length > index + 10) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 9;
                } else {
                    numDashes2 = numDashes;
                }
            } else if ((c1 == '3' && c2 == '0') || ((c1 == '4' && c2 == '0') || ((c1 == '6' && c2 == '9') || (c1 == '8' && c2 == '9')))) {
                if (length > index + 4) {
                    dashPositions[0] = index + 2;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 6) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 5;
                }
            } else if (length > index + 3) {
                char c3 = sb.charAt(index + 2);
                char c4 = sb.charAt(index + 3);
                int key3 = ((c1 - '0') * 100) + ((c2 - '0') * 10) + (c3 - '0');
                int key4 = (key3 * 10) + (c4 - '0');
                if (c3 == '1' || (Arrays.binarySearch(Germany_THREE_PART_REGION_CODES, key3) >= 0 && (key3 != 212 || (key3 == 212 && c4 != '9')))) {
                    if (length > index + 4) {
                        dashPositions[0] = index + 3;
                        numDashes = 1;
                    } else {
                        numDashes = 0;
                    }
                    if (length > index + 7) {
                        numDashes2 = numDashes + 1;
                        dashPositions[numDashes] = index + 6;
                    }
                } else if (c1 != '3' || (c1 == '3' && Arrays.binarySearch(Germany_FOUR_PART_REGION_CODES, key4) >= 0)) {
                    if (length > index + 5) {
                        dashPositions[0] = index + 4;
                        numDashes = 1;
                    } else {
                        numDashes = 0;
                    }
                    if (length > index + 8) {
                        numDashes2 = numDashes + 1;
                        dashPositions[numDashes] = index + 7;
                    }
                } else {
                    if (length > index + 6) {
                        dashPositions[0] = index + 5;
                        numDashes = 1;
                    } else {
                        numDashes = 0;
                    }
                    if (length > index + 9) {
                        numDashes2 = numDashes + 1;
                        dashPositions[numDashes] = index + 8;
                    }
                }
            }
        } else if (length >= phoneNumPosition + 6 && length <= phoneNumPosition + 8) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 3;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatItalyNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            char c3 = sb.charAt(index + 2);
            int key = ((c1 - '0') * 100) + ((c2 - '0') * 10) + (c3 - '0');
            if (Arrays.binarySearch(ITALY_MOBILE_PREFIXS, key) >= 0) {
                if (length > index + 5) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 8) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                } else {
                    numDashes2 = numDashes;
                }
            } else if (c1 == '2' || c1 == '6') {
                numDashes2 = 1;
                dashPositions[0] = index + 1;
            } else if (c2 == '0' || c2 == '1' || c2 == '5' || c2 == '9') {
                if (length > index + 4) {
                    numDashes2 = 1;
                    dashPositions[0] = index + 2;
                }
            } else if (length > index + 5) {
                numDashes2 = 1;
                dashPositions[0] = index + 3;
            }
        } else {
            char c12 = sb.charAt(phoneNumPosition);
            char c22 = sb.charAt(phoneNumPosition + 1);
            char c32 = sb.charAt(phoneNumPosition + 2);
            int key2 = ((c12 - '0') * 100) + ((c22 - '0') * 10) + (c32 - '0');
            if (Arrays.binarySearch(ITALY_MOBILE_PREFIXS, key2) >= 0) {
                if (length > phoneNumPosition + 5) {
                    dashPositions[0] = phoneNumPosition + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > phoneNumPosition + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = phoneNumPosition + 6;
                }
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatFranceNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[4];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        int c = sb.charAt(phoneNumPosition);
        if (phoneNumPosition > 0 || c == 48 || c == 52 || c == 53 || c == 55 || c == 57) {
            int index = phoneNumPosition;
            if ((phoneNumPosition == 0 && (c == 48 || c == 52 || c == 53 || c == 55 || c == 57)) || (phoneNumPosition > 0 && c == 48)) {
                index = phoneNumPosition + 1;
            }
            dashPositions[0] = index + 1;
            if (length <= index + 4) {
                numDashes = 1;
            } else {
                numDashes = 1 + 1;
                dashPositions[1] = index + 3;
            }
            if (length > index + 6) {
                dashPositions[numDashes] = index + 5;
                numDashes++;
            }
            if (length > index + 8) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = index + 7;
            } else {
                numDashes2 = numDashes;
            }
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatEnglandNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            char c1 = sb.charAt(index);
            char c2 = sb.charAt(index + 1);
            char c3 = sb.charAt(index + 2);
            if (c1 == '7') {
                if (length > index + 5) {
                    numDashes2 = 1;
                    dashPositions[0] = index + 4;
                }
            } else if (c1 == '2') {
                numDashes2 = 1;
                dashPositions[0] = index + 2;
                if (length > index + 7) {
                    dashPositions[1] = index + 6;
                    numDashes2 = 1 + 1;
                }
            } else if (c1 == '1') {
                char c4 = sb.charAt(index + 2);
                int key = ((c1 - '0') * 1000) + ((c2 - '0') * 100) + ((c3 - '0') * 10) + c4;
                if (c2 == '1' || c3 == '1') {
                    if (length > index + 4) {
                        dashPositions[0] = index + 3;
                        numDashes = 1;
                    } else {
                        numDashes = 0;
                    }
                    if (length > index + 7) {
                        numDashes2 = numDashes + 1;
                        dashPositions[numDashes] = index + 6;
                    } else {
                        numDashes2 = numDashes;
                    }
                } else if (key == 1387 || key == 1539 || key == 1697 || key == 1768 || key == 1946) {
                    if (length > index + 6) {
                        numDashes2 = 1;
                        dashPositions[0] = index + 5;
                    }
                } else if (length > index + 5) {
                    numDashes2 = 1;
                    dashPositions[0] = index + 4;
                }
            } else if (c1 == '3' || c1 == '8' || c1 == '9') {
                if (length > index + 4) {
                    dashPositions[0] = index + 3;
                    numDashes = 1;
                } else {
                    numDashes = 0;
                }
                if (length > index + 7) {
                    numDashes2 = numDashes + 1;
                    dashPositions[numDashes] = index + 6;
                }
            } else {
                numDashes2 = 1;
                dashPositions[0] = index + 2;
                if (length > index + 7) {
                    dashPositions[1] = index + 6;
                    numDashes2 = 1 + 1;
                }
            }
        } else if (length > phoneNumPosition + 4 && length < phoneNumPosition + 8) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 3;
        } else if (length >= phoneNumPosition + 8) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 4;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    private static String formatTurkeyNumber(StringBuilder sb, int blankPosition) {
        int numDashes;
        int length = sb.length();
        int[] dashPositions = new int[2];
        int numDashes2 = 0;
        int phoneNumPosition = blankPosition == -1 ? 0 : blankPosition + 1;
        if (phoneNumPosition > 0 || sb.charAt(phoneNumPosition) == '0') {
            int index = phoneNumPosition;
            if (sb.charAt(phoneNumPosition) == '0') {
                index = phoneNumPosition + 1;
            }
            if (length <= index + 4) {
                numDashes = 0;
            } else {
                dashPositions[0] = index + 3;
                numDashes = 1;
            }
            if (length > index + 7) {
                numDashes2 = numDashes + 1;
                dashPositions[numDashes] = index + 6;
            } else {
                numDashes2 = numDashes;
            }
        } else if (length > phoneNumPosition + 4) {
            numDashes2 = 1;
            dashPositions[0] = phoneNumPosition + 3;
        }
        for (int i = 0; i < numDashes2; i++) {
            int pos = dashPositions[i];
            sb.replace(pos + i, pos + i, "-");
        }
        return sb.toString();
    }

    public static void log(String info) {
    }
}
