package java.io;

public class FileNotFoundException extends IOException {
    private static final long serialVersionUID = -897856973823710492L;

    public FileNotFoundException() {
    }

    public FileNotFoundException(String str) {
        super(str);
    }

    private FileNotFoundException(String str, String str2) {
        String str3;
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        if (str2 == null) {
            str3 = "";
        } else {
            str3 = " (" + str2 + ")";
        }
        sb.append(str3);
        super(sb.toString());
    }
}
