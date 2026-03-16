package com.android.contacts.common.util;

import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TtsSpan;
import android.util.Patterns;
import com.android.contacts.R;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;

public class ContactDisplayUtils {
    private static final String TAG = ContactDisplayUtils.class.getSimpleName();

    public static boolean isCustomPhoneType(Integer type) {
        return type.intValue() == 0 || type.intValue() == 19;
    }

    public static int getPhoneLabelResourceId(Integer type) {
        if (type == null) {
            return R.string.call_other;
        }
        switch (type.intValue()) {
        }
        return R.string.call_other;
    }

    public static int getSmsLabelResourceId(Integer type) {
        if (type == null) {
            return R.string.sms_other;
        }
        switch (type.intValue()) {
        }
        return R.string.sms_other;
    }

    public static boolean isPossiblePhoneNumber(CharSequence text) {
        if (text == null) {
            return false;
        }
        return Patterns.PHONE.matcher(text.toString()).matches();
    }

    public static Spannable getTelephoneTtsSpannable(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        Spannable spannable = new SpannableString(phoneNumber);
        TtsSpan ttsSpan = getTelephoneTtsSpan(phoneNumber);
        spannable.setSpan(ttsSpan, 0, phoneNumber.length(), 33);
        return spannable;
    }

    public static Spannable getTelephoneTtsSpannable(String message, String phoneNumber) {
        if (message == null) {
            return null;
        }
        Spannable spannable = new SpannableString(message);
        int start = phoneNumber == null ? -1 : message.indexOf(phoneNumber);
        while (start >= 0) {
            int end = start + phoneNumber.length();
            TtsSpan ttsSpan = getTelephoneTtsSpan(phoneNumber);
            spannable.setSpan(ttsSpan, start, end, 33);
            start = message.indexOf(phoneNumber, end);
        }
        return spannable;
    }

    public static TtsSpan getTelephoneTtsSpan(String phoneNumberString) {
        if (phoneNumberString == null) {
            throw new NullPointerException();
        }
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneNumberUtil.parse(phoneNumberString, (String) null);
        } catch (NumberParseException e) {
        }
        TtsSpan.TelephoneBuilder builder = new TtsSpan.TelephoneBuilder();
        if (phoneNumber == null) {
            builder.setNumberParts(PhoneNumberUtils.stripSeparators(phoneNumberString));
        } else {
            if (phoneNumber.hasCountryCode()) {
                builder.setCountryCode(Integer.toString(phoneNumber.getCountryCode()));
            }
            builder.setNumberParts(Long.toString(phoneNumber.getNationalNumber()));
        }
        return builder.build();
    }
}
