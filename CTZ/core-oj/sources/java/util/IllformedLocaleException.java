package java.util;

public class IllformedLocaleException extends RuntimeException {
    private static final long serialVersionUID = -5245986824925681401L;
    private int _errIdx;

    public IllformedLocaleException() {
        this._errIdx = -1;
    }

    public IllformedLocaleException(String str) {
        super(str);
        this._errIdx = -1;
    }

    public IllformedLocaleException(String str, int i) {
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        if (i < 0) {
            str2 = "";
        } else {
            str2 = " [at index " + i + "]";
        }
        sb.append(str2);
        super(sb.toString());
        this._errIdx = -1;
        this._errIdx = i;
    }

    public int getErrorIndex() {
        return this._errIdx;
    }
}
