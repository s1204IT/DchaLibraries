package java.nio.file;

public class AtomicMoveNotSupportedException extends FileSystemException {
    static final long serialVersionUID = 5402760225333135579L;

    public AtomicMoveNotSupportedException(String str, String str2, String str3) {
        super(str, str2, str3);
    }
}
