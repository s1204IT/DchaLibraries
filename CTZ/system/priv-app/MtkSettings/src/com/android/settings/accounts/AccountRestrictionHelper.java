package com.android.settings.accounts;

import android.content.Context;
import com.android.settings.AccessiblePreferenceCategory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class AccountRestrictionHelper {
    private final Context mContext;

    public AccountRestrictionHelper(Context context) {
        this.mContext = context;
    }

    public void enforceRestrictionOnPreference(RestrictedPreference restrictedPreference, String str, int i) {
        if (restrictedPreference == null) {
            return;
        }
        if (hasBaseUserRestriction(str, i)) {
            restrictedPreference.setEnabled(false);
        } else {
            restrictedPreference.checkRestrictionAndSetDisabled(str, i);
        }
    }

    public boolean hasBaseUserRestriction(String str, int i) {
        return RestrictedLockUtils.hasBaseUserRestriction(this.mContext, str, i);
    }

    public AccessiblePreferenceCategory createAccessiblePreferenceCategory(Context context) {
        return new AccessiblePreferenceCategory(context);
    }

    public static boolean showAccount(String[] strArr, ArrayList<String> arrayList) {
        if (strArr == null || arrayList == null) {
            return true;
        }
        for (String str : strArr) {
            if (arrayList.contains(str)) {
                return true;
            }
        }
        return false;
    }
}
