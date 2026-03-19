package java.sql;

import java.util.Arrays;

public class BatchUpdateException extends SQLException {
    private static final long serialVersionUID = 5977529877145521757L;
    private final int[] updateCounts;

    public BatchUpdateException(String str, String str2, int i, int[] iArr) {
        super(str, str2, i);
        this.updateCounts = iArr == null ? null : Arrays.copyOf(iArr, iArr.length);
    }

    public BatchUpdateException(String str, String str2, int[] iArr) {
        this(str, str2, 0, iArr);
    }

    public BatchUpdateException(String str, int[] iArr) {
        this(str, (String) null, 0, iArr);
    }

    public BatchUpdateException(int[] iArr) {
        this((String) null, (String) null, 0, iArr);
    }

    public BatchUpdateException() {
        this((String) null, (String) null, 0, (int[]) null);
    }

    public BatchUpdateException(Throwable th) {
        this(th == null ? null : th.toString(), null, 0, null, th);
    }

    public BatchUpdateException(int[] iArr, Throwable th) {
        this(th == null ? null : th.toString(), null, 0, iArr, th);
    }

    public BatchUpdateException(String str, int[] iArr, Throwable th) {
        this(str, null, 0, iArr, th);
    }

    public BatchUpdateException(String str, String str2, int[] iArr, Throwable th) {
        this(str, str2, 0, iArr, th);
    }

    public BatchUpdateException(String str, String str2, int i, int[] iArr, Throwable th) {
        super(str, str2, i, th);
        this.updateCounts = iArr == null ? null : Arrays.copyOf(iArr, iArr.length);
    }

    public int[] getUpdateCounts() {
        if (this.updateCounts == null) {
            return null;
        }
        return Arrays.copyOf(this.updateCounts, this.updateCounts.length);
    }
}
