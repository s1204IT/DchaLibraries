package android.icu.util;

import java.util.Date;
import java.util.Locale;

public class TaiwanCalendar extends GregorianCalendar {
    public static final int BEFORE_MINGUO = 0;
    private static final int GREGORIAN_EPOCH = 1970;
    public static final int MINGUO = 1;
    private static final int Taiwan_ERA_START = 1911;
    private static final long serialVersionUID = 2583005278132380631L;

    public TaiwanCalendar() {
    }

    public TaiwanCalendar(TimeZone timeZone) {
        super(timeZone);
    }

    public TaiwanCalendar(Locale locale) {
        super(locale);
    }

    public TaiwanCalendar(ULocale uLocale) {
        super(uLocale);
    }

    public TaiwanCalendar(TimeZone timeZone, Locale locale) {
        super(timeZone, locale);
    }

    public TaiwanCalendar(TimeZone timeZone, ULocale uLocale) {
        super(timeZone, uLocale);
    }

    public TaiwanCalendar(Date date) {
        this();
        setTime(date);
    }

    public TaiwanCalendar(int i, int i2, int i3) {
        super(i, i2, i3);
    }

    public TaiwanCalendar(int i, int i2, int i3, int i4, int i5, int i6) {
        super(i, i2, i3, i4, i5, i6);
    }

    @Override
    protected int handleGetExtendedYear() {
        if (newerField(19, 1) == 19 && newerField(19, 0) == 19) {
            return internalGet(19, GREGORIAN_EPOCH);
        }
        if (internalGet(0, 1) == 1) {
            return internalGet(1, 1) + Taiwan_ERA_START;
        }
        return (1 - internalGet(1, 1)) + Taiwan_ERA_START;
    }

    @Override
    protected void handleComputeFields(int i) {
        super.handleComputeFields(i);
        int iInternalGet = internalGet(19) - 1911;
        if (iInternalGet > 0) {
            internalSet(0, 1);
            internalSet(1, iInternalGet);
        } else {
            internalSet(0, 0);
            internalSet(1, 1 - iInternalGet);
        }
    }

    @Override
    protected int handleGetLimit(int i, int i2) {
        if (i == 0) {
            return (i2 == 0 || i2 == 1) ? 0 : 1;
        }
        return super.handleGetLimit(i, i2);
    }

    @Override
    public String getType() {
        return "roc";
    }
}
