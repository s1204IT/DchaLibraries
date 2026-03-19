package java.nio.file;

import java.io.IOException;

public class FileSystemException extends IOException {
    static final long serialVersionUID = -3055425747967319812L;
    private final String file;
    private final String other;

    public FileSystemException(String str) {
        super((String) null);
        this.file = str;
        this.other = null;
    }

    public FileSystemException(String str, String str2, String str3) {
        super(str3);
        this.file = str;
        this.other = str2;
    }

    public String getFile() {
        return this.file;
    }

    public String getOtherFile() {
        return this.other;
    }

    public String getReason() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        if (this.file == null && this.other == null) {
            return getReason();
        }
        StringBuilder sb = new StringBuilder();
        if (this.file != null) {
            sb.append(this.file);
        }
        if (this.other != null) {
            sb.append(" -> ");
            sb.append(this.other);
        }
        if (getReason() != null) {
            sb.append(": ");
            sb.append(getReason());
        }
        return sb.toString();
    }
}
