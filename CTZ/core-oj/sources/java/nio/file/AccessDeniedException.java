package java.nio.file;

public class AccessDeniedException extends FileSystemException {
    private static final long serialVersionUID = 4943049599949219617L;

    public AccessDeniedException(String str) {
        super(str);
    }

    public AccessDeniedException(String str, String str2, String str3) {
        super(str, str2, str3);
    }
}
