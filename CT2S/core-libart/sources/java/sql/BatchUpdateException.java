package java.sql;

import java.io.Serializable;

public class BatchUpdateException extends SQLException implements Serializable {
    private static final long serialVersionUID = 5977529877145521757L;
    private int[] updateCounts;

    public BatchUpdateException() {
        this.updateCounts = null;
    }

    public BatchUpdateException(Throwable cause) {
        this((int[]) null, cause);
    }

    public BatchUpdateException(int[] updateCounts, Throwable cause) {
        super(cause);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, int[] updateCounts, Throwable cause) {
        super(reason, cause);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, String SQLState, int[] updateCounts, Throwable cause) {
        super(reason, SQLState, cause);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode, int[] updateCounts, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(int[] updateCounts) {
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, int[] updateCounts) {
        super(reason);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, String SQLState, int[] updateCounts) {
        super(reason, SQLState);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode, int[] updateCounts) {
        super(reason, SQLState, vendorCode);
        this.updateCounts = null;
        this.updateCounts = updateCounts;
    }

    public int[] getUpdateCounts() {
        return this.updateCounts;
    }
}
