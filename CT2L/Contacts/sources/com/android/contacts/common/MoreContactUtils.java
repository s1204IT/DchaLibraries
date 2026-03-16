package com.android.contacts.common;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.android.contacts.common.model.account.AccountType;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class MoreContactUtils {
    private static final String WAIT_SYMBOL_AS_STRING = String.valueOf(';');

    public static boolean shouldCollapse(CharSequence mimetype1, CharSequence data1, CharSequence mimetype2, CharSequence data2) {
        if (!TextUtils.equals(mimetype1, mimetype2)) {
            return false;
        }
        if (TextUtils.equals(data1, data2)) {
            return true;
        }
        if (data1 == null || data2 == null || !TextUtils.equals("vnd.android.cursor.item/phone_v2", mimetype1)) {
            return false;
        }
        return shouldCollapsePhoneNumbers(data1.toString(), data2.toString());
    }

    private static boolean shouldCollapsePhoneNumbers(String number1, String number2) {
        String[] dataParts1 = number1.split(WAIT_SYMBOL_AS_STRING);
        String[] dataParts2 = number2.split(WAIT_SYMBOL_AS_STRING);
        if (dataParts1.length != dataParts2.length) {
            return false;
        }
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        for (int i = 0; i < dataParts1.length; i++) {
            String dataPart1 = PhoneNumberUtils.convertKeypadLettersToDigits(dataParts1[i]);
            String dataPart2 = dataParts2[i];
            if (!TextUtils.equals(dataPart1, dataPart2)) {
                PhoneNumberUtil.MatchType result = util.isNumberMatch(dataPart1, dataPart2);
                switch (result) {
                    case NOT_A_NUMBER:
                    case NO_MATCH:
                    case SHORT_NSN_MATCH:
                        return false;
                    case EXACT_MATCH:
                        break;
                    case NSN_MATCH:
                        try {
                            if (util.parse(dataPart1, null).getCountryCode() != 1 || dataPart2.trim().charAt(0) == '1') {
                                return false;
                            }
                        } catch (NumberParseException e) {
                            try {
                                util.parse(dataPart2, null);
                                return false;
                            } catch (NumberParseException e2) {
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown result value from phone number library");
                }
            }
        }
        return true;
    }

    public static Rect getTargetRectFromView(View view) {
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        Rect rect = new Rect();
        rect.left = pos[0];
        rect.top = pos[1];
        rect.right = pos[0] + view.getWidth();
        rect.bottom = pos[1] + view.getHeight();
        return rect;
    }

    public static TextView createHeaderView(Context context, int textResourceId) {
        TextView textView = (TextView) View.inflate(context, com.android.contacts.R.layout.list_separator, null);
        textView.setText(context.getString(textResourceId));
        return textView;
    }

    public static void setHeaderViewBottomPadding(Context context, TextView textView, boolean isFirstRow) {
        int topPadding;
        if (isFirstRow) {
            topPadding = (int) context.getResources().getDimension(com.android.contacts.R.dimen.frequently_contacted_title_top_margin_when_first_row);
        } else {
            topPadding = (int) context.getResources().getDimension(com.android.contacts.R.dimen.frequently_contacted_title_top_margin);
        }
        textView.setPaddingRelative(textView.getPaddingStart(), topPadding, textView.getPaddingEnd(), textView.getPaddingBottom());
    }

    public static Intent getInvitableIntent(AccountType accountType, Uri lookupUri) {
        String syncAdapterPackageName = accountType.syncAdapterPackageName;
        String className = accountType.getInviteContactActivityClassName();
        if (TextUtils.isEmpty(syncAdapterPackageName) || TextUtils.isEmpty(className)) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName(syncAdapterPackageName, className);
        intent.setAction("com.android.contacts.action.INVITE_CONTACT");
        intent.setData(lookupUri);
        return intent;
    }
}
