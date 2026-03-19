package android.icu.text;

import java.text.FieldPosition;
import java.text.Format;

@Deprecated
public class UFieldPosition extends FieldPosition {
    private int countVisibleFractionDigits;
    private long fractionDigits;

    @Deprecated
    public UFieldPosition() {
        super(-1);
        this.countVisibleFractionDigits = -1;
        this.fractionDigits = 0L;
    }

    @Deprecated
    public UFieldPosition(int i) {
        super(i);
        this.countVisibleFractionDigits = -1;
        this.fractionDigits = 0L;
    }

    @Deprecated
    public UFieldPosition(Format.Field field, int i) {
        super(field, i);
        this.countVisibleFractionDigits = -1;
        this.fractionDigits = 0L;
    }

    @Deprecated
    public UFieldPosition(Format.Field field) {
        super(field);
        this.countVisibleFractionDigits = -1;
        this.fractionDigits = 0L;
    }

    @Deprecated
    public void setFractionDigits(int i, long j) {
        this.countVisibleFractionDigits = i;
        this.fractionDigits = j;
    }

    @Deprecated
    public int getCountVisibleFractionDigits() {
        return this.countVisibleFractionDigits;
    }

    @Deprecated
    public long getFractionDigits() {
        return this.fractionDigits;
    }
}
