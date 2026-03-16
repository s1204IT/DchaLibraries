package com.android.contacts.detail;

import android.content.Context;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.view.MenuItem;
import com.android.contacts.R;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.preference.ContactsPreferences;

public class ContactDisplayUtils {
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();

    public static CharSequence getDisplayName(Context context, Contact contactData) {
        ContactsPreferences prefs = new ContactsPreferences(context);
        CharSequence displayName = contactData.getDisplayName();
        if (prefs.getDisplayOrder() == 1) {
            if (!TextUtils.isEmpty(displayName)) {
                if (contactData.getDisplayNameSource() == 20) {
                    return sBidiFormatter.unicodeWrap(displayName.toString(), TextDirectionHeuristics.LTR);
                }
                return displayName;
            }
        } else {
            CharSequence altDisplayName = contactData.getAltDisplayName();
            if (!TextUtils.isEmpty(altDisplayName)) {
                return altDisplayName;
            }
        }
        return context.getResources().getString(R.string.missing_name);
    }

    public static void configureStarredMenuItem(MenuItem starredMenuItem, boolean isDirectoryEntry, boolean isUserProfile, boolean isStarred) {
        if (!isDirectoryEntry && !isUserProfile) {
            starredMenuItem.setVisible(true);
            int resId = isStarred ? R.drawable.ic_star_24dp : R.drawable.ic_star_outline_24dp;
            starredMenuItem.setIcon(resId);
            starredMenuItem.setChecked(isStarred);
            starredMenuItem.setTitle(isStarred ? R.string.menu_removeStar : R.string.menu_addStar);
            return;
        }
        starredMenuItem.setVisible(false);
    }
}
