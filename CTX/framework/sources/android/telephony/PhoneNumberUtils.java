package android.telephony;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.CountryDetector;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.SettingsStringUtil;
import android.telecom.PhoneAccount;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.SparseIntArray;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.i18n.phonenumbers.ShortNumberInfo;
import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.midi.MidiConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberUtils {
    private static final String BCD_CALLED_PARTY_EXTENDED = "*#abc";
    private static final String BCD_EF_ADN_EXTENDED = "*#,N;";
    public static final int BCD_EXTENDED_TYPE_CALLED_PARTY = 2;
    public static final int BCD_EXTENDED_TYPE_EF_ADN = 1;
    private static final int CCC_LENGTH;
    private static final String CLIR_OFF = "#31#";
    private static final String CLIR_ON = "*31#";
    private static final boolean[] COUNTRY_CALLING_CALL;
    private static final boolean DBG = false;
    public static final int FORMAT_JAPAN = 2;
    public static final int FORMAT_NANP = 1;
    public static final int FORMAT_UNKNOWN = 0;
    private static final String JAPAN_ISO_COUNTRY_CODE = "JP";
    private static final String KOREA_ISO_COUNTRY_CODE = "KR";
    static final String LOG_TAG = "PhoneNumberUtils";
    private static final String NANP_IDP_STRING = "011";
    private static final int NANP_LENGTH = 10;
    private static final int NANP_STATE_DASH = 4;
    private static final int NANP_STATE_DIGIT = 1;
    private static final int NANP_STATE_ONE = 3;
    private static final int NANP_STATE_PLUS = 2;
    public static final char PAUSE = ',';
    private static final char PLUS_SIGN_CHAR = '+';
    private static final String PLUS_SIGN_STRING = "+";
    public static final int TOA_International = 145;
    public static final int TOA_Unknown = 129;
    public static final char WAIT = ';';
    public static final char WILD = 'N';
    private static String[] sConvertToEmergencyMap;
    private static Class<?> sMtkPhoneNumberUtils = getMtkPhoneNumberUtils();
    private static final Pattern GLOBAL_PHONE_NUMBER_PATTERN = Pattern.compile("[\\+]?[0-9.-]+");
    private static final String[] NANP_COUNTRIES = {"US", "CA", "AS", "AI", "AG", "BS", "BB", "BM", "VG", "KY", "DM", "DO", "GD", "GU", "JM", "PR", "MS", "MP", "KN", "LC", "VC", "TT", "TC", "VI"};
    static int MIN_MATCH = getMinMatch();
    private static final SparseIntArray KEYPAD_MAP = new SparseIntArray();

    @Retention(RetentionPolicy.SOURCE)
    public @interface BcdExtendType {
    }

    static {
        KEYPAD_MAP.put(97, 50);
        KEYPAD_MAP.put(98, 50);
        KEYPAD_MAP.put(99, 50);
        KEYPAD_MAP.put(65, 50);
        KEYPAD_MAP.put(66, 50);
        KEYPAD_MAP.put(67, 50);
        KEYPAD_MAP.put(100, 51);
        KEYPAD_MAP.put(101, 51);
        KEYPAD_MAP.put(102, 51);
        KEYPAD_MAP.put(68, 51);
        KEYPAD_MAP.put(69, 51);
        KEYPAD_MAP.put(70, 51);
        KEYPAD_MAP.put(103, 52);
        KEYPAD_MAP.put(104, 52);
        KEYPAD_MAP.put(105, 52);
        KEYPAD_MAP.put(71, 52);
        KEYPAD_MAP.put(72, 52);
        KEYPAD_MAP.put(73, 52);
        KEYPAD_MAP.put(106, 53);
        KEYPAD_MAP.put(107, 53);
        KEYPAD_MAP.put(108, 53);
        KEYPAD_MAP.put(74, 53);
        KEYPAD_MAP.put(75, 53);
        KEYPAD_MAP.put(76, 53);
        KEYPAD_MAP.put(109, 54);
        KEYPAD_MAP.put(110, 54);
        KEYPAD_MAP.put(111, 54);
        KEYPAD_MAP.put(77, 54);
        KEYPAD_MAP.put(78, 54);
        KEYPAD_MAP.put(79, 54);
        KEYPAD_MAP.put(112, 55);
        KEYPAD_MAP.put(113, 55);
        KEYPAD_MAP.put(114, 55);
        KEYPAD_MAP.put(115, 55);
        KEYPAD_MAP.put(80, 55);
        KEYPAD_MAP.put(81, 55);
        KEYPAD_MAP.put(82, 55);
        KEYPAD_MAP.put(83, 55);
        KEYPAD_MAP.put(116, 56);
        KEYPAD_MAP.put(117, 56);
        KEYPAD_MAP.put(118, 56);
        KEYPAD_MAP.put(84, 56);
        KEYPAD_MAP.put(85, 56);
        KEYPAD_MAP.put(86, 56);
        KEYPAD_MAP.put(119, 57);
        KEYPAD_MAP.put(120, 57);
        KEYPAD_MAP.put(121, 57);
        KEYPAD_MAP.put(122, 57);
        KEYPAD_MAP.put(87, 57);
        KEYPAD_MAP.put(88, 57);
        KEYPAD_MAP.put(89, 57);
        KEYPAD_MAP.put(90, 57);
        COUNTRY_CALLING_CALL = new boolean[]{true, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, true, false, true, true, true, true, true, false, true, false, false, true, true, false, false, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, false, true, false, false, true, true, true, true, true, true, true, false, false, true, false};
        CCC_LENGTH = COUNTRY_CALLING_CALL.length;
        sConvertToEmergencyMap = null;
    }

    public static boolean isISODigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static final boolean is12Key(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#';
    }

    public static final boolean isDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == 'N';
    }

    public static final boolean isReallyDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
    }

    public static final boolean isNonSeparator(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == 'N' || c == ';' || c == ',';
    }

    public static final boolean isStartsPostDial(char c) {
        return c == ',' || c == ';';
    }

    private static boolean isPause(char c) {
        return c == 'p' || c == 'P';
    }

    private static boolean isToneWait(char c) {
        return c == 'w' || c == 'W';
    }

    private static boolean isSeparator(char c) {
        return !isDialable(c) && ('a' > c || c > 'z') && ('A' > c || c > 'Z');
    }

    public static String getNumberFromIntent(Intent intent, Context context) throws Throwable {
        Cursor cursorQuery;
        Uri data = intent.getData();
        String string = null;
        if (data == null) {
            return null;
        }
        String scheme = data.getScheme();
        if (scheme.equals(PhoneAccount.SCHEME_TEL) || scheme.equals("sip")) {
            return data.getSchemeSpecificPart();
        }
        if (context == null) {
            return null;
        }
        intent.resolveType(context);
        String authority = data.getAuthority();
        String str = Contacts.AUTHORITY.equals(authority) ? "number" : ContactsContract.AUTHORITY.equals(authority) ? "data1" : null;
        try {
            cursorQuery = context.getContentResolver().query(data, new String[]{str}, null, null, null);
            if (cursorQuery != null) {
                try {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            string = cursorQuery.getString(cursorQuery.getColumnIndex(str));
                        }
                    } catch (RuntimeException e) {
                        e = e;
                        Rlog.e(LOG_TAG, "Error getting phone number.", e);
                        if (cursorQuery != null) {
                        }
                        return string;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
        } catch (RuntimeException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
            if (cursorQuery != null) {
            }
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return string;
    }

    public static String extractNetworkPortion(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            int iDigit = Character.digit(cCharAt, 10);
            if (iDigit != -1) {
                sb.append(iDigit);
            } else if (cCharAt != '+') {
                if (isDialable(cCharAt)) {
                    sb.append(cCharAt);
                } else if (isStartsPostDial(cCharAt)) {
                    break;
                }
            } else {
                String string = sb.toString();
                if (string.length() == 0 || string.equals(CLIR_ON) || string.equals(CLIR_OFF)) {
                    sb.append(cCharAt);
                }
            }
        }
        return sb.toString();
    }

    public static String extractNetworkPortionAlt(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        boolean z = false;
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '+') {
                if (z) {
                    continue;
                } else {
                    z = true;
                }
            }
            if (isDialable(cCharAt)) {
                sb.append(cCharAt);
            } else if (isStartsPostDial(cCharAt)) {
                break;
            }
        }
        return sb.toString();
    }

    public static String stripSeparators(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            int iDigit = Character.digit(cCharAt, 10);
            if (iDigit != -1) {
                sb.append(iDigit);
            } else if (isNonSeparator(cCharAt)) {
                sb.append(cCharAt);
            }
        }
        return sb.toString();
    }

    public static String convertAndStrip(String str) {
        return stripSeparators(convertKeypadLettersToDigits(str));
    }

    public static String convertPreDial(String str) {
        if (str == null) {
            return null;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (isPause(cCharAt)) {
                cCharAt = ',';
            } else if (isToneWait(cCharAt)) {
                cCharAt = ';';
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }

    private static int minPositive(int i, int i2) {
        if (i >= 0 && i2 >= 0) {
            return i < i2 ? i : i2;
        }
        if (i >= 0) {
            return i;
        }
        if (i2 >= 0) {
            return i2;
        }
        return -1;
    }

    private static void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private static int indexOfLastNetworkChar(String str) {
        int length = str.length();
        int iMinPositive = minPositive(str.indexOf(44), str.indexOf(59));
        if (iMinPositive < 0) {
            return length - 1;
        }
        return iMinPositive - 1;
    }

    public static String extractPostDialPortion(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int iIndexOfLastNetworkChar = indexOfLastNetworkChar(str) + 1; iIndexOfLastNetworkChar < length; iIndexOfLastNetworkChar++) {
            char cCharAt = str.charAt(iIndexOfLastNetworkChar);
            if (isNonSeparator(cCharAt)) {
                sb.append(cCharAt);
            }
        }
        return sb.toString();
    }

    public static boolean compare(String str, String str2) {
        return compare(str, str2, false);
    }

    public static boolean compare(Context context, String str, String str2) {
        return compare(str, str2, context.getResources().getBoolean(R.bool.config_use_strict_phone_number_comparation));
    }

    public static boolean compare(String str, String str2, boolean z) {
        return z ? compareStrictly(str, str2) : compareLoosely(str, str2);
    }

    public static boolean compareLoosely(String str, String str2) {
        int i;
        int i2;
        boolean z;
        if (str == null || str2 == null) {
            return str == str2;
        }
        if (str.length() == 0 || str2.length() == 0) {
            return false;
        }
        int iIndexOfLastNetworkChar = indexOfLastNetworkChar(str);
        int iIndexOfLastNetworkChar2 = indexOfLastNetworkChar(str2);
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        while (true) {
            if (iIndexOfLastNetworkChar < 0 || iIndexOfLastNetworkChar2 < 0) {
                break;
            }
            char cCharAt = str.charAt(iIndexOfLastNetworkChar);
            if (isDialable(cCharAt)) {
                i = i3;
                i2 = iIndexOfLastNetworkChar;
                z = false;
            } else {
                i = i3 + 1;
                i2 = iIndexOfLastNetworkChar - 1;
                z = true;
            }
            char cCharAt2 = str2.charAt(iIndexOfLastNetworkChar2);
            if (!isDialable(cCharAt2)) {
                iIndexOfLastNetworkChar2--;
                i4++;
                z = true;
            }
            if (!z) {
                if (cCharAt2 == cCharAt || cCharAt == 'N' || cCharAt2 == 'N') {
                    i2--;
                    iIndexOfLastNetworkChar2--;
                    i5++;
                } else {
                    iIndexOfLastNetworkChar = i2;
                    i3 = i;
                    break;
                }
            }
            iIndexOfLastNetworkChar = i2;
            i3 = i;
        }
        if (i5 < MIN_MATCH) {
            int length = str.length() - i3;
            return length == str2.length() - i4 && length == i5;
        }
        if (i5 >= MIN_MATCH && (iIndexOfLastNetworkChar < 0 || iIndexOfLastNetworkChar2 < 0)) {
            return true;
        }
        int i6 = iIndexOfLastNetworkChar + 1;
        if (matchIntlPrefix(str, i6) && matchIntlPrefix(str2, iIndexOfLastNetworkChar2 + 1)) {
            return true;
        }
        if (matchTrunkPrefix(str, i6) && matchIntlPrefixAndCC(str2, iIndexOfLastNetworkChar2 + 1)) {
            return true;
        }
        return matchTrunkPrefix(str2, iIndexOfLastNetworkChar2 + 1) && matchIntlPrefixAndCC(str, i6);
    }

    public static boolean compareStrictly(String str, String str2) {
        return compareStrictly(str, str2, true);
    }

    public static boolean compareStrictly(java.lang.String r17, java.lang.String r18, boolean r19) {
        throw new UnsupportedOperationException("Method not decompiled: android.telephony.PhoneNumberUtils.compareStrictly(java.lang.String, java.lang.String, boolean):boolean");
    }

    public static String toCallerIDMinMatch(String str) {
        return internalGetStrippedReversed(extractNetworkPortionAlt(str), MIN_MATCH);
    }

    public static String getStrippedReversed(String str) {
        String strExtractNetworkPortionAlt = extractNetworkPortionAlt(str);
        if (strExtractNetworkPortionAlt == null) {
            return null;
        }
        return internalGetStrippedReversed(strExtractNetworkPortionAlt, strExtractNetworkPortionAlt.length());
    }

    private static String internalGetStrippedReversed(String str, int i) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(i);
        int length = str.length();
        for (int i2 = length - 1; i2 >= 0 && length - i2 <= i; i2--) {
            sb.append(str.charAt(i2));
        }
        return sb.toString();
    }

    public static String stringFromStringAndTOA(String str, int i) {
        if (str == null) {
            return null;
        }
        if (i == 145 && str.length() > 0 && str.charAt(0) != '+') {
            return PLUS_SIGN_STRING + str;
        }
        return str;
    }

    public static int toaFromString(String str) {
        if (str != null && str.length() > 0 && str.charAt(0) == '+') {
            return 145;
        }
        return 129;
    }

    @Deprecated
    public static String calledPartyBCDToString(byte[] bArr, int i, int i2) {
        return calledPartyBCDToString(bArr, i, i2, 1);
    }

    public static String calledPartyBCDToString(byte[] bArr, int i, int i2, int i3) {
        StringBuilder sb;
        StringBuilder sb2 = new StringBuilder((i2 * 2) + 1);
        if (i2 < 2) {
            return "";
        }
        boolean z = (bArr[i] & 240) == 144;
        internalCalledPartyBCDFragmentToString(sb2, bArr, i + 1, i2 - 1, i3);
        if (z && sb2.length() == 0) {
            return "";
        }
        if (z) {
            String string = sb2.toString();
            Matcher matcher = Pattern.compile("(^[#*])(.*)([#*])(.*)(#)$").matcher(string);
            if (matcher.matches()) {
                if ("".equals(matcher.group(2))) {
                    sb = new StringBuilder();
                    sb.append(matcher.group(1));
                    sb.append(matcher.group(3));
                    sb.append(matcher.group(4));
                    sb.append(matcher.group(5));
                    sb.append(PLUS_SIGN_STRING);
                } else {
                    sb = new StringBuilder();
                    sb.append(matcher.group(1));
                    sb.append(matcher.group(2));
                    sb.append(matcher.group(3));
                    sb.append(PLUS_SIGN_STRING);
                    sb.append(matcher.group(4));
                    sb.append(matcher.group(5));
                }
            } else {
                Matcher matcher2 = Pattern.compile("(^[#*])(.*)([#*])(.*)").matcher(string);
                if (matcher2.matches()) {
                    sb = new StringBuilder();
                    sb.append(matcher2.group(1));
                    sb.append(matcher2.group(2));
                    sb.append(matcher2.group(3));
                    sb.append(PLUS_SIGN_STRING);
                    sb.append(matcher2.group(4));
                } else {
                    sb2 = new StringBuilder();
                    sb2.append(PLUS_SIGN_CHAR);
                    sb2.append(string);
                    sb = sb2;
                }
            }
        } else {
            sb = sb2;
        }
        return sb.toString();
    }

    private static void internalCalledPartyBCDFragmentToString(StringBuilder sb, byte[] bArr, int i, int i2, int i3) {
        char cBcdToChar;
        char cBcdToChar2;
        int i4 = i;
        while (true) {
            int i5 = i2 + i;
            if (i4 >= i5 || (cBcdToChar = bcdToChar((byte) (bArr[i4] & MidiConstants.STATUS_CHANNEL_MASK), i3)) == 0) {
                return;
            }
            sb.append(cBcdToChar);
            byte b = (byte) ((bArr[i4] >> 4) & 15);
            if ((b == 15 && i4 + 1 == i5) || (cBcdToChar2 = bcdToChar(b, i3)) == 0) {
                return;
            }
            sb.append(cBcdToChar2);
            i4++;
        }
    }

    @Deprecated
    public static String calledPartyBCDFragmentToString(byte[] bArr, int i, int i2) {
        return calledPartyBCDFragmentToString(bArr, i, i2, 1);
    }

    public static String calledPartyBCDFragmentToString(byte[] bArr, int i, int i2, int i3) {
        StringBuilder sb = new StringBuilder(i2 * 2);
        internalCalledPartyBCDFragmentToString(sb, bArr, i, i2, i3);
        return sb.toString();
    }

    private static char bcdToChar(byte b, int i) {
        int i2;
        if (b < 10) {
            return (char) (48 + b);
        }
        String str = null;
        if (1 == i) {
            str = BCD_EF_ADN_EXTENDED;
        } else if (2 == i) {
            str = BCD_CALLED_PARTY_EXTENDED;
        }
        if (str == null || (i2 = b - 10) >= str.length()) {
            return (char) 0;
        }
        return str.charAt(i2);
    }

    private static int charToBCD(char c, int i) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        String str = null;
        if (1 == i) {
            str = BCD_EF_ADN_EXTENDED;
        } else if (2 == i) {
            str = BCD_CALLED_PARTY_EXTENDED;
        }
        if (str == null || str.indexOf(c) == -1) {
            throw new RuntimeException("invalid char for BCD " + c);
        }
        return 10 + str.indexOf(c);
    }

    public static boolean isWellFormedSmsAddress(String str) {
        String strExtractNetworkPortion = extractNetworkPortion(str);
        return (strExtractNetworkPortion.equals(PLUS_SIGN_STRING) || TextUtils.isEmpty(strExtractNetworkPortion) || !isDialable(strExtractNetworkPortion)) ? false : true;
    }

    public static boolean isGlobalPhoneNumber(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return GLOBAL_PHONE_NUMBER_PATTERN.matcher(str).matches();
    }

    private static boolean isDialable(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isDialable(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNonSeparator(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isNonSeparator(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static byte[] networkPortionToCalledPartyBCD(String str) {
        return numberToCalledPartyBCDHelper(extractNetworkPortion(str), false, 1);
    }

    public static byte[] networkPortionToCalledPartyBCDWithLength(String str) {
        return numberToCalledPartyBCDHelper(extractNetworkPortion(str), true, 1);
    }

    @Deprecated
    public static byte[] numberToCalledPartyBCD(String str) {
        return numberToCalledPartyBCD(str, 1);
    }

    public static byte[] numberToCalledPartyBCD(String str, int i) {
        return numberToCalledPartyBCDHelper(str, false, i);
    }

    private static byte[] numberToCalledPartyBCDHelper(String str, boolean z, int i) {
        int length = str.length();
        char c = PLUS_SIGN_CHAR;
        char c2 = 0;
        boolean z2 = str.indexOf(43) != -1;
        int i2 = z2 ? length - 1 : length;
        if (i2 == 0) {
            return null;
        }
        int i3 = (i2 + 1) / 2;
        int i4 = z ? 2 : 1;
        int i5 = i3 + i4;
        byte[] bArr = new byte[i5];
        int i6 = 0;
        int i7 = 0;
        while (i6 < length) {
            char cCharAt = str.charAt(i6);
            if (cCharAt != c) {
                int i8 = (i7 >> 1) + i4;
                bArr[i8] = (byte) (((byte) ((charToBCD(cCharAt, i) & 15) << ((i7 & 1) == 1 ? 4 : 0))) | bArr[i8]);
                i7++;
            }
            i6++;
            c = PLUS_SIGN_CHAR;
        }
        if ((i7 & 1) == 1) {
            int i9 = i4 + (i7 >> 1);
            bArr[i9] = (byte) (bArr[i9] | 240);
        }
        if (z) {
            bArr[0] = (byte) (i5 - 1);
            c2 = 1;
        }
        bArr[c2] = (byte) (z2 ? 145 : 129);
        return bArr;
    }

    @Deprecated
    public static String formatNumber(String str) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        formatNumber(spannableStringBuilder, getFormatTypeForLocale(Locale.getDefault()));
        return spannableStringBuilder.toString();
    }

    @Deprecated
    public static String formatNumber(String str, int i) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
        formatNumber(spannableStringBuilder, i);
        return spannableStringBuilder.toString();
    }

    @Deprecated
    public static int getFormatTypeForLocale(Locale locale) {
        return getFormatTypeFromCountryCode(locale.getCountry());
    }

    @Deprecated
    public static void formatNumber(Editable editable, int i) {
        if (editable.length() > 2 && editable.charAt(0) == '+') {
            i = editable.charAt(1) == '1' ? 1 : (editable.length() >= 3 && editable.charAt(1) == '8' && editable.charAt(2) == '1') ? 2 : 0;
        }
        switch (i) {
            case 0:
                removeDashes(editable);
                break;
            case 1:
                formatNanpNumber(editable);
                break;
            case 2:
                formatJapaneseNumber(editable);
                break;
        }
    }

    @Deprecated
    public static void formatNanpNumber(Editable editable) {
        int i;
        int length = editable.length();
        if (length <= "+1-nnn-nnn-nnnn".length() && length > 5) {
            CharSequence charSequenceSubSequence = editable.subSequence(0, length);
            removeDashes(editable);
            int length2 = editable.length();
            int[] iArr = new int[3];
            int i2 = 0;
            int i3 = 0;
            char c = 1;
            for (int i4 = 0; i4 < length2; i4++) {
                char cCharAt = editable.charAt(i4);
                if (cCharAt != '+') {
                    if (cCharAt != '-') {
                        switch (cCharAt) {
                            case '1':
                                if (i2 == 0 || c == 2) {
                                    c = 3;
                                    break;
                                }
                            case '0':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                if (c == 2) {
                                    editable.replace(0, length2, charSequenceSubSequence);
                                } else {
                                    if (c == 3) {
                                        i = i3 + 1;
                                        iArr[i3] = i4;
                                    } else if (c == 4 || !(i2 == 3 || i2 == 6)) {
                                        i = i3;
                                    } else {
                                        i = i3 + 1;
                                        iArr[i3] = i4;
                                    }
                                    i2++;
                                    i3 = i;
                                    c = 1;
                                }
                                break;
                            default:
                                editable.replace(0, length2, charSequenceSubSequence);
                                break;
                        }
                        return;
                    }
                    c = 4;
                } else {
                    if (i4 != 0) {
                        editable.replace(0, length2, charSequenceSubSequence);
                        return;
                    }
                    c = 2;
                }
            }
            if (i2 == 7) {
                i3--;
            }
            for (int i5 = 0; i5 < i3; i5++) {
                int i6 = iArr[i5] + i5;
                editable.replace(i6, i6, NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            }
            for (int length3 = editable.length(); length3 > 0; length3--) {
                int i7 = length3 - 1;
                if (editable.charAt(i7) != '-') {
                    return;
                }
                editable.delete(i7, length3);
            }
        }
    }

    @Deprecated
    public static void formatJapaneseNumber(Editable editable) {
        JapanesePhoneNumberFormatter.format(editable);
    }

    private static void removeDashes(Editable editable) {
        int i = 0;
        while (i < editable.length()) {
            if (editable.charAt(i) == '-') {
                editable.delete(i, i + 1);
            } else {
                i++;
            }
        }
    }

    public static String formatNumberToE164(String str, String str2) {
        return formatNumberInternal(str, str2, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public static String formatNumberToRFC3966(String str, String str2) {
        return formatNumberInternal(str, str2, PhoneNumberUtil.PhoneNumberFormat.RFC3966);
    }

    private static String formatNumberInternal(String str, String str2, PhoneNumberUtil.PhoneNumberFormat phoneNumberFormat) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(str, str2);
            if (phoneNumberUtil.isValidNumber(phoneNumber)) {
                return phoneNumberUtil.format(phoneNumber, phoneNumberFormat);
            }
            return null;
        } catch (NumberParseException e) {
            return null;
        }
    }

    public static boolean isInternationalNumber(String str, String str2) {
        if (TextUtils.isEmpty(str) || str.startsWith("#") || str.startsWith(PhoneConstants.APN_TYPE_ALL)) {
            return false;
        }
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            return phoneNumberUtil.parseAndKeepRawInput(str, str2).getCountryCode() != phoneNumberUtil.getCountryCodeForRegion(str2);
        } catch (NumberParseException e) {
            return false;
        }
    }

    public static String formatNumber(String str, String str2) {
        String inOriginalFormat;
        if (str.startsWith("#") || str.startsWith(PhoneConstants.APN_TYPE_ALL)) {
            return str;
        }
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber andKeepRawInput = phoneNumberUtil.parseAndKeepRawInput(str, str2);
            if (KOREA_ISO_COUNTRY_CODE.equalsIgnoreCase(str2) && andKeepRawInput.getCountryCode() == phoneNumberUtil.getCountryCodeForRegion(KOREA_ISO_COUNTRY_CODE) && andKeepRawInput.getCountryCodeSource() == Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN) {
                inOriginalFormat = phoneNumberUtil.format(andKeepRawInput, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            } else if (JAPAN_ISO_COUNTRY_CODE.equalsIgnoreCase(str2) && andKeepRawInput.getCountryCode() == phoneNumberUtil.getCountryCodeForRegion(JAPAN_ISO_COUNTRY_CODE) && andKeepRawInput.getCountryCodeSource() == Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN) {
                inOriginalFormat = phoneNumberUtil.format(andKeepRawInput, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            } else {
                inOriginalFormat = phoneNumberUtil.formatInOriginalFormat(andKeepRawInput, str2);
            }
            return inOriginalFormat;
        } catch (NumberParseException e) {
            return null;
        }
    }

    public static String formatNumber(String str, String str2, String str3) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isDialable(str.charAt(i))) {
                return str;
            }
        }
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        if (str2 != null && str2.length() >= 2 && str2.charAt(0) == '+') {
            try {
                String regionCodeForNumber = phoneNumberUtil.getRegionCodeForNumber(phoneNumberUtil.parse(str2, "ZZ"));
                if (!TextUtils.isEmpty(regionCodeForNumber)) {
                    if (normalizeNumber(str).indexOf(str2.substring(1)) <= 0) {
                        str3 = regionCodeForNumber;
                    }
                }
            } catch (NumberParseException e) {
            }
        }
        String number = formatNumber(str, str3);
        return number != null ? number : str;
    }

    public static String normalizeNumber(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            int iDigit = Character.digit(cCharAt, 10);
            if (iDigit != -1) {
                sb.append(iDigit);
            } else if (sb.length() == 0 && cCharAt == '+') {
                sb.append(cCharAt);
            } else if ((cCharAt >= 'a' && cCharAt <= 'z') || (cCharAt >= 'A' && cCharAt <= 'Z')) {
                return normalizeNumber(convertKeypadLettersToDigits(str));
            }
        }
        return sb.toString();
    }

    public static String replaceUnicodeDigits(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (char c : str.toCharArray()) {
            int iDigit = Character.digit(c, 10);
            if (iDigit != -1) {
                sb.append(iDigit);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int getMinMatch() {
        if (sMtkPhoneNumberUtils != null) {
            try {
                return ((Integer) sMtkPhoneNumberUtils.getMethod("getMinMatch", new Class[0]).invoke(null, new Object[0])).intValue();
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkPhoneNumberUtils! Used AOSP for instead!");
                return 7;
            }
        }
        return 7;
    }

    public static boolean isEmergencyNumber(String str) {
        return isEmergencyNumber(getDefaultVoiceSubId(), str);
    }

    public static boolean isEmergencyNumber(int i, String str) {
        return isEmergencyNumberInternal(i, str, true);
    }

    public static boolean isPotentialEmergencyNumber(String str) {
        return isPotentialEmergencyNumber(getDefaultVoiceSubId(), str);
    }

    public static boolean isPotentialEmergencyNumber(int i, String str) {
        return isEmergencyNumberInternal(i, str, false);
    }

    private static boolean isEmergencyNumberInternal(String str, boolean z) {
        return isEmergencyNumberInternal(getDefaultVoiceSubId(), str, z);
    }

    private static boolean isEmergencyNumberInternal(int i, String str, boolean z) {
        return isEmergencyNumberInternal(i, str, null, z);
    }

    public static boolean isEmergencyNumber(String str, String str2) {
        return isEmergencyNumber(getDefaultVoiceSubId(), str, str2);
    }

    public static boolean isEmergencyNumber(int i, String str, String str2) {
        return isEmergencyNumberInternal(i, str, str2, true);
    }

    public static boolean isPotentialEmergencyNumber(String str, String str2) {
        return isPotentialEmergencyNumber(getDefaultVoiceSubId(), str, str2);
    }

    public static boolean isPotentialEmergencyNumber(int i, String str, String str2) {
        return isEmergencyNumberInternal(i, str, str2, false);
    }

    private static boolean isEmergencyNumberInternal(String str, String str2, boolean z) {
        return isEmergencyNumberInternal(getDefaultVoiceSubId(), str, str2, z);
    }

    private static boolean isEmergencyNumberInternal(int i, String str, String str2, boolean z) {
        if (sMtkPhoneNumberUtils != null) {
            try {
                return ((Boolean) sMtkPhoneNumberUtils.getMethod("isEmergencyNumberExt", Integer.TYPE, String.class, String.class, Boolean.TYPE).invoke(null, Integer.valueOf(i), str, str2, Boolean.valueOf(z))).booleanValue();
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkPhoneNumberUtils! Used AOSP for instead! e: " + e);
            }
        }
        if (str == null || isUriNumber(str)) {
            return false;
        }
        String strExtractNetworkPortionAlt = extractNetworkPortionAlt(str);
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        String str3 = SystemProperties.get(slotIndex <= 0 ? "ril.ecclist" : "ril.ecclist" + slotIndex, "");
        Rlog.d(LOG_TAG, "slotId:" + slotIndex + " subId:" + i + " country:" + str2 + " emergencyNumbers: " + str3);
        if (TextUtils.isEmpty(str3)) {
            str3 = SystemProperties.get("ro.ril.ecclist");
        }
        if (!TextUtils.isEmpty(str3)) {
            String[] strArrSplit = str3.split(",");
            for (String str4 : strArrSplit) {
                if (z || "BR".equalsIgnoreCase(str2)) {
                    if (strExtractNetworkPortionAlt.equals(str4)) {
                        return true;
                    }
                } else if (strExtractNetworkPortionAlt.startsWith(str4)) {
                    return true;
                }
            }
            return false;
        }
        Rlog.d(LOG_TAG, "System property doesn't provide any emergency numbers. Use embedded logic for determining ones.");
        for (String str5 : (slotIndex < 0 ? "112,911,000,08,110,118,119,999" : "112,911").split(",")) {
            if (z) {
                if (strExtractNetworkPortionAlt.equals(str5)) {
                    return true;
                }
            } else if (strExtractNetworkPortionAlt.startsWith(str5)) {
                return true;
            }
        }
        if (str2 == null) {
            return false;
        }
        ShortNumberInfo shortNumberInfo = ShortNumberInfo.getInstance();
        if (z) {
            return shortNumberInfo.isEmergencyNumber(strExtractNetworkPortionAlt, str2);
        }
        return shortNumberInfo.connectsToEmergencyNumber(strExtractNetworkPortionAlt, str2);
    }

    public static boolean isLocalEmergencyNumber(Context context, String str) {
        return isLocalEmergencyNumber(context, getDefaultVoiceSubId(), str);
    }

    public static boolean isLocalEmergencyNumber(Context context, int i, String str) {
        return isLocalEmergencyNumberInternal(i, str, context, true);
    }

    public static boolean isPotentialLocalEmergencyNumber(Context context, String str) {
        return isPotentialLocalEmergencyNumber(context, getDefaultVoiceSubId(), str);
    }

    public static boolean isPotentialLocalEmergencyNumber(Context context, int i, String str) {
        return isLocalEmergencyNumberInternal(i, str, context, false);
    }

    private static boolean isLocalEmergencyNumberInternal(String str, Context context, boolean z) {
        return isLocalEmergencyNumberInternal(getDefaultVoiceSubId(), str, context, z);
    }

    private static boolean isLocalEmergencyNumberInternal(int i, String str, Context context, boolean z) {
        String country;
        if (sMtkPhoneNumberUtils != null) {
            try {
                return ((Boolean) sMtkPhoneNumberUtils.getMethod("isLocalEmergencyNumberInternal", Integer.TYPE, String.class, Context.class, Boolean.TYPE).invoke(null, Integer.valueOf(i), str, context, Boolean.valueOf(z))).booleanValue();
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkPhoneNumberUtils! Used AOSP for instead! e: " + e);
            }
        }
        CountryDetector countryDetector = (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
        if (countryDetector != null && countryDetector.detectCountry() != null) {
            country = countryDetector.detectCountry().getCountryIso();
        } else {
            country = context.getResources().getConfiguration().locale.getCountry();
            Rlog.w(LOG_TAG, "No CountryDetector; falling back to countryIso based on locale: " + country);
        }
        return isEmergencyNumberInternal(i, str, country, z);
    }

    public static boolean isVoiceMailNumber(String str) {
        return isVoiceMailNumber(SubscriptionManager.getDefaultSubscriptionId(), str);
    }

    public static boolean isVoiceMailNumber(int i, String str) {
        return isVoiceMailNumber(null, i, str);
    }

    public static boolean isVoiceMailNumber(Context context, int i, String str) {
        TelephonyManager telephonyManagerFrom;
        boolean z;
        CarrierConfigManager carrierConfigManager;
        PersistableBundle configForSubId;
        try {
            if (context == null) {
                telephonyManagerFrom = TelephonyManager.getDefault();
            } else {
                telephonyManagerFrom = TelephonyManager.from(context);
            }
            String voiceMailNumber = telephonyManagerFrom.getVoiceMailNumber(i);
            String line1Number = telephonyManagerFrom.getLine1Number(i);
            String strExtractNetworkPortionAlt = extractNetworkPortionAlt(str);
            if (TextUtils.isEmpty(strExtractNetworkPortionAlt)) {
                return false;
            }
            if (context != null && (carrierConfigManager = (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE)) != null && (configForSubId = carrierConfigManager.getConfigForSubId(i)) != null) {
                z = configForSubId.getBoolean(CarrierConfigManager.KEY_MDN_IS_ADDITIONAL_VOICEMAIL_NUMBER_BOOL);
            } else {
                z = false;
            }
            if (z) {
                return compare(strExtractNetworkPortionAlt, voiceMailNumber) || compare(strExtractNetworkPortionAlt, line1Number);
            }
            return compare(strExtractNetworkPortionAlt, voiceMailNumber);
        } catch (SecurityException e) {
            return false;
        }
    }

    public static String convertKeypadLettersToDigits(String str) {
        int length;
        if (str == null || (length = str.length()) == 0) {
            return str;
        }
        char[] charArray = str.toCharArray();
        for (int i = 0; i < length; i++) {
            char c = charArray[i];
            charArray[i] = (char) KEYPAD_MAP.get(c, c);
        }
        return new String(charArray);
    }

    public static String cdmaCheckAndProcessPlusCode(String str) {
        if (sMtkPhoneNumberUtils != null) {
            try {
                return (String) sMtkPhoneNumberUtils.getMethod("cdmaCheckAndProcessPlusCode", String.class).invoke(null, str);
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkPhoneNumberUtils! Used AOSP for instead! e: " + e);
            }
        }
        if (!TextUtils.isEmpty(str) && isReallyDialable(str.charAt(0)) && isNonSeparator(str)) {
            String networkCountryIso = TelephonyManager.getDefault().getNetworkCountryIso();
            String simCountryIso = TelephonyManager.getDefault().getSimCountryIso();
            if (!TextUtils.isEmpty(networkCountryIso) && !TextUtils.isEmpty(simCountryIso)) {
                return cdmaCheckAndProcessPlusCodeByNumberFormat(str, getFormatTypeFromCountryCode(networkCountryIso), getFormatTypeFromCountryCode(simCountryIso));
            }
        }
        return str;
    }

    public static String cdmaCheckAndProcessPlusCodeForSms(String str) {
        if (!TextUtils.isEmpty(str) && isReallyDialable(str.charAt(0)) && isNonSeparator(str)) {
            String simCountryIso = TelephonyManager.getDefault().getSimCountryIso();
            if (!TextUtils.isEmpty(simCountryIso)) {
                int formatTypeFromCountryCode = getFormatTypeFromCountryCode(simCountryIso);
                return cdmaCheckAndProcessPlusCodeByNumberFormat(str, formatTypeFromCountryCode, formatTypeFromCountryCode);
            }
        }
        return str;
    }

    public static String cdmaCheckAndProcessPlusCodeByNumberFormat(String str, int i, int i2) {
        boolean z;
        String strExtractNetworkPortionAlt;
        String strProcessPlusCode;
        if (i != i2 || i != 1) {
            z = false;
        } else {
            z = true;
        }
        if (str != null && str.lastIndexOf(PLUS_SIGN_STRING) != -1) {
            String str2 = null;
            String strSubstring = str;
            while (true) {
                if (z) {
                    strExtractNetworkPortionAlt = extractNetworkPortion(strSubstring);
                } else {
                    strExtractNetworkPortionAlt = extractNetworkPortionAlt(strSubstring);
                }
                strProcessPlusCode = processPlusCode(strExtractNetworkPortionAlt, z);
                if (!TextUtils.isEmpty(strProcessPlusCode)) {
                    if (str2 != null) {
                        strProcessPlusCode = str2.concat(strProcessPlusCode);
                    }
                    String strExtractPostDialPortion = extractPostDialPortion(strSubstring);
                    if (!TextUtils.isEmpty(strExtractPostDialPortion)) {
                        int iFindDialableIndexFromPostDialStr = findDialableIndexFromPostDialStr(strExtractPostDialPortion);
                        if (iFindDialableIndexFromPostDialStr >= 1) {
                            strProcessPlusCode = appendPwCharBackToOrigDialStr(iFindDialableIndexFromPostDialStr, strProcessPlusCode, strExtractPostDialPortion);
                            strSubstring = strExtractPostDialPortion.substring(iFindDialableIndexFromPostDialStr);
                        } else {
                            if (iFindDialableIndexFromPostDialStr < 0) {
                                strExtractPostDialPortion = "";
                            }
                            Rlog.e("wrong postDialStr=", strExtractPostDialPortion);
                        }
                    }
                    if (TextUtils.isEmpty(strExtractPostDialPortion) || TextUtils.isEmpty(strSubstring)) {
                        break;
                    }
                    str2 = strProcessPlusCode;
                } else {
                    Rlog.e("checkAndProcessPlusCode: null newDialStr", strProcessPlusCode);
                    return str;
                }
            }
            return strProcessPlusCode;
        }
        return str;
    }

    public static CharSequence createTtsSpannable(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        Spannable spannableNewSpannable = Spannable.Factory.getInstance().newSpannable(charSequence);
        addTtsSpan(spannableNewSpannable, 0, spannableNewSpannable.length());
        return spannableNewSpannable;
    }

    public static void addTtsSpan(Spannable spannable, int i, int i2) {
        spannable.setSpan(createTtsSpan(spannable.subSequence(i, i2).toString()), i, i2, 33);
    }

    @Deprecated
    public static CharSequence ttsSpanAsPhoneNumber(CharSequence charSequence) {
        return createTtsSpannable(charSequence);
    }

    @Deprecated
    public static void ttsSpanAsPhoneNumber(Spannable spannable, int i, int i2) {
        addTtsSpan(spannable, i, i2);
    }

    public static TtsSpan createTtsSpan(String str) {
        Phonenumber.PhoneNumber phoneNumber = null;
        if (str == null) {
            return null;
        }
        try {
            phoneNumber = PhoneNumberUtil.getInstance().parse(str, (String) null);
        } catch (NumberParseException e) {
        }
        TtsSpan.TelephoneBuilder telephoneBuilder = new TtsSpan.TelephoneBuilder();
        if (phoneNumber == null) {
            telephoneBuilder.setNumberParts(splitAtNonNumerics(str));
        } else {
            if (phoneNumber.hasCountryCode()) {
                telephoneBuilder.setCountryCode(Integer.toString(phoneNumber.getCountryCode()));
            }
            telephoneBuilder.setNumberParts(Long.toString(phoneNumber.getNationalNumber()));
        }
        return telephoneBuilder.build();
    }

    private static String splitAtNonNumerics(CharSequence charSequence) {
        Object objValueOf;
        StringBuilder sb = new StringBuilder(charSequence.length());
        for (int i = 0; i < charSequence.length(); i++) {
            if (is12Key(charSequence.charAt(i))) {
                objValueOf = Character.valueOf(charSequence.charAt(i));
            } else {
                objValueOf = WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
            sb.append(objValueOf);
        }
        return sb.toString().replaceAll(" +", WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER).trim();
    }

    private static String getCurrentIdp(boolean z) {
        if (z) {
            return NANP_IDP_STRING;
        }
        return SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_IDP_STRING, PLUS_SIGN_STRING);
    }

    private static boolean isTwoToNine(char c) {
        if (c >= '2' && c <= '9') {
            return true;
        }
        return false;
    }

    private static int getFormatTypeFromCountryCode(String str) {
        int length = NANP_COUNTRIES.length;
        for (int i = 0; i < length; i++) {
            if (NANP_COUNTRIES[i].compareToIgnoreCase(str) == 0) {
                return 1;
            }
        }
        if ("jp".compareToIgnoreCase(str) != 0) {
            return 0;
        }
        return 2;
    }

    public static boolean isNanp(String str) {
        if (str != null) {
            if (str.length() != 10 || !isTwoToNine(str.charAt(0)) || !isTwoToNine(str.charAt(3))) {
                return false;
            }
            for (int i = 1; i < 10; i++) {
                if (!isISODigit(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        Rlog.e("isNanp: null dialStr passed in", str);
        return false;
    }

    private static boolean isOneNanp(String str) {
        if (str != null) {
            String strSubstring = str.substring(1);
            if (str.charAt(0) != '1' || !isNanp(strSubstring)) {
                return false;
            }
            return true;
        }
        Rlog.e("isOneNanp: null dialStr passed in", str);
        return false;
    }

    public static boolean isUriNumber(String str) {
        return str != null && (str.contains("@") || str.contains("%40"));
    }

    public static String getUsernameFromUriNumber(String str) {
        int iIndexOf = str.indexOf(64);
        if (iIndexOf < 0) {
            iIndexOf = str.indexOf("%40");
        }
        if (iIndexOf < 0) {
            Rlog.w(LOG_TAG, "getUsernameFromUriNumber: no delimiter found in SIP addr '" + str + "'");
            iIndexOf = str.length();
        }
        return str.substring(0, iIndexOf);
    }

    public static Uri convertSipUriToTelUri(Uri uri) {
        if (!"sip".equals(uri.getScheme())) {
            return uri;
        }
        String[] strArrSplit = uri.getSchemeSpecificPart().split("[@;:]");
        if (strArrSplit.length == 0) {
            return uri;
        }
        return Uri.fromParts(PhoneAccount.SCHEME_TEL, strArrSplit[0], null);
    }

    private static String processPlusCode(String str, boolean z) {
        if (str != null && str.charAt(0) == '+' && str.length() > 1) {
            String strSubstring = str.substring(1);
            if (!z || !isOneNanp(strSubstring)) {
                return str.replaceFirst("[+]", getCurrentIdp(z));
            }
            return strSubstring;
        }
        return str;
    }

    private static int findDialableIndexFromPostDialStr(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (isReallyDialable(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String appendPwCharBackToOrigDialStr(int i, String str, String str2) {
        if (i == 1) {
            return str + str2.charAt(0);
        }
        return str.concat(str2.substring(0, i));
    }

    private static boolean matchIntlPrefix(String str, int i) {
        char c = 0;
        for (int i2 = 0; i2 < i; i2++) {
            char cCharAt = str.charAt(i2);
            if (c != 0) {
                if (c != 2) {
                    if (c != 4) {
                        if (isNonSeparator(cCharAt)) {
                            return false;
                        }
                    } else if (cCharAt == '1') {
                        c = 5;
                    } else if (isNonSeparator(cCharAt)) {
                        return false;
                    }
                } else if (cCharAt == '0') {
                    c = 3;
                } else if (cCharAt == '1') {
                    c = 4;
                } else if (isNonSeparator(cCharAt)) {
                    return false;
                }
            } else if (cCharAt == '+') {
                c = 1;
            } else if (cCharAt == '0') {
                c = 2;
            } else if (isNonSeparator(cCharAt)) {
                return false;
            }
        }
        return c == 1 || c == 3 || c == 5;
    }

    private static boolean matchIntlPrefixAndCC(String str, int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            char cCharAt = str.charAt(i3);
            switch (i2) {
                case 0:
                    if (cCharAt == '+') {
                        i2 = 1;
                    } else if (cCharAt == '0') {
                        i2 = 2;
                    } else {
                        if (isNonSeparator(cCharAt)) {
                            return false;
                        }
                    }
                    break;
                case 1:
                case 3:
                case 5:
                    if (isISODigit(cCharAt)) {
                        i2 = 6;
                    } else {
                        if (isNonSeparator(cCharAt)) {
                            return false;
                        }
                    }
                    break;
                case 2:
                    if (cCharAt == '0') {
                        i2 = 3;
                    } else if (cCharAt == '1') {
                        i2 = 4;
                    } else {
                        if (isNonSeparator(cCharAt)) {
                            return false;
                        }
                    }
                    break;
                case 4:
                    if (cCharAt == '1') {
                        i2 = 5;
                    } else {
                        if (isNonSeparator(cCharAt)) {
                            return false;
                        }
                    }
                    break;
                case 6:
                case 7:
                    if (isISODigit(cCharAt)) {
                        i2++;
                    } else {
                        if (isNonSeparator(cCharAt)) {
                            return false;
                        }
                    }
                    break;
                default:
                    if (isNonSeparator(cCharAt)) {
                        return false;
                    }
                    break;
                    break;
            }
        }
        return i2 == 6 || i2 == 7 || i2 == 8;
    }

    private static boolean matchTrunkPrefix(String str, int i) {
        boolean z = false;
        for (int i2 = 0; i2 < i; i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '0' && !z) {
                z = true;
            } else if (isNonSeparator(cCharAt)) {
                return false;
            }
        }
        return z;
    }

    private static boolean isCountryCallingCode(int i) {
        return i > 0 && i < CCC_LENGTH && COUNTRY_CALLING_CALL[i];
    }

    private static int tryGetISODigit(char c) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        return -1;
    }

    private static class CountryCallingCodeAndNewIndex {
        public final int countryCallingCode;
        public final int newIndex;

        public CountryCallingCodeAndNewIndex(int i, int i2) {
            this.countryCallingCode = i;
            this.newIndex = i2;
        }
    }

    private static CountryCallingCodeAndNewIndex tryGetCountryCallingCodeAndNewIndex(String str, boolean z) {
        int length = str.length();
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < length; i3++) {
            char cCharAt = str.charAt(i3);
            switch (i) {
                case 0:
                    if (cCharAt == '+') {
                        i = 1;
                    } else if (cCharAt == '0') {
                        i = 2;
                    } else if (cCharAt == '1') {
                        if (!z) {
                            return null;
                        }
                        i = 8;
                    } else {
                        if (isDialable(cCharAt)) {
                            return null;
                        }
                    }
                    break;
                case 1:
                case 3:
                case 5:
                case 6:
                case 7:
                    int iTryGetISODigit = tryGetISODigit(cCharAt);
                    if (iTryGetISODigit > 0) {
                        i2 = (i2 * 10) + iTryGetISODigit;
                        if (i2 >= 100 || isCountryCallingCode(i2)) {
                            return new CountryCallingCodeAndNewIndex(i2, i3 + 1);
                        }
                        i = (i != 1 && i != 3 && i != 5) ? i + 1 : 6;
                    } else {
                        if (isDialable(cCharAt)) {
                            return null;
                        }
                    }
                    break;
                case 2:
                    if (cCharAt == '0') {
                        i = 3;
                    } else if (cCharAt == '1') {
                        i = 4;
                    } else {
                        if (isDialable(cCharAt)) {
                            return null;
                        }
                    }
                    break;
                case 4:
                    if (cCharAt == '1') {
                        i = 5;
                    } else {
                        if (isDialable(cCharAt)) {
                            return null;
                        }
                    }
                    break;
                case 8:
                    if (cCharAt == '6') {
                        i = 9;
                    } else {
                        if (isDialable(cCharAt)) {
                            return null;
                        }
                    }
                    break;
                case 9:
                    if (cCharAt == '6') {
                        return new CountryCallingCodeAndNewIndex(66, i3 + 1);
                    }
                    return null;
                default:
                    return null;
            }
        }
        return null;
    }

    private static int tryGetTrunkPrefixOmittedIndex(String str, int i) {
        int length = str.length();
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (tryGetISODigit(cCharAt) >= 0) {
                return i + 1;
            }
            if (isDialable(cCharAt)) {
                return -1;
            }
            i++;
        }
        return -1;
    }

    private static boolean checkPrefixIsIgnorable(String str, int i, int i2) {
        boolean z = false;
        while (i2 >= i) {
            if (tryGetISODigit(str.charAt(i2)) >= 0) {
                if (z) {
                    return false;
                }
                z = true;
            } else if (isDialable(str.charAt(i2))) {
                return false;
            }
            i2--;
        }
        return true;
    }

    private static int getDefaultVoiceSubId() {
        if (sMtkPhoneNumberUtils != null) {
            try {
                return ((Integer) sMtkPhoneNumberUtils.getMethod("getDefaultVoiceSubId", new Class[0]).invoke(null, new Object[0])).intValue();
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkPhoneNumberUtils! Used AOSP for instead!");
            }
        }
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    public static String convertToEmergencyNumber(Context context, String str) {
        String[] strArrSplit;
        String str2;
        if (context == null || TextUtils.isEmpty(str)) {
            return str;
        }
        String strNormalizeNumber = normalizeNumber(str);
        if (isEmergencyNumber(strNormalizeNumber)) {
            return str;
        }
        if (sConvertToEmergencyMap == null) {
            sConvertToEmergencyMap = context.getResources().getStringArray(R.array.config_convert_to_emergency_number_map);
        }
        if (sConvertToEmergencyMap == null || sConvertToEmergencyMap.length == 0) {
            return str;
        }
        for (String str3 : sConvertToEmergencyMap) {
            String[] strArrSplit2 = null;
            if (!TextUtils.isEmpty(str3)) {
                strArrSplit = str3.split(SettingsStringUtil.DELIMITER);
            } else {
                strArrSplit = null;
            }
            if (strArrSplit != null && strArrSplit.length == 2) {
                str2 = strArrSplit[1];
                if (!TextUtils.isEmpty(strArrSplit[0])) {
                    strArrSplit2 = strArrSplit[0].split(",");
                }
            } else {
                str2 = null;
            }
            if (!TextUtils.isEmpty(str2) && strArrSplit2 != null && strArrSplit2.length != 0) {
                for (String str4 : strArrSplit2) {
                    if (!TextUtils.isEmpty(str4) && str4.equals(strNormalizeNumber)) {
                        return str2;
                    }
                }
            }
        }
        return str;
    }

    private static Class<?> getMtkPhoneNumberUtils() {
        try {
            return Class.forName("com.mediatek.internal.telephony.MtkPhoneNumberUtils");
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "No MtkPhoneNumberUtils! Used AOSP for instead!");
            return null;
        }
    }
}
