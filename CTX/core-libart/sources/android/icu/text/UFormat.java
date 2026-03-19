package android.icu.text;

import android.icu.util.ULocale;
import java.text.Format;

public abstract class UFormat extends Format {
    private static final long serialVersionUID = -4964390515840164416L;
    private ULocale actualLocale;
    private ULocale validLocale;

    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale uLocale, ULocale uLocale2) {
        if ((uLocale == null) != (uLocale2 == null)) {
            throw new IllegalArgumentException();
        }
        this.validLocale = uLocale;
        this.actualLocale = uLocale2;
    }
}
