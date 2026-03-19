package com.android.contacts.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Patterns;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.preference.ContactsPreferences;
import com.google.common.base.Preconditions;
import com.mediatek.contacts.util.Log;

public class ContactDisplayUtils {
    public static final int INTERACTION_CALL = 1;
    public static final int INTERACTION_SMS = 2;
    private static final String TAG = ContactDisplayUtils.class.getSimpleName();

    public static boolean isCustomPhoneType(Integer num) {
        return num.intValue() == 0 || num.intValue() == 19;
    }

    public static CharSequence getLabelForCallOrSms(Integer num, CharSequence charSequence, int i, Context context) {
        int phoneLabelResourceId;
        Preconditions.checkNotNull(context);
        if (isCustomPhoneType(num)) {
            return charSequence == null ? "" : charSequence;
        }
        if (i == 2) {
            phoneLabelResourceId = getSmsLabelResourceId(num);
        } else {
            phoneLabelResourceId = getPhoneLabelResourceId(num);
            if (i != 1) {
                Log.e(TAG, "Un-recognized interaction type: " + i + ". Defaulting to ContactDisplayUtils.INTERACTION_CALL.");
            }
        }
        return context.getResources().getText(phoneLabelResourceId);
    }

    public static int getPhoneLabelResourceId(Integer num) {
        if (num == null) {
            return R.string.call_other;
        }
        switch (num.intValue()) {
        }
        return R.string.call_other;
    }

    public static int getSmsLabelResourceId(Integer num) {
        if (num == null) {
            return R.string.sms_other;
        }
        switch (num.intValue()) {
        }
        return R.string.sms_other;
    }

    public static boolean isPossiblePhoneNumber(CharSequence charSequence) {
        if (charSequence == null) {
            return false;
        }
        return Patterns.PHONE.matcher(charSequence.toString()).matches();
    }

    public static Spannable getTelephoneTtsSpannable(String str, String str2) {
        if (str == null) {
            return null;
        }
        SpannableString spannableString = new SpannableString(str);
        int iIndexOf = str2 == null ? -1 : str.indexOf(str2);
        while (iIndexOf >= 0) {
            int length = str2.length() + iIndexOf;
            spannableString.setSpan(PhoneNumberUtilsCompat.createTtsSpan(str2), iIndexOf, length, 33);
            iIndexOf = str.indexOf(str2, length);
        }
        return spannableString;
    }

    public static CharSequence getTtsSpannedPhoneNumber(Resources resources, int i, String str) {
        return getTelephoneTtsSpannable(resources.getString(i, str), str);
    }

    public static String getPreferredDisplayName(String str, String str2, ContactsPreferences contactsPreferences) {
        if (contactsPreferences == null) {
            return str != null ? str : str2;
        }
        if (contactsPreferences.getDisplayOrder() != 1 && contactsPreferences.getDisplayOrder() == 2 && !TextUtils.isEmpty(str2)) {
            return str2;
        }
        return str;
    }

    public static String getPreferredSortName(String str, String str2, ContactsPreferences contactsPreferences) {
        if (contactsPreferences == null) {
            return str != null ? str : str2;
        }
        if (contactsPreferences.getSortOrder() != 1 && contactsPreferences.getSortOrder() == 2 && !TextUtils.isEmpty(str2)) {
            return str2;
        }
        return str;
    }
}
