package gov.nist.core;

public class Debug {
    public static boolean debug = false;
    public static boolean parserDebug = false;
    static StackLogger stackLogger;

    public static void setStackLogger(StackLogger stackLogger2) {
        stackLogger = stackLogger2;
    }

    public static void println(String str) {
        if ((parserDebug || debug) && stackLogger != null) {
            stackLogger.logDebug(str + Separators.RETURN);
        }
    }

    public static void printStackTrace(Exception exc) {
        if ((parserDebug || debug) && stackLogger != null) {
            stackLogger.logError("Stack Trace", exc);
        }
    }

    public static void logError(String str, Exception exc) {
        if ((parserDebug || debug) && stackLogger != null) {
            stackLogger.logError(str, exc);
        }
    }
}
