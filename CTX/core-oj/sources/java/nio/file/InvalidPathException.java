package java.nio.file;

public class InvalidPathException extends IllegalArgumentException {
    static final long serialVersionUID = 4355821422286746137L;
    private int index;
    private String input;

    public InvalidPathException(String str, String str2, int i) {
        super(str2);
        if (str == null || str2 == null) {
            throw new NullPointerException();
        }
        if (i < -1) {
            throw new IllegalArgumentException();
        }
        this.input = str;
        this.index = i;
    }

    public InvalidPathException(String str, String str2) {
        this(str, str2, -1);
    }

    public String getInput() {
        return this.input;
    }

    public String getReason() {
        return super.getMessage();
    }

    public int getIndex() {
        return this.index;
    }

    @Override
    public String getMessage() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(getReason());
        if (this.index > -1) {
            stringBuffer.append(" at index ");
            stringBuffer.append(this.index);
        }
        stringBuffer.append(": ");
        stringBuffer.append(this.input);
        return stringBuffer.toString();
    }
}
