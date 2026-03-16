package java.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import libcore.io.IoUtils;

public class SimpleFormatter extends Formatter {
    @Override
    public String format(LogRecord r) throws Throwable {
        StringWriter sw;
        PrintWriter pw;
        StringBuilder sb = new StringBuilder();
        sb.append(MessageFormat.format("{0, date} {0, time} ", new Date(r.getMillis())));
        sb.append(r.getSourceClassName()).append(" ");
        sb.append(r.getSourceMethodName()).append(System.lineSeparator());
        sb.append(r.getLevel().getName()).append(": ");
        sb.append(formatMessage(r)).append(System.lineSeparator());
        if (r.getThrown() != null) {
            sb.append("Throwable occurred: ");
            Throwable t = r.getThrown();
            PrintWriter pw2 = null;
            try {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
            } catch (Throwable th) {
                th = th;
            }
            try {
                t.printStackTrace(pw);
                sb.append(sw.toString());
                IoUtils.closeQuietly(pw);
            } catch (Throwable th2) {
                th = th2;
                pw2 = pw;
                IoUtils.closeQuietly(pw2);
                throw th;
            }
        }
        return sb.toString();
    }
}
