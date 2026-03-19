package java.sql;

public class DataTruncation extends SQLWarning {
    private static final long serialVersionUID = 6464298989504059473L;
    private int dataSize;
    private int index;
    private boolean parameter;
    private boolean read;
    private int transferSize;

    public DataTruncation(int i, boolean z, boolean z2, int i2, int i3) {
        super("Data truncation", z2 ? "01004" : "22001");
        this.index = i;
        this.parameter = z;
        this.read = z2;
        this.dataSize = i2;
        this.transferSize = i3;
    }

    public DataTruncation(int i, boolean z, boolean z2, int i2, int i3, Throwable th) {
        super("Data truncation", z2 ? "01004" : "22001", th);
        this.index = i;
        this.parameter = z;
        this.read = z2;
        this.dataSize = i2;
        this.transferSize = i3;
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

    public int getDataSize() {
        return this.dataSize;
    }

    public int getTransferSize() {
        return this.transferSize;
    }
}
