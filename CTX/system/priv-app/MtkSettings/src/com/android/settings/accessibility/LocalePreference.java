package com.android.settings.accessibility;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;
import com.android.internal.app.LocalePicker;
import com.android.settings.R;
import java.util.List;
/* loaded from: classes.dex */
public class LocalePreference extends ListPreference {
    public LocalePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public LocalePreference(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context) {
        int i = 0;
        List allAssetLocales = LocalePicker.getAllAssetLocales(context, false);
        int size = allAssetLocales.size();
        int i2 = size + 1;
        CharSequence[] charSequenceArr = new CharSequence[i2];
        CharSequence[] charSequenceArr2 = new CharSequence[i2];
        charSequenceArr[0] = context.getResources().getString(R.string.locale_default);
        charSequenceArr2[0] = "";
        while (i < size) {
            LocalePicker.LocaleInfo localeInfo = (LocalePicker.LocaleInfo) allAssetLocales.get(i);
            i++;
            charSequenceArr[i] = localeInfo.toString();
            charSequenceArr2[i] = localeInfo.getLocale().toString();
        }
        setEntries(charSequenceArr);
        setEntryValues(charSequenceArr2);
    }
}
