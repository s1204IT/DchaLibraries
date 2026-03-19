package android.icu.number;

import android.icu.util.ULocale;
import java.util.Locale;

public class UnlocalizedNumberFormatter extends NumberFormatterSettings<UnlocalizedNumberFormatter> {
    UnlocalizedNumberFormatter() {
        super(null, 12, new Long(3L));
    }

    UnlocalizedNumberFormatter(NumberFormatterSettings<?> numberFormatterSettings, int i, Object obj) {
        super(numberFormatterSettings, i, obj);
    }

    public LocalizedNumberFormatter locale(Locale locale) {
        return new LocalizedNumberFormatter(this, 1, ULocale.forLocale(locale));
    }

    public LocalizedNumberFormatter locale(ULocale uLocale) {
        return new LocalizedNumberFormatter(this, 1, uLocale);
    }

    @Override
    UnlocalizedNumberFormatter create(int i, Object obj) {
        return new UnlocalizedNumberFormatter(this, i, obj);
    }
}
