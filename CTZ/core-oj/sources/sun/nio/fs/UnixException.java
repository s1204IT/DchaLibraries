package sun.nio.fs;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

class UnixException extends Exception {
    static final long serialVersionUID = 7227016794320723218L;
    private int errno;
    private String msg;

    UnixException(int i) {
        this.errno = i;
        this.msg = null;
    }

    UnixException(String str) {
        this.errno = 0;
        this.msg = str;
    }

    int errno() {
        return this.errno;
    }

    void setError(int i) {
        this.errno = i;
        this.msg = null;
    }

    String errorString() {
        if (this.msg != null) {
            return this.msg;
        }
        return Util.toString(UnixNativeDispatcher.strerror(errno()));
    }

    @Override
    public String getMessage() {
        return errorString();
    }

    private IOException translateToIOException(String str, String str2) {
        if (this.msg != null) {
            return new IOException(this.msg);
        }
        if (errno() == UnixConstants.EACCES) {
            return new AccessDeniedException(str, str2, null);
        }
        if (errno() == UnixConstants.ENOENT) {
            return new NoSuchFileException(str, str2, null);
        }
        if (errno() == UnixConstants.EEXIST) {
            return new FileAlreadyExistsException(str, str2, null);
        }
        return new FileSystemException(str, str2, errorString());
    }

    void rethrowAsIOException(String str) throws IOException {
        throw translateToIOException(str, null);
    }

    void rethrowAsIOException(UnixPath unixPath, UnixPath unixPath2) throws IOException {
        String pathForExceptionMessage;
        if (unixPath != null) {
            pathForExceptionMessage = unixPath.getPathForExceptionMessage();
        } else {
            pathForExceptionMessage = null;
        }
        throw translateToIOException(pathForExceptionMessage, unixPath2 != null ? unixPath2.getPathForExceptionMessage() : null);
    }

    void rethrowAsIOException(UnixPath unixPath) throws IOException {
        rethrowAsIOException(unixPath, null);
    }

    IOException asIOException(UnixPath unixPath) {
        return translateToIOException(unixPath.getPathForExceptionMessage(), null);
    }
}
