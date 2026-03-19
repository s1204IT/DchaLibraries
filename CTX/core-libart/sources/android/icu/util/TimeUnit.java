package android.icu.util;

import android.icu.util.MeasureUnit;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;

public class TimeUnit extends MeasureUnit {
    private static final long serialVersionUID = -2839973855554750484L;
    private final int index;

    TimeUnit(String str, String str2) {
        super(str, str2);
        this.index = 0;
    }

    public static TimeUnit[] values() {
        return new TimeUnit[]{SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR};
    }

    private Object writeReplace() throws ObjectStreamException {
        return new MeasureUnit.MeasureUnitProxy(this.type, this.subType);
    }

    private Object readResolve() throws ObjectStreamException {
        switch (this.index) {
            case 0:
                return YEAR;
            case 1:
                return MONTH;
            case 2:
                return WEEK;
            case 3:
                return DAY;
            case 4:
                return HOUR;
            case 5:
                return MINUTE;
            case 6:
                return SECOND;
            default:
                throw new InvalidObjectException("Bad index: " + this.index);
        }
    }
}
