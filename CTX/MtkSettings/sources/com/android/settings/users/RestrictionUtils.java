package com.android.settings.users;

import android.content.Context;
import android.content.RestrictionEntry;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.R;
import java.util.ArrayList;
import java.util.Iterator;

public class RestrictionUtils {
    public static final String[] sRestrictionKeys = {"no_share_location"};
    public static final int[] sRestrictionTitles = {R.string.restriction_location_enable_title};
    public static final int[] sRestrictionDescriptions = {R.string.restriction_location_enable_summary};

    public static ArrayList<RestrictionEntry> getRestrictions(Context context, UserHandle userHandle) {
        Resources resources = context.getResources();
        ArrayList<RestrictionEntry> arrayList = new ArrayList<>();
        Bundle userRestrictions = UserManager.get(context).getUserRestrictions(userHandle);
        for (int i = 0; i < sRestrictionKeys.length; i++) {
            RestrictionEntry restrictionEntry = new RestrictionEntry(sRestrictionKeys[i], !userRestrictions.getBoolean(sRestrictionKeys[i], false));
            restrictionEntry.setTitle(resources.getString(sRestrictionTitles[i]));
            restrictionEntry.setDescription(resources.getString(sRestrictionDescriptions[i]));
            restrictionEntry.setType(1);
            arrayList.add(restrictionEntry);
        }
        return arrayList;
    }

    public static void setRestrictions(Context context, ArrayList<RestrictionEntry> arrayList, UserHandle userHandle) {
        UserManager userManager = UserManager.get(context);
        Iterator<RestrictionEntry> it = arrayList.iterator();
        while (it.hasNext()) {
            userManager.setUserRestriction(it.next().getKey(), !r0.getSelectedState(), userHandle);
        }
    }
}
