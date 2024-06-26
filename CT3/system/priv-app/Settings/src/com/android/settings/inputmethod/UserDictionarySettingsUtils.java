package com.android.settings.inputmethod;

import android.content.Context;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.Locale;
/* loaded from: classes.dex */
public class UserDictionarySettingsUtils {
    public static String getLocaleDisplayName(Context context, String localeStr) {
        if (TextUtils.isEmpty(localeStr)) {
            return context.getResources().getString(R.string.user_dict_settings_all_languages);
        }
        Locale locale = Utils.createLocaleFromString(localeStr);
        Locale systemLocale = context.getResources().getConfiguration().locale;
        return locale.getDisplayName(systemLocale);
    }
}
