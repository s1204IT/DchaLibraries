package java.util.logging;

import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javax.xml.transform.OutputKeys;

public class XMLFormatter extends Formatter {
    private static final String indent = "    ";

    @Override
    public String format(LogRecord r) {
        long time = r.getMillis();
        String date = MessageFormat.format("{0, date} {0, time}", new Date(time));
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("<record>").append(nl);
        append(sb, 1, "date", date);
        append(sb, 1, "millis", Long.valueOf(time));
        append(sb, 1, "sequence", Long.valueOf(r.getSequenceNumber()));
        if (r.getLoggerName() != null) {
            append(sb, 1, "logger", r.getLoggerName());
        }
        append(sb, 1, "level", r.getLevel().getName());
        if (r.getSourceClassName() != null) {
            append(sb, 1, "class", r.getSourceClassName());
        }
        if (r.getSourceMethodName() != null) {
            append(sb, 1, OutputKeys.METHOD, r.getSourceMethodName());
        }
        append(sb, 1, "thread", Integer.valueOf(r.getThreadID()));
        formatMessages(r, sb);
        Object[] params = r.getParameters();
        if (params != null) {
            for (Object element : params) {
                append(sb, 1, "param", element);
            }
        }
        formatThrowable(r, sb);
        sb.append("</record>").append(nl);
        return sb.toString();
    }

    private void formatMessages(LogRecord r, StringBuilder sb) {
        String message;
        ResourceBundle rb = r.getResourceBundle();
        String pattern = r.getMessage();
        if (rb != null && pattern != null) {
            try {
                message = rb.getString(pattern);
            } catch (Exception e) {
                message = null;
            }
            if (message == null) {
                append(sb, 1, "message", pattern);
                return;
            }
            append(sb, 1, "message", message);
            append(sb, 1, "key", pattern);
            append(sb, 1, "catalog", r.getResourceBundleName());
            return;
        }
        if (pattern != null) {
            append(sb, 1, "message", pattern);
        } else {
            sb.append(indent).append("<message/>");
        }
    }

    private void formatThrowable(LogRecord r, StringBuilder sb) {
        Throwable t = r.getThrown();
        if (t != null) {
            String nl = System.lineSeparator();
            sb.append(indent).append("<exception>").append(nl);
            append(sb, 2, "message", t.toString());
            StackTraceElement[] elements = t.getStackTrace();
            for (StackTraceElement e : elements) {
                sb.append(indent).append(indent).append("<frame>").append(nl);
                append(sb, 3, "class", e.getClassName());
                append(sb, 3, OutputKeys.METHOD, e.getMethodName());
                append(sb, 3, "line", Integer.valueOf(e.getLineNumber()));
                sb.append(indent).append(indent).append("</frame>").append(nl);
            }
            sb.append(indent).append("</exception>").append(nl);
        }
    }

    private static void append(StringBuilder sb, int indentCount, String tag, Object value) {
        for (int i = 0; i < indentCount; i++) {
            sb.append(indent);
        }
        sb.append("<").append(tag).append(">");
        sb.append(value);
        sb.append("</").append(tag).append(">");
        sb.append(System.lineSeparator());
    }

    @Override
    public String getHead(Handler h) {
        String encoding = null;
        if (h != null) {
            encoding = h.getEncoding();
        }
        if (encoding == null) {
            encoding = System.getProperty("file.encoding");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"").append(encoding).append("\" standalone=\"no\"?>");
        sb.append(System.lineSeparator());
        sb.append("<!DOCTYPE log SYSTEM \"logger.dtd\">");
        sb.append(System.lineSeparator());
        sb.append("<log>");
        return sb.toString();
    }

    @Override
    public String getTail(Handler h) {
        return "</log>";
    }
}
