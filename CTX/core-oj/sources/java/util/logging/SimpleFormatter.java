package java.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import sun.util.logging.LoggingSupport;

public class SimpleFormatter extends Formatter {
    private static final String format = LoggingSupport.getSimpleFormat();
    private final Date dat = new Date();

    @Override
    public synchronized String format(LogRecord logRecord) {
        String loggerName;
        String message;
        String string;
        this.dat.setTime(logRecord.getMillis());
        if (logRecord.getSourceClassName() != null) {
            loggerName = logRecord.getSourceClassName();
            if (logRecord.getSourceMethodName() != null) {
                loggerName = loggerName + " " + logRecord.getSourceMethodName();
            }
        } else {
            loggerName = logRecord.getLoggerName();
        }
        message = formatMessage(logRecord);
        string = "";
        if (logRecord.getThrown() != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println();
            logRecord.getThrown().printStackTrace(printWriter);
            printWriter.close();
            string = stringWriter.toString();
        }
        return String.format(format, this.dat, loggerName, logRecord.getLoggerName(), logRecord.getLevel().getLocalizedLevelName(), message, string);
    }
}
