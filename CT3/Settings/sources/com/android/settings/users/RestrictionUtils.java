package com.android.settings.users;

import android.content.Context;
import android.content.RestrictionEntry;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.R;
import java.util.ArrayList;

public class RestrictionUtils {
    public static final String[] sRestrictionKeys = {"no_share_location"};
    public static final int[] sRestrictionTitles = {R.string.restriction_location_enable_title};
    public static final int[] sRestrictionDescriptions = {R.string.restriction_location_enable_summary};

    public static ArrayList<RestrictionEntry> getRestrictions(Context context, UserHandle user) {
        Resources res = context.getResources();
        ArrayList<RestrictionEntry> entries = new ArrayList<>();
        UserManager um = UserManager.get(context);
        Bundle userRestrictions = um.getUserRestrictions(user);
        for (int i = 0; i < sRestrictionKeys.length; i++) {
            RestrictionEntry entry = new RestrictionEntry(sRestrictionKeys[i], !userRestrictions.getBoolean(sRestrictionKeys[i], false));
            entry.setTitle(res.getString(sRestrictionTitles[i]));
            entry.setDescription(res.getString(sRestrictionDescriptions[i]));
            entry.setType(1);
            entries.add(entry);
        }
        return entries;
    }

    public static void setRestrictions(Context context, ArrayList<RestrictionEntry> entries, UserHandle user) {
        UserManager um = UserManager.get(context);
        for (RestrictionEntry entry : entries) {
            um.setUserRestriction(entry.getKey(), !entry.getSelectedState(), user);
        }
    }
}
