package android.text.method;

import android.icu.lang.UCharacter;
import android.icu.text.DecimalFormatSymbols;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

public class DigitsKeyListener extends NumberKeyListener {
    private static final int DECIMAL = 2;
    private static final String DEFAULT_DECIMAL_POINT_CHARS = ".";
    private static final String DEFAULT_SIGN_CHARS = "-+";
    private static final char EN_DASH = 8211;
    private static final char MINUS_SIGN = 8722;
    private static final int SIGN = 1;
    private char[] mAccepted;
    private final boolean mDecimal;
    private String mDecimalPointChars;
    private final Locale mLocale;
    private boolean mNeedsAdvancedInput;
    private final boolean mSign;
    private String mSignChars;
    private final boolean mStringMode;
    private static final char HYPHEN_MINUS = '-';
    private static final char[][] COMPATIBILITY_CHARACTERS = {new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}, new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', HYPHEN_MINUS, '+'}, new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'}, new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', HYPHEN_MINUS, '+', '.'}};
    private static final Object sLocaleCacheLock = new Object();

    @GuardedBy("sLocaleCacheLock")
    private static final HashMap<Locale, DigitsKeyListener[]> sLocaleInstanceCache = new HashMap<>();
    private static final Object sStringCacheLock = new Object();

    @GuardedBy("sStringCacheLock")
    private static final HashMap<String, DigitsKeyListener> sStringInstanceCache = new HashMap<>();

    @Override
    protected char[] getAcceptedChars() {
        return this.mAccepted;
    }

    private boolean isSignChar(char c) {
        return this.mSignChars.indexOf(c) != -1;
    }

    private boolean isDecimalPointChar(char c) {
        return this.mDecimalPointChars.indexOf(c) != -1;
    }

    @Deprecated
    public DigitsKeyListener() {
        this(null, false, false);
    }

    @Deprecated
    public DigitsKeyListener(boolean z, boolean z2) {
        this(null, z, z2);
    }

    public DigitsKeyListener(Locale locale) {
        this(locale, false, false);
    }

    private void setToCompat() {
        this.mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        this.mSignChars = DEFAULT_SIGN_CHARS;
        this.mAccepted = COMPATIBILITY_CHARACTERS[(this.mSign ? 1 : 0) | (this.mDecimal ? 2 : 0)];
        this.mNeedsAdvancedInput = false;
    }

    private void calculateNeedForAdvancedInput() {
        this.mNeedsAdvancedInput = !ArrayUtils.containsAll(COMPATIBILITY_CHARACTERS[(this.mSign ? 1 : 0) | (this.mDecimal ? 2 : 0)], this.mAccepted);
    }

    private static String stripBidiControls(String str) {
        String strValueOf = "";
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (!UCharacter.hasBinaryProperty(cCharAt, 2)) {
                strValueOf = strValueOf.isEmpty() ? String.valueOf(cCharAt) : strValueOf + cCharAt;
            }
        }
        return strValueOf;
    }

    public DigitsKeyListener(Locale locale, boolean z, boolean z2) {
        this.mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        this.mSignChars = DEFAULT_SIGN_CHARS;
        this.mSign = z;
        this.mDecimal = z2;
        this.mStringMode = false;
        this.mLocale = locale;
        if (locale == null) {
            setToCompat();
            return;
        }
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        if (!NumberKeyListener.addDigits(linkedHashSet, locale)) {
            setToCompat();
            return;
        }
        if (z || z2) {
            DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale);
            if (z) {
                String strStripBidiControls = stripBidiControls(decimalFormatSymbols.getMinusSignString());
                String strStripBidiControls2 = stripBidiControls(decimalFormatSymbols.getPlusSignString());
                if (strStripBidiControls.length() > 1 || strStripBidiControls2.length() > 1) {
                    setToCompat();
                    return;
                }
                char cCharAt = strStripBidiControls.charAt(0);
                char cCharAt2 = strStripBidiControls2.charAt(0);
                linkedHashSet.add(Character.valueOf(cCharAt));
                linkedHashSet.add(Character.valueOf(cCharAt2));
                this.mSignChars = "" + cCharAt + cCharAt2;
                if (cCharAt == 8722 || cCharAt == 8211) {
                    linkedHashSet.add(Character.valueOf(HYPHEN_MINUS));
                    this.mSignChars += HYPHEN_MINUS;
                }
            }
            if (z2) {
                String decimalSeparatorString = decimalFormatSymbols.getDecimalSeparatorString();
                if (decimalSeparatorString.length() > 1) {
                    setToCompat();
                    return;
                } else {
                    Character chValueOf = Character.valueOf(decimalSeparatorString.charAt(0));
                    linkedHashSet.add(chValueOf);
                    this.mDecimalPointChars = chValueOf.toString();
                }
            }
        }
        this.mAccepted = NumberKeyListener.collectionToArray(linkedHashSet);
        calculateNeedForAdvancedInput();
    }

    private DigitsKeyListener(String str) {
        this.mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        this.mSignChars = DEFAULT_SIGN_CHARS;
        this.mSign = false;
        this.mDecimal = false;
        this.mStringMode = true;
        this.mLocale = null;
        this.mAccepted = new char[str.length()];
        str.getChars(0, str.length(), this.mAccepted, 0);
        this.mNeedsAdvancedInput = false;
    }

    @Deprecated
    public static DigitsKeyListener getInstance() {
        return getInstance(false, false);
    }

    @Deprecated
    public static DigitsKeyListener getInstance(boolean z, boolean z2) {
        return getInstance(null, z, z2);
    }

    public static DigitsKeyListener getInstance(Locale locale) {
        return getInstance(locale, false, false);
    }

    public static DigitsKeyListener getInstance(Locale locale, boolean z, boolean z2) {
        int i = (z2 ? 2 : 0) | (z ? 1 : 0);
        synchronized (sLocaleCacheLock) {
            DigitsKeyListener[] digitsKeyListenerArr = sLocaleInstanceCache.get(locale);
            if (digitsKeyListenerArr != null && digitsKeyListenerArr[i] != null) {
                return digitsKeyListenerArr[i];
            }
            if (digitsKeyListenerArr == null) {
                digitsKeyListenerArr = new DigitsKeyListener[4];
                sLocaleInstanceCache.put(locale, digitsKeyListenerArr);
            }
            DigitsKeyListener digitsKeyListener = new DigitsKeyListener(locale, z, z2);
            digitsKeyListenerArr[i] = digitsKeyListener;
            return digitsKeyListener;
        }
    }

    public static DigitsKeyListener getInstance(String str) {
        DigitsKeyListener digitsKeyListener;
        synchronized (sStringCacheLock) {
            digitsKeyListener = sStringInstanceCache.get(str);
            if (digitsKeyListener == null) {
                digitsKeyListener = new DigitsKeyListener(str);
                sStringInstanceCache.put(str, digitsKeyListener);
            }
        }
        return digitsKeyListener;
    }

    public static DigitsKeyListener getInstance(Locale locale, DigitsKeyListener digitsKeyListener) {
        if (digitsKeyListener.mStringMode) {
            return digitsKeyListener;
        }
        return getInstance(locale, digitsKeyListener.mSign, digitsKeyListener.mDecimal);
    }

    @Override
    public int getInputType() {
        if (this.mNeedsAdvancedInput) {
            return 1;
        }
        int i = 2;
        if (this.mSign) {
            i = 4098;
        }
        if (this.mDecimal) {
            return i | 8192;
        }
        return i;
    }

    @Override
    public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
        CharSequence charSequenceFilter = super.filter(charSequence, i, i2, spanned, i3, i4);
        if (!this.mSign && !this.mDecimal) {
            return charSequenceFilter;
        }
        if (charSequenceFilter != null) {
            i2 = charSequenceFilter.length();
            charSequence = charSequenceFilter;
            i = 0;
        }
        int length = spanned.length();
        int i5 = -1;
        int i6 = -1;
        for (int i7 = 0; i7 < i3; i7++) {
            char cCharAt = spanned.charAt(i7);
            if (isSignChar(cCharAt)) {
                i5 = i7;
            } else if (isDecimalPointChar(cCharAt)) {
                i6 = i7;
            }
        }
        while (i4 < length) {
            char cCharAt2 = spanned.charAt(i4);
            if (isSignChar(cCharAt2)) {
                return "";
            }
            if (isDecimalPointChar(cCharAt2)) {
                i6 = i4;
            }
            i4++;
        }
        SpannableStringBuilder spannableStringBuilder = null;
        for (int i8 = i2 - 1; i8 >= i; i8--) {
            char cCharAt3 = charSequence.charAt(i8);
            boolean z = true;
            if (isSignChar(cCharAt3)) {
                if (i8 == i && i3 == 0 && i5 < 0) {
                    i5 = i8;
                    z = false;
                }
            } else if (!isDecimalPointChar(cCharAt3)) {
                z = false;
            } else if (i6 < 0) {
                i6 = i8;
                z = false;
            }
            if (z) {
                if (i2 == i + 1) {
                    return "";
                }
                if (spannableStringBuilder == null) {
                    spannableStringBuilder = new SpannableStringBuilder(charSequence, i, i2);
                }
                spannableStringBuilder.delete(i8 - i, (i8 + 1) - i);
            }
        }
        if (spannableStringBuilder != null) {
            return spannableStringBuilder;
        }
        if (charSequenceFilter != null) {
            return charSequenceFilter;
        }
        return null;
    }
}
