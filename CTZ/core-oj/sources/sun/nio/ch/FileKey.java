package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;

public class FileKey {
    private long st_dev;
    private long st_ino;

    private native void init(FileDescriptor fileDescriptor) throws IOException;

    private FileKey() {
    }

    public static FileKey create(FileDescriptor fileDescriptor) {
        FileKey fileKey = new FileKey();
        try {
            fileKey.init(fileDescriptor);
            return fileKey;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public int hashCode() {
        return ((int) (this.st_dev ^ (this.st_dev >>> 32))) + ((int) (this.st_ino ^ (this.st_ino >>> 32)));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FileKey)) {
            return false;
        }
        FileKey fileKey = (FileKey) obj;
        return this.st_dev == fileKey.st_dev && this.st_ino == fileKey.st_ino;
    }
}
