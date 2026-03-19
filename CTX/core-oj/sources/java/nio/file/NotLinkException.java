package java.nio.file;

public class NotLinkException extends FileSystemException {
    static final long serialVersionUID = -388655596416518021L;

    public NotLinkException(String str) {
        super(str);
    }

    public NotLinkException(String str, String str2, String str3) {
        super(str, str2, str3);
    }
}
