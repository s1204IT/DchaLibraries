package com.android.settings.accessibility;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import com.android.internal.app.LocalePicker;
import com.android.settings.R;
import java.util.List;

public class LocalePreference extends ListPreference {
    public LocalePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocalePreference(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context) {
        List<LocalePicker.LocaleInfo> locales = LocalePicker.getAllAssetLocales(context, false);
        int finalSize = locales.size();
        CharSequence[] entries = new CharSequence[finalSize + 1];
        CharSequence[] entryValues = new CharSequence[finalSize + 1];
        entries[0] = context.getResources().getString(R.string.locale_default);
        entryValues[0] = "";
        for (int i = 0; i < finalSize; i++) {
            LocalePicker.LocaleInfo info = locales.get(i);
            entries[i + 1] = info.toString();
            entryValues[i + 1] = info.getLocale().toString();
        }
        setEntries(entries);
        setEntryValues(entryValues);
    }
}
