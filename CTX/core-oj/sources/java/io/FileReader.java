package java.io;

public class FileReader extends InputStreamReader {
    public FileReader(String str) throws FileNotFoundException {
        super(new FileInputStream(str));
    }

    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

    public FileReader(FileDescriptor fileDescriptor) {
        super(new FileInputStream(fileDescriptor));
    }
}
