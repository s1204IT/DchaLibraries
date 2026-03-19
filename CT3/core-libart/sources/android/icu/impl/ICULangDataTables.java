package android.icu.impl;

import android.icu.impl.LocaleDisplayNamesImpl;
import android.icu.util.ULocale;

public class ICULangDataTables extends LocaleDisplayNamesImpl.ICUDataTables {
    @Override
    public LocaleDisplayNamesImpl.DataTable get(ULocale locale) {
        return super.get(locale);
    }

    public ICULangDataTables() {
        super("android/icu/impl/data/icudt56b/lang");
    }
}
