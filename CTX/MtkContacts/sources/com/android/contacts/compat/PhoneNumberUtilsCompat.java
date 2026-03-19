package com.android.contacts.compat;

import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberUtilsCompat {
    public static String normalizeNumber(String str) {
        if (CompatUtils.isLollipopCompatible()) {
            return PhoneNumberUtils.normalizeNumber(str);
        }
        return normalizeNumberInternal(str);
    }

    private static String normalizeNumberInternal(String str) {
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
                return normalizeNumber(PhoneNumberUtils.convertKeypadLettersToDigits(str));
            }
        }
        return sb.toString();
    }

    public static String formatNumber(String str, String str2, String str3) {
        if (CompatUtils.isLollipopCompatible()) {
            return PhoneNumberUtils.formatNumber(str, str2, str3);
        }
        return PhoneNumberUtils.formatNumber(str);
    }

    public static CharSequence createTtsSpannable(CharSequence charSequence) {
        if (CompatUtils.isMarshmallowCompatible()) {
            return PhoneNumberUtils.createTtsSpannable(charSequence);
        }
        return createTtsSpannableInternal(charSequence);
    }

    public static TtsSpan createTtsSpan(String str) {
        if (CompatUtils.isMarshmallowCompatible()) {
            return PhoneNumberUtils.createTtsSpan(str);
        }
        if (CompatUtils.isLollipopCompatible()) {
            return createTtsSpanLollipop(str);
        }
        return null;
    }

    private static CharSequence createTtsSpannableInternal(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        Spannable spannableNewSpannable = Spannable.Factory.getInstance().newSpannable(charSequence);
        addTtsSpanInternal(spannableNewSpannable, 0, spannableNewSpannable.length());
        return spannableNewSpannable;
    }

    public static void addTtsSpan(Spannable spannable, int i, int i2) {
        if (CompatUtils.isMarshmallowCompatible()) {
            PhoneNumberUtils.addTtsSpan(spannable, i, i2);
        } else {
            addTtsSpanInternal(spannable, i, i2);
        }
    }

    private static void addTtsSpanInternal(Spannable spannable, int i, int i2) {
        spannable.setSpan(createTtsSpan(spannable.subSequence(i, i2).toString()), i, i2, 33);
    }

    private static TtsSpan createTtsSpanLollipop(String str) throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = null;
        if (str == null) {
            return null;
        }
        try {
            phoneNumber = PhoneNumberUtil.getInstance().parse(str, null);
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
            if (PhoneNumberUtils.isISODigit(charSequence.charAt(i))) {
                objValueOf = Character.valueOf(charSequence.charAt(i));
            } else {
                objValueOf = " ";
            }
            sb.append(objValueOf);
        }
        return sb.toString().replaceAll(" +", " ").trim();
    }
}
