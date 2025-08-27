package com.android.settings.language;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.inputmethod.UserDictionarySettings;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.TreeSet;

/* loaded from: classes.dex */
public class UserDictionaryPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public UserDictionaryPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "key_user_dictionary_settings";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        Class cls;
        if (!isAvailable() || preference == null) {
            return;
        }
        TreeSet<String> dictionaryLocales = getDictionaryLocales();
        Bundle extras = preference.getExtras();
        if (dictionaryLocales.size() <= 1) {
            if (!dictionaryLocales.isEmpty()) {
                extras.putString("locale", dictionaryLocales.first());
            }
            cls = UserDictionarySettings.class;
        } else {
            cls = UserDictionaryList.class;
        }
        preference.setFragment(cls.getCanonicalName());
    }

    protected TreeSet<String> getDictionaryLocales() {
        return UserDictionaryList.getUserDictionaryLocalesSet(this.mContext);
    }
}
