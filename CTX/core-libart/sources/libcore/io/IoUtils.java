package libcore.io;

import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class IoUtils {
    private IoUtils() {
    }

    public static void close(FileDescriptor fileDescriptor) throws IOException {
        if (fileDescriptor != null) {
            try {
                if (fileDescriptor.valid()) {
                    Libcore.os.close(fileDescriptor);
                }
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        }
    }

    public static void closeQuietly(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
            }
        }
    }

    public static void closeQuietly(FileDescriptor fileDescriptor) {
        try {
            close(fileDescriptor);
        } catch (IOException e) {
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    public static void setBlocking(FileDescriptor fileDescriptor, boolean z) throws IOException {
        int i;
        try {
            int iFcntlVoid = Libcore.os.fcntlVoid(fileDescriptor, OsConstants.F_GETFL);
            if (!z) {
                i = OsConstants.O_NONBLOCK | iFcntlVoid;
            } else {
                i = (~OsConstants.O_NONBLOCK) & iFcntlVoid;
            }
            Libcore.os.fcntlInt(fileDescriptor, OsConstants.F_SETFL, i);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public static byte[] readFileAsByteArray(String str) throws IOException {
        return new FileReader(str).readFully().toByteArray();
    }

    public static String readFileAsString(String str) throws IOException {
        return new FileReader(str).readFully().toString(StandardCharsets.UTF_8);
    }

    public static void deleteContents(File file) throws IOException {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                if (file2.isDirectory()) {
                    deleteContents(file2);
                }
                file2.delete();
            }
        }
    }

    public static File createTemporaryDirectory(String str) {
        File file;
        do {
            file = new File(System.getProperty("java.io.tmpdir"), str + Math.randomIntInternal());
        } while (!file.mkdir());
        return file;
    }

    public static boolean canOpenReadOnly(String str) {
        try {
            Libcore.os.close(Libcore.os.open(str, OsConstants.O_RDONLY, 0));
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    public static void throwInterruptedIoException() throws InterruptedIOException {
        Thread.currentThread().interrupt();
        throw new InterruptedIOException();
    }

    private static class FileReader {
        private byte[] bytes;
        private int count;
        private FileDescriptor fd;
        private boolean unknownLength;

        public FileReader(String str) throws IOException {
            try {
                this.fd = IoBridge.open(str, OsConstants.O_RDONLY);
                try {
                    int i = (int) Libcore.os.fstat(this.fd).st_size;
                    if (i == 0) {
                        this.unknownLength = true;
                        i = 8192;
                    }
                    this.bytes = new byte[i];
                } catch (ErrnoException e) {
                    IoUtils.closeQuietly(this.fd);
                    throw e.rethrowAsIOException();
                }
            } catch (FileNotFoundException e2) {
                throw e2;
            }
        }

        public FileReader readFully() throws IOException {
            int length = this.bytes.length;
            while (true) {
                try {
                    try {
                        int i = Libcore.os.read(this.fd, this.bytes, this.count, length - this.count);
                        if (i == 0) {
                            break;
                        }
                        this.count += i;
                        if (this.count == length) {
                            if (!this.unknownLength) {
                                break;
                            }
                            int i2 = length * 2;
                            byte[] bArr = new byte[i2];
                            System.arraycopy(this.bytes, 0, bArr, 0, length);
                            this.bytes = bArr;
                            length = i2;
                        }
                    } catch (ErrnoException e) {
                        throw e.rethrowAsIOException();
                    }
                } finally {
                    IoUtils.closeQuietly(this.fd);
                }
            }
            return this;
        }

        @FindBugsSuppressWarnings({"EI_EXPOSE_REP"})
        public byte[] toByteArray() {
            if (this.count == this.bytes.length) {
                return this.bytes;
            }
            byte[] bArr = new byte[this.count];
            System.arraycopy(this.bytes, 0, bArr, 0, this.count);
            return bArr;
        }

        public String toString(Charset charset) {
            return new String(this.bytes, 0, this.count, charset);
        }
    }
}
