package java.io;

public class FileWriter extends OutputStreamWriter {
    public FileWriter(String str) throws IOException {
        super(new FileOutputStream(str));
    }

    public FileWriter(String str, boolean z) throws IOException {
        super(new FileOutputStream(str, z));
    }

    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file));
    }

    public FileWriter(File file, boolean z) throws IOException {
        super(new FileOutputStream(file, z));
    }

    public FileWriter(FileDescriptor fileDescriptor) {
        super(new FileOutputStream(fileDescriptor));
    }
}
