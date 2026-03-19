package com.android.contacts.detail;

import android.content.Context;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.view.MenuItem;
import com.android.contacts.R;
import com.android.contacts.model.Contact;
import com.android.contacts.preference.ContactsPreferences;

public class ContactDisplayUtils {
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();

    public static CharSequence getDisplayName(Context context, Contact contact) {
        ContactsPreferences contactsPreferences = new ContactsPreferences(context);
        String displayName = contact.getDisplayName();
        if (contactsPreferences.getDisplayOrder() == 1) {
            if (!TextUtils.isEmpty(displayName)) {
                if (contact.getDisplayNameSource() == 20) {
                    return sBidiFormatter.unicodeWrap(displayName.toString(), TextDirectionHeuristics.LTR);
                }
                return displayName;
            }
        } else {
            String altDisplayName = contact.getAltDisplayName();
            if (!TextUtils.isEmpty(altDisplayName)) {
                return altDisplayName;
            }
        }
        return context.getResources().getString(R.string.missing_name);
    }

    public static String getPhoneticName(Context context, Contact contact) {
        String phoneticName = contact.getPhoneticName();
        if (!TextUtils.isEmpty(phoneticName)) {
            return phoneticName;
        }
        return null;
    }

    public static void configureStarredMenuItem(MenuItem menuItem, boolean z, boolean z2, boolean z3) {
        int i;
        if (!z && !z2) {
            menuItem.setVisible(true);
            if (z3) {
                i = R.drawable.quantum_ic_star_vd_theme_24;
            } else {
                i = R.drawable.quantum_ic_star_border_vd_theme_24;
            }
            menuItem.setIcon(i);
            menuItem.setChecked(z3);
            menuItem.setTitle(z3 ? R.string.menu_removeStar : R.string.menu_addStar);
            return;
        }
        menuItem.setVisible(false);
    }
}
