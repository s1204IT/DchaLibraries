package android.icu.impl;

import android.icu.impl.LocaleDisplayNamesImpl;
import android.icu.util.ULocale;

public class ICURegionDataTables extends LocaleDisplayNamesImpl.ICUDataTables {
    @Override
    public LocaleDisplayNamesImpl.DataTable get(ULocale locale) {
        return super.get(locale);
    }

    public ICURegionDataTables() {
        super("android/icu/impl/data/icudt56b/region");
    }
}
