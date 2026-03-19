package java.util.logging;

public class ErrorManager {
    public static final int CLOSE_FAILURE = 3;
    public static final int FLUSH_FAILURE = 2;
    public static final int FORMAT_FAILURE = 5;
    public static final int GENERIC_FAILURE = 0;
    public static final int OPEN_FAILURE = 4;
    public static final int WRITE_FAILURE = 1;
    private boolean reported = false;

    public synchronized void error(String str, Exception exc, int i) {
        if (this.reported) {
            return;
        }
        this.reported = true;
        String str2 = "java.util.logging.ErrorManager: " + i;
        if (str != null) {
            str2 = str2 + ": " + str;
        }
        System.err.println(str2);
        if (exc != null) {
            exc.printStackTrace();
        }
    }
}
