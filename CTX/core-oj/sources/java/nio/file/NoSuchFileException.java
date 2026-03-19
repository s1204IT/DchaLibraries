package java.nio.file;

public class NoSuchFileException extends FileSystemException {
    static final long serialVersionUID = -1390291775875351931L;

    public NoSuchFileException(String str) {
        super(str);
    }

    public NoSuchFileException(String str, String str2, String str3) {
        super(str, str2, str3);
    }
}
