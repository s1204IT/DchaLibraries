package java.sql;

import java.io.Serializable;

public class DataTruncation extends SQLWarning implements Serializable {
    private static final int THE_ERROR_CODE = 0;
    private static final String THE_REASON = "Data truncation";
    private static final String THE_SQLSTATE_READ = "01004";
    private static final String THE_SQLSTATE_WRITE = "22001";
    private static final long serialVersionUID = 6464298989504059473L;
    private int dataSize;
    private int index;
    private boolean parameter;
    private boolean read;
    private int transferSize;

    public DataTruncation(int index, boolean parameter, boolean read, int dataSize, int transferSize) {
        super(THE_REASON, THE_SQLSTATE_READ, 0);
        this.index = 0;
        this.parameter = false;
        this.read = false;
        this.dataSize = 0;
        this.transferSize = 0;
        this.index = index;
        this.parameter = parameter;
        this.read = read;
        this.dataSize = dataSize;
        this.transferSize = transferSize;
    }

    public DataTruncation(int index, boolean parameter, boolean read, int dataSize, int transferSize, Throwable cause) {
        super(THE_REASON, read ? THE_SQLSTATE_READ : THE_SQLSTATE_WRITE, 0, cause);
        this.index = 0;
        this.parameter = false;
        this.read = false;
        this.dataSize = 0;
        this.transferSize = 0;
        this.index = index;
        this.parameter = parameter;
        this.read = read;
        this.dataSize = dataSize;
        this.transferSize = transferSize;
    }

    public int getDataSize() {
        return this.dataSize;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean getParameter() {
        return this.parameter;
    }

    public boolean getRead() {
        return this.read;
    }

    public int getTransferSize() {
        return this.transferSize;
    }
}
