package gov.nist.core;

public class InternalErrorHandler {
    public static void handleException(Exception exc) throws RuntimeException {
        System.err.println("Unexpected internal error FIXME!! " + exc.getMessage());
        exc.printStackTrace();
        throw new RuntimeException("Unexpected internal error FIXME!! " + exc.getMessage(), exc);
    }

    public static void handleException(Exception exc, StackLogger stackLogger) {
        System.err.println("Unexpected internal error FIXME!! " + exc.getMessage());
        stackLogger.logError("UNEXPECTED INTERNAL ERROR FIXME " + exc.getMessage());
        exc.printStackTrace();
        stackLogger.logException(exc);
        throw new RuntimeException("Unexpected internal error FIXME!! " + exc.getMessage(), exc);
    }

    public static void handleException(String str) {
        new Exception().printStackTrace();
        System.err.println("Unexepcted INTERNAL ERROR FIXME!!");
        System.err.println(str);
        throw new RuntimeException(str);
    }

    public static void handleException(String str, StackLogger stackLogger) {
        stackLogger.logStackTrace();
        stackLogger.logError("Unexepcted INTERNAL ERROR FIXME!!");
        stackLogger.logFatalError(str);
        throw new RuntimeException(str);
    }
}
