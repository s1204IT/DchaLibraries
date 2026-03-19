package android.icu.text;

import android.icu.impl.CurrencyData;
import android.icu.util.ULocale;
import java.util.Locale;
import java.util.Map;

public abstract class CurrencyDisplayNames {
    public abstract String getName(String str);

    public abstract String getPluralName(String str, String str2);

    public abstract String getSymbol(String str);

    public abstract ULocale getULocale();

    public abstract Map<String, String> nameMap();

    public abstract Map<String, String> symbolMap();

    public static CurrencyDisplayNames getInstance(ULocale uLocale) {
        return CurrencyData.provider.getInstance(uLocale, true);
    }

    public static CurrencyDisplayNames getInstance(Locale locale) {
        return getInstance(locale, true);
    }

    public static CurrencyDisplayNames getInstance(ULocale uLocale, boolean z) {
        return CurrencyData.provider.getInstance(uLocale, !z);
    }

    public static CurrencyDisplayNames getInstance(Locale locale, boolean z) {
        return getInstance(ULocale.forLocale(locale), z);
    }

    @Deprecated
    public static boolean hasData() {
        return CurrencyData.provider.hasData();
    }

    @Deprecated
    protected CurrencyDisplayNames() {
    }
}
