package java.nio.file;

public class FileAlreadyExistsException extends FileSystemException {
    static final long serialVersionUID = 7579540934498831181L;

    public FileAlreadyExistsException(String str) {
        super(str);
    }

    public FileAlreadyExistsException(String str, String str2, String str3) {
        super(str, str2, str3);
    }
}
